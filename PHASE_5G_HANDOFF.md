# DompetKu — Phase 5G Handoff
**Dibuat:** 2026-03-07  
**Dari:** Phase 5F → Phase 5G Bug Fix + Performance Optimization Sprint  
**Untuk:** Sesi berikutnya — lanjut sisa roadmap 5F

---

## 1. STATUS TERKINI

### ✅ Selesai di Phase 5G

| Batch | Bug | File | Status |
|---|---|---|---|
| Batch 1 | BUG-01 search field, BUG-05 date picker, BUG-06 UI reshuffle | `TransactionsViewModel.kt`, `TransactionsScreen.kt`, `CommonComponents.kt` | ✅ |
| Batch 2 | BUG-02 type detection, BUG-03 note pollution, BUG-04 transfer pair merge | `ExportImportManager.kt` | ✅ |
| Batch 3 | **Performance optimization app-wide** | lihat detail di bawah | ✅ |
| Batch 4 | BUG-07 carousel peek, BUG-09 FAB spring | `HomeScreen.kt`, `FanNav.kt` | ✅ |

### ❌ Sisa Roadmap 5F (belum dikerjakan — next session)
1. **Notifikasi reminder** — WorkManager sudah ada di deps, tinggal implementasi
2. **AccountFormSheet color wheel** — ganti color picker dengan wheel/circle picker
3. **Analytics lanjutan** — monthly trend chart 6/12 bulan, savings rate widget

---

## 2. DETAIL PERUBAHAN SESI INI (Performance + BUG-07/BUG-08)

### `AnalyticsViewModel.kt` — REFACTOR BESAR
- Seluruh komputasi berat (filtering, pie data, bar data, lifestyle, salary insight) dipindah ke dalam `combine` + `flowOn(Dispatchers.Default)` → tidak lagi jalan di main thread
- `filtered(state)` dihapus dari public API — sekarang semua sudah ada di `AnalyticsUiState`
- Tambah data classes: `PieDatum`, `BarDatum`, `LifestyleData`, `SalaryInsight` (replace `Triple` lama)
- `computeLifestyle()` dan `computeSalaryInsight()` dipindah jadi top-level private functions

### `AnalyticsScreen.kt`
- Bersih, tinggal consume `state.pieData`, `state.barData`, `state.lifestyle`, `state.salaryInsight` langsung
- Tidak ada lagi `remember(state) { heavy compute }` di UI thread

### `HomeViewModel.kt`
- Tambah `accountMap: Map<String, Account>` di `HomeUiState` — dihitung sekali di ViewModel saat data berubah

### `HomeScreen.kt`
- `Column + verticalScroll` → **`LazyColumn`** (off-screen tidak di-compose)
- Account lookup: `state.accounts.find { }` O(n) → `state.accountMap[id]` O(1)
- **BUG-07 fix:** `isDragging` state (`derivedStateOf { offsetPx.value != 0f }`) — peek cards hanya di-render saat user sedang drag, bukan selalu
- Carousel snap spring: `StiffnessMedium` → `StiffnessMediumLow` + `DampingRatioLowBouncy`

### `FanNav.kt`
- Pulse ring: `LinearEasing 2500ms` → `FastOutSlowInEasing 2200ms`, `initialValue` respects `open` state (stop buang frame saat fan terbuka)
- FAB icon spring: `DampingRatioMediumBouncy` → `0.65f`, `StiffnessMedium` → `StiffnessMediumLow`
- FanItem open spring: `stiffness 300f` → `380f`, damping `0.6f` → `0.58f` (iOS-like snap)
- FanItem close tween: `250ms` → `200ms`

---

## 3. CATATAN TEKNIS CARRY-OVER (PENTING)

- DB version = 2, `fallbackToDestructiveMigration()` aktif
- `gradientStart/End` disimpan sebagai `Long` — selalu `.toInt()` saat buat `Color()`
- `enableEdgeToEdge()` sudah **DIHAPUS** — jangan tambahkan lagi
- `Toggle` composable: `onToggle = { }` tanpa parameter
- `TxnUiState.allTxns` sudah **dihapus** — gunakan `grouped`, `totalCount`, `accountMap`
- Public `filtered(state)` sudah **dihapus** dari `TransactionsViewModel` — jangan di-restore
- Public `filtered(state)` sudah **dihapus** dari `AnalyticsViewModel` — sudah masuk ke state
- `AnalyticsUiState` sekarang punya: `filteredTxns`, `totalIncome`, `totalExpense`, `pieData`, `barData`, `lifestyle`, `salaryInsight`
- `HomeUiState` sekarang punya: `accountMap: Map<String, Account>`
- `flowOn(Dispatchers.Default)` ada di `HomeViewModel`, `TransactionsViewModel`, dan `AnalyticsViewModel` combine
- FileProvider authority: `com.dompetku.fileprovider`

---

## 4. FILE PATHS

```
C:\WORK\Projects\DompetKu\app\src\main\java\com\dompetku\
├── ui/
│   ├── MainScaffold.kt
│   ├── RootViewModel.kt
│   ├── navigation/
│   │   ├── FanNav.kt          ← diubah sesi ini
│   │   ├── NavGraph.kt
│   │   └── Screen.kt
│   ├── components/
│   │   ├── CommonComponents.kt ← diubah Batch 1
│   │   └── DompetKuLogo.kt
│   └── screen/
│       ├── home/
│       │   ├── HomeScreen.kt   ← diubah sesi ini
│       │   └── HomeViewModel.kt ← diubah sesi ini
│       ├── analytics/
│       │   ├── AnalyticsScreen.kt ← diubah sesi ini
│       │   └── AnalyticsViewModel.kt ← diubah sesi ini (BESAR)
│       ├── transactions/
│       │   ├── TransactionsScreen.kt ← diubah Batch 1
│       │   ├── TransactionsViewModel.kt ← diubah Batch 1
│       │   ├── TransactionFormSheet.kt
│       │   ├── TransactionDetailSheet.kt
│       │   └── TransferSheet.kt
│       ├── accounts/
│       │   ├── AccountsScreen.kt
│       │   ├── AccountsViewModel.kt
│       │   ├── AccountFormSheet.kt  ← next: color wheel
│       │   ├── AccountDetailScreen.kt
│       │   └── CurrencySheet.kt
│       ├── profile/
│       │   ├── ProfileScreen.kt
│       │   └── ProfileViewModel.kt
│       ├── pin/
│       │   ├── PinLockScreen.kt
│       │   └── PinViewModel.kt
│       └── onboarding/
│           ├── OnboardingScreen.kt
│           └── OnboardingViewModel.kt
└── util/
    └── ExportImportManager.kt ← diubah Batch 2
```

---

*Project path: `C:\WORK\Projects\DompetKu`*  
*Build target: `assembleDebug` — selalu verifikasi setelah perubahan*
