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

import java.io.File;

//import de.unihalle.informatik.MiToBo.apps.neurites2D.NeuritDetector2DGUI;

/**
 * Class to filter the files by a file chooser. Only images of the types below are
 * allowed.
 * 
 * 
 * @author misiak
 * 
 */

/* ImageFilter.java is used by FileChooserDemo2.java. */
public class ImageFilter extends javax.swing.filechooser.FileFilter implements java.io.FileFilter {
	
	  private String jpeg = "jpeg";
	  private String jpg = "jpg";
	  private String gif = "gif";
	  private String tiff = "tiff";
	  private String tif = "tif";
	  private String png = "png";
	  private String lif = "lif";
	  private String pgm = "pgm";
	  private String ppm = "ppm";
	  private String ext = null;
	  private String name = null;
	
	protected String[] extensions = null;
	
	public ImageFilter() {
		this.extensions = new String[9];
		this.extensions[0] = "jpeg";
		this.extensions[1] = "jpg";
		this.extensions[2] = "gif";
		this.extensions[3] = "tiff";
		this.extensions[4] = "tif";
		this.extensions[5] = "png";
		this.extensions[6] = "lif";
		this.extensions[7] = "pgm";
		this.extensions[8] = "ppm"; 
	}
	
	public ImageFilter(String[] extensionlist) {
		this.extensions = extensionlist;
	}



  // Accept all directories and all gif, jpg, tiff, or png files.
  @Override
  public boolean accept(File f) {
    if (f.isDirectory()) {
      return true;
    }
    String extension = FilePathManipulator.getExtension(f.getPath().toLowerCase());
    if (extension != null) {
    	boolean inList = false;
    	
    	for (int i = 0; i < this.extensions.length; i++) {
    		inList = inList || (extension.compareTo(this.extensions[i])==0);
    		if (inList)
    			break;
    	}
    	
    	return inList;
    }
    return false;
  }

  // Accept all directories and all gif, jpg, tiff, or png files.
  public boolean acceptStacks(File f) {
    if (f.isDirectory()) {
      return true;
    }
    String extension = FilePathManipulator.getExtension(f.getPath().toLowerCase());
    if (extension != null) {
    	
    	boolean inList = false;
    	
    	for (int i = 0; i < this.extensions.length; i++) {
    		inList = inList || (extension.compareTo(this.extensions[i])==0 &&
    							(extension.equals(tiff) || extension.equals(tif)
    				          || extension.equals(gif) || extension.equals(jpeg)
    				          || extension.equals(jpg) || extension.equals(png)
    				          || extension.equals(lif)));
    		if (inList)
    			break;
    	}
    	
    	return inList;
    }
    return false;
  }

  // The description of this filter
  @Override
  public String getDescription() {
	  String s = "*." + this.extensions[0];
	  
	  for (int i = 1; i < this.extensions.length; i++)
		  s += ";*." + this.extensions[i];
	  
    return s;
  }
}
