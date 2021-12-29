package net.sourceforge.ck2httt.pxAnalyzer;

/**
 * This class represents any data that has been read during the analysis.<p>
 * 
 * @author Copyright 2007 Yves Prélot ; distributed under the terms of the GNU General Public License
 */
public class Token {

	/**
	 * The interface defining the methods required by Analyzer.
	 */
	public interface Tokenizer {
		/** Returns the next CLOSE Token that closes the current structure. */
	    Token getNextClose();
	    /** Returns the next token in the flow */
	    Token getNext();
	    /** Returns the current position in the data ; used only in case of error. */
	    int   getPos();
	};

	// PX Tokens:
	// OPEN    = {	
	// CLOSE   = }
	// EQUAL   = =
	// STRING  = "xxxxx"
	// TOKEN   = other contiguous alphanum including {-,_,.}
	// END     = end reached
	
	public enum TokenType {
		OPEN, CLOSE, EQUAL, STRING, TOKEN, ERROR, END
	};

	/**
	 * Kind of data for that PXTokens ; the Hook interface will only receive TOKEN
	 * and STRING
	 */
	final public TokenType _token;
	/**
	 * Position of the first character of that PXTokens in the data flow
	 */
	final public int _beg;
	/**
	 * Position of the last character of that PXTokens in the data flow
	 */
	final public int _end;
	/**
	 * Value of the token converted as a Java String
	 */
	final public String _value;
	/**
	 * Line where this PXTokens was read
	 */
	final public int _line;

	/**
	 * Constructor
	 * 
	 * @param token
	 *            Type of PXTokens
	 * @param begin
	 *            Position of the first character of that PXTokens in the data flow
	 * @param end
	 *            Position of the last character of that PXTokens in the data flow
	 * @param value
	 *            String value for that PXTokens
	 * @param line
	 *            Line number where this PXTokens was found
	 */
	public Token(TokenType token, int begin, int end, String value, int line) {
		_token = token;
		_beg = begin;
		_end = end;
		_value = value;
		_line = line;
	}

	/**
	 * Constructor by copy
	 */
	public Token(Token t) {
		_token = t._token;
		_beg = t._beg;
		_end = t._end;
		_value = t._value;
		_line = t._line;
	}

	/**
	 * Visual representation of the PXTokens, showing both Type and Value
	 */
	public String toString() {
		return (_value != null && _value.length() > 0) ? _token.name() + " ("
				+ _value + ", line:" + _line + ")" : _token.name() + "(line:"
				+ _line + ")";
	}
};
	
