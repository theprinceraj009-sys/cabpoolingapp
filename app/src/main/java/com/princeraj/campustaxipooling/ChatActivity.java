package com.princeraj.campustaxipooling;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.content.Intent;
import android.net.Uri;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.princeraj.campustaxipooling.model.Message;
import com.princeraj.campustaxipooling.repository.IChatRepository;
import com.princeraj.campustaxipooling.repository.IRideRepository;
import com.princeraj.campustaxipooling.repository.IUserRepository;
import com.princeraj.campustaxipooling.ui.chat.MessageAdapter;
import com.princeraj.campustaxipooling.ui.chat.RatingDialogFragment;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import dagger.hilt.android.AndroidEntryPoint;

/**
 * Real-time chat screen between two ride-pool partners.
 *
 * Architecture:
 *  - Firestore real-time listener for messages (addSnapshotListener)
 *  - Client-side moderation runs BEFORE every Firestore write
 *  - Blocked messages are stored with isBlocked=true (admin-visible, not shown to recipient)
 */
@AndroidEntryPoint
public class ChatActivity extends BaseActivity {

    private RecyclerView messagesRecyclerView;
    private EditText messageEt;
    private FloatingActionButton sendBtn;
    private TextView partnerInitialTv, partnerNameTv, rideRouteHeaderTv;
    private ImageView callBtn, rateBtn;

    private MessageAdapter messageAdapter;
    private final List<Message> messages = new ArrayList<>();

    @Inject
    IChatRepository chatRepo;
    
    @Inject
    IUserRepository userRepo;
    
    @Inject
    IRideRepository rideRepo;

    private String connectionId;
    private String rideId;
    private String currentUid;
    private String currentUserName = "User";
    
    // Store partner info for rating
    private String partnerUid;
    private String partnerName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        connectionId = getIntent().getStringExtra("connectionId");
        rideId = getIntent().getStringExtra("rideId");

        com.google.firebase.auth.FirebaseUser user = userRepo.getCurrentFirebaseUser();
        if (connectionId == null || user == null) {
            finish();
            return;
        }
        currentUid = user.getUid();

        bindViews();
        setupRecyclerView();
        loadChatHeader();
        observeMessages();
        loadCurrentUserName();

        sendBtn.setOnClickListener(v -> sendMessage());
        
        rateBtn.setOnClickListener(v -> {
            if (partnerUid != null && partnerName != null) {
                RatingDialogFragment.newInstance(rideId, currentUid, partnerUid, partnerName)
                        .show(getSupportFragmentManager(), "rating");
            }
        });

        messageEt.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                sendMessage();
                return true;
            }
            return false;
        });
    }

    private void bindViews() {
        messagesRecyclerView = findViewById(R.id.messagesRecyclerView);
        messageEt = findViewById(R.id.messageEt);
        sendBtn = findViewById(R.id.sendBtn);
        partnerInitialTv = findViewById(R.id.partnerInitial);
        partnerNameTv = findViewById(R.id.partnerName);
        rideRouteHeaderTv = findViewById(R.id.rideRouteHeader);
        callBtn = findViewById(R.id.callBtn);
        rateBtn = findViewById(R.id.rateBtn);

        ImageView backBtn = findViewById(R.id.backBtn);
        backBtn.setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        messageAdapter = new MessageAdapter(messages, currentUid);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        messagesRecyclerView.setLayoutManager(layoutManager);
        messagesRecyclerView.setAdapter(messageAdapter);
    }

    private void loadChatHeader() {
        if (rideId != null) {
            rideRepo.getRideById(rideId).observe(this, result -> {
                if (result.isSuccess() && result.getData() != null) {
                    com.princeraj.campustaxipooling.model.Ride ride = result.getData();
                    rideRouteHeaderTv.setText(ride.getSource() + " → " + ride.getDestination());
                }
            });
        }

        rideRepo.getConnection(connectionId).observe(this, result -> {
            if (result.isSuccess() && result.getData() != null) {
                com.princeraj.campustaxipooling.model.Connection conn = result.getData();
                partnerUid = currentUid.equals(conn.getPosterUid()) ? conn.getJoinerUid() : conn.getPosterUid();

                if (partnerUid != null) {
                    userRepo.getUserProfile(partnerUid).observe(this, userResult -> {
                        if (userResult.isSuccess() && userResult.getData() != null) {
                            com.princeraj.campustaxipooling.model.User partner = userResult.getData();
                            partnerName = partner.getName();
                            partnerNameTv.setText(partnerName);
                            partnerInitialTv.setText(String.valueOf(partnerName.charAt(0)).toUpperCase());

                            if (partner.getPhoneNumber() != null && partner.isPhoneVisibleToMatches()) {
                                callBtn.setVisibility(View.VISIBLE);
                                callBtn.setOnClickListener(v -> {
                                    Intent intent = new Intent(Intent.ACTION_DIAL);
                                    intent.setData(Uri.parse("tel:" + partner.getPhoneNumber()));
                                    startActivity(intent);
                                });
                            } else {
                                callBtn.setVisibility(View.GONE);
                            }
                        }
                    });
                }
            }
        });
    }

    private void loadCurrentUserName() {
        userRepo.getUserProfile(currentUid).observe(this, result -> {
            if (result.isSuccess() && result.getData() != null) {
                currentUserName = result.getData().getName();
            }
        });
    }

    private void observeMessages() {
        chatRepo.getMessages(connectionId).observe(this, result -> {
            if (result.isSuccess() && result.getData() != null) {
                messages.clear();
                messages.addAll(result.getData());
                messageAdapter.notifyDataSetChanged();
                if (!messages.isEmpty()) {
                    messagesRecyclerView.smoothScrollToPosition(messages.size() - 1);
                }
            } else if (result.isError()) {
                Snackbar.make(sendBtn, "Error sync messages: " + result.getMessage(), Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    private void sendMessage() {
        String text = messageEt.getText() != null ? messageEt.getText().toString().trim() : "";
        if (TextUtils.isEmpty(text)) return;

        com.princeraj.campustaxipooling.repository.IChatRepository.ModerationResult mod = chatRepo.moderateMessage(text);
        if (!mod.isClean) {
            Snackbar.make(sendBtn, "⚠️ " + mod.userMessage, Snackbar.LENGTH_LONG).show();
            chatRepo.sendFlaggedMessage(connectionId, currentUid, text, mod.flagReason);
            messageEt.setText("");
            return;
        }

        Message message = new Message(currentUid, text);
        messageEt.setText("");

        chatRepo.sendMessage(connectionId, message, currentUserName).observe(this, result -> {
            if (result.isError()) {
                Snackbar.make(sendBtn, "Failed to send: " + result.getMessage(), Snackbar.LENGTH_SHORT).show();
            }
        });
    }
}
