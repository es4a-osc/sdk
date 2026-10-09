# ES4A SDK

SDK 提供 Simple 编译器、Android 运行库、扩展类库、项目模板和编译运行工具。根入口为 `sdk.json`。

## 工具准备

JDK 1.8.0_503 和 Apache Ant 1.9.15 未随仓库提供，请通过[ES4A 下载入口](https://dwz.wsd.cx/es4a-xz)准备。该入口的下载内容尚未核实，解压后检查实际工具文件，不保证入口当前包含哪些包。

| 工具 | 用途 | 解压目录 |
| --- | --- | --- |
| JDK 1.8.0_503 | 运行编译器，以及用 javac、jar 构建 Java 类库 | `sdk/tools/jdk1.8.0_503/`，目录内直接有 `bin/java.exe`、`javac.exe`、`jar.exe` |
| Apache Ant 1.9.15 | 源码构建工具；普通应用与当前公共类库批处理不直接调用 Ant | `sdk/tools/apache-ant-1.9.15/`，目录内直接有 `bin/ant.bat` |

## 基本使用

在 ES4A“类库”标题栏点击“选择 SDK”，选本目录 `sdk.json`；更新 SDK 后点击“刷新类库”。应用项目通过“编译应用”和“调试应用”调用 SDK 工具链。未配置或路径失效时，ES4A 不自动寻找 SDK。

`simple/` 保存核心 JAR 与清单，`libraries/` 保存扩展类库，`templates/` 保存模板，`capabilities/` 保存能力脚本，`tools/` 保存工具。

类库公共构建入口为 `libraries/build.bat`，样例入口为各库的 `sample/project.properties`。

## 按目的阅读

- 应用开发：[环境准备与 SDK 使用](https://es4a.paike.it/#/project/environment)、[快速入门](https://es4a.paike.it/#/project/quickstart)、[扩展库](https://es4a.paike.it/#/project/extensions)。
- 类库开发：[Java 实现](https://es4a.paike.it/#/library/java)、[构建与交付](https://es4a.paike.it/#/library/build)。
- 接入与配置：[清单定义参考](https://es4a.paike.it/#/library/manifests)、[SDK 配置参考](https://es4a.paike.it/#/library/sdk)。

详细规则统一在在线知识库维护。
