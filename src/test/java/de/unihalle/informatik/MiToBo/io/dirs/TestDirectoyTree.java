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

package de.unihalle.informatik.MiToBo.io.dirs;

import static org.junit.Assert.*;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Vector;

import org.junit.Before;
import org.junit.Test;

import de.unihalle.informatik.Alida.helpers.ALDFilePathManipulator;

/**
 * JUnit test class for {@link DirectoryTree}.
 * 
 * @author moeller
 */
public class TestDirectoyTree {

	/**
	 * Fixture.
	 */
	@Before
	public void initTestClass() {
		// nothing to do here
	}
	
	/**
	 * Test the functionality of {@link DirectoryTree}.
	 */
	@Test
	public void testDirectoryTree() {		
		
		boolean exceptionThrown = false;
		try {
			URL url = this.getClass().getResource("/");
			URI uri = new URI(url.toString());
			String testDir = uri.getPath() + File.separator + "io/dirA"; 
			
			// test non-recursive processing
			DirectoryTree testTree = new DirectoryTree(testDir, false);
			Vector<String> files = testTree.getFileList();
			
			assertTrue("Expected to find two files, but found " + files.size(), 
					files.size() == 2);
			String file = files.get(0);
			file = ALDFilePathManipulator.removeLeadingDirectories(file);
			assertTrue("First file should be testFileA.txt, but is " + file,
					file.equals("testFileA1.txt"));
			file = files.get(1);
			file = ALDFilePathManipulator.removeLeadingDirectories(file);
			assertTrue("Second file should be testFileA2.txt, but is " + file,
					file.equals("testFileA2.txt"));

			// test recursive processing
			testTree = new DirectoryTree(testDir);
			files = testTree.getFileList();
			
			assertTrue("Expected to find four files, but found " 
					+ files.size(),	files.size() == 5);
			file = files.get(0);
			// throw away any absolute path above test folder
			file = file.substring(file.indexOf("//")+2);
			assertTrue("First file should be io/dirA/testFileA.txt, but is " 
					+ file,	file.equals("io/dirA/testFileA1.txt"));
			file = files.get(1);
			// throw away any absolute path above test folder
			file = file.substring(file.indexOf("//")+2);
			assertTrue("Second file should be io/dirA/testFileA2.txt, but is " 
					+ file,	file.equals("io/dirA/testFileA2.txt"));
			file = files.get(2);
			// throw away any absolute path above test folder
			file = file.substring(file.indexOf("//")+2);
			String targetFile = 
					"io/dirA/subDirAA/subSubDirAAB/testFileAAB.txt";
			assertTrue("Second file should be " + targetFile + ", but is " 
					+ file,	file.equals(targetFile));
			file = files.get(3);
			// throw away any absolute path above test folder
			file = file.substring(file.indexOf("//")+2);
			targetFile = 
				"io/dirA/subDirAA/subSubDirAAB/subSubSubDirAABB/testFileAABB.txt";
			assertTrue("Second file should be " + targetFile + ", but is " 
					+ file,	file.equals(targetFile));
			file = files.get(4);
			// throw away any absolute path above test folder
			file = file.substring(file.indexOf("//")+2);
			targetFile = "io/dirA/subDirAB/testFileAB.txt";
			assertTrue("Second file should be " + targetFile + ", but is " 
					+ file,	file.equals(targetFile));

			// test getting list of sub-directories...
			testTree = new DirectoryTree(testDir);
			Vector<String> subDirs = testTree.getSubdirectoryList();
			
			assertTrue("Expected to find four sub-directories, but found " 
					+ subDirs.size(), subDirs.size() == 4);
			String dir = subDirs.get(0);
			// throw away any absolute path above test folder
			dir = dir.substring(dir.indexOf("//")+2);
			String targetDir = "io/dirA/subDirAA";
			assertTrue("First directory should be " + targetDir + ", but is " 
					+ dir, dir.equals(targetDir));
			
			dir = subDirs.get(1);
			// throw away any absolute path above test folder
			dir = dir.substring(dir.indexOf("//")+2);
			targetDir = "io/dirA/subDirAA/subSubDirAAB";
			assertTrue("First directory should be " + targetDir + ", but is " 
					+ dir, dir.equals(targetDir));
			
			dir = subDirs.get(2);
			// throw away any absolute path above test folder
			dir = dir.substring(dir.indexOf("//")+2);
			targetDir = "io/dirA/subDirAA/subSubDirAAB/subSubSubDirAABB";
			assertTrue("First directory should be " + targetDir + ", but is " 
					+ dir, dir.equals(targetDir));

			dir = subDirs.get(3);
			// throw away any absolute path above test folder
			dir = dir.substring(dir.indexOf("//")+2);
			targetDir = "io/dirA/subDirAB";
			assertTrue("First directory should be " + targetDir + ", but is " 
					+ dir, dir.equals(targetDir));

		} catch (URISyntaxException e) {
			exceptionThrown = true;
		}
		assertFalse("Caught an exception on execution...!?", 
				exceptionThrown);
	}

}