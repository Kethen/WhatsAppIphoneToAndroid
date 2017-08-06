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
	int sort_timestamp; //ZLASTMESSAGEDATE, Android: Millisecond Unix, iPhone: NSDate=Unix - 978307200
	int my_messages; // =1
	int plaintext_disabled; // =1
	public ChatListItem(String key_remote_jid, String subject, int archived, int sort_timestamp, int last_message_table_id){
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
			if(!result.next()){
				result.close();
				sql.close();
				// insert the newest message
				return true;
			}
			result.close();
			sql.close();
			PreparedStatement newRow = android.prepareStatement("INSERT INTO chat_list(key_remote_jid, subject, creation, archived, sort_timestamp, my_message, plaintext_disabled, last_message_table_id, last_read_message_table_id, last_read_receipt_sent_message_table_id) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
			newRow.setString(1, key_remote_jid);
			if(subject == null){
				newRow.setNull(2, java.sql.Types.VARCHAR);
			}else{
				newRow.setString(2, subject);
			}
			newRow.setInt(3, creation);
			newRow.setInt(4, archived);
			newRow.setInt(5, sort_timestamp);
			newRow.setInt(6, my_messages);
			newRow.setInt(7, plaintext_disabled);
			if(last_message_table_id == 0){
				newRow.setNull(8, Types.INTEGER);
				newRow.setNull(9, Types.INTEGER);
				newRow.setNull(10, Types.INTEGER);
			}else{
				newRow.setInt(8, last_message_table_id);
				newRow.setInt(9, last_message_table_id);
				newRow.setInt(10, last_message_table_id);
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
}