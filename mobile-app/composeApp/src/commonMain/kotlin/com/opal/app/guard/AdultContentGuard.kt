package com.opal.app.guard

/**
 * Nomaqbul (kattalar uchun) kontent detektori.
 *
 * Aniqlik tamoyili — "bitta so'z = blok" EMAS:
 *  - **Kuchli** atama (porn, hentai, aniq domén) — yolg'iz o'zi bloklaydi.
 *  - **O'rtacha** atamalar guruhlarga bo'lingan (nudity, erotic, fetish...).
 *    Kamida 2 xil guruh yoki 1 guruh + 2 zaif guruh kerak.
 *  - **Zaif** atamalar (sex, adult, intim) — faqat 3 xil guruh birgalikda.
 *  - Ta'lim/tibbiyot/yangilik/musiqa konteksti bo'lsa — faqat kuchli atama bloklaydi.
 *  - URL faqat **manzil satridan** tekshiriladi (sahifa matnidagi havola emas).
 *
 * Shu tufayli "Sex" nomli qo'shiq, "sex education" maqolasi yoki "18+" belgisi
 * bloklanmaydi; haqiqiy nomaqbul sahifa esa bloklanadi.
 */
enum class GuardSensitivity(val code: String, val label: String) {
    /** Faqat aniq signal. */
    LOW("low", "Past"),

    /** Standart: 2 xil o'rtacha guruh yoki strong atama. */
    MEDIUM("medium", "O'rta"),

    /** Ko'proq signal bilan ishlaydi, lekin bitta noaniq so'z bilan emas. */
    HIGH("high", "Yuqori");

    companion object {
        fun from(code: String?): GuardSensitivity =
            entries.firstOrNull { it.code == code } ?: MEDIUM
    }
}

enum class GuardKind { NONE, TEXT, SITE, APP, VISUAL }

data class GuardInput(
    val screenText: String = "",
    val addressBarText: String = "",
    val appLabel: String = "",
    val windowTitle: String = ""
)

data class GuardVerdict(
    val kind: GuardKind = GuardKind.NONE,
    val signals: List<String> = emptyList(),
    val host: String? = null
) {
    val isBlocking: Boolean get() = kind != GuardKind.NONE

    /** Foydalanuvchiga ko'rsatiladigan qisqa sabab. */
    fun reasonLabel(): String = when (kind) {
        GuardKind.SITE -> "Nomaqbul sayt aniqlandi"
        GuardKind.APP -> "Nomaqbul ilova aniqlandi"
        GuardKind.VISUAL -> "Ekranda nomaqbul kontent"
        GuardKind.TEXT -> "Nomaqbul kontent aniqlandi"
        GuardKind.NONE -> ""
    }
}

object AdultContentGuard {

    /* ==================== kuchli atamalar ==================== */

    /** Aniq nomaqbul atamalar — yolg'iz o'zi bloklaydi. */
    private val STRONG: Set<String> = setOf(
        // ingliz
        "porn", "porno", "pornography", "pornographic", "pornstar",
        "xxx", "xrated", "hentai",
        "gangbang", "creampie", "cumshot", "blowjob", "handjob", "deepthroat",
        "orgy", "incest", "bestiality", "necrophilia", "pedophilia",
        // rus
        "порно", "порнография", "порнуха", "порносайт", "хентай", "инцест",
        "минет", "куннилингус", "мастурбация", "онанизм", "сперма",
        "трахать", "трахаться", "выебать", "дрочка",
        "групповуха", "зоофилия", "педофилия",
        // o'zbek (lotin)
        "pornografiya", "pornografik"
    )

    /**
     * Platforma/brand nomlari. Bular sahifa MATNIDA yolg'iz uchrasa bloklanmaydi
     * (izohdagi havola, yangilik maqolasi), lekin manzil satri yoki ilova nomida
     * bo'lsa — blok. Matnda ular "brand" guruhi sifatida hisoblanadi.
     */
    private val BRANDS: Set<String> = setOf(
        "pornhub", "rule34", "e621", "onlyfans", "fansly", "chaturbate",
        "stripchat", "cam4", "bongacams", "livejasmin", "spankbang",
        "xvideos", "xnxx", "xhamster", "redtube", "youporn", "brazzers",
        "nhentai", "hanime", "missav", "javbus", "javhd", "supjav",
        "porntrex", "tnaflix", "beeg", "txxx", "motherless",
        "adultfriendfinder", "ashleymadison",
        "сексшоп", "секс-шоп", "seks-shop", "seksshop"
    )

    /** O'rtacha atamalar — guruhlar bo'yicha (sinonimlar bitta guruh). */
    private val MEDIUM_GROUPS: Map<String, Set<String>> = mapOf(
        "nudity" to setOf(
            "nude", "nudes", "nudity", "naked", "nakedly",
            "нагота", "голая", "голые", "обнажённая", "обнаженная",
            "обнажённые", "обнаженные", "yalang'och", "yalangoch"
        ),
        "erotic" to setOf(
            "erotic", "erotica", "eroticism", "эротика", "эротический",
            "shahvoniy", "shahvat", "eros"
        ),
        "nsfw" to setOf("nsfw", "sexcam", "webcamsex", "camgirl", "camsex"),
        "fetish" to setOf("fetish", "фетиш"),
        "bdsm" to setOf("bdsm", "бдсм"),
        "sexact" to setOf(
            "orgasm", "masturbation", "vagina", "penis", "clitoris", "erection",
            "вагина", "пенис", "клитор", "эрекция", "оргазм", "разврат", "блуд"
        ),
        "exploitation" to setOf(
            "escort", "prostitute", "prostitution", "brothel", "lapdance",
            "striptease", "stripper", "проститутка", "шлюха", "fohisha", "sutenyor"
        )
    )

    /** Zaif atamalar — guruhlar bo'yicha. */
    private val WEAK_GROUPS: Map<String, Set<String>> = mapOf(
        "sex" to setOf(
            "sex", "seks", "seksual", "sexual", "sexuality", "sexting",
            "секс", "сексуальный", "jinsiy", "jinsiyaloqa"
        ),
        "adult" to setOf("adult", "adults", "adultcontent", "adultvideo"),
        "intimate" to setOf(
            "intim", "интим", "эротичный", "kattalar", "voyaga", "erotik"
        )
    )

    /** Ta'lim/tibbiyot/yangilik/media konteksti — faqat kuchli atama bloklaydi. */
    private val SAFE_CONTEXT: Set<String> = setOf(
        "education", "educational", "school", "lesson", "course", "science",
        "research", "study", "article", "wikipedia", "encyclopedia", "news",
        "report", "statistics", "medical", "medicine", "health", "clinic",
        "doctor", "gynecology", "gynecologist", "urology", "urologist",
        "sexology", "sexologist", "therapy", "reproductive", "pregnancy",
        "anatomy", "biology", "psychology", "relationship", "harassment",
        "music", "official", "lyrics", "video", "trailer", "movie", "series",
        "documentary", "podcast", "interview", "shorts", "subscribe",
        "ta'lim", "talim", "tarbiya", "tarbiyaviy", "dars", "darslik", "ilm",
        "ilmiy", "tadqiqot", "maqola", "yangilik", "hisobot", "statistika",
        "tibbiyot", "tibbiy", "shifokor", "klinika", "ginekolog", "urolog",
        "salomatlik", "qonun", "huquq", "jinoyat", "profilaktika",
        "reproduktiv", "musiqa", "rasmiy", "treyler", "film", "serial",
        "safecontext",
        "образование", "учебник", "урок", "наука", "исследование", "статья",
        "новости", "медицина", "медицинский", "здоровье", "врач",
        "гинеколог", "уролог", "сексология", "терапия", "закон", "право",
        "музыка", "официальный", "трейлер", "фильм", "сериал"
    )

    /** Xavfsiz iboralar — tahlildan oldin neytrallanadi. */
    private val SAFE_PHRASES = listOf(
        Regex("(?i)same[\\s-]*sex"),
        Regex("(?i)sex\\s*education"),
        Regex("(?i)sexual\\s*education"),
        Regex("(?i)sexual\\s*health"),
        Regex("(?i)sexual\\s*harassment"),
        Regex("(?i)sex\\s*ism"),
        Regex("(?i)sex\\s*ology"),
        Regex("(?i)jinsiy\\s*tarbiya"),
        Regex("(?i)jinsiy\\s*salomatlik"),
        Regex("(?i)jinsiy\\s*zo'?ravonlik"),
        Regex("(?i)reproductive\\s*health"),
        Regex("(?i)сексуальное\\s*образование"),
        Regex("(?i)сексуальное\\s*здоровье"),
        Regex("(?i)сексуальное\\s*домогательство")
    )

    /** Mashhur nomaqbul doménlar (faqat manzil satri uchun). */
    private val ADULT_DOMAINS: List<String> = listOf(
        "pornhub.com", "pornhub.org", "xvideos.com", "xnxx.com", "xhamster.com",
        "xhamster.desi", "xhamster.xxx", "redtube.com", "youporn.com",
        "brazzers.com", "spankbang.com", "beeg.com", "txxx.com", "hclips.com",
        "tube8.com", "porn.com", "pornhd.com", "porn300.com", "porntrex.com",
        "tnaflix.com", "motherless.com", "e621.net", "rule34.xxx",
        "rule34.paheal.net", "nhentai.net", "hentaihaven.xxx", "hanime.tv",
        "javhd.com", "javbus.com", "missav.com", "supjav.com", "91porn.com",
        "onlyfans.com", "fansly.com", "chaturbate.com", "stripchat.com",
        "cam4.com", "camsoda.com", "bongacams.com", "livejasmin.com",
        "xlovecam.com", "myfreecams.com", "adultfriendfinder.com",
        "ashleymadison.com", "seeking.com", "spicy.com", "eporner.com",
        "pornhat.com", "pornone.com", "sexvid.xxx", "hqporner.com",
        "letmejerk.com", "jav.guru", "javmost.com", "njav.tv", "sextb.net"
    )

    private val ADULT_TLDS: List<String> = listOf(
        ".xxx", ".porn", ".sex", ".adult", ".sexy"
    )

    private val DOMAIN_RE =
        Regex("""(?i)\b((?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\.)+(?:[a-z]{2,24}))\b""")

    private val NON_ALNUM_RE = Regex("""[^\p{L}\p{N}]+""")

    private val LEET = mapOf(
        '0' to 'o', '1' to 'i', '3' to 'e', '4' to 'a', '5' to 's',
        '7' to 't', '8' to 'b', '9' to 'g', '@' to 'a', '$' to 's',
        '€' to 'e', '£' to 'l', '!' to 'i', '|' to 'l'
    )

    /** Kirill ↔ lotin o'xshash harflar (obfuskatsiya: "pоrn" — kirill "о"). */
    private val HOMOGLYPHS: Map<Char, String> = mapOf(
        'а' to "a", 'в' to "b", 'е' to "e", 'ё' to "e", 'к' to "k", 'м' to "m",
        'н' to "h", 'о' to "o", 'р' to "p", 'с' to "c", 'т' to "t", 'у' to "y",
        'х' to "x", 'і' to "i", 'ј' to "j", 'ѕ' to "s", 'ԁ' to "d", 'ӏ' to "l",
        'п' to "n", 'г' to "r", 'и' to "u", 'л' to "n", 'д' to "d",
        'ч' to "ch", 'ш' to "sh", 'щ' to "sch", 'ж' to "zh", 'з' to "z",
        'ф' to "f", 'ц' to "c", 'ы' to "y", 'ь' to "", 'ъ' to "", 'э' to "e",
        'ю' to "yu", 'я' to "ya", 'й' to "y", 'б' to "b",
        'А' to "A", 'В' to "B", 'Е' to "E", 'К' to "K", 'М' to "M", 'Н' to "H",
        'О' to "O", 'Р' to "P", 'С' to "C", 'Т' to "T", 'У' to "Y", 'Х' to "X"
    )

    /* ==================== asosiy API ==================== */

    fun evaluate(input: GuardInput, sensitivity: GuardSensitivity = GuardSensitivity.MEDIUM): GuardVerdict {
        // 1) Manzil satridagi 18+ domén — eng ishonchli signal.
        val host = findAdultHost(input.addressBarText)
        if (host != null) {
            return GuardVerdict(GuardKind.SITE, listOf(host), host)
        }

        // 2) Ilova nomi — kuchli atama yoki brand.
        val labelTokens = tokenize(input.appLabel)
        val labelStrong = strongHits(labelTokens)
        val labelBrand = brandHits(labelTokens)
        if (labelStrong.isNotEmpty() || labelBrand.isNotEmpty()) {
            return GuardVerdict(GuardKind.APP, (labelStrong + labelBrand).take(4))
        }

        // 3) Ekrandagi ko'rinadigan matn.
        val tokens = tokenize(input.screenText + "\n" + input.windowTitle)
        val strong = strongHits(tokens)
        if (strong.isNotEmpty()) {
            return GuardVerdict(GuardKind.TEXT, strong)
        }

        val medium = mediumGroups(tokens)
        val weak = weakGroups(tokens)
        val safe = hasSafeContext(tokens)

        val block = when (sensitivity) {
            GuardSensitivity.LOW -> medium.size >= 4
            GuardSensitivity.MEDIUM ->
                (!safe && medium.size >= 2) || (!safe && medium.size >= 1 && weak.size >= 2)
            GuardSensitivity.HIGH ->
                (!safe && medium.size >= 1 && weak.size >= 1) ||
                    medium.size >= 2 ||
                    weak.size >= 3
        }
        if (!block) return GuardVerdict()

        return GuardVerdict(
            kind = GuardKind.TEXT,
            signals = (medium.keys + weak.keys).take(6)
        )
    }

    /** Ilova nomi bo'yicha (Watchdog zaxira dvigateli uchun). */
    fun evaluateLabel(appLabel: String): GuardVerdict {
        if (appLabel.isBlank()) return GuardVerdict()
        val tokens = tokenize(appLabel)
        val strong = strongHits(tokens)
        val brands = brandHits(tokens)
        if (strong.isNotEmpty() || brands.isNotEmpty()) {
            return GuardVerdict(GuardKind.APP, (strong + brands).take(4))
        }
        val medium = mediumGroups(tokens)
        if (medium.size >= 2) return GuardVerdict(GuardKind.APP, medium.keys.toList())
        return GuardVerdict()
    }

    /** Manzil satridagi nomaqbul domén. */
    fun findAdultHost(text: String): String? {
        if (text.isBlank()) return null
        for (match in DOMAIN_RE.findAll(text.take(8_000))) {
            val host = match.groupValues[1].lowercase().trimEnd('.')
            val bare = host.removePrefix("www.").removePrefix("m.")
            if (isAdultDomain(bare)) return bare
        }
        return null
    }

    /** Vizual model uchun pol (qanchalik yuqori bo'lsa, shunchalik qat'iy). */
    fun visualThreshold(sensitivity: GuardSensitivity): Float = when (sensitivity) {
        GuardSensitivity.LOW -> 0.96f
        GuardSensitivity.MEDIUM -> 0.91f
        GuardSensitivity.HIGH -> 0.85f
    }

    private fun isAdultDomain(host: String): Boolean {
        if (ADULT_DOMAINS.any { host == it || host.endsWith(".$it") }) return true
        if (ADULT_TLDS.any { host.endsWith(it) }) return true
        return false
    }

    /* ==================== normalizatsiya ==================== */

    private fun tokenize(text: String): List<String> {
        if (text.isBlank()) return emptyList()
        val limit = if (text.length > 12_000) text.substring(0, 12_000) else text
        var lowered = limit.lowercase()
            .replace('\u200B', ' ')
            .replace('\u00A0', ' ')
            .replace('\uFEFF', ' ')
            .replace('ё', 'е')
        if (lowered.contains("sex") || lowered.contains("jinsiy") || lowered.contains("сексуаль")) {
            for (phrase in SAFE_PHRASES) lowered = phrase.replace(lowered, " safecontext ")
        }
        val raw = lowered.split(NON_ALNUM_RE)
        val out = ArrayList<String>(raw.size)
        for (token in raw) {
            if (token.isEmpty()) continue
            val latin = latinize(token)
            out += latin
            if (latin != token) out += token
        }
        // "p o r n" kabi bo'laklab yozilgan so'zlar
        var i = 0
        while (i < out.size) {
            if (out[i].length == 1 && out[i][0].isLetter()) {
                var j = i
                val sb = StringBuilder()
                while (j < out.size && out[j].length == 1 && out[j][0].isLetter()) {
                    sb.append(out[j]); j++
                }
                if (j - i >= 3) out += sb.toString()
                i = j
            } else {
                i++
            }
        }
        return out
    }

    private fun latinize(token: String): String {
        var changed = false
        val sb = StringBuilder(token.length)
        for (ch in token) {
            val mapped = HOMOGLYPHS[ch]
            if (mapped != null) {
                sb.append(mapped); changed = true
            } else {
                sb.append(ch)
            }
        }
        return if (changed) sb.toString() else token
    }

    private fun deLeet(token: String): String {
        var changed = false
        val sb = StringBuilder(token.length)
        for (ch in token) {
            val mapped = LEET[ch]
            if (mapped != null) {
                sb.append(mapped); changed = true
            } else {
                sb.append(ch)
            }
        }
        return if (changed) sb.toString() else token
    }

    private fun squeeze(value: String): String {
        if (value.length < 4) return value
        val sb = StringBuilder(value.length)
        var prev = ' '
        for (ch in value) {
            if (ch != prev) sb.append(ch)
            prev = ch
        }
        return if (sb.length < 3) value else sb.toString()
    }

    /* ==================== ballash ==================== */

    private fun strongHits(tokens: List<String>): List<String> {
        val hits = ArrayList<String>(2)
        for (token in tokens) {
            val term = normalizeToken(token)
            if (term in STRONG) hits += term
        }
        return hits.distinct()
    }

    private fun brandHits(tokens: List<String>): List<String> {
        val hits = ArrayList<String>(2)
        for (token in tokens) {
            val term = normalizeToken(token)
            if (term in BRANDS) hits += term
        }
        return hits.distinct()
    }

    private fun mediumGroups(tokens: List<String>): Map<String, String> {
        val hits = LinkedHashMap<String, String>()
        for (token in tokens) {
            val term = normalizeToken(token)
            if (term.isEmpty()) continue
            if (term in BRANDS) {
                hits["brand"] = term
                continue
            }
            for ((group, terms) in MEDIUM_GROUPS) {
                if (term in terms) {
                    hits[group] = term
                    break
                }
            }
        }
        return hits
    }

    private fun weakGroups(tokens: List<String>): Map<String, String> {
        val hits = LinkedHashMap<String, String>()
        for (token in tokens) {
            val term = normalizeToken(token)
            if (term.isEmpty()) continue
            for ((group, terms) in WEAK_GROUPS) {
                if (term in terms) {
                    hits[group] = term
                    break
                }
            }
        }
        return hits
    }

    private fun hasSafeContext(tokens: List<String>): Boolean =
        tokens.any { it in SAFE_CONTEXT }

    private fun normalizeToken(token: String): String {
        val deLeeted = deLeet(token)
        return when {
            deLeeted != token -> deLeeted
            token.length >= 4 -> squeeze(token)
            else -> token
        }
    }
}
