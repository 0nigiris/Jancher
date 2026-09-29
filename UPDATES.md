# Как выпускается обновление

Приложение (сборка `direct`) при открытии лаунчера читает `update.json`
из ветки `master` этого репозитория и предлагает обновиться, если
`versionCode` в манифесте больше установленного.

Порядок выпуска:

1. Поднять `versionCode` и `versionName` в `app/build.gradle.kts`
2. `./gradlew :app:assembleDirectRelease`
3. `sha256sum app/build/outputs/apk/direct/release/app-direct-release.apk`
4. `gh release create vX.Y.Z <apk> --title ... --notes ...`
5. Обновить `update.json`: `versionCode`, `versionName`, `apkUrl`, `sha256`,
   `sizeBytes`, `notes` — и запушить в `master`

Почему манифест лежит в ветке, а не в релизе: его можно поправить, не
перевыкладывая APK — например, если нужно срочно отозвать неудачную версию,
вернув в манифест предыдущую.

`minVersionCode` — страховка на случай несовместимого изменения формата
конфигурации: слишком старым версиям обновление предлагаться не будет.

Подпись APK обязана совпадать: Android откажет в установке обновления,
подписанного другим ключом. Ключ лежит вне репозитория (`~/.jancher/`).
