package de.unihalle.informatik.MiToBo.apps.xylem;

import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.Alida.operator.events.ALDOperatorExecutionProgressEvent;
import de.unihalle.informatik.MiToBo.color.conversion.RGBToHSXConverter;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBContour2D;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2D;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBStructuringElement;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageByte;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;
import de.unihalle.informatik.MiToBo.gui.MTBTableModel;
import de.unihalle.informatik.MiToBo.morphology.BasicMorphology;
import de.unihalle.informatik.MiToBo.morphology.ComponentPostprocess;
import de.unihalle.informatik.MiToBo.segmentation.regions.filling.FillHoles2D;
import de.unihalle.informatik.MiToBo.segmentation.regions.labeling.LabelComponentsSequential;
import de.unihalle.informatik.MiToBo.topology.MTBTopologicalNumber.Point3D;
import de.unihalle.informatik.MiToBo.topology.MTBTopologicalNumber2D;
import de.unihalle.informatik.MiToBo.topology.MTBTopologicalNumber2DN4;
import de.unihalle.informatik.MiToBo.topology.MTBTopologicalNumber2DN8;

import java.awt.geom.Point2D;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.TreeMap;
import java.util.Vector;

/**
 * This operators implements region growing for xylem segmentation.
 * <p>
 * Prerequisite is 
 * HS[X]-image (hue, saturation, intensity/value/brightness) as the input
 * and some thresholds to control the growing process and
 * an initial segmentation of xylem regions supplied as a binary image.
 * <p>
 * All three channels of the hs[x] image are assume to be a byte image and values
 * in the range <code>0 - 255</code>.
 * <p>
 * The initial regions are eroded to get seed regions, where we
 * determine the mean-value of the hue and/or saturation and/or I/V/B-channel.
 * <br>
 * Subsequently the region are grown via region growing. We compare each pixel of the
 * contour of those seed regions with the pixel of the background beside them,
 * either in a 4-neighbourhood or an 8-neighbourhood.
 * <br />
 * This is repeated until the list of uninspected pixel is not empty.
 * Afterwards we may do some post processing. E.G. calculate gradient
 * informations on the found regions to split regions who contain more then one
 * xylem.
 * <p/>
 * @author Tim Langhammer, Halle/Saale, Europe <aerhx@vodafone.de>
 * @author posch
 */
@ALDAOperator( genericExecutionMode = ALDAOperator.ExecutionMode.ALL,
               level = ALDAOperator.Level.APPLICATION,
               shortDescription="Implements region growing for xylem segmentation.")
public class XylemGrower extends MTBOperator {
	
	// ===================================================================================
	// default parameter values
	
	/**
	 * Default size of the structuring element to erode the image
	 */
	public static final int DEFAULT_erodeSize = 5;
	
	/**
	 * Default minimum area for a region to be further eroded 
	 */
	public static final int DEFAULT_minAreaSeedRegions = 250;
	
	/**
	 * Default linkage mode for growing
	 */
	public static final XylemGrower.GrowingMode DEFAULT_linkageMode = XylemGrower.GrowingMode.HUE_SAT_X_STATIC;

	/**
	 * Default hue threshold
	 */
	public static final double DEFAULT_hueThresh = 55D;
	
	/**
	 * Default saturation threshold
	 */
	public static final double DEFAULT_satThresh = 55D;
	
	/**
	 * Default intensity threshold
	 */
	public static final double DEFAULT_xThresh = 30D;
	
	/**
	 * Default neighborhood for growing
	 */
	public static final XylemGrower.Neighbourhood DEFAULT_neighbourhood = XylemGrower.Neighbourhood.FOUR;

	/**
	 * Default size of structuring element for opening (post processing)
	 */
	public static final int DEFAULT_openingSESize = 7;
	
	/**
	 * Minimum area of a region in post processing.
	 */
	public static final int DEFAULT_minAreaPostProcessing = 300;

	/**
	 * Minimum size of an SE.
	 */
	private static final int MIN_SE_SIZE = 1;

	// ===================================================================================
	// enumerations

	/**
	 * The region growing modus. The suffix STATIC means, that the threshold for
	 * this channel is not updated after adding new pixels to the region. X is
	 * an acronym for {Intensity|Brightness|Value}.
	 */
	public enum GrowingMode {

		/**
		 * Hue only.
		 */
		HUE_ONLY,
		/**
		 * Hue, with static X.
		 */
		HUE_X_STATIC,
		/**
		 * Saturation, with static X.
		 */
		SAT_X_STATIC,
		/**
		 * Hue and Saturation, with static X.
		 */
		HUE_SAT_X_STATIC,
		/**
		 * Hue, Saturation and X.
		 */
		HUE_SAT_X;
	}

	/**
	 * The kind of neighbourhood to inspect a pixel. 
	 */
	public enum Neighbourhood {
		FOUR,
		EIGHT;
	}

	/**
	 * Sort Mode for different MTBRegion2D's.
	 */
	public enum SortMode {
		ID, AREA
	}

	// ===================================================================================
	// operator parameter

	/**
	 * HS(X)-Color Channel Image.
	 * <p/>
	 * Should be of type {
	 * <p/>
	 * @codeMTBImageByte }, containing three color channel( Hue, Saturation and
	 * Intensity/Brightness/Value).
	 */
	@Parameter( label = "XylemImageHSX", required = true,
			dataIOOrder = 0,
			direction = Parameter.Direction.IN,
			description = "Xylem image as HSX-image" )
	private transient MTBImageByte xylemHsxImage = null;

	/**
	 * Initial segmentation of xylem regions.
	 */
	@Parameter( label = "Inital segmentation", required = true,
			dataIOOrder = 1,
			direction = Parameter.Direction.IN,
			description = "Initial segmentation as a binary image" )
	private transient MTBImageByte initialSegmentation = null;

	/**
	 * The size of the structuring element for erosion to compute seed regions. If {@code ErodeMode} is
	 * set to {@linkplain ErodeMode#DYNAMIC Dynamic}, you should use a small
	 * size. Otherwise use a size >= 20.
	 */
	@Parameter( label = "SizeErosion", required = false, dataIOOrder = 3,
			direction = Parameter.Direction.IN,
			description = "Size of structuring element for ersion to compute seed regions",
			mode=ExpertMode.ADVANCED)
	private int erodeSize = DEFAULT_erodeSize;

	/**
	 * The minimum size of a {@code MTBRegion2D} to further erode when computing seed regions.
	 */
	@Parameter( label = "MinAreaToErode", required = false,
			dataIOOrder = 3,
			direction = Parameter.Direction.IN,
			description = "Minimal size of a region for further erosion of seed regions",
			mode=ExpertMode.ADVANCED )
	private int minAreaSeedRegions = DEFAULT_minAreaSeedRegions;

	/**
	 * The algorithm we use to link new pixels to the region.
	 */
	@Parameter( label = "The growing mode", required = true, dataIOOrder = 5,
			direction = Parameter.Direction.IN,
			description = "The method for growing the region" )
	private XylemGrower.GrowingMode linkageMode = DEFAULT_linkageMode;

	/**
	 * The hue threshold.
	 * <p/>
	 * Indicates whether a new pixel is part of a region or not, depending on
	 * growing algorithm.
	 */
	@Parameter( label = "HueThreshold", required = true, dataIOOrder = 6,
			direction = Parameter.Direction.IN,
			description = "Hue channel threshold" )
	private double hueThresh = DEFAULT_hueThresh;

	/**
	 * The saturation threshold.
	 * <p/>
	 * Indicates whether a new pixel is part of a region or not, depending on
	 * growing algorithm.
	 */
	@Parameter( label = "SaturationThreshold", required = true,
			dataIOOrder = 7,
			direction = Parameter.Direction.IN,
			description = "Saturation channel threshold" )
	private double satThresh = DEFAULT_satThresh;

	/**
	 * The Intensity/Brightness/Value threshold.
	 * <p/>
	 * Indicated whether a pixel is part of a region. Since we calculate the
	 * derivation of the mean of the regions value, this can be set to arbitrary
	 * values > 0.
	 * <p/>
	 */
	@Parameter( label = "XThreshold",
			required = true, dataIOOrder = 8,
			direction = Parameter.Direction.IN,
			description = "Intensity channel threshold" )
	private double xThresh = DEFAULT_xThresh;

	/**
	 * The neighbourhood mode to inspect new pixel.
	 */
	@Parameter( label = "Neighbourhood used", required = false, dataIOOrder = 9,
			direction = Parameter.Direction.IN,
			description = "The neighbourhood for the pixel to visit" ,
			mode=ExpertMode.ADVANCED)
	private XylemGrower.Neighbourhood neighbourhood = DEFAULT_neighbourhood;

	/**
	 * The minimum size of a {@code MTBRegion2D} before erosion.
	 */
	@Parameter( label = "SizeOpening (post processing)", required = true,
			dataIOOrder = 10,
			direction = Parameter.Direction.IN,
			description = "Size of SE for opening (post processing)" )
	private int openingSESize = DEFAULT_openingSESize;

	/**
	 * The minimum size of a {@code MTBRegion2D} to not be removed (after
	 * opening the grown regions).
	 */
	@Parameter( label = "MinArea (post processing)", required = true,
			dataIOOrder = 11,
			direction = Parameter.Direction.IN,
			description = "Minimal area of a region (post processing of growing)" )
	private int minAreaPostProcessing = DEFAULT_minAreaPostProcessing;

	
	/**
	 * Final xylem regions after post processing.
	 */
	@Parameter( label = "Resulting Xylem Regions", 			dataIOOrder = 1,
			direction = Parameter.Direction.OUT,
			description = "Final xylem regions after post processing" )
	private transient MTBImageByte resultXylemRegions;

	/**
	 * A table containing information for each region 
	 */
	@Parameter(label = "results table", required = true, 
			direction = Parameter.Direction.OUT, 
					dataIOOrder = 2,
			supplemental = false, description = "table containing the resulting values")
	private MTBTableModel resultsTable = null;

	/**
	 * The seed regions as a binary image after the final erosion.
	 */
	@Parameter( label = "Seed Regions", required = true,
			supplemental = true,
			direction = Parameter.Direction.OUT, dataIOOrder = 4,
			description = "Seed regions from erosion" )
	private MTBImageByte seedRegions;

	/**
	 * Resulting regions from growing.
	 * <p/>
	 * Each region is identified by its own id ( possible not unique ).
	 */
	@Parameter( label = "GrownRegions", required = true,
			dataIOOrder = 5,
			supplemental = true,
			direction = Parameter.Direction.OUT,
			description = "regions from growing" )
	private transient MTBImageByte grownRegions = null;


	// width, height of the binary input image 
	private int sizeX, sizeY;
	
	@Override
	public void validateCustom() throws ALDOperatorException {
		if( xylemHsxImage == null ) {
			throw new ALDOperatorException( ALDOperatorException.OperatorExceptionType.VALIDATION_FAILED,
					"HSX xylem input image is null!" );
		}

		if( initialSegmentation == null ) {
			throw new ALDOperatorException( ALDOperatorException.OperatorExceptionType.VALIDATION_FAILED,
					"Inital segmentation image is null!" );
		}

		if ( (initialSegmentation.getSizeX() != xylemHsxImage.getSizeX() ||
				initialSegmentation.getSizeY() != xylemHsxImage.getSizeY()	)) {
			throw new ALDOperatorException( ALDOperatorException.OperatorExceptionType.VALIDATION_FAILED,
					"Size of initial segmentation and xylem input image do not match!" );
		}
	}

	// ===================================================================================
	// METHODS
	
	/**
	 * Default Constructor.
	 * <p/>
	 * @throws ALDOperatorException
	 */
	public XylemGrower() throws ALDOperatorException {
		// empty
	}

	/**
	 *
	 * @param binaryImage
	 * @param hsxImage
	 * @param linkageMode
	 * @throws ALDOperatorException
	 */
	public XylemGrower( final MTBImageByte binaryImage, final MTBImageByte hsxImage,
			final XylemGrower.GrowingMode linkageMode ) throws
			ALDOperatorException {
		this.initialSegmentation = binaryImage;
		this.xylemHsxImage = hsxImage;
		this.linkageMode = linkageMode;
	}

	/**
	 * {@inheritDoc }
	 * <p/>
	 * @throws ALDOperatorException
	 * @throws ALDProcessingDAGException
	 */
	@Override
	protected void operate() throws ALDOperatorException,
	ALDProcessingDAGException {
		
		this.setHidingMode( HidingMode.HIDE_CHILDREN);
			
		//==================================================
        // initialization
		sizeX = initialSegmentation.getSizeX();
		sizeY = initialSegmentation.getSizeY();

		// init the set of grown regions 
		// probably not used currently
		final MTBRegion2DSet grownRegionSet = new MTBRegion2DSet( 0D, 0D, sizeX, sizeY );

		//==================================================
		// compute seed regions
		this.fireOperatorExecutionProgressEvent( new ALDOperatorExecutionProgressEvent(this,
		                "XylemGrower computing seed regions"));
		
		// clone the original input image	
		final MTBImageByte imageToErode = ( MTBImageByte ) initialSegmentation.duplicate();
		final List<MTBRegion2D> regionsToGrowList = 
				createSeedRegions( imageToErode, getminAreaSeedRegions(), erodeSize );
		 
		// construct an empty image to hold the seed regions
		// and label with regions found
		MTBImageByte seedRegionImage = (MTBImageByte) MTBImage.createMTBImage( sizeX, sizeY, 1, 1, 1, MTBImage.MTBImageType.MTB_BYTE);

		// for all regions to grow the the ids in seedRegionImage
		for( int i = 0; i < regionsToGrowList.size(); i++ ) {
			Iterator<Point2D.Double> pointsOfRegionIt = regionsToGrowList.get( i ).getPoints().iterator();
			int tempID = regionsToGrowList.get( i ).getID();
			while( pointsOfRegionIt.hasNext() ) {
				Point2D.Double tempP2D = pointsOfRegionIt.next();
				seedRegionImage.putValueInt( ( int ) tempP2D.x, ( int ) tempP2D.y, 0, 0, 0, tempID );
			}
		}
		seedRegionImage.setTitle("seedregions");
		setSeedImage( seedRegionImage);

		//==================================================
		// we now grow the regions
		this.fireOperatorExecutionProgressEvent( new ALDOperatorExecutionProgressEvent(this,
                "XylemGrower Growing the regions"));

		MTBImageByte grownRegionImage = (MTBImageByte) seedRegionImage.duplicate();
		for( int i = 0; i < regionsToGrowList.size(); i++ ) {
			final XylemGrower.GrowingRegion2D gr2D = new XylemGrower.GrowingRegion2D( 
					regionsToGrowList.get( i ), grownRegionImage,
					getNeighbourhood(), regionsToGrowList.get( i ).getID() );

			
			MTBRegion2D grownRegion = gr2D.growRegion();
			grownRegionSet.add( grownRegion );
		}

		// After growing we may have some holes in our regions
		final FillHoles2D fh2D = new FillHoles2D( grownRegionImage );
		fh2D.runOp( HidingMode.HIDDEN);

		MTBImage grownRegions = fh2D.getResultImage();
		grownRegions.setTitle("grownRegions");
		setGrownRegions( (MTBImageByte)grownRegions);
		
		//==================================================
		// post processing
		this.fireOperatorExecutionProgressEvent( new ALDOperatorExecutionProgressEvent(this,
                "XylemGrower post processing"));

		MTBImageByte resultImg = postProcessAfterGrowing( getGrownRegions());
		resultImg.setTitle("xylemRegions");
		setXylemResultRegions( resultImg);

		// create and write result table
		MTBRegion2DSet ppRSet = labelComponentsOfImage( getResultXylemRegions() );
		resultsTable = this.makeTable( ppRSet, fh2D.getResultImage(), sizeX, sizeY);
	}

	/**
	 * create results table
	 * @param labelImg 
	 * @param sizeY2 
	 * @param sizeX2 
	 * @param ppRSet 
	 * 
	 * @return table containing the results
	 */
	private MTBTableModel makeTable(final MTBRegion2DSet regionSet, 
			MTBImage labelImg, final int imgWidth, final int imgHeight)
	{
		int n = regionSet.size();
		
		// display options
		NumberFormat nf = NumberFormat.getInstance( Locale.ENGLISH);
		nf.setMaximumFractionDigits(2);

		// initialize table
		Vector<String> header = new Vector<String>();
		header.add("ID");
		header.add("label");
		header.add("Area");
		header.add("Circularity");
		header.add("Length of mayjor axis");
		header.add("Centroid x");
		header.add("Centroid y");
		header.add("Touches image border");
		
		MTBTableModel table = new MTBTableModel(n, header.size(), header);
		
		// get sorted MTBRegion2D's by Id
		Iterator<MTBRegion2D> sortedRegionsIt = sortRegionSetBy( regionSet, XylemGrower.SortMode.ID ).iterator();
		MTBRegion2D tempRegion = null;
		int row = 0;
		while( sortedRegionsIt.hasNext() ) {
			tempRegion = sortedRegionsIt.next();
			
			table.setValueAt( tempRegion.getID(), row, 0);
			try {
				int x = (int) tempRegion.getPoints().elementAt(0).x;
				int y = (int) tempRegion.getPoints().elementAt(0).y;

				table.setValueAt( labelImg.getValueInt(x, y), row, 1);
			} catch (Exception e) {
				// empty region
				table.setValueAt( Integer.MIN_VALUE, row, 1);
			}
			
			table.setValueAt( tempRegion.getArea(), row, 2);
			
			double circ;
			try {
				circ = tempRegion.getCircularity();

			} catch (Exception e) {
				circ = Double.NaN;
			}
			table.setValueAt( nf.format( circ), row, 3);

			table.setValueAt( nf.format( tempRegion.getMajorAxisLength()), row, 4);
			table.setValueAt( Math.round( tempRegion.getCenterOfMass_X() ), row, 5);
			table.setValueAt( Math.round( tempRegion.getCenterOfMass_Y() ), row, 6);
			table.setValueAt( 
					Boolean.valueOf( regionTouchesBoundary( tempRegion, imgWidth, imgHeight)).toString(), 
					row, 7);

			row++;
		}

		return table;
	}


	/**
	 * Here we do some morphological operations to the shape of the regions from
	 * the region growing of {@linkplain XylemGrower}.
	 * <p>
	 * @param grownRegionBinImage
	 * @return
	 * @throws ALDOperatorException
	 * @throws ALDProcessingDAGException
	 */
	private MTBImageByte postProcessAfterGrowing( final MTBImageByte grownRegionBinImage )
			throws ALDOperatorException, ALDProcessingDAGException {
		//
		MTBImageByte pastProGrowingImage = ( MTBImageByte ) grownRegionBinImage.duplicate();
		pastProGrowingImage = ( MTBImageByte ) XylemGrower.openByteImageWithCircle( pastProGrowingImage, getOpeningSESize() );
		pastProGrowingImage = ( MTBImageByte ) XylemGrower.removeSmallComponents( pastProGrowingImage, getMinRegionArea() );
		return pastProGrowingImage;
	}

	/**
	 * Set pixels of the points contained in the given region to
	 * {@code 0(zero)}.
	 * <p/>
	 * @param prelabeledImage The {@code MTBImageByte} image with prelabeled
	 * regions in it.
	 * @param regionToClear The {@code MTBRegion2D} region do delete from the
	 * {@code prelabeledImage}.
	 */
	private void deleteRegionFromImage( final MTBImageByte prelabeledImage, final MTBRegion2D regionToClear ) {

		final Iterator<Point2D.Double> regionPointsIt = regionToClear.getPoints().iterator();

		Point2D.Double tempPoint = null;

		while( regionPointsIt.hasNext() ) {
			tempPoint = regionPointsIt.next();
			prelabeledImage.putValueInt( ( int ) tempPoint.x, ( int ) tempPoint.y, 0, 0, 0, 0 );
		}

	}

	/**
	 * Create seed regions from the intial segmentation.
	 * <p/>
	 * <b>The workflow:</b>
	 * <br />
	 * i) we label our raw input image, to get the initial regions we want to
	 * erode.<br />
	 * ii) we set a unique id for each region<br />
	 * { <i>until there are regions to erode left ( area > minAreaSize )</i>
	 * <br />
	 * iii) we erode the image and relabel the result.
	 * <br />
	 * iv) we put for each pixel of all eroded regions the id of the regions
	 * before erosion, to preserve the
	 * <br />id's even if regions are splitted.
	 * <br />
	 * <i>- optional :</i>
	 * <br />
	 * v) if there are erased regions - we put the region before erasure to the
	 * list of regions to grow ( if set )
	 * <br />
	 * }
	 * <i>- optional :</i>
	 * <br />
	 * vi) we join splitted regions ( if set )
	 * <br />
	 * Step iii-v in detail:
	 * <p/>
	 * After we erode the image and label it we look at first for erased region!
	 * We can't put the code for removing small regions in the same loop,
	 * because those removed regions( by us ) are handled as erased regions( by
	 * the morpho op ) in the next step!
	 * <p/>
	 * So don't change this order!
	 * <p/>
	 * @see
	 * #labelComponentsOfImage(de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage)
	 * @see
	 * #erodeImage(de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage,
	 * int)
	 * @see
	 * #relabelRegion(de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageByte,
	 * de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2D)
	 * @param regionsBeforeErodeSet The regions after the opening.
	 * @param regionsAfterErodeSet The regions after the following erosion.
	 * @return A {@code  MTBRegion2DSet} of the possible erased regions.
	 */
	private List<MTBRegion2D> createSeedRegions( MTBImageByte imageToErode, final int minimumPixelPerArea, final int sizeOfStructuringElement )
			throws ALDOperatorException, ALDProcessingDAGException {
		// the list of regions to grow
		final List<MTBRegion2D> regionsToGrowL = new ArrayList<MTBRegion2D>();

		// i ) 
		// label the components of the input image
		MTBRegion2DSet regionsBeforeErosionSet = labelComponentsOfImage( imageToErode );

		// ii )
		// set a unique id for each pixel for each region of the labeled image 
		// the first unique ID, start with 2 to not run into problems with foreground pixel values of 1
		int uniqueID = 2;
		for( int i = 0; i < regionsBeforeErosionSet.size(); i++ ) {
			MTBRegion2D tempRegion = regionsBeforeErosionSet.get( i );
			// set the internal id to the unique id too
			tempRegion.setID( uniqueID );
			// set id for pixel of this region
			for( final Point2D point : tempRegion.getPoints() ) {
				imageToErode.putValueInt( (int)point.getX(), (int)point.getY(), 0, 0, 0, uniqueID );
			}
			uniqueID++;
		}

		// ------------------------------------------ the main loop ------------------------------------------

		// are there regions to erode?i.e. regions which are larger then minimumAreaSize
		boolean regionsToErodeLeft = false;

		do {		
			regionsToErodeLeft = false;

			// erode the image and label it
			imageToErode = (MTBImageByte)erodeImageWithCircle( imageToErode, sizeOfStructuringElement);
			MTBRegion2DSet regionsAfterErosionSet = labelComponentsOfImage( imageToErode );

			// counter for the occurence of id's of regions before erosion
			int[] regionIDCnt = new int[regionsBeforeErosionSet.size()];

			// after the erosion we have to relabel the new regions -> set our unique id's
			// this is very important to get always the same id for the new region
			// even if regions are deleted or splitted
			for( int idxAE = 0; idxAE < regionsAfterErosionSet.size(); idxAE++ ) {
				MTBRegion2D tempRegion = regionsAfterErosionSet.get( idxAE );
				
				relabelRegion( imageToErode, tempRegion );
				
				// increment the occurency number if the region is present
				for( int idxBE = 0; idxBE < regionsBeforeErosionSet.size(); idxBE++ ) {
					if( regionsAfterErosionSet.get( idxAE ).getID() == regionsBeforeErosionSet.get( idxBE ).getID() ) {
						regionIDCnt[idxBE]++;
					}
				}
			}

			// now we can look for erased regions
			for( int i = 0; i < regionIDCnt.length; i++ ) {
				// get the count
				if( regionIDCnt[i] == 0 ) {
					// and add it to the regions to grow-   -                                               regionsToGrowL.add( regionsBeforeErosionSet.get( i ) );
					regionsToGrowL.add( regionsBeforeErosionSet.get( i));
				}
			}

			// look now for regions which are too small to erode again 
			
			// list of MRBRegion2D-objects who are too small to erode again
			List<MTBRegion2D> toRemoveFromRegionSetList = new ArrayList<MTBRegion2D>();
			
			Iterator<MTBRegion2D> regionAfterErosionIt = regionsAfterErosionSet.iterator();
			while( regionAfterErosionIt.hasNext() ) {
				MTBRegion2D tempRegion = regionAfterErosionIt.next();
				
				if( tempRegion.getArea() > minimumPixelPerArea ) {
					// big enough to erode it again thus we have to erode the image again
					regionsToErodeLeft = true;
				} else {
					// not big enough to erode it again
					// add this region to the list of regions to grow
					// WARNING: we add the region after the size shrinks < minErodeSize
					//          even if the size is only 2 Pixel ...
					regionsToGrowL.add( tempRegion );
					
					// delete the region from the eroded image
					deleteRegionFromImage( imageToErode, tempRegion );
					
					toRemoveFromRegionSetList.add( tempRegion );
				}
			}

			// remove the small regions from the region set
			for( final MTBRegion2D region : toRemoveFromRegionSetList ) {
				regionsAfterErosionSet.remove( region );
			}

			// swap the region sets
			regionsBeforeErosionSet = regionsAfterErosionSet;
		} while( regionsToErodeLeft );

		return regionsToGrowL;
	}

	/**
	 * Erode an image of type {@link MTBImageByte MTBImageByte}.
	 * 
	 * @param image The {@code MTBImage} to erode.
	 * @param sizeOfSE The size of the structuring element ( >= 1 )
	 * @return The eroded {@code MTBImage} image.
	 * @throws ALDOperatorException
	 * @throws ALDProcessingDAGException
	 */
	static MTBImage erodeImageWithCircle( final MTBImage image, final int sizeOfSE )
			throws ALDOperatorException, ALDProcessingDAGException {
		if( image == null ) {
			throw new NullPointerException( "Null image @erodeImage" );
		}
		if( sizeOfSE < 1 ) {
			throw new IllegalArgumentException( "Size of structuring element should be greater 0 ( " + sizeOfSE + ")" );
		}
		final BasicMorphology bm = new BasicMorphology( image,
				MTBStructuringElement.createCircularElement( sizeOfSE ) );
		bm.setMode( BasicMorphology.opMode.ERODE );
		bm.runOp( HidingMode.HIDDEN);
		return bm.getResultImage();
	}

	/** Open an image of type {@link MTBImageByte MTBImageByte}.
	 *
	 * @param image
	 * @param sizeOfSE
	 * @return
	 * @throws ALDOperatorException
	 * @throws ALDProcessingDAGException
	 */
	private static MTBImage openByteImageWithCircle( final MTBImage image, final int sizeOfSE )
			throws ALDOperatorException, ALDProcessingDAGException {
		if( image == null ) {
			throw new NullPointerException( "Null image @openImage" );
		}
		if( sizeOfSE < 1 ) {
			throw new IllegalArgumentException( "Size of structuring element should be greater 0 ( " + sizeOfSE + ")" );
		}
		final BasicMorphology bm = new BasicMorphology( image,
				MTBStructuringElement.createCircularElement( sizeOfSE ) );
		bm.setMode( BasicMorphology.opMode.OPEN );
		bm.runOp( HidingMode.HIDDEN);
		return bm.getResultImage();
	}

	/** Remove small componentes from an image of type {@link MTBImageByte MTBImageByte}.
	 *
	 * @param image
	 * @param minimalArea
	 * @return
	 * @throws ALDOperatorException
	 * @throws ALDProcessingDAGException
	 */
	
	private static MTBImage removeSmallComponents( final MTBImageByte image, final int minimalArea )
			throws ALDOperatorException, ALDProcessingDAGException {
		if( image == null ) {
			throw new NullPointerException( "Null image @removeSmallComponents!" );
		}
		if( minimalArea < 1 ) {
			throw new IllegalArgumentException( "Minimal area should be greater 0 ( " + minimalArea + ")" );
		}
		final ComponentPostprocess cp = new ComponentPostprocess( image, ComponentPostprocess.ProcessMode.ERASE_SMALL_COMPS );
		cp.setMinimalComponentSize( minimalArea );
		cp.runOp( HidingMode.HIDDEN);
		return cp.getResultImage();
	}

	/**
	 * Test whether a region is located at, i.e. touches, the boundary of the image.
	 * 
	 * @param region2D
	 * @param width
	 * @param height
	 * @return true if region touches the boundary
	 */
	private static boolean regionTouchesBoundary( final MTBRegion2D region2D, 
			final int width, final int height ) {
		boolean rc = false;
		Iterator<Point2D.Double> areaIt = region2D.getPoints().iterator();
		Point2D.Double tempPixelCo = null;
		int tempX, tempY;
		while( areaIt.hasNext() ) {
			tempPixelCo = areaIt.next();
			tempX = Double.valueOf( tempPixelCo.x ).intValue();
			tempY = Double.valueOf( tempPixelCo.y ).intValue();
			// check whether this pixel is at the border of the image
			if( tempX == 0 || tempY == 0 || tempX == width-1 || tempY == height-1 ) {
				rc = true;
				break;
			}
		}
		return rc;
	}

	/**
	 * Label a {@linkplain MTBImage MTBImage}.
	 * 
	 * @see
	 * de.unihalle.informatik.MiToBo.segmentation.regions.labeling.LabelComponentsSequential
	 * @param image The {@code MTBImage} to label.
	 * @return The resulting {@linkplain MTBRegion2DSet MTBRegion2DSet}.
	 * @throws ALDOperatorException
	 * @throws ALDProcessingDAGException
	 */
	static MTBRegion2DSet labelComponentsOfImage( final MTBImage image )
			throws ALDOperatorException, ALDProcessingDAGException {
		final LabelComponentsSequential scs = new LabelComponentsSequential( image, true );
		scs.runOp( HidingMode.HIDDEN);
		return scs.getResultingRegions();
	}

	/**
	 * Set the unique id of the given region to the pixel value found at the
	 * coordinate of the first pixel in the region on the pre labeled
	 * {@code MTBImageByte}.
	 * <p/>
	 * @param preLabeledImage The {@code MTBImageByte} image we fetch the wished
	 * id from.
	 * @param region The {@code MTBRegion2D} region to set the found {@code id}
	 * to.
	 */
	private void relabelRegion( final MTBImageByte preLabeledImage, final MTBRegion2D region ) {
		if ( region.getArea() <= 0)
			return;
			
		// get the first pixel of this region
		final Point2D.Double pointForID = region.getPoints().get( 0 );
		// get it's id ( we put this id after our initial labeling )
		int initialUniqueID = preLabeledImage.getValueInt( ( int ) pointForID.x, ( int ) pointForID.y, 0, 0, 0 );
		// set the id
		region.setID( initialUniqueID );
	}

	/**
	 *
	 * @param regionSet
	 * @param sortMode
	 * @return
	 */
	private static Collection<MTBRegion2D> sortRegionSetBy( final MTBRegion2DSet regionSet, final XylemGrower.SortMode sortMode ) {

		Collection<MTBRegion2D> sortedRegions = null;

		if( sortMode.equals( XylemGrower.SortMode.ID ) ) {
			TreeMap<Integer, MTBRegion2D> sortMap = new TreeMap<Integer, MTBRegion2D>();
			for( int i = 0; i < regionSet.size(); i++ ) {
				sortMap.put( regionSet.get( i ).getID(), regionSet.get( i ) );
			}
			sortedRegions = sortMap.values();
		} else {
			TreeMap<Integer, MTBRegion2D> sortMap = new TreeMap<Integer, MTBRegion2D>();
			for( int i = 0; i < regionSet.size(); i++ ) {
				sortMap.put( regionSet.get( i ).getArea(), regionSet.get( i ) );
			}
			sortedRegions = sortMap.values();
		}
		return sortedRegions;
	}

	/**
	 * This class is responsible for the growing of a single
	 * {@linkplain MTBRegion2D} which is the result of the preprocessing steps.
	 */
	private class GrowingRegion2D {

		//  background pixel
		private final int BACKGROUND_PIXEL = 0;

		// region to grow
		private final MTBRegion2D growingRegion;

		// image of growing/grown regions
		private MTBImageByte grownRegionImage;

		// neighborhood for growing
		private final XylemGrower.Neighbourhood neighbourhood;

		// id of the region to grow ( > 0 )
		private final int regionID;

		// mean of the hue channel
		// ATTENTION  : The raw hue values are in the range of 0-255
		private double hueMeanDegree;

		// sum of hue values as vectors (use a Point2D to store the sum)
		private Point2D.Double sumOfHueCartesian;


		// sum and mean of saturation	
		private double satSum;
		private double satMean;

		// sum and mean of intensity/brightness/value
		private double xMean;
		private double xMeanSum;

		// static x threshold: mean of the initial region (before growing)
		// minus the threshold supplied by the user
		private double localXThresh;

		// The number of pixel with defined hue channel value
		private int numOfDefinedHuePixel;

		// The number of pixel with defined saturation channel value
		private int numOfDefinedSaturationPixel;

		// The number of pixels with X(brightness/intensity/value) pixel, which 
		// is true for all pixels
		private int numOfXPixel;

		// List of pixel to inspect for membership to this region
		private final Queue<Point2D.Double> toInspectL = new LinkedList<Point2D.Double>();

		// the used Neighbourhood-Inspector
		private final MTBTopologicalNumber2D topoNumber2D;

		private GrowingRegion2D( final MTBRegion2D initialRegion, MTBImageByte grownRegionImage, 
				final XylemGrower.Neighbourhood nb, final int uniqueID ) {
			this.regionID = uniqueID;
			this.growingRegion = initialRegion;
			this.grownRegionImage = grownRegionImage;
			this.neighbourhood = nb;
			this.sumOfHueCartesian = new Point2D.Double();
			switch( neighbourhood ) {
			case FOUR:
				topoNumber2D = new MTBTopologicalNumber2DN4();
				break;
			case EIGHT:
				topoNumber2D = new MTBTopologicalNumber2DN8();
				break;
			default:
				topoNumber2D = new MTBTopologicalNumber2DN4();
			}
		}

		/**
		 * Grow the region.
		 * <p/>
		 * This is the main loop, where we inspect each pixel based on the
		 * contour of the region, and visit each neighbor ( eight/four ) and
		 * compare the hue and the intensity.
		 * 
		 * @see #visitNeighboursOf(java.awt.geom.Point2D.Double)
		 * @return The {@code MTBRegion2D} after the growing is finished, i.e.
		 * there are no more pixel left in the list of unvisited pixels.
		 * @throws ALDOperatorException
		 * @throws ALDProcessingDAGException
		 */
		private MTBRegion2D growRegion() throws ALDOperatorException, 	ALDProcessingDAGException {
			// initialization
			this.setup();

			// add all pixels of the initial region contour to the list of pixel to inspect
			final MTBContour2D regionContour = growingRegion.getContour();
			for( final Point2D.Double point : regionContour.getPoints() ) {
				toInspectL.add( point );
			}

			// grow the region
			while( ! toInspectL.isEmpty() ) {
				// get the first element of the list ( queue )
				Point2D.Double tempPoint = toInspectL.remove();
				final List<Point2D.Double> addedPixelL = visitNeighboursOf( tempPoint );
				toInspectL.addAll( addedPixelL );
			}

			return growingRegion;
		}

		/**
		 * Recalculate the mean including incrementing the number
		 * of pixels with define hue value, i.e. the number of pixels contributing
		 * to the hue sum.
		 * <p/>
		 * It first converts the angle into a vector of length 1, then add the new
		 * coordinate to the sum of vectors. Then it calculates the new
		 * hue mean in radians-format, finally setting the hue mean by converting
		 * it into degree.
		 * 
		 * @param newHueValue The new hue-value.
		 */
		private void recalcHueMean( final double newHueValue ) {
			final Point2D.Double newHuePolarCoordinate = MathHelper.toCartesianAsPoint2D( newHueValue );

			sumOfHueCartesian.x += newHuePolarCoordinate.x;
			sumOfHueCartesian.y += newHuePolarCoordinate.y;
			numOfDefinedHuePixel++;

			final double hueMeanRadian = Math.atan2( sumOfHueCartesian.y / numOfDefinedHuePixel,
					sumOfHueCartesian.x / numOfDefinedHuePixel );

			hueMeanDegree = Math.toDegrees( hueMeanRadian );
			// IMPORTANT : Add 360 degree if the angle is negative
			if( hueMeanDegree < 0D ) {
				hueMeanDegree += 360D;
			}
		}

		/**
		 * Recalculate the saturation of the region including incrementing the number
		 * of pixels with define saturation value, i.e. the number of pixels contributing
		 * to the saturation sum.
		 * 
		 * @param newSaturationValue
		 */
		private void recalcSatMean( final double newSaturationValue ) {
			satSum += newSaturationValue;
			numOfDefinedSaturationPixel++;

			satMean = satSum / numOfDefinedSaturationPixel;
		}

		/**
		 * Recalculate the X(Intensity / Brightness / Value )-value of the
		 * region.
		 * <p/>
		 * NOTE: we assume that <code>numOfXPixel</code> is already incremented
		 * 
		 * @param newXValue
		 */
		private void recalcXMean( final double newXValue ) {
			xMeanSum += newXValue;
			xMean = xMeanSum / numOfXPixel;
		}

		/**
		 * Initialize the class for the region
		 */
		private void setup() {
			int x, y;
			float tempHue, tempSaturation, tempX;

			// for all Pixel of our region
			final Iterator<Point2D.Double> pointsIt = growingRegion.getPoints().iterator();

			while( pointsIt.hasNext() ) {
				Point2D.Double tempPoint = pointsIt.next();
				x = ( int ) tempPoint.x;
				y = ( int ) tempPoint.y;

				tempHue = Double.valueOf( xylemHsxImage.getValueDouble( x, y, 0, 0, 
						RGBToHSXConverter.ColorChannel.HUE.getIndex() ) ).floatValue();
				// scale to 0-359
				tempHue *= RGBToHSXConverter.BYTE_TO_DEGREE;

				// saturation
				tempSaturation = Double.valueOf( xylemHsxImage.getValueDouble( x, y, 0, 0, 
						RGBToHSXConverter.ColorChannel.SATURATION.getIndex())).floatValue();

				// x-value
				tempX = Double.valueOf( xylemHsxImage.getValueDouble( x, y, 0, 0, 
						RGBToHSXConverter.ColorChannel.INTENSITY.getIndex())).floatValue();

				// the brightness/intensity/value channel is always defined
				numOfXPixel++;
				xMeanSum += tempX;

				// only if the hue is defined we increase the hue sum
				// and calculate the vector sum
				if( ! RGBToHSXConverter.isHueUndefined( tempSaturation) ) {
					numOfDefinedHuePixel++;
					Point2D.Double tempCartesianCo = MathHelper.toCartesianAsPoint2D( tempHue);
					sumOfHueCartesian.x += tempCartesianCo.x;
					sumOfHueCartesian.y += tempCartesianCo.y;
				}

				// only if the saturation is defined increase the saturation sum
				if( ! RGBToHSXConverter.isSaturationUndefined( tempX ) ) {
					numOfDefinedSaturationPixel++;
					satSum += tempSaturation;
				}
			}

			// now we compute the means if required

			// calculate the hue mean via arcus tangens 2
			// ATTENTION: the formula is atan( y / x )!
			// ATTENTION: the result is in the range of [-pi,pi]

			final double hueMeanArc = Math.atan2( sumOfHueCartesian.y / numOfDefinedHuePixel,
					sumOfHueCartesian.x / numOfDefinedHuePixel );

			hueMeanDegree = Math.toDegrees( hueMeanArc );

			// if we got negative angle, we got to add 360 
			// positive angles lies in the first or fourth quadrant, negatives in the second or third
			// QUESTION: do we need  '<' or '<=' here?
			if( hueMeanDegree < 0D ) {
				hueMeanDegree += 360D;
			}

			// mean of saturation and x 
			satMean = satSum / numOfDefinedSaturationPixel;
			xMean = xMeanSum / numOfXPixel;

			// for static x threshold, we use the mean of x minus the threshold from the user
			if( linkageMode.equals( GrowingMode.HUE_X_STATIC ) ||
					linkageMode.equals( GrowingMode.SAT_X_STATIC ) ||
					linkageMode.equals( GrowingMode.HUE_SAT_X_STATIC ) 	) {
				localXThresh = xMean - getXThresh();
			}
		}

		/**
		 * Inspect the eight/four neighbours of the pixel with the given
		 * coordinate.
		 * <p/>
		 * @param pixelToVisit The {@code Point2D.Double}-coordinate of the
		 * pixel to visit.
		 * @return int The count of added pixel
		 */
		private List<Point2D.Double> visitNeighboursOf( final Point2D.Double pixelToVisit ) {
			// list of neighbors added to the region during visitNeighboursOf
			final List<Point2D.Double> pointsAddedL = new ArrayList<Point2D.Double>();

			// the integer coordinates of the pixel to visit
			final int x = Double.valueOf( pixelToVisit.x ).intValue();
			final int y = Double.valueOf( pixelToVisit.y ).intValue();

			// inspect all neigbours of the actual pixel ( x, y )
			final Iterator<Point3D> offsets = topoNumber2D.iteratorOffsets();
			while( offsets.hasNext() ) {
				// next neighbour-pixel
				final Point3D offset = offsets.next();

				// set the new x, y coordinate
				// coordinates of the neighbor
				int xNeighbor = x + offset.getX();
				int yNeighbor = y + offset.getY();

				checkNeighbor( pointsAddedL, xNeighbor, yNeighbor);
			}

			return pointsAddedL;
		}


		/** Test if the given neighbor is to be added to the region.
		 * If so is actually added to the region and also added to <code>pointsAddedL</code>
		 * which may me modified as a side effect.
		 * 
		 * @param pointsAddedL
		 * @param xNeighbor
		 * @param yNeighbor
		 * @return
		 */
		private void checkNeighbor( List<Point2D.Double> pointsAddedL, 
				int xNeighbor, int yNeighbor) {
			// check borders
			if( xNeighbor >= 0 && yNeighbor >= 0 && xNeighbor < sizeX && yNeighbor < sizeY ) {
				// check the label ( id ) of the new pixel
				final int labelNeighbor = grownRegionImage.getValueInt( xNeighbor, yNeighbor, 0, 0, 0 );
				// Background -> check the hue channel and or the saturation channel and intensity
				if( labelNeighbor == BACKGROUND_PIXEL ) {

					// placeholder for hue, saturation and intensity
					float hueChannel, saturationChannel, xChannel;
					// is the hue channel value valid?
					boolean hueDefined = false;
					// is the saturation channel value valid?
					boolean saturationDefined = false;

					// hue
					hueChannel = Double.valueOf( xylemHsxImage.getValueDouble( xNeighbor, yNeighbor, 0, 0, RGBToHSXConverter.ColorChannel.HUE.getIndex() ) ).floatValue();
					// increase range to [0-359]
					hueChannel *= RGBToHSXConverter.BYTE_TO_DEGREE;
					// saturation
					saturationChannel = Double.valueOf( xylemHsxImage.getValueDouble( xNeighbor, yNeighbor, 0, 0, RGBToHSXConverter.ColorChannel.SATURATION.getIndex() ) ).floatValue();
					// intensity / brightness / value
					xChannel = Double.valueOf( xylemHsxImage.getValueDouble( xNeighbor, yNeighbor, 0, 0, RGBToHSXConverter.ColorChannel.INTENSITY.getIndex() ) ).floatValue();

					// is hue defined?
					hueDefined = !RGBToHSXConverter.isHueUndefined( saturationChannel );
					// is saturation defined?
					saturationDefined = !RGBToHSXConverter.isSaturationUndefined( xChannel );

					// the difference of the hue angles
					final double hueDiff = MathHelper.angleDiff360( hueMeanDegree, hueChannel );
					// saturation difference
					final double saturationDiff = Math.abs( satMean - saturationChannel );
					// intensity/brightness/value
					final double xDiff = Math.abs( xMean - xChannel );
					// add the pixel
					boolean add = false;
					// 
					switch( linkageMode ) {

					case HUE_ONLY: 
						if( hueDefined ) {
							add = hueDiff < getHueThresh();
							if( add ) {
								recalcHueMean( hueChannel );
							}
						} else {
							add =false;
						}
						break;

					case HUE_X_STATIC:
						if( hueDefined ) {
							add = hueDiff < getHueThresh() && xChannel > localXThresh;
							if( add ) {
								recalcHueMean( hueChannel );
							}
						} else {
							add = xChannel > localXThresh;
						}
						break;
						
					case SAT_X_STATIC:
						if( saturationDefined ) {
							add = saturationDiff < getSatTresh() && xChannel > localXThresh;
							if( add ) {
								recalcSatMean( saturationChannel );
							}
						} else {
							add = xChannel > localXThresh;
						}
						break;
						
					case HUE_SAT_X_STATIC:
						add = (! hueDefined || hueDiff < getHueThresh() ) && 
						      (! saturationDefined || saturationDiff < getSatTresh() ) && 
						        xChannel > localXThresh;

						if( add && hueDefined ) {
							recalcHueMean( hueChannel );
						}
						
						if( add && saturationDefined ) {
							recalcSatMean( saturationChannel );
						}

						break;
						
					case HUE_SAT_X:
						add = (! hueDefined || hueDiff < getHueThresh() ) && 
					          (! saturationDefined || saturationDiff < getSatTresh() ) &&
					          xDiff < getXThresh();

						if( add && hueDefined ) {
							recalcHueMean( hueChannel );
						}
						
						if( add && saturationDefined ) {
							recalcSatMean( saturationChannel );
						}
						
						recalcXMean( xChannel );

						break;
					}
					// pixel accepted...
					if( add ) {
						numOfXPixel++;
						growingRegion.addPixel( xNeighbor, yNeighbor );
						pointsAddedL.add( new Point2D.Double( xNeighbor, yNeighbor ) );
						grownRegionImage.putValueInt( xNeighbor, yNeighbor, 0, 0, 0, regionID );
					} else if( labelNeighbor == regionID ) {
						// Pixel is part of the actual region
					} else {
						// Pixel belongs to an other region
					}
				}
			}
		}
	}


	public static String printReadabeTimeInfo( final long cTime ) {

		final int millis = ( int ) ( cTime / 1000000 );
		final int seconds = millis / 1000;
		final int minutes = seconds / 60;

		return "Millis : " + millis + " Seconds : " + seconds + " Minutes " + minutes;
	}

	//=================================================================================================
	// GETTER and SETTER
	/**
	 *
	 * @return
	 */
	public MTBImage getInitialSegmentation() {
		return initialSegmentation;
	}

	/**
	 *
	 * @param image
	 */
	public void setInitalSegmentation( final MTBImageByte image ) {
		this.initialSegmentation = image;
	}

	/**
	 * Set the size of the eroding element in pixel.
	 * <p/>
	 * @param size The site of the structuring element used for erosion in
	 * pixel.
	 */
	public void setErodSize( final int size ) {
		this.erodeSize = size;
	}

	/**
	 * The size of the eroding element.
	 * <p/>
	 * @return The size of the eroding element in pixel.
	 */
	public int getErodeSize() {
		return erodeSize;
	}

	/**
	 * Return the Hue/Saturation/{Intensity|Brightness|Value}-{@code MTBImage}.
	 * <p/>
	 * @return
	 */
	public MTBImage getXylemImage() {
		return xylemHsxImage;
	}

	/**
	 * Set the Hue/Saturation/{Intensity|Brightness|Value}-{@code MTBImage}.
	 * <p/>
	 * @param hsxMTBImage
	 */
	public void setXylemImage( final MTBImageByte hsxMTBImage ) {
		this.xylemHsxImage = hsxMTBImage;
	}

	/**
	 * Return the binary image after the post processing of the grown region
	 * image.
	 * 
	 * @return
	 */
	public MTBImageByte getResultXylemRegions() {
		return resultXylemRegions;
	}

	/**
	 * Set the binary image after the morphological processing on the grown
	 * region image.
	 * 
	 * @param regions
	 */
	public void setXylemResultRegions( MTBImageByte regions ) {
		this.resultXylemRegions = regions;
	}

	/**
	 * @return the resultsTable
	 */
	public MTBTableModel getResultsTable() {
		return resultsTable;
	}

	/**
	 * @param resultsTable the resultsTable to set
	 */
	public void setResultsTable(MTBTableModel resultsTable) {
		this.resultsTable = resultsTable;
	}

	/**
	 * Return the treshold for the hue channel.
	 * 
	 * @return
	 */
	public double getHueThresh() {
		return hueThresh;
	}

	/**
	 *
	 * @param hueThresh
	 */
	public void setHueThresh( final double hueThresh ) {
		this.hueThresh = hueThresh;
	}

	/**
	 *
	 * @return
	 */
	public XylemGrower.GrowingMode getLinkageMode() {
		return linkageMode;
	}

	/**
	 *
	 * @param linkageMode
	 */
	public void setLinkageMode( final XylemGrower.GrowingMode linkageMode ) {
		this.linkageMode = linkageMode;
	}

	/**
	 *
	 * @return
	 */
	public int getminAreaSeedRegions() {
		return minAreaSeedRegions;
	}

	/**
	 *
	 * @param minAreaSeedRegions
	 */
	public void setminAreaSeedRegions( int minAreaSeedRegions ) {
		this.minAreaSeedRegions = minAreaSeedRegions;
	}

	/**
	 *
	 * @return
	 */
	public XylemGrower.Neighbourhood getNeighbourhood() {
		return neighbourhood;
	}

	/**
	 *
	 * @param nb
	 */
	public void setNeighbourhood( final XylemGrower.Neighbourhood nb ) {
		this.neighbourhood = nb;
	}

	/**
	 * Returns a {@code MTBImageByte} image after growing.
	 * <p/>
	 * @return
	 */
	public MTBImageByte getGrownRegions() {
		return grownRegions;
	}

	/**
	 *
	 * @param imageByte
	 */
	public void setGrownRegions( final MTBImageByte imageByte ) {
		this.grownRegions = imageByte;
	}

	/**
	 *
	 * @return
	 */
	public double getSatTresh() {
		return satThresh;
	}

	/**
	 *
	 * @param satTresh
	 */
	public void setSatTresh( final double satTresh ) {
		this.satThresh = satTresh;
	}

	/**
	 *
	 * @return
	 */
	public MTBImageByte getSeedRegions() {
		return seedRegions;
	}

	/**
	 *
	 * @param seedRegions
	 */
	public void setSeedImage( MTBImageByte seedRegions ) {
		this.seedRegions = seedRegions;
	}

	/**
	 * The Intensity/Brighntness/Value-Parameter (depending on the used input
	 * image type).
	 * <p/>
	 * @return The Intensity/Brighntness/Value-Parameter (depending on the used
	 * input image type).
	 */
	public double getXThresh() {
		return xThresh;
	}

	/**
	 * Set the Intensity/Brighntness/Value-Parameter (depending on the used
	 * input image type).
	 * <p/>
	 * @param xThresh The threshold for the intensity/brightness/value.
	 */
	public void setXThresh( final double xThresh ) {
		this.xThresh = xThresh;
	}

	/**
	 *
	 * @return
	 */
	public int getOpeningSESize() {
		return openingSESize;
	}

	/**
	 *
	 * @param size
	 */
	public void setOpeningSESize( final int size ) {
		if( size < MIN_SE_SIZE ) {
			throw new IllegalArgumentException( "Min size for SE is " + MIN_SE_SIZE + "  , you provide : " + size );
		}
		this.openingSESize = size;
	}

	public int getMinRegionArea() {
		return minAreaPostProcessing;
	}

	public void setMinRegionArea( final int minAreaPostProcessing ) {
		if( minAreaPostProcessing < MIN_SE_SIZE ) {
			throw new IllegalArgumentException( "min region area got to be >= " + DEFAULT_minAreaPostProcessing + ", but is : " + minAreaPostProcessing );
		}
		this.minAreaPostProcessing = minAreaPostProcessing;
	}

	@Override
	public String getDocumentation() {
		return "This operator grows Xylem regions in microscopic sections of woods.\r\n" + 
				"\r\n" + 
				" <p>\r\n" + 
				" Prerequisite is \r\n" + 
				" HS[X]-image (hue, saturation, intensity/value/brightness) as the input\r\n" + 
				" and some thresholds to control the growing process and\r\n" + 
				" an initial segmentation of xylem regions supplied as a binary image.\r\n" + 
				" <p>\r\n" + 
				" All three channels of the hs[x] image are assume to be a byte image and values\r\n" + 
				" in the range <code>0 - 255</code>.\r\n" + 
				" <p>\r\n" + 
				" The initial regions are eroded to get seed regions, where we\r\n" + 
				" determine the mean-value of the hue and/or saturation and/or I/V/B-channel.\r\n" + 
				" <p>\r\n" + 
				" Subsequently the region are grown via region growing. We compare each pixel of the\r\n" + 
				" contour of those seed regions with the pixel of the background beside them,\r\n" + 
				" either in a 4-neighbourhood or an 8-neighbourhood.\r\n" + 
				" <p>\r\n" + 
				" This is repeated until the list of uninspected pixel is not empty.\r\n" + 
				" Afterwards we may do some post processing. E.G. calculate gradient\r\n" + 
				" informations on the found regions to split regions who contain more then one\r\n" + 
				" xylem.\r\n" + 
				"\r\n" + 
				"<h3> Parameters</h3>\r\n" + 
				"<ul>\r\n" + 
				"<li>Size of structuring element for ersion to compute seed regions</li>\r\n" + 
				"<li>Minimal size of a region for further erosion of seed regions</li>\r\n" + 
				"<li>The method for growing the region</li>\r\n" + 
				"<li>Hue channel threshold</li>\r\n" + 
				"<li>Saturation channel threshold</li>\r\n" + 
				"<li>Intensity channel threshold</li>\r\n" + 
				"<li>The neighbourhood for the pixel to visit</li>\r\n" + 
				"<li>Size of SE for opening (post processing)</li>\r\n" + 
				"</ul>\r\n" + 
				"\r\n" + 
				"<h3>Results</h3>\r\n" + 
				"As a result the detected Xylem regions are returned as a binary image and\r\n" + 
				"a table of features for each Xylem regions is created.\r\n" + 
				"<p>\r\n" + 
				"Optionally intermediate results may be returned.\r\n";
	}
	
}

