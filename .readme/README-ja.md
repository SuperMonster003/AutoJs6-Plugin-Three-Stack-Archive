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
- アーカイブ全体を展開せず、読み取り可能な文書、画像、音声、動画をホスト既存のビューアーでプレビューできます。
- アーカイブ全体、現在の内部フォルダー、選択項目を、進行表示、キャンセル、安全な競合名、公開前検証、コミット前ロールバック付きで展開できます。
- 暗号化 ZIP、7Z、RAR は、初回オープンまたは展開時にホストのパスワード画面を使用し、誤ったパスワードも現在のパスを失わず再入力できます。
- ホストのパスバーから ZIP ファイル名エンコーディングを直接修正できます。同じ読み取り専用セッションで索引を再構築し、可能な限り現在の内部パスと選択を保持します。
- 完全な標準 `.z01 + .zip`、最新 WinRAR `partN.rar`、連番 `.zip.001`、連番 `.7z.001` セットを、ホストが制限して許可した兄弟ボリューム記述子で閲覧、プレビュー、展開できます。欠落または変更されたボリュームは明示的に失敗します。
- 単一項目または同じ親の選択から ZIP、7Z、TAR、TAR.GZ、TAR.XZ、TAR.BZ2、TAR.ZST を作成できます。ZIP は AES-256、標準分割、項目ごとの個別作成にも対応します。
- 通常の単一ボリューム ZIP は、ファイルやフォルダーツリーの追加、空フォルダー作成、名前変更、削除を検証付き再構築で行い、完全な再読み取り後にだけ元ファイルを原子的に置換します。
- 危険な名前を読み取り専用で隔離し、書き込み前に構造とリソース制限を適用し、可能ならホストの seek 可能な記述子から直接読み取ります。

### 現在の形式

現行版は次の参照・展開可能な拡張子を認識します:

```text
zip, zip.001, jar, aar, war, 7z, 7z.001, rar, tar, tar.gz, tgz, tar.xz, txz, tar.bz2, tbz2, tar.zst, tzst
```

現行版は次の形式を作成できます:

```text
zip, 7z, tar, tar.gz, tar.xz, tar.bz2, tar.zst
```

> ネイティブ統合には Explorer Action v14 を備えた対応 AutoJs6 6.8.0 ビルド (バージョンコード 5276 以降) が必要です。RAR と分割アーカイブは読み取り専用で、編集できるのは通常の単一ボリューム `.zip` だけです。標準分割 ZIP は最終 `.zip`、最新 WinRAR セットは先頭 `partN.rar`、連番 ZIP または 7Z は `.001` から開き、必要な全ボリュームを同じフォルダーに置いてください。作成時のファイル名暗号化、圧縮後の元ファイル削除、7Z/RAR/TAR 系の内部編集には未対応です。

### 使い方

1. Archive Manager をインストールし、AutoJs6 プラグインセンターで有効にします。
2. 対応ファイルの主操作をタップするか、アーカイブを開くを選択し、ホストのパスバーで通常のフォルダーと同様に閲覧します。
3. パスバーの展開操作で現在の内部フォルダーを展開するか、ファイル名エンコーディング操作で ZIP 名を修正します。長押しで選択項目を展開するか、ファイルメニューの展開先...でアーカイブ全体を展開します。必要な場合はパスワード画面が表示されます。
4. ファイルまたはフォルダーで圧縮...を選ぶか、同じディレクトリの複数項目を選択して下部バーの圧縮...を使います。
5. 内容の追加、名前変更、削除が必要な場合だけ、通常の単一ボリューム ZIP でアーカイブを管理...を選びます。

### 権限とデータ

Archive Manager はストレージ権限もネットワーク権限も要求しません。ホストは短命な読み取り専用記述子とプラグイン UID 固定の出力トランザクションだけを渡し、任意のパス選択を許しません。Explorer Action v11 はパスワードを上限付き同期再試行だけで運び、永続化しません。Explorer Action v12 が加えるのは、セッション限定で上限付きのホスト承認済み兄弟ボリューム一覧だけで、不透明 ID とファイル同一性を再検証します。Explorer Action v13 は同じ一時保存済みソースだけを再索引し、完全な置換索引が準備できるまで以前の状態を保持します。Explorer Action v14 は `.zip.001` と `.7z.001` などの上限付き複合サフィックスだけを照合し、任意の `.001` ファイルには一致せず、フォルダーやパスの権限を追加せずに v12 一覧を再利用します。危険なパスは隔離され、出力は公開前に検証され、予算確認でも構造安全検査は無効になりません。

### Roadmap

残作業はチェック可能な項目として追跡します。通常 ZIP 以外の書き込み再構築、元ファイル削除または取り消しトランザクション、アクセシビリティ、残りの端末と生成ツールの組み合わせ、初回公開リリース資料です。

- [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/ROADMAP.md)

### リリースノート

#### v2.7.0

_2026/08/27_

- `注記` このリリースには Explorer Action v14 を備えた対応 AutoJs6 6.8.0 ビルド (バージョンコード 5276 以降) が必要です
- `追加` 完全な連番 `.zip.001` と `.7z.001` セットを `.001` ボリュームから閲覧、プレビュー、展開できるようになりました。連番セットは読み取り専用です
- `追加` Explorer Action v14 は上限付き複合ファイル名サフィックス照合を追加し、UID に固定された v12 兄弟ボリュームソースを再利用します。任意の `.001` ファイルには一致しません
- `修正` Android 7 では、ホストが許可した連番 ZIP ボリュームを Zip4j 互換経路の前に 1 つのプライベートローカルファイルへ結合し、正常なセットを破損として報告しなくなりました
- `改善` 兄弟ボリューム番号は `.002` から `.128` に制限され、存在する全ボリュームは連続している必要があります。reader は次に欠けたボリュームを正確に報告し、具体化の前後で同一性を再検証し、内部編集機能を公開しません

#### v2.6.0

_2026/08/27_

- `注記` このバージョンには Explorer Action v13 を備えた対応 AutoJs6 6.8.0 ビルド (バージョンコード 5276 以降) が必要です
- `追加` ホストのネイティブアーカイブ画面で管理画面を開かずにパスバーから ZIP ファイル名エンコーディングを選択できるようになり, 変更後も現在の内部フォルダーと存在する選択項目を保持します
- `追加` Explorer Action v13 は同じ読み取り専用セッションで一時保存済みソースを再索引し, 安定した項目 ID で利用可能な最深パスと存在する項目を復元します
- `改善` 置換索引は完成後にのみ公開されます; 無効な選択, スキャン失敗, 実行中のプレビューまたは展開では以前の索引を維持し, 入力済みパスワードは消去可能なメモリーだけに保持されます

#### v2.5.0

_2026/08/27_

- `注記` このバージョンには Explorer Action v12 を備えた対応 AutoJs6 6.8.0 ビルド (バージョンコード 5276 以降) が必要です
- `追加` 完全な標準 `.z01 + .zip` セットと最新 WinRAR の `partN.rar` セットをネイティブページで閲覧、プレビュー、展開できるようになりました。分割アーカイブは読み取り専用です
- `追加` Explorer Action v12 はホストが承認した兄弟ボリュームの上限付きカタログだけを渡し、ディレクトリやファイルシステムパスを公開せず、不透明 ID ごとに読み取り専用記述子を開きます
- `修正` Android 7 以降で分割 ZIP 最終ボリュームのディレクトリ情報を正しく受け入れ、最終ボリュームを破損扱いせずに許可済み全ボリュームからデータを読み取ります
- `修正` RAR 分割セグメントの CRC を再構成済みデータと比較しないよう修正しました。欠落またはオープン後に変更されたボリュームは安定した型付きエラーになります
- `改善` ボリューム数、名前、ID、オープン回数、呼び出し元 UID、ファイル同一性、セッション寿命を制限して再検証し、中断されたステージングは私有キャッシュの断片をすべて削除します

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
