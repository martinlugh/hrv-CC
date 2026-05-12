package com.hrv.core.exception;

/**
 * 当读取的文件为空时抛出此异常。
 * 对应Python原项目中的 EmptyFileError。
 */
public class EmptyFileException extends RuntimeException {

    public EmptyFileException(String message) {
        super(message);
    }

    public EmptyFileException(String message, Throwable cause) {
        super(message, cause);
    }
}
