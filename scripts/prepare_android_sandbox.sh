#!/usr/bin/env bash
#
# Build a self-contained, offline-ready Alpine ARM64 sandbox for ALTIVE DEV AI.
# Customers do not need Termux, PowerShell, apk add, or programming knowledge.
# Run AFTER deps/build_proot.sh; CI uses docker/setup-qemu-action for arm64.
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
ASSETS_DIR="$PROJECT_ROOT/src/android/app/src/main/assets"
ALPINE_VERSION="3.21"
ALPINE_RELEASE="3.21.3"
ALPINE_URL="https://dl-cdn.alpinelinux.org/alpine/v${ALPINE_VERSION}/releases/aarch64/alpine-minirootfs-${ALPINE_RELEASE}-aarch64.tar.gz"
ROOTFS_FILE="$ASSETS_DIR/alpine-minirootfs.tar.gz"
PROOT_FILE="$ASSETS_DIR/proot-aarch64"
PACKAGES=(nodejs npm python3 py3-pip git bash curl wget unzip zip ca-certificates)

mkdir -p "$ASSETS_DIR"
test -s "$PROOT_FILE" || { echo "::error::Build PRoot before preparing the sandbox" >&2; exit 1; }
command -v docker >/dev/null || { echo "::error::Docker is required for reproducible ARM64 toolchain packaging" >&2; exit 1; }

STAGING="$(mktemp -d)"
trap 'rm -rf "$STAGING"' EXIT
mkdir -p "$STAGING/rootfs"
echo "Downloading Alpine ${ALPINE_RELEASE} aarch64 minirootfs..."
curl --fail --location --retry 3 --output "$STAGING/base.tar.gz" "$ALPINE_URL"
tar -xzf "$STAGING/base.tar.gz" -C "$STAGING/rootfs"

# Run the Alpine APK manager IN the arm64 guest, never on the Android host.
# apk verifies repository package signatures. CI should have configured QEMU.
# The resulting packages are shipped with the APK: a fresh install is offline-ready.
echo "Provisioning verified ARM64 programming tools: ${PACKAGES[*]}"
docker run --rm --platform linux/arm64 \
  --mount "type=bind,source=$STAGING/rootfs,target=/altiv-rootfs" \
  alpine:3.21 /bin/sh -euc '
    cp /etc/resolv.conf /altiv-rootfs/etc/resolv.conf
    chroot /altiv-rootfs /sbin/apk add --no-cache \
      nodejs npm python3 py3-pip git bash curl wget unzip zip ca-certificates
    chroot /altiv-rootfs /bin/sh -ec "
      node --version
      npm --version
      npx --version
      python3 --version
      python3 -m pip --version
      git --version
      bash --version | head -n 1
      node -e \"console.log(12345)\" | grep -qx 12345
      python3 -c \"print(67890)\" | grep -qx 67890
    "
    rm -f /altiv-rootfs/etc/resolv.conf
  '

# RootfsManager.extractTar understands POSIX ustar + gzip; PAX/GNU longname
# extension entries would be ignored and silently lose executable names.
tar --format=ustar --numeric-owner --owner=0 --group=0 \
  -czf "$ROOTFS_FILE" -C "$STAGING/rootfs" .
test -s "$ROOTFS_FILE"
# Avoid SIGPIPE under pipefail: grep must not terminate a streaming tar.
tar -tzf "$ROOTFS_FILE" > "$STAGING/rootfs-content.list"
for name in node npm npx python3 git bash apk; do
  if ! grep -Eq "(^|/)(usr/bin|bin|sbin)/$name$" "$STAGING/rootfs-content.list"; then
    echo "::error::Required ARM64 binary missing from rootfs: $name" >&2
    exit 1
  fi
done
echo "✓ Offline-ready Alpine ARM64 sandbox: $(du -h "$ROOTFS_FILE" | cut -f1)"
echo "✓ PRoot binary: $PROOT_FILE"
