include ../config

PRIVATE_CLASSPATH = .:../libs/jdom-1.0.jar:../libs/rome-1.0.jar:../libs/glazedlists_java15.jar:../libs/javac2.jar:../../../app/dist/commons-codec-1.2.jar:../../../app/dist/xercesImpl-2.9.1.jar:

plugin:
	echo ${PRIVATE_CLASSPATH}
	cd src && javac -cp $(CP):${PRIVATE_CLASSPATH} -d ../classes \
        corelyzer/plugin/iCores/*.java && cd ..
	cp -R src/corelyzer/plugin/iCores/ui/resources classes/corelyzer/plugin/iCores/ui/
	cd classes && jar -cvmf ../MANIFEST.MF ../../iCores.jar corelyzer >& /dev/null && cd ..

clean:
	cd classes && rm -rf corelyzer && cd .. && rm -f ../iCores.jar

install: plugin
	cp *jar $(CW)/plugins
