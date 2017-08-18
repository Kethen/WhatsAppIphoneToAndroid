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

public class ChatListItem{ // chat_list <- ZWACHATSESSION
	//id auto increment
	String key_remote_jid; // ZCONTACTJID
	String subject; // ZPARTNERNAME 
	int last_message_table_id; // _id of a message
	int creation; // millisecond
	int archived; // null if 0
	long sort_timestamp; //ZLASTMESSAGEDATE, Android: Millisecond Unix, iPhone: NSDate=Unix - 978307200
	int my_messages; // =1
	int plaintext_disabled; // =1
	public ChatListItem(){
		return;
	}
	public ChatListItem(String key_remote_jid, String subject, int archived, long sort_timestamp, int last_message_table_id){
		my_messages = 1;
		plaintext_disabled = 1;
		creation = 0;
		this.key_remote_jid = key_remote_jid;
		this.subject = subject;
		this.creation = creation;
		this.archived = archived;
		this.sort_timestamp = sort_timestamp;
		this.last_message_table_id = last_message_table_id;
	}
	public boolean injectAndroid(Connection android){
		try{
			PreparedStatement sql = android.prepareStatement("SELECT _id FROM chat_list WHERE key_remote_jid = ?");
			sql.setString(1, key_remote_jid);
			ResultSet result = sql.executeQuery();
			if(result.next()){
				result.close();
				sql.close();
				// insert the newest message
				return true;
			}
			result.close();
			sql.close();
			PreparedStatement newRow = android.prepareStatement("INSERT INTO chat_list(key_remote_jid, subject, creation, archived, sort_timestamp, my_messages, plaintext_disabled, last_message_table_id, last_read_message_table_id, last_read_receipt_sent_message_table_id, message_table_id, unseen_message_count, unseen_row_count, unseen_missed_calls_count) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, 0, 0)");
			newRow.setString(1, key_remote_jid);
			if(subject == null){
				newRow.setNull(2, java.sql.Types.VARCHAR);
			}else{
				newRow.setString(2, subject);
			}
			newRow.setInt(3, creation);
			if(archived != 0){
				newRow.setInt(4, archived);
			}else{
				newRow.setNull(4, Types.INTEGER);
			}
			newRow.setLong(5, sort_timestamp);
			newRow.setInt(6, my_messages);
			newRow.setInt(7, plaintext_disabled);
			if(last_message_table_id == 0){
				newRow.setNull(8, Types.INTEGER);
				newRow.setNull(9, Types.INTEGER);
				newRow.setNull(10, Types.INTEGER);
				newRow.setNull(11, Types.INTEGER);
			}else{
				newRow.setInt(8, last_message_table_id);
				newRow.setInt(9, last_message_table_id);
				newRow.setInt(10, last_message_table_id);
				newRow.setInt(11, last_message_table_id);
			}
			newRow.execute();
			newRow.close();
		}catch(Exception ex){
			System.out.println("failed to inject chat session " + subject);
			System.out.println(ex.getMessage());
			ex.printStackTrace();
			return false;
		}
		return true;
	}
	public boolean updateLastMessage(Connection android){
		try{
			PreparedStatement sql = android.prepareStatement("SELECT key_remote_jid FROM chat_list");
			ResultSet result = sql.executeQuery();
			while(result.next()){
				String jid = result.getString("key_remote_jid");
				PreparedStatement sql2 = android.prepareStatement("SELECT MAX(_id), MAX(timestamp), MIN(timestamp) FROM messages WHERE key_remote_jid = ?");
				sql2.setString(1, jid);
				ResultSet result2 = sql2.executeQuery();
				result2.next();
				long latestMsgId = result2.getLong("MAX(_id)");
				long lastMsgTimeStamp = result2.getLong("MAX(timestamp)");
				long firstMsgTimeStamp = result2.getLong("MIN(timestamp)");
				result2.close();
				sql2.close();
				sql2 = android.prepareStatement("UPDATE chat_list SET last_message_table_id = ?, message_table_id = ?, last_read_message_table_id = ?, sort_timestamp = ?, creation = ?, last_read_receipt_sent_message_table_id = ? WHERE key_remote_jid = ?");
				sql2.setLong(1, latestMsgId);
				sql2.setLong(2, latestMsgId);
				sql2.setLong(3, latestMsgId);
				sql2.setLong(4, lastMsgTimeStamp);
				sql2.setLong(5, firstMsgTimeStamp);
				sql2.setLong(6, latestMsgId);
				sql2.setString(7, jid);
				sql2.execute();
				sql2.close();
			}
			result.close();
			sql.close();
		}catch(Exception ex){
			System.out.println("failed to update latest messages");
			return false;
		}
		return true;
	}
}
