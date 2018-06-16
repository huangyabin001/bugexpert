package com.bill.bugexpert.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegExpUtil {
	// e.g "== dumpstate: 2018-01-30 08:35:52"
	public static final String DUMPSTATE_PATTERN = "(={2})\\s+(dumpstate:)\\s+(\\d{4}-\\d{2}-\\d{2})\\s+(\\d{2}:\\d{2}:\\d{2})";

	// e.g "------ SHOW MAP 620 (/system/bin/wtemonitor) (/system/xbin/su root
	// showmap -q 620) ------";
	public static final String SECTION_PATTERN = "(-{6})\\s+([A-Z]+)\\s+(.+)(-{6})";

	// e.g "------ 0.085s was the duration of 'SHOW MAP 620
	// (/system/bin/wtemonitor)' ------";
	public static final String SECTION_DURATION_PATTERN = "(-{6})\\s+(\\d+\\.\\d{3}s)\\s+(was the duration of)(.+)(-{6})";

	// e.g "SHOW MAP";
	public static final String SHOW_MAP = "(SHOW)\\s+(MAP)";

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
}
