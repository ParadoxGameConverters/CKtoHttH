package net.sourceforge.ck2httt.pxAnalyzer;

import java.io.IOException;
import java.io.Reader;

import net.sourceforge.ck2httt.pxAnalyzer.Token.*;



/**
 * Allows the breakdown of some data into Tokens.
 * A {@link Tokenizer} implementation based on Reader.
 * This implementation is rather slow and quite strict about
 * what input it accepts.
 * 
 * @see Tokenizer
 * @author Copyright 2007 Yves Pr�lot ; distributed under the terms of the GNU General Public License
 */
public class ReaderParser  implements Tokenizer {
		
		//for analysis
	    private int       _cur  = -1;
	    private int       _next =  0;
	    private int       _pos  = -2;
	    private Reader    _r    = null;
	    private int       _line = 1;
	    
	    //for result
	    private char[]    _b = new char[500];
	    private int       _i = -1;
	
	    ReaderParser(Reader r) { _r=r; read(); }
	    
	    private boolean isTokenChar(int c) { return Character.isLetterOrDigit(c) || c=='_' || c=='-' || c=='.' || (c<=-1 && c>=-64); }
	    private void passSpaces()
	    {
	    	do {
	    	    do { read(); if (_cur=='\n') _line++; } while (Character.isWhitespace(_cur));
	    	    if (_cur=='#') do { read(); } while (_next!='\n');
	    	    else break;
	    	}
	    	while (true);
	    }
	    private void passToken() throws IOException        { while (isTokenChar(_next)) read(); }
	    private void passToChar(int c) throws IOException  { do read(); while (c!=_cur); }
	    
	    /* 
	     * This is a call to advance fast in the data to get to the next appropriate closing }
	     * we will want to use it to skip data structures which we know we won't be using:
	     * why should we delve into them ?
	     */ 
	    private void passToClose() {
	    	int i=0;
	    	while (i>=0) {
	    		_cur=_next;
	    		if (_cur=='{') i++;
	    		if (_cur=='}') i--;
	    		if (_cur=='\n') _line++;
	    	    try {
	    	    	_pos++;
	    	       _next=_r.read();
	    	    }
	    	    catch (IOException e) {
	    	       _next=-1;
	    	       break;
	    	    }	    		
	    	}
	    }
	    private void read() {
	    	_cur=_next;
 	        if (_i>0) _b[_i++]=(char)_cur;
	    	if (_next!=-1)
	    	    try {
	    	       _pos++;
	    	       _next=_r.read();
	    	    }
	    	    catch (IOException e) {
	    	       _next=-1;
	    	    }
	    }
	    
	    private Token getString() {
    		int start=_pos; _i=1; _b[0]=(char)_cur;
	    	try {
	    		passToChar('"');
    		    Token t = new Token(TokenType.TOKEN,start,_pos,new String(_b,0,_i),_line);
    		    //Token t = new Token(TokenType.TOKEN,start,_pos,null);
	    		_i=-1;
	    		return t;
	    	}
	    	catch (IOException e) {
	    		return new Token(TokenType.ERROR,start,start,"",_line);
	    	}
	    }
	    
	    private Token getOther() {
	    	if (isTokenChar(_cur))
	    	    try {
	    	    	int start=_pos; _i=1; _b[0]=(char)_cur;
	    		    passToken();
	    		    Token t = new Token(TokenType.TOKEN,start,_pos,new String(_b,0,_i),_line);
	    		    //Token t = new Token(TokenType.TOKEN,start,_pos,null);
	    	    	_i=-1;
	    	    	return t;
	    	    }
	    	    catch (IOException e) {
	    	    }
	    	return new Token(TokenType.ERROR,_pos,_pos,"",_line);
	    }
	    
	    public Token getNext() {
	    	passSpaces();
	    	switch (_cur) {
	    	    case '{' : return new Token(TokenType.OPEN,_pos,_pos,null,_line);
	    	    case '}' : return new Token(TokenType.CLOSE,_pos,_pos,null,_line);
	    	    case '=' : return new Token(TokenType.EQUAL,_pos,_pos,null,_line);
	    	    case -1  : return new Token(TokenType.END,_pos,_pos,null,_line);
	    	    case '"' : return getString();
	    	    default  : return getOther();
	    	}
	    }
	    public Token getNextClose() {
	    	passToClose();
	    	return (_cur=='}') ? new Token(TokenType.CLOSE,_pos,_pos,null,_line) :
	    		                 new Token(TokenType.END,_pos,_pos,null,_line);
	    }
		public int getPos() {
			return _pos;
		}
	
}
