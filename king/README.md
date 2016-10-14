#KiNG source quick guide#
##Building KiNG from source##
KiNG uses Apache Ant to automate the building process. Use `ant build` in the KiNG directory to build the base king.jar file you need to run KiNG. The molikin code gets automatically built and included in the king.jar file. Many of the tools used in KiNG are in other javadev directories. The `chiropraxis` directory includes the model manager/sidechain rotator/sidechain mutator/backrub tools. Run `ant build` in the chiropraxis directory to build the chiropraxis.jar file you need for those tools. The `extratools` directory includes many other KiNG tools. Run `ant build` in the extratools directory to build extratools.jar. 
##Running KiNG##
After building the jar files, there are two easy choices to run KiNG so it includes the plugin jars. 
1. Copy the chiropraxis.jar and extratool.jar files into a plugins directory that is in the same directory as the king.jar file. Then you can do `java -jar king.jar` and it should automatically incorporate all the tools in the plugin directory.
2. Include the jar file locations in the classpath in a command used to run KiNG such as the following: `java -cp "[directory]/javadev/king/king.jar:[directory]/javadev/chiropraxis/chiropraxis.jar:[directory]/javadev/extratools/extratools.jar" king.KingMain`.
