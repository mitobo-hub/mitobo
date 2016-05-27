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

package de.unihalle.informatik.MiToBo.tools.strings;

import static org.junit.Assert.*;

import java.util.Vector;

import org.junit.Before;
import org.junit.Test;

/**
 * JUnit test class for {@link StringAnalysis}.
 * 
 * @author moeller
 */
public class TestStringAnalysis {

	/**
	 * Test set of strings.
	 */
	private Vector<String> testSet;
	
	/**
	 * Fixture.
	 */
	@Before
	public void initTestClass() {
		// nothing to do here
	}
	
	/**
	 * Test routine for longest common prefix extraction.
	 */
	@Test
	public void testGetLongestCommonPrefixes() {

		// fill set with test strings
		this.testSet = new Vector<String>();
		this.testSet.add("aabbccd");
		this.testSet.add("aabbddd");
		this.testSet.add("a");
		this.testSet.add("bbbaaacc");
		this.testSet.add("bbaaaccc");
		this.testSet.add("bbbaaadd");

		// get result
		Vector<String> result = 
			StringAnalysis.getLongestCommonPrefixes(this.testSet);
		// check if all expected prefixes have been found
		assertTrue("Expected to get 2 prefixes, but received " 
				+ result.size(), result.size() == 2);
		assertTrue(result.contains("aabb"));
		assertTrue(result.contains("bbbaaa"));
		
		// do another test
		this.testSet = new Vector<String>();
		this.testSet.add("HTshC-01");
		this.testSet.add("HTshC-02");
		this.testSet.add("HTshC-03");
		this.testSet.add("HTshC-04");
		this.testSet.add("HT384-01");
		this.testSet.add("HT384-02");
		this.testSet.add("HT384-03");
		this.testSet.add("HT384-04");

		result = StringAnalysis.getLongestCommonPrefixes(this.testSet);
		assertTrue("Expected to get 2 prefixes, but received " 
				+ result.size(), result.size() == 2);
		assertTrue(result.contains("HTshC-0"));
		assertTrue(result.contains("HT384-0"));
	}
}