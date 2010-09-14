package corelyzer.data;

public class Column {
	int datatype;
	int minIndex;
	int maxIndex;

	// void* vecPtr;
	// void* dataArray;
	float dataArray[];
	boolean valid[];

	public Column() {
		this.minIndex = -1;
		this.maxIndex = -1;
	}

	public Column(final int length) {
		this();
		this.dataArray = new float[length];
		this.valid = new boolean[length];
	}

	int getDataType() {
		return this.datatype;
	}

	boolean isValid(final int r) {
		return this.valid[r];
	}

	void setDataType(final int type) {
		this.datatype = type;
	}

	float valueAtRow(final int r) {
		return this.dataArray[r];
	}
}
