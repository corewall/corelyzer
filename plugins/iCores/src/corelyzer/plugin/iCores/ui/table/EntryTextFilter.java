package corelyzer.plugin.iCores.ui.table;

import ca.odell.glazedlists.TextFilterator;
import com.sun.syndication.feed.synd.SyndEntry;

import java.util.List;

/**
 * A text filter for an entry.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class EntryTextFilter implements TextFilterator<SyndEntry> {

	/**
	 * Get the filter strings.
	 */
	public void getFilterStrings(List<String> list, SyndEntry entry) {
        list.add(EntryTableFormat.getType(entry));
		list.add(entry.getTitle());
		list.add(EntryTableFormat.getDepth(entry));
        list.add(EntryTableFormat.getLength(entry));
        list.add(EntryTableFormat.getDPI(entry));
        list.add(EntryTableFormat.getURL(entry));
        list.add(EntryTableFormat.getOrientation(entry));
        list.add(EntryTableFormat.getStatus(entry).getText()); // JLabel for status
        list.add(EntryTableFormat.getThumbnail(entry));
        list.add(entry.getTitle()); // SyndEntry entry for summary panel
        list.add(Boolean.toString(false)); // FIXME
    }
}
