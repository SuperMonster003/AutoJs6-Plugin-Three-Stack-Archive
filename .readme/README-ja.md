<div align="center">
  <p>
    <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="Archive Manager" width="128" />
  </p>

  <h1>Archive Manager</h1>

  <p>ZIP アーカイブを開く・展開する・作成するための AutoJs6 ファイルマネージャープラグイン</p>

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

Archive Manager は ZIP の参照、展開、作成を AutoJs6 ファイルマネージャーに統合します。現行版は外部と内部のパスを示しながらホストのネイティブ一覧でアーカイブを参照でき、単一項目または同じ親フォルダー内の複数選択を圧縮できます。追加形式、項目プレビュー、内部編集は Roadmap に沿って進めます。

### 現在利用できる機能

- ZIP 系アーカイブを AutoJs6 のネイティブファイル一覧で直接開き、ホストのテーマ、ダークモード、ダイナミックカラーを使用。
- パスバーに外部フォルダー、アーカイブ名、内部フォルダーを表示。階層をタップして移動し、戻る操作で内部の上位へ戻ってからアーカイブを終了。
- 「展開先...」ショートカットから、参照画面を開かずにアーカイブ全体を展開。
- アーカイブ内のフォルダーを参照、検索、並べ替え。
- 全項目を事前展開せず、ディレクトリメタデータから一覧を作成。
- Zip64、自己展開形式の前置データ、旧式の名前エンコーディング、Windows 区切り文字に対応。
- 通常のファイル、フォルダー、同じ親フォルダー内の複数選択に「圧縮...」を表示。
- ファイル名と圧縮レベルを指定して ZIP を作成。単一項目は対象名、複数項目は親フォルダー名を既定値として使用。
- 同じフォルダーの一時ファイルへ書き込んでからアトミックに確定し、既存ファイルを上書きせず競合名へ番号を追加。

### 現在の形式

現行版は次の ZIP 系拡張子を認識します:

```text
zip, jar, aar, war
```

現行版は次の形式を作成できます:

```text
zip
```

> Explorer Action v5 のネイティブ参照と v4 の圧縮には AutoJs6 バージョンコード 5276 以降が必要です。アーカイブ内ファイルのプレビュー、項目単位の展開、7z、tar 系、パスワード、分割、ファイル名暗号化、個別アーカイブ、元項目の削除、アーカイブ内の追加/削除はまだ公開済み機能ではありません。Roadmap のチェック状態を基準にしてください。

### 使い方

1. プラグインをインストールし、AutoJs6 プラグインセンターで有効にします。
2. ZIP、JAR、AAR、WAR ファイルのメニューを開きます。
3. 「アーカイブを開く」を選び、ホストのファイル一覧でフォルダーを開く、検索する、またはパスバーから移動します。
4. 全体を展開する場合は、ファイルメニューから「展開先...」を選び、Android のシステム選択画面で出力フォルダーを指定します。
5. ZIP を作成するには通常のファイルまたはフォルダーのメニューで「圧縮...」を選ぶか、同じフォルダーの複数項目を選択して下部バーの「圧縮...」を使用します。

### 権限とデータ

ストレージ権限やネットワーク権限は要求しません。ネイティブ参照はホスト UID に固定された短期の読み取り専用アーカイブセッションを使い、画面の終了または切断時に一時入力を削除します。展開はホストが一時付与した入力 URI だけを使います。ZIP 作成はプラグイン UID に固定されたホストファイルセッションで対象をページ単位に読み、現在の親フォルダーにのみトランザクション出力を作成できます。固定 4 GiB 入力上限と参照時のサイズ/圧縮率制限は削除しましたが、パス隔離、整合性検証、失敗時の清掃は維持します。

### Roadmap

追加形式、パスワードと分割、内部プレビューと項目単位の展開、アーカイブ編集、完全な端末マトリックスのタスクと受け入れ条件は Roadmap にあります。未チェック項目は現行機能ではありません。

- [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/ROADMAP.md)

### リリースノート

#### Unreleased

_未公開_

- `追加` 製品名を Archive Manager、ファイル操作を「アーカイブを開く」に統一
- `追加` Explorer Action v5 により既存のパスバー、テーマ、戻る操作を使って AutoJs6 のネイティブ一覧でアーカイブを参照
- `追加` 出力先を選んで全体を展開する「展開先...」ショートカット
- `追加` Explorer Action v4 により通常のファイル/フォルダーメニューと同じ親フォルダー内の五項目複数選択バーへ「圧縮...」を追加
- `追加` 既定名、圧縮レベル、進捗、キャンセル、競合名の自動番号付けに対応した ZIP 作成
- `修正` ZIP 一覧をメタデータ方式に変更し、自己展開形式の前置データ、旧式の名前エンコーディング、Windows 区切り文字、追加の読取可能メソッドに対応
- `修正` 不明なサイズ、有効な DocumentsProvider URI、ホストの追加書込権限で有効なアーカイブが拒否される問題を修正
- `修正` 新しいシステム専用 API の呼び出しにより Android 7.x で ZIP を参照・展開できない問題を修正
- `改善` 固定 4 GiB 上限と参照時のサイズ/圧縮率制限を削除し、パス隔離と整合性検証は維持
- `改善` チェック可能な Roadmap を追加し README と CHANGELOG を全面更新
- `改善` 独立画面がシステムの昼夜モードと Material 動的色に追従
- `改善` ZIP 出力をプラグイン UID に固定したホストセッションで同じフォルダーの一時ファイルへ書き込み、ストレージ権限や上書きなしでアトミックに確定

#### v1.0.1

_2026/08/08_

- `修正` プラグイン有効化時にサービスバインドが空になる問題
- `改善` 名前、説明、使用手順を簡潔化

#### v1.0.0

_2026/08/02_

- `追加` ZIP、JAR、AAR、WAR の参照と選択内容の展開に対応した初回版
- `追加` 検索、選択、進捗、キャンセル、一時データ清掃、多言語 UI を追加

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
- Android Storage Access Framework: https://developer.android.com/guide/topics/providers/document-provider
