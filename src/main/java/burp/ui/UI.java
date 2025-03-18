package burp.ui;

import burp.Main;
import burp.api.montoya.ui.editor.HttpRequestEditor;
import burp.api.montoya.ui.editor.HttpResponseEditor;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class UI {
    private static JTable currentTable;                   // 表格组件
    private static JPanel tableJPanel;                    // 用于放置表格的面板
    private static HttpRequestEditor requestEditor;       // HTTP 请求编辑器
    private static HttpResponseEditor httpResponseEditor; // HTTP 响应编辑器
    private static int num = 0;                           // 序号计数器

    // 用一个 Set 来存储已添加的条目，避免重复添加
    private static Set<String> addedEntries = new HashSet<>();

    /**
     * 创建表格并设置列标题
     */
    private static Component createTable() {
        // 为表格添加新的列
        String[] tableTitle = new String[]{"序号", "url", "漏洞类型", "厂商"};

        // 设置表格和编辑权限
        currentTable = new JTable() {
            public boolean isCellEditable(int row, int col) {
                // 只允许第三列可编辑
                return col == 1;
            }
        };
        currentTable.getTableHeader().setReorderingAllowed(false);  // 禁止列拖动

        // 设置表格模型
        DefaultTableModel model = (DefaultTableModel) currentTable.getModel();
        model.setColumnIdentifiers(tableTitle); // 设置表格列标题
        currentTable.setModel(model);

        // 设置表格内容居中
        centerTableContent();

        // 添加鼠标点击事件
        currentTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int r = currentTable.getSelectedRow();  // 获取被点击的行
                ListsModule valueAt = (ListsModule) currentTable.getValueAt(r, 1);
                requestEditor.setRequest(valueAt.getRequestResponse().request());
                httpResponseEditor.setResponse(valueAt.getRequestResponse().response());
                httpResponseEditor.setSearchExpression(currentTable.getValueAt(r, 2).toString());
            }
        });

        // 调整列宽
        adjustColumnWidths();

        // 将表格放入滚动面板
        return new JScrollPane(currentTable);
    }

    /**
     * 设置表格的内容居中
     */
    private static void centerTableContent() {
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);

        // 设置每列的渲染器为居中
        for (int i = 0; i < currentTable.getColumnCount(); i++) {
            currentTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }
    }

    /**
     * 调整表格列宽
     * 使得每一列自适应其内容的宽度
     * URL 列根据百分比来设置宽度
     */
    private static void adjustColumnWidths() {
        TableColumn column;

        // 获取当前表格的总宽度
        int tableWidth = currentTable.getPreferredSize().width;

        column = currentTable.getColumnModel().getColumn(1);
        column.setPreferredWidth((int) (tableWidth * 0.8));

        int remainingWidth = tableWidth - (int) (tableWidth * 0.2);

        for (int i = 0; i < currentTable.getColumnCount(); i++) {
            if (i == 1) continue;  // 跳过 URL 列

            column = currentTable.getColumnModel().getColumn(i);
            column.setPreferredWidth(remainingWidth / (currentTable.getColumnCount() - 1));
        }
    }

    /**
     * 创建界面布局，包括表格和编辑器
     */
    private static Component createLayout(HttpRequestEditor reqEdit, HttpResponseEditor respEdit) {
        requestEditor = reqEdit;
        httpResponseEditor = respEdit;

        // 上方的表格区域
        JScrollPane tableScrollPane = new JScrollPane(createTable());

        // 请求和响应编辑器区域
        JPanel reqRespEdit = new JPanel(new GridLayout(1, 2));
        reqRespEdit.add(reqEdit.uiComponent());
        reqRespEdit.add(respEdit.uiComponent());

        // 使用 JSplitPane 创建可调整的分隔布局
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableScrollPane, reqRespEdit);
        splitPane.setResizeWeight(0.5); // 初始分配表格和编辑器区域的空间比例
        splitPane.setOneTouchExpandable(true); // 允许通过单击快速调整

        // 主面板布局
        JPanel mainPane = new JPanel(new BorderLayout());
        mainPane.add(splitPane, BorderLayout.CENTER);  // 将分隔布局放置在主面板中心
        mainPane.setVisible(true);
        return mainPane;
    }

    /**
     * 获取 UI 界面
     */
    public static Component getUI(HttpRequestEditor reqEdit, HttpResponseEditor respEdit) {
        return createLayout(reqEdit, respEdit);
    }

    /**
     * 设置表格数据，包括序号、URL、匹配参数和厂商
     * 这里添加了去重逻辑
     */
    private static void setModeData(ListsModule listsModule) {
        DefaultTableModel model = (DefaultTableModel) currentTable.getModel();

        // 生成唯一的键，用于去重
        String uniqueKey = generateUniqueKey(listsModule);

        // 如果该条目没有被添加过，则加入表格
        if (!addedEntries.contains(uniqueKey)) {
            model.addRow(new Object[]{num++, listsModule, listsModule.getVulType(), listsModule.getVendor()});
            addedEntries.add(uniqueKey);  // 标记该条目为已添加
        }
        currentTable.setModel(model);

        // 重新调整列宽，确保更新后的内容自适应
        adjustColumnWidths();
    }

    /**
     * 更新 UI 数据，将新数据添加到表格中
     */
    public static void updateUIData(List<ListsModule> list) {
        //更新UI颜色
        if (!list.isEmpty()) {
            Main.pane.setBackgroundAt(Main.tabIndex,Color.RED);
            Main.api.logging().logToOutput("目标：" + list.get(0).getURL() + " 存在漏洞" +  list.get(0).getVulType());
        }

        for (ListsModule listsModule : list) {
            setModeData(listsModule);
        }
    }

    /**
     * 生成唯一的键，用于判断条目的唯一性
     * 该键是由 vulType 和 vendor 组成的字符串
     */
    private static String generateUniqueKey(ListsModule listsModule) {
        return listsModule.getVulType() + "-" + listsModule.getVendor() + "-" + listsModule.getURL();
    }
}
