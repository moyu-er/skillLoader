package com.skillloader.config;

import com.skillloader.api.exceptions.ConfigException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class SkillLoaderConfigTest {
    
    @Test
    void shouldCreateConfigWithBuilder() {
        SkillLoaderConfig config = SkillLoaderConfig.builder()
            .addPath(new PathEntry("test", "./skills", 10, false, PathType.FILESYSTEM))
            .build();
        
        assertThat(config.paths()).hasSize(1);
        assertThat(config.paths().get(0).name()).isEqualTo("test");
    }
    
    @Test
    void shouldThrowExceptionWhenNoPathsConfigured() {
        assertThatThrownBy(() -> SkillLoaderConfig.builder().build())
            .isInstanceOf(ConfigException.class)
            .hasMessageContaining("At least one path must be configured");
    }
    
    @Test
    void shouldSortPathsByPriority() {
        SkillLoaderConfig config = SkillLoaderConfig.builder()
            .addPath(new PathEntry("high", "./high", 5, false, PathType.FILESYSTEM))
            .addPath(new PathEntry("low", "./low", 20, false, PathType.FILESYSTEM))
            .addPath(new PathEntry("mid", "./mid", 10, false, PathType.FILESYSTEM))
            .build();
        
        assertThat(config.paths())
            .extracting(PathEntry::priority)
            .containsExactly(5, 10, 20);
    }
    
    @Test
    void shouldUseDefaultConfigsWhenNotSpecified() {
        SkillLoaderConfig config = SkillLoaderConfig.builder()
            .addPath(new PathEntry("test", "./skills", 10, false, PathType.FILESYSTEM))
            .build();
        
        assertThat(config.parser()).isNotNull();
        assertThat(config.parser().markerFile()).isEqualTo("SKILL.md");
        assertThat(config.security()).isNotNull();
        assertThat(config.security().strictMode()).isTrue();
        assertThat(config.generator()).isNotNull();
    }
    
    @Test
    void shouldCreateDefaultConfig() {
        SkillLoaderConfig config = SkillLoaderConfig.defaults();
        
        assertThat(config.paths()).hasSize(1);
        assertThat(config.paths().get(0).name()).isEqualTo("default");
    }
    
    @Test
    void shouldSupportConvenienceMethods() {
        SkillLoaderConfig config = SkillLoaderConfig.builder()
            .addFilesystemPath("fs", "./skills")
            .addClasspathPath("cp", "/skills")
            .build();
        
        assertThat(config.paths()).hasSize(2);
        assertThat(config.paths().get(0).type()).isEqualTo(PathType.FILESYSTEM);
        assertThat(config.paths().get(1).type()).isEqualTo(PathType.CLASSPATH);
    }
    
    @Test
    void shouldPreserveCustomConfigs() {
        ParserConfig parser = new ParserConfig("CUSTOM.md", "GBK", 2048);
        SecurityConfig security = new SecurityConfig(false, true, 5);
        GeneratorConfig generator = new GeneratorConfig("custom", "START", "END");
        
        SkillLoaderConfig config = SkillLoaderConfig.builder()
            .addPath(new PathEntry("test", "./skills", 10, false, PathType.FILESYSTEM))
            .parser(parser)
            .security(security)
            .generator(generator)
            .build();
        
        assertThat(config.parser().markerFile()).isEqualTo("CUSTOM.md");
        assertThat(config.security().maxDepth()).isEqualTo(5);
        assertThat(config.generator().markerStart()).isEqualTo("START");
    }
}
