package com.bill.bugexpert.plugins.stacktrace;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.Vector;

import com.bill.bugexpert.BugExpertModule;
import com.bill.bugexpert.doc.Chapter;
import com.bill.bugexpert.ps.PSRecord;

/* package */ final class Process implements Iterable<StackTrace> {

    private int mPid;
    private String mName;
    private Vector<StackTrace> mStacks = new Vector<StackTrace>();
    private Vector<PSRecord> mUnknownThreads= new Vector<PSRecord>();
    private WeakReference<Processes> mGroup;
    private String mDate;
    private String mTime;
    private Chapter mChapter;

    public Process(BugExpertModule br, Processes processes, int pid, String date, String time) {
        mGroup = new WeakReference<Processes>(processes);
        mPid = pid;
        mDate = date;
        mTime = time;
        mChapter = new Chapter(br.getContext(), "");
    }

    public Processes getGroup() {
        return mGroup.get();
    }

    public String getDate() {
        return mDate;
    }

    public String getTime() {
        return mTime;
    }

    public void addBusyThreadStack(StackTrace stack) {
        mGroup.get().addBusyThreadStack(stack);
    }

    public StackTrace findTid(int tid) {
        for (StackTrace stack : mStacks) {
            if (stack.getTid() == tid) {
                return stack;
            }
        }
        return null;
    }

    public StackTrace findPid(int pid) {
        for (StackTrace stack : mStacks) {
            if (stack.getPid() == pid) {
                return stack;
            }
        }
        return null;
    }

    public int indexOf(int tid) {
        for (int i = 0; i < mStacks.size(); i++) {
            if (mStacks.get(i).getTid() == tid) {
                return i;
            }
        }
        return -1;
    }

    public int getPid() {
        return mPid;
    }

    //the name in cmdline,like "Cmd line: android.process.media".
    public void setName(String cmdline) {
        mName = cmdline;
        mChapter.setName(cmdline + " (" + mPid + ")");
    }

    public String getName() {
        return mName;
    }

    public void addStackTrace(StackTrace stackTrace) {
        mStacks.add(stackTrace);
    }

    public int getCount() {
        return mStacks.size();
    }

    public StackTrace get(int idx) {
        return mStacks.get(idx);
    }

    public void addUnknownThread(PSRecord psr) {
        mUnknownThreads.add(psr);
    }

    public int getUnknownThreadCount() {
        return mUnknownThreads.size();
    }

    public PSRecord getUnknownThread(int idx) {
        return mUnknownThreads.get(idx);
    }

    @Override
    public Iterator<StackTrace> iterator() {
        return mStacks.iterator();
    }

    public Chapter getChapter() {
        return mChapter;
    }

}