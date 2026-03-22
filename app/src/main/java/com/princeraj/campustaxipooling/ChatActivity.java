package com.princeraj.campustaxipooling;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.princeraj.campustaxipooling.model.Message;
import com.princeraj.campustaxipooling.repository.ChatRepository;
import com.princeraj.campustaxipooling.ui.chat.MessageAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Real-time chat screen between two ride-pool partners.
 *
 * Architecture:
 *  - Firestore real-time listener for messages (addSnapshotListener)
 *  - Client-side moderation runs BEFORE every Firestore write
 *  - Blocked messages are stored with isBlocked=true (admin-visible, not shown to recipient)
 */
public class ChatActivity extends AppCompatActivity {

    private RecyclerView messagesRecyclerView;
    private TextInputEditText messageEt;
    private FloatingActionButton sendBtn;
    private TextView partnerInitialTv, partnerNameTv, rideRouteHeaderTv;

    private MessageAdapter messageAdapter;
    private final List<Message> messages = new ArrayList<>();

    private final ChatRepository chatRepo = ChatRepository.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private String connectionId;
    private String rideId;
    private String currentUid;
    private String currentUserName = "User";

    private ListenerRegistration messageListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        connectionId = getIntent().getStringExtra("connectionId");
        rideId = getIntent().getStringExtra("rideId");

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (connectionId == null || user == null) {
            finish();
            return;
        }
        currentUid = user.getUid();

        bindViews();
        setupRecyclerView();
        loadChatHeader();
        attachMessageListener();
        loadCurrentUserName();

        sendBtn.setOnClickListener(v -> sendMessage());

        // Also send on keyboard "Send" action
        messageEt.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                sendMessage();
                return true;
            }
            return false;
        });
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    private void bindViews() {
        messagesRecyclerView = findViewById(R.id.messagesRecyclerView);
        messageEt = findViewById(R.id.messageEt);
        sendBtn = findViewById(R.id.sendBtn);
        partnerInitialTv = findViewById(R.id.partnerInitial);
        partnerNameTv = findViewById(R.id.partnerName);
        rideRouteHeaderTv = findViewById(R.id.rideRouteHeader);

        ImageView backBtn = findViewById(R.id.backBtn);
        backBtn.setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        messageAdapter = new MessageAdapter(messages, currentUid);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);      // Messages start from bottom
        messagesRecyclerView.setLayoutManager(layoutManager);
        messagesRecyclerView.setAdapter(messageAdapter);
    }

    private void loadChatHeader() {
        // Load ride details for the header subtitle
        if (rideId != null) {
            db.collection("rides").document(rideId).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            String src = doc.getString("source");
                            String dst = doc.getString("destination");
                            if (src != null && dst != null) {
                                rideRouteHeaderTv.setText(src + " → " + dst);
                            }
                        }
                    });
        }

        // Load connection to find partner name
        db.collection("connections").document(connectionId).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;
                    String posterUid = doc.getString("posterUid");
                    String joinerUid = doc.getString("joinerUid");
                    String partnerUid = currentUid.equals(posterUid) ? joinerUid : posterUid;

                    if (partnerUid != null) {
                        db.collection("users").document(partnerUid).get()
                                .addOnSuccessListener(userDoc -> {
                                    String name = userDoc.getString("name");
                                    if (name == null) name = "Partner";
                                    partnerNameTv.setText(name);
                                    partnerInitialTv.setText(
                                            String.valueOf(name.charAt(0)).toUpperCase());
                                });
                    }
                });
    }

    private void loadCurrentUserName() {
        db.collection("users").document(currentUid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        currentUserName = doc.getString("name");
                        if (currentUserName == null) currentUserName = "User";
                    }
                });
    }

    // ── Real-time listener ────────────────────────────────────────────────────

    private void attachMessageListener() {
        messageListener = chatRepo.getMessagesQuery(connectionId)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) return;

                    messages.clear();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        Message msg = doc.toObject(Message.class);
                        if (msg != null && !msg.isBlocked()) {
                            // Only show non-blocked messages to the recipient
                            messages.add(msg);
                        }
                    }

                    messageAdapter.notifyDataSetChanged();
                    // Scroll to latest message
                    if (!messages.isEmpty()) {
                        messagesRecyclerView.smoothScrollToPosition(messages.size() - 1);
                    }
                });
    }

    // ── Send Message ──────────────────────────────────────────────────────────

    private void sendMessage() {
        String text = messageEt.getText() != null
                ? messageEt.getText().toString().trim() : "";

        if (TextUtils.isEmpty(text)) return;

        // ── Step 1: Client-side moderation (synchronous, instant) ──────────
        ChatRepository.ModerationResult result = chatRepo.moderateMessage(text);

        if (!result.isClean) {
            // Message is flagged — warn user, do NOT send to partner
            Snackbar.make(sendBtn,
                    "⚠️ " + result.userMessage,
                    Snackbar.LENGTH_LONG).show();

            // Still write flagged version for admin audit (isBlocked = true)
            chatRepo.sendFlaggedMessage(connectionId, currentUid, text, result.flagReason);

            // Clear the field
            messageEt.setText("");
            return;
        }

        // ── Step 2: Message is clean — write to Firestore ──────────────────
        Message message = new Message(currentUid, text);
        messageEt.setText("");      // Clear immediately for responsive UX

        chatRepo.sendMessage(connectionId, message, currentUserName)
                .addOnFailureListener(e ->
                        Snackbar.make(sendBtn,
                                "Failed to send. Check your connection.",
                                Snackbar.LENGTH_SHORT).show());
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    protected void onStop() {
        super.onStop();
        // Detach listener when activity is not visible
        if (messageListener != null) {
            messageListener.remove();
            messageListener = null;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Re-attach if activity comes back
        if (messageListener == null && connectionId != null) {
            attachMessageListener();
        }
    }
}
