# Build setup — from a blank machine to a compiling project

Read this **before** `START_HERE.md`. It gets the toolchain onto your computer. If your machine is
already set up for Android work, skip to `START_HERE.md`.

Everything here is a one-time cost. Budget an afternoon, most of it downloading.

---

## What you need

| Thing | Why | Size |
| --- | --- | --- |
| Android Studio | SDK, emulator, Gradle, a bundled JDK 17 | ~1.2 GB installer, ~8 GB on disk after SDK |
| Node.js 18+ | to install Claude Code | ~30 MB |
| Git | to push to `BonnieMundia/cleared` | ~50 MB |
| An Android phone, API 26+ (Android 8.0 or newer) | to run the thing | — |

**On bandwidth.** The first Gradle build downloads another 300–500 MB of dependencies. Do the
install and the first `./gradlew assembleDebug` on unmetered wifi if you can. After that, day-to-day
builds are offline — Gradle caches everything, and the app itself needs no network.

---

## 1. Git

**Windows** — [git-scm.com/download/win](https://git-scm.com/download/win), accept the defaults.
**macOS** — `xcode-select --install` (git comes with the command line tools), or `brew install git`.
**Linux** — `sudo apt install git` / your distro's equivalent.

Then, once:

```bash
git config --global user.name "Bonnie Mundia"
git config --global user.email "your@email.com"
```

Use the email your GitHub account uses, or your commits won't link to your profile.

---

## 2. Node.js and Claude Code

Install Node 18 or newer from [nodejs.org](https://nodejs.org) (the LTS build). Then:

```bash
npm install -g @anthropic-ai/claude-code
claude
```

The first `claude` run opens a browser to log in. If `npm install -g` fails with a permissions error
on macOS or Linux, don't reach for `sudo` — install Node via
[nvm](https://github.com/nvm-sh/nvm) instead and retry.

---

## 3. Android Studio

Download from [developer.android.com/studio](https://developer.android.com/studio) and run the
installer. On first launch pick the **Standard** setup — it installs the SDK, platform tools, and an
emulator image without asking you anything.

Then confirm the pieces this project needs. **Settings → Languages & Frameworks → Android SDK**:

- **SDK Platforms** tab: check **Android 15 (API 35)** — the compile target.
- **SDK Tools** tab: **Android SDK Build-Tools**, **Android SDK Platform-Tools**, **Android SDK
  Command-line Tools**. All three, latest versions.

Apply, let it download.

### Put `adb` on your PATH

You need `adb` from a terminal to talk to the phone.

**macOS / Linux** — add to `~/.zshrc` or `~/.bashrc`:

```bash
export ANDROID_HOME="$HOME/Library/Android/sdk"        # macOS
# export ANDROID_HOME="$HOME/Android/Sdk"              # Linux
export PATH="$PATH:$ANDROID_HOME/platform-tools"
```

**Windows** — the SDK lives at `%LOCALAPPDATA%\Android\Sdk`. Add
`%LOCALAPPDATA%\Android\Sdk\platform-tools` to your user PATH (Settings → search "environment
variables" → Path → Edit → New).

Open a **new** terminal and check:

```bash
adb version
java -version     # should say 17.x
```

If `java` is missing or the wrong version, point `JAVA_HOME` at the JDK Android Studio bundled:
`/Applications/Android Studio.app/Contents/jbr/Contents/Home` on macOS,
`C:\Program Files\Android\Android Studio\jbr` on Windows.

---

## 4. The phone

1. **Settings → About phone → tap Build number seven times.** You'll get a toast saying you're now a
   developer.
2. **Settings → System → Developer options** — turn on **USB debugging**. Turn on **Stay awake**
   too; it saves you a lot of unlocking.
3. Plug the phone into the computer with a **data** cable. Charge-only cables are a classic hour lost.
4. The phone shows an "Allow USB debugging?" dialog with a fingerprint. Tick **Always allow** and
   accept.
5. Check the computer sees it:

```bash
adb devices
```

You want one line ending in `device`. If it says `unauthorized`, the dialog on the phone wasn't
accepted. If the list is empty: try a different cable, a different port, and on the phone switch USB
mode from "Charging" to "File transfer".

**Windows only:** you may need the OEM USB driver for your phone brand —
[developer.android.com/studio/run/oem-usb](https://developer.android.com/studio/run/oem-usb).

### Check the phone's API level

Settings → About phone → Android version. **8.0 or higher** works. Below that, tell me and I'll
adjust the spec — nothing in this design needs API 26, it's just the sensible floor.

### No cable? Wireless debugging

Android 11+ can pair over wifi. Developer options → **Wireless debugging** → Pair device with
pairing code, then on the computer:

```bash
adb pair <ip>:<port>      # the port from the pairing dialog
adb connect <ip>:<port>   # the port from the main Wireless debugging screen
```

Both devices must be on the same network.

---

## 5. Verify the whole chain

Before touching Cleared, prove the toolchain works end to end. In Android Studio: **New Project →
Empty Activity**, name it `throwaway`, finish, wait for Gradle, then hit **Run** with your phone
connected. A blank white screen saying "Hello Android" on the phone means everything above is
correct.

Delete it. Then go to `START_HERE.md`.

---

## If you don't have a computer to hand

You can get an APK without ever running Gradle locally, but you can't practically *develop* this way.

**Cloud build.** Push the project to GitHub and let Actions compile it:

> Add a GitHub Actions workflow that runs `./gradlew assembleDebug` on every push to main and
> uploads the APK as a build artifact.

Every push then leaves a downloadable APK on the repo's **Actions** tab. Open that page in the phone
browser, download the artifact, unzip, tap the APK, allow "install unknown apps" for your browser
once. Slow loop — five to ten minutes per change — but it works from the phone alone.

**GitHub Codespaces** gives you a real Linux machine in the browser with a 60-hour monthly free
allowance; Claude Code runs in it fine and it can build the APK. It cannot install to a phone over
USB, so pair it with the Actions workflow above. Editing code in a phone browser is genuinely
unpleasant, so treat this as a fallback, not the plan.

---

## Known-good versions

The spec was written against these. Newer is usually fine; if something breaks in a way that smells
like tooling, pin to these.

```
Android Studio     Ladybug or newer
Kotlin             2.0+
compileSdk         35
minSdk             26
JDK                17
Gradle             8.7+
AGP                8.5+
Compose BOM        2024.09.00 or newer
```
