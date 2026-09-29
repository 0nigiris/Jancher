package io.jancher.launcher.updater

/**
 * Решение «предлагать ли это обновление» — отдельно от Android.
 *
 * Именно здесь легко ошибиться так, что ошибку не заметишь месяцами:
 * предложить установить старую версию, показывать плашку после отказа,
 * или предложить обновление, которое невозможно поставить поверх текущей.
 */
internal object UpdateDecision {

    fun shouldOffer(
        manifest: UpdateManifest,
        currentVersionCode: Int,
        dismissedVersionCode: Int,
    ): Boolean {
        if (manifest.versionCode <= currentVersionCode) return false
        // Отказ распространяется и на более старые версии: если отказались
        // от 5, предлагать 4 бессмысленно.
        if (dismissedVersionCode >= manifest.versionCode) return false
        // Слишком старая установленная версия: обновиться напрямую нельзя,
        // и молчание тут лучше, чем предложение, которое не сработает.
        if (currentVersionCode < manifest.minVersionCode) return false
        return manifest.apkUrl.startsWith("https://") && manifest.sha256.length == SHA256_HEX_LENGTH
    }

    private const val SHA256_HEX_LENGTH = 64
}
