package com.skillloader.api.exceptions;

/**
 * 配置错误异常。
 * 当配置文件格式错误、缺少必要配置时抛出。
 */
public class ConfigException extends SkillLoaderException {
    
    public ConfigException(String message) {
        super(message);
    }
    
    public ConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
