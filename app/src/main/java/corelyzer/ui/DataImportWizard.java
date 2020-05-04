/******************************************************************************
 *
 * CoreWall / Corelyzer - An Initial Core Description Tool
 * Copyright (C) 2004, 2005 Arun Gangadhar Gudur Rao, Julian Yu-Chung Chen
 * Electronic Visualization Laboratory, University of Illinois at Chicago
 *
 * This software is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either Version 2.1 of the License, or 
 * (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public 
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser Public License along
 * with this software; if not, write to the Free Software Foundation, Inc., 
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 * Questions or comments about CoreWall should be directed to 
 * cavern@evl.uic.edu
 *
 *****************************************************************************/

package corelyzer.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;

import java.util.List;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ProgressMonitor;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.SwingWorker;

import net.miginfocom.swing.MigLayout;

import com.opencsv.exceptions.CsvException;

import corelyzer.helper.ExampleFileFilter;
import corelyzer.util.FileUtility;
import corelyzer.data.DepthMode;
import corelyzer.data.TabularToXMLConversion;
import corelyzer.data.tabular.OpenCSVParser;


// A text data import wizard allowing users to specify delimiter,
// start/end row, fields and units rows, section name columns, etc
// to properly convert tabular data to Corelyzer's XML data format.

public class DataImportWizard extends JDialog implements ActionListener, ChangeListener, PropertyChangeListener {
	public static enum FieldSeparator {
		COMMA, TAB, SPACE, SEMICOLON;
		
		public String toString() {
			if (this.ordinal() == 0) {
				return "Comma";
			} else if (this.ordinal() == 1) {
				return "Tab";
			} else if (this.ordinal() == 2) {
				return "Space";
			} else { // (this.ordinal() == 3) {
				return "Semicolon";
			}
		}
	}

	public static enum RunMode {
		CORELYZER, STANDALONE
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 2750206706267241630L;
	private static File lastInputFileDirectory = null;


	public static void main(final String[] args) {
		DataImportWizard wiz = new DataImportWizard(null);
		wiz.setRunningMode(RunMode.STANDALONE);

		final String inputFile = "/Users/lcdev/proj/corewall/corewall_data/Corelyzer/318-U1357/data/318-U1357-GRA-AB_secnames_sorted.csv";
		// final String inputFile = "/Users/lcdev/Desktop/import data testing/MEXI_XYZ.csv";
		if (args.length < 1) {
			wiz.setInputFile(inputFile);
			// System.out.println("Usage: java corelyzer.ui.DataImportWizard <input>.");
			// System.exit(0);
		} else if (args[0] != null) {
			System.out.println("arg " + 0 + ": " + args[0]);
			wiz.setInputFile(args[0]);
		}

		wiz.setVisible(true);
	}

	// Data as parsed by OpenCSV
	List<String[]> parsedData = null;

	// Running mode information
	RunMode mode = RunMode.CORELYZER;

	// stage information - brg 7/31/2015 "stages" are tabs, vars used for Back/Next button handling
	int number_of_stages = 2;
	int current_stage = 0;

	JButton nextBtn, backBtn, cancelBtn;
	JTabbedPane stageTab;
	JTable fileContent;
	/**
	 * Parse information Parse file 'inputFile', using specified delimiter. Data
	 * starts from 'start_row' to 'end_row'. Use 'label_row' as the labels for
	 * columns, 'unit_row' for units. 'sectionname_column' will be used as name
	 * of the sections, and 'sectiondepth_column' will be used as the depth
	 * value of the core section. Vector 'dataColumns' will be checked columns
	 * to get data values.
	 */
	JLabel fileLabel, sectionNamePreview, unitRowLabel;
	JTextField start_number, end_number, ignore_values;
	JTextField label_number, unit_number;
	JTextField name_column, depth_column;
	JCheckBox unitRowCheckbox;
	final String FieldsRowLabel = "Fields Row", DataStartRowLabel = "Data Start Row",
		DataEndRowLabel = "Data End Row", DepthColLabel = "Depth Column";
	
	JComboBox<FieldSeparator> fsComboBox;
	boolean suspendFieldSeparatorListener = false;
	JComboBox<DepthMode> depthModeComboBox;

	CheckBoxList columnList;

	Vector<DataColumnCheckBox> columnListModel;
	SwingWorker<Boolean,Void> convertTask;
	ProgressMonitor progressMonitor;

	File inputFile, outputFile;

	// --------------------------------------------------------------------------

	public DataImportWizard(final Frame f) {
		super(f);
		setupUI();

		this.setLocationRelativeTo(f);
	}

	public DataImportWizard(final Frame view, final File f) {
		this(view);
		setInputFile(f);
	}

	public void actionPerformed(final ActionEvent e) {
		JButton b = (JButton) e.getSource();
		String command = b.getText();

		if (command.equals("Cancel")) {
			this.setVisible(false);
		} else if (command.equals("Quit")) {
			System.exit(0);
		} else if (command.equals("Next")) {

			if (current_stage + 1 < this.number_of_stages) {
				current_stage++;
				this.stageTab.setSelectedIndex(current_stage);

				if (current_stage == number_of_stages - 1) {
					this.nextBtn.setText("Finish");
				}
			}
		} else if (command.equals("Back")) {
			if (current_stage - 1 >= 0) {
				this.current_stage--;
				this.stageTab.setSelectedIndex(current_stage);

				if (current_stage != number_of_stages - 1) {
					this.nextBtn.setText("Next");
				}
			}
		} else if (command.equals("Finish")) {
			System.out.println("---> Finish: start convert, save, load data");
			onFinish();
		} else if (command.equals("Select...")) {
			String selectedFile = FileUtility.selectASingleFile(this, "Select tabular data to import", null, FileUtility.LOAD);
			if (selectedFile != null) {
				this.setInputFile(new File(selectedFile));
			}
		} else {
			System.err.println("WARNING: Action Command not Found " + command);
		}
	}
	
	// returns zero-based number of first row of data, or -1 if Data Start Row input is invalid.
	private int getDataStartRow() {
		int result = -1;
		try {
			final int start = Integer.parseInt(this.start_number.getText());
			result = start - 1; // zero-base
		} catch (NumberFormatException e) {}
		return result;
	}
	
	private void updateSectionNamePreview() {
		String previewStr = "[can't create preview]";
		final int startRow = getDataStartRow();
		if (startRow != -1 && startRow >= 0 && startRow < this.parsedData.size()) {
			final String[] line = this.parsedData.get(startRow);
			String pattern = name_column.getText();
			String compName = TabularToXMLConversion.compositeSectionName(line, pattern);
			if (compName.length() > 0) {
				previewStr = compName;
			}
		}
		sectionNamePreview.setText(previewStr);
	}
	
	private JPanel createImportParamPanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		
		// File Import Parameters Panel
		JPanel fipp = new JPanel(new MigLayout("insets 5", "[][]20[]push[][]"));
		fipp.setBorder(BorderFactory.createTitledBorder("File Import Parameters"));
		fileLabel = new JLabel("[File Name]");
		JButton fileBtn = new JButton("Select...");
		fileBtn.addActionListener(this);

		this.fsComboBox = new JComboBox<FieldSeparator>();
		fsComboBox.setEditable(false);
		for (FieldSeparator fs : FieldSeparator.values()) {
			fsComboBox.addItem(fs);
		}
		fsComboBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (!suspendFieldSeparatorListener) {
					loadInputFile(inputFile, false); // reprocess current input file with selected delimiter
				}
			}
		});
		
		fipp.add(new JLabel("Selected File: "));
		fipp.add(fileLabel);
		fipp.add(fileBtn);
		fipp.add(new JLabel("Field Delimiter: "));
		fipp.add(fsComboBox);
		
		panel.add(fipp);
		
		// Data Import Parameters Panel
		JPanel dipp = new JPanel(new MigLayout("insets 5", "[][grow]15[][grow]", "[][][]15[][]"));
		dipp.setBorder(BorderFactory.createTitledBorder("Data Import Parameters"));
		
		// Data start/end rows
		start_number = new JTextField("3");
		end_number = new JTextField();
		dipp.add(new JLabel(DataStartRowLabel + ": "));
		dipp.add(start_number, "growx");
		dipp.add(new JLabel(DataEndRowLabel + ": "));
		dipp.add(end_number, "growx, wrap");		

		// Label, units rows
		label_number = new JTextField("1");
		dipp.add(new JLabel(FieldsRowLabel + ": "));
		dipp.add(label_number, "growx");
		unit_number = new JTextField("2");
		unitRowCheckbox = new JCheckBox("", true);
		unitRowLabel = new JLabel("Units Row: ");
		dipp.add(unitRowCheckbox, "split 2, gapright 0px");
		dipp.add(unitRowLabel);
		dipp.add(unit_number, "growx, wrap");

		// enable/disable Units Row label and field depending on checkbox state
		unitRowCheckbox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				final boolean enable = ((JCheckBox)e.getSource()).isSelected();
				unitRowLabel.setEnabled(enable);
				unit_number.setEnabled(enable);
			}
		});

		// Depth Column and Mode
		depth_column = new JTextField("2");
		depthModeComboBox = new JComboBox<DepthMode>();
		for (DepthMode dm : DepthMode.values()) {
			depthModeComboBox.addItem(dm);
		}
		dipp.add(new JLabel(DepthColLabel + ": "));
		dipp.add(depth_column, "growx");
		dipp.add(new JLabel("Depth Mode: "));
		dipp.add(depthModeComboBox, "growx, wrap");
		
		// Section Name Column and preview
		final JLabel name_label = new JLabel("Section Name Column: ");
		name_column = new JTextField("1");
		
		// Section name preview update listeners
		DocumentListener dl = new DocumentListener() {
			public void changedUpdate(DocumentEvent e) { updateSectionNamePreview(); }
			public void insertUpdate(DocumentEvent e) { updateSectionNamePreview(); }
			public void removeUpdate(DocumentEvent e) {updateSectionNamePreview(); }
		};

		start_number.getDocument().addDocumentListener(dl);
		name_column.getDocument().addDocumentListener(dl);

		dipp.add(name_label, "aligny center");
		dipp.add(name_column, "growx, aligny center, wmin 150");

		JPanel previewPanel = new JPanel(new MigLayout("insets 5", "[grow]", ""));
		previewPanel.setBorder(BorderFactory.createTitledBorder("Section Name Preview"));
		sectionNamePreview = new JLabel();
		java.awt.Font curFont = sectionNamePreview.getFont();
		sectionNamePreview.setFont(curFont.deriveFont(java.awt.Font.BOLD));
		previewPanel.add(sectionNamePreview, "alignx left, grow");
		dipp.add(previewPanel, "span 2, growx, wrap");
		panel.add(dipp);

		// Values to ignore
		dipp.add(new JLabel("Value to Ignore: "), "span 4, split 2");
		ignore_values = new JTextField("");
		dipp.add(ignore_values, "growx, wrap");

		return panel;
	}

	private JPanel createFieldSelectionPanel() {
		JPanel p = new JPanel(new BorderLayout());
		p.add(new JLabel("Select Data Columns to Import. Shift-click to select multiple items."), BorderLayout.NORTH);

		columnListModel = new Vector<DataColumnCheckBox>();
		columnList = new CheckBoxList();
		columnList.setListData(columnListModel);

		JScrollPane sp = new JScrollPane(columnList);

		p.add(sp, BorderLayout.CENTER);
		return p;
	}

	private void loadInputFile(final File f, final boolean detectSeparator) {
		if (f.exists()) {
			try {
				if (detectSeparator) {
					final String ext = FileUtility.getExtension(f).toLowerCase();
					if (ext.equals("tsv")) {
						setFieldSeparatorChar('\t');
					} else if (ext.equals("txt")) {
						setFieldSeparatorChar(' ');
					} else { // default to comma
						setFieldSeparatorChar(',');
					}
				}
				parsedData = OpenCSVParser.parseCSV(f, getFieldSeparatorChar());
				reduceParsedDataWhitespace();
				fileContent.setModel(new OpenCSVTableModel(parsedData));

				// if file is parsed successfully...
				fileContent.getColumnModel().getColumn(0).setCellRenderer(new RowColumnNumberRenderer(false));
				this.fileLabel.setText(f.getName());
				this.end_number.setText(Integer.toString(this.fileContent.getModel().getRowCount()));
				updateSectionNamePreview();
				this.pack(); // resize window to fit updated filename JLabel
			} catch (IOException e) {
				System.out.println("IOException: " + e.getMessage());
			} catch (CsvException e) {
				System.out.println("CSVException: " + e.getMessage());
			}
		}
	}

	// for cells with only whitespace, reduce to empty string
	private void reduceParsedDataWhitespace() {
		for (String[] row : this.parsedData) {
			for (int idx = 0; idx < row.length; idx++) {
				final String whitespaceStripped = row[idx].replaceAll("\\s", "");
				if (whitespaceStripped.equals("")) {
					row[idx] = whitespaceStripped;
				}
			}
		}
	}
	
	private char getFieldSeparatorChar() {
		char fs = ','; // default
		FieldSeparator fSeparator = (FieldSeparator) fsComboBox.getSelectedItem();
		if (fSeparator == FieldSeparator.TAB) {
			fs = '\t';
		} else if (fSeparator == FieldSeparator.SPACE) {
			fs = ' ';
		} else if (fSeparator == FieldSeparator.SEMICOLON) {
			fs = ';';
		}
		return fs;
	}

	private void setFieldSeparatorChar(final char fschar) {
		FieldSeparator fs = FieldSeparator.COMMA; // default
		if (fschar == '\t') {
			fs = FieldSeparator.TAB;
		} else if (fschar == ';') {
			fs = FieldSeparator.SEMICOLON;
		} else if (fschar == ' ') {
			fs = FieldSeparator.SPACE;
		}
		// stifle event when changing the combo box programmatically
		suspendFieldSeparatorListener = true;
		fsComboBox.setSelectedItem(fs);
		suspendFieldSeparatorListener = false;
	}

	// Get default XML export destination.
	private File getExportDir() {
		File exportDir = new File("");
		if (this.mode == RunMode.STANDALONE) { return exportDir; }
		final String curSessionStr = CorelyzerApp.getApp().getCurrentSessionFile();
		if (!curSessionStr.equals("")) {
			final File curSessionFile = new File(curSessionStr);
			exportDir = curSessionFile.getParentFile();
		} else { // use input file directory
			exportDir = this.inputFile.getParentFile();
		}
		return exportDir;
	}

	private void onFinish() {
		// Gather indices of data types to be imported
		Vector<Integer> vals = new Vector<Integer>();
		for (int i = 0; i < columnListModel.size(); i++) {
			DataColumnCheckBox c = columnListModel.elementAt(i);
			if (c.isSelected()) {
				vals.add(c.getDataColumn());
			}
		}
		if (vals.size() == 0) {
			JOptionPane.showMessageDialog(this, "Select at least one data column to import.");
			return;
		}
		System.out.println("Selected data import column indices (zero-based) = " + vals);
		
		int startLine, endLine, labelLine, unitLine, depthCol;
		try {
			// Convert 1-based user-facing line numbers to 0-based for processing
			startLine = Integer.parseInt(this.start_number.getText()) - 1;
			endLine = Integer.parseInt(this.end_number.getText()) - 1;
			labelLine = Integer.parseInt(this.label_number.getText()) - 1;
			if (unitRowCheckbox.isSelected()) {
				unitLine = Integer.parseInt(this.unit_number.getText()) - 1;
			} else {
				unitLine = labelLine;
			}
			depthCol = Integer.parseInt(this.depth_column.getText()) - 1;
		}  catch (NumberFormatException ex) {
			JOptionPane.showMessageDialog(this, "One or more fields contains an invalid number: " + ex.getMessage());
			return;
		}

		final String sectionNameCol = name_column.getText();
		DepthMode dm = (DepthMode) depthModeComboBox.getSelectedItem();

		float ignoreValue = Float.NaN;
		String inputText = ignore_values.getText();
		if (inputText.length() > 0) {
			if (inputText.contains(",")) {
				inputText = inputText.replace(",", ".");
			}

			try {
				ignoreValue = Float.valueOf(inputText);
			} catch (NumberFormatException e) {
				JOptionPane.showMessageDialog(this, "Invalid ignore value '" + inputText + "'");
				return;
			}
		}

		// Ask for output filename and save the file
		ExampleFileFilter xmlFilter = new ExampleFileFilter("xml", "XML file");
		JFileChooser chooser = new JFileChooser("Select Export Filename");
		chooser.setFileFilter(xmlFilter);
		chooser.setCurrentDirectory(getExportDir());
		int retVal = chooser.showSaveDialog(this);
		if (retVal != JFileChooser.APPROVE_OPTION) {
			return;
		}

		// make sure outputFile ends with .xml
		outputFile = chooser.getSelectedFile().getAbsoluteFile();
		String path = outputFile.getAbsolutePath();
		path = path.replace('\\', '/');
		String[] toks = path.split("/");
		if (!toks[toks.length - 1].contains(".xml")) {
			path = path + ".xml";
			outputFile = new File(path);
		}
		System.out.println("---> You choose to export to file: " + outputFile);

		convertTask = TabularToXMLConversion.createConvertTask(this, this.parsedData, outputFile, startLine, endLine,
			labelLine, unitLine, sectionNameCol, depthCol, vals, dm, ignoreValue);

		progressMonitor = new ProgressMonitor(stageTab, "Converting tabular data to XML...", null, 0, 100);
		progressMonitor.setProgress(0);
		progressMonitor.setMillisToDecideToPopup(500);
		progressMonitor.setMillisToPopup(0);
		convertTask.addPropertyChangeListener(this);
		convertTask.execute();
	}

	// Handle progress during XML conversion.
	public void propertyChange(PropertyChangeEvent evt) {
        System.out.println("Property change: " + evt.toString());
		if ("progress" == evt.getPropertyName()) {
			int progress = (Integer) evt.getNewValue();
			progressMonitor.setProgress(progress);
			String message = String.format("%d%% complete...", progress);
			progressMonitor.setNote(message);
			if (progressMonitor.isCanceled()) {
				if (progressMonitor.isCanceled()) {
					convertTask.cancel(true);
				}
			}
		} else if ("state" == evt.getPropertyName()) {
			final SwingWorker.StateValue state = ((SwingWorker.StateValue)evt.getNewValue());
			if (convertTask.isDone() && state == SwingWorker.StateValue.DONE) {
				try {
					if (convertTask.get()) {
						// conversion succeeded, load the generated XML data and close the dialog
						if (this.mode == RunMode.CORELYZER) {
							dispose();
							Runnable r = new Runnable() {
								public void run() { CorelyzerApp.getApp().loadData(outputFile);	}
							};
							new Thread(r).start();
						} else {
							JOptionPane.showMessageDialog(this, "Conversion Done", "Done", JOptionPane.INFORMATION_MESSAGE);
						}
					} else {
						JOptionPane.showMessageDialog(this, "Conversion Failed", "Done", JOptionPane.INFORMATION_MESSAGE);
					}
				} catch (Exception e) {
					// should never get here; the task is complete and thus won't throw an exception
					System.out.println("Completed(?) conversion somehow threw an exception: " + e.getMessage());
				}
			}
		}
		if (convertTask.isDone()) {
			progressMonitor.close();
		}
	}

	public void setInputFile(final File f) {
		this.inputFile = f;
		DataImportWizard.lastInputFileDirectory = new File(f.getAbsolutePath());
		loadInputFile(this.inputFile, true);
	}

	public void setInputFile(final String f) {
		this.setInputFile(new File(f));
	}

	public void setRunningMode(final RunMode m) {
		this.mode = m;
		if (mode == RunMode.CORELYZER) {
			cancelBtn.setText("Cancel");
		} else if (mode == RunMode.STANDALONE) {
			cancelBtn.setText("Quit");
		} else {
			System.err.println("WARNING: UNSUPPORTED RUNNING MODE.");
		}
	}

	private void setupUI() {
		this.setTitle("Plain Text Data Import");
		this.setSize(600, 600);
		this.setLocation(400, 50);

		// layout
		this.setLayout(new BorderLayout());

		// stage buttons
		nextBtn = new JButton("Next");
		nextBtn.addActionListener(this);
		backBtn = new JButton("Back");
		backBtn.addActionListener(this);
		cancelBtn = new JButton("Cancel");
		cancelBtn.addActionListener(this);

		JPanel p = new JPanel(new GridLayout(1, 3));
		p.add(cancelBtn);
		p.add(backBtn);
		p.add(nextBtn);

		this.add(p, BorderLayout.SOUTH);

		// -----------------------------------------
		// Main staging content
		stageTab = new JTabbedPane();
		stageTab.addTab("Import Parameters", createImportParamPanel());
		stageTab.addTab("Fields Selection", createFieldSelectionPanel());

		stageTab.addChangeListener(this);
		this.add(stageTab, BorderLayout.NORTH);

		// Table displaying parsed file contents
		fileContent = new JTable();
		fileContent.getTableHeader().setReorderingAllowed(false);
		fileContent.setShowGrid(true);
		fileContent.setGridColor(Color.GRAY);
		fileContent.getTableHeader().setDefaultRenderer(new RowColumnNumberRenderer(true));
		fileContent.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		JScrollPane sp = new JScrollPane(fileContent, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		sp.setBorder(BorderFactory.createTitledBorder("File Content"));
		sp.setBackground(p.getBackground()); // override default white backvground with root panel's background
		this.add(sp, BorderLayout.CENTER);
	}

	// selected tab changed
	public void stateChanged(final ChangeEvent e) {
		Object source = e.getSource();

		if (source instanceof JTabbedPane) {
			JTabbedPane p = (JTabbedPane) source;
			current_stage = p.getSelectedIndex();

			if (current_stage == this.number_of_stages - 1) {
				updateSelectedColumnList();
				this.nextBtn.setText("Finish");
			} else {
				this.nextBtn.setText("Next");
			}
		}
	}

	private boolean isValidPositiveInteger(String str) {
		try {
			final int val = Integer.parseInt(str);
			if (val <= 0) {
				return false;
			} else {
				return true;
			}
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private int getIntegerValue(String str) throws NumberFormatException {
		int result = 0;
		try {
			result = Integer.parseInt(str);
			return result;
		} catch (NumberFormatException e) {
			throw e;
		}
	}

	private boolean isValidNumber(String str) {
		try {
			Float.parseFloat(str);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private boolean isValidNumberOrEmpty(String str) {
		if (str.equals("") || isValidNumber(str)) {
			return true;
		}
		return false;
	}

	private boolean isValidDataColumn(int col, int startRow, int endRow) {
		for (int row = startRow; row <= endRow; row++) {
			final String cellStr = this.parsedData.get(row)[col];
			if (!isValidNumberOrEmpty(cellStr)) {
				System.out.println("Invalid data value '" + cellStr + "' found at row " + row + " col " +
					col + ", excluding this column from import list");
				return false;
			}
		}
		return true;
	}

	private boolean validateIntegerField(JTextField field, String fieldName) {
		if (!isValidPositiveInteger(field.getText())) {
			JOptionPane.showMessageDialog(this, fieldName + " requires a positive integer value.");
			return false;
		}
		return true;
	}

	private void updateSelectedColumnList() {
		columnListModel.clear();
		JTextField[] integerFields = { label_number, start_number, end_number, depth_column };
		String[] integerFieldNames = { FieldsRowLabel, DataStartRowLabel, DataEndRowLabel, DepthColLabel };
		for (int fieldIdx = 0; fieldIdx < integerFields.length; fieldIdx++) {
			if (!validateIntegerField(integerFields[fieldIdx], integerFieldNames[fieldIdx])) {
				return;
			}
		}

		// validate inputs
		int depthCol, fieldsRow, startRow, endRow;
		try {
			depthCol = getIntegerValue(depth_column.getText()) - 1;
			fieldsRow = getIntegerValue(label_number.getText()) - 1;
			startRow = getIntegerValue(start_number.getText()) - 1;
			endRow = getIntegerValue(end_number.getText()) - 1;
			
			if (endRow < startRow) {
				String msg = DataEndRowLabel + " must be >= to " + DataStartRowLabel;
				JOptionPane.showMessageDialog(this, msg);
				return;
			}
			if (endRow >= parsedData.size()) { // is data end row beyond end of file?
				String msg = DataEndRowLabel + " " + this.end_number.getText() + " exceeds the number of lines in the file (" + parsedData.size() + ").";
				JOptionPane.showMessageDialog(this, msg);
				return;
			}
			if (depthCol >= parsedData.get(0).length) { // is depth col in column range?
				String msg = DepthColLabel + " " + depth_column.getText() + " exceeds the number of columns in the file (" + parsedData.get(0).length + ").";
				JOptionPane.showMessageDialog(this, msg);
				return;
			}
			
			// ensure depth column is all numeric with no blanks
			for (int row = startRow; row <= endRow; row++) {
				final String strVal = this.parsedData.get(row)[depthCol];
				if (!isValidNumber(strVal)) {
					final String depthColName = this.parsedData.get(fieldsRow)[depthCol];
					JOptionPane.showMessageDialog(this, "Invalid depth '" + strVal + "' found at row " +
						(row + 1) + " of Depth Column '" + depthColName + "'");
					return;
				}
			}
		} catch (NumberFormatException e) {
			JOptionPane.showMessageDialog(this, "Invalid row or column number found: " + e.getMessage());
			return;
		}

		final int colCount = parsedData.get(0).length;
		for (int col = 0; col < colCount; col++) {
			if (col == depthCol) { continue; } // ignore depth column
			String colLabel = parsedData.get(fieldsRow)[col];
			if (colLabel.equals("")) { // use column # for label if empty
				colLabel = "[Column " + (col + 1) + "]";
			}
			columnListModel.addElement(new DataColumnCheckBox(colLabel, col));
		}
		columnList.updateUI();

		// brg 4/15/2020
		// We decided not to do this validation in 2.1. Corelyzer handles non-numeric values by
		// ignoring them when plotting, and we don't want to prevent users from plotting what
		// they want. In a future version, add UI to make better use of this,
		// indicating locations of non-numeric values in a separate list or the like.
		//
		// Add checkboxes for columns with numeric values or blanks in startRow
		// through endRow range.
		// (Inner class so we have access to method-local variables, neat!)
		// class ValidateDataColumnsTask extends SwingWorker<Void, Void> {
		// 	@Override
		// 	public Void doInBackground() {
		// 		final int colCount = parsedData.get(0).length;
		// 		for (int col = 0; col < colCount && !isCancelled(); col++) {
		// 			setProgress((int)(col/(float)colCount * 100.0f));
		// 			if (col == depthCol) { continue; } // ignore depth column
		// 			if (isValidDataColumn(col, startRow, endRow)) {
		// 				String colLabel = parsedData.get(fieldsRow)[col];
		// 				if (colLabel.equals("")) { // use column # for label if empty
		// 					colLabel = "[Column " + (col + 1) + "]";
		// 				}
		// 				columnListModel.addElement(new DataColumnCheckBox(colLabel, col));
		// 			}
		// 		}
		// 		return null;
		// 	}
	
		// 	@Override
		// 	public void done() {
		// 		columnList.updateUI();
		// 	}
		// }

		// progressMonitor = new ProgressMonitor(stageTab, "Finding valid data columns...", null, 0, 100);
		// progressMonitor.setProgress(0);
		// progressMonitor.setMillisToDecideToPopup(100);
		// progressMonitor.setMillisToPopup(0);
		// ValidateDataColumnsTask task = new ValidateDataColumnsTask();
		// task.addPropertyChangeListener(new TaskProgressListener(task, "Completed %d%%.\n", progressMonitor));
		// task.execute();
	}
}

// Model data parsed by OpenCSV. Display row number in first column.
class OpenCSVTableModel extends AbstractTableModel {
	static final public long serialVersionUID = -1L;
	private List<String[]> data;
	public OpenCSVTableModel(List<String[]> data) {
		this.data = data;
	}

	public String getColumnName(int col) {
		if (col == 0) {
			return "Row\\Column";
		} else {
			return new Integer(col).toString();
		}
	}
	public int getRowCount() {
		return data.size();
	}
	public int getColumnCount() { 
		return data.get(0).length + 1;
	}
	public Object getValueAt(int row, int col) {
		if (col == 0) {
			return new Integer(row + 1).toString();
		 } else {
			final int adjCol = col - 1;
			if (data.get(row).length > (adjCol)) {
				return data.get(row)[adjCol];
			}
		 }
		 return "";
	}
	public boolean isCellEditable(int row, int col) { return false; }
	public void setValueAt(Object value, int row, int col) { return; }
}

// render row & column number cells with gray background a la Excel
class RowColumnNumberRenderer extends JLabel implements TableCellRenderer {
	public final static long serialVersionUID = -1L;
    public RowColumnNumberRenderer(boolean border) {
		if (border) {
			setBorder(BorderFactory.createEtchedBorder());
		}
		setForeground(Color.BLACK);
		setBackground(new Color(222,222,222));
		setOpaque(true);
		setHorizontalAlignment(JLabel.CENTER);
    }
     
    @Override
	public Component getTableCellRendererComponent(JTable table, Object value,
		boolean isSelected, boolean hasFocus, int row, int column) {
        setText(value.toString());
        return this;
    }
}

// Track the data column corresponding to the label derived from specified fieldsRow.
// Now that we exclude non-numeric columns, we can no longer rely on the checkbox's
// index in the list matching its column in the tabular data source.
class DataColumnCheckBox extends JCheckBox {
	public final static long serialVersionUID = -1L;
	private int dataColumn = -1;
	public DataColumnCheckBox(String label, int dataColumn) {
		super(label);
		this.dataColumn = dataColumn;
	}
	public int getDataColumn() { return dataColumn; }
}
