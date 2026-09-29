package io.jancher.launcher.updater

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateDecisionTest {

    private val validHash = "a".repeat(64)

    private fun manifest(
        versionCode: Int = 5,
        url: String = "https://example.com/app.apk",
        sha: String = validHash,
        minVersionCode: Int = 1,
    ) = UpdateManifest(
        versionCode = versionCode,
        versionName = "0.$versionCode.0",
        apkUrl = url,
        sha256 = sha,
        minVersionCode = minVersionCode,
    )

    @Test
    fun `новая версия предлагается`() {
        assertTrue(UpdateDecision.shouldOffer(manifest(), currentVersionCode = 4, dismissedVersionCode = 0))
    }

    @Test
    fun `та же версия не предлагается`() {
        assertFalse(UpdateDecision.shouldOffer(manifest(5), currentVersionCode = 5, dismissedVersionCode = 0))
    }

    @Test
    fun `откат на старую версию не предлагается`() {
        assertFalse(UpdateDecision.shouldOffer(manifest(3), currentVersionCode = 5, dismissedVersionCode = 0))
    }

    @Test
    fun `после отказа та же версия больше не предлагается`() {
        assertFalse(UpdateDecision.shouldOffer(manifest(5), currentVersionCode = 4, dismissedVersionCode = 5))
    }

    @Test
    fun `отказ от новой версии не скрывает следующую`() {
        assertTrue(UpdateDecision.shouldOffer(manifest(6), currentVersionCode = 4, dismissedVersionCode = 5))
    }

    @Test
    fun `обновление без https игнорируется`() {
        assertFalse(
            UpdateDecision.shouldOffer(
                manifest(url = "http://example.com/app.apk"),
                currentVersionCode = 4,
                dismissedVersionCode = 0,
            ),
        )
    }

    @Test
    fun `обновление без контрольной суммы игнорируется`() {
        assertFalse(
            UpdateDecision.shouldOffer(manifest(sha = ""), currentVersionCode = 4, dismissedVersionCode = 0),
        )
    }

    @Test
    fun `слишком старая версия не получает предложения`() {
        assertFalse(
            UpdateDecision.shouldOffer(
                manifest(versionCode = 10, minVersionCode = 8),
                currentVersionCode = 5,
                dismissedVersionCode = 0,
            ),
        )
    }
}
