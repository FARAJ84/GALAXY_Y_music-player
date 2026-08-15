package com.galaxyy.music.util;

import android.util.Log;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;

/**
 * Basic ID3 tag editor for MP3 files.
 * Supports ID3v2.3 (most common) and ID3v1.1 fallback.
 * No external libraries — manual frame parsing to keep APK tiny.
 * Only edits Title (TIT2), Artist (TPE1), Album (TALB) frames.
 */
public class TagEditor {
    private static final String TAG = "GalaxyYTag";

    private File mFile;
    private String mTitle;
    private String mArtist;
    private String mAlbum;
    private boolean mHasId3v2 = false;
    private long mId3v2Size = 0;

    public TagEditor(String filePath) {
        mFile = new File(filePath);
        readTags();
    }

    public String getTitle() { return mTitle; }
    public String getArtist() { return mArtist; }
    public String getAlbum() { return mAlbum; }

    public void setTitle(String title) { mTitle = title; }
    public void setArtist(String artist) { mArtist = artist; }
    public void setAlbum(String album) { mAlbum = album; }

    private void readTags() {
        if (!mFile.exists()) return;
        try {
            RandomAccessFile raf = new RandomAccessFile(mFile, "r");
            try {
                byte[] header = new byte[10];
                raf.readFully(header);
                if (header[0] == 'I' && header[1] == 'D' && header[2] == '3') {
                    mHasId3v2 = true;
                    mId3v2Size = decodeSyncsafeInt(header[6], header[7], header[8], header[9]) + 10;
                    parseId3v2Frames(raf);
                } else {
                    raf.seek(mFile.length() - 128);
                    byte[] id3v1 = new byte[128];
                    raf.readFully(id3v1);
                    if (id3v1[0] == 'T' && id3v1[1] == 'A' && id3v1[2] == 'G') {
                        mTitle = readIsoString(id3v1, 3, 30);
                        mArtist = readIsoString(id3v1, 33, 30);
                        mAlbum = readIsoString(id3v1, 63, 30);
                    }
                }
            } finally {
                raf.close();
            }
        } catch (IOException e) {
            Log.w(TAG, "Read error: " + e.getMessage());
        }
    }

    private void parseId3v2Frames(RandomAccessFile raf) throws IOException {
        long endPos = mId3v2Size;
        while (raf.getFilePointer() < endPos - 10) {
            byte[] frameHeader = new byte[10];
            raf.readFully(frameHeader);
            String frameId = new String(frameHeader, 0, 4, "ISO-8859-1");
            int frameSize = ((frameHeader[4] & 0xFF) << 24) | ((frameHeader[5] & 0xFF) << 16)
                    | ((frameHeader[6] & 0xFF) << 8) | (frameHeader[7] & 0xFF);

            if (frameSize <= 0 || frameSize > 1000000) break;

            byte[] frameData = new byte[frameSize];
            raf.readFully(frameData);

            if (frameId.equals("TIT2")) mTitle = readTextFrame(frameData);
            else if (frameId.equals("TPE1")) mArtist = readTextFrame(frameData);
            else if (frameId.equals("TALB")) mAlbum = readTextFrame(frameData);
        }
    }

    private String readTextFrame(byte[] data) {
        if (data.length < 2) return "";
        int encoding = data[0] & 0xFF;
        try {
            if (encoding == 0) {
                return new String(data, 1, data.length - 1, "ISO-8859-1").trim();
            } else if (encoding == 1 || encoding == 2) {
                return new String(data, 1, data.length - 1, "UTF-16").trim();
            } else if (encoding == 3) {
                return new String(data, 1, data.length - 1, "UTF-8").trim();
            }
        } catch (Exception e) {}
        return "";
    }

    private String readIsoString(byte[] data, int offset, int len) {
        String s = new String(data, offset, len, Charset.forName("ISO-8859-1"));
        int nullPos = s.indexOf(0);
        return nullPos >= 0 ? s.substring(0, nullPos).trim() : s.trim();
    }

    private long decodeSyncsafeInt(byte b1, byte b2, byte b3, byte b4) {
        return ((b1 & 0x7F) << 21) | ((b2 & 0x7F) << 14) | ((b3 & 0x7F) << 7) | (b4 & 0x7F);
    }

    public boolean save() {
        if (!mFile.exists()) return false;
        File tempFile = new File(mFile.getParent(), mFile.getName() + ".tmp");

        try {
            byte[] tagData = buildId3v2Tag();

            RandomAccessFile out = new RandomAccessFile(tempFile, "rw");
            RandomAccessFile in = new RandomAccessFile(mFile, "r");
            try {
                out.write(tagData);
                long audioStart = mHasId3v2 ? mId3v2Size : 0;
                in.seek(audioStart);

                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) > 0) {
                    out.write(buffer, 0, read);
                }
            } finally {
                out.close();
                in.close();
            }

            if (!mFile.delete()) {
                Log.e(TAG, "Failed to delete original file");
                tempFile.delete();
                return false;
            }
            return tempFile.renameTo(mFile);

        } catch (IOException e) {
            Log.e(TAG, "Save error: " + e.getMessage());
            tempFile.delete();
            return false;
        }
    }

    private byte[] buildId3v2Tag() throws IOException {
        java.io.ByteArrayOutputStream frames = new java.io.ByteArrayOutputStream();

        if (mTitle != null && mTitle.length() > 0) {
            frames.write(makeTextFrame("TIT2", mTitle));
        }
        if (mArtist != null && mArtist.length() > 0) {
            frames.write(makeTextFrame("TPE1", mArtist));
        }
        if (mAlbum != null && mAlbum.length() > 0) {
            frames.write(makeTextFrame("TALB", mAlbum));
        }

        byte[] frameBytes = frames.toByteArray();
        int tagSize = frameBytes.length;

        byte[] header = new byte[10];
        header[0] = 'I'; header[1] = 'D'; header[2] = '3';
        header[3] = 3; header[4] = 0;
        header[5] = 0;
        header[6] = (byte) ((tagSize >> 21) & 0x7F);
        header[7] = (byte) ((tagSize >> 14) & 0x7F);
        header[8] = (byte) ((tagSize >> 7) & 0x7F);
        header[9] = (byte) (tagSize & 0x7F);

        byte[] result = new byte[10 + tagSize];
        System.arraycopy(header, 0, result, 0, 10);
        System.arraycopy(frameBytes, 0, result, 10, tagSize);
        return result;
    }

    private byte[] makeTextFrame(String id, String text) throws IOException {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        byte[] textBytes = text.getBytes("UTF-8");

        out.write(id.getBytes("ISO-8859-1"));
        int size = 1 + textBytes.length;
        out.write((size >> 24) & 0xFF);
        out.write((size >> 16) & 0xFF);
        out.write((size >> 8) & 0xFF);
        out.write(size & 0xFF);
        out.write(0);
        out.write(0);
        out.write(3);
        out.write(textBytes);

        return out.toByteArray();
    }
}