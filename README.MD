Description:

This tool converts iphone ChatStorage.db + net.whatsapp.WhatsApp to android android /sdcard/WhatsApp

This is an Alpha release, I don't garunteer that this will work flawlessly. Please backup your data before trying.


What works:

Currently this tool should be able to migrate 


1. messages

2. active chat listing

3. image

4. audio

5. video

6. contacts

7. location

8. documents

9. links

10. recent mentions

11. quotes


What doesn't:

Some older mentions can't be moved from iPhone, will be displaying as @phonenumber. Still investigating changes in the ZWAMEDIAITEM.ZMETADATA bplist

**Due to recent changes on WhatsApp's end, chat groups' information are retrived from wa.db instead of msgstore.db, while restoring backup does not copy group info from msgstore.db to wa.db. Even with an official local backup, chat groups that you have left from or was removed from gets a subject of 'unknown subject'. If you have a rooted phone, you might be able to update records in wa.db to restore those name. Will further look into that since WhatsApp might fix this bug themselves.

Output can be used in the following way:

1. Wipe WhatsApp app data on anroid phone then restore the output as a backup after putting WhatsApp folder into /sdcard/

**inserting msgstore.db into /data directly proves to make whatsapp unstable


Build:

	$make


Usage:

$java -jar whatsappi2a.jar \<iphone ChatStorage.db path\> \<iphone net.whatsapp.Whatsapp path\> \<directory to output the WhatsApp directory\> 

Open Source Software used in this project:

java-bplist : https://github.com/pvanassen/java-bplist

sqlite-jdbc : https://github.com/xerial/sqlite-jdbc
