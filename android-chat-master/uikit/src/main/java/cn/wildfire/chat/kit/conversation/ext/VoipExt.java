/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation.ext;

import android.Manifest;
import android.content.Context;
import android.os.Build;
import android.view.View;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.annotation.ExtContextMenuItem;
import cn.wildfire.chat.kit.conversation.ConversationFragment;
import cn.wildfire.chat.kit.conversation.ext.core.ConversationExt;
import cn.wildfirechat.model.SecretChatInfo;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.uikit.permission.PermissionKit;
import cn.wildfirechat.avenginekit.AVEngineKit;
import cn.wildfirechat.model.Conversation;

public class VoipExt extends ConversationExt {
//    private String targetId;

    @ExtContextMenuItem(tag = ConversationExtMenuTags.TAG_VOIP_VIDEO)
    public void video(View containerView, Conversation conversation) {
        String[] permissions;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions = new String[]{
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA,
                Manifest.permission.BLUETOOTH_CONNECT
            };
        } else {
            permissions = new String[]{
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA
            };
        }
        PermissionKit.PermissionReqTuple[] tuples = PermissionKit.buildRequestPermissionTuples(activity, permissions);
        PermissionKit.checkThenRequestPermission(activity, activity.getSupportFragmentManager(), tuples, o -> {
            if (o) {
                switch (conversation.type) {
                    case Single:
                        videoChat(conversation.target);
                        break;
                    case SecretChat:
                        SecretChatInfo secretChatInfo = ChatManager.Instance().getSecretChatInfo(conversation.target);
                        videoChat(secretChatInfo.getUserId());
                        break;
                    case Group:
                        ((ConversationFragment) fragment).pickGroupMemberToVoipChat(false);
                        break;
                    default:
                        break;
                }
            }
        });
    }

    @ExtContextMenuItem(tag = ConversationExtMenuTags.TAG_VOIP_AUDIO)
    public void audio(View containerView, Conversation conversation) {
        String[] permissions;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions = new String[]{
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.BLUETOOTH_CONNECT
            };
        } else {
            permissions = new String[]{
                Manifest.permission.RECORD_AUDIO
            };
        }
        PermissionKit.PermissionReqTuple[] tuples = PermissionKit.buildRequestPermissionTuples(activity, permissions);
        PermissionKit.checkThenRequestPermission(activity, activity.getSupportFragmentManager(), tuples, o -> {
            if (o) {
                switch (conversation.type) {
                    case Single:
                        audioChat(conversation.target);
                        break;
                    case SecretChat:
                        SecretChatInfo secretChatInfo = ChatManager.Instance().getSecretChatInfo(conversation.target);
                        audioChat(secretChatInfo.getUserId());
                        break;
                    case Group:
                        ((ConversationFragment) fragment).pickGroupMemberToVoipChat(true);
                        break;
                    default:
                        break;
                }
            }
        });
    }

    private void audioChat(String targetId) {
        WfcUIKit.singleCall(activity, conversation, targetId, true);
        // 下面是开始录制系统音频的示例代码
//        this.targetId = targetId;
//        MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) fragment.getActivity().getSystemService(Context.MEDIA_PROJECTION_SERVICE);
//        startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), 102);
    }

//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        WfcUIKit.singleCall(activity, targetId, true);
//        Intent intent = new Intent(fragment.getContext(), VoipCallService.class);
//        intent.putExtra("screenShareForSystemAudioRecord", true);
//        intent.putExtra("data", data);
//        VoipCallService.start(fragment.getContext(), intent);
//    }

    private void videoChat(String targetId) {
        WfcUIKit.singleCall(activity, conversation, targetId, false);
    }

    @Override
    public int priority() {
        return 99;
    }

    @Override
    public int iconResId() {
        return R.mipmap.ic_func_video;
    }

    @Override
    public boolean filter(Conversation conversation) {
        if ((conversation.type == Conversation.ConversationType.Group && AVEngineKit.isSupportMultiCall())) {
            return false;
        }

        if (conversation.type == Conversation.ConversationType.Single) {
//            UserInfo userInfo = ChatManager.Instance().getUserInfo(conversation.target, false);
//            // robot
//            if (userInfo.type == 1) {
//                return true;
//            }
            return false;
        }
        if (conversation.type == Conversation.ConversationType.SecretChat) {
            return false;
        }
        return true;
    }

    @Override
    public String title(Context context) {
        return context.getString(R.string.voip_ext_video);
    }

    @Override
    public String contextMenuTitle(Context context, String tag) {
        if (ConversationExtMenuTags.TAG_VOIP_AUDIO.equals(tag)) {
            return context.getString(R.string.voip_ext_audio);
        } else {
            return context.getString(R.string.voip_ext_video);
        }
    }
}
