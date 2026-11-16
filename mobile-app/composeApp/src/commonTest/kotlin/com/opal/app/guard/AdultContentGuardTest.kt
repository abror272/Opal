package com.opal.app.guard

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AdultContentGuardTest {

    private fun eval(
        text: String = "",
        address: String = "",
        label: String = "",
        sensitivity: GuardSensitivity = GuardSensitivity.MEDIUM
    ): GuardVerdict = AdultContentGuard.evaluate(
        GuardInput(screenText = text, addressBarText = address, appLabel = label),
        sensitivity
    )

    @Test
    fun adultDomainIsBlockedOnlyFromAddressBar() {
        val fromBar = eval(address = "https://m.xvideos.com/video123")
        assertEquals(GuardKind.SITE, fromBar.kind)
        assertEquals("xvideos.com", fromBar.host)
        assertTrue(fromBar.isBlocking)

        // Sahifa matnidagi havola (masalan izohdagi link) yolg'iz bloklamaydi.
        val inBody = eval(text = "Look at this: pornhub.com")
        assertFalse(inBody.isBlocking)
    }

    @Test
    fun adultTldAndSubdomains() {
        assertNotNull(AdultContentGuard.findAdultHost("example.xxx"))
        assertNotNull(AdultContentGuard.findAdultHost("www.onlyfans.com/user"))
        assertEquals(null, AdultContentGuard.findAdultHost("wikipedia.org"))
        assertEquals(null, AdultContentGuard.findAdultHost("example.com"))
    }

    @Test
    fun strongTermBlocksAlone() {
        assertTrue(eval(text = "eng yaxshi porn saytlari").isBlocking)
        assertTrue(eval(text = "bu yerda порно bor").isBlocking)
        assertTrue(eval(text = "hentai manga").isBlocking)
    }

    @Test
    fun obfuscatedStrongTerm() {
        assertTrue(eval(text = "p0rn video").isBlocking)
        assertTrue(eval(text = "p o r n").isBlocking)
        assertTrue(eval(text = "poooorn").isBlocking)
        assertTrue(eval(text = "p\u043Ern").isBlocking) // kirill "о"
    }

    @Test
    fun singleAmbiguousTermDoesNotBlock() {
        assertFalse(eval(text = "sex").isBlocking)
        assertFalse(eval(text = "adult").isBlocking)
        assertFalse(eval(text = "nude").isBlocking)
        assertFalse(eval(text = "18+").isBlocking)
        assertFalse(eval(text = "18+ video").isBlocking)
    }

    @Test
    fun youtubeStyleContentIsNotBlocked() {
        assertFalse(eval(text = "Official music video: Sex — new song 2026").isBlocking)
        assertFalse(eval(text = "Shorts Subscribe Comments Recommended Trending").isBlocking)
        assertFalse(eval(text = "Sex education: reproductive health explained").isBlocking)
        assertFalse(eval(text = "Sexual harassment: news report and law changes").isBlocking)
        assertFalse(eval(text = "Nude art in the museum — documentary").isBlocking)
        assertFalse(eval(text = "Jinsiy tarbiya darsligi, tibbiy maqola").isBlocking)
    }

    @Test
    fun twoMediumGroupsBlock() {
        assertTrue(eval(text = "nude erotic photo set").isBlocking)
        assertTrue(eval(text = "fetish bdsm gallery").isBlocking)
    }

    @Test
    fun mediumPlusWeakSignalsBlockAtMedium() {
        // medium: nudity; weak: sex + adult
        assertTrue(eval(text = "nude sex adult").isBlocking)
    }

    @Test
    fun weakOnlyNeedsHigh() {
        val text = "sex adult intim"
        assertFalse(eval(text = text, sensitivity = GuardSensitivity.MEDIUM).isBlocking)
        assertTrue(eval(text = text, sensitivity = GuardSensitivity.HIGH).isBlocking)
    }

    @Test
    fun adultAppLabelIsBlocked() {
        val byLabel = eval(label = "Porn Hub")
        assertEquals(GuardKind.APP, byLabel.kind)
        assertTrue(byLabel.isBlocking)

        val normal = AdultContentGuard.evaluateLabel("Instagram")
        assertFalse(normal.isBlocking)
    }

    @Test
    fun strongTermWinsEvenInSafeContext() {
        assertTrue(eval(text = "porn addiction therapy research").isBlocking)
    }

    @Test
    fun visualThresholdsAreOrdered() {
        val low = AdultContentGuard.visualThreshold(GuardSensitivity.LOW)
        val medium = AdultContentGuard.visualThreshold(GuardSensitivity.MEDIUM)
        val high = AdultContentGuard.visualThreshold(GuardSensitivity.HIGH)
        assertTrue(low > medium && medium > high)
    }

    @Test
    fun emptyInputIsSafe() {
        val verdict = eval()
        assertEquals(GuardKind.NONE, verdict.kind)
        assertFalse(verdict.isBlocking)
    }
}
