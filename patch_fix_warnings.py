import sys, os

path = r"C:\Users\Yahya H\Desktop\dompetku2\src\App.jsx"
d = open(path, encoding="utf-8").read()

# ── FIX 1: Duplicate style on Cell — merge into one ──────────────────────────
old = (
    '                <Cell key={i} fill={e.color} style={{outline:"none",cursor:"pointer"}}\n'
    '                  opacity={activeIdx==null||activeIdx===i?1:0.35}\n'
    '                  transform={activeIdx===i?"scale(1.06)":"scale(1)"}\n'
    '                  style={{outline:"none",cursor:"pointer",transition:"opacity .2s, transform .2s",transformOrigin:"center"}}\n'
    '                />'
)
new = (
    '                <Cell key={i} fill={e.color}\n'
    '                  opacity={activeIdx==null||activeIdx===i?1:0.35}\n'
    '                  style={{outline:"none",cursor:"pointer",transition:"opacity .2s, transform .2s",transformOrigin:"center"}}\n'
    '                />'
)
assert old in d, "GAGAL: Cell duplicate style tidak ditemukan"
d = d.replace(old, new, 1)
print("✓ FIX1: Duplicate style Cell diperbaiki")

# ── FIX 2: Duplicate key "Import Data" ───────────────────────────────────────
# Find and remove first occurrence of "Import Data": "Import Data",
first = d.find('"Import Data": "Import Data"')
second = d.find('"Import Data": "Import Data"', first + 1)
if second > 0:
    # Remove the second occurrence + surrounding comma/newline
    snippet = d[second-2:second+30]
    d = d[:second-2] + d[second+28:]
    print("✓ FIX2: Duplicate key 'Import Data' dihapus")
else:
    print("~ FIX2: Hanya ada satu 'Import Data', skip")

# ── FIX 3: Duplicate key "Analisis Gaya Hidup" ───────────────────────────────
first = d.find('"Analisis Gaya Hidup": "Lifestyle Analysis"')
second = d.find('"Analisis Gaya Hidup": "Lifestyle Analysis"', first + 1)
if second > 0:
    d = d[:second-2] + d[second+43:]
    print("✓ FIX3: Duplicate key 'Analisis Gaya Hidup' dihapus")
else:
    print("~ FIX3: Hanya ada satu 'Analisis Gaya Hidup', skip")

open(path, "w", encoding="utf-8").write(d)
print(f"\n✅ Selesai! Lines: {d.count(chr(10))}")
