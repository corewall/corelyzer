include ../config

PRIVATE_CLASSPATH = .:../libs/psicat.resources.lithologies.jar

plugin:

#brg 1/23/2012: Prefix echo with '@' to prevent double-echo
	@echo [Expected JAR version] ${CORELYZER_VERSION}
	@echo [Base classpath] ${CP}
	@echo [Private classpath] ${PRIVATE_CLASSPATH}
	@echo
	cd src && javac -cp $(CP):${PRIVATE_CLASSPATH} -d ../classes \
        corelyzer/plugin/psicat/*.java && cd ..
	cd classes && jar -cvmf ../MANIFEST.MF ../../psicat.jar corelyzer >& /dev/null && cd ..

clean:
	cd classes && rm -rf corelyzer && cd .. && rm -f ../psicat.jar

install: plugin
	cp *jar $(CW)/plugins
