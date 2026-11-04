# Steam Workshop Deployment Suite for Songs of Syx

This directory provides a **zero-credential, privacy-first deployment pipeline** for publishing and updating **Syx Auto-Planner** and **Syx Architect** on the Steam Workshop (AppID `1162750`).

---

## 🔒 Security & Privacy Guarantee

- **No Passwords or Keys**: SteamCMD handles your password and Steam Guard 2FA directly via its own secure terminal prompts. Neither this repository, nor any script, ever touches, logs, or stores your credentials.
- **No Hardcoded User Paths**: User directories (`%APPDATA%`) and local user names are resolved dynamically at runtime on your machine.
- **Gitignored Private Data**: All generated descriptors (`steam/generated/`), local Workshop IDs (`steam/workshop.properties`), and SteamCMD caches are strictly excluded via `.gitignore`.

---

## Quick Start: Publishing to Steam Workshop

### Option A: Using the Automated Script (Recommended)

1. **Verify your local build**:
   ```powershell
   ./gradlew deployAll
   ```
2. **Perform a dry-run (generates descriptors without uploading)**:
   ```powershell
   .\steam\upload-workshop.ps1 -DryRun
   ```
   Inspect the dynamically generated `.vdf` files in `steam/generated/` to ensure paths and versions look right.

3. **Upload both mods**:
   ```powershell
   .\steam\upload-workshop.ps1
   ```
   Or via the batch runner:
   ```cmd
   steam\upload-workshop.bat
   ```
   - SteamCMD will prompt for your Steam username, password, and Steam Guard code directly.
   - Visibility is set to **Private (`2`)** by default so you can inspect your Workshop item pages before making them public.

4. **Saving your Workshop IDs (for subsequent updates)**:
   When Steam outputs the new `PublishedFileId` (e.g., `3182910485`), record it in `steam/workshop.properties` (this file is gitignored):
   ```properties
   AUTO_PLANNER_WORKSHOP_ID=3182910485
   ARCHITECT_WORKSHOP_ID=3182910486
   ```
   Future runs of `upload-workshop.ps1` will automatically update these existing workshop items instead of creating duplicates.

---

### Option B: Using a GUI Tool (e.g. Steam Workshop Uploader)

If you prefer a graphical tool rather than the command line:
1. Open the tool and enter AppID `1162750`.
2. Select your deployed mod directory:
   - **Auto-Planner**: `%APPDATA%\songsofsyx\mods\SyxAutoPlanner`
   - **Architect**: `%APPDATA%\songsofsyx\mods\SyxArchitect`
3. Select the `icon.png` inside that folder as your preview image.
4. Copy the title and formatted BBCode description from:
   - [`docs/STEAM_WORKSHOP_AUTOPLANNER.txt`](../docs/STEAM_WORKSHOP_AUTOPLANNER.txt)
   - [`docs/STEAM_WORKSHOP_ARCHITECT.txt`](../docs/STEAM_WORKSHOP_ARCHITECT.txt)
5. Submit your item.
