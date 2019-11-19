package corelyzer.ui;

/**
 * Note: Download from webpage: http://www.devx.com/tips/Tip/5342
 */

import java.awt.Component;

import java.util.Vector;

import javax.swing.JCheckBox;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;


/**
 * A class that extends the JList class so that a listing of selectable items
 * with checkboxes next to them to help indicate that they have been selected.
 */
public class CheckBoxList extends JList<JCheckBox> implements ListSelectionListener {
	protected class CellRenderer implements ListCellRenderer<JCheckBox> {
		public Component getListCellRendererComponent(final JList<? extends JCheckBox> list, final JCheckBox value, final int index, final boolean isSelected,
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

	private static final long serialVersionUID = -4862307481063472246L;

	protected static Border noFocusBorder = new EmptyBorder(1, 1, 1, 1);

	public CheckBoxList() {
		super();
		setCellRenderer(new CellRenderer());
		addListSelectionListener(this);
	}
	
	private JCheckBox getBox(final int index) { return (JCheckBox)getModel().getElementAt(index); }
	
	// When selection changes, update checkbox state. If multiple items are selected,
	// always check and never un-check. When a single item is selected, toggle as normal.
	public void valueChanged(ListSelectionEvent e) {
		if (e.getValueIsAdjusting()) {
			int[] selArray = getSelectedIndices();
			if (selArray.length > 1) {
				for (int idx : selArray) { getBox(idx).setSelected(true); }
			} else if (selArray.length == 1) {
				JCheckBox cb = getBox(selArray[0]);
				cb.setSelected(!cb.isSelected());
				
				// If list only contains a single item and user clicks, it becomes selected
				// and checkbox toggles. But clicking it again won't change the selection,
				// thus this method won't be triggered, thus the checkbox will not toggle as
				// expected. Clear the selection to avoid this situation. 
				this.clearSelection();
			}
			repaint();
		}
	}

	// Returns array containing the indices of checked boxes.
	public int[] getCheckedIndices() {
		Vector<Integer> indices = new Vector<Integer>();

		for (int i = 0; i < getModel().getSize(); i++) {
			if (getBox(i).isSelected()) {
				indices.addElement(i);
			}
		}

		int[] ret = new int[indices.size()];
		for (int k = 0; k < indices.size(); k++) {
			ret[k] = indices.elementAt(k);
		}

		return ret;
	}
}
