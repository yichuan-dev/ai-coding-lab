# Android 0.1.0 实际测试与交付报告

测试日期：2026-09-18。交付范围为可安装的工程验证版；最终需求基准仍是上传文档全部 55 章，未完成项没有删减。真实 BOSS 自动投递、自动回复与附件闭环尚未验收。

## 本次交付

| 项目 | 值 |
| --- | --- |
| 安装包 | BossAssistant-0.1.0.apk |
| 大小 | 8,495,814 字节 |
| 包名 / 版本 | cn.local.jobassistant / 0.1.0（1） |
| 最低系统 / 目标 SDK | Android 8.0（API 26）/ API 35；最低版本尚未逐一实机验证 |
| 发布代码提交 | `f1d04e2158702649e0cd213e7ebfde92ea0e9179`；之后仅补充测试等待逻辑和交付文档 |
| APK SHA-256 | `54b8f804654a7ea2d60f405e6a42d8b9c0302851ece23accadbf6c1d657029f7` |
| 签名证书 SHA-256 | `5281280ff3237324d7d320f771a8205ca0ac1f0a0b9aafee0ce7a8ca4a25627a` |

发布包非 debuggable，已验证 APK Signature Scheme v2。发布签名私钥独立保存，未提交源码仓库。

## 已实际执行的检查

| 检查 | 结果与边界 |
| --- | --- |
| 核心 Java 单元测试 | 49 通过，0 失败：SafetyTest 31、HrScenariosTest 12、RecoveryTest 6 |
| Android 设备测试 | 11 通过，0 失败；Android 9 / API 28、x86_64 模拟器，480×800 |
| lintDebug | 0 错误、7 条警告，见下述说明 |
| Debug、设备测试包和签名 Release 构建 | Gradle 实际执行成功 |
| 签名 Release 安装 / 启动 | adb 返回 Success；启动 Status: ok；读取到中文 Key 页面与空输入框 |
| 新任务保留旧进程 | 先注入合成 Key，再创建新 Activity；Key 清空且发送状态为 STOPPED |
| 关闭任务 | 等待 Android 异步销毁 Activity 后验证 Key 清空，即使没有运行前台服务 |
| 强制结束进程后重开 | 合成 Key 注入后 force-stop，再启动，返回 Key 页面且输入为空 |
| UI | 11 类原生页面实际启动；检查 FLAG_SECURE；人工查看 Key、首页、资料页测试图 |
| GitHub Actions | [与发布代码对应的运行成功](https://github.com/yichuan-dev/ai-coding-lab/actions/runs/35352858524)；执行核心测试、lint、构建；CI 没有运行模拟器测试 |
| 源码隐私检查 | 扫描通过；源码仅包含合成测试资料，无真实 Key、Cookie、Session、简历或签名私钥 |

Android 设备测试使用独立预览包和合成资料。HTTP 使用测试替身，验证状态码/超时/JSON/计数/Key 传递；没有使用用户真实 API Key，也不表示真实 DeepSeek 连接、账单或 TLS 证书链已经验收。界面测试图片由测试代码绘制，未关闭正式页面的安全窗口保护。

静态警告中有 3 条来自 PDF 依赖的 BouncyCastle `bcpkix` 包内通用 TrustManager，另有格式化与文本提示类警告。应用 AI 网络代码使用 Android 默认 HTTPS 校验，未设置 TrustManager、HostnameVerifier 或 SSL 工厂，也不调用该依赖的网络证书服务。没有通过压制全部警告把它们隐藏；这不是完整的第三方依赖安全审计。

## 设备测试项目

1. 11 类页面启动与安全窗口。
2. 清除本地资料后拒绝较早发起的异步导入写回。
3. DOCX 文字提取与外部实体拒绝。
4. 加密备份允许字段、错误密码拒绝。
5. Keystore/AES-GCM 保存、恢复与密文篡改拒绝。
6. 旧进程的新任务启动及任务销毁时清除 Key。
7. HTTP 401/403/402/429/500、超时的安全错误提示。
8. 请求使用临时 Key；数据库、备份及应用文件中没有合成 Key。
9. 非法 AI JSON 不成为答复。
10. Android 消息历史去重及备份导入规则兼容性。
11. 文字型 PDF 简历提取。

核心测试另覆盖：30 分钟单调时钟过期、停止使旧授权失效、默认测试模式不发、每日上限、UNKNOWN 不自动重试、去重窗口、AI 预算、敏感问题、人工接管、连续追问不无限拖延、翻动历史不重复回复、严格备份字段等。

12 类 HR 合成场景：求职状态、问候、简历、毕业时间、技能、到岗、薪资、加班、面试、联系方式、项目、身份证等敏感资料。已授权事实可以回答，未知或敏感资料进入等待本人处理；不编造到岗时间、经验或薪资。

## 测试中发现并处理的问题

- 修正 Actions 旧版 SDK tools 安装参数和 Android 字体常量 lint 错误，实际重新构建通过。
- 使用 Android 兼容的 JSONArray 访问方式；补上导入字段验证、聊天历史重放保护和清除资料后的异步写回保护。
- 补强 Android 保留进程但重开任务的情况，清除旧 Key 与发送授权。
- 新增退出测试最初在异步 onDestroy 执行前断言，出现 1 次失败；调整为等待实际销毁后检查，重新执行全部 11 项设备测试通过。失败记录未被当作通过。

## 尚未通过的最终验收

- 真实手机、当前 BOSS 版本的控件绑定、完整搜索/评分/投递/回复流程。
- 首次投递结果的可靠自动确认；当前需要本人到 BOSS 核对，UNKNOWN 不自动重试。
- 自动选择并发送简历附件；当前必须在 BOSS 官方 APP 手动发送。
- 全部会话巡检、不可见历史完整获取、平台已读/面试/Offer 状态同步。
- 真实 BOSS 验证码、登录失效、风控页面以及 OEM 后台管理行为。
- 真实 DeepSeek Key 连接和实际计费；ARM 手机、Android 8/其他版本、鸿蒙容器兼容性。
- 助手托管 BOSS 临时/记住登录、版本更新检查和完整用户日志界面。

因此，这次交付达到“可安装 APK + 核心功能 + 合成设备测试”阶段，未达到 55 章最终需求的全部验收。后续需要手机型号、系统版本、BOSS 版本和无私人内容的页面适配证据；不需要向开发者提供 Key、Cookie、Session 或私人简历。

## 可复现入口

构建和设备测试命令见 [README](../README.md)。测试代码在 `core/src/test/` 和 `app/src/androidTest/`。机器可读结果见 [test-evidence.json](test-evidence.json)；上游实跑结论见 [UPSTREAM_REVIEW.md](UPSTREAM_REVIEW.md)。
