# Simple 类库开发

文档类型：Java 实现与交付指南。核对日期：2026-10-09。适用范围：当前 SDK 的扩展对象和 Android 组件。依据：随附演示源码、类库构建脚本与清单，并核对当前 RuntimeLoader、Compiler 和运行库注解实现。

本文负责 Java 实现、构建和交付。IDE 元数据单独查阅 [类库清单定义](类库清单定义.md)，注册与能力配置查阅 [SDK 清单定义](SDK%20清单定义.md)。应用侧调用见 [Simple 项目开发](Simple%20项目开发.md#引用已有类库)。

| 当前任务 | 阅读章节 |
| --- | --- |
| 创建工程、准备依赖 | [准备类库工程](#准备类库工程) |
| 编写普通对象、函数、属性或事件 | [注解映射](#注解映射)、[普通对象](#普通对象) |
| 编写可视组件或容器 | [Android 组件](#android-组件) |
| 声明权限或额外 Android 节点 | [Android 权限](#android-权限)、[Android 清单节点](#android-清单节点) |
| 生成 JAR、接入 SDK 并交付 | [构建 classes.jar](#构建-classesjar)、[验证与发布](#验证与发布) |

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
| `@UsesPermissions` | [Android 权限声明](Simple%20类库开发.md#android-权限) |
| `@ManifestNodes` | [Android 清单节点](Simple%20类库开发.md#android-清单节点) |

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

## 准备类库工程

### 目录结构

每个类库占用 `sdk/libraries` 下的一个直接子目录。

```text
sdk/libraries/
├─ build.bat
└─ com.example.demo/
   ├─ library.json
   ├─ classes.jar
   ├─ src/
   ├─ libs/
   ├─ res/
   ├─ assets/
   ├─ jni/
   ├─ icons/
   ├─ sample/
   └─ build/
```

| 路径 | 必需 | 说明 |
| --- | --- | --- |
| `library.json` | 是 | ES4A 类库清单 |
| `classes.jar` | 是 | 编译器加载并打入应用的类库代码 |
| `src/` | 开发时 | Java 源码 |
| `libs/` | 否 | 类库依赖的 JAR |
| `res/` | 否 | Android 资源 |
| `assets/` | 否 | Android 资产文件 |
| `jni/` | 否 | 按 ABI 组织的原生库 |
| `icons/` | 否 | `library.json` 引用的 SVG 图标 |
| `sample/` | 推荐 | 用于验证类库的 Simple 项目 |
| `build/` | 生成目录 | 公共构建脚本的中间输出 |

`com.example.demo` 是示例包名。实际类库使用自己的命名空间和独立目录，不放入 `simple.runtime` 包。

## 构建 classes.jar

以下命令在含 `sdk/` 的上级目录运行；也可把类库文件夹拖到公共 `build.bat` 上。完整工具包准备见 [SDK 工具准备](../README.md#工具准备)，这个类库脚本直接调用 `javac` 和 `jar`，无需 Ant。

所有 SDK 类库共用 `sdk/libraries/build.bat`：

```bat
sdk\libraries\build.bat "sdk\libraries\com.example.demo"
```

公共脚本会：

1. 使用按[SDK 工具准备](../README.md#工具准备)放置的 JDK 8 编译 `src`。
2. 将 Android API 26、`SimpleAndroidRuntime.jar` 和 `libs/*` 加入编译类路径。
3. 清理类库自己的 `build/classes`。
4. 把编译结果打包到类库根目录的 `classes.jar`。

## 接入 SDK

按[SDK 配置参考的类库注册](SDK%20清单定义.md#类库注册)登记 `library.json`，在 ES4A 中选择这个 `sdk.json` 并刷新类库。字段说明见[清单定义](类库清单定义.md)。

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
2. 从干净的构建目录生成 `classes.jar`，确认组件接口与实现类、依赖和资源完整。
3. 在 ES4A 中[选择 SDK 并刷新类库](Simple%20项目开发.md#准备开发环境)，检查分类、定义、补全、悬停和参数提示。
4. 对组件，按[窗口设计](Simple%20项目开发.md#窗口与事件)验证添加、属性、缺省值、候选项、专用编辑器与事件生成；普通对象或接口在代码中验证，调用示例见[普通对象](Simple%20类库开发.md#普通对象)。
5. 编译并打包真实 Simple 样例，在 Android 设备验证函数、属性、事件、权限、资源与生命周期。
6. 使用 `@ManifestNodes` 时，验证有引用、无引用、缺少必填宏和宏含特殊字符的情况，规则见[Android 清单节点](Simple%20类库开发.md#android-清单节点)。

类库树和代码提示只能证明清单已加载，不能代替编译与设备验证。组件未出现在设计器时，检查 `kind`、`type` 与继承关系，不手写属性区绕过清单问题。

### 交付内容

提供完整类库目录、稳定包名与版本、依赖和许可证说明，以及可复现的 Simple 样例和使用说明。说明最低设备条件、权限及未验证能力。

## 常见问题

### ES4A 能看到类库，但编译失败

先检查类库根目录是否存在最新的 `classes.jar`，再检查 Java 完整类型名是否与 `library.json.type` 一致。类库树可见只说明清单已加载。

### 编译器能识别类型，但 ES4A 中没有显示

先确认 ES4A 已通过 **选择 SDK** 载入正确的 `sdk.json`，再运行 **刷新类库**。侧边栏“类库”中看不到分类时，检查 `hidden: true`；窗口设计器“可用”区看不到组件时，检查定义的 `kind` 是否为 `component` 或其子类型，以及窗口组件是否被误当作普通子组件。

### 报告“缺少组件实现”

确认实现类直接实现组件接口，并且接口和实现类都已进入 `classes.jar`。仅通过父类或中间接口间接实现时，当前加载器无法完成配对。

### 修改 Java 后行为没有变化

重新运行公共构建脚本，并确认类库根目录的 `classes.jar` 修改时间已经更新。只修改 `src` 不会影响编译器实际加载的代码。

### 补全信息与编译结果不一致

`library.json` 与 Java 注解是两个独立输入。清单不会校验或生成 Java 成员；修改接口、参数或返回类型时应同步更新两边。
