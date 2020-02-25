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

package de.unihalle.informatik.MiToBo.apps.xylem;

import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.Alida.operator.events.ALDOperatorExecutionProgressEvent;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageByte;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;
import de.unihalle.informatik.MiToBo.morphology.BasicMorphology;
import de.unihalle.informatik.MiToBo.morphology.ComponentPostprocess;
import de.unihalle.informatik.MiToBo.segmentation.thresholds.CalcGlobalThreshOtsu;
import de.unihalle.informatik.MiToBo.segmentation.thresholds.ImgThresh;

/**
 * Purpose of this operator is to make an initial segmentation of Xylem Cell
 * Images.
 * <p>
 * The input image is a grey level image of the Xylem cells. The output image is
 * a binary image of the initial segmentation.
 * <ol>
 * <li>Calculate otsu threshold</li>
 * <li>Binarize the greyscaled image based on the otsu threshold</li>
 * <li>Open the bin-image</li>
 * <li>Remove small components</li>
 * <li>Erode the image</li>
 * <li>Remove small components</li>
 * <li>Remove uncompact regions</li>
 * </ol>
 * </p>
 * <p>
 * @author Tim Langhammer, Halle/Saale, Europe
 * @author posch
 * <tim.langhammer@student.uni-halle.de>
 */
@ALDAOperator( genericExecutionMode = ALDAOperator.ExecutionMode.ALL,
               level = ALDAOperator.Level.APPLICATION,
               shortDescription="Makes an initial segmentation of Xylem Cell Images")
public class XylemInitialSegmentation extends MTBOperator {

	public final static int DEFAULT_seOpeningSize = 9;
	public final static int DEFAULT_minAreaAfterOpening = 600;
	public final static double DEFAULT_minCompactness = 0.24D;
	
    /**
     * The Xylem Cell microscope {@linkplain MTBImageByte} grey level image.
     */
    @Parameter( label = "XylemCellGrayImage", required = true,
                dataIOOrder = 1,
                direction = Parameter.Direction.IN,
                description = "The Xylem Cell microscope grey level image" )
    private transient MTBImage xylemGrayedImage = null;

    /**
     * Size of the structuring element for Opening.
     */
    @Parameter( label = "SizeStructuringElementOpening", required = false,
                dataIOOrder = 2,
                direction = Parameter.Direction.IN,
                description = "The size of the structuring element for the opening",
                mode=Parameter.ExpertMode.ADVANCED)
    private int seOpeningSize = DEFAULT_seOpeningSize;

    /**
     * Minimal area (in pixels) of a region required after opening.
     * <p/>
     * If the size of the region is <code>< minAreaAfterOpening</code> the region
     * will be removed by the
     * {@linkplain de.unihalle.informatik.MiToBo.morphology.ComponentPostprocess}
     * operator.
     */
    @Parameter( label = "MinRegionSize", required = false, dataIOOrder = 3,
                direction = Parameter.Direction.IN,
                description = "The minimal size of the region required after opening",
                mode=Parameter.ExpertMode.ADVANCED)
    private int minAreaAfterOpening = DEFAULT_minAreaAfterOpening;

    /**
     * Minimal compactness of a region required after opening.
     * <p/>
     * If the compactness of the region is <code> < minCompactness</code> the
     * region will be removed by the
     * {@linkplain de.unihalle.informatik.MiToBo.morphology.ComponentPostprocess}
     * operator.
     */
    @Parameter( label = "MinCompactness", required = false, dataIOOrder = 4,
                direction = Parameter.Direction.IN,
                description = "The minimal compactness of the region required after opening" ,
                mode=Parameter.ExpertMode.ADVANCED)
    private double minCompactness = DEFAULT_minCompactness;
    
	/**
	 * Should time of processing be printed to stdout
	 */
    @Parameter( label = "PrintTiming", required = false, dataIOOrder = 4,
            direction = Parameter.Direction.IN,
            description = "Print timing information to standard output" ,
            mode=Parameter.ExpertMode.ADVANCED)
	private boolean printTiming = false;

    /**
     * The resulting binary image after the initial segmentation.
     */
    @Parameter( label = "InitialSegmentation", required = true,
                dataIOOrder = 1,
                direction = Parameter.Direction.OUT,
                description = "The resulting initial segmentation as a binary image" )
    private transient MTBImageByte initSegImageByte = null;

    /**
     * Size of the structuring element for the erosion.
     */
    private int seErosionSize = 9;
    
   /**
     * The shape of structuring element for the opening.
     */
    private BasicMorphology.maskShape shapeForOpening = BasicMorphology.maskShape.CIRCLE;

    /**
     * The shape of structuring element for the Erosion.
     */
    private BasicMorphology.maskShape shapeForErosion = BasicMorphology.maskShape.SQUARE;

    /**
     * The minimum size of regions after the erosion to get preserved.
     */
    private int minAreaAfterErosion = 300;

   /**
     * Default constructor.
     * 
     * @throws ALDOperatorException
     */
    public XylemInitialSegmentation() throws ALDOperatorException {
        // empty constructor
    }

    /**
     *
     * @param grayScaleXylemImage
     * @throws ALDOperatorException
     */
    public XylemInitialSegmentation( final MTBImageByte grayScaleXylemImage ) throws ALDOperatorException {
        this.xylemGrayedImage = grayScaleXylemImage;
    }

    @Override
    protected void operate() throws ALDOperatorException, ALDProcessingDAGException {
		this.fireOperatorExecutionProgressEvent( new ALDOperatorExecutionProgressEvent(this,
                "XylemInitialSegmentation"));

        // Otsu Operator
        final MTBImage grayedImage = getXylemImage();
        final CalcGlobalThreshOtsu cgto = new CalcGlobalThreshOtsu( grayedImage );
        cgto.runOp( HidingMode.HIDE_CHILDREN);

        // Treshold operator
        final ImgThresh imgThresh = new ImgThresh();
        imgThresh.setInputImage( grayedImage );
        imgThresh.setThreshold( cgto.getOtsuThreshold().getValue() );
        imgThresh.runOp( HidingMode.HIDE_CHILDREN);

        MTBImageByte binImage = ( MTBImageByte ) imgThresh.getResultImage();

        // ----------------------------------------- Opening -------------------------------------------------- 
        BasicMorphology bm  = new BasicMorphology();
        bm.setInImg( binImage );
        bm.setMode( BasicMorphology.opMode.OPEN );
        bm.setMask( getShapeForOpening(), getSeOpeningSize() );
        bm.runOp( HidingMode.HIDE_CHILDREN);

        binImage = ( MTBImageByte ) bm.getResultImage();
        
        //  ----------------------------------------- Remove Small Comp  --------------------------------------------------
        ComponentPostprocess cp = new ComponentPostprocess( binImage,
        		ComponentPostprocess.ProcessMode.ERASE_SMALL_COMPS );
        cp.setMinimalComponentSize( getMinAreaAfterOpening() );
        cp.runOp( HidingMode.HIDE_CHILDREN);

        binImage = (MTBImageByte)cp.getResultImage();
        // ----------------------------------------- Erosion -------------------------------------------------- 
        bm = new BasicMorphology();
        bm.setInImg( binImage );
        bm.setMode( BasicMorphology.opMode.ERODE );
        bm.setMask( getShapeForErosion(), getSeErosionSize() );
        bm.runOp( HidingMode.HIDE_CHILDREN);

        binImage = ( MTBImageByte ) bm.getResultImage();
        // ---------------------------------- Remove Small Comp -------------------------------------------- 
        cp = new ComponentPostprocess( binImage,
        		ComponentPostprocess.ProcessMode.ERASE_SMALL_COMPS );
        cp.setMinimalComponentSize( getMinAreaAfterErosion() );
        cp.runOp( HidingMode.HIDE_CHILDREN);

        binImage = (MTBImageByte)cp.getResultImage();
        // ----------------------------------------- Remove Uncompact Comp------------------------------------------------- 
        cp = new ComponentPostprocess( binImage,
        		ComponentPostprocess.ProcessMode.EREASE_NON_COMPACT_COMPS );
        cp.setCompactnessThreshold( getMinCompactness() );
        cp.runOp( HidingMode.HIDE_CHILDREN);

        MTBImageByte resultImg = (MTBImageByte)cp.getResultImage();
        resultImg.setTitle("initialXylemRegions");
        setInitSegImageByte( resultImg);
    }

    // GETTER and SETTER
    /**
     * Return the input xylem image.
     * 
     * @return
     */
    public MTBImage getXylemImage() {
        return xylemGrayedImage;
    }

    /**
     * Set the input xylem image - this must be an 8bit image.
     * <p>
     * @param xylemImage
     */
    public void setXylemImage( MTBImage xylemImage ) {
        this.xylemGrayedImage = xylemImage;
    }

    /**
     * Return the resulting initial segmentation image.
     * <p>
     * @return
     */
    public MTBImageByte getInitSegImageByte() {
        return initSegImageByte;
    }

    /**
     *
     * @param initSegImageByte
     */
    public void setInitSegImageByte( final MTBImageByte initSegImageByte ) {
        this.initSegImageByte = initSegImageByte;
    }

    public int getSeOpeningSize() {
        return seOpeningSize;
    }

    public void setSeOpeningSize( final int seOpeningSize ) {
        this.seOpeningSize = seOpeningSize;
    }

    public int getSeErosionSize() {
        return seErosionSize;
    }

    public void setSeErosionSize( final int seErosionSize ) {
        this.seErosionSize = seErosionSize;
    }

    public int getMinAreaAfterOpening() {
        return minAreaAfterOpening;
    }

    public void setMinAreaAfterOpening( final int minAreaAfterOpening ) {
        this.minAreaAfterOpening = minAreaAfterOpening;
    }

    public int getMinAreaAfterErosion() {
        return minAreaAfterErosion;
    }

    public void setMinAreaAfterErosion( final int minAreaAfterErosion ) {
        this.minAreaAfterErosion = minAreaAfterErosion;
    }

    public double getMinCompactness() {
        return minCompactness;
    }

    public void setMinCompactness( final double minCompactness ) {
        this.minCompactness = minCompactness;
    }

    public BasicMorphology.maskShape getShapeForOpening() {
        return shapeForOpening;
    }

    public void setShapeForOpening( final BasicMorphology.maskShape shapeForOpening ) {
        this.shapeForOpening = shapeForOpening;
    }

    public BasicMorphology.maskShape getShapeForErosion() {
        return shapeForErosion;
    }

    public void setShapeForErosion( final BasicMorphology.maskShape shapeForErosion ) {
        this.shapeForErosion = shapeForErosion;
    }
    
    @Override
    public String getDocumentation() {
    	return "This operator computes a initial segmentation of Xylem regions in microscopic sections of woods.\n" + 
    			"Segmentation is accompished in two phases\n" + 
    			"<ul>\n" + 
    			" <li>Calculate otsu threshold</li>\n" + 
    			" <li>Binarize the greyscaled image based on the otsu threshold</li>\n" + 
    			" <li>Open the bin-image</li>\n" + 
    			" <li>Remove small components</li>\n" + 
    			" <li>Erode the image</li>\n" + 
    			" <li>Remove small components</li>\n" + 
    			" <li>Remove uncompact regions</li>\n" + 
    			"\n" + 
    			"</ul>\n" + 
    			"\n" + 
    			"<br>\n" + 
    			"As input a gray value image of a microscopic section is required.\n" + 
    			"\n" + 
    			"<h3>Parameters</h3>\n" + 
    			"<ul>\n" + 
    			"<li>The size of the structuring element for the opening</li>\n" + 
    			"<li>The minimal size of the region required after opening</li>\n" + 
    			"<li>The minimal compactness of the region required after opening</li>\n" + 
    			"</ul>\n" + 
    			"\n" + 
    			"<h3>Results</h3>\n" + 
    			"The initial Xylem regions are returned as abinary image\n";
    }

}
