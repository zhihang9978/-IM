/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversationlist.viewholder;

import android.content.Context;
import android.content.Intent;
import android.text.style.ImageSpan;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.lqr.emoji.MoonUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.annotation.ConversationContextMenuItem;
import cn.wildfire.chat.kit.conversation.ConversationActivity;
import cn.wildfire.chat.kit.conversation.ConversationViewModel;
import cn.wildfire.chat.kit.conversation.Draft;
import cn.wildfire.chat.kit.conversationlist.ConversationListViewModel;
import cn.wildfire.chat.kit.conversationlist.ConversationListViewModelFactory;
import cn.wildfire.chat.kit.group.GroupViewModel;
import cn.wildfire.chat.kit.third.utils.TimeUtils;
import cn.wildfire.chat.kit.third.utils.UIUtils;
import cn.wildfire.chat.kit.utils.WfcTextUtils;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.core.MessageDirection;
import cn.wildfirechat.message.core.MessageStatus;
import cn.wildfirechat.message.notification.NotificationMessageContent;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.ConversationInfo;
import cn.wildfirechat.remote.ChatManager;

@SuppressWarnings("unused")
public abstract class ConversationViewHolder extends RecyclerView.ViewHolder {
    protected Fragment fragment;
    protected View itemView;
    protected ConversationInfo conversationInfo;
    protected RecyclerView.Adapter adapter;
    protected ConversationListViewModel conversationListViewModel;
    private ConversationViewModel conversationViewModel;

    protected TextView nameTextView;
    protected TextView timeTextView;
    protected ImageView portraitImageView;
    protected ImageView silentImageView;
    protected TextView unreadCountTextView;
    protected View redDotView;
    protected TextView contentTextView;
    protected TextView promptTextView;

    protected ImageView statusImageView;

    protected ImageView secretChatIndicator;
    protected GroupViewModel groupViewModel;

    protected final CenterCrop centerCropTransformation = new CenterCrop();
    protected RoundedCorners roundedCornerTransformation = null;

    private LiveData<String> groupMemberDisplayNameLiveData;
    private Observer<String> groupMemberDisplayNameObserver;

    public ConversationViewHolder(Fragment fragment, RecyclerView.Adapter adapter, View itemView) {
        super(itemView);
        this.fragment = fragment;
        this.itemView = itemView;
        this.adapter = adapter;
        bindViews(itemView);
        conversationListViewModel = new ViewModelProvider(fragment, new ConversationListViewModelFactory(Arrays.asList(Conversation.ConversationType.Single, Conversation.ConversationType.Group, Conversation.ConversationType.SecretChat, Conversation.ConversationType.ChatRoom), Arrays.asList(0)))
            .get(ConversationListViewModel.class);
        conversationViewModel = new ViewModelProvider(fragment).get(ConversationViewModel.class);
    }

    private void bindViews(View itemView) {
        nameTextView = itemView.findViewById(R.id.nameTextView);
        timeTextView = itemView.findViewById(R.id.timeTextView);
        portraitImageView = itemView.findViewById(R.id.portraitImageView);
        silentImageView = itemView.findViewById(R.id.slient);
        unreadCountTextView = itemView.findViewById(R.id.unreadCountTextView);
        redDotView = itemView.findViewById(R.id.redDotView);
        contentTextView = itemView.findViewById(R.id.contentTextView);
        promptTextView = itemView.findViewById(R.id.promptTextView);
        statusImageView = itemView.findViewById(R.id.statusImageView);
        secretChatIndicator = itemView.findViewById(R.id.secretChatIndicator);
    }

    final public void onBind(ConversationInfo conversationInfo, int position) {
        this.conversationInfo = conversationInfo;
        onBind(conversationInfo);
    }

    /**
     * 设置头像、名称
     *
     * @param conversationInfo
     */
    protected abstract void onBindConversationInfo(ConversationInfo conversationInfo);

    public void onBind(ConversationInfo conversationInfo) {
        secretChatIndicator.setVisibility(View.GONE);
        onBindConversationInfo(conversationInfo);

        timeTextView.setText(TimeUtils.getMsgFormatTime(conversationInfo.timestamp));
        silentImageView.setVisibility(conversationInfo.isSilent ? View.VISIBLE : View.GONE);
        statusImageView.setVisibility(View.GONE);

        if (roundedCornerTransformation == null) {
            roundedCornerTransformation = new RoundedCorners(UIUtils.dip2Px(fragment.getContext(), 4));
        }

        itemView.setBackgroundResource(conversationInfo.top > 0 ? R.drawable.selector_stick_top_item : R.drawable.selector_common_item);
        redDotView.setVisibility(View.GONE);
        if (conversationInfo.isSilent) {
            if (conversationInfo.unreadCount.unread > 0) { // 显示红点
                unreadCountTextView.setText("");
                unreadCountTextView.setVisibility(View.GONE);
                redDotView.setVisibility(View.VISIBLE);
            } else {
                unreadCountTextView.setVisibility(View.GONE);
            }
        } else {
            if (conversationInfo.unreadCount.unread > 0) {
                unreadCountTextView.setVisibility(View.VISIBLE);
                unreadCountTextView.setText(conversationInfo.unreadCount.unread > 99 ? "99+" : conversationInfo.unreadCount.unread + "");
            } else {
                unreadCountTextView.setVisibility(View.GONE);
            }
        }


        Draft draft = Draft.fromDraftJson(conversationInfo.draft);
        if (draft != null) {
            String draftString = draft.getContent() != null ? draft.getContent() : fragment.getString(R.string.draft);
            MoonUtils.identifyFaceExpression(fragment.getActivity(), contentTextView, draft.getContent(), ImageSpan.ALIGN_BOTTOM);
            setViewVisibility(R.id.promptTextView, View.VISIBLE);
            setViewVisibility(R.id.contentTextView, View.VISIBLE);
        } else {
            if (conversationInfo.unreadCount.unreadMentionAll > 0 || conversationInfo.unreadCount.unreadMention > 0) {
                promptTextView.setText(fragment.getString(R.string.mention_you));
                promptTextView.setVisibility(View.VISIBLE);
            } else {
                promptTextView.setVisibility(View.GONE);
            }
            setViewVisibility(R.id.contentTextView, View.VISIBLE);
            if (conversationInfo.lastMessage != null && conversationInfo.lastMessage.content != null) {
                Message lastMessage = conversationInfo.lastMessage;
                // the message maybe invalid
                try {
                    if (conversationInfo.conversation.type == Conversation.ConversationType.Group
                        && lastMessage.direction == MessageDirection.Receive
                        && !(lastMessage.content instanceof NotificationMessageContent)) {
                        if (groupViewModel == null) {
                            groupViewModel = new ViewModelProvider(fragment).get(GroupViewModel.class);
                        }
                        groupMemberDisplayNameLiveData = groupViewModel.getGroupMemberDisplayNameAsync(conversationInfo.conversation.target, conversationInfo.lastMessage.sender);
                        groupMemberDisplayNameObserver = senderDisplayName -> {
                            String content = senderDisplayName + ":" + lastMessage.digest();
                            content = WfcTextUtils.htmlToText(content);
                            MoonUtils.identifyFaceExpression(fragment.getActivity(), contentTextView, content, ImageSpan.ALIGN_BOTTOM);
                        };
                        groupMemberDisplayNameLiveData.observe(fragment, groupMemberDisplayNameObserver);
                    } else {
                        String content = lastMessage.digest();
                        content = WfcTextUtils.htmlToText(content);
                        MoonUtils.identifyFaceExpression(fragment.getActivity(), contentTextView, content, ImageSpan.ALIGN_BOTTOM);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                switch (lastMessage.status) {
                    case Sending:
                        statusImageView.setVisibility(View.VISIBLE);
                        // TODO update sending image resource
                        statusImageView.setImageResource(R.mipmap.ic_sending);
                        break;
                    case Send_Failure:
                        statusImageView.setVisibility(View.VISIBLE);
                        statusImageView.setImageResource(R.mipmap.img_error);
                        break;
                    default:
                        statusImageView.setVisibility(View.GONE);
                        break;
                }

            } else {
                contentTextView.setText("");
            }
        }
    }

    public void removeLiveDataObserver() {
        if (groupMemberDisplayNameLiveData != null && groupMemberDisplayNameObserver != null) {
            groupMemberDisplayNameLiveData.removeObserver(groupMemberDisplayNameObserver);
            groupMemberDisplayNameObserver = null;
        }
    }

    public void onClick(View itemView) {
        Intent intent = new Intent(fragment.getActivity(), ConversationActivity.class);
        intent.putExtra("conversation", conversationInfo.conversation);
        fragment.startActivity(intent);
    }

    @ConversationContextMenuItem(tag = ConversationContextMenuItemTags.TAG_REMOVE,
        confirm = true,
        priority = 0)
    public void removeConversation(View itemView, ConversationInfo conversationInfo) {
        conversationListViewModel.removeConversation(conversationInfo, true);
    }

    @ConversationContextMenuItem(tag = ConversationContextMenuItemTags.TAG_TOP, priority = 1)
    public void stickConversationTop(View itemView, ConversationInfo conversationInfo) {
        conversationListViewModel.setConversationTop(conversationInfo, 1);
    }

    @ConversationContextMenuItem(tag = ConversationContextMenuItemTags.TAG_CANCEL_TOP, priority = 2)
    public void cancelStickConversationTop(View itemView, ConversationInfo conversationInfo) {
        conversationListViewModel.setConversationTop(conversationInfo, 0);
    }

    @ConversationContextMenuItem(tag = ConversationContextMenuItemTags.TAG_MarkAsRead, priority = 3)
    public void clearConversationUnread(View itemView, ConversationInfo conversationInfo) {
        conversationListViewModel.clearConversationUnread(conversationInfo);
    }

    @ConversationContextMenuItem(tag = ConversationContextMenuItemTags.TAG_MarkAsUnread, priority = 2)
    public void markConversationUnread(View itemView, ConversationInfo conversationInfo) {
        conversationListViewModel.markConversationUnread(conversationInfo);
    }

    /**
     * 长按触发的context menu的标题
     *
     * @param tag
     * @return
     */
    public String contextMenuTitle(Context context, String tag) {
        String title;
        switch (tag) {
            case ConversationContextMenuItemTags.TAG_REMOVE:
                title = context.getString(R.string.delete_conversation);
                break;
            case ConversationContextMenuItemTags.TAG_TOP:
                title = context.getString(R.string.stick_on_top);
                break;
            case ConversationContextMenuItemTags.TAG_CANCEL_TOP:
                title = context.getString(R.string.cancel_stick_on_top);
                break;
            case ConversationContextMenuItemTags.TAG_MarkAsRead:
                title = context.getString(R.string.mark_as_read);
                break;
            case ConversationContextMenuItemTags.TAG_MarkAsUnread:
                title = context.getString(R.string.mark_as_unread);
                break;
            default:
                title = context.getString(R.string.message_unknown_option);
                break;
        }
        return title;
    }

    /**
     * 执行长按menu操作，需要确认时的提示信息。比如长按会话 -> 删除 -> 提示框进行二次确认
     *
     * @param tag
     * @return
     */
    public String contextConfirmPrompt(Context context, String tag) {
        String title;
        switch (tag) {
            case ConversationContextMenuItemTags.TAG_REMOVE:
                title = context.getString(R.string.delete_conversation_confirm);
                break;
            default:
                title = context.getString(R.string.message_unknown_option);
                break;
        }
        return title;
    }

    /**
     * @param conversationInfo
     * @param itemTag
     * @return 返回true，将从context menu中排除
     */
    public boolean contextMenuItemFilter(ConversationInfo conversationInfo, String itemTag) {
        if (ConversationContextMenuItemTags.TAG_TOP.equals(itemTag)) {
            return conversationInfo.top > 0;
        }

        if (ConversationContextMenuItemTags.TAG_CANCEL_TOP.equals(itemTag)) {
            return conversationInfo.top == 0;
        }

        if (ConversationContextMenuItemTags.TAG_MarkAsRead.equals(itemTag)) {
            return conversationInfo.unreadCount.unread == 0 && conversationInfo.unreadCount.unreadMention == 0 && conversationInfo.unreadCount.unreadMentionAll == 0;
        }

        if (ConversationContextMenuItemTags.TAG_MarkAsUnread.equals(itemTag)) {
            if (conversationInfo.unreadCount.unread > 0 || conversationInfo.unreadCount.unreadMention > 0 || conversationInfo.unreadCount.unreadMentionAll > 0) {
                return true;
            }
            List<Integer> messageStatuses = new ArrayList<>();
            messageStatuses.add(MessageStatus.Readed.value());
            messageStatuses.add(MessageStatus.Played.value());
            List<Message> messages = ChatManager.Instance().getMessagesByMessageStatus(conversationInfo.conversation, messageStatuses, 0, false, 1, "");
            return messages == null || messages.size() == 0;
        }

        return false;
    }

    protected <T extends View> T getView(int viewId) {
        View view;
        view = itemView.findViewById(viewId);
        return (T) view;
    }

    protected ConversationViewHolder setViewVisibility(int viewId, int visibility) {
        View view = itemView.findViewById(viewId);
        view.setVisibility(visibility);
        return this;
    }

    public Fragment getFragment() {
        return fragment;
    }
}
