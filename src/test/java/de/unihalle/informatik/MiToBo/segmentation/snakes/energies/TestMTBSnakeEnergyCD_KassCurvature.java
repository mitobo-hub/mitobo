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

import static org.junit.Assert.*;

import java.util.Vector;

import org.junit.Before;
import org.junit.Test;

import Jama.Matrix;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBPolygon2DSet;
import de.unihalle.informatik.MiToBo.segmentation.activecontours.exceptions.MTBSnakeException;
import de.unihalle.informatik.MiToBo.segmentation.snakes.datatypes.MTBSet_SnakeEnergyDerivable;
import de.unihalle.informatik.MiToBo.segmentation.snakes.datatypes.MTBSnake;
import de.unihalle.informatik.MiToBo.segmentation.snakes.datatypes.MTBSnakePoint2D;
import de.unihalle.informatik.MiToBo.segmentation.snakes.energies.paramAdapt.MTBSnakeEnergyCD_KassCurvature_ParamAdaptNone;
import de.unihalle.informatik.MiToBo.segmentation.snakes.optimize.SnakeOptimizerSingle.EnergyNormalizationMode;

/**
 * JUnit test class for {@link MTBSnakeEnergyCD_KassCurvature}.
 * 
 * @author moeller
 */
public class TestMTBSnakeEnergyCD_KassCurvature {

	private static final String IDS = "[TestMTBSnakeEnergyCD_KassCurvature]";
	
	private static final double accuracy = 10e-10;
	
	private MTBSnake testSnake;
	
	/**
	 * Fixture.
	 */
	@Before
	public void initTestClass() {
		Vector<MTBSnakePoint2D> spoints = new Vector<MTBSnakePoint2D>();
		spoints.add(new MTBSnakePoint2D(2.0, 1.0));
		spoints.add(new MTBSnakePoint2D(3.0, 1.0));
		spoints.add(new MTBSnakePoint2D(4.0, 2.0));
		spoints.add(new MTBSnakePoint2D(4.0, 3.0));
		spoints.add(new MTBSnakePoint2D(3.0, 4.0));
		spoints.add(new MTBSnakePoint2D(2.0, 4.0));
		spoints.add(new MTBSnakePoint2D(1.0, 3.0));
		spoints.add(new MTBSnakePoint2D(1.0, 2.0));
		this.testSnake = new MTBSnake(spoints, true);
	}
	
	/**
	 * Test methods.
	 */
	@Test
	public void testMTBSnakeEnergyCD_KassCurvature() {
		MTBSnakeEnergyCD_KassCurvature energy = 
			new MTBSnakeEnergyCD_KassCurvature(2.0, 
				new MTBSnakeEnergyCD_KassCurvature_ParamAdaptNone());
		assertTrue(IDS + " initial beta should be 2.0, it's " 
			+ energy.getInitBeta(), Math.abs(energy.getInitBeta() - 2.0) < accuracy);
		
		// init energy
		boolean exceptionThrown = false;
    try {
  		MockSnakeOptimizerSingleVarCalc opt = 
  			new MockSnakeOptimizerSingleVarCalc();
    	MTBPolygon2DSet inSnakes = new MTBPolygon2DSet();
    	inSnakes.add(this.testSnake);
    	opt.setInitialSnakes(inSnakes);
    	opt.setNormalizationMode(EnergyNormalizationMode.NORM_NONE);
    	MTBSet_SnakeEnergyDerivable eSet = new MTBSet_SnakeEnergyDerivable();
    	eSet.addEnergy(energy);
    	opt.setEnergySet(eSet);
    	try {
	      opt.initOptimizer();
      } catch (MTBSnakeException e) {
      	exceptionThrown = true;
      }
    	double snakeEnergy = energy.calcEnergy(opt);
    	assertTrue(IDS + " curvature energy should be 8.0, is " + snakeEnergy,
    		Math.abs(snakeEnergy - 8.0) < accuracy);
    	Matrix A = energy.getDerivative_MatrixPart(opt);
    	assertTrue(IDS + " expecting A having 16 rows...", 
    		A.getRowDimension() == 16);
    	assertTrue(IDS + " expecting A having 16 columns...", 
      	A.getColumnDimension() == 16);
    	
    	/*
    	 * upper left block
    	 */
    	
    	// first row
   		assertTrue(IDS + " entry ( 0, 0 ) should be 12.0...", 
   			Math.abs(A.get(0, 0) - 12.0) < accuracy); 
   		assertTrue(IDS + " entry ( 0, 1 ) should be -8.0...", 
   			Math.abs(A.get(0, 1) - -8.0) < accuracy);
   		assertTrue(IDS + " entry ( 0, 2 ) should be 2.0...", 
   			Math.abs(A.get(0, 2) - 2.0) < accuracy);
  		assertTrue(IDS + " entry ( 0, 6 ) should be 2.0, it's "	+ A.get(0, 6) 
   			+ "...", Math.abs(A.get(0, 6) - 2.0) < accuracy); 
  		assertTrue(IDS + " entry ( 0, 7 ) should be -8.0, it's "	+ A.get(0, 7) 
  			+ "...", Math.abs(A.get(0, 7) - -8.0) < accuracy); 
    	// second row
   		assertTrue(IDS + " entry ( 1, 0 ) should be -8.0, it's "	+ A.get(1, 0)
   			+ "...", Math.abs(A.get(1, 0) - -8.0) < accuracy); 
   		assertTrue(IDS + " entry ( 1, 1 ) should be 12.0...", 
   			Math.abs(A.get(1, 1) - 12.0) < accuracy);
   		assertTrue(IDS + " entry ( 1, 2 ) should be -8.0...", 
   			Math.abs(A.get(1, 2) - -8.0) < accuracy);
  		assertTrue(IDS + " entry ( 1, 3 ) should be 2.0, it's "	+ A.get(1, 3) 
   			+ "...", Math.abs(A.get(1, 3) - 2.0) < accuracy); 
  		assertTrue(IDS + " entry ( 1, 7 ) should be 2.0, it's "	+ A.get(1, 7) 
  			+ "...", Math.abs(A.get(1, 7) - 2.0) < accuracy); 
   		// rows 2 to 5
    	for (int row = 2; row < 6; ++row) {
    		assertTrue(IDS + " entry (" +row+ "," +(row-2)+ ") should be 2.0...", 
     			Math.abs(A.get(row, row-2) - 2.0) < accuracy); 
    		assertTrue(IDS + " entry (" +row+ "," +(row-1)+ ") should be -8.0...", 
    			Math.abs(A.get(row, row-1) - -8.0) < accuracy); 
    		assertTrue(IDS + " entry (" +row+ "," +(row)+ ") should be 12.0...", 
     			Math.abs(A.get(row, row) - 12.0) < accuracy); 
    		assertTrue(IDS + " entry (" +row+ "," +(row+1)+ ") should be -8.0...", 
     			Math.abs(A.get(row, row+1) - -8.0) < accuracy); 
    		assertTrue(IDS + " entry (" +row+ "," +(row+2)+ ") should be 2.0...", 
     			Math.abs(A.get(row, row+2) - 2.0) < accuracy); 
    	}
    	// pre-last row of upper block
  		assertTrue(IDS + " entry ( 6, 0 ) should be 2.0...", 
   			Math.abs(A.get(6, 0) - 2.0) < accuracy); 
  		assertTrue(IDS + " entry ( 6, 4 ) should be 2.0...", 
   			Math.abs(A.get(6, 4) - 2.0) < accuracy); 
  		assertTrue(IDS + " entry ( 6, 5 ) should be -8.0...", 
   			Math.abs(A.get(6, 5) - -8.0) < accuracy); 
   		assertTrue(IDS + " entry ( 6, 6 ) should be 12.0...", 
   			Math.abs(A.get(6, 6) - 12.0) < accuracy);
   		assertTrue(IDS + " entry ( 6, 7 ) should be -8.0...", 
   			Math.abs(A.get(6, 7) - -8.0) < accuracy); 
    	// last row of upper block
  		assertTrue(IDS + " entry ( 7, 0 ) should be -8.0...", 
   			Math.abs(A.get(7, 0) - -8.0) < accuracy); 
  		assertTrue(IDS + " entry ( 7, 1 ) should be 2.0...", 
   			Math.abs(A.get(7, 1) - 2.0) < accuracy); 
  		assertTrue(IDS + " entry ( 7, 5 ) should be 2.0...", 
   			Math.abs(A.get(7, 5) - 2.0) < accuracy); 
   		assertTrue(IDS + " entry ( 7, 6 ) should be -8.0...", 
   			Math.abs(A.get(7, 6) - -8.0) < accuracy);
   		assertTrue(IDS + " entry ( 7, 7 ) should be 12.0...", 
   			Math.abs(A.get(7, 7) - 12.0) < accuracy); 
   		
    	/*
    	 * lower right block
    	 */
    	
    	// first row (row 8)
   		assertTrue(IDS + " entry ( 8, 8 ) should be 12.0...", 
 				Math.abs(A.get(8, 8) - 12.0) < accuracy); 
   		assertTrue(IDS + " entry ( 8, 9 ) should be -8.0...", 
 				Math.abs(A.get(8, 9) - -8.0) < accuracy);
   		assertTrue(IDS + " entry ( 8, 10 ) should be 2.0...", 
 				Math.abs(A.get(8, 10) - 2.0) < accuracy);
   		assertTrue(IDS + " entry ( 8, 14 ) should be 2.0, it's "	+ A.get(8, 14) 
 				+ "...", Math.abs(A.get(8, 14) - 2.0) < accuracy); 
   		assertTrue(IDS + " entry ( 8, 15 ) should be -8.0, it's "	+ A.get(8, 15) 
 				+ "...", Math.abs(A.get(8, 15) - -8.0) < accuracy); 
    	// second row
   		assertTrue(IDS + " entry ( 9, 8 ) should be -8.0, it's "	+ A.get(9, 8)
   			+ "...", Math.abs(A.get(9, 8) - -8.0) < accuracy); 
   		assertTrue(IDS + " entry ( 9, 9 ) should be 12.0...", 
   			Math.abs(A.get(9, 9) - 12.0) < accuracy);
   		assertTrue(IDS + " entry ( 9, 10 ) should be -8.0...", 
   			Math.abs(A.get(9, 10) - -8.0) < accuracy);
  		assertTrue(IDS + " entry ( 9, 11 ) should be 2.0, it's "	+ A.get(9, 11) 
   			+ "...", Math.abs(A.get(9, 11) - 2.0) < accuracy); 
  		assertTrue(IDS + " entry ( 9, 15 ) should be 2.0, it's "	+ A.get(9, 15) 
  			+ "...", Math.abs(A.get(9, 15) - 2.0) < accuracy); 
   		// rows 10 to 13
   		for (int row = 10; row < 14; ++row) {
   			assertTrue(IDS + " entry (" +row+ "," +(row-2)+ ") should be 2.0...", 
   					Math.abs(A.get(row, row-2) - 2.0) < accuracy); 
   			assertTrue(IDS + " entry (" +row+ "," +(row-1)+ ") should be -8.0...", 
   					Math.abs(A.get(row, row-1) - -8.0) < accuracy); 
   			assertTrue(IDS + " entry (" +row+ "," +(row)+ ") should be 12.0...", 
   					Math.abs(A.get(row, row) - 12.0) < accuracy); 
   			assertTrue(IDS + " entry (" +row+ "," +(row+1)+ ") should be -8.0...", 
   					Math.abs(A.get(row, row+1) - -8.0) < accuracy); 
   			assertTrue(IDS + " entry (" +row+ "," +(row+2)+ ") should be 2.0...", 
   					Math.abs(A.get(row, row+2) - 2.0) < accuracy); 
   		}
    	// pre-last row of lower block
  		assertTrue(IDS + " entry ( 14, 8 ) should be 2.0...", 
   			Math.abs(A.get(14, 8) - 2.0) < accuracy); 
  		assertTrue(IDS + " entry ( 14, 12 ) should be 2.0...", 
   			Math.abs(A.get(14, 12) - 2.0) < accuracy); 
  		assertTrue(IDS + " entry ( 14, 13 ) should be -8.0...", 
   			Math.abs(A.get(14, 13) - -8.0) < accuracy); 
   		assertTrue(IDS + " entry ( 14, 14 ) should be 12.0...", 
   			Math.abs(A.get(14, 14) - 12.0) < accuracy);
   		assertTrue(IDS + " entry ( 14, 15 ) should be -8.0...", 
   			Math.abs(A.get(14, 15) - -8.0) < accuracy); 
   		// last row of lower block
   		assertTrue(IDS + " entry ( 15, 8) should be -8.0...", 
   				Math.abs(A.get(15,8) - -8.0) < accuracy); 
   		assertTrue(IDS + " entry ( 15, 9 ) should be 2.0...", 
   				Math.abs(A.get(15,9) - 2.0) < accuracy); 
   		assertTrue(IDS + " entry ( 15, 13 ) should be 2.0...", 
   				Math.abs(A.get(15,13) - 2.0) < accuracy); 
   		assertTrue(IDS + " entry ( 15, 14 ) should be -8.0...", 
   				Math.abs(A.get(15,14) - -8.0) < accuracy);
   		assertTrue(IDS + " entry ( 15, 15 ) should be 12.0...", 
   				Math.abs(A.get(15,15) - 12.0) < accuracy); 

    } catch (ALDOperatorException e) {
    	exceptionThrown = true;
    }
    assertFalse(IDS + " got an exception during testing the energy!!!",
    	exceptionThrown);
	}
}