package corelyzer.plugin.iCores.ui.tree;

import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

/**
 * A tree model for feeds.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class FeedTreeModel implements TreeModel {
	private final String feedURL;
	
	/**
	 * Create a new FeedTreeModel for the specified URL.
	 * 
	 * @param feedURL the specified URL.
	 */
	public FeedTreeModel(String feedURL) {
		this.feedURL = feedURL;
	}
	
	public void addTreeModelListener(TreeModelListener arg0) {
		// TODO Auto-generated method stub

	}

	public Object getChild(Object arg0, int arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	public int getChildCount(Object arg0) {
		// TODO Auto-generated method stub
		return 0;
	}

	public int getIndexOfChild(Object arg0, Object arg1) {
		// TODO Auto-generated method stub
		return 0;
	}

	public Object getRoot() {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isLeaf(Object arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	public void removeTreeModelListener(TreeModelListener arg0) {
		// TODO Auto-generated method stub

	}

	public void valueForPathChanged(TreePath arg0, Object arg1) {
		// TODO Auto-generated method stub

	}

}
