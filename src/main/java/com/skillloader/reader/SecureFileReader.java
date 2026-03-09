package com.skillloader.reader;

import com.skillloader.api.exceptions.SecurityException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;

/**
 * 安全文件读取器接口。
 * 所有读取操作都经过白名单校验。
 */
public interface SecureFileReader {
    
    /**
     * 检查路径是否在白名单内。
     */
    boolean isAllowed(Path path);
    
    /**
     * 安全读取文件内容为字符串。
     * 
     * @throws SecurityException 路径不在白名单
     * @throws IOException 读取失败或文件过大
     */
    String read(Path path) throws SecurityException, IOException;
    
    /**
     * 列出目录内容（仅一层，不递归）。
     * 
     * @throws SecurityException 路径不在白名单
     * @throws IOException 读取失败
     */
    List<Path> listDirectory(Path dir) throws SecurityException, IOException;
    
    /**
     * 检查文件/目录是否存在且在白名单。
     */
    boolean exists(Path path);
    
    /**
     * 检查路径是否是目录且在白名单。
     */
    boolean isDirectory(Path path) throws SecurityException;
    
    /**
     * 打开文件输入流。
     * 
     * @throws SecurityException 路径不在白名单
     * @throws IOException 打开失败
     */
    InputStream openStream(Path path) throws SecurityException, IOException;
}
