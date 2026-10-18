# AAPT2 中文路径兼容说明

本地工具修改说明，核对日期：2026-10-16。依据：二进制对比、实际构建验证及微软进程代码页文档。适用于本目录的 Windows 版 `aapt2.exe`。

## 修改内容

现有 AAPT2 在处理中文目录时，部分文件接口使用系统 ANSI 代码页，与程序内部的 UTF-8 路径不一致，导致资源目录读取失败。

已在 `aapt2.exe` 的 Windows 应用清单资源（`RT_MANIFEST`，编号 `1`）中加入以下声明：

```xml
<activeCodePage xmlns="http://schemas.microsoft.com/SMI/2019/WindowsSettings">UTF-8</activeCodePage>
```

该声明仅让 AAPT2 进程使用 UTF-8，不修改系统编码、编译器或 BAT。程序代码段保持不变，版本仍为 `2:19`；EXE 文件及其哈希已改变，属于本地修改版本，不是官方原始文件。`aapt.exe` 未修改。

此机制要求 Windows 10 1903 及以上；旧版 Windows 不保证生效。参见[微软说明](https://learn.microsoft.com/zh-cn/windows/apps/design/globalizing/use-utf8-code-page)。

## 验证结果

在当前 Windows 11 环境中，中文项目目录、中文 SDK 路径和英文资源路径均通过验证。“计时器”“资源使用”“网页交互”三个示例完整生成 APK，中文资产子目录及文件名正确打包并校验内容。未进行设备运行或其它 Windows 版本验证。

## 原版备份与恢复

`_aapt2.exe` 是修改前的原版备份。需要恢复时，关闭正在构建的任务，用 `_aapt2.exe` 覆盖 `aapt2.exe`；恢复后将重新出现已复现的中文路径问题。

SHA-256：

- 原版 `_aapt2.exe`：`76fcaa1f9cb846c34544539e619e8e03dbf6b3aa23bb015d523b6c21b5c65c6e`
- 修改版 `aapt2.exe`：`c42ab7159cee7bcd0028903f71d26c444e9a9fffd5db6ea385cc5d5fc152a87c`

后续替换或升级 AAPT2 时，需要重新验证中文路径支持，不能假定新版已包含此兼容处理。
