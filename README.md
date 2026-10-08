# ES4A SDK

> SDK 入口说明｜核对日期：2026-10-08｜依据：当前入口清单、能力脚本与扩展加载代码｜适用范围：SDK 根目录维护

## 入口与目录

本目录提供 Simple 编译器、Android 运行库、扩展类库和开发工具。入口为 [`sdk.json`](sdk.json)，由 ES4A 的“选择 SDK”命令保存到 `es4a.sdk.path`。

ES4A 只加载用户选定的本地 JSON 入口；未配置或路径失效时保持未加载状态。各清单提供 IDE 元数据，实际编译与运行由能力脚本和工具链执行。

```text
sdk/
├─ sdk.json          # SDK 入口清单
├─ capabilities/     # 编译、安装和启动等能力脚本
├─ simple/           # 编译器、运行库及其 IDE 清单
├─ libraries/        # 扩展类库
├─ templates/        # 项目与单元模板
└─ tools/            # JDK、Android 等工具链
```

- [编译器、运行库与通用清单](simple/README.md)：核心交付物及三类清单共用的字段、属性规则。
- [扩展类库开发](libraries/README.md)：类库目录、Java 实现、构建、打包和交付。

## 入口字段

下例展示核心入口字段，完整配置见 `sdk.json`：

```json
{
	"compiler": "simple/SimpleCompiler.json",
	"runtime": "simple/SimpleAndroidRuntime.json",
	"libraries": [
		"libraries/com.example.demo/library.json"
	]
}
```

| 字段 | 说明 |
| --- | --- |
| `name`、`description` | 可选名称和说明元数据。 |
| `version` | 可选 SDK 版本。 |
| `authors` | 可选作者数组；每项 `name` 必需，`email` 可选。 |
| `compiler` | 编译器清单路径，目标 `kind` 为 `compiler`。 |
| `runtime` | 运行库清单路径，目标 `kind` 为 `runtime`。 |
| `libraries` | 可选类库清单路径数组，目标 `kind` 为 `library`。 |
| `capabilities` | 可选项目能力与独立工具声明。 |
| `templates` | 可选项目与单元模板声明。 |

相对路径统一以声明它的清单所在目录为基准。入口中的清单、命令和模板路径相对于 `sdk.json`；子清单中的图标路径相对于该子清单。

编译器和运行库是完整 SDK 的必备项。入口无法读取或结构无效时加载失败；编译器、运行库或单个类库清单有误时记录 SDK 问题，保留其他有效清单。

## 类库注册

- `libraries` 必须为明确路径数组，仅加载注册的清单，不支持通配表达式。
- 编译器、运行库固定在前；扩展类库按注册数组顺序加载和显示，调整数组位置即可调整顺序。
- 重复路径只保留第一次注册；同名定义优先使用最先加载的定义。
分类、定义及设计器分组规则见[通用清单](simple/README.md#分类与定义)。

此处登记控制 IDE 元数据。Simple 编译器通过 `LIBRARIES_HOME` 扫描类库代码，具体规则见[编译与打包](libraries/README.md#编译与打包)。

## 能力调用

项目能力以稳定标识注册，例如：

```json
{
	"capabilities": {
		"compile": {
			"command": "capabilities/compile.bat",
			"description": "编译应用",
			"arguments": ["PROJECT_FILE"]
		},
		"debug": {
			"command": "capabilities/debug.bat",
			"description": "调试应用",
			"arguments": ["PROJECT_FILE", "APK_FILE"]
		}
	}
}
```

- 对象键为能力标识，`command` 必需；`name` 省略时使用标识作为名称，`description` 可选。
- `arguments` 是可选字符串数组，按声明顺序传入项目参数。未知标识或无法解析的值会在启动前报错，扩展不添加未声明的参数。
- `capabilities.projects` 也可用数组声明项目能力；数组项 `name`、`command` 必需，`id` 省略时使用 `name`。同标识的直接声明优先。
- `capabilities.tools` 用数组声明独立工具，字段要求与 `projects` 相同；当前工具命令直接启动，不传入项目参数。

| 项目参数标识 | 传入值 |
| --- | --- |
| `PROJECT_FILE` | 项目的 `project.properties` 绝对路径。 |
| `APK_FILE` | 项目构建目录下 `deploy/<应用名称>.apk` 的绝对路径。 |

当前 `compile` 使用 SDK 自带 JDK、Android API 26 和 Simple 工具链编译项目；`debug` 先编译，再通过 ADB 安装 APK 并启动应用。脚本负责工具路径、环境变量和执行流程。

## 模板

模板以稳定标识登记在 `templates` 中，每项包含 `template` 路径和 `variables` 数组。例如：

```json
{
	"templates": {
		"window": {
			"template": "templates/窗口.simple",
			"variables": ["窗口名称"]
		}
	}
}
```

| 标识 | 模板路径 | 变量 |
| --- | --- | --- |
| `project` | `templates/project.properties` | 主窗口限定名、应用名称 |
| `main` | `templates/主窗口.simple` | 应用名称 |
| `window` | `templates/窗口.simple` | 窗口名称 |
| `object` | `templates/对象.simple` | 对象名称 |
| `interface` | `templates/接口.simple` | 无（`[]`） |
| `service` | `templates/服务.simple` | 无（`[]`） |

占位符写作 `{$变量名}`。`variables` 必须与模板中的变量名称完全一致；文件不存在、重复声明、未声明或未使用的变量会使该模板不可用。

新建项目使用 `project` 与 `main` 模板；普通单元按类型选择模板。Simple 单元模板替换变量后，属性区先解析为 XML 模型，再通过统一持久化边界生成文件。

## 维护检查

1. 入口及子清单是有效 UTF-8 JSON，路径存在，`kind` 与用途一致。
2. 能力命令存在，参数标识和顺序与脚本一致。
3. 模板文件存在，占位符与 `variables` 一致。
4. 在 ES4A 中刷新类库，确认 SDK 无加载问题，并验证本次修改涉及的功能。
