package simple.library.wsd.accessibility;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import android.annotation.TargetApi;
import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

/**
 * 由 Android 系统绑定的无障碍服务。
 * 服务负责转发事件和执行全局操作；窗口组件只订阅回调，不持有服务实例。
 */
public final class AccessibilityListener extends AccessibilityService {

	/** 服务诊断日志标识。 */
	private static final String TAG = "ES4AAccessibility";

	/** 可在回调期间安全增删订阅者的集合。 */
	private static final Set<Listener> LISTENERS = new CopyOnWriteArraySet<Listener>();

	/** 最近连接且尚未解绑的服务实例。 */
	private static volatile AccessibilityListener activeService;

	/** 服务事件订阅者；回调由无障碍服务分发。 */
	interface Listener {
		/**
		 * 转发 Android 无障碍事件。
		 *
		 * @param event 当前系统事件
		 */
		void onAccessibilityEvent(AccessibilityEvent event);

		/**
		 * 报告音量键方向，不消费原按键。
		 *
		 * @param increased 真为音量加键，假为音量减键
		 */
		void onVolumeChanged(boolean increased);
	}

	/**
	 * 注册组件订阅者；集合会去重。
	 *
	 * @param listener 要注册的组件订阅者
	 */
	static void addListener(Listener listener) {
		LISTENERS.add(listener);
	}

	/**
	 * 移除组件订阅者，防止窗口销毁后继续收到事件。
	 *
	 * @param listener 要移除的组件订阅者
	 */
	static void removeListener(Listener listener) {
		LISTENERS.remove(listener);
	}

	/**
	 * 读取已连接服务的活动窗口根节点。
	 *
	 * @return 根节点；服务或 API 不可用时为空值
	 */
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	static AccessibilityNodeInfo getActiveRoot() {
		AccessibilityListener service = activeService;
		return service != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN
				? service.getRootInActiveWindow() : null;
	}

	/**
	 * 执行服务全局操作。
	 *
	 * @param action Android 无障碍全局操作编号
	 * @return 操作已被服务接受时为真；服务或 API 不可用时为假
	 */
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	static boolean performAction(int action) {
		AccessibilityListener service = activeService;
		return service != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN
				&& service.performGlobalAction(action);
	}

	@Override
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
	protected void onServiceConnected() {
		super.onServiceConnected();

		// 资源 ID 查询需要显式请求上报视图 ID；保留系统配置的其它标志。
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
			AccessibilityServiceInfo serviceInfo = getServiceInfo();
			if (serviceInfo != null
					&& (serviceInfo.flags & AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS) == 0) {
				serviceInfo.flags |= AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;
				setServiceInfo(serviceInfo);
			}
		}

		// 只记录当前已连接实例，不在静态字段中缓存窗口节点。
		activeService = this;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		// 旧服务解绑时不能清掉随后连接的新实例。
		if (activeService == this) {
			activeService = null;
		}
		return super.onUnbind(intent);
	}

	@Override
	public void onDestroy() {
		if (activeService == this) {
			activeService = null;
		}
		super.onDestroy();
	}

	@Override
	public void onAccessibilityEvent(AccessibilityEvent event) {
		if (event == null) {
			return;
		}

		// 单个组件回调失败不影响其他组件接收同一事件。
		for (Listener listener : LISTENERS) {
			try {
				listener.onAccessibilityEvent(event);
			} catch (RuntimeException error) {
				Log.e(TAG, "无障碍事件处理失败", error);
			}
		}
	}

	@Override
	protected boolean onKeyEvent(KeyEvent event) {
		// 仅报告音量键按下，不消费按键，让系统继续处理音量。
		if (event != null && event.getAction() == KeyEvent.ACTION_DOWN) {
			int code = event.getKeyCode();
			if (code == KeyEvent.KEYCODE_VOLUME_UP || code == KeyEvent.KEYCODE_VOLUME_DOWN) {
				for (Listener listener : LISTENERS) {
					try {
						listener.onVolumeChanged(code == KeyEvent.KEYCODE_VOLUME_UP);
					} catch (RuntimeException error) {
						Log.e(TAG, "音量事件处理失败", error);
					}
				}
			}
		}
		return false;
	}

	@Override
	public void onInterrupt() {
		// 中断不等于解绑；服务重新提供事件时仍使用现有订阅者。
	}
}
