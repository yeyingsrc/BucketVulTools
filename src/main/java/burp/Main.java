package burp;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import burp.task.IScanCheck;
import burp.ui.UI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class Main implements BurpExtension {
    public static MontoyaApi api = null;
    public static JTabbedPane pane;
    public static Color foreground;
    public static Color background;
    public static int tabIndex;
    @Override
    public void initialize(MontoyaApi api) {
        Main.api = api;
        api.extension().setName("BucketVulTool");

        // 创建自定义的 Tab 组件并注册
        api.userInterface().registerSuiteTab("BucketVulTool", UI.getUI(
                api.userInterface().createHttpRequestEditor(),
                api.userInterface().createHttpResponseEditor())
        );
        api.scanner().registerScanCheck(new IScanCheck());
        //查找插件面板

    }

    public static void findComponent(Component component) {
        // 如果组件是 JTabbedPane，遍历其中的 Tabs
        if (component instanceof JTabbedPane) {
            JTabbedPane tabbedPane = (JTabbedPane) component;

            // 遍历每个 Tab
            for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                String tabTitle = tabbedPane.getTitleAt(i);  // 获取 Tab 名称
                // 如果找到目标 Tab
                if (tabTitle.equals("BucketVulTool")) {
                    // 输出 Tab 页的序号和组件
                    System.out.println("Found Tab: " + tabTitle + " at index " + i);
                    // 可以将 Tab 页和序号存储到某个地方，供后续使用
                    pane = tabbedPane;  // 存储 JTabbedPane 组件
                    tabIndex = i;  // 存储 Tab 页的索引
                }
            }

        }

        // 如果组件是容器，继续递归
        if (component instanceof Container) {
            Component[] childComponents = ((Container) component).getComponents();
            for (Component child : childComponents) {
                findComponent(child);  // 递归查找
            }
        }
    }

    //获取注册的扩展面板
    public static void getExtUIPanel()
    {
        if (Main.pane == null) {
            Frame frame = Main.api.userInterface().swingUtils().suiteFrame();
            for (Component component : frame.getComponents()) {
                Main.findComponent(component);
            }
            Main.foreground = Main.pane.getComponentAt(Main.tabIndex).getForeground();
            Main.background = Main.pane.getComponentAt(Main.tabIndex).getBackground();

            Main.pane.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    // 获取点击的位置对应的标签索引
                    int tabIndex = Main.pane.indexAtLocation(e.getX(), e.getY());
                    if (tabIndex == Main.tabIndex) {
                        Main.pane.setForegroundAt(Main.tabIndex,Main.foreground);
                        Main.pane.setBackgroundAt(Main.tabIndex,Main.foreground);
                    }
                }
            });
        }
    }

}
