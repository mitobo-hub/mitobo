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

package de.unihalle.informatik.MiToBo.math.distributions.interfaces;


/**
 * Interface for conditional densities, e.g. p(x|y)
 * @author Oliver Gress
 *
 * @param <T> class type of the conditional variable, i.e. y in the upper example
 */
public interface ConditionalDistribution<T> {

	/**
	 * Get conditional variable
	 * @return object which specifies the condition
	 */
	T getCondition();
	
	/**
	 * Set the conditional variable
	 * @param condition conditional variable
	 */
	void setCondition(T condition);
}
