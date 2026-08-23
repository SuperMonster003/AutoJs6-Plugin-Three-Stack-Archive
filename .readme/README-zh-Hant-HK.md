<div align="center">
  <p>
    <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="Archive Manager" width="128" />
  </p>

  <h1>Archive Manager</h1>

  <p>用於瀏覽、解壓縮及建立 ZIP、7Z 與 TAR 系列壓縮檔的 AutoJs6 檔案管理器外掛程式</p>

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

壓縮檔管理器把 ZIP、7Z 與 TAR 系列壓縮檔的瀏覽、解壓縮及建立功能帶入 AutoJs6 檔案管理器。目前版本可在主程式原生檔案清單中瀏覽壓縮檔、顯示外部及內部路徑、預覽支援的項目，並可把單一項目或同一上層目錄多選建立為支援的格式；按項目解壓縮及檔案內修改會按 Roadmap 推進。

### 目前能力

- 直接在 AutoJs6 原生檔案清單中開啟 ZIP、7Z 及 TAR 系列壓縮檔，沿用主程式主題、深色模式及動態色彩。
- 路徑列同時顯示外部目錄、壓縮檔名稱及內部目錄；可按下層級跳轉，返回鍵會先返回內部上一級，再離開壓縮檔。
- 使用主程式現有預覽器開啟支援的文件、圖片、音訊及影片項目。
- 透過「解壓縮到...」捷徑直接解壓縮整個壓縮檔，無需先進入瀏覽頁面。
- 按目錄瀏覽、搜尋及排序壓縮檔內容。
- 只讀取目錄中繼資料便顯示清單，不會預先解壓縮全部內容。
- 一般壓縮檔優先透過主程式唯讀可 seek 描述符及獨立位置讀取通道直接瀏覽，無需複製整份內容；管道、不可 seek 或可寫輸入、Android 7，以及必須使用本機檔案的相容後端（目前為加密 ZIP）才回退到私人快取，並在關閉時清理。
- 解壓縮資源預算提供相容、嚴格及自訂模式；超出項目數、路徑、輸出大小或壓縮比預算的檔案仍可瀏覽，寫出前會列出預計空間與風險並要求單次確認。
- 無法安全寫出的上層穿越、絕對路徑、磁碟機路徑或控制字元名稱會進入路徑列可見的「危險路徑」唯讀目錄；內容仍可預覽，解壓縮整個壓縮檔前會明確要求略過這些項目，正常項目不受影響。
- 瀏覽、預覽及解壓縮未壓縮 TAR；符號連結、硬連結、裝置節點及稀疏項目只會列出，不會當作一般檔案寫出。
- 瀏覽、預覽及解壓縮 TAR.GZ/TGZ、TAR.XZ/TXZ、TAR.BZ2/TBZ2 與 TAR.ZST/TZST；沿用相同的內部路徑、特殊項目隔離及完整性檢查。
- 瀏覽、預覽及解壓縮一般或 solid 7Z，包括常見壓縮/篩選器鏈、內容加密及標頭加密輸入；缺少密碼或密碼錯誤時會顯示明確診斷。
- 根據實際 ZIP/7Z/TAR 結構確認格式，並統一控制預覽、解壓縮及建立能力；未支援的選項保持停用。
- 相容 Zip64、自解壓縮式前置資料、傳統檔名編碼及 Windows 路徑分隔符。
- 識別標準 `.z01 + .zip` 分卷 ZIP 結構，並列出所需的前序分卷名稱，不再將單獨開啟的最後一卷誤報為損壞；分卷讀取及建立尚未開放。
- 瀏覽及解壓縮使用傳統 ZipCrypto 或 AES 的加密 ZIP；密碼錯誤可原位重試。建立 ZIP 時可選用 AES-256 密碼，檔案名稱仍然可見；建立加密 ZIP 前須再次輸入相同密碼確認。
- 自動偵測 ZIP 檔案名稱編碼不正確時可手動覆寫；瀏覽及解壓縮會重用同一選擇。
- 壓縮檔失敗時顯示格式、處理階段、穩定代碼及明確原因；偵錯版本可複製完整診斷資訊。
- 為一般檔案、資料夾及同一上層目錄的多選項目提供「壓縮...」動作。
- 可建立 ZIP、7Z、TAR、TAR.GZ、TAR.XZ、TAR.BZ2 與 TAR.ZST；表單只顯示所選格式真正支援的壓縮等級及密碼選項。
- 先寫入同一目錄的暫存檔，再以原子方式提交；名稱衝突時自動加入序號，不會覆寫現有檔案。

### 目前支援

目前版本識別以下可瀏覽及解壓縮的副檔名:

```text
zip, jar, aar, war, 7z, tar, tar.gz, tgz, tar.xz, txz, tar.bz2, tbz2, tar.zst, tzst
```

目前版本可建立以下格式:

```text
zip, 7z, tar, tar.gz, tar.xz, tar.bz2, tar.zst
```

> Explorer Action v6 原生瀏覽與項目預覽以及 v4 壓縮整合需要 AutoJs6 版本代碼 5276 或以上。主程式原生頁面內按項目解壓縮、建立分卷、建立時加密檔案名稱、個別壓縮、壓縮後刪除來源項目及檔案內新增/刪除尚未發布；請以 Roadmap 的核取狀態為準。

### 使用方法

1. 安裝外掛程式，並在 AutoJs6 外掛程式中心啟用。
2. 在檔案管理器開啟 ZIP、JAR、AAR、WAR、7Z 或 TAR 系列壓縮檔的選單。
3. 選擇「開啟壓縮檔」，然後在主程式檔案清單進入目錄、搜尋或使用路徑列跳轉。
4. 如要解壓縮整個壓縮檔，請從檔案選單選擇「解壓縮到...」，再透過 Android 系統選擇器指定輸出目錄。
5. 如要建立壓縮檔，請從一般檔案或資料夾選單選擇「壓縮...」；亦可先在同一目錄多選項目，再使用底部的「壓縮...」動作，然後選擇輸出格式及可用設定。

### 權限與資料

外掛程式不要求儲存空間或網絡權限。原生瀏覽優先租用主程式的唯讀可 seek 描述符，並以獨立位置讀取通道直接存取一般壓縮檔；管道、不可 seek 或可寫輸入、Android 7，以及必須使用本機檔案的相容後端（目前為加密 ZIP）才回退到私人快取。描述符租約或快取會於頁面關閉、解除連接、失敗或過期後清理。解壓縮只使用主程式臨時授予的輸入 URI。建立壓縮檔透過綁定外掛程式 UID 的主程式檔案工作階段分頁讀取目標，並且只能在目前上層目錄建立交易式輸出。密碼只保存在可清零的記憶體緩衝區，不會寫入 Bundle、偏好設定、日誌或診斷資訊，並於取代、工作結束或頁面銷毀時清除。固定 4 GiB 輸入上限及瀏覽階段大小/壓縮比門檻已移除；路徑隔離、來源大小核對、輸出交易及失敗清理仍然保留。

資源預算只決定何時警告或要求確認，不會放寬結構安全。確認後亦只把本次解壓縮的即時位元組及壓縮比邊界擴大到所選項目的聲明值；未聲明的額外增長、來源檔案變更、大小或 CRC 不一致仍會中止並清理輸出。

### Roadmap

更多格式、分卷、按項目解壓縮、壓縮檔編輯及完整裝置矩陣的任務與驗收條件均記錄在 Roadmap。未勾選項目不是目前功能。

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
- `新增` 支援建立非 solid 7Z，可選 0 至 9 壓縮等級及 AES-256 內容加密；檔案名稱保持可見，不會誤報檔案名稱加密能力
- `新增` 支援建立 TAR、TAR.GZ、TAR.XZ、TAR.BZ2 與 TAR.ZST，切換格式會更新完整副檔名及可用設定
- `新增` 支援瀏覽及解壓縮 ZipCrypto/AES 加密 ZIP，密碼錯誤可原位重試；建立 ZIP 時可選用 AES-256 密碼，檔案名稱保持可見並要求兩次密碼一致
- `新增` 支援瀏覽、預覽及解壓縮一般或 solid 7Z，涵蓋常見壓縮與篩選器鏈、AES 內容加密及標頭加密；缺少或錯誤密碼可明確診斷
- `新增` 支援在主程式原生檔案清單中瀏覽、預覽及解壓縮未壓縮 TAR；按標頭校驗和確認結構，連結、裝置節點及稀疏項目只讀列出
- `新增` 支援透過相同的主程式原生路徑瀏覽、預覽及解壓縮 TAR.GZ/TGZ、TAR.XZ/TXZ、TAR.BZ2/TBZ2 與 TAR.ZST/TZST；格式識別同時驗證壓縮串流簽名及內部 TAR 結構
- `修正` 改用目錄中繼資料快速開啟 ZIP，並相容自解壓縮式前置資料、傳統檔名編碼、Windows 分隔符及更多可讀取的 ZIP 方法
- `修正` 標準 `.z01 + .zip` 分卷 ZIP 會列出所需的前序分卷，不再將單獨開啟的最後一卷誤報為損壞
- `修正` 含上層穿越、絕對路徑、磁碟機前綴或控制字元名稱的壓縮檔不再因單一危險項目而整體無法瀏覽；危險名稱進入唯讀隔離目錄並保持可預覽，寫出前必須明確略過
- `修正` 自動偵測 ZIP 檔案名稱編碼不正確時可手動覆寫，解壓縮會重用所選編碼
- `修正` 未知或不精確大小、有效 DocumentsProvider URI 及主程式額外寫入授權不再令有效壓縮檔在解析前被拒絕
- `修正` 修正 Android 7.x 因呼叫新版系統專有 API 而無法瀏覽或解壓縮 ZIP 的問題
- `修正` 錯誤密碼統一歸類為 PASSWORD/WRONG_PASSWORD，並修正 AES v2 項目校驗值為零時被誤判損壞的問題
- `修正` TAR.GZ、TAR.XZ、TAR.BZ2、TAR.ZST 等複合副檔名的預設解壓縮資料夾會移除完整後綴，不再錯誤保留 `.tar`
- `改善` 移除固定 4 GiB 輸入上限及瀏覽階段大小/壓縮比門檻，同時保留路徑隔離、完整性驗證及失敗清理
- `新增` 解壓縮資源預算支援相容、嚴格及自訂模式；超出預算仍可唯讀瀏覽，並在寫出前顯示預計輸出、超限項目及單次確認
- `改善` 新增可逐項追蹤的 Roadmap，並重寫 README 與 CHANGELOG
- `改善` 獨立頁面改為跟隨系統日夜模式及 Material 動態色
- `改善` ZIP、7Z 與 TAR 系列輸出透過綁定外掛程式 UID 的主程式工作階段寫入同目錄暫存檔後原子提交，無需儲存權限且不會覆寫現有檔案
- `改善` 統一檢查格式及項目的預覽、解壓縮與建立能力，讓不可用選項保持停用
- `改善` 壓縮檔失敗時標明格式、處理階段、穩定代碼及原因，偵錯版本可複製完整診斷資訊
- `依賴` 新增以 Apache License 2.0 授權的 Zip4j 2.11.5，用於加密 ZIP 資料串流、AES-256 建立及 Android 7.x 相容路徑
- `依賴` 新增以 0BSD 授權的 XZ for Java 1.12，以純 Java 讀寫 TAR.XZ/TXZ，不增加原生 ABI
- `依賴` 新增以 BSD 授權的 zstd-jni 1.5.7-15，用於讀寫 TAR.ZST/TZST；四個 Android ABI 均通過 16 KiB ELF 對齊及 RELRO 檢查

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
- 第三方軟件聲明: [THIRD_PARTY_NOTICES.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/THIRD_PARTY_NOTICES.md)
- Android Storage Access Framework: https://developer.android.com/guide/topics/providers/document-provider
