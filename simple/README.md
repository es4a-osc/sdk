# Simple 编译器、运行库与通用清单

> SDK 清单参考｜核对日期：2026-10-08｜依据：当前清单、SDK 加载与属性消费代码｜适用范围：编译器、运行库及扩展类库清单维护

本目录保存核心 JAR 和 IDE 清单。下述字段及属性规则由三类清单共用；SDK 入口、注册和路径规则见 [SDK 说明](../README.md)，扩展实现与构建见[类库开发](../libraries/README.md)。

## 核心交付物

| 文件 | 职责 |
| --- | --- |
| [`SimpleCompiler.jar`](SimpleCompiler.jar) | Simple 编译器。 |
| [`SimpleCompiler.json`](SimpleCompiler.json) | 关键字、语法和语言定义，`kind: "compiler"`。 |
| [`SimpleAndroidRuntime.jar`](SimpleAndroidRuntime.jar) | Android 运行库。 |
| [`SimpleAndroidRuntime.json`](SimpleAndroidRuntime.json) | 类型、组件和成员元数据，`kind: "runtime"`。 |
| `icons/` | 清单引用的 SVG 图标。 |

JAR 决定实际编译与运行行为，JSON 提供 IDE 元数据。修改运行行为须重新构建对应 JAR。

## 通用清单结构

```json
{
	"name": "运行库",
	"kind": "runtime",
	"categories": [
		{
			"name": "视图组件",
			"definitions": []
		}
	]
}
```

| 字段 | 要求与用途 |
| --- | --- |
| `name` | 必需，清单显示名称。 |
| `kind` | 必需，分别为 `compiler`、`runtime` 或 `library`。 |
| `categories` | 必需，分类数组。 |
| `description`、`version` | 可选，说明与版本。 |
| `authors` | 可选作者数组；每项 `name` 必需，`email` 可选。 |

## 分类与定义

分类的 `name` 和 `definitions` 数组必需，`description` 可选。`hidden: true` 只隐藏类库树中的分类，其中定义仍参与语言功能和设计器候选；定义级 `hidden` 被忽略。

分类和定义按各自数组顺序显示。设计器只合并含可实例化组件的同名分类，保留首次出现的位置；后续组件按清单声明顺序追加，并按名称去重。窗口根组件不作为可拖入的子组件。

| 定义字段 | 用途 |
| --- | --- |
| `name` | 必需，Simple 定义名称。 |
| `description`、`icon` | 说明与 SVG 图标；图标路径相对于所属清单。 |
| `kind` | 定义用途，如 `component`、`component.window`、`object`、`interface`、`layout` 或语言类别。 |
| `type` | 对应 Java 类或接口的完整运行时类型名。 |
| `inherits` | 直接父类型标识数组。 |
| `constants`、`variables`、`properties`、`functions`、`events` | 各类成员数组。 |
| `hover` | 设为 `true` 时允许通常被忽略的标点定义显示悬停说明。 |

组件 SVG 采用 `24×24`、`viewBox="0 0 24 24"`、`currentColor` 和 `2px` 描边，与现有图标风格一致。

## 成员与参数

| 成员字段 | 用途 |
| --- | --- |
| `name` | 必需，成员名称。 |
| `description`、`type`、`value` | 说明、数据类型与常量值。 |
| `global` | 设为 `true` 时作为全局常量、变量或函数候选。 |
| `static` | 设为 `true` 时，IDE 将函数或变量识别为类型成员；须与 Java 的 `static` 声明一致。 |
| `params`、`return` | 函数或事件的有序参数数组、函数返回类型。 |
| `initializer`、`writable` | 缺省表达式；`writable: false` 表示属性只读。 |
| `editor`、`select` | 属性编辑器与候选值。 |
| `group`、`layouts`、`projection` | 自定义分组、适用布局与设计器投影。 |

参数的 `name` 必需，`type`、`description` 可选；`byRef: true` 表示传址，缺省为传值。自定义元数据只有被消费代码明确支持时才产生 IDE 行为。

## 缺省值与候选值

```json
{
	"name": "宽度",
	"type": "变体型",
	"editor": "simple.pixel",
	"initializer": {
		"label": "匹配父级",
		"value": "长度_匹配父级"
	},
	"select": {
		"input": true,
		"options": [
			{
				"label": "适应内容",
				"value": "长度_适应内容"
			},
			{
				"label": "匹配父级",
				"value": "长度_匹配父级"
			}
		]
	}
}
```

- `initializer` 和 `select.options` 项均支持非空字符串或 `{label, value}` 对象；字符串表示显示文本与表达式相同，`label` 只用于显示，`value` 为实际表达式。
- `initializer` 不自动写入 XML。清除显式赋值后显示缺省值；未声明缺省值时保持未设置。
- `select.options` 必须为数组。`select.input` 缺省为 `false`，仅可选择候选项；设为 `true` 时允许自定义输入，由 `editor` 校验。
- 已有值未匹配候选项时保留原值，不自动改写。

## 分组与适用布局

```json
{
	"name": "位于左边",
	"type": "整数型",
	"editor": "simple.anchor",
	"layouts": [
		"布局_相对"
	],
	"group": "相对组件"
}
```

- `group` 可独立声明，属性框显示“父级分组 / 自定义分组”。
- `layouts` 为非空字符串数组，引用 SDK 布局常量；属性只在直接父容器布局匹配时显示并可编辑。省略时不受该限制，窗口根没有直接父容器。
- 父容器 XML 显式布局值优先，未赋值时使用其布局属性的 `initializer`；不适用的已有 XML 赋值仍原样保留。

## 设计器投影

`projection` 将组件属性映射为设计器语义，不依赖中文属性名。读取 XML 显式值或 SDK 缺省表达式，继承属性同样有效；同一投影以最具体的属性定义为准。省略或未知投影不参与画布投影，属性仍可编辑。

标识区分大小写；这里只控制低保真显示与画布交互，实际 Android 行为由运行库决定。

| 用途 | `projection` 值 |
| --- | --- |
| 标识、尺寸和绝对位置 | `id`、`width`、`height`、`x`、`y` |
| 外边距 | `leftMargin`、`topMargin`、`rightMargin`、`bottomMargin` |
| 内边距 | `paddingLeft`、`paddingTop`、`paddingRight`、`paddingBottom` |
| 文本与外观 | `text`、`hint`、`textColor`、`hintTextColor`、`textSize`、`fontFamily`、`fontBold`、`fontItalic`、`singleLine`、`backgroundColor`、`backgroundImage` |
| 布局与内容对齐 | `layout`、`orientation`、`gravity`、`layoutGravity`、`baselineAligned`、`weight`、`weightSum`、`scrollable`、`scrollbarEnabled` |
| 表格布局 | `rowCount`、`columnCount`、`row`、`column`、`shrinkAllColumns`、`stretchAllColumns` |
| 相对布局同级关系 | `leftOf`、`above`、`rightOf`、`below`、`alignBaseline`、`alignLeft`、`alignTop`、`alignRight`、`alignBottom`、`startOf`、`endOf`、`alignStart`、`alignEnd` |
| 相对布局父级关系 | `alignParentLeft`、`alignParentTop`、`alignParentRight`、`alignParentBottom`、`alignParentStart`、`alignParentEnd`、`centerInParent`、`centerHorizontal`、`centerVertical` |

`gravity` 表示组件内部文本或子内容对齐；`layoutGravity` 表示组件自身在父容器中的对齐。

## 属性编辑器

| 标识 | 用途 |
| --- | --- |
| `simple.boolean` | 逻辑值选择，逻辑型的缺省编辑方式。 |
| `simple.integer` | 整数输入，整数类类型的缺省编辑器。 |
| `simple.color` | 颜色表达式、预览与选择。 |
| `simple.pixel` | 尺寸与边距，支持整数及 `px`、`dp`、`dip`、`sp` 像素文本。 |
| `simple.layout` | 容器布局选择与适用属性过滤。 |
| `simple.anchor` | 选择直接可视兄弟，写入 `组件名.标识`，排除自身。 |
| `simple.single`、`simple.string`、`simple.asset` | 使用普通输入框。 |

组件改名同步更新 XML 兄弟锚点引用；删除或移出同级容器时清理失效锚点，移动子树内部仍有效的锚点保留。

## 属性输入

属性值使用 Simple 表达式。直接值按类型校验，正式合法性以编译器为准：

- 像素整数按数值保存，`30dp` 等有效单位文本按字符串表达式保存；`30dx` 等错误单位拒绝提交。
- 整数和浮点类型校验数值字面量；普通文本输入转换为字符串字面量，已有字符串或表达式保持原意；变体型按输入外形处理。
- 单独符号、调用或资源引用由当前 SDK 与项目语义索引核对；含运算符的组合表达式不在属性框递归验证。

## 维护检查

1. JSON 有效，清单类别正确，分类、定义、成员和参数名称非空。
2. 完整类型、继承、布局常量和投影语义与实际实现一致，图标路径存在。
3. 在 ES4A 中刷新类库，用真实项目验证语言功能、属性框和设计器；核心运行行为变更同时验证重建后的 JAR。

