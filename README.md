# 📱 M-Pesa Expense Tracker — Android App

Automatically tracks your M-Pesa income and expenses by reading incoming SMS messages. No manual entry required.

---

## ✨ Features

| Feature | Details |
|---|---|
| **Auto SMS Detection** | Listens for incoming M-Pesa SMS in real time |
| **Inbox Scan** | Imports historical M-Pesa messages on first launch |
| **Smart Parsing** | Handles Send, Receive, Paybill, Buy Goods, Withdraw, Airtime |
| **Auto-Categorization** | Groceries, Utilities, Transport, Food & Dining, Health, etc. |
| **Dashboard** | Monthly summary cards + interactive pie chart |
| **Transaction List** | Search, filter by category, swipe to delete |
| **Budget Tracking** | Set monthly limits per category with progress bars |
| **CSV Export** | Export any date range, share via email/Drive/WhatsApp |
| **Notifications** | Instant alert for every detected transaction |

---

## 🏗️ Architecture

```
MVVM + Repository Pattern
│
├── UI Layer         (Fragments + ViewModels)
├── Repository       (Single data source of truth)
├── Room Database    (Local SQLite via Room)
└── SMS Layer        (BroadcastReceiver + Parser)
```

**Libraries used:**
- Room 2.6 — local database
- Navigation Component 2.7 — fragment routing
- MPAndroidChart — pie chart
- Material Design 3 — UI components
- Coroutines + Flow — async operations
- WorkManager — background processing

---

## 🚀 Setup Instructions

### 1. Prerequisites
- Android Studio Hedgehog (2023.1.1) or newer
- Android SDK 34
- Java 17 (bundled with Android Studio)

### 2. Open Project
```bash
# Clone or extract the project
cd mpesa-tracker
# Open in Android Studio: File → Open → select this folder
```

### 3. Sync Gradle
- Android Studio will prompt to sync. Click **Sync Now**.
- Wait for dependencies to download (~2 min on first run).

### 4. Run
- Connect an Android device (API 26+) or use an emulator.
- Click **▶ Run** or press `Shift+F10`.

### 5. Grant Permissions
On first launch, grant:
- **Receive SMS** — for real-time detection
- **Read SMS** — for inbox scan
- **Notifications** — for transaction alerts

---

## 📁 Project Structure

```
app/src/main/
├── java/com/mpesa/tracker/
│   ├── MpesaTrackerApp.kt            # Application class
│   ├── data/
│   │   ├── db/
│   │   │   ├── AppDatabase.kt        # Room database
│   │   │   ├── TransactionDao.kt     # Transaction queries
│   │   │   ├── BudgetDao.kt          # Budget queries
│   │   │   └── Converters.kt         # Type converters
│   │   ├── model/
│   │   │   ├── Transaction.kt        # Transaction entity
│   │   │   └── Budget.kt             # Budget entity
│   │   └── repository/
│   │       └── TransactionRepository.kt
│   ├── sms/
│   │   ├── MpesaParser.kt            # ⭐ Core regex parser
│   │   ├── SmsReceiver.kt            # BroadcastReceiver
│   │   └── SmsScanner.kt             # Historical inbox scan
│   ├── ui/
│   │   ├── MainActivity.kt           # Nav host
│   │   ├── dashboard/                # Summary + chart
│   │   ├── transactions/             # Full transaction list
│   │   ├── budget/                   # Budget management
│   │   └── export/                   # CSV export
│   └── utils/
│       ├── CsvExporter.kt            # CSV generation + sharing
│       └── NotificationHelper.kt     # Push notifications
└── res/
    ├── layout/                       # XML layouts
    ├── navigation/nav_graph.xml      # Navigation routes
    ├── menu/bottom_nav_menu.xml      # Bottom nav items
    └── values/                       # Colors, strings, themes
```

---

## 🔍 SMS Formats Supported

| Type | Example |
|---|---|
| Send Money | `QGH1234XY Confirmed. Ksh1,000.00 sent to JOHN DOE 0712345678...` |
| Receive Money | `Ksh500.00 received from JANE DOE 0798765432 on 1/5/24...` |
| Paybill | `Confirmed. Ksh2,000.00 paid to KPLC PREPAID 123456. on 1/5/24...` |
| Buy Goods | `Confirmed. Ksh350.00 paid to NAIVAS SUPERMARKET. New M-PESA...` |
| Withdraw | `Confirmed. Ksh3,000.00 withdrawn from agent JOHN AGENT...` |
| Airtime | `Ksh100.00 airtime for 0712345678 has been successfully topped up` |

---

## 🗂️ CSV Export Format

```csv
Date,Transaction ID,Type,Category,Amount (Ksh),Recipient/Sender,Phone,Balance (Ksh),Note
2026-05-01 10:30:00,QGH1234XY,Buy Goods,Groceries,350.00,NAIVAS SUPERMARKET,,5000.00,
```

---

## 🛣️ Roadmap (Phase 2)

- [ ] Django REST backend for multi-device sync
- [ ] Bar chart for daily/weekly spending trends
- [ ] M-Pesa statement PDF import (backup)
- [ ] Dark mode
- [ ] Budget alerts (push notification when 80% used)
- [ ] Google Sheets export integration
- [ ] Recurring transaction detection

---

## 🔒 Privacy

- SMS are read **locally on-device** only
- No data is sent to any server
- Room database is stored in the app's private storage
- App can be used entirely offline

---

## 💰 Cost

| Item | Cost |
|---|---|
| Development tools | Free |
| SMS access | Free |
| Google Play Store | $25 one-time |
| Backend hosting | Optional (free tier available) |

---

## 📝 License

MIT License — free for personal and commercial use.
