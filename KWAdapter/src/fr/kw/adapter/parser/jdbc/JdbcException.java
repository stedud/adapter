package fr.kw.adapter.parser.jdbc;

/**
 * @author sdu01
 *
 *         To change this generated comment edit the template variable
 *         "typecomment": Window>Preferences>Java>Templates. To enable and
 *         disable the creation of type comments go to
 *         Window>Preferences>Java>Code Generation.
 */
public class JdbcException extends Exception {

	private Exception initialException = null;

	/**
	 * Constructor for JdbcException.
	 */
	public JdbcException() {
		super();
	}

	/**
	 * Constructor for JdbcException.
	 * 
	 * @param arg0
	 */
	public JdbcException(String arg0) {
		super(arg0);
	}

	public JdbcException(String arg0, Exception e) {
		super(arg0);
		this.initialException = e;
	}

	public Exception getInitialException() {
		return this.initialException;
	}

}
