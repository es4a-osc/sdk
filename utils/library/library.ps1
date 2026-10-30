<#
SDK 扩展库工具入口：使用随 SDK 提供的环境创建、准备和构建类库。
xhwsd@qq.com 2026-10-25
#>
[CmdletBinding()]
param (
	[ValidateSet('help', 'create', 'prepare', 'build')]
	[string]$Command = 'help',
	[string]$Path,
	[string]$Value,
	[string]$Name = '我的扩展库',
	[ValidateSet('exclude', 'include')]
	[string]$Dependencies = 'exclude',
	[ValidateSet('build', 'sdk')]
	[string]$Destination
)

$ErrorActionPreference = 'Stop'
$toolRoot = $PSScriptRoot
$sdkRoot = [IO.Path]::GetFullPath((Join-Path $toolRoot '../..'))

try {
	# 工作区选择以文本选项传递，避免原生进程将布尔字符串误绑定为开关。
	if (($PSBoundParameters.ContainsKey('Dependencies') -or $PSBoundParameters.ContainsKey('Destination')) -and $Command -ne 'build') {
		throw '-Dependencies 和 -Destination 仅用于 build。'
	}
	if ($Destination -eq 'sdk' -and $Value) {
		throw '-Destination sdk 不能同时指定交付根目录。'
	}
	foreach ($module in @('build', 'create', 'prepare')) {
		Import-Module (Join-Path $toolRoot "modules/$module.psm1") -DisableNameChecking
	}
	switch ($Command) {
		'help' {
			Write-Output @'
ES4A SDK 扩展库开发工具
  library.ps1 create "新库目录" "simple.library.author.demo" "显示名称" 创建并准备最小扩展库
  library.ps1 prepare "库目录"                               刷新依赖和 VS Code 工作区
  library.ps1 build "库目录" ["交付根目录"]                    构建完整交付，默认位于 build/<基础包名>/
  library.ps1 build "库目录" -Dependencies include                同时携带离线依赖仓库
  library.ps1 build "库目录" -Dependencies include -Destination sdk 携带离线依赖并交付到 SDK

初始文件统一从 templates/ 生成；工作区在依赖准备后填入本机路径。
仓库依赖在 library.gradle 声明，本地 JAR 放 libs/，本地 AAR 放 aars/。
构建结果可直接复制传播，也可直接输出到 sdk/libraries/<基础包名>/。
'@
		}
		default {
			$config = Get-Configuration
			if ($Command -eq 'create') {
				$libraryRoot = New-Library $Path $Value $Name
			} else {
				$libraryRoot = Get-LibraryDirectory $Path
			}
			$task = if ($Command -eq 'build') { 'buildLibrary' } else { 'prepareIde' }
			$outputRoot = if ($Command -eq 'build' -and $Value) { Get-AbsolutePath $Value } else { $null }
			if ($Destination -eq 'sdk') {
				$outputRoot = Join-Path $config.sdk 'libraries'
			}
			Invoke-LibraryBuild $libraryRoot $config $task $outputRoot -IncludeDependencies:($Dependencies -eq 'include')
			if ($Command -eq 'build') {
				$delivery = [IO.File]::ReadAllText((Join-Path $libraryRoot 'build/gradle/delivery.txt'))
				Write-Output "扩展库构建完成：$delivery"
			} else {
				$workspaceFile = Write-Workspace $libraryRoot $config
				Write-Output "开发工作区：$workspaceFile"
			}
		}
	}
	exit 0
} catch {
	[Console]::Error.WriteLine('扩展库工具：' + $_.Exception.Message)
	exit 1
}
