/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.voip;

import static org.webrtc.RendererCommon.ScalingType.SCALE_ASPECT_BALANCED;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

import cn.wildfire.chat.kit.BuildConfig;
import cn.wildfire.chat.kit.Config;
import cn.wildfire.chat.kit.R;
import cn.wildfirechat.avenginekit.AVEngineKit;
import cn.wildfirechat.avenginekit.PeerConnectionClient;
import cn.wildfirechat.message.JoinCallRequestMessageContent;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.MultiCallOngoingMessageContent;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.OnReceiveMessageListener;

public class VoipCallService extends Service implements OnReceiveMessageListener {
    private static final int NOTIFICATION_ID = 1;

    private WindowManager wm;
    private View view;
    private WindowManager.LayoutParams params;
    private Intent resumeActivityIntent;
    private boolean showFloatingWindow = false;

    private String focusTargetId;

    private final Handler handler = new Handler();

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("VoipService", "onCreate");
        ChatManager.Instance().addOnReceiveMessageListener(this);

        AVEngineKit.CallSession session = AVEngineKit.Instance().getCurrentSession();

        // 对音频进行变声处理的示例代码，需要高级版音视频 SDK 才支持对音频流进行处理
        // 建议使用 soundTouch 来进行处理
//        session.setAudioProcessor(new AudioProcessor() {
//            @Override
//            public byte[] onAudioRecorded(JavaAudioDeviceModule.AudioSamples audioSamples) {
//                byte[] inputAudioBytes = audioSamples.getData();
//
//                if (inputAudioBytes == null || inputAudioBytes.length == 0) {
//                    // 如果原始音频数据为空，则直接返回，或者调用父类方法
//                    return AudioProcessor.super.onAudioRecorded(audioSamples);
//                }
//
//                int channelCount = audioSamples.getChannelCount();
//                // WebRTC 通常使用 16-bit PCM 编码，所以每个采样点2字节
//                int bytesPerSample = 2;
//                int bytesPerFrame = channelCount * bytesPerSample;
//
//                if (bytesPerFrame <= 0) {
//                    // 无效的帧大小，返回原始数据
//                    return inputAudioBytes;
//                }
//
//                int inputFrameCount = inputAudioBytes.length / bytesPerFrame;
//                if (inputFrameCount == 0) {
//                    // 没有足够的帧进行处理，返回原始数据
//                    return inputAudioBytes;
//                }
//
//                // 调整变声效果：进一步提高音调。
//                // 实现方式：每2帧中，保留第1帧，丢弃第2帧。
//                // 新的长度约为原来的一半，播放速度（音调）变为原来的两倍。
//                int outputFrameCount = inputFrameCount / 2;
//
//                if (outputFrameCount == 0) {
//                    // 处理后的帧数为0（例如输入只有1帧时），返回原始音频，避免产生空音频。
//                    return inputAudioBytes;
//                }
//
//                byte[] outputAudioBytes = new byte[outputFrameCount * bytesPerFrame];
//
//                int outputFrameIndex = 0;
//                for (int inputFrameIndex = 0; inputFrameIndex < inputFrameCount && outputFrameIndex < outputFrameCount; inputFrameIndex++) {
//                    if (inputFrameIndex % 3 != 2) {
//                        int srcPos = inputFrameIndex * bytesPerFrame;
//                        int destPos = outputFrameIndex * bytesPerFrame;
//                        System.arraycopy(inputAudioBytes, srcPos, outputAudioBytes, destPos, bytesPerFrame);
//                        outputFrameIndex++;
//                    }
//                }
//                return outputAudioBytes;
//            }
//        });
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // 仅在WfcUIKit 里面，发起、收到音视频通话时调用
    public static void start(Context context, boolean showFloatingView) {
        Intent intent = new Intent(context, VoipCallService.class);
        intent.putExtra("showFloatingView", showFloatingView);
        intent.putExtra("playback", true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    public static void start(Context context, Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    public static void stop(Context context) {
        Intent intent = new Intent(context, VoipCallService.class);
        context.stopService(intent);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        AVEngineKit.CallSession session = AVEngineKit.Instance().getCurrentSession();
        if (session == null || session.state == AVEngineKit.CallState.Idle) {
            stopSelf();
            return START_NOT_STICKY;
        }
        boolean screenShare = intent.getBooleanExtra("screenShare", false);
        boolean playback = intent.getBooleanExtra("playback", false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            ServiceCompat.startForeground(this, NOTIFICATION_ID, buildNotification(session),
                screenShare || session.isScreenSharing() ? ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION :
                    (playback ? ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK : ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE));
        } else {
            startForeground(NOTIFICATION_ID, buildNotification(session));
        }
        if (screenShare) {
            Intent data = intent.getParcelableExtra("data");
            session.startScreenShare(data);
            return START_NOT_STICKY;
        }

        // 录制系统音频示例代码
//        boolean screenShareForRecord = intent.getBooleanExtra("screenShareForSystemAudioRecord", false);
//        if (screenShareForRecord) {
//            Intent data = intent.getParcelableExtra("data");
//            session.startRecordSystemAudio(data);
//            return START_NOT_STICKY;
//        }

        focusTargetId = intent.getStringExtra("focusTargetId");
        Log.e("wfc", "on startCommand " + focusTargetId);
        checkCallState();
        showFloatingWindow = intent.getBooleanExtra("showFloatingView", false);
        if (showFloatingWindow) {
            rendererInitialized = false;
            lastState = null;
            try {
                showFloatingWindow(session);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            hideFloatBox();
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        if (Intent.ACTION_MAIN.equals(rootIntent.getAction())) {
            AVEngineKit.CallSession session = AVEngineKit.Instance().getCurrentSession();
            if (session != null && session.isConference()) {
                session.leaveConference(false);
            }
        }
    }

    private void checkCallState() {
        AVEngineKit.CallSession session = AVEngineKit.Instance().getCurrentSession();
        if (session == null || AVEngineKit.CallState.Idle == session.getState()) {
            stopSelf();
        } else {
            updateNotification(session);
            if (showFloatingWindow && session.getState() == AVEngineKit.CallState.Connected) {
                if (session.isScreenSharing()) {
                    showScreenSharingView(session);
                } else if (session.isAudioOnly()) {
                    showAudioView(session);
                } else {
                    String nextFocusUserId = nextFocusUserId(session);
                    if (nextFocusUserId != null) {
                        showVideoView(session, nextFocusUserId);
                    } else {
                        showAudioView(session);
                    }
                }
            }

            if (session.getState() == AVEngineKit.CallState.Connected
                && ChatManager.Instance().getUserId().equals(session.getInitiator())) {
                broadcastCallOngoing(session);
            }

            handler.removeCallbacks(this::checkCallState);
            handler.postDelayed(this::checkCallState, 1000);
        }
    }

    private void updateNotification(AVEngineKit.CallSession session) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, buildNotification(session));
    }

    private Notification buildNotification(AVEngineKit.CallSession session) {
        resumeActivityIntent = new Intent(this, VoipDummyActivity.class);
        resumeActivityIntent.putExtra(SingleCallActivity.EXTRA_FROM_FLOATING_VIEW, true);
        resumeActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent pendingIntent;
        if (Build.VERSION.SDK_INT >= 23) {
            pendingIntent = PendingIntent.getActivity(this, 0, resumeActivityIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        } else {
            pendingIntent = PendingIntent.getActivity(this, 0, resumeActivityIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        String channelId = "";
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            channelId = BuildConfig.LIBRARY_PACKAGE_NAME + ".voip";
            String channelName = "voip";
            NotificationChannel chan = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW);
            chan.setLightColor(Color.BLUE);
            chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            manager.createNotificationChannel(chan);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId);

        String title;
        if (session != null) {
            switch (session.getState()) {
                case Outgoing:
                    title = getString(R.string.call_waiting_for_answer);
                    break;
                case Incoming:
                    title = getString(R.string.call_invitation);
                    break;
                case Connecting:
                    title = getString(R.string.call_connecting);
                    break;
                default:
                    title = getString(R.string.call_in_progress);
                    break;
            }
        } else {
            title = "VOIP...";
        }
        return builder.setSmallIcon(R.mipmap.ic_launcher_notification)
            .setContentTitle(title)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (wm != null && view != null) {
            wm.removeView(view);
        }
        ChatManager.Instance().removeOnReceiveMessageListener(this);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_ID);
    }

    private void showFloatingWindow(AVEngineKit.CallSession session) {
        if (wm != null) {
            return;
        }
        session.restVideoViews();

        wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        params = new WindowManager.LayoutParams();

        int type;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            type = WindowManager.LayoutParams.TYPE_PHONE;
        }
        params.type = type;
        params.flags = WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
            | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;

        params.format = PixelFormat.TRANSLUCENT;
        params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        params.gravity = Gravity.CENTER;
        params.x = getResources().getDisplayMetrics().widthPixels;
        params.y = 0;

        view = LayoutInflater.from(this).inflate(R.layout.av_voip_float_view, null);
        view.setOnTouchListener(onTouchListener);
        wm.addView(view, params);
        if (session.getState() != AVEngineKit.CallState.Connected && !(!session.isAudioOnly() && session.getState() == AVEngineKit.CallState.Outgoing)) {
            showUnConnectedCallInfo(session);
        } else {
            if (session.isScreenSharing()) {
                showScreenSharingView(session);
            } else if (session.isAudioOnly()) {
                showAudioView(session);
            } else {
                String nextFocusUserId = nextFocusUserId(session);
                if (session.state == AVEngineKit.CallState.Outgoing) {
                    nextFocusUserId = ChatManager.Instance().getUserId();
                }
                if (nextFocusUserId != null) {
                    showVideoView(session, nextFocusUserId);
                } else {
                    showAudioView(session);
                }
            }
        }
    }

    public void hideFloatBox() {
        if (wm != null && view != null) {
            wm.removeView(view);
            wm = null;
            view = null;
        }
    }

    private void showUnConnectedCallInfo(AVEngineKit.CallSession session) {
        FrameLayout remoteVideoFrameLayout = view.findViewById(R.id.remoteVideoFrameLayout);
        if (remoteVideoFrameLayout.getVisibility() == View.VISIBLE) {
            remoteVideoFrameLayout.setVisibility(View.GONE);
            wm.removeView(view);
            wm.addView(view, params);
//            wm.updateViewLayout(view, params);
        }

        view.findViewById(R.id.audioLinearLayout).setVisibility(View.VISIBLE);
        TextView timeView = view.findViewById(R.id.durationTextView);
        ImageView mediaIconV = view.findViewById(R.id.av_media_type);
        mediaIconV.setImageResource(R.drawable.av_float_audio);

        String title = "";
        switch (session.getState()) {
            case Outgoing:
                title = "等待接听";
                break;
            case Incoming:
                title = "等待接听";
                break;
            case Connecting:
                title = "接听中";
                break;
        }
        timeView.setText(title);
    }

    private void showScreenSharingView(AVEngineKit.CallSession session) {
        FrameLayout remoteVideoFrameLayout = view.findViewById(R.id.remoteVideoFrameLayout);
        if (remoteVideoFrameLayout.getVisibility() == View.VISIBLE) {
            remoteVideoFrameLayout.setVisibility(View.GONE);
            wm.removeView(view);
            wm.addView(view, params);
        }
        view.findViewById(R.id.screenSharingTextView).setVisibility(View.VISIBLE);
        TextView durationTextView = view.findViewById(R.id.durationTextView);
        durationTextView.setVisibility(View.VISIBLE);
        long duration = (ChatManager.Instance().getServerTimestamp() - session.getConnectedTime()) / 1000;
        if (duration >= 3600) {
            durationTextView.setText(String.format("%d:%02d:%02d", duration / 3600, (duration % 3600) / 60, (duration % 60)));
        } else {
            durationTextView.setText(String.format("%02d:%02d", (duration % 3600) / 60, (duration % 60)));
        }

        view.findViewById(R.id.av_media_type).setVisibility(View.GONE);
    }

    private void showAudioView(AVEngineKit.CallSession session) {
        FrameLayout remoteVideoFrameLayout = view.findViewById(R.id.remoteVideoFrameLayout);
        if (remoteVideoFrameLayout.getVisibility() == View.VISIBLE) {
            remoteVideoFrameLayout.setVisibility(View.GONE);
            wm.removeView(view);
            wm.addView(view, params);
//            wm.updateViewLayout(view, params);
        }

        view.findViewById(R.id.audioLinearLayout).setVisibility(View.VISIBLE);
        TextView timeView = view.findViewById(R.id.durationTextView);
        ImageView mediaIconV = view.findViewById(R.id.av_media_type);
        mediaIconV.setImageResource(R.drawable.av_float_audio);

        long duration = (ChatManager.Instance().getServerTimestamp() - session.getConnectedTime()) / 1000;
        if (duration >= 3600) {
            timeView.setText(String.format("%d:%02d:%02d", duration / 3600, (duration % 3600) / 60, (duration % 60)));
        } else {
            timeView.setText(String.format("%02d:%02d", (duration % 3600) / 60, (duration % 60)));
        }
    }

    private boolean rendererInitialized = false;
    private AVEngineKit.CallState lastState = null;
    private String lastFocusUserId = null;

    private String nextFocusUserId(AVEngineKit.CallSession session) {
        if (!TextUtils.isEmpty(focusTargetId) && (session.getParticipantIds().contains(focusTargetId))) {
            PeerConnectionClient client = session.getClient(focusTargetId);
            if (client != null && client.state == AVEngineKit.CallState.Connected && !client.videoMuted && !client.audience) {
                return focusTargetId;
            }
        }
        if (ChatManager.Instance().getUserId().equals(focusTargetId)) {
            if (session.state == AVEngineKit.CallState.Connected && !session.videoMuted) {
                return focusTargetId;
            }
        }
        String targetId = null;
        if (session.isConference()) {
            List<String> participants = session.getParticipantIds();
            if (!participants.isEmpty()) {
                for (String participant : participants) {
                    PeerConnectionClient client = session.getClient(participant);
                    if (client.state == AVEngineKit.CallState.Connected && !client.audience && !client.videoMuted) {
                        targetId = participant;
                        break;
                    }
                }
            }

        } else {
            if (session.getConversation().type == Conversation.ConversationType.Group) {
                for (AVEngineKit.ParticipantProfile profile : session.getParticipantProfiles()) {
                    if (profile.getState() == AVEngineKit.CallState.Connected && !profile.isVideoMuted()) {
                        targetId = profile.getUserId();
                        break;
                    }
                }
            } else {
                targetId = session.getConversation().target;
            }
        }
        if (targetId == null && session.state == AVEngineKit.CallState.Connected && !session.videoMuted) {
            targetId = ChatManager.Instance().getUserId();
        }
        return targetId;
    }

    private void showVideoView(AVEngineKit.CallSession session, String nextFocusUserId) {
        view.findViewById(R.id.audioLinearLayout).setVisibility(View.GONE);
        FrameLayout remoteVideoFrameLayout = view.findViewById(R.id.remoteVideoFrameLayout);
        remoteVideoFrameLayout.setVisibility(View.VISIBLE);
        LinearLayout videoContainer = remoteVideoFrameLayout.findViewById(R.id.videoContainer);

//        Log.e("wfc", "nextFocusUserId " + nextFocusUserId);
        if (!rendererInitialized || lastState != session.getState() || !TextUtils.equals(lastFocusUserId, nextFocusUserId)) {
            rendererInitialized = true;
            lastState = session.getState();

            ImageView portraitImageView = remoteVideoFrameLayout.findViewById(R.id.portraitImageView);
            UserInfo userInfo = ChatManager.Instance().getUserInfo(nextFocusUserId, false);
            Glide.with(remoteVideoFrameLayout).load(userInfo.portrait).placeholder(R.mipmap.avatar_def).into(portraitImageView);

            if (TextUtils.equals(ChatManager.Instance().getUserId(), nextFocusUserId)) {
                session.setupLocalVideoView(videoContainer, SCALE_ASPECT_BALANCED);
                // 因为remoteVideoViewContainer 和 localVideoViewContainer 是同一个，所以切换的时候，需要清一下
                if (!TextUtils.isEmpty(lastFocusUserId)) {
                    session.setupRemoteVideoView(lastFocusUserId, null, SCALE_ASPECT_BALANCED);
                }
            } else {
                session.setupRemoteVideoView(nextFocusUserId, videoContainer, SCALE_ASPECT_BALANCED);
                session.setupLocalVideoView(null, SCALE_ASPECT_BALANCED);
            }
            lastFocusUserId = nextFocusUserId;
        }
    }

    private void clickToResume() {
        AVEngineKit.CallSession session = AVEngineKit.Instance().getCurrentSession();
        if (session != null) {
            if (session.isScreenSharing()) {
                session.stopScreenShare();
            }

//            if (rendererInitialized) {
//                session.resetRenderer();
//            }
        }
        showFloatingWindow = false;
        startActivity(resumeActivityIntent);
    }

    private void broadcastCallOngoing(AVEngineKit.CallSession callSession) {
        if (Config.ENABLE_MULTI_CALL_AUTO_JOIN && !callSession.isConference() && callSession.getConversation().type == Conversation.ConversationType.Group) {
            MultiCallOngoingMessageContent ongoingMessageContent = new MultiCallOngoingMessageContent(callSession.getCallId(), callSession.getInitiator(), callSession.isAudioOnly(), callSession.getParticipantIds());
            ChatManager.Instance().sendMessage(callSession.getConversation(), ongoingMessageContent, null, 0, null);
        }
    }

    View.OnTouchListener onTouchListener = new View.OnTouchListener() {
        float lastX, lastY;
        int oldOffsetX, oldOffsetY;
        int tag = 0;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            final int action = event.getAction();
            float x = event.getX();
            float y = event.getY();
            if (tag == 0) {
                oldOffsetX = params.x;
                oldOffsetY = params.y;
            }
            if (action == MotionEvent.ACTION_DOWN) {
                lastX = x;
                lastY = y;
            } else if (action == MotionEvent.ACTION_MOVE) {
                // 减小偏移量,防止过度抖动
                params.x += (int) (x - lastX) / 3;
                params.y += (int) (y - lastY) / 3;
                tag = 1;
                wm.updateViewLayout(v, params);
            } else if (action == MotionEvent.ACTION_UP) {
                int newOffsetX = params.x;
                int newOffsetY = params.y;
                if (Math.abs(oldOffsetX - newOffsetX) <= 20 && Math.abs(oldOffsetY - newOffsetY) <= 20) {
                    clickToResume();
                } else {
                    tag = 0;
                }
            }
            return true;
        }
    };

    @Override
    public void onReceiveMessage(List<Message> messages, boolean hasMore) {
        AVEngineKit.CallSession session = AVEngineKit.Instance().getCurrentSession();
        if (session != null && session.getState() == AVEngineKit.CallState.Connected) {
            for (int i = 0; i < messages.size(); i++) {
                Message message = messages.get(i);
                if (message.content instanceof JoinCallRequestMessageContent) {
                    JoinCallRequestMessageContent request = (JoinCallRequestMessageContent) message.content;
                    if (session.getCallId().equals(request.getCallId()) && session.getInitiator().equals(ChatManager.Instance().getUserId())) {
                        List<String> ps = new ArrayList<>();
                        ps.add(message.sender);
                        session.inviteNewParticipants(ps, request.getClientId(), true);
                    }
                }
            }
        }
    }
}
