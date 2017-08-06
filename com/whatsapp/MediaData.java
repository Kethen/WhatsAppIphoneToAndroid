// created by Katharine Chui
// https://github.com/Kethen
package com.whatsapp;

import java.io.File;
import java.io.Serializable;

//debug
import java.io.*;
import java.util.*;

public class MediaData
implements Serializable {
    public static int a = 0;
    public static int b = 1;
    public static int c = 2;
    public static int d = 3;
    private static final long serialVersionUID = -3211751283609594L;
    public boolean autodownloadRetryEnabled;
    public long cachedDownloadedBytes;
    public byte[] cipherKey;
    public String doodleId;
    public transient boolean e = false;
    public transient boolean f = false;
    public int faceX;
    public int faceY;
    public int failErrorCode;
    public File file;
    public long fileSize;
    //public transient xr g;
    public int gifAttribution;
    //public transient vu h;
    public boolean hasStreamingSidecar;
    public int height;
    public byte[] hmacKey;
    //public transient xq i;
    public byte[] iv;
    public transient boolean j;
    public transient boolean k;
    public transient boolean l;
    public byte[] mediaKey;
    public long progress;
    public byte[] refKey;
    public boolean showDownloadedBytes;
    public int suspiciousContent;
    public float thumbnailHeightWidthRatio;
    public boolean transcoded;
    public boolean transferred = false;
    public long trimFrom;
    public long trimTo;
    public boolean uploadRetry;
    public String uploadUrl;
    public int width;

    public MediaData() {}
    public final MediaData clone() {
        MediaData localMediaData = new MediaData();
        localMediaData.transferred = this.transferred;
        localMediaData.progress = this.progress;
        localMediaData.l = this.l;
        localMediaData.file = this.file;
        localMediaData.fileSize = this.fileSize;
        localMediaData.autodownloadRetryEnabled = this.autodownloadRetryEnabled;
        localMediaData.transcoded = this.transcoded;
        localMediaData.suspiciousContent = this.suspiciousContent;
        localMediaData.trimFrom = this.trimFrom;
        localMediaData.trimTo = this.trimTo;
        localMediaData.faceX = this.faceX;
        localMediaData.faceY = this.faceY;
        localMediaData.mediaKey = this.mediaKey;
        localMediaData.refKey = this.refKey;
        localMediaData.cipherKey = this.cipherKey;
        localMediaData.hmacKey = this.hmacKey;
        localMediaData.iv = this.iv;
        localMediaData.uploadUrl = this.uploadUrl;
        localMediaData.failErrorCode = this.failErrorCode;
        localMediaData.width = this.width;
        localMediaData.height = this.height;
        localMediaData.doodleId = this.doodleId;
        localMediaData.gifAttribution = this.gifAttribution;
        localMediaData.thumbnailHeightWidthRatio = this.thumbnailHeightWidthRatio;
        localMediaData.uploadRetry = this.uploadRetry;
        return localMediaData;
    }
    public MediaData(MediaData paramMediaData) {
	this.transferred = paramMediaData.transferred;
	this.file = paramMediaData.file;
	this.fileSize = paramMediaData.fileSize;
	this.suspiciousContent = paramMediaData.suspiciousContent;
	this.faceX = paramMediaData.faceX;
	this.faceY = paramMediaData.faceY;
	this.mediaKey = paramMediaData.mediaKey;
	this.refKey = paramMediaData.refKey;
	this.cipherKey = paramMediaData.cipherKey;
	this.hmacKey = paramMediaData.hmacKey;
	this.iv = paramMediaData.iv;
	this.failErrorCode = paramMediaData.failErrorCode;
	this.width = paramMediaData.width;
	this.height = paramMediaData.height;
	this.doodleId = paramMediaData.doodleId;
	this.gifAttribution = paramMediaData.gifAttribution;
	this.thumbnailHeightWidthRatio = paramMediaData.thumbnailHeightWidthRatio;
	this.uploadRetry = paramMediaData.uploadRetry;
    }
	// debug, trying to load a specimen then print it
    public void printMe() {
	System.out.println("transferred: " + transferred);
	System.out.println("relative path: " + file.getPath());
	System.out.println("absolute path: " + file.getAbsolutePath());
	System.out.println("file :" + file.toString());
	System.out.println("fileSize :" + fileSize);
	System.out.println("suspiciousContent :" + suspiciousContent);
	System.out.println("faceX :" + faceX);
	System.out.println("faceY :" + faceY);
	System.out.println("mediaKey :" + mediaKey);
	System.out.println("refKey :" + refKey);
	System.out.println("cipherKey :" + cipherKey);
	System.out.println("hmacKey :" + hmacKey);
	System.out.println("iv :" + iv);
	System.out.println("failErrorCode :" + failErrorCode);
	System.out.println("width :" + width);
	System.out.println("height :" + height);
	System.out.println("doodleId :" + doodleId);
	System.out.println("gifAttribution :" + gifAttribution);
	System.out.println("thumbnailHeightWidthRatio :" + thumbnailHeightWidthRatio);
	System.out.println("uploadRetry :" + uploadRetry);
    }
    public static void main(String[] param) {
	MediaData testread;
	if(param.length == 0){
		System.out.println("no input file");
	}else{
		try {
			InputStream file = new FileInputStream(param[0]);
			InputStream buffer = new BufferedInputStream(file);
			ObjectInput input = new ObjectInputStream(buffer);
			testread = (MediaData)input.readObject();
			testread.printMe();
		} catch (Exception ex) {
			System.out.println(ex.getMessage());
			ex.printStackTrace();
			return;
		}
	}
        // create a custom version of the same thing, trying to manupulate the path and see if that works after shoving into another field
        testread = new MediaData();
        testread.transferred = true;
        testread.file = new File("Media/From Iphone/0.jpg");
        testread.fileSize = 999;
        testread.suspiciousContent = 0;
        testread.faceX = -1; // 0 for videos
        testread.faceY = -1; // 0 for videos
        testread.mediaKey = new byte[3];
        Arrays.fill(testread.mediaKey, (byte) 'A');
        testread.refKey = new byte[3];
        Arrays.fill(testread.refKey, (byte) 'A');
        testread.cipherKey = new byte[3];
        Arrays.fill(testread.cipherKey, (byte) 'A');
        testread.hmacKey = new byte[3];
        Arrays.fill(testread.hmacKey, (byte) 'A');
        testread.iv = new byte[3];
        Arrays.fill(testread.iv, (byte) 'A');
        testread.failErrorCode = 0;
        testread.width = 999;
        testread.height = 999;
        testread.doodleId = "Does it really matter?";
        testread.gifAttribution = 0;
        testread.thumbnailHeightWidthRatio = 9.9F;
        testread.uploadRetry = false;
        try{
		OutputStream file = new FileOutputStream("new.jobj");
		OutputStream buffer = new BufferedOutputStream(file);
		ObjectOutput output = new ObjectOutputStream(buffer);
		output.writeObject(testread);
		output.close();
        }catch (Exception ex){
		System.out.println(ex.getMessage());
		ex.printStackTrace();
        }
        try{
		InputStream file = new FileInputStream("test2.jobj");
		InputStream buffer = new BufferedInputStream(file);
		ObjectInput input = new ObjectInputStream(buffer);
		Object o = input.readObject();
		System.out.println(o instanceof String);
        }catch(Exception ex){
		System.out.println(ex.getMessage());
		ex.printStackTrace();
        }
    }
}
