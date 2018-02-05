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
import ij.WindowManager;
import ij.gui.ImageWindow;
import ij.gui.StackWindow;
import ij.io.FileInfo;
import ij.measure.Calibration;
import ij.plugin.Zoom;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.geom.Point2D;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.Parameter.Direction;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException.OperatorExceptionType;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.Alida.operator.ALDData;
import de.unihalle.informatik.Alida.operator.ALDOperator.HidingMode;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBLineSegment2D;
import de.unihalle.informatik.MiToBo.core.exceptions.MTBImageException;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;

/**
 * Abstract wrapper class for easy access to hyperstacks. 
 * <p>
 * Indices range is different from ImageJ. Here, indices in each dimension 
 * range from 0 to (dimSize - 1), while ImageJ stack indices range from 
 * 1 to dimSize. Subclasses implement the wrapper for different types.
 * 
 * @author gress
 */
public abstract class MTBImage extends ALDData 
		implements MTBImageManipulator, KeyListener {

  /** Title string for new and untitled images */
  public static final String UNTITLED = "Untitled";
	
  // ----- member fields

  /** the underlying ImagePlus object */
  protected ImagePlus m_img;

  /** reference to the ImageStack object of the underlying ImagePlus object */
  protected ImageStack m_imgStack;

  /** size of x-dimension */
  protected int m_sizeX;

  /** size of y-dimension */
  protected int m_sizeY;

  /** size of z-dimension */
  protected int m_sizeZ;

  /** size of t-dimension */
  protected int m_sizeT;

  /** size of c-dimension */
  protected int m_sizeC;

  /** stacksize */
  protected int m_sizeStack;

  /** MiToBo image type */
  protected MTBImageType m_type;

  /**
   * Index of the current slice.
   * <p>
   * This has nothing to do with the ij.ImageStack's current slice.
   */
  protected int m_currentSliceIdx;

  /** current t-coordinate, for 3D only access functions */
  protected int m_currentT;

  /** current c-coordinate, for 3D only access functions */
  protected int m_currentC;

  /**
   * Image title
   * <p>
   * This member is private to enforce access via the corresponding 
   * setter/getter only. They take care of properly synchronizing 
   * the image title with the ImageJ GUI as it might have been changed
   * by the user without MiToBo noticing that. 
   */
  private String m_title;

  /** physical voxel size (stepsize) in x-dimension */
//  protected double m_stepX;

  /** physical voxel size (stepsize) in y-dimension */
// protected double m_stepY;

  /** physical voxel size (stepsize) in x-dimension */
//  protected double m_stepZ;

  /** stepsize in t-dimension */
//  protected double m_stepT;

  /** spatial x-unit */
//  protected String m_unitX;

  /** spatial y-unit */
//  protected String m_unitY;

  /** spatial z-unit */
//  protected String m_unitZ;

  /** time unit */
//  protected String m_unitT;
 
  /** calibration object (from ImageJ) */
  protected Calibration calibration;
  
  /** File description XML string if available */
  protected String xml;
  
  /** MiToBo image types */
  public enum MTBImageType {
	  MTB_BYTE, MTB_SHORT, MTB_INT, MTB_FLOAT, MTB_DOUBLE, MTB_RGB
  }
  
  /** methods to create new image from an existing one. used in MTBImageFactory */
  protected enum FactoryMethod {
	  DUPLICATE, CONVERT, SLICE, IMAGE_PART
  }


  // ----- Constructors

  /**
   * Constructor. Initializes the sizes corresponding to the underlying
   * ImagePlus. Current slice index, current z-stack coordinates are all
   * initialized to 0. If a Calibration object is present in the ImagePlus,
   * physical pixel sizes and units are assigned.
   * 
   * @param img		Wrapped ImagePlus object.
   */
  protected MTBImage(ImagePlus img) {

    // reference to ImagePlus object
    this.m_img = img;
    this.m_imgStack = this.m_img.getStack();

    // dimension sizes
    this.m_sizeX = this.m_img.getWidth();
    this.m_sizeY = this.m_img.getHeight();
    this.m_sizeZ = this.m_img.getNSlices();
    this.m_sizeT = this.m_img.getNFrames();
    this.m_sizeC = this.m_img.getNChannels();
    
    this.setProperty("SizeX", new Integer(this.m_sizeX));
    this.setProperty("SizeY", new Integer(this.m_sizeY));
    this.setProperty("SizeZ", new Integer(this.m_sizeZ));
    this.setProperty("SizeT", new Integer(this.m_sizeT));
    this.setProperty("SizeC", new Integer(this.m_sizeC));
    

    this.m_sizeStack = this.m_img.getStackSize();

    this.m_currentSliceIdx = 0;

    this.m_currentT = 0;
    this.m_currentC = 0;

   // m_type = -1;

    this.m_title = img.getTitle().equals("") ? MTBImage.UNTITLED : img.getTitle();

    FileInfo fi = img.getFileInfo();
    this.xml = fi == null ? null : fi.description == null ? null :
		fi.description.indexOf("xml") == -1 ? null : fi.description;
    
    
    if (img.getCalibration() != null) {
    	this.calibration = img.getCalibration();
    	
    }
    else {
    	this.calibration = new Calibration();
        this.setStepsizeX(1.0);
        this.setStepsizeY(1.0);
        this.setStepsizeZ(1.0);
        this.setStepsizeT(1.0);

        this.setUnitX("pixel");
        this.setUnitY("pixel");
        this.setUnitZ("pixel");
        this.setUnitT("sec");
        
      //  this.calibration.disableDensityCalibration();
    }
    
//    Calibration cal = img.getCalibration();
//    if (cal != null) {
//      this.setStepsizeX(cal.pixelWidth);
//      this.setStepsizeY(cal.pixelHeight);
//      this.setStepsizeZ(cal.pixelDepth);
//      this.setStepsizeT(cal.frameInterval);
//
//      this.setUnitX(cal.getXUnit());
//      this.setUnitY(cal.getYUnit());
//      this.setUnitZ(cal.getZUnit());
//      this.setUnitT(cal.getTimeUnit());
//    } else {
//      this.setStepsizeX(1.0);
//      this.setStepsizeY(1.0);
//      this.setStepsizeZ(1.0);
//      this.setStepsizeT(1.0);
//
//      this.setUnitX("");
//      this.setUnitY("");
//      this.setUnitZ("");
//      this.setUnitT("");
//    }
    
    if (this.m_img.getWindow() != null)
    	if (this.m_img.getWindow().getKeyListeners().length == 0)
    		this.m_img.getWindow().addKeyListener(this);
  }

  /**
   * Constructor for initializing an empty image (dimension sizes = -1). Current
   * slice index, current z-stack coordinates are all initialized to 0. Needed
   * for non-ImageJ types, which cannot be represented by an ImagePlus (e.g.
   * Double)
   */
  protected MTBImage() {

    // reference to ImagePlus object
	  this.m_img = null;
    this.m_imgStack = null;

    // dimension sizes
    this.m_sizeX = -1;
    this.m_sizeY = -1;
    this.m_sizeZ = -1;
    this.m_sizeT = -1;
    this.m_sizeC = -1;

    this.m_sizeStack = -1;

    this.m_currentSliceIdx = 0;

    this.m_currentT = 0;
    this.m_currentC = 0;

   // m_type = -1;

    this.m_title = MTBImage.UNTITLED;
    
    this.xml = null;
    
    this.calibration = new Calibration();
    
    this.setStepsizeX(1.0);
    this.setStepsizeY(1.0);
    this.setStepsizeZ(1.0);
    this.setStepsizeT(1.0);

    this.setUnitX("pixel");
    this.setUnitY("pixel");
    this.setUnitZ("pixel");
    this.setUnitT("sec");
    
 //   this.calibration.disableDensityCalibration();
  }

  // ----- Data access functions
  /**
   * Get the calibration object. The MTBImage and the underlying ImagePlus (if available)
   * share the same Calibration object. This object stores information about physical pixel size,
   * world coordinates (it can return world coordinate for a specified pixel coordinate) etc.
   */
  public Calibration getCalibration() {
	  return this.calibration;
  }
  
  /**
   * Set the calibration object (see getCalibration() for further information).
   * @param _calibration	Calibration for image.
   */
  public void setCalibration(Calibration _calibration) {
	  this.calibration = _calibration;
	  
	  this.setProperty("StepsizeX", new Double(this.calibration.pixelWidth));
	  this.setProperty("StepsizeY", new Double(this.calibration.pixelHeight));
	  this.setProperty("StepsizeZ", new Double(this.calibration.pixelDepth));
	  this.setProperty("StepsizeT", new Double(this.calibration.frameInterval));

	  this.setProperty("UnitX", this.calibration.getXUnit());
	  this.setProperty("UnitY", this.calibration.getYUnit());
	  this.setProperty("UnitZ", this.calibration.getZUnit());
	  this.setProperty("UnitT", this.calibration.getTimeUnit());
	  
	  if (this.m_img != null) {
		  this.m_img.setCalibration(this.calibration);
	  }
  }
  
  /**
   * Get the XML file description string if one was available. The MTBImage and the underlying ImagePlus (if available)
   * share the same XML String. The string is found in the FileInfo object of the ImagePlus by the key "xml".
   * @return String representation of image in XML format.
   */
  public String getXML() {
	  return this.xml;
  }
  
  /**
   * Set the XML file description string if one was available
   * @param _xml
   */
  public void setXML(String _xml) {
	  this.xml = _xml;
	  
  }
  
  /**
   * Checks if the given type identifier is valid.
   * 
   * @param type
   *          Integer specifying a MTBImage type.
   * @return True, if type integer is valid.
   */
  @Deprecated
  public static boolean isValidType(MTBImageType type) {
    return (type == MTBImageType.MTB_BYTE) || (type == MTBImageType.MTB_SHORT) || (type == MTBImageType.MTB_INT)
        || (type == MTBImageType.MTB_FLOAT) || (type == MTBImageType.MTB_DOUBLE) || (type == MTBImageType.MTB_RGB);
  }

  /**
   * Returns type identifier for given id.
   * 
   * @param type
   *          Numerical identifier for image.
   * @return Identifier string, null is non-existent.
   */
  @Deprecated
  public static String getTypeName(int type) {
    switch (type) {
      case 0:
        return "MTB_BYTE";
      case 1:
        return "MTB_SHORT";
      case 3:
        return "MTB_INT";
      case 4:
        return "MTB_FLOAT";
      case 5:
        return "MTB_DOUBLE";
      case 6:
        return "MTB_RGB";
    }
    return null;
  }

  /**
   * Test if this object and img have the same size in x-, y-, z-, t- and
   * c-dimension
   * 
   * @param img
   *          image to compare with
   * @return true if this object and img have the same size, else false
   */
  public boolean equalSize(MTBImage img) {

    return (this.m_sizeX == img.m_sizeX && this.m_sizeY == img.m_sizeY
        && this.m_sizeZ == img.m_sizeZ && this.m_sizeT == img.m_sizeT && this.m_sizeC == img.m_sizeC);
  }

  /**
   * Test if this object and img are of same type, have same size and same
   * values
   * 
   * @param img
   *          image to compare with
   * @return true if same type, size and values
   */
  public boolean equals(MTBImage img) {

    if (this.m_type != img.m_type) {
      return false;
    }

    if (!this.equalSize(img)) {
      return false;
    }

    boolean equal = true;
    int tIdx = this.m_currentSliceIdx;
    int iIdx = img.m_currentSliceIdx;
    for (int i = 0; i < this.m_sizeStack && equal; i++) {
      this.m_currentSliceIdx = i;
      img.m_currentSliceIdx = i;

      for (int y = 0; y < this.m_sizeY && equal; y++) {
        for (int x = 0; x < this.m_sizeX && equal; x++) {
          if (this.getValueDouble(x, y) != img.getValueDouble(x, y)) {
            equal = false;
          }
        }
      }
    }
    this.m_currentSliceIdx = tIdx;
    img.m_currentSliceIdx = iIdx;

    return equal;
  }

  /**
   * Get the physical size of a voxel (stepsize) in x-dimension
   * 
   * @return physical voxel size in x-dimension
   */
  @Override
  public double getStepsizeX() {
    return this.calibration.pixelWidth;
  }

  /**
   * Get the physical size of a voxel (stepsize) in y-dimension
   * 
   * @return physical voxel size in y-dimension
   */
  @Override
  public double getStepsizeY() {
    return this.calibration.pixelHeight;
  }

  /**
   * Get the physical size of a voxel (stepsize) in z-dimension
   * 
   * @return physical voxel size in z-dimension
   */
  @Override
  public double getStepsizeZ() {
    return this.calibration.pixelDepth;
  }

  /**
   * Get the stepsize in t-dimension (timestep)
   * 
   * @return time stepsize
   */
  @Override
  public double getStepsizeT() {
    return this.calibration.frameInterval;
  }

  /**
   * Set the physical size of a voxel (stepsize) in x-dimension
   * 
   * @param stepsize
   */
  public void setStepsizeX(double stepsize) {
    this.calibration.pixelWidth = stepsize;
    this.setProperty("StepsizeX", new Double(stepsize));
  }

  /**
   * Set the physical size of a voxel (stepsize) in y-dimension
   * 
   * @param stepsize
   */
  public void setStepsizeY(double stepsize) {
	this.calibration.pixelHeight = stepsize;
    this.setProperty("StepsizeY", new Double(stepsize));
  }

  /**
   * Set the physical size of a voxel (stepsize) in z-dimension
   * 
   * @param stepsize
   */
  public void setStepsizeZ(double stepsize) {
	this.calibration.pixelDepth = stepsize;
    this.setProperty("StepsizeZ", new Double(stepsize));
  }

  /**
   * Set the stepsize in t-dimension (timestep)
   * 
   * @param stepsize
   */
  public void setStepsizeT(double stepsize) {
    this.calibration.frameInterval = stepsize;
    this.setProperty("StepsizeT", new Double(stepsize));
  }

  /**
   * Get the unit of the x-dimension
   * 
   * @return String of x-dimension's unit
   */
  @Override
  public String getUnitX() {
    return this.calibration.getXUnit();
  }

  /**
   * Get the unit of the y-dimension
   * 
   * @return String of y-dimension's unit
   */
  @Override
  public String getUnitY() {
	return this.calibration.getYUnit();
  }

  /**
   * Get the unit of the z-dimension
   * 
   * @return String of z-dimension's unit
   */
  @Override
  public String getUnitZ() {
	return this.calibration.getZUnit();
  }

  /**
   * Get the unit of the t-dimension
   * 
   * @return String of t-dimension's unit
   */
  @Override
  public String getUnitT() {
    return this.calibration.getTimeUnit();
  }

  /**
   * Set the unit of the x-dimension
   * 
   * @param unit
   *          String of x-dimension unit
   */
  public void setUnitX(String unit) {
    this.calibration.setXUnit(unit);
    this.setProperty("UnitX", unit);
  }

  /**
   * Set the unit of the y-dimension
   * 
   * @param unit
   *          String of y-dimension unit
   */
  public void setUnitY(String unit) {
    this.calibration.setYUnit(unit);
    this.setProperty("UnitY", unit);
  }

  /**
   * Set the unit of the z-dimension
   * 
   * @param unit
   *          String of z-dimension unit
   */
  public void setUnitZ(String unit) {
    this.calibration.setZUnit(unit);
    this.setProperty("UnitZ", unit);
  }

  /**
   * Set the unit of the t-dimension
   * 
   * @param unit
   *          String of t-dimension unit
   */
  public void setUnitT(String unit) {
    this.calibration.setTimeUnit(unit);
    this.setProperty("UnitT", unit);
  }
  
  /**
   * Copy physical properties like stepsizes and units from sourceImg to this. 
   * @param sourceImg
   */
  public void copyPhysicalProperties(MTBImage sourceImg) {
	  this.setStepsizeX(sourceImg.getStepsizeX());
	  this.setStepsizeY(sourceImg.getStepsizeY());
	  this.setStepsizeZ(sourceImg.getStepsizeZ());
	  this.setStepsizeT(sourceImg.getStepsizeT());
	  this.setUnitX(sourceImg.getUnitX());
	  this.setUnitY(sourceImg.getUnitY());
	  this.setUnitZ(sourceImg.getUnitZ());
	  this.setUnitT(sourceImg.getUnitT()); 
  }

  /**
   * Update physical properties (stepsizes, units) of the image (member
   * variables) from its properties hashtable
   */
  protected void updatePhysProperties_PropToImg() {

    this.calibration.pixelWidth = Double.parseDouble(this.getProperty("StepsizeX"));
    this.calibration.pixelHeight = Double.parseDouble(this.getProperty("StepsizeY"));
    this.calibration.pixelDepth = Double.parseDouble(this.getProperty("StepsizeZ"));
    this.calibration.frameInterval = Double.parseDouble(this.getProperty("StepsizeT"));

    this.calibration.setXUnit(this.getProperty("UnitX"));
    this.calibration.setYUnit(this.getProperty("UnitY"));
    this.calibration.setZUnit(this.getProperty("UnitZ"));
    this.calibration.setTimeUnit(this.getProperty("UnitT"));

  }

  /**
   * Update physical properties (stepsizes, units) in the properties hashtable
   * from its member variables
   */
  protected void updatePhysProperties_ImgToProp() {

    this.setProperty("StepsizeX", new Double(this.calibration.pixelWidth));
    this.setProperty("StepsizeY", new Double(this.calibration.pixelHeight));
    this.setProperty("StepsizeZ", new Double(this.calibration.pixelDepth));
    this.setProperty("StepsizeT", new Double(this.calibration.frameInterval));

    this.setProperty("UnitX", this.calibration.getXUnit());
    this.setProperty("UnitY", this.calibration.getYUnit());
    this.setProperty("UnitZ", this.calibration.getZUnit());
    this.setProperty("UnitT", this.calibration.getTimeUnit());
  }

  /**
   * Update the image size in the properties hashtable from the image's size
   */
  protected void updateImageSize_ImgToProp() {
    this.setProperty("SizeX", new Integer(this.m_sizeX));
    this.setProperty("SizeY", new Integer(this.m_sizeY));
    this.setProperty("SizeZ", new Integer(this.m_sizeZ));
    this.setProperty("SizeT", new Integer(this.m_sizeT));
    this.setProperty("SizeC", new Integer(this.m_sizeT));
  }

  /**
   * Get the image's title
   */
  @Override
  public String getTitle() {
  	// update title from GUI (might have been changed by user...)
  	if (this.m_img != null) 
  		this.m_title = this.m_img.getTitle(); 
    return this.m_title;
  }

  /**
   * Set the image's title.
   * 
   * @param title		New title for image.
   * @throws MTBImageException
   */
  public void setTitle(String title) {
  	// ignore null string as title, cannot be set in ImagePlus
  	if (title == null)
  		return;
    this.m_title = title;

    if (this.m_img != null) {
      this.m_img.setTitle(title);
    }
  }

  /**
   * Get size of x-dimension
   * 
   * @return size of x-dimension
   */
  @Override
  public int getSizeX() {
    return this.m_sizeX;
  }

  /**
   * Get size of y-dimension
   * 
   * @return size of y-dimension
   */
  @Override
  public int getSizeY() {
    return this.m_sizeY;
  }

  /**
   * Get size of z-dimension
   * 
   * @return size of z-dimension
   */
  @Override
  public int getSizeZ() {
    return this.m_sizeZ;
  }

  /**
   * Get size of t(ime)-dimension
   * 
   * @return size of t(ime)-dimension
   */
  @Override
  public int getSizeT() {
    return this.m_sizeT;
  }

  /**
   * Get size of c(hannel)-dimension
   * 
   * @return size of c(hannel)-dimension
   */
  @Override
  public int getSizeC() {
    return this.m_sizeC;
  }

  /**
   * Get MiToBo image type
   * 
   * @return image type (MTBImage.MTB_BYTE, MTBImage.MTB_SHORT,
   *         MTBImage.MTB_INT, MTBImage.MTB_FLOAT, MTBImage.MTB_DOUBLE)
   */
  @Override
  public MTBImageType getType() {
    return this.m_type;
  }

  /**
   * Get size of the underlying (ImageJ) stack which corresponds to the number
   * of slices
   * 
   * @return size of c(hannel)-dimension
   */
  public int getSizeStack() {
    return this.m_sizeStack;
  }

  /**
   * Return the maximum value that the current type can handle
   * 
   * @return	Maximal possible value in data type of image.
   */
  public double getTypeMax() {
	  
    if (this.m_type == MTBImageType.MTB_BYTE)
    	return (Byte.MAX_VALUE) * 2.0 + 1.0;
    else if (this.m_type == MTBImageType.MTB_SHORT)
        return (Short.MAX_VALUE) * 2.0 + 1.0;
    else if (this.m_type == MTBImageType.MTB_INT)
        return Integer.MAX_VALUE;
    else if (this.m_type == MTBImageType.MTB_FLOAT)
        return Float.MAX_VALUE;
    else if (this.m_type == MTBImageType.MTB_DOUBLE)
        return Double.MAX_VALUE;
    else if (this.m_type == MTBImageType.MTB_RGB)
        return (Byte.MAX_VALUE) * 2.0 + 1.0;
    else
    	return -1.0;
    
  }

  /**
   * Return the minimum value that the current type can handle
   * 
   * @return Minimal possible value in data type of image.
   */
  public double getTypeMin() {
	  if (this.m_type == MTBImageType.MTB_BYTE)
        return 0.0;
	  else if (this.m_type == MTBImageType.MTB_SHORT)
        return 0.0;
	  else if (this.m_type == MTBImageType.MTB_INT)
        return Integer.MIN_VALUE;
	  else if (this.m_type == MTBImageType.MTB_FLOAT)
        return Float.MIN_VALUE;
	  else if (this.m_type == MTBImageType.MTB_DOUBLE)
        return Double.MIN_VALUE;
	  else if (this.m_type == MTBImageType.MTB_RGB)
        return 0.0;
	  else
        return -1.0;
  }

  /**
   * Get the current slice coordinates (z,t,c) (for functions that work on a
   * slice only, this has nothing to do with the ij.ImageStack's current slice)
   * 
   * @return (z,t,c)-coordinate of the current slice. [0]= z-coordinate, [1]=
   *         t-coordinate, [2]= c-coordinate
   */
  public int[] getCurrentSliceCoords() {
    int[] coords = new int[3];

    coords[0] = (this.m_currentSliceIdx / this.m_sizeC) % this.m_sizeZ;
    coords[1] = this.m_currentSliceIdx / (this.m_sizeC * this.m_sizeZ);
    coords[2] = this.m_currentSliceIdx % this.m_sizeC;

    return coords;
  }

  /**
   * Get the current slice index (for functions that work on a slice only, this
   * has nothing to do with the ij.ImageStack's current slice)
   * 
   * @return (z,t,c)-coordinate of the current slice. [0]= z-coordinate, [1]=
   *         t-coordinate, [2]= c-coordinate
   */
  public int getCurrentSliceIndex() {

    return this.m_currentSliceIdx;
  }

  /**
   * Set the current slice for functions that work on a slice only (this has
   * nothing to do with the ij.ImageStack's current slice)
   */
  public void setCurrentSliceCoords(int z, int t, int c) {
    this.m_currentSliceIdx = t * this.m_sizeC * this.m_sizeZ + z * this.m_sizeC + c;
  }

  /**
   * Set the current slice for functions that work on a slice only (this has
   * nothing to do with the ij.ImageStack's current slice) Stack indices range
   * from 0 to N-1 (unlike ImageJ, where stack indices range from 1 to N)
   * 
   * @param stackindex index of the slice in the underlying (ImageJ) stack
   */
  public void setCurrentSliceIndex(int stackindex) {
    this.m_currentSliceIdx = stackindex;
  }

  /**
   * Get the current z-stack coordinates. [0]=t-coordinate, [1]=c-coordinate.
   * They are independent of the current slice index.
   * 
   * @return array with the current t and c coordinates ([0]= t-coordinate, [1]=
   *         c-coordinate)
   */
  public int[] getCurrentZStackCoords() {
    int[] actTC = { this.m_currentT, this.m_currentC };
    return actTC;
  }

  /**
   * Set the current z-stack coordinates for functions that work on (spatial) 3D
   * only. The coordinates are not checked for their validity. They are
   * independent of the current slice index. E.g. after setting z-stack
   * coordinates to 't' and 'c', the functions getValueInt(x,y,z) is equivalent
   * to getValueInt(x,y,z,'t','c').
   * 
   * @param t
   *          current t-coordinate
   * @param c
   *          current c-coordinate
   */
  public void setCurrentZStackCoordinates(int t, int c) {
    this.m_currentT = t;
    this.m_currentC = c;
  }

  /**
   * Display the image as an ImagePlus. Keep in mind that the MiToBo image types
   * MTB_INT and MTB_DOUBLE have no equivalent ImageJ types. Thus this function
   * creates an ImagePlus object of a reduced precision type (short/float) for
   * display. These ImagePlus objects do NOT share data and thus are uncoupled
   * of the MTBImage. You can find the MTBImage in the ImagePlus' properties
   * with the key "MTBImage"
   */
  public void show() {

    this.updateImagePlus();
    // reset the adjustment
    this.m_img.getProcessor().resetMinAndMax();
    // show the image
    this.m_img.show();

    ImageWindow w = this.m_img.getWindow();
    if (w instanceof StackWindow) {
      int actslice = this.m_img.getCurrentSlice() - 1;
      ((StackWindow) w).setPosition((actslice % this.m_sizeC) + 1,
          (((actslice / this.m_sizeC)) % this.m_sizeZ) + 1,
          (actslice / (this.m_sizeC * this.m_sizeZ)) + 1);
      ((StackWindow) w).updateSliceSelector();
    }
    
 //   KeyListener[] kl = w.getKeyListeners();
 //   boolean containsThis = false;
  //  for (int i = 0; i < kl.length && (containsThis == false); i++) {
 //   	if (kl[i] == this)
 //   		containsThis = true;
 //   }
    
    if (w.getKeyListeners().length == 0)
    	w.addKeyListener(this);
  }

  /**
   * Update and repaint the image window if the image is displayed by ImageJ
   */
  public void updateAndRepaintWindow() {
    this.updateImagePlus();
    this.m_img.updateAndRepaintWindow();
    
//    ImageWindow w = m_img.getWindow();
//    if (w != null) {
//    	System.out.println("# of key listeners: " + w.getKeyListeners().length);
//    	if (w.getKeyListeners().length == 0)
//    		w.addKeyListener(this);
//    }
  }

  /**
   * Close the displayed ImagePlus
   */
  public void close() {
    if (this.m_img != null) {
      this.m_img.close();
    }
  }

		/**
		 * Method to scale the intensity values (range [curMin, curMax]) of the image
		 * to a given range [scaleMin, scaleMax]. In general curMin < curMax, curMin
		 * != curMax and scaleMin < scaleMax is required.
		 * 
		 * @param c
		 *          current c-coordinate of channel dimension
		 * @param t
		 *          current t-coordinate of time dimension
		 * @param curMin
		 *          minimal intensity value of the current image
		 * @param curMax
		 *          maximal intensity value of the current image
		 * @param scaleMin
		 *          new minimal intensity value of the scaled image
		 * @param scaleMax
		 *          new maximal intensity value of the scaled image
		 * @return Image with scaled intensity values in range [scaleMin, scaleMax].
		 * @throws IllegalArgumentException
		 *           on illegal scaling values
		 */
		public MTBImageDouble scaleValues(int c, int t, double curMin, double curMax,
		    double scaleMin, double scaleMax) throws IllegalArgumentException {

				if (t < 0 || t >= this.m_sizeT || c < 0 || c >= this.m_sizeC) {
						throw new IllegalArgumentException("Invalid slice coordinate. (c,t)=("
						    + c + "," + t + ") is outside the image domain.");
				}
				if (curMin > curMax || curMin == curMax) {
						throw new IllegalArgumentException(
						    "MTBImage.scaleValues(..): Current min value must be smaller than current max value. Current min and max values must be different.");
				}
				if (scaleMin > scaleMax) {
						throw new IllegalArgumentException(
						    "MTBImage.scaleValues(..): Min value for new scale must be smaller than max value for new scale.");
				}
				MTBImageDouble scaledImage = (MTBImageDouble) this.convertType(
				    MTBImageType.MTB_DOUBLE, true);
				for (int z = 0; z < this.getSizeZ(); ++z) {
						for (int y = 0; y < this.getSizeY(); ++y) {
								for (int x = 0; x < this.getSizeX(); ++x) {
										double value = 0.0;
										/*
										 * Found value smaller than curMin -> set to scaleMin (minimum of new
										 * range)
										 */
										if ((this.getValueDouble(x, y, z, t, c)) < curMin) {
												value = scaleMin;

										} else
										/*
										 * Found value greater than curMax -> set to scaleMax (maximum of new
										 * range)
										 */
										if ((this.getValueDouble(x, y, z, t, c)) > curMax) {
												value = scaleMax;
										} else {
												// scale value to new range
												value = (this.getValueDouble(x, y, z, t, c) - curMin)
												    * ((scaleMax - scaleMin) / (curMax - curMin)) + scaleMin;
										}
										scaledImage.putValueDouble(x, y, z, t, c, value);
								}
						}
				}
				return scaledImage;
		}
  
		/**
		 * Create an image of given type from this image's values. Information is lost
		 * if a type of less precision is used! This method uses the internal
		 * MTBImageFactory, which extends MTBOperator, thus the new image will share
		 * the history. If you need to specify the calling operator, use
		 * getImagePart(MTBOperator, ...)
		 * 
		 * @param type
		 *          new image's type (see static final fields for types)
		 * @param scaleDown
		 *          If true, the data is scaled down to fit in the range of the new
		 *          image type values, if the new image type is of less precision. If
		 *          false, the values are simply casted.
		 * @return new image of given type or null if the operation was not successful
		 */
  public MTBImage convertType(MTBImageType type, boolean scaleDown) {
	  return this.convertType(null, type, scaleDown);
  }
  
  /**
   * Create an image of given type from this image's values. Information is lost
   * if a type of less precision is used!
   * This method uses the internal MTBImageFactory, which extends MTBOperator,
   * thus the new image will share the history.
   * 
   * @param callingOperator the MTBOperator, which calls this method
   * @param type
   *          new image's type (see static final fields for types)
   * @param scaleDown
   *          If true, the data is scaled down to fit in the range of the new
   *          image type values, if the new image type is of less precision. If
   *          false, the values are simply casted.
   * @return new image of given type or null if the operation was not successful
   */
  public MTBImage convertType(
  		@SuppressWarnings("unused") MTBOperator callingOperator, 
  		MTBImageType type, boolean scaleDown) {

	  MTBImageFactory factory = null;
	  MTBImage newImg = null;
	  
	  try {
		  
		factory = new MTBImageFactory(this, type, scaleDown);
		
		factory.runOp(null);
		
		newImg = factory.getResultImg();
		
	  } catch (ALDOperatorException e) {
		e.printStackTrace();
	  } catch (ALDProcessingDAGException e) {
		e.printStackTrace();
	  }
	  
	  return newImg; 
  }

  
  /**
   * Get a copy of a part of this image as new MTBImage. This method uses the internal MTBImageFactory, which extends MTBOperator,
   * thus the new image will share the history. If you need to specify the calling operator, use getImagePart(MTBOperator, ...)
   * 
   * @param x
   *          x-coordinate where the first value is copied from
   * @param y
   *          y-coordinate where the first value is copied from
   * @param z
   *          z-coordinate where the first value is copied from
   * @param t
   *          t-coordinate where the first value is copied from
   * @param c
   *          c-coordinate where the first value is copied from
   * @param sizeX
   *          size of the copied part in x-dimension
   * @param sizeY
   *          size of the copied part in y-dimension
   * @param sizeZ
   *          size of the copied part in z-dimension
   * @param sizeT
   *          size of the copied part in t-dimension
   * @param sizeC
   *          size of the copied part in c-dimension
   * @return new MTBImage with values equal to the specified part or null if the operation was not successful
   * @throws IllegalArgumentException
   *           if image boundaries are exceeded in any way
   */
  public MTBImage getImagePart(int x, int y, int z, int t, int c, int sizeX,
		  							int sizeY, int sizeZ, int sizeT, int sizeC)
      							throws IllegalArgumentException {

	  return this.getImagePart(null, x, y, z, t, c, sizeX, sizeY, sizeZ, sizeT, sizeC);
  }
  
  
  /**
   * Get a copy of a part of this image as new MTBImage. This method uses the internal MTBImageFactory, which extends MTBOperator,
   * thus the new image will share the history.
   * 
   * @param callingOperator the MTBOperator, which calls this method
   * @param x
   *          x-coordinate where the first value is copied from
   * @param y
   *          y-coordinate where the first value is copied from
   * @param z
   *          z-coordinate where the first value is copied from
   * @param t
   *          t-coordinate where the first value is copied from
   * @param c
   *          c-coordinate where the first value is copied from
   * @param sizeX
   *          size of the copied part in x-dimension
   * @param sizeY
   *          size of the copied part in y-dimension
   * @param sizeZ
   *          size of the copied part in z-dimension
   * @param sizeT
   *          size of the copied part in t-dimension
   * @param sizeC
   *          size of the copied part in c-dimension
   * @return new MTBImage with values equal to the specified part or null if the operation was not successful
   * @throws IllegalArgumentException
   *           if image boundaries are exceeded in any way
   */
  public MTBImage getImagePart(
  		@SuppressWarnings("unused") MTBOperator callingOperator,
  		int x, int y, int z, int t, int c, int sizeX,
		  int sizeY, int sizeZ, int sizeT, int sizeC)
      		throws IllegalArgumentException {

    if (x + sizeX > this.m_sizeX || y + sizeY > this.m_sizeY
        || z + sizeZ > this.m_sizeZ || t + sizeT > this.m_sizeT
        || c + sizeC > this.m_sizeC) {
      throw new IllegalArgumentException(
          "MTBImage.getImagePart(..): Specified image part exceeds the image boundaries.");
    } else if (x < 0 || x >= this.m_sizeX || y < 0 || y >= this.m_sizeY
        || z < 0 || z >= this.m_sizeZ || t < 0 || t >= this.m_sizeT || c < 0
        || c >= this.m_sizeC) {
      throw new IllegalArgumentException(
          "MTBImage.getImagePart(..): Specified coordinate exceeds the image boundaries.");
    } else {

  	  MTBImageFactory factory = null;
	  MTBImage newImg = null;
	  
	  try {
		  
		factory = new MTBImageFactory(this, x, y, z, t, c, sizeX, sizeY, sizeZ, sizeT, sizeC);
		
		factory.runOp(null);
		
		newImg = factory.getResultImg();
		
	  } catch (ALDOperatorException e) {
		e.printStackTrace();
	  } catch (ALDProcessingDAGException e) {
		e.printStackTrace();
	  }

      return newImg;
    }
  }

  /**
   * Set a part of this image to the values of the source
   * 
   * @param src
   *          source MTBImage (or MTBImageWindow)
   * @param x
   *          x-coordinate where the first value is copied to
   * @param y
   *          y-coordinate where the first value is copied to
   * @param z
   *          z-coordinate where the first value is copied to
   * @param t
   *          t-coordinate where the first value is copied to
   * @param c
   *          c-coordinate where the first value is copied to
   * @throws IllegalArgumentException
   *           if image boundaries are exceeded in any way
   */
  public void setImagePart(MTBImageManipulator src, int x, int y, int z, int t,
      int c) throws IllegalArgumentException {
    if (x + src.getSizeX() > this.m_sizeX || y + src.getSizeY() > this.m_sizeY
        || z + src.getSizeZ() > this.m_sizeZ
        || t + src.getSizeT() > this.m_sizeT
        || c + src.getSizeC() > this.m_sizeC) {
      throw new IllegalArgumentException(
          "MTBImage.setImagePart(..): Specified position with the size of source exceed target size");
    } else if (x < 0 || x >= this.m_sizeX || y < 0 || y >= this.m_sizeY
        || z < 0 || z >= this.m_sizeZ || t < 0 || t >= this.m_sizeT || c < 0
        || c >= this.m_sizeC) {
      throw new IllegalArgumentException(
          "MTBImage.setImagePart(..): Specified coordinate exceeds the image boundaries.");
    } else {
      int sx = src.getSizeX();
      int sy = src.getSizeY();
      int sz = src.getSizeZ();
      int st = src.getSizeT();
      int sc = src.getSizeC();

      for (int cc = 0; cc < sc; cc++) {
        for (int tt = 0; tt < st; tt++) {
          for (int zz = 0; zz < sz; zz++) {
            for (int yy = 0; yy < sy; yy++) {
              for (int xx = 0; xx < sx; xx++) {
                this.putValueDouble(x + xx, y + yy, z + zz, t + tt, c + cc, src
                    .getValueDouble(xx, yy, zz, tt, cc));
              }
            }
          }
        }
      }
    }
  }

  /**
   * Get a copy of the specified slice. This method uses the internal MTBImageFactory, which extends MTBOperator,
   * thus the new image will share the history. If you need to specify the calling operator, use getSlice(MTBOperator, ...)
   * 
   * @param z
   * @param t
   * @param c
   * @return copy of specified slice or null if the operation was not successful
   * @throws IllegalArgumentException if z, c or t do not match the image size
   */
  public MTBImage getSlice(int z, int t, int c) throws IllegalArgumentException {
	  return this.getSlice(null, z, t, c);
  }

  /**
   * Get a copy of the specified slice. This method uses the internal MTBImageFactory, which extends MTBOperator,
   * thus the new image will share the history.
   * 
   * @param callingOperator the MTBOperator, which calls this method
   * @param z
   * @param t
   * @param c
   * @return copy of specified slice or null if the operation was not successful
   * @throws IllegalArgumentException if z, c or t do not match the image size
   */
  public MTBImage getSlice(
  		@SuppressWarnings("unused") MTBOperator callingOperator, 
  		int z, int t, int c) throws IllegalArgumentException {
	  
	  if (z < 0 || z >= this.m_sizeZ || t < 0 || t >= this.m_sizeT || c < 0 || c >= this.m_sizeC)
		  throw new IllegalArgumentException("Invalid slice coordinate. (z,t,c)=("+z+","+t+","+c+") is outside the image domain.");
	  
  	  MTBImageFactory factory = null;
	  MTBImage newImg = null;
	  
	  try {
		  
		factory = new MTBImageFactory(this, z, t, c);
		
		factory.runOp(null);
		
		newImg = factory.getResultImg();
		
	  } catch (ALDOperatorException e) {
		e.printStackTrace();
	  } catch (ALDProcessingDAGException e) {
		e.printStackTrace();
	  }

      return newImg;
  }
  
  /**
   * Get a copy of the current slice. This method uses the internal MTBImageFactory, which extends MTBOperator,
   * thus the new image will share the history. If you need to specify the calling operator, use getCurrentSlice(MTBOperator)
   * 
   * @return copy of current slice or null if the operation was not successful
   * @throws IllegalArgumentException
   */
  public MTBImage getCurrentSlice() {
	  int[] sliceCoord = this.getCurrentSliceCoords();	  
	  MTBImage newImg = null;
	  
	  try {
		  
		  newImg = this.getSlice(null, sliceCoord[0], sliceCoord[1], sliceCoord[2]);
		  
	  } catch (IllegalArgumentException e) {
		  e.printStackTrace();
	  }
	  
	  return newImg;
  }
  
  /**
   * Get a copy of the current slice. This method uses the internal MTBImageFactory, which extends MTBOperator,
   * thus the new image will share the history.
   * 
   * @param callingOperator the MTBOperator, which calls this method
   * @return copy of current slice or null if the operation was not successful
   * @throws IllegalArgumentException
   */
  public MTBImage getCurrentSlice(MTBOperator callingOperator) {
	  int[] sliceCoord = this.getCurrentSliceCoords();
	  MTBImage newImg = null;
	  
	  try {
		  
		  newImg = this.getSlice(callingOperator, sliceCoord[0], sliceCoord[1], sliceCoord[2]);
		  
	  } catch (IllegalArgumentException e) {
		  e.printStackTrace();
	  }
	  
	  return newImg;
  }
  
  /**
   * Copy the value of source to the specified slice
   * 
   * @param src
   * @param z
   * @param t
   * @param c
   * @throws IllegalArgumentException
   */
  public void setSlice(MTBImageManipulator src, int z, int t, int c)
      throws IllegalArgumentException {
    try {
      this.setImagePart(src, 0, 0, z, t, c);
    } catch (IllegalArgumentException e) {
      throw e;
    }
  }

  /**
   * Copy the values of the source to the current slice
   * 
   * @param src
   * @throws IllegalArgumentException
   */
  public void setCurrentSlice(MTBImageManipulator src)
      throws IllegalArgumentException {
    try {
      int[] sliceCoord = this.getCurrentSliceCoords();
      this.setImagePart(src, 0, 0, sliceCoord[0], sliceCoord[1], sliceCoord[2]);
    } catch (IllegalArgumentException e) {
      throw e;
    }
  }

  /**
   * Returns the String "IMG("title of the image")"
   */
  @Override
  public String toString() {
    return this.getTitle();
  }

  /**
   * Draws a 2D line into the current slice of the image.
   * <p>
   * MTBImages are 5D, but here t- and c-dimensions are ignored. 
   * <p>
   * This function basically relies on the Bresenham algorithm for rendering
   * line segments as implemented in method
   * {@link MTBLineSegment2D#getPixelsAlongSegment()} of class
   * {@link MTBLineSegment2D}.
   * 
   * @param xstart	x-coordinate of start point.
   * @param ystart	y-coordinate of start point.
   * @param xend		x-coordinate of end point.
   * @param yend		y-coordinate of end point.
   * @param value		Color or gray-scale value to use for drawing the segment.
   */
  public void drawLine2D(int xstart, int ystart, int xend, int yend, 
  		int value) {

  	MTBLineSegment2D line = new MTBLineSegment2D(xstart, ystart, xend, yend);
  	
  	LinkedList<Point2D.Double> pixelList = line.getPixelsAlongSegment();

  	int x, y;
  	for (Point2D.Double p: pixelList) {
  		x = (int)p.x;
  		y = (int)p.y;
  		
  		// check for pixel not falling outside of image domain
      if (x >= 0 && x < this.getSizeX() && y >= 0 && y < this.getSizeY())
        this.putValueInt(x, y, value);
  	}  	
  }
  
  /**
   * Draws a point at given position into the x-y-plane.
   * <p>
   * MTBImages are 5D, but here t- and c-dimensions are ignored.
   * 
   * @param x	x-coordinate of point.
   * @param y y-coordinate of point.
   * @param z z-coordinate of point.
   * @param value Color/gray-scale value to be used.
   * @param mode	Shape of the point (0 = one pixel, 1 = X, 2 = cross)
   */
  public void drawPoint2D(int x, int y, int z, int value, int mode) {

  	// check if position is inside image
  	if (   x<0 || x>=this.getSizeX() 
  			|| y<0 || y>=this.getSizeY() 
  			|| z<0 || z>=this.getSizeZ())
  		return;
  	
  	// implement different modes
  	switch (mode) 
  	{
  	case 1: // draw an X
      this.putValueInt(x,   y, value);
      if (x > 0) {
      	if (y > 0)
      		this.putValueInt(x-1, y-1, value);
      	if (y < this.getSizeY()-1)
      		this.putValueInt(x-1, y+1, value);
      }
      if (x < this.getSizeX()-1) {
      	if (y > 0)
      		this.putValueInt(x+1, y-1, value);
      	if (y < this.getSizeY()-1)
      		this.putValueInt(x+1, y+1, value);
      }
  		break;
  	case 2: // draw a cross 
  		this.putValueInt(x,   y, value);
  		if (x > 0)
  			this.putValueInt(x-1, y, value);
  		if (y > 0)
  			this.putValueInt(x  , y-1, value);
  		if (y < this.getSizeY()-1)
  			this.putValueInt(x  , y+1, value);
  		if (x < this.getSizeX()-1)
  			this.putValueInt(x+1, y, value);
  		break;
  	default:	
  	case 0: // simply mark the position
      this.putValueInt(x, y, value);
      break;
  	}
  }

  /**
   * Draws a circle at the given position into the x-y-plane.
   * <p>
   * MTBImages are 5D, but here t- and c-dimensions are ignored.
   * @param x				Center of circle in x.
   * @param y				Center of circle in y.
   * @param z 			Circle position in z.
   * @param radius 	Radius of circle.
   * @param color 	Color of circle
   */
  public void drawCircle2D(int x, int y, int z, int radius, int color) {
  	// calculate angular sampling
  	double deltaAlpha = Math.asin(0.5/radius);
  	// draw circle
  	double cx, cy;
  	for (double alpha=0; alpha<=2*Math.PI; alpha+=deltaAlpha) {
  		cx = x + Math.cos(alpha) * radius;
  		cy = y + Math.sin(alpha) * radius;
  		int icx = (int)(cx+0.5);
  		int icy = (int)(cy+0.5);
  		if (icx >= 0 && icx < this.m_sizeX && icy >= 0 && icy < this.m_sizeY)
  			this.putValueInt(icx, icy, z, 0, 0, color);
  	}
  }
  
  /**
   * Draws a 2D circle at the given z position into the x-y-plane.
   * <p>
   * MTBImages are 5D, but here t- and c-dimensions are ignored.
   * @param x				Center of circle in x.
   * @param y				Center of circle in y.
   * @param z 			Circle position in z.
   * @param radius 	Radius of circle.
   * @param color 	Color of circle
   */
  public void drawFilledCircle2D(int x, int y, int z, int radius, int color) {
  	int icx, icy;
  	int squRadius = radius*radius;
  	for (int dy=0; dy<=radius; ++dy) {
    	for (int dx=0; dx<=radius; ++dx) {
    		if ((dx*dx) + (dy*dy) <= squRadius) {
    			// bottom-right
    			icx = x+dx;
    			icy = y+dy;
      		if (icx < this.m_sizeX && icy < this.m_sizeY)
      			this.putValueInt(icx, icy, z, 0, 0, color);
      		// top-left
    			icx = x-dx;
    			icy = y-dy;
      		if (icx >= 0 && icy >= 0)
      			this.putValueInt(icx, icy, z, 0, 0, color);
      		// bottom-left
    			icx = x-dx;
    			icy = y+dy;
      		if (icx >= 0 && icy < this.m_sizeY)
      			this.putValueInt(icx, icy, z, 0, 0, color);
      		// top-right
    			icx = x+dx;
    			icy = y-dy;
      		if (icx < this.m_sizeX && icy >= 0)
      			this.putValueInt(icx, icy, z, 0, 0, color);
    		}
    	}
  	}
  }

  /**
   * Get the slice label of the slice specified by the current slice index
   * @return	Label of current slice.
   */
  public String getCurrentSliceLabel() {
	  if (this.m_img != null) {
		  return this.m_img.getStack().getSliceLabel(this.m_currentSliceIdx + 1);
	  }
		return null;
  }
  
  /**
   * Get the slice label of the slice specified by (z,t,c)
   * @param z		z-coordinate of desired label.
   * @param t		t-coordinate of desired label.
   * @param c		c-coordinate of desired label.
   * @return	Label of requested slice.
   */
  public String getSliceLabel(int z, int t, int c) {
	  int tmpIdx = this.m_currentSliceIdx;
	  this.setCurrentSliceCoords(z, t, c);
	  
	  String s = this.getCurrentSliceLabel();
	  this.m_currentSliceIdx = tmpIdx;
	  
	  return s;
  }
  
  /**
   * Set the slice label of the slice specified by the current slice index
   * @param label
   */
  public void setCurrentSliceLabel(String label) {
	  if (this.m_img != null) {
		  this.m_img.getStack().setSliceLabel(label, this.m_currentSliceIdx + 1);
	  } 
  }
  
  /**
   * Set the slice label of the slice specified by (z,t,c)
   * @param label
   * @param z
   * @param t
   * @param c
   */
  public void setSliceLabel(String label, int z, int t, int c) {
	  int tmpIdx = this.m_currentSliceIdx;
	  this.setCurrentSliceCoords(z, t, c);	  
	  this.setCurrentSliceLabel(label);
	  this.m_currentSliceIdx = tmpIdx;
  }

  /**
   * Copy slice labels from src to this
   * @param src
   */
  public void adoptSliceLabels(MTBImage src) throws IllegalArgumentException {
	  
	  if (this.m_sizeStack != src.m_sizeStack) {
		  throw new IllegalArgumentException("Images must have the same number of slices.");
	  }
		int tmpIdx = this.m_currentSliceIdx;
		int tmpSrcIdx = src.m_currentSliceIdx;
		
		for (int i = 0; i < this.m_sizeStack; i++) {
		  this.m_currentSliceIdx = i;
		  src.m_currentSliceIdx = i;
		  this.setCurrentSliceLabel(src.getCurrentSliceLabel());
		}
		this.m_currentSliceIdx = tmpIdx;
		src.m_currentSliceIdx = tmpSrcIdx;
  }
  
  public boolean hasImagePlus() {
	  return (this.m_img != null);
  }
  
  /**
   * Get an ImagePlus object. For ImageJ-types (MTB_BYTE, MTB_SHORT, MTB_FLOAT)
   * with underlying ImagePlus object, a reference to this ImagePlus is
   * returned. For Non-ImageJ-types (MTB_INT, MTB_DOUBLE) an ImagePlus object is
   * created for displaying but with definite LOSS OF INFORMATION. The ImagePlus
   * object's properties contain a reference to the MTBImage under the key
   * "MTBImage", which can be retrieved by the following code:
   * 
   * {@code (MTBImage)(ImagePlus.getProperty("MTBImage")) };
   * 
   * @return ImagePlus object
   */
  abstract public ImagePlus getImagePlus();

  /**
   * Should be used to create or update ImagePlus data if the MTBImage is not
   * using an ImagePlus to store the data (MTB_INT, MTB_DOUBLE, MTB_RGB). For
   * the other types, changes are made to the underlying ImagePlus directly,
   * thus this method should do nothing.
   */
  abstract protected void updateImagePlus();

  /**
   * Get the value of the 5D image at coordinate (x,y,z,t,c) as an Integer
   * 
   * @param x
   *          x-coordinate ranging from 0 to (sizeX - 1)
   * @param y
   *          y-coordinate ranging from 0 to (sizeY - 1)
   * @param z
   *          z-coordinate ranging from 0 to (sizeZ - 1)
   * @param t
   *          t-coordinate ranging from 0 to (sizeT - 1)
   * @param c
   *          c-coordinate ranging from 0 to (sizeC - 1)
   * @return voxel value
   */
  @Override
  abstract public int getValueInt(int x, int y, int z, int t, int c);

  /**
   * Get the value of the 5D image at coordinate (x,y,z,t,c) as a Double
   * 
   * @param x
   *          x-coordinate ranging from 0 to (sizeX - 1)
   * @param y
   *          y-coordinate ranging from 0 to (sizeY - 1)
   * @param z
   *          z-coordinate ranging from 0 to (sizeZ - 1)
   * @param t
   *          t-coordinate ranging from 0 to (sizeT - 1)
   * @param c
   *          c-coordinate ranging from 0 to (sizeC - 1)
   * @return voxel value
   */
  @Override
  abstract public double getValueDouble(int x, int y, int z, int t, int c);

  /**
   * Set the value of the 5D image at coordinate (x,y,z,t,c) using an Integer
   * 
   * @param x
   *          x-coordinate ranging from 0 to (sizeX - 1)
   * @param y
   *          y-coordinate ranging from 0 to (sizeY - 1)
   * @param z
   *          z-coordinate ranging from 0 to (sizeZ - 1)
   * @param t
   *          t-coordinate ranging from 0 to (sizeT - 1)
   * @param c
   *          c-coordinate ranging from 0 to (sizeC - 1)
   * @param value
   *          to set the voxel to
   */
  @Override
  abstract public void putValueInt(int x, int y, int z, int t, int c, int value);

  /**
   * Set the value of the 5D image at coordinate (x,y,z,t,c) using a Double
   * 
   * @param x
   *          x-coordinate ranging from 0 to (sizeX - 1)
   * @param y
   *          y-coordinate ranging from 0 to (sizeY - 1)
   * @param z
   *          z-coordinate ranging from 0 to (sizeZ - 1)
   * @param t
   *          t-coordinate ranging from 0 to (sizeT - 1)
   * @param c
   *          c-coordinate ranging from 0 to (sizeC - 1)
   * @param value
   *          to set the voxel to
   */
  @Override
  abstract public void putValueDouble(int x, int y, int z, int t, int c,
      double value);

  /**
   * Get the value of the current z-stack coordinate (x,y,z) as an Integer
   * 
   * @param x
   *          x-coordinate ranging from 0 to (sizeX - 1)
   * @param y
   *          y-coordinate ranging from 0 to (sizeY - 1)
   * @param z
   *          z-coordinate ranging from 0 to (sizeZ - 1)
   * @return voxel value
   */
  abstract public int getValueInt(int x, int y, int z);

  /**
   * Get the value of the current slice at coordinate (x,y,z) as an Double
   * 
   * @param x
   *          x-coordinate ranging from 0 to (sizeX - 1)
   * @param y
   *          y-coordinate ranging from 0 to (sizeY - 1)
   * @param z
   *          z-coordinate ranging from 0 to (sizeZ - 1)
   * @return voxel value
   */
  abstract public double getValueDouble(int x, int y, int z);

  /**
   * Set the value of the current slice at coordinate (x,y,z) using an Integer
   * 
   * @param x
   *          x-coordinate ranging from 0 to (sizeX - 1)
   * @param y
   *          y-coordinate ranging from 0 to (sizeY - 1)
   * @param z
   *          z-coordinate ranging from 0 to (sizeZ - 1)
   * @param value
   *          to set the voxel to
   */
  abstract public void putValueInt(int x, int y, int z, int value);

  /**
   * Set the value of the current slice at coordinate (x,y,z) using a Double
   * 
   * @param x
   *          x-coordinate ranging from 0 to (sizeX - 1)
   * @param y
   *          y-coordinate ranging from 0 to (sizeY - 1)
   * @param z
   *          z-coordinate ranging from 0 to (sizeZ - 1)
   * @param value
   *          to set the voxel to
   */
  abstract public void putValueDouble(int x, int y, int z, double value);

  /**
   * Get the value of the current slice at coordinate (x,y) as an Integer
   * 
   * @param x
   *          x-coordinate ranging from 0 to (sizeX - 1)
   * @param y
   *          y-coordinate ranging from 0 to (sizeY - 1)
   * @return voxel value
   */
  abstract public int getValueInt(int x, int y);

  /**
   * Get the value of the current slice at coordinate (x,y) as an Double
   * 
   * @param x
   *          x-coordinate ranging from 0 to (sizeX - 1)
   * @param y
   *          y-coordinate ranging from 0 to (sizeY - 1)
   * @return voxel value
   */
  abstract public double getValueDouble(int x, int y);

  /**
   * Set all pixels of the image to white, i.e. the maximal available value.
   */
  public void fillWhite() {
    for (int c = 0; c < this.getSizeC(); ++c)
      for (int t = 0; t < this.getSizeT(); ++t)
        for (int z = 0; z < this.getSizeZ(); ++z)
          for (int y = 0; y < this.getSizeY(); ++y)
            for (int x = 0; x < this.getSizeX(); ++x)
              this.putValueDouble(x, y, z, t, c, this.getTypeMax());
  }

  /**
   * Set all pixels of the image to black, i.e. the minimal available value.
   */
  public void fillBlack() {
    for (int c = 0; c < this.getSizeC(); ++c)
      for (int t = 0; t < this.getSizeT(); ++t)
        for (int z = 0; z < this.getSizeZ(); ++z)
          for (int y = 0; y < this.getSizeY(); ++y)
            for (int x = 0; x < this.getSizeX(); ++x)
              this.putValueDouble(x, y, z, t, c, this.getTypeMin());
  }

  /**
   * Set the value of the current slice at coordinate (x,y) using an Integer
   * 
   * @param x
   *          x-coordinate ranging from 0 to (sizeX - 1)
   * @param y
   *          y-coordinate ranging from 0 to (sizeY - 1)
   * @param value
   *          to set the voxel to
   */
  abstract public void putValueInt(int x, int y, int value);

  /**
   * Set the value of the current slice at coordinate (x,y) using a Double
   * 
   * @param x
   *          x-coordinate ranging from 0 to (sizeX - 1)
   * @param y
   *          y-coordinate ranging from 0 to (sizeY - 1)
   * @param value
   *          to set the voxel to
   */
  abstract public void putValueDouble(int x, int y, double value);

  /**
   * Get minimum and maximum value of the image as int
   * 
   * @return min at int[0], max at int[1]
   */
  abstract public int[] getMinMaxInt();

  /**
   * Get minimum and maximum value of the image as double
   * 
   * @return min at double[0], max at double[1]
   */
  abstract public double[] getMinMaxDouble();

  /**
   * Duplicates the object. 
   * <p>
   * That means also a copy of the underlying ImagePlus
   * (or other data structure) is created. The method uses the internal 
   * MTBImageFactory, which extends MTBOperator, thus the duplicated image 
   * will keep its history. The reference to the calling operator is set 
   * to null, use duplicate(MTBOperator)
   * if you need to specify the calling operator.
   * 
   * @return MTBImage of the same type
   */
  public MTBImage duplicate() {
  	MTBOperator op = null;
	  return this.duplicate(op);
  }
  
  /**
   * Duplicates the object.
   * <p>
   * The parameter allows to configure how the internal operator call should
   * be handled in the history, i.e., to directly hand over a hiding mode 
   * to the underlying operator.
   * 
   * @param h Hiding mode to be used for history construction.
   * 
   * @return {@link MTBImage} of the same type.
   */
  public MTBImage duplicate(HidingMode h) {
  	MTBImageFactory factory = null;
  	MTBImage newImg = null;

  	try {

  		factory = new MTBImageFactory(this);
  		factory.runOp(h);
  		newImg = factory.getResultImg();

  	} catch (ALDOperatorException e) {
  		e.printStackTrace();
  	} catch (ALDProcessingDAGException e) {
  		e.printStackTrace();
  	}

  	return newImg; 
  }

  /**
   * Duplicates the object. That means also a copy of the underlying ImagePlus
   * (or other data structure) is created. The method uses the internal MTBImageFactory, which extends MTBOperator, 
   * thus the duplicated image will keep its history.
   * 
   * @param callingOperator the MTBOperator, which calls this method
   * @return MTBImage of the same type
   */
  @Deprecated
  public MTBImage duplicate(
  		@SuppressWarnings("unused") MTBOperator callingOperator) {
	  
	  MTBImageFactory factory = null;
	  MTBImage newImg = null;
	  
	  try {
		  
		factory = new MTBImageFactory(this);
		
		factory.runOp(null);
		
		newImg = factory.getResultImg();
		
	  } catch (ALDOperatorException e) {
		e.printStackTrace();
	  } catch (ALDProcessingDAGException e) {
		e.printStackTrace();
	  }
	  
	  return newImg; 
  }
  
  /**
   * For a given String, return the corresponding MTBImageType. If the String doesn't match any type, null is returned
   * @param s String must be one of "MTB_BYTE", "MTB_SHORT", "MTB_INT", "MTB_FLOAT", "MTB_DOUBLE", "MTB_RGB"
   * @return	Type specified by given string.
   */
  public static MTBImageType stringToType(String s) {
	  
	  MTBImageType type = null;
	  
	  if (s.equals("MTB_BYTE"))
		  type = MTBImageType.MTB_BYTE;
	  else if (s.equals("MTB_SHORT"))
		  type = MTBImageType.MTB_SHORT;
	  else if (s.equals("MTB_INT"))
		  type = MTBImageType.MTB_INT;			  
	  else if (s.equals("MTB_FLOAT"))
		  type = MTBImageType.MTB_FLOAT;
	  else if (s.equals("MTB_DOUBLE"))
		  type = MTBImageType.MTB_DOUBLE;
	  else if (s.equals("MTB_RGB"))
		  type = MTBImageType.MTB_RGB;
	  
	  return type;
  }
  
  /**
   * Return the corresponding MTBImageType for its index in the enum. If this index doesn't exist, null is returned
   * @param enumIdx		Type identifier.
   * @return Type corresponding to given identifier.
   */
  public static MTBImageType ordinalToType(int enumIdx) {
	  
	  MTBImageType type = null;
	  
	  if (enumIdx == 0)
		  type = MTBImageType.MTB_BYTE;
	  else if (enumIdx == 1)
		  type = MTBImageType.MTB_SHORT;
	  else if (enumIdx == 2)
		  type = MTBImageType.MTB_INT;			  
	  else if (enumIdx == 3)
		  type = MTBImageType.MTB_FLOAT;
	  else if (enumIdx == 4)
		  type = MTBImageType.MTB_DOUBLE;
	  else if (enumIdx == 5)
		  type = MTBImageType.MTB_RGB;
	  
	  return type;
  }
  
	@Override
	public void keyPressed(KeyEvent e) {
		char cmd = e.getKeyChar();

	//	Point loc = canvas.getCursorLoc();
	//	int x = canvas.screenX(loc.x);
	//	int y = canvas.screenY(loc.y);
		if (this.m_img != null) {
			if (this.m_img.getWindow() != null) {
			
				WindowManager.setCurrentWindow(this.m_img.getWindow());
				
				if (cmd == '+' || cmd == ']') {
		 			//canvas.zoomIn(x, y);
					//if (canvas.getMagnification()<=1.0) img.getImagePlus().repaintWindow();
					Zoom z = new Zoom();
					z.run("in");
				}
				else if (cmd == '-' || cmd == '/') {
					//canvas.zoomOut(x, y);
					//if (canvas.getMagnification()<1.0) img.getImagePlus().repaintWindow();
					Zoom z = new Zoom();
					z.run("out");
				}
			}
		}
		
	}


	@Override
	public void keyReleased(KeyEvent e) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void keyTyped(KeyEvent e) {
		// TODO Auto-generated method stub
		
	}
  
  
  /**
   * Factory method to create MTBImage objects. Checks the image type and
   * returns the appropriate subclass object (MTBImageByte, MTBImageShort, ...)
   * If the ImagePlus already contains a MTBImage in its properties, this
   * MTBImage is returned
   * 
   * @param img
   *          ImagePlus object to wrap
   * @return MTBImage subclass object
   * @throws IllegalArgumentException
   *           for unsupported types
   */
  public static MTBImage createMTBImage(ImagePlus img)
      throws IllegalArgumentException {

    if (img.getProperties() != null
        && img.getProperties().containsKey("MTBImage")) {

      return (MTBImage) img.getProperty("MTBImage");
    }
		if (img.getType() == ImagePlus.GRAY8) {
		  return new MTBImageByte(img);
		} else if (img.getType() == ImagePlus.GRAY16) {
		  return new MTBImageShort(img);
		} else if (img.getType() == ImagePlus.GRAY32) {
		  return new MTBImageFloat(img);
		} else if (img.getType() == ImagePlus.COLOR_RGB) {
		  return new MTBImageRGB(img);
		} else {
		  throw new IllegalArgumentException(
		      "MTBImage.createMTBImage(..): Image type " + img.getType()
		          + " not supported.");
		}
  }

  /**
   * Factory method to create MTBImage objects with value 0 at each coordinate.
   * 
   * @param sizeX
   *          size in x-dimension
   * @param sizeY
   *          size in y-dimension
   * @param sizeZ
   *          size in z-dimension
   * @param sizeT
   *          size in t-dimension
   * @param sizeC
   *          size in c-dimension
   * @param type
   *          image type (one of MTB_BYTE, MTB_SHORT, MTB_INT, MTB_FLOAT,
   *          MTB_DOUBLE)
   * @return MTBImage subclass object
   * @throws IllegalArgumentException 
   * 					Thrown in case of invalid arguments.
   */
  public static MTBImage createMTBImage(int sizeX, int sizeY, int sizeZ,
      int sizeT, int sizeC, MTBImageType type) 
      		throws IllegalArgumentException {

  	if (sizeX<=0 || sizeY<=0 || sizeC<=0 || sizeZ<=0 || sizeT<=0)
      throw new IllegalArgumentException(
        "[MTBImage] createMTBImage: one of the dimensions has "
      		+ "a size of zero... please specify sizes larger than zero!");
  	
    if (type == MTBImageType.MTB_BYTE) {
      return new MTBImageByte(sizeX, sizeY, sizeZ, sizeT, sizeC);
    } else if (type == MTBImageType.MTB_SHORT) {
      return new MTBImageShort(sizeX, sizeY, sizeZ, sizeT, sizeC);
    } else if (type == MTBImageType.MTB_INT) {
      return new MTBImageInt(sizeX, sizeY, sizeZ, sizeT, sizeC);
    } else if (type == MTBImageType.MTB_FLOAT) {
      return new MTBImageFloat(sizeX, sizeY, sizeZ, sizeT, sizeC);
    } else if (type == MTBImageType.MTB_DOUBLE) {
      return new MTBImageDouble(sizeX, sizeY, sizeZ, sizeT, sizeC);
    } else if (type == MTBImageType.MTB_RGB) {
      return new MTBImageRGB(sizeX, sizeY, sizeZ, sizeT, sizeC);
    } else {
      throw new IllegalArgumentException(
          "MTBImage.createMTBImage(.., int type): Unknown type: " + type);
    }
  }

  /**
   * Creates an image of type {@link MTBImageType#MTB_BYTE} from
   * the given data array.
   * <p>
   * The array should contain for each xy-slice of the image a 1D-array,
   * and the c,t and z dimensions should multiply to the number of slices given.
   * Note that the dimension of the array must be consistent with the 
   * requested size of the image, if not, an exception occurs.
   * 
   * @param sizeX		Size of new image in x dimension.
   * @param sizeY  Size of new image in y dimension.
   * @param sizeZ  Size of new image in z dimension.
   * @param sizeT  Size of new image in t dimension.
   * @param sizeC  Size of new image in c dimension.
   * @param data   Data array.
   * @return	Generated byte image.
   * @throws IllegalArgumentException
   */
  public static MTBImageByte createMTBImageByte(
  		int sizeX, int sizeY, int sizeZ, int sizeT, int sizeC, byte[][] data) 
  				throws IllegalArgumentException {
    MTBImageByte byteImg = null;
    try {
      byteImg = new MTBImageByte(data, sizeX, sizeY, sizeZ, sizeT, sizeC);
    } catch (IllegalArgumentException e) {
      throw e;
    }
    return byteImg;
  }
  
  
	/**
	 * Return a unique title for the copy of an image, i.e. a running number is appended/incremented.
	 * For example, if "TITLE" is the original title, then "TITLE-1" is the return title. If "TITLE-2" is
	 * entered, then "TITLE-3" is returned. File extensions are considered: "TITLE.tif" results in "TITLE-1.tif".
	 * If ImageJ's WindowManager is available, displayed images are also considered to determine the highest running
	 * number.
	 * @param title of the original image
	 * @return title used for the copy of an image
	 */
	public static String getTitleRunning(String title) {
		
		String ext = "";
		String base = title;
		
		int len = title.length();
        int lastDot = title.lastIndexOf(".");
        if (lastDot!=-1 /*&& len-lastDot<6*/ && lastDot!=len-1) {
            ext = title.substring(lastDot+1, len);
            base = title.substring(0, lastDot);
        }
		
        int d = 0;
        
		Pattern p = Pattern.compile("(.*)-(\\d+)$");
		Matcher m = p.matcher(base);
		if (m.matches()) {
			base = m.group(1);
			d = Integer.parseInt(m.group(2));
		}
        
		d++;
		
		p = Pattern.compile("(.*)-(\\d+)\\.?" + ext + "$");
		String ititle;
		int di;
		for (int i = 0; i < WindowManager.getImageCount(); i++) {
			ititle = WindowManager.getImage(WindowManager.getIDList()[i]).getTitle();
			
			m = p.matcher(ititle);
			if (m.matches()) {
				di = Integer.parseInt(m.group(2));
				if (di >= d)
					d = di+1;
			}
		}
		
		return base + "-" + d + (ext.equals("") ? "" : "."+ext);
	}
  
  /**
   * A class for creating MTBImages which implements the MTBOperator.
   * @author gress
   *
   */
  public class MTBImageFactory extends MTBOperator {
	  
	@Parameter( label= "inImg", required = true, 
			direction = Direction.IN, description = "Input image")
	private MTBImage inImg = null;

	@Parameter( label= "resultImg", required = true, 
			direction = Direction.OUT, description = "Result image")
	private MTBImage resultImg = null;
  
	
	@Parameter( label= "factoryMethod", required = true, 
			direction = Direction.IN, description = "Type of image creation method")
	private FactoryMethod factoryMethod = null;
	

	@Parameter( label= "targetImageType", required = false, 
			direction = Direction.IN, 
      description = "Image type of new image if factory method is CONVERT")
	private MTBImageType targetImageType = null;
	
	@Parameter( label= "scaleDown", required = false, 
			direction = Direction.IN, 
      description = "Scale image values if factory method is CONVERT")
	private boolean scaleDown = false;

	
	@Parameter( label= "x", required = false, direction = Direction.IN, 
			description = "Starting coordinate in x-dimension for copying an image part if factory method is IMAGE_PART")
	private Integer x = new Integer(0);

	@Parameter( label= "y", required = false, direction = Direction.IN, 
			description = "Starting coordinate in y-dimension for copying an image part if factory method is IMAGE_PART")
	private Integer y = new Integer(0);
	
	@Parameter( label= "z", required = false, direction = Direction.IN, 
			description = "Starting coordinate in z-dimension for copying an image part if factory method is IMAGE_PART or slice coordinate if factory method is SLICE")
	private Integer z = new Integer(0);

	@Parameter( label= "t", required = false, direction = Direction.IN, 
			description = "Starting coordinate in t-dimension for copying an image part if factory method is IMAGE_PART or slice coordinate if factory method is SLICE")
	private Integer t = new Integer(0);
	
	@Parameter( label= "c", required = false, direction = Direction.IN, 
			description = "Starting coordinate in c-dimension for copying an image part if factory method is IMAGE_PART or slice coordinate if factory method is SLICE")
	private Integer c = new Integer(0);
	
	
	@Parameter( label= "sizeX", required = false, direction = Direction.IN, 
			description = "Size in x-dimension for copying an image part if factory method is IMAGE_PART")
	private Integer sizeX = new Integer(0);

	@Parameter( label= "sizeY", required = false, direction = Direction.IN, 
			description = "Size in y-dimension for copying an image part if factory method is IMAGE_PART")
	private Integer sizeY = new Integer(0);

	@Parameter( label= "sizeZ", required = false, direction = Direction.IN, 
			description = "Size in z-dimension for copying an image part if factory method is IMAGE_PART")
	private Integer sizeZ = new Integer(0);

	@Parameter( label= "sizeT", required = false, direction = Direction.IN, 
			description = "Size in t-dimension for copying an image part if factory method is IMAGE_PART")
	private Integer sizeT = new Integer(0);
	
	@Parameter( label= "sizeC", required = false, direction = Direction.IN, 
			description = "Size in c-dimension for copying an image part if factory method is IMAGE_PART")
	private Integer sizeC = new Integer(0);

	  
	public MTBImageFactory()  throws ALDOperatorException {
		
	}
	
	/**
	 * Duplicate constructor
	 * @param _inImg
	 * @throws ALDOperatorException 
	 */
  	public MTBImageFactory(MTBImage _inImg) throws ALDOperatorException {
  		this.setInImg(_inImg);
  		this.setFactoryMethod(FactoryMethod.DUPLICATE);
  	}
  	

	/**
  	 * Convert constructor
  	 * @param _inImg
  	 * @param type
  	 * @param _scaleDown
  	 * @throws ALDOperatorException
  	 */
  	public MTBImageFactory(MTBImage _inImg, MTBImageType type, boolean _scaleDown) 
  			throws ALDOperatorException {
  		this.setInImg(_inImg);
  		this.setFactoryMethod(FactoryMethod.CONVERT);
  		this.setTargetImageType(type);
  		this.setScaleDown(_scaleDown);
  	}
  	
  	/**
  	 * Get slice constructor
  	 * @param _inImg
  	 * @param _z
  	 * @param _t
  	 * @param _c
  	 * @throws ALDOperatorException
  	 */
  	public MTBImageFactory(MTBImage _inImg, int _z, int _t, int _c) 
  			throws ALDOperatorException {
  		this.setInImg(_inImg);
  		this.setFactoryMethod(FactoryMethod.SLICE);
  		this.setZ(_z);
  		this.setT(_t);
  		this.setC(_c);
  	}
  	
  	/**
  	 * Create a new image from the specified image part
  	 * @param _inImg
  	 * @param _x
  	 * @param _y
  	 * @param _z
  	 * @param _t
  	 * @param _c
  	 * @param _sizeX
  	 * @param _sizeY
  	 * @param _sizeZ
  	 * @param _sizeT
  	 * @param _sizeC
  	 * @throws ALDOperatorException
  	 */
  	public MTBImageFactory(MTBImage _inImg, int _x, int _y, int _z, int _t, 
  			int _c, int _sizeX, int _sizeY, int _sizeZ, int _sizeT, int _sizeC) 
  					throws ALDOperatorException {
  		this.setInImg(_inImg);
  		this.setFactoryMethod(FactoryMethod.IMAGE_PART);
  		this.setX(_x);
  		this.setY(_y);
  		this.setZ(_z);
  		this.setT(_t);
  		this.setC(_c);
  		this.setSizeX(_sizeX);
  		this.setSizeY(_sizeY);
  		this.setSizeZ(_sizeZ);
  		this.setSizeT(_sizeT);
  		this.setSizeC(_sizeC);
  	}
  	
//  	public boolean validateCustom() {
//  		boolean valid = true;
//  		
//  		FactoryMethod fm = null;
//  		
//		try {
//			fm = this.getFactoryMethod();
//		} catch (MTBOperatorException e) {
//			e.printStackTrace();
//			return false;
//		}
//  		
//  		if (fm == FactoryMethod.CONVERT) {
//  		
//  			try {
//				valid &= (this.getImageType() != null);
//			} catch (MTBOperatorException e) {
//				e.printStackTrace();
//				valid = false;
//			}
//  		}
//  		else if (fm == FactoryMethod.SLICE) {
//  			try {
//  				MTBImage img = this.getInImg();
//  				
//  				valid = valid && (this.getParameter("z") != null) && this.getZ() >= 0 && this.getZ() < img.getSizeZ();
//  				valid = valid && (this.getParameter("t") != null) && this.getT() >= 0 && this.getT() < img.getSizeT();
//  				valid = valid && (this.getParameter("c") != null) && this.getC() >= 0 && this.getC() < img.getSizeC();	
//  				
//  				
//  			} catch (MTBOperatorException e) {
//				e.printStackTrace();
//				valid = false;
//			}
//  		}
//  		else if (fm == FactoryMethod.IMAGE_PART) {
//  			try {
//  				
//  				// TODO: check size of copied part !!!!
//  				
//  				valid = valid && (this.getParameter("x") != null);
//  				valid = valid && (this.getParameter("y") != null);
//  				valid = valid && (this.getParameter("z") != null);
//  				valid = valid && (this.getParameter("t") != null);
//  				valid = valid && (this.getParameter("c") != null);	
//  				valid = valid && (this.getParameter("sizeX") != null);
//  				valid = valid && (this.getParameter("sizeY") != null);
//  				valid = valid && (this.getParameter("sizeZ") != null);
//  				valid = valid && (this.getParameter("sizeT") != null);
//  				valid = valid && (this.getParameter("sizeC") != null);
//		
//  				
//  			} catch (MTBOperatorException e) {
//				e.printStackTrace();
//				valid = false;
//			} 			
//  				
//  		}
//  		
//  		
//  		return valid;
//  	}
  	
  	@Override
  	protected void operate() throws ALDOperatorException {
  		
  		FactoryMethod fm = this.getFactoryMethod();
  		
		MTBImage resultImg = null;
  		
  		if (fm == FactoryMethod.DUPLICATE) {

  			// duplicate image
  			resultImg = this.duplicate(this.getInImg());
  			
  			if (resultImg != null) {
  				try {
  					resultImg.adoptSliceLabels(this.getInImg());
				} catch (IllegalArgumentException e) {
  					e.printStackTrace();
  					throw new ALDOperatorException(OperatorExceptionType.OPERATE_FAILED, "MTBImageFactory.operate(): DUPLICATE failed: " + e.getMessage());
  				}
				
  				this.setResultImg(resultImg);
  			}
  			else 
  				throw new ALDOperatorException(OperatorExceptionType.OPERATE_FAILED, "MTBImageFactory.operate(): DUPLICATE failed.");
  		}
  		else if (fm == FactoryMethod.CONVERT) {

  			// convert image to another type
  			resultImg = this.convertType(this.getInImg(), this.getTargetImageType(), this.getScaleDown());
  			
  			if (resultImg != null) {
  				try {
  					resultImg.adoptSliceLabels(this.getInImg());
  				} catch (IllegalArgumentException e) {
  					e.printStackTrace();
  					throw new ALDOperatorException(OperatorExceptionType.OPERATE_FAILED, "MTBImageFactory.operate(): CONVERT failed: " + e.getMessage());
  				}
  				
  				this.setResultImg(resultImg);
  			}
  			else 
  				throw new ALDOperatorException(OperatorExceptionType.OPERATE_FAILED, "MTBImageFactory.operate(): CONVERT failed.");
  		}
  		else if (fm == FactoryMethod.SLICE) {
  			
  			// get the copy of a slice of the image (in fact runs getImagePart)
  			resultImg = this.getSlice(this.getInImg(), this.getZ(), this.getT(), this.getC());
  				
  			if (resultImg != null) {
  				resultImg.setSliceLabel(this.getInImg().getSliceLabel(this.getZ(), this.getT(), this.getC()), 0, 0, 0);
  				this.setResultImg(resultImg);
  			}
  			else 
  				throw new ALDOperatorException(OperatorExceptionType.OPERATE_FAILED, "MTBImageFactory.operate(): SLICE copying failed.");
  		}
  		else if (fm == FactoryMethod.IMAGE_PART) {
  			
  			// get the copy of a specified image part
  			resultImg = this.getImagePart(this.getInImg(), this.getX(), this.getY(), this.getZ(), this.getT(), this.getC(),
  											this.getSizeX(), this.getSizeY(), this.getSizeZ(), this.getSizeT(), this.getSizeC());
				
  			if (resultImg != null) {
  				MTBImage inImg = this.getInImg();
  				int z0 = this.getZ(); int t0 = this.getT(); int c0 = this.getC();
  				int sz = this.getSizeZ(); int st = this.getSizeT(); int sc = this.getSizeC();
  				
  				for (int t = 0; t < st; t++) {
  					for (int z = 0; z < sz; z++) {
  						for (int c = 0; c < sc; c++) {
  							resultImg.setSliceLabel(inImg.getSliceLabel(z0 + z, t0 + t, c0 + c), z, t, c);
  						}
  					}
  				}
			
  				this.setResultImg(resultImg);
  			}
  			else 
  				throw new ALDOperatorException(OperatorExceptionType.OPERATE_FAILED, "MTBImageFactory.operate(): IMAGE_PART copying failed.");
  		}
  		else {
  			throw new ALDOperatorException(OperatorExceptionType.OPERATE_FAILED, "MTBImageFactory.operate() failed: Unknown creation method " + fm.toString());
  		}

  	}

  	

  	
  	
  	public MTBImage duplicate(MTBImage img) {
  		
  		MTBImage newImg = null;
  		
  		if (img.getType() == MTBImageType.MTB_INT
  				|| img.getType() == MTBImageType.MTB_DOUBLE
  				|| img.getType() == MTBImageType.MTB_RGB) {
  			
  			// duplicate MTBImages which are not based on ImageJ's ImagePlus
  			newImg = this.duplicateNonImageJType(img);
  			
  		}
  		else {
  			
  		// duplicate MTBImages which are based on ImagePlus
  			newImg = this.duplicateImageJType(img);
  		}
  		
  		return newImg;
  	}
  	
  	
  	private MTBImage duplicateImageJType(MTBImage img) {
  		
		// create new image stack
		ImageStack newStack = new ImageStack(img.m_sizeX, img.m_sizeY);
		
		// duplicate slices
		for (int i = 1; i <= img.m_sizeStack; i++) {
			newStack.addSlice(img.m_imgStack.getSliceLabel(i), img.m_imgStack.getProcessor(i).duplicate());
		}
		
		// create new ImagePlus
		ImagePlus newImP = img.m_img.createImagePlus();
	
		newImP.setDimensions(img.m_img.getNChannels(), img.m_img.getNSlices(), img.m_img.getNFrames());
		newImP.setStack(img.m_img.getTitle(), newStack);
		newImP.setOpenAsHyperStack(img.m_img.isHyperStack() || img.m_img.getOpenAsHyperStack());
		
		newImP.setPosition(img.m_img.getCurrentSlice());
		newImP.setDisplayRange(img.m_img.getDisplayRangeMin(), img.m_img.getDisplayRangeMax());
		

		newImP.setTitle(MTBImage.getTitleRunning(img.m_title));
		
		MTBImage newImg = MTBImage.createMTBImage(newImP);
		
		// copy current slice index
		newImg.m_currentSliceIdx = img.m_currentSliceIdx;
		
		// copy calibration object
		newImg.calibration = img.calibration.copy();
		
		if (img.xml != null)
			newImg.setXML(MTBImage.this.xml);
		
		// return new image wrapper
		return newImg;
  	}
  	
  	private MTBImage duplicateNonImageJType(MTBImage img) {
  		
  		MTBImage newImg = null;
  		
  		if (img.getType() == MTBImageType.MTB_DOUBLE) {
			newImg = new MTBImageDouble(img.m_sizeX, img.m_sizeY, img.m_sizeZ, img.m_sizeT, img.m_sizeC);	
			
			for (int i = 0; i < MTBImage.this.m_sizeStack; i++) {
				System.arraycopy(((MTBImageDouble)img).m_data[i], 0, ((MTBImageDouble)newImg).m_data[i], 0, img.m_sizeX*img.m_sizeY);
			}
  		}  		
  		else if (img.getType() == MTBImageType.MTB_INT) {
			newImg = new MTBImageInt(img.m_sizeX, img.m_sizeY, img.m_sizeZ, img.m_sizeT, img.m_sizeC);	
			
			for (int i = 0; i < MTBImage.this.m_sizeStack; i++) {
				System.arraycopy(((MTBImageInt)img).m_data[i], 0, ((MTBImageInt)newImg).m_data[i], 0, img.m_sizeX*img.m_sizeY);
			}
  		}
  		else if (img.getType() == MTBImageType.MTB_RGB) {
			newImg = new MTBImageRGB(img.m_sizeX, img.m_sizeY, img.m_sizeZ, img.m_sizeT, img.m_sizeC);	
			
			for (int i = 0; i < MTBImage.this.m_sizeStack; i++) {
				System.arraycopy(((MTBImageRGB)img).m_dataR[i], 0, ((MTBImageRGB)newImg).m_dataR[i], 0, img.m_sizeX*img.m_sizeY);
				System.arraycopy(((MTBImageRGB)img).m_dataG[i], 0, ((MTBImageRGB)newImg).m_dataG[i], 0, img.m_sizeX*img.m_sizeY);
				System.arraycopy(((MTBImageRGB)img).m_dataB[i], 0, ((MTBImageRGB)newImg).m_dataB[i], 0, img.m_sizeX*img.m_sizeY);
			}
  		}
	
		// set title
	      newImg.setTitle(MTBImage.getTitleRunning(img.m_title));
		
		// copy current slice index
		newImg.m_currentSliceIdx = img.m_currentSliceIdx;
		
		// copy calibration object
		newImg.calibration = img.calibration.copy();
		
		if (img.xml != null)
			newImg.setXML(MTBImage.this.xml);	
		
		// return new image wrapper
		return newImg;
	}
  	
  	
    /**
     * Create an image of given type from this image's values. Information is lost
     * if a type of less precision is used!
     * 
     * @param type
     *          new image's type (see static final fields for types)
     * @param scaleDown
     *          If true, the data is scaled down to fit in the range of the new
     *          image type values, if the new image type is of less precision. If
     *          false, the values are simply casted.
     * @return new image of given type
     */
    protected MTBImage convertType(MTBImage img, MTBImageType type, boolean scaleDown) {

      // create new image of given type
      MTBImage newImg = MTBImage.createMTBImage(img.m_sizeX, img.m_sizeY, img.m_sizeZ,
    		  img.m_sizeT, img.m_sizeC, type);

      // set title
      	newImg.setTitle(MTBImage.getTitleRunning(img.m_title));

      // copy calibration object
	  newImg.calibration = img.calibration.copy();
	  
	  if (img.xml != null)
			newImg.setXML(MTBImage.this.xml);
	  
      // get current slice index
      int idx = img.getCurrentSliceIndex();

      if (img.m_type == MTBImageType.MTB_RGB && type != MTBImageType.MTB_RGB) {
        // Conversion from RGB to gray value type

        int[] rgbValue;

        for (int i = 0; i < img.m_sizeStack; i++) {

          img.setCurrentSliceIndex(i);
          newImg.setCurrentSliceIndex(i);

          for (int y = 0; y < img.m_sizeY; y++) {
            for (int x = 0; x < img.m_sizeX; x++) {
              rgbValue = ((MTBImageRGB) img).getValue(x, y);

              newImg.putValueDouble(x, y, 0.299 * rgbValue[0] + 0.587
                  * rgbValue[1] + 0.114 * rgbValue[2]);
            }
          }
        }
      } 
      else if (img.m_type != MTBImageType.MTB_RGB && type == MTBImageType.MTB_RGB) {
        // Conversion from gray value type to RGB

        double val;

        double[] minmax = null;
        if (scaleDown) {
          minmax = img.getMinMaxDouble();
        }

        for (int i = 0; i < img.m_sizeStack; i++) {

          img.setCurrentSliceIndex(i);
          newImg.setCurrentSliceIndex(i);

          for (int y = 0; y < img.m_sizeY; y++) {
            for (int x = 0; x < img.m_sizeX; x++) {
              val = img.getValueDouble(x, y);

              if (scaleDown) {
                val = Math.round((val - minmax[0]) / (minmax[1] - minmax[0])
                    * 255.0);
              }

              ((MTBImageRGB) newImg).putValue(x, y, (int) val, (int) val,
                  (int) val);
            }
          }
        }
      } 
      else {
        // Conversion of gray value types or conversion from RGB to RGB

        double val;

        double[] minmax = null;

        if (type.ordinal() < img.m_type.ordinal() && scaleDown) {
          minmax = img.getMinMaxDouble();
        }

        for (int i = 0; i < img.m_sizeStack; i++) {

          img.setCurrentSliceIndex(i);
          newImg.setCurrentSliceIndex(i);

          for (int y = 0; y < img.m_sizeY; y++) {
            for (int x = 0; x < img.m_sizeX; x++) {
              val = img.getValueDouble(x, y);

              if (type.ordinal() < img.m_type.ordinal() && scaleDown) {
                val = Math.round((val - minmax[0]) / (minmax[1] - minmax[0])
                    * (newImg.getTypeMax() - newImg.getTypeMin())
                    + newImg.getTypeMin());
              }

              newImg.putValueDouble(x, y, val);
            }
          }
        }
      }

      // restore current slice index
      img.setCurrentSliceIndex(idx);
      newImg.setCurrentSliceIndex(0);

      return newImg;
    }
  		
  	
    /**
     * Get a copy of a part of this image as new MTBImage
     * 
     * @param x
     *          x-coordinate where the first value is copied from
     * @param y
     *          y-coordinate where the first value is copied from
     * @param z
     *          z-coordinate where the first value is copied from
     * @param t
     *          t-coordinate where the first value is copied from
     * @param c
     *          c-coordinate where the first value is copied from
     * @param sizeX
     *          size of the copied part in x-dimension
     * @param sizeY
     *          size of the copied part in y-dimension
     * @param sizeZ
     *          size of the copied part in z-dimension
     * @param sizeT
     *          size of the copied part in t-dimension
     * @param sizeC
     *          size of the copied part in c-dimension
     * @return new MTBImage with values equal to the specified part
     * @throws IllegalArgumentException
     *           if image boundaries are exceeded in any way
     */
    protected MTBImage getImagePart(MTBImage img, 
    								int x, int y, int z, int t, int c, int sizeX,
    								int sizeY, int sizeZ, int sizeT, int sizeC)
        							throws IllegalArgumentException {

      if (x + sizeX > img.m_sizeX || y + sizeY > img.m_sizeY
          || z + sizeZ > img.m_sizeZ || t + sizeT > img.m_sizeT
          || c + sizeC > img.m_sizeC) {
        throw new IllegalArgumentException(
            "MTBImage.getImagePart(..): Specified image part exceeds the image boundaries.");
      } else if (x < 0 || x >= img.m_sizeX || y < 0 || y >= img.m_sizeY
          || z < 0 || z >= img.m_sizeZ || t < 0 || t >= img.m_sizeT || c < 0
          || c >= img.m_sizeC) {
        throw new IllegalArgumentException(
            "MTBImage.getImagePart(..): Specified coordinate exceeds the image boundaries.");
      } else {

        MTBImage imgPart = MTBImage.createMTBImage(sizeX, sizeY, sizeZ, sizeT,
            sizeC, img.m_type);

        for (int cc = 0; cc < sizeC; cc++) {
          for (int tt = 0; tt < sizeT; tt++) {
            for (int zz = 0; zz < sizeZ; zz++) {
              for (int yy = 0; yy < sizeY; yy++) {
                for (int xx = 0; xx < sizeX; xx++) {
                  imgPart.putValueDouble(xx, yy, zz, tt, cc, img.getValueDouble(
                      x + xx, y + yy, z + zz, t + tt, c + cc));
                }
              }
            }
          }
        }

        imgPart.m_title = MTBImage.getTitleRunning(img.m_title);

        // copy calibration object
		imgPart.calibration = img.calibration.copy();

		// update origin
		imgPart.calibration.xOrigin = img.calibration.xOrigin + x;
		imgPart.calibration.yOrigin = img.calibration.yOrigin + y;
		imgPart.calibration.zOrigin = img.calibration.zOrigin + z;

		if (img.xml != null)
			imgPart.setXML(MTBImage.this.xml);
		
        return imgPart;
      }
    }
    
    /**
     * Get a copy of the specified slice
     * 
     * @param img			Image to process.	
     * @param _z			z-coordinate of desired slice.
     * @param _t			t-coordinate of desired slice.
     * @param _c			c-coordinate of desired slice.
     * @return	Copy of requested slice.
     * @throws IllegalArgumentException
     */
    protected MTBImage getSlice(MTBImage img, int _z, int _t, int _c) 
    		throws IllegalArgumentException {
    	return 
    			this.getImagePart(img,0,0,_z,_t,_c,img.m_sizeX,img.m_sizeY,1,1,1);
    }
    
  	
  	protected MTBImage getInImg() {
  		return this.inImg;
  	}
  	
  	protected void setInImg(MTBImage _inImg) {
  		this.inImg = _inImg;
  	}
  	
  	public MTBImage getResultImg() {
  		return this.resultImg;
  	}
  	
  	public void setResultImg(MTBImage _resultImg) {
  		this.resultImg = _resultImg;
  	}  	
  	
  	protected FactoryMethod getFactoryMethod() {
  		return this.factoryMethod;
  	}
  	
  	protected void setFactoryMethod(FactoryMethod fm) {
  		this.factoryMethod = fm;
  	}  	
  	
  	protected MTBImageType getTargetImageType() {
  		return this.targetImageType;
  	}
  	
  	protected void setTargetImageType(MTBImageType type) {
  		this.targetImageType = type;
  	}
  	
  	protected boolean getScaleDown() {
  		return this.scaleDown;
  	}
  	
  	@SuppressWarnings("unused")
    protected void setScaleDown(boolean _scaleDown) 
  			throws ALDOperatorException {
  		this.scaleDown = _scaleDown;
  	}
  	
  	protected int getX() {
  		return this.x.intValue();
  	}
  	
  	protected void setX(int _x) {
  		this.x = new Integer(_x);
  	}
  	
  	protected int getY() {
  		return this.y.intValue();
  	}
  	
  	protected void setY(int _y) {
  		this.y = new Integer(_y);
  	}
  	
  	protected int getZ() {
  		return this.z.intValue();
  	}
  	
  	protected void setZ(int _z) {
  		this.z = new Integer(_z);
  	}
  	
  	protected int getT() {
  		return this.t.intValue();
  	}
  	
  	protected void setT(int _t) {
  		this.t = new Integer(_t);
  	}
  	
  	protected int getC() {
  		return this.c.intValue();
  	}
  	
  	protected void setC(int _c) {
  		this.c = new Integer(_c);
  	}
  	
  	protected int getSizeX() {
  		return this.sizeX.intValue();
  	}
  	
  	protected void setSizeX(int _sizeX) {
  		this.sizeX = new Integer(_sizeX);
  	}
  	
  	protected int getSizeY() {
  		return this.sizeY.intValue();
  	}
  	
  	protected void setSizeY(int _sizeY) {
  		this.sizeY = new Integer(_sizeY);
  	}
  	
  	protected int getSizeZ() {
  		return this.sizeZ.intValue();
  	}
  	
  	protected void setSizeZ(int _sizeZ) {
  		this.sizeZ = new Integer(_sizeZ);
  	}
  	
  	protected int getSizeT() {
  		return this.sizeT.intValue();
  	}
  	
  	protected void setSizeT(int _sizeT) {
  		this.sizeT = new Integer(_sizeT);
  	}
  	
  	protected int getSizeC() {
  		return this.sizeC.intValue();
  	}
  	
  	protected void setSizeC(int _sizeC) {
  		this.sizeC = new Integer(_sizeC);
  	}
  	
  }
  
  
  
}
