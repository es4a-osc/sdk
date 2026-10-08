package com.example.demo;

import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;

/**
 * 演示普通对象的实例函数和静态函数，不依赖 Android 界面。
 *
 * @author 树先生 xhwsd@qq.com
 */
@SimpleObject
public final class 演示对象 {

	/**
	 * Simple 的“创建 演示对象”调用此公开无参构造方法。
	 */
	public 演示对象() {
	}

	/**
	 * 实例函数：先创建对象，再通过实例调用。
	 *
	 * @param 文本 要添加前缀的文本
	 * @return 带有 [Simple] 前缀的文本
	 */
	@SimpleFunction
	public String 加前缀(String 文本) {
		return "[Simple] " + 文本;
	}

	/**
	 * 静态函数：通过类型调用，无需创建对象。
	 *
	 * @param 文本 要计算长度的文本
	 * @return 文本的 UTF-16 代码单元数
	 */
	@SimpleFunction
	public static int 取长度(String 文本) {
		return 文本.length();
	}
}
