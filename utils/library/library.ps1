<#
SDK 扩展库工具入口：使用随 SDK 提供的环境创建、准备、编译和发布类库。
xhwsd@qq.com 2026-10-25
#>
[CmdletBinding()]
param (
	[ValidateSet('help', 'create', 'prepare', 'compile', 'publish', 'open')]
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
	if (($PSBoundParameters.ContainsKey('Dependencies') -or $PSBoundParameters.ContainsKey('Destination')) -and $Command -ne 'compile') {
		throw '-Dependencies 和 -Destination 仅用于 compile。'
	}
	if ($Destination -eq 'sdk' -and $Value) {
		throw '-Destination sdk 不能同时指定交付根目录。'
	}
	foreach ($module in @('compile', 'create', 'prepare', 'publish')) {
		Import-Module (Join-Path $toolRoot "modules/$module.psm1") -DisableNameChecking
	}
	switch ($Command) {
		'help' {
			Write-Output @'
ES4A SDK 扩展库开发工具
  library.ps1 create "新库目录" "simple.library.author.demo" "显示名称" 创建并准备最小扩展库
  library.ps1 prepare "库目录"                               刷新依赖和 VS Code 工作区
  library.ps1 compile "库目录" ["交付根目录"]                  编译完整交付，默认位于 build/<基础包名>/
  library.ps1 compile "库目录" -Dependencies include              同时携带离线依赖仓库
  library.ps1 compile "库目录" -Dependencies include -Destination sdk 携带离线依赖并交付到 SDK
  library.ps1 publish "库目录或交付目录"                      将已有交付复制到当前 SDK
  library.ps1 open "库目录"                                  准备后用 VS Code 打开

初始文件统一从 templates/ 生成；工作区在依赖准备后填入本机路径。
仓库依赖在 library.gradle 声明，本地 JAR 放 libs/，本地 AAR 放 aars/。
编译结果可直接复制传播，发布仅安装到 sdk/libraries/<基础包名>/。
'@
		}
		'publish' {
			Publish-Library (Get-AbsolutePath $Path) @{ sdk = $sdkRoot }
		}
		default {
			$config = Get-Configuration
			if ($Command -eq 'create') {
				$libraryRoot = New-Library $Path $Value $Name
			} else {
				$libraryRoot = Get-LibraryDirectory $Path
			}
			$task = if ($Command -eq 'compile') { 'compileLibrary' } else { 'prepareIde' }
			$outputRoot = if ($Command -eq 'compile' -and $Value) { Get-AbsolutePath $Value } else { $null }
			if ($Destination -eq 'sdk') {
				$outputRoot = Join-Path $config.sdk 'libraries'
			}
			Invoke-LibraryBuild $libraryRoot $config $task $outputRoot -IncludeDependencies:($Dependencies -eq 'include')
			if ($Command -eq 'compile') {
				$delivery = [IO.File]::ReadAllText((Join-Path $libraryRoot 'build/gradle/delivery.txt'))
				Write-Output "扩展库编译完成：$delivery"
			} else {
				$workspaceFile = Write-Workspace $libraryRoot $config
				Write-Output "开发工作区：$workspaceFile"
			}
			if ($Command -eq 'open') {
				$code = Get-Command code -ErrorAction SilentlyContinue
				if (-not $code) {
					throw '未找到 VS Code 的 code 命令；请手动打开上面生成的工作区。'
				}
				& $code.Source --new-window $workspaceFile
				if ($LASTEXITCODE -ne 0) {
					throw "VS Code 启动失败：$LASTEXITCODE"
				}
			}
		}
	}
	exit 0
} catch {
	[Console]::Error.WriteLine('扩展库工具：' + $_.Exception.Message)
	exit 1
}
