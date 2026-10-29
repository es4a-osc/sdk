# Simple 类库开发

运行库提供 `SimpleAndroidRuntime.jar`；扩展库提供自己的 `classes.jar`。本文以扩展库为主，Java 注解与类型规则也适用于运行库成员。

| 要做什么 | 阅读章节 |
| --- | --- |
| 确定编译器支持的目录、依赖和打包规则 | [编译器读取的类库结构](#编译器读取的类库结构) |
| 编写对象、组件及公开成员 | [Java 编写约定](#java-编写约定)、[注解映射](#注解映射)、[普通对象](#普通对象)、[Android 组件](#android-组件) |
| 声明 Android 权限和组件节点 | [Android 权限](#android-权限)、[Android 清单节点](#android-清单节点) |
| 生成类库并接入 IDE | [构建 classes.jar](#构建-classesjar)、[填写类库清单](#填写类库清单)、[安装与接入 SDK](#安装与接入-sdk) |
| 开发与发布前测试 | [开发与验证流程](#开发与验证流程)、[类库测试项目](#类库测试项目)、[常见问题](#常见问题) |

## 编译器读取的类库结构

当前 SDK 编译能力通过 `LIBRARIES_HOME` 指定扩展库根目录，随附脚本使用 `sdk/libraries/`。编译器仅扫描其直接子目录，以其中的 `classes.jar` 识别已安装类库；不递归发现嵌套类库。根目录未配置或不存在时只加载运行库，路径不是目录或无法读取时报告错误。

下面展示编译器识别的路径。`com.example.demo` 是示例类库目录名；除 `classes.jar` 外，各目录仅在实际使用相应内容时提供：

```text
sdk/libraries/com.example.demo/
├─ classes.jar
├─ dependencies.json
├─ dependencies.module
├─ AndroidManifest.generated.xml
├─ R.txt
├─ libs/
│  └─ local.jar
├─ aars/
│  └─ local.aar
├─ res/
│  ├─ layout/demo_panel.xml
│  └─ values/strings.xml
├─ assets/demo/config.json
└─ jni/<ABI>/libdemo.so
```

| 固定路径（相对于类库根目录） | 是否必需 | 编译器处理 |
| --- | --- | --- |
| `classes.jar` | 是 | 直接读取类文件注解建立符号；项目引用该库后，整个 JAR 的类加入 DEX |
| `dependencies.json` | 工具生成 | schemaVersion 2，记录资源包名、Java、最低 API、模块描述、生成清单路径及本地原包；文件路径相对于类库根目录 |
| `libs/*.jar`、`aars/*.aar` | 按需 | 本地原始依赖；无依赖描述时扫描第一层，有描述时以描述为准 |
| `dependencies.module` | 工具生成 | Gradle 原生依赖声明，保留编译/运行范围、版本约束和排除规则；不包含下载的传递依赖或本机缓存路径 |
| `AndroidManifest.generated.xml`、`R.txt` | 工具生成 | 用于清单合并及本库资源包的 R 类生成；生成清单由 `dependencies.json` 的 `manifest` 字段指定。未提供该字段的既有类库仍读取 `AndroidManifest.xml` |
| `res/` | 按需 | 作为 Android 资源交给 AAPT2 编译、合并 |
| `assets/` | 按需 | 作为应用资产打包，不生成资源编号 |
| `jni/<ABI>/*.so` | 按需 | 作为预编译原生库交给 APK 组装工具 |

类库目录名和 Java 包名不是由上表固定的，但公开类型的完整包名必须与 Java 声明和 IDE 清单一致。类库可以在其它目录开发，构建后再安装到实际编译能力扫描的位置。`library.json` 和 IDE 图标另见 [填写类库清单](#填写类库清单)，不参与编译器的类库发现。

扩展库是独立于 SDK 的第三方类库。源码中的 `test/` 是本库独立维护的测试项目，用于开发和发布前验证，不安装到 `sdk/libraries/`。当前阶段不规定或维护扩展库用户示例。编译与发布不主动收集、下载许可证；已有及上游包自带的许可证、NOTICE 和源码版权声明保留原样。本地 JAR 的同名源码包放在 `libs/<原包名>-sources.jar`，仅作为 IDE 开发附件，不作为编译或运行依赖，也不随发布工具安装到 SDK。

### 加载与打包范围

编译器先直接读取已安装类库的类文件注解，不加载扩展库实现或准备其第三方依赖。资源准备前通过独立语义树发现项目实际引用，再由 SDK 统一解析这些库的运行依赖；正式语义解析及代码生成使用完整资源索引。未使用库的依赖缺失或依赖描述损坏不阻断构建；本体类文件损坏或重复导出仍会报告错误。

当前仍以 Simple 对象引用识别项目使用的扩展库；只在资源 XML 中写入某个库的主题名称，不会单独将该库选入构建。

项目引用一个库中的对象后，该库完整 `classes.jar`、所需依赖组、资源、资产和原生库参与打包，不按方法删除未用代码。仅在依赖 JAR 中提供普通 Java 类型，不会自动得到 Simple API；公开类型和成员需使用后文注解。

### 依赖版本与冲突

SDK 扩展库工具在开发阶段通过 Gradle 解析 `library.gradle`，供 Java 编译及 IDE 共用。编译扩展库时，在库根目录生成 JAR、依赖描述和必要的清单、资源附加文件，本地运行依赖均通过库内相对路径引用；即使不携带 `build/`，该目录也可直接复制和消费。仓库依赖仅保留 Gradle 原生声明，不携带已下载的完整传递依赖或解构目录。作者不手工维护第二份依赖声明。新式交付需要配套支持相应描述字段的 SDK 编译器。消费者必须遵守的版本要求应使用依赖约束；开发工程本地的 `resolutionStrategy` 等规则不会自动成为交付契约。

应用首次构建或依赖声明变化时，SDK 使用随附 Gradle/AGP 对项目所用扩展库统一解析，编译器消费返回的原始制品路径。缓存结果位于项目 `build/dependencies/resolution/`，声明、工具路径与 SDK `simple/gradle/` 解析脚本一致，且依赖制品存在、内容校验通过时复用，不再次启动 Gradle。旧 schemaVersion 1 本地快照仍可读取，迁移时应使用当前工具重新编译以生成新依赖描述，再安装或传播完整编译结果。解析缓存不等于项目长期版本锁；清理缓存或显式刷新后会按声明重新解析。

- 仓库模块的版本约束、排除和 Android 变体由 Gradle 处理；候选仅来自项目实际使用的扩展库。
- 扩展库 `libs/*.jar` 与所有 AAR 内嵌 `libs/*.jar` 统一选择独立 JAR：明确仓库坐标优先，其次使用唯一有效的 Maven `pom.properties` 身份，最后采用 `名称-版本.jar` 约定。
- 独立 JAR 版本比较复用 Maven 的 `ComparableVersion`，支持数字段和 `rc`、`SNAPSHOT` 等限定符。同身份同版本但内容不同报错；没有可靠身份或版本的文件只按内容去重，不静默覆盖。最终输入仍有重复类时，报告双方来源。
- 本地 JAR 与 Gradle 已选的同模块制品内容不一致时明确报错，要求统一声明或本地原包，不在文件选版阶段绕过模块约束。
- AAR 本体的 `classes.jar` 随所属 AAR 处理，不因同名而跨 AAR 选版。相同内容的代码归档去重。
- 本地包只提供文件本身，不读取同名 POM 解析传递依赖；需要的外部依赖由作者在 `library.gradle` 显式补齐。JAR 内的 Maven 元数据仅用于识别身份。
- 自动选版不保证调用方二进制或运行兼容性，依赖升级后仍需验证。Android 平台和 Simple 运行库直接引用 SDK，不重复放入交付依赖。

依赖准备可能访问仓库。离线构建使用项目工具的 `-Offline`；显式重新检查仓库使用 `-RefreshDependencies`，两者不能同时指定。移动 SDK、清理缓存或首次在新机器构建时，需要可访问的仓库或完整的已有 Gradle 缓存。

### 与 AAR 的关系

以上编译交付结构参考 [AAR 包内部布局](https://developer.android.google.cn/studio/projects/android-library?hl=zh-cn#aar-contents)。AAR 是 ZIP，唯一必需条目是 `AndroidManifest.xml`；ES4A 以目录中的 `classes.jar` 发现扩展库，并消费其依赖描述和原始 AAR。

| AAR 条目 | 当前 ES4A 处理 |
| --- | --- |
| `classes.jar`、`libs/*.jar` | 展开代码与内嵌 JAR，供类加载、DEX 和 Java 资源打包使用 |
| `res/`、`assets/`、`jni/` | 参与资源链接、资产合并及原生库打包 |
| `AndroidManifest.xml` | 与应用及扩展库清单通过官方 Manifest Merger 合并，冲突及缺失占位符报错 |
| `R.txt` | 根据最终链接结果为原资源包生成 R 类，包括 styleable 数组及索引 |
| `public.txt` | 不额外限制当前应用的资源访问 |
| `proguard.txt`、`lint.jar`、`api.jar`、`prefab/` | 当前没有对应消费流程 |

### Android 资源与资产

`res/` 遵循 Android 资源目录，例如 `layout/`、`drawable/`、`values/`、`raw/`；资源名称使用合法的小写字母、数字和下划线，并加本库前缀以减少冲突，例如 `demo_panel`。

- `res/layout/demo_panel.xml`：可通过 Android 资源编号加载的布局。
- `res/values/strings.xml`：`<resources>` 内的字符串等资源声明。
- `res/raw/demo_beep.ogg`：可通过资源编号读取的原始资源。
- `assets/demo/config.json`：通过 `context.getAssets().open("demo/config.json")` 读取，路径不包含 `assets/`。

应用构建统一链接所用资源，并按扩展库交付描述及 AAR 清单中的资源包名生成 R 类。类库的 `R.txt` 提供符号声明，最终资源编号由应用链接确定，不能把开发阶段的占位 R 类合并进 `classes.jar`。

依赖资源在前、使用者在后，项目资源具有最终覆盖优先级；应用主题和布局可引用已选 AAR 资源。资产应使用本库子目录：依赖间同路径、同内容可以合并，不同内容报错，项目自身同路径资产可显式覆盖。资源 XML、编号和运行主题必须通过应用构建与运行验证，Java 编译通过不能代替这些检查。

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
- 导出类型名及需要直接以普通标识符访问的名称避开 Simple 保留关键字。通过点号访问的方法、属性和字段允许与关键字同名，如 `对象实例.匹配(文本)`；具体语言规则见 [标识符](Simple语言定义.md#标识符)。
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
| `@UsesPermissions` | [批量 Android 权限声明](#android-权限) |
| `@UsesPermission` | [可重复的单项 Android 权限声明及版本上限](#android-权限) |
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
@UsesPermissions(permissionNames =
	"android.permission.INTERNET"
)
public final class HTTP服务 {
}
```

多个权限使用逗号分隔。编译器只为项目实际引用的对象收集权限。该注解只负责生成 Android 清单声明；危险权限仍需在应用运行时请求，并处理用户拒绝授权的情况。

单项权限可以使用 `simple.runtime.annotations.UsesPermission`，同一类型上允许重复声明；需要限制权限适用的最高 API 等级时使用 `maxSdkVersion`：

```java
@SimpleObject
@UsesPermission(permissionName = "android.permission.CAMERA")
@UsesPermission(
	permissionName = "android.permission.WRITE_EXTERNAL_STORAGE",
	maxSdkVersion = 28
)
public final class 拍摄服务 {
}
```

`permissionName` 必须是单项非空权限名称，不能填写逗号分隔的列表。`maxSdkVersion` 默认 `0`，表示不设上限；正数表示最高 API 等级，负数会导致编译失败。Java 会自动保存重复注解，开发者不需要手写容器注解。

两种权限注解可以同时使用。同名权限统一输出一项，范围覆盖所有实际引用对象的需求：任一声明不设上限，最终就不设上限；全部声明都有上限时，取最大的上限。批量注解中的权限均不设上限。

权限放在实际被引用的公开类型上，不要只放在未作为 Simple 对象加载的 `Impl` 或内部辅助类上。权限名称、系统版本条件与运行时请求按实际使用的 Android API 核实，不能把声明存在当作已经授权。

## Android 清单节点

`@ManifestNodes` 可以向 Android 清单的固定位置加入节点。以下为类型声明片段，须导入 `simple.runtime.annotations.SimpleObject` 和 `simple.runtime.annotations.ManifestNodes`：

权限必须使用上述权限注解声明；`@ManifestNodes` 不接受 `uses-permission`、`uses-permission-sdk-23` 或 `uses-permission-sdk-m` 节点，编译器会明确报错。

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

Activity、Service、Receiver 等辅助类使用完整 Java 类名，并确认已进入 `classes.jar` 或所需依赖 JAR。注解先生成应用节点，再与扩展库及 AAR 的独立清单通过官方 Manifest Merger 合并。第三方清单已有的内容无需重复迁入注解；项目可通过 `manifest.placeholder.<名称>` 提供占位符，未解决的冲突会导致构建失败。

## 构建 classes.jar

Java 开发需要 Android 平台和 `SimpleAndroidRuntime.jar`。使用其它构建工具时，源码与输出目录可自行组织，最终提供兼容的 `classes.jar` 及所需依赖、资源即可。

使用 SDK 的 `utils/library/library.ps1`，工具按自身位置定位 SDK，不依赖同级源码仓库。扩展库根目录维护 `src/`、`library.json` 和 `library.gradle`，本地 JAR 放 `libs/`，本地 AAR 放 `aars/`。

```powershell
& "D:\ES4A\sdk\utils\library\library.ps1" prepare "D:\LibraryWork\com.example.demo"
& "D:\ES4A\sdk\utils\library\library.ps1" compile "D:\LibraryWork\com.example.demo"
```

刷新依赖与 Java 编译共用解析结果。SDK 的 Android 平台和 Simple 运行库直接引用原文件；仓库依赖及 AAR 编译所需 JAR 由工具准备到 `build/ide/`。开发中间产物位于 `build/gradle/` 和 `build/ide/`；`classes.jar`、依赖描述、资源符号及生成清单直接保存在库根目录。作者的 `AndroidManifest.xml` 保持原样。命令和开发源码附件规则见 [扩展库工具说明](../utils/library/README.md)。

## 填写类库清单

Java 注解决定编译器能调用哪些成员；`library.json` 为 ES4A 提供分类、补全、悬停和设计器元数据。两者分别维护，清单不能代替 Java 实现。

`library.json` 是 IDE 和公共发布工具约定的类库清单文件名；IDE 加载所选 SDK 的 `libraries/*/library.json`。

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

通过 SDK 扩展库工具发布完整交付内容，例如：

```powershell
& "D:\ES4A\sdk\utils\library\library.ps1" publish "D:\LibraryWork\com.example.demo"
```

编译后的扩展库目录可直接复制到 SDK 的 `libraries/`，不需要附带 `build/`。`publish` 只是辅助安装命令，从已有编译结果复制本库实现、声明中的本地依赖、生成清单、资源和必要附加资料，不执行编译，不依赖 `src/` 或 `library.gradle`，也不包含 `test/`。存在 `example/` 时随库复制并排除其 `build/`。安装目录中的 `library.json` 即表示向 IDE 注册，无需额外列表；更新后刷新类库。不得只复制 `classes.jar` 而遗漏依赖描述与原始制品。

## 开发与验证流程

1. 确定 Java 包名、公开 API 和对象形态，按前文示例或自己的工程布局编写实现。
2. 构建 `classes.jar`，补齐真正需要的依赖、资源、资产和原生库。
3. 编写 IDE 清单，核对类型、继承、成员名称、参数顺序、类型、静态与传址标志，以及属性默认值。
4. 检查 IDE 清单后，使用本库独立的 `test/` 项目验证对象创建、函数、属性和事件；测试不依赖 SDK 中附带测试源码。
5. 编译应用检查资源、清单和 APK 内容；按所用功能在设备上验证权限、线程、失败处理、重复操作及资源清理，记录未验证项。

Java 编译、IDE 中的成员与设计器检查、APK 构建和设备运行应分别完成；编译成功不代表权限、资源或设备功能已经可用。

### 类库测试项目

每个类库只维护一份独立的 `test/` 测试项目，测试目录统一用单数。以下是 `test/project.properties` 的入口配置：

```properties
main=com.example.demo.test.主窗口
name=演示扩展库测试
source=./src
assets=./assets
```

主窗口位于 `test/src/com/example/demo/test/主窗口.simple`，限定名与 `main`、相对源码路径一致；应用名称和窗口标题表明测试用途。保持声明的资产目录存在，需要资源时再配置 `res=./res`。工程配置见 [Simple 项目开发](Simple项目开发.md)。

测试面向类库开发者，验证公开功能、属性读写、事件参数、生命周期及必要的异常路径。可确定的结果用固定输入和断言核对预期值；界面、声音、打印等需要观察的结果应明确核对标准，不能仅显示调用完成就判为成功。权限、设备、服务器或账号等条件就近写在界面或源码中，未满足条件的用例记录为未执行。

测试与教学演示职责分开，不另建示例工程，也不建立跨类库的集中测试项目。编译器、运行库与扩展库均须测试，但各扩展库独立维护自己的测试。测试是开发和发布前的验证内容，不随类库安装到 `sdk/libraries/`；当前阶段不规定或维护扩展库用户示例。

## 常见问题

| 现象 | 检查要点 |
| --- | --- |
| IDE 能看到类库，编译却找不到 | 检查实际扫描位置的 `classes.jar`、依赖，以及清单 `type` 与 Java 完整名称；IDE 可见只证明清单已加载 |
| 编译可用，IDE 不显示 | 检查所选 SDK 目录、`libraries/*/library.json`、分类 `hidden`、定义 `kind` 和继承关系，更新后刷新 |
| 缺少组件实现 | 实现类直接实现被注解的接口或直接继承被注解的组件类，并与接口一起交付 |
| 修改 Java 后行为没变 | 重新构建并安装本次 `classes.jar`；只改源码或开发目录 JAR 不会更新已安装 SDK |
| 重复类、缺失类型或方法不存在 | 按诊断来源检查全部已安装类库的依赖及实际选中版本，不能只检查当前项目使用的库 |
| Java 编译成功，资源失败 | 检查 `res/` XML、资源名、重名及未生成的第三方包 `R`；Java 构建不验证 Android 资源 |
| 原生库加载失败 | 检查 APK `lib/<ABI>/` 内容、设备进程架构、加载名称、原生依赖和 JNI 绑定；改目录名不能转换 ABI |

## 延伸参考

- [类库清单定义](类库清单定义.md)：分类、成员、属性编辑器及设计器投影字段。
- [Simple 项目开发](Simple项目开发.md)：选择 SDK、使用项目工具和项目模板。
