---
name: jenkins-setup-guide
description: SkillLoader 项目的 Jenkins CI/CD 配置指南。包含 Jenkins 安装、插件配置、Pipeline 设置等。
tags: [jenkins, cicd, pipeline]
---

# Jenkins CI/CD 配置指南

## Jenkins vs GitHub Actions 对比

| 特性 | Jenkins | GitHub Actions |
|------|---------|----------------|
| 部署方式 | 私有化/自托管 | 云托管 |
| 定制性 | ⭐⭐⭐⭐⭐ 极高 | ⭐⭐⭐ 中等 |
| 维护成本 | 需要维护服务器 | 零维护 |
| 集成度 | 通用，支持任何 Git 仓库 | 与 GitHub 深度集成 |
| 适用场景 | 企业内网、复杂流程 | 开源项目、快速启动 |

## 推荐选择

- **GitHub Actions**: 如果代码在 GitHub，且团队规模小
- **Jenkins**: 如果需要私有化部署、复杂流程编排、多环境管理

## Jenkins 安装

### Docker 方式（推荐）

```bash
# 1. 创建 Jenkins 目录
mkdir -p ~/jenkins/data

# 2. 启动 Jenkins
docker run -d \
  --name jenkins \
  -p 8080:8080 \
  -p 50000:50000 \
  -v ~/jenkins/data:/var/jenkins_home \
  -v /var/run/docker.sock:/var/run/docker.sock \
  jenkins/jenkins:lts

# 3. 查看初始密码
docker logs jenkins | grep "initialAdminPassword"

# 4. 访问 http://localhost:8080 完成初始化
```

### 必要插件

在 Jenkins 管理界面安装：

1. **Pipeline** - Pipeline 支持
2. **Git** - Git 仓库集成
3. **GitHub Branch Source** - GitHub 多分支 Pipeline
4. **JUnit** - 测试报告展示
5. **HTML Publisher** - HTML 报告展示
6. **JaCoCo** - 覆盖率报告
7. **Checkstyle** - 代码风格报告
8. **Warnings Next Generation** - 警告收集
9. **Workspace Cleanup** - 工作区清理

## Jenkins 配置

### 1. 配置 JDK 21

**Manage Jenkins → Global Tool Configuration → JDK**

```
Name: JDK-21
Install automatically: ✓
Version: OpenJDK 21
```

### 2. 配置 Maven

**Manage Jenkins → Global Tool Configuration → Maven**

```
Name: Maven-3.9
Install automatically: ✓
Version: Apache Maven 3.9.x
```

### 3. 配置 GitHub 凭据

**Manage Jenkins → Manage Credentials → System → Global credentials**

```
Kind: Username with password
Username: your-github-username
Password: your-github-token
ID: github-credentials
```

> 建议使用 GitHub Personal Access Token 代替密码

## 创建 Pipeline

### 方式 1: Jenkinsfile（推荐）

1. 在 Jenkins 创建新任务
2. 选择 **Pipeline**
3. 配置 Pipeline：

```
Definition: Pipeline script from SCM
SCM: Git
Repository URL: https://github.com/xxsddm/skillLoader.git
Credentials: github-credentials
Branch Specifier: */master, */develop, */feature/*
Script Path: Jenkinsfile
```

### 方式 2: 手动配置

如果不想使用 Jenkinsfile，可以在 Jenkins 中直接配置：

```groovy
// 复制 Jenkinsfile 内容到 Pipeline 脚本区域
```

## 多分支 Pipeline（推荐）

为整个仓库创建多分支 Pipeline：

1. **New Item → Multibranch Pipeline**
2. **Branch Sources → GitHub**
3. 配置：

```
Repository HTTPS URL: https://github.com/xxsddm/skillLoader.git
Credentials: github-credentials
Behaviors:
  - Discover branches: All branches
  - Discover pull requests from origin: ✓
  - Discover pull requests from forks: ✓

Build Configuration:
  Mode: by Jenkinsfile
  Script Path: Jenkinsfile
```

## Webhook 配置（自动触发）

### GitHub 仓库设置

1. 进入仓库 Settings → Webhooks
2. **Add webhook**
3. 配置：

```
Payload URL: http://your-jenkins-server/github-webhook/
Content type: application/json
Events:
  - Pushes
  - Pull requests
```

### Jenkins 端配置

1. 确保安装了 **GitHub Branch Source** 插件
2. 在 Multibranch Pipeline 中启用 webhook

## 报告展示

### 测试报告

Jenkins 会自动展示：
- JUnit 测试结果
- 测试趋势图
- 失败测试详情

### 覆盖率报告

访问：
- `项目 → JaCoCo Coverage Report`
- 或 HTML 报告中的链接

### 代码风格报告

访问：
- `项目 → Checkstyle Report`
- 查看警告和错误

## 邮件通知

在 Jenkinsfile 中添加：

```groovy
post {
    failure {
        emailext (
            subject: "Build Failed: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
            body: """
                Build failed for ${env.JOB_NAME} #${env.BUILD_NUMBER}
                
                Check console output at ${env.BUILD_URL}
            """,
            to: "${env.CHANGE_AUTHOR_EMAIL ?: 'team@example.com'}"
        )
    }
    success {
        emailext (
            subject: "Build Success: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
            body: """
                Build successful for ${env.JOB_NAME} #${env.BUILD_NUMBER}
                
                View details: ${env.BUILD_URL}
            """,
            to: "${env.CHANGE_AUTHOR_EMAIL ?: 'team@example.com'}"
        )
    }
}
```

需要先配置 **Extended E-mail Notification** 插件和 SMTP。

## 与 GitHub PR 集成

### GitHub Checks API

1. 安装 **GitHub Checks** 插件
2. 在 Multibranch Pipeline 中启用 "GitHub Checks"

效果：
- PR 页面显示 Jenkins 构建状态
- 构建失败时阻止合并

### PR 状态检查

在 GitHub 仓库设置：

```
Settings → Branches → Branch protection rules → master

Require status checks to pass before merging: ✓
Status checks that are required:
  - continuous-integration/jenkins/pr-merge
```

## 故障排查

### 问题 1: Maven 命令找不到

```bash
# 在 Jenkins 容器内安装 Maven
docker exec -it jenkins bash
apt-get update
apt-get install -y maven
```

### 问题 2: 权限不足

```bash
# 修复 Jenkins 用户权限
docker exec -it jenkins bash
chown -R jenkins:jenkins /var/jenkins_home
```

### 问题 3: 覆盖率报告不显示

检查 JaCoCo 插件配置：
```groovy
jacoco {
    execPattern: 'target/jacoco.exec'
    classPattern: 'target/classes'
    sourcePattern: 'src/main/java'
}
```

## 完整 Jenkinsfile

见项目根目录 `Jenkinsfile`

## 参考

- [Jenkins Pipeline 文档](https://www.jenkins.io/doc/book/pipeline/)
- [Jenkinsfile 语法](https://www.jenkins.io/doc/book/pipeline/syntax/)
