package corelyzer.plugin.iCores.ui;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.sun.syndication.feed.synd.SyndCategory;
import com.sun.syndication.feed.synd.SyndFeed;
import corelyzer.plugin.iCores.cache.CacheFeedUtil;
import corelyzer.plugin.iCores.data.CollectionEntry;
import corelyzer.plugin.iCores.ui.tree.FeedTreeNode;
import corelyzer.util.StringUtility;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Vector;

public class SubscribeDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JButton browseButton;
    private JTextArea feedTextArea;

    Frame owner;

    // model
    private Vector<CollectionEntry> subscribedCollections;

    public SubscribeDialog(Frame owner) {
        super(owner);
        this.owner = owner;

        setTitle("Subscribe to Corecast");
        subscribedCollections = new Vector<CollectionEntry>();

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
        browseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                onBrowse();
            }
        });
    }

    private void onBrowse() {
        this.dispose();

        AvailableCollectionsDialog dialog = new AvailableCollectionsDialog(
                owner, this.subscribedCollections);
        dialog.pack();
        dialog.setLocationRelativeTo(owner);
        dialog.setAlwaysOnTop(true);
        dialog.setVisible(true);

        if ((subscribedCollections != null) &&
                (subscribedCollections.size() > 0)) {

            ICoreFrame f = ICoreFrame.getIcoreFrame();
            JProgressBar mainProgressBar = f.getMainProgressBar();
            FeedTreeNode subscriptionCategory = f.getSubscriptionCategory();

            for (final CollectionEntry he : this.subscribedCollections) {
                mainProgressBar.setIndeterminate(true);

                // Compare new with those which are already subscribed
                String url = he.getUrl();
                String title = he.getName();

                // If already subscribed, just issue refresh
                FeedTreeNode oldNode = f.getSubscriptionNode(url);
                if (oldNode != null) {
                    System.out.println("---> [INFO] Already subscribed to '"
                            + url + "', just issue refresh at the old node");

                    (new Thread(oldNode)).start();
                    continue;
                }

                System.out.println(
                        "---> Adding new '" + url + "' to subscription");

                FeedTreeNode node = new FeedTreeNode(title, url,
                        subscriptionCategory);
                subscriptionCategory.getChildren().add(node);
                (new Thread(node)).start();

                f.getAllSubscriptions().put(url, node); // fixme
            }

            mainProgressBar.setIndeterminate(false);
            // f.expandAllNodes();
            // f.coreRepoTree.updateUI();

            dialog.dispose();
            this.dispose();
        }

        dialog.dispose();
    }

    private void onOK() {
        String feedURLStr = this.feedTextArea.getText();
        try {
            URL feedURL = new URL(feedURLStr);

            boolean isValidFeed = isFeed(feedURL.toString());
            if (!isValidFeed) {
                String mesg = "The url '" + feedURL +
                        "' \nis not a valid Corecast feed.";
                JOptionPane.showMessageDialog(this, mesg);

                return;
            }

            CollectionEntry he = new CollectionEntry("A new feed", feedURLStr);
            ICoreFrame f = ICoreFrame.getIcoreFrame();
            FeedTreeNode subscriptionCategory = f.getSubscriptionCategory();

            String url = he.getUrl();
            String title = he.getName();

            // If already subscribed, just issue refresh
            FeedTreeNode oldNode = f.getSubscriptionNode(url);
            if (oldNode != null) {
                System.out.println("---> [INFO] Already subscribed to '"
                        + url + "', just issue refresh at the old node");

                (new Thread(oldNode)).start();
            }

            FeedTreeNode node = new FeedTreeNode(title, url,
                    subscriptionCategory);
            subscriptionCategory.getChildren().add(node);
            (new Thread(node)).start();

            f.getAllSubscriptions().put(url, node); // fixme

            dispose();
        } catch (MalformedURLException e) {
            String message = "Invalid feed url";
            JOptionPane.showMessageDialog(this, message);

            // e.printStackTrace();
        }
    }

    // check the feed
    private boolean isFeed(String aFeedUrl) {
        boolean isFeed = false;
        SyndFeed feed = CacheFeedUtil.readFeed(
                ICoreFrame.getCacheMgr(), aFeedUrl);

        if (feed == null) {
            return false;
        }

        List categories = feed.getCategories();

        for (Object obj : categories) {
            if (obj instanceof SyndCategory) {
                SyndCategory category = (SyndCategory) obj;
                if (category.getName().equalsIgnoreCase("feed")) {
                    isFeed = true;
                }
            }
        }

        return isFeed;
    }

    private void onCancel() {
        dispose();
    }

    public void setVisible(boolean b) {
        String clipText = StringUtility.getClipboard();
        if (clipText.toLowerCase().startsWith("http")) {
            this.feedTextArea.setText(clipText);
        }

        feedTextArea.selectAll();

        super.setVisible(b);
    }

    public static void main(String[] args) {
        SubscribeDialog dialog = new SubscribeDialog(null);
        dialog.pack();
        dialog.setSize(320, 150);
        dialog.setVisible(true);
        System.exit(0);
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        contentPane = new JPanel();
        contentPane.setLayout(new GridLayoutManager(2, 1, new Insets(10, 10, 10, 10), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel1.add(spacer1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        buttonCancel = new JButton();
        buttonCancel.setText("Cancel");
        panel2.add(buttonCancel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonOK = new JButton();
        buttonOK.setText("OK");
        panel2.add(buttonOK, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        browseButton = new JButton();
        browseButton.setText("Browse...");
        panel2.add(browseButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("URL: ");
        panel3.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel3.add(panel4, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel4.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), null));
        final JScrollPane scrollPane1 = new JScrollPane();
        scrollPane1.setHorizontalScrollBarPolicy(31);
        panel4.add(scrollPane1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        feedTextArea = new JTextArea();
        feedTextArea.setLineWrap(true);
        scrollPane1.setViewportView(feedTextArea);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPane;
    }
}
