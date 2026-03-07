# DompetKu — Phase 5F Handoff Report
**Dibuat:** 2026-03-07  
**Dari:** Phase 5F (Session selesai — Export/Import)  
**Untuk:** Phase 5F lanjutan / Phase 5G

---

## 1. YANG SUDAH DIKERJAKAN DI FASE INI

### ✅ Export / Import Data (XLSX)

#### File Baru
| File | Keterangan |
|---|---|
| `util/ExportImportManager.kt` | Pure logic export & import, injectable singleton |
| `res/xml/file_paths.xml` | FileProvider path config untuk share file |

#### File Dimodifikasi
| File | Perubahan |
|---|---|
| `AndroidManifest.xml` | Tambah `<provider>` FileProvider (`${applicationId}.fileprovider`) |
| `ui/screen/profile/ProfileViewModel.kt` | Inject `ExportImportManager`, tambah `EiEvent` sealed class, `triggerExport()`, `previewImport()`, `commitImport()` |
| `ui/screen/profile/ProfileScreen.kt` | Tambah 2 row baru di section DATA, import launcher, event collector, `ImportPreviewDialog` composable |

---

## 2. ARSITEKTUR EXPORT/IMPORT

### Flow Export
1. User tap **"Ekspor Data"** di ProfileScreen → `viewModel.triggerExport()`
2. ViewModel collect semua txns + akun dari repo → `ExportImportManager.exportXlsx()`
3. Manager buat XSSFWorkbook (2 sheet: Transaksi + Akun) → tulis ke `getExternalFilesDir/Documents/DompetKu/dompetku_export_YYYYMMDD.xlsx`
4. URI di-wrap via FileProvider → emit `EiEvent.ExportSuccess(uri)`
5. ProfileScreen terima event → launch `Intent.ACTION_SEND` (share sheet)

### Flow Import
1. User tap **"Impor Data"** → `importLauncher.launch(mimeType xlsx)`
2. User pilih file dari storage (SAF - no permission needed)
3. URI di-pass ke `viewModel.previewImport(uri)` → `ExportImportManager.importXlsx(uri)`
4. Manager parse file → return `ImportResult(transactions, accounts, errors)`
5. Emit `EiEvent.ImportPreviewReady` → UI tampilkan `ImportPreviewDialog`
6. User pilih **Gabungkan** (merge, skip duplicate ID) atau **Ganti Semua** (delete all → insert)
7. Emit `EiEvent.ImportCommitted` → Snackbar "Import selesai: X transaksi, Y akun"

### Format XLSX
**Sheet "Transaksi":** `id, type, amount, adminFee, category, note, date, time, accountId, fromId, toId`  
**Sheet "Akun":** `id, type, name, balance, last4, brandKey, gradientStart, gradientEnd, sortOrder`

Header-based column mapping (robust terhadap pergeseran kolom).

---

## 3. CATATAN TEKNIS

- `ExportImportManager` inject `@ApplicationContext` — aman untuk singleton
- FileProvider authority: `com.dompetku.fileprovider`
- File path di `file_paths.xml`: `external-files-path` + fallback `files-path`
- Import pakai `ActivityResultContracts.GetContent` (SAF) — tidak butuh storage permission
- `commitImport(replace=false)`: insert satu per satu, error per row di-skip (tidak atomic rollback)
- `commitImport(replace=true)`: `deleteAll()` dulu → insert semua (akun dulu, baru transaksi)
- Snackbar host ada di dalam `Box` wrapper di ProfileScreen, anchored `BottomCenter` + `padding(bottom=90dp)`

---

## 4. ✅ BUILD ERROR — SUDAH DIPATCH

### Error (sudah resolved)
```
e: ProfileScreen.kt:280:54 Unresolved reference 'Import'.
```

### Root Cause
`PhosphorIcons.Regular.Import` dan `PhosphorIcons.Regular.Export` **tidak ada** di library `com.adamglin:phosphor-icon:1.0.0`. Phosphor tidak punya icon dengan nama harfiah tersebut.

### Fix yang sudah diterapkan
Di `ProfileScreen.kt` (sudah diubah langsung ke file):

```kotlin
// SEBELUM (error):
icon = PhosphorIcons.Regular.Export   // baris Ekspor Data
icon = PhosphorIcons.Regular.Import   // baris Impor Data

// SESUDAH (sudah diapply):
icon = PhosphorIcons.Regular.ArrowSquareOut   // Ekspor Data
icon = PhosphorIcons.Regular.ArrowSquareIn    // Impor Data
```

### Catatan tambahan (masih berlaku)
- **`EiEvent` qualified name** di ProfileScreen (`com.dompetku.ui.screen.profile.EiEvent.XYZ`) — sudah pakai fully qualified name di `when` block. Bisa disederhanakan dengan tambah import di bagian atas file.
- **Apache POI memory** — `XSSFWorkbook` boros RAM untuk file besar. Untuk dataset ribuan transaksi masih oke. Kalau ada OOM, pertimbangkan `SXSSFWorkbook` untuk write.

---

## 5. SISA ROADMAP 5F

| Item | Status |
|---|---|
| Export/Import CSV+Excel | ✅ Done (XLSX) |
| Smart Import universal | ✅ Done (lihat section 5A) |
| Notifikasi reminder | ❌ Belum — WorkManager sudah di deps |
| AccountFormSheet color wheel | ❌ Belum |
| Performance polish (skeleton, haptic, pull-to-refresh) | ❌ Belum |
| Analytics lanjutan (monthly trend 6/12 bulan, savings rate) | ❌ Belum |

---

## 5A. ✅ SMART IMPORT — SUDAH DIIMPLEMENTASIKAN (2026-03-07)

**Files yang diubah:** `ExportImportManager.kt`, `ProfileViewModel.kt`, `ProfileScreen.kt`

### Masalah yang Diselesaikan
Import sebelumnya hanya bisa baca file format persis DompetKu. File dari Money Manager, bank, app keuangan lain → gagal total karena nama sheet/kolom beda.

### Arsitektur Baru

**`ExportImportManager.kt` — additions:**
- Data classes: `SmartImportResult`, `SmartTransaction`, `DetectedAccount`, `AccountResolution` (sealed: UseExisting, CreateNew, Skip)
- `fun smartImportXlsx(uri, existingAccounts): SmartImportResult` — scan semua sheet, auto-detect header row (scan up to row 5)
- `SmartImportEngine` internal object:
  - `detectColumns(headerRow)` — heuristic scoring 9 role (DATE/TIME/AMOUNT/DEBIT/CREDIT/CATEGORY/NOTE/ACCOUNT/TYPE). Bahasa bebas (ID+EN)
  - `parseRow()` — 3 mode: debit+credit terpisah, single signed amount, single unsigned + type column
  - `parseDate()` — support ISO, YYYYMMDD, DD/MM/YYYY, MM/DD/YYYY, YYYY/MM/DD, "7 Mar 2025"
  - `mapCategory()` — 100+ keyword → 12 kategori DompetKu
  - `matchAccount()` — Jaccard similarity + subset boost, threshold 0.25
  - `Row.str()` handle date-formatted cells via `DateUtil.isCellDateFormatted()`
- **Nominal negatif** → otomatis `expense` + flag `wasSignFlipped`
- **Kolom tidak dikenal** → append ke `note` sebagai `[NamaKolom: nilai]`

**`ProfileViewModel.kt` — additions:**
- `ProfileUiState` tambah `accounts: List<Account>` (untuk dialog)
- `EiEvent.SmartImportPreviewReady(result)` ditambah ke sealed class
- `fun smartPreviewImport(uri)` — load existing accounts → call engine → emit event
- `fun commitSmartImport(result, accountResolutions, replace)` — buat akun baru (type=other, gradient abu-abu, sortOrder=999) lalu insert transactions

**`ProfileScreen.kt` — changes:**
- `importLauncher` → call `smartPreviewImport()` (bukan `previewImport()`)
- State baru: `smartImportPreview: SmartImportResult?`
- `SmartImportDialog` (multi-step):
  - **Step 0** (jika ada akun confidence < 70%): per-akun card dengan opsi Pakai suggestion / Pilih akun lain (dropdown) / Buat akun baru / Lewati
  - **Step 1** ringkasan: stats chips, banner auto-sign + extra fields + warnings, category mapping collapsible, Gabungkan / Ganti Semua
- Helper composables: `AccountResolutionCard`, `SmartStatChip`, `SmartInfoBanner`

### Threshold
- Score ≥ 0.70 → auto-resolve (skip Step 0)
- Score 0.25–0.70 → tampil di Step 0 dengan suggestion
- Score < 0.25 → tampil di Step 0 tanpa suggestion

---

## 6. CATATAN CARRY-OVER DARI FASE SEBELUMNYA

- Database version = 2, `fallbackToDestructiveMigration()` aktif
- `gradientStart/End` disimpan sebagai `Long` — selalu `.toInt()` saat buat `Color()`
- `enableEdgeToEdge()` sudah DIHAPUS — jangan tambahkan lagi
- `Toggle` composable: `onToggle = { }` tanpa parameter
- `SmartTransaction` — tidak perlu unused import `SmartTransaction` di ViewModel, bisa dihapus jika Kotlin complain

---

*Project path: `C:\WORK\Projects\DompetKu`*
