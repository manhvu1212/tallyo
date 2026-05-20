# Generates Play Store assets (app icon + feature graphic) from the
# adaptive launcher icon design. Run from repo root or this folder.
#
#   powershell -NoProfile -ExecutionPolicy Bypass -File generate.ps1

Add-Type -AssemblyName System.Drawing

$here  = Split-Path -Parent $MyInvocation.MyCommand.Path
$icon  = Join-Path $here 'ic_launcher_512.png'
$feat  = Join-Path $here 'feature_graphic.png'

# Palette (matches drawable/ic_launcher_*.xml + Compose Color.kt)
$bg    = [System.Drawing.Color]::FromArgb(0x0F, 0x11, 0x15)
$plus  = [System.Drawing.Color]::FromArgb(0x4E, 0xD2, 0xA8)
$minus = [System.Drawing.Color]::FromArgb(0xE5, 0x48, 0x4D)
$white = [System.Drawing.Color]::FromArgb(0xF7, 0xF8, 0xFA)
$muted = [System.Drawing.Color]::FromArgb(0x8B, 0x93, 0xA1)

# Draw the stacked +/- glyph into a graphics context.
# `s` = full viewport size (the adaptive icon uses 108 units).
function Draw-Glyph {
    param([System.Drawing.Graphics]$g, [int]$ox, [int]$oy, [int]$s)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $u = $s / 108.0
    $plusBrush  = New-Object System.Drawing.SolidBrush $plus
    $minusBrush = New-Object System.Drawing.SolidBrush $minus
    # Plus: vertical bar (49,29 w10 h30) + horizontal bar (39,39 w30 h10)
    $g.FillRectangle($plusBrush,  [single]($ox + 49*$u), [single]($oy + 29*$u), [single](10*$u), [single](30*$u))
    $g.FillRectangle($plusBrush,  [single]($ox + 39*$u), [single]($oy + 39*$u), [single](30*$u), [single](10*$u))
    # Minus: horizontal bar (39,69 w30 h10)
    $g.FillRectangle($minusBrush, [single]($ox + 39*$u), [single]($oy + 69*$u), [single](30*$u), [single](10*$u))
    $plusBrush.Dispose(); $minusBrush.Dispose()
}

# --- App icon (512x512) ---
$bmp = New-Object System.Drawing.Bitmap 512, 512
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.Clear($bg)
Draw-Glyph -g $g -ox 0 -oy 0 -s 512
$g.Dispose()
$bmp.Save($icon, [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()
Write-Output "wrote $icon ($([math]::Round((Get-Item $icon).Length/1KB,1)) KB)"

# --- Feature graphic (1024x500) ---
$bmp = New-Object System.Drawing.Bitmap 1024, 500
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.SmoothingMode    = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$g.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit
$g.Clear($bg)

# Subtle diagonal accent stripe so the banner isn't pure flat
$accent = [System.Drawing.Color]::FromArgb(28, 0x4E, 0xD2, 0xA8)  # alpha 28
$accentBrush = New-Object System.Drawing.SolidBrush $accent
$g.FillPolygon($accentBrush, @(
    (New-Object System.Drawing.Point 0,   500),
    (New-Object System.Drawing.Point 0,   380),
    (New-Object System.Drawing.Point 420, 0),
    (New-Object System.Drawing.Point 540, 0),
    (New-Object System.Drawing.Point 0,   500)
))
$accentBrush.Dispose()

# Glyph on the left (centered vertically, size 380)
$glyphSize = 380
$gx = 80
$gy = (500 - $glyphSize) / 2
Draw-Glyph -g $g -ox $gx -oy $gy -s $glyphSize

# Title + tagline on the right
$titleFont   = New-Object System.Drawing.Font 'Segoe UI', 96, ([System.Drawing.FontStyle]::Bold), ([System.Drawing.GraphicsUnit]::Pixel)
$taglineFont = New-Object System.Drawing.Font 'Segoe UI', 34, ([System.Drawing.FontStyle]::Regular), ([System.Drawing.GraphicsUnit]::Pixel)
$titleBrush   = New-Object System.Drawing.SolidBrush $white
$taglineBrush = New-Object System.Drawing.SolidBrush $muted

$textX = 520
$g.DrawString('Tallyo',                       $titleFont,   $titleBrush,   [single]$textX, 170)
$g.DrawString('Score keeper for every game.', $taglineFont, $taglineBrush, [single]$textX, 290)

$titleFont.Dispose(); $taglineFont.Dispose()
$titleBrush.Dispose(); $taglineBrush.Dispose()
$g.Dispose()
$bmp.Save($feat, [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()
Write-Output "wrote $feat ($([math]::Round((Get-Item $feat).Length/1KB,1)) KB)"
