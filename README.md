## SS coding

<img src="/fastlane/metadata/android/en-US/images/icon.png" alt="SS coding Icon" width="90" height="90" align="left"/>

**SS coding** is a versatile and extensible code editor and terminal environment for Android, featuring syntax highlighting,
LSP-powered code intelligence, a built-in Ubuntu Linux terminal, Git version control, and fast project-wide tools for
efficient editing.

![Android CI](https://github.com/stylesheet48-max/SS-Coding/actions/workflows/android.yml/badge.svg?event=push&style=for-the-badge)

---

## Download

- **Latest Stable Release**: Download from [GitHub Releases](https://github.com/stylesheet48-max/SS-Coding/releases)
- **Nightly Actions Builds**: Download from [GitHub Actions](https://github.com/stylesheet48-max/SS-Coding/actions/)

---

## Screenshots

<div>
  <img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/01.jpg" width="32%" />
  <img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/02.jpg" width="32%" />
  <img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/03.jpg" width="32%" />
</div>
<div>
  <img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/04.jpg" width="32%" />
  <img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/05.jpg" width="32%" />
  <img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/06.jpg" width="32%" />
</div>

---

## Build the Project

The application is built for Android with target SDK 36:

| Flavor      | applicationId           | Extensions module | Notes                                                    |
| ----------- | ----------------------- | ----------------- | -------------------------------------------------------- |
| `community` | `com.stylesheet.editor` | included          | Community flavor. Direct APK distribution & updates.     |
| `playstore` | `com.stylesheet.editor` | excluded          | Google Play build; Google Play Policy compliant.         |

The `community` flavor is the default flavor (used by CI and by tooling that needs a default
variant). Build a specific flavor with a flavor-qualified task; the unqualified aggregate tasks such
as `assembleDebug` build every flavor.

### Detecting the flavour at runtime

The flavour is declared once per product flavour in `app/build.gradle.kts` as the
`BuildConfig.FLAVOUR` field, and each flavour's `XedApplication` publishes it during startup. Library
modules cannot read the application module's `BuildConfig`, so they branch on the shared
`com.rk.app.AppFlavour` instead:

```kotlin
// True when this build bundles the extensions module and the user has not disabled it.
if (AppFlavour.current.bundlesExtensions && FeatureRegistry.isEnabled("enable_extension")) {
    // e.g. show the theme/icon-pack/extension entries
}
```

Unknown or uninitialized flavours fall back to `AppFlavour.COMMUNITY`, so previews and unit tests
still work.

### Bundled terminal rootfs (playstore)

The `playstore` flavour bundles the arm64 Ubuntu rootfs at
`app/src/playstore/assets/arm64-v8a/ubuntu.rootfs`, so the terminal sets up without downloading it.
That flavour therefore supports `arm64-v8a` devices only and shows an error dialog on other ABIs; the
`community` flavour downloads the matching rootfs from the network.

Choose one of the following build methods:

<details>
<summary><strong>Option 1: Build Locally</strong></summary>

Build the **debug APK** (signed with the included test key):

```bash
./gradlew assembleCommunityDebug
```

The compiled APK will be located at `app/build/outputs/apk/community/debug/app-community-debug.apk`.

To build the Play Store variant instead:

```bash
./gradlew assemblePlaystoreRelease
```

</details>

<details>
<summary><strong>Option 2: Build with Docker</strong></summary>

If you don't have the Android SDK or JDK 21 installed locally, you can build the project in a Docker
container:

```bash
DOCKER_BUILDKIT=1 docker build --target export-stage --output ./out .
```

The generated debug APK will be located at `out/community/debug/app-community-debug.apk`.

</details>

---

## Contributing

We welcome contributions! Please read our [Contributing Guidelines](CONTRIBUTING.md) to
learn how you can get involved, submit bug reports, suggest features, or open pull requests.

---

## Privacy Policy

User privacy is paramount. Please review our [Privacy Policy](PRIVACY_POLICY.md) for details on how permissions and data are handled.

---

## License & Attribution

This project is licensed under the **GNU General Public License v3.0 (GPL-3.0)**.  
See the [LICENSE](LICENSE) file for the full license text.

- **Project:** SS coding
- **Maintainer:** stylesheet48-max (<stylesheet48@gmail.com>)
- **Repository:** [https://github.com/stylesheet48-max/SS-Coding](https://github.com/stylesheet48-max/SS-Coding)
- **Attribution:** Based on [Xed-Editor](https://github.com/Xed-Editor/Xed-Editor) by Rohit Kushvaha and contributors (licensed under GPL-3.0).
