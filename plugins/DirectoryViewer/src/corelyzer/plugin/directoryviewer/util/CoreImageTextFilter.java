package corelyzer.plugin.directoryviewer.util;

import java.util.List;

import ca.odell.glazedlists.TextFilterator;
import corelyzer.lib.datamodel.CoreImageFile;

/**
 * A text filter for an entry.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class CoreImageTextFilter implements TextFilterator<CoreImageFile> {

    /**
     * {@inheritDoc}
     */
    public void getFilterStrings(final List<String> list,
            final CoreImageFile cif) {
        // add our track name
        String track = cif.getConfig().getTrack();
        if ((track == null) || track.equals("")) {
            track = cif.getFile().getParentFile().getName();
        }

        // add our track name and depth
        list.add(track);
        list.add("" + Math.floor(cif.getDepth()));
    }
}
