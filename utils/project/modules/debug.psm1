<#
使用编译器确定的 APK 执行 ADB 安装、包名读取和应用启动。
xhwsd@qq.com 2026-10-28
#>

function Invoke-ProjectDeployment {
	<#
	安装与启动结果转为中文提示，未知失败保留工具原始输出。
	#>
	param ([Collections.IDictionary]$Context)
	$apk = $Context.apk
	$adb = $Context.adb
	$aapt = $Context.aapt
	if (-not (Test-Path -LiteralPath $apk -PathType Leaf)) {
		throw "未找到文件：$apk"
	}
	Write-Host
	Write-Host '[安装应用]'
	# Windows PowerShell 会将重定向的原生命令错误流包装为错误记录，先收集再按退出码判断。
	$previousErrorActionPreference = $ErrorActionPreference
	try {
		$ErrorActionPreference = 'Continue'
		$installOutput = @(& $adb 'install' '-r' $apk 2>&1)
		$installExitCode = $LASTEXITCODE
	} finally {
		$ErrorActionPreference = $previousErrorActionPreference
	}
	# 原始输出仅供结果识别，错误记录取实际消息，避免混入异常类型名。
	$installOutput = @($installOutput | ForEach-Object {
		if ($_ -is [Management.Automation.ErrorRecord]) {
			$_.Exception.Message
		} else {
			[string]$_
		}
	})
	$installText = $installOutput -join "`n"
	if ($installExitCode -ne 0) {
		$installErrorCode = $null
		$unknownInstallError = $false
		$reason = '未知安装错误。'
		if ($installText -match '\b(INSTALL_(?:FAILED|PARSE_FAILED)_[A-Z0-9_]+)\b') {
			$installErrorCode = $Matches[1]
			$reason = switch ($installErrorCode) {
				'INSTALL_FAILED_UPDATE_INCOMPATIBLE' { '签名证书不一致。' }
				'INSTALL_FAILED_INSUFFICIENT_STORAGE' { '设备存储空间不足。' }
				'INSTALL_FAILED_VERSION_DOWNGRADE' { '应用版本号低于已安装版本。' }
				'INSTALL_FAILED_USER_RESTRICTED' { '安装被用户或设备策略阻止。' }
				'INSTALL_FAILED_NO_MATCHING_ABIS' { '原生库与设备架构不兼容。' }
				'INSTALL_FAILED_OLDER_SDK' { '设备 Android 版本低于应用要求。' }
				'INSTALL_FAILED_ALREADY_EXISTS' { '应用已存在。' }
				'INSTALL_FAILED_INVALID_APK' { '安装包无效。' }
				default {
					$unknownInstallError = $true
					'未知安装错误。'
				}
			}
		} elseif ($installText -match 'device unauthorized|unauthorized') {
			$reason = '设备未授权 USB 调试。'
		} elseif ($installText -match 'device offline') {
			$reason = '设备离线。'
		} elseif ($installText -match 'more than one device|more than one emulator') {
			$reason = '存在多个设备或模拟器。'
		} elseif ($installText -match 'no devices/emulators found|no device|device .* not found') {
			$reason = '未找到可用设备。'
		} else {
			$unknownInstallError = $true
		}
		$message = "安装应用失败：$reason"
		if ($installErrorCode) {
			$message += "`n安装错误码：$installErrorCode"
		}
		if ($unknownInstallError -and -not [string]::IsNullOrWhiteSpace($installText)) {
			$message += "`nADB 原始输出：`n$installText"
		}
		throw "$message`nADB 退出码：$installExitCode"
	}
	Write-Host '安装应用成功。'

	# 优先使用已有 UTF-8 AAPT2；执行失败时报告错误，不切换工具重试。
	$badging = @(& $aapt 'dump' 'badging' $apk)
	if ($LASTEXITCODE -ne 0) {
		throw "读取 APK 包名失败，AAPT2 退出码：$LASTEXITCODE"
	}
	$package = $null
	foreach ($line in $badging) {
		if ($line -match "^package: name='([^']+)'") {
			$package = $Matches[1]
			break
		}
	}
	if (-not $package) {
		throw "未能从 APK 取得应用包名：$apk"
	}
	Write-Host
	Write-Host '[启动应用]'
	$previousErrorActionPreference = $ErrorActionPreference
	try {
		$ErrorActionPreference = 'Continue'
		$launch = @(& $adb 'shell' 'am' 'start' '-W' '-n' "$package/simple.runtime.android.MainActivity" 2>&1)
		$launchExitCode = $LASTEXITCODE
	} finally {
		$ErrorActionPreference = $previousErrorActionPreference
	}
	$launch = @($launch | ForEach-Object {
		if ($_ -is [Management.Automation.ErrorRecord]) {
			$_.Exception.Message
		} else {
			[string]$_
		}
	})
	$launchText = $launch -join "`n"
	# am 可能在退出码为 0 时仍报告启动错误，必须同时核对等待结果。
	if ($launchExitCode -ne 0 -or -not ($launch -match '^Status:\s+ok\s*$') -or
		($launch -match '^\s*(Error:|Error type\s+\d+|.*Exception:)')) {
		$unknownLaunchError = $false
		if ($launchText -match 'Error type 3|Activity class .* does not exist|unable to resolve Intent') {
			$reason = '未找到应用启动入口。'
		} elseif ($launchText -match 'Permission Denial|Permission denied|not exported from uid') {
			$reason = '启动权限不足。'
		} elseif ($launchText -match '(?m)^Status:\s*timeout\s*$') {
			$reason = '应用启动超时。'
		} elseif ($launchText -match 'device unauthorized|unauthorized') {
			$reason = '设备未授权 USB 调试。'
		} elseif ($launchText -match 'device offline') {
			$reason = '设备离线。'
		} elseif ($launchText -match 'more than one device|more than one emulator') {
			$reason = '存在多个设备或模拟器。'
		} elseif ($launchText -match 'no devices/emulators found|no device|device .* not found') {
			$reason = '未找到可用设备。'
		} else {
			$reason = '未能确认启动成功。'
			$unknownLaunchError = $true
		}
		$message = "启动应用失败：$reason"
		if ($launchText -match '(?m)^Error type\s+(\d+)\s*$') {
			$message += "`n启动错误码：$($Matches[1])"
		}
		if ($unknownLaunchError -and -not [string]::IsNullOrWhiteSpace($launchText)) {
			$message += "`nADB 原始输出：`n$launchText"
		}
		throw "$message`nADB 退出码：$launchExitCode"
	}
	Write-Host '启动应用成功。'
}

Export-ModuleMember -Function Invoke-ProjectDeployment
