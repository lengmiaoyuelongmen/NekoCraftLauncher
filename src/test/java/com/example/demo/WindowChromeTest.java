package com.example.demo;

import com.example.demo.core.app.LauncherConfig;
import com.example.demo.ui.shell.WindowChrome;
import javafx.stage.Stage;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** 关闭行为：配置为「直接退出」时不再弹询问框，直接回调 quit。 */
class WindowChromeTest {

    private static final class FakeHost implements WindowChrome.Host {
        final LauncherConfig config = new LauncherConfig();
        final List<String> logs = new ArrayList<>();
        final AtomicInteger quitCount = new AtomicInteger();

        @Override
        public LauncherConfig config() {
            return config;
        }

        @Override
        public Stage stage() {
            return null;
        }

        @Override
        public StackPane dialogLayer() {
            return null;
        }

        @Override
        public boolean gameRunning() {
            return false;
        }

        @Override
        public void log(String message) {
            logs.add(message);
        }

        @Override
        public void quit() {
            quitCount.incrementAndGet();
        }
    }

    @Test
    void exitActionQuitsDirectly() {
        FakeHost host = new FakeHost();
        host.config.setCloseAction("exit");
        WindowChrome chrome = new WindowChrome(host);

        chrome.requestClose();

        assertEquals(1, host.quitCount.get(), "closeAction=exit 应直接退出");
    }

    @Test
    void missingStageStillQuits() {
        FakeHost host = new FakeHost();
        host.config.setCloseAction("ask");
        WindowChrome chrome = new WindowChrome(host);

        chrome.requestClose();

        assertEquals(1, host.quitCount.get(), "没有窗口时只能退出，不能留一个僵尸进程");
    }
}
