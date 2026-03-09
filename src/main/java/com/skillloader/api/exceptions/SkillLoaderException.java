package com.skillloader.api.exceptions;

/**
 * SkillLoader 基础异常。
 * 所有 SkillLoader 异常的根类。
 */
public class SkillLoaderException extends Exception {
    
    public SkillLoaderException(String message) {
        super(message);
    }
    
    public SkillLoaderException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public SkillLoaderException(Throwable cause) {
        super(cause);
    }
}
