package com.bill.bugexpert.plugins.stacktrace;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import com.bill.bugexpert.BugExpertModule;
import com.bill.bugexpert.Section;
import com.bill.bugexpert.util.RegExpUtil;

/**
 * This class is responsible to scan the stack trace output and collect the data
 */
/* package */ final class StackTraceScanner {

	private static final int STATE_PROC = 0;
	private static final int STATE_THREAD = 1;
	private static final int STATE_STACK = 2;

	public StackTraceScanner(StackTracePlugin stackTracePlugin) {
	}

	public Processes scan(BugExpertModule br, int id, Section sec, String chapterName) {
		br.logI("StackTraceScanner:scan(), scanning...");
		int cnt = sec.getLineCount();
		int state = STATE_PROC;
		Processes processes = new Processes(br, id, chapterName, sec.getName());
		Process curProc = null;
		StackTrace curStackTrace = null;
		for (int i = 0; i < cnt; i++) {
			String buff = sec.getLine(i);
			Matcher m0 = RegExpUtil.getMatch(RegExpUtil.VM_TRACES_THREAD_STACKS0, buff);// #
			Matcher m1 = RegExpUtil.getMatch(RegExpUtil.VM_TRACES_THREAD_STACKS1, buff);// |
			Matcher m2 = RegExpUtil.getMatch(RegExpUtil.VM_TRACES_THREAD_STACKS2, buff);// native
			Matcher m2_1 = RegExpUtil.getMatch(RegExpUtil.VM_TRACES_THREAD_STACKS2_1, buff);// native
			Matcher m3 = RegExpUtil.getMatch(RegExpUtil.VM_TRACES_THREAD_STACKS3, buff);// at
			Matcher m3_1 = RegExpUtil.getMatch(RegExpUtil.VM_TRACES_THREAD_STACKS3_1, buff);// at
			Matcher m4 = RegExpUtil.getMatch(RegExpUtil.VM_TRACES_THREAD_STACKS4, buff);// (
			Matcher m5 = RegExpUtil.getMatch(RegExpUtil.VM_TRACES_THREAD_STACKS5, buff);// -
			switch (state) {
			case STATE_PROC:
				Matcher mInit = RegExpUtil.getMatch(RegExpUtil.VM_TRACES_PID, buff);
				if (mInit != null) {
					state = STATE_THREAD;
					int pid = Integer.parseInt(mInit.group(1));
					String data = mInit.group(2);
					String time = mInit.group(3);
					curProc = new Process(br, processes, pid, data, time);
					// br.logI("pid = " + pid + ", data = " + data + ", time = " + time + ".");
					processes.add(curProc);
				}
				break;
			case STATE_THREAD:
				// "Cmd line: android.process.media"
				Matcher cmdM = RegExpUtil.getMatch(RegExpUtil.VM_TRACES_PID_CMDLINE, buff);
				// "\"main\" prio=5 tid=1 Native"
				Matcher threadM = RegExpUtil.getMatch(RegExpUtil.VM_TRACES_THREAD2, buff);
				// "\"main\" daemon prio=5 tid=1 NATIVE"
				Matcher threadM_ = RegExpUtil.getMatch(RegExpUtil.VM_TRACES_THREAD, buff);
				// "\"provider@2.4-se\" sysTid=344"
				Matcher threadM1 = RegExpUtil.getMatch(RegExpUtil.VM_TRACES_THREAD1, buff);
				// "\"RenderThread\" prio=5 (not attached)
				Matcher threadM3 = RegExpUtil.getMatch(RegExpUtil.VM_TRACES_THREAD3, buff);
				// e.g "----- end 329 -----"
				Matcher endM = RegExpUtil.getMatch(RegExpUtil.VM_TRACES_PID_END, buff);

				if (endM != null) {
					curProc = null;
					state = STATE_PROC;
				} else if (cmdM != null) {
					curProc.setName(cmdM.group(1));
					// br.logI("STATE_PROC::cmdline = " + cmdM.group(1) + ".");
				} else if (threadM != null || threadM1 != null || threadM3 != null) {
					state = STATE_STACK;
					String separator = "---";
					if (threadM != null) {
						if (threadM_ != null) {
							// "\"main\" daemon prio=5 tid=1 NATIVE"
							String threadName = separator + threadM_.group(1);
							int prio = Integer.parseInt(threadM_.group(3));
							int tid = Integer.parseInt(threadM_.group(4));
							String threadState = threadM_.group(5);

							curStackTrace = new StackTrace(curProc, threadName, tid, prio, threadState);
							curProc.addStackTrace(curStackTrace);
						} else {
							// "\"main\" prio=5 tid=1 Native"
							String threadName2 = separator + threadM.group(1);
							int prio2 = Integer.parseInt(threadM.group(2));
							int tid2 = Integer.parseInt(threadM.group(3));
							String threadState2 = threadM.group(4);

							curStackTrace = new StackTrace(curProc, threadName2, tid2, prio2, threadState2);
							curProc.addStackTrace(curStackTrace);
						}
					} else if (threadM1 != null) {
						// "bluetooth@1.0-s" sysTid=343
						String threadName1 = separator + threadM1.group(1);
						String sysTid1 = threadM1.group(2);
						curStackTrace = new StackTrace(curProc, threadName1, -1, -1, "?");
						curProc.addStackTrace(curStackTrace);
						if (sysTid1 != null) {
							curStackTrace.parseProperties(sysTid1);
						}
					} else if (threadM3 != null) {
						// "\"RenderThread\" prio=5 (not attached)
						String threadName3 = separator + threadM3.group(1);
						int prio3 = Integer.parseInt(threadM3.group(2));
						String threadState3 = threadM3.group(3);

						curStackTrace = new StackTrace(curProc, threadName3, prio3, -1, threadState3);
						curProc.addStackTrace(curStackTrace);
					}
				}
				break;
			case STATE_STACK:
				if (m0 != null) {// #
					try {
						String pc = m0.group(1);
						String fileName = m0.group(2);
						String method = m0.group(3);
						int methodOffset = Integer.parseInt(m0.group(4));
						StackTraceItem item = new StackTraceItem(pc, fileName, method, methodOffset);
						curStackTrace.addStackTraceItem(item);
					} catch (Exception e) {
						br.logE("StackTraceScanner[#]::scan; parse [" + buff + "] error!s");
						e.printStackTrace();
					}
				} else if (m1 != null) {// |
					// Parse the extra properties
					curStackTrace.parseProperties(buff);
					// br.logI("m1_buff = " + buff);
				} else if (m2 != null | m2_1 != null) {// native
					br.logI("m2_buff = " + buff);
					try {
						String pc = "unknown";
						String method = "unknown";
						String fileName = "unknown";
						int methodOffset = 0;
						if (m2 != null) {
							pc = m2.group(1);
							fileName = m2.group(2);
							method = m2.group(3);
							methodOffset = Integer.parseInt(m2.group(4));
							StackTraceItem item = new StackTraceItem(pc, fileName, method, methodOffset);
							curStackTrace.addStackTraceItem(item);
						} else if (m2_1 != null) {
							pc = m2_1.group(1);
							fileName = m2_1.group(2);
							StackTraceItem item = new StackTraceItem(pc, fileName);
							curStackTrace.addStackTraceItem(item);
						} else {
							br.logE("StackTraceScanner[native]::scan; parse [" + buff + "] error!s");
						}
					} catch (Exception e) {
						br.logE("StackTraceScanner[native]::scan; parse [" + buff + "] error!s");
						e.printStackTrace();
					}
				} else if (m3 != null || m3_1 != null) {// at
					String method = "unknown";
					String fileName = "unknown";
					int line = 0;

					try {
						if (m3 != null) {
							fileName = m3.group(1);
							method = m3.group(2);
							line = Integer.parseInt(m3.group(3));
							StackTraceItem item = new StackTraceItem(method, fileName, line);
							curStackTrace.addStackTraceItem(item);
						} else if (m3_1 != null) {
							fileName = m3_1.group(1);
							method = m3_1.group(2);
							StackTraceItem item = new StackTraceItem(method, fileName, line);
							curStackTrace.addStackTraceItem(item);
						} else {
							br.logE("StackTraceScanner[at]::scan; parse [" + buff + "] error!s");
						}
					} catch (Exception e) {
						br.logE("StackTraceScanner[at]::scan; parse [" + buff + "] error!s");
						e.printStackTrace();
					}
				} else if (m4 != null) {// ()
					// do nothing.
				} else if (m5 != null) {// -
					StackTraceItem item = new StackTraceItem("", buff, 0);
					curStackTrace.addStackTraceItem(item);
					processWaitingToLockLine(curStackTrace, buff);
				} else {
					state = STATE_THREAD;
					curStackTrace = null;
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
