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

Archive Manager 直接在 AutoJs6 檔案管理器內運作, 不會另建一套檔案清單. 支援的檔案會沿用宿主清單、路徑列、主題、預覽器、多選、進度與目錄重新整理. 獨立管理頁只用於需要更完整表單的詳細格式資訊與附加設定.

### 目前功能

- 在 AutoJs6 原生檔案清單中瀏覽 ZIP/JAR/AAR/WAR、7Z、RAR4/RAR5 與 TAR 系列檔案, 並沿用內部路徑列、搜尋、排序與返回導覽.
- 檔案名稱未符合支援的壓縮檔案時, 提供明確的「作為壓縮檔案開啟...」複核, 並在路徑列標示偵測到的實際格式; TAR 複合後綴依完整名稱精確比對, 不會把一般 `.gz`、`.xz`、`.bz2` 或 `.zst` 壓縮串流顯示為壓縮檔案.
- 直接使用宿主現有的文件、圖片、音訊與影片預覽器讀取可用項目, 無需先解壓縮整個檔案.
- 可解壓縮整個檔案、目前內部目錄或已選項目, 支援進度、取消、安全衝突編號、發佈前驗證與提交前復原.
- 加密 ZIP、7Z 與 RAR 可在首次開啟或解壓縮時使用宿主原生密碼框; 密碼錯誤後可在原位重試, 不會失去目前檔案路徑.
- 可從宿主路徑列直接修正 ZIP 檔名編碼; 同一唯讀工作階段在原位重建索引, 並盡量保留目前內部路徑與已選項目.
- 可透過宿主有界授權的同層卷描述符瀏覽、預覽與解壓縮完整的標準 `.z01 + .zip`、現代 WinRAR `partN.rar`、編號 `.zip.001` 與編號 `.7z.001` 卷組; 缺卷或卷變更會回傳明確錯誤.
- 可由單一項目或同父層多選建立 ZIP、7Z、TAR、TAR.GZ、TAR.XZ、TAR.BZ2 與 TAR.ZST; ZIP 另支援 AES-256 與標準分卷, 多選也可為每項分別建立檔案.
- 分割 ZIP 與「分別壓縮每個項目」會在所有實體輸出逐項驗證後透過一個可復原批次統一發佈; 失敗或宿主重新啟動不會把一般部分結果當作成功.
- 普通單卷 ZIP、安全普通 7Z，以及符合條件的 TAR、TAR.GZ/TGZ、TAR.XZ/TXZ、TAR.BZ2/TBZ2 與 TAR.ZST/TZST 可透過已驗證重建加入檔案、匯入完整目錄樹、建立空目錄、重新命名與刪除; 只有完整讀回通過後才以原子方式取代來源檔案.
- 可使用壓縮檔專屬的路徑列操作加入檔案或完整目錄樹的明確混合選取, 或建立空目錄; 同一原生清單中也可直接重新命名項目或多選刪除. 操作重用現有對話框、進度與路徑/選擇還原, 只有工作階段與相關項目允許時才顯示入口.
- 可寫壓縮檔修改成功後, 可從成功提示或管理頁選單還原上一版本; 宿主在有界保留期內只提供一次還原, 目標被其他應用程式改寫後會拒絕覆蓋.
- 管理頁集中顯示實際格式、內容統計、可用修改操作與精確的唯讀原因; 每次修改都會在預留輸出前列出實際工作量、完整重建與輔助中繼資料影響, 取消不會建立待提交輸出.
- 危險檔案名稱保持唯讀隔離, 寫出前仍執行結構與資源檢查; 對可 seek 的宿主描述符優先直接讀取, 避免不必要的整份複製.
- 可選擇只在所有實體輸出驗證並提交後, 將完整來源選擇移入宿主回收桶; 此選項預設關閉, 來源身分變更或輸出證明不完整都會在移除來源資料前中止.

### 目前支援

目前版本識別以下可瀏覽與解壓縮的副檔名:

```text
zip, zip.001, jar, aar, war, 7z, 7z.001, rar, tar, tar.gz, tgz, tar.xz, txz, tar.bz2, tbz2, tar.zst, tzst
```

目前版本可建立以下格式:

```text
zip, 7z, tar, tar.gz, tar.xz, tar.bz2, tar.zst
```

> 原生整合需要搭配 AutoJs6 6.8.0 Explorer Action v20 建置 (版本代碼 5276 或更新版本). RAR 與分割檔明確維持唯讀. 普通單卷 ZIP、未加密且非 solid 並位於解碼資源預算內的一般單卷 7Z，以及只含安全普通檔案和目錄的 TAR 系列壓縮檔可以修改; 加密、solid、分割、危險路徑、不支援方法或超出預算的 7Z 維持唯讀. 標準分割 ZIP 應從最終 `.zip` 開啟, 現代 WinRAR 卷組應從首個 `partN.rar` 開啟, 編號 ZIP 或 7Z 應從 `.001` 開啟, 且所有必要同層卷須位於同一目錄. 建立時加密檔名, 以及修改 JAR/AAR/WAR 或 RAR 仍不是目前功能.

### 使用方式

1. 安裝 Archive Manager, 並在 AutoJs6 外掛中心啟用.
2. 點選檔案的主要操作, 或從選單選擇「開啟壓縮檔案」. 名稱未被識別時, 可選擇「作為壓縮檔案開啟...」. 接著可像普通目錄一樣使用宿主路徑列; 名稱與實際格式不符時會標示偵測結果.
3. 使用路徑列右側操作解壓縮目前內部目錄, 或透過檔名編碼操作修正 ZIP 名稱; 長按項目可多選解壓縮; 檔案選單的「解壓縮到...」用於解壓縮整個檔案. 需要密碼時宿主會自動提示.
4. 對檔案或資料夾選擇「壓縮...」; 也可在同一目錄多選後使用底部操作列的「壓縮...」. 如需成功後清理來源項目, 請明確開啟預設關閉的「壓縮完成後將來源項目移入回收桶」.
5. 在可寫壓縮檔案的原生頁面中, 使用路徑列的「加入檔案或資料夾」與「新建資料夾」, 也可直接重新命名項目或多選刪除; 詳細格式資訊與附加設定位於「管理壓縮檔案...」.

### 權限與資料

Archive Manager 不會申請儲存空間或網路權限. 宿主只提供短生命週期唯讀描述符與固定外掛 UID 的輸出交易, 外掛不能自行選擇任意檔案系統路徑. Explorer Action v11 只在有界同步重試要求中傳遞密碼, 雙方立即移除並清理保留緩衝, 且從不持久化. Explorer Action v12 只增加工作階段級、有界的宿主核准同層卷目錄: 外掛取得不透明 ID 而非路徑, 呼叫端 UID、檔案身分、大小、修改時間與生命週期均在使用前重複驗證. Explorer Action v13 只重新索引同一暫存來源, 並在完整替換索引準備完成前保留舊狀態. Explorer Action v14 只精確比對 `.zip.001` 與 `.7z.001` 等有界複合後綴, 不會比對任意 `.001` 檔案, 且重用 v12 目錄而不授予目錄或路徑存取能力. Explorer Action v17 只在一般主要操作未比對時增加無比對器的唯讀溢出複核入口; 它只在使用者點選後重用一次既有壓縮檔案工作階段, 不增加路徑、目錄或寫入權限. Android 與 Java 程式庫仍可能產生無法完全控制的短生命週期執行期副本, 因此密碼清理屬盡力而為, 並非絕對保證. 路徑穿越與危險名稱始終隔離, 輸出在發佈前完成驗證, 資源預算確認也不會關閉結構安全檢查.

Explorer Action v15 只把同一工作階段中已驗證的新檔案輸出組成最多 128 項的可復原批次. Explorer Action v16 只有在外掛提交完整有序來源選擇與所有已提交輸出交易的精確證明後, 才讓宿主重新核對來源與輸出身分並將來源項目移入回收桶. 宿主先同步復原副本並持久記錄項目, 再移除來源資料; 外掛不會取得任意路徑或直接刪除能力. Binder 回應遺失時只查詢同一冪等終態, 不會盲目重試.

Explorer Action v18 只在宿主私有持久儲存中保留上一版壓縮檔案, 向外掛回傳不透明歷史 ID 而非備份路徑. 只有父目錄與目標仍精確符合本次提交結果時才可還原一次. 外部改寫會使歷史失效; 中斷的復原證據會保留, 並在宿主處理前阻止再次取代同一目標. 一般歷史受保留時間、數量、總位元組與剩餘空間限制, v8-v17 工作階段不會建立替換備份. Explorer Action v19 只為能力允許的既有項目傳遞刪除或重新命名所需的不透明 ID 與安全葉名稱. Explorer Action v20 只為明確選定的輸入根提供凍結授權: 外掛取得有界的不透明節點、詮釋資料與一次性唯讀描述符, 不會取得來源路徑、URI、未選同層項目或通用儲存存取權. 宿主在提交前重新驗證完整快照, 任一輸入變更或失敗都會中止整個壓縮檔取代.

### Roadmap

宿主原生建立、加入、刪除與重新命名已完成, 且沒有改變普通檔案管理器版面. 其餘可勾選工作包括回收桶群組復原與歷史、其餘裝置矩陣, 以及首次公開發布資料.

- [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/ROADMAP.md)

### 版本記錄

#### v2.21.0

_2026/08/30_

- `提示` 本版本需要搭配 Explorer Action v21 的 AutoJs6 6.8.0 建置 (版本代碼 5276 或以上)
- `新增` 壓縮完成後現在會保留完成頁面與來源項目還原記錄; 經使用者明確選擇移入垃圾桶的來源項目可在 Activity 或主機工作階段重建後還原
- `修正` 還原來源項目絕不覆寫既有名稱, 也不會刪除已建立的壓縮檔; 衝突、部分還原或中斷還原會保留復原證據並回報逐項結果
- `改善` 有界持久記錄由主機持有且只公開不透明詮釋資料; 還原介面只位於外掛壓縮頁面, 一般檔案管理器版面維持不變

#### v2.20.0

_2026/08/30_

- `提示` 本版本需要搭配 Explorer Action v20 的 AutoJs6 6.8.0 建置 (版本代碼 5276 或以上)
- `新增` 可寫壓縮檔的宿主原生頁面現在可直接建立空目錄, 並從檔案與完整目錄樹的明確混合選取中加入內容; 空目錄會保留, 同名目錄根會安全編號
- `修正` 提交前會重新驗證凍結的輸入快照; 取消、來源內容變更、所選目錄樹內出現等價名稱歧義或直接檔案衝突時, 整批加入都會失敗並保留原壓縮檔
- `改善` Explorer Action v20 只為選定輸入公開有界的不透明節點、詮釋資料與一次性唯讀描述符; 不授予路徑、URI、未選同層項目或通用儲存存取權, 且不改變普通檔案管理器版面

#### v2.19.0

_2026/08/29_

- `提示` 此版本需要搭配 AutoJs6 6.8.0 Explorer Action v19 建置 (版本代碼 5276 或更新版本)
- `新增` 可寫壓縮檔案中的檔案與資料夾現在可直接在 AutoJs6 原生壓縮檔案清單中重新命名或刪除, 並支援多選刪除; 對話框、進度、路徑列與選擇還原均重用宿主現有框架
- `修正` 刪除與重新命名入口現在同時受工作階段與逐項能力約束; 危險路徑、缺卷、RAR、分割檔案及其他唯讀變體不會錯誤宣告可用操作
- `改善` Explorer Action v19 只傳遞有界的不透明項目 ID 與安全葉名稱, 透過宿主持有的待提交輸出完成重建、完整讀回、原子取代與原位重新索引; 沒有改動普通檔案管理器的版面或既有視覺樣式

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
