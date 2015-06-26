#! /bin/sh

# examples: 
#	sh oprunner.sh -r '.\*'
#	sh oprunner.sh GaussFilter inputImg=yourimage.tif resultImg=result.tif
#	sh oprunner.sh ImgThresh inputImage=yourimage.tif  threshold=100 resultImage=binary.tif


## set this variable to the directory where you unpacked the Mi_To_Bo.zip
MITOBO_HOME=

if [ -z "$MITOBO_HOME" ] ; then
        ## assume current working directory
        MITOBO_HOME=$PWD
fi

## determine machine type and set LD_LIBRARY_PATH
LD=
if [ -z "$LD" ] ; then
        ARCH=`uname -m`
        case "$ARCH" in
                i686)   LD=$MITOBO_HOME/lib/lib32
			break
                        ;;
                x86_64) LD=$MITOBO_HOME/lib/lib64
			break
                        ;;
                *)      echo "can not determine processor architecture"
                        ;;
        esac
fi

CP=""
for jar in $MITOBO_HOME/jars/*jar ; do
	CP="$CP:$jar"
done

if [ -z "$LD_LIBRARY_PATH" ] ; then
        LD_LIBRARY_PATH=/usr/lib:$LD
else
        LD_LIBRARY_PATH="$LD_LIBRARY_PATH:$LD"
fi

export LD_LIBRARY_PATH

# Ok, let's start the generic commandline interface of MiToBo
java -cp "$CP" de/unihalle/informatik/Alida/tools/ALDOpRunner $*
