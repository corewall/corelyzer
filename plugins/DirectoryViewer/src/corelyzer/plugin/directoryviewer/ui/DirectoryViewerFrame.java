package corelyzer.plugin.directoryviewer.ui;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.swing.EventTableModel;
import ca.odell.glazedlists.swing.TextComponentMatcherEditor;
import corelyzer.lib.datamodel.CoreImageDirectory;
import corelyzer.lib.datamodel.CoreImageFile;
import corelyzer.lib.datamodel.ui.CoreImageConfigurationDialog;
import corelyzer.plugin.directoryviewer.DirectoryViewerPlugin;
import corelyzer.plugin.directoryviewer.util.AddCoreImageFileTask;
import corelyzer.plugin.directoryviewer.util.CoreImageTableFormat;
import corelyzer.plugin.directoryviewer.util.CoreImageTextFilter;
import corelyzer.util.TableSorter;
import org.jdesktop.layout.GroupLayout;
import org.jdesktop.layout.LayoutStyle;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * A frame for viewing images in a directory.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class DirectoryViewerFrame extends JFrame {
    private static final long serialVersionUID = 3475353465411664899L;

    // fields
    private DefaultListModel model = new DefaultListModel();
    private EventList<CoreImageFile> images = GlazedLists
            .eventList(new ArrayList<CoreImageFile>());
    private boolean initialized = false;

    // components
    private JButton addButton;
    private JButton allButton;
    private JLabel directoryLabel;
    private JList directoryList;
    private JScrollPane directoryScrollPane;
    private JTable fileTable;
    private JPanel filesPanel;
    private JScrollPane filesScrollPane;
    private JLabel filterLabel;
    private JTextField filterText;
    private JPanel listPanel;
    private JButton removeButton;
    private JSplitPane splitPane;

    /** Creates new form NewJFrame */
    public DirectoryViewerFrame() {
        initComponents();
    }

    private void addRecent(final File file) {
        Preferences prefs = Preferences.userNodeForPackage(getClass());

        // update our recent paths list
        boolean match = false;
        String[] recent = prefs.get("recent", "").split(File.pathSeparator);
        StringBuffer buffer = new StringBuffer();
        for (String path : recent) {
            if ((path != null) && !path.equals("")) {
                buffer.append(path + File.pathSeparator);
            }

            if (path.equals(file.getAbsolutePath())) {
                match = true;
            }
        }
        if (!match) {
            buffer.append(file.getAbsolutePath());
        }
        prefs.put("recent", buffer.toString());

        // flush our changes
        try {
            prefs.flush();
        } catch (BackingStoreException e) {
            e.printStackTrace();
        }
    }

    private void initComponents() {
        splitPane = new JSplitPane();
        listPanel = new JPanel();
        directoryLabel = new JLabel();
        directoryScrollPane = new JScrollPane();
        directoryList = new JList();
        addButton = new JButton();
        allButton = new JButton();
        removeButton = new JButton();
        filesPanel = new JPanel();
        filterLabel = new JLabel();
        filterText = new JTextField();
        filesScrollPane = new JScrollPane();
        fileTable = new JTable();

        setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);

        splitPane.setDividerLocation(130);
        splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);

        directoryLabel.setText("Directories:");

        directoryScrollPane.setViewportView(directoryList);

        directoryList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        directoryList.setModel(model);
        directoryList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(final ListSelectionEvent evt) {
                onSelection();
            }
        });

        addButton.setText("+");
        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent evt) {
                onAdd();
            }
        });

        allButton.setText("*");
        allButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent evt) {
                onAll();
            }
        });

        removeButton.setText("-");
        removeButton.setEnabled(false);
        removeButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent evt) {
                onRemove();
            }
        });

        // setup our table
        FilterList<CoreImageFile> filteredList = new FilterList<CoreImageFile>(
                images, new TextComponentMatcherEditor<CoreImageFile>(
                        filterText, new CoreImageTextFilter()));

        EventTableModel<CoreImageFile> _model = new EventTableModel<CoreImageFile>(filteredList,
                new CoreImageTableFormat());

        // add table sorter
        TableSorter sorter = new TableSorter();
        sorter.setTableModel(_model);
        sorter.setTableHeader(fileTable.getTableHeader());        
        fileTable.setModel(sorter);

        GroupLayout listPanelLayout = new GroupLayout(listPanel);
        listPanel.setLayout(listPanelLayout);
        listPanelLayout
                .setHorizontalGroup(listPanelLayout
                        .createParallelGroup(GroupLayout.LEADING)
                        .add(
                                listPanelLayout
                                        .createSequentialGroup()
                                        .addContainerGap()
                                        .add(
                                                listPanelLayout
                                                        .createParallelGroup(
                                                                GroupLayout.LEADING)
                                                        .add(
                                                                directoryScrollPane,
                                                                GroupLayout.DEFAULT_SIZE,
                                                                395,
                                                                Short.MAX_VALUE)
                                                        .add(
                                                                listPanelLayout
                                                                        .createSequentialGroup()
                                                                        .add(
                                                                                directoryLabel)
                                                                        .addPreferredGap(
                                                                                LayoutStyle.RELATED,
                                                                                134,
                                                                                Short.MAX_VALUE)
                                                                        .add(
                                                                                addButton,
                                                                                GroupLayout.PREFERRED_SIZE,
                                                                                50,
                                                                                GroupLayout.PREFERRED_SIZE)
                                                                        .addPreferredGap(
                                                                                LayoutStyle.RELATED)
                                                                        .add(
                                                                                removeButton,
                                                                                GroupLayout.PREFERRED_SIZE,
                                                                                50,
                                                                                GroupLayout.PREFERRED_SIZE)
                                                                        .addPreferredGap(
                                                                                LayoutStyle.RELATED)
                                                                        .add(
                                                                                allButton,
                                                                                GroupLayout.PREFERRED_SIZE,
                                                                                50,
                                                                                GroupLayout.PREFERRED_SIZE)
                                                                        .addPreferredGap(
                                                                                LayoutStyle.RELATED)))
                                        .addContainerGap()));
        listPanelLayout.setVerticalGroup(listPanelLayout.createParallelGroup(
                GroupLayout.LEADING).add(
                listPanelLayout.createSequentialGroup().addContainerGap().add(
                        listPanelLayout.createParallelGroup(
                                GroupLayout.BASELINE).add(directoryLabel).add(
                                removeButton).add(addButton).add(allButton))
                        .addPreferredGap(LayoutStyle.RELATED).add(
                                directoryScrollPane, GroupLayout.DEFAULT_SIZE,
                                54, Short.MAX_VALUE).addContainerGap()));
        splitPane.setTopComponent(listPanel);

        filterLabel.setText("Filter:");

        filesScrollPane.setViewportView(fileTable);

        GroupLayout filesPanelLayout = new GroupLayout(filesPanel);
        filesPanel.setLayout(filesPanelLayout);
        filesPanelLayout
                .setHorizontalGroup(filesPanelLayout
                        .createParallelGroup(GroupLayout.LEADING)
                        .add(
                                GroupLayout.TRAILING,
                                filesPanelLayout
                                        .createSequentialGroup()
                                        .addContainerGap()
                                        .add(
                                                filesPanelLayout
                                                        .createParallelGroup(
                                                                GroupLayout.TRAILING)
                                                        .add(
                                                                GroupLayout.LEADING,
                                                                filesScrollPane,
                                                                GroupLayout.DEFAULT_SIZE,
                                                                395,
                                                                Short.MAX_VALUE)
                                                        .add(
                                                                filesPanelLayout
                                                                        .createSequentialGroup()
                                                                        .add(
                                                                                filterLabel)
                                                                        .addPreferredGap(
                                                                                LayoutStyle.RELATED)
                                                                        .add(
                                                                                filterText,
                                                                                GroupLayout.DEFAULT_SIZE,
                                                                                349,
                                                                                Short.MAX_VALUE)))
                                        .addContainerGap()));
        filesPanelLayout.setVerticalGroup(filesPanelLayout.createParallelGroup(
                GroupLayout.LEADING).add(
                filesPanelLayout.createSequentialGroup().addContainerGap().add(
                        filesPanelLayout.createParallelGroup(
                                GroupLayout.BASELINE).add(filterLabel).add(
                                filterText, GroupLayout.PREFERRED_SIZE,
                                GroupLayout.DEFAULT_SIZE,
                                GroupLayout.PREFERRED_SIZE)).addPreferredGap(
                        LayoutStyle.RELATED).add(filesScrollPane,
                        GroupLayout.DEFAULT_SIZE, 191, Short.MAX_VALUE)
                        .addContainerGap()));
        splitPane.setRightComponent(filesPanel);

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(
                GroupLayout.LEADING).add(splitPane, GroupLayout.DEFAULT_SIZE,
                439, Short.MAX_VALUE));
        layout
                .setVerticalGroup(layout.createParallelGroup(
                        GroupLayout.LEADING).add(splitPane,
                        GroupLayout.DEFAULT_SIZE, 389, Short.MAX_VALUE));
        pack();
    }

    private void initRecent() {
        Preferences prefs = Preferences.userNodeForPackage(getClass());

        // update our recent paths list
        String[] recent = prefs.get("recent", "").split(File.pathSeparator);
        for (String path : recent) {
            CoreImageDirectory directory = new CoreImageDirectory(
                    new File(path));
            if ((directory != null) && (directory.getImages().size() > 0)) {
                model.addElement(directory);
                images.addAll(directory.getImages());
            }
        }

        initialized = true;
    }

    private void onAdd() {
        CoreImageConfigurationDialog dialog = new CoreImageConfigurationDialog(
                this);
        dialog.setVisible(true);
        CoreImageDirectory directory = dialog.getCoreImageDirectory();
        if (directory != null) {
            model.addElement(directory);
            images.addAll(directory.getImages());
            addRecent(directory.getDirectory());
        }
    }

    private void onAll() {
        for (CoreImageFile cif : images) {
            DirectoryViewerPlugin.getDefault().submitJob(
                    new AddCoreImageFileTask(cif));
        }
    }

    private void onRemove() {
        CoreImageDirectory directory = (CoreImageDirectory) directoryList
                .getSelectedValue();
        if (directory != null) {
            model.removeElement(directory);
            images.removeAll(directory.getImages());
            removeRecent(directory.getDirectory());
        }
    }

    private void onSelection() {
        CoreImageDirectory directory = (CoreImageDirectory) directoryList
                .getSelectedValue();

        // update the button state
        boolean hasSelection = (directory != null);
        removeButton.setEnabled(hasSelection);
    }

    private void removeRecent(final File file) {
        Preferences prefs = Preferences.userNodeForPackage(getClass());

        // update our recent paths list
        String[] recent = prefs.get("recent", "").split(File.pathSeparator);
        StringBuffer buffer = new StringBuffer();
        for (String path : recent) {
            if (!path.equals(file.getAbsolutePath())) {
                buffer.append(path + File.pathSeparator);
            }
        }
        prefs.put("recent", buffer.toString());

        // flush our changes
        try {
            prefs.flush();
        } catch (BackingStoreException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setVisible(final boolean visible) {
        super.setVisible(visible);
        if (visible && !initialized) {
            initRecent();
        }
    }
}
