# Campus Taxi Pooling - Implementation Guide
## Phase 1-3: DI, SafeResult, Offline Caching

**Last Updated:** March 24, 2026  
**Status:** Phase 1-2 Complete ✅ | Phase 3 Infrastructure Ready 📦

---

## Quick Start: Using the New Architecture

### 1. Creating a ViewModel with Hilt DI

```java
@HiltViewModel
public class MyViewModel extends ViewModel {
    
    private final IRideRepository rideRepo;
    private final MutableLiveData<SafeResult<List<Ride>>> rides = new MutableLiveData<>();
    
    @Inject
    public MyViewModel(IRideRepository rideRepo) {
        this.rideRepo = rideRepo;
    }
    
    public LiveData<SafeResult<List<Ride>>> getRides() {
        return rides;
    }
    
    public void loadRides(String campusId, String uid) {
        rideRepo.getRideFeed(campusId, uid, 20)
            .observeForever(result -> rides.setValue(result));
    }
}
```

### 2. Observing Results in Fragment/Activity

```java
public class RideFeedFragment extends Fragment {
    
    private RideFeedViewModel viewModel;
    
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        viewModel = new ViewModelProvider(this).get(RideFeedViewModel.class);
        
        // Observe SafeResult<List<Ride>>
        viewModel.getRidesFeed().observe(this, result -> {
            if (result.isLoading()) {
                showLoadingSpinner();
            } else if (result.isSuccess()) {
                displayRides(result.getOrNull());
            } else if (result.isError()) {
                String msg = result.getUserMessage();
                ErrorHandler.showError(getView(), msg, result.canRetry(), 
                    () -> viewModel.retryLoadRidesFeed());
            } else if (result.isCached()) {
                displayRides(result.getOrNull());
                showSnackbar("Offline: Showing cached rides");
            }
        });
        
        viewModel.loadRidesFeed(campusId, currentUserUid);
    }
}
```

### 3. Creating a Repository Implementation

```java
@ActivityScoped
public class MyRepositoryImpl implements IMyRepository {
    
    private final FirebaseFirestore db;
    private final List<ListenerRegistration> listeners = new ArrayList<>();
    
    @Inject
    public MyRepositoryImpl(FirebaseFirestore db) {
        this.db = db;
    }
    
    @Override
    public LiveData<SafeResult<List<Item>>> getItems() {
        MutableLiveData<SafeResult<List<Item>>> result = new MutableLiveData<>(SafeResult.loading());
        
        ListenerRegistration listener = db.collection("items")
            .addSnapshotListener((snapshot, error) -> {
                if (error != null) {
                    Log.e(TAG, "Error loading items", error);
                    result.setValue(SafeResult.error(error, "Failed to load items"));
                    return;
                }
                
                if (snapshot != null) {
                    List<Item> items = snapshot.toObjects(Item.class);
                    result.setValue(SafeResult.success(items));
                }
            });
        
        listeners.add(listener);
        return result;
    }
    
    public void cleanup() {
        for (ListenerRegistration listener : listeners) {
            listener.remove();
        }
        listeners.clear();
    }
}
```

---

## Dependency Injection Setup

### Files in `di/` folder

1. **FirebaseModule.java** — Provides Firebase services
   - Configures Firestore with persistence enabled
   - Provides Auth, Storage instances

2. **RepositoryModule.java** — Binds interfaces to implementations
   - `@Binds IRideRepository → RideRepositoryImpl`
   - Scope: `@ActivityScoped` (one instance per Activity)

### How Hilt Works

```
┌─────────────────┐
│ @HiltAndroidApp │ CampusTaxiApp
│ (Root Component)│
└────────┬────────┘
         │
    ┌────▼────────────────────┐
    │  Hilt Component Graph   │
    │  (Dependency Tree)      │
    │                         │
    │  ┌─ FirebaseModule     │
    │  ├─ RepositoryModule   │
    │  └─ [Other modules]    │
    └────┬────────────────────┘
         │
    ┌────▼──────────────┐
    │ Activity/Fragment │
    │  @AndroidEntryPoint
    │                   │
    │  @Inject IRideRepo
    └───────────────────┘
```

**Key Points:**
- `@HiltAndroidApp` on Application class
- `@AndroidEntryPoint` on Activities/Fragments
- `@Inject` on constructor parameters
- Hilt generates code at compile time (annotation processor)

---

## SafeResult<T> Wrapper

### Status Enum

| Status | Usage | UI Action |
|--------|-------|-----------|
| `LOADING` | Async operation in progress | Show spinner |
| `SUCCESS` | Data available | Display data |
| `ERROR` | Operation failed | Show error message + retry button |
| `CACHED` | Data from local cache (offline) | Display data + "offline" badge |

### Error Codes

```
PERMISSION_DENIED    — User lacks access
NOT_FOUND           — Resource doesn't exist
TIMEOUT             — Operation took too long (retryable)
SERVICE_UNAVAILABLE — Server down (retryable)
NETWORK_ERROR       — Internet connection failed
UNAUTHENTICATED     — User needs to log in
INVALID_ARGUMENT    — Bad user input
QUOTA_EXCEEDED      — Too many requests
OFFLINE_CACHE       — Showing cached data
```

### Usage Pattern

```java
SafeResult<List<Ride>> result = ...;

// Check status
if (result.isLoading()) { }
else if (result.isSuccess()) { }
else if (result.isError()) { }
else if (result.isCached()) { }

// Get data
List<Ride> rides = result.getOrNull();
Throwable error = result.getException();

// Retry logic
if (result.canRetry()) {
    showRetryButton();
}

// User message
String msg = result.getUserMessage();
```

---

## Error Handling with ErrorHandler Utility

### Show Error Snackbar

```java
// With retry button
ErrorHandler.showError(
    rootView,
    "Failed to load rides",
    true,  // Show retry button
    () -> viewModel.retryLoadRidesFeed()
);

// Without retry
ErrorHandler.showError(rootView, "An error occurred");
```

### Show Fatal Error Dialog

```java
ErrorHandler.showFatalError(
    this,
    "Banned",
    "Your account has been suspended",
    "OK",
    () -> startActivity(new Intent(this, LoginActivity.class))
);
```

### Get Recommended Action

```java
ErrorHandler.ErrorAction action = ErrorHandler.getRecommendedAction(result);

switch (action) {
    case SHOW_RETRY:
        showRetryButton();
        break;
    case SHOW_LOGIN:
        redirectToLogin();
        break;
    case SHOW_SETTINGS:
        redirectToSettings();
        break;
    default:
        showDismissButton();
}
```

---

## Offline Caching with Room Database

### Architecture

```
Online Flow:
Firestore → RideRepository → Room (cache) → ViewModel → UI

Offline Flow:
(No Firestore) → RideRepository (fallback) → Room → ViewModel → UI
```

### Files Created

**Entities (in `db/entity/`):**
- `RideEntity.java` — Caches Ride documents
- `MessageEntity.java` — Caches chat messages
- `UserEntity.java` — Caches user profiles

**DAOs (in `db/dao/`):**
- `RideDao.java` — CRUD + queries on rides
- `MessageDao.java` — CRUD + queries on messages
- `UserDao.java` — CRUD + queries on users

**Database:**
- `CampusTaxiDatabase.java` — Room database instance

### Usage: Inserting Rides into Cache

```java
// In RideRepositoryImpl
rideRepository.getRideFeed(campusId, uid, limit)
    .observeForever(result -> {
        if (result.isSuccess()) {
            List<Ride> rides = result.getOrNull();
            
            // Also cache locally
            List<RideEntity> entities = convertToEntities(rides);
            database.rideDao().insertRides(entities);
        }
    });
```

### Usage: Reading from Cache (Offline)

```java
// In RideRepositoryImpl (fallback when offline)
private void getFeedFromCache(String campusId) {
    // Read from Room on background thread
    Thread thread = new Thread(() -> {
        List<RideEntity> cachedRides = database.rideDao()
            .getRideFeed(campusId, 20);
        
        List<Ride> rides = convertFromEntities(cachedRides);
        liveData.postValue(SafeResult.cached(rides));
    });
    thread.start();
}
```

### Syncing Offline Writes

Phase 3 deliverable: `SyncManager.java`
```java
public class SyncManager {
    
    public void syncOfflineWrites(Context context) {
        // 1. Get unsynced rides
        List<RideEntity> unsyncedRides = database.rideDao()
            .getUnsyncedRides();
        
        // 2. Retry uploading to Firestore
        for (RideEntity ride : unsyncedRides) {
            firestore.collection("rides")
                .document(ride.getRideId())
                .set(convertToFirestore(ride))
                .addOnSuccess(aVoid -> {
                    // Mark as synced
                    database.rideDao()
                        .markRideSynced(ride.getRideId());
                })
                .addOnFailure(e -> {
                    // Log and retry later
                    logSyncFailure(ride.getRideId(), e);
                });
        }
    }
}
```

---

## Testing Strategy

### Unit Test Example: RideRepositoryImplTest

```java
@ExtendWith(MockitoExtension.class)
class RideRepositoryImplTest {
    
    @Mock FirebaseFirestore mockDb;
    @InjectMocks RideRepositoryImpl repository;
    
    @Test
    void testGetRideFeed_Success() {
        // Arrange
        String campusId = "CU_CHANDIGARH";
        List<Ride> expectedRides = Arrays.asList(
            new Ride(...), new Ride(...)
        );
        
        // Mock Firestore response
        when(mockDb.collection("rides")
            .whereEqualTo("campusId", campusId)
            .addSnapshotListener(any()))
            .thenReturn(mockListenerRegistration);
        
        // Act
        LiveData<SafeResult<List<Ride>>> result = 
            repository.getRideFeed(campusId, "user123", 20);
        
        // Assert (would need to observe LiveData)
        // This is simplified - see Phase 5 for full test implementation
    }
    
    @Test
    void testPostRide_FirebaseError() {
        // Arrange
        Ride ride = new Ride(...);
        
        // Mock exception
        when(mockDb.collection("rides").add(any()))
            .thenReturn(Tasks.forException(
                new FirebaseFirestoreException("Permission denied")));
        
        // Act
        LiveData<SafeResult<String>> result = repository.postRide(ride);
        
        // Assert: Error is wrapped, not thrown
        assertFalse(result.getValue().isSuccess());
        assertEquals("PERMISSION_DENIED", result.getValue().getErrorCode());
    }
}
```

### UI Test Example: RideLifecycleTest (Espresso)

```java
@RunWith(AndroidJUnit4.class)
class RideLifecycleTest {
    
    @Rule
    public ActivityTestRule<HomeActivity> rule = 
        new ActivityTestRule<>(HomeActivity.class);
    
    @Test
    public void postRide_ThenJoinRide_Success() {
        // 1. Navigate to feed
        onView(withId(R.id.nav_feed))
            .perform(click());
        
        // 2. Click FAB to post ride
        onView(withId(R.id.postRideFab))
            .perform(click());
        
        // 3. Fill ride form
        onView(withId(R.id.sourceEt))
            .perform(typeText("Library"));
        onView(withId(R.id.destinationEt))
            .perform(typeText("Gate 1"));
        onView(withId(R.id.seatsEt))
            .perform(typeText("4"));
        onView(withId(R.id.fareEt))
            .perform(typeText("50"));
        
        // 4. Submit
        onView(withId(R.id.submitBtn))
            .perform(click());
        
        // 5. Verify success
        onView(withText("Ride posted successfully"))
            .check(matches(isDisplayed()));
    }
}
```

---

## Migration Checklist: From Old Code to New

### Step 1: Add Hilt to Activity/Fragment

```java
// Old
public class RideFeedFragment extends Fragment { }

// New
@AndroidEntryPoint
public class RideFeedFragment extends Fragment {
    @Inject IRideRepository rideRepo;
}
```

### Step 2: Replace Repository Singletons

```java
// Old
private RideRepository repo = RideRepository.getInstance();
repo.getRideFeed(...).addOnSuccessListener(...);

// New
@Inject IRideRepository repo;
repo.getRideFeed(...).observeForever(result -> { ... });
```

### Step 3: Handle SafeResult in Observer

```java
// Old
repo.getRideFeed(...).addOnSuccessListener(snapshots -> {
    for (DocumentSnapshot doc : snapshots) {
        Ride ride = doc.toObject(Ride.class);
        rides.add(ride);
    }
    adapter.notifyDataSetChanged();
}).addOnFailureListener(e -> {
    Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
});

// New
repo.getRideFeed(...).observeForever(result -> {
    if (result.isSuccess()) {
        adapter.setRides(result.getOrNull());
    } else if (result.isError()) {
        ErrorHandler.showError(view, result.getUserMessage(), result.canRetry(), 
            () -> retryLoad());
    }
});
```

### Step 4: Create ViewModel

```java
@HiltViewModel
public class RideFeedViewModel extends ViewModel {
    private final IRideRepository repo;
    private final MutableLiveData<SafeResult<List<Ride>>> feed = new MutableLiveData<>();
    
    @Inject
    public RideFeedViewModel(IRideRepository repo) {
        this.repo = repo;
    }
    
    public LiveData<SafeResult<List<Ride>>> getFeed() {
        return feed;
    }
    
    public void loadFeed(String campusId, String uid) {
        repo.getRideFeed(campusId, uid, 20)
            .observeForever(feed::setValue);
    }
}
```

### Step 5: Test Your Changes

```bash
cd C:\Users\HP\AndroidStudioProjects\CampusTaxiPooling

# Build
./gradlew clean build

# Run tests
./gradlew test

# Run on device
./gradlew installDebug
```

---

## Common Issues & Fixes

### Issue 1: "Cannot find symbol: @Inject"

**Cause:** Missing Hilt dependencies or annotation processor not running.

**Fix:**
```gradle
// In app/build.gradle.kts
plugins {
    id("com.google.dagger.hilt.android") version "2.48"
    id("kotlin-kapt")
}

dependencies {
    implementation("com.google.dagger:hilt-android:2.48")
    kapt("com.google.dagger:hilt-compiler:2.48")
}
```

### Issue 2: "Cannot resolve method observeForever"

**Cause:** Forgot to import correct SafeResult factories.

**Fix:**
```java
import com.princeraj.campustaxipooling.util.SafeResult;

// Correct
SafeResult.success(data)
SafeResult.error(exception, msg)
SafeResult.loading()
```

### Issue 3: Firestore Persistence Not Working

**Cause:** Settings not enabled in FirebaseModule.

**Fix:**
```java
// FirebaseModule.java
@Provides
@Singleton
public static FirebaseFirestore provideFirestore() {
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    try {
        db.firestoreSettings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)  // <-- CRITICAL
            .build();
    } catch (IllegalStateException e) {
        // Settings already set
    }
    return db;
}
```

### Issue 4: LiveData Never Updates in Tests

**Cause:** Need to use `observeForever` in tests (no lifecycle).

**Fix:**
```java
@Test
public void testRide() throws InterruptedException {
    MutableLiveData<SafeResult<Ride>> liveData = new MutableLiveData<>();
    
    // Observe without lifecycle
    Observer<SafeResult<Ride>> observer = liveData::setValue;
    liveData.observeForever(observer);
    
    // Test...
    assertEquals(SafeResult.Status.SUCCESS, liveData.getValue().getStatus());
    
    liveData.removeObserver(observer);
}
```

---

## Next: Phase 3 Full Implementation

### SyncManager (To Be Created)

```java
@Singleton
public class SyncManager {
    
    public void syncRidesOfflineWrites(List<RideEntity> unsyncedRides) {
        // Logic to retry failed writes
    }
    
    public void syncMessagesOfflineWrites(List<MessageEntity> unsyncedMessages) {
        // Logic to upload queued messages
    }
}
```

### ConflictResolver (To Be Created)

```java
@Singleton
public class ConflictResolver {
    
    /**
     * Merges local changes with remote changes.
     * Strategy: Last-write-wins for most fields, custom logic for critical fields.
     */
    public Ride resolveRideConflict(Ride local, Ride remote) {
        // Compare timestamps and choose winner
    }
}
```

---

## Resources & References

- **Hilt Documentation:** https://dagger.dev/hilt/
- **Room Database:** https://developer.android.com/training/data-storage/room
- **Firebase Firestore:** https://firebase.google.com/docs/firestore
- **LiveData:** https://developer.android.com/topic/libraries/architecture/livedata

---

## Summary Checklist

- ✅ Phase 1: Hilt DI configured
- ✅ Phase 2: SafeResult wrapper created
- ✅ Phase 2: Repository interfaces defined
- ✅ Phase 2: Error handling with ErrorHandler
- 📦 Phase 3: Room entities created
- 📦 Phase 3: DAOs created
- 📦 Phase 3: CampusTaxiDatabase created
- ⏳ Phase 3: SyncManager (pending)
- ⏳ Phase 3: ConflictResolver (pending)
- ⏳ Phase 4: Security audit & optimization
- ⏳ Phase 5: Testing infrastructure
- ⏳ Phase 6: Migration & monitoring

**Now ready to continue with Phase 3!** 🚀

