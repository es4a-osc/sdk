package simple.library.wsd.viewpager;

/* Simple类库标记类 */
import simple.runtime.annotations.SimpleComponent;
import simple.runtime.annotations.SimpleDataElement;
import simple.runtime.annotations.SimpleObject;
import simple.runtime.annotations.SimpleEvent;
import simple.runtime.annotations.SimpleProperty;
import simple.runtime.annotations.SimpleFunction;
/* Simple基础类 */
import simple.runtime.components.可视组件;
import simple.runtime.components.组件容器;

/**
 * 滑动页面框组件
 *
 * @author 树先生 xhwsd@qq.com
 */
@SimpleComponent
@SimpleObject
public interface 滑动页面框 extends 可视组件, 组件容器 {

	@SimpleDataElement
	static final int 滑动状态_未滑动 = 0;
	@SimpleDataElement
	static final int 滑动状态_正在滑动 = 1;
	@SimpleDataElement
	static final int 滑动状态_滑动完毕 = 2;

	/* 事件 */

	/**
	 * 当页面被滑动时触发此事件。
	 *
	 * @param position 页面索引。
	 * @param positionOffset 偏移百分比。
	 * @param positionOffsetPixels 偏移像素位置。
	 */
	@SimpleEvent
	void 页面被滑动(int position, float positionOffset, int positionOffsetPixels);

	/**
	 * 当页面滑动状态改变时触发此事件，返回滑动状态：0、未滑动 1、正在滑动 2、滑动完毕。
	 *
	 * @param state 滑动状态。
	 */
	@SimpleEvent
	void 滑动状态改变(int state);

	/**
	 * 当滑动页面后选中某个页面时触发此事件。
	 *
	 * @param position 页面索引。
	 */
	@SimpleEvent
	void 页面被选择(int position);

	/* 属性 */

	@SimpleProperty
	boolean 禁止滑动();

	@SimpleProperty(
		type = SimpleProperty.PROPERTY_TYPE_BOOLEAN,
		initializer = "False"
	)
	void 禁止滑动(boolean noScroll);

	@SimpleProperty
	boolean 切换特效();

	@SimpleProperty(
		type = SimpleProperty.PROPERTY_TYPE_BOOLEAN,
		initializer = "True"
	)
	void 切换特效(boolean isOpen);

	@SimpleProperty
	int 现行页面();

	@SimpleProperty
	void 现行页面(int position);

	/* 方法 */

	@SimpleFunction
	void 添加页面(可视组件 component);

	@SimpleFunction
	void 删除页面(int position);

	@SimpleFunction
	void 清空页面();

	@SimpleFunction
	int 取页面数();
}
