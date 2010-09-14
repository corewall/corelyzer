package corelyzer.ui;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SpringLayout;
import javax.swing.WindowConstants;

import corelyzer.data.CRPreferences;

/**
 * This class is used to help manage the display configuration. It draws
 * outlines of the layout and keeps true to the aspect ratio of the screens.
 * 
 * This class will save settings for loading in the future as defaults.
 */
public class DisplayConfig extends WindowAdapter {

	/** This class draw the configuration by extending the Canvas class. */
	class ConfigCanvas extends Canvas {
		/**
		 * 
		 */
		private static final long serialVersionUID = 6783717557196622580L;
		private int numberOfRows = 1;
		private int numberOfColumns = 1;
		private final int seam = 5;

		public ConfigCanvas() {
		}

		public void changeTileConfig(final int _numberOfRows, final int _numberOfColumns) {
			this.numberOfRows = _numberOfRows;
			this.numberOfColumns = _numberOfColumns;
			this.repaint();
		}

		private void drawTileConfig(final Graphics2D g2d) {
			g2d.setColor(new Color(1.0f, 1.0f, 0.0f));

			int strechedtileW = (getWidth() - 2 * seam) / numberOfColumns;
			int strechedtileH = (getHeight() - 2 * seam) / numberOfRows;

			int tileW = Integer.parseInt(width.getValue().toString());
			int tileH = Integer.parseInt(height.getValue().toString());
			float sfac;

			if (tileW > strechedtileW) {
				sfac = (float) strechedtileW / (float) tileW;
				tileH *= sfac;
				tileW *= sfac;
			}

			if (tileH > strechedtileH) {
				sfac = (float) strechedtileH / (float) tileH;
				tileW *= sfac;
				tileH *= sfac;
			}

			for (int i = 0; i < numberOfRows; i++) {
				for (int j = 0; j < numberOfColumns; j++) {
					g2d.drawRect(seam + j * tileW, seam + i * tileH, tileW, tileH);

					/*
					 * g2d.setColor( new Color(0.8f, 0.8f, 0.8f) );
					 * g2d.fillRect(seam+j*tileW+seam, seam+i*tileH+seam,
					 * tileW-seam, tileH-seam);
					 */
				}
			}

		}

		@Override
		public void paint(final Graphics g) {
			Graphics2D g2d = (Graphics2D) g;
			g2d.setColor(new Color(0.5f, 0.5f, 0.5f));
			g2d.fillRect(0, 0, getWidth(), getHeight());

			// draw tile configurations
			this.drawTileConfig(g2d);
		}
	}

	/**
	 * Sends the results to the CorelyzerApp object and saves the settings for
	 * the future.
	 */
	class OkListener implements ActionListener {

		public void actionPerformed(final ActionEvent e) {
			if (app == null) {
				System.out.println("Display OK: Null app pointer!");
				return;
			}

			app.destroyGLWindows();

			app.setNumRows(Integer.parseInt(rows.getValue().toString()));
			app.setNumCols(Integer.parseInt(cols.getValue().toString()));
			app.setTileWidth(Integer.parseInt(width.getValue().toString()));
			app.setTileHeight(Integer.parseInt(height.getValue().toString()));
			app.setScreenDpiX(Float.parseFloat(dpix.getValue().toString()));
			app.setScreenDpiY(Float.parseFloat(dpiy.getValue().toString()));

			app.setBorderLeft(Float.parseFloat(borderLeft.getValue().toString()));
			app.setBorderRight(Float.parseFloat(borderRight.getValue().toString()));
			app.setBorderDown(Float.parseFloat(borderDown.getValue().toString()));
			app.setBorderUp(Float.parseFloat(borderUp.getValue().toString()));

			app.setDisplayOffsets(Integer.parseInt(column_offset.getValue().toString()), Integer.parseInt(row_offset.getValue().toString()));

			saveSettings();

			app.createGLWindows();

			dlg.setVisible(false);
			app.getMainFrame().setVisible(true);
			app.getToolFrame().setVisible(true);

		}
	}

	/**
	 * This class is used in the corelyzer.ui.DisplayConfig class and listens
	 * for property changes in rows, column, etc.
	 */
	class TileConfigListener implements PropertyChangeListener {

		public void propertyChange(final PropertyChangeEvent evt) {
			// call canvas to update its rows and cols values
			int n_row = Integer.parseInt(rows.getValue().toString());
			int n_col = Integer.parseInt(cols.getValue().toString());
			cnvs.changeTileConfig(n_row, n_col);
		}
	}

	CorelyzerApp app;

	ConfigCanvas cnvs;

	JButton okbtn;

	JDialog dlg;

	JFormattedTextField rows;

	JFormattedTextField cols;

	JFormattedTextField width;

	JFormattedTextField height;

	JFormattedTextField borderLeft;

	JFormattedTextField borderRight;

	JFormattedTextField borderDown;
	JFormattedTextField borderUp;
	JFormattedTextField dpix;
	JFormattedTextField dpiy;
	JFormattedTextField column_offset;
	JFormattedTextField row_offset;

	public DisplayConfig() {
		dlg = new JDialog((JFrame) null, "Corelyzer Display Configuration");
		dlg.setSize(400, 675);

		Dimension scrnsize = Toolkit.getDefaultToolkit().getScreenSize();
		int loc_x = scrnsize.width / 2 - dlg.getSize().width / 2;
		int loc_y = scrnsize.height / 2 - dlg.getSize().height / 2;
		dlg.setLocation(loc_x, loc_y);

		SpringLayout layout = new SpringLayout();
		dlg.getContentPane().setLayout(layout);
		dlg.addWindowListener(this);

		app = CorelyzerApp.getApp();

		cnvs = new ConfigCanvas();
		cnvs.setPreferredSize(new Dimension(370, 370));
		dlg.getContentPane().add(cnvs);
		PositionWidget(cnvs, layout, 15, 15);

		// parameters
		DecimalFormat twosig = new DecimalFormat("#.00");
		JLabel label;
		label = new JLabel("Rows ");
		rows = new JFormattedTextField(NumberFormat.getIntegerInstance());
		rows.setValue(new Integer(1));
		rows.setPreferredSize(new Dimension(50, 25));
		rows.addPropertyChangeListener(new TileConfigListener());
		dlg.getContentPane().add(label);
		dlg.getContentPane().add(rows);
		PositionWidget(rows, layout, 15, 400);
		PositionWidget(label, layout, 75, 400);

		label = new JLabel("Columns ");
		cols = new JFormattedTextField(NumberFormat.getIntegerInstance());
		cols.setValue(new Integer(1));
		cols.setPreferredSize(new Dimension(50, 25));
		cols.addPropertyChangeListener(new TileConfigListener());
		dlg.getContentPane().add(cols);
		dlg.getContentPane().add(label);
		PositionWidget(cols, layout, 200, 400);
		PositionWidget(label, layout, 265, 400);

		label = new JLabel("Screen Width");
		width = new JFormattedTextField(NumberFormat.getIntegerInstance());
		width.setValue(new Integer(1024));
		width.setPreferredSize(new Dimension(50, 25));
		width.addPropertyChangeListener(new TileConfigListener());
		dlg.getContentPane().add(width);
		dlg.getContentPane().add(label);
		PositionWidget(width, layout, 15, 440);
		PositionWidget(label, layout, 75, 440);

		label = new JLabel("Screen Height");
		height = new JFormattedTextField(NumberFormat.getIntegerInstance());
		height.setValue(new Integer(768));
		height.setPreferredSize(new Dimension(50, 25));
		height.addPropertyChangeListener(new TileConfigListener());
		dlg.getContentPane().add(height);
		dlg.getContentPane().add(label);
		PositionWidget(height, layout, 200, 440);
		PositionWidget(label, layout, 265, 440);

		label = new JLabel("Screen DPI X");
		dpix = new JFormattedTextField(twosig);
		dpix.setValue(new Float(72.0));
		dpix.setPreferredSize(new Dimension(50, 25));
		dlg.getContentPane().add(dpix);
		dlg.getContentPane().add(label);
		PositionWidget(dpix, layout, 15, 475);
		PositionWidget(label, layout, 75, 475);

		label = new JLabel("Screen DPI Y");
		dpiy = new JFormattedTextField(twosig);
		dpiy.setValue(new Float(72.0));
		dpiy.setPreferredSize(new Dimension(50, 25));
		dlg.getContentPane().add(dpiy);
		dlg.getContentPane().add(label);
		PositionWidget(dpiy, layout, 200, 475);
		PositionWidget(label, layout, 265, 475);

		label = new JLabel("Column Offset");
		this.column_offset = new JFormattedTextField(NumberFormat.getIntegerInstance());
		this.column_offset.setValue(0);
		this.column_offset.setPreferredSize(new Dimension(50, 25));
		dlg.getContentPane().add(this.column_offset);
		dlg.getContentPane().add(label);
		PositionWidget(this.column_offset, layout, 15, 510);
		PositionWidget(label, layout, 75, 510);

		label = new JLabel("Row Offset");
		this.row_offset = new JFormattedTextField(NumberFormat.getIntegerInstance());
		this.row_offset.setValue(0);
		this.row_offset.setPreferredSize(new Dimension(50, 25));
		dlg.getContentPane().add(this.row_offset);
		dlg.getContentPane().add(label);
		PositionWidget(this.row_offset, layout, 200, 510);
		PositionWidget(label, layout, 265, 510);

		label = new JLabel("Border Thickness (inches)");
		dlg.getContentPane().add(label);
		PositionWidget(label, layout, 15, 540);
		label = new JLabel("Top");
		borderUp = new JFormattedTextField(twosig);
		borderUp.setValue(new Float(1.0));
		borderUp.setPreferredSize(new Dimension(40, 25));
		dlg.getContentPane().add(borderUp);
		dlg.getContentPane().add(label);
		PositionWidget(borderUp, layout, 15, 570);
		PositionWidget(label, layout, 60, 570);

		label = new JLabel("Bottom");
		borderDown = new JFormattedTextField(twosig);
		borderDown.setValue(new Float(1.0));
		borderDown.setPreferredSize(new Dimension(40, 25));
		dlg.getContentPane().add(borderDown);
		dlg.getContentPane().add(label);
		PositionWidget(borderDown, layout, 100, 570);
		PositionWidget(label, layout, 145, 570);

		label = new JLabel("Left");
		borderLeft = new JFormattedTextField(twosig);
		borderLeft.setValue(new Float(1.0));
		borderLeft.setPreferredSize(new Dimension(40, 25));
		dlg.getContentPane().add(borderLeft);
		dlg.getContentPane().add(label);
		PositionWidget(borderLeft, layout, 200, 570);
		PositionWidget(label, layout, 245, 570);

		label = new JLabel("Right");
		borderRight = new JFormattedTextField(twosig);
		borderRight.setValue(new Float(1.0));
		borderRight.setPreferredSize(new Dimension(40, 25));
		dlg.getContentPane().add(borderRight);
		dlg.getContentPane().add(label);
		PositionWidget(borderRight, layout, 300, 570);
		PositionWidget(label, layout, 345, 570);

		// load the past settings if there are any
		loadPastSettings();

		// okbtn = new JButton("OK");
		// okbtn.setEnabled(false);
		// okbtn.addActionListener( new OkListener() );
		// dlg.getContentPane().add(okbtn);
		// PositionWidget(okbtn,layout,15,600);
		dlg.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
	}

	public boolean getPreferences(final CRPreferences p) {
		/*
		 * p.numberOfRows = Integer.parseInt(rows.getValue().toString());
		 * p.numberOfColumns = Integer.parseInt(cols.getValue().toString());
		 * p.screenWidth = Integer.parseInt(width.getValue().toString());
		 * p.screenHeight = Integer.parseInt(height.getValue().toString());
		 * 
		 * p.borderLeft = Float.parseFloat(borderLeft.getValue().toString());
		 * p.borderRight = Float.parseFloat(borderRight.getValue().toString());
		 * p.borderDown = Float.parseFloat(borderDown.getValue().toString());
		 * p.borderUp = Float.parseFloat(borderUp.getValue().toString());
		 * 
		 * p.dpix = Float.parseFloat(dpix.getValue().toString()); p.dpiy =
		 * Float.parseFloat(dpiy.getValue().toString());
		 */

		// if there is change, then return true otherwise false
		boolean resultchanged = false;
		int newrows = Integer.parseInt(rows.getValue().toString());
		int newcols = Integer.parseInt(cols.getValue().toString());
		int newwidth = Integer.parseInt(width.getValue().toString());
		int newheight = Integer.parseInt(height.getValue().toString());
		float newborderLeft = Float.parseFloat(borderLeft.getValue().toString());
		float newborderRight = Float.parseFloat(borderRight.getValue().toString());
		float newborderDown = Float.parseFloat(borderDown.getValue().toString());
		float newborderUp = Float.parseFloat(borderUp.getValue().toString());
		float newdpix = Float.parseFloat(dpix.getValue().toString());
		float newdpiy = Float.parseFloat(dpiy.getValue().toString());
		int newColumnOffset = Integer.parseInt(this.column_offset.getValue().toString());
		int newRowOffset = Integer.parseInt(this.row_offset.getValue().toString());

		// check configuration changed or not
		if (newrows != p.numberOfRows) {
			resultchanged = true;
		}
		p.numberOfRows = newrows;
		if (newcols != p.numberOfColumns) {
			resultchanged = true;
		}
		p.numberOfColumns = newcols;
		if (newwidth != p.screenWidth) {
			resultchanged = true;
		}
		p.screenWidth = newwidth;
		if (newheight != p.screenHeight) {
			resultchanged = true;
		}
		p.screenHeight = newheight;
		if (newborderLeft != p.borderLeft) {
			resultchanged = true;
		}
		p.borderLeft = newborderLeft;
		if (newborderRight != p.borderRight) {
			resultchanged = true;
		}
		p.borderRight = newborderRight;
		if (newborderDown != p.borderDown) {
			resultchanged = true;
		}
		p.borderDown = newborderDown;
		if (newborderUp != p.borderUp) {
			resultchanged = true;
		}
		p.borderUp = newborderUp;
		if (newdpix != p.dpix) {
			resultchanged = true;
		}
		p.dpix = newdpix;
		if (newdpiy != p.dpiy) {
			resultchanged = true;
		}
		p.dpiy = newdpiy;

		if (newColumnOffset != p.column_offset) {
			resultchanged = true;
		}
		p.column_offset = newColumnOffset;
		if (newRowOffset != p.row_offset) {
			resultchanged = true;
		}
		p.row_offset = newRowOffset;

		return resultchanged;
	}

	void loadPastSettings() {
		try {
			File prevsettings = new File("display.conf");
			if (!prevsettings.exists()) {
				return;
			}

			FileReader fr = new FileReader(prevsettings);
			BufferedReader br = new BufferedReader(fr);
			String line;
			// data is
			// width
			// height
			// rows
			// cols
			// dpi_x
			// dpi_y
			// top border
			// down border
			// left border
			// right border
			line = br.readLine();
			width.setValue(new Integer(line));

			line = br.readLine();
			height.setValue(new Integer(line));

			line = br.readLine();
			rows.setValue(new Integer(line));

			line = br.readLine();
			cols.setValue(new Integer(line));

			line = br.readLine();
			dpix.setValue(new Float(line));

			line = br.readLine();
			dpiy.setValue(new Float(line));

			line = br.readLine();
			borderUp.setValue(new Float(line));

			line = br.readLine();
			borderDown.setValue(new Float(line));

			line = br.readLine();
			borderLeft.setValue(new Float(line));

			line = br.readLine();
			borderRight.setValue(new Float(line));

			try {
				line = br.readLine();
				this.column_offset.setValue(new Integer(line));

				line = br.readLine();
				this.row_offset.setValue(new Integer(line));
			} catch (NumberFormatException e) {
				this.column_offset.setValue(0);
				this.row_offset.setValue(0);
			}

			br.close();
			fr.close();

		} catch (Exception e) {
			System.out.println("ERROR Reading previous settings");
			e.printStackTrace();
		}
	}

	private void PositionWidget(final Component c, final SpringLayout l, final int x, final int y) {
		l.putConstraint(SpringLayout.WEST, c, x, SpringLayout.WEST, dlg.getContentPane());
		l.putConstraint(SpringLayout.NORTH, c, y, SpringLayout.NORTH, dlg.getContentPane());
	}

	public void run() {
		dlg.setVisible(true);
	}

	void saveSettings() {
		try {
			File f = new File("display.conf");
			FileWriter fw = new FileWriter(f);
			String line;
			line = width.getValue().toString() + "\n";
			fw.write(line, 0, line.length());
			line = height.getValue().toString() + "\n";
			fw.write(line, 0, line.length());
			line = rows.getValue().toString() + "\n";
			fw.write(line, 0, line.length());
			line = cols.getValue().toString() + "\n";
			fw.write(line, 0, line.length());
			line = dpix.getValue().toString() + "\n";
			fw.write(line, 0, line.length());
			line = dpiy.getValue().toString() + "\n";
			fw.write(line, 0, line.length());
			line = borderUp.getValue().toString() + "\n";
			fw.write(line, 0, line.length());
			line = borderDown.getValue().toString() + "\n";
			fw.write(line, 0, line.length());
			line = borderLeft.getValue().toString() + "\n";
			fw.write(line, 0, line.length());
			line = borderRight.getValue().toString() + "\n";
			fw.write(line, 0, line.length());
			line = this.column_offset.getValue().toString() + "\n";
			fw.write(line, 0, line.length());
			line = this.row_offset.getValue().toString() + "\n";
			fw.write(line, 0, line.length());

			fw.close();
		} catch (Exception e) {
			System.out.println("ERROR Saving Display settings");
			e.printStackTrace();
		}
	}

	public void setPreferences(final CRPreferences p) {
		this.rows.setValue(p.numberOfRows);
		this.cols.setValue(p.numberOfColumns);
		this.width.setValue(p.screenWidth);
		this.height.setValue(p.screenHeight);

		this.borderLeft.setValue(p.borderLeft);
		this.borderRight.setValue(p.borderRight);
		this.borderDown.setValue(p.borderDown);
		this.borderUp.setValue(p.borderUp);

		this.dpix.setValue(p.dpix);
		this.dpiy.setValue(p.dpiy);

		this.column_offset.setValue(p.column_offset);
		this.row_offset.setValue(p.row_offset);
	}

	/**
	 * Sends the results to the CorelyzerApp object and saves the settings for
	 * the future.
	 */

	@Override
	public void windowClosing(final WindowEvent e) {

		if (app == null) {
			System.out.println("Display closing: Null app pointer!");
			return;
		}

		app.destroyGLWindows();

		app.setNumRows(Integer.parseInt(rows.getValue().toString()));
		app.setNumCols(Integer.parseInt(cols.getValue().toString()));
		app.setTileWidth(Integer.parseInt(width.getValue().toString()));
		app.setTileHeight(Integer.parseInt(height.getValue().toString()));
		app.setScreenDpiX(Float.parseFloat(dpix.getValue().toString()));
		app.setScreenDpiY(Float.parseFloat(dpiy.getValue().toString()));

		app.setBorderLeft(Float.parseFloat(borderLeft.getValue().toString()));
		app.setBorderRight(Float.parseFloat(borderRight.getValue().toString()));
		app.setBorderDown(Float.parseFloat(borderDown.getValue().toString()));
		app.setBorderUp(Float.parseFloat(borderUp.getValue().toString()));

		app.setDisplayOffsets(Integer.parseInt(this.column_offset.getValue().toString()), Integer.parseInt(this.row_offset.getValue().toString()));

		saveSettings();

		app.createGLWindows();
		app.getMainFrame().setVisible(true);
		app.getToolFrame().setVisible(true);

	}
}
