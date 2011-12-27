package corelyzer.plugin.iCores.ui.tree;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.swing.EventTableModel;
import ca.odell.glazedlists.swing.TextComponentMatcherEditor;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import corelyzer.plugin.iCores.ui.FilterTablePanel;
import corelyzer.plugin.iCores.ui.ICoreFrame;
import corelyzer.plugin.iCores.ui.table.EntryTableFormat;
import corelyzer.plugin.iCores.ui.table.EntryTextFilter;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.DefaultTableModel;

/**
 * Listen to tree selection events.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class FeedTreeSelectionListener implements TreeSelectionListener {
	private FeedTreeNode selected = null;
	private FilterTablePanel table = null;
    private JButton removeButton = null;

    public FeedTreeSelectionListener() {
        super();
    }
    
    /**
	 * Create a new FeedTreeSelectionListener.
	 * 
	 * @param table the table panel.
	 */
	public FeedTreeSelectionListener(FilterTablePanel table) {
        this();
        this.table = table;
	}

    public FeedTreeSelectionListener(FilterTablePanel table, JButton button) {
        this.table = table;
        this.removeButton = button; 
    }

    /**
	 * Only act on leaf selections.
	 */
	public void valueChanged(TreeSelectionEvent e) {
		Object obj = e.getNewLeadSelectionPath().getLastPathComponent();

        if (obj == null) return;

        if (obj instanceof FeedTreeNode) {
            if(!obj.toString().equalsIgnoreCase("local") &&
               !obj.toString().equalsIgnoreCase("subscriptions") &&
               !obj.toString().equalsIgnoreCase("cmls")
              )
            {
                removeButton.setEnabled(true);                
            } else {
                removeButton.setEnabled(false);

                // TODO
                if(obj.toString().equalsIgnoreCase("local")) {
                    ICoreFrame.getIcoreFrame().switchToLocalPane();
                } else if(obj.toString().equalsIgnoreCase("subscriptions")) {
                    ICoreFrame.getIcoreFrame().switchToInfoPane();
                } else if(obj.toString().equalsIgnoreCase("cmls")) {
                    ICoreFrame.getIcoreFrame().switchToInfoPane();                    
                } else {
                    System.out.println("ignore" + obj.toString());
                }

                return;
            }
            ICoreFrame.getIcoreFrame().switchToInfoPane();

            FeedTreeNode node = (FeedTreeNode) obj;

            if(node.isUpdateing()) {
                // Put up a blank/refreshing panel content
                System.out.println(
                        "---> [INFO] This feed is refreshing, ignore.");
                table.setTableModel(new DefaultTableModel());
                return;
            }
            
            // only register selection events with nodes that are leaves and
			// not direct descendents of the root node
			if (node.isLeaf() && node.getParent().getParent() != null) {
				if (node != selected) {
					selectedFeed(node.getFeed());
				}
				selected = node;
			}
		} else {
            if(removeButton != null) removeButton.setEnabled(false);            
        }
	}

    private void selectedFeed(SyndFeed feed) {
        //noinspection unchecked
        EventList<SyndEntry> entries = GlazedLists.eventList(feed.getEntries());

        FilterList<SyndEntry> filteredEntries =
                new FilterList<SyndEntry>(
                        entries,
                        new TextComponentMatcherEditor<SyndEntry>(
                                table.getFilterField(), new EntryTextFilter()));
		table.setTableModel(
                new EventTableModel<SyndEntry>(
                        filteredEntries, new EntryTableFormat()));

	}
}
