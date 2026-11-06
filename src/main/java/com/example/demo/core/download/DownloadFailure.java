package com.example.demo.core.download;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;

/**
 * 下载失败分类：把「404」「超时」「磁盘满」区分开，给用户看的提示才有用。
 *
 * <p>以前失败只会显示一串 URL，用户不知道是资源不存在、网络问题还是硬盘满了。
 */
public final class DownloadFailure {

    public enum Kind {
        /** 资源不存在（404/410）。 */
        NOT_FOUND,
        /** 没有权限（401/403）。 */
        FORBIDDEN,
        /** 服务端错误（5xx）。 */
        SERVER,
        /** 请求超时。 */
        TIMEOUT,
        /** 其它网络错误（DNS、连接重置）。 */
        NETWORK,
        /** 校验不符（下到了坏文件）。 */
        CHECKSUM,
        /** 本地磁盘/权限问题。 */
        DISK,
        UNKNOWN
    }

    private DownloadFailure() {
    }

    public static Kind classify(int httpStatus) {
        if (httpStatus == 404 || httpStatus == 410) {
            return Kind.NOT_FOUND;
        }
        if (httpStatus == 401 || httpStatus == 403) {
            return Kind.FORBIDDEN;
        }
        if (httpStatus >= 500) {
            return Kind.SERVER;
        }
        return Kind.UNKNOWN;
    }

    public static Kind classify(Throwable error) {
        if (error == null) {
            return Kind.UNKNOWN;
        }
        if (error instanceof HttpTimeoutException || error instanceof SocketTimeoutException) {
            return Kind.TIMEOUT;
        }
        if (error instanceof java.io.FileNotFoundException) {
            return Kind.NOT_FOUND;
        }
        if (error instanceof java.nio.file.AccessDeniedException
                || error instanceof java.io.IOException && isDiskError(error.getMessage())) {
            return Kind.DISK;
        }
        if (error instanceof IOException) {
            return Kind.NETWORK;
        }
        return Kind.UNKNOWN;
    }

    /** 给日志用的中文说明。 */
    public static String describe(Kind kind) {
        if (kind == null) {
            return "未知原因";
        }
        return switch (kind) {
            case NOT_FOUND -> "资源不存在（404），可能是版本/文件已被删除";
            case FORBIDDEN -> "没有访问权限（403），可能被限流或需要登录";
            case SERVER -> "下载源服务端错误（5xx），稍后重试";
            case TIMEOUT -> "连接超时，网络慢或源不可达";
            case NETWORK -> "网络错误（连接被重置/无法解析）";
            case CHECKSUM -> "文件校验不符（下到的内容不对）";
            case DISK -> "本地磁盘错误或没有写权限";
            case UNKNOWN -> "未知原因";
        };
    }

    private static boolean isDiskError(String message) {
        if (message == null) {
            return false;
        }
        String lower = message.toLowerCase(java.util.Locale.ROOT);
        return lower.contains("no space") || lower.contains("磁盘") || lower.contains("denied")
                || lower.contains("read-only");
    }
}
