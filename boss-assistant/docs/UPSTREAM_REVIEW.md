# 上游实跑与 Android 复用判断

检查及运行日期：2026-09-15。本项目不把“能启动桌面服务”等同于“目前可以完成 BOSS 实际投递”。真实账号、DeepSeek 实际计费调用和 BOSS 当前手机页面尚未联调。

| 项目 | 检查提交 | 实际结果 | Android 结论 |
| --- | --- | --- | --- |
| [boss-zhipin-bot](https://github.com/as161233574-alt/boss-zhipin-bot) | `4f62a113de42fa3e8b14d243b4f7d2de45dfd732` | 补齐缺失依赖后 FastAPI TestClient 的 `/api/health`、`/` 均返回 200；6 组测试合计 132 通过、2 失败 | 保留规则层思路，重写 Android 宿主、登录、密钥和 UI 驱动 |
| [ai-job](https://github.com/yangfeng20/ai-job) | `ef84db4bdee2f7eef84093bfa684ffe9dcfe6e04` | 原始 npm 安装出现 Vite 依赖冲突；兼容安装后 vue-tsc 与解析到的 TypeScript 不兼容；测试副本固定 TypeScript 5.3.3 后仍有 2 个源码类型错误 | 不直接移植；只参考分层和任务组织 |

首选项目原始 requirements 缺少运行/测试所需的 numpy、pdfplumber 等。测试环境另装 httpx、pytest、python-multipart、socksio；没有改动真实账号或执行联系 HR 的流程。测试命令：

```sh
python -m pytest tests/test_models.py tests/test_services.py tests/test_routes.py tests/test_llm_client.py tests/test_config.py tests/test_dedup_endpoint.py -q
```

失败项为 `TestApplication::test_clear_and_trash`、`TestDedupeFunction::test_dedupe_empty_db`，都出现数据库记录数与预期不一致。测试数据库隔离是排查方向；不能仅据此断言生产数据库必定损坏。

备选项目测试副本的具体错误：`AiJob.vue:384` 的 Timeout 不能赋给 number；`Preference.vue:457` 的表单回调返回 boolean，不符合 void / Promise<void>。Spring 服务的 pom 还固定了 Windows javac.exe 绝对路径，并依赖 MySQL、Spring AI SNAPSHOT 等外部环境。先前 Maven 尝试在依赖解析阶段达到时间上限，未取得服务启动成功的证据；没有把环境超时当成上游接口已失效的证据。

## 实际复用范围

| 模块 | 处理 | 原因 |
| --- | --- | --- |
| HR 活跃度规则 | 将首选项目 `score_hr_activity` 移植至 `JobRules.hrActivity`，保留 MIT 许可 | 纯规则，与浏览器和账号无关，可独立测试 |
| 岗位去重 | 复用公司、岗位、城市的联合标识思路；增加 HR，去掉易变化的薪资，改 SHA-256 长度分隔编码 | 防止薪资变动后重复投递；有稳定 BOSS ID 时优先使用 |
| JSON 输出处理 | 保留代码围栏容错后严格解析的思路 | 避免把不合法的模型文本用于操作 |
| AI 客户端 | 重写为 HTTPS OpenAI Compatible 客户端 | Key 仅存内存，禁止日志、重定向、磁盘缓存；加入预算和过期检查 |
| 简历解析 | Android PDFBox + 有界 DOCX XML 解析，用户核对后启用 | 桌面 PDF/文档库不能直接用于 APK |
| 数据库与任务调度 | Android Keystore/AES-GCM + AtomicFile + 发送账本 | 原桌面配置会持久保存 Key；手机需处理进程死亡和发送结果不确定 |
| BOSS 登录 | 使用官方 APP 手动登录 | 助手不获取密码、Cookie、Session；不冒充平台客户端 |
| BOSS 搜索、页面、消息与投递 | 独立无障碍适配模块 | Playwright、Tampermonkey 页面选择器不能直接复用于原生 Android |
| 反检测、设备伪装、私有通信 Hook | 不使用 | 与最终需求明确冲突 |
| 写死的候选人资料、夸大经历提示词 | 不使用 | 未知资料必须保持未知 |

可安装 APK 的路线确定为原生 Android，而不是把 Python、Node 或桌面浏览器环境装进手机。BOSS 当前版本的控件匹配仍需要真实手机上的可见页面证据；本报告不将未联调功能标记为可用。
