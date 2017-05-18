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

package de.unihalle.informatik.MiToBo.enhance;

import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;

import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.Parameter.Direction;
import de.unihalle.informatik.MiToBo.core.datatypes.*;
import de.unihalle.informatik.MiToBo.core.datatypes.defines.MTBConstants;
import de.unihalle.informatik.MiToBo.core.datatypes.images.*;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;

/**
 * Histogram linearization by entropy maximization.
 * <p>
 * Basically, the cumulative distribution of the normalized intensity
 * histogram is calculated and used as transfer function to linearize
 * the histogram. In theory this yields an entropy maximization, but
 * due to discretization in practice the histogram entropy might at best
 * remain constant and most of the time slightly decreases. Anyway, the 
 * contrast of the histogram and the underlying image is increased 
 * applying this approach.
 * 
 * @author moeller
 */
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.ALL,
	level=Level.APPLICATION, allowBatchMode=true)
public class HistogramEqualization extends MTBOperator {

	/**
	 * Input image to process.
	 */
	@Parameter( label= "Input Image", required = true, dataIOOrder = 0,
			direction = Direction.IN, description = "Input image.")
	protected MTBImage inImg = null;

	/**
	 * Result image with improved contrast.
	 */
	@Parameter( label= "Result Image", dataIOOrder = 0,
			direction = Direction.OUT, description = "Result image.")
	protected MTBImage resultImg = null;
	
	/**
	 * Initial Shannon entropy of the input image.
	 */
	@Parameter( label= "Entropy of Input Image", dataIOOrder = 1,
			direction = Direction.OUT, description = "Input image entropy.")
	protected double inputEntropy = 0;
	
	/**
	 * Final Shannon entropy of the output image.
	 */
	@Parameter( label= "Entropy of Output Image", dataIOOrder = 2,
			direction = Direction.OUT, description = "Output image entropy.")
	protected double outputEntropy = 0;

	/**
	 * Maximal value of intensity range, depending on input image type.
	 */
	private int maxVal = 255;
	
	/**
	 * Default constructor.
	 * @throws ALDOperatorException Thrown in case of failure.
	 */
	public HistogramEqualization() throws ALDOperatorException	{
		// nothing to do here
	}
	
	/**
	 * Constructor with default image.
	 * @param img		Image to enhance.
	 * @throws ALDOperatorException	Thrown in case of failure.
	 */
	public HistogramEqualization(MTBImage img) throws ALDOperatorException {	
		this.inImg = img;	
	}
	
	/**
	 * Returns the result image.
	 * @return	Result image.
	 */
	public MTBImage getResultImage() {
		return this.resultImg;
	}
		
	/**
	 * This method does the actual work. 
	 */
	@Override
	protected void operate() {
		// determine maximum of intensity range
		if (this.inImg instanceof MTBImageByte)
			this.maxVal = 255;
		else if (this.inImg instanceof MTBImageShort)
			this.maxVal = 256*256-1;
		// perform histogram linearization
		this.equalizeImageHisto(this.inImg);
	}
	
	/**
	 * Apply histogram equalization to the given image.
	 * 
	 * @param ip	Input image to normalize.
	 */
	private void equalizeImageHisto(MTBImage ip) {

		int width = ip.getSizeX();
		int height = ip.getSizeY();
		int depth = ip.getSizeZ();
		int times = ip.getSizeT();
		int channels = ip.getSizeC();
		
		// calculate histogram
		MTBImageHistogram histo = 
				new MTBImageHistogram(ip,this.maxVal+1,-0.5,this.maxVal+0.5);
		histo.normalize();
		this.inputEntropy = HistogramEqualization.calcEntropy(histo);
		
		// get transfer function
		int [] transfer= this.getTransferFunktion(histo);
		
		// transform image values, result image depends on input image type
		if (histo.getMaxValueBin() > 255) { 
			this.resultImg = MTBImage.createMTBImage(width, height, depth,
					times, channels, MTBImageType.MTB_SHORT);
		}
		else {
			this.resultImg = MTBImage.createMTBImage(width, height, depth, 
					times, channels, MTBImageType.MTB_BYTE);
		}
		
		for (int c=0; c<channels; ++c) {
			for (int t=0; t<times; ++t) {
				for (int z=0; z<depth; ++z) {
					for (int y=0; y<height; ++y) {
						for (int x=0; x<width; ++x) {
							this.resultImg.putValueInt(x, y, z, t, c,
									transfer[ip.getValueInt(x, y, z, t, c)]);
						}
					}
				}
			}
		}
		this.resultImg.setTitle("Histogram equalization result for \"" + 
				this.inImg.getTitle() + "\"");
		
		// calculate entropy of output image
		histo = new MTBImageHistogram(this.resultImg, 
				this.maxVal+1, -0.5, this.maxVal+0.5);
		histo.normalize();
		this.outputEntropy = HistogramEqualization.calcEntropy(histo);
	}
	
	/**
	 * Get the discrete transfer function for the histogram.
	 * 
	 * @param histo		(Normalized!) histogram to transfer.
	 * @return	Array containing discrete transfer function.
	 */
	private int [] getTransferFunktion(MTBImageHistogram histo) {
		int binNum= histo.getSize();
		double [] histoData= histo.getData();
				
		// calculate cumulative histogram
		double [] cHisto= new double[binNum];
		
		double sum= 0;
		for (int i=0; i<binNum; ++i) {
			sum= sum + histoData[i];
			cHisto[i]= sum;
		}
	
		// get transfer function
		int [] transfer= new int[binNum];
		for (int i=0; i<binNum; ++i) {
			transfer[i]= (int)(this.maxVal * cHisto[i] + 1) - 1;
		}		
		return transfer;
	}
	
	/**
	 * Calculates the Shannon entropy for the given (normalized!) histogram.
	 * @param histo		Input histogram.
	 * @return	Shannon entropy value.
	 */
	private static double calcEntropy(MTBImageHistogram histo) {
		int binNum= histo.getSize();
		double [] histoData= histo.getData();
				
		double entropy= 0;
		for (int i=0; i<binNum; ++i) {
			if (histoData[i] < MTBConstants.epsilon)
				continue;
			entropy += histoData[i] * Math.log10(histoData[i]) / Math.log10(2.0);
		}
		return -entropy;
	}
}
