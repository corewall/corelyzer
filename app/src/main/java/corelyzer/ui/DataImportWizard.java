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
import java.io.BufferedReader;
import java.io.File;

import java.io.FileReader;
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
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;

import net.miginfocom.swing.MigLayout;

import com.opencsv.exceptions.CsvException;

import corelyzer.helper.ExampleFileFilter;
import corelyzer.data.DepthMode;
import corelyzer.data.TabularToXMLConversion;
import corelyzer.data.tabular.OpenCSVParser;

/**
 * A text data import wizard that let user specify what their table data looks
 * like, reference Microsoft Excel's "Text Import Wizard"
 * 
 * Steps: 0. Init, select the import file and the field separator 1. Select the
 * 'Start' and 'End' row number of data 2. Select the 'label row' and 'unit row'
 * 3. Select the section-number/'label text' column Select the section depth
 * column 4. Check the columns that needs to be imported. AND Go!
 **/
public class DataImportWizard extends JDialog implements ActionListener, ChangeListener {
	public static enum FieldSeparator {
		COMMA, TAB, SPACE;
		
		public String toString() {
			if (this.ordinal() == 0)
				return "Comma";
			else if (this.ordinal() == 1)
				return "Tab";
			else // (this.ordinal() == 2)
				return "Space";
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
	// LineNumberedPaper fileContent;
	JTable fileContent;
	/**
	 * Parse information Parse file 'inputFile', using specified delimiter. Data
	 * starts from 'start_row' to 'end_row'. Use 'label_row' as the labels for
	 * columns, 'unit_row' for units. 'sectionname_column' will be used as name
	 * of the sections, and 'sectiondepth_column' will be used as the depth
	 * value of the core section. Vector 'dataColumns' will be checked columns
	 * to get data values.
	 */
	JLabel fileLabel, sectionNamePreview;
	JTextField start_number, end_number, ignore_values;
	JTextField label_number, unit_number;

	JTextField name_prefix, name_column, depth_column;
	JComboBox<FieldSeparator> fsComboBox;
	JComboBox<DepthMode> depthModeComboBox;

	CheckBoxList columnList;

	Vector<JCheckBox> columnListModel;

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
			JFileChooser chooser = new JFileChooser();
			chooser.setDialogTitle("Select import file");
			chooser.setMultiSelectionEnabled(false);
			chooser.setCurrentDirectory(DataImportWizard.lastInputFileDirectory);
			int returnVal = chooser.showOpenDialog(this);

			if (returnVal == JFileChooser.APPROVE_OPTION) {
				File f = chooser.getSelectedFile();
				this.setInputFile(f);
			}
		} else {
			System.err.println("WARNING: Action Command not Found " + command);
		}
	}
	
	private int getDataStartLine() {
		int result = -1;
		try {
			int start = Integer.parseInt(this.start_number.getText());
			result = start;
		} catch (NumberFormatException e) {}
		return result;
	}
	
	// return contents at specified line number (1-based)
	private String getInputFileLine(final int lineNumber) {
		String line = null;
		int curNum = 1;
		try {
			BufferedReader br = new BufferedReader(new FileReader(this.inputFile));
			String curLine = "";
			while (curLine != null) {
				curLine = br.readLine();
				if (curNum == lineNumber)
					break;
				curNum++;
			}
			line = curLine;
			br.close();
		} catch (IOException e) {
			
		}
		
		return line;
	}
	
	private void updateSectionNamePreview() {
		String previewStr = "[can't create preview]";
		final int startLine = getDataStartLine();
		if (startLine != -1) {
			String prefix = name_prefix.getText();
			String line = getInputFileLine(startLine);
			String fs = getFieldSeparatorChar();
			String pattern = name_column.getText();
			String compName = TabularToXMLConversion.compositeSectionName(prefix, line, fs, pattern);
			if (compName.length() > 0)
				previewStr = compName;
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
				System.out.println("Delimiter changed, reprocessing...");
				setInputFile(inputFile); // reprocess current input file with new delimiter
			}
		});
		
		fipp.add(new JLabel("Selected File: "));
		fipp.add(fileLabel);
		fipp.add(fileBtn);
		fipp.add(new JLabel("Field Delimiter: "));
		fipp.add(fsComboBox);
		
		panel.add(fipp);
		
		// Data Import Parameters Panel
		JPanel dipp = new JPanel(new MigLayout("insets 5", "[][grow]15[][grow]", ""));
		dipp.setBorder(BorderFactory.createTitledBorder("Data Import Parameters"));
		
		// data start/end
		start_number = new JTextField("3");
		end_number = new JTextField();
		dipp.add(new JLabel("Data Start Line: "));
		dipp.add(start_number, "growx");
		dipp.add(new JLabel("Data End Line: "));
		dipp.add(end_number, "growx, wrap");		

		// label, units
		label_number = new JTextField("1");
		unit_number = new JTextField("2");
		dipp.add(new JLabel("Fields Line: "));
		dipp.add(label_number, "growx");
		dipp.add(new JLabel("Units Line: "));
		dipp.add(unit_number, "growx, wrap");

		// depth, section name column, prefix
		depth_column = new JTextField("2");
		depthModeComboBox = new JComboBox<DepthMode>();
		for (DepthMode dm : DepthMode.values()) {
			depthModeComboBox.addItem(dm);
		}
		dipp.add(new JLabel("Depth Column: "));
		dipp.add(depth_column, "growx");
		dipp.add(new JLabel("Depth Mode: "));
		dipp.add(depthModeComboBox, "growx, wrap");
		
		final JLabel name_label = new JLabel("Section Name Column: ");
		name_column = new JTextField("1");
		JButton multiColumnButton = new JButton("Multiple Section Columns...");
		// JPanel sectionNamePanel = new JPanel(new MigLayout("insets 5"));
		dipp.add(name_label);
		dipp.add(name_column, "growx");
		dipp.add(multiColumnButton, "span 2, wrap, align center");
		// dipp.add(sectionNamePanel, "growx, span, wrap");

		
		// Section Name subpanel
		// JPanel snpp = new JPanel(new MigLayout("insets 5", "[grow]", ""));
		// snpp.setBorder(BorderFactory.createTitledBorder("Section Name"));
		
		// final String snht = new String("<html>If your data includes a column with full section names, " +
		// 		"enter the column number in the Column/Pattern field. " +
		// 		"If components of the section name are in separate columns, " +
		// 		"they can be flexibly combined by entering a pattern of column numbers " +
		// 		"and separators. This pattern will be applied per-row. " +
		// 		"The contents of the Prefix field, if any, will be prepended with a trailing hyphen.<br/>" +
		// 		"Examples:<ul><li>  1,-,2 with no Prefix: '[col1]-[col2]'</li>" +
		// 		"<li>3,2,-,4  with Prefix BOB: 'BOB-[col3][col2]-[col4]'</li>" +
		// 		"<li>7,@@@,1,2,3 with Prefix hello: 'hello-[col7]@@@[col1][col2][col3]'</li></ul></html>");
		// final JLabel sectionNameHelpText = new JLabel(snht);
		// snpp.add(sectionNameHelpText, "span");
		
		
		final JLabel prefix_label = new JLabel("Prefix: ");
		name_prefix = new JTextField();
		
		DocumentListener dl = new DocumentListener() {
			public void changedUpdate(DocumentEvent e) { updateSectionNamePreview(); }
			public void insertUpdate(DocumentEvent e) { updateSectionNamePreview(); }
			public void removeUpdate(DocumentEvent e) {updateSectionNamePreview(); }
		};
		
		name_column.getDocument().addDocumentListener(dl);
		name_prefix.getDocument().addDocumentListener(dl);
		
		// JPanel secDataPanel = new JPanel(new MigLayout("", "[][grow]15[][grow]", ""));
		// secDataPanel.add(name_label);
		// secDataPanel.add(name_column, "growx, wmin 150");
		// secDataPanel.add(prefix_label);
		// secDataPanel.add(name_prefix, "growx, wmin 150");
		// snpp.add(secDataPanel, "wrap");
		
		JPanel previewPanel = new JPanel(new MigLayout("insets 5", "[grow][]", ""));
		previewPanel.setBorder(BorderFactory.createTitledBorder("Section Name Preview"));
		sectionNamePreview = new JLabel();
		java.awt.Font curFont = sectionNamePreview.getFont();
		sectionNamePreview.setFont(curFont.deriveFont(java.awt.Font.BOLD));
		previewPanel.add(sectionNamePreview, "grow");
		JButton updatePreview = new JButton("Update Preview");
		updatePreview.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				updateSectionNamePreview();
			}
		});
		previewPanel.add(updatePreview);
		dipp.add(previewPanel, "growx, span 4, split 2, wrap");
		// panel.add(previewPanel);
		// snpp.add(previewPanel, "span, growx, wrap");
		
		// panel.add(sectionNamePanel);
		// value to ignore
		dipp.add(new JLabel("Ignore Values: "), "span 4, split 2");
		ignore_values = new JTextField("");
		dipp.add(ignore_values, "growx");

		panel.add(dipp);
		
		return panel;
	}

	private JPanel createFieldSelectionPanel() {
		JPanel p = new JPanel(new BorderLayout());
		p.setBorder(BorderFactory.createTitledBorder("Select Data Columns for Import"));

		columnListModel = new Vector<JCheckBox>();
		columnList = new CheckBoxList();
		columnList.setListData(columnListModel);

		JScrollPane sp = new JScrollPane(columnList);

		p.add(sp, BorderLayout.CENTER);
		return p;
	}

	private void loadInputFile(final File f) {
		if (f.exists()) {
			try {
				parsedData = OpenCSVParser.parseCSV(f, getFieldSeparatorChar().charAt(0));
				fileContent.setModel(new OpenCSVTableModel(parsedData));

				// if file is parsed successfully...
				fileContent.getColumnModel().getColumn(0).setCellRenderer(new RowColumnNumberRenderer(false));
				this.fileLabel.setText(f.getName());
				this.end_number.setText(Integer.toString(this.fileContent.getModel().getRowCount()));
				this.pack(); // resize window to fit updated filename JLabel
			} catch (IOException e) {
				System.out.println("IOException: " + e.getMessage());
			} catch (CsvException e) {
				System.out.println("CSVException: " + e.getMessage());
			}
		}
	}
	
	// return last data line, ignoring empty lines at end of file
	// private int getDefaultEndLine() {
	// 	return this.fileContent.getRowCount();
	// 	// int dataEndLine = lastLine;
	// 	// try {
	// 	// 	for (int curLine = lastLine; curLine >= 1; curLine--) {
	// 	// 		final int start = this.fileContent.getLineStartOffset(curLine - 1);
	// 	// 		final int end = this.fileContent.getLineEndOffset(curLine - 1);
	// 	// 		if (this.fileContent.getText(start, end - start).trim().length() == 0)
	// 	// 			dataEndLine = curLine - 1;
	// 	// 		else
	// 	// 			break;
	// 	// 	}
	// 	// } catch (Exception e) {
	// 	// 	System.err.println("Error finding last data line: " + e.getMessage());
	// 	// }
	// 	// return lastLine;
	// }
	
	private String getFieldSeparatorChar() {
		String fs = "";
		FieldSeparator fSeparator = (FieldSeparator) fsComboBox.getSelectedItem();
		if (fSeparator == FieldSeparator.COMMA) {
			fs = ",";
		} else if (fSeparator == FieldSeparator.TAB) {
			fs = "\t";
		} else if (fSeparator == FieldSeparator.SPACE) {
			fs = " ";
		} else {
			fs = " ";
		}
		return fs;
	}

	private void onFinish() {
		// Gather indices of data types to be imported
		Vector<Integer> vals = new Vector<Integer>();
		for (int i = 0; i < columnListModel.size(); i++) {
			JCheckBox c = columnListModel.elementAt(i);
			if (c.isSelected()) {
				vals.add(i);
			}
		}
		if (vals.size() == 0) {
			JOptionPane.showMessageDialog(this, "Select at least one data column to import.");
			return;
		}
		
		int startLine, endLine, labelLine, unitLine, depthCol;
		try {
			// Convert 1-based user-facing line numbers to 0-based for processing
			startLine = Integer.parseInt(this.start_number.getText()) - 1;
			endLine = Integer.parseInt(this.end_number.getText()) - 1;
			labelLine = Integer.parseInt(this.label_number.getText()) - 1;
			unitLine = Integer.parseInt(this.unit_number.getText()) - 1;
			depthCol = Integer.parseInt(this.depth_column.getText()) - 1;
		}  catch (NumberFormatException ex) {
			JOptionPane.showMessageDialog(this, "One or more fields contains an invalid number");
			return;
		}

		if (endLine >= parsedData.size()) {
			String msg = "Data End Line " + this.end_number.getText() + " exceeds the number of lines in the file (" + parsedData.size() + ").";
			JOptionPane.showMessageDialog(this, msg);
			return;
		}

		final String prefix = name_prefix.getText();
		final String sectionNameCol = name_column.getText();
		final boolean useCustomizedSectionName = (prefix.length() > 0 || sectionNameCol.length() > 0);
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

		// todo - try/catch; progress indicator; don't dispose until process succeeds
		// (otherwise user has to re-enter everything from scratch)
		// passive warning about unmatched section names (in Section Name preview?)
		// ProgressDialog progDlg = new ProgressDialog();
		TabularToXMLConversion.convertOpenCSV(this, this.parsedData, outputFile, prefix, startLine, endLine, labelLine, unitLine, sectionNameCol, depthCol, vals, dm, useCustomizedSectionName, ignoreValue);

		if (this.mode == RunMode.CORELYZER) {
			Runnable r = new Runnable() {
				public void run() {
					CorelyzerApp.getApp().loadData(outputFile);
				}
			};
			new Thread(r).start();
			dispose();
		} else { // standalone mode
			JOptionPane.showMessageDialog(this, "Conversion Done", "Done", JOptionPane.INFORMATION_MESSAGE);
		}
	}

	public void setInputFile(final File f) {
		this.inputFile = f;
		DataImportWizard.lastInputFileDirectory = new File(f.getAbsolutePath());
		loadInputFile(this.inputFile);
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

		// Text area to show file content
		// fileContent = new LineNumberedPaper(10, 10);
		// fileContent.setEditable(false);
		// fileContent.setBackground(new Color(254, 255, 182));
		fileContent = new JTable();
		fileContent.setShowGrid(true);
		fileContent.setGridColor(Color.GRAY);
		fileContent.getTableHeader().setDefaultRenderer(new RowColumnNumberRenderer(true));
		fileContent.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		JScrollPane sp = new JScrollPane(fileContent, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		sp.setBorder(BorderFactory.createTitledBorder("File Content"));
		sp.setBackground(p.getBackground()); // override default white backvground with root panel's background
		this.add(sp, BorderLayout.CENTER);
	}

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

	private void updateSelectedColumnList() {
		columnListModel.clear();

		if (label_number.getText().equals("")) {
			return;
		}

		int label_line_number = Integer.parseInt(label_number.getText());
		int count = 1;
		String label_line = "";

		try {
			BufferedReader reader = new BufferedReader(new FileReader(this.inputFile));

			while ((label_line = reader.readLine()) != null) {
				if (count == label_line_number) {
					break;
				}
				count++;
			}
			reader.close();

		} catch (Exception e) {
			System.err.println("Error! Cannot access input file");
		}

		if (label_line != null && !label_line.equals("")) {
			final String fs = getFieldSeparatorChar();
			String[] tuples = label_line.split(fs);

			for (String tuple : tuples) {
				JCheckBox c = new JCheckBox(tuple);
				columnListModel.addElement(c);
			}
		}
		columnList.updateUI();
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