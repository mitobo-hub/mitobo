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

package de.unihalle.informatik.MiToBo.filters.nonlinear;

import java.util.Vector;

import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;

/**
 * class for calculating several statistical values for the neighborhood of the pixels</br>
 * in an image
 * 
 * @author glass
 *
 */
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.ALL)
public class StatisticsFilter extends MTBOperator
{
	@Parameter(label = "input image", required = true, direction = Parameter.Direction.IN, supplemental = false, description = "input image", mode=ExpertMode.STANDARD, dataIOOrder = 0)
	private transient MTBImage inImg = null;
	
	@Parameter(label = "filter method", required = true, direction = Parameter.Direction.IN, supplemental = false, description = "filtering method", mode=ExpertMode.STANDARD, dataIOOrder = 1)
	private FilterMethod filterMethod = null;

	@Parameter(label = "mask size", required = true, direction = Parameter.Direction.IN, supplemental = false, description = "size of filter mask", mode=ExpertMode.STANDARD, dataIOOrder = 2)
	private Integer maskSize = null;

	@Parameter(label = "result image", required = true, direction = Parameter.Direction.OUT, supplemental = false, description = "result image", mode=ExpertMode.STANDARD, dataIOOrder = 3)
	private transient MTBImage resultImg = null;
	
	/**
	 * available filtermodes
	 * 
	 * @author glass
	 *
	 */
	public static enum FilterMethod
	{
		VARIANCE,
		STDDEV,
		KURTOSIS,
		ENTROPY
	}
	
	

	public StatisticsFilter() throws ALDOperatorException
	{
		
	}
	
	
	public StatisticsFilter(MTBImage inImg, FilterMethod method, int maskSize) throws ALDOperatorException
	{
		this.inImg = inImg;
		this.filterMethod = method;
		this.maskSize = maskSize;
	}
	

	@Override
	protected void operate() throws ALDOperatorException, ALDProcessingDAGException
	{
		switch(filterMethod)
		{
			case VARIANCE:
				resultImg = varianceFiltering(inImg, maskSize);
				break;
			
			case STDDEV:
				resultImg = stdDeviationFiltering(inImg, maskSize);
				break;
			
			case KURTOSIS:
				resultImg = kurtosisFiltering(inImg, maskSize);
				break;
				
			default:
				resultImg = entropyFiltering(inImg, maskSize);
				break;
				
		}	
	}
	
	
	/**
	 * 
	 * @param img		input image
	 * @param maskSize 	size of filter mask
	 * @return 			variance filtered image
	 */
	private MTBImage varianceFiltering(MTBImage img, int maskSize)
	{
		int w = img.getSizeX();
		int h = img.getSizeY();
		int sizeT = img.getSizeT();
		
		int r = maskSize / 2;
//		MTBImage result = MTBImage.createMTBImage(w, h, 1, 1, 1, MTBImage.MTBImageType.MTB_DOUBLE);
		MTBImage result = MTBImage.createMTBImage(w, h, 1, sizeT, 1, MTBImage.MTBImageType.MTB_DOUBLE);
		
		for(int t = 0; t < sizeT; t++)
		{
			for(int y = 0; y < h; y++)
			{
				for(int x = 0; x < w; x++)
				{
					Vector<Double> data = new Vector<Double>();
					double sum = 0;
					
					for(int j = -r; j <= r; j++)
					{
						if((y + j) >= 0 && (y + j) < h)
						{
							for(int i = -r; i <= r; i++)
							{
								if((x + i) >= 0 && (x + i) < w)
								{
									double val = img.getValueDouble(x+i,y+j, 0, t, 0);
									data.add(val);
									sum += val;
								}
								
							}
						}
						
					}
					
					double mean = sum / data.size();
					double var = variance(data, mean);
					
					result.putValueDouble(x,y,0,t,0,var);
				}
			}
		}
		
		
		return result;
	}
	
	
	private MTBImage stdDeviationFiltering(MTBImage img, int maskSize)
	{
		MTBImage result = varianceFiltering(img, maskSize);
	
		int w = img.getSizeX();
		int h = img.getSizeY();
		int sizeT = img.getSizeT();
		
		for(int t = 0; t < sizeT; t++)
		{
			for(int y = 0; y < h; y++)
			{
				for(int x = 0; x < w; x++)
				{
					result.putValueDouble(x, y, 0, t, 0, Math.sqrt(result.getValueDouble(x, y, 0, t, 0)));
				}
			}
		}
		
		
		return result;
	}
	
	/**
	 * 
	 * @param img 		input image
	 * @param maskSize	size of filter mask
	 * @return			kurtosis filtered image
	 */
	private MTBImage kurtosisFiltering(MTBImage img, int maskSize)
	{
		int w = img.getSizeX();
		int h = img.getSizeY();
		int sizeT = img.getSizeT();
		
		int r = maskSize / 2;
		MTBImage result = MTBImage.createMTBImage(w, h, 1, sizeT, 1, MTBImage.MTBImageType.MTB_DOUBLE);
		
		for(int t = 0; t < sizeT; t++)
		{
			for(int y = 0; y < h; y++)
			{
				for(int x = 0; x < w; x++)
				{
					Vector<Double> data = new Vector<Double>();
					float sum = 0;
					
					for(int j = -r; j <= r; j++)
					{
						if((y + j) >= 0 && (y + j) < h)
						{
							for(int i = -r; i <= r; i++)
							{
								if((x + i) >= 0 && (x + i) < w)
								{
									double val = img.getValueDouble(x+i,y+j, 0, t, 0);
									data.add(val);
									sum += val;
								}
								
							}
						}
						
					}
					
					double mean = sum / data.size();
					double var = variance(data, mean);
					double kurtosis = kurtosis(data, mean, var);
					
					result.putValueDouble(x, y, 0, t, 0, kurtosis);
				}
			}
		}
		
		return result;
	}
	
	
	/**
	 * 
	 * @param img		input image
	 * @param maskSize	size of filter mask
	 * @return			entropy filtered image
	 */
	private MTBImage entropyFiltering(MTBImage img, int maskSize)
	{
		int w = img.getSizeX();
		int h = img.getSizeY();
		int sizeT = img.getSizeT();
		
		int r = maskSize / 2;
		int k = img.getMinMaxInt()[1];
//		System.out.println("maximum value: " + k);
		
		double log_of_2 = Math.log(2);
		
		MTBImage result = MTBImage.createMTBImage(w, h, 1, sizeT, 1, MTBImage.MTBImageType.MTB_DOUBLE);
		
		// calculate entropy for every pixel
		for(int t = 0; t < sizeT; t++)
		{
			for(int y = 0; y < h; y++)
			{
				for(int x = 0; x < w; x++)
				{
					double val = 0;
					double[] hist = new double[k+1];
					int elements = 0;
					
					// first, intensities inside window are collected in array to get their distribution
					for(int j = -r; j <= r; j++)
					{
						if((y + j) >= 0 && (y + j) < h)
						{
							for(int i = -r; i <= r; i++)
							{
								if((x + i) >= 0 && (x + i) < w)
								{
									int q = img.getValueInt(x+i,y+j, 0, t, 0);
									hist[q]++;
									elements++;
								}
							}
						}
						
					}
					
					// second, intensity numbers are divided by pixel number to estimate intensity probabilities 
					for(int i = 0; i < k; i++)
					{
						hist[i] /= (elements);
					}
					
					// third, entropy for pixel (x,y) is calculated
					for(int i = 0; i < k; i++)
					{
						if(hist[i] != 0)
						{
							val -= hist[i] * (Math.log(hist[i]) / log_of_2);
						}	
					}
					
					result.putValueDouble(x, y, 0, t, 0, val);			
				}
			}
		}
		
		return result;
	}
	
	
	/**
	 * 
	 * @param data 	vector of data
	 * @param mean	mean of data
	 * @return		variance of data
	 */
	private double variance(Vector<Double> data, double mean)
	{
		double var = 0;
		int n = data.size();
		
		for(int i = 0; i < n; i++)
		{
			var += (data.elementAt(i) - mean) * (data.elementAt(i) - mean);
		}
		
		return var/(n);
	}
	
	
	/**
	 * 
	 * @param data	vector of data
	 * @param mean	mean of data
	 * @param var	variance of data
	 * @return		kurtosis of data
	 */
	private double kurtosis(Vector<Double> data, double mean, double var)
	{
		if(var != 0)
		{
			double kurtosis = 0;
			int n = data.size();
			
			for(int i = 0; i < n; i++)
			{
				double diff = data.elementAt(i) - mean;
				
				kurtosis += Math.pow(diff,4);
			}
			
			return kurtosis / (n * var * var);
		}
		else
		{
			return 0;
		}
		
	}
	
	
	public MTBImage getResultImage()
	{
		return this.resultImg;
	}

	
	public MTBImage getInputImage()
	{
		return this.inImg;
	}
	
}

/*BEGIN_MITOBO_ONLINE_HELP
<p><a target="_blank" href="http://www2.informatik.uni-halle.de/agprbio/mitobo//api/de/unihalle/informatik/MiToBo/filters/nonlinear/StatisticsFilter.html">API</a></p>

<ul><li>
<p>Operator for calculating local statistical measures for every pixel in an image</p>
</li><li>
<p>outputs an image where each pixel intensity value equals the calculated measure for the corresponding pixel of the input image</p>
</li></ul>
<h2>Usage:</h2>
<h3>required parameters:</h3>

<ul><li>
<p>input image
<ul><li>
<p>image to be filtered</p>
</li></ul>
</p>
</li><li>
<p>filter method
<ul><li>
<p>statistic calculated for every pixel</p>
</li><li>
<p>available methods are 
<ul><li>
<p>VARIANCE - variance</p>
</li><li>
<p>STDDEV - standard deviation</p>
</li><li>
<p>KURTOSIS - kurtosis</p>
</li><li>
<p>ENTROPY - Shannon entropy</p>
</li></ul>
</p>
</li></ul>
</p>
</li><li>
<p>mask size 
<ul><li>
<p>side length (pixels) of squared window used to calculate the statistics</p>
</li></ul>
</p>
</li></ul>
<h3>supplemental parameters:</h3>

<ul><li>
<p><tt>Verbose</tt>
<ul><li>
<p>output somme additional information</p>
</li></ul>
</p>
</li></ul>
END_MITOBO_ONLINE_HELP*/