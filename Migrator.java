// created by Katharine Chui
// https://github.com/Kethen
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.sql.Types;
import com.whatsapp.MediaData;
import java.io.*;
import java.util.Arrays;
import java.awt.Image;
import java.awt.image.BufferedImage;
public class Migrator{
	public Migrator(){}
	/*db connection android*/
	Connection android;
	/*db connection iphone*/
	Connection iphone;
	File whatsappFolder;
	File iphoneFolder;
	boolean loadIphoneDb(String path){
		try{
			iphone = DriverManager.getConnection("jdbc:sqlite:" + path);
			if(iphone == null){
				System.out.println("failed opening iphone database");
				return false;
			}
		}catch(Exception ex){
			System.out.println("failed opening iphone database");
			System.out.println(ex.getMessage());
			ex.printStackTrace();
		}
		return true;
	}
	boolean loadAndroidDb(String path, String path2){
		try{
			File check = new File(path2);
			if(check.exists()){
				System.out.println("database file " + path2 + " exists!");
				return false;
			}
			FileInputStream dbtemplate = new FileInputStream(path);
			FileOutputStream newdb = new FileOutputStream(path2);
			BufferedInputStream bufferedInFile = new BufferedInputStream(dbtemplate);
			BufferedOutputStream bufferedOutFile = new BufferedOutputStream(newdb);
			byte[] buffer = new byte[1024];
			int readSize = bufferedInFile.read(buffer, 0, 1024);
			while(readSize != -1){
				bufferedOutFile.write(buffer, 0, readSize);
				readSize = bufferedInFile.read(buffer, 0, 1024);
			}
			bufferedOutFile.close();
			android = DriverManager.getConnection("jdbc:sqlite:" + path2);
			if(android == null){
				System.out.println("failed opening android database");
				return false;
			}
			
		}catch(Exception ex){
			System.out.println("failed opening android database");
			System.out.println(ex.getMessage());
			ex.printStackTrace();
		}
		return true;
	}
	boolean closeIphoneDb(){
		try{
			iphone.close();
		}catch(Exception ex){
			System.out.println("error closing iphone database");
			System.out.println(ex.getMessage());
			ex.printStackTrace();
			return false;
		}
		return true;
	}
	boolean closeAndroidDb(){
		try{
			android.close();
		}catch(Exception ex){
			System.out.println("error closing android database");
			System.out.println(ex.getMessage());
			ex.printStackTrace();
			return false;
		}
		return true;
	}
	boolean createAndroidFolder(String path){
		//create the folder under that path then move to that folder
		try{
			whatsappFolder = new File(path + "/WhatsApp");
			whatsappFolder.mkdir();
			new File(whatsappFolder.getAbsolutePath() + "/Media/From iPhone").mkdirs();
			new File(whatsappFolder.getAbsolutePath() + "/Databases").mkdir();
		}catch(Exception ex){
			System.out.println("create folder failed");
			System.out.println(ex.getMessage());
			ex.printStackTrace();
			return false;
		}
		return true;
	}
	boolean openIphoneFolder(String path){
		try{
			iphoneFolder = new File(path + "/Library/Media");
			if(!iphoneFolder.isDirectory()){
				System.out.println("folder does not exist");
				return false;
			}
			iphoneFolder = new File(path + "/Library");
		}catch(Exception ex){
			System.out.println("cannot open iphone folder");
			System.out.println(ex.getMessage());
			ex.printStackTrace();
			return false;
		}
		return true;
	}
	boolean createAndroidDb(String path){
		// create an android database
		try{
			File check = new File(path);
			if(check.exists()){
				System.out.println("database file " + path + " exists!");
				return false;
			}
			InputStream dbTemplate = this.getClass().getClassLoader().getResourceAsStream("template.db");
			FileOutputStream outFile = new FileOutputStream(path);
			BufferedOutputStream bufferedOutFile = new BufferedOutputStream(outFile);
			byte[] copyBuffer = new byte[1024];
			int readSize = dbTemplate.read(copyBuffer, 0, 1024);
			while(readSize != -1){
				bufferedOutFile.write(copyBuffer, 0, readSize);
				readSize = dbTemplate.read(copyBuffer, 0, readSize);
			}
			bufferedOutFile.close();
			android = DriverManager.getConnection("jdbc:sqlite:" + path);
		}catch(Exception ex){
			System.out.println("create database failed!");
			System.out.println(ex.getMessage());
			ex.printStackTrace();
			return false;
		}
		return true;
	}
	boolean iphone2Android(){
		if(iphone == null){
			System.out.println("iphone database not loaded");
			return false;
		}
		if(android == null){
			System.out.println("android database not loaded");
			return false;
		}
		// first work with chat list
		try{	
			Statement sql = iphone.createStatement();
			ResultSet result = sql.executeQuery("SELECT COUNT(Z_PK) AS number FROM ZWACHATSESSION");
			result.next();
			long numberOfSessions = result.getLong("number");
			result.close();
			sql.close();
			sql = iphone.createStatement();
			result = sql.executeQuery("SELECT ZCONTACTJID, ZPARTNERNAME, ZLASTMESSAGEDATE, ZARCHIVED, ZGROUPINFO, ZLASTMESSAGE FROM ZWACHATSESSION");
			long current = 0;
			//String key_remote_jid, String subject, int archived, int sort_timestamp
			System.out.println("begin chatlist migration");
			while(result.next()){
				String jid = result.getString("ZCONTACTJID");
				PreparedStatement sql2 = iphone.prepareStatement("SELECT COUNT(Z_PK) AS number FROM ZWAMESSAGE WHERE (ZFROMJID = ? OR ZTOJID = ?) AND (ZMESSAGETYPE = 0 OR ZMESSAGETYPE = 1 OR ZMESSAGETYPE = 2 OR ZMESSAGETYPE = 3 OR ZMESSAGETYPE = 4 OR ZMESSAGETYPE = 5 OR ZMESSAGETYPE = 8)");
				sql2.setString(1, jid);
				sql2.setString(2, jid);
				ResultSet result2 = sql2.executeQuery();
				result2.next();
				if(result2.getInt("number") != 0){
					String subject = null;
					if(result.getString("ZGROUPINFO") != null){
						subject = result.getString("ZPARTNERNAME");
					}
					int archived = result.getInt("ZARCHIVED");
					long timestamp = MessageItem.nsDateToMilliSecondTimeStamp(result.getFloat("ZLASTMESSAGEDATE"));
					int lastmsg = result.getInt("ZLASTMESSAGE");
					ChatListItem row = new ChatListItem(jid, subject, archived, timestamp, lastmsg);
					if(!row.injectAndroid(android)){
						System.out.println("insert chatlist failed");
						return false;
					}
				}
				current++;
				System.out.print("\r");
				System.out.print("chat sessions added: " + current + "/" + numberOfSessions);
			}
			System.out.println("");
			result.close();
			sql.close();
		}catch(Exception ex){
			System.out.println("insert chatlist failed");
			System.out.println(ex.getMessage());
			ex.printStackTrace();
			return false;
		}
		// then work with messages
		try{
			Statement sql = iphone.createStatement();
			ResultSet result = sql.executeQuery("SELECT COUNT(Z_PK) as number FROM ZWAMESSAGE");
			result.next();
			long numberOfMessage = result.getLong("number");
			result.close();
			sql.close();
			sql = iphone.createStatement();
			result = sql.executeQuery(MessageItem.standardSql + MessageItem.standardSqlAfterWhere);
			// file counter
			int fileCount = 0;
			long current = 0;
			System.out.println("begin message migration");
			while(result.next()){
				int mediaWaType = result.getInt(7/*"ZWAMESSAGE.ZMESSAGETYPE"*//*"ZMESSAGETYPE"*/);
				if(mediaWaType == 0 || mediaWaType == 1 || mediaWaType == 2 || mediaWaType == 3 || mediaWaType == 4 || mediaWaType == 5 || mediaWaType == 8){
					// write the item into the android database
					//public MessageItem(int id, String key_remote_jid, int key_from_me, int timestamp, String media_caption, String media_mime_type, String media_name, String data, int media_wa_type, int media_duration, String remote_resource, byte[] thumb_image)
					//MessageItem message = new MessageItem(id, jid, fromMe, msgDate, mediaCaption, mediaMimeType, mediaName, data, mediaWaType, mediaDuration, remoteResource, thumbImage, longitude, latitude, keyId);
					byte[] thumbImage = null;
					byte[] rawData = null;
					// copy the file
					if(mediaWaType == 8 || mediaWaType == 1 || mediaWaType == 2 || mediaWaType == 3){
						String localMediaPath = result.getString(15/*"ZWAMEDIAITEM.ZMEDIALOCALPATH"*/);
						String fileExtension = null;
						if(localMediaPath != null){
							String[] splitted = localMediaPath.split("\\.");
							if(splitted.length == 0){
								System.out.println("sum ting wong with ZMEDIALOCALPATH");
								System.out.println("ZMEDIALOCALPATH currently is: " + localMediaPath);
								return false;
							}
							fileExtension = splitted[splitted.length - 1];
							FileInputStream inFile = new FileInputStream(iphoneFolder.getAbsolutePath() + "/" + localMediaPath);
							FileOutputStream outFile = new FileOutputStream(whatsappFolder.getAbsolutePath() + "/Media/From iPhone/" + fileCount + "." + fileExtension);
							BufferedInputStream bufferedInFile = new BufferedInputStream(inFile);
							BufferedOutputStream bufferedOutFile = new BufferedOutputStream(outFile);
							byte[] copyBuffer = new byte[1024];
							int readSize = bufferedInFile.read(copyBuffer, 0, 1024);
							while(readSize != -1){
								bufferedOutFile.write(copyBuffer, 0, readSize);
								readSize = bufferedInFile.read(copyBuffer, 0, 1024);
							}
							bufferedInFile.close();
							bufferedOutFile.close();
						}
						// craft a com.whatsapp.MediaData object
						
						MediaData crafted = new MediaData();
						crafted.transferred = true;
						if(localMediaPath != null){
							crafted.file = new File("Media/From Iphone/" + fileCount + "." + fileExtension);
						}else{
							crafted.file = new File("Media/From Iphone/OVERTHERAINBOW");
						}
						crafted.fileSize = result.getInt(14/*"ZWAMEDIAITEM.ZFILESIZE"*/)/*result2.getInt("ZFILESIZE")*/;
						crafted.suspiciousContent = 0;
						if(mediaWaType == 3){
							crafted.faceX = 0;
							crafted.faceY = 0;
						}else{
							crafted.faceX = -1;
							crafted.faceY = -1;
						}
						crafted.mediaKey = new byte[3];
						Arrays.fill(crafted.mediaKey, (byte) 'A');
						crafted.refKey = new byte[3];
						Arrays.fill(crafted.refKey, (byte) 'A');
						crafted.cipherKey = new byte[3];
						Arrays.fill(crafted.cipherKey, (byte) 'A');
						crafted.hmacKey = new byte[3];
						Arrays.fill(crafted.hmacKey, (byte) 'A');
						crafted.iv = new byte[3];
						Arrays.fill(crafted.iv, (byte) 'A');
						crafted.failErrorCode = 0;
						crafted.width = result.getInt(16/*"ZWAMEDIAITEM.ZLONGITUDE"*/)/*result2.getInt("ZLONGITUDE")*/;
						crafted.height = result.getInt(17/*"ZWAMEDIAITEM.ZLATITUDE"*/) /*result2.getInt("ZLATITUDE")*/;
						crafted.doodleId = "Does it really matter?";
						crafted.gifAttribution = 0;
						crafted.thumbnailHeightWidthRatio = crafted.height == 0 ? 0 : crafted.width / crafted.height;
						crafted.uploadRetry = false;
						fileCount++;
						// serialize the object
						ByteArrayOutputStream craftedBuffer = new ByteArrayOutputStream();
						ObjectOutputStream objectOutput = new ObjectOutputStream(craftedBuffer);
						objectOutput.writeObject(crafted);
						objectOutput.close();
						thumbImage = craftedBuffer.toByteArray();
					}
					MessageItem message = new MessageItem();
					if(!message.populateFromResult(iphone, result, 0, true, thumbImage, android, iphoneFolder)){
						System.out.println("loading message failed");
						return false;
					}
					if(message.injectAndroid(android, false) == -1){
						System.out.println("insert message failed");
						return false;
					}
				}
				current++;
				System.out.print("\r");
				System.out.print("messages added: " + current + "/" + numberOfMessage);
			}
			ChatListItem chat = new ChatListItem();
			if(!chat.updateLastMessage(android)){
				System.out.print("failed to update the latest message id into the chat_list table");
				return false;
			}
			System.out.println("\ndone!");
			result.close();
			sql.close();
		}catch(Exception ex){
			System.out.println("insert message failed");
			System.out.println(ex.getMessage());
			ex.printStackTrace();
			return false;
		}
		return true;
	}
	boolean standardFlow(String iphoneDb, String iphoneFolder, String androidFolder){
		return loadIphoneDb(iphoneDb) && openIphoneFolder(iphoneFolder) && createAndroidFolder(androidFolder) && createAndroidDb(androidFolder + "WhatsApp/Databases/msgstore.db") /*loadAndroidDb("template.db", androidFolder + "WhatsApp/Databases/msgstore.db")*/ && iphone2Android() && closeAndroidDb() && closeIphoneDb() ? true : false;
	}
	public static void main(String[] param){
		// register sqlite jdbc driver
		try{
			Class sqliteDriver = Class.forName("org.sqlite.JDBC");
		}catch(Exception ex){
			System.out.println("failed loading sqlite driver");
			System.out.println(ex.getMessage());
			ex.printStackTrace();
		}
		if(param.length != 3){
			System.out.println("Usage: java -jar whatsappi2a.jar <iphone database> <iphone folder (net.whatsapp.WhatsApp)> <android folder output>");
			return;
		}
		Migrator instance = new Migrator();
		instance.standardFlow(param[0], param[1], param[2]);
	}
}
