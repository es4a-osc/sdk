# Android SDK 构建目录说明

本文说明 Android SDK 中以下三个目录的职责和依赖关系：

- `build-tools`
- `platforms\android-N`
- `platform-tools`

文中的目录示例以本机 Android SDK 根目录 `D:\AndroidSdk` 为准。

## 目录结构

```text
D:\AndroidSdk\
├─ build-tools\
│  ├─ 26.0.3\
│  ├─ 28.0.3\
│  └─ 36.0.0\
├─ platforms\
│  ├─ android-26\
│  ├─ android-28\
│  └─ android-37.0\
└─ platform-tools\
```

这三个目录是 Android SDK 根目录下相互独立的工具包，不是父子目录，也不要求版本号相同。

## 一句话理解

| 目录 | 解决的问题 |
| --- | --- |
| `platforms\android-N` | 应用按照哪个 Android API 等级编译 |
| `build-tools\版本号` | 使用哪个版本的工具把源码和资源制作成 APK |
| `platform-tools` | 怎样与手机、模拟器和引导加载程序通信 |

## `platforms\android-N`

`platforms` 保存不同 Android API 等级的编译平台。

例如：

```text
D:\AndroidSdk\platforms\android-26
D:\AndroidSdk\platforms\android-28
```

其中最重要的文件是：

| 文件 | 作用 |
| --- | --- |
| `android.jar` | Android API 的编译桩和系统资源；Java/Kotlin 编译器及 AAPT2 链接资源时使用 |
| `android-stubs-src.jar` | Android API 桩源码，主要用于源码查看和开发工具支持 |
| `framework.aidl` | 编译平台提供的基础 AIDL 声明 |
| `source.properties` | 记录该平台的 API 等级和修订版本 |

选择 `platforms\android-26\android.jar`，表示编译期间只能直接使用 API 26 及以下提供的 Android API。它通常对应构建配置中的 `compileSdk = 26`。

`platforms\android-N` 不负责：

- 把资源编译成二进制资源；
- 把字节码转换成 DEX；
- 签名或对齐 APK；
- 连接、安装或调试设备。

这些工作分别由 Build Tools 和 Platform Tools 完成。

## `build-tools`

`build-tools` 保存构建 Android 应用所需的工具。每个版本单独安装，可以同时存在多个版本。

例如：

```text
D:\AndroidSdk\build-tools\26.0.3
D:\AndroidSdk\build-tools\28.0.3
D:\AndroidSdk\build-tools\36.0.0
```

常见文件如下：

| 文件 | 作用 |
| --- | --- |
| `aapt2.exe` | 编译和链接 Android 资源，处理 `AndroidManifest.xml`，生成资源表和资源 APK |
| `aapt.exe` | 旧版资源打包工具，也可读取 APK 的清单和资源信息 |
| `d8.bat` / `d8.jar` | 把 Java 字节码转换为 DEX；现代工具链使用 D8 |
| `dx.bat` / `dx.jar` | 旧版 DEX 转换工具；已经被 D8 取代 |
| `apksigner.bat` | 对 APK 进行签名及签名验证 |
| `zipalign.exe` | 对 APK 中的数据进行对齐优化 |
| `aidl.exe` | 编译 AIDL 接口定义 |
| `source.properties` | 记录 Build Tools 版本 |

Build Tools 的版本不是 Android API 等级：

- Build Tools 28.0.3 不等于 Android API 28；
- Build Tools 28.0.3 可以配合 `platforms\android-26\android.jar` 构建应用；
- 最终允许代码调用哪些 Android API，主要由所选 `android.jar` 决定，而不是由 Build Tools 的目录版本决定。

## `platform-tools`

`platform-tools` 保存与 Android 设备、模拟器和引导加载程序通信的工具。

目录位置：

```text
D:\AndroidSdk\platform-tools
```

常见文件如下：

| 文件 | 作用 |
| --- | --- |
| `adb.exe` | 连接设备、安装 APK、执行设备命令、传输文件和查看日志 |
| `AdbWinApi.dll` | Windows 下 ADB 的运行依赖 |
| `AdbWinUsbApi.dll` | Windows 下 ADB USB 通信的运行依赖 |
| `fastboot.exe` | 在 Fastboot 模式下操作设备和分区 |
| `source.properties` | 记录 Platform Tools 版本 |

Platform Tools 不参与 APK 编译。没有连接设备需求时，只构建 APK 不需要 `adb` 或 `fastboot`。

反过来，已经有 APK 时，使用 `adb install` 安装它也不需要对应版本的 Build Tools 或 `platforms\android-N`。

## 三者的依赖关系

### 构建阶段

```text
应用源码
   │
   ├── Java/Kotlin 编译器
   │       └── 依赖 platforms\android-N\android.jar
   │
   ├── build-tools\版本号\aapt2
   │       └── 链接资源时依赖 platforms\android-N\android.jar
   │
   ├── build-tools\版本号\d8 或 dx
   │       └── 将字节码转换为 classes.dex
   │
   ├── build-tools\版本号\apksigner
   │       └── 签名 APK
   │
   └── build-tools\版本号\zipalign
           └── 对齐 APK
```

构建 APK 时的核心关系是：

```text
一个编译平台 + 一套 Build Tools

platforms\android-N\android.jar
                +
build-tools\具体版本中的构建程序
```

这里是“构建流程依赖”，不是要求二者版本号相同。

### 安装和调试阶段

```text
已经生成的 APK
        │
        └── platform-tools\adb.exe
                    │
                    └── 手机或模拟器
```

Platform Tools 位于构建流程之后。它消费构建结果，但不参与生成 APK。

### 完整关系图

```text
              platforms\android-N
                 android.jar
                    ▲
                    │ 编译 API、链接系统资源
                    │
项目源码和资源 ─── build-tools ─── APK
                                      │
                                      │ 安装、运行、调试
                                      ▼
                               platform-tools
                                      │
                                      ▼
                                手机或模拟器
```

## 版本如何搭配

三者可以独立选择版本：

```text
platforms\android-26
build-tools\28.0.3
platform-tools\37.0.1
```

这组搭配表示：

- 以 API 26 的 `android.jar` 作为编译平台；
- 使用 Build Tools 28.0.3 处理资源、DEX 和 APK；
- 使用 Platform Tools 37.0.1 连接设备。

这种版本号不同的搭配本身没有矛盾。应分别判断：

1. Build Tools 是否支持当前构建参数、JDK 和项目格式；
2. 选择的 `android.jar` 是否包含项目需要使用的 Android API；
3. 应用的 `minSdkVersion` 是否允许在目标低版本系统安装和运行；
4. 使用高版本 API 时，代码是否进行了系统版本判断和兼容处理；
5. Platform Tools 是否能正常识别和连接目标设备。

## 容易混淆的概念

### Build Tools 版本不决定最低系统版本

把 Build Tools 从 26.0.3 升级到 28.0.3，不会自动让应用只能运行在 Android 9。应用能安装到多低的 Android 系统，主要由 `minSdkVersion` 决定。

### 编译平台不等于运行平台

使用 API 28 的 `android.jar` 编译，不代表应用只能在 API 28 上运行。只要 `minSdkVersion` 允许，并且对高版本 API 做好兼容判断，应用仍可在较低版本 Android 系统运行。

### Platform Tools 通常可以单独更新

`adb` 和 `fastboot` 面向设备通信，通常不需要和 `platforms\android-N` 或 Build Tools 保持相同版本。Windows 下更新 ADB 时，应把 `adb.exe`、`AdbWinApi.dll` 和 `AdbWinUsbApi.dll` 作为一组更新。

## 维护建议

- 从 `D:\AndroidSdk\platforms\android-N` 取得平台文件。
- 从 `D:\AndroidSdk\build-tools\具体版本` 取得构建工具，不要根据 `android-N` 猜测 Build Tools 版本。
- 从 `D:\AndroidSdk\platform-tools` 取得 ADB、Fastboot 及其 DLL。
- 不要因为版本号看起来不同，就强行把三者升级或降级成相同数字。
- 替换 Build Tools 后应重新验证资源编译、DEX 生成、APK 签名和安装结果。
- 替换 Platform Tools 后应验证 `adb devices`、APK 安装、日志读取和应用启动。
