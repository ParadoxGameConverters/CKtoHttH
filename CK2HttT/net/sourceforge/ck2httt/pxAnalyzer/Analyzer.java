package net.sourceforge.ck2httt.pxAnalyzer;
import java.io.Reader;

import net.sourceforge.ck2httt.pxAnalyzer.Token.*;


/**
 * This class is meant to analyze any paradox file.
 * <p>
 * It contains one method <b>analyze(Reader, Hooks);</b> which does all the
 * analyzing stuff.
 * <p>
 * 
 * The Hooks interface defines exactly what has to be done when each element of
 * the data is met and will have to be implemented on a case by case if you want
 * to do more than just check the data grammatical correctness.
 * <p>
 * <br>
 * This package contains several Hooks implementations:
 * <ul>
 * <li>PXAnalyzer.NoHooks is an implementation that just does nothing</li>
 * <li>PXTree is an implementation of Hooks that loads the data into memory</li>
 * <li>PXchooser is an implementation of Hooks that filters out unneeded data</li>
 * <li>PXTreeChooser merges the tow previous implementations together in a
 * powerful do-all analyzer</li>
 * </ul>
 * 
 * Because our grammar is very simple, we are able to make a simple grammatic
 * analysis.
 * <ul>
 * <li>Name = TOKEN EQUAL</li>
 * <li>Data = TOKEN | STRING</li>
 * <li>List = OPEN DataList CLOSE | OPEN CLOSE</li>
 * <li>Struct = OPEN CompoundList CLOSE | OPEN CLOSE | OPEN AnonList CLOSE</li>
 * <li>Anon = OPEN BaseList CLOSE</li>
 * <li>Compound = Name Data | Name Struct | Name List</li>
 * <li>BaseList = Name Data | Name Data BaseList</li>
 * <li>DataList = Data | Data DataList</li>
 * <li>AnonList = Anon | Anon AnonList</li>
 * <li>CompoundList = Compound | Compound CompoundList</li>
 * <li>All = CompoundList END</li>
 * </ul>
 * List and Struct are ambiguous when empty. However, both occupy the same
 * grammatical niche and the ambiguity is acceptable.<p>
 * 
 * We can analyse any data by fetching at most 3 tokens in advance.<p>
 * 
 * NOTE : Anon is found in HOI and VIC<p>
 * 
 * @see PXTree
 * @see PXChooser
 * @see PXTreeChooser
 * @author Copyright 2007 Yves Pr�lot ; distributed under the terms of the GNU General Public License
 */
public class Analyzer {

	/**
	 * This interface specifies what we will be doing during the analysis.
	 * The returns are:<p>
	 * <ul>
	 *   <li>false : nothing special : continue processing normally</li>
	 *   <li>true  :<br>
	 *     <ul>
	 *     <li> in the "before" calls, skip the current sub-structure being entered</li>
	 *     <li> in the "after"  calls, skip the current (upper) structure being analyzed</li>
	 *     </ul>
	 *     </li>
	 * </ul>
	 * You can always return false safely ; the return is only used to accelerate the analysis.<p>
	 * The Token passed is the appropriate Token for the place you are : for example, in 
	 * beforeStruct, the Token you get is the opening {.<p>
	 */
	static public interface Hooks {
		/**called before we process the first element in the compound list from a structure ; current Token is { */
		boolean beforeStruct(Token t);

		/**called after we process the last element in the compound list from a structure ; current Token is } */
		boolean afterStruct(Token t);

		/**called before we process the first element in the base list from an anonymous structure ; current Token is { */
		boolean beforeAnon(Token t);

		/**called after we process the last element in the base list from an anonymous structure ; current Token is } */
		boolean afterAnon(Token t);

		/**called before we process the first element in a list ; current Token is { */
		boolean beforeList(Token t);

		/**called after we process the last element in a list ; current Token is } */
		boolean afterList(Token t);

		/**called before we process the first element in an empty ; current Token is {*/
		void beforeEmpty(Token t);

		/**called after we process the first element in an empty ; current Token is }*/
		boolean afterEmpty(Token t);

		/**called before the actual analysis begins ; useful for initialization stuff and such. */
		void begin();

		/**called after a successful analysis ; current Token is END. */
		void end(Token t);

		/**called when a Name is found : current Token is the name */
		void getName(Token t);

		/**called when a BaseField is found : current Token is the name */
		void beforeBase(Token t);

		/**called when a BaseField data is found : current Token is the data */
		boolean afterBase(Token t);

		/**called when a data from a List is found : current Token is the data */
		boolean afterListData(Token t);
	};

	/**
	 * This Hooks implementation just does nothing....<p>
	 * However, it's a good starting point for anything you might consider doing.<p>
	 */
	static public class NoHooks implements Hooks {
		public boolean beforeStruct(Token t) {
			return false;
		};

		public boolean afterStruct(Token t) {
			return false;
		};

		public boolean beforeAnon(Token t) {
			return false;
		};

		public boolean afterAnon(Token t) {
			return false;
		};

		public boolean beforeList(Token t) {
			return false;
		};

		public boolean afterList(Token t) {
			return false;
		};

		public void beforeEmpty(Token t) {
		};

		public boolean afterEmpty(Token t) {
			return false;
		};

		public void begin() {
		};

		public void end(Token t) {
		};

		public void getName(Token t) {
			_lastName = t;
		};

		public void beforeBase(Token t) {
		};

		public boolean afterBase(Token t) {
			return false;
		};

		public boolean afterListData(Token t) {
			return false;
		};

		/** last name Token read in the flow */
		protected Token _lastName;
	};

	private Tokenizer _t; // analyzes the data
	private Token[] _stack = new Token[4]; // stores fetched tokens (min required 3 ; more for more contextual info when grammatic error met)
	private int _p = 0; // stack current size
	private Hooks _h;
	private boolean _skip = false; // true if skip the end of the calling level

	public Analyzer(Tokenizer p) {
		this(p, new NoHooks());
	}

	public Analyzer(Tokenizer p, Hooks h) {
		_t = p;
		fill();
		_h = h;
	}

	/* refills the stack with tokens */
	private final void fill() {
		while (_p < _stack.length) {
			_stack[_p++] = _t.getNext();
		}
	}

	/* consume q tokens and refills the stack */
	private final void consume(int q) {
		if (q > 0) {
			System.arraycopy(_stack, q, _stack, 0, _stack.length - q);
			_p -= q;
			fill();
		}
	}

	/* fast forward to next CLOSE at terminating the current level : CLOSE or END is now the first token in the stack */
	private void consumeToClose() {
		int n = 0;
		int i;
		// check if we already have it in the stack
		for (i = 0; i < _stack.length; i++) {
			if (_stack[i]._token == TokenType.CLOSE)
				n -= 1;
			else if (_stack[i]._token == TokenType.OPEN)
				n += 1;
			if (n < 0)
				break;
		}
		if (n < 0) // yes, found
			consume(i);
		else { // no... use the fast forward call from the parser : we are gaining a lot of time here
			Token t = null;
			while (n >= 0) {
				t = _t.getNextClose();
				if (t._token != TokenType.CLOSE)
					break; //END reached without close ; probably a grammatical error
				n -= 1;
			}
			_stack[0] = t;
			_p = 1;
			fill();
		}
	}

	private int analyzeStruct() {
		int r = 0;
		//first, determine if we are really in a struct
		if (_stack[0]._token != TokenType.OPEN)
			return 0;
		if (_stack[1]._token == TokenType.TOKEN
				&& _stack[2]._token == TokenType.EQUAL)
			r = 1;
		else if (_stack[1]._token == TokenType.OPEN)
			r = 2;
		else if (_stack[1]._token == TokenType.CLOSE)
			r = 3;
		else
			return 0;
		//that's the case : analyze
		Token t = _stack[0];
		consume(1);
		if (r == 3) {
			_h.beforeEmpty(t);
			_skip = _h.afterEmpty(_stack[0]);
			consume(1);
			return 1;
		} else if (!_h.beforeStruct(t)) {
			if (r == 1)
				r = analyzeCompoundList();
			else if (r == 2)
				r = analyzeAnonList();
		} else
			consumeToClose();
		if (r < 0 || _stack[0]._token != TokenType.CLOSE)
			return -1;
		t = _stack[0];
		consume(1);
		_skip = _h.afterStruct(t);
		return 1;
	}

	private int analyzeCompoundList() {
		int r;
		do
			r = analyzeCompound();
		while (r > 0 && !_skip && _stack[0]._token!=Token.TokenType.END && _stack[0]._token!=Token.TokenType.CLOSE);
		if (_skip)
			consumeToClose();
		return (r>=0) ? 1 : -1;
	}

	private int analyzeBaseList() {
		int r;
		do {
			r = analyzeName();
			if (r == 1)
				r = analyzeData(false);
		} while (r > 0 && !_skip);
		return 1;
	}

	private int analyzeCompound() {
		int r = analyzeName();
		if (r != 1)
			return r;
		r = analyzeStruct();
		if (r != 0)
			return r;
		r = analyzeList();
		if (r != 0)
			return r;
		r = analyzeData(false);
		return (r != 1) ? -1 : 1;
	}

	private int analyzeList() {
		if (_stack[0]._token != TokenType.OPEN)
			return 0;
		int r = 0;
		Token t = _stack[0];
		consume(1);
		if (!_h.beforeList(t))
			r = analyzeDataList();
		else
			consumeToClose();
		if (r < 0 || _stack[0]._token != TokenType.CLOSE)
			return -1;
		t = _stack[0];
		consume(1);
		_skip = _h.afterList(t);
		return 1;
	}

	private int analyzeDataList() {
		int r;
		do
			r = analyzeData(true);
		while (r > 0 && !_skip);
		if (_skip)
			consumeToClose();
		return 1;
	}

	private int analyzeAnon() {
		if (_stack[0]._token != TokenType.OPEN)
			return 0;
		int r = 0;
		Token t = _stack[0];
		consume(1);
		if (!_h.beforeAnon(t))
			r = analyzeBaseList();
		else
			consumeToClose();
		if (r < 0 || _stack[0]._token != TokenType.CLOSE)
			return -1;
		t = _stack[0];
		consume(1);
		_skip = _h.afterAnon(t);
		return 1;
	}

	private int analyzeAnonList() {
		int r;
		do
			r = analyzeAnon();
		while (r > 0 && !_skip);
		if (_skip)
			consumeToClose();
		return 1;
	}

	private int analyzeData(boolean list) {
		if (_stack[0]._token == TokenType.TOKEN
				|| _stack[0]._token == TokenType.STRING) {
			_skip = (list) ? _h.afterListData(_stack[0]) : _h
					.afterBase(_stack[0]);
			consume(1);
			return 1;
		}
		return 0;
	}

	private int analyzeName() {
		if (_stack[0]._token == TokenType.TOKEN
				&& _stack[1]._token == TokenType.EQUAL) {
			_h.getName(_stack[0]);
			if (_stack[2]._token == TokenType.TOKEN
					|| _stack[2]._token == TokenType.STRING)
				_h.beforeBase(_stack[0]);
			consume(2);
			return 1;
		}
		return -1;
	}

	/**
	 *  prints the Tokens currently in the stack ; useful for dump and debug purposes
	 */
	public String tokenStack() {
		String s = "";
		for (Token t : _stack)
			s = s + " " + t.toString();
		return s;
	}

	/**
	 *  returns the current index in the input flow ; this index is
	 *  certainly ahead from the Token currently beeing analyzed.
	 */
	public int getCurrentIndex() {
		return _t.getPos();
	}

	/**
	 * Analyzes the flow of Token returned by the given Tokenizer.
	 * @return true if the analysis was correct.
	 */
	public boolean analyze() {
		_h.begin();
		if (analyzeCompoundList() < 0)
			return false;
		boolean r = _stack[0]._token == TokenType.END;
		if (r)
			_h.end(_stack[0]);
		return r;
	}

	/**
	 * Stack trace for the last error found
	 */
	static public String _error;
	/**
	 * Current position in the data when the error was found. That pointer place is 
	 * certainly somewhere not too far after the actual error location.
	 */
	static public int _errorPosition;

	/**
	 * This method just takes data and makes the grammatical analysis according to
	 * the grammar defined for Paradox files.<p>
	 * What you do with the found tokens is defined within the Hooks interface which
	 * you have to implement.<p>
	 * You can use the default implementation NoHooks, which will enable you to check
	 * the grammatical correctness of the data.<p>
	 * 
	 * In case of error, the too static fields _error and _errorPosition are set.<br>
	 * The first gives the analyzer stack trace.<br>
	 * The second indicates the position in the data when the error was found.<br>
	 * In case of success, _error is empty and _errorPosition points to one character
	 * beyond the last character in the flow.<p>
	 * 
	 * @param r  data to analyze
	 * @param h  hooks to use
	 * @return   true if the analysis is OK.
	 */
	public static boolean analyze(Reader r, Hooks h) {
		return analyze(new ReaderParser(r), h);
	};

	/**
	 * Same as {@link #analyze(Reader, ck.Analyzer.Hooks)}, except
	 * that it takes the data in a byte array rather than a reader. The parser
	 * used is much faster.
	 * 
	 * @param data array of bytes constituting the data to analyze (ASCII flow)
	 * @param h    hooks to use
	 * @return     true if the analysis is OK.
	 */
	public static boolean analyze(byte[] data, Hooks h) {
		return analyze(new FastParser(data), h);
	}
	
	
	static private  boolean analyze(Tokenizer t, Hooks h) {
		Analyzer a = new Analyzer(t, h);
		try {
			boolean b = a.analyze();
			_error = (b) ? "" : a.tokenStack();
			_errorPosition = a.getCurrentIndex();
			return b;
		} catch (Exception e) {
			_error = a.tokenStack();
			_errorPosition = a.getCurrentIndex();
			e.printStackTrace(System.out);
			return false;
		}
	}
}

