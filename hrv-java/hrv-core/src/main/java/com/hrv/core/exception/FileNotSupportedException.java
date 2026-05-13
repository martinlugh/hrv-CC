package com.hrv.core.exception;

/**
 * 当文件格式不受支持时抛出此异常。
 * 对应Python原项目中的 FileNotSupportedError。
 */
public class FileNotSupportedException extends RuntimeException {

    public FileNotSupportedException(String message) {
        super(message);
    }

    public FileNotSupportedException(String message, Throwable cause) {
        super(message, cause);
    }
}
