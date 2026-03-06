# Phase 5B Handoff — untuk Claude Coder

> Baca PHASE5A_CHANGELOG.md dulu sebelum ini.
> Semua yang di bawah adalah status terkini sebelum kamu mulai fase 5B.

---

## Yang SUDAH dikerjakan Watcher (jangan disentuh lagi)

### ✅ AccountFormSheet.kt — SELESAI (redesign besar)
- Type selector: sudah grid 2 kolom dengan icon (Dompet/Tunai, Rekening Debit, Kartu Kredit, E-Wallet, Tabungan, Investasi)
- Pilih "Lainnya" → muncul free-text field + tombol "Pilih list" untuk kembali ke dropdown
- Brand auto-detect aktif di free-text field (ketik "Bank Jago Syariah" → gradient terdeteksi)
- `AccountType.credit` sudah ditambah ke enum di Models.kt
- List bank sudah diperluas: Maybank, BTN, Bukopin, Muamalat, BJB, Jago, Jago Syariah, Jenius, SeaBank, Blu, HSBC, Citibank
- Padding card kompak (vertical 10dp)

### ✅ TransactionFormSheet.kt — Tab slide animation SELESAI
- Animated sliding indicator sudah ada (BoxWithConstraints + animateDpAsState + spring)
- Spacer antar FormCard sudah diperbaiki (tidak overlapping)
- Label kategori: nama lengkap, fontSize 7sp, maxLines 2

### ✅ AccountsScreen.kt — Jiggle mode SELESAI
- InfiniteTransition tween(100) — sudah continuous oscillate
- graphicsLayer SEBELUM shadow & clip — seluruh card ikut rotate

### ✅ TransferSheet.kt — Gradient SELESAI
- AccountPickerCard sudah pakai `Color(account.gradientStart.toInt())`

### ✅ BrandDetector.kt + Color.kt — Brand baru SELESAI
- Tambah: JagoSyariah, SeaBank, Blu, Maybank, BTN, Bukopin, Muamalat, BJB, BankJatim, BankNTT, Standard, HSBC, Citibank
- Fuzzy alias: "bank jago syariah", "bank central asia", "shopee pay", dll
- Longest-match algorithm (hindari "jago" match "jago syariah")

---

## Yang BELUM dikerjakan — tugas kamu di fase 5B

### 1. MiniGameScreen.kt — Redesign
**Yang harus berubah:**
- Numpad custom: layout 1–9, backspace, 0, ✓ (bukan keyboard sistem)
- Result screen: tampilkan 3 score dots (✓/✗ per soal), tombol "Main Lagi" + "Selesai"
- Trigger masuk MiniGame: 10x ketuk section "Tentang DompetKu" di ProfileScreen (bukan avatar)

### 2. ProfileScreen.kt — Redesign
**Yang harus berubah:**
- Avatar + edit pencil icon inline di foto profil
- Stats "1031 transaksi · 7 akun" tampil satu baris (bukan dua baris terpisah)
- Edit profil (nama, umur, pekerjaan, pendidikan) lewat BottomSheet terpisah (bukan inline)
- Age input: scroll wheel picker (WheelPicker style), bukan text field
- Trigger MiniGame: 10x ketuk section "Tentang DompetKu"

### 3. OnboardingScreen.kt — Swipe gesture fix
**Yang harus berubah:**
- Swipe kiri/kanan di slide berfungsi untuk navigasi antar slide
- Saat ini `detectHorizontalDragGestures` dipasang tapi tidak ada logic threshold/navigation
- Implementasi: track total drag distance, jika > 120px → next/prev

### 4. BudgetSheet (di HomeScreen.kt) — Upgrade
**Yang harus berubah:**
- Tambah slider "Target Tabungan" (0–70% dari pemasukan)
- Tambah 4 cards analisis: Ditabung / Bisa Dibelanjakan / Budget Final / Limit Harian
- Tambah checkbox "Pakai nominal kustom" (jika dicentang, muncul input manual seperti sekarang)

### 5. AccountFormSheet.kt — Color wheel picker (opsional jika ada waktu)
- Tambah opsi custom gradient via color wheel di bawah preset chips
- Jika terlalu kompleks, skip dulu ke fase 5E

---

## Known Issues / Aturan wajib

| File | Aturan |
|---|---|
| `AccountFormSheet.kt` | `Color(account.gradientStart.toInt())` — BUKAN `.value.toLong()` |
| `BrandInfo.gradientStart` | Bertipe `Color` langsung — tidak perlu `.toInt()` |
| `ProfileScreen.kt` | Toggle: `onToggle = { }` tanpa parameter `it` |
| `MainActivity.kt` | `enableEdgeToEdge()` sudah DIHAPUS — jangan tambahkan lagi |
| `AccountType` enum | Sudah ada: `bank, ewallet, cash, savings, investment, credit, other` |
| `Models.kt` | Jangan ubah field `Account` tanpa update Room migration |

---

## Struktur file yang relevan

```
ui/screen/
  home/HomeScreen.kt          ← BudgetSheet ada di sini (private composable)
  profile/ProfileScreen.kt    ← Redesign + MiniGame trigger
  onboarding/OnboardingScreen.kt ← Swipe fix
  MiniGameScreen.kt           ← Redesign numpad + result
  accounts/AccountFormSheet.kt ← Color wheel (opsional)
```

---

## Checklist sebelum ZIP

- [ ] Jangan copy file Gradle
- [ ] Path: `kotlin/` di ZIP → `java/` di project
- [ ] `RootViewModel.kt` ada di `ui/` bukan `ui/navigation/`
- [ ] Jalankan `./gradlew assembleDebug` setelah apply
