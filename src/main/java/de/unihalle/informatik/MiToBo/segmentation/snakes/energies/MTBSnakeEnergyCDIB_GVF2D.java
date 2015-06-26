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

package de.unihalle.informatik.MiToBo.segmentation.snakes.energies;

import ij.IJ;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import de.unihalle.informatik.Alida.annotations.ALDClassParameter;
import de.unihalle.informatik.Alida.annotations.ALDDerivedClass;
import de.unihalle.informatik.Alida.annotations.ALDParametrizedClass;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBVectorField2D;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.fields.GVFFieldCalculator2D;
import de.unihalle.informatik.MiToBo.segmentation.snakes.optimize.SnakeOptimizerSingle;

/**
 * Class for external energy of a GVF field energy vector. The GVF field from a
 * given image returns to energy flow (vector for x- and y-direction). Every
 * energy vector is stored in a one dimensional array, so the width of the image
 * is used to get a energy value at position (x,y). The GVF field can be
 * normalized in the range[-1,1], see `normalizeEnergy` method.
 * 
 * @see GVFFieldCalculator2D
 * 
 * 
 * @author Danny Misiak
 */
@ALDDerivedClass
@ALDParametrizedClass
public class MTBSnakeEnergyCDIB_GVF2D extends MTBSnakeEnergyCDImageBased {

		@ALDClassParameter(label = "Input Image")
		private MTBImage image;

		@ALDClassParameter(label = "Number of Iterations")
		private int numIterations = 120;

		/**
		 * The resulting gradient vector flow field.
		 */
		MTBVectorField2D gvf;

		/**
		 * Potential field reconstructed from GVF.
		 */
		private double[] energyPotential;

		/**
		 * Default constructor.
		 */
		public MTBSnakeEnergyCDIB_GVF2D() {
				// nothing to do here
		}

		/**
		 * Constructor to create a new SnakeExternalEnergyGVF2D object.
		 * 
		 * @param image
		 *          input image for calculating the gvf field
		 * @param numIterations
		 *          number of iterations used for calculating the gvf field
		 * 
		 */
		public MTBSnakeEnergyCDIB_GVF2D(MTBImage _image, int _numIterations) {
				this.image = _image;
				this.numIterations = _numIterations;
		}

		@Override
		public boolean initEnergy(SnakeOptimizerSingle o) {
				if (this.image == null) {
						this.image = o.getWorkingImage();
				}
				GVFFieldCalculator2D gvfCalc;
				try {
						gvfCalc = new GVFFieldCalculator2D(this.image, this.numIterations);
						gvfCalc.runOp(null);
						this.gvf = gvfCalc.getVectorField();
						this.width = gvfCalc.getInputImage().getSizeX();
						this.height = gvfCalc.getInputImage().getSizeY();
				} catch (ALDOperatorException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
				} catch (ALDProcessingDAGException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
				}
				// reconstruct the potential
				this.gvf.resetPotential();
				this.energyPotential = this.gvf.calcPotential_approxLeastSquares(true);
				return super.initEnergy(o);
		}

		/**
		 * Get the calculated Gradient Vector Flow field.
		 * 
		 * @return The GVF field as VectorField2D object.
		 */
		public MTBVectorField2D getGVF() {
				return this.gvf;
		}

		/**
		 * Get the energy potential field from this external energy.
		 * 
		 * @return Potential field in a double array.
		 */
		public double[] getPotential() {
				return this.energyPotential.clone();
		}

		/**
		 * Returns the value of the external energy at the given position.
		 * <p>
		 * In case of GVFs the external energy is given by a potential field
		 * reconstructed from the given GVF. Since the potential reconstruction yields
		 * only approximate results, the energy values should be interpreted as a more
		 * or less good approximation to the real potential.
		 * <p>
		 * The potential is normalized so that the minimum equals 0.
		 * 
		 * @param x
		 *          x-coordinate of position.
		 * @param y
		 *          y-coordinate of position.
		 * @return Absolute value of external energy (always positive!).
		 */
		@Override
		public double getValue(double x, double y) {
				int px = (int) Math.round(x);
				int py = (int) Math.round(y);
				return this.energyPotential[py * this.width + px];
		}

		/**
		 * Get x-derivative of external snake energy at given position.
		 * 
		 * @param x
		 *          x-coordinate of pixel position
		 * @param y
		 *          y-coordinate of pixel position
		 * @return Negative x-derivative value at given position.
		 */
		@Override
    public double getDerivativeX(double x, double y) {
				int px = (int) Math.round(x);
				int py = (int) Math.round(y);
				return ((-1) * (this.gvf.getValueU(px, py)));
		}

		/**
		 * Get y-derivative of external snake energy at given position.
		 * 
		 * @param x
		 *          x-coordinate of pixel position
		 * @param y
		 *          y-coordinate of pixel position
		 * @return Negative y-derivative value at given position.
		 */
		@Override
    public double getDerivativeY(double x, double y) {
				int px = (int) Math.round(x);
				int py = (int) Math.round(y);
				return ((-1) * (this.gvf.getValueV(px, py)));
		}

		/**
		 * Save the gvf field in a MATLAB like file.
		 * 
		 * @param file
		 *          complete path to the file which should be saved
		 */
		public void saveExtEnergy(String file) {
				StringBuffer input = new StringBuffer();
				input.insert(0, "x = [");
				File output;
				FileWriter fw;
				BufferedWriter bw;
				double count = 0;
				for (int i = 0; i < this.height; i++) {
						double proz = (count) / (this.height);
						IJ.showProgress(proz);
						count += 0.5;
						for (int j = 0; j < this.width; j++) {
								if (j < this.width - 1)
										input.insert(input.length(), this.gvf.getValueU(j, i) + ",");
								else
										input.insert(input.length(), this.gvf.getValueU(j, i) + ";\n");
						}
				}
				input.insert(input.length(), "];\n\ny = [");
				for (int i = 0; i < this.height; i++) {
						double proz = (count) / (this.height);
						IJ.showProgress(proz);
						count += 0.5;
						for (int j = 0; j < this.width; j++) {
								if (j < this.width - 1)
										input.insert(input.length(), this.gvf.getValueV(j, i) + ",");
								else
										input.insert(input.length(), this.gvf.getValueV(j, i) + ";\n");
						}
				}
				input.insert(input.length(), "];\n");
				try {
						output = new File(file);
						fw = new FileWriter(output);
						bw = new BufferedWriter(fw);
						bw.write(input.toString());
						bw.close();
						System.out.println("   --> gvf saved to: " + file);
				} catch (ArrayIndexOutOfBoundsException aioobe) {
						System.out.println("Error writing GVF: " + aioobe);
				} catch (IOException ioe) {
						System.out.println("Error writing GVF: " + ioe);
				}
		}

		/**
		 * <pre>
		 * 
		 * Normalization of the GVF field.
		 * 
		 * !!! ATTENTION: the GVF field is normalized in range [-1.0, 1.0] !!!
		 *                Independent of the given lower or upper bound values,
		 *                because the gradient can be negative.
		 * </pre>
		 * 
		 * @throws ALDOperatorException
		 * @throws ALDOperatorException
		 */
		@Override
		public void normalizeEnergy() {
				double maxX = 0.0;
				double maxY = 0.0;
				for (int y = 0; y < this.height; y++) {
						for (int x = 0; x < this.width; ++x) {
								if (Math.abs(this.gvf.getValueU(x, y)) > maxX)
										maxX = Math.abs(this.gvf.getValueU(x, y));
						}
				}
				for (int y = 0; y < this.height; y++) {
						for (int x = 0; x < this.width; ++x) {
								if (Math.abs(this.gvf.getValueV(x, y)) > maxY)
										maxY = Math.abs(this.gvf.getValueV(x, y));
						}
				}
				for (int y = 0; y < this.height; y++) {
						for (int x = 0; x < this.width; ++x) {
								try {
										this.gvf.setU(x, y, this.gvf.getValueU(x, y) / maxX);
								} catch (ALDOperatorException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
								}
						}
				}
				for (int y = 0; y < this.height; y++) {
						for (int x = 0; x < this.width; ++x) {
								try {
										this.gvf.setV(x, y, this.gvf.getValueV(x, y) / maxY);
								} catch (ALDOperatorException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
								}
						}
				}
		}

		@Override
		public String toString() {
				return new String("SnakeExtEnergy-GVF");
		}
}
