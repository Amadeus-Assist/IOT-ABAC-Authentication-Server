package gui;

import service.ClientContext;
import utils.Utils;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.plaf.FontUIResource;
import javax.swing.text.StyleContext;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class OperateWindow {
    private JList<String> iotJList;
    private JList<String> actionsJList;
    private JButton logOutButton;
    private JButton sendButton;
    private JButton searchButton;
    private JLabel actionsLabel;
    private JLabel iotListLabel;
    private final JFrame mainFrame;
    private JPanel mainPanel;
    private JLabel titleLabel;
    private JScrollPane loggerScrollPane;
    private JTextPane loggerTextPane;
    private JScrollPane iotListJScrollPane;
    private JScrollPane actionsJScrollPane;
    private final ClientContext context;
    private final JFrame clientFrame;
    private final DefaultListModel<String> iotListModel;
    private Map<String, DefaultListModel<String>> actionModelMap;
    private final OperateWindow thisWindow;
    private volatile boolean isSearching;
    private final DefaultListModel<String> emptyMdodel;

    public OperateWindow(TestClient client) {
        this.mainFrame = new JFrame("Operating Window");
        this.clientFrame = client.getMainFrame();
        this.context = client.getContext();
        this.loggerTextPane.setDocument(client.getLoggerTextPane().getStyledDocument());
        this.iotListModel = new DefaultListModel<>();
        this.actionModelMap = new HashMap<>();
        this.thisWindow = this;
        this.isSearching = false;
        this.emptyMdodel = new DefaultListModel<>();

        this.mainFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                try {
                    context.getClient().close();
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
                context.getSearchService().close();
            }
        });

        this.logOutButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                mainFrame.dispose();
                clientFrame.setEnabled(true);
                clientFrame.setVisible(true);
                clientFrame.setFocusable(true);
                clientFrame.requestFocus();
            }
        });

        this.searchButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (isSearching) {
                    return;
                }
                isSearching = true;
                Utils.appendToPane(loggerTextPane, "searching nearby devices...\n", Color.black);
                new Thread(() -> {
                    Map<String, DefaultListModel<String>> newModelMap = new HashMap<>();
                    context.getSearchService().search();
                    iotListModel.clear();
                    context.getSearchService().getNearByDevices().forEach((k, v) -> {
                        iotListModel.addElement(k);
                        DefaultListModel<String> localModel = new DefaultListModel<>();
                        String[] actions = v.split("/");
                        for (String act : actions) {
                            localModel.addElement(act);
                        }
                        newModelMap.put(k, localModel);
                    });
                    thisWindow.actionModelMap = newModelMap;
                    if (!iotListModel.isEmpty()) {
                        iotJList.setSelectedIndex(0);
                    }
                    Utils.appendToPane(loggerTextPane, "nearby devices: " + newModelMap + "\n", Color.black);
                    isSearching = false;
                }).start();
            }
        });

        this.iotJList.addListSelectionListener(e -> {
            System.out.println("actionModelMap: " + actionModelMap);
            actionsJList.setModel(iotJList.getSelectedValue() == null ? emptyMdodel :
                    actionModelMap.get(iotJList.getSelectedValue()) == null ? emptyMdodel :
                            actionModelMap.get(iotJList.getSelectedValue()));
            System.out.println("selected index: " + iotJList.getSelectedIndex());
        });

        this.sendButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
            }
        });
    }

    public void run() {
        this.iotJList.setModel(this.iotListModel);
        this.loggerTextPane.setEditable(false);
        this.mainFrame.setContentPane(this.mainPanel);
        this.mainFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        this.mainFrame.pack();
        this.mainFrame.setLocationRelativeTo(clientFrame);
        this.mainFrame.setVisible(true);
        this.searchButton.dispatchEvent(new MouseEvent(searchButton, MouseEvent.MOUSE_CLICKED,
                System.currentTimeMillis(), 0, 0, 0, 1, false));
//        this.searchButton.doClick();
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        mainPanel = new JPanel();
        mainPanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(5, 10, new Insets(10, 20, 20, 20), -1,
                -1));
        iotListLabel = new JLabel();
        iotListLabel.setText("IoT List");
        mainPanel.add(iotListLabel, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 1,
                com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST,
                com.intellij.uiDesigner.core.GridConstraints.FILL_NONE,
                com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        actionsLabel = new JLabel();
        actionsLabel.setText("Actions");
        mainPanel.add(actionsLabel, new com.intellij.uiDesigner.core.GridConstraints(2, 1, 1, 1,
                com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST,
                com.intellij.uiDesigner.core.GridConstraints.FILL_NONE,
                com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        sendButton = new JButton();
        sendButton.setText("send");
        mainPanel.add(sendButton, new com.intellij.uiDesigner.core.GridConstraints(3, 7, 1, 1,
                com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER,
                com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL,
                com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        searchButton = new JButton();
        searchButton.setText("search");
        mainPanel.add(searchButton, new com.intellij.uiDesigner.core.GridConstraints(3, 6, 1, 1,
                com.intellij.uiDesigner.core.GridConstraints.ANCHOR_EAST,
                com.intellij.uiDesigner.core.GridConstraints.FILL_NONE,
                com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final com.intellij.uiDesigner.core.Spacer spacer1 = new com.intellij.uiDesigner.core.Spacer();
        mainPanel.add(spacer1, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1,
                com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER,
                com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL,
                com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final com.intellij.uiDesigner.core.Spacer spacer2 = new com.intellij.uiDesigner.core.Spacer();
        mainPanel.add(spacer2, new com.intellij.uiDesigner.core.GridConstraints(1, 9, 1, 1,
                com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER,
                com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL,
                com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final com.intellij.uiDesigner.core.Spacer spacer3 = new com.intellij.uiDesigner.core.Spacer();
        mainPanel.add(spacer3, new com.intellij.uiDesigner.core.GridConstraints(3, 1, 1, 4,
                com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER,
                com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL,
                com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        titleLabel = new JLabel();
        Font titleLabelFont = this.$$$getFont$$$(null, -1, 18, titleLabel.getFont());
        if (titleLabelFont != null) titleLabel.setFont(titleLabelFont);
        titleLabel.setHorizontalAlignment(0);
        titleLabel.setText("Authorization Service");
        mainPanel.add(titleLabel, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 10,
                com.intellij.uiDesigner.core.GridConstraints.ANCHOR_NORTH,
                com.intellij.uiDesigner.core.GridConstraints.FILL_NONE,
                com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW,
                com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(320, 40), null, 0,
                false));
        loggerScrollPane = new JScrollPane();
        mainPanel.add(loggerScrollPane, new com.intellij.uiDesigner.core.GridConstraints(4, 1, 1, 7,
                com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER,
                com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH,
                com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(-1, 200), null, 0, false));
        loggerTextPane = new JTextPane();
        loggerScrollPane.setViewportView(loggerTextPane);
        iotListJScrollPane = new JScrollPane();
        mainPanel.add(iotListJScrollPane, new com.intellij.uiDesigner.core.GridConstraints(1, 2, 1, 6,
                com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER,
                com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH,
                com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(220, -1), null, 0, false));
        iotJList = new JList();
        iotListJScrollPane.setViewportView(iotJList);
        actionsJScrollPane = new JScrollPane();
        mainPanel.add(actionsJScrollPane, new com.intellij.uiDesigner.core.GridConstraints(2, 2, 1, 6,
                com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER,
                com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH,
                com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        actionsJList = new JList();
        actionsJScrollPane.setViewportView(actionsJList);
        logOutButton = new JButton();
        logOutButton.setText("log out");
        mainPanel.add(logOutButton, new com.intellij.uiDesigner.core.GridConstraints(3, 5, 1, 1,
                com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER,
                com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL,
                com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    private Font $$$getFont$$$(String fontName, int style, int size, Font currentFont) {
        if (currentFont == null) return null;
        String resultName;
        if (fontName == null) {
            resultName = currentFont.getName();
        } else {
            Font testFont = new Font(fontName, Font.PLAIN, 10);
            if (testFont.canDisplay('a') && testFont.canDisplay('1')) {
                resultName = fontName;
            } else {
                resultName = currentFont.getName();
            }
        }
        Font font = new Font(resultName, style >= 0 ? style : currentFont.getStyle(), size >= 0 ? size :
                currentFont.getSize());
        boolean isMac = System.getProperty("os.name", "").toLowerCase(Locale.ENGLISH).startsWith("mac");
        Font fontWithFallback = isMac ? new Font(font.getFamily(), font.getStyle(), font.getSize()) :
                new StyleContext().getFont(font.getFamily(), font.getStyle(), font.getSize());
        return fontWithFallback instanceof FontUIResource ? fontWithFallback : new FontUIResource(fontWithFallback);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }

}
