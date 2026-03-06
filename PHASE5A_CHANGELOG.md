# Phase 5A + Hotfix Changelog
> Semua perubahan ini sudah diterapkan langsung ke project. Jangan overwrite file-file di bawah.

---

## Files yang DIMODIFIKASI (jangan di-overwrite tanpa baca dulu)

### 1. `MainActivity.kt`
- Hapus `enableEdgeToEdge()` — menyebabkan konten overlap status bar & nav bar
- Tambah auto-lock: `onStop()` simpan timestamp, `onStart()` trigger lock jika > 30 detik
- Inject `RootViewModel` via `by viewModels()`

### 2. `RootViewModel.kt`
- Tambah `_shouldLock: MutableStateFlow<Boolean>`
- Tambah `triggerLock()` dan `clearLock()` untuk auto-lock flow

### 3. `NavGraph.kt` (`DompetKuNavHost`)
- Observe `shouldLock` dari RootViewModel → navigate ke PinLock otomatis
- Tambah parameter `onOpenTransfer` di content lambda `MainScaffold`
- Wire `onOpenTransfer` ke `AccountsScreen`

### 4. `MainScaffold.kt`
- Tambah `onOpenTransfer: () -> Unit` ke content lambda
- Pass `{ showTransfer = true }` ke content

### 5. `AccountsScreen.kt`
- Fix gradient card: `gradientStart.toInt()` & `gradientEnd.toInt()` (Color(Long) → Color(Int))
- Jiggle mode: `InfiniteTransition` dengan `tween(100)` — continuous oscillate saat edit mode
- `graphicsLayer` dipindah SEBELUM `shadow` & `clip` supaya seluruh card ikut rotate
- Tombol header: "Tambah" (hapus "+"), spasi dikurangi, font lebih kecil
- Transfer button di header sekarang berfungsi (via `onOpenTransfer`)

### 6. `AccountFormSheet.kt`
- **REDESIGN BESAR**: Type selector berubah dari horizontal scroll chips → 2-column grid dengan icon
- Tambah `AccountType.credit` support
- List bank diperluas: Maybank, BTN, Bukopin, Muamalat, BJB, Jago, Jago Syariah, Jenius, SeaBank, Blu, HSBC, Citibank
- Ketika pilih "Lainnya" → muncul free-text field + tombol "Pilih list" untuk kembali
- Brand auto-detect aktif di free-text field juga
- `gradientStart/End` disimpan dengan `.toArgb().toLong()` (fix dari sebelumnya pakai `.value.toLong()`)
- Padding card dikompakkan (vertical 10dp, bukan 12dp)

### 7. `TransactionFormSheet.kt`
- Tambah `Spacer(10.dp)` antar setiap `FormCard` — fix overlapping cards
- Label kategori: tampilkan nama lengkap (`maxLines = 2, fontSize = 7.sp`) bukan kata pertama saja
- Transfer fields: tambah `Spacer` antar semua section
- `FormCard` padding dikurangi: `vertical = 10.dp`

### 8. `HomeScreen.kt`
- Hapus `statusBarsPadding()` (tidak diperlukan setelah `enableEdgeToEdge()` diremove)
- `padding(bottom = 120.dp)` untuk clearance nav bar + FAB

### 9. `PinLockScreen.kt`
- `repeat(4)` → `repeat(6)` di `PinDotRow`
- `length >= 4` → `length >= 6` di validasi input
- `next.length < 4` → `next.length < 6`

### 10. `ProfileScreen.kt`
- `onCheckedChange = { enabling -> }` → `onToggle = { }` (sesuai signature Toggle composable)
- PIN toggle: cek `!prefs.pinEnabled` → buka `showSetPinSheet`
- Bio toggle: `viewModel.setBioEnabled(!prefs.bioEnabled)`
- Sound toggle: `viewModel.setSoundEnabled(!prefs.soundEnabled)`

---

## Files BARU yang ditambahkan (tidak ada di fase sebelumnya)

Tidak ada file baru — semua perubahan di file existing.

---

## Domain model changes

### `Models.kt`
- `AccountType` enum: tambah `credit` → `bank, ewallet, cash, savings, investment, credit, other`

### `Color.kt` (BrandGradients)
- Tambah: `JagoSyariah`, `SeaBank`, `Blu`, `Maybank`, `BTN`, `Bukopin`, `Muamalat`, `BJB`, `BankJatim`, `BankNTT`, `Standard`, `HSBC`, `Citibank`

### `BrandDetector.kt`
- Brand baru: semua bank di atas + alias mapping (fuzzy: "bank jago syariah", "bank central asia", dll)
- Algoritma detect: alias check dulu, lalu match by key/displayName, ambil yang paling panjang (hindari "jago" match "jago syariah")

---

## Bug yang di-skip / belum dikerjakan
- Drag-to-reorder account card (bukan scope 5A)
- Analytics screen (scope fase lain)
- TransactionRow tertindih nav bar saat scroll (minor, acceptable)

---

## Catatan penting untuk fase berikutnya
- `Toggle` composable hanya punya `onToggle: () -> Unit` — TIDAK ada `onCheckedChange`
- `Color(Long)` harus `.toInt()` dulu — `Color(account.gradientStart.toInt())`
- `BrandInfo.gradientStart` bertipe `Color` (bukan Long) — TIDAK perlu `.toInt()`
- `runCatching` perlu explicit type param: `runCatching<T>`
- `PinLockScreen` pakai `LocalContext.current as? FragmentActivity`
- `enableEdgeToEdge()` sudah DIHAPUS dari MainActivity — jangan tambahkan lagi
