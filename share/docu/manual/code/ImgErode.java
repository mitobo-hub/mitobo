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
//import de.unihalle.informatik.MiToBo_admin.annotations.MTBMetaInfo;
//import de.unihalle.informatik.MiToBo_admin.annotations.MTBMetaInfo.Exportable;

/**
 * This class implements morphological erosion on 2D binary/grayscale images. 
 * <p>
 * If the given image only contains two pixel values it is interpreted as 
 * binary image. In the resulting image the background pixels will be set 
 * to the smaller value, while the foreground pixels will be set to the 
 * larger ones.
 * <p> 
 * The structuring element is a square matrix of size 'masksize' x 'masksize', 
 * with reference pixel in the center of the matrix.
 * 
 * Attention: if masksize is even, errors result due to 
 *            non-given symmetry
 *
 * @author moeller
 */
...
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.ALL,
		level=Level.APPLICATION)
public class ImgErode extends MTBOperator {

	@Parameter( label= "Masksize", required = true, dataIOOrder = 1, 
			direction = Parameter.Direction.IN, description = "Masksize")
	private int masksize = 3;

	@Parameter( label= "Input Image", required = true, dataIOOrder = 0,
			direction = Parameter.Direction.IN, description = "Input image")
	private transient MTBImage inImg = null;

	@Parameter( label= "Result Image", required = true,
			direction = Parameter.Direction.OUT, description = "Result image")
	private transient MTBImage resultImg = null;

	/**
	 * Default constructor.
	 *  @throws ALDOperatorException
	 */
	public ImgErode() throws ALDOperatorException {
		// nothing to do here
	}		

	/**
	 * Returns the eroded image, null if not available.
	 */
	public MTBImage getResultImage() {
		return this.resultImg;
	}

	/**
	 * This method does the actual work.
	 */
	@Override
	protected void operate() {
		MTBImage result= this.applyMask(this.getInputImage(), this.getMasksize());
		this.resultImg = result;
	}
	...

	/**
	 * Invokes erosion.
	 * <p>
	 * The method first checks if image is binary or not. Subsequently the 
	 * corresponding erosion function is called.
	 * 
	 * @param mImg	Input image.
	 * @param msize	Size of square mask.
	 * @return	Eroded image.
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
	 * Applies an erosion mask to the given binary image. 
	 * 
	 * Pixels with larger gray value are assumed to belong to the foreground 
	 * while pixels with smaller intensities are handled as background pixels.
	 * 
	 * @param ip	Input image for erosion.
	 * @param msize	Size of squared erosion mask.
	 * @param minVal Minimal intensity in image.
	 * @param maxVal Maximal intensity in image.
	 * @return	Eroded result image.
	 */
	private MTBImage applyMaskBinary(MTBImage mImg, int msize, 
				int minVal, int maxVal) {

		int masksize_2= (int)(msize/2.0);

		int width= mImg.getSizeX();
		int height= mImg.getSizeY();

		// create result image
		MTBImageType intype= mImg.getType();
		MTBImage result= MTBImage.createMTBImage(width,height,1,1,1,intype); 

		// erode the image
		for (int y=0; y<height;++y) {
			for (int x=0; x<width; ++x) {
				result.putValueInt(x, y, maxVal);
				for (int dy= -masksize_2; dy<=masksize_2; ++dy) {
					if (y+dy < 0 || y+dy >= height)
						continue;
					for (int dx=-masksize_2; dx<=masksize_2; ++dx) {
						if (x+dx < 0 || x+dx >= width)
							continue;
						if (mImg.getValueInt(x+dx,y+dy) == minVal) {
							result.putValueInt(x, y, minVal);
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
	 * Applies an erosion mask to the given intensity image. 
	 * 
	 * @param ip	Input image for erosion.
	 * @param msize	Size of squared erosion mask.
	 * @return	Eroded result image.
	 */
	private MTBImage applyMaskGray(MTBImage mImg, int msize) {

		int width= mImg.getSizeX();
		int height= mImg.getSizeY();
		
		int masksize_2= (int)(msize/2.0);
		
		// create result image
		MTBImageType intype= mImg.getType();
		MTBImage result= MTBImage.createMTBImage(width, height,1,1,1,intype); 

		// erode the image
		for (int y=0; y<height;++y) {
			for (int x=0; x<width; ++x) {
				int min= Integer.MAX_VALUE;
				for (int dy= -masksize_2; dy<=masksize_2; ++dy) {
					if (y+dy < 0 || y+dy >= height)
						continue;
					for (int dx=-masksize_2; dx<=masksize_2; ++dx) {
						if (x+dx < 0 || x+dx >= width)
							continue;
						if (mImg.getValueInt(x+dx,y+dy) < min)
							min= mImg.getValueInt(x+dx,y+dy);
					}
				}
				result.putValueInt(x, y, min);
			}
		}
		return result;
	}
}
