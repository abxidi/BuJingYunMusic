# NovaPulse Android Design

这是一套面向 Android Studio 的设计稿资源，保留原 Web 稿的核心产品结构：

- 全屏 MP3 播放器作为主界面。
- 曲库、收藏、手机目录、设置收进左侧菜单。
- 播放控制行中，随机/循环模式按钮在最左侧，收藏按钮在最右侧。
- 曲库歌曲可收藏，收藏列表可作为优先播放队列。
- 手机音乐目录建议落地时使用 MediaStore 读取公共音频库，或用 Storage Access Framework 让用户选择目录。

## 文件结构

```text
android-design/
  res/
    layout/activity_main.xml
    drawable/*.xml
    values/colors.xml
    values/strings.xml
```

## Android Studio 使用方式

1. 将 `res` 目录复制到 Android 项目的 `app/src/main/res`。
2. 布局依赖 `androidx.drawerlayout:drawerlayout`。
3. 播放、扫描、收藏数据逻辑可接入 ViewModel：
   - `btnMode`：切换随机、循环、单曲循环。
   - `btnFavorite`：收藏或取消收藏当前音乐。
   - `drawerRoot`：打开或关闭左侧菜单。
   - `btnPickFolder`：触发目录选择或申请音频读取权限。

