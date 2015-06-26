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
# R script to fit the calculated protein profile to a smooth curve.
# 
# Date: 14.01.2011
#
# Author: misiak
###############################################################################

# Global directory varibale is initialised by the calling Java Programm,
# using the JRI Library.
fileDir <- arg[1]
# begin recording all plots in one pdf file
pdf(file = paste(fileDir,"/", "ProtProfileFit.pdf", sep = ""),
    onefile = TRUE, paper = "special",
    title = "R Graphics Output for MTBProteinProfileFit"
)
# calculate fit of profiles for all proteins


files <- list.files(path = fileDir, pattern = "Prot([0-9]+)Data.txt$", all.files = FALSE,
    full.names = TRUE, recursive = FALSE,
    ignore.case = FALSE)

for(i in 1:length(files)) {
  path = files[i]    
  # get file name like Prot1Data
  fileName = sub(".txt$", "", basename(path))
  # set main title
  maintitle = paste("Protein Profile of ", arg[(i+1)], sep="")
  # get name of experiment directory like aktin_merge_all
  expName = basename(dirname(dirname(path)))
  # read the data for the profile
  prof <- read.delim(file=path, header = TRUE, dec = ",", colClass="numeric", sep="\t") 
  # get id of length of the neurite from the columname of every neurite
  # every columname has imageName_neuriteNumber_NeuriteLength, separated by "_"
  id = seq(4,length(colnames(prof))*4,by=4)
  # get leth of all neurites as numeric vector
  NeuriteLength <- unlist(strsplit(colnames(prof), "_"))[id]
  NeuriteLength = as.numeric(NeuriteLength)
  # get maximum x-axis value if axis should be separated into bins of length 50
  maxX = (max(NeuriteLength) + (50-(max(NeuriteLength)%%50)))
  # get maximum y-axis value if axis should be separated into bins of length 5
  maxY = (max(hist(NeuriteLength, breaks = (maxX/50), plot = FALSE)$counts))
  maxY = (maxY + (5-(maxY%%5)))
  # get data from box plots
  data = boxplot(t(prof), plot=FALSE, outline = FALSE)$stats
  # the single box plot datas to plot (lower, upper quartil and median)
  plot25 = data[2,]
  plotMedian = data[3,]
  plot75 = data[4,]
  
  # plots with mean and sd
  plotMean = rowMeans((prof))
  # normalize to range [0,1]
  plotMean = (plotMean/max(plotMean))
  errorBar = sd((plotMean))
  
  
  # maximum y-value for box plot values
  maxBox = (round(max(plotMedian))+1) / 100
  # x values for x-axis
  x = seq(1,100)
  #############################################################################
  ### fit the profile ###
  #############################################################################
  # create data set
  ds <- data.frame(x = x, y = plotMedian)  
  # several polynomial curves of different degrees
# rhs <- function(x, a) {x^a}
# rhs <- function(x, a,b) {a*x+b}
 rhs <- function(x, a,b,c) {a*x^2+b*x+c}
# rhs <- function(x, a,b,c,d) {a*x^3+b*x^2+c*x+d}
# rhs <- function(x, a,b,c,d,e) {a*x^4+b*x^3+c*x^2+d*x+e}
# rhs <- function(x,a,b,c,d,e,f) {a*x^5+b*x^4+c*x^3+d*x^2+e*x+f}
# rhs <- function(x,a,b,c,d,e,f,g) {a*x^6+b*x^5+c*x^4+d*x^3+e*x^2+f*x+g}
  # create the fit model
  m <- nls(y ~ rhs(x,a,b,c), data = ds, start = list(a=0,b=0,c=0),
      trace = F,control = list(maxiter = 5000, minFactor = 0.1e-20))
  #print(summary(m))
  # residual sum of squares
  RSS.p <- sum(residuals(m)^2)
  # print(RSS.p)
  # total sum of squares
  TSS <- sum((plotMedian - mean(plotMedian))^2)
  # print(TSS)
  # R-squared: coefficient of determination
  RR <- (1 - (RSS.p/TSS))
  # print(paste(RR,sum(resid(m)^2), sep = " - "))
  #############################################################################
  # plot box plot values for all neurites
  plot(x/100, plotMedian/100, type="p", pch=20,
      lwd = 1, lty = 1, cex = 1.0,
      main=maintitle,
      sub=paste("Cluster 1 - length [100, inf)", length(colnames(prof)), sep = " | "),
      xlab="Neurite Length",
      ylab="Median Relative Intensity", cex.lab = 1.3, cex.main = 1.3,
      axes=FALSE, xlim = c(0,1), ylim = c(0,maxBox),
      col="red"
  )
  lines(x/100, plotMedian/100, type="l", pch=20, lwd = 1, lty = 1, col="red")
  lines(x/100, predict(m, list(x = x))/100, type="l", pch=20, lwd = 3, lty = 1, col="blue")
  
  # >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
  # alternative plot mean and sd error bars
#  library (gplots)
#  plotCI(x, plotMean, uiw = errorBar, type="p", pch=20,
#      lwd = 1, lty = 1, cex = 1.0,
#      main=maintitle,
#      sub=paste("Cluster 1 - length [100, inf)", length(colnames(prof)), sep = " | "),
#      xlab="Neurite Length [%]",
#      ylab="[Arbitrary Fluorescence Units]",
#      axes=FALSE, xlim = c(1,100), ylim = c(0,2),
#      col="red"
#  )
#  ds <- data.frame(x = x, y = plotMean)  
#  m <- nls(y ~ rhs(x,a,b,c), data = ds, start = list(a=0,b=0,c=0),
#      trace = F,control = list(maxiter = 5000, minFactor = 0.1e-20))
#  RSS.p <- sum(residuals(m)^2)
#  TSS <- sum((plotMean - mean(plotMean))^2)
#  RR <- (1 - (RSS.p/TSS))
#  print(RR)
#  lines(x, predict(m, list(x = x)), type="l", pch=20, lwd = 3, lty = 1, col="blue")
  # <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
  
  
  text(0, 0, paste("R² = ", round(RR,4), sep = ""), pos = 4)
  axis(1, seq(0, 1, by=0.1),cex.axis = 1.3)
  axis(2, seq(0, maxBox , by=0.005),cex.axis = 1.3)
  # plot only the fitted curve
  plot(x/100, predict(m, list(x = x))/100, type="l", pch=20,
      lwd = 3, lty = 1, cex = 1.0,
      main=maintitle,
      sub=paste("Cluster 1 - length [100, inf)", length(colnames(prof)), sep = " | "),
      xlab="Neurite Length",
      ylab="Median Relative Intensity",
      axes=FALSE, xlim = c(0,1), ylim = c(0,maxBox),cex.axis = 5,
      col="blue"
  )
  axis(1, seq(0, 1, by=0.1))
  axis(2, seq(0, maxBox , by=0.005))
}
# end recording pdf file
dev.off()  

