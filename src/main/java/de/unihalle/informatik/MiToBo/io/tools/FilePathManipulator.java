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

/*
 * Copyright (c) 1995 - 2008 Sun Microsystems, Inc.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Sun Microsystems nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/**
 * Costum file filter for image files only.
 * 
 *  @author misiak
 *  @see NeuriteDetector2DGUI
 *  
 *  last update: 20090427
 */

package de.unihalle.informatik.MiToBo.io.tools;

/**
 * Class providing helpers to manipulate file and directory paths.
 * 
 * @author moeller, posch
 */
public class FilePathManipulator {

		/**
		 * Remove extension from filename if any.
		 * 
		 * @param str
		 *          Filename to process.
		 * @return Filename without extension.
		 * 
		 */
		public static String removeExtension(String str) {

				String separator = System.getProperty("file.separator");
				String filename;
				String path; // str before last file.separator

				// Remove the path upto the filename.
				int lastSeparatorIndex = str.lastIndexOf(separator);
				if (lastSeparatorIndex == -1) {
						filename = str;
						path = new String("");
				} else {
						filename = str.substring(lastSeparatorIndex + 1);
						path = str.substring(0, lastSeparatorIndex + 1);
				}

				// Remove the extension.
				int extensionIndex = filename.lastIndexOf(".");
				if (extensionIndex == -1)
						return path + filename;

				return path + filename.substring(0, extensionIndex);
		}

		/**
		 * Get extension from filename if any.
		 * 
		 * @param str
		 *          Filename to be processed.
		 * @return Extension or empty string.
		 * 
		 */
		public static String getExtension(String str) {
				String separator = System.getProperty("file.separator");
				String filename;

				// Remove the path upto the filename.
				int lastSeparatorIndex = str.lastIndexOf(separator);
				if (lastSeparatorIndex == -1) {
						filename = str;
				} else {
						filename = str.substring(lastSeparatorIndex + 1);
				}

				// Remove the extension.
				int extensionIndex = filename.lastIndexOf(".");
				if (extensionIndex == -1)
						return new String("");
				return filename.substring(extensionIndex + 1);
		}

		/**
		 * Remove all leading pathname components from a filename.
		 * 
		 * @param str
		 *          Filename to be processed.
		 * @return Tail of the filename.
		 */
		public static String removeLeadingDirectories(String str) {
				String separator = System.getProperty("file.separator");
				String filename;

				// Remove the path upto the filename.
				int lastSeparatorIndex = str.lastIndexOf(separator);
				if (lastSeparatorIndex == -1) {
						filename = str;
				} else {
						filename = str.substring(lastSeparatorIndex + 1);
				}
				return filename;
		}

		/**
		 * Returns the file name without leading directories or file description
		 * (extension).
		 * 
		 * @param str
		 *          filename to be processed
		 * @return Filename without leading directories and file extension.
		 */
		public static String getFileName(String str) {
				String filename = removeExtension(str);
				filename = removeLeadingDirectories(filename);
				return filename;
		}
}
