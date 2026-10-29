<#
Simple 项目编译、工具检查及临时进程环境；构建目录仍由编译器解析。
xhwsd@qq.com 2026-10-28
#>

function Get-InputPath {
	<#
	相对输入按调用者当前目录解析，SDK 工具按脚本所在目录定位。
	#>
	param ([string]$Value, [string]$Description)
	if ([string]::IsNullOrWhiteSpace($Value)) {
		throw "请指定$Description。执行 project.ps1 help 查看用法。"
	}
	return $ExecutionContext.SessionState.Path.GetUnresolvedProviderPathFromPSPath($Value)
}

function Assert-File {
	<#
	在调用外部程序前确认必需文件存在，避免掩盖工具缺失原因。
	#>
	param ([string]$File)
	if (-not (Test-Path -LiteralPath $File -PathType Leaf)) {
		throw "未找到文件：$File"
	}
}

function Invoke-ProjectCompilation {
	<#
	在临时 SDK 环境中编译项目，恢复调用前的环境和编码；调试时返回实际 APK 与部署工具路径。
	#>
	param ([string]$SdkRoot, [string]$ProjectFile, [switch]$Offline,
		[switch]$RefreshDependencies, [switch]$ForDebug)
	$environment = @{}
	foreach ($key in @('JAVA_HOME', 'ANDROID_HOME', 'SIMPLE_HOME', 'LIBRARIES_HOME', 'ES4A_SDK_HOME', 'ES4A_OFFLINE', 'ES4A_REFRESH_DEPENDENCIES')) {
		$environment[$key] = [Environment]::GetEnvironmentVariable($key, 'Process')
	}
	$originalEncoding = [Console]::OutputEncoding
	$originalOutputEncoding = $OutputEncoding
	try {
		$project = Get-InputPath $ProjectFile 'Simple 项目文件'
		Assert-File $project

		$sdkRoot = [IO.Path]::GetFullPath($SdkRoot)
		$env:JAVA_HOME = Join-Path $sdkRoot 'tools/jdk-11.0.32.1'
		$env:ANDROID_HOME = Join-Path $sdkRoot 'tools/android-sdk'
		$env:SIMPLE_HOME = Join-Path $sdkRoot 'simple'
		$env:LIBRARIES_HOME = Join-Path $sdkRoot 'libraries'
		$env:ES4A_SDK_HOME = $sdkRoot
		$env:ES4A_OFFLINE = if ($Offline) { '1' } else { '0' }
		$env:ES4A_REFRESH_DEPENDENCIES = if ($RefreshDependencies) { '1' } else { '0' }
		if ($Offline -and $RefreshDependencies) {
			throw '离线构建不能同时刷新仓库依赖。'
		}
		$java = Join-Path $env:JAVA_HOME 'bin/java.exe'
		$compiler = Join-Path $env:SIMPLE_HOME 'SimpleCompiler.jar'
		$compilerLibraries = Join-Path $env:SIMPLE_HOME 'libs'
		if (-not (Test-Path -LiteralPath $compilerLibraries -PathType Container) -or
			@(Get-ChildItem -LiteralPath $compilerLibraries -Filter '*.jar' -File).Count -eq 0) {
			throw "缺少编译器配套依赖目录：$compilerLibraries。请交付完整编译器工具包。"
		}
		# 此次保留编译器现有的 API 30 工具布局，不改变其内部构建流程。
		$platform = Join-Path $env:ANDROID_HOME 'platforms/android-30'
		$d8 = Join-Path $platform 'tools/lib/d8.jar'
		foreach ($file in @($java, $compiler, $d8, (Join-Path $platform 'android.jar'),
			(Join-Path $env:SIMPLE_HOME 'SimpleAndroidRuntime.jar'))) {
			Assert-File $file
		}
		$release = [IO.File]::ReadAllText((Join-Path $env:JAVA_HOME 'release'))
		if ($release -notmatch 'JAVA_VERSION="11[.\"]') {
			throw '项目工具需要 SDK 提供的 JDK 11。'
		}
		if ($ForDebug) {
			$adb = Join-Path $env:ANDROID_HOME 'platform-tools/adb.exe'
			$aapt = Join-Path $platform 'tools/aapt2_utf8.exe'
			if (-not (Test-Path -LiteralPath $aapt -PathType Leaf)) {
				$aapt = Join-Path $platform 'tools/aapt2.exe'
			}
			Assert-File $adb
			Assert-File $aapt
		}

		# JDK 11 的控制台输出可独立使用系统代码页，file.encoding 不会覆盖该编码。
		# 标准输出和错误输出显式使用 UTF-8，与 PowerShell 的解码保持一致。
		[Console]::OutputEncoding = [Text.UTF8Encoding]::new($false)
		$OutputEncoding = [Console]::OutputEncoding
		$compilerArguments = @(
			'-Dfile.encoding=UTF-8',
			'-Dsun.stdout.encoding=UTF-8',
			'-Dsun.stderr.encoding=UTF-8',
			'-classpath',
			"$d8;$compiler;$compilerLibraries/*",
			'simple.compiler.Main'
		)
		if ($ForDebug) {
			# 构建目录由编译器自身读取，保留重复键、转义、续行及自定义目录的既有语义。
			$buildDirectory = @(& $java @compilerArguments '--build-directory' $project)
			if ($LASTEXITCODE -ne 0 -or $buildDirectory.Count -ne 1 -or
				[string]::IsNullOrWhiteSpace([string]$buildDirectory[0]) -or
				-not [IO.Path]::IsPathRooted([string]$buildDirectory[0])) {
				throw '无法取得项目构建目录。'
			}
			$apk = Join-Path ([string]$buildDirectory[0]) 'apks/signed.apk'
		}
		Write-Host '[编译应用]'
		& $java @compilerArguments $project | ForEach-Object { Write-Host $_ }
		if ($LASTEXITCODE -ne 0) {
			throw "编译应用失败，编译器退出码：$LASTEXITCODE"
		}
		return @{ apk = $apk; adb = $adb; aapt = $aapt }
	} finally {
		foreach ($key in $environment.Keys) {
			[Environment]::SetEnvironmentVariable($key, $environment[$key], 'Process')
		}
		[Console]::OutputEncoding = $originalEncoding
		$OutputEncoding = $originalOutputEncoding
	}
}

Export-ModuleMember -Function Invoke-ProjectCompilation
