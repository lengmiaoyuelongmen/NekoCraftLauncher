package example.hello;

import com.example.demo.core.app.AppVersion;
import com.example.demo.core.plugin.NekoPlugin;
import com.example.demo.core.plugin.PluginContext;

/**
 * 示例插件：演示如何通过 ServiceLoader 挂到启动器上。
 *
 * <p>它只做两件事：启动时写一行日志；在命令面板（Ctrl+K）里注册一条命令。
 * 其余能力（界面、启动流程）暂时不开放给插件，避免插件把主流程改坏。
 */
public final class HelloPlugin implements NekoPlugin {

    @Override
    public String id() {
        return "example.hello";
    }

    @Override
    public String name() {
        return "Hello 插件";
    }

    @Override
    public void init(PluginContext context) {
        context.log("Hello 插件已加载，宿主版本 " + AppVersion.CURRENT
                + "，数据目录 " + context.dataDir().getAbsolutePath());
        context.addCommand(
                "插件示例：打个招呼",
                "Hello 插件 · 演示第三方扩展点",
                () -> context.log("你好，我是插件 example.hello，宿主是 Neko Craft Launcher " + AppVersion.CURRENT));
    }
}
