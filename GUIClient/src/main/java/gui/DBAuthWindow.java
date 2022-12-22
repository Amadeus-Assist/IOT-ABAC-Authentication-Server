package gui;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.http.util.EntityUtils;
import pojo.AccessResponse;
import pojo.DevInfo;
import service.ClientContext;
import service.ClientService;
import utils.Utils;

import org.apache.http.client.methods.CloseableHttpResponse;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Collections;
public class DBAuthWindow extends JDialog {
    private JList<String> tablesJList;
    private JList<String> permissionsJList;
    private JButton sendButton;
    private JLabel permissionsListLabel;
    private JLabel tablesListLabel;
    private final JFrame mainFrame;
    private JPanel mainPanel;

    private JLabel titleLabel;
    private JScrollPane permScrollPane;
    private JTextPane loggerTextPane;
    private JScrollPane tablesScrollPane;
    private JScrollPane permissionsScrollPane;
    private JButton getButton;
    private JScrollPane loggerScrollPane;
    private JButton addButton;
    private JTextPane permTextPane;
    private JScrollPane tablesListJScrollPane;
    private JScrollPane permissionsJScrollPane;
    private final ClientContext context;
    private final JFrame clientFrame;

    private final JFrame operateFrame;
    private final DefaultListModel<String> tablesListModel;
    private Map<String, DefaultListModel<String>> actionModelMap;
    private final DBAuthWindow thisWindow;
    //    private volatile boolean isSearching;
    private final DefaultListModel<String> emptyModel;

    private Map<String, String> permMap;

    public DBAuthWindow(TestClient client, OperateWindow operate, CloseableHttpResponse response, String targetDevId) {
        this.mainFrame = new JFrame("DB Auth Window");
        this.operateFrame = operate.getMainFrame();
        this.clientFrame = client.getMainFrame();
        this.context = client.getContext();
        this.loggerTextPane.setDocument(client.getLoggerTextPane().getStyledDocument());
        this.tablesListModel = new DefaultListModel<>();
        this.actionModelMap = new HashMap<>();
        this.thisWindow = this;
        this.emptyModel = new DefaultListModel<>();
        this.permMap =  new HashMap<String, String>();

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

        this.getButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                sendButton.setEnabled(false);
                Utils.appendToPane(loggerTextPane, "checking required tables...\n", Color.black);
                new Thread(() -> {
                    Map<String, DefaultListModel<String>> newModelMap = new HashMap<>();
                    context.getSearchService().search();
                    tablesListModel.clear();
                    context.getSearchService().getNearByDevices().forEach((k, v) -> {
                        tablesListModel.addElement("user_attrs");
                        DefaultListModel<String> localModel = new DefaultListModel<>();
                        String[] permissions = {"allow once", "allow in 7 days", "allow in 30 days", "always allow", "deny once", "deny in 7 days", "deny in 30 days", "always deny"};
                        for (String perm:permissions) {
                            localModel.addElement(perm);
                        }
                        newModelMap.put("user_attrs", localModel);
                    });
                    thisWindow.actionModelMap = newModelMap;
                    if (!tablesListModel.isEmpty()) {
                        tablesJList.setSelectedIndex(0);
                    }
                    Utils.appendToPane(loggerTextPane, "required tables: " + newModelMap + "\n", Color.black);
//                    isSearching = false;
                    sendButton.setEnabled(true);
                }).start();
            }
        });

        this.tablesJList.addListSelectionListener(e -> {
//            System.out.println("actionModelMap: " + actionModelMap);
            permissionsJList.setModel(tablesJList.getSelectedValue() == null ? emptyModel :
                    actionModelMap.get(tablesJList.getSelectedValue()) == null ? emptyModel :
                            actionModelMap.get(tablesJList.getSelectedValue()));
        });

        this.addButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                String targetTable = tablesJList.getSelectedValue();
                String targetPerm = permissionsJList.getSelectedValue();
                if(!Utils.hasText(targetTable) || !Utils.hasText(targetPerm)) {
                    Utils.appendToPane(loggerTextPane, "Please select table and permission\n", Color.red);
                    return;
                }
                Utils.appendToPane(permTextPane, targetTable + ": " + targetPerm + "\n", Color.blue);
                permMap.put(targetTable, targetPerm);
            }
        });

        this.sendButton.addMouseListener(new MouseAdapter() { //send and quit
            @Override
            public void mouseClicked(MouseEvent e) {
                Map<String, DevInfo> nearByDevices = context.getSearchService().getNearByDevices();
                DevInfo targetDevInfo = nearByDevices.get(targetDevId);
                String url = "http://localhost:" + targetDevInfo.getPort() + "/iot-client/access-request";
                CloseableHttpResponse response2;
                try {
                    String accessRequestBody = ClientService.generateSecureAccessRequestBodyFromUser(context.getUsername(),
                            context.getPassword(), targetDevId, "read", "true", "user_attrs:" + permMap.get("user_attrs"));
                    Utils.appendToPane(loggerTextPane, "send secure access request\n" + accessRequestBody + "\n", Color.black);
                    response2 = ClientService.sendHttpPostRequest(context.getClient(), url, accessRequestBody);
                } catch (IOException ioException) {
                    Utils.appendToPane(loggerTextPane, "error send secure access request: "+ioException.getMessage()+ "\n", Color.red);
                    return;
                }
                assert response2 != null;
                String responseBodyStr2;
                try {
                    responseBodyStr2 = EntityUtils.toString(response2.getEntity(), "UTF-8");
                } catch (IOException ioException) {
                    Utils.appendToPane(loggerTextPane, "err: read response body exception\n", Color.red);
                    return;
                }
                assert Utils.hasText(responseBodyStr2);
                if (response2.getStatusLine().getStatusCode() == 200) {
                    try {
                        AccessResponse decision2 = Utils.mapper.readValue(responseBodyStr2, AccessResponse.class);
                        Utils.appendToPane(loggerTextPane, "decision: " + decision2.getDecision() + "\n", Color.blue);
                    } catch (JsonProcessingException jsonProcessingException) {
                        Utils.appendToPane(loggerTextPane, "secure db bad response, can't parse" + responseBodyStr2 + "\n", Color.red);
                    }
                } else {
                    Utils.appendToPane(loggerTextPane, "evaluate failed, returned body:\n" + responseBodyStr2 + "\n",
                            Color.red);
                }
                mainFrame.dispose();
                operateFrame.setEnabled(true);
                operateFrame.setVisible(true);
                operateFrame.setFocusable(true);
                operateFrame.requestFocus();
            }
        });

    }

    public Map<String, String> run() {
        Utils.appendToPane(permTextPane, "Your permission:\n", Color.black);
        this.tablesJList.setModel(this.tablesListModel);
        this.loggerTextPane.setEditable(false);
        this.permTextPane.setEditable(false);
        this.mainFrame.setContentPane(this.mainPanel);
        this.mainFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        this.mainFrame.pack();
        this.mainFrame.setLocationRelativeTo(clientFrame);
        this.mainFrame.setVisible(true);
        this.getButton.doClick();
        return permMap;
    }

}
