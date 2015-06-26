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

package de.unihalle.informatik.MiToBo.core.datatypes.images;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;

/**
 * JUnit test class for {@link MTBImageTileAdapter}.
 * 
 * @author moeller
 */
public class TestMTBImageTileAdapter {

	/**
	 * Folder for temporary output data.
	 */
	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	/**
	 * Dummy image for tests.
	 */
	protected MTBImage dummyImage;
	
	/**
	 * Tile adapter to test.
	 */
	protected MTBImageTileAdapter testObj;
	
	/**
	 * Tile adapter to test in case of overlap.
	 */
	protected MTBImageTileAdapter testObjOverlap;

	/**
	 * Fixture.
	 */
	@Before
	public void initTestClass() {
		// create a test image...
		this.dummyImage = 
				MTBImage.createMTBImage(1000, 1000, 1, 1, 1, MTBImageType.MTB_BYTE);
		// ... and a corresponding tile adapters
		this.testObj = 
			new MTBImageTileAdapter(this.dummyImage, 64, 64);
		this.testObjOverlap = 
			new MTBImageTileAdapter(this.dummyImage, 64, 64, 32, 32);
	}
	
	/**
	 * Test if correct number of tiles and correct sizes are returned.
	 */
	@Test
	public void testTileCount() {
		// get all the tiles
		MTBImage[][] allTiles = this.testObj.getAllTiles();
		assertTrue("Tile array should have 16 rows!", allTiles.length == 16);
		assertTrue("Tile array should have 16 cols!", allTiles[0].length == 16);
		
		// check total number of tiles
		assertTrue("Expected 16 tiles in x!",	this.testObj.getTileRows() == 16);
		assertTrue("Expected 16 tiles in y!",	this.testObj.getTileCols() == 16);
		
		// check if all tiles have correct size
		for (int y=0;y<this.testObj.getTileRows();++y) {
			for (int x=0;x<this.testObj.getTileCols();++x) {
				MTBImage tile = allTiles[y][x];
				if (x!=15)
					assertTrue("Tile should have a col size of 64!",tile.getSizeX()==64);
				else
					assertTrue("Tile should have a col size of 40!",tile.getSizeX()==40);
				if (y!=15)
					assertTrue("Tile should have a row size of 64!",tile.getSizeY()==64);
				else
					assertTrue("Tile should have a row size of 40!",tile.getSizeY()==40);
			}
		}
	}
	
	/**
	 * Test if correct number of tiles and correct sizes are returned.
	 */
	@Test
	public void testTileCountOverlap() {
		// get all the tiles
		MTBImage[][] allTiles = this.testObjOverlap.getAllTiles();
		assertTrue("Tile array should have 32 rows, got " + allTiles.length+"!", 
			allTiles.length == 32);
		assertTrue("Tile array should have 32 cols, got " + allTiles[0].length+"!", 
			allTiles[0].length == 32);
		
		// check total number of tiles
		assertTrue("Expected 32 tiles in x!",	
			this.testObjOverlap.getTileRows() == 32);
		assertTrue("Expected 32 tiles in y!",	
			this.testObjOverlap.getTileCols() == 32);
		
		// check if all tiles have correct size
		for (int y=0;y<this.testObjOverlap.getTileRows();++y) {
			for (int x=0;x<this.testObjOverlap.getTileCols();++x) {
				MTBImage tile = allTiles[y][x];
				if (x < 30)
					assertTrue("Tile should have a col size of 64!",tile.getSizeX()==64);
				else if (x == 30)
					assertTrue("Tile should have a col size of 40!",tile.getSizeX()==40);
				else 
					assertTrue("Tile should have a col size of 8, got " 
						+ tile.getSizeX() + "!",tile.getSizeX()==8);					
				if (y < 30)
					assertTrue("Tile should have a row size of 64!",tile.getSizeY()==64);
				else if (y == 30)
					assertTrue("Tile should have a row size of 40!",tile.getSizeY()==40);
				else 
					assertTrue("Tile should have a col size of 8, got "
						+ tile.getSizeY() + "!",tile.getSizeY()==8);					
			}
		}
	}

	/**
	 * Test if implementation of {@link Iterable} works correctly. 
	 */
	@Test
	public void testIterableInterface() {

		// check sizes again by iterating over list of tiles
		int x=0, y=0, counter=0;
		for (MTBImage tile: this.testObj) {
			counter++;
			if (x!=15)
				assertTrue("Tile should have a col size of 64!, " +
									"but is " + tile.getSizeX() + "!",tile.getSizeX()==64);
			else
				assertTrue("Tile should have a col size of 40, " +
									"but is " + tile.getSizeX() + "!",tile.getSizeX()==40);
			if (y!=15)
				assertTrue("Tile should have a row size of 64! " + 
									"but is " + tile.getSizeY() + "!",tile.getSizeY()==64);
			else
				assertTrue("Tile should have a row size of 40!" +
									"but is " + tile.getSizeY() + "!",tile.getSizeY()==40);
			// increment counter
			++x;
			// if end of row is reached, jump to next row
			if (x==16) {
				++y;
				x=0;
			}
		}
		// check if all tiles were touched
		assertTrue("Did not check all tiles, counter < 256!?", counter == 16*16);
	}
	
	/**
	 * Test if implementation of {@link Iterable} works correctly. 
	 */
	@Test
	public void testIterableInterfaceOverlap() {

		// check sizes again by iterating over list of tiles
		int x=0, y=0, counter=0;
		for (MTBImage tile: this.testObjOverlap) {
			counter++;
			if (x<30)
				assertTrue("Tile should have a col size of 64!, " +
									"but is " + tile.getSizeX() + "!",tile.getSizeX()==64);
			else if (x==30)
				assertTrue("Tile should have a col size of 40, " +
									"but is " + tile.getSizeX() + "!",tile.getSizeX()==40);
			else 
				assertTrue("Tile should have a col size of 8, " +
									"but is " + tile.getSizeX() + "!",tile.getSizeX()==8);
			if (y<30)
				assertTrue("Tile should have a row size of 64! " + 
									"but is " + tile.getSizeY() + "!",tile.getSizeY()==64);
			else if (y==30)
				assertTrue("Tile should have a row size of 40!" +
									"but is " + tile.getSizeY() + "!",tile.getSizeY()==40);
			else 
				assertTrue("Tile should have a row size of 8!" +
									"but is " + tile.getSizeY() + "!",tile.getSizeY()==8);
			// increment counter
			++x;
			// if end of row is reached, jump to next row
			if (x==32) {
				++y;
				x=0;
			}
		}
		// check if all tiles were touched
		assertTrue("Did not check all tiles, counter < 1024!?", counter == 32*32);
	}

	/**
	 * Test if images are properly numbered upon saving tiles to disk.
	 * <p>
	 * Note, the folder and all the files are automatically removed by the 
	 * JUnit support for temporary directories.
	 */
	@Test
	public void testSavingRoutine() {

		// setup temporary folder for tile images
		boolean exceptionThrown = false;
		File testFileFolder;
    try {
	    testFileFolder = this.folder.newFolder("mitobo_TileAdapterJUnitData");
		
	    // save the tiles to the directory
	    this.testObj.saveTilesToFiles(testFileFolder.getAbsolutePath() 
	    		+ File.separator + "testTile");

	    // check if all files have been saved to disk
	    for (int i=0;i<256;++i) {
	    	File testFile = new File(testFileFolder.getAbsolutePath() 
	    			+ File.separator + "testTile-" + i + ".tif");
	    	assertTrue("File \"" + testFile.getName() + "\" not found! Error!",
	    			testFile.exists());
	    }
    } catch (IOException e) {
    	exceptionThrown = true;
    }
    assertFalse("Something went wrong while testing saving routine, " 
    	+ "caught an IOException...", exceptionThrown);
	}
}