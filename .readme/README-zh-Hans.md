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

Archive Manager 直接工作在 AutoJs6 文件管理器内, 不再另造一套文件列表. 受支持档案复用宿主的列表、路径栏、主题、预览器、多选、进度与目录刷新. 独立管理页只用于需要更完整表单的详细格式信息与附加设置.

### 界面截图

以下 Android 真实界面截图展示宿主菜单集成、原生档案浏览、档案创建与详细管理, 仅使用可公开的合成数据.

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

- 截图说明与完整集合: [docs/images/screenshots/README.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/docs/images/screenshots/README.md)

### 当前能力

- 在 AutoJs6 原生文件列表中浏览 ZIP/JAR/AAR/WAR、7Z、RAR4/RAR5 与 TAR 系列档案, 并复用内部路径栏、搜索、排序和返回导航.
- 文件名称未命中受支持档案时, 提供显式的“作为压缩档案打开...”复核, 并在路径栏标注探测到的实际格式; TAR 复合后缀按完整名称精确匹配, 不会把普通 `.gz`、`.xz`、`.bz2` 或 `.zst` 压缩流显示为档案.
- 直接使用宿主现有的文档、图片、音频和视频预览器读取可用条目, 无需先解压整个档案.
- 可解压整个档案、当前内部目录或勾选条目, 支持进度、取消、安全冲突编号、发布前校验与提交前回滚.
- 加密 ZIP、7Z 与 RAR 可在首次打开或解压时使用宿主原生密码框; 密码错误后可原位重试, 不会丢失当前档案路径.
- 可从宿主路径栏直接修正 ZIP 文件名编码; 同一只读会话原位重建索引, 并尽可能保留当前内部路径与勾选条目.
- 可通过宿主有界授权的同级卷描述符浏览、预览和解压完整的标准 `.z01 + .zip`、现代 WinRAR `partN.rar`、编号 `.zip.001` 与编号 `.7z.001` 卷组; 缺卷或卷变化会返回明确错误.
- 可从单项或同父级多选创建 ZIP、7Z、TAR、TAR.GZ、TAR.XZ、TAR.BZ2 与 TAR.ZST; ZIP 另支持 AES-256 与标准分卷, 多选也可为每项单独创建档案.
- 分卷 ZIP 和“单独压缩每个项目”会在全部物理输出逐项校验后通过一个可恢复批次统一发布; 失败或宿主重启不会把正常部分结果当作成功.
- 普通单卷 ZIP、安全普通 7Z，以及符合条件的 TAR、TAR.GZ/TGZ、TAR.XZ/TXZ、TAR.BZ2/TBZ2 与 TAR.ZST/TZST 可通过校验重建执行添加文件、导入完整目录树、新建空目录、重命名和删除; 只有完整读回通过后才原子替换源档案.
- 可使用档案专属的路径栏操作添加文件或完整目录树的显式混合选择, 或新建空目录; 同一原生列表中还可直接重命名条目或多选删除. 操作复用现有对话框、进度和路径/选择恢复, 只有会话与相关条目允许时才显示入口.
- 可写档案修改成功后, 可从成功提示或管理页菜单恢复上一版本; 宿主在有界保留期内只提供一次恢复, 目标被其他应用改写后会拒绝覆盖.
- 管理页集中显示实际格式、内容统计、可用修改动作和精确的只读原因; 每次修改都会在预留输出前列出事实工作量、完整重建及辅助元数据影响, 取消不会创建待提交输出.
- 危险档案名称保持只读隔离, 写出前继续执行结构与资源检查; 对可 seek 的宿主描述符优先直接读取, 避免无必要的整包复制.
- 压缩时可选择在全部物理输出校验并提交后将完整源选择移入宿主回收站; 该选项默认关闭, 源身份变化或输出证明不完整都会在移除源数据前中止.

### 当前支持

当前版本识别以下可浏览与解压的扩展名:

```text
zip, zip.001, jar, aar, war, 7z, 7z.001, rar, tar, tar.gz, tgz, tar.xz, txz, tar.bz2, tbz2, tar.zst, tzst
```

当前版本可创建以下格式:

```text
zip, 7z, tar, tar.gz, tar.xz, tar.bz2, tar.zst
```

> 原生集成需要配套的 AutoJs6 6.8.0 Explorer Action v21 构建 (版本代码 5276 或更高). RAR 与分卷档案明确保持只读. 普通单卷 ZIP、未加密且非 solid 并位于解码资源预算内的普通单卷 7Z，以及只含安全普通文件和目录的 TAR 系列档案可以修改; 加密、solid、分卷、危险路径、不支持方法或超预算的 7Z 保持只读. 标准分卷 ZIP 应从最终 `.zip` 打开, 现代 WinRAR 卷组应从首个 `partN.rar` 打开, 编号 ZIP 或 7Z 应从 `.001` 打开, 且全部必需同级卷须位于同一目录. 创建时加密文件名, 以及修改 JAR/AAR/WAR 和 RAR 仍不是当前能力.

### 使用方法

1. 安装 Archive Manager, 并在 AutoJs6 插件中心启用它.
2. 点击档案文件的主操作按钮, 或从菜单选择“打开压缩档案”. 名称未被识别时, 可从文件菜单选择“作为压缩档案打开...”. 随后可像浏览普通目录一样使用宿主路径栏; 名称与实际格式不符时, 路径栏会标注探测结果.
3. 使用路径栏右侧动作解压当前内部目录, 或通过文件名编码动作修正 ZIP 名称; 长按条目可多选解压; 文件菜单中的“解压到...”用于解压整个档案. 需要密码时宿主会自动提示.
4. 对文件或文件夹选择“压缩...”; 也可在同一目录多选后使用底部操作栏中的“压缩...”. 如需在成功后清理源项目, 请明确开启默认关闭的“压缩完成后将源项目移入回收站”.
5. 在可写档案的原生页面中, 使用路径栏的“添加文件或文件夹”和“新建文件夹”, 也可直接重命名条目或多选删除; 详细格式信息与附加设置位于“管理压缩档案...”中.

### 权限与数据

Archive Manager 不申请存储或网络权限. 宿主只提供短生命周期只读描述符和固定插件 UID 的输出事务, 插件不能自行选择任意文件系统路径. Explorer Action v11 仅在有界同步重试请求中传递密码, 双方立即移除并清理保留缓冲, 且从不持久化. Explorer Action v12 仅增加会话级、有界的宿主批准同级卷目录: 插件获得不透明 ID 而不是路径, 调用方 UID、文件身份、大小、修改时间与生命周期均在使用前重复校验. Explorer Action v13 只重新索引同一暂存源, 并在完整替换索引准备完成前保留旧状态. Explorer Action v14 只精确匹配 `.zip.001` 与 `.7z.001` 等有界复合后缀, 不会匹配任意 `.001` 文件, 且复用 v12 目录而不授予目录或路径访问能力. Explorer Action v17 只在普通主动作未匹配时增加无匹配器的只读溢出复核入口; 它仅在用户点击后复用一次既有档案会话, 不增加路径、目录或写入权限. Android 与 Java 库仍可能产生无法完全控制的短生命周期运行时副本, 因此密码清理属于尽力而为, 不是绝对保证. 路径穿越和危险名称始终隔离, 输出在发布前完成校验, 资源预算确认也不会关闭结构安全检查.

Explorer Action v15 只将同一会话中已验证的新文件输出组成最多 128 项的可恢复批次. Explorer Action v16 只有在插件提交完整有序源选择和全部已提交输出事务的精确证明后, 才允许宿主重新核对源与输出身份并把源项目移入回收站. 宿主先同步恢复副本并持久记录条目, 再移除源数据; 插件不获得任意路径或直接删除能力. Binder 响应丢失时只查询同一幂等终态, 不盲目重试.

Explorer Action v18 只在宿主私有持久存储中保留上一版档案, 向插件返回不透明历史 ID 而不是备份路径. 只有父目录与目标仍精确匹配本次提交结果时才能恢复一次. 外部改写会使历史失效; 中断的恢复证据会保留, 并在宿主处理前阻止再次替换同一目标. 普通历史受保留时间、数量、总字节数和剩余空间约束, v8-v17 会话不会创建替换备份. Explorer Action v19 只为能力允许的现有条目传递删除或重命名所需的不透明 ID 与安全叶名称. Explorer Action v20 只为明确选定的输入根提供冻结授权: 插件得到有界的不透明节点、元数据和一次性只读描述符, 不会得到源路径、URI、未选同级项目或通用存储访问. 宿主在提交前重新验证完整快照, 任一输入变化或失败都会中止整个档案替换. Explorer Action v21 在 Activity 与宿主会话重建后仍保留有界的宿主持有源恢复批次; 恢复源项目不会覆盖已有名称, 也不会删除已经创建的档案.

### Roadmap

宿主原生浏览、解压、创建、档案修改、上一版本恢复与持久源恢复均已完成, 且没有改变普通文件管理器布局. Roadmap 中未勾选的项目属于后续可选协议、后端或边界增强, 不代表当前能力.

- [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/ROADMAP.md)

### 版本记录

#### v2.22.0

_2026/08/30_

- `提示` 本版本需要配套使用 Explorer Action v21 的 AutoJs6 6.8.0 构建 (版本代码 5276 或更高)
- `新增` 插件中心现可直接从插件识别所需宿主版本、运行服务与已打包的设备架构
- `修复` 公开简介与使用说明现会准确区分浏览、解压、创建、修改、加密与分卷支持, 不再夸大可写格式范围
- `修复` 压缩档案添加操作现会在终态回调前关闭输入授权并注销活动状态, 完成或失败后立即重试不再误报已有操作正在运行
- `优化` 新增格式能力矩阵与安装指南, 安装前即可核对兼容性、只读边界、恢复行为与故障排查方式

#### v2.21.0

_2026/08/30_

- `提示` 本版本需要配套使用 Explorer Action v21 的 AutoJs6 6.8.0 构建 (版本代码 5276 或更高)
- `新增` 压缩完成后现会保留完成页面与源项目恢复历史; 经用户明确选择移入回收站的源项目可在 Activity 或宿主会话重建后恢复
- `修复` 恢复源项目绝不覆盖既有名称, 也不会删除已创建的压缩档案; 冲突、部分恢复或中断恢复会保留恢复证据并报告逐项结果
- `优化` 有界持久历史由宿主持有且只公开不透明元数据; 恢复界面仅位于插件压缩页面, 普通文件管理器布局保持不变

#### v2.20.0

_2026/08/30_

- `提示` 本版本需要配套使用 Explorer Action v20 的 AutoJs6 6.8.0 构建 (版本代码 5276 或更高)
- `新增` 可写档案的宿主原生页面现可直接新建空目录, 并从文件与完整目录树的显式混合选择中添加内容; 空目录会保留, 同名目录根会安全编号
- `修复` 提交前会重新校验冻结的输入快照; 取消、源内容变化、所选目录树内出现等价名称歧义或直接文件冲突时, 整批添加都会失败并保留原档案
- `优化` Explorer Action v20 只为选定输入公开有界的不透明节点、元数据和一次性只读描述符; 不授予路径、URI、未选同级项目或通用存储访问, 且不改变普通文件管理器布局

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

- 安装指南: [docs/INSTALLATION.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/docs/INSTALLATION.md)
- 格式能力矩阵: [docs/FORMAT_CAPABILITIES.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/docs/FORMAT_CAPABILITIES.md)
- 安全策略: [SECURITY.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/SECURITY.md)
- AutoJs6 文档: https://docs.autojs6.com
- 第三方软件声明: [THIRD_PARTY_NOTICES.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/THIRD_PARTY_NOTICES.md)
- Android Storage Access Framework: https://developer.android.com/guide/topics/providers/document-provider
