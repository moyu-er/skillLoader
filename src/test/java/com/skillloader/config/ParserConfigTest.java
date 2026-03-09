package com.skillloader.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class ParserConfigTest {
    
    @Test
    void shouldCreateWithDefaults() {
        ParserConfig config = ParserConfig.defaults();
        
        assertThat(config.markerFile()).isEqualTo("SKILL.md");
        assertThat(config.encoding()).isEqualTo("UTF-8");
        assertThat(config.maxFileSize()).isEqualTo(1024 * 1024);
    }
    
    @Test
    void shouldCreateWithCustomValues() {
        ParserConfig config = new ParserConfig("CUSTOM.md", "GBK", 2048);
        
        assertThat(config.markerFile()).isEqualTo("CUSTOM.md");
        assertThat(config.encoding()).isEqualTo("GBK");
        assertThat(config.maxFileSize()).isEqualTo(2048);
    }
    
    @Test
    void shouldUseDefaultsWhenNull() {
        ParserConfig config = new ParserConfig(null, null, 0);
        
        assertThat(config.markerFile()).isEqualTo("SKILL.md");
        assertThat(config.encoding()).isEqualTo("UTF-8");
        assertThat(config.maxFileSize()).isEqualTo(1024 * 1024);
    }
    
    @Test
    void shouldUseDefaultWhenNegativeSize() {
        ParserConfig config = new ParserConfig("test.md", "UTF-8", -1);
        
        assertThat(config.maxFileSize()).isEqualTo(1024 * 1024);
    }
}
