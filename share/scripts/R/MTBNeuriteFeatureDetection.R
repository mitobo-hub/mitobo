###############################################################################
# This file is part of MiToBo, the Microscope Image Analysis Toolbox.
#
# Copyright (C) 2010
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.
#
# Fore more information on MiToBo, visit
#
#    http://www.informatik.uni-halle.de/mitobo/
#
###############################################################################

#
# R script to detect morphological features in neurites, such as cell body
# region, axon shaft and the growth cone, by using a 1D neurite width profile.
#
# The script uses the "centWave" algorithm for
#
#   "Feature detection for high resolution LC/MS data".
#
# Author: Colin A. Smith, Ralf Tautenhahn , Steffen Neumann , Paul Benton
# Maintainer: Colin A. Smith
# Version: 1.25.5.
#
# For more details, see:
# http://bioconductor.org/help/bioc-views/2.8/bioc/html/xcms.html
#
# Insallation:
#  sudo apt-get install libnetcdf-dev build-essential
#  xcms_*.*.*.tar.gz downloaden
#  cd ~/Downloads
#  sudo R
#  install.packages("./xcms_1.25.5.tar.gz")
#
#
# Date: 18.03.2011
# 
# Author: misiak
# Script template by Carsten Kuhl.
###############################################################################

# Output, that feature detection is started.
print("", quote=FALSE)
print(" starting MTBNeuriteFeatureDetection...", quote=FALSE)
print("", quote=FALSE)

# Load xcms library.
library(xcms)


# Global path varibale is initialised by the calling Java Programm,
# using the JRI Library. The variable includes the file path for the 1D
# neurite width data for the currently processed neuron neurites, which are
# loaded subsequently into the convolution and the xcmsRaw object for feature
# detection using the "centWave" algorithm.
path <- arg

#
# >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
# Set general parameters and variables for the script.
#

# Border to handle border problems on left side. Details see below.
border <- 20

# Pause between find and plot feature detection in seconds, if 0 no plot of the
# feature detection itself is shown. Default is 0.
featurePlot = 0

# Set main title, empty for first time.

maintitle <- ""

#
# Initialize the result vectors, including the positions for the borderline of
# the neuron cell body region between the axon shaft and the borderline of the
# axon shaft between the growth cone region.
#
bodyPositions <- c()
conePositions <- c()
profileID <- c()
#neuriteLengths <- c()
neuriteWidth <- c()
shaftWidth <- c()
coneWidth <- c()

#
# <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
#


#
# >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
# Set parameters for data convolution of the 1D neurite with data.
#

# The gaussian filter function.
gaussian <- function(x, s) (1/(sqrt(2*pi)*s))*exp(-((x^2)/(2*s^2)))

# Set the gaussian kernel size: 2k+1.
k <- 5

# Set sigam for gaussian filter.
sigma <- 2.0

#
# <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
#


#
# >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
# Set input/output parameters and variables for the script.
#


# Get the current file name (without extension) of the processed input image,
# including the neuron with its neuritres.
fileName <- sub("-neuriteWidth.mtb$", "", basename(path))
fileName <- sub("[.].*", "", fileName)

# Read 1D neurite width data from file and test if file is empty or other
# errors occure, when opening the file.
data <- try(
    read.delim(
        file = path,
        header = FALSE,
        dec = ".",
        colClass = "numeric",
        sep = "\t"
    ), silent = TRUE
)

# Automatic adapt window height, depends on the featurePlot value.
if(featurePlot == 0) {
  plotHeight <- 3
} else {
  plotHeight <- 10
}  

# Begin recording all plots in one pdf file with special page size.
pdf(file = paste(dirname(path),"/", fileName,"-neuriteFeaturePlot.pdf", sep = ""),
    onefile = TRUE, paper = "special",
    width = 8.0, height = plotHeight,
    title = "R Graphics Output for ..."
)

#
# <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
#


# Start feature detection on the given 1D neurite width data.
# Break if neurite reading width data file is not possible and use only
# neurites with length >= 100 pixel.
if((length(rownames(data)) >= 100) && (!inherits(data, "try-error"))) {
  
  # Set max y value for all y axes of each neurite width plot, to have the same
  # axis view for each plot.
  maxY <- max(data, na.rm = TRUE)
  maxY <- (maxY + (10-(maxY%%10)))
  
  # Process every single 1D profile of the neurite width.
  for(i in 1:length(data)) {
    # Set current main title.
    maintitle <- paste("Neurite Features of ",fileName, sep = "")
    # Set current neurite length.
    count <- length(data[!is.na(data[,i]),i])
    # Set x and y values, use only neurites longer 100 pixel.
    if(count >= 100) {
      #neuriteLengths <- c(neuriteLengths, count);
      x <- seq(1, count)
      y <- data[seq(1,count),i]      
      # Handle border problems of data for convolution
      # (first/last value is repeated k-times).
      yConvolved <- c(rep(y[1],k), y , rep(y[length(y)],k))
      
      #
      # 1. Filter 1D neurite width data with gaussian.
      #
      yConvolved <- filter(
          yConvolved,
          gaussian(-k:k, sigma),
          method = "convolution",
          sides = 2, circular = FALSE)
      # Gaussian filtered neurite width data.
      y <- yConvolved[!is.na(yConvolved)]
      
      #
      # 2. Use centWave algorithm for feature detection.
      #
      
      # To handle border problems on left side (the neuron cell body side),
      # mirror the first "#border" data points of the 1D neurite width profile
      # and add them to the left side.
      # To handle border problems on right side (the neuron growth cone side),
      # repeat the last data point "#border" times of the 1D neurite width
      # profile and add them to the right side.
      
      # left side correction
      #yBordered = c(rev(y[1:border]), y)
      # left and right side correction - mirroring
      #yBordered = c(rev(y[1:border]), y, rev(y[(length(y)-border):(length(y))]))
      # left and right side correction - mirroring and repeating
      yBordered <- c(rep(y[1], border), y, rep(y[length(y)], border))
      
      
      halfLength <- round(length(yBordered)/2)
      
      yLeft <- yBordered[1:halfLength]
      yRight <- yBordered[(halfLength+1):length(yBordered)]
      
      #
      #
      # Feature detection for left side of the neurite with data
      # (first 50% of data).
      #
      #
      
      # Create new xcmsRaw object for feature detection.
      xrawLeft <- new("xcmsRaw")
      
      # Set the xcmsRaw parameters.
      scantime       <- 1:length(yLeft); #x-values
      scanindex      <- 1:length(yLeft); #x-values copy, assume scanrate 1Hz
      tic            <- rep(0,length(yLeft));
      acquisitionNum <- 1:length(yLeft);
      #filepath       <- getwd()
      polarity       <- rep("positive",length(yLeft));
      polarity       <- factor(polarity,c("negative","positive","unknown"));
      
      # Allocate parameters to the xcmsRaw object.
      xrawLeft@scantime       <- scantime
      xrawLeft@scanindex      <- scanindex
      xrawLeft@tic            <- tic
      xrawLeft@acquisitionNum <- acquisitionNum
      #xrawLeft@filepath       <- filepath
      xrawLeft@polarity       <- polarity
      mz.signals          <- rep(100,length(yLeft))
      xrawLeft@env$mz         <- mz.signals
      xrawLeft@env$intensity  <- yLeft      
      
      # Use findPeaks method, using centWave algorithm to detect the neurite
      # features.
      featuresLeft <- findPeaks(
          xrawLeft,
          method="centWave",
          # Algorithm parameters.
          #peakwidth=c(20,100), # c(min,max) peak width as range (min, max)
          prefilter=c(0,100), # c(k,I), only profiles with at least k peaks with intensity >= I 
          #ppm=30,
          snthresh=0, # signal to noise ration cutoff
          noise=0, 
          integrate=2, # default =1
          # Display and view settings.
          sleep=featurePlot, # pause between find and plot, 0 shows no plot
          fitgauss=TRUE , # fit Gaussian to each peak
          verbose.columns=TRUE # show additional peak data
      )
      
      #
      #
      # Feature detection for right side of the neurite with data
      # (last 50% of data).
      #
      #
      
      # Create new xcmsRaw object for feature detection.
      xrawRight <- new("xcmsRaw")
      
      # Set the xcmsRaw parameters.
      scantime       <- 1:length(yRight); #x-values
      scanindex      <- 1:length(yRight); #x-values copy, assume scanrate 1Hz
      tic            <- rep(0,length(yRight));
      acquisitionNum <- 1:length(yRight);
      #filepath       <- getwd()
      polarity       <- rep("positive",length(yRight));
      polarity       <- factor(polarity,c("negative","positive","unknown"));
      
      # Allocate parameters to the xcmsRaw object.
      xrawRight@scantime       <- scantime
      xrawRight@scanindex      <- scanindex
      xrawRight@tic            <- tic
      xrawRight@acquisitionNum <- acquisitionNum
      #xrawRight@filepath       <- filepath
      xrawRight@polarity       <- polarity
      mz.signals          <- rep(100,length(yRight))
      xrawRight@env$mz         <- mz.signals
      xrawRight@env$intensity  <- yRight      
      
      # Use findPeaks method, using centWave algorithm to detect the neurite
      # features.
      featuresRight <- findPeaks(
          xrawRight,
          method="centWave",
          # Algorithm parameters.
          #peakwidth=c(20,100), # c(min,max) peak width as range (min, max)
          prefilter=c(0,100), # c(k,I), only profiles with at least k peaks with intensity >= I 
          #ppm=30,
          snthresh=0, # signal to noise ration cutoff
          noise=0, 
          integrate=2, # default =1
          # Display and view settings.
          sleep=featurePlot, # pause between find and plot, 0 shows no plot
          fitgauss=TRUE , # fit Gaussian to each peak
          verbose.columns=TRUE # show additional peak data
      )
      
      #
      # Correct the position values for right side due to the cut of the data
      # into two parts.
      # Combine detected features of the left and rigth side in one matrix.
      # 
      featuresRight[,colnames(featuresRight) ==  "rt"] <- ((featuresRight[,colnames(featuresRight) ==  "rt"]) + halfLength)
      featuresRight[,colnames(featuresRight) ==  "rtmin"] <- ((featuresRight[,colnames(featuresRight) ==  "rtmin"]) + halfLength)
      featuresRight[,colnames(featuresRight) ==  "rtmax"] <- ((featuresRight[,colnames(featuresRight) ==  "rtmax"]) + halfLength)
      features <- rbind(featuresLeft, featuresRight)
      
      #
      # 4. Plot the whole neurite width profile.
      #
      
      # Set max x value for the axes.
      maxX <- (length(y) + (10-(length(y)%%10)))
      # Set max y value for the axes (see above).
      # Set x values.
      x <- seq(1, length(y))
      # Plot the neurite width along the neurite skeleton.
      plot(x, y, type = "l", pch = 21,
          lwd = 1, lty = 1,
          main = maintitle,
          sub = paste("Neurite ", i, " - ", length(y), sep = ""),
          xlab = "Neurite Length [Pixel]",
          ylab = "[Arbitrary Width Units]",
          axes = FALSE, xlim = c(1,maxX), ylim = c(0,maxY),
          col = "blue"
      )
      # Label the axes.
      axis(1, seq(0, maxX, by = 10))
      axis(2, seq(0, maxY , by = 10))
      
      #
      # 5. Plot the detected feature positions for the desired borderline
      # between:
      #         cell body region and axon shaft   --> first rtmax value
      #         axon shaft and growth cone region --> last rtmin value
      # for every neurite width profile.
      # 
      # Features are only plotted if there are at least 2 detected features.
      #
      
      if (length(features[, colnames(features) ==  "rt"]) > 1) {
        
        # Sort the features by rt position (peak midpoint) if more than one
        # feature was detected.
        features <- features[order(features[, colnames(features) ==  "rt"]), ]
        
        # peak midpoint position, border subtracted
        rt <- ((features[,colnames(features) ==  "rt"]) - border)
        # position of peak leading edge, border subtracted
        rtMin <- ((features[,colnames(features) ==  "rtmin"]) - border)
        # position of peak tailing edge, border subtracted
        rtMax <- ((features[,colnames(features) ==  "rtmax"]) - border)
        ## standard deviation of Gaussian function
        sdGaus <- ((features[,colnames(features) ==  "sigma"]))    
        
        
        posFactor1 = 0
        if(is.na(sdGaus[1])) {
          posFactor1 = (rtMax[1]-rt[1])
          posFactor1 = posFactor1 / 2
        } else {
          posFactor1 = (1.5*sdGaus[1])
        }
        
        posFactor2 = 0
        if(is.na(sdGaus[length(rt)])) {
          posFactor2 = (rt[length(rt)]-rtMin[length(rt)])
          posFactor2 = posFactor2/2
        } else {
          posFactor2 = (1.5*sdGaus[length(rt)])
        } 
        
        # -1 beacause R count starts with 1 and not with 0
        if((round(rt[1]+posFactor1)-1) >= 0) {
          
          
          # plot borderline for cell body - shaft
          #lines(c(rtMax[1],rtMax[1]),c(0,maxY-7), col="red")
          lines(c(round(rt[1]+posFactor1),round(rt[1]+posFactor1)),c(0,maxY-7), col="red")
          #text(rtMax[1], maxY-9, rtMax[1], pos = 3, cex=0.7)
          text(round(rt[1]+posFactor1), maxY-9, round(rt[1]+posFactor1), pos = 3, cex=0.7)
          
          
          
          # plot border line for shaft - growth cone
          #lines(c(rtMin[length(rt)],rtMin[length(rt)]),c(0,maxY-7), col="red")
          lines(c(round(rt[length(rt)]-posFactor2),round(rt[length(rt)]-posFactor2)),c(0,maxY-7), col="red")
          #text(rtMin[length(rt)], maxY-9, rtMin[length(rt)], pos =3, cex=0.7)
          text(round(rt[length(rt)]-posFactor2), maxY-9, round(rt[length(rt)]-posFactor2), pos = 3, cex=0.7)
          
          # -1 beacause R count starts with 1 and not with 0
          #bodyPositions <- c(bodyPositions, rtMax[1]-1)
          #conePositions <- c(conePositions, rtMin[length(rt)]-1)
          
          
          
          bodyPositions <- c(bodyPositions, round(rt[1]+posFactor1)-1)
          conePositions <- c(conePositions, round(rt[length(rt)]-posFactor2)-1)
          profileID <- c(profileID, i-1)
          
          # get average width of the different neurite parts
          # get complete neurite width average
          neuriteWidth <- c(neuriteWidth, (round(mean(y[seq(bodyPositions[length(bodyPositions)], length(y))]),digits = 2)))
          # get average width of neurite shaft
          shaftWidth <- c(shaftWidth, (round(mean(y[seq(bodyPositions[length(bodyPositions)], conePositions[length(conePositions)])]),digits=2)))
          # get average width of groth cone
          coneWidth <- c(coneWidth, (round(mean(y[seq(conePositions[length(conePositions)], length(y))]),digits=2)))
        }
      }
      #
      # 6. Save the border value, the convolved 1D neurite width data
      # (including border correction) and the detected features in a RData
      # object file.
      # 
      save(border, yBordered, yLeft, yRight, features,
          file = paste(
              dirname(path), "/", fileName,
              "-neuriteFeature_", i, ".RData", sep = ""
          )
      )
      #print(neuriteWidth)
      #print(shaftWidth)
      #print(coneWidth)
    }
  }
} else {
  # If an error occures, reading the profile file or if no nurites included
  # with length >= 100 pixel, then an empty plot is drawn in the pdf file.
  plot(c(1:10), rep(0,10), type = "l", pch = 21,
      lwd = 1, lty = 1,
      main = "no neurites with length >= 100",
      xlab = "Neurite Length [Pixel]",
      ylab = "[Arbitrary Width Units]",
      col = "blue"
  )
  print(
      " ***** Error: No neurites >= 100 found, or an other error occured!",
      quote=FALSE
  )
}
# End recording pdf file.
dev.off()
# Output, that feature detection is done.
print("", quote=FALSE)
print(" MTBNeuriteFeatureDetection...done!", quote=FALSE)
print("", quote=FALSE)
