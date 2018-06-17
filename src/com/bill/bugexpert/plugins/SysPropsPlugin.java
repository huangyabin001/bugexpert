package com.bill.bugexpert.plugins;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.bill.bugexpert.BugExpertModule;
import com.bill.bugexpert.Module;
import com.bill.bugexpert.Plugin;
import com.bill.bugexpert.Section;

public class SysPropsPlugin extends Plugin {

    private static final String TAG = "[SysPropsPlugin]";

    private static final Pattern UPTIME_PATTERN = Pattern.compile("up time: (.*), idle time: (.*), sleep time: (.*)");
    private static final Pattern TIME_PATTERN_1 = Pattern.compile("(.*) days, (.*):(.*):(.*)");
    private static final Pattern TIME_PATTERN_2 = Pattern.compile("(.*):(.*):(.*)");

    private HashMap<String, String> mMap = new HashMap<String, String>();

    private long mUpTime;
    private long mIdleTime;
    private long mSleepTime;

    @Override
    public int getPrio() {
        return 1; // Need to execute first
    }

    public long getUpTime() {
        return mUpTime;
    }

    public long getIdleTime() {
        return mIdleTime;
    }

    public long getSleepTime() {
        return mSleepTime;
    }

    @Override
    public void reset() {
        // NOP
    }

    @Override
    public void load(Module rep) {
		mMod.logD("SysPropsPlugin:load(),loading...");
        BugExpertModule br = (BugExpertModule)rep;

        loadSystemProperties(br);
        loadUptime(br);
    }

    private void loadUptime(BugExpertModule br) {
        mUpTime = 0;
        mIdleTime = 0;
        mSleepTime = 0;
        Section sec = br.findSection(Section.UPTIME);
        if (sec == null) {
            br.logE(TAG + "Cannot find section: " + Section.UPTIME);
            return;
        }

        String line = sec.getLine(0);
        Matcher m = UPTIME_PATTERN.matcher(line);
        if (!m.matches()) {
            br.logE(TAG + "Cannot parse uptime: " + line);
            return;
        }
        mUpTime = parseTime(br, m.group(1));
        mIdleTime = parseTime(br, m.group(2));
        mSleepTime = parseTime(br, m.group(3));
        br.setUptime(mUpTime, 100);
    }

    private long parseTime(BugExpertModule br, String str) {
        Matcher m = TIME_PATTERN_1.matcher(str);
        if (m.matches()) {
            long days = Long.parseLong(m.group(1));
            long hours = Long.parseLong(m.group(2));
            long minutes = Long.parseLong(m.group(3));
            long seconds = Long.parseLong(m.group(4));
            return ((days * 24 + hours) * 60 + minutes) * 60 + seconds;
        }
        m = TIME_PATTERN_2.matcher(str);
        if (m.matches()) {
            long hours = Long.parseLong(m.group(1));
            long minutes = Long.parseLong(m.group(2));
            long seconds = Long.parseLong(m.group(3));
            return (hours * 60 + minutes) * 60 + seconds;
        }
        br.logE(TAG + "Cannot parse time string: " + str);
        return 0;
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
            if (idx < 0) continue;
            if (line.charAt(0) != '[') continue;
            if (line.charAt(idx-1) != ']') continue;
            if (line.charAt(idx+2) != '[') continue;
            if (line.charAt(len-1) != ']') continue;
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
