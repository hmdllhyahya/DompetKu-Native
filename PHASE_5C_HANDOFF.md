# DompetKu — Phase 5C Handoff Report
**Dibuat:** 2026-03-07  
**Dari:** Phase 5B (Session selesai)  
**Untuk:** Phase 5C (Session baru)

---

## 1. RINGKASAN PHASE 5B — APA YANG SUDAH DIKERJAKAN

### ✅ Fitur Baru yang Diimplementasikan

| Fitur | File | Status |
|---|---|---|
| Akun tipe KUE (Kartu E-Money) | Models.kt, AccountFormSheet.kt, HomeScreen.kt, AccountsScreen.kt, AccountDetailScreen.kt | ✅ Done |
| Transfer tampil di kedua akun (from → to) | CommonComponents.kt, AccountDetailScreen.kt, TransactionsScreen.kt, HomeScreen.kt, NavGraph.kt | ✅ Done |
| Carousel swipe real-time follow finger | HomeScreen.kt | ✅ Done |
| Language toggle slide animation | ProfileScreen.kt | ✅ Done |
| Predictor popup detail sheet | HomeScreen.kt | ✅ Done |
| Hide balance toggle di AccountsScreen | AccountsScreen.kt, AccountsViewModel.kt | ✅ Done |
| Hide balance toggle di TransactionsScreen | TransactionsScreen.kt, TransactionsViewModel.kt | ✅ Done |
| toggleHideBalance() global | UserPreferences.kt | ✅ Done |
| AppHeader trailingContent parameter | CommonComponents.kt | ✅ Done |
| JOBS list expanded (14 opsi) | ProfileScreen.kt | ✅ Done |
| EDUS list sync dengan onboarding | ProfileScreen.kt | ✅ Done |
| Pekerjaan "Lainnya" → free text input | ProfileScreen.kt | ✅ Done |
| ProfileInfoChip layout proporsional | ProfileScreen.kt | ✅ Done |
| ProfileCard innerPadding parameter | ProfileScreen.kt | ✅ Done |

### ✅ Bug Fixes Phase 5B

| Bug | Root Cause | Fix | File |
|---|---|---|---|
| Warna akun card hilang (abu-abu) | `Color(Long)` salah konstruktor, bukan `Color(Int)` | `.toInt()` di semua pemakaian gradientStart/gradientEnd | AccountDetailScreen.kt, AccountsScreen.kt |
| Transfer balance race condition | `adjustBalance()` pakai read-modify-write (getById → update) — bisa race di concurrent ops | Ganti ke atomic SQL `UPDATE accounts SET balance = balance + :delta` | Daos.kt, Repositories.kt |
| X button terpotong (AccountFormSheet, TransferSheet) | `padding(top)` kurang di header Row | Tambah `padding(top = 8.dp)` | AccountFormSheet.kt, TransferSheet.kt |
| emoney icon tidak muncul di `when` expression | Enum baru tidak ditambah ke semua `when` | Tambah `AccountType.emoney → WifiHigh` di semua when | AccountsScreen.kt, AccountDetailScreen.kt |

---

## 2. BUG YANG DITEMUKAN TAPI BELUM DIPERBAIKI — MASUK 5C

### 🔴 CRITICAL: Transfer Edit — Biaya Admin tidak update Mandiri dengan benar

**Deskripsi:**  
Ketika transfer sudah dibuat (GoPay → Mandiri 100rb) kemudian di-edit untuk menambah biaya admin (misal +5rb), hasil yang terjadi:
- GoPay berubah benar (dikurangi tambahan admin fee)
- Mandiri **tidak berubah** — seharusnya tetap sama (karena credit ke Mandiri tidak berubah)

**Root Cause yang diduga:**  
Di `updateTransaction()` di `TransactionsViewModel.kt`, flow untuk transfer adalah:
1. Reverse old: `adjustBalance(fromId, +(old.amount + old.adminFee))` dan `adjustBalance(toId, -old.amount)`
2. Apply new: `applyTransfer(fromId, toId, new.amount, new.adminFee)`

Masalahnya: `applyTransfer` yang baru sekarang pakai `applyTransferBalance` dari DAO (atomic), tapi `adjustBalance` di step reverse **masih pakai** `dao.incrementBalance` secara sequential. Kemungkinan step 1 reverse untuk `toId` tidak dieksekusi karena `txn.toId` pada object `old` adalah null saat di-pass dari `TransactionDetailSheet` → edit flow.

**File yang perlu diperiksa:**
- `TransactionsViewModel.kt` — `updateTransaction(old, new)` — pastikan `old.toId` tidak null
- `TransactionDetailSheet.kt` — pastikan `old` transaction di-pass lengkap dengan `toId` ke onEdit
- `NavGraph.kt` / `MainScaffold.kt` — pastikan edit flow untuk transfer memakai `onTxnUpdated`, bukan `onTransferSaved`

---

## 3. FITUR YANG DIDEFER KE PHASE 5C

### 3A. LAMPIRAN FOTO / FILE DI FORM TRANSAKSI
**Deskripsi:** User bisa melampirkan struk/foto/file saat mencatat transaksi pengeluaran, pemasukan, maupun transfer.  
**Scope:**
- Tambah tombol attachment di `TransactionFormSheet.kt` dan `TransferSheet.kt`
- Pakai `ActivityResultContracts.GetContent` atau camera intent
- Simpan path ke `AttachmentRepository` (sudah ada schema-nya di DB)
- Tampilkan thumbnail di `TransactionDetailSheet.kt`

**File terkait:** `TransactionFormSheet.kt`, `TransferSheet.kt`, `TransactionDetailSheet.kt`, `AttachmentRepository`, `Entities.kt` (AttachmentEntity sudah ada)

---

### 3B. ACCOUNT CARD BERWARNA DI FORM PILIH AKUN
**Deskripsi:** Di form pencatatan pengeluaran/pemasukan (TransactionFormSheet), saat user tap "Pilih Akun", tampilkan card akun dengan gradien warna seperti di halaman Akun — bukan plain dropdown.  
**Scope:**
- Ganti dropdown akun di `TransactionFormSheet.kt` dengan horizontal scrollable card picker
- Card pakai `Brush.linearGradient(gradientStart, gradientEnd)` dari akun
- Tampilkan nama, saldo, icon type

**File terkait:** `TransactionFormSheet.kt`

---

### 3C. EXPAND ANIMATION AKUN → FULLSCREEN
**Deskripsi:** Ketika user tap MiniAccountCard di HomeScreen, animasi expand dari koordinat card tersebut hingga fullscreen AccountDetailScreen — seperti iOS shared element transition.  
**Scope:**
- Pakai `SharedTransitionLayout` + `AnimatedVisibilityScope` (Compose 1.7+)
- Atau implementasi manual dengan `Animatable` offset + size dari koordinat card ke fullscreen
- Card warna gradient ikut expand
- Berlaku untuk semua posisi card (termasuk yang terpotong di kanan)

**File terkait:** `HomeScreen.kt`, `AccountDetailScreen.kt`, `NavGraph.kt`

---

### 3D. ANALYTICS PIE CHART + BAR CHART + TOGGLE
**Deskripsi:** Di AnalyticsScreen, tambah visualisasi:
- Pie chart untuk distribusi kategori pengeluaran
- Bar chart untuk perbandingan pemasukan vs pengeluaran per bulan
- Toggle switch antara pie dan bar

**File terkait:** `AnalyticsScreen.kt`, `AnalyticsViewModel.kt`  
**Library:** Pakai `com.github.PhilJay:MPAndroidChart` atau `co.yml.ycharts` yang sudah lightweight untuk Compose

---

### 3E. SMART CATEGORY DETECTION
**Deskripsi:** Ketika user mengetik catatan transaksi, sistem otomatis detect kategori berdasarkan keyword:
- "Transjakarta koridor X" → Transportasi
- "MRT stasiun X" → Transportasi  
- "bioskop / CGV / XXI [judul film]" → Hiburan
- "konser [artis]" → Hiburan
- "tol [nama gerbang]" → Transportasi
- "pesawat / [maskapai]" → Transportasi
- EASTER EGG: "KCIJ" / "Whoosh" → kategori Transportasi + animasi "wush" ✨

**File terkait:** `SmartCategoryDetector.kt` (sudah ada, perlu diisi logic), `TransactionFormSheet.kt`

---

### 3F. TRANSACTION DETAIL PAGE DENGAN CONTEXTUAL FIELDS
**Deskripsi:** Halaman detail transaksi menampilkan field tambahan sesuai kategori yang terdeteksi (misal: nama maskapai, nomor penerbangan untuk kategori Transportasi → Pesawat).

**File terkait:** `TransactionDetailSheet.kt`, `Models.kt` (`detailsJson` sudah ada)

---

### 3G. HIDE BALANCE DI ACCOUNT DETAIL SCREEN
**Deskripsi:** Ketika hide balance aktif, nominal di `AccountDetailScreen` (header saldo besar + income/expense stats) juga tersembunyi.

**File terkait:** `AccountDetailScreen.kt` — tambah `hidden` parameter, connect ke `UserPreferences`

---

## 4. ARSITEKTUR & STATE PENTING UNTUK 5C

### File Kritis
```
app/src/main/java/com/dompetku/
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt          — version = 2, fallbackToDestructiveMigration
│   │   ├── dao/Daos.kt             — incrementBalance() atomic SQL sudah ada
│   │   └── entity/Entities.kt     — AttachmentEntity sudah ada
│   ├── preferences/UserPreferences.kt  — toggleHideBalance() sudah ada
│   └── repository/Repositories.kt — applyTransfer() pakai applyTransferBalance()
├── domain/model/Models.kt         — AccountType: bank, ewallet, cash, savings, emoney
├── ui/
│   ├── MainScaffold.kt            — global sheet host, routing transfer vs txn
│   ├── navigation/NavGraph.kt     — AccountDetail dapat accounts list
│   └── screen/
│       ├── home/HomeScreen.kt     — carousel real-time, predictor popup, MiniAccountCard
│       ├── accounts/
│       │   ├── AccountsScreen.kt  — grid 2 kolom, hide balance toggle
│       │   ├── AccountDetailScreen.kt  — filter toId juga, TransactionRow clickable
│       │   └── AccountFormSheet.kt     — KUE type + EMONEY brand list
│       ├── transactions/
│       │   ├── TransactionFormSheet.kt
│       │   ├── TransferSheet.kt
│       │   ├── TransactionDetailSheet.kt
│       │   └── TransactionsViewModel.kt — updateTransaction(old, new) ← perlu fix
│       └── profile/ProfileScreen.kt    — JOBS/EDUS list, Lainnya free text
└── util/
    ├── BrandDetector.kt           — 33 brands dengan alias fuzzy detection
    └── SmartCategoryDetector.kt   — placeholder, belum diisi logic
```

### Catatan Penting
- **Database version = 2**, `fallbackToDestructiveMigration()` aktif — kalau schema berubah, semua data terhapus. Pertimbangkan migration proper di 5C jika ada schema change.
- **`gradientStart` / `gradientEnd` disimpan sebagai `Long`** — selalu pakai `.toInt()` saat membuat `Color()`, jangan `Color(Long)` langsung.
- **Transfer transaction**: `accountId = fromId`, `fromId = fromId`, `toId = toId` — ketiganya diset di TransferSheet.
- **`AppHeader` punya `trailingContent` parameter** — bisa inject icon apapun di kanan header.

---

## 5. PRIORITAS PENGERJAAN PHASE 5C

**Urutan yang disarankan:**

1. 🔴 **Fix transfer edit bug** (critical, 30 menit) — debug `updateTransaction` untuk transfer type
2. 🟡 **Hide balance di AccountDetailScreen** (quick win, 15 menit)
3. 🟡 **Account card berwarna di TransactionFormSheet** (UX improvement, 1 jam)
4. 🟡 **Lampiran foto/file** (feature, 2-3 jam) — butuh permission handling
5. 🟢 **Analytics chart** (feature, 2 jam)
6. 🟢 **Smart category detection** (feature, 1-2 jam)
7. 🔵 **Expand animation** (complex, 3+ jam) — defer ke 5D kalau perlu

---

*Report ini dibuat otomatis di akhir Phase 5B session.*  
*Project path: `C:\WORK\Projects\DompetKu`*
