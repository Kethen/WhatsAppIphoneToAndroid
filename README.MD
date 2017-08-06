Description:

This tool converts iphone ChatStorage.db + net.whatsapp.WhatsApp to android android /sdcard/WhatsApp

This is an Alpha release, I don't garunteer that this will work flawlessly. Please backup your data before trying.

Currently this tool should be about to migrate messages, image, audio, video, contacts, location, documents and links

Output can be used two ways:

1. Wipe WhatsApp app data on anroid phone then activate and restore as backup after putting WhatsApp folder into /sdcard/

2. After putting Whatsapp folder into /sdcard/, push the WhatsApp/Databases/msgstore.db into /data/data/com.whatsapp/databases. This way requires root and some knowledge on linux permissions and selinux

**You only need to do one of the above steps

Build:

1. Download sqlite driver from https://bitbucket.org/xerial/sqlite-jdbc/downloads/, this was written when the 3.20.0 version was the latest. Rename it to sqlite.jar and place it next to Makefile

2. Open a shell, move to the directory containing Makefile, run 

$make

Usage:

$java -jar whatsappi2a.jar <iphone ChatStorage.db path> <iphone net.whatsapp.Whatsapp path> <directory to output the WhatsApp directory> 

**You need sqlite.jar next to whatsappi2a.jar for the sqlite jdbc driver to work (loaded as a library)
