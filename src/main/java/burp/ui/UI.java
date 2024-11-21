package burp.ui;

import burp.Main;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.scanner.AuditResult;
import burp.api.montoya.scanner.audit.issues.AuditIssue;
import burp.api.montoya.ui.editor.HttpRequestEditor;
import burp.api.montoya.ui.editor.HttpResponseEditor;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

public class UI {
    private static JTable currentTable;                   // 表格组件
    private static JPanel tableJPanel;                    // 用于放置表格的面板
    private static HttpRequestEditor requestEditor;       // HTTP 请求编辑器
    private static HttpResponseEditor httpResponseEditor; // HTTP 响应编辑器
    private static int num = 0;                           // 序号计数器
    private static Map<String, String> regexConfigurations = new HashMap<>(); // 存储正则配置

    /**
     * 创建表格并设置列标题
     */
    private static Component createTable() {
        // 为表格添加新的“厂商”列
        String[] tableTitle = new String[]{"序号", "url", "匹配参数", "厂商"};

        // 设置表格和编辑权限
        currentTable = new JTable() {
            public boolean isCellEditable(int row, int col) {
                // 只允许第三列可编辑
                return col == 2;
            }
        };
        currentTable.getTableHeader().setReorderingAllowed(false);  // 禁止列拖动

        // 设置表格模型
        DefaultTableModel model = (DefaultTableModel) currentTable.getModel();
        model.setColumnIdentifiers(tableTitle); // 设置表格列标题
        currentTable.setModel(model);

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

        // 将表格放入滚动面板
        return new JScrollPane(currentTable);
    }

    /**
     * 创建配置菜单按钮并弹出配置窗口
     */
    private static void createConfigMenu(JPanel mainPane) {
        // 创建菜单栏
        JMenuBar menuBar = new JMenuBar();
        JMenu configMenu = new JMenu("配置");

        // 配置菜单按钮
        JMenuItem configureItem = new JMenuItem("配置正则表达式");
        configureItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // 显示配置窗口
                showConfigWindow();
            }
        });

        configMenu.add(configureItem);
        menuBar.add(configMenu);
        mainPane.add(menuBar, BorderLayout.NORTH);
    }

    /**
     * 显示配置窗口，用于添加和查看正则表达式配置
     */
    private static void showConfigWindow() {
        // 配置窗口
        JFrame configFrame = new JFrame("正则配置");
        configFrame.setSize(400, 300);
        configFrame.setLayout(new BorderLayout());

        // 配置列表
        DefaultTableModel model = new DefaultTableModel(new String[]{"名称", "正则表达式"}, 0);
        JTable configTable = new JTable(model);

        // 填充现有配置
        for (Map.Entry<String, String> entry : regexConfigurations.entrySet()) {
            model.addRow(new Object[]{entry.getKey(), entry.getValue()});
        }

        // 添加正则表达式配置输入框
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new GridLayout(2, 2));

        JLabel nameLabel = new JLabel("名称：");
        JTextField nameField = new JTextField();
        JLabel regexLabel = new JLabel("正则表达式：");
        JTextField regexField = new JTextField();
        inputPanel.add(nameLabel);
        inputPanel.add(nameField);
        inputPanel.add(regexLabel);
        inputPanel.add(regexField);

        // 添加按钮
        JButton addButton = new JButton("添加配置");
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String name = nameField.getText();
                String regex = regexField.getText();
                if (!name.isEmpty() && !regex.isEmpty()) {
                    regexConfigurations.put(name, regex);
                    model.addRow(new Object[]{name, regex});
                    nameField.setText("");
                    regexField.setText("");
                } else {
                    JOptionPane.showMessageDialog(configFrame, "名称和正则表达式不能为空", "错误", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        configFrame.add(new JScrollPane(configTable), BorderLayout.CENTER);
        configFrame.add(inputPanel, BorderLayout.NORTH);
        configFrame.add(addButton, BorderLayout.SOUTH);
        configFrame.setVisible(true);
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

        // 创建配置菜单
        createConfigMenu(mainPane);
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
     */
    private static void setModeData(ListsModule listsModule, String key, String vendor) {
        DefaultTableModel model = (DefaultTableModel) currentTable.getModel();
        // 向表格中添加一行数据，包括新列“厂商”
        model.addRow(new Object[]{num++, listsModule, key, vendor});
        currentTable.setModel(model);
    }

    /**
     * 更新 UI 数据，将新数据添加到表格中
     */
    public static void updateUIData(ListsModule listsModule, String key, String vendor) {
        setModeData(listsModule, key, vendor);
    }
}
