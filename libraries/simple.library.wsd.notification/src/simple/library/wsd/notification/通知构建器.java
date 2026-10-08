package simple.library.wsd.notification;

import simple.runtime.annotations.SimpleDataElement;
import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.RemoteInput;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Icon;
import android.os.Build;

/**
 * 可逐项定制的通知对象，持有 Android {@link Notification.Builder}。
 * 通知编号在管理器发送时确定；操作按钮会在首次发送时绑定管理器和编号。
 */
@SimpleObject
public final class 通知构建器 {
	/** 未自定义小图标时使用 Android 内置通知图标。 */
	private static final int DEFAULT_NOTIFICATION_ICON = android.R.drawable.stat_notify_more;

	/* 未指定操作按钮图标时，按按钮类型使用对应的 Android 内置图标。 */
	private static final int DEFAULT_ACTION_ICON = android.R.drawable.ic_menu_view;
	private static final int DEFAULT_REPLY_ACTION_ICON = android.R.drawable.ic_menu_send;

	// 本封装每条通知最多允许三个操作按钮。
	private static final int MAX_ACTIONS = 3;
	// 为各按钮分配稳定且不同的 PendingIntent 请求码。
	private static final AtomicInteger NEXT_ACTION_REQUEST_CODE = new AtomicInteger(1);

	/* 编译平台为 API 26；以下两个常量用于 Android 12 的内联回复兼容处理。 */
	private static final int ANDROID_12_API_LEVEL = 31;
	private static final int FLAG_MUTABLE = 0x02000000;

	/** 以下广播数据键和操作类型供通知管理器Impl读取。 */
	public static final String EXTRA_NOTIFICATION_ID = "notificationId";
	public static final String EXTRA_ACTION_KIND = "actionKind";
	public static final String EXTRA_ACTION_MARK = "actionMark";
	public static final String REPLY_RESULT_KEY = "reply";
	public static final int ACTION_CONTENT = 0;
	public static final int ACTION_BUTTON = 1;
	public static final int ACTION_REPLY = 2;

	/* 以下是一组通知优先级常量；API 26 起映射为通知渠道重要程度。 */
	@SimpleDataElement
	public static final int 优先级_最低 = -2;
	@SimpleDataElement
	public static final int 优先级_低 = -1;
	@SimpleDataElement
	public static final int 优先级_默认 = 0;
	@SimpleDataElement
	public static final int 优先级_高 = 1;
	@SimpleDataElement
	public static final int 优先级_最高 = 2;

	/* 以下是一组锁屏内容可见性常量，仅 API 21 及以上有效。 */
	@SimpleDataElement
	public static final int 公开范围_公开 = Notification.VISIBILITY_PUBLIC;
	@SimpleDataElement
	public static final int 公开范围_私密 = Notification.VISIBILITY_PRIVATE;
	@SimpleDataElement
	public static final int 公开范围_保密 = Notification.VISIBILITY_SECRET;

	private final Notification.Builder builder;
	private final Context context;

	/* 发送前暂存按钮，发送时才有通知编号可用于创建 PendingIntent。 */
	private final List<ActionSpec> actions = new ArrayList<ActionSpec>();
	private boolean playSound;
	private int priority = 优先级_默认;

	/* 已附加的按钮意图固定属于一个管理器和通知编号。 */
	private Integer boundNotificationId;
	private String boundBroadcastAction;
	private int addedActionCount;

	/**
	 * 一条尚未绑定通知编号的操作按钮配置。
	 * 管理器发送通知时才确定编号，并据此生成互不覆盖的点击意图。
	 */
	private static final class ActionSpec {
		private final int kind;
		private final int icon;
		private final String title;
		private final String mark;
		private final String hint;
		// 请求码区分普通按钮和回复按钮的 PendingIntent。
		private final int requestCode = NEXT_ACTION_REQUEST_CODE.getAndIncrement();

		/**
		 * 保存按钮配置；通知编号在发送时才确定。
		 *
		 * @param kind 按钮类型
		 * @param icon 按钮图标资源编号
		 * @param title 按钮文字
		 * @param mark 回调标记
		 * @param hint 回复输入提示，普通按钮为 null
		 */
		private ActionSpec(int kind, int icon, String title, String mark, String hint) {
			this.kind = kind;
			this.icon = icon;
			this.title = title;
			this.mark = mark;
			this.hint = hint;
		}
	}

	/* 以下构造器和方法仅供同包的通知管理器Impl调用，保留包内可见性。 */

	/**
	 * 构造方法
	 */
	public 通知构建器() {
		throw new UnsupportedOperationException("请通过\"通知管理器.创建通知()\"创建");
	}

	/**
	 * 保存由通知管理器创建的 Android 构建器，并设置内置小图标。
	 *
	 * @param builder Android 通知构建器
	 * @param context 用于读取资源和创建操作意图的上下文
	 */
	public 通知构建器(Notification.Builder builder, Context context) {
		this.builder = builder.setSmallIcon(DEFAULT_NOTIFICATION_ICON);
		this.context = context;
	}

	/**
	 * 取得持有的 Android 构建器，供通知管理器发布。
	 *
	 * @return Android 通知构建器
	 */
	public Notification.Builder getBuilder() {
		return builder;
	}

	/**
	 * 取得当前提示音设置，供通知管理器选择渠道。
	 *
	 * @return 是否播放提示音
	 */
	public boolean getPlaysSound() {
		return playSound;
	}

	/**
	 * 取得提醒级别，供通知管理器选择渠道。
	 *
	 * @return Android 通知优先级
	 */
	public int getPriority() {
		return priority;
	}

	/**
	 * 发送前为操作按钮绑定管理器和通知编号；再次发送同一通知时只追加新操作。
	 *
	 * @param notificationId 要发送的通知编号
	 * @param broadcastAction 此通知管理器的广播动作
	 */
	public void bindActions(int notificationId, String broadcastAction) {
		if (actions.isEmpty()) {
			return;
		}

		// 已附加的按钮意图不能改投到另一编号或另一管理器。
		if (boundNotificationId != null && (boundNotificationId.intValue() != notificationId
				|| !boundBroadcastAction.equals(broadcastAction))) {
			throw new IllegalArgumentException("带操作按钮的通知构建器不能用于不同通知编号或通知管理器");
		}

		// 只追加本次新增的按钮，避免更新通知时重复添加旧按钮。
		boundNotificationId = notificationId;
		boundBroadcastAction = broadcastAction;
		for (int i = addedActionCount; i < actions.size(); i++) {
			ActionSpec action = actions.get(i);
			appendAction(action, createActionPendingIntent(notificationId, broadcastAction, action));
			addedActionCount++;
		}
	}

	/**
	 * 使用按钮独立的请求码区分 PendingIntent；不设置数据 URI，以匹配按动作注册的接收器。
	 *
	 * @param notificationId 当前通知编号
	 * @param broadcastAction 所属通知管理器的广播动作
	 * @param action 要绑定的按钮定义
	 * @return 按钮点击时发出的广播意图
	 */
	private PendingIntent createActionPendingIntent(int notificationId, String broadcastAction, ActionSpec action) {
		// 请求码区分同一通知上的不同按钮；extras 保存回调所需的数据。
		Intent intent = new Intent(broadcastAction);
		intent.setPackage(context.getPackageName());
		intent.putExtra(EXTRA_NOTIFICATION_ID, notificationId);
		intent.putExtra(EXTRA_ACTION_KIND, action.kind);
		intent.putExtra(EXTRA_ACTION_MARK, action.mark);

		// 回复意图必须可变：API 24 至 30 默认可变，API 31 起显式声明；普通按钮保持不可变。
		int flags = PendingIntent.FLAG_UPDATE_CURRENT;
		if (action.kind == ACTION_REPLY) {
			if (Build.VERSION.SDK_INT >= ANDROID_12_API_LEVEL) {
				flags |= FLAG_MUTABLE;
			}
		} else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			flags |= PendingIntent.FLAG_IMMUTABLE;
		}

		return PendingIntent.getBroadcast(context, action.requestCode, intent, flags);
	}

	/**
	 * 按系统版本附加按钮；回复按钮额外携带输入框。
	 *
	 * @param action 要添加的按钮定义
	 * @param pendingIntent 点击按钮时发送的意图
	 */
	private void appendAction(ActionSpec action, PendingIntent pendingIntent) {
		// 回复按钮必须携带输入框；普通按钮按系统版本选择可用的 API。
		if (action.kind == ACTION_REPLY) {
			appendReplyAction(action, pendingIntent);
		} else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			builder.addAction(createModernActionBuilder(action, pendingIntent).build());
		} else {
			appendLegacyAction(action, pendingIntent);
		}
	}

	/**
	 * API 24 起为回复按钮附加系统内联输入框。
	 *
	 * @param action 回复按钮定义
	 * @param pendingIntent 提交回复时发送的意图
	 */
	@TargetApi(Build.VERSION_CODES.N)
	private void appendReplyAction(ActionSpec action, PendingIntent pendingIntent) {
		RemoteInput input = new RemoteInput.Builder(REPLY_RESULT_KEY).setLabel(action.hint).build();
		builder.addAction(createModernActionBuilder(action, pendingIntent).addRemoteInput(input).build());
	}

	/**
	 * API 23 起使用 Icon 构造操作按钮，避开已弃用的整数图标构造器。
	 * 从资源编号取实际包名，兼容应用图标和 android.R 的内置图标。
	 *
	 * @param action 要添加的按钮定义
	 * @param pendingIntent 点击按钮时发送的意图
	 * @return 可继续添加内联输入的操作构建器
	 */
	@TargetApi(Build.VERSION_CODES.M)
	private Notification.Action.Builder createModernActionBuilder(ActionSpec action, PendingIntent pendingIntent) {
		String resourcePackage = context.getResources().getResourcePackageName(action.icon);
		Icon icon = Icon.createWithResource(resourcePackage, action.icon);
		return new Notification.Action.Builder(icon, action.title, pendingIntent);
	}

	/**
	 * API 16 至 22 使用整数图标的操作按钮；API 23 起使用 Icon。
	 *
	 * @param action 要添加的按钮定义
	 * @param pendingIntent 点击按钮时发送的意图
	 */
	@SuppressWarnings("deprecation")
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	private void appendLegacyAction(ActionSpec action, PendingIntent pendingIntent) {
		builder.addAction(action.icon, action.title, pendingIntent);
	}

	/**
	 * API 25 及以下使用构建器的声音设置；API 26 起由渠道负责。
	 *
	 * @param playSound 是否播放系统默认通知提示音
	 */
	@SuppressWarnings("deprecation")
	private void applyLegacySound(boolean playSound) {
		builder.setDefaults(playSound ? Notification.DEFAULT_SOUND : 0);
		if (!playSound) {
			builder.setSound(null);
		}
	}

	/**
	 * API 16 至 25 使用构建器优先级；API 26 起由渠道重要程度负责。
	 *
	 * @param value 通知优先级常量
	 */
	@SuppressWarnings("deprecation")
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	private void applyLegacyPriority(int value) {
		builder.setPriority(value);
	}

	/**
	 * 拒绝在 API 16 以下调用展开样式。
	 */
	private void requireStyleSupport() {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
			throw new UnsupportedOperationException("通知展开样式需要 Android API 16");
		}
	}

	/**
	 * 验证并暂存按钮定义，等待发送时绑定通知编号。
	 *
	 * @param kind 普通按钮或回复按钮
	 * @param icon 图标资源编号，0 表示使用内置操作图标
	 * @param title 按钮文字
	 * @param mark 回调标记
	 * @param hint 回复输入框提示，普通按钮为 null
	 */
	private void addAction(int kind, int icon, String title, String mark, String hint) {
		// 先拒绝不支持的版本和无效配置，再保存尚未绑定编号的按钮。
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
			throw new UnsupportedOperationException("通知操作按钮需要 Android API 16");
		}
		if (actions.size() >= MAX_ACTIONS) {
			throw new IllegalStateException("每条通知最多添加三个操作按钮");
		}
		if (icon < 0 || title == null || title.length() == 0 || mark == null || mark.length() == 0) {
			throw new IllegalArgumentException("操作图标、标题或标记无效");
		}

		// 未指定图标时按按钮类型选用不同的系统图标。
		int actionIcon = icon == 0 ? (kind == ACTION_REPLY ? DEFAULT_REPLY_ACTION_ICON : DEFAULT_ACTION_ICON) : icon;
		actions.add(new ActionSpec(kind, actionIcon, title, mark, hint));
	}

	/**
	 * 设置通知首次出现时的状态栏提示，不影响通知标题。
	 *
	 * @param 提示 状态栏提示文字
	 */
	@SimpleFunction
	public void 置状态栏提示(String 提示) {
		builder.setTicker(提示);
	}

	/**
	 * 设置通知标题。
	 *
	 * @param 标题 通知标题
	 */
	@SimpleFunction
	public void 置标题(String 标题) {
		builder.setContentTitle(标题);
	}

	/**
	 * 设置通知正文。
	 *
	 * @param 内容 通知正文
	 */
	@SimpleFunction
	public void 置内容(String 内容) {
		builder.setContentText(内容);
	}

	/**
	 * 用指定资源更换当前通知小图标；未调用时保留内置图标。
	 *
	 * @param 图标资源 非零的图片资源编号
	 */
	@SimpleFunction
	public void 置小图标(int 图标资源) {
		if (图标资源 == 0) {
			throw new IllegalArgumentException("通知小图标资源编号不能为 0");
		}
		builder.setSmallIcon(图标资源);
	}

	/**
	 * 选择是否播放系统默认提示音。API 26 起声音由通知渠道控制。
	 *
	 * @param 播放提示音 是否使用有声通知渠道
	 */
	@SimpleFunction
	public void 置提示音(boolean 播放提示音) {
		// 保存设置供发送时选渠道；旧系统直接配置构建器。
		playSound = 播放提示音;
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
			applyLegacySound(播放提示音);
		}
	}

	/**
	 * 设置提醒优先级。API 16 至 25 设置构建器，API 26 起对应通知渠道；更早版本不生效。
	 *
	 * @param 优先级 使用本对象的优先级常量
	 */
	@SimpleFunction
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	public void 置优先级(int 优先级) {
		if (优先级 < 优先级_最低 || 优先级 > 优先级_最高) {
			throw new IllegalArgumentException("无效的通知优先级");
		}

		// 新系统在发送时按此值选渠道，旧系统直接设置通知优先级。
		priority = 优先级;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN
				&& Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
			applyLegacyPriority(优先级);
		}
	}

	/**
	 * 设置锁屏内容可见性；API 21 以下不支持此设置。
	 *
	 * @param 公开范围 使用本对象的公开范围常量
	 */
	@SimpleFunction
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public void 置公开范围(int 公开范围) {
		if (公开范围 != 公开范围_公开 && 公开范围 != 公开范围_私密 && 公开范围 != 公开范围_保密) {
			throw new IllegalArgumentException("无效的通知公开范围");
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			builder.setVisibility(公开范围);
		}
	}

	/**
	 * 设置通知是否不能通过滑动清除。
	 *
	 * @param 常驻 是否保持常驻
	 */
	@SimpleFunction
	public void 置常驻(boolean 常驻) {
		builder.setOngoing(常驻).setAutoCancel(!常驻);
	}

	/**
	 * 设置展开后的长文本样式；需要 API 16。
	 *
	 * @param 长文本 展开后显示的正文
	 */
	@SimpleFunction
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	public void 置长文本样式(String 长文本) {
		requireStyleSupport();
		builder.setStyle(new Notification.BigTextStyle().bigText(长文本));
	}

	/**
	 * 设置展开后的多行样式；需要 API 16。
	 *
	 * @param 内容行 各行内容，不能为 null
	 */
	@SimpleFunction
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	public void 置多行样式(String[] 内容行) {
		requireStyleSupport();
		if (内容行 == null) {
			throw new IllegalArgumentException("内容行不能为空");
		}

		// 每个数组元素对应展开区域中的一行。
		Notification.InboxStyle style = new Notification.InboxStyle();
		for (String line : 内容行) {
			style.addLine(line);
		}
		builder.setStyle(style);
	}

	/**
	 * 设置展开后的大图片样式；需要 API 16。
	 *
	 * @param 图片资源 可解码为位图的图片资源编号
	 */
	@SimpleFunction
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	public void 置大图片样式(int 图片资源) {
		requireStyleSupport();

		// 解码失败时立即报错，避免生成缺少图片的展开样式。
		Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), 图片资源);
		if (bitmap == null) {
			throw new IllegalArgumentException("无法读取通知图片资源");
		}
		builder.setStyle(new Notification.BigPictureStyle().bigPicture(bitmap));
	}

	/**
	 * 添加文本操作按钮。点击后由通知管理器发出按钮事件。
	 *
	 * @param 标题 按钮上显示的文字
	 * @param 标记 区分不同按钮的自定义标记
	 */
	@SimpleFunction
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	public void 添加按钮(String 标题, String 标记) {
		添加按钮(0, 标题, 标记);
	}

	/**
	 * 添加带图标的操作按钮，最多三个。
	 *
	 * @param 图标资源 按钮图标资源编号，0 表示使用内置操作图标
	 * @param 标题 按钮上显示的文字
	 * @param 标记 区分不同按钮的自定义标记
	 */
	@SimpleFunction
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	public void 添加按钮(int 图标资源, String 标题, String 标记) {
		addAction(ACTION_BUTTON, 图标资源, 标题, 标记, null);
	}

	/**
	 * 添加内联回复操作；需要 API 24。
	 *
	 * @param 标题 回复按钮的文字
	 * @param 标记 区分不同回复操作的自定义标记
	 * @param 输入提示 回复输入框的提示文字
	 */
	@SimpleFunction
	@TargetApi(Build.VERSION_CODES.N)
	public void 添加回复(String 标题, String 标记, String 输入提示) {
		添加回复(0, 标题, 标记, 输入提示);
	}

	/**
	 * 添加带图标的内联回复操作；需要 API 24。
	 *
	 * @param 图标资源 回复按钮图标资源编号，0 表示使用内置回复图标
	 * @param 标题 回复按钮的文字
	 * @param 标记 区分不同回复操作的自定义标记
	 * @param 输入提示 回复输入框的提示文字
	 */
	@SimpleFunction
	@TargetApi(Build.VERSION_CODES.N)
	public void 添加回复(int 图标资源, String 标题, String 标记, String 输入提示) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
			throw new UnsupportedOperationException("通知内联回复需要 Android API 24");
		}
		addAction(ACTION_REPLY, 图标资源, 标题, 标记, 输入提示);
	}

	/**
	 * 按 0 到 100 的百分比设置通知进度。
	 *
	 * @param 进度 当前完成百分比
	 */
	@SimpleFunction
	public void 置进度(int 进度) {
		置进度(100, 进度);
	}

	/**
	 * 设置最大进度和当前进度；当前值会限制在有效范围内。
	 *
	 * @param 最大进度 必须大于 0 的最大值
	 * @param 当前进度 当前值
	 */
	@SimpleFunction
	public void 置进度(int 最大进度, int 当前进度) {
		if (最大进度 <= 0) {
			throw new IllegalArgumentException("最大进度必须大于 0");
		}

		// 限制当前值；进度变化不改动当前小图标。
		builder.setProgress(最大进度, Math.max(0, Math.min(最大进度, 当前进度)), false);
	}

	/**
	 * 移除之前设置的进度条，保留当前小图标。
	 */
	@SimpleFunction
	public void 清除进度() {
		builder.setProgress(0, 0, false);
	}
}
