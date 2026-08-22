# 岁岁提醒（SuiSui Reminder）

一款**原生 Android** 的农历/公历生日与纪念日提醒应用：
- 添加**农历**或**公历**生日、纪念日、普通日程，自由选择
- 自动**多级提醒**：提前 1 个月 → 提前 1 周 → 提前 1 天 → 当天早晨，每年自动重复
- 提醒精度可选：**精确闹钟 / 普通通知 / 两者都要**（每个日程还可单独覆盖）
- 内置**农历换算**（1900–2100 年，纯算法、零第三方依赖、无需联网）
- **一键导入**：手机系统日历日程 + 联系人里填写的生日，批量转成提醒
- **记录美好瞬间**：照片 + 一句话，本地相册式浏览，可一键备份到云盘文件夹
- 所有数据保存在本机（Room 数据库），提醒由系统闹钟服务触发，开机/日期变化自动重排

---

## ✨ 功能亮点

- **农历/公历随心选**：生日、纪念日、普通日程，每条都能自由选择按公历还是农历记录
- **多级智能提醒**：提前 1 个月 → 提前 1 周 → 提前 1 天 → 当天早晨，每年自动重复；农历生日自动换算成当年的公历日期
- **提醒精度可选**：精确闹钟 / 普通通知 / 两者都要（全局设置 + 每条日程可单独覆盖）
- **内置农历引擎**：1900–2100 年纯算法换算，无需联网、零第三方依赖
- **一键导入**：手机系统日历里的日程、联系人里填写的生日，勾选即可批量转成每年提醒
- **记录美好瞬间**：照片 + 一句话，本地相册式浏览；一键备份到云盘文件夹（Google 云盘 / OneDrive / Dropbox 等）
- **本地私密**：数据全部保存在手机本机，不经过任何服务器

---

## 一、直接下载 APK 安装（推荐）

1. 打开本仓库的 **Releases** 页面：`https://github.com/<你的用户名>/<仓库名>/releases`
2. 找到最新版本（`v1.0.0` 等），下载 **`app-release.apk`**
3. 把 APK 传到手机（或用手机浏览器直接打开 Releases 页面下载）
4. 安装时如提示"未知来源"，按提示允许：**设置 → 安全 → 允许安装未知应用**
5. 首次打开会请求**通知权限**（Android 13+），请允许；想更准时的可再开"精确闹钟权限"

> 如果还没有 Release，手动触发构建：仓库 **Actions** 页 → **Build & Release APK** → **Run workflow**，完成后到该次运行的 **Artifacts** 里下载 `app-release-apk.zip`，解压即得 APK。

## 二、发布新版本（打标签自动构建 + 发布）

在仓库页面点击 **Create a new release**，填写标签如 `v1.0.1`，确认后 GitHub Actions 会自动构建并把 APK 附到该 Release 下，用户即可直接下载。

## 三、本地用 Android Studio 构建

1. 安装 [Android Studio](https://developer.android.com/studio)
2. 用 Android Studio 打开本目录（`File → Open`，选择含 `settings.gradle.kts` 的根目录）
3. 等待 Gradle 同步完成（需要联网下载依赖）
4. 点 **Build → Generate Signed App Bundle / APK → APK**，或直接点绿色 ▶ 运行到模拟器/真机

> 说明：仓库没有提交 `gradle-wrapper.jar`（二进制文件无法在此环境生成）。Android Studio 打开项目时会自动补全；若提示找不到 wrapper，先安装 Gradle 8.9 后在根目录执行一次 `gradle wrapper --gradle-version 8.9` 即可。

## 四、关于签名（重要）

- **未配置签名时**：Release APK 自动使用标准 debug 签名（`~/.android/debug.keystore`），可直接安装。注意**不同签名之间不能覆盖安装**，升级请卸载旧版或统一签名。
- **配置稳定签名（推荐，升级可覆盖安装）**：
  1. 本机生成密钥库：`keytool -genkeypair -v -keystore keystore.jks -keyalg RSA -keysize 2048 -validity 10000 -alias key0`
  2. 本地构建：在项目根目录创建 `keystore.properties`（已 gitignore）：
     ```
     storeFile=keystore.jks
     storePassword=你的密码
     keyAlias=key0
     keyPassword=你的密码
     ```
  3. CI 构建：把密钥库 Base64 与密码配为仓库 **Secrets**（Settings → Secrets and variables → Actions）：
     - `KEYSTORE_BASE64`：`base64 keystore.jks` 的输出
     - `KEYSTORE_PASSWORD`、`KEY_ALIAS`、`KEY_PASSWORD`

## 五、提醒机制说明

| 事项 | 说明 |
|---|---|
| 提醒时机 | 提前 1 个月、1 周、1 天、当天早晨（可在设置中分别开关、改时间） |
| 农历生日 | 每年换算成当年的公历日期后提醒（闰月生日只在出现对应闰月的年份提醒） |
| 精度选项 | 精确闹钟（`AlarmManager` 精确到分）；普通通知（非精确，更省电）；两者都要 |
| 重启/日期变化 | 开机、改时间、改时区、日期变化、应用更新后自动重排所有提醒 |
| 权限 | 通知权限（Android 13+ 必选）、精确闹钟权限（可选，App 内可一键引导）、读取日历（导入功能用） |

## 六、GitHub 上传与发布（初次部署）

**方式一：命令行（推荐）**

```bash
# 1. 在项目根目录（含 settings.gradle.kts 的目录）初始化并提交
git init
git add .
git commit -m "init: 岁岁提醒 v1.0.0"
git branch -M main

# 2. 关联远程仓库（先到 GitHub 新建空仓库，复制其地址）
git remote add origin https://github.com/<你的用户名>/suisui-reminder.git

# 3. 推送代码（GitHub Actions 会自动构建 APK 到 Artifacts）
git push -u origin main

# 4. 打标签，触发自动构建并把 APK 发布到 Releases
git tag v1.0.0
git push origin v1.0.0
```

**方式二：网页拖拽上传**

1. 打开 https://github.com/new 新建**公开**空仓库（名字如 `suisui-reminder`，**不要**勾选 “Add a README file”）
2. 进入仓库页 → **Add file → Upload files** → 把本项目所有文件拖入上传（注意不要上传 `keystore.properties`、`local.properties`、`build` 等本地文件）→ Commit changes
3. 之后发布新版本：仓库 → **Actions → Build & Release APK → Run workflow**，或直接打 tag

**发布后用户如何下载**：仓库 → **Releases** 页面 → 下载 `app-release.apk`（详见第一节）。

## 七、技术栈与结构

Kotlin 2.0 · Jetpack Compose (Material 3) · Room · DataStore · Coil · AlarmManager

```
app/src/main/java/com/suisui/reminder/
├── SuiSuiApp.kt               # Application：初始化仓库/调度器/通知渠道
├── MainActivity.kt            # 入口 + 通知权限请求
├── data/                      # Room 实体/DAO/仓库、DataStore 设置
├── lunar/LunarConverter.kt    # 内置农历换算（已验证，1900-2100）
├── reminder/                  # 提醒计算、AlarmManager 调度、广播、通知
├── calendar/                  # 系统日历读取与导入
└── ui/                        # Compose 界面：首页/新增编辑/详情/瞬间/设置/导入
```

## 八、常见问题

- **收不到提醒？** 先确认：① 通知权限已开；② 应用未被强行停止（Android 会拦截被"强行停止"的应用的闹钟）；③ 若选了"精确闹钟"但权限未开，会退化为普通通知，可到设置页一键引导开启。
- **升级装不上？** 不同签名不能覆盖安装，卸载旧版再装（或配置统一签名，见第四节）。
- **农历不准？** 农历算法覆盖 1900–2100 年，内置数据表经已知日期与自洽性校验。
