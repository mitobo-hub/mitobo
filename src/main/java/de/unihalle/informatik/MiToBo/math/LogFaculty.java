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

package de.unihalle.informatik.MiToBo.math;

import java.util.Arrays;


/**
 * Class to compute the logarithm of the faculty of an integer n.
 * This class stores all values from log(0!) to log(n_max!) to provide fast access for values of n in the range [0,n_max].
 * If values for n > n_max are requested, the internal array is extended and n_max := n.  
 * @author Oliver Gress
 *
 */
public class LogFaculty {
	
	/** array to store log(0!) to log(n!) */
	private double[] logfac;
	
	/**
	 * Constructor to precompute log(0!) to log(n_max!)
	 * @param n_max
	 */
	public LogFaculty(int n_max) {
		this.logfac = new double[n_max+1];
		
		this.logfac[0] = 0;
		for (int n = 1; n <= n_max; n++)
			this.logfac[n] = Math.log(n) + this.logfac[n-1];
	}
	
	/**
	 * log(n!)
	 * @param n
	 * @return
	 */
	public double getLogFaculty(int n) {
		if (n >= this.logfac.length) {
			int nmax_old = this.logfac.length-1;
			
			this.logfac = Arrays.copyOf(this.logfac, n+1);
			
			for (int nn = nmax_old+1; nn <= n; nn++)
				this.logfac[nn] = Math.log(nn) + this.logfac[nn-1];
		}
		
		return this.logfac[n];
	}
	
	/**
	 * log(n1! / n2!)
	 * @param n1 nominator faculty
	 * @param n2 denominator faculty
	 * @return log of n1!/n2!
	 */
	public double getLogFacultyFraction(int n1, int n2) {

		if (n1 >= this.logfac.length || n2 >= this.logfac.length) {
			int n = (n1 > n2) ? n1 : n2;
			int nmax_old = this.logfac.length-1;
			
			this.logfac = Arrays.copyOf(this.logfac, n+1);
			
			for (int nn = nmax_old+1; nn <= n; nn++)
				this.logfac[nn] = Math.log(nn) + this.logfac[nn-1];
		}
		
		return this.logfac[n1] - this.logfac[n2];
	}
	
	public int getMaxN() {
		return this.logfac.length;
	}

}
