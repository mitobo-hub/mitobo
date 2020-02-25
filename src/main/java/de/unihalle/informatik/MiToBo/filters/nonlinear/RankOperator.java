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

import java.util.Arrays;
import java.util.Vector;

import loci.common.StatusEvent;
import loci.common.StatusListener;
import loci.common.StatusReporter;

import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException.OperatorExceptionType;
import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;


/**
 * Rank operator class to compute Median, Minimum or Maximum filter for hyperstack, stack and plain images.
 * Slow but memory efficient implementation.
 * 
 * @author gress
 * 
 */
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.ALL,
		shortDescription="Applies a non-linear rank operator to the image.")
public class RankOperator extends MTBOperator implements StatusReporter {
	

	@Parameter( label= "Input image", required = true, direction = Parameter.Direction.IN, 
			 mode=ExpertMode.STANDARD, dataIOOrder=1, description = "Input image")
	private MTBImage inImg = null;

	@Parameter( label= "Result image", required = true, direction = Parameter.Direction.OUT, 
			 mode=ExpertMode.STANDARD, dataIOOrder=1, description = "Resulting image")
	private MTBImage resultImg = null;
	
	
	@Parameter( label= "Rank operation", required = true, direction = Parameter.Direction.IN, 
			 mode=ExpertMode.STANDARD, dataIOOrder=2, description = "Type of the rank operation")
	private RankOpMode rankOpMode = RankOpMode.MEDIAN;
	
	
	@Parameter( label= "Mask-radius x", required = true, direction = Parameter.Direction.IN, 
			 mode=ExpertMode.ADVANCED, dataIOOrder=3, description = "Radius of the mask in x-dimension in a city-block sense (rectangular mask)")
	private int maskRadiusX = 1;

	@Parameter( label= "Mask-radius y", required = true, direction = Parameter.Direction.IN, 
			 mode=ExpertMode.ADVANCED, dataIOOrder=4, description = "Radius of the mask in y-dimension in a city-block sense (rectangular mask)")
	private int maskRadiusY = 1;

	@Parameter( label= "Mask-radius z", required = true, direction = Parameter.Direction.IN, 
			 mode=ExpertMode.ADVANCED, dataIOOrder=5, description = "Radius of the mask in z-dimension in a city-block sense (rectangular mask)")
	private int maskRadiusZ = 1;
	
	@Parameter( label= "Mask-radius t", required = true, direction = Parameter.Direction.IN, 
			 mode=ExpertMode.ADVANCED, dataIOOrder=6, description = "Radius of the mask in t-dimension in a city-block sense (rectangular mask)")
	private int maskRadiusT = 0;

	@Parameter( label= "Mask-radius c", required = true, direction = Parameter.Direction.IN, 
			 mode=ExpertMode.ADVANCED, dataIOOrder=7, description = "Radius of the mask in c-dimension in a city-block sense (rectangular mask)")
	private int maskRadiusC = 0;


	
	/** Defines the operation */
	public enum RankOpMode {
		MEDIAN, MIN, MAX
	}
	
	/** Size of the dimensions, size of the real stack. These values are set when the median(..) function is called with an ImagePlus object */
	private int m_sizeX, m_sizeY, m_sizeZ, m_sizeT, m_sizeC;
	
	/** Radius of the median in the corresponding dimension. These values are set when the rankOp(..) function is called with an ImagePlus object */
	private int m_rX, m_rY, m_rZ, m_rT, m_rC;
	
	/** vector of installed StatusListeners */
	private Vector<StatusListener> m_statusListeners;
	
	/** index of the minimum, median or maximum element in the sorted array */
	private int m_idx;
	
	
	// ----- Constructors
	/**
	 * Constructor
	 */
	public RankOperator() throws ALDOperatorException {
		m_statusListeners = new Vector<StatusListener>(1);
	}	
	
	/**
	 * Constructor
	 * @param inImg input image
	 * @param mode rank operation
	 * @param maskRadius mask radius in a city-block sense for cubic mask of size (2*maskRadius + 1) in x-, y-dimension and z-dimension if available 
	 * @throws ALDOperatorException if given parameters are not valid
	 */
	public RankOperator(MTBImage inImg,
						RankOpMode mode,
						int maskRadius) throws ALDOperatorException {
		
		this.setInImg(inImg);
		this.setRankOpMode(mode);
		this.setMaskRadiusX(maskRadius);
		this.setMaskRadiusY(maskRadius);
		this.setMaskRadiusZ(maskRadius);
		this.setMaskRadiusT(0);
		this.setMaskRadiusC(0);
		
		m_statusListeners = new Vector<StatusListener>(1);
	}
	
	
	/**
	 * Constructor 
	 * @param inImg input image
	 * @param mode rank operation
	 * @param maskRadiusX mask radius in x-dimension in a city-block sense (rectangular mask)
	 * @param maskRadiusY mask radius in y-dimension in a city-block sense (rectangular mask)
	 * @param maskRadiusZ mask radius in z-dimension in a city-block sense (rectangular mask)
	 * @param maskRadiusT mask radius in t-dimension in a city-block sense (rectangular mask)
	 * @param maskRadiusC mask radius in c-dimension in a city-block sense (rectangular mask)
	 * @throws ALDOperatorException if given parameters are not valid
	 */
	public RankOperator(MTBImage inImg,
						RankOpMode mode,
						int maskRadiusX,
						int maskRadiusY,
						int maskRadiusZ,
						int maskRadiusT,
						int maskRadiusC) throws ALDOperatorException {
		
		this.setInImg(inImg);
		this.setRankOpMode(mode);
		this.setMaskRadiusX(maskRadiusX);
		this.setMaskRadiusY(maskRadiusY);
		this.setMaskRadiusZ(maskRadiusZ);
		this.setMaskRadiusT(maskRadiusT);
		this.setMaskRadiusC(maskRadiusC);
		
		m_statusListeners = new Vector<StatusListener>(1);
	}
	
	@Override
	protected void operate() throws ALDOperatorException {
		MTBImage resultImg = null;
		
		resultImg = rankOp(this.getInImg(),
								this.getRankOpMode(),
								this.getMaskRadiusX(),
								this.getMaskRadiusY(),
								this.getMaskRadiusZ(),
								this.getMaskRadiusT(),
								this.getMaskRadiusC());

		

		if (resultImg != null) {

			this.setResultImg(resultImg);
		}
		else {
			throw new ALDOperatorException(OperatorExceptionType.OPERATE_FAILED, "RankOperator.operate(): Result image is 'null'");
		}

	}
	
	/**
	 * @throws ALDOperatorException 
	 * 
	 */
	@Override
	public void validateCustom() throws ALDOperatorException {
		
		boolean valid = !(this.getMaskRadiusX() < 0 
								|| this.getMaskRadiusY() < 0
								|| this.getMaskRadiusZ() < 0
								|| this.getMaskRadiusT() < 0
								|| this.getMaskRadiusC() < 0);


		if (!valid)
			throw new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED, "RankOperator.validateCustom(): " +
						     													"MaskRadius must not be negative.");
	}

	
	// ---- rank operation functions ----
	
	/**
	 * Compute the rank operation of a gray-valued image, stack or hyperstack
	 * @param img input MTBImage
	 * @param radius radius of the median mask
	 * @param mode rank operator mode
	 * @return median image
	 */
	protected MTBImage rankOp(MTBImage img, RankOpMode mode, int radiusX, int radiusY, int radiusZ, int radiusT, int radiusC) {
		
		MTBImage rankOpImg;
		
		
		// get dimension sizes and radius of the mask in each dimension	
		m_sizeX = img.getSizeX();
		if (radiusX > 0) {
			if (radiusX >= m_sizeX)
				m_rX = m_sizeX - 1;
			else 
				m_rX = radiusX;
		}
		else 
			m_rX = 0;
		
		m_sizeY = img.getSizeY();
		if (radiusY > 0)  {
			if (radiusX >= m_sizeY)
				m_rY = m_sizeY - 1;
			else 
				m_rY = radiusX;
		}
		else 
			m_rY = 0;		
		
		m_sizeZ = img.getSizeZ();
		if (radiusZ > 0) {
			if (radiusZ >= m_sizeZ)
				m_rZ = m_sizeZ - 1;
			else 
				m_rZ = radiusZ;
		}
		else 
			m_rZ = 0;	
			
		m_sizeT = img.getSizeT();
		if (radiusT > 0) {
			if (radiusZ >= m_sizeT)
				m_rT = m_sizeT - 1;
			else 
				m_rT = radiusZ;
		}
		else
			m_rT = 0;		
				
		m_sizeC = img.getSizeC();
		if (radiusC > 0) {
			if (radiusC >= m_sizeC)
				m_rC = m_sizeC - 1;
			else 
				m_rC = radiusC;
		}
		else
			m_rC = 0;	
		
	
		
		// create rank operator result image
		rankOpImg = img.duplicate();

		
		// array for median neighborhood values
		int size = (2*m_rX+1)*(2*m_rY+1)*(2*m_rZ+1)*(2*m_rT+1)*(2*m_rC+1);
		double[] values = new double[size];
		
		// determine idx of min, median or max element in the sorted array
		if (mode == RankOpMode.MIN) {
			m_idx = 0;
		}
		else if (mode == RankOpMode.MAX) {
			m_idx = size-1;
		}
		else if (mode == RankOpMode.MEDIAN){
			m_idx = size/2;
		}
		
		// progress bar update
		//int ctzy_minus = m_sizeC*m_sizeT*m_sizeZ*m_sizeY;
		int ctz_minus = m_sizeC*m_sizeT*m_sizeZ;
		int tzy = m_sizeT*m_sizeZ*m_sizeY;
		int zy = m_sizeZ*m_sizeY;
	//	fireStatusEvent(0, ctzy_minus, "");		
		
		// compute rank operator
		for (int c = 0; c < m_sizeC; c++) {
			for (int t = 0; t < m_sizeT; t++) {
				for (int z = 0; z < m_sizeZ; z++) {
					for (int y = 0; y < m_sizeY; y++) {
						for (int x = 0; x < m_sizeX; x++) {
							
							rankOpImg.putValueDouble(x, y, z, t, c, rankedValueAt(x,y,z,t,c,img,values));
							
						}
					}
					
					this.notifyListeners(new StatusEvent(c*tzy + t*zy + z, ctz_minus, ""));
				}
			}
		}

		return rankOpImg;
	}

	/**
	 * Compute rank operator value for coordinate (x,y,z,t,c)
	 * Treatment at boundaries: Image values are mirrored at the border element to determine
	 * values under the mask that are outside the image. For example I(-1,-1)=I(1,1)
	 * @param x x-coordinate
	 * @param y y-coordinate
	 * @param z z-coordinate
	 * @param t t-coordinate
	 * @param c c-coordinate
	 * @param img Image to compute the ranked value from
	 * @return ranked value
	 */
	private double rankedValueAt(int x, int y, int z, int t, int c, MTBImage img, double[] values) {
		
	//	int size = (2*m_rX+1)*(2*m_rY+1)*(2*m_rZ+1)*(2*m_rT+1)*(2*m_rC+1);
	//	int[] values = new int[size];
		
		int xx, yy, zz, tt, cc;
		int cnt = 0;
		
		for (int ic = c - m_rC; ic <= c + m_rC; ic++) {
			for (int it = t - m_rT; it <= t + m_rT; it++) {
				for (int iz = z - m_rZ; iz <= z + m_rZ; iz++) {
					for (int iy = y - m_rY; iy <= y + m_rY; iy++) {
						for (int ix = x - m_rX; ix <= x + m_rX; ix++) {
							
							xx = ix;
							if (ix < 0) xx = -ix;			
							if (ix >= m_sizeX) xx = m_sizeX - (ix - m_sizeX) - 2;						
							
							yy = iy;
							if (iy < 0) yy = -iy;			
							if (iy >= m_sizeY) yy = m_sizeY - (iy - m_sizeY) - 2;
							
							zz = iz;
							if (iz < 0) zz = -iz;			
							if (iz >= m_sizeZ) zz = m_sizeZ - (iz - m_sizeZ) - 2;
							
							tt = it;
							if (it < 0) tt = -it;
							if (it >= m_sizeT) tt = m_sizeT - (it - m_sizeT) - 2;	
							
							cc = ic;
							if (ic < 0) cc = -ic;				
							if (ic >= m_sizeC) cc = m_sizeC - (ic - m_sizeC) - 2;
							
							values[cnt++] = img.getValueDouble(xx, yy, zz, tt, cc);	
						}
					}
				}
			}
		}		
		
		Arrays.sort(values);
		
		return values[m_idx];
	}
	
	
	// ----- Function to be implemented for StatusReporter interface
	
	@Override
	public void addStatusListener(StatusListener statuslistener) {	
		m_statusListeners.add(statuslistener);	
	}

	@Override
	public void removeStatusListener(StatusListener statuslistener) {
		m_statusListeners.remove(statuslistener);
	}
	
	@Override
	public void notifyListeners(StatusEvent e) {
		for (int i = 0; i < m_statusListeners.size(); i++) {
			m_statusListeners.get(i).statusUpdated(e);
		}
	}


	//--- getter/setter methods
	
	/** Get value of Parameter argument maskRadiusX.
	  * @return value of maskRadiusX
	  */
	public int getMaskRadiusX() {
		return this.maskRadiusX;
	}
	
	/** Set value of Parameter argument maskRadiusX.
	  * @param value New value for maskRadiusX
	  */
	public void setMaskRadiusX( int value ) {
		 this.maskRadiusX = value;
	}

	/** Get value of Parameter argument maskRadiusT.
	  * @return value of maskRadiusT
	  */
	public int getMaskRadiusT() {
		return this.maskRadiusT;
	}
	
	/** Set value of Parameter argument maskRadiusT.
	  * @param value New value for maskRadiusT
	  */
	public void setMaskRadiusT( int value ) {
		 this.maskRadiusT = value;
	}

	/** Get value of Parameter argument rankOpMode.
	  * @return value of rankOpMode
	  */
	public RankOpMode getRankOpMode() {
		return this.rankOpMode;
	}
	
	/** Set value of Parameter argument rankOpMode.
	  * @param value New value for rankOpMode
	  */
	public void setRankOpMode( RankOpMode opMode ) {
		 this.rankOpMode = opMode;
	}

	/** Get value of Parameter argument maskRadiusZ.
	  * @return value of maskRadiusZ
	  */
	public int getMaskRadiusZ() throws ALDOperatorException {
		return this.maskRadiusZ;
	}
	
	/** Set value of Parameter argument maskRadiusZ.
	  * @param value New value for maskRadiusZ
	  */
	public void setMaskRadiusZ( int value ) throws ALDOperatorException {
		 this.maskRadiusZ = value;
	}

	/** Get value of Parameter argument maskRadiusC.
	  * @return value of maskRadiusC
	  */
	public int getMaskRadiusC() throws ALDOperatorException {
		return this.maskRadiusC;
	}
	
	/** Set value of Parameter argument maskRadiusC.
	  * @param value New value for maskRadiusC
	  */
	public void setMaskRadiusC( int value ) throws ALDOperatorException {
		 this.maskRadiusC = value;
	}
	
	/** Get value of Parameter argument maskRadiusY.
	  * @return value of maskRadiusY
	  */
	public int getMaskRadiusY() throws ALDOperatorException {
		return this.maskRadiusY;
	}
	
	/** Set value of Parameter argument maskRadiusY.
	  * @param value New value for maskRadiusY
	  */
	public void setMaskRadiusY( int value ) throws ALDOperatorException {
		 this.maskRadiusY = value;
	}

	/** Get value of Input argument inImg.
	  * @return value of inImg
	  */
	public MTBImage getInImg() throws ALDOperatorException {
		return this.inImg;
	}
	
	/** Set value of Input argument inImg.
	  * @param value New value for inImg
	  */
	public void setInImg( MTBImage img ) throws ALDOperatorException {
		 this.inImg = img;
	}

	/** Get value of Output argument resultImg.
	  * @return value of resultImg
	  */
	public MTBImage getResultImg() throws ALDOperatorException {
		return this.resultImg;
	}
	
	/** Set value of Output argument resultImg.
	  * @param value New value for resultImg
	  */
	protected void setResultImg( MTBImage img ) throws ALDOperatorException {
		 this.resultImg = img;
	}

	@Override
	public String getDocumentation() {
		return "<p>A rank operator that implements the Median-, Minimum- and Maximum-filter. To compute the new value of a pixel the rank operator considers pixels in the current pixel's neighborhood determined by mask. The current pixel value is then given by the median, the minimum or the maximum value of the considered pixel values.</p>\n" + 
				"<h3>Required input:</h3>\n" + 
				"\n" + 
				"<ul><li>\n" + 
				"<p><b>Input image</b>:</p>\n" + 
				"\n" + 
				"<p>Image to be filtered</p>\n" + 
				"</li><li>\n" + 
				"<p><b>Rank operation</b>:</p>\n" + 
				"\n" + 
				"<p>Determines the rank operation:</p>\n" + 
				"\n" + 
				"<ul><li>\n" + 
				"<p>MEDIAN: Median-filter</p>\n" + 
				"</li><li>\n" + 
				"<p>MIN: Minimum-filter</p>\n" + 
				"</li><li>\n" + 
				"<p>MAX: Maximum-filter</p>\n" + 
				"</li></ul>\n" + 
				"</li><li>\n" + 
				"<p><b>Mask-radius x</b>: </p>\n" + 
				"\n" + 
				"<p>The radius of the mask in x-dimension in a city-block sense (rectangular mask)</p>\n" + 
				"</li><li>\n" + 
				"<p><b>Mask-radius y</b>: </p>\n" + 
				"\n" + 
				"<p>The radius of the mask in y-dimension in a city-block sense (rectangular mask)</p>\n" + 
				"</li><li>\n" + 
				"<p><b>Mask-radius z</b>: </p>\n" + 
				"\n" + 
				"<p>The radius of the mask in z-dimension in a city-block sense (rectangular mask)</p>\n" + 
				"</li><li>\n" + 
				"<p><b>Mask-radius t</b>: </p>\n" + 
				"\n" + 
				"<p>The radius of the mask in t-dimension in a city-block sense (rectangular mask)</p>\n" + 
				"</li><li>\n" + 
				"<p><b>Mask-radius c</b>: </p>\n" + 
				"\n" + 
				"<p>The radius of the mask in c-dimension in a city-block sense (rectangular mask)</p>\n" + 
				"</li></ul>\n" + 
				"<h3>Output:</h3>\n" + 
				"\n" + 
				"<ul><li>\n" + 
				"<p><b>Result image</b>:</p>\n" + 
				"\n" + 
				"<p>The filtered image of same type as the input image</p>\n" + 
				"</li></ul>\n";
	}
}

