# TvLauncher — план реализации

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Лёгкий главный экран для TCL Google TV: часы, дата, погода в Кудрово, курсы ЦБ, кнопка HDMI 1 и настраиваемая сетка приложений в стиле «Тёмный минимализм».

**Architecture:** Одно Android-приложение (один модуль `app`). Вся логика, которую можно проверить без устройства (сетка, разбор ответов, форматирование, правила обновления, выбор входа), живёт в чистых Kotlin-объектах и покрыта JUnit-тестами на JVM. Тонкий слой Android (Activity, RecyclerView, PackageManager, сеть, SharedPreferences) проверяется на телевизоре через adb: скриншоты, нажатия клавиш, замер памяти. Фоновой работы нет: данные обновляются только в момент включения телевизора и пока главный экран на виду.

**Tech Stack:** Kotlin (встроенный в AGP 9.3.1), Gradle 9.5.0 wrapper, compileSdk 37, minSdk 30, классические View + `androidx.recyclerview:recyclerview:1.4.0`, `HttpURLConnection`, `org.json`, `java.time`. Тесты: JUnit 4.13.2, `org.json:json:20180813` (только для JVM-тестов).

**Spec:** `docs/superpowers/specs/2026-10-22-tv-launcher-design.md`, эталон вида — `.superpowers/brainstorm/81834-1792683682/content/home-v3.html`.

Сокращения путей: `SRC` = `app/src/main/java/com/example/tvlauncher`, `RES` = `app/src/main/res`, `TEST` = `app/src/test/java/com/example/tvlauncher`.

## Global Constraints

- Пакет и applicationId: `com.example.tvlauncher` (как в других проектах пользователя). Главная activity: `com.example.tvlauncher/.ui.HomeActivity`.
- `minSdk = 30`, `targetSdk = 37`, `compileSdk { version = release(37) { minorApiLevel = 0 } }`, Java/Kotlin target 17.
- Единственная runtime-зависимость: `androidx.recyclerview:recyclerview:1.4.0`. Без Compose, Leanback, AppCompat, OkHttp/Retrofit, WorkManager, сервисов и будильников.
- Устройство: Android TV 11 (API 30), `armeabi-v7a`, 1920×1080 при density 320 → экран 960×540 dp. Управление только пультом.
- ADB: `TV=192.168.31.103:5555`, все adb-команды с `-s $TV`.
- Весь текст интерфейса на русском.
- Шрифт: системный `sans-serif` / `sans-serif-medium` (Roboto). Inter не подключаем — решение по пункту 9 spec, чтобы не тянуть шрифт в APK.
- Цвета (spec §6): фон `#09090B`, свечение `#3D7C7CFF` (rgba(124,124,255,.24)) сверху по центру; панели и плитки `#131316`, рамка 1dp `#13FFFFFF`; текст `#EDEDEF`; вторичный `#8B8D98`; подписи разделов `#6B6D78`; акцент `#8B8BFF`; рост `#3DDC84`; падение `#FF6B6B`.
- Размеры из наброска (1 % ширины наброска = 9.6 dp): поля экрана 35dp сверху и снизу и 42dp по бокам; промежутки между блоками 17dp; часы 81sp `sans-serif-medium`, letterSpacing −0.055; дата 15sp; карточки — радиус 12dp, отступы 11×15dp; подписи разделов 10sp, заглавные, letterSpacing 0.1; кнопка HDMI 12.5sp, радиус 8dp, отступы 8×18dp, точка 6dp; сетка 6 колонок, промежуток 12dp, плитка 16:9, радиус 10dp, иконка 31dp в квадрате с радиусом 9dp, подпись 10sp `#A1A3AD` (у плитки в фокусе — белая).
- Фокус: масштаб 1.1 за 150 мс, кольцо 2dp `#8B8BFF` и мягкое свечение того же цвета.
- Сеть только к `api.open-meteo.com` и `open.er-api.com` (изменено в Task 9: см. spec §7.5). Таймауты 10 с, заголовок `User-Agent: TvLauncher/1.0 (Android TV)`.
- Ключ служебной плитки «Настройки»: `"@settings"`.
- `.superpowers/`, `build/`, `.gradle/`, `local.properties`, `.idea/` — в `.gitignore`.

## Review Focus

1. **Удалённое или отключённое приложение, которое стоит на главном экране.** Ожидается: плитка молча исчезает. Если приложение потом поставить снова, оно появляется в конце с меткой «новое». Тест: `uninstalledAppDisappearsAndReinstallIsFresh` в Task 2.
2. **Перемещение у края сетки** (влево с первой плитки, вниз с последнего ряда, попытка заехать на «Все»/«+»). Ожидается: плитка упирается в край и остаётся среди приложений. Тест: `moveClampsTargetIntoRange` в Task 2, плюс ограничение индексов в Task 6.
3. **Температура около нуля** (−0.4, −0.5, +0.4). Ожидается «0°», а не «−0°» или «+0°». Тест: `temperatureNearZeroHasNoSign` в Task 8.
4. **Курс почти не изменился** (|Δ| < 0.005). Ожидается «0,00» без стрелки, нейтральным цветом, а не «▲ 0,00». Тест: `tinyDeltaIsFlat` в Task 9.
5. **Сервер ответил не тем** (HTML-заглушка, пустой JSON, в XML нет одной из валют). Ожидается: разбор возвращает `null`, остаются сохранённые данные, ошибка нигде не показывается. Тесты: `parseRejectsGarbage` в Task 8 и `parseRejectsHtmlAndPartial` в Task 9.

---

### Task 1: Каркас проекта, установка на телевизор, назначение главным экраном

**Files:**
- Create: `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml`, `app/build.gradle.kts`, `.gitignore`, `local.properties` (не в git), Gradle wrapper
- Create: `app/src/main/AndroidManifest.xml`, `SRC/ui/HomeActivity.kt`, `RES/values/{colors,dimens,strings,themes}.xml`, `RES/drawable/bg_home.xml`, `RES/layout/activity_home.xml`
- Create: `tools/deploy.sh`, `tools/shot.sh`

**Interfaces:**
- Produces: `HomeActivity` (MAIN + HOME + DEFAULT), тема `Theme.TvLauncher`, цвета и размеры из Global Constraints как ресурсы (`@color/bg`, `@color/panel`, `@color/panel_stroke`, `@color/text`, `@color/text_muted`, `@color/label`, `@color/accent`, `@color/up`, `@color/down`, `@color/tile_label`), скрипты `tools/deploy.sh [debug|release]` и `tools/shot.sh NAME`.

- [ ] **Step 1: git и wrapper.** `git init -b main`, первый коммит с `docs/`. Затем ветка `git switch -c launcher-v1`. Wrapper: `gradle wrapper --gradle-version 9.5.0 --distribution-type bin`. `local.properties`: `sdk.dir=/Users/vadimkojlov/Library/Android/sdk`.
- [ ] **Step 2: Gradle-файлы** по образцу `~/StudioProjects/Spotifyyyyy` (AGP 9.3.1 со встроенным Kotlin, без плагина `kotlin-android`). Каталог версий: `agp = "9.3.1"`, `recyclerview = "1.4.0"`, `junit = "4.13.2"`, `orgJson = "20180813"`. В `app`: `testImplementation` junit и org.json. `gradle.properties`: `org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8`, `android.useAndroidX=true`, `kotlin.code.style=official`. Release собирается с `optimization { enable = true }` и подписывается debug-ключом (`signingConfig = signingConfigs.getByName("debug")`), чтобы ставиться поверх отладочной сборки.
- [ ] **Step 3: Манифест.** Разрешения `INTERNET`, `ACCESS_NETWORK_STATE`. `uses-feature` `android.software.leanback` и `android.hardware.touchscreen` с `required=false`. Блок `<queries>` с intent'ами `MAIN`+`LEANBACK_LAUNCHER` и `MAIN`+`LAUNCHER`. `HomeActivity`: `launchMode="singleTask"`, `stateNotNeeded="true"`, `clearTaskOnLaunch="true"`, `excludeFromRecents="true"`, `screenOrientation="landscape"`, фильтр `MAIN` + `HOME` + `DEFAULT`, `exported="true"`. Тема: наследник `@android:style/Theme.DeviceDefault.NoActionBar.Fullscreen`, `windowBackground=@drawable/bg_home`, `android:defaultFocusHighlightEnabled=false`.
- [ ] **Step 4: `bg_home.xml`** — layer-list: заливка `#09090B` + радиальный градиент `#3D7C7CFF` → прозрачный, центр по горизонтали в 50 %, по вертикали около −8 %, радиус около 70 % ширины. `activity_home.xml` пока содержит только часы-заглушку `18:41` с параметрами из Global Constraints.
- [ ] **Step 5: `tools/deploy.sh`** (bash 3.2, `set -euo pipefail`, `TV` по умолчанию `192.168.31.103:5555`, аргумент `debug|release`, по умолчанию debug). Скрипт делает `adb connect`, `./gradlew :app:assembleDebug` или `:app:assembleRelease`, `adb -s $TV install -r <apk>`, `adb -s $TV shell cmd package set-home-activity com.example.tvlauncher/.ui.HomeActivity`, `adb -s $TV shell input keyevent KEYCODE_HOME`. `tools/shot.sh NAME` делает `adb -s $TV exec-out screencap -p > build/screens/NAME.png` (каталог создаётся).
- [ ] **Step 6: Установить и проверить.**
  Run: `tools/deploy.sh && adb -s 192.168.31.103:5555 shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME | tail -1`
  Expected: `com.example.tvlauncher/.ui.HomeActivity`. Затем `tools/shot.sh t1-home`: на скриншоте тёмный фон со свечением и «18:41».
- [ ] **Step 7: Commit** — `git add -A && git commit -m "feat: каркас лаунчера, установка как HOME"`.

### Task 2: Логика сетки главного экрана

**Files:**
- Create: `SRC/grid/GridState.kt`, `SRC/grid/HomeGrid.kt`, `SRC/grid/GridCodec.kt`, `SRC/grid/GridStore.kt`
- Test: `TEST/grid/HomeGridTest.kt`, `TEST/grid/GridCodecTest.kt`

**Interfaces:**
- Produces:
  - `data class GridState(val order: List<String>, val known: Set<String>, val fresh: Set<String>, val initialized: Boolean)` с `companion val EMPTY = GridState(emptyList(), emptySet(), emptySet(), false)`. `order` — ключи на главном экране по порядку, `known` — все ключи, которые лаунчер уже видел установленными, `fresh` — ключи с меткой «новое».
  - `object HomeGrid { const val SETTINGS_KEY = "@settings"; val DEFAULT_ORDER: List<String>; fun reconcile(state: GridState, installed: Set<String>): GridState; fun move(state: GridState, from: Int, to: Int): GridState; fun remove(state: GridState, key: String): GridState; fun add(state: GridState, key: String): GridState; fun clearFresh(state: GridState, key: String): GridState }`
  - `object GridCodec { fun encode(s: GridState): Map<String, String>; fun decode(m: Map<String, String>): GridState }`
  - `class GridStore(prefs: SharedPreferences) { fun load(): GridState; fun save(s: GridState) }` — prefs-файл `"grid"`, хранит строки из `GridCodec`.

`DEFAULT_ORDER` (spec §7.1): `com.google.android.youtube.tv`, `ru.kinopoisk.tv`, `to.pda.falcofemoralis.hdrezkaapp`, `ru.more.play`, `tv.twitch.android.app`, `org.videolan.vlc`, `com.internet.tvbrowser`, `com.alphainventor.filemanager`, `com.pigeoncast.screen.mirroring.android.tv`, `"@settings"`.

Правила `reconcile`. Первый запуск (`!initialized`): `order = DEFAULT_ORDER ∩ installed` в порядке `DEFAULT_ORDER`, `known = installed`, `fresh = ∅`. Дальше: `gone = known − installed` убирается из `order`, `fresh` и `known`; `added = installed − known` (отсортированные по ключу) дописываются в конец `order`, добавляются в `fresh` и `known`. Приложение, которое пользователь убрал с главного, остаётся в `known` и само не возвращается. `move` ограничивает `to` диапазоном `0..order.lastIndex`. `remove` убирает ключ из `order` и `fresh`. `add` дописывает ключ в конец, если его там нет.

- [ ] **Step 1: Write the failing tests** — `HomeGridTest`:
  - `firstRunUsesDefaultOrderFilteredByInstalled`: installed = {youtube, kinopoisk, vlc, `rtb.mobile.android`, `@settings`} → `order == [youtube, kinopoisk, vlc, @settings]`, `known == installed`, `fresh` пустой, `initialized`.
  - `appInstalledLaterIsAppendedAndFresh`: после первого запуска reconcile с тем же installed + `com.new.app` → `com.new.app` последний в `order` и есть в `fresh`. `rtb.mobile.android` (известен с первого запуска) в `order` не появляется.
  - `removedFromHomeDoesNotComeBack`: `remove(youtube)`, затем reconcile с тем же installed → youtube нет в `order`.
  - `uninstalledAppDisappearsAndReinstallIsFresh`: убрать vlc из installed → нет в `order` и `known`. Вернуть → последний в `order` и в `fresh`.
  - `moveRelocatesKey`: `[a,b,c,d]`, `move(0, 2)` → `[b,c,a,d]`.
  - `moveClampsTargetIntoRange`: `[a,b,c]`, `move(0, 99)` → `[b,c,a]`; `move(2, -5)` → `[c,a,b]`.
  - `addAppendsOnceAndRemoveDropsFresh`: `add` два раза → ключ один раз в конце. `remove` свежего ключа → его нет в `fresh`.
  - `clearFreshRemovesBadgeOnly`.
  `GridCodecTest`: `roundTripKeepsEverything` (непустое состояние), `roundTripOfEmptyListsIsEmpty` (`decode(encode(EMPTY.copy(initialized = true))).order` пустой, а не `[""]`), `decodeOfEmptyMapIsEmptyState`.
- [ ] **Step 2: Run** `./gradlew :app:testDebugUnitTest --tests 'com.example.tvlauncher.grid.*'` — Expected: FAIL (классов нет).
- [ ] **Step 3: Implement** `GridState`, `HomeGrid`, `GridCodec` (списки и множества через `"\n"`-join, пустая строка → пустой список; `initialized` — `"1"`/`"0"`), `GridStore`.
- [ ] **Step 4: Run** ту же команду — Expected: PASS.
- [ ] **Step 5: Commit** — `feat: логика сетки и её сохранение`.

### Task 3: Список установленных приложений, подписи, сортировка

**Files:**
- Create: `SRC/apps/Labels.kt`, `SRC/apps/AppSorting.kt`, `SRC/apps/RuPlural.kt`, `SRC/apps/InstalledApps.kt`
- Test: `TEST/apps/LabelsTest.kt`, `TEST/apps/AppSortingTest.kt`, `TEST/apps/RuPluralTest.kt`

**Interfaces:**
- Consumes: `HomeGrid.SETTINGS_KEY`.
- Produces:
  - `object Labels { fun display(packageName: String, systemLabel: String): String }`
  - `object AppSorting { fun <T> byLabel(items: List<T>, label: (T) -> String): List<T> }` — `Collator.getInstance(Locale("ru"))`, strength `SECONDARY`.
  - `object RuPlural { fun apps(n: Int): String }`
  - `data class LaunchableApp(val key: String, val label: String, val launch: Intent)`
  - `class InstalledApps(context: Context) { fun query(): Map<String, LaunchableApp>; fun icon(key: String): Drawable }`

`Labels.display`: обрезает пробелы. Сокращения по пакету: `com.internet.tvbrowser` → «Браузер», `com.alphainventor.filemanager` → «Файлы», `to.pda.falcofemoralis.hdrezkaapp` → «HDrezka». Пустое название → имя пакета. `InstalledApps.query()`: activity с `MAIN`+`LEANBACK_LAUNCHER`, затем `MAIN`+`LAUNCHER` (по одной записи на пакет, leanback важнее), без собственного пакета. Плюс запись `"@settings"` с подписью «Настройки» и intent `Settings.ACTION_SETTINGS`. Запуск: `getLeanbackLaunchIntentForPackage` ?: `getLaunchIntentForPackage`, флаг `NEW_TASK`. `icon()` кеширует `Drawable` в `HashMap` на время жизни объекта. Иконка настроек — `getApplicationIcon("com.android.tv.settings")`.

- [ ] **Step 1: Write the failing tests:**
  - `LabelsTest`: `("com.internet.tvbrowser","BROWSER")` → «Браузер»; `("com.alphainventor.filemanager","Файловый менеджер +")` → «Файлы»; `("to.pda.falcofemoralis.hdrezkaapp","HDrezka Mod")` → «HDrezka»; `("ru.more.play","Okko")` → «Okko»; `("x.y"," VLC ")` → «VLC»; `("a.b","")` → «a.b».
  - `AppSortingTest`: `["Настройки","браузер","Кинопоиск"]` → `["браузер","Кинопоиск","Настройки"]`; `["youtube","VLC","Okko"]` → `["Okko","VLC","youtube"]`.
  - `RuPluralTest`: 1 → «1 приложение», 2 → «2 приложения», 5 → «5 приложений», 11 → «11 приложений», 12 → «12 приложений», 19 → «19 приложений», 21 → «21 приложение», 22 → «22 приложения», 111 → «111 приложений».
- [ ] **Step 2: Run** `./gradlew :app:testDebugUnitTest --tests 'com.example.tvlauncher.apps.*'` — Expected: FAIL.
- [ ] **Step 3: Implement** четыре файла по Interfaces.
- [ ] **Step 4: Run** — Expected: PASS.
- [ ] **Step 5: Commit** — `feat: список приложений, подписи и сортировка`.

### Task 4: Главный экран — шапка с часами, кнопка HDMI, сетка с фокусом и запуском

**Files:**
- Create: `SRC/info/Format.kt` (пока `time` и `date`), `SRC/ui/TileAdapter.kt`, `SRC/ui/FocusAnim.kt`, `RES/layout/item_tile.xml`, `RES/drawable/{bg_panel,bg_tile,bg_pill,bg_new_badge,dot_green}.xml`
- Modify: `SRC/ui/HomeActivity.kt`, `RES/layout/activity_home.xml`
- Test: `TEST/info/FormatTest.kt`

**Interfaces:**
- Consumes: `GridStore`, `HomeGrid`, `InstalledApps`, `Labels`.
- Produces:
  - `object Format { fun time(t: LocalTime): String; fun date(d: LocalDate): String }` (в Tasks 8–9 сюда добавляются `temperature`, `rate`, `delta`).
  - `sealed interface Tile { data class App(val key: String, val label: String, val fresh: Boolean) : Tile; object All : Tile; object Add : Tile }`
  - `class TileAdapter(private val apps: InstalledApps, private val onClick: (Tile) -> Unit, private val onLongClick: (Tile, View) -> Unit) : RecyclerView.Adapter<…> { fun submit(tiles: List<Tile>); var movingKey: String? }`
  - В разметке `activity_home.xml` есть id: `clock`, `date`, `offline_icon` (пока `gone`), `weather_card`, `rates_card` (пока `invisible`, заполняются в Tasks 8–9), `hdmi_button`, `grid`, `move_hint` (`gone`).

Сетка: `GridLayoutManager` на 6 колонок, у `RecyclerView` `clipChildren=false`, `clipToPadding=false` и внутренний отступ 10dp, чтобы увеличенная плитка не обрезалась. Плитки: по `App` на каждый ключ из `state.order`, который есть в `query()`; ключи, которых в `query()` нет, пропускаются. В конце `All` («⋯», «Все») и `Add` (пунктирная рамка, «+»). У `fresh`-плитки метка «новое» (8sp, `#8B8BFF` на `#298B8BFF`, справа сверху). Шапка не прокручивается. Часы обновляются по `ACTION_TIME_TICK` (receiver регистрируется в `onStart`, снимается в `onStop`). В `onResume`: `reconcile` с `query().keys`, `save`, `submit`. Фокус при открытии и в `onNewIntent` — на первой плитке, сетка прокручена в начало. «Назад» на главном ничего не делает. Нажатие `App` запускает приложение и вызывает `clearFresh`. Кнопка HDMI и плитки `All`/`Add` в этой задаче ничего не делают: их подключают Tasks 5 и 7. Вверх с первого ряда — на кнопку HDMI, вниз с кнопки — на первую плитку.

- [ ] **Step 1: Write the failing test** — `FormatTest.timeIsTwoDigit24h`: `time(LocalTime.of(9,5)) == "09:05"`, `time(LocalTime.of(18,41)) == "18:41"`; `dateIsCapitalizedRussianGenitive`: `date(LocalDate.of(2026,10,22)) == "Четверг, 22 октября"`, `date(LocalDate.of(2026,3,1)) == "Воскресенье, 1 марта"`.
- [ ] **Step 2: Run** `./gradlew :app:testDebugUnitTest --tests 'com.example.tvlauncher.info.FormatTest'` — Expected: FAIL.
- [ ] **Step 3: Implement** `Format.time/date` (шаблон `EEEE, d MMMM`, `Locale("ru")`, первая буква заглавная), затем разметку, drawables, `TileAdapter`, `FocusAnim` (масштаб 1.1 за 150 мс + кольцо из selector'а `bg_tile` для `state_focused`) и `HomeActivity`.
- [ ] **Step 4: Run** unit-тест — Expected: PASS. Затем `tools/deploy.sh && tools/shot.sh t4-home`. Expected на скриншоте: часы, дата, кнопка HDMI 1 с зелёной точкой, 10 плиток (YouTube… Настройки) + «Все» + «+», первая плитка в фокусе с кольцом. `adb -s $TV shell input keyevent KEYCODE_DPAD_RIGHT` и `tools/shot.sh t4-focus` → фокус на «Кинопоиск». `KEYCODE_DPAD_CENTER` → открылся Кинопоиск; `KEYCODE_HOME` → вернулись на главный экран, фокус на первой плитке.
- [ ] **Step 5: Commit** — `feat: главный экран с часами и сеткой`.

### Task 5: Экран «Все приложения»

**Files:**
- Create: `SRC/ui/AllAppsActivity.kt`, `RES/layout/activity_all_apps.xml`, `RES/drawable/dot_home.xml`, `SRC/ui/TileMenu.kt`
- Modify: `app/src/main/AndroidManifest.xml`, `SRC/ui/HomeActivity.kt` (плитки `All` и `Add` открывают `AllAppsActivity`), `SRC/ui/TileAdapter.kt` (поддержка точки «на главном»)

**Interfaces:**
- Consumes: `InstalledApps`, `AppSorting.byLabel`, `RuPlural.apps`, `GridStore`, `HomeGrid.add/remove`.
- Produces: `class TileMenu(anchor: View, items: List<Pair<String, () -> Unit>>) { fun show() }` — `PopupWindow` с `focusable=true`, тёмная панель (`bg_panel`). У пунктов фокус выделяется так же, как в наброске (заливка `#2E7C7CFF` и кольцо `#8B8BFF`). Позиция — под плиткой, а если внизу нет места — над ней.

`AllAppsActivity` (не exported, тема та же): заголовок «Все приложения» (29sp `sans-serif-medium`) и справа `RuPlural.apps(n)`. Сетка на 6 колонок из всех `query()`, отсортированных по подписи; у тех, что есть в `order`, точка `#8B8BFF` справа сверху. Внизу подсказка: «Точка — уже на главном экране · Долгое нажатие OK — добавить или убрать». OK запускает приложение. Долгое OK открывает `TileMenu` с одним пунктом: «Добавить на главный экран» или «Убрать с главного экрана». Изменение сохраняется сразу.

- [ ] **Step 1: Implement** по Interfaces (новых чистых функций нет, логика уже покрыта Tasks 2–3).
- [ ] **Step 2: Проверка на устройстве.** `tools/deploy.sh`, фокус на «Все» (`DPAD_DOWN` ×1, `DPAD_RIGHT` ×4 от первой плитки), `DPAD_CENTER`, `tools/shot.sh t5-all`. Expected: заголовок, «N приложений» с правильным окончанием, алфавитный порядок, точки у 10 приложений. На плитке RUTUBE: `adb -s $TV shell input keyevent --longpress KEYCODE_DPAD_CENTER` → меню «Добавить на главный экран» → `DPAD_CENTER`, затем `BACK`, затем `tools/shot.sh t5-added`. Expected: RUTUBE в конце сетки, без метки «новое».
- [ ] **Step 3: Commit** — `feat: экран «Все приложения»`.

### Task 6: Меню плитки и режим перемещения

**Files:**
- Modify: `SRC/ui/HomeActivity.kt`, `SRC/ui/TileAdapter.kt`, `RES/drawable/bg_tile.xml` (состояние «перемещается»), `RES/layout/activity_home.xml` (`move_hint`)

**Interfaces:**
- Consumes: `TileMenu`, `HomeGrid.move/remove/clearFresh`, `TileAdapter.movingKey`.

Долгое OK на `App`-плитке открывает `TileMenu`: «Переместить» и «Убрать с главного экрана». На `All` и `Add` меню нет. Режим перемещения: запоминается исходный `GridState`, `movingKey` выставляется. Плитка получает заливку `#2E7C7CFF` и кольцо, внизу экрана подсказка `move_hint`: «◀ ▶ ▲ ▼ — переместить · OK — готово · Назад — отмена». `HomeActivity.dispatchKeyEvent` перехватывает клавиши. LEFT/RIGHT дают ±1, UP/DOWN — ±6. Целевой индекс ограничен диапазоном `0..order.lastIndex`, на «Все» и «+» плитка не заезжает. Применяется `HomeGrid.move`, затем `notifyItemMoved` и `scrollToPosition`, фокус остаётся на перемещаемой плитке. OK — `clearFresh` и `save`. «Назад» восстанавливает исходное состояние. «Убрать с главного экрана» — `remove` и `save`, фокус на соседнюю плитку.

- [ ] **Step 1: Implement** по описанию.
- [ ] **Step 2: Проверка на устройстве.** На «Кинопоиск»: `input keyevent --longpress KEYCODE_DPAD_CENTER`, `tools/shot.sh t6-menu` → меню из двух пунктов. «Переместить», затем `DPAD_LEFT`, `tools/shot.sh t6-moving` → Кинопоиск первый, подсвечен, внизу подсказка. `DPAD_LEFT` ещё раз → остаётся первым (край). `DPAD_CENTER` для фиксации. `adb -s $TV shell run-as com.example.tvlauncher cat shared_prefs/grid.xml` → в `order` первым идёт `ru.kinopoisk.tv`. Повторить перемещение и нажать `BACK` → порядок не изменился. «Убрать с главного экрана» на VLC → VLC пропал, в «Все» нет точки. `adb -s $TV shell am force-stop com.example.tvlauncher && adb -s $TV shell input keyevent KEYCODE_HOME` → порядок сохранился.
- [ ] **Step 3: Commit** — `feat: перемещение и удаление плиток пультом`.

### Task 7: Переключение на HDMI 1

**Files:**
- Create: `SRC/hdmi/HdmiPicker.kt`, `SRC/hdmi/HdmiSwitcher.kt`
- Modify: `SRC/ui/HomeActivity.kt` (кнопка `hdmi_button`)
- Test: `TEST/hdmi/HdmiPickerTest.kt`

**Interfaces:**
- Produces:
  - `data class InputDesc(val id: String, val isHdmi: Boolean, val label: String?)`
  - `object HdmiPicker { const val FALLBACK_HDMI1 = "com.tcl.tvinput/.passthroughinput.TvPassThroughService/HW1413744128"; fun pickHdmi1(inputs: List<InputDesc>): String }`
  - `class HdmiSwitcher(context: Context) { fun switchToHdmi1(): Boolean }`

`pickHdmi1`: сначала HDMI-вход, у которого подпись без пробелов и без учёта регистра равна `hdmi1`. Иначе HDMI-вход с наименьшим числом из `HW(\d+)$` в конце id. Если HDMI-входов нет — `FALLBACK_HDMI1`. `HdmiSwitcher`: `TvInputManager.tvInputList` → `InputDesc(info.id, info.type == TvInputInfo.TYPE_HDMI, info.loadLabel(context)?.toString())`, затем `startActivity(Intent(ACTION_VIEW, TvContract.buildChannelUriForPassthroughInput(id)).addFlags(NEW_TASK))`. При `ActivityNotFoundException` или `SecurityException` — `false` и `Toast` «Не удалось переключиться на HDMI 1».

- [ ] **Step 1: Write the failing tests** (id из `dumpsys tv_input` этого телевизора):
  - `picksLowestHdmiWhenNoLabels`: AV `…/HW1413743104` (не HDMI), HDMI `…HW1413744640`, `…HW1413744128`, `…HW1413744384`, подписи `null` → `…HW1413744128`.
  - `prefersExplicitHdmi1Label`: те же входы, у `…HW1413744384` подпись «HDMI 1» → `…HW1413744384`.
  - `fallsBackWhenNoHdmi`: пустой список → `FALLBACK_HDMI1`.
- [ ] **Step 2: Run** `./gradlew :app:testDebugUnitTest --tests 'com.example.tvlauncher.hdmi.*'` — Expected: FAIL.
- [ ] **Step 3: Implement** по Interfaces.
- [ ] **Step 4: Run** — Expected: PASS. Затем `tools/deploy.sh`, фокус на HDMI 1 (`DPAD_UP`), `DPAD_CENTER`, и `adb -s $TV shell dumpsys activity activities | grep -m1 ResumedActivity` → `com.tcl.tv/.TVActivity`. Попросить пользователя подтвердить, что на экране картинка с устройства на HDMI 1. `KEYCODE_HOME` → снова лаунчер.
- [ ] **Step 5: Commit** — `feat: кнопка HDMI 1`.

### Task 8: Погода и механизм обновления

**Files:**
- Create: `SRC/info/Weather.kt` (`Weather`, `WeatherParser`, `WeatherCodes`, `WeatherIcon`), `SRC/info/RefreshPolicy.kt`, `SRC/info/Http.kt`, `SRC/info/InfoCache.kt`, `SRC/info/InfoRepository.kt`, `SRC/LauncherApp.kt`, `SRC/ui/InfoViews.kt`, `RES/drawable/ic_wx_{clear_day,clear_night,partly_day,partly_night,cloudy,fog,rain,snow,thunder}.xml`
- Modify: `SRC/info/Format.kt` (+`temperature`), `SRC/ui/HomeActivity.kt`, `RES/layout/activity_home.xml` (содержимое `weather_card`), манифест (`android:name=".LauncherApp"`)
- Test: `TEST/info/WeatherTest.kt`, `TEST/info/RefreshPolicyTest.kt`, `TEST/info/FormatTest.kt`

**Interfaces:**
- Produces:
  - `data class Weather(val tempC: Double, val code: Int, val isDay: Boolean)`; `object WeatherParser { const val URL = "https://api.open-meteo.com/v1/forecast?latitude=59.9006&longitude=30.5135&current=temperature_2m,weather_code,is_day&timezone=Europe%2FMoscow"; fun parse(json: String): Weather? }`
  - `enum class WeatherIcon { CLEAR_DAY, CLEAR_NIGHT, PARTLY_DAY, PARTLY_NIGHT, CLOUDY, FOG, RAIN, SNOW, THUNDER }`; `object WeatherCodes { fun describe(code: Int): String; fun icon(code: Int, isDay: Boolean): WeatherIcon }`
  - `Format.temperature(c: Double): String`
  - `enum class Trigger { POWER_ON, HOME_VISIBLE }`; `object RefreshPolicy { const val WEATHER_MIN_INTERVAL_MS = 1_800_000L; fun weatherDue(trigger: Trigger, lastSuccessMs: Long?, nowMs: Long): Boolean; fun ratesDue(trigger: Trigger, ratesPending: Boolean): Boolean }`
  - `object Http { fun get(url: String, charset: Charset = Charsets.UTF_8): String? }` — `null` при любой ошибке и при ответе не 200.
  - `class InfoCache(prefs: SharedPreferences)` — prefs-файл `"info"`, свойства `weather: Weather?` и `weatherAt: Long?`. Поля курсов добавляет Task 9.
  - `class InfoRepository(cache: InfoCache, now: () -> Long = System::currentTimeMillis) { var listener: (() -> Unit)?; fun onPowerOn(); fun onHomeVisible() }` — однопоточный `Executor`, `listener` вызывается на главном потоке, повторный запуск, пока прошлый запрос ещё идёт, не делается.
  - `class LauncherApp : Application { lateinit var info: InfoRepository }` — в `onCreate` создаёт репозиторий, вызывает `info.onPowerOn()` и регистрирует receiver `ACTION_SCREEN_ON` → `info.onPowerOn()`.
  - `object InfoViews { fun bindWeather(card: View, w: Weather?, dim: Boolean) }`

`describe` (строчными, для «Кудрово · …»): 0 «ясно», 1 «малооблачно», 2 «переменная облачность», 3 «пасмурно», 45/48 «туман», 51/53/55 «морось», 56/57 «ледяная морось», 61 «небольшой дождь», 63 «дождь», 65 «сильный дождь», 66/67 «ледяной дождь», 71 «небольшой снег», 73 «снег», 75 «сильный снег», 77 «снежная крупа», 80/81 «ливень», 82 «сильный ливень», 85/86 «снегопад», 95 «гроза», 96/99 «гроза с градом», иначе `""` (тогда показывается просто «Кудрово»). `icon`: 0 → CLEAR, 1–2 → PARTLY (день или ночь по `isDay`), 3 → CLOUDY, 45/48 → FOG, 51–67 и 80–82 → RAIN, 71–77 и 85–86 → SNOW, 95–99 → THUNDER, иначе CLOUDY. `temperature`: `roundToInt()`; 0 → «0°», больше нуля → «+N°», меньше — «−N°» (U+2212). `weatherDue`: `POWER_ON` → всегда; `HOME_VISIBLE` → `last == null || now − last ≥ 30 мин`. `ratesDue`: `POWER_ON` → всегда; `HOME_VISIBLE` → `ratesPending`. `HomeActivity`: `onStart` → `onHomeVisible()`, и на каждом `TIME_TICK` тоже `onHomeVisible()`. Карточка погоды: иконка 38dp, температура 27sp, подпись 11sp «Кудрово · {describe}»; если данных нет — «—».

- [ ] **Step 1: Write the failing tests:**
  - `WeatherTest.parsesOpenMeteoCurrent`: JSON, полученный 22.10.2026 (`{"latitude":59.897118,…,"current":{"time":"2026-10-22T19:00","interval":900,"temperature_2m":8.3,"apparent_temperature":5.3,"weather_code":1,"is_day":0}}`) → `Weather(8.3, 1, false)`.
  - `WeatherTest.parseRejectsGarbage`: `"{}"`, `"<html>Bad gateway</html>"`, `""` → `null`.
  - `WeatherTest.codesMapToRussianAndIcons`: `describe(3) == "пасмурно"`, `describe(1) == "малооблачно"`, `describe(1234) == ""`, `icon(0,false) == CLEAR_NIGHT`, `icon(2,true) == PARTLY_DAY`, `icon(63,true) == RAIN`, `icon(73,true) == SNOW`, `icon(96,true) == THUNDER`.
  - `FormatTest.temperatureHasSign`: `8.3` → «+8°», `−3.6` → «−4°», `0.5` → «+1°».
  - `FormatTest.temperatureNearZeroHasNoSign`: `−0.4`, `0.4`, `−0.5` → «0°».
  - `RefreshPolicyTest`: `weatherDue(POWER_ON, t, t)` true; `weatherDue(HOME_VISIBLE, null, t)` true; `weatherDue(HOME_VISIBLE, t − 29 мин, t)` false; `weatherDue(HOME_VISIBLE, t − 30 мин, t)` true; `ratesDue(POWER_ON, false)` true; `ratesDue(HOME_VISIBLE, false)` false; `ratesDue(HOME_VISIBLE, true)` true.
- [ ] **Step 2: Run** `./gradlew :app:testDebugUnitTest --tests 'com.example.tvlauncher.info.*'` — Expected: FAIL.
- [ ] **Step 3: Implement** чистую часть (`Weather*`, `RefreshPolicy`, `Format.temperature`) → Run → PASS.
- [ ] **Step 4: Implement** Android-часть: `Http`, `InfoCache`, `InfoRepository`, `LauncherApp`, `InfoViews.bindWeather`, иконки в стиле B (простые контурные или заливные вектора, солнце `#FFC94D`, облако `#D4D4D8`).
- [ ] **Step 5: Проверка на устройстве.** `tools/deploy.sh && tools/shot.sh t8-weather` → карточка «+N° Кудрово · …» с подходящей иконкой. Сравнить температуру с `curl -s "<URL>"` на Mac. `adb -s $TV shell input keyevent KEYCODE_SLEEP`, затем `KEYCODE_WAKEUP`, затем `adb -s $TV logcat -d -s TvLauncher:*`: в логе `weather fetch trigger=POWER_ON` (лог-тег `TvLauncher`, одна строка на каждый запрос).
- [ ] **Step 6: Commit** — `feat: погода в Кудрово`.

### Task 9: Курсы ЦБ

> Изменено при выполнении: cbr.ru недоступен с Android 11 (нет корня HARICA 2021), по решению пользователя источник — рыночный курс open.er-api.com. Фактические классы: `MarketRates`, `Rates(current, previous?)`, `RatesParser`, `RatesHistory.advance`; `RefreshPolicy.ratesDue(trigger, pending, lastAttemptMs, nowMs)` с повтором не чаще 10 минут. См. spec §7.5.

**Files:**
- Create: `SRC/info/Rates.kt` (`CbrDaily`, `Rates`, `CbrParser`, `Cbr`)
- Modify: `SRC/info/Format.kt` (+`rate`, `delta`, `Trend`, `Delta`), `SRC/info/InfoCache.kt`, `SRC/info/InfoRepository.kt`, `SRC/ui/InfoViews.kt` (+`bindRates`), `RES/layout/activity_home.xml` (содержимое `rates_card`)
- Test: `TEST/info/RatesTest.kt`, `TEST/info/FormatTest.kt`

**Interfaces:**
- Produces:
  - `data class CbrDaily(val date: LocalDate, val usd: Double, val eur: Double)`; `data class Rates(val current: CbrDaily, val previous: CbrDaily)`
  - `object CbrParser { fun parse(xml: String): CbrDaily? }`
  - `object Cbr { const val LATEST_URL = "https://www.cbr.ru/scripts/XML_daily.asp"; fun urlFor(date: LocalDate): String; fun previousRequestDate(latest: LocalDate): LocalDate }`
  - `enum class Trend { UP, DOWN, FLAT }`; `data class Delta(val text: String, val trend: Trend)`; `Format.rate(v: Double): String`; `Format.delta(d: Double): Delta`
  - `InfoViews.bindRates(card: View, r: Rates?, dim: Boolean)`
  - В `InfoCache`: `rates: Rates?`, `ratesPending: Boolean` (по умолчанию `true`).

`CbrParser`: ответ скачивается как windows-1251 (`Http.get(url, Charset.forName("windows-1251"))`). Дата берётся из `<ValCurs Date="dd.MM.yyyy"`. Для `USD` и `EUR` курс — `VunitRate`, если он есть, иначе `Value / Nominal`; десятичная запятая. Нет даты или одной из валют → `null`. Разбор регулярными выражениями по блокам `<Valute …>…</Valute>`, без XML-парсера платформы, чтобы тестировалось на JVM. `urlFor` → `LATEST_URL + "?date_req=dd/MM/yyyy"`. `previousRequestDate(latest) = latest.minusDays(1)`. Репозиторий: при `ratesDue` запрашивает `LATEST_URL`, потом `urlFor(previousRequestDate(current.date))`. Только если оба ответа разобрались — сохраняет `Rates` и ставит `ratesPending = false`. В момент `POWER_ON` перед запросом ставит `ratesPending = true`. `rate`: `BigDecimal(v.toString()).setScale(2, HALF_UP)`, запятая. `delta`: если `|d| < 0.005` → `Delta("0,00", FLAT)`, иначе «▲ x,xx» (UP) или «▼ x,xx» (DOWN) с модулем, округлённым так же. Цвета: UP `#3DDC84`, DOWN `#FF6B6B`, FLAT `#8B8D98`. Карточка: строки `$` и `€` (символ 12sp `#8B8D98`, курс 17sp, разница 10sp).

- [ ] **Step 1: Write the failing tests:**
  - `RatesTest.parsesLatestCbr`: XML с `<ValCurs Date="23.10.2026" name="Foreign Currency Market">`, USD `<Nominal>1</Nominal>…<Value>81,1255</Value><VunitRate>81,1255</VunitRate>`, EUR `<Value>91,1364</Value><VunitRate>91,1364</VunitRate>` → `CbrDaily(2026-10-23, 81.1255, 91.1364)`.
  - `RatesTest.usesNominalWhenNoVunitRate`: USD `<Nominal>10</Nominal><Value>811,255</Value>` без `VunitRate` → `usd == 81.1255` (±1e-9).
  - `RatesTest.parseRejectsHtmlAndPartial`: `"<html>Доступ запрещён</html>"` → `null`; XML без EUR → `null`; `""` → `null`.
  - `RatesTest.urlsAndPreviousDate`: `urlFor(2026-10-22) == "https://www.cbr.ru/scripts/XML_daily.asp?date_req=22/10/2026"`, `previousRequestDate(2026-10-23) == 2026-10-22`.
  - `FormatTest.rateHasTwoDecimalsWithComma`: `81.1255` → «81,13», `91.1364` → «91,14».
  - `FormatTest.deltaShowsArrow`: `81.1255 − 81.9739` → `Delta("▼ 0,85", DOWN)`, `0.34` → `Delta("▲ 0,34", UP)`.
  - `FormatTest.tinyDeltaIsFlat`: `0.004`, `−0.0049`, `0.0` → `Delta("0,00", FLAT)`.
- [ ] **Step 2: Run** `./gradlew :app:testDebugUnitTest --tests 'com.example.tvlauncher.info.*'` — Expected: FAIL.
- [ ] **Step 3: Implement** чистую часть → Run → PASS.
- [ ] **Step 4: Implement** Android-часть (`InfoCache`, `InfoRepository`, `bindRates`).
- [ ] **Step 5: Проверка на устройстве.** `tools/deploy.sh && tools/shot.sh t9-rates` → `$` и `€` совпадают с `curl -s https://www.cbr-xml-daily.ru/daily_json.js` (поля `Value` и `Previous`) до копейки, стрелки в нужную сторону. `adb -s $TV logcat -d -s TvLauncher:*` → после установки (`POWER_ON`) есть `rates fetch`. Переход в «Все» и обратно не даёт нового `rates fetch`.
- [ ] **Step 6: Commit** — `feat: курсы ЦБ`.

### Task 10: Режим без интернета

**Files:**
- Create: `SRC/info/Connectivity.kt`, `RES/drawable/ic_wifi_off.xml`
- Modify: `SRC/ui/HomeActivity.kt`, `SRC/ui/InfoViews.kt`, `RES/layout/activity_home.xml` (`offline_icon` рядом с датой), `app/build.gradle.kts` (`buildFeatures { buildConfig = true }`)

**Interfaces:**
- Consumes: `InfoRepository.onHomeVisible`, `InfoViews.bindWeather/bindRates(…, dim)`.
- Produces: `class Connectivity(context: Context) { fun isOnline(): Boolean; fun watch(onChange: (Boolean) -> Unit); fun unwatch() }` — онлайн, если у активной сети есть `NET_CAPABILITY_VALIDATED`. `watch` — `registerDefaultNetworkCallback`, колбэк на главном потоке. Только в debug-сборке (`BuildConfig.DEBUG`): если существует файл `filesDir/force_offline`, `isOnline()` возвращает `false`. Это нужно, чтобы проверить офлайн-вид, не обрывая adb.

`HomeActivity`: `watch` в `onStart`, `unwatch` в `onStop`. Офлайн → `offline_icon` виден (перечёркнутый Wi-Fi 16dp `#8B8D98`), карточки погоды и курсов с `alpha = 0.45`. Переход в онлайн → значок скрыт, `alpha = 1`, вызывается `info.onHomeVisible()`. Ни тостов, ни диалогов.

- [ ] **Step 1: Implement** по Interfaces.
- [ ] **Step 2: Проверка на устройстве (debug-сборка).** `adb -s $TV shell run-as com.example.tvlauncher touch files/force_offline`, затем открыть «Все» и вернуться (`onStart` перечитает состояние), затем `tools/shot.sh t10-offline`. Expected: значок рядом с датой, карточки тусклые, прежние цифры на месте, ошибок нет. `adb -s $TV shell run-as com.example.tvlauncher rm files/force_offline`, снова «Все» и назад, `tools/shot.sh t10-online` → обычный вид без значка.
- [ ] **Step 3: Commit** — `feat: значок «нет интернета» и тусклые данные`.

### Task 11: Release-сборка, замер памяти и итоговая проверка

**Files:**
- Modify: при необходимости `app/build.gradle.kts` и правила R8 (`app/proguard-rules.pro`), если R8 что-то вырезал.

- [ ] **Step 1: Собрать и поставить release:** `tools/deploy.sh release`. Expected: установилось поверх debug, HOME — наш лаунчер.
- [ ] **Step 2: Пройти критерии spec §10** на устройстве: перезагрузка `adb -s $TV reboot`, после неё HOME — лаунчер (`resolve-activity`, как в Task 1), погода и курсы загрузились. Скриншоты `t11-home`, `t11-all`. Навигация пультом по всей сетке без задержек — спросить у пользователя.
- [ ] **Step 3: Замер памяти.** Через 2 минуты на главном экране: `adb -s $TV shell ps -A -o RSS,NAME | grep tvlauncher`. Expected: RSS заметно меньше 160 000 КБ, ориентир ≤ 60 000 КБ. Записать число в коммит.
- [ ] **Step 4: Commit** — `chore: release-сборка, замер памяти NN МБ`.
