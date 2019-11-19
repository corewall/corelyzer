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
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
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
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import net.miginfocom.swing.MigLayout;

import org.apache.xerces.dom.DocumentImpl;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.w3c.dom.Element;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import javanet.staxutils.IndentingXMLStreamWriter;

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
		SECTION_DEPTH, ACCUM_DEPTH;
		
		public String toString() {
			if (this.ordinal() == 0)
				return "Section Depth";
			else
				return "Accumulated Depth";
		}
	}

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

	public static void sax_convert(final File fin, final File fout, final String fs, final String prefix, final int start, final int end, final int label,
			final int unit, final String name, final int depth, final Vector<Integer> vals, final DepthMode dm, final boolean useCustomizedSectionName,
			final float ignoreValue)
	{
		sax_convert(null, fin, fout, fs, prefix, start, end, label, unit, name, depth, vals, dm, useCustomizedSectionName, ignoreValue);
	}
	
	private static void writeNewSection( IndentingXMLStreamWriter ixmlWriter, final float depthOffset, final String currentSection, 
			final String depthUnit,	final Vector<Integer> vals, final String[] units, final String[] labels)
	{
		try { 
			ixmlWriter.writeStartElement("section");
			
			ixmlWriter.writeAttribute("offset", Float.toString(depthOffset));
			
			ixmlWriter.writeStartElement("id");
			ixmlWriter.writeCharacters(currentSection);
			ixmlWriter.writeEndElement();

			ixmlWriter.writeStartElement("depth_unit");
			ixmlWriter.writeCharacters( depthUnit );
			ixmlWriter.writeEndElement();
			
			// add fields block
			for (int i = 0; i < vals.size(); i++) {
				ixmlWriter.writeEmptyElement("field");

				int val_idx = vals.elementAt(i);
				String _unit = val_idx < units.length ? units[val_idx].trim() : "";
				ixmlWriter.writeAttribute("localid", String.valueOf(i));
				ixmlWriter.writeAttribute("name", labels[val_idx].trim());
				ixmlWriter.writeAttribute("units", _unit);
			}
		} catch (Exception e) {
			System.out.println("Exception in writeNewSection: " + e.getMessage());
		}
	}
	
	private static void writeDepthAndSensors( IndentingXMLStreamWriter ixmlWriter, final String depth, final Vector<Integer> vals,
			final String[] tuples, final float ignoreValue )
	{
		try {
			ixmlWriter.writeStartElement("depth");
			ixmlWriter.writeCharacters(depth);
			ixmlWriter.writeEndElement(); // <depth>
	
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
	
					ixmlWriter.writeStartElement("sensor");
					ixmlWriter.writeAttribute("id", "" + i);
					ixmlWriter.writeCharacters(value);
					ixmlWriter.writeEndElement(); // <sensor>
				}
			}
		} catch (Exception e) {
			System.out.println("Exception in writeDepthAndSensors(): " + e.getMessage());
		}
	}
	
	public static void sax_convert(final JDialog owner, final File fin, final File fout, final String fs, final String prefix, final int start, final int end,
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
		
		XMLStreamWriter baseXmlWriter = null;
		IndentingXMLStreamWriter ixmlWriter = null;
		try {
			FileOutputStream fos = new FileOutputStream( fout );
			BufferedOutputStream bos = new BufferedOutputStream( fos );
			baseXmlWriter = XMLOutputFactory.newInstance().createXMLStreamWriter( bos, "utf-8" );
			ixmlWriter = new IndentingXMLStreamWriter( baseXmlWriter );
		} catch ( Exception e ) {
			System.out.println("Exception converting raw data file to XML: " + e.getMessage() );
		}

		// line counter
		int lineCount = 0;
		boolean needNewSection = true;

		// Vars for auto-section-definition
		int currentSectionSeqNumber = -1;
		int currentSectionSamplesCount = 0;
		float currentSectionDepthOffset = 0.0f;

		try {
			ixmlWriter.writeStartDocument("utf-8", "1.0");
			ixmlWriter.writeStartElement("corewall_data");

			// read input file line by line
			BufferedReader reader = new BufferedReader(new FileReader(fin));

			String curInputLine;
			String[] labels = {};
			String[] units = {};
			String currentSection = "";

			while ((curInputLine = reader.readLine()) != null) {
				progress.setValue(lineCount);

				// Put it this way in case someone doesn't have a unit line,
				// he/she will use the label line directly
				if (lineCount == unit) { // unit
					units = curInputLine.split(fs);
				}
				if (lineCount == label) { // label
					labels = curInputLine.split(fs);
				}
				
				if (lineCount >= start && lineCount <= end)
				{
					String[] tuples = curInputLine.split(fs);
					String depth_value = tuples[depth].trim();
					if (depth_value.contains(",")) {
						depth_value = depth_value.replace(",", ".");
					}

					if (dm == DepthMode.ACCUM_DEPTH) {
						if ( needNewSection )
							currentSectionDepthOffset = Float.valueOf(depth_value);
						
						float sectionDepth = Float.valueOf(depth_value) - currentSectionDepthOffset;
						depth_value = String.valueOf(sectionDepth);
					}

					if ( needNewSection )
					{	
						System.out.println("---> New section at line #" + lineCount);

						if (useCustomizedSectionName) {
							currentSection = compositeSectionName(prefix, curInputLine, fs, name);
						} else {
							currentSectionSeqNumber++;
							currentSection = prefix + "-" + currentSectionSeqNumber;
							currentSectionSamplesCount = 0;
						}

						final String depthUnit = units[depth].trim();
						writeNewSection( ixmlWriter, currentSectionDepthOffset, currentSection, depthUnit, vals, units, labels );
						
						needNewSection = false;
					}

					writeDepthAndSensors( ixmlWriter, depth_value, vals, tuples, ignoreValue );
					
					// time to start a new section?
					if ( useCustomizedSectionName ) {
						String mysec = compositeSectionName(prefix, curInputLine, fs, name);
						needNewSection = !mysec.equals(currentSection);
					} else {
						currentSectionSamplesCount++;
						needNewSection = currentSectionSamplesCount >= maxSamplesPerSection;
					}
					
					// if so, make sure we end the current section
					if ( needNewSection )
					{
						ixmlWriter.writeEndElement(); // <section>
 
						// flush buffered writer periodically or buffer will become ginormous
						// and cause OutOfMemoryErrors.
						ixmlWriter.flush();
					}
				}

				lineCount++;
			} // end input file line-reading while loop
			reader.close();

			System.out.println("---> " + lineCount + " lines scanned.");
			progress.setIndeterminate(true);
			progress.setString("Writing to output file...");

			ixmlWriter.writeEndElement(); // <corelyzer_data>
			ixmlWriter.writeEndDocument();
			ixmlWriter.flush();
			ixmlWriter.close();

			progress.setIndeterminate(false);
			progress.setString("Writing to output file... done");

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

							// sensors - brg DUPLICATION
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

								// sensors - brg DUPLICATION
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
								System.out.println("Java VM Heap Size: " + (int)(Runtime.getRuntime().totalMemory() / 1024) + "kB");

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
								// brg: -1 arg ensures all delimiters are parsed and included in resulting array...without
								// this arg, tuples can be shorter than vals array resulting in ArrayOutOfBoundsExceptions.
								String[] tuples = line.split(fs, -1);

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
			reader.close();

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

	// stage information - brg 7/31/2015 "stages" are tabs, vars used for Back/Next button handling
	int number_of_stages = 2;
	int current_stage = 0;

	JButton nextBtn, backBtn, cancelBtn;
	JTabbedPane stageTab;
	LineNumberedPaper fileContent;
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
			String compName = compositeSectionName(prefix, line, fs, pattern);
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
		
		// value to ignore
		dipp.add(new JLabel("Exclude Specific Value: "), "span 4, split 2");
		ignore_values = new JTextField("");
		dipp.add(ignore_values, "growx");

		panel.add(dipp);
		
		// Section Name subpanel
		JPanel snpp = new JPanel(new MigLayout("insets 5", "[grow]", ""));
		snpp.setBorder(BorderFactory.createTitledBorder("Section Name"));
		
		final String snht = new String("<html>If your data includes a column with full section names, " +
				"enter the column number in the Column/Pattern field. " +
				"If components of the section name are in separate columns, " +
				"they can be flexibly combined by entering a pattern of column numbers " +
				"and separators. This pattern will be applied per-row. " +
				"The contents of the Prefix field, if any, will be prepended with a trailing hyphen.<br/>" +
				"Examples:<ul><li>  1,-,2 with no Prefix: '[col1]-[col2]'</li>" +
				"<li>3,2,-,4  with Prefix BOB: 'BOB-[col3][col2]-[col4]'</li>" +
				"<li>7,@@@,1,2,3 with Prefix hello: 'hello-[col7]@@@[col1][col2][col3]'</li></ul></html>");
		final JLabel sectionNameHelpText = new JLabel(snht);
		snpp.add(sectionNameHelpText, "span");
		
		final JLabel name_label = new JLabel("Column/Pattern: ");
		name_column = new JTextField("1");
		
		final JLabel prefix_label = new JLabel("Prefix: ");
		name_prefix = new JTextField();
		
		DocumentListener dl = new DocumentListener() {
			public void changedUpdate(DocumentEvent e) { updateSectionNamePreview(); }
			public void insertUpdate(DocumentEvent e) { updateSectionNamePreview(); }
			public void removeUpdate(DocumentEvent e) {updateSectionNamePreview(); }
		};
		
		name_column.getDocument().addDocumentListener(dl);
		name_prefix.getDocument().addDocumentListener(dl);

		JPanel secDataPanel = new JPanel(new MigLayout("", "[][grow]15[][grow]", ""));
		secDataPanel.add(name_label);
		secDataPanel.add(name_column, "growx, wmin 150");
		secDataPanel.add(prefix_label);
		secDataPanel.add(name_prefix, "growx, wmin 150");
		snpp.add(secDataPanel, "wrap");
		
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
		snpp.add(previewPanel, "span, growx, wrap");
		
		panel.add(snpp);
		
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
				FileReader fr = new FileReader(f);
				this.fileContent.read(fr, null);
				this.fileLabel.setText(f.getName());
				this.end_number.setText(String.valueOf(getDefaultEndLine()));
				updateSectionNamePreview();
			} catch (FileNotFoundException ex) {
				System.err.println("Error: File not found " + f);
			} catch (IOException ex) {
				System.err.println("Error: IO Exception " + f);
			}
		}
	}
	
	// return last data line, ignoring empty lines at end of file
	private int getDefaultEndLine() {
		final int lastLine = this.fileContent.getLineCount();
		int dataEndLine = lastLine;
		try {
			for (int curLine = lastLine; curLine >= 1; curLine--) {
				final int start = this.fileContent.getLineStartOffset(curLine - 1);
				final int end = this.fileContent.getLineEndOffset(curLine - 1);
				if (this.fileContent.getText(start, end - start).trim().length() == 0)
					dataEndLine = curLine - 1;
				else
					break;
			}
		} catch (Exception e) {
			System.err.println("Error finding last data line: " + e.getMessage());
		}
		return dataEndLine;
	}
	
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

		try {
			// Users assume line/column numbers starts from '1' instead of '0'
			int start = Integer.parseInt(this.start_number.getText()) - 1;
			int end = Integer.parseInt(this.end_number.getText()) - 1;
			int label = Integer.parseInt(this.label_number.getText()) - 1;
			int unit = Integer.parseInt(this.unit_number.getText()) - 1;

			final String fs = getFieldSeparatorChar();
			String prefix = name_prefix.getText();
			String name = name_column.getText();
			int depth = Integer.parseInt(this.depth_column.getText()) - 1;

			boolean useCustomizedSectionName = (prefix.length() > 0 || name.length() > 0);
			DepthMode dm = (DepthMode) depthModeComboBox.getSelectedItem();

			float ignoreValue;
			String inputText = ignore_values.getText();
			if (inputText.length() > 0) {
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
			JOptionPane.showMessageDialog(this, "One or more fields contains an invalid number");
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
		stageTab.addTab("Import Parameters", createImportParamPanel());
		stageTab.addTab("Fields Selection", createFieldSelectionPanel());

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
