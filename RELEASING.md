# AnvilLib 发布

在默认分支 `dev/26.1` 打开 **Actions → Create Multi-Version Releases → Run workflow**，在必填的 `tag` 中输入本次版本，例如 `2.0.0+pre-release.13`，不加 `v` 前缀。勾选 `dry_run` 可先构建七个版本并查看文件校验表，取消勾选后执行发布。同一时间只运行一次发布或演练。

可选的 `generate_release_notes` 默认关闭。开启后直接调用 GitHub 的 Generate release notes API，以本次锁定的 `dev/26.1` SHA 为目标，显式指定上一 tag 生成合并 PR、贡献者及完整变更链接。正文采用 GitHub 的返回内容，并附上七份构件的源码和校验表。仓库的 `.github/release.yml` 或 `.github/release.yaml` 配置同样生效。结果保存在原运行的 `source-plan` 和 tag 注释中，重跑复用该内容，`dry_run` 摘要也会展示它。

上一 tag 从已公开的 Release 中按发布时间由近到远选择，必须位于锁定提交的祖先历史中，允许继承自旧分支的 tag。Pre-release 可以接上一 Release 或 Pre-release，Release 只接上一 Release。草稿、当前 tag 和其他分支的后继或分叉提交均跳过。选择结果和原生说明一起冻结，重跑沿用。仓库首次发布使用 GitHub 原生初次发布说明；已有发布但找不到合格祖先时，摘要说明原因并跳过增量日志，避免 GitHub 自动选错范围。

## 版本与构件

七条开发线为 `1.21.1`、`1.21.4`、`1.21.8`、`1.21.11`、`26.1`、`26.2`、`26.3`。预检锁定所有分支头，发布版本只取手动输入的 `tag`。构建和 Central 部署通过 `-Pmod_version=<tag>` 覆盖 Gradle 属性，不修改源码中的 `gradle.properties`，也不要求各分支的开发版本一致。版本类型按以下项目约定自动识别，后缀使用 `+`，`N` 为无前导零的非负整数。

| 输入 tag | GitHub 类型 | Modrinth / CurseForge 类型 | 发布成功后升 patch |
| --- | --- | --- | --- |
| `X.Y.Z+alpha.N` | Pre-release | Alpha | 否 |
| `X.Y.Z+beta.N` | Pre-release | Beta | 否 |
| `X.Y.Z+pre-release.N` | Pre-release | Beta | 否 |
| `X.Y.Z+rc.N` | Pre-release | Beta | 否 |
| `X.Y.Z+build.N` | Release | Release | 否 |
| `X.Y.Z+hotfix.N` | Release | Release | 否 |
| `X.Y.Z` | Release | Release | 是 |

例如输入 `2.0.0+beta.1`，生成的 tag 就是 `2.0.0+beta.1`，Maven 坐标和 JAR 内版本使用该完整值。后续发布 `2.0.0+rc.1` 或 `2.0.0` 时，在输入框填写对应 tag 即可。所有类型都等待七个 Maven Central 部署达到 `PUBLISHED` 后公开 GitHub Release。Pre-release 使用独立 tag，GitHub 的 Latest 标记由正式版维护。

每条分支用自己的 Gradle Wrapper 和 JDK 从锁定的 SHA 构建。`stageRelease` 收集该分支模块清单中的全部发布单元、sync 注解处理器和主模块。校验覆盖 POM 的内部依赖、主 JAR 的模组版本、每个内嵌 slim JAR 的字节及 Maven 附件。

带注解 tag `<tag>` 指向锁定的默认分支提交。运行编号、七个源码 SHA、平台项目 ID 和供下载文件的 SHA-256 都在 tag 注释中。GitHub Release 展示七个主 JAR 及对应的源码链接。Minecraft 文件名取自实际游戏版本，例如 `dev/26.1` 构建的文件名中使用 `26.1.2`。

总控自动运行 Modrinth、CurseForge、Maven 和 ReleaseFile 上传，不提供平台选择。Maven 或 ReleaseFile 失败会使整体运行失败并保留草稿；两站独立运行，失败只记录警告和回执，公开 Release 与升版任务不等待两站。

| 渠道 | 完成条件 | 失败处理 |
| --- | --- | --- |
| Maven Central | 每个 Minecraft 版本一个 JReleaser 签名部署，七个部署均为 `PUBLISHED` | 保留 GitHub 草稿，在原运行使用 **Re-run failed jobs** |
| GitHub | 七个附件的摘要与 tag 清单一致，七个 Central 成功回执已记录 | 在原运行续传缺失附件或重试公开草稿 |
| Modrinth、CurseForge | 各自接收各游戏版本的主包和 sources、javadoc 附件 | 对应任务给出警告，不重试；GitHub 公开和升版 PR 独立执行 |

纯 `X.Y.Z` 的 Release 公开后，工作流对七条开发分支分别创建一个升至下一 patch 的 PR。标题和单一提交消息都包含 `[skip ci]`。按仓库现有规则审核并保留默认 squash 标题后合并。下一 patch 按输入 tag 计算，并从分支最新提交创建 PR；仅当根版本仍等于锁定源码中的开发版本时才修改，若已是目标版本则记为完成，其他版本报告冲突。重跑会复用升版分支与 PR。普通开发推送继续发布内部 Maven 快照以及两站的 Beta 构建。

## 开发 CI

每次开发分支推送或 PR 都在同一个 checkout 运行一次 `ciBuild`。Gradle 使用本地项目依赖构建 19 个子模块、sync 注解处理器和主模块。CI 校验 21 个 Maven 坐标、POM 的内部依赖范围及 Gradle 模块元数据。Actions 的构建附件包含这 21 个 JAR。

PR 使用 `+pr.N` 版本，运行服务端启动与关闭测试，并为子模块和主包生成 Roseau 报告。报告汇总在 `roseau-reports` 附件和现有 PR 评论中。开发推送使用 `+snapshot.N`。服务端测试成功后，CI 把已构建的文件上传到内部 Maven，所有子模块完成后才上传主包，再使用本次主包及 sources、javadoc 附件上架 Beta 快照。

## 续跑

- 正式流程中断时优先重跑原运行。已完成的构建通过原运行的 Actions artifact 复用。已存在的 tag、草稿附件和远端回执须与本次版本吻合。可在 tag 的注释中找到原运行编号。
- 两站上传各执行一次。原运行重跑时复用成功回执，已有失败或未决记录不会再次上传。Modrinth 可核对远端文件后补全成功回执。
- 发布进度记录在每条开发线的源码提交状态中。状态名形如 `release/2.0.0/26.1/central` 或 `release/2.0.0/26.1/curseforge/sources`。每条记录链接到产生它的 Actions 运行。CurseForge 为主文件和两个子文件分别记录回执，Modrinth 可从主文件的 SHA-512 找回已接收的版本。
- Central 日志输出 deployment ID 时会立即记录，重跑继续查询同一 ID。Central 明确拒绝部署时，摘要或错误日志会给出原因。若上传请求已发出却没有回执，先在 Central Portal 或 CurseForge 作者后台核对项目、版本、文件名和文件哈希。确认后可用下方命令补录状态，再重跑原任务。

```sh
gh api --method POST "repos/Anvil-Dev/AnvilLib/statuses/$SOURCE_SHA" -f context="$CONTEXT" -f state="$STATE" -f description="$RECEIPT"
```

Central 已接收时使用 `pending` 和 `deployment <UUID>`，由工作流确认最终状态。CurseForge 已接收时使用 `success` 和该文件的数字 ID。Central 在后台确认未接收后可使用 `failure` 和 `confirmed absent` 允许重新部署；两站已有失败记录时仍不重传。首次发布前应完成七条分支的构建适配，默认分支的总控最后启用。

## 凭据

Central 任务使用 `MAVEN_CENTRAL_USERNAME`、`MAVEN_CENTRAL_PASSWORD`、`SIGNING_KEY`、`SIGNING_PUBLIC_KEY` 和 `SIGNING_PASSWORD`。两站各使用自己的 `MODRINTH_TOKEN` 或 `CURSEFORGE_TOKEN`。仓库需允许 Actions 创建 PR。各任务按需使用 `GITHUB_TOKEN` 写入 tag、Release、提交状态和普通升版分支。

Gradle 的 Cjsah Maven 依赖仓库统一使用 `https://server.cjsah.net:1002/maven/releases/`；内部 Maven 上传继续读取机密 `MAVEN_URL`，两者分别配置。
