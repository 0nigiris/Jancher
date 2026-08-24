# Jancher — Android API и ограничения платформы

Раздел, который дешевле прочитать сейчас, чем узнать через месяц.

## 1. Что нужно, чтобы стать лаунчером

```xml
<activity android:name=".HomeActivity"
    android:launchMode="singleTask"
    android:stateNotNeeded="true"
    android:excludeFromRecents="true"
    android:screenOrientation="nosensor"
    android:configChanges="keyboard|keyboardHidden|navigation|orientation|screenSize|screenLayout|uiMode|density|smallestScreenSize"
    android:windowSoftInputMode="adjustNothing"
    android:enableOnBackInvokedCallback="true"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.HOME" />
        <category android:name="android.intent.category.DEFAULT" />
    </intent-filter>
</activity>
```

- `stateNotNeeded` — система не обязана сохранять состояние: при нехватке памяти лаунчер убивается и должен уметь подняться с нуля.
- `singleTask` + `onNewIntent` — нажатие Home не создаёт новую активити, а даёт нам событие «вернись в исходное состояние».
- `excludeFromRecents` — лаунчера не должно быть в списке недавних.

Смена лаунчера по умолчанию: своего API нет. Открываем `Settings.ACTION_HOME_SETTINGS` (или `RoleManager.ROLE_HOME` на 29+, что даёт системный диалог) и просим пользователя выбрать.

## 2. Основные API

| Задача | API | Примечания |
|---|---|---|
| Список приложений | `LauncherApps.getActivityList(null, user)` | Возвращает `LauncherActivityInfo` — именно активити, не пакеты |
| Реакция на установку/удаление | `LauncherApps.registerCallback` | Единственный правильный путь. Broadcast-ы `PACKAGE_ADDED` — устаревшая и неполная альтернатива |
| Запуск | `LauncherApps.startMainActivity(component, user, sourceBounds, opts)` | `sourceBounds` даёт системе точку, из которой рисовать анимацию раскрытия |
| Рабочие профили | `UserManager.getUserProfiles()` | Иконки бейджатся через `PackageManager.getUserBadgedIcon` |
| Шорткаты приложений | `LauncherApps.getShortcuts(query, user)` | **Работает только у лаунчера по умолчанию.** Иначе — `SecurityException` |
| Закрепление шортката | `LauncherApps.PinItemRequest` | Обработка `ACTION_CONFIRM_PIN_SHORTCUT` |
| Виджеты | `AppWidgetHost` + `AppWidgetManager` | Подробности ниже |
| Обои | `WallpaperManager` | `setWallpaperOffsets` для параллакса |
| Частота использования | `UsageStatsManager` | Разрешение `PACKAGE_USAGE_STATS`, выдаётся вручную в настройках |
| Уведомления/бейджи | `NotificationListenerService` | Разрешение выдаётся вручную; отдельная декларация в Play |
| Удаление приложения | `Intent.ACTION_DELETE` | Молча удалить нельзя, всегда системный диалог |
| Инфо о приложении | `LauncherApps.startAppDetailsActivity` | |

### Видимость пакетов — важный нюанс

`QUERY_ALL_PACKAGES` **не нужен**. Это опасное разрешение, из-за которого приложения заворачивают в Play. `LauncherApps` не подпадает под ограничения видимости пакетов, а для точечных запросов через `PackageManager` достаточно:

```xml
<queries>
    <intent>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent>
</queries>
```

## 3. Ограничения, которые нельзя обойти

Их надо знать до того, как они станут «багами».

**Поиск не видит содержимое чужих приложений.** Аналога iOS Core Spotlight в Android нет. Файлы, письма, заметки, треки внутри сторонних приложений проиндексировать невозможно — приложение обязано само их отдать, а почти никто этого не делает. Реально достижимо для «как Spotlight»: приложения, их шорткаты, контакты, экраны настроек, вычисления/конвертация, веб-запрос в браузер, история собственных запусков. Это будет честно отражено в UI.

**Скрытие приложения — не защита.** Приложение остаётся в «Настройки → Приложения», в шторке, в недавних, в системном поиске. Лаунчер прячет только себя.

**Наш «архив» и системное архивирование — разные вещи.** С Android 15 (API 35) есть `PackageInstaller.requestArchive`: система удаляет APK, оставляя иконку и данные, и освобождает место. Это можно предложить дополнительно, но это другой сценарий.

**Виджеты рисуются не нами.** Виджет — это `RemoteViews` чужого процесса. Мы не можем менять его шрифты, цвета, скругления, не можем анимировать содержимое. Что мы контролируем: размер, отступы, фон под ним. Дополнительно:
- `AppWidgetHost.startListening()` / `stopListening()` строго в `onStart`/`onStop`, иначе виджеты либо не обновляются, либо жгут батарею.
- Часть виджетов требует разрешения при привязке: `bindAppWidgetIdIfAllowed` → при отказе `ACTION_APPWIDGET_BIND`.
- Часть требует экрана конфигурации при добавлении (`configure`-активити).
- Размер сообщается виджету через `updateAppWidgetOptions`; виджет может его проигнорировать.
- Хостинг `AppWidgetHostView` внутри Compose — через `AndroidView`; это единственное место в проекте, где будет View-система.

**Бейджи уведомлений.** Точных счётчиков («7 непрочитанных») Android не даёт: приложения публикуют их по-разному, а `Notification.number` почти никто не заполняет. Реально доступно: количество активных уведомлений от пакета и текст последнего. `NotificationListenerService` умирает при обновлении приложения и требует переподключения (`requestRebind`). Плюс это чувствительное разрешение: Play требует отдельной декларации, а сама фича должна быть **опциональной и выключенной по умолчанию**.

**Жесты у краёв.** Система забирает полосы у левого и правого края под «назад». Отвоевать можно через `setSystemGestureExclusionRects`, но суммарно не больше ~200dp по высоте на край, и система вправе игнорировать. Мы это не используем: рельс отступает от края (см. 01-ARCHITECTURE §0.5).

**Анимация запуска приложения.** Полноценный морфинг иконки в окно (как у системного лаунчера Pixel) сторонним лаунчерам недоступен: он живёт в SystemUI/quickstep. Доступно: `ActivityOptions.makeClipRevealAnimation` / `makeScaleUpAnimation` от прямоугольника иконки, плюс `sourceBounds`. Выглядит хорошо, но это не то же самое.

**Экран недавних приложений** заменить нельзя вообще — это привилегия системного лаунчера.

**Блокировка экрана двойным тапом** требует либо Device Admin (грубо, пугает пользователя), либо Accessibility Service (Play придирается к обоснованию). Ставим поздно и как явную опцию с объяснением.

**One UI отдельно.** Samsung не даёт сторонним лаунчерам панель слева (Google Discover), Edge-панели работают поверх, а на некоторых прошивках агрессивный менеджер батареи может убивать процесс лаунчера — стоит проверить в реальной эксплуатации и, если понадобится, попросить исключение из оптимизации.

**Иконпаки** — не стандарт Android, а сложившееся соглашение: сторонний APK с ресурсом `appfilter.xml`, где `ComponentName` → имя drawable. Читаем через `PackageManager.getResourcesForApplication`. Совместимость никогда не бывает стопроцентной; поддерживаем распространённый формат (Nova/ADW).

## 4. Требования Google Play

| Что | Как закрываем |
|---|---|
| Data Safety | «Данные не собираются, не передаются» — правда, у нас нет сети |
| Разрешения | Ни одного опасного по умолчанию. Контакты, Usage Access, Notification Listener — запрашиваются только при включении соответствующей фичи |
| Декларация Notification Listener | Обязательна; обоснование — отображение бейджей на иконках, фича опциональна |
| targetSdk | Последний обязательный (36) |
| Политика конфиденциальности | Нужна ссылка даже при нулевом сборе данных — короткая страница на GitHub Pages |
| Подпись | Play App Signing; ключ загрузки хранится вне репозитория |
| GPL в Play | Допустимо. Требование GPL — предоставить исходники, а не запретить дистрибуцию. Ссылка на репозиторий в описании |
