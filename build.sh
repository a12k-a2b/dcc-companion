#!/bin/zsh
# Builds dcc-companion.apk without Gradle, using SDK build-tools directly
# (same pattern as dc1-keys). Signs with the CLUB key — the Companion is
# the club's own dish, and club-signed dishes update cleanly over each
# other. The key lives in the club repo; its password is public by design.
set -e

PROJ=~/code/dcc-companion
APP=$PROJ/app
OUT=$PROJ/build
SDK=~/Library/Android/sdk
BT=$SDK/build-tools/35.0.0
PLATFORM=$SDK/platforms/android-35/android.jar
CLUB_KEY=~/code/daylight-computing-club-community-hub/signing/dcc.keystore
export JAVA_HOME=~/code/dc1-keys/tools/jdk-17.0.19+10/Contents/Home
export PATH=$JAVA_HOME/bin:$PATH

rm -rf $OUT
mkdir -p $OUT/compiled $OUT/gen $OUT/classes

# 1. Compile and link resources
$BT/aapt2 compile --dir $APP/res -o $OUT/compiled/res.zip
$BT/aapt2 link -o $OUT/unsigned.apk \
    -I $PLATFORM \
    --manifest $APP/AndroidManifest.xml \
    --java $OUT/gen \
    $OUT/compiled/res.zip

# 2. Compile Java
javac -source 11 -target 11 -classpath $PLATFORM \
    -d $OUT/classes \
    $OUT/gen/club/daylightcomputer/companion/*.java \
    $APP/src/club/daylightcomputer/companion/*.java

# 3. Dex
$BT/d8 --release --lib $PLATFORM --output $OUT $OUT/classes/club/daylightcomputer/companion/*.class

# 4. Package dex into APK
cd $OUT && zip -q -j unsigned.apk classes.dex

# 5. Align + sign with the club key
$BT/zipalign -f 4 $OUT/unsigned.apk $OUT/dcc-companion.apk
$BT/apksigner sign --ks $CLUB_KEY --ks-pass pass:android \
    --key-pass pass:android $OUT/dcc-companion.apk

echo "Built: $OUT/dcc-companion.apk"
