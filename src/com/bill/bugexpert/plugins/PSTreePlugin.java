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
package com.bill.bugexpert.plugins;

import com.bill.bugexpert.BugExpertModule;
import com.bill.bugexpert.Module;
import com.bill.bugexpert.Plugin;
import com.bill.bugexpert.doc.Chapter;
import com.bill.bugexpert.doc.ProcessLink;
import com.bill.bugexpert.doc.Table;
import com.bill.bugexpert.ps.PSRecord;

public class PSTreePlugin extends Plugin {

    private static final String TAG = "[PSTreePlugin]";

    @Override
    public int getPrio() {
        return 98;
    }

    @Override
    public void reset() {
        // NOP
    }

    @Override
    public void load(Module br) {
        // NOP
    }

    @Override
    public void generate(Module rep) {
        BugExpertModule br = (BugExpertModule) rep;

        PSRecord ps = br.getPSTree();
        if (ps == null) {
            br.logE(TAG + "Process list information not found! (aborting plugin)");
            return;
        }

        Chapter ch = br.findOrCreateChapter("CPU/Process tree");

        Table t = new Table(Table.FLAG_NONE, ch);
        t.addStyle("treeTable pstree");
        t.addColumn("PID", Table.FLAG_NONE);
        t.addColumn("PPID", Table.FLAG_ALIGN_RIGHT);
        t.addColumn("Name", Table.FLAG_NONE);
        t.addColumn("Nice", Table.FLAG_ALIGN_RIGHT);
        t.addColumn("Policy", Table.FLAG_ALIGN_RIGHT);
        t.begin();
        genPSTree(br, ps, t);
        t.end();
    }

    private void genPSTree(BugExpertModule br, PSRecord ps, Table t) {
        int pid = ps.getPid();
        if (pid != 0) {
            int ppid = ps.getParentPid();
            if (ppid != 0) {
                t.setNextRowStyle("child-of-pstree-" + ppid);
            }
            t.setNextRowId("pstree-" + pid);
            t.addData(new ProcessLink(br, pid, ProcessLink.SHOW_PID));
            t.addData(new ProcessLink(br, ppid, ProcessLink.SHOW_PID));
            t.addData(new ProcessLink(br, pid, ProcessLink.SHOW_NAME));
            t.addData(ps.getNice());
            t.addData(ps.getPolicyStr());
        }

        for (PSRecord child : ps) {
            genPSTree(br, child, t);
        }
    }

}
