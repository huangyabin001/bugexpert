package com.bill.bugexpert.plugins.apps;

import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.bill.bugexpert.Module;
import com.bill.bugexpert.Plugin;
import com.bill.bugexpert.Section;
import com.bill.bugexpert.doc.Chapter;
import com.bill.bugexpert.util.DumpTree;
import com.bill.bugexpert.util.DumpTree.Node;

public class AppActivitiesPlugin extends Plugin {

    private static final String TAG = "[ViewHierarchyPlugin]";

    private boolean mLoaded;

    private Vector<Task> mTasks = new Vector<Task>();

    @Override
    public int getPrio() {
        return 85;
    }

    @Override
    public void reset() {
        mTasks.clear();
        mLoaded = false;
    }

    @Override
    public void load(Module mod) {
    	mMod.logD("AppActivitiesPlugin:load(),loading...");
        Section sec = mod.findSection(Section.APP_ACTIVITIES);
        if (sec == null) {
            mod.logE(TAG + "Section not found: " + Section.APP_ACTIVITIES + " (aborting plugin)");
            return;
        }

        DumpTree tree = new DumpTree(sec, 0);
        Node root = tree.getRoot();
        Pattern pTask = Pattern.compile("TASK (.+) id=([0-9]+)");
        Pattern pAct = Pattern.compile("ACTIVITY (.+) ([0-9a-f]+) pid=([0-9]+)");

        // Cycle through tasks
        for (int i = 0; i < root.getChildCount(); i++) {
            Node taskNode = root.getChild(i);
            Matcher m = pTask.matcher(taskNode.getLine());
            if (!m.matches()) continue;
            String taskName = m.group(1);
            int taskId = Integer.parseInt(m.group(2));
            Task task = new Task(taskName, taskId);
            mTasks.add(task);

            // Cycle through activities
            for (int j = 0; j < taskNode.getChildCount(); j++) {
                Node actNode = taskNode.getChild(j);
                m = pAct.matcher(actNode.getLine());
                if (!m.matches()) continue;

                String actName = m.group(1);
                int pid = Integer.parseInt(m.group(3));
                Activity act = new Activity(actName, pid, task);
                task.add(act);

                parseViewHierarchy(mod, act, actNode);
            }
        }
        mLoaded = true;
    }

    private void parseViewHierarchy(Module mod, Activity act, Node actNode) {
        Node views = actNode.find("View Hierarchy:", false);
        if (views == null) return;
        act.setViewHierarchy(new View(mod, views.getChild(0)));

    }

    @Override
    public void generate(Module mod) {
        if (!mLoaded) return;

        // Create the chapter
        Chapter ch = mod.findOrCreateChapter("Applications/Running");
        for (Task task : mTasks) {
            Chapter chTask = new Chapter(mod.getContext(), task.getName());
            chTask.setStandalone(true);
            ch.addChapter(chTask);
            for (int i = 0; i < task.getActivityCount(); i++) {
                Activity act = task.getActivity(i);
                Chapter chAct = new Chapter(mod.getContext(), act.getName());
                chAct.setStandalone(true);
                chTask.addChapter(chAct);

                View views = act.getViewHierarchy();
                if (views != null && views.getRect().w > 0 && views.getRect().h > 0) {
                    chAct.add(new ViewHierarchyGenerator(views));
                }
            }
        }
    }

}
