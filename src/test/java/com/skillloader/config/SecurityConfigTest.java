package com.skillloader.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class SecurityConfigTest {
    
    @Test
    void shouldCreateWithDefaults() {
        SecurityConfig config = SecurityConfig.defaults();
        
        assertThat(config.strictMode()).isTrue();
        assertThat(config.allowSymlinks()).isFalse();
        assertThat(config.maxDepth()).isEqualTo(3);
    }
    
    @Test
    void shouldCreateWithCustomValues() {
        SecurityConfig config = new SecurityConfig(false, true, 5);
        
        assertThat(config.strictMode()).isFalse();
        assertThat(config.allowSymlinks()).isTrue();
        assertThat(config.maxDepth()).isEqualTo(5);
    }
    
    @Test
    void shouldUseDefaultWhenZeroDepth() {
        SecurityConfig config = new SecurityConfig(true, false, 0);
        
        assertThat(config.maxDepth()).isEqualTo(3);
    }
    
    @Test
    void shouldUseDefaultWhenNegativeDepth() {
        SecurityConfig config = new SecurityConfig(true, false, -1);
        
        assertThat(config.maxDepth()).isEqualTo(3);
    }
}
