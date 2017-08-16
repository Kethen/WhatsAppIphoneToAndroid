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
}
