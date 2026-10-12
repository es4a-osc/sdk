# SDK 开发文档

文档类型：阅读导航。核对日期：2026-10-12。适用范围：当前 SDK、中文 Simple 与 ES4A 扩展。依据：随附清单、模板、能力脚本、演示工程，并与当前编译器、运行库和扩展源码核对。

这些文档随 SDK 提供，AI 和用户可以直接在本地阅读。按任务选择文档，不必先通读所有内容，也不需要访问内部开发知识库或源码工作区。

| 要做什么 | 阅读文档 |
| --- | --- |
| 查语法、类型、声明、表达式、事件或属性区 | [Simple 语言定义](Simple%20语言定义.md) |
| 开发应用，组织单元、资源，调用已有类库，编译运行 | [Simple 项目开发](Simple%20项目开发.md) |
| 用 Java 编写扩展对象或组件，从工程准备到构建交付 | [Simple 类库开发](Simple%20类库开发.md) |
| 用 JSON 定义分类、成员、图标、属性编辑器或设计器投影 | [类库清单定义](类库清单定义.md) |
| 注册类库，配置编译器、运行库、能力或模板 | [SDK 清单定义](SDK%20清单定义.md) |

项目开发只需按需阅读前两篇。Java 类库开发指南包含基本清单与注册流程；清单定义参考包含字段、示例与检查步骤，两篇均可按各自任务独立阅读，延伸链接用于查更详细的主题。类库包括运行库和扩展库，开发指南的工程构建流程针对扩展库。具体 API 以对应清单和使用说明为入口，样例及可选源码供进一步查阅，不另维护一份成员全集。

使用 SDK 开发 Simple 项目时，使用已编译的类库即可，不要求各库含 Java `src/`，也不需要重建类库。只有开发类库才需要 Java 源码工程，工程可以放在 SDK 之外；构建后再将交付文件安装到所用 SDK。

## 开发前核对

- 先确认正在使用的 SDK 版本和 `sdk.json`。文档示例中的路径以各篇说明的运行目录为准。
- 中文 Simple 的规则见语言定义，不按 Visual Basic 或其他 BASIC 方言推断语法。
- `SimpleCompiler.json`、`SimpleAndroidRuntime.json` 和 `library.json` 描述语言提示与设计器元数据；编译和运行使用实际 JAR。清单加载成功不等于代码已经编译或在设备验证。
- 开发扩展类库时，同时核对 Java 公开注解与 `library.json`，重新生成并安装 `classes.jar`，再使用真实 Simple 样例验证；这不是应用开发的前置步骤。
- 实际编译错误以编译器输出为准；设备能力、权限和生命周期以对应运行库或类库版本的实际行为为准。

## 随附参考

| 内容 | 本地入口 |
| --- | --- |
| SDK 注册、能力与模板 | [sdk.json](../sdk.json) |
| 编译器语言清单 | [SimpleCompiler.json](../simple/SimpleCompiler.json) |
| Android 运行库清单 | [SimpleAndroidRuntime.json](../simple/SimpleAndroidRuntime.json) |
| 项目模板 | [project.properties](../templates/project.properties) |

类库源码与 `sample/` 是可选内容；SDK 提供样例时可直接添加其中的 `project.properties`。文档内的基本示例不要求另行打开演示源码。
