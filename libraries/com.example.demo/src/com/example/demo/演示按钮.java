package com.example.demo;

import simple.runtime.annotations.SimpleComponent;
import simple.runtime.annotations.SimpleEvent;
import simple.runtime.annotations.SimpleObject;
import simple.runtime.annotations.SimpleProperty;
import simple.runtime.components.可视组件;

/**
 * 演示通过注解向 Simple 公开可视组件、属性和事件。
 *
 * @author 树先生 xhwsd@qq.com
 */
@SimpleComponent
@SimpleObject
public interface 演示按钮 extends 可视组件 {

	/**
	 * 用户单击按钮时触发，由 Android 实现类分派。
	 */
	@SimpleEvent
	void 被单击();

	/**
	 * 读取按钮显示的文本。
	 */
	@SimpleProperty
	String 标题();

	/**
	 * 设置按钮显示的文本；运行库根据设置器上的注解初始化为空文本。
	 *
	 * @param value 按钮显示的文本
	 */
	@SimpleProperty(
		type = SimpleProperty.PROPERTY_TYPE_STRING,
		initializer = "\"\""
	)
	void 标题(String value);
}
