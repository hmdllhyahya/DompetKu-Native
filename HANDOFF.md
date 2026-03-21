# DompetKu — Master Handoff
> **File ini adalah satu-satunya handoff yang aktif.** Semua handoff lama sudah di-merge ke sini dan dihapus.
> Update file ini di setiap akhir sesi. Tambahkan entry baru di bagian bawah LOG PERUBAHAN.

---

## QUICK REFERENCE — RULES WAJIB

| Rule | Detail |
|------|--------|
| `enableEdgeToEdge()` | **DIHAPUS** dari MainActivity — jangan tambahkan lagi |
| `Color(gradientStart)` | Selalu `.toInt()` dulu — `Color(account.gradientStart.toInt())` |
| `BrandInfo.gradientStart` | Sudah bertipe `Color` — tidak perlu `.toInt()` |
| `Toggle` composable | `onToggle = { }` tanpa parameter (tidak ada `onCheckedChange`) |
| `TxnUiState.allTxns` | **DIHAPUS** — gunakan `grouped`, `totalCount`, `accountMap` |
| `filtered(state)` public | **DIHAPUS** dari TransactionsViewModel & AnalyticsViewModel |
| DB version | `3`, `fallbackToDestructiveMigration()` aktif |
| FileProvider authority | `com.dompetku.fileprovider` |
| Version string | `0.0703.5G` (di ProfileScreen subtitle + build.gradle.kts) |
| Konvensi versi | `0.MMDD.PHASE` — contoh `0.0703.5G` = 7 Maret, Phase 5G |

---

## ARSITEKTUR RINGKAS

```
app/src/main/java/com/dompetku/
├── DompetKuApp.kt               — Application, WorkManager config, NotificationChannel
├── MainActivity.kt              — FragmentActivity (BUKAN ComponentActivity!) auto-lock 30s
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt       — Room DB v2, fallbackToDestructiveMigration
│   │   ├── dao/Daos.kt          — TransactionDao, AccountDao, AttachmentDao
│   │   └── entity/Entities.kt  — TransactionEntity, AccountEntity, AttachmentEntity
│   ├── preferences/
│   │   └── UserPreferences.kt  — DataStore, semua keys: ONBOARDED/PIN/BIO/NOTIF/dll
│   └── repository/
│       └── Repositories.kt     — TransactionRepository, AccountRepository, AttachmentRepository
├── di/AppModule.kt
├── domain/model/Models.kt       — Transaction, Account, UserProfile, AppPreferences, AccountType
├── ui/
│   ├── MainScaffold.kt          — global sheet host
│   ├── RootViewModel.kt         — shouldLock flow, triggerLock(), clearLock()
│   ├── navigation/
│   │   ├── FanNav.kt            — FAB fan navigation
│   │   ├── NavGraph.kt
│   │   └── Screen.kt
│   ├── components/CommonComponents.kt, DompetKuLogo.kt
│   └── screen/
│       ├── home/HomeScreen.kt + HomeViewModel.kt
│       ├── analytics/AnalyticsScreen.kt + AnalyticsViewModel.kt
│       ├── transactions/TransactionsScreen.kt + TransactionsViewModel.kt + sheets
│       ├── accounts/AccountsScreen.kt + AccountsViewModel.kt + sheets
│       ├── profile/ProfileScreen.kt + ProfileViewModel.kt
│       ├── pin/PinLockScreen.kt + PinViewModel.kt
│       └── onboarding/OnboardingScreen.kt + OnboardingViewModel.kt
├── worker/ReminderWorker.kt
└── util/
    ├── BrandDetector.kt, CurrencyFormatter.kt, DateUtils.kt
    ├── ExportImportManager.kt (+ SmartImportEngine internal object)
    ├── PinHasher.kt, SoundManager.kt, SmartCategoryDetector.kt
    └── ExportImportManager.kt
```

---

## STATE PENTING PER VIEWMODEL

### `HomeUiState`
```kotlin
accountMap: Map<String, Account>   // O(1) lookup
recentTxns: List<Transaction>      // observeRecent(10)
// monthlyIncome, monthlyExpense, totalBalance, dll
```

### `TxnUiState`
```kotlin
grouped: Map<String, List<Transaction>>   // grouped by date
totalCount: Int
totalIncome: Long
totalExpense: Long
accountMap: Map<String, Account>
// TIDAK ADA allTxns
```

### `AnalyticsUiState`
```kotlin
filteredTxns: List<Transaction>
totalIncome: Long
totalExpense: Long
pieData: List<PieDatum>
barData: List<BarDatum>
lifestyle: LifestyleData?
salaryInsight: SalaryInsight?
monthlyTrend: List<MonthDatum>
savingsRate: Int
trendMonths: Int   // 6 atau 12
```

### `AppPreferences` (DataStore)
```kotlin
onboarded, pinEnabled, pinHash, bioEnabled, soundEnabled,
hideBalance, lang, monthlyBudget, savedPct, avatarPath,
notifEnabled, userProfile: UserProfile
```

---

## SECURITY AUDIT (2026-03-08)

### ✅ Yang Sudah Baik
| Item | Status |
|------|--------|
| `android:allowBackup="false"` | ✅ Cegah backup data sensitif ke cloud |
| Auto-lock 30 detik background | ✅ Aktif di MainActivity onStop/onStart |
| Biometric prompt | ✅ BIOMETRIC_STRONG \| BIOMETRIC_WEAK — kompatibel banyak HP |
| FileProvider untuk export | ✅ File tidak bisa diakses app lain |
| SAF untuk import | ✅ Tidak butuh storage permission |
| Biometric error code 10 | ✅ User cancel tidak dianggap error |
| App-specific external storage | ✅ `getExternalFilesDir` — terisolasi per app |

### ⚠️ Yang Perlu Diperhatikan / Diimprove
| Item | Severity | Detail |
|------|----------|--------|
| **PIN hashing (SHA-256 + static salt)** | MEDIUM | `PinHasher` pakai SHA-256 dengan salt statis `"dompetku_salt_2024"`. SHA-256 terlalu cepat untuk PIN storage — idealnya PBKDF2 atau bcrypt dengan iterasi. Namun PIN 6 digit = 1 juta kombinasi, dan hash hanya bisa diserang jika attacker sudah punya akses ke DataStore (berarti device sudah rooted). Risiko nyata: rendah untuk threat model normal. |
| **DataStore tidak dienkripsi** | LOW-MEDIUM | PIN hash, nama, usia, pekerjaan tersimpan di DataStore biasa (plaintext di `/data/data/`). Hanya accessible jika device rooted. Bisa upgrade ke `EncryptedSharedPreferences` atau `EncryptedDataStore` untuk defense-in-depth. |
| **Room DB tidak dienkripsi** | LOW-MEDIUM | Seluruh data transaksi tersimpan plaintext di SQLite. Sama seperti DataStore — accessible hanya jika rooted. Bisa pakai SQLCipher jika diperlukan. |
| **Tidak ada FLAG_SECURE** | LOW | Layar app bisa di-screenshot dan muncul di app switcher. Bisa tambah `window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)` di MainActivity untuk cegah screenshot. |
| **INTERNET permission ada tapi tidak dipakai** | INFO | `AndroidManifest.xml` punya `<uses-permission android:name="android.permission.INTERNET" />`. Kalau memang tidak ada fitur online, pertimbangkan hapus untuk memperkecil attack surface. |
| **Salt PIN hardcoded** | LOW | Salt statis artinya dua user dengan PIN sama akan punya hash sama. Untuk app single-user di device, ini tidak masalah praktis. |

### 🔴 Bug Diperbaiki Sesi Ini
| Bug | Root Cause | Fix |
|-----|-----------|-----|
| **Fingerprint tidak berjalan** | `MainActivity` extends `ComponentActivity`, cast ke `FragmentActivity` di PinLockScreen selalu `null` | Ubah `MainActivity : ComponentActivity()` → `MainActivity : FragmentActivity()` |

### Rekomendasi Prioritas ke Depan
1. **[MEDIUM]** Ganti SHA-256 ke PBKDF2WithHmacSHA256 dengan iterasi 100.000+ di `PinHasher.kt`
2. **[LOW]** Tambah `FLAG_SECURE` di MainActivity
3. **[LOW]** Pertimbangkan `EncryptedSharedPreferences` untuk DataStore
4. **[INFO]** Hapus `INTERNET` permission jika tidak ada rencana fitur online

---

## FITUR YANG SUDAH SELESAI (SEMUA FASE)

### Phase 5A
- AccountFormSheet redesign: type grid 2 kolom, "Lainnya" free text, brand auto-detect
- TransactionFormSheet: animated tab slide, spacer fixes
- AccountsScreen: jiggle mode InfiniteTransition
- BrandDetector: 33+ brands, fuzzy alias, longest-match
- PinLockScreen: 6 digit (dari 4)
- Auto-lock: 30 detik background trigger
- AccountType: tambah `credit`

### Phase 5B / 5C
- Transfer tampil di kedua akun (from + to)
- Carousel swipe real-time follow finger (HomeScreen)
- Language toggle slide animation (ProfileScreen)
- Predictor popup detail sheet (HomeScreen)
- Hide balance toggle (AccountsScreen + TransactionsScreen + AccountDetailScreen)
- ProfileInfoChip, ProfileCard, JOBS/EDUS expanded, Lainnya free text
- AccountType: tambah `emoney`
- Fix transfer balance race condition → atomic SQL `UPDATE balance + :delta`
- Fix transfer edit bug (adminFee)
- Attachment lampiran foto/file (TransactionFormSheet, TransactionDetailSheet)
- Account card berwarna di TransactionFormSheet (LazyRow colored cards)
- Smart category detection + Easter egg KCIJ/Whoosh

### Phase 5E
- Analytics donut chart (Canvas-based, animasi draw-in, tap segment, shadow glow)
- Monthly trend chart 6/12 bulan
- Savings rate circular gauge

### Phase 5F
- Export XLSX (2 sheet: Transaksi + Akun)
- Smart Import universal (detect kolom otomatis, 9 role, multi-bahasa)
- SmartImportDialog (2 step: account resolution + confirmation)
- Sound effects (ToneGenerator, SoundManager)
- Drag-to-reorder account cards (swap ←/→ di editMode)
- AccountFormSheet Color Wheel (HSV wheel + brightness slider)

### Phase 5G
- Performance: LazyColumn, flowOn(Default), accountMap O(1)
- BUG-01: search field fix
- BUG-02: transfer type detection di import
- BUG-03: note pollution (kolom IGNORED_HEADERS tidak di-append)
- BUG-04: transfer pair merge (Money Manager format)
- BUG-05: date picker fix
- BUG-07: carousel peek cards (isDragging derivedStateOf)
- BUG-09: FAB spring tuning
- FanNav animation tuning (pulse, FAB spring, item spring/tween)
- Smart Budget Notifications (5 slot: 06/12/13/17/21, transport analysis, adaptive messages)
- DompetKuApp: CHANNEL_BUDGET_ID IMPORTANCE_HIGH
- Smart Import Bug Fixes: column detection false positive ("in" substring), amount format Indonesia, datetime combined column, dash filter, emoji category strip
- ProfileScreen: [DEBUG] Test Notifikasi tombol (BuildConfig.DEBUG → sementara `if (true)`)

### Phase 5H — Sesi Ini (2026-03-08)
- **BUG-BIO: Fingerprint tidak berjalan** → `MainActivity : FragmentActivity()` (dari ComponentActivity)
- Security Audit (lihat section di atas)

---

## NOTIFIKASI SMART BUDGET — DETAIL TEKNIS

**5 slot per hari:**
| Slot | Jam | Konten |
|------|-----|--------|
| SLOT_MORNING (0) | 06:00 | Budget harian + transport analysis |
| SLOT_LUNCH (1) | 12:00 | Sisa budget, pengeluaran pagi |
| SLOT_RETURN (2) | 13:00 | Recap + reminder transport pulang |
| SLOT_COMMUTE (3) | 17:00 | Budget vs biaya transport pulang |
| SLOT_SUMMARY (4) | 21:00 | Ringkasan harian |

**Transport analysis:** scan 60 hari terakhir, keyword: ojek/grab/gojek/krl/mrt/lrt/bus/bensin/parkir/tol/dll. Frekuensi ≥20% hari → `isRegularCommuter = true`. `avgTransport` = rata-rata biaya per transaksi transport.

**WorkManager:** 5 PeriodicWorkRequest terpisah (`dk_notif_0600` dst), initial delay ke jam target berikutnya. Legacy `dompetku_daily_reminder` di-cancel otomatis.

**NotifID:** `NOTIF_BASE = 2001`, slot 0-4 pakai `2001–2005`. Channel: `dompetku_budget` (IMPORTANCE_HIGH).

**Test:** Profil → TENTANG → [DEBUG] Test Notifikasi → kirim semua 5 slot sekarang.
> ⚠️ Tombol ini sementara `if (true)` — ubah ke `if (BuildConfig.DEBUG)` sebelum release (setelah first `assembleDebug` pernah jalan, BuildConfig sudah tersedia).

---

## SMART IMPORT — COLUMN DETECTION ROLES

| Role | Keywords (sebagian) |
|------|-------------------|
| DATE | date, tanggal, tgl, waktu, datetime, timestamp |
| TIME | time, jam, hour, pukul |
| AMOUNT | amount, nominal, jumlah, total, nilai, uang |
| DEBIT | debit, expense, pengeluaran, keluar, out, minus |
| CREDIT | credit, income, pemasukan, masuk, plus, deposit |
| CATEGORY | category, kategori, kat, jenis, grup |
| NOTE | note, catatan, keterangan, memo, nama, judul |
| ACCOUNT | account, akun, wallet, dompet, source, rekening |
| TYPE | type, tipe, jenis transaksi, in/out |

**Rules:** substring check hanya untuk keyword ≥3 char (fix false positive "in" → "nominal"). Emoji di-strip sebelum category lookup.

**Amount parsing:** Multi-dot = Indonesia ribuan. 1 dot + 3 digit setelah = thousand sep. Dash (`-`/`—`) dianggap null/kosong.

---

## PENDING / KNOWN ISSUES

| Item | Severity | Detail |
|------|----------|--------|
| ~~Test notif tombol `if (true)`~~ | ~~LOW~~ | ✅ **FIXED** — `BuildConfig.DEBUG` + import proper sudah diterapkan |
| ~~PIN hashing weak~~ | ~~MEDIUM~~ | ✅ **FIXED** — PBKDF2WithHmacSHA256, 100k iterasi, random salt per hash. Migration transparan dari SHA-256 lama |
| ~~DataStore tidak enkripsi~~ | ~~LOW~~ | ✅ **FIXED** — EncryptedSharedPreferences untuk data sensitif (PIN, bio, profil). Migration transparan dari DataStore lama. |
| ~~FLAG_SECURE tidak ada~~ | ~~LOW~~ | ✅ **FIXED** — `window.addFlags(FLAG_SECURE)` di MainActivity.onCreate |
| ~~INTERNET permission unused~~ | ~~INFO~~ | Dikembalikan — dibutuhkan oleh CurrencySheet (real-time kurs via exchangerate-api.com). Komentar justifikasi ditambah di Manifest. |

---

---

## UNTUK GPT CODEX — HANDOFF AKTIF

> Dokumen ini adalah satu-satunya referensi project. Baca seluruh HANDOFF sebelum mulai coding.
> Project path: `C:\Users\Yahya Hamdallah\Documents\GitHub\DompetKu-Native`
> Gunakan Filesystem MCP untuk baca/edit file. Jangan gunakan git, terminal, atau tool lain.

### Konteks sebelum kamu mulai
- App: **DompetKu** — Android personal finance, Kotlin + Jetpack Compose + Room + Hilt + MVVM
- Claude sudah mengerjakan semua fitur sampai hari ini (Sabtu, 21 Maret 2026)
- Weekly limit Claude hampir habis, pekerjaan dilanjutkan oleh kamu (Codex)
- Claude akan kembali Kamis depan untuk review dan lanjut

### Rules wajib yang TIDAK BOLEH dilanggar
| Rule | Detail |
|------|--------|
| `enableEdgeToEdge()` | HAPUS dari MainActivity — jangan tambahkan |
| `MainActivity` | Extends `FragmentActivity`, BUKAN `ComponentActivity` |
| `Toggle` composable | `onToggle = { }` tanpa parameter |
| DB version | Sekarang `3` — kalau ada schema change, bump ke `4` dan `fallbackToDestructiveMigration()` tetap aktif |
| `TxnUiState.allTxns` | TIDAK ADA — gunakan `grouped`, `totalCount`, `accountMap` |
| Color gradients | `Color(account.gradientStart.toInt())` — selalu `.toInt()` |

### Tugas yang harus dikerjakan Codex

**TASK 1 — PIN Setup Screen baru (prioritas tinggi)**
- Saat ini: tap "Kunci PIN" → toggle ON → muncul `ChangePinSheet` (bottom sheet biasa dengan TextField)
- Yang diinginkan: **full screen baru** mirip `PinLockScreen.kt` dengan custom keypad DompetKu
- File referensi: `ui/screen/pin/PinLockScreen.kt` — lihat layout custom keypad-nya
- Buat file baru: `ui/screen/pin/PinSetupScreen.kt`
- Alur: 2 tahap — (1) masukkan PIN baru, (2) konfirmasi PIN ulang. Kalau cocok, simpan.
- Navigasi: dari `ProfileScreen`, saat toggle PIN ON → navigate ke `PinSetupScreen` via NavGraph
- Tambahkan `Screen.PinSetup` di `Screen.kt` dan route di `NavGraph.kt`
- Warna: gradient hijau DompetKu (GreenPrimary → GreenDark), sama persis dengan `PinLockScreen`

**TASK 2 — Vibration / Haptics**
- Tambah haptic feedback di:
  1. Setiap tap tombol di keypad (PinLockScreen, PinSetupScreen baru)
  2. Toggle ON dan OFF di `ProfileScreen` — gunakan pola berbeda (ON = 2 pulsa, OFF = 1 pulsa)
  3. FAB tap di `FanNav.kt`
- Gunakan `android.os.Vibrator` via `getSystemService(Context.VIBRATOR_SERVICE)`
- Android 26+: gunakan `VibrationEffect.createOneShot()` atau `createWaveform()`
- Buat helper object: `util/HapticHelper.kt` dengan fungsi `tapLight()`, `tapMedium()`, `toggleOn()`, `toggleOff()`
- Permission `VIBRATE` sudah ada di `AndroidManifest.xml`

**TASK 3 — Biometric confirmation saat enable**
- File: `ui/screen/profile/ProfileScreen.kt`
- Saat ini: user toggle biometric ON → langsung enable tanpa verifikasi
- Yang diinginkan: toggle ON → langsung tampilkan `BiometricPrompt` untuk konfirmasi identitas → kalau berhasil baru enable, kalau gagal toggle kembali OFF
- Referensi implementasi BiometricPrompt: `ui/screen/pin/PinLockScreen.kt` — sudah ada implementasi yang benar
- Context penting: `MainActivity extends FragmentActivity` (sudah benar)
- Callback: `onAuthenticationSucceeded` → `viewModel.setBioEnabled(true)`, `onAuthenticationFailed/Error` → tidak enable

**TASK 4 — Language Switch**
- Toggle ID/EN sudah ada di `ProfileScreen` dan menyimpan `"id"`/`"en"` ke DataStore via `UserPreferences.setLang()`
- Yang belum ada: implementasi aktual untuk mengganti bahasa app
- Cara yang direkomendasikan untuk Compose:
  1. Buat `res/values/strings.xml` (Indonesia, ini default)
  2. Buat `res/values-en/strings.xml` (English)
  3. Di `MainActivity.onCreate` atau `Application.attachBaseContext`, baca lang dari DataStore dan set locale dengan `LocaleListCompat`
  4. Gunakan `AppCompatDelegate.setApplicationLocales()` (AndroidX AppCompat sudah jadi dependency via Material)
- String yang perlu diterjemahkan: semua label UI di semua screen (minimal Home, Transaksi, Akun, Analisis, Profil)
- Dependency yang mungkin perlu ditambah: pastikan `androidx.appcompat:appcompat` sudah ada di `libs.versions.toml`

### File-file penting yang perlu dibaca
```
ui/screen/pin/PinLockScreen.kt         — referensi custom keypad
ui/screen/pin/PinViewModel.kt          — referensi verifikasi PIN
ui/screen/profile/ProfileScreen.kt     — tempat toggle PIN & biometrik
ui/screen/profile/ProfileViewModel.kt  — fungsi setPinEnabled, setBioEnabled
ui/navigation/Screen.kt                — tambah PinSetup route di sini
ui/navigation/NavGraph.kt              — tambah composable PinSetup di sini
util/SoundManager.kt                   — referensi pattern untuk HapticHelper
```

### Cara update HANDOFF
Setelah selesai mengerjakan setiap task, tambahkan entry baru di LOG PERUBAHAN dengan format:
```
### YYYY-MM-DD — [Nama Task]
- Perubahan apa yang dilakukan
- File apa yang diubah/dibuat
```

---

### AUDIT AKTIF — 2026-03-22 (belum ada code change)

**Status sesi ini**
- User minta audit penuh dulu, jangan ubah code aplikasi dulu.
- Di sesi ini hanya boleh kumpulkan temuan, susun rencana kerja, dan update `HANDOFF.md`.
- Build yang diverifikasi di sesi ini: `assembleDebug` berhasil.

**Tujuan audit**
1. Cari akar masalah white screen saat setup PIN pertama kali.
2. Audit bottleneck performa utama: unlock PIN, perpindahan Home ke Profile, render/list cost.
3. Audit widget budget/predictor di Home: gesture, animasi, dan perubahan layout.
4. Audit jiggle mode/reorder akun vs requirement drag-and-drop.
5. Audit back navigation di tab Analisis.
6. Audit konsistensi language toggle dan string hardcoded.

**Temuan utama**

**1. PIN setup first-enable → white screen**
- Akar masalah paling kuat ada di kombinasi:
  - `RootViewModel.startDestination` yang reaktif terhadap `pinEnabled`
  - `NavHost(startDestination = start)` yang ikut membaca state itu terus-menerus
  - flow sukses `PinSetupScreen` yang hanya melakukan `popBackStack()`
- File terkait:
  - `app/src/main/java/com/dompetku/ui/RootViewModel.kt`
  - `app/src/main/java/com/dompetku/ui/navigation/NavGraph.kt`
  - `app/src/main/java/com/dompetku/ui/screen/pin/PinSetupScreen.kt`
- Detail logika:
  - Setelah PIN baru tersimpan, `PinSetupScreen` memanggil `viewModel.setPinEnabled(true)` lalu `onSuccess()`.
  - Begitu `pinEnabled = true`, `RootViewModel.startDestination` berubah dari `Screen.Main.route` menjadi `Screen.PinLock.route`.
  - Pada saat yang sama, route `PinSetup` hanya `popBackStack()`.
- Hipotesis audit:
  - `startDestination` seharusnya hanya dipakai untuk initial route saat app cold start, bukan berubah saat user sedang berada di flow aktif.
  - Reaktifnya `startDestination` saat setup PIN kemungkinan merusak sinkronisasi back stack/graph dan memunculkan blank screen.
  - Gejala user cocok: setelah blank screen, back berikutnya keluar app karena stack utama sudah tidak sehat.

**2. Delay 1–2 detik setelah digit ke-6 saat unlock PIN**
- Akar masalah sangat mungkin valid: hashing/verifikasi PBKDF2 masih berjalan di main thread.
- File terkait:
  - `app/src/main/java/com/dompetku/ui/screen/pin/PinViewModel.kt`
  - `app/src/main/java/com/dompetku/util/PinHasher.kt`
- `PinHasher.verifyAndUpgrade()` memakai `PBKDF2WithHmacSHA256` 100.000 iterasi.
- `PinViewModel.verifyPin()` dan `savePin()` menjalankan kerja itu di `viewModelScope.launch` default tanpa `Dispatchers.Default`.
- Dampak:
  - Setelah digit ke-6 ditekan, UI bisa terasa freeze 1–2 detik sebelum navigation callback sukses jalan.

**3. Home → Profile terasa lambat**
- Bottleneck yang terlihat saat audit:
  - `ProfileViewModel.uiState` menggabungkan `userPrefs.appPrefsFlow`, `transactionRepo.observeAll()`, dan `accountRepo.observeAll()`.
  - Padahal untuk first paint layar Profile, kebutuhan awal utamanya hanya `prefs`, `txnCount`, `accountCount`, dan sebagian kecil data akun.
- File terkait:
  - `app/src/main/java/com/dompetku/ui/screen/profile/ProfileViewModel.kt`
  - `app/src/main/java/com/dompetku/data/repository/Repositories.kt`
- Dampak:
  - Masuk ke Profile ikut memicu hydrate seluruh transaksi ke domain object.
  - `TransactionRepository.observeAll()` juga parse `detailsJson` dan `attachmentIdsJson` untuk semua transaksi.
  - Ini memperlambat tab switch walau animasi tab sendiri sederhana.

**4. Widget budget/predictor di Home tidak smooth**
- File terkait: `app/src/main/java/com/dompetku/ui/screen/home/HomeScreen.kt`
- Temuan:
  - Gesture memakai `detectHorizontalDragGestures`.
  - Tiap delta drag memanggil `scope.launch { offsetPx.snapTo(newOffset) }`.
  - Setelah drag selesai, `carouselPage` langsung diubah dan konten kartu utama di-swap hard cut.
  - Tinggi `BudgetCard` dan `PredictorCard` berbeda, tetapi parent tidak meng-animate perubahan tinggi/layout.
  - Peek-card sekarang hanya muncul saat `isDragging`, jadi continuity visual lebih rendah dibanding transisi animasi penuh.
- Kesimpulan:
  - Masalah ada pada kombinasi gesture loop, hard page swap, dan absennya animated size/layout transition.

**5. Jiggle mode reorder akun belum sesuai desain**
- File terkait: `app/src/main/java/com/dompetku/ui/screen/accounts/AccountsScreen.kt`
- Temuan:
  - Jiggle animation ada dan cukup baik.
  - Reorder masih menggunakan tombol panah kiri/kanan di card.
  - Layout akun masih dibangun dari `accounts.chunked(2).forEach` di `Column`.
  - Tidak ada long-press drag state, tidak ada drag overlay/scale-up aktif, tidak ada animated placement untuk card lain.
- Kesimpulan:
  - Ini bukan bug kecil, tetapi memang arsitektur reorder saat ini belum sesuai requirement drag-and-drop.

**6. Back di tab Analisis langsung keluar app**
- File terkait:
  - `app/src/main/java/com/dompetku/ui/MainScaffold.kt`
  - `app/src/main/java/com/dompetku/ui/screen/analytics/AnalyticsScreen.kt`
  - `app/src/main/java/com/dompetku/ui/navigation/NavGraph.kt`
- Temuan:
  - Semua tab utama hidup di satu `MainScaffold` dengan state lokal `currentTab`.
  - Tidak ada `BackHandler` yang mengatur:
    - tab non-Home → kembali ke Home
    - tab Home → tampilkan dialog konfirmasi keluar
- Akibat:
  - Saat user ada di Analisis lalu tekan back sistem, Android langsung mem-pop activity dan app keluar.

**7. Language toggle aktif sebagian, tapi belum konsisten**
- Hal yang sudah benar:
  - state bahasa disimpan via `UserPreferences.setLang()`
  - `MainActivity` memanggil `AppCompatDelegate.setApplicationLocales(...)`
  - `values/strings.xml` dan `values-en/strings.xml` sudah ada
- Masalah nyata:
  - Masih banyak string hardcoded di Kotlin, terutama di `HomeScreen.kt`, `ProfileScreen.kt`, `AccountsScreen.kt`, `AccountDetailScreen.kt`, `TransactionsScreen.kt`, dan beberapa sheet/detail screen.
  - `CommonComponents.kt` masih memaksa formatter tanggal `Locale("id", "ID")`.
  - Beberapa label pakai `stringResource`, tetapi sibling text di screen yang sama masih hardcoded, jadi hasil toggle campur Indonesia/English.
- Kesimpulan:
  - Toggle bahasa bekerja, tetapi coverage translasi belum complete dan ada locale-sensitive formatter yang masih dikunci ke Indonesia.

**Masalah performa tambahan yang ikut tercatat**
- `NavGraph.kt` masih membentuk `allTxns` dari `txnUiState.grouped.flatMap { ... }` untuk route `Main` dan `AccountDetail`.
- `MainScaffold` masih punya parameter `allTxns`, tetapi parameter itu tidak dipakai.
- `AccountDetailScreen.kt` masih menerima semua transaksi, filter + sort ulang di composable, render list via `verticalScroll`, dan memakai lookup `accounts.find` per row.
- `TransactionsScreen.kt` sudah lebih baik, tetapi virtualisasi masih per grup tanggal; semua transaksi dalam satu tanggal tetap dirender sekaligus.

**Planning perbaikan (belum dieksekusi)**

**Prioritas 1 — stabilkan flow PIN**
1. Pisahkan initial route vs runtime lock navigation.
2. Hentikan perubahan `startDestination` yang reaktif setelah app sudah hidup.
3. Ubah outcome sukses `PinSetup` menjadi navigation flow yang eksplisit dan aman, bukan sekadar `popBackStack()`.

**Prioritas 2 — percepat PIN**
1. Pindahkan `PinHasher.hash()` dan `verifyAndUpgrade()` ke `Dispatchers.Default`.
2. Pastikan callback sukses/gagal PIN hanya mengurus state ringan di main thread.

**Prioritas 3 — ringankan Home/Profile**
1. Ganti dependency full `observeAll()` di `ProfileViewModel` dengan query count/ringkasan yang ringan.
2. Hapus materialisasi `allTxns` yang tidak dipakai di `NavGraph/MainScaffold`.
3. Evaluasi query spesifik untuk `AccountDetail`.

**Prioritas 4 — perbaiki widget Home**
1. Refactor gesture supaya tidak spawn coroutine per drag delta.
2. Tambahkan transisi halaman yang tetap smooth sesudah drag selesai.
3. Tambahkan animated size/placement agar pergantian Budget ↔ Predictor tidak membuat konten bawah loncat.

**Prioritas 5 — reorder akun sesuai desain**
1. Ganti model tombol panah menjadi drag-and-drop.
2. Long-press untuk masuk drag state.
3. Tambahkan scale/haptic feedback item aktif.
4. Tambahkan animated placement untuk card lain agar reposisi natural.

**Prioritas 6 — back navigation policy**
1. Tambah `BackHandler` di shell utama.
2. Policy target:
   - jika tab != Home → back ke Home
   - jika tab == Home → tampil dialog konfirmasi keluar

**Prioritas 7 — language consistency sweep**
1. Inventaris string hardcoded per screen/component.
2. Pindahkan ke resources `values` + `values-en`.
3. Hapus formatter teks/tanggal yang masih memaksa locale Indonesia.
4. Verifikasi ulang page utama dan sheet/detail yang sering dipakai.

**Catatan keputusan**
- Sesi ini belum mengubah code aplikasi; hanya audit source + build verification.
- Urutan implementasi yang paling aman nanti:
  1. stabilkan PIN flow
  2. optimasi PIN
  3. repair widget Home
  4. back navigation policy
  5. language sweep
  6. drag-and-drop reorder akun

**Progress implementasi setelah audit**
- `DONE` Stabilkan flow PIN level root:
  - `DompetKuNavHost` sekarang mengunci `startDestination` hanya untuk cold start, tidak lagi ikut berubah saat `pinEnabled` berubah di tengah sesi.
- `DONE` Optimasi PIN:
  - hash dan verify PIN dipindahkan ke background thread (`Dispatchers.Default`) agar unlock/setup tidak memblokir main thread.
- `DONE` Back navigation shell:
  - tab non-Home sekarang akan kembali ke Home dulu saat back.
  - Home sekarang menampilkan dialog konfirmasi keluar.
- `DONE` Bottleneck awal Profile/NavGraph:
  - Profile count sekarang pakai query count ringan, tidak lagi hydrate full transaction list hanya untuk menghitung jumlah.
  - materialisasi `allTxns` yang tidak dipakai di `MainScaffold` sudah dihapus.
  - Account detail sekarang memakai query transaksi account-spesifik, bukan flatten seluruh state transaksi.
- `DONE` Repair awal widget Home:
  - swipe logic diringankan, transisi kartu diubah ke animated content, dan container kartu ikut `animateContentSize` agar perubahan tinggi lebih smooth.
  - Ini adalah perbaikan tahap 1; feel di device nyata masih perlu dicek user.
- `PARTIAL` Language consistency:
  - beberapa label baru yang disentuh sudah dipindahkan ke resources.
  - formatter tanggal shared tidak lagi dipaksa ke locale Indonesia.
- `TODO` Drag-and-drop reorder akun belum dikerjakan di patch ini; implementasi saat ini masih tombol panah.

---

## LOG PERUBAHAN

### 2026-03-22 — Fix batch 1: PIN flow, back nav, Home widget, lightweight data paths
- **PIN flow stabilized:** `DompetKuNavHost` sekarang memakai `startDestination` hanya saat cold start. Ini mencegah root graph berubah di tengah sesi ketika `pinEnabled` baru saja diaktifkan dari `PinSetup`.
- **PIN performance:** `PinViewModel` memindahkan `PinHasher.hash()` dan verifikasi PIN ke `Dispatchers.Default`, sehingga PBKDF2 100k iterasi tidak lagi memblokir main thread saat digit ke-6 ditekan.
- **Back navigation utama:** `MainScaffold` sekarang intercept system back. Jika user ada di tab selain Home, back kembali ke Home dulu; jika sudah di Home, tampil dialog konfirmasi keluar.
- **Home widget repair (tahap 1):** swipe budget/predictor diganti ke gesture yang lebih ringan dan transisi konten animatif, plus container ikut `animateContentSize()` agar pergantian tinggi kartu tidak hard cut ke konten bawah.
- **Profile performance:** `TransactionDao` + `AccountDao` punya `observeCount()` baru. `ProfileViewModel` sekarang memakai count ringan, bukan `observeAll()` transaksi penuh hanya untuk menghitung jumlah.
- **NavGraph/MainScaffold cleanup:** materialisasi `allTxns` yang tidak dipakai di route `Main` dihapus.
- **Account detail performance:** tambah query `observeLinkedToAccount(accountId)` dan route detail akun sekarang mengamati transaksi akun-spesifik, bukan flatten seluruh grouped transaction state lalu filter di UI.
- **Language consistency (awal):** tambah beberapa string resource baru untuk label yang disentuh dan formatter tanggal shared tidak lagi memaksa `Locale("id", "ID")`.
- **Build verification:** `assembleDebug` berhasil setelah patch batch ini.

### 2026-03-22 — Audit penuh: PIN flow, performa, widget, back nav, language
- Belum ada code change aplikasi. Sesi ini khusus audit source untuk 6 issue utama yang dilaporkan user.
- Tambah section `AUDIT AKTIF — 2026-03-22` ke `HANDOFF.md` berisi akar masalah, hipotesis teknis, prioritas, dan planning.
- Temuan audit utama:
  - White screen saat enable PIN pertama kemungkinan berasal dari `startDestination` yang ikut berubah saat `pinEnabled = true`, lalu bentrok dengan flow `popBackStack()` dari `PinSetup`.
  - Delay unlock PIN kemungkinan berasal dari PBKDF2 verify/hash yang masih berjalan di main thread.
  - Home ke Profile lambat karena `ProfileViewModel` masih bergantung pada full `observeAll()` transaksi.
  - Widget budget/predictor tidak smooth karena gesture loop spawn coroutine per drag delta, page swap masih hard cut, dan tinggi kartu tidak dianimasikan.
  - Jiggle mode masih arsitektur tombol panah, belum drag-and-drop.
  - Back di tab Analisis langsung keluar app karena shell utama belum punya `BackHandler`.
  - Language toggle aktif sebagian, tetapi masih banyak string hardcoded dan formatter yang memaksa locale Indonesia.

### 2026-03-22 — Corpus-level date format detection
- **Refactor arsitektur deteksi tanggal** dari per-row menjadi per-sheet (corpus-level). Sebelumnya swap month↔day dilakukan per baris berdasarkan apakah hasilnya masa depan — ini tidak aman untuk file DompetKu yang mungkin punya sedikit transaksi masa depan (tagihan, budget).
- **`DateFormatHint`**: data class baru di `SmartImportEngine` berisi `swapDatetimeCells: Boolean` dan `swapStringDates: Boolean`.
- **`detectDateFormat(sheet, dateColIdx, headerRowIdx)`**: scan seluruh kolom tanggal sebelum parsing dimulai. Hitung berapa persen tanggal yang jatuh di masa depan setelah default parsing. Kalau >30% dan ≥3 sampel → set flag swap. Threshold berbeda untuk datetime cells dan string dates.
- **ISO format selalu skip**: string `YYYY-MM-DD` tidak pernah masuk hitungan → DompetKu export tidak akan pernah di-swap.
- **Unambiguous dates skip**: string seperti `03/20/2026` (b=20>12) tidak masuk hitungan karena sudah jelas US style.
- **`Row.str(swapDatetime)` dan `parseDate(swapAmbiguous)`**: kedua fungsi sekarang terima flag eksplisit dari corpus-level detection, bukan memutuskan sendiri per baris.
- **Keputusan dibuat satu kali per sheet**, diteruskan ke setiap `parseRow()` call.

### 2026-03-22 — Date parse fix (Excel datetime month↔day swap)
- **Root cause ditemukan dari file asli**: Money Manager menyimpan sebagian baris sebagai Excel date serial (bukan string). Apache POI membaca cell ini sebagai `localDateTimeCellValue`. Karena Money Manager menggunakan locale ID (DD/MM/YYYY), Excel menyimpan tanggal dengan month dan day ter-tukar. Contoh: transaksi "12 Maret 2026" (DD=12, MM=03) tersimpan sebagai month=12, day=3 → POI return 3 Desember 2026 (masa depan).
- **Fix**: Di `Row.str()` untuk `isCellDateFormatted`, setelah baca `localDateTimeCellValue`, cek apakah hasilnya lebih dari 7 hari ke depan. Kalau ya DAN month dan day bisa di-swap jadi tanggal valid non-masa-depan, lakukan swap. Ini menangani semua 479 datetime cells di file Money Manager yang ter-swap.
- **Dua class of date errors di file yang sama**: String cells ("03/20/2026") = US MM/DD format, ditangani di `parseDate()` dengan sanity-check future-date swap. Datetime cells (POI type) = month-day swapped by Excel locale mismatch, ditangani di `Row.str()` level sebelum string parsing.

### 2026-03-21 — Date parse fix + Vibration toggle
- **BUG FIX (kritikal): parseDate swap identik dengan candidate** — baris `String.format("%04d-%02d-%02d", c, b, a)` di sanity-check adalah sama persis dengan candidate, jadi swap tidak pernah berhasil. Fix: ganti ke `(c, a, b)` untuk benar-benar menukar month↔day. Sekarang `03/05/2026` (Money Manager = March 5) yang ter-parse salah jadi May 3 akan di-swap ke March 5 dengan benar.
- **Algoritma deteksi ID vs US**: Default tetap ID style (DD/MM/YYYY). Kalau hasil parse > 7 hari ke depan, coba US style (MM/DD) sebagai fallback. Kalau US style hasilnya tidak masa depan, pakai itu. Ini menangani Money Manager (MM/DD) dan input manual DompetKu (DD/MM) secara otomatis.
- **Toggle Getaran**: Tambah `vibrationEnabled: Boolean` di `AppPreferences`, key di `UserPreferences`, setter di `ProfileViewModel`, dan toggle baru di ProfileScreen section Preferensi (ungu, antara Suara dan Pengingat Harian). Saat user enable, langsung kasih demonstrasi haptic.
- **HapticHelper**: Semua fungsi public sekarang terima `enabled: Boolean = true` — tinggal pass `prefs.vibrationEnabled` di call site untuk gate seluruh haptic feedback.

### 2026-03-21 — Import date/time fix + Loading Overlay
- **Bug: Waktu Excel numeric cells hilang** — `Row.str()` untuk `DateUtil.isCellDateFormatted` sebelumnya hanya return tanggal (`%04d-%02d-%02d`). Sekarang include waktu (`%04d-%02d-%02d %02d:%02d:%02d`) sehingga `extractTimeFromDateStr` bisa parse jam yang benar.
- **Bug: Tanggal masa depan (MM/DD ambiguous)** — `parseDate("03/05/2026")` menghasilkan `2026-05-03` (Mei = masa depan) karena `b=5 ≤ 12` tidak bisa dibedakan. Fix: setelah parse, cek apakah hasil > today+7 hari. Kalau ya, coba swap month↔day. Kalau hasil swap valid dan bukan masa depan, gunakan itu.
- **Loading Overlay** — composable `DompetKuLoadingOverlay` baru di `ProfileScreen.kt`. Muncul saat `isImporting` atau `isExporting` true. Desain DompetKu-branded: logo hijau dengan pulse animation + spinning arc hijau + teks pesan.
- **HANDOFF** — section "UNTUK GPT CODEX" ditambahkan dengan 4 task lengkap: PIN Setup Screen, Haptics, Biometric confirm, Language Switch.

### 2026-03-21 — Smart Import Fix Round 2 + Dialog Info Ekspor/Impor
- **Bug 1 — `[Amount: xxx]` di semua nama transaksi:** Kolom `Amount` (index 8) Money Manager = duplikat IDR, kolom `Description` selalu kosong. Keduanya ditambahkan ke `IGNORED_HEADERS` sehingga tidak masuk `extras` dan tidak mencemari nama transaksi.
- **Bug 2 — Waktu selalu `00:00`:** Regex `extractTimeFromDateStr` reject `23:37:56` karena diikuti `:56`. Fix: ubah pattern ke `(?<![0-9])(\d{1,2}):(\d{2})(?::\d{2})?` yang accept detik opsional.
- **Bug 3 — Transfer-Out tidak terdeteksi:** `Role.TYPE` dipindahkan ke posisi pertama dalam `priority` detection loop agar `Income/Expense` kolom diklaim sebelum AMOUNT. Juga tambah `"income\/expense"` ke ROLE_KEYWORDS TYPE.
- **Dialog Ekspor:** BottomSheet baru muncul sebelum ekspor dijalankan. Menampilkan jumlah transaksi & akun, isi 2 sheet, cara berbagi, disclaimer format.
- **Dialog Impor:** BottomSheet baru muncul sebelum file picker dibuka. Menampilkan format yang didukung (Money Manager), kolom yang dikenali, cara kerja transfer, disclaimer akurasi.
- Subtitle "Impor Data" diupdate jadi "Muat dari Money Manager XLSX" agar lebih jelas.

### 2026-03-21 — Smart Import: Money Manager Compatibility
- **Bug 1 — Date parsing MM/DD/YYYY**: `03/20/2026` diparsing jadi `2026-20-03` (invalid). Fix: cek `b > 12` sebelum `c > 1900` sehingga MM/DD/YYYY ter-detect dengan benar.
- **Bug 2 — Duplicate "Account" column**: Money Manager export punya dua kolom bernama "Account" (index 1 = nama akun, index 10 = angka amount). Yang kedua masuk ke `extras` dan polusi note dengan `[Account: 43500.0]`. Fix: `detectColumns` skip kolom yang header-nya sudah dipakai role lain.
- **Bug 3 — Transfer-Out tanpa pasangan**: Money Manager hanya export 1 baris per transfer (Transfer-Out), tidak ada Transfer-In. Kolom Category berisi nama akun TUJUAN. Fix: deteksi `transfer-out` di TYPE column → set `toRawAccountName` dari Category, skip category mapping untuk row ini.
- **Bug 4 — toRawAccountName tidak di-register**: akun tujuan transfer tidak masuk ke `accountRawNames` sehingga tidak muncul di dialog resolusi. Fix: tambah loop untuk collect `toRawAccountName` ke `accountRawNames`.
- **Bug 5 — toId tidak di-resolve saat commit**: transfer hanya debit from-account, to-account tidak di-credit. Fix: `commitSmartImport` sekarang resolve `toRawAccountName` ke `toId`, set `fromId`, dan credit balance to-account.
- **Bug 6 — IDR/rupiah tidak dikenali sebagai AMOUNT**: tambah `"idr","rp","rupiah"` ke ROLE_KEYWORDS AMOUNT.
- **Bug 7 — Category mapping Money Manager**: tambah mapping untuk `allowance, petty cash, modified bal, household, social life, beauty, data, bensin` ke CATEGORY_MAP.
- **SmartTransaction**: tambah field `toRawAccountName: String?`
- **ProfileViewModel.resolveAccountId**: helper baru yang juga fuzzy-match ke akun yang baru di-create, bukan hanya dari `accountResolutions`.

### 2026-03-21 — Smart Notification Fix
- **Root cause:** Dua masalah yang bikin notifikasi tidak jalan:
  1. `testNotifications()` pakai `OneTimeWorkRequest` biasa tanpa priority — bisa di-delay lama oleh WorkManager/battery optimization
  2. Android 13+ wajib `POST_NOTIFICATIONS` runtime permission — tidak pernah di-request, jadi notifikasi selalu di-block OS
- **Fix 1 — testNotifications():** Tambah `.setExpedited(RUN_AS_NON_EXPEDITED_WORK_REQUEST)` agar WorkManager jalankan segera
- **Fix 2 — ReminderWorker:** Tambah `getForegroundInfo()` override (wajib untuk Expedited workers)
- **Fix 3 — ProfileScreen toggle:** Saat user ON-kan Pengingat Harian di Android 13+, sekarang muncul dialog izin notifikasi dari OS sebelum enable. Android 12 ke bawah langsung enable tanpa dialog.
- **Cara test setelah fix:** Profil → TENTANG → [DEBUG] Test Notifikasi → 5 notifikasi harusnya muncul dalam 5–10 detik

### 2026-03-21 — Performance Audit & Fixes
- **DB v3:** `AppDatabase.version` naik ke 3 — indexes sudah ada di entity (`@Entity indices`) tapi version belum di-bump, sekarang fixed
- **AccountsViewModel:** hapus `allTransactions: StateFlow` yang buang-buang RAM — ganti dengan `suspend fun txnCountForAccount(id)` yang query DB count langsung. `AccountsScreen` pakai `LaunchedEffect` + `mutableIntStateOf` untuk load count on-demand saat delete dialog muncul.
- **TransactionDao:** tambah `countByAccount(accountId): Int` query
- **TransactionRepository:** tambah `countByAccount(accountId): Int` delegate
- **AnalyticsViewModel:** tambah `debounce(300)` pada `observeAll()` — batch import tidak lagi trigger 100+ recompute. Guard `computeLifestyle` & `computeSalaryInsight` dengan `if (totalExp > 0)` — tidak jalan saat data kosong atau filter menghasilkan 0 pengeluaran.
- **TransactionsViewModel:** hapus `sortedByDescending { date + time }` di `applyFilters()` — DAO sudah `ORDER BY date DESC, time DESC`, sort ulang di Kotlin tidak perlu (O(n log n) dihilangkan).
- **AccountsScreen — jiggle:** pindah `rememberInfiniteTransition` ke dalam `if (editMode)` branch — animasi Choreographer tidak berjalan saat user cuma browse akun.
- **TransactionsScreen:** ganti `forEach { item(...) item(...) }` dengan `items(state.grouped, key = { "grp_$date" })` — LazyColumn sekarang benar-benar virtualize per group tanggal.

### 2026-03-07 — Phase 5A
- AccountFormSheet, TransactionFormSheet, AccountsScreen, BrandDetector, PinLockScreen, ProfileScreen

### 2026-03-07 — Phase 5B/5C  
- Transfer dual-account display, carousel, language toggle, predictor, hide balance, attachment, smart category

### 2026-03-07 — Phase 5E
- Analytics donut chart Canvas-based

### 2026-03-07 — Phase 5F
- Export/Import XLSX, Smart Import, Sound, Drag reorder, Color wheel

### 2026-03-07 — Phase 5G
- Performance LazyColumn/flowOn, bug fixes batch 1-4, Smart Budget Notifications (5 slot), Smart Import bug fixes (5 bug)

### 2026-03-08 — Phase 5H, sesi 1
- **FIX BUG-BIO:** `MainActivity : ComponentActivity` → `MainActivity : FragmentActivity` → fingerprint sekarang bekerja
- **Security Audit:** lihat section SECURITY AUDIT di atas
- **Handoff:** merge semua 6 handoff lama → 1 file ini

### 2026-03-08 — Phase 5H, sesi 6 (Import: Account Type + Balance Sync)
- **Account type detection saat import:** `commitSmartImport` sekarang pakai `BrandDetector.detect()` + `detectAccountType()` untuk menentukan tipe akun baru yang dibuat dari hasil impor. GoPay → ewallet, BCA/Mandiri → bank, Dompet → cash, dst. Sebelumnya semua akun baru default ke `AccountType.other` (abu-abu).
- **Brand & gradient otomatis:** Akun baru dari impor juga dapat `brandKey` dan gradient warna dari BrandDetector — sama seperti akun yang dibuat manual.
- **Balance sync post-import:** Setelah semua transaksi diinsert, saldo tiap akun dihitung dari net semua transaksi (income +, expense -, transfer -) dan di-apply via `accountRepo.adjustBalance()`. Sebelumnya akun selalu kosong Rp 0 setelah impor.

### 2026-03-08 — Phase 5H, sesi 5 (Performance)
- **FanNav — pulse Animatable:** Ganti `rememberInfiniteTransition` dengan `Animatable` + coroutine loop. Pulse berhenti total saat fan open (tidak ada frame draw yang terbuang).
- **FanNav — backdrop animated:** Backdrop sekarang fade in/out smooth (`tween(220)`) via `animateFloatAsState` + `graphicsLayer`. Sebelumnya muncul tiba-tiba.
- **FanNav — iOS spring:** Spring tuning: `dampingRatio = 0.62f, stiffness = 450f` untuk fan items (lebih snappy). FAB icon pakai `DampingRatioMediumBouncy`. Nav tab color pakai `animateColorAsState`.
- **FanNav — offset ke graphicsLayer:** Fan items sekarang pakai `graphicsLayer { translationX/Y }` bukan `Modifier.offset {}` — transform di draw phase, tidak trigger layout.
- **NavGraph — AnimatedContent:** Tab switch sekarang pakai `AnimatedContent` dengan crossfade (fadeIn 180ms + fadeOut 120ms). Sebelumnya `when(currentTab)` biasa — ini sumber jank terbesar.
- **HomeScreen — REVERTED:** Semua perubahan HomeScreen (state hoist, LazyColumn split) di-revert. Behaviour carousel & widget budget/prediksi kembali ke kondisi semula per permintaan user.

### 2026-03-08 — Phase 5H, sesi 4 (Export/Import Fix)
- **Export redesign:** Sheet Transaksi kolom baru — `Tanggal, Waktu, Jenis Transaksi, Nominal, Nama Transaksi, Kategori, Akun, Dari Akun, Ke Akun, Biaya Admin`. Dihapus: `id, accountId, fromId, toId`. Sheet Akun disederhanakan: `Nama, Tipe, Saldo, Nomor Akhir, Brand`.
- **Format nominal accounting-style:** Pengeluaran → `(14.500)`, Pemasukan → `500.000`, Transfer → `⇔ 50.000`.
- **Import fix — TXNNAME role baru:** `SmartImportEngine` sekarang bedain kolom `Nama Transaksi` (maps ke `Transaction.note`) vs `Catatan` (info tambahan). Sebelumnya keduanya salah digabung ke satu field note.
- **Import fix — note format baru:** `Nama Transaksi` jadi primer, `Catatan` di-append dengan separator ` — `, extra columns tetap pakai `[header: value]`.
- **SmartCategoryDetector link:** Setiap transaksi hasil import kini melewati `SmartCategoryDetector.detect()` menggunakan nama transaksi — persis seperti input manual. Jika terdeteksi (confidence ≥ 0.7 atau kategori asli = Lainnya), kategori di-override dan `detected = true` di-set.
- **parseAmountString fix:** Handle format `(14.500)` sebagai negatif/pengeluaran, dan `⇔`/`↔` prefix (transfer) tanpa crash.
- **ProfileViewModel:** `commitSmartImport` sekarang pass `detected` flag ke Transaction.

### 2026-03-08 — Phase 5H, sesi 3 (DataStore Encryption)
- **DataStore encryption:** Split DataStore jadi 2 — `SecurePreferences.kt` (baru) pakai `EncryptedSharedPreferences` (AES256-GCM/SIV, MasterKey di Android Keystore) untuk data sensitif: PIN hash, PIN/bio enabled, user profile. DataStore biasa hanya menyimpan data non-sensitif (sound, lang, budget, dll).
- **Migration transparan:** `UserPreferences` auto-migrate data lama dari DataStore → EncryptedSharedPreferences saat pertama kali jalan. Legacy keys langsung dihapus dari DataStore setelah migration. Flag `secure_migrated` mencegah re-run.
- **Public API tidak berubah:** Semua ViewModel tidak perlu disentuh.
- **Dependency baru:** `androidx.security:security-crypto:1.1.0-alpha06` di `libs.versions.toml` + `build.gradle.kts`.
- **Semua PENDING issues sekarang 0 item open.**

### 2026-03-08 — Phase 5H, sesi 2 (Security Hardening)
- **PBKDF2 migration:** `PinHasher.kt` rewrite total — PBKDF2WithHmacSHA256, 100.000 iterasi, 16-byte random salt per hash, constant-time comparison. Format: `pbkdf2$iter$salt_hex$hash_hex`. Legacy SHA-256 hash tetap bisa diverifikasi dan auto-upgrade ke PBKDF2 saat login berikutnya via `verifyAndUpgrade()`
- **PinViewModel.kt:** `verifyPin()` sekarang pakai `verifyAndUpgrade()` — user lama ter-upgrade transparan tanpa perlu set ulang PIN
- **FLAG_SECURE:** `window.addFlags(FLAG_SECURE)` di `MainActivity.onCreate` — cegah screenshot & app-switcher preview data keuangan
- **INTERNET permission:** sempat dihapus, lalu dikembalikan — dibutuhkan `CurrencySheet` untuk fetch kurs realtime ke `exchangerate-api.com`. Komentar justifikasi ditambah di Manifest agar jelas.
- **BuildConfig.DEBUG:** tombol [DEBUG] Test Notifikasi di ProfileScreen sekarang pakai `BuildConfig.DEBUG` yang proper (bukan `if (true)`) + import `com.dompetku.BuildConfig` ditambahkan
### 2026-03-21 â€” PIN Setup Screen + Haptics + Biometric Confirm + Language Switch
- **PIN setup full screen:** tambah `PinSetupScreen.kt` dengan keypad custom DompetKu, flow setup 2 langkah, plus mode ganti PIN yang verifikasi PIN lama dulu sebelum set PIN baru. `PinLockScreen.kt` juga di-refactor agar berbagi layout keypad yang sama via `PinScreenComponents.kt`.
- **Navigasi PIN baru:** tambah route `Screen.PinSetup` di `Screen.kt` dan composable baru di `NavGraph.kt`. `ProfileScreen.kt` sekarang membuka full-screen PIN setup saat toggle PIN ON dan saat user pilih "Ganti PIN".
- **Haptic feedback:** helper baru `util/HapticHelper.kt` ditambahkan. Dipakai untuk keypad PIN, toggle PIN/biometrik di `ProfileScreen.kt`, dan FAB utama di `ui/navigation/FanNav.kt`.
- **Biometric confirmation:** toggle biometrik di `ProfileScreen.kt` sekarang menampilkan `BiometricPrompt` dulu saat enable. Hanya `onAuthenticationSucceeded` yang mengaktifkan `bioEnabled`.
- **Language switch aktif:** tambah `values/strings.xml` + `values-en/strings.xml`, dependency `androidx.appcompat:appcompat`, dan locale apply via `AppCompatDelegate.setApplicationLocales()` di `MainActivity.kt`. Label inti di Home/Transactions/Accounts/Analytics/Profile serta komponen shared utama mulai dipindahkan ke resources.
- **Build verification:** `assembleDebug` berhasil setelah clean generated KSP cache lokal.

### 2026-03-22 â€” Tahap A: drag-and-drop reorder akun
- **AccountsScreen refactor:** implement reorder kartu akun berbasis long-press drag-and-drop, menggantikan tombol panah kiri/kanan lama. Struktur list sekarang memakai `LazyVerticalGrid` 2 kolom dengan state lokal `draftAccounts`, `draggedId`, `dragOffset`, dan `itemBounds`.
- **Gesture & feedback:** drag hanya aktif saat `editMode`; long-press pertama tetap bisa masuk mode edit, lalu kartu dapat ditahan dan digeser. Saat drag dimulai dan saat swap antar posisi terjadi, `HapticHelper` dipanggil untuk feedback yang lebih terasa.
- **Animasi:** kartu yang sedang di-drag sedikit scale up, mendapat `zIndex` lebih tinggi, dan kartu lain memakai `animateItemPlacement(...)` supaya perpindahan posisi terasa lebih halus dan tidak hard cut.
- **UI cleanup:** aksi bawah kartu kini fokus ke `Edit` dan `Hapus`, plus indikator drag handle saat edit mode. Ditambah helper text reorder dan string resources baru untuk hint + dialog hapus akun.
- **Build verification:** `assembleDebug` berhasil setelah refactor Tahap A.

### 2026-03-22 â€” Tahap B: language consistency sweep
- **Resource expansion:** tambah batch string resource baru di `values/strings.xml` dan `values-en/strings.xml` untuk area Profile, Home budget/predictor, dan Analytics summary/legend/card labels.
- **ProfileScreen cleanup:** chooser ekspor, snackbar hasil import, loading overlay, section About/Data labels tertentu, toggle vibration, subtitle ekspor/impor, serta dialog hapus semua sekarang memakai string resource agar ikut locale aktif.
- **HomeScreen cleanup:** placeholder nama user, label budget card, indikator sisa hari ini, subtitle pengeluaran hari ini, beberapa label predictor/budget sheet utama mulai dipindah ke resource agar toggle bahasa tidak lagi campur di area yang paling sering terlihat.
- **AnalyticsScreen cleanup:** filter "Tahun Ini", legend pemasukan/pengeluaran, title salary insight, beberapa label savings/trend, dan sejumlah label ringkasan kini memakai string resource.
- **Catatan sisa kerja:** masih ada hardcoded copy yang lebih panjang di beberapa dialog import/export dan beberapa deskripsi detail insight yang belum disapu penuh di batch ini, tetapi area yang paling terlihat oleh user sehari-hari sudah jauh lebih konsisten.
- **Build verification:** `assembleDebug` berhasil setelah Tahap B.

### 2026-03-22 â€” Tahap C: polish + verification
- **Home budget sheet polish:** label-label utama di `BudgetSheet` (`Atur Budget Bulanan`, target tabungan, analisis rekomendasi, custom budget, apply budget) kini ikut resource locale sehingga alur budget tidak lagi campur bahasa setelah toggle.
- **Profile import/export dialog polish:** title/subtitle dialog, unit transaksi/akun, CTA `Gabungkan/Ganti Semua Data`, info rows ekspor/impor, disclaimer, dan tombol pilih file/ekspor sekarang ikut resource `ID/EN`.
- **Analytics polish:** header `FUN FACT`, `MISI MINGGU INI`, dan message status savings rate kini ikut resource locale yang sama.
- **Verification pass:** `assembleDebug` kembali berhasil setelah batch polish Tahap C.
- **Residual note:** masih ada sejumlah hardcoded copy yang lebih dalam di flow import mapping/profile edit sheet yang belum disapu total, tetapi area utama yang dilihat user setiap hari dan seluruh area yang disentuh Tahap A/B/C sudah stabil dan konsisten secara build.
