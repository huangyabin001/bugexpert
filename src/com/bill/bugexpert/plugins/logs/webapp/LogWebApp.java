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
package com.bill.bugexpert.plugins.logs.webapp;

import java.io.IOException;

import com.bill.bugexpert.Module;
import com.bill.bugexpert.doc.Block;
import com.bill.bugexpert.doc.Button;
import com.bill.bugexpert.doc.Chapter;
import com.bill.bugexpert.doc.DocNode;
import com.bill.bugexpert.doc.HtmlNode;
import com.bill.bugexpert.doc.Renderer;
import com.bill.bugexpert.doc.Script;
import com.bill.bugexpert.doc.Span;
import com.bill.bugexpert.plugins.logs.LogData;
import com.bill.bugexpert.plugins.logs.LogLine;
import com.bill.bugexpert.webserver.ChkBugReportWebServer;
import com.bill.bugexpert.webserver.Web;
import com.bill.bugexpert.webserver.engine.HTTPRenderer;
import com.bill.bugexpert.webserver.engine.HTTPRequest;
import com.bill.bugexpert.webserver.engine.HTTPResponse;

public class LogWebApp {

    /** Reference to the log plugin */
    private LogData mLog;
    /** The cached value of the log's info id */
    private String mId;
    /** The set of filters/filter groups created by the user */
    private Filters mFilters;
    /** The set of log comments created by the user */
    private Comments mComments;
    /** Reference to the web server */
    private ChkBugReportWebServer mWS;

    public LogWebApp(LogData logData, ChkBugReportWebServer ws) {
        mLog = logData;
        mId = logData.getInfoId();
        mWS = ws;
        mFilters = new Filters(mWS.getModule().getSaveFile(), mId);
        mComments = new Comments(mWS.getModule().getSaveFile(), mId);
    }

    @Web
    public void log(Module mod, HTTPRequest req, HTTPResponse resp) {
        Chapter ch = new Chapter(mod.getContext(), "Log");

        // Add extra views to the header
        Span filterSelect = new Span();
        filterSelect.add("Filter group:");
        new HtmlNode("select", filterSelect).setName("filter").setId("filter");
        filterSelect.add(new Button("New filter", "javascript:logNewFilterGroup()"));
        ch.addCustomHeaderView(filterSelect);

        // Add placeholder for confirmation dialog boxes
        new Block(ch).addStyle("dialog").setId("generic-dlg");

        // Add "New filter" dialog box
        new Block(ch).addStyle("dialog").setId("new-filter-dlg")
            .add("Filter name:")
            .add(new HtmlNode("input")
                .setAttr("type", "text")
                .setAttr("name", "name")
                .addStyle("name")
                .addStyle("ui-widget-content ui-corner-all"))
            .add(new Block().addStyle("tip"));

        // Add placeholder for the log filter
        new Block(ch).setId("log-filter").addStyle("auto-collapsible ui-accordion ui-widget ui-helper-reset")
            .add(new Block()
                .addStyle("header auto-collapsible-header auto-sortable-handle ui-accordion-header ui-helper-reset ui-corner-top ui-accordion-icons")
                .add(new Span()))
            .add(new Block()
                .addStyle("body auto-collapsible-content ui-helper-reset ui-widget-content ui-corner-bottom")
                .add(new Block().addStyle("content"))
                .add(new Block().addStyle("add-new")));

        // Add placeholder for the log
        new Block(ch).setId("log-placeholder");

        // Add custom javascript code
        new Script(ch).println("var logid=\"" + mId + "\";");
        new Script(ch, "lib_log.js");

        try {
            Renderer r = new HTTPRenderer(resp, mId + "$log", ch);
            ch.prepare(r);
            ch.render(r);
        } catch (IOException e) {
            e.printStackTrace();
            resp.setResponseCode(500);
        }
    }

    @Web
    public void logOnly(Module mod, HTTPRequest req, HTTPResponse resp) {
        String filterName = req.getArg("filter");
        FilterGroup fg = mFilters.find(filterName);
        DocNode log = new Block().addStyle("log").addStyle("log-dynamic");
        int cnt = mLog.size();
        boolean prevSkipped = false;
        for (int i = 0; i < cnt; i++) {
            LogLine sl = mLog.get(i);
            if (fg != null && !fg.handle(sl)) {
                prevSkipped = true;
                continue;
            }
            LogLine llCopy = sl.copy();
            if (prevSkipped) {
                llCopy.addStyle("log-skipped-lines-before");
            }
            log.add(llCopy);
            mComments.collectLogs(llCopy, log);
            prevSkipped = false;
        }
        try {
            Renderer r = new HTTPRenderer(resp, mId + "$log", null);
            log.prepare(r);
            log.render(r);
        } catch (IOException e) {
            e.printStackTrace();
            resp.setResponseCode(500);
        }
    }

    @Web
    public void listFilterGroups(Module mod, HTTPRequest req, HTTPResponse resp) {
        mFilters.listFilterGroups(mod, req, resp);
    }

    @Web
    public void newFilterGroup(Module mod, HTTPRequest req, HTTPResponse resp) {
        mFilters.newFilterGroup(mod, req, resp);
    }

    @Web
    public void deleteFilterGroup(Module mod, HTTPRequest req, HTTPResponse resp) {
        mFilters.deleteFilterGroup(mod, req, resp);
    }

    @Web
    public void listFilters(Module mod, HTTPRequest req, HTTPResponse resp) {
        mFilters.listFilters(mod, req, resp);
    }

    @Web
    public void newFilter(Module mod, HTTPRequest req, HTTPResponse resp) {
        mFilters.newFilter(mod, req, resp);
    }

    @Web
    public void updateFilter(Module mod, HTTPRequest req, HTTPResponse resp) {
        mFilters.updateFilter(mod, req, resp);
    }

    @Web
    public void deleteFilter(Module mod, HTTPRequest req, HTTPResponse resp) {
        mFilters.deleteFilter(mod, req, resp);
    }

    @Web
    public void addComment(Module mod, HTTPRequest req, HTTPResponse resp) {
        mComments.addComment(mod, req, resp);
    }

    @Web
    public void updateComment(Module mod, HTTPRequest req, HTTPResponse resp) {
        mComments.updateComment(mod, req, resp);
    }

    @Web
    public void deleteComment(Module mod, HTTPRequest req, HTTPResponse resp) {
        mComments.deleteComment(mod, req, resp);
    }
}
