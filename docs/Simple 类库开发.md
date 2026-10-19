# Simple 类库开发

文档类型：Java 类库开发与交付指南。核对日期：2026-10-19。适用范围：扩展对象和 Android 组件。依据：编译器、运行库、类库构建发布脚本及演示工程。

本文说明 Java API 导出、依赖与资源交付、IDE 接入及构建发布。编译器约定与构建发布脚本的目录要求分别说明。

运行库提供 `SimpleAndroidRuntime.jar`；扩展库提供自己的 `classes.jar`。本文以扩展库为主，Java 注解与类型规则也适用于运行库成员。

| 要做什么 | 阅读章节 |
| --- | --- |
| 确定编译器支持的目录、依赖和打包规则 | [编译器读取的类库结构](#编译器读取的类库结构) |
| 编写对象、组件及公开成员 | [Java 编写约定](#java-编写约定)、[注解映射](#注解映射)、[普通对象](#普通对象)、[Android 组件](#android-组件) |
| 声明 Android 权限和组件节点 | [Android 权限](#android-权限)、[Android 清单节点](#android-清单节点) |
| 生成类库并接入 IDE | [构建 classes.jar](#构建-classesjar)、[填写类库清单](#填写类库清单)、[安装与接入 SDK](#安装与接入-sdk) |
| 验证调用、编写可选示例 | [开发与验证流程](#开发与验证流程)、[示例项目](#示例项目)、[常见问题](#常见问题) |

## 编译器读取的类库结构

当前 SDK 编译能力通过 `LIBRARIES_HOME` 指定扩展库根目录，随附脚本使用 `sdk/libraries/`。编译器仅扫描其直接子目录，以其中的 `classes.jar` 识别已安装类库；不递归发现嵌套类库。根目录未配置或不存在时只加载运行库，路径不是目录或无法读取时报告错误。

下面展示编译器识别的路径。`com.example.demo` 是示例类库目录名；除 `classes.jar` 外，各目录仅在实际使用相应内容时提供：

```text
sdk/libraries/com.example.demo/
├─ classes.jar
├─ libs/
│  └─ okhttp-3.9.0.jar
├─ res/
│  ├─ layout/demo_panel.xml
│  └─ values/strings.xml
├─ assets/demo/config.json
└─ jni/<ABI>/libdemo.so
```

| 固定路径（相对于类库根目录） | 是否必需 | 编译器处理 |
| --- | --- | --- |
| `classes.jar` | 是 | 加载 Java 类；项目引用该库后，整个 JAR 的类加入 DEX |
| `libs/*.jar` | 按需 | 只扫描第一层 JAR，统一选版、去重和类冲突检查；所需依赖加入 DEX，已选 JAR 的非 class 资源参与 APK 组装 |
| `res/` | 按需 | 作为 Android 资源交给 AAPT2 编译、合并 |
| `assets/` | 按需 | 作为应用资产打包，不生成资源编号 |
| `jni/<ABI>/*.so` | 按需 | 作为预编译原生库交给 APK 组装工具 |

类库目录名和 Java 包名不是由上表固定的，但公开类型的完整包名必须与 Java 声明和 IDE 清单一致。类库可以在其它目录开发，构建后再安装到实际编译能力扫描的位置。`library.json` 和 IDE 图标另见 [填写类库清单](#填写类库清单)，不参与编译器的类库发现。

示例、说明、许可证和依赖源码等附加资料可按需提供，名称和位置自行组织；缺少这些文件不会影响编译器加载和使用类库。推荐随库附带示例，也可以没有；目录可叫 `sample/`、`example/`、`demo/` 等。附加的源码 JAR 应放在编译扫描范围之外，放入 `libs/` 第一层就会被当成依赖；授权资料按上游要求保留。

### 加载与打包范围

编译器先加载全部已发现类库及已选依赖，检查重复类并反射分析对象；解析项目后，再决定哪些类库进入 APK。因此，未被项目使用的已安装类库也可能因依赖冲突或类型加载失败阻断编译。

项目引用一个库中的对象后，该库完整 `classes.jar`、所需依赖组、资源、资产和原生库参与打包，不按方法删除未用代码。仅在依赖 JAR 中提供普通 Java 类型，不会自动得到 Simple API；公开类型和成员需使用后文注解。

### 依赖版本与冲突

`libs/` 接受任意名称的 JAR；只有文件名符合 `名称-数字版本.jar` 时才参与版本选择。版本仅由数字和数字段之间的点组成，例如 `okhttp-3.9.0.jar`、`my-library-1.2.jar`；`1` 也有效。`1.0-beta`、`1..2`、`.1`、`1.` 不作为版本识别，相应 JAR 仍按普通依赖保留。

- 同名依赖选择最高数字版本，逐段比较，`3.10` 高于 `3.9`；缺少的尾段按零处理，`1.2` 与 `1.2.0` 相等。名称匹配区分大小写。
- 最高版本有多份副本时，内容相同保留一份，内容不同报错；不同文件名但内容完全相同的 JAR 也去重。
- 选版后仍有重复 Java 类时，报告类名及来源；损坏 JAR 或依赖目录无法读取时报告路径。改变文件名或注册顺序不能消除重复类。
- 候选来自全部已安装类库。项目需要某个依赖组时，实际选中的版本可能由另一个已安装类库提供；类加载、DEX 和 JAR 资源打包共用这一选择，不在打包阶段重新选版。
- `classes.jar` 不参与版本选择；不要将依赖合并进它后，又在 `libs/` 重复提供相同类。

建议保留上游正式制品名称，版本未知时不猜测。依赖及其所需 JAR 应完整提供，并适配当前 JDK 8、dx、Android API 和设备；自动选最高版本不保证 API 或运行兼容性。

当前没有 Maven/Gradle 的传递依赖解析、自动下载、每项目版本锁定或包名隔离。Android 平台、Simple 运行库以及其它已安装扩展库不应再复制进 `libs/` 重复打包；跨扩展库的 Java 编译类路径需另行配置，不能依赖 SDK 清单注册顺序。

### 与 AAR 的关系

以上编译交付结构参考 [AAR 包内部布局](https://developer.android.google.cn/studio/projects/android-library?hl=zh-cn#aar-contents)。AAR 是 ZIP，唯一必需条目是 `AndroidManifest.xml`；ES4A 当前使用目录式类库，以 `classes.jar` 为发现条件，不直接读取 `.aar`。

| AAR 条目 | 当前 ES4A 处理 |
| --- | --- |
| `classes.jar`、`libs/`、`res/`、`assets/`、`jni/` | 沿用目录和文件用途，按本文规则处理 |
| `AndroidManifest.xml` | 不读取类库独立清单；通过 `@UsesPermissions`、`@ManifestNodes` 收集声明 |
| `R.txt`、`public.txt` | 不作为类库输入读取，应用构建时生成自己的资源索引 |
| `proguard.txt`、`lint.jar`、`api.jar`、`prefab/` | 当前没有对应消费流程 |

### Android 资源与资产

`res/` 遵循 Android 资源目录，例如 `layout/`、`drawable/`、`values/`、`raw/`；资源名称使用合法的小写字母、数字和下划线，并加本库前缀以减少冲突，例如 `demo_panel`。

- `res/layout/demo_panel.xml`：可通过 Android 资源编号加载的布局。
- `res/values/strings.xml`：`<resources>` 内的字符串等资源声明。
- `res/raw/demo_beep.ogg`：可通过资源编号读取的原始资源。
- `assets/demo/config.json`：通过 `context.getAssets().open("demo/config.json")` 读取，路径不包含 `assets/`。

当前构建流程生成应用包的 `R.java`，不自动为每个第三方库包生成独立 `R` 类。Java 类库不能假定 `com.example.demo.R` 会被公共构建脚本生成；确需动态定位时，可使用应用资源和应用包名，例如 `context.getResources().getIdentifier("demo_panel", "layout", context.getPackageName())`，并处理返回 `0` 的缺失情况。

库资源在项目引用后参与 AAPT2 合并；不同库之间同名资源可能报冲突，项目资源按当前链接流程作为覆盖输入处理，不能照搬 Gradle 的库优先级推断。资产也应使用本库独立子目录，避免路径冲突。资源缺失或 XML 错误在应用资源编译阶段报告，Java 编译通过不能证明资源正确。

### 原生库

预编译 `.so` 放在 `jni/<ABI>/`，例如：

```text
jni/
├─ armeabi-v7a/libdemo.so
└─ arm64-v8a/libdemo.so
```

每个文件须针对所在 ABI 构建。`armeabi`、`armeabi-v7a`、`arm64-v8a`、`x86` 等目录名不能互换，改目录名不能转换机器码。

Java 中通常用 `System.loadLibrary("demo")` 加载 `libdemo.so`，参数不含 `lib` 和 `.so`。若它依赖其它非系统原生库，也须在相应 ABI 下提供，并按底层库要求加载。JNI 包名、类名或 native 方法签名与原生实现绑定时，不得只改 Java 名称。

当前编译器将已引用类库的 `jni/` 交给 APK 组装工具，原生库在 APK 中位于 `lib/<ABI>/`；它不会编译 C/C++、补齐 ABI 或验证机器码兼容性，也不扫描 `libs/<ABI>/`。必须实际检查 APK 中的文件并在匹配设备上验证加载；只有 32 位原生库时，不能宣称支持仅运行 64 位应用的设备。

## Java 编写约定

编译器按 Java 类型、注解和签名识别 API。建议采用以下命名、注释和排版方式；这些风格约定不改变 API 导出规则：

- 导出类、接口、方法、属性、事件和常量按需使用中文，MQTT、NFC、TCP 等缩写保持规范大小写。Java 参数和内部实现使用英文；清单展示参数名使用简短中文。
- 导出类型名及需要直接以普通标识符访问的名称避开 Simple 保留关键字。通过点号访问的方法、属性和字段允许与关键字同名，如 `对象实例.匹配(文本)`；具体语言规则见 [标识符](Simple%20语言定义.md#标识符)。
- 示例实现类采用“组件名 + `Impl`”；编译器实际按继承关系配对，不按此后缀猜实现。
- 组件接口按“常量 → 事件 → 属性 → 方法”排列。实现类保留字段、构造和初始化在前，对外成员顺序相近；属性获取器、设置器相邻，辅助方法按职责归组。
- 公开 API 用 JavaDoc 说明职责、参数、返回值和限制；`@param 参数名 描述`、`@return 描述` 各部分之间一个空格。`void` 方法和构造方法不写 `@return`。
- 有参数的注解逐项多行，结束括号另起一行；无参数注解独占一行。接口声明 Simple 注解，实现类使用 `@Override`；普通对象直接在公开类和成员上声明。
- Java 源码使用 UTF-8、CRLF、Tab 缩进（宽度 4）和末尾换行。方法及功能代码块之间留空白行，注释说明职责、状态和兼容原因。

以下完整文件示例可以直接编译；成员片段需放进所属类型并补充注明的 import。

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

公开 API 优先使用表中的基本类型、数组或已有 Simple 对象；不要直接公开 `List<T>`、`Map<K,V>`、泛型、可变参数或第三方 SDK 类型，并假定语言侧会自动转换。必要时在本库封装为明确的 Simple API。普通 Java 辅助类不加 `@SimpleObject`，不会因此成为可供 Simple 创建的对象。

传址成员片段如下，所在类需导入 `simple.runtime.annotations.SimpleFunction` 和 `simple.runtime.parameters.IntegerReferenceParameter`：

```java
/**
 * 将调用者的整数增加一。
 *
 * @param value 可读取并回写的整数引用
 */
@SimpleFunction
public void 增加一(IntegerReferenceParameter value) {
	value.set(value.get() + 1);
}
```

清单对应参数使用 `{ "name": "数值", "type": "整数型", "byRef": true }`；不能只在 JSON 中声明传址而仍使用普通 `int` 参数。

## 普通对象

`classes.jar` 中的 Java 类型提供 Simple 程序实际调用的对象、函数、属性和事件。

下面是入门演示类库的完整对象文件，保存为 `src/com/example/demo/演示对象.java`。实例方法在创建对象后调用，静态方法通过类型调用。

```java
package com.example.demo;

import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;

/**
 * 演示普通对象的实例函数和静态函数，不依赖 Android 界面。
 */
@SimpleObject
public final class 演示对象 {

	/** 供 Simple 的“创建 演示对象”调用。 */
	public 演示对象() {
	}

	/* 方法 */

	/**
	 * 为文本添加固定前缀。
	 *
	 * @param text 要添加前缀的文本
	 * @return 带有 [Simple] 前缀的文本
	 */
	@SimpleFunction
	public String 加前缀(String text) {
		return "[Simple] " + text;
	}

	/**
	 * 返回文本长度。
	 *
	 * @param text 要计算长度的文本
	 * @return UTF-16 代码单元数
	 */
	@SimpleFunction
	public static int 取长度(String text) {
		return text.length();
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

普通对象使用 `创建` 时必须有公开无参构造方法；不能依靠带参 Java 构造器生成 Simple 创建语法。需要容器或 Android 生命周期的功能应使用组件，不通过全局静态实例绕过组件创建。

### 函数和过程

- 带返回值的 `@SimpleFunction` 方法在 Simple 中是函数。
- 返回 `void` 的 `@SimpleFunction` 方法在 Simple 中是过程。
- `static` 方法属于类型；非静态方法属于对象实例。
- 同名方法可以按参数个数形成重载；不要只依靠参数类型区分相同参数个数的重载。
- 参数和返回类型必须能转换为 Simple 类型。

### 常量和变量

公开字段需要使用 `@SimpleDataElement`。以下是类型内的成员片段，须导入 `simple.runtime.annotations.SimpleDataElement`：

```java
@SimpleDataElement
public static final int 模式_普通 = 0;

@SimpleDataElement
public static int 当前模式 = 模式_普通;
```

`public static final` 且值可在编译期读取的字段会成为常量；其他公开字段会成为变量。常量值支持逻辑、数值和文本等编译器可识别的基本类型。

### 属性

属性使用同名的获取器和设置器。以下是类型内的成员片段，须导入 `simple.runtime.annotations.SimpleProperty`：

```java
private String title;

/**
 * 返回当前标题。
 *
 * @return 标题文本
 */
@SimpleProperty
public String 标题() {
	return title;
}

/**
 * 设置标题。
 *
 * @param value 新标题
 */
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

| 属性 | Java 设置器的注解参数 | 清单的初值表达式 |
| --- | --- | --- |
| 空文本 | `type = SimpleProperty.PROPERTY_TYPE_STRING, initializer = "\"\""` | `"initializer": "\"\""` |
| 逻辑假 | `type = SimpleProperty.PROPERTY_TYPE_BOOLEAN, initializer = "False"` | `"initializer": "假"` |
| 整数零 | `type = SimpleProperty.PROPERTY_TYPE_INTEGER, initializer = "0"` | `"initializer": "0"` |

上表将参数写在同一格便于对照；实际 Java 注解仍按多行格式编写。不要把 Java 的字符串转义与 JSON 的字符串转义混为一层，也不要把 `initializer` 当作会执行任意 Simple 表达式的脚本。

### 事件

事件声明使用 `@SimpleEvent`。对象或组件触发事件时，通过 `EventDispatcher.dispatchEvent` 分派。以下是普通对象内的成员片段，须导入 `simple.runtime.annotations.SimpleEvent` 和 `simple.runtime.events.EventDispatcher`；组件接口只声明事件签名，分派放在实现类：

```java
/**
 * 操作完成时触发。
 *
 * @param result 操作结果
 */
@SimpleEvent
public void 完成(String result) {
	EventDispatcher.dispatchEvent(this, "完成", result);
}
```

事件名、参数顺序和参数类型必须与 `library.json` 中的说明一致。

## Android 组件

组件通常由“公开接口”和“Android 实现类”组成。

| 功能形态 | 公开类型 | 实现基础 |
| --- | --- | --- |
| 纯计算或工具对象 | `@SimpleObject` 普通类 | 通常不继承运行库实现类，提供公开无参构造方法 |
| 非可视组件 | 同时带 `@SimpleObject`、`@SimpleComponent` 的接口，继承 `组件` | `simple.runtime.components.impl.组件Impl` |
| 可视组件 | 同样的组件注解，接口继承 `可视组件` | `simple.runtime.components.impl.android.视图组件` |
| 可视容器 | 接口还继承 `组件容器` | 除 View 外还须实现容器的添加、布局等契约，不仅是加一个清单标记 |

### 非可视组件

以下两个完整文件演示事件和普通方法，保存到对应 Java 路径。此示例故意同步触发事件，真实网络、蓝牙和数据库操作应按后文的线程与生命周期要求实现。

`src/com/example/demo/演示任务.java`：

```java
package com.example.demo;

import simple.runtime.annotations.SimpleComponent;
import simple.runtime.annotations.SimpleEvent;
import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;
import simple.runtime.components.组件;

/** 演示接收文本并通过事件返回结果的非可视组件。 */
@SimpleComponent
@SimpleObject
public interface 演示任务 extends 组件 {

	/* 事件 */

	/**
	 * 执行完毕时触发。
	 *
	 * @param result 执行结果
	 */
	@SimpleEvent
	void 完成(String result);

	/* 方法 */

	/**
	 * 执行一次演示操作。
	 *
	 * @param text 输入文本
	 */
	@SimpleFunction
	void 处理文本(String text);
}
```

`src/com/example/demo/演示任务Impl.java`：

```java
package com.example.demo;

import simple.runtime.components.组件容器;
import simple.runtime.components.impl.组件Impl;
import simple.runtime.events.EventDispatcher;

/** 将公开组件接口与执行逻辑连接。 */
public final class 演示任务Impl extends 组件Impl implements 演示任务 {

	/**
	 * 创建非可视组件。
	 *
	 * @param container 所属组件容器
	 */
	public 演示任务Impl(组件容器 container) {
		super(container);
	}

	/* 事件 */

	@Override
	public void 完成(String result) {
		EventDispatcher.dispatchEvent(this, "完成", result);
	}

	/* 方法 */

	@Override
	public void 处理文本(String text) {
		// 同步示例直接回传；实际异步操作应在完成回调中分派。
		完成(text);
	}
}
```

### 组件接口

可视组件示例保存为 `src/com/example/demo/演示按钮.java`：

```java
package com.example.demo;

import simple.runtime.annotations.SimpleComponent;
import simple.runtime.annotations.SimpleEvent;
import simple.runtime.annotations.SimpleObject;
import simple.runtime.annotations.SimpleProperty;
import simple.runtime.components.可视组件;

/** 提供标题属性和单击事件的演示按钮。 */
@SimpleComponent
@SimpleObject
public interface 演示按钮 extends 可视组件 {

	/* 事件 */

	/** 用户单击按钮时触发。 */
	@SimpleEvent
	void 被单击();

	/* 属性 */

	/**
	 * 返回按钮标题。
	 *
	 * @return 标题文本
	 */
	@SimpleProperty
	String 标题();

	/**
	 * 设置按钮标题。
	 *
	 * @param value 新标题
	 */
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

保存为 `src/com/example/demo/演示按钮Impl.java`：

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

	/* 事件 */

	@Override
	public void 被单击() {
		// 事件名必须与接口中声明的 @SimpleEvent 方法一致。
		EventDispatcher.dispatchEvent(this, "被单击");
	}

	/* 属性 */

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
- 将组件接口和实现放在同一 `classes.jar`，确保引用该库时两者一并进入 APK。

> 间接实现组件接口时，当前加载器不能把实现类与组件自动配对，编译时会报告缺少组件实现。

### 线程、初始化与生命周期

- 组件构造器只完成必要装配。`视图组件` 的父类构造器会调用 `createView()`，此时子类字段初始化尚未完成，不能依赖子类字段已有值。
- 由 Simple 创建对象或组件时，运行库在对应创建流程中应用属性初值。设置器须能接受声明的默认值；不要在构造器中启动依赖属性配置的网络连接、扫描或耗时工作。
- 耗时 I/O 放在工作线程；修改 Android View 以及供界面逻辑消费的完成、失败事件，应调度回主线程。`EventDispatcher.dispatchEvent` 在调用线程同步执行处理程序，不会自动切换线程。
- 线程、连接、广播接收器、服务绑定和系统监听分别记录是否已取得，并在组件 `销毁()` 或实际需要的生命周期回调中释放。重写 `销毁()` 时还要调用 `super.销毁()`，保留基础事件解绑。
- `组件Impl` 本身不替你登记所有 Android Activity 生命周期监听；需要时显式使用当前运行库提供的监听机制，并避免销毁后继续分派旧回调。
- 避免在静态初始化块中访问设备、打开连接或无条件加载原生库。编译器在 Windows 上反射分析 Java 类型，读取静态常量还可能触发类初始化，设备能力只能在合适的 Android 运行时机使用。

## Android 权限

需要 Android 权限的公开对象或组件接口使用 `@UsesPermissions`。以下为类型声明片段，须导入 `simple.runtime.annotations.SimpleObject` 和 `simple.runtime.annotations.UsesPermissions`；组件还需自己的组件注解与接口契约：

```java
@SimpleObject
@UsesPermissions(
	permissionNames = "android.permission.INTERNET"
)
public final class HTTP服务 {
}
```

多个权限使用逗号分隔。编译器只为项目实际引用的对象收集权限。该注解只负责生成 Android 清单声明；危险权限仍需在应用运行时请求，并处理用户拒绝授权的情况。

权限放在实际被引用的公开类型上，不要只放在未作为 Simple 对象加载的 `Impl` 或内部辅助类上。权限名称、系统版本条件与运行时请求按实际使用的 Android API 核实，不能把声明存在当作已经授权。

## Android 清单节点

`@ManifestNodes` 可以向 Android 清单的固定位置加入节点。以下为类型声明片段，须导入 `simple.runtime.annotations.SimpleObject` 和 `simple.runtime.annotations.ManifestNodes`：

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

Activity、Service、Receiver 等辅助类使用完整 Java 类名，并确认已进入 `classes.jar` 或所需依赖 JAR。当前流程按注解文本收集节点，不是 Gradle Manifest Merger；不会自动读取 AAR 清单、解决节点属性冲突或补齐辅助类声明。第三方库需要的清单内容须逐项迁入对应插入位置并验证生成结果。

## 构建 classes.jar

Java 开发需要 Android 平台和 `SimpleAndroidRuntime.jar`。使用其它构建工具时，源码与输出目录可自行组织，最终提供兼容的 `classes.jar` 及所需依赖、资源即可。

类库仓库提供 `build.bat`。按 [SDK README](../README.md) 准备工具，并将 [类库仓库](https://gitee.com/es4a/libraries) 与 [SDK 仓库](https://gitee.com/es4a/sdk) 同级放置；这是该脚本的路径约定：

| SDK 内路径 | 用途 |
| --- | --- |
| `tools/jdk1.8.0_503/bin/javac.exe`、`jar.exe` | JDK 8 编译和打包 |
| `tools/android/platforms/android-26/android.jar` | Android API 26 |
| `simple/SimpleAndroidRuntime.jar` | Simple 注解、类型及基础组件 |

在 Windows 命令提示符中运行，例如：

```bat
"D:\ES4A\libraries\build.bat" "D:\LibraryWork\com.example.demo"
```

该脚本从自身同级 `sdk` 取工具，递归编译传入工程 `src/` 的 Java，以 UTF-8 读取源码；编译类路径是 Android 平台、运行库和本库 `libs/*`。它重建 `build/classes/`，用 `build/source.txt` 记录输入，最后生成根目录 `classes.jar`。

这里的 `src/`、`build/` 是脚本约定，不是应用编译器对类库源码布局的要求。脚本只构建指定类库，不安装、不注册；失败时不要发布上次遗留 JAR。需要跨扩展库编译依赖时，另行配置兼容的 Java 类路径。

## 填写类库清单

Java 注解决定编译器能调用哪些成员；`library.json` 为 ES4A 提供分类、补全、悬停和设计器元数据。两者分别维护，清单不能代替 Java 实现。

`library.json` 是下文及公共发布脚本使用的文件名；IDE 实际按 SDK 入口注册的 JSON 路径加载，不要求清单必须叫这个名称。使用公共 `release.bat` 时则须满足脚本的固定文件名约定。

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

清单的 `name` 和 `description` 面向用户；为类型和公开成员补充简短用途说明，为参数填写中文名称。函数返回 `void` 时不填写 `return`；只读属性设置 `writable: false`；静态成员设置 `static: true`；传址参数设置 `byRef: true`。不要把 JavaDoc、参数名或静态标记会自动从 JAR 同步到 JSON 当作前提。

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

本篇非可视组件的定义如下，同样加入分类的 `definitions`：

```json
{
	"name": "演示任务",
	"description": "执行操作并通过完成事件返回结果。",
	"kind": "component",
	"type": "com.example.demo.演示任务",
	"inherits": ["simple.runtime.components.组件"],
	"events": [
		{
			"name": "完成",
			"params": [{ "name": "结果", "type": "文本型" }]
		}
	],
	"functions": [
		{
			"name": "处理文本",
			"params": [{ "name": "文本", "type": "文本型" }]
		}
	]
}
```

自定义图标再加 `"icon": "icons/实际图标.svg"`；文件不存在时不能保留占位路径。Java API 有增删或签名变化时，同步核对全部定义，而不是只更新类库版本字段。

### IDE 图标

`icon` 路径相对于声明它的清单，例如 `"icon": "icons/square-rounded-check.svg"`；`icons/` 只是参考目录名，可以使用其它路径。组件图标推荐 [Tabler Icons](https://tabler.io/icons) 描边 SVG，保持 `24×24`、`viewBox="0 0 24 24"`、`currentColor` 和 `2px` 描边。图标只供 IDE 显示，应用图片应放 `res/` 或 `assets/`。

## 安装与接入 SDK

将构建产物与所需依赖、资源安装到 `sdk/libraries/<类库目录>/`。编译器使用前文的固定路径；接入 IDE 还需类库清单，两者独立维护。开发者使用已编译类库不需要重建 Java 或携带源码。

公共 `release.bat` 安装指定类库的已有产物，不重新编译；例如：

```bat
"D:\ES4A\libraries\release.bat" "D:\LibraryWork\com.example.demo"
```

脚本复制 `classes.jar`、`library.json`、`libs/`、`res/`、`assets/`、`jni/`、`icons/`、`sample/` 及根目录的 `README.md`、`LICENSE`、`LICENSE.txt`，排除 `libs/sources/` 和 `sample/build/`。其它附加资料需自行交付。

发布会整体替换 SDK 中该类库的交付目录，旧文件会被清除，不影响其它类库。不要在交付目录维护需保留的文件；开发目录与发布目录重叠时脚本拒绝执行。

发布脚本优先注册到已存在的 `sdk.local.json`，否则更新 `sdk.json`，不自动创建本地入口。手动安装则在所用入口的 `libraries` 数组追加清单路径，保留其它配置：

```json
{
	"compiler": "simple/SimpleCompiler.json",
	"runtime": "simple/SimpleAndroidRuntime.json",
	"libraries": ["libraries/com.example.demo/library.json"]
}
```

路径相对于所选 SDK 入口，只加载明确列出的清单，不使用通配符。ES4A 中执行“选择 SDK”，选择 `sdk.json` 或 `sdk.local.json`；更新后执行“刷新类库”。

注册控制 IDE 元数据，不改变编译器扫描范围。仅注册其它位置的清单不会使那里的 JAR 自动参与编译，仍需安装到实际编译能力扫描的位置。图标使用其它目录时，也须随清单一同提供并核对实际发布结果。

## 开发与验证流程

1. 确定 Java 包名、公开 API 和对象形态，按前文示例或自己的工程布局编写实现。
2. 构建 `classes.jar`，补齐真正需要的依赖、资源、资产和原生库。
3. 编写 IDE 清单，核对类型、继承、成员名称、参数顺序、类型、静态与传址标志，以及属性默认值。
4. 安装产物并刷新 IDE，用已有或临时 Simple 项目验证对象创建、函数、属性和事件；可按下一节创建示例，但无需随类库交付。
5. 编译应用检查资源、清单和 APK 内容；按所用功能在设备上验证权限、线程、失败处理、重复操作及资源清理，记录未验证项。

Java 编译、IDE 中的成员与设计器检查、APK 构建和设备运行应分别完成；编译成功不代表权限、资源或设备功能已经可用。

### 示例项目

推荐提供聚焦本库用法的独立 Simple 示例，位置和目录名自行决定；没有示例也不影响类库安装和使用。以下仅以 `sample/` 为例，采用“类库名 + 示例”的应用名：

```properties
main=com.example.demo.sample.主窗口
name=演示扩展库示例
source=./src
assets=./assets
```

这些内容保存为示例项目的 `project.properties`，路径相对于该文件。主窗口位于 `src/com/example/demo/sample/主窗口.simple`，包名与 `main`、相对源码路径一致。保持声明的资产目录存在，需要资源时再配置 `res=./res`；应用工程详情见 [Simple 项目开发](Simple%20项目开发.md)。

普通对象可用前文的 `主窗口.初始化` 代码验证。验证组件时，通过 IDE 新建主窗口，在设计器中添加 `演示按钮1` 和 `演示任务1`，再生成事件并填写：

```simple
事件 演示按钮1.被单击()
	演示任务1.处理文本("你好，Simple！")
结束 事件

事件 演示任务1.完成(结果 为 文本型)
	显示提示框(结果)
结束 事件
```

以上是用户代码，窗口和组件定义由 IDE 维护，复制事件代码不会自动创建组件。各库 README 可说明依赖及版本、配置、权限、设备或服务要求、功能限制和示例入口；具体操作流程放在示例中。

## 常见问题

| 现象 | 检查要点 |
| --- | --- |
| IDE 能看到类库，编译却找不到 | 检查实际扫描位置的 `classes.jar`、依赖，以及清单 `type` 与 Java 完整名称；IDE 可见只证明清单已加载 |
| 编译可用，IDE 不显示 | 检查所选 SDK 入口、清单注册、分类 `hidden`、定义 `kind` 和继承关系，更新后刷新 |
| 缺少组件实现 | 实现类直接实现被注解的接口或直接继承被注解的组件类，并与接口一起交付 |
| 修改 Java 后行为没变 | 重新构建并安装本次 `classes.jar`；只改源码或开发目录 JAR 不会更新已安装 SDK |
| 重复类、缺失类型或方法不存在 | 按诊断来源检查全部已安装类库的依赖及实际选中版本，不能只检查当前项目使用的库 |
| Java 编译成功，资源失败 | 检查 `res/` XML、资源名、重名及未生成的第三方包 `R`；Java 构建不验证 Android 资源 |
| 原生库加载失败 | 检查 APK `lib/<ABI>/` 内容、设备进程架构、加载名称、原生依赖和 JNI 绑定；改目录名不能转换 ABI |
| JDK 8 找不到真实存在的中文源码 | 确认 UTF-8，必要时在当前 PowerShell 设置 `$env:JAVA_TOOL_OPTIONS = "-Dfile.encoding=UTF-8"` 排查，结束后恢复原值，无需转换 BAT 编码 |

## 延伸参考

- [类库清单定义](类库清单定义.md)：分类、成员、属性编辑器及设计器投影字段。
- [SDK 清单定义](SDK%20清单定义.md)：SDK 入口、能力、模板和注册规则。
