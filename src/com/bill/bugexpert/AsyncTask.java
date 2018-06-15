/*
 * Copyright (C) 2012 Sony Mobile Communications AB
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
package com.bill.bugexpert;

import javax.swing.SwingUtilities;

/* package */ abstract class AsyncTask {

    public abstract void before();

    public abstract void run();

    public abstract void after();

    public void exec() {
        before();
        new Thread() {
            @Override
            public void run() {
                AsyncTask.this.run();
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        after();
                    }
                });
            }

        }.start();
    }

}
