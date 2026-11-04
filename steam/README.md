# Steam Workshop Deployment Guide for Songs of Syx

This directory contains the necessary configuration files to publish and update **Syx Auto-Planner** and **Syx Architect** on the Steam Workshop for Songs of Syx (AppID `1162750`).

---

## Prerequisites
1. **Steam Account**: You must be logged into the Steam account that owns Songs of Syx.
2. **Steam Workshop Agreement**: Ensure you have accepted the Steam Workshop Legal Agreement on Steam.
3. **Build & Deploy**: Run `./gradlew deployAll` in the repository root to ensure the latest binaries, `_Info.txt`, and `icon.png` are deployed to `%APPDATA%/songsofsyx/mods/`.

---

## Method 1: Using SteamCMD (Recommended & Scriptable)

1. **Install SteamCMD**: If not already installed, download [SteamCMD](https://developer.valvesoftware.com/wiki/SteamCMD#Downloading_SteamCMD) and extract it (e.g. to `C:\steamcmd\`).
2. **First Upload (New Workshop Items)**:
   In `steam/SyxAutoPlanner.vdf` and `steam/SyxArchitect.vdf`, `publishedfileid` is set to `"0"`, and `visibility` is set to `"2"` (Private, so you can review before making public).
3. **Execute Upload**:
   Open a terminal and run:
   ```cmd
   steamcmd +login <your_steam_username> +workshop_build_item "%CD%\steam\SyxAutoPlanner.vdf" +quit
   steamcmd +login <your_steam_username> +workshop_build_item "%CD%\steam\SyxArchitect.vdf" +quit
   ```
4. **Save Workshop IDs**:
   Steam will output the assigned `PublishedFileID` (e.g., `3182910485`).
   - Replace `"publishedfileid" "0"` with your new ID in the `.vdf` file for future updates.
   - Update `_Info.txt` if you want the game launcher to show the workshop link.

---

## Method 2: Using the Community Steam Workshop Uploader GUI

If you prefer a graphic interface instead of the command line:
1. Download a standalone Steam Workshop Uploader tool (such as [Steam Workshop Uploader GUI](https://github.com/CyberSys/Steam-Workshop-Uploader) or similar).
2. Set AppID to `1162750`.
3. Select the content folder:
   - Auto-Planner: `%APPDATA%\songsofsyx\mods\SyxAutoPlanner`
   - Architect: `%APPDATA%\songsofsyx\mods\SyxArchitect`
4. Select the preview image (`icon.png` inside the mod folder).
5. Paste the title and formatted description from `docs/STEAM_WORKSHOP_AUTOPLANNER.txt` or `docs/STEAM_WORKSHOP_ARCHITECT.txt`.
6. Click **Upload**.

---

## Workshop Descriptions & Formatting
Full formatted BBCode descriptions ready to be pasted into the Steam Workshop item pages are located in:
- `docs/STEAM_WORKSHOP_AUTOPLANNER.txt`
- `docs/STEAM_WORKSHOP_ARCHITECT.txt`
