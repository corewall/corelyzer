/******************************************************************************
 *
 * CoreWall / Corelyzer - An Initial Core Description Tool
 * Copyright (C) 2007 Julian Yu-Chung Chen
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
package corelyzer.plugin.iCores.ui.tree;

import com.sun.syndication.feed.synd.SyndCategory;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndLink;
import corelyzer.plugin.iCores.cache.CacheFeedUtil;
import corelyzer.plugin.iCores.ui.ICoreFrame;

import javax.swing.*;
import javax.swing.tree.TreeNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public class SubscriptionTreeNode implements TreeNode {
    private final SyndFeed feed;
    private final SubscriptionTreeNode parent;
    private final List<SubscriptionTreeNode> children;
    private ImageIcon icon = null;

    public SubscriptionTreeNode(SyndFeed feed, SubscriptionTreeNode parent) {
        this.feed = feed;
        this.parent = parent;
        children = new ArrayList<SubscriptionTreeNode>();
        // parseChildren();
    }

    public SyndFeed getFeed() {
        return feed;
    }

    public Enumeration<SubscriptionTreeNode> children() {
        return Collections.enumeration(children);
    }

    public boolean getAllowsChildren() {
        return false;
    }

    public TreeNode getChildAt(int i) {
        if (i >= 0 && i < children.size()) {
            return children.get(i);
        } else {
            return null;
        }
    }

    public int getChildCount() {
        return children.size();
    }

    public int getIndex(TreeNode node) {
        return children.indexOf(node);
    }

    public TreeNode getParent() {
        return parent;
    }

    public boolean isLeaf() {
        return children.size() == 0;
    }

    public void parseChildren() {
        // walk the entries and pull out any entries that are marked as category "feed"
        List<SyndEntry> entries = feed.getEntries();
        for (SyndEntry entry : entries) {
            if (isFeed(entry)) {
                SyndFeed feed = parseFeed(entry);
                if (feed != null) {
                    children.add(new SubscriptionTreeNode(feed, this));
                }
            }
        }
    }

    private boolean isFeed(SyndEntry entry) {
        boolean isFeed = false;
        List<SyndCategory> categories = entry.getCategories();
        for (SyndCategory category : categories) {
            if (category.getName().equalsIgnoreCase("feed")) {
                isFeed = true;
            }
        }
        return isFeed;
    }

    // TODO need to add lazy evaluation
    private SyndFeed parseFeed(SyndEntry entry) {
        // try the link first
        SyndLink link = null;

        // look for a rel="self" link
        List<SyndLink> links = entry.getLinks();
        for (SyndLink l : links) {
            if (l.getRel().equalsIgnoreCase("self")) {
                link = l;
            }
        }

        // no rel="self" link, so pick the first one
        if (link == null && links.size() > 0) {
            link = links.get(0);
        }

        // bail if no link
        if (link == null) {
            System.out.println("---> [DEBUG] Bailed, link is null");
            return null;
        }

        // parse the feed FIXME
        // return ROMEUtils.readFeed(link.getHref());
        System.out.println("---> [DEBUG] Return feed with cache using URL: '" + link.getHref() + "'");
        return CacheFeedUtil.readFeed(ICoreFrame.getCacheMgr(), link.getHref());
    }

    public ImageIcon getIcon() {
        if (icon != null) {
            return icon;
        }

        // check for any rel="icon" links
        List<SyndLink> links = feed.getLinks();
        if (links == null) {
            return null;
        }

        // check the links for a rel="" containing icon
        SyndLink iconLink = null;
        for (SyndLink l : links) {
            if (l.getRel().toLowerCase().indexOf("icon") > -1) {
                iconLink = l;
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
        return feed.getTitle();
    }

    public List<SubscriptionTreeNode> getChildren() {
        return children;
    }


}
