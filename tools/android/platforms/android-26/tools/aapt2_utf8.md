# AAPT2 UTF-8 副本

工具说明，核对日期：2026-10-20。依据：生成脚本、Windows 进程代码页机制及本机资源编译验证。适用于 Windows 版 AAPT2。

## 文件

- `aapt2.exe`：原版工具，保持不动。
- `aapt2_utf8.bat`：独立的一键生成脚本，使用系统自带 PowerShell，无需其它脚本或额外安装工具。
- `aapt2_utf8.exe`：生成的副本，用于处理中文路径。

## 生成

将 `aapt2_utf8.bat` 与原版 `aapt2.exe` 放在同一目录，双击 BAT 即可生成。

脚本复制原版，向副本写入 UTF-8 进程声明，检查能够启动后生成或替换 `aapt2_utf8.exe`。更换原版后，重新运行脚本即可生成对应版本的副本；脚本不绑定 Build Tools 版本。

原版不会被修改。编译器和调试脚本优先使用已存在的 `aapt2_utf8.exe`，不存在时使用原版 `aapt2.exe`；工具执行失败时直接报告错误，不自动回退重试。生成失败时显示错误，已有副本在资源修改或启动检查失败时保留。

## 修改方式

通过 Windows 应用清单设置副本的进程代码页：

```xml
<activeCodePage xmlns="http://schemas.microsoft.com/SMI/2019/WindowsSettings">UTF-8</activeCodePage>
```

不修改程序代码或系统代码页。此机制需要 Windows 10 1903 及以上，参见[微软说明](https://learn.microsoft.com/zh-cn/windows/apps/design/globalizing/use-utf8-code-page)。

## 验证范围

已验证首次与重复生成、中文及空格路径、资源编译与链接、资源索引及包名读取。未进行完整 APK 设备测试或其它 Windows 版本验证；更换原版后的实际兼容性需重新验证。
