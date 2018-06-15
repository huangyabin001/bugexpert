package com.bill.bugexpert.plugins.battery;

import com.bill.bugexpert.Module;
import com.bill.bugexpert.chart.LogFilterChartPlugin;
import com.bill.bugexpert.util.XMLNode;

public class Hooks {

    private BatteryInfoPlugin mPlugin;

    public Hooks(BatteryInfoPlugin plugin) {
        mPlugin = plugin;
    }

    public void add(Module mod, XMLNode hook) {
        for (XMLNode item : hook) {
            String tag = item.getName();
            if (tag == null) continue;
            if (tag.equals("logchart")) {
                LogFilterChartPlugin logChart = LogFilterChartPlugin.parse(mod, item);
                mPlugin.addBatteryLevelChartPlugin(logChart);
            } else {
                mod.logE("Unknown tag in battery info hook: " + tag);
            }
        }
    }

}
