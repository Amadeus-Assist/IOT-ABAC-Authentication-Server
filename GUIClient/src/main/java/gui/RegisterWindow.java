package gui;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.util.EntityUtils;
import service.ClientContext;
import service.ClientService;
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

public class RegisterWindow {
    private final JFrame mainFrame;
    private JPanel mainPanel;
    private JTextField usernameTextField;
    private JTextField passwordTextField;
    private JButton submitButton;
    private JLabel usernameLabel;
    private JLabel passwordLabel;
    private JLabel titleLabel;
    private JScrollPane loggerScrollPane;
    private JLabel attributesLabel;
    private JTextArea attributesTextArea;
    private JScrollPane attributesJScrollPane;
    private JTextPane loggerTextPane;
    private JButton backButton;
    private ClientContext context;
    private JFrame clientFrame;

    public RegisterWindow(TestClient client) {
        this.mainFrame = new JFrame("Register");
        this.clientFrame = client.getMainFrame();
        this.context = client.getContext();
        this.loggerTextPane.setDocument(client.getLoggerTextPane().getStyledDocument());
        this.mainFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                mainFrame.dispose();
                clientFrame.setVisible(true);
                clientFrame.setEnabled(true);
                clientFrame.setFocusable(true);
                clientFrame.requestFocus();
            }
        });

        this.backButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                mainFrame.dispatchEvent(new WindowEvent(mainFrame, WindowEvent.WINDOW_CLOSING));
            }
        });

        this.submitButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                String username = usernameTextField.getText();
                String password = passwordTextField.getText();
                String attrs = attributesTextArea.getText();
                if (!Utils.hasText(username) || !Utils.hasText(password) || !Utils.hasText(attrs)) {
                    Utils.appendToPane(loggerTextPane, "err: incomplete register info\n", Color.red);
                    return;
                }
                String body;
                try {
                    body = ClientService.generateUserRegisterBody(username, password, attrs);
                } catch (JsonProcessingException jsonProcessingException) {
                    Utils.appendToPane(loggerTextPane, "err: JsonProcessingException\n", Color.red);
                    return;
                }
                assert Utils.hasText(body);
                Utils.appendToPane(loggerTextPane, "request sent, body:\n" + body + "\n", Color.black);
                CloseableHttpResponse response;
                try {
                    response = ClientService.sendHttpPostRequest(context.getClient(),
                            Constants.USER_REG_URL, body);
                } catch (IOException ioException) {
                    Utils.appendToPane(loggerTextPane, "err: send register info error\n", Color.red);
                    return;
                }
                assert response != null;
                if (response.getStatusLine().getStatusCode() == 200) {
                    Utils.appendToPane(loggerTextPane, "register success!\n", Color.blue);
                } else {
                    String responseBodyStr;
                    try {
                        responseBodyStr = EntityUtils.toString(response.getEntity(), "UTF-8");
                    } catch (IOException ioException) {
                        Utils.appendToPane(loggerTextPane, "err: read response body exception\n", Color.red);
                        return;
                    }
                    assert Utils.hasText(responseBodyStr);
                    Utils.appendToPane(loggerTextPane, "register fail, returned body:\n" + responseBodyStr + "\n",
                            Color.red);
                }
            }
        });
    }

    public void run() {
        this.loggerTextPane.setEditable(false);
        this.mainFrame.setContentPane(this.mainPanel);
        this.mainFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.mainFrame.pack();
        this.mainFrame.setLocationRelativeTo(clientFrame);
        this.mainFrame.setVisible(true);
    }


}
