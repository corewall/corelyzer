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

/**
 * Java side data structure to keep information of graph drawn.
 * 
 * 
 */
public class CoreSectionGraph {
	/** Field ID to be drawn */
	int field;
	/** Dataset ID to be used to fetch numerical data */
	int wellDataSet;
	/**
	 * corelyzer.data.Table ID within a dataset to be used to fetch numerical
	 * data
	 */
	int sectionTable;
	/** Minimum value of the viewing range */
	float min;
	/** Maximum value of the viewing range */
	float max;
	/** corelyzer.data.CoreSectionGraph ID of this graph object: native code id */
	int graphId;

	String sectionname;

	TrackSceneNode track;

	/** Default constructor */
	public CoreSectionGraph() {
		super();
	}

	/**
	 * Constructor with required data members
	 * 
	 * @param dataset
	 *            dataset ID used to fetch data
	 * @param table
	 *            table ID used to fetch data
	 * @param field
	 *            field ID used to fetch data
	 * @param id
	 *            graph ID returned from native scene graph
	 */
	public CoreSectionGraph(final int dataset, final int table, final int field, final int id, final TrackSceneNode t) {
		this();
		this.graphId = id;
		this.wellDataSet = dataset;
		this.sectionTable = table;
		this.field = field;
		this.track = t;
	}

	/** Data member access methods */
	public int getDataSetIndex() {
		return this.wellDataSet;
	}

	/** Data member access methods */
	public int getFieldIndex() {
		return this.field;
	}

	public int getId() {
		return this.graphId;
	}

	/** Data member access methods */
	public float getMax() {
		return this.max;
	}

	/** Data member access methods */
	public float getMin() {
		return this.min;
	}

	String getName() {
		return this.sectionname;
	}

	/** Data member access methods */
	public int getTableIndex() {
		return this.sectionTable;
	}

	public void setName(final String name) {
		this.sectionname = name;
	}
}
