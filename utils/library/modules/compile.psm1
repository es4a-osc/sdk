<#
扩展库环境核对、路径解析与公共 Gradle 调用。
xhwsd@qq.com 2026-10-28
#>
$toolRoot = Split-Path $PSScriptRoot -Parent
$sdkRoot = [IO.Path]::GetFullPath((Join-Path $toolRoot '../..'))

function Get-AbsolutePath {
	<#
	按调用者当前目录解析输入，不依赖工具包的安装盘符。
	#>
	param ([string]$InputPath)
	if ([string]::IsNullOrWhiteSpace($InputPath)) {
		throw '请指定目录。执行 library.ps1 help 查看用法。'
	}
	return $ExecutionContext.SessionState.Path.GetUnresolvedProviderPathFromPSPath($InputPath)
}

function Get-Configuration {
	<#
	以工具所在位置定位 SDK；工具路径集中维护，不读取本机配置或外部环境变量。
	#>
	$config = @{
		sdk = $sdkRoot
		androidSdk = (Join-Path $sdkRoot 'tools/android-sdk')
		jdk = (Join-Path $sdkRoot 'tools/jdk-11.0.32.1')
		gradle = (Join-Path $sdkRoot 'tools/gradle-8.14.6')
	}
	$required = @(
		(Join-Path $config.sdk 'simple/SimpleAndroidRuntime.jar'),
		(Join-Path $config.jdk 'bin/javac.exe'),
		(Join-Path $config.gradle 'bin/gradle.bat'),
		(Join-Path $config.androidSdk 'platforms/android-30/android.jar'),
		(Join-Path $config.androidSdk 'build-tools/36.1.0/aapt2.exe')
	)
	foreach ($file in $required) {
		if (-not (Test-Path -LiteralPath $file -PathType Leaf)) {
			throw "缺少 SDK 开发工具或文件：$file。请核对 SDK 的固定工具目录。"
		}
	}
	$release = [IO.File]::ReadAllText((Join-Path $config.jdk 'release'))
	if ($release -notmatch 'JAVA_VERSION="11[.\"]') {
		throw "本工具链需要 SDK 提供的 JDK 11。"
	}
	return $config
}

function Get-LibraryDirectory {
	<#
	限定操作对象为具有源码、Simple 清单和唯一构建声明的扩展库。
	#>
	param ([string]$InputPath)
	$libraryRoot = Get-AbsolutePath $InputPath
	foreach ($file in @('library.json', 'library.gradle')) {
		if (-not (Test-Path -LiteralPath (Join-Path $libraryRoot $file) -PathType Leaf)) {
			throw "不是可构建的扩展库，缺少：$(Join-Path $libraryRoot $file)"
		}
	}
	if (-not (Test-Path -LiteralPath (Join-Path $libraryRoot 'src') -PathType Container)) {
		throw "未找到扩展库源码目录：$libraryRoot\src"
	}
	return $libraryRoot
}

function Invoke-LibraryBuild {
	<#
	正式编译和 IDE 准备共用同一 Gradle 工程、依赖解析及 JDK。
	#>
	param ([string]$LibraryRoot, $Configuration, [string]$Task, [string]$OutputRoot, [switch]$IncludeDependencies)
	$gradle = Join-Path $Configuration.gradle 'bin/gradle.bat'
	$previousJava = $env:JAVA_HOME
	$previousAndroid = $env:ANDROID_HOME
	$previousAndroidRoot = $env:ANDROID_SDK_ROOT
	$previousJavaOptions = $env:JAVA_OPTS
	$previousEncoding = [Console]::OutputEncoding
	try {
		[Console]::OutputEncoding = [Text.UTF8Encoding]::new($false)
		$env:JAVA_HOME = $Configuration.jdk
		$env:ANDROID_HOME = $Configuration.androidSdk
		$env:ANDROID_SDK_ROOT = $Configuration.androidSdk
		$env:JAVA_OPTS = "$previousJavaOptions -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8"
		$arguments = @(
			'--console=plain', '--no-daemon',
			'-p', $LibraryRoot,
			'--settings-file', (Join-Path $toolRoot 'gradle/settings.gradle'),
			'--project-cache-dir', (Join-Path $LibraryRoot 'build/gradle/cache'),
			"-PlibraryDir=$LibraryRoot", "-PsdkDir=$($Configuration.sdk)",
			"-Pandroid.aapt2FromMavenOverride=$(Join-Path $Configuration.androidSdk 'build-tools/36.1.0/aapt2.exe')", $Task
		)
		if ($IncludeDependencies) {
			$arguments += '-PincludeDependencies=true'
		}
		if ($OutputRoot) {
			$arguments += "-PoutputRoot=$OutputRoot"
		}
		& $gradle @arguments
		if ($LASTEXITCODE -ne 0) {
			throw "Gradle 执行失败，退出代码：$LASTEXITCODE。上方保留了实际诊断信息。"
		}
	} finally {
		$env:JAVA_HOME = $previousJava
		$env:ANDROID_HOME = $previousAndroid
		$env:ANDROID_SDK_ROOT = $previousAndroidRoot
		$env:JAVA_OPTS = $previousJavaOptions
		[Console]::OutputEncoding = $previousEncoding
	}
}

Export-ModuleMember -Function Get-AbsolutePath, Get-Configuration, Get-LibraryDirectory, Invoke-LibraryBuild
