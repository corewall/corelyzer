include config

plugins = iCores

all:
	@for OBJ in ${plugins}; do \
		echo "---> Building $${OBJ}";      \
		cd $${OBJ} && make && cd ..;       \
	done

clean:
	@for OBJ in ${plugins}; do \
		echo "---> Cleaning $${OBJ}";      \
		cd $${OBJ} && make clean && cd ..; \
	done
