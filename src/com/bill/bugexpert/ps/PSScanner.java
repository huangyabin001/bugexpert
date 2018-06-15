package com.bill.bugexpert.ps;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.bill.bugexpert.BugExpertModule;
import com.bill.bugexpert.ProcessRecord;
import com.bill.bugexpert.Section;

public class PSScanner {

    private BugExpertModule mBr;
    private static final String HEADER_1 = "USER     PID   PPID  VSIZE  RSS    PCY  WCHAN    PC         NAME";
    private static final Pattern PATTERN_1 = Pattern.compile("([a-z0-9_]+) *([0-9]+) *([0-9]+) *([0-9]+) *([0-9]+) *(fg|bg|un)?  ([0-9-a-f]{8}) ([0-9-a-f]{8}) (.) (.*)");
    private static final String HEADER_2 = "USER     PID   PPID  VSIZE  RSS   PRIO  NICE  RTPRI SCHED  PCY  WCHAN    PC         NAME";
    private static final Pattern PATTERN_2 = Pattern.compile("([a-z0-9_]+) *([0-9]+) *([0-9]+) *([0-9]+) *([0-9]+) *([0-9-]+) *([0-9-]+) *([0-9-]+) *([0-9-]+) *(fg|bg|un)?  ([0-9-a-f]{8}) ([0-9-a-f]{8}) (.) (.*)");

    public PSScanner(BugExpertModule br) {
        mBr = br;
    }

    public PSRecords run() {
        PSRecords ret = readPS(Section.PROCESSES_AND_THREADS);
        if (ret == null) {
            ret = readPS(Section.PROCESSES);
        }
        return ret;
    }

    private PSRecords readPS(String sectionName) {
        Section ps = mBr.findSection(sectionName);
        if (ps == null) {
            mBr.logE("Cannot find section: " + sectionName + " (ignoring it)");
            return null;
        }

        // Process the PS section
        PSRecords ret = new PSRecords();
        Pattern p = null;
        int lineIdx = 0, idxPid = -1, idxPPid = -1, idxPcy = -1, idxName = -1, idxNice = -1;
        for (int tries = 0; tries < 10 && lineIdx < ps.getLineCount(); tries++) {
            String buff = ps.getLine(lineIdx++);
            if (buff.equals(HEADER_1)) {
                p = PATTERN_1;
                idxPid = 2;
                idxPPid = 3;
                idxPcy = 6;
                idxName = 10;
                break;
            }
            if (buff.equals(HEADER_2)) {
                p = PATTERN_2;
                idxPid = 2;
                idxPPid = 3;
                idxNice = 7;
                idxPcy = 10;
                idxName = 14;
                break;
            }
        }
        if (p == null) {
            mBr.logE("Could not find header in ps output");
            return null;
        }

        // Now read and process every line
        int pidZygote = -1;
        int cnt = ps.getLineCount();
        for (int i = lineIdx; i < cnt; i++) {
            String buff = ps.getLine(i);
            if (buff.startsWith("[")) break;
            Matcher m = p.matcher(buff);
            if (!m.matches()) {
                mBr.logE("Error parsing line: " + buff);
                continue;
            }

            int pid = -1;
            if (idxPid >= 0) {
                String sPid = m.group(idxPid);
                try {
                    pid = Integer.parseInt(sPid);
                } catch (NumberFormatException nfe) {
                    mBr.logE("Error parsing pid from: " + sPid);
                    break;
                }
            }

            // Extract ppid
            int ppid = -1;
            if (idxPPid >= 0) {
                String sPid = m.group(idxPPid);
                try {
                    ppid = Integer.parseInt(sPid);
                } catch (NumberFormatException nfe) {
                    mBr.logE("Error parsing ppid from: " + sPid);
                    break;
                }
            }

            // Extract nice
            int nice = PSRecord.NICE_UNKNOWN;
            if (idxNice >= 0) {
                String sNice = m.group(idxNice);
                try {
                    nice = Integer.parseInt(sNice);
                } catch (NumberFormatException nfe) {
                    mBr.logE("Error parsing nice from: " + sNice);
                    break;
                }
            }

            // Extract scheduler policy
            int pcy = PSRecord.PCY_UNKNOWN;
            if (idxPcy >= 0) {
                String sPcy = m.group(idxPcy);
                if ("fg".equals(sPcy)) {
                    pcy = PSRecord.PCY_NORMAL;
                } else if ("bg".equals(sPcy)) {
                    pcy = PSRecord.PCY_BATCH;
                } else if ("un".equals(sPcy)) {
                    pcy = PSRecord.PCY_FIFO;
                } else {
                    pcy = PSRecord.PCY_OTHER;
                }
            }

            // Exctract name
            String name = "";
            if (idxName >= 0) {
                name = m.group(idxName);
            }

            // Fix the name
            ret.put(pid, new PSRecord(pid, ppid, nice, pcy, name));

            // Check if we should create a ProcessRecord for this
            if (pidZygote == -1 && name.equals("zygote")) {
                pidZygote = pid;
            }
            ProcessRecord pr = mBr.getProcessRecord(pid, true, false);
            pr.suggestName(name, 10);
        }

        // Build tree structure as well
        for (PSRecord psr : ret) {
            int ppid = psr.mPPid;
            PSRecord parent = ret.getPSRecord(ppid);
            if (parent == null) {
                parent = ret.getPSTree();
            }
            parent.mChildren.add(psr);
            psr.mParent = parent;
        }

        return ret;
    }

}
