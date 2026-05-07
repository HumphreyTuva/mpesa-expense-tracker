# ⚡ Quick Setup — Open in Android Studio

## Step 1 — Unzip & Open
1. Unzip `MpesaTracker.zip`
2. Open **Android Studio**
3. Click **File → Open** → select the `mpesa-tracker` folder
4. Click **OK**

## Step 2 — Fix Gradle Wrapper (one-time step)
Because the `gradle-wrapper.jar` binary cannot be bundled here, do ONE of these:

### Option A — Let Android Studio fix it automatically (easiest)
When Android Studio opens the project, it may show a yellow banner:
> *"Gradle wrapper is missing"*

Click **"Generate Gradle Wrapper"** → done ✅

### Option B — Use the menu
- Go to **Tools → Generate Gradle Wrapper**
- Click **OK** → done ✅

### Option C — Terminal (if you have Gradle installed)
```bash
cd mpesa-tracker
gradle wrapper --gradle-version 8.4
```

## Step 3 — Sync & Run
1. Wait for **Gradle sync** to finish (~2 min on first run, downloads dependencies)
2. Connect an Android phone (or start an emulator, API 26+)
3. Click **▶ Run** (green play button)
4. On your phone, tap **Allow** for SMS and Notification permissions

## That's it! 🎉
The app will immediately scan your M-Pesa SMS inbox and populate the dashboard.

---

## Troubleshooting

| Error | Fix |
|---|---|
| `SDK location not found` | Open **File → Project Structure → SDK Location** and set your Android SDK path |
| `Unresolved reference` errors | Click **File → Sync Project with Gradle Files** |
| `Minimum SDK` warning | Your emulator must be API 26+ (Android 8.0 Oreo) |
| Chart library not found | Make sure `https://jitpack.io` is in your `build.gradle` repositories ✓ already added |
