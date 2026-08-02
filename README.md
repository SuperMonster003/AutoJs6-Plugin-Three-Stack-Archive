<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-archive-browser-ic-launcher" border="0" width="128" />
  </p>

  <p>为 AutoJs6 文件浏览器提供 ZIP 系列压缩包只读浏览与选择性 SAF 解压</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Archive-Browser?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Archive-Browser?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Archive-Browser?color=534BAE&label=License"/></a>
  </p>
</div>

******

### 语言 (Languages)

******

当前 README.md 支持以下语言:

- 简体中文 [zh-Hans] # 当前
- [香港繁體 [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/.readme/README-zh-Hant-HK.md)
- [台灣繁體 [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/.readme/README-ar.md)

******

### 简介

******

AutoJs6 压缩包浏览器插件为 AutoJs6 文件浏览器提供只读压缩包浏览功能. 插件在专用的层级查看器中打开基于 ZIP 的容器, 并且只将用户选择的条目解压到通过 Android Storage Access Framework 选定的输出文件夹.

******

### 功能

******

- 通过共享的 `org.autojs.plugin.EXPLORER_ACTION` 协议注册单文件只读文件浏览器溢出菜单动作.
- 浏览压缩包目录, 并显示解压后大小, 压缩后大小, CRC 和修改时间元数据.
- 搜索规范化条目路径, 支持选择单个文件, 文件夹或全部可见条目.
- 将所选条目解压到用户选定的 SAF 目录树, 并提供进度报告与取消操作.
- 接受使用受支持 ZIP 压缩方法的 ZIP, JAR, AAR 和 WAR 容器.
- 将只读输入暂存到应用私有缓存, 并在查看器关闭时移除临时数据.

******

### 支持的格式

******

版本 1 识别以下 ZIP 系列文件扩展名:

```text
zip, jar, aar, war
```

******

### 插件接口

******

AutoJs6 通过以下身份发现并执行插件:

```text
service action: org.autojs.plugin.EXPLORER_ACTION
execute action: org.autojs.plugin.EXPLORER_ACTION_EXECUTE
plugin id: archive-browser
engine: explorer-action
variant: default
```

版本 1 仅在 AutoJs6 主文件浏览器中提供单文件只读溢出菜单动作.

******

### 安全性

******

插件不申请存储或网络权限. 宿主仅临时授予输入 content URI 的只读权限, 输出权限则限制在用户明确选择的 SAF 目录. 插件会拒绝绝对路径, 父目录穿越, 驱动器前缀, 反斜杠, 不安全 Unicode, 重复路径, 文件与目录冲突, 不受支持的压缩方法, 大小不匹配和 CRC 不匹配.

******

### 安全限制

******

- 暂存输入最大值: `4 GiB`.
- 压缩包路径节点最大数量, 包含隐式目录: `20,000`.
- ZIP 中央目录最大值: `64 MiB`.
- 规范化路径最大长度: `1,024` 个字符.
- 路径最大深度: `64` 层.
- 单个条目解压后最大值: `512 MiB`.
- 解压后总数据最大值: `2 GiB`.
- 最大压缩比: `1000:1`.

******

### 发行历史

******

# v1.0.0

###### 2026/08/02

* `新增` 压缩包浏览器插件, 插件 ID 为 `archive-browser`, 引擎为 `explorer-action`, 变体为 `default`
* `新增` 面向 ZIP, JAR, AAR 和 WAR 容器的单文件只读文件浏览器动作, 包含层级浏览, 路径搜索和条目选择
* `新增` 将所选条目解压到用户选择的 SAF 目录树, 支持进度与取消, 仅临时读取输入, 不申请存储或网络权限
* `新增` 安全限制为输入 4 GiB, 20,000 个路径节点并包含隐式目录, ZIP 中央目录 64 MiB, 单个条目解压后 512 MiB, 解压后总数据 2 GiB, 压缩比 1000:1
* `新增` 针对不安全路径, 重复和冲突条目, 不受支持的压缩方法, 输入源变化, 大小不匹配和 CRC 不匹配的校验
* `新增` 插件元数据, 界面文本, 使用说明, README 和 CHANGELOG 的多语言资源: 西班牙语/法语/俄语/阿拉伯语/日语/韩语/英语/简体中文/香港繁体/台湾繁体

##### 更多发行历史可参阅

* [CHANGELOG-zh-Hans.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/app/src/main/assets/doc/CHANGELOG-zh-Hans.md)

******

### 构建

******

```powershell
.\gradlew.bat :app:assembleDebug
```

Release 构建:

```powershell
.\gradlew.bat :app:assembleRelease
```

构建参数来自 `version.properties`, 当前最低 SDK 为 24, 目标 SDK 为 36.

******

### 资源结构

******

```text
.readme/lang_*.json
.changelog/lang_*.json
.python/generate_markdown.py
app/src/main/assets/doc/CHANGELOG-*.md
app/src/main/res/values-*/strings.xml
app/src/main/res/values-*/plurals.xml
app/src/main/res/raw-*/plugin_instruction.md
```

`strings.xml` 提供插件元数据和浏览器固定文本的本地化, `plurals.xml` 提供浏览器数量文本的本地化. `plugin_instruction.md` 提供宿主侧展示的使用说明. README 与 CHANGELOG 由 `.python/generate_markdown.py` 根据 JSON 源文件生成.

******

### 相关链接

******

- AutoJs6 文档: https://docs.autojs6.com
- Android Storage Access Framework: https://developer.android.com/guide/topics/providers/document-provider
