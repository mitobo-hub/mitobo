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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

/**
 * Class providing helpers to analyze (sets of) strings.
 * 
 * @author moeller
 */
public class StringAnalysis {

	/**
	 * Extracts the set of all prefixes shared by at least two strings in the 
	 * given set.
	 * 
	 * @param vec		Set of strings to be analyzed.
	 * @return	Set of common prefixes.
	 */
	public static Vector<String> getLongestCommonPrefixes(Vector<String> vec) {
		// call the recursive sub-routine
		return findPrefixes(vec);
	}
	
	/**
	 * Helper to recursively extract common prefixes.
	 * @param vec		Set of strings to analyze.
	 * @return	Set of common prefixes.
	 */
	protected static Vector<String> findPrefixes(Vector<String> vec) {
		
		// hash table for equivalence groups
		HashMap<Integer, Vector<String>> htab = 
				new HashMap<Integer, Vector<String>>();

		// identify groups, i.e., subsets of strings starting with same character
		char groupChar;
		int groupID= 0;
		for (String s : vec) {
			if (s.isEmpty())
				continue;
			
			groupChar= s.charAt(0);
			groupID= groupChar;

			if (htab.containsKey(new Integer(groupID))) {
				// es wurde schon ein Suffix mit 'groupChar' gefunden
				htab.get(new Integer(groupID)).add(s);
			}
			else {
				// andernfalls wird eine neue SuffixGroup angelegt
				htab.put(new Integer(groupID), new Vector<String>());
				htab.get(new Integer(groupID)).add(s);
			}
		}
		
		// allocate result vector
		Vector<String> result = new Vector<String>();

		// for each group, find common prefixes
		Set<Integer> keys= htab.keySet();
		Iterator<Integer> kit= keys.iterator();
		while (kit.hasNext()) {
			Vector<String> group = htab.get(kit.next());
			if (group.size() == 1) {
				// group contains just one element, i.e., is to be ignored
				continue;
			}

			// group contains more than one element, search for common prefix
			int j= 0;
			boolean prefixFound= false;
			String prefix= new String();
			while(!prefixFound) {
				if (group.get(0).length() == j)
					break;

				char c= group.get(0).charAt(j);
				boolean noMatch= false;
				for (int k= 0; k<group.size() && !noMatch; ++k) {
					if (group.get(k).length() == j)
						noMatch= true;
					else {
						if (group.get(k).charAt(j) != c)
							noMatch= true;
					}
				}
				if (!noMatch) {
					prefix= prefix + c;
					j++;
				}
				else {
					prefixFound= true;
				}	
			}
			
			// if a prefix was found, recursively work on remainig suffixes
			if (!prefix.isEmpty()) {
				Vector<String> suffixes = new Vector<String>();
				for (String s: group) {
					suffixes.add(s.replaceFirst(prefix, ""));
				}
				
				// recursive call
				Vector<String> sufPrefixes = findPrefixes(suffixes);
				
				// if no more prefixes were found, the current prefix is complete 
				if (sufPrefixes.isEmpty()) {
					result.add(prefix);
				}
				// otherwise extend current prefix
				else {
					for (String s: sufPrefixes) {
						result.add(prefix + s);
					}
				}
			}
		}
		// return the overall result
		return result;
	}
}