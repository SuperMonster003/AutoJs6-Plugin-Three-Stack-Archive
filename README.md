<div align="center">
  <p>
    <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="Archive Manager" width="128" />
  </p>

  <h1>Archive Manager</h1>

  <p>AutoJs6 文件管理器插件，用于打开、解压和创建 ZIP 压缩档案</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Archive-Manager?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Archive-Manager?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Archive-Manager?color=534BAE&label=License"/></a>
  </p>
</div>

### 语言 (Languages)

README 提供以下语言版本:

- 简体中文 [zh-Hans] # 当前
- [香港繁體 [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-zh-Hant-HK.md)
- [台灣繁體 [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-ar.md)

### 项目简介

压缩档案管理器把 ZIP 浏览、解压和创建能力带入 AutoJs6 文件管理器。当前版本可在宿主原生文件列表中浏览档案、显示外部与内部路径、预览支持的条目，并支持单项和同父级多选压缩；更多格式、按条目解压和档案内修改继续按 Roadmap 推进。

### 当前能力

- 直接在 AutoJs6 原生文件列表中打开 ZIP 系列档案，沿用宿主主题、暗色模式和动态色。
- 路径栏同时显示外部目录、档案名和内部目录；可点击层级跳转，返回键会先返回内部上一级，再退出档案。
- 使用宿主现有预览器打开支持的文档、图片、音频和视频条目。
- 通过“解压到...”快捷动作直接解压整个档案，无需先进入浏览页面。
- 按目录浏览、搜索和排序档案内容。
- 只读取目录元数据即可显示列表，不会为了打开页面预先解压全部内容。
- 根据真实 ZIP 结构确认格式，并统一控制预览、解压和创建能力；未支持的选项保持禁用。
- 兼容 Zip64、自解压式前导数据、传统文件名编码和 Windows 风格路径分隔符。
- 为普通文件、文件夹以及同父级多选提供“压缩...”动作。
- 创建 ZIP 时可设置文件名和压缩级别；单项默认使用目标名，多选默认使用父文件夹名。
- 输出先写入同目录临时文件，再原子提交；名称冲突时自动添加序号，既有文件不会被覆盖。

### 当前支持

当前版本识别以下 ZIP 系列扩展名:

```text
zip, jar, aar, war
```

当前版本可创建以下格式:

```text
zip
```

> Explorer Action v6 原生浏览与条目预览以及 v4 压缩集成需要 AutoJs6 版本代码 5276 或更高。按内部条目解压、7z、tar 族、密码、分卷、文件名加密、单独压缩、压缩后删除源文件以及档案内添加/删除尚未发布；请以 Roadmap 的勾选状态为准。

### 使用方法

1. 安装插件，并在 AutoJs6 插件中心启用它。
2. 在文件管理器中打开 ZIP、JAR、AAR 或 WAR 文件的菜单。
3. 选择“打开压缩档案”，在宿主文件列表中进入目录、搜索或使用路径栏跳转。
4. 若要解压整个档案，请从文件菜单选择“解压到...”，再通过 Android 系统选择器指定输出目录。
5. 要创建 ZIP，请打开普通文件或文件夹的菜单并选择“压缩...”；也可先多选同一目录中的项目，再使用底部“压缩...”动作。

### 权限与数据

插件不申请存储或网络权限。原生浏览使用绑定到宿主 UID 的短期只读档案会话，关闭页面或解绑后清理暂存输入；解压只使用宿主临时授予的输入 URI。创建 ZIP 通过绑定到插件 UID 的宿主文件会话分页读取目标，并且只能在当前父目录执行事务式输出。固定 4 GiB 输入上限和浏览阶段的解压大小/压缩比门槛已取消；路径穿越防护、目标隔离、完整性校验和失败清理仍然保留。

### Roadmap

更多格式、密码与分卷、按条目解压、档案修改及完整设备矩阵的任务与验收条件集中维护在 Roadmap 中。未勾选项目不代表当前版本已经支持。

- [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/ROADMAP.md)

### 版本记录

#### Unreleased

_未发布_

- `新增` 产品名称统一为“压缩档案管理器”，打开动作调整为“打开压缩档案”
- `新增` 通过 Explorer Action v5 在 AutoJs6 原生文件列表中浏览档案，并复用路径栏、主题和返回导航
- `新增` 通过 Explorer Action v6 使用宿主的文档、图片、音频和视频预览器打开支持的档案条目
- `新增` “解压到...”快捷动作，可直接选择目录并解压整个档案
- `新增` 接入 Explorer Action v4，在普通文件、文件夹及同父级多选的五项操作栏中提供“压缩...”
- `新增` 新增 ZIP 创建表单，支持默认命名、压缩级别、进度、取消和同名自动编号
- `修复` 改用目录元数据快速打开 ZIP，并兼容自解压式前导数据、传统文件名编码、Windows 路径分隔符及更多可读取的 ZIP 方法
- `修复` 接受未知或不精确的文件大小、合法 DocumentsProvider URI 和宿主附加的写入授权，避免有效档案在解析前被拒绝
- `修复` 修复 Android 7.x 因调用新系统专有 API 而无法浏览或解压 ZIP 的问题
- `优化` 移除固定 4 GiB 输入上限和浏览阶段的解压大小/压缩比门槛，同时保留路径隔离、完整性校验和失败清理
- `优化` 新增可逐项追踪的 Roadmap，并重写 README 与 CHANGELOG，使当前能力和后续计划清楚分开
- `优化` 独立页面改为遵循系统日夜模式和 Material 动态色
- `优化` ZIP 输出通过绑定插件 UID 的宿主会话写入同目录临时文件后原子提交，无需存储权限且不会覆盖既有文件
- `优化` 统一校验格式和条目的预览、解压与创建能力，使不可用选项保持禁用

#### v1.0.1

_2026/08/08_

- `修复` 插件中心启用插件时服务绑定为空的问题
- `优化` 简化插件名称、描述和使用说明

#### v1.0.0

_2026/08/02_

- `新增` 首个可用版本，支持从文件管理器浏览 ZIP、JAR、AAR 和 WAR，并解压所选文件或文件夹
- `新增` 提供搜索、选择、进度、取消、临时输入清理和多语言界面

##### 完整记录

* [CHANGELOG-zh-Hans.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/app/src/main/assets/doc/CHANGELOG-zh-Hans.md)

### 构建

```powershell
.\gradlew.bat :app:assembleDebug
```

Release 构建:

```powershell
.\gradlew.bat :app:assembleRelease
```

在项目根目录使用 Gradle Wrapper；SDK 与 JDK 要求以 `version.properties` 为准.

### 相关链接

- AutoJs6 文档: https://docs.autojs6.com
- Android Storage Access Framework: https://developer.android.com/guide/topics/providers/document-provider
