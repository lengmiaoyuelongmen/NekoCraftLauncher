package com.example.demo;

import com.example.demo.core.download.DownloadFailure;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.http.HttpConnectTimeoutException;
import java.nio.file.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 下载失败分类：HTTP 状态、异常类型、中文说明。 */
class DownloadFailureTest {

    @Test
    void classifiesHttpStatus() {
        assertEquals(DownloadFailure.Kind.NOT_FOUND, DownloadFailure.classify(404));
        assertEquals(DownloadFailure.Kind.NOT_FOUND, DownloadFailure.classify(410));
        assertEquals(DownloadFailure.Kind.FORBIDDEN, DownloadFailure.classify(403));
        assertEquals(DownloadFailure.Kind.FORBIDDEN, DownloadFailure.classify(401));
        assertEquals(DownloadFailure.Kind.SERVER, DownloadFailure.classify(500));
        assertEquals(DownloadFailure.Kind.SERVER, DownloadFailure.classify(503));
        assertEquals(DownloadFailure.Kind.UNKNOWN, DownloadFailure.classify(302));
    }

    @Test
    void classifiesExceptions() {
        assertEquals(DownloadFailure.Kind.TIMEOUT, DownloadFailure.classify(new SocketTimeoutException()));
        assertEquals(DownloadFailure.Kind.TIMEOUT, DownloadFailure.classify(new HttpConnectTimeoutException("t")));
        assertEquals(DownloadFailure.Kind.NOT_FOUND, DownloadFailure.classify(new FileNotFoundException("x")));
        assertEquals(DownloadFailure.Kind.DISK,
                DownloadFailure.classify(new AccessDeniedException("C:/x")));
        assertEquals(DownloadFailure.Kind.DISK, DownloadFailure.classify(new IOException("磁盘空间不足")));
        assertEquals(DownloadFailure.Kind.NETWORK, DownloadFailure.classify(new IOException("Connection reset")));
        assertEquals(DownloadFailure.Kind.UNKNOWN, DownloadFailure.classify(new RuntimeException("boom")));
        assertEquals(DownloadFailure.Kind.UNKNOWN, DownloadFailure.classify(null));
    }

    @Test
    void describesReasonsForUsers() {
        assertTrue(DownloadFailure.describe(DownloadFailure.Kind.NOT_FOUND).contains("404"));
        assertTrue(DownloadFailure.describe(DownloadFailure.Kind.FORBIDDEN).contains("403"));
        assertTrue(DownloadFailure.describe(DownloadFailure.Kind.TIMEOUT).contains("超时"));
        assertTrue(DownloadFailure.describe(DownloadFailure.Kind.DISK).contains("磁盘"));
        assertTrue(DownloadFailure.describe(DownloadFailure.Kind.CHECKSUM).contains("校验"));
        assertEquals("未知原因", DownloadFailure.describe(null));
    }
}
