package com.bill.bugexpert.plugins.stacktrace;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.bill.bugexpert.BugExpertModule;
import com.bill.bugexpert.Section;
import com.bill.bugexpert.util.RegExpUtil;

/**
 * This class is responsible to scan the stack trace output and collect the data
 */
/* package */ final class StackTraceScanner {

	private static final int STATE_INIT = 0;
	private static final int STATE_PROC = 1;
	private static final int STATE_STACK = 2;

	public StackTraceScanner(StackTracePlugin stackTracePlugin) {
	}

	public Processes scan(BugExpertModule br, int id, Section sec, String chapterName) {
		br.logI("StackTraceScanner:scan(), scanning...");
		Pattern pNat = Pattern.compile("  #..  pc (........)  ([^() ]+)(?: \\((.*)\\+(.*)\\))?");
		Pattern pNatAlt = Pattern.compile("  #..  pc (........)  ([^() ]+) \\(deleted\\)");
		int cnt = sec.getLineCount();
		int state = STATE_INIT;
		Processes processes = new Processes(br, id, chapterName, sec.getName());
		Process curProc = null;
		StackTrace curStackTrace = null;

		for (int i = 0; i < cnt; i++) {
			String buff = sec.getLine(i);
			switch (state) {
			case STATE_INIT:
				Matcher mInit = RegExpUtil.getMatch(RegExpUtil.VM_TRACES_PID, buff);
				if (mInit != null) {
					state = STATE_PROC;
					int pid = Integer.parseInt(mInit.group(1));
					String data = mInit.group(2);
					String time = mInit.group(3);
					curProc = new Process(br, processes, pid, data, time);
					processes.add(curProc);
				}
				break;
			case STATE_PROC:
				Matcher cmdM = RegExpUtil.getMatch(RegExpUtil.VM_TRACES_PID_CMDLINE, buff);
				Matcher threadM = RegExpUtil.getMatch(RegExpUtil.VM_TRACES_THREAD, buff);
				Matcher threadMN = RegExpUtil.getMatch(RegExpUtil.VM_TRACES_THREAD_NATIVE, buff);
				if (RegExpUtil.isMatch(RegExpUtil.VM_TRACES_PID_END, buff)) {
					curProc = null;
					state = STATE_INIT;
				} else if (cmdM != null) {
					curProc.setName(cmdM.group(1));
				} else if (threadM != null || threadMN != null) {
					// "Binder:329_4" prio=5 tid=15 Native
					state = STATE_STACK;
					if (threadM != null) {
						String threadName = threadM.group(1);
						int prio = Integer.parseInt(threadM.group(2));
						int tid = Integer.parseInt(threadM.group(3));
						String threadState = threadM.group(4);

						curStackTrace = new StackTrace(curProc, threadName, tid, prio, threadState);
						br.logI(curStackTrace.toString());
						curProc.addStackTrace(curStackTrace);
					} else if (threadMN != null) {
						// "bluetooth@1.0-s" sysTid=343
						String threadName = threadMN.group(1);
						String sysTid = threadMN.group(2);
						curStackTrace = new StackTrace(curProc, threadName, -1, -1, "?");
						curProc.addStackTrace(curStackTrace);
						if (sysTid != null) {
							curStackTrace.parseProperties(sysTid);
						}
					}
				}
				break;
			case STATE_STACK:
				if (!buff.startsWith("  ")) {
					state = STATE_PROC;
					curStackTrace = null;
				} else if (buff.startsWith("  | ")) {
					// Parse the extra properties
					curStackTrace.parseProperties(buff.substring(4));
				} else if (buff.startsWith("  - ")) {
					buff = buff.substring(4);
					StackTraceItem item = new StackTraceItem("", buff, 0);
					curStackTrace.addStackTraceItem(item);
					if (buff.startsWith("waiting ")) {
						processWaitingToLockLine(curStackTrace, buff);
					}
				} else if (buff.startsWith("  at ")) {
					int idx0 = buff.indexOf('(');
					int idx1 = buff.indexOf(':');
					int idx2 = buff.indexOf(')');
					if (idx0 >= 0 && idx2 >= 0 && idx2 > idx0) {
						String method = buff.substring(5, idx0);
						String fileName = null;
						int line = -1;
						if (idx1 >= 0 && idx1 > idx0 && idx2 > idx1) {
							fileName = buff.substring(idx0 + 1, idx1);
							String lineS = buff.substring(idx1 + 1, idx2);
							if (lineS.startsWith("~")) {
								lineS = lineS.substring(1);
							} else if (lineS.lastIndexOf(':') > 0) {
								int position = lineS.lastIndexOf(':');
								lineS = lineS.substring(position + 1);
							}
							line = Integer.parseInt(lineS);
						}
						StackTraceItem item = new StackTraceItem(method, fileName, line);
						curStackTrace.addStackTraceItem(item);
					}
				} else if (buff.startsWith("  #")) {
					Matcher m = pNat.matcher(buff);
					if (!m.matches()) {
						m = pNatAlt.matcher(buff);
					}
					if (!m.matches()) {
						br.logE("Cannot parse line: " + buff);
						continue;
					}
					long pc = Long.parseLong(m.group(1), 16);
					String fileName = m.group(2);
					String method = (m.groupCount() >= 3) ? m.group(3) : null;
					int methodOffset = (method == null) ? -1 : Integer.parseInt(m.group(4));
					StackTraceItem item = new StackTraceItem(pc, fileName, method, methodOffset);
					curStackTrace.addStackTraceItem(item);
				}
			}

		}
		br.logD("StackTraceScanner:scan(), Processes's size is " + processes.size());
		return processes;
	}

	private void processWaitingToLockLine(StackTrace curStackTrace, String buff) {
		int idx = -1;
		String needle = "";
		for (String possibleNeedle : getPossibleWaitingNeedles()) {
			idx = buff.indexOf(possibleNeedle);
			if (idx > 0) {
				needle = possibleNeedle;
				break;
			}
		}
		if (idx > 0) {
			idx += needle.length();
			int idx2 = buff.indexOf(' ', idx);
			if (idx2 < 0) {
				idx2 = buff.length();
			}
			if (idx2 > 0) {
				int tid = Integer.parseInt(buff.substring(idx, idx2));
				if (tid != curStackTrace.getTid()) {
					String lockId = buff.substring(buff.indexOf("<") + 1, buff.indexOf(">"));
					String lockType = buff.substring(buff.indexOf("(") + 1, buff.indexOf(")"));
					curStackTrace.setWaitOn(new StackTrace.WaitInfo(tid, lockId, lockType));
				}
			}
		}
	}

	private Iterable<String> getPossibleWaitingNeedles() {
		List<String> list = new ArrayList<String>();
		list.add("held by threadid=");
		list.add("held by tid=");
		list.add("held by thread ");
		return list;
	}

}
