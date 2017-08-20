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
	public Migrator(W2ALogInterface log){
		this.log = log;
	}
	/*db connection android*/
	Connection android;
	/*db connection iphone*/
	Connection iphone;
	File whatsappFolder;
	File iphoneFolder;
	W2ALogInterface log;
	boolean loadIphoneDb(String path){
		try{
			iphone = DriverManager.getConnection("jdbc:sqlite:" + path);
			if(iphone == null){
				log.println("failed opening iphone database");
				return false;
			}
		}catch(Exception ex){
			log.println("failed opening iphone database");
			log.println(ex.getMessage());
			ex.printStackTrace();
		}
		return true;
	}
	boolean loadAndroidDb(String path, String path2){
		try{
			File check = new File(path2);
			if(check.exists()){
				log.println("database file " + path2 + " exists!");
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
				log.println("failed opening android database");
				return false;
			}
			
		}catch(Exception ex){
			log.println("failed opening android database");
			log.println(ex.getMessage());
			ex.printStackTrace();
		}
		return true;
	}
	boolean closeIphoneDb(){
		try{
			iphone.close();
		}catch(Exception ex){
			log.println("error closing iphone database");
			log.println(ex.getMessage());
			ex.printStackTrace();
			return false;
		}
		return true;
	}
	boolean closeAndroidDb(){
		try{
			android.close();
		}catch(Exception ex){
			log.println("error closing android database");
			log.println(ex.getMessage());
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
			log.println("create folder failed");
			log.println(ex.getMessage());
			ex.printStackTrace();
			return false;
		}
		return true;
	}
	boolean openIphoneFolder(String path){
		try{
			iphoneFolder = new File(path + "/Library/Media");
			if(!iphoneFolder.isDirectory()){
				log.println("folder does not exist");
				return false;
			}
			iphoneFolder = new File(path + "/Library");
		}catch(Exception ex){
			log.println("cannot open iphone folder");
			log.println(ex.getMessage());
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
				log.println("database file " + path + " exists!");
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
			log.println("create database failed!");
			log.println(ex.getMessage());
			ex.printStackTrace();
			return false;
		}
		return true;
	}
	boolean iphone2Android(){
		if(iphone == null){
			log.println("iphone database not loaded");
			return false;
		}
		if(android == null){
			log.println("android database not loaded");
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
			result = sql.executeQuery(ChatListItem.standardsql);
			long current = 0;
			//String key_remote_jid, String subject, int archived, int sort_timestamp
			log.println("begin chatlist migration");
			while(result.next()){
				String jid = result.getString(1/*"ZCONTACTJID"*/);
				PreparedStatement sql2 = iphone.prepareStatement("SELECT COUNT(Z_PK) AS number FROM ZWAMESSAGE WHERE (ZFROMJID = ? OR ZTOJID = ?) AND (ZMESSAGETYPE = 0 OR ZMESSAGETYPE = 1 OR ZMESSAGETYPE = 2 OR ZMESSAGETYPE = 3 OR ZMESSAGETYPE = 4 OR ZMESSAGETYPE = 5 OR ZMESSAGETYPE = 8)");
				sql2.setString(1, jid);
				sql2.setString(2, jid);
				ResultSet result2 = sql2.executeQuery();
				result2.next();
				if(result2.getInt("number") != 0){
					ChatListItem row = new ChatListItem(log);
					if(!row.populateFromResult(result)){
						log.println("failed loading chatlist");
						return false;
					}
					if(!row.injectAndroid(android)){
						log.println("insert chatlist failed");
						return false;
					}
				}
				current++;
				log.print("\r");
				log.print("chat sessions added: " + current + "/" + numberOfSessions);
			}
			log.println("");
			result.close();
			sql.close();
		}catch(Exception ex){
			log.println("insert chatlist failed");
			log.println(ex.getMessage());
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
			MessageItem.fileCount = 0;
			long current = 0;
			log.println("begin message migration");
			while(result.next()){
				int mediaWaType = result.getInt(7/*"ZWAMESSAGE.ZMESSAGETYPE"*//*"ZMESSAGETYPE"*/);
				if(mediaWaType == 0 || mediaWaType == 1 || mediaWaType == 2 || mediaWaType == 3 || mediaWaType == 4 || mediaWaType == 5 || mediaWaType == 8){
					MessageItem message = new MessageItem(log);
					if(!message.populateFromResult(iphone, result, 0, true, android, iphoneFolder)){
						log.println("loading message failed");
						return false;
					}
					if(message.injectAndroid(android, false, iphoneFolder, whatsappFolder) == -1){
						log.println("insert message failed");
						return false;
					}
				}
				current++;
				log.print("\r");
				log.print("messages added: " + current + "/" + numberOfMessage);
			}
			ChatListItem chat = new ChatListItem(log);
			if(!chat.updateLastMessage(android)){
				log.print("failed to update the latest message id into the chat_list table");
				return false;
			}
			log.println("\ndone!");
			result.close();
			sql.close();
		}catch(Exception ex){
			log.println("insert message failed");
			log.println(ex.getMessage());
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
		Migrator instance = new Migrator(new W2ALogInterface());
		instance.standardFlow(param[0], param[1], param[2]);
	}
}
