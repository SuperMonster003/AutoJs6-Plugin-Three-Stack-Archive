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
- 檔案名稱未符合支援的壓縮檔案時, 提供明確的「作為壓縮檔案開啟...」複核, 並在路徑列標示偵測到的實際格式; TAR 複合後綴依完整名稱精確比對, 不會把一般 `.gz`、`.xz`、`.bz2` 或 `.zst` 壓縮串流顯示為壓縮檔案.
- 直接使用宿主現有的文件、圖片、音訊與影片預覽器讀取可用項目, 無需先解壓縮整個檔案.
- 可解壓縮整個檔案、目前內部目錄或已選項目, 支援進度、取消、安全衝突編號、發佈前驗證與提交前復原.
- 加密 ZIP、7Z 與 RAR 可在首次開啟或解壓縮時使用宿主原生密碼框; 密碼錯誤後可在原位重試, 不會失去目前檔案路徑.
- 可從宿主路徑列直接修正 ZIP 檔名編碼; 同一唯讀工作階段在原位重建索引, 並盡量保留目前內部路徑與已選項目.
- 可透過宿主有界授權的同層卷描述符瀏覽、預覽與解壓縮完整的標準 `.z01 + .zip`、現代 WinRAR `partN.rar`、編號 `.zip.001` 與編號 `.7z.001` 卷組; 缺卷或卷變更會回傳明確錯誤.
- 可由單一項目或同父層多選建立 ZIP、7Z、TAR、TAR.GZ、TAR.XZ、TAR.BZ2 與 TAR.ZST; ZIP 另支援 AES-256 與標準分卷, 多選也可為每項分別建立檔案.
- 分割 ZIP 與「分別壓縮每個項目」會在所有實體輸出逐項驗證後透過一個可復原批次統一發佈; 失敗或宿主重新啟動不會把一般部分結果當作成功.
- 普通單卷 ZIP 可透過已驗證重建加入檔案、匯入完整目錄樹、建立空目錄、重新命名與刪除; 只有完整讀回通過後才以原子方式取代來源檔案.
- 管理頁集中顯示實際格式、內容統計、可用修改操作與精確的唯讀原因; 每次 ZIP 修改都會在預留輸出前列出實際工作量、完整重建與輔助中繼資料影響, 取消不會建立待提交輸出.
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

> 原生整合需要搭配 AutoJs6 6.8.0 Explorer Action v17 建置 (版本代碼 5276 或更新版本). RAR 與分割檔明確維持唯讀, 目前只有一般單卷 `.zip` 可修改. 標準分割 ZIP 應從最終 `.zip` 開啟, 現代 WinRAR 卷組應從首個 `partN.rar` 開啟, 編號 ZIP 或 7Z 應從 `.001` 開啟, 且所有必要同層卷須位於同一目錄. 建立時加密檔名, 以及修改 7Z、RAR 與 TAR 系列檔案均不是目前功能.

### 使用方式

1. 安裝 Archive Manager, 並在 AutoJs6 外掛中心啟用.
2. 點選檔案的主要操作, 或從選單選擇「開啟壓縮檔案」. 名稱未被識別時, 可選擇「作為壓縮檔案開啟...」. 接著可像普通目錄一樣使用宿主路徑列; 名稱與實際格式不符時會標示偵測結果.
3. 使用路徑列右側操作解壓縮目前內部目錄, 或透過檔名編碼操作修正 ZIP 名稱; 長按項目可多選解壓縮; 檔案選單的「解壓縮到...」用於解壓縮整個檔案. 需要密碼時宿主會自動提示.
4. 對檔案或資料夾選擇「壓縮...」; 也可在同一目錄多選後使用底部操作列的「壓縮...」. 如需成功後清理來源項目, 請明確開啟預設關閉的「壓縮完成後將來源項目移入回收桶」.
5. 只有需要加入、重新命名或刪除內容時, 才對普通單卷 ZIP 選擇「管理壓縮檔案...」.

### 權限與資料

Archive Manager 不會申請儲存空間或網路權限. 宿主只提供短生命週期唯讀描述符與固定外掛 UID 的輸出交易, 外掛不能自行選擇任意檔案系統路徑. Explorer Action v11 只在有界同步重試要求中傳遞密碼, 雙方立即移除並清理保留緩衝, 且從不持久化. Explorer Action v12 只增加工作階段級、有界的宿主核准同層卷目錄: 外掛取得不透明 ID 而非路徑, 呼叫端 UID、檔案身分、大小、修改時間與生命週期均在使用前重複驗證. Explorer Action v13 只重新索引同一暫存來源, 並在完整替換索引準備完成前保留舊狀態. Explorer Action v14 只精確比對 `.zip.001` 與 `.7z.001` 等有界複合後綴, 不會比對任意 `.001` 檔案, 且重用 v12 目錄而不授予目錄或路徑存取能力. Explorer Action v17 只在一般主要操作未比對時增加無比對器的唯讀溢出複核入口; 它只在使用者點選後重用一次既有壓縮檔案工作階段, 不增加路徑、目錄或寫入權限. Android 與 Java 程式庫仍可能產生無法完全控制的短生命週期執行期副本, 因此密碼清理屬盡力而為, 並非絕對保證. 路徑穿越與危險名稱始終隔離, 輸出在發佈前完成驗證, 資源預算確認也不會關閉結構安全檢查.

Explorer Action v15 只把同一工作階段中已驗證的新檔案輸出組成最多 128 項的可復原批次. Explorer Action v16 只有在外掛提交完整有序來源選擇與所有已提交輸出交易的精確證明後, 才讓宿主重新核對來源與輸出身分並將來源項目移入回收桶. 宿主先同步復原副本並持久記錄項目, 再移除來源資料; 外掛不會取得任意路徑或直接刪除能力. Binder 回應遺失時只查詢同一冪等終態, 不會盲目重試.

### Roadmap

其餘工作繼續以可勾選項目追蹤: 一般 ZIP 以外的可寫重建、回收桶群組復原與歷史、其餘裝置與製作工具矩陣, 以及首次公開發布資料.

- [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/ROADMAP.md)

### 版本記錄

#### v2.10.0

_2026/08/28_

- `提示` 此版本需要搭配 AutoJs6 6.8.0 Explorer Action v17 建置 (版本代碼 5276 或更新版本)
- `新增` 名稱或副檔名未被識別的檔案現在可選擇「作為壓縮檔案開啟...」; 結構偵測成功後, 名稱與實際格式不符時宿主原生路徑列會標示真實格式
- `新增` Explorer Action v17 新增綁定一般壓縮檔案主要操作、無比對器且唯讀的複核入口, 並透過既有工作階段回傳有界的實際格式中繼資料
- `新增` 管理頁新增壓縮檔案資訊入口, 集中顯示實際格式、內容統計、可用修改操作與精確的唯讀原因
- `修正` TAR.GZ、TAR.XZ、TAR.BZ2 與 TAR.ZST 現在使用精確的完整檔案名稱後綴, 不再發布通用 `gz`、`xz`、`bz2` 或 `zst` 葉副檔名, 一般壓縮串流不會取得壓縮檔案主要操作
- `改善` 宿主建立選單時不會在背景掃描檔案; 只有使用者明確點選後才執行一次既有唯讀壓縮檔案開啟呼叫, 且不新增路徑、目錄或寫入權限
- `改善` 加入、匯入、建立、重新命名與刪除現在統一在預留宿主輸出前顯示實際工作量、完整重建與輔助中繼資料影響; 取消確認不會建立待提交輸出

#### v2.9.0

_2026/08/27_

- `提示` 此版本需要搭配 AutoJs6 6.8.0 Explorer Action v16 建置 (版本代碼 5276 或更新版本)
- `新增` 壓縮表單新增預設關閉的「壓縮完成後將來源項目移入回收桶」; 只有所有實體輸出完成驗證並成功提交後才會執行
- `新增` Explorer Action v16 只接受本次完整有序選擇與所有已提交輸出交易的精確證明, 再由宿主重新核對來源與輸出身分並執行回收桶操作
- `修正` 宿主現在先同步復原副本並持久記錄回收桶項目, 再移除來源; 目錄只部分移除時會保留可復原副本, 不再清除唯一復原資料
- `改善` 移入回收桶階段不可取消, 成功、需要復原、失敗與結果未知會分別提示; Binder 回應遺失時查詢宿主終態而不盲目重試

#### v2.8.0

_2026/08/27_

- `提示` 此版本需要搭配 AutoJs6 6.8.0 Explorer Action v15 建置 (版本代碼 5276 或更新版本)
- `新增` 標準分割 ZIP 與「分別壓縮每個項目」現在會先完成所有輸出的寫入及逐項讀回驗證, 再透過 Explorer Action v15 作為一個可復原批次統一發佈
- `修正` 多輸出建立不再於一般故障路徑留下已提交的部分結果; 只有完整批次提交後才重新整理 Explorer 並回報成功
- `修正` 壓縮選項開關現在可在 Android 7 上正確繪製並保持可點按, 不再只顯示為一般文字標籤
- `改善` 宿主在發佈前持久記錄父目錄與每個暫存檔案的身分; 失敗或重新啟動時只回滾身分仍相符的成員, 外部改寫的檔案會保留並回報需要手動復原

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
