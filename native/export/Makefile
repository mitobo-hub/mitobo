include Makefile.rules

.PHONY: all clean install

all:	
	@ if [ `uname -m` = "x86_64" ]; 			\
	  then	\
		echo "\n Building 64-Bit libraries...";	\
	else	\
		echo "\n Building 32-Bit libraries...";	\
	fi;
	$(MAKE) -w -C src -f Makefile all

clean:	
	echo "\n Cleaning all builds..."
	$(MAKE) -w -C src -f Makefile clean
	@ rm -rf lib32 lib64

install: all
	@ echo "\n Installing libraries to $(MITOBO_HOME)/lib ..."
	@ if [ -d "lib32" ]; 			\
	  then							\
		mv lib32/*.so $(MITOBO_HOME)/lib/lib32; \
	fi;
	@ if [ -d "lib64" ]; 			\
	  then							\
		mv lib64/*.so $(MITOBO_HOME)/lib/lib64; \
	fi;
