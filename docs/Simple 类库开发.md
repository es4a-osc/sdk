# Simple 类库开发

文档类型：Java 实现与交付指南。核对日期：2026-10-12；仓库结构与本地清单补充核对：2026-10-14。适用范围：当前 SDK 的扩展对象和 Android 组件。依据：独立类库仓库的演示源码、构建发布脚本与清单，并核对当前 RuntimeLoader、Compiler 和运行库注解实现。

本文按“准备工程 → 编写 Java → 生成 JAR → 填写清单 → 注册与验证”组织，包含完成基本扩展类库所需的步骤和示例，可独立阅读。

Simple 类库包括运行库和扩展库。运行库提供基础对象、组件与注解，随 SDK 提供 `SimpleAndroidRuntime.jar`；扩展库在此基础上提供自己的 `classes.jar`。本文的注解与类型规则也适用于运行库成员，但目录、构建和交付流程针对扩展库，不是运行库源码构建说明。

| 当前任务 | 阅读章节 |
| --- | --- |
| 创建工程、准备依赖 | [准备类库工程](#准备类库工程) |
| 编写普通对象、函数、属性或事件 | [注解映射](#注解映射)、[普通对象](#普通对象) |
| 编写可视组件或容器 | [Android 组件](#android-组件) |
| 声明权限或额外 Android 节点 | [Android 权限](#android-权限)、[Android 清单节点](#android-清单节点) |
| 生成 JAR、接入 SDK 并交付 | [构建 classes.jar](#构建-classesjar)、[填写类库清单](#填写类库清单)、[安装与接入 SDK](#安装与接入-sdk)、[验证与发布](#验证与发布) |

## 准备类库工程

### 环境与依赖

获取 [SDK 仓库](https://gitee.com/es4a/sdk) 与 [类库仓库](https://gitee.com/es4a/libraries)，将两者同级放置，按 [SDK README](../README.md) 准备 JDK 和 Android 工具。构建脚本由类库仓库提供，在 Windows 上使用 `javac` 和 `jar`，无需 Ant。执行前确认以下 SDK 文件存在：

| SDK 内路径 | 用途 |
| --- | --- |
| `tools/jdk1.8.0_503/bin/javac.exe`、`jar.exe` | JDK 8 编译和打包工具 |
| `tools/android/platforms/android-26/android.jar` | Android API 26 编译依赖 |
| `simple/SimpleAndroidRuntime.jar` | Simple 注解、类型与组件基础实现 |

Java 源文件使用 UTF-8，路径与包名对应。例如 `com.example.demo.演示对象` 放在类库目录的 `src/com/example/demo/演示对象.java`。本篇命令中的 `sdk` 表示准备好工具的 SDK 目录；名称或位置不同时，替换为实际路径。

### 开发工程

类库开发工程可以放在任意目录，不要求位于 `sdk/libraries`。例如：

```text
D:/LibraryWork/com.example.demo/
├─ src/com/example/demo/演示对象.java
├─ library.json
├─ libs/
├─ res/
├─ assets/
├─ jni/
├─ icons/
├─ sample/
├─ build/       构建后生成
└─ classes.jar  构建后生成
```

| 路径 | 必需 | 说明 |
| --- | --- | --- |
| `library.json` | 是 | ES4A 类库清单 |
| `classes.jar` | 是 | 编译器加载并打入应用的类库代码 |
| `src/` | 使用公共脚本开发时 | Java 源码；使用已编译类库不需要 |
| `libs/` | 否 | 类库依赖的 JAR |
| `res/` | 否 | Android 资源 |
| `assets/` | 否 | Android 资产文件 |
| `jni/` | 否 | 按 ABI 组织的原生库 |
| `icons/` | 否 | `library.json` 引用的 SVG 图标 |
| `sample/` | 开发时推荐 | 用于验证类库的 Simple 项目，可按需交付 |
| `build/` | 生成目录 | 公共构建脚本的中间输出 |

`com.example.demo` 是示例包名。实际类库使用自己的命名空间和独立目录，不放入 `simple.runtime` 包。

使用 SDK 开发 Simple 应用只需要已编译的类库。`src/` 和 `build/` 属于类库开发工程，不是 SDK 中每个类库都必须携带的目录；SDK 安装布局见本篇“安装与接入 SDK”。

组件图标首选 [Tabler Icons](https://tabler.io/icons) 的描边 SVG，保存到类库的 `icons/`。保持 `24×24`、`viewBox="0 0 24 24"`、`currentColor` 和 `2px` 描边；清单中的 `icon` 相对于 `library.json` 所在目录，例如 `icons/square-rounded-check.svg`。普通对象可以不提供图标。

## 注解映射

编译器扫描 `classes.jar` 中的类，只有带 `@SimpleObject` 的类型才会作为 Simple 对象加载。对象的函数、属性、事件和数据成员分别由对应注解公开。

| Java 声明 | Simple 中的含义 |
| --- | --- |
| `@SimpleObject` 类或接口 | 对象类型或接口 |
| `@SimpleComponent` | 与 `@SimpleObject` 同时使用时，声明可由组件系统创建的组件 |
| `@SimpleFunction` 方法 | 函数或过程 |
| `@SimpleProperty` 方法 | 属性获取器或设置器 |
| `@SimpleEvent` 方法 | 事件 |
| `@SimpleDataElement` 字段 | 常量或变量 |
| `@UsesPermissions` | [Android 权限声明](#android-权限) |
| `@ManifestNodes` | [Android 清单节点](#android-清单节点) |

## 数据类型对应

| Java 类型 | Simple 类型 |
| --- | --- |
| `boolean` | `逻辑型` |
| `byte` | `字节型` |
| `short` | `短整数型` |
| `int` | `整数型` |
| `long` | `长整数型` |
| `float` | `单精度小数型` |
| `double` | `双精度小数型` |
| `String` | `文本型` |
| `java.util.Calendar` | `日期时间型` |
| `simple.runtime.variants.Variant` | `变体型` |
| Java 数组 | 对应元素类型和维数的 Simple 数组 |
| 带 `@SimpleObject` 的 Java 类型 | 对应的 Simple 对象类型 |
| `void` 返回值 | 过程 |

传址参数不能直接使用普通 Java 类型，需要使用 `simple.runtime.parameters` 中对应的 `ReferenceParameter` 类型。例如 `IntegerReferenceParameter` 对应传址的 `整数型`，`StringReferenceParameter` 对应传址的 `文本型`。

## 普通对象

`classes.jar` 中的 Java 类型提供 Simple 程序实际调用的对象、函数、属性和事件。

下面的普通对象取自入门演示类库。实例方法在创建对象后调用，静态方法通过类型调用。

```java
package com.example.demo;

import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;

@SimpleObject
public final class 演示对象 {

	public 演示对象() {
	}

	@SimpleFunction
	public String 加前缀(String 文本) {
		return "[Simple] " + 文本;
	}

	@SimpleFunction
	public static int 取长度(String 文本) {
		return 文本.length();
	}
}
```

在已有主窗口单元中，Simple 代码可以使用完全限定名，也可以先声明别名：

```simple
别名 演示对象 = com.example.demo.演示对象

事件 主窗口.初始化()
	变量 工具 为 演示对象
	工具 = 创建 演示对象

	变量 结果 为 文本型
	结果 = 工具.加前缀("你好")

	变量 长度 为 整数型
	长度 = 演示对象.取长度("你好")
	显示提示框(结果 & "，长度：" & 长度)
结束 事件
```

> 扩展类库中的静态函数不会自动成为全局函数。即使 `library.json` 将成员标为 `global: true`，那也只影响 ES4A 的候选提示，不会改变编译器的名称解析规则。

演示对象的 `取长度` 在 Java 中声明为 `static`，在清单对应函数中声明 `static: true`，使 ES4A 按类型成员提供补全。实例函数 `加前缀` 不设置此标记。

### 函数和过程

- 带返回值的 `@SimpleFunction` 方法在 Simple 中是函数。
- 返回 `void` 的 `@SimpleFunction` 方法在 Simple 中是过程。
- `static` 方法属于类型；非静态方法属于对象实例。
- 同名方法可以按参数个数形成重载；不要只依靠参数类型区分相同参数个数的重载。
- 参数和返回类型必须能转换为 Simple 类型。

### 常量和变量

公开字段需要使用 `@SimpleDataElement`：

```java
@SimpleDataElement
public static final int 模式_普通 = 0;

@SimpleDataElement
public static int 当前模式 = 模式_普通;
```

`public static final` 且值可在编译期读取的字段会成为常量；其他公开字段会成为变量。常量值支持逻辑、数值和文本等编译器可识别的基本类型。

### 属性

属性使用同名的获取器和设置器：

```java
@SimpleProperty
public String 标题() {
	return title;
}

@SimpleProperty(
	type = SimpleProperty.PROPERTY_TYPE_STRING,
	initializer = "\"\""
)
public void 标题(String value) {
	title = value;
}
```

- 获取器无参数并返回属性值。
- 设置器返回 `void`，并且只有一个参数。
- 只提供获取器时，属性为只读。
- 获取器与设置器必须使用相同名称和完全相同的类型。

`@SimpleProperty` 的 `type` 和 `initializer` 应写在设置器上。对象创建后，运行库会按 `type` 解析 `initializer`，再调用设置器写入初值；不要在构造方法中重复设置同一个默认值。

> Java 注解中的 `initializer` 是运行时初值，`library.json` 中的 `initializer` 是 ES4A 显示和分析使用的设计期缺省值。两处应表达同一个默认状态，但作用不同，修改时需要同步核对。当前运行时的逻辑初值使用 `True`、`False`，而 `library.json` 使用 Simple 表达式 `真`、`假`。

### 事件

事件声明使用 `@SimpleEvent`。对象或组件触发事件时，通过 `EventDispatcher.dispatchEvent` 分派：

```java
@SimpleEvent
public void 完成(String 结果) {
	EventDispatcher.dispatchEvent(this, "完成", 结果);
}
```

事件名、参数顺序和参数类型必须与 `library.json` 中的说明一致。

## Android 组件

组件通常由“公开接口”和“Android 实现类”组成。

### 组件接口

```java
package com.example.demo;

import simple.runtime.annotations.SimpleComponent;
import simple.runtime.annotations.SimpleEvent;
import simple.runtime.annotations.SimpleObject;
import simple.runtime.annotations.SimpleProperty;
import simple.runtime.components.可视组件;

@SimpleComponent
@SimpleObject
public interface 演示按钮 extends 可视组件 {

	@SimpleEvent
	void 被单击();

	@SimpleProperty
	String 标题();

	@SimpleProperty(
		type = SimpleProperty.PROPERTY_TYPE_STRING,
		initializer = "\"\""
	)
	void 标题(String value);
}
```

常用基础接口：

| 类型 | 用途 |
| --- | --- |
| `组件` | 非可视组件 |
| `可视组件` | 提供 Android `View` 的组件 |
| `组件容器` | 可以包含其他组件的容器 |

容器型可视组件可以同时继承 `可视组件` 和 `组件容器`。

### Android 实现类

```java
package com.example.demo;

import android.view.View;
import android.widget.Button;
import simple.runtime.android.MainActivity;
import simple.runtime.components.组件容器;
import simple.runtime.components.impl.android.视图组件;
import simple.runtime.events.EventDispatcher;

/**
 * 演示按钮的 Android 实现。直接实现组件接口，供编译器配对。
 * Simple 对外成员由接口声明，实现类不重复添加 Simple 注解。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class 演示按钮Impl extends 视图组件 implements 演示按钮 {

	/**
	 * 父类负责创建视图并将组件加入容器。
	 *
	 * @param container 容纳按钮的组件容器，不可为 null
	 */
	public 演示按钮Impl(组件容器 container) {
		super(container);
	}

	@Override
	protected View createView() {
		// 此方法由父类构造器调用，不依赖子类尚未初始化的字段。
		Button button = new Button(MainActivity.getContext());
		// 保留标题的大小写，避免主题自动转换英文文本。
		button.setAllCaps(false);
		button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				被单击();
			}
		});
		return button;
	}

	@Override
	public void 被单击() {
		// 事件名必须与接口中声明的 @SimpleEvent 方法一致。
		EventDispatcher.dispatchEvent(this, "被单击");
	}

	@Override
	public String 标题() {
		return ((Button) getView()).getText().toString();
	}

	@Override
	public void 标题(String value) {
		((Button) getView()).setText(value);
	}
}
```

当前编译器按 Java 的直接继承关系寻找组件实现，因此必须遵守以下规则：

- 实现类直接 `implements` 带 `@SimpleComponent` 的组件接口，或者直接继承带该注解的组件类。
- 实现类提供接收 `组件容器` 的公开构造方法。
- 可视组件返回真实 Android `View`。
- 事件由实现类在真实发生时分派，不要在属性设置器中伪造事件。
- 接口与实现类都必须打入同一个可加载的 `classes.jar`。

> 间接实现组件接口时，当前加载器不能把实现类与组件自动配对，编译时会报告缺少组件实现。

## Android 权限

需要 Android 权限的对象使用 `@UsesPermissions`：

```java
@SimpleObject
@UsesPermissions(permissionNames = "android.permission.INTERNET")
public final class HTTP服务 {
}
```

多个权限使用逗号分隔。编译器只为项目实际引用的对象收集权限。该注解只负责生成 Android 清单声明；危险权限仍需在应用运行时请求，并处理用户拒绝授权的情况。

## Android 清单节点

`@ManifestNodes` 可以向 Android 清单的固定位置加入节点：

```java
@SimpleObject
@ManifestNodes(
	rootXml = "<uses-feature android:name=\"android.hardware.camera\" />",
	applicationXml = "<meta-data android:name=\"demo.appId\" android:value=\"${应用标识}\" />"
)
public final class 示例对象 {
}
```

| 字段 | 插入位置 |
| --- | --- |
| `rootXml` | `/manifest` |
| `applicationXml` | `/manifest/application` |
| `activityXml` | 主 Activity |
| `intentFilterXml` | 主 Activity 的 Intent Filter |

节点可以引用项目属性：

- `${宏名}`：必填，未配置时编译失败。
- `${宏名=缺省值}`：可选，未配置时使用缺省值。

项目在 `project.properties` 中按“Java 类简名.宏名”赋值：

```properties
示例对象.应用标识=my-app-id
```

> 宏是文本替换，不会自动转义 XML。类库必须保证注解文本和替换后的值都是合法 XML。

可选宏的缺省值可以为空文本；仅项目实际引用对象的节点参与注入。宏值中的美元符号和反斜杠按字面替换，仍须保证替换后的 XML 合法。

## 构建 classes.jar

类库仓库的 `build.bat` 接受一个类库开发工程目录。以下示例在 Windows 命令提示符中运行，假设类库仓库位于 `D:\ES4A\libraries`，SDK 位于同级的 `D:\ES4A\sdk`，待构建工程位于 `D:\LibraryWork\com.example.demo`。工程可以另行存放，但公共脚本仍从类库仓库的同级 `sdk` 取得工具与运行库；也可把工程文件夹拖到脚本上。

```bat
"D:\ES4A\libraries\build.bat" "D:\LibraryWork\com.example.demo"
```

工程中的 `src/` 是该脚本的源码输入约定，不限制工程必须存放在 SDK 内；自行使用其它构建工具时，按自己的工程布局构建兼容的 JAR。

公共脚本会：

1. 使用 SDK 内的 JDK 8，以 UTF-8 编译类库 `src` 中的 Java 源码。
2. 将 Android API 26、`SimpleAndroidRuntime.jar` 和 `libs/*` 加入编译类路径。
3. 清理类库自己的 `build/classes`。
4. 把编译结果打包到类库根目录的 `classes.jar`。

脚本只构建传入的工程，不自动将成果复制到 SDK，也不自动注册清单。

构建成功后运行同一仓库的 `release.bat` 安装已有产物并注册清单，发布不会重新编译：

```bat
"D:\ES4A\libraries\release.bat" "D:\LibraryWork\com.example.demo"
```

## 填写类库清单

Java 注解决定编译器能调用哪些成员；`library.json` 为 ES4A 提供分类、补全、悬停和设计器元数据。两者分别维护，清单不能代替 Java 实现。

将以下内容保存为类库根目录的 `library.json`，对应本篇“普通对象”的两个函数：

```json
{
	"name": "演示扩展库",
	"kind": "library",
	"categories": [
		{
			"name": "扩展对象",
			"definitions": [
				{
					"name": "演示对象",
					"kind": "object",
					"type": "com.example.demo.演示对象",
					"functions": [
						{
							"name": "加前缀",
							"return": "文本型",
							"params": [{ "name": "文本", "type": "文本型" }]
						},
						{
							"name": "取长度",
							"static": true,
							"return": "整数型",
							"params": [{ "name": "文本", "type": "文本型" }]
						}
					]
				}
			]
		}
	]
}
```

普通对象使用 `kind: "object"`，接口使用 `interface`，组件使用 `component`。`type` 填公开 Java 类型的完整名称；组件填接口名称，不填 `Impl` 类名。`inherits` 填直接父类型的完整名称，成员名称、类型、参数顺序和静态标记须与 Java 公开声明一致。

可视组件还需声明属性和事件。本篇演示按钮对应的定义如下，将它加入某个分类的 `definitions` 数组即可；继承的属性无需重复列出：

```json
{
	"name": "演示按钮",
	"kind": "component",
	"type": "com.example.demo.演示按钮",
	"inherits": ["simple.runtime.components.可视组件"],
	"properties": [
		{
			"name": "标题",
			"type": "文本型",
			"initializer": "\"\"",
			"projection": "text"
		}
	],
	"events": [{ "name": "被单击" }]
}
```

`initializer` 提供设计期缺省显示，不自动写入单元属性；应与 Java 设置器注解中的运行时初值保持一致。`projection: "text"` 让设计器用“标题”属性显示按钮文本。容器组件还须有 `组件容器` 继承关系，不能仅凭投影字段获得容器能力。

## 安装与接入 SDK

使用当前 SDK 随附的编译能力时，将构建成果安装到 `sdk/libraries` 的一个直接子目录。以下是交付目录，`library.json` 和 `classes.jar` 必需，其它目录只在类库实际使用时提供：

```text
sdk/libraries/com.example.demo/
├─ library.json
├─ classes.jar
├─ libs/        可选依赖 JAR
├─ res/         可选 Android 资源
├─ assets/      可选资产
├─ jni/         可选原生库
└─ icons/       可选清单图标
```

从开发工程复制上述交付文件即可，不要求复制 `src/`、`build/`。样例和源码可以另外提供；应用开发者无需重建该类库。

手动安装时，在所用 SDK 入口清单的 `libraries` 数组中追加路径，保留原有注册项、能力和模板。使用类库仓库的 `release.bat` 时自动注册，优先更新 `sdk.local.json`，不存在时更新 `sdk.json`；本地开发可先将标准清单复制为同目录的 `sdk.local.json`。SDK 仓库不预置演示库，以下展示安装后的相关入口字段：

```json
{
	"compiler": "simple/SimpleCompiler.json",
	"runtime": "simple/SimpleAndroidRuntime.json",
	"libraries": ["libraries/com.example.demo/library.json"]
}
```

注册路径相对于实际所选 SDK 入口清单，只加载明确列出的清单，不使用通配符。扩展库按数组顺序显示。清单注册控制 IDE 元数据，编译器的 JAR 发现规则见本篇“编译与打包”。

安装 ES4A 扩展后，在 VS Code 命令面板执行“ES4A: 选择 SDK”，选择实际使用的入口清单（`sdk.json` 或 `sdk.local.json`）；已经选过时执行“ES4A: 刷新类库”。确认类库树出现新分类，代码中能补全对象及其成员。

## 编译与打包

SDK 编译入口将 `sdk/libraries` 传入 `LIBRARIES_HOME`。编译器扫描直接子目录，仅加载含 `classes.jar` 的类库；未设置或目录不存在时只加载运行库。IDE 注册不改变编译器扫描范围。

| 目录或文件 | 处理方式 |
| --- | --- |
| `classes.jar` | 加入类加载，项目引用后作为整体进入 DEX；已引用类库在打包时缺失此文件则报错。 |
| `libs/*.jar` | 依赖先进入类加载器，随引用类库加入 DEX。 |
| `res/` | 编译并合并 Android 资源。 |
| `assets/` | 打包资产文件。 |
| `libs/` 的非 class 资源、`jni/` | 参与 APK 组装；原生库按 ABI 组织。 |

仅当前项目实际引用的类库连同其依赖、资源、资产和原生库进入 APK。

当前公共脚本只把运行库、Android 平台与本类库的 `libs/*` 放入 Java 编译类路径。编译期依赖其他扩展类库时，需显式提供兼容依赖 JAR，不能把 SDK 入口注册顺序当作 Java 编译依赖解析。`libs` 使用 JAR，当前流程没有直接消费 AAR 的完整方案。

## 验证与发布

### 验证类库

1. 检查 `library.json` 为 UTF-8 JSON，`kind` 为 `library`，类型、成员和继承关系与 Java 实现一致；图标路径存在。
2. 从干净的构建目录生成 `classes.jar`，安装到 SDK 的类库交付目录，确认组件接口与实现类、依赖和资源完整；不要求交付源码。
3. 执行“ES4A: 选择 SDK”或“ES4A: 刷新类库”，检查分类、定义、补全、悬停和参数提示。
4. 使用随附 Simple 样例，或在 ES4A 项目视图中新建项目。普通对象按本篇“普通对象”的代码创建并调用；组件在窗口设计器中添加，检查属性、缺省值、候选项、专用编辑器和事件生成，并保存单元。
5. 选择样例项目，执行“ES4A: 编译应用”；连接已启用 USB 调试的 Android 设备后执行“ES4A: 调试应用”，验证函数、属性、事件、权限、资源与生命周期。命令由所选 SDK 的能力声明提供。
6. 使用 `@ManifestNodes` 时，按本篇宏规则验证有引用、无引用、缺少必填宏和宏含特殊字符的情况。

类库树和代码提示只能证明清单已加载，不能代替编译与设备验证。组件未出现在设计器时，检查 `kind`、`type` 与继承关系，不手写属性区绕过清单问题。

### 交付内容

提供可直接使用的类库交付目录、稳定包名与版本、依赖和许可证说明，以及可复现的 Simple 样例和使用说明。说明最低设备条件、权限及未验证能力。Java 源码工程可独立发布或按需附带，不是应用使用该类库的必备内容。

## 常见问题

### ES4A 能看到类库，但编译失败

先检查类库根目录是否存在最新的 `classes.jar`，再检查 Java 完整类型名是否与 `library.json.type` 一致。类库树可见只说明清单已加载。

### 编译器能识别类型，但 ES4A 中没有显示

先确认 ES4A 已通过 **选择 SDK** 载入正确的 SDK 入口清单，再运行 **刷新类库**。侧边栏“类库”中看不到分类时，检查 `hidden: true`；窗口设计器“可用”区看不到组件时，检查定义的 `kind` 是否为 `component` 或其子类型，以及窗口组件是否被误当作普通子组件。

### 报告“缺少组件实现”

确认实现类直接实现组件接口，并且接口和实现类都已进入 `classes.jar`。仅通过父类或中间接口间接实现时，当前加载器无法完成配对。

### 修改 Java 后行为没有变化

重新构建 Java 工程，并把新的 `classes.jar` 更新到所选 SDK 的类库交付目录。只修改源码或更新工程中的 JAR，不会改变另一个 SDK 目录中的已安装版本。

### 补全信息与编译结果不一致

`library.json` 与 Java 注解是两个独立输入。清单不会校验或生成 Java 成员；修改接口、参数或返回类型时应同步更新两边。

## 延伸参考

本篇已经包含基本实现、构建、清单和注册流程。需要查更多属性编辑器、候选值或设计器投影字段时，再阅读 [类库清单定义](类库清单定义.md)；需要修改 SDK 能力或模板时，阅读 [SDK 清单定义](SDK%20清单定义.md)。
