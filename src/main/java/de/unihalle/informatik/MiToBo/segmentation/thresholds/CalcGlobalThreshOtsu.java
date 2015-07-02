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

package de.unihalle.informatik.MiToBo.segmentation.thresholds;

import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException.OperatorExceptionType;
import de.unihalle.informatik.Alida.exceptions.ALDWorkflowException;
import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.Parameter.Direction;
import de.unihalle.informatik.Alida.annotations.Parameter.ParameterModificationMode;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBImageHistogram;
import de.unihalle.informatik.MiToBo.core.datatypes.images.*;
import de.unihalle.informatik.MiToBo.core.datatypes.wrapper.MTBDoubleData;
import de.unihalle.informatik.MiToBo.core.operator.*;

/**
 * This class implements Otsu's method for calculating an optimal 
 * threshold for a given grayscale images.
 * <p>
 * Basically, the optimal threshold is defined as the threshold which 
 * best separates the image histogram in two classes, more or less equal 
 * in size and well-separated in terms of their average intensities.
 * <p>
 * The threshold calculation is based on the following goal function,
 * 
 * {@latex.ilb %preamble{\\usepackage{amssymb,amsmath}} 
 * \\begin{eqnarray*} J(T) &=&
 * \\pi_{fg}(T) \\cdot \\pi_{bg}(T) 
 * 		\\cdot ( \\mu_{fg}(T) - \\mu_{bg}(T) )^2, \\\\
 * \\pi_{fg}(T) &=& \\text{a-priori probability of a pixel for 
 * 											belonging to the foreground}\\\\
 * \\pi_{bg}(T) &=& \\text{a-priori probability of a pixel for 
 * 											belonging	to the background}\\\\
 * \\mu_{fg}(T) &=& \\text{average intensity in foreground class}\\\\
 * \\mu_{bg}(T) &=& \\text{average intensity in background class}
 * \\end{eqnarray*}}
 * 
 * which is to be maximized with regard to threshold T. Maximization is
 * done by explicitly calculating the value of the goal function for
 * all possible thresholds T and searching for the maximum value.
 * <p>
 * Since the number of possible thresholds is directly linked to the 
 * number of histogram bins and, hence, to the computation time 
 * the maximum number of possible bins is restricted to 1024 in this 
 * implementation to keep the computational effort acceptable.
 * <p>
 * Reference: <i>Nobuyuki Otsu, "A threshold selection method from 
 * gray-level histograms", IEEE Trans. Sys., Man., Cyber. 9 (1): 62–66,
 * 1979. </i>
 * 
 * @author Birgit Moeller
 */
@SuppressWarnings("javadoc")
@ALDAOperator(genericExecutionMode = ALDAOperator.ExecutionMode.ALL)
public class CalcGlobalThreshOtsu extends MTBOperator {

		/**
		 * Type of input to work on.
		 */
		public static enum InputType {
				/**
				 * Input data is provided as image.
				 */
				IMAGE,
				/**
				 * Input data is provided as histogram.
				 */
				HISTOGRAM
		}

		/**
		 * Input type.
		 */
		@Parameter(label = "Input Type", required = true, dataIOOrder = 0, 
			supplemental = false, callback = "inputTypeChanged", 
			direction = Direction.IN, description = "Type of input data.", 
			paramModificationMode = ParameterModificationMode.MODIFIES_INTERFACE)
		private InputType inType = InputType.IMAGE;

		/**
		 * Input image.
		 */
		@Parameter(label = "Input Image", required = true, supplemental = false, dataIOOrder = 1, direction = Direction.IN, description = "Input image.")
		private transient MTBImage inImg = null;

		/**
		 * Input histogram.
		 */
		@Parameter(label = "Input Histogram", required = true, supplemental = false, dataIOOrder = 1, direction = Direction.IN, description = "Input histogram")
		private transient MTBImageHistogram inHisto = null;

		/**
		 * Result threshold.
		 */
		@Parameter(label = "Threshold", supplemental = false, direction = Direction.OUT, description = "Result threshold")
		private MTBDoubleData threshold = null;

		/**
		 * Default constructor.
		 * 
		 * @throws ALDOperatorException Thrown if construction fails.
		 */
		public CalcGlobalThreshOtsu() throws ALDOperatorException {
				this.setParameter("inType", InputType.IMAGE);
		}

		/**
		 * Default constructor with input image as argument.
		 * 
		 * @param img
		 *          Input image.
		 * @throws ALDOperatorException Thrown if construction fails.
		 */
		public CalcGlobalThreshOtsu(MTBImage img) throws ALDOperatorException {
				this.setParameter("inType", InputType.IMAGE);
				this.inImg = img;
		}

		/**
		 * Default constructor with input histogram as argument.
		 * 
		 * @param histo
		 *          Input histogram.
		 * @throws ALDOperatorException Thrown if construction fails.
		 */
		public CalcGlobalThreshOtsu(MTBImageHistogram histo)
		    throws ALDOperatorException {
				this.setParameter("inType", InputType.HISTOGRAM);
				this.inHisto = histo;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see de.unihalle.informatik.Alida.operator.ALDOperator#validateCustom()
		 */
		@Override
		public void validateCustom() throws ALDOperatorException {
				if (this.inImg == null && this.inHisto == null)
						throw new ALDOperatorException(OperatorExceptionType.OPERATE_FAILED,
						    "[CalcGlobalThreshOtsu] "
						        + "No input data, specify either an image or a histogram!");
		}

		/**
		 * Get the resulting threshold.
		 * 
		 * @return Otsu threshold.
		 */
		public MTBDoubleData getOtsuThreshold() {
				return this.threshold;
		}

		@Override
		protected void operate() {
			if (this.verbose.booleanValue())
				System.out.println("[CalcGlobalThreshOtsu] Processing image...");
			if (this.inImg != null)
				this.threshold = this.calcThreshold(this.inImg);
			else {
				// copy input histogram since histogram will be modified
				// during Otsu calculations
				MTBImageHistogram workHisto = this.inHisto.duplicate();
				this.threshold = this.calcThreshold(workHisto);
			}
		}

		/**
		 * Calculate Otsu threshold on given image.
		 * 
		 * @param image
		 *          MiToBo image.
		 * @return Calculated Otsu threshold.
		 */
		private MTBDoubleData calcThreshold(MTBImage image) {

				// calculate histogram from given image,
				// but with not less than 128 bins neither more than 1024 bins
				double minBinValue = image.getMinMaxDouble()[0];
				double maxBinValue = image.getMinMaxDouble()[1];

				if (this.verbose.booleanValue()) {
						System.out.println("[CalcGlobalThreshOtsu] Min. histo entry = "
						    + minBinValue);
						System.out.println("[CalcGlobalThreshOtsu] Max. histo entry = "
						    + maxBinValue);
				}

				if(minBinValue == maxBinValue)	// if there is only one pixel value present, return this value
				{
					return new MTBDoubleData(minBinValue);
				}
				
				int binCount = (int) (maxBinValue - minBinValue + 1);
				if (binCount < 128)
						binCount = 128;
				if (binCount > 1024)
						binCount = 1024;

				if (this.verbose.booleanValue()) {
						System.out.println("[CalcGlobalThreshOtsu] Using " + binCount + " bins.");
				}
				MTBImageHistogram histo = new MTBImageHistogram(image, binCount,
				    minBinValue, maxBinValue);
				return this.calcThreshold(histo);
		}

		/**
		 * Calculate Otsu threshold on given histogram.
		 * 
		 * @param histo
		 *          Histogram.
		 * @return Calculated Otsu threshold.
		 */
		private MTBDoubleData calcThreshold(MTBImageHistogram histo) {

				int minBinID = 0;
				int maxBinID = histo.getSize() - 1;
				int binCount = histo.getSize();

				// normalize histogram
				histo.normalize();

				// calculate mean
				double mean = histo.getMean();
				if (this.verbose.booleanValue()) {
						System.out.println("[CalcGlobalThreshOtsu] Mean value = " + mean);
				}

				// search threshold
				double j_max = -1;

				int thresh = 0;
				for (int t = 0; t < binCount; ++t) {

						double omega_1 = 0;
						for (int i = minBinID; i <= t; ++i)
								omega_1 = omega_1 + histo.getBinValue(i);
						double omega_2 = 0;
						for (int i = t + 1; i <= maxBinID; ++i)
								omega_2 = omega_2 + histo.getBinValue(i);

						double mu_1 = 0;
						for (int i = minBinID; i <= t; ++i)
								mu_1 = mu_1 + histo.mapIndexToValue(i) * histo.getBinValue(i) / omega_1;
						double mu_2 = 0;
						for (int i = t + 1; i <= maxBinID; ++i)
								mu_2 = mu_2 + histo.mapIndexToValue(i) * histo.getBinValue(i) / omega_2;

						if (this.verbose.booleanValue()) {
								System.out.print("[CalcGlobalThreshOtsu] Testing "
								    + histo.mapIndexToValue(t));
								System.out.print("... J =  "
								    + (omega_1 * (mu_1 - mean) * (mu_1 - mean) + omega_2
								        * (mu_2 - mean) * (mu_2 - mean)) + "\n");
						}
						if (omega_1 * (mu_1 - mean) * (mu_1 - mean) + omega_2 * (mu_2 - mean)
						    * (mu_2 - mean) > j_max) {
								j_max = omega_1 * (mu_1 - mean) * (mu_1 - mean) + omega_2
								    * (mu_2 - mean) * (mu_2 - mean);
								thresh = t;
						}
				}
				if (this.verbose.booleanValue()) {
						System.out.println("[CalcGlobalThreshOtsu] Otsu threshold = "
						    + histo.mapIndexToValue(thresh));
				}
				return new MTBDoubleData(new Double(histo.mapIndexToValue(thresh)));
		}

		/**
		 * Callback routine to change parameters on change of input type.
		 */
		@SuppressWarnings("unused")
		private void inputTypeChanged() {
				try {
						if (this.inType == InputType.IMAGE) {
								if (this.hasParameter("inHisto")) {
										this.removeParameter("inHisto");
								}

								if (!this.hasParameter("inImg")) {
										this.addParameter("inImg");
								}
						} else if (this.inType == InputType.HISTOGRAM) {
								if (this.hasParameter("inImg")) {
										this.removeParameter("inImg");
								}

								if (!this.hasParameter("inHisto")) {
										this.addParameter("inHisto");
								}
						}
						// add logging messages (FATAL) as soon as logj4 is configured
				} catch (SecurityException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
				} catch (ALDOperatorException e) {
						e.printStackTrace();
				}
		}
}
