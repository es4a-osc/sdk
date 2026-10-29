<#
扩展库初始文件的模板填充、JSON 动态片段格式化及文本写入。
xhwsd@qq.com 2026-10-28
#>
$templateRoot = Join-Path (Split-Path $PSScriptRoot -Parent) 'templates'

function Write-Utf8File {
	<#
	统一写出 UTF-8、CRLF 和末尾换行，保留模板中的 Tab 与内容排版。
	#>
	param ([string]$File, [string]$Text)
	$normalized = ($Text -replace "`r?`n", "`r`n").TrimEnd("`r", "`n") + "`r`n"
	[IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($File)) | Out-Null
	[IO.File]::WriteAllText($File, $normalized, [Text.UTF8Encoding]::new($false))
}

function ConvertTo-TemplateJson {
	<#
	由序列化器负责转义，再仅格式化字符串外的 JSON 标点；空数组和空对象保持单行。
	#>
	param ($Value)
	$json = ConvertTo-Json -InputObject $Value -Depth 20 -Compress
	$result = [Text.StringBuilder]::new()
	$depth = 0
	$quoted = $false
	$escaped = $false
	$previous = [char]0
	for ($index = 0; $index -lt $json.Length; $index++) {
		$character = $json[$index]
		if ($quoted) {
			[void]$result.Append($character)
			if ($escaped) {
				$escaped = $false
			} elseif ($character -eq '\') {
				$escaped = $true
			} elseif ($character -eq '"') {
				$quoted = $false
			}
		} elseif ($character -eq '"') {
			$quoted = $true
			[void]$result.Append($character)
		} elseif (-not [char]::IsWhiteSpace($character)) {
			switch ($character) {
				{ $_ -in '{', '[' } {
					[void]$result.Append($character)
					$next = $index + 1
					while ($next -lt $json.Length -and [char]::IsWhiteSpace($json[$next])) {
						$next++
					}
					if ($next -lt $json.Length -and $json[$next] -notin '}', ']') {
						$depth++
						[void]$result.Append("`r`n" + ("`t" * $depth))
					}
				}
				{ $_ -in '}', ']' } {
					if ($previous -notin '{', '[') {
						$depth--
						[void]$result.Append("`r`n" + ("`t" * $depth))
					}
					[void]$result.Append($character)
				}
				',' { [void]$result.Append(",`r`n" + ("`t" * $depth)) }
				':' { [void]$result.Append(': ') }
				default { [void]$result.Append($character) }
			}
		}
		if (-not [char]::IsWhiteSpace($character)) {
			$previous = $character
		}
	}
	return $result.ToString()
}

function Write-LibraryTemplate {
	<#
	单次替换原模板中的占位符，避免把用户文本误当作二次模板；JSON 值始终通过序列化填充。
	#>
	param ([string]$Template, [string]$File, [Collections.IDictionary]$Values)
	$source = Join-Path $templateRoot $Template
	$text = [IO.File]::ReadAllText($source)
	$jsonTemplate = $Template.EndsWith('.json.template') -or $Template.EndsWith('.code-workspace.template')
	$pattern = if ($jsonTemplate) { '"@([A-Z_]+)@"' } else { '@([A-Z_]+)@' }
	$rendered = [regex]::Replace($text, $pattern, [Text.RegularExpressions.MatchEvaluator]{
		param ($match)
		$key = $match.Groups[1].Value
		if (-not $Values.Contains($key)) {
			throw "模板缺少占位符数据：$Template / $key"
		}
		if (-not $jsonTemplate) {
			return [string]$Values[$key]
		}
		$fragment = ConvertTo-TemplateJson $Values[$key]
		$lineStart = $text.LastIndexOf("`n", $match.Index) + 1
		$indent = [regex]::Match($text.Substring($lineStart), '^\t*').Value
		return $fragment.Replace("`r`n", "`r`n" + $indent)
	})
	if ($jsonTemplate) {
		$null = $rendered | ConvertFrom-Json -ErrorAction Stop
	}
	Write-Utf8File $File $rendered
}

function New-Library {
	<#
	按模板相对目录创建初始文件；工作区依赖模型准备完成后再填充，不额外生成通用名工作区。
	#>
	param ([string]$InputPath, [string]$Namespace, [string]$DisplayName)
	if ([string]::IsNullOrWhiteSpace($InputPath)) {
		throw '请指定目录。执行 library.ps1 help 查看用法。'
	}
	$libraryRoot = $ExecutionContext.SessionState.Path.GetUnresolvedProviderPathFromPSPath($InputPath)
	if (Test-Path -LiteralPath $libraryRoot) {
		throw "目标已存在，不会覆盖：$libraryRoot"
	}
	if ($Namespace -notmatch '^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+$') {
		throw '基础包名使用小写英文、数字、下划线和点，例如 com.example.demo。'
	}
	$keywords = 'abstract assert boolean break byte case catch char class const continue default do double else enum extends final finally float for goto if implements import instanceof int interface long native new package private protected public return short static strictfp super switch synchronized this throw throws transient try void volatile while true false null'.Split(' ')
	if (@($Namespace.Split('.') | Where-Object { $_ -in $keywords }).Count -gt 0) {
		throw '基础包名不能包含 Java 关键字。'
	}
	if ([string]::IsNullOrWhiteSpace($DisplayName)) {
		throw '类库显示名称不能为空。'
	}
	foreach ($name in @('library.json.template', 'library.code-workspace.template')) {
		if (-not (Test-Path -LiteralPath (Join-Path $templateRoot $name) -PathType Leaf)) {
			throw "缺少类库模板：$name"
		}
	}
	$values = @{
		NAME = $DisplayName
		NAMESPACE = $Namespace
		TOOL_TYPE = "$Namespace.工具"
		AUTHOR = [Environment]::UserName
		YEAR = (Get-Date).Year
	}
	foreach ($template in Get-ChildItem -LiteralPath $templateRoot -File -Recurse) {
		if ($template.Name -eq 'library.code-workspace.template') {
			continue
		}
		$relative = $template.FullName.Substring($templateRoot.Length + 1)
		$destination = $relative -replace '(^|[\\/])namespace(?=[\\/])', ('$1' + $Namespace.Replace('.', '/'))
		if ($destination.EndsWith('.template')) {
			$destination = $destination.Substring(0, $destination.Length - '.template'.Length)
		}
		Write-LibraryTemplate $relative (Join-Path $libraryRoot $destination) $values
	}
	return $libraryRoot
}

Export-ModuleMember -Function New-Library, Write-LibraryTemplate
