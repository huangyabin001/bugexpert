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
package com.bill.bugexpert.plugins.logs.kernel.iptables;

import java.util.HashMap;
import java.util.Vector;

import com.bill.bugexpert.Module;
import com.bill.bugexpert.chart.ChartGenerator;
import com.bill.bugexpert.chart.ChartPlugin;
import com.bill.bugexpert.chart.ChartPluginRepo;
import com.bill.bugexpert.chart.Data;
import com.bill.bugexpert.chart.DataSet;
import com.bill.bugexpert.chart.DataSetInfo;
import com.bill.bugexpert.doc.Chapter;
import com.bill.bugexpert.plugins.battery.BatteryLevelChart;
import com.bill.bugexpert.plugins.battery.ScreenOnPlugin;
import com.bill.bugexpert.plugins.logs.event.BatteryLevels;

public class PacketGraph {

    public void run(Module mod, Chapter ch, Vector<Packet> packets, String title, String filename) {
        // if we don't have real timestamps, abort
        if (packets.get(0).realTs == 0) return;

        ChartGenerator chart = new ChartGenerator(title);
        BatteryLevels bl = (BatteryLevels) mod.getInfo(BatteryLevels.INFO_ID);
        if (bl != null) {
            chart.addPlugin(new BatteryLevelChart(bl, BatteryLevelChart.LEVEL_ONLY));
        }
        chart.addPlugin(new ScreenOnPlugin());

        PacketChart all = new PacketChart("All", null);
        chart.addPlugin(all);

        HashMap<String, PacketChart> cats = new HashMap<String, PacketChart>();
        for (Packet p : packets) {
            if (!filter(p)) continue;
            all.add(p);
            String cat = p.getCategory();
            PacketChart pc = cats.get(cat);
            if (pc == null) {
                pc = new PacketChart(cat, null);
                cats.put(cat, pc);
                chart.addPlugin(pc);
            }
            pc.add(p);
        }
        chart.setOutput(filename);
        ch.add(chart.generate(mod));
    }

    protected boolean filter(Packet p) {
        // Placeholder to filter packets
        return true;
    }

    class PacketChart extends ChartPlugin {

        private String mName;
        private Vector<Packet> mPackets;

        public PacketChart(String name, Vector<Packet> packets) {
            mName = name;
            mPackets = packets;
            if (mPackets == null) {
                mPackets = new Vector<Packet>();
            }
        }

        public void add(Packet p) {
            mPackets.add(p);
        }

        @Override
        public boolean init(Module mod, ChartGenerator chart) {
            if (mPackets.isEmpty()) {
                return false;
            }
            final ChartPluginRepo repo = mod.getChartPluginRepo();
            init(chart, true, "IN  ", repo);
            init(chart, false, "OUT ", repo);
            return true;
        }

        private void init(ChartGenerator chart, boolean in, String pref, ChartPluginRepo repo) {
            DataSet ds = new DataSet(DataSet.Type.STATE, pref + mName);
            ds.addColor(0x40ffffff);
            ds.addColor(0x40000000);
            long last = -1, pw = 1000;
            for (Packet p : mPackets) {
                if (p.isInput() ^ in) continue;
                long ts = p.realTs;
                if (last != -1 && last + pw < ts) {
                    ds.addData(new Data(last + pw, 0));
                }
                ds.addData(new Data(ts, 1));
                last = ts;
            }
            if (last != -1) {
                ds.addData(new Data(last + pw, 0));
            }
            chart.add(ds);
            repo.add(new DataSetInfo(ds, "Packets"));
        }

        @Override
        public long getFirstTs() {
            return mPackets.get(0).realTs;
        }

        @Override
        public long getLastTs() {
            return mPackets.get(mPackets.size() - 1).realTs;
        }

    }

}
