package com.dompetku.util

import androidx.compose.ui.graphics.Color
import com.dompetku.ui.theme.BrandGradients
import com.dompetku.ui.theme.GreenDark
import com.dompetku.ui.theme.GreenPrimary

data class BrandInfo(
    val key:           String,
    val displayName:   String,
    val gradientStart: Color,
    val gradientEnd:   Color,
    val deepLinkUri:   String  = "",
    val fallbackPkg:   String  = "",
)

object BrandDetector {

    private val BRANDS = listOf(
        // ── Bank umum ──────────────────────────────────────────────────────────
        BrandInfo("bca",           "BCA",              BrandGradients.BCA.first,         BrandGradients.BCA.second,         "mybca://",             "com.bca"),
        BrandInfo("bri",           "BRI",              BrandGradients.BRI.first,         BrandGradients.BRI.second,         "id.co.bri.brimo://",   "com.bri.brimo"),
        BrandInfo("bni",           "BNI",              BrandGradients.BNI.first,         BrandGradients.BNI.second,         "livin://",             "com.bni.mobilebanking"),
        BrandInfo("mandiri",       "Mandiri",          BrandGradients.Mandiri.first,     BrandGradients.Mandiri.second,     "livinbymandiri://",    "com.bankmandiri.livin"),
        BrandInfo("bsi",           "BSI",              BrandGradients.BSI.first,         BrandGradients.BSI.second,         "bsimobile://",         "id.bsi.mobile"),
        BrandInfo("cimb",          "CIMB Niaga",       BrandGradients.CIMB.first,        BrandGradients.CIMB.second,        "cimbniaga://",         "com.cimbniaga.mobile.production"),
        BrandInfo("permata",       "Permata",          BrandGradients.Permata.first,     BrandGradients.Permata.second,     "permatabank://",       "com.permatabank.mobile"),
        BrandInfo("danamon",       "Danamon",          BrandGradients.Danamon.first,     BrandGradients.Danamon.second,     "danamon://",           "id.co.danamon.mobile"),
        BrandInfo("panin",         "Panin",            BrandGradients.Panin.first,       BrandGradients.Panin.second,       "paninbank://",         "com.panin.mobilebanking"),
        BrandInfo("ocbc",          "OCBC",             BrandGradients.OCBC.first,        BrandGradients.OCBC.second,        "ocbc://",              "com.onada.obc"),
        BrandInfo("maybank",       "Maybank",          BrandGradients.Maybank.first,     BrandGradients.Maybank.second),
        BrandInfo("btn",           "BTN",              BrandGradients.BTN.first,         BrandGradients.BTN.second),
        BrandInfo("bukopin",       "Bukopin",          BrandGradients.Bukopin.first,     BrandGradients.Bukopin.second),
        BrandInfo("muamalat",      "Bank Muamalat",    BrandGradients.Muamalat.first,    BrandGradients.Muamalat.second),
        BrandInfo("bjb",           "BJB",              BrandGradients.BJB.first,         BrandGradients.BJB.second),
        BrandInfo("jatim",         "Bank Jatim",       BrandGradients.BankJatim.first,   BrandGradients.BankJatim.second),
        BrandInfo("ntt",           "Bank NTT",         BrandGradients.BankNTT.first,     BrandGradients.BankNTT.second),
        BrandInfo("hsbc",          "HSBC",             BrandGradients.HSBC.first,        BrandGradients.HSBC.second),
        BrandInfo("citibank",      "Citibank",         BrandGradients.Citibank.first,    BrandGradients.Citibank.second),
        BrandInfo("standard",      "Standard Chartered",BrandGradients.Standard.first,   BrandGradients.Standard.second),

        // ── Bank digital ───────────────────────────────────────────────────────
        BrandInfo("jenius",        "Jenius",           BrandGradients.Jenius.first,      BrandGradients.Jenius.second,      "jenius://",            "com.btpn.jenius"),
        BrandInfo("jago syariah",  "Jago Syariah",     BrandGradients.JagoSyariah.first, BrandGradients.JagoSyariah.second),
        BrandInfo("jago",          "Jago",             BrandGradients.Jago.first,        BrandGradients.Jago.second,        "jago://",              "id.jago.android"),
        BrandInfo("seabank",       "SeaBank",          BrandGradients.SeaBank.first,     BrandGradients.SeaBank.second),
        BrandInfo("blu",           "Blu by BCA",       BrandGradients.Blu.first,         BrandGradients.Blu.second),
        BrandInfo("flip",          "Flip",             BrandGradients.Flip.first,        BrandGradients.Flip.second,        "flip://",              "com.flip"),

        // ── E-Wallet ───────────────────────────────────────────────────────────
        BrandInfo("gopay",         "GoPay",            BrandGradients.GoPay.first,       BrandGradients.GoPay.second,       "gojek://gopay",        "com.gojek.app"),
        BrandInfo("ovo",           "OVO",              BrandGradients.OVO.first,         BrandGradients.OVO.second,         "ovo://",               "com.ipaymu.ovo"),
        BrandInfo("dana",          "DANA",             BrandGradients.DANA.first,        BrandGradients.DANA.second,        "dana://",              "id.dana"),
        BrandInfo("shopeepay",     "ShopeePay",        BrandGradients.ShopeePay.first,   BrandGradients.ShopeePay.second,   "shopeeid://",          "com.shopee.id"),
        BrandInfo("linkaja",       "LinkAja",          BrandGradients.LinkAja.first,     BrandGradients.LinkAja.second,     "linkaja://",           "com.telkom.mwallet"),
    )

    // Alias mapping untuk fuzzy match tambahan
    private val ALIASES = mapOf(
        "bank central asia"     to "bca",
        "bank rakyat"           to "bri",
        "bank negara"           to "bni",
        "bank mandiri"          to "mandiri",
        "bank syariah indonesia" to "bsi",
        "bank tabungan negara"  to "btn",
        "bank jago syariah"     to "jago syariah",
        "bank jago"             to "jago",
        "sea bank"              to "seabank",
        "blu bca"               to "blu",
        "shopee pay"            to "shopeepay",
        "link aja"              to "linkaja",
        "go pay"                to "gopay",
    )

    private val keyMap: Map<String, BrandInfo> = BRANDS.associateBy { it.key }

    /** Detect brand from account name — checks aliases first, then key/displayName */
    fun detect(accountName: String): BrandInfo? {
        if (accountName.isBlank()) return null
        val lower = accountName.lowercase().trim()

        // Check aliases first (more specific)
        for ((alias, key) in ALIASES) {
            if (lower.contains(alias)) return keyMap[key]
        }

        // Check by key or display name (longest match wins to avoid "jago" matching "jago syariah")
        return BRANDS
            .filter { lower.contains(it.key) || lower.contains(it.displayName.lowercase()) }
            .maxByOrNull { it.key.length }
    }

    /** Lookup by explicit key */
    fun byKey(key: String): BrandInfo? = keyMap[key.lowercase()]

    /** All known brands */
    fun allBrands(): List<BrandInfo> = BRANDS

    val defaultGradient = Pair(GreenPrimary, GreenDark)
}
