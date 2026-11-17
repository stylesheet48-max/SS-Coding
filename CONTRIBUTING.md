# Contributing to SS coding

Thank you for your interest in contributing to **SS coding**! We appreciate community contributions to help improve and enhance this open-source code editor for Android.

---

## 1. Code of Conduct & Etiquette

- Be polite, considerate, and constructive in all interactions.
- Respect the diverse backgrounds, skill levels, and perspectives of other contributors.
- Keep discussions relevant to the project and issue topics.

---

## 2. How Can I Contribute?

### Reporting Bugs
If you find a bug:
1. Check the [Issues tab](https://github.com/stylesheet48-max/SS-Coding/issues) to ensure it hasn't already been reported.
2. If it's a new issue, open a new bug report with:
   - A clear and descriptive title.
   - Exact steps to reproduce the bug.
   - Your Android version, device model, and SS coding version.
   - Relevant log outputs or screenshots if available.

### Suggesting Enhancements
1. Open a new issue under the **Enhancement** or **Feature Request** label.
2. Explain the problem your proposed feature solves and why it would benefit users.

### Submitting Translations
Translations are always welcome! You can contribute new translations or improve existing language strings in `core/resources/src/main/res/values-*/strings.xml` by submitting a pull request.

---

## 3. Development Setup & Building

### Prerequisites
- JDK 17 or JDK 21
- Android Studio Ladybug / Meerkat or later
- Android SDK Platform 36 and Android NDK (r27 or newer)
- Git

### Build Instructions
1. Fork and clone the repository:
   ```bash
   git clone --recursive https://github.com/<your-username>/SS-Coding.git
   cd SS-Coding
   ```
2. Build the Community Debug APK:
   ```bash
   ./gradlew assembleCommunityDebug
   ```
   The generated APK will be output to `app/build/outputs/apk/community/debug/app-community-debug.apk`.

3. Build the Play Store Release variant:
   ```bash
   ./gradlew assemblePlaystoreRelease
   ```

---

## 4. Code Formatting & Quality Standards

To maintain clean and readable code across the project, please adhere to these formatting guidelines:

- **Kotlin:** Follow standard Kotlin style guidelines formatted with `ktfmt`.
  Run the formatting task before committing:
  ```bash
  ./gradlew ktfmtFormat
  ```
- **Automatic Pre-commit Hook:**
  You can enable automatic code formatting on commit:
  ```bash
  git config core.hooksPath .githooks
  ```
- **XML:** Standard Android Studio XML layout formatting with 4 spaces indent.
- **Commit Messages:** Use clear, concise commit messages following Conventional Commits (e.g., `feat: ...`, `fix: ...`, `docs: ...`, `refactor: ...`).

---

## 5. Proposing a Change (Pull Requests)

We follow the standard **GitHub Flow**:

1. Fork the repository on GitHub.
2. Create a topic branch from `main`:
   ```bash
   git checkout -b feature/my-feature-name
   ```
3. Implement your changes, test thoroughly, and ensure code formatting passes.
4. Commit your changes:
   ```bash
   git commit -m "feat: add support for XYZ"
   ```
5. Push to your fork:
   ```bash
   git push origin feature/my-feature-name
   ```
6. Open a Pull Request against the `main` branch of [stylesheet48-max/SS-Coding](https://github.com/stylesheet48-max/SS-Coding).
7. Describe the changes, motivations, and reference any open issues fixed.

---

## 6. License & Legal

By contributing to SS coding, you agree that your contributions will be licensed under the **GNU General Public License v3.0 (GPL-3.0)**.  
See the [LICENSE](LICENSE) file for more information.
