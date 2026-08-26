<div align="center">
  <p>
    <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="Archive Manager" width="128" />
  </p>

  <h1>Archive Manager</h1>

  <p>深度整合 AutoJs6 檔案管理器的通用壓縮檔案管理外掛, 支援安全瀏覽、解壓縮、建立與受控修改</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Archive-Manager?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Archive-Manager?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Archive-Manager?color=534BAE&label=License"/></a>
  </p>
</div>

### 語言 (Languages)

README 提供以下語言版本:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-zh-Hans.md)
- [香港繁體 [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-zh-Hant-HK.md)
- 台灣繁體 [zh-Hant-TW] # 目前
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-ar.md)

### 專案簡介

Archive Manager 直接在 AutoJs6 檔案管理器內運作, 不會另建一套檔案清單. 支援的檔案會沿用宿主清單、路徑列、主題、預覽器、多選、進度與目錄重新整理. 只有需要更多設定或寫入操作時才進入獨立管理頁.

### 目前功能

- 在 AutoJs6 原生檔案清單中瀏覽 ZIP/JAR/AAR/WAR、7Z、RAR4/RAR5 與 TAR 系列檔案, 並沿用內部路徑列、搜尋、排序與返回導覽.
- 直接使用宿主現有的文件、圖片、音訊與影片預覽器讀取可用項目, 無需先解壓縮整個檔案.
- 可解壓縮整個檔案、目前內部目錄或已選項目, 支援進度、取消、安全衝突編號、發佈前驗證與提交前復原.
- 加密 ZIP、7Z 與 RAR 可在首次開啟或解壓縮時使用宿主原生密碼框; 密碼錯誤後可在原位重試, 不會失去目前檔案路徑.
- 可由單一項目或同父層多選建立 ZIP、7Z、TAR、TAR.GZ、TAR.XZ、TAR.BZ2 與 TAR.ZST; ZIP 另支援 AES-256 與標準分卷, 多選也可為每項分別建立檔案.
- 普通單卷 ZIP 可透過已驗證重建加入檔案、匯入完整目錄樹、建立空目錄、重新命名與刪除; 只有完整讀回通過後才以原子方式取代來源檔案.
- 危險檔案名稱保持唯讀隔離, 寫出前仍執行結構與資源檢查; 對可 seek 的宿主描述符優先直接讀取, 避免不必要的整份複製.

### 目前支援

目前版本識別以下可瀏覽與解壓縮的副檔名:

```text
zip, jar, aar, war, 7z, rar, tar, tar.gz, tgz, tar.xz, txz, tar.bz2, tbz2, tar.zst, tzst
```

目前版本可建立以下格式:

```text
zip, 7z, tar, tar.gz, tar.xz, tar.bz2, tar.zst
```

> 原生整合需要 AutoJs6 6.8.0, 版本代碼 5276 或更新版本, 並使用 Explorer Action v11. RAR 明確保持唯讀. 目前只有普通單卷 `.zip` 可修改. 現有 ZIP/RAR 分卷尚不能作為完整卷組讀取, 因為宿主目前只授予所選檔案的描述符; RAR 首卷可顯示中繼資料, 但不會提供解壓縮入口. 建立時加密檔案名稱、壓縮後刪除來源檔案, 以及修改 7Z、RAR 和 TAR 系列檔案均不是目前能力.

### 使用方式

1. 安裝 Archive Manager, 並在 AutoJs6 外掛中心啟用.
2. 點選檔案的主要操作, 或從選單選擇「開啟壓縮檔案」. 接著可像普通目錄一樣使用宿主路徑列.
3. 使用路徑列右側操作解壓縮目前內部目錄; 長按項目可多選解壓縮; 檔案選單的「解壓縮到...」用於解壓縮整個檔案. 需要密碼時宿主會自動提示.
4. 對檔案或資料夾選擇「壓縮...」; 也可在同一目錄多選後使用底部操作列的「壓縮...」.
5. 只有需要加入、重新命名或刪除內容時, 才對普通單卷 ZIP 選擇「管理壓縮檔案...」.

### 權限與資料

Archive Manager 不會申請儲存空間或網路權限. 宿主只提供短生命週期唯讀描述符與固定外掛 UID 的輸出交易, 外掛不能自行選擇任意檔案系統路徑. Explorer Action v11 只會在同步重試要求中傳遞有界密碼; 宿主與外掛會立即移除並清理各自保留的緩衝, 且不把密碼儲存到狀態、偏好設定、日誌或診斷. Android 與 Java 程式庫仍可能產生無法完全控制的短生命週期執行期副本, 因此這是盡力清理, 並非絕對不存在副本. 路徑穿越與危險名稱始終隔離, 輸出在發佈前完成驗證, 資源預算確認也不會關閉結構安全檢查.

### Roadmap

其餘工作繼續以可勾選項目追蹤: ZIP/RAR 分卷的同層卷輸入、宿主原生檔案名稱編碼修正、普通 ZIP 以外的可寫重建、復原或來源檔案刪除交易、無障礙檢查, 以及其餘裝置與製作工具矩陣.

- [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/ROADMAP.md)

### 版本記錄

#### v2.4.0

_2026/08/26_

- `提示` 此版本需要 AutoJs6 6.8.0 Explorer Action v11, 版本代碼 5276 或更新版本
- `新增` 支援瀏覽、預覽與解壓縮 RAR4/RAR5, 包括內容加密與標頭加密檔案; RAR 明確保持唯讀
- `新增` AutoJs6 原生檔案頁可在首次開啟或解壓縮期間要求密碼, 並在不失去目前路徑或選擇的情況下重試
- `修正` RAR 分卷首卷仍可顯示可讀中繼資料, 但同層卷不可用時不再顯示虛假的解壓縮入口
- `修正` 密碼錯誤後會清除上次輸入, 並針對未變更的檔案快照重試, 無需離開原生頁面
- `改善` RAR 會在可用時直接讀取宿主的可 seek 描述符, 不增加原生 ABI, 並沿用其他格式的路徑、資源與輸出安全檢查
- `相依性` 加入 Junrar 8.1.0 與 SLF4J 2.0.17, 依隨附授權條款提供唯讀 RAR 支援

#### v2.3.0

_2026/08/26_

- `提示` 此版本需要搭配 AutoJs6 6.8.0 Explorer Action v10 建置 (版本代碼 5276 或更高)
- `新增` 主程式原生壓縮檔頁面現可從路徑列解壓縮目前內部目錄, 或從選取列解壓縮已勾選項目, 無需開啟獨立管理頁面
- `新增` 原生解壓縮透過主程式擁有的目錄輸出樹寫入, 支援進度、取消、安全衝突編號與 Explorer 自動更新
- `修正` Android 7 離開壓縮檔時, 主程式清理預覽快取不再導致當機
- `修正` 五項檔案選取列的文字在窄螢幕上會置中顯示於圖示下方
- `改善` 壓縮檔選取模式僅顯示「離開」與「解壓縮」, 隱藏不適用於壓縮檔內部的檔案系統操作

#### v2.2.0

_2026/08/26_

- `提示` 此版本需要配套的 AutoJs6 6.8.0 Explorer Action v9 版本 (版本代碼 5276 或以上)
- `新增` 「解壓縮到...」現在建議使用目前的資料夾, 並透過主程式持有的目錄交易建立同名輸出資料夾; 等價名稱已存在時會安全編號, 且絕不變更現有內容
- `新增` 一般單一分割檔 ZIP 管理支援透過 Android 系統選擇器匯入完整資料夾, 包括巢狀檔案與空資料夾
- `修正` 解壓縮位置選擇對話框在窄螢幕與短螢幕上保持完整可用, 並顯示正確的預設路徑
- `修正` 同目錄解壓縮在取消、失敗、操作中斷或空間不足時會回復尚未發布的輸出; 下次工作階段會復原中斷的主程式交易, 且來源壓縮檔保持不變
- `改善` Explorer Action v9 以不可分割的方式發布已驗證的目錄樹, 提交後 AutoJs6 會立即顯示新的輸出資料夾

##### 完整記錄

* [CHANGELOG-zh-Hant-TW.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/app/src/main/assets/doc/CHANGELOG-zh-Hant-TW.md)

### 建置

```powershell
.\gradlew.bat :app:assembleDebug
```

Release 建置:

```powershell
.\gradlew.bat :app:assembleRelease
```

在專案根目錄使用 Gradle Wrapper；SDK 與 JDK 要求以 `version.properties` 為準.

### 相關連結

- AutoJs6 文件: https://docs.autojs6.com
- 第三方軟體聲明: [THIRD_PARTY_NOTICES.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/THIRD_PARTY_NOTICES.md)
- Android Storage Access Framework: https://developer.android.com/guide/topics/providers/document-provider
