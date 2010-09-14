package corelyzer.data;

import java.util.Vector;

/**
 * User: julian Date: Mar 20, 2006 Time: 3:10:57 PM
 */

public class AnnotationRepository {
	Vector<AnnotationThread> annothreads;

	public AnnotationRepository() {
		this.annothreads = new Vector<AnnotationThread>();
	}

	void clean() {
		this.annothreads.removeAllElements();
	}

	void free(final int index) {
		// TODO
		this.annothreads.removeElementAt(index);
	}

	AnnotationThread getThread(final int index) {
		return this.annothreads.elementAt(index);
	}

	int insert(final AnnotationThread thread) {
		/*
		 * int index = annothreads.size(); annothreads.push_back( thread );
		 * moveToFront(index); return index;
		 */
		return 0;
	}

	int length() {
		return annothreads.size();
	}

	void moveToFront(final int i) {
		// TODO
		/*
		 * if (i<0 || i>= annothreads.size() ) { return; }
		 * 
		 * corelyzer.data.AnnotationThread val = annothreads.elementAt(i);
		 * 
		 * for (int k=i; k>0; k--) { annothreads[k] = annothreads[k-1]; }
		 * annothreads[0] = val;
		 */
	}
}
