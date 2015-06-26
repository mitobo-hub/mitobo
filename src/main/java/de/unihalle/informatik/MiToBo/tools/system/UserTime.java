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

/* derived from:
 */

/*
 * This file is part of Jstacs.
 * 
 * Jstacs is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * Jstacs is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * Jstacs. If not, see <http://www.gnu.org/licenses/>.
 * 
 * For more information on Jstacs, visit http://www.jstacs.de
 */

package de.unihalle.informatik.MiToBo.tools.system;

import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo;
import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo.ExportPolicy;

/**
 * This is a stopwatch to measure elapsed user or real time used.
 * <p>
 * If the proper native library is availabe and is requested user time is 
 * elapsed. Otherwith real time is elapsed.
 * 
 * If you like to use this class you must set the VM option
 * <code>-Djava.library.path</code> to the directory where the native library
 * resides.<br />
 * The native library is
 * <ul>
 * <li> <code>JNI_time.dll</code> for Windows,</li>
 * <li> <code>libJNI_time.so</code> for Linux,</li>
 * <li> <code>libJNI_time.jnilib</code> for Mac OS X.</li>
 * </ul>
 * 
 * @author Jens Keilwagen, modified: Stefan Posch
 */
@ALDMetaInfo(export=ExportPolicy.ALLOWED)
public class UserTime {

	/** Were we able to load the JNI library?
      */
	static boolean haveJNI;

	/** Basename of the JNI library.
	 */
	static final String timeLib = new String( "JNI_time");

	/** Does this instance use JNI and thus user time? 
      */
	private boolean useJNI;

	/** For internal use.
	 */
	private static boolean debug = false;

	/*
	 * Static initialization of class, loading of library, if available.
	 */
	static {
		if ( debug ) {
			String javaLibraryPath = System.getProperty("java.library.path");
			System.out.println("java.library.path=" + javaLibraryPath);
			System.out.println("   " + System.mapLibraryName( timeLib));
		}

		try {
			System.loadLibrary( timeLib );
			haveJNI = true;
			if ( debug )
				System.out.println("   "+System.mapLibraryName( timeLib)+" found");
		}
		catch(UnsatisfiedLinkError e) {
			if ( debug )
				System.out.println("   "+System.mapLibraryName( timeLib)+" not found\n" 
						+	e.getMessage());
			haveJNI = false;
		}
	}

	/**
	 * Variable to save start value.
	 */
	float start;
	/**
	 * Variable to remember ticks.
	 */
	float ticks;

	/**
	 * Variable to save real start value.
	 */
	long startReal;

	/**
	 * Creates a new time object and starts the clock.
	 * Elapses user time, if runtime library is available, 
	 * otherwise the real time.
	 */
	public UserTime() {
		this( true);
	}

	/**
	 * Creates a new time object and starts the clock elapsing real time.
	 * Elapses user time, if <code>useUsertime</code> is true and the runtime 
	 * library is available, otherwise the real time.
	 */
	public UserTime( boolean useUsertime) {
		if ( useUsertime && haveJNI )
			this.useJNI = true;
		else
			this.useJNI = false;

		reset();
	}

	/**
	 * Returns the elapsed time since last reset (or invoking the constructor) in seconds
	 */
	public double getElapsedTime() {
		if ( this.useJNI )
			return ( getUserTime() - this.start ) / this.ticks;
		return ( System.currentTimeMillis() - this.startReal ) / 1000d;
	}

	/** 
	 * Reset time
	 */
	public void reset() {
		if ( this.useJNI ) {
			this.ticks = getTicks();
			this.start = getUserTime();
		} else {
			this.startReal = System.currentTimeMillis();
		} 
	}

	/** 
	 * Return UserTime or RealTime depending on mode of operation 
	 */
	public String getOperation() {
		if ( this.useJNI ) {
			return new String( "UserTime");
		}
		return new String( "RealTime"); 
	}

	/**
	 * Declaration of native method for user time measurement. 
	 * @return	Current user time.
	 */
	private native float getUserTime();

	/**
	 * Declaration of native method for getting ticks. 
	 * @return	Ticks.
	 */
	private native long getTicks();
}
