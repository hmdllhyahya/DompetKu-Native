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

## LOG PERUBAHAN

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
