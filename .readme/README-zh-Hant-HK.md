<div align="center">
  <p>
    <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="Archive Manager" width="128" />
  </p>

  <h1>Archive Manager</h1>

  <p>用於開啟、解壓縮及建立 ZIP 壓縮檔的 AutoJs6 檔案管理器外掛程式</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Archive-Manager?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Archive-Manager?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Archive-Manager?color=534BAE&label=License"/></a>
  </p>
</div>

### 語言 (Languages)

README 提供以下語言版本:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-zh-Hans.md)
- 香港繁體 [zh-Hant-HK] # 目前
- [台灣繁體 [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-ar.md)

### 項目簡介

壓縮檔管理器把 ZIP 瀏覽、解壓縮及建立功能帶入 AutoJs6 檔案管理器。目前版本可在主程式原生檔案清單中瀏覽壓縮檔、顯示外部及內部路徑、預覽支援的項目，並支援單一項目及同一上層目錄多選壓縮；更多格式、按項目解壓縮及檔案內修改會按 Roadmap 推進。

### 目前能力

- 直接在 AutoJs6 原生檔案清單中開啟 ZIP 系列壓縮檔，沿用主程式主題、深色模式及動態色彩。
- 路徑列同時顯示外部目錄、壓縮檔名稱及內部目錄；可按下層級跳轉，返回鍵會先返回內部上一級，再離開壓縮檔。
- 使用主程式現有預覽器開啟支援的文件、圖片、音訊及影片項目。
- 透過「解壓縮到...」捷徑直接解壓縮整個壓縮檔，無需先進入瀏覽頁面。
- 按目錄瀏覽、搜尋及排序壓縮檔內容。
- 只讀取目錄中繼資料便顯示清單，不會預先解壓縮全部內容。
- 相容 Zip64、自解壓縮式前置資料、傳統檔名編碼及 Windows 路徑分隔符。
- 為一般檔案、資料夾及同一上層目錄的多選項目提供「壓縮...」動作。
- 建立 ZIP 時可設定檔案名稱及壓縮等級；單一項目預設使用目標名稱，多選預設使用上層資料夾名稱。
- 先寫入同一目錄的暫存檔，再以原子方式提交；名稱衝突時自動加入序號，不會覆寫現有檔案。

### 目前支援

目前版本識別以下 ZIP 系列副檔名:

```text
zip, jar, aar, war
```

目前版本可建立以下格式:

```text
zip
```

> Explorer Action v6 原生瀏覽與項目預覽以及 v4 壓縮整合需要 AutoJs6 版本代碼 5276 或以上。按項目解壓縮、7z、tar 系列、密碼、分卷、檔案名稱加密、個別壓縮、壓縮後刪除來源項目及檔案內新增/刪除尚未發布；請以 Roadmap 的核取狀態為準。

### 使用方法

1. 安裝外掛程式，並在 AutoJs6 外掛程式中心啟用。
2. 在檔案管理器開啟 ZIP、JAR、AAR 或 WAR 的選單。
3. 選擇「開啟壓縮檔」，然後在主程式檔案清單進入目錄、搜尋或使用路徑列跳轉。
4. 如要解壓縮整個壓縮檔，請從檔案選單選擇「解壓縮到...」，再透過 Android 系統選擇器指定輸出目錄。
5. 如要建立 ZIP，請從一般檔案或資料夾選單選擇「壓縮...」；亦可先在同一目錄多選項目，再使用底部的「壓縮...」動作。

### 權限與資料

外掛程式不要求儲存空間或網絡權限。原生瀏覽使用綁定主程式 UID 的短期唯讀壓縮檔工作階段，頁面關閉或解除連接後清理暫存輸入；解壓縮只使用主程式臨時授予的輸入 URI。建立 ZIP 透過綁定外掛程式 UID 的主程式檔案工作階段分頁讀取目標，並且只能在目前上層目錄建立交易式輸出。固定 4 GiB 輸入上限及瀏覽階段大小/壓縮比門檻已移除；路徑隔離、完整性驗證及失敗清理仍然保留。

### Roadmap

更多格式、密碼與分卷、按項目解壓縮、壓縮檔編輯及完整裝置矩陣的任務與驗收條件均記錄在 Roadmap。未勾選項目不是目前功能。

- [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/ROADMAP.md)

### 版本記錄

#### Unreleased

_未發布_

- `新增` 產品名稱統一為「壓縮檔管理器」，開啟動作調整為「開啟壓縮檔」
- `新增` 透過 Explorer Action v5 在 AutoJs6 原生檔案清單中瀏覽壓縮檔，並沿用路徑列、主題及返回導覽
- `新增` 透過 Explorer Action v6 使用主程式的文件、圖片、音訊及影片預覽器開啟支援的壓縮檔項目
- `新增` 「解壓縮到...」捷徑，可直接選取目錄並解壓縮整個壓縮檔
- `新增` 接入 Explorer Action v4，在一般檔案、資料夾及同一上層目錄多選的五項操作列提供「壓縮...」
- `新增` 新增 ZIP 建立表單，支援預設命名、壓縮等級、進度、取消及同名自動編號
- `修正` 改用目錄中繼資料快速開啟 ZIP，並相容自解壓縮式前置資料、傳統檔名編碼、Windows 分隔符及更多可讀取的 ZIP 方法
- `修正` 未知或不精確大小、有效 DocumentsProvider URI 及主程式額外寫入授權不再令有效壓縮檔在解析前被拒絕
- `修正` 修正 Android 7.x 因呼叫新版系統專有 API 而無法瀏覽或解壓縮 ZIP 的問題
- `改善` 移除固定 4 GiB 輸入上限及瀏覽階段大小/壓縮比門檻，同時保留路徑隔離、完整性驗證及失敗清理
- `改善` 新增可逐項追蹤的 Roadmap，並重寫 README 與 CHANGELOG
- `改善` 獨立頁面改為跟隨系統日夜模式及 Material 動態色
- `改善` ZIP 輸出透過綁定外掛程式 UID 的主程式工作階段寫入同目錄暫存檔後原子提交，無需儲存權限且不會覆寫現有檔案

#### v1.0.1

_2026/08/08_

- `修正` 在外掛程式中心啟用時服務綁定為空的問題
- `改善` 簡化名稱、描述及使用說明

#### v1.0.0

_2026/08/02_

- `新增` 首個可用版本，支援瀏覽 ZIP、JAR、AAR 及 WAR 並解壓縮所選內容
- `新增` 提供搜尋、選取、進度、取消、暫存清理及多語言介面

##### 完整記錄

* [CHANGELOG-zh-Hant-HK.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/app/src/main/assets/doc/CHANGELOG-zh-Hant-HK.md)

### 建置

```powershell
.\gradlew.bat :app:assembleDebug
```

Release 建置:

```powershell
.\gradlew.bat :app:assembleRelease
```

在項目根目錄使用 Gradle Wrapper；SDK 及 JDK 要求以 `version.properties` 為準.

### 相關連結

- AutoJs6 文件: https://docs.autojs6.com
- Android Storage Access Framework: https://developer.android.com/guide/topics/providers/document-provider
