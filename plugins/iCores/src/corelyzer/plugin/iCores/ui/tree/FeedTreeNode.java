package corelyzer.plugin.iCores.ui.tree;

import com.sun.syndication.feed.synd.SyndCategory;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndLink;
import corelyzer.plugin.iCores.cache.CacheEntry;
import corelyzer.plugin.iCores.cache.CacheFeedUtil;
import corelyzer.plugin.iCores.cache.CacheManager;
import corelyzer.plugin.iCores.ui.ICoreFrame;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.image.ImageObserver;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import javax.swing.*;
import javax.swing.tree.TreeNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * A TreeNode implementation that wraps a SyndFeed element.
 * It exposes all entries that have a category
 * of "feed" as children.  Other entries are ignored but accessible via the
 * getFeed() method.
 * 
 * @author Josh Reed (jareed@andrill.org)
 * @author Julian Yu-Chung Chen (julian@evl.uic.edu)
 */
public class FeedTreeNode extends DefaultMutableTreeNode implements Runnable {
    private final FeedTreeNode parent;
    private final List<FeedTreeNode> children;
    private SyndFeed feed;
    private ImageIcon icon;

    private String title;
    private String url;
    private String type; // 'category' or 'subscription'
    private boolean isUpdateing = false;    
    private CacheEntry cacheEntry; // for refresh updates
        
    /**
	 * Create a new FeedTreeNode.
	 *
	 * @param feed the feed.
     * @param parent parent node
	 */
    public FeedTreeNode(SyndFeed feed, FeedTreeNode parent) {
		this.feed = feed;
		this.parent = parent;
		children = new ArrayList<FeedTreeNode>();

        this.title = feed.getTitle();
        this.url   = feed.getLink();

        if(this.isIntermediateNode(feed)) {
            type = "category";
            this.parseChildren();
        } else {
            type = "subscription";
            icon = new ImageIcon(getClass().getResource(
                  "/corelyzer/plugin/iCores/ui/resources/icons/indicator.gif"));
            setObserver();
        }
    }

    public FeedTreeNode(SyndFeed feed, FeedTreeNode parent, ImageIcon aIcon) {
        this(feed, parent);

        if(aIcon == null) {
            icon = new ImageIcon(getClass().getResource(
                  "/corelyzer/plugin/iCores/ui/resources/icons/indicator.gif"));

        } else {
            this.icon = aIcon;
        }
        setObserver();
    }

    // Constructor for feed's later evaluation, used for remote subscriptions
    public FeedTreeNode(String title, String url, FeedTreeNode aParent) {
        this.title = title;
        this.url = url;
        parent = aParent;

        type = "subscription";
        children = new ArrayList<FeedTreeNode>();
        icon = new ImageIcon(getClass().getResource(
                "/corelyzer/plugin/iCores/ui/resources/icons/indicator.gif"));
        setObserver();

        // TODO fix the not-spinning indicator here?
        CacheEntry cEntry = CacheManager.getCacheManager().fetch(url);
        if(cEntry != null) {
            cEntry.setObserver(this);
        }
    }

    private void setObserver() {
        if (icon != null) {
            // if (icon.getImageObserver() == null) {
                JTree aTree = ICoreFrame.getIcoreFrame().getRepoTree();
                icon.setImageObserver(
                        new NodeImageObserver(aTree, this));
            // } else {
            //     System.out.println("---> Sorry already has an observer");
            // }
        } else {
            System.out.println("---> Sorry no observer, icon is null");
        }
    }

    public void setFeed(SyndFeed aFeed) {
        feed = aFeed;
    }

    /**
	 * Get the feed that this tree node wraps.
	 * 
	 * @return the feed.
	 */
	public SyndFeed getFeed() {
		return feed;
	}

	/**
	 * {@inheritDoc}
	 */
	public Enumeration<TreeNode> children() {
        List<TreeNode> kids = new ArrayList<TreeNode>();
        for (TreeNode tn : children) {
            kids.add(tn);
        }
		return Collections.enumeration(kids);
    }

	/**
	 * Don't allow children?
	 */
	public boolean getAllowsChildren() {
		// return false;
        return type.equalsIgnoreCase("category");
    }

	/**
	 * {@inheritDoc}
	 */
	public TreeNode getChildAt(int i) {
		if (i >= 0 && i < children.size()) {
			return children.get(i);
		} else {
			return null;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public int getChildCount() {
		return children.size();
	}

	/**
	 * {@inheritDoc}
	 */
	public int getIndex(TreeNode node) {
        //noinspection SuspiciousMethodCalls
        return children.indexOf(node);
	}

	/**
	 * {@inheritDoc}
	 */
	public TreeNode getParent() {
		return parent;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isLeaf() {
		return (children.size() == 0);
	}

	public void parseChildren() {
		// walk the entries and pull out any entries that are marked as category "feed"
		List entries = feed.getEntries();
		for (Object obj : entries) {
            if(obj instanceof SyndEntry) {
                SyndEntry entry = (SyndEntry) obj;

                if (isFeed(entry)) {
                    // Add it anyway 1st and then refresh a littlt bit
                    // later with some animated icon
                    if(feed.getTitle().equalsIgnoreCase("subscriptions")) {
                        FeedTreeNode aNode = new FeedTreeNode(entry.getTitle(),
                                entry.getLink(), this);
                        children.add(aNode);

                        // Add to iCoreFrame's allSubscriptions
                        ICoreFrame.getIcoreFrame().addSubscriptionNode(aNode);

                        (new Thread(aNode)).start();
                    } else {
                        SyndFeed feed = parseFeed(entry);
                        if (feed != null) {
                            children.add(new FeedTreeNode(feed, this));
                        }                        
                    }
                }
            }
        }
	}

    private boolean isIntermediateNode(SyndFeed feed) {
        String title = feed.getTitle();

        return title.equalsIgnoreCase("local") ||
                title.equalsIgnoreCase("subscriptions") ||
                title.equalsIgnoreCase("cml") ||
                title.equalsIgnoreCase("icores root");
    }

    private boolean isFeed(SyndEntry entry) {
		boolean isFeed = false;
		List categories = entry.getCategories();
		for (Object obj : categories) {
            if(obj instanceof SyndCategory) {
                SyndCategory category = (SyndCategory) obj;
                if (category.getName().equalsIgnoreCase("feed")) {
                    isFeed = true;
                }
            }
        }
        
        return isFeed;
	}

    // Need to add lazy evaluation
    private SyndFeed parseFeed(SyndEntry entry) {
		// try the link first
		SyndLink link = null;
		
		// look for a rel="self" link
		List links = entry.getLinks();
		for (Object obj : links) {
            if(obj instanceof SyndLink) {
                SyndLink l = (SyndLink) obj;
                if (l.getRel().equalsIgnoreCase("self")) {
                    link = l;
                }
            }
        }
		
		// no rel="self" link, so pick the first one
		if (link == null && links.size() > 0) {
			link = (SyndLink) links.get(0);
		}
		
		// bail if no link
		if (link == null) {
            System.out.println("---> [DEBUG] Bailed, link is null");
            return null;
		}
		
        return CacheFeedUtil.readFeed(ICoreFrame.getCacheMgr(), link.getHref());
    }
	
	/**
	 * Gets the icon associated with this link.
	 * 
	 * @return the icon.
	 */
	public ImageIcon getIcon() {
        if (icon != null && type.equalsIgnoreCase("subscription")) {
            if(isUpdateing) {
                return icon;
            }
		}

        if(feed == null) return null;

        // check for any rel="icon" links
		List links = feed.getLinks();
		if (links == null) {
			return null;
		}
		
		// check the links for a rel="" containing icon
		SyndLink iconLink = null;
		for (Object obj : links) {
            if(obj instanceof SyndLink) {
                SyndLink l = (SyndLink) obj;
                
                if (l.getRel().toLowerCase().indexOf("icon") > -1) {
                    iconLink = l;
                }
            }
        }
		
		// check if we got something
		if (iconLink == null) {
			return null;
		} else {
			icon = new ImageIcon(iconLink.getHref());
			return icon;
		}
	}
	
	/**
	 * Return the feed title.
	 */
	public String toString() {
        if(title != null) return title;
        
        return "Unknown";
    }

    public List<FeedTreeNode> getChildren() {
        return children;
    }

    public void setIcon(ImageIcon icon) {
        this.icon = icon;
    }

    public boolean isUpdateing() {
        return isUpdateing;
    }

    // For refresh & update the 'feed' object
    // called when 1. just subscribe a feed
    //             2. refresh a subscribed feed
    //             3. created with pre-existing
    public void run() {
        updateUI(true);

        // Refresh cacheEntry
        // Do a CacheEntry refresh download before parsing as a feed
        CacheManager cacheMgr = CacheManager.getCacheManager();
        if(cacheMgr == null) {
            System.out.println("---> No cacheManager, just return");    
        }

        if(feed == null) {
            if(url == null || url.equals("")) {
                System.out.println("---> No access ways, ignore the node");
                return;
            } else {
                System.out.println("---> Refresh using 'this.url' '" +
                        this.url + "'");

                feed = CacheFeedUtil.readFeed(CacheManager.getCacheManager(),
                        this);

                if(feed != null) {
                    this.title = feed.getTitle();
                }
            }
        } else {
            System.out.println("---> Refresh using url: " + url + "'");
            String cacheDir = CacheManager.getCacheManager().getCacheDir();

            boolean isDownloaded = cacheEntry.doDownload(cacheDir);
            if (isDownloaded) {
                feed = CacheFeedUtil.readFeed(CacheManager.getCacheManager(),
                        url);
            }

            if(feed != null) {
                this.title = feed.getTitle();
            }

            // TODO Fire a event to update the table
            /*
            JTree aTree = ICoreFrame.getIcoreFrame().getRepoTree();
            DefaultTreeModel model = (DefaultTreeModel) aTree.getModel();
            TreePath path = new TreePath(model.getPathToRoot(this));

            TreeSelectionEvent selectionEvent =
                    new TreeSelectionEvent(this, path, true, path, path);
            aTree.getTreeSelectionListeners()[0].valueChanged(selectionEvent);
            */
        }

        System.gc();

        updateUI(false);
    }

    private void updateUI(boolean running) {
        this.isUpdateing = running;

        SwingUtilities.invokeLater(
                new Runnable() {
                    public void run() {
                        ICoreFrame.getIcoreFrame().updateTreeUI();
                    }
                }
        );
    }

    public void setCacheEntry(CacheEntry anEntry) {
        this.cacheEntry = anEntry;
    }

    public CacheEntry getCacheEntry() {
        return cacheEntry;
    }

    public String getUrl() {
        return url;
    }

    public String getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    // For animated image update    
    class NodeImageObserver implements ImageObserver {
        JTree tree;
        DefaultTreeModel model;
        TreeNode node;

        public NodeImageObserver(JTree aTree, TreeNode aNode) {
            this.tree = aTree;
            this.model = (DefaultTreeModel) tree.getModel();
            this.node = aNode;
        }

        public boolean imageUpdate(Image img, int flags, int x, int y, int w,
                                   int h) {
            if ((flags & (FRAMEBITS | ALLBITS)) != 0) {
                TreePath path = new TreePath(model.getPathToRoot(node));
                Rectangle rect = tree.getPathBounds(path);
                if (rect != null) {
                    tree.repaint(rect);
                }
            }
            return (flags & (ALLBITS | ABORT)) == 0;
        }
    }

}
