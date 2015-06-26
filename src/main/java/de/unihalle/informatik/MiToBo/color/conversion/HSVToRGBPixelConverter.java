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

package de.unihalle.informatik.MiToBo.color.conversion;

import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.MiToBo.core.datatypes.defines.MTBConstants;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;

/**
 * <pre>
 * Class to convert values from HSV color space into other color spaces like RGB
 * or so.
 * 
 * H: color value.
 * S: saturation of color.
 * V: brightness value of color.
 * 
 * Ranges of HSI:
 *   H in [0,360) (representing degrees),
 *   S and V in [0, 1] (representing [0, 100 %]).
 * </pre>
 * <p>
 * This implementation is taken from <br>
 * W. Burger/M. Burge, "Digitale Bildverarbeitung", pp.256, Springer, 
 * 2nd edition, 2006.
 * 
 * @author Danny Misiak
 */

public class HSVToRGBPixelConverter extends MTBOperator {
  /**
   * Hue of HSV space.
   */
	@Parameter( label= "Hue", required = true, dataIOOrder = 1, 
		direction = Parameter.Direction.IN, description = "Hue input value.")
  private double h;
  /**
   * Saturation of HSV space.
   */
	@Parameter( label= "Saturation", required = true, dataIOOrder = 2, 
		direction = Parameter.Direction.IN, description = "Saturation input value.")
  private double s;
  /**
   * Intensity of HSV space.
   */
	@Parameter( label= "Brightness Value", required = true, dataIOOrder = 3, 
		direction = Parameter.Direction.IN, description = "Brightness input value.")
  private double v;

	/**
	 * Resulting RGB values.
	 */
	@Parameter( label= "RGB values", required = true, dataIOOrder = 1, 
		direction = Parameter.Direction.OUT, description = "Result.")
  private int[] rgbColor;

	/**
   * Standard constructor.
   */
  public HSVToRGBPixelConverter() throws ALDOperatorException {
    this.h = 0.0;
    this.s = 0.0;
    this.v = 0.0;
  }

  /**
   * Constructor to create a HSVconverter object
   * 
   * @param _h
   *          hue of HSV space
   * @param _s
   *          saturation of HSV space
   * @param _v
   *          brightness value of HSV space
   */
  public HSVToRGBPixelConverter(double _h, double _s, double _v) 
  		throws ALDOperatorException {
    this.h = _h;
    this.s = _s;
    this.v = _v;
  }

  /**
   * Get RGB values.
   * @return	Array of R, G and B value.
   */
  public int[] getRGBResult() {
  	return this.rgbColor;
  }
  
	@Override
  protected void operate() {
		this.rgbColor = toRGB(this.h, this.s, this.v);
  }
	
  /**
   * Method to convert a single color from HSV color space into RGB color space.
   * 
   * @return Integer array with values for r,g,b.
   */
  protected static int[] toRGB(double _h, double _s, double _v) {
    int[] color = new int[3];
    // make sure arguments stay in range [0,360) for H and in [0,1] for S,V
    double h = Math.max(0, Math.min(360.0, _h));
    double s = Math.max(0, Math.min(1.0, _s));
    double v = Math.max(0, Math.min(1.0, _v));
    // test if color is monochrome (gray)
    if (s < MTBConstants.epsilon) {
      color[0] = (int) Math.round(v * 255);
      color[1] = (int) Math.round(v * 255);
      color[2] = (int) Math.round(v * 255);
    } else {
      int Hi = (int) Math.floor(h / 60.0);
      double f = (h / 60) - Hi;
      double p = v * (1 - s);
      double q = v * (1 - s * f);
      double t = v * (1 - s * (1 - f));
      // calculate r,g,b values for RGB color space
      switch (Hi) {
        case 0:
          color[0] = (int) Math.round(v * 255);
          color[1] = (int) Math.round(t * 255);
          color[2] = (int) Math.round(p * 255);
          break;
        case 1:
          color[0] = (int) Math.round(q * 255);
          color[1] = (int) Math.round(v * 255);
          color[2] = (int) Math.round(p * 255);
          break;
        case 2:
          color[0] = (int) Math.round(p * 255);
          color[1] = (int) Math.round(v * 255);
          color[2] = (int) Math.round(t * 255);
          break;
        case 3:
          color[0] = (int) Math.round(p * 255);
          color[1] = (int) Math.round(q * 255);
          color[2] = (int) Math.round(v * 255);
          break;
        case 4:
          color[0] = (int) Math.round(t * 255);
          color[1] = (int) Math.round(p * 255);
          color[2] = (int) Math.round(v * 255);
          break;
        case 5:
          color[0] = (int) Math.round(v * 255);
          color[1] = (int) Math.round(p * 255);
          color[2] = (int) Math.round(q * 255);
          break;
        case 6:
          color[0] = (int) Math.round(v * 255);
          color[1] = (int) Math.round(t * 255);
          color[2] = (int) Math.round(p * 255);
          break;
      }
    }
    return color;
  }
}
