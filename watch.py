import os
import sys
import time
import subprocess

WATCH_DIRS = [
    os.path.join("app", "src"),
    os.path.join("app"),
]
GRADLE_CMD = "./gradlew"
INSTALL_TASK = "installDebug"
PACKAGE_NAME = "com.citecircle.app"
MAIN_ACTIVITY = "com.citecircle.app.MainActivity"

def get_mtimes():
    mtimes = {}
    for watch_dir in WATCH_DIRS:
        if not os.path.exists(watch_dir):
            continue
        for root, dirs, files in os.walk(watch_dir):
            # Skip build directories to avoid infinite build loops
            if "build" in root or "bin" in root or "out" in root:
                continue
            for f in files:
                if f.endswith(".kt") or f.endswith(".xml") or f.endswith(".gradle.kts"):
                    path = os.path.join(root, f)
                    try:
                        mtimes[path] = os.path.getmtime(path)
                    except OSError:
                        pass
    return mtimes

def main():
    print("Starting build watcher for CiteCircle...")
    print(f"Watching directories: {', '.join(WATCH_DIRS)}")
    last_mtimes = get_mtimes()
    
    while True:
        time.sleep(1.5)
        current_mtimes = get_mtimes()
        
        changed = False
        if len(current_mtimes) != len(last_mtimes):
            changed = True
        else:
            for path, mtime in current_mtimes.items():
                if path not in last_mtimes or mtime > last_mtimes[path]:
                    changed = True
                    break
        
        if changed:
            print("\nChange detected! Recompiling and redeploying to emulator...")
            sys.stdout.flush()
            try:
                # Run installDebug task
                process = subprocess.Popen(
                    [GRADLE_CMD, INSTALL_TASK],
                    stdout=subprocess.PIPE,
                    stderr=subprocess.STDOUT,
                    text=True
                )
                
                # Print output in real-time
                while True:
                    line = process.stdout.readline()
                    if not line:
                        break
                    print(f"  [gradle] {line.strip()}")
                    sys.stdout.flush()
                
                process.wait()
                
                if process.returncode == 0:
                    print("Build and install successful! Launching app...")
                    subprocess.run([
                        "adb", "shell", "am", "start", "-n",
                        f"{PACKAGE_NAME}/{MAIN_ACTIVITY}"
                    ])
                    print("App launched successfully on emulator.")
                else:
                    print(f"Build failed with exit code {process.returncode}.")
            except Exception as e:
                print(f"Error executing build: {e}")
            
            sys.stdout.flush()
            last_mtimes = current_mtimes

if __name__ == "__main__":
    main()
