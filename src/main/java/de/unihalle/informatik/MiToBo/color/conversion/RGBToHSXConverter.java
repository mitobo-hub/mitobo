package de.unihalle.informatik.MiToBo.color.conversion;

import java.awt.Color;

import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageRGB;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;

/**
 * Converts an image of type {@link MTBImageRGB MTBImageRGB} ( RGB color space )
 * into an image with HSX( hue saturation intensity/brightness/value ) color
 * space.
 * <p/>
 * The type of the resulting image is {@code MTBImageByte MTBImageByte }
 * or {@code MTBImageFloat MTBImageFloat }.
 * In the later case all three channels have values in the range <code>0..1</code>,
 * otherwise in the range <code>0..255</code>.
 * <p/>
 * There are three ways to convert an rgb-image.
 * <ol>
 * <li>RGB to HSI introduced by Sonka</li>
 * <li>RGB to HSB, pure Java Implementation</li>
 * <li>RGB to HSV, adapted from <a
 * href="http://www.easyrgb.com/">http://www.easyrgb.com</a></li>
 * </ol>
 * <p/>
 * @book{Sonka99, AUTHOR = {Sonka, Milan and Hlavac, Vaclav and Boyle, Roger},
 * TITLE = {Image Processing: Analysis and Machine Vision}, YEAR = {1999},
 * PUBLISHER = {PWS Publishing}, city = {Pacific Grove}, }
 * <p/>
 * @author Tim Langhammer, Halle/Saale, <tim.langhammer@student.uni-halle.de>
 */
@ALDAOperator( genericExecutionMode = ALDAOperator.ExecutionMode.ALL,
               level = ALDAOperator.Level.APPLICATION )
public class RGBToHSXConverter extends MTBOperator {

    /**
     * Constants to convert from [0-255] to [0-359] and vice versa.
     */
    public static final float BYTE_TO_DEGREE = 360F / 255F;
    public static final float DEGREE_TO_BYTE = 255F / 360F;
    // placeholder for a three component colorspace size
    private final int THREE_COMPONENTS_COLOR_SPACE = 3;
    // constants for indexing the components of a rgb-pixel
    private static final int RED_COMPONENT_INDEX = 0;
    private static final int GREEN_COMPONENT_INDEX = 1;
    private static final int BLUE_COMPONENT_INDEX = 2;
    // constants for indexing the components of a hsv-pixel
    private static final int HUE_COMPONENT_INDEX = 0;
    private static final int SATURATION_COMPONENT_INDEX = 1;
    private static final int X_COMPONENT_INDEX = 2;
    
    /**
     * To cope with undefined saturation/hue-values, we
     * use the following scheme. If x component equals zero then saturation and hue
     * are undefined. We set them to UNDEFINED defined as zero.
     * If saturation is zero, then hue is undefined (and set to UNDEFINED defined as zero).
     */
    public static final float SATURATION_UNDEFINED = 0F;
    public static final float HUE_UNDEFINED = 0F;

    /**
     * Supported Color Channels.
     */
    public enum ColorChannel {

        /**
         * Red Color Channel.
         */
        RED( RED_COMPONENT_INDEX, 0, 255 ),
        /**
         * Green Color Channel.
         */
        GREEN( GREEN_COMPONENT_INDEX, 0, 255 ),
        /**
         * Blue Color Channel.
         */
        BLUE( BLUE_COMPONENT_INDEX, 0, 255 ),
        /**
         * Hue Color Channel.
         */
        HUE( HUE_COMPONENT_INDEX, 0, 359 ),
        /**
         * Saturation Color Channel.
         */
        SATURATION( SATURATION_COMPONENT_INDEX, 0, 255 ),
        /**
         * Intensity Color Channel.
         */
        INTENSITY( X_COMPONENT_INDEX, 0, 255 ),
        /**
         * Value Color Channel.
         */
        VALUE( X_COMPONENT_INDEX, 0, 255 ),
        /**
         * Brightness Color Channel.
         */
        BRIGHTNESS( X_COMPONENT_INDEX, 0, 255 );
        // Index of the channel
        private final int index;
        // min value
        private final int min;
        // max value
        private final int max;

        private ColorChannel( final int index, final int min, final int max ) {
            this.index = index;
            this.min = min;
            this.max = max;
        }

        /**
         * The index of this channel.
         * <p/>
         * @return
         */
        public int getIndex() {
            return index;
        }

        /**
         * The minimum value of this channel.
         * <p/>
         * @return
         */
        public int getMin() {
            return min;
        }

        /**
         * The maximum value of this channel.
         * <p/>
         * @return
         */
        public int getMax() {
            return max;
        }
    }

    /**
     * The way of converting an image from rgb to hsi.
     */
    public enum Mode {

        /**
         * RGB to HSI. Algorithm introduced by Sonka et al.
         */
        RGB_HSI_SONKA,
        /**
         * RGB to HSB. Pure Java implementation.
         */
        RGB_HSB_JRE,
        /**
         * RGB to HSV.
         * <p/>
         * This is an implementation from the website:
         * <a href="http://www.easyrgb.com/">http://www.easyrgb.com</a>
         */
        RGB_HSV_EASY_RGB;
    }
    /**
     * Input Image.
     * <p/>
     * Should be of type <code>MTBImageRGB</code>.
     */
    @Parameter( label = "Input Image", required = true, dataIOOrder = 0,
                direction = Parameter.Direction.IN,
                description = "Input RGB-image" )
    private transient MTBImageRGB inputMTBImageRGB = null;
    
    /**
     * Converter mode/algorithm
     */
    @Parameter( label = "Algorithm used", required = true, dataIOOrder = 1,
                direction = Parameter.Direction.IN,
                description = "The algorithm used" )
    private RGBToHSXConverter.Mode mode = RGBToHSXConverter.Mode.RGB_HSI_SONKA;
    
    /**
     * Create byte or float image
     */
    @Parameter( label = "Create float image", required = true, dataIOOrder = 1,
                direction = Parameter.Direction.IN,
                description = "If true a float image is created, otherwise a byte image" )
    private boolean createFloatImage = false;
    
   /**
     * Result Image.
     * <p/>
     * The result image is a <code>MTBImage</code>.
     */
    @Parameter( label = "Result Image", required = true,
                direction = Parameter.Direction.OUT, dataIOOrder = -1,
                description = "Result image (HSX color space)" )
    private transient MTBImage resultMTBImg = null;
    
    /**
     * Hue channel image.
     * <p/>
     * The hue channel image is a <code>MTBImage</code>.
     */
    @Parameter( label = "Hue channel Image", required = true,
                direction = Parameter.Direction.OUT, dataIOOrder = -2,
                description = "Hue-channel image" )
    private transient MTBImage hueMTBImg = null;
    
    /**
     * Saturation channel image.
     * <p/>
     * The result image is a <code>MTBImage</code>.
     * <p/>
     */
    @Parameter( label = "Saturation channel Image", required = true,
                direction = Parameter.Direction.OUT, dataIOOrder = -4,
                description = "Saturation-channel image" )
    private transient MTBImage satMTBImg = null;
    /**
     * Result Image.
     * <p/>
     * The result image is a <code>MTBImage</code>.
     * <p/>
     */
    @Parameter( label = "X channel Image", required = true,
                direction = Parameter.Direction.OUT, dataIOOrder = -8,
                description = "Wheather I or V/B-channel image" )
    private transient MTBImage xMTBImg = null;

    /**
     * Default constructor.
     * <p/>
     * @throws ALDOperatorException
     */
    public RGBToHSXConverter() throws ALDOperatorException {
        // default constructor
    }

    /**
     * Constructor.
     * <p/>
     * @param image The input image.
     * @throws ALDOperatorException
     */
    public RGBToHSXConverter( final MTBImageRGB image ) throws ALDOperatorException {
        if ( image == null ) {
            throw new IllegalArgumentException( "input image is null!" );
        }
        this.inputMTBImageRGB = image;
    }

    /**
     * Returns the converted image, null if not available.
     * <p/>
     * @return The result image or <code>null</code> if not set.
     */
    public MTBImage getResultMTBImage() {
        return this.resultMTBImg;
    }

    /**
     * Returns the input image, null if not set.
     * <p/>
     * @return The input image or <code>null</code> if not set.
     */
    public MTBImageRGB getInputMTBImgRGB() {
        return this.inputMTBImageRGB;
    }

    /**
     * Set the input image.
     * <p/>
     * @param inputImage The input image.
     */
    public void setInputMTBImgRGB( final MTBImageRGB inputImage ) {
        this.inputMTBImageRGB = inputImage;
    }

    /**
     * Get the hue channel image.
     * <p/>
     * @return The image which contains the hue channel informations of * * type
     *         <code>MTBImage</code>.
     */
    public MTBImage getHueMTBImg() {
        return this.hueMTBImg;
    }

    /**
     * Get the saturation channel image.
     * <p/>
     * @return The image which contains the saturation channel informations of
     *         type {@code MTBImage}.
     */
    public MTBImage getSatMTBImg() {
        return this.satMTBImg;
    }

    /**
     * Get the value channel image.
     * <p/>
     * @return The image which contains the value channel informations of * *
     *         type <code>MTBImage</code>.
     */
    public MTBImage getXMTBImg() {
        return this.xMTBImg;
    }

    /**
     * Set the hue channel image.
     * <p/>
     * @param hueChannelImage The image which contains the hue channel
     *                        informations of type <code>MTBImage</code>.
     */
    public void setHueMTBImg( final MTBImage hueChannelImage ) {
        this.hueMTBImg = hueChannelImage;
    }

    /**
     * Set the saturation channel image.
     * <p/>
     * @param saturationChannelImage The image which contains the saturation
     *                               channel informations of * * type
     *                               <code>MTBImage</code>.
     */
    public void setSatMTBImg( final MTBImage saturationChannelImage ) {
        this.satMTBImg = saturationChannelImage;
    }

    /**
     * Set the value channel image.
     * <p/>
     * @param valueChannelImage The image which contains the value channel
     *                          informations of type <code>MTBImage</code>.
     */
    public void setXMTBImg( final MTBImage valueChannelImage ) {
        this.xMTBImg = valueChannelImage;
    }

    /**
     * Creates a <code>MTBImage</code> with three channels, the same size as
     * the original one and converts each rgb pixel to a
     * <i>hsx</i>
     * pixel, where <i>x</i> stands for Inentsity or Brightness or Value
     * depending on the mode set.
     * <p/>
     * @throws ALDOperatorException
     * @throws ALDProcessingDAGException
     */
    @Override
    protected void operate() throws ALDOperatorException,
                                    ALDProcessingDAGException {
        final MTBImageRGB input = this.getInputMTBImgRGB();
        // if input is null -> throw an error
        if ( input == null ) {
            throw new ALDOperatorException( ALDOperatorException.OperatorExceptionType.INSTANTIATION_ERROR, "The input image is null!" );
        }
        if ( mode == null ) {
            throw new ALDOperatorException( ALDOperatorException.OperatorExceptionType.INSTANTIATION_ERROR, "The operation mode is null!" );
        }
        final int width = input.getSizeX();
        final int height = input.getSizeY();

        // creates a 3-channel image
        MTBImage.MTBImageType imageType;
        if ( createFloatImage ) {
        	imageType = MTBImage.MTBImageType.MTB_FLOAT;
        } else {
        	imageType = MTBImage.MTBImageType.MTB_BYTE;
        }
        resultMTBImg = MTBImage.createMTBImage( width, height, 1, 1,
                                                THREE_COMPONENTS_COLOR_SPACE, imageType);

        // Array of size 3 - containing the r/g/b-values of the actual pixel
        final int[] rgbColorValues = new int[THREE_COMPONENTS_COLOR_SPACE];

        // Array of size 3 - containing the h/s/v-values of the actual pixel
        float[] hsxColorValues = null;
 
        float hueByte, satByte, xByte;
 
        // For each Pixel of the input image...
        for ( int y = 0; y < height; y++ ) {
            for ( int x = 0; x < width; x++ ) {

                // rgb-Color : actual Pixel
                int rgbColorPack = inputMTBImageRGB.getValueInt( x, y );
                // deflate the color components
                rgbColorValues[RED_COMPONENT_INDEX] = ( rgbColorPack >> 16 ) & 0xFF;
                rgbColorValues[GREEN_COMPONENT_INDEX] = ( rgbColorPack >> 8 ) & 0xFF;
                rgbColorValues[BLUE_COMPONENT_INDEX] = ( rgbColorPack ) & 0xFF;

                // conversion from rgb to hsv
                switch ( mode ) {
                    case RGB_HSI_SONKA:
                        hsxColorValues = rgbToHSI_Sonka( rgbColorValues );
                        break;
                    case RGB_HSB_JRE:
                        hsxColorValues = rgbToHSB_JRE( rgbColorValues );
                        break;
                    case RGB_HSV_EASY_RGB:
                        hsxColorValues = rgbToHSV_EasyRGB( rgbColorValues );
                        break;

                    default:
                        throw new IllegalArgumentException( "The Algorithm : " + mode + " is not supported!" );
                }

                // mapping from [0,1] to [0,255]
                hueByte = hsxColorValues[HUE_COMPONENT_INDEX] * 255F;
                satByte = hsxColorValues[SATURATION_COMPONENT_INDEX] * 255F;
                xByte = hsxColorValues[X_COMPONENT_INDEX] * 255F;

                // set each channel
                if ( createFloatImage) {
                	resultMTBImg.putValueDouble( x, y, 0, 0, HUE_COMPONENT_INDEX, hsxColorValues[HUE_COMPONENT_INDEX]);
                	resultMTBImg.putValueDouble( x, y, 0, 0, SATURATION_COMPONENT_INDEX, hsxColorValues[SATURATION_COMPONENT_INDEX]);
                	resultMTBImg.putValueDouble( x, y, 0, 0, X_COMPONENT_INDEX, hsxColorValues[X_COMPONENT_INDEX]);
                } else {
                    resultMTBImg.putValueInt( x, y, 0, 0, HUE_COMPONENT_INDEX, Float.valueOf( hueByte ).intValue() );
                    resultMTBImg.putValueInt( x, y, 0, 0, SATURATION_COMPONENT_INDEX, Float.valueOf( satByte ).intValue() );
                    resultMTBImg.putValueInt( x, y, 0, 0, X_COMPONENT_INDEX, Float.valueOf( xByte ).intValue() );               	
                }
            }
        }
        // the hue channel is shown by default
        resultMTBImg.setTitle( "HSX image" );
        // make a copy of each channel
        final MTBImage tempHueImage = resultMTBImg.getSlice( 0, 0, HUE_COMPONENT_INDEX );
        final MTBImage tempSatImage = resultMTBImg.getSlice( 0, 0, SATURATION_COMPONENT_INDEX );
        final MTBImage tempXImage = resultMTBImg.getSlice( 0, 0, X_COMPONENT_INDEX );
        // label it
        tempHueImage.setTitle( "Hue image" );
        tempSatImage.setTitle( "Saturation image" );

        final String xChannelName = mode.equals( RGBToHSXConverter.Mode.RGB_HSI_SONKA ) ? "Intensity "
                : mode.equals( RGBToHSXConverter.Mode.RGB_HSB_JRE ) ? "Brightness" : "Value ";

        tempXImage.setTitle( xChannelName + "image");
        // set the channel images
        setHueMTBImg(tempHueImage );
        setSatMTBImg( tempSatImage );
        setXMTBImg( tempXImage );
    }

    /**
     * Conversion from RGB to HSI color space according to Sonka et al.
     * 
     * @param rgbColor The array of red/green/blue-parts as ints.
     * @return The array of float containing the hue/saturation/intensity-parts in the range <code>0.0 : 1.0</code>.
     */
    private float[] rgbToHSI_Sonka( final int[] rgbColor ) {
        assert rgbColor != null;
        // hsi color array
        final float[] hsiColor = new float[THREE_COMPONENTS_COLOR_SPACE];
        // convert rgb-int values to float range( 0.0-1.0 )
        float redPart = rgbColor[RED_COMPONENT_INDEX] / 255F;
        float greenPart = rgbColor[GREEN_COMPONENT_INDEX] / 255F;
        float bluePart = rgbColor[BLUE_COMPONENT_INDEX] / 255F;

        //System.out.println( "RGB : " + redPart + "|" + greenPart + "|" + bluePart );
        float huePart = 0F;
        float saturationPart = 0F;
        float intensityPart = 0F;

        // Intensity
        //
        // complete black pixel -> insentity set to zero
        if ( redPart == 0 && greenPart == 0 && bluePart == 0 ) {
            intensityPart = 0F;
        }
        else {
            intensityPart = Double.valueOf( ( redPart + greenPart + bluePart ) / 3D ).floatValue();
        }

        // Hue
        //
        // grayscale image -> hue is undefined, NOT zero!
        // CAUTION: We got to set the hue value anyway, so it is important to not
        // confound this value with a regular hue value!
        if ( redPart == greenPart && greenPart == bluePart ) {
            huePart = HUE_UNDEFINED;
        }
        else {
            double divisor = ( ( redPart - greenPart ) + ( redPart - bluePart ) ) / 2D;
            //System.out.println( "Divi . " + divisor );
            double divident = Math.sqrt( ( redPart - greenPart ) * ( redPart - greenPart ) + ( ( redPart - bluePart ) * ( greenPart - bluePart ) ) );
            //System.out.println( "Divii : " + divident );
            huePart = Double.valueOf( Math.acos( divisor / divident ) ).floatValue();

        }
        
        // if intensity == 0 -> saturation is undefined -> set to 'SATURATION_UNDEFINED'
        if ( intensityPart == 0F ) {
            saturationPart = SATURATION_UNDEFINED;
        }
        else {
            saturationPart = Double.valueOf( 1D - ( 3D / ( redPart + greenPart + bluePart )
                    * Math.min( redPart, Math.min( greenPart, bluePart ) ) ) ).floatValue();
        }

        // check ?
        if ( bluePart / intensityPart > greenPart / intensityPart ) {
            huePart = Double.valueOf( Math.PI * 2D ).floatValue() - huePart;
        }
        
        // normalize to [0,1]
        huePart /= Double.valueOf( Math.PI * 2D ).floatValue();
       
        // set each part
        hsiColor[HUE_COMPONENT_INDEX] = huePart;
        hsiColor[SATURATION_COMPONENT_INDEX] = saturationPart;
        hsiColor[X_COMPONENT_INDEX] = intensityPart;

        return hsiColor;
    }

    /**
     * Conversion from RGB into HSB color space.
     * <p/>
     * This method simply calls the <code>RGBtoHSB</code> method containing in
     * the jre, in the <code>java.awt.Color</code>-class.
     * 
     * @see java.awt.Color#RGBtoHSB(int, int, int, float[])
     * @param rgbColor The array of red/green/blue-parts as ints.
     * @return The array of float containing the
     *         hue/saturation/brightness-parts in the range <code>0.0 : 1.0</code>.
     */
    private float[] rgbToHSB_JRE( final int[] rgbColor ) {
        assert rgbColor != null;

        return Color.RGBtoHSB( rgbColor[RED_COMPONENT_INDEX], rgbColor[GREEN_COMPONENT_INDEX], rgbColor[BLUE_COMPONENT_INDEX], null );
    }

    /**
     * Conversion from RGB into HSV color space.
     * <p>
     * Code modified from : <a href="http://www.easyrgb.com/">Easy RGB</a>. A
     * short description of the hsv/hsb colorspace :
     * <a href="http://en.wikipedia.org/wiki/HSV_color_space">Wikipedia</a>.
     * 
     * @param rgbColor The array of red/green/blue-parts as ints.
     * @return The array of float containing the hue/saturation/value-parts 
     * in the range <code>0.0 : 1.0</code>.
     */
    private float[] rgbToHSV_EasyRGB( final int[] rgbColor ) {
        assert rgbColor != null;
        final float[] hsvColor = new float[THREE_COMPONENTS_COLOR_SPACE];
        // convert rgb-int values to float range( 0.0-1.0 )
        float redPart = rgbColor[RED_COMPONENT_INDEX] / 255F;
        float greenPart = rgbColor[GREEN_COMPONENT_INDEX] / 255F;
        float bluePart = rgbColor[BLUE_COMPONENT_INDEX] / 255F;
        // the 
        float huePart = 0F;
        float saturationPart = 0F;
        float valuePart = 0F;

        // min RGB-value
        float minValue = Math.min( redPart, Math.min( greenPart, bluePart ) );
        // Max RGB-value
        float maxValue = Math.max( redPart, Math.max( greenPart, bluePart ) );
        // Color range
        float deltaMax = maxValue - minValue;

        // Value is set
        valuePart = maxValue;
        // if max == min -> grayscale image
        if ( maxValue == minValue ) {
        	// this means r == g == b and hue is undefined
        	// if min == max == 0 then also saturation is undefined
            huePart = 0F;
            saturationPart = 0F;
        } // Color image
        else {
            saturationPart = deltaMax / maxValue;
            float deltaRed = ( ( ( maxValue - redPart ) / 6F ) + ( deltaMax / 2F ) ) / deltaMax;
            float deltaGreen = ( ( ( maxValue - greenPart ) / 6F ) + ( deltaMax / 2F ) ) / deltaMax;
            float deltaBlue = ( ( ( maxValue - bluePart ) / 6F ) + ( deltaMax / 2F ) ) / deltaMax;

            // 
            if ( redPart == maxValue ) {
                huePart = deltaBlue - deltaGreen;
            }
            //
            else if ( greenPart == maxValue ) {
                huePart = ( 1F / 3F ) + deltaRed - deltaBlue;
            }
            //
            else if ( bluePart == maxValue ) {
                huePart = ( 2F / 3F ) + deltaGreen - deltaRed;
            }

            /**
             * check for value range violations
             */
            if ( huePart < 0F ) {
                huePart += 1F;
            }
            if ( huePart > 1F ) {
                huePart -= 1F;
            }
        }

        // set each part
        hsvColor[HUE_COMPONENT_INDEX] = huePart;
        hsvColor[SATURATION_COMPONENT_INDEX] = saturationPart;
        hsvColor[X_COMPONENT_INDEX] = valuePart;

        return hsvColor;
    }

    /**
     * Return {@code true} if the hue channel is undefined which is true is
     * saturation is zero or undefined which in turn is encoded as zero.
     *
     * @param saturation
     * @return
     */
    public static final boolean isHueUndefined( final float saturation ) {
        return saturation == SATURATION_UNDEFINED;
    }

    /** Return {@code true} if the saturation channel is undefined which is true
     * if the x channel is zero.
     * 
     * @param xValue
     * @return
     */
    public static final boolean isSaturationUndefined( final float xValue ) {
        return xValue == 0F;
    }
    
    /**
     * Return {@code true} if the hue channel is undefined which is true is
     * saturation is zero or undefined which in turn is encoded as zero.
     *
     * @param saturation
     * @return
     */
    public static final boolean isHueUndefined( final int saturation ) {
        return saturation == SATURATION_UNDEFINED;
    }

    /** Return {@code true} if the saturation channel is undefined which is true
     * if the x channel is zero.
     * 
     * @param xValue
     * @return
     */
    public static final boolean isSaturationUndefined( final int xValue ) {
        return xValue == 0;
    }

}
