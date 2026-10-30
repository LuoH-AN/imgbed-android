package com.luoh.imgbed.ui

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luoh.imgbed.data.AppAppearance
import com.luoh.imgbed.data.AppearancePreferences
import com.luoh.imgbed.model.TransferStatus
import com.luoh.imgbed.model.UploadChannel
import com.luoh.imgbed.ui.agora.ColorSchemePreset
import com.luoh.imgbed.ui.agora.SchemeStyle
import com.luoh.imgbed.ui.agora.ThemeMode
import com.luoh.imgbed.ui.agora.colorSchemeForPreset
import com.luoh.imgbed.ui.agora.colorSchemeForSeed
import com.luoh.imgbed.ui.agora.withAmoledBackground
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class, qualifiers = "w400dp-h800dp-mdpi")
class AppearanceUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private lateinit var model: AppViewModel
    private lateinit var scheme: ColorScheme
    private lateinit var root: View

    @Before fun reset() {
        compose.activity.getSharedPreferences("imgbed_appearance", Context.MODE_PRIVATE).edit().clear().commit()
        compose.activity.getSharedPreferences("imgbed_settings", Context.MODE_PRIVATE).edit().clear().commit()
        compose.activity.noBackupFilesDir.resolve("imgbed-api-token").delete()
        model = AppViewModel(compose.activity.application)
        model.saveAppearance(AppAppearance(mode = ThemeMode.LIGHT))
    }

    private fun showAppearance() {
        compose.setContent {
            val appearance by model.appearance.collectAsStateWithLifecycle()
            val view = LocalView.current
            ImgBedTheme(appearance) {
                val colors = MaterialTheme.colorScheme
                SideEffect { scheme = colors; root = view }
                Surface(Modifier.fillMaxSize()) {
                    PageScaffold("应用外观", onBack = {}, reserveDock = false) {
                        item { AppearanceSettings(appearance, model::saveAppearance) }
                    }
                }
            }
        }
    }

    private fun clickRow(label: String) { compose.onNodeWithText(label).performScrollTo().performClick() }
    private fun select(label: String, choice: String) { clickRow(label); compose.onNodeWithText(choice).performClick() }
    private fun assertScheme(expected: ColorScheme) {
        compose.runOnIdle {
            assertEquals(expected.primary, scheme.primary)
            assertEquals(expected.tertiary, scheme.tertiary)
            assertEquals(expected.background, scheme.background)
            assertEquals(expected.surfaceContainer, scheme.surfaceContainer)
        }
    }

    @Test fun themeControlsApplyImmediatelyAndSurviveNewPreferencesInstance() {
        showAppearance()
        clickRow("AMOLED")
        compose.onNodeWithText("AMOLED").assertIsOn()
        select("主题", "深色")
        select("配色方案", "日落")
        select("方案风格", "鲜艳")
        assertScheme(colorSchemeForPreset(ColorSchemePreset.SUNSET, SchemeStyle.VIBRANT, true).withAmoledBackground(true, true))
        compose.runOnIdle {
            val saved = AppearancePreferences(compose.activity).read()
            assertTrue(saved.amoled)
            assertEquals(ThemeMode.DARK, saved.mode)
            assertEquals(ColorSchemePreset.SUNSET, saved.preset)
            assertEquals(SchemeStyle.VIBRANT, saved.style)
        }
    }

    @Test fun dynamicColorsDisableManualControlsAndRestoreCustomSeed() {
        val accent = android.graphics.Color.parseColor("#AD1457")
        model.saveAppearance(model.appearance.value.copy(accent = accent))
        showAppearance()
        clickRow("动态颜色")
        compose.onNodeWithText("配色方案").assertIsNotEnabled()
        compose.onNodeWithText("方案风格").assertIsNotEnabled()
        compose.onNodeWithText("主调颜色").assertIsNotEnabled()
        assertScheme(dynamicLightColorScheme(compose.activity))
        clickRow("动态颜色")
        assertScheme(colorSchemeForSeed(accent, style = SchemeStyle.NEUTRAL))
        compose.runOnIdle { assertEquals(accent, AppearancePreferences(compose.activity).read().accent) }
    }

    @Test fun customColorDialogValidatesInputAndPresetClearsCustomColor() {
        showAppearance()
        clickRow("主调颜色")
        compose.onNode(hasSetTextAction()).performTextReplacement("#XYZ")
        compose.onNodeWithText("应用", substring = false).assertIsNotEnabled()
        compose.onNode(hasSetTextAction()).performTextReplacement("#111111")
        compose.onNodeWithText("应用", substring = false).performClick()
        val accent = android.graphics.Color.parseColor("#111111")
        assertScheme(colorSchemeForSeed(accent, style = SchemeStyle.NEUTRAL))
        select("配色方案", "森林")
        compose.runOnIdle { assertNull(AppearancePreferences(compose.activity).read().accent) }
        assertScheme(colorSchemeForPreset(ColorSchemePreset.FOREST, SchemeStyle.NEUTRAL))
    }

    @Test fun settingsOverlayReturnsToTheListAndReducedMotionPersists() {
        compose.setContent { ImgBedApp(model) }
        compose.onNodeWithText("设置").performClick()
        compose.onNodeWithText("应用动效").performScrollTo()
        assertTrue(compose.onNodeWithText("应用动效").fetchSemanticsNode().boundsInRoot.bottom <
            compose.onNodeWithText("文件").fetchSemanticsNode().boundsInRoot.top)
        compose.onNodeWithText("应用动效").performClick()
        compose.onNodeWithText("减少动效").performScrollTo().performClick()
        compose.onNodeWithContentDescription("返回").performClick()
        compose.onNodeWithText("应用外观").assertIsDisplayed()
        compose.onNodeWithText("应用动效").performClick()
        compose.onNodeWithText("减少动效").assertIsOn()
        compose.runOnIdle { assertTrue(AppearancePreferences(compose.activity).read().reduceMotion) }
    }

    @Test @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun darkAppearanceScreenshotUsesReferenceTypographyAndGroups() {
        model.saveAppearance(AppAppearance(mode = ThemeMode.DARK, amoled = true,
            preset = ColorSchemePreset.LAVENDER, style = SchemeStyle.TONAL_SPOT))
        showAppearance()
        compose.onNodeWithText("主调颜色").assertIsDisplayed()
        saveScreenshot("appearance-dark")
    }

    @Test @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun uploadScreenshotContainsNoPromotionalCopy() {
        compose.setContent {
            val view = LocalView.current
            SideEffect { root = view }
            ImgBedApp(model)
        }
        compose.onNodeWithText("选择文件").assertIsDisplayed()
        compose.onNodeWithText("轻放云端").assertDoesNotExist()
        compose.onNodeWithText("让分享，\n再轻一点。").assertDoesNotExist()
        saveScreenshot("upload-light")
    }

    @Test fun uploadShortcutReturnsToQueueWithBothBackActions() {
        addDocuments(3)
        compose.setContent { ImgBedApp(model) }
        compose.onNodeWithText("图片处理").performScrollTo().performClick()
        compose.onNodeWithText("快速方案").assertIsDisplayed()
        compose.onNodeWithContentDescription("返回").performClick()
        compose.onNodeWithText("先设置服务连接").assertIsDisplayed()
        compose.onNodeWithText("图片处理").performScrollTo().performClick()
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithText("先设置服务连接").assertIsDisplayed()
        compose.runOnIdle { assertEquals(3, model.documents.value.size) }
    }

    @Test fun channelAndFolderCanBeChangedWithoutLeavingUpload() {
        compose.setContent { ImgBedApp(model) }
        select("上传渠道", "Cloudflare R2")
        compose.runOnIdle { assertEquals(UploadChannel.R2, model.upload.value.channel) }
        clickRow("上传目录")
        compose.onNode(hasSetTextAction()).performTextReplacement("photos/2026")
        compose.onNodeWithText("保存").performClick()
        compose.runOnIdle { assertEquals("photos/2026", model.upload.value.uploadFolder) }
        compose.onNodeWithText("选择文件").assertIsDisplayed()
    }

    @Test fun remoteConnectionShortcutReturnsToFiles() {
        compose.setContent { ImgBedApp(model) }
        compose.onNodeWithText("文件").performClick()
        compose.onNodeWithText("连接图床后查看文件").assertIsDisplayed()
        compose.onNodeWithText("设置服务连接").performClick()
        compose.onNodeWithText("ImgBed 地址").assertIsDisplayed()
        compose.onNodeWithContentDescription("返回").performClick()
        compose.onNodeWithText("连接图床后查看文件").assertIsDisplayed()
        compose.runOnIdle { assertTrue(model.remoteError.value.isBlank()) }
    }

    @Test fun missingConnectionDoesNotProcessOrFailQueuedFiles() {
        addDocuments(2)
        compose.runOnIdle { model.uploadAll() }
        compose.runOnIdle {
            assertFalse(model.busy.value)
            assertFalse(model.uploading.value)
            assertTrue(model.documents.value.all { it.status == TransferStatus.WAITING })
            assertEquals("请先设置服务连接", model.message.value)
        }
    }

    @Test fun invalidServerAddressCannotBeSaved() {
        compose.setContent { ImgBedApp(model) }
        clickRow("服务连接")
        clickRow("ImgBed 地址")
        compose.onNode(hasSetTextAction()).performTextReplacement("invalid address")
        compose.onNodeWithText("保存").assertIsNotEnabled()
        compose.onNode(hasSetTextAction()).performTextReplacement("https://example.com")
        compose.onNodeWithText("保存").performClick()
        compose.runOnIdle { assertEquals("https://example.com", model.upload.value.serverUrl) }
    }

    @Test @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun longQueueKeepsPrimaryActionVisibleBeforeAndAfterScrolling() {
        addDocuments(20)
        compose.setContent {
            val view = LocalView.current
            SideEffect { root = view }
            ImgBedApp(model)
        }
        compose.onNodeWithText("先设置服务连接").assertIsDisplayed()
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(15)
        compose.onNodeWithText("先设置服务连接").assertIsDisplayed().performClick()
        compose.onNodeWithContentDescription("返回").performClick()
        compose.onNodeWithText("先设置服务连接").assertIsDisplayed()
        saveScreenshot("upload-queue-light")
    }

    private fun addDocuments(count: Int) {
        val uris = (1..count).map { index ->
            val file = compose.activity.cacheDir.resolve("photo-$index.jpg")
            file.writeBytes(byteArrayOf(1, 2, 3))
            Uri.fromFile(file)
        }
        model.addUris(uris)
        assertEquals(count, model.documents.value.size)
    }

    private fun saveScreenshot(name: String) {
        compose.waitForIdle()
        compose.runOnIdle {
            assertTrue(root.width > 0 && root.height > 0)
            val bitmap = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
            root.draw(Canvas(bitmap))
            val file = File("build/reports/ui-screenshots/$name.png")
            file.parentFile?.mkdirs()
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
    }
}
