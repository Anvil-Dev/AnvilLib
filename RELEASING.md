# AnvilLib 正式发布

在默认分支 `dev/26.1` 打开 **Actions → Release AnvilLib → Run workflow**。勾选 `dry_run` 可先构建七个版本并查看文件校验表。正式发布时取消勾选即可。同一时间只运行一次正式发布或演练。

## 版本与构件

七条开发线为 `1.21.1`、`1.21.4`、`1.21.8`、`1.21.11`、`26.1`、`26.2`、`26.3`。预检读取所有分支头，要求根 `mod_version` 同为 `X.Y.Z`。每条分支用自己的 Gradle Wrapper 和 JDK 从锁定的 SHA 构建。`stageRelease` 收集该分支模块清单中的全部发布单元、sync 注解处理器和主模块。校验覆盖 POM 的内部依赖、主 JAR 的模组版本、每个内嵌 slim JAR 的字节及 Maven 附件。

带注解 tag `vX.Y.Z` 指向锁定的默认分支提交。运行编号、七个源码 SHA、平台项目 ID 和供下载文件的 SHA-256 都在 tag 注释中。GitHub Release 展示七个主 JAR 及对应的源码链接。Minecraft 文件名取自实际游戏版本，例如 `dev/26.1` 构建的文件名中使用 `26.1.2`。

| 渠道 | 完成条件 | 失败处理 |
| --- | --- | --- |
| Maven Central | 每个 Minecraft 版本一个 JReleaser 签名部署，七个部署均为 `PUBLISHED` | 保留 GitHub 草稿，在原运行使用 **Re-run failed jobs** |
| GitHub | 七个附件的摘要与 tag 清单一致，七个 Central 成功回执已记录 | 在原运行续传缺失附件或重试公开草稿 |
| Modrinth、CurseForge | 各自接收各游戏版本的主包和 sources、javadoc 附件 | 对应任务给出警告，GitHub 公开和升版 PR 独立执行 |

Release 公开后，工作流对七条开发分支分别创建一个升至下一 patch 的 PR。标题和单一提交消息都包含 `[skip ci]`。按仓库现有规则审核并保留默认 squash 标题后合并。重跑会复用升版分支与 PR。待七条分支版本一致后再发下一版。普通开发推送继续发布内部 Maven 快照以及两站的 Beta 构建。

## 开发 CI

每次开发分支推送或 PR 都在同一个 checkout 运行一次 `ciBuild`。Gradle 使用本地项目依赖构建 19 个子模块、sync 注解处理器和主模块。CI 校验 21 个 Maven 坐标、POM 的内部依赖范围及 Gradle 模块元数据。Actions 的构建附件包含这 21 个 JAR。

PR 使用 `+pr.N` 版本，运行服务端启动与关闭测试，并为子模块和主包生成 Roseau 报告。报告汇总在 `roseau-reports` 附件和现有 PR 评论中。开发推送使用 `+snapshot.N`。服务端测试成功后，CI 把已构建的文件上传到内部 Maven，所有子模块完成后才上传主包，再使用本次主包及 sources、javadoc 附件上架 Beta 快照。

## 续跑和补发

- 正式流程中断时优先重跑原运行。已完成的构建通过原运行的 Actions artifact 复用。已存在的 tag、草稿附件和远端回执须与本次版本吻合。可在 tag 的注释中找到原运行编号。
- 单独补发模组平台时，从默认分支运行 **Upload release to a mod site**，选择原 tag、开发线和平台。它核验原文件的 SHA-256。Actions 构件过期后，主包取自 GitHub Release，sources 和 javadoc 取自同版 Maven Central。
- 发布进度记录在每条开发线的源码提交状态中。状态名形如 `release/v2.0.0/26.1/central` 或 `release/v2.0.0/26.1/curseforge/sources`。每条记录链接到产生它的 Actions 运行。CurseForge 为主文件和两个子文件分别记录回执，Modrinth 可从主文件的 SHA-512 找回已接收的版本。
- Central 日志输出 deployment ID 时会立即记录，重跑继续查询同一 ID。Central 明确拒绝部署时，摘要或错误日志会给出原因。若上传请求已发出却没有回执，先在 Central Portal 或 CurseForge 作者后台核对项目、版本、文件名和文件哈希。确认后可用下方命令补录状态，再重跑原任务。

```sh
gh api --method POST "repos/Anvil-Dev/AnvilLib/statuses/$SOURCE_SHA" \
  -f context="$CONTEXT" -f state="$STATE" -f description="$RECEIPT"
```

Central 已接收时使用 `pending` 和 `deployment <UUID>`，由工作流确认最终状态。CurseForge 已接收时使用 `success` 和该文件的数字 ID。仅在后台确认未接收时，使用 `failure` 和 `confirmed absent` 允许重新上传。首次发布前应完成七条分支的构建适配，默认分支的总控最后启用。

## 凭据

Central 任务使用 `MAVEN_CENTRAL_USERNAME`、`MAVEN_CENTRAL_PASSWORD`、`SIGNING_KEY`、`SIGNING_PUBLIC_KEY` 和 `SIGNING_PASSWORD`。两站各使用自己的 `MODRINTH_TOKEN` 或 `CURSEFORGE_TOKEN`。仓库需允许 Actions 创建 PR。各任务按需使用 `GITHUB_TOKEN` 写入 tag、Release、提交状态和普通升版分支。
