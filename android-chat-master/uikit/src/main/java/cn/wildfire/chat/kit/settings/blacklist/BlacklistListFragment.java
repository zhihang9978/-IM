/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.settings.blacklist;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.GeneralCallback;


public class BlacklistListFragment extends Fragment implements BlacklistListAdapter.OnBlacklistItemClickListener, PopupMenu.OnMenuItemClickListener {
    RecyclerView recyclerView;
    private BlacklistViewModel blacklistViewModel;
    private BlacklistListAdapter blacklistListAdapter;

    private String selectedUserId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.blacklist_list_frament, container, false);
        bindViews(view);
        init();
        return view;
    }

    private void bindViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerView);
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshBlacklist();
    }

    private void init() {
        blacklistViewModel = new ViewModelProvider(getActivity()).get(BlacklistViewModel.class);

        blacklistListAdapter = new BlacklistListAdapter();
        blacklistListAdapter.setOnBlacklistItemClickListener(this);

        recyclerView.setAdapter(blacklistListAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
    }

    private void refreshBlacklist() {
        List<String> blacklists = blacklistViewModel.getBlacklists();
        blacklistListAdapter.setBlackedUserIds(blacklists);
        blacklistListAdapter.notifyDataSetChanged();
    }

    @Override
    public void onItemClick(String userId, View v) {

        PopupMenu popup = new PopupMenu(getActivity(), v);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.blacklist_popup, popup.getMenu());
        popup.setOnMenuItemClickListener(this);
        popup.show();
        selectedUserId = userId;

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if (item.getItemId() == R.id.remove) {
            ChatManager.Instance().setBlackList(selectedUserId, false, new GeneralCallback() {
                @Override
                public void onSuccess() {
                    blacklistListAdapter.getBlackedUserIds().remove(selectedUserId);
                    blacklistListAdapter.notifyDataSetChanged();
                }

                @Override
                public void onFail(int errorCode) {
                    Toast.makeText(getActivity(), getActivity().getString(R.string.blacklist_remove_failed), Toast.LENGTH_SHORT).show();
                }
            });
            return true;
        }
        return false;
    }
}
