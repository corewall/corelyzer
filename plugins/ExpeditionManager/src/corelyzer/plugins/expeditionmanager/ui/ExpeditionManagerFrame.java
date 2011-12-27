package corelyzer.plugins.expeditionmanager.ui;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.jdesktop.layout.GroupLayout;
import org.jdesktop.layout.LayoutStyle;
import org.jdesktop.swingx.JXFrame;
import org.jdesktop.swingx.JXHeader;
import org.xml.sax.SAXException;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.gui.AbstractTableComparatorChooser;
import ca.odell.glazedlists.swing.EventTableModel;
import ca.odell.glazedlists.swing.TableComparatorChooser;
import corelyzer.plugins.expeditionmanager.Expedition;
import corelyzer.plugins.expeditionmanager.ExpeditionManagerPlugin;
import corelyzer.plugins.expeditionmanager.ExpeditionXMLHandler;
import corelyzer.plugins.expeditionmanager.data.IDataStore;
import corelyzer.plugins.expeditionmanager.handlers.DataHandlerContext;
import corelyzer.plugins.expeditionmanager.handlers.DepthRange;
import corelyzer.plugins.expeditionmanager.handlers.IDataHandler;
import corelyzer.plugins.expeditionmanager.util.FileUtils;

/**
 * The Expedition Manager UI.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class ExpeditionManagerFrame extends JXFrame {
    private static final long serialVersionUID = 1L;

    // components
    private JTable dataStoreTable;
    private JXHeader header;
    private JTextField intervalField;
    private JLabel intervalLabel;
    private JButton setButton;
    private JLabel statusLabel;
    private JScrollPane tableScrollPane;
    private JFileChooser fileChooser;

    // fields
    private Expedition expedition;
    private EventList<IDataStore> dataStores = GlazedLists
            .eventList(new ArrayList<IDataStore>());
    private final DataHandlerContext context;
    private boolean initialized = false;

    /**
     * Create a new ExpeditionManagerFrame.
     */
    public ExpeditionManagerFrame() {
        initComponents();
        context = new DataHandlerContext(ExpeditionManagerPlugin.getDefault(),
                statusLabel);
    }

    private void initComponents() {
        // set our title and layout manager
        setTitle("Expedition Manager");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        // create our components
        header = new JXHeader();
        intervalLabel = new JLabel();
        intervalField = new JTextField();
        tableScrollPane = new JScrollPane();
        dataStoreTable = new JTable();
        statusLabel = new JLabel();
        setButton = new JButton();
        fileChooser = new JFileChooser();

        // configure our components
        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu("File");
        Action openAction = new AbstractAction("Open...") {
            private static final long serialVersionUID = 1L;

            /**
             * {@inheritDoc}
             */
            public void actionPerformed(ActionEvent action) {
                onOpen();
            }
        };
        menu.add(openAction);

        Action openURLAction = new AbstractAction("Open URL...") {
            private static final long serialVersionUID = 1L;

            /**
             * {@inheritDoc}
             */
            public void actionPerformed(ActionEvent action) {
                String url = JOptionPane.showInputDialog(
                        ExpeditionManagerFrame.this, "URL:", "");
                if (url != null) {

                    try {
                        open(new URL(url));
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        menu.add(openURLAction);

        menuBar.add(menu);
        setJMenuBar(menuBar);

        header.setFont(new Font("Tahoma", Font.BOLD, 18));

        intervalLabel.setText("Interval of Interest:");
        intervalField.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                setRange();
            }
        });

        // set up our table
        SortedList<IDataStore> sorted = new SortedList<IDataStore>(dataStores,
                new DataStoreComparator());
        dataStoreTable.setModel(new EventTableModel<IDataStore>(sorted,
                new DataStoreTableFormat()));
        TableComparatorChooser.install(dataStoreTable, sorted,
                AbstractTableComparatorChooser.MULTIPLE_COLUMN_MOUSE);

        tableScrollPane.setViewportView(dataStoreTable);

        setButton.setText("Set");
        setButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                setRange();
            }
        });

        statusLabel.setText(" ");

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(
                GroupLayout.LEADING).add(
                layout.createSequentialGroup().add(
                        layout.createParallelGroup(GroupLayout.LEADING).add(
                                layout.createSequentialGroup()
                                        .addContainerGap().add(header,
                                                GroupLayout.DEFAULT_SIZE, 417,
                                                Short.MAX_VALUE)).add(
                                layout.createSequentialGroup().add(20, 20, 20)
                                        .add(intervalLabel).addPreferredGap(
                                                LayoutStyle.RELATED).add(
                                                intervalField,
                                                GroupLayout.DEFAULT_SIZE, 203,
                                                Short.MAX_VALUE)
                                        .addPreferredGap(LayoutStyle.RELATED)
                                        .add(setButton)).add(
                                layout.createSequentialGroup()
                                        .addContainerGap().add(statusLabel,
                                                GroupLayout.DEFAULT_SIZE, 417,
                                                Short.MAX_VALUE)).add(
                                layout.createSequentialGroup()
                                        .addContainerGap().add(tableScrollPane,
                                                GroupLayout.DEFAULT_SIZE, 417,
                                                Short.MAX_VALUE)))
                        .addContainerGap()));
        layout.setVerticalGroup(layout.createParallelGroup(GroupLayout.LEADING)
                .add(
                        layout.createSequentialGroup().addContainerGap().add(
                                header, GroupLayout.PREFERRED_SIZE, 100,
                                GroupLayout.PREFERRED_SIZE).add(19, 19, 19)
                                .add(
                                        layout.createParallelGroup(
                                                GroupLayout.BASELINE).add(
                                                intervalLabel).add(
                                                intervalField,
                                                GroupLayout.PREFERRED_SIZE,
                                                GroupLayout.DEFAULT_SIZE,
                                                GroupLayout.PREFERRED_SIZE)
                                                .add(setButton))
                                .addPreferredGap(LayoutStyle.RELATED).add(
                                        tableScrollPane,
                                        GroupLayout.DEFAULT_SIZE, 125,
                                        Short.MAX_VALUE).addPreferredGap(
                                        LayoutStyle.RELATED).add(statusLabel)
                                .addContainerGap()));

        // update our header
        updateHeader();

        // pack
        pack();
    }

    protected void onOpen() {
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            open(FileUtils.getURL(fileChooser.getSelectedFile()));
        }
    }

    private void open(final URL url) {
        Expedition expedition = new Expedition(url);
        ExpeditionXMLHandler handler = new ExpeditionXMLHandler(expedition);
        final SAXParserFactory factory = SAXParserFactory.newInstance();
        try {
            factory.newSAXParser().parse(url.openStream(), handler);
        } catch (final SAXException e) {
            expedition = null;
        } catch (final IOException e) {
            expedition = null;
        } catch (final ParserConfigurationException e) {
            expedition = null;
        }

        if (expedition == null) {
            JOptionPane.showMessageDialog(this, url.getFile()
                    + " was not an expedition file");
        } else {
            Preferences prefs = Preferences.userNodeForPackage(getClass());
            prefs.put("expedition", url.toExternalForm());
            try {
                prefs.flush();
            } catch (BackingStoreException e) {
                e.printStackTrace(System.err);
            }
        }
        setExpedition(expedition);
    }

    /**
     * Sets the current expedition.
     * 
     * @param expedition
     *            the expedition.
     */
    public void setExpedition(final Expedition expedition) {
        this.expedition = expedition;

        // update the header
        updateHeader();
        updateDataStores();
    }

    private void setRange() {
        SimpleExpression expression = SimpleExpression.parse(intervalField
                .getText());
        if (expression != null) {
            // parse our operands
            double op1 = 0.0;
            if (SimpleExpression.isNumber(expression.getOperand1())) {
                op1 = Double.parseDouble(expression.getOperand1());
            }

            double op2 = 0.0;
            if (SimpleExpression.isNumber(expression.getOperand2())) {
                op2 = Double.parseDouble(expression.getOperand2());
            }

            // setup our range
            DepthRange range = new DepthRange();
            if (expression.getOperator().equals("<")) {
                range.setTop(0.0);
                range.setBottom(op1);
            } else if (expression.getOperator().equals(">")) {
                range.setTop(op1);
                range.setBottom(5000);
            } else if (expression.getOperator().equals("-")) {
                range.setTop(op1);
                range.setBottom(op2);
            } else {
                range.setTop(op1);
                range.setBottom(op1);
            }

            // now set the range
            for (IDataStore ds : dataStores) {
                IDataHandler handler = ds.getExpedition().getHandler(ds);
                handler.setVisibleRange(range);
            }
        }
    }

    /**
     * Open the previous expedition.
     */
    @Override
    public void setVisible(final boolean visible) {
        super.setVisible(visible);
        if (visible && !initialized) {
            Preferences prefs = Preferences.userNodeForPackage(getClass());
            String saved = prefs.get("expedition", null);
            if (saved != null) {
                try {
                    open(new URL(saved));
                } catch (MalformedURLException e) {
                    e.printStackTrace(System.err);
                }
            }
            initialized = true;
        }
    }

    private void updateDataStores() {
        dataStores.getReadWriteLock().writeLock().lock();
        dataStores.clear();
        if (expedition != null) {
            try {
                // set the context for each data handler
                for (IDataStore ds : expedition.getDataStores()) {
                    IDataHandler handler = expedition.getHandler(ds);
                    handler.setContext(context);
                }

                // add all the data stores to the list
                dataStores.addAll(expedition.getDataStores());
            } finally {
                dataStores.getReadWriteLock().writeLock().unlock();
            }
        }
    }

    private void updateHeader() {
        if (expedition == null) {
            header.setTitle("<No expedition open>");
            header
                    .setDescription("Use the 'File -> Open' menu option to open an expedition.");
            header.setIcon(null);
        } else {
            header.setTitle(expedition.getName());
            header.setDescription("");
            if (expedition.getLogo() == null) {
                header.setIcon(null);
            } else {
                header.setIcon(new ImageIcon(expedition.getLogo()));
            }
        }
    }
}
