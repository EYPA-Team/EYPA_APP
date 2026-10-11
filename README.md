# EYPA Android

EYPA 安卓客户端，支持文章、论坛、用户资料、消息、图片与音视频浏览。

当前版本：**v0.6.1011B**（Beta），内部版本号 `9`。安装包见 [GitHub Releases](https://github.com/EYPA-Team/EYPA_APP/releases/tag/v0.6.1011B)。

## 本版改动

- 接入站点现行媒体鉴权，图片、视频和音乐使用服务器签发的访问凭证。
- 附件下载及保存图片使用独立的一次性下载票据，遵守网站权限和下载配额。
- 恢复图片缓存，按当前登录身份隔离；滚动列表可复用已加载图片。
- 修复 `//cdn...` 头像地址、SVG 解码器误读位图，以及占位图替换后的缩放问题。
- 保留仓库已有的论坛与消息功能，将其图片加载也接入同一缓存及鉴权逻辑。

## 安装与签名迁移

需要 Android 8.1（API 27）或更高版本。

**v0.5.0406B 的原签名密钥已丢失，v0.6.1011B 开始使用新发布签名。旧版用户需要先卸载旧版再安装新版，卸载会清除本地设置、浏览记录及登录状态。** 请先保存需要保留的本地信息。后续版本继续使用新发布签名，可覆盖安装。

## 版本规则

格式为 `v0.Y.MMDD`，测试版在末尾加阶段标识：`B` 为 Beta，`A` 为 Alpha，正式版没有后缀。年份递进值在 2025 年为 `5`，2026 年为 `6`；`MMDD` 为发布日期。Android `versionCode` 每次发布独立递增。

## 构建

环境：JDK 17 或 21、Android SDK 34、Gradle Wrapper 8.7。通过 Android Studio 配置 SDK，或在不提交的 `local.properties` 中设置 `sdk.dir`。

```sh
./gradlew :app:assembleRelease :app:testDebugUnitTest :app:lintRelease
```

Windows 使用 `gradlew.bat`。Release 构建默认输出未签名 APK；发布前使用保存在仓库外的发布密钥签名，并使用 `apksigner verify` 校验。密钥、密码、本地 SDK 配置和构建产物不提交到仓库。

## 媒体服务

客户端通过 `eqmemory.cn` 的现有 APP API 登录，并使用 `/eu-json/app/v1/media/config` 和 `/eu-json/app/v1/media/grant` 获取媒体配置与授权。站点服务端完成文件归属、文章权限、隐藏/付费内容、通行证及配额检查；客户端不持有媒体签名密钥。

图片相关设备测试位于 `MediaImagesInstrumentedTest`。其中真实站点验证需要显式传入 `eypa_live_images=true`。在已有开发设备上验证时，使用保留数据的安装与 instrumentation 流程。
