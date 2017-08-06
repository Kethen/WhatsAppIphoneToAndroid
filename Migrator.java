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
			int numberOfSessions = result.getInt("number");
			result.close();
			sql.close();
			sql = iphone.createStatement();
			result = sql.executeQuery("SELECT ZCONTACTJID, ZPARTNERNAME, ZLASTMESSAGEDATE, ZARCHIVED, ZGROUPINFO, ZLASTMESSAGE FROM ZWACHATSESSION");
			int current = 0;
			//String key_remote_jid, String subject, int archived, int sort_timestamp
			System.out.println("begin chatlist migration");
			while(result.next()){
				String jid = result.getString("ZCONTACTJID");
				PreparedStatement sql2 = iphone.prepareStatement("SELECT COUNT(Z_PK) AS number FROM ZWAMESSAGE WHERE ZFROMJID = ? OR ZTOJID = ?");
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
					long timestamp = (long) Math.floor(1000 * (result.getFloat("ZLASTMESSAGEDATE") + 978307200));
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
			int numberOfMessage = result.getInt("number");
			result.close();
			sql.close();
			sql = iphone.createStatement();
			result = sql.executeQuery("SELECT ZTOJID, ZFROMJID, ZISFROMME, ZMESSAGEDATE, ZMEDIAITEM, ZGROUPMEMBER, ZTEXT, Z_PK, ZMESSAGETYPE, ZSTANZAID FROM ZWAMESSAGE");
			// file counter
			int fileCount = 0;
			int current = 0;
			System.out.println("begin message migration");
			while(result.next()){
				// common items
				int id = result.getInt("Z_PK");
				String remoteResource = null;
				int groupMember = result.getInt("ZGROUPMEMBER");
				PreparedStatement sql2;
				ResultSet result2;
				if(groupMember != 0){
					sql2 = iphone.prepareStatement("SELECT ZMEMBERJID FROM ZWAGROUPMEMBER WHERE Z_PK = ?");
					sql2.setInt(1, groupMember);
					result2 = sql2.executeQuery();
					if(result2.next()){
						remoteResource = result2.getString("ZMEMBERJID");
					}
					result2.close();
					sql2.close();
				}
				long msgDate = (long)Math.floor(1000 * (result.getFloat("ZMESSAGEDATE") + 978307200));
				int fromMe = result.getInt("ZISFROMME");
				String jid;
				if(fromMe == 1){
					jid = result.getString("ZTOJID");
				}else{
					jid = result.getString("ZFROMJID");
				}
				String keyId = result.getString("ZSTANZAID");
				String data = null;
				String mediaCaption = null;
				String mediaName = null;
				int mediaDuration = 0;
				byte[] thumbImage = null;
				int mediaWaType = result.getInt("ZMESSAGETYPE");
				if(mediaWaType == 0 || mediaWaType == 1 || mediaWaType == 2 || mediaWaType == 3 || mediaWaType == 4 || mediaWaType == 5 || mediaWaType == 8){
					String mediaMimeType = null;
					float longitude = 0;
					float latitude = 0;
					int mediaItemId = result.getInt("ZMEDIAITEM");
					sql2 = iphone.prepareStatement("SELECT Z_PK, ZTITLE, ZSUMMARY FROM ZWAMESSAGEDATAITEM WHERE ZMESSAGE = ?");
					sql2.setInt(1, result.getInt("Z_PK"));
					result2 = sql2.executeQuery();
					// the sent message is a link
					if(result2.next()){
						data = result.getString("ZTEXT");
						mediaCaption = result2.getString("ZTITLE");
						mediaName = result2.getString("ZSUMMARY");
						result2.close();
						sql2.close();
						mediaWaType = -1; // actually for injecting
					}
					// the sent message is just a text message
					else if(mediaWaType == 0){
						result2.close();
						sql2.close();
						data = result.getString("ZTEXT");
						// potentially use plist here to parse messages_quotes
						// not sure if it's that important tho quotes are repeated messages...
						// need to figure out how quotes work in android first
					}
					// the sent message is a media
					else if(mediaItemId != 0){
						result2.close();
						sql2.close();
						sql2 = iphone.prepareStatement("SELECT ZTITLE, ZVCARDSTRING, ZVCARDNAME, ZMOVIEDURATION, ZFILESIZE, ZMEDIALOCALPATH, ZLONGITUDE, ZLATITUDE FROM ZWAMEDIAITEM WHERE Z_PK = ?"); // longitude -> width, latitude -> height
						sql2.setInt(1, mediaItemId);
						result2 = sql2.executeQuery();
						if(result2.next()){
							String vcardString = result2.getString("ZVCARDSTRING");
							String localMediaPath = result2.getString("ZMEDIALOCALPATH");
							// the media is a vcard
							if(mediaWaType == 4){
								data = vcardString;
								mediaName = result2.getString("ZVCARDNAME");
								//mediaWaType = 4;
							}
							// the media is a location
							else if(mediaWaType == 5){
								// stub ish, not enough iphone location sample
								data = result.getString("ZTEXT");
								longitude = result2.getFloat("ZLONGITUDE");
								latitude = result2.getFloat("ZLATITUDE");
							}
							// the media is a media
							else{
								// common components
								mediaMimeType = vcardString;
								mediaCaption = result2.getString("ZTITLE");
								mediaDuration = result2.getInt("ZMOVIEDURATION");
								// copy the file
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
								crafted.fileSize = result2.getInt("ZFILESIZE");
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
								crafted.width = result2.getInt("ZLONGITUDE");
								crafted.height = result2.getInt("ZLATITUDE");
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
								// media is a document
								if(mediaWaType == 8){
									mediaWaType = 9;
									String fileName = result.getString("ZTEXT");
									mediaName = fileName;
									if(fileName != null){
										String[] splitted;
										splitted = fileName.split("\\.");
										if(splitted.length > 1){
											mediaCaption = fileName.substring(0, fileName.length() - splitted[splitted.length - 1].length() - 1);
										}else{
											mediaCaption = fileName;
										}
									}
								}
								// media is an audio
								else if(vcardString != null && vcardString.indexOf("audio") == 0){
									mediaWaType = 2;
								}
								// media is a video
								else if(vcardString != null && vcardString.indexOf("video") == 0){
									mediaWaType = 3;
								}
								// media is an image
								else if(vcardString != null && vcardString.indexOf("image") == 0){
									mediaWaType = 1;
								}
							}
						}
					}
					// write the item into the android database
					//public MessageItem(int id, String key_remote_jid, int key_from_me, int timestamp, String media_caption, String media_mime_type, String media_name, String data, int media_wa_type, int media_duration, String remote_resource, byte[] thumb_image)
					MessageItem message = new MessageItem(id, jid, fromMe, msgDate, mediaCaption, mediaMimeType, mediaName, data, mediaWaType, mediaDuration, remoteResource, thumbImage, longitude, latitude, keyId);
					
					if(!message.injectAndroid(android)){
						System.out.println("insert message failed");
						return false;
					}
				}
				current++;
				System.out.print("\r");
				System.out.print("messages added: " + current + "/" + numberOfMessage);
			}
			System.out.println("");
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
		if(param.length != 3){
			System.out.println("Usage: java -jar whatsappi2a.jar <iphone database> <iphone folder (net.whatsapp.WhatsApp)> <android folder output>");
			return;
		}
		Migrator instance = new Migrator();
		instance.standardFlow(param[0], param[1], param[2]);
	}
}
