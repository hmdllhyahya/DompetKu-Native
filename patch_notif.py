import sys, os

path = r"C:\Users\Yahya H\Desktop\dompetku2\src\App.jsx"
if not os.path.exists(path):
    print("ERROR: File tidak ditemukan:", path)
    sys.exit(1)

d = open(path, encoding="utf-8").read()

# ── 1. Add import after last existing import line ────────────────────────────
old1 = "import * as XLSX from \"xlsx\";"
new1 = ("import * as XLSX from \"xlsx\";\n"
        "import { scheduleSmartNotifications, cancelSmartNotifications, refreshNotifications } from './notifications';")
assert old1 in d, "GAGAL: import XLSX tidak ditemukan"
d = d.replace(old1, new1, 1)
print("✓ Import notifikasi ditambahkan")

# ── 2. Add notifEnabled state after soundEnabled state ───────────────────────
old2 = "  const [soundEnabled, setSoundEnabled] = useState(()=>lsGet(\"dk_sound\",false));"
new2 = ("  const [soundEnabled, setSoundEnabled] = useState(()=>lsGet(\"dk_sound\",false));\n"
        "  const [notifEnabled, setNotifEnabled] = useState(()=>lsGet('dk_notif_enabled',false));")
assert old2 in d, "GAGAL: soundEnabled state tidak ditemukan"
d = d.replace(old2, new2, 1)
print("✓ State notifEnabled ditambahkan")

# ── 3. Add useEffects + toggleNotif — find a stable anchor in App() ──────────
old3 = ("  const [monthlyBudget,setMonthlyBudget]= useState(()=>lsGet(\"dk_budget\",10000000));")
new3 = ("  const [monthlyBudget,setMonthlyBudget]= useState(()=>lsGet(\"dk_budget\",10000000));\n"
        "\n"
        "  // ── Notifikasi harian ──\n"
        "  useEffect(()=>{\n"
        "    if(notifEnabled) scheduleSmartNotifications();\n"
        "    return ()=>{ if(notifEnabled) cancelSmartNotifications(); };\n"
        "  },[]);\n"
        "  useEffect(()=>{\n"
        "    if(!notifEnabled) return;\n"
        "    const timer=setTimeout(()=>refreshNotifications(),1500);\n"
        "    return ()=>clearTimeout(timer);\n"
        "  },[transactions,monthlyBudget,notifEnabled]);\n"
        "  const toggleNotif=async()=>{\n"
        "    const next=!notifEnabled;\n"
        "    setNotifEnabled(next);\n"
        "    lsSet('dk_notif_enabled',next);\n"
        "    if(next) await scheduleSmartNotifications();\n"
        "    else await cancelSmartNotifications();\n"
        "  };")
assert old3 in d, "GAGAL: monthlyBudget state tidak ditemukan"
d = d.replace(old3, new3, 1)
print("✓ useEffect + toggleNotif ditambahkan")

# ── 4. Add toggle UI in ProfileScreen — after Efek Suara row ────────────────
old4 = ("<SRow icon={soundEnabled?<Volume2 size={15}/>:<VolumeX size={15}/>} "
        "title=\"Efek Suara\" sub=\"Suara saat mencatat transaksi\" "
        "right={<Tog on={soundEnabled} onToggle={()=>setSoundEnabled(p=>!p)}/>}/>")
new4 = (old4 + "\n"
        "        <SRow icon={<Bell size={15}/>} title=\"Notifikasi Harian\" "
        "sub=\"Pengingat budget jam 6, 12, 13 & 16\" "
        "right={<Tog on={notifEnabled} onToggle={toggleNotif}/>}/>")
assert old4 in d, "GAGAL: SRow Efek Suara tidak ditemukan"
d = d.replace(old4, new4, 1)
print("✓ Toggle notifikasi ditambahkan di ProfileScreen")

# ── 5. Pass notifEnabled + toggleNotif to ProfileScreen ─────────────────────
old5 = ("soundEnabled={soundEnabled} setSoundEnabled={setSoundEnabled} "
        "setTransactions={setTransactions}")
new5 = ("soundEnabled={soundEnabled} setSoundEnabled={setSoundEnabled} "
        "notifEnabled={notifEnabled} toggleNotif={toggleNotif} "
        "setTransactions={setTransactions}")
assert old5 in d, "GAGAL: ProfileScreen props tidak ditemukan"
d = d.replace(old5, new5, 1)
print("✓ Props notifEnabled + toggleNotif diteruskan ke ProfileScreen")

# ── 6. Add notifEnabled + toggleNotif to ProfileScreen function signature ────
old6 = ("function ProfileScreen({ userName, setUserName, userAvatar, setUserAvatar, "
        "transactions, accounts, pinEnabled, pinHash, setPinEnabled, setPinHash, "
        "bioEnabled, setBioEnabled, soundEnabled, setSoundEnabled, setTransactions")
new6 = ("function ProfileScreen({ userName, setUserName, userAvatar, setUserAvatar, "
        "transactions, accounts, pinEnabled, pinHash, setPinEnabled, setPinHash, "
        "bioEnabled, setBioEnabled, soundEnabled, setSoundEnabled, "
        "notifEnabled, toggleNotif, setTransactions")
assert old6 in d, "GAGAL: ProfileScreen signature tidak ditemukan"
d = d.replace(old6, new6, 1)
print("✓ ProfileScreen signature diperbarui")

# ── Save ─────────────────────────────────────────────────────────────────────
open(path, "w", encoding="utf-8").write(d)
print(f"\n✅ Selesai! App.jsx berhasil dipatch. Lines: {d.count(chr(10))}")
