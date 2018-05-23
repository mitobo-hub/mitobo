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

package de.unihalle.informatik.MiToBo.core.datatypes;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo;
import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo.ExportPolicy;
import de.unihalle.informatik.Alida.annotations.ALDParametrizedClass;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.operator.ALDData;
import de.unihalle.informatik.MiToBo.fields.VectorField2DPotentialFinder;

/**
 * Class to represent several kinds of vector fields. For example gvf-fields. A
 * vector field has two field vectors, U and V. For the first time a kind of
 * basic methods are implemented and implementation of other methods will be
 * continued.
 * 
 * 
 * @author moeller, misiak
 */
@ALDMetaInfo(export = ExportPolicy.ALLOWED)
@ALDParametrizedClass
public class MTBVectorField2D extends ALDData {
		/**
		 * Vector U including the x-values of the vector field.
		 */
		@Parameter
		private double[] U = new double[0];
		/**
		 * Vector V including the y-values of the vector field.
		 */
		@Parameter
		private double[] V = new double[0];
		/**
		 * Width of the image where the vector field comes from. The width is
		 * necessary to calculate the right position in the vectors U and V, because
		 * the values are stored in a one dimensional array of length (width *
		 * height). To get a value on a position (x,y) of U or V you must calculate
		 * the right position in the vector by calculating the position = (y * width +
		 * height).
		 */
		@Parameter
		private int width;
		
		/**
		 * Height of the image where the vector field comes from.
		 */
		@Parameter
		private int height;

		/**
		 * Helper object for reconstructing potential from vector field.
		 */
		private VectorField2DPotentialFinder potFinder;
		
		/**
		 * Potential of the vector field.
		 */
		@Parameter
		private double[] potential = null;

		public MTBVectorField2D() {
				this.U = new double[0];
				this.V = new double[0];
				this.width = 0;
				this.height = 0;
				this.potFinder = null;
		}

		public MTBVectorField2D(double[] u, double[] v, int width, int height) {
				this.U = u;
				this.V = v;
				this.width = width;
				this.height = height;
				this.potFinder = new VectorField2DPotentialFinder(this);
		}

		public int getFieldSizeX() {
				return this.width;
		}

		public int getFieldSizeY() {
				return this.height;
		}

		/**
		 * Get vector U of the field.
		 * 
		 * @return Vector U including the x-values of the vector field.
		 */
		public double[] getU() {
				return this.U.clone();
		}

		/**
		 * Get vector V of the field.
		 * 
		 * @return Vector V including the y-values of the vector field.
		 */
		public double[] getV() {
				return this.V.clone();
		}

		/**
		 * Get value from vector U at position (x,y).
		 * 
		 * @param x
		 *          x-coordinate
		 * @param y
		 *          y-coordinate
		 * @return Value from vector U at position (x,y).
		 */
		public double getValueU(double x, double y) {
				int px = (int) Math.round(x);
				int py = (int) Math.round(y);
				return U[py * width + px];
		}

		/**
		 * Get value from vector V at position (x,y).
		 * 
		 * @param x
		 *          x-coordinate
		 * @param y
		 *          y-coordinate
		 * @return Value from vector V at position (x,y).
		 */
		public double getValueV(double x, double y) {
				int px = (int) Math.round(x);
				int py = (int) Math.round(y);
				return V[py * width + px];
		}

		/**
		 * Set vector U of the field.
		 * 
		 * @param u
		 *          vector to set U
		 * @throws ALDOperatorException
		 */
		public void setU(double[] u, int w, int h) throws ALDOperatorException {
				this.U = u;
				this.width = w;
				this.height = h;
				// reset potential field
				this.potential = null;
		}

		/**
		 * Set value on (x,y) for vector U of the field.
		 * <p>
		 * Attention! Method assumes that memory for U is s already allocated.
		 * 
		 * @param x
		 *          x-coordinate
		 * @param y
		 *          y-coordinate
		 * @param value
		 *          value to set on position (x,y)
		 * @throws ALDOperatorException
		 */
		public void setU(double x, double y, double value)
		    throws ALDOperatorException {
				int px = (int) Math.round(x);
				int py = (int) Math.round(y);
				this.U[py * width + px] = value;
				// reset potential field
				this.potential = null;
		}

		/**
		 * Set value on (x,y) for vector V of the field.
		 * 
		 * @param v
		 *          vector to set V
		 * @throws ALDOperatorException
		 */
		public void setV(double[] v, int w, int h) throws ALDOperatorException {
				this.V = v;
				this.width = w;
				this.height = h;
				// reset potential field
				this.potential = null;
		}

		/**
		 * Set value on (x,y) for vector V of the field.
		 * <p>
		 * Attention! Method assumes that memory for V is s already allocated.
		 * 
		 * @param x
		 *          x-coordinate
		 * @param y
		 *          y-coordinate
		 * @param value
		 *          value to set on position (x,y)
		 * @throws ALDOperatorException
		 */
		public void setV(double x, double y, double value)
		    throws ALDOperatorException {
				int px = (int) Math.round(x);
				int py = (int) Math.round(y);
				this.V[py * this.width + px] = value;
				// reset potential field
				this.potential = null;
		}

		/**
		 * Get magnitude of the vector at position (x,y) in the vector field.
		 * 
		 * @param x
		 *          x-coordinate
		 * @param y
		 *          y-coordinate
		 * @return Magnitude of vector (U(x,y), V(x,y)) from the vector field at
		 *         position (x,y).
		 */
		public double getMagnitude(double x, double y) {
				int px = (int) Math.round(x);
				int py = (int) Math.round(y);
				return (Math.sqrt(Math.pow(U[py * width + px], 2)
				    + Math.pow(V[py * width + px], 2)));
		}

		/**
		 * Get direction of the vector at position (x,y) in the vector field.
		 * <p>
		 * The direction is given relative to the x-axis and in radians, 
		 * not in degrees. The range is from 0 to 2 * pi.
		 * 
		 * @param x
		 *          x-coordinate
		 * @param y
		 *          y-coordinate
		 * @return Direction of vector (U(x,y), V(x,y)) from the vector field at
		 *         position (x,y).
		 */
		public double getDirection(double x, double y) {
				int px = (int) Math.round(x);
				int py = (int) Math.round(y);
				double xValue = U[py * width + px];
				double yValue = V[py * width + px];
				return (yValue >= 0.0 ? Math.atan2(yValue, xValue) : (2 * Math.PI + Math
				    .atan2(yValue, xValue)));
		}

		/**
		 * Returns a copy of the potential field (for faster access!).
		 */
		public double[] getPotential() {
				if (this.potential != null)
						return this.potential.clone();
				return null;
		}

		/**
		 * Returns value of potential at specified position.
		 * 
		 * @param x
		 *          x coordinate of position.
		 * @param y
		 *          y coordinate of position.
		 * @return 0 if potential does not exist.
		 */
		public double getValuePotential(double x, double y) {
				if (this.potential != null)
						return this.potential[(int) (y * this.width + x)];
				return 0;
		}

		/**
		 * Resets the potential, next access to it will enforce recalculation.
		 */
		public void resetPotential() {
				this.potential = null;
		}

		// -----------------------------------------
		// ------- Calculate Field Potential -------
		// -----------------------------------------

		/**
		 * Calculates the potential of the field by solving a LSE with least squares.
		 * <p>
		 * Attention! This methods takes a lot of resources. If your field is large
		 * you better use another routine for potential reconstruction.
		 * 
		 * @param ignoreBorder
		 *          If true, outer rows and columns of the field are ignored.
		 * @return Returns a copy of the calculated potential field.
		 */
		public double[] calcPotential_exactLeastSquares(boolean ignoreBorder) {
				// if potential exists, just return
				if (this.potential != null)
						return this.potential.clone();
				// otherwise calculate it
				this.potential = this.potFinder
				    .calcPotential_exactLeastSquares(ignoreBorder);
				return this.potential.clone();
		}

		/**
		 * Calculates an approximation of the potential of the field by gradient
		 * descent.
		 * 
		 * @param ignoreBorder
		 *          If true, outer rows and columns of the field are ignored.
		 * @return Returns a copy of the calculated potential field.
		 */
		public double[] calcPotential_approxLeastSquares(boolean ignoreBorder) {
				// if potential exists, just return
				if (this.potential != null)
						return this.potential.clone();
				// otherwise calculate it
				this.potential = this.potFinder.calcPotential_gradientDescent(ignoreBorder);
				return this.potential.clone();
		}

		/**
		 * Calculates a rough approximation of the potential of the field in linear
		 * time.
		 * 
		 * @param ignoreBorder
		 *          If true, outer rows and columns of the field are ignored.
		 * @return Returns a copy of the calculated potential field.
		 */
		public double[] calcPotential_incrLeastSquares(boolean ignoreBorder) {
				// if potential exists, just return
				if (this.potential != null)
						return this.potential.clone();
				// otherwise calculate it
				this.potential = this.potFinder
				    .calcPotential_incrementalLeastSquares(ignoreBorder);
				return this.potential.clone();
		}

		// ---------------------------------------------------------------
		// ------- Save/Read the Field to/from XML or Binary Files -------
		// ---------------------------------------------------------------

		// /**
		// * Saves the vector field to a file in XML format.
		// * <p>
		// * Attention! If the field is too large memory problems may occure! Better
		// use
		// * saveToBinFile()/readFromBinFile() then.
		// *
		// * @param file
		// * Output file.
		// * @return True, if saving was successful.
		// */
		// public boolean saveToXml(String file) {
		// MTBXmlObject xmlField = new MTBXmlObject();
		// xmlField.addNewTag("VectorField2D");
		// xmlField.addNewTag("Type");
		// xmlField.setAttributeValue("value", this.type.toString());
		// xmlField.parentLevel();
		// xmlField.addNewTag("XDim");
		// xmlField.setAttributeValue("value", new Integer(this.width).toString());
		// xmlField.parentLevel();
		// xmlField.addNewTag("YDim");
		// xmlField.setAttributeValue("value", new Integer(this.height).toString());
		// xmlField.parentLevel();
		// xmlField.addNewTag("Field");
		// for (int y = 0; y < this.height; ++y) {
		// for (int x = 0; x < this.width; ++x) {
		// xmlField.addNewTag("FieldEntry");
		// xmlField.setAttributeValue("x", new Integer(x).toString());
		// xmlField.setAttributeValue("y", new Integer(y).toString());
		// xmlField.setAttributeValue("u", new Double(this.getValueU(x, y))
		// .toString());
		// xmlField.setAttributeValue("v", new Double(this.getValueV(x, y))
		// .toString());
		// xmlField.parentLevel();
		// }
		// }
		// xmlField.parentLevel();
		// if (this.potential != null) {
		// xmlField.addNewTag("Potential");
		// for (int y = 0; y < this.height; ++y) {
		// for (int x = 0; x < this.width; ++x) {
		// xmlField.addNewTag("PotentialEntry");
		// xmlField.setAttributeValue("x", new Integer(x).toString());
		// xmlField.setAttributeValue("y", new Integer(y).toString());
		// xmlField.setAttributeValue("value", new Double(this.potential[y
		// * this.width + x]).toString());
		// xmlField.parentLevel();
		// }
		// }
		// xmlField.parentLevel();
		// }
		// return xmlField.writeToFile(file);
		// }
		//
		// /**
		// * Reads a vector field from a file in XML format.
		// *
		// * @param file
		// * Input file.
		// * @return True, if reading was successful.
		// */
		// public boolean readFromXml(String file) {
		// MTBXmlObject xmlField = new MTBXmlObject();
		// if (!xmlField.readFromFile(file))
		// return false;
		// if (!xmlField.tagExists("VectorField2D")) {
		// System.err
		// .println("VectorField2D::readFromXml - File does not contain field data!");
		// return false;
		// }
		// this.type = MTBImage.stringToType(xmlField.getAttributeValue("Type", 0,
		// "value"));
		// this.width = (new Integer(xmlField.getAttributeValue("XDim", 0, "value")))
		// .intValue();
		// this.height = (new Integer(xmlField.getAttributeValue("YDim", 0, "value")))
		// .intValue();
		// this.U = new double[this.width * this.height];
		// this.V = new double[this.width * this.height];
		// for (int i = 0; i < this.width * this.height; ++i) {
		// int x = new Integer(xmlField.getAttributeValue("FieldEntry", i, "x"))
		// .intValue();
		// int y = new Integer(xmlField.getAttributeValue("FieldEntry", i, "y"))
		// .intValue();
		// double u = new Double(xmlField.getAttributeValue("FieldEntry", i, "u"))
		// .doubleValue();
		// double v = new Double(xmlField.getAttributeValue("FieldEntry", i, "v"))
		// .doubleValue();
		// this.U[y * this.width + x] = u;
		// this.V[y * this.width + x] = v;
		// }
		// if (xmlField.tagExists("Potential")) {
		// this.potential = new double[this.height * this.width];
		// for (int i = 0; i < this.width * this.height; ++i) {
		// int x = new Integer(xmlField
		// .getAttributeValue("PotentialEntry", i, "x")).intValue();
		// int y = new Integer(xmlField
		// .getAttributeValue("PotentialEntry", i, "y")).intValue();
		// double val = new Double(xmlField.getAttributeValue("PotentialEntry", i,
		// "value")).doubleValue();
		// this.potential[y * this.width + x] = val;
		// }
		// }
		// return true;
		// }
		//
		/**
		 * Saves the vector field to a file in binary format.
		 * 
		 * @param file
		 *          Output file.
		 * @return True, if saving was successful.
		 */
		public boolean saveToBinFile(String file) {

				FileOutputStream fos;
				try {
						fos = new FileOutputStream(new File(file));
						DataOutputStream dos = new DataOutputStream(fos);

						// write field meta data...
						// dos.writeInt(this.type.ordinal());
						dos.writeInt(this.width);
						dos.writeInt(this.height);
						// ... and then the field itself
						for (int y = 0; y < this.height; ++y) {
								for (int x = 0; x < this.width; ++x) {
										dos.writeInt(x);
										dos.writeInt(y);
										dos.writeDouble(this.U[y * this.width + x]);
										dos.writeDouble(this.V[y * this.width + x]);
								}
						}
						// finally the potential
						if (this.potential != null) {
								// flag indicating that potential is available
								dos.writeInt(1);
								for (int y = 0; y < this.height; ++y) {
										for (int x = 0; x < this.width; ++x) {
												dos.writeInt(x);
												dos.writeInt(y);
												dos.writeDouble(this.potential[y * this.width + x]);
										}
								}
						} else {
								// flag indicating that no potential is given
								dos.writeInt(0);
						}
						dos.close();
						fos.close();
				} catch (FileNotFoundException e) {
						e.printStackTrace();
				} catch (IOException e) {
						e.printStackTrace();
				}
				return true;
		}

		/**
		 * Reads a vector field from a file in binary format.
		 * 
		 * @param file
		 *          Input file.
		 * @return True, if reading was successful.
		 */
		public boolean readFromBinFile(String file) {
				FileInputStream fis;
				try {
						fis = new FileInputStream(new File(file));
						DataInputStream dis = new DataInputStream(fis);

						// write field meta data...
						// this.type = MTBImage.ordinalToType(dis.readInt());
						this.width = dis.readInt();
						this.height = dis.readInt();
						// return false if file is not binary
						// if it is not a binary file, a OutOfMemory error appears by initializing
						// U and V
						try {
								this.U = new double[this.width * this.height];
								this.V = new double[this.width * this.height];
						} catch (OutOfMemoryError e) {
								e.printStackTrace();
								return false;
						}

						// ... and then the field itself
						for (int y = 0; y < this.height; ++y) {
								for (int x = 0; x < this.width; ++x) {
										// x value
										dis.readInt();
										// y value
										dis.readInt();
										this.U[y * this.width + x] = dis.readDouble();
										this.V[y * this.width + x] = dis.readDouble();
								}
						}
						// finally the potential
						if (dis.readInt() == 1) {
								this.potential = new double[this.width * this.height];
								for (int y = 0; y < this.height; ++y) {
										for (int x = 0; x < this.width; ++x) {
												dis.readInt();
												dis.readInt();
												this.potential[y * this.width + x] = dis.readDouble();
										}
								}
						} else {
								this.potential = null;
						}
						dis.close();
						fis.close();
				} catch (FileNotFoundException e) {
						e.printStackTrace();
				} catch (IOException e) {
						e.printStackTrace();
				}
				return true;
		}
}
