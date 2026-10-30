package com.luoh.imgbed

import com.luoh.imgbed.compression.NamingEngine
import com.luoh.imgbed.model.NamingMode
import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale

class NamingEngineTest {
    @Test
    fun keepsBaseNameAndChangesExtension() {
        assertEquals(
            "photo.webp",
            NamingEngine.build("photo.JPG", "webp", NamingMode.ORIGINAL, "", 1),
        )
    }

    @Test
    fun expandsTemplateAndSanitizesInvalidCharacters() {
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).parse("2026-10-30 22:10:09")!!
        assertEquals(
            "20261030_221009_007_a_b.avif",
            NamingEngine.build(
                originalName = "a:b.png",
                outputExtension = "avif",
                mode = NamingMode.TEMPLATE,
                template = "{yyyy}{MM}{dd}_{HH}{mm}{ss}_{index}_{name}",
                index = 7,
                now = date,
                uuid = "fixed",
            ),
        )
    }

    @Test
    fun templateExtensionIsNotDuplicated() {
        assertEquals(
            "photo.webp",
            NamingEngine.build("photo.png", "webp", NamingMode.TEMPLATE, "{name}.{ext}", 1),
        )
    }
}
