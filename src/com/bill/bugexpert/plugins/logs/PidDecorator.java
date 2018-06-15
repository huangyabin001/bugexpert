package com.bill.bugexpert.plugins.logs;

import com.bill.bugexpert.BugExpertModule;
import com.bill.bugexpert.ProcessRecord;
import com.bill.bugexpert.doc.Renderer;

public class PidDecorator extends Decorator {

    private ProcessRecord mPr;

    public PidDecorator(int start, int end, BugExpertModule br, int pid) {
        super(start, end);
        mPr = br.getProcessRecord(pid, true, true);
    }

    @Override
    public void render(Renderer r, boolean start) {
        if (start) {
            r.print("<a href=\"" + mPr.getAnchor().getHRef() + "\">");
        } else {
            r.print("</a>");
        }

    }

}
