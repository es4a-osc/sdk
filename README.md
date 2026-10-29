# ES4A SDK

本仓库面向 Simple 编译器、运行库开发者，维护 SDK 工具、模板和文档。

> 普通用户和扩展库开发者请下载[完整 SDK](https://dwz.wsd.cx/es4a-sdk)。


## 工具准备

| 下载 | 目录 |
| --- | --- |
| [jdk-11.0.32.1](https://dwz.wsd.cx/es4a-jdk) | `tools/jdk-11.0.32.1/` |
| [gradle-8.14.6](https://dwz.wsd.cx/es4a-gradle) | `tools/gradle-8.14.6/` |
| [android-sdk](https://dwz.wsd.cx/es4a-android-sdk) | `tools/android-sdk/` |

> 下载所需工具，并解压至表中对应目录。


## 使用

将 [Simple 仓库](https://gitee.com/es4a/simple)与本仓库同级放置，按其构建说明将编译器、运行库及依赖发布到 `simple/`。随后在 ES4A“类库”视图中选择此 SDK 根目录；扩展库按需安装到 `libraries/`。

- [使用与开发文档](docs/README.md)
- [扩展库开发工具](utils/library/README.md)
- [扩展类库仓库](https://gitee.com/es4a/libraries)


## 相关链接

- [开源仓库](https://gitee.com/es4a)
- [留言反馈](https://dwz.wsd.cx/wsd-ly)
