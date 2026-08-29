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

Archive Manager 直接在 AutoJs6 檔案管理器內運作, 不會另建一套檔案列表. 支援的檔案會沿用宿主列表、路徑列、主題、預覽器、多選、進度及目錄重新整理. 獨立管理頁只用於需要更完整表單的詳細格式資料及附加設定.

### 目前能力

- 直接在 AutoJs6 原生檔案清單中開啟 ZIP、7Z 及 TAR 系列壓縮檔，沿用主程式主題、深色模式及動態色彩。
- 路徑列同時顯示外部目錄、壓縮檔名稱及內部目錄；可按下層級跳轉，返回鍵會先返回內部上一級，再離開壓縮檔。
- 無需離開主程式原生壓縮檔頁, 即可從路徑列解壓縮當前內部目錄, 或進入選擇模式解壓縮已勾選的檔案及目錄; 任務顯示進度並可取消, 完成後父目錄會自動更新.
- 使用主程式現有預覽器開啟支援的文件、圖片、音訊及影片項目。
- 透過「解壓縮到...」將整個壓縮檔解壓縮到建議的同目錄資料夾, 或使用 Android 系統選擇器選擇其他資料夾; 現有的等價資料夾名稱會安全編號.
- 選擇「管理壓縮檔案...」可進入管理頁, 解壓縮全部、目前內部目錄或目前勾選內容, 亦可透過「加入檔案...」「加入資料夾...」「新增資料夾...」「重新命名...」及「刪除」修改一般單卷 ZIP 或符合條件的 TAR、TAR.GZ/TGZ、TAR.XZ/TXZ、TAR.BZ2/TBZ2 和 TAR.ZST/TZST; 「解壓縮到...」仍是整個壓縮檔的捷徑.
- ZIP 及 TAR 修改會在寫入前產生計劃, 重建至主程式持有的待提交輸出並完整讀回, 只有驗證通過後才原子取代原壓縮檔. 取消或失敗不會改變來源壓縮檔, 提交成功後 Explorer 會自動重新整理.
- 輸出名稱等價時可選擇「每次詢問」「略過」「覆寫」或「自動重新命名」；「套用至全部」可處理其後相容衝突，現有輸出資料夾一律自動編號並保持不變。
- 按目錄瀏覽、搜尋及排序壓縮檔內容。
- 只讀取目錄中繼資料便顯示清單，不會預先解壓縮全部內容。
- 一般壓縮檔優先透過主程式唯讀可 seek 描述符及獨立位置讀取通道直接瀏覽，無需複製整份內容；管道、不可 seek 或可寫輸入、Android 7，以及必須使用本機檔案的相容後端（目前為加密 ZIP）才回退到私人快取，並在關閉時清理。
- 解壓縮資源預算提供相容、嚴格及自訂模式；超出項目數、路徑、輸出大小或壓縮比預算的檔案仍可瀏覽，寫出前會列出預計空間與風險並要求單次確認。
- 解壓縮過程顯示項目與位元組進度、目前項目、傳輸速度及預計剩餘時間；取消或失敗會回復本次新建輸出根，目標提供程式拒絕刪除時會列出可能殘留的名稱及 URI。
- 無法安全寫出的上層穿越、絕對路徑、磁碟機路徑或控制字元名稱會進入路徑列可見的「危險路徑」唯讀目錄；內容仍可預覽，解壓縮整個壓縮檔前會明確要求略過這些項目，正常項目不受影響。
- 瀏覽、預覽及解壓縮未壓縮 TAR；符號連結、硬連結、裝置節點及稀疏項目只會列出，不會當作一般檔案寫出。
- 瀏覽、預覽及解壓縮 TAR.GZ/TGZ、TAR.XZ/TXZ、TAR.BZ2/TBZ2 與 TAR.ZST/TZST；沿用相同的內部路徑、特殊項目隔離及完整性檢查。
- 瀏覽、預覽及解壓縮一般或 solid 7Z，包括常見壓縮/篩選器鏈、內容加密及標頭加密輸入；缺少密碼或密碼錯誤時會顯示明確診斷。
- 根據實際 ZIP/7Z/TAR 結構確認格式，並統一控制預覽、解壓縮及建立能力；未支援的選項保持停用。
- 相容 Zip64、自解壓縮式前置資料、傳統檔名編碼及 Windows 路徑分隔符。
- 透過主程式有界授權的同級卷描述符瀏覽、預覽及解壓縮完整的標準 `.z01 + .zip` 卷組；可用預設或自訂 MiB 大小建立標準分卷 ZIP，並明確報告缺卷或卷變更。
- 瀏覽及解壓縮使用傳統 ZipCrypto 或 AES 的加密 ZIP；密碼錯誤可原位重試。建立 ZIP 時可選用 AES-256 密碼，檔案名稱仍然可見；建立加密 ZIP 前須再次輸入相同密碼確認。
- 自動偵測 ZIP 檔案名稱編碼不正確時可手動覆寫；瀏覽及解壓縮會重用同一選擇。
- 壓縮檔失敗時顯示格式、處理階段、穩定代碼及明確原因；偵錯版本可複製完整診斷資訊。
- 為一般檔案、資料夾及同一上層目錄的多選項目提供「壓縮...」動作。
- 同一上層目錄的多選項目可為每個項目個別建立壓縮檔案；表單會預覽輸出數量及衍生名稱，現有或重複名稱會自動編號且絕不覆寫。每個輸出獨立提交；如中途取消或失敗，介面會保留並報告已完成結果，同時阻止可能重複建立的整批重試。
- 可建立一般或標準分卷 ZIP，以及 7Z、TAR、TAR.GZ、TAR.XZ、TAR.BZ2 與 TAR.ZST；表單只顯示所選格式真正支援的壓縮等級及密碼選項。
- 先寫入同一目錄的暫存檔，再以原子方式提交；可選擇自動編號，或先嘗試精確名稱並在改用編號名稱前詢問，絕不覆寫現有檔案。名稱預留後會先掃描並固定有界來源清單，再開啟暫存輸出；表單分別顯示掃描、壓縮、驗證和提交狀態，以及檔案總數、已讀取位元組與大小不明資訊。最終名稱發布前會完整讀回仍處於隱藏狀態的輸出，並核對格式、項目、大小、CRC 及內容指紋。建立或驗證失敗會統一中止交易；若主程式無法確認暫存輸出已清理，表單會顯示預定路徑並停止重試。

### 目前支援

目前版本識別以下可瀏覽及解壓縮的副檔名:

```text
zip, zip.001, jar, aar, war, 7z, 7z.001, rar, tar, tar.gz, tgz, tar.xz, txz, tar.bz2, tbz2, tar.zst, tzst
```

目前版本可建立以下格式:

```text
zip, 7z, tar, tar.gz, tar.xz, tar.bz2, tar.zst
```

> 原生整合需要配套的 AutoJs6 6.8.0 Explorer Action v20 建置 (版本代碼 5276 或以上). RAR 及分卷檔案明確保持唯讀. 普通單卷 ZIP、未加密且非 solid 並位於解碼資源預算內的一般單卷 7Z，以及只含安全普通檔案和目錄的 TAR 系列壓縮檔可以修改; 加密、solid、分卷、危險路徑、不支援方法或超出預算的 7Z 保持唯讀. 標準分卷 ZIP 應從最終 `.zip` 開啟, 現代 WinRAR 卷組應從首個 `partN.rar` 開啟, 編號 ZIP 或 7Z 應從 `.001` 開啟, 且所有必需同級卷須位於同一目錄. 建立時加密檔案名稱, 以及修改 JAR/AAR/WAR 或 RAR 仍不是目前能力.

### 使用方法

1. 安裝外掛程式，並在 AutoJs6 外掛程式中心啟用。
2. 在檔案管理器開啟 ZIP、JAR、AAR、WAR、7Z 或 TAR 系列壓縮檔的選單。
3. 選擇「開啟壓縮檔」，然後在主程式檔案清單進入目錄、搜尋或使用路徑列跳轉。
4. 如要解壓縮整個壓縮檔, 請從檔案選單選擇「解壓縮到...」. 使用建議的目前資料夾, 或透過 Android 系統選擇器選擇其他資料夾, 然後確認準確的輸出路徑.
5. 如要解壓縮當前內部目錄, 請點選路徑列右側的解壓縮按鈕. 如要解壓縮指定項目, 請長按項目進入選擇模式, 勾選檔案或目錄後點選底部「解壓縮」. 需要密碼、編碼修正、危險路徑確認、衝突策略或其他目標目錄時, 請使用「管理壓縮檔案...」或「解壓縮到...」.
6. 如要修改一般單卷 ZIP 或符合條件的 TAR、TAR.GZ/TGZ、TAR.XZ/TXZ、TAR.BZ2/TBZ2 及 TAR.ZST/TZST, 請選擇「管理壓縮檔案...」, 再使用「加入檔案...」「加入資料夾...」匯入完整目錄樹、「新增資料夾...」建立空資料夾、「重新命名...」或「刪除」. 請等待重建、驗證及成功提示完成後再離開頁面.
7. 在管理頁解壓縮前可選擇如何處理等價輸出名稱。「每次詢問」可將一次略過、覆寫或自動重新命名決定套用至所有其後相容衝突。
8. 如要建立壓縮檔，請從一般檔案或資料夾選單選擇「壓縮...」；亦可先在同一目錄多選項目，再使用底部的「壓縮...」動作。如要為每個項目分別建立壓縮檔案，請開啟「個別壓縮每個檔案和資料夾」，核對輸出預覽後再建立；此模式固定以自動編號安全處理名稱衝突。建立 ZIP 時可選擇「無」、常用 MiB 預設或 1 至 4096 MiB 的整數自訂值；輸出超過所選大小時由 `.z01`、`.z02` 等編號卷及最終 `.zip` 組成，較小的輸出仍為單一 `.zip`。

### 權限與資料

Archive Manager 不會申請儲存空間或網絡權限. 宿主只提供短生命週期唯讀描述符及固定插件 UID 的輸出交易, 插件不能自行選擇任意檔案系統路徑. Explorer Action v11 只在有界同步重試要求中傳遞密碼, 雙方立即移除及清理保留緩衝, 且從不持久化. Explorer Action v12 只增加工作階段級、有界的宿主批准同級卷目錄: 插件取得不透明 ID 而非路徑, 呼叫方 UID、檔案身份、大小、修改時間及生命週期均在使用前重複驗證. Explorer Action v13 只重新索引同一暫存來源, 並在完整替換索引準備完成前保留舊狀態. Explorer Action v14 只精確匹配 `.zip.001` 及 `.7z.001` 等有界複合後綴, 不會匹配任意 `.001` 檔案, 並重用 v12 目錄而不授予目錄或路徑存取能力. Explorer Action v17 只在普通主要操作未匹配時增加無匹配器的唯讀溢出覆核入口; 它只在使用者點按後重用一次既有壓縮檔案工作階段, 不增加路徑、目錄或寫入權限. Android 及 Java 程式庫仍可能產生無法完全控制的短生命週期執行期副本, 因此密碼清理屬盡力而為, 並非絕對保證. 路徑穿越及危險名稱始終隔離, 輸出在發佈前完成驗證, 資源預算確認亦不會關閉結構安全檢查.

Explorer Action v15 只把同一工作階段中已驗證的新檔案輸出組成最多 128 項的可恢復批次. Explorer Action v16 只有在插件提交完整有序來源選擇及所有已提交輸出交易的精確證明後, 才讓宿主重新核對來源與輸出身份並把來源項目移入回收筒. 宿主先同步恢復副本並持久記錄條目, 再移除來源資料; 插件不會取得任意路徑或直接刪除能力. Binder 回應遺失時只查詢同一幂等終態, 不會盲目重試.

Explorer Action v18 只在宿主私有持久儲存中保留上一版壓縮檔案, 向插件返回不透明歷史 ID 而非備份路徑. 只有父目錄及目標仍精確匹配本次提交結果時才可恢復一次. 外部改寫會令歷史失效; 中斷的恢復證據會保留, 並在宿主處理前阻止再次取代同一目標. 普通歷史受保留時間、數量、總位元組及剩餘空間限制, v8-v17 工作階段不會建立替換備份. Explorer Action v19 只為能力允許的既有項目傳遞刪除或重新命名所需的不透明 ID 及安全葉名稱. Explorer Action v20 只為明確選定的輸入根提供凍結授權: 插件取得有界的不透明節點、元資料及一次性唯讀描述符, 不會取得來源路徑、URI、未選同級項目或通用儲存存取權. 宿主在提交前重新驗證完整快照, 任一輸入變更或失敗都會中止整個壓縮檔取代.

### Roadmap

宿主原生建立、加入、刪除及重新命名已完成, 且沒有改變普通檔案管理器佈局. 其餘可勾選工作包括回收筒群組復原與歷史、其餘裝置矩陣, 以及首次公開發布資料.

- [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/ROADMAP.md)

### 版本記錄

#### v2.20.0

_2026/08/30_

- `提示` 本版本需要配套使用 Explorer Action v20 的 AutoJs6 6.8.0 建置 (版本代碼 5276 或以上)
- `新增` 可寫壓縮檔案的宿主原生頁面現可直接建立空目錄, 並從檔案及完整目錄樹的明確混合選擇中加入內容; 空目錄會保留, 同名目錄根會安全編號
- `修正` 提交前會重新驗證凍結的輸入快照; 取消、來源內容變更、所選目錄樹內出現等價名稱歧義或直接檔案衝突時, 整批加入都會失敗並保留原壓縮檔案
- `改善` Explorer Action v20 只為選定輸入公開有界的不透明節點、元資料及一次性唯讀描述符; 不授予路徑、URI、未選同級項目或通用儲存存取權, 且不改變普通檔案管理器佈局

#### v2.19.0

_2026/08/29_

- `提示` 此版本需要配套的 AutoJs6 6.8.0 Explorer Action v19 建置 (版本代碼 5276 或以上)
- `新增` 可寫壓縮檔案中的檔案及資料夾現在可直接在 AutoJs6 原生壓縮檔案列表中重新命名或刪除, 並支援多選刪除; 對話框、進度、路徑列及選擇恢復均重用宿主現有框架
- `修正` 刪除及重新命名入口現在同時受工作階段及逐項能力約束; 危險路徑、缺卷、RAR、分卷檔案及其他唯讀變體不會錯誤宣告可用操作
- `改善` Explorer Action v19 只傳遞有界的不透明項目 ID 及安全葉名稱, 透過宿主持有的待提交輸出完成重建、完整讀回、原子取代及原位重新索引; 沒有改動普通檔案管理器的佈局或既有視覺樣式

#### v2.18.0

_2026/08/29_

- `提示` 本版本仍需配套使用 Explorer Action v18 的 AutoJs6 6.8.0 建置 (版本代碼 5276 或以上)
- `改善` 安全普通 7Z 及全部可寫 TAR 系列格式現共用格式中立的完整重建計劃層; 每個後端明確提供能力及保留項目驗證, 消除舊 TAR 命名及耦合, 不改變支援格式、輸出設定或唯讀邊界

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
