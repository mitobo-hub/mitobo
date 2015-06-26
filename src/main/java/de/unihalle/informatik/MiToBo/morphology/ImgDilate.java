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

/* 
 * Most recent change(s):
 * 
 * $Rev$
 * $Date$
 * $Author$
 * 
 */

package de.unihalle.informatik.MiToBo.morphology;

import java.util.Hashtable;
import java.util.Set;

import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo;
import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo.ExportPolicy;
import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.MiToBo.core.datatypes.images.*;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.operator.*;

/**
 * This class implements morphological dilation on 2D binary/grayscale images.
 * <p>
 * If the given image only contains two pixel values it is interpreted as 
 * binary image. In the resulting image the background pixels will be set 
 * to the smaller value, while the foreground pixels will be set to the 
 * larger ones.
 * <p> 
 * The structuring element is a square matrix of size 'masksize' x 'masksize', 
 * with reference pixel in the center of the matrix.
 *
 * Attention: if masksize is even, errors may result due 
 *            to lack of operator symmetry
 *
 * @author moeller
 */
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.ALL,
		level=Level.APPLICATION)
@ALDMetaInfo(export=ExportPolicy.MANDATORY)
public class ImgDilate extends MTBOperator {

	@Parameter( label= "Masksize", required = true, dataIOOrder = 1, 
			direction = Parameter.Direction.IN, description = "Masksize")
	private int masksize = 3;

	@Parameter( label= "Input Image", required = true, dataIOOrder = 0,
			direction = Parameter.Direction.IN, description = "Input image")
	private transient MTBImage inImg = null;

	@Parameter( label= "Result Image", required = true,
			direction = Parameter.Direction.OUT, description = "Result image")
	private transient MTBImage resultImg = null;

//	@Parameter( label= "verbose", required = false)
//	@MTBArgumentAnnotation( type = MTBArgumentAnnotation.ALDArgumentType.SUPPLEMENTAL, 
//	                        explanation = "Verbose flag")
//	private Boolean verbose = false;

	/**
	 * Default constructor.
	 *  @throws ALDOperatorException
	 */
	public ImgDilate() throws ALDOperatorException {
		// nothing to do here
	}

	/**
	 * Constructor. 
	 * 
	 * @param inimg	Input image.
	 * @param ms	Size of square mask.
	 * @throws ALDOperatorException
	 */
	public ImgDilate(MTBImage inimg, int ms) throws ALDOperatorException {
		this.inImg = inimg;
		this.masksize = ms;
	}

	/** Get value of masksize.
	  * Explanation: Masksize.
	  * @return value of masksize
	  */
	public int getMasksize(){
		return this.masksize;
	}

	/** Get value of inImg.
	  * Explanation: Input image.
	  * @return value of inImg
	  */
	public MTBImage getInputImage(){
		return this.inImg;
	}


	/** Get value of resultImg.
	  * Explanation: Result image.
	  * @return value of resultImg
	  */
	public MTBImage getResultImage(){
		return this.resultImg;
	}

	/** Set value of resultImg.
	  * Explanation: Result image.
	  * @param value New value of resultImg
	  */
	public void setResultImage( MTBImage value){
		this.resultImg = value;
	}

	/**
	 * This method does the actual work. 
	 */
	@Override
	protected void operate() {
		MTBImage result= this.applyMask(this.getInputImage(), this.getMasksize());
		this.setResultImage(result);
	}
	
	/**
	 * Invokes dilation.
	 * <p>
	 * The method first checks if image is binary or not. Subsequently the 
	 * corresponding dilation function is called.
	 * 
	 * @param mImg	Input image.
	 * @param msize	Size of square mask.
	 * @return	Dilated image.
	 */
	private MTBImage applyMask(MTBImage mImg, int msize) {
	
		// check type of image
		boolean binary = true;
		Hashtable<Integer,Integer> imgHash= new Hashtable<Integer,Integer>();
		for (int y=0; binary && y<mImg.getSizeY(); ++y) {
			for (int x=0; binary && x<mImg.getSizeX(); ++x) {
				if (!imgHash.containsKey(new Integer(mImg.getValueInt(x, y))))
					imgHash.put(new Integer(mImg.getValueInt(x, y)), new Integer(0));
				if (imgHash.keySet().size() > 2)
						binary = false;					
			}
		}
		// error check: image completely homogeneous?
		if (imgHash.keySet().size() == 1)
			return mImg;
		if (binary) {
			int minVal= Integer.MAX_VALUE;
			int maxVal= 0;
			Set<Integer> keys= imgHash.keySet();
			for (Integer inum: keys) {
				if (inum.intValue() > maxVal)
					maxVal = inum.intValue();
				if (inum.intValue() < minVal)
					minVal = inum.intValue();
			}
			return this.applyMaskBinary(mImg, msize, minVal, maxVal);
		}
		return this.applyMaskGray(mImg, msize);
	}

	/**
	 * Applies a dilation mask to the given binary image. 
	 * 
 	 * Pixels with larger gray value are assumed to belong to the foreground 
	 * while pixels with smaller intensities are handled as background pixels.
	 * 
	 * @param ip	Input image for dilation.
	 * @param msize	Size of squared dilation mask.
	 * @param minVal Minimal intensity in image.
	 * @param maxVal Maximal intensity in image.
	 * @return	Dilated result image.
	 */
	private MTBImage applyMaskBinary(MTBImage img, int msize,
			int minVal, int maxVal) {

		int width= img.getSizeX();
		int height= img.getSizeY();
		
		int masksize_2= (int)(msize/2.0);
		
		// create result image
		MTBImageType intype= img.getType();
		MTBImage result= MTBImage.createMTBImage(width,height,1,1,1,intype); 

		// dilate the image
		for (int y=0; y<height;++y) {
			for (int x=0; x<width; ++x) {
				result.putValueInt(x, y, minVal);
				for (int dy= -masksize_2; dy<=masksize_2; ++dy) {
					if (y+dy < 0 || y+dy >= height)
						continue;
					for (int dx=-masksize_2; dx<=masksize_2; ++dx) {
						if (x+dx < 0 || x+dx >= width)
							continue;
						if (img.getValueInt(x+dx,y+dy) == maxVal) {
							result.putValueInt(x, y, maxVal);
							dx= masksize_2+1;
							dy= masksize_2+1;
						}
					}
				}
			}
		}
		return result;
	}
	
	/**
	 * Applies a dilation mask to the given intensity image. 
	 * 
	 * @param ip	Input image for dilation.
	 * @param msize	Size of squared dilation mask.
	 * @return	Dilated result image.
	 */
	private MTBImage applyMaskGray(MTBImage mImg, int msize) {

		int width= mImg.getSizeX();
		int height= mImg.getSizeY();
		
		int masksize_2= (int)(msize/2.0);
		
		// create result image
		MTBImageType intype= mImg.getType();
		MTBImage result= MTBImage.createMTBImage(width, height,1,1,1,intype); 

		// dilate the image
		for (int y=0; y<height;++y) {
			for (int x=0; x<width; ++x) {
				int max= 0;
				for (int dy= -masksize_2; dy<=masksize_2; ++dy) {
					if (y+dy < 0 || y+dy >= height)
						continue;
					for (int dx=-masksize_2; dx<=masksize_2; ++dx) {
						if (x+dx < 0 || x+dx >= width)
							continue;
						if (mImg.getValueInt(x+dx,y+dy) > max)
							max= mImg.getValueInt(x+dx,y+dy);
					}
				}
				result.putValueInt(x, y, max);
			}
		}
		return result;
	}
}
