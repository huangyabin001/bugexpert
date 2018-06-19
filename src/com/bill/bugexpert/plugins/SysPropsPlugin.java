package com.bill.bugexpert.plugins;

import java.util.HashMap;
import java.util.regex.Matcher;

import com.bill.bugexpert.BugExpertModule;
import com.bill.bugexpert.Module;
import com.bill.bugexpert.Plugin;
import com.bill.bugexpert.Section;
import com.bill.bugexpert.util.RegExpUtil;

public class SysPropsPlugin extends Plugin {

	private static final String TAG = "[SysPropsPlugin]";
	private HashMap<String, String> mMap = new HashMap<String, String>();

	private long mUpTime;

	@Override
	public int getPrio() {
		return 1; // Need to execute first
	}

	public long getUpTime() {
		return mUpTime;
	}

	@Override
	public void reset() {
		// NOP
	}

	@Override
	public void load(Module rep) {
		mMod.logD("SysPropsPlugin:load(),loading...");
		BugExpertModule br = (BugExpertModule) rep;

		loadSystemProperties(br);
		loadUptime(br);
	}

	private void loadUptime(BugExpertModule br) {
		mUpTime = 0;
		Section sec = br.findSection(Section.UPTIME);
		if (sec == null) {
			br.logE(TAG + "Cannot find section: " + Section.UPTIME);
			return;
		}

		String line = sec.getLine(0);
		Matcher m = RegExpUtil.getMatch(RegExpUtil.UPTIME, line);
		if (m == null) {
			br.logE(TAG + "Cannot parse uptime: " + line);
			return;
		}

		String day = null;
		String time = null;
		long days = 0;
		long times = 0;
		if (m.group(3) != null) {
			day = m.group(3);
			Matcher mDay = RegExpUtil.getMatch(RegExpUtil.UPTIME_DAYS, day);
			days = Long.parseLong(mDay.group(1)) * 24 * 60 * 60;
		} else {
			br.logW(TAG + "Cannot parse day string: " + line + ", ignoring...");
		}

		if (m.group(4) != null) {
			time = m.group(4);
			Matcher mTime = RegExpUtil.getMatch(RegExpUtil.UPTIME_TIMES, time);
			times = Long.parseLong(mTime.group(1)) * 60 * 60 + Long.parseLong(mTime.group(1)) * 60;
		} else {
			br.logE(TAG + "Cannot parse time string: " + line);
		}

		mUpTime = days + times;
		br.logD(TAG + "uptime is " + mUpTime + " .");
		br.setUptime(mUpTime, 100);
	}

	private void loadSystemProperties(BugExpertModule br) {
		// reset
		mMap.clear();
		Section sec = br.findSection(Section.SYSTEM_PROPERTIES);
		if (sec == null) {
			br.logE(TAG + "Cannot find section: " + Section.SYSTEM_PROPERTIES);
			return;
		}
		int cnt = sec.getLineCount();
		for (int i = 0; i < cnt; i++) {
			String line = sec.getLine(i);
			int len = line.length();
			int idx = line.indexOf(':');
			if (idx < 0)
				continue;
			if (line.charAt(0) != '[')
				continue;
			if (line.charAt(idx - 1) != ']')
				continue;
			if (line.charAt(idx + 2) != '[')
				continue;
			if (line.charAt(len - 1) != ']')
				continue;
			String key = line.substring(1, idx - 1);
			String value = line.substring(idx + 3, len - 1);
			mMap.put(key, value);
		}

		// Now let the others now what is the current android version
		br.setAndroidVersion(mMap.get("ro.build.version.release"));
		br.setAndroidSdkVersion(mMap.get("ro.build.version.sdk"));
	}

	@Override
	public void generate(Module br) {
		// NOP
	}

	public String getProp(String key) {
		return mMap.get(key);
	}

}
