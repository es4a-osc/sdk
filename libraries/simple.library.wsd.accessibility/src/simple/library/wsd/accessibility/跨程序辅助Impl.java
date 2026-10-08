package simple.library.wsd.accessibility;

import simple.runtime.android.MainActivity;
import simple.runtime.components.组件容器;
import simple.runtime.components.impl.组件Impl;
import simple.runtime.events.EventDispatcher;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.Settings;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

/**
 * 无障碍服务的 Simple 组件适配层。
 * 组件按需订阅服务事件，活动窗口节点每次都从已连接的服务读取。
 */
public final class 跨程序辅助Impl extends 组件Impl implements 跨程序辅助, AccessibilityListener.Listener, MainActivity.OnDestroyListener {

	/** 日志标识与节点附加信息中使用的私有键。 */
	private static final String TAG = "ES4AAccessibility";
	private static final String MARK_KEY = "标记";

	/** 组件所属窗口，用于打开设置页和注销生命周期回调。 */
	private final MainActivity activity;

	/** 调用者按名称暂存的节点引用；窗口变化后可能失效。 */
	private final Map<String, AccessibilityNodeInfo> savedNodes = new HashMap<String, AccessibilityNodeInfo>();

	/** 最近一次窗口切换事件的类名。 */
	private volatile String currentClassName = "";

	/** 最近一次通知事件中可执行的内容操作。 */
	private volatile PendingIntent notificationIntent;

	/** 本组件是否已注册为无障碍服务订阅者。 */
	private boolean listening;

	/**
	 * 注册窗口销毁回调，以便释放事件订阅和已保存的节点引用。
	 *
	 * @param container 组件所属容器
	 */
	public 跨程序辅助Impl(组件容器 container) {
		super(container);
		activity = MainActivity.getContext();
		activity.addOnDestroyListener(this);
	}

	@Override
	public void 初始化() {
		// 重复调用只注册一次；系统授权与组件订阅是两个独立步骤。
		if (!listening) {
			AccessibilityListener.addListener(this);
			listening = true;
		}

		if (!是否已开启()) {
			try {
				activity.startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
			} catch (RuntimeException error) {
				Log.e(TAG, "无法打开无障碍设置", error);
			}
		}
	}

	@Override
	public void onDestroy() {
		// MainActivity 正在遍历销毁监听器，此处不能从该列表移除自身。
		AccessibilityListener.removeListener(this);
		savedNodes.clear();
		notificationIntent = null;
		listening = false;
	}

	@Override
	public boolean 是否已开启() {
		if (Settings.Secure.getInt(activity.getContentResolver(),
				Settings.Secure.ACCESSIBILITY_ENABLED, 0) != 1) {
			return false;
		}
		String enabled = Settings.Secure.getString(activity.getContentResolver(),
				Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
		if (enabled == null) {
			return false;
		}

		// 系统设置记录的是组件名；不把总开关当作本服务已授权。
		String serviceName = new ComponentName(activity, AccessibilityListener.class).flattenToString();
		for (String name : enabled.split(":")) {
			if (serviceName.equalsIgnoreCase(name)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void onAccessibilityEvent(AccessibilityEvent event) {
		// 每类系统事件只映射到相应的 Simple 事件。
		switch (event.getEventType()) {
			case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
				// 旧窗口的节点引用已经失效，不继续向调用者返回它们。
				savedNodes.clear();
				currentClassName = asString(event.getClassName());
				窗口被切换();
				break;
			case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED:
				窗口发生变化();
				break;
			case AccessibilityEvent.TYPE_VIEW_SCROLLED:
				控件被滑动();
				break;
			case AccessibilityEvent.TYPE_VIEW_CLICKED:
				控件被单击();
				break;
			case AccessibilityEvent.TYPE_VIEW_LONG_CLICKED:
				控件被长按();
				break;
			case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED:
				onNotificationChanged(event);
				break;
			default:
				break;
		}
	}

	@Override
	public void onVolumeChanged(boolean increased) {
		音量被改变(increased);
	}

	/**
	 * 提取通知文本和可执行的内容操作，再转发给组件事件。
	 *
	 * @param event 通知状态变化事件
	 */
	@TargetApi(Build.VERSION_CODES.KITKAT)
	private void onNotificationChanged(AccessibilityEvent event) {
		Parcelable data = event.getParcelableData();
		Notification notification = data instanceof Notification ? (Notification) data : null;
		if (notification != null) {
			notificationIntent = notification.contentIntent;
		}

		// 事件来源才是发送通知的应用；点击意图可能由另一应用创建。
		String packageName = asString(event.getPackageName());

		// 通知正文通常位于 Notification.extras；事件文本可能为空或只含摘要。
		String message = "";
		if (notification != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT
				&& notification.extras != null) {
			Bundle extras = notification.extras;
			message = asString(extras.getCharSequence(Notification.EXTRA_BIG_TEXT));
			if (message.isEmpty()) {
				message = asString(extras.getCharSequence(Notification.EXTRA_TEXT));
			}
		}

		// 旧版通知或未提供正文的通知仍可从无障碍事件中提取文本。
		if (message.isEmpty()) {
			List<CharSequence> text = event.getText();
			for (int index = text.size() - 1; index >= 0; index--) {
				message = asString(text.get(index));
				if (!message.isEmpty()) {
					break;
				}
			}
		}
		通知栏消息被改变(message, packageName);
	}

	/**
	 * 统一把 Android 可空文本转换为 Simple 可用的非空文本。
	 *
	 * @param value Android 文本，可为空值
	 * @return 原文本的字符串形式，空值转为空文本
	 */
	private static String asString(CharSequence value) {
		return value == null ? "" : value.toString();
	}

	@Override
	public String 取当前类名() {
		return currentClassName;
	}

	@Override
	public boolean 执行全局动作(int action) {
		return AccessibilityListener.performAction(action);
	}

	@Override
	public boolean 打开最近通知() {
		// 执行最近一次通知的内容意图，不负责展开系统通知栏。
		PendingIntent intent = notificationIntent;
		if (intent == null) {
			return false;
		}
		try {
			intent.send();
			return true;
		} catch (PendingIntent.CanceledException error) {
			Log.w(TAG, "通知操作已失效", error);
			return false;
		}
	}

	@Override
	public AccessibilityNodeInfo 取根控件() {
		return AccessibilityListener.getActiveRoot();
	}

	@Override
	public AccessibilityNodeInfo 取父控件(AccessibilityNodeInfo node) {
		return node == null ? null : node.getParent();
	}

	@Override
	public int 取子控件数(AccessibilityNodeInfo node) {
		return node == null ? 0 : node.getChildCount();
	}

	@Override
	public AccessibilityNodeInfo 取子控件(int index, AccessibilityNodeInfo node) {
		return node == null || index < 0 || index >= node.getChildCount()
				? null : node.getChild(index);
	}

	@Override
	public void 保存控件(String name, AccessibilityNodeInfo node) {
		// 仅保存引用；窗口变化后调用者须重新取得节点。
		savedNodes.put(name, node);
	}

	@Override
	public AccessibilityNodeInfo 读取控件(String name) {
		return savedNodes.get(name);
	}

	@Override
	@TargetApi(Build.VERSION_CODES.KITKAT)
	public void 设置标记(String mark, AccessibilityNodeInfo node) {
		if (node != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			node.getExtras().putString(MARK_KEY, mark);
		}
	}

	@Override
	@TargetApi(Build.VERSION_CODES.KITKAT)
	public String 取标记(AccessibilityNodeInfo node) {
		if (node == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
			return "";
		}
		String mark = node.getExtras().getString(MARK_KEY);
		return mark == null ? "" : mark;
	}

	@Override
	public boolean 是否可点击(AccessibilityNodeInfo node) {
		return node != null && node.isClickable();
	}

	@Override
	public boolean 是否可滑动(AccessibilityNodeInfo node) {
		return node != null && node.isScrollable();
	}

	@Override
	public boolean 是否可长按(AccessibilityNodeInfo node) {
		return node != null && node.isLongClickable();
	}

	/**
	 * 执行节点动作并隔离已失效节点带来的运行时异常。
	 *
	 * @param node 目标节点
	 * @param action Android 节点操作编号
	 * @return 节点接受操作时为真，节点为空或操作失败时为假
	 */
	private static boolean performAction(AccessibilityNodeInfo node, int action) {
		if (node == null) {
			return false;
		}
		try {
			return node.performAction(action);
		} catch (RuntimeException error) {
			Log.w(TAG, "控件操作失败", error);
			return false;
		}
	}

	@Override
	public boolean 正向滑动控件(AccessibilityNodeInfo node) {
		return performAction(node, AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
	}

	@Override
	public boolean 反向滑动控件(AccessibilityNodeInfo node) {
		return performAction(node, AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);
	}

	@Override
	public boolean 单击父控件(AccessibilityNodeInfo node) {
		return performAction(取父控件(node), AccessibilityNodeInfo.ACTION_CLICK);
	}

	@Override
	public boolean 单击控件(AccessibilityNodeInfo node) {
		return performAction(node, AccessibilityNodeInfo.ACTION_CLICK);
	}

	@Override
	public boolean 长按父控件(AccessibilityNodeInfo node) {
		return performAction(取父控件(node), AccessibilityNodeInfo.ACTION_LONG_CLICK);
	}

	@Override
	public boolean 长按控件(AccessibilityNodeInfo node) {
		return performAction(node, AccessibilityNodeInfo.ACTION_LONG_CLICK);
	}

	@Override
	public boolean 获取控件焦点(AccessibilityNodeInfo node) {
		return performAction(node, AccessibilityNodeInfo.ACTION_FOCUS);
	}

	@Override
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
	public boolean 粘贴(AccessibilityNodeInfo node) {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2
				&& performAction(node, AccessibilityNodeInfo.ACTION_PASTE);
	}

	@Override
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
	public boolean 复制(AccessibilityNodeInfo node) {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2
				&& performAction(node, AccessibilityNodeInfo.ACTION_COPY);
	}

	@Override
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public boolean 置控件内容(AccessibilityNodeInfo node, String content) {
		if (node == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
			return false;
		}

		// 设置文本需要带参数的动作，不能与无参数动作共用 performAction。
		Bundle arguments = new Bundle();
		arguments.putCharSequence(
				AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, content);
		try {
			return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);
		} catch (RuntimeException error) {
			Log.w(TAG, "设置控件内容失败", error);
			return false;
		}
	}

	/** 仅在遍历过程中使用的节点访问操作。 */
	private interface NodeVisitor {
		/**
		 * 处理当前节点；异常由遍历器隔离。
		 *
		 * @param node 当前节点
		 */
		void visit(AccessibilityNodeInfo node);
	}

	/**
	 * 按子节点原有顺序深度优先遍历。
	 * 节点处理失败不应阻断其子树或其他节点；是否访问起点由调用者决定。
	 *
	 * @param start 遍历起点，为空时不调用访问器
	 * @param includeStart 是否访问起点本身
	 * @param visitor 节点访问器
	 */
	private static void visitTree(AccessibilityNodeInfo start, boolean includeStart,
			NodeVisitor visitor) {
		if (start == null) {
			return;
		}
		ArrayDeque<AccessibilityNodeInfo> stack = new ArrayDeque<AccessibilityNodeInfo>();
		stack.push(start);
		while (!stack.isEmpty()) {
			AccessibilityNodeInfo node = stack.pop();
			if (includeStart || node != start) {
				try {
					visitor.visit(node);
				} catch (RuntimeException error) {
					Log.w(TAG, "处理控件失败", error);
				}
			}

			// 逆序入栈，使访问顺序仍与界面上的子节点顺序一致。
			int childCount;
			try {
				childCount = node.getChildCount();
			} catch (RuntimeException error) {
				Log.w(TAG, "读取控件子节点数失败", error);
				continue;
			}
			for (int index = childCount - 1; index >= 0; index--) {
				try {
					AccessibilityNodeInfo child = node.getChild(index);
					if (child != null) {
						stack.push(child);
					}
				} catch (RuntimeException error) {
					Log.w(TAG, "读取控件子节点失败", error);
				}
			}
		}
	}

	@Override
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public void 清空所有编辑框() {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
			return;
		}

		// 从活动窗口的后代中筛选可编辑节点，保持根节点本身不被清空。
		visitTree(取根控件(), false, new NodeVisitor() {
			@Override
			public void visit(AccessibilityNodeInfo node) {
				CharSequence className = node.getClassName();
				if (node.isEditable()
						|| className != null && className.toString().contains("EditText")) {
					置控件内容(node, "");
				}
			}
		});
	}

	@Override
	public List<AccessibilityNodeInfo> 按内容描述查找控件(AccessibilityNodeInfo start,
			final String content) {
		final List<AccessibilityNodeInfo> matches = new ArrayList<AccessibilityNodeInfo>();
		if (content == null) {
			return matches;
		}

		visitTree(start, false, new NodeVisitor() {
			@Override
			public void visit(AccessibilityNodeInfo node) {
				CharSequence description = node.getContentDescription();
				if (description != null && description.toString().contains(content)) {
					matches.add(node);
				}
			}
		});
		return matches;
	}

	@Override
	public void 尝试长按所有控件() {
		visitTree(取根控件(), true, new NodeVisitor() {
			private int count;

			@Override
			public void visit(AccessibilityNodeInfo node) {
				count++;
				Log.d(TAG, count + ": " + performAction(node,
						AccessibilityNodeInfo.ACTION_LONG_CLICK) + " " + node.getClassName());
			}
		});
	}

	@Override
	public List<AccessibilityNodeInfo> 按资源ID查找控件(String viewId) {
		return 在控件内按资源ID查找控件(viewId, 取根控件());
	}

	@Override
	public List<AccessibilityNodeInfo> 按文本查找控件(String text) {
		AccessibilityNodeInfo root = 取根控件();
		return root == null || text == null
				? null : root.findAccessibilityNodeInfosByText(text);
	}

	@Override
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
	public List<AccessibilityNodeInfo> 在控件内按资源ID查找控件(String viewId,
			AccessibilityNodeInfo node) {
		return node == null || viewId == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2
				? null : node.findAccessibilityNodeInfosByViewId(viewId);
	}

	@Override
	public int 取控件集合项目数(List<AccessibilityNodeInfo> nodes) {
		return nodes == null ? 0 : nodes.size();
	}

	@Override
	public AccessibilityNodeInfo 取控件(int index, List<AccessibilityNodeInfo> nodes) {
		return nodes == null || index < 0 || index >= nodes.size()
				? null : nodes.get(index);
	}

	@Override
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
	public String 取控件资源ID(AccessibilityNodeInfo node) {
		return node == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2
				? "" : asString(node.getViewIdResourceName());
	}

	@Override
	public String 取控件内容描述(AccessibilityNodeInfo node) {
		return node == null ? "" : asString(node.getContentDescription());
	}

	@Override
	public String 取控件内容(AccessibilityNodeInfo node) {
		return node == null ? "" : asString(node.getText());
	}

	@Override
	public Rect 取控件坐标(AccessibilityNodeInfo node) {
		Rect bounds = new Rect();
		if (node != null) {
			node.getBoundsInScreen(bounds);
		}
		return bounds;
	}

	@Override
	public int 取左边界(Rect bounds) {
		return bounds == null ? 0 : bounds.left;
	}

	@Override
	public int 取上边界(Rect bounds) {
		return bounds == null ? 0 : bounds.top;
	}

	@Override
	public int 取右边界(Rect bounds) {
		return bounds == null ? 0 : bounds.right;
	}

	@Override
	public int 取下边界(Rect bounds) {
		return bounds == null ? 0 : bounds.bottom;
	}

	/**
	 * 把节点身份、边界、状态及当前系统支持的扩展字段编码成 JSON。
	 *
	 * @param node 要读取的节点
	 * @return 节点信息对象
	 * @throws JSONException 写入 JSON 字段失败时抛出
	 */
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	private static JSONObject describeNode(AccessibilityNodeInfo node) throws JSONException {
		Rect bounds = new Rect();
		node.getBoundsInScreen(bounds);

		JSONObject result = new JSONObject();
		result.put("Content-DESC", node.getContentDescription());
		result.put("Package", node.getPackageName());
		result.put("Class", node.getClassName());
		result.put("Id", Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2
				? node.getViewIdResourceName() : JSONObject.NULL);
		result.put("Text", node.getText());
		result.put("Bounds", "[" + bounds.top + "," + bounds.left + "]["
				+ bounds.bottom + "," + bounds.right + "]");

		result.put("Selected", node.isSelected());
		result.put("Checkable", node.isCheckable());
		result.put("Checked", node.isChecked());
		result.put("Clickable", node.isClickable());
		result.put("Enable", node.isEnabled());
		result.put("Focusable", node.isFocusable());
		result.put("Focused", node.isFocused());
		result.put("Scrollable", node.isScrollable());
		result.put("Long-Clickable", node.isLongClickable());
		result.put("Password", node.isPassword());

		// 较新系统才提供的字段保持原有缺省值，避免在旧系统调用不存在的 API。
		result.put("Editable", Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 && node.isEditable());
		result.put("Visible", Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN && node.isVisibleToUser());
		result.put("ChildCount", node.getChildCount());
		result.put("Error", Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ? node.getError() : JSONObject.NULL);
		result.put("InputType", Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT ? node.getInputType() : 0);
		result.put("MaxTextLength", Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
				? node.getMaxTextLength() : -1);
		result.put("TextSelectionStart", Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2
				? node.getTextSelectionStart() : -1);
		result.put("TextSelectionEnd", Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2
				? node.getTextSelectionEnd() : -1);
		return result;
	}

	@Override
	public String 取控件信息(AccessibilityNodeInfo node) {
		if (node == null) {
			return "";
		}
		try {
			return describeNode(node).toString();
		} catch (JSONException error) {
			Log.w(TAG, "生成控件信息失败", error);
			return "";
		} catch (RuntimeException error) {
			Log.w(TAG, "读取控件信息失败", error);
			return "";
		}
	}

	@Override
	public String 取所有控件信息() {
		AccessibilityNodeInfo root = 取根控件();
		if (root == null) {
			return "";
		}

		final JSONArray result = new JSONArray();
		visitTree(root, false, new NodeVisitor() {
			@Override
			public void visit(AccessibilityNodeInfo node) {
				try {
					result.put(describeNode(node));
				} catch (JSONException error) {
					Log.w(TAG, "生成控件信息失败", error);
				}
			}
		});
		return result.toString();
	}

	@Override
	public void 控件被滑动() {
		EventDispatcher.dispatchEvent(this, "控件被滑动");
	}

	@Override
	public void 控件被单击() {
		EventDispatcher.dispatchEvent(this, "控件被单击");
	}

	@Override
	public void 控件被长按() {
		EventDispatcher.dispatchEvent(this, "控件被长按");
	}

	@Override
	public void 音量被改变(boolean increased) {
		EventDispatcher.dispatchEvent(this, "音量被改变", increased);
	}

	@Override
	public void 窗口被切换() {
		EventDispatcher.dispatchEvent(this, "窗口被切换");
	}

	@Override
	public void 窗口发生变化() {
		EventDispatcher.dispatchEvent(this, "窗口发生变化");
	}

	@Override
	public void 通知栏消息被改变(String message, String packageName) {
		EventDispatcher.dispatchEvent(this, "通知栏消息被改变", message, packageName);
	}
}
