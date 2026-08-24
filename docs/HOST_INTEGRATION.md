# AutoJs6 文件管理器集成说明

本文档同时记录两类内容:

- 已经落地并通过端到端验证的 Explorer Action v4 受控文件会话、v5 只读档案列表与 v6 条目读取会话;
- 为档案内部选择、按项解压和档案内修改预留的后续合同.

完成状态以 [`ROADMAP.md`](../ROADMAP.md) 为准. 文中标记为“当前”的能力可以在现有宿主与插件中使用; 标记为“后续”的内容仍不可当作已发布功能.

## 当前集成结果

AutoJs6 文件管理器现在可以从插件目录动态发现以下动作:

| 动作 ID | 目标 | 位置 | 访问范围 | 呈现方 |
| --- | --- | --- | --- | --- |
| `open-archive` | 单个受支持的档案文件 | 文件菜单/主动作 | 只读目标 | 宿主 Explorer |
| `selective-extract` | 单个受支持的档案文件 | 文件菜单 | 只读目标 | 插件 Activity |
| `extract-to` | 单个受支持的档案文件 | 文件菜单 | 只读目标 | 插件 Activity |
| `compress` | 单个普通文件或目录 | 文件/目录菜单 | 读取目标并在父目录创建输出 | 插件 Activity |
| `compress-selection` | 同一真实父目录中的多个文件、目录或混合目标 | 多选操作栏 | 读取目标并在共同父目录创建输出 | 插件 Activity |

动作是否出现由插件目录、目标类型、基数、扩展名/MIME、入口位置、插件启用状态和信任状态共同决定. 未安装或禁用插件时不会留下占位菜单; 虚拟目标或跨父目录多选也不会显示一个无法执行的压缩入口.

多选操作栏使用五个等宽单元格, 图标在上、文字在下. 当前尺寸为 22 dp 图标与 11 sp 文字, 已在 411 dp 等效窄屏上完成真实点击和截图验证. 320/360/600 dp、大字体、横屏和 RTL 仍属于 Roadmap 中的测试矩阵.

## Explorer Action v4/v5/v6 目录

v4 在保留 v1-v3 解析能力的同时加入多目标与受控输出. v5 再加入呈现方式和只读档案列表, v6 为会话增加受控条目读取:

| 字段 | 语义 |
| --- | --- |
| `protocolVersion` | 当前为 `6` |
| `targetKind` | `FILE`、`DIRECTORY` 或 `MIXED` |
| `cardinality` | `SINGLE` 或 `MULTIPLE` |
| `accessMode` | `READ_ONLY` 或 `CREATE_IN_PARENT` |
| `placements` | 主动作、单项溢出菜单或多选操作栏 |
| `presentation` | 插件 Activity 或宿主 Explorer |
| `extensions`/`mimeTypes` | 动作初筛条件; 不是最终格式判定 |
| `priority` | 多个插件动作同时匹配时的稳定排序依据 |

当前压缩动作使用通配 MIME/扩展名和 `FILE`/`DIRECTORY`/`MIXED` 目标, 因此能覆盖普通文件、目录和同父级混合多选. 打开、选择性解压与整包快捷解压动作匹配 ZIP/JAR/AAR/WAR、TAR 及已注册的 GZIP/XZ/BZIP2/Zstandard 压缩 TAR 别名.

每个目录字段都有数量或长度上限. 宿主会拒绝非法目标类型、基数、位置、授权和呈现方式组合, 而不是静默扩大插件权限. `HOST_EXPLORER` 在 v5 及后续版本中只允许 `FILE + SINGLE + READ_ONLY + PRIMARY` 动作, 不能借此获取目录写入或多选能力.

## v4 执行请求

宿主通过显式 Activity Intent 启动解压与压缩动作. 压缩请求包含:

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

openOutput(transactionId)
    -> write-only ParcelFileDescriptor

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
- 同名目标可选择失败或自动编号, 并由宿主进程全局预留以避免并发覆盖.

创建表单默认使用自动编号. 用户也可以选择每次询问: 插件先把精确名称与 `OUTPUT_CONFLICT_FAIL` 交给宿主; 若当前宿主以 `IllegalArgumentException` 拒绝这次预留, 插件会在尚未遍历源目录、打开源文件或打开输出描述符时返回表单, 让用户编辑名称、取消, 或明确改用 `OUTPUT_CONFLICT_AUTO_RENAME` 重试. 插件只把精确预留失败表述为“宿主无法预留该名称”, 不解析宿主私有异常文本, 也不会把它伪装成已经确认存在同名文件. 其他异常仍按普通创建失败处理.

Explorer Action v6 的公共 AAR 只有 `OUTPUT_CONFLICT_FAIL` 与 `OUTPUT_CONFLICT_AUTO_RENAME`; 没有覆盖策略、类型化名称冲突状态或替换既有目标的提交语义. 当前宿主提交还要求最终目标不存在, 因此插件不会通过 URI 猜测本地路径、删除既有目标后重试或调用宿主私有实现来伪造覆盖. 后续协议若开放覆盖, 至少需要满足以下合同:

- 返回可机器判定的名称冲突状态, 使询问只在真实冲突时出现;
- 在宿主进程内同时预留目标名称与原目标身份, 防止检查和提交之间被替换;
- 始终写入同目录临时文件, 写入关闭并通过只读重开校验后才发布;
- 提交时使用受支持的原子替换, 或以备份与回滚保证任一步失败后原档案仍可恢复;
- 中止、插件退出或宿主进程恢复只清理本次临时状态, 不修改原档案.

## v5/v6 只读档案会话

`open-archive` 不再启动插件文件列表 Activity. 宿主重新验证动作匹配、插件信任与启用状态后, 以只读方式打开规范化的源文件, 再通过专用服务绑定调用:

```text
IExplorerActionPlugin.openArchive(sourcePfd, request)
    -> IExplorerArchiveSession

request
├─ displayName
└─ size                         # -1 表示未知

session.getInfo()
    -> sessionId + rootId + displayName + sourceSize + sourceLastModified
       + canOpenEntries

session.listChildren(parentId, offset, limit)
    -> items[] + nextOffset + complete

item
├─ id                           # 会话内不透明 ID
├─ parentId
├─ name
├─ kind                         # 文件或目录
├─ size / compressedSize
├─ lastModified
└─ canExtract

session.openEntry(entryId)      # v6, 仅普通可读取条目
    -> read-only ParcelFileDescriptor

session.close()
```

当前 ZIP、7Z 与 TAR 族实现优先租用宿主传入的只读可 seek 描述符, 不再为普通档案复制整份内容. 输入只有同时通过进程自身 `fdinfo` 的只读模式位、`fstat` 普通文件类型、`lseek` 随机定位、`pread` 首尾读取、实际大小与宿主报告大小核对后才进入直读路径. 每个统一 reader 都在同一租约上建立拥有独立逻辑位置的只读通道, 实际读取通过 `pread` 完成, 不共享或修改描述符的文件偏移, 不通过 `/proc/self/fd` 路径重新打开档案, 也不把描述符编号跨 Binder 返回. 描述符租约持续到页面关闭、解绑或会话失败. 管道、可写描述符、不可 seek 代理、Android 7, 以及必须获得进程可读本地文件的后端会回退到插件私有缓存; 当前加密 ZIP 由只接受 `File` 的 Zip4j 读取, 因而在后端确认加密后才按需物化, 普通 ZIP 仍保持直读. 7Z 支持普通/solid、常见压缩与过滤器链以及 AES 内容/头部加密读取; TAR 族包括 TAR、TAR.GZ/TGZ、TAR.XZ/TXZ、TAR.BZ2/TBZ2 与 TAR.ZST/TZST. 页大小最大为 128; 宿主会持续取页直到 `complete`, 同时验证条目数、ID、父子关系、名称、类型、大小和分页游标. 插件不会把源描述符、缓存路径或真实档案内部路径暴露给宿主, 条目使用会话内不透明 ID.

回退缓存仍以“实际可用空间减 128 MiB 保留空间”为复制边界, 已知大小会在创建临时目录前预检, 未知大小会在复制过程中持续检查并使用溢出安全累加. 完成后再次核对宿主报告大小; 取消、读取失败、大小变化或空间不足会删除本次部分文件与目录. 活跃缓存目录登记为进程内租约, 不会被并发会话的过期清理误删; 未持有租约且超过 7 天的 `archive-input-*` 目录会在下次打开档案时回收.

目录索引只把严格通过路径策略的名称映射为正常虚拟路径. 父级穿越、绝对路径、驱动器前缀、控制字符、双向文本控制符或空名称等 `INVALID_PATH` 条目会被平铺到一个独立的只读隔离目录: 会话 ID 由安全序号路径生成, 与原始名称无关; 宿主只收到经可见转义的显示名. 后端数据可读时 `openEntry` 仍可通过不透明 ID 预览, 但条目能力始终拒绝写出. 隔离根会避开档案内的合法同名路径, 并作为普通目录层级进入宿主现有路径栏. 扫描器另有不可配置的进程结构上限 (250,000 个目录条目、500,000 个展开节点、65,536 字符路径与 1,024 层深度); 只有超过这些硬边界才拒绝建立索引, 较低的解压资源预算不会阻止浏览.

v6 的 `openEntry` 追加在 v5 AIDL 方法之后, 因此 v5 的 `getInfo`、`listChildren` 和 `close` 事务编号保持不变. `canOpenEntries` 缺失或为 `false` 时, 宿主继续提供 v5 浏览但不显示预览入口, 也不会调用新方法.

打开条目时, 插件根据不透明 ID 找回扫描快照中的普通文件, 重新打开对应后端并核对源文件身份、目录元数据、条目类型与声明大小; ZIP 继续核对压缩方法、加密状态和 CRC, 7Z 继续核对方法链、加密状态、声明大小和 CRC, TAR 继续核对可见头部校验和及条目元数据. 数据经可靠只读管道发送; 关闭会话会中止排队或进行中的管道. 宿主仅把支持现有主动作的文档、图片或媒体条目复制到自己的私有会话缓存, 并在发布缓存文件前核对准确字节数和可用空间. 文档沿用 8 MiB 限制, 图片和媒体目前分别使用 256 MiB 与 512 MiB 预览预算; 这些限制只影响预览, 不影响目录浏览.

会话生命周期与权限边界如下:

- 插件在打开会话时固定宿主调用 UID, 后续每次 Binder 调用都必须来自同一 UID;
- 宿主为档案浏览持有独立服务绑定租约, 不与动作目录发现或 Activity 启动复用;
- Binder 死亡、页面销毁、离开档案或显式关闭都会关闭会话;
- 插件关闭会话或服务销毁时关闭直读描述符租约, 或删除对应的回退缓存; 关闭操作幂等;
- 宿主的预览副本使用会话 ID 和条目 ID 的散列目录, 不把不可信名称用作目录路径; 离开档案时删除本会话副本, 并在新会话启动时清理过期目录;
- 条目管道保持只读且不提供真实档案路径; v6 仍没有档案写入、内部选择或任意路径访问接口.

## 当前档案输出事务

插件当前可创建 ZIP、7Z、TAR、TAR.GZ、TAR.XZ、TAR.BZ2 与 TAR.ZST. 所有格式使用同一工作流:

1. 宿主在共同父目录预留最终名称;
2. 宿主在同一目录创建隐藏的 `.autojs6-explorer-*.part` 暂存文件;
3. 插件通过只写描述符生成所选格式, 保留目录、空目录和可用的修改时间, 并核对已声明的源文件大小;
4. 成功关闭档案流后调用 `commitOutput`;
5. 宿主确认既有目标仍不存在, 再执行同目录原子改名;
6. 取消或失败时调用 `abortOutput`; 关闭会话也会清理尚未提交的事务.

当前默认冲突策略为自动编号, 例如 `Archive.zip`、`Archive (1).zip`. 现有目标不会被覆盖. ZIP 密码已经开放, 非空密码会生成 AES-256 加密条目; 创建加密 ZIP 时必须输入一致的确认密码, ZIP 中央目录文件名仍然可见. TAR 系列当前不支持密码. 分卷、文件名加密和压缩后删除源文件尚无完整事务后端, 因此界面明确显示为禁用状态.

同父级多选可启用"单独压缩每个文件和文件夹". 插件先按选择顺序固化一个单目标输出计划, 在表单中展示输出总数、最多 5 个派生名称和自动编号规则, 再通过同一宿主会话依次执行上述事务. 每个输出都以目标自身名称和所选格式的完整后缀命名; 既有名称或重复名称始终请求 `OUTPUT_CONFLICT_AUTO_RENAME`, 不会在前一项已经提交后再弹出逐项冲突询问.

Explorer Action v6 的每个 `commitOutput` 都独立发布最终文件, 不存在批次提交或撤销已提交输出的方法. 因此单独压缩不是伪装的全有或全无事务: 后续项目失败或取消时, 已提交压缩档案会保留, 当前临时输出仍按单项事务中止, 表单报告完成数/总数和失败输出, 关闭会话并禁止整批盲目重试. 若首个输出尚未提交就失败或取消, 则没有部分结果, 表单仍允许安全重试. 真正的批次原子语义仍需要宿主新增批次预留、统一提交和可恢复回滚协议.

仍需补齐的事务能力包括:

- 提交前重新打开输出并验证目录/大小/CRC;
- 插件进程异常死亡后的宿主超时与遗留 `.part` 文件回收;
- 询问/覆盖冲突策略和可恢复替换;
- 多输出的整体预留、统一提交或可恢复回滚 (当前单独压缩采用明确的独立提交与部分成功语义);
- 输出完成后的最小范围目录刷新事件;
- 只有完整验证成功后才可触发的源文件删除/回收站流程.

### 操作结果与目录刷新边界

当前 Explorer Action v6 的 Activity 请求没有操作结果或刷新范围字段, 宿主的只读动作启动器使用 `startActivity`, 而 `IExplorerActionHostSession` 只提供目录读取与输出事务方法, 没有请求刷新宿主页面的接口. `commitOutput` 返回的输出元数据也不构成页面刷新回执. 因此插件无法仅靠现有公开合同可靠地触发最小范围刷新; 当前不发送私有广播, 也不引用宿主内部类或 action 字符串.

后续协议应在保持旧版本解析的前提下定义结构化操作结果, 至少包含结果状态、受影响父目录的稳定标识与 URI、创建/删除/替换的目标标识, 以及宿主应采用的刷新范围. Activity 与绑定会话两种呈现方式应通过同一合同回传, 由宿主验证调用方、会话和目录归属后执行刷新.

### 提交前重开验证的最小协议扩展

已直接核对当前随插件编译的 `explorer-action-api.aar`: v6 `IExplorerActionHostSession` 只有 `prepareOutput`、只写 `openOutput`、`commitOutput` 和 `abortOutput`, 没有取得待提交临时文件只读描述符的方法. 只写描述符关闭后, 插件既不能可靠地从同一描述符读取中央目录, 也不应把整个大档案复制到另一份插件私有临时文件来冒充事务验证. 因此当前阶段保持源文件删除关闭, 并把提交前重开验证留作明确的宿主协议任务.

建议在后续 Explorer Action 版本中把下列方法追加到现有 AIDL 末尾, 保持已有事务编号不变:

```text
openPendingOutput(transactionId)
    -> read-only seekable ParcelFileDescriptor
```

合同要求如下:

- 仅原始请求绑定的插件 UID 可以读取该事务;
- 调用前插件必须关闭写描述符; 调用后宿主拒绝再次打开写描述符;
- 临时文件在验证结束前继续隐藏且最终名称不发布;
- 插件关闭只读描述符并完成目录、声明大小、实际数据与 CRC 校验后才能调用 `commitOutput`;
- 读取、验证、取消或 Binder 调用失败均调用 `abortOutput`, 由宿主清理同目录临时文件;
- 旧版宿主仍可维持现有创建能力, 但插件不得开放“压缩后删除源文件”, 也不得把关闭写流等同于完整验证.

## 路径栏与宿主原生档案页面

宿主的普通文件路径栏已经实现. v4 压缩请求复用它所在页面的父目录并传递规范显示路径; v5/v6 档案页面直接使用同一个路径栏和文件列表, 不创建插件私有导航界面.

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

档案名和内部祖先页面可点击跳转. 点击外部层级会先关闭档案会话, 再回到对应文件系统目录. 普通浏览状态下, 返回键先回到档案内部上一级, 到达根目录后再关闭会话并恢复进入前的文件系统页面与滚动状态.

档案目录当前复用宿主的列表、排序、搜索、下拉刷新、加载、错误、空状态、主题、暗色模式、动态色和系统栏. 目录项可以下钻; v6 可读取条目会按宿主原有类型规则使用文档、图片或媒体预览器, 不支持预览的类型仍明确不可用. 因协议尚未提供原生页面的选择写出事务和修改任务接口, 内部选择、条目菜单和浮动创建按钮继续禁用. `选择性解压...`、`解压到...` 与压缩表单仍由插件 Activity 呈现.

## 管理页选择性解压

文件菜单中的 `selective-extract` 动作以只读单文件请求打开管理页, 不会触发整包快捷解压. 档案完成索引后, 解压按钮根据当前位置提供真实可用的范围:

- “全部”始终选择档案根;
- “当前目录”只在档案内部可提取的非根目录显示, 并选择当前目录的完整子树; 危险名称的只读隔离目录不会伪装成可提取范围;
- “当前勾选”只在至少存在一个勾选路径时显示.

用户选定范围时, 页面立即把它固化为有序去重的档案路径集合. 后续密码解锁、危险路径确认、资源预算确认、系统目录选择和 Activity 重新扫描都继续使用同一集合, 不会退回当时可能已经变化的勾选状态. 文件菜单中的 `extract-to` 则保持快速路径: 校验档案后直接选择 SAF 输出目录并解压整个档案.

这两个动作目前都只获得档案 URI 的读授权. 请求中的 `parentUri` 是显示和一致性校验上下文, 宿主没有向插件授予父目录写权限, `IExplorerActionHostSession` 也没有随只读动作提供同级输出事务. 因此当前输出仍由 Android 系统目录选择器取得明确的 SAF 目录授权; 默认写入档案同级同名目录以及“当前文件系统目录”选项必须等待公共协议扩展, 不能由插件根据 URI 结构猜测或绕过授权实现.

### 输出名称冲突

管理页的冲突策略下拉框提供“每次询问”“跳过”“覆盖”和“自动重命名”, 默认逐项询问. 询问对话框显示档案路径、既有项目与待写入项目的名称和类型, 并允许把当前决定应用到全部兼容冲突. 文件/目录类型不一致或目标节点不属于本次解压时, 覆盖选项会明确禁用.

解压器先在新输出根内规划目录和文件节点, 再读取条目数据. 因此跳过的项目不会抬高实际进度总量, 自动重命名会在扩展名前添加编号, 覆盖只会复用本次任务已经创建的同类型节点. 固定“覆盖”策略遇到类型不一致时自动改用重命名, 不删除已经规划的目录树. 完成结果分别报告跳过、覆盖/合并和自动重命名的数量.

冲突策略不会把本次任务扩展为对既有目录的非事务式修改. 若系统选择目录下已经有同名输出根, 根目录仍自动编号, 原目录及其内容不会被读取、合并、覆盖或删除. 这个边界保证任何后续失败或取消都只需删除本次新建的根, 不会要求在通用 SAF provider 上备份并恢复用户原有内容.

## 后续可写档案 provider

下一阶段在 v6 只读列表和条目流基础上增加选择能力与任务接口:

```text
probe(target) -> DetectedFormat + FormatCapabilities
search(sessionId, query, page) -> entries + nextPage
capabilities(sessionId, selection) -> SelectionCapabilities
startExtraction(...)/startMutation(...) -> taskId
observeTask(taskId) -> progress/events
cancelTask(taskId)
```

每个条目需要独立的 `canOpen`/`canExtract`/`canDelete` 等能力与不可用原因. `entryId` 不能只由规范化路径生成, 因为真实档案可能包含重复名称、原始编码差异或同名文件/目录.

`ArchiveEntryPathStatus` 与后端 `ArchiveEntryCapabilities` 分开表达. `UNSAFE_ISOLATED` 不会撤销后端的只读 `canOpen`, 但有效 `canExtract` 必定为 `false`. 根目录或其他正常选择解析时会把这类条目放入 `skippedUnsafeEntries`, 不会把隔离目录或其虚拟序号写到目标. 管理/解压页必须展示数量并由用户选择“跳过并继续”; 解压器默认返回 `UNSAFE_PATH_CONFIRMATION_REQUIRED`, 只有调用方显式传入 `skipUnsafePaths` 才能继续写出其余安全条目. 仅选择危险条目时返回空安全选择, 不创建空输出根.

## 格式能力模型

插件已经通过 `ArchiveEngine` 统一扫描、预览、解压和创建链路. `ArchiveFormat` 提供格式标识、扩展名和 MIME 类型; `ArchiveReader`/`ArchiveWriter` 隔离具体库; `FormatCapabilities` 与 `ArchiveEntryCapabilities` 分别表达格式级和条目级真实能力. ZIP 字符集探测和底层目录对象只存在于 ZIP 后端内部.

ZIP 后端同时公布可用的文件名解码覆盖列表. 管理/解压 Activity 可在自动识别结果与 UTF-8/GB18030/Shift_JIS/EUC-KR/windows-1251/windows-1256/windows-1252/IBM437 之间切换, 每次切换都会重新索引同一暂存输入. 扫描快照保存最终选择, 预览流和解压器重新打开档案时必须复用它, 防止列表名称正确而实际写出时使用另一套编码. 当前 v6 只读会话尚无原位更新 reader 选项的方法; 宿主原生档案页的编码切换与一次性密码请求均需要后续协议扩展. 在此之前, 宿主原生页面仍可无密码浏览加密 ZIP 的目录, 解锁、预览和解压加密条目则进入管理/解压 Activity 完成.

ZIP reader 会在解析目录前识别标准 PKZIP 风格的 `.z01 + .zip` 分卷. 首卷通过分卷标记识别, 末卷通过 EOCD/Zip64 结构确认; 当前 v6 只向插件传递一个只读档案描述符, 没有枚举或打开同级伴随卷的合同, 所以 reader 不声明分卷读取能力. 当末卷单独打开时, 诊断返回 `INDEX/MISSING_VOLUME`, 根据受控显示名列出所需 `.z01`、`.z02` 等名称和最终 `.zip` 名称; 列表最多展开 8 项, 避免恶意卷数元数据造成无界分配. `.zip.001` 连续分片、分卷读取和分卷创建仍属于后续工作.

读取失败通过统一诊断模型区分输入、格式识别、目录索引、密码、条目数据、输出和清理阶段. 可公开的摘要只包含格式、阶段、稳定错误码及受控原因; 调试构建由用户主动复制的诊断才包含异常链和堆栈. `ExplorerActionService` 把相同安全摘要放入跨进程异常消息, 不传递插件缓存路径.

当前压缩表单的格式列表、压缩级别、密码、文件名加密和分卷控件由已注册 writer 的能力生成. ZIP 将密码声明为可选并提供显示/隐藏控件; TAR 系列声明不支持密码, 切换格式时会清空并禁用密码输入. 未压缩 TAR 只提供级别 0; GZIP、XZ、BZIP2 与 Zstandard 容器提供各自后端验证过的级别. 文件名加密和分卷继续分别声明为不支持. 动作目录的可读扩展名与 MIME 类型也来自同一格式注册表. 尚未实现的后端不会只因 Roadmap 中出现格式名称就进入菜单.

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

当前注册 ZIP、7Z 与 TAR 族后端. ZIP 支持识别/列表/预览/打开/解压/创建和可选密码, 读取传统 ZipCrypto 与 AES, 加密创建固定使用 AES-256; `jar`、`aar` 和 `war` 是扩展名别名. 标准 ZIP 分卷可以识别并在伴随卷不可用时给出精确诊断, 但能力表仍正确声明分卷读取和创建不受支持. 7Z 支持普通/solid 档案及 AES 内容/头部加密读取, 创建非 solid 输出并可选 AES-256 内容加密; 级别 0 使用 Copy, 1 至 9 使用 LZMA2, 文件名保持可见. TAR 族支持识别/列表/预览/打开/解压/创建, 包含未压缩 TAR 以及 GZIP/XZ/BZIP2/Zstandard 容器. TAR writer 使用 POSIX PAX 处理 UTF-8 与长路径, 不跟随源符号链接; 无法预先获得文件大小时会先测量再重新打开输入. Zstandard 使插件 APK 包含 `arm64-v8a`、`armeabi-v7a`、`x86` 与 `x86_64` 原生库, `PluginInfo.supportedAbis` 必须与该完整清单一致. 所有后端均不声明尚未实现的添加/删除/重命名/创建时文件名加密/分卷能力. 后端路线、APK/ABI/许可证门禁和测试要求见 [`docs/adr/0001-archive-engine-and-backend-strategy.md`](adr/0001-archive-engine-and-backend-strategy.md).

外部工具生成的兼容性样本、复现命令和 SHA-256 清单位于 [`compatibility`](../compatibility/README.md). 总样本矩阵只有在 Android、Windows 资源管理器、7-Zip、WinRAR、Info-ZIP、macOS Archive Utility 及 Java/Kotlin 工具链的对应样本均落地后才可标记完成.

## 不可取消的安全边界

固定输入大小和浏览阶段压缩比门槛可以调整或移除, 但以下边界不是兼容性限额, 不提供关闭开关:

- 目标根目录与规范路径约束;
- Zip Slip/绝对路径/驱动器路径和符号链接越界防护;
- 危险原始名称只读隔离、可见字符转义和写出前显式跳过确认;
- Binder 调用 UID 固定与最小 URI/文件描述符授权;
- 输出暂存、失败清理及不覆盖既有文件的事务规则;
- 档案内部路径冲突按原始安全路径精确判断, 允许合法的大小写与 Unicode 拼写变体继续浏览; 写入 SAF 目标时则按 NFC 与大小写折叠识别已有根名称并自动编号. 如果文档提供程序把新建文件或目录折叠到 writer 已创建或目标中既有的节点, 解压立即以输出失败中止并删除本次新建根目录, 不把创建操作降级为覆盖;
- 密码不写日志、不进状态 Bundle、不持久化;
- 密码使用可主动清零的字符缓冲; 扫描快照替换、创建任务结束或页面销毁时清零;
- 无密码时仍可列出加密 ZIP 的目录; 管理/解压页在打开或写出加密条目前请求密码, 错误密码保持选择并允许原位重试;
- 源文件删除前的完整输出验证.

管理/解压页提供可解释、可配置的兼容、严格和自定义资源预算. 兼容档位为 100,000 个选中输出项、16,384 字符路径、256 层深度、32 GiB 单项、256 GiB 总输出与 100,000:1 压缩比; 严格档位分别为 10,000、4,096、64、2 GiB、8 GiB 与 1,000:1. 自定义档位中, 结构维度填 `0` 表示使用相应的不可配置硬上限 (选中输出项使用 500,000 个展开节点上限), 大小和压缩比填 `0` 表示不设资源告警阈值.

资源预算仅影响写出决策, 不影响元数据浏览. 选择超出预算时, 页面在打开目标选择器之前列出预计输出大小、选中条目数和每个超限维度, 提醒通用 SAF 目标的可用空间无法可靠查询, 并要求本次操作的明确确认. 确认不会完全关闭实时保护: 单项/总字节和压缩比边界只扩展到当前选择的声明值; 未声明增长、源身份变化、大小/CRC 不一致、危险路径和失败清理仍按原规则中止.

## 解压进度与回滚结果

解压器通过同一 `ExtractionProgress` 模型报告 `PREPARING`、`EXTRACTING`、`CLEANING_UP`、`CLEANUP_FAILED` 和 `COMPLETED` 阶段. 进度包含已完成/总条目数、已写入/预计总字节数和当前条目路径. 管理页使用单调时钟计算任务平均传输速度与预计剩余时间; 总字节可用时展示确定进度, 否则按条目数估算. 为避免大量小条目或高速复制阻塞主线程, 页面只在经过 250 ms、增加 8 MiB 或完成 64 个条目时刷新一次, 但解压器仍在每个复制块和每个条目边界检查协程取消.

任务取消或写出失败后, 解压器进入 `NonCancellable` 清理区并只删除本次创建的输出根, 不碰触目标中原有内容. 如果 DocumentsProvider 拒绝删除或清理本身失败, 解压器同时保留类型化的 `ArchiveCleanupException` 并发出 `CLEANUP_FAILED` 进度事件; 后者不能仅依赖取消异常的 suppressed 链, 因为协程取消传播不保证该链能完整到达 UI. 事件携带完整、稳定的 `ArchiveOutputLocation`, 页面以有界且转义后的显示名和 URI 标识列出残留根, 让用户能够定位并手动处理.

普通文件写出完成后按 reader 声明校验实际写入大小和 CRC; 完成整个选择后还会复核源身份. 任一校验失败都会进入相同回滚链路. 这只完成插件侧的数据完整性与清理闭环; 当前只读 Explorer Action 通过普通 `startActivity` 启动, v5/v6 公共协议没有 Activity 结果或目录刷新回调, 因此插件不会发送私有广播冒充宿主刷新. 输出目录的可靠刷新仍需后续公共协议扩展.

## 兼容与发布边界

- 项目尚未公开发布, 因此直接使用 `archive-manager`、`io.github.supermonster003.autojs6.plugin.archivemanager` 和 Manager 类/资源名, 不保留旧命名别名.
- 当前插件的原生浏览与条目预览要求 Explorer Action v6, 压缩使用 v4 文件会话; 对应 AutoJs6 版本代码为 5276 或更高版本.
- 宿主仍可解析 v1-v4 插件目录, 但本插件不会发布旧动作、旧协议目录或旧 applicationId 的兼容入口.
- 后续协议字段必须保持显式版本与上限; 未知可选字段可以忽略, 未知必需能力必须明确拒绝.

具体任务与验收矩阵见 [`ROADMAP.md`](../ROADMAP.md).
