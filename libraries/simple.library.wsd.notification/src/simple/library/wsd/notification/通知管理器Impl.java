package simple.library.wsd.notification;

import simple.runtime.android.MainActivity;
import simple.runtime.components.组件容器;
import simple.runtime.components.impl.组件Impl;
import simple.runtime.events.EventDispatcher;

import java.util.UUID;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.RemoteInput;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;

/**
 * 通知管理器组件的 Android 实现。
 * 负责通知渠道、发布与当前窗口存活期间的点击广播分发。
 */
public final class 通知管理器Impl extends 组件Impl implements 通知管理器 {
	/* 有声与静音通知使用不同渠道；渠道声音设置创建后不可更改。 */
	private static final String ALERT_CHANNEL_ID = "es4a.notification.alert";
	private static final String SILENT_CHANNEL_ID = "es4a.notification.silent";

	/** 持有广播接收器的窗口，供组件销毁时释放注册。 */
	private final MainActivity activity;
	/** 发布和删除通知所用的系统服务。 */
	private final NotificationManager notificationManager;
	/** 每个组件实例使用独立的点击动作，避免多个实例串收事件。 */
	private final String clickAction;
	/** 操作按钮和内联回复共用另一广播动作，与通知正文点击区分。 */
	private final String operationAction;
	/** 接收此组件创建的通知正文点击、按钮和回复。 */
	private final BroadcastReceiver clickReceiver;
	/** 窗口销毁时注销动态广播接收器。 */
	private final MainActivity.OnDestroyListener destroyListener;
	/** 防止组件销毁和窗口销毁时重复注销接收器。 */
	private boolean receiverRegistered;

	/**
	 * 创建通知组件并注册通知点击接收器。
	 *
	 * @param container 承载此组件的容器
	 */
	public 通知管理器Impl(组件容器 container) {
		super(container);

		// 为每个组件实例创建独立广播动作，避免多个管理器相互分发事件。
		activity = MainActivity.getContext();
		notificationManager = (NotificationManager) activity.getSystemService(Context.NOTIFICATION_SERVICE);
		clickAction = activity.getPackageName() + ".ES4A_NOTIFICATION_CLICK." + UUID.randomUUID();
		operationAction = clickAction + ".ACTION";

		// 两种广播均不携带 data URI，由同一个接收器处理并在窗口销毁时注销。
		clickReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				handleNotificationBroadcast(intent);
			}
		};
		IntentFilter filter = new IntentFilter(clickAction);
		filter.addAction(operationAction);
		activity.registerReceiver(clickReceiver, filter);
		receiverRegistered = true;

		destroyListener = new MainActivity.OnDestroyListener() {
			@Override
			public void onDestroy() {
				unregisterClickReceiver();
			}
		};
		activity.addOnDestroyListener(destroyListener);
	}

	/**
	 * 只分发当前组件实例发出的广播，避免不同通知管理器互相接收操作。
	 *
	 * @param intent 收到的通知点击或按钮广播
	 */
	private void handleNotificationBroadcast(Intent intent) {
		// 只处理本组件发出的有效通知广播。
		if (intent == null || (!clickAction.equals(intent.getAction())
				&& !operationAction.equals(intent.getAction()))) {
			return;
		}

		Bundle extras = intent.getExtras();
		if (extras == null || !extras.containsKey(通知构建器.EXTRA_NOTIFICATION_ID)) {
			return;
		}

		// 按操作类型分发，回复正文从 RemoteInput 取得。
		int notificationId = extras.getInt(通知构建器.EXTRA_NOTIFICATION_ID);
		int kind = extras.getInt(通知构建器.EXTRA_ACTION_KIND, 通知构建器.ACTION_CONTENT);
		if (kind == 通知构建器.ACTION_BUTTON) {
			按钮被单击(notificationId, extras.getString(通知构建器.EXTRA_ACTION_MARK));
		} else if (kind == 通知构建器.ACTION_REPLY && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			// 回复可能没有文字结果，统一作为空文本交给事件。
			Bundle results = RemoteInput.getResultsFromIntent(intent);
			CharSequence reply = results == null ? null : results.getCharSequence(通知构建器.REPLY_RESULT_KEY);
			收到回复(
				notificationId,
				extras.getString(通知构建器.EXTRA_ACTION_MARK),
				reply == null ? "" : reply.toString()
			);
		} else if (kind == 通知构建器.ACTION_CONTENT) {
			通知被单击(notificationId);
		}
	}

	/**
	 * 注销动态广播接收器；组件和窗口可以先后触发释放。
	 */
	private void unregisterClickReceiver() {
		if (receiverRegistered) {
			activity.unregisterReceiver(clickReceiver);
			receiverRegistered = false;
		}
	}

	/**
	 * 为通知创建携带编号的点击广播。
	 *
	 * @param notificationId 通知编号，同时用作 PendingIntent 的请求码
	 * @return 通知点击时发送的 PendingIntent
	 */
	private PendingIntent createClickPendingIntent(int notificationId) {
		// 请求码以通知编号区分，更新通知时复用并刷新原有意图。
		Intent intent = new Intent(clickAction);
		intent.setPackage(activity.getPackageName());
		intent.putExtra(通知构建器.EXTRA_NOTIFICATION_ID, notificationId);

		// Android 12 起要求明确指定可变性；通知主体点击不需要修改意图。
		int flags = PendingIntent.FLAG_UPDATE_CURRENT;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			flags |= PendingIntent.FLAG_IMMUTABLE;
		}

		return PendingIntent.getBroadcast(activity, notificationId, intent, flags);
	}

	/**
	 * 创建 Android 构建器；通知渠道在发送时注册。
	 *
	 * @return 尚未设置小图标的 Android 构建器
	 */
	@SuppressWarnings("deprecation")
	private Notification.Builder createNotificationBuilder() {
		Notification.Builder builder;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			builder = new Notification.Builder(activity, SILENT_CHANNEL_ID);
		} else {
			// API 25 及以下使用旧版构造器，并关闭默认提示音。
			builder = new Notification.Builder(activity);
			builder.setSound(null);
		}
		return builder.setAutoCancel(true).setOnlyAlertOnce(true);
	}

	/**
	 * 根据声音和优先级选择渠道；渠道创建后的设置由系统管理。
	 *
	 * @param playSound 是否使用有声渠道
	 * @param priority 通知优先级
	 * @return 当前通知使用的渠道编号
	 */
	@TargetApi(Build.VERSION_CODES.O)
	private String ensureNotificationChannel(boolean playSound, int priority) {
		// 声音和优先级决定渠道身份；已创建渠道的设置不能直接改写。
		String baseId = playSound ? ALERT_CHANNEL_ID : SILENT_CHANNEL_ID;
		String channelId = priority == 通知构建器.优先级_默认 ? baseId : baseId + "." + priority;
		if (notificationManager.getNotificationChannel(channelId) != null) {
			return channelId;
		}

		// 静音渠道同时关闭声音和振动，避免系统默认行为产生提醒。
		NotificationChannel channel = new NotificationChannel(
			channelId,
			channelName(playSound, priority),
			channelImportance(priority)
		);
		if (!playSound) {
			channel.setSound(null, null);
			channel.enableVibration(false);
		}
		notificationManager.createNotificationChannel(channel);
		return channelId;
	}

	/**
	 * Android 8 起 priority 本身不决定提醒强度，需映射为渠道重要程度。
	 *
	 * @param priority 通知优先级
	 * @return Android 渠道重要程度
	 */
	@TargetApi(Build.VERSION_CODES.O)
	private static int channelImportance(int priority) {
		switch (priority) {
			case 通知构建器.优先级_最低:
				return NotificationManager.IMPORTANCE_MIN;
			case 通知构建器.优先级_低:
				return NotificationManager.IMPORTANCE_LOW;
			case 通知构建器.优先级_高:
			case 通知构建器.优先级_最高:
				return NotificationManager.IMPORTANCE_HIGH;
			default:
				return NotificationManager.IMPORTANCE_DEFAULT;
		}
	}

	/**
	 * 生成用户在系统设置中看到的渠道名称。
	 *
	 * @param playSound 是否使用有声渠道
	 * @param priority 通知优先级
	 * @return 中文渠道名称
	 */
	private static String channelName(boolean playSound, int priority) {
		String prefix = playSound ? "通知" : "静音通知";
		switch (priority) {
			case 通知构建器.优先级_最低:
				return prefix + " · 最低";
			case 通知构建器.优先级_低:
				return prefix + " · 低";
			case 通知构建器.优先级_高:
				return prefix + " · 高";
			case 通知构建器.优先级_最高:
				return prefix + " · 最高";
			default:
				return prefix;
		}
	}

	/**
	 * API 16 起使用 build，API 14 至 15 使用旧版 getNotification。
	 *
	 * @param builder 已设置内容的通知构建器
	 * @return 可交给 NotificationManager 发布的通知
	 */
	@SuppressWarnings("deprecation")
	private Notification buildNotification(Notification.Builder builder) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
			return builder.build();
		}
		// API 14 至 15 没有 build，使用旧版完成方法。
		return builder.getNotification();
	}

	@Override
	public 通知构建器 创建通知() {
		return new 通知构建器(createNotificationBuilder(), activity);
	}

	@Override
	public void 发送通知(int notificationId, 通知构建器 notification) {
		if (notification == null) {
			throw new IllegalArgumentException("通知构建器不能为空");
		}

		// 先绑定渠道和按钮，再设置通知点击意图并发布。
		Notification.Builder builder = notification.getBuilder();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			builder.setChannelId(ensureNotificationChannel(notification.getPlaysSound(), notification.getPriority()));
		}
		notification.bindActions(notificationId, operationAction);

		builder.setWhen(System.currentTimeMillis()).setContentIntent(createClickPendingIntent(notificationId));
		notificationManager.notify(notificationId, buildNotification(builder));
	}

	@Override
	public void 删除通知(int notificationId) {
		notificationManager.cancel(notificationId);
	}

	@Override
	public void 清空通知() {
		notificationManager.cancelAll();
	}

	@Override
	public void 通知被单击(int notificationId) {
		EventDispatcher.dispatchEvent(this, "通知被单击", notificationId);
	}

	@Override
	public void 按钮被单击(int notificationId, String mark) {
		EventDispatcher.dispatchEvent(this, "按钮被单击", notificationId, mark);
	}

	@Override
	public void 收到回复(int notificationId, String mark, String reply) {
		EventDispatcher.dispatchEvent(this, "收到回复", notificationId, mark, reply);
	}

	@Override
	public void 销毁() {
		activity.removeOnDestroyListener(destroyListener);
		unregisterClickReceiver();
		super.销毁();
	}
}
