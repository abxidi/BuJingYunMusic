# NovaPulse MP3 Android App

这是按当前设计稿开发的 Android 原生 MP3 播放器工程。

## 已实现

- 全屏科幻播放器首页。
- 左侧菜单：曲库、收藏、手机目录、设置。
- 播放控制：随机/循环/单曲循环、上一首、播放/暂停、下一首、收藏。
- 读取手机音频：通过 `MediaStore` 扫描本机音频。
- 目录设置：通过系统目录选择器保存用户选择的目录 URI。
- 收藏：支持曲库中收藏/取消收藏，并同步到收藏列表。
- 播放：使用 Android `MediaPlayer` 播放扫描到的本地音频 URI。

## 工程结构

```text
app/
  build.gradle
  src/main/
    AndroidManifest.xml
    java/com/novapulse/mp3/MainActivity.java
    res/layout/activity_main.xml
    res/drawable/*.xml
    res/values/*.xml
```

## 构建运行

1. 用 Android Studio 打开当前目录。
2. 等待 Android Gradle Plugin 同步。
3. 连接 Android 手机或启动模拟器。
4. 运行 `app`。
5. 首次进入时授权音频读取权限。

## 已打包 APK

已使用 Android SDK 工具链手工构建并签名 debug APK：

```text
dist/NovaPulse-debug.apk
```

签名验证通过 v1、v2、v3。
