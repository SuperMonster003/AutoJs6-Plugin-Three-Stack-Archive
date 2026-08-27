<div align="center">
  <p>
    <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="Archive Manager" width="128" />
  </p>

  <h1>Archive Manager</h1>

  <p>深度整合 AutoJs6 檔案管理器的通用壓縮檔案管理插件, 支援安全瀏覽、解壓、建立及受控修改</p>

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

Archive Manager 直接在 AutoJs6 檔案管理器內運作, 不會另建一套檔案列表. 支援的檔案會沿用宿主列表、路徑列、主題、預覽器、多選、進度及目錄重新整理. 只有需要更多設定或寫入操作時才進入獨立管理頁.

### 目前能力

- 在 AutoJs6 原生檔案列表中瀏覽 ZIP/JAR/AAR/WAR、7Z、RAR4/RAR5 及 TAR 系列檔案, 並沿用內部路徑列、搜尋、排序及返回導覽.
- 直接使用宿主現有的文件、圖片、音訊及影片預覽器讀取可用項目, 無需先解壓整個檔案.
- 可解壓整個檔案、目前內部目錄或已選項目, 支援進度、取消、安全衝突編號、發佈前驗證及提交前回復.
- 加密 ZIP、7Z 及 RAR 可在首次開啟或解壓時使用宿主原生密碼框; 密碼錯誤後可在原位重試, 不會失去目前檔案路徑.
- 可從宿主路徑列直接修正 ZIP 檔案名稱編碼; 同一唯讀工作階段在原位重建索引, 並盡量保留目前內部路徑及已選項目.
- 可透過宿主有界授權的同級卷描述符瀏覽、預覽及解壓完整的標準 `.z01 + .zip`、現代 WinRAR `partN.rar`、編號 `.zip.001` 及編號 `.7z.001` 卷組; 缺卷或卷變更會返回明確錯誤.
- 可由單一項目或同父級多選建立 ZIP、7Z、TAR、TAR.GZ、TAR.XZ、TAR.BZ2 及 TAR.ZST; ZIP 另支援 AES-256 及標準分卷, 多選亦可為每項分別建立檔案.
- 普通單卷 ZIP 可透過已驗證重建加入檔案、匯入完整目錄樹、建立空目錄、重新命名及刪除; 只有完整讀回通過後才以原子方式取代來源檔案.
- 危險檔案名稱保持唯讀隔離, 寫出前仍執行結構及資源檢查; 對可 seek 的宿主描述符優先直接讀取, 避免不必要的整份複製.

### 目前支援

目前版本識別以下可瀏覽及解壓縮的副檔名:

```text
zip, zip.001, jar, aar, war, 7z, 7z.001, rar, tar, tar.gz, tgz, tar.xz, txz, tar.bz2, tbz2, tar.zst, tzst
```

目前版本可建立以下格式:

```text
zip, 7z, tar, tar.gz, tar.xz, tar.bz2, tar.zst
```

> 原生整合需要配套的 AutoJs6 6.8.0 Explorer Action v14 建置 (版本代碼 5276 或以上). RAR 及分卷檔案明確保持唯讀, 目前只有普通單卷 `.zip` 可修改. 標準分卷 ZIP 應從最終 `.zip` 開啟, 現代 WinRAR 卷組應從首個 `partN.rar` 開啟, 編號 ZIP 或 7Z 應從 `.001` 開啟, 且所有必需同級卷須位於同一目錄. 建立時加密檔案名稱、壓縮後刪除來源檔案, 以及修改 7Z、RAR 和 TAR 系列檔案均不是目前能力.

### 使用方法

1. 安裝 Archive Manager, 並在 AutoJs6 插件中心啟用.
2. 點按檔案的主要操作, 或從選單選擇「開啟壓縮檔案」. 之後可像普通目錄一樣使用宿主路徑列.
3. 使用路徑列右側操作解壓目前內部目錄, 或透過檔案名稱編碼操作修正 ZIP 名稱; 長按項目可多選解壓; 檔案選單的「解壓到...」用於解壓整個檔案. 需要密碼時宿主會自動提示.
4. 對檔案或資料夾選擇「壓縮...」; 亦可在同一目錄多選後使用底部操作列的「壓縮...」.
5. 只有需要加入、重新命名或刪除內容時, 才對普通單卷 ZIP 選擇「管理壓縮檔案...」.

### 權限與資料

Archive Manager 不會申請儲存空間或網絡權限. 宿主只提供短生命週期唯讀描述符及固定插件 UID 的輸出交易, 插件不能自行選擇任意檔案系統路徑. Explorer Action v11 只在有界同步重試要求中傳遞密碼, 雙方立即移除及清理保留緩衝, 且從不持久化. Explorer Action v12 只增加工作階段級、有界的宿主批准同級卷目錄: 插件取得不透明 ID 而非路徑, 呼叫方 UID、檔案身份、大小、修改時間及生命週期均在使用前重複驗證. Explorer Action v13 只重新索引同一暫存來源, 並在完整替換索引準備完成前保留舊狀態. Explorer Action v14 只精確匹配 `.zip.001` 及 `.7z.001` 等有界複合後綴, 不會匹配任意 `.001` 檔案, 並重用 v12 目錄而不授予目錄或路徑存取能力. Android 及 Java 程式庫仍可能產生無法完全控制的短生命週期執行期副本, 因此密碼清理屬盡力而為, 並非絕對保證. 路徑穿越及危險名稱始終隔離, 輸出在發佈前完成驗證, 資源預算確認亦不會關閉結構安全檢查.

### Roadmap

其餘工作繼續以可勾選項目追蹤: 普通 ZIP 以外的可寫重建、復原或來源檔案刪除交易、無障礙檢查、其餘裝置與製作工具矩陣, 以及首次公開發布資料.

- [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/ROADMAP.md)

### 版本記錄

#### v2.7.0

_2026/08/27_

- `提示` 此版本需要配套的 AutoJs6 6.8.0 Explorer Action v14 建置 (版本代碼 5276 或以上)
- `新增` 現在可從 `.001` 首卷瀏覽、預覽及解壓完整的編號 `.zip.001` 與 `.7z.001` 卷組; 編號卷組保持唯讀
- `新增` Explorer Action v14 增加有界複合檔案名稱後綴匹配, 並重用綁定插件 UID 的 v12 同級卷來源, 不會把任意 `.001` 檔案識別為壓縮檔
- `修正` Android 7 會在進入 Zip4j 相容路徑前把宿主授權的編號 ZIP 卷組合成一個私有本機檔案, 不再把有效卷組誤報為損壞
- `改善` 伴隨卷編號限定在 `.002` 至 `.128`, 且已提供卷必須連續; reader 精確報告下一個缺失卷, 在具體化前後重複驗證卷身份, 且始終不公布壓縮檔內修改能力

#### v2.6.0

_2026/08/27_

- `提示` 此版本需要配套的 AutoJs6 6.8.0 Explorer Action v13 建置 (版本代碼 5276 或以上)
- `新增` 宿主原生檔案頁現在可從路徑列直接選擇 ZIP 檔案名稱編碼, 無需進入管理頁; 切換後保留目前內部目錄及仍然存在的已選項目
- `新增` Explorer Action v13 在同一唯讀工作階段重新索引暫存來源, 並以穩定項目 ID 恢復最深可用路徑及仍然存在的項目
- `改善` 替換索引只會在完整建立後發佈; 無效選擇、掃描失敗、進行中的預覽或解壓均保留舊索引, 已輸入密碼繼續只保留在可清理記憶體中

#### v2.5.0

_2026/08/27_

- `提示` 此版本需要配套的 AutoJs6 6.8.0 Explorer Action v12 建置 (版本代碼 5276 或以上)
- `新增` 完整的標準 `.z01 + .zip` 卷組及現代 WinRAR `partN.rar` 卷組現可在宿主原生檔案頁瀏覽、預覽及解壓; 分卷檔案仍保持唯讀
- `新增` Explorer Action v12 只向插件提供有界的宿主批准同級卷目錄, 每一卷均以不透明 ID 開啟為唯讀描述符, 不會暴露目錄或檔案系統路徑
- `修正` Android 7 或以上版本現可正確接受分卷 ZIP 最終卷的目錄中繼資料, 並跨所有已授權卷讀取項目內容, 不再把最終卷誤判為損壞
- `修正` RAR 分卷片段 CRC 不再與重組後的完整項目資料比較; 缺卷及開啟後發生變更的卷會返回穩定的類型化錯誤
- `改善` 卷數量、名稱、ID、開啟次數、呼叫方 UID、檔案身份及工作階段生命週期均受限制並重複驗證; 暫存中斷會刪除所有私有快取殘片

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
- 第三方軟件聲明: [THIRD_PARTY_NOTICES.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/THIRD_PARTY_NOTICES.md)
- Android Storage Access Framework: https://developer.android.com/guide/topics/providers/document-provider
