/******************************************************************************
 *
 * CoreWall / Corelyzer - An Initial Core Description Tool
 * Copyright (C) 2007 Julian Yu-Chung Chen
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
package corelyzer.plugin.iCores.handlers;

public abstract class Job implements Runnable {
    public static final double SYSTEM_JOB = -100.0;
    public static final double UI_JOB = -10.0;

    protected RequestContext context;
    protected final String name;

    public Job(final String name) {
        this.name = name;
    }

    public abstract double getDepth();

    public String getName() {
        return name;
    }

    protected abstract void execute();
    
    public final void run() {
        execute();
        context.jobCompleted();
    }

    public void setContext(final RequestContext context) {
        this.context = context;
    }

}
