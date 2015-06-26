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

/* 
 * Most recent change(s):
 * 
 * $Rev$
 * $Date$
 * $Author$
 * 
 */

package de.unihalle.informatik.MiToBo.fields;

import java.io.IOException;
import Jama.Matrix;

import de.jstacs.algorithms.optimization.*;
import de.jstacs.algorithms.optimization.termination.SmallDifferenceOfFunctionEvaluationsCondition;
import de.jstacs.utils.*;

import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo;
import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo.ExportPolicy;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBVectorField2D;

/**
 * Routines for reconstructing potential from vector field.
 * 
 * @see MTBVectorField2D
 * 
 * 
 * @author moeller
 */
@ALDMetaInfo(export = ExportPolicy.ALLOWED)
public class VectorField2DPotentialFinder {

		/**
		 * Vector field the potential is reconstructed for.
		 */
		protected MTBVectorField2D field;

		/**
		 * Standard constructor.
		 */
		public VectorField2DPotentialFinder() {
				this.field = new MTBVectorField2D();
		}

		/**
		 * Default constructor.
		 * 
		 * @param f
		 *          Vector field.
		 */
		public VectorField2DPotentialFinder(MTBVectorField2D f) {
				this.field = f;
		}

		/**
		 * Exact least squares reconstruction of the potential.
		 * <p>
		 * Attention!!! This function takes a lot of memory! Never use it with fields
		 * larger than approx. 30 x 30 pixels!!!
		 * <p>
		 * The function reconstructs the potential by solving a linear system of
		 * equations via explicit LQ-decomposition. The linear system contains a lot
		 * of rows and columns:
		 * <ul>
		 * <li>#cols = number of pixels in the image
		 * <li>#rows = approx. 2 * number of pixels in the image
		 * </ul>
		 * As a consequence the function should only be applied to quite small images
		 * not exceeding 30 pixels in width or height.
		 * 
		 * @param ignoreBorder
		 *          If true, first/last column/row of U and V will be ignored.
		 * @return Calculated potential field.
		 */
		public double[] calcPotential_exactLeastSquares(boolean ignoreBorder) {

				// Security check! This function might cause computers to crash!
				// Verify that the user is really sure what he/she is doing!!!
				if (this.field.getFieldSizeX() > 30 || this.field.getFieldSizeY() > 30) {
						System.err
						    .println("=================================================================");
						System.err
						    .println("= ATTENTION!!!                                                  =");
						System.err
						    .println("=                                                               =");
						System.err
						    .println("= You called the following method:                              =");
						System.err
						    .println("= VectorField2DPotentialFinder::calcPotential_exactLeastSquares =");
						System.err
						    .println("=                                                               =");
						System.err
						    .println("= Your vector field is larger than 30 x 30 pixels!              =");
						System.err
						    .println("= If you are not working with a computer of type BlueGene or    =");
						System.err
						    .println("= something comparable, than think again about what you are     =");
						System.err
						    .println("= doing! If you continue, this function will most probably      =");
						System.err
						    .println("= cause your machine to hang or even crash!                     =");
						System.err
						    .println("=                                                               =");
						System.err
						    .println("= Still sure that you would like to continue?                   =");
						System.err
						    .println("= If so, press <return> and enjoy the things coming up... ;-)   =");
						System.err
						    .println("= If not, better press <strg> + <c> for exit.                   =");
						System.err
						    .println("=================================================================");
						try {
								System.in.read();
						} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
						}
				}

				int fw = this.field.getFieldSizeX();
				int fh = this.field.getFieldSizeY();

				double[][] aData = new double[(fw - 1) * fh + fw * (fh - 1)][fw * fh];
				// fill upper half
				for (int block = 0; block < fh; ++block) {
						for (int line = 0; line < fw - 1; ++line) {
								aData[line + block * fh - block * 1][line + fh * block] = -1;
								aData[line + block * fh - block * 1][line + fh * block + 1] = 1;
						}
				}
				// fill lower half
				int rowOffset = fh * (fw - 1);
				for (int block = 0; block < fh - 1; ++block) {
						for (int line = 0; line < fw; ++line) {
								aData[rowOffset + line + block * fh][line + fh * block] = -1;
								aData[rowOffset + line + block * fh][line + fh * block + fw] = 1;
						}
				}
				Matrix A = new Matrix(aData);

				double[][] bData = new double[fh * (fw - 1) + fh * (fw - 1)][1];
				int i = 0;
				for (int y = 0; y < fh; ++y) {
						for (int x = 0; x < fw - 1; ++x) {
								bData[i][0] = this.field.getValueU(x, y);
								++i;
						}
				}
				i = fh * (fw - 1);
				for (int y = 0; y < fh - 1; ++y) {
						for (int x = 0; x < fw; ++x) {
								bData[i][0] = this.field.getValueV(x, y);
								++i;
						}
				}
				Matrix B = new Matrix(bData);

				// dimension reduction
				Matrix A_T = A.transpose();
				Matrix A_s = A_T.times(A);
				Matrix B_s = A_T.times(B);

				// solve linear system of equations
				Matrix x = A_s.solve(B_s);

				// return result
				double[] result = x.getRowPackedCopy();
				this.normalizePotentialToMin(result, ignoreBorder);
				return result;
		}

		/**
		 * Reconstruct potential via gradient descent.
		 * 
		 * @param ignoreBorder
		 *          If true, border pixels are ignored in reconstruction.
		 * @return Calculated potential field.
		 */
		public double[] calcPotential_gradientDescent(boolean ignoreBorder) {

				PotentialFinderOptFunction goalF = this.new PotentialFinderOptFunction(
				    ignoreBorder);
				// double [] init= this.calcPotential_incrementalLeastSquares();

				double[] init = new double[this.field.getFieldSizeX()
				    * this.field.getFieldSizeY()];
				for (int i = 0; i < init.length; ++i)
						init[i] = 1;

				LimitedMedianStartDistance forecast_limit = new LimitedMedianStartDistance(
				    5, 0.1);
				// SafeOutputStream os= new SafeOutputStream(System.out);
				SafeOutputStream os = SafeOutputStream.getSafeOutputStream(null);
				try {
						Optimizer.optimize(
								Optimizer.CONJUGATE_GRADIENTS_FR,
								goalF,
								init,
								new SmallDifferenceOfFunctionEvaluationsCondition(0.000001),
								0.0001, forecast_limit, os);
				} catch (DimensionException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
				} catch (TerminationException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
				} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
				} catch (EvaluationException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
				} catch (Exception e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
        }
				this.normalizePotentialToMin(init, ignoreBorder);
				return init;
		}

		/**
		 * Reconstructs the potential of the vector field incrementally.
		 * <p>
		 * Starting with last entry field bottom right in the vector field the
		 * potential is reconstructed incrementally by least square solutions of the
		 * two given constraint equations for each image pixel. The solution is only
		 * approximative as no global optimization criterion is considered.
		 * Nevertheless, it might be used as initialization for the gradient descent
		 * routines, if it is not of direct interest.
		 * 
		 * @param ignoreBorder
		 *          If true, first/last row/column of U and V are ignored.
		 */
		public double[] calcPotential_incrementalLeastSquares(boolean ignoreBorder) {

				int fw = this.field.getFieldSizeX();
				int fh = this.field.getFieldSizeY();

				// allocate memory for result data
				double[][] result = new double[fh][fw];

				// we do not have any information about the first entry, fix it
				if (ignoreBorder)
						result[fh - 2][fw - 2] = 1.0;
				else
						result[fh - 1][fw - 1] = 1.0;

				// calculate entries for last row based on x-derivative
				if (!ignoreBorder)
						for (int x = fw - 2; x >= 0; --x) {
								result[fh - 1][x] = result[fh - 1][x + 1]
								    - this.field.getValueU(x, (fh - 1));
						}
				else
						for (int x = fw - 3; x >= 1; --x) {
								result[fh - 2][x] = result[fh - 2][x + 1]
								    - this.field.getValueV(x, (fh - 2));
						}

				// fill rest of the matrix
				int yStart = 0, yEnd = 0, xStart = 0, xEnd = 0;
				if (ignoreBorder) {
						yStart = fh - 3;
						yEnd = 1;
						xStart = fw - 2;
						xEnd = 1;
				} else {
						yStart = fh - 2;
						yEnd = 0;
						xStart = fw - 1;
						xEnd = 0;
				}
				for (int y = yStart; y >= yEnd; --y) {
						// first entry in a column comes from the next row
						result[y][xStart] = result[y + 1][xStart]
						    - this.field.getValueV(xStart, y);
						// fill rest of the row
						for (int x = xStart - 1; x >= xEnd; --x) {
								double a = result[y + 1][x];
								double b = result[y][x + 1];
								double u = this.field.getValueU(x, y);
								double v = this.field.getValueV(x, y);

								double[][] aData = new double[2][1];
								aData[0][0] = 1;
								aData[1][0] = 1;

								Matrix A = new Matrix(aData);

								double[][] bData = new double[2][1];
								bData[0][0] = a - u;
								bData[1][0] = b - v;

								Matrix B = new Matrix(bData);

								// solve linear system of equations
								Matrix c = A.solve(B);
								result[y][x] = c.get(0, 0);
						}
				}
				double[] returnVec = new double[fh * fw];
				for (int y = 0; y < fh; ++y)
						for (int x = 0; x < fw; ++x)
								returnVec[y * fw + x] = result[y][x];
				this.normalizePotentialToMin(returnVec, ignoreBorder);
				return returnVec;
		}

		/**
		 * Function to normalize a potential to the minimum value of 0.
		 * 
		 * @param pot
		 *          Potential field to normalize.
		 * @param ignoreBorder
		 *          If true outer rows and cols are ignored.
		 */
		private void normalizePotentialToMin(double[] pot, boolean ignoreBorder) {

				int w = this.field.getFieldSizeX();
				int h = this.field.getFieldSizeY();

				double min = Double.MAX_VALUE;
				int iStart = 0, iEnd = 0, jStart = 0, jEnd = 0;
				if (ignoreBorder) {
						iStart = 1;
						iEnd = w - 2;
						jStart = 1;
						jEnd = h - 2;
				} else {
						iStart = 0;
						iEnd = w - 1;
						jStart = 0;
						jEnd = h - 1;
				}
				for (int j = jStart; j <= jEnd; ++j) {
						for (int i = iStart; i <= iEnd; ++i) {
								if (pot[j * w + i] < min)
										min = pot[j * w + i];
						}
				}
				for (int j = jStart; j <= jEnd; ++j) {
						for (int i = iStart; i <= iEnd; ++i) {
								pot[j * w + i] -= min;
						}
				}
				// set borders to zero
				if (ignoreBorder) {
						for (int x = 0; x < w; ++x) {
								pot[x] = 0;
								pot[(h - 1) * w + x] = 0;
						}
						for (int y = 0; y < h; ++y) {
								pot[y * w] = 0;
								pot[y * w + w - 1] = 0;
						}
				}
		}

		/**
		 * Optimization function for gradient-based potential reconstruction.
		 * <p>
		 * The class is derived from classed to be found in the Jstacs package to be
		 * found here: http://www.jstacs.de.
		 * 
		 * @author moeller
		 */
		protected class PotentialFinderOptFunction extends DifferentiableFunction {

				/**
				 * Flag to indicate how the border should be handled.
				 */
				private boolean ignoreBorder = false;

				/**
				 * Default constructor.
				 * 
				 * @param ignBorder
				 *          If true, border pixels are ignored.
				 */
				public PotentialFinderOptFunction(boolean ignBorder) {
						this.ignoreBorder = ignBorder;
				}

				@Override
				public double[] evaluateGradientOfFunction(double[] xval) {

						int w = VectorField2DPotentialFinder.this.field.getFieldSizeX();
						int h = VectorField2DPotentialFinder.this.field.getFieldSizeY();
						double[] U = VectorField2DPotentialFinder.this.field.getU();
						double[] V = VectorField2DPotentialFinder.this.field.getV();

						double[] diff = new double[w * h];
						int xStart = 0, xEnd = 0, yStart = 0, yEnd = 0;
						if (this.ignoreBorder) {
								xStart = yStart = 1;
								xEnd = w - 2;
								yEnd = h - 2;
						} else {
								xStart = yStart = 0;
								xEnd = w - 1;
								yEnd = h - 1;
						}
						for (int y = yStart; y <= yEnd; ++y) {
								for (int x = xStart; x <= xEnd; ++x) {
										diff[y * w + x] = 0;
										if (x >= xStart + 1)
												diff[y * w + x] += -2
												    * (U[y * w + x - 1] - xval[y * w + x] + xval[y * w + x - 1]);
										if (x <= xEnd - 1)
												diff[y * w + x] += 2 * (U[y * w + x] - xval[y * w + x + 1] + xval[y
												    * w + x]);
										if (y >= yStart + 1)
												diff[y * w + x] += -2
												    * (V[(y - 1) * w + x] - xval[y * w + x] + xval[(y - 1) * w + x]);
										if (y <= yEnd - 1)
												diff[y * w + x] += 2 * (V[(y) * w + x] - xval[(y + 1) * w + x] + xval[y
												    * w + x]);
								}
						}
						return diff;
				}

				@Override
				public double evaluateFunction(double[] xval) {

						double val = 0;
						int w = VectorField2DPotentialFinder.this.field.getFieldSizeX();
						int h = VectorField2DPotentialFinder.this.field.getFieldSizeY();
						double[] U = VectorField2DPotentialFinder.this.field.getU();
						double[] V = VectorField2DPotentialFinder.this.field.getV();

						int xStart = 0, xEnd = 0, yStart = 0, yEnd = 0;
						if (this.ignoreBorder) {
								xStart = yStart = 1;
								xEnd = w - 3;
								yEnd = h - 3;
						} else {
								xStart = yStart = 0;
								xEnd = w - 2;
								yEnd = h - 2;
						}

						for (int y = yStart; y <= yEnd; ++y) {
								for (int x = xStart; x <= xEnd; ++x) {
										val += (U[y * w + x] - xval[y * w + x + 1] + xval[y * w + x])
										    * (U[y * w + x] - xval[y * w + x + 1] + xval[y * w + x]);
										val += (V[y * w + x] - xval[(y + 1) * w + x] + xval[y * w + x])
										    * (V[y * w + x] - xval[(y + 1) * w + x] + xval[y * w + x]);
								}
						}
						return val;
				}

				@Override
				public int getDimensionOfScope() {
						int w = VectorField2DPotentialFinder.this.field.getFieldSizeX();
						int h = VectorField2DPotentialFinder.this.field.getFieldSizeY();
						return w * h;
				}
		}
}
