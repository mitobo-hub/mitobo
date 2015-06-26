package de.unihalle.informatik.MiToBo.apps.xylem;

import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.MiToBo.color.conversion.RGBToHSXConverter;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageByte;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageRGB;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;
import de.unihalle.informatik.MiToBo.gui.MTBTableModel;
import de.unihalle.informatik.MiToBo.segmentation.regions.convert.Region2DSetFromLabelimage;
import de.unihalle.informatik.MiToBo.tools.system.UserTime;
import de.unihalle.informatik.MiToBo.visualization.drawing.DrawRegion2DSet;
import de.unihalle.informatik.MiToBo.visualization.drawing.DrawRegion2DSet.DrawType;

/** This operator implements xylem detection in RGB or HSX images.
 * 
 * @author posch
 */
@ALDAOperator( genericExecutionMode = ALDAOperator.ExecutionMode.ALL,
               level = ALDAOperator.Level.APPLICATION )
public class XylemDetector extends MTBOperator {

	/**
	 * RGB-Color Channel Image.
	 */
	@Parameter( label = "XylemImageRGB", required = false,
			dataIOOrder = 0,
			direction = Parameter.Direction.IN,
			description = "Xylem image as RGB-image" )
	private transient MTBImageRGB xylemRGBImage = null;

	/**
	 * HS(X)-Color Channel Image.
	 * <p/>
	 * Should be of type {
	 * <p/>
	 * @codeMTBImageByte }, containing three color channel( Hue, Saturation and
	 * Intensity/Brightness/Value).
	 */
	@Parameter( label = "XylemImageHSX", required = false,
			dataIOOrder = 1,
			direction = Parameter.Direction.IN,
			description = "Xylem image as HSX-image" )
	private transient MTBImageByte xylemHsxImage = null;

	@Parameter( label = "XylemcreateContourOverlayImageHSX", required = false,
			dataIOOrder = 3,
			direction = Parameter.Direction.IN,
			description = "Create overlay image with xylem contours on initial image" )
	private Boolean createContourOverlay = false;

	// ---------------------------------------
	// Parameters for intial segmentation
	
    /**
     * Size of the structuring element for Opening.
     */
    @Parameter( label = "SizeOpening (InitSeg)", required = false,
                dataIOOrder = 3,
                direction = Parameter.Direction.IN,
                description = "The size of the structuring element for the opening (initial segmentation)",
                mode=Parameter.ExpertMode.ADVANCED)
    private int seOpeningSizeInitSeg = XylemInitialSegmentation.DEFAULT_seOpeningSize;

    /**
     * Minimal area (in pixels) of a region required after opening.
     * <p/>
     * If the size of the region is <code>< minAreaAfterOpening</code> the region
     * will be removed by the
     * {@linkplain de.unihalle.informatik.MiToBo.morphology.ComponentPostprocess}
     * operator.
     */
    @Parameter( label = "MinRegionSize (InitSeg)", required = false, dataIOOrder = 4,
                direction = Parameter.Direction.IN,
                description = "The minimal size of the region required after opening (initial segmentation)",
                mode=Parameter.ExpertMode.ADVANCED)
    private int minAreaAfterOpeningInitSeg = XylemInitialSegmentation.DEFAULT_minAreaAfterOpening;

    /**
     * Minimal compactness of a region required after opening.
     * <p/>
     * If the compactness of the region is <code> < minCompactness</code> the
     * region will be removed by the
     * {@linkplain de.unihalle.informatik.MiToBo.morphology.ComponentPostprocess}
     * operator.
     */
    @Parameter( label = "MinCompactness (InitSeg)", required = false, dataIOOrder = 5,
                direction = Parameter.Direction.IN,
                description = "The minimal compactness of the region required after opening (initial segmentation)" ,
                mode=Parameter.ExpertMode.ADVANCED)
    private double minCompactnessInitSeg = XylemInitialSegmentation.DEFAULT_minCompactness;
    
	// ---------------------------------------
    // Parameters for growing
	/**
	 * The size of the structuring element for erosion to compute seed regions. If {@code ErodeMode} is
	 * set to {@linkplain ErodeMode#DYNAMIC Dynamic}, you should use a small
	 * size. Otherwise use a size >= 20.
	 */
	@Parameter( label = "SizeErosion (growing)", required = false, dataIOOrder = 6,
			direction = Parameter.Direction.IN,
			description = "Size of structuring element for ersion to compute seed regions (Growing)",
			mode=ExpertMode.ADVANCED)
	private int erodeSize = XylemGrower.DEFAULT_erodeSize;

	/**
	 * The minimum size of a {@code MTBRegion2D} to further erode when computing seed regions.
	 */
	@Parameter( label = "MinAreaToErode (growing)", required = false,
			dataIOOrder = 7,
			direction = Parameter.Direction.IN,
			description = "Minimal size of a region for further erosion of seed regions (Growing)",
			mode=ExpertMode.ADVANCED )
	private int minAreaSeedRegions = XylemGrower.DEFAULT_minAreaSeedRegions;

	/**
	 * The algorithm we use to link new pixels to the region.
	 */
	@Parameter( label = "GrowingMode", required = true, dataIOOrder = 8,
			direction = Parameter.Direction.IN,
			description = "The method for growing the region" )
	private XylemGrower.GrowingMode linkageMode = XylemGrower.DEFAULT_linkageMode;

	/**
	 * The hue threshold.
	 * <p/>
	 * Indicates whether a new pixel is part of a region or not, depending on
	 * growing algorithm.
	 */
	@Parameter( label = "HueThreshold", required = true, dataIOOrder = 9,
			direction = Parameter.Direction.IN,
			description = "Intensity channel threshold" )
	private double hueThresh = XylemGrower.DEFAULT_hueThresh;

	/**
	 * The saturation threshold.
	 * <p/>
	 * Indicates whether a new pixel is part of a region or not, depending on
	 * growing algorithm.
	 */
	@Parameter( label = "SaturationThreshold", required = true,
			dataIOOrder = 10,
			direction = Parameter.Direction.IN,
			description = "Intensity channel threshold" )
	private double satThresh = XylemGrower.DEFAULT_satThresh;

	/**
	 * The Intensity/Brightness/Value threshold.
	 * <p/>
	 * Indicated whether a pixel is part of a region. Since we calculate the
	 * derivation of the mean of the regions value, this can be set to arbitrary
	 * values > 0.
	 * <p/>
	 */
	@Parameter( label = "XThreshold",
			required = true, dataIOOrder = 11,
			direction = Parameter.Direction.IN,
			description = "Intensity channel threshold" )
	private double xThresh = XylemGrower.DEFAULT_xThresh;

	/**
	 * The neighbourhood mode to inspect new pixel.
	 */
	@Parameter( label = "Neighbourhood used (growing)", required = false, dataIOOrder = 12,
			direction = Parameter.Direction.IN,
			description = "The neighbourhood for the pixel to visit (Growing)" ,
			mode=ExpertMode.ADVANCED)
	private XylemGrower.Neighbourhood neighbourhood = XylemGrower.DEFAULT_neighbourhood;

	/**
	 * The minimum size of a {@code MTBRegion2D} before erosion.
	 */
	@Parameter( label = "SizeOpening (postprocessing growing)", required = true,
			dataIOOrder = 13,
			direction = Parameter.Direction.IN,
			description = "Size of SE for opening (post processing of growing)" )
	private int openingSESize = XylemGrower.DEFAULT_openingSESize;

	/**
	 * The minimum size of a {@code MTBRegion2D} to not be removed (after
	 * opening the grown regions).
	 */
	@Parameter( label = "MinArea (postprocessing growing)", required = true,
			dataIOOrder = 14,
			direction = Parameter.Direction.IN,
			description = "Minimal area of a region (post processing of growing)" )
	private int minAreaPostProcessing = XylemGrower.DEFAULT_minAreaPostProcessing;

	// ---------------------------------------
    // Output
	
	/**
	 * Final xylem regions after postprocessing.
	 */
	@Parameter( label = "XylemRegions", 			
			dataIOOrder = 1,
			direction = Parameter.Direction.OUT,
			description = "Final xylem regions after postprocessing" )
	private transient MTBImageByte resultXylemRegions;

	/**
	 * A table containing information for each region 
	 */
	@Parameter(label = "ResultTable", required = true, 
			direction = Parameter.Direction.OUT, 
					dataIOOrder = 2,
			supplemental = false, description = "Table containing the features for each xylem")
	private MTBTableModel resultsTable = null;

	/**
	 * Initial segmentation of xylem regions computed of supplied
	 */
	@Parameter( label = "InitalSegmentation", 
			dataIOOrder = 3,
			direction = Parameter.Direction.OUT,
			description = "Initial segmentation as a binary image" )
	private transient MTBImageByte initialSegmentationOut = null;



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

	/**
	 * Resulting regions from growing.
	 * <p/>
	 * Each region is identified by its own id ( possible not unique ).
	 */
	@Parameter( label = "XylemRegionOverlay", required = false,
			dataIOOrder = 6,
			supplemental = true,
			direction = Parameter.Direction.OUT,
			description = "Xylem contours overlay in original image to segment" )
	private transient MTBImage xylemRegionsOverlay = null;
	/**
	 * Consumed time in seconds where <code>TimingMode</code> states if
	 * this reflect user or real time.
	 */
	@Parameter( label = "Timing", 
			dataIOOrder =10,
			supplemental = true,
			direction = Parameter.Direction.OUT,
			description = "Consumed time in seconds" )
	private Double timing;

	/**
	 * Timing mode, may be user time or real time.
	 */
	@Parameter( label = "TimingMode", 
			dataIOOrder = 11,
			supplemental = true,
			direction = Parameter.Direction.OUT,
			description = "Timing Mode of timing" )
	private String timingMode;


	@Override
	public void validateCustom() throws ALDOperatorException {
		if( xylemHsxImage == null && xylemRGBImage == null) {
			throw new ALDOperatorException( ALDOperatorException.OperatorExceptionType.VALIDATION_FAILED,
					"Both RGB and HSX xylem input images are null!" );
		}
	}

	/**
	 * Default Constructor.
	 * <p/>
	 * @throws ALDOperatorException
	 */
	public XylemDetector() throws ALDOperatorException {
		// empty
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
		
		// timing
		final UserTime timer = new UserTime( true );

		// do RGB to HSX conversion or recover x-channel from hsx input image
		MTBImageByte xylemGrayImage = null;
		if ( xylemHsxImage == null ) {
			RGBToHSXConverter rgbConverterOp = new RGBToHSXConverter( xylemRGBImage);
			rgbConverterOp.runOp();
			xylemHsxImage = (MTBImageByte) rgbConverterOp.getResultMTBImage();
			xylemGrayImage = (MTBImageByte) rgbConverterOp.getXMTBImg();
		} else {
			// extract x-channel
			// assume three Z channels or three time channels
			if ( xylemHsxImage.getSizeZ() == 3) {
				xylemGrayImage = 
						(MTBImageByte) xylemHsxImage.getSlice( RGBToHSXConverter.ColorChannel.INTENSITY.getIndex(), 0, 0);
			} else {
				xylemGrayImage = (MTBImageByte) xylemHsxImage.getImagePart(
						0, 0, 0, RGBToHSXConverter.ColorChannel.INTENSITY.getIndex(), 0, 
						xylemHsxImage.getSizeX(), 
						xylemHsxImage.getSizeY(), 
						xylemHsxImage.getSizeZ(), 
						1, 
						xylemHsxImage.getSizeC());
			}
		}
		
		// initial segmentation
		XylemInitialSegmentation initSegOp = new XylemInitialSegmentation();
		initSegOp.setXylemImage(xylemGrayImage);
		initSegOp.setSeOpeningSize(seOpeningSizeInitSeg);
		initSegOp.setMinAreaAfterOpening(minAreaAfterOpeningInitSeg);
		initSegOp.setMinCompactness(minCompactnessInitSeg);
		
		initSegOp.runOp();

		this.initialSegmentationOut = initSegOp.getInitSegImageByte();
		
		// region growing
		XylemGrower growerOp = new XylemGrower();
		growerOp.setXylemImage(xylemHsxImage);
		growerOp.setInitalSegmentation( this.initialSegmentationOut);
		growerOp.setLinkageMode(this.linkageMode);
		growerOp.setErodSize(erodeSize);
		growerOp.setminAreaSeedRegions(minAreaSeedRegions);
		growerOp.setHueThresh(hueThresh);
		growerOp.setSatTresh(satThresh);
		growerOp.setXThresh(xThresh);
		growerOp.setNeighbourhood(neighbourhood);
		growerOp.setOpeningSESize(openingSESize);
		
		growerOp.runOp();
		
		this.resultXylemRegions = growerOp.getResultXylemRegions();
		this.resultsTable = growerOp.getResultsTable();
		
		this.seedRegions = growerOp.getSeedRegions();
		this.grownRegions = growerOp.getGrownRegions();
		
		// timing
		this.timing = timer.getElapsedTime();
		this.timingMode = timer.getOperation();
		
		if ( createContourOverlay ) {
			this.xylemRegionsOverlay = createOverlay();
		}
		}

	private MTBImage createOverlay() throws ALDOperatorException, ALDProcessingDAGException {
		Region2DSetFromLabelimage op = new Region2DSetFromLabelimage();
		op.setLabelImage(resultXylemRegions);
		op.runOp(HidingMode.HIDE_CHILDREN);
		MTBRegion2DSet regions = op.getRegionSet();	
		
		DrawRegion2DSet drawOp = new DrawRegion2DSet();
		drawOp.setDrawType( DrawType.CONTOURS);
		drawOp.setInputRegions( regions);
		drawOp.setImageType(MTBImageType.MTB_RGB);


		if ( xylemRGBImage != null ) {
			drawOp.setTargetImage( xylemRGBImage);
		} else {
			drawOp.setTargetImage( xylemHsxImage);
		}
		
		drawOp.runOp(HidingMode.HIDE_CHILDREN);
		return drawOp.getResultImage();

	}
}

/*BEGIN_MITOBO_ONLINE_HELP
<p><a target="_blank" href="http://www2.informatik.uni-halle.de/agprbio/mitobo//api/de/unihalle/informatik/MiToBo/apps/xylem/XylemDetector.html">API</a></p>
 
This operator detects Xylem regions in microscopic sections of woods.
Detection is accompished in two phases
<ul>
<li> Initial segmentation </li>
<li> Region growing </li>
</ul>

<h3>Input</h3>
As input a RGB or HSX image of a microscopic section is required.
If both images are given, the RGB image is ignored.

<h3>Results</h3>
As a result the detected Xylem regions are returned as a binary image and
a table of features for each Xylem regions is created.
<p>
Optionally contours of the detected Xylem regions may be overlayed onto the input image
and intermediate results be return, as well as timing information.
<p>
For a description of parameters see the online help of
the operators <I>XylemInitialSegmentation</I> and <I>XylemGrower</I>.

END_MITOBO_ONLINE_HELP*/
