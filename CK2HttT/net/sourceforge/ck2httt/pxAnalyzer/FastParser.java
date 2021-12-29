package net.sourceforge.ck2httt.pxAnalyzer;
import net.sourceforge.ck2httt.pxAnalyzer.Token.TokenType;
import net.sourceforge.ck2httt.pxAnalyzer.Token.Tokenizer;

/**
 * A Parser implementation that reads directly the data in a byte table.
 * It is fast and efficient. Note than the Tokens are fuzzier than in the
 * strict ReaderParser implementation (more precisely, it accepts a wider
 * range of characters as Token data and as space data).
 * 
 * @author Copyright 2007 Yves Prélot ; distributed under the terms of the GNU General Public License
 */
public class FastParser implements Tokenizer {

	private int    _line = 0; //current line
	private byte[] _data;     //data to analyze
	private int    _pos =-1;  //current position=last analyzed
	private int    _max;      //last accessible index
	
	//A fast but fuzzy char tester ; it is sufficient for our needs
	private boolean checkChar(byte c) {
		return c>',' && c<='z' && c!='=' || c<0;
	}
	
	//A fast but fuzzy space tester ; it is sufficient for our needs
	private boolean checkSpace(byte c) {
		if (c=='\n') { _line++; return true; }
		return (c<=' ');
	}
	
	private void passSpaces() {
		do {
    	    while (_pos<_max && checkSpace(_data[++_pos]));
		    if (_data[_pos]!='#') break;
			while (_pos<_max && _data[++_pos]!='\n');
		} while(true);
	}
		
	public FastParser(byte[] b) {
		_data = b;
		_max  = b.length-1;
	}

    private void passToClose() {
    	int i=0;
    	while (i>=0) {
        	if (_pos>=_max) return;
    		byte c = _data[++_pos];
    		if      (c=='{')  i++;
    		else if (c=='}')  i--;
    		else if (c=='\n') _line++;
    	}
    }

    private Token getString() {
		int start=_pos;
    	while(_pos<_max && _data[++_pos]!='"');
    	if (_pos<=_max) {
		    return new Token(TokenType.TOKEN,start,_pos,new String(_data,start,_pos-start+1),_line);
    	}
    	else
        	return new Token(TokenType.ERROR,start,start,"",_line);
    }

    private Token getOther() {
    	int start=_pos;
    	if (_pos==_max)
    		 return new Token(TokenType.END,_pos,_pos,null,_line);
    	while (_pos<_max && checkChar(_data[++_pos]));
    	if (_pos<=_max)
    	    return new Token(TokenType.TOKEN,start,_pos,new String(_data,start,_pos-- - start),_line);
    	else
    	    return new Token(TokenType.ERROR,start,start,"",_line);
    }

	
	public Token getNext() {
		if (_pos==_max) return new Token(TokenType.END,_pos,_pos,null,_line);
    	passSpaces();
     	switch (_data[_pos]) {
    	    case '{' : return new Token(TokenType.OPEN,_pos,_pos,null,_line);
    	    case '}' : return new Token(TokenType.CLOSE,_pos,_pos,null,_line);
    	    case '=' : return new Token(TokenType.EQUAL,_pos,_pos,null,_line);
    	    case '"' : return getString();
    	    default  : return getOther();
    	}
	}

	public Token getNextClose() {
    	passToClose();
    	return (_pos<=_max && _data[_pos]=='}') ?
    			new Token(TokenType.CLOSE,_pos,_pos,null,_line) :
    		    new Token(TokenType.END,_pos,_pos,null,_line);
	}

	public int getPos() {
		return _pos;
	}
	
}
