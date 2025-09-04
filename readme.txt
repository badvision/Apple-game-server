Note: This program has a dependancy on the "rxtx" library, found at rxtx.org.
Please visit that site for libraries relevant to your system if you're not using a windows machine.  The windows versions are shipped with this package.  You can also look at the ADT Pro disk transfer utility for some additional tips on getting RXTX working.

1) Install 1.5 or higher JDK (for compiling sources) or JRE (for executing only) from java.sun.com.
  DO NOT USE 1.4 -- IT WILL NOT WORK
2) make sure the java bin directory is part of your path
3) On the apple, type IN#2 (and return) from the basic prompt and leave it alone. (if using a //c, type ctrl-a and press the i key to see what is sent to the apple)
4) from the directory containing the dist and lib directories of a2gameserver, try:
java -jar dist/ags.jar

Edit the settings to match your setup.  Port is either TCP port (6502 for applewin) or Serial port (COM1, /dev/...)

ATTENTION //gs USERS!
There has been an update allowing //gs support -- these settings assume port 2 (modem port) at the standard settings (1200, 8n1).

HOW TO USE THE MENU:
Once you get the games menu on the apple ][, use the following keys:
- A-Z to jump to the part of the list starting with that letter
- Left/Right or [ and ] to go half a screen up and down.  
- On a //e, //c, or //gs, use up and down to move up and down one line.  
- On a legacy ][, use left/right to move up and down a line instead.  
- Press TAB to view more information -- from that view press V to view any screenshots.
- Press ESC to flip between the main screen and the search screen.  TAB selects fields, etc.
- One you've found the game you want to play, press Return to load it.

brendan.robert (a) gmail.com
http://a2gameserver.sourceforge.net
http://brendan.robert.googlepages.com
usenet: comp.sys.apple2, comp.emulators.apple2, comp.sys.apple2.programmer
