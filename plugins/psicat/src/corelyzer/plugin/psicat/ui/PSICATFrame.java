package corelyzer.plugin.psicat.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.jdesktop.layout.GroupLayout;
import org.jdesktop.layout.LayoutStyle;

import corelyzer.plugin.psicat.PSICATPlugin;
import corelyzer.ui.CorelyzerApp;

/**
 * The PSICAT plugin frame.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class PSICATFrame extends JFrame {
    private static final long serialVersionUID = 1L;

    // to remember current directory
    private static File currentDirectory = null;

    /**
     * @param args
     *            the command line arguments
     */
    public static void main(final String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new PSICATFrame().setVisible(true);
            }
        });
    }

    // our fields
    private File file;
    private LithologyRenderer lithologyRenderer = null;

    // our components
    private JCheckBox annotationsCheckbox;
    private JButton browseButton;
    private JPanel dataPanel;
    private JTextField fileText;
    private JLabel instructionsLabel;
    private JCheckBox lithologiesCheckbox;
    private JCheckBox symbolsCheckbox;

    // End of variables declaration
    /** Creates new form PSICATFrame */
    public PSICATFrame() {
        initComponents();
    }

    private void initComponents() {
        instructionsLabel = new JLabel();
        fileText = new JTextField();
        browseButton = new JButton();
        dataPanel = new JPanel();
        annotationsCheckbox = new JCheckBox();
        annotationsCheckbox.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                onAnnotationsChange();
            }
        });
        lithologiesCheckbox = new JCheckBox();
        lithologiesCheckbox.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                onLithologiesChange();
            }
        });
        symbolsCheckbox = new JCheckBox();
        symbolsCheckbox.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                onSymbolsChange();
            }
        });

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        instructionsLabel.setText("Select the exported PSICAT data file:");

        // listen for changes
        fileText.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(final DocumentEvent e) {
                onFileTextChange();
            }

            public void insertUpdate(final DocumentEvent e) {
                onFileTextChange();
            }

            public void removeUpdate(final DocumentEvent e) {
                onFileTextChange();
            }
        });

        browseButton.setText("Browse...");
        browseButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                onBrowse(e);
            }
        });

        dataPanel.setBorder(BorderFactory.createTitledBorder(" Data "));
        annotationsCheckbox.setText("Annotations");
        annotationsCheckbox.setBorder(BorderFactory.createEmptyBorder(0, 0, 0,
                0));
        annotationsCheckbox.setEnabled(false);
        annotationsCheckbox.setMargin(new java.awt.Insets(0, 0, 0, 0));

        lithologiesCheckbox.setText("Lithologies");
        lithologiesCheckbox.setBorder(BorderFactory.createEmptyBorder(0, 0, 0,
                0));
        lithologiesCheckbox.setEnabled(false);
        lithologiesCheckbox.setMargin(new java.awt.Insets(0, 0, 0, 0));

        symbolsCheckbox.setText("Symbols");
        symbolsCheckbox.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        symbolsCheckbox.setEnabled(false);
        symbolsCheckbox.setMargin(new java.awt.Insets(0, 0, 0, 0));

        GroupLayout dataPanelLayout = new GroupLayout(dataPanel);
        dataPanel.setLayout(dataPanelLayout);
        dataPanelLayout.setHorizontalGroup(dataPanelLayout.createParallelGroup(
                GroupLayout.LEADING).add(
                dataPanelLayout.createSequentialGroup().addContainerGap().add(
                        dataPanelLayout
                                .createParallelGroup(GroupLayout.LEADING).add(
                                        annotationsCheckbox).add(
                                        lithologiesCheckbox).add(
                                        symbolsCheckbox)).addContainerGap(232,
                        Short.MAX_VALUE)));
        dataPanelLayout.setVerticalGroup(dataPanelLayout.createParallelGroup(
                GroupLayout.LEADING).add(
                dataPanelLayout.createSequentialGroup().addContainerGap().add(
                        annotationsCheckbox).addPreferredGap(
                        LayoutStyle.RELATED).add(lithologiesCheckbox)
                        .addPreferredGap(LayoutStyle.RELATED).add(
                                symbolsCheckbox).addContainerGap(113,
                                Short.MAX_VALUE)));

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(
                GroupLayout.LEADING).add(
                layout.createSequentialGroup().addContainerGap().add(
                        layout.createParallelGroup(GroupLayout.LEADING).add(
                                dataPanel, GroupLayout.DEFAULT_SIZE,
                                GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).add(
                                GroupLayout.TRAILING,
                                layout.createSequentialGroup().add(fileText,
                                        GroupLayout.DEFAULT_SIZE, 267,
                                        Short.MAX_VALUE).addPreferredGap(
                                        LayoutStyle.RELATED).add(browseButton))
                                .add(instructionsLabel)).addContainerGap()));
        layout.setVerticalGroup(layout.createParallelGroup(GroupLayout.LEADING)
                .add(
                        layout.createSequentialGroup().addContainerGap().add(
                                instructionsLabel).addPreferredGap(
                                LayoutStyle.RELATED).add(
                                layout
                                        .createParallelGroup(
                                                GroupLayout.BASELINE).add(
                                                browseButton).add(fileText,
                                                GroupLayout.PREFERRED_SIZE,
                                                GroupLayout.DEFAULT_SIZE,
                                                GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(LayoutStyle.RELATED).add(
                                        dataPanel, GroupLayout.DEFAULT_SIZE,
                                        GroupLayout.DEFAULT_SIZE,
                                        Short.MAX_VALUE).addContainerGap()));
        pack();
    }

    /**
     * Called when our annotations checkbox changes.
     */
    protected void onAnnotationsChange() {
        // do nothing
    }

    // show our file chooser
    private void onBrowse(final ActionEvent evt) {
        // create our file chooser
        JFileChooser fileChooser;
        if (currentDirectory == null) {
            fileChooser = new JFileChooser();
        } else {
            fileChooser = new JFileChooser(currentDirectory);
        }
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            fileText.setText(fileChooser.getSelectedFile().getAbsolutePath());
            currentDirectory = file.getParentFile();
        }
    }

    private void onFileTextChange() {
        // disable our checkboxes
        annotationsCheckbox.setEnabled(false);
        lithologiesCheckbox.setEnabled(false);
        symbolsCheckbox.setEnabled(false);

        // figure out if we got a directory
        File d = new File(fileText.getText());
        if (d.exists() && d.isFile()) {
            file = d;
            parseData();
        } else {
            file = null;
        }
    }

    /**
     * Called when the lithologies checkbox changes
     */
    protected void onLithologiesChange() {
        // create our track
        int trackId = CorelyzerApp.getApp().createTrack("lithology");

        // create our renderer
        lithologyRenderer = new LithologyRenderer(file);
        if (lithologyRenderer.parse()) {
            float top = lithologyRenderer.getTop();
            float bot = lithologyRenderer.getBottom();

            // create a freedraw
            PSICATPlugin.getDefault().createFreedrawForTrack(lithologyRenderer,
                    trackId, top, 0.0f, (bot - top), 0.1f);
        }
    }

    /**
     * Called with the symbols checkbox changes.
     */
    protected void onSymbolsChange() {
        // do nothing
    }

    private void parseData() {
        try {
            ZipFile zip = new ZipFile(file);
            for (ZipEntry entry : Collections.list(zip.entries())) {
                if (entry.getName().startsWith("annotations")) {
                    annotationsCheckbox.setEnabled(true);
                } else if (entry.getName().equals(
                        LithologyRenderer.LITHOLOGY_DATA)) {
                    lithologiesCheckbox.setEnabled(true);
                } else if (entry.getName().startsWith("symbols")) {
                    symbolsCheckbox.setEnabled(true);
                }
            }
            zip.close();
        } catch (ZipException e) {
            // wasn't a zip file
        } catch (IOException e) {
            // wasn't a zip file
        }
    }
}
