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
                    Utils.appendToPane(loggerTextPane, "err: incomplete login info\n", Color.blue);
                    return;
                }
                String body;
                try {
                    body = ClientService.generateUserLoginBody(username, password);
                } catch (JsonProcessingException jsonProcessingException) {
                    Utils.appendToPane(loggerTextPane, "err: JsonProcessingException\n", Color.pink);
                    return;
                }
                assert Utils.hasText(body);
                Utils.appendToPane(loggerTextPane, "request sent, body:\n" + body + "\n", Color.black);
                CloseableHttpResponse response;
                try {
                    response = ClientService.sendHttpPostRequest(context.getClient(),
                            Constants.USER_LOGIN_URL, body);
                } catch (IOException ioException) {
                    Utils.appendToPane(loggerTextPane, "err: send login info error\n", Color.green);
                    return;
                }
                assert response != null;
                if (response.getStatusLine().getStatusCode() == 200) {
                    context.setUsername(username);
                    context.setPassword(password);
                    Utils.appendToPane(loggerTextPane, "login success!\n", Color.black);
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


    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }

}
