package corelyzer.ui;

/**
 * Note: Download from webpage: http://www.devx.com/tips/Tip/5342
 */

import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Vector;

import javax.swing.JCheckBox;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

/**
 * A class that extends the JList class so that a listing of selectable items
 * with checkboxes next to them to help indicate that they have been selected.
 */
public class CheckBoxList extends JList {
	protected class CellRenderer implements ListCellRenderer {

		public Component getListCellRendererComponent(final JList list, final Object value, final int index, final boolean isSelected,
				final boolean cellHasFocus) {
			JCheckBox checkbox = (JCheckBox) value;
			checkbox.setBackground(isSelected ? getSelectionBackground() : getBackground());
			checkbox.setForeground(isSelected ? getSelectionForeground() : getForeground());
			checkbox.setEnabled(isEnabled());
			checkbox.setFont(getFont());
			checkbox.setFocusPainted(false);
			checkbox.setBorderPainted(true);
			checkbox.setBorder(isSelected ? UIManager.getBorder("List.focusCellHighlightBorder") : noFocusBorder);
			return checkbox;
		}
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -4862307481063472246L;

	protected static Border noFocusBorder = new EmptyBorder(1, 1, 1, 1);

	public CheckBoxList() {
		super();
		setCellRenderer(new CellRenderer());

		addMouseListener(new MouseAdapter() {

			@Override
			public void mousePressed(final MouseEvent e) {
				int index = locationToIndex(e.getPoint());

				if (index != -1) {
					JCheckBox checkbox = (JCheckBox) getModel().getElementAt(index);
					checkbox.setSelected(!checkbox.isSelected());
					repaint();
				}
			}
		});

		setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	}

	/**
	 * Returns the indices of items with checkboxes enabled.
	 */

	@Override
	public int[] getSelectedIndices() {
		Vector<Integer> indicies = new Vector<Integer>();

		for (int i = 0; i < getModel().getSize(); i++) {
			JCheckBox cb = (JCheckBox) getModel().getElementAt(i);
			if (cb.isSelected()) {
				indicies.addElement(i);
			}
		}

		int[] ret = new int[indicies.size()];
		for (int k = 0; k < indicies.size(); k++) {
			ret[k] = indicies.elementAt(k);
		}

		return ret;
	}
}
