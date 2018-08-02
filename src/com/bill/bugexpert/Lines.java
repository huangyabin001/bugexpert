package com.bill.bugexpert;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Vector;

/**
 * A named collection of text lines.
 */
public class Lines {

    private String mName;

    private Vector<String> mLines = new Vector<String>();

    public Lines(String name) {
        mName = name;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public void clear() {
        mLines.clear();
    }

    public void addLine(String line) {
        mLines.add(line);
    }

    public void addLine(String line, int idx) {
        mLines.add(idx, line);
    }

    public void removeLine(int idx) {
        mLines.remove(idx);
    }

    public int getLineCount() {
        return mLines.size();
    }

    public String getLine(int idx) {
        return mLines.get(idx);
    }

    public void addLines(Lines lines) {
        int cnt = lines.getLineCount();
        for (int i = 0; i < cnt; i++) {
            addLine(lines.getLine(i));
        }
    }

    public boolean writeTo(String fn) {
        try {
            FileOutputStream fos = new FileOutputStream(fn);
            PrintStream ps = new PrintStream(fos);
            writeTo(ps);
            ps.close();
            fos.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void writeTo(PrintStream ps) {
        for (String line : mLines) {
            ps.println(line);
        }
    }

}
