#!/bin/bash
# sign-app.sh - Sign all binaries in a macOS .app bundle for notarization.
#
# Apple notarization requires that EVERY Mach-O binary (dylib, executable)
# is signed with a Developer ID certificate, hardened runtime, and a secure
# timestamp.  codesign --deep does NOT reach inside JAR files and can miss
# nested binaries, so we sign everything individually, inside-out.
#
# Usage: sign-app.sh <app-path> <signing-identity> <entitlements-plist>

set -euo pipefail

APP_PATH="$1"
IDENTITY="$2"
ENTITLEMENTS="$3"

echo "=== Signing app bundle: $APP_PATH ==="
echo "    Identity: $IDENTITY"

# Sign a dylib (no entitlements needed for libraries)
sign_lib() {
    echo "  sign lib: $(echo "$1" | sed "s|$APP_PATH/||")"
    codesign --force --options runtime --timestamp \
        --sign "$IDENTITY" "$1"
}

# Sign an executable (with entitlements for JIT, unsigned memory, etc.)
sign_exec() {
    echo "  sign exe: $(echo "$1" | sed "s|$APP_PATH/||")"
    codesign --force --options runtime --timestamp \
        --entitlements "$ENTITLEMENTS" \
        --sign "$IDENTITY" "$1"
}

# --- Step 1: Sign dylibs inside JOGL native JAR files ---
echo "--- Step 1: JOGL native JARs ---"
for jar in "$APP_PATH"/Contents/app/*-natives-*.jar; do
    [ -f "$jar" ] || continue
    echo "  Processing: $(basename "$jar")"
    tmpdir=$(mktemp -d)
    unzip -q "$jar" -d "$tmpdir"
    find "$tmpdir" -name "*.dylib" | while read -r dylib; do
        sign_lib "$dylib"
    done
    # Repack: remove old JAR, create new one from extracted contents
    rm "$jar"
    (cd "$tmpdir" && zip -qr "$jar" .)
    rm -rf "$tmpdir"
done

# --- Step 2: Sign all Mach-O binaries in the JRE runtime ---
echo "--- Step 2: JRE runtime binaries ---"
find "$APP_PATH/Contents/runtime" -type f | while read -r f; do
    # Use the file command to detect Mach-O binaries
    if file "$f" | grep -q "Mach-O"; then
        sign_lib "$f"
    fi
done

# --- Step 3: Sign native executables bundled in Contents/app ---
echo "--- Step 3: Native executables (probe, suitename, etc.) ---"
for bin in "$APP_PATH"/Contents/app/*; do
    [ -f "$bin" ] || continue
    if file "$bin" | grep -q "Mach-O"; then
        sign_exec "$bin"
    fi
done

# --- Step 4: Sign the main application launcher ---
echo "--- Step 4: Main launcher ---"
sign_exec "$APP_PATH/Contents/MacOS/KiNG"

# --- Step 5: Sign the overall .app bundle ---
echo "--- Step 5: App bundle ---"
sign_exec "$APP_PATH"

echo "=== Signing complete ==="

# Verify
echo "--- Verifying signature ---"
codesign --verify --deep --strict "$APP_PATH"
echo "Signature verified OK."
