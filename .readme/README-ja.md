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
- 単一項目または同じ親の選択から ZIP、7Z、TAR、TAR.GZ、TAR.XZ、TAR.BZ2、TAR.ZST を作成できます。ZIP は AES-256、標準分割、項目ごとの個別作成にも対応します。
- 通常の単一ボリューム ZIP は、ファイルやフォルダーツリーの追加、空フォルダー作成、名前変更、削除を検証付き再構築で行い、完全な再読み取り後にだけ元ファイルを原子的に置換します。
- 危険な名前を読み取り専用で隔離し、書き込み前に構造とリソース制限を適用し、可能ならホストの seek 可能な記述子から直接読み取ります。

### 現在の形式

現行版は次の参照・展開可能な拡張子を認識します:

```text
zip, jar, aar, war, 7z, rar, tar, tar.gz, tgz, tar.xz, txz, tar.bz2, tbz2, tar.zst, tzst
```

現行版は次の形式を作成できます:

```text
zip, 7z, tar, tar.gz, tar.xz, tar.bz2, tar.zst
```

> ネイティブ統合には Explorer Action v11 を備えた AutoJs6 6.8.0、バージョンコード 5276 以降が必要です。RAR は意図的に読み取り専用です。編集できるのは通常の単一ボリューム `.zip` だけです。ホストが選択ファイルの記述子しか渡さないため、既存の分割 ZIP/RAR はまだ完全な一組として読み取れません。RAR の先頭ボリュームはメタデータを表示できますが、展開は無効です。作成時のファイル名暗号化、圧縮後の元ファイル削除、7Z/RAR/TAR 系の内部編集には未対応です。

### 使い方

1. Archive Manager をインストールし、AutoJs6 プラグインセンターで有効にします。
2. 対応ファイルの主操作をタップするか、アーカイブを開くを選択し、ホストのパスバーで通常のフォルダーと同様に閲覧します。
3. パスバーの操作で現在の内部フォルダーを展開し、長押しで選択項目を展開するか、ファイルメニューの展開先...でアーカイブ全体を展開します。必要な場合はパスワード画面が表示されます。
4. ファイルまたはフォルダーで圧縮...を選ぶか、同じディレクトリの複数項目を選択して下部バーの圧縮...を使います。
5. 内容の追加、名前変更、削除が必要な場合だけ、通常の単一ボリューム ZIP でアーカイブを管理...を選びます。

### 権限とデータ

Archive Manager はストレージ権限もネットワーク権限も要求しません。ホストは短時間の読み取り専用記述子と、プラグイン UID に固定した出力トランザクションだけを渡すため、プラグインは任意のファイルシステムパスを選べません。Explorer Action v11 はパスワードを上限付き同期再試行要求だけで運び、ホストとプラグインは保持したバッファーを直ちに削除して消去し、状態、設定、ログ、診断には保存しません。ただし Android や Java ライブラリが避けられない短命な実行時コピーを作る可能性はあるため、これは最善努力のメモリー衛生であり絶対保証ではありません。危険なパスは隔離され、出力は公開前に検証され、リソース予算の確認でも構造安全検査は無効になりません。

### Roadmap

残作業はチェック可能な項目として追跡します。分割 ZIP/RAR の兄弟ボリューム入力、ホスト内でのファイル名エンコーディング修正、通常 ZIP 以外の書き込み再構築、元ファイル削除または取り消しトランザクション、アクセシビリティ、残りの端末と生成ツールの組み合わせです。

- [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/ROADMAP.md)

### リリースノート

#### v2.4.0

_2026/08/26_

- `注記` このバージョンには Explorer Action v11 を備えた AutoJs6 6.8.0、バージョンコード 5276 以降が必要です
- `追加` RAR4/RAR5 の閲覧、プレビュー、展開に対応し、内容暗号化とヘッダー暗号化も扱えます。RAR は意図的に読み取り専用です
- `追加` AutoJs6 のネイティブアーカイブページが初回オープンまたは展開中にパスワードを要求し、現在のパスや選択を失わず再試行できます
- `修正` 分割 RAR の先頭ボリュームは読み取り可能なメタデータを保持しますが、兄弟ボリュームがない場合は展開を表示しなくなりました
- `修正` 誤ったパスワードは前回入力を消去し、ネイティブページを離れず変更されていないスナップショットに対して再試行します
- `改善` RAR は可能な場合にホストの seek 可能な記述子から直接読み取り、ネイティブ ABI を追加せず、共通の安全検査を再利用します
- `依存関係` 同梱ライセンス条件の下で読み取り専用 RAR を提供する Junrar 8.1.0 と SLF4J 2.0.17 を追加しました

#### v2.3.0

_2026/08/26_

- `注記` このバージョンには Explorer Action v10 対応の AutoJs6 6.8.0 ビルド (バージョンコード 5276 以降) が必要です
- `追加` ネイティブアーカイブ画面で、別の管理画面を開かずにパスバーから現在の内部フォルダー、または選択バーから選択項目を展開できるようになりました
- `追加` ネイティブ展開はホスト所有の出力ツリーへ書き込み、進行表示、取り消し、安全な競合番号付け、Explorer の自動更新に対応します
- `修正` Android 7 でアーカイブを離れる際、ホストのプレビューキャッシュ消去中にクラッシュする問題を修正しました
- `修正` 狭い画面で 5 アクションのファイル選択バーのラベルがアイコンの下に中央揃えされるようになりました
- `改善` アーカイブ選択モードでは「終了」と「展開」だけを表示し、アーカイブ内部に適用できないファイルシステム操作を非表示にしました

#### v2.2.0

_2026/08/26_

- `注記` このバージョンには Explorer Action v9 を含む対応 AutoJs6 6.8.0 ビルド (バージョンコード 5276 以降) が必要
- `追加` 「展開先...」で現在のフォルダーを推奨し, ホスト所有のディレクトリトランザクションで同名の出力フォルダーを作成; 同等名が存在する場合は内容を変更せず安全に採番
- `追加` 通常の単一ボリューム ZIP 管理で Android のシステム選択画面からフォルダー全体を取り込み可能, ネストしたファイルと空フォルダーにも対応
- `修正` 展開先選択画面が幅の狭い画面や高さの低い画面でも完全に使用でき, 正確な既定パスを表示
- `修正` 同じディレクトリへの展開がキャンセル, 失敗, 中断または容量不足になった場合に未公開出力をロールバック; 次のセッションで中断されたホストトランザクションを復旧し, 元のアーカイブは変更しない
- `改善` Explorer Action v9 が検証済みディレクトリツリーをアトミックに公開し, commit 直後に AutoJs6 の新しい出力フォルダーを更新

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
