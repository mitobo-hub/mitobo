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
# R script to transfer Java results of intensity extraction to a global profile
# data file.
#
# Date: 21.03.2011
# 
# Author: misiak
###############################################################################

# Global path varibale is initialised by the calling Java Programm,
# using the JRI Library.
path <- arg

files <- list.files(path = path, pattern = "-neuriteProfile-prot([0-9]+).mtb$", all.files = FALSE,
    full.names = TRUE, recursive = FALSE,
    ignore.case = FALSE)

for(i in 1:length(files)) {
  myInt = matrix()
  myLen = matrix()
  myRes = matrix()
  # read the table data
  test = count.fields(file=files[i], skip = 0, blank.lines.skip = TRUE, sep="\t")
  if (length(test) > 0) {
    prof <- read.delim(file=files[i], header = FALSE, colClass="numeric", sep="\t") 
    max = length(rownames(prof))
    for(j in 1:length(prof)) {
      count = length(prof[!is.na(prof[,j]),j])
      if(j == 1) {
        if(count > 0) {
          myLen = matrix(c(seq(1:count)*100/count, rep(NA, max-count)), ncol = 1)
          myInt = matrix((prof[,j]*100)/sum(prof[,j], na.rm=TRUE), ncol = 1)
          myRes = cbind(myLen[,j], myInt[,j])
        }
      }
      if(j > 1) {
        if(count > 0) {
          myLen = cbind(myLen, matrix((c((seq(1:count)*100)/(count), rep(NA, max-count))), ncol = 1))
          myInt = cbind(myInt, matrix(((prof[,j]*100)/sum(prof[,j], na.rm=TRUE)), ncol = 1))
          myRes = cbind(myRes,myLen[,j], myInt[,j])
        }  
      }
    }
    theName = paste(dirname(dirname(path)), "profileData", basename(files[i]), sep = "/")
    write.table(
        myRes,  file = paste(theName, "txt", sep="."),
        append = FALSE,  quote = FALSE,
        sep = "\t",  eol = "\n",  na = "",
        dec = ",",  row.names = FALSE,  col.names = FALSE
    )   
  }
}


