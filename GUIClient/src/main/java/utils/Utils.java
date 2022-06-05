package utils;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.swing.*;
import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import java.awt.*;

public class Utils {
    public static ObjectMapper mapper = new ObjectMapper();

    public static void appendToPane(JTextPane pane, String text, Color c) {
        pane.setEditable(true);
        StyleContext sc = StyleContext.getDefaultStyleContext();
        AttributeSet aset = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, c);

        aset = sc.addAttribute(aset, StyleConstants.FontFamily, "Lucida Console");
        aset = sc.addAttribute(aset, StyleConstants.Alignment, StyleConstants.ALIGN_JUSTIFIED);

        int len = pane.getDocument().getLength();
        pane.setCaretPosition(len);
        pane.setCharacterAttributes(aset, false);
        pane.replaceSelection(text);
        pane.setEditable(false);
    }

    public static boolean hasText(String s) {
        return s != null && s.length() != 0;
    }
}
