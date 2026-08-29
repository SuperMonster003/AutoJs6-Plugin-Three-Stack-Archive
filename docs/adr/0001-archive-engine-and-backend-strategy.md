# ADR 0001: 统一档案引擎与后端演进策略

- 状态: 已接受统一架构、ZIP 加密与单卷事务重建后端、7Z 读写后端、RAR4/RAR5 只读后端与 TAR/TAR.GZ/TAR.XZ/TAR.BZ2/TAR.ZST 读写后端; 其他格式及未完成能力的后端选型待原型数据
- 日期: 2026-08-27
- 范围: 插件内部格式识别、列表、预览、解压、创建和档案修改

## 背景

项目最初只有 ZIP 实现. 扫描、预览和解压分别直接重新打开 ZIP, 创建页面也直接实例化 ZIP writer. 这种结构会让新增 7z 或 tar 后出现以下问题:

- UI 通过格式名称猜测能力, 容易显示后端无法完成的密码、分卷或修改选项;
- 同一条目的可预览、可解压和可修改状态无法分别表达;
- 扫描快照携带 ZIP 专用字段, 其他后端只能继续扩充条件分支;
- 格式识别容易退化为扩展名判断;
- 更换后端时必须同时修改 Activity、宿主会话和安全校验代码.

当前生产依赖包含 Apache Commons Compress 1.28.0、XZ for Java 1.12、zstd-jni 1.5.7-15、Zip4j 2.11.5、Junrar 8.1.0 与 SLF4J 2.0.17 API/NOP. Commons Compress 负责 ZIP 目录元数据、7Z seekable reader/writer、TAR 头与条目流及 GZIP/XZ/BZIP2 压缩流适配; XZ for Java 提供纯 Java XZ 编解码器; zstd-jni 提供 Zstandard Android 流式编解码; Zip4j 接管 ZipCrypto/AES 条目数据、加密方法元数据、AES-256 输出, 并在 Android 7.x 代替会调用缺失 `FileTime` API 的 Commons ZIP 路径; Junrar 负责只读 RAR4/RAR5 解析和加密内容读取, SLF4J NOP 只满足其日志接口且不产生输出. ZIP、7Z、RAR 与 TAR 族均以 Android 运行时、宿主会话和外部工具样本覆盖.

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

### 2. 只注册已经通过能力验证的 ZIP、7Z、RAR 与 TAR 族后端

当前能力表如下:

| 能力 | ZIP 当前状态 | 7Z 当前状态 | RAR 当前状态 | TAR 当前状态 |
| --- | --- | --- | --- | --- |
| 结构识别/列表/预览/打开/解压 | 支持; ZipCrypto/AES 条目在提供密码后可读取, 其他条目仍受具体压缩方法约束 | 支持普通与 solid 档案、常见压缩/过滤器链及 AES 内容/头部加密读取; 头部加密必须先提供密码, 解码内存上限为 262,144 KiB | 支持单卷及完整分卷 RAR4/RAR5, 包括内容/头部加密; 头部加密必须先提供密码, 字典上限为 256 MiB | 支持 TAR、TAR.GZ/TGZ、TAR.XZ/TXZ、TAR.BZ2/TBZ2 与 TAR.ZST/TZST; 识别 POSIX/GNU/Ant/AIX 及校验和有效的 V7 头 |
| 创建 | 支持普通 ZIP; 选择分卷后, 超过阈值时输出标准 `.z01 + .zip`, 较小结果仍为单个 `.zip`; 压缩级别 0 至 9, 可选密码使用 AES-256 | 支持非 solid 输出; 级别 0 使用 Copy, 1 至 9 使用 LZMA2, 可选密码使用 AES-256 内容加密 | 不支持; 不注册 writer | 支持; TAR 为级别 0, GZIP 为 0 至 9, XZ/BZIP2/Zstandard 为 1 至 9, 不支持密码 |
| 添加/删除/重命名 | 普通单卷 `.zip` 支持添加文件、导入完整目录树、新建空目录、删除和重命名; 通过 Explorer Action v8 重建、完整校验并原子替换. JAR/AAR/WAR 只读, 分卷 ZIP 在结构探测后拒绝修改 | 普通单卷、未加密、非 solid、路径与方法安全且解码器位于 64 MiB 修改预算内的输入支持完整重建; 加密、solid、分卷及超预算变体保持只读 | 不支持; 明确保持只读 | 仅含安全普通文件和目录的 TAR、TAR.GZ/TGZ、TAR.XZ/TXZ、TAR.BZ2/TBZ2 与 TAR.ZST/TZST 支持完整重建; 特殊条目 TAR 保持只读 |
| 密码 | 可选; 无密码仍可浏览加密条目的目录元数据 | 可选; 仅内容加密时无密码可列出目录, 头部加密时索引返回 `PASSWORD_REQUIRED` | 可选; 内容加密时可先列出目录, 头部加密时索引返回 `PASSWORD_REQUIRED` | 不支持 |
| 文件名加密 | 不支持; ZIP AES-256 输出仍公开中央目录文件名 | 可读取头部加密输入, 但 writer 不创建加密头部, 因此创建选项保持不支持 | 可读取加密头部, 但不创建 RAR, 因此创建选项保持不支持 | 不支持 |
| 分卷 | 创建及只读打开支持标准 `.z01 + .zip`; Explorer Action v14/v12 另支持从 `.zip.001` 首卷只读打开连续编号卷组 | Explorer Action v14/v12 支持从 `.7z.001` 首卷只读打开连续编号卷组; 不支持分卷创建 | v12 支持现代 `partN.rar` 及传统 `.rar/.r00` 只读卷组; 缺卷返回 `MISSING_VOLUME`, 卷身份变化返回 `SOURCE_CHANGED` | 不支持 |

TAR 普通文件可预览和解压, 目录可创建; 符号链接、硬链接、FIFO、设备节点、未知特殊类型及稀疏项保留在列表中并准确声明不可打开、不可解压. 首阶段不跟随链接, 也不把特殊项物化为普通文件.

`jar`、`aar` 和 `war` 是 ZIP 后端的只读扩展名别名, 不是独立格式, 也不继承 `.zip` 替换入口. `7z` 与 `rar` 使用各自独立的格式标识和后端. `tar` 及 `tar.gz`/`tgz`、`tar.xz`/`txz`、`tar.bz2`/`tbz2`、`tar.zst`/`tzst` 各有独立格式标识, 但共用 TAR 条目模型、源遍历器和 writer. 动作目录和 Intent 初筛使用已注册的可列表格式生成, 创建菜单只使用已注册 writer 的格式, 管理动作另从真实 mutation provider 生成安全叶扩展名与复合后缀; 当前发布 `.zip`、`.7z`、`.tar`、`.tgz`、`.txz`、`.tbz2`、`.tzst` 与 `.tar.gz`、`.tar.xz`、`.tar.bz2`、`.tar.zst`, 并在索引后复核单卷结构、solid/加密状态、方法资源和条目类型, 避免文档、菜单和实际后端能力出现三份不同来源.

Explorer Action v14 增加有界复合文件名后缀字段, 当前用于精确发布 `.zip.001`、`.7z.001` 与 `.tar.gz`/`.tar.xz`/`.tar.bz2`/`.tar.zst`, 不把任意 `.001` 或普通 `.gz`/`.xz`/`.bz2`/`.zst` 压缩流误识别为档案. `tgz`/`txz`/`tbz2`/`tzst` 继续作为无歧义叶别名发布; 收到动作后仍核对外层压缩签名和解压后的 TAR 结构.

### 3. ZIP、7Z、RAR 与 TAR 族采用已验证后端, 其他格式仍按原型数据选择

候选路线:

| 路线 | 预期用途 | 必须回答的问题 |
| --- | --- | --- |
| Commons Compress + Zip4j | ZIP 加密、7Z 与 tar 族的 Java 路线 | Android API 24 兼容性、AES/ZipCrypto/Zip64、7Z solid/头部加密/过滤器/写入边界、APK 增量和许可证清单 |
| Junrar | RAR4/RAR5 只读 Java 路线 | Android API 24、内容/头部加密、seekable 描述符、分卷边界、字典内存、密码残留、UnRAR 许可限制和安全公告 |
| libarchive | 多格式读取和 tar 族能力 | Android ABI 构建、seek/密码/分卷能力、JNI 取消、CVE 更新流程、压缩后的每 ABI 体积和许可证 |
| 7-Zip SDK 或受维护封装 | 7z 固实档案、头部加密和过滤器 | Android ABI/许可证、线程取消、密码内存处理、分卷、包体积及维护成本 |

任何候选进入 `app` 的生产依赖前都必须提交可复现原型数据:

1. 在 API 24 和当前最高 API 上读取真实工具生成的样本;
2. 覆盖 Unicode、超长名、空档案、损坏尾部、密码、错误密码、Zip64、固实档案、分卷和取消;
3. 记录 Debug/Release APK 的总体积和每个 ABI 增量;
4. 记录许可证、传递依赖、版本、已知 CVE 检查方法和升级责任;
5. 证明输入流、临时文件、密码缓冲和原生资源在取消及进程异常后可清理;
6. 对无法写入的格式返回只读能力, 不在 UI 中展示虚假修改动作.

ZIP 加密原型已满足 API 24、真实 AES/ZipCrypto 样本、错误密码分类、无新增 ABI 和 Release 体积门禁, 因此 Zip4j 2.11.5 已进入生产依赖. 它不替换 Commons Compress: 非加密条目仍优先走 Commons, 只有加密数据流以及 Android 7.x 兼容路径交给 Zip4j. 普通单卷 ZIP 修改共用两者的已验证读取能力, 先把所选文件或目录树固化为有界不可变计划, 再把保留与新增条目重建到 v8 宿主待提交输出; Stored/Deflate、ZipCrypto/AES、可用时间戳与实际内容会被校验, 注释、非必要 extra metadata 和 Unix 权限属性则会规范化.

标准 ZIP 分卷创建继续复用 Zip4j 2.11.5 的 `SplitOutputStream`, 不引入新的依赖或 ABI. 该 API 需要本地可寻址文件, 所以 writer 在插件私有缓存的平面工作区完整生成卷组并通过 Zip4j 读回校验, 再把实际 `.zNN` 与最终 `.zip` 逐个复制到宿主待提交事务. 如果压缩结果未超过所选大小, Zip4j 合法地产生单个最终 `.zip`, writer 不人为填充或制造空编号卷. 整组名称在发布前精确预留, 自动冲突编号对所有实际输出一致; 每个宿主待提交卷都以 v7 只读描述符和本地卷逐字节比对, 全部通过后才清理私有工作区. 两个及以上卷通过 Explorer Action v15 一次登记为可恢复输出批次; 宿主先持久记录父目录和文件身份, 再顺序发布并同步目录, 失败或重启时仅回滚身份仍匹配的成员. 该方案不宣称底层多文件改名原子, 但消除了正常故障路径中的部分成功语义; 外部改写会保留并进入人工恢复状态.

7Z 复用既有 Commons Compress 1.28.0, 不新增 Maven 组件或 ABI. 官方 [`SevenZFile`/`SevenZOutputFile` API](https://commons.apache.org/proper/commons-compress/apidocs/org/apache/commons/compress/archivers/sevenz/SevenZOutputFile.html)、[格式限制说明](https://commons.apache.org/proper/commons-compress/limitations.html)与 1.28.0 源码确认 reader 支持 seekable channel、solid 档案、AES-256-SHA256 及常见方法链, writer 的密码构造器会对条目内容加入 AES 层, 但没有 solid、头部压缩或头部加密写出能力. 因此 reader 公布普通/solid/内容加密/头部加密能力, writer 只公布非 solid 与可选内容加密, 不公布创建文件名加密或分卷. reader 与 writer 都从宿主文件描述符构造自有 seekable channel, 不依赖 Android API 26 才提供的 `Path` 路线; reader 把 Commons 的普通浏览最大内存限制固定为 262,144 KiB. solid 档案按随机条目打开时可能需要重放同一 solid block, 这是当前 Java 后端的性能边界, 不伪装为常数时间访问.

创建 7Z 时, 级别 0 映射为 Copy, 级别 1 至 9 映射为对应 preset 的 LZMA2; 每个源项目独立写入, 因而输出不是 solid. 受控源遍历、Unicode 名称、空目录、修改时间、源大小复核、进度、取消、待提交输出和失败回滚与其他 writer 共用.

档案修改只对普通单卷、未加密、非 solid、路径安全、全部保留条目均可读取且源 LZMA/LZMA2 解码器报告内存不超过 64 MiB 的输入开放. 输出固定使用非 solid LZMA2 preset 3, 字典为 4 MiB, XZ for Java 报告编码内存 31,410 KiB, 并以 40 MiB 显式上限封闭. provider 在预留输出前给出保留与新增内容的事实读取量, 直接把源条目流重压缩到 v8 待提交输出, 核对源身份、声明大小、可用 CRC 和内容指纹, 再通过统一 verifier 完整重开结果后提交. 取消、源变化、损坏待提交头部或真实写入失败都中止事务并保留原档案.

加密输入无论是否已提供密码都保持只读, 因为当前 writer 无法保留头部加密, 不能把修改静默降级为公开文件名. solid 输入同样保持只读, 避免在无法准确表达 block 重放成本时给出虚假的预检工作量. 编号或其他多卷输入、危险路径、不支持的方法、anti item 和超出修改资源预算的输入也不会进入 provider. 只有在需要创建加密头部、分卷、修改上述只读变体或 Java 后端无法接受的 solid 随机读取性能时, 才启动 libarchive/原生 7-Zip 原型; 当前交付不为这些边界引入额外原生后端.

RAR 读取采用 Junrar 8.1.0 的纯 Java reader, 不为当前能力引入 JNI 或新的 ABI. `RarArchiveBackend` 通过应用自己的只读 `SeekableByteChannel` 适配器读取已经验证的宿主描述符, 不调用 Junrar 的文件系统解压便利 API; 条目数据继续经过中立源身份、路径、声明/实际大小、可用 CRC、资源预算和输出事务检查. RAR4、RAR5、内容加密及头部加密已由固定样本和 Android 运行时验证. Junrar 的最大字典设置为 256 MiB, 防止档案元数据直接触发无界内存请求.

Explorer Action v12 由宿主按用户所选卷名称生成最多 127 个确定性同级候选, 只收录实际存在、可读、非符号链接的普通文件. 插件得到不透明 ID、受控显示名、大小和修改时间, 不得到目录或路径; Junrar 的 `VolumeManager` 只能按既有卷名规则请求目录中的 ID. 宿主在目录与每次打开时复核调用 UID、设备号、inode、大小、修改时间和可读性, 插件在扫描、读取与解压前后再次比较目录快照. 提供完整卷组时可只读浏览、预览和解压; 缺少后续卷返回 `MISSING_VOLUME`, 会话期间卷变化返回 `SOURCE_CHANGED`.

RAR 刻意保持只读. Junrar 所含 UnRAR 源码许可允许软件免费处理和分发 RAR, 但明确禁止使用这些源码重建专有的 RAR 压缩算法, 并要求修改版在文档和源码注释中保留这一限制. 本项目不注册 RAR writer, 不创建、添加、删除或重命名 RAR 内容; 若未来需要写入, 必须选择独立且许可允许的方案并重新完成 ADR, 不能从当前 reader 推导兼容压缩器.

本地构建以精确源码片段门禁替换 Junrar 的 `ArchiveOptions` 和 `Rar5Crypt` 类: builder 交出密码后立即清零源数组, options 在关闭时清零保留副本, 并禁用跨会话的 JVM 全局 RAR5 密钥派生缓存. SLF4J 2.0.17 API/NOP 只满足 Junrar 的日志接口, 不产生额外日志. 这些措施减少密码与派生材料的可达时间, 但 Java、Binder 或密码学实现仍可能产生无法主动擦除的短生命周期副本, 因而只承诺尽力清理.

未压缩 TAR 复用既有 Commons Compress 1.28.0 生产依赖, 不新增 Maven 组件或 ABI. reader 会完整遍历头部、校验每个可见条目的头校验和并在按条目读取时重新核对快照元数据; 数据流仍经过统一源身份与实际大小校验. writer 使用 UTF-8 POSIX PAX 处理长路径和非 ASCII 名称, 只写普通文件与目录, 不跟随源符号链接, 并通过宿主待提交输出事务发布结果.

TAR.GZ 使用 Commons Compress 自带的 GZIP 流, TAR.XZ 使用 Commons Compress 适配器与 XZ for Java 1.12. 两者按官方建议在缓冲压缩流外叠加同一 `TarArchiveInputStream`, 支持连接流, 并在外层签名之后再次验证 TAR 结构; 索引结束时继续读取压缩流尾部, 让 GZIP/XZ 完整性校验实际执行. writer 以同一层次反向组合 `TarArchiveOutputStream` 与对应压缩输出流, 并把表单级别映射到各自编码器. 流式压缩容器无法提供准确的逐条目压缩大小, 因而中立模型记录 `-1` 未知值, 不伪造压缩比. XZ 解码器设置 262,144 KiB 内存上限; 超限返回明确的不支持诊断, 不让字典申请直接耗尽进程内存. XZ for Java 是纯 Java 0BSD 组件, Release APK 实测增加 21,464 bytes 且不新增 ABI.

TAR.GZ、TAR.XZ、TAR.BZ2 与 TAR.ZST mutation provider 复用普通 TAR 的一次顺序重建器, 在同一管线中解压源容器并分别以 GZIP 级别 6、XZ preset 4、BZIP2 block-size preset 6 和 Zstandard 级别 3 直接写入宿主持有的待提交输出, 不建立私有未压缩 TAR. 重建后的 GZIP 固定 `mtime=0`、不写原文件名或注释、OS 字段为 255; XZ 固定使用 4 MiB 字典与 CRC64 检查; BZIP2 固定使用 600,000-byte block 并产生 `BZh6` 流; Zstandard 固定为单线程、1 MiB 窗口并写入 XXH64 帧 checksum. XZ for Java 1.12 对 preset 4 报告 48,058 KiB 编码内存, provider 另以 64 MiB 显式上限拒绝超预算配置. BZIP2 provider 按 Commons Compress 1.28 的主字节区、`fmap` 映射区、`sfmap` 区与后备 `eclass` 区给出 `13 * blockBytes + 512 KiB` 保守最坏工作区估算; preset 6 为 8,324,288 bytes, 低于 16 MiB 显式预算. Zstandard 1.5.7 的 `ZSTD_estimateCStreamSize_usingCCtxParams()` 在 G8441/arm64 对固定参数给出 2,614,809 bytes, 加上 `ZSTD_CStreamOutSize()` 对应的 131,591-byte zstd-jni 输出缓冲共 2,746,400 bytes, 低于 8 MiB 显式预算. 这些自动修改参数都不影响创建表单原有的 1 至 9 级显式选择. 能力模型会公布四种包装层的压缩设置与容器元数据规范化. 关闭 reader 时的容器尾部排空逐块调用取消检查; GZIP/XZ/BZIP2/Zstandard 尾部或 checksum 损坏、真实输出写入失败、源身份变化及完整读回失败都留在 v8 中止边界内.

TAR.BZ2 继续复用 Commons Compress 的纯 Java BZIP2 输入/输出流及现有 TAR 重建器, 不增加 Maven 组件或 ABI. TAR.ZST 使用 zstd-jni 的连续帧输入流与带 checksum 的输出流; reader 接受标准帧与 skippable frame magic, 将最大窗口限制为 `2^28` bytes, 并与其他压缩 TAR 一样在索引关闭前消费尾部以触发完整性检查. 外层只有 Zstandard magic 且不足最小帧长度时归类为已识别格式的截断, 而不是未知格式. 两种容器中只含安全普通文件与目录的输入都可进入上述 mutation provider; 特殊 TAR 条目继续保持只读.

TAR 族与 7Z 的完整重建现在共用格式中立的 `ArchiveRewritePlan`、`ArchiveRewriteEntry`、`ArchiveRewriteSource` 与 `ArchiveRewritePlanner`. 共享 planner 只负责生成最终档案树、检查路径与条目冲突、计算事实型工作量、绑定源版本并汇总辅助元数据影响; 它不查询 TAR 能力表, 也不隐式套用某一种格式的保留条目规则. 每个 mutation provider 必须显式传入自己的能力集合和保留条目校验器: TAR 适配器只接受可安全重写的普通 TAR 文件与目录, 7Z 适配器继续执行单卷、非加密、非 solid、路径、方法及资源预算边界. ZIP 仍保留现有的格式专属计划器; 共享层的引入不扩大任何格式、操作或只读变体的支持范围.

zstd-jni 只用于 Zstandard 流式编解码, 不代表选择 libarchive 或原生 7-Zip 作为通用后端. 生产构建按上游建议使用 Android AAR, JVM 测试使用桌面原生 JAR; R8 保留 JNI 所需的原始类名. 1.5.7-15 AAR 要求 `compileSdk 37`, 因而插件仅把编译 SDK 提升到 37, `targetSdk 36` 与 `minSdk 24` 不变. AAR 内 `arm64-v8a`、`armeabi-v7a`、`x86` 与 `x86_64` 四个 ELF 均由 NDK r29 构建, `LOAD` 对齐均为 `0x4000` 且含 `GNU_RELRO`; 最终 APK 还需通过 `zipalign -P 16` 复核. 插件的 `PluginInfo.supportedAbis` 同步公布完整四 ABI 清单.

版本、许可证、已知 CVE 范围、传递依赖、APK 增量和 ABI 结论记录在 [`THIRD_PARTY_NOTICES.md`](../../THIRD_PARTY_NOTICES.md). Zip4j、Commons Compress、XZ for Java、zstd-jni、内嵌 Zstandard、Junrar 与 SLF4J 的上游许可证或 NOTICE 原文保存在 `third_party`, 并作为应用资产打包.

## 后果

正面影响:

- 新格式只需注册后端和本地化显示名称, 主流程不再复制格式分支;
- 文件扩展名错误时, 引擎仍可通过实际结构识别 ZIP、7Z、RAR 与 TAR 族;
- 格式和条目能力可以分别驱动宿主预览与创建表单;
- 密码、文件名加密、分卷和修改能力分别声明, 不再把已支持的密码与仍不支持的文件名加密混为一个开关;
- 添加、删除与重命名通过格式无关的 `ArchiveMutationProvider` 注册; Activity 不再直接实例化 ZIP mutator, 能力表、动作目录和实际 provider 由引擎启动时交叉核对;
- TAR 族与 7Z 的 provider 复用格式中立的完整重建计划, 但必须显式提供各自的能力与保留条目校验政策; 共享 planner 不拥有任何格式注册表或隐式安全默认值;
- provider 在宿主输出预留前固化源版本、最终条目计划、事实型工作量和辅助元数据政策. 输出大小受压缩比影响时保持未知, 不用看似精确的猜测替代预检事实;
- 管理页通过统一状态模型展示探测格式、内容统计、真实操作集合及稳定的动态只读原因; 可写格式的辅助元数据影响不再只存在于后端实现中;
- Activity 必须在任何宿主输出预留之前展示 provider 的不可变计划, 只有用户明确继续后才执行该计划. 取消预检不产生宿主写调用, 不创建待提交输出, 也不丢失当前选择;
- Explorer Action v8 的目标替换事务不依赖档案格式, ZIP、7Z 与 TAR 整包重建直接复用同一合同. Explorer Action v18 只补充 v8 不具备的宿主持有最近版本与一次恢复, 不把新增可写格式本身误当作升级协议的理由;
- 安全校验继续基于中立快照, 更换底层库不会绕过源身份、大小和 CRC 校验.
- 不可配置的索引结构上限与可配置的解压资源预算分离; 超过预算不会让某个后端格式失去浏览能力, 单次确认也不会绕过路径或完整性校验.
- 宿主提供的只读普通文件描述符通过进程自身 `fdinfo` 模式位、`fstat`、`lseek`、首尾 `pread` 与报告大小验证后由会话直接租用; ZIP、7Z、RAR 与 TAR reader 在同一租约上分别建立拥有独立逻辑位置的 `pread` 通道, 不依赖 `/proc/self/fd` 路径重新打开. 管道、不可 seek 或可写输入、Android 7, 以及必须使用本地 `File` 的兼容后端才复制到有空间边界和生命周期清理的私有缓存; 当前加密 ZIP 在 Zip4j 请求本地文件时才延迟物化, 普通 ZIP 保持直读.

代价与未完成项:

- 普通单卷 ZIP、安全普通 7Z，以及符合条件的 TAR、TAR.GZ/TGZ、TAR.XZ/TXZ、TAR.BZ2/TBZ2、TAR.ZST/TZST 已开放添加文件、导入完整目录树、新建空目录、删除、重命名及最近一次替换的一次恢复; v19 允许从宿主原生档案列表直接删除和重命名, v20 进一步允许在当前原生档案目录中新建空文件夹. 辅助元数据完全无损保留、多版本历史、原生列表内添加文件或目录树、分卷 ZIP、JAR/AAR/WAR、RAR，以及加密/solid/分卷 7Z 修改仍未开放;
- 宿主文件管理器深度重构完成后, v18 按重构后的替换事务边界落地, v19 再在既有档案页操作槽位加入能力驱动的删除与重命名; 两轮均没有改变普通文件管理器的整体布局或既有视觉结构;
- TAR.ZST 为 APK 引入四个 Android ABI, 不支持这些 ABI 的设备不会发现该插件动作;
- 宿主动作初筛不在建立菜单时读取魔数; v17 仅在用户明确选择“作为压缩档案打开...”后, 通过既有只读会话执行一次结构识别;
- 编号 `.zip.001` 与 `.7z.001` 卷组的只读浏览、预览和解压已经通过 Explorer Action v14/v12 落地; 这些编号卷组的创建或修改、7Z 头部加密创建以及尚未开放格式的档案内容修改仍需要对应后端重建能力, 但普通单目标整包替换可复用既有 v8 事务合同;
- 其他格式的最终依赖选型需在取得候选项目一手资料与本地测量数据后继续更新本 ADR.

## 验证

- 单元测试验证 ZIP 能力表声明可选密码、可选分卷和普通单卷添加/删除/重命名, 7Z 能力表声明动态添加/删除/重命名但不误报文件名加密或分卷; JAR/AAR/WAR、分卷 ZIP、RAR、加密/solid/分卷 7Z 与特殊条目 TAR 不获得实际修改能力;
- 单元测试验证引擎把 ZIP、7Z 与五种 TAR 容器全部注册为当前可写 provider, provider 操作集合与格式能力完全一致, `.7z`、`.tgz`/`.tar.gz`、`.txz`/`.tar.xz`、`.tbz2`/`.tar.bz2` 与 `.tzst`/`.tar.zst` 都进入管理目录而 JAR/AAR/WAR 与 RAR 不进入; 分卷、危险路径、solid、加密、超资源预算与特殊条目快照返回稳定只读原因, 准备计划绑定源版本并给出条目、目录、已知字节与未知大小数量;
- 单元测试直接以 7Z 能力和校验器调用共享 `ArchiveRewritePlanner`, 验证它不会读取 TAR 注册表或默认校验政策; TAR 适配器的既有路径、冲突、工作量和特殊条目边界保持逐项覆盖;
- 单元测试验证 ZIP 变更计划对文件添加、空目录、文件及目录子树重命名/删除生成稳定结果, 并在写出前拒绝危险路径、重复或等价名称、文件/目录碰撞和无法保留的条目;
- 宿主与插件自动化验证 v18 最近替换的可用/恢复/失效/需要恢复状态、一次性语义、外部改写拒绝覆盖、提交与恢复中断日志以及备份损坏清理; 真实进程终止和真实低存储两阶段门禁继续列在 Roadmap, 不以合成故障冒充设备结果;
- 单元测试验证改名为 `.bin` 的真实 ZIP 仍由结构探测识别;
- 单元测试验证带 `.zip` 后缀的普通文本不会被识别为 ZIP;
- 单元测试使用固定 SHA-256 的 Zip4j 2.11.5 两卷样本验证 `.z01` 首卷标记、`.zip` 末卷 EOCD、缺卷 `INDEX/MISSING_VOLUME` 映射、授权完整卷组的精确数据流、精确卷名和恶意大卷数下的有界摘要; Android 7 与 Android 15 运行时另外创建真实分卷并通过 v12 Binder 复核同一链路;
- 单元测试覆盖 ZIP 分卷预设、自定义 MiB 边界、完整卷组名称长度和统一编号; Android 仪器测试覆盖普通/AES 分卷往返、伴随卷冲突、询问策略、取消、提交中途失败及回滚失败. 真实 AutoJs6 宿主产物由独立 7-Zip 完整测试、解压并与源文件核对 SHA-256;
- 单元测试验证 reader 输出中立方法标识、条目能力和实际数据流;
- 单元测试使用 7-Zip 22.00 夹具验证 solid LZMA2、BCJ + LZMA2、AES 内容加密、AES 头部加密、无密码/错误密码/正确密码、签名和截断诊断; 普通非 solid BCJ + LZMA2 与 MT Manager v2.26.8 普通样本进入修改边界, solid、加密及超资源预算输入保持只读, 能力表不误报创建时文件名加密或分卷;
- 单元测试使用固定 SHA-256 的 WinRAR RAR4、RAR5、内容加密、头部加密与三卷 RAR5 样本验证结构探测、Unicode 目录、完整卷组精确内容流、缺少最终卷、卷变化、缺少/错误/正确密码和 `MISSING_VOLUME`/`SOURCE_CHANGED`; 能力表不误报 writer 或档案修改;
- 单元测试验证 TAR 的 Unicode、空档案、V7 头、校验和损坏、截断数据、预览流和解压闭环; 指向父级或绝对路径的符号/硬链接保持不可打开和不可解压, 4 GiB old GNU sparse 条目只公开真实大小与压缩占用元数据, 不作为普通文件读取;
- 单元测试验证 GZIP/XZ/BZIP2/Zstandard 外层签名、内部 TAR 二次验证、改名识别、空档案、连接流、截断与损坏尾部、Unicode 预览及格式中立解压;
- 单元测试验证父级穿越、绝对路径、Windows 驱动器前缀、双向文本控制符和非法输出根不会创建输出; 指向档案外的 TAR 符号/硬链接不可提取且不会到达输出 writer;
- 现有扫描、预览、解压及创建回归测试继续通过;
- 单元测试覆盖六个资源预算维度、兼容/严格档位顺序、未知压缩大小、超预算浏览、写出前拒绝及确认后仅扩展至声明值;
- 单元测试验证解压进度优先使用字节比例、未知大小时回退到条目比例, 并以单调时钟计算平均速度和剩余时间; 时钟回退、零字节档案和清理阶段不会产生负值或伪造 ETA;
- 单元测试验证普通取消会删除本次新建根, 清理失败会携带准确的残留输出位置和 `CLEANUP/OUTPUT_FAILURE` 诊断; 即使取消异常传播丢失 suppressed 异常, `CLEANUP_FAILED` 事件仍能把残留根交给 UI;
- 单元测试验证管理页解压范围在档案根、可提取内部目录、只读隔离目录和勾选状态下只公布真实可用选项, 并把勾选集合有序去重复制, 不保留可变 UI 集合的引用;
- 单元测试验证输出名称冲突的询问/跳过/覆盖/自动重命名策略、兼容冲突的“应用到全部”、不兼容覆盖的安全重命名回退、扩展名前编号、结果计数和跳过后的真实进度总量;
- 单元测试以固定生成的 8 MiB 高压缩比 ZIP、20,001 条目 ZIP、512/1,025 层路径和畸形非关键 extra field 验证元数据浏览、资源预算、硬结构上限及兼容读取行为;
- Android 仪器测试覆盖当前 reader 的可 seek 描述符直读、源名称移除后的租约可用性、管道与可写描述符回退、报告大小变化、失败清理、活跃缓存保护、过期目录回收, 以及加密 ZIP 在后端提出本地文件要求后延迟物化并完成 AES 解密; 未知大小管道超过缓存复制预算时还必须关闭两端并移除部分缓存;
- Android 仪器测试通过统一 reader、writer 与宿主会话验证 7Z 及全部 TAR 族格式的设备运行链路, 包含 Unicode 路径、空目录、未知源大小、取消和源大小变化后的事务回滚.
- Explorer Action v19 在 G8441 Android 9 验证删除最后一个 ZIP 条目后的完整空档案闭环: 插件当前会话与生产扫描器均返回零条目, 宿主跨进程 provider 刷新为空, 关闭后重新绑定已安装的签名 Release 插件仍可按 ZIP 打开. 实际提交物是 22-byte EOCD-only ZIP, SHA-256 为 `8739C76E681F900923B900C9DF0EF75CF421D39CABB54650C4B9AD19B6A76D85`, 7-Zip 22.00 完整性测试通过; Android `java.util.zip.ZipFile` 的平台差异不作为空档案有效性门禁.
- Android 仪器测试在 API 24、API 25、G8441 Android 9 与 Android 15 小米平板验证 RAR4 描述符读取、加密 RAR5 缺少/错误/正确密码恢复和会话能力; API 24 的 v11 完整插件套件 125 项全部通过. Explorer Action v12 又以 184 项插件 JVM 测试、四端完整插件仪器套件、23 项宿主策略 JVM 测试及 API 24/Android 15 各 4 项宿主分卷源设备测试验证宿主授权分卷目录、完整两卷 ZIP 的原生列表与条目流, 并以固定三卷 WinRAR RAR5 样本验证完整读取、缺少最终卷及伴随卷变化. G8441 与 Android 15 平板还通过最终 2.5.0 Release 的真实 AutoJs6 原生路径栏分别浏览 `.z01 + .zip` 与 `part01/part02.rar`, 会话日志无崩溃或档案错误. Explorer Action v13 再以 186 项插件 JVM 测试、26 项宿主专项 JVM 测试和四端各 133 项实际通过的插件仪器测试验证同会话文件名编码原子重建与稳定条目 ID; G8441 和 Android 15 的最终 2.6.0 Release 均完成 GB18030/IBM437/自动检测切换并保持深层路径, 两个外部 Mac ZIP 样本还验证局部/中央头差异、未置 UTF-8 标志的 emoji、多语言及 NFD 路径在生产 reader 和宿主原生页面中保持可浏览. 详细数量、制品哈希和清理记录以 Roadmap 的发布门禁为准.
- Explorer Action v14 以完整文件名大小写不敏感匹配有界复合后缀, 仅 `.zip.001` 与 `.7z.001` 首卷进入编号卷源, 普通 `.001` 保持普通文件. 插件以 7-Zip 22.00 固定三卷 ZIP/7Z 样本覆盖连续卷拼接、缺卷名称、卷身份变化、Android 7 ZIP 本地兼容暂存、原生列表、条目预览和解压; API 24 与 Android 15 的宿主设备测试分别验证 5 项分卷源策略. G8441 与 Android 15 的最终 2.7.0 Release 还通过真实宿主路径栏浏览并解压两类编号卷组, 输出名称去除 `.001` 及档案扩展名, 内容 SHA-256 与源条目一致. 详细测试数量、制品哈希和清理记录以 Roadmap 的发布门禁为准.
- Android 仪器测试使用一个按 NFC 与大小写折叠名称、并在冲突时故意返回旧文档 ID 的 Debug DocumentsProvider, 验证等价根名称安全编号、未提供冲突决定时封闭失败、本次输出根回滚和既有内容逐字节不变; 同一 provider 还能拒绝删除, 用于验证取消回滚成功路径及清理拒绝时残留根名称/URI 的精确报告; 该 provider 不进入 Release 清单.
- Android 仪器测试验证动作目录发布独立的 v8 `manage-archive` Activity 入口, Intent 不会误触发 `extract-to` 快捷路径; 真实 ZIP 管理页在根目录、内部目录和勾选后分别显示全部、当前目录与当前勾选范围, 修改操作另通过受控目标替换会话提交.
- Android 仪器测试在名称按 NFC 与大小写折叠的真实 DocumentsProvider 上验证四种冲突策略、逐项询问的“应用到全部”、类型不一致时禁用覆盖、策略选择跨 Activity 重建恢复, 以及既有输出根和哨兵内容始终保持不变.
- 外部 7-Zip 22.00 AES-256/ZipCrypto 样本验证无密码浏览、正确密码读取和错误密码的 `PASSWORD/WRONG_PASSWORD` 诊断; AES-256 writer 产物由统一 reader 重新打开并校验.
- 外部 7-Zip 22.00 生成的 TAR/TAR.GZ/TAR.XZ/TAR.BZ2 及 bsdtar 3.8.4/libzstd 1.5.7 生成的 TAR.ZST 样本以固定 SHA-256 验证 Unicode 目录、条目元数据和内容流.
- G8441 上的 TAR.GZ 端到端修改产物由 7-Zip 22.00 同时验证 GZIP 与内层 TAR, 并由 bsdtar 3.8.4/libarchive 3.8.4 完整列出和提取; 取消、损坏尾部、源变化及 `/dev/full` 的真实 `ENOSPC` 均验证原档案保持不变且待提交输出被中止.
- G8441 上的 TAR.XZ 端到端修改产物由 7-Zip 22.00 验证 LZMA2 preset 4、CRC64 与内层 TAR, 并由 bsdtar 3.8.4/libarchive 3.8.4 完整列出和提取; 同一五次连续修改、取消、损坏尾部、源变化及 `/dev/full` 的真实 `ENOSPC` 均验证原档案保持不变且待提交输出被中止.
- G8441 上的 TAR.BZ2 端到端修改产物由 7-Zip 22.00 验证 `BZh6` 包装层与 6,144 bytes 内层 TAR, 并由 bsdtar 3.8.4/libarchive 3.8.4 直接完整列出和提取; 同一五次连续修改、取消、损坏尾部、源变化及 `/dev/full` 的真实 `ENOSPC` 均验证原档案保持不变且待提交输出被中止. 最终 250 bytes 产物 SHA-256 为 `488B2717BFEB790437C60539FE3B335E2448A61A14A7F4FB775BFF4D468800CF`, 3 个文件/4 个目录与 17 bytes 内容在两种外部工具间一致.
- G8441 上的 TAR.ZST 端到端修改产物由官方 Zstandard CLI 1.5.7 验证单帧、1 MiB 窗口与 XXH64 checksum, 由 bsdtar 3.8.4/libarchive 3.8.4 直接完整列出和提取, 并由 7-Zip 22.00 验证解压后的 6,144 bytes 内层 TAR; 同一五次连续修改、取消、checksum 损坏、源变化及 `/dev/full` 的真实 `ENOSPC` 均验证原档案保持不变且待提交输出被中止. 最终 234 bytes 产物 SHA-256 为 `8D399043AE807DCAAAA8A1F95193E901C892CECA20411CF85365891F166899E4`, 内层 TAR SHA-256 为 `31D2044CD97944484C9986127C27C6F41F02AC21D30E6CA24F9B3C4D8B6618E0`, 3 个文件/4 个目录与 17 bytes 内容一致; 7-Zip 22.00 不识别外层 Zstandard 容器, 因而不把它作为该包装层的直接门禁.
- G8441 上的 7Z 端到端修改连续完成目录子树重命名、删除、新建空目录、未知大小文件添加和完整目录树导入; 另覆盖取消时停止压缩源读取、源身份变化、损坏待提交头部及 `/dev/full` 的真实 `ENOSPC`, 所有失败路径均保留原档案. 最终 386 bytes 非 solid LZMA2 产物由 7-Zip 22.00 完整测试、列出及提取, 并由 bsdtar 3.8.4/libarchive 3.8.4 直接列出和提取; 3 个文件/4 个目录与 17 bytes 内容一致.
- 插件在 `emulator-5554` 通过真实 AutoJs6 压缩入口创建 TAR.ZST 与普通 TAR; bsdtar 3.8.4/libarchive 3.8.4 完整提取 TAR.ZST, 7-Zip 22.00 与 bsdtar 均验证普通 TAR, 创建产物还能由宿主原生档案页面重新打开并预览哈希一致的 Unicode 条目.
- 插件在 `emulator-5554` 通过真实 AutoJs6 压缩入口创建普通与 AES-256 内容加密 7Z; 7-Zip 22.00 验证非 solid/LZMA2、正确与错误密码行为、完整提取、空目录和 Unicode 内容 SHA-256, 宿主原生档案页面重新打开普通产物并通过路径栏与预览器读取同一 Unicode 条目.
