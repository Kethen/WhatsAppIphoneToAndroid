all : whatsappi2a.jar

whatsappi2a.jar : MANIFEST.MF Migrator.class MessageItem.class ChatListItem.class com/whatsapp/MediaData.class template.db
	jar -cmf MANIFEST.MF whatsappi2a.jar sqlite.jar template.db Migrator.class MessageItem.class ChatListItem.class com/whatsapp/MediaData.class

Migrator.class : com/whatsapp/MediaData.class Migrator.java MessageItem.class ChatListItem.class
	javac -classpath "sqlite.jar:./"  Migrator.java

MessageItem.class : com/whatsapp/MediaData.class MessageItem.java
	javac -classpath "sqlite.jar:./" MessageItem.java

ChatListItem.class : com/whatsapp/MediaData.class ChatListItem.java
	javac -classpath "sqlite.jar:./" ChatListItem.java

com/whatsapp/MediaData.class : com/whatsapp/MediaData.java
	javac com/whatsapp/MediaData.java

clean :
	rm Migrator.class ChatListItem.class MessageItem.class com/whatsapp/MediaData.class whatsappi2a.jar &
