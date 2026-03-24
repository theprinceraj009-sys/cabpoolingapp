package com.princeraj.campustaxipooling.ui.chat;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.princeraj.campustaxipooling.model.Connection;
import com.princeraj.campustaxipooling.repository.RideRepository;

import java.util.ArrayList;
import java.util.List;

public class ChatListViewModel extends ViewModel {

    private final RideRepository rideRepo = RideRepository.getInstance();
    private ListenerRegistration connectionsListener;

    private final MutableLiveData<List<Connection>> connectionsLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(true);
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();

    public LiveData<List<Connection>> getConnections() {
        return connectionsLiveData;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getError() {
        return errorLiveData;
    }

    public void loadConnections() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";

        if (uid.isEmpty()) {
            errorLiveData.setValue("User not logged in");
            isLoading.setValue(false);
            return;
        }

        if (connectionsListener != null) {
            connectionsListener.remove();
        }

        isLoading.setValue(true);
        connectionsListener = rideRepo.getMyConnections(uid)
                .addSnapshotListener((snapshots, error) -> {
                    isLoading.setValue(false);
                    if (error != null) {
                        errorLiveData.setValue(error.getMessage());
                        return;
                    }

                    if (snapshots == null) return;

                    // Group by partner to avoid showing multiple chats for the same person
                    java.util.Map<String, Connection> uniquePartners = new java.util.HashMap<>();
                    String myUid = FirebaseAuth.getInstance().getUid();

                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        Connection conn = doc.toObject(Connection.class);
                        if (conn != null && conn.isActive()) {
                            String partnerUid = myUid.equals(conn.getPosterUid()) 
                                    ? conn.getJoinerUid() : conn.getPosterUid();
                            
                            if (partnerUid == null) continue;

                            // Keep the latest connection for this partner
                            if (!uniquePartners.containsKey(partnerUid) || 
                                (conn.getConnectedAt() != null && 
                                 uniquePartners.get(partnerUid).getConnectedAt() != null &&
                                 conn.getConnectedAt().compareTo(uniquePartners.get(partnerUid).getConnectedAt()) > 0)) {
                                uniquePartners.put(partnerUid, conn);
                            }
                        }
                    }

                    List<Connection> currentConnections = new ArrayList<>(uniquePartners.values());

                    // Sort locally by connectedAt (DESCENDING)
                    currentConnections.sort((c1, c2) -> {
                        if (c1.getConnectedAt() == null || c2.getConnectedAt() == null) return 0;
                        return c2.getConnectedAt().compareTo(c1.getConnectedAt());
                    });

                    connectionsLiveData.setValue(currentConnections);
                });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (connectionsListener != null) {
            connectionsListener.remove();
            connectionsListener = null;
        }
    }
}
