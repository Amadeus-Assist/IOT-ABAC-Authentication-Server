package gui;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import javax.swing.text.StyleContext;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Locale;

public class ExceptionWindow {
    private final JFrame exceptionFrame;
    private JPanel mainPanel;
    private JLabel InfoLabel;
    private JButton OKButton;
    private final JFrame clientFrame;

    public ExceptionWindow(String msg, JFrame clientFrame) {
        super();
        this.exceptionFrame = new JFrame("Info");
        this.clientFrame = clientFrame;
        this.exceptionFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.exceptionFrame.setContentPane(mainPanel);
        InfoLabel.setText(msg);
        this.exceptionFrame.pack();
        OKButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                exceptionFrame.dispatchEvent(new WindowEvent(exceptionFrame, WindowEvent.WINDOW_CLOSING));
            }
        });
        this.exceptionFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                exceptionFrame.dispose();
                clientFrame.setEnabled(true);
                clientFrame.setFocusable(true);
                clientFrame.requestFocus();
            }
        });
        this.exceptionFrame.setLocationRelativeTo(clientFrame);
        this.exceptionFrame.setVisible(true);
    }

}
