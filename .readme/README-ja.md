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

Archive Manager は AutoJs6 のファイルマネージャーを置き換えず、その内部で動作します。対応アーカイブはホストの一覧、パスバー、テーマ、ビューアー、選択モード、進行表示、ディレクトリ更新をそのまま利用します。詳細な設定や書き込み操作だけを専用管理ページで行います。

### 現在利用できる機能

- ZIP/JAR/AAR/WAR、7Z、RAR4/RAR5、TAR 系を AutoJs6 のネイティブ一覧で閲覧し、内部パスバー、検索、並べ替え、戻る操作を利用できます。
- 名前が対応アーカイブと一致しないファイルには明示的な「アーカイブとして開く...」再確認を表示し、検出した形式をパスバーに示します。TAR の複合サフィックスを完全名で照合するため、通常の `.gz`、`.xz`、`.bz2`、`.zst` ストリームを TAR と誤表示しません。
- アーカイブ全体を展開せず、読み取り可能な文書、画像、音声、動画をホスト既存のビューアーでプレビューできます。
- アーカイブ全体、現在の内部フォルダー、選択項目を、進行表示、キャンセル、安全な競合名、公開前検証、コミット前ロールバック付きで展開できます。
- 暗号化 ZIP、7Z、RAR は、初回オープンまたは展開時にホストのパスワード画面を使用し、誤ったパスワードも現在のパスを失わず再入力できます。
- ホストのパスバーから ZIP ファイル名エンコーディングを直接修正できます。同じ読み取り専用セッションで索引を再構築し、可能な限り現在の内部パスと選択を保持します。
- 完全な標準 `.z01 + .zip`、最新 WinRAR `partN.rar`、連番 `.zip.001`、連番 `.7z.001` セットを、ホストが制限して許可した兄弟ボリューム記述子で閲覧、プレビュー、展開できます。欠落または変更されたボリュームは明示的に失敗します。
- 単一項目または同じ親の選択から ZIP、7Z、TAR、TAR.GZ、TAR.XZ、TAR.BZ2、TAR.ZST を作成できます。ZIP は AES-256、標準分割、項目ごとの個別作成にも対応します。
- 標準分割 ZIP と項目ごとの個別圧縮は、検証済みの全物理出力を一つの復旧可能バッチで公開します。失敗やホスト再起動を成功した部分結果として表示しません。
- 通常の単一ボリューム ZIP、安全な通常 7Z、対応する TAR、TAR.GZ/TGZ、TAR.XZ/TXZ、TAR.BZ2/TBZ2、TAR.ZST/TZST は、ファイルやフォルダーツリーの追加、空フォルダー作成、名前変更、削除を検証付き再構築で行い、完全な再読み取り後にだけ元ファイルを原子的に置換します。
- 書き込み可能なアーカイブの変更に成功した後は、成功メッセージまたは管理画面メニューから前のバージョンを復元できます。ホストの制限付き保存期間内に一度だけ利用でき、別のアプリが対象を変更した場合は拒否されます。
- 管理画面には実際の形式、内容件数、利用可能な編集操作、正確な読み取り専用理由をまとめて表示します。各編集は出力予約前に実際の作業量、全体再構築、メタデータへの影響を確認し、取り消しても保留中出力を作成しません。
- 危険な名前を読み取り専用で隔離し、書き込み前に構造とリソース制限を適用し、可能ならホストの seek 可能な記述子から直接読み取ります。
- 全物理出力の検証とコミット後に限り、元の選択全体をホストのゴミ箱へ移動できます。この設定は既定でオフで、元項目の変化や不完全な出力証明がある場合は元データを削除する前に停止します。

### 現在の形式

現行版は次の参照・展開可能な拡張子を認識します:

```text
zip, zip.001, jar, aar, war, 7z, 7z.001, rar, tar, tar.gz, tgz, tar.xz, txz, tar.bz2, tbz2, tar.zst, tzst
```

現行版は次の形式を作成できます:

```text
zip, 7z, tar, tar.gz, tar.xz, tar.bz2, tar.zst
```

> ネイティブ統合には Explorer Action v18 を備えた対応 AutoJs6 6.8.0 ビルド (バージョンコード 5276 以降) が必要です。RAR と分割アーカイブは読み取り専用です。通常の単一ボリューム ZIP、暗号化なし、非 solid、単一ボリュームかつデコーダー予算内の 7Z、安全な通常ファイルとフォルダーだけを含む TAR 系アーカイブを編集できます。暗号化、solid、分割、危険なパス、未対応方式、予算超過の 7Z は読み取り専用です。標準分割 ZIP は最終 `.zip`、最新 WinRAR セットは先頭 `partN.rar`、連番 ZIP または 7Z は `.001` から開き、必要な全ボリュームを同じフォルダーに置いてください。作成時のファイル名暗号化と JAR/AAR/WAR、RAR の内部編集には引き続き未対応です。

### 使い方

1. Archive Manager をインストールし、AutoJs6 プラグインセンターで有効にします。
2. 対応ファイルの主操作をタップするか、アーカイブを開くを選択します。名前が認識されない場合は、ファイルメニューから「アーカイブとして開く...」を選択します。通常のフォルダーと同様に閲覧でき、名前と形式が異なる場合はパスバーに検出結果が表示されます。
3. パスバーの展開操作で現在の内部フォルダーを展開するか、ファイル名エンコーディング操作で ZIP 名を修正します。長押しで選択項目を展開するか、ファイルメニューの展開先...でアーカイブ全体を展開します。必要な場合はパスワード画面が表示されます。
4. ファイルまたはフォルダーで圧縮...を選ぶか、同じディレクトリの複数項目を選択して下部バーの圧縮...を使います。成功後に元項目を整理する場合は、既定でオフの圧縮後に元項目をゴミ箱へ移動を明示的にオンにします。
5. 内容の追加、名前変更、削除が必要な場合は、通常の単一ボリューム ZIP、安全な通常 7Z、または対応する TAR、TAR.GZ/TGZ、TAR.XZ/TXZ、TAR.BZ2/TBZ2、TAR.ZST/TZST でアーカイブを管理...を選びます。

### 権限とデータ

Archive Manager はストレージ権限もネットワーク権限も要求しません。ホストは短命な読み取り専用記述子とプラグイン UID 固定の出力トランザクションだけを渡し、任意のパス選択を許しません。Explorer Action v11 はパスワードを上限付き同期再試行だけで運び、永続化しません。Explorer Action v12 が加えるのは、セッション限定で上限付きのホスト承認済み兄弟ボリューム一覧だけで、不透明 ID とファイル同一性を再検証します。Explorer Action v13 は同じ一時保存済みソースだけを再索引し、完全な置換索引が準備できるまで以前の状態を保持します。Explorer Action v14 は `.zip.001` と `.7z.001` などの上限付き複合サフィックスだけを照合し、任意の `.001` ファイルには一致せず、フォルダーやパスの権限を追加せずに v12 一覧を再利用します。Explorer Action v17 は通常の主操作が一致しない場合に、照合条件を持たない読み取り専用の補助操作だけを追加します。ユーザー操作後に既存セッションを一度再利用し、パス、フォルダー、書き込み権限は追加しません。危険なパスは隔離され、出力は公開前に検証され、予算確認でも構造安全検査は無効になりません。

Explorer Action v15 は同一セッションで検証済みの新規出力だけを最大 128 件の復旧可能バッチにまとめます。Explorer Action v16 は、プラグインが元の選択全体を順序どおりに示し、コミット済みの全出力トランザクションを正確に証明した場合だけ、ホストが元項目と出力を再検証してゴミ箱へ移動します。ホストは元データを除去する前に復旧コピーを同期し記録を永続化します。プラグインには任意パスや直接削除の権限を与えません。Binder 応答を失った場合は移動を再試行せず、同じ冪等終端結果を照会します。

Explorer Action v18 は前のアーカイブをホストの非公開永続ストレージだけに保存し、バックアップパスではなく不透明な履歴 ID を返します。親フォルダーと対象が確定した置換結果に完全一致する間だけ一度復元できます。外部変更があると履歴は無効になり、中断した復旧証拠はホストが解決するまで保持され、同じ対象の次の置換を止めます。通常履歴には期間、件数、総バイト数、空き容量の制限があり、v8-v17 セッションは置換バックアップを作成しません。

### Roadmap

形式に依存しない共有再構築 planner は完了しました。残作業は、ファイルマネージャーのレイアウトを変えないホストネイティブの書き込み可能アーカイブ provider の評価、ゴミ箱のグループ取り消しと履歴、残りの端末マトリックス、初回公開リリース資料として追跡します。

- [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/ROADMAP.md)

### リリースノート

#### v2.18.0

_2026/08/29_

- `注記` このバージョンも Explorer Action v18 対応の AutoJs6 6.8.0 ビルド (バージョンコード 5276 以降) が必要です
- `改善` 安全な通常 7Z と編集可能な全 TAR 系形式が、形式に依存しない完全再構築 planner を共有するようになりました。各 backend が能力と保持項目の検証を明示的に提供し、対応形式、出力設定、読み取り専用境界を変えずに従来の TAR 命名と結合を解消します

#### v2.17.0

_2026/08/29_

- `注記` このバージョンも Explorer Action v18 対応の AutoJs6 6.8.0 ビルド (バージョンコード 5276 以降) が必要です
- `追加` 通常の単一ボリューム、暗号化なし、非 solid かつデコーダー予算内の 7Z で、管理画面からファイルや完全なフォルダーツリーの追加、空フォルダー作成、名前変更、削除が可能になりました
- `修正` 管理操作が `.7z` に対応しました。暗号化、solid、分割、危険なパス、未対応方式、予算超過の 7Z は構造検査後も読み取り専用です
- `改善` 7Z の変更は制限付き非 solid LZMA2 出力へ再構築し、原子的置換前に全体を再読み取りします。取り消し、元ファイル変更、保留中出力の破損、書き込み失敗では元ファイルを保持します

#### v2.16.0

_2026/08/29_

- `注記` このバージョンも Explorer Action v18 を備えた対応 AutoJs6 6.8.0 ビルド (バージョンコード 5276 以降) が必要です
- `追加` 安全な通常ファイルとディレクトリだけを含む TAR.ZST および TZST で、管理画面からファイルまたはフォルダーツリー全体の追加、空フォルダー作成、名前変更、削除が可能になりました
- `修正` 管理アクションが `.tar.zst` と `.tzst` を正確に認識するようになり、対応するすべての TAR ラッパーが同じ検証付き書き込み再構築境界を使用します
- `改善` TAR.ZST の変更は、1 MiB ウィンドウとフレームチェックサムを持つ上限付き単一スレッド Zstandard レベル 3 を使用し、ホスト所有の保留中出力へ直接ストリーム再構築します。キャンセル、チェックサム完全性、実際の書き込み失敗、元データ変更、完全な再読み取りはすべてロールバック境界内に維持されます

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

- AutoJs6 ドキュメント: https://docs.autojs6.com
- サードパーティーソフトウェアに関する通知: [THIRD_PARTY_NOTICES.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/THIRD_PARTY_NOTICES.md)
- Android Storage Access Framework: https://developer.android.com/guide/topics/providers/document-provider
