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
import nl.pvanassen.bplist.converter.ConvertToXml;
import nl.pvanassen.bplist.ext.nanoxml.XMLElement;

public class MessageItem{ // messages <- ZWAMESSAGE
	int id; // _id <- Z_PK
	String key_id;
	String key_remote_jid; // ZTOJID, if null ZFROMJID
	int key_from_me; // ZISFROMME
	//int key_id; // nullable
	int status; // 0 receive 13 sent
	int needs_push; // 0
	long timestamp; // ZMESSAGEDATE, Android: Millisecond Unix, iPhone: NSDate=Unix - 978307200
	String media_caption; // ZTITLE in ZWAMEDIAITEM if ZMEDIAITEM is not null, ZTITLE in ZWAMESSAGEDATAITEM if record exists
	//String media_url; // ZMEDIAURL in in ZWAMEDIAITEM if ZMEDIAITEM is not null
	String media_mime_type; // ZVCARDSTRING in ZWAMEDIAITEM if ZMEDIAITEM is not null and ZVCARDSTRING is not a vcard (starts with BEGIN:)
	String media_name; // ZVCARDNAME if vcard in ZWAMEDIAITEM, ZSUMMARY in ZWAMESSAGEDATAITEM if record exists
	String data; // ZVCARDSTRING in ZWAMEDIAITEM if ZMEDIAITEM is not null and ZVCARDSTRING starts with BEGIN:, otherwise ZTEXT
	int media_wa_type; // audio: 2, contact: 4, image: 1, location: 5(no specimen now), system: 7(???), undefined: 0, video: 3, document: 9
	int media_duration; // ZMOVIEDURATION
	//byte[] media_hash; // doesn't matter
	//String media_name; // null for some reason
	String remote_resource; // ZGROUPMEMBER not null -> ZWAGROUPMEMBER -> ZMEMBERJID
	byte[] thumb_image; // serialized java data, com.whatsapp.MediaDat
	float longitude;
	float latitude;
	int quoted_row_id;
	String mentioned_jids;
	// *** if ZWAMESSAGEDATAITEM record exists, add messages_links record
	public MessageItem(int id, String key_remote_jid, int key_from_me, long timestamp, String media_caption, String media_mime_type, String media_name, String data, int media_wa_type, int media_duration, String remote_resource, byte[] thumb_image, float longitude, float latitude, String key_id, int quoted_row_id, String mentioned_jids){
		status = key_from_me == 1 ? 13 : 0;
		needs_push = 0;
		this.id = id;
		this.key_remote_jid = key_remote_jid;
		this.key_from_me = key_from_me;
		this.timestamp = timestamp;
		this.media_caption = media_caption;
		this.media_mime_type = media_mime_type;
		this.media_name = media_name;
		this.data = data;
		this.media_wa_type = media_wa_type;
		this.media_duration = media_duration;
		this.remote_resource = remote_resource;
		this.thumb_image = thumb_image;
		this.longitude = longitude;
		this.latitude = latitude;
		this.key_id = key_id;
		this.quoted_row_id = quoted_row_id;
		this.mentioned_jids = mentioned_jids;
	}

	public MessageItem(Connection android, ResultSet result, int id){
		String remoteResource = null;
		int groupMember = result.getInt("ZWAGROUPMEMBER.Z_PK"/*"ZGROUPMEMBER"*/);
		PreparedStatement sql2;
		ResultSet result2;
		if(groupMember != 0){
			//sql2 = iphone.prepareStatement("SELECT ZMEMBERJID FROM ZWAGROUPMEMBER WHERE Z_PK = ?");
			//sql2.setInt(1, groupMember);
			//result2 = sql2.executeQuery();
			//if(result2.next()){
				//remoteResource = result2.getString("ZMEMBERJID");
			//}
			//result2.close();
			//sql2.close();
			remoteResource = result.getString("ZWAGROUPMEMBER.ZMEMBERJID");
		}
		long msgDate = nsDateToMilliSecondTimeStamp(result.getFloat("ZWAMESSAGE.ZMESSAGEDATE"/*"ZMESSAGEDATE"*/));
		int fromMe = result.getInt("ZWAMESSAGE.ZISFROMME"/*"ZISFROMME"*/);
		String jid;
		if(fromMe == 1){
			jid = result.getString("ZWAMESSAGE.ZTOJID"/*"ZTOJID"*/);
		}else{
			jid = result.getString("ZWAMESSAGE.ZFROMJID"/*"ZFROMJID"*/);
		}
		String keyId = result.getString("ZWAMESSAGE.ZSTANZAID"/*"ZSTANZAID"*/);
		String data = null;
		String mediaCaption = null;
		String mediaName = null;
		int mediaDuration = 0;
		byte[] thumbImage = null;
		String mediaMimeType = null;
		float longitude = 0;
		float latitude = 0;
		int mediaItemId = result.getInt("ZWAMEDIAITEM.Z_PK"/*"ZMEDIAITEM"*/);
		/*sql2 = iphone.prepareStatement("SELECT Z_PK, ZTITLE, ZSUMMARY FROM ZWAMESSAGEDATAITEM WHERE ZMESSAGE = ?");
		sql2.setInt(1, result.getInt("Z_PK"));
		result2 = sql2.executeQuery();
		// the sent message is a link
		if(result2.next()){*/
		if(result.getInt("ZWAMESSAGEDATAITEM.Z_PK") != 0)
			data = result.getString("ZWAMESSAGE.ZTEXT"/*"ZTEXT"*/);
			mediaCaption = result.getString("ZWAMESSAGEDATAITEM.ZTITLE") /*result2.getString("ZTITLE")*/;
			mediaName = result.getString("ZWAMESSAGEDATAITEM.ZSUMMARY") /*result2.getString("ZSUMMARY")*/;
			//result2.close();
			//sql2.close();
			mediaWaType = -1; // actually for injecting
		}
		// the sent message is just a text message
		else if(mediaWaType == 0){
			result2.close();
			sql2.close();
			data = result.getString("ZWAMESSAGE.ZTEXT"/*"ZTEXT"*/);
		}
		// the sent message is a media
		else if(mediaItemId != 0){
			result2.close();
			sql2.close();
			//sql2 = iphone.prepareStatement("SELECT ZTITLE, ZVCARDSTRING, ZVCARDNAME, ZMOVIEDURATION, ZFILESIZE, ZMEDIALOCALPATH, ZLONGITUDE, ZLATITUDE FROM ZWAMEDIAITEM WHERE Z_PK = ?"); // longitude -> width, latitude -> height
			//sql2.setInt(1, mediaItemId);
			//result2 = sql2.executeQuery();
			//if(result2.next()){
			String vcardString = result.getString("ZWAMEDIAITEM.ZVCARDSTRING") /*result2.getString("ZVCARDSTRING")*/;
			String localMediaPath = result.getString("ZWAMEDIAITEM.ZMEDIALOCALPATH") /*result2.getString("ZMEDIALOCALPATH")*/;
			// the media is a vcard
			if(mediaWaType == 4){
				data = vcardString;
				mediaName = result.getString("ZWAMEDIAITEM.ZVCARDNAME") /*result2.getString("ZVCARDNAME")*/;
				//mediaWaType = 4;
			}
			// the media is a location
			else if(mediaWaType == 5){
				// stub ish, not enough iphone location sample
				data = result.getString("ZWAMESSAGE.ZTEXT"/*"ZTEXT"*/);
				longitude = result.getFloat("ZWAMEDIAITEM.ZLONGITUDE")/*result2.getFloat("ZLONGITUDE")*/;
				latitude = result.getFloat("ZWAMEDIAITEM.ZLATITUDE")/*result2.getFloat("ZLATITUDE")*/;
			}
			// the media is a media
			else{
				// common components
				mediaMimeType = vcardString;
				mediaCaption = result.getString("ZWAMEDIAITEM.ZTITLE")/*result2.getString("ZTITLE")*/;
				mediaDuration = result.getInt("ZWAMEDIAITEM.ZMOVIEDURATION")/*result2.getInt("ZMOVIEDURATION")*/;
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
				crafted.fileSize = result.getInt("ZWAMEDIAITEM.ZFILESIZE")/*result2.getInt("ZFILESIZE")*/;
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
				crafted.width = result.getInt("ZWAMEDIAITEM.ZLONGITUDE")/*result2.getInt("ZLONGITUDE")*/;
				crafted.height = result.getInt("ZWAMEDIAITEM.ZLATITUDE") /*result2.getInt("ZLATITUDE")*/;
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
					String fileName = result.getString("ZWAMESSAGE.ZTEXT"/*"ZTEXT"*/);
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
			//}
		}
		// parse bplist
		int quotedRowId = 0;
		String mentionedJids = null;
		byte[] bplist = result.getBytes("ZWAMEDIAITEM.ZMETADATA");
		if(bplist != null){
			XMLElement 
			// 1. traverse to the first dict element
			// 2. traverse to the first array element
			// 3. traverse to the first dict element
			// 4. traverse through the key elements, check UID element after mentions and quotedMessageData
			// 5. if UID after mentions is not 0
			// 5.1 leave the dict element
			// 5.2 ignore 1 string element
			// 5.3 ignore 1 dict element
			// 5.4 collect all incoming string elements
			// 5.5 set mentioned_jids accordingly
			// 6. if UID after quotedMessageData is not 0
			// 6.1 leave the dict element
			// 6.2 capture first string as the key_id of the message to be clone
			// 6.3 clone the message to messages_quotes
			// 6.4 obtain _id of the cloned message
			// 6.5 set quoted_row_id to _id of the cloned message
		}
		MessageItem(id, jid, fromMe, msgDate, mediaCaption, mediaMimeType, mediaName, data, mediaWaType, mediaDuration, remoteResource, thumbImage, longitude, latitude, keyId, quotedRowId, mentionedJids);
	}
	public boolean injectAndroid(Connection android, boolean quoted){
		try{
			boolean link = false;
			if(media_wa_type == -1){
				link = true;
				media_wa_type = 0;
			}
			String insertStatement;
			if(!quoted){
				insertStatement = "INSERT INTO messages(_id, key_remote_jid, key_from_me, timestamp, media_caption, media_mime_type, media_name, data, media_wa_type, media_duration, remote_resource, thumb_image, needs_push, status, key_id, longitude, latitude, quoted_row_id, mentioned_jids) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
			}else{
				insertStatement = "INSERT INTO messages_quotes(_id, key_remote_jid, key_from_me, timestamp, media_caption, media_mime_type, media_name, data, media_wa_type, media_duration, remote_resource, thumb_image, needs_push, status, key_id, longitude, latitude, quoted_row_id, mentioned_jids) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
			}
			PreparedStatement sql = android.prepareStatement(insertStatement);
			sql.setInt(1, id);
			sql.setString(2, key_remote_jid);
			sql.setInt(3, key_from_me);
			sql.setLong(4, timestamp);
			if(media_caption == null){
				sql.setNull(5, Types.VARCHAR);
			}else{
				sql.setString(5, media_caption);
			}
			if(media_mime_type == null){
				sql.setNull(6, Types.VARCHAR);
			}else{
				sql.setString(6, media_mime_type);
			}
			if(media_name == null){
				sql.setNull(7, Types.VARCHAR);
			}else{
				sql.setString(7, media_name);
			}
			if(data == null){
				sql.setNull(8, Types.VARCHAR);
			}else{
				sql.setString(8, data);
			}
			sql.setInt(9, media_wa_type);
			sql.setInt(10, media_duration);
			if(remote_resource == null){
				sql.setNull(11, Types.VARCHAR);
			}else{
				sql.setString(11, remote_resource);
			}
			if(thumb_image == null){
				sql.setNull(12, Types.BLOB);
			}else{
				sql.setBytes(12, thumb_image);
			}
			sql.setInt(13, needs_push);
			sql.setInt(14, status);
			sql.setString(15, key_id);
			sql.setFloat(16, longitude);
			sql.setFloat(17, latitude);
			if(quoted_row_id != 0){
				sql.setInt(18, quoted_row_id);
			}else{
				sql.setNull(18, Types.INTEGER);
			}
			if(mentioned_jids != NULL){
				sql.setString(19, mentioned_jids);
			}else{
				sql.setNull(19, Types.VARCHAR);
			}
			sql.execute();
			sql.close();
			if(link){
				sql = android.prepareStatement("INSERT INTO messages_links(key_remote_jid, message_row_id) VALUES(?, ?)");
				sql.setString(1, remote_resource == null ? key_remote_jid : remote_resource);
				sql.setInt(2, id);
				sql.execute();
				sql.close();
			}
		}catch(Exception ex){
			System.out.println("failed to inject message " + id + "\nquoted: " + quoted);
			System.out.println(ex.getMessage());
			ex.printStackTrace();
			return false;
		}
		return true;
	}
	// helper functions
	public static long nsDateToMilliSecondTimeStamp(float in){
		return (long) Math.floor(1000 * (in + 978307200));
	}
}
