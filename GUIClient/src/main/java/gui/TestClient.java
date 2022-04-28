package gui;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import service.ClientContext;
import service.ClientService;
import service.SearchService;
import utils.Constants;
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
import java.util.Locale;

public class TestClient {
    private final JFrame mainFrame;
    private JPanel mainPanel;
    private JTextField usernameTextField;
    private JTextField passwordTextField;
    private JButton logInButton;
    private JButton registerButton;
    private JLabel usernameLabel;
    private JLabel passwordLabel;
    private JLabel titleLabel;
    private JScrollPane loggerScrollPane;
    private JTextPane loggerTextPane;
    private final ClientContext context;
    private TestClient me;

    public TestClient(ClientContext context) {
        this.mainFrame = new JFrame("Test Client");
        this.context = context;
    }

    public JFrame getMainFrame() {
        return this.mainFrame;
    }

    public JTextPane getLoggerTextPane() {
        return this.loggerTextPane;
    }

    public ClientContext getContext() {
        return this.context;
    }

    public void run() {
        this.mainFrame.setContentPane(this.mainPanel);
        this.mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.mainFrame.pack();
        this.mainFrame.setLocationRelativeTo(null);
        this.mainFrame.setVisible(true);
        this.me = this;
        this.loggerTextPane.setEditable(false);

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

        registerButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                new RegisterWindow(me).run();
                mainFrame.setFocusable(false);
                mainFrame.setEnabled(false);
                mainFrame.setVisible(false);
            }
        });

        logInButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                String username = usernameTextField.getText();
                String password = passwordTextField.getText();
                if (!Utils.hasText(username) || !Utils.hasText(password)) {
                    Utils.appendToPane(loggerTextPane, "err: incomplete login info\n", Color.red);
                    return;
                }
                String body;
                try {
                    body = ClientService.generateUserLoginBody(username, password);
                } catch (JsonProcessingException jsonProcessingException) {
                    Utils.appendToPane(loggerTextPane, "err: JsonProcessingException\n", Color.red);
                    return;
                }
                assert Utils.hasText(body);
                Utils.appendToPane(loggerTextPane, "request sent, body:\n" + body + "\n", Color.black);
                CloseableHttpResponse response;
                try {
                    response = ClientService.sendHttpPostRequest(context.getClient(),
                            Constants.USER_LOGIN_URL, body);
                } catch (IOException ioException) {
                    Utils.appendToPane(loggerTextPane, "err: send login info error\n", Color.red);
                    return;
                }
                assert response != null;
                if (response.getStatusLine().getStatusCode() == 200) {
                    context.setUsername(username);
                    context.setPassword(password);
                    Utils.appendToPane(loggerTextPane, "login success!\n", Color.blue);
                    new OperateWindow(me).run();
                    mainFrame.setFocusable(false);
                    mainFrame.setEnabled(false);
                    mainFrame.setVisible(false);
                } else {
                    String responseBodyStr;
                    try {
                        responseBodyStr = EntityUtils.toString(response.getEntity(), "UTF-8");
                    } catch (IOException ioException) {
                        Utils.appendToPane(loggerTextPane, "err: read response body exception\n", Color.red);
                        return;
                    }
                    assert Utils.hasText(responseBodyStr);
                    Utils.appendToPane(loggerTextPane, "login fail, returned body:\n" + responseBodyStr + "\n",
                            Color.red);
                }
            }

        });
    }

    public static void main(String[] args) {
        ClientContext context = new ClientContext();
        context.setClient(HttpClients.createDefault());
        TestClient client = new TestClient(context);
        SearchService searchService = new SearchService(-1, client.getLoggerTextPane());
        context.setSearchService(searchService);
        client.run();
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
        mainPanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(5, 9, new Insets(10, 20, 20, 20), -1,
                -1));
        usernameLabel = new JLabel();
        usernameLabel.setText("Username");
        mainPanel.add(usernameLabel, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 1,
                com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST,
                com.intellij.uiDesigner.core.GridConstraints.FILL_NONE,
                com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        usernameTextField = new JTextField();
        usernameTextField.setColumns(0);
        mainPanel.add(usernameTextField, new com.intellij.uiDesigner.core.GridConstraints(1, 2, 1, 5,
                com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST,
                com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL,
                com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW,
                com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(220, -1), null, 0,
                false));
        passwordLabel = new JLabel();
        passwordLabel.setText("Password");
        mainPanel.add(passwordLabel, new com.intellij.uiDesigner.core.GridConstraints(2, 1, 1, 1,
                com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST,
                com.intellij.uiDesigner.core.GridConstraints.FILL_NONE,
                com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        passwordTextField = new JTextField();
        passwordTextField.setText("");
        mainPanel.add(passwordTextField, new com.intellij.uiDesigner.core.GridConstraints(2, 2, 1, 5,
                com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST,
                com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL,
                com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW,
                com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(100, -1), null, 0,
                false));
        logInButton = new JButton();
        logInButton.setText("Log In");
        mainPanel.add(logInButton, new com.intellij.uiDesigner.core.GridConstraints(3, 6, 1, 1,
                com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER,
                com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL,
                com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        registerButton = new JButton();
        registerButton.setText("Register");
        mainPanel.add(registerButton, new com.intellij.uiDesigner.core.GridConstraints(3, 5, 1, 1,
                com.intellij.uiDesigner.core.GridConstraints.ANCHOR_EAST,
                com.intellij.uiDesigner.core.GridConstraints.FILL_NONE,
                com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final com.intellij.uiDesigner.core.Spacer spacer1 = new com.intellij.uiDesigner.core.Spacer();
        mainPanel.add(spacer1, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1,
                com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER,
                com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL,
                com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final com.intellij.uiDesigner.core.Spacer spacer2 = new com.intellij.uiDesigner.core.Spacer();
        mainPanel.add(spacer2, new com.intellij.uiDesigner.core.GridConstraints(1, 8, 1, 1,
                com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER,
                com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL,
                com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final com.intellij.uiDesigner.core.Spacer spacer3 = new com.intellij.uiDesigner.core.Spacer();
        mainPanel.add(spacer3, new com.intellij.uiDesigner.core.GridConstraints(3, 4, 1, 1,
                com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER,
                com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL,
                com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        titleLabel = new JLabel();
        Font titleLabelFont = this.$$$getFont$$$(null, -1, 18, titleLabel.getFont());
        if (titleLabelFont != null) titleLabel.setFont(titleLabelFont);
        titleLabel.setHorizontalAlignment(0);
        titleLabel.setText("Welcome to the Authorization Service");
        mainPanel.add(titleLabel, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 9,
                com.intellij.uiDesigner.core.GridConstraints.ANCHOR_NORTH,
                com.intellij.uiDesigner.core.GridConstraints.FILL_NONE,
                com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW,
                com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(320, 40), null, 0,
                false));
        loggerScrollPane = new JScrollPane();
        mainPanel.add(loggerScrollPane, new com.intellij.uiDesigner.core.GridConstraints(4, 1, 1, 6,
                com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER,
                com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH,
                com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(-1, 200), null, 0, false));
        loggerTextPane = new JTextPane();
        loggerScrollPane.setViewportView(loggerTextPane);
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
