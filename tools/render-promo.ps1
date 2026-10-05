param([string]$CaptureDirectory = "$PSScriptRoot\..\artifacts\promo")
Add-Type -AssemblyName System.Drawing
$outDir = [IO.Path]::GetFullPath($CaptureDirectory)
$canvas = New-Object Drawing.Bitmap 2560,1440
$g = [Drawing.Graphics]::FromImage($canvas)
$g.SmoothingMode = [Drawing.Drawing2D.SmoothingMode]::AntiAlias
$g.InterpolationMode = [Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$g.TextRenderingHint = [Drawing.Text.TextRenderingHint]::AntiAliasGridFit
function Brush([string]$hex) { New-Object Drawing.SolidBrush ([Drawing.ColorTranslator]::FromHtml($hex)) }
function Label([string]$text, [single]$size, [single]$x, [single]$y, [string]$color, [bool]$bold = $false) {
    $style = if ($bold) { [Drawing.FontStyle]::Bold } else { [Drawing.FontStyle]::Regular }
    $font = New-Object Drawing.Font 'Yu Gothic UI',$size,$style,([Drawing.GraphicsUnit]::Pixel)
    $brush = Brush $color
    $g.DrawString($text,$font,$brush,$x,$y)
    $brush.Dispose(); $font.Dispose()
}
$g.Clear([Drawing.ColorTranslator]::FromHtml('#F1F7F3'))
$wash = Brush '#E3EFE7'
$g.FillEllipse($wash,1580,-400,1400,1900)
$wash.Dispose()
Label 'Tabiline' 106 126 128 '#214D3B' $true
Label '旅の予定を、' 82 126 390 '#142C22' $true
Label 'ひとつの流れに。' 82 126 504 '#142C22' $true
Label '電車も、徒歩も、空き時間も。' 36 132 686 '#456453'
Label '移動と予定を、見やすいタイムラインで。' 36 132 748 '#456453'
Label '乗り換え時間も、ホームの移動も。' 29 132 908 '#2D7053' $true
Label 'Android' 28 132 1176 '#456453'
Label 'github.com/m4sa-k1/tabiline' 28 132 1226 '#214D3B'
Label '実際のアプリ画面を使用。旅程・時刻は架空のサンプルです。' 22 132 1330 '#62776A'
function RoundedRect([single]$x, [single]$y, [single]$width, [single]$height, [single]$radius) {
    $path = New-Object Drawing.Drawing2D.GraphicsPath
    $d = $radius * 2
    $path.AddArc($x,$y,$d,$d,180,90)
    $path.AddArc($x+$width-$d,$y,$d,$d,270,90)
    $path.AddArc($x+$width-$d,$y+$height-$d,$d,$d,0,90)
    $path.AddArc($x,$y+$height-$d,$d,$d,90,90)
    $path.CloseFigure()
    return $path
}
# Original generic device frame drawn in code; no third-party frame asset.
function Screen([string]$name, [int]$x, [int]$y) {
    $img = [Drawing.Image]::FromFile((Join-Path $outDir $name))
    $height = 1170
    $width = [int]([double]$img.Width / $img.Height * $height)
    $shadow = Brush '#D4DFD8'
    $shadowPath = RoundedRect ($x-17) ($y-8) ($width+45) ($height+46) 66
    $g.FillPath($shadow,$shadowPath)
    $metal = Brush '#727C78'
    $outer = RoundedRect ($x-20) ($y-20) ($width+40) ($height+40) 68
    $g.FillPath($metal,$outer)
    $frame = Brush '#18201D'
    $inner = RoundedRect ($x-16) ($y-16) ($width+32) ($height+32) 64
    $g.FillPath($frame,$inner)
    $g.FillRectangle($metal,$x+$width+18,$y+210,7,96)
    $g.FillRectangle($metal,$x-25,$y+170,7,125)
    $screenClip = RoundedRect $x $y $width $height 49
    $saved = $g.Save()
    $g.SetClip($screenClip)
    $g.DrawImage($img,$x,$y,$width,$height)
    $g.Restore($saved)
    $g.FillEllipse($frame,[single]($x+$width/2-10),[single]($y+16),20,20)
    $lens = Brush '#34413E'
    $g.FillEllipse($lens,[single]($x+$width/2-4),[single]($y+22),8,8)
    $lens.Dispose(); $screenClip.Dispose(); $inner.Dispose(); $outer.Dispose()
    $shadowPath.Dispose(); $shadow.Dispose(); $metal.Dispose(); $frame.Dispose(); $img.Dispose()
}
Screen 'home.png' 1300 120
Screen 'timeline.png' 1910 170
$canvas.Save((Join-Path $outDir 'tabiline-promo-2560x1440.png'),[Drawing.Imaging.ImageFormat]::Png)
$g.Dispose(); $canvas.Dispose()
Write-Output (Join-Path $outDir 'tabiline-promo-2560x1440.png')
