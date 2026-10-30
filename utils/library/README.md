# 扩展库开发工具

使用当前 SDK 的 JDK、Gradle 和 Android SDK，创建、刷新、编译及发布扩展库。当前支持 Windows，无需另外配置 SDK 路径。工具按 SDK 固定目录定位，AGP 从 Simple 交付的 `simple/libs/` 离线加载，AAPT2 使用 SDK 自带版本。类库业务依赖仍由 `library.gradle` 声明，构建参数及仓库地址在 `gradle/` 工程内维护。

当前 AGP 要求 SDK 安装路径不含中文等非 ASCII 字符；扩展库源码目录支持中文和空格。

```powershell
.\library.ps1 create "D:\我的类库" "simple.library.author.demo" "我的扩展库"
.\library.ps1 prepare "D:\我的类库"
.\library.ps1 compile "D:\我的类库"
.\library.ps1 compile "D:\我的类库" -Dependencies include
.\library.ps1 compile "D:\我的类库" "E:\ES4A\sdk\libraries"
.\library.ps1 publish "D:\我的类库"
```

`library.ps1` 负责参数和命令分派，构建、模板生成、工作区及发布分别由 `modules/` 内对应模块处理。初始文件统一来自 `templates/`，其中 `library.json.template` 提供清单，`library.code-workspace.template` 在依赖准备完成后填入本机路径和依赖模型。

源码可以放在任意位置。创建后双击库根目录的 `<基础包名>.code-workspace`，使用 VS Code 开发；工作区任务提供“刷新依赖”和“构建类库”；构建时依次选择是否携带离线依赖、是否发布到 SDK，默认输出到本库构建目录。携带离线依赖的准备过程仍可联网。工作区是本机生成物，切换设备、移动源码或 SDK 后，重新执行 `prepare`。

库作者维护 `src/`、`library.json`、`library.gradle` 和自有资源。Android 兼容扩展统一采用 AndroidX；公共配置已启用 AndroidX，不默认引入 AppCompat 等具体组件，也不自动转换旧 Support 包。发现 `com.android.support` 模块依赖时构建报错，库作者应改用 AndroidX 版本。

仓库依赖在 `library.gradle` 中使用标准 Gradle 声明，由 Gradle 下载并解析发布元数据中的传递依赖。公共仓库包含 Google Maven、Maven Central 和 JitPack，库作者无需重复配置仓库。例如：

```groovy
dependencies {
	implementation 'androidx.appcompat:appcompat:1.2.0'
}
```

本地 JAR 放在 `libs/` 第一层，本地 AAR 放在 `aars/` 第一层，由工具自动加入。两个目录分别只接受对应格式，放错目录时明确报错。同名 POM 不参与本地包的依赖解析，本地包所需的外部依赖统一在 `library.gradle` 中显式补充；线上包仍由 Gradle 根据发布元数据解析传递依赖。工具不根据文件名、字节码或网络搜索推测依赖，不修改原包，保留下载、归档及编译的实际诊断。

`library.gradle` 中 `files('相对路径')` 等文件路径以扩展库根目录为基准；显式声明的本地文件依赖按 Gradle 依赖作用域纳入 IDE 类路径或交付目录，`compileOnly` 不随库交付，`runtimeOnly` 不加入 Java 编译类路径。

IDE 与正式 Java 编译共用依赖模型。SDK 的 `android.jar`、`SimpleAndroidRuntime.jar` 和本地 JAR 直接引用原路径；线上 JAR、AAR 的编译用 JAR、内嵌 JAR 和所需的资源类统一整理到 `build/ide/libs/`，不复制 SDK 基础依赖。工作区由工具生成 `include`、`exclude` 和 `sources`，不引用个人 Gradle 缓存路径；刷新时清理旧副本及失效输入，无需人工维护。

SDK 中已有的 Android 源码直接关联原位置；本地 JAR 的源码附件放在同目录 `libs/<原包名>-sources.jar`，本地 AAR 的源码附件放在 `aars/sources/<原包名>-sources.jar`；线上可取得的匹配源码随对应编译 JAR 整理到 `build/ide/libs/`，按同目录的 `xxx.jar` / `xxx-sources.jar` 自动关联，仅跨目录等情况生成显式 `sources` 映射。源码不重新打包，也不依赖 SDK 之外的源码仓库。Simple 运行库不提供、不生成也不关联源码包。`prepare` 只刷新依赖和 IDE 模型，不编译本库 Java；`compile` 使用同一依赖模型编译本库，不生成或改写工作区；需要更新代码提示时执行 `prepare`。JDK 11、编译 API 30 和最低 API 24 的要求仍然适用，选择 AndroidX 版本时需核对对应要求。

开发中间文件集中在 `build/`：`gradle/` 保存公共构建配置、缓存及 Android 编译中间产物，`ide/` 保存 VS Code 类路径和编辑器编译输出。该目录无需人工维护，可删除后由工具重新生成。

`compile` 默认生成完整交付目录 `build/<基础包名>/`，可直接复制该文件夹传播。可选第二个参数指定交付根目录，工具自动追加 `android.namespace` 作为子目录名；指定 `sdk/libraries/` 时即直接交付到 SDK。构建失败保留原有交付，编译不覆盖源码清单或开发工作区。

交付目录包含 `classes.jar`、`library.json`、`AndroidManifest.xml`、`R.txt`、Gradle 原生 `dependencies.module` 及实际存在的图标、资源等。namespace 从标准 Android 清单的 `package` 读取；本地运行原包统一放在 `libs/*.jar` 或 `aars/*.aar`，同名不同内容时报错。无需额外的依赖 JSON 索引或 `files/` 目录；默认只交付仓库依赖的原生声明。Java 11、编译 API 30 和最低 API 24 是工具链基线，不由类库另行调整。

需要随库携带离线依赖时，为 `compile` 添加 `-Dependencies include`，可与交付根目录参数同时使用。工具将已选运行依赖的原始 JAR/AAR、POM、原生 `.module` 及必要的父 POM、BOM 保存到交付目录的 `dependencies/<组织路径>/<模块>/<版本>/`，不复制个人 Gradle 缓存、展开结果或源码附件。准备过程可能联网；普通编译不额外导出这份仓库。

APK 编译会联合读取实际所用库的 `dependencies/`、应用 `build/dependencies/repository/` 和 Gradle 缓存，按原生声明统一选版。正常构建可联网补齐缺项并保存到应用仓库；依赖齐全时，项目工具的 `-Offline` 支持空 Gradle 缓存下构建。多个库选出的版本可能不同于单库准备结果，缺少最终版本时仍须补齐；离线缺项或同坐标同文件不同内容明确报错。`publish` 原样携带这份可选仓库，不重新解析依赖。

`publish` 仅复制已有完整交付，不编译或准备 IDE。可以传入交付目录，或传入源码目录以安装其默认 `build/<基础包名>/` 产物。安装时不携带类库实现源码、测试、开发缓存及 `*-sources.jar`，也不维护注册列表；SDK 安装目录中的 `library.json` 即表示注册。新交付格式须配套使用支持标准目录约定的编译器；已安装的旧库需重新编译后再安装。

库根目录下存在 `example/` 时，将其按原目录结构随库交付，排除整个 `example/build/`；没有 `example/` 时照常发布，不生成示例目录。

交付的 Android 清单采用 Gradle 处理后的结果，包含有效包名与最低 API 条件；应用的目标 API 由应用构建维护。

此工具完成类库构建和交付准备；包含 AAR 的交付物仍要求 Simple 应用构建工具具备相应处理能力。
