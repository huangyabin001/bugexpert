/*
 * Copyright (C) 2012-2013 Sony Mobile Communications AB
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
package com.bill.bugexpert.plugins.logs.kernel;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.bill.bugexpert.BugExpertModule;
import com.bill.bugexpert.Section;
import com.bill.bugexpert.doc.Block;
import com.bill.bugexpert.doc.Bug;
import com.bill.bugexpert.doc.Chapter;
import com.bill.bugexpert.doc.DocNode;
import com.bill.bugexpert.doc.Hint;
import com.bill.bugexpert.doc.Link;
import com.bill.bugexpert.doc.WebOnlyChapter;
import com.bill.bugexpert.plugins.logs.LogData;
import com.bill.bugexpert.plugins.logs.LogLine;
import com.bill.bugexpert.plugins.logs.LogLines;
import com.bill.bugexpert.plugins.logs.LogToolbar;
import com.bill.bugexpert.plugins.logs.PidDecorator;
import com.bill.bugexpert.plugins.logs.kernel.iptables.IPTableLogAnalyzer;

public class KernelLogData implements LogData {

    private static final Pattern SELECT_TO_KILL = Pattern.compile(".*] select (\\d+) .*, to kill$");
    private static final Pattern SEND_SIGKILL = Pattern.compile(".*] send sigkill to (\\d+) .*");
    private static final Pattern BINDER_RELEASE_NOT_FREED = Pattern.compile(".*] binder: release proc (\\d+)" +
            ", transaction (\\d+), not freed$");

    private Chapter mCh;
    private boolean mLoaded = false;
    private LogLines mParsedLog = new LogLines();
    private int mUnparsed;
    private PMStats mPMStats;
    private String mId;
    private BugExpertModule mMod;
    private String mInfoId;

    public KernelLogData(BugExpertModule mod, Section section, String chapterName, String id, String infoId) {
        mMod = mod;
        mId = id;
        mInfoId = infoId;
        mPMStats = new PMStats(this, mod);
        mCh = new Chapter(mod.getContext(), chapterName);

        if (section != null) {
            // Load and parse the lines
            int cnt = section.getLineCount();
            for (int i = 0; i < cnt; i++) {
                addLine(mod, section.getLine(i), -1);
            }
        }
    }

    protected void addLine(BugExpertModule mod, String line, long realTs) {
        LogLine kl = new LogLine(mod, line, LogLine.FMT_KERNEL, null);
        kl.realTs = realTs;
        if (kl.ok) {
            mParsedLog.add(kl);
        } else {
            mUnparsed++;
        }
    }

    public String getId() {
        return mId;
    }

    public boolean finishLoad() {
        int cnt = mParsedLog.size();

        // Annotate the log
        for (int i = 0; i < cnt; i++) {
            LogLine kl = mParsedLog.get(i);
            annotate(kl, mMod, i);
        }

        // Analyze the log
        for (int i = 0; i < cnt;) {
            LogLine kl = mParsedLog.get(i);
            i += analyze(kl, i, mMod);
        }

        mPMStats.load();

        // Load successful
        mLoaded = true;

        mMod.addInfo(mInfoId, mParsedLog);
        if (null != mCh) {
            mCh.addChapter(new WebOnlyChapter(mCh.getContext(), "Log (editable)", mInfoId + "$log"));
        }

        return mLoaded;
    }

    public int getLineCount() {
        return mParsedLog.size();
    }

    public LogLine getLine(int i) {
        return mParsedLog.get(i);
    }

    @Override
    public String getInfoId() {
        return mInfoId;
    }

    @Override
    public int size() {
        return mParsedLog.size();
    }

    @Override
    public LogLine get(int i) {
        return mParsedLog.get(i);
    }

    public void generate(BugExpertModule br) {
        if (!mLoaded || getLineCount() == 0) {
            return;
        }

        if (mUnparsed > 0) {
            new Hint(mCh).add("NOTE: " + mUnparsed + " lines were not parsed");
        }

        br.addChapter(mCh);
        generateLog(br);
        mPMStats.generate(br, mCh);
        new DeepSleepDetector(this, mMod, mParsedLog).run();
        new IPTableLogAnalyzer(this, mMod, mParsedLog).run();
    }

    private void generateLog(BugExpertModule br) {
        Chapter ch = new Chapter(br.getContext(), "Log");
        mCh.addChapter(ch);
        new LogToolbar(ch);
        DocNode log = new Block(ch).addStyle("log");
        int cnt = mParsedLog.size();
        for (int i = 0; i < cnt; i++) {
            LogLine kl = mParsedLog.get(i);
            log.add(kl);
        }
    }

    /**
     * Analyze a log line to see if it can be annotated with further information, such as links to
     * referenced data.
     */
    private void annotate(LogLine kl, BugExpertModule br, int i) {
        annotateSelectToKill(kl, br);
        annotateSendSigkill(kl, br);
        annotateBinderReleaseNotFreed(kl, br);
    }

    /**
     * Link the pid to its process record in a "select to kill" line.
     *
     * select 13814 (android.support), adj 8, size 5977, to kill
     */
    private void annotateSelectToKill(LogLine kl, BugExpertModule br) {
        Matcher matcher = SELECT_TO_KILL.matcher(kl.line);
        if (!matcher.matches()) {
            return;
        }

        int start = matcher.start(1);
        int end = matcher.end(1);
        markPid(kl, br, start, end);
    }

    /**
     * Link the pid to its process record in a "send sigkill" line.
     *
     * send sigkill to 8506 (et.digitalclock), adj 10, size 5498
     */
    private void annotateSendSigkill(LogLine kl, BugExpertModule br) {
        Matcher matcher = SEND_SIGKILL.matcher(kl.line);
        if (!matcher.matches()) {
            return;
        }

        int start = matcher.start(1);
        int end = matcher.end(1);
        markPid(kl, br, start, end);
    }

    /**
     * Link the pid to its process record in a "binder: release not freed" line.
     *
     * binder: release proc 9107, transaction 1076363, not freed
     */
    private void annotateBinderReleaseNotFreed(LogLine kl, BugExpertModule br) {
        Matcher matcher = BINDER_RELEASE_NOT_FREED.matcher(kl.line);
        if (!matcher.matches()) {
            return;
        }

        int start = matcher.start(1);
        int end = matcher.end(1);
        markPid(kl, br, start, end);
    }

    private void markPid(LogLine kl, BugExpertModule br, int start, int end) {
        kl.addDecorator(new PidDecorator(start, end, br, Integer.parseInt(kl.line.substring(start, end))));
    }

    /**
     * Analyze the log starting from line 'i' to see if it contain any
     * information that will result in a Bug.
     *
     * Return the number of lines consumed.
     */
    private int analyze(LogLine kl, int i, BugExpertModule br) {
        int inc = analyzeFatal(kl, br, i);
        if (inc > 0) return inc;


        return 1;
    }

    /**
     * Generate a Bug for each block of log lines with a level of 1 or 2.
     */
    private int analyzeFatal(LogLine kl, BugExpertModule br, int i) {
        // Put a marker box
        String type;
        int level = kl.level;
        switch (level) {
            case 1:
                type = "EMERGENCY";
                break;
            case 2:
                type = "ALERT";
                break;
            default:
                return 1;
        }
        kl.addMarker("log-float-err", "KERNEL<br/>" + type, null);

        // Create a bug and store the relevant log lines
        Bug bug = new Bug(Bug.Type.PHONE_ERR, Bug.PRIO_ALERT_KERNEL_LOG, kl.ts, "KERNEL " + type);
        new Block(bug).add(new Link(kl.getAnchor(), "(link to log)"));
        DocNode log = new Block(bug).addStyle("log");
        log.add(kl.symlink());
        int end = i + 1;
        while (end < mParsedLog.size()) {
            LogLine extra = mParsedLog.get(end);
            if (extra.level != level)
                break;
            log.add(extra.symlink());
            end++;
        }
        bug.setAttr(Bug.ATTR_FIRST_LINE, i);
        bug.setAttr(Bug.ATTR_LAST_LINE, end);
        bug.setAttr(Bug.ATTR_LOG_INFO_ID, mInfoId);
        br.addBug(bug);

        return end - i;
    }

    public void addChapter(Chapter ch) {
        mCh.addChapter(ch);
    }

}
