@ECHO OFF
REM 一键生成 AAPT2 UTF-8 副本 xhwsd@qq.com 2026-10-20

REM 通过系统 PowerShell 执行本文件中的资源处理逻辑，不依赖其它脚本。
SET "AAPT2_UTF8_BAT=%~f0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "$text = [IO.File]::ReadAllText($env:AAPT2_UTF8_BAT, [Text.Encoding]::GetEncoding(936)); $marker = '# POWERSHELL_BEGIN'; & ([ScriptBlock]::Create($text.Substring($text.LastIndexOf($marker) + $marker.Length)))"
SET "AAPT2_UTF8_BAT="
GOTO :EOF

# POWERSHELL_BEGIN

# 从同目录原版生成 AAPT2 UTF-8 副本
# xhwsd@qq.com 2026-10-20

# 文件位置以 BAT 所在目录为准，不受当前工作目录影响。
$directory = Split-Path -LiteralPath $env:AAPT2_UTF8_BAT
$source = Join-Path $directory 'aapt2.exe'
$target = Join-Path $directory 'aapt2_utf8.exe'
$temporary = Join-Path ([IO.Path]::GetTempPath()) ('aapt2_utf8-' + [Guid]::NewGuid().ToString('N') + '.exe')
$ErrorActionPreference = 'Stop'

try {
	if (-not (Test-Path -LiteralPath $source -PathType Leaf)) {
		throw '未找到同目录的原版 aapt2.exe。'
	}

	# 通过 Windows 资源接口加入清单，不改写程序代码或系统设置。
	if (-not ('Aapt2Utf8Resource' -as [type])) {
		Add-Type -TypeDefinition @'
using System;
using System.Runtime.InteropServices;

public static class Aapt2Utf8Resource
{
	[DllImport("kernel32.dll", CharSet = CharSet.Unicode, SetLastError = true)]
	public static extern IntPtr BeginUpdateResource(string file, bool deleteExistingResources);

	[DllImport("kernel32.dll", CharSet = CharSet.Unicode, SetLastError = true)]
	public static extern bool UpdateResource(IntPtr handle, IntPtr type, IntPtr name,
		ushort language, byte[] data, uint size);

	[DllImport("kernel32.dll", CharSet = CharSet.Unicode, SetLastError = true)]
	public static extern bool EndUpdateResource(IntPtr handle, bool discard);
}
'@
	}

	$manifest = [Text.Encoding]::UTF8.GetBytes(@'
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<assembly xmlns="urn:schemas-microsoft-com:asm.v1" manifestVersion="1.0">
  <application xmlns="urn:schemas-microsoft-com:asm.v3">
    <windowsSettings>
      <activeCodePage xmlns="http://schemas.microsoft.com/SMI/2019/WindowsSettings">UTF-8</activeCodePage>
    </windowsSettings>
  </application>
</assembly>
'@)

	# 只处理原版的临时副本，成功前不覆盖已有的 UTF-8 工具。
	Copy-Item -LiteralPath $source -Destination $temporary
	$handle = [Aapt2Utf8Resource]::BeginUpdateResource($temporary, $false)
	if ($handle -eq [IntPtr]::Zero) {
		throw [ComponentModel.Win32Exception]::new([Runtime.InteropServices.Marshal]::GetLastWin32Error())
	}

	$updated = [Aapt2Utf8Resource]::UpdateResource($handle, [IntPtr]24, [IntPtr]1, 0, $manifest, $manifest.Length)
	if (-not $updated) {
		$failure = [ComponentModel.Win32Exception]::new([Runtime.InteropServices.Marshal]::GetLastWin32Error())
		[Aapt2Utf8Resource]::EndUpdateResource($handle, $true) | Out-Null
		throw $failure
	}
	if (-not [Aapt2Utf8Resource]::EndUpdateResource($handle, $false)) {
		throw [ComponentModel.Win32Exception]::new([Runtime.InteropServices.Marshal]::GetLastWin32Error())
	}

	# 检查副本能够启动，再发布到同目录；重复执行会重新生成副本。
	& $temporary version
	if ($LASTEXITCODE -ne 0) {
		throw 'UTF-8 副本启动失败，未替换已有文件。'
	}
	Move-Item -LiteralPath $temporary -Destination $target -Force
	Write-Output '已生成 aapt2_utf8.exe，原版 aapt2.exe 保持不变。'
} finally {
	# 失败时也清理本次临时副本，不清理其它文件。
	if (Test-Path -LiteralPath $temporary) {
		Remove-Item -LiteralPath $temporary -Force
	}
}
