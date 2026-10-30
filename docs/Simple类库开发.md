# Simple 类库开发

本文介绍如何用 Java 编写可供 Simple 使用的对象和组件。清单字段单独说明，见 [类库清单定义](类库清单定义.md)。

## 开发准备

下载并解压 [完整 SDK](https://dwz.wsd.cx/es4a-sdk)，无需获取或构建编译器、运行库源码。创建类库工程的方法见 [扩展库开发工具](../utils/library/README.md)，也可参考 [演示类库](https://gitee.com/es4a/libraries/tree/main/com.example.demo)。

开发时主要维护以下内容：

| 内容 | 位置 |
| --- | --- |
| Java 源码 | `src/` |
| IDE 中显示的类型、成员及说明 | `library.json` |
| 命名空间和仓库依赖 | `library.gradle` |
| 本地 JAR、AAR | `libs/`、`aars/` |
| 本库测试项目 | `test/` |

按功能选择写法：计算、文本处理等可用普通对象；需要 Android 界面、容器或生命周期的功能使用组件。先完成一个可调用的类型，再逐步添加成员。

## 普通对象

下面的类提供一个实例函数和一个静态函数。保存为 `src/com/example/demo/演示对象.java`：

```java
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
	 * 供 Simple 的“创建 演示对象”调用。
	 */
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

在已有 Simple 主窗口中调用：

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

运行后，实例函数给文本加上前缀，静态函数返回文本长度。普通对象通过 `创建` 实例化，必须提供公开无参构造方法；Simple 不会自动调用带参 Java 构造方法。

`@SimpleObject` 让类型可被 Simple 识别，`@SimpleFunction` 公开方法。静态方法属于类型，实例方法属于创建的对象；静态函数不会自动变成全局函数，清单中的 `global` 也不改变这一点。

### 函数和过程

使用 `@SimpleFunction` 公开方法：有返回值时是函数，返回 `void` 时是过程。同名方法可按参数个数重载，不能只用参数类型区分同参数个数的重载。

参数和返回值应使用 Simple 支持的类型，见 [数据类型对应](#数据类型对应)。Java 中的参数名使用英文，清单展示给用户的参数名使用中文。

### 常量和变量

字段使用 `@SimpleDataElement` 公开。以下成员放在类型内部，并导入 `simple.runtime.annotations.SimpleDataElement`：

```java
@SimpleDataElement
public static final int 模式_普通 = 0;

@SimpleDataElement
public static int 当前模式 = 模式_普通;
```

`public static final` 且可在编译期读取的字段成为常量；其他公开字段成为变量。常量使用逻辑、数值、文本等编译器可识别的基本类型。

### 属性

用同名的获取器和设置器公开属性。以下成员放在类型内部，并导入 `simple.runtime.annotations.SimpleProperty`：

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

获取器没有参数并返回属性值；设置器接收一个参数，返回 `void`。两者名称和类型必须相同，只有获取器时是只读属性。

属性初值写在设置器的 `@SimpleProperty` 中。对象创建后，运行库按 `type` 解析 `initializer` 并调用设置器，不要在构造方法中重复设置同一个初值。

Java 注解的 `initializer` 决定运行时初值；JSON 中的同名字段决定 IDE 未赋值时显示什么，二者需同步维护：

| 属性 | Java 设置器的注解参数 | 清单的初值表达式 |

Java 逻辑初值使用 `True`、`False`，清单使用 Simple 表达式 `真`、`假`。注意 Java 与 JSON 各自的字符串转义；`initializer` 不用于执行任意 Simple 脚本。

### 事件

用 `@SimpleEvent` 声明事件，在事件实际发生时调用 `EventDispatcher.dispatchEvent`。以下是普通对象内的成员，需导入 `simple.runtime.annotations.SimpleEvent` 和 `simple.runtime.events.EventDispatcher`：

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

事件名、参数类型和顺序要与清单一致。组件接口只声明事件，实现类负责分派，见下节。

## Android 组件

组件通常分为公开接口和 Android 实现类。接口供 Simple 使用，实现类完成实际操作：

| 功能形态 | 公开类型 | 实现基础 |

### 非可视组件

下面的组件接收文本并触发完成事件。示例是同步操作；网络、蓝牙等耗时任务的处理见 [线程、初始化与生命周期](#线程初始化与生命周期)。

保存为 `src/com/example/demo/演示任务.java`：

```java
package com.example.demo;

import simple.runtime.annotations.SimpleComponent;
import simple.runtime.annotations.SimpleEvent;
import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;
import simple.runtime.components.组件;

/**
 * 接收文本并通过事件返回结果的非可视组件。
 *
 * @author 树先生 xhwsd@qq.com
 */
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

保存为 `src/com/example/demo/演示任务Impl.java`：

```java
package com.example.demo;

import simple.runtime.components.组件容器;
import simple.runtime.components.impl.组件Impl;
import simple.runtime.events.EventDispatcher;

/**
 * 将公开组件接口与执行逻辑连接。
 *
 * @author 树先生 xhwsd@qq.com
 */
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

下面是一个按钮的公开接口，保存为 `src/com/example/demo/演示按钮.java`：

```java
package com.example.demo;

import simple.runtime.annotations.SimpleComponent;
import simple.runtime.annotations.SimpleEvent;
import simple.runtime.annotations.SimpleObject;
import simple.runtime.annotations.SimpleProperty;
import simple.runtime.components.可视组件;

/**
 * 提供标题属性和单击事件的演示按钮。
 *
 * @author 树先生 xhwsd@qq.com
 */
@SimpleComponent
@SimpleObject
public interface 演示按钮 extends 可视组件 {

	/* 事件 */

	/**
	 * 用户单击按钮时触发。
	 */
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

`组件` 是非可视组件的基础接口，`可视组件` 表示具有 Android View，`组件容器` 表示能够容纳子组件。可视容器可以同时继承后两者，还须在实现类中实现添加子组件和布局等功能。

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

该实现创建真实按钮，在单击时分派事件。实现类必须直接实现带 `@SimpleComponent` 的接口，或直接继承带该注解的组件类，并提供接收 `组件容器` 的公开构造方法；间接继承时，当前编译器不能自动配对。

可视组件应返回真实 Android View。接口与实现类放在同一个类库中，事件按实际操作触发，不在属性设置器中伪造事件。

### 线程、初始化与生命周期

| 时机或操作 | 要注意什么 |
| --- | --- |
| 构造组件 | 只做必要装配；依赖属性配置的连接、扫描等操作应稍后启动 |
| 实现 `createView()` | 父类构造器会调用它，此时子类字段尚未完成初始化 |
| 设置属性初值 | 运行库会调用设置器，设置器应能接受声明的默认值 |
| 执行耗时 I/O | 使用工作线程；界面操作及供界面处理的完成、失败事件切回主线程 |
| 分派事件 | `EventDispatcher.dispatchEvent` 在当前线程同步执行，不会自动切换线程 |
| 销毁组件 | 释放已获取的线程、连接、接收器、服务绑定和监听，调用 `super.销毁()` 保留基础解绑 |
| 监听 Activity 生命周期 | 按需登记运行库监听，`组件Impl` 不会自动登记所有监听；销毁后不再分派旧回调 |

编译器会在 Windows 上反射分析 Java 类型，读取静态常量也可能触发类初始化。不要在静态初始化块中访问设备、打开连接或无条件加载原生库。

## Android 权限

在实际供 Simple 使用的公开类型上声明权限，不要只写在 `Impl` 或内部辅助类上。

批量声明使用 `@UsesPermissions`，多个权限用逗号分隔。以下是类型声明片段，需导入 `SimpleObject`、`UsesPermissions` 注解；组件还需自己的接口和组件注解：

```java
@SimpleObject
@UsesPermissions(permissionNames = "android.permission.INTERNET")
public final class HTTP服务 {
}
```

单项声明使用 `@UsesPermission`，同一类型可重复书写。需要限制最高 API 等级时，设置 `maxSdkVersion`：

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

`permissionName` 只能填写一个非空权限名。`maxSdkVersion` 默认为 `0`，表示不设上限；正数是最高 API 等级，负数会导致编译失败。重复注解由 Java 自动处理，无需手写容器注解。

两种注解可以混用。编译器收集项目实际引用对象的权限，同名权限只输出一次：任一声明无上限，结果就无上限；全部有上限时取最大值。批量注解中的权限均无上限。

这些注解只生成清单声明。危险权限仍需运行时请求，并处理用户拒绝；权限名称和适用系统版本按实际 Android API 核实。

## Android 清单节点

需要加入 Activity、Service 或其他 Android 清单节点时，使用 `@ManifestNodes`。以下是类型声明片段，需导入 `SimpleObject` 和 `ManifestNodes`：

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

权限使用上一节的权限注解；`@ManifestNodes` 不接受 `uses-permission`、`uses-permission-sdk-23`、`uses-permission-sdk-m` 节点，填写后编译会报错。

节点中的宏允许由使用者配置：`${宏名}` 必填，`${宏名=缺省值}` 可省略，缺省值也可以为空文本。在应用的 `project.properties` 中按“Java 类简名.宏名”填写：

```properties
示例对象.应用标识=my-app-id
```

宏是文本替换，不自动转义 XML；美元符号和反斜杠按字面保留，替换后必须仍是合法 XML。只有项目实际引用对象的节点参与注入。

辅助类使用完整 Java 类名，并确保已包含在类库或依赖中。已有第三方清单节点不必重复写入注解；应用可通过 `manifest.placeholder.<名称>` 提供 AAR 占位符，未解决的合并冲突会中断构建。

## 填写类库清单

在类库根目录维护 `library.json`，让 IDE 知道有哪些类型和成员。名称、类型、参数顺序和静态等标记要与 Java API 保持一致；组件填写公开接口或组件类，不填写 `Impl` 类名。

结构、字段和示例统一见 [类库清单定义](类库清单定义.md)。

### IDE 图标

图标配置见 [组件图标](类库清单定义.md#组件图标)。

## 构建 classes.jar

打开类库的 `.code-workspace`，运行“ES4A扩展库: 构建类库”，按提示选择是否包含离线依赖、是否输出到 SDK。需要更新代码提示时，运行“ES4A扩展库: 刷新依赖”。

也可从 SDK 根目录执行：

```powershell
& .\utils\library\library.ps1 compile "D:\LibraryWork\com.example.demo"
```

`compile` 自动准备依赖并编译，不要求先执行 `prepare`；`prepare` 只刷新依赖和工作区，不编译本库。`compile` 不改写工作区和源码清单。

| 需要做什么 | 命令或选项 |
| --- | --- |
| 构建到本库 `build/<基础包名>/` | `compile <库目录>` |
| 包含离线依赖 | 添加 `-Dependencies include`，默认是 `exclude` |
| 直接构建到 SDK | 添加 `-Destination sdk` |
| 指定其他输出位置 | `compile <库目录> <交付根目录>`，自动追加基础包名，不与 `-Destination sdk` 同用 |
| 刷新 VS Code 代码提示 | `prepare <库目录>` |

使用其他构建工具时，需引用 Android 平台和 `SimpleAndroidRuntime.jar`，并遵守本文类型及依赖约定。

## 开发与验证流程

1. 构建类库，确认 Java 实现可以编译。
2. 在 IDE 检查清单中的类型、成员、继承、参数及属性默认值。
3. 用本库的 `test/` 编译 APK，再到设备上检查功能、权限、事件、失败处理和资源释放。
4. 测试通过后再安装供其他项目使用；未执行的验证明确记录。

Java 编译、IDE 显示和设备功能需要分别检查，不能相互代替。

### 类库测试项目

每个类库维护一份独立的 `test/`，目录名用单数。以下配置保存为 `test/project.properties`：

```properties
main=com.example.demo.test.主窗口
name=演示扩展库测试
source=./src
assets=./assets
```

主窗口对应 `test/src/com/example/demo/test/主窗口.simple`。声明的资产目录须存在，需要 Android 资源时再配置 `res`；其他项目配置见 [Simple 项目开发](Simple项目开发.md)。

测试名称和界面应表明测试用途。确定性结果使用固定输入和断言，界面、声音、打印等写清观察标准；设备、权限、服务器或账号条件就近说明。测试不与教学演示混用，也不随类库安装到 SDK。

## 安装与接入 SDK

将构建生成的 `build/<基础包名>/` 整个目录复制到 SDK 的 `libraries/`。也可从 SDK 根目录执行：

```powershell
& .\utils\library\library.ps1 publish "D:\LibraryWork\com.example.demo"
```

`publish` 只安装已有结果，不重新编译。更新后执行“ES4A: 刷新类库”即可；无需复制源码、测试或开发缓存。

## 编译器读取的类库结构

交付目录由工具生成，不需要手工整理其中的文件。复制时保留整个目录，避免遗漏依赖和资源。

本地依赖放在 `libs/*.jar`、`aars/*.aar`。本地 JAR 的源码附件与原包同目录，命名为 `*-sources.jar`，只供 IDE 使用。工具不主动收集、下载许可证；已有及上游包自带的许可证、NOTICE 和源码版权声明随对应内容保留。

### 加载与打包范围

编译器扫描已安装库的导出类型，再为项目实际引用的库准备运行依赖。未使用库的依赖缺失不阻断构建，但损坏的本体 JAR 或重复导出类型仍会报错。

项目引用库内对象后，整个类库 JAR、所需依赖和资源参与打包，不按方法裁剪。仅在资源 XML 中填写库的主题名称，不会单独选入该库；普通第三方 Java 类型也不会自动变成 Simple API，需自行封装并添加注解。

### 依赖版本与冲突

仓库依赖在 `library.gradle` 中声明，Java 编译与 IDE 共用 Gradle 解析结果。工具链统一使用 Java 11、编译 API 30、最低 API 24，Android 平台和 Simple 运行库直接引用 SDK，不重复打包。

应用构建统一选择实际所用库的依赖版本。需要传递给应用的版本要求写成 Gradle 依赖约束；开发工程的 `resolutionStrategy` 不会自动传递。

| 依赖情况 | 处理方式 |
| --- | --- |
| 仓库模块 | Gradle 处理版本约束、排除规则和 Android 变体 |
| 独立 JAR，包括 AAR 内嵌的 `libs/*.jar` | 按仓库坐标、唯一有效的 `pom.properties`、`名称-版本.jar` 的顺序识别身份；版本用 Maven `ComparableVersion` 比较，支持 `rc`、`SNAPSHOT` 等限定符 |
| 身份或版本无法可靠识别的文件 | 只按内容去重，不静默覆盖 |
| 同身份同版本但内容不同，或最终仍有重复类 | 报告冲突及双方来源 |
| 本地包与 Gradle 已选的同模块内容不同 | 报错，需统一依赖声明或本地原包 |
| AAR 本体中的 `classes.jar` | 跟随所属 AAR，不按文件名跨 AAR 选版；相同内容归档去重 |
| 本地包旁的同名 POM | 不解析传递依赖；所需外部依赖在 `library.gradle` 显式补齐 |

自动选版不保证二进制和运行兼容，升级依赖后仍需测试。

### 准备和消费离线依赖

默认构建只携带仓库依赖声明。需要随库携带已下载依赖时，从 SDK 根目录执行：

```powershell
& .\utils\library\library.ps1 compile "D:\我的类库" -Dependencies include
```

工具会在交付目录增加 `dependencies/`，复制整个类库即可携带，准备过程可能联网。

应用可联合复用随库依赖、应用侧仓库和 Gradle 缓存。依赖及必要元数据齐全时，项目工具的 `-Offline` 可在空缓存中构建 APK；缺项时报错，不自动降级。各库仍统一选版，同坐标文件内容不同会报错，应用不会改写随库仓库。

离线构建仍需完整 SDK。依赖宜指定明确版本；`-RefreshDependencies` 用于联网刷新，不能与 `-Offline` 同用。

### 与 AAR 的关系

本地 AAR 放在 `aars/`，仓库 AAR 在 `library.gradle` 中声明。代码、资源、资产、原生库、清单及对应包的 R 类由工具处理，无需手工解包。

当前不消费 AAR 的 ProGuard、Lint、API 检查及 Prefab 配置，也不以 `public.txt` 限制资源访问，依赖这些能力的库需另行适配。

### Android 资源与资产

| 内容 | 示例 | 用法 |
| --- | --- | --- |
| 布局 | `res/layout/demo_panel.xml` | 通过资源编号加载 |
| 字符串 | `res/values/strings.xml` | 在 `<resources>` 内声明 |
| 原始资源 | `res/raw/demo_beep.ogg` | 通过资源编号读取 |
| 资产文件 | `assets/demo/config.json` | `context.getAssets().open("demo/config.json")`，路径不带 `assets/` |

资源名使用合法的小写字母、数字和下划线，建议加本库前缀减少冲突。应用统一链接资源并生成各包的 R 类，最终编号由应用链接决定，不要把开发阶段占位 R 类打进本库 JAR。

资源按“依赖在前、使用者在后”合并，项目资源最后覆盖。资产建议放本库子目录：依赖间同路径同内容可以合并，不同内容报错，项目资产可以显式覆盖。Java 编译通过后，仍要验证应用资源编译和运行主题。

### 原生库

预编译的 `.so` 按设备 ABI 放在 `jni/<ABI>/`：

```text
jni/
├─ armeabi-v7a/libdemo.so
└─ arm64-v8a/libdemo.so
```

Java 用 `System.loadLibrary("demo")` 加载 `libdemo.so`。如果依赖其他非系统原生库，也需提供对应 ABI 的文件，并按要求加载；修改 JNI 绑定的包名、类名或方法签名时，须同步原生实现。

编译器只打包原生库，不编译 C/C++、补齐 ABI 或转换机器码，也不扫描 `libs/<ABI>/`。APK 中的路径为 `lib/<ABI>/`。目录改名不能转换 ABI；只有 32 位原生库时，不支持仅运行 64 位应用的设备，必须在匹配设备上验证加载。

## Java 编写约定

示例中的完整文件可直接作为开发参考；成员片段放入所属类型，并补齐注明的 import。代码统一遵循以下约定：

- 导出类型和成员按需使用中文，MQTT、NFC、TCP 等缩写保留规范大小写；Java 参数及内部实现使用英文，清单参数使用中文。
- 类型名和需直接作为标识符使用的名称避开 Simple 关键字；点号后的成员名允许与关键字同名，如 `对象实例.匹配(文本)`，见 [标识符](Simple语言定义.md#标识符)。
- 实现类通常以 `Impl` 结尾，配对依据仍是直接继承关系，不是名称。
- 组件接口按“常量、事件、属性、方法”排列；实现类先放字段和构造，获取器与设置器相邻，辅助方法按职责归组。
- 类型、构造方法和非重写方法使用多行 JavaDoc，写明用途及必要的参数、返回值、限制，完整类型保留作者信息。重写方法可继承说明；`void` 方法和构造方法不写 `@return`。
- JavaDoc、注解与声明之间不留空行。无参数或单个简短参数的注解可写一行，多参数或长内容分行，结束括号另起一行。组件接口声明 Simple 注解，实现类使用 `@Override`。
- 使用 UTF-8、CRLF、Tab 缩进（宽度 4）和末尾换行；方法及功能块之间留空行，注释说明职责、状态和兼容原因。

## 注解映射

只有带 `@SimpleObject` 的类型会作为 Simple 对象加载。其他公开成员按相应注解识别：

| Java 声明 | Simple 中的含义 |

## 数据类型对应

| Java 类型 | Simple 类型 |

公开 API 优先使用这些类型。集合、泛型、可变参数和第三方 SDK 类型不会自动转换，需封装成明确的 Simple API。内部辅助类不加 `@SimpleObject`。

传址参数使用 `simple.runtime.parameters` 中的引用参数类型，例如 `IntegerReferenceParameter`、`StringReferenceParameter`。以下成员需导入 `SimpleFunction` 和 `IntegerReferenceParameter`：

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

清单参数同时声明 `byRef: true`，不能只改 JSON 而仍使用普通 `int`。

## 常见问题

| 现象 | 检查要点 |

## 延伸参考

- [类库清单定义](类库清单定义.md)：填写类型、成员、属性编辑器及设计器投影。
- [Simple 项目开发](Simple项目开发.md)：创建项目、编译 APK 和运行应用。
