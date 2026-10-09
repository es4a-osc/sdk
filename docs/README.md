# SDK 开发文档

文档类型：阅读导航。核对日期：2026-10-09。适用范围：当前 SDK、中文 Simple 与 ES4A 扩展。依据：随附清单、模板、能力脚本、演示工程，并与当前编译器、运行库和扩展源码核对。

这些文档随 SDK 提供，AI 和用户可以直接在本地阅读。按任务选择文档，不必先通读所有内容。

| 要做什么 | 阅读文档 |
| --- | --- |
| 查语法、类型、声明、表达式、事件或属性区 | [Simple 语言定义](Simple%20语言定义.md) |
| 开发应用，组织单元、资源，调用已有类库，编译运行 | [Simple 项目开发](Simple%20项目开发.md) |
| 用 Java 编写对象或组件，处理注解、依赖和构建交付 | [Simple 类库开发](Simple%20类库开发.md) |
| 定义分类、成员、属性编辑器或设计器投影 | [类库清单定义](类库清单定义.md) |
| 注册类库，配置编译器、运行库、能力或模板 | [SDK 清单定义](SDK%20清单定义.md) |

项目开发只需按需阅读前两篇；开发类库再阅读 Java 实现与清单参考。类库的具体 API 和使用方法由对应清单、源码及样例提供，不另维护一份成员全集。

## 开发前核对

- 先确认正在使用的 SDK 版本和 `sdk.json`。文档示例中的路径以各篇说明的运行目录为准。
- 中文 Simple 的规则见语言定义，不按 Visual Basic 或其他 BASIC 方言推断语法。
- `SimpleCompiler.json`、`SimpleAndroidRuntime.json` 和 `library.json` 描述语言提示与设计器元数据；编译和运行使用实际 JAR。清单加载成功不等于代码已经编译或在设备验证。
- 修改类库时同时核对 Java 公开注解与 `library.json`，重新生成 `classes.jar`，再使用真实 Simple 样例验证。
- 实际编译错误以编译器输出为准；设备能力、权限和生命周期以对应运行库或类库版本的实际行为为准。

## 随附参考

| 内容 | 本地入口 |
| --- | --- |
| SDK 注册、能力与模板 | [sdk.json](../sdk.json) |
| 编译器语言清单 | [SimpleCompiler.json](../simple/SimpleCompiler.json) |
| Android 运行库清单 | [SimpleAndroidRuntime.json](../simple/SimpleAndroidRuntime.json) |
| 项目模板 | [project.properties](../templates/project.properties) |
| 入门演示类库 | [演示类库](../libraries/com.example.demo/README.md)、[类库清单](../libraries/com.example.demo/library.json) |
| 演示项目 | [project.properties](../libraries/com.example.demo/sample/project.properties) |
