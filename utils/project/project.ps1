<#
SDK 项目工具入口：编译 Simple 应用，或编译后安装并启动应用。
xhwsd@qq.com 2026-10-26
#>
[CmdletBinding()]
param (
	[ValidateSet('help', 'compile', 'debug')]
	[string]$Command = 'help',
	[string]$ProjectFile,
	[switch]$Offline,
	[switch]$RefreshDependencies
)

$ErrorActionPreference = 'Stop'
$originalEncoding = [Console]::OutputEncoding
$originalOutputEncoding = $OutputEncoding
$exitCode = 0
try {
	# 入口统一日志编码和退出状态；编译与部署分别由内部模块实现。
	[Console]::OutputEncoding = [Text.UTF8Encoding]::new($false)
	$OutputEncoding = [Console]::OutputEncoding
	foreach ($module in @('compile', 'debug')) {
		Import-Module (Join-Path $PSScriptRoot "modules/$module.psm1") -DisableNameChecking
	}
	if ($Command -eq 'help') {
		Write-Host 'ES4A 项目工具'
		Write-Host '  project.ps1 compile "项目文件.properties" [-Offline | -RefreshDependencies]'
		Write-Host '  project.ps1 debug "项目文件.properties" [-Offline | -RefreshDependencies]'
	} else {
		$sdkRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
		$context = Invoke-ProjectCompilation -SdkRoot $sdkRoot -ProjectFile $ProjectFile `
			-Offline:$Offline -RefreshDependencies:$RefreshDependencies -ForDebug:($Command -eq 'debug')
		if ($Command -eq 'debug') {
			Invoke-ProjectDeployment $context
		}
	}
} catch {
	[Console]::Error.WriteLine($_.Exception.Message)
	$exitCode = 1
} finally {
	[Console]::OutputEncoding = $originalEncoding
	$OutputEncoding = $originalOutputEncoding
}
exit $exitCode
