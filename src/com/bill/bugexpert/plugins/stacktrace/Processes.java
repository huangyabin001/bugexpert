package com.bill.bugexpert.plugins.stacktrace;

import java.util.Vector;

import com.bill.bugexpert.Module;
import com.bill.bugexpert.doc.Chapter;

/* package */ final class Processes extends Vector<Process> {

    private int mId;
    private String mName;
    private String mSectionName;
    private Vector<StackTrace> mBusy = new Vector<StackTrace>();
    private Chapter mCh;

    public Processes(Module report, int id, String name, String sectionName) {
        mId = id;
        mName = name;
        mSectionName = sectionName;
        mCh = new Chapter(report.getContext(), name);
    }

    public int getId() {
        return mId;
    }

    public String getName() {
        return mName;
    }

    public String getSectionName() {
        return mSectionName;
    }

    public Chapter getChapter() {
        return mCh;
    }

    private static final long serialVersionUID = 1L;

    public void addBusyThreadStack(StackTrace stack) {
        if (!mBusy.contains(stack)) {
            mBusy.add(stack);
        }
    }

    public Vector<StackTrace> getBusyStackTraces() {
        return mBusy;
    }

    public Process findPid(int pid) {
        for (Process p : this) {
            if (p.getPid() == pid) {
                return p;
            }
        }
        return null;
    }

    public Vector<StackTrace> getAIDLCalls() {
        Vector<StackTrace> ret = new Vector<StackTrace>();
        for (Process proc : this) {
            for (StackTrace thread : proc) {
                if (thread.getAidlDependency() != null) {
                    ret.add(thread);
                }
            }
        }
        return ret;
    }

}