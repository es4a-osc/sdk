# Simple 扩展类库开发

> 类库接入说明｜核对日期：2026-10-08｜依据：当前类库样例、公共构建脚本与编译器源码｜适用范围：`sdk/libraries` 下的类库开发与交付

每个类库占用一个直接子目录。`library.json` 提供 IDE 元数据，`classes.jar` 提供实际代码，两者须与 Java 实现一致。

SDK 选择与登记见[入口注册规则](../README.md#类库注册)；公共字段和属性规则见[通用清单](../simple/README.md#通用清单结构)。

## 类库目录

```text
libraries/
├─ build.bat              # 公共构建脚本
└─ <类库>/
   ├─ library.json        # IDE 清单
   ├─ classes.jar         # 交付代码
   ├─ src/                # Java 源码
   ├─ icons/              # 清单引用的 SVG
   ├─ libs/               # 可选 JAR 依赖
   ├─ res/、assets/、jni/  # 可选资源、资产和原生库
   ├─ sample/             # 验证项目
   └─ build/              # 生成目录
```

## 清单与接入

入门先读[演示类库](com.example.demo/README.md)。下例取自其 [library.json](com.example.demo/library.json)，展示最小可视组件声明：

```json
{
	"name": "演示扩展库",
	"kind": "library",
	"categories": [
		{
			"name": "扩展可视组件",
			"definitions": [
				{
					"name": "演示按钮",
					"kind": "component",
					"type": "com.example.demo.演示按钮",
					"icon": "icons/square-rounded-check.svg",
					"inherits": [
						"simple.runtime.components.可视组件"
					]
				}
			]
		}
	]
}
```

按实际实现补充[成员与参数](../simple/README.md#成员与参数)，以及需要的[属性元数据](../simple/README.md#缺省值与候选值)、[投影](../simple/README.md#设计器投影)和[编辑器](../simple/README.md#属性编辑器)。清单只描述已有能力，不生成 Java 代码。

在 `sdk.json` 的 `libraries` 数组登记该清单，再执行“刷新类库”。详细路径和顺序规则统一见 [SDK 说明](../README.md#类库注册)。

## Java 实现与构建

通过运行库注解公开能力：

| 注解 | 用途 |
| --- | --- |
| `@SimpleObject` | 公开 Simple 对象。 |
| `@SimpleComponent` | 声明可由组件系统创建的组件类型。 |
| `@SimpleProperty`、`@SimpleEvent`、`@SimpleFunction` | 公开属性、事件和函数。 |

从 `sdk/libraries` 目录调用公共 [`build.bat`](build.bat)，也可将类库文件夹拖到脚本上：

```bat
build.bat "E:\路径\到\类库文件夹"
```

脚本使用 SDK 自带 JDK 8 编译 `src`，类路径包含 Android API 26、`SimpleAndroidRuntime.jar` 和类库 `libs/*`；清理 `build/classes` 后编译并生成根目录 `classes.jar`。

## 编译与打包

SDK 编译入口将 `sdk/libraries` 传入 `LIBRARIES_HOME`。编译器扫描直接子目录，仅加载含 `classes.jar` 的类库；未设置或目录不存在时只加载核心运行库。IDE 注册不改变编译器扫描范围。

| 目录或文件 | 处理方式 |
| --- | --- |
| `classes.jar` | 加入类加载，项目引用后作为整体进入 DEX；已引用类库在打包时缺失此文件则报错。 |
| `libs/*.jar` | 依赖先进入类加载器，随引用类库加入 DEX。 |
| `res/` | 编译并合并 Android 资源。 |
| `assets/` | 打包资产文件。 |
| `libs/` 的非 class 资源、`jni/` | 参与 APK 组装；原生库按 ABI 组织。 |

仅当前项目实际引用的类库连同其依赖、资源、资产和原生库进入 APK。

## Android 清单扩展

使用 `simple.runtime.annotations.ManifestNodes` 向 Android 清单追加节点。例如：

```java
@SimpleObject
@ManifestNodes(
	applicationXml = "<meta-data android:name=\"demo.appId\" android:value=\"${应用标识}\" />",
	activityXml = "<meta-data android:name=\"demo.mode\" android:value=\"${模式=normal}\" />")
public final class 示例对象 {
}
```

| 注解字段 | 插入位置 |
| --- | --- |
| `rootXml` | `/manifest` |
| `applicationXml` | `/manifest/application` |
| `activityXml` | 主 `activity` |
| `intentFilterXml` | 主 `activity/intent-filter` |

节点仅在项目实际引用该对象时注入。`${宏名}` 为必填宏，缺失时编译失败；`${宏名=缺省值}` 使用可选缺省值，缺省值允许为空。

在 `project.properties` 中按“类简名.宏名”赋值：

```properties
示例对象.应用标识=my-app-id
示例对象.模式=debug
```

宏只作文本替换，不自动转义 XML；注解及替换结果必须组成合法 Android 清单。

## 交付检查

1. 清单、Java 类型、继承和成员一致，图标及依赖路径存在。
2. 通过公共构建脚本生成 `classes.jar`，将依赖、资源与原生库放入约定目录。
3. 在 SDK 入口登记并刷新类库，用真实项目验证 IDE、编译、APK 打包和设备运行。
4. 使用清单扩展时，验证有引用、无引用、缺失必填宏和含特殊字符的宏值。
