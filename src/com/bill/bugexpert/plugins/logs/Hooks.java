/*
 * Copyright (C) 2013 Sony Mobile Communications AB
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
package com.bill.bugexpert.plugins.logs;

import java.util.Vector;

import com.bill.bugexpert.BugExpertModule;
import com.bill.bugexpert.doc.Block;
import com.bill.bugexpert.doc.Bug;
import com.bill.bugexpert.doc.DocNode;
import com.bill.bugexpert.doc.Link;
import com.bill.bugexpert.doc.Para;
import com.bill.bugexpert.doc.Bug.Type;
import com.bill.bugexpert.util.Util;
import com.bill.bugexpert.util.XMLNode;

public class Hooks {

    private Vector<XMLNode> mHooks = new Vector<XMLNode>();
    private LogPlugin mLog;

    public Hooks(LogPlugin logPlugin) {
        mLog = logPlugin;
    }

    public void reset() {
        mHooks.clear();
    }

    public void add(XMLNode hook) {
        mHooks.add(hook);
    }

    public void execute(BugExpertModule mod) {
        for (XMLNode hook : mHooks) {
            execute(mod, hook);
        }
    }

    private void execute(BugExpertModule mod, XMLNode hook) {
        for (XMLNode item : hook) {
            String tag = item.getName();
            if (tag == null) continue;
            if (tag.equals("filter")) {
                filterLog(mod, item);
            } else {
                mod.logE("Unknown log hook: " + tag);
            }
        }
    }

    private void filterLog(BugExpertModule mod, XMLNode filter) {
        LogMatcher lm = new LogMatcher(mod, filter);
        LogLines logs = mLog.getLogs();
        for (LogLine ll : logs) {
            if (lm.matches(ll)) {
                executeAction(mod, filter, ll);
            }
        }
    }

    private void executeAction(BugExpertModule mod, XMLNode filter, LogLine ll) {
        for (XMLNode action : filter) {
            String tag = action.getName();
            if (tag == null) continue;
            if (tag.equals("hide")) {
                ll.setHidden(true);
            } else if (tag.equals("note")) {
                addNote(mod, ll, action);
            } else if (tag.equals("bug")) {
                addBug(mod, ll, action);
            } else {
                mod.logE("Unknown log filter action: " + tag);
            }
        }
    }

    private void addNote(BugExpertModule mod, LogLine ll, XMLNode action) {
        String text = action.getAttr("text");
        String css = "log-float";
        if (Util.parseBoolean(action.getAttr("error"), false)) {
            css = "log-float-err";
        }
        if (text == null) {
            mod.logE("Missing text attribute from 'note' log filter action!");
        } else {
            ll.addMarker(css, text, null);
        }
    }

    private void addBug(BugExpertModule mod, LogLine ll, XMLNode action) {
        String text = action.getAttr("text");
        String title = action.getAttr("title");
        String type = action.getAttr("type");
        Type bugType = Type.PHONE_WARN;
        int prio = Util.parseInt(action.getAttr("prio"), 100);
        if (title == null) {
            mod.logE("Missing title attribute from 'note' log filter action!");
            return;
        }
        if (type != null) {
            if ("phone err".equals(type)) {
                bugType = Type.PHONE_ERR;
            } else if ("phone warn".equals(type)) {
                bugType = Type.PHONE_WARN;
            } else if ("tool err".equals(type)) {
                bugType = Type.TOOL_ERR;
            } else if ("tool warn".equals(type)) {
                bugType = Type.TOOL_WARN;
            } else {
                mod.logE("Invalid value for 'type' attribute: " + type + ", must be one of 'phone err', 'phone warn', 'tool err', 'tool warn'!");
            }
        }
        Bug bug = new Bug(bugType, prio, ll.ts, title);
        new Block(bug).add(new Link(ll.getAnchor(), "(link to log)"));
        if (text != null) {
            new Para(bug).add(text);
        }
        DocNode log = new Block(bug).addStyle("log");
        log.add(ll.symlink());
        mod.addBug(bug);
    }

}
