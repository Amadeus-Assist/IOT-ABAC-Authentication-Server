package gui;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.util.EntityUtils;
import pojo.AccessResponse;
import pojo.DevInfo;
import service.ClientContext;
import service.ClientService;
import utils.Utils;

import javax.swing.*;
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
    //    private volatile boolean isSearching;
    private final DefaultListModel<String> emptyModel;

    public OperateWindow(TestClient client) {
        this.mainFrame = new JFrame("Operating Window");
        this.clientFrame = client.getMainFrame();
        this.context = client.getContext();
        this.loggerTextPane.setDocument(client.getLoggerTextPane().getStyledDocument());
        this.iotListModel = new DefaultListModel<>();
        this.actionModelMap = new HashMap<>();
        this.thisWindow = this;
        this.emptyModel = new DefaultListModel<>();

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
//                if (isSearching) {
//                    return;
//                }
//                isSearching = true;
                searchButton.setEnabled(false);
                sendButton.setEnabled(false);
                Utils.appendToPane(loggerTextPane, "searching nearby devices...\n", Color.black);
                new Thread(() -> {
                    Map<String, DefaultListModel<String>> newModelMap = new HashMap<>();
                    context.getSearchService().search();
                    iotListModel.clear();
                    context.getSearchService().getNearByDevices().forEach((k, v) -> {
                        iotListModel.addElement(k);
                        DefaultListModel<String> localModel = new DefaultListModel<>();
                        String[] actions = v.getActions().split("/");
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
//                    isSearching = false;
                    searchButton.setEnabled(true);
                    sendButton.setEnabled(true);
                }).start();
            }
        });

        this.iotJList.addListSelectionListener(e -> {
//            System.out.println("actionModelMap: " + actionModelMap);
            actionsJList.setModel(iotJList.getSelectedValue() == null ? emptyModel :
                    actionModelMap.get(iotJList.getSelectedValue()) == null ? emptyModel :
                            actionModelMap.get(iotJList.getSelectedValue()));
//            System.out.println("selected index: " + iotJList.getSelectedIndex());
        });

        this.sendButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                String targetDevId = iotJList.getSelectedValue();
                String targetAction = actionsJList.getSelectedValue();
                if (!Utils.hasText(targetDevId) || !Utils.hasText(targetAction)) {
                    Utils.appendToPane(loggerTextPane, "Please select the device and action first!\n", Color.red);
                    return;
                }
                Map<String, DevInfo> nearByDevices = context.getSearchService().getNearByDevices();
                DevInfo targetDevInfo = nearByDevices.get(targetDevId);
                String url = "http://localhost:" + targetDevInfo.getPort() + "/iot-client/access-request";
                CloseableHttpResponse response;
                try {
                    String accessRequestBody = ClientService.generateAccessRequestBodyFromUser(context.getUsername(),
                            context.getPassword(), targetDevId, targetAction);
                    Utils.appendToPane(loggerTextPane, "send access request\n" + accessRequestBody + "\n", Color.black);
                    response = ClientService.sendHttpPostRequest(context.getClient(), url, accessRequestBody);
                } catch (IOException ioException) {
                    Utils.appendToPane(loggerTextPane, "error send access request\n", Color.red);
                    return;
                }
                assert response != null;
                String responseBodyStr;
                try {
                    responseBodyStr = EntityUtils.toString(response.getEntity(), "UTF-8");
                } catch (IOException ioException) {
                    Utils.appendToPane(loggerTextPane, "err: read response body exception\n", Color.red);
                    return;
                }
                assert Utils.hasText(responseBodyStr);

                if (response.getStatusLine().getStatusCode() == 200) {
                    try {
                        AccessResponse decision = Utils.mapper.readValue(responseBodyStr, AccessResponse.class);
                        Utils.appendToPane(loggerTextPane, "decision: " + decision.getDecision() + "\n", Color.blue);
                    } catch (JsonProcessingException jsonProcessingException) {
                        Utils.appendToPane(loggerTextPane, "bad response, can't parse\n", Color.red);
                    }
                } else {
                    Utils.appendToPane(loggerTextPane, "evaluate failed, returned body:\n" + responseBodyStr + "\n",
                            Color.red);
                }
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
        mainPanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(5, 8, new Insets(10, 20, 20, 20), -1,
                -1));
        iotListLabel = new JLabel();
        iotListLabel.setText("IoT List");
        mainPanel.add(iotListLabel, new com.intellij.uiDesigner.core.GridConstraints(1, 3, 1, 1,
                com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST,
                com.intellij.uiDesigner.core.GridConstraints.FILL_NONE,
                com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        actionsLabel = new JLabel();
        actionsLabel.setText("Actions");
        mainPanel.add(actionsLabel, new com.intellij.uiDesigner.core.GridConstraints(2, 3, 1, 1,
                com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST,
                com.intellij.uiDesigner.core.GridConstraints.FILL_NONE,
                com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        searchButton = new JButton();
        searchButton.setText("search");
        mainPanel.add(searchButton, new com.intellij.uiDesigner.core.GridConstraints(3, 5, 1, 1,
                com.intellij.uiDesigner.core.GridConstraints.ANCHOR_EAST,
                com.intellij.uiDesigner.core.GridConstraints.FILL_NONE,
                com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, new Dimension(80, -1), 0, false));
        titleLabel = new JLabel();
        Font titleLabelFont = this.$$$getFont$$$(null, -1, 18, titleLabel.getFont());
        if (titleLabelFont != null) titleLabel.setFont(titleLabelFont);
        titleLabel.setHorizontalAlignment(0);
        titleLabel.setText("Authorization Service");
        mainPanel.add(titleLabel, new com.intellij.uiDesigner.core.GridConstraints(0, 2, 1, 5,
                com.intellij.uiDesigner.core.GridConstraints.ANCHOR_NORTH,
                com.intellij.uiDesigner.core.GridConstraints.FILL_NONE,
                com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW,
                com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(320, 40), null, 0,
                false));
        loggerScrollPane = new JScrollPane();
        mainPanel.add(loggerScrollPane, new com.intellij.uiDesigner.core.GridConstraints(4, 3, 1, 4,
                com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER,
                com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH,
                com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(-1, 200), new Dimension(400, -1), 0, false));
        loggerTextPane = new JTextPane();
        loggerScrollPane.setViewportView(loggerTextPane);
        iotListJScrollPane = new JScrollPane();
        mainPanel.add(iotListJScrollPane, new com.intellij.uiDesigner.core.GridConstraints(1, 4, 1, 3,
                com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER,
                com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH,
                com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(220, -1), new Dimension(300, -1), 0, false));
        iotJList = new JList();
        iotListJScrollPane.setViewportView(iotJList);
        actionsJScrollPane = new JScrollPane();
        mainPanel.add(actionsJScrollPane, new com.intellij.uiDesigner.core.GridConstraints(2, 4, 1, 3,
                com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER,
                com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH,
                com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, null, new Dimension(300, -1), 0, false));
        actionsJList = new JList();
        actionsJScrollPane.setViewportView(actionsJList);
        logOutButton = new JButton();
        logOutButton.setText("log out");
        mainPanel.add(logOutButton, new com.intellij.uiDesigner.core.GridConstraints(3, 4, 1, 1,
                com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER,
                com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL,
                com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, new Dimension(80, -1), 0, false));
        sendButton = new JButton();
        sendButton.setText("send");
        mainPanel.add(sendButton, new com.intellij.uiDesigner.core.GridConstraints(3, 6, 1, 1,
                com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER,
                com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL,
                com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, new Dimension(80, -1), 1, false));
        final com.intellij.uiDesigner.core.Spacer spacer1 = new com.intellij.uiDesigner.core.Spacer();
        mainPanel.add(spacer1, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 2,
                com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER,
                com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL,
                com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final com.intellij.uiDesigner.core.Spacer spacer2 = new com.intellij.uiDesigner.core.Spacer();
        mainPanel.add(spacer2, new com.intellij.uiDesigner.core.GridConstraints(1, 7, 1, 1,
                com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER,
                com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL,
                com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final com.intellij.uiDesigner.core.Spacer spacer3 = new com.intellij.uiDesigner.core.Spacer();
        mainPanel.add(spacer3, new com.intellij.uiDesigner.core.GridConstraints(3, 7, 1, 1,
                com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER,
                com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL,
                com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final com.intellij.uiDesigner.core.Spacer spacer4 = new com.intellij.uiDesigner.core.Spacer();
        mainPanel.add(spacer4, new com.intellij.uiDesigner.core.GridConstraints(3, 1, 1, 3,
                com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER,
                com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL,
                com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final com.intellij.uiDesigner.core.Spacer spacer5 = new com.intellij.uiDesigner.core.Spacer();
        mainPanel.add(spacer5, new com.intellij.uiDesigner.core.GridConstraints(4, 0, 1, 3,
                com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER,
                com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL,
                com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final com.intellij.uiDesigner.core.Spacer spacer6 = new com.intellij.uiDesigner.core.Spacer();
        mainPanel.add(spacer6, new com.intellij.uiDesigner.core.GridConstraints(4, 7, 1, 1,
                com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER,
                com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL,
                com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
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
