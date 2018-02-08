package org.rohub.rodl.exceptions;


/**
 * <p>
 * A runtime exception thrown when user does not have permission to access particular resource.
 * </p>
 * 
 * @author nowakm
 */

public class ForbiddenException extends RuntimeException {

	private static final long serialVersionUID = 4191934331043521020L;

	/**
     * Constructor.
     * 
     * @param message
     *            message
     */
    public ForbiddenException(String message) {
        super(message);
    }

}
