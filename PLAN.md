# SkillLoader 开发计划

## 项目概述
- **目标**: Java 21 Skill Loader SDK
- **核心功能**: 本地 skill 扫描、解析、AGENTS.md 生成
- **技术栈**: Java 21, Maven, SnakeYAML (唯一依赖)
- **代码量预估**: 800-1000 行

## 开发阶段

### Phase 1: 项目骨架 (30分钟)
- [ ] 创建 Maven 项目结构
- [ ] 配置 pom.xml (Java 21, SnakeYAML)
- [ ] 创建包结构

### Phase 2: 核心模型 (30分钟)
- [ ] Skill.java - Skill 领域对象
- [ ] SkillMetadata.java - YAML frontmatter 模型
- [ ] SkillContent.java - 完整内容模型
- [ ] SkillSource.java - 枚举 (PROJECT/GLOBAL)

### Phase 3: 配置模块 (20分钟)
- [ ] SkillLoaderConfig.java - 配置类 + Builder
- [ ] 默认配置逻辑

### Phase 4: 扫描器 (40分钟)
- [ ] SkillScanner.java - 目录扫描
- [ ] 优先级搜索路径实现
- [ ] 去重逻辑

### Phase 5: 解析器 (40分钟)
- [ ] SkillParser.java - SKILL.md 解析
- [ ] YAML frontmatter 提取
- [ ] Markdown 正文提取
- [ ] 资源文件检测

### Phase 6: 注册表 (30分钟)
- [ ] SkillRegistry.java - 内存索引
- [ ] 按名称查询
- [ ] 按标签过滤

### Phase 7: 生成器 (30分钟)
- [ ] AgentsMdGenerator.java
- [ ] skills_system XML 生成
- [ ] 现有文件更新逻辑

### Phase 8: Facade (20分钟)
- [ ] SkillLoader.java - 统一入口
- [ ] 便捷方法封装

### Phase 9: 测试 (40分钟)
- [ ] 单元测试
- [ ] 集成测试 (使用示例 skills)
- [ ] 边界情况测试

### Phase 10: 示例 & 文档 (20分钟)
- [ ] 示例 skill (test-skill/)
- [ ] README.md
- [ ] 使用示例代码

## 任务分配建议

| 任务 | 预估时间 | 难度 |
|------|----------|------|
| Phase 1-3 | 1.5h | ⭐ |
| Phase 4-6 | 2h | ⭐⭐ |
| Phase 7-8 | 1h | ⭐⭐ |
| Phase 9-10 | 1h | ⭐⭐ |
| **总计** | **~5.5h** | |

## 下一步行动

等待用户确认后，开始 Phase 1。
