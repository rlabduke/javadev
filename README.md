This repository contains the various Java-based projects from the Richardsons' lab at Duke University.  It contains archives of work dating back to the early 2000s so not all of it is still currently relevant.

The currently most used program is the KiNG visualization software. The main KiNG code is in the king/ directory, but important supplemental plugins for KiNG are in chiropraxis/, extratools/, molikin/, and rdcvis/.  

Note that several pre-built versions of KiNG are available as "Releases" in this repository. For mac users, your computer may report that the download is "damaged".  This is due to a security setting on the file, in order to get it to run, you may have to run the following command in the Terminal program on the installed King.app (substitute the appropriate path for where you installed the program, usually /Applications/).

sudo xattr -r -d com.apple.quarantine /path/to/KiNG.app
