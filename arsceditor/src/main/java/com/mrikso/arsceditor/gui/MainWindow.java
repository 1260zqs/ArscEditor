package com.mrikso.arsceditor.gui;

import com.google.common.io.Files;
import com.mrikso.arsceditor.gui.dialogs.AboutDialog;
import com.mrikso.arsceditor.gui.dialogs.ErrorDialog;
import com.mrikso.arsceditor.gui.tree.ArscTableView;
import com.mrikso.arsceditor.gui.tree.ArscTreeView;
import com.mrikso.arsceditor.intrefaces.TableChangedListener;
import com.mrikso.arsceditor.valueeditor.ArscWriter;
import com.mrikso.arsceditor.valueeditor.ValueHelper;
import com.google.devrel.gmscore.tools.apk.arsc.BinaryResourceFile;
import com.google.devrel.gmscore.tools.apk.arsc.Chunk;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainWindow extends JFrame implements TableChangedListener {

    public static String openFileArg;
    public static String patchFileArg;

    private ArscTreeView treeView;
    private JSplitPane splitPane;
    private JMenuItem saveBtn;
    private JMenuItem saveAs;
    private String openedFilePath;

    public MainWindow() {
        loadComponent();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                onWindowReady();
            }
        });
    }

    protected void loadComponent() {
        ArscTableView arscTableView = new ArscTableView(this);
        arscTableView.setTableChangedListener(this);

        treeView = new ArscTreeView(this, null, arscTableView);
        treeView.setTableChangedListener(this);

        BorderLayout layout = new BorderLayout();
        JPanel panel = new JPanel();
        panel.setLayout(layout);

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(treeView), new JScrollPane(arscTableView));

        initSplitPane();

        panel.add(splitPane, BorderLayout.CENTER);

        this.setJMenuBar(createMenuBar());
        this.getContentPane().add(panel);

        this.setPreferredSize(new Dimension(900, 600));
        this.setMinimumSize(new Dimension(600, 600));
        this.setLocationRelativeTo(null);
        this.setTitle("Arsc Editor");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.pack();
        this.setVisible(true);
    }

    private void onWindowReady() {
        if (openFileArg != null && java.nio.file.Files.exists(Paths.get(openFileArg))) {
            Runnable next = null;
            if (patchFileArg != null && java.nio.file.Files.exists(Paths.get(patchFileArg))) {
                next = () -> patchFile(patchFileArg);
            }
            openedFilePath = openFileArg;
            openFile(openFileArg, next);
        }
    }

    /**
     * Creates a menu bar.
     */
    protected JMenuBar createMenuBar() {
        JMenu fileMenu = new JMenu("File");
        JMenuItem menuItem;

        menuItem = new JMenuItem("Open");
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
        menuItem.addActionListener(ae -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setAcceptAllFileFilterUsed(false);
            FileNameExtensionFilter filter = new FileNameExtensionFilter("Android resource files", "arsc");
            fileChooser.addChoosableFileFilter(filter);

            int result = fileChooser.showOpenDialog(getRootPane());
            if (result == JFileChooser.APPROVE_OPTION) {
                openedFilePath = fileChooser.getSelectedFile().getPath();
                openFile(openedFilePath, null);
            }
        });

        saveBtn = new JMenuItem("Save");
        saveBtn.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
        saveBtn.setEnabled(false);
        saveBtn.addActionListener(l -> {
            if (openedFilePath != null) {
                saveFile(new File(openedFilePath), null);
            }
        });

        saveAs = new JMenuItem("Save as");
        saveAs.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
        saveAs.setEnabled(false);
        saveAs.addActionListener(l -> selectPathToSave());

        fileMenu.add(menuItem);
        fileMenu.addSeparator();
        fileMenu.add(saveBtn);
        fileMenu.add(saveAs);

        menuItem = new JMenuItem("Exit");
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_DOWN_MASK));
        menuItem.addActionListener(ae -> System.exit(0));
        fileMenu.addSeparator();
        fileMenu.add(menuItem);

        JMenu aboutMenu = new JMenu("About");
        menuItem = new JMenuItem("Open source code");
        menuItem.addActionListener(l -> gitHomepage());
        aboutMenu.add(menuItem);
        aboutMenu.addSeparator();
        menuItem = new JMenuItem("Info");
        menuItem.addActionListener(l -> new AboutDialog(MainWindow.this).showDialog());
        aboutMenu.add(menuItem);

        JMenu patchMenu = new JMenu("Patch");
        menuItem = new JMenuItem("Select Patch File");
        menuItem.addActionListener(l -> selectPatchFile());

        patchMenu.add(menuItem);

        // Create a menu bar
        JMenuBar menuBar = new JMenuBar();

        menuBar.add(fileMenu);
        menuBar.add(patchMenu);
        menuBar.add(aboutMenu);

        return menuBar;
    }

    private void gitHomepage() {
        try {
            Desktop.getDesktop().browse(new URI("https://github.com/MrIkso/ArscEditor"));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                    this,
                    ex.getMessage(), this.getTitle(),
                    JOptionPane.WARNING_MESSAGE);
        }
    }

    private void initSplitPane() {
        splitPane.setDividerLocation(200);
        splitPane.setResizeWeight(0);
        Dimension minimumSize = new Dimension(200, 600);
        splitPane.getLeftComponent().setMinimumSize(minimumSize);
        splitPane.getRightComponent().setMinimumSize(minimumSize);
    }

    @Override
    public void tableChanged() {
        saveAs.setEnabled(true);
        saveBtn.setEnabled(true);
    }

    private void patchFile(String path) {
        try {
            List<String> lines = java.nio.file.Files.readAllLines(Paths.get(path));
            String key = null;
            String packageName = null;
            Map<String, String> patch = null;
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line == null || line.isBlank()) {
                    continue;
                }
                if (line.startsWith("@")) {
                    if (packageName != null) {
                        treeView.patch(packageName, patch);
                    }
                    packageName = line.substring(1);
                    patch = new HashMap<>();
                    continue;
                }
                if (patch == null) continue;
                if (key == null) {
                    key = line;
                } else {
                    patch.put(key, line);
                    key = null;
                }
            }
            if (packageName != null) {
                treeView.patch(packageName, patch);
            }
            if (patch != null) {
                if (patch.isEmpty()) {
                    int result = JOptionPane.showConfirmDialog(
                            this,
                            "Patch success save file & exit?",
                            "",
                            JOptionPane.OK_CANCEL_OPTION,
                            JOptionPane.QUESTION_MESSAGE
                    );
                    if (result == JOptionPane.OK_OPTION) {
                        saveFile(new File(openedFilePath), () -> System.exit(0));
                    }
                } else {
                    JOptionPane.showMessageDialog(
                            this,
                            String.join("\n", patch.keySet()),
                            "follow key not patched",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
//            throw new RuntimeException(e);
        }
    }

    private void openFile(String path, Runnable next) {
        new SwingWorker<List<Chunk>, Chunk>() {
            @Override
            protected List<Chunk> doInBackground() throws Exception {
                byte[] resContents = java.nio.file.Files.readAllBytes(Paths.get(path));
                BinaryResourceFile binaryRes = new BinaryResourceFile(resContents);
                return binaryRes.getChunks();
            }

            @Override
            protected void process(List<Chunk> chunks) {
                super.process(chunks);
            }

            @Override
            protected void done() {
                try {
                    treeView.setRootWithFile(get(), new File(path).getName());
                    if (next != null) next.run();
                } catch (Exception e) {
                    e.printStackTrace();
                    new ErrorDialog(MainWindow.this, e);
                }
            }
        }.execute();

    }

    private void selectPatchFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setAcceptAllFileFilterUsed(false);
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Select patch file", "txt");
        fileChooser.addChoosableFileFilter(filter);

        int result = fileChooser.showOpenDialog(getRootPane());
        if (result == JFileChooser.APPROVE_OPTION) {
            patchFile(fileChooser.getSelectedFile().getPath());
        }
    }

    private void selectPathToSave() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setAcceptAllFileFilterUsed(false);
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Android resource files", "arsc");
        fileChooser.addChoosableFileFilter(filter);
        String fileName = Files.getNameWithoutExtension(openedFilePath);

        fileChooser.setSelectedFile(new File(new File(openedFilePath).getParent() + "/"
                + fileName + "_mod.arsc"));

        int result = fileChooser.showSaveDialog(getRootPane());
        if (result == JFileChooser.APPROVE_OPTION) {
            saveFile(fileChooser.getSelectedFile(), null);
        }
    }

    private void saveFile(File path, Runnable next) {
        new SwingWorker<Boolean, Integer>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                new ArscWriter(ValueHelper.getResourceTableChunk(), path).write();
                return true;
            }

            @Override
            protected void done() {
                try {
                    saveAs.setEnabled(false);
                    if (next != null) {
                        next.run();
                        return;
                    }
                    JOptionPane.showMessageDialog(MainWindow.this, "File saved!");
                } catch (Exception e) {
                    e.printStackTrace();
                    new ErrorDialog(MainWindow.this, e);
                }
            }
        }.execute();

    }
}
