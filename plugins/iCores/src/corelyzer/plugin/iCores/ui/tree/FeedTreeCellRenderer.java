package corelyzer.plugin.iCores.ui.tree;

import com.sun.syndication.feed.synd.SyndFeed;

import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;

/**
 * Customize the tree cell renderer to ask the FeedTreeNode for a description and an icon.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class FeedTreeCellRenderer extends DefaultTreeCellRenderer {
	private static final long serialVersionUID = -12379661031392302L;

	/**
	 * Override so we can get the tooltip from the FeedTreeNode.
	 */
	@Override
	public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
		super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
		
		// check if dealing with a FeedTreeNode
		if (value instanceof FeedTreeNode) {
			FeedTreeNode node = (FeedTreeNode) value;
			setIcon(node.getIcon());
			setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));

            SyndFeed feed = node.getFeed();
            if(feed != null) {
                setToolTipText(feed.getDescription());
            }

        }
		
		return this;
	}

}
