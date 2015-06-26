REM Batch file to start MiToBo on Windows

REM Please set the path variables according to your installation!

REM Path to ImageJ installation, e.g. c:\\Programme\ImageJ
set IMAGEJ_HOME=c:\\Programme\ImageJ

REM Path to where you extracted the MiToBo.zip, e.g. c:\\Programme\Mitobo
set MITOBO_HOME=c:\\Programme\MiToBo

REM Path to your ImageJ plugins directory
set PLUGINS_DIR=c:\\Programme\ImageJ


REM Run ImageJ with MiToBo plugins!
java -cp %MITOBO_HOME%\mtb-imageio-ext-tiff.jar;%MITOBO_HOME%\xbean.jar;%MITOBO_HOME%\xstream-1.3.1.jar;%MITOBO_HOME%\mtbxml.jar;%MITOBO_HOME%\mtbgraphml.jar;%MITOBO_HOME%\jai_imageio.jar;%IMAGEJ_HOME%\ij.jar -Dplugins.dir=%PLUGINS_DIR% ij.ImageJ
