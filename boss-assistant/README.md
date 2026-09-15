# BOSS 求职助手 Android

本项目以用户提供的《BOSS直聘 AI 自动求职助手需求说明》全部 55 章为验收基准。当前开发版本 0.1.0，正在恢复构建与设备验证；尚未完成真实 BOSS 账号联调，不代表全部需求已验收。

## 开发方向

原生 Android（Java 17，Android 8.0 起），独立 BOSS 无障碍适配层，DeepSeek 默认、兼容 OpenAI 格式。用户通过 BOSS 官方 APP 手动登录；助手不接触其密码、Cookie 或 Session。官方 APP 的登录持久性由 BOSS 管理，助手不能替另一 APP 清除登录状态。

API Key 仅存进程内存；完全重启后重新输入，后台超过 30 分钟清除。默认测试模式不发送消息和投递。发送前先保存 UNKNOWN 任务；无法确定发送结果时暂停，避免崩溃后的重复发送。验证码、安全验证和平台限制一律停止并交给本人。

## 上游

- 首选：[boss-zhipin-bot](https://github.com/as161233574-alt/boss-zhipin-bot)，检查提交 `4f62a113de42fa3e8b14d243b4f7d2de45dfd732`。移植 HR 活跃度规则；岗位去重概念改为 SHA-256（岗位 ID 优先、公司/岗位/城市/HR 后备），保留 MIT 许可。
- 参考：[ai-job](https://github.com/yangfeng20/ai-job)，检查提交 `ef84db4bdee2f7eef84093bfa684ffe9dcfe6e04`。参考任务分层；不复制私有接口、反检测、登录和支付服务。

桌面 Python/Playwright、Tampermonkey 和 Spring 服务不直接装进 APK。两项目的永久 Key 配置、桌面登录缓存、虚构个人资料提示词不复用。

## 构建

安装 JDK 17、Android SDK 35、Gradle 8.9 后，在此目录执行 `gradle :core:test :app:assembleDebug`。构建无需 API Key。APK 生成在 `app/build/outputs/apk/debug/`。完整安装、操作、安全与测试说明随验收结果更新。
