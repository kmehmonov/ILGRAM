#!/usr/bin/env bash
# ILGRAM CI preparation. Must be *sourced* (". ilgram/ci/prepare.sh") so the exports reach Gradle.
# Secrets come from GitHub Actions; nothing secret is ever committed to the repository.

# 1) Release signing key (stable across builds so MDM can update the app in place)
if [ -n "${ILGRAM_KEYSTORE_B64:-}" ]; then
  echo "$ILGRAM_KEYSTORE_B64" | base64 -d > TMessagesProj/config/release.keystore
  export ORG_GRADLE_PROJECT_RELEASE_STORE_PASSWORD="$ILGRAM_KEYSTORE_PASSWORD"
  export ORG_GRADLE_PROJECT_RELEASE_KEY_PASSWORD="$ILGRAM_KEY_PASSWORD"
  export ORG_GRADLE_PROJECT_RELEASE_KEY_ALIAS="$ILGRAM_KEY_ALIAS"
  echo "ILGRAM: signing with the ILGRAM release key"
else
  echo "::warning::ILGRAM_KEYSTORE_B64 secret is missing - APK is signed with the public dummy key (not for distribution)"
fi

# 2) Telegram API credentials
if [ -z "${ILGRAM_API_ID:-}" ] || [ -z "${ILGRAM_API_HASH:-}" ]; then
  echo "::warning::ILGRAM_API_ID / ILGRAM_API_HASH secrets are missing - the APK builds, but Telegram login will fail (API_ID_INVALID). Get them at https://my.telegram.org"
fi

# 3) School configuration (allowed group / user IDs). Kept in a secret because user IDs are personal
#    data and this repository is public. Format: the same key=value lines as assets/ilgram.properties.
if [ -n "${ILGRAM_CONFIG:-}" ]; then
  printf '%s\n' "$ILGRAM_CONFIG" > TMessagesProj/src/main/assets/ilgram.properties
  echo "ILGRAM: school configuration injected from the ILGRAM_CONFIG secret ($(grep -c '=' TMessagesProj/src/main/assets/ilgram.properties) keys)"
else
  echo "::warning::ILGRAM_CONFIG secret is missing - the APK uses the default config (no allowed groups/users yet)"
fi

# 4) Version: Telegram base version code * 1000 + CI run number (monotonic, so MDM updates work)
BASE_CODE=$(grep '^APP_VERSION_CODE=' gradle.properties | cut -d= -f2)
BASE_NAME=$(grep '^APP_VERSION_NAME=' gradle.properties | cut -d= -f2)
RUN=${GITHUB_RUN_NUMBER:-0}
export ORG_GRADLE_PROJECT_APP_VERSION_CODE=$(( BASE_CODE * 1000 + RUN % 1000 ))
export ORG_GRADLE_PROJECT_APP_VERSION_NAME="${BASE_NAME}-ilgram.${RUN}"
echo "ILGRAM: versionName=${ORG_GRADLE_PROJECT_APP_VERSION_NAME} versionCode(base)=${ORG_GRADLE_PROJECT_APP_VERSION_CODE}"
