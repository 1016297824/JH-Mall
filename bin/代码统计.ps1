$tmp = "$env:TEMP\cloc_raw.csv"
& "G:\scoop\shims\cloc.exe" client/ruoyi-ui/ server/ruoyi/ server/mall/ `
  --exclude-dir=node_modules,target --csv --quiet --out=$tmp

$keep = @('Java', 'Vuejs Component', 'TypeScript', 'JavaScript', 'SCSS')
$r = Import-Csv $tmp | Where-Object { $_.language -in $keep }

$o = @('语言,文件数,空行,注释,代码')
$tf = 0; $tb = 0; $tc = 0; $tk = 0
foreach ($x in $r) {
  $o += "$($x.language),$($x.files),$($x.blank),$($x.comment),$($x.code)"
  $tf += [int]$x.files; $tb += [int]$x.blank
  $tc += [int]$x.comment; $tk += [int]$x.code
}
$o += "合计,${tf},${tb},${tc},${tk}"
$content = ($o -join "`r`n") + "`r`n"
[System.IO.File]::WriteAllText("$PSScriptRoot\..\docs\统计\代码统计.csv", $content, [Text.UTF8Encoding]::new($true))
Write-Host "已写入，合计 ${tk} 行代码"
Remove-Item $tmp
