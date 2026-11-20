3-Stack Archive 为 AutoJs6 文件管理器加入压缩档案功能: 像浏览文件夹一样查看 ZIP, 7Z, RAR 与 TAR 系列档案, 解压全部或部分内容, 也可以把文件和文件夹压缩为档案. 插件首页可以单独打开档案进行浏览和解压.

### 开始使用

1. 在 "插件中心" 启用 3-Stack Archive.
2. 在 AutoJs6 文件管理器中打开压缩档案的菜单, 选择 "查看档案"; 档案内容显示在同一个文件列表中.
3. 进入文件夹, 搜索, 或点按条目用 AutoJs6 的预览器打开; 返回键先回到档案内的上一级.
4. 需要取出文件时, 从档案的菜单选择 "解压到..." 解压整个档案, 或长按条目多选后只解压勾选的部分.

### 主要功能

- **浏览与预览**: 在 AutoJs6 的文件列表中浏览 ZIP, JAR, 7Z, RAR 与 TAR 系列档案, 可以搜索和用路径栏跳转; 文档, 图片, 音频和视频可以直接预览, 无需先解压.
- **作为压缩档案打开**: 文件名未被识别时, 从文件菜单选择 "作为压缩档案打开...", 按文件内容识别格式, 路径栏会标出实际格式.
- **解压**: 解压整个档案, 当前目录 (路径栏右侧的按钮) 或勾选的条目, 显示进度并可取消; 遇到同名文件时可以跳过, 覆盖或自动编号.
- **密码与文件名编码**: 加密的 ZIP, 7Z 与 RAR 在打开或解压时弹出密码框, 密码错误可以直接重试; ZIP 文件名乱码时可在路径栏切换编码.
- **分卷档案**: 完整的 `.z01` + `.zip`, `partN.rar`, `.zip.001` 与 `.7z.001` 分卷组可以浏览, 预览和解压, 但不能修改.
- **压缩**: 对文件, 文件夹或同一文件夹中多选的项目选择 "压缩...", 创建 ZIP, 7Z, TAR, TAR.GZ 等档案; ZIP 与 7Z 可设置密码, ZIP 可按大小分卷.
- **逐项压缩**: 多选时开启 "单独压缩每个文件和文件夹", 为每个项目分别生成一个档案, 名称冲突时自动编号.
- **修改档案**: 普通单卷 ZIP, 未加密的非 solid 7Z 与 TAR 系列档案可以添加文件和文件夹, 新建文件夹, 重命名和删除条目; 修改成功后可以 "恢复上一版本" 一次.
- **源项目移入回收站**: 开启默认关闭的 "压缩完成后将源项目移入回收站" 后, 源项目在全部输出校验完成后才移入 AutoJs6 回收站, 可从压缩页菜单中的 "源项目恢复记录" 恢复.
- **独立首页**: 从启动器打开插件, 点按 "打开压缩档案" 即可浏览和解压档案; "设置" 中可以调整语言, 夜间模式, 主题色与启动器图标.

### 在脚本中使用

插件工具 `archive_list`, `archive_read` 与 `archive_extract` 用于在脚本中列出档案内容, 读取其中的文本文件和解压档案:

```js
let list = plugins.tools.call('archive_list', { archive: './demo.zip' });
list.result.entries.forEach(e => console.log(e.path, e.size));

let readme = plugins.tools.call('archive_read', { archive: './demo.zip', path: 'README.md' });
console.log(readme.result.text);

let extracted = plugins.tools.call('archive_extract', { archive: './demo.zip' }, {
    outputs: { files: './out/demo' },
});
console.log(extracted.result.filesExtracted, extracted.result.kept);
```

- 相对路径以 AutoJs6 的工作目录为准; 解压到的文件夹可以已存在, 其中已有的文件保持不变, 数量记在 `kept` 中.
- `archive_read` 只读取 UTF-8 文本, 每次最多 64 KiB; 二进制文件不返回文本, `binary` 为 `true`.
- 加密档案用 `password` 参数传入密码; 分卷档案不能通过插件工具处理.

### 运行要求

- Android 7.0 或更高版本.
- AutoJs6 6.8.0 或更高版本, 且本插件在 "插件中心" 中处于启用状态; 插件首页可以不经 AutoJs6 单独使用.
- 插件不申请存储与网络权限, 只处理你在 AutoJs6 或系统文件选择器中选择的文件.

### 常见问题

**菜单中没有本插件动作时的处理**

确认本插件已在 "插件中心" 中启用. 部分 AutoJs6 版本把插件动作放在以插件命名的子菜单 "3-Stack Archive" 中; 文件名未被识别的档案请使用 "作为压缩档案打开...".

**档案只能查看, 不能修改的原因**

RAR, 分卷档案, JAR/AAR/WAR, 以及加密或 solid 的 7Z 只能浏览和解压. "管理压缩档案..." 会列出当前档案的格式与只读原因.

**加密档案中的文件名**

ZIP 与 7Z 加密的是文件内容, 档案中的文件名仍然可见, 因此敏感信息不宜写进文件名.

**分卷档案无法打开时的处理**

标准分卷 ZIP 从最后的 `.zip` 打开, WinRAR 分卷从第一个 `partN.rar` 打开, 编号分卷从 `.001` 打开; 全部分卷须放在同一个文件夹中.

插件工具的全部参数与返回值见 [AutoJs6 文档 > 插件](https://docs.autojs6.com/#/plugins), 其他说明见 [项目主页](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stack-Archive).
