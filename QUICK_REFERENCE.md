# Campus Taxi Pooling - Quick Reference Card
## Phases 1-3 Implementation Guide

---

## 🎯 Architecture at a Glance

```
UI Layer (Fragments/Activities)
         ↓ observes
ViewModel (Business Logic)
         ↓ calls
Repository Interface (Abstraction)
         ↓ implements
Repository Implementation (Firestore + Room)
         ↓ uses
Firebase/Firestore + Room Database
```

---

## 📦 Dependency Injection (Hilt)

### 1. Mark Application Class
```java
@HiltAndroidApp
public class CampusTaxiApp extends Application { }
```

### 2. Mark Activities
```java
@AndroidEntryPoint
public class MainActivity extends AppCompatActivity { }
```

### 3. Inject Dependencies
```java
@HiltViewModel
public class MyViewModel extends ViewModel {
    @Inject
    public MyViewModel(IRideRepository repo) {
        this.repo = repo;
    }
}
```

### 4. Available Injections
- `FirebaseFirestore` (singleton, persistence enabled)
- `FirebaseAuth` (singleton)
- `FirebaseStorage` (singleton)
- `IRideRepository` (activity-scoped)
- `IUserRepository` (activity-scoped)
- `IChatRepository` (activity-scoped)
- `IReportRepository` (activity-scoped)

---

## 📊 SafeResult<T> Status

| Status | Meaning | UI Action |
|--------|---------|-----------|
| `LOADING` | Fetching data | Show spinner |
| `SUCCESS` | Data ready | Display data |
| `ERROR` | Operation failed | Show error + retry |
| `CACHED` | Offline data | Display + offline badge |

---

## 🔍 SafeResult Usage Pattern

```java
result.isLoading()              → boolean (show spinner)
result.isSuccess()              → boolean (display data)
result.isError()                → boolean (show error dialog)
result.isCached()               → boolean (show offline badge)

result.getOrNull()              → T (the data)
result.getException()           → Exception (for logging)
result.getUserMessage()         → String (user-friendly error)
result.getErrorCode()           → String (error type)
result.canRetry()               → boolean (is retryable)
result.isOffline()              → boolean (network down?)
```

---

## 🛠️ Common Code Patterns

### Pattern 1: Load Data
```java
// ViewModel
public LiveData<SafeResult<List<Ride>>> getRides() {
    return rides;
}

public void loadRides(String campusId, String uid) {
    repo.getRideFeed(campusId, uid, 20)
        .observeForever(rides::setValue);
}

// Fragment
viewModel.getRides().observe(this, result -> {
    if (result.isSuccess()) {
        showRides(result.getOrNull());
    } else if (result.isError()) {
        ErrorHandler.showError(view, result.getUserMessage(), 
            result.canRetry(), () -> viewModel.loadRides(...));
    }
});
```

### Pattern 2: Create/Update Data
```java
// ViewModel
public LiveData<SafeResult<String>> postRide(Ride ride) {
    return repo.postRide(ride);
}

// Fragment
viewModel.postRide(ride).observe(this, result -> {
    if (result.isSuccess()) {
        showSuccess("Ride posted!");
        navigateBack();
    } else {
        showError(result.getUserMessage());
    }
});
```

### Pattern 3: Handle Errors Gracefully
```java
result.observe(this, res -> {
    if (res.isError()) {
        String msg = res.getUserMessage();
        boolean canRetry = res.canRetry();
        boolean isOffline = res.isOffline();
        
        ErrorHandler.showError(view, msg, canRetry, this::retry);
        
        if (isOffline) {
            showOfflineBadge();
        }
    }
});
```

---

## 💾 Room Database Operations

### Insert/Update
```java
RideEntity entity = ...;
database.rideDao().insertRide(entity);
database.rideDao().updateRide(entity);
```

### Query
```java
List<RideEntity> rides = database.rideDao()
    .getRideFeed(campusId, 20);

RideEntity ride = database.rideDao()
    .getRideById(rideId);

List<RideEntity> unsyncedRides = database.rideDao()
    .getUnsyncedRides();
```

### Offline Fallback (Repository Pattern)
```java
public LiveData<SafeResult<List<Ride>>> getRideFeed(...) {
    return db.collection("rides")
        .whereEqualTo("campusId", campusId)
        .addSnapshotListener((snap, err) -> {
            if (err != null) {
                // Fallback to cache
                List<RideEntity> cached = database.rideDao()
                    .getRideFeed(campusId, limit);
                
                liveData.setValue(SafeResult.cached(
                    convertFromEntities(cached)));
            }
        });
}
```

---

## 🎨 Error Handling Utilities

```java
// Show error snackbar with retry
ErrorHandler.showError(
    rootView, 
    "Failed to load rides", 
    true,  // show retry button
    () -> viewModel.retryLoadRidesFeed()
);

// Get recommended action for error
ErrorHandler.ErrorAction action = 
    ErrorHandler.getRecommendedAction(result);

if (action == ErrorHandler.ErrorAction.SHOW_LOGIN) {
    redirectToLogin();
} else if (action == ErrorHandler.ErrorAction.SHOW_RETRY) {
    showRetryButton();
}

// Log error for debugging
ErrorHandler.logError(TAG, result);
```

---

## 🔐 Repository Interfaces

### IRideRepository
```
postRide(Ride)                              → LiveData<SafeResult<String>>
getRideFeed(campusId, uid, limit)           → LiveData<SafeResult<List<Ride>>>
getMyPostedRides(uid)                       → LiveData<SafeResult<List<Ride>>>
getRideById(rideId)                         → LiveData<SafeResult<Ride>>
acceptSeatRequest(rideId, requestId, ...)  → LiveData<SafeResult<Void>>
sendJoinRequest(rideId, request, ...)      → LiveData<SafeResult<String>>
```

### IUserRepository
```
registerUser(email, password, name, ...)    → LiveData<SafeResult<AuthResult>>
loginUser(email, password)                  → LiveData<SafeResult<AuthResult>>
getUserProfile(uid)                         → LiveData<SafeResult<User>>
updateFcmToken(uid, token)                  → LiveData<SafeResult<Void>>
isUserBanned(uid)                           → LiveData<SafeResult<Boolean>>
```

### IChatRepository
```
getMessages(connectionId)                   → LiveData<SafeResult<List<Message>>>
sendMessage(connectionId, message, ...)     → LiveData<SafeResult<Void>>
moderateMessage(text)                       → ModerationResult
```

### IReportRepository
```
submitReport(report)                        → LiveData<SafeResult<Void>>
getPendingReports()                         → LiveData<SafeResult<List<Report>>>
banUser(uid, reason)                        → LiveData<SafeResult<Void>>
```

---

## 🧪 Testing with Hilt & Mocks

```java
@ExtendWith(MockitoExtension.class)
class RideRepositoryTest {
    
    @Mock FirebaseFirestore mockDb;
    @InjectMocks RideRepositoryImpl repository;
    
    @Test
    void testPostRide_Success() {
        // Arrange
        Ride ride = new Ride(...);
        
        // Act
        LiveData<SafeResult<String>> result = repo.postRide(ride);
        
        // Assert
        assertTrue(result.getValue().isSuccess());
    }
}
```

---

## 📋 Entity Classes

### RideEntity
```java
// Key fields:
String rideId
String postedByUid
String campusId
String source, destination
long totalFare
int seatsRemaining
String status           // ACTIVE, FULL, COMPLETED, CANCELLED
Long syncedAt          // Track sync state
```

### MessageEntity
```java
String messageId
String connectionId
String senderUid
String text
boolean isFlagged      // Moderation flag
Long syncedAt
```

### UserEntity
```java
String uid
String name, email
String role            // STUDENT, FACULTY, ADMIN
boolean isBanned
Long syncedAt
```

---

## 🚨 Error Codes Reference

```
PERMISSION_DENIED       User lacks access (non-retryable)
NOT_FOUND              Resource doesn't exist (non-retryable)
TIMEOUT                Operation too slow (RETRYABLE ⚡)
SERVICE_UNAVAILABLE    Server down (RETRYABLE ⚡)
NETWORK_ERROR          No internet (RETRYABLE ⚡)
UNAUTHENTICATED        Not logged in (non-retryable)
QUOTA_EXCEEDED         Too many requests (RETRYABLE ⚡)
INVALID_ARGUMENT       Bad input (non-retryable)
OFFLINE_CACHE          Using cached data
```

---

## ⚡ Performance Tips

1. **Use observeForever sparingly** — Unsubscribe when done
```java
Observer<SafeResult<T>> observer = result -> { };
liveData.observeForever(observer);
// Later...
liveData.removeObserver(observer);
```

2. **Batch database operations**
```java
database.rideDao().insertRides(rideList);  // Good
// vs
for (Ride r : rideList) database.rideDao().insertRide(r);  // Slow
```

3. **Use composite Firestore indexes**
```
Collection: rides
Indexes: [campusId, status, createdAt DESC]
```

4. **Enable Firestore persistence** ✅ (already done in FirebaseModule)

---

## 🔧 Gradle Build Commands

```bash
# Full build
./gradlew clean build

# Build without tests
./gradlew build -x test

# Run unit tests
./gradlew test

# Run instrumentation tests
./gradlew connectedAndroidTest

# Lint check
./gradlew lint

# Install on device/emulator
./gradlew installDebug

# Quick compile check
./gradlew compileDebugSources
```

---

## 📱 Testing Offline (Emulator)

```
Android Studio → Emulator Control Panel → Network
→ Set to "Offline" or "Limited"

Or via adb:
adb shell cmd connectivity airplane-mode enable
adb shell cmd connectivity airplane-mode disable
```

---

## 🎯 Next Steps Checklist

- [ ] Run `./gradlew clean build` to verify no errors
- [ ] Migrate one Activity to @AndroidEntryPoint
- [ ] Update one Fragment to use ViewModel + SafeResult
- [ ] Test with ErrorHandler on error cases
- [ ] Try offline mode in emulator
- [ ] Create unit test for one Repository
- [ ] Read IMPLEMENTATION_GUIDE.md for detailed examples

---

## 📚 Documentation Files (In Order)

1. **PROGRESS_SUMMARY.md** — Overall status & next steps
2. **PRODUCTION_AUDIT_REPORT.md** — Findings & security audit
3. **IMPLEMENTATION_GUIDE.md** — Detailed usage guide
4. **This file** — Quick reference card

---

**Good luck! You've got this! 🚀**

For detailed examples, see:
- `RideFeedViewModel.java` — ViewModel pattern
- `RideRepositoryImpl.java` — Repository pattern with SafeResult
- `ErrorHandler.java` — Error handling utilities


