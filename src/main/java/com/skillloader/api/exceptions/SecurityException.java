package com.skillloader.api.exceptions;

/**
 * 安全违规异常。
 * 当尝试访问白名单外的路径、路径遍历攻击、符号链接违规时抛出。
 */
public class SecurityException extends SkillLoaderException {
    
    public SecurityException(String message) {
        super(message);
    }
    
    public SecurityException(String message, Throwable cause) {
        super(message, cause);
    }
}
