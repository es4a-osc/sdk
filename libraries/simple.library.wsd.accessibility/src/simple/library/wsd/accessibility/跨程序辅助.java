package simple.library.wsd.accessibility;

import simple.runtime.annotations.ManifestNodes;
import simple.runtime.annotations.SimpleComponent;
import simple.runtime.annotations.SimpleEvent;
import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;
import simple.runtime.components.组件;

import java.util.List;

import android.graphics.Rect;
import android.view.accessibility.AccessibilityNodeInfo;

/**
 * 通过系统无障碍服务读取活动窗口、接收事件并操作节点的不可视组件。
 * 调用 {@link #初始化()} 后才订阅事件；系统授权及服务连接由 Android 管理。
 * 节点和节点列表以 Simple 对象传递，离开原窗口后可能失效，应重新读取。
 */
@SimpleComponent
@SimpleObject
@ManifestNodes(applicationXml =
	"<service" +
		" android:enabled=\"true\"" +
		" android:exported=\"true\"" +
		" android:label=\"@string/app_name\"" +
		" android:name=\"simple.library.wsd.accessibility.AccessibilityListener\"" +
		" android:permission=\"android.permission.BIND_ACCESSIBILITY_SERVICE\">" +
		"<intent-filter>" +
			"<action android:name=\"android.accessibilityservice.AccessibilityService\" />" +
		"</intent-filter>" +
		"<meta-data" +
			" android:name=\"android.accessibilityservice\"" +
			" android:resource=\"@xml/config\" />" +
	"</service>"
)
public interface 跨程序辅助 extends 组件 {

	/**
	 * 开始接收无障碍服务事件；服务尚未启用时打开系统无障碍设置。
	 */
	@SimpleFunction
	void 初始化();

	/**
	 * 无障碍服务收到节点滚动事件时触发。
	 */
	@SimpleEvent
	void 控件被滑动();

	/**
	 * 无障碍服务收到节点点击事件时触发。
	 */
	@SimpleEvent
	void 控件被单击();

	/**
	 * 无障碍服务收到节点长按事件时触发。
	 */
	@SimpleEvent
	void 控件被长按();

	/**
	 * 无障碍服务收到音量键按下事件时触发。
	 *
	 * @param 增大 真表示按下音量增大键，假表示按下音量减小键。
	 */
	@SimpleEvent
	void 音量被改变(boolean 增大);

	/**
	 * 无障碍服务收到窗口状态变化事件时触发。
	 */
	@SimpleEvent
	void 窗口被切换();

	/**
	 * 无障碍服务收到窗口内容变化事件时触发。
	 */
	@SimpleEvent
	void 窗口发生变化();

	/**
	 * 无障碍服务收到通知状态变化事件时触发。
	 *
	 * @param 消息 通知中的消息文本。
	 * @param 包名 发送通知的应用包名。
	 */
	@SimpleEvent
	void 通知栏消息被改变(String 消息, String 包名);

	/**
	 * 判断本应用的无障碍服务是否已在系统设置中启用；不保证服务已连接。
	 *
	 * @return 本应用的服务已在系统设置启用时为真；不表示服务一定已连接。
	 */
	@SimpleFunction
	boolean 是否已开启();

	/**
	 * 取得最近一次窗口切换事件报告的界面类名。
	 *
	 * @return 最近一次窗口切换事件的类名；尚未收到事件时为空文本。
	 */
	@SimpleFunction
	String 取当前类名();

	/**
	 * 执行无障碍服务的全局操作；动作是全局操作编号，不是键盘按键码。
	 *
	 * @param 动作 无障碍服务的全局操作编号。
	 * @return 已连接的服务接受该操作时为真，否则为假。
	 */
	@SimpleFunction
	boolean 执行全局动作(int 动作);

	/**
	 * 触发最近一次收到的通知的内容操作。
	 *
	 * @return 已发送通知操作时为真；没有可用操作或操作失效时为假。
	 */
	@SimpleFunction
	boolean 打开最近通知();

	/**
	 * 取得当前活动窗口的根无障碍节点；服务未连接或没有活动根节点时返回空值。
	 *
	 * @return 活动窗口的根节点；服务未连接或无法读取时为空值。
	 */
	@SimpleFunction
	AccessibilityNodeInfo 取根控件();

	/**
	 * 取得指定节点的父节点。
	 *
	 * @param 控件 要读取或操作的无障碍节点对象（AccessibilityNodeInfo）。
	 * @return 父节点；参数为空或没有父节点时为空值。
	 */
	@SimpleFunction
	AccessibilityNodeInfo 取父控件(AccessibilityNodeInfo 控件);

	/**
	 * 取得指定节点的直接子节点数量。
	 *
	 * @param 控件 要读取或操作的无障碍节点对象（AccessibilityNodeInfo）。
	 * @return 直接子节点数量；参数为空时为零。
	 */
	@SimpleFunction
	int 取子控件数(AccessibilityNodeInfo 控件);

	/**
	 * 按从零开始的索引取得直接子节点；索引无效时返回空值。
	 *
	 * @param 索引 从零开始的节点索引。
	 * @param 控件 要读取或操作的无障碍节点对象（AccessibilityNodeInfo）。
	 * @return 指定索引处的子节点；参数或索引无效时为空值。
	 */
	@SimpleFunction
	AccessibilityNodeInfo 取子控件(int 索引, AccessibilityNodeInfo 控件);

	/**
	 * 按名称保存当前窗口的节点引用；窗口切换时自动清除。
	 *
	 * @param 名称 保存或查找节点所用的名称。
	 * @param 控件 要读取或操作的无障碍节点对象（AccessibilityNodeInfo）。
	 */
	@SimpleFunction
	void 保存控件(String 名称, AccessibilityNodeInfo 控件);

	/**
	 * 按名称读取当前窗口中此前保存的节点引用。
	 *
	 * @param 名称 保存或查找节点所用的名称。
	 * @return 已保存的节点引用；名称不存在或窗口已切换时为空值。
	 */
	@SimpleFunction
	AccessibilityNodeInfo 读取控件(String 名称);

	/**
	 * 把标记保存到节点的附加信息；仅 Android API 19 及以上有效。
	 *
	 * @param 标记 保存到节点附加信息中的文本。
	 * @param 控件 要读取或操作的无障碍节点对象（AccessibilityNodeInfo）。
	 */
	@SimpleFunction
	void 设置标记(String 标记, AccessibilityNodeInfo 控件);

	/**
	 * 取得节点附加信息中的标记；不存在时返回空文本。
	 *
	 * @param 控件 要读取或操作的无障碍节点对象（AccessibilityNodeInfo）。
	 * @return 节点标记；节点为空、版本不支持或未设置时为空文本。
	 */
	@SimpleFunction
	String 取标记(AccessibilityNodeInfo 控件);

	/**
	 * 判断节点是否声明可点击。
	 *
	 * @param 控件 要读取或操作的无障碍节点对象（AccessibilityNodeInfo）。
	 * @return 节点存在且声明可点击时为真。
	 */
	@SimpleFunction
	boolean 是否可点击(AccessibilityNodeInfo 控件);

	/**
	 * 判断节点是否声明可滚动。
	 *
	 * @param 控件 要读取或操作的无障碍节点对象（AccessibilityNodeInfo）。
	 * @return 节点存在且声明可滚动时为真。
	 */
	@SimpleFunction
	boolean 是否可滑动(AccessibilityNodeInfo 控件);

	/**
	 * 判断节点是否声明可长按。
	 *
	 * @param 控件 要读取或操作的无障碍节点对象（AccessibilityNodeInfo）。
	 * @return 节点存在且声明可长按时为真。
	 */
	@SimpleFunction
	boolean 是否可长按(AccessibilityNodeInfo 控件);

	/**
	 * 对节点执行向前滚动操作。
	 *
	 * @param 控件 要读取或操作的无障碍节点对象（AccessibilityNodeInfo）。
	 * @return 节点接受该操作时为真，否则为假。
	 */
	@SimpleFunction
	boolean 正向滑动控件(AccessibilityNodeInfo 控件);

	/**
	 * 对节点执行向后滚动操作。
	 *
	 * @param 控件 要读取或操作的无障碍节点对象（AccessibilityNodeInfo）。
	 * @return 节点接受该操作时为真，否则为假。
	 */
	@SimpleFunction
	boolean 反向滑动控件(AccessibilityNodeInfo 控件);

	/**
	 * 对指定节点的父节点执行点击操作，并返回是否执行成功。
	 *
	 * @param 控件 要读取或操作的无障碍节点对象（AccessibilityNodeInfo）。
	 * @return 父节点接受点击操作时为真。
	 */
	@SimpleFunction
	boolean 单击父控件(AccessibilityNodeInfo 控件);

	/**
	 * 直接对指定节点执行点击操作，并返回是否执行成功。
	 *
	 * @param 控件 要读取或操作的无障碍节点对象（AccessibilityNodeInfo）。
	 * @return 指定节点接受点击操作时为真。
	 */
	@SimpleFunction
	boolean 单击控件(AccessibilityNodeInfo 控件);

	/**
	 * 对指定节点的父节点执行长按操作。
	 *
	 * @param 控件 要读取或操作的无障碍节点对象（AccessibilityNodeInfo）。
	 * @return 父节点接受长按操作时为真，否则为假。
	 */
	@SimpleFunction
	boolean 长按父控件(AccessibilityNodeInfo 控件);

	/**
	 * 直接对指定节点执行长按操作。
	 *
	 * @param 控件 要读取或操作的无障碍节点对象（AccessibilityNodeInfo）。
	 * @return 指定节点接受长按操作时为真，否则为假。
	 */
	@SimpleFunction
	boolean 长按控件(AccessibilityNodeInfo 控件);

	/**
	 * 对指定节点执行获取焦点操作。
	 *
	 * @param 控件 要读取或操作的无障碍节点对象（AccessibilityNodeInfo）。
	 * @return 节点接受焦点操作时为真，否则为假。
	 */
	@SimpleFunction
	boolean 获取控件焦点(AccessibilityNodeInfo 控件);

	/**
	 * 设置节点文本；仅 Android API 21 及以上有效。
	 *
	 * @param 控件 要读取或操作的无障碍节点对象（AccessibilityNodeInfo）。
	 * @param 内容 要设置或查找的文本。
	 * @return 节点接受设置文本操作时为真，否则为假。
	 */
	@SimpleFunction
	boolean 置控件内容(AccessibilityNodeInfo 控件, String 内容);

	/**
	 * 对节点执行粘贴操作；仅 Android API 18 及以上有效。
	 *
	 * @param 控件 要读取或操作的无障碍节点对象（AccessibilityNodeInfo）。
	 * @return 节点接受粘贴操作时为真，否则为假。
	 */
	@SimpleFunction
	boolean 粘贴(AccessibilityNodeInfo 控件);

	/**
	 * 对节点执行复制操作；仅 Android API 18 及以上有效。
	 *
	 * @param 控件 要读取或操作的无障碍节点对象（AccessibilityNodeInfo）。
	 * @return 节点接受复制操作时为真，否则为假。
	 */
	@SimpleFunction
	boolean 复制(AccessibilityNodeInfo 控件);

	/**
	 * 遍历当前活动窗口的后代节点并清空可编辑内容；仅 Android API 21 及以上有效。
	 */
	@SimpleFunction
	void 清空所有编辑框();

	/**
	 * 遍历当前活动窗口的节点，逐个触发长按并记录结果；会实际操作界面。
	 */
	@SimpleFunction
	void 尝试长按所有控件();

	/**
	 * 深度优先搜索起点的后代节点，返回内容描述包含指定文本的节点列表；不包含起点。
	 *
	 * @param 控件 要读取或操作的无障碍节点对象（AccessibilityNodeInfo）。
	 * @param 内容 要设置或查找的文本。
	 * @return 匹配的后代节点列表；起点为空或查询文本为空时为空列表。
	 */
	@SimpleFunction
	List<AccessibilityNodeInfo> 按内容描述查找控件(AccessibilityNodeInfo 控件, String 内容);

	/**
	 * 按视图资源 ID 搜索当前活动窗口中的节点；根节点不可用或 Android API 低于 18 时返回空值。
	 *
	 * @param 资源ID 要匹配的视图资源 ID。
	 * @return 匹配的节点列表；根节点不可用或版本不支持时为空值。
	 */
	@SimpleFunction
	List<AccessibilityNodeInfo> 按资源ID查找控件(String 资源ID);

	/**
	 * 按文本搜索当前活动窗口中的节点；根节点不可用时返回空值。
	 *
	 * @param 文本 要搜索的节点文本。
	 * @return 匹配的节点列表；根节点不可用时为空值。
	 */
	@SimpleFunction
	List<AccessibilityNodeInfo> 按文本查找控件(String 文本);

	/**
	 * 按视图资源 ID 搜索指定节点及其子树中的节点；节点为空或 Android API 低于 18 时返回空值。
	 *
	 * @param 资源ID 要匹配的视图资源 ID。
	 * @param 控件 要读取或操作的无障碍节点对象（AccessibilityNodeInfo）。
	 * @return 匹配的节点列表；起点为空或版本不支持时为空值。
	 */
	@SimpleFunction
	List<AccessibilityNodeInfo> 在控件内按资源ID查找控件(String 资源ID, AccessibilityNodeInfo 控件);

	/**
	 * 取得节点列表中的项目数量；列表为空时返回零。
	 *
	 * @param 控件集合 无障碍节点列表对象（List&lt;AccessibilityNodeInfo&gt;）。
	 * @return 列表大小；列表为空值时为零。
	 */
	@SimpleFunction
	int 取控件集合项目数(List<AccessibilityNodeInfo> 控件集合);

	/**
	 * 按从零开始的索引取得节点列表中的节点；无效时返回空值。
	 *
	 * @param 索引 从零开始的节点索引。
	 * @param 控件集合 无障碍节点列表对象（List&lt;AccessibilityNodeInfo&gt;）。
	 * @return 指定索引处的节点；参数或索引无效时为空值。
	 */
	@SimpleFunction
	AccessibilityNodeInfo 取控件(int 索引, List<AccessibilityNodeInfo> 控件集合);

	/**
	 * 取得节点的视图资源 ID；仅 Android API 18 及以上可读取。
	 *
	 * @param 控件 要读取或操作的无障碍节点对象（AccessibilityNodeInfo）。
	 * @return 视图资源 ID；节点为空、无 ID 或版本不支持时为空文本。
	 */
	@SimpleFunction
	String 取控件资源ID(AccessibilityNodeInfo 控件);

	/**
	 * 取得节点的内容描述。
	 *
	 * @param 控件 要读取或操作的无障碍节点对象（AccessibilityNodeInfo）。
	 * @return 内容描述；节点为空或没有描述时为空文本。
	 */
	@SimpleFunction
	String 取控件内容描述(AccessibilityNodeInfo 控件);

	/**
	 * 取得节点显示的文本。
	 *
	 * @param 控件 要读取或操作的无障碍节点对象（AccessibilityNodeInfo）。
	 * @return 节点文本；节点为空或没有文本时为空文本。
	 */
	@SimpleFunction
	String 取控件内容(AccessibilityNodeInfo 控件);

	/**
	 * 取得节点在屏幕上的边界矩形，返回 Android Rect 对象。
	 *
	 * @param 控件 要读取或操作的无障碍节点对象（AccessibilityNodeInfo）。
	 * @return 节点的屏幕边界矩形；节点为空时为坐标均为零的矩形。
	 */
	@SimpleFunction
	Rect 取控件坐标(AccessibilityNodeInfo 控件);

	/**
	 * 取得边界矩形的左边界坐标。
	 *
	 * @param 坐标 取控件坐标返回的边界矩形对象（Rect）。
	 * @return 左边界坐标；矩形为空时为零。
	 */
	@SimpleFunction
	int 取左边界(Rect 坐标);

	/**
	 * 取得边界矩形的上边界坐标。
	 *
	 * @param 坐标 取控件坐标返回的边界矩形对象（Rect）。
	 * @return 上边界坐标；矩形为空时为零。
	 */
	@SimpleFunction
	int 取上边界(Rect 坐标);

	/**
	 * 取得边界矩形的右边界坐标。
	 *
	 * @param 坐标 取控件坐标返回的边界矩形对象（Rect）。
	 * @return 右边界坐标；矩形为空时为零。
	 */
	@SimpleFunction
	int 取右边界(Rect 坐标);

	/**
	 * 取得边界矩形的下边界坐标。
	 *
	 * @param 坐标 取控件坐标返回的边界矩形对象（Rect）。
	 * @return 下边界坐标；矩形为空时为零。
	 */
	@SimpleFunction
	int 取下边界(Rect 坐标);

	/**
	 * 将指定节点的信息编码为 JSON 文本；节点为空时返回空文本。
	 *
	 * @param 控件 要读取或操作的无障碍节点对象（AccessibilityNodeInfo）。
	 * @return JSON 对象文本；节点为空或读取失败时为空文本。
	 */
	@SimpleFunction
	String 取控件信息(AccessibilityNodeInfo 控件);

	/**
	 * 将当前活动窗口后代节点的信息编码为 JSON 数组文本。
	 *
	 * @return 后代节点的 JSON 数组文本；没有活动根节点时为空文本。
	 */
	@SimpleFunction
	String 取所有控件信息();
}
