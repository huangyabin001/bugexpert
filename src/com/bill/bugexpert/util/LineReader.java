package com.bill.bugexpert.util;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

public class LineReader {

    private static final int STATE_IDLE = 0;
    private static final int STATE_0D0D = 1;
    private static final int STATE_0A   = 2;
    private static final int STATE_EOF  = 3;

    private int mState = STATE_IDLE;
    private boolean mFirstLine = true;
    private InputStream mIs;
    private byte[] mBuff;
    private int mOffs;
    private int mLen;
    private StringBuilder mSB = new StringBuilder();

    public LineReader(InputStream is) {
        mIs = new BufferedInputStream(is);
    }

    public LineReader(byte[] buff, int offs, int len) {
        mBuff = buff;
        mOffs = offs;
        mLen = len;
    }

    public String readLine() {
        mSB.setLength(0);
        boolean firstWarning = false;
        try {
            while (true) {
                int b = read();
                if (b < 0) {
                    if (mSB.length() == 0) return null;
                    mState = STATE_EOF;
                    break; // EOF
                }
                if (b == 0xd) {
                    if (firstWarning) {
                        mState = STATE_0D0D;
                        break;
                    }
                    firstWarning = true;
                    continue; // Skip ugly windows line ending
                }
                if (b == 0xa) {
                    if (mSB.length() == 0 && mState == STATE_0D0D) {
                        // Workaround for "0x0d 0x0d 0x0a" line endings
                        continue;
                    }
                    mState = STATE_0A;
                    break; // EOL
                }
                mSB.append((char)b);
            }
        } catch (IOException e) {
            // Ignore exception
            e.printStackTrace();
            return null;
        }
        if (mFirstLine && mSB.length() > 3) {
            if (mSB.charAt(0) == 239 && mSB.charAt(1) == 187 && mSB.charAt(2) == 191) {
                // Workaround for UTF8 marker
                mSB.delete(0, 3);
            }
        }
        mFirstLine = false;
        return mSB.toString();
    }

    private int read() throws IOException {
        if (mIs != null) {
            return mIs.read();
        }
        if (mLen <= 0) {
            return -1; // eof
        }
        mLen--;
        return mBuff[mOffs++];
    }

    public void close() {
        if (mIs != null) {
            try {
                mIs.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
