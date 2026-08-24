# ADR 0001: 统一档案引擎与后端演进策略

- 状态: 已接受统一架构、ZIP 加密后端、7Z 读写后端与 TAR/TAR.GZ/TAR.XZ/TAR.BZ2/TAR.ZST 读写后端; 其他格式及未完成能力的后端选型待原型数据
- 日期: 2026-08-23
- 范围: 插件内部格式识别、列表、预览、解压、创建和未来档案修改

## 背景

项目最初只有 ZIP 实现. 扫描、预览和解压分别直接重新打开 ZIP, 创建页面也直接实例化 ZIP writer. 这种结构会让新增 7z 或 tar 后出现以下问题:

- UI 通过格式名称猜测能力, 容易显示后端无法完成的密码、分卷或修改选项;
- 同一条目的可预览、可解压和可修改状态无法分别表达;
- 扫描快照携带 ZIP 专用字段, 其他后端只能继续扩充条件分支;
- 格式识别容易退化为扩展名判断;
- 更换后端时必须同时修改 Activity、宿主会话和安全校验代码.

当前生产依赖包含 Apache Commons Compress 1.28.0、XZ for Java 1.12、zstd-jni 1.5.7-15 与 Zip4j 2.11.5. Commons Compress 负责 ZIP 目录元数据、7Z seekable reader/writer、TAR 头与条目流及 GZIP/XZ/BZIP2 压缩流适配; XZ for Java 提供纯 Java XZ 编解码器; zstd-jni 提供 Zstandard Android 流式编解码; Zip4j 接管 ZipCrypto/AES 条目数据、加密方法元数据、AES-256 输出, 并在 Android 7.x 代替会调用缺失 `FileTime` API 的 Commons ZIP 路径. ZIP、7Z 与 TAR 族均以 Android 运行时、宿主会话和外部工具样本覆盖.

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

### 2. 只注册已经通过能力验证的 ZIP、7Z 与 TAR 族后端

当前能力表如下:

| 能力 | ZIP 当前状态 | 7Z 当前状态 | TAR 当前状态 |
| --- | --- | --- | --- |
| 结构识别/列表/预览/打开/解压 | 支持; ZipCrypto/AES 条目在提供密码后可读取, 其他条目仍受具体压缩方法约束 | 支持普通与 solid 档案、常见压缩/过滤器链及 AES 内容/头部加密读取; 头部加密必须先提供密码, 解码内存上限为 262,144 KiB | 支持 TAR、TAR.GZ/TGZ、TAR.XZ/TXZ、TAR.BZ2/TBZ2 与 TAR.ZST/TZST; 识别 POSIX/GNU/Ant/AIX 及校验和有效的 V7 头 |
| 创建 | 支持普通 ZIP; 选择分卷后, 超过阈值时输出标准 `.z01 + .zip`, 较小结果仍为单个 `.zip`; 压缩级别 0 至 9, 可选密码使用 AES-256 | 支持非 solid 输出; 级别 0 使用 Copy, 1 至 9 使用 LZMA2, 可选密码使用 AES-256 内容加密 | 支持; TAR 为级别 0, GZIP 为 0 至 9, XZ/BZIP2/Zstandard 为 1 至 9, 不支持密码 |
| 添加/删除/重命名 | 不支持; 必须先实现重建事务 | 不支持; 必须先实现重建事务 | 不支持; 必须先实现重建事务 |
| 密码 | 可选; 无密码仍可浏览加密条目的目录元数据 | 可选; 仅内容加密时无密码可列出目录, 头部加密时索引返回 `PASSWORD_REQUIRED` | 不支持 |
| 文件名加密 | 不支持; ZIP AES-256 输出仍公开中央目录文件名 | 可读取头部加密输入, 但 writer 不创建加密头部, 因此创建选项保持不支持 | 不支持 |
| 分卷 | 创建支持标准 `.z01 + .zip`; 读取可识别结构并诊断缺卷, 但宿主尚无同级伴随卷读取合同; `.zip.001` 不支持 | 不支持; 未把多卷连接通道接入宿主文件会话 | 不支持 |

TAR 普通文件可预览和解压, 目录可创建; 符号链接、硬链接、FIFO、设备节点、未知特殊类型及稀疏项保留在列表中并准确声明不可打开、不可解压. 首阶段不跟随链接, 也不把特殊项物化为普通文件.

`jar`、`aar` 和 `war` 是 ZIP 后端的扩展名别名, 不是独立格式. `7z` 使用独立格式标识和后端. `tar` 及 `tar.gz`/`tgz`、`tar.xz`/`txz`、`tar.bz2`/`tbz2`、`tar.zst`/`tzst` 各有独立格式标识, 但共用 TAR 条目模型、源遍历器和 writer. 动作目录和 Intent 初筛使用已注册的可列表格式生成, 创建菜单只使用已注册 writer 的格式, 避免文档、菜单和实际后端能力出现三份不同来源.

Explorer Action 当前只接受不含句点的叶扩展名. 插件内部保留全部复合后缀, 对宿主目录发布 `gz`/`xz`/`bz2`/`zst` 叶扩展名, 收到 Activity 动作时核对完整文件名, 所有入口最终还会核对外层压缩签名和解压后的 TAR 结构. 这会让对应的独立压缩流在宿主中看到候选动作, 但插件不会把它们误识别为 TAR, 结构探测失败后也会清理暂存输入; 后续宿主协议若支持复合后缀, 可移除这一入口层折衷而无需改变后端.

### 3. ZIP、7Z 与 TAR 族采用已验证后端, 其他格式仍按原型数据选择

候选路线:

| 路线 | 预期用途 | 必须回答的问题 |
| --- | --- | --- |
| Commons Compress + Zip4j | ZIP 加密、7Z 与 tar 族的 Java 路线 | Android API 24 兼容性、AES/ZipCrypto/Zip64、7Z solid/头部加密/过滤器/写入边界、APK 增量和许可证清单 |
| libarchive | 多格式读取和 tar 族能力 | Android ABI 构建、seek/密码/分卷能力、JNI 取消、CVE 更新流程、压缩后的每 ABI 体积和许可证 |
| 7-Zip SDK 或受维护封装 | 7z 固实档案、头部加密和过滤器 | Android ABI/许可证、线程取消、密码内存处理、分卷、包体积及维护成本 |

任何候选进入 `app` 的生产依赖前都必须提交可复现原型数据:

1. 在 API 24 和当前最高 API 上读取真实工具生成的样本;
2. 覆盖 Unicode、超长名、空档案、损坏尾部、密码、错误密码、Zip64、固实档案、分卷和取消;
3. 记录 Debug/Release APK 的总体积和每个 ABI 增量;
4. 记录许可证、传递依赖、版本、已知 CVE 检查方法和升级责任;
5. 证明输入流、临时文件、密码缓冲和原生资源在取消及进程异常后可清理;
6. 对无法写入的格式返回只读能力, 不在 UI 中展示虚假修改动作.

ZIP 加密原型已满足 API 24、真实 AES/ZipCrypto 样本、错误密码分类、无新增 ABI 和 Release 体积门禁, 因此 Zip4j 2.11.5 已进入生产依赖. 它不替换 Commons Compress: 非加密条目仍优先走 Commons, 只有加密数据流以及 Android 7.x 兼容路径交给 Zip4j.

标准 ZIP 分卷创建继续复用 Zip4j 2.11.5 的 `SplitOutputStream`, 不引入新的依赖或 ABI. 该 API 需要本地可寻址文件, 所以 writer 在插件私有缓存的平面工作区完整生成卷组并通过 Zip4j 读回校验, 再把实际 `.zNN` 与最终 `.zip` 逐个复制到 Explorer Action v7 宿主待提交事务. 如果压缩结果未超过所选大小, Zip4j 合法地产生单个最终 `.zip`, writer 不人为填充或制造空编号卷. 整组名称在发布前精确预留, 自动冲突编号对所有实际输出一致; 每个宿主待提交卷都以只读描述符和本地卷逐字节比对, 全部通过后才清理私有工作区, 编号卷先提交且最终 `.zip` 最后提交. v7 没有批次原子提交或撤销已提交文件, 因而中途提交失败必须返回已提交卷、失败卷和可能的待提交残留, 不能宣称整组已回滚.

7Z 复用既有 Commons Compress 1.28.0, 不新增 Maven 组件或 ABI. 官方 `SevenZFile`/`SevenZOutputFile` API 与 1.28.0 源码确认 reader 支持 seekable channel、solid 档案、AES-256-SHA256 及常见方法链, writer 的密码构造器会对条目内容加入 AES 层, 但仍把文件名写入未加密头部. 因此 reader 公布普通/solid/内容加密/头部加密能力, writer 只公布非 solid 与可选内容加密, 不公布创建文件名加密或分卷. reader 与 writer 都从宿主文件描述符构造自有 seekable channel, 不依赖 Android API 26 才提供的 `Path` 路线; reader 把 Commons 的最大内存限制固定为 262,144 KiB. solid 档案按随机条目打开时可能需要重放同一 solid block, 这是当前 Java 后端的性能边界, 不伪装为常数时间访问.

创建 7Z 时, 级别 0 映射为 Copy, 级别 1 至 9 映射为对应 preset 的 LZMA2; 每个源项目独立写入, 因而输出不是 solid. 受控源遍历、Unicode 名称、空目录、修改时间、源大小复核、进度、取消、待提交输出和失败回滚与其他 writer 共用. 只有在需要创建加密头部、分卷、档案内修改或 Java 后端无法接受的 solid 随机读取性能时, 才启动 libarchive/原生 7-Zip 原型; 当前交付不为这些未完成能力引入额外原生后端.

未压缩 TAR 复用既有 Commons Compress 1.28.0 生产依赖, 不新增 Maven 组件或 ABI. reader 会完整遍历头部、校验每个可见条目的头校验和并在按条目读取时重新核对快照元数据; 数据流仍经过统一源身份与实际大小校验. writer 使用 UTF-8 POSIX PAX 处理长路径和非 ASCII 名称, 只写普通文件与目录, 不跟随源符号链接, 并通过宿主待提交输出事务发布结果.

TAR.GZ 使用 Commons Compress 自带的 GZIP 流, TAR.XZ 使用 Commons Compress 适配器与 XZ for Java 1.12. 两者按官方建议在缓冲压缩流外叠加同一 `TarArchiveInputStream`, 支持连接流, 并在外层签名之后再次验证 TAR 结构; 索引结束时继续读取压缩流尾部, 让 GZIP/XZ 完整性校验实际执行. writer 以同一层次反向组合 `TarArchiveOutputStream` 与对应压缩输出流, 并把表单级别映射到各自编码器. 流式压缩容器无法提供准确的逐条目压缩大小, 因而中立模型记录 `-1` 未知值, 不伪造压缩比. XZ 解码器设置 262,144 KiB 内存上限; 超限返回明确的不支持诊断, 不让字典申请直接耗尽进程内存. XZ for Java 是纯 Java 0BSD 组件, Release APK 实测增加 21,464 bytes 且不新增 ABI.

TAR.BZ2 继续复用 Commons Compress 的纯 Java BZIP2 输入/输出流, 不增加 Maven 组件或 ABI. TAR.ZST 使用 zstd-jni 的连续帧输入流与带 checksum 的输出流; reader 接受标准帧与 skippable frame magic, 将最大窗口限制为 `2^28` bytes, 并与其他压缩 TAR 一样在索引关闭前消费尾部以触发完整性检查. 外层只有 Zstandard magic 且不足最小帧长度时归类为已识别格式的截断, 而不是未知格式.

zstd-jni 只用于 Zstandard 流式编解码, 不代表选择 libarchive 或原生 7-Zip 作为通用后端. 生产构建按上游建议使用 Android AAR, JVM 测试使用桌面原生 JAR; R8 保留 JNI 所需的原始类名. 1.5.7-15 AAR 要求 `compileSdk 37`, 因而插件仅把编译 SDK 提升到 37, `targetSdk 36` 与 `minSdk 24` 不变. AAR 内 `arm64-v8a`、`armeabi-v7a`、`x86` 与 `x86_64` 四个 ELF 均由 NDK r29 构建, `LOAD` 对齐均为 `0x4000` 且含 `GNU_RELRO`; 最终 APK 还需通过 `zipalign -P 16` 复核. 插件的 `PluginInfo.supportedAbis` 同步公布完整四 ABI 清单.

版本、许可证、已知 CVE 范围、传递依赖、APK 增量和 ABI 结论记录在 [`THIRD_PARTY_NOTICES.md`](../../THIRD_PARTY_NOTICES.md). Zip4j、Commons Compress、XZ for Java、zstd-jni 与内嵌 Zstandard 的上游许可证或 NOTICE 原文保存在 `third_party`, 并作为应用资产打包.

## 后果

正面影响:

- 新格式只需注册后端和本地化显示名称, 主流程不再复制格式分支;
- 文件扩展名错误时, 引擎仍可通过实际结构识别 ZIP、7Z 与 TAR 族;
- 格式和条目能力可以分别驱动宿主预览与创建表单;
- 密码、文件名加密、分卷和修改能力分别声明, 不再把已支持的密码与仍不支持的文件名加密混为一个开关;
- 安全校验继续基于中立快照, 更换底层库不会绕过源身份、大小和 CRC 校验.
- 不可配置的索引结构上限与可配置的解压资源预算分离; 超过预算不会让某个后端格式失去浏览能力, 单次确认也不会绕过路径或完整性校验.
- 宿主提供的只读普通文件描述符通过进程自身 `fdinfo` 模式位、`fstat`、`lseek`、首尾 `pread` 与报告大小验证后由会话直接租用; ZIP、7Z 与 TAR reader 在同一租约上分别建立拥有独立逻辑位置的 `pread` 通道, 不依赖 `/proc/self/fd` 路径重新打开. 管道、不可 seek 或可写输入、Android 7, 以及必须使用本地 `File` 的兼容后端才复制到有空间边界和生命周期清理的私有缓存; 当前加密 ZIP 在 Zip4j 请求本地文件时才延迟物化, 普通 ZIP 保持直读.

代价与未完成项:

- ZIP、7Z 与 TAR 族已有创建 writer, 但全部后端仍未开放档案内添加、删除或重命名;
- TAR.ZST 为 APK 引入四个 Android ABI, 不支持这些 ABI 的设备不会发现该插件动作;
- 宿主动作初筛尚未提供文件魔数探测合同, 错误扩展名只能在插件已经获得输入后进行结构识别;
- ZIP 分卷读取、`.zip.001` 连续分片、7Z 分卷、7Z 头部加密创建和档案内容修改仍需要后端能力及事务合同; 标准 `.z01 + .zip` 创建已经落地, 但不等同于现有单源会话可以读取伴随卷;
- 其他格式的最终依赖选型需在取得候选项目一手资料与本地测量数据后继续更新本 ADR.

## 验证

- 单元测试验证 ZIP 能力表只声明可选密码与可选分卷, 不误报添加、删除、重命名或文件名加密;
- 单元测试验证改名为 `.bin` 的真实 ZIP 仍由结构探测识别;
- 单元测试验证带 `.zip` 后缀的普通文本不会被识别为 ZIP;
- 单元测试使用固定 SHA-256 的 Zip4j 2.11.5 两卷样本验证 `.z01` 首卷标记、`.zip` 末卷 EOCD、缺卷 `INDEX/MISSING_VOLUME` 映射、精确卷名和恶意大卷数下的有界摘要; Android 运行时另外创建真实分卷复核同一链路;
- 单元测试覆盖 ZIP 分卷预设、自定义 MiB 边界、完整卷组名称长度和统一编号; Android 仪器测试覆盖普通/AES 分卷往返、伴随卷冲突、询问策略、取消、提交中途失败及回滚失败. 真实 AutoJs6 宿主产物由独立 7-Zip 完整测试、解压并与源文件核对 SHA-256;
- 单元测试验证 reader 输出中立方法标识、条目能力和实际数据流;
- 单元测试使用 7-Zip 22.00 夹具验证 solid LZMA2、BCJ + LZMA2、AES 内容加密、AES 头部加密、无密码/错误密码/正确密码、签名和截断诊断; 能力表不误报创建时文件名加密、分卷或档案内修改;
- 单元测试验证 TAR 的 Unicode、空档案、V7 头、校验和损坏、截断数据、预览流和解压闭环; 指向父级或绝对路径的符号/硬链接保持不可打开和不可解压, 4 GiB old GNU sparse 条目只公开真实大小与压缩占用元数据, 不作为普通文件读取;
- 单元测试验证 GZIP/XZ/BZIP2/Zstandard 外层签名、内部 TAR 二次验证、改名识别、空档案、连接流、截断与损坏尾部、Unicode 预览及格式中立解压;
- 单元测试验证父级穿越、绝对路径、Windows 驱动器前缀、双向文本控制符和非法输出根不会创建输出; 指向档案外的 TAR 符号/硬链接不可提取且不会到达输出 writer;
- 现有扫描、预览、解压及创建回归测试继续通过;
- 单元测试覆盖六个资源预算维度、兼容/严格档位顺序、未知压缩大小、超预算浏览、写出前拒绝及确认后仅扩展至声明值;
- 单元测试验证解压进度优先使用字节比例、未知大小时回退到条目比例, 并以单调时钟计算平均速度和剩余时间; 时钟回退、零字节档案和清理阶段不会产生负值或伪造 ETA;
- 单元测试验证普通取消会删除本次新建根, 清理失败会携带准确的残留输出位置和 `CLEANUP/OUTPUT_FAILURE` 诊断; 即使取消异常传播丢失 suppressed 异常, `CLEANUP_FAILED` 事件仍能把残留根交给 UI;
- 单元测试验证选择性解压范围在档案根、可提取内部目录、只读隔离目录和勾选状态下只公布真实可用选项, 并把勾选集合有序去重复制, 不保留可变 UI 集合的引用;
- 单元测试验证输出名称冲突的询问/跳过/覆盖/自动重命名策略、兼容冲突的“应用到全部”、不兼容覆盖的安全重命名回退、扩展名前编号、结果计数和跳过后的真实进度总量;
- 单元测试以固定生成的 8 MiB 高压缩比 ZIP、20,001 条目 ZIP、512/1,025 层路径和畸形非关键 extra field 验证元数据浏览、资源预算、硬结构上限及兼容读取行为;
- Android 仪器测试覆盖七种当前 reader 的可 seek 描述符直读、源名称移除后的租约可用性、管道与可写描述符回退、报告大小变化、失败清理、活跃缓存保护、过期目录回收, 以及加密 ZIP 在后端提出本地文件要求后延迟物化并完成 AES 解密; 未知大小管道超过缓存复制预算时还必须关闭两端并移除部分缓存;
- Android 仪器测试通过统一 reader、writer 与宿主会话验证 7Z 及全部 TAR 族格式的设备运行链路, 包含 Unicode 路径、空目录、未知源大小、取消和源大小变化后的事务回滚.
- Android 仪器测试使用一个按 NFC 与大小写折叠名称、并在冲突时故意返回旧文档 ID 的 Debug DocumentsProvider, 验证等价根名称安全编号、未提供冲突决定时封闭失败、本次输出根回滚和既有内容逐字节不变; 同一 provider 还能拒绝删除, 用于验证取消回滚成功路径及清理拒绝时残留根名称/URI 的精确报告; 该 provider 不进入 Release 清单.
- Android 仪器测试验证动作目录发布独立的只读 `selective-extract` Activity 入口, Intent 不会误触发 `extract-to` 快捷路径; 真实 ZIP 管理页在根目录、内部目录和勾选后分别显示全部、当前目录与当前勾选范围.
- Android 仪器测试在名称按 NFC 与大小写折叠的真实 DocumentsProvider 上验证四种冲突策略、逐项询问的“应用到全部”、类型不一致时禁用覆盖、策略选择跨 Activity 重建恢复, 以及既有输出根和哨兵内容始终保持不变.
- 外部 7-Zip 22.00 AES-256/ZipCrypto 样本验证无密码浏览、正确密码读取和错误密码的 `PASSWORD/WRONG_PASSWORD` 诊断; AES-256 writer 产物由统一 reader 重新打开并校验.
- 外部 7-Zip 22.00 生成的 TAR/TAR.GZ/TAR.XZ/TAR.BZ2 及 bsdtar 3.8.4/libzstd 1.5.7 生成的 TAR.ZST 样本以固定 SHA-256 验证 Unicode 目录、条目元数据和内容流.
- 插件在 `emulator-5554` 通过真实 AutoJs6 压缩入口创建 TAR.ZST 与普通 TAR; bsdtar 3.8.4/libarchive 3.8.4 完整提取 TAR.ZST, 7-Zip 22.00 与 bsdtar 均验证普通 TAR, 创建产物还能由宿主原生档案页面重新打开并预览哈希一致的 Unicode 条目.
- 插件在 `emulator-5554` 通过真实 AutoJs6 压缩入口创建普通与 AES-256 内容加密 7Z; 7-Zip 22.00 验证非 solid/LZMA2、正确与错误密码行为、完整提取、空目录和 Unicode 内容 SHA-256, 宿主原生档案页面重新打开普通产物并通过路径栏与预览器读取同一 Unicode 条目.
