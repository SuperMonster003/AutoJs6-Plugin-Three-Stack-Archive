<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-archive-browser-ic-launcher" border="0" width="128" />
  </p>

  <p>AutoJs6 エクスプローラー向けの ZIP 系アーカイブ読み取り専用参照と選択的 SAF 展開</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Archive-Browser?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Archive-Browser?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Archive-Browser?color=534BAE&label=License"/></a>
  </p>
</div>

******

### 言語 (Languages)

******

現在の README.md は次の言語に対応しています:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/.readme/README-zh-Hans.md)
- [香港繁體 [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/.readme/README-zh-Hant-HK.md)
- [台灣繁體 [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/.readme/README-es.md)
- 日本語 [ja] # 現在
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/.readme/README-ar.md)

******

### 概要

******

AutoJs6 Archive Browser プラグインは AutoJs6 エクスプローラーに読み取り専用のアーカイブ参照機能を追加します. ZIP ベースのコンテナーを専用の階層ビューアーで開き, ユーザーが選択したエントリーだけを Android Storage Access Framework で指定した出力フォルダーに展開します.

******

### 機能

******

- 共有 `org.autojs.plugin.EXPLORER_ACTION` プロトコルを通して単一ファイル向けの読み取り専用オーバーフローアクションを登録します.
- アーカイブのフォルダーを参照し, 展開後サイズ, 圧縮サイズ, CRC, 更新日時のメタデータを表示します.
- 正規化したエントリーパスを検索し, 個別のファイル, フォルダー, 表示中の全エントリーを選択できます.
- 選択したエントリーをユーザー指定の SAF ツリーへ展開し, 進捗表示とキャンセルに対応します.
- 対応する ZIP 圧縮方式を使った ZIP, JAR, AAR, WAR コンテナーを受け付けます.
- 読み取り専用入力をプライベートキャッシュに一時保存し, ビューアー終了時に一時データを削除します.

******

### 対応形式

******

バージョン 1 は次の ZIP 系ファイル拡張子を認識します:

```text
zip, jar, aar, war
```

******

### プラグインインターフェース

******

AutoJs6 は次の識別情報でプラグインを検出して実行します:

```text
service action: org.autojs.plugin.EXPLORER_ACTION
execute action: org.autojs.plugin.EXPLORER_ACTION_EXECUTE
plugin id: archive-browser
engine: explorer-action
variant: default
```

バージョン 1 は AutoJs6 のメインエクスプローラーにおける単一ファイル向け読み取り専用オーバーフローアクションに限定されます.

******

### セキュリティ

******

プラグインはストレージ権限もネットワーク権限も要求しません. ホストは入力 content URI への一時的な読み取り専用アクセスだけを付与し, 出力アクセスはユーザーが明示的に選択した SAF ディレクトリに限定されます. 絶対パス, 親ディレクトリ移動, ドライブ接頭辞, バックスラッシュ, 安全でない Unicode, 重複パス, ファイルとディレクトリの競合, 未対応の圧縮方式, サイズ不一致, CRC 不一致は拒否されます.

******

### 安全制限

******

- 一時保存する入力の最大サイズ: `4 GiB`.
- 暗黙のディレクトリを含むアーカイブパスノードの最大数: `20,000`.
- ZIP 中央ディレクトリの最大サイズ: `64 MiB`.
- 正規化パスの最大長: `1,024` 文字.
- パスの最大深さ: `64` セグメント.
- 単一エントリーの展開後最大サイズ: `512 MiB`.
- 展開後データの合計最大サイズ: `2 GiB`.
- 最大圧縮率: `1000:1`.

******

### リリース履歴

******

# v1.0.0

###### 2026/08/02

* `機能` プラグイン ID `archive-browser`, エンジン `explorer-action`, バリアント `default` の Archive Browser プラグイン
* `機能` ZIP, JAR, AAR, WAR コンテナー向けの単一ファイル読み取り専用エクスプローラーアクション, 階層参照, パス検索, エントリー選択
* `機能` ユーザー指定の SAF ツリーへの選択的な展開, 進捗とキャンセル, 一時的な読み取り専用入力アクセス, ストレージ権限とネットワーク権限なし
* `機能` 入力 4 GiB, 暗黙のディレクトリを含む 20,000 パスノード, ZIP 中央ディレクトリ 64 MiB, 展開後の単一エントリー 512 MiB, 展開後の合計 2 GiB, 圧縮率 1000:1 の安全制限
* `機能` 安全でないパス, 重複または競合するエントリー, 未対応の圧縮方式, 入力元の変更, サイズ不一致, CRC 不一致の検証
* `機能` スペイン語/フランス語/ロシア語/アラビア語/日本語/韓国語/英語/簡体字中国語/香港繁体字中国語/台湾繁体字中国語のメタデータ, UI, 使用説明, README, CHANGELOG

##### その他のリリース履歴

* [CHANGELOG-ja.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/app/src/main/assets/doc/CHANGELOG-ja.md)

******

### ビルド

******

```powershell
.\gradlew.bat :app:assembleDebug
```

Release ビルド:

```powershell
.\gradlew.bat :app:assembleRelease
```

ビルドパラメーターは `version.properties` から取得します. 現在の最小 SDK は 24, ターゲット SDK は 36 です.

******

### リソース構成

******

```text
.readme/lang_*.json
.changelog/lang_*.json
.python/generate_markdown.py
app/src/main/assets/doc/CHANGELOG-*.md
app/src/main/res/values-*/strings.xml
app/src/main/res/raw-*/plugin_instruction.md
```

`strings.xml` はプラグインメタデータとブラウザー UI をローカライズし, `plugin_instruction.md` はホストに表示する使用説明を提供します. README と CHANGELOG は `.python/generate_markdown.py` が JSON ソースから生成します.

******

### リンク

******

- AutoJs6 ドキュメント: https://docs.autojs6.com
- Android Storage Access Framework: https://developer.android.com/guide/topics/providers/document-provider
