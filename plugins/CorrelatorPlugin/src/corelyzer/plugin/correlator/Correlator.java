/******************************************************************************
 *
 * CoreWall / Corelyzer - An Initial Core Description Tool
 * Copyright (C) 2008 Julian Yu-Chung Chen
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
package corelyzer.plugin.correlator;

/**  The Java interface to native Correlator instance */

public class Correlator {
    static {
        System.loadLibrary("correlator");
    }

    // JNI native methods
    public native static boolean init();
    public native static void finish();

    public native static void updateData();
    public native static void execute(boolean hasSplice);

    // inputs
    public native static void setCoreFormat(int format);
    public native static int  getCoreFormat();
    public native static void setCoreType(int type);
    public native static int  getCoreType();

    public native static void loadAffineTable(String aFile);
    public native static void loadSpliceTable(String aFile);
    public native static void loadData(String aFile);

    // outputs
    public native static String[] getHoleNames();
    public native static String[] getCoreNames(int holeIdx);
    public native static String[] getSectionNames(int holeIdx, int coreIdx);
    public native static String getLeg(int holeIdx);
    public native static String getSite(int holeIdx);
    public native static char getCoreType(int holeIdx, int coreIdx);

    // a float array of [d0, v0, d1, v1, ...]
    public native static float [] getHoleData(int holeIdx);

    // return single [depth, value] tuple (2 floats)
    public native static float [] getDataTuple(int holeIdx, int sectionIdx,
                                               int tupleIdx);

    // debug printouts
    public native static void debugPrintOut();
    public native static void printoutData();    

    // parameters
    // culling
    public native static void setCullFilter(boolean isEnabled);
    public native static void setCullTop(float aValue);
    public native static void setCullNo(float aValue);
    public native static void setCullSignal(float aValue);
    public native static void setCullEquation(float v1, float v2,
                                       int sign1, int sign2, int join);

    // decimation
    public native static void setDecimationFilter(boolean isEnabled);
    public native static void setDecimationParam(float aValue);

    // gaussian
    public native static void setGaussianFilter(boolean isEnabled);
    public native static void setGaussianParam(float smooth, int unit);

    // data retriver
    public native static float[] getTuple(int index);

    public native static float[] getSectionInterval(int holeIdx, int coreIdx,
                                                    int sectionIdx);
    
}
