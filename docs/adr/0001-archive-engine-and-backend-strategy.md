# ADR 0001: 统一档案引擎与后端演进策略

- 状态: 已接受统一架构、ZIP 加密后端与 TAR/TAR.GZ/TAR.XZ/TAR.BZ2/TAR.ZST 只读后端; 其他格式后端选型待原型数据
- 日期: 2026-08-23
- 范围: 插件内部格式识别、列表、预览、解压、创建和未来档案修改

## 背景

项目最初只有 ZIP 实现. 扫描、预览和解压分别直接重新打开 ZIP, 创建页面也直接实例化 ZIP writer. 这种结构会让新增 7z 或 tar 后出现以下问题:

- UI 通过格式名称猜测能力, 容易显示后端无法完成的密码、分卷或修改选项;
- 同一条目的可预览、可解压和可修改状态无法分别表达;
- 扫描快照携带 ZIP 专用字段, 其他后端只能继续扩充条件分支;
- 格式识别容易退化为扩展名判断;
- 更换后端时必须同时修改 Activity、宿主会话和安全校验代码.

当前生产依赖包含 Apache Commons Compress 1.28.0、XZ for Java 1.12、zstd-jni 1.5.7-15 与 Zip4j 2.11.5. Commons Compress 负责 ZIP 目录元数据、TAR 头与条目流及 GZIP/XZ/BZIP2 压缩流适配; XZ for Java 提供纯 Java XZ 解码器; zstd-jni 提供 Zstandard Android 流式解码; Zip4j 接管 ZipCrypto/AES 条目数据、加密方法元数据、AES-256 输出, 并在 Android 7.x 代替会调用缺失 `FileTime` API 的 Commons ZIP 路径. ZIP 与 TAR 族均以 Android 运行时、宿主只读会话和外部工具样本覆盖.

## 决策

### 1. 先固定中立合同

所有后端都通过以下模型接入:

- `ArchiveFormat`: 格式标识、显示名称、扩展名和 MIME 类型;
- `FormatCapabilities`: 识别、列表、预览、打开、解压、创建、添加、删除和重命名能力;
- `ArchiveOptionMode`: 密码、文件名加密和分卷分别使用 `UNSUPPORTED`、`OPTIONAL`、`REQUIRED`;
- `ArchiveEntryCapabilities`: 每个条目分别表达可打开、可解压、可删除、可重命名及不可用原因;
- `ArchiveReader`: 返回中立目录元数据并按条目打开只读数据流;
- `ArchiveWriter`: 通过宿主受控输出会话创建档案;
- `ArchiveEngine`: 注册后端、结构探测、能力查询和 reader/writer 创建.

UI、扫描器、预览流、解压器和宿主会话不得直接依赖 ZIP 类. ZIP 专用字符集探测、目录对象和流实现只存在于 `ZipArchiveBackend` 以下.

### 2. 只注册已经通过能力验证的 ZIP 与 TAR 族后端

当前能力表如下:

| 能力 | ZIP 当前状态 | TAR 当前状态 |
| --- | --- | --- |
| 结构识别/列表/预览/打开/解压 | 支持; ZipCrypto/AES 条目在提供密码后可读取, 其他条目仍受具体压缩方法约束 | 支持 TAR、TAR.GZ/TGZ、TAR.XZ/TXZ、TAR.BZ2/TBZ2 与 TAR.ZST/TZST; 识别 POSIX/GNU/Ant/AIX 及校验和有效的 V7 头 |
| 创建 | 支持; 压缩级别 0 至 9, 可选密码使用 AES-256 | 不支持; 未注册 writer |
| 添加/删除/重命名 | 不支持; 必须先实现重建事务 | 不支持; 必须先实现重建事务 |
| 密码 | 可选; 无密码仍可浏览加密条目的目录元数据 | 不支持 |
| 文件名加密 | 不支持; ZIP AES-256 输出仍公开中央目录文件名 | 不支持 |
| 分卷 | 不支持 | 不支持 |

TAR 普通文件可预览和解压, 目录可创建; 符号链接、硬链接、FIFO、设备节点、未知特殊类型及稀疏项保留在列表中并准确声明不可打开、不可解压. 首阶段不跟随链接, 也不把特殊项物化为普通文件.

`jar`、`aar` 和 `war` 是 ZIP 后端的扩展名别名, 不是独立格式. `tar` 及 `tar.gz`/`tgz`、`tar.xz`/`txz`、`tar.bz2`/`tbz2`、`tar.zst`/`tzst` 各有独立格式标识, 但共用 TAR 条目模型与只读能力. 动作目录和 Intent 初筛使用已注册的可列表格式生成, 创建菜单只使用已注册 writer 的格式, 避免文档、菜单和实际后端能力出现三份不同来源.

Explorer Action 当前只接受不含句点的叶扩展名. 插件内部保留全部复合后缀, 对宿主目录发布 `gz`/`xz`/`bz2`/`zst` 叶扩展名, 收到 Activity 动作时核对完整文件名, 所有入口最终还会核对外层压缩签名和解压后的 TAR 结构. 这会让对应的独立压缩流在宿主中看到候选动作, 但插件不会把它们误识别为 TAR, 结构探测失败后也会清理暂存输入; 后续宿主协议若支持复合后缀, 可移除这一入口层折衷而无需改变后端.

### 3. ZIP 采用纯 Java 混合后端, 其他格式仍按原型数据选择

候选路线:

| 路线 | 预期用途 | 必须回答的问题 |
| --- | --- | --- |
| Commons Compress + Zip4j | ZIP 加密/分卷及 tar 族的纯 Java 路线 | Android API 24 兼容性、AES/ZipCrypto/Zip64/分卷覆盖、7z 写入限制、APK 增量和许可证清单 |
| libarchive | 多格式读取和 tar 族能力 | Android ABI 构建、seek/密码/分卷能力、JNI 取消、CVE 更新流程、压缩后的每 ABI 体积和许可证 |
| 7-Zip SDK 或受维护封装 | 7z 固实档案、头部加密和过滤器 | Android ABI/许可证、线程取消、密码内存处理、分卷、包体积及维护成本 |

任何候选进入 `app` 的生产依赖前都必须提交可复现原型数据:

1. 在 API 24 和当前最高 API 上读取真实工具生成的样本;
2. 覆盖 Unicode、超长名、空档案、损坏尾部、密码、错误密码、Zip64、固实档案、分卷和取消;
3. 记录 Debug/Release APK 的总体积和每个 ABI 增量;
4. 记录许可证、传递依赖、版本、已知 CVE 检查方法和升级责任;
5. 证明输入流、临时文件、密码缓冲和原生资源在取消及进程异常后可清理;
6. 对无法写入的格式返回只读能力, 不在 UI 中展示虚假修改动作.

ZIP 加密原型已满足 API 24、真实 AES/ZipCrypto 样本、错误密码分类、无新增 ABI 和 Release 体积门禁, 因此 Zip4j 2.11.5 已进入生产依赖. 它不替换 Commons Compress: 非加密条目仍优先走 Commons, 只有加密数据流以及 Android 7.x 兼容路径交给 Zip4j. 其他格式的评估顺序仍优先使用无新增 ABI 的纯 Java 原型; 只有在 7z 固实档案、头部加密、分卷或性能目标无法可靠满足时, 才进入原生后端原型.

未压缩 TAR 原型复用既有 Commons Compress 1.28.0 生产依赖, 不新增 Maven 组件或 ABI. reader 会完整遍历头部、校验每个可见条目的头校验和并在按条目读取时重新核对快照元数据; 数据流仍经过统一源身份与实际大小校验.

TAR.GZ 使用 Commons Compress 自带的 GZIP 流, TAR.XZ 使用 Commons Compress 适配器与 XZ for Java 1.12. 两者按官方建议在缓冲压缩流外叠加同一 `TarArchiveInputStream`, 支持连接流, 并在外层签名之后再次验证 TAR 结构; 索引结束时继续读取压缩流尾部, 让 GZIP/XZ 完整性校验实际执行. 流式压缩容器无法提供准确的逐条目压缩大小, 因而中立模型记录 `-1` 未知值, 不伪造压缩比. XZ 解码器设置 262,144 KiB 内存上限; 超限返回明确的不支持诊断, 不让字典申请直接耗尽进程内存. XZ for Java 是纯 Java 0BSD 组件, Release APK 实测增加 21,464 bytes 且不新增 ABI. 该结论只覆盖读取, 不提前决定压缩 TAR writer、7z 或其他格式后端.

TAR.BZ2 继续复用 Commons Compress 的纯 Java BZIP2 流, 不增加 Maven 组件或 ABI. TAR.ZST 使用 zstd-jni 的连续帧输入流; 它接受标准帧与 skippable frame magic, 将最大窗口限制为 `2^28` bytes, 并与其他压缩 TAR 一样在索引关闭前消费尾部以触发完整性检查. 外层只有 Zstandard magic 且不足最小帧长度时归类为已识别格式的截断, 而不是未知格式.

zstd-jni 只用于 Zstandard 流, 不代表选择 libarchive 或原生 7-Zip 作为通用后端. 生产构建按上游建议使用 Android AAR, JVM 测试使用桌面原生 JAR; R8 保留 JNI 所需的原始类名. 1.5.7-15 AAR 要求 `compileSdk 37`, 因而插件仅把编译 SDK 提升到 37, `targetSdk 36` 与 `minSdk 24` 不变. AAR 内 `arm64-v8a`、`armeabi-v7a`、`x86`、`x86_64` 四个 ELF 均由 NDK r29 构建, `LOAD` 对齐均为 `0x4000` 且含 `GNU_RELRO`; 最终 APK 还需通过 `zipalign -P 16` 复核. 插件的 `PluginInfo.supportedAbis` 同步公布完整四 ABI 清单.

版本、许可证、已知 CVE 范围、传递依赖、APK 增量和 ABI 结论记录在 [`THIRD_PARTY_NOTICES.md`](../../THIRD_PARTY_NOTICES.md). Zip4j、Commons Compress、XZ for Java、zstd-jni 与内嵌 Zstandard 的上游许可证或 NOTICE 原文保存在 `third_party`, 并作为应用资产打包.

## 后果

正面影响:

- 新格式只需注册后端和本地化显示名称, 主流程不再复制格式分支;
- 文件扩展名错误时, 引擎仍可通过实际目录结构识别 ZIP;
- 格式和条目能力可以分别驱动宿主预览与创建表单;
- 密码、文件名加密、分卷和修改能力分别声明, 不再把已支持的密码与仍不支持的文件名加密混为一个开关;
- 安全校验继续基于中立快照, 更换底层库不会绕过源身份、大小和 CRC 校验.

代价与未完成项:

- 当前只有 ZIP writer; 全部 TAR 族格式均为只读后端, 不会在创建或修改菜单中出现;
- TAR.ZST 为 APK 引入四个 Android ABI, 不支持这些 ABI 的设备不会发现该插件动作;
- 宿主动作初筛尚未提供文件魔数探测合同, 错误扩展名只能在插件已经获得输入后进行结构识别;
- ZIP 分卷和档案内容修改仍需要后端原型及事务合同;
- 其他格式的最终依赖选型需在取得候选项目一手资料与本地测量数据后继续更新本 ADR.

## 验证

- 单元测试验证 ZIP 能力表只声明可选密码, 不误报添加、删除、重命名、文件名加密或分卷;
- 单元测试验证改名为 `.bin` 的真实 ZIP 仍由结构探测识别;
- 单元测试验证带 `.zip` 后缀的普通文本不会被识别为 ZIP;
- 单元测试验证 reader 输出中立方法标识、条目能力和实际数据流;
- 单元测试验证 TAR 的 Unicode、空档案、V7 头、校验和损坏、截断数据、链接隔离、预览流和解压闭环;
- 单元测试验证 GZIP/XZ/BZIP2/Zstandard 外层签名、内部 TAR 二次验证、改名识别、空档案、连接流、截断与损坏尾部、Unicode 预览及格式中立解压;
- 现有扫描、预览、解压及创建回归测试继续通过;
- Android 仪器测试通过统一 reader、writer 与宿主只读档案会话验证全部 TAR 族格式的设备运行链路.
- 外部 7-Zip 22.00 AES-256/ZipCrypto 样本验证无密码浏览、正确密码读取和错误密码的 `PASSWORD/WRONG_PASSWORD` 诊断; AES-256 writer 产物由统一 reader 重新打开并校验.
- 外部 7-Zip 22.00 生成的 TAR/TAR.GZ/TAR.XZ/TAR.BZ2 及 bsdtar 3.8.4/libzstd 1.5.7 生成的 TAR.ZST 样本以固定 SHA-256 验证 Unicode 目录、条目元数据和内容流.
