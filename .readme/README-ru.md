<div align="center">
  <p>
    <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="Archive Manager" width="128" />
  </p>

  <h1>Archive Manager</h1>

  <p>Плагин файлового менеджера AutoJs6 для открытия, извлечения и создания ZIP-архивов</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Archive-Manager?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Archive-Manager?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Archive-Manager?color=534BAE&label=License"/></a>
  </p>
</div>

### Языки (Languages)

README доступен на следующих языках:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-zh-Hans.md)
- [香港繁體 [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-zh-Hant-HK.md)
- [台灣繁體 [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-ko.md)
- Русский [ru] # текущий
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-ar.md)

### О проекте

Archive Manager интегрирует просмотр, извлечение и создание ZIP в файловый менеджер AutoJs6. Текущая версия просматривает архивы в нативном списке хоста с внешним и внутренним путями, а также сжимает один объект или выбор из общей родительской папки. Дополнительные форматы, предпросмотр элементов и внутреннее редактирование остаются в Roadmap.

### Доступно сейчас

- Открытие ZIP-подобных архивов прямо в нативном списке файлов AutoJs6 с темой хоста, тёмным режимом и динамическими цветами.
- Отображение внешней папки, имени архива и внутренней папки в строке пути; переход по нажатию на уровень и подъём кнопкой Назад перед выходом из архива.
- Быстрое извлечение всего архива через «Извлечь в...» без предварительного открытия просмотра.
- Навигация по папкам архива, поиск и сортировка.
- Построение списка по метаданным без предварительной распаковки каждого элемента.
- Поддержка Zip64, префиксов самораспаковывающихся архивов, старых кодировок имён и разделителей Windows.
- Команда «Сжать...» для обычных файлов, папок и множественного выбора с общей родительской папкой.
- Создание ZIP с настраиваемыми именем и уровнем сжатия; имя объекта по умолчанию для одного элемента и имя родительской папки для нескольких.
- Запись во временный файл в той же папке с последующей атомарной фиксацией; автоматическая нумерация конфликтов без перезаписи существующих файлов.

### Текущие форматы

Текущая версия распознаёт следующие расширения семейства ZIP:

```text
zip, jar, aar, war
```

Текущая версия может создавать следующие форматы:

```text
zip
```

> Для нативного просмотра Explorer Action v5 и сжатия v4 требуется AutoJs6 с кодом версии 5276 или новее. Предпросмотр внутренних файлов, извлечение отдельных элементов, 7z, варианты tar, пароли, многотомные архивы, шифрование имён, отдельные архивы, удаление исходников и добавление/удаление внутри архива ещё не выпущены. Ориентируйтесь на отметки Roadmap.

### Использование

1. Установите плагин и включите его в центре плагинов AutoJs6.
2. Откройте меню файла ZIP, JAR, AAR или WAR.
3. Выберите «Открыть архив», затем открывайте папки, ищите или переходите по строке пути в списке файлов хоста.
4. Чтобы извлечь весь архив, выберите «Извлечь в...» в меню файла и укажите выходную папку через системный выбор Android.
5. Чтобы создать ZIP, выберите «Сжать...» в меню обычного файла или папки либо отметьте несколько элементов в одной папке и используйте «Сжать...» на нижней панели.

### Разрешения и данные

Плагин не запрашивает доступ к хранилищу или сети. Нативный просмотр использует короткий сеанс архива только для чтения, привязанный к UID хоста, и удаляет временный ввод при закрытии страницы или отключении; извлечение использует только временно выданный хостом URI. Создание ZIP использует файловый сеанс хоста, привязанный к UID плагина, читает цели постранично и может создать транзакционный вывод только в текущей родительской папке. Фиксированный предел ввода 4 ГиБ и ограничения просмотра удалены; изоляция путей, проверки целостности и очистка после ошибок сохранены.

### Roadmap

Задачи и критерии для новых форматов, паролей и томов, внутреннего предпросмотра и извлечения отдельных элементов, редактирования архивов и полной матрицы устройств находятся в Roadmap. Неотмеченный пункт не является текущей функцией.

- [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/ROADMAP.md)

### Примечания к выпуску

#### Unreleased

_Не выпущено_

- `Добавлено` Название продукта изменено на Archive Manager, действие — «Открыть архив»
- `Добавлено` Explorer Action v5 открывает архивы в нативном списке AutoJs6 с существующими строкой пути, темой и навигацией Назад
- `Добавлено` Действие «Извлечь в...» для выбора папки и извлечения всего архива
- `Добавлено` Explorer Action v4 добавляет «Сжать...» в меню обычных файлов и папок и на панель из пяти действий для выбора с общей родительской папкой
- `Добавлено` Создание ZIP с именем по умолчанию, уровнями сжатия, прогрессом, отменой и автоматической нумерацией конфликтов
- `Исправлено` Список ZIP теперь строится по метаданным и поддерживает префиксы SFX, старые кодировки, разделители Windows и больше читаемых методов
- `Исправлено` Неизвестный размер, корректные URI DocumentsProvider и дополнительные права записи больше не блокируют допустимый архив
- `Исправлено` Исправлены просмотр и распаковка ZIP в Android 7.x, ранее вызывавшие API только новых версий системы
- `Улучшено` Удалены фиксированный предел 4 ГиБ и пороги размера/коэффициента при просмотре с сохранением изоляции и проверки целостности
- `Улучшено` Добавлен проверяемый Roadmap, README и CHANGELOG переписаны
- `Улучшено` Отдельный экран следует системной теме и динамическим цветам Material
- `Улучшено` ZIP записывается через привязанный к UID сеанс хоста во временный файл той же папки и атомарно фиксируется без разрешения хранилища и перезаписи

#### v1.0.1

_2026/08/08_

- `Исправлено` Пустая привязка службы при включении плагина
- `Улучшено` Упрощены название, описание и инструкция

#### v1.0.0

_2026/08/02_

- `Добавлено` Первый выпуск для просмотра ZIP, JAR, AAR и WAR и извлечения выбранного
- `Добавлено` Поиск, выбор, прогресс, отмена, очистка временных данных и локализованный интерфейс

##### Полная история

* [CHANGELOG-ru.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/app/src/main/assets/doc/CHANGELOG-ru.md)

### Сборка

```powershell
.\gradlew.bat :app:assembleDebug
```

Release-сборка:

```powershell
.\gradlew.bat :app:assembleRelease
```

Используйте Gradle Wrapper из корня; требования SDK/JDK определяются `version.properties`.

### Ссылки

- Документация AutoJs6: https://docs.autojs6.com
- Android Storage Access Framework: https://developer.android.com/guide/topics/providers/document-provider
