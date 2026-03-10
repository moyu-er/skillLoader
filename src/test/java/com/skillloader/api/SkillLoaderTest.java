package com.skillloader.api;

import com.skillloader.api.exceptions.SkillNotFoundException;
import com.skillloader.config.PathEntry;
import com.skillloader.config.PathType;
import com.skillloader.config.SkillLoaderConfig;
import com.skillloader.model.Skill;
import com.skillloader.model.SkillContent;
import com.skillloader.model.SkillMetadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

class SkillLoaderTest {
    
    @TempDir
    Path tempDir;
    
    private void createSkill(Path dir, String name) throws Exception {
        Path skillDir = dir.resolve(name);
        Files.createDirectories(skillDir);
        String content = """
            ---
            name: %s
            description: Test %s skill
            ---
            # %s Skill
            Test content.
            """.formatted(name, name, name);
        Files.writeString(skillDir.resolve("SKILL.md"), content);
    }
    
    private SkillLoader createLoader() throws Exception {
        createSkill(tempDir, "pdf");
        createSkill(tempDir, "weather");
        
        return SkillLoader.builder()
            .addFilesystemPath("test", tempDir.toString())
            .build();
    }
    
    @Test
    void shouldDiscoverSkills() throws Exception {
        SkillLoader loader = createLoader();
        
        List<Skill> skills = loader.discover();
        
        assertThat(skills).hasSize(2);
    }
    
    @Test
    void shouldLoadSkill() throws Exception {
        SkillLoader loader = createLoader();
        
        SkillContent content = loader.load("pdf");
        
        assertThat(content.metadata().name()).isEqualTo("pdf");
        assertThat(content.markdownContent()).contains("# pdf Skill");
    }
    
    @Test
    void shouldThrowWhenSkillNotFound() throws Exception {
        SkillLoader loader = createLoader();
        
        assertThatThrownBy(() -> loader.load("non-existent"))
            .isInstanceOf(SkillNotFoundException.class)
            .hasMessageContaining("non-existent");
    }
    
    @Test
    void shouldGetMetadata() throws Exception {
        SkillLoader loader = createLoader();
        
        Optional<SkillMetadata> metadata = loader.getMetadata("pdf");
        
        assertThat(metadata).isPresent();
        assertThat(metadata.get().name()).isEqualTo("pdf");
    }
    
    @Test
    void shouldReturnEmptyMetadataForNonExistent() throws Exception {
        SkillLoader loader = createLoader();
        
        Optional<SkillMetadata> metadata = loader.getMetadata("non-existent");
        
        assertThat(metadata).isEmpty();
    }
    
    @Test
    void shouldGenerateAgentsMd() throws Exception {
        SkillLoader loader = createLoader();
        
        String agentsMd = loader.generateAgentsMd();
        
        assertThat(agentsMd).contains("<skills_system");
        assertThat(agentsMd).contains("<name>pdf</name>");
        assertThat(agentsMd).contains("<name>weather</name>");
    }
    
    @Test
    void shouldSyncToFile() throws Exception {
        SkillLoader loader = SkillLoader.builder()
            .addFilesystemPath("test", tempDir.toString())
            .enableGenerator()
            .build();
        createSkill(tempDir, "pdf");
        Path agentsFile = tempDir.resolve("AGENTS.md");
        
        loader.syncToFile(agentsFile);
        
        assertThat(agentsFile).exists();
        String content = Files.readString(agentsFile);
        assertThat(content).contains("<skills_system");
    }
    
    @Test
    void shouldUpdateFile() throws Exception {
        SkillLoader loader = SkillLoader.builder()
            .addFilesystemPath("test", tempDir.toString())
            .enableGenerator()
            .build();
        createSkill(tempDir, "pdf");
        Path agentsFile = tempDir.resolve("AGENTS.md");
        Files.writeString(agentsFile, "# Existing\n\nOld content");
        
        loader.updateFile(agentsFile);
        
        String content = Files.readString(agentsFile);
        assertThat(content).contains("# Existing");
        assertThat(content).contains("<skills_system");
    }
    
    @Test
    void shouldThrowWhenSyncToFileWithoutEnablingGenerator() throws Exception {
        SkillLoader loader = createLoader();
        Path agentsFile = tempDir.resolve("AGENTS.md");
        
        assertThatThrownBy(() -> loader.syncToFile(agentsFile))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("AGENTS.md generation is disabled");
    }
    
    @Test
    void shouldGetAllowedPaths() throws Exception {
        SkillLoader loader = createLoader();
        
        List<PathEntry> paths = loader.getAllowedPaths();
        
        assertThat(paths).hasSize(1);
        assertThat(paths.get(0).path()).isEqualTo(tempDir.toString());
    }
    
    @Test
    void shouldGetConfig() throws Exception {
        SkillLoader loader = createLoader();
        
        SkillLoaderConfig config = loader.getConfig();
        
        assertThat(config).isNotNull();
        assertThat(config.paths()).hasSize(1);
    }
    
    @Test
    void shouldCreateWithDefaultConfig() {
        SkillLoader loader = SkillLoader.createDefault();
        
        assertThat(loader).isNotNull();
        assertThat(loader.getConfig()).isNotNull();
    }
    
    @Test
    void shouldCreateWithBuilder() throws Exception {
        createSkill(tempDir, "builder-test");
        
        SkillLoader loader = SkillLoader.builder()
            .addFilesystemPath("fs", tempDir.toString())
            .addClasspathPath("cp", "/skills")
            .build();
        
        assertThat(loader.discover()).hasSize(1);
        assertThat(loader.getAllowedPaths()).hasSize(2);
    }
    
    @Test
    void shouldReturnRegistry() throws Exception {
        SkillLoader loader = createLoader();
        
        assertThat(loader.registry()).isNotNull();
        assertThat(loader.registry().discover()).hasSize(2);
    }
}
