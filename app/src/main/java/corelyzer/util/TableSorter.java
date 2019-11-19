package corelyzer.util;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

/**
 * TableSorter is a decorator for TableModels; adding sorting functionality to a
 * supplied TableModel. corelyzer.util.TableSorter does not store or copy the
 * data in its TableModel; instead it maintains a map from the row indexes of
 * the view to the row indexes of the model. As requests are made of the sorter
 * (like getValueAt(row, col)) they are passed to the underlying model after the
 * row numbers have been translated via the internal mapping array. This way,
 * the corelyzer.util.TableSorter appears to hold another copy of the table with
 * the rows in a different order.
 * <p/>
 * TableSorter registers itself as a listener to the underlying model, just as
 * the JTable itself would. Events recieved from the model are examined,
 * sometimes manipulated (typically widened), and then passed on to the
 * corelyzer.util.TableSorter's listeners (typically the JTable). If a change to
 * the model has invalidated the order of corelyzer.util.TableSorter's rows, a
 * note of this is made and the sorter will resort the rows the next time a
 * value is requested.
 * <p/>
 * When the tableHeader property is set, either by using the setTableHeader()
 * method or the two argument constructor, the table header may be used as a
 * complete UI for corelyzer.util.TableSorter. The default renderer of the
 * tableHeader is decorated with a renderer that indicates the sorting status of
 * each column. In addition, a mouse listener is installed with the following
 * behavior:
 * <ul>
 * <li>
 * Mouse-click: Clears the sorting status of all other columns and advances the
 * sorting status of that column through three values: {NOT_SORTED, ASCENDING,
 * DESCENDING} (then back to NOT_SORTED again).
 * <li>
 * SHIFT-mouse-click: Clears the sorting status of all other columns and cycles
 * the sorting status of the column through the same three values, in the
 * opposite order: {NOT_SORTED, DESCENDING, ASCENDING}.
 * <li>
 * CONTROL-mouse-click and CONTROL-SHIFT-mouse-click: as above except that the
 * changes to the column do not cancel the statuses of columns that are already
 * sorting - giving a way to initiate a compound sort.
 * </ul>
 * <p/>
 * This is a long overdue rewrite of a class of the same name that first
 * appeared in the swing table demos in 1997.
 * 
 * @author Philip Milne
 * @author Brendon McLean
 * @author Dan van Enckevort
 * @author Parwinder Sekhon
 * @version 2.0 02/27/04
 */

public class TableSorter extends AbstractTableModel {
	private static class Arrow implements Icon {
		private final boolean descending;
		private final int size;
		private final int priority;

		public Arrow(final boolean descending, final int size, final int priority) {
			this.descending = descending;
			this.size = size;
			this.priority = priority;
		}

		public int getIconHeight() {
			return size;
		}

		public int getIconWidth() {
			return size;
		}

		public void paintIcon(final Component c, final Graphics g, final int x, int y) {
			Color color = c == null ? Color.GRAY : c.getBackground();
			// In a compound sort, make each succesive triangle 20%
			// smaller than the previous one.
			int dx = (int) (size / 2 * Math.pow(0.8, priority));
			int dy = descending ? dx : -dx;
			// Align icon (roughly) with font baseline.
			y = y + 5 * size / 6 + (descending ? -dy : 0);
			int shift = descending ? 1 : -1;
			g.translate(x, y);

			// Right diagonal.
			g.setColor(color.darker());
			g.drawLine(dx / 2, dy, 0, 0);
			g.drawLine(dx / 2, dy + shift, 0, shift);

			// Left diagonal.
			g.setColor(color.brighter());
			g.drawLine(dx / 2, dy, dx, 0);
			g.drawLine(dx / 2, dy + shift, dx, shift);

			// Horizontal line.
			if (descending) {
				g.setColor(color.darker().darker());
			} else {
				g.setColor(color.brighter().brighter());
			}
			g.drawLine(dx, 0, 0, 0);

			g.setColor(color);
			g.translate(-x, -y);
		}
	}

	private static class Directive {
		private final int column;
		private final int direction;

		public Directive(final int column, final int direction) {
			this.column = column;
			this.direction = direction;
		}
	}

	private class MouseHandler extends MouseAdapter {

		@Override
		public void mouseClicked(final MouseEvent e) {
			if (e.getButton() == MouseEvent.BUTTON1) {
				JTableHeader h = (JTableHeader) e.getSource();
				TableColumnModel columnModel = h.getColumnModel();
				int viewColumn = columnModel.getColumnIndexAtX(e.getX());
				int column = columnModel.getColumn(viewColumn).getModelIndex();
				if (column != -1) {
					int status = getSortingStatus(column);
					if (!e.isControlDown()) {
						cancelSorting();
					}
					// Cycle the sorting states through {NOT_SORTED, ASCENDING,
					// DESCENDING} or
					// {NOT_SORTED, DESCENDING, ASCENDING} depending on whether
					// shift is pressed.
					status = status + (e.isShiftDown() ? -1 : 1);
					status = (status + 4) % 3 - 1; // signed mod, returning {-1,
													// 0, 1}
					setSortingStatus(column, status);
				}
			}
		}
	}

	private class Row implements Comparable<Object> {
		private final int modelIndex;

		public Row(final int index) {
			this.modelIndex = index;
		}

		public int compareTo(final Object o) {
			int row1 = modelIndex;
			int row2 = ((Row) o).modelIndex;

			for (Iterator<Directive> it = (Iterator<Directive>)sortingColumns.iterator(); it.hasNext();) {
				Directive directive = (Directive) it.next();
				int column = directive.column;
				Object o1 = tableModel.getValueAt(row1, column);
				Object o2 = tableModel.getValueAt(row2, column);

				int comparison = 0;
				// Define null less than everything, except null.
				if (o1 == null && o2 == null) {
					comparison = 0;
				} else if (o1 == null) {
					comparison = -1;
				} else if (o2 == null) {
					comparison = 1;
				} else {
					comparison = getComparator(column).compare(o1, o2);
				}
				if (comparison != 0) {
					return directive.direction == DESCENDING ? -comparison : comparison;
				}
			}
			return 0;
		}
	}

	private class SortableHeaderRenderer implements TableCellRenderer {
		private final TableCellRenderer tableCellRenderer;

		public SortableHeaderRenderer(final TableCellRenderer tableCellRenderer) {
			this.tableCellRenderer = tableCellRenderer;
		}

		public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected, final boolean hasFocus, final int row,
				final int column) {
			Component c = tableCellRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			if (c instanceof JLabel) {
				JLabel l = (JLabel) c;
				l.setHorizontalTextPosition(SwingConstants.LEFT);
				int modelColumn = table.convertColumnIndexToModel(column);
				l.setIcon(getHeaderRendererIcon(modelColumn, l.getFont().getSize()));
			}
			return c;
		}
	}

	private class TableModelHandler implements TableModelListener {

		public void tableChanged(final TableModelEvent e) {
			// If we're not sorting by anything, just pass the event along.
			if (!isSorting()) {
				clearSortingState();
				fireTableChanged(e);
				return;
			}

			// If the table structure has changed, cancel the sorting; the
			// sorting columns may have been either moved or deleted from
			// the model.
			if (e.getFirstRow() == TableModelEvent.HEADER_ROW) {
				cancelSorting();
				fireTableChanged(e);
				return;
			}

			// We can map a cell event through to the view without widening
			// when the following conditions apply:
			//
			// a) all the changes are on one row (e.getFirstRow() ==
			// e.getLastRow()) and,
			// b) all the changes are in one column (column !=
			// TableModelEvent.ALL_COLUMNS) and,
			// c) we are not sorting on that column (getSortingStatus(column) ==
			// NOT_SORTED) and,
			// d) a reverse lookup will not trigger a sort (modelToView != null)
			//
			// Note: INSERT and DELETE events fail this test as they have column
			// == ALL_COLUMNS.
			//
			// The last check, for (modelToView != null) is to see if
			// modelToView
			// is already allocated. If we don't do this check; sorting can
			// become
			// a performance bottleneck for applications where cells
			// change rapidly in different parts of the table. If cells
			// change alternately in the sorting column and then outside of
			// it this class can end up re-sorting on alternate cell updates -
			// which can be a performance problem for large tables. The last
			// clause avoids this problem.
			int column = e.getColumn();
			if (e.getFirstRow() == e.getLastRow() && column != TableModelEvent.ALL_COLUMNS && getSortingStatus(column) == NOT_SORTED && modelToView != null) {
				int viewIndex = getModelToView()[e.getFirstRow()];
				fireTableChanged(new TableModelEvent(TableSorter.this, viewIndex, viewIndex, column, e.getType()));
				return;
			}

			// Something has happened to the data that may have invalidated the
			// row order.
			clearSortingState();
			fireTableDataChanged();
			return;
		}
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -6665112629581216843L;
	protected TableModel tableModel;

	public static final int DESCENDING = -1;
	public static final int NOT_SORTED = 0;

	public static final int ASCENDING = 1;
	private static Directive EMPTY_DIRECTIVE = new Directive(-1, NOT_SORTED);
	public static final Comparator<Object> COMPARABLE_COMAPRATOR = new Comparator<Object>() {

		public int compare(final Object o1, final Object o2) {
			return ((Comparable<Object>) o1).compareTo(o2);
		}
	};
	public static final Comparator<Object> LEXICAL_COMPARATOR = new Comparator<Object>() {

		public int compare(final Object o1, final Object o2) {
			return o1.toString().compareTo(o2.toString());
		}
	};
	private Row[] viewToModel;

	private int[] modelToView;

	private JTableHeader tableHeader;

	private final MouseListener mouseListener;

	private final TableModelListener tableModelListener;

	private final Map<Class<?>, Comparator<Object>> columnComparators = new HashMap<Class<?>, Comparator<Object>>();

	private final List<Directive> sortingColumns = new ArrayList<Directive>();

	public TableSorter() {
		this.mouseListener = new MouseHandler();
		this.tableModelListener = new TableModelHandler();
	}

	public TableSorter(final TableModel tableModel) {
		this();
		setTableModel(tableModel);
	}

	public TableSorter(final TableModel tableModel, final JTableHeader tableHeader) {
		this();
		setTableHeader(tableHeader);
		setTableModel(tableModel);
	}

	private void cancelSorting() {
		sortingColumns.clear();
		sortingStatusChanged();
	}

	private void clearSortingState() {
		viewToModel = null;
		modelToView = null;
	}

	@Override
	public Class<?> getColumnClass(final int column) {
		return tableModel.getColumnClass(column);
	}

	public int getColumnCount() {
		return tableModel == null ? 0 : tableModel.getColumnCount();
	}

	@Override
	public String getColumnName(final int column) {
		return tableModel.getColumnName(column);
	}

	protected Comparator<Object> getComparator(final int column) {
		Class<?> columnType = tableModel.getColumnClass(column);
		Comparator<Object> comparator = (Comparator<Object>) columnComparators.get(columnType);
		if (comparator != null) {
			return comparator;
		}
		if (Comparable.class.isAssignableFrom(columnType)) {
			return COMPARABLE_COMAPRATOR;
		}
		return LEXICAL_COMPARATOR;
	}

	private Directive getDirective(final int column) {
		for (int i = 0; i < sortingColumns.size(); i++) {
			Directive directive = (Directive) sortingColumns.get(i);
			if (directive.column == column) {
				return directive;
			}
		}
		return EMPTY_DIRECTIVE;
	}

	protected Icon getHeaderRendererIcon(final int column, final int size) {
		Directive directive = getDirective(column);
		if (directive == EMPTY_DIRECTIVE) {
			return null;
		}
		return new Arrow(directive.direction == DESCENDING, size, sortingColumns.indexOf(directive));
	}

	private int[] getModelToView() {
		if (modelToView == null) {
			int n = getViewToModel().length;
			modelToView = new int[n];
			for (int i = 0; i < n; i++) {
				modelToView[modelIndex(i)] = i;
			}
		}
		return modelToView;
	}

	public int getRowCount() {
		return tableModel == null ? 0 : tableModel.getRowCount();
	}

	public int getSortingStatus(final int column) {
		return getDirective(column).direction;
	}

	// TableModel interface methods

	public JTableHeader getTableHeader() {
		return tableHeader;
	}

	public TableModel getTableModel() {
		return tableModel;
	}

	public Object getValueAt(final int row, final int column) {
		return tableModel.getValueAt(modelIndex(row), column);
	}

	private Row[] getViewToModel() {
		if (viewToModel == null) {
			int tableModelRowCount = tableModel.getRowCount();
			viewToModel = new Row[tableModelRowCount];
			for (int row = 0; row < tableModelRowCount; row++) {
				viewToModel[row] = new Row(row);
			}

			if (isSorting()) {
				Arrays.sort(viewToModel);
			}
		}
		return viewToModel;
	}

	@Override
	public boolean isCellEditable(final int row, final int column) {
		return tableModel.isCellEditable(modelIndex(row), column);
	}

	public boolean isSorting() {
		return sortingColumns.size() != 0;
	}

	public int modelIndex(final int viewIndex) {
		return getViewToModel()[viewIndex].modelIndex;
	}

	public void reSortByColumn(final int col) {
		if (col != -1) {
			int status = 0;
			cancelSorting();
			setSortingStatus(col, status);
		}
	}

	// Helper classes

	public void setColumnComparator(final Class<?> type, final Comparator<Object> comparator) {
		if (comparator == null) {
			columnComparators.remove(type);
		} else {
			columnComparators.put(type, comparator);
		}
	}

	public void setSortingStatus(final int column, final int status) {
		Directive directive = getDirective(column);
		if (directive != EMPTY_DIRECTIVE) {
			sortingColumns.remove(directive);
		}
		if (status != NOT_SORTED) {
			sortingColumns.add(new Directive(column, status));
		}
		sortingStatusChanged();
	}

	public void setTableHeader(final JTableHeader tableHeader) {
		if (this.tableHeader != null) {
			this.tableHeader.removeMouseListener(mouseListener);
			TableCellRenderer defaultRenderer = this.tableHeader.getDefaultRenderer();
			if (defaultRenderer instanceof SortableHeaderRenderer) {
				this.tableHeader.setDefaultRenderer(((SortableHeaderRenderer) defaultRenderer).tableCellRenderer);
			}
		}
		this.tableHeader = tableHeader;
		if (this.tableHeader != null) {
			this.tableHeader.addMouseListener(mouseListener);
			this.tableHeader.setDefaultRenderer(new SortableHeaderRenderer(this.tableHeader.getDefaultRenderer()));
		}
	}

	public void setTableModel(final TableModel tableModel) {
		if (this.tableModel != null) {
			this.tableModel.removeTableModelListener(tableModelListener);
		}

		this.tableModel = tableModel;
		if (this.tableModel != null) {
			this.tableModel.addTableModelListener(tableModelListener);
		}

		clearSortingState();
		fireTableStructureChanged();
	}

	@Override
	public void setValueAt(final Object aValue, final int row, final int column) {
		tableModel.setValueAt(aValue, modelIndex(row), column);
	}

	private void sortingStatusChanged() {
		clearSortingState();
		fireTableDataChanged();
		if (tableHeader != null) {
			tableHeader.repaint();
		}
	}
}
