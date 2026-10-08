# 入门演示类库

> 当前示例｜核对日期：2026-10-08｜依据：Java 源码、类库清单和 Simple 示例编译｜适用范围：Simple 类库入门

本例只包含一个按钮组件和一个普通对象。`com.example.demo` 是示例包名，仿写时替换为自己的命名空间。

## 从哪里读起

| 文件 | 内容 |
| --- | --- |
| [演示按钮.java](src/com/example/demo/演示按钮.java) | 用注解声明组件、标题属性和单击事件。 |
| [演示按钮Impl.java](src/com/example/demo/演示按钮Impl.java) | 创建 Android 按钮，将实际单击分派为 Simple 事件。 |
| [演示对象.java](src/com/example/demo/演示对象.java) | 用最小文本操作演示实例函数和静态函数。 |
| [library.json](library.json) | 为 ES4A 提供类型、成员、图标和设计器投影。 |
| [主窗口.simple](sample/src/com/example/demo/sample/主窗口.simple) | 同时演示完整限定名、别名、属性读取和两种函数调用。 |

## 注解要点

- `@SimpleObject` 公开类型；`@SimpleComponent` 将按钮接口声明为可创建的组件。
- `@SimpleProperty` 标记同名获取器与设置器；初值写在设置器上，并与清单一致。
- `@SimpleEvent` 声明事件；实现类通过 `EventDispatcher` 分派实际单击。
- `@SimpleFunction` 公开函数；`static` 决定通过类型调用，非静态函数通过实例调用。
- 清单中的 `取长度` 同步声明 `static: true`，让 IDE 按类型成员提供补全；不设为全局函数。
- 按钮实现类直接 `implements 演示按钮`，供编译器配对，不重复公开一个 Simple 类型。

## 构建与验证

在工作区根目录执行：

```bat
sdk\libraries\build.bat "sdk\libraries\com.example.demo"
sdk\capabilities\compile.bat "sdk\libraries\com.example.demo\sample\project.properties"
```

第一个命令生成 `classes.jar`；第二个命令生成示例 APK。打开示例项目，两个按钮分别调用实例函数 `加前缀` 和静态函数 `取长度`。按系统返回键退出应用。

详细接入规则见[类库开发说明](../README.md)。本次已验证构建、APK 编译与签名及 IDE 模型读取；真实设备点击和设计器可见交互尚未验证。
