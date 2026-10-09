# SDK 清单定义

文档类型：SDK 入口字段参考。核对日期：2026-10-09。适用范围：当前 SDK 与 ES4A 扩展。依据：随附 sdk.json、模板与能力脚本，并核对扩展 SDK 加载、参数解析及编译器源码。

[`sdk.json`](../sdk.json) 注册编译器、运行库和扩展类库，并声明工具能力与项目模板。

SDK 根目录包括 `sdk.json`、`capabilities/`、`simple/`、`libraries/`、`templates/` 和 `tools/`。ES4A 仅加载用户通过“选择 SDK”明确指定的入口；未配置或入口路径失效时保持未加载状态，不推断工作区中的 SDK。

## 入口字段

下例展示核心入口字段，完整配置见 [sdk.json](../sdk.json)：

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

分类、定义及设计器分组规则见[清单定义参考](类库清单定义.md#分类与定义)。

此处登记控制 IDE 元数据。Simple 编译器通过 `LIBRARIES_HOME` 扫描类库代码，具体规则见[编译与打包](Simple%20类库开发.md#编译与打包)。

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

当前 `compile` 使用按 [SDK 工具准备](../README.md#工具准备) 放置的 JDK、Android API 26 和 Simple 工具链编译项目；`debug` 先编译，再通过 ADB 安装 APK 并启动应用。脚本负责工具路径、环境变量和执行流程。

### 编译能力与签名

当前编译器默认使用调试签名。自定义签名由项目配置中的证书位置 `key.location`、别名 `key.alias` 和能力进程的环境变量 `KEY_PASSWORD` 提供；`key.password` 不生效，缺少证书或密码时仍使用调试签名。SDK 维护者接入发布流程时，应验证 APK 的实际签名，不能仅以编译成功判断发布签名已生效。

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
