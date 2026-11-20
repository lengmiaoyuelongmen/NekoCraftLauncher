package example.hello;

import com.example.demo.core.app.AppVersion;
import com.example.demo.core.plugin.NekoPlugin;
import com.example.demo.core.plugin.PluginConfig;
import com.example.demo.core.plugin.PluginContext;
import com.example.demo.core.plugin.PluginHook;

/**
 * 示例插件：演示插件的三种能力 —— 命令面板命令、事件钩子、自己的持久化配置。
 *
 * <p>它统计「启动了多少次」并把计数写进 {@code <数据目录>/plugins/Hello_插件.properties}，
 * 用来证明插件能跨进程保存状态；钩子只做打印，证明插件不会被主流程当成依赖。
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
        PluginConfig config = context.config();
        int launches = config.getInt("launches", 0);
        context.log("Hello 插件已加载，宿主 " + AppVersion.CURRENT
                + "，累计启动 " + launches + " 次，配置 " + config.file().getAbsolutePath());

        context.addCommand(
                "插件示例：打个招呼",
                "Hello 插件 · 演示第三方扩展点",
                () -> context.log("你好，我是插件 example.hello，宿主是 Neko Craft Launcher "
                        + AppVersion.CURRENT));

        context.addCommand(
                "插件示例：启动计数",
                "读取插件自己的配置文件",
                () -> context.log("插件记录的累计启动次数：" + config.getInt("launches", 0)));

        context.on(PluginHook.LAUNCH_START, instanceId -> {
            int next = config.getInt("launches", 0) + 1;
            config.putInt("launches", next);
            boolean saved = config.save();
            context.log("实例 " + instanceId + " 开始启动（第 " + next + " 次"
                    + (saved ? "" : "，计数没能写盘") + "）");
        });

        context.on(PluginHook.LAUNCH_FINISH, instanceId ->
                context.log("实例 " + instanceId + " 已结束，下次启动计数会 +1"));

        context.on(PluginHook.PAGE_SWITCH, page ->
                context.log("切到页面：" + page));
    }
}
