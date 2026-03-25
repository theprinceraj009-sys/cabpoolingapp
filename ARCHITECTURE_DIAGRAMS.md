# Architecture Diagrams & System Design
## Campus Taxi Pooling Application

---

## 1. Overall System Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                         PRESENTATION LAYER                          │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌────────────┐ │
│  │  Activities │  │  Fragments  │  │    Views    │  │ Resources  │ │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘  └────────────┘ │
│         │                 │                │                         │
│         └─────────────────┼─────────────────┘                         │
│                           ↓                                           │
│        ┌──────────────────────────────────┐                          │
│        │      ViewModels (@HiltViewModel) │                          │
│        │  • RideFeedViewModel             │                          │
│        │  • ChatListViewModel             │                          │
│        │  • MyRidesViewModel              │                          │
│        └──────────────┬───────────────────┘                          │
│                       ↓ observes                                      │
│    ┌──────────────────────────────────────┐                          │
│    │  LiveData<SafeResult<T>>             │                          │
│    │  • Encapsulates data + state + error │                          │
│    └──────────────┬───────────────────────┘                          │
└────────────────────┼────────────────────────────────────────────────┘
                     ↓
┌────────────────────────────────────────────────────────────────────────┐
│                      BUSINESS LOGIC LAYER                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌───────────┐ │
│  │  IRideRepo  │  │ IUserRepo    │  │ IChatRepo    │  │IReportRepo│ │
│  │ (Interface) │  │ (Interface)  │  │ (Interface)  │  │(Interface)│ │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘  └─────┬─────┘ │
│         │                 │                 │               │        │
│         ↓                 ↓                 ↓               ↓        │
│  ┌─────────────┐  ┌──────────────┐  ┌───────────────┐  ┌─────────┐ │
│  │ RideRepoImpl │  │UserRepoImpl   │  │ChatRepoImpl    │  │ReportRI │ │
│  │ (Impl)      │  │(Impl)        │  │(Impl)         │  │(Impl)   │ │
│  └──────┬──────┘  └──────┬───────┘  └─────┬─────────┘  └────┬────┘ │
│         │                │                 │                 │       │
└─────────┼────────────────┼─────────────────┼─────────────────┼──────┘
          │ @Inject        │                 │                 │
┌─────────┼────────────────┼─────────────────┼─────────────────┼──────┐
│         ↓                ↓                 ↓                 ↓      │
│  ┌──────────────────────────────────────────────────────┐            │
│  │          Dependency Injection (Hilt)                 │            │
│  │  ┌──────────────────┐  ┌──────────────────────────┐  │            │
│  │  │ FirebaseModule   │  │ RepositoryModule        │  │            │
│  │  │                  │  │                         │  │            │
│  │  │ • Firestore (PS) │  │ • Binds IRideRepo       │  │            │
│  │  │ • Auth           │  │ • Binds IUserRepo       │  │            │
│  │  │ • Storage        │  │ • Binds IChatRepo       │  │            │
│  │  └──────────────────┘  │ • Binds IReportRepo     │  │            │
│  │                        └──────────────────────────┘  │            │
│  └────────────────────────────────────────────────────┘            │
│         ↓                                                           │
└────────────────────────────────────────────────────────────────────┘
         │
┌────────┴─────────────────────────────────────────────────────────────┐
│                        DATA LAYER                                    │
│  ┌──────────────────┐              ┌──────────────────────────────┐  │
│  │  Cloud Firestore │  ←→ SYNC →   │   Room Database (Local)     │  │
│  │                  │              │                            │  │
│  │ Collections:     │              │ Tables:                    │  │
│  │ • rides          │              │ • rides                    │  │
│  │ • users          │              │ • messages                 │  │
│  │ • connections    │              │ • users                    │  │
│  │ • messages       │              │                            │  │
│  │ • reports        │              │ DAOs:                      │  │
│  │                  │              │ • RideDao                  │  │
│  │ Firestore Rules  │              │ • MessageDao               │  │
│  │ • Ban checks     │              │ • UserDao                  │  │
│  │ • Auth guards    │              │                            │  │
│  │ • Field validate │              │ (Offline cache + sync)     │  │
│  └──────────────────┘              └──────────────────────────┘  │
│         ↓                                        ↓                  │
│  ┌──────────────────┐                ┌──────────────────┐          │
│  │  Firebase Auth   │                │ SharedPreferences│          │
│  │  (Email/Password)│                │ (Settings, tokens)          │
│  └──────────────────┘                └──────────────────┘          │
└────────────────────────────────────────────────────────────────────┘
```

---

## 2. Data Flow: Loading Rides (Success & Error)

### Success Flow (Online)
```
Fragment
   ↓
click "Load Rides"
   ↓
ViewModel.loadRides(campusId, uid)
   ↓
IRideRepository.getRideFeed(campusId, uid, limit)
   ↓
Firestore: collection("rides")
           .whereEqualTo("campusId", campusId)
           .whereEqualTo("status", "ACTIVE")
   ↓
DocumentSnapshot[] received
   ↓
Convert to List<Ride>
   ↓
Cache to Room: database.rideDao().insertRides(entities)
   ↓
MutableLiveData.setValue(SafeResult.success(rides))
   ↓
Fragment observes: result.isSuccess() → displayRides(rides)
   ↓
✅ Rides displayed
```

### Error Flow (Network Failure → Fallback to Cache)
```
Fragment
   ↓
click "Load Rides"
   ↓
ViewModel.loadRides(campusId, uid)
   ↓
IRideRepository.getRideFeed(campusId, uid, limit)
   ↓
Firestore query listener.addSnapshotListener()
   ↓
❌ ERROR: Network unreachable
   ↓
Catch exception in onFailure()
   ↓
ERROR: Fallback to Room cache
   ↓
Thread (background):
  List<RideEntity> cached = database.rideDao()
    .getRideFeed(campusId, limit)
   ↓
MutableLiveData.postValue(SafeResult.cached(rides))
   ↓
Fragment observes: result.isCached() → 
  displayRides(rides) + showOfflineBadge()
   ↓
✅ Cached rides displayed with "Offline" indicator
```

### Retry Flow
```
User sees error + "RETRY" button
   ↓
Clicks RETRY
   ↓
ViewModel.retryLoadRidesFeed()
   ↓
Same as Success Flow above
   ↓
Either succeeds or shows error again
```

---

## 3. SafeResult<T> State Machine

```
                    ┌─────────────┐
                    │   Created   │
                    └──────┬──────┘
                           │
                    ┌──────▼──────┐
                    │   LOADING   │ ◄─────┐
                    └──────┬──────┘       │
                           │             │
                 ┌─────────┴─────────┐    │
                 ↓                   ↓    │
            ┌─────────┐         ┌─────────┐
            │ SUCCESS │         │  ERROR  │
            └─────────┘         └────┬────┘
                                     │
                         ┌───────────┴────────────┐
                         │                        │
                    canRetry=true            canRetry=false
                         │                        │
                         └────────────┬───────────┘
                                      ↓
                             (Terminal state)

Additional:
CACHED: Data from local Room database (offline)
        isOffline=true, canRetry=false
```

---

## 4. Dependency Injection Component Graph

```
┌──────────────────────────────────────────────────────┐
│              CampusTaxiApp                           │
│         (@HiltAndroidApp on Application)             │
└──────────────────────┬───────────────────────────────┘
                       │
         ┌─────────────┼─────────────┐
         │             │             │
    ┌────▼──┐    ┌────▼──┐    ┌────▼──┐
    │Module1│    │Module2│    │Module3│
    └────┬──┘    └────┬──┘    └────┬──┘
         │             │             │
    [Bindings from FirebaseModule, RepositoryModule, etc.]
         │             │             │
         └─────────────┼─────────────┘
                       ↓
         ┌─────────────────────────────────┐
         │  Dependency Graph (Singleton)   │
         │                                 │
         │  • FirebaseFirestore            │
         │  • FirebaseAuth                 │
         │  • FirebaseStorage              │
         │  • IRideRepository              │
         │  • IUserRepository              │
         │  • IChatRepository              │
         │  • IReportRepository            │
         │  • CampusTaxiDatabase           │
         └────────────┬────────────────────┘
                      │
         ┌────────────┴────────────┐
         ↓                         ↓
    @AndroidEntryPoint     @HiltViewModel
    Activity/Fragment      ViewModel
    
    @Inject IRideRepository repo;
    @Inject IUserRepository repo;
```

---

## 5. Offline-First with Room & Firestore

```
SCENARIO: User offline, posts ride

┌─────────────────────────────────────────────────────┐
│                    UI (Fragment)                    │
│  User fills form & clicks "Post Ride"               │
└─────────────────────┬───────────────────────────────┘
                      ↓
             ┌─────────────────┐
             │ ViewModel       │
             │.postRide(ride)  │
             └────────┬────────┘
                      ↓
       ┌──────────────────────────────┐
       │ Repository                   │
       │ .postRide(ride)              │
       │                              │
       │ Step 1: Save to Room locally │
       │   database.rideDao()         │
       │     .insertRide(entity)      │
       │   ✅ Immediate success       │
       │                              │
       │ Step 2: Try Firestore        │
       │   db.collection("rides")     │
       │     .add(ride)               │
       │   ❌ Network error!          │
       │                              │
       │ Step 3: Mark unsync in Room  │
       │   entity.syncedAt = null     │
       │   database.rideDao()         │
       │     .updateRide(entity)      │
       │                              │
       │ Return:                      │
       │   SafeResult.error(          │
       │     ex,                      │
       │     "Saved locally,          │
       │      will sync when online"  │
       │   )                          │
       └──────────────┬───────────────┘
                      ↓
          ┌────────────────────────┐
          │ Fragment               │
          │ Shows: "Saved locally" │
          │ But not on server yet  │
          └────────────────────────┘

LATER: User goes online

       SyncManager.syncOfflineWrites()
       ↓
       Get unsynced rides from Room
       ↓
       For each ride:
         - Upload to Firestore
         - Mark syncedAt = now
       ↓
       User sees: "Synced successfully!"
```

---

## 6. Firestore Rules Flow

```
User Action: Try to write ride

        User write request
                ↓
    ┌───────────────────────┐
    │ Is authenticated?     │
    └─────────┬─────────────┘
              ↓ (if no)
          ❌ PERMISSION_DENIED
    
    ┌───────────────────────────────────┐
    │ Is user banned?                   │
    │ get(/users/{uid}).isBanned == true│
    └─────────┬───────────────────────┘
              ↓ (if yes)
          ❌ PERMISSION_DENIED
              ("You're banned")
    
    ┌────────────────────────────────┐
    │ For rides: is valid ride data? │
    │ • seats >= 1                   │
    │ • fare >= 0                    │
    │ • request.auth.uid == postedBy │
    └─────────┬──────────────────────┘
              ↓ (if yes)
         ✅ PERMISSION_GRANTED
    
    ┌──────────────────────────────┐
    │ Update is atomic?            │
    │ Using runTransaction()?      │
    └──────────┬───────────────────┘
               ↓ (if yes)
    ✅ Transaction succeeds
       (Race condition prevented!)
```

---

## 7. Error Handling Flow

```
User Action
    ↓
Repository tries operation
    ↓
❌ Exception caught
    ↓
SafeResult.error(exception, userMessage)
    │
    ├─ Infer error code (TIMEOUT, PERMISSION_DENIED, etc.)
    ├─ Determine if retryable
    ├─ Detect if offline
    └─ Create user-friendly message
    ↓
LiveData.setValue(result)
    ↓
Fragment observes result.isError()
    ↓
ErrorHandler.showError(
    view,
    result.getUserMessage(),
    result.canRetry(),
    retry_callback
)
    ↓
    ├─ Show Snackbar with error message
    ├─ If retryable: Add "RETRY" button
    └─ If offline: Show offline badge
    ↓
User taps RETRY
    ↓
Call callback → retry operation
```

---

## 8. Transaction: Accept Seat Request

```
┌──────────────────────────────────────────────────────┐
│ User clicks "Accept Request" for ride with 2 seats   │
└──────────────────┬───────────────────────────────────┘
                   ↓
    ┌──────────────────────────────┐
    │ Repository.acceptSeatRequest │
    └──────────────┬───────────────┘
                   ↓
    db.runTransaction(transaction -> {
    
    // Step 1: READ latest ride state
    Ride rideSnap = transaction.get(rideRef)
    seatsRemaining = rideSnap.seatsRemaining  // = 2
    
    if (seatsRemaining <= 0) {
        ❌ FAIL: "No seats remaining"
    }
    
    // Step 2: UPDATE seat request
    transaction.update(requestRef, {
        status: "ACCEPTED",
        respondedAt: now
    })
    
    // Step 3: DECREMENT seats
    transaction.update(rideRef, {
        seatsRemaining: 1,  // 2-1
        updatedAt: now
    })
    
    // Step 4: CREATE connection doc
    transaction.set(connectionRef, {
        rideId, posterUid, joinerUid,
        participants: [posterUid, joinerUid],
        connectedAt: now
    })
    
    ✅ COMMIT (all or nothing)
    })
    
    ┌────────────────────────────────┐
    │ Result:                        │
    │ ✅ Request ACCEPTED            │
    │ ✅ Seats decremented to 1      │
    │ ✅ Connection created for chat │
    │ ✅ Race condition prevented!   │
    │    (Another user can't also    │
    │     accept while we read)      │
    └────────────────────────────────┘

RACE CONDITION PREVENTED:
Without transaction, two users could simultaneously
read seatsRemaining=2 and both think they can accept,
causing overbooking.

With transaction, only ONE succeeds; other gets:
"No seats remaining. Someone else accepted the last seat."
```

---

## 9. Request/Response Lifecycle

```
┌─────────────────────────────────────────────────────┐
│              User Opens App                         │
└────────────────┬────────────────────────────────────┘
                 ↓
     ┌──────────────────────┐
     │ Fragment.onViewCreated│
     │ viewModel =          │
     │   ViewModelProvider.get(VM.class)
     └────────┬─────────────┘
              ↓
     ┌────────────────────────────────┐
     │ ViewModel injected:            │
     │ @Inject IRideRepository repo   │
     │ via Hilt                       │
     └────────┬──────────────────────┘
              ↓
     ┌────────────────────────────────┐
     │ ViewModel.loadRides(...)       │
     │ repo.getRideFeed(...)          │
     │   .observeForever(result -> {})
     └────────┬──────────────────────┘
              ↓
   ┌──────────────────────────┐
   │ Repository creates       │
   │ MutableLiveData          │
   │ setValue(SafeResult.load)│
   └────────┬─────────────────┘
            ↓
   ┌──────────────────────────────────┐
   │ Fragment observes & displays:   │
   │ Spinner (is result.isLoading()) │
   └────────┬───────────────────────┘
            ↓
   ┌──────────────────────────────────┐
   │ Repository.addSnapshotListener() │
   │ Firestore query executes         │
   └────────┬───────────────────────┘
            ↓
   ┌──────────────────────────────────┐
   │ ✅ Snapshot received (or error)  │
   │ setValue(SafeResult.success(...))│
   └────────┬───────────────────────┘
            ↓
   ┌──────────────────────────────────┐
   │ Fragment observes & displays:   │
   │ List of rides                    │
   │ (or error dialog if error)       │
   └──────────────────────────────────┘

Listener remains active for real-time updates
(when others post new rides)
```

---

## 10. Memory & Lifecycle Management

```
┌─────────────────────────────────────────────────────┐
│  Fragment Created                                   │
│  @AndroidEntryPoint enables injection               │
└────────────────┬────────────────────────────────────┘
                 ↓
         Hilt creates Activity Scope
                 ↓
    ┌────────────────────────────────┐
    │ @Inject dependencies:          │
    │ IRideRepository → RideRepoImpl  │
    │ (Activity-scoped singleton)    │
    └────────┬──────────────────────┘
             ↓
   ┌──────────────────────────────────┐
   │ RideRepoImpl constructor          │
   │ activeListeners = new ArrayList()│
   │ repository.getRideFeed() adds    │
   │   ListenerRegistration           │
   │   to activeListeners             │
   └────────┬───────────────────────┘
            ↓
  ┌────────────────────────────────────┐
  │ Fragment uses ViewModel            │
  │ ViewModel holds repo reference     │
  │ ViewModel lives across rotations   │
  └────────┬──────────────────────────┘
           ↓
   ┌──────────────────────────────┐
   │ Fragment destroyed (rotation) │
   │ ViewModel retained            │
   │ Repository retained           │
   │ Listeners still active        │
   └────────┬─────────────────────┘
            ↓
   ┌──────────────────────────────┐
   │ Fragment recreated (rotation)│
   │ ViewModel reattached         │
   │ Listeners still active ✅    │
   └────────┬─────────────────────┘
            ↓
   ┌──────────────────────────────┐
   │ Activity destroyed           │
   │ Activity scope ends          │
   │ RideRepoImpl.cleanup()        │
   │   called automatically       │
   │ Listeners.remove()           │
   │ No memory leak! ✅           │
   └──────────────────────────────┘
```

---

## Summary

This architecture provides:
- ✅ **Separation of Concerns** (UI, Business Logic, Data)
- ✅ **Testability** (Interfaces allow mocking)
- ✅ **Offline Resilience** (Room fallback)
- ✅ **Type Safety** (SafeResult<T> wraps all results)
- ✅ **Memory Safety** (Listener cleanup, scope management)
- ✅ **Real-time Updates** (Firestore listeners)
- ✅ **Graceful Error Handling** (User-friendly messages)
- ✅ **Race Condition Prevention** (Firestore transactions)


