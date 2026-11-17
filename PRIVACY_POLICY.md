# Privacy Policy for SS coding

**Effective Date:** November 17, 2026  
**Application Name:** SS coding  
**Application ID:** `com.stylesheet.editor`  
**Developer Contact:** stylesheet48@gmail.com  

---

## 1. Introduction & Overview

**SS coding** ("we", "our", or "the application") is a fully featured, open-source code editor, Linux terminal emulator, and Git client for Android, distributed under the GNU General Public License v3.0 (GPL-3.0).

We are deeply committed to user privacy. **SS coding operates under a strict privacy-first model: we do not collect, store, sell, or share your personal data, code, files, or telemetry.**

---

## 2. Information We Do NOT Collect

1. **No Personal Identifiable Information (PII):** We do not collect names, phone numbers, contacts, device identifiers (IMEI/MAC address), or biometric data.
2. **No Analytics or Telemetry:** SS coding contains zero third-party analytics trackers, zero advertising SDKs, and zero background usage monitoring.
3. **No Code or File Scanning:** The files you create, edit, clone, or delete on your device are 100% private to you. They are never uploaded to any remote server operated by us.

---

## 3. Permissions Requested & Why They Are Needed

To function as a complete software development environment, SS coding requests specific Android permissions. Every requested permission is strictly used for user-directed actions:

### 3.1 All Files Access (`MANAGE_EXTERNAL_STORAGE`)
- **Purpose:** Allows users to open, edit, compile, search, and save source code projects anywhere in device storage (e.g., project directories, Git repositories, SD cards).
- **Scope:** Used exclusively in direct response to user file navigation and editing.

### 3.2 Foreground Service (`FOREGROUND_SERVICE` & `FOREGROUND_SERVICE_SPECIAL_USE`)
- **Purpose:** Keeps long-running compilation tasks, package installations (e.g., `apt`, `npm`, `pip`), and interactive terminal shell sessions active in the background when you switch applications or lock your device.
- **Notification:** A persistent system notification is displayed whenever a terminal session is active.

### 3.3 Internet Access (`INTERNET`)
- **Purpose:** Used strictly for user-initiated network actions:
  - Cloning and pushing Git repositories (`git clone`, `git push`).
  - Terminal network commands (e.g., `curl`, `apt update`, `ping`).
  - Optional AI chat completions using the user's own supplied API key.
  - Checking for software updates via public GitHub releases.
- **Security:** All application-level remote network requests strictly enforce TLS 1.3 / HTTPS encryption. Cleartext HTTP traffic is disabled by default.

### 3.4 Notifications (`POST_NOTIFICATIONS`)
- **Purpose:** Notifies you of completed compilation jobs, terminal session status, and in-app update availability on Android 13+.

---

## 4. Third-Party Services & User-Configured Integrations

- **GitHub (Optional):** When checking for app updates, stargazers counts, or reporting crash issues, the app queries the public GitHub REST API. No authentication tokens are transmitted unless you explicitly configure a Git Personal Access Token for your own private repositories.
- **AI Providers (Optional):** If you use the AI assistant feature, prompts and code excerpts are transmitted directly from your device to the AI provider endpoint you configure (e.g., OpenAI, Anthropic, or local Ollama). SS coding does not operate an intermediate proxy server and never logs your API keys or prompts.

---

## 5. Children's Privacy

SS coding is a developer productivity tool and does not knowingly collect any data from children under the age of 13.

---

## 6. Data Security & Storage

All user data, preferences, syntax settings, and local extension packages are stored locally on your device in protected app-private storage or user-selected folders. Uninstalling the app removes all app-private data.

---

## 7. Open Source License & Source Code

SS coding is free and open-source software licensed under the **GNU General Public License v3.0**. You can inspect the complete source code, audit its network behavior, and review licensing notices at:  
`https://github.com/stylesheet48-max/SS-Coding`

---

## 8. Contact Information

If you have any questions, concerns, or inquiries regarding this Privacy Policy, please contact the maintainer:

- **Email:** stylesheet48@gmail.com
- **Repository:** https://github.com/stylesheet48-max/SS-Coding/issues
