# Android 旧版 APK 组装兼容库

本目录保存 Simple Android 编译链使用的旧版 APK 组装工具。这些文件只在编译期间运行，不会打入生成的应用。

## 文件职责

| 文件 | 职责 | 依赖性质 |
| --- | --- | --- |
| `apkbuilder.jar` | 将资源包、DEX、依赖资源和原生库组装为未签名 APK | 编译器直接依赖 |
| `jarutils.jar` | 提供归档写入、资源过滤和旧式签名辅助能力 | ApkBuilder 运行依赖 |
| `androidprefs.jar` | 定位 Android 用户配置目录和默认调试密钥库 | ApkBuilder 间接兼容依赖 |

### `apkbuilder.jar`

旧版 Android APK 组装工具，负责将以下内容合并为未签名 APK：

- Android 资源工具生成的 `.ap_` 资源包；
- DX 生成的 `classes.dex`；
- 依赖 JAR 中需要随应用发布的非 class 资源；
- 按 ABI 目录组织的原生 `.so` 文件。

APK 的最终签名由后续签名步骤完成。

### `jarutils.jar`

ApkBuilder 使用的归档工具库，主要提供：

- ZIP/JAR 条目写入；
- 依赖资源过滤；
- APK 归档处理；
- 旧式签名及调试密钥辅助类。

即使生成的是未签名 APK，ApkBuilder 仍会使用其中的归档和资源过滤能力。

### `androidprefs.jar`

旧版 Android 工具的配置目录辅助库，用于查找 Android 用户配置目录及默认调试密钥库位置。

当前构建流程不依赖它完成最终签名，但旧版 ApkBuilder 的相关类型仍间接引用该库，因此继续作为兼容依赖保留。

## 构建位置

```text
编译、链接 Android 资源
        ↓
生成 classes.dex
        ↓
ApkBuilder 组装未签名 APK
        ↓
签名工具生成最终 APK
```

本目录中的三个 JAR 参与第三步。

## 历史原因

Google Simple 原有编译链建立在早期 Android SDK 之上，当时 Android SDK 在 `tools/lib` 中提供 ApkBuilder 及其依赖。现代 Android SDK 已经移除这套工具，Simple 为保持既有项目和编译行为兼容而继续随 SDK 提供所需文件。

## 维护约束

- 三个 JAR 构成一组兼容依赖，不应单独删除、替换或移动。
- 它们属于编译器内部工具，不是 Simple 应用开发接口，也不应作为应用类库引用。
- 不应使用来源或版本不明的同名 JAR 覆盖现有文件。
- 只有在 APK 组装流程被完整替换，并验证资源、DEX、依赖资源、原生库和签名结果后，才能移除这组兼容库。
