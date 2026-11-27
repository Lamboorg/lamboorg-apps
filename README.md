# Lamboorg Ultra apps

Thin app wrappers around [lamboorg.neocities.org](https://lamboorg.neocities.org): the apps load the live site,
so every site update reaches the apps automatically. Only the wrappers themselves live here.

| | |
|---|---|
| Android | `android/` · full-screen app window, own icon and splash screen, signed with the Lamboorg key |
| Windows / macOS / Linux | `desktop/` · Electron window (installer, DMG, AppImage + .deb) |

Every push to `main` builds everything on GitHub Actions and publishes it as the latest release.
The download page links to `releases/latest/download/<file>`, so the links never change.

**Signing (Android):** the workflow needs two repository secrets, `SIGNING_KEYSTORE_B64` and `SIGNING_PASSWORD`.
Always the same key: Android only installs an update over the old app if it's signed with the same key.
Keep a backup of the keystore somewhere safe; never commit it.
