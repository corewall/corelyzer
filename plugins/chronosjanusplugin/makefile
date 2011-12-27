include ../config

plugin:
	cd src && javac -cp $(CP) -d ../classes \
        corelyzer/plugin/ChronosJanusPlugin.java && cd ..
	cd classes && jar -cvmf ../MANIFEST.MF ../../ChronosJanusPlugin.jar corelyzer >& /dev/null && cd ..

clean:
	cd classes && rm -rf corelyzer && cd .. && rm -f ../ChronosJanusPlugin.jar
