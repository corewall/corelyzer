package corelyzer.lib.datamodel.ui;

import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import org.jdesktop.layout.GroupLayout;
import org.jdesktop.layout.LayoutStyle;

import corelyzer.lib.datamodel.CoreImageConfiguration;
import corelyzer.lib.datamodel.Image;

/**
 * A panel to capture image properties.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class ImagePropertiesPanel extends CoreImageConfigurationPanel {
	private static final long serialVersionUID = 2225201494174741023L;

	// components
	private JLabel dpixAltLabel;
	private JLabel dpixLabel;
	private JTextField dpixText;
	private JLabel dpiyAltLabel;
	private JLabel dpiyLabel;
	private JTextField dpiyText;
	private JRadioButton horizRadio;
	private ButtonGroup orientationGroup;
	private JLabel orientationLabel;
	private JRadioButton vertRadio;

	@Override
	protected void initComponents() {
		orientationGroup = new ButtonGroup();
		orientationLabel = new JLabel();
		horizRadio = new JRadioButton();
		vertRadio = new JRadioButton();
		dpixLabel = new JLabel();
		dpixText = new JTextField();
		dpixAltLabel = new JLabel();
		dpiyLabel = new JLabel();
		dpiyText = new JTextField();
		dpiyAltLabel = new JLabel();

		orientationLabel.setText("Orientation:");

		orientationGroup.add(horizRadio);
		horizRadio.setSelected(true);
		horizRadio.setText("Horizontal");
		horizRadio.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
		horizRadio.setMargin(new Insets(0, 0, 0, 0));

		orientationGroup.add(vertRadio);
		vertRadio.setText("Vertical");
		vertRadio.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
		vertRadio.setMargin(new Insets(0, 0, 0, 0));

		dpixLabel.setText("DPI X:");

		dpixAltLabel.setText("(if known)");

		dpiyLabel.setText("DPI Y:");

		dpiyAltLabel.setText("(if known)");
	}

	@Override
	protected void layoutComponents() {
		GroupLayout layout = new GroupLayout(this);
		setLayout(layout);
		layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.LEADING).add(
				layout.createSequentialGroup()
						.addContainerGap()
						.add(layout.createParallelGroup(GroupLayout.TRAILING).add(dpiyLabel).add(dpixLabel).add(orientationLabel))
						.add(15, 15, 15)
						.add(layout.createParallelGroup(GroupLayout.LEADING, false).add(dpiyText).add(dpixText)
								.add(horizRadio, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)).add(24, 24, 24)
						.add(layout.createParallelGroup(GroupLayout.LEADING).add(vertRadio).add(dpixAltLabel).add(dpiyAltLabel))
						.addContainerGap(108, Short.MAX_VALUE)));
		layout.setVerticalGroup(layout.createParallelGroup(GroupLayout.LEADING).add(
				layout.createSequentialGroup()
						.addContainerGap()
						.add(layout.createParallelGroup(GroupLayout.BASELINE).add(orientationLabel).add(horizRadio).add(vertRadio))
						.addPreferredGap(LayoutStyle.RELATED)
						.add(layout.createParallelGroup(GroupLayout.BASELINE).add(dpixLabel)
								.add(dpixText, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE).add(dpixAltLabel))
						.addPreferredGap(LayoutStyle.RELATED)
						.add(layout.createParallelGroup(GroupLayout.BASELINE).add(dpiyLabel)
								.add(dpiyText, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE).add(dpiyAltLabel))
						.addContainerGap(54, Short.MAX_VALUE)));
	}

	@Override
	protected void saveComponenets() {
		if (directory == null) {
			return;
		}

		// set our DPI X
		CoreImageConfiguration config = directory.getConfig();
		try {
			config.setDPIX(Double.parseDouble(dpixText.getText()));
		} catch (Exception e) {
			// do nothing
		}

		// set our DPI Y
		try {
			config.setDPIY(Double.parseDouble(dpiyText.getText()));
		} catch (Exception e) {
			// do nothing
		}

		// set our orientation
		if (horizRadio.isSelected()) {
			config.setOrientation(Image.HORIZONTAL);
		} else if (vertRadio.isSelected()) {
			config.setOrientation(Image.VERTICAL);
		}
	}

	@Override
	protected void updateComponents() {
		if (directory == null) {
			return;
		}

		// update fields from our config
		CoreImageConfiguration config = directory.getConfig();
		horizRadio.setSelected(config.getOrientation().equals(Image.HORIZONTAL));
		vertRadio.setSelected(config.getOrientation().equals(Image.VERTICAL));
		if (config.getDPIX() > 0) {
			dpixText.setText("" + config.getDPIX());
		} else {
			dpixText.setText("");
		}
		if (config.getDPIY() > 0) {
			dpiyText.setText("" + config.getDPIY());
		} else {
			dpiyText.setText("");
		}
	}
}
