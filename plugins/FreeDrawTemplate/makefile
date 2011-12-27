include ../config

plugin:
	cd src && javac -cp $(CP):$(CW)/bin/jars/javac2.jar -d ../classes \
        corelyzer/plugin/*.java && cd ..
	cd classes && jar -cvmf ../MANIFEST.MF ../../FreeDrawTemplate.jar corelyzer >& /dev/null && cd ..

clean:
	cd classes && rm -rf corelyzer && cd .. && rm -f ../FreeDrawTemplate.jar

install: plugin
	cp *jar $(CW)/plugins
