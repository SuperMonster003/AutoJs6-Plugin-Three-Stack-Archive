# AutoJs6 文件管理器集成说明

本文档同时记录两类内容:

- 已经落地并通过验证的 Explorer Action v4 受控文件会话、v5 只读档案列表、v6 条目读取会话、v7 待提交输出校验、v8 原目标替换、v9 目录输出事务、v10 宿主原生解压、v11 原生密码恢复、v12 有界同级分卷输入、v13 原生文件名编码重建、v14 有界复合文件名后缀匹配、v15 可恢复多输出批次、v16 经验证的源项目回收站交接、v17 显式档案复核、v18 最近替换恢复、v19 宿主原生删除/重命名与 v20 宿主原生创建/添加;
- 当前统一档案管理 Activity 的完整写入边界, 以及 v20 宿主原生档案页创建空文件夹、添加文件或完整目录树、删除和重命名的最小授权合同.

完成状态以 [`ROADMAP.md`](../ROADMAP.md) 为准. 文中标记为“当前”的能力可以在现有宿主与插件中使用; 标记为“后续”的内容仍不可当作已发布功能.

## 当前集成结果

AutoJs6 文件管理器现在可以从插件目录动态发现以下动作:

| 动作 ID | 目标 | 位置 | 访问范围 | 呈现方 |
| --- | --- | --- | --- | --- |
| `open-archive` | 单个受支持的档案文件 | 文件菜单/主动作 | 只读目标 | 宿主 Explorer |
| `open-as-archive` | 名称与 MIME 未命中 `open-archive` 的单个文件 | 文件菜单 | 只读目标 | 宿主 Explorer |
| `manage-archive` | 单个 `.zip`、`.tar`、`.tar.gz`、`.tgz`、`.tar.xz`、`.txz`、`.tar.bz2`、`.tbz2`、`.tar.zst` 或 `.tzst` 候选; 插件结构探测后仅开放普通单卷 ZIP 或符合条件的 TAR/TAR.GZ/TAR.XZ/TAR.BZ2/TAR.ZST 修改 | 文件菜单 | 受控替换原目标 | 插件 Activity |
| `extract-to` | 单个受支持的档案文件 | 文件菜单 | 读取目标并在父目录创建受控目录输出 | 插件 Activity |
| `compress` | 单个普通文件或目录 | 文件/目录菜单 | 读取目标并在父目录创建输出 | 插件 Activity |
| `compress-selection` | 同一真实父目录中的多个文件、目录或混合目标 | 多选操作栏 | 读取目标并在共同父目录创建输出 | 插件 Activity |

动作是否出现由插件目录、目标类型、基数、完整文件名后缀、扩展名/MIME、入口位置、插件启用状态和信任状态共同决定. v17 的 `open-as-archive` 仅在其引用的主动作没有静态匹配单个普通文件时出现; 宿主建立菜单时不会读取文件. 未安装或禁用插件时不会留下占位菜单; 虚拟目标或跨父目录多选也不会显示一个无法执行的压缩入口.

多选操作栏使用五个等宽单元格, 图标在上、文字在下. 当前尺寸为 22 dp 图标与 11 sp 文字, 已在 320/360/600 dp、大字体、横屏、RTL 与真实窄屏设备上完成自动化和点击验证.

## Explorer Action v4/v5/v6/v7/v8/v9/v10/v11/v12/v13/v14/v15/v16/v17/v18/v19/v20 目录

v4 在保留 v1-v3 解析能力的同时加入多目标与受控输出. v5 再加入呈现方式和只读档案列表, v6 为会话增加受控条目读取, v7 为待提交输出增加只读重开校验, v8 增加经校验的原目标替换, v9 增加受控目录树输出与恢复, v10 为宿主原生档案页增加按条目解压、进度和取消, v11 加入首次打开与解压过程中的有界密码恢复, v12 再加入由宿主持有的有界同级分卷目录与逐卷只读描述符, v13 在既有只读会话内加入有界文件名编码重建, v14 为动作目录加入有界复合文件名后缀匹配, v15 为同一文件会话加入可恢复多输出批次, v16 再加入由完整输出证明约束的源项目回收站交接, v17 为静态未匹配的单文件加入显式只读复核与实际格式元数据, v18 为受控目标替换加入宿主持有的最近版本与一次恢复, v19 为宿主原生档案页加入能力驱动的删除和重命名, v20 再加入空目录创建与有界冻结输入添加:

| 字段 | 语义 |
| --- | --- |
| `protocolVersion` | 当前为 `20` |
| `targetKind` | `FILE`、`DIRECTORY` 或 `MIXED` |
| `cardinality` | `SINGLE` 或 `MULTIPLE` |
| `accessMode` | `READ_ONLY`、`CREATE_IN_PARENT` 或 `REPLACE_TARGET` |
| `placements` | 主动作、单项溢出菜单或多选操作栏 |
| `presentation` | 插件 Activity 或宿主 Explorer |
| `extensions`/`mimeTypes` | 动作初筛条件; 不是最终格式判定 |
| `fileNameSuffixes` | v14 复合文件名初筛; 最多 128 项且每项不超过 128 字符 |
| `probeForActionId` | v17 可选; 指向同目录中一个宿主原生只读主动作 |
| `priority` | 多个插件动作同时匹配时的稳定排序依据 |

当前压缩动作使用通配 MIME/扩展名和 `FILE`/`DIRECTORY`/`MIXED` 目标, 因此能覆盖普通文件、目录和同父级混合多选. 打开与整包快捷解压动作匹配 ZIP/JAR/AAR/WAR、7Z、RAR、TAR 及 `tgz`/`txz`/`tbz2`/`tzst` 别名; v14 完整文件名字段精确匹配 `.zip.001`、`.7z.001`、`.tar.gz`、`.tar.xz`、`.tar.bz2` 与 `.tar.zst`. 通用 `gz`/`xz`/`bz2`/`zst` 叶扩展名不再发布, 避免把普通压缩流当作 TAR. 名称未命中时可由用户明确选择 `open-as-archive`, 再执行一次完整结构探测. `manage-archive` 目录按已注册可写 provider 的 `.zip`、`.7z`、`.tar`、`.tgz`、`.txz`、`.tbz2`、`.tzst` 叶扩展名与 `.tar.gz`、`.tar.xz`、`.tar.bz2`、`.tar.zst` 复合后缀初筛, 不匹配编号首卷、JAR/AAR/WAR 或 RAR; 分卷 ZIP 末卷、加密/solid/分卷 7Z 与特殊条目 TAR 仍可能命中名称, 因而必须在插件结构探测后保持不可修改.

每个目录字段都有数量或长度上限. 宿主会拒绝非法目标类型、基数、位置、授权和呈现方式组合, 而不是静默扩大插件权限. `HOST_EXPLORER` 在 v5-v16 只允许 `FILE + SINGLE + READ_ONLY + PRIMARY`; v17 唯一增加的组合是无匹配器、引用上述主动作的 `FILE + SINGLE + READ_ONLY + OVERFLOW` 复核动作. 它不能获取目录写入、多选、输出、替换或 Activity 能力, 每个主动作也只能被一个有效复核动作引用. v8 的 `REPLACE_TARGET` 只接受 `FILE + SINGLE + OVERFLOW + ACTIVITY`, 且不能同时声明独立输出. v9 目录输出复用 `CREATE_IN_PARENT` 授权; `manage-archive` 的替换授权不会隐式扩大为父目录创建权限. v10 解压请求只传递当前会话内的不透明条目 ID; 实际写入只能通过宿主单独建立的 v9 输出树进行. v11 密码只存在于一次同步 Binder 请求中, 不增加文件或目录授权. v12 同级卷源只允许宿主按所选名称推导出的 ZIP/RAR 分卷候选, 插件只看到不透明 ID、受控显示名和身份元数据, 不获得父目录 URI、路径或任意名称查询能力. v13 文件名编码请求只接受插件已公布的最多 32 个名称之一或自动模式, 不创建新的文件、目录或 URI 授权. v14 的 `fileNameSuffixes` 只接受以非句点字符开头的有界复合后缀; 匹配完整文件名且不把通用叶扩展名 `001` 当作授权. v13 及更早目录不能发布该字段. v15 只组合会话已经预留并验证的新文件输出, 不新增目标、路径、覆盖或删除授权. v16 只接受会话原始完整目标 ID 列表与本次全部已提交输出事务 ID, 不接受路径、附加目标或目标子集. v17 复用现有 `openArchiveV11` 只读会话, 不新增授权类型. v18 只让协议版本不低于 18 的 `REPLACE_TARGET`/`MANAGE_TARGET` 动作查询和恢复其原始单文件目标, 不扩大路径、目录或任意覆盖能力; v8-v17 会话仍按旧行为执行且不创建替换备份. v19 的档案修改任务也不改变动作目录授权: 宿主只在插件同时公布匹配目标的 v19 管理能力时建立一次独立的原目标替换会话, 并只把该会话 Binder 交给同一插件的既有档案会话.

## v4 执行请求

宿主通过显式 Activity Intent 启动解压与压缩动作. v4 及后续请求包含:

```text
ExplorerActionRequestV4
├─ protocolVersion = 4
├─ requestId                    # UUID
├─ actionId
├─ sourceSurface
├─ parentUri                   # 上下文, 不自动授予目录遍历权限
├─ parentDisplayPath
├─ targets[]                   # 有序, 最多 128 项
│  ├─ id                       # 稳定目标 ID
│  ├─ uri
│  ├─ displayName
│  ├─ kind
│  ├─ mimeType
│  ├─ size                     # -1 表示未知
│  └─ lastModified
└─ hostSession.binder
```

`ClipData` 只包含与 `targets[]` 顺序完全相同的目标 URI. 插件必须逐项核对 Bundle 与 `ClipData`; 不能根据 content URI 的字符串片段推断本地路径或父子关系. 共同父目录由宿主在构造请求前以规范路径验证, `parentUri` 仅用于上下文, 不作为任意目录读取或写入授权.

当前插件还会验证请求 UUID、动作 ID、目标数量、显示名/类型/大小、URI 唯一性、宿主版本和 Binder 接口描述符. 任一必需字段不一致时会拒绝整个请求.

## 受控宿主文件会话

v4 不把本地绝对路径交给插件. 对目录递归与输出写入, 宿主附加一个请求级 `IExplorerActionHostSession`:

```text
listChildren(targetId, relativePath, offset, limit)
    -> items[] + nextOffset + complete

openFile(targetId, relativePath)
    -> read-only ParcelFileDescriptor

prepareOutput(displayName, mimeType, conflictPolicy)
    -> transactionId + resolved display name/path

prepareTargetReplacement(targetId)
    -> transactionId + original display name/path/size/lastModified

prepareOutputTree(displayName, conflictPolicy)
    -> transactionId + resolved display name/path

createOutputDirectory(transactionId, relativePath)

openOutputFile(transactionId, relativePath)
    -> write-only ParcelFileDescriptor

openOutput(transactionId)
    -> write-only ParcelFileDescriptor

openPendingOutput(transactionId)
    -> read-only seekable ParcelFileDescriptor

queryOutput(transactionId)
listOutputs()
attachClient(clientToken)

commitOutput(transactionId)
    -> committed name/path/size/lastModified

abortOutput(transactionId)
close()
```

目录页大小最大为 128. 相对路径、目标 ID、显示名、MIME 和输出名称均有协议上限. ZIP、7Z 与 TAR 系列创建器通过共享的源遍历器分页深度遍历目标, 只在需要时打开单个输入描述符, 拒绝越界路径与符号链接, 并持续响应取消.

会话的权限边界如下:

- 每次 Binder 调用都固定到收到请求的插件 UID;
- 目标以不可猜测用途的稳定 ID 索引, 插件不能提交任意绝对路径;
- 相对路径不允许空段、`.`、`..`、NUL、绝对路径或越过目标根目录;
- 规范路径不一致的符号链接会标记为不可读, 打开时再次拒绝;
- 输出名称只能是安全的单个叶名称, 不能包含分隔符或控制字符;
- 输出只能创建在请求的共同父目录, 不授予其他目录写权限;
- 同名目标可选择失败或自动编号, 并由宿主进程全局预留以避免并发覆盖;
- v9 目录输出的每个相对路径都必须经过相同的安全路径校验, 且只能创建新的节点; 插件无法读取、复用或覆盖既有兄弟目录内容;
- v8 替换只能预留原请求中的一个目标, 暂存文件位于同一目录, 不允许插件选择兄弟文件或任意路径;
- 替换在预留和提交前都核对设备、inode、大小与修改时间, 防止外部修改在检查与提交之间被覆盖.

创建表单默认使用自动编号. 用户也可以选择每次询问: 插件先把精确名称与 `OUTPUT_CONFLICT_FAIL` 交给宿主; 若当前宿主以 `IllegalArgumentException` 拒绝这次预留, 插件会在尚未遍历源目录、打开源文件或打开输出描述符时返回表单, 让用户编辑名称、取消, 或明确改用 `OUTPUT_CONFLICT_AUTO_RENAME` 重试. 插件只把精确预留失败表述为“宿主无法预留该名称”, 不解析宿主私有异常文本, 也不会把它伪装成已经确认存在同名文件. 其他异常仍按普通创建失败处理.

Explorer Action v7 的创建输出仍只有 `OUTPUT_CONFLICT_FAIL` 与 `OUTPUT_CONFLICT_AUTO_RENAME`, 不提供任意名称覆盖. v8 另行提供严格限定的原目标替换, 并采用以下合同:

- 插件以请求中的稳定 `targetId` 调用 `prepareTargetReplacement`, 不能传入名称或路径选择另一个文件;
- 宿主预留时记录原目标身份, 并在提交前再次核对;
- 插件始终写入宿主在同目录创建的隐藏暂存文件, 关闭 writer 后必须通过 `openPendingOutput` 完整校验;
- 未执行读回校验的替换事务不能提交;
- 校验通过后宿主同步暂存数据并使用同目录原子改名替换目标;
- 中止、取消或会话关闭只清理本次暂存状态, 不修改原档案;
- 成功提交后同一会话更新目标身份, 因而管理页可以继续执行下一次受控修改.

## v5/v6/v11/v19/v20 档案会话

`open-archive` 不再启动插件文件列表 Activity. 宿主重新验证动作匹配、插件信任与启用状态后, 以只读方式打开规范化的源文件, 再通过专用服务绑定调用:

```text
IExplorerActionPlugin.openArchive(sourcePfd, request)       # v5-v10, 签名保持不变
    -> IExplorerArchiveSession

IExplorerActionPlugin.openArchiveV11(sourcePfd, request)    # v11
    -> Bundle { sessionBinder | errorCode + errorMessage }

request
├─ displayName
├─ size                         # -1 表示未知
├─ lastModified
└─ password                     # v11 可选瞬态 CharArray, 最长 1,024 字符

session.getInfo()
    -> sessionId + rootId + displayName + sourceSize + sourceLastModified
       + canOpenEntries + canDeleteEntries + canRenameEntries
       + canCreateDirectory + canAddEntries

session.listChildren(parentId, offset, limit)
    -> items[] + nextOffset + complete

item
├─ id                           # 会话内不透明 ID
├─ parentId
├─ name
├─ kind                         # 文件或目录
├─ size / compressedSize
├─ lastModified
├─ canOpen
├─ canExtract
├─ canDelete
├─ canRename
├─ canCreateChildren
└─ canAddChildren

session.openEntry(entryId)      # v6, 仅普通可读取条目
    -> read-only ParcelFileDescriptor

session.close()

session.mutateEntries(request, replacementSession, callback)  # v19
session.cancelMutation(operationId)                            # v19
session.addEntries(request, replacementSession, inputSession, callback)  # v20
```

v11 成功结果同时包含 `sessionBinder` 和错误码 `0`; 失败结果不包含会话 Binder, 并使用 `11000` 不可恢复、`11001` 需要密码或 `11002` 密码错误. 宿主只对后两种结果显示原生密码框并重试; 其他错误继续进入既有安全诊断页面. 旧 `openArchive` 的签名、AIDL 位置和事务号没有改变, v5-v10 宿主与插件仍按原合同互操作.

宿主把密码放入 `ExplorerArchiveRequestKeys.PASSWORD`, 仅用于一次同步 `openArchiveV11` 调用. 对话框输入在提交或关闭后立即清空; 请求返回后, 宿主与插件都会清零并从 Bundle 移除各自仍持有的 `CharArray`. 密码不进入页面状态、配置、日志、异常或诊断. Android、Binder 与 Java 库仍可能产生应用无法全部控制的短生命周期副本, 因此这是尽力清理合同, 不是绝对不存在运行时副本的承诺. 首次打开重试仍绑定触发操作的同一 Explorer 目标和规范路径, 并重新执行动作匹配、插件授权、文件可读性与报告大小检查. 解压重试则在既有会话内重新扫描并要求源长度、修改时间、格式、结构限制和全部稳定条目元数据与原快照一致; 不一致时终止恢复.

当前 ZIP、7Z、RAR 与 TAR 族实现优先租用宿主传入的只读可 seek 描述符, 不再为普通档案复制整份内容. 输入只有同时通过进程自身 `fdinfo` 的只读模式位、`fstat` 普通文件类型、`lseek` 随机定位、`pread` 首尾读取、实际大小与宿主报告大小核对后才进入直读路径. 每个统一 reader 都在同一租约上建立拥有独立逻辑位置的只读通道, 实际读取通过 `pread` 完成, 不共享或修改描述符的文件偏移, 不通过 `/proc/self/fd` 路径重新打开档案, 也不把描述符编号跨 Binder 返回. 描述符租约持续到页面关闭、解绑或会话失败. 管道、可写描述符、不可 seek 代理、Android 7, 以及必须获得进程可读本地文件的后端会回退到插件私有缓存; 当前加密 ZIP 由只接受 `File` 的 Zip4j 读取, 因而在后端确认加密后才按需物化, 普通 ZIP 仍保持直读. 7Z 支持普通/solid、常见压缩与过滤器链以及 AES 内容/头部加密读取; RAR 支持单卷 RAR4/RAR5 及内容/头部加密读取并保持只读; TAR 族包括 TAR、TAR.GZ/TGZ、TAR.XZ/TXZ、TAR.BZ2/TBZ2 与 TAR.ZST/TZST. 页大小最大为 128; 宿主会持续取页直到 `complete`, 同时验证条目数、ID、父子关系、名称、类型、大小和分页游标. 插件不会把源描述符、缓存路径或真实档案内部路径暴露给宿主, 条目使用会话内不透明 ID.

回退缓存仍以“实际可用空间减 128 MiB 保留空间”为复制边界, 已知大小会在创建临时目录前预检, 未知大小会在复制过程中持续检查并使用溢出安全累加. 完成后再次核对宿主报告大小; 取消、读取失败、大小变化或空间不足会删除本次部分文件与目录. 活跃缓存目录登记为进程内租约, 不会被并发会话的过期清理误删; 未持有租约且超过 7 天的 `archive-input-*` 目录会在下次打开档案时回收.

目录索引只把严格通过路径策略的名称映射为正常虚拟路径. 父级穿越、绝对路径、驱动器前缀、控制字符、双向文本控制符或空名称等 `INVALID_PATH` 条目会被平铺到一个独立的只读隔离目录: 会话 ID 由安全序号路径生成, 与原始名称无关; 宿主只收到经可见转义的显示名. 后端数据可读时 `openEntry` 仍可通过不透明 ID 预览, 但条目能力始终拒绝写出. 隔离根会避开档案内的合法同名路径, 并作为普通目录层级进入宿主现有路径栏. 扫描器另有不可配置的进程结构上限 (250,000 个目录条目、500,000 个展开节点、65,536 字符路径与 1,024 层深度); 只有超过这些硬边界才拒绝建立索引, 较低的解压资源预算不会阻止浏览.

v6 的 `openEntry` 追加在 v5 AIDL 方法之后, 因此 v5 的 `getInfo`、`listChildren` 和 `close` 事务编号保持不变. `canOpenEntries` 缺失或为 `false` 时, 宿主继续提供 v5 浏览但不显示预览入口, 也不会调用新方法. v19 的修改方法继续追加在 v13 编码重建方法之后, v20 的 `addEntries` 再追加在 v19 方法之后; 旧插件缺少对应会话级或逐条目能力位时, 宿主按 `false` 处理并保持原有只读行为.

打开条目时, 插件根据不透明 ID 找回扫描快照中的普通文件, 重新打开对应后端并核对源文件身份、目录元数据、条目类型与声明大小; ZIP 继续核对压缩方法、加密状态和 CRC, 7Z 继续核对方法链、加密状态、声明大小和 CRC, RAR 继续核对卷状态、加密状态、声明大小和可用 CRC, TAR 继续核对可见头部校验和及条目元数据. 数据经可靠只读管道发送; 关闭会话会中止排队或进行中的管道. 宿主仅把支持现有主动作的文档、图片或媒体条目复制到自己的私有会话缓存, 并在发布缓存文件前核对准确字节数和可用空间. 文档沿用 8 MiB 限制, 图片和媒体目前分别使用 256 MiB 与 512 MiB 预览预算; 这些限制只影响预览, 不影响目录浏览.

会话生命周期与权限边界如下:

- 插件在打开会话时固定宿主调用 UID, 后续每次 Binder 调用都必须来自同一 UID;
- 宿主为档案浏览持有独立服务绑定租约, 不与动作目录发现或 Activity 启动复用;
- Binder 死亡、页面销毁、离开档案或显式关闭都会关闭会话;
- 插件关闭会话或服务销毁时关闭直读描述符租约, 或删除对应的回退缓存; 关闭操作幂等;
- 宿主的预览副本使用会话 ID 和条目 ID 的散列目录, 不把不可信名称用作目录路径; 离开档案时删除本会话副本, 并在新会话启动时清理过期目录;
- 条目管道保持只读且不提供真实档案路径; v6 仍没有档案写入、内部选择或任意路径访问接口.

## 当前档案输出事务

插件当前可创建 ZIP、7Z、TAR、TAR.GZ、TAR.XZ、TAR.BZ2 与 TAR.ZST. 普通单文件输出使用同一工作流:

1. 宿主在共同父目录预留最终名称;
2. 宿主在同一目录创建隐藏的 `.autojs6-explorer-*.part` 暂存文件;
3. 插件先扫描并固化有界源清单, 再通过只写描述符生成所选格式, 保留目录、空目录和可用的修改时间, 并记录实际输入编码流的逐文件 SHA-256;
4. 成功关闭档案流后调用 `openPendingOutput`, 在最终名称尚未发布时完整读回格式、路径、类型、条目数、声明大小、实际数据、内容指纹与可用 CRC;
5. 全部校验通过并关闭只读描述符后调用 `commitOutput`;
6. 宿主确认既有目标仍不存在, 再执行同目录原子改名;
7. 取消或失败时调用 `abortOutput`; 关闭会话也会清理尚未提交的事务.

当前默认冲突策略为自动编号, 例如 `Archive.zip`、`Archive (1).zip`. 现有目标不会被覆盖. ZIP 密码已经开放, 非空密码会生成 AES-256 加密条目; 创建加密 ZIP 时必须输入一致的确认密码, ZIP 中央目录文件名仍然可见. TAR 系列当前不支持密码. 文件名加密尚无完整后端, 因此界面明确显示为禁用状态. “压缩完成后将源项目移入回收站”由 v16 提供, 默认关闭, 且只在全部输出校验并提交后执行.

ZIP 还可创建标准 PKZIP 风格的 `.z01 + .zip` 分卷. 表单提供 10/50/100/500/1024/4096 MiB 预设以及 1 至 4096 MiB 的整数自定义值. 只有输出超过所选大小时才产生 `.z01`、`.z02` 等编号卷; 较小的输出是合法的单个 `.zip`. Zip4j 的分卷输出需要可寻址本地文件, 因而插件先在自身私有缓存的平面工作区完整生成卷组并核对卷数、后缀、父目录和可用空间, 再用 Zip4j 对整组执行完整读回. 该工作区不会暴露给宿主; 每个卷复制到宿主待提交输出后都通过 v7 只读描述符逐字节比对, 全部通过后才删除私有工作区并开始发布最终文件.

分卷输出不是多个互不相关的自动编号事务. 插件先预留最终 `.zip` 以在访问源之前执行所选冲突策略, 完成暂存后再按实际卷组精确预留 `.z01`、`.z02` 等全部名称. 任一编号卷冲突时, 自动模式会中止当前预留并为整组统一使用下一个名称; 询问模式则返回具体冲突卷, 不覆盖既有文件. 全部卷写入待提交输出并逐字节验证后, 插件才把整组事务交给 v15 批次提交. 宿主只在整批发布与目录同步完成后刷新 Explorer, 因而插件不会在正常失败路径报告一个缺卷的成功结果.

v15 不把连续多个 `rename` 误称为文件系统原子操作. 宿主在发布前先持久记录父目录和每个暂存文件的设备号、inode、大小及修改时间; 提交失败、客户端断开或宿主重启时, 只删除身份仍与日志一致的已发布或待发布文件. 若路径被外部进程占用或改写, 宿主保留该文件并把批次标为 `RECOVERY_REQUIRED`, 不会为了宣称回滚而误删外部数据.

同父级多选可启用"单独压缩每个文件和文件夹". 插件先按选择顺序固化一个单目标输出计划, 在表单中展示输出总数、最多 5 个派生名称和自动编号规则, 再通过同一宿主会话依次执行上述事务. 每个输出都以目标自身名称和所选格式的完整后缀命名; 既有名称或重复名称始终请求 `OUTPUT_CONFLICT_AUTO_RENAME`, 不会在前一项已经提交后再弹出逐项冲突询问.

插件现在让每个单独压缩任务完成写入与读回校验, 但延迟最终发布. 全部任务成功后, 两个及以上物理输出通过同一 v15 批次提交; 任一较早任务失败或取消时, 已验证的待提交输出也会一起中止. 只有一个物理输出时继续使用既有单项提交, 避免为单文件引入额外日志. v15 批次最多包含 128 个新文件, 不接受覆盖、目标替换或目录树事务.

仍需补齐的事务能力包括:

- 单项 v4-v8 文件事务在没有客户端租约时的长期遗留 `.part` 文件回收;
- 创建输出的类型化名称冲突结果; 最近一次目标替换历史已由 v18 落地, 但尚未扩展为多版本浏览;
- 无法仅靠同目录改名提供的严格文件系统级多文件原子提交; 当前 v15 提供持久日志、身份约束与可恢复回滚;
- 源项目回收站操作的会话级组撤销提示和组级历史展示; v16 已确保每个被移除源项目都有宿主持久恢复副本, 但不把多个逐项移动误称为跨文件系统原子操作.

### 操作结果与目录刷新边界

文件输出、目录树输出与目标替换都在宿主拥有的提交边界触发刷新, 不依赖插件 Activity 返回值. 文件与 v9 目录输出发布条目新增事件; v8 目标替换发布原条目变更事件; v15 批次则在全部成员发布、同步并写入完成状态后才逐项通知 Explorer. 插件不发送私有广播, 也不引用宿主内部类或 action 字符串.

不经过宿主文件会话提交的只读 Activity 动作仍没有通用类型化结果. 如果未来动作需要刷新其他目录或删除多个目标, 应在保持旧版本解析的前提下定义结构化操作结果, 由宿主验证调用方、会话和目录归属后执行最小范围刷新.

### Explorer Action v7 提交前重开验证

Explorer Action v6 的公共 AAR 只能取得只写 `openOutput`, 无法在最终名称发布前验证待提交文件. v7 在现有 AIDL 末尾追加以下方法, 保持旧方法编号与 v4 直接提交语义不变:

```text
openPendingOutput(transactionId)
    -> read-only seekable ParcelFileDescriptor
```

当前合同如下:

- 仅原始请求绑定的插件 UID 可以读取该事务;
- 调用前插件必须关闭写描述符; 调用后宿主拒绝再次打开写描述符;
- 临时文件在验证结束前继续隐藏且最终名称不发布;
- 插件关闭只读描述符并完成格式、路径、类型、条目数、声明大小、实际数据、内容 SHA-256 与可用 CRC 校验后才能调用 `commitOutput`;
- 读取、验证、取消或 Binder 调用失败均调用 `abortOutput`, 由宿主清理同目录临时文件;
- v4 客户端仍可按旧合同在写描述符关闭后直接提交; 当前插件要求 v7 并始终执行读回校验;
- 完整读回只解决输出发布前的数据完整性. v16 在所有输出已经提交后另行验证完整输出证明与源身份, 再提供独立的源项目回收站交接; v7 本身仍不授予删除能力.

### Explorer Action v15 可恢复多输出批次

v15 在既有文件会话 AIDL 末尾追加 `prepareOutputBatch`、`commitOutputBatch`、`abortOutputBatch`、`queryOutputBatch` 与 `listOutputBatches`. 旧方法和事务编号保持不变. 一个批次只接受同一会话内 2 至 128 个已经通过 `openPendingOutput` 进入校验态的 `CREATE` 文件事务:

```text
prepareOutputBatch(transactionIds)
    -> PREPARED + batchId
commitOutputBatch(batchId)
    -> COMMITTED + ordered outputs
queryOutputBatch(batchId)
listOutputBatches()
abortOutputBatch(batchId)
```

宿主在发布任何最终名称前使用私有 `AtomicFile` 日志持久保存父目录身份、成员顺序、目标/暂存规范路径和暂存文件身份. 提交过程中每发布一个成员都会推进持久计数, 完成目录同步和全成员身份复核后才写入 `COMMITTED`. 插件若丢失提交响应, 会查询同一批次: 已提交则恢复为成功, 其他非终态则请求整批中止, 不会盲目重试并生成重复档案.

批次成员不能再通过单项 `commitOutput` 或 `abortOutput` 操作. 回滚和启动恢复只删除规范父目录内、仍是普通非符号链接且身份与日志完全一致的文件; 外部变化转为 `RECOVERY_REQUIRED`. 终态日志有界保留并可查询, 正常成功只在整批完成后发布目录刷新. 该合同解决分卷 ZIP 与单独压缩的常规部分发布问题, 但 v15 本身不授予源删除、覆盖、任意路径或目录树批次能力; 源项目回收站授权由 v16 另行约束.

### Explorer Action v16 经验证的源项目回收站交接

v16 在 v15 文件会话方法之后追加 `moveTargetsToTrash(targetIds, outputTransactionIds)` 与 `queryTargetTrash()`. 该能力只服务于用户在压缩表单中明确开启的“压缩完成后将源项目移入回收站”, 并且调用必须晚于全部物理输出的完整读回校验和成功提交:

```text
moveTargetsToTrash(exactOriginalTargetIds, exactCommittedOutputTransactionIds)
    -> COMMITTED | RECOVERY_REQUIRED | FAILED | UNKNOWN
queryTargetTrash()
    -> same terminal result after Binder response loss
```

授权和执行边界如下:

- `targetIds` 必须与会话建立时的完整有序选择逐项一致, 不能只移除成功读取的子集, 也不能加入后来发现的路径;
- `outputTransactionIds` 必须与本次创建返回的全部物理输出事务按提交顺序完全一致, 每一项都必须是有效 UUID 且仍可证明为 `COMMITTED`;
- 宿主在操作前再次核对所有源根的规范位置、类型、设备号、inode、大小和修改时间, 任一变化都会在移除源数据前拒绝请求;
- 插件只提交不透明 ID, 不提交源路径、回收站路径或删除策略. 实际移动由宿主既有 `TrashRepository` 执行并在完成后刷新 Explorer;
- 对每个源项目, 宿主先把恢复副本写入回收站私有目录, 同步文件并完成原子发布, 再持久记录数据库条目, 最后才移除源. 目录只部分移除或后续项目失败时保留已经建立的恢复副本和条目, 返回 `RECOVERY_REQUIRED` 及可恢复项目 ID;
- 一次会话只产生一个幂等终态. Binder 响应丢失时插件调用 `queryTargetTrash`, 不重新发起移动, 从而避免重复条目或对已变化源再次操作;
- `COMMITTED` 表示所有源项目已进入宿主回收站, `RECOVERY_REQUIRED` 表示至少一项已有可恢复副本但整组未完全完成, `FAILED` 表示宿主确认没有进入不确定执行边界, `UNKNOWN` 表示客户端无法证明宿主终态. 插件分别提示这些结果, 不把不确定状态显示为成功.

v16 保证“移除任何源数据之前已经有宿主持久恢复副本”, 但当前并不提供多个源项目同时可见或同时消失的文件系统级原子性. 宿主进程若在恢复副本持久化后、源删除前终止, 可能暂时同时存在源项目与回收站副本; 这比没有恢复副本的数据丢失更安全. 当前回收站 UI 可逐项恢复, 尚未提供本次压缩操作的组级 Snackbar 撤销或会话级历史日志.

### Explorer Action v8 原目标替换

v8 在 v7 AIDL 末尾追加 `prepareTargetReplacement(targetId)`. 它只对目录中声明 `REPLACE_TARGET` 的单文件 Activity 动作可用, 并强制采用:

```text
prepareTargetReplacement
    -> openOutput
    -> close writer
    -> openPendingOutput
    -> full read-back verification
    -> commitOutput
```

宿主创建同目录隐藏暂存文件, 在提交前同步数据并复核原目标身份, 然后以同目录原子改名替换. 未读回校验、目标已由其他进程修改、暂存输出损坏或任何 Binder 调用失败都会拒绝提交; 插件随后中止事务, 原档案保持不变. 成功后宿主更新会话中的目标身份并发布条目变更事件, 支持同一管理页连续修改与自动刷新.

该合同只约束“替换哪个目标”和“何时允许提交”, 不解释待提交文件的档案格式. 因而普通单卷 ZIP、7Z、TAR 或其他整包重建都可以复用同一个 v8 流程; 新增一种可写格式本身不需要增加 AIDL 方法或提升宿主协议版本. v8-v17 不保留被替换旧版本; 只有下述 v18 及之后的相应会话由宿主在提交边界建立最近版本, 插件不能私自复制路径或伪造回滚.

### Explorer Action v18 最近替换恢复

v18 在当时已有的全部宿主文件会话方法之后追加 `queryTargetReplacement(targetId)` 与 `undoTargetReplacement(targetId, replacementHistoryId)`, 因而不改变 v1-v17 的 Binder 事务编号. 该能力只在协议版本不低于 18 且访问模式为 `REPLACE_TARGET` 或 `MANAGE_TARGET` 时启用. 提交流程扩展为:

```text
verified pending output
    -> sync previous target into host-private backup
    -> persist PREPARED journal
    -> atomic target rename
    -> persist AVAILABLE journal
    -> return opaque history ID and bounded metadata
```

备份位于宿主 `noBackupFilesDir` 的私有目录, 插件只得到最多 128 字符的不透明 UUID、状态、旧大小和建立时间, 从不得到备份文件名或路径. 宿主记录插件 UID、父目录设备号/inode、目标规范路径、替换前后文件身份与旧内容 SHA-256. 恢复时先把私有备份复制到目标同目录隐藏暂存文件并核对哈希, 持久记录 `RESTORING`, 再重新确认父目录和当前目标仍精确等于该历史提交的替换结果后原子改名. 成功后状态变为 `RESTORED`, 更新当前会话的目标身份并复用既有条目变更通知; 同一历史 ID 不可再次恢复.

查询状态为 `NONE`、`AVAILABLE`、`RESTORED`、`STALE`、`RECOVERY_REQUIRED` 或 `UNKNOWN`. 当前目标或父目录被外部替换时, 历史转为 `STALE` 并删除普通恢复副本, 绝不覆盖外部数据. 宿主若在替换改名后、`AVAILABLE` 落盘前终止, 下次会话会依据 `PREPARED` 日志和文件身份恢复为可用; 若在恢复改名前后终止, 则依据 `RESTORING` 日志恢复为 `AVAILABLE` 或 `RESTORED`. 无法唯一判定的证据转为 `RECOVERY_REQUIRED`, 不自动删除, 并阻止同一插件对同一目标再次建立替换历史.

普通终态与可用历史按 24 小时保留, 全局最多 32 条、备份总量最多 4 GiB, 每次复制前保留至少 128 MiB 可用空间. 超过配额时只淘汰可安全删除的最旧普通记录; `PREPARED`、`RESTORING` 与 `RECOVERY_REQUIRED` 不为满足配额而被自动丢弃. 若旧目标本身超过总量限制或空间不足, v18 修改在替换前失败, 原目标保持不变. 这些约束只作用于 v18 管理/替换动作, 不向旧协议会话追溯增加 I/O 或存储开销.

插件管理页在 ZIP 替换成功后以 Snackbar 提供一次“恢复上一版本”, 并在自身工具栏溢出菜单及档案信息中反映可用/已恢复/失效/需要恢复状态. 这些都是插件 Activity 独有内容; 宿主文件管理器列表、路径栏、选择栏及整体布局没有改动.

宿主仪器测试另设仅由测试构造器注入、生产默认无操作的两个内部中断点: 新目标发布后且 `AVAILABLE` 尚未落盘, 以及旧目标恢复后且 `RESTORED` 尚未落盘. G8441 Android 9 已在这两个窗口分别由外部进程执行真实 `force-stop`; 新宿主进程依次从 `PREPARED` 恢复为 `AVAILABLE`, 再从 `RESTORING` 收敛为 `RESTORED`, 原目标和恢复内容均逐字节正确. 日志自身的 `AtomicFile` 重写中断也由仪器测试模拟 `.bak` 恢复, 不会因只剩私有备份文件而丢失 `PREPARED` 证据; 记录更新时间单调不减, 设备壁钟回拨也不会阻断状态推进. 同一设备的真实低存储门禁把 `/data` 可用空间降至约 96 MiB, 1 MiB 原目标在发布前被 128 MiB 保留空间规则拒绝, 原文件不变且没有生成历史. 填充文件与测试状态均在测试结束时清理; 这些测试注入点不属于公开 API, 不改变插件权限或生产控制流.

### Explorer Action v19 宿主原生删除与重命名

v19 在 `IExplorerArchiveSession` 的绝对末尾追加 `mutateEntries(request, replacementSession, callback)` 与 `cancelMutation(operationId)`. 第一阶段只接受两种请求:

```text
Delete
├─ operationId                 # UUID
└─ entryIds[]                  # 有序去重, 1 至 128 个会话内不透明 ID

Rename
├─ operationId                 # UUID
├─ entryId                     # 一个会话内不透明 ID
└─ newName                     # 安全叶名称, 最长 4,096 字符
```

会话信息分别声明能否删除或重命名任意条目, 每个列表项再声明自身能力. 宿主只有在两层能力同时为真时才复用现有的重命名或删除入口; 单项可重命名, 单项和多选均可删除. 危险路径隔离项、缺卷项、RAR、分卷档案、加密或 solid 等只读变体不会显示无法完成的动作. 插件仍须在执行前按当前完整子树复核能力, 因而宿主缓存的旧能力不能绕过结构边界.

每次任务由宿主建立独立的 `IExplorerActionHostSession`, 其中唯一目标固定为当前档案, 允许 `prepareTargetReplacement`、提交前只读重开、v18 最近替换历史和一次恢复, 但不允许创建兄弟输出、枚举目录或选择其他路径. 插件使用与管理 Activity 相同的格式中立 mutation provider 完整重建档案, 回调依次报告准备、写入、校验、提交和重索引阶段. 只有宿主原子提交成功、重新打开目标并构造完整替换索引后, 才返回更新后的同一会话信息. 重命名后的条目及其子树保留原稳定 ID; 删除后仍存在的条目也保留 ID, 供宿主恢复最深可用路径、滚动/折叠状态和仍存在的选择.

宿主和插件都只允许一个解压或修改任务占用会话. 用户取消、插件 Binder 断开、源变化、输出失败、校验失败或畸形回调均关闭任务专属替换会话; 提交前失败不会改变原档案. 提交已完成而插件随后无法重建索引时, 物理档案仍是宿主已验证并原子发布的新版本, 当前页面会封闭失败并要求重新打开, 不会把旧缓存视作写入终态.

v19 没有传递新增文件、目录树、URI 或任意路径, 因而当时不提供宿主原生“添加文件”或“新建文件夹”. v20 已追加空文件夹创建和独立的冻结输入会话; 文件与完整目录树现在可以从宿主原生档案页添加, 同时仍不把选择器 URI、真实路径或未选同级项目交给插件. 管理 Activity 保留详细格式信息和附加设置, 不再是这些基本修改的唯一入口.

删除最后一个条目是普通的成功修改, 不是损坏或不支持状态. 插件必须提交后把当前索引刷新为空, 宿主必须呈现既有档案空状态; 关闭页面后, 同一物理档案仍须由新会话按原格式重新打开. Android 9 闭环已经覆盖插件会话、生产扫描器、宿主跨进程 provider、重新绑定已安装 Release 插件和实际 22-byte 空 ZIP. 后续 Android 7/API 24、Android 7.1/API 25 与 Android 15/API 35 联合矩阵又在每端通过真实跨进程闭环 2/2, 并分别导出实际提交产物; 三份 ZIP 的 EOCD 条目数与中央目录大小均为 0, 文件均为 22 bytes, SHA-256 均为 `8739C76E681F900923B900C9DF0EF75CF421D39CABB54650C4B9AD19B6A76D85`, 7-Zip 22.00 均通过完整性测试. Android 平台 `java.util.zip.ZipFile` 对最小空 ZIP 的版本差异不作为产品有效性门禁.

### Explorer Action v20 宿主原生创建与添加合同

v20 采用追加式兼容, 不改写 v1-v19 的 AIDL 方法、事务号、Bundle 字段或默认能力. v20 宿主仍按既有插件声明的 v1-v19 目录版本运行它们; 插件声明版本与目录版本继续严格一致, 不以静默降级伪造新能力. 宿主只有在目录版本不低于 20 且会话明确公布能力时才显示新入口, 因此旧插件和其他 Explorer Action 插件不会因宿主升级获得额外文件访问权. 当前已经落地不需要输入授权的“新建文件夹”和使用独立冻结输入清单的“添加文件或文件夹”. 两者都不把覆盖、删除源文件或任意路径访问混入同一个版本.

会话与条目能力按以下层次发布:

```text
session.getInfo()
    -> canCreateDirectory       # 当前已实现
    -> canAddEntries            # 当前已实现

directory item
    -> canCreateChildren        # 当前已实现
    -> canAddChildren           # 当前已实现
```

根目录使用现有 `rootId`; 其他目标目录继续使用会话内不透明 `entryId`. 能力只有在当前格式和档案变体已经具备 `ADD` mutation provider、目标目录路径安全、会话可替换且当前没有活动任务时才为真. 文件、危险路径隔离项和只读变体不公布子项创建能力. 普通文件系统页面的创建入口、列表、路径栏和选择栏不改变; 宿主只在档案 provider 中启用已有扩展槽位.

新建空目录继续调用 v19 已有的 `mutateEntries`, 但使用 v20 新操作 `CREATE_DIRECTORY`:

```text
CreateDirectory
├─ operationId
├─ parentEntryId              # rootId 或安全目录 entryId
└─ newName                    # 安全叶名称, 最长 4,096 字符
```

请求不携带内部路径. 插件把不透明 ID 解析到当前不可变快照, 再交给现有格式中立 mutation provider. 目标已有同名、大小写折叠或 Unicode 等价项目时整项失败, 不覆盖也不静默编号; 用户修改名称后以新 operation ID 重试.

提交后重建索引时, 插件先保留全部仍存在条目的既有 ID, 再为新增目录分配避开整组保留值的新 ID. 不能直接复用重建后条目序号的默认哈希, 因为新条目可能占用旧条目原有序号并与保留 ID 碰撞. 这项约束保证当前路径与选择可以继续按既有条目恢复, 同时让新增目录获得会话内唯一的不透明身份.

添加文件或目录树通过追加在 `IExplorerArchiveSession` 末尾的 `addEntries(request, replacementSession, inputSession, callback)` 完成. `request` 只包含 operation ID 与目标目录 ID; `replacementSession` 仍是只能替换当前档案的 v8/v18 事务会话. 新增的 `IExplorerArchiveInputSession` 是独立的只读最小授权, 不复用同时含输出、回收站和历史方法的通用 `IExplorerActionHostSession`:

```text
IExplorerArchiveInputSession
├─ getInfo()                  # grantId、virtualRootId、冻结计数与已知总大小
├─ listChildren(parentId, offset, limit)
├─ openFile(itemId)
├─ verifySnapshot()
└─ close()
```

宿主在用户确认选择后先扫描最多 128 个根项目, 构造最多 100,000 个节点、最大深度 1,024、累计名称文本不超过 16 MiB 的冻结清单. 每个节点只向插件公开 grant 内不透明 ID、父 ID、安全显示名、文件/目录类型、大小与修改时间; 不公开 URI、规范路径、父目录或未选择的兄弟项目. 宿主拒绝符号链接、特殊文件、不可读项目、越界树、选择后的结构漂移, 以及同一实际目录内按 NFC 与大小写折叠后产生歧义的兄弟名称. 不同父目录中被用户分别选中的同名根保持为不同不透明身份, 交由档案根冲突规则安全编号. `openFile` 仅接受冻结清单中的普通文件 ID, 以只读描述符打开并再次核对设备号、inode、类型、大小、修改时间与状态变化时间身份. 插件读完全部源以后、验证待提交档案以前必须调用 `verifySnapshot`; 任一节点或目录成员变化都返回 `SOURCE_CHANGED`, 中止替换事务.

输入会话和替换会话由宿主同时创建并绑定同一插件 UID、档案 session ID 与 operation ID. 每个档案会话仍只允许一个解压或修改任务. 用户取消、页面离开、插件 Binder 死亡、输入变化、读取失败、输出失败或回调畸形都会关闭两个任务会话并回滚待提交输出; 插件不得保存 Binder、节点 ID 或描述符供下一项操作复用. 成功路径必须依次完成源读取、输入快照复核、待提交档案完整重开验证、宿主原子替换和新索引构造, 然后才能回调完成.

直接添加多个文件保持整批原子语义: 任一目标名称与现有条目或同批项目冲突时全部失败, 不覆盖部分项目. 导入目录树延续现有管理 Activity 的规则, 根目录碰撞时按选择根身份分别把导入根安全编号为 `name (2)`、`name (3)` 等名称; 两个来自不同父目录的同名根不会合并, 实际树内任何冲突仍使整批失败. 空目录可以导入; 空的多选请求不可提交. v20 不提供交互式逐项覆盖, 避免在已经读取部分输入后改变不可变计划. 如果以后需要覆盖, 必须以新的显式冲突决定和重试合同扩展, 不能改变 v20 默认行为.

v20 创建与添加纵切片先在 G8441/Android 9 通过插件会话 10/10、宿主冻结输入会话 5/5、真实跨进程档案闭环 2/2 与文件管理器无障碍回归 2/2, 随后又在 Android 7/API 24 x86、Android 7.1/API 25 x86 与 Android 15/API 35 arm64 小米平板逐端通过同样的 10/10、5/5、2/2 与 2/2, 每端 19/19、跨版本合计 57/57. 覆盖根目录和嵌套空目录创建、文件与完整目录树混合添加、两个不同父目录下同名选择根的独立编号、重复名称与文件父节点拒绝、ZIP/TAR 族能力发布、旧 ID 保留与新增 ID 避碰、一次性只读文件、同尺寸内容改写复核、等价名称歧义、重叠输入、符号链接及所选父目录替换竞态, 以及删除至合法空 ZIP 后重新绑定打开. 新按钮在普通页面继续默认 `gone`, 五项选择操作和既有文件管理器布局保持不变. Android 9 设备恢复到测试前的 arm64 AutoJs6 6.8.0/5276 与 Archive Manager 2.19.0/25; 两台 Android 7 AVD 恢复为宿主、插件及两侧测试包均未安装; Android 15 设备恢复为唯一原有的 Archive Manager 2.9.0/15. 各设备需要保留的安装包均从实际安装路径回读并与测试前备份逐字节一致.

### Explorer Action v9 目录输出与恢复

v9 在 v8 AIDL 末尾追加 `prepareOutputTree`、`createOutputDirectory`、`openOutputFile`、`queryOutput`、`listOutputs` 与 `attachClient`. `extract-to` 以 `CREATE_IN_PARENT` 打开会话, 采用以下流程:

```text
attachClient
    -> prepareOutputTree
    -> createOutputDirectory / openOutputFile
    -> close every writer
    -> host tree verification
    -> commitOutput
```

宿主在当前父目录预留一个尚未发布且不会复用既有内容的根目录. 插件只提交安全相对路径; 宿主逐项限制节点数、路径长度与总写入量, 文件描述符关闭后校验完整目录树, 同步数据并以同目录原子改名发布根目录. 等价名称冲突统一安全编号, 因而整个任务可以在失败时删除新根而不触碰既有文件夹.

目录事务状态写入有界持久化日志. `attachClient` 把活动事务绑定到插件进程的 Binder token, 客户端死亡会立即触发回滚; 宿主进程重建后, 新会话通过 `listOutputs` 与日志恢复清理未完成事务. 已提交终态保留有限时间供客户端核对, 不暴露隐藏临时路径. `queryOutput`/`listOutputs` 只返回调用插件 UID 自己的有界记录.

## 路径栏与宿主原生档案页面

宿主的普通文件路径栏已经实现. v4 压缩请求复用它所在页面的父目录并传递规范显示路径; v5-v20 档案页面直接使用同一个路径栏和文件列表, 不创建插件私有导航界面.

宿主视觉集成遵循“复用而不重排”的边界: 插件动作、档案 provider 和档案专属控件可以进入宿主已经定义的扩展槽位, 但不得以插件集成为由改变普通文件管理器的全局列表、路径栏、分类栏或选择栏几何尺寸与既有样式. 无障碍改进优先增加角色、状态、说明和播报等语义属性; 若必须改变全局触控或排版尺寸, 应作为独立的宿主产品决策评审, 不再夹带在插件阶段中. 插件独有的 Activity、档案页面专属操作以及仅在档案会话中出现的内容可以在现有设计语言内单独优化.

进入档案时, 宿主保存原 Explorer、根页面、当前页面、历史栈和滚动状态, 再切换到只读档案 provider. 当前逻辑状态为:

```text
ArchivePathState
├─ sessionId
├─ archiveDisplayName
├─ sourceParentSegments[]
├─ currentEntryId / ancestorPages[]
└─ previousExplorerState
```

路径栏当前绘制:

```text
外部路径 / 档案名 / 档案内部目录层级
```

v13 对支持文件名编码覆盖的 ZIP 在路径栏右侧显示编码操作. 宿主从会话信息读取有界的支持列表、当前有效编码和显式覆盖值; 选择自动或某个编码后调用追加在 `IExplorerArchiveSession` 末尾的 `reindexFilenameCharset(name)`. 插件只重新扫描同一暂存源, 成功构造完整替换状态后才原子切换. 无效名称、扫描失败、活跃条目流或解压任务均保留旧索引.

v17 会话在结构探测成功后返回有界的稳定格式 ID、显示名称, 以及源显示名是否已经匹配该格式. 名称不匹配时, 路径栏只把档案根显示为例如 `backup.data [ZIP]`; 内部目录层级保持原样. 缺失或畸形元数据会使宿主拒绝建立 v17 页面, 而 v5-v16 会话继续保持原行为. 这些字段只用于说明实际格式, 不改变任何读取、写入、分卷或修改授权.

真实条目 ID 由源目录序号派生, 隐式目录 ID 则由其后代源序号和层级派生, 不使用解码后的路径文本. 宿主因此可在名称变化后重建祖先链和选择; 精确路径不存在时退回仍存在的最深祖先, 只在精确恢复时复用滚动与折叠状态. 会话 ID、根 ID、源大小和修改时间在重建前后必须一致. 页面离开、会话关闭或任务取消时不会把迟到结果应用到新的 Explorer 状态.

档案名和内部祖先页面可点击跳转. 点击外部层级会先关闭档案会话, 再回到对应文件系统目录. 普通浏览状态下, 返回键先回到档案内部上一级, 到达根目录后再关闭会话并恢复进入前的文件系统页面与滚动状态.

档案目录当前复用宿主的列表、排序、搜索、下拉刷新、加载、错误、空状态、主题、暗色模式、动态色和系统栏. 目录项可以下钻; v6 可读取条目会按宿主原有类型规则使用文档、图片或媒体预览器, 不支持预览的类型仍明确不可用. v10 在路径栏右侧提供当前内部目录解压, v11 在同一宿主页面呈现密码、危险路径与资源预算交互, v19 按能力复用既有重命名和删除流程, v20 再在档案专属路径栏扩展区提供新建空目录与添加文件或文件夹. 这些入口在普通文件系统页面保持 `gone`, 文件系统工具栏和整体布局不变. `manage-archive`、`extract-to` 与压缩表单仍由插件 Activity 呈现, 其中管理动作的目标替换由 v8 宿主会话控制.

## 管理页解压与档案修改

文件菜单中的 `manage-archive` 动作以 v8 单文件目标替换请求打开 ZIP、可写 7Z 或可写 TAR 管理页, 不会触发整包快捷解压. 档案完成索引后, 解压按钮根据当前位置提供真实可用的范围:

- “全部”始终选择档案根;
- “当前目录”只在档案内部可提取的非根目录显示, 并选择当前目录的完整子树; 危险名称的只读隔离目录不会伪装成可提取范围;
- “当前勾选”只在至少存在一个勾选路径时显示.

用户选定范围时, 页面立即把它固化为有序去重的档案路径集合. 后续密码解锁、危险路径确认、资源预算确认、系统目录选择和 Activity 重新扫描都继续使用同一集合, 不会退回当时可能已经变化的勾选状态. 文件菜单中的 `extract-to` 则保持快速路径: 校验档案后推荐通过 v9 解压到档案同级的同名目录, 也可改用系统目录选择器指定 SAF 目录.

`extract-to` 的同级输出权限来自目录声明的 v9 `CREATE_IN_PARENT` 会话, 不是 `parentUri` 本身. `parentUri` 仍只用于显示和一致性校验, 插件不能根据 URI 结构猜测本地路径. 用户选择其他位置时, 输出只使用 Android 系统选择器授予的 SAF 目录权限. `manage-archive` 使用 v8 `REPLACE_TARGET`, 因而管理页的范围解压直接选择 SAF 目录, 不显示一个无权执行的“当前文件夹”选项.

管理页对普通单卷 ZIP 与符合条件的 TAR、TAR.GZ/TGZ、TAR.XZ/TXZ、TAR.BZ2/TBZ2、TAR.ZST/TZST 显示“添加文件...”“添加文件夹...”“新建文件夹...”“重命名...”和“删除”. “添加文件夹...”通过系统选择器读取完整目录树, 以有界深度优先扫描固化嵌套文件、空目录、大小与顺序, 再与其他操作一样生成不可变变更计划. 每次计划都会检查危险路径、重复/大小写或 Unicode 等价名称、文件/目录冲突、目录子树碰撞、无法保留的方法或加密及源档案变化; TAR 另拒绝链接、设备节点、稀疏项等特殊条目, 全部通过后才预留 v8 替换事务.

ZIP 重建按原顺序复制保留条目, 支持 Stored/Deflate、ZipCrypto 与 AES, 保留可用时间戳; 加密档案中新增文件使用 AES. TAR 重建以一次顺序源扫描复制保留的普通文件与目录, 保留可用时间戳, 并按需使用 POSIX/PAX 表达长名称或非 ASCII 名称. TAR.GZ、TAR.XZ、TAR.BZ2 与 TAR.ZST 在同一次源扫描中流式解压, 再分别以固定 GZIP 级别 6、XZ preset 4、BZIP2 block-size preset 6 和 Zstandard 级别 3 直接写入宿主持有的待提交输出; 四者都不创建私有未压缩 TAR. 输出 GZIP 的修改时间固定为 0, 不写原文件名或注释, OS 字段固定为 255. 输出 XZ 使用 4 MiB 字典与 CRC64 检查; XZ for Java 1.12 报告 preset 4 编码内存为 48,058 KiB, 低于 mutation provider 的 64 MiB 显式预算. 输出 BZIP2 使用 600,000-byte block; provider 按 Commons Compress 1.28 的主排序区、映射区与后备排序区保守估算 8,324,288 bytes 最坏工作区, 低于 16 MiB 显式预算. 输出 Zstandard 固定为单线程、1 MiB 窗口和帧 checksum; 官方 Zstandard 1.5.7 的 arm64 流编码器估算与 zstd-jni 输出缓冲合计 2,746,400 bytes, 低于 8 MiB 显式预算. 因此界面会说明四种包装层的压缩设置与容器元数据可能被规范化.

所有可写格式生成后都通过统一 `CreatedArchiveVerifier` 重新读取全部条目并核对路径、类型、大小、可用 CRC 与实际内容指纹, 之后才提交. 压缩 TAR 的 reader 会在关闭时继续读取到容器尾部以验证校验值, 同时逐块响应取消; 取消、GZIP/XZ/BZIP2/Zstandard 尾部或 checksum 损坏、输出写入失败、验证失败或源变化都会中止待提交输出. 四种压缩 TAR 的磁盘空间模型都不包含私有未压缩 TAR, 但仍需要宿主同目录待提交压缩输出以及 v18 最近替换恢复副本. 7Z 修改同样直接写入宿主持有的待提交输出并完整读回; 加密、solid、分卷或解码资源超预算的 7Z 保持只读. JAR/AAR/WAR、RAR 与含特殊条目的 TAR 没有管理能力; 分卷 ZIP 虽可能通过 `.zip` 扩展名初筛, 但结构探测不会进入可写状态.

### 输出名称冲突

管理页的冲突策略下拉框提供“每次询问”“跳过”“覆盖”和“自动重命名”, 默认逐项询问. 询问对话框显示档案路径、既有项目与待写入项目的名称和类型, 并允许把当前决定应用到全部兼容冲突. 文件/目录类型不一致或目标节点不属于本次解压时, 覆盖选项会明确禁用.

解压器先在新输出根内规划目录和文件节点, 再读取条目数据. 因此跳过的项目不会抬高实际进度总量, 自动重命名会在扩展名前添加编号, 覆盖只会复用本次任务已经创建的同类型节点. 固定“覆盖”策略遇到类型不一致时自动改用重命名, 不删除已经规划的目录树. 完成结果分别报告跳过、覆盖/合并和自动重命名的数量.

冲突策略不会把本次任务扩展为对既有目录的非事务式修改. 若系统选择目录下已经有同名输出根, 根目录仍自动编号, 原目录及其内容不会被读取、合并、覆盖或删除. 这个边界保证任何后续失败或取消都只需删除本次新建的根, 不会要求在通用 SAF provider 上备份并恢复用户原有内容.

## v10/v11/v19/v20 宿主原生任务与创建能力

v10 在 `IExplorerArchiveSession` 末尾追加 `extractEntries(request, outputSession, callback)` 与 `cancelExtraction(operationId)`. `request` 只包含 UUID 操作标识和当前会话内有序去重的不透明条目 ID; 单次最多 128 项. 宿主为每个任务单独建立 v9 目录输出会话, 插件不获得父目录路径、URI 或任意写权限.

one-way 回调只允许以下有界事件:

```text
onProgress(operationId, phase, completedEntries, totalEntries,
           writtenBytes, totalBytes, currentEntry)
onCompleted(operationId, outputDisplayName, completedEntries, writtenBytes)
onFailed(operationId, errorCode, safeMessage)
```

宿主同时校验插件 UID、操作标识、阶段、单调计数与文本长度; 每个档案会话只允许一个活动解压任务. 取消、插件 Binder 死亡、宿主页面离开或任何失败都关闭并回滚未发布输出; 只有插件完成写入、宿主校验整棵树并提交后, 才显示新目录并刷新 Explorer.

“当前目录”请求传递当前页直接子项, 其中目录由插件展开完整子树; “当前勾选”传递宿主选择栏中的不透明 ID. v11 把密码请求、密码错误、危险路径确认和资源预算确认表达为类型化交互, 由宿主原生对话框处理后重试同一不可变条目集合; SAF 目标仍转入管理/解压 Activity, 文件名编码修正则由 v13 留在原生页面完成.

v11 没有改变 `extractEntries` 或回调的 AIDL 签名, 只扩展 Bundle 字段. 插件用 `EXTRACTION_ERROR_INTERACTION_REQUIRED` 失败结果携带一个 `interactionKind`: `PASSWORD_REQUIRED`、`WRONG_PASSWORD`、`UNSAFE_PATHS` 或 `RESOURCE_BUDGET`. 宿主冻结有序去重的条目 ID、当前内部路径和源快照, 获取用户决定后以新的操作 ID 重试, 并按交互类型传入瞬态 `PASSWORD`、`SKIP_UNSAFE_PATHS` 或 `ALLOW_RESOURCE_BUDGET_OVERRIDE`. 这些确认只作用于原选择: 重试前若会话、源身份、路径上下文或选中集合变化, 操作封闭失败.

解压密码与首次打开密码遵循相同的 1,024 字符边界和清理合同. 宿主在一次同步 `extractEntries` 请求返回后立即清零并移除请求中的密码; 插件复制到有界 reader 选项后立即清理 Bundle 缓冲, 并在任务成功、失败、取消或快照替换时清理内部副本. 错误密码不会复用旧输入, 宿主会先清空输入框再允许重试.

v10 只交付了解压; v19 补齐删除/重命名, v20 再补齐创建/添加的会话级与逐条目能力、进度、取消和可回滚修改任务. 档案目标继续只使用会话内不透明 ID:

```text
session.getInfo()
    -> canDeleteEntries + canRenameEntries
       + canCreateDirectory + canAddEntries

item
    -> canDelete + canRename + canCreateChildren + canAddChildren

session.mutateEntries(request, replacementSession, callback)
session.addEntries(request, replacementSession, inputSession, callback)
session.cancelMutation(operationId)
```

这一跨进程虚拟目录合同的删除/重命名部分由 v19 落地, v20 再交付空文件夹创建与文件/完整目录树添加. 插件管理 Activity 内部既有的 `ArchiveMutationProvider` 是格式后端基础, 跨进程层只增加最小 AIDL 编排并继续以 v8 目标替换提交完整重建结果. 文件和目录树通过独立的 `IExplorerArchiveInputSession` 取得受控输入, 不从删除/重命名、空目录请求或通用文件会话推导权限.

每个条目需要独立的 `canOpen`/`canExtract`/`canDelete` 等能力与不可用原因. `entryId` 不能只由规范化路径生成, 因为真实档案可能包含重复名称、原始编码差异或同名文件/目录.

`ArchiveEntryPathStatus` 与后端 `ArchiveEntryCapabilities` 分开表达. `UNSAFE_ISOLATED` 不会撤销后端的只读 `canOpen`, 但有效 `canExtract` 必定为 `false`. 根目录或其他正常选择解析时会把这类条目放入 `skippedUnsafeEntries`, 不会把隔离目录或其虚拟序号写到目标. 管理/解压页必须展示数量并由用户选择“跳过并继续”; 解压器默认返回 `UNSAFE_PATH_CONFIRMATION_REQUIRED`, 只有调用方显式传入 `skipUnsafePaths` 才能继续写出其余安全条目. 仅选择危险条目时返回空安全选择, 不创建空输出根.

## 格式能力模型

插件已经通过 `ArchiveEngine` 统一扫描、预览、解压和创建链路. `ArchiveFormat` 提供格式标识、扩展名和 MIME 类型; `ArchiveReader`/`ArchiveWriter` 隔离具体库; `FormatCapabilities` 与 `ArchiveEntryCapabilities` 分别表达格式级和条目级真实能力. ZIP 字符集探测和底层目录对象只存在于 ZIP 后端内部.

修改链路现在另由 `ArchiveMutationProvider` 注册到同一个后端. 通用请求只表达添加文件、添加目录树、新建目录、删除和重命名; provider 在预留宿主输出之前生成绑定源快照版本的不可变计划. 计划公布结果条目/文件/目录数量、需要读取的已知内容字节、未知大小文件数、当前源档案大小和可能受影响的辅助元数据, 但不会在压缩比未知时伪造精确输出大小. 动态可用性把不支持格式、分卷、危险路径、缺少密码、不支持的方法和只读后端变体区分为稳定原因.

插件内部的 TAR 族与 7Z provider 共用格式中立的 `ArchiveRewritePlanner`; planner 只处理最终条目树、冲突、工作量、源版本和元数据影响, 每个 provider 仍显式提供自己的能力集合与保留条目校验器. 这项复用不改变 Explorer Action 协议、最低宿主版本、动作目录或任何宿主 UI, 也不会把 ZIP、RAR、加密/solid/分卷 7Z 或特殊条目 TAR 的既有边界自动扩展为可写.

管理 Activity 通过 `ArchiveManagementStatus` 把上述后端动态可用性与当前会话是否为管理动作、是否具有 v8 目标替换事务合并为一个展示模型. 档案信息入口因此可以同时显示探测格式、内容统计、真实操作集合和精确只读原因; 即使某个 ZIP 变体当前只读, 仍可说明普通同格式 provider 的能力与该变体被拒绝的具体边界, 而不是只隐藏按钮.

每次修改均先调用 `prepare`, 在主线程显示事实型预检, 只有用户明确继续后才调用 `execute`. `execute` 内部才允许向宿主预留 v8 待提交输出; 因此取消、返回、对话框关闭或协程取消不会产生宿主写调用, 也不会创建需要回收的待提交事务. 预检展示源档案大小、结果条目/文件/目录数、已知读取量、未知大小文件数、完整重建策略和稳定排序的辅助元数据影响; 最终压缩大小未知时明确说明未知, 不使用虚假的压缩比预测. 不可变计划随后直接交给执行器, 不在确认后悄悄重新规划另一组变更.

这些管理信息与预检首先由插件管理 Activity 实现. 宿主文件管理器深度重构完成后, v18 在既有替换事务边界增加最近版本恢复, v19 再把删除/重命名编排接入宿主原生档案页, 最低宿主版本代码继续为 5276. v19 只修改档案会话模型、provider 与既有动作分派, 没有修改宿主 XML、尺寸、图标、普通文件列表、路径栏、分类栏、选择栏或整体视觉结构.

`ArchiveEngine` 启动时强制要求 `FormatCapabilities.canAdd/canDelete/canRename` 与 provider 操作集合完全一致. “管理压缩档案...”的扩展名和复合后缀只从已注册 provider 的安全叶扩展名与复合后缀生成; 因此创建 writer 存在并不会自动开放档案修改, JAR/AAR/WAR 也不会因为共享 ZIP reader 而获得修改入口. 当前已注册普通单卷 ZIP、符合安全边界的 7Z、TAR、TAR.GZ、TAR.XZ、TAR.BZ2 与 TAR.ZST provider; `.tgz`/`.txz`/`.tbz2`/`.tzst` 作为安全叶别名进入目录, `.tar.gz`/`.tar.xz`/`.tar.bz2`/`.tar.zst` 使用 v14 复合后缀. 7Z provider 在索引后拒绝加密、solid、分卷、危险路径、不支持方法或解码资源超预算的输入.

ZIP 后端同时公布可用的文件名解码覆盖列表. 管理/解压 Activity 与 v13 宿主原生页面均可在自动识别结果与 UTF-8/GB18030/Shift_JIS/EUC-KR/windows-1251/windows-1256/windows-1252/IBM437 之间切换, 每次切换都会重新索引同一暂存输入. 扫描快照保存最终选择, 预览流和解压器重新打开档案时必须复用它, 防止列表名称正确而实际写出时使用另一套编码. 一次性密码请求由 v11 落地: 加密 ZIP/7Z/RAR 可在首次打开或解压条目时留在宿主原生页面完成解锁与错误密码重试; v13 重建索引时复制既有密码到替换快照, 成功或失败后清理不再使用的缓冲.

ZIP reader 会在解析目录前识别标准 PKZIP 风格的 `.z01 + .zip` 分卷. 首卷通过分卷标记识别, 末卷通过 EOCD/Zip64 结构确认. Explorer Action v12 在用户选择最终 `.zip` 时由宿主生成最多 127 个确定性 `.zNN` 候选名, 仅把实际存在、可读且不是符号链接的普通文件放入目录; 插件按 EOCD 所需卷数精确打开并复制完整授权卷组到私有平面目录, 通过 Zip4j 只读索引和数据流, 且不声明删除或重命名. 缺卷时仍返回 `INDEX/MISSING_VOLUME`. RAR reader 通过同一 v12 源按 Junrar 卷名规则惰性打开现代 `partN.rar` 或传统 `.rar/.r00` 伴随卷. 完整卷组可浏览、预览和解压, 缺少后续卷时返回 `MISSING_VOLUME`, 任一卷目录或身份变化时返回 `SOURCE_CHANGED`; RAR 始终保持只读.

编号 `.zip.001` 与 `.7z.001` 通过 v14 动作后缀进入同一 v12 分卷源. 宿主只为完整文件名匹配的 `.001` 首卷生成 `.002` 至 `.128` 候选, 普通 `notes.001` 不会匹配; 插件只接受从首卷开始的连续编号, 以只读 `MultiReadOnlySeekableByteChannel` 顺序拼接获批描述符. 7Z 可直接使用多通道 reader; ZIP 在后端请求本地文件时通过统一缓存暂存器组合全部卷, Android 7 也使用这一兼容路径. 暂存前后会重复核对聚合大小、最新修改时间和逐卷身份. 缺口报告下一个预期卷名, 任一卷变化报告 `SOURCE_CHANGED`, 两类编号卷组均保持只读.

读取失败通过统一诊断模型区分输入、格式识别、目录索引、密码、条目数据、输出和清理阶段. 可公开的摘要只包含格式、阶段、稳定错误码及受控原因; 调试构建由用户主动复制的诊断才包含异常链和堆栈. `ExplorerActionService` 把相同安全摘要放入跨进程异常消息, 不传递插件缓存路径.

当前压缩表单的格式列表、压缩级别、密码、文件名加密和分卷控件由已注册 writer 的能力生成. ZIP 将密码和分卷声明为可选并提供对应控件; TAR 系列声明两者均不支持, 7Z 只声明可选密码. 切换格式时会清空不适用的密码; 不支持分卷的格式显示 "无"、禁用控件且不提交值, 返回 ZIP 时恢复先前选择. 未压缩 TAR 只提供级别 0; GZIP、XZ、BZIP2 与 Zstandard 容器提供各自后端验证过的级别. 文件名加密继续声明为不支持. 动作目录的可读扩展名与 MIME 类型也来自同一格式注册表. 尚未实现的后端不会只因 Roadmap 中出现格式名称就进入菜单.

压缩和修改表单必须继续由“真实格式 + 当前后端 + 当前选择”共同生成:

```text
FormatCapabilities
├─ canList / canExtract / canCreate
├─ canAdd / canDelete / canRename
├─ password: NONE | OPTIONAL | REQUIRED
├─ filenameEncryption: UNSUPPORTED | OPTIONAL | REQUIRED
├─ splitVolumes: UNSUPPORTED | OPTIONAL | REQUIRED
├─ compressionLevels[]
└─ limitations[]
```

特别是“同时加密文件名”应按三态显示: 不支持时关闭且禁用; 可选时默认关闭; 格式强制时开启且禁用. 禁用控件旁必须展示原因, 不能仅用灰色暗示.

当前注册 ZIP、7Z、RAR 与 TAR 族后端. ZIP 支持识别/列表/预览/打开/解压/创建和可选密码, 读取传统 ZipCrypto 与 AES, 加密创建固定使用 AES-256; `jar`、`aar` 和 `war` 是只读扩展名别名. 普通单卷 `.zip` 另声明添加文件、新建空目录、删除与重命名能力, 并通过 v8 事务重建; 别名格式与分卷 ZIP 不声明这些能力. ZIP writer 声明可选分卷, reader 通过 v12 读取完整标准 `.z01 + .zip` 卷组并在伴随卷不可用时给出精确诊断. 7Z 支持普通/solid 档案及 AES 内容/头部加密读取, 创建非 solid 输出并可选 AES-256 内容加密; 级别 0 使用 Copy, 1 至 9 使用 LZMA2, 文件名保持可见. 普通单卷、未加密、非 solid 且解码资源位于 64 MiB 修改预算内的 7Z 另声明添加/删除/重命名, 以固定非 solid LZMA2 preset 3 通过 v8 重建; 加密、solid、分卷与超预算变体保持只读. RAR reader 支持单卷或完整分卷 RAR4/RAR5、内容加密与头部加密读取, 使用 256 MiB 字典上限; 它不声明创建、添加、删除或重命名, 缺卷时只保留安全元数据或返回类型化失败. TAR 族支持识别/列表/预览/打开/解压/创建, 包含未压缩 TAR 以及 GZIP/XZ/BZIP2/Zstandard 容器. 仅含安全普通文件和目录的五种 TAR 容器另声明添加/删除/重命名并通过 v8 事务重建; 特殊条目 TAR 不声明这些能力. TAR writer 与重建器使用 POSIX PAX 处理 UTF-8 与长路径, 不跟随源符号链接; 无法预先获得新增文件大小时会先测量再重新打开输入. Zstandard 使插件 APK 包含 `arm64-v8a`、`armeabi-v7a`、`x86` 与 `x86_64` 原生库, `PluginInfo.supportedAbis` 必须与该完整清单一致. 所有 writer 均不声明创建时文件名加密; 7Z 与 TAR writer 也不声明分卷. 后端路线、APK/ABI/许可证门禁和测试要求见 [`docs/adr/0001-archive-engine-and-backend-strategy.md`](adr/0001-archive-engine-and-backend-strategy.md).

v20 对普通单卷 ZIP、安全普通 7Z 和符合条件的五种 TAR 容器复用同一组动态修改能力, 因而只有这些后端及当前结构同时允许 `ADD` 时, 宿主原生页才公布创建和添加入口; 只读别名、RAR、分卷、加密、solid、危险路径、特殊条目或超预算变体不会显示不可执行动作.

外部工具生成的兼容性样本、复现命令和 SHA-256 清单位于 [`compatibility`](../compatibility/README.md). Android、Windows 资源管理器、7-Zip、WinRAR、Info-ZIP、macOS Archive Utility 及 Java/Kotlin 工具链均已有行为样本; 其中 Windows Explorer、7-Zip、WinRAR、Info-ZIP 与 Java/Kotlin 生产者使用可复现的已提交夹具, MT Manager v2.26.8 与两个外部 macOS ZIP 使用固定哈希的本地观察语料. 后两类只有在取得原始创建设置后才可提升为可重新生成的已提交夹具, 但不再阻塞格式行为矩阵.

## 不可取消的安全边界

固定输入大小和浏览阶段压缩比门槛可以调整或移除, 但以下边界不是兼容性限额, 不提供关闭开关:

- 目标根目录与规范路径约束;
- Zip Slip/绝对路径/驱动器路径和符号链接越界防护;
- 危险原始名称只读隔离、可见字符转义和写出前显式跳过确认;
- Binder 调用 UID 固定、同级分卷不透明 ID 与最小 URI/文件描述符授权;
- v20 添加输入只公开冻结清单内的不透明节点、元数据与一次性只读描述符, 不公开源路径、URI、未选同级项目或通用存储权限, 并在提交前重新验证完整清单;
- 输出暂存、失败清理及不覆盖既有文件的事务规则;
- 档案内部路径冲突按原始安全路径精确判断, 允许合法的大小写与 Unicode 拼写变体继续浏览; 写入 SAF 目标时则按 NFC 与大小写折叠识别已有根名称并自动编号. 如果文档提供程序把新建文件或目录折叠到 writer 已创建或目标中既有的节点, 解压立即以输出失败中止并删除本次新建根目录, 不把创建操作降级为覆盖;
- 密码不写日志、不进页面状态 Bundle、不持久化, 也不进入错误诊断;
- 密码使用可主动清零的有界字符缓冲; Explorer Action v11 同步请求返回、扫描快照替换、创建或解压任务结束及页面销毁时清零并移除. 运行时库可能产生无法完全控制的短生命周期副本, 因此只承诺尽力清理;
- 无密码时可列出只加密内容且头部可见的 ZIP/7Z/RAR 目录; 头部加密档案在首次打开时由宿主请求密码, 条目解压中的缺少或错误密码也在保留路径和选择的情况下原位重试;
- 源项目移入回收站前的完整输出校验、提交证明、源身份复核与持久恢复副本.

管理/解压页提供可解释、可配置的兼容、严格和自定义资源预算. 兼容档位为 100,000 个选中输出项、16,384 字符路径、256 层深度、32 GiB 单项、256 GiB 总输出与 100,000:1 压缩比; 严格档位分别为 10,000、4,096、64、2 GiB、8 GiB 与 1,000:1. 自定义档位中, 结构维度填 `0` 表示使用相应的不可配置硬上限 (选中输出项使用 500,000 个展开节点上限), 大小和压缩比填 `0` 表示不设资源告警阈值.

资源预算仅影响写出决策, 不影响元数据浏览. 选择超出预算时, 页面在打开目标选择器之前列出预计输出大小、选中条目数和每个超限维度, 提醒通用 SAF 目标的可用空间无法可靠查询, 并要求本次操作的明确确认. 确认不会完全关闭实时保护: 单项/总字节和压缩比边界只扩展到当前选择的声明值; 未声明增长、源身份变化、大小/CRC 不一致、危险路径和失败清理仍按原规则中止.

## 解压进度与回滚结果

解压器通过同一 `ExtractionProgress` 模型报告 `PREPARING`、`EXTRACTING`、`COMMITTING`、`CLEANING_UP`、`CLEANUP_FAILED` 和 `COMPLETED` 阶段. 进度包含已完成/总条目数、已写入/预计总字节数和当前条目路径. 管理页使用单调时钟计算任务平均传输速度与预计剩余时间; 总字节可用时展示确定进度, 否则按条目数估算. 为避免大量小条目或高速复制阻塞主线程, 页面只在经过 250 ms、增加 8 MiB 或完成 64 个条目时刷新一次, 但解压器仍在每个复制块和每个条目边界检查协程取消.

任务取消或写出失败后, 解压器进入 `NonCancellable` 清理区并只删除本次创建的输出根, 不碰触目标中原有内容. 如果 DocumentsProvider 拒绝删除或清理本身失败, 解压器同时保留类型化的 `ArchiveCleanupException` 并发出 `CLEANUP_FAILED` 进度事件; 后者不能仅依赖取消异常的 suppressed 链, 因为协程取消传播不保证该链能完整到达 UI. 事件携带完整、稳定的 `ArchiveOutputLocation`, 页面以有界且转义后的显示名和 URI 标识列出残留根, 让用户能够定位并手动处理.

普通文件写出完成后按 reader 声明校验实际写入大小和 CRC; 完成整个选择后还会复核源身份. 任一校验失败都会进入相同回滚链路. v9/v10 同级输出由宿主在提交点刷新, 插件不会发送私有广播. SAF 输出仍由对应 DocumentsProvider 管理可见性; 如果后续动作需要刷新没有参与宿主事务的其他文件系统目录, 仍应扩展公共类型化结果而不是引用宿主私有 action.

## 兼容与发布边界

- 项目尚未公开发布, 因此直接使用 `archive-manager`、`io.github.supermonster003.autojs6.plugin.archivemanager` 和 Manager 类/资源名, 不保留旧命名别名.
- 当前插件的原生浏览与条目预览使用 Explorer Action v6, 压缩入口使用 v4 文件会话, 创建后的提交前完整校验要求 v7, ZIP/7Z/TAR 档案修改使用 v8, 受控目录输出使用 v9, 宿主原生当前目录/选择项解压使用 v10, 首次打开与解压密码恢复使用 v11, ZIP/RAR/编号 ZIP/7Z 同级分卷读取使用 v12, 宿主原生文件名编码重建使用 v13, `.zip.001`/`.7z.001` 与 TAR 复合后缀初筛使用 v14, 分卷与单独压缩的可恢复统一发布使用 v15, 经完整输出证明约束的源项目回收站交接使用 v16, 名称未匹配时的显式档案复核与实际格式提示使用 v17, 最近一次目标替换的宿主持有恢复使用 v18, 宿主原生档案删除与重命名使用 v19, 宿主原生新建空文件夹及添加文件或完整目录树使用 v20; 配套 AutoJs6 版本代码仍为 5276 或更高版本.
- 宿主仍可解析 v1-v4 插件目录, 但本插件不会发布旧动作、旧协议目录或旧 applicationId 的兼容入口.
- 后续协议字段必须保持显式版本与上限; 未知可选字段可以忽略, 未知必需能力必须明确拒绝.

具体任务与验收矩阵见 [`ROADMAP.md`](../ROADMAP.md).
