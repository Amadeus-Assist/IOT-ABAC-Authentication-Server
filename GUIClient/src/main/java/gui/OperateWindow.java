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

    public JFrame getMainFrame() {
        return this.mainFrame;
    }

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
                    Utils.appendToPane(loggerTextPane, "error send access request: "+ioException.getMessage()+ "\n", Color.red);
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
                        if (decision.getDecision().equals("dbauth")) {
                            Map<String, String> permMap = new DBAuthWindow(client, thisWindow, response, targetDevId).run();
                            mainFrame.setFocusable(false);
                            mainFrame.setEnabled(false);
                            mainFrame.setVisible(false);
                            if (!permMap.isEmpty()) {
                                for (String str : permMap.keySet()) {
                                    Utils.appendToPane(loggerTextPane, str + "\n", Color.orange);
                                }
                            }
                        }
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

}
