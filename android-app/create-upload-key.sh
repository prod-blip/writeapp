#!/bin/zsh
set -euo pipefail

SCRIPT_DIR=${0:A:h}
KEYSTORE_PATH="$SCRIPT_DIR/clearwrite-upload.jks"
PROPERTIES_PATH="$SCRIPT_DIR/keystore.properties"

if [[ -e "$KEYSTORE_PATH" || -e "$PROPERTIES_PATH" ]]; then
  echo "Signing files already exist. Nothing was changed."
  echo "Back them up securely; do not create a second upload key unless you intend to replace the first."
  exit 1
fi

read -s "UPLOAD_PASSWORD?Create a private upload-key password (12+ characters): "
echo
if (( ${#UPLOAD_PASSWORD} < 12 )); then
  echo "Password must contain at least 12 characters."
  exit 1
fi
read -s "UPLOAD_PASSWORD_CONFIRM?Enter it again: "
echo
if [[ "$UPLOAD_PASSWORD" != "$UPLOAD_PASSWORD_CONFIRM" ]]; then
  echo "Passwords did not match."
  exit 1
fi

keytool -genkeypair \
  -keystore "$KEYSTORE_PATH" \
  -storepass "$UPLOAD_PASSWORD" \
  -keypass "$UPLOAD_PASSWORD" \
  -alias clearwrite-upload \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000 \
  -dname "CN=ClearWrite Upload, O=ClearWrite, C=IN"

umask 077
UPLOAD_PASSWORD_BASE64=$(printf '%s' "$UPLOAD_PASSWORD" | base64)
{
  printf 'storeFile=clearwrite-upload.jks\n'
  printf 'storePasswordBase64=%s\n' "$UPLOAD_PASSWORD_BASE64"
  printf 'keyAlias=clearwrite-upload\n'
  printf 'keyPasswordBase64=%s\n' "$UPLOAD_PASSWORD_BASE64"
} > "$PROPERTIES_PATH"

unset UPLOAD_PASSWORD UPLOAD_PASSWORD_CONFIRM UPLOAD_PASSWORD_BASE64
echo "Upload key created. Back up clearwrite-upload.jks and the password in a password manager."
