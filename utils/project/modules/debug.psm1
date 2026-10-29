<#
使用编译器确定的 APK 执行 ADB 安装、包名读取和应用启动。
xhwsd@qq.com 2026-10-28
#>

function Invoke-ProjectDeployment {
	<#
	安装后仅在状态成功时继续启动；保留阶段间空行、工具输出及失败中断语义。
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
	& $adb 'install' '-r' $apk
	if ($LASTEXITCODE -ne 0) {
		throw "应用安装失败，ADB 退出码：$LASTEXITCODE"
	}

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
	$launch = @(& $adb 'shell' 'am' 'start' '-W' '-n' "$package/simple.runtime.android.MainActivity")
	$launch | ForEach-Object { Write-Host $_ }
	if ($LASTEXITCODE -ne 0 -or -not ($launch -match '^Status:\s+ok\s*$')) {
		throw '应用启动失败，请检查 ADB 输出。'
	}
	Write-Host '应用安装并启动成功。'
}

Export-ModuleMember -Function Invoke-ProjectDeployment
