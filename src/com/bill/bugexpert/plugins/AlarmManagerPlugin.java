package com.bill.bugexpert.plugins;

import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.bill.bugexpert.BugExpertModule;
import com.bill.bugexpert.Module;
import com.bill.bugexpert.Plugin;
import com.bill.bugexpert.Section;
import com.bill.bugexpert.doc.Anchor;
import com.bill.bugexpert.doc.Chapter;
import com.bill.bugexpert.doc.Link;
import com.bill.bugexpert.doc.Para;
import com.bill.bugexpert.doc.ShadedValue;
import com.bill.bugexpert.doc.Table;
import com.bill.bugexpert.util.DumpTree;
import com.bill.bugexpert.util.Util;
import com.bill.bugexpert.util.DumpTree.Node;

public class AlarmManagerPlugin extends Plugin {

    private static final String TAG = "[AlarmManagerPlugin]";

    private boolean mLoaded;
    private Section mSection;
    private Vector<Alarm> mAlarms = new Vector<Alarm>();
    private Vector<AlarmStat> mStats = new Vector<AlarmStat>();
    private int mNextAlarmAnchor;

    @Override
    public int getPrio() {
        return 91;
    }

    @Override
    public void reset() {
        // Reset
        mLoaded = false;
        mSection = null;
        mAlarms.clear();
        mNextAlarmAnchor = 0;
    }

    @Override
    public void load(Module br) {
        // Load data
        mSection = br.findSection(Section.DUMP_OF_SERVICE_ALARM);
        if (mSection == null) {
            br.logE(TAG + "Section not found: " + Section.DUMP_OF_SERVICE_ALARM + " (aborting plugin)");
            return;
        }

        // Parse the data
        DumpTree dump = new DumpTree(mSection, 0);
        final String nodeKey = "Current Alarm Manager state:";
        DumpTree.Node root = dump.find(nodeKey, false);
        if (root == null) {
            br.logE("Cannot find node '" + nodeKey + "'");
            return;
        }
        if (root.getChildCount() == 0) {
            loadFromScrewedUpIndentation(br, root.getParent());
        } else {
            loadOldFormat(br, root);
        }

        // Done
        mLoaded = true;
    }

    private void loadFromScrewedUpIndentation(Module br, Node root) {
        // Parse stats
        final String nodeKey = "Alarm Stats:";
        DumpTree.Node stats = root.find(nodeKey, true);
        if (stats == null) {
            br.logE("Cannot find node '" + nodeKey + "'");
        } else {
            Node parent = stats.getParent();
            int idx = parent.indexOf(stats);
            if (idx < 0) {
                br.logE("Something's fishy with node '" + nodeKey + "'");
            } else {
                int childCount = parent.getChildCount();
                while (++idx < childCount) {
                    Node item = parent.getChild(idx);
                    if (item.getLine().endsWith("wakeups:")) {
                        addPackageStat(br, item);
                    }
                }
            }
        }

        // Parse alarms
        for (DumpTree.Node batch : root) {
            String line = batch.getLine();
            if (!line.startsWith("Batch{")) continue;
            for (DumpTree.Node item : batch) {
                line = item.getLine();
                if (line.startsWith("ELAPSED ")) {
                    addAlarm(br, item);
                } else if (line.startsWith("ELAPSED_WAKEUP ")) {
                    addAlarm(br, item);
                } else if (line.startsWith("RTC ")) {
                    addAlarm(br, item);
                } else if (line.startsWith("RTC_WAKEUP ")) {
                    addAlarm(br, item);
                }
            }
        }
    }

    private void loadOldFormat(Module br, Node root) {
        boolean stats = false;
        for (DumpTree.Node item : root) {
            String line = item.getLine();
            if (stats) {
                addPackageStat(br, item);
            } else if (line.startsWith("Alarm Stats:")) {
                stats = true;
            } else if (line.startsWith("ELAPSED ")) {
                addAlarm(br, item);
            } else if (line.startsWith("ELAPSED_WAKEUP ")) {
                addAlarm(br, item);
            } else if (line.startsWith("RTC ")) {
                addAlarm(br, item);
            } else if (line.startsWith("RTC_WAKEUP ")) {
                addAlarm(br, item);
            }
        }
    }

    private void addPackageStat(Module br, Node item) {
        AlarmStat stat = new AlarmStat();
        stat.anchor = new Anchor("a" + mNextAlarmAnchor++);
        stat.pkg = item.getLine();
        for (int i = 0; i < item.getChildCount(); i++) {
            Node child = item.getChild(i);
            if (i == 0) {
                // Note: either the first raw (old style) or the parent line (new style) contains this info
                String summary = child.getLine();
                boolean newStyle = stat.pkg.contains(" ");
                if (newStyle) {
                    int idx = stat.pkg.indexOf(' ');
                    summary = stat.pkg.substring(idx + 1);
                    stat.pkg = stat.pkg.substring(0, idx);
                }
                Pattern p = Pattern.compile("(.*) running, (.*) wakeups:?");
                Matcher m = p.matcher(summary);
                if (!m.matches()) {
                    br.logE("Cannot parse alarm stat: " + child.getLine());
                    return;
                }
                stat.runtime = Util.parseRelativeTimestamp(m.group(1));
                stat.wakeups = Long.parseLong(m.group(2));
                if (!newStyle) {
                    continue; // if old style, then first line already parsed
                }
            }

            Pattern pOld = Pattern.compile("(.*) alarms: (.*)");
            Pattern pNew = Pattern.compile("(.*) ([0-9]+) wakes ([0-9]+) alarms: (.*)");
            Matcher m = pNew.matcher(child.getLine());
            AlarmAction action = new AlarmAction();
            if (m.matches()) {
                // New format detected
                action.time = Util.parseRelativeTimestamp(m.group(1));
                action.wakeCount = Long.parseLong(m.group(2));
                action.alarmCount = Long.parseLong(m.group(3));
                action.action = m.group(4);
            } else {
                m = pOld.matcher(child.getLine());
                if (!m.matches()) {
                    br.logE("Cannot parse alarm stat: " + child.getLine());
                    return;
                }
                // New format detected
                action.time = -1;
                action.wakeCount = -1;
                action.alarmCount = Long.parseLong(m.group(1));
                action.action = m.group(2);
            }
            stat.actions.add(action);
            stat.alarms += action.alarmCount;
        }
        mStats.add(stat);
    }

    private void addAlarm(Module br, Node item) {
        Alarm alarm = new Alarm();
        Pattern p = Pattern.compile("([A-Z_]+) #[0-9]+: Alarm\\{[a-f0-9]+ type [0-3] (.*)\\}");
        Matcher m = p.matcher(item.getLine());
        if (!m.matches()) {
            br.logE("Cannot parse alarm: " + item.getLine());
            return;
        }

        alarm.type = m.group(1);
        alarm.pkg = m.group(2);

        String props = item.getChild(0).getLine();
        p = Pattern.compile("type=(.?) .*when=([^ ]*)( window=.*)? repeatInterval=(.*) count=(.*)");
        m = p.matcher(props);
        if (!m.matches()) {
            br.logE("Cannot parse alarm properties: " + props);
            return;
        }
        alarm.whenS = m.group(2);
        alarm.when = readTs(alarm.whenS);
        alarm.repeat = Long.parseLong(m.group(4));
        alarm.count = Long.parseLong(m.group(5));

        String op = item.getChild(1).getLine();
        p = Pattern.compile("operation=PendingIntent\\{[0-9a-f]+: PendingIntentRecord\\{[0-9a-f]+ (.*) ([a-zA-Z]*)\\}\\}");
        m = p.matcher(op);
        if (!m.matches()) {
            br.logE("Cannot parse alarm operation: " + op);
            return;
        }
        alarm.opPkg = m.group(1);
        alarm.opMet = m.group(2);

        mAlarms.add(alarm);
    }

    private long readTs(String s) {
        s = Util.strip(s);
        long ret = 0;
        int idx;

        // skip over the negative and positive signs
        if (s.charAt(0) == '-') {
            s = s.substring(1);
        }
        if (s.charAt(0) == '+') {
            s = s.substring(1);
        }

        // Remove the "ms" from the end... it screws up our parsing
        if (s.endsWith("ms")) {
            s = s.substring(0, s.length() - 2);
        }

        // Ignore everything before the first digit
        while (s.length() > 0 && !Character.isDigit(s.charAt(0))) {
            s = s.substring(1);
        }

        // parse day
        idx = s.indexOf("d");
        if (idx >= 0) {
            int day = Integer.parseInt(s.substring(0, idx));
            s = s.substring(idx + 1);
            ret += day * (24 * 3600000L);
        }
        // parse hours
        idx = s.indexOf("h");
        if (idx >= 0) {
            int hour = Integer.parseInt(s.substring(0, idx));
            s = s.substring(idx + 1);
            ret += hour * 3600000L;
        }

        // parse minutes
        idx = s.indexOf("m");
        if (idx >= 0) {
            int min = Integer.parseInt(s.substring(0, idx));
            s = s.substring(idx + 1);
            ret += min * 60000L;
        }

        // parse seconds
        idx = s.indexOf("s");
        if (idx >= 0) {
            int sec = Integer.parseInt(s.substring(0, idx));
            s = s.substring(idx + 1);
            ret += sec * 1000L;
        }

        // parse millis
        int ms = Integer.parseInt(s);
        ret += ms;

        return ret;
    }

    @Override
    public void generate(Module rep) {
        if (!mLoaded) return;
        BugExpertModule br = (BugExpertModule) rep;

        // Generate the report
        Chapter mainCh = new Chapter(br.getContext(), "AlarmManager");
        br.addChapter(mainCh);

        genAlarmList(br, mainCh);
        genAlarmStat(br, mainCh);
        genAlarmStatDetailed(br, mainCh);
        genAlarmStatCombined(br, mainCh);
    }

    private Chapter genAlarmList(BugExpertModule br, Chapter mainCh) {
        Chapter ch = new Chapter(br.getContext(), "Alarms");
        mainCh.addChapter(ch);

        new Para(ch).add("List of alarms (" + mAlarms.size() + "):");

        Table tg = new Table(Table.FLAG_SORT, ch);
        tg.setCSVOutput(br, "alarm_list");
        tg.setTableName(br, "alarm_list");
        tg.addColumn("Type", null, Table.FLAG_NONE, "type");
        tg.addColumn("Pkg", null, Table.FLAG_NONE, "pkg");
        tg.addColumn("Time", null, Table.FLAG_ALIGN_RIGHT, "time varchar");
        tg.addColumn("Time(ms)", null, Table.FLAG_ALIGN_RIGHT, "time_ms int");
        tg.addColumn("Interv(ms)", null, Table.FLAG_ALIGN_RIGHT, "interv_ms int");
        tg.addColumn("Count", null, Table.FLAG_ALIGN_RIGHT, "count int");
        tg.addColumn("OpPkg", null, Table.FLAG_NONE, "op_pkg varchar");
        tg.addColumn("OpMet", null, Table.FLAG_NONE, "op_met varchar");
        tg.begin();

        for (Alarm alarm : mAlarms) {
            tg.addData(alarm.type);
            tg.addData(alarm.pkg);
            tg.addData(alarm.whenS);
            tg.addData(new ShadedValue(alarm.when));
            tg.addData(new ShadedValue(alarm.repeat));
            tg.addData(alarm.count);
            tg.addData(alarm.opPkg);
            tg.addData(alarm.opMet);
        }
        tg.end();
        return ch;
    }

    private Chapter genAlarmStat(BugExpertModule br, Chapter mainCh) {
        Chapter ch = new Chapter(br.getContext(), "Alarm stats");
        mainCh.addChapter(ch);
        Table tg = new Table(Table.FLAG_SORT, ch);
        tg.setCSVOutput(br, "alarm_stat");
        tg.setTableName(br, "alarm_stat");
        tg.addColumn("Pkg", null, Table.FLAG_NONE, "pkg");
        tg.addColumn("Runtime(ms)", null, Table.FLAG_ALIGN_RIGHT, "runtime_ms int");
        tg.addColumn("Wakeups", null, Table.FLAG_ALIGN_RIGHT, "wakeups int");
        tg.addColumn("Alarms", null, Table.FLAG_ALIGN_RIGHT, "alarms int");
        tg.begin();

        for (AlarmStat stat : mStats) {
            tg.addData(new Link(stat.anchor, stat.pkg));
            tg.addData(new ShadedValue(stat.runtime));
            tg.addData(new ShadedValue(stat.wakeups));
            tg.addData(new ShadedValue(stat.alarms));
        }
        tg.end();
        return ch;
    }

    private Chapter genAlarmStatDetailed(BugExpertModule br, Chapter mainCh) {
        Chapter ch = new Chapter(br.getContext(), "Alarm detailed stats");
        mainCh.addChapter(ch);

        for (AlarmStat stat : mStats) {
            Chapter childCh = new Chapter(br.getContext(), stat.pkg);
            ch.addChapter(childCh);

            childCh.add(stat.anchor);
            Table tg = new Table(Table.FLAG_SORT, childCh);
            tg.setCSVOutput(br, "alarm_stat_detailed_" + stat.pkg);
            tg.addColumn("Time", Table.FLAG_ALIGN_RIGHT);
            tg.addColumn("Wakes", Table.FLAG_ALIGN_RIGHT);
            tg.addColumn("Alarms", Table.FLAG_ALIGN_RIGHT);
            tg.addColumn("Action", Table.FLAG_NONE);
            tg.begin();

            for (AlarmAction act : stat.actions) {
                if (act.time < 0) {
                    tg.addData("NA");
                } else {
                    tg.addData(new ShadedValue(act.time));
                }
                if (act.wakeCount < 0) {
                    tg.addData("NA");
                } else {
                    tg.addData(new ShadedValue(act.wakeCount));
                }
                tg.addData(new ShadedValue(act.alarmCount));
                tg.addData(act.action);
            }
            tg.end();
        }
        return ch;
    }

    private Chapter genAlarmStatCombined(BugExpertModule br, Chapter mainCh) {
        Chapter ch = new Chapter(br.getContext(), "Alarm combined stats");
        mainCh.addChapter(ch);

        Table t = new Table(Table.FLAG_SORT, ch);
        t.setCSVOutput(br, "alarm_stat_combined");
        t.setTableName(br, "alarm_stat_combined");
        t.addColumn("Pkg", null, Table.FLAG_NONE, "pkg varchar");
        t.addColumn("Time", null, Table.FLAG_ALIGN_RIGHT, "time int");
        t.addColumn("Wakes", null, Table.FLAG_ALIGN_RIGHT, "wakes int");
        t.addColumn("Alarms", null, Table.FLAG_ALIGN_RIGHT, "alarms int");
        t.addColumn("Action", null, Table.FLAG_NONE, "action varchar");
        t.begin();

        for (AlarmStat stat : mStats) {
            for (AlarmAction act : stat.actions) {
                t.addData(stat.pkg);
                t.addData(new ShadedValue(act.time));
                t.addData(new ShadedValue(act.wakeCount));
                t.addData(new ShadedValue(act.alarmCount));
                t.addData(act.action);
            }
        }

        t.end();
        return ch;
    }

    class Alarm {
        public String type;
        public String pkg;
        public String whenS;
        public long when;
        public long repeat;
        public long count;
        public String opPkg;
        public String opMet;
    }

    class AlarmStat {
        public long runtime;
        public String pkg;
        public long wakeups;
        public long alarms;
        public Vector<AlarmAction> actions = new Vector<AlarmAction>();
        public Anchor anchor;
    }

    class AlarmAction {
        public String action;
        public long wakeCount;
        public long alarmCount;
        public long time;
    }
}
