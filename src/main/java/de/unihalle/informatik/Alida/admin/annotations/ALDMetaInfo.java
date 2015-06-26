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

/* 
 * Most recent change(s):
 * 
 * $Rev$
 * $Date$
 * $Author$
 * 
 */

package de.unihalle.informatik.Alida.admin.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.Documented;

/**
 * Annotation for adding meta data to Alida classes.
 * 
 * @author moeller
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented

@ALDMetaInfo(export=de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo.ExportPolicy.ALLOWED)
public @interface ALDMetaInfo {

	/**
	 * Class export policy.
	 * 
	 * @author moeller
	 */
	enum ExportPolicy { 
		/**
		 * Class is to be exported in any case. 
		 */
		MANDATORY, 
		/**
		 * Export is allowed if necessary due to dependencies.
		 */
		ALLOWED, 
		/**
		 * Export is explicitly forbidden.
		 */
		FORBIDDEN 
	}

	/**
	 * Returns export policy for annotated class.
	 */
	ExportPolicy export() default de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo.ExportPolicy.FORBIDDEN;
}
