# ImgBed Android

ImgBed 的原生 Android 客户端。应用以本地优先方式处理图片，压缩结果和普通文件通过 ImgBed API 上传；远程文件管理直接使用现有管理 API。

## 当前功能

- 从系统文件选择器选择任意类型文件，也可通过 Android 分享菜单接收文件。
- 图片本地转码：JPEG、PNG、WebP 有损、WebP 无损、AVIF。
- 可配置质量、最长边、目标体积和“结果更大时保留原文件”。
- GIF、SVG 与动画 WebP 默认原样上传，避免静态编码器破坏动画或矢量内容。
- 可选择 Telegram、Cloudflare R2、S3、Discord、Hugging Face、WebDAV 渠道。
- 原文件名、时间戳、UUID、模板、服务端随机命名。
- 模板变量：`{name}`、`{ext}`、`{yyyy}`、`{MM}`、`{dd}`、`{HH}`、`{mm}`、`{ss}`、`{uuid}`、`{index}`。
- 上传进度、结果链接复制与分享。
- 远程目录浏览、搜索、删除、重命名和移动。
- API Token 使用 AndroidKeyStore 加密，存放在 `noBackupFilesDir`，不会进入 Android 备份。

## API Token

仅上传需要 `upload` 权限。应用内文件管理还需要：

- `list`：浏览和搜索文件；
- `delete`：删除文件；
- `manage`：重命名和移动文件。

不要把管理员密码或 Token 写入源码、APK 资源或 CI 配置。

## 图片处理

JPEG、PNG 与 WebP 使用 Android Bitmap 编码器。AVIF 使用 [awxkee/avif-coder](https://github.com/awxkee/avif-coder)，编码在设备本地完成。Telegram 渠道默认附加 `serverCompress=false`，避免服务端再次压缩本地结果。

“目标体积”会逐步降低有损质量；到达最低质量后继续等比缩小图片。PNG 和 WebP 无损只能通过缩小尺寸降低体积。

## 构建

构建与测试统一在 GitHub Actions 执行。推送代码后，工作流使用 JDK 17 和 Android SDK 36 运行：

```sh
./gradlew testDebugUnitTest assembleDebug
```

在 [Android Actions](https://github.com/LuoH-AN/imgbed-android/actions/workflows/android.yml) 下载 `imgbed-debug` 构建产物；推送 `v` 开头的标签会发布 APK 到 GitHub Release。

## 界面设计

布局参考 [lyrics-android](https://github.com/LuoH-AN/lyrics-android) 的大标题和分组设置视觉，使用独立编写的 Compose 组件：

- 森林灰绿主题，跟随系统切换浅色和深色。
- 随滚动收起的大标题，24dp 外圆角、5dp 内圆角的设置分组。
- 悬浮胶囊导航、选中状态过渡、页面淡入淡出，以及页面滚动位置保留。
- 上传主视觉、处理方案快捷入口、传输统计和状态队列。
- 文件搜索、分类筛选、时间／名称排序、目录导航及明确的加载／错误状态。
- 设置按服务连接、上传偏好、图片处理、文件命名拆分详情页。

## 项目结构

- `compression/ImageCompressor.kt`：采样解码、方向处理、缩放和多格式编码。
- `compression/NamingEngine.kt`：上传文件命名规则。
- `network/ImgBedClient.kt`：上传和文件管理 API。
- `data/AppPreferences.kt`：设置与加密 Token。
- `ui/AppViewModel.kt`：上传队列和远程文件状态。
- `ui/ImgBedApp.kt`：页面切换与悬浮导航。
- `ui/ImgBedTheme.kt`、`ui/UiComponents.kt`：主题、滚动标题与共享组件。
- `ui/UploadScreen.kt`、`ui/RemoteFilesScreen.kt`、`ui/SettingsScreen.kt`：上传、文件与设置页面。

## 下一步

- WorkManager 后台上传和系统通知；
- 多选远程文件与批量操作；
- 图片压缩前后预览和滑动对比；
- 动图压缩、保留 EXIF 的可选策略；
- 本地上传历史数据库与失败队列恢复。
