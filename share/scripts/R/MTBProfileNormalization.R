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
# R script to normalize the global profile data. Normaization is done by
# setting the neurite length and sum of intensity to 100 %, respectively.
#
# Date: 12.04.2011
# 
# Author: misiak
###############################################################################

# Global path varibale is initialised by the calling Java Programm,
# using the JRI Library.
path <- arg[1]


for (p in 1:arg[2]) {
  files <- list.files(path = path, pattern = paste("-neuriteProfile-prot([", p ,"]+).mtb.txt$", sep = ""), all.files = FALSE,
      full.names = TRUE, recursive = FALSE,
      ignore.case = FALSE)
  allData = NULL
  allNames = NULL   
  for(i in 1:length(files)) {
    name = ""
  
    delete = NULL 
    # get the image name
    name = unlist(strsplit(basename(files[i]), "-"))[1]
    # read the table raw data
    rawData = read.table(file=files[i], header = FALSE, colClass="numeric", sep="\t", dec=",")
    
    columnNames <- c(
        sapply(
            seq(1, length(colnames(rawData))/2, by=1),
            function(nID){
              return(c(nID, paste(name, i, nID, length(rawData[!is.na(rawData[,nID*2]),nID*2]), sep="_")))
            }
        )
    )
    colnames(rawData) = columnNames
    
    for(m in 1:length(colnames(rawData))) {
      if(length(rawData[!is.na(rawData[,m]),m]) > 99) {
        delete = c(delete, m)
      }
    }
    if(length(delete) > 0){
      rawData = rawData[,delete]
      x = seq(1, length(colnames(rawData)), by = 2)
      y = seq(2, length(colnames(rawData)), by = 2)
      
      # Bilden der Summe f�r jdenen Prozentpunkt, damit am Ende eine Intensit�t
      # von 100% innerhalb des Neuriten ensteht und die Verschiedenen Neuriten mit
      # verschiedennen Intensit�ten vergleichbar sind.
      columnNames = NULL
      data = matrix(rep(0,length(x)*100), nrow=100);
      for(j in 1:length(x)) {
        for(k in 1 : 100) {
          tmp = sort(rawData[rawData[,x[j]] > (k-1) & rawData[,x[j]] <= k,y[j]])
          summe = sum(tmp)
          data[k,j] = summe
          
        }
        allNames = c(allNames, colnames(rawData)[y[j]])
      }
      allData = cbind(allData, data)
    }
   
  }
  colnames(allData) = allNames
  rownames(allData) = seq(1:100)
  outName = paste("Prot", p, "Data.txt", sep="")
  write.table(
      allData,  file = file.path(path, outName),
      append = FALSE,  quote = FALSE,
      sep = "\t",  eol = "\n",  na = "",
      dec = ",",  row.names = TRUE,  col.names = TRUE
  )
}

