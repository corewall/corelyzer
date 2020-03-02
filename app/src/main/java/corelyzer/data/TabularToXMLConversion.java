package corelyzer.data;

import java.io.BufferedReader;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.util.List;
import java.util.Vector;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;

import org.apache.xerces.dom.DocumentImpl;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.w3c.dom.Element;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import javanet.staxutils.IndentingXMLStreamWriter;
import corelyzer.data.tabular.OpenCSVParser;
import corelyzer.ui.CorelyzerApp;


public class TabularToXMLConversion {
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
	public static String compositeSectionName(final String prefix, final String line, final String fs, final String pattern) {
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

	public static String compositeSectionName(final String[] tuples, final String pattern) {
		String[] patternTuples = pattern.split(",");
		String sectionName = "";

		for (String pt : patternTuples) {
			pt = pt.trim();

			if (isInteger(pt)) {
				// user input ordering starts from 1
				int order = Integer.parseInt(pt) - 1;

				if (tuples.length >= 0 && order >= 0 && order < tuples.length) {
					sectionName += tuples[order].trim();
				} /*
				 * else { ; // ignore out of bound pattern tuple assignment }
				 */
			} else {
				sectionName += pt;
			}
		}
		return sectionName;
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
	
	private static void writeNewSection(IndentingXMLStreamWriter ixmlWriter, final String depthOffset, final String currentSection, 
			final String depthUnit,	final Vector<Integer> vals, final String[] units, final String[] labels)
	{
		try { 
			ixmlWriter.writeStartElement("section");
			
			ixmlWriter.writeAttribute("offset", depthOffset);
			
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
	
	private static void writeDepthAndSensors(IndentingXMLStreamWriter ixmlWriter, final String depth, final Vector<Integer> vals,
			final String[] tuples, final float ignoreValue)
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
						System.err.println("---> Ignore malformed " + "number: " + value);
						// floatValue = Float.NaN;
						continue;
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

    // Line numbers are 0-based. Client is responsible for adjusting user-facing
    // (presumably 1-based) line numbers before passing to convert() method.
    public static void convertOpenCSV(final JDialog owner, List<String[]> table, final File fout, final int startLine, final int endLine,
        final int labelLine, final int unitLine, final String sectionNameCol, final int depthCol, final Vector<Integer> vals, final DepthMode dm,
        final float ignoreValue)
    {
		System.out.println("Converting...");

        // brg 2/5/2020 worth using progress bar?
		// ProgressDialog progress = new ProgressDialog();
		// CorelyzerApp app = CorelyzerApp.getApp();
		// JProgressBar progress = app.getProgressUI();
		// progress.setString("Converting data");
		// progress.setValue(0);
		// progress.setMaximum(endLine);
		// progress.setString("");
		// progress.setVisible(true);

		// prepare XML writer
		XMLStreamWriter baseXmlWriter = null;
		IndentingXMLStreamWriter ixmlWriter = null;
		try {
			FileOutputStream fos = new FileOutputStream(fout);
			BufferedOutputStream bos = new BufferedOutputStream(fos);
			baseXmlWriter = XMLOutputFactory.newInstance().createXMLStreamWriter(bos, "UTF-8");
			ixmlWriter = new IndentingXMLStreamWriter(baseXmlWriter);
		} catch (Exception e) {
            System.out.println("Exception converting raw data file to XML: " + e.getMessage());
            // pop error dialog? throw exception back to client?
            return;
        }
        
		try {
			ixmlWriter.writeStartDocument("UTF-8", "1.0");
            ixmlWriter.writeStartElement("corewall_data");
            
            int curLine = 0;
			String[] labels = {};
			String[] units = {};
            String currentSection = "";
			float currentSectionDepthOffset = 0.0f;
            
            for (curLine = 0; curLine <= endLine; curLine++) {
				// progress.setValue(curLine);

				// Put it this way in case someone doesn't have a unit line,
				// he/she will use the label line directly
				if (curLine == unitLine) { // unit
                    units = table.get(curLine);
				}
				if (curLine == labelLine) { // label
                    labels = table.get(curLine);
                }
                if (curLine < startLine || curLine == unitLine || curLine == labelLine) {
                    continue;
				}
				
				String[] tuples = table.get(curLine);

                // time to start a new section?
				String mysec = compositeSectionName(tuples, sectionNameCol);
				boolean needNewSection = !mysec.equals(currentSection);
                
                // if so, make sure we end the current section
                if (needNewSection && currentSection != "")
                {
                    ixmlWriter.writeEndElement(); // </section>
                    ixmlWriter.flush(); // flush buffered writer regularly to avoid OutOfMemoryErrors
                }

                String depth_value = tuples[depthCol].trim();
                if (depth_value.contains(",")) {
                    depth_value = depth_value.replace(",", ".");
                }

                final String sectionOffsetString = depth_value;

                if (dm == DepthMode.ACCUM_DEPTH) {
                    if (needNewSection)
                        currentSectionDepthOffset = Float.valueOf(depth_value);
                    
                    float sectionDepth = Float.valueOf(depth_value) - currentSectionDepthOffset;
                    depth_value = String.valueOf(sectionDepth);
                }

                if (needNewSection)
                {	
					currentSection = compositeSectionName(tuples, sectionNameCol);
					
                    final String depthUnit = units[depthCol].trim();
                    writeNewSection(ixmlWriter, sectionOffsetString, currentSection, depthUnit, vals, units, labels);
                    
					needNewSection = false;
					
					System.out.println("---> New section " + currentSection + " at (0-based) line " + curLine);
                }

                writeDepthAndSensors(ixmlWriter, depth_value, vals, tuples, ignoreValue);
            }
			// progress.setIndeterminate(true);
			// progress.setString("Writing to output file...");

			ixmlWriter.writeEndElement(); // </corelyzer_data>
			ixmlWriter.writeEndDocument();
			ixmlWriter.flush();
			ixmlWriter.close();

			// progress.setIndeterminate(false);
			// progress.setString("Writing to output file... done");
        } catch (Exception e) {
			// System.err.println("Conversion Error! " + e);
			JOptionPane.showMessageDialog(owner, "Conversion Error!\n" + e);
			e.printStackTrace();
        }

		// progress.dispose();
		// progress.setString("Done");
		// progress.setValue(0);

		System.out.println("---> Done!");        
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
						writeNewSection(ixmlWriter, depth_value, currentSection, depthUnit, vals, units, labels);
						
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
		// CorelyzerApp app = CorelyzerApp.getApp();
		// JProgressBar progress = app.getProgressUI();
		// progress.setString("Converting data");
		// progress.setValue(0);
		// progress.setMaximum(end);
		// progress.setString("");
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
				// progress.setValue(count);

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
			// progress.setIndeterminate(true);
			// progress.setString("Writing to output file...");

			// Write to file
			OutputFormat format = new OutputFormat(doc);
            format.setIndenting(true);
            format.setIndent(2);

			XMLSerializer serializer = new XMLSerializer(new FileOutputStream(fout), format);

			serializer.serialize(doc);

			// progress.setIndeterminate(false);
			// progress.setString("Writing to output file... done");

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
		// progress.setString("Done");
		// progress.setValue(0);

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

    public static void main(String[] args) {
        // Testing...import same file + parameters with old and new method,
		// hopefully get the same resulting XML.
		
		// TODO: Set up proper testing framework and directory structure???
		if (args.length == 0) {
			System.out.println("Required argument: path to testdata dir");
			return;
		} else {
			System.out.println("args[0] = " + args[0]);
		}
		final File testdataDir = new File(args[0]);
		// System.out.println("root testdata dir = " + testdataDir.getAbsolutePath());
		// test1 - single section, labelLine = 0 (no unitLine), import single value (column 11, Bulk Density (GRA))
        {
			File f = new File(testdataDir, "318-U1357-GRA-AB_secnames_1sec.csv");
			if (f.exists()) {
				System.out.println("It exists! " + f.getAbsolutePath());
			} else {
				System.out.println("Doesn't exist! " + f.getAbsolutePath());
			}
            Vector<Integer> vals = new Vector<Integer>();
            vals.add(new Integer(11));
            test(f, "/Users/lcdev/Desktop/test1", ",", "", 1, 60, 0, 0, "1", 9, vals, DepthMode.ACCUM_DEPTH, true, Float.NaN);
        }

		// test2 - as above with unitLine = 1
        {
            File f = new File(testdataDir, "318-U1357-GRA-AB_secnames_1sec_unitsrow.csv");
            Vector<Integer> vals = new Vector<Integer>();
            vals.add(new Integer(11));
            test(f, "/Users/lcdev/Desktop/test2", ",", "", 2, 61, 0, 1, "1", 9, vals, DepthMode.ACCUM_DEPTH, true, Float.NaN);
		}
		
		// test3 - as above with DepthMode.SECTION_DEPTH
        {
            File f = new File(testdataDir, "318-U1357-GRA-AB_secnames_1sec_unitsrow.csv");
            Vector<Integer> vals = new Vector<Integer>();
            vals.add(new Integer(11));
            test(f, "/Users/lcdev/Desktop/test3", ",", "", 2, 61, 0, 1, "1", 9, vals, DepthMode.SECTION_DEPTH, true, Float.NaN);
		}

		// test4 - goofy out-of-order section 1, labelLine = 0 unitLine = 1
        {
            File f = new File(testdataDir, "318-U1357-GRA-AB_secnames_1sec_out_of_order.csv");
            Vector<Integer> vals = new Vector<Integer>();
            vals.add(new Integer(11));
            test(f, "/Users/lcdev/Desktop/test4", ",", "", 2, 121, 0, 1, "1", 9, vals, DepthMode.ACCUM_DEPTH, true, Float.NaN);
		}		

		// test5 - two sections, labelLine = 0 unitLine = 1
        {
            File f = new File(testdataDir, "318-U1357-GRA-AB_secnames_2secs.csv");
            Vector<Integer> vals = new Vector<Integer>();
            vals.add(new Integer(11));
            test(f, "/Users/lcdev/Desktop/test5", ",", "", 2, 244, 0, 1, "1", 9, vals, DepthMode.ACCUM_DEPTH, true, Float.NaN);
		}
		
		// test6 - many sections and cores, labelLine = 0, unitLine = 1
		{
            File f = new File(testdataDir, "318-U1357-GRA-AB_secnames.csv");
            Vector<Integer> vals = new Vector<Integer>();
            vals.add(new Integer(11));
            test(f, "/Users/lcdev/Desktop/test6", ",", "", 2, 2392, 0, 1, "1", 9, vals, DepthMode.ACCUM_DEPTH, true, Float.NaN);
		}		

		// test7 - many sections and cores, properly sorted! labelLine = 0, unitLine = 1
		{
            File f = new File(testdataDir, "318-U1357-GRA-AB_secnames_sorted.csv");
            Vector<Integer> vals = new Vector<Integer>();
            vals.add(new Integer(11));
            test(f, "/Users/lcdev/Desktop/test7", ",", "", 2, 2392, 0, 1, "1", 9, vals, DepthMode.ACCUM_DEPTH, true, Float.NaN);
		}	

		// test8 - single section, single imported data column, first row of that column is blank
		// this should be fine in oldConvert() and newConvert()
		{
            File f = new File(testdataDir, "318-U1357-GRA-AB_secnames_1sec_blankfirstrow.csv");
            Vector<Integer> vals = new Vector<Integer>();
			vals.add(new Integer(11));
			// vals.add(new Integer(17));
            test(f, "/Users/lcdev/Desktop/test8", ",", "", 1, 60, 0, 0, "1", 9, vals, DepthMode.ACCUM_DEPTH, true, Float.NaN);
		}

		// test9 - single section, two imported data columns, first row of last imported column is blank
		// this should cause the oldConvert() method to fail - newConvert() should work correctly, ignoring the blank value
		// in first imported row and gather values in subsequent rows
		{
            File f = new File(testdataDir, "318-U1357-GRA-AB_secnames_1sec_blankfirstrow.csv");
            Vector<Integer> vals = new Vector<Integer>();
			vals.add(new Integer(11));
			vals.add(new Integer(16));
            test(f, "/Users/lcdev/Desktop/test9", ",", "", 1, 60, 0, 0, "1", 9, vals, DepthMode.ACCUM_DEPTH, true, Float.NaN);
		}

		// test10 - MSCL data for DSCI February 2020 class, ensure identical result from old and new conversion
		{
            File f = new File(testdataDir, "MCC_MSCL-S_Feldman.csv");
			Vector<Integer> vals = new Vector<Integer>();
			for (int i = 8; i <= 16; i++) {
				vals.add(new Integer(i));
			}
            test(f, "/Users/lcdev/Desktop/test10", ",", "", 1, 5810, 0, 0, "1", 7, vals, DepthMode.ACCUM_DEPTH, true, Float.NaN);
		}
		
		// test11 - XYZ data for DSCI February 2020 class, ensure identical result from old and new conversion
		{
            File f = new File(testdataDir, "MCC_MSCL-XYZ_Feldman.csv");
            Vector<Integer> vals = new Vector<Integer>();
			vals.add(new Integer(8)); // Laser Profiler
			vals.add(new Integer(9)); // MS
            test(f, "/Users/lcdev/Desktop/test11", ",", "", 1, 5400, 0, 0, "1", 7, vals, DepthMode.ACCUM_DEPTH, true, Float.NaN);
		}
    }

    public static void test(File inFile, String baseOutPath, String delimiter, String prefix, int startLine,
        int endLine, int labelLine, int unitLine, String sectionNameCol, int depthCol, Vector<Integer> vals,
        DepthMode dm, boolean customSectionName, float ignoreVal) {
        oldConvert(inFile, baseOutPath, delimiter, prefix, startLine, endLine, labelLine, unitLine, sectionNameCol, depthCol, vals, dm, customSectionName, ignoreVal);
        newConvert(inFile, baseOutPath, delimiter, startLine, endLine, labelLine, unitLine, sectionNameCol, depthCol, vals, dm, ignoreVal);
        // do a manual diff for now...
    }

    public static void oldConvert(File inFile, String baseOutPath, String delimiter, String prefix, int startLine,
        int endLine, int labelLine, int unitLine, String sectionNameCol, int depthCol, Vector<Integer> vals,
        DepthMode dm, boolean customSectionName, float ignoreVal)
    {
        File outFile = new File(baseOutPath + "_old.xml");
        TabularToXMLConversion.convert(inFile, outFile, delimiter, prefix, startLine, endLine, labelLine, unitLine, sectionNameCol, depthCol, vals, DepthMode.ACCUM_DEPTH, customSectionName, ignoreVal);
    }

    public static void newConvert(File inFile, String baseOutPath, String delimiter, int startLine,
        int endLine, int labelLine, int unitLine, String sectionNameCol, int depthCol, Vector<Integer> vals,
        DepthMode dm, float ignoreVal)
    {
        try {
            List<String[]> parsedData = OpenCSVParser.parseCSV(inFile, delimiter.charAt(0));
            File outFile = new File(baseOutPath + "_new.xml");
            TabularToXMLConversion.convertOpenCSV(null, parsedData, outFile, startLine, endLine, labelLine, unitLine, sectionNameCol, depthCol, vals, DepthMode.ACCUM_DEPTH, ignoreVal);
        } catch (Exception e) {
            System.out.println("Oh dear. " + e.getMessage());
        }
    }

}