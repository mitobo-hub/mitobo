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

import java.util.concurrent.ConcurrentLinkedQueue;

import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException.OperatorExceptionType;
import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.ExecutionMode;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBPoint3D;
import de.unihalle.informatik.MiToBo.core.datatypes.images.*;
import de.unihalle.informatik.MiToBo.core.operator.*;

/**
 * H-dome transform in 3D (straightforward use with 2D-images). The h-dome transform finds regional maxima, which means connected components D of an image I with following properties:
 * - every pixel p neighbor of D satisfies: I(p) < min{ I(q) | q element of D}
 * - max{ I(q) | q element of D} - min{ I(q) | q element of D} < h
 *  [Vincent93]
 *  
 * The implementation follows the work of Luc Vincent:
 * [Vincent93] Luc Vincent "Morphological Grayscale Reconstruction in Image Analysis: Applications and Efficient Algorithms"
 *             in IEEE Transactions on Image Processing, Vol. 2, No. 2, pp. 176-201, April 1993
 *  
 *  
 * @author gress
 *
 */
@ALDAOperator(genericExecutionMode=ExecutionMode.ALL,level=Level.STANDARD)
public class HDomeTransform3D extends MTBOperator {
	
	@Parameter( label= "h", required = true,  direction = Parameter.Direction.IN, 
			 mode=ExpertMode.STANDARD, dataIOOrder=2, description = "Dome height parameter")
	private Double h = null;

	@Parameter( label= "inputImage", required = true,  direction = Parameter.Direction.IN, 
			 mode=ExpertMode.STANDARD, dataIOOrder=1, description = "Input image")
	private MTBImage inputImage = null;

	@Parameter( label= "resultImage", required = true,  direction = Parameter.Direction.OUT, 
			 mode=ExpertMode.STANDARD, dataIOOrder=1, description = "Result image")
	private MTBImage resultImage = null;

	/**
	 * Constructor
	 *
	 */
	public HDomeTransform3D() throws ALDOperatorException {
	}
	
	/**
	 * Constructor
	 * @param img input image
	 * @param h max size of h-domes
	 * @throws ALDOperatorException 
	 */
	public HDomeTransform3D(MTBImage img, double h) throws ALDOperatorException {
		this.inputImage = img;
		this.h = h;
	}

	/**
	 * Get reference to the current input image.
	 */
	public MTBImage getInputImage() {
		return this.inputImage;
	}
	
	/**
	 * Set input image
	 */
	public void setInputImage(MTBImage img) {
		this.inputImage = img;
	}
	
	/**
	 * Get current h-parameter (max height of the h-domes).
	 */
	public double getH() {
		return this.h;
	}
	
	/**
	 * Set current h-parameter (max height of the h-domes).
	 */
	public void setH(double h) {
		this.h = h;
	}
	
	/**
	 * Get the resulting h-dome image.
	 */
	public MTBImage getResultImage() {
		return this.resultImage;
	}
	
	/**
	 * Set the result image.
	 */
	protected void setResultImage(MTBImage resultImage) {
		this.resultImage = resultImage;
	}

	@Override
	protected void operate() throws ALDOperatorException {
		
		// compute h-dome transform
		MTBImage resImg = transform(this.inputImage,this.h);
		
		// attach the resulting image to the parameter object
		this.setResultImage(resImg);
		
		if (resImg == null) 
			throw new ALDOperatorException(OperatorExceptionType.OPERATE_FAILED, "HDomeTransform3D.operate(): Result image is 'null'");
	}
	
	@Override
	public void validateCustom() throws ALDOperatorException {
		if (this.h < 0) {
			throw new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED, "HDomeTransform3D.validateCustom(): h must be >= 0");
		}
	}
	
	// ---- operator functionality ----
	/**
	 * Compute the h-dome transform
	 * @param img input image
	 * @param h max height of h-domes
	 * @return h-dome image
	 */
	protected MTBImage transform(MTBImage img, double h) {
		MTBImage outImg = img.duplicate();
		
		int sizeX = img.getSizeX();
		int sizeY = img.getSizeY();
		int sizeZ = img.getSizeZ();
		int sizeT = img.getSizeT();
		int sizeC = img.getSizeC();
		
		double val;
		
		// subtract h from the input image to obtain marker image
		for (int c = 0; c < sizeC; c++) {
			for (int t = 0; t < sizeT; t++) {
				for (int z = 0; z < sizeZ; z++) {
					for (int y = 0; y < sizeY; y++) {
						for (int x = 0; x < sizeX; x++) {
							val = img.getValueDouble(x, y, z, t, c);
							
							val -= h;
							
							if (val < 0) {
								val = 0;
							}
							
							outImg.putValueDouble(x, y, z, t, c, val);
						}
					}
				}
			}
		}
		
		// reconstruction image
		hybridGrayscaleReconstruct(img, outImg);
		
		// construct h-dome image
		for (int c = 0; c < sizeC; c++) {
			for (int t = 0; t < sizeT; t++) {
				for (int z = 0; z < sizeZ; z++) {
					for (int y = 0; y < sizeY; y++) {
						for (int x = 0; x < sizeX; x++) {
							val = img.getValueDouble(x, y, z, t, c);
							
							val -= outImg.getValueDouble(x, y, z, t, c);
							
							if (val < 0)
								val = 0;
							
							outImg.putValueDouble(x, y, z, t, c, val);
						}
					}
				}
			}
		}
			
		return outImg;
	}
	
	/**
	 * Implements the fast hybrid grayscale reconstruction algo of [Vincent93].
	 * Mask image (I) corresponds to the input image, marker image determines the marker (J).
	 * Remember that J <= I everywhere in the image. Result is stored in marker!!
	 * @param mask
	 * @param marker
	 */
	protected void hybridGrayscaleReconstruct(MTBImage mask, MTBImage marker) {
		double val, maxval;
		boolean smallerNeighbor;
		double cval; // value of the current marker pixel
		ConcurrentLinkedQueue<MTBPoint3D> fifo = new ConcurrentLinkedQueue<MTBPoint3D>();
		
		int sizeX = mask.getSizeX();
		int sizeY = mask.getSizeY();
		int sizeZ = mask.getSizeZ();
		int sizeT = mask.getSizeT();
		int sizeC = mask.getSizeC();
			
		// for all 3D-volumes
		for (int c = 0; c < sizeC; c++) {
			for (int t = 0; t < sizeT; t++) {
				
				// scan in raster order
				for (int z = 0; z < sizeZ; z++) {
					for (int y = 0; y < sizeY; y++) {
						for (int x = 0; x < sizeX; x++) {
							
							// max value of neighborhood
							maxval = marker.getValueDouble(x,y,z,t,c);
							
							if (z > 0) {
								val = marker.getValueDouble(x,y,z-1,t,c);
								
								if (val > maxval) {
									maxval = val;
								}
							}
							
							if (y > 0) {
								val = marker.getValueDouble(x,y-1,z,t,c);
								
								if (val > maxval) {
									maxval = val;
								}
							}					
							if (x > 0) {
								val = marker.getValueDouble(x-1,y,z,t,c);
								
								if (val > maxval) {
									maxval = val;
								}
							}			
							
							val = mask.getValueDouble(x,y,z,t,c);
							
							if (maxval < val)
								val = maxval;
							
							marker.putValueDouble(x, y, z, t, c, val);
						}
					}
				}	
		

		
				// scan in anti-raster order
				for (int z = sizeZ-1; z >= 0; z--) {
					for (int y = sizeY-1; y >= 0; y--) {
						for (int x = sizeX-1; x >= 0; x--) {
							
							smallerNeighbor = false;
							
							// max value of neighborhood
							maxval = marker.getValueDouble(x,y,z,t,c);
							cval = maxval;
							
							if (cval < mask.getValueDouble(x,y,z,t,c))
								smallerNeighbor = true;
							
							if (z < sizeZ-1) {
								val = marker.getValueDouble(x,y,z+1,t,c);
								
								if (val > maxval) {
									maxval = val;
								}
								
								if (val < cval && val < mask.getValueDouble(x,y,z+1,t,c))
									smallerNeighbor = true;
							}
							
							if (y < sizeY-1) {
								val = marker.getValueDouble(x,y+1,z,t,c);
								
								if (val > maxval) {
									maxval = val;
								}
								
								if (val < cval && val < mask.getValueDouble(x,y+1,z,t,c))
									smallerNeighbor = true;
							}		
							
							if (x < sizeX-1) {
								val = marker.getValueDouble(x+1,y,z,t,c);
								
								if (val > maxval) {
									maxval = val;
								}
								
								if (val < cval && val < mask.getValueDouble(x+1,y,z,t,c))
									smallerNeighbor = true;
							}					
							
							
							val = mask.getValueDouble(x,y,z,t,c);
							
							if (maxval < val)
								val = maxval;
							
							marker.putValueDouble(x, y, z, t, c, val);
							
							if (smallerNeighbor) {
								fifo.add(new MTBPoint3D(x,y,z));
							}
						}
					}
				}
		
				MTBPoint3D p;
				int x,y,z;
				double maskval;
				//propagation step
				while (!fifo.isEmpty()) {
					p = fifo.poll();
					x = (int)p.getX();
					y = (int)p.getY();
					z = (int)p.getZ();
					
					cval = marker.getValueDouble(x, y, z, t, c);
					
					if (z > 0) {
						val = marker.getValueDouble(x, y, z-1, t, c);
						maskval = mask.getValueDouble(x, y, z-1, t, c);
						
						if (val < cval && val != maskval) {
							val = (cval < maskval) ? cval : maskval;
							marker.putValueDouble(x, y, z-1, t, c, val);
							fifo.add(new MTBPoint3D(x,y,z-1));
						}
					}
					if (z < sizeZ-1) {
						val = marker.getValueDouble(x, y, z+1, t, c);
						maskval = mask.getValueDouble(x, y, z+1, t, c);
						
						if (val < cval && val != maskval) {
							val = (cval < maskval) ? cval : maskval;
							marker.putValueDouble(x, y, z+1, t, c, val);
							fifo.add(new MTBPoint3D(x,y,z+1));
						}
					}			
					
					if (y > 0) {
						val = marker.getValueDouble(x, y-1, z, t, c);
						maskval = mask.getValueDouble(x, y-1, z, t, c);
						
						if (val < cval && val != maskval) {
							val = (cval < maskval) ? cval : maskval;
							marker.putValueDouble(x, y-1, z, t, c, val);
							fifo.add(new MTBPoint3D(x,y-1, z));
						}
					}
					if (y < sizeY-1) {
						val = marker.getValueDouble(x, y+1, z, t, c);
						maskval = mask.getValueDouble(x, y+1, z, t, c);
						
						if (val < cval && val != maskval) {
							val = (cval < maskval) ? cval : maskval;
							marker.putValueDouble(x, y+1, z, t, c, val);
							fifo.add(new MTBPoint3D(x,y+1, z));
						}
					}
					
					if (x > 0) {
						val = marker.getValueDouble(x-1, y, z, t, c);
						maskval = mask.getValueDouble(x-1, y, z, t, c);
						
						if (val < cval && val != maskval) {
							val = (cval < maskval) ? cval : maskval;
							marker.putValueDouble(x-1, y, z, t, c, val);
							fifo.add(new MTBPoint3D(x-1, y, z));
						}
					}
					if (x < sizeX-1) {
						val = marker.getValueDouble(x+1, y, z, t, c);
						maskval = mask.getValueDouble(x+1, y, z, t, c);
						
						if (val < cval && val != maskval) {
							val = (cval < maskval) ? cval : maskval;
							marker.putValueDouble(x+1, y, z, t, c, val);
							fifo.add(new MTBPoint3D(x+1, y, z));
						}
					}	
				}	
				
			} // end for (int t ...
		} // end for (int c ...
		
	}
	
}
