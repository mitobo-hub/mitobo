/*
 * This file is part of Alida, a Java library for 
 * Advanced Library for Integrated Development of Data Analysis Applications.
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
 * Fore more information on Alida, visit
 *
 *    http://www.informatik.uni-halle.de/alida/
 *
 */

package de.unihalle.informatik.MiToBo.core.dataio.provider.xmlbeans;

import static org.junit.Assert.*;

import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;

import org.apache.xmlbeans.XmlException;
import org.junit.Before;
import org.junit.Test;

import de.unihalle.informatik.Alida.dataio.ALDDataIOManagerXmlbeans;
import de.unihalle.informatik.Alida.exceptions.ALDDataIOManagerException;
import de.unihalle.informatik.Alida.exceptions.ALDDataIOProviderException;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBPolygon2D;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBPolygon2DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2D;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2DSetBag;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageByte;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.io.images.ImageWriterMTB;
import de.unihalle.informatik.MiToBo.segmentation.snakes.datatypes.MTBSet_SnakeEnergyDerivable;
import de.unihalle.informatik.MiToBo.segmentation.snakes.energies.MTBSnakeEnergyCD_KassCurvature;
import de.unihalle.informatik.MiToBo.segmentation.snakes.energies.MTBSnakeEnergyCD_KassLength;
import de.unihalle.informatik.MiToBo.segmentation.snakes.energies.paramAdapt.MTBSnakeEnergyCD_KassCurvature_ParamAdaptNone;
import de.unihalle.informatik.MiToBo.segmentation.snakes.energies.paramAdapt.MTBSnakeEnergyCD_KassLength_ParamAdaptNone;
import de.unihalle.informatik.MiToBo.segmentation.snakes.optimize.SnakeOptimizerCoupled;
import de.unihalle.informatik.MiToBo.segmentation.snakes.optimize.SnakeOptimizerSingle;
import de.unihalle.informatik.MiToBo.segmentation.snakes.optimize.SnakeOptimizerSingleVarCalc;
import de.unihalle.informatik.MiToBo.segmentation.snakes.optimize.stepsize.MTBGammaPtWiseExtEner;
import de.unihalle.informatik.MiToBo.segmentation.snakes.optimize.termination.MTBTermMotionDiff;
import de.unihalle.informatik.MiToBo.topology.MTBTopologicalNumber2DN8;
import de.unihalle.informatik.MiToBo.topology.MTBTopologicalNumber.Point3D;

/**
 * JUnit test class for {@link MTBWrapperDataIOXmlbeans}.
 * 
 * @author posch
 */
public class TestOperatorDataIOXmlbeans {

	private boolean debug = true;
	
	private final int xSize = 500;
	private final int ySize = 500;
	private final int maxNumPolygons = 50;
	private final int maxNumPoints = 100;
	/**
	 * Fixture.
	 */
	@Before
	public void initTestClass() {
	}
	
	/**
	 * Test xmlbeans provider for regions and polygons (various MTB data types)
	 * as define in {@link MTBDataIOFileXmlbeans}.
	 * @throws IOException 
	 * @throws ALDDataIOManagerException 
	 * @throws ALDDataIOProviderException 
	 * @throws XmlException 
	 * @throws ALDProcessingDAGException 
	 * @throws ALDOperatorException 
	 * @throws InterruptedException 
	 */
	@Test
	public void testSnakeOptimizerCoupled() throws IOException, ALDDataIOProviderException, ALDDataIOManagerException, XmlException, ALDOperatorException, ALDProcessingDAGException, InterruptedException {
		File tmpFile = File.createTempFile("tmp", "xml");
		Random rndGen = new Random(); 
		
		// -------------------
		// MTBPolygon2DSet
		double xmin = 0.0;
		double xmax = xSize + (rndGen.nextDouble()-0.5)*50;
		double ymin = 0.0;
		double ymax = ySize + (rndGen.nextDouble()-0.5)*50;
		
		MTBPolygon2DSet polgygon2DSet = new MTBPolygon2DSet(xmin, ymin, xmax, ymax);

		int numPolygons = rndGen.nextInt(maxNumPolygons)+1;
		if ( debug )
			System.out.println(numPolygons + " polygons");
		for ( int p = 0 ; p < numPolygons ; p++) {
			MTBPolygon2D polygon = new MTBPolygon2D();
			int numPoints = rndGen.nextInt(maxNumPoints) + 1 ;
			if ( debug ) 
				System.out.println( numPoints + " points in polygon");
			
			for ( int i = 0 ; i < numPoints ; i++) {
				double x = rndGen.nextDouble() * xmax;
				double y = rndGen.nextDouble() * ymax;
				polygon.addPoint(x, y);
			}
			polgygon2DSet.add(polygon);
		}
		
		// -------------------
		// single optimizer
		MTBSet_SnakeEnergyDerivable energies = new MTBSet_SnakeEnergyDerivable();
		double alpha = 1.52;
		MTBSnakeEnergyCD_KassLength lengthEnergy = new MTBSnakeEnergyCD_KassLength(
				alpha , new MTBSnakeEnergyCD_KassLength_ParamAdaptNone());
		double beta = Math.PI;
		MTBSnakeEnergyCD_KassCurvature curvEnergy = new MTBSnakeEnergyCD_KassCurvature(
				beta, new MTBSnakeEnergyCD_KassCurvature_ParamAdaptNone());

		/*
		 * Add internal energy to snake energy.
		 */
		energies.addEnergy(lengthEnergy);
		energies.addEnergy(curvEnergy);

		double motionFraction = 0.5;
		int maxIterations = 1000;
		double resampleConstant = 0.9;
		Double stepSize = 2.0;
		SnakeOptimizerSingleVarCalc SO = new SnakeOptimizerSingleVarCalc(
			    null, null, energies, new MTBGammaPtWiseExtEner(),
			    stepSize, new MTBTermMotionDiff(motionFraction, maxIterations),
			    new Boolean(true), new Double((double) resampleConstant));
		
		// -------------------
		// coupled optimizer 
		MTBImage dummyImage = 
				MTBImage.createMTBImage(10, 10, 10, 1, 1, MTBImageType.MTB_BYTE);

		SnakeOptimizerCoupled snakeOptimizer = new SnakeOptimizerCoupled();
		snakeOptimizer.setInitialSnakes(polgygon2DSet);
		snakeOptimizer.setInputImage(dummyImage);
		snakeOptimizer.setParameter("snakeOptimizer", SO);

		
		ALDDataIOManagerXmlbeans.writeXml(tmpFile, snakeOptimizer);
		SnakeOptimizerCoupled snakeOptimizerBack = 
				(SnakeOptimizerCoupled)ALDDataIOManagerXmlbeans.readXml(tmpFile,SnakeOptimizerCoupled.class);

		assertTrue("Got different Snake optimizer operators back: ", 
				this.isEqual(snakeOptimizer, snakeOptimizerBack));
		

	}

	// =======================================================================================
	// private helper methods
	
	private boolean isEqual(SnakeOptimizerCoupled snakeOptimizer,
			SnakeOptimizerCoupled snakeOptimizerBack) throws ALDOperatorException {
		if ( ! isEqual(snakeOptimizer.getInitialSnakes(), snakeOptimizerBack.getInitialSnakes()) ) {
			System.out.println( "Inital snakes are different");
			return false;
		}
		
		if ( snakeOptimizerBack.getWorkingImage() != null ) {
			System.out.println(" Inimage of snake optimzer coupled read is non null");
			return false;
		}
		
		SnakeOptimizerSingle SO = (SnakeOptimizerSingleVarCalc)snakeOptimizer.getParameter("snakeOptimizer");
		SnakeOptimizerSingle SOBack = (SnakeOptimizerSingleVarCalc)snakeOptimizerBack.getParameter("snakeOptimizer");
		
		LinkedList<String> pNames = new  LinkedList<String>();
		pNames.add( "initialGammas");		
		pNames.add( "resampleSegLength");
		pNames.add( "doResampling");

		for ( String pName : pNames) {
			if ( SO.getParameter(pName) != null && SOBack.getParameter(pName) != null &&
					! SO.getParameter(pName).equals(SOBack.getParameter(pName)) ) {
				System.out.println( "Different parameters in snake opimizer single: " +
						pName + ": " + SO.getParameter(pName) + " vs " + SOBack.getParameter(pName));
				return false;
			}
		}
		
		// test the specific energies we put into SO
		MTBSet_SnakeEnergyDerivable energies = (MTBSet_SnakeEnergyDerivable) SO.getParameter("energySet");
		MTBSet_SnakeEnergyDerivable energiesBack = (MTBSet_SnakeEnergyDerivable) SOBack.getParameter("energySet");

		if ( energies.getEnergyList().size() != energiesBack.getEnergyList().size() ) {
			System.out.println( "Energies have different length " +
					energies.getEnergyList().size() + " vs " + energiesBack.getEnergyList().size());
			return false;
		}
		
		// rely on the order we used 
		MTBSnakeEnergyCD_KassLength e0;
		try {
			e0 =  (MTBSnakeEnergyCD_KassLength) energies.getEnergy(0);
		} catch (Exception e) {
			System.out.println( "wrong energy[0] in SO " + energies.getEnergy(0).getClass().getName());
			return false;
		}
		
		MTBSnakeEnergyCD_KassLength e0Back;
		try {
			e0Back =  (MTBSnakeEnergyCD_KassLength) energiesBack.getEnergy(0);
		} catch (Exception e) {
			System.out.println( "wrong energy[0] in SOBack " + energiesBack.getEnergy(0).getClass().getName());
			return false;
		}
		
		if ( e0.getInitAlpha() != e0Back.getInitAlpha()) {
			System.out.println( "Different alpha " + 
					e0.getInitAlpha() + " vs " + e0Back.getInitAlpha());
			return false;
		}

		MTBSnakeEnergyCD_KassCurvature e1;
		try {
			e1 =  (MTBSnakeEnergyCD_KassCurvature) energies.getEnergy(1);
		} catch (Exception e) {
			System.out.println( "wrong energy[1] in SO " + energies.getEnergy(1).getClass().getName());
			return false;
		}
		
		MTBSnakeEnergyCD_KassCurvature e1Back;
		try {
			e1Back =  (MTBSnakeEnergyCD_KassCurvature) energiesBack.getEnergy(1);
		} catch (Exception e) {
			System.out.println( "wrong energy[1] in SOBack " + energiesBack.getEnergy(1).getClass().getName());
			return false;
		}
		
		if ( e1.getInitBeta() != e1Back.getInitBeta()) {
			System.out.println( "Different alpha " + 
					e1.getInitBeta() + " vs " + e1Back.getInitBeta());
			return false;
		}

		return true;
	}


	/**
	 * Are two sets of 2D Polygons equal ?
	 * 
	 * @param polgygon2dSet
	 * @param polgygon2dSetBack
	 * @return
	 */
	private boolean isEqual(MTBPolygon2DSet polgygon2dSet,
			MTBPolygon2DSet polgygon2dSetBack) {
		if ( polgygon2dSet.size() != polgygon2dSetBack.size() ) {
			System.out.println("polygon 2D set:  different size " + polgygon2dSet.size() + " vs " +
					polgygon2dSetBack.size());
			return false;
		}
		
		for ( int i = 0 ; i < polgygon2dSet.size() ; i++) {
			if ( ! polgygon2dSet.elementAt(i).equals( polgygon2dSetBack.elementAt(i))) {
				System.out.println("polygon 2D set: " + i + "-th polygon unequal");				
				return false;
			}
		}
		
		return true;
	}

}
