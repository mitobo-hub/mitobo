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

package de.unihalle.informatik.MiToBo.visualization.drawing;

import java.util.HashMap;
import java.util.Random;


/**
 * A color lookup table that generates colors dynamically and randomly for indices that do not exist.
 * @author Oliver Gress
 *
 */
public class DynamicColorLUT {

	private Random rand;
	
	private HashMap<Integer,Integer> colors;
	
	private int coffset;
	
	/**
	 * Constructor with a (seeded) random generator and channel offset = 20.
	 */
	public DynamicColorLUT() {
		this.rand = new Random(1231223);
		this.colors = new HashMap<Integer,Integer>();
		this.coffset = 20;
	}
	
	/**
	 * Constructor for a specific random generator and channel offset = 20..
	 */
	public DynamicColorLUT(Random rand) {
		this.rand = rand;
		this.colors = new HashMap<Integer,Integer>();
		this.coffset = 20;
	}
	
	/**
	 * Constructor for a specific random generator and a given channel offset
	 * @param rand random generator
	 * @param channeloffset the minimum value of R, G and B
	 */
	public DynamicColorLUT(Random rand, int channeloffset) {
		this.rand = rand;
		this.colors = new HashMap<Integer,Integer>();
		this.coffset = channeloffset;
	}
	
	/**
	 * Set the color for given key.
	 */
	public void setColor(int key, int color) {
		this.colors.put(key, color);
	}
	
	/**
	 * Get the color for a given key. Generates a random color if <code>key</code> is not present.
	 * @param key lookup table key (or index)
	 * @return color coded into an integer (lowest byte: blue, second-lowest byte: green, third-lowest byte: red)
	 */
	public int getColor(int key) {
		
		if (colors.containsKey(key))
			return colors.get(key).intValue();
		else {
			// new id, create new random color
			int c = ((((int)(this.rand.nextDouble()*(256-this.coffset))) + this.coffset) << 16)
					+ ((((int)(this.rand.nextDouble()*(256-this.coffset))) + this.coffset) << 8)
					+ (((int)(this.rand.nextDouble()*(256-this.coffset))) + this.coffset);
			
			colors.put(key, c);
			
			return c;
		}
	}
	
	
}
