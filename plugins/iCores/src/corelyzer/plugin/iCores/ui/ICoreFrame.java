/******************************************************************************
 *
 * CoreWall / Corelyzer - An Initial Core Description Tool
 * Copyright (C) 2004 - 2007 Arun Gangadhar Gudur Rao, Julian Yu-Chung Chen
 * Electronic Visualization Laboratory, University of Illinois at Chicago
 *
 * This software is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either Version 2.1 of the License, or
 * (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser Public License along
 * with this software; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 * Questions or comments about CoreWall should be directed to
 * cavern@evl.uic.edu
 *
 *****************************************************************************/

package corelyzer.plugin.iCores.ui;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.sun.syndication.feed.synd.*;


import corelyzer.data.CRPreferences;
import corelyzer.handlers.ProgressHandler;
import corelyzer.handlers.SubscribeHandler;
import corelyzer.plugin.iCores.cache.CacheManager;
import corelyzer.plugin.iCores.cache.ICacheManager;
//import corelyzer.data.Preferences; // brg 12/08/2011
//import corelyzer.plugin.iCores.cache.CacheManager;
//import corelyzer.plugin.iCores.cache.ICacheManager;
import corelyzer.plugin.iCores.data.CollectionEntry;
import corelyzer.plugin.iCores.helper.MyAuthenticator;
//import corelyzer.plugin.iCores.ui.AvailableCollectionsDialog;
import corelyzer.plugin.iCores.ui.tree.FeedTreeCellRenderer;
import corelyzer.plugin.iCores.ui.tree.FeedTreeNode;
import corelyzer.plugin.iCores.ui.tree.FeedTreeSelectionListener;
import corelyzer.plugin.iCores.rome.ROMEUtils;
import corelyzer.ui.CorelyzerApp;
import corelyzer.ui.CorelyzerApp;
import corelyzer.util.StringUtility;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.net.Authenticator;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

public class ICoreFrame extends JFrame implements SubscribeHandler, ProgressHandler {
    private JPanel contentPane;
    private JProgressBar mainProgressBar;
    private JTree coreRepoTree;
    private JButton refershButton;
    private JButton subscribeButton;
    private JButton removeButton;
    @SuppressWarnings({"FieldCanBeLocal"})
    private JSplitPane vSplitPane;
    private JScrollPane repoScrollPanel;
    private JPanel infoPane;

    // JTree base categories
    private FeedTreeNode subscriptionCategory;
    @SuppressWarnings({"FieldCanBeLocal", "UnusedDeclaration"})
    private FeedTreeNode localCategory;
    @SuppressWarnings({"FieldCanBeLocal", "UnusedDeclaration"})
    private FeedTreeNode cmlsCategory;

    // cache opened subscription list tables for persistent UI
    // private HashMap<String, JPanel> collectionPanels;
    private CollectionInfoPanel holeInfoPane;
    Hashtable<String, FeedTreeNode> allSubscriptions;

    CRPreferences prefs;
    private String iCoresFilename;
    private String localFilename;
    private String subscriptionsFilename;
    private String cmlsFilename;

    // Helpers
    static ICoreFrame iCoreFrame;
    static ICacheManager cacheMgr;
    static ResourceManager resourceManager;
    //static ICoreFrame iCoreFrame;

    public ICoreFrame() {
        this(null);
    }

    public ICoreFrame(CRPreferences p) {
        super();

		String versionNumber = CorelyzerApp.getApp().getCorelyzerVersion();
        if (versionNumber == null || versionNumber.equals("")) {
            versionNumber = "undetermined";
        }
        setTitle("Corelyzer " + versionNumber);
        iCoreFrame = this;

        // Model init
        // collectionPanels = new HashMap<String, JPanel>();
        this.allSubscriptions = new Hashtable<String, FeedTreeNode>();

        // Getting configuration filenames
        prefs = (p == null) ? (new CRPreferences()) : p;
        String sp = System.getProperty("file.separator");
        this.iCoresFilename = prefs.config_Directory + sp + "icores.xml";
        this.localFilename = prefs.config_Directory + sp + "icores_local.xml";
        this.subscriptionsFilename = prefs.config_Directory + sp +
                "icores_subscriptions.xml";
        this.cmlsFilename = prefs.config_Directory + sp + "icores_CMLs.xml";

        // Helpers
        // init http authenticator
        Authenticator.setDefault(new MyAuthenticator());
        resourceManager = new ResourceManager();
        cacheMgr = new CacheManager(prefs);
        cacheMgr.init();

        // View
        $$$setupUI$$$();
        setContentPane(contentPane);
        setupUI();

        // Controllers
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onClose();

                CorelyzerApp app = CorelyzerApp.getApp();

                if (app != null) {
                    app.windowClosing(e);
                }
            }

            public void windowIconified(WindowEvent e) {
                CorelyzerApp app = CorelyzerApp.getApp();

                if (app != null) {
                    app.windowIconified(e);
                }
            }

            public void windowDeiconified(WindowEvent e) {
                boolean MAC_OS_X = (System.getProperty("os.name").
                        toLowerCase().startsWith("mac os x"));

                if (MAC_OS_X) {
                    CorelyzerApp app = CorelyzerApp.getApp();

                    if (app != null) {
                        app.windowDeiconified(e);
                        app.getDefaultMainFrame().setVisible(false);
                    }
                }
            }
        });

        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onClose();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        refershButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                onRefresh();
            }
        });

        subscribeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                onSubscribe();
            }
        });

        removeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                onRemove();
            }
        });

        // Update model & view based on configs init-ed
        refreshTree();
    }

    public static ICoreFrame getIcoreFrame() {
        return iCoreFrame;
    }

    public static ICacheManager getCacheMgr() {
        return cacheMgr;
    }

    private void onRefresh() {
        // Refresh a feed
        Object obj = coreRepoTree.getSelectionPath().getLastPathComponent();
        FeedTreeNode aNode = (FeedTreeNode) obj;
        (new Thread(aNode)).start();
        coreRepoTree.updateUI();
    }

    private SyndFeed writeICoreConf(String filepath) {
        SyndFeed feed = new SyndFeedImpl();
        feed.setFeedType("atom_1.0");

        // add our feed metadata
        feed.setUri("tag:localhost," + ROMEUtils.DATE.format(new Date()) +
                ":/" + filepath);
        feed.setTitle("iCores Root");
        feed.setAuthor("iCores v 0.0.1");
        feed.setLinks(new ArrayList());
        String fileURL = null;
        try {
            fileURL = (new File(filepath)).toURL().toString();
        } catch (MalformedURLException e) {
            System.out.println("---> Malformed URL in writeICoreConf");
        }
        //noinspection unchecked
        feed.getLinks().add(
                ROMEUtils.createLink(fileURL, "self", null, null));
        feed.setDescription("Main iCores collection root");
        feed.setPublishedDate(new Date());

        return feed;
    }

    private void writeLocalConf(String filename) {
        SyndFeed feed = new SyndFeedImpl();
        feed.setFeedType("atom_1.0");

        // add our feed metadata
        feed.setUri("tag:localhost," + ROMEUtils.DATE.format(new Date()) + ":/" + filename);
        feed.setTitle("Local");
        feed.setAuthor("iCores v 0.0.1");
        feed.setLinks(new ArrayList());
        String fileURL = null;
        try {
            fileURL = (new File(filename)).toURL().toString();
        } catch (MalformedURLException e) {
            System.out.println("---> MalformedException in writeLocalConf: "
                    + filename);
        }
        //noinspection unchecked
        feed.getLinks().add(ROMEUtils.createLink(fileURL, "self", null, null));
        feed.setDescription("Local resources such as cached items and CML files");
        feed.setPublishedDate(new Date());

        // set our entries
        feed.setEntries(new ArrayList());

        // write out our feed
        ROMEUtils.writeFeed(feed, filename);
    }

    private void writeSubscriptionConf(String filename) {
        SyndFeed feed = new SyndFeedImpl();
        feed.setFeedType("atom_1.0");

        // add our feed metadata
        feed.setUri("tag:localhost," + ROMEUtils.DATE.format(new Date()) + ":/" + filename);
        feed.setTitle("Subscriptions");
        feed.setAuthor("iCores v 0.0.1");
        feed.setLinks(new ArrayList());
        String fileURL = null;
        try {
            fileURL = (new File(filename)).toURL().toString();
        } catch (MalformedURLException e) {
            System.out.println(
                    "---> MalformedException in writeSubscriptionConf "
                            + filename);
        }
        //noinspection unchecked
        feed.getLinks().add(ROMEUtils.createLink(fileURL, "self", null, null));
        feed.setDescription("Manages the user's hole subscriptions in the Corelyzer client");
        feed.setPublishedDate(new Date());

        // write out our feed
        ROMEUtils.writeFeed(feed, filename);
    }

    private void writeCMLsConf(String filename) {
        // TODO
    }

    private void generateDefaultiCoresConfigs() {
        // Write the icores.xml file
        SyndFeed rootFeed = this.writeICoreConf(this.iCoresFilename);
        this.writeLocalConf(this.localFilename);
        this.writeSubscriptionConf(this.subscriptionsFilename);
        this.writeCMLsConf(this.cmlsFilename);

        // add some default root entries
        List<SyndEntry> entries = new ArrayList<SyndEntry>();
        SyndEntry entry;
        SyndContent content;

        // create our categories
        List<SyndCategory> categories =
                new ArrayList<SyndCategory>();
        categories.add(ROMEUtils.createCategory("feed", null));

        // add the local entry
        entry = new SyndEntryImpl();
        entry.setUri("tag:localhost," + ROMEUtils.DATE.format(new Date()) +
                this.localFilename);
        entry.setTitle("Local");
        entry.setAuthor("iCores v 0.0.1");
        String fileURL = null;
        try {
            fileURL = (new File(localFilename)).toURL().toString();
        } catch (MalformedURLException e) {
            System.out.println("---> MalformedException in " +
                    "generateDefaultiCoresConfigs " + localFilename);
        }
        entry.setLink(fileURL);
        entry.setCategories(categories);
        entry.setPublishedDate(new Date());
        content = new SyndContentImpl();
        content.setValue("Resources on the local computer such as in " +
                "the cache and CML files");
        entry.setDescription(content);
        entries.add(entry);

        // add the subscriptions entry
        entry = new SyndEntryImpl();
        entry.setUri("tag:localhost," + ROMEUtils.DATE.format(new Date()) +
                this.subscriptionsFilename);
        entry.setTitle("Subscriptions");
        entry.setAuthor("iCores v 0.0.1");
        try {
            fileURL = (new File(subscriptionsFilename)).toURL().toString();
        } catch (MalformedURLException e) {
            System.out.println("---> MalformedException in " +
                    "generateDefaultiCoresConfigs " + subscriptionsFilename);
        }
        entry.setLink(fileURL);
        entry.setCategories(categories);
        entry.setPublishedDate(new Date());
        content = new SyndContentImpl();
        content.setValue("iCores subscriptions");
        entry.setDescription(content);
        entries.add(entry);

        // set our entries
        rootFeed.setEntries(entries);

        // write out our feed
        ROMEUtils.writeFeed(rootFeed, this.iCoresFilename);
    }

    private void refreshTree() {
        FeedTreeNode top;

        if (this.iCoresFilename == null) return;

        File f = new File(this.iCoresFilename);
        if (!f.exists()) { // create a new icores root feed
            System.out.println("---> [INFO] Creat new default iCores configs");
            generateDefaultiCoresConfigs();
        }

        System.out.println("---> [INFO] Load iCores configs");
        String mainConfigURL = null;
        try {
            mainConfigURL = (new File(iCoresFilename)).toURL().toString();
        } catch (MalformedURLException e) {
            System.out.println("---> MalformedURLException in refreshTree() "
                    + iCoresFilename);
        }

		System.out.print("creating FeedTreeNode...");
        
		top = new FeedTreeNode(ROMEUtils.readFeed(mainConfigURL), null);

		System.out.println("success");
		
        // Renew view
        //noinspection BoundFieldAssignment
        coreRepoTree = new JTree(top);
        coreRepoTree.setEditable(false);
        coreRepoTree.setRootVisible(false);
        coreRepoTree.setShowsRootHandles(true);
        repoScrollPanel.setViewportView(coreRepoTree);

        coreRepoTree.setCellRenderer(new FeedTreeCellRenderer());
        coreRepoTree.getSelectionModel().setSelectionMode(
                TreeSelectionModel.SINGLE_TREE_SELECTION);

        // tree action listener
        FeedTreeSelectionListener treeSelectListener =
                new FeedTreeSelectionListener(
                        this.holeInfoPane.getEntriesTablePane(),
                        this.removeButton);

        coreRepoTree.addTreeSelectionListener(treeSelectListener);
        ToolTipManager.sharedInstance().registerComponent(coreRepoTree);

        for (final FeedTreeNode node : top.getChildren()) {
            String title = node.toString();

            if (title.equalsIgnoreCase("local")) {
                localCategory = node;
            } else if (title.equalsIgnoreCase("subscriptions")) {
                subscriptionCategory = node;
            } else if (title.equalsIgnoreCase("cmls")) {
                cmlsCategory = node;
            } else {
                System.out.println("---> Don't know title: " + title);
            }
        }

        expandAllNodes();
        coreRepoTree.updateUI();
    }

    public void terminate() {
        onClose();

        // Stop CacheManager
        cacheMgr.finish();

        dispose();
    }

    private void onClose() {
    	// Save feed configs to files
        List<SyndEntry> entries = new ArrayList<SyndEntry>();

        List<SyndCategory> categories = new ArrayList<SyndCategory>();
        categories.add(ROMEUtils.createCategory("hole", null));
        categories.add(ROMEUtils.createCategory("feed", null));

        SyndContentImpl content = new SyndContentImpl();
        content.setValue("Local subscription saved in iCores");

        // 5/15/2012 brg: Can't assume subscriptionCategory is non-null - I'm not sure how, but test
        // machines can lose the Subscriptions tree entry, in which case a NullPointerException is thrown
        // without this check - that prevents the quit/exit confirm dialog in CorelyzerAppController.quit()
        // from being popped, leading to user confusion.
        if ( subscriptionCategory != null )
        {
        	if (subscriptionCategory.getChildren() == null) return;
	        for (FeedTreeNode node : subscriptionCategory.getChildren()) {
	            SyndEntry entry = new SyndEntryImpl();
	            SyndFeed feed = node.getFeed();
	            if (feed == null) continue;
	
	            entry.setTitle(feed.getTitle());
	            entry.setAuthor(feed.getAuthor());
	            entry.setLink(node.getUrl()); // feed.getLink());
	            entry.setCategories(categories);
	            entry.setPublishedDate(new Date());
	            entry.setDescription(content);
	
	            System.out.println("---> Saving Feed: '" + entry.getTitle() + "'");
	
	            entries.add(entry);
	        }
	
	        SyndFeed subsFeed = subscriptionCategory.getFeed();
	        subsFeed.setPublishedDate(new Date());
	        subsFeed.setEntries(entries);
	
	        System.out.println("---> Saved " + entries.size() +
	                " subscriptions on close");
	        ROMEUtils.writeFeed(subsFeed, this.subscriptionsFilename);
        }
    }

    public FeedTreeNode getSubscriptionNode(String url) {
        if (allSubscriptions.containsKey(url)) {
            return allSubscriptions.get(url);
        } else {
            return null;
        }
    }

    public void addSubscriptionNode(FeedTreeNode aNode) {
        this.allSubscriptions.put(aNode.getUrl(), aNode);
    }

    public void onSubscribe() {
        //AvailableCollectionsDialog dialog = new AvailableCollectionsDialog( null, new Vector<CollectionEntry>() );
		SubscribeDialog dialog = new SubscribeDialog( this );
        dialog.pack();
        dialog.setSize(320, 150);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);

        dialog.dispose();

        //this.coreRepoTree.updateUI();
    }

    public void onSubscribe(String url) {
        StringUtility.setClipboard(url);

        SubscribeDialog dialog = new SubscribeDialog(this);
        dialog.pack();
        dialog.setSize(320, 150);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);

        dialog.dispose();
    }

    public void onAddLocalFile() {
        // TODO add local collection to the tree
        // CollectionEntry entry = new CollectionEntry("localFile1",
        //        "file:///tmp/123.xml");
        // addHoleEntryToRepoTree(entry, this.localCategory);
    }

    public void onAddCMLFile() {
        // TODO add local CML to the tree
        // CollectionEntry entry = new CollectionEntry("localFile1",
        //        "file:///tmp/123.cml");
        // addHoleEntryToRepoTree(entry, this.cmlsCategory);
    }

    private void onRemove() {
        // Confirm and remove current selected feed
        Object obj = coreRepoTree.getSelectionPath().getLastPathComponent();

        int ans = JOptionPane.showConfirmDialog(this,
                "Remove '" + obj + "' ?", "Confirmation",
                JOptionPane.YES_NO_OPTION);

        if (ans == 0) {
            System.out.println("---> Removing " + obj);

            // TODO cleanup right-hand-side panel
            // Remove associated CacheEntry & feed node
            FeedTreeNode aNode = (FeedTreeNode) obj;
            String url = aNode.getUrl();

            if (cacheMgr.hasItem(url)) {
                System.out.println("---> [INFO] Remove subscription: '" +
                        url + "'");
                cacheMgr.remove(url);
            }

            allSubscriptions.remove(url);
            System.out.println("---> [INFO] Removed SubsNode: '" + url +
                    "' Node: '" + aNode.getTitle() + "', " +
                    allSubscriptions.size() + " left");

            //noinspection SuspiciousMethodCalls
            this.subscriptionCategory.getChildren().remove(obj);
            
            this.coreRepoTree.updateUI();
        }
    }

    public static void main(String[] args) {
        ICoreFrame dialog = new ICoreFrame();
        dialog.pack();
        dialog.setTitle("iCores Plugin");
        dialog.setSize(800, 600);
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }

    final static String HOLE_INFO_PANE = "holeInfoPane";
    final static String LOCAL_INFO_PANE = "localInfoPane";
    boolean hasLocal = false;
    boolean hasiCores = false;

    private void setupUI() {
        holeInfoPane = new CollectionInfoPanel();

        infoPane.add(HOLE_INFO_PANE, holeInfoPane.getContentPane());
        hasiCores = true;
    }

    private boolean checkLeftPanelsAvailability() {
        CorelyzerApp app = CorelyzerApp.getApp();
        if (app == null) return false;

        if (!hasLocal) {
            JPanel p = (JPanel) app.getDefaultMainFrame().getContentPane();
            infoPane.add(LOCAL_INFO_PANE, p);
            hasLocal = true;
        }

        if (!hasiCores) {
            infoPane.add(HOLE_INFO_PANE, holeInfoPane.getContentPane());
            hasiCores = true;
        }

        return true;
    }

    public void switchToLocalPane() {
        if (!this.checkLeftPanelsAvailability()) return;

        CardLayout lmanager = (CardLayout) infoPane.getLayout();
        lmanager.show(infoPane, LOCAL_INFO_PANE);
    }

    public void switchToInfoPane() {
        if (!this.checkLeftPanelsAvailability()) return;

        CardLayout lmanager = (CardLayout) infoPane.getLayout();
        lmanager.show(infoPane, HOLE_INFO_PANE);
    }

    public JProgressBar getMainProgressBar() {
        return mainProgressBar;
    }

    private void createUIComponents() { // called in static

    }

    private void expandAllNodes() {
        // Some hiccup when init the tree
        if (coreRepoTree.getRowCount() == 0) return;

        try {
	        for (int row = 0; row < this.coreRepoTree.getRowCount(); row++) {
	            coreRepoTree.expandRow(row);
	        }
            coreRepoTree.setSelectionRow(0);
        } catch (NullPointerException e) {
        	System.out.println(e.getMessage());
        }
    }

    public JTree getRepoTree() {
        return this.coreRepoTree;
    }

    public void updateTreeUI() {
        this.coreRepoTree.updateUI();
    }

    public FeedTreeNode getSubscriptionCategory() {
        return subscriptionCategory;
    }

    public Hashtable<String, FeedTreeNode> getAllSubscriptions() {
        return allSubscriptions;
    }

    public JComponent getProgressUI() {
        return getMainProgressBar();
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
        contentPane.setLayout(new GridLayoutManager(3, 1, new Insets(10, 10, 10, 10), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel1, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 4, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        subscribeButton = new JButton();
        subscribeButton.setIcon(new ImageIcon(getClass().getResource("/corelyzer/plugin/iCores/ui/resources/icons/add.png")));
        subscribeButton.setText("");
        subscribeButton.setToolTipText("Add");
        panel2.add(subscribeButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(32, 32), null, 0, false));
        removeButton = new JButton();
        removeButton.setEnabled(false);
        removeButton.setIcon(new ImageIcon(getClass().getResource("/corelyzer/plugin/iCores/ui/resources/icons/remove.png")));
        removeButton.setText("");
        removeButton.setToolTipText("Remove");
        panel2.add(removeButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(32, 32), null, 0, false));
        refershButton = new JButton();
        refershButton.setEnabled(true);
        refershButton.setIcon(new ImageIcon(getClass().getResource("/corelyzer/plugin/iCores/ui/resources/icons/pi.png")));
        refershButton.setText("");
        refershButton.setToolTipText("Refresh");
        panel2.add(refershButton, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(32, 32), null, 0, false));
        mainProgressBar = new JProgressBar();
        mainProgressBar.setIndeterminate(false);
        mainProgressBar.setString("Status");
        mainProgressBar.setStringPainted(true);
        mainProgressBar.setValue(0);
        panel2.add(mainProgressBar, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel3, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(212, 301), null, 0, false));
        vSplitPane = new JSplitPane();
        vSplitPane.setContinuousLayout(true);
        vSplitPane.setDividerLocation(232);
        vSplitPane.setOneTouchExpandable(false);
        panel3.add(vSplitPane, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200), null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel4.setBackground(new Color(-14664286));
        panel4.setOpaque(true);
        vSplitPane.setLeftComponent(panel4);
        repoScrollPanel = new JScrollPane();
        panel4.add(repoScrollPanel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        coreRepoTree = new JTree();
        repoScrollPanel.setViewportView(coreRepoTree);
        final JLabel label1 = new JLabel();
        label1.setBackground(new Color(-14664286));
        label1.setForeground(new Color(-1));
        label1.setOpaque(true);
        label1.setText("My Core Repository");
        panel4.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        infoPane = new JPanel();
        infoPane.setLayout(new CardLayout(0, 0));
        vSplitPane.setRightComponent(infoPane);
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel5, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPane;
    }
}
