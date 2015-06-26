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

package de.unihalle.informatik.MiToBo.segmentation.regions.mser;

import java.awt.geom.Point2D;

import net.imglib2.algorithm.componenttree.mser.*;
import net.imglib2.img.*;
import net.imglib2.img.cell.CellImgFactory;
import net.imglib2.Cursor;
import net.imglib2.Localizable;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.ByteType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.ShortType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;

import java.util.Iterator;
import java.util.Vector;

import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException.OperatorExceptionType;
import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBPoint3D;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2D;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion3D;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion3DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegionSetInterface;
import de.unihalle.informatik.MiToBo.core.datatypes.images.*;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.operator.*;

/**
 * Implementation of maximally stable extremal regions based on ImgLib2.
 * <p>
 * For details about the implementation and its parameters please refer to the
 * corresponding documentation pages in the ImgLib2 Javadoc API.
 * <p>
 * Please note that currently only gray-scale images are supported. The 
 * implementation handles data up to 3D, i.e. x/y/z. Multiple channels and time
 * steps are ignored, i.e. only the first channel and the first time step of 
 * an image is taken into account.
 * <p>
 * Related paper: Matas et al., 
 * <i>Robust wide-baseline stereo from maximally stable extremal regions</i>,
 * Image and Vision Computing, vol. 22, no. 10, pp. 761-767, 2004.
 * 
 * @see <a href="http://jenkins.imagej.net/job/ImgLib-daily/javadoc/net/imglib2/algorithm/componenttree/mser/MserTree.html">
 *      				ImgLib2 Javadoc API for MserTree</a>
 *
 * @author moeller
 */
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.ALL,
		level=Level.APPLICATION)
public class DetectMSERs extends MTBOperator {
	
	/**
	 * Modes for direction of intensity thresholding.
	 */
	public static enum Thresholding_Direction {
		/**
		 * Threshold the image starting with small thresholds. 
		 */
		DARK_TO_BRIGHT,
		/**
		 * Threshold the image starting with large thresholds.
		 */
		BRIGHT_TO_DARK
	}

	/**
	 * Input image to process.
	 */
	@Parameter( label= "Input Image", required= true, dataIOOrder= 0,
		direction= Parameter.Direction.IN, description= "Input image")
	private transient MTBImage inImg = null;
	
	/**
	 * Difference between threshold levels to compare.
	 * <p>
	 * This difference is to be specified as an absolute intensity difference.
	 */
	@Parameter( label= "Delta Threshold", required= true, dataIOOrder= 1, 
		direction= Parameter.Direction.IN, 
		description= "Threshold delta for computing instability score.")
	private double delta = 20;
	
	/**
	 * Minimum size of regions.
	 */
	@Parameter( label= "Minimum Region Size", required= true, dataIOOrder= 2, 
		direction= Parameter.Direction.IN, 
		description= "Minimum size (in pixels) of accepted MSERs.")
  private long minSize = 0;

	/**
	 * Maximum size of regions.
	 */
	@Parameter( label= "Maximum Region Size", required= true, dataIOOrder= 3, 
		direction= Parameter.Direction.IN, 
		description= "Maximum size (in pixels) of accepted MSERs.")
  private long maxSize = Long.MAX_VALUE;
  
	/**
	 * Maximum instability score, should be in the interval of [0,1].
	 * <p>
	 * The score is defined as 
	 * {@latex.inline $s(R_i) = \\frac{|R_i \\backslash R_{i-\\Delta}|}{|R_i|}$}.
	 * <p>
	 * The values of the score range from a minimum of 0 for a region of size 0
	 * to a maximum of 1. The MSER algorithms discards regions with an instability 
	 * score exceeding the given maximum instability score. Accordingly, the 
	 * maximum score should be set to a value within the range of [0,1]. 
	 * If the score is set to 0, all regions are discarded, if it is set to 1, 
	 * all regions are kept.
	 */
	@Parameter( label="Maximum Instability Score", required= true, 
		dataIOOrder= 4, direction = Parameter.Direction.IN, 
		description= "Maximum instability score of accepted MSERs, " + 
			"should lie between 0 and 1.")
  private double maxVar = 1.0;
  
	/**
	 * Minimum diversity score, should be in the interval of [0,1].
	 * <p>
	 * The diversity of two regions A and B is defined as
	 * {@latex.inline $\\frac{|B \\backslash A|}{|B|}$}.
	 * <p>
	 * A region A is discarded if the diversity with regard to B is less or equal
	 * to the given minimal diversity. The values of the diversity score range 
	 * from 0 to 1, i.e., the minimal diversity value should be chosen from this 
	 * range. A value of 1 discards all regions overlapping with their parents, 
	 * a value of 0 keeps all regions regardless of any existing overlap.
	 */
	@Parameter( label= "Minimal Diversity", required= true, dataIOOrder= 5, 
		direction= Parameter.Direction.IN, 
		description= "Minimal diversity of adjacent accepted MSERs, " +
			"should lie between 0 and 1.")
  private double minDiversity = 0.0;
  
	/**
	 * Thresholding direction.
	 * @see Thresholding_Direction
	 */
	@Parameter( label="Thresholding Direction", required= true, 
		dataIOOrder= 6, direction= Parameter.Direction.IN,
		description= "If to apply thresholds from dark to bright or vice versa.")
  private Thresholding_Direction tDirection;

	/**
	 * Flag to enable binary mask creation.
	 */
	@Parameter( label= "Create Binary Masks", required= false, dataIOOrder= 1,
		direction= Parameter.Direction.IN, description= "Turn on mask creation.")
	private transient boolean createMasks = false;

	/**
	 * Count of detected MSERs.
	 */
	@Parameter( label= "Number of MSERs", required= true, dataIOOrder= 0,
		direction= Parameter.Direction.OUT, description= "Number of MSERs.")
	private transient int mserCount;
  
	/**
	 * Set of detected MSER regions.
	 */
	@Parameter( label= "Resulting MSER regions", required= true, dataIOOrder= 1,
		direction= Parameter.Direction.OUT, description= "Result regions.")
	private transient MTBRegionSetInterface resultMSERs = null;

	/**
	 * (Optinal) stack with binary segmentation masks.
	 * <p>
	 * Only created if <code>createMasks</code> is set to true.
	 */
	@Parameter( label= "Binary Masks of Segmentation", dataIOOrder= 2,
		direction= Parameter.Direction.OUT, description= "Binary masks.")
	private transient MTBImageByte binaryMasks = null;

	/**
	 * Width of input image, i.e. dimension in x or first axis.
	 */
	private int iWidth;
	
	/**
	 * Height of input image, i.e. dimension in y or second axis.
	 */
	private int iHeight;
	
	/**
	 * Depth of input image, i.e. dimension in z or third axis.
	 */
	private int iDepth;

	/**
	 * Time dimensionality of input image, i.e. dimension in t or time line.
	 */
	private int iTimeDim;

	/**
	 * Default constructor.
	 *  @throws ALDOperatorException
	 */
	public DetectMSERs() throws ALDOperatorException {
		// nothing to do here
	}		

	/**
	 * Set input image to process.
	 * @param img	Image to process.
	 */
	public void setInputImage(MTBImage img) {
		this.inImg = img;
	}
	
	/**
	 * Set distance between threshold images to be compared.
	 * @param d	Distance to be applied.
	 */
	public void setDelta(double d) {
		this.delta = d;
	}
	
	/**
	 * Set minimal size of valid regions.
	 * <p>
	 * Regions smaller than the given threshold are discarded.
	 * @param ms	Minimal region size.
	 */
	public void setMinimalSize(long ms) {
		this.minSize = ms;
	}
	
	/**
	 * Set maximal size of valid regions.
	 * <p>
	 * Regions larger than the given threshold are discarded.
	 * @param ms	Maximal region size.
	 */
	public void setMaximalSize(long ms) {
		this.maxSize = ms;
	}
  
	/**
	 * Set maximal instability score.
	 * @param ms	Maximal instability score.
	 */
	public void setMaximalInstabilityScore(double ms) {
		this.maxVar = ms;
	}

	/**
	 * Set minimal diversity.
	 * @param ms	Minimal diversity allowed for valid regions.
	 */
	public void setMinimalDiversity(double md) {
		this.minDiversity = md;
	}

	/**
	 * Set direction of threshold compuations.
	 * @param ms	Direction of thresholding.
	 */
	public void setThresholdingDirection(Thresholding_Direction td) {
		this.tDirection = td;
	}

	/**
	 * Enable/disable creation of binary masks.
	 * @param b	If true, masks are created, otherwise not.
	 */
	public void setCreateBinaryMasks(boolean b) {
		this.createMasks = b;
	}
	
	/**
	 * Returns number of detected MSERs.
	 */
	public int getNumberOfMSERs() {
		return this.mserCount;
	}

	/**
	 * Returns set of detected MSERs.
	 * <p>
	 * Depending on the dimension of the input image either a set of 2D 
	 * or 3D regions is returned, i.e. the object is either of type 
	 * {@link MTBRegion2DSet} or {@link MTBRegion3DSet}.
	 */
	public MTBRegionSetInterface getMSERs() {
		return this.resultMSERs;
	}

	/**
	 * This method does the actual work.
	 * @throws ALDOperatorException 
	 */
	@Override
	protected void operate() throws ALDOperatorException {
		
		// reset operator
		this.binaryMasks = null;
		
		// init some helper variables
		this.iHeight = this.inImg.getSizeY();
		this.iWidth = this.inImg.getSizeX();
		this.iDepth = this.inImg.getSizeZ();
		this.iTimeDim = 1;

		MTBImageType T = this.inImg.getType();
		switch(T)
		{
			case MTB_BYTE:
			{
				// create an image factory that will instantiate the ImgLib2 image
				final ImgFactory<ByteType> imgFactory = 
					new CellImgFactory<ByteType>( 1 );
		    final Img<ByteType> iLibImg = imgFactory.create( 
		      new long[]{ this.iWidth, this.iHeight, this.iDepth, this.iTimeDim}, 
		      	new ByteType() );
				// set data
				Cursor<ByteType> iLibCursor = iLibImg.cursor();
//				for (int t=0; t<this.iTimeDim; ++t) {
				int t=0;
					for (int z=0; z<this.iDepth; ++z) {
						for (int y=0; y<this.iHeight; ++y) {
							for (int x=0; x<this.iWidth; ++x) {
								iLibCursor.fwd();
								iLibCursor.get().set((byte)this.inImg.getValueInt(x,y,z,t,0));
							}
						}
					}
//				}
				// detect MSERs
				this.extractMSERs(iLibImg);
				break;
			}
		case MTB_DOUBLE:
			{
				// create an image factory that will instantiate the ImgLib2 image
				final ImgFactory<DoubleType> imgFactory = 
					new CellImgFactory<DoubleType>( 1 );
		    final Img<DoubleType> iLibImg = imgFactory.create( 
		      	new long[]{ this.iWidth, this.iHeight, this.iDepth, this.iTimeDim}, 
		      		new DoubleType() );
				// set data
		    Cursor<DoubleType> iLibCursor = iLibImg.cursor();
//		    for (int t=0; t<this.iTimeDim; ++t) {
				int t=0;
		    	for (int z=0; z<this.iDepth; ++z) {
		    		for (int y=0; y<this.iHeight; ++y) {
		    			for (int x=0; x<this.iWidth; ++x) {
		    				iLibCursor.fwd();
		    				iLibCursor.get().set(this.inImg.getValueDouble(x,y,z,t,0));
		    			}
		    		}
		    	}
//		    }
		    // detect MSERs
		    this.extractMSERs(iLibImg);
		    break;
			}
		case MTB_FLOAT:
			{
				// create an image factory that will instantiate the ImgLib2 image
				final ImgFactory<FloatType> imgFactory = 
					new CellImgFactory<FloatType>( 1 );
		    final Img<FloatType> iLibImg = imgFactory.create( 
		      	new long[]{ this.iWidth, this.iHeight, this.iDepth, this.iTimeDim}, 
		      		new FloatType() );
				// set data
		    Cursor<FloatType> iLibCursor = iLibImg.cursor();
//		    for (int t=0; t<this.iTimeDim; ++t) {
				int t=0;
		    	for (int z=0; z<this.iDepth; ++z) {
		    		for (int y=0; y<this.iHeight; ++y) {
		    			for (int x=0; x<this.iWidth; ++x) {
		    				iLibCursor.fwd();
		    				iLibCursor.get().set((float)this.inImg.getValueDouble(x,y,z,t,0));
		    			}
		    		}
		    	}
//		    }
		    // detect MSERs
		    this.extractMSERs(iLibImg);
		    break;
			}
		case MTB_INT:
		{
			// create an image factory that will instantiate the ImgLib2 image
			final ImgFactory<IntType> imgFactory = 
				new CellImgFactory<IntType>( 1 );
	    final Img<IntType> iLibImg = imgFactory.create( 
	      	new long[]{ this.iWidth, this.iHeight, this.iDepth, this.iTimeDim}, 
	      		new IntType() );
			// set data
	    Cursor<IntType> iLibCursor = iLibImg.cursor();
//	    for (int t=0; t<this.iTimeDim; ++t) {
			int t=0;
	    	for (int z=0; z<this.iDepth; ++z) {
	    		for (int y=0; y<this.iHeight; ++y) {
	    			for (int x=0; x<this.iWidth; ++x) {
	    				iLibCursor.fwd();
	    				iLibCursor.get().set(this.inImg.getValueInt(x,y,z,t,0));
	    			}
	    		}
	    	}
//	    }
	    // detect MSERs
	    this.extractMSERs(iLibImg);
	    break;
		}
		case MTB_SHORT:
			{
				// create an image factory that will instantiate the ImgLib2 image
				final ImgFactory<ShortType> imgFactory = 
					new CellImgFactory<ShortType>( 1 );
		    final Img<ShortType> iLibImg = imgFactory.create( 
		      	new long[]{ this.iWidth, this.iHeight, this.iDepth, this.iTimeDim}, 
		      		new ShortType() );
				// set data
		    Cursor<ShortType> iLibCursor = iLibImg.cursor();
//		    for (int t=0; t<this.iTimeDim; ++t) {
				int t=0;
		    	for (int z=0; z<this.iDepth; ++z) {
		    		for (int y=0; y<this.iHeight; ++y) {
		    			for (int x=0; x<this.iWidth; ++x) {
		    				iLibCursor.fwd();
		    				iLibCursor.get().set((short)this.inImg.getValueInt(x,y,z,t,0));
		    			}
		    		}
		    	}
//		    }
		    // detect MSERs
		    this.extractMSERs(iLibImg);
		    break;
			}
		case MTB_RGB:
			{
				throw new ALDOperatorException(OperatorExceptionType.OPERATE_FAILED,
					"[DetectMSERs] color images not yet supported!");
			}
		}
		// create binary masks if requested
		if (this.createMasks) {
			this.binaryMasks = (MTBImageByte)MTBImage.createMTBImage(
					this.iWidth, this.iHeight, this.iDepth, this.iTimeDim, 1, 
					MTBImageType.MTB_BYTE);
			this.binaryMasks.fillBlack();
			if (this.iDepth == 1) {
				// 2D data
				MTBRegion2DSet regSet2D = (MTBRegion2DSet)this.resultMSERs;
				for (MTBRegion2D reg: regSet2D) {
					Vector<Point2D.Double> ps = reg.getPoints();
					for (Point2D.Double p: ps) {
						this.binaryMasks.putValueInt((int)p.x, (int)p.y, 255);
					}
				}
			}
			else {
				// 3D data
				MTBRegion3DSet regSet3D = (MTBRegion3DSet)this.resultMSERs;
				for (int i=0; i<regSet3D.size(); ++i) {
					MTBRegion3D reg = regSet3D.get(i);
					Vector<MTBPoint3D> ps = reg.getPoints();
					for (MTBPoint3D p: ps) {
						this.binaryMasks.putValueInt((int)p.x, (int)p.y, (int)p.z, 255);
					}
				}
			}
			this.binaryMasks.setTitle("MSER Binary Regions for <" 
				+ this.inImg.getTitle() + ">");
		}
	}

	/**
	 * Method to calculate MSERs from given image.
	 * @param img	Image to process.
	 */
	private <T extends RealType<T>> void extractMSERs(Img<T> img) {

		boolean darkToBright = false;
		if (this.tDirection.equals(Thresholding_Direction.DARK_TO_BRIGHT))
			darkToBright = true;
		else
			darkToBright = false;
		
		// build MSER tree
    MserTree< T > tree = MserTree.buildMserTree(img, this.delta, 
    	this.minSize, this.maxSize, this.maxVar, this.minDiversity,	darkToBright);
    
    // save number of detected MSERs
    this.mserCount = tree.size();

    // convert MSERs into region set
    double x, y, z;
    double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, 
    	minZ = Double.MAX_VALUE, maxX = 0, maxY = 0, maxZ = 0;
    Iterator< Mser<T> > it = tree.iterator();
    if (this.iDepth == 1) {
      Vector<Point2D.Double> ps2;
      Vector<MTBRegion2D> regs2 = new Vector<MTBRegion2D>();
      while(it.hasNext()) {
      	ps2 = new Vector<Point2D.Double>();
      	Mser<T> mser = it.next();
      	Iterator<Localizable> itl = mser.iterator();
      	while (itl.hasNext()) {
      		Localizable l = itl.next();
        	x = l.getDoublePosition(0);
        	y = l.getDoublePosition(1);
        	if (x < minX) minX = x;
        	if (x > maxX) maxX = x;
        	if (y < minY) minY = y;
        	if (y > maxY) maxY = y;
      		ps2.add( new Point2D.Double(x, y) );
      	}
      	regs2.add(new MTBRegion2D(ps2));
      }
    	this.resultMSERs = 
    		new MTBRegion2DSet(regs2, minX, minY, maxX, maxY);
    }
    else {
    	Vector<MTBPoint3D> ps3;
    	Vector<MTBRegion3D> regs3 = new Vector<MTBRegion3D>();
    	while(it.hasNext()) {
    		ps3 = new Vector<MTBPoint3D>();
    		Mser<T> mser = it.next();
    		Iterator<Localizable> itl = mser.iterator();
    		while (itl.hasNext()) {
    			Localizable l = itl.next();
    			x = l.getDoublePosition(0);
    			y = l.getDoublePosition(1);
    			z = l.getDoublePosition(2);
    			if (x < minX) minX = x;
    			if (x > maxX) maxX = x;
    			if (y < minY) minY = y;
    			if (y > maxY) maxY = y;
    			if (z < minZ) minZ = z;
    			if (z > maxZ) maxZ = z;
    			ps3.add( new MTBPoint3D(x, y, z) );
    		}
    		regs3.add(new MTBRegion3D(ps3));
    	}
    	this.resultMSERs = 
    			new MTBRegion3DSet(regs3, minX, minY, minZ, maxX, maxY, maxZ);
    }
	}
}
