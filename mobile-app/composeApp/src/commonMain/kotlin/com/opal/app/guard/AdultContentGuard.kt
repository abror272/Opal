package com.opal.app.guard

/**
 * 18+ (kattalar uchun) kontent detektori.
 *
 * Sof Kotlin — platformadan mustaqil, shuning uchun JVM'da test qilish oson.
 * Android'da `OpalAccessibilityService` ekrandagi matnni yig'ib shu yerga beradi.
 *
 * Aniqlash 3 manbadan ishlaydi:
 *  1. HAVOLA (URL/domén) — eng ishonchli signal (200 ball, darhol blok).
 *  2. MATN — ko'p tilli (o'zbek / rus / ingliz) lug'at + obfuskatsiyaga chidamli
 *     normalizatsiya (p0rn, p-o-r-n, poooorn, Cyrillic↔Latin almashtirish).
 *  3. ILOVA NOMI — 18+ ilovalar uchun.
 *
 * Xavfsiz kontekst (ta'lim, tibbiyot, yangilik) so'zlari ballni pasaytiradi —
 * "sex education" kabi sahifalar bloklanmasligi uchun.
 */
enum class GuardSensitivity(val code: String, val threshold: Int) {
    /** Faqat aniq signal: domén yoki kuchli so'z ("porn", "hentai"). */
    LOW("low", 100),

    /** Standart: bitta o'rtacha signal ("nude", "seks", "18+") yetadi. */
    MEDIUM("medium", 60),

    /** Eng qat'iy: "adult" kabi zaif signal ham bloklaydi. */
    HIGH("high", 35);

    companion object {
        fun from(code: String?): GuardSensitivity =
            entries.firstOrNull { it.code == code } ?: MEDIUM
    }
}

enum class GuardKind { NONE, TEXT, SITE, APP }

data class GuardInput(
    val screenText: String = "",
    val packageName: String = "",
    val appLabel: String = "",
    val windowTitle: String = ""
)

data class GuardVerdict(
    val score: Int = 0,
    val kind: GuardKind = GuardKind.NONE,
    val signals: List<String> = emptyList(),
    val host: String? = null
) {
    fun isAdult(sensitivity: GuardSensitivity): Boolean =
        kind != GuardKind.NONE && score >= sensitivity.threshold

    /** Foydalanuvchiga ko'rsatiladigan qisqa sabab (so'zning o'zi ko'rsatilmaydi). */
    fun reasonLabel(): String = when (kind) {
        GuardKind.SITE -> "18+ sayt aniqlandi"
        GuardKind.APP -> "18+ ilova aniqlandi"
        GuardKind.TEXT -> "18+ kontent aniqlandi"
        GuardKind.NONE -> ""
    }
}

object AdultContentGuard {

    /* ==================== og'irliklar ==================== */

    private const val W_STRONG = 100
    private const val W_MEDIUM = 65
    private const val W_WEAK = 35
    private const val W_18PLUS = 60
    private const val W_REPEAT = 12
    private const val SAFE_CONTEXT_PENALTY = 45

    /** Aniq 18+ atamalar (bitta o'zi ham yetarli). */
    private val STRONG_TERMS: Map<String, Int> = buildMap {
        listOf(
            // ingliz
            "porn", "porno", "pornography", "pornographic", "pornhub", "pornstar",
            "xxx", "xrated", "hentai", "rule34", "e621", "onlyfans", "fansly",
            "chaturbate", "stripchat", "cam4", "bongacams", "livejasmin", "spankbang",
            "xvideos", "xnxx", "xhamster", "redtube", "youporn", "brazzers", "nhentai",
            "hanime", "missav", "javbus", "javhd", "supjav", "porntrex", "tnaflix",
            "beeg", "txxx", "motherless", "adultfriendfinder", "ashleymadison",
            "gangbang", "creampie", "cumshot", "blowjob", "handjob", "deepthroat",
            "orgy", "incest", "bestiality", "necrophilia", "pedophilia",
            // rus
            "порно", "порнография", "порнуха", "порносайт", "хентай", "инцест",
            "минет", "куннилингус", "мастурбация", "онанизм", "сперма",
            "трахать", "трахаться", "выебать", "дрочка", "сексшоп", "секс-шоп",
            "групповуха", "зоофилия", "педофилия",
            // o'zbek (lotin)
            "pornografiya", "pornografik", "seks-shop", "seksshop"
        ).forEach { put(it, W_STRONG) }
    }

    /** Kuchli, lekin ba'zan tibbiy/ta'limiy kontekstda uchraydigan atamalar. */
    private val MEDIUM_TERMS: Map<String, Int> = buildMap {
        listOf(
            "nude", "nudes", "nudity", "naked", "nakedly", "erotic", "erotica",
            "eroticism", "camgirl", "camsex", "webcamsex", "escort", "nsfw",
            "fetish", "orgasm", "milf", "bdsm", "sex", "seks", "seksual",
            "sexual", "sexuality", "sexcam", "sexting", "striptease", "stripper",
            "lapdance", "brothel", "prostitute", "prostitution", "sperm",
            "vagina", "penis", "clitoris", "erection", "masturbation",
            // rus
            "эротика", "эротический", "нагота", "голая", "голые", "обнажённая",
            "обнаженная", "обнажённые", "обнаженные", "интим", "проститутка",
            "шлюха", "фетиш", "бдсм", "вагина", "пенис", "клитор", "эрекция",
            "порноактриса", "разврат", "блуд",
            // o'zbek
            "yalang'och", "yalangoch", "shahvoniy", "shahvat", "fohisha",
            "jinsiy", "jinsiyaloqa", "eros", "intim"
        ).forEach { put(it, W_MEDIUM) }
    }

    /** Zaif signallar — faqat birgalikda yoki yuqori sezgirlikda bloklaydi. */
    private val WEAK_TERMS: Map<String, Int> = buildMap {
        listOf(
            "sex", "adult", "adults", "adultcontent", "adultvideo", "18plus",
            "seks", "kattalar", "voyaga", "fohishalik", "sutenyor", "erotik",
            // rus
            "секс", "сексуальный", "эротичный", "совращение", "растление"
        ).forEach { put(it, W_WEAK) }
    }

    /**
     * Atamalar birlashtirilgan jadval (eng yuqori og'irlik ustun).
     * Kalit — normalizatsiya qilingan atama.
     */
    private val TERMS: Map<String, Int> = HashMap<String, Int>().apply {
        putAll(WEAK_TERMS)
        putAll(MEDIUM_TERMS)
        putAll(STRONG_TERMS)
        // takroriy harflar uchun ixchamlangan variant: "pooorn" -> "porn"
        for ((term, weight) in entries.toList()) {
            val squeezed = squeeze(term)
            if (squeezed != term && weight > (this[squeezed] ?: 0)) put(squeezed, weight)
        }
    }

    /** Ta'lim/tibbiyot/yangilik konteksti — ballni pasaytiradi. */
    private val SAFE_CONTEXT: Set<String> = setOf(
        "education", "educational", "school", "lesson", "course", "science",
        "research", "study", "article", "wikipedia", "encyclopedia", "news",
        "report", "statistics", "medical", "medicine", "health", "clinic",
        "doctor", "gynecology", "gynecologist", "urology", "urologist",
        "sexology", "sexologist", "therapy", "reproductive", "pregnancy",
        "ta'lim", "talim", "tarbiya", "tarbiyaviy", "dars", "darslik", "ilm", "ilmiy", "tadqiqot",
        "maqola", "yangilik", "hisobot", "statistika", "tibbiyot", "tibbiy",
        "shifokor", "klinika", "ginekolog", "urolog", "salomatlik", "qonun",
        "huquq", "jinoyat", "profilaktika", "reproduktiv", "safecontext",
        "образование", "учебник", "урок", "наука", "исследование", "статья",
        "новости", "медицина", "медицинский", "здоровье", "врач",
        "гинеколог", "уролог", "сексология", "терапия", "закон", "право"
    )

    /** Mashhur 18+ doménlar (suffiks bo'yicha ham mos keladi). */
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

    /** 18+ uchun ajratilgan yuqori darajali doménlar. */
    private val ADULT_TLDS: List<String> = listOf(
        ".xxx", ".porn", ".sex", ".adult", ".sexy", ".cam"
    )

    private val DOMAIN_RE =
        Regex("""(?i)\b((?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\.)+(?:[a-z]{2,24}))\b""")

    private val NON_ALNUM_RE = Regex("""[^\p{L}\p{N}]+""")

    private val PLUS_18_RE = Regex("""(?i)\b18\s*\+""")

    /**
     * Xavfsiz iboralar: tahlildan OLDIN neytrallanadi.
     * Masalan "same-sex marriage" yoki "sex education" — 18+ kontent emas.
     */
    private val SAFE_PHRASES = listOf(
        Regex("(?i)same[\\s-]*sex"),
        Regex("(?i)sex\\s*education"),
        Regex("(?i)sexual\\s*education"),
        Regex("(?i)sexual\\s*health"),
        Regex("(?i)sex\\s*ology"),
        Regex("(?i)jinsiy\\s*tarbiya"),
        Regex("(?i)jinsiy\\s*salomatlik"),
        Regex("(?i)reproductive\\s*health"),
        Regex("(?i)сексуальное\\s*образование"),
        Regex("(?i)сексуальное\\s*здоровье")
    )

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

    fun evaluate(input: GuardInput): GuardVerdict {
        // 1) Havola — eng kuchli signal, qolganini tekshirish shart emas.
        val host = findAdultHost(input.screenText)
        if (host != null) {
            return GuardVerdict(score = 200, kind = GuardKind.SITE, signals = listOf(host), host = host)
        }

        // 2) Ekrandagi matn.
        val textTokens = tokenize(input.screenText + "\n" + input.windowTitle)
        val (textScore, textSignals) = scoreTokens(textTokens, safeContext = true)

        // 3) Ilova nomi.
        val labelTokens = tokenize(input.appLabel)
        val (labelScore, labelSignals) = scoreTokens(labelTokens, safeContext = false)

        // 4) "18+" ni alohida tekshiramiz (tokenizatsiyada "+" yo'qoladi).
        val plus18 = PLUS_18_RE.containsMatchIn(input.screenText) ||
            PLUS_18_RE.containsMatchIn(input.appLabel)
        // "adult" so'zi token sifatida allaqachon WEAK jadvalda.

        var score = textScore
        var kind = GuardKind.NONE
        val signals = textSignals.toMutableList()
        if (labelScore >= textScore && labelScore > 0) {
            score = maxOf(score, labelScore + minOf(textScore, W_MEDIUM))
            signals += labelSignals
            kind = GuardKind.APP
        } else if (textScore > 0) {
            kind = GuardKind.TEXT
        } else if (labelScore > 0) {
            score = maxOf(score, labelScore)
            signals += labelSignals
            kind = GuardKind.APP
        }
        if (plus18) {
            score += W_18PLUS
            signals += "18+"
            if (kind == GuardKind.NONE) kind = GuardKind.TEXT
        }

        return GuardVerdict(
            score = score.coerceIn(0, 250),
            kind = kind,
            signals = signals.distinct().take(8)
        )
    }

    /** Faqat ilova nomi bo'yicha tez tekshiruv (Watchdog uchun). */
    fun evaluateLabel(appLabel: String, sensitivity: GuardSensitivity): GuardVerdict {
        if (appLabel.isBlank()) return GuardVerdict()
        val (score, signals) = scoreTokens(tokenize(appLabel), safeContext = false)
        val plus18 = if (PLUS_18_RE.containsMatchIn(appLabel)) W_18PLUS else 0
        val total = score + plus18
        if (total <= 0) return GuardVerdict()
        return GuardVerdict(
            score = total.coerceAtMost(250),
            kind = GuardKind.APP,
            signals = (signals + if (plus18 > 0) listOf("18+") else emptyList()).distinct()
        ).takeIf { it.isAdult(sensitivity) } ?: GuardVerdict()
    }

    /** Matnda 18+ domén bormi? */
    fun findAdultHost(text: String): String? {
        if (text.isBlank()) return null
        for (match in DOMAIN_RE.findAll(text.take(20_000))) {
            val host = match.groupValues[1].lowercase().trimEnd('.')
            val bare = host.removePrefix("www.").removePrefix("m.")
            if (isAdultDomain(bare)) return bare
        }
        return null
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
        // Xavfsiz iboralarni neytrallash (faqat tegishli so'z bo'lsa — arzon tekshiruv).
        if (lowered.contains("sex") || lowered.contains("jinsiy") ||
            lowered.contains("сексуаль")
        ) {
            for (phrase in SAFE_PHRASES) lowered = phrase.replace(lowered, " safecontext ")
        }
        val raw = lowered.split(NON_ALNUM_RE)
        val out = ArrayList<String>(raw.size)
        for (token in raw) {
            if (token.isEmpty()) continue
            // Kirill harflari aralashgan lotin so'zlar: "pоrn" -> "porn"
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
        if (value.length < 3) return value
        val sb = StringBuilder(value.length)
        var prev = ' '
        for (ch in value) {
            if (ch != prev) sb.append(ch)
            prev = ch
        }
        return sb.toString()
    }

    /* ==================== ballash ==================== */

    private fun scoreTokens(tokens: List<String>, safeContext: Boolean): Pair<Int, List<String>> {
        if (tokens.isEmpty()) return 0 to emptyList()
        var score = 0
        val signals = ArrayList<String>(4)
        val seen = HashMap<String, Int>()

        for (raw in tokens) {
            val direct = TERMS[raw]
            val deLeeted = deLeet(raw)
            val weight = direct ?: TERMS[deLeeted] ?: run {
                if (deLeeted.length >= 4) TERMS[squeeze(deLeeted)] else null
            } ?: continue

            val key = if (direct != null) raw else deLeeted
            val count = (seen[key] ?: 0) + 1
            seen[key] = count
            if (count == 1) {
                score += weight
                signals += key
            } else if (count <= 5) {
                // Mavzu takrorlanmoqda — qo'shimcha signal.
                score += W_REPEAT
            }
        }

        if (safeContext && signals.isNotEmpty()) {
            val safeHits = tokens.count { it in SAFE_CONTEXT }
            if (safeHits > 0) {
                score -= SAFE_CONTEXT_PENALTY * minOf(safeHits, 2)
            }
        }
        return score.coerceAtLeast(0) to signals
    }

}
