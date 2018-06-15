/*
 * Copyright (C) 2011 Sony Ericsson Mobile Communications AB
 * Copyright (C) 2012 Sony Mobile Communications AB
 *
 * This file is part of ChkBugReport.
 *
 * ChkBugReport is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * ChkBugReport is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ChkBugReport.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.bill.bugexpert.plugins.ftrace;

import java.util.Comparator;

/* package */ class FTraceProcessRecordComparator implements Comparator<FTraceProcessRecord> {

    @Override
    public int compare(FTraceProcessRecord o1, FTraceProcessRecord o2) {
        if (o1.runTime < o2.runTime) return 1;
        if (o1.runTime > o2.runTime) return -1;
        if (o1.waitTime < o2.waitTime) return 1;
        if (o1.waitTime > o2.waitTime) return -1;
        if (o1.diskTime < o2.diskTime) return 1;
        if (o1.diskTime > o2.diskTime) return -1;
        return 0;
    }

}

