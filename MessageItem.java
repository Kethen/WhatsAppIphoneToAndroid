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
import java.util.*;
public class MessageItem{ // messages <- ZWAMESSAGE
	long id; // _id <- Z_PK
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
	byte[] thumbnailImage; // for media with files, interestingly thumbnail images are stored in another table. this ia a place holder
	float longitude, width; // longitude, media width
	float latitude, height; // latitude, media height
	long quoted_row_id; // bplist parsing
	String media_url; // if it's a link, put it in media_url from ZWAMESSAGEDATAITEM->ZCONTENT1
	String mentioned_jids; // bplist parsing
	int size; // media size
	String localMediaPath; // path in iphone folder
	MessageItem quote;
	W2ALogInterface log;
	public static int fileCount;
	public static final String standardSql = "SELECT ZWAMESSAGE.ZTOJID, ZWAMESSAGE.ZFROMJID, ZWAMESSAGE.ZISFROMME, ZWAMESSAGE.ZMESSAGEDATE, ZWAMESSAGE.ZTEXT, ZWAMESSAGE.Z_PK, ZWAMESSAGE.ZMESSAGETYPE, ZWAMESSAGE.ZSTANZAID, "
		+
		/*9*/"ZWAMEDIAITEM.Z_PK, ZWAMEDIAITEM.ZTITLE, ZWAMEDIAITEM.ZVCARDSTRING, ZWAMEDIAITEM.ZVCARDNAME, ZWAMEDIAITEM.ZMOVIEDURATION, ZWAMEDIAITEM.ZFILESIZE, ZWAMEDIAITEM.ZMEDIALOCALPATH, ZWAMEDIAITEM.ZLONGITUDE, ZWAMEDIAITEM.ZLATITUDE, "
		+
		/*18*/"ZWAGROUPMEMBER.Z_PK, ZWAGROUPMEMBER.ZMEMBERJID, "
		+
		/*20*/"ZWAMESSAGEDATAITEM.Z_PK, ZWAMESSAGEDATAITEM.ZTITLE, ZWAMESSAGEDATAITEM.ZSUMMARY, "
		+
		/*23*/"ZWAMEDIAITEM.ZMETADATA, ZWAMEDIAITEM.ZXMPPTHUMBPATH, "
		+
		/*25*/"ZWAMESSAGEDATAITEM.ZCONTENT1 "
		+
		"FROM ZWAMESSAGE "
		+
		"LEFT JOIN ZWAMEDIAITEM ON ZWAMESSAGE.ZMEDIAITEM = ZWAMEDIAITEM.Z_PK "
		+
		"LEFT JOIN ZWAGROUPMEMBER ON ZWAMESSAGE.ZGROUPMEMBER = ZWAGROUPMEMBER.Z_PK "
		+
		"LEFT JOIN ZWAMESSAGEDATAITEM ON ZWAMESSAGE.Z_PK = ZWAMESSAGEDATAITEM.ZMESSAGE ";
	public static final String standardSqlAfterWhere = "GROUP BY ZWAMESSAGE.Z_PK ORDER BY ZWAMESSAGE.ZMESSAGEDATE";
	// *** if ZWAMESSAGEDATAITEM record exists, add messages_links record
	public void init(long id, String key_remote_jid, int key_from_me, long timestamp, String media_caption, String media_mime_type, String media_name, String data, int media_wa_type, int media_duration, String remote_resource, byte[] thumb_image, float longitude, float latitude, String key_id, String mentioned_jids, MessageItem quote, byte[] thumbnailImage, String media_url, String localMediaPath, float width, float height, int size){
		status = key_from_me == 1 ? 13 : 0;
		needs_push = 0;
		quoted_row_id = 0;
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
		this.mentioned_jids = mentioned_jids;
		this.quote = quote;
		this.thumbnailImage = thumbnailImage;
		this.media_url = media_url;
		this.localMediaPath = localMediaPath;
		this.width = width;
		this.height = height;
		this.size = size;
	}
	public MessageItem(W2ALogInterface log){
		this.log = log;
	}
	public boolean populateFromResult(Connection iphone, ResultSet result, long id, boolean checkQuoted, Connection android, File iphoneFolder){
		try{
			String remoteResource = null;
			if(result.getInt(18/*("ZWAGROUPMEMBER.Z_PK"*//*"ZGROUPMEMBER"*/) != 0){
				remoteResource = result.getString(19/*"ZWAGROUPMEMBER.ZMEMBERJID"*/);
			}
			long msgDate = nsDateToMilliSecondTimeStamp(result.getFloat(4/*"ZWAMESSAGE.ZMESSAGEDATE"*//*"ZMESSAGEDATE"*/));
			int fromMe = result.getInt(3/*"ZWAMESSAGE.ZISFROMME"*//*"ZISFROMME"*/);
			String jid;
			if(fromMe == 1){
				jid = result.getString(1/*"ZWAMESSAGE.ZTOJID"*//*"ZTOJID"*/);
			}else{
				jid = result.getString(2/*"ZWAMESSAGE.ZFROMJID"*//*"ZFROMJID"*/);
			}
			String keyId = result.getString(8/*"ZWAMESSAGE.ZSTANZAID"*//*"ZSTANZAID"*/);
			String data = null;
			String mediaCaption = null;
			String mediaName = null;
			int mediaDuration = 0;
			String mediaMimeType = null;
			float longitude = 0;
			float latitude = 0;
			String mediaUrl = null;
			int mediaWaType = result.getInt(7/*"ZWAMESSAGE.ZMESSAGETYPE"*/);
			byte[] thumbnail = null;
			float mediaWidth = 0;
			float mediaHeight = 0;
			int mediaSize = 0;
			byte[] thumbImage = null;
			// the sent message is a link
			if(result.getInt(20/*"ZWAMESSAGEDATAITEM.Z_PK"*/) != 0){
				data = result.getString(5/*"ZWAMESSAGE.ZTEXT"*//*"ZTEXT"*/);
				mediaCaption = result.getString(21/*"ZWAMESSAGEDATAITEM.ZTITLE"*/) /*result2.getString("ZTITLE")*/;
				mediaName = result.getString(22/*"ZWAMESSAGEDATAITEM.ZSUMMARY"*/) /*result2.getString("ZSUMMARY")*/;
				mediaWaType = -1; // actually for injecting
				mediaUrl = result.getString(25/*"ZWAMESSAGEDATAITEM.ZCONTENT1"*/);
			}
			// the sent message is just a text message
			else if(mediaWaType == 0){
				data = result.getString(5/*"ZWAMESSAGE.ZTEXT"*//*"ZTEXT"*/);
			}
			// the sent message is a media
			else if(result.getInt(9/*"ZWAMEDIAITEM.Z_PK"*//*"ZMEDIAITEM"*/) != 0){
				String vcardString = result.getString(11/*"ZWAMEDIAITEM.ZVCARDSTRING"*/) /*result2.getString("ZVCARDSTRING")*/;
				// the media is a vcard
				if(mediaWaType == 4){
					data = vcardString;
					mediaName = result.getString(12/*"ZWAMEDIAITEM.ZVCARDNAME"*/) /*result2.getString("ZVCARDNAME")*/;
				}
				// the media is a location
				else if(mediaWaType == 5){
					// stub ish, not enough iphone location sample
					data = result.getString(5/*"ZWAMESSAGE.ZTEXT"*//*"ZTEXT"*/);
					longitude = result.getFloat(16/*"ZWAMEDIAITEM.ZLONGITUDE"*/)/*result2.getFloat("ZLONGITUDE")*/;
					latitude = result.getFloat(17/*"ZWAMEDIAITEM.ZLATITUDE"*/)/*result2.getFloat("ZLATITUDE")*/;
				}
				// the media is a media
				else{
					// common components
					mediaMimeType = vcardString;
					mediaCaption = result.getString(10/*"ZWAMEDIAITEM.ZTITLE"*/)/*result2.getString("ZTITLE")*/;
					mediaDuration = result.getInt(13/*"ZWAMEDIAITEM.ZMOVIEDURATION"*/)/*result2.getInt("ZMOVIEDURATION")*/;
					mediaSize = result.getInt(14/*"ZWAMEDIAITEM.ZFILESIZE"*/)/*result2.getInt("ZFILESIZE")*/;
					
					// file copying and thumimage generation in parent due to number of file tracking
					// media is a document
					if(mediaWaType == 8){
						mediaWaType = 9;
						String fileName = result.getString(5/*"ZWAMESSAGE.ZTEXT"*//*"ZTEXT"*/);
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
						mediaWidth = result.getFloat(16/*"ZWAMEDIAITEM.ZLONGITUDE"*/)/*result2.getFloat("ZLONGITUDE")*/;
						mediaHeight = result.getFloat(17/*"ZWAMEDIAITEM.ZLATITUDE"*/)/*result2.getFloat("ZLATITUDE")*/;
					}
					// media is an image
					else if(vcardString != null && vcardString.indexOf("image") == 0){
						mediaWaType = 1;
						mediaWidth = result.getFloat(16/*"ZWAMEDIAITEM.ZLONGITUDE"*/)/*result2.getFloat("ZLONGITUDE")*/;
						mediaHeight = result.getFloat(17/*"ZWAMEDIAITEM.ZLATITUDE"*/)/*result2.getFloat("ZLATITUDE")*/;
					}
					
				}
				
			}
			// manage thumbnail
			String thumbnailPath = result.getString(24/*ZWAMEDIAITEM.ZXMPPTHUMBPATH*/);
			byte[] copyBuffer = new byte[1024];
			int copySize;
			if(thumbnailPath != null){
				FileInputStream inputFile = new FileInputStream(iphoneFolder.getAbsolutePath() + "/" + thumbnailPath);
				ByteArrayOutputStream inputFileBuffer = new ByteArrayOutputStream();
				while((copySize = inputFile.read(copyBuffer, 0, 1024)) != -1){
					inputFileBuffer.write(copyBuffer, 0, copySize);
				}
				if(mediaWaType == 1 || mediaWaType == 2 || mediaWaType == 3 || mediaWaType == 9){
					// for filed media type, store as byte[] in thumbnail
					thumbnail = inputFileBuffer.toByteArray();
				}else{
					// for non filed types, store ad byte[] in thumb_image
					ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
					ObjectOutputStream objOutputStream = new ObjectOutputStream(outputBuffer);
					objOutputStream.writeObject(inputFileBuffer.toByteArray());
					objOutputStream.close();
					thumbImage = outputBuffer.toByteArray();
				}
			}else{
				if(mediaWaType == 1 || mediaWaType == 2){
					// give a dummy thumbnail to images and videos
					InputStream inputFile = this.getClass().getClassLoader().getResourceAsStream("dummy.jpg");
					ByteArrayOutputStream inputFileBuffer = new ByteArrayOutputStream();
					while((copySize = inputFile.read(copyBuffer, 0, 1024)) != -1){
						inputFileBuffer.write(copyBuffer, 0, copySize);
					}
					thumbnail = inputFileBuffer.toByteArray();
				}
			}
			// parse bplist
			String mentionedJids = null;
			MessageItem quotedMessage = null;
			if(checkQuoted){
				byte[] bplist = result.getBytes(23/*"ZWAMEDIAITEM.ZMETADATA"*/);
				if(bplist != null){
					//log.println("bplist != null");
					ConvertToXml converter = new ConvertToXml();
					XMLElement xml = null;
					try{
						xml = converter.convertToXml(new ByteArrayInputStream(bplist));
					}catch(Exception ex){
						log.println("bad bplist from ZMETADATA x.x");
						return false;
					}

					if(xml != null){
						// 1. traverse to the first dict element
						Iterator<XMLElement> firstLayerFinder = xml.iterateChildren();
						XMLElement firstLayer = null;
						while(firstLayerFinder.hasNext()){
							XMLElement temp = firstLayerFinder.next();
							if(temp.getName().equals("dict")){
								firstLayer = temp;
								break;
							}
						}
						// 2. traverse to the first array element
						if(firstLayer != null){
							XMLElement secondLayer = null;
							Iterator<XMLElement> secondLayerFinder = firstLayer.iterateChildren();
							while(secondLayerFinder.hasNext()){
								XMLElement temp = secondLayerFinder.next();
								if(temp.getName().equals("array")){
									secondLayer = temp;
									break;
								}
							}
						// 3. traverse to the first dict element
							if(secondLayer != null){
								XMLElement thirdLayer = null;
								Iterator<XMLElement> thirdLayerFinder = secondLayer.iterateChildren();
								while(thirdLayerFinder.hasNext()){
									XMLElement temp = thirdLayerFinder.next();
									if(temp.getName().equals("dict")){
										thirdLayer = temp;
										break;
									}
								}
						// 4. traverse through the key elements, check UID element after mentions and quotedMessageData
								if(thirdLayer != null){
									Iterator<XMLElement> keyFinder = thirdLayer.iterateChildren();
									boolean mentions = false;
									boolean quotedMessageData = false;
									while(keyFinder.hasNext()){
										XMLElement temp = keyFinder.next();
										if(temp.getName().equals("key") && temp.getContent().equals("mentions")){
											if(keyFinder.hasNext()){
												temp = keyFinder.next();
												if(temp.getName().equals("UID") && !temp.getContent().equals("0")){
													mentions = true;
												}
											}
										}else if(temp.getName().equals("key") && temp.getContent().equals("quotedMessageData")){
											if(keyFinder.hasNext()){
												temp = keyFinder.next();
												if(temp.getName().equals("UID") && !temp.getContent().equals("0")){
													quotedMessageData = true;
												}
											}
										}
									}
						// 5. leave the dict element, to the first string element after
									if(mentions || quotedMessageData){
										thirdLayer = null;
										while(thirdLayerFinder.hasNext()){
											XMLElement temp = thirdLayerFinder.next();
											if(temp.getName().equals("string")){
												thirdLayer = temp;
												break;
											}
										}
										if(thirdLayer != null && quotedMessageData){
											String quotedMessageKeyId = thirdLayer.getContent();
											PreparedStatement sql = iphone.prepareStatement(
											standardSql
											+
											"WHERE ZWAMESSAGE.ZSTANZAID = ?"
											+
											standardSqlAfterWhere);
											sql.setString(1, quotedMessageKeyId);
											ResultSet result2 = sql.executeQuery();
											if(result2.next()){
												quotedMessage = new MessageItem(log);
												if(!quotedMessage.populateFromResult(iphone, result2, 0, false, android, iphoneFolder)){
													log.println("failed loading quoted message");
													return false;
												}
											}
											// need to clone media it seems
											/*quotedMessage = new MessageItem();
											if(!quotedMessage.cloneFromAndroid(android, quotedMessageKeyId)){
												quotedMessage = null;
											}*/
										}
										if(thirdLayer != null && mentions){
											while(thirdLayerFinder.hasNext()){
												thirdLayer = thirdLayerFinder.next();
												if(thirdLayer.getName().equals("string")){
													if(mentionedJids == null){
														mentionedJids = thirdLayer.getContent();
													}else{
														mentionedJids = mentionedJids + "," + thirdLayer.getContent();
													}
												}
											}
										}
									}
								}
							}
						}
						
						
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
				}
			}
			init(id, jid, fromMe, msgDate, mediaCaption, mediaMimeType, mediaName, data, mediaWaType, mediaDuration, remoteResource, thumbImage, longitude, latitude, keyId, mentionedJids, quotedMessage, thumbnail, mediaUrl, result.getString(15/*"ZWAMEDIAITEM.ZMEDIALOCALPATH"*/), mediaWidth, mediaHeight, mediaSize);
		}catch(Exception ex){
			log.println("failed populating from result set");
			log.println(ex.getMessage());
			ex.printStackTrace();
			return false;
		}
		return true;
	}
	public long injectAndroid(Connection android, boolean quoted, File iphoneFolder, File whatsappFolder){
		try{
			if(media_wa_type == 1 || media_wa_type == 2 || media_wa_type == 3 || media_wa_type == 9 && !quoted){
				// copy the file
				String fileExtension = null;
				if(localMediaPath != null){
					String[] splitted = localMediaPath.split("\\.");
					if(splitted.length == 0){
						log.println("sum ting wong with ZMEDIALOCALPATH");
						log.println("ZMEDIALOCALPATH currently is: " + localMediaPath);
						return -1;
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
				crafted.fileSize = size;
				if(media_wa_type == 3){
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
				crafted.width = (int)width;
				crafted.height = (int)height;
				crafted.doodleId = "Does it really matter?";
				crafted.gifAttribution = 0;
				crafted.thumbnailHeightWidthRatio = crafted.height == 0 ? 0 : crafted.width / crafted.height;
				crafted.uploadRetry = false;
				crafted.suspiciousContent = 0;
				fileCount++;
				// serialize the object
				ByteArrayOutputStream craftedBuffer = new ByteArrayOutputStream();
				ObjectOutputStream objectOutput = new ObjectOutputStream(craftedBuffer);
				objectOutput.writeObject(crafted);
				objectOutput.close();
				thumb_image = craftedBuffer.toByteArray();
			}
			String table;
			if(quoted){
				table = "messages_quotes";
			}else{
				table = "messages";
			}
			if(id == 0){
				Statement sql = android.createStatement();
				ResultSet result = sql.executeQuery("SELECT MAX(_id) FROM " + table);
				if(result.next()){
					id = result.getLong("MAX(_id)") + 1;
				}else{
					id = 2;
				}
			}
			if(quote != null && !quoted){
				quoted_row_id = quote.injectAndroid(android, true, iphoneFolder, whatsappFolder);
				if(quoted_row_id == -1){
					log.println("failed inserting quoted message");
					return -1;
				}
			}
			boolean link = false;
			if(media_wa_type == -1){
				link = true;
				media_wa_type = 0;
			}
			String insertStatement = "INSERT INTO " + table + "(_id, key_remote_jid, key_from_me, timestamp, media_caption, media_mime_type, media_name, data, media_wa_type, media_duration, remote_resource, thumb_image, needs_push, status, key_id, longitude, latitude, quoted_row_id, mentioned_jids, media_url) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

			PreparedStatement sql = android.prepareStatement(insertStatement);
			sql.setLong(1, id);
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
				sql.setLong(18, quoted_row_id);
			}else{
				sql.setNull(18, Types.INTEGER);
			}
			if(mentioned_jids != null){
				sql.setString(19, mentioned_jids);
			}else{
				sql.setNull(19, Types.VARCHAR);
			}
			if(media_url != null){
				sql.setString(20, media_url);
			}else{
				sql.setNull(20, Types.VARCHAR);
			}
			sql.execute();
			sql.close();
			if(link){
				sql = android.prepareStatement("INSERT INTO messages_links(key_remote_jid, message_row_id, link_index) VALUES(?, ?, ?)");
				sql.setString(1, key_remote_jid);
				sql.setLong(2, id);
				sql.setInt(3, 0);
				sql.execute();
				sql.close();
			}
			if(thumbnailImage != null){
				if(quoted){
					sql = android.prepareStatement("UPDATE messages_quotes SET raw_data = ? WHERE key_id = ?");
					sql.setBytes(1, thumbnailImage);
					sql.setString(2, key_id);
					sql.execute();
					sql.close();
				}else{
					sql = android.prepareStatement("INSERT INTO message_thumbnails(thumbnail, timestamp, key_remote_jid, key_from_me, key_id) VALUES(?, ?, ?, ?, ?)");
					sql.setBytes(1, thumbnailImage);
					sql.setLong(2, timestamp);
					sql.setString(3, key_remote_jid);
					sql.setInt(4, key_from_me);
					sql.setString(5, key_id);
					sql.execute();
					sql.close();
				}
			}
		}catch(Exception ex){
			log.println("failed to inject message " + id + "\nquoted: " + quoted + "\nkey_id: " + key_id);
			log.println(ex.getMessage());
			ex.printStackTrace();
			return -1;
		}
		return id;
	}
	public boolean cloneFromAndroid(Connection android, String keyId){
		try{
			PreparedStatement sql = android.prepareStatement("SELECT key_remote_jid, key_from_me, timestamp, media_caption, media_mime_type, media_name, data, media_wa_type, media_duration, remote_resource, thumb_image, longitude, latitude, key_id, mentioned_jids, quoted_row_id, media_url FROM messages WHERE key_id = ?");
			sql.setString(1, keyId);
			ResultSet result = sql.executeQuery();
			if(result.next()){
				init(0, result.getString("key_remote_jid"), result.getInt("key_from_me"), result.getLong("timestamp"), result.getString("media_caption"), result.getString("media_mime_type"), result.getString("media_name"), result.getString("data"), result.getInt("media_wa_type"), result.getInt("media_duration"), result.getString("remote_resource"), result.getBytes("thumb_image"), result.getFloat("longitude"), result.getFloat("latitude"), result.getString("key_id"), result.getString("mentioned_jids"), null, null, result.getString("media_url"), null, 0, 0, 0);
				quoted_row_id = result.getLong("quoted_row_id");
			}else{
				//message probably deleted
				//log.println("clone from android failed");
				//log.println("\ncan't find message, from future perhaps? key_id: " + keyId);
				return false;
			}
			result.close();
			sql.close();
			sql = android.prepareStatement("SELECT thumbnail FROM message_thumbnails WHERE key_id = ?");
			sql.setString(1, keyId);
			result = sql.executeQuery();
			if(result.next()){
				thumbnailImage = result.getBytes("thumbnail");
			}
		}catch(Exception ex){
			log.println("clone from android failed");
			log.println(ex.getMessage());
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
