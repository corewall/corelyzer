package corelyzer.plugin.iCores.ui;

import corelyzer.util.TableSorter;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Hashtable;
import java.util.Map;

/**
 * Create a new FilterTable.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class FilterTablePanel extends JPanel {
	private static final long serialVersionUID = -2087731573794536011L;
	private final JLabel filterLabel;
	private final JTextField filterField;
	private final JTable table;

    Hashtable<String, ColumnEntry> defaultColumns;

    /**
	 * Create a new filter table.
	 */
	public FilterTablePanel() {
		// set our layout manager
		setLayout(new BorderLayout());
		setBorder(null);
		
		// create our filter field
		JPanel filterPanel = new JPanel();
		filterPanel.setBorder(null);
		filterPanel.setLayout(new GridBagLayout());
		filterLabel = new JLabel("");
        filterLabel.setIcon(new ImageIcon(getClass().getResource(
                "/corelyzer/plugin/iCores/ui/resources/icons/spyglass.jpg")));
        filterLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
		filterLabel.setEnabled(false);
		filterPanel.add(filterLabel, new GridBagConstraints(
                0, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST,
                GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
		
		filterField = new JTextField();
		filterField.setEnabled(false);
		filterPanel.add(filterField, new GridBagConstraints(
                1, 0, 1, 1, 1.0, 0.0,
                GridBagConstraints.WEST,
                GridBagConstraints.HORIZONTAL,
                new Insets(0, 0, 0, 0), 0, 0));
		filterPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
		add(filterPanel, BorderLayout.NORTH);
		
		// create our scroll pane
        table = new JTable();
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setViewportView(table);
        add(scrollPane, BorderLayout.CENTER);

        // Decorate the table
        table.setShowGrid(false);
        table.setDragEnabled(true);
        table.getTableHeader().setReorderingAllowed(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        TableMouseListener listener = new TableMouseListener(table);
        table.addMouseListener(listener);

        // TableHeader PopupMenu
        table.getTableHeader().addMouseListener(new TableHeaderMouseListener());        
    }
	
	/**
	 * Gets the filter field.
	 * 
	 * @return the filter field.
	 */
	public JTextField getFilterField() {
		return filterField;
	}
	
	/**
	 * Set the new table model.
	 * 
	 * @param model the table model.
	 */
	public void setTableModel(TableModel model) {
		// check if out model is null
		if (model == null) {
			filterLabel.setEnabled(false);
			filterField.setText("");
			filterField.setEnabled(false);
		} else {
			filterLabel.setEnabled(true);
			filterField.setEnabled(true);

            // Add table sorter
            TableSorter sorter = new TableSorter(model, table.getTableHeader());
            table.setModel(sorter);

            // Build a copy of default columns
            defaultColumns = new Hashtable<String, ColumnEntry>();
            TableColumnModel tModel = table.getColumnModel();

            for(int i=0; i<sorter.getColumnCount(); i++) {
                String label = (String) tModel.getColumn(i).getHeaderValue();
                ColumnEntry entry = new ColumnEntry(i, tModel.getColumn(i));
                defaultColumns.put(label, entry);
            }

            // Not show 'url' & 'thumbnail' at init
            ColumnEntry entry;
            String [] notShownColumns = 
                    {"URL", "Thumbnail", "DPI", "SyndEntry"};

            for (String aColumn : notShownColumns) {
                entry = defaultColumns.get(aColumn);

                if(entry != null) {
                    table.removeColumn(entry.getColumn());
                    entry.setShown(false);
                }
            }

            TableColumn statusColumn = table.getColumnModel().getColumn(0);
            statusColumn.setCellRenderer(new AnimatedLabelRenderer());
        }
	}

    public JTable getTable() {
        return table;
    }

    private class TableHeaderMouseListener implements MouseListener {

        public TableHeaderMouseListener() {
            super();
        }

        public void mouseClicked(MouseEvent event) {
            if (event.isPopupTrigger()) {
                JPopupMenu menu = new JPopupMenu("Attribs");
                Point p = event.getPoint();

                final JCheckBoxMenuItem colItem =
                        new JCheckBoxMenuItem("Resize Column Width");
                final boolean isAutoResize = (table.getAutoResizeMode() ==
                        JTable.AUTO_RESIZE_ALL_COLUMNS);

                colItem.setSelected(isAutoResize);
                colItem.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            int mode;
                            if(isAutoResize) {
                                mode = JTable.AUTO_RESIZE_OFF;
                            } else {
                                mode = JTable.AUTO_RESIZE_ALL_COLUMNS;
                            }

                            table.setAutoResizeMode(mode);
                            colItem.setSelected(!isAutoResize);
                        }
                    }
                );
                menu.add(colItem);

                menu.addSeparator();

                for(Map.Entry<String, ColumnEntry> entry :
                        defaultColumns.entrySet())
                {
                    String label = entry.getKey();
                    final ColumnEntry column = entry.getValue();
                    final JCheckBoxMenuItem item =
                            new JCheckBoxMenuItem(label, column.isShown());

                    if(label.equalsIgnoreCase("title")) item.setEnabled(false);
                    
                    item.addActionListener(
                        new ActionListener() {
                            public void actionPerformed(ActionEvent e) {
                                column.setShown(item.isSelected());
                                
                                if(item.isSelected()) {
                                    table.addColumn(column.getColumn());
                                    if(column.getOrigIndex() <
                                            table.getColumnCount())
                                    {
                                        table.moveColumn(
                                                table.getColumnCount()-1,
                                                column.getOrigIndex());
                                    }
                                } else {
                                    table.removeColumn(column.getColumn());
                                }
                            }
                        }
                    );
                    menu.add(item);
                }

                menu.show(table.getTableHeader(), p.x, p.y);
            }
        }

        // Not used mouse events
        public void mousePressed(MouseEvent event) {
        }

        public void mouseReleased(MouseEvent event) {
        }

        public void mouseEntered(MouseEvent event) {
        }

        public void mouseExited(MouseEvent event) {
        }
    }

    private class ColumnEntry {
        boolean isShown;
        int origIndex;
        TableColumn column;

        public ColumnEntry() {
            super();
        }

        public ColumnEntry(int index, TableColumn aColumn) {
            origIndex = index;
            column = aColumn;
            isShown = true;
        }

        public int getOrigIndex() {
            return origIndex;
        }

        public void setOrigIndex(int origIndex) {
            this.origIndex = origIndex;
        }

        public TableColumn getColumn() {
            return column;
        }

        public void setColumn(TableColumn column) {
            this.column = column;
        }

        public boolean isShown() {
            return isShown;
        }

        public void setShown(boolean shown) {
            isShown = shown;
        }
    }

    private class AnimatedLabelRenderer extends JLabel
            implements TableCellRenderer {

        public Component getTableCellRendererComponent(JTable jTable,
                                                       Object value,
                                                       boolean isSelected,
                                                       boolean hasFocus,
                                                       int row, int column) {

            int realColumn = jTable.convertColumnIndexToModel(column);

            if(realColumn == 0) {
                JLabel aLabel = (JLabel) value;
                aLabel.setOpaque(isSelected);    

                return aLabel;
            } else {
                System.out.println("---> [AnimatedLabelRenderer] what's this?"
                        + row + ", " + realColumn);
                return (new JLabel(value.toString()));
            }
        }
    }
}
