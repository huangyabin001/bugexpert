package com.bill.bugexpert.ps;

import java.util.Iterator;
import java.util.Vector;

public class PSRecord implements Iterable<PSRecord> {

	/** The policy cannot be recognized (parsing failed) */
	public static final int PCY_OTHER = -2;
	/** The policy is unknown (no data) */
	public static final int PCY_UNKNOWN = -1;
	/** Normal/foreground process */
	public static final int PCY_NORMAL = 0;
	/** FIFO/important process */
	public static final int PCY_FIFO = 1;
	/** Background process */
	public static final int PCY_BATCH = 3;

	/** Special value when the real value is unknown */
	public static final int NICE_UNKNOWN = -100;

	String mLabel;// Security Context
	String mUser;// User Name
	int mPid;// Process ID
	int mTid;// Thread ID
	int mPPid;// Parent Process ID
	// int mVsz;// Total memory size,(include code+data+stack)(bytes)
	// int mRss;// Total physical memory used by the process(kbytes)
	// String mWchan;// Whether the current process is running, "-" means it is running.
	// String mAddr;// 
	// String mS;// Process status.
	int mPri;// Kernel scheduling priority.
	int mNice;// Ni;
	//int mRtprio;// 
	//int mSch;//
	int mPcy;// 
	String mCmd;// Simple format for executing commands.
	PSRecord mParent;
	Vector<PSRecord> mChildren = new Vector<PSRecord>();

	public PSRecord(int pid, int ppid, int nice, int pcy, String cmd, String label, String user, int tid) {
		mPid = pid;
		mPPid = ppid;
		mNice = nice;
		mPcy = pcy;
		mCmd = cmd;
		mLabel = label;
		mUser = user;
		mTid = tid;
	}

	public String getLabel() {
		return mLabel;
	}
	
	public String getUser() {
		return mUser;
	}
	
	public int getPid() {
		return mPid;
	}

	public int getTid() {
		return mTid;
	}
	
	public int getParentPid() {
		return mPPid;
	}

	public int getNice() {
		return mNice;
	}

	public int getPolicy() {
		return mPcy;
	}

	public String getPolicyStr() {
		switch (mPcy) {
		case PCY_NORMAL:
			return "fg";
		case PCY_BATCH:
			return "bg";
		case PCY_FIFO:
			return "un";
		default:
			return "??";
		}
	}

	public String getCmd() {
		return mCmd;
	}

	@Override
	public Iterator<PSRecord> iterator() {
		return mChildren.iterator();
	}

	public void getChildren(Vector<PSRecord> ret) {
		for (PSRecord child : mChildren) {
			ret.add(child);
		}
	}

}
