<div align="center">
  <p>
    <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="Archive Manager" width="128" />
  </p>

  <h1>Archive Manager</h1>

  <p>AutoJs6 ファイルマネージャーに統合され、対応アーカイブの閲覧、展開、作成、安全な編集を行う汎用アーカイブマネージャー</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Archive-Manager?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Archive-Manager?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Archive-Manager?color=534BAE&label=License"/></a>
  </p>
</div>

### 言語 (Languages)

README は次の言語で提供しています:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-zh-Hans.md)
- [香港繁體 [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-zh-Hant-HK.md)
- [台灣繁體 [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-es.md)
- 日本語 [ja] # 現在
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-ar.md)

### 概要

Archive Manager は AutoJs6 のファイルマネージャーを置き換えず、その内部で動作します。対応アーカイブはホストの一覧、パスバー、テーマ、ビューアー、選択モード、進行表示、ディレクトリ更新をそのまま利用します。詳しい形式情報と、より詳細なフォームが必要な設定には専用管理ページを使用します。

### スクリーンショット

実際の Android 画面で、ホストメニューとの連携、ネイティブのアーカイブ参照、アーカイブ作成、詳細管理を示します。公開可能な合成データだけを使用しています。

<table>
  <tr>
    <td><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/docs/images/screenshots/explorer-actions.png?raw=true" alt="Archive actions in AutoJs6 Explorer" width="280" /></td>
    <td><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/docs/images/screenshots/native-archive-browsing.png?raw=true" alt="Native archive browsing" width="280" /></td>
  </tr>
  <tr>
    <td><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/docs/images/screenshots/create-archive-form.png?raw=true" alt="Archive creation form" width="280" /></td>
    <td><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/docs/images/screenshots/archive-management.png?raw=true" alt="Archive management page" width="280" /></td>
  </tr>
</table>

- 撮影条件と全スクリーンショット: [docs/images/screenshots/README.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/docs/images/screenshots/README.md)

### 現在利用できる機能

- ZIP 系、7Z、TAR 系アーカイブを AutoJs6 のネイティブファイル一覧で直接開き、ホストのテーマ、ダークモード、ダイナミックカラーを使用。
- パスバーに外部フォルダー、アーカイブ名、内部フォルダーを表示。階層をタップして移動し、戻る操作で内部の上位へ戻ってからアーカイブを終了。
- ホストのネイティブ画面を離れず、パスバーから現在の内部フォルダーを展開するか、選択モードでチェックしたファイルとフォルダーを展開。進行状況を表示して取り消しでき、完了後は親フォルダーを自動更新。
- 対応する文書、画像、音声、動画の項目をホスト既存のビューアーでプレビュー。
- 「展開先...」でアーカイブ全体を推奨される同じディレクトリのフォルダーへ展開するか、Android のシステム選択画面で別のフォルダーを選択。同等名の既存フォルダーは安全に採番。
- 「アーカイブを管理...」で管理画面を開き、アーカイブ全体、現在の内部フォルダー、現在のチェック項目を展開するか、通常の単一ボリューム ZIP または対応する TAR、TAR.GZ/TGZ、TAR.XZ/TXZ、TAR.BZ2/TBZ2、TAR.ZST/TZST を「ファイルを追加...」「フォルダーを追加...」「新しいフォルダー...」「名前を変更...」「削除」で編集。「展開先...」は全体展開のショートカットとして維持。
- ZIP と TAR の変更は書き込み前に計画され、ホスト所有の保留中出力へ再構築して完全に読み戻し、検証後にだけ元ファイルをアトミックに置換します。キャンセルまたは失敗時は元ファイルを変更せず、確定成功後は Explorer を自動更新します。
- 同等の出力名には「毎回確認」「スキップ」「上書き」「自動で名前を変更」を選択可能。「すべてに適用」で残りの互換性のある競合を処理し、既存の出力フォルダーは常に番号を付けて保持します。
- アーカイブ内のフォルダーを参照、検索、並べ替え。
- 全項目を事前展開せず、ディレクトリメタデータから一覧を作成。
- 通常のアーカイブはホストの読み取り専用 seek 対応ディスクリプターと独立した位置を持つ読み取りチャネルから、全体をコピーせず直接参照。パイプ、seek 不可または書き込み可能な入力、Android 7、プロセスから読めるローカルファイルを必要とする reader（現在は暗号化 ZIP）だけをプライベートキャッシュへ退避し、終了時に削除。
- 展開リソース予算を「互換」「厳格」「カスタム」から選択。エントリ数、パス、出力サイズ、圧縮率の予算を超えるアーカイブも参照でき、書き出し前に推定容量とリスクを表示して一度だけ確認します。
- 展開中に項目数とバイト数、現在の項目、転送速度、推定残り時間を表示。キャンセルまたは失敗時は新規出力ルートをロールバックし、プロバイダーが削除を拒否した残存場所は名前と URI で示します。
- 親ディレクトリへの移動、絶対パス、ドライブ接頭辞、制御文字を含む名前をパスバーに表示される「安全でないパス」フォルダーへ隔離。読取可能なデータはプレビューでき、全体展開では通常項目に影響を与えず、これらの明示的なスキップを要求。
- 非圧縮 TAR を参照、プレビュー、展開。シンボリックリンク、ハードリンク、デバイスノード、スパース項目は一覧表示のみで、通常ファイルとして書き出しません。
- TAR.GZ/TGZ、TAR.XZ/TXZ、TAR.BZ2/TBZ2、TAR.ZST/TZST を同じ内部パス、特殊項目の分離、整合性検査で参照、プレビュー、展開。
- 通常または solid の 7Z を参照、プレビュー、展開。一般的な圧縮/フィルターチェーン、内容暗号化、ヘッダー暗号化に対応し、パスワード不足または誤りは明確に診断。
- 実際の ZIP/7Z/TAR 構造を確認し、プレビュー、展開、作成の機能判定を統一。未対応の設定は無効のまま表示。
- Zip64、自己展開形式の前置データ、旧式の名前エンコーディング、Windows 区切り文字に対応。
- ホストが上限付きで許可した同一フォルダーの記述子から、完全な標準 `.z01 + .zip` セットを参照、プレビュー、展開。プリセットまたはカスタム MiB サイズで標準分割 ZIP を作成でき、ボリューム不足や変更は明示的に報告します。
- ZipCrypto または AES で保護された ZIP を参照・展開し、誤ったパスワードをその場で再試行できます。作成時はファイル名を表示したまま AES-256 パスワードを任意で設定でき、暗号化 ZIP の作成には一致する確認入力が必要です。
- ZIP ファイル名の自動検出が正しくない場合は手動で上書きし、参照と展開で同じ選択を再利用。
- 失敗時に形式、処理段階、安定したコード、明確な理由を表示し、デバッグ版では完全な診断情報をコピー可能。
- 通常のファイル、フォルダー、同じ親フォルダー内の複数選択に「圧縮...」を表示。
- 同じ親フォルダー内の複数選択から項目ごとに個別のアーカイブを作成。フォームで出力数と派生名を確認でき、既存名や重複名には上書きせず自動で番号が付きます。各出力は個別に確定され、途中のキャンセルや失敗では完了済みの出力を保持して報告し、曖昧な一括再試行を防ぎます。
- 通常または標準分割 ZIP、7Z、TAR、TAR.GZ、TAR.XZ、TAR.BZ2、TAR.ZST を作成し、選択形式が実際に対応する圧縮レベルとパスワード設定だけを表示。
- 同じフォルダーの一時ファイルへ書き込んでからアトミックに確定し、自動採番または正確な名前を先に試して番号付きで再試行する前の確認を選択。既存ファイルは上書きしません。名前の予約後、一時出力を開く前に上限付きのソーススナップショットを走査し、画面には走査、圧縮、検証、確定の各段階と総ファイル数、読み取りバイト数、サイズ不明ファイル数を表示します。公開前に非表示の出力全体を読み直し、形式、項目、サイズ、CRC、内容フィンガープリントを確認します。作成または検証失敗時はトランザクションを中止し、一時出力の清掃をホストが確認できない場合は予定パスを表示して再試行を停止します。

### 現在の形式

現行版は次の参照・展開可能な拡張子を認識します:

```text
zip, zip.001, jar, aar, war, 7z, 7z.001, rar, tar, tar.gz, tgz, tar.xz, txz, tar.bz2, tbz2, tar.zst, tzst
```

現行版は次の形式を作成できます:

```text
zip, 7z, tar, tar.gz, tar.xz, tar.bz2, tar.zst
```

> ネイティブ統合には Explorer Action v21 を備えた対応 AutoJs6 6.8.0 ビルド (バージョンコード 5276 以降) が必要です。RAR と分割アーカイブは読み取り専用です。通常の単一ボリューム ZIP、暗号化なし、非 solid、単一ボリュームかつデコーダー予算内の 7Z、安全な通常ファイルとフォルダーだけを含む TAR 系アーカイブを編集できます。暗号化、solid、分割、危険なパス、未対応方式、予算超過の 7Z は読み取り専用です。標準分割 ZIP は最終 `.zip`、最新 WinRAR セットは先頭 `partN.rar`、連番 ZIP または 7Z は `.001` から開き、必要な全ボリュームを同じフォルダーに置いてください。作成時のファイル名暗号化と JAR/AAR/WAR、RAR の内部編集には引き続き未対応です。

### 使い方

1. プラグインをインストールし、AutoJs6 プラグインセンターで有効にします。
2. ZIP、JAR、AAR、WAR、7Z、または TAR 系アーカイブのメニューを開きます。
3. 「アーカイブを開く」を選び、ホストのファイル一覧でフォルダーを開く、検索する、またはパスバーから移動します。
4. 全体を展開する場合は、ファイルメニューから「展開先...」を選びます。推奨される現在のフォルダーを使うか、Android のシステム選択画面で別のフォルダーを選び、正確な出力パスを確認します。
5. 現在の内部フォルダーを展開するには、パスバー右側の展開ボタンをタップします。特定の項目は長押して選択モードに入り、ファイルまたはフォルダーを選んで下部の「展開」をタップします。パスワード、文字コード修正、危険パスの確認、競合ルール、または別の出力先が必要な場合は「アーカイブを管理...」または「展開先...」を使用します。
6. 通常の単一ボリューム ZIP または対応する TAR、TAR.GZ/TGZ、TAR.XZ/TXZ、TAR.BZ2/TBZ2、TAR.ZST/TZST を編集するには「アーカイブを管理...」を選び、「ファイルを追加...」、「フォルダーを追加...」でフォルダーツリー全体を取り込む、「新しいフォルダー...」で空フォルダーを作成する、「名前を変更...」、または「削除」を使用します。再構築、検証、成功メッセージが完了するまで画面を離れないでください。
7. 管理画面で展開する前に、同等の出力名の処理方法を選択します。「毎回確認」では、スキップ、上書き、自動名前変更の決定を残りの互換性のある競合すべてに適用できます。
8. アーカイブを作成するには通常のファイルまたはフォルダーのメニューで「圧縮...」を選ぶか、同じフォルダーの複数項目を選択して下部バーの「圧縮...」を使用します。項目ごとに作成する場合は「各項目を個別に圧縮」を有効にし、出力プレビューを確認してから作成してください。このモードの名前競合は常に安全な自動採番で処理されます。ZIP では「分割なし」、一般的な MiB プリセット、または 1 から 4096 MiB の整数を選べます。出力がそのサイズを超える場合は `.z01`、`.z02`、... の番号付きボリュームと最後の `.zip` で構成され、小さい場合は 1 個の `.zip` のままです。

### 権限とデータ

Archive Manager はストレージ権限もネットワーク権限も要求しません。ホストは短命な読み取り専用記述子とプラグイン UID 固定の出力トランザクションだけを渡し、任意のパス選択を許しません。Explorer Action v11 はパスワードを上限付き同期再試行だけで運び、永続化しません。Explorer Action v12 が加えるのは、セッション限定で上限付きのホスト承認済み兄弟ボリューム一覧だけで、不透明 ID とファイル同一性を再検証します。Explorer Action v13 は同じ一時保存済みソースだけを再索引し、完全な置換索引が準備できるまで以前の状態を保持します。Explorer Action v14 は `.zip.001` と `.7z.001` などの上限付き複合サフィックスだけを照合し、任意の `.001` ファイルには一致せず、フォルダーやパスの権限を追加せずに v12 一覧を再利用します。Explorer Action v17 は通常の主操作が一致しない場合に、照合条件を持たない読み取り専用の補助操作だけを追加します。ユーザー操作後に既存セッションを一度再利用し、パス、フォルダー、書き込み権限は追加しません。危険なパスは隔離され、出力は公開前に検証され、予算確認でも構造安全検査は無効になりません。

Explorer Action v15 は同一セッションで検証済みの新規出力だけを最大 128 件の復旧可能バッチにまとめます。Explorer Action v16 は、プラグインが元の選択全体を順序どおりに示し、コミット済みの全出力トランザクションを正確に証明した場合だけ、ホストが元項目と出力を再検証してゴミ箱へ移動します。ホストは元データを除去する前に復旧コピーを同期し記録を永続化します。プラグインには任意パスや直接削除の権限を与えません。Binder 応答を失った場合は移動を再試行せず、同じ冪等終端結果を照会します。

Explorer Action v18 は前のアーカイブをホストの非公開永続ストレージだけに保存し、バックアップパスではなく不透明な履歴 ID を返します。親フォルダーと対象が確定した置換結果に完全一致する間だけ一度復元できます。外部変更があると履歴は無効になり、中断した復旧証拠はホストが解決するまで保持され、同じ対象の次の置換を止めます。通常履歴には期間、件数、総バイト数、空き容量の制限があり、v8-v17 セッションは置換バックアップを作成しません。Explorer Action v19 は許可された項目の削除または名前変更に必要な不透明 ID と安全な末尾名だけを渡します。Explorer Action v20 は明示的に選択した入力ルートだけに凍結権限を追加します。プラグインが受け取るのは上限付きの不透明ノード、メタデータ、1 回限りの読み取り専用記述子で、元のパス、URI、未選択の同階層項目、一般ストレージへのアクセスは受け取りません。ホストは確定前に完全なスナップショットを再検証し、変更または失敗した入力が一つでもあればアーカイブ置換全体を中止します。Explorer Action v21 は Activity やホストセッションの再作成後も、ホスト所有の有界な元項目復元バッチを保持します。復元は使用中の名前を上書きせず、作成済みアーカイブも削除しません。

### Roadmap

ネイティブの参照、展開、作成、アーカイブ変更、前バージョン復元、永続的な元項目復元は、通常のファイルマネージャーレイアウトを変えずに完了しました。Roadmap の未チェック項目は将来の任意のプロトコル、バックエンド、境界ケース改善であり、現在の機能ではありません。

- [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/ROADMAP.md)

### リリースノート

#### v2.22.3

_2026/09/19_

- `修正` 共有ビルドプラグイン 1.8.3 により, AGP 9.1 での SDK XML v4 解析警告と, JVM 単体テストの組み立て時に APK ネイティブライブラリのアラインメント検証が誤って実行される問題
- `改善` compileSdk に続き targetSdk を 37 (Android 17) に引き上げ, プラグインの動作は新しいターゲットの影響を受けない

#### v2.22.2

_2026/09/13_

- `修正` プラグインセンターのバージョンと ABI 情報がインストール済み APK と一致
- `修正` バージョン日付は英語の統一形式で表示されます
- `改善` ダウンロード用ファイルの作成前に, リリース APK のバージョン, 署名, バリアントの完全性を検証

#### v2.22.1

_2026/09/11_

- `改善` 64 ビットのネイティブライブラリの 16 KB ページアラインメントをビルド時に検証, manifest 契約の検査と JSON レポートに対応

##### 全履歴

* [CHANGELOG-ja.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/app/src/main/assets/doc/CHANGELOG-ja.md)

### ビルド

```powershell
.\gradlew.bat :app:assembleDebug
```

Release ビルド:

```powershell
.\gradlew.bat :app:assembleRelease
```

リポジトリ直下の Gradle Wrapper を使い、SDK/JDK 要件は `version.properties` を参照してください.

### リンク

- インストールガイド: [docs/INSTALLATION.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/docs/INSTALLATION.md)
- 形式別機能表: [docs/FORMAT_CAPABILITIES.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/docs/FORMAT_CAPABILITIES.md)
- セキュリティポリシー: [SECURITY.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/SECURITY.md)
- AutoJs6 ドキュメント: https://docs.autojs6.com
- サードパーティーソフトウェアに関する通知: [THIRD_PARTY_NOTICES.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/THIRD_PARTY_NOTICES.md)
- Android Storage Access Framework: https://developer.android.com/guide/topics/providers/document-provider


[16 KB page alignment and build verification](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/docs/16kb.md)
