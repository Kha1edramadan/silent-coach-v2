# Silent Coach V2 — GitHub Setup

## Repository

**Repository name:** `silent-coach-v2`

**Description:** `Silent Coach — personal fitness, workout, nutrition and progress tracker.`

For the first upload, keep the repository **Private**.

## Upload from the GitHub website

1. Open GitHub and choose **New repository**.
2. Repository name: `silent-coach-v2`
3. Description: `Silent Coach — personal fitness, workout, nutrition and progress tracker.`
4. Choose **Private**.
5. Do **not** add another README, `.gitignore`, or license because this package already contains them.
6. Create the repository.
7. Open the repository and choose **Add file → Upload files**.
8. Extract this ZIP on your computer.
9. Upload the **contents of the extracted `SilentCoachV2-ProductFinal` folder**, not the outer folder itself.
10. Commit message: `Initial Silent Coach V2 product build`
11. Commit directly to `main`.

GitHub's browser upload supports up to 100 files at once, with a 25 MiB limit per file. This project is well below those limits.

## Build the APK on GitHub

The repository already contains:

`.github/workflows/android-release.yml`

After the first push:

1. Open the repository on GitHub.
2. Click **Actions**.
3. Open **Android release checks**.
4. Click the latest successful workflow run.
5. Scroll to **Artifacts**.
6. Download `silent-coach-v2-android-build`.
7. Inside it you will find:
   - `app-debug.apk`
   - `app-release.aab`

For a phone installation, use `app-debug.apk`.

## Manual workflow run

You can also run it without changing code:

**Actions → Android release checks → Run workflow → Run workflow**

The workflow runs the core self-test, builds the debug APK, builds the release AAB, and uploads the generated artifacts.

## Important

Do not put API keys, passwords, signing keys, or other secrets into the repository. GitHub recommends keeping secrets out of commits, and push protection may block files containing supported secrets.

The current product keeps external food imports unverified until they are reviewed against a traceable source or the current product label.
