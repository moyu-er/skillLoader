package com.skillloader.registry;

import com.skillloader.config.*;
import com.skillloader.model.Skill;
import com.skillloader.model.SkillSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

class DefaultSkillRegistryTest {
    
    @TempDir
    Path tempDir;
    
    private void createSkill(Path dir, String name) throws Exception {
        Path skillDir = dir.resolve(name);
        Files.createDirectories(skillDir);
        String content = """
            ---
            name: %s
            description: Test skill
            ---
            # %s
            """.formatted(name, name);
        Files.writeString(skillDir.resolve("SKILL.md"), content);
    }
    
    private DefaultSkillRegistry createRegistry(Path... paths) throws Exception {
        List<PathEntry> pathEntries = new java.util.ArrayList<>();
        int priority = 10;
        for (Path path : paths) {
            pathEntries.add(new PathEntry("p" + priority, path.toString(), priority, false, PathType.FILESYSTEM));
            priority += 10;
        }
        
        SkillLoaderConfig config = new SkillLoaderConfig(
            pathEntries,
            ParserConfig.defaults(),
            SecurityConfig.defaults(),
            GeneratorConfig.defaults(),
            CacheConfig.defaults()
        );
        return new DefaultSkillRegistry(config);
    }
    
    @Test
    void shouldDiscoverSkills() throws Exception {
        createSkill(tempDir, "pdf");
        createSkill(tempDir, "weather");
        
        DefaultSkillRegistry registry = createRegistry(tempDir);
        List<Skill> skills = registry.discover();
        
        assertThat(skills).hasSize(2);
    }
    
    @Test
    void shouldFindSkillByName() throws Exception {
        createSkill(tempDir, "pdf");
        
        DefaultSkillRegistry registry = createRegistry(tempDir);
        Optional<Skill> skill = registry.find("pdf");
        
        assertThat(skill).isPresent();
        assertThat(skill.get().name()).isEqualTo("pdf");
    }
    
    @Test
    void shouldReturnEmptyWhenSkillNotFound() throws Exception {
        DefaultSkillRegistry registry = createRegistry(tempDir);
        Optional<Skill> skill = registry.find("non-existent");
        
        assertThat(skill).isEmpty();
    }
    
    @Test
    void shouldDeduplicateByPriority() throws Exception {
        // 创建两个同名 skill 在不同路径，不同优先级
        Path dir1 = tempDir.resolve("dir1");
        Path dir2 = tempDir.resolve("dir2");
        Files.createDirectories(dir1);
        Files.createDirectories(dir2);
        createSkill(dir1, "test");
        createSkill(dir2, "test");
        
        // dir1 priority=10, dir2 priority=20
        DefaultSkillRegistry registry = createRegistry(dir1, dir2);
        List<Skill> skills = registry.discover();
        
        assertThat(skills).hasSize(1);
        assertThat(skills.get(0).priority()).isEqualTo(10); // 低数字 = 高优先级
    }
    
    @Test
    void shouldFindBySource() throws Exception {
        createSkill(tempDir, "skill1");
        createSkill(tempDir, "skill2");
        
        DefaultSkillRegistry registry = createRegistry(tempDir);
        List<Skill> projectSkills = registry.findBySource(SkillSource.PROJECT);
        
        assertThat(projectSkills).hasSize(2);
    }
    
    @Test
    void shouldGetSkillNames() throws Exception {
        createSkill(tempDir, "alpha");
        createSkill(tempDir, "beta");
        
        DefaultSkillRegistry registry = createRegistry(tempDir);
        List<String> names = registry.getSkillNames();
        
        assertThat(names).containsExactlyInAnyOrder("alpha", "beta");
    }
    
    @Test
    void shouldCheckHasSkill() throws Exception {
        createSkill(tempDir, "exists");
        
        DefaultSkillRegistry registry = createRegistry(tempDir);
        
        assertThat(registry.hasSkill("exists")).isTrue();
        assertThat(registry.hasSkill("not-exists")).isFalse();
    }
    
    @Test
    void shouldRefresh() throws Exception {
        createSkill(tempDir, "skill1");
        
        DefaultSkillRegistry registry = createRegistry(tempDir);
        assertThat(registry.discover()).hasSize(1);
        
        // 添加新 skill
        createSkill(tempDir, "skill2");
        
        // 刷新前还是 1 个
        assertThat(registry.discover()).hasSize(1);
        
        // 刷新后变成 2 个
        registry.refresh();
        assertThat(registry.discover()).hasSize(2);
    }
    
    @Test
    void shouldReturnEmptyListForNoSkills() throws Exception {
        DefaultSkillRegistry registry = createRegistry(tempDir);
        List<Skill> skills = registry.discover();
        
        assertThat(skills).isEmpty();
    }
    
    @Test
    void shouldHandleConcurrentAccess() throws Exception {
        createSkill(tempDir, "concurrent");
        
        DefaultSkillRegistry registry = createRegistry(tempDir);
        
        // 多线程访问
        List<Thread> threads = new java.util.ArrayList<>();
        for (int i = 0; i < 10; i++) {
            threads.add(new Thread(() -> {
                registry.discover();
                registry.find("concurrent");
                registry.hasSkill("concurrent");
            }));
        }
        
        threads.forEach(Thread::start);
        for (Thread t : threads) {
            t.join();
        }
        
        // 没有异常即成功
        assertThat(registry.discover()).hasSize(1);
    }
}
