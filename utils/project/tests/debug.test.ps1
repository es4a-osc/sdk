<#
验证设备选择、显式目标失败及安装启动共用目标；模拟 ADB，不操作真实设备。
xhwsd@qq.com 2026-11-3
#>
$ErrorActionPreference = 'Stop'
$temporaryRoot = [IO.Path]::GetFullPath([IO.Path]::GetTempPath())
$testRoot = Join-Path $temporaryRoot ('es4a-deployment-' + [Guid]::NewGuid().ToString('N'))
$previousSerial = $env:ANDROID_SERIAL
$global:es4aDeploymentTest = @{ calls = [Collections.Generic.List[object]]::new() }

function global:Invoke-Es4aDeploymentTestAdb {
	<#
	记录完整参数，并模拟设备列表在安装后发生变化的情形。
	#>
	$arguments = @($args)
	$global:es4aDeploymentTest.calls.Add($arguments)
	$global:LASTEXITCODE = 0
	if ($arguments[0] -eq 'devices') {
		if ($global:es4aDeploymentTest.listError) {
			$global:LASTEXITCODE = 9
			Write-Output 'adb: cannot connect to daemon'
			return
		}
		Write-Output 'List of devices attached'
		Write-Output $global:es4aDeploymentTest.devices
		return
	}
	if ($arguments[0] -ne '-s') {
		throw '安装与启动必须显式指定目标设备。'
	}
	if ($arguments[2] -eq 'install') {
		$global:es4aDeploymentTest.devices = @('changed-device device')
		Write-Output 'Success'
		return
	}
	if ($arguments[2] -eq 'shell') {
		Write-Output 'Status: ok'
		return
	}
	throw '未预期的 ADB 调用。'
}

function global:Invoke-Es4aDeploymentTestAapt {
	<#
	仅返回部署步骤实际需要的应用包名。
	#>
	$global:LASTEXITCODE = 0
	Write-Output "package: name='simple.test.deployment' versionCode='1'"
}

function Invoke-DeviceCase {
	<#
	从公开部署入口验证选择结果和实际传参，不直接调用内部选择函数。
	#>
	param ([string]$Name, [string[]]$Devices, [string]$Requested, [string]$Expected,
		[string]$ExpectedError, [switch]$ListError)
	$global:es4aDeploymentTest.devices = $Devices
	$global:es4aDeploymentTest.listError = [bool]$ListError
	$global:es4aDeploymentTest.calls.Clear()
	$env:ANDROID_SERIAL = $Requested
	$failure = $null
	try {
		Invoke-ProjectDeployment $script:deploymentContext 6>$null
	} catch {
		$failure = $_.Exception.Message
	}

	$calls = $global:es4aDeploymentTest.calls
	if ($ExpectedError) {
		if (-not $failure -or -not $failure.Contains($ExpectedError)) {
			throw "$Name：预期错误（$ExpectedError），实际为（$failure）。"
		}
		if ($calls.Count -ne 1) {
			throw "$Name：选定设备失败后仍执行了安装或启动。"
		}
	} else {
		if ($failure) {
			throw "$Name：$failure"
		}
		if ($calls.Count -ne 3 -or ($calls[0] -join '|') -ne 'devices|-l' -or
			($calls[1] -join '|') -cne "-s|$Expected|install|-r|$($script:deploymentContext.apk)" -or
			($calls[2] -join '|') -cne "-s|$Expected|shell|am|start|-W|-n|simple.test.deployment/simple.runtime.android.MainActivity") {
			throw "$Name：安装和启动参数不一致，或设备列表被重复读取。"
		}
	}
	Write-Output "通过：$Name"
}

try {
	[IO.Directory]::CreateDirectory($testRoot) | Out-Null
	$apk = Join-Path $testRoot "中文 & '应用'.apk"
	[IO.File]::WriteAllBytes($apk, [byte[]]@())
	$script:deploymentContext = @{ apk = $apk; adb = 'Invoke-Es4aDeploymentTestAdb'; aapt = 'Invoke-Es4aDeploymentTestAapt' }
	Import-Module (Join-Path $PSScriptRoot '../modules/debug.psm1') -Force -DisableNameChecking

	Invoke-DeviceCase '优先第一台在线模拟器' @('usb-device device', 'emulator-5556 device', 'emulator-5554 device') '' 'emulator-5556'
	Invoke-DeviceCase '跳过离线和未授权模拟器' @('emulator-5554 offline', 'emulator-5556 unauthorized', 'emulator-5558 device') '' 'emulator-5558'
	Invoke-DeviceCase '单台在线设备' @('usb-device device') '' 'usb-device'
	Invoke-DeviceCase '没有模拟器时选择第一台在线设备' @('usb-device offline', '127.0.0.1:5555 device', 'usb-second device') '' '127.0.0.1:5555'
	Invoke-DeviceCase '显式目标优先并保留序列号大小写' @('emulator-5554 device', 'USB-AbC device') 'USB-AbC' 'USB-AbC'
	Invoke-DeviceCase '显式 TCP 目标' @('127.0.0.1:5555 device', 'emulator-5554 device') '127.0.0.1:5555' '127.0.0.1:5555'
	Invoke-DeviceCase '指定目标不存在时不回退' @('emulator-5554 device') 'missing-device' '' '未找到指定设备'
	Invoke-DeviceCase '指定目标离线时不回退' @('emulator-5554 device', 'usb-device offline') 'usb-device' '' '设备离线'
	Invoke-DeviceCase '指定目标未授权时不回退' @('emulator-5554 device', 'usb-device unauthorized') 'usb-device' '' '设备未授权 USB 调试'
	Invoke-DeviceCase '没有设备' @() '' '' '未找到在线设备'
	Invoke-DeviceCase '设备均不可用' @('emulator-5554 offline', 'usb-device unauthorized') '' '' '未找到在线设备'
	Invoke-DeviceCase '设备列表命令失败' @() '' '' 'ADB 退出码：9' -ListError
} finally {
	$env:ANDROID_SERIAL = $previousSerial
	Remove-Item -LiteralPath 'Function:Invoke-Es4aDeploymentTestAdb', 'Function:Invoke-Es4aDeploymentTestAapt' -ErrorAction SilentlyContinue
	Remove-Variable -Name es4aDeploymentTest -Scope Global -ErrorAction SilentlyContinue
	$resolvedTestRoot = [IO.Path]::GetFullPath($testRoot)
	if ([IO.Path]::GetDirectoryName($resolvedTestRoot).TrimEnd('\', '/') -ne $temporaryRoot.TrimEnd('\', '/')) {
		throw "拒绝清理系统临时目录之外的路径：$resolvedTestRoot"
	}
	if (Test-Path -LiteralPath $resolvedTestRoot) {
		Remove-Item -LiteralPath $resolvedTestRoot -Recurse -Force
	}
}
