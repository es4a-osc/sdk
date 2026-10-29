# Simple 项目开发

Simple 项目通过 `project.properties` 组织源码、资源和构建配置。语言规则见 [Simple 语言定义](Simple语言定义.md)。

开发 Simple 应用使用 SDK 的编译器、运行库及按需安装的扩展库，无需编写 Java 或重建已有类库。

## 准备开发环境

准备 Windows 和 Visual Studio Code，下载并安装 [ES4A 扩展](https://dwz.wsd.cx/es4a-vsix)，再下载并解压 [打包版 SDK](https://dwz.wsd.cx/es4a-sdk)；从 [SDK 仓库](https://gitee.com/es4a/sdk) 获取时，按 [工具准备](../README.md#工具准备) 下载 JDK、Gradle 并核对 Android 工具目录。工具齐全后无需另配全局环境变量。

当前随附项目工具使用以下 SDK 内路径，解压后保持工具目录结构：

| 路径 | 用途 |
| --- | --- |
| `tools/jdk-11.0.32.1/` | JDK 11 |
| `tools/gradle-8.14.6/` | 应用及扩展库依赖解析所用 Gradle |
| `tools/android-sdk/` | Android 编译、打包、安装和启动工具，随附脚本使用 API 30 |
| `simple/SimpleCompiler.jar`、`simple/SimpleAndroidRuntime.jar` | 编译器与 Android 运行库 |
| `libraries/<类库目录>/` | 已安装扩展库及其所需依赖和资源 |

在 ES4A“类库”标题栏或命令面板执行“ES4A: 选择 SDK”，选择 SDK 根目录。该目录须包含 `simple/SimpleCompiler.json` 和 `simple/SimpleAndroidRuntime.json`；`es4a.sdk.path` 保存所选目录的绝对路径。未配置或目录失效时保持未加载状态，不从工作区推断 SDK，也不接受旧入口 JSON 文件。SDK 不使用 `sdk.json`、`sdk.local.json` 或额外注册列表。

确认编译器、运行库和需要的扩展库已加载。扩展库按需从 [类库仓库](https://gitee.com/es4a/libraries) 安装到 SDK 的 `libraries/`，其中的 `*/library.json` 自动注册。更新 SDK 或类库后刷新类库，移动 SDK 后重新选择目录。目录概要见 [SDK README](../README.md)，发现与刷新规则见 [路径与注册](类库清单定义.md#路径与注册)。

## 项目结构

```text
HelloSimple/
├─ project.properties
├─ src/
│  └─ com/example/hello/主窗口.simple
├─ res/
├─ assets/
└─ build/
```

`project.properties` 是项目入口，目录路径相对于该文件所在目录。例如：

```properties
main=com.example.hello.主窗口
name=你好 Simple
version.code=1
version.name=1.0
source=./src
res=./res
assets=./assets
build=./build
```

| 属性 | 用途 |
| --- | --- |
| `main` | 主窗口完整限定名；其所在包名同时决定应用包名 |
| `name` | 应用显示名称；最终 APK 固定为构建目录下的 `apks/signed.apk` |
| `version.code`、`version.name` | 整数版本号与显示版本文字 |
| `source` | 源码根目录；多个根目录用逗号分隔 |
| `res`、`assets` | Android 资源目录与原样打包的资产目录 |
| `build` | 编译输出目录 |
| `icon`、`orientation`、`theme` | 应用图标、方向与主题 |

`main` 和 `source` 应明确填写；上例的 `res`、`assets`、`build` 分别采用常用目录 `./res`、`./assets`、`./build`。属性文件使用 UTF-8。其他可选属性可参考随附项目模板。`src/com/example/hello/主窗口.simple` 的限定名为 `com.example.hello.主窗口`，不在代码中另写包名声明。

## 第一个按钮应用

1. 在 ES4A 项目标题栏创建项目，选择上级目录，填写项目目录名、包名和应用名称。
2. 包名例如 `com.example.hello`；至少两段，每段以英文字母开头，只含英文字母、数字和下划线。
3. 打开主窗口设计器，拖入运行库“按钮”，名称设为 `按钮1`，文本设为“打招呼”。
4. 为按钮添加 `被单击` 事件，在事件体写入提示调用：

```simple
事件 按钮1.被单击()
	显示提示框("你好，Simple")
结束 事件
```

已有事件声明时只填写事件体，不再嵌套事件声明。分别保存代码和设计器中的修改，再编译运行。

## 项目与单元

已有项目通过“添加项目”选择 `project.properties`。扩展库源码中的 `test/project.properties` 面向类库开发者，用于开发和发布前验证；测试不安装到 `sdk/libraries/`。当前阶段不规定或维护扩展库用户示例。

窗口用于界面，对象用于组织实例或静态成员，接口声明约定，服务用于 Android 服务。各类单元的声明和属性区格式见 [程序单元](Simple语言定义.md#程序单元)。

限定名随单元在源码根目录下的路径变化。ES4A 的单元、包自动重构默认启用：单元或源码文件夹改名、同项目移动时，更新可确认的关联引用及受影响的 `main`；单元改名还更新自身生命周期事件的对象名，窗口改名更新根组件名称。自动重构不猜测普通字符串或动态名称，操作后检查修改并保存，再重新编译。

`es4a.automaticSync.unit` 控制单个单元，`es4a.automaticSync.package` 控制整个源码文件夹；关闭对应设置后，名称或路径变化不自动重构关联引用。拖拽单元移动只支持同一项目。

“移除项目”只从列表移出；“删除项目”会删除项目目录。生成的 `build` 内容不作为源码维护。

### SDK 模板

项目和单元创建使用 SDK 中的固定模板：

| 标识 | 模板路径 |
| --- | --- |
| `project` | `templates/project.properties` |
| `main` | `templates/主窗口.simple` |
| `window` | `templates/窗口.simple` |
| `object` | `templates/对象.simple` |
| `interface` | `templates/接口.simple` |
| `service` | `templates/服务.simple` |

新建项目使用 `project` 和 `main`；新建单元按类型选用模板。缺少模板时对应创建操作不可用。模板变量取自文件中的 `{$变量名}`，同名占位符可以重复使用，创建时必须提供所有变量的值，无需额外变量声明。

Simple 单元模板先解析为 XML 属性模型，再由统一文件保存边界生成单元，不直接拼接属性代码；格式见 [属性区](Simple语言定义.md#属性区)。

## 窗口与事件

窗口单元包含用户代码和末尾的属性区。ES4A 设计器负责组件、属性、布局和事件入口；代码编辑器负责事件体与其他用户代码。

容器控制其子组件排列。窗口和面板可选择布局；滚动框只能直接放一个可视子组件，需要多个组件时先放入面板。扩展容器在设计器中使用清单提供的投影，实际设备行为以该组件实现为准。

尺寸可以使用 `长度_适应内容`、`长度_匹配父级` 或固定数值。其他属性表达式及属性区规则见 [属性区](Simple语言定义.md#属性区)。代码和设计器分别保存，编译前保存两边的修改。

## 资源与资产

- Android 资源放在 `res` 的对应子目录，例如 `res/drawable/icon.png`。编译后使用生成的 `R` 成员，如 `R.drawable_icon`。
- 应用图标使用资源路径表达式，例如 `icon=@drawable/icon`。
- `assets` 的文件原样进入 APK。具体 API 接收的名称或路径按该成员说明填写，不把资产名称当作 Android 资源索引。

在 ES4A 中可导入资源，并通过项目“编辑属性”选择已导入的图标、应用名称、版本、方向和主题。

## 引用已有类库

在 ES4A 类库树中查阅当前 SDK 的类型、属性、函数、事件及参数。需要完整定义时打开对应 `library.json`；运行库成员见 [SimpleAndroidRuntime.json](../simple/SimpleAndroidRuntime.json)。

普通对象可以使用当前作用域可见的短名称、相对限定名、完整限定名或别名。以下示例使用 `com.example.demo.演示对象`，需要所选 SDK 已安装该演示类库的 `classes.jar` 并注册清单；SDK 未附带该库时，使用已安装类库的实际类型与 API：

```simple
别名 演示对象 = com.example.demo.演示对象

事件 主窗口.初始化()
	变量 工具 为 演示对象
	工具 = 创建 演示对象
	显示提示框(工具.加前缀("你好"))
	显示提示框("长度：" & 演示对象.取长度("你好"))
结束 事件
```

实例函数通过对象调用，静态函数通过类型调用。扩展库的静态函数不会自动变成全局函数。组件通过设计器添加，或按语言定义的属性区格式声明，再编写其事件处理过程。

扩展库的依赖、配置、权限与限制见该库 README，公开成员的参数与行为以注解和版本化清单为准。类库要求清单宏时，在项目属性中按“对象简名.宏名”设置，例如 `示例对象.应用标识=my-app-id`。声明 Android 权限和运行时请求权限是不同步骤。

## 编译与运行

在 ES4A 项目树上选择“编译应用”。连接已授权的 Android 设备后选择“调试应用”，执行编译、ADB 安装与启动。当前入口不提供断点、单步或变量查看。

也可从 SDK 根目录在 PowerShell 中执行：

```powershell
& .\utils\project\project.ps1 compile "D:\Projects\HelloSimple\project.properties"
& .\utils\project\project.ps1 debug "D:\Projects\HelloSimple\project.properties"
```

编译器从扩展库 `classes.jar` 的注解识别项目引用，再由 SDK 随附 Gradle 统一解析这些库交付的 `dependencies.module`；作者提供的本地 JAR/AAR 随库原样携带。应用构建不读取扩展库开发目录，下载制品的路径由解析器返回，清单、资源及各包最终 R 类继续由编译器构建。

依赖声明、SDK 工具路径及 `simple/gradle/` 解析脚本不变，且解析制品存在、内容校验通过时复用项目构建缓存，不再次启动 Gradle。首次准备可能联网；可在 `project.ps1 compile` 后指定 `-Offline` 使用缓存，或指定 `-RefreshDependencies` 重新检查仓库，两者互斥。

依赖准备时的 Gradle 诊断完整保存到构建目录的 `dependencies/resolution/gradle.log`。终端显示准备摘要，有警告时提示诊断日志，失败时回显详情；缓存命中时不启动 Gradle，也不重写已有日志。

AAR 清单需要自定义占位符时，在 `project.properties` 使用 `manifest.placeholder.<名称>=<值>`；`${applicationId}` 自动取应用包名，不能用该配置覆盖。依赖要求的主题及其它运行条件仍以类库说明为准。

两条命令都只传入 `project.properties` 的绝对路径。扩展通过系统 PowerShell 调用同一入口，由脚本负责工具定位、环境和执行流程。`debug` 会先编译，再安装并启动项目构建目录内的 `apks/signed.apk`。构建目录由编译器读取项目配置，支持自定义 `build`；不接受外部 APK 路径。

编译不需要连接手机。安装启动需要设备启用 USB 调试并接受电脑授权；当前脚本没有设备选择参数，运行前只连接本次使用的设备。

默认使用调试签名。发布签名使用项目的 `key.location`、`key.alias` 与进程环境变量 `KEY_PASSWORD`；`key.password` 不生效。未同时提供密钥库位置和密码时仍使用调试签名；已选择发布签名但证书无效或签名失败时中断构建。正式发布前检查 APK 的实际签名。

## 排查编译与运行问题

| 情况 | 先检查 |
| --- | --- |
| 工具无法启动 | JDK、Android 工具及两个 Simple JAR 是否位于 SDK 要求的位置 |
| 主窗口找不到 | `main` 是否与源码根目录下的路径及文件名一致 |
| 类型或成员找不到 | 对应类库是否存在、限定名与参数是否正确，实际 `classes.jar` 是否包含该实现 |
| 类库树与编译结果不同 | 是否选中了正确 SDK，清单与 JAR 是否为同一版本；更新后刷新类库并重新编译 |
| 资源编译失败 | 资源名称、目录、引用与终端第一条正式错误 |
| 安装或启动失败 | ADB 设备连接与授权、实际构建目录下的 `apks/signed.apk` 和脚本输出 |

语言错误以编译器报告的文件和行号为准。具体组件行为结合本 SDK 的清单、公开说明与设备运行结果核对。
