package net.sourceforge.ck2httt.pxAnalyzer;

import java.io.CharArrayReader;
import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;
import java.util.LinkedList;

/**
 * This class defines Patterns, which are a way to do two things:
 * <ul>
 *     <li>specify a field hierarchy or a list of hierarchical field</li>
 *     <li>attach callbacks to fields</li>
 * </ul>
 * It doesn't define the way the Patterns are used, but only the way to create
 * them and the way to attach callbacks to fields.<p>
 * 
 * To define the pattern of fields to analyze either a String or from a Reader
 * must be provided, that contain the descriptive text defining the patern.
 * That text must follow a strict grammar :<p>
 * <ul>
 *     <li>id            = a name with you choose ; it must not begin with a number</li>
 *     <li>name          = the name of a field which you expect to analyze later</li>
 *     <li>open          = { | {/ | {!</li>
 *     <li>close         = }</li>
 *     <li>callback      = ( id ) | ( )
 *     <li>simplelist    = open jokerlist close</li>
 *     <li>list          = namelist | namelist , list</li>
 *     <li>jokerlist     = list | list jokermatch</li>
 *     <li>jokercallback = * | * callback</li>
 *     <li>namecallback  = name | name callback</li>
 *     <li>namelist      = namematch | namematch simplelist</li>
 *     <li>all           = simplelist</li>
 * </ul>
 * Spaces (including tabulations and line break) are non significant : you can thus
 * format the hierarchy definition as you wish. This is especially convenient when the
 * pattern is stored in a file<p>
 *
 * The hierarchy created by the open/close syntax is obviously closely tied to the actual
 * field hierarchy which you expect to analyze later.<br>
 * For example, suppose that the startdate information is found under the header
 * structure, which is itself at the root level. You also notice that there is a startdate
 * field in the global data structure which is also at the root level. Now, supposing you
 * wanted to reach both fields, and not be interested by other fields, you would use a
 * basic pattern such as :<p>
 * 
 * <b>{header{startgame},globaldata{startgame}}</b><br>
 * <br>
 * which would be provided either directly in a String (appropriate choice when the filter
 * is short as in this example), or in a file (reached through a Reader).<p>
 * 
 * When opening a sub-list of fields (even at top level), it is possible to use:<br>
 * <ul>
 *    <li>{  : try to match all fields at all time</li>
 *    <li>{/ : terminate when all fields have been matched at least once until a
 *         non matching field is met</li>
 *    <li>{! : expect the fields in the given order : a field that appears out of order
 *         will cause some fields to be skipped</li>
 * </ul>
 * These modifiers are not always significant, depending on the context.<p>
 * 
 * In our previous example, we could improve performances by using the ! modifier (after
 * all, we're only reading one field in each sub-structure. So our filter would change to:
 * <p>
 * <b>{header{!startgame},globaldata{!startgame}}</b><br>
 * <br>
 * The * name is quite special ; it will always match any field. It must be put last in a
 * field hierarchy as defined in the grammar : this means that any named field will match
 * it's name and not the joker. The first interest of the joker is that you can attach a
 * general callback which will be called for all the fields that did not match another field.
 * The second use of a joker is to reach subfields even though you don't know (or care)
 * exactly in which top field you are.
 * 
 * This is still hardly useful : defining the expected hierarchy isn't going to help
 * much unless something can actually be done. You can do this by attaching callbacks to
 * each field.
 * 
 * The callback complement is somewhat more involved to put in place (at least initially,
 * because it later simplifies much of the work!) ; a callback is put in place by attaching
 * an instance of the {@link Patterns.Callbacks} interface to a field. That process is done
 * for you during the pattern analysis : As you have seen, the name of a field can optionally
 * be followed by:<p>
 * <ul>
 *     <li>(id) : if id is a number, then the callback with that index in the list of matchers
 *     is attached to that field</li>
 *     <li>(id) : if id is a name, then the first callback in the list of Matchers for which
 *     the isMatch call returns true is attached to that field</li>
 *     <li>() : as above, except that the name is automatically built for you : it is the field
 *     hierarchy form the root (for example "/header/startdate") ; be aware that the hierarchy
 *     given that of the pattern, and not the actual one ; usually, it makes no difference ; but
 *     it will when you use a joker : in that case, you get to see the joker instead of the
 *     actual field that matched. In the exemple above, if the first level had been a joker, the
 *     match function would have received "\*\startdate" as name (actually / instead of \ but
 *     this would close the javadoc comment...) ; note that this is logical since the callbacks
 *     are attached when the pattern is analyzed : at that time, there is no knowledge about
 *     actual fields.</li>
 * </ul>
 *
 *@author Copyright 2007 Yves Prélot ; distributed under the terms of the GNU General Public License
 */
public class Patterns {

	/**
	 * The interface used in the class to define our callbacks.
	 * This is the most generic form.
	 */
	public interface Callbacks {
		/**
		 * Used when compiling a pattern to determine when to hook that callback
		 * in the tree.
		 * @param  path this is either the name provided by the user int the pattern
		 *         definition (in the field(name) grammatical contruct), or the full
		 *         path to reach this field (/rootfield/field1/.../finalfield).<br>
		 *         When accessing callbacks through indexes (in the field(index)
		 *         grammatical contruct), this method will not be invoked.
		 * @return true if the callback matches the path
		 */
		public boolean isMatch(String path);
	};

	protected enum Shortcut {
		NONE,    //no shortcut
		SHORT,   //when all items have been found, skip to end
		ORDER;   //all items are expected in the given order and we skip when last has been read
	}
	
	static protected class Selector {
		static protected Selector[] __always = {};
		static protected Selector   _generic = new Selector("");
		public String     _name=null;
		public Selector[] _children=__always;
		public Shortcut   _short=Shortcut.NONE;
		public Callbacks  _matcher=null;
		
		protected int     _cur=0;       //current index when Selecting
		protected boolean _found=false; //field already found
		
		static protected Selector __SKIP = new Selector("");
		
		public Selector(String name)               { _name=name; }
		public Selector(String name, Selector[] s) { _name=name; _children=s; }
		
		protected Selector getMatch(String child) {
			if (_children==__always) return _generic;
			switch (_short) {
		    case NONE  : return getMatchBasic(child);
		    case SHORT : return getMatchShort(child);
		    case ORDER : return getMatchOrder(child);
			}
			return null;
		}
		
		//used for dynamic building
		public LinkedList<Selector> _l;
	
		public Selector addName(String n) {
			if (_l==null) _l = new LinkedList<Selector>();
			Selector s = new Selector(n);
			_l.addLast(s);
			return s;
		}
		//change the linked list to array for easy access
		public void complete() {
			if (_l!=null) {
		       _children = _l.toArray(new Selector[_l.size()]);
		       _l=null; //dispose of useless _l...
			}
		}
		public void reset() {
			if (_name.equals("*")) return;
			_cur=0;
			for (Selector s : _children) s._found=false;
		}
		public boolean isMatch(String name) {
			return _name.equals(name) || _name.equals("*");
		}
		
		//internals
		private Selector getMatchBasic(String child) {
			if (_children==null) return null;
			for (Selector s : _children) {
				if (s._name.equals(child)) return s;
			}
			if (_children[_children.length-1]._name.equals("*"))
				return _children[_children.length-1];
			return null;
		}
		private Selector getMatchShort(String child) {
			//_cur indicates here how many fields have been found
			if (_children==null) return null;
			for (Selector s : _children) {
				if (s._name.equals(child)) {
					if (!s._found) { _cur++;  s._found=true; }
					return s;
				}
			}
			return (_cur==_children.length) ? __SKIP : null;
		}
		private Selector getMatchOrder(String child) {
			//_cur indicates here from which field we are expecting
			if (_children==null) return null;
			for (int i=_cur; i<_children.length; i++) {
				Selector s = _children[i];
				if (s._name.equals(child)) {
					if (!s._found) { _cur=i;  s._found=true; }
					return s;
				}
			}
			return (_cur==_children.length) ? __SKIP : null;
		}		
	}

	/**
	 * This class is the inner parser/analyzer for making Selectors out of text.
	 * 
	 * It is possible to specify what we want to read from the data, in order to:
	 *  - reduce the amount of data analyzed (faster analysis)
	 *  - reduce the amount of data stored (smaller memory footprint)
	 * This is specified in the following way:
	 * 
	 *     <li>id         = a name with you choose ; it must not begin with either / or a number</li>
	 *     <li>name       = the name of a field which you expect to analyze later</li>
	 *     <li>open       = { | {/ | {!</li>
	 *     <li>close      = }</li>
	 *     <li>match      = ( id ) | ( )
	 *     <li>namematch  = name | name match</li>
	 *     <li>jokermatch = * | * match</li>
	 *     <li>namelist   = namematch | namematch simplelist</li>
	 *     <li>jokerlist  = jokermatch | jokermatch simplelist</li>
	 *     <li>list       = namelist | namelist , list</li>
	 *     <li>fulllist   = list | list jokermatch</li>
	 *     <li>simplelist = open fulllist close</li>
	 *     <li>all        = simplelist</li>
	 *
	 * The hierachy created is obviously closely tied to the actual field hierarchy.
	 * When sepecifying a sub-list of fields (even at top level), it is possible to use:
	 *    {  : try to match all fields at all time
	 *    {/ : terminate when all fields have been matched at least once until a
	 *         non matching field is met
	 *    {! : expect the fields in the given order : a field that appears out of order
	 *         will cause some fields to be skipped
	 * 
	 * @author Copyright 2007 Yves Prélot ; distributed under the terms of the GNU General Public License
	 */
	static protected class SelectorAnalyzer {
		
		protected final static String __OPEN         = new String("{");
		protected final static String __CLOSE        = new String("}");
		protected final static String __COMMA        = new String(",");
		protected final static String __SLASH        = new String("/");
		protected final static String __EXCLAIM      = new String("!");
		protected final static String __END          = new String();
		protected final static String __OPENBRACKET  = new String("(");
		protected final static String __CLOSEBRACKET = new String(")");
		protected final static String __JOKER        = new String("*");
	
		static protected class Parser {
			
			//for analysis
			protected int       _cur  = -1;
			protected int       _next =  0;
			protected int       _pos  = -2;
			protected Reader    _r    = null;
		    
		    //for result
			protected char[]    _b = new char[50];
			protected int       _i = -1;
		
		    Parser(Reader r) { _r=r; read(); }
		    
		    protected final boolean isTokenChar(int c)           { return Character.isLetterOrDigit(c) || c=='_' || c=='-' || c=='.' || c=='@'; }
		    protected final void passSpaces()                    { do { read(); } while (Character.isWhitespace(_cur)); }
		    protected final void passToken() throws IOException  { while (isTokenChar(_next)) read(); }
		    
		    protected void read() {
		    	_cur=_next;
	 	        if (_i>0) _b[_i++]=(char)_cur;
		    	if (_next!=-1)
		    	    try {
		    	    	_pos++;
		    	       _next=_r.read();
		    	    }
		    	    catch (IOException e) {
		    	       _next=-1;;
		    	    }
		    }    
		    protected String getOther() {
		    	if (isTokenChar(_cur))
		    	    try {
		    	    	_i=1; _b[0]=(char)_cur;
		    		    passToken();
		    		    String t = new String(_b,0,_i);
		    	    	_i=-1;
		    	    	return t;
		    	    }
		    	    catch (IOException e) {
		    	    }
		    	return null;
		    }
		    
		    protected String getNext() {
		    	passSpaces();
		    	switch (_cur) {
	    	        case '{' : return __OPEN;
	    	        case '}' : return __CLOSE;
	    	        case '(' : return __OPENBRACKET;
	    	        case ')' : return __CLOSEBRACKET;
		    	    case ',' : return __COMMA;
		    	    case '/' : return __SLASH;
		    	    case '!' : return __EXCLAIM;
		    	    case '*' : return __JOKER;
		    	    case -1  : return __END;
		    	    default  : return getOther();
		    	}
		    }
	    };
	    
	    static protected class Hooks {
	    	protected Selector    _root        = new Selector("");
	    	protected Selector    _stack[]     = new Selector[40];
	    	protected String      _path[]      = new String[40];
	    	protected Selector    _last        = _root;
	    	protected int         _l           = 0;
	    	protected Callbacks[] _matcherList = null;
	    	
	    	protected Hooks(Callbacks[] matcherList) { _stack[0]=_root; _path[0]=""; _matcherList=matcherList; }
	    	protected void beforeList(Shortcut sc, String name) {
	    		_stack[++_l]=_last;
	    		_last._short=sc;
	    		if (name==null || name.length()==0)
	    		    _path[_l]=_path[_l-1];
	    		else
	    			_path[_l]=_path[_l-1]+ "/" + name;
	    	};
	    	protected void afterList()               { _stack[_l].complete(); _path[_l]=null; _stack[_l--]=null; };
	    	protected void end()                     { _root.complete(); }
	    	protected int addName(String name, String matcher) {
				_last = _stack[_l].addName(name);
				if (matcher!=null) {
					if (matcher.length()==0) {
						String path = _path[_l] + "/" + name;
						for (Callbacks m : _matcherList) {
							if (m.isMatch(path)) {
								_last._matcher = m;
							    return 1;
							}
						}
					}
					else if (java.lang.Character.isDigit(matcher.charAt(0))) {
					    int n = new Integer(matcher);
					    if (n<0 || _matcherList==null || n>=_matcherList.length) return -1;
					    _last._matcher = _matcherList[n];
					}
					else {
						for (Callbacks m : _matcherList) {
							if (m.isMatch(matcher)) {
								_last._matcher = m;
							    return 1;
							}
						}
					}
				}
				return 1;
			};
	    }
	    
		static protected class Analyzer {
	
			protected Parser   _t;                     // analyzes the data
			protected String[] _stack = new String[3]; // stores fetched tokens (min required 1)
			protected int      _p = 0;                 // stack current size
			protected Hooks    _h;
			protected String   _lastName;
			
			public Analyzer(Reader r, Hooks h)  { _t=new Parser(r); fill(); _h=h; }
			
			/* refills the stack with tokens */
			protected final void fill() {
				while (_p<_stack.length) {
					_stack[_p++] = _t.getNext();
				}
			}
			/* consume q tokens and refills the stack */
			protected final void consume(int q) { if (q>0) { System.arraycopy(_stack, q, _stack, 0, _stack.length-q); _p-=q; fill(); } }	
	
			//   match      = ( id ) | ( )
			protected int analyzeMatch() {
	            if (_stack[0]!=__OPENBRACKET)
	            	return (_h.addName(_lastName,null)<0) ? -1 : 1;
	            consume(1);
	            String value = null;
	            if (_stack[0]!=__CLOSEBRACKET) {
	            	value = _stack[0];
	            	consume(1);
	            }
	            else
	            	value="";
	            if (_h.addName(_lastName,value)<0)  return -1;;
	            if (_stack[0]!=__CLOSEBRACKET) return -1;
	            consume(1);
	            return 1;
			}
			//   namematch  = name | name match
			protected int analyzeNameMatch() {
				if (_stack[0]==__JOKER) return 0;
				_lastName = _stack[0];
	            consume(1);
	            int r= analyzeMatch();
	            return (r==-1) ? -1 : 1;
			}
			//   jokermatch = * | * match
			protected int analyzeJokerMatch() {
				if (_stack[0]!=__JOKER) return 0;
				_lastName = "*";
	            consume(1);
	            int r= analyzeMatch();
	            return (r==-1) ? -1 : 1;
			}
			//   namelist   = namematch | namematch simplelist
			protected int analyzeNameList() {
				int r = analyzeNameMatch();
	            if (r<=0) return r;
	            if (analyzeSimpleList()<0) return -1;
	            return 1;
			}
			//   namelist   = jokermatch | jokermatch simplelist
			protected int analyzeJokerList() {
				int r = analyzeJokerMatch();
	            if (r<=0) return r;
	            if (analyzeSimpleList()<0) return -1;
	            return 1;
			}
			//   list       = namelist | namelist , list
			protected int analyzeList() {
				do {   //will stop when : either we find a namelist but no comma, or a comma followed by joker
					if (analyzeNameList()<0) return -1;
					if (_stack[0]!=__COMMA)  return  1;
	                consume(1);	
	                if (_stack[0]==__JOKER)  return 1;
				} while(true);
			}
			//   jokerlist  = list | list jokermatch
			protected int analyzeFullList() {
				if (analyzeList()<0) return -1;
				int r = analyzeJokerList();
				return (r<0) ? -1 : (r>0) ? 2 : 1 ;
			}
			//   simplelist = open jokerlist close
			protected int analyzeSimpleList() {
	            if (_stack[0]!=__OPEN) return 0;
	            consume(1);
	            Shortcut sc=Shortcut.NONE;
	            if      (_stack[0]==__SLASH)   sc=Shortcut.SHORT;
	            else if (_stack[0]==__EXCLAIM) sc=Shortcut.ORDER;
	            if (sc!=Shortcut.NONE) consume(1);
	            _h.beforeList(sc,_lastName);
	            int r = analyzeFullList();
	            _h.afterList();
	            if (r==2 && sc!=Shortcut.NONE) return -1;
	            if (r<0 || _stack[0]!=__CLOSE) return -1;
	            consume(1);
	            return 1;
			}
	
			
			public Selector analyze() throws ParseException {
				if (analyzeSimpleList()<=0) {
					String s = ""; for (String x : _stack) s = s+x;
					throw new ParseException(s,0);
				}
				if (_stack[0]!=__END) throw new IllegalStateException();
				_h.end();
				return _h._root;
			}		
		};
		
		static public Selector compile(String s, Callbacks[] matcherList) throws ParseException {
			return compile(new CharArrayReader(s.toCharArray()),matcherList);
		}
		static public Selector compile(Reader r, Callbacks[] matcherList) throws ParseException {
			Analyzer a = new Analyzer(r,new Hooks(matcherList));
			return a.analyze();
		}
			
	}

}
