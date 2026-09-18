# BOSS 求职助手 Android

本项目以用户提供的《BOSS直聘 AI 自动求职助手需求说明》全部 55 章为最终验收基准。当前版本 **0.1.0 是 Android 工程验证版**。可以安装、录入资料、导入简历、配置 AI、分析岗位、生成回复并保存本地记录；真实 BOSS 控件适配和自动发送闭环尚未完成手机验收，不能当成已经完成的全自动产品。

完整状态见 [逐项需求追踪](docs/REQUIREMENTS.md)、[上游实跑报告](docs/UPSTREAM_REVIEW.md)、[隐私说明](docs/PRIVACY.md)、[测试报告](docs/TEST_REPORT.md)。

## 安装与首次使用

1. 将交付的 `BossAssistant-0.1.0.apk` 下载到 Android 8.0 或更高版本手机，在系统安装页面确认安装。只需 APK，不需要在手机安装 Python、Node、ADB 或 Android Studio。
2. 打开“求职助手”。每次完整重启都会进入 Key 页面。只在你自己的手机中输入 DeepSeek Key，点击“测试连接并进入”；测试会产生一次很小的 API 调用。
3. 进入“资料”，填写真实信息，留空视为未知。可以先不输入 Key，使用“先配置资料与求职条件”。不要向聊天中的开发者提供 Key、Cookie、Session 或私人简历。
4. 进入“简历”导入 PDF、DOCX 或 TXT。最多 5 MB、PDF 最多 30 页；扫描图片型 PDF 暂不能识别。核对提取文字后点击确认，未确认版本不用于 AI 判断。支持多个版本和默认简历。
5. 在设置中填写目标岗位、城市、月薪范围、学历/经验、实习/全职、关键词、黑名单、评分门槛、每日投递与 AI 上限。
6. 在“我的 HR 常见问题”填入并授权真实答案，例如到岗时间、薪资期望、加班安排。未知问题会转为自然拖延及本人提醒，不由模型编造答案。
7. 在 BOSS 官方 APP 手动登录。助手不索取 BOSS 密码、不获取其登录会话。需要临时登录时，结束使用后到 BOSS 官方 APP 手动退出。
8. 当前工程版需要完成 BOSS 控件绑定，再用测试模式核对岗位/双方消息是否读取正确。若只是先验证 AI，可手动录入一个真实岗位 JD 或带完整上下文的 HR 问题。

华为设备需要实际支持 Android APK。若使用 Android 容器，助手与 BOSS 必须位于能互相访问无障碍控件的同一个 Android 环境。原生鸿蒙 APP 与 Android 容器间不保证可互相操作；此项尚未验证。

## 模式与停止

- **测试模式（首次安装默认）**：读取、筛选、分析和生成草稿；不会实际投递或发送。先核对真实资料、识别结果与生成内容。
- **半自动模式**：在助手内确认指定岗位或回复，60 秒内打开对应 BOSS 页面；发送前再次核对身份、消息、输入框、权限和状态。输入框已有文字时暂停。
- **自动模式**：只对经过核对的可见页面按规则操作；第一版仍需人工确认首次沟通结果，不支持已验收的全自动附件发送/所有会话巡检。请在真实手机验收通过后使用。
- 顶部“立即停止”立即使已排队的搜索和发送授权失效；“暂停”保留资料。通知栏也提供两者。退出助手会清除内存 Key。

首次“立即沟通”可能发送 BOSS 官方默认招呼，设置中必须先明确允许。AI 生成的个性化招呼目前作为草稿显示；不要误以为这个草稿必然就是 BOSS 首次发出的内容。

## HR 回复、接管和学习

第一次捕获会话后，请补齐并核对 HR、公司、岗位 JD 与此前聊天。AI 会结合已经记录的历史、资料、简历、偏好及 FAQ 选择真实答案；不能自动读取到的历史需要你补充。

未知答案会生成短暂的自然回应并提醒你。已拖延一次的会话进入“等待本人处理”；追问不会触发无休止的重复拖延。你可以补充实际答案、保存草稿，再核对发送；勾选授权后，成功确认发送才保存为 FAQ。人工接管期间停止此会话自动发送；检测到你在 BOSS 输入时也会暂停。

身份证、银行卡、验证码、密码等高敏感问题不会自动发送真实资料。简历附件与联系方式等当前需要到 BOSS 官方 APP 本人处理。

## API 配置和费用

默认 `https://api.deepseek.com`、`deepseek-flash`，使用 `/chat/completions` 的 OpenAI Compatible JSON 接口。模型名称以 [DeepSeek 官方文档](https://api-docs.deepseek.com/zh-cn/) 为准，可以在设置修改。更改服务地址会清除旧 Key，需要重新输入并确认服务商。

Key 不保存到数据库、配置或备份，进程结束即消失；关闭任务后重新打开，即使 Android 仍保留进程，也会清除旧 Key 和发送授权。进入后台超过 30 分钟自动清除。系统杀后台后不会自动恢复发送。

调用次数在发请求前占用额度，失败也计数；记录服务返回的 token 用量，缺失时估计。费用按你填入的单价估算，初始单价为 0 表示未配置，不表示 API 免费；token/费用上限属于估计控制，实际账单以服务商为准。

## 备份和清除

“设置 → 加密备份与隐私清除”可导出/导入资料、求职设置、FAQ。密码至少 10 字符，忘记无法恢复；备份没有 API Key、BOSS 会话、聊天或简历附件。导入后首次沟通授权保持关闭。

清除聊天或岗位时保留发送去重记录。清除全部本地数据会删除这些指纹，之后要人工核对过去的投递；已经导出的备份由你在导出位置自行删除。BOSS 登录状态只能在官方 APP 中退出。

## 常见故障

| 现象 | 处理 |
| --- | --- |
| 重开后要求输入 Key | 正常安全行为，Key 不永久保存 |
| Key 无效、余额不足、请求太频繁 | 在服务商账户核对；可更换 Key 或稍后重试，日志不含原始 Key |
| 已达到今日 AI/投递上限 | 停止对应操作；不要重复点击尝试突破限制 |
| 等待验证码或 BOSS 登录失效 | 到 BOSS 本人完成验证/登录，再明确继续 |
| BOSS 页面需重新适配 | 暂停，核对 BOSS 版本及控件；不尝试坐标乱点或绕过验证 |
| 发送结果不确定 | 到 BOSS 核对后记录；系统不会自动重试 UNKNOWN 任务 |
| 手机锁屏或切换 APP | 暂停点击；打开 BOSS 后继续。前台服务无法绕过系统屏幕限制 |
| 系统结束后台 | 重新打开并输入 Key；账本保留，发送不会自动恢复 |
| 简历提取为空 | 使用文字型 PDF/DOCX/TXT，核对提取结果 |
| 本地数据无法解密 | 不会覆盖损坏数据；保留原文件，或明确选择清除全部 |

通知与无障碍均可在系统设置撤销。没有短信、通讯录、定位、录音、相机或全部文件访问权限。

## 开发方向

原生 Android（Java 17，Android 8.0 起），独立 BOSS 无障碍适配层，DeepSeek 默认、兼容 OpenAI 格式。用户通过 BOSS 官方 APP 手动登录；助手不接触其密码、Cookie 或 Session。官方 APP 的登录持久性由 BOSS 管理，助手不能替另一 APP 清除登录状态。

API Key 仅存进程内存；完全重启后重新输入，后台超过 30 分钟清除。默认测试模式不发送消息和投递。发送前先保存 UNKNOWN 任务；无法确定发送结果时暂停，避免崩溃后的重复发送。验证码、安全验证和平台限制一律停止并交给本人。

## 上游

- 首选：[boss-zhipin-bot](https://github.com/as161233574-alt/boss-zhipin-bot)，检查提交 `4f62a113de42fa3e8b14d243b4f7d2de45dfd732`。移植 HR 活跃度规则；岗位去重概念改为 SHA-256（岗位 ID 优先、公司/岗位/城市/HR 后备），保留 MIT 许可。
- 参考：[ai-job](https://github.com/yangfeng20/ai-job)，检查提交 `ef84db4bdee2f7eef84093bfa684ffe9dcfe6e04`。参考任务分层；不复制私有接口、反检测、登录和支付服务。

桌面 Python/Playwright、Tampermonkey 和 Spring 服务不直接装进 APK。两项目的永久 Key 配置、桌面登录缓存、虚构个人资料提示词不复用。

## 构建

安装 JDK 17、Android SDK 35 和 build-tools 34.0.0。项目使用 AGP 8.7.3、Gradle 8.9。在此目录运行：

```sh
./gradlew :core:test :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest
```

没有 wrapper 时可用本机 Gradle 8.9 执行相同任务。构建不需要真实 API Key。调试 APK 在 `app/build/outputs/apk/debug/`；包名为 `cn.local.jobassistant.preview`，与交付签名包 `cn.local.jobassistant` 分开。

GitHub Actions 选择 **BOSS Android APK**，成功运行后下载 `boss-assistant-preview-apk-and-reports`，解压取得预览 APK。Actions 调试签名可能随运行环境变化，不保证覆盖安装前次预览版；正式交付包用单独保管的稳定发布签名。卸载会删除本机数据，先自行导出需要的备份。

构建同包名发布更新：在仓库外保存发布签名文件，通过环境变量 `ASSISTANT_SIGNING_FILE` 和 `ASSISTANT_SIGNING_PASSWORD` 提供，别名 `assistant`，执行 `./gradlew :app:assembleRelease`。不要将签名文件或密码提交 GitHub。不提供签名变量时无法得到已签署的发布包。

设备合成测试使用独立预览包：

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w cn.local.jobassistant.preview.test/cn.local.jobassistant.DeviceTests
```

设备测试会清空预览包的合成测试数据，不能在存有个人真实资料的预览安装上运行。测试 APK 不应作为用户日常 APP 安装。
