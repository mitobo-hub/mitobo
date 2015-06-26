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
import java.util.Random;

import org.apache.xmlbeans.XmlException;
import org.junit.After;
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
import de.unihalle.informatik.MiToBo.topology.MTBTopologicalNumber2DN8;
import de.unihalle.informatik.MiToBo.topology.MTBTopologicalNumber.Point3D;

/**
 * JUnit test class for {@link MTBWrapperDataIOXmlbeans}.
 * 
 * @author posch
 */
public class TestMTBDataIOXmlbeans {

	private boolean debug = true;
	
	private final int xSize = 500;
	private final int ySize = 500;
	private final int maxNumRegionSets = 20;
	private final int maxNumRegions = 20;
	private final int maxArea = 200;

	private final int maxNumPolygons = 50;
	private final int maxNumPoints = 100;
	private MTBImage image;
			
	/**
	 * Fixture.
	 */
	@Before
	public void initTestClass() {
	}
	
	/**
	 * Closure.
	 */
	@After
	public void cleanUp() {
	    if (new File("set.tif.ald").exists())
		new File("set.tif.ald").delete();
	    if (new File("set.tif").exists())
		new File("set.tif").delete();
	    if (new File("setbag.tif.ald").exists())
		new File("setbag.tif.ald").delete();
	    if (new File("setbag.tif").exists())
		new File("setbag.tif").delete();
	    if (new File("setback.tif.ald").exists())
		new File("setback.tif.ald").delete();
	    if (new File("setback.tif").exists())
		new File("setback.tif").delete();
	    if (new File("setbagback.tif.ald").exists())
		new File("setbagback.tif.ald").delete();
	    if (new File("setbagback.tif").exists())
		new File("setbagback.tif").delete();
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
	public void testRegionsAndPolygonIO() throws IOException, ALDDataIOProviderException, ALDDataIOManagerException, XmlException, ALDOperatorException, ALDProcessingDAGException, InterruptedException {
		File tmpFile = File.createTempFile("tmp", "xml");
		Random rndGen = new Random(); 
		
		// create images to check the structures created visually?
		boolean createImages = true;
		
		ImageWriterMTB writer;
		MTBRegion2DSetBag region2DSetBag = null;
		
		// =====================================================================
		// test regions2DSetBag
		if ( createImages)
			image = MTBImage.createMTBImage(xSize, ySize, 1, 1, 1, MTBImageType.MTB_BYTE);

		region2DSetBag = new MTBRegion2DSetBag();

		// create region sets
		int numRegionSets = rndGen.nextInt(maxNumRegionSets) + 1;
		if ( debug )
			System.out.println(numRegionSets + " regions sets");
		
		for ( int i = 0 ; i < numRegionSets ; i++ ) {
			MTBRegion2DSet region2DSet = new MTBRegion2DSet();
			int numRegions = rndGen.nextInt(maxNumRegions) + 1;
			if ( debug )
				System.out.println( "   " + numRegions + " regions");

			for ( int r = 0 ; r < numRegions ; r++) {
				MTBRegion2D region2D = MTBRegion2D.createRandomRegion2D(xSize, ySize, maxArea, rndGen);
				region2DSet.add( region2D);
				image = region2D.toMTBImage(null, image);
			}
			if ( createImages )
				region2DSetBag.add(region2DSet);
		}

		if ( createImages) {
			writer = new ImageWriterMTB(image, "setbag.tif");
			writer.runOp();
		}

		ALDDataIOManagerXmlbeans.writeXml(tmpFile, region2DSetBag);
		MTBRegion2DSetBag region2DSetBagBack = 
				(MTBRegion2DSetBag)ALDDataIOManagerXmlbeans.readXml(tmpFile,MTBRegion2DSetBag.class);

		if ( createImages ) {
			image = MTBImage.createMTBImage(xSize, ySize, 1, 1, 1, MTBImageType.MTB_BYTE);

			for ( int i = 0 ; i < region2DSetBagBack.size() ; i++) {
				MTBRegion2DSet regionSet = region2DSetBagBack.get(i);
				for ( int r = 0 ; r < regionSet.size() ; r++ ) {
					image = regionSet.get(r).toMTBImage(null, image);
				}
			}
			writer = new ImageWriterMTB(image, "setbagback.tif");
			writer.runOp();

		}
		
		assertTrue("Got different Region 2D set bags back: ", 
				this.isEqual(region2DSetBag, region2DSetBagBack));
		
		// =========================================
		// MTBRegion2DSet
		if ( createImages)
			image = MTBImage.createMTBImage(xSize, ySize, 1, 1, 1, MTBImageType.MTB_BYTE);

		MTBRegion2DSet region2DSet = new MTBRegion2DSet();
		int numRegions = rndGen.nextInt(maxNumRegions)  + 1;
		if ( debug )
			System.out.println( "   " + numRegions + " regions");

		for ( int r = 0 ; r < numRegions ; r++) {
			MTBRegion2D region2D = MTBRegion2D.createRandomRegion2D(xSize, ySize, maxArea, rndGen);
			region2DSet.add( region2D);
			image = region2D.toMTBImage(null, image);
		}
		if ( createImages )
			region2DSetBag = new MTBRegion2DSetBag();

			region2DSetBag.add(region2DSet);

		if ( createImages) {
			writer = new ImageWriterMTB(image, "set.tif");
			writer.runOp();
		}

		ALDDataIOManagerXmlbeans.writeXml(tmpFile, region2DSet);
		MTBRegion2DSet region2DSetBack = 
				(MTBRegion2DSet)ALDDataIOManagerXmlbeans.readXml(tmpFile,MTBRegion2DSet.class);

		if ( createImages ) {
			image = MTBImage.createMTBImage(xSize, ySize, 1, 1, 1, MTBImageType.MTB_BYTE);

			for ( int r = 0 ; r < region2DSetBack.size() ; r++ ) {
				image = region2DSetBack.get(r).toMTBImage(null, image);
			}
			writer = new ImageWriterMTB(image, "setback.tif");
			writer.runOp();

		}

		assertTrue("Got different Region 2D set back: ", 
				this.isEqual(region2DSet, region2DSetBack));
		
		// =========================================
		// MTBRegion3DSet
		// TODO
		
		// =========================================
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
		
		ALDDataIOManagerXmlbeans.writeXml(tmpFile, polgygon2DSet);
		MTBPolygon2DSet polgygon2DSetBack = 
				(MTBPolygon2DSet)ALDDataIOManagerXmlbeans.readXml(tmpFile,MTBPolygon2DSet.class);

		assertTrue("Got different Polygon 2D set back: ", 
				this.isEqual(polgygon2DSet, polgygon2DSetBack));
		

		// =========================================
		// MTBContour2DSet

	}

	// =======================================================================================
	// private helper methods
	
	/** 
	 * Are to bags of sets of 2D region equal?
	 * 
	 * @param region2DSetBag
	 * @param region2DSetBagBack
	 * @return
	 */
	private boolean isEqual(MTBRegion2DSetBag region2DSetBag, MTBRegion2DSetBag region2DSetBagBack) {
		if ( region2DSetBag.size() != region2DSetBagBack.size()) {
			System.out.println("region 2D set bag: different size " + region2DSetBag.size() + " vs " +
					region2DSetBagBack.size());
			return false;
		}
		
		for ( int i = 0 ; i < region2DSetBag.size() ; i++) {
			if ( ! isEqual( region2DSetBag.get(i), region2DSetBagBack.get(i)) ) {
				System.out.println("region 2D set bag: " + i + "-th region set unequal");
				return false;				
			}
		}
		return true;
	}

	/** Are two sets of 2D regions equal
	 * 
	 * @param region2DSet
	 * @param region2DSetBack
	 * @return
	 */
	private boolean isEqual(MTBRegion2DSet region2DSet, MTBRegion2DSet region2DSetBack) {
		if ( region2DSet.size() != region2DSetBack.size() ) {
				System.out.println("region 2D set: different size " + region2DSet.size() + " vs " +
					region2DSetBack.size());
				return false;							
		}
		
		for ( int i = 0 ; i < region2DSet.size() ; i++) {
			if ( ! region2DSet.get(i).equals( region2DSetBack.get(i)) ) {
				System.out.println("region 2D set: " + i + "-th region unequal");
				return false;				
			}
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