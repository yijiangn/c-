# 学习记录

仅供个人使用的原生 Android 学习任务与计时 App。项目使用 Kotlin、Jetpack Compose、Material 3、Room 和 MVVM，最低支持 Android 9（API 28）。

## 功能

- 今日任务：添加、编辑、删除、完成、按状态/科目筛选，长按任务管理。
- 科目分类：5 个内置科目，自定义科目、颜色、重命名与安全删除。
- 学习计时：正计时、番茄钟、暂停/继续、提前结束、任务关联、前台通知。
- 日历记录：月历学习圆点、每日任务、总时长、历史补记与修改。
- 学习统计：今日/本周/本月、7 天柱状图、科目排行、完成率、连续学习天数。
- 学习目标：每日/每周目标进度与完成提示。
- 数据管理：JSON 完整备份、覆盖/合并恢复、CSV 导出、双重确认清除。
- 主题：浅色、深色、跟随系统。

App 未声明 `INTERNET` 权限，不含登录、广告、分析 SDK 或云端接口。数据仅存储在本机 Room 数据库中。文件导入导出使用 Android 系统文件选择器，不申请通用存储权限。

## 项目结构

```text
StudyRecord/
├── app/
│   ├── build.gradle.kts
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/yanzu/studyrecord/
│       │   │   ├── MainActivity.kt
│       │   │   ├── AppViewModel.kt
│       │   │   ├── StudyRecordApplication.kt
│       │   │   ├── data/          # Room 实体、DAO、数据库、Repository、备份
│       │   │   ├── service/       # 前台计时服务
│       │   │   ├── ui/            # 计时弹窗、主题、四个主页面
│       │   │   └── util/          # 日期与统计计算
│       │   └── res/
│       └── test/                   # 本地单元测试
├── gradle/wrapper/
├── build.gradle.kts
├── settings.gradle.kts
├── gradlew
└── gradlew.bat
```

## Android Studio 打开和运行

1. 安装 Android Studio，确保已安装 Android SDK 35 和 JDK 17（Android Studio 自带的 JDK 即可）。
2. 选择“打开”，选中本项目根目录 `StudyRecord`。
3. 等待 Gradle Sync 下载依赖并完成索引。
4. 连接 Android 9 或更高版本的手机并开启 USB 调试，或创建模拟器。
5. 选择 `app` 配置，点击“运行”。

首次启动学习计时时，Android 13 及更高版本会询问通知权限。拒绝后任务和计时仍可使用，只是不显示计时通知。

## 构建 APK

### Android Studio

选择：`Build` → `Build Bundle(s) / APK(s)` → `Build APK(s)`。

生成位置：

```text
app/build/outputs/apk/debug/app-debug.apk
```

### 命令行

Windows：

```powershell
.\gradlew.bat clean testDebugUnitTest assembleDebug
```

macOS / Linux：

```bash
./gradlew clean testDebugUnitTest assembleDebug
```

## 关键实现说明

- 计时状态、开始时间和暂停累计时长写入 `UserSettings` 表。显示时长根据系统时间与开始时间实时计算，不依赖页面定时器累计，因此切后台或锁屏不会造成时长漂移。
- 计时运行时使用前台服务显示低打扰通知；服务被系统重启后会从 Room 恢复状态。
- `StudySession.taskId` 外键使用 `ON DELETE SET NULL`。删除任务不会删除历史学习记录。
- 任务“实际学习时长”由计时记录和手动记录同步维护；统计只读取大于 0 分钟的真实 `StudySession`。
- JSON 恢复会验证应用标识、版本、必需字段、外键、日期、枚举和时长后才写入数据库。
- 覆盖恢复和清除全部数据均使用 Room 事务，避免只写入一半。

## 测试

运行：

```bash
./gradlew testDebugUnitTest
```

单元测试覆盖：每日/周/月时长、本周完成率、科目累计时长和连续学习天数。完整的人工验收清单见 [TESTING.md](TESTING.md)。
