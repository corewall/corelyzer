package corelyzer.lib.datamodel.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextField;

import org.jdesktop.layout.GroupLayout;
import org.jdesktop.layout.LayoutStyle;

import corelyzer.lib.datamodel.util.ImageFileFilter;

/**
 * A panel that captures the depth pattern property.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class DepthPatternPanel extends CoreImageConfigurationPanel implements ComponentListener {
	private static final long serialVersionUID = -3531199465069954616L;
	private static final String[] PATTERNS = new String[] { "(\\d+\\.\\d+)", "(\\d+-\\d+)", "(\\d+_\\d+)", "_(\\d+_\\d+)" };

	private boolean visible = false;

	// components
	private JLabel depthInstructionsLabel;
	private JTextField filenameText;
	private JLabel patternLabel;
	private JComboBox patternCombo;

	/**
	 * Create a new DepthPatternPanel.
	 */
	public DepthPatternPanel() {
		super();
		addComponentListener(this);
	}

	public void componentHidden(final ComponentEvent e) {
		visible = false;
	}

	public void componentMoved(final ComponentEvent e) {
		// do nothing
	}

	public void componentResized(final ComponentEvent e) {
		// do nothing
	}

	public void componentShown(final ComponentEvent e) {
		visible = true;
		highlightMatch();
	}

	private void highlightMatch() {
		Pattern pattern = Pattern.compile((String) patternCombo.getSelectedItem());
		int start = -1;
		int end = -1;
		if (pattern != null) {
			Matcher m = pattern.matcher(filenameText.getText());
			if (m.find()) {
				start = m.start(1);
				end = m.end(1);
			}
		}

		// update our field
		filenameText.requestFocus();
		filenameText.setSelectionStart(start);
		filenameText.setSelectionEnd(end);
	}

	@Override
	protected void initComponents() {
		// create our instruction label
		depthInstructionsLabel = new JLabel("Select the pattern that highlights the depth in the filename below:");

		// create filename textfield
		filenameText = new JTextField();
		filenameText.setEditable(false);

		// create our pattern label
		patternLabel = new JLabel("Pattern:");

		// create our pattern combobox
		patternCombo = new JComboBox(PATTERNS);
		patternCombo.setEditable(true);
		patternCombo.setEnabled(false);
		patternCombo.addActionListener(new ActionListener() {

			public void actionPerformed(final ActionEvent e) {
				highlightMatch();
			}
		});
	}

	@Override
	protected void layoutComponents() {
		GroupLayout layout = new GroupLayout(this);
		setLayout(layout);
		layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.LEADING).add(
				layout.createSequentialGroup()
						.addContainerGap()
						.add(layout
								.createParallelGroup(GroupLayout.LEADING)
								.add(filenameText, GroupLayout.DEFAULT_SIZE, 359, Short.MAX_VALUE)
								.add(depthInstructionsLabel)
								.add(layout.createSequentialGroup().add(patternLabel).addPreferredGap(LayoutStyle.RELATED)
										.add(patternCombo, GroupLayout.DEFAULT_SIZE, 301, Short.MAX_VALUE))).addContainerGap()));
		layout.setVerticalGroup(layout.createParallelGroup(GroupLayout.LEADING).add(
				layout.createSequentialGroup()
						.addContainerGap()
						.add(depthInstructionsLabel)
						.addPreferredGap(LayoutStyle.RELATED)
						.add(filenameText, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addPreferredGap(LayoutStyle.RELATED)
						.add(layout.createParallelGroup(GroupLayout.BASELINE).add(patternLabel)
								.add(patternCombo, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
						.addContainerGap(54, Short.MAX_VALUE)));
	}

	@Override
	protected void saveComponenets() {
		if (directory == null) {
			return;
		}

		// set our pattern
		directory.getConfig().setPattern((String) patternCombo.getSelectedItem());
	}

	@Override
	protected void updateComponents() {
		// check that our directory is set
		if ((directory == null) || (directory.getDirectory() == null)) {
			return;
		}

		// set our filename text field
		File[] images = directory.getDirectory().listFiles(new ImageFileFilter());
		if ((images == null) || (images.length == 0)) {
			filenameText.setText("< no images found >");
			patternCombo.setEnabled(false);
		} else {
			filenameText.setText(images[0].getName());
			patternCombo.setEnabled(true);

			// select the appropriate pattern
			int sel = -1;
			for (int i = 0; i < PATTERNS.length; i++) {
				if (PATTERNS[i].equals(directory.getConfig().getPattern())) {
					sel = i;
				}
			}
			if (sel >= 0) {
				patternCombo.setSelectedIndex(sel);
			}
		}

		// highlight if we're visible
		if (visible) {
			highlightMatch();
		}
	}
}
