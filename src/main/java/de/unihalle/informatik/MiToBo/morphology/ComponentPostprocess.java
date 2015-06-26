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

package de.unihalle.informatik.MiToBo.morphology;

import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.ExecutionMode;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.Parameter.Direction;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException.OperatorExceptionType;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.MiToBo.core.datatypes.*;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBBorder2D.BorderConnectivity;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageByte;
import de.unihalle.informatik.MiToBo.core.operator.*;
import de.unihalle.informatik.MiToBo.segmentation.contours.extraction.BordersOnLabeledComponents;
import de.unihalle.informatik.MiToBo.segmentation.contours.extraction.BordersOnLabeledComponents.BorderType;
import de.unihalle.informatik.MiToBo.segmentation.regions.labeling.*;
import ij.process.BinaryProcessor;
import ij.process.ByteProcessor;

import java.awt.geom.*;
import java.util.*;

/**
 * Methods for post-processing components/pixels in binary images.
 * <p>
 * Thresholding algorithms yield binary images with distinct connected
 * components of foreground pixels, i.e. regions. The routines of this 
 * operator provide functionality for post-processing such binary 
 * (unlabeled) component images. Exemplary post-processing steps might 
 * include linking/merging adjacent components or changing a component's 
 * morphological shape.
 * <p>
 * Note that the different processing modes may require different 
 * parameter settings. These settings have to be done explicitly by 
 * calling the corresponding setter-functions the class provides. 
 * If the parameters are not set by the user default values are used.
 * <p/>
 * @author moeller
 */
@ALDAOperator( genericExecutionMode = ExecutionMode.ALL, 
	level = Level.APPLICATION )
public class ComponentPostprocess extends MTBOperator {

	/**
	 * Processing mode identifiers.
	 */
	public static enum ProcessMode {
		/**
		 * Delete components below a size threshold from the image.
		 * <p>
		 * Set threshold with method {@link setMinimalComponentSize}.
		 */
		ERASE_SMALL_COMPS,
		/**
		 * Delete components above a size threshold from the image.
		 * <p>
		 * Set threshold with method {@link setMaximalComponentSize}.
		 */
		ERASE_LARGE_COMPS,
		/**
		 * Link adjacent components with a line if distance below threshold.
		 * <p>
		 * Set distance with method {@link setMaximalComponentDistance}.
		 */
		LINK_ADJ_COMPS,
		/**
		 * Link adjacent pixels if distance below threshold.
		 * <p>
		 * Set distance with method {@link setMaximalComponentDistance}.
		 */
		LINK_ADJ_PIXELS,
		/**
		 * Perform Voronoi expansion for pixels close to components.
		 * <p>
		 * The operation works with label images as well as binary images.
		 * Note that the type of the result image equals the type of the input
		 * image which is processed.
		 * <p>
		 * Set the dilation mask size to apply with
		 * method {@link setMaximalVoronoiExpansionDistance}.
		 */
		VORONOI_EXPAND,
		/**
		 * Delete components with a rounded shape instead of an sustained shape.
		 * The roundness threshold is given by the user and is in range [0,1],
		 * 1 means a sustained region.
		 * <p>
		 * Set threshold with method {@link setRoundnessThreshold}.
		 */
		ERASE_ROUND_COMPS,
		/**
		 * Delete  non-compact regions.
		 * The compactness threshold is given by the user in a range 
		 * of [0,1]. 1 means perfect compact( circle ).
		 * For the definition of compactness see 
		 * {@link MTBRegion2D#getCircularity()}.
		 * <p/>
		 */
		EREASE_NON_COMPACT_COMPS;
	}
    /**
     * Binary input image.
     */
    @Parameter( label = "Binary Input Image", required = true, dataIOOrder = -10,
                direction = Direction.IN, description = "Binary input image" )
    private transient MTBImage inImg = null;
    /**
     * Binary result image.
     */
    @Parameter( label = "Result Image", required = true,
                direction = Direction.OUT, description = "Result image" )
    private transient MTBImage resultImg = null;
    /**
     * Minimal size of components in mode ERASE_SMALL_COMPS.
     */
    @Parameter( label = "Min. Component Size", required = false,
                direction = Direction.IN, dataIOOrder = -8,
                description = "Minimal component size, smaller ones are deleted." )
    private int minCompSize = 0;
    /**
     * Minimal size of components in mode ERASE_SMALL_COMPS.
     */
    @Parameter( label = "Max. Component Size", required = false,
                direction = Direction.IN, dataIOOrder = -8,
                description = "Maximal component size, larger ones are deleted." )
    private int maxCompSize = 0;
    /**
     * Maximal distance of adjacent components.
     */
    @Parameter( label = "Max. Distance of Componens", required = false,
                direction = Direction.IN, dataIOOrder = -6,
                description = "Maximum allowed distance of components to be linked." )
    private int maxCompDist = 0;
    /**
     * Voronoi expansion distance.
     */
    @Parameter( label = "Max. Voronoi Expansion Distance", required = false,
                direction = Direction.IN, dataIOOrder = -4,
                description = "Distance to which components are expanded in "
            + "Voronoi expansion mode." )
    private int maxVoronoiExpDist = 0;
    /**
     * Flag for using 8-neighborhood.
     */
    @Parameter( label = "Use Diagonal Neighbors?   ", required = false,
                direction = Direction.IN, dataIOOrder = -10,
                description = "Flag for considering 8-neighborhood instead of 4-NB" )
    private boolean diagonalNeighbors = false;
    /**
     * Roundness threshold for components.
     */
    @Parameter( label = "Roundness Threshold", required = false,
                direction = Direction.IN, dataIOOrder = -2,
                description = "Threshold for removing round components, "
            + " larger values enforce greater eccentricity of surviving components." )
    private double roundnessThreshold = 0.0;
    /**
     * Compactness threshold for components.
     */
    @Parameter( label = "Compactness Threshold", required = false,
                direction = Direction.IN, dataIOOrder = -7,
                description = "The theshold for removing non"
            + "compact components, larger values enforce more compact regions" )
    private double compactnessTreshold = 0.5D;
    /**
     * Processing mode.
     */
    @Parameter( label = "Process Mode", required = true,
                direction = Direction.IN,
                description = "Process Modus", dataIOOrder = -8 )
    private ProcessMode processMode = null;

    /**
     * Default constructor.
     * <p/>
     * @throws ALDOperatorException
     */
    public ComponentPostprocess() throws ALDOperatorException {
        // nothing to do here
    }

    /**
     * Default constructor.
     * <p>
     * Note that parameters for the chosen processing mode have to be
     * set explicitly. Parameters for other modes are ignored.
     * <p/>
     * @param img
     *            Image to work on.
     * @param pm
     *            Process mode of operator.
     * @throws ALDOperatorException
     *                              <p/>
     */
    public ComponentPostprocess( MTBImage img, ProcessMode pm )
            throws ALDOperatorException {
        this.inImg = img;
        this.processMode = pm;
    }

    /**
     * Default constructor. A new empty meta data object is initialized.
     * <p/>
     * @param img
     *                          Image to work on.
     * @param pm
     *                          Process mode of operator.
     * @param _minCompSize
     *                          Minimal region size for eliminating small comps.
     * @param _maxCompDist
     *                          Maximal region distance in linking.
     * @param maxVoroExpandDist
     *                          Max. distance of pixel considered in Voronoi expansion.
     * <p/>
     * Note: Depending on the process mode each time only one of the
     * parameters is actually in use.
     * <p/>
     * @throws ALDOperatorException
     * @deprecated
     */
    @Deprecated
    public ComponentPostprocess( MTBImage img, ProcessMode pm,
                                 int _minCompSize, int _maxCompDist, int maxVoroExpandDist )
            throws ALDOperatorException {
        this.inImg = img;
        this.processMode = pm;
        this.minCompSize = _minCompSize;
        this.maxCompDist = _maxCompDist;
        this.maxVoronoiExpDist = maxVoroExpandDist;
    }

    /**
     * Default constructor. A new empty meta data object is initialized.
     * <p/>
     * @param img
     * @param pm
     *                            Process mode of operator.
     * <p/>
     * @param _minCompSize
     * @param _maxCompDist
     * @param maxVoroExpandDist
     * @param _roundnessThreshold
     * @throws ALDOperatorException
     * @deprecated
     */
    @Deprecated
    public ComponentPostprocess( MTBImage img, ProcessMode pm,
                                 int _minCompSize, int _maxCompDist, int maxVoroExpandDist,
                                 double _roundnessThreshold ) throws ALDOperatorException {
        this.inImg = img;
        this.processMode = pm;
        this.minCompSize = _minCompSize;
        this.maxCompDist = _maxCompDist;
        this.maxVoronoiExpDist = maxVoroExpandDist;
        this.roundnessThreshold = _roundnessThreshold;
    }

    /**
     * Set minimal component size for erasing small components.
     * <p>
     * Only used in mode 'ERASE_SMALL_COMPS'.
     * <p/>
     * @param _minCompSize
     */
    public void setMinimalComponentSize( int _minCompSize ) {
        this.minCompSize = _minCompSize;
    }

    /**
     * Set maximal component size for erasing large components.
     * <p>
     * Only used in mode 'ERASE_LARGE_COMPS'.
     */
    public void setMaximalComponentSize( int _maxCompSize ) {
        this.maxCompSize = _maxCompSize;
    }

    /**
     * Set maximal component/pixel distance for linking.
     * <p>
     * Only used in modes 'LINK_ADJ_COMPS' and 'LINK_ADJ_PIXELS'.
     */
    public void setMaximalComponentDistance( int _maxCompDist ) {
        this.maxCompDist = _maxCompDist;
    }

    /**
     * Set radius of dilation mask in Voronoi expansion.
     * <p>
     * Only used in mode 'VORONOI_EXPAND'.
     * <p/>
     * @param _maxVoroExpandDist
     */
    public void setMaximalVoronoiExpansionDistance( int _maxVoroExpandDist ) {
        this.maxVoronoiExpDist = _maxVoroExpandDist;
    }

    /**
     * Set roundness threshold for eliminating round components.
     * <p>
     * Only used in mode 'ERASE_ROUND_COMPS'.
     * <p/>
     * @param _roundnessThreshold
     */
    public void setRoundnessThreshold( double _roundnessThreshold ) {
        this.roundnessThreshold = _roundnessThreshold;
    }

    /**
     * Set the compactness threshold for eliminating non compact components.
     * <p/>
     * @param _compactnessTrheshold
     */
    public void setCompactnessThreshold( final double _compactnessTrheshold ) {
        this.compactnessTreshold = _compactnessTrheshold;
    }

    /**
     * If true 8-neighborhood, i.e. diagonal neighbors, will be used in
     * mode 'VORONOI_EXPAND' for Voronoi dilation.
     * <p/>
     * @param value
     */
    public void setDiagonalNeighbors( boolean value ) {
        this.diagonalNeighbors = value;
    }

    /**
     * Get reference to the current input image.
     * <p/>
     * @return Input image to work on.
     */
    public MTBImage getInputImage() {
        return this.inImg;
    }

    /**
     * Get current process mode.
     * <p/>
     * @return Process mode of the component process.
     */
    public ProcessMode getProcessMode() {
        return this.processMode;
    }

    /**
     * Returns the minimal valid component size for component removal.
     * <p/>
     * @return
     */
    public int getMinimalComponentSize() {
        return this.minCompSize;
    }

    /**
     * Returns the maximal valid component size for component removal.
     * <p/>
     * @return
     */
    public int getMaximalComponentSize() {
        return this.maxCompSize;
    }

    /**
     * Returns the maximal component distance for linking.
     * <p/>
     * @return
     */
    public int getMaximalComponentDist() {
        return this.maxCompDist;
    }

    /**
     * Returns the maximal distance of pixels considered in Voronoi expansion.
     * <p/>
     * @return
     */
    public int getMaxVoronoiExpandDist() {
        return this.maxVoronoiExpDist;
    }

    /**
     * Get value of Parameter argument DiagonalNeighbors.
     * <p/>
     * @return value of DiagonalNeighbors
     */
    public boolean getDiagonalNeighbors() {
        return this.diagonalNeighbors;
    }

    /**
     * Returns the currently active roundness threshold.
     * <p/>
     * @return
     */
    public double getRoundnessThreshold() {
        return this.roundnessThreshold;
    }

    /**
     * Returns the actual value of compactness  threshold.
     * <p/>
     * @return
     */
    public double getCompactnessThreshold() {
        return this.compactnessTreshold;
    }

    /**
     * Get the result image after applying operator. Attention, reference might
     * be
     * null.
     * <p/>
     * @return Reference to result image.
     */
    public MTBImage getResultImage() {
        return this.resultImg;
    }
    // input image width and height
    private transient int width;
    private transient int height;
    // temporary variables needed during internal calculations
    private transient Vector<Vector<Point2D.Double>> endPointHash;

    @Override
    protected void operate() throws ALDOperatorException,
                                    ALDProcessingDAGException {

    	// all operations except Voronoi expansion require byte images
    	MTBImageByte byteImg = null;
    	if (this.getProcessMode() != ProcessMode.VORONOI_EXPAND) {
    		if ( this.inImg.getType().equals( MTBImage.MTBImageType.MTB_BYTE ) ) {
    			byteImg = ( MTBImageByte ) this.inImg;
    		}
    		else {
    			byteImg = ( MTBImageByte ) this.inImg.convertType(
    					MTBImage.MTBImageType.MTB_BYTE, true );
    		}
    	}

        MTBImage result = null;
        switch ( this.getProcessMode() ) {
            case ERASE_SMALL_COMPS:
                result = this.EraseSmallComponents( byteImg,
                                                    this.getMinimalComponentSize() );
                break;
            case ERASE_LARGE_COMPS:
                result = this.EraseLargeComponents( byteImg,
                                                    this.getMaximalComponentSize() );
                break;
            case LINK_ADJ_COMPS:
                result = this.LinkAdjacentComponents( byteImg );
                break;
            case LINK_ADJ_PIXELS:
                result = this.LinkAdjacentPixels( byteImg, this.getMaximalComponentDist() );
                break;
            case VORONOI_EXPAND:
                result = this.VoronoiExpandComponents( this.inImg,
                                                       this.getMaxVoronoiExpandDist() );
                break;
            case ERASE_ROUND_COMPS:
                result = this.EraseRoundComponents( byteImg, this.getRoundnessThreshold() );
                break;
            case EREASE_NON_COMPACT_COMPS:
                result = this.eraseNonCompactComponents( byteImg,
                                                         this.
                        getCompactnessThreshold() );
                break;
        }
        this.resultImg = result;

        if ( result == null ) {
            throw new ALDOperatorException( OperatorExceptionType.OPERATE_FAILED,
                                            "ComponentPostprocess.operate() failed: " + "Unknown process mode "
                    + this.getProcessMode() );
        }
    }

    /**
     * Function to remove small components from a binary image.
     * <p>
     * Small components are components that have a
     * size below the given threshold. Suitable for noise reduction in binary
     * images.
     * <p/>
     * @param binIP
     *                         binary input image
     * @param minComponentSize
     *                         minimum size of valid components
     * @return image with too small components erased
     * @throws ALDOperatorException
     */
    protected MTBImageByte EraseSmallComponents( MTBImageByte binIP,
                                                 int minComponentSize ) throws ALDOperatorException,
                                                                               ALDProcessingDAGException {

        this.width = binIP.getSizeX();
        this.height = binIP.getSizeY();

        // initialize result image
        MTBImageByte resultImage = ( MTBImageByte ) MTBImage.createMTBImage(
                this.width, this.height, 1, 1, 1, MTBImageType.MTB_BYTE );
        resultImage.fillWhite();

        // copy input image data
        resultImage.setImagePart( binIP, 0, 0, 0, 0, 0 );

        // label components in binary image
        LabelComponentsSequential regionLabler = new LabelComponentsSequential(
                binIP, getDiagonalNeighbors() );
        regionLabler.runOp( false );
        MTBRegion2DSet binRegions = regionLabler.getResultingRegions();

        for ( int i = 0; i < binRegions.size(); ++i ) {
            MTBRegion2D r = binRegions.elementAt( i );
            if ( r.getArea() < minComponentSize ) {
                // iterate over all region points...
                Vector<Point2D.Double> points = r.getPoints();
                for ( int j = 0; j < points.size(); ++j ) {

                    int x = ( int ) points.elementAt( j ).x;
                    int y = ( int ) points.elementAt( j ).y;

                    resultImage.putValueInt( x, y, 0 );
                }
            }
        }
        return resultImage;
    }

    /**
     * Function to remove large components from a binary image.
     * <p>
     * Large components are components that have a
     * size above the given threshold.
     * <p/>
     * @param binIP
     *                         binary input image
     * @param maxComponentSize
     *                         maximum size of valid components
     * @return image with too large components erased
     * @throws ALDOperatorException
     * @throws ALDProcessingDAGException
     */
    protected MTBImageByte EraseLargeComponents( MTBImageByte binIP,
                                                 int maxComponentSize ) throws ALDOperatorException,
                                                                               ALDProcessingDAGException {

        this.width = binIP.getSizeX();
        this.height = binIP.getSizeY();

        // initialize result image
        MTBImageByte resultImage = ( MTBImageByte ) MTBImage.createMTBImage(
                this.width, this.height, 1, 1, 1, MTBImageType.MTB_BYTE );
        resultImage.fillWhite();

        // copy input image data
        resultImage.setImagePart( binIP, 0, 0, 0, 0, 0 );

        // label components in binary image
        LabelComponentsSequential regionLabler = new LabelComponentsSequential(
                binIP, getDiagonalNeighbors() );
        regionLabler.runOp( false );
        MTBRegion2DSet binRegions = regionLabler.getResultingRegions();

        for ( int i = 0; i < binRegions.size(); ++i ) {
            MTBRegion2D r = binRegions.elementAt( i );
            if ( r.getArea() > maxComponentSize ) {
                // iterate over all region points...
                Vector<Point2D.Double> points = r.getPoints();
                for ( int j = 0; j < points.size(); ++j ) {

                    int x = ( int ) points.elementAt( j ).x;
                    int y = ( int ) points.elementAt( j ).y;

                    resultImage.putValueInt( x, y, 0 );
                }
            }
        }
        return resultImage;
    }

    /**
     * Remove components with small eccentricity.
     * <p/>
     * @param binImage		Input image.
     * @param threshold	Eccentricity threshold.
     * @return	Result image.
     * @throws ALDOperatorException
     * @throws ALDProcessingDAGException
     */
    protected MTBImageByte EraseRoundComponents( MTBImageByte binImage,
                                                 double threshold ) throws ALDOperatorException, ALDProcessingDAGException {
        this.width = binImage.getSizeX();
        this.height = binImage.getSizeY();
        // initialize result image
        MTBImageByte resultImage = ( MTBImageByte ) MTBImage.createMTBImage(
                this.width, this.height, 1, 1, 1, MTBImageType.MTB_BYTE );
        resultImage.fillBlack();
        // label components in binary image
        LabelComponentsSequential regionLabler = new LabelComponentsSequential(
                binImage, getDiagonalNeighbors() );
        regionLabler.runOp( false );
        MTBRegion2DSet binRegions = regionLabler.getResultingRegions();
        for ( int i = 0; i < binRegions.size(); ++i ) {
            MTBRegion2D r = binRegions.elementAt( i );
            if ( r.getEccentricity() >= threshold ) {
                // iterate over all region points...
                Vector<Point2D.Double> points = r.getPoints();
                for ( int j = 0; j < points.size(); ++j ) {
                    int x = ( int ) points.elementAt( j ).x;
                    int y = ( int ) points.elementAt( j ).y;
                    resultImage.putValueInt( x, y, 255 );
                }
            }
        }
        return resultImage;
    }

    /**
     * Function to remove all non compact/circular components of a binary image.
     * <p/>
     * @param binImage  The input image.
     * @param threshold The threshold of compactness
     * @return A binary image with all regions removed who got a compactness
     *         less <code>threshold</code>
     * @throws ALDOperatorException
     * @throws ALDProcessingDAGException
     */
    protected MTBImageByte eraseNonCompactComponents(
            final MTBImageByte binImage,
            final double threshold ) throws ALDOperatorException,
                                            ALDProcessingDAGException {
        this.width = binImage.getSizeX();
        this.height = binImage.getSizeY();

        // initialize result image
        final MTBImageByte resultImage = ( MTBImageByte ) MTBImage.
                createMTBImage(
                this.width, this.height, 1, 1, 1, MTBImageType.MTB_BYTE );
        resultImage.fillWhite();

        // copy input image data
        resultImage.setImagePart( binImage, 0, 0, 0, 0, 0 );

        // label components in binary image
        final LabelComponentsSequential regionLabler = new LabelComponentsSequential(
                binImage, getDiagonalNeighbors() );
        regionLabler.runOp( false );

        final MTBRegion2DSet binRegions = regionLabler.getResultingRegions();
        // inspect all regions
        for ( int i = 0; i < binRegions.size(); ++i ) {
            final MTBRegion2D r = binRegions.elementAt( i );
            // below threshold
            if ( r.getCircularity() < threshold ) {
                // set those pixel to background
                foregroundToBackground( r.getPoints(), resultImage );
            }
        }

        return resultImage;
    }

    /**
     * Function for linking adjacent components/regions in a binary image.
     * <p>
     * This function processes a binary image with foreground objects being
     * white.
     * Based on an initial component labeling step and subsequent
     * skeletonization
     * of resulting components, adjacent components are linked by a line of
     * width
     * one pixel in the result image.
     * <p>
     * The criterion for two components to be adjacent is defined based on the
     * distance of pairs of skeleton endpoints of both components. If there
     * exists
     * at least one pair of endpoints between two components with a distance
     * below
     * the given threshold, the components are linked. If there is more than one
     * pair of such points, the pair with the smallest distance is selected to
     * be
     * linked to each other.
     * <p/>
     * @param binIP input binary image with regions
     * @return binary image where adjacent components are linked by a line of
     *         width 1 pixel
     * @throws ALDOperatorException
     * @throws ALDProcessingDAGException
     */
    protected MTBImageByte LinkAdjacentComponents( MTBImageByte binIP )
            throws ALDOperatorException, ALDProcessingDAGException {

        this.width = binIP.getSizeX();
        this.height = binIP.getSizeY();

        // initialize result image
        MTBImageByte resultImage = ( MTBImageByte ) MTBImage.createMTBImage(
                this.width, this.height, 1, 1, 1, MTBImageType.MTB_BYTE );
        resultImage.fillWhite();

        // copy input image data
        resultImage.setImagePart( binIP, 0, 0, 0, 0, 0 );

        // label components in binary image
        LabelComponentsSequential regionLabler = new LabelComponentsSequential(
                binIP, getDiagonalNeighbors() );
        regionLabler.runOp( false );
        MTBRegion2DSet binRegions = regionLabler.getResultingRegions();

        // calculate skeletons of initial binary regions,
        // skeletonize function requires inverse input image (object = black)
        ByteProcessor bP = new ByteProcessor( this.width, this.height );
        for ( int y = 0; y < this.height; ++y ) {
            for ( int x = 0; x < this.width; ++x ) {
                if ( binIP.getValueInt( x, y ) > 0 ) {
                    bP.putPixel( x, y, 0 );
                }
                else {
                    bP.putPixel( x, y, 255 );
                }
            }
        }
        BinaryProcessor bbP = new BinaryProcessor( bP );
        bbP.skeletonize();

        // array where endpoints are marked
        int endPointArray[][] = new int[this.height][this.width];

        // pseudo-hashtable with all endpoints of binary regions:
        // remember the endpoints of all regions, indexed by region index
        this.endPointHash = new Vector<Vector<Point2D.Double>>();
        for ( int i = 0; i < binRegions.size(); ++i ) {
            this.endPointHash.add( new Vector<Point2D.Double>() );
        }

        // find all endpoints of all binary regions
        for ( int i = 0; i < binRegions.size(); ++i ) {
            MTBRegion2D r = binRegions.elementAt( i );
            Vector<Point2D.Double> points = r.getPoints();

            // iterate over all region points...
            for ( int j = 0; j < points.size(); ++j ) {

                int x = ( int ) points.elementAt( j ).x;
                int y = ( int ) points.elementAt( j ).y;

                // found a point of the skeleton?
                if ( bbP.getPixel( x, y ) == 0 ) {

                    // if so, count neighboring skeleton points
                    int nCount = 0;
                    for ( int dx = -1; dx <= 1; dx++ ) {
                        for ( int dy = -1; dy <= 1; ++dy ) {
                            if ( dx == 0 && dy == 0 ) {
                                continue;
                            }
                            if ( bbP.getPixel( x + dx, y + dy ) == 0 ) {
                                nCount++;
                            }
                        }
                    }
                    // if there is only one neighbor in 8-NB, (x,y) is endpoint
                    if ( nCount == 1 ) {
                        endPointArray[y][x] = 1;
                        // binEndPoints.add(new Point2D.Double(x,y));

                        // put endpoint in hash table
                        this.endPointHash.elementAt( i ).add( new Point2D.Double( x, y ) );
                    }
                }
            }
        } // end of for-loop: all binary regions

        // iterate over all regions and link them to neighbor regions
        for ( int i = 0; i < binRegions.size() - 1; ++i ) {

            // get endpoints of this region
            Vector<Point2D.Double> regionEndPoints = this.endPointHash.elementAt( i );

            // find close endpoints in neighboring regions
            for ( int j = i + 1; j < binRegions.size(); ++j ) {

                // get endpoints of neighbor region
                Vector<Point2D.Double> neighborEndPoints = this.endPointHash
                        .elementAt( j );

                double regionDist = this.maxCompDist;
                int closestPointID_region = -1;
                int closestPointID_neighbor = -1;

                for ( int m = 0; m < regionEndPoints.size(); ++m ) {
                    double rx = regionEndPoints.elementAt( m ).x;
                    double ry = regionEndPoints.elementAt( m ).y;

                    for ( int n = 0; n < neighborEndPoints.size(); ++n ) {
                        double nx = neighborEndPoints.elementAt( n ).x;
                        double ny = neighborEndPoints.elementAt( n ).y;

                        double dist = ( rx - nx ) * ( rx - nx ) + ( ry - ny ) * ( ry - ny );

                        // accept endpoints with distance below threshold
                        if ( dist < regionDist ) {
                            closestPointID_region = m;
                            closestPointID_neighbor = n;
                            regionDist = dist;
                        }
                    }
                }
                // check if regions are close to each other
                if ( regionDist < this.maxCompDist ) {
                    // if so, draw line between the two endpoints
                    Point2D.Double regPoint = regionEndPoints
                            .elementAt( closestPointID_region );
                    Point2D.Double neighbPoint = neighborEndPoints
                            .elementAt( closestPointID_neighbor );
                    resultImage.drawLine2D( ( int ) regPoint.x, ( int ) regPoint.y,
                                            ( int ) neighbPoint.x, ( int ) neighbPoint.y, 255 );
                }
            } // end of for-loop: iterate over all pairs of region endpoints
        } // end of for-loop: iterate over all regions

        return resultImage;

    } // end of function: LinkRegions(...)

    /**
     * Function for linking adjacent pixels in a binary image.
     * <p>
     * This function processes a binary image containing only isolated pixels in
     * the foreground. The foreground color is white. Based on the given maximal
     * distance adjacent pixels are linked together to components. The result is
     * comparable to a single-linkage clustering. As result an image is returned
     * where adjacent pixels are linked to each other by a line of width one.
     * <p>
     * Note that the behaviour of the function is undefined if there are
     * foreground objects present in the image other than single pixels!
     * <p/>
     * @param binIP   input binary image with regions
     * @param maxDist
     * @return binary image where adjacent pixels are linked by a line of width
     *         1
     *         pixel
     * @throws ALDOperatorException
     *                                   <p/>
     * @throws ALDProcessingDAGException
     * @todo Rückgabe ändern, es sollten Regionen oder so zurückkommen, damit
     * man
     * auf Punkte pro Region schliessen kann...
     */
    protected MTBImageByte LinkAdjacentPixels( MTBImageByte binIP, int maxDist )
            throws ALDOperatorException, ALDProcessingDAGException {

        this.width = binIP.getSizeX();
        this.height = binIP.getSizeY();

        // initialize result image
        MTBImageByte resultImage = ( MTBImageByte ) MTBImage.createMTBImage(
                this.width, this.height, 1, 1, 1, MTBImageType.MTB_BYTE );
        resultImage.fillWhite();

        // copy input image data
        resultImage.setImagePart( binIP, 0, 0, 0, 0, 0 );

        // label components in binary image
        LabelComponentsSequential regionLabler = new LabelComponentsSequential(
                binIP, getDiagonalNeighbors() );
        regionLabler.runOp( false );
        MTBRegion2DSet binRegions = regionLabler.getResultingRegions();

        // iterate over all regions and link them to neighbor regions
        for ( int i = 0; i < binRegions.size() - 1; ++i ) {

            int reg_ax = ( int ) binRegions.get( i ).getCenterOfMass_X();
            int reg_ay = ( int ) binRegions.get( i ).getCenterOfMass_Y();

            // find neighboring regions
            for ( int j = i + 1; j < binRegions.size(); ++j ) {

                int reg_bx = ( int ) binRegions.get( j ).getCenterOfMass_X();
                int reg_by = ( int ) binRegions.get( j ).getCenterOfMass_Y();

                double dist = ( reg_ax - reg_bx ) * ( reg_ax - reg_bx ) + ( reg_ay - reg_by )
                        * ( reg_ay - reg_by );

                // accept endpoints with distance below threshold
                if ( dist < maxDist * maxDist ) {
                    resultImage.drawLine2D( reg_ax, reg_ay, reg_bx, reg_by, 255 );
                }
            }
        }
        return resultImage;
    } // end of function: LinkRegions(...)

    /**
     * Dilate components, but avoid merges with adjacent components.
     * <p>
     * Each component is dilated up to the maximum given pixel distance.
     * However, the dilation is stopped immediately if its continuation
     * would cause the component to merge with one of its neighbors.
     * 
     * @param labelImg		Input image.
     * @param maxDist		Size of dilation mask to apply.
     * @return Resulting binary image.
     * @throws ALDOperatorException
     * @throws ALDProcessingDAGException
     */
    protected MTBImage VoronoiExpandComponents( MTBImage labelImg, 
    		int maxDist ) 
    	throws ALDOperatorException, ALDProcessingDAGException {

    	// dilate the image
    	ImgDilate dilOp = new ImgDilate( labelImg, (maxDist + 1) * 2 );
    	dilOp.runOp();
    	MTBImage dilateBinImg = dilOp.getResultImage();

    	// determine pixels in dilated areas
    	MTBImage relPixels = MTBImage.createMTBImage( 
    		labelImg.getSizeX(), labelImg.getSizeY(), 1, 1, 1, 
    			MTBImageType.MTB_BYTE );
    	for ( int y = 0; y < labelImg.getSizeY(); ++y ) {
    		for ( int x = 0; x < labelImg.getSizeX(); ++x ) {
    			if (   dilateBinImg.getValueInt( x, y ) > 0 
    					&& labelImg.getValueInt( x, y ) == 0 ) {
    				relPixels.putValueInt( x, y, 255 );
    			}
    		}
    	}

    	// perform component labeling on original input image
    	LabelComponentsSequential regionLabler = 
    		new LabelComponentsSequential(labelImg, getDiagonalNeighbors());
    	regionLabler.runOp();
    	MTBRegion2DSet comps = regionLabler.getResultingRegions();

    	// calculate border pixels of all components
//    	MTBImageByte binImgByte = 
//    		(MTBImageByte)binImg.convertType(MTBImageType.MTB_BYTE, false);
    	BordersOnLabeledComponents blc = 
   			new BordersOnLabeledComponents( labelImg, null, 
 					BorderConnectivity.CONNECTED_8, BorderType.OUTER_BORDERS, 1);
    	blc.runOp();
    	MTBBorder2DSet conts = blc.getResultBorders();
    	
    	// check each pixel in the image if it belongs to a dilated area,
    	// if so, find the nearest component based on the distance to the
    	// component's contour
    	for ( int y = 0; y < labelImg.getSizeY(); ++y ) {
    		for ( int x = 0; x < labelImg.getSizeX(); ++x ) {

    			// pixel is not new, i.e. was there before -> ignore it
    			if ( relPixels.getValueInt( x, y ) == 0 ) {
    				continue;
    			}

    			// iterate over all contours
    			double d;
  				double c_dist = Double.MAX_VALUE;
    			double minDist_l = Double.MAX_VALUE;
    			int minID = -1;
    			for ( int i = 0; i < conts.size(); ++i ) {
    				MTBBorder2D c = conts.elementAt( i );
    				Vector<Point2D.Double> pc = c.getPoints();

    				// iterate over all points of the contour
    				c_dist = Double.MAX_VALUE;
    				for ( int k = 0; k < pc.size(); ++k ) {
    					Point2D.Double pk = pc.elementAt( k );
    					d = ( x - pk.getX() ) * ( x - pk.getX() )
    						+ ( y - pk.getY() ) * ( y - pk.getY() ) ;
    					if ( d < c_dist ) {
    						c_dist = d;
    					}
    				}
    				if ( c_dist < minDist_l ) {
    					minDist_l = c_dist;
    					minID = i;
    				}
    			}
    			// add the new pixel to the corresponding component
    			if ( minDist_l < maxDist*maxDist && minID != -1 ) {
    				comps.elementAt( minID ).addPixel( x, y );
    			}
    		}
    	}

    	// create result image
    	MTBImage resultImage = MTBImage.createMTBImage( 
    		labelImg.getSizeX(), labelImg.getSizeY(), 1, 1, 1, this.inImg.getType()); 
    	for ( int i = 0; i < comps.size(); ++i ) {
    		MTBRegion2D r = comps.elementAt( i );
    		Vector<Point2D.Double> points = r.getPoints();
    		for ( int j = 0; j < points.size(); ++j ) {
    			Point2D.Double p = points.elementAt( j );
    			resultImage.putValueInt( 
    				(int)Math.round( p.getX()), (int)Math.round(p.getY()), i+1);
    		}
    	}
    	return resultImage;
    }

    /**
     * Set the pixel value containing in a {@code java.util.Vector}
     * of {@code java.awt.geom.Points2D} to 0.
     * <p/>
     * This is a private helper method.
     * <p/>
     * @param points
     * @param image
     */
    private void foregroundToBackground( final Vector<Point2D.Double> points,
                                         final MTBImage image ) {
        assert points != null;
        assert image != null;

        // clear all foreground pixel
        for ( int j = 0; j < points.size(); ++j ) {

            int x = ( int ) points.elementAt( j ).x;
            int y = ( int ) points.elementAt( j ).y;

            image.putValueInt( x, y, 0 );
        }
    }
}
