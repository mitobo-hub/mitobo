# global variable settings
export SHELL		:= /bin/sh

export SOURCE 		:= $(shell pwd)

# sub-directories
export DIRS:= 			manual/tex

.PHONY:		$(DIRS)

# targets
all:   
	   ( for module in $(DIRS) ; 							\
	 	  do												\
			echo "\nProcessing directory $$module....";		\
			$(MAKE) -w -C $$module -f Makefile $@;			\
		  done ) ;

