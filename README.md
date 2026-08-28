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

- 直接在 AutoJs6 原生文件列表中打开 ZIP、7Z 与 TAR 系列档案，沿用宿主主题、暗色模式和动态色。
- 路径栏同时显示外部目录、档案名和内部目录；可点击层级跳转，返回键会先返回内部上一级，再退出档案。
- 无需离开宿主原生档案页, 即可从路径栏解压当前内部目录, 或进入选择模式解压勾选的文件与目录; 任务显示进度并可取消, 完成后父目录自动刷新.
- 使用宿主现有预览器打开支持的文档、图片、音频和视频条目。
- 通过“解压到...”将整个档案解压到推荐的同目录文件夹, 或使用 Android 系统选择器选择其他文件夹; 已有的等价文件夹名称会安全编号.
- 选择“管理压缩档案...”可进入管理页, 解压全部、当前内部目录或当前勾选内容, 也可通过“添加文件...”“添加文件夹...”“新建文件夹...”“重命名...”和“删除”修改普通单卷 ZIP; “解压到...”仍是整包快捷动作.
- ZIP 修改会在写入前生成计划, 重建到宿主持有的待提交输出并完整读回, 只有校验通过后才原子替换原档案. 取消或失败不会改变源档案, 提交成功后 Explorer 会自动刷新.
- 输出名称等价时可选择“每次询问”“跳过”“覆盖”或“自动重命名”；“应用到全部”可处理后续兼容冲突，既有输出文件夹始终自动编号并保持不变。
- 按目录浏览、搜索和排序档案内容。
- 只读取目录元数据即可显示列表，不会为了打开页面预先解压全部内容。
- 普通档案优先通过宿主只读可 seek 描述符和独立位置读取通道直接浏览，无需整份复制；管道、不可 seek 或可写输入、Android 7，以及必须使用本地文件的兼容后端（当前为加密 ZIP）才回退到私有缓存，并在关闭时清理。
- 解压资源预算提供兼容、严格和自定义档位；超过条目数、路径、输出大小或压缩比预算的档案仍可浏览，写出前会列出预计空间与风险并要求单次确认。
- 解压过程显示条目与字节进度、当前项目、传输速度和预计剩余时间；取消或失败会回滚本次新建输出根，目标提供程序拒绝删除时会列出可能残留的名称和 URI。
- 无法安全写出的父级穿越、绝对路径、驱动器路径或控制字符名称会进入路径栏可见的“危险路径”只读目录；内容仍可预览，解压整个档案前会明确要求跳过这些条目，正常条目不受影响。
- 浏览、预览和解压未压缩 TAR；符号链接、硬链接、设备节点和稀疏项只列出，不会作为普通文件写出。
- 浏览、预览和解压 TAR.GZ/TGZ、TAR.XZ/TXZ、TAR.BZ2/TBZ2 与 TAR.ZST/TZST；复用相同的内部路径、特殊条目隔离和完整性检查。
- 浏览、预览和解压普通或 solid 7Z，包括常见压缩/过滤器链、内容加密与头部加密档案；缺少密码或密码错误时会给出明确诊断。
- 根据真实 ZIP/7Z/TAR 结构确认格式，并统一控制预览、解压和创建能力；未支持的选项保持禁用。
- 兼容 Zip64、自解压式前导数据、传统文件名编码和 Windows 风格路径分隔符。
- 通过宿主有界授权的同级卷描述符浏览、预览和解压完整的标准 `.z01 + .zip` 卷组；可用预设或自定义 MiB 大小创建标准分卷 ZIP，并明确报告缺卷或卷变化。
- 浏览和解压使用传统 ZipCrypto 或 AES 的加密 ZIP；密码错误可原位重试。创建 ZIP 时可选择 AES-256 密码，文件名仍然可见；创建加密 ZIP 前需再次输入相同密码确认。
- 自动识别 ZIP 文件名编码不正确时允许手动覆盖；浏览与解压复用同一选择。
- 压缩档案失败时显示格式、处理阶段、稳定代码和明确原因；调试版本可复制完整诊断信息。
- 为普通文件、文件夹以及同父级多选提供“压缩...”动作。
- 同父级多选可为每个项目单独创建压缩档案；表单会预览输出数量和派生名称，既有或重复名称自动编号且绝不覆盖。每个输出独立提交；若中途取消或失败，界面会保留并报告已完成结果，同时阻止可能重复创建的整批重试。
- 可创建普通或标准分卷 ZIP，以及 7Z、TAR、TAR.GZ、TAR.XZ、TAR.BZ2 与 TAR.ZST；表单只展示当前格式真正支持的压缩级别和密码选项。
- 输出先写入同目录临时文件，再原子提交；可选择自动编号，或先尝试精确名称并在改用编号名称前询问，始终不会覆盖既有文件。名称预留后会先扫描并固化有界源清单，再打开临时输出；表单分别显示扫描、压缩、校验和提交状态以及文件总数、读取字节与未知大小信息。最终名称发布前会完整读回仍处于隐藏状态的输出，并核对格式、条目、大小、CRC 和内容指纹。创建或校验失败会统一中止事务；若宿主无法确认临时输出已清理，表单会显示预定路径并停止重试。

### 当前支持

当前版本识别以下可浏览与解压的扩展名:

```text
zip, zip.001, jar, aar, war, 7z, 7z.001, rar, tar, tar.gz, tgz, tar.xz, txz, tar.bz2, tbz2, tar.zst, tzst
```

当前版本可创建以下格式:

```text
zip, 7z, tar, tar.gz, tar.xz, tar.bz2, tar.zst
```

> 原生集成需要配套的 AutoJs6 6.8.0 Explorer Action v17 构建 (版本代码 5276 或更高). RAR 与分卷档案明确保持只读, 当前只有普通单卷 `.zip` 可修改. 标准分卷 ZIP 应从最终 `.zip` 打开, 现代 WinRAR 卷组应从首个 `partN.rar` 打开, 编号 ZIP 或 7Z 应从 `.001` 打开, 且全部必需同级卷须位于同一目录. 创建时加密文件名, 以及修改 7Z、RAR 和 TAR 系列档案均不是当前能力.

### 使用方法

1. 安装插件，并在 AutoJs6 插件中心启用它。
2. 在文件管理器中打开 ZIP、JAR、AAR、WAR、7Z 或 TAR 系列档案的菜单。
3. 选择“打开压缩档案”，在宿主文件列表中进入目录、搜索或使用路径栏跳转。
4. 若要解压整个档案, 请从文件菜单选择“解压到...”. 使用推荐的当前文件夹, 或通过 Android 系统选择器选择其他文件夹, 然后确认准确的输出路径.
5. 若要解压当前内部目录, 请点击路径栏右侧的解压按钮. 若要解压指定条目, 请长按条目进入选择模式, 勾选文件或目录后点击底部“解压”. 需要密码、编码修正、危险路径确认、冲突策略或其他目标目录时, 请使用“管理压缩档案...”或“解压到...”.
6. 若要修改普通单卷 ZIP, 请选择“管理压缩档案...”, 再使用“添加文件...”“添加文件夹...”导入完整目录树、“新建文件夹...”创建空文件夹、“重命名...”或“删除”. 请等待重建、校验及成功提示完成后再离开页面.
7. 在管理页解压前可选择如何处理等价输出名称。“每次询问”可将一次跳过、覆盖或自动重命名决定应用于全部后续兼容冲突。
8. 要创建档案，请打开普通文件或文件夹的菜单并选择“压缩...”；也可先多选同一目录中的项目，再使用底部“压缩...”动作。若要每个项目分别生成压缩档案，请开启“单独压缩每个文件和文件夹”，核对输出预览后再创建；此模式固定以自动编号安全处理名称冲突。创建 ZIP 时可选择“无”、常用 MiB 预设或 1 至 4096 MiB 的整数自定义值；输出超过所选大小时由 `.z01`、`.z02` 等编号卷和最终 `.zip` 组成，较小的输出仍为单个 `.zip`。

### 权限与数据

Archive Manager 不申请存储或网络权限. 宿主只提供短生命周期只读描述符和固定插件 UID 的输出事务, 插件不能自行选择任意文件系统路径. Explorer Action v11 仅在有界同步重试请求中传递密码, 双方立即移除并清理保留缓冲, 且从不持久化. Explorer Action v12 仅增加会话级、有界的宿主批准同级卷目录: 插件获得不透明 ID 而不是路径, 调用方 UID、文件身份、大小、修改时间与生命周期均在使用前重复校验. Explorer Action v13 只重新索引同一暂存源, 并在完整替换索引准备完成前保留旧状态. Explorer Action v14 只精确匹配 `.zip.001` 与 `.7z.001` 等有界复合后缀, 不会匹配任意 `.001` 文件, 且复用 v12 目录而不授予目录或路径访问能力. Explorer Action v17 只在普通主动作未匹配时增加无匹配器的只读溢出复核入口; 它仅在用户点击后复用一次既有档案会话, 不增加路径、目录或写入权限. Android 与 Java 库仍可能产生无法完全控制的短生命周期运行时副本, 因此密码清理属于尽力而为, 不是绝对保证. 路径穿越和危险名称始终隔离, 输出在发布前完成校验, 资源预算确认也不会关闭结构安全检查.

Explorer Action v15 只将同一会话中已验证的新文件输出组成最多 128 项的可恢复批次. Explorer Action v16 只有在插件提交完整有序源选择和全部已提交输出事务的精确证明后, 才允许宿主重新核对源与输出身份并把源项目移入回收站. 宿主先同步恢复副本并持久记录条目, 再移除源数据; 插件不获得任意路径或直接删除能力. Binder 响应丢失时只查询同一幂等终态, 不盲目重试.

### Roadmap

剩余工作继续以可勾选项目追踪: 普通 ZIP 之外的可写重建、回收站组级撤销与历史、其余设备与生产工具矩阵, 以及首次公开发布资料.

- [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/ROADMAP.md)

### 版本记录

#### v2.10.0

_2026/08/28_

- `提示` 此版本需要配套的 AutoJs6 6.8.0 Explorer Action v17 构建 (版本代码 5276 或更高)
- `新增` 名称或扩展名未被识别的文件现在可选择“作为压缩档案打开...”; 结构探测成功后, 名称与实际格式不符时宿主原生路径栏会标注真实格式
- `新增` Explorer Action v17 增加绑定普通档案主动作、无匹配器且只读的复核入口, 并通过既有档案会话返回有界的实际格式元数据
- `修复` TAR.GZ、TAR.XZ、TAR.BZ2 与 TAR.ZST 现在使用精确的完整文件名后缀, 不再发布通用 `gz`、`xz`、`bz2` 或 `zst` 叶扩展名, 普通压缩流不会获得档案主动作
- `优化` 宿主建立菜单时不会在后台扫描文件; 只有用户明确点击后才执行一次既有只读档案打开调用, 且不新增路径、目录或写入权限

#### v2.9.0

_2026/08/27_

- `提示` 此版本需要配套的 AutoJs6 6.8.0 Explorer Action v16 构建 (版本代码 5276 或更高)
- `新增` 压缩表单新增默认关闭的“压缩完成后将源项目移入回收站”; 只有全部物理输出完成校验并成功提交后才会执行
- `新增` Explorer Action v16 只接受本次完整有序选择和全部已提交输出事务的精确证明, 再由宿主重新核对源与输出身份并执行回收站操作
- `修复` 宿主现在先同步恢复副本并持久记录回收站条目, 再移除源项目; 目录仅部分移除时会保留可恢复副本, 不再清除唯一恢复数据
- `优化` 移入回收站阶段不可取消, 成功、需要恢复、失败和结果未知分别提示; Binder 响应丢失时查询宿主终态而不盲目重试

#### v2.8.0

_2026/08/27_

- `提示` 此版本需要配套的 AutoJs6 6.8.0 Explorer Action v15 构建 (版本代码 5276 或更高)
- `新增` 分卷 ZIP 与“单独压缩每个项目”现在会先完成全部输出的写入和逐项读回校验, 再通过 Explorer Action v15 作为一个可恢复批次统一发布
- `修复` 多输出创建不再在正常故障路径留下已提交的部分结果; 只有完整批次提交后才刷新 Explorer 并报告成功
- `修复` 压缩选项开关现在可在 Android 7 上正确绘制并保持可点击, 不再只显示为普通文字标签
- `优化` 宿主在发布前持久记录父目录与每个暂存文件的身份; 失败或重启时只回滚身份仍匹配的成员, 外部改写的文件会保留并报告需要人工恢复

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
