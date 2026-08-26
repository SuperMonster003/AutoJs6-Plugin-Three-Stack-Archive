<div align="center">
  <p>
    <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="Archive Manager" width="128" />
  </p>

  <h1>Archive Manager</h1>

  <p>用於瀏覽、解壓縮與建立支援的壓縮檔, 並以交易方式修改一般 ZIP 的 AutoJs6 檔案管理器外掛程式</p>

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

Archive Manager 把壓縮檔的瀏覽、解壓縮、建立與管理功能帶入 AutoJs6 檔案管理器. 目前版本透過主程式原生清單與路徑列瀏覽 ZIP、7Z 及 TAR 系列壓縮檔, 預覽支援的項目, 解壓縮所選範圍, 從單一項目或同一上層目錄多選建立支援格式, 並可在管理頁以交易方式修改一般單一分割檔 ZIP.

### 目前功能

- 直接在 AutoJs6 原生檔案清單中開啟 ZIP、7Z 與 TAR 系列壓縮檔，沿用主程式主題、深色模式與動態色彩。
- 路徑列同時顯示外部目錄、壓縮檔名稱與內部目錄；可點選層級跳轉，返回鍵會先返回內部上一層，再離開壓縮檔。
- 無需離開主程式原生壓縮檔頁面, 即可從路徑列解壓縮目前內部目錄, 或進入選取模式解壓縮已勾選的檔案與目錄; 作業顯示進度並可取消, 完成後上層目錄會自動更新.
- 使用主程式現有預覽器開啟支援的文件、圖片、音訊與影片項目。
- 透過「解壓縮到...」將整個壓縮檔解壓縮到建議的同目錄資料夾, 或使用 Android 系統選擇器選擇其他資料夾; 現有的等價資料夾名稱會安全編號.
- 選擇「管理壓縮檔案...」可進入管理頁, 解壓縮全部、目前內部目錄或目前勾選內容, 也可透過「加入檔案...」「加入資料夾...」「新增資料夾...」「重新命名...」與「刪除」修改一般單一分割檔 ZIP; 「解壓縮到...」仍是整個壓縮檔的捷徑.
- ZIP 修改會在寫入前產生計畫, 重建至主程式持有的待提交輸出並完整讀回, 只有驗證通過後才原子取代原壓縮檔. 取消或失敗不會變更來源壓縮檔, 提交成功後 Explorer 會自動重新整理.
- 輸出名稱等價時可選擇「每次詢問」「略過」「覆寫」或「自動重新命名」；「套用至全部」可處理後續相容衝突，現有輸出資料夾一律自動編號並保持不變。
- 依目錄瀏覽、搜尋及排序壓縮檔內容。
- 只讀取目錄中繼資料即可顯示清單，不會預先解壓縮全部內容。
- 一般壓縮檔優先透過主程式唯讀可 seek 描述符及獨立位置讀取通道直接瀏覽，無需複製整份內容；管道、不可 seek 或可寫輸入、Android 7，以及必須使用本機檔案的相容後端（目前為加密 ZIP）才回退到私有快取，並在關閉時清理。
- 解壓縮資源預算提供相容、嚴格與自訂模式；超過項目數、路徑、輸出大小或壓縮比預算的檔案仍可瀏覽，寫出前會列出預估空間與風險並要求單次確認。
- 解壓縮過程顯示項目與位元組進度、目前項目、傳輸速度及預估剩餘時間；取消或失敗會回復本次新建輸出根，目標提供程式拒絕刪除時會列出可能殘留的名稱與 URI。
- 無法安全寫出的上層穿越、絕對路徑、磁碟機路徑或控制字元名稱會進入路徑列可見的「危險路徑」唯讀目錄；內容仍可預覽，解壓縮整個壓縮檔前會明確要求略過這些項目，正常項目不受影響。
- 瀏覽、預覽與解壓縮未壓縮 TAR；符號連結、硬連結、裝置節點與稀疏項目只會列出，不會當作一般檔案寫出。
- 瀏覽、預覽與解壓縮 TAR.GZ/TGZ、TAR.XZ/TXZ、TAR.BZ2/TBZ2 及 TAR.ZST/TZST；沿用相同的內部路徑、特殊項目隔離與完整性檢查。
- 瀏覽、預覽與解壓縮一般或 solid 7Z，包括常見壓縮/篩選器鏈、內容加密與標頭加密輸入；缺少密碼或密碼錯誤時會顯示明確診斷。
- 根據實際 ZIP/7Z/TAR 結構確認格式，並統一控制預覽、解壓縮與建立能力；未支援的選項保持停用。
- 相容 Zip64、自解壓縮式前置資料、傳統檔名編碼與 Windows 路徑分隔符號。
- 辨識標準 `.z01 + .zip` 分割 ZIP 結構，並列出所需的前序分割檔名稱，不再將單獨開啟的最後一個分割檔誤報為損壞；可用預設或自訂 MiB 大小建立標準分割 ZIP，讀取現有分割檔仍等待主程式提供同層卷存取介面。
- 瀏覽與解壓縮使用傳統 ZipCrypto 或 AES 的加密 ZIP；密碼錯誤可原地重試。建立 ZIP 時可選用 AES-256 密碼，檔名仍然可見；建立加密 ZIP 前須再次輸入相同密碼確認。
- 自動偵測 ZIP 檔名編碼不正確時可手動覆寫；瀏覽與解壓縮會重用同一選擇。
- 壓縮檔失敗時顯示格式、處理階段、穩定代碼與明確原因；偵錯版本可複製完整診斷資訊。
- 為一般檔案、資料夾與同一上層目錄的多選項目提供「壓縮...」動作。
- 同一上層目錄的多選項目可為每個項目個別建立壓縮檔案；表單會預覽輸出數量與衍生名稱，現有或重複名稱會自動編號且絕不覆寫。每個輸出獨立提交；若中途取消或失敗，介面會保留並回報已完成結果，同時阻止可能重複建立的整批重試。
- 可建立一般或標準分割 ZIP，以及 7Z、TAR、TAR.GZ、TAR.XZ、TAR.BZ2 與 TAR.ZST；表單只顯示所選格式真正支援的壓縮等級與密碼選項。
- 先寫入同一目錄的暫存檔，再以原子方式提交；可選擇自動編號，或先嘗試精確名稱並在改用編號名稱前詢問，絕不覆寫現有檔案。名稱預留後會先掃描並固定有界來源清單，再開啟暫存輸出；表單分別顯示掃描、壓縮、驗證和提交狀態，以及檔案總數、已讀取位元組與大小不明資訊。最終名稱發布前會完整讀回仍處於隱藏狀態的輸出，並核對格式、項目、大小、CRC 與內容指紋。建立或驗證失敗會統一中止交易；若主程式無法確認暫存輸出已清理，表單會顯示預定路徑並停止重試。

### 目前支援

目前版本識別以下可瀏覽與解壓縮的副檔名:

```text
zip, jar, aar, war, 7z, tar, tar.gz, tgz, tar.xz, txz, tar.bz2, tbz2, tar.zst, tzst
```

目前版本可建立以下格式:

```text
zip, 7z, tar, tar.gz, tar.xz, tar.bz2, tar.zst
```

> 完整整合使用 Explorer Action v10 執行主程式原生的目前目錄/選取項目解壓縮, v9 執行已驗證的目錄輸出與復原, v8 執行已驗證的目標取代, v7 執行提交前輸出驗證, v6 執行原生瀏覽與項目預覽, v4 檔案工作階段執行壓縮; 需要 AutoJs6 版本代碼 5276 或以上. 目前僅一般單一分割檔 `.zip` 支援修改. 讀取現有分割檔、主程式原生密碼/檔名編碼互動、建立時加密檔名、壓縮後刪除來源項目, 以及修改 JAR/AAR/WAR、7Z 或 TAR 系列壓縮檔尚未發布; 請以 Roadmap 的勾選狀態為準.

### 使用方式

1. 安裝外掛程式，並在 AutoJs6 外掛程式中心啟用。
2. 在檔案管理器開啟 ZIP、JAR、AAR、WAR、7Z 或 TAR 系列壓縮檔的選單。
3. 選擇「開啟壓縮檔」，然後在主程式檔案清單進入目錄、搜尋或使用路徑列跳轉。
4. 若要解壓縮整個壓縮檔, 請從檔案選單選擇「解壓縮到...」. 使用建議的目前資料夾, 或透過 Android 系統選擇器選擇其他資料夾, 然後確認正確的輸出路徑.
5. 若要解壓縮目前內部目錄, 請點選路徑列右側的解壓縮按鈕. 若要解壓縮指定項目, 請長按項目進入選取模式, 勾選檔案或目錄後點選底部「解壓縮」. 需要密碼、編碼修正、危險路徑確認、衝突策略或其他目標目錄時, 請使用「管理壓縮檔案...」或「解壓縮到...」.
6. 若要修改一般單一分割檔 ZIP, 請選擇「管理壓縮檔案...」, 再使用「加入檔案...」「加入資料夾...」匯入完整目錄樹、「新增資料夾...」建立空資料夾、「重新命名...」或「刪除」. 請等待重建、驗證與成功提示完成後再離開頁面.
7. 在管理頁解壓縮前可選擇如何處理等價輸出名稱。「每次詢問」可將一次略過、覆寫或自動重新命名決定套用至所有後續相容衝突。
8. 若要建立壓縮檔，請從一般檔案或資料夾選單選擇「壓縮...」；也可先在同一目錄多選項目，再使用底部的「壓縮...」動作。若要為每個項目分別建立壓縮檔案，請開啟「個別壓縮每個檔案和資料夾」，核對輸出預覽後再建立；此模式固定以自動編號安全處理名稱衝突。建立 ZIP 時可選擇「無」、常用 MiB 預設或 1 至 4096 MiB 的整數自訂值；輸出超過所選大小時由 `.z01`、`.z02` 等編號卷與最終 `.zip` 組成，較小的輸出仍為單一 `.zip`。

### 權限與資料

外掛程式不要求儲存空間或網路權限。原生瀏覽優先租用主程式的唯讀可 seek 描述符，並以獨立位置讀取通道直接存取一般壓縮檔；管道、不可 seek 或可寫輸入、Android 7，以及必須使用本機檔案的相容後端（目前為加密 ZIP）才回退到私有快取。描述符租約或快取會於頁面關閉、解除連線、失敗或過期後清理。同目錄解壓縮只透過綁定外掛程式 UID 且由主程式持有的目錄交易寫出; 選擇其他資料夾時只使用 Android 系統選擇器授予的目錄權限。建立壓縮檔透過綁定外掛程式 UID 的主程式檔案工作階段分頁讀取目標，而且只能在目前上層目錄建立交易式輸出。密碼只保存在可清零的記憶體緩衝區，不會寫入 Bundle、偏好設定、日誌或診斷資訊，並於取代、工作結束或頁面銷毀時清除。固定 4 GiB 輸入上限與瀏覽階段大小/壓縮比門檻已移除；路徑隔離、來源大小核對、輸出交易與失敗清理仍然保留。

資源預算只決定何時警告或要求確認，不會放寬結構安全。確認後也只把本次解壓縮的即時位元組與壓縮比邊界擴大到所選項目的宣告值；未宣告的額外增長、來源檔案變更、大小或 CRC 不一致仍會中止並清理輸出。

建立分割 ZIP 時，外掛程式先在私有快取組裝並完整驗證卷組，再把各卷複製到主程式隱藏的待提交輸出並逐位元組比對。全部通過後才刪除私有暫存副本，並按編號卷在前、最終 `.zip` 在後的順序發布；現有名稱絕不會被覆寫。目前主程式沒有整組原子提交能力，因此任何部分提交結果都會明確回報，不會偽裝為完整壓縮檔。

危險名稱只會作為不透明 ID 下的唯讀顯示資訊，不會成為輸出路徑。

### Roadmap

一般 ZIP 以外的可寫格式、分割檔讀取、主程式原生密碼/檔名編碼互動、建立時加密檔名、壓縮後刪除來源項目、復原及剩餘失敗恢復的任務及驗收條件均記錄於 Roadmap. 未勾選項目不是目前功能.

- [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/ROADMAP.md)

### 版本記錄

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

#### v2.1.0

_2026/08/25_

- `提示` 目前僅一般單一分割檔 `.zip` 支援修改. JAR/AAR/WAR, 分割 ZIP, 7Z 與 TAR 系列壓縮檔仍為唯讀; 重建會正規化壓縮檔註解, 非必要 extra metadata 與 Unix 權限屬性
- `新增` 為所選 ZIP 提供「管理壓縮檔...」, 可在管理頁新增檔案, 建立空目錄, 重新命名與刪除, 並正確處理目錄子樹的重新命名及刪除
- `新增` Explorer Action v8 將重建結果寫入主程式持有的待提交輸出, 完整讀回驗證後才原子取代原壓縮檔, 並自動重新整理 Explorer 項目
- `改善` 每次修改都會先產生不可變計畫, 在提交取代輸出前檢查危險路徑, 重複或等價名稱, 檔案/目錄衝突, 不支援保留的項目及來源壓縮檔變更
- `改善` ZIP 重建會保留 Stored/Deflate 項目內容, 可用時間戳與支援的 ZipCrypto/AES 加密; 取消或任何驗證失敗都會中止待提交輸出, 原壓縮檔保持不變

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
