<#
将正式 IDE 类路径映射为工作区模板的动态值，不在脚本中维护工作区固定结构。
xhwsd@qq.com 2026-10-28
#>
Import-Module (Join-Path $PSScriptRoot 'create.psm1') -DisableNameChecking
$toolRoot = Split-Path $PSScriptRoot -Parent

function Get-WorkspacePath {
	<#
	同盘路径相对于类库根目录，跨盘保留绝对路径；不推断固定的 SDK 安装位置。
	#>
	param ([string]$LibraryRoot, [string]$File)
	if ([IO.Path]::GetPathRoot($LibraryRoot) -eq [IO.Path]::GetPathRoot($File)) {
		$libraryUri = [Uri]::new($LibraryRoot.TrimEnd('\', '/') + [IO.Path]::DirectorySeparatorChar)
		return [Uri]::UnescapeDataString($libraryUri.MakeRelativeUri([Uri]::new($File)).ToString())
	}
	return $File.Replace('\', '/')
}

function Write-Workspace {
	<#
	依赖来自正式编译模型；保留 SDK 直接引用及源码关联规则，仅填充模板中的动态字段。
	#>
	param ([string]$LibraryRoot, $Configuration)
	$model = Get-Content -LiteralPath (Join-Path $LibraryRoot 'build/ide/classpath.json') -Raw -Encoding UTF8 | ConvertFrom-Json
	$jdk = Get-WorkspacePath $LibraryRoot $Configuration.jdk
	if (-not [IO.Path]::IsPathRooted($jdk)) {
		$jdk = '${workspaceFolder}/' + $jdk
	}
	$ideRoot = Join-Path $LibraryRoot 'build/ide/libs'
	$localJars = Join-Path $LibraryRoot 'libs'
	$includes = @()
	$sources = [ordered]@{}
	foreach ($library in $model.libraries) {
		$binary = Get-WorkspacePath $LibraryRoot $library.path
		if (-not $library.path.StartsWith($ideRoot + '\', [StringComparison]::OrdinalIgnoreCase) -and
			[IO.Path]::GetDirectoryName($library.path) -ne $localJars) {
			$includes += $binary
		}
		if ($library.source -and $library.source -ne ($library.path -replace '\.jar$', '-sources.jar')) {
			$sources[$binary] = Get-WorkspacePath $LibraryRoot $library.source
		}
	}
	$values = @{
		JDK_PATH = $jdk
		JAVA_RUNTIME = "JavaSE-$($model.java)"
		LIBRARY_TOOL = Get-WorkspacePath $LibraryRoot (Join-Path $toolRoot 'library.ps1')
		REFERENCE_INCLUDES = @($includes + @('libs/*.jar', 'build/ide/libs/**/*.jar') | Select-Object -Unique)
		REFERENCE_SOURCES = $sources
	}
	$file = Join-Path $LibraryRoot ($model.namespace + '.code-workspace')
	Write-LibraryTemplate 'library.code-workspace.template' $file $values
	return $file
}

Export-ModuleMember -Function Write-Workspace
