include ../../Makefile.rules

# filenames
LIBRARYFILE	= libJNI_Cgal_Polygon2D.so
SOURCEFILE	= ./Polygon2D_Cgal.cxx
OBJECTFILE	= ./Polygon2D_Cgal.o

# JNI and CGAL header directories
INCL_DIR = -I$(JAVA_PATH)/include/ 			\
	       -I$(JAVA_PATH)/include/linux		\
	       -I$(CGAL_HEADERS_PATH)

# directory of CGAL libraries
LIB_DIRS = -L$(CGAL_LIBRARY_PATH)

LIBS = -lCGAL \
	   -lCGAL_Core

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
	@-rm -f $(LIBRARYFILE) $(OBJECTFILE)

install: all









