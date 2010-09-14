/** Deprecated */
package corelyzer.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.util.LinkedList;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;

import corelyzer.graphics.SceneGraph;

public class ToolFrame extends JFrame implements ActionListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3198935546015305211L;
	static final String NORMALMODE = "NormalMode";
	static final String MEASUREMODE = "MeasureMode";
	static final String MARKERMODE = "MarkerMode";
	static final String CLASTMODE = "ClastMode";
	static final String APPFRAMEMODE = "AppFrameMode";
	static final String MINIMIZEACTION = "MinAppMode";
	static final String CLOSEACTION = "CloseApplication";
	static final String MEASURESELECT = "MeasureSelec";

	private JToolBar modeBar;
	private ButtonGroup modeGroup;
	private JToggleButton normalBT;
	private JToggleButton measureBT;
	private JToggleButton markerBT;
	private JToggleButton mainappBT;
	private JButton minimizeBT;
	private JButton closeBT;
	private JToggleButton clastBT;

	// measure info panel
	private JComboBox measurefield;
	private final LinkedList<float[]> measureStack;
	// private int measureCount;
	private float[] measurePt;
	private boolean bNewMeasure;
	private JTextArea measureText;
	private JTextArea measureClipBoard;

	// Clast UpperLeft coord and LowerRight corners
	private float[] clastUpperLeft = { 0.0f, 0.0f };
	private float[] clastLowerRight = { 0.0f, 0.0f };

	public ToolFrame() {
		super("ToolFrame");
		this.setUndecorated(true);
		this.setupUI();
		this.setSize(222, 30);

		// Dimension screenDim = Toolkit.getDefaultToolkit().getScreenSize();
		int canvasWidth = CorelyzerApp.getApp().preferences().screenWidth;
		Dimension myDim = this.getSize();
		int myPosX = canvasWidth / 2 - myDim.width / 2;
		this.setLocation(myPosX, 0);

		// measure info
		// this.measureCount = 0;
		this.measureStack = new LinkedList<float[]>();
		this.bNewMeasure = false;

	}

	public void actionPerformed(final ActionEvent e) {
		String cmd = e.getActionCommand();
		if (NORMALMODE.equals(cmd)) {
			if (normalBT.isSelected()) {
				CorelyzerApp.getApp().setMode(0);
				this.setSize(222, 30);
			}
		} else if (MEASUREMODE.equals(cmd)) {
			if (measureBT.isSelected()) {
				CorelyzerApp.getApp().setMode(1);
				this.setSize(222, 160);
			} else {
				this.setSize(222, 30);
			}
		} else if (MARKERMODE.equals(cmd)) {
			if (markerBT.isSelected()) {
				CorelyzerApp.getApp().setMode(2);
				this.setSize(222, 30);
			}
		} else if (CLASTMODE.equals(cmd)) {
			if (clastBT.isSelected()) {
				CorelyzerApp.getApp().setMode(3);
				this.setSize(222, 30);
			}
		} else if (APPFRAMEMODE.equals(cmd)) {
			boolean isVisible;
			String status;

			if (mainappBT.isSelected()) {
				isVisible = true;
				status = "Hide";
			} else {
				isVisible = false;
				status = "Show";
			}

			CorelyzerApp app = CorelyzerApp.getApp();
			JFrame f;
			if (app.isUsePluginUI()) {
				f = app.getPluginFrame();
				app.getMainFrame().setVisible(isVisible);
			} else {
				f = app.getMainFrame();
			}

			f.setVisible(isVisible);
			mainappBT.setToolTipText(status + " CorelyzerApp window");
		} else if (MINIMIZEACTION.equals(cmd)) {
			CorelyzerApp.getApp().getMainFrame().setVisible(true);
			mainappBT.setSelected(true);
			CorelyzerApp.getApp().getMainFrame().setExtendedState(ICONIFIED);
		} else if (CLOSEACTION.equals(cmd)) {
			WindowEvent ee = new WindowEvent(CorelyzerApp.getApp().getMainFrame(), WindowEvent.WINDOW_CLOSING);
			CorelyzerApp.getApp().getMainFrame().dispatchEvent(ee);
		} else if (MEASURESELECT.equals(cmd)) {
			if (bNewMeasure) {
				bNewMeasure = false;
				return;
			}
			// update measure detail text area
			int measureIdx = measurefield.getSelectedIndex();
			float[] mdata = measureStack.get(measureIdx);

			String result = "Point0 ";
			result = result + String.format("(%.2f,%.2f)\n", mdata[0], mdata[1]);
			result = result + "Point1 ";
			result = result + String.format("(%.2f,%.2f)\n", mdata[2], mdata[3]);
			result = result + "Distance: ";
			result = result + String.format("%.2f cm", mdata[4]);
			measureText.setText(result);
			// measureText.selectAll();
			// measureText.copy();
			// measureText.select(0,0);

			// make clipboard content with measure label
			String label = "measure @";
			if (Math.abs(mdata[0]) > 100.0f) {
				float meter = mdata[0] / 100.0f;
				meter = (float) Math.floor(meter);
				float centi = mdata[0] - meter * 100;
				label += String.format(" %.0f m ", meter);
				label += String.format(" %.1f cm\n", centi);
			} else {
				label += " 0 m";
				label += String.format(" %.1f cm\n", mdata[0]);
			}
			measureClipBoard.setText(label + result);
			measureClipBoard.selectAll();
			measureClipBoard.copy();

			// draw this measure data in scenegraph
			SceneGraph.lock();
			SceneGraph.setMeasurePoint(mdata[0], mdata[1], mdata[2], mdata[3]);

			// move scene location to selected measure area
			/*
			 * // the first try of scene relocation // move to the first point
			 * position float canvas_dpix =
			 * corelyzer.helper.SceneGraph.getCanvasDPIX(0); float canvas_dpiy =
			 * corelyzer.helper.SceneGraph.getCanvasDPIY(0); float w_x =
			 * canvas_dpix * mdata[0] / 2.54f; float w_y = canvas_dpiy *
			 * mdata[1] / 2.54f; corelyzer.helper.SceneGraph.positionScene(w_x,
			 * w_y);
			 */
			SceneGraph.positionScene(mdata[5], mdata[6]);

			SceneGraph.unlock();
			CorelyzerApp.getApp().updateGLWindows();
		}
	}

	public void addMeasure(final float coord[], final int nPoint) {
		// check wether first or second point incoming
		if (nPoint == 1) {
			measurePt = new float[7]; // two vertex and one distance
			measurePt[0] = coord[0]; // store first point coord
			measurePt[1] = coord[1];
		} else if (nPoint == 2) {
			// update measure point info
			bNewMeasure = true; // complete measure entry
			measurePt[2] = coord[0]; // store second point coord
			measurePt[3] = coord[1];

			// make label for this measure
			// split meters and centimeters
			String label = "Measure@";
			if (Math.abs(measurePt[0]) > 100.0f) {
				float meter = measurePt[0] / 100.0f;
				meter = (float) Math.floor(meter);
				float centi = measurePt[0] - meter * 100;
				label += String.format(" %.0fm", meter);
				label += String.format(" %.1fcm", centi);
			} else {
				label += "0 m";
				label += String.format(" %.1fcm", measurePt[0]);
			}

			// calc distance
			double dist = (measurePt[2] - measurePt[0]) * (measurePt[2] - measurePt[0]) + (measurePt[3] - measurePt[1]) * (measurePt[3] - measurePt[1]);
			dist = Math.sqrt(dist);
			measurePt[4] = (float) dist; // store distance value

			// scene center
			measurePt[5] = SceneGraph.getSceneCenterX();
			measurePt[6] = SceneGraph.getSceneCenterY();

			// check size of measure history
			if (measureStack.size() == 20) {
				measurefield.removeItemAt(19);
				// measurefield.insertItemAt("measure " + measureCount, 0);
				measurefield.insertItemAt(label, 0);
				measurefield.setSelectedIndex(0);
				measureStack.removeLast();
				measureStack.addFirst(measurePt);
				// measureCount++;
			} else {
				// measurefield.insertItemAt("measure " + measureCount, 0);
				measurefield.insertItemAt(label, 0);
				measurefield.setSelectedIndex(0);
				measureStack.addFirst(measurePt);
				// measureCount++;
			}
			// update text area
			String result = "Point0 ";
			result = result + String.format("(%.2f,%.2f)\n", measurePt[0], measurePt[1]);
			result = result + "Point1 ";
			result = result + String.format("(%.2f,%.2f)\n", measurePt[2], measurePt[3]);
			result = result + "Distance: ";
			result = result + String.format("%.2f cm", measurePt[4]);
			measureText.setText(result);
			// copy to clipboard
			// measureText.selectAll();
			// measureText.copy();
			// measureText.select(0,0);

			measureClipBoard.setText(label + "\n" + result);
			measureClipBoard.selectAll();
			measureClipBoard.copy();

		}
	}

	public float[] getClastLowerRight() {
		return clastLowerRight;
	}

	public float[] getClastUpperLeft() {
		return clastUpperLeft;
	}

	public boolean isAppFrameSelected() {
		return mainappBT.isSelected();
	}

	public void setAppFrameSelected(final boolean value) {
		mainappBT.setSelected(value);
	}

	public void setClastLowerRight(final float[] clastLowerRight) {
		this.clastLowerRight = clastLowerRight;
	}

	public void setClastUpperLeft(final float[] clastUpperLeft) {
		this.clastUpperLeft = clastUpperLeft;
	}

	@Override
	public void setLocation(final int x, int y) {
		boolean MAC_OS_X = System.getProperty("os.name").toLowerCase().startsWith("mac os x");

		if (MAC_OS_X) {
			y += 20;
		}

		super.setLocation(x, y);
		this.repaint();
	}

	public void setMode(final int imode) {
		// this func is called by popup menu in glcanvas
		// update tool mode button selection
		switch (imode) {
			case 0:
				normalBT.setSelected(true);
				this.setSize(222, 30);
				break;
			case 1:
				measureBT.setSelected(true);
				this.setSize(222, 160);
				break;
			case 2:
				markerBT.setSelected(true);
				this.setSize(222, 30);
				break;
			case 3:
				clastBT.setSelected(true);
				this.setSize(222, 30);
				break;
		}

		// set app to new mode
		CorelyzerApp.getApp().setMode(imode);
	}

	public void setupUI() {

		try {
			this.setAlwaysOnTop(true);
		} catch (SecurityException e) {
			System.out.println(e);
			System.out.println("Could not set tool frame to always be front");
		}

		this.getContentPane().setLayout(new BorderLayout());

		// create toolbar
		modeBar = new JToolBar();
		modeBar.setFloatable(false);

		// create buttons...
		normalBT = new JToggleButton(new ImageIcon("resources/icons/normal.gif"));
		normalBT.addActionListener(this);
		normalBT.setToolTipText("Change to Normal Mode");
		normalBT.setActionCommand(NORMALMODE);
		normalBT.setSelected(true);
		modeBar.add(normalBT);

		measureBT = new JToggleButton(new ImageIcon("resources/icons/ruler.gif"));
		measureBT.addActionListener(this);
		measureBT.setToolTipText("Change to Measure Mode");
		measureBT.setActionCommand(MEASUREMODE);
		modeBar.add(measureBT);

		markerBT = new JToggleButton(new ImageIcon("resources/icons/marker.gif"));
		markerBT.addActionListener(this);
		markerBT.setToolTipText("Change to Marker Mode");
		markerBT.setActionCommand(MARKERMODE);
		modeBar.add(markerBT);

		clastBT = new JToggleButton(new ImageIcon("resources/icons/copyright.gif"));
		clastBT.addActionListener(this);
		clastBT.setToolTipText("Change to Clast Mode");
		clastBT.setActionCommand(CLASTMODE);
		modeBar.add(clastBT);

		modeBar.addSeparator();

		mainappBT = new JToggleButton(new ImageIcon("resources/icons/mainframe.gif"));
		mainappBT.addActionListener(this);
		mainappBT.setToolTipText("Hide CorelyzerApp window");
		mainappBT.setActionCommand(APPFRAMEMODE);
		modeBar.add(mainappBT);
		modeBar.addSeparator();

		minimizeBT = new JButton(new ImageIcon("resources/icons/minimize.gif"));
		minimizeBT.addActionListener(this);
		minimizeBT.setToolTipText("Iconify application");
		minimizeBT.setActionCommand(MINIMIZEACTION);
		modeBar.add(minimizeBT);

		closeBT = new JButton(new ImageIcon("resources/icons/close.gif"));
		closeBT.addActionListener(this);
		closeBT.setToolTipText("Close application");
		closeBT.setActionCommand(CLOSEACTION);
		modeBar.add(closeBT);

		modeGroup = new ButtonGroup();
		modeGroup.add(normalBT);
		modeGroup.add(measureBT);
		modeGroup.add(markerBT);
		modeGroup.add(clastBT);

		// tool bar
		this.getContentPane().add(modeBar, BorderLayout.NORTH);

		// add measure info panel
		JPanel p0 = new JPanel(new FlowLayout(FlowLayout.LEFT));
		p0.setBorder(BorderFactory.createTitledBorder("Measure History"));
		measurefield = new JComboBox();
		measurefield.setEditable(false);
		measurefield.addActionListener(this);
		measurefield.setActionCommand(MEASURESELECT);
		p0.add(measurefield);

		JPanel p1 = new JPanel();
		measureText = new JTextArea(3, 16);
		measureText.setEditable(false);
		measureText.setText("");
		measureClipBoard = new JTextArea(4, 30);

		p1.add(measureText);

		// Action a =
		// measureText.getActionMap().get(DefaultEditorKit.copyAction);
		// a.putValue(Action.SMALL_ICON, new
		// ImageIcon("resources/icons/copy.gif"));
		// JButton bt = new JButton(a);
		// bt.setText("");
		// p0.add(bt);
		this.getContentPane().add(p0, BorderLayout.CENTER);

		this.getContentPane().add(p1, BorderLayout.SOUTH);

		// end measure info

		this.pack();
		// this.setVisible(false);
	}
}
