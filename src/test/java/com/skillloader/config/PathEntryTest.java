package com.skillloader.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class PathEntryTest {
    
    @Test
    void shouldCreatePathEntry() {
        PathEntry entry = new PathEntry("test", "./skills", 10, true, PathType.FILESYSTEM);
        
        assertThat(entry.name()).isEqualTo("test");
        assertThat(entry.path()).isEqualTo("./skills");
        assertThat(entry.priority()).isEqualTo(10);
        assertThat(entry.required()).isTrue();
        assertThat(entry.type()).isEqualTo(PathType.FILESYSTEM);
    }
    
    @Test
    void shouldThrowExceptionWhenNameIsNull() {
        assertThatThrownBy(() -> new PathEntry(null, "./skills", 10, false, PathType.FILESYSTEM))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("name cannot be null");
    }
    
    @Test
    void shouldThrowExceptionWhenPathIsNull() {
        assertThatThrownBy(() -> new PathEntry("test", null, 10, false, PathType.FILESYSTEM))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("path cannot be null");
    }
    
    @Test
    void shouldThrowExceptionWhenTypeIsNull() {
        assertThatThrownBy(() -> new PathEntry("test", "./skills", 10, false, null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("type cannot be null");
    }
    
    @Test
    void shouldBeEqual() {
        PathEntry entry1 = new PathEntry("test", "./skills", 10, false, PathType.FILESYSTEM);
        PathEntry entry2 = new PathEntry("test", "./skills", 10, false, PathType.FILESYSTEM);
        
        assertThat(entry1).isEqualTo(entry2);
        assertThat(entry1.hashCode()).isEqualTo(entry2.hashCode());
    }
    
    @Test
    void shouldNotBeEqual() {
        PathEntry entry1 = new PathEntry("test1", "./skills", 10, false, PathType.FILESYSTEM);
        PathEntry entry2 = new PathEntry("test2", "./skills", 10, false, PathType.FILESYSTEM);
        
        assertThat(entry1).isNotEqualTo(entry2);
    }
    
    @Test
    void shouldHaveToString() {
        PathEntry entry = new PathEntry("test", "./skills", 10, false, PathType.FILESYSTEM);
        
        assertThat(entry.toString()).contains("test", "./skills", "FILESYSTEM");
    }
}
