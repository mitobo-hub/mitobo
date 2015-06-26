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
# R script to plot the calculated protein profiles and if wanted the fitted or
# unfitted data.
#
# Date: 27.05.2011
# 
# Author: misiak
###############################################################################

# Global directory varibale is initialised by the calling Java Programm,
# using the JRI Library.
fileDir <- arg[1]


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
  #
  #############################################################################
  #
  # begin recording all plots in one pdf file
  pdf(file = paste(dirname(path),"/", fileName,"_plot.pdf", sep = ""),
      onefile = TRUE, paper = "special",
      title = "R Graphics Output for MTBProteinProfilePlot"
  )
  #calculate and plot the histogram and x- and y-axis
  hist(NeuriteLength, breaks = (maxX/50), axes = FALSE, plot = TRUE,
      xlim = c(0,maxX), ylim = c(0,maxY), col = "blue",
      sub=paste("Experiment: ", expName, "  ,   Date: ", date(), sep = " ")
  )
  axis(1, seq(0,maxX, by=50))
  axis(2, seq(0,maxY, by=5))
  # get data from box plots
  data = boxplot(t(prof), plot=FALSE, outline = FALSE)$stats
  # the single box plot datas to plot
  plot25 = data[2,]
  plotMean = data[3,]
  plot75 = data[4,]
  # maximum y-value for box plot values
  maxBox = round(max(plot75)) + 1
  # x values for x-axis
  x = seq(1,100)
  # plot box plot values for all neurites
  plot(x, plot25, type="l", pch=20,
      lwd = 1, lty = 3,
      main=maintitle,
      sub=paste("Cluster 1 - length [100, inf)", length(colnames(prof)), sep = " | "),
      xlab="Neurite Length [%]",
      ylab="[Arbitrary Fluorescence Units]",
      axes=FALSE, xlim = c(1,100), ylim = c(0,maxBox),
      col="blue"
  )
  lines(x, plotMean, type="l", pch=20, lwd = 2, lty = 1, col="blue")
  lines(x, plot75, type="l", pch=20, lwd = 1, lty = 3, col="blue")
  axis(1, seq(0, 100, by=5))
  axis(2, seq(0, maxBox , by=0.5))
  #
  #############################
# plot box plot values for neurites in cluster 2 (length 100-149)
#
#    cluster2 = prof[, colnames(prof)[NeuriteLength >=100 & NeuriteLength <= 149]]
#    data = boxplot(t(cluster2), plot=FALSE, outline = FALSE)$stats
#    plot25C2 = data[2,]
#    plotMeanC2 = data[3,]
#    plot75C2 = data[4,]
#    plot(x, plot25C2, type="l", pch=20,
#        lwd = 1, lty = 3,
#        main=maintitle,
#        sub=paste("Cluster 2 - length [100, 150)", length(colnames(cluster2)), sep = " | "),
#        xlab="Neurite Length [%]",
#        ylab="[Arbitrary Fluorescence Units]",
#        axes=FALSE, xlim = c(1,100), ylim = c(0,maxBox),
#        col="blue"
#    )
#    lines(x, plotMeanC2, type="l", pch=20, lwd = 2, lty = 1, col="blue")
#    lines(x, plot75C2, type="l", pch=20, lwd = 1, lty = 3, col="blue")
#    axis(1, seq(0, 100, by=5))
#    axis(2, seq(0, maxBox , by=0.5))
  ##
#    #############################
  ## plot box plot values for neurites in cluster 3 (length 150-299)
  ##
#    cluster3 = prof[, colnames(prof)[NeuriteLength >=150 & NeuriteLength <= 299]]
#    data = boxplot(t(cluster3), plot=FALSE, outline = FALSE)$stats
#    plot25C3 = data[2,]
#    plotMeanC3 = data[3,]
#    plot75C3 = data[4,]
#    plot(x, plot25C3, type="l", pch=20,
#        lwd = 1, lty = 3,
#        main=maintitle,
#        sub=paste("Cluster 3 - length [150, 300)", length(colnames(cluster3)), sep = " | "),
#        xlab="Neurite Length [%]",
#        ylab="[Arbitrary Fluorescence Units]",
#        axes=FALSE, xlim = c(1,100), ylim = c(0,maxBox),
#        col="blue"
#    )
#    lines(x, plotMeanC3, type="l", pch=20, lwd = 2, lty = 1, col="blue")
#    lines(x, plot75C3, type="l", pch=20, lwd = 1, lty = 3, col="blue")
#    axis(1, seq(0, 100, by=5))
#    axis(2, seq(0, maxBox , by=0.5))
  ##
#    #############################
  ## plot box plot values for neurites in cluster 4 (length >= 300)
  ##
#    cluster4 = prof[, colnames(prof)[NeuriteLength >=300]]
#    data = boxplot(t(cluster4), plot=FALSE, outline = FALSE)$stats
#    plot25C4 = data[2,]
#    plotMeanC4 = data[3,]
#    plot75C4 = data[4,]
#    plot(x, plot25C4, type="l", pch=20,
#        lwd = 1, lty = 3,
#        main=maintitle,
#        sub=paste("Cluster 4 - length [300, inf)", length(colnames(cluster4)), sep = " | "),
#        xlab="Neurite Length [%]",
#        ylab="[Arbitrary Fluorescence Units]",
#        axes=FALSE, xlim = c(1,100), ylim = c(0,maxBox),
#        col="blue"
#    )
#    lines(x, plotMeanC4, type="l", pch=20, lwd = 2, lty = 1, col="blue")
#    lines(x, plot75C4, type="l", pch=20, lwd = 1, lty = 3, col="blue")
#    axis(1, seq(0, 100, by=5))
#    axis(2, seq(0, maxBox , by=0.5))
  ##
#    #############################
  ## plot box plot values for mean values of all 4 clusters and all data
  ##
#    
#    plot(x, plotMean, type="l", pch=20,
#        lwd = 2, lty = 1,
#        main=maintitle,
#        sub="Median Values of All Data (black) and Cluster 2 - 4 (rgb)",
#        xlab="Neurite Length [%]",
#        ylab="[Arbitrary Fluorescence Units]",
#        axes=FALSE, xlim = c(1,100), ylim = c(0,maxBox),
#        col="black"
#    )
#    lines(x, plotMeanC2, type="l", pch=20, lwd = 1, lty = 1, col="red")
#    lines(x, plotMeanC3, type="l", pch=20, lwd = 1, lty = 1, col="green")
#    lines(x, plotMeanC4, type="l", pch=20, lwd = 1, lty = 1, col="blue")
#    axis(1, seq(0, 100, by=5))
#    axis(2, seq(0, maxBox , by=0.5))
# end recording pdf file
  dev.off()
}
#
################################################################################
#


