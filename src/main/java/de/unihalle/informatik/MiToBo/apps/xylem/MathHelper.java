package de.unihalle.informatik.MiToBo.apps.xylem;

import java.awt.geom.Point2D;
import java.util.Arrays;

public final class MathHelper {

	public static final double PI_HALF = Math.PI / 2D;

	/**
	 * Return the {@code arcus cotangens} of {@code value} in radian.
	 * <p>
	 * This function is implemented as described in "Teubner - Taschenbuch
	 * der Mathematik [Bronstein/Samendjajew]".
	 * <p>
	 * <p/>
	 * @param value A rational floating point number.
	 * @return The {@code Arcuscotangens} of {@code value}.
	 */
	public static final double acot( final double value ) {
		double rc;

		// Zero == Pi/2
		// 
		if( value == 0D ) {
			rc = PI_HALF;
		} // Everything above zero
		else if( value > 0D ) {
			rc = Math.atan( 1D / value );
		} // Everything below zero
		else {
			rc = Math.PI + Math.atan( 1D / value );
		}

		return rc;
	}

	/**
	 *
	 * @param angleOne
	 * @param angleTwo
	 * @return
	 */
	public static final double angleDiff360( final double angleOne, final double angleTwo ) {
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
	 * Return the mean value of an array of double.
	 * <p/>
	 * @param values
	 * @return
	 */
	public static double calcMean( final double[] values ) {
		if( values == null ) {
			throw new IllegalArgumentException( "vector values == null!" );
		}
		if( values.length == 0 ) {
			return 0D;
		}

		double temp = 0;
		for( int i = 0; i < values.length; i++ ) {
			temp += values[i];
		}
		return temp / ( double ) values.length;
	}

	/**
	 * Returns the median value of an array of double.
	 * <p/>
	 * @param values
	 * @return
	 */
	public static double calcMedian( final double[] values ) {
		if( values == null ) {
			throw new IllegalArgumentException( "vector values == null!" );
		}
		if( values.length == 0 ) {
			return 0D;
		}

		double rc;

		Arrays.sort( values );

		final int dataLength = values.length;

		// even number of samples
		if( dataLength % 2 == 0 ) {
			//
			rc = ( values[ ( dataLength / 2 ) - 1] + values[ ( dataLength / 2 )] ) / 2;

		} else {
			// odd number of sample
			rc = values[( dataLength / 2 )];
		}

		return rc;
	}

	/**
	 * Returns the min and max value.
	 * <p/>
	 * @param values
	 * @return
	 */
	public static double[] calcMinMax( final double[] values ) {
		if( values == null ) {
			throw new IllegalArgumentException( "vector values == null!" );
		}
		if( values.length == 0 ) {
			return new double[2];
		}
		final double[] minMaxValue = new double[2];

		Arrays.sort( values );

		minMaxValue[0] = values[0];
		minMaxValue[1] = values[values.length - 1];

		//System.out.println( "Min/Max : " + minMaxValue[0] + "," + minMaxValue[1] );
		return minMaxValue;
	}

	/**
	 * Returns the variance of an array of double.
	 * <p/>
	 * @param values
	 * @return
	 */
	public static double calcVariance( final double[] values ) {
		if( values == null ) {
			throw new IllegalArgumentException( "vector values == null!" );
		}
		if( values.length == 0 ) {
			return 0D;
		}
		double rc;

		final int dataLength = values.length;
		final double mean = calcMean( values );
		double temp = 0D;

		for( int i = 0; i < dataLength; i++ ) {
			temp += ( values[i] - mean ) * ( values[i] - mean );
		}

		rc = ( 1D / ( dataLength - 1D ) ) * temp;

		return rc;
	}

	/**
	 * Return the {@code cotangens} of {@code value}. In case of {@code 0}
	 * the returned value is {@link java.lang.Double#NaN not a number} - the
	 * tangens function is not defined for zero -> 1/<em>Infinty</em> is not
	 * a number.
	 * <p/>
	 * @param value The argument of the cotangens function.
	 * @return The result of the cotangens function.
	 */
	public static final double cot( final double value ) {
		return value == 0 ? Double.NaN : 1D / Math.tan( value );
	}

	/**
	 * Substract two {@code Point2D.Double }.
	 * <p/>
	 * @param pointOne
	 * @param pointTwo
	 * @return
	 */
	public static final Point2D.Double point2DDiff( final Point2D.Double pointOne, final Point2D.Double pointTwo ) {
		if( pointOne == null || pointTwo == null ) {
			throw new IllegalArgumentException( "point one or point two are null!( " + pointOne + ", " + pointTwo + " )" );
		}

		return new Point2D.Double( pointOne.x - pointTwo.x, pointOne.y - pointTwo.y );
	}

	/**
	 * Sums up two {@code Point2D.Double }.
	 * <p/>
	 * @param pointOne
	 * @param pointTwo
	 * @return
	 */
	public static final Point2D.Double point2DSum( final Point2D.Double pointOne, final Point2D.Double pointTwo ) {
		if( pointOne == null || pointTwo == null ) {
			throw new IllegalArgumentException( "point one or point two are null!( " + pointOne + ", " + pointTwo + " )" );
		}

		return new Point2D.Double( pointOne.x + pointTwo.x, pointOne.y + pointTwo.y );
	}

	/**
	 * Converts an angle in degree to its corresponding in cartesian
	 * coordinates.
	 * <p/>
	 * @param angleDegree An angle in degree.
	 * @return A {@code Point2D.Double } containing the coordinate in
	 * cartesian coordinate system
	 */
	public static final Point2D.Double toCartesianAsPoint2D( final double angleDegree ) {

		final double x = Math.cos( Math.toRadians( angleDegree ) );
		final double y = Math.sin( Math.toRadians( angleDegree ) );

		return new Point2D.Double( x, y );

	}

	/**
	 * Converts an angle in degree to its correspondant in cartesian
	 * coordinates.
	 * <p/>
	 * @param angleDegree An angle between 0 and 359 degree.
	 * @return An array with the {@code x } and {@code y } cartesian
	 * coordinate.
	 */
	public static final double[] toCartesianAsVector( final double angleDegree ) {
		if( angleDegree < 0 || angleDegree > 360 ) {
			throw new IllegalArgumentException( "angle ( "
					+ angleDegree + " ) is out of bounds, should be between 0 and 360 degree" );
		}
		final double[] vector = new double[2];
		vector[0] = Math.cos( Math.toRadians( angleDegree ) );
		vector[1] = Math.sin( Math.toRadians( angleDegree ) );

		return vector;
	}

	/**
	 * Simple way to convert negative angles to 0-360 degree.
	 * <p/>
	 * @param angle An angle in the range of [-360,360]
	 * @return
	 */
	public static final double toPositiveAngle360( final double angle ) {
		if( angle < -360D || angle > 360 ) {
			throw new IllegalArgumentException( "Angle out of range : " + angle + ", should be between -360 and 360!" );
		}
		return angle < 0D ? 360D + angle : angle;
	}

	/**
	 * Subtract two vectors.
	 * <p/>
	 * @param vecOne
	 * @param vecTwo
	 * @return
	 */
	public static double[] vectorDiff( final double[] vecOne, final double[] vecTwo ) {
		if( vecOne == null || vecTwo == null ) {
			throw new IllegalArgumentException( "vector 'vecOne' or 'vecTwo' == null!" );
		}
		if( vecOne.length != vecTwo.length ) {

			throw new IllegalArgumentException( "The vectors differs in length!( " + vecOne.length + " != " + vecTwo.length );
		}
		double[] ret = new double[vecOne.length];

		for( int i = 0; i < ret.length; i++ ) {
			ret[i] = vecOne[i] - vecTwo[i];
		}
		return ret;
	}

	/**
	 * Returns the norm of a vector.
	 * <p/>
	 * @param vector
	 * @return
	 */
	public static double vectorNorm( final double[] vector ) {
		if( vector == null ) {
			throw new NullPointerException( "Null vector@vectorNorm!" );
		}

		double temp = 0;
		for( int i = 0; i < vector.length; i++ ) {
			temp += vector[i] * vector[i];
		}

		return Math.sqrt( temp );
	}

	/**
	 * Return the unit vector of {@code vector}.
	 * <p>
	 * @param vector
	 * @return
	 */
	public static double[] unitVector( final double[] vector ) {
		if( vector == null ) {
			throw new NullPointerException( "Null vector@vectorNorm!" );
		}

		final double normOfVector = vectorNorm( vector );
		final double[] unitVector = new double[vector.length];
		for( int i = 0; i < vector.length; i++ ) {
			unitVector[i] = vector[i] / normOfVector;
		}
		return unitVector;
	}

	/**
	 * Sums up two vectors.
	 * <p/>
	 * @param vecOne
	 * @param vecTwo
	 * @return
	 */
	public static final double[] vectorSum( final double[] vecOne, final double[] vecTwo ) {
		if( vecOne == null || vecTwo == null ) {
			throw new IllegalArgumentException( "vector 'vecOne' or 'vecTwo' == null!" );
		}
		if( vecOne.length != vecTwo.length ) {

			throw new IllegalArgumentException( "The vectors differs in length!( " + vecOne.length + " != " + vecTwo.length );
		}
		double[] ret = new double[vecOne.length];

		for( int i = 0; i < ret.length; i++ ) {
			ret[i] = vecOne[i] + vecTwo[i];
		}
		return ret;
	}

	private MathHelper() {
	}
}
