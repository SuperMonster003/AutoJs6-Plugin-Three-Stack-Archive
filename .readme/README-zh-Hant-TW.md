<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-archive-browser-ic-launcher" border="0" width="128" />
  </p>

  <p>為 AutoJs6 檔案瀏覽器提供 ZIP 系列壓縮檔唯讀瀏覽與選擇性 SAF 解壓縮</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Archive-Browser?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Archive-Browser?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Archive-Browser?color=534BAE&label=License"/></a>
  </p>
</div>

******

### 語言 (Languages)

******

目前 README.md 支援以下語言:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/.readme/README-zh-Hans.md)
- [香港繁體 [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/.readme/README-zh-Hant-HK.md)
- 台灣繁體 [zh-Hant-TW] # 目前
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/.readme/README-ar.md)

******

### 簡介

******

AutoJs6 壓縮檔瀏覽器外掛程式為 AutoJs6 檔案瀏覽器提供唯讀壓縮檔瀏覽功能. 外掛程式會在專用的階層檢視器中開啟以 ZIP 為基礎的容器, 並且只會將使用者選取的項目解壓縮到透過 Android Storage Access Framework 選定的輸出資料夾.

******

### 功能

******

- 透過共用的 `org.autojs.plugin.EXPLORER_ACTION` 協定註冊單一檔案唯讀檔案瀏覽器更多選單動作.
- 瀏覽壓縮檔目錄, 並顯示解壓縮後大小, 壓縮後大小, CRC 和修改時間中繼資料.
- 搜尋正規化項目路徑, 支援選取個別檔案, 資料夾或全部可見項目.
- 將所選項目解壓縮到使用者選定的 SAF 目錄樹, 並提供進度回報與取消操作.
- 接受使用支援 ZIP 壓縮方法的 ZIP, JAR, AAR 和 WAR 容器.
- 將唯讀輸入暫存到應用程式私有快取, 並在檢視器關閉時移除暫存資料.

******

### 支援格式

******

版本 1 辨識以下 ZIP 系列檔案副檔名:

```text
zip, jar, aar, war
```

******

### 外掛程式介面

******

AutoJs6 透過以下識別資訊發現並執行外掛程式:

```text
service action: org.autojs.plugin.EXPLORER_ACTION
execute action: org.autojs.plugin.EXPLORER_ACTION_EXECUTE
plugin id: archive-browser
engine: explorer-action
variant: default
```

版本 1 只在 AutoJs6 主檔案瀏覽器中提供單一檔案唯讀更多選單動作.

******

### 安全性

******

外掛程式不會要求儲存空間或網路權限. 主程式只會暫時授予輸入 content URI 的唯讀權限, 輸出權限則限制在使用者明確選取的 SAF 目錄. 外掛程式會拒絕絕對路徑, 上層目錄穿越, 磁碟機前綴, 反斜線, 不安全 Unicode, 重複路徑, 檔案與目錄衝突, 不支援的壓縮方法, 大小不符和 CRC 不符.

******

### 安全限制

******

- 暫存輸入上限: `4 GiB`.
- 壓縮檔路徑節點數量上限, 包含隱含目錄: `20,000`.
- ZIP 中央目錄大小上限: `64 MiB`.
- 正規化路徑長度上限: `1,024` 個字元.
- 路徑深度上限: `64` 層.
- 單一項目解壓縮後上限: `512 MiB`.
- 解壓縮後總資料上限: `2 GiB`.
- 壓縮比率上限: `1000:1`.

******

### 發行記錄

******

# v1.0.0

###### 2026/08/02

* `功能` 壓縮檔瀏覽器外掛程式, 外掛程式 ID 為 `archive-browser`, 引擎為 `explorer-action`, 變體為 `default`
* `功能` 適用於 ZIP, JAR, AAR 和 WAR 容器的單一檔案唯讀檔案瀏覽器動作, 包含階層瀏覽, 路徑搜尋和項目選取
* `功能` 將所選項目解壓縮到使用者選取的 SAF 目錄樹, 支援進度與取消, 僅暫時讀取輸入, 不會要求儲存空間或網路權限
* `功能` 安全限制為輸入 4 GiB, 20,000 個路徑節點並包含隱含目錄, ZIP 中央目錄 64 MiB, 單一項目解壓縮後 512 MiB, 解壓縮後總資料 2 GiB, 壓縮比率 1000:1
* `功能` 針對不安全路徑, 重複和衝突項目, 不支援的壓縮方法, 輸入來源變更, 大小不符和 CRC 不符的驗證
* `功能` 外掛程式中繼資料, 介面文字, 使用說明, README 和 CHANGELOG 的多語言資源: 西班牙文/法文/俄文/阿拉伯文/日文/韓文/英文/簡體中文/香港繁體/台灣繁體

##### 更多發行記錄請參閱

* [CHANGELOG-zh-Hant-TW.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/app/src/main/assets/doc/CHANGELOG-zh-Hant-TW.md)

******

### 建置

******

```powershell
.\gradlew.bat :app:assembleDebug
```

Release 建置:

```powershell
.\gradlew.bat :app:assembleRelease
```

建置參數來自 `version.properties`, 目前最低 SDK 為 24, 目標 SDK 為 36.

******

### 資源結構

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

`strings.xml` 提供外掛程式中繼資料和瀏覽器固定文字的本地化, `plurals.xml` 提供瀏覽器數量文字的本地化. `plugin_instruction.md` 提供主程式端顯示的使用說明. README 與 CHANGELOG 由 `.python/generate_markdown.py` 根據 JSON 來源檔案產生.

******

### 相關連結

******

- AutoJs6 文件: https://docs.autojs6.com
- Android Storage Access Framework: https://developer.android.com/guide/topics/providers/document-provider
