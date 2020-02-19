package de.unihalle.informatik.MiToBo.io.images;

import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;

/**
 * Special exception for the ImageWriterMTB operator.
 * This exception is thrown if the file to write to already exists, but the
 * overwrite flag is set to 'false'.
 * 
 * @author Oliver Gress
 *
 */
public class OverwriteException extends ALDOperatorException {

	public OverwriteException(OperatorExceptionType t, String c) {
		super(t, c);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 3887795755287599831L;

}
