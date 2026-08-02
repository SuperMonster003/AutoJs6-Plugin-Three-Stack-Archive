<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-archive-browser-ic-launcher" border="0" width="128" />
  </p>

  <p>Просмотр архивов семейства ZIP только для чтения и выборочное извлечение через SAF для Проводника AutoJs6</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Archive-Browser?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Archive-Browser?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Archive-Browser?color=534BAE&label=License"/></a>
  </p>
</div>

******

### Языки (Languages)

******

Текущий файл README.md поддерживает следующие языки:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/.readme/README-zh-Hans.md)
- [香港繁體 [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/.readme/README-zh-Hant-HK.md)
- [台灣繁體 [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/.readme/README-ko.md)
- Русский [ru] # текущий
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/.readme/README-ar.md)

******

### Введение

******

Плагин AutoJs6 Archive Browser добавляет в Проводник AutoJs6 просмотр архивов только для чтения. Он открывает контейнеры на основе ZIP в отдельном иерархическом окне и извлекает только выбранные пользователем записи в выходную папку, указанную через Android Storage Access Framework.

******

### Возможности

******

- Регистрирует дополнительное действие только для чтения над одним файлом через общий протокол `org.autojs.plugin.EXPLORER_ACTION`.
- Отображает папки архива, распакованный и сжатый размеры, CRC и время изменения.
- Ищет нормализованные пути записей и позволяет выбирать отдельные файлы, папки или все видимые записи.
- Извлекает выбранные записи в указанное пользователем дерево SAF с отображением хода и возможностью отмены.
- Принимает контейнеры ZIP, JAR, AAR и WAR с поддерживаемыми методами сжатия ZIP.
- Копирует входной файл только для чтения в закрытый кэш и удаляет временные данные при закрытии окна.

******

### Поддерживаемые форматы

******

Версия 1 распознает следующие расширения семейства ZIP:

```text
zip, jar, aar, war
```

******

### Интерфейс плагина

******

AutoJs6 обнаруживает и запускает плагин со следующими идентификаторами:

```text
service action: org.autojs.plugin.EXPLORER_ACTION
execute action: org.autojs.plugin.EXPLORER_ACTION_EXECUTE
plugin id: archive-browser
engine: explorer-action
variant: default
```

Версия 1 ограничена дополнительным действием только для чтения над одним файлом в главном Проводнике AutoJs6.

******

### Безопасность

******

Плагин не запрашивает разрешения на хранилище или сеть. Хост выдает временный доступ только для чтения к входному content URI, а доступ на запись ограничен каталогом SAF, явно выбранным пользователем. Абсолютные пути, переходы в родительский каталог, префиксы дисков, обратные косые черты, небезопасный Unicode, повторяющиеся пути, конфликты файлов и каталогов, неподдерживаемые методы сжатия, несовпадения размеров и CRC отклоняются.

******

### Ограничения безопасности

******

- Максимальный размер входного файла в кэше: `4 GiB`.
- Максимальное число узлов путей архива, включая неявные каталоги: `20,000`.
- Максимальный размер центрального каталога ZIP: `64 MiB`.
- Максимальная длина нормализованного пути: `1,024` символа.
- Максимальная глубина пути: `64` сегмента.
- Максимальный распакованный размер одной записи: `512 MiB`.
- Максимальный общий распакованный размер: `2 GiB`.
- Максимальный коэффициент сжатия: `1000:1`.

******

### История выпусков

******

# v1.0.0

###### 2026/08/02

* `Функция` Плагин Archive Browser с ID `archive-browser`, движком `explorer-action` и вариантом `default`
* `Функция` Действие Проводника только для чтения над одним контейнером ZIP, JAR, AAR или WAR с иерархическим просмотром, поиском путей и выбором записей
* `Функция` Выборочное извлечение в указанное пользователем дерево SAF с ходом и отменой, временный доступ только для чтения к входному файлу и отсутствие разрешений на хранилище или сеть
* `Функция` Ограничения безопасности: входной файл 4 GiB, 20,000 узлов путей с учетом неявных каталогов, центральный каталог ZIP 64 MiB, 512 MiB для одной распакованной записи, 2 GiB распакованных данных всего и коэффициент сжатия 1000:1
* `Функция` Проверка небезопасных путей, повторяющихся или конфликтующих записей, неподдерживаемых методов сжатия, изменений источника, несовпадений размеров и CRC
* `Функция` Локализованные метаданные, интерфейс, инструкции, README и CHANGELOG на испанском, французском, русском, арабском, японском, корейском, английском, упрощенном китайском, традиционном китайском для Гонконга и традиционном китайском для Тайваня

##### Другие выпуски

* [CHANGELOG-ru.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/app/src/main/assets/doc/CHANGELOG-ru.md)

******

### Сборка

******

```powershell
.\gradlew.bat :app:assembleDebug
```

Сборка Release:

```powershell
.\gradlew.bat :app:assembleRelease
```

Параметры сборки берутся из `version.properties`. Текущий минимальный SDK равен 24, целевой SDK равен 36.

******

### Структура ресурсов

******

```text
.readme/lang_*.json
.changelog/lang_*.json
.python/generate_markdown.py
app/src/main/assets/doc/CHANGELOG-*.md
app/src/main/res/values-*/strings.xml
app/src/main/res/values-*/plurals.xml
app/src/main/res/raw-*/plugin_instruction.md
```

`strings.xml` локализует метаданные плагина и постоянные тексты интерфейса, а `plurals.xml` локализует тексты, зависящие от количества. `plugin_instruction.md` содержит отображаемые хостом инструкции. `.python/generate_markdown.py` создает README и журналы изменений из исходных файлов JSON.

******

### Ссылки

******

- Документация AutoJs6: https://docs.autojs6.com
- Android Storage Access Framework: https://developer.android.com/guide/topics/providers/document-provider
