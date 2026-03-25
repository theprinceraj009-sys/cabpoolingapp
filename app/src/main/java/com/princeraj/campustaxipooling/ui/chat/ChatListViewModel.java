package com.princeraj.campustaxipooling.ui.chat;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.princeraj.campustaxipooling.model.Connection;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class ChatListViewModel extends ViewModel {

    private final com.princeraj.campustaxipooling.repository.IRideRepository rideRepo;
    private final com.princeraj.campustaxipooling.repository.IUserRepository userRepo;

    private final MutableLiveData<List<Connection>> connectionsLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(true);
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();

    @Inject
    public ChatListViewModel(com.princeraj.campustaxipooling.repository.IRideRepository rideRepo,
                             com.princeraj.campustaxipooling.repository.IUserRepository userRepo) {
        this.rideRepo = rideRepo;
        this.userRepo = userRepo;
    }

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
        com.google.firebase.auth.FirebaseUser user = userRepo.getCurrentFirebaseUser();
        String uid = user != null ? user.getUid() : "";

        if (uid.isEmpty()) {
            errorLiveData.setValue("User not logged in");
            isLoading.setValue(false);
            return;
        }

        isLoading.setValue(true);
        // Phase 3: Observe connections from repository (Room -> Firestore)
        rideRepo.getMyConnections(uid).observeForever(result -> {
            if (result.isLoading() && (connectionsLiveData.getValue() == null || connectionsLiveData.getValue().isEmpty())) {
                return;
            }

            isLoading.setValue(false);
            if (result.isError()) {
                errorLiveData.setValue(result.getMessage());
                return;
            }

            if (result.getData() != null) {
                // Group by partner to avoid showing multiple chats for the same person
                java.util.Map<String, Connection> uniquePartners = new java.util.HashMap<>();
                String myUid = uid;

                for (Connection conn : result.getData()) {
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

                // Sort locally by connectedAt (DESCENDING) - API 21 compatible
                java.util.Collections.sort(currentConnections, (c1, c2) -> {
                    if (c1.getConnectedAt() == null || c2.getConnectedAt() == null) return 0;
                    return c2.getConnectedAt().compareTo(c1.getConnectedAt());
                });

                connectionsLiveData.setValue(currentConnections);
            }
        });
    }
}
