package corelyzer.plugin.directoryviewer.util;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import ca.odell.glazedlists.gui.AdvancedTableFormat;
import ca.odell.glazedlists.gui.WritableTableFormat;
import corelyzer.data.CoreSection;
import corelyzer.data.TrackSceneNode;
import corelyzer.helper.SceneGraph;
import corelyzer.lib.datamodel.CoreImageFile;
import corelyzer.plugin.directoryviewer.DirectoryViewerPlugin;
import corelyzer.ui.CorelyzerApp;

/**
 * Our core image file table format.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class CoreImageTableFormat implements
        WritableTableFormat<CoreImageFile>, AdvancedTableFormat<CoreImageFile> {
    private static class BooleanComparator implements Comparator<Boolean> {
        public int compare(final Boolean o1, final Boolean o2) {
            return o1.compareTo(o2);
        }
    }

    private static class DoubleComparator implements Comparator<Double> {
        public int compare(final Double o1, final Double o2) {
            return o1.compareTo(o2);
        }
    }

    private static class StringComparator implements Comparator<String> {
        public int compare(final String o1, final String o2) {
            return o1.compareTo(o2);
        }
    }

    private static final DecimalFormat FORMAT = new DecimalFormat("0.00");

    private List<CoreImageFile> checked = new ArrayList<CoreImageFile>();

    /**
     * Called when an image should be added to Corelyzer.
     * 
     * @param cif
     *            the core image to be added to corelyzer.
     */
    private void addImage(final CoreImageFile cif) {
        DirectoryViewerPlugin.getDefault().submitJob(
                new AddCoreImageFileTask(cif));
    }

    /**
     * {@inheritDoc}
     */
    public boolean getChecked(final CoreImageFile cif) {
        return checked.contains(cif);
    }

    /**
     * {@inheritDoc}
     */
    public Class getColumnClass(final int column) {
        switch (column) {
            case 0:
                return String.class;
            case 1:
                return String.class;
            case 2:
                return Double.class;
            case 3:
                return Double.class;
            case 4:
                return Boolean.class;
        }
        return null;
    }

    public Comparator getColumnComparator(final int column) {
        switch (column) {
            case 0:
                return new StringComparator();
            case 1:
                return new StringComparator();
            case 2:
                return new DoubleComparator();
            case 3:
                return new DoubleComparator();
            case 4:
                return new BooleanComparator();
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public int getColumnCount() {
        return 5;
    }

    /**
     * {@inheritDoc}
     */
    public String getColumnName(final int column) {
        switch (column) {
            case 0:
                return "Track";
            case 1:
                return "Name";
            case 2:
                return "Depth";
            case 3:
                return "Length";
            case 4:
                return "Display";
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public Object getColumnValue(final CoreImageFile cif, final int column) {
        switch (column) {
            case 0:
                String track = cif.getConfig().getTrack();
                if ((track == null) || track.equals("")) {
                    track = cif.getFile().getParentFile().getName();
                }
                return track;
            case 1:
                return cif.getFile().getName();
            case 2:
                return cif.getDepth();
            case 3:
                return cif.getLength();
            case 4:
                return checked.contains(cif) || isChecked(cif);
        }
        return null;
    }

    private int getTrack(final CoreImageFile cif) {
        String trackName = cif.getConfig().getTrack();
        if ((trackName == null) || trackName.equals("")) {
            trackName = cif.getFile().getParentFile().getName();
        }

        return SceneGraph.getTrackIDByName(trackName);
    }

    private boolean isChecked(final CoreImageFile cif) {
        // get our track id
        int trackId = getTrack(cif);
        if (trackId == -1) {
            return false;
        }
        
        // get the name and track
        String name = cif.getFile().getName().substring(0,
                cif.getFile().getName().lastIndexOf('.'));
        TrackSceneNode track = (TrackSceneNode) CorelyzerApp.getApp()
                .getTrackListModel().get(trackId);
        if (track == null) {
            return false;
        }

        CoreSection cs = track.getCoreSection(name);
        return (cs != null);
    }

    /**
     * Only the "display" column is available
     */
    public boolean isEditable(final CoreImageFile cif, final int column) {
        return (column == 4);
    }

    /**
     * Called when an image is to be removed from Corelyzer.
     * 
     * @param cif
     *            the core image file.
     */
    private void removeImage(final CoreImageFile cif) {
        DirectoryViewerPlugin.getDefault().submitJob(
                new RemoveCoreImageFileTask(cif));
    }

    /**
     * {@inheritDoc}
     */
    public CoreImageFile setColumnValue(final CoreImageFile cif,
            final Object value, final int column) {
        // update the checked list
        if (column == 4) {
            if (checked.contains(cif)) {
                checked.remove(cif);
                removeImage(cif);
            } else {
                checked.add(cif);
                addImage(cif);
            }
        }
        return cif;
    }
}

