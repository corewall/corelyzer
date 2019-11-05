include ../config

LDFLAGS = $(LIBRARY_DIR) $(LIBRARIES) $(SCENEGRAPH_LIBRARIES)

ifeq ($(BUILD_LIB_MODE), debug)
    COMPILER_FLAGS += -g -O
    CXXFLAGS += -fPIC -DDEBUG
else
    COMPILER_FLAGS += -O
    CXXFLAGS += -fPIC
endif

CPP     = clang++
CC      = clang
LIBTOOL = libtool

OBJ_FILES = correlator.o
EXEC = libcorrelator.jnilib

pluginName = CorrelatorPlugin

%.o:	src/%.cpp
	@echo "Compile $@"
	$(ENVP) $(CPP) -c -I./include $(COMPILER_FLAGS) $(CXXFLAGS) $(INCLUDE_DIR) -o $@ $<

all: jni $(EXEC) jar
	@echo "Done"

plugin:
	cd src && javac -cp $(CP):../lib/javac2.jar -d ../classes corelyzer/plugin/correlator/*.java && cd ..

jar: jni
#	cp *dylib *jnilib classes
	cd classes && jar -cvmf ../MANIFEST.MF ../../$(pluginName).jar ./* >& /dev/null

jni: plugin
	javah -classpath ./classes -d include corelyzer.plugin.correlator.Correlator

clean:
	cd classes && rm -rf corelyzer com && cd .. && rm -f ../$(pluginName).jar && rm -f *.o *.jnilib

native: jni
	@echo "Compile C++ files"

$(EXEC): $(OBJ_FILES)
	$(ENVP) $(CPP) -g $(DYNFLAG) -o $(EXEC) \
	$(OBJ_FILES) $(LDFLAGS) $(DYN_LOOKUP) -framework Cocoa ./lib/libcore.dylib ./lib/libxerces-c.a
	$(REZ)
	/bin/cp $(EXEC) ../../bin/dyld/

run: all
	/System/Library/Frameworks/JavaVM.framework/Home/bin/java -Xcheck:jni -Dfile.encoding=MacRoman -classpath /System/Library/Frameworks/JavaVM.framework/Versions/1.5.0/Home/lib/deploy.jar:/System/Library/Frameworks/JavaVM.framework/Versions/1.5.0/Classes/dt.jar:/System/Library/Frameworks/JavaVM.framework/Versions/1.5.0/Classes/jce.jar:/System/Library/Frameworks/JavaVM.framework/Versions/1.5.0/Home/lib/plugin.jar:/System/Library/Frameworks/JavaVM.framework/Versions/1.5.0/Home/lib/sa-jdi.jar:/System/Library/Frameworks/JavaVM.framework/Versions/1.5.0/Classes/charsets.jar:/System/Library/Frameworks/JavaVM.framework/Versions/1.5.0/Classes/classes.jar:/System/Library/Frameworks/JavaVM.framework/Versions/1.5.0/Classes/jconsole.jar:/System/Library/Frameworks/JavaVM.framework/Versions/1.5.0/Classes/jsse.jar:/System/Library/Frameworks/JavaVM.framework/Versions/1.5.0/Classes/laf.jar:/System/Library/Frameworks/JavaVM.framework/Versions/1.5.0/Classes/ui.jar:/System/Library/Frameworks/JavaVM.framework/Versions/1.5.0/Home/lib/ext/apple_provider.jar:/System/Library/Frameworks/JavaVM.framework/Versions/1.5.0/Home/lib/ext/dnsns.jar:/System/Library/Frameworks/JavaVM.framework/Versions/1.5.0/Home/lib/ext/localedata.jar:/System/Library/Frameworks/JavaVM.framework/Versions/1.5.0/Home/lib/ext/sunjce_provider.jar:/System/Library/Frameworks/JavaVM.framework/Versions/1.5.0/Home/lib/ext/sunpkcs11.jar:/Users/julian/dev/EVL/corelyzer/branches/corelyzer-db/plugins/CorrelatorPlugin/classes:/Users/julian/dev/EVL/corelyzer/branches/corelyzer-db/bin/Corelyzer.jar:/Users/julian/dev/EVL/corelyzer/branches/corelyzer-db/bin/jars/javac2.jar corelyzer.plugin.correlator.CorrelatorDialog

