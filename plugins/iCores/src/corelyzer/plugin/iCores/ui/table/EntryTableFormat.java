package corelyzer.plugin.iCores.ui.table;

import ca.odell.glazedlists.gui.TableFormat;
import com.sun.syndication.feed.synd.SyndCategory;
import com.sun.syndication.feed.synd.SyndEntry;
import corelyzer.data.CRPreferences;
import corelyzer.plugin.iCores.cache.CacheEntry;
import corelyzer.plugin.iCores.cache.CacheManager;
import corelyzer.util.core.CoreModule;
import corelyzer.util.image.ImageModule;
import corelyzer.util.ROMEUtils;

import javax.swing.*;
import java.io.File;

/**
 * The table format for a list of feed entries.
 *
 * @author Josh Reed (jareed@andrill.org)
 */
public class EntryTableFormat implements TableFormat<SyndEntry> {

    /**
     * Gets the depth from an entry.
     *
     * @param entry the entry.
     * @return the formatted depth or "-" if no depth.
     */
    public static String getDepth(SyndEntry entry) {
        CoreModule module = (CoreModule) entry.getModule(CoreModule.CORE_URI);
        if (module == null) {
            return "-";
        } else {
            return ROMEUtils.DOUBLE.format(module.getDepth());
        }
    }

    public static String getDPI(SyndEntry entry) {
        ImageModule module =
                (ImageModule) entry.getModule(ImageModule.IMAGE_URI);

        if (module == null) {
            return "-";
        } else {
            String dpix = ROMEUtils.DOUBLE.format(module.getDPIX());
            String dpiy = ROMEUtils.DOUBLE.format(module.getDPIY());
            return dpix + ", " + dpiy;
        }
    }

    public static String getLength(SyndEntry entry) {
        CoreModule module = (CoreModule) entry.getModule(CoreModule.CORE_URI);
        if (module == null) {
            return "-";
        } else {
            return ROMEUtils.DOUBLE.format(module.getLength());
        }
    }

    public static String getOrientation(SyndEntry entry) {
        ImageModule module =
                (ImageModule) entry.getModule(ImageModule.IMAGE_URI);

        if (module == null) {
            return ImageModule.HORIZONTAL;
        } else {
            return module.getOrientation();
        }
    }

    public static JLabel getStatus(SyndEntry entry) {
        String status;
        CacheManager cacheMgr = CacheManager.getCacheManager();

        String url = entry.getLink();

        if(cacheMgr.hasItem(url)) {
            CacheEntry cacheEntry = cacheMgr.fetch(url);
            String sp = System.getProperty("file.separator");
            String localFileString = cacheMgr.getCacheDir() + sp + "downloads"
                    + sp + cacheEntry.getLocal();

            File aFile = new File(localFileString);
            if(aFile.exists()) {
                status = "Downloaded";
            } else {
                status = "N/A";
            }
        } else {
            status = "N/A";
        }

        JLabel aLabel;
        if(status.endsWith("ing")) { // TODO
            String resPath =
                    "/corelyzer/plugin/iCores/ui/resources/icons/pi.png";
            ImageIcon icon = new ImageIcon(
                    EntryTableFormat.class.getClassLoader().getResource(
                            resPath));

            aLabel = new JLabel(status, icon, 0);
            aLabel.setHorizontalAlignment(SwingConstants.LEADING);
        } else {
            aLabel = new JLabel(status);
        }

        return aLabel;
    }

    public static String getThumbnail(SyndEntry entry) {
        ImageModule module = (ImageModule) entry.getModule(ImageModule.IMAGE_URI);

        return (module == null) ? CRPreferences.defaultThumbnailURL :
                (module.getThumbnail());
    }

    /**
     * Gets the type of an entry.
     *
     * @param entry the entry.
     * @return the type of an entry of "-" if no type.
     */
    public static String getType(SyndEntry entry) {
        if (entry.getCategories().size() == 0) {
            return "-";
        } else {
            SyndCategory category = (SyndCategory) entry.getCategories().get(0);
            return category.getName();
        }
    }

    public static String getURL(SyndEntry entry) {
        if (entry == null) {
            return "-";
        } else {
            return entry.getLink(); // FIXME multi-resolution using 'Links'?
        }
    }

    public static SyndEntry getSyndEntry(SyndEntry entry) {
        return entry;
    }

    /**
     * {@inheritDoc}
     */
    public int getColumnCount() {
        return 10;
    }

    /**
     * {@inheritDoc}
     */
    public String getColumnName(int idx) {
        switch (idx) {
            case 0:
                return "Status";
            case 1:
                return "Type";
            case 2:
                return "Title";
            case 3:
                return "Depth";
            case 4:
                return "Length";
            case 5:
                return "DPI";
            case 6:
                return "URL";
            case 7:
                return "Orientation";
            case 8:
                return "Thumbnail";
            case 9:
                return "SyndEntry";
            default:
                return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public Object getColumnValue(SyndEntry entry, int idx) {
        switch (idx) {
            case 0:
                return getStatus(entry);
            case 1:
                return getType(entry);
            case 2:
                return entry.getTitle();
            case 3:
                return getDepth(entry);
            case 4:
                return getLength(entry);
            case 5:
                return getDPI(entry);
            case 6:
                return getURL(entry);
            case 7:
                return getOrientation(entry);
            case 8:
                return getThumbnail(entry);
            case 9:
                return getSyndEntry(entry);
        }
        
        return entry.getTitle();
	}
}
