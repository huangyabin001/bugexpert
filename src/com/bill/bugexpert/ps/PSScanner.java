package com.bill.bugexpert.ps;

import java.util.regex.Matcher;

import com.bill.bugexpert.BugExpertModule;
import com.bill.bugexpert.ProcessRecord;
import com.bill.bugexpert.Section;
import com.bill.bugexpert.util.RegExpUtil;

public class PSScanner {

	private BugExpertModule mBr;

	public PSScanner(BugExpertModule br) {
		mBr = br;
	}

	public PSRecords run() {
		mBr.logI("Parsing PROCESSES AND THREADS");
		PSRecords ret = readPS(Section.PROCESSES_AND_THREADS);
		if (ret == null) {
			mBr.logD("Can't not readPS PROCESSES_AND_THREADS.");
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
		int lineIdx = 0, idxLabel = -1, idxUser = -1, idxPid = -1, idxTid = -1, idxPPid = -1, idxNice = -1, idxPcy = -1,
				idxCmd = -1;
		for (int tries = 0; tries < 10 && lineIdx < ps.getLineCount(); tries++) {
			String buff = ps.getLine(lineIdx++);
			if (RegExpUtil.isMatch(RegExpUtil.PROCESSES_THREADS_PATTERN, buff)) {
				idxLabel = 1;
				idxUser = 2;
				idxPid = 3;
				idxTid = 4;
				idxPPid = 5;
				idxNice = 12;
				idxPcy = 15;
				idxCmd = 16;
				break;
			}
		}

		// Now read and process every line
		int pidZygote = -1;
		int cnt = ps.getLineCount();
		for (int i = lineIdx; i < cnt; i++) {
			String buff = ps.getLine(i);
			if (buff.startsWith("["))
				break;
			if (RegExpUtil.isMatch(RegExpUtil.SECTION_DURATION_PATTERN, buff)) {
				mBr.logW("parsing line: " + buff + " ,ignoring...");
				continue;
			}
			Matcher m = RegExpUtil.getMatch(RegExpUtil.PROCESSES_THREADS_DATA_PATTERN, buff);
			if (m == null) {
				mBr.logE("Error parsing line: " + buff);
				continue;
			}

			// Exctract label
			String label = null;
			if (idxLabel >= 0) {
				label = m.group(idxLabel);
			}

			// Exctract user
			String user = null;
			if (idxUser >= 0) {
				user = m.group(idxUser);
			}

			// Extract pid
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

			// Extract tid
			int tid = -1;
			if (idxTid > 0) {
				String sTid = m.group(idxTid);
				try {
					tid = Integer.parseInt(sTid);
				} catch (NumberFormatException nfe) {
					mBr.logE("Error parsing tid from: " + sTid);
					break;
				}
			}

			// Extract ppid
			int ppid = -1;
			if (idxPPid > 0) {
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
			if (idxNice > 0) {
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
			if (idxPcy > 0) {
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

			// Exctract cmd
			String cmd = "";
			if (idxCmd >= 0) {
				cmd = m.group(idxCmd);
			}

			// Fix the name
			if (cmd != null && label != null && user != null)
				ret.put(pid, new PSRecord(pid, ppid, nice, pcy, cmd, label, user, tid));

			// Check if we should create a ProcessRecord for this
			if (pidZygote == -1 && cmd.equals("zygote")) {
				pidZygote = pid;
			}
			ProcessRecord pr = mBr.getProcessRecord(pid, true, false);
			pr.suggestName(cmd, 10);
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
