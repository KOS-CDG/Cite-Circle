# 🔄 CiteCircle Auto-Build Watcher (`watch.py`)

A lightweight developer daemon script that monitors the codebase for file changes, compiles them automatically, installs the update on a connected emulator/device, and brings the app back to focus.

---

## How It Works

The `watch.py` daemon runs a continuous loop (polling every 1.5 seconds) checking the last modification times of source files. When changes are detected:
1. It triggers the Gradle compilation and install task (`./gradlew installDebug`).
2. It streams build logs to the console.
3. If successful, it launches the application on the active emulator/device via the Android Debug Bridge (`adb`).

---

## File Types Monitored
- Kotlin source files (`.kt`)
- Android layout and resources (`.xml`)
- Gradle configurations (`.gradle.kts`)

*Note: The script automatically ignores `build`, `bin`, and `out` directories to avoid infinite build loops.*

---

## Usage

### 1. Prerequisites
- **Emulator/Device**: Ensure you have an Android emulator running or a physical device connected via USB with Developer Mode and USB Debugging enabled.
- **ADB Command**: Ensure `adb` is installed and registered in your system path. Verify by running `adb devices`.

### 2. Launch the Watcher
Run the Python script from the root directory of the project:
```bash
python3 watch.py
```

### 3. Log Output Example
```text
Starting build watcher for CiteCircle...
Watching directories: app/src, app

Change detected! Recompiling and redeploying to emulator...
  [gradle] > Task :app:compileDebugKotlin FROM-CACHE
  [gradle] > Task :app:installDebug
Build and install successful! Launching app...
App launched successfully on emulator.
```
