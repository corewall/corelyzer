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
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.xerces.dom.DocumentImpl;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.w3c.dom.Element;

import corelyzer.helper.ExampleFileFilter;

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
	public static enum DepthMode {
		SECTION_DEPTH, ACCUM_DEPTH
	}

	public static enum FieldSeparator {
		COMMA, TAB, SPACE
	}

	public static enum RunMode {
		CORELYZER, STANDALONE
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 2750206706267241630L;

	// Define auto-sections contains 1000 rows per section
	public static int maxSamplesPerSection = 300;

	/**
	 * Allow user use selected columns to compose a section name for example:
	 * prefix="glad-3A" pattern="7,2,-,9" return: "glad-3A-$7$2-$9"
	 * 
	 * @param prefix
	 *            Section name prefix
	 * @param line
	 *            The whole line of original data
	 * @param fs
	 *            Field separator
	 * @param pattern
	 *            Suffix pattern
	 * @return Composite section name
	 */
	private static String compositeSectionName(final String prefix, final String line, final String fs, final String pattern) {
		if (pattern == null || pattern.equals("")) {
			return prefix;
		}

		String[] tuples = pattern.split(",");
		String[] fields = line.split(fs);
		String suffix = "";

		for (String tuple : tuples) {
			tuple = tuple.trim();

			if (isInteger(tuple)) {
				// user input ordering starts from 1
				int order = Integer.parseInt(tuple) - 1;

				if (fields.length >= 0 && order >= 0 && order < fields.length) {
					suffix += fields[order].trim();
				} /*
				 * else { ; // ignore out of bound tuple assignment }
				 */
			} else {
				suffix += tuple;
			}
		}

		if (prefix.equals("")) {
			return suffix;
		} else {
			return prefix + "-" + suffix;
		}
	}

	public static void convert(final File fin, final File fout, final String fs, final String prefix, final int start, final int end, final int label,
			final int unit, final String name, final int depth, final Vector<Integer> vals, final DepthMode dm, final boolean useCustomizedSectionName,
			final float ignoreValue) {
		convert(null, fin, fout, fs, prefix, start, end, label, unit, name, depth, vals, dm, useCustomizedSectionName, ignoreValue);
	}

	public static void convert(final JDialog owner, final File fin, final File fout, final String fs, final String prefix, final int start, final int end,
			final int label, final int unit, final String name, final int depth, final Vector<Integer> vals, final DepthMode dm,
			final boolean useCustomizedSectionName, final float ignoreValue) {
		System.out.println("Converting...");

		// ProgressDialog progress = new ProgressDialog();
		CorelyzerApp app = CorelyzerApp.getApp();
		JProgressBar progress = app.getProgressUI();
		progress.setString("Converting data");
		progress.setValue(0);
		progress.setMaximum(end);
		progress.setString("");
		// progress.setVisible(true);

		// prepare XML writer
		DocumentImpl doc = new DocumentImpl();
		Element root;
		Element section_e = doc.createElement("test");

		// line counter
		int count = 0;

		// Vars for auto-section-definition
		int currentSectionSeqNumber = -1;
		int currentSectionSamplesCount = 0;
		float currentSectionDepthOffset = 0.0f;
		boolean isOffsetDefined;

		// State var for identifying sections
		boolean inSection = false;

		String line;
		String[] labels = {};
		String[] units = {};
		String currentSection = "";

		try {
			doc = new DocumentImpl();
			root = doc.createElement("corewall_data");

			if (root == null) {
				System.out.println("NULL!!!");
			}
			doc.appendChild(root);

			// read input file line by line
			BufferedReader reader = new BufferedReader(new FileReader(fin));

			while ((line = reader.readLine()) != null) {
				progress.setValue(count);

				// Put it this way in case someone doesn't have a unit line,
				// he/she will use the label line directly
				if (count == unit) { // unit
					units = line.split(fs);
				}

				if (count == label) { // label
					labels = line.split(fs);
				} else {
					if (count < start || count > end) {
						// skip to next line
						count++;
						continue;
					} else {
						if (!inSection) {
							// A new section
							System.out.println("---> New section at line #" + count);

							isOffsetDefined = false;

							section_e = doc.createElement("section");
							root.appendChild(section_e);
							inSection = true;

							if (useCustomizedSectionName) {
								currentSection = compositeSectionName(prefix, line, fs, name);
							} else {
								currentSectionSeqNumber++;
								currentSectionSamplesCount++;
								currentSection = prefix + "-" + currentSectionSeqNumber;
							}

							System.out.println("---> New section [" + currentSection + " created from the beginning");

							// add id block
							Element id_e = doc.createElement("id");
							id_e.setTextContent(currentSection);
							section_e.appendChild(id_e);

							// add depth_unit block
							Element depthunit_e = doc.createElement("depth_unit");
							String depth_unit = units[depth].trim();
							depthunit_e.setTextContent(depth_unit);
							section_e.appendChild(depthunit_e);

							// add fields block
							for (int i = 0; i < vals.size(); i++) {
								Element val_e = doc.createElement("field");

								int val_idx = vals.elementAt(i);
								val_e.setAttribute("localid", String.valueOf(i));
								val_e.setAttribute("name", labels[val_idx].trim());

								String _unit = val_idx < units.length ? units[val_idx].trim() : "";

								val_e.setAttribute("units", _unit);

								section_e.appendChild(val_e);
							}

							// add a depth + several sensors
							String[] tuples = line.split(fs);

							// depth
							Element depth_e = doc.createElement("depth");

							String depth_value = tuples[depth].trim();
							if (depth_value.contains(",")) {
								depth_value = depth_value.replace(",", ".");
							}

							// Remember the 1st depth value as the section's
							// depth offset
							if (dm == DepthMode.ACCUM_DEPTH) {
								if (!isOffsetDefined) {
									section_e.setAttribute("offset", depth_value);
									currentSectionDepthOffset = Float.valueOf(depth_value);
									isOffsetDefined = true;
								}

								float sectionDepth = Float.valueOf(depth_value) - currentSectionDepthOffset;

								depth_value = String.valueOf(sectionDepth);
							}

							depth_e.setTextContent(depth_value);
							section_e.appendChild(depth_e);

							// sensors
							for (int i = 0; i < vals.size(); i++) {
								String value = tuples[vals.elementAt(i)].trim();

								if (value.equals("")) {
									System.out.println("---> Ignore empty " + "value string.");
									// continue;
								} else {
									float floatValue;

									if (value.contains(",")) {
										value = value.replace(",", ".");
									}

									try {
										floatValue = Float.valueOf(value);
									} catch (NumberFormatException e) {
										// Not a number should just ignore it
										System.err.println("---> Ignore malformed " + "numbers: " + value);
										floatValue = Float.NaN;
									}

									// Ignore NaN and the ignoreValue match
									if (floatValue == ignoreValue) {
										continue;
									}

									Element value_e = doc.createElement("sensor");
									value_e.setAttribute("id", "" + i);
									value_e.setTextContent(value);
									section_e.appendChild(value_e);
								}
							}

						} else {
							// switch to a new section?
							boolean isStillInCurrentSection;

							if (useCustomizedSectionName) {
								String mysec = compositeSectionName(prefix, line, fs, name);
								isStillInCurrentSection = mysec.equals(currentSection);
							} else {
								isStillInCurrentSection = currentSectionSamplesCount < maxSamplesPerSection;
							}

							if (isStillInCurrentSection) {
								// Still within a section
								String[] tuples = line.split(fs);

								if (!useCustomizedSectionName) {
									currentSectionSamplesCount++;
								}

								// depth
								Element depth_e = doc.createElement("depth");

								String depth_value = tuples[depth].trim();
								if (depth_value.contains(",")) {
									depth_value = depth_value.replace(",", ".");
								}

								// Offset "currentSectionDepthOffset" to
								// to generate the section depth
								if (dm == DepthMode.ACCUM_DEPTH) {
									float sectionDepth = Float.valueOf(depth_value) - currentSectionDepthOffset;
									depth_value = String.valueOf(sectionDepth);
								}

								depth_e.setTextContent(depth_value);
								section_e.appendChild(depth_e);

								// sensors
								for (int i = 0; i < vals.size(); i++) {

									// Give a special value for missing cells?
									String value;
									if (vals.elementAt(i) < tuples.length) {
										value = tuples[vals.elementAt(i)].trim();
									} else {
										value = null;
									}

									if (value == null || value.trim().equals("")) {
										System.out.println("---> Ignore empty value " + "string.");
										// continue;
									} else {
										float floatValue;

										if (value.contains(",")) {
											value = value.replace(",", ".");
										}

										try {
											floatValue = Float.valueOf(value);
										} catch (NumberFormatException e) {
											// Not a number just ignore it
											System.err.println("---> Ignore malformed " + "numbers: " + value);
											floatValue = Float.NaN;
										}

										// Ignore NaN and the ignoreValue match
										if (floatValue == ignoreValue) {
											continue;
										}

										Element value_e = doc.createElement("sensor");
										value_e.setAttribute("id", "" + i);
										value_e.setTextContent(value);
										section_e.appendChild(value_e);
									}
								}

							} else {
								// A new section
								System.out.println("---> Switch to New section at line #" + count);
								isOffsetDefined = false;

								section_e = doc.createElement("section");
								root.appendChild(section_e);
								inSection = true;

								if (useCustomizedSectionName) {
									currentSection = compositeSectionName(prefix, line, fs, name);
								} else {
									currentSectionSeqNumber++;
									currentSection = prefix + "-" + currentSectionSeqNumber;

									currentSectionSamplesCount = 0;
								}

								System.out.println("---> Another New section [" + currentSection + " created from the last entry of " + "last section");

								// add id block
								Element id_e = doc.createElement("id");
								id_e.setTextContent(currentSection);
								section_e.appendChild(id_e);

								// add depth_unit block
								Element depthunit_e = doc.createElement("depth_unit");
								String depth_unit = units[depth].trim();
								depthunit_e.setTextContent(depth_unit);
								section_e.appendChild(depthunit_e);

								// add fields block
								for (int i = 0; i < vals.size(); i++) {
									Element val_e = doc.createElement("field");

									int val_idx = vals.elementAt(i);

									val_e.setAttribute("localid", String.valueOf(i));
									val_e.setAttribute("name", labels[val_idx].trim());

									String _unit = val_idx < units.length ? units[val_idx].trim() : "";

									val_e.setAttribute("units", _unit);

									section_e.appendChild(val_e);
								}

								// depth & sensors
								// Still within a section
								String[] tuples = line.split(fs);

								// depth
								Element depth_e = doc.createElement("depth");

								String depth_value = tuples[depth].trim();
								if (depth_value.contains(",")) {
									depth_value = depth_value.replace(",", ".");
								}

								// Remember the 1st depth value as the section's
								// depth offset
								if (dm == DepthMode.ACCUM_DEPTH) {
									if (!isOffsetDefined) {
										section_e.setAttribute("offset", depth_value);
										currentSectionDepthOffset = Float.valueOf(depth_value);
										isOffsetDefined = true;
									}

									float sectionDepth = Float.valueOf(depth_value) - currentSectionDepthOffset;

									depth_value = String.valueOf(sectionDepth);
								}

								depth_e.setTextContent(depth_value);
								section_e.appendChild(depth_e);

								// sensors
								for (int i = 0; i < vals.size(); i++) {
									String value = tuples[vals.elementAt(i)].trim();

									if (value.equals("")) {
										System.out.println("---> Ignore empty value " + "string.");
										// continue;
									} else {
										float floatValue;

										if (value.contains(",")) {
											value = value.replace(",", ".");
										}

										try {
											floatValue = Float.valueOf(value);
										} catch (NumberFormatException e) {
											// Not a number just ignore it
											System.err.println("---> Ignore malformed " + "numbers: " + value);
											floatValue = Float.NaN;
										}

										// Ignore NaN and the ignoreValue match
										if (floatValue == ignoreValue) {
											continue;
										}

										Element value_e = doc.createElement("sensor");
										value_e.setAttribute("id", "" + i);
										value_e.setTextContent(value);
										section_e.appendChild(value_e);
									} // end of value-null check
								} // end of for loop
							} // end of if-isStillInSection check
						} // end of if-insection-check
					} // end of if-line-count-range check
				} // end of if-label-line check

				count++;
			} // end of while loop

			System.out.println("---> " + count + " lines scaned.");
			progress.setIndeterminate(true);
			progress.setString("Writing to output file...");

			// Write to file
			OutputFormat format = new OutputFormat(doc);
			format.setIndenting(true);

			XMLSerializer serializer = new XMLSerializer(new FileOutputStream(fout), format);

			serializer.serialize(doc);

			progress.setIndeterminate(false);
			progress.setString("Writing to output file... done");

			/*
			 * FileOutputStream fos = new FileOutputStream(fout);
			 * XMLWriter.write(doc, fos); fos.flush(); fos.close();
			 */
		} catch (Exception e) {
			// System.err.println("Conversion Error! " + e);
			JOptionPane.showMessageDialog(owner, "Conversion Error!\n" + e);
			e.printStackTrace();
		}

		// progress.dispose();
		progress.setString("Done");
		progress.setValue(0);

		System.out.println("---> Done!");
	}

	private static boolean isInteger(final String str) {
		try {
			Integer.parseInt(str);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	public static void main(final String[] args) {
		DataImportWizard wiz = new DataImportWizard(null);
		wiz.setRunningMode(RunMode.STANDALONE);

		if (args.length < 1) {
			System.out.println("Usage: java corelyzer.ui.DataImportWizard <input>.");
			System.exit(0);
		}

		if (args[0] != null) {
			System.out.println("arg " + 0 + ": " + args[0]);
			wiz.setInputFile(args[0]);
		}

		wiz.setVisible(true);
	}

	// Running mode information
	RunMode mode = RunMode.CORELYZER;
	// stage information
	int number_of_stages = 5;

	int current_stage = 0;

	JButton nextBtn;
	JButton backBtn;
	JButton cancelBtn;
	JTabbedPane stageTab;
	LineNumberedPaper fileContent;
	/**
	 * Parse information Parse file 'inputFile', using 'delimitnator'. Data
	 * starts from 'start_row' to 'end_row'. Use 'label_row' as the labels for
	 * columns, 'unit_row' for units. 'sectionname_column' will be used as name
	 * of the sections, and 'sectiondepth_column' will be used as the depth
	 * value of the core section. Vector 'dataColumns' will be checked columns
	 * to get data values.
	 */
	JLabel fileLabel;
	JTextField start_number, end_number, ignore_values;
	JTextField label_number, unit_number;

	JTextField name_prefix, name_column, depth_column;
	JComboBox fsComboBox;

	JComboBox depthModeComboBox;

	JCheckBox toDefineSectionName;

	JCheckBox isIgnoreValues;

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
		} else if (command.equals("File...")) {
			JFileChooser chooser = new JFileChooser();
			chooser.setDialogTitle("Select import file");
			chooser.setMultiSelectionEnabled(false);
			int returnVal = chooser.showOpenDialog(this);

			if (returnVal == JFileChooser.APPROVE_OPTION) {
				File f = chooser.getSelectedFile();
				this.setInputFile(f);
			}
		} else {
			System.err.println("WARNING: Action Command not Found " + command);
		}
	}

	// stage 0
	private JPanel create_init_panel() {
		JPanel p = new JPanel(new GridLayout(2, 2));

		fileLabel = new JLabel("File Name");
		JButton fileBtn = new JButton("File...");
		fileBtn.addActionListener(this);

		JLabel fs_label = new JLabel("Field separator: ");
		// fs_string = new JTextField(",");

		this.fsComboBox = new JComboBox();
		fsComboBox.setEditable(false);
		for (FieldSeparator fs : FieldSeparator.values()) {
			fsComboBox.addItem(fs);
		}

		p.add(fileLabel);
		p.add(fileBtn);
		p.setBorder(BorderFactory.createTitledBorder("Input file name and field separator"));

		p.add(fs_label);
		p.add(fsComboBox);
		// p.add(fs_string);

		return p;
	}

	// stage 2
	private JPanel create_label_unit_panel() {
		JPanel p = new JPanel(new GridLayout(2, 2));
		p.setBorder(BorderFactory.createTitledBorder("Label  and Unit Row"));

		JLabel label_label = new JLabel("Fields Label Line Number: ");
		JLabel unit_label = new JLabel("Unit Label Line Number: ");

		label_number = new JTextField("4");
		unit_number = new JTextField("5");

		p.add(label_label);
		p.add(label_number);
		p.add(unit_label);
		p.add(unit_number);

		return p;
	}

	// stage 3
	private JPanel create_name_depth_panel() {
		JPanel p = new JPanel(new GridLayout(5, 2));
		p.setBorder(BorderFactory.createTitledBorder("Name and Depth Column"));

		final JLabel prefix_label = new JLabel("Section Prefix: ");
		prefix_label.setEnabled(false);
		final JLabel name_label = new JLabel("Name Column Number:  ");
		name_label.setEnabled(false);
		final JLabel depth_label = new JLabel("Depth Column Number: ");
		final JLabel depthMode_label = new JLabel("Depth Mode");

		depth_column = new JTextField("1");
		depthModeComboBox = new JComboBox();
		for (DepthMode dm : DepthMode.values()) {
			depthModeComboBox.addItem(dm);
		}

		name_prefix = new JTextField();
		name_prefix.setEnabled(false);
		name_column = new JTextField("");
		name_column.setEnabled(false);

		toDefineSectionName = new JCheckBox("Customize Section Name?");
		toDefineSectionName.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				boolean isChecked = toDefineSectionName.isSelected();

				name_prefix.setEnabled(isChecked);
				name_column.setEnabled(isChecked);
				prefix_label.setEnabled(isChecked);
				name_label.setEnabled(isChecked);
			}
		});

		p.add(depth_label);
		p.add(depth_column);

		p.add(depthMode_label);
		p.add(depthModeComboBox);

		p.add(toDefineSectionName);
		p.add(new JLabel(""));

		p.add(prefix_label);
		p.add(name_prefix);

		p.add(name_label);
		p.add(name_column);

		return p;
	}

	// --------------------------------------------------------------------------

	// stage 1
	private JPanel create_range_panel() {
		JPanel p = new JPanel(new GridLayout(3, 2));
		p.setBorder(BorderFactory.createTitledBorder("Start Row and End Row"));

		JLabel start_label = new JLabel("Start Line Number: ");
		JLabel end_label = new JLabel("End Line Number: ");

		start_number = new JTextField("7");
		end_number = new JTextField("17");
		ignore_values = new JTextField("");
		ignore_values.setEnabled(false);

		isIgnoreValues = new JCheckBox("Ignore some bad value?");
		isIgnoreValues.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				boolean isChecked = isIgnoreValues.isSelected();
				ignore_values.setEnabled(isChecked);
			}
		});

		p.add(start_label);
		p.add(start_number);
		p.add(end_label);
		p.add(end_number);
		p.add(isIgnoreValues);
		p.add(ignore_values);

		return p;
	}

	// stage 4
	private JPanel create_select_column_pane() {
		JPanel p = new JPanel(new BorderLayout());
		p.setBorder(BorderFactory.createTitledBorder("Pick Value Columns"));

		columnListModel = new Vector<JCheckBox>();
		columnList = new CheckBoxList();
		columnList.setListData(columnListModel);

		JScrollPane sp = new JScrollPane(columnList);

		p.add(sp, BorderLayout.CENTER);
		return p;
	}

	private JPanel create_stage(final int i) {
		JPanel p = new JPanel();

		switch (i) {
			case 0:
				return create_init_panel();
			case 1:
				return create_range_panel();
			case 2:
				return create_label_unit_panel();
			case 3:
				return create_name_depth_panel();
			case 4:
				return create_select_column_pane();
			default:
				System.err.println("create_stage: parameter out of range " + i);
		}

		return p;
	}

	private void loadInputFile(final File f) {
		if (f.exists()) {
			try {
				FileReader fr = new FileReader(f);
				this.fileContent.read(fr, null);
				this.fileLabel.setText(f.getName());
				this.end_number.setText(String.valueOf(this.fileContent.getLineCount() - 1));
			} catch (FileNotFoundException ex) {
				System.err.println("Error: File not found " + f);
			} catch (IOException ex) {
				System.err.println("Error: IO Exception " + f);
			}
		}
	}

	private void onFinish() {
		// Ask for output filename and save the file
		ExampleFileFilter xmlFilter = new ExampleFileFilter("xml", "XML file");

		JFileChooser chooser = new JFileChooser("Select Export Filename");
		chooser.setFileFilter(xmlFilter);
		int retVal = chooser.showSaveDialog(this);

		if (retVal != JFileChooser.APPROVE_OPTION) {
			return;
		}

		// make sure outputFile end with .xml
		outputFile = chooser.getSelectedFile().getAbsoluteFile();
		String path = outputFile.getAbsolutePath();
		path = path.replace('\\', '/');
		String[] toks = path.split("/");
		if (!toks[toks.length - 1].contains(".xml")) {
			path = path + ".xml";
			outputFile = new File(path);
		}
		System.out.println("---> You choose to export to file: " + outputFile);

		// Collect field values
		Vector<Integer> vals = new Vector<Integer>();
		for (int i = 0; i < columnListModel.size(); i++) {
			JCheckBox c = columnListModel.elementAt(i);

			if (c.isSelected()) {
				vals.add(i);
			}
		}

		// String fs = fs_string.getText();
		String fs;
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

		try {
			// Users assume line/column numbers starts from '1' instead of '0'
			int start = Integer.parseInt(this.start_number.getText()) - 1;
			int end = Integer.parseInt(this.end_number.getText()) - 1;
			int label = Integer.parseInt(this.label_number.getText()) - 1;
			int unit = Integer.parseInt(this.unit_number.getText()) - 1;

			String prefix = name_prefix.getText();
			String name = name_column.getText();
			int depth = Integer.parseInt(this.depth_column.getText()) - 1;

			boolean useCustomizedSectionName = toDefineSectionName.isSelected();
			DepthMode dm = (DepthMode) depthModeComboBox.getSelectedItem();

			float ignoreValue;
			if (this.isIgnoreValues.isSelected()) {
				String inputText = ignore_values.getText();

				if (inputText.contains(",")) {
					inputText = inputText.replace(",", ".");
				}

				try {
					ignoreValue = Float.valueOf(inputText);
				} catch (NumberFormatException e) {
					JOptionPane.showMessageDialog(this, "Input ignore value error");
					return;
				}
			} else {
				ignoreValue = Float.NaN;
			}

			// might need a progress thingy
			convert(inputFile, outputFile, fs, prefix, start, end, label, unit, name, depth, vals, dm, useCustomizedSectionName, ignoreValue);
		} catch (NumberFormatException ex) {
			JOptionPane.showMessageDialog(this, "Some fields you entered are not valid numbers");
			return;
		}

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
		loadInputFile(this.inputFile);

		String filename = f.getName();
		String prefix = filename.split("_")[0];
		this.name_prefix.setText(prefix);
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
		this.setSize(600, 800);
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

		// decision panel
		JPanel[] stage_pane = new JPanel[number_of_stages];
		String[] tabLabels = { "File Info", "Data Range", "Field & Unit Label", "Depth Setup", "Fields Selection" };
		for (int i = 0; i < number_of_stages; i++) {
			stage_pane[i] = create_stage(i);
			stageTab.addTab(tabLabels[i], stage_pane[i]);
		}

		stageTab.addChangeListener(this);
		this.add(stageTab, BorderLayout.NORTH);

		// Text area to show file content
		fileContent = new LineNumberedPaper(10, 10);
		fileContent.setEditable(false);
		fileContent.setBackground(new Color(254, 255, 182));
		JScrollPane sp = new JScrollPane(fileContent);
		sp.setBorder(BorderFactory.createTitledBorder("File Content"));
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

		} catch (Exception e) {
			System.err.println("Error! Cannot access inputfile");
		}

		if (label_line != null && !label_line.equals("")) {
			String fs;
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

			String[] tuples = label_line.split(fs);

			for (String tuple : tuples) {
				JCheckBox c = new JCheckBox(tuple);
				columnListModel.addElement(c);
			}
		}
		columnList.updateUI();
	}
}
