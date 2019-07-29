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

package de.unihalle.informatik.MiToBo.core.datatypes.images;

import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.NewImage;

/**
 * Class for easy access to Int (hyper)stacks. The Int type is a Non-ImageJ type, 
 * thus the data is not a reference to ImagePlus data, but is allocated for this MTBImage object.
 * Indices range is different from ImageJ
 * Here, indices in each dimension range from 0 to (dimSize - 1), while
 * ImageJ stack indices range from 1 to dimSize.
 * 
 * @author gress
 *
 */
public class MTBImageInt extends MTBImage {
	
	/** image data */
	protected int m_data[][];

	/** slice labels */
	protected String[] m_sliceLabels;
	
	/**
	 * Constructor
	 * @param sizeX size in x-dimension
	 * @param sizeY size in y-dimension
	 * @param sizeZ size in z-dimension
	 * @param sizeT size in t-dimension
	 * @param sizeC size in c-dimension
	 */
	protected MTBImageInt(int sizeX, int sizeY, int sizeZ, int sizeT, int sizeC) {
		super();
		
		// dimension sizes
		m_sizeX = sizeX;
		m_sizeY = sizeY;
		m_sizeZ = sizeZ;
		m_sizeT = sizeT;
		m_sizeC = sizeC;	

		this.setProperty("SizeX", m_sizeX);
		this.setProperty("SizeY", m_sizeY);
		this.setProperty("SizeZ", m_sizeZ);
		this.setProperty("SizeT", m_sizeT);
		this.setProperty("SizeC", m_sizeC);

		m_sizeStack = m_sizeZ*m_sizeT*m_sizeC;
		
		// set image type
		m_type = MTBImageType.MTB_INT;
		
		// create data array
		m_data = new int[m_sizeStack][m_sizeX*m_sizeY];
		
		// create slice label array
		m_sliceLabels = new String[m_sizeStack];
	}
	
	/**
	 * Get an ImagePlus object. 
	 * An ImagePlus object of type ImagePlus.GRAY32 (FloatProcessor) is created and int values are casted to float.
	 * The ImagePlus diplay range is set to [min, max] of the MTBImage.
	 * This ImagePlus does not share memory with this MTBImage.
	 * @return ImagePlus object
	 */
	@Override
	public ImagePlus getImagePlus() {

		this.updateImagePlus();
		
		// return ImagePlus
		return m_img;
	}
	
	/**
	 * Creates or updates an ImagePlus of Float type from the MTBImage Int data, which can be returned by getImagePlus() or displayed by show()
	 */
	@Override
	protected void updateImagePlus() {
		
		if (m_img == null) {
			// create new ImagePlus
			m_img = NewImage.createFloatImage(this.getTitle(), 
					this.m_sizeX, this.m_sizeY, this.m_sizeStack,
					NewImage.FILL_BLACK);
			m_img.setIgnoreFlush(true);
			this.m_img.setCalibration(this.calibration);
			// setCalibration on ImagePlus creates new object, preserver consistency!
			this.calibration = this.m_img.getCalibration();  
			m_img.setIgnoreFlush(true);
		}
		
		m_img.setDimensions(m_sizeC, m_sizeZ, m_sizeT);
		m_img.setOpenAsHyperStack((m_sizeC > 1) || (m_sizeT > 1));
		
		float[] pixels;
		ImageStack stack = m_img.getStack();
		
		for (int i = 1; i <= m_sizeStack; i++) {

			pixels = (float[]) stack.getProcessor(i).getPixels();
			
			for (int j = 0; j < pixels.length; j++) {
				pixels[j] = (float)m_data[i-1][j];
			}
			
			stack.setSliceLabel(this.m_sliceLabels[i-1], i);
		}
		
		// reference to this MTBImage from the ImagePlus object
		m_img.setProperty("MTBImage", this);
		
		// set display range
		double[] minmax = this.getMinMaxDouble();
		m_img.setDisplayRange(minmax[0], minmax[1]);
	}
	
	/**
	 * Get the slice label of the slice specified by the actual slice index
	 * @return
	 */
	@Override
	public String getCurrentSliceLabel() {
		return this.m_sliceLabels[this.m_currentSliceIdx];
	}
  
	/**
	 * Set the slice label of the slice specified by the actual slice index
	 * @param label
	 */
	@Override
	public void setCurrentSliceLabel(String label) {
		this.m_sliceLabels[this.m_currentSliceIdx] = label;
		if (m_img != null) {
			m_img.getStack().setSliceLabel(label, this.m_currentSliceIdx + 1);
		} 
	}
	
	/**
	 * Get the voxel value of the 5D image at coordinate (x,y,z,t,c)
	 * No test of coordinate validity
	 * @param x x-coordinate ranging from 0 to (sizeX - 1)
	 * @param y y-coordinate ranging from 0 to (sizeY - 1)
	 * @param z z-coordinate ranging from 0 to (sizeZ - 1)
	 * @param t t-coordinate ranging from 0 to (sizeT - 1)
	 * @param c c-coordinate ranging from 0 to (sizeC - 1)
	 * @return value as int
	 */
	@Override
	public int getValueInt(int x, int y, int z, int t, int c) {
				
        return m_data[t*m_sizeC*m_sizeZ + z*m_sizeC + c][y*m_sizeX + x];
	}

	/**
	 * Get the voxel value of the 5D image at coordinate (x,y,z,t,c)
	 * No test of coordinate validity
	 * @param x x-coordinate ranging from 0 to (sizeX - 1)
	 * @param y y-coordinate ranging from 0 to (sizeY - 1)
	 * @param z z-coordinate ranging from 0 to (sizeZ - 1)
	 * @param t t-coordinate ranging from 0 to (sizeT - 1)
	 * @param c c-coordinate ranging from 0 to (sizeC - 1)
	 * @return value as int
	 */
	@Override
	public double getValueDouble(int x, int y, int z, int t, int c) {
				
        return (double)m_data[t*m_sizeC*m_sizeZ + z*m_sizeC + c][y*m_sizeX + x];
	}

	/**
	 * Set the voxel value of the 5D image at coordinate (x,y,z,t,c)
	 * @param x x-coordinate ranging from 0 to (sizeX - 1)
	 * @param y y-coordinate ranging from 0 to (sizeY - 1)
	 * @param z z-coordinate ranging from 0 to (sizeZ - 1)
	 * @param t t-coordinate ranging from 0 to (sizeT - 1)
	 * @param c c-coordinate ranging from 0 to (sizeC - 1)
	 * @param value to set the voxel to 
	 */	
	@Override
	public void putValueInt(int x, int y, int z, int t, int c, int value) {
		
        m_data[t*m_sizeC*m_sizeZ + z*m_sizeC + c][y*m_sizeX + x] = value;	
	}

	/**
	 * Set the voxel value of the 5D image at coordinate (x,y,z,t,c)
	 * @param x x-coordinate ranging from 0 to (sizeX - 1)
	 * @param y y-coordinate ranging from 0 to (sizeY - 1)
	 * @param z z-coordinate ranging from 0 to (sizeZ - 1)
	 * @param t t-coordinate ranging from 0 to (sizeT - 1)
	 * @param c c-coordinate ranging from 0 to (sizeC - 1)
	 * @param value to set the voxel to 
	 */	
	@Override
	public void putValueDouble(int x, int y, int z, int t, int c, double value) {
		
        m_data[t*m_sizeC*m_sizeZ + z*m_sizeC + c][y*m_sizeX + x] = (int)value;	
	}
	
	/**
	 * Get the voxel value of the actual z-stack at coordinate (x,y,z)
	 * No test of coordinate validity
	 * @param x x-coordinate ranging from 0 to (sizeX - 1)
	 * @param y y-coordinate ranging from 0 to (sizeY - 1)
	 * @param z z-coordinate ranging from 0 to (sizeZ - 1)
	 * @return value as int
	 */
	@Override
	public int getValueInt(int x, int y, int z) {
				
        return m_data[m_currentT*m_sizeC*m_sizeZ + z*m_sizeC + m_currentC][y*m_sizeX + x];
	}
	
	/**
	 * Get the voxel value of the actual z-stack at coordinate (x,y,z)
	 * No test of coordinate validity
	 * @param x x-coordinate ranging from 0 to (sizeX - 1)
	 * @param y y-coordinate ranging from 0 to (sizeY - 1)
	 * @param z z-coordinate ranging from 0 to (sizeZ - 1)
	 * @return value as int
	 */
	@Override
	public double getValueDouble(int x, int y, int z) {
				
        return (double)m_data[m_currentT*m_sizeC*m_sizeZ + z*m_sizeC + m_currentC][y*m_sizeX + x] ;
	}
	
	/**
	 * Set the voxel value of the actual z-stack at coordinate (x,y,z)
	 * @param x x-coordinate ranging from 0 to (sizeX - 1)
	 * @param y y-coordinate ranging from 0 to (sizeY - 1)
	 * @param z z-coordinate ranging from 0 to (sizeZ - 1)
	 * @param value to set the voxel to 
	 */	
	@Override
	public void putValueInt(int x, int y, int z, int value) {
		
        m_data[m_currentT*m_sizeC*m_sizeZ + z*m_sizeC + m_currentC][y*m_sizeX + x] = value;	
	}
	
	/**
	 * Set the voxel value of the actual z-stack at coordinate (x,y,z)
	 * @param x x-coordinate ranging from 0 to (sizeX - 1)
	 * @param y y-coordinate ranging from 0 to (sizeY - 1)
	 * @param z z-coordinate ranging from 0 to (sizeZ - 1)
	 * @param value to set the voxel to 
	 */	
	@Override
	public void putValueDouble(int x, int y, int z, double value) {
		
        m_data[m_currentT*m_sizeC*m_sizeZ + z*m_sizeC + m_currentC][y*m_sizeX + x] = (int)value;	
	}
	
	/**
	 * Get the value of the actual slice at coordinate (x,y) as an Integer
	 * @param x x-coordinate ranging from 0 to (sizeX - 1)
	 * @param y y-coordinate ranging from 0 to (sizeY - 1)
	 * @return voxel value
	 */
	@Override
	public int getValueInt(int x, int y) {
        return m_data[m_currentSliceIdx][y*m_sizeX + x];
	}
	
	/**
	 * Get the value of the actual slice at coordinate (x,y) as an Double
	 * @param x x-coordinate ranging from 0 to (sizeX - 1)
	 * @param y y-coordinate ranging from 0 to (sizeY - 1)
	 * @return voxel value
	 */
	@Override
	public double getValueDouble(int x, int y) {
		return (double)m_data[m_currentSliceIdx][y*m_sizeX + x];
	}
	
	/**
	 * Set the value of the actual slice at coordinate (x,y) using an Integer
	 * @param x x-coordinate ranging from 0 to (sizeX - 1)
	 * @param y y-coordinate ranging from 0 to (sizeY - 1)
	 * @param value to set the voxel to 
	 */	
	@Override
	public void putValueInt(int x, int y, int value) {
		m_data[m_currentSliceIdx][y*m_sizeX + x] = value;	
	}
	
	/**
	 * Set the value of the actual slice at coordinate (x,y) using a Double
	 * @param x x-coordinate ranging from 0 to (sizeX - 1)
	 * @param y y-coordinate ranging from 0 to (sizeY - 1)
	 * @param value to set the voxel to 
	 */	
	@Override
	public void putValueDouble(int x, int y, double value) {
		m_data[m_currentSliceIdx][y*m_sizeX + x] = (int)value;
	}

	/**
	 * Get minimum and maximum value of the image as int
	 * @return min at int[0], max at int[1]
	 */
	@Override
	public int[] getMinMaxInt() {
		int[] minmax = null;
		int sizeXY = m_sizeX*m_sizeY;
		int val;
		
		for (int i = 0; i < m_sizeStack; i++) {
			for (int j = 0; j < sizeXY; j++) {
				
				val = m_data[i][j];
				
				if (minmax == null) {
					minmax = new int[2];
					minmax[0] = val;
					minmax[1] = val;
				}
				else {
					
					if (val < minmax[0])
						minmax[0] = val;
					
					if (val > minmax[1])
						minmax[1] = val;
				}		
			}
		}
		
		return minmax;
	}
	
	/**
	 * Get minimum and maximum value of the image as double
	 * @return min at double[0], max at double[1]
	 */
	@Override
	public double[] getMinMaxDouble() {
		double[] minmax = null;
		int sizeXY = m_sizeX*m_sizeY;
		double val;
		
		for (int i = 0; i < m_sizeStack; i++) {
			for (int j = 0; j < sizeXY; j++) {
				
				val = m_data[i][j];
				
				if (minmax == null) {
					minmax = new double[2];
					minmax[0] = val;
					minmax[1] = val;
				}
				else {
					
					if (val < minmax[0])
						minmax[0] = val;
					
					if (val > minmax[1])
						minmax[1] = val;
				}		
			}
		}
		
		return minmax;
	}
	
}
