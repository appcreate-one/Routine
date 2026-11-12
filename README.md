# BranchOps Pulse 🚀

[![Build Android APK](https://github.com/OWNER/REPOSITORY/actions/workflows/build-apk.yml/badge.svg)](https://github.com/OWNER/REPOSITORY/actions/workflows/build-apk.yml)

**BranchOps Pulse** is a modern Branch Operations & Daily Routine Tracker Android application built with **Kotlin**, **Jetpack Compose (Material 3)**, and **Room Database**. It intercepts and logs shared WhatsApp data (via Android's native `ACTION_SEND` and `ACTION_SEND_MULTIPLE` Share Sheet) and tracks a strict **21-point daily branch operations checklist** across all 31 days of the month.

It also includes an automated **5:00 PM Daily Routine Alarm & Notification Scheduler** and an interactive **31-Day Custom Canvas Completion Bar Chart**.

---

## 📱 Features

- **WhatsApp Direct Share Handling (`ACTION_SEND`)**:
  - Automatically captures shared WhatsApp text notes, URLs/spreadsheets, images, videos, audio, and PDF documents.
  - Categorizes incoming content into Text, Link, Image, and Document feeds.
  - One-tap clipboard copy and deletion.
  - Interactive "Simulate Share" button for quick testing in emulators.
- **21-Point Daily Branch Routine Tracker**:
  - Pinned/sticky task column on the left with horizontal scrolling across days 1–31.
  - Tracks all 21 exact branch checkpoints:
    1. *Staff Reporting cross check*
    2. *Field Visits (If Need)*
    3. *Branch Reporting*
    4. *Attendance Marking*
    5. *System Opening*
    6. *Today Business Planing*
    7. *Disbursment*
    8. *GRT & PD*
    9. *Fresh Arrear Cross check*
    10. *All Pending mails clearnes*
    11. *All Other registers and files cross check*
    12. *HM Cross Check - IGL, IL & MSME (Every 2 hr. gap)*
    13. *Disb Closing (Try to complete the Disb before 3.00PM)*
    14. *Collection Deposit (Before 4.00PM)*
    15. *Demand pending Follow up 3.00PM Onwards*
    16. *PTP Visit Cross Check*
    17. *RO wise Survey Cross check*
    18. *Cash Book Updation*
    19. *EOD*
    20. *System Closing before 6.00PM*
    21. *Cross check all above mentioned points once again*
- **31-Day Routine Performance Bar Chart**:
  - Custom Jetpack Compose `Canvas` drawing daily completion percentages (`0%`–`100%`).
  - Color-coded performance tiers: **High (`≥85%`, Emerald)**, **Moderate (`50%–84%`, Amber)**, **Low (`<50%`, Cyan)**.
  - Toggleable 3-day moving average trend curve and interactive day inspection.
- **5:00 PM Daily Notification Check**:
  - `AlarmManager` schedules a daily 5:00 PM check.
  - Posts an alert notification if any of today's 21 branch checkpoints are still unchecked before the 6:00 PM system closing deadline.
  - Re-registers automatically after device reboot via `BOOT_COMPLETED`.
- **Standalone HTML App Included**:
  - Packaged at `app/src/main/assets/index.html` for instant browser execution on mobile or desktop.

---

## 🛠️ Tech Stack & Architecture

- **Language**: Kotlin 2.2+
- **UI Toolkit**: Jetpack Compose (Material Design 3)
- **Architecture**: MVVM (Model-View-ViewModel) + Repository Pattern
- **Local Persistence**: Room Database (`2.7.0`) via KSP (Kotlin Symbol Processing)
- **Asynchronous Flow**: Kotlin Coroutines & `StateFlow`
- **Build System**: Gradle 9.3.1 with Android Gradle Plugin (AGP) 9.1.1
- **CI/CD**: GitHub Actions workflow (`.github/workflows/build-apk.yml`)

---

## 🚀 How to Push to GitHub & Build the APK

### Option A: Using the AI Studio UI (Easiest)
1. Click the **Settings** (gear icon) or **Share / Export** menu in the top-right corner of Google AI Studio.
2. Select **Push to GitHub** (or **Export to GitHub**).
3. Authorize your GitHub account and choose your repository name.
4. Once pushed, go to your repository on GitHub and click the **Actions** tab.
5. The **Build Android APK** workflow will run automatically.
6. When the build finishes, download **`BranchOps-Pulse-Debug-APK`** under the **Artifacts** section!

### Option B: Using Git from your Terminal
```bash
# 1. Download or export the project ZIP from AI Studio and extract it
cd branchops-pulse

# 2. Initialize git if not already initialized
git init
git add .
git commit -m "Initial commit of BranchOps Pulse app"

# 3. Add your remote GitHub repository
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/YOUR_REPOSITORY.git

# 4. Push to GitHub
git push -u origin main
```

---

## ⚙️ Building the APK Locally

If you clone the repository onto your computer:

```bash
# Ensure Java 21 is installed
java -version

# Build the debug APK
./gradlew :app:assembleDebug

# The APK will be generated at:
# app/build/outputs/apk/debug/app-debug.apk
```

---

## 📄 License
This project is open source and available under the [MIT License](LICENSE).
