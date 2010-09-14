package corelyzer.data;

import java.util.Vector;

/** TODO Class Deprecated */
/** This class holds corelyzer.data.WellLogDataSet handles. */
public class DataSetRepository {
	public static DataSetRepository dsRepo;

	public static DataSetRepository getsingleton() {
		return dsRepo;
	}

	public Vector<WellLogDataSet> dataSets;

	// -- Methods
	// constructors
	public DataSetRepository() {
		this.dataSets = new Vector<WellLogDataSet>();
		dsRepo = this;
	}

	public void clear() {
		dataSets.removeAllElements();
	}

	public WellLogDataSet getDataSet(final int index) {
		return this.dataSets.elementAt(index);
	}

	public int insert(final WellLogDataSet w) {
		this.dataSets.add(w);
		return this.dataSets.size() - 1;
	}

	public int length() {
		return this.dataSets.size();
	}
}
