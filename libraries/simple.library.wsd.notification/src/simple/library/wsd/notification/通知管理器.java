package simple.library.wsd.notification;

import simple.runtime.annotations.SimpleComponent;
import simple.runtime.annotations.SimpleEvent;
import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;
import simple.runtime.annotations.UsesPermissions;
import simple.runtime.components.组件;

/**
 * 创建、发送和管理应用通知的非可视组件。
 * 通知点击与操作回调由当前窗口中的管理器实例接收。
 */
@SimpleObject
@SimpleComponent
@UsesPermissions(permissionNames = "android.permission.POST_NOTIFICATIONS")
public interface 通知管理器 extends 组件 {
	/**
	 * 创建使用内置小图标的通知构建器；可继续设置标题、正文和自定义图标。
	 *
	 * @return 可继续定制的通知构建器
	 */
	@SimpleFunction
	通知构建器 创建通知();

	/**
	 * 发布或更新通知。同一编号再次发送会更新已有通知。
	 *
	 * @param 通知编号 用于更新、点击事件和删除通知的编号
	 * @param 通知 已定制的通知构建器
	 */
	@SimpleFunction
	void 发送通知(int 通知编号, 通知构建器 通知);

	/**
	 * 删除指定编号的通知。
	 *
	 * @param 通知编号 要删除的通知编号
	 */
	@SimpleFunction
	void 删除通知(int 通知编号);

	/**
	 * 清除本应用发布的所有通知。
	 */
	@SimpleFunction
	void 清空通知();

	/**
	 * 用户单击此组件发送的通知时触发。
	 *
	 * @param 通知编号 被单击的通知编号
	 */
	@SimpleEvent
	void 通知被单击(int 通知编号);

	/**
	 * 用户点击通知操作按钮时触发；组件所在窗口仍需保持活动。
	 *
	 * @param 通知编号 发送时使用的通知编号
	 * @param 标记 添加按钮时设置的标记
	 */
	@SimpleEvent
	void 按钮被单击(int 通知编号, String 标记);

	/**
	 * 用户提交内联回复时触发；组件所在窗口仍需保持活动。
	 *
	 * @param 通知编号 发送时使用的通知编号
	 * @param 标记 添加回复时设置的标记
	 * @param 回复内容 用户提交的文字
	 */
	@SimpleEvent
	void 收到回复(int 通知编号, String 标记, String 回复内容);
}
