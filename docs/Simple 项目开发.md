# Simple 项目开发

文档类型：应用开发指南。核对日期：2026-10-09。适用范围：使用当前 SDK 和已有类库开发 Android 应用。依据：SDK 模板、能力脚本、演示项目，并核对当前编译器与 ES4A 项目操作源码。

本文说明项目结构、窗口与事件、资源、已有类库调用及编译运行。语言规则按需查阅 [Simple 语言定义](Simple%20语言定义.md)。

## 准备开发环境

准备 Windows、Visual Studio Code、ES4A 扩展及完整 SDK。工具下载与放置位置见 [SDK 说明](../README.md#工具准备)。编译应用需要 JDK、随附 Android 工具链，以及 `simple/SimpleCompiler.jar` 和 `simple/SimpleAndroidRuntime.jar`。

在 ES4A“类库”标题栏选择 SDK 根目录的 `sdk.json`，确认编译器、运行库和需要的扩展库已加载。更新 SDK 后刷新类库；移动 SDK 后重新选择入口。

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
| `name` | 应用显示名称，也是 APK 文件名使用的名称 |
| `version.code`、`version.name` | 整数版本号与显示版本文字 |
| `source` | 源码根目录；多个根目录用逗号分隔 |
| `res`、`assets` | Android 资源目录与原样打包的资产目录 |
| `build` | 编译输出目录 |
| `icon`、`orientation`、`theme` | 应用图标、方向与主题 |

其他属性及缺省值见随附 [项目模板](../templates/project.properties)。`src/com/example/hello/主窗口.simple` 的限定名为 `com.example.hello.主窗口`，不在代码中另写包名声明。

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

已有项目通过“添加项目”选择 `project.properties`。可直接添加 SDK 类库的 `sample/project.properties`，从可运行的示例开始开发。

窗口用于界面，对象用于组织实例或静态成员，接口声明约定，服务用于 Android 服务。各类单元的声明和属性区格式见 [程序单元](Simple%20语言定义.md#程序单元)。

限定名随单元在源码根目录下的路径变化。ES4A 项目树中的文件夹或主窗口单元改名会自动同步受影响的 `main`；窗口改名同步根组件名称，单元改名同步自身已有的加载、初始化事件对象名。其他单元中的引用仍须检查，改名或移动后重新编译。

“移除项目”只从列表移出；“删除项目”会删除项目目录。生成的 `build` 内容不作为源码维护。

## 窗口与事件

窗口单元包含用户代码和末尾的属性区。ES4A 设计器负责组件、属性、布局和事件入口；代码编辑器负责事件体与其他用户代码。

容器控制其子组件排列。窗口和面板可选择布局；滚动框只能直接放一个可视子组件，需要多个组件时先放入面板。扩展容器在设计器中使用清单提供的投影，实际设备行为以该组件实现为准。

尺寸可以使用 `长度_适应内容`、`长度_匹配父级` 或固定数值。其他属性表达式及属性区规则见 [属性区](Simple%20语言定义.md#属性区)。代码和设计器分别保存，编译前保存两边的修改。

## 资源与资产

- Android 资源放在 `res` 的对应子目录，例如 `res/drawable/icon.png`。编译后使用生成的 `R` 成员，如 `R.drawable_icon`。
- 应用图标使用资源路径表达式，例如 `icon=@drawable/icon`。
- `assets` 的文件原样进入 APK。具体 API 接收的名称或路径按该成员说明填写，不把资产名称当作 Android 资源索引。

在 ES4A 中可导入资源，并通过项目“编辑属性”选择已导入的图标、应用名称、版本、方向和主题。

## 引用已有类库

在 ES4A 类库树中查阅当前 SDK 的类型、属性、函数、事件及参数。需要完整定义时打开对应 `library.json`；运行库成员见 [SimpleAndroidRuntime.json](../simple/SimpleAndroidRuntime.json)。

普通对象使用完整限定名或别名。例如随附演示类库：

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

优先阅读目标库的样例；入门示例见 [演示项目](../libraries/com.example.demo/sample/project.properties) 和 [主窗口](../libraries/com.example.demo/sample/src/com/example/demo/sample/主窗口.simple)。类库要求清单宏时，在项目属性中按“对象简名.宏名”设置；声明 Android 权限和运行时请求权限是不同步骤。

## 编译与运行

在 ES4A 项目树上选择“编译应用”。连接已授权的 Android 设备后选择“调试应用”，执行编译、ADB 安装与启动。当前入口不提供断点、单步或变量查看。

也可从 SDK 根目录在命令提示符中执行：

```bat
capabilities\compile.bat "D:\Projects\HelloSimple\project.properties"
capabilities\debug.bat "D:\Projects\HelloSimple\project.properties" "D:\Projects\HelloSimple\build\deploy\你好 Simple.apk"
```

第二条命令会先编译，再安装并启动指定 APK。APK 默认位于项目构建目录的 `deploy/<应用名称>.apk`。项目修改了构建目录或名称时，对应调整路径。

编译不需要连接手机。安装启动需要设备启用 USB 调试并接受电脑授权；当前脚本没有设备选择参数，运行前只连接本次使用的设备。

默认使用调试签名。发布签名使用项目的 `key.location`、`key.alias` 与能力进程环境变量 `KEY_PASSWORD`；`key.password` 不生效。正式发布前检查 APK 的实际签名。

## 排查编译与运行问题

| 情况 | 先检查 |
| --- | --- |
| 工具无法启动 | JDK、Android 工具及两个 Simple JAR 是否位于 SDK 要求的位置 |
| 主窗口找不到 | `main` 是否与源码根目录下的路径及文件名一致 |
| 类型或成员找不到 | 对应类库是否存在、限定名与参数是否正确，实际 `classes.jar` 是否包含该实现 |
| 类库树与编译结果不同 | 是否选中了正确 SDK，清单与 JAR 是否为同一版本；更新后刷新类库并重新编译 |
| 资源编译失败 | 资源名称、目录、引用与终端第一条正式错误 |
| 安装或启动失败 | ADB 设备连接与授权、指定 APK 路径和脚本输出 |

语言错误以编译器报告的文件和行号为准。具体组件行为结合本 SDK 的清单、样例与设备运行结果核对。
