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

package de.unihalle.informatik.MiToBo.core.operator;

import java.io.*;

import de.unihalle.informatik.Alida.version.ALDVersionProvider;

/**
 * Info class which provides MiToBo plugins with version information 
 * from release file in a jar archive.
 *
 * @author moeller
 */
public class MTBVersionProviderReleaseFile extends ALDVersionProvider {
	
	/**
	 * Local version information.
	 */
	private static String localVersion = null;
	
	/**
	 * Name of revision file from where to read revision information.
	 * <p>
	 * If the name needs to be changed to a non-default name this needs to  
	 * be done prior to the first call of getVersion() of the provider.
	 */
	private static String revisionFile = "revision-mitobo.txt";

	/**
	 * Set the name of the revision file.
	 * @param revFile		Name of file to be used.
	 */
	public static void setRevisionFile(String revFile) {
		revisionFile = revFile;
	}
	
	/**
	 * Request name of the revision file.
	 * @return revFile		Name of file.
	 */
	public static String getRevisionFile() {
		return revisionFile;
	}

	@Override
  public String getVersion() {
	  return getRepositoryTag();
  }
	
	/**
	 * Returns the tag/release of the current jar.
	 * <p>
	 * If a file is passed to the function the tag/release information is  
	 * extracted from that file. If the file does not exist or is empty,
	 * a dummy string is returned.
	 * 
	 * @param infofile 	file where to find the tag information,
	 *                  for MiToBo this is usually './revision-mitobo.txt'
	 * @return 			Tag of version or dummy string if tag not available.
	 */
	private static String getRepositoryTag(String infofile) {

		InputStream is= null;
		BufferedReader br= null;
		String vLine= null;

		String dummy= "Unknown_Release";

		if (MTBVersionProviderReleaseFile.localVersion != null) {
			return MTBVersionProviderReleaseFile.localVersion;
		}

			// initialize file reader 
		try { 
			is= MTBVersionProviderReleaseFile.class.getResourceAsStream(
						"/" + infofile);
			br= new BufferedReader(new InputStreamReader(is));
			vLine= br.readLine();
			if (vLine == null) {
				System.err.println("getRepositoryTag: file is empty...!?");
				br.close();
				MTBVersionProviderReleaseFile.localVersion = dummy;
				return dummy;
			}	
			br.close();
			MTBVersionProviderReleaseFile.localVersion = vLine;
			return vLine;
		}
		catch (Exception e) {
			try {
				if (br != null)
					br.close();
				if (is != null)
					is.close();
			} catch (IOException ee) {
				System.err.println(
					"[MTBVersionProviderReleaseJar::getReleaseVersion] "
						+ "problems on closing the file handles...");
				ee.printStackTrace();
			}
			MTBVersionProviderReleaseFile.localVersion = dummy;
			return dummy;
		}
	}

	/**
	 * Returns the tag/release of the current checkout, as specified 
	 * in a given info file.
	 * 
	 * @return tag/release of checked out version or null if not available
	 */
	private static String getRepositoryTag() {
		MTBVersionProviderReleaseFile.localVersion = 
			MTBVersionProviderReleaseFile.getRepositoryTag(revisionFile);
		return MTBVersionProviderReleaseFile.localVersion;
	}
}
