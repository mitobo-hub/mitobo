/*
 * This file is part of MiToBo, the Microscope Image Analysis Toolbox.
 *
 * Copyright (C) 2010 - @YEAR@
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Fore more information on MiToBo, visit
 *
 *    http://www.informatik.uni-halle.de/mitobo/
 *
 */

package de.unihalle.informatik.MiToBo.core.datatypes;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;

import de.unihalle.informatik.Alida.annotations.ALDClassParameter;
import de.unihalle.informatik.Alida.annotations.ALDParametrizedClass;
import de.unihalle.informatik.Alida.operator.ALDData;
import de.unihalle.informatik.MiToBo.core.datatypes.images.*;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;

/**
 * Class for generating intensity histograms of objects from class 
 * {@link de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage}.
 * 
 * @author glass
 */
@ALDParametrizedClass
public class MTBImageHistogram extends ALDData
{
	/**
	 * Array holding the histogram data
	 */
	@ALDClassParameter(label="Histogram Data") 
	private double[] data;

	/**
	 * number of bins
	 */
	@ALDClassParameter(label="Number of Bins")
	private int size;

	/**
	 * smallest intensity value contained in the input image
	 */
	@ALDClassParameter(label="Smallest non-empty Bin.")
	private int smallestNonEmptyBin;

	/**
	 * largest intensity value contained in the input image
	 */
	@ALDClassParameter(label="Largest non-empty Bin.")
	private int largestNonEmptyBin;

	/**
	 * sum of all histogram entries
	 */
	@ALDClassParameter(label="Sum of all entries")
	private double sum;

	/**
	 * number of histogram entries
	 */
	@ALDClassParameter(label="Number of entries")
	private double numEntries;

	/**
	 * lower boundary of the first bin.
	 */
	@ALDClassParameter(label="Lower bound of first bin")
	private double lowBound;

	/**
	 * upper boundary of the last bin
	 */
	@ALDClassParameter(label="Upper bound of last bin")
	private double highBound;

	/**
	 * Default constructor.
	 */
	public MTBImageHistogram() {
		// nothing to initialize here
	}
	
		/**
		 * construct a histogram for the given input image</br>
		 * with specification of binning and value range</br>
		 * all MTBImage types except RGB type are supported
		 * 
		 * @param img
		 *          input image for which the histogram is calculated
		 * @param bins
		 *          number of histogram bins
		 * @param lowBoundary
		 *          determines the lower boundary of the first bin
		 * @param highBoundary
		 *          determines the upper boundary of the last bin
		 * @throws IllegalArgumentException
		 *           if an RGB image is given as input
		 */
		public MTBImageHistogram(MTBImage img, int bins, double lowBoundary, double highBoundary) throws IllegalArgumentException
		{
			if(img.getType() == MTBImageType.MTB_RGB)
			{
				throw new IllegalArgumentException("Histogram creation for RGB images not supported.");
			}
			
			if(lowBoundary >= highBoundary)
			{
				throw new IllegalArgumentException("Histogram  must be larger than lowest value");
			}
			
			this.lowBound = lowBoundary;
			this.highBound = highBoundary;
			this.data = new double[bins];

			int sizeX = img.getSizeX();
			int sizeY = img.getSizeY();
			int sizeZ = img.getSizeZ();
			int sizeT = img.getSizeT();
			int sizeC = img.getSizeC();
			
//			System.out.println("sizeX: " + sizeX);
//			System.out.println("sizeY: " + sizeY);
//			System.out.println("sizeZ: " + sizeZ);
//			System.out.println("sizeT: " + sizeT);
//			System.out.println("sizeC: " + sizeC);
			
			// collect the data
			for(int c = 0; c < sizeC; c++)
			{
				for(int t = 0; t < sizeT; t++)
				{
					for(int z = 0; z < sizeZ; z++)
					{
						for(int y = 0; y < sizeY; y++)
						{
							for(int x = 0; x < sizeX; x++)
							{
								this.data[this.getBinIndex(img.getValueDouble(x, y, z, t, c))] += 1.0;	
							}
						}
					}
				}
			}
			
			initialize();
		}
		
		
		/**
		 * construct a histogram for the given input image only for the pixels specified by a binary mask</br>
		 * all MTBImage types except RGB type are supported</br>
		 * with specification of binning and value range
		 * 
		 * @param img
		 *          input image for which the histogram is calculated
		 * @param mask
		 * 			binary mask specifying which pixels are considered for the creation of the histogram.
		 *          A value of zeor in the mask image indicates this pixel not to be considered
		 * @param bins
		 *          number of histogram bins
		 * @param lowBoundary
		 *          determines the lower boundary of the first bin
		 * @param highBoundary
		 *          determines the upper boundary of the last bin
		 * @throws IllegalArgumentException
		 *           if an RGB image is given as input
		 */
		public MTBImageHistogram(MTBImage img, MTBImage mask, int bins, double lowBoundary, double highBoundary) throws IllegalArgumentException
		{
			if(img.getType() == MTBImageType.MTB_RGB) 
			{
				throw new IllegalArgumentException("Histogram creation for RGB images not supported.");
			}
				
			if(lowBoundary >= highBoundary)
			{
				throw new IllegalArgumentException("Histogram  must be larger than lowest value");
			}
				
			this.lowBound = lowBoundary;
			this.highBound = highBoundary;
			this.data = new double[bins];
			
			int sizeX = img.getSizeX();
			int sizeY = img.getSizeY();
			int sizeZ = img.getSizeZ();
			int sizeT = img.getSizeT();
			int sizeC = img.getSizeC();
			
			// collect the data if dimensions of mask and image are equal
			if(mask.getSizeX() == sizeX && mask.getSizeY() == sizeY && mask.getSizeZ() == sizeZ && mask.getSizeT() == sizeT && mask.getSizeC() == sizeC)
			{
				for(int c = 0; c < sizeC; c++)
				{
					for(int t = 0; t < sizeT; t++)
					{
						for(int z = 0; z < sizeZ; z++)
						{
							for(int y = 0; y < sizeY; y++)
							{
								for(int x = 0; x < sizeX; x++)
								{
									if(mask.getValueDouble(x, y, z, t, c) != 0)
									{
										this.data[this.getBinIndex(img.getValueDouble(x, y, z, t, c))] += 1.0;
									}
								}
							}
						}
					}
				}
				
				initialize();
			}
			else
			{
				throw new IllegalArgumentException("dimensions of image and mask must be equal");
			}
			
		}

        /**
         * construct a new MTBHistogram object from the given MTBImage<br/>
         * the histogram ranges from the smallest image value to the largest image value
         *
         * @param img
         *          MTBImage
         * @param mask
         * 			binary mask specifying which pixels are considered for the creation of the histogram
         * @param bins
         *          number of bins
         */
        public MTBImageHistogram(MTBImage img, MTBImage mask, int bins)
        {
            this(img, mask, bins, img.getMinMaxDouble()[0], img.getMinMaxDouble()[1]);
        }
		
		
		/**
		 * construct a new MTBHistogram object from the given MTBImage<br/>
		 * the histogram ranges from the smallest image value to the largest image value
		 * 
		 * @param img
		 *          MTBImage
		 * @param bins
		 *          number of bins
		 */
		public MTBImageHistogram(MTBImage img, int bins)
		{
			this(img, bins, img.getMinMaxDouble()[0], img.getMinMaxDouble()[1]);
		}
		
		/**
		 * construct a new MTBHistogram object from the given MTBImage<br/>
		 * number of bins is 256 and the histogram ranges from the smallest <br/>
		 * image value to the largest image value
		 * 
		 * @param img
		 *          MTBImage
		 */
		public MTBImageHistogram(MTBImage img) 
		{
			this(img, 256, img.getMinMaxDouble()[0], img.getMinMaxDouble()[1]);
		}
		
		/**
		 * Construct new histogram from array of values.
		 * @param vals					Values
		 * @param bins					Number of bins.
		 * @param lowBoundary		Lower boundary of first bin.
		 * @param highBoundary	Upper boundary of last bin.
		 */
		public MTBImageHistogram(double[] vals, int bins, 
															double lowBoundary, double highBoundary) {
			if(lowBoundary >= highBoundary) {
				throw new IllegalArgumentException(
															"Histogram  must be larger than lowest value");
			}

			this.lowBound = lowBoundary;
			this.highBound = highBoundary;
			this.data = new double[bins];

			int arraySize = vals.length;

			// collect the data
			for(int c = 0; c < arraySize; c++) {
				this.data[this.getBinIndex(vals[c])] += 1.0;
			}
			initialize();
		}
		
		
		/**
		 * construct a MTBImageHistogram from histogram data contained in an array <br/>
		 * 
		 * @param values		histogram data
		 * @param lowBoundary	the lower boundary of the first bin
		 * @param binWidth		the width of the bins
		 */
		public MTBImageHistogram(double[] values, double lowBoundary, double binWidth)
		{	
			this.lowBound = lowBoundary;
			this.highBound = lowBoundary + (values.length * binWidth) - binWidth;
			
			this.data = values.clone();
			
			initialize();
		}
		

		/**
		 * initialize histogram, i.e. determine smallest non empty bin, largest non<br/>
		 * empty bin, number of histogram entries and sum of the histogram
		 * 
		 * @param h
		 *          initializing values in a double array
		 */
		private void initialize()
		{
			this.size = this.data.length;
			this.sum = 0;
			this.numEntries = 0;
			
			// search for first non-empty entry
			for(int i = 0; i < this.size; i++)
			{
				if(this.data[i] != 0)
				{
					this.smallestNonEmptyBin = i;
					break;
				}
			}
			
			// search for last non-empty entry
			for(int i = this.size - 1; i >= 0; i--)
			{
				if(this.data[i] != 0)
				{
					this.largestNonEmptyBin = i;
					break;
				}
			}
			
			// calculate the sum of all entries
			
			
			//for(int i = smallestNonEmptyBin; i <= largestNonEmptyBin; i++)
			for(int i = 0; i < this.size; i++)
			{
				this.numEntries += this.data[i];
				this.sum += this.data[i] * mapIndexToValue(i);
			}
		}

		
		/**
		 * determines the index of the histogram bin, to which a value is assigned, if
		 * histogram value range and binning were specified at creation
		 * 
		 * @param value
		 *          image value
		 * @return index of histogram bin for a given value (indices range from 0 to
		 *         (bins-1))
		 */
		public int getBinIndex(double value)
		{
			if(this.lowBound == this.highBound)
			{
				return (int)Math.round(value);
			}
			int idx = (int)((value - this.lowBound) * 
				this.data.length / (this.highBound - this.lowBound));
				
			if(idx >= this.data.length)
			{
					idx = this.data.length - 1;
			} 
			else if(idx < 0)
			{
					idx = 0;
			}
			
			return idx;
		}

		/**
		 * determines the value midpoint of a histogram bin for given bin index, if<br/>
		 * histogram value range and binning were specified at creation
		 * 
		 * @param binIndex
		 *          bin index
		 * @return (gray value) midpoint of a bin
		 */
		public double getBinMidpoint(int binIndex)
		{
			if(this.lowBound == this.highBound)
			{
					return binIndex;
			}
			return(this.lowBound + (this.highBound - this.lowBound) / this.data.length * (binIndex + 0.5));
		}

		
		/**
		 * 
		 * @param i
		 *          position
		 * @return bin value of the ith bin
		 */
		public double getBinValue(int i)
		{
			if(i >= 0 && i < this.size)
			{
				return this.data[i];
			}
			return 0;
		}

		
		/**
		 * 
		 * @param index
		 * @return intensity value of the bin with index index
		 */
		public double mapIndexToValue(int index)
		{
			if(index < 0)
			{
				return this.lowBound;
			}
			else if(index > this.size)
			{
				return this.highBound;
			}
			
			return ((double)index / (this.size - 1)) * (this.highBound - this.lowBound) + this.lowBound;
		}
		
		/**
		 * set the bin value at i-th position
		 * 
		 * @param i
		 *          position
		 * @param val
		 *          bin value
		 */
		public void setBinValue(int i, double val)
		{
			if(i >= 0 && i < this.size)
			{
				this.data[i] = val;
			}
			
			initialize();
		}

		/**
		* 
		 * @return largest value
		 */
		public double getMaxValue()
		{
			double maxValue = 0;
			
			for (int i = 0; i < this.size; i++)
			{
				if(this.data[i] > maxValue)
				{
					maxValue = this.data[i];
				}
			}
			
			return maxValue;
		}

		
		/**
		 * 
		 * @return bin with largest value
		 */
		public double getMaxValueBin()
		{
			double maxValue = 0;
			int maxValueBin = 0;
			
			for (int i = 0; i < this.size; i++)
			{
				if (this.data[i] > maxValue)
				{
					maxValue = this.data[i];
					maxValueBin = i;
				}
			}
			
			return maxValueBin;
		}
		
		
		/**
		 *
		 * @return mean intensity value
		 */
		public double getMean()
		{	
			return (this.sum / this.numEntries);
		}

		
		/**
		 * 
		 * @return variance of the intensity values
		 */
		public double getVariance()
		{
			double var = 0;
			double m = this.getMean();
			
			for (int i = 0; i < this.size; i++)
			{
				var += (mapIndexToValue(i) - m) * (mapIndexToValue(i) - m) * this.data[i];
			}
			
			return (var / this.numEntries);
		}
		
		
		/**
		 * 
		 * @return standard deviation of the intensity values
		 */
		public double getStdDev()
		{
			return Math.sqrt(this.getVariance());
		}
		
		
		@Override
		public String toString()
		{
			StringBuffer bins = new StringBuffer("");
			StringBuffer values = new StringBuffer("");

			for (int i = 0; i < this.size - 1; i++)
			{
				bins.append(mapIndexToValue(i) + "\t");
				values.append(this.data[i] + "\t");
			}
			
			bins.append(mapIndexToValue(this.size - 1));
			values.append(this.data[this.size - 1]);
			
			return (bins + "\n" + values);
		}

		
		/**
		 * add histogram data to this MTBHistogram object.
		 * 
		 * @param h
		 *          MTBHistogram to add
		 * @throws IllegalArgumentException
		 */
		public void add(MTBImageHistogram h) throws IllegalArgumentException
		{
			if(this.data.length != h.data.length || this.lowBound != h.lowBound || this.highBound != h.highBound)
			{
				throw new IllegalArgumentException("Input histogram doesn't match the histogram it is added to.");
			}
			
			for(int i = 0; i < this.size; i++)
			{
				this.data[i] += h.data[i];
			}
			
			initialize();
		}


		/**
		 * Normalize the histogram.
		 * <p>
		 * Attention! Note that due to accuracy issues in the internal calculations 
		 * the re-normalization of an already normalized histogram may cause 
		 * histogram entries to change again which usually must not happen! 
		 */
		public void normalize()
		{
			for (int i = 0; i < this.size; i++)
			{
//				data[i] /= sum;
				this.data[i] /= this.numEntries;
			}
			
			initialize();
		}

		/**
		 * truncate the histogram to the specified interval
		 * 
		 * @param l
		 *          left border
		 * @param r
		 *          right border
		 */
		public void truncate(int l, int r)
		{
			double[] temp = new double[r - l + 1];
			
			for(int i = l; i <= r; i++)
			{
				temp[i - l] = this.data[i];
			}
			
			this.data = temp;
			
			initialize();
		}

		
		/**
		 * change the binning of the histogram
		 * 
		 * @param bins
		 *          number of bins
		 */
		public void binning(int bins)
		{
			double[] temp = new double[bins];
			
			if(this.size % bins == 0) // binsize integer
			{
				// System.out.println("integer");
				int binsize = this.size / bins;
				
				if(binsize > 1)
				{
					for(int i = 0; i < this.size; i++)
					{
						double value = this.data[i];
						temp[i / binsize] += value;
					}
						
				}
				else // number of bins larger than size
				{
					// first size bins keep their values, following bins get 0
					for(int i = 0; i < this.size; i++)
					{
						double value = this.data[i];
						temp[i] += value;
					}		
				}
			}
			else // binsize not integer
			{
				// System.out.println("not integer");
				double binsize = (double) this.size / bins;
				double[] borders = new double[bins];
				
				if(binsize >= 1)
				{
					for(int i = 0; i < bins; i++)
					{
						borders[i] = (binsize * (i + 1)) - 1; // -1 because [0, ..., size - 1]
						System.out.println(borders[i]);
					}
					
					int x = 0;
					
					for(int i = 0; i < this.size; i++)
					{
						int bin = (int) (i / binsize);
						double value = this.data[i];
						temp[bin] += value;
						double f = borders[x] - i;
						// System.out.println("f: " + f);
						
						if(f < 1)
						{
							x++;
							
							if(f > 0)
							{
								// System.out.println("i " + i + " bin: " + bin);
								temp[bin] += f * this.data[i + 1];
								temp[bin + 1] += (1 - f) * this.data[i + 1];
								i++;
							}
						}
					}
				}
				else // number of bins larger than size
				{
					// first size bins keep their values, following bins get 0
					
					for(int i = 0; i < this.size; i++)
					{
						double value = this.data[i];
						temp[i] += value;
					}
				}
			}
			
			this.data = temp;
			
			initialize();
		}

		/**
		 * calculate the logarithmic (base e) histogram; entries smaller than one become zero!
		 */
		public void logarithmize()
		{
			for(int i = 0; i < this.size; i++)
			{
				double value = this.data[i];
				
				// log([value <= 1]) becomes 0
				if(value > 1)
				{
					this.data[i] = Math.log(value);
				}
			}
			
			initialize();
		}

		
		/**
		 * calculate the cumulative histogram
		 */
		public void cumulate()
		{
			double hsum = 0;
			
			for(int i = 0; i < this.size; i++)
			{
				hsum += this.data[i];
				this.data[i] = hsum;
			}
			
			initialize();
		}
		
		
		/**
		 * calculate the cumulative histogram without changing the sum of absolute
		 * frequencies (normalizing factor), because cumulate() calls initialize() and
		 * thus destroys this normalizing factor
		 */
		public void cumulateOnly()
		{
			double hsum = 0.0;
			
			for(int i = 0; i < this.size; i++)
			{
				hsum += this.data[i];
				this.data[i] = hsum;
			}
		}

		/**
		 * 
		 * @return data values
		 */
		public double[] getData()
		{
			return this.data;
		}

		
		/**
		 * 
		 * @return number of bins
		 */
		public int getSize()
		{
			return this.size;
		}

		
		/**
		 * 
		 * @return smallest non empty bin
		 */
		public int getSmallestNonEmptyBin()
		{
			return this.smallestNonEmptyBin;
		}

		
		/**
		 * 
		 * @return largest non empty bin
		 */
		public int getLargestNonEmptyBin()
		{
			return this.largestNonEmptyBin;
		}

		
		/**
		 * 
		 * @return sum of entries
		 */
		public double getSum()
		{
			return this.sum;
		}
		
		
		/**
		 * 
		 * @return number of entries
		 */
		public double getNumEntries()
		{
			return this.numEntries;
		}


		/**
		 * write the histogram data into a text file
		 * 
		 * @param fileName file name for histogram data
		 */
		public void save(String fileName) 
		{
			// write histogram into file
			try 
			{
				BufferedWriter bw = new BufferedWriter(new FileWriter(fileName));
						
				bw.write(this.toString());
						
				bw.close();
			}
			catch(FileNotFoundException e)
			{
				System.err.println(e);
			}
			catch(IOException e) 
			{
				System.err.println(e);
			}
		}
		
		
		/**
		 * 
		 * @return a copy of this histogram
		 */
		public MTBImageHistogram duplicate()
		{
			double width = (this.highBound - this.lowBound) / ((double)this.size - 1);
			
			return new MTBImageHistogram(this.data, this.lowBound, width);
		}
}
