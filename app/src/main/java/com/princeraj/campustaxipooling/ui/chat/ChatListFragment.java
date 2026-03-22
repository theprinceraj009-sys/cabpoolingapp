package com.princeraj.campustaxipooling.ui.chat;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.princeraj.campustaxipooling.ChatActivity;
import com.princeraj.campustaxipooling.R;
import com.princeraj.campustaxipooling.model.Connection;
import com.princeraj.campustaxipooling.repository.RideRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Shows all active connections the user can chat in.
 * Powered by a real-time Firestore listener.
 */
public class ChatListFragment extends Fragment {

    private RecyclerView connectionsRecyclerView;
    private LinearLayout emptyStateView;

    private ChatListAdapter adapter;
    private final List<Connection> connections = new ArrayList<>();

    private ChatListViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        connectionsRecyclerView = view.findViewById(R.id.connectionsRecyclerView);
        emptyStateView = view.findViewById(R.id.emptyStateView);

        setupRecyclerView();

        viewModel = new ViewModelProvider(this).get(ChatListViewModel.class);
        setupObservers();

        if (savedInstanceState == null) {
            viewModel.loadConnections();
        }
    }

    private void setupRecyclerView() {
        adapter = new ChatListAdapter(connections, connection -> {
            Intent intent = new Intent(requireContext(), ChatActivity.class);
            intent.putExtra("connectionId", connection.getConnectionId());
            intent.putExtra("rideId", connection.getRideId());
            startActivity(intent);
        });
        connectionsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        connectionsRecyclerView.setAdapter(adapter);
    }

    private void setupObservers() {
        viewModel.getConnections().observe(getViewLifecycleOwner(), newConnections -> {
            connections.clear();
            if (newConnections != null) {
                connections.addAll(newConnections);
            }
            adapter.notifyDataSetChanged();

            boolean empty = connections.isEmpty();
            connectionsRecyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
            emptyStateView.setVisibility(empty ? View.VISIBLE : View.GONE);
        });
    }
}
