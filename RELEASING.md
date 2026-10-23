# 发布流程

本仓库的模块制品只在**发布 GitHub Release** 时上传到 Maven Central。向 `dev/**` 分支推送代码、发起
Pull Request 都只做构建与测试，不会把任何制品部署到中央仓库。

Modrinth 与 CurseForge 的上架（`publishMods`）不受此限制，仍由 `dev/**` 推送的构建执行。Release 构建会
一并触发该步骤，但 `release.yml` 未传入上架令牌，因此它在该路径上不会真正上架。

## 发布到 Maven Central

1. 推进版本号。版本号取自根目录 `gradle.properties` 的 `mod_version`，发布前必须把它改成这次要发布的
   新版本，并随改动正常合入 `dev/**` 分支。

   Maven Central 的版本一经发布不可撤回、也不允许覆盖同一坐标。忘记改版本号会让部署在
   `jreleaserDeploy` 阶段失败，而不是重发一个同名版本。

2. 在 GitHub 上创建 Release 并发布（`release: published` 事件）。不要用草稿或仅保存，只有真正点击发布
   才会触发工作流。

   Release 事件检出的是被发布 tag 指向的提交（`GITHUB_SHA` 为该 tag 的最后一次提交），工作流文件也取自
   那里。tag 必须打在本文件所述流程所在的提交上，否则跑的是旧版本工作流。

3. 工作流 `Release for Mod` 依次执行：按模块依赖拓扑分层构建 → 逐层上传 `staging-deploy` 制品 →
   调用 `Publish to Maven Central` 用 JReleaser 签名并部署到中央仓库 → 最后构建汇总模块 `anvillib`
   并部署。汇总模块的部署会等各子模块全部部署完成，避免消费者解析到依赖尚未就绪的 pom。

   预发布（prerelease）同样会发布到 Maven Central。若只是为了给测试者提供构建，请先确认版本号不会
   与将来的正式版本重复。

## 需要的仓库密钥

| 密钥 | 用途 |
| --- | --- |
| `MAVEN_CENTRAL_USERNAME` / `MAVEN_CENTRAL_PASSWORD` | Maven Central 发布账号 |
| `SIGNING_KEY` / `SIGNING_PUBLIC_KEY` / `SIGNING_PASSWORD` | GPG 签名 |
| `MAVEN_URL` / `MAVEN_USER` / `MAVEN_PASS` | 开发期的内部 Maven 仓库，用于模块间互相依赖 |
| `CURSEFORGE_TOKEN` / `MODRINTH_TOKEN` | 上架 CurseForge / Modrinth，由 `dev/**` 推送的构建使用 |

## 相关文件

- `.github/workflows/release.yml`：Release 触发的构建与 Maven Central 部署。
- `.github/workflows/ci.yml`：`dev/**` 推送触发的构建与测试，不含 Maven Central 部署。
- `.github/workflows/publish_maven_central.yml`：可复用的单个模块部署工作流，只由 `release.yml` 调用。
- `.github/workflows/build_and_test.yml`：可复用的单模块构建工作流。
