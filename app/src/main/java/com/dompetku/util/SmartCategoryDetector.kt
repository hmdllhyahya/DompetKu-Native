package com.dompetku.util

/**
 * Smart Category Detector — category names EXACTLY match CATS_EXPENSE / CATS_INCOME
 * so auto-detected category persists correctly when transaction is saved.
 */
object SmartCategoryDetector {

    data class DetectionResult(
        val category:   String,
        val confidence: Float,
        val isEasterEgg: Boolean = false,
        val easterEggLabel: String = "",
    )

    private val RULES: List<Pair<String, List<String>>> = listOf(

        "Makan & Minum" to listOf(
            "makan", "minum", "restoran", "restaurant", "warung", "warteg", "cafe", "kafe",
            "coffee", "kopi", "tea", "teh", "boba", "bubble tea", "jus", "juice", "smoothie",
            "pizza", "burger", "sushi", "ramen", "mie", "nasi", "bakso", "soto", "gado",
            "pempek", "rendang", "ayam", "ikan", "seafood", "steak", "pasta", "salad",
            "sandwich", "hotdog", "kebab", "dimsum", "kwetiau", "laksa", "martabak",
            "gorengan", "bakmi", "noodle", "rice", "food", "lunch", "dinner",
            "breakfast", "sarapan", "makan siang", "makan malam", "snack", "cemilan",
            "jajan", "kue", "roti", "bread", "cake", "donat", "donut", "eskrim", "ice cream",
            "gelato", "starbucks", "kfc", "mcdonalds", "mcd", "a&w", "subway", "wendy",
            "pizzahut", "domino", "hokben", "yoshinoya", "pepper lunch", "solaria",
            "gofood", "grabfood", "shopeefood", "eatery", "kantin", "ngopi", "minuman", "makanan",
        ),

        "Belanja Harian" to listOf(
            "belanja", "shopping", "beli", "purchase", "toko", "shop", "mall", "plaza",
            "supermarket", "indomaret", "alfamart", "alfamidi", "lawson", "circle k",
            "hypermart", "carrefour", "lottemart", "giant", "hero", "ranch market",
            "farmer", "pasar", "market", "grocery", "groceries", "minimarket",
            "fashion", "baju", "sepatu", "tas", "celana", "kaos", "jaket", "dress",
            "gaun", "parfum", "kosmetik", "skincare", "makeup", "aksesoris", "jam tangan",
            "kacamata", "dompet", "wallet", "sandal", "boots", "sneakers",
        ),

        "Belanja Online" to listOf(
            "tokopedia", "shopee", "lazada", "bukalapak", "blibli", "jd.id",
            "tiktokshop", "tiktok shop", "zalora", "berrybenka", "sociolla",
            "orami", "mamaway", "mothercare", "cod", "cash on delivery",
            "ongkir", "shipping", "pengiriman", "jne", "jnt", "sicepat", "anteraja",
            "ninja", "pos indonesia", "gosend", "lalamove", "amazon", "online shop",
        ),

        "Transportasi" to listOf(
            "transjakarta", "tj", "busway", "mrt", "lrt", "krl", "commuter", "kereta",
            "train", "bus", "angkot", "angkutan", "taksi", "taxi", "ojek", "gojek",
            "grab", "goride", "grabride", "maxim", "indriver", "becak", "bajaj",
            "bentor", "damri", "biskota", "bis", "travel", "shuttle", "tol", "parkir",
            "parking", "toll", "bbm", "bensin", "solar", "pertamina", "shell", "vivo",
            "spbu", "bahan bakar", "fuel", "isi bensin", "servis motor", "servis mobil",
            "bengkel", "tambal ban", "cuci mobil", "cuci motor", "driver", "ride",
            "perjalanan", "ongkos", "tiket", "tiket kereta", "tiket pesawat",
            // pesawat & maskapai
            "pesawat", "flight", "boarding", "bandara", "airport", "terminal",
            "garuda", "lion air", "lionair", "batik air", "batikair", "citilink",
            "airasia", "sriwijaya", "wings air", "nam air", "pelita", "trigana",
            "maskapai", "airline", "check-in", "check in", "bagasi", "luggage",
        ),

        "Hiburan" to listOf(
            "hiburan", "entertainment", "film", "movie", "bioskop", "cinema", "cgv",
            "xxi", "cinepolis", "21", "imax", "4dx", "nonton", "konser", "concert",
            "event", "festival", "pertunjukan", "show", "karaoke", "game", "gaming",
            "playstation", "xbox", "nintendo", "steam", "mobile legend", "pubg",
            "freefire", "ff", "valorant", "main game", "voucher game", "top up", "topup",
            "recharge", "liburan", "wisata", "tour", "hotel", "penginapan", "resort",
            "airbnb", "traveloka", "tiket.com", "agoda", "booking",
        ),

        "Tagihan" to listOf(
            "listrik", "electricity", "pln", "air", "water", "pdam", "gas", "pgn",
            "internet", "wifi", "indihome", "biznet", "firstmedia", "myrepublic",
            "telkom", "speedy", "fiber", "pulsa", "paket data", "data plan",
            "telkomsel", "xl", "indosat", "tri", "smartfren", "by.u",
            "iuran", "tagihan", "bill", "cicilan", "kredit", "kpr", "bayar tagihan",
            "payment", "premium", "subscription", "langganan", "netflix", "spotify",
            "youtube premium", "disney", "apple", "google play", "itunes",
        ),

        "Kesehatan" to listOf(
            "kesehatan", "health", "dokter", "doctor", "rumah sakit", "rs", "hospital",
            "klinik", "clinic", "puskesmas", "apotek", "apotik", "pharmacy", "obat",
            "medicine", "vitamin", "suplemen", "supplement", "cek kesehatan",
            "medical check", "laboratorium", "lab", "rontgen", "usg", "bpjs",
            "asuransi", "insurance", "konsultasi", "periksa", "opname", "rawat",
            "gigi", "dental", "mata", "optik", "fisioterapi",
            "gym", "fitness", "olahraga", "sport", "futsal", "badminton", "basket",
            "renang", "swimming", "lari", "running", "yoga", "pilates", "zumba",
            "member gym", "membership gym",
        ),

        "Pendidikan" to listOf(
            "pendidikan", "education", "sekolah", "school", "kuliah", "college",
            "universitas", "university", "kursus", "course", "les", "bimbel",
            "bimbingan belajar", "seminar", "workshop", "training", "pelatihan",
            "sertifikasi", "certification", "buku", "book", "materi", "modul",
            "spp", "ukt", "biaya kuliah", "tuition", "beasiswa",
            "ruangguru", "zenius", "quipper", "coursera", "udemy",
        ),

        "Tempat Tinggal" to listOf(
            "rumah", "kost", "kontrakan", "sewa", "rent", "indekos", "apartemen",
            "apartment", "perabot", "furniture", "ikea", "dekorasi", "decoration",
            "cat", "renovasi", "renovation", "bangunan", "material", "tukang",
            "servis ac", "servis kulkas", "servis mesin cuci", "elektronik",
            "peralatan rumah", "cleaning", "kebersihan", "laundry", "cuci baju",
        ),

        "Perawatan" to listOf(
            "salon", "barbershop", "barber", "cukur", "pangkas", "potong rambut",
            "haircut", "hair", "rambut", "spa", "pijat", "massage", "manicure",
            "pedicure", "facial", "kecantikan", "beauty", "perawatan diri",
            "serum", "moisturizer", "sunscreen", "lipstik", "lipstick", "skincare premium",
        ),

        "Gaji" to listOf(
            "gaji", "salary", "upah", "wage", "honor", "honorarium", "bonus",
            "thr", "insentif", "incentive", "komisi", "commission",
            "pembayaran diterima", "payment received", "income", "pemasukan",
            "penghasilan", "earnings", "cashback", "refund", "kembalian uang",
        ),

        "Freelance" to listOf(
            "freelance", "proyek", "project", "invoice", "jasa", "fee proyek",
            "bayaran proyek", "honorarium jasa", "konsultan", "consultant",
            "desainer", "programmer", "content creator", "copywriter",
        ),

        "Hadiah" to listOf(
            "hadiah", "gift", "kado", "present", "sumbangan", "donation", "donasi",
            "amal", "charity", "sedekah", "zakat", "infak", "wakaf",
            "pesta", "party", "pernikahan", "wedding", "ultah", "birthday",
            "anniversary", "kondangan", "angpau", "reward", "bonus hadiah",
        ),

        "Investasi" to listOf(
            "investasi", "investment", "saham", "stock", "reksa dana", "reksadana",
            "mutual fund", "obligasi", "bond", "deposito", "deposit", "tabungan berjangka",
            "crypto", "bitcoin", "ethereum", "aset", "asset",
            "bibit", "bareksa", "ipot", "ajaib", "pluang", "stockbit",
        ),

        "Penyesuaian Saldo" to listOf(
            "penyesuaian", "adjustment", "koreksi", "correction", "saldo awal",
            "opening balance", "initial balance",
        ),
    )

    // Easter egg keywords → Kereta Cepat Indonesia-China
    private val EASTER_EGG_WHOOSH = listOf("kcij", "whoosh", "kereta cepat", "halim", "padalarang", "tegalluar", "karawang kcij")

    fun detect(note: String): DetectionResult? {
        if (note.isBlank()) return null
        val lower = note.lowercase().trim()

        // Easter egg: KCIJ / Whoosh
        if (EASTER_EGG_WHOOSH.any { lower.contains(it) }) {
            return DetectionResult(
                category     = "Transportasi",
                confidence   = 1f,
                isEasterEgg  = true,
                easterEggLabel = "Wuuush! 🚄 Kereta Cepat terdeteksi!",
            )
        }

        var bestCategory: String? = null
        var bestScore = 0
        for ((category, keywords) in RULES) {
            var score = 0
            for (kw in keywords) { if (lower.contains(kw)) score++ }
            if (score > bestScore) { bestScore = score; bestCategory = category }
        }
        if (bestCategory == null || bestScore == 0) return null
        return DetectionResult(category = bestCategory, confidence = (bestScore / 3f).coerceAtMost(1f))
    }

    fun allCategories(): List<String> = RULES.map { it.first }

    // ── Contextual field definitions per category ────────────────────────────
    // Each entry: fieldKey -> hint label shown to user
    data class ContextField(val key: String, val label: String, val hint: String)

    val CONTEXTUAL_FIELDS: Map<String, List<ContextField>> = mapOf(
        "Transportasi" to listOf(
            ContextField("dari",     "Dari",         "cth: Blok M, Halim..."),
            ContextField("ke",       "Ke",           "cth: Monas, Bandung..."),
        ),
        "Hiburan" to listOf(
            ContextField("judul",    "Judul / Event", "cth: Avengers, Coldplay..."),
        ),
        "Tagihan" to listOf(
            ContextField("provider", "Provider",      "cth: Telkomsel, PLN..."),
        ),
        "Kesehatan" to listOf(
            ContextField("dokter",   "Dokter / Klinik", "cth: dr. Budi, RS Medistra..."),
        ),
        "Pendidikan" to listOf(
            ContextField("institusi", "Institusi / Kursus", "cth: Udemy, Binus..."),
        ),
        "Belanja Online" to listOf(
            ContextField("platform", "Platform", "cth: Shopee, Tokopedia..."),
        ),
    )

    fun contextFieldsFor(category: String): List<ContextField> =
        CONTEXTUAL_FIELDS[category] ?: emptyList()
}
