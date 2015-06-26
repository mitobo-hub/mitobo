LIB_DIRS= 	./polygon	\
		./time

.PHONY: all clean 

all:
	@( for dir in $(LIB_DIRS) ;						\
		do								\
			echo "\n Building library in $$dir....";		\
			$(MAKE) -w -C $$dir -f Makefile all;	\
		done ) ;
	
clean:
	@( for dir in $(LIB_DIRS) ;						\
		do								\
			echo "\n Cleaning builds in $$dir....";			\
			$(MAKE) -w -C $$dir -f Makefile clean;	\
		done ) ;
