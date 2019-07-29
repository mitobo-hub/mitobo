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
import ij.process.ColorProcessor;

/**
 * Class for easy access to RGB (hyper)stacks. This image consists of three separate byte images, 
 * thus the data is not a reference to ImagePlus data, but is allocated for this MTBImage object.
 * Indices range is different from ImageJ
 * Here, indices in each dimension range from 0 to (dimSize - 1), while
 * ImageJ stack indices range from 1 to dimSize.
 * 
 * @author gress
 *
 */
public class MTBImageRGB extends MTBImage {
	
	// ----- member fields
	/** reference to the ImagePlus pixel data (red channel) */
	protected byte[][] m_dataR;
	
	/** reference to the ImagePlus pixel data (green channel) */
	protected byte[][] m_dataG;
	
	/** reference to the ImagePlus pixel data (blue channel) */
	protected byte[][] m_dataB;
	
	/** reference to the red channel MTBImage */
	private MTBImageByte m_imgR;
	
	/** reference to the red channel MTBImage */
	private MTBImageByte m_imgG;
	
	/** reference to the red channel MTBImage */
	private MTBImageByte m_imgB;	

	/** slice labels */
	protected String[] m_sliceLabels;
	
	/**
	 * Constructor
	 * @param img
	 */
	protected MTBImageRGB(ImagePlus img) {
		super(img);
		
		// set image type
		this.m_type = MTBImageType.MTB_RGB;
		
		// create slice label array
		this.m_sliceLabels = new String[this.m_sizeStack];
		
		// init data arrays
		this.m_dataR = new byte[this.m_sizeStack][];
		this.m_dataG = new byte[this.m_sizeStack][];
		this.m_dataB = new byte[this.m_sizeStack][];
		
		for (int i = 0; i < this.m_sizeStack; i++) {
			this.m_dataR[i] = new byte[this.m_sizeX*this.m_sizeY];
			this.m_dataG[i] = new byte[this.m_sizeX*this.m_sizeY];
			this.m_dataB[i] = new byte[this.m_sizeX*this.m_sizeY];
			
			//((ColorProcessor)m_imgStack.getProcessor(i)).getRGB(m_dataR[i-1], m_dataG[i-1], m_dataB[i-1]);
		}
		
		// get data from underlying image
		for (int i = 1; i <= this.m_sizeStack; i++) {
			((ColorProcessor)this.m_imgStack.getProcessor(i)).getRGB(this.m_dataR[i-1], this.m_dataG[i-1], this.m_dataB[i-1]);
		}
		
		this.m_imgR = MTBImage.createMTBImageByte(this.m_sizeX, this.m_sizeY, this.m_sizeZ, this.m_sizeT, this.m_sizeC, this.m_dataR);
		this.m_imgG = MTBImage.createMTBImageByte(this.m_sizeX, this.m_sizeY, this.m_sizeZ, this.m_sizeT, this.m_sizeC, this.m_dataG);
		this.m_imgB = MTBImage.createMTBImageByte(this.m_sizeX, this.m_sizeY, this.m_sizeZ, this.m_sizeT, this.m_sizeC, this.m_dataB);

		this.m_imgR.setCalibration(this.calibration);
		this.m_imgG.setCalibration(this.calibration);
		this.m_imgB.setCalibration(this.calibration);
		
		if (this.xml != null) {
			this.m_imgR.setXML(this.xml);
			this.m_imgG.setXML(this.xml);
			this.m_imgB.setXML(this.xml);
		}
		
		// reference to this MTBImage from the ImagePlus object
		this.m_img.setProperty("MTBImage", this);
	}
	
	/**
	 * Constructor
	 * @param sizeX size in x-dimension
	 * @param sizeY size in y-dimension
	 * @param sizeZ size in z-dimension
	 * @param sizeT size in t-dimension
	 * @param sizeC size in c-dimension
	 */
	protected MTBImageRGB(int sizeX, int sizeY, int sizeZ, int sizeT, int sizeC) {
		super();
		
		// dimension sizes
		this.m_sizeX = sizeX;
		this.m_sizeY = sizeY;
		this.m_sizeZ = sizeZ;
		this.m_sizeT = sizeT;
		this.m_sizeC = sizeC;		
		
		this.setProperty("SizeX", new Integer(this.m_sizeX));
		this.setProperty("SizeY", new Integer(this.m_sizeY));
		this.setProperty("SizeZ", new Integer(this.m_sizeZ));
		this.setProperty("SizeT", new Integer(this.m_sizeT));
		this.setProperty("SizeC", new Integer(this.m_sizeC));
		
		this.m_sizeStack = this.m_sizeZ*this.m_sizeT*this.m_sizeC;
		
		// set image type
		this.m_type = MTBImageType.MTB_RGB;
		
		// create slice label array
		this.m_sliceLabels = new String[this.m_sizeStack];
		
		// init data arrays
		this.m_dataR = new byte[this.m_sizeStack][];
		this.m_dataG = new byte[this.m_sizeStack][];
		this.m_dataB = new byte[this.m_sizeStack][];
	
		// get data from underlying image
		for (int i = 0; i < this.m_sizeStack; i++) {
			this.m_dataR[i] = new byte[this.m_sizeX*this.m_sizeY];
			this.m_dataG[i] = new byte[this.m_sizeX*this.m_sizeY];
			this.m_dataB[i] = new byte[this.m_sizeX*this.m_sizeY];
		}
		
		this.m_imgR = MTBImage.createMTBImageByte(this.m_sizeX, this.m_sizeY, this.m_sizeZ, this.m_sizeT, this.m_sizeC, this.m_dataR);
		this.m_imgG = MTBImage.createMTBImageByte(this.m_sizeX, this.m_sizeY, this.m_sizeZ, this.m_sizeT, this.m_sizeC, this.m_dataG);
		this.m_imgB = MTBImage.createMTBImageByte(this.m_sizeX, this.m_sizeY, this.m_sizeZ, this.m_sizeT, this.m_sizeC, this.m_dataB);
		
		this.m_imgR.setCalibration(this.calibration);
		this.m_imgG.setCalibration(this.calibration);
		this.m_imgB.setCalibration(this.calibration);
		
		this.setTitle(this.getTitle());
	}

	/**
	 * Set the images title
	 * @param title
	 */
	@Override
	public void setTitle(String title) {
		// null title string are not allowed as they cannot be handled by ImageJ
		if (title == null)
			return;
		super.setTitle(title);
		this.m_imgR.setTitle(title + " [red]");
		this.m_imgG.setTitle(title + " [green]");
		this.m_imgB.setTitle(title + " [blue]");
	}
	
	/**
	 * Get an ImagePlus object. 
	 * An ImagePlus object of type ImagePlus.COLOR_RGB is created and returned. This ImagePlus does not share memory with this MTBImage.
	 * @return ImagePlus object
	 */
	@Override
	public ImagePlus getImagePlus() {

		this.updateImagePlus();
		
		// return ImagePlus
		return this.m_img;
	}
	
	/**
	 * Creates or updates an ImagePlus of RGB type from the MTBImage RGB(3 byte-channels) data, can be returned by getImagePlus() or displayed by show()
	 */
	@Override
	protected void updateImagePlus() {
		
		if (this.m_img == null) {
			// create new ImagePlus
			this.m_img = NewImage.createRGBImage(this.getTitle(), 
					this.m_sizeX, this.m_sizeY, this.m_sizeStack, 
					NewImage.FILL_BLACK);
			this.m_img.setCalibration(this.calibration);
			// setCalibration on ImagePlus creates new object, preserve consistency!
			this.calibration = this.m_img.getCalibration();  
			this.m_img.setIgnoreFlush(true);
		}
		
		this.m_img.setDimensions(this.m_sizeC, this.m_sizeZ, this.m_sizeT);
		this.m_img.setOpenAsHyperStack((this.m_sizeC > 1) || (this.m_sizeT > 1));
		
		int[] pixels;
		ImageStack stack = this.m_img.getStack();
		
		for (int i = 1; i <= this.m_sizeStack; i++) {

			pixels = (int[]) stack.getProcessor(i).getPixels();
			
			for (int j = 0; j < pixels.length; j++) {
				// put channel values in an int
				pixels[j] = ((this.m_dataR[i-1][j] & 0xff) << 16) 
				          + ((this.m_dataG[i-1][j] & 0xff) << 8)
				          +  (this.m_dataB[i-1][j] & 0xff);
			}
			
			stack.setSliceLabel(this.m_sliceLabels[i-1], i);
		}
		
		// reference to this MTBImage from the ImagePlus object
		this.m_img.setProperty("MTBImage", this);
		// make sure that image title is synchronized
		this.m_img.setTitle(this.getTitle());
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
		if (this.m_img != null) {
			this.m_img.getStack().setSliceLabel(label, this.m_currentSliceIdx + 1);
		} 
		
		int tmpIdx = this.m_imgR.m_currentSliceIdx;
		this.m_imgR.m_currentSliceIdx = this.m_currentSliceIdx;
		this.m_imgR.setCurrentSliceLabel(label);
		this.m_imgR.m_currentSliceIdx = tmpIdx;
		
		tmpIdx = this.m_imgG.m_currentSliceIdx;
		this.m_imgG.m_currentSliceIdx = this.m_currentSliceIdx;
		this.m_imgG.setCurrentSliceLabel(label);
		this.m_imgG.m_currentSliceIdx = tmpIdx;
		
		tmpIdx = this.m_imgB.m_currentSliceIdx;
		this.m_imgB.m_currentSliceIdx = this.m_currentSliceIdx;
		this.m_imgB.setCurrentSliceLabel(label);
		this.m_imgB.m_currentSliceIdx = tmpIdx;
	}
	
	/**
	 * Set the physical size of a voxel (stepsize) in x-dimension.
	 * @param stepsize	Stepsize in x-dimension.
	 */
	@Override
	public void setStepsizeX(double stepsize) {
		this.calibration.pixelWidth = stepsize;
		this.setProperty("StepsizeX", new Double(stepsize));

		if (this.m_imgR != null)
			this.m_imgR.setStepsizeX(stepsize);
		if (this.m_imgG != null)
			this.m_imgG.setStepsizeX(stepsize);
		if (this.m_imgB != null)
			this.m_imgB.setStepsizeX(stepsize);
	}
	
	/**
	 * Set the physical size of a voxel (stepsize) in y-dimension.
	 * @param stepsize	Stepsize in y-dimension.
	 */
	@Override
	public void setStepsizeY(double stepsize) {
		this.calibration.pixelHeight = stepsize;
		this.setProperty("StepsizeY", new Double(stepsize));

		if (this.m_imgR != null)
			this.m_imgR.setStepsizeY(stepsize);
		if (this.m_imgG != null)
			this.m_imgG.setStepsizeY(stepsize);
		if (this.m_imgB != null)
			this.m_imgB.setStepsizeY(stepsize);
	}

	/**
	 * Set the physical size of a voxel (stepsize) in z-dimension.
	 * @param stepsize	Stepsize in z-dimension.
	 */
	@Override
	public void setStepsizeZ(double stepsize) {
		this.calibration.pixelDepth = stepsize;
		this.setProperty("StepsizeZ", new Double(stepsize));

		if (this.m_imgR != null)
			this.m_imgR.setStepsizeZ(stepsize);
		if (this.m_imgG != null)
			this.m_imgG.setStepsizeZ(stepsize);
		if (this.m_imgB != null)
			this.m_imgB.setStepsizeZ(stepsize);
	}
	
	/**
	 * Set the stepsize in t-dimension (timestep).
	 * @param stepsize	Stepsize in t-dimension.
	 */
	@Override
	public void setStepsizeT(double stepsize) {
		this.calibration.frameInterval = stepsize;
		this.setProperty("StepsizeT", new Double(stepsize));

		if (this.m_imgR != null)
			this.m_imgR.setStepsizeT(stepsize);
		if (this.m_imgG != null)
			this.m_imgG.setStepsizeT(stepsize);
		if (this.m_imgB != null)
			this.m_imgB.setStepsizeT(stepsize);
	}
	
	/**
	 * Set the unit of the x-dimension.
	 * @param unit	String of x-dimension unit.
	 */
	@Override
	public void setUnitX(String unit) {
		this.calibration.setXUnit(unit);
		this.setProperty("UnitX", unit);

		if (this.m_imgR != null)
			this.m_imgR.setUnitX(unit);
		if (this.m_imgG != null)
			this.m_imgG.setUnitX(unit);
		if (this.m_imgB != null)
			this.m_imgB.setUnitX(unit);
	}

	/**
	 * Set the unit of the y-dimension.
	 * @param unit	String of y-dimension unit.
	 */
	@Override
	public void setUnitY(String unit) {
		this.calibration.setYUnit(unit);
		this.setProperty("UnitY", unit);

		if (this.m_imgR != null)
			this.m_imgR.setUnitY(unit);
		if (this.m_imgG != null)
			this.m_imgG.setUnitY(unit);
		if (this.m_imgB != null)
			this.m_imgB.setUnitY(unit);
	}

	/**
	 * Set the unit of the z-dimension.
	 * @param unit	String of z-dimension unit.
	 */
	@Override
	public void setUnitZ(String unit) {
		this.calibration.setZUnit(unit);
		this.setProperty("UnitZ", unit);

		if (this.m_imgR != null)
			this.m_imgR.setUnitZ(unit);
		if (this.m_imgG != null)
			this.m_imgG.setUnitZ(unit);
		if (this.m_imgB != null)
			this.m_imgB.setUnitZ(unit);
	}

	/**
	 * Set the unit of the t-dimension.
	 * @param unit	String of t-dimension unit.
	 */
	@Override
	public void setUnitT(String unit) {
		this.calibration.setTimeUnit(unit);
		this.setProperty("UnitT", unit);

		if (this.m_imgR != null)
			this.m_imgR.setUnitT(unit);
		if (this.m_imgG != null)
			this.m_imgG.setUnitT(unit);
		if (this.m_imgB != null)
			this.m_imgB.setUnitT(unit);
	}
	
	
	/**
	 * Get minimum and maximum value of the image (all channels) as double
	 * @return min at double[0], max at double[1]
	 */
	@Override
	public double[] getMinMaxDouble() {
		double[] minmaxR = this.m_imgR.getMinMaxDouble();
		double[] minmaxG = this.m_imgG.getMinMaxDouble();
		double[] minmaxB = this.m_imgB.getMinMaxDouble();
		
		if (minmaxR[0] < minmaxG[0]) {
			if (minmaxB[0] < minmaxR[0]) {
				minmaxR[0] = minmaxB[0];
			}
		}
		else {
			if (minmaxB[0] < minmaxG[0]) {
				minmaxR[0] = minmaxB[0];
			}
			else {
				minmaxR[0] = minmaxG[0];
			}
		}
		
		if (minmaxR[1] > minmaxG[1]) {
			if (minmaxB[1] > minmaxR[1]) {
				minmaxR[1] = minmaxB[1];
			}
		}
		else {
			if (minmaxB[1] > minmaxG[1]) {
				minmaxR[1] = minmaxB[1];
			}
			else {
				minmaxR[1] = minmaxG[1];
			}
		}	
		
		return minmaxR;
	}
	
	/**
	 * Get minimum and maximum value of the image as int
	 * @return min at int[0], max at int[1]
	 */
	@Override
	public int[] getMinMaxInt() {		
		int[] minmaxR = this.m_imgR.getMinMaxInt();
		int[] minmaxG = this.m_imgG.getMinMaxInt();
		int[] minmaxB = this.m_imgB.getMinMaxInt();
		
		if (minmaxR[0] < minmaxG[0]) {
			if (minmaxB[0] < minmaxR[0]) {
				minmaxR[0] = minmaxB[0];
			}
		}
		else {
			if (minmaxB[0] < minmaxG[0]) {
				minmaxR[0] = minmaxB[0];
			}
			else {
				minmaxR[0] = minmaxG[0];
			}
		}
		
		if (minmaxR[1] > minmaxG[1]) {
			if (minmaxB[1] > minmaxR[1]) {
				minmaxR[1] = minmaxB[1];
			}
		}
		else {
			if (minmaxB[1] > minmaxG[1]) {
				minmaxR[1] = minmaxB[1];
			}
			else {
				minmaxR[1] = minmaxG[1];
			}
		}	
		
		return minmaxR;
	}

	
	/**
	 * Get the voxel value of the 5D image at coordinate (x,y,z,t,c)
	 * No test of coordinate validity
	 * @param x x-coordinate ranging from 0 to (sizeX - 1)
	 * @param y y-coordinate ranging from 0 to (sizeY - 1)
	 * @param z z-coordinate ranging from 0 to (sizeZ - 1)
	 * @param t t-coordinate ranging from 0 to (sizeT - 1)
	 * @param c c-coordinate ranging from 0 to (sizeC - 1)
	 * @return RGB value (three byte-values) stored in an int (like ImageJ), then casted to double
	 */
	@Override
	public double getValueDouble(int x, int y, int z, int t, int c) {
		return (double)((this.m_dataR[t*this.m_sizeC*this.m_sizeZ + z*this.m_sizeC + c][y*this.m_sizeX + x] & 0xff) << 16)
						+ ((this.m_dataG[t*this.m_sizeC*this.m_sizeZ + z*this.m_sizeC + c][y*this.m_sizeX + x] & 0xff) << 8)
						+  (this.m_dataB[t*this.m_sizeC*this.m_sizeZ + z*this.m_sizeC + c][y*this.m_sizeX + x] & 0xff);
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
				
		return (double)((this.m_dataR[this.m_currentT*this.m_sizeC*this.m_sizeZ + z*this.m_sizeC + this.m_currentC][y*this.m_sizeX + x] & 0xff) << 16)
						+ ((this.m_dataG[this.m_currentT*this.m_sizeC*this.m_sizeZ + z*this.m_sizeC + this.m_currentC][y*this.m_sizeX + x] & 0xff) << 8)
						+  (this.m_dataB[this.m_currentT*this.m_sizeC*this.m_sizeZ + z*this.m_sizeC + this.m_currentC][y*this.m_sizeX + x] & 0xff);
	}
	
	/**
	 * Get the value of the actual slice at coordinate (x,y) as a Double
	 * @param x x-coordinate ranging from 0 to (sizeX - 1)
	 * @param y y-coordinate ranging from 0 to (sizeY - 1)
	 * @return RGB value (three byte-values) stored in an int (like ImageJ), then casted to double
	 */
	@Override
	public double getValueDouble(int x, int y) {
		return (double)((this.m_dataR[this.m_currentSliceIdx][y*this.m_sizeX + x] & 0xff) << 16)
						+ ((this.m_dataG[this.m_currentSliceIdx][y*this.m_sizeX + x] & 0xff) << 8)
						+  (this.m_dataB[this.m_currentSliceIdx][y*this.m_sizeX + x] & 0xff);
	}

	/**
	 * Get the voxel value of the 5D image at coordinate (x,y,z,t,c)
	 * No test of coordinate validity
	 * @param x x-coordinate ranging from 0 to (sizeX - 1)
	 * @param y y-coordinate ranging from 0 to (sizeY - 1)
	 * @param z z-coordinate ranging from 0 to (sizeZ - 1)
	 * @param t t-coordinate ranging from 0 to (sizeT - 1)
	 * @param c c-coordinate ranging from 0 to (sizeC - 1)
	 * @return RGB value (three byte-values) stored in an int (like ImageJ) 
	 */
	@Override
	public int getValueInt(int x, int y, int z, int t, int c) {
		return ((this.m_dataR[t*this.m_sizeC*this.m_sizeZ + z*this.m_sizeC + c][y*this.m_sizeX + x] & 0xff) << 16)
				+ ((this.m_dataG[t*this.m_sizeC*this.m_sizeZ + z*this.m_sizeC + c][y*this.m_sizeX + x] & 0xff) << 8)
				+  (this.m_dataB[t*this.m_sizeC*this.m_sizeZ + z*this.m_sizeC + c][y*this.m_sizeX + x] & 0xff);
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
				
		return ((this.m_dataR[this.m_currentT*this.m_sizeC*this.m_sizeZ + z*this.m_sizeC + this.m_currentC][y*this.m_sizeX + x] & 0xff) << 16)
				+ ((this.m_dataG[this.m_currentT*this.m_sizeC*this.m_sizeZ + z*this.m_sizeC + this.m_currentC][y*this.m_sizeX + x] & 0xff) << 8)
				+  (this.m_dataB[this.m_currentT*this.m_sizeC*this.m_sizeZ + z*this.m_sizeC + this.m_currentC][y*this.m_sizeX + x] & 0xff);
	}
	
	/**
	 * Get the value of the actual slice at coordinate (x,y) as an Integer
	 * @param x x-coordinate ranging from 0 to (sizeX - 1)
	 * @param y y-coordinate ranging from 0 to (sizeY - 1)
	 * @return RGB value (three byte-values) stored in an int (like ImageJ)
	 */
	@Override
	public int getValueInt(int x, int y) {
		return ((this.m_dataR[this.m_currentSliceIdx][y*this.m_sizeX + x] & 0xff) << 16)
				+ ((this.m_dataG[this.m_currentSliceIdx][y*this.m_sizeX + x] & 0xff) << 8)
				+  (this.m_dataB[this.m_currentSliceIdx][y*this.m_sizeX + x] & 0xff);
	}

	/**
	 * Set the voxel value of the 5D image at coordinate (x,y,z,t,c)
	 * @param x x-coordinate ranging from 0 to (sizeX - 1)
	 * @param y y-coordinate ranging from 0 to (sizeY - 1)
	 * @param z z-coordinate ranging from 0 to (sizeZ - 1)
	 * @param t t-coordinate ranging from 0 to (sizeT - 1)
	 * @param c c-coordinate ranging from 0 to (sizeC - 1)
	 * @param RGB value (three byte-values) stored in an int (like ImageJ), then casted to double
	 */	
	@Override
	public void putValueDouble(int x, int y, int z, int t, int c, double value) {
		int val = (int)value;
		this.m_dataR[t*this.m_sizeC*this.m_sizeZ + z*this.m_sizeC + c][y*this.m_sizeX + x] = (byte)((val & 0xff0000) >> 16);
		this.m_dataG[t*this.m_sizeC*this.m_sizeZ + z*this.m_sizeC + c][y*this.m_sizeX + x] = (byte)((val & 0x00ff00) >> 8);
		this.m_dataB[t*this.m_sizeC*this.m_sizeZ + z*this.m_sizeC + c][y*this.m_sizeX + x] = (byte)(val & 0x0000ff);
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
		
		int val = (int)value;
		this.m_dataR[this.m_currentT*this.m_sizeC*this.m_sizeZ + z*this.m_sizeC + this.m_currentC][y*this.m_sizeX + x] = (byte)((val & 0xff0000) >> 16);
		this.m_dataG[this.m_currentT*this.m_sizeC*this.m_sizeZ + z*this.m_sizeC + this.m_currentC][y*this.m_sizeX + x] = (byte)((val & 0x00ff00) >> 8);
		this.m_dataB[this.m_currentT*this.m_sizeC*this.m_sizeZ + z*this.m_sizeC + this.m_currentC][y*this.m_sizeX + x] = (byte)(val & 0x0000ff);
	}
	
	
	/**
	 * Set the value of the actual slice at coordinate (x,y) using a Double
	 * @param x x-coordinate ranging from 0 to (sizeX - 1)
	 * @param y y-coordinate ranging from 0 to (sizeY - 1)
	 * @param RGB value (three byte-values) stored in an int (like ImageJ), then casted to double
	 */	
	@Override
	public void putValueDouble(int x, int y, double value) {
		int val = (int)value;
		this.m_dataR[this.m_currentSliceIdx][y*this.m_sizeX + x] = (byte)((val & 0xff0000) >> 16);
		this.m_dataG[this.m_currentSliceIdx][y*this.m_sizeX + x] = (byte)((val & 0x00ff00) >> 8);
		this.m_dataB[this.m_currentSliceIdx][y*this.m_sizeX + x] = (byte)(val & 0x0000ff);
	}

	/**
	 * Set the voxel value of the 5D image at coordinate (x,y,z,t,c)
	 * @param x x-coordinate ranging from 0 to (sizeX - 1)
	 * @param y y-coordinate ranging from 0 to (sizeY - 1)
	 * @param z z-coordinate ranging from 0 to (sizeZ - 1)
	 * @param t t-coordinate ranging from 0 to (sizeT - 1)
	 * @param c c-coordinate ranging from 0 to (sizeC - 1)
	 * @param RGB value (three byte-values) stored in an int (like ImageJ) 
	 */	
	@Override
	public void putValueInt(int x, int y, int z, int t, int c, int value) {
		this.m_dataR[t*this.m_sizeC*this.m_sizeZ + z*this.m_sizeC + c][y*this.m_sizeX + x] = (byte)((value & 0xff0000) >> 16);
		this.m_dataG[t*this.m_sizeC*this.m_sizeZ + z*this.m_sizeC + c][y*this.m_sizeX + x] = (byte)((value & 0x00ff00) >> 8);
		this.m_dataB[t*this.m_sizeC*this.m_sizeZ + z*this.m_sizeC + c][y*this.m_sizeX + x] = (byte)(value & 0x0000ff);
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
		this.m_dataR[this.m_currentT*this.m_sizeC*this.m_sizeZ + z*this.m_sizeC + this.m_currentC][y*this.m_sizeX + x] = (byte)((value & 0xff0000) >> 16);
		this.m_dataG[this.m_currentT*this.m_sizeC*this.m_sizeZ + z*this.m_sizeC + this.m_currentC][y*this.m_sizeX + x] = (byte)((value & 0x00ff00) >> 8);
		this.m_dataB[this.m_currentT*this.m_sizeC*this.m_sizeZ + z*this.m_sizeC + this.m_currentC][y*this.m_sizeX + x] = (byte)(value & 0x0000ff);	
	}
	
	/**
	 * Set the value of the actual slice at coordinate (x,y) using an Integer
	 * @param x x-coordinate ranging from 0 to (sizeX - 1)
	 * @param y y-coordinate ranging from 0 to (sizeY - 1)
	 * @param RGB value (three byte-values) stored in an int (like ImageJ)
	 */	
	@Override
	public void putValueInt(int x, int y, int value) {
		this.m_dataR[this.m_currentSliceIdx][y*this.m_sizeX + x] = (byte)((value & 0xff0000) >> 16);
		this.m_dataG[this.m_currentSliceIdx][y*this.m_sizeX + x] = (byte)((value & 0x00ff00) >> 8);
		this.m_dataB[this.m_currentSliceIdx][y*this.m_sizeX + x] = (byte)(value & 0x0000ff);
	}
	
	/**
	 * Get RGB value from the specified position.
	 * @param x
	 * @param y
	 * @param z
	 * @param t
	 * @param c
	 * @return 3-element array with red[0], green[1], blue[2] values
	 */
	public int[] getValue(int x, int y, int z, int t, int c) {
		int[] rgb = {(this.m_dataR[t*this.m_sizeC*this.m_sizeZ + z*this.m_sizeC + c][y*this.m_sizeX + x] & 0xff),
					 (this.m_dataG[t*this.m_sizeC*this.m_sizeZ + z*this.m_sizeC + c][y*this.m_sizeX + x] & 0xff),
					 (this.m_dataB[t*this.m_sizeC*this.m_sizeZ + z*this.m_sizeC + c][y*this.m_sizeX + x] & 0xff)};
		return rgb;
	}

	/**
	 * Get RGB value from the specified position in the actual slice.
	 * @param x
	 * @param y
	 * @return 3-element array with red[0], green[1], blue[2] values
	 */
	public int[] getValue(int x, int y) {
		int[] rgb = {(this.m_dataR[this.m_currentSliceIdx][y*this.m_sizeX + x] & 0xff),
				 	 (this.m_dataG[this.m_currentSliceIdx][y*this.m_sizeX + x] & 0xff),
				 	 (this.m_dataB[this.m_currentSliceIdx][y*this.m_sizeX + x] & 0xff)};
		return rgb;
	}
	
	/**
	 * Put a new RGB value at the specified position. RGB values must be in the range [0, 255]
	 * @param x
	 * @param y
	 * @param z
	 * @param t
	 * @param c
	 * @param red
	 * @param green
	 * @param blue
	 */
	public void putValue(int x, int y, int z, int t, int c, int red, int green, int blue) {
		this.m_dataR[t*this.m_sizeC*this.m_sizeZ + z*this.m_sizeC + c][y*this.m_sizeX + x] = (byte)red;
		this.m_dataG[t*this.m_sizeC*this.m_sizeZ + z*this.m_sizeC + c][y*this.m_sizeX + x] = (byte)green;
		this.m_dataB[t*this.m_sizeC*this.m_sizeZ + z*this.m_sizeC + c][y*this.m_sizeX + x] = (byte)blue;
	}
	
	/**
	 * Put a new RGB value at the specified position in the actual slice. RGB values must be in the range [0, 255]
	 * @param x
	 * @param y
	 * @param red
	 * @param green
	 * @param blue
	 */
	public void putValue(int x, int y, int red, int green, int blue) {
		this.m_dataR[this.m_currentSliceIdx][y*this.m_sizeX + x] = (byte)red;
		this.m_dataG[this.m_currentSliceIdx][y*this.m_sizeX + x] = (byte)green;
		this.m_dataB[this.m_currentSliceIdx][y*this.m_sizeX + x] = (byte)blue;	
	}
	
	/**
	 * Get red value from the specified position.
	 * @param x
	 * @param y
	 * @param z
	 * @param t
	 * @param c
	 * @return red value
	 */
	public int getValueR(int x, int y, int z, int t, int c) {
		return (this.m_dataR[t*this.m_sizeC*this.m_sizeZ + z*this.m_sizeC + c][y*this.m_sizeX + x] & 0xff);
	}

	/**
	 * Get red value from the specified position in the actual slice.
	 * @param x
	 * @param y
	 * @return red value
	 */
	public int getValueR(int x, int y) {
		return (this.m_dataR[this.m_currentSliceIdx][y*this.m_sizeX + x] & 0xff);
	}
	
	/**
	 * Get green value from the specified position.
	 * @param x
	 * @param y
	 * @param z
	 * @param t
	 * @param c
	 * @return green value
	 */
	public int getValueG(int x, int y, int z, int t, int c) {
		return (this.m_dataG[t*this.m_sizeC*this.m_sizeZ + z*this.m_sizeC + c][y*this.m_sizeX + x] & 0xff);
	}

	/**
	 * Get green value from the specified position in the actual slice.
	 * @param x
	 * @param y
	 * @return green value
	 */
	public int getValueG(int x, int y) {
		return (this.m_dataG[this.m_currentSliceIdx][y*this.m_sizeX + x] & 0xff);
	}
	
	/**
	 * Get blue value from the specified position.
	 * @param x
	 * @param y
	 * @param z
	 * @param t
	 * @param c
	 * @return blue value
	 */
	public int getValueB(int x, int y, int z, int t, int c) {
		return (this.m_dataB[t*this.m_sizeC*this.m_sizeZ + z*this.m_sizeC + c][y*this.m_sizeX + x] & 0xff);
	}

	/**
	 * Get blue value from the specified position in the actual slice.
	 * @param x
	 * @param y
	 * @return blue value
	 */
	public int getValueB(int x, int y) {
		return (this.m_dataB[this.m_currentSliceIdx][y*this.m_sizeX + x] & 0xff);
	}
	
	/**
	 * Put a new red value at the specified position. Red values must be in the range [0, 255]
	 * @param x
	 * @param y
	 * @param z
	 * @param t
	 * @param c
	 * @param red
	 */
	public void putValueR(int x, int y, int z, int t, int c, int red) {
		this.m_dataR[t*this.m_sizeC*this.m_sizeZ + z*this.m_sizeC + c][y*this.m_sizeX + x] = (byte)red;
	}
	
	/**
	 * Put a new red value at the specified position in the actual slice. Red values must be in the range [0, 255]
	 * @param x
	 * @param y
	 * @param red
	 */
	public void putValueR(int x, int y, int red) {
		this.m_dataR[this.m_currentSliceIdx][y*this.m_sizeX + x] = (byte)red;
	}
	
	/**
	 * Put a new green value at the specified position. Green values must be in the range [0, 255]
	 * @param x
	 * @param y
	 * @param z
	 * @param t
	 * @param c
	 * @param green
	 */
	public void putValueG(int x, int y, int z, int t, int c, int green) {
		this.m_dataG[t*this.m_sizeC*this.m_sizeZ + z*this.m_sizeC + c][y*this.m_sizeX + x] = (byte)green;
	}
	
	/**
	 * Put a new green value at the specified position in the actual slice. Green values must be in the range [0, 255]
	 * @param x
	 * @param y
	 * @param green
	 */
	public void putValueG(int x, int y, int green) {
		this.m_dataG[this.m_currentSliceIdx][y*this.m_sizeX + x] = (byte)green;
	}
	
	/**
	 * Put a new blue value at the specified position. Blue values must be in the range [0, 255]
	 * @param x
	 * @param y
	 * @param z
	 * @param t
	 * @param c
	 * @param blue
	 */
	public void putValueB(int x, int y, int z, int t, int c, int blue) {
		this.m_dataB[t*this.m_sizeC*this.m_sizeZ + z*this.m_sizeC + c][y*this.m_sizeX + x] = (byte)blue;
	}
	
	/**
	 * Put a new blue value at the specified position in the actual slice. Blue values must be in the range [0, 255]
	 * @param x
	 * @param y
	 * @param blue
	 */
	public void putValueB(int x, int y, int blue) {
		this.m_dataB[this.m_currentSliceIdx][y*this.m_sizeX + x] = (byte)blue;
	}
	
	/**
	 * Get a reference to the red channel image.
	 * @return MTBImage of type MTB_BYTE
	 */
	public MTBImage getChannelR() {
		return this.m_imgR;
	}
	
	/**
	 * Get a reference to the green channel image.
	 * @return MTBImage of type MTB_BYTE
	 */
	public MTBImage getChannelG() {
		return this.m_imgG;
	}
	
	/**
	 * Get a reference to the blue channel image.
	 * @return MTBImage of type MTB_BYTE
	 */
	public MTBImage getChannelB() {
		return this.m_imgB;
	}

  /* (non-Javadoc)
   * @see de.unihalle.informatik.MiToBo.datatypes.images.MTBImage#fillWhite()
   */
  @Override
  public void fillWhite() {
  	for (int c = 0; c < this.getSizeC(); ++c)
  		for (int t = 0; t < this.getSizeT(); ++t)
  			for (int z = 0; z < this.getSizeZ(); ++z)
  				for (int y = 0; y < this.getSizeY(); ++y)
  					for (int x = 0; x < this.getSizeX(); ++x) {
  						this.putValueR(x, y, z, t, c, 255);
  						this.putValueG(x, y, z, t, c, 255);
  						this.putValueB(x, y, z, t, c, 255);
  					}
  }

}
