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
			/*Statement sql = android.createStatement();
			String newdbstring = "CREATE TABLE messages (_id INTEGER PRIMARY KEY AUTOINCREMENT, key_remote_jid TEXT NOT NULL, key_from_me INTEGER, key_id TEXT NOT NULL, status INTEGER, needs_push INTEGER, data TEXT, timestamp INTEGER, media_url TEXT, media_mime_type TEXT, media_wa_type TEXT, media_size INTEGER, media_name TEXT, media_caption TEXT, media_hash TEXT, media_duration INTEGER, origin INTEGER, latitude REAL, longitude REAL, thumb_image TEXT, remote_resource TEXT, received_timestamp INTEGER, send_timestamp INTEGER, receipt_server_timestamp INTEGER, receipt_device_timestamp INTEGER, read_device_timestamp INTEGER, played_device_timestamp INTEGER, raw_data BLOB, recipient_count INTEGER, participant_hash TEXT, starred INTEGER, quoted_row_id INTEGER, mentioned_jids TEXT, multicast_id TEXT, edit_version INTEGER, media_enc_hash TEXT); CREATE UNIQUE INDEX messages_key_index on messages (key_remote_jid, key_from_me, key_id); CREATE INDEX messages_jid_id_index on messages (key_remote_jid, _id); CREATE INDEX media_hash_index on messages (media_hash); CREATE INDEX media_type_index on messages (media_wa_type); CREATE INDEX media_type_jid_index on messages (key_remote_jid, media_wa_type); CREATE INDEX starred_index on messages (starred); CREATE TABLE chat_list (_id INTEGER PRIMARY KEY AUTOINCREMENT, key_remote_jid TEXT UNIQUE, message_table_id INTEGER, subject TEXT, creation INTEGER, last_read_message_table_id INTEGER, last_read_receipt_sent_message_table_id INTEGER, archived INTEGER, sort_timestamp INTEGER, mod_tag INTEGER, gen REAL, my_messages INTEGER, plaintext_disabled BOOLEAN, last_message_table_id INTEGER, unseen_message_count INTEGER, unseen_missed_calls_count INTEGER, unseen_row_count INTEGER, vcard_ui_dismissed INTEGER); CREATE TABLE props (_id INTEGER PRIMARY KEY AUTOINCREMENT, key TEXT UNIQUE, value TEXT); CREATE TABLE 'messages_fts_content'(docid INTEGER PRIMARY KEY, 'c0content'); CREATE TABLE 'messages_fts_segments'(blockid INTEGER PRIMARY KEY, block BLOB); CREATE TABLE 'messages_fts_segdir'(level INTEGER,idx INTEGER,start_block INTEGER,leaves_end_block INTEGER,end_block INTEGER,root BLOB,PRIMARY KEY(level, idx)); CREATE TABLE messages_quotes (_id INTEGER PRIMARY KEY AUTOINCREMENT, key_remote_jid TEXT NOT NULL, key_from_me INTEGER, key_id TEXT NOT NULL, status INTEGER, needs_push INTEGER, data TEXT, timestamp INTEGER, media_url TEXT, media_mime_type TEXT, media_wa_type TEXT, media_size INTEGER, media_name TEXT, media_caption TEXT, media_hash TEXT, media_duration INTEGER, origin INTEGER, latitude REAL, longitude REAL, thumb_image TEXT, remote_resource TEXT, received_timestamp INTEGER, send_timestamp INTEGER, receipt_server_timestamp INTEGER, receipt_device_timestamp INTEGER, read_device_timestamp INTEGER, played_device_timestamp INTEGER, raw_data BLOB, recipient_count INTEGER, participant_hash TEXT, starred INTEGER, quoted_row_id INTEGER, mentioned_jids TEXT, multicast_id TEXT, edit_version INTEGER, media_enc_hash TEXT); CREATE TABLE messages_vcards (_id INTEGER PRIMARY KEY AUTOINCREMENT, message_row_id INTEGER, sender_jid TEXT, vcard TEXT); CREATE TABLE messages_vcards_jids (_id INTEGER PRIMARY KEY AUTOINCREMENT, message_row_id INTEGER, vcard_jid TEXT, vcard_row_id INTEGER); CREATE TABLE messages_edits (_id INTEGER PRIMARY KEY AUTOINCREMENT, key_remote_jid TEXT NOT NULL, key_from_me INTEGER, key_id TEXT NOT NULL, status INTEGER, needs_push INTEGER, data TEXT, timestamp INTEGER, media_url TEXT, media_mime_type TEXT, media_wa_type TEXT, media_size INTEGER, media_name TEXT, media_caption TEXT, media_hash TEXT, media_duration INTEGER, origin INTEGER, latitude REAL, longitude REAL, thumb_image TEXT, remote_resource TEXT, received_timestamp INTEGER, send_timestamp INTEGER, receipt_server_timestamp INTEGER, receipt_device_timestamp INTEGER, read_device_timestamp INTEGER, played_device_timestamp INTEGER, raw_data BLOB, recipient_count INTEGER, participant_hash TEXT, starred INTEGER, quoted_row_id INTEGER, mentioned_jids TEXT, multicast_id TEXT, edit_version INTEGER, media_enc_hash TEXT); CREATE TABLE messages_links (_id INTEGER PRIMARY KEY AUTOINCREMENT, key_remote_jid TEXT, message_row_id INTEGER, link_index INTEGER); CREATE TABLE frequents (_id INTEGER PRIMARY KEY AUTOINCREMENT, jid TEXT NOT NULL, type INTEGER NOT NULL, message_count INTEGER NOT NULL); CREATE TABLE receipts (_id INTEGER PRIMARY KEY AUTOINCREMENT, key_remote_jid TEXT NOT NULL, key_id TEXT NOT NULL, remote_resource TEXT, receipt_device_timestamp INTEGER, read_device_timestamp INTEGER, played_device_timestamp INTEGER); CREATE INDEX receipts_key_index on receipts (key_remote_jid, key_id); CREATE TABLE group_participants (_id INTEGER PRIMARY KEY AUTOINCREMENT, gjid TEXT NOT NULL, jid TEXT NOT NULL, admin INTEGER, pending INTEGER, sent_sender_key INTEGER); CREATE UNIQUE INDEX group_participants_index on group_participants (gjid, jid); CREATE TABLE group_participants_history (_id INTEGER PRIMARY KEY AUTOINCREMENT, timestamp DATETIME NOT NULL, gjid TEXT NOT NULL, jid TEXT NOT NULL, action INTEGER NOT NULL, old_phash TEXT NOT NULL, new_phash TEXT NOT NULL); CREATE INDEX group_participants_history_index on group_participants_history (gjid); CREATE TABLE media_refs (_id INTEGER PRIMARY KEY AUTOINCREMENT, path TEXT UNIQUE, ref_count INTEGER); CREATE TABLE media_streaming_sidecar (_id INTEGER PRIMARY KEY AUTOINCREMENT, sidecar BLOB, timestamp DATETIME, key_remote_jid TEXT NOT NULL, key_from_me INTEGER, key_id TEXT NOT NULL); CREATE TABLE message_thumbnails (thumbnail BLOB, timestamp DATETIME, key_remote_jid TEXT NOT NULL, key_from_me INTEGER, key_id TEXT NOT NULL); CREATE UNIQUE INDEX messages_thumbnail_key_index on message_thumbnails (key_remote_jid, key_from_me, key_id); CREATE TABLE status_list (_id INTEGER PRIMARY KEY AUTOINCREMENT, key_remote_jid TEXT UNIQUE, message_table_id INTEGER, last_read_message_table_id INTEGER, last_read_receipt_sent_message_table_id INTEGER, first_unread_message_table_id INTEGER, autodownload_limit_message_table_id INTEGER, timestamp INTEGER, unseen_count INTEGER, total_count INTEGER); CREATE TABLE conversion_tuples (key_remote_jid TEXT PRIMARY KEY, data TEXT, source TEXT, last_interaction INTEGER, first_interaction INTEGER NOT NULL);";
			String[] splitted = newdbstring.split(";");
			for(int i = 0;i < splitted.length;i++){
				sql.execute(splitted[i]);
			}
			sql.close();*/
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
					// copy the file
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
					byte[] thumbImage = null;
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
					MessageItem message = new MessageItem();
					if(!message.populateFromResult(iphone, result, 0, true, thumbImage)){
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
			System.out.println("done!");
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
