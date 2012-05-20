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

package corelyzer.data;

import java.awt.Point;
import java.io.File;

import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableColumn;

/**
 * Image property table and model for loading images with different 1. Image
 * filepath 2. Orientation 3. Length 3. DPI_X 4. DPI_Y 5. Depth in meter 6. URL
 * todo
 */
public class ImagePropertyTable extends JTable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -6893081336980390952L;
	final public static float DEFAULT_DPI = 254.0f;
	final public static float DEFAULT_DEPTH = 0.0f;
	final public static float DEFAULT_LENGTH = 1.5f;
	float current_depth = 0.0f;

	public ImagePropertyTableModel model;

	public ImagePropertyTable() {
		super();
		model = new ImagePropertyTableModel();
		setModel(model);
		setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		// Set column header text
		getColumnModel().getColumn(0).setHeaderValue("Filename");
		getColumnModel().getColumn(1).setHeaderValue("Orientation");
		getColumnModel().getColumn(2).setHeaderValue("Length (m)");
		getColumnModel().getColumn(3).setHeaderValue("DPI_X");
		getColumnModel().getColumn(4).setHeaderValue("DPI_Y");
		getColumnModel().getColumn(5).setHeaderValue("Depth (m)");
		
		// Set preferred widths - large filename column, smaller columns for numeric data
		getColumnModel().getColumn(0).setPreferredWidth( 200 );
		getColumnModel().getColumn(1).setPreferredWidth( 40 );
		getColumnModel().getColumn(2).setPreferredWidth( 40 );
		getColumnModel().getColumn(3).setPreferredWidth( 40 );
		getColumnModel().getColumn(4).setPreferredWidth( 40 );
		getColumnModel().getColumn(5).setPreferredWidth( 40 );

		// JComboBox column
		JComboBox comboBox = new JComboBox();
		comboBox.addItem(ImagePropertyTableModel.HORIZONTAL);
		comboBox.addItem(ImagePropertyTableModel.VERTICAL);
		TableColumn col = getColumnModel().getColumn(1);
		col.setCellEditor(new DefaultCellEditor(comboBox));

		this.setShowGrid(true);
		this.setShowHorizontalLines(true);
		this.setShowVerticalLines(true);

		getTableHeader().resizeAndRepaint();
	}

	public void addImageAndProperties(final String file, final String orientation, final float length, final float dpix, final float dpiy, final float depth) {
		/*
		 * String orientation = ImagePropertyTableModel.HORIZONTAL; if(0 ==
		 * orientationIdx) { orientation = ImagePropertyTableModel.HORIZONTAL; }
		 * else if(1 == orientationIdx) { orientation =
		 * ImagePropertyTableModel.VERTICAL; }
		 */

		model.filepathVec.add(file);
		model.fileNameVec.add(new File(file).getName());
		model.orientationVec.add(orientation);
		model.lengthVec.add(length);
		model.dpixVec.add(dpix);
		model.dpiyVec.add(dpiy);
		model.depthVec.add(depth);
		// todo model.urlVec.add(url);

		current_depth += depth;
	}

	public void addImagePath(final String filepath) {
		model.filepathVec.add(filepath);
		model.fileNameVec.add(new File(filepath).getName());

		// FIXME Use ImageIO with query image properties
		File f = new File(filepath);

		getImageDPI(f);

		model.orientationVec.add(ImagePropertyTableModel.HORIZONTAL);
		model.lengthVec.add(DEFAULT_LENGTH);
		model.dpixVec.add(0.0f);
		model.dpiyVec.add(0.0f);
		model.depthVec.add(this.current_depth);
		current_depth += DEFAULT_LENGTH;
	}

	public void applyAllDepths(float start, final float inc) {
		for (int i = 0; i < model.getRowCount(); i++) {
			model.depthVec.set(i, start);
			start += inc;
		}
		updateUI();
	}

	public void function() {
		System.out.println("hello");
	}
	
	public void applyAllDPIX(final float dpiX) {
		for (int i = 0; i < this.model.getRowCount(); i++) {
			model.dpixVec.set(i, dpiX);
		}
		updateUI();
	}

	public void applyAllDPIY(final float dpiY) {
		for (int i = 0; i < this.model.getRowCount(); i++) {
			model.dpiyVec.set(i, dpiY);
		}
		updateUI();
	}

	// FIXME To show image file dimension
	/*
	 * public String getToolTipText(MouseEvent e) { String tip = null; Point p =
	 * e.getPoint(); int rowIndex = rowAtPoint(p); int colIndex =
	 * columnAtPoint(p);
	 * 
	 * int realColumnIndex = convertColumnIndexToModel(colIndex);
	 * 
	 * if(realColumnIndex == 0) { // filename column Dimension d =
	 * model.imgSize.get(rowIndex); tip = "Image size: " + d; }
	 * 
	 * return tip; }
	 */

	public void applyAllLength(final float length) {
		for (int i = 0; i < model.getRowCount(); i++) {
			model.lengthVec.set(i, length);
		}

		updateUI();
	}

	public void applyAllOrientation(final int orientationIdx) {
		String orientation = ImagePropertyTableModel.HORIZONTAL;
		if ( orientationIdx == 1 ) {
			orientation = ImagePropertyTableModel.VERTICAL;
		} else if ( orientationIdx == 2 ) {
			return; // don't apply "[Blank]", which exists only to exclude orientation from a batch apply
		}

		for (int i = 0; i < this.model.getRowCount(); i++) {
			model.orientationVec.set(i, orientation);
		}
		updateUI();
	}

	public void clearTable() {
		model.filepathVec.clear();
		model.fileNameVec.clear();
		model.orientationVec.clear();
		model.lengthVec.clear();
		model.dpixVec.clear();
		model.dpiyVec.clear();
		model.depthVec.clear();
		// model.imgSize.clear();
		current_depth = 0.0f;

		repaint();
	}

	// FIXME method to get image DPI using ImageIO
	// ref: http://dave.thielen.com/articles/Bitmap%20Resolution%20in%20Java.htm
	private Point getImageDPI(final Object fil) {
		float xDPI = ImagePropertyTable.DEFAULT_DPI;
		float yDPI = ImagePropertyTable.DEFAULT_DPI;

		/*
		 * try { // get a reader that can read this bitmap type ImageInputStream
		 * imageInput = ImageIO.createImageInputStream(fil); Iterator it =
		 * ImageIO.getImageReaders(imageInput); if (!it.hasNext()) return new
		 * Point(72, 72); ImageReader reader = (ImageReader) it.next();
		 * 
		 * reader.setInput(imageInput); IIOMetadata meta =
		 * reader.getImageMetadata(0); org.w3c.dom.Node n =
		 * meta.getAsTree("javax_imageio_1.0");
		 * 
		 * n = n.getFirstChild(); while (n != null) { if
		 * (n.getNodeName().equals("Dimension")) { org.w3c.dom.Node n2 =
		 * n.getFirstChild(); while (n2 != null) { if
		 * (n2.getNodeName().equals("HorizontalPixelSize")) {
		 * org.w3c.dom.NamedNodeMap nnm = n2.getAttributes(); org.w3c.dom.Node
		 * n3 = nnm.item(0); float hps = Float.parseFloat(n3.getNodeValue());
		 * xDPI = Math.round(25.4f / hps); } if
		 * (n2.getNodeName().equals("VerticalPixelSize")) {
		 * org.w3c.dom.NamedNodeMap nnm = n2.getAttributes(); org.w3c.dom.Node
		 * n3 = nnm.item(0); float vps = Float.parseFloat(n3.getNodeValue());
		 * yDPI = Math.round(25.4f / vps); } n2 = n2.getNextSibling(); } } n =
		 * n.getNextSibling(); }
		 * 
		 * } catch (Exception e) { e.printStackTrace(); }
		 */

		return new Point((int) xDPI, (int) yDPI);
	}
	
	public static class ImageProperties {
		public String orientation;
		public float length;
		public float depth;
		public float dpix;
		public float dpiy;
		
		public ImageProperties() {
			orientation = "Horizontal";
			length = DEFAULT_LENGTH;
			depth = DEFAULT_DEPTH;
			dpix = dpiy = DEFAULT_DPI;
		}
		
		public String toString() {
			return orientation + ", length " + length + ", depth " + depth + ", DPIx " + dpix + ", DPIy " + dpiy;
		}
	}
}
