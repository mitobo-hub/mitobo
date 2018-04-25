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

package de.unihalle.informatik.MiToBo.features.contours;

import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBContour2D;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBContour2DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;

import java.awt.geom.Point2D;
import java.util.Vector;

/**
 * Implementation of curvature estimating algorithm's for discrete curved lines
 * in a 2D space.
 * <p>
 * The following algorithm's used:
 * <ul>
 * H. Freeman, L.S. Davis, "A Corner-Finding Algorithm for Chain-Coded Curves,"
 * IEEE Transactions on Computers, vol. 26, no. 3, pp. 297-303, March 1977
 * </ul>
 * 
 * @paper{FD1977, AUTHOR = {Freeman, H. and Davis, L.S.}, TITLE = {A
 * Corner-Finding Algorithm for Chain-Coded Curves}, YEAR = {1977}, PUBLISHER =
 * {IEEE Transactions on Computers} }
 * 
 * @author Tim Langhammer, Halle/Saale, <tim.langhammer@student.uni-halle.de>
 * @author posch
 */
 
// * Implementation  of
// * <ul>
// * H.L. Beus and S.S.H. Tiu. "An improved corner detection algorithm based on
// * chain-coded plane curves". Pattern Recognition, 20:291-296, 1987.
// * </ul>
// * 
// * @paper{BT1987, AUTHOR = {Beus, H.L and Tiu, S.S.H. }, TITLE = {An improved
// * corner detection algorithm based on chain-coded plane curves}, YEAR = {1987},
// * PUBLISHER = {Pattern Recognition} }
// * is prepared but commented (as not thoroughly tested


@ALDAOperator( genericExecutionMode = ALDAOperator.ExecutionMode.ALL,
               level = ALDAOperator.Level.STANDARD )
public class Contour2DCurvatureCalculator extends MTBOperator {
	
    /** 
     * in length of vector k  as suggested by H.Freeman and L.S.Davis 
     */
    private static final int K_MIN = 2;

    public enum Algorithm {
        MODIFIED_FREEMAN_DAVIS,
        BEUS_TIU
    }

   /**
     * Set of contours to compute curvatures for
     */
	@Parameter( label= "Contour set", required = true, dataIOOrder = 0,
			direction = Parameter.Direction.IN, description = "Set of input contours")
	private MTBContour2DSet inContours = null;

    /**
     * The algorithm used for estimating the curvature.
     */
//    @Parameter( label = "Algorithm used", required = true,
//                direction = Parameter.Direction.IN, dataIOOrder = 1,
//                description = "Algorithm to estimate the curvature" )
    private Contour2DCurvatureCalculator.Algorithm algorithm = Contour2DCurvatureCalculator.Algorithm.MODIFIED_FREEMAN_DAVIS;
  


    /**
     * Used only for MODIFIED_FREEMAN_DAVIS
     */
    @Parameter( label = "k", required = true,
                dataIOOrder = 4,
                direction = Parameter.Direction.IN,
                description = "k for freeman davis" )
    private Integer k = 3;
 
    /**
     * Used only for BEUS_TIU
     */
//    @Parameter( label = "k lower", required = true,
//                dataIOOrder = 5,
//                direction = Parameter.Direction.IN,
//                description = "lower bound of k for beus tiu" )
    private Integer kLower = K_MIN;
    
    /**
     * Used only for BEUS_TIU
     */
//    @Parameter( label = "k upper", required = true,
//                dataIOOrder = 6,
//                direction = Parameter.Direction.IN,
//                description = "upper bound of k for beus tiu" )
    private Integer kUpper = 9;
    
	@Parameter( label= "Input Image", required = false, dataIOOrder = 7,
			direction = Parameter.Direction.IN, description = "Input image.")
	private MTBImage inputImage = null;
    
    @Parameter( label = "displayThres", required = false,
                dataIOOrder = 8,
                direction = Parameter.Direction.IN,
                description = "threshold between 0 and 1 to threshold absolute curvature normalized to [-1;1] and display with displayColor; if negative display curvature" )
    private Double displayThres = 0.9;

   
    @Parameter( label = "displayColor", required = false,
            dataIOOrder = 8,
            direction = Parameter.Direction.IN,
            description = "color to display curvatures if displayThres >=0\nOtherwise the curvature is displayed" )
    private Double displayColor= 255.0;

    /**
     * Resulting Curvatures for each Pixel on the given {@code MTBContour2D}.
     * <p/>
     */
    @Parameter( label = "Resulting vector of curvatures",
                direction = Parameter.Direction.OUT, dataIOOrder = 0,
                description = "The vector of curvatures for each pixel of each contour" )
    private Vector<double[]> vectorOfCurvatures;
    
	@Parameter( label= "Output Image", required = true, dataIOOrder = 1,
			direction = Parameter.Direction.OUT, description = "Image including curvatures (if input image is given).")
	private MTBImage outimg = null;

   // ========================================================
    
    /**
     * Default empty constructor.
     * <p/>
     * @throws ALDOperatorException
     */
    public Contour2DCurvatureCalculator() throws ALDOperatorException {
        // default constructor
    }

    /**
     *
     * @param mtbContour2D
     * @throws ALDOperatorException
     */
    public Contour2DCurvatureCalculator( final MTBContour2DSet inContours ) throws ALDOperatorException {
        if ( inContours == null ) {
            throw new IllegalArgumentException( "Null MTBContour2D!" );
        }
        this.inContours = inContours;
    }

    @Override
    public void validateCustom() throws ALDOperatorException {

    	if ( inputImage != null && 
    		 (inputImage.getSizeC() != 1 ||
    			inputImage.getSizeZ() != 1 ||
    			inputImage.getSizeT() != 1 ) )
            throw new ALDOperatorException( ALDOperatorException.OperatorExceptionType.VALIDATION_FAILED,
                    "Operator handles only 2D images");

    	if ( inputImage != null && inputImage.getType() == MTBImageType.MTB_RGB)
            throw new ALDOperatorException( ALDOperatorException.OperatorExceptionType.VALIDATION_FAILED,
                    "Operator cannot handle RGB images");

        if ( algorithm == Algorithm.MODIFIED_FREEMAN_DAVIS && getK() < K_MIN ) {
            throw new ALDOperatorException( ALDOperatorException.OperatorExceptionType.VALIDATION_FAILED,
                                            "k( " + getK() + " ) is lesser then " + K_MIN );
        }
        if ( algorithm == Algorithm.BEUS_TIU && getKLower() < K_MIN ) {
            throw new ALDOperatorException( ALDOperatorException.OperatorExceptionType.VALIDATION_FAILED,
                                            "lower bound( " + getKLower() + " is lesser then " + K_MIN );
        }
        if ( algorithm == Algorithm.BEUS_TIU &&  getKUpper() < getKLower() ) {
            throw new ALDOperatorException( ALDOperatorException.OperatorExceptionType.VALIDATION_FAILED,
                                            "upper bound( " + getKUpper() + " ) is smaller then lower bound( " + getKLower() + " )" );
        }

    }

    @Override
    protected void operate() throws ALDOperatorException, ALDProcessingDAGException {

    	vectorOfCurvatures = new Vector<double[]>(inContours.size());

    	for (int i=0; i<inContours.size();++i) {
    		final Vector<Point2D.Double> contourPixelVec = inContours.elementAt(i).getPoints();

    		switch ( getAlgorithm() ) {

    		case MODIFIED_FREEMAN_DAVIS:
    			vectorOfCurvatures.add( i, calculateCurvatureModifiedFreemanDavis( contourPixelVec, getK()));
    			break;
    		case BEUS_TIU:
    			vectorOfCurvatures.add( i, calculateCurvatureBeusTiu( contourPixelVec));
    			break;
    		default:
    			throw new IllegalArgumentException( "Mode : " + getAlgorithm() + " not supported!" );
    		}
    	}
    	
    	if ( inputImage != null ) {
    		if ( displayThres < 0 ) {
    			// create float image
    			this.outimg = MTBImage.createMTBImage( inputImage.getSizeX(), inputImage.getSizeX(),
    					1, 1, 1, MTBImageType.MTB_FLOAT);
    		} else {
    			// clone image
    			this.outimg = this.inputImage.duplicate();
    		}
    		
    		for (int i=0; i<inContours.size();++i) {
    			MTBContour2D c = inContours.elementAt(i);

				double maxCurve = 0;
				double[] curvatures = vectorOfCurvatures.get(i);
				for (int j = 0; j<curvatures.length;++j) {
					if ( Math.abs( curvatures[j]) > maxCurve)
						maxCurve =  Math.abs( curvatures[j]);
				}

    			double thresh = displayThres * maxCurve;
    			for (int j = 0; j<curvatures.length ; ++j) {
    				if ( displayThres < 0 ) {
    					this.outimg.putValueDouble( (int)c.getPointAt(j).x, (int)c.getPointAt(j).y,
    							curvatures[j]);
    				} else {
    					if ( Math.abs( curvatures[j]) >= thresh ) {
    						this.outimg.putValueDouble(
    								(int)c.getPointAt(j).x, (int)c.getPointAt(j).y, displayColor);
    					} else
    						System.out.println("not displayed " + curvatures[j] + " thres " + thresh);
    				}
    			}
    		}
    	}
    }

    /**
     * Calculates the curvatures by a modified version of the Freeman and Davis
     * curvature estimation algorithm.
     * <p>
     * @param contourVec The {@code Vector<Point2D.Double>} of all contour
     *                   pixels.
     * @param k          k (index distance between contour points).
     * @return Return a list of {@code CurvatureForPointVO} calculated by a
     *         modified version of the Freeman and Davis algorithm.
     */
    private double[] calculateCurvatureModifiedFreemanDavis(
            final Vector<Point2D.Double> contourVec, final int k ) {
        assert contourVec != null : "";
        assert k > K_MIN : "";

        final double[] thetas = computeThetas( contourVec, k );
        final double[] deltas = computeDeltas( thetas );
        final double[] averagedDeltas = averageDeltas( deltas, k );
        
        return averagedDeltas;
    }

    private double[] calculateCurvatureBeusTiu( Vector<Point2D.Double> contourPixelVec ) {
        
    	double[] curvatures = new double[contourPixelVec.size()];
        double[][]E_Vector = new double[kUpper-kLower+1][contourPixelVec.size()];
        for ( int k = kLower ; k <= kUpper ; k++ ) {
        	E_Vector[k-kLower] = calculateCurvatureModifiedFreemanDavis(contourPixelVec, k);
        }
        for ( int i = 0; i < contourPixelVec.size(); i++ ) {
        	double averageE = 0.0;
            for ( int k = kLower ; k <= kUpper ; k++ ) {
            	averageE += E_Vector[k-kLower][i];
            }
            averageE *= 1D / ( 1D + kUpper - kLower);
            curvatures[i] = averageE;
        }
        return curvatures;
    }

    /**
     * Return the centered angles (named delta).
     * <p>
     * A centered angle of pixel {@code i} is the difference of
     * {@code theta(i+1)} and {@code theta(i-1)}.
     * 
     * @param thetaAngleArray The {@code theta} angles.
     * @return deltas.
     */
    private double[] computeDeltas( final double[] thetaAngleArray ) {
        assert thetaAngleArray != null : "";

        final int numOfPixel = thetaAngleArray.length;
        final double[] deltas = new double[numOfPixel];
        
        int prevIdx, nextIdx;

        for ( int i = 0; i < numOfPixel; i++ ) {
            prevIdx = ( i - 1 ) < 0 ? numOfPixel + ( i - 1 ) : ( i - 1 );
            nextIdx = ( i + 1 ) % numOfPixel;
            deltas[i] = angleDiffPM180( thetaAngleArray[nextIdx], thetaAngleArray[prevIdx] );

            
        }

        return deltas;
    }

    /**
     * Return the averaged deltas, i.e. <code>E</code>.
     * <p>
     * In contrast to the original method we normalize with the number of
     * angles (deltas) averaged, i.e {@code 1 / k + 1}.
     * <p/>
     * @param deltas
     * @param k
     * @return averaged deltas
     */
    private double[] averageDeltas( final double[] deltas, final int k ) {
        assert deltas != null;
        assert k >= K_MIN;

        final double[] averagedDeltas = new double[deltas.length];
        
        for ( int i = 0; i < deltas.length; i++ ) {
            double sum = 0D;
            
            for ( int idx = i ; idx <= i+k ; idx ++) {
                sum += deltas[idx % deltas.length];
            }
            averagedDeltas[i] = ( 1D / ( 1D + k ) ) * sum;
        }
        return averagedDeltas;
    }

    /**
     * Calculate the {@code theta} angle for all contour pixels provided.
     * <p>
     * @param contourPixelVec The {@code Vector} of contour pixels.
     * @param k               The number of neighbor pixels involved in the
     *                        calculation.
     * @return
     */
    private double[] computeThetas( final Vector<Point2D.Double> contourPixelVec, final int k ) {
        assert contourPixelVec != null : "contourPixelVec got to be non null";
        assert k >= K_MIN : "k got to be >= " + K_MIN;

        final int numOfPixel = contourPixelVec.size();
        final double[] thetaAngleArray = new double[numOfPixel];
        
        for ( int i = 0; i < numOfPixel; i++ ) {
        	int bvIdx = i - k;
            
            // NOTE : In Java '%' is not Modulo but the Remainder of the integer division!, so we can't use it
            // in case of negative numbers
        	while ( bvIdx < 0)
        		bvIdx += numOfPixel;
        	
            int x = ( int ) contourPixelVec.get( i ).x;
            int y = ( int ) contourPixelVec.get( i ).y;
            int xb = ( int ) contourPixelVec.get( bvIdx ).x;
            int yb = ( int ) contourPixelVec.get( bvIdx ).y;
            
            int deltaX = x - xb;
            int deltaY = y - yb;

            double tempTheta = Math.atan2( deltaY, deltaX );
            // we want only positive angles
            thetaAngleArray[i] = toPositiveAngle360( Math.toDegrees( tempTheta ) );
        }

        return thetaAngleArray;
    }
    
    // ===== Getter and Setter
    
    /**
     * Get calculated curvatures.
     * @return	Vector of curvatures.
     */
    public Vector<double[]> getResultVectorOfCurvatures() {
    	return this.vectorOfCurvatures;
    }
    
    /**
    *
    * @return
    */
   public Contour2DCurvatureCalculator.Algorithm getAlgorithm() {
       return algorithm;
   }

   /**
    *
    * @param algorithm
    */
   public void setAlgorithm( final Contour2DCurvatureCalculator.Algorithm algorithm ) {
       this.algorithm = algorithm;
   }

   public int getKLower() {
       return kLower;
   }

   public void setKLower( int lowerBound ) {
       this.kLower = lowerBound;
   }

   public int getKUpper() {
       return kUpper;
   }

   public void setKUpper( int upperBound ) {
       this.kUpper = upperBound;
   }

   public int getK() {
       return k;
   }

   public void setK( final int vectorLength ) {
       this.k = vectorLength;
   }


   // ===== HELPER
   /**
   * Compute angle difference of two angles in interval 0 up to 360 degree
   * 
   * @param angleOne
   * @param angleTwo
   * @return
   */
   @SuppressWarnings("unused")
   private static final double angleDiff360( final double angleOne, final double angleTwo ) {
      if( angleOne < 0D || angleOne > 360D ) {
          throw new IllegalArgumentException( "angle one ( " + angleOne + " ) is out of bounds, should be between 0 and 360" );
      }
      if( angleTwo < 0D || angleTwo > 360D ) {
          throw new IllegalArgumentException( "angle one ( " + angleTwo + " ) is out of bounds, should be between 0 and 360" );
      }
      final double temp = Math.abs( angleOne - angleTwo );
      return Math.min( temp, 360D - temp );
  }
  
  /**
  * Compute angle difference of two angles in interval -180 up to 180 degree
  * 
  * @param angleOne
  * @param angleTwo
  * @return
  */
 private static final double angleDiffPM180( final double angleOne, final double angleTwo ) {
     if( angleOne < 0D || angleOne > 360D ) {
         throw new IllegalArgumentException( "angle one ( " + angleOne + " ) is out of bounds, should be between 0 and 360" );
     }
     if( angleTwo < 0D || angleTwo > 360D ) {
         throw new IllegalArgumentException( "angle one ( " + angleTwo + " ) is out of bounds, should be between 0 and 360" );
     }
     double temp = angleOne - angleTwo;
     if ( temp < -180) 
   	  temp += 360D;
     else if ( temp > 180)
   	  temp -= 360D;
     
     return temp;
 }

  /**
   * Normalize angles (in the range -360 to 360 degree) to the range 0-360 degree.
   * 
   * @param angle An angle in the range of [-360,360]
   * @return
   */
 private static final double toPositiveAngle360( final double angle ) {
      if( angle < -360D || angle > 360 ) {
          throw new IllegalArgumentException( "Angle out of range : " + angle + ", should be between -360 and 360!" );
      }
      return angle < 0D ? 360D + angle : angle;
  }
}
