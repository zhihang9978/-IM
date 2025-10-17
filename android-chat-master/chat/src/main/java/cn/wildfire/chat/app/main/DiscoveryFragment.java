/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.app.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import cn.wildfire.chat.kit.WfcIntent;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.WfcWebViewActivity;
import cn.wildfire.chat.kit.channel.ChannelListActivity;
import cn.wildfire.chat.kit.chatroom.ChatRoomListActivity;
import cn.wildfire.chat.kit.conversation.ConversationActivity;
import cn.wildfire.chat.kit.viewmodel.MessageViewModel;
import cn.wildfire.chat.kit.voip.conference.ConferencePortalActivity;
import cn.wildfire.chat.kit.widget.OptionItemView;
import cn.wildfirechat.avenginekit.AVEngineKit;
import cn.wildfirechat.chat.BuildConfig;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.core.MessageStatus;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.remote.ChatManager;

public class DiscoveryFragment extends Fragment {
    OptionItemView momentOptionItemView;
    OptionItemView conferenceOptionItemView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.main_fragment_discovery, container, false);
        bindViews(view);
        bindEvents(view);
        initMoment();
        if (!AVEngineKit.isSupportConference()) {
            conferenceOptionItemView.setVisibility(View.GONE);
        }
        return view;
    }

    private void bindEvents(View view) {
        view.findViewById(R.id.chatRoomOptionItemView).setOnClickListener(v -> chatRoom());
        view.findViewById(R.id.robotOptionItemView).setOnClickListener(v -> robot());
        view.findViewById(R.id.channelOptionItemView).setOnClickListener(v -> channel());
        view.findViewById(R.id.cookbookOptionItemView).setOnClickListener(v -> cookbook());
        view.findViewById(R.id.momentOptionItemView).setOnClickListener(v -> moment());
        view.findViewById(R.id.conferenceOptionItemView).setOnClickListener(v -> conference());
    }

    private void bindViews(View view) {
        momentOptionItemView = view.findViewById(R.id.momentOptionItemView);
        conferenceOptionItemView = view.findViewById(R.id.conferenceOptionItemView);
    }

    private void updateMomentBadgeView() {
        List<Message> messages = ChatManager.Instance().getMessagesEx2(Collections.singletonList(Conversation.ConversationType.Single), Collections.singletonList(1), Arrays.asList(MessageStatus.Unread), 0, true, 100, null);
        int count = messages == null ? 0 : messages.size();
        momentOptionItemView.setBadgeCount(count);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (WfcUIKit.getWfcUIKit().isSupportMoment()) {
            updateMomentBadgeView();
        }
    }

    void chatRoom() {
        Intent intent = new Intent(getActivity(), ChatRoomListActivity.class);
        startActivity(intent);
    }

    void robot() {
        Intent intent = ConversationActivity.buildConversationIntent(getActivity(), Conversation.ConversationType.Single, "FireRobot", 0);
        startActivity(intent);
    }

    void channel() {
        Intent intent = new Intent(getActivity(), ChannelListActivity.class);
        startActivity(intent);
    }

    void cookbook() {
        if (BuildConfig.APPLICATION_ID.startsWith("cn.wildfirechat.")) {
        WfcWebViewActivity.loadUrl(getContext(), getString(R.string.wfc_doc_title), getString(R.string.wfc_doc_url));
        } else {
            Toast.makeText(getContext(), "野火IM 开发文档对第三方应用不适用", Toast.LENGTH_SHORT).show();
        }
    }

    private void initMoment() {
        if (!WfcUIKit.getWfcUIKit().isSupportMoment()) {
            momentOptionItemView.setVisibility(View.GONE);
            return;
        }
        MessageViewModel messageViewModel =new ViewModelProvider(this).get(MessageViewModel.class);
        messageViewModel.messageLiveData().observe(getViewLifecycleOwner(), uiMessage -> updateMomentBadgeView());
        messageViewModel.clearMessageLiveData().observe(getViewLifecycleOwner(), o -> updateMomentBadgeView());
    }

    void moment() {
        Intent intent = new Intent(WfcIntent.ACTION_MOMENT);
        // 具体项目中，如果不能隐式启动，可改为下面这种显示启动朋友圈页面
//        Intent intent = new Intent(getActivity(), FeedListActivity.class);
        startActivity(intent);
    }

    void conference() {
        Intent intent = new Intent(getActivity(), ConferencePortalActivity.class);
        startActivity(intent);
    }

}
