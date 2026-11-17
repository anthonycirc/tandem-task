package dev.thingscompanion.cloud

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TitleEmojiTest {

    @Test
    fun `an emoji goes in front of the title, replacing the one already there`() {
        assertEquals("📞 Call Ana", TitleEmoji.apply("Call Ana", "📞"))
        assertEquals("🛒 Call Ana", TitleEmoji.apply("📞 Call Ana", "🛒"))
        assertEquals("🛒", TitleEmoji.apply("", "🛒"))
    }

    @Test
    fun `no emoji takes the current one off`() {
        assertEquals("Call Ana", TitleEmoji.apply("📞 Call Ana", null))
        assertEquals("Call Ana", TitleEmoji.apply("Call Ana", null))
    }

    @Test
    fun `the leading emoji is read back, including ones made of several code points`() {
        assertEquals("📞", TitleEmoji.leading("📞 Call Ana"))
        assertEquals("❤️", TitleEmoji.leading("❤️ Mum"))
        assertEquals("👨‍💻", TitleEmoji.leading("👨‍💻 Ship"))
        assertNull(TitleEmoji.leading("Call Ana"))
        assertNull(TitleEmoji.leading("2 calls"))
    }

    @Test
    fun `a leading symbol that is not an emoji stays part of the title`() {
        assertNull(TitleEmoji.leading("→ Next step"))
        assertNull(TitleEmoji.leading("© 2026 notice"))
        assertNull(TitleEmoji.leading("★ Starred"))
        assertEquals("📞 → Next step", TitleEmoji.apply("→ Next step", "📞"))
        assertEquals("→ Next step", TitleEmoji.apply("→ Next step", null))
    }

    @Test
    fun `a flag, two regional indicators, is one emoji`() {
        assertEquals("🇪🇸", TitleEmoji.leading("🇪🇸 Trip"))
        assertEquals("📞 Trip", TitleEmoji.apply("🇪🇸 Trip", "📞"))
        assertEquals("Trip", TitleEmoji.apply("🇪🇸 Trip", null))
    }
}
