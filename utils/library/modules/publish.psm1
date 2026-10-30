<#
将已编译的扩展库交付目录安装到当前 SDK；失败时保留原安装内容。
xhwsd@qq.com 2026-10-25
#>

function Assert-PublishTree {
	<#
	发布复制和清理不跟随符号链接或目录联接；同步软件的重解析标记不等于链接。
	#>
	param ([string]$Directory)
	if (-not (Test-Path -LiteralPath $Directory)) {
		return
	}
	$item = Get-Item -LiteralPath $Directory -Force
	if ($item.LinkType) {
		throw "发布目录不能是链接：$Directory"
	}
	if (-not $item.PSIsContainer) {
		throw "发布目录被普通文件占用：$Directory"
	}
	foreach ($child in Get-ChildItem -LiteralPath $Directory -Force) {
		if ($child.LinkType) {
			throw "发布文件不能是链接：$($child.FullName)"
		}
		if ($child.PSIsContainer) {
			Assert-PublishTree $child.FullName
		}
	}
}

function Resolve-PublishFile {
	<#
	已编译库的文件引用必须位于库内且脱离 build；复制前检查沿途目录，不跟随链接。
	#>
	param ([string]$Root, [string]$Relative)
	$file = [IO.Path]::GetFullPath((Join-Path $Root $Relative))
	if ([string]::IsNullOrWhiteSpace($Relative) -or [IO.Path]::IsPathRooted($Relative) -or
		$Relative -match '(^|[\\/])\.\.([\\/]|$)' -or
		-not $file.StartsWith($Root + '\', [StringComparison]::OrdinalIgnoreCase) -or
		$file.StartsWith($Root + '\build\', [StringComparison]::OrdinalIgnoreCase) -or
		-not (Test-Path -LiteralPath $file -PathType Leaf)) {
		throw "交付目录中的依赖或文件无效：$Relative"
	}
	$item = Get-Item -LiteralPath $file -Force
	while ($item.FullName -ne $Root) {
		if ($item.LinkType) {
			throw "发布文件不能经过链接：$($item.FullName)"
		}
		$item = Get-Item -LiteralPath (Split-Path $item.FullName -Parent) -Force
	}
	return $file
}

function Copy-PublishDirectory {
	<#
	复制类库自有目录并拒绝链接；仅示例根目录的 build 子目录不进入安装内容。
	#>
	param ([string]$Source, [string]$Destination, [bool]$ExcludeBuild = $false)
	$item = Get-Item -LiteralPath $Source -Force
	if ($item.LinkType -or -not $item.PSIsContainer) {
		throw "发布目录无效或属于链接：$Source"
	}
	[IO.Directory]::CreateDirectory($Destination) | Out-Null
	foreach ($child in Get-ChildItem -LiteralPath $Source -Force) {
		if ($ExcludeBuild -and $child.PSIsContainer -and $child.Name -eq 'build') {
			continue
		}
		if ($child.LinkType) {
			throw "发布文件不能是链接：$($child.FullName)"
		}
		$target = Join-Path $Destination $child.Name
		if ($child.PSIsContainer) {
			Copy-PublishDirectory $child.FullName $target
		} else {
			Copy-Item -LiteralPath $child.FullName -Destination $target -Force
		}
	}
}

function Publish-Library {
	<#
	按资源命名空间安装本次完整产物；不读取或改写 SDK 总清单。
	相同类库发布串行执行，先准备完整目录，再替换旧安装；替换失败时恢复旧目录。
	#>
	param ([string]$LibraryRoot, $Configuration)
	$sourceRoot = [IO.Path]::GetFullPath($LibraryRoot).TrimEnd('\', '/')
	if (-not (Test-Path -LiteralPath $sourceRoot -PathType Container) -or
		(Get-Item -LiteralPath $sourceRoot -Force).LinkType) {
		throw "类库目录无效或属于链接：$sourceRoot"
	}
	# 源码目录默认安装上次的 build/<命名空间>/；交付目录可以直接复制或传给 publish。
	if (Test-Path -LiteralPath (Join-Path $sourceRoot 'library.gradle') -PathType Leaf) {
		$build = Join-Path $sourceRoot 'build'
		$candidates = @(Get-ChildItem -LiteralPath $build -Directory -ErrorAction SilentlyContinue | Where-Object {
			(Test-Path -LiteralPath (Join-Path $_.FullName 'classes.jar') -PathType Leaf) -and
			(Test-Path -LiteralPath (Join-Path $_.FullName 'library.json') -PathType Leaf)
		})
		if ($candidates.Count -ne 1) {
			throw '请先编译扩展库，或直接指定唯一的完整交付目录。'
		}
		$sourceRoot = $candidates[0].FullName
	}
	if ((Get-Item -LiteralPath $sourceRoot -Force).LinkType) {
		throw "类库交付目录不能是链接：$sourceRoot"
	}
	$files = @('classes.jar', 'library.json', 'AndroidManifest.xml', 'dependencies.module', 'R.txt')
	foreach ($name in $files) {
		Resolve-PublishFile $sourceRoot $name | Out-Null
	}
	$manifest = Get-Content -LiteralPath (Join-Path $sourceRoot 'library.json') -Raw -Encoding UTF8 | ConvertFrom-Json
	if ($manifest.kind -ne 'library') {
		throw '发布的 library.json 必须声明 kind 为 library。'
	}

	# 资源命名空间以标准 Android 清单为准，不依赖文件夹名称或额外索引。
	$settings = [Xml.XmlReaderSettings]::new()
	$settings.DtdProcessing = [Xml.DtdProcessing]::Prohibit
	$settings.XmlResolver = $null
	$reader = [Xml.XmlReader]::Create((Join-Path $sourceRoot 'AndroidManifest.xml'), $settings)
	try {
		$androidManifest = [Xml.XmlDocument]::new()
		$androidManifest.XmlResolver = $null
		$androidManifest.Load($reader)
		$namespace = $androidManifest.DocumentElement.GetAttribute('package')
	} finally {
		$reader.Dispose()
	}
	if ($androidManifest.DocumentElement.Name -ne 'manifest' -or
		$namespace -notmatch '^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+$') {
		throw 'AndroidManifest.xml 中的类库命名空间无效。'
	}
	$module = Get-Content -LiteralPath (Join-Path $sourceRoot 'dependencies.module') -Raw -Encoding UTF8 | ConvertFrom-Json
	if ($module.formatVersion -ne '1.1' -or $module.component.group -ne 'es4a.extension' -or
		$module.component.module -ne $namespace -or
		[string]$module.component.version -notmatch '^[A-Za-z0-9][A-Za-z0-9._+\-]*$' -or
		@($module.variants | Where-Object { $_.attributes.'org.gradle.usage' -eq 'java-runtime' }).Count -eq 0 -or
		@($module.variants | Where-Object { $_.files -or $_.'available-at' }).Count -gt 0) {
		throw '类库原生依赖元数据身份不匹配或包含不应交付的制品引用。'
	}
	foreach ($entry in @(@('libs', '*.jar'), @('aars', '*.aar'))) {
		$directory = Join-Path $sourceRoot $entry[0]
		if (Test-Path -LiteralPath $directory -PathType Container) {
			$files += @(Get-ChildItem -LiteralPath $directory -File -Force | Where-Object {
				$_.Name -like $entry[1] -and $_.Name -notlike '*-sources.jar'
			} | ForEach-Object { "$($entry[0])/$($_.Name)" })
		}
	}

	# 安装目录由命名空间决定，与源码文件夹名称和源码所在盘符无关。
	$installRoot = Join-Path $Configuration.sdk 'libraries'
	$target = Join-Path $installRoot $namespace
	$source = $sourceRoot
	if ($source.Equals($target, [StringComparison]::OrdinalIgnoreCase) -or
		$source.StartsWith($target + '\', [StringComparison]::OrdinalIgnoreCase) -or
		$target.StartsWith($source + '\', [StringComparison]::OrdinalIgnoreCase)) {
		throw '源码目录不能与 SDK 中的安装目录重叠。'
	}
	foreach ($directory in @($installRoot, (Join-Path $Configuration.sdk 'build'))) {
		if ((Test-Path -LiteralPath $directory) -and
			(Get-Item -LiteralPath $directory -Force).LinkType) {
			throw "SDK 发布目录不能是链接：$directory"
		}
	}
	Assert-PublishTree $target
	[IO.Directory]::CreateDirectory($installRoot) | Out-Null
	$workParent = [IO.Path]::GetFullPath((Join-Path $Configuration.sdk 'build/library/publish'))
	Assert-PublishTree (Join-Path $Configuration.sdk 'build/library')
	[IO.Directory]::CreateDirectory($workParent) | Out-Null
	$lock = [IO.File]::Open((Join-Path $workParent ($namespace + '.lock')),
		[IO.FileMode]::OpenOrCreate, [IO.FileAccess]::ReadWrite, [IO.FileShare]::None)
	$work = Join-Path $workParent ([Guid]::NewGuid().ToString('N'))
	$pending = Join-Path $work 'new'
	$previous = Join-Path $work 'previous'
	$cleanup = $true
	try {
		[IO.Directory]::CreateDirectory($pending) | Out-Null
		# 直接从完整库根安装已编译文件，排除源码、测试、构建目录和本机开发配置。
		foreach ($name in @('README.md', 'LICENSE', 'LICENSE.txt', 'NOTICE', 'NOTICE.txt')) {
			if (Test-Path -LiteralPath (Join-Path $sourceRoot $name) -PathType Leaf) {
				$files += $name
			}
		}
		foreach ($directory in @('libs', 'aars')) {
			$localRoot = Join-Path $sourceRoot $directory
			if (Test-Path -LiteralPath $localRoot -PathType Container) {
				$files += @(Get-ChildItem -LiteralPath $localRoot -File -Force | Where-Object {
					# 源码附件不发布，即使文件名同时符合许可证或 NOTICE 前缀。
					$_.Name -notlike '*-sources.jar' -and
						($_.Name -like '*.txt' -or $_.Name -like 'LICENSE*' -or $_.Name -like 'NOTICE*')
				} | ForEach-Object { "$directory/$($_.Name)" })
			}
		}
		foreach ($relative in $files | Select-Object -Unique) {
			$file = Resolve-PublishFile $sourceRoot $relative
			$destination = Join-Path $pending $file.Substring($sourceRoot.Length + 1)
			[IO.Directory]::CreateDirectory((Split-Path $destination -Parent)) | Out-Null
			Copy-Item -LiteralPath $file -Destination $destination -Force
		}
		foreach ($name in @('icons', 'res', 'assets', 'jni', 'example', 'dependencies')) {
			$directory = Join-Path $sourceRoot $name
			if (Test-Path -LiteralPath $directory) {
				Copy-PublishDirectory $directory (Join-Path $pending $name) ($name -eq 'example')
			}
		}
		$hadPrevious = Test-Path -LiteralPath $target
		if ($hadPrevious) {
			Move-Item -LiteralPath $target -Destination $previous
		}
		try {
			Move-Item -LiteralPath $pending -Destination $target
		} catch {
			$failure = $_
			if ($hadPrevious) {
				try {
					Move-Item -LiteralPath $previous -Destination $target
				} catch {
					$cleanup = $false
					throw "安装失败且恢复未完成；原安装文件保留在：$previous。$($_.Exception.Message)"
				}
			}
			throw $failure
		}
		Write-Output "类库已发布：$target"
	} finally {
		try {
			# 递归清理只限本次发布工作目录，不能删除安装根目录或类库源码。
			if ($cleanup -and (Test-Path -LiteralPath $work)) {
				$resolved = (Resolve-Path -LiteralPath $work).Path
				if (-not $resolved.StartsWith($workParent + '\', [StringComparison]::OrdinalIgnoreCase)) {
					throw '发布清理路径超出 SDK 生成目录。'
				}
				Assert-PublishTree $resolved
				Remove-Item -LiteralPath $resolved -Recurse -Force
			}
		} finally {
			$lock.Dispose()
		}
	}
}

Export-ModuleMember -Function Publish-Library
