package com.princeraj.campustaxipooling;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.Observer;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.princeraj.campustaxipooling.db.CampusTaxiDatabase;
import com.princeraj.campustaxipooling.model.Ride;
import com.princeraj.campustaxipooling.repository.RideRepositoryImpl;
import com.princeraj.campustaxipooling.sync.SyncManager;
import com.princeraj.campustaxipooling.util.SafeResult;
import com.princeraj.campustaxipooling.util.AppExecutors;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class RideRepositoryTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock
    FirebaseFirestore db;
    @Mock
    CampusTaxiDatabase database;
    @Mock
    SyncManager syncManager;
    @Mock
    CollectionReference collectionReference;
    @Mock
    DocumentReference documentReference;
    @Mock
    Observer<SafeResult<String>> resultObserver;
    @Mock
    AppExecutors appExecutors;

    private RideRepositoryImpl repository;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        when(db.collection("rides")).thenReturn(collectionReference);
        when(collectionReference.document()).thenReturn(documentReference);
        when(documentReference.getId()).thenReturn("test_id");

        repository = new RideRepositoryImpl(db, database, syncManager, appExecutors);
    }

    @Test
    public void testPostRideInitialization() {
        Ride ride = new Ride("uid", "Name", "Campus", "Src", "Dst", null, 100, 4);
        
        // This test ensures the repository initializes the local write properly
        // without crashing—further logic would test LiveData observer callbacks.
        repository.postRide(ride).observeForever(resultObserver);
        
        assert(ride.getRideId().equals("test_id"));
    }
}
