# AutoJs6 文件管理器集成说明

本文档同时记录两类内容:

- 已经落地并通过端到端验证的 Explorer Action v4 受控文件会话与 v5 只读档案会话;
- 为档案条目预览、按项解压和档案内修改预留的后续合同.

完成状态以 [`ROADMAP.md`](../ROADMAP.md) 为准. 文中标记为“当前”的能力可以在现有宿主与插件中使用; 标记为“后续”的内容仍不可当作已发布功能.

## 当前集成结果

AutoJs6 文件管理器现在可以从插件目录动态发现以下动作:

| 动作 ID | 目标 | 位置 | 访问范围 | 呈现方 |
| --- | --- | --- | --- | --- |
| `open-archive` | 单个受支持的档案文件 | 文件菜单/主动作 | 只读目标 | 宿主 Explorer |
| `extract-to` | 单个受支持的档案文件 | 文件菜单 | 只读目标 | 插件 Activity |
| `compress` | 单个普通文件或目录 | 文件/目录菜单 | 读取目标并在父目录创建输出 | 插件 Activity |
| `compress-selection` | 同一真实父目录中的多个文件、目录或混合目标 | 多选操作栏 | 读取目标并在共同父目录创建输出 | 插件 Activity |

动作是否出现由插件目录、目标类型、基数、扩展名/MIME、入口位置、插件启用状态和信任状态共同决定. 未安装或禁用插件时不会留下占位菜单; 虚拟目标或跨父目录多选也不会显示一个无法执行的压缩入口.

多选操作栏使用五个等宽单元格, 图标在上、文字在下. 当前尺寸为 22 dp 图标与 11 sp 文字, 已在 411 dp 等效窄屏上完成真实点击和截图验证. 320/360/600 dp、大字体、横屏和 RTL 仍属于 Roadmap 中的测试矩阵.

## Explorer Action v4/v5 目录

v4 在保留 v1-v3 解析能力的同时加入多目标与受控输出. v5 再加入呈现方式和只读档案会话:

| 字段 | 语义 |
| --- | --- |
| `protocolVersion` | 当前为 `5` |
| `targetKind` | `FILE`、`DIRECTORY` 或 `MIXED` |
| `cardinality` | `SINGLE` 或 `MULTIPLE` |
| `accessMode` | `READ_ONLY` 或 `CREATE_IN_PARENT` |
| `placements` | 主动作、单项溢出菜单或多选操作栏 |
| `presentation` | 插件 Activity 或宿主 Explorer |
| `extensions`/`mimeTypes` | 动作初筛条件; 不是最终格式判定 |
| `priority` | 多个插件动作同时匹配时的稳定排序依据 |

当前压缩动作使用通配 MIME/扩展名和 `FILE`/`DIRECTORY`/`MIXED` 目标, 因此能覆盖普通文件、目录和同父级混合多选. 打开与解压动作仍只匹配 ZIP/JAR/AAR/WAR.

每个目录字段都有数量或长度上限. 宿主会拒绝非法目标类型、基数、位置、授权和呈现方式组合, 而不是静默扩大插件权限. `HOST_EXPLORER` 只允许 v5 的 `FILE + SINGLE + READ_ONLY + PRIMARY` 动作, 不能借此获取目录写入或多选能力.

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

`ClipData` 只包含与 `targets[]` 顺序完全相同的目标 URI. 插件必须逐项核对 Bundle 与 `ClipData`; 不能根据 content URI 的字符串片段推断本地路径或父子关系. 共同父目录由宿主在构造请求前以规范路径验证, `parentUri` 仅用于上下文, 不作为任意目录读取授权.

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

目录页大小最大为 128. 相对路径、目标 ID、显示名、MIME 和输出名称均有协议上限. ZIP 创建器通过分页深度遍历目标, 只在需要时打开单个输入描述符, 并持续响应取消.

会话的权限边界如下:

- 每次 Binder 调用都固定到收到请求的插件 UID;
- 目标以不可猜测用途的稳定 ID 索引, 插件不能提交任意绝对路径;
- 相对路径不允许空段、`.`、`..`、NUL、绝对路径或越过目标根目录;
- 规范路径不一致的符号链接会标记为不可读, 打开时再次拒绝;
- 输出名称只能是安全的单个叶名称, 不能包含分隔符或控制字符;
- 输出只能创建在请求的共同父目录, 不授予其他目录写权限;
- 同名目标可选择失败或自动编号, 并由宿主进程全局预留以避免并发覆盖.

## v5 只读档案会话

`open-archive` 不再启动插件文件列表 Activity. 宿主重新验证动作匹配、插件信任与启用状态后, 以只读方式打开规范化的源文件, 再通过专用服务绑定调用:

```text
IExplorerActionPlugin.openArchive(sourcePfd, request)
    -> IExplorerArchiveSession

request
├─ displayName
└─ size                         # -1 表示未知

session.getInfo()
    -> sessionId + rootId + displayName + sourceSize + sourceLastModified

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

session.close()
```

当前 ZIP 实现把输入描述符暂存到插件私有缓存, 只扫描目录元数据并建立索引. 页大小最大为 128; 宿主会持续取页直到 `complete`, 同时验证条目数、ID、父子关系、名称、类型、大小和分页游标. 插件不会把缓存路径或真实档案内部路径暴露给宿主, 条目使用会话内不透明 ID.

会话生命周期与权限边界如下:

- 插件在打开会话时固定宿主调用 UID, 后续每次 Binder 调用都必须来自同一 UID;
- 宿主为档案浏览持有独立服务绑定租约, 不与动作目录发现或 Activity 启动复用;
- Binder 死亡、页面销毁、离开档案或显式关闭都会关闭会话;
- 插件关闭会话或服务销毁时删除对应暂存输入, 关闭操作幂等;
- 宿主只获得列表元数据; v5 当前没有条目流、写入或任意路径访问接口.

## 当前 ZIP 输出事务

插件当前只提供 ZIP 创建. 工作流为:

1. 宿主在共同父目录预留最终名称;
2. 宿主在同一目录创建隐藏的 `.autojs6-explorer-*.part` 暂存文件;
3. 插件通过只写描述符生成 ZIP, 保留目录、空目录和修改时间;
4. 成功关闭 ZIP 后调用 `commitOutput`;
5. 宿主确认既有目标仍不存在, 再执行同目录原子改名;
6. 取消或失败时调用 `abortOutput`; 关闭会话也会清理尚未提交的事务.

当前默认冲突策略为自动编号, 例如 `Archive.zip`、`Archive (1).zip`. 现有目标不会被覆盖. 密码、分卷、文件名加密、分别压缩和压缩后删除源文件尚无完整事务后端, 因此界面明确显示为禁用状态.

仍需补齐的事务能力包括:

- 提交前重新打开输出并验证目录/大小/CRC;
- 插件进程异常死亡后的宿主超时与遗留 `.part` 文件回收;
- 询问/覆盖冲突策略和可恢复替换;
- 多输出的整体提交或回滚;
- 输出完成后的最小范围目录刷新事件;
- 只有完整验证成功后才可触发的源文件删除/回收站流程.

## 路径栏与宿主原生档案页面

宿主的普通文件路径栏已经实现. v4 压缩请求复用它所在页面的父目录并传递规范显示路径; v5 档案页面直接使用同一个路径栏和文件列表, 不创建插件私有导航界面.

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

档案目录当前复用宿主的列表、排序、搜索、下拉刷新、加载、错误、空状态、主题、暗色模式、动态色和系统栏. 目录项可以下钻; 因 v5 尚未提供条目流和任务接口, 文件预览、内部选择、条目菜单和浮动创建按钮会明确不可用. `解压到...` 与压缩表单仍由插件 Activity 呈现.

## 后续可写档案 provider

下一阶段在 v5 只读列表基础上增加条目流、选择能力和任务接口:

```text
probe(target) -> DetectedFormat + FormatCapabilities
search(sessionId, query, page) -> entries + nextPage
openEntry(sessionId, entryId) -> read-only descriptor/stream
capabilities(sessionId, selection) -> SelectionCapabilities
startExtraction(...)/startMutation(...) -> taskId
observeTask(taskId) -> progress/events
cancelTask(taskId)
```

每个条目需要独立的 `canOpen`/`canExtract`/`canDelete` 等能力与不可用原因. `entryId` 不能只由规范化路径生成, 因为真实档案可能包含重复名称、原始编码差异或同名文件/目录.

## 格式能力模型

压缩和修改表单最终必须由“真实格式 + 当前后端 + 当前选择”共同生成:

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

## 不可取消的安全边界

固定输入大小和浏览阶段压缩比门槛可以调整或移除, 但以下边界不是兼容性限额, 不应提供关闭开关:

- 目标根目录与规范路径约束;
- Zip Slip/绝对路径/驱动器路径和符号链接越界防护;
- Binder 调用 UID 固定与最小 URI/文件描述符授权;
- 输出暂存、失败清理及不覆盖既有文件的事务规则;
- 密码不写日志、不进状态 Bundle、不持久化;
- 源文件删除前的完整输出验证.

资源预算应改为可解释、可配置的兼容/严格/高级档位. 超出预算时优先允许只读浏览, 在真正写出前再展示预计空间与风险; 不能为了取消固定阈值而削弱路径隔离或事务完整性.

## 兼容与发布边界

- 项目尚未公开发布, 因此直接使用 `archive-manager`、`io.github.supermonster003.autojs6.plugin.archivemanager` 和 Manager 类/资源名, 不保留 Browser 别名.
- 当前插件的原生浏览要求 Explorer Action v5, 压缩使用 v4 文件会话; 对应 AutoJs6 版本代码为 5276 或更高版本.
- 宿主仍可解析 v1-v4 插件目录, 但本插件不会发布旧动作、旧协议目录或旧 applicationId 的兼容入口.
- 后续协议字段必须保持显式版本与上限; 未知可选字段可以忽略, 未知必需能力必须明确拒绝.

具体任务与验收矩阵见 [`ROADMAP.md`](../ROADMAP.md).
