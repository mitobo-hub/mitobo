include ../../Makefile.rules

# filenames
LIBRARYFILE	= libJNI_time.so
SOURCEFILE	= ./UserTime.c
OBJECTFILE	= ./UserTime.o

# JNI header directories
INCL_DIR = -I$(JAVA_PATH)/include/ 			\
	       -I$(JAVA_PATH)/include/linux

LIB_DIRS=
LIBS=

.PHONY:	all clean install

all:
	@ if [ `uname -m` = "x86_64" ]; 	\
	  then								\
	    if [ -z "$(LIB32)"]; \
	      then \
	  		g++ -fPIC -frounding-math $(INCL_DIR) -c -o $(OBJECTFILE) $(SOURCEFILE);	\
			g++ -shared -Wl,-soname,$(LIBRARYFILE) -o $(LIBRARYFILE) $(OBJECTFILE) $(LIB_DIRS) $(LIBS); \
		  	mkdir ../../lib64; \
		 	mv $(LIBRARYFILE) ../../lib64; \
	  	  else \
	  	    g++ -m32 -fPIC -frounding-math $(INCL_DIR) -c -o $(OBJECTFILE) $(SOURCEFILE);	\
			g++ -m32 -fPIC -shared -Wl,-soname,$(LIBRARYFILE) -o $(LIBRARYFILE) $(OBJECTFILE) $(LIB_DIRS) $(LIBS); \
	  		mkdir ../../lib32; \
	 		mv $(LIBRARYFILE) ../../lib32; \
	  	  fi;\
	  else								\
		g++ -fPIC -frounding-math -c $(INCL_DIR) -o $(OBJECTFILE) $(SOURCEFILE);	\
	  	g++ -shared -Wl,-soname,$(LIBRARYFILE) -o $(LIBRARYFILE) $(OBJECTFILE) $(LIB_DIRS) $(LIBS);	\
	  	mkdir ../../lib32; \
	 	mv $(LIBRARYFILE) ../../lib32; \
	  fi;
	  
clean:
	@-rm $(LIBRARYFILE) $(OBJECTFILE)

