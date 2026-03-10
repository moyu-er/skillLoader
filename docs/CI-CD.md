# SkillLoader CI/CD 方案对比

## 方案选择

### 方案 1: GitHub Actions（已配置）
- **位置**: `.github/workflows/ci.yml`
- **适用**: GitHub 仓库、快速启动
- **优点**: 零维护、与 GitHub 深度集成
- **缺点**: 定制化受限

### 方案 2: Jenkins（已配置）
- **位置**: `Jenkinsfile`
- **适用**: 私有化部署、复杂流程
- **优点**: 极高定制性、插件丰富
- **缺点**: 需要维护服务器

## 功能对比

| 功能 | GitHub Actions | Jenkins |
|------|---------------|---------|
| 自动触发 PR 构建 | ✅ | ✅ |
| JUnit 测试报告 | ✅ | ✅ |
| 覆盖率报告 (JaCoCo) | ✅ | ✅ |
| 代码风格检查 | ✅ | ✅ |
| 多分支 Pipeline | ✅ | ✅ |
| 私有化部署 | ❌ | ✅ |
| 邮件通知 | 需配置 | 原生支持 |
| 复杂流程编排 | 中等 | 极强 |

## 推荐选择

| 场景 | 推荐方案 |
|------|---------|
| 开源项目/小团队 | GitHub Actions |
| 企业内网/隐私要求 | Jenkins |
| 复杂部署流程 | Jenkins |
| 快速启动 | GitHub Actions |

## 同时使用

可以同时配置两种方案：
- GitHub Actions 用于快速反馈
- Jenkins 用于正式发布流程
