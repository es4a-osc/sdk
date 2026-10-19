# SDK 开发文档

文档类型：阅读导航。核对日期：2026-10-19。适用范围：当前 SDK 与 ES4A。依据：SDK 清单、模板、能力脚本及配套源码。

按任务选择以下文档，使用时无需访问源码工作区或内部开发资料。

| 要做什么 | 阅读文档 |
| --- | --- |
| 查语法、类型、声明、表达式、事件或属性区 | [Simple 语言定义](Simple%20语言定义.md) |
| 开发应用，组织单元、资源，调用已有类库，编译运行 | [Simple 项目开发](Simple%20项目开发.md) |
| 用 Java 编写扩展对象或组件，从工程准备到构建交付 | [Simple 类库开发](Simple%20类库开发.md) |
| 用 JSON 定义分类、成员、图标、属性编辑器或设计器投影 | [类库清单定义](类库清单定义.md) |
| 注册类库，配置编译器、运行库、能力或模板 | [SDK 清单定义](SDK%20清单定义.md) |

应用开发从《Simple 项目开发》开始，语言规则查《Simple 语言定义》。类库开发指南说明 Java 实现与交付，两篇清单参考分别说明类库元数据和 SDK 入口字段。

运行库和扩展库的成员说明可在 ES4A 类库树及配套清单中查阅，用法见对应示例。扩展库的依赖、配置、权限与限制见各库 README。

## 开发前核对

- 先确认正在使用的 SDK 版本及入口清单（`sdk.json` 或本地 `sdk.local.json`）。文档示例中的路径以各篇说明的运行目录为准。
- 中文 Simple 的规则见语言定义，不按 Visual Basic 或其他 BASIC 方言推断语法。
- `SimpleCompiler.json`、`SimpleAndroidRuntime.json` 和 `library.json` 描述语言提示与设计器元数据；编译和运行使用实际 JAR。清单加载成功不等于代码已经编译或在设备验证。
- 开发应用使用已编译类库即可；只有修改类库实现时才需要 Java 源码和重新构建。
- 实际编译错误以编译器输出为准；设备能力、权限和生命周期以对应运行库或类库版本的实际行为为准。

## 随附参考

| 内容 | 本地入口 |
| --- | --- |
| SDK 注册、能力与模板 | [sdk.json](../sdk.json) |
| 编译器语言清单 | [SimpleCompiler.json](../simple/SimpleCompiler.json) |
| Android 运行库清单 | [SimpleAndroidRuntime.json](../simple/SimpleAndroidRuntime.json) |
| 项目模板 | [project.properties](../templates/project.properties) |

扩展类库与演示库在 [类库仓库](https://gitee.com/es4a/libraries) 维护，按需安装并注册到所用 SDK。打开已有示例时，在 ES4A 中添加其 `project.properties`。
