package gui;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import pojo.DevInfo;
import service.ClientIotContext;
import service.ClientService;
import service.LocalHttpServer;
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
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TestIotClient {
    private final JFrame mainFrame;
    private JPanel mainPanel;
    private JTextField devIdTextField;
    private JTextField tokenTextField;
    private JButton logInButton;
    private JButton registerButton;
    private JLabel devIdLabel;
    private JLabel tokenLabel;
    private JLabel titleLabel;
    private JScrollPane loggerScrollPane;
    private JTextPane loggerTextPane;
    private JList<String> actionsList;
    private final DefaultListModel<String> actionsListModel;
    private JButton queryActionsButton;
    private final ClientIotContext context;
    private TestIotClient me;
    private volatile boolean isRunning;
    private ScheduledExecutorService executorService;

    public TestIotClient(ClientIotContext context) {
        this.context = context;
        this.mainFrame = new JFrame("IoT Client");
        this.isRunning = false;
        this.actionsListModel = new DefaultListModel<>();
    }

    public JFrame getMainFrame() {
        return this.mainFrame;
    }

    public JTextPane getLoggerTextPane() {
        return this.loggerTextPane;
    }

    public ClientIotContext getContext() {
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
        this.queryActionsButton.setEnabled(false);
        this.actionsList.setModel(this.actionsListModel);

        this.mainFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (isRunning) {
                    stopPeriodicSearch();
                }
                context.getSearchService().close();
                try {
                    context.getClient().close();
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        });

        registerButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                new RegisterIotWindow(me).run();
                mainFrame.setFocusable(false);
                mainFrame.setEnabled(false);
                mainFrame.setVisible(false);
            }
        });

        logInButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!isRunning) {
                    String devId = devIdTextField.getText();
                    String token = tokenTextField.getText();
                    if (!Utils.hasText(devId) || !Utils.hasText(token)) {
                        Utils.appendToPane(loggerTextPane, "err: incomplete login info\n", Color.red);
                        return;
                    }
                    String body;
                    try {
                        body = ClientService.generateDevLoginBody(devId, token);
                    } catch (JsonProcessingException jsonProcessingException) {
                        Utils.appendToPane(loggerTextPane, "err: JsonProcessingException\n", Color.red);
                        return;
                    }
                    assert Utils.hasText(body);
                    Utils.appendToPane(loggerTextPane, "request sent, body:\n" + body + "\n", Color.black);
                    CloseableHttpResponse response;
                    try {
                        response = ClientService.sendHttpPostRequest(context.getClient(),
                                Constants.DEV_LOGIN_URL, body);
                    } catch (IOException ioException) {
                        Utils.appendToPane(loggerTextPane, "err: send login info error\n", Color.red);
                        return;
                    }
                    assert response != null;
                    if (response.getStatusLine().getStatusCode() == 200) {
                        context.setDevId(devId);
                        context.setToken(token);
                        context.setLoggedIn(true);
                        Utils.appendToPane(loggerTextPane, "login success!\n", Color.blue);
                        registerButton.setEnabled(false);
                        logInButton.setText("log out");
                        devIdTextField.setEnabled(false);
                        tokenTextField.setEnabled(false);
                        queryActionsButton.setEnabled(true);
                        isRunning = true;
                        titleLabel.setText("IoT Client is Running");
                        queryActionsButton.dispatchEvent(new MouseEvent(queryActionsButton, MouseEvent.MOUSE_CLICKED,
                                System.currentTimeMillis(), 0, 0, 0, 1, false));
                        startPeriodicSearch(5000);
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
                } else {
                    stopPeriodicSearch();
                    context.setLoggedIn(false);
                    registerButton.setEnabled(true);
                    logInButton.setText("log in");
                    devIdTextField.setEnabled(true);
                    tokenTextField.setEnabled(true);
                    queryActionsButton.setEnabled(false);
                    actionsListModel.clear();
                    context.setActionsStr("");
                    context.setLoggedIn(false);
                    isRunning = false;
                    titleLabel.setText("Welcome to the Authorization Service");
                }
            }

        });

        queryActionsButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                String body;
                try {
                    body = ClientService.generateActionQueryBody(context.getDevId(), context.getToken());
                } catch (JsonProcessingException jsonProcessingException) {
                    Utils.appendToPane(loggerTextPane, "err: JsonProcessingException\n", Color.red);
                    return;
                }
                assert Utils.hasText(body);
                Utils.appendToPane(loggerTextPane, "request sent, body:\n" + body + "\n", Color.black);
                CloseableHttpResponse response;
                try {
                    response = ClientService.sendHttpPostRequest(context.getClient(),
                            Constants.DEV_ACT_QUERY_URL, body);
                } catch (IOException ioException) {
                    Utils.appendToPane(loggerTextPane, "err: send login info error\n", Color.red);
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
                    Utils.appendToPane(loggerTextPane, "returned body:\n" + responseBodyStr + "\n", Color.black);
                    try {
                        Map<String, String> respMap = new ObjectMapper().readValue(responseBodyStr, Map.class);
                        if (!respMap.containsKey("actions")) {
                            Utils.appendToPane(loggerTextPane, "bad response body, no actions\n",
                                    Color.red);
                            return;
                        }
                        String actionsStr = respMap.get("actions");
                        if (Utils.hasText(actionsStr) && (!Utils.hasText(context.getActionsStr()) || !actionsStr.equals(context.getActionsStr()))) {
                            context.setActionsStr(actionsStr);
                            actionsListModel.clear();
                            String[] actions = actionsStr.split("/");
                            for (String act : actions) {
                                actionsListModel.addElement(act);
                            }
                        }
                    } catch (JsonProcessingException jsonProcessingException) {
                        Utils.appendToPane(loggerTextPane, "error parse returned body\n",
                                Color.red);
                        return;
                    }
                } else {
                    Utils.appendToPane(loggerTextPane, "query failed, returned body\n" + responseBodyStr + "\n",
                            Color.red);
                }
            }
        });

        context.getLocalHttpServer().start();
    }

    private void startPeriodicSearch(int period) {
        context.getSearchService().restart();
        this.executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleWithFixedDelay(() -> {
            Map<String, DevInfo> previousNearbyDevices = context.getSearchService().getNearByDevices();
            context.getSearchService().search();
            if (!previousNearbyDevices.equals(context.getSearchService().getNearByDevices())) {
                try {
                    String nearbyDevJson =
                            Utils.mapper.writerWithDefaultPrettyPrinter().writeValueAsString(context.getSearchService().getNearByDevices());
                    Utils.appendToPane(loggerTextPane, "current nearby devices:\n" + nearbyDevJson + "\n", Color.black);
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            }
        }, 0, period, TimeUnit.MILLISECONDS);
    }

    private void stopPeriodicSearch() {
        if (this.executorService != null) {
            context.getSearchService().stop();
            this.executorService.shutdown();
        }
    }

    public static void main(String[] args) {
        if (args.length < 1 || !args[0].matches(Constants.PORT_REGEX)) {
            throw new RuntimeException("Invalid launch args, please provide the port number (6200-6209)");
        }
        int port = Integer.parseInt(args[0]);
        if (!(port >= 6200 && port < 6210)) {
            throw new RuntimeException("Invalid launch args, please provide the port number (6200-6209)");
        }
        ClientIotContext context = new ClientIotContext();
        context.setClient(HttpClients.createDefault());
        TestIotClient client = new TestIotClient(context);
        LocalHttpServer localHttpServer = null;
        try {
            localHttpServer = new LocalHttpServer(port, client);
        } catch (IOException ioException) {
            ioException.printStackTrace();
            throw new RuntimeException("cannot launch local httpserver");
        }
        context.setLocalHttpServer(localHttpServer);
        SearchService searchService = new SearchService(port, client.getLoggerTextPane());
        context.setSearchService(searchService);
        client.run();
    }

}
