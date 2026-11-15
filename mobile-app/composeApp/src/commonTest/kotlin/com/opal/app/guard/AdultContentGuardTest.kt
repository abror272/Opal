package com.opal.app.guard

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AdultContentGuardTest {

    private fun check(text: String, label: String = ""): Pair<Boolean, GuardVerdict> {
        val verdict = AdultContentGuard.evaluate(GuardInput(screenText = text, appLabel = label))
        return verdict.isAdult(GuardSensitivity.MEDIUM) to verdict
    }

    @Test
    fun detectsAdultDomains() {
        val verdict = AdultContentGuard.evaluate(
            GuardInput(screenText = "Salom, pornhub.com saytiga o'tildi")
        )
        assertEquals(GuardKind.SITE, verdict.kind)
        assertEquals("pornhub.com", verdict.host)
        assertTrue(verdict.isAdult(GuardSensitivity.LOW))
    }

    @Test
    fun detectsDomainsInUrlsAndSubdomains() {
        assertNotNull(AdultContentGuard.findAdultHost("https://m.xvideos.com/video123"))
        assertNotNull(AdultContentGuard.findAdultHost("www.onlyfans.com/user"))
        assertNotNull(AdultContentGuard.findAdultHost("example.xxx"))
        assertEquals(null, AdultContentGuard.findAdultHost("https://wikipedia.org/wiki/Sex"))
        assertEquals(null, AdultContentGuard.findAdultHost("example.com"))
    }

    @Test
    fun detectsStrongTerms() {
        assertTrue(check("eng yaxshi porn saytlari").first)
        assertTrue(check("bu yerda порно bor").first)
        assertTrue(check("xxx videos").first)
        assertTrue(check("hentai manga").first)
    }

    @Test
    fun detectsObfuscatedTerms() {
        assertTrue(check("p0rn video").first)
        assertTrue(check("p o r n").first)
        assertTrue(check("poooorn").first)
        // kirill "о" bilan yozilgan lotin so'z
        assertTrue(check("p\u043Ern").first)
        assertTrue(check("P.O.R.N.").first)
    }

    @Test
    fun safeContextIsNotBlocked() {
        assertFalse(check("sex education darsligi").first)
        assertFalse(check("same-sex marriage haqida yangilik").first)
        assertFalse(check("jinsiy tarbiya va reproduktiv salomatlik").first)
        assertFalse(check("sexual health clinic and medical research").first)
        assertFalse(check("гинеколог ва репродуктив саломатлик").first)
        assertFalse(check("bugun havo juda issiq, parkda sayr qildik").first)
        assertFalse(check("instagram post: yangi kofe do'koni ochildi").first)
    }

    @Test
    fun mediumBlocksOneClearSignal() {
        assertTrue(check("18+ video").first)
        assertTrue(check("nude photo").first)
        assertTrue(check("seks").first)
    }

    @Test
    fun weakSignalNeedsHighSensitivity() {
        val verdict = AdultContentGuard.evaluate(GuardInput(screenText = "adult"))
        assertFalse(verdict.isAdult(GuardSensitivity.MEDIUM))
        assertTrue(verdict.isAdult(GuardSensitivity.HIGH))
    }

    @Test
    fun adultAppLabelIsDetected() {
        val verdict = AdultContentGuard.evaluate(GuardInput(appLabel = "Porn Hub"))
        assertEquals(GuardKind.APP, verdict.kind)
        assertTrue(verdict.isAdult(GuardSensitivity.MEDIUM))

        val labelOnly = AdultContentGuard.evaluateLabel("OnlyFans", GuardSensitivity.MEDIUM)
        assertTrue(labelOnly.isAdult(GuardSensitivity.MEDIUM))

        val normal = AdultContentGuard.evaluateLabel("Instagram", GuardSensitivity.MEDIUM)
        assertFalse(normal.isAdult(GuardSensitivity.MEDIUM))
    }

    @Test
    fun repeatedSignalsAddUp() {
        val once = AdultContentGuard.evaluate(GuardInput(screenText = "seks"))
        val many = AdultContentGuard.evaluate(GuardInput(screenText = "seks seks seks seks"))
        assertTrue(many.score > once.score)
        assertTrue(many.isAdult(GuardSensitivity.MEDIUM))
    }

    @Test
    fun emptyInputIsSafe() {
        val verdict = AdultContentGuard.evaluate(GuardInput())
        assertEquals(GuardKind.NONE, verdict.kind)
        assertFalse(verdict.isAdult(GuardSensitivity.HIGH))
    }
}
