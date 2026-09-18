# ChaatLedger 🍲📊

A modern, offline-first Android application designed specifically for street food vendors, chaat stalls, and small retail businesses to track daily sales, split payment collections, and raw material vendor expenses in real-time.

---

## 🌟 The Problem
Street food businesses operate in chaotic, fast-paced environments where hundreds of micro-transactions occur daily across cash and UPI/Paytm. Most vendors track collections in paper notebooks or WhatsApp chats:
- Split payment tallies (Cash vs. UPI) get lost or miscalculated at the end of exhausting 12-hour shifts.
- Supplier invoices (dairy, oil, spices, packaging) get misplaced.
- When two business partners run the stall, keeping both phones synchronized in real-time without constant manual check-ins is challenging.

## 🚀 Key Features
- **Fast Sales Logging (<10s):** Log daily closing sales with auto-calculated totals for Cash and UPI/Paytm.
- **Supplier & Raw Material Expense Management:** Categorize expenses (Dairy, Vegetables, Spices, Oil/Ghee, Packaging, Utilities) and monitor paid vs. unpaid dues.
- **Real-Time Multi-Device Sync:** Data syncs seamlessly across all stall owners' phones using Google Cloud Firestore.
- **Offline-First Architecture:** Complete offline functionality powered by local Android Room (SQLite) caching. Mutations are safely queued and synchronized automatically when an internet connection is re-established.
- **Zero-Friction Authentication:** Instant background anonymous authentication with Firebase—no passwords or login barriers during peak rush hours.
- **Live Sync Indicator:** Interactive top-bar cloud indicator displaying real-time sync state (Online, Syncing, Offline Queued).
- **Daily Net Profit Engine:** Instant breakdown of Gross Revenue, Total Expenses, and Net Daily Take-Home Profit.

---

## 🛠️ Architecture & Tech Stack
- **Language:** [Kotlin](https://kotlinlang.org/)
- **UI Framework:** [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material Design 3
- **Architecture:** Clean Architecture + MVVM (Model-View-ViewModel) + Repository Pattern
- **Reactive Streams:** Kotlin Coroutines & `StateFlow`
- **Local Database (Offline Cache):** [Android Room (SQLite)](https://developer.android.com/training/data-storage/room)
- **Cloud Backend:** [Firebase Firestore](https://firebase.google.com/docs/firestore) & Firebase Authentication
- **Image Handling:** Android Photo Picker & [Coil](https://coil-kt.github.io/coil/)

---

## 🔧 Getting Started

### Prerequisites
- Android Studio Ladybug or newer
- JDK 17+
- Android SDK 35 (minimum SDK 26)

### Firebase Configuration
1. Clone this repository:
   ```bash
   git clone https://github.com/your-username/chaatledger.git
   cd chaatledger
   ```
2. Create a project in the [Firebase Console](https://console.firebase.google.com/).
3. Add an Android app with package name `com.aistudio.chaatledger.wypf`.
4. Enable **Anonymous Authentication** under *Authentication > Sign-in method*.
5. Enable **Cloud Firestore** in production or test mode.
6. Download your `google-services.json` from Firebase and place it in the `/app` folder (see `google-services.json.example` for reference).
7. Build and run:
   ```bash
   gradle :app:assembleDebug
   ```

---

## 📄 License
This project is open-source under the MIT License.
