package com.bill.bugexpert.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegExpUtil {

	// e.g "== dumpstate: 2018-01-30 08:35:52"
	public static final String DUMPSTATE = "(={2})\\s+(dumpstate:)\\s+(\\d{4}-\\d{2}-\\d{2})\\s+(\\d{2}:\\d{2}:\\d{2})";

	// e.g "------ SHOW MAP 620 (/system/bin/wtemonitor) (/system/xbin/su root
	// showmap -q 620) ------";
	public static final String SECTION = "(-{6})\\s+([A-Z]+)\\s+(.+)(-{6})";

	// e.g "------ 0.085s was the duration of 'SHOW MAP 620
	// (/system/bin/wtemonitor)' ------";
	public static final String SECTION_DURATION = "(-{6})\\s+(\\d+\\.\\d{3}s)\\s+(was the duration of)(.+)(-{6})";

	// e.g "SHOW MAP";
	public static final String SHOW_MAP = "(SHOW)\\s+(MAP)";

	// e.g "LABEL USER PID TID PPID VSZ RSS WCHAN ADDR S PRI NI RTPRIO SCH PCY CMD";
	public static final String PROCESSES_THREADS = "(LABEL)\\s+(USER)\\s+(PID)\\s+(TID)\\s+(PPID)\\s+(VSZ)\\s+(RSS)\\s+(WCHAN)\\s+(ADDR)\\s+(S)\\s+(PRI)\\s+(NI)\\s+(RTPRIO)\\s+(SCH)\\s+(PCY)\\s+(CMD)";

	// e.g "u:r:init:s0 root 1 1 0 15256 1440 SyS_epoll_wait 0 S 19 0 - 0 fg init"
	public static final String PROCESSES_THREADS_DATA = "(\\S+:\\S+:\\S+:\\S+)\\s+(\\S+)\\s+([0-9]+)\\s+([0-9]+)\\s+([0-9]+)\\s+([0-9]+)\\s+([0-9]+)\\s+(\\S+)\\s+(\\S+)\\s+(\\S+)\\s+([0-9]+)\\s+(-*[0-9]+)\\s+(\\S+)\\s+([0-9]+)\\s+(\\S+)\\s+(\\S+)";

	// e.g " 01:14:09 up 4 days, 18:00, 0 users, load average: 2.00, 2.01, 2.05"
	// 01:14:09 //current time of the system.
	// up 4 days, 18:00 //run time of the system¡£
	// 12 user //number of user connections.
	// load average // system average load.
	public static final String UPTIME = "\\s+(\\d{2}:\\d{2}:\\d{2})\\s+(up)\\s+([0-9]+\\s+days,\\s+){0,1}(\\d{1,2}:\\d{1,2},\\s+){0,1}([0-9])\\s+(users,)\\s+(load)\\s+(average:)\\s+([0-9]+.[0-9]+,)\\s+([0-9]+.[0-9]+,)\\s+([0-9]+.[0-9]+)";
	// e.g "4 days, "
	public static final String UPTIME_DAYS = "([0-9]+)\\s+days,\\s+";
	// e.g "18:00, "
	public static final String UPTIME_TIMES = "(\\d{1,2}):(\\d{1,2}),\\s+";

	// e.g "----- pid 329 at 2018-01-30 08:36:04 -----"
	public static final String VM_TRACES_PID = "-{5}\\s+pid\\s+(\\d+)\\s+at\\s+(\\d{4}-\\d{2}-\\d{2})\\s+(\\d{2}:\\d{2}:\\d{2})\\s+-{5}";
	// e.g "----- end 329 -----"
	public static final String VM_TRACES_PID_END = "-{5}\\s+end\\s+(\\d+)\\s+-{5}";

	// e.g "Cmd line: android.process.media"
	public static final String VM_TRACES_PID_CMDLINE = "Cmd\\s+line:\\s+(\\S*)";

	// e.g "\"main\" daemon prio=5 tid=1 NATIVE"
	public static final String VM_TRACES_THREAD = "\"(\\S*)\"\\s+\\S*\\s+{0,1}prio=(\\d+)\\s+tid=(\\d+)\\s(\\S*)";

	// e.g "\"provider@2.4-se\" sysTid=344"
	public static final String VM_TRACES_THREAD_NATIVE = "\"(\\S*)\"\\s+sysTid=(\\d+)";
	
	public static boolean isMatch(String pattern, String str) {
		// e.g
		// String s = "== dumpstate: 2018-01-30 08:35:52";
		// String p =
		// "(={2})\\s+(dumpstate:)\\s+(\\d{4}-\\d{2}-\\d{2})\\s+(\\d{2}:\\d{2}:\\d{2})";
		// "------ SHOW MAP 620 (/system/bin/wtemonitor) (/system/xbin/su root showmap
		// -q 620) ------"
		// "------ 0.085s was the duration of 'SHOW MAP 620 (/system/bin/wtemonitor)'
		// ------"

		// Craeat a pattern Object.
		Pattern r = Pattern.compile(pattern);

		// Create a matcher Object.
		Matcher m = r.matcher(str);
		if (m.find()) {
			for (int i = 0; i <= m.groupCount(); i++) {
				// System.out.println("Found value: " + m.group(i));
			}
			return true;
		} else {
			// System.out.println(str + " is NO MATCH");
			return false;
		}
	}

	public static Matcher getMatch(String pattern, String str) {
		Pattern r = Pattern.compile(pattern);

		// Create a matcher Object.
		Matcher m = r.matcher(str);
		if (m.find()) {
			for (int i = 0; i <= m.groupCount(); i++) {
				// System.out.println("Found value: " + m.group(i));
			}
			return m;
		} else {
			// System.out.println(str + " is NO MATCH");
			return null;
		}
	}
}
