<div align="center">
  <p>
    <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="Archive Manager" width="128" />
  </p>

  <h1>Archive Manager</h1>

  <p>AutoJs6 文件管理器插件, 用于浏览、解压和创建受支持的档案, 并以事务方式修改普通 ZIP</p>

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

Archive Manager 把压缩档案的浏览、解压、创建和管理能力带入 AutoJs6 文件管理器. 当前版本通过宿主原生列表和路径栏浏览 ZIP、7Z 与 TAR 系列档案, 预览受支持条目, 解压选定范围, 从单个项目或同父级多选创建受支持格式, 并可在管理页以事务方式修改普通单卷 ZIP.

### 当前能力

- 直接在 AutoJs6 原生文件列表中打开 ZIP、7Z 与 TAR 系列档案，沿用宿主主题、暗色模式和动态色。
- 路径栏同时显示外部目录、档案名和内部目录；可点击层级跳转，返回键会先返回内部上一级，再退出档案。
- 使用宿主现有预览器打开支持的文档、图片、音频和视频条目。
- 通过“解压到...”快捷动作直接解压整个档案，无需先进入浏览页面。
- 选择“管理压缩档案...”可进入管理页, 解压全部、当前内部目录或当前勾选内容, 也可通过“添加文件...”“新建文件夹...”“重命名...”和“删除”修改普通单卷 ZIP; “解压到...”仍是整包快捷动作.
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
- 识别标准 `.z01 + .zip` 分卷 ZIP 结构，并列出所需的前序分卷名称，不再把单独打开的末卷误报为损坏；可用预设或自定义 MiB 大小创建标准分卷 ZIP，读取既有分卷仍等待宿主提供同级卷访问接口。
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
zip, jar, aar, war, 7z, tar, tar.gz, tgz, tar.xz, txz, tar.bz2, tbz2, tar.zst, tzst
```

当前版本可创建以下格式:

```text
zip, 7z, tar, tar.gz, tar.xz, tar.bz2, tar.zst
```

> 完整集成使用 Explorer Action v8 执行已校验的目标替换, v7 执行提交前输出校验, v6 执行原生浏览与条目预览, v4 文件会话执行压缩; 需要 AutoJs6 版本代码 5276 或更高. 当前仅普通单卷 `.zip` 支持修改. 宿主原生页面内按条目解压、读取既有分卷、导入完整目录树、创建时加密文件名、压缩后删除源文件, 以及修改 JAR/AAR/WAR、7Z 或 TAR 系列档案尚未发布; 请以 Roadmap 的勾选状态为准.

### 使用方法

1. 安装插件，并在 AutoJs6 插件中心启用它。
2. 在文件管理器中打开 ZIP、JAR、AAR、WAR、7Z 或 TAR 系列档案的菜单。
3. 选择“打开压缩档案”，在宿主文件列表中进入目录、搜索或使用路径栏跳转。
4. 若要解压整个档案，请从文件菜单选择“解压到...”，再通过 Android 系统选择器指定输出目录。
5. 若要解压特定范围, 请选择“管理压缩档案...”, 在管理页浏览或勾选条目, 点击“解压到...”, 选择范围后再指定输出目录.
6. 若要修改普通单卷 ZIP, 请选择“管理压缩档案...”, 再使用“添加文件...”“新建文件夹...”“重命名...”或“删除”. 请等待重建、校验及成功提示完成后再离开页面.
7. 在管理页解压前可选择如何处理等价输出名称。“每次询问”可将一次跳过、覆盖或自动重命名决定应用于全部后续兼容冲突。
8. 要创建档案，请打开普通文件或文件夹的菜单并选择“压缩...”；也可先多选同一目录中的项目，再使用底部“压缩...”动作。若要每个项目分别生成压缩档案，请开启“单独压缩每个文件和文件夹”，核对输出预览后再创建；此模式固定以自动编号安全处理名称冲突。创建 ZIP 时可选择“无”、常用 MiB 预设或 1 至 4096 MiB 的整数自定义值；输出超过所选大小时由 `.z01`、`.z02` 等编号卷和最终 `.zip` 组成，较小的输出仍为单个 `.zip`。

### 权限与数据

插件不申请存储或网络权限。原生浏览优先租用宿主的只读可 seek 描述符，并以独立位置读取通道直接访问普通档案；管道、不可 seek 或可写输入、Android 7，以及必须使用本地文件的兼容后端（当前为加密 ZIP）才回退到私有缓存。描述符租约或缓存会在页面关闭、解绑、失败或过期后清理。解压只使用宿主临时授予的输入 URI。创建档案通过绑定到插件 UID 的宿主文件会话分页读取目标，并且只能在当前父目录执行事务式输出。密码只保存在可清零的内存缓冲区中，不写入 Bundle、偏好设置、日志或诊断信息，并在替换、任务结束或页面销毁时清除。固定 4 GiB 输入上限和浏览阶段的解压大小/压缩比门槛已取消；路径穿越防护、目标隔离、源大小核对、输出事务和失败清理仍然保留。

资源预算只决定何时警告或要求确认，不会放宽结构安全。确认后也只把本次解压的实时字节与压缩比边界扩大到所选条目的声明值；未声明的额外增长、源档案变化、大小或 CRC 不一致仍会中止并清理输出。

创建分卷 ZIP 时，插件先在私有缓存中组装并完整校验卷组，再把各卷复制到宿主隐藏的待提交输出并逐字节比对。全部通过后才删除私有暂存副本，并按编号卷在前、最终 `.zip` 在后的顺序发布；既有名称始终不会被覆盖。当前宿主没有整组原子提交能力，因此任何部分提交结果都会被明确报告，不会伪装为完整档案。

危险名称只会作为不透明 ID 下的只读显示信息，不会成为输出路径。

### Roadmap

普通 ZIP 之外的可写格式、目录树导入、分卷读取、宿主原生页面内按条目解压、撤销、失败恢复及完整设备矩阵的任务与验收条件集中维护在 Roadmap 中. 未勾选项目不代表当前版本已经支持.

- [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/ROADMAP.md)

### 版本记录

#### v2.1.0

_2026/08/25_

- `提示` 当前仅普通单卷 `.zip` 支持修改. JAR/AAR/WAR, 分卷 ZIP, 7Z 与 TAR 系列档案仍为只读; 重建会规范化档案注释, 非必要 extra metadata 和 Unix 权限属性
- `新增` 为选中的 ZIP 提供“管理压缩档案...”, 可在管理页添加文件, 新建空目录, 重命名和删除, 并正确处理目录子树的重命名与删除
- `新增` Explorer Action v8 将重建结果写入宿主持有的待提交输出, 完整读回校验后才原子替换原档案, 并自动刷新 Explorer 条目
- `优化` 每次修改都会先生成不可变计划, 在提交替换输出前检查危险路径, 重复或等价名称, 文件/目录冲突, 不支持保留的条目及源档案变化
- `优化` ZIP 重建会保留 Stored/Deflate 条目内容, 可用时间戳和受支持的 ZipCrypto/AES 加密; 取消或任一校验失败都会中止待提交输出, 原档案保持不变

#### v2.0.0

_2026/08/25_

- `新增` 产品名称统一为不可翻译的英文名称 Archive Manager，打开动作调整为“打开压缩档案”
- `新增` 通过 Explorer Action v5 在 AutoJs6 原生文件列表中浏览档案，并复用路径栏、主题和返回导航
- `新增` 通过 Explorer Action v6 使用宿主的文档、图片、音频和视频预览器打开支持的档案条目
- `新增` “解压到...”快捷动作，可直接选择目录并解压整个档案
- `新增` “管理压缩档案...”打开管理页, 解压时可选择全部、当前内部目录或当前勾选范围, 同时保留整包快捷动作
- `新增` 解压名称冲突支持询问、跳过、覆盖和自动重命名，可应用到全部兼容冲突并准确汇总结果，同时保留既有输出文件夹
- `新增` 接入 Explorer Action v4，在普通文件、文件夹及同父级多选的五项操作栏中提供“压缩...”
- `新增` 新增 ZIP 创建表单，支持默认命名、压缩级别、进度、取消和同名自动编号
- `新增` 支持创建标准分卷 ZIP（包括 AES-256），可选常用 MiB 预设或整数自定义大小；整组分卷统一安全编号，最终 `.zip` 在编号卷之后发布
- `新增` 同父级多选可为每个项目单独创建压缩档案，并在创建前预览输出数量和名称；冲突自动编号，中途失败或取消会准确保留并报告已完成结果
- `新增` 支持创建非 solid 7Z，可选 0 至 9 压缩级别和 AES-256 内容加密；文件名保持可见，不误报文件名加密能力
- `新增` 支持创建 TAR、TAR.GZ、TAR.XZ、TAR.BZ2 与 TAR.ZST，格式切换会更新完整扩展名和可用设置
- `新增` 支持浏览和解压 ZipCrypto/AES 加密 ZIP，密码错误可原位重试；创建 ZIP 时可选用 AES-256 密码，文件名保持可见并要求两次密码一致
- `新增` 支持浏览、预览和解压普通或 solid 7Z，覆盖常见压缩与过滤器链、AES 内容加密和头部加密；缺少或错误密码可明确诊断
- `新增` 支持在宿主原生文件列表中浏览、预览和解压未压缩 TAR；按头校验和确认结构，链接、设备节点和稀疏项只读列出
- `新增` 支持通过相同的宿主原生路径浏览、预览和解压 TAR.GZ/TGZ、TAR.XZ/TXZ、TAR.BZ2/TBZ2 与 TAR.ZST/TZST；格式识别同时验证压缩流签名和内部 TAR 结构
- `新增` 解压资源预算支持兼容、严格和自定义档位；超出预算仍可只读浏览，并在写出前显示预计输出、超限项目及单次确认
- `新增` 解压过程显示条目、字节、当前项目、传输速度和预计剩余时间，并支持可靠取消
- `修复` 改用目录元数据快速打开 ZIP，并兼容自解压式前导数据、传统文件名编码、Windows 路径分隔符及更多可读取的 ZIP 方法
- `修复` 标准 `.z01 + .zip` 分卷 ZIP 会列出所需的前序分卷，不再把单独打开的末卷误报为损坏
- `修复` 自动识别 ZIP 文件名编码不正确时可手动覆盖，解压过程会复用所选编码
- `修复` 接受未知或不精确的文件大小、合法 DocumentsProvider URI 和宿主附加的写入授权，避免有效档案在解析前被拒绝
- `修复` 修复 Android 7.x 因调用新系统专有 API 而无法浏览或解压 ZIP 的问题
- `修复` 修复 Android 7 打开含 Unicode 文件名的 ZIP 时出现乱码的问题，包括缺少 UTF-8 文件名标记或后端处理标记不一致的档案
- `修复` 错误密码统一归类为 PASSWORD/WRONG_PASSWORD，并修正 AES v2 条目校验值为零时被误判损坏的问题
- `修复` TAR.GZ/TAR.XZ/TAR.BZ2/TAR.ZST 等复合扩展名的默认解压文件夹会去掉完整后缀，不再错误残留 `.tar`
- `修复` 含父级穿越、绝对路径、驱动器前缀或控制字符名称的档案不再因单个危险条目整体无法浏览；危险名称进入只读隔离目录并保持可预览，写出前必须明确跳过
- `修复` 当目标提供程序把大小写差异或 Unicode 等价名称视为相同时，解压不再覆盖既有文件或文件夹；等价的输出文件夹名称会自动编号
- `修复` 取消或解压失败后会在不可取消的清理阶段回滚本次新建输出根；目标提供程序拒绝删除时，会列出可能残留的名称和 URI，不再只显示笼统的清理错误
- `修复` 修复 Android 7 创建加密 7Z 失败的问题，并按路径匹配校验条目，避免后端返回顺序不同导致有效档案被误判
- `修复` 管理页现可适配纵向和横向的较矮屏幕：档案及路径信息移入工具栏，设置保留在紧凑横向栏中，档案条目和操作在最高 2.0 倍字体下仍保持可见
- `优化` 移除固定 4 GiB 输入上限和浏览阶段的解压大小/压缩比门槛，同时保留路径隔离、完整性校验和失败清理
- `优化` 新增可逐项追踪的 Roadmap，并重写 README 与 CHANGELOG，使当前能力和后续计划清楚分开
- `优化` 独立页面改为遵循系统日夜模式和 Material 动态色
- `优化` ZIP、7Z 与 TAR 系列输出通过绑定插件 UID 的宿主会话写入同目录临时文件后原子提交，无需存储权限且不会覆盖既有文件
- `优化` 统一校验格式和条目的预览、解压与创建能力，使不可用选项保持禁用
- `优化` 压缩档案失败时标明格式、处理阶段、稳定代码和原因，调试版本可复制完整诊断信息
- `优化` 普通档案现在通过只读宿主描述符上的独立位置读取通道直接浏览，无需整份复制；不兼容的输入或后端（当前包括加密 ZIP）按需回退到私有缓存，并在关闭、失败或过期后清理
- `优化` 统一 ZIP、7Z 与 TAR 系列的创建输出事务；源不可读及输出预留、打开、写入、提交失败会返回稳定阶段，回滚无法确认时会关闭会话、显示预定路径并阻止误重试
- `优化` 创建档案会在打开临时输出前扫描源项目，并分别显示扫描、压缩、校验和提交状态，以及文件总数、读取字节与未知大小文件数
- `优化` 创建的档案会在发布前完整读回并核对格式、条目、大小、CRC 和内容指纹；每个待提交的 ZIP 分卷也会逐字节比对
- `依赖` 新增 Apache License 2.0 许可的 Zip4j 2.11.5，用于加密 ZIP 数据流、AES-256 创建及 Android 7.x 兼容路径
- `依赖` 新增 0BSD 许可的 XZ for Java 1.12，用纯 Java 读写 TAR.XZ/TXZ，不增加原生 ABI
- `依赖` 新增 BSD 许可的 zstd-jni 1.5.7-15，用于读写 TAR.ZST/TZST；四个 Android ABI 均通过 16 KiB ELF 对齐与 RELRO 检查

#### v1.0.1

_2026/08/08_

- `修复` 插件中心启用插件时服务绑定为空的问题
- `优化` 简化插件名称、描述和使用说明

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
