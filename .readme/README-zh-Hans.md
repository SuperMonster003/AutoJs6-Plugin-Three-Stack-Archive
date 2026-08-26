<div align="center">
  <p>
    <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="Archive Manager" width="128" />
  </p>

  <h1>Archive Manager</h1>

  <p>深度集成 AutoJs6 文件管理器的通用压缩档案管理插件, 支持安全浏览、解压、创建与受控修改</p>

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

Archive Manager 直接工作在 AutoJs6 文件管理器内, 不再另造一套文件列表. 受支持档案复用宿主的列表、路径栏、主题、预览器、多选、进度与目录刷新. 只有需要更多设置或写入操作时才进入独立管理页.

### 当前能力

- 在 AutoJs6 原生文件列表中浏览 ZIP/JAR/AAR/WAR、7Z、RAR4/RAR5 与 TAR 系列档案, 并复用内部路径栏、搜索、排序和返回导航.
- 直接使用宿主现有的文档、图片、音频和视频预览器读取可用条目, 无需先解压整个档案.
- 可解压整个档案、当前内部目录或勾选条目, 支持进度、取消、安全冲突编号、发布前校验与提交前回滚.
- 加密 ZIP、7Z 与 RAR 可在首次打开或解压时使用宿主原生密码框; 密码错误后可原位重试, 不会丢失当前档案路径.
- 可通过宿主有界授权的同级卷描述符浏览、预览和解压完整的标准 `.z01 + .zip` 卷组与现代 WinRAR `partN.rar` 卷组; 缺卷或卷变化会返回明确错误.
- 可从单项或同父级多选创建 ZIP、7Z、TAR、TAR.GZ、TAR.XZ、TAR.BZ2 与 TAR.ZST; ZIP 另支持 AES-256 与标准分卷, 多选也可为每项单独创建档案.
- 普通单卷 ZIP 可通过校验重建执行添加文件、导入完整目录树、新建空目录、重命名和删除; 只有完整读回通过后才原子替换源档案.
- 危险档案名称保持只读隔离, 写出前继续执行结构与资源检查; 对可 seek 的宿主描述符优先直接读取, 避免无必要的整包复制.

### 当前支持

当前版本识别以下可浏览与解压的扩展名:

```text
zip, jar, aar, war, 7z, rar, tar, tar.gz, tgz, tar.xz, txz, tar.bz2, tbz2, tar.zst, tzst
```

当前版本可创建以下格式:

```text
zip, 7z, tar, tar.gz, tar.xz, tar.bz2, tar.zst
```

> 原生集成需要配套的 AutoJs6 6.8.0 Explorer Action v12 构建 (版本代码 5276 或更高). RAR 与分卷档案明确保持只读, 当前只有普通单卷 `.zip` 可修改. 标准分卷 ZIP 应从最终 `.zip` 打开, 现代 WinRAR 卷组应从首个 `partN.rar` 打开, 且全部必需同级卷须位于同一目录. 编号 `.zip.001`、分卷 7Z、创建时加密文件名、压缩后删除源文件, 以及修改 7Z、RAR 和 TAR 系列档案均不是当前能力.

### 使用方法

1. 安装 Archive Manager, 并在 AutoJs6 插件中心启用它.
2. 点击档案文件的主操作按钮, 或从菜单选择“打开压缩档案”. 随后可像浏览普通目录一样使用宿主路径栏.
3. 使用路径栏右侧动作解压当前内部目录; 长按条目可多选解压; 文件菜单中的“解压到...”用于解压整个档案. 需要密码时宿主会自动提示.
4. 对文件或文件夹选择“压缩...”; 也可在同一目录多选后使用底部操作栏中的“压缩...”.
5. 仅在需要添加、重命名或删除内容时, 对普通单卷 ZIP 选择“管理压缩档案...”.

### 权限与数据

Archive Manager 不申请存储或网络权限. 宿主只提供短生命周期只读描述符和固定插件 UID 的输出事务, 插件不能自行选择任意文件系统路径. Explorer Action v11 仅在有界同步重试请求中传递密码, 双方立即移除并清理保留缓冲, 且从不持久化. Explorer Action v12 仅增加会话级、有界的宿主批准同级卷目录: 插件获得不透明 ID 而不是路径, 调用方 UID、文件身份、大小、修改时间与生命周期均在使用前重复校验. Android 与 Java 库仍可能产生无法完全控制的短生命周期运行时副本, 因此密码清理属于尽力而为, 不是绝对保证. 路径穿越和危险名称始终隔离, 输出在发布前完成校验, 资源预算确认也不会关闭结构安全检查.

### Roadmap

剩余工作继续以可勾选项目追踪: 宿主原生文件名编码修正、编号 `.zip.001` 与分卷 7Z 调研、普通 ZIP 之外的可写重建、撤销或源文件删除事务、无障碍复核, 以及其余设备与生产工具矩阵.

- [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/ROADMAP.md)

### 版本记录

#### v2.5.0

_2026/08/27_

- `提示` 本版本需要配套的 AutoJs6 6.8.0 Explorer Action v12 构建 (版本代码 5276 或更高)
- `新增` 完整的标准 `.z01 + .zip` 卷组与现代 WinRAR `partN.rar` 卷组现可在宿主原生档案页浏览、预览和解压; 分卷档案仍保持只读
- `新增` Explorer Action v12 只向插件提供有界的宿主批准同级卷目录, 每一卷均通过不透明 ID 打开为只读描述符, 不暴露目录或文件系统路径
- `修复` Android 7 及更高版本现可正确接受分卷 ZIP 最终卷中的目录元数据, 并跨全部已授权卷读取条目内容, 不再把最终卷误判为损坏
- `修复` RAR 分卷片段 CRC 不再与重组后的完整条目数据比较; 缺卷以及打开后发生变化的卷会返回稳定的类型化错误
- `优化` 卷数量、名称、ID、打开次数、调用方 UID、文件身份和会话生命周期均受到限制并重复校验; 暂存中断会删除全部私有缓存残片

#### v2.4.0

_2026/08/26_

- `提示` 本版本需要 AutoJs6 6.8.0 Explorer Action v11, 版本代码 5276 或更高
- `新增` 支持浏览、预览和解压 RAR4/RAR5, 包括内容加密与头部加密档案; RAR 明确保持只读
- `新增` AutoJs6 原生档案页可在首次打开或解压过程中请求密码, 并在不丢失当前路径或选择的情况下重试
- `修复` RAR 分卷首卷仍可显示可读元数据, 但同级卷不可用时不再展示虚假的解压入口
- `修复` 密码错误后会清空上次输入, 并针对未变化的档案快照重试, 无需离开原生页面
- `优化` RAR 在可用时直接读取宿主的可 seek 描述符, 不增加原生 ABI, 并复用其他格式的路径、资源与输出安全检查
- `依赖` 附加 Junrar 8.1.0 与 SLF4J 2.0.17, 在随附许可条款下提供只读 RAR 支持

#### v2.3.0

_2026/08/26_

- `提示` 本版本需要配套的 AutoJs6 6.8.0 Explorer Action v10 构建 (版本代码 5276 或更高)
- `新增` 宿主原生档案页现可从路径栏解压当前内部目录, 或从选择栏解压勾选条目, 无需打开独立管理页
- `新增` 原生解压通过宿主所有的目录输出树写入, 支持进度、取消、安全冲突编号和 Explorer 自动刷新
- `修复` Android 7 离开档案时, 宿主清理预览缓存不再导致崩溃
- `修复` 五项文件选择栏的文字在窄屏上会居中显示于图标下方
- `优化` 档案选择模式仅显示“退出”和“解压”, 隐藏不适用于档案内部的文件系统操作

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
- 第三方软件声明: [THIRD_PARTY_NOTICES.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/THIRD_PARTY_NOTICES.md)
- Android Storage Access Framework: https://developer.android.com/guide/topics/providers/document-provider
