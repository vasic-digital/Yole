#!/bin/bash

# SPDX-FileCopyrightText: 2025 Milos Vasic
# SPDX-License-Identifier: Apache-2.0

# Script to generate launcher icons from Assets/Logo.png for all platforms

set -e

LOGO="Assets/Logo.png"

if [ ! -f "$LOGO" ]; then
    echo "Error: $LOGO not found"
    exit 1
fi

# Check if ImageMagick is installed
if ! command -v convert &> /dev/null; then
    echo "Error: ImageMagick 'convert' command not found. Please install ImageMagick."
    exit 1
fi

# Function to generate icon
generate_icon() {
    local input="$1"
    local output="$2"
    local size="$3"
    mkdir -p "$(dirname "$output")"
    convert "$input" -resize "${size}x${size}" "$output"
    echo "Generated $output (${size}x${size})"
}

# Android densities
ANDROID_DENSITIES=(
    "mdpi:48"
    "hdpi:72"
    "xhdpi:96"
    "xxhdpi:144"
    "xxxhdpi:192"
)

# Generate for app (main Android app)
echo "Generating Android icons for app..."
for density in "${ANDROID_DENSITIES[@]}"; do
    IFS=':' read -r name size <<< "$density"
    generate_icon "$LOGO" "app/src/main/res/mipmap-${name}/ic_launcher.png" "$size"
done

# Generate for app variants
VARIANTS=("todo" "quicknote" "linkbox" "share_into")
echo "Generating Android variant icons for app..."
for variant in "${VARIANTS[@]}"; do
    for density in "${ANDROID_DENSITIES[@]}"; do
        IFS=':' read -r name size <<< "$density"
        generate_icon "$LOGO" "app/src/main/res/mipmap-${name}/ic_launcher_${variant}.png" "$size"
    done
done

# iOS icon sizes (selected common ones)
IOS_SIZES=(
    "20x20"
    "29x29"
    "40x40"
    "58x58"
    "60x60"
    "76x76"
    "80x80"
    "87x87"
    "120x120"
    "152x152"
    "167x167"
    "180x180"
    "1024x1024"
)

# Create iOS Assets.xcassets/AppIcon.appiconset
IOS_ICONSET="iosApp/iosApp/Assets.xcassets/AppIcon.appiconset"
mkdir -p "$IOS_ICONSET"

# Generate Contents.json for iOS
cat > "$IOS_ICONSET/Contents.json" << 'EOF'
{
  "images" : [
    {
      "filename" : "Icon-20.png",
      "idiom" : "iphone",
      "scale" : "2x",
      "size" : "20x20"
    },
    {
      "filename" : "Icon-20@3x.png",
      "idiom" : "iphone",
      "scale" : "3x",
      "size" : "20x20"
    },
    {
      "filename" : "Icon-29.png",
      "idiom" : "iphone",
      "scale" : "1x",
      "size" : "29x29"
    },
    {
      "filename" : "Icon-29@2x.png",
      "idiom" : "iphone",
      "scale" : "2x",
      "size" : "29x29"
    },
    {
      "filename" : "Icon-29@3x.png",
      "idiom" : "iphone",
      "scale" : "3x",
      "size" : "29x29"
    },
    {
      "filename" : "Icon-40.png",
      "idiom" : "iphone",
      "scale" : "2x",
      "size" : "40x40"
    },
    {
      "filename" : "Icon-40@3x.png",
      "idiom" : "iphone",
      "scale" : "3x",
      "size" : "40x40"
    },
    {
      "filename" : "Icon-60@2x.png",
      "idiom" : "iphone",
      "scale" : "2x",
      "size" : "60x60"
    },
    {
      "filename" : "Icon-60@3x.png",
      "idiom" : "iphone",
      "scale" : "3x",
      "size" : "60x60"
    },
    {
      "filename" : "Icon-20.png",
      "idiom" : "ipad",
      "scale" : "1x",
      "size" : "20x20"
    },
    {
      "filename" : "Icon-20@2x.png",
      "idiom" : "ipad",
      "scale" : "2x",
      "size" : "20x20"
    },
    {
      "filename" : "Icon-29.png",
      "idiom" : "ipad",
      "scale" : "1x",
      "size" : "29x29"
    },
    {
      "filename" : "Icon-29@2x.png",
      "idiom" : "ipad",
      "scale" : "2x",
      "size" : "29x29"
    },
    {
      "filename" : "Icon-40.png",
      "idiom" : "ipad",
      "scale" : "1x",
      "size" : "40x40"
    },
    {
      "filename" : "Icon-40@2x.png",
      "idiom" : "ipad",
      "scale" : "2x",
      "size" : "40x40"
    },
    {
      "filename" : "Icon-76.png",
      "idiom" : "ipad",
      "scale" : "1x",
      "size" : "76x76"
    },
    {
      "filename" : "Icon-76@2x.png",
      "idiom" : "ipad",
      "scale" : "2x",
      "size" : "76x76"
    },
    {
      "filename" : "Icon-83.5@2x.png",
      "idiom" : "ipad",
      "scale" : "2x",
      "size" : "83.5x83.5"
    },
    {
      "filename" : "Icon-1024.png",
      "idiom" : "ios-marketing",
      "scale" : "1x",
      "size" : "1024x1024"
    }
  ],
  "info" : {
    "author" : "xcode",
    "version" : 1
  }
}
EOF

echo "Generating iOS icons..."
# Note: Simplified, generating only some sizes
generate_icon "$LOGO" "$IOS_ICONSET/Icon-20.png" "20"
generate_icon "$LOGO" "$IOS_ICONSET/Icon-20@2x.png" "40"
generate_icon "$LOGO" "$IOS_ICONSET/Icon-20@3x.png" "60"
generate_icon "$LOGO" "$IOS_ICONSET/Icon-29.png" "29"
generate_icon "$LOGO" "$IOS_ICONSET/Icon-29@2x.png" "58"
generate_icon "$LOGO" "$IOS_ICONSET/Icon-29@3x.png" "87"
generate_icon "$LOGO" "$IOS_ICONSET/Icon-40.png" "40"
generate_icon "$LOGO" "$IOS_ICONSET/Icon-40@2x.png" "80"
generate_icon "$LOGO" "$IOS_ICONSET/Icon-40@3x.png" "120"
generate_icon "$LOGO" "$IOS_ICONSET/Icon-60@2x.png" "120"
generate_icon "$LOGO" "$IOS_ICONSET/Icon-60@3x.png" "180"
generate_icon "$LOGO" "$IOS_ICONSET/Icon-76.png" "76"
generate_icon "$LOGO" "$IOS_ICONSET/Icon-76@2x.png" "152"
generate_icon "$LOGO" "$IOS_ICONSET/Icon-83.5@2x.png" "167"
generate_icon "$LOGO" "$IOS_ICONSET/Icon-1024.png" "1024"

# Desktop icon
echo "Generating desktop icon..."
DESKTOP_ICON="desktopApp/src/main/resources/icon.png"
generate_icon "$LOGO" "$DESKTOP_ICON" "512"

# Web favicon
echo "Generating web favicon..."
WEB_FAVICON="webApp/src/wasmJsMain/resources/favicon.ico"
# For ICO, need multiple sizes
convert "$LOGO" -resize 16x16 -resize 32x32 -resize 48x48 "$WEB_FAVICON"
echo "Generated $WEB_FAVICON"

# Splash screen (higher res, bigger dimension)
echo "Generating splash screen asset..."
SPLASH="Assets/splash.png"
generate_icon "$LOGO" "$SPLASH" "2048"

# Also, for Android splash, perhaps place in drawable
ANDROID_SPLASH="app/src/main/res/drawable/splash.png"
generate_icon "$LOGO" "$ANDROID_SPLASH" "2048"

# Remove legacy vector icons
echo "Removing legacy vector icons..."
rm -f app/src/main/res/drawable-anydpi-v26/ic_launcher*.xml
rm -f app/src/main/res/drawable/ic_launcher*.xml
rm -f app/src/main/ic_launcher*-web.png
rm -f app/src/main/res/drawable/ic_launcher_*_background.xml
rm -f app/src/main/res/drawable/ic_launcher_*_foreground.xml
rm -f app/src/main/res/drawable/ic_launcher_monochrome.xml

echo "Icon generation complete!"