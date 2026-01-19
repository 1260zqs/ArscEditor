package com.mrikso.arsceditor;

import com.mrikso.arsceditor.gui.MainWindow;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

public class App {

    public static void main(String[] args) {
        try {
            Map<String, String> parsed = parseArgs(args);
            MainWindow.openFileArg = parsed.getOrDefault("-file", null);
            MainWindow.patchFileArg = parsed.getOrDefault("-patch", null);

            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            SwingUtilities.invokeLater(MainWindow::new);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].startsWith("-")) {
                map.put(args[i], args[i + 1]);
                i++;
            }
        }
        return map;
    }
}
