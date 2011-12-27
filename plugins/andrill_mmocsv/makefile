include ../config

all: plugin server

plugin:
	cd src && \
	javac -d ../classes -cp $(CP) corelyzer/plugin/andrill/*.java \
	&& cd ..
	cd classes && jar -cvmf ../MANIFEST.MF ../../CorelyzerSessionClientPlugin.jar corelyzer >& /dev/null && cd ..

server:
	cd src && \
	javac -d ../classes -cp $(CP) corelyzer/plugin/andrill/*.java \
	&& cd ..
	cd classes && jar -cvf ../CorelyzerSessionServer.jar corelyzer >& /dev/null && cd ..

clean:
	cd classes && rm -rf corelyzer && cd .. && rm -f CorelyzerSessionServer.jar ../CorelyzerSessionClientPlugin.jar

run-server:
	java -cp CorelyzerSessionServer.jar corelyzer.plugin.andrill.CorelyzerSessionServer

run-client:
	java -cp CorelyzerSessionServer.jar corelyzer.plugin.andrill.CorelyzerSessionAdminClient localhost

install:
	cp -f CorelyzerSessionClientPlugin.jar $(CW)/plugins/
#	cp -f CorelyzerSessionClientPlugin.jar ~/


