package com.skillloader.config;

import java.util.Objects;

/**
 * 解析器配置。
 */
public final class ParserConfig {
    
    public static final String DEFAULT_MARKER_FILE = "SKILL.md";
    public static final String DEFAULT_ENCODING = "UTF-8";
    public static final long DEFAULT_MAX_FILE_SIZE = 1024 * 1024; // 1MB
    
    private final String markerFile;
    private final String encoding;
    private final long maxFileSize;
    
    public ParserConfig(String markerFile, String encoding, long maxFileSize) {
        this.markerFile = markerFile != null ? markerFile : DEFAULT_MARKER_FILE;
        this.encoding = encoding != null ? encoding : DEFAULT_ENCODING;
        this.maxFileSize = maxFileSize > 0 ? maxFileSize : DEFAULT_MAX_FILE_SIZE;
    }
    
    public static ParserConfig defaults() {
        return new ParserConfig(DEFAULT_MARKER_FILE, DEFAULT_ENCODING, DEFAULT_MAX_FILE_SIZE);
    }
    
    public String markerFile() {
        return markerFile;
    }
    
    public String encoding() {
        return encoding;
    }
    
    public long maxFileSize() {
        return maxFileSize;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ParserConfig that = (ParserConfig) o;
        return maxFileSize == that.maxFileSize &&
               Objects.equals(markerFile, that.markerFile) &&
               Objects.equals(encoding, that.encoding);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(markerFile, encoding, maxFileSize);
    }
    
    @Override
    public String toString() {
        return "ParserConfig{" +
               "markerFile='" + markerFile + '\'' +
               ", encoding='" + encoding + '\'' +
               ", maxFileSize=" + maxFileSize +
               '}';
    }
}
