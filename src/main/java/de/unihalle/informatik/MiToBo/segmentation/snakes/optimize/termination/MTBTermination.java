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

/* 
 * Most recent change(s):
 * 
 * $Rev$
 * $Date$
 * $Author$
 * 
 */

package de.unihalle.informatik.MiToBo.segmentation.snakes.optimize.termination;

import de.unihalle.informatik.Alida.annotations.ALDClassParameter;
import de.unihalle.informatik.Alida.annotations.ALDParametrizedClass;
import de.unihalle.informatik.MiToBo.segmentation.snakes.optimize.SnakeOptimizerSingle;
import de.unihalle.informatik.MiToBo.segmentation.snakes.optimize.SnakeOptimizer.Snake_status;

/**
 * Class for checking termination criteria of the current snake iteration.
 * Termination criteria can be defined by several methods, which can
 * be found in the derived classes in detail.
 * 
 * @author misiak
 */
@ALDParametrizedClass
public abstract class MTBTermination implements Cloneable {
	
	/**
	 * Flag to turn display of additional information on/off.
	 */
	@ALDClassParameter(label="Verbose", dataIOOrder = 100)
	protected boolean verbose = false;

		/**
		 * Current snake optimizer with all its parameters and fields.
		 * <p>
		 * Parameters and fields include, e.g., the current
		 * snake, old snake, gamma values and energies.
		 */
		protected SnakeOptimizerSingle optimizer;
		/**
		 * Status of current snake. 
		 * <p>
		 * Termination status can be SNAKE_DONE or SNAKE_SUCCESS.
		 */
		protected Snake_status status;

		/**
		 * Method to check termination status of the snake. 
		 * Necessary for all subclasses.
		 * 
		 * @return Status of the current snake.
		 */
		public abstract Snake_status terminate();

		/**
		 * Initializing routine which is called once before the termination is
		 * actually used.
		 * <p>
		 * In this routine global parameter settings can be handled or other
		 * initialization stuff be done. The SnakeOptimizer will call this routine
		 * once before the actual use of the termination strategy. If no stuff needs
		 * to be done in advance the routine should at least return true.
		 * 
		 * @param opt
		 *          calling snake optimizer
		 * @return True if init was successful.
		 */
		public abstract boolean init(SnakeOptimizerSingle opt);

		/**
		 * Method for short termination description name.
		 */
		@Override
		public abstract String toString();
		

		/**
		 * Clones this object.
		 * <p>
		 * Note that all internal variables should be cloned. As this is not 
		 * possible for the {@link SnakeOptimizerSingle} object the internal 
		 * reference should be left to null. It will be properly initialized 
		 * later when {@link SnakeOptimizerSingle.initOptimizer()} is invoked.
		 */
		/* (non-Javadoc)
		 * @see java.lang.Object#clone()
		 */
		@Override
    public abstract MTBTermination clone() throws CloneNotSupportedException;
}
