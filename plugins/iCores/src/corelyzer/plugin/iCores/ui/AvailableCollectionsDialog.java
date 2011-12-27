package corelyzer.plugin.iCores.ui;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndLink;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;
import corelyzer.plugin.iCores.data.CollectionEntry;
import corelyzer.plugin.iCores.helper.MyAuthenticator;
import corelyzer.plugin.iCores.ui.table.CollectionTableModel;

import javax.swing.*;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.*;
import java.net.Authenticator;
import java.net.URL;
import java.util.List;
import java.util.Vector;

public class AvailableCollectionsDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JComboBox atomSource;
    private JButton refreshButton;
    private JTable collectionsTable;
    private JLabel statusLabel;

    private CollectionTableModel tableModel;
    private Vector<CollectionEntry> subscribedCollections;

    public AvailableCollectionsDialog(Frame owner,
                                      Vector<CollectionEntry> aVec) {
        super(owner);
        
        $$$setupUI$$$();
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        setTitle("Available Holes");

        refreshButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                onRefresh();
            }
        });

        this.subscribedCollections = aVec;

        // init http authenticator
        Authenticator.setDefault(new MyAuthenticator());

        String localSrc = System.getProperty(
                "corelyzer.plugin.iCores.localAtomSrc");
        if (localSrc != null) {
            Vector oldItems = new Vector();

            int oldSize = this.atomSource.getModel().getSize();
            for (int i = 0; i < oldSize; i++) {
                //noinspection unchecked
                oldItems.add(atomSource.getItemAt(i));
            }

            atomSource.removeAllItems();

            atomSource.addItem(localSrc);
            for (int i = 0; i < oldSize; i++) {
                atomSource.addItem(oldItems.elementAt(i));
            }
        }
    }

    private void onOK() {
        for (int i = 0; i < this.tableModel.getRowCount(); i++) {
            if ((Boolean) tableModel.getValueAt(i, 0)) {
                CollectionEntry he = new CollectionEntry();
                he.setName(tableModel.getValueAt(i, 1).toString());
                he.setUrl(tableModel.getValueAt(i, 2).toString());

                subscribedCollections.add(he);
            }
        }

        dispose();
    }

    private void onCancel() {
        dispose();
    }

    private void onRefresh() {
        String feed = this.atomSource.getSelectedItem().toString();
        RefreshListThread updateThread = new RefreshListThread(this, feed);
        updateThread.start();
    }

    public static void main(String[] args) {
        AvailableCollectionsDialog dialog = new AvailableCollectionsDialog(
                null, new Vector<CollectionEntry>());
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
        System.exit(0);
    }

    public void setData(TableModel data) {
        this.collectionsTable.setModel(data);
    }

    public TableModel getData() {
        return this.collectionsTable.getModel();
    }

    public boolean isModified(CollectionTableModel data) {
        return false;
    }


    public Vector<CollectionEntry> getSubscribedHoles() {
        return subscribedCollections;
    }

    private void createUIComponents() {
        createCollectionTable();
    }

    private void createCollectionTable() {
        collectionsTable = new JTable();

        tableModel = new CollectionTableModel();
        this.collectionsTable.setModel(tableModel);
        this.collectionsTable.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION);
        this.collectionsTable.setShowGrid(true);
        this.collectionsTable.setShowHorizontalLines(true);

        this.collectionsTable.getColumnModel().getColumn(0).setHeaderValue(
                "Subscribe");
        this.collectionsTable.getColumnModel().getColumn(1).setHeaderValue(
                "Title");
        this.collectionsTable.getColumnModel().getColumn(2).setHeaderValue(
                "URL");
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        createUIComponents();
        contentPane = new JPanel();
        contentPane.setLayout(new GridLayoutManager(2, 1, new Insets(10, 10, 10, 10), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel1.add(spacer1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        buttonOK = new JButton();
        buttonOK.setText("OK");
        panel2.add(buttonOK, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonCancel = new JButton();
        buttonCancel.setText("Cancel");
        panel2.add(buttonCancel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(1, 4, new Insets(0, 0, 0, 0), -1, -1));
        panel3.add(panel4, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("Project Source:");
        panel4.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        refreshButton = new JButton();
        refreshButton.setText("Refresh");
        panel4.add(refreshButton, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        statusLabel = new JLabel();
        statusLabel.setIcon(new ImageIcon(getClass().getResource("/corelyzer/plugin/iCores/ui/resources/icons/pi.png")));
        statusLabel.setText("");
        panel4.add(statusLabel, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        atomSource = new JComboBox();
        atomSource.setEditable(true);
        final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
        defaultComboBoxModel1.addElement("http://www.evl.uic.edu/cavern/corewall/iCores/feeds/subscriptions.xml");
        defaultComboBoxModel1.addElement("http://localhost/subscriptions.xml");
        atomSource.setModel(defaultComboBoxModel1);
        panel4.add(atomSource, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        panel3.add(scrollPane1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        scrollPane1.setViewportView(collectionsTable);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPane;
    }

    class RefreshListThread extends Thread {
        private String atomFeed;
        private Component caller;

        public RefreshListThread(Component c, String feed) {
            caller = c;
            atomFeed = feed;
        }

        private void updateIndicator(boolean isRunning) {
            String iconName;

            if (isRunning) {
                iconName = "indicator.gif";
            } else {
                iconName = "pi.png";
            }

            final ImageIcon icon = new ImageIcon(getClass().getResource(
                    "/corelyzer/plugin/iCores/ui/resources/icons/"
                            + iconName));

            SwingUtilities.invokeLater(
                    new Runnable() {
                        public void run() {
                            statusLabel.setIcon(icon);
                        }
                    }
            );
        }

        public void run() {
            this.updateIndicator(true);

            try {
                URL feedUrl = new URL(atomFeed);
                SyndFeedInput input = new SyndFeedInput();

                SyndFeed feed = input.build(new XmlReader(feedUrl));

                List entries = feed.getEntries();

                tableModel.clear();

                for (Object listEntry : entries) {
                    SyndEntry entry = (SyndEntry) listEntry;
                    String title = entry.getTitle();
                    List links = entry.getLinks();

                    for (Object obj : links) {
                        SyndLink link = (SyndLink) obj;

                        if (link.getRel().compareToIgnoreCase("alternate") == 0) { // or 'related'?
                            tableModel.addElement(false, title, link.getHref());
                        }
                    }
                }

                collectionsTable.updateUI();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(caller,
                        "Cannot get feed: " + atomFeed + "\nError: " + ex);
            }

            this.updateIndicator(false);
        }
    }

}
