package net.sourceforge.ck2httt.pxAnalyzer;

import java.util.LinkedList;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.TreeSet;
import java.util.SortedSet;
import java.io.IOException;
import java.io.Writer;
import java.io.Reader;
import java.text.ParseException;

import net.sourceforge.ck2httt.pxAnalyzer.Analyzer.*;
import net.sourceforge.ck2httt.pxAnalyzer.Patterns.*;
import net.sourceforge.ck2httt.pxAnalyzer.Token.*;


/**
 *   This class defines the very important {@link Field} class to contain and organize the information
 *   we can read from any PX save file.<p>
 *   <p>
 *   These classes allow the user easy access to any analyzed data by name, and it
 *   gives the possibility to edit, delete or add fields. Note that while this
 *   possibility is given, there is no warranty that the result you'll get will
 *   be consistent : editing a file must be done carefully.<p>
 *   <p>
 *   It then proposes a Hooks implementation {@link Analyzer} that does the job of
 *   filling these fields. That Hooks implementation is fairly harmless as it will never
 *   skip any Token on it's own. However, doing so will result in loading the whole data
 *   into memory, which is seldom what we intend to do. Instead, it should be coupled
 *   with another Hooks scheme (such as the one provided by {@link PXChooser}) in order 
 *   to filter out what is really required. The {@link PXTreeChooser} class can be
 *   usefully consulted as it merges all these classes into a neat-and-easy-to-use class<p>
 *   
 *   The only usable things from this class are:<p>
 *   - class {@link TreeHooks} (especially method {@link TreeHooks#getDataTree()})<p>
 *   - class {@link Field} and derived classes, as generic containers for analyzed data.<p>
 *   
 * @author Copyright 2007 Yves Prélot ; distributed under the terms of the GNU General Public License
 * @see Analyzer
 */
public class PXTree {
	
	/**
	 * This class helps us manage the writing of the fields to the output
	 * while losing no information that was not analyzed. For this, we have
	 * to read the input file as we write to the output.
	 * 
	 * @author yvesp
	 */
	static private class ReaderWriter {
		/**
		 * The reader where we get the original data
		 */
		private Reader _in;
		/**
		 * The output for the new data
		 */
		private Writer _out;
		/**
		 * The current index in the _in Reader : all has been read up to and including this index
		 */
		private int _cur=-1;
		/**
		 * The index up to which we are to copy to, itself included
		 */
		private int _pos=-1;
		/**
		 * An internal buffer
		 */
		final static char[] _b = new char[4096];
		/**
		 * Constructor
		 * @param in    data from which to read
		 * @param out   stream where we write
		 */
		ReaderWriter(Reader in, Writer out) {
			_in  = in;
			_out = out;
		}
		/**
		 * Reads the input from it's current position up to an including the
		 * new position ; the data read is written unchanged to the output.
		 * @throws IOException
		 */
		void write() throws IOException {
			int lg = _pos-_cur; //number of bytes to read
			if (lg<=0) return;
			while (_b.length<=lg) {
				_in.read(_b);
				_out.write(_b);
				lg-=_b.length;
			}
			_in.read(_b, 0, lg);
			_out.write(_b, 0, lg);
			_cur=_pos;
		}
		/**
		 * Skips characters from the input until the new position is reached.
		 * This operation will force the writing of all pending data.
		 * @param pos the position up to (and included) which we read
		 * @throws IOException
		 */
		void skipTo(int pos)  throws IOException {
			if (pos<_cur) throw new IllegalStateException();
			if (pos==_cur) return;
			write();
			_in.skip(pos-_cur);
			_cur=pos;
		}
		/**
		 * Indicates that characters from the input must be written until
		 * the new position is reached. Nothing is actually done : writing
		 * will only happen when a write() is forced.
		 * @param pos the position up to which we want to write
		 * @throws IOException
		 */
		void write(int pos) {
			_pos=pos;
		}
		/**
		 * Writes a String to the output ; because that string is out of
		 * the reading flow, we have to synchronize the write operation.
		 * @param s
		 */
		void write(String s) throws IOException {
			write();
			_out.write(s);
		}
		/**
		 * Terminates writing all missing data ; this includes any data that
		 * may not have been analyzed (in the process of being skipped).
		 */
		void flush(StructField f) throws IOException {
			write();       //force writing
			write(f.endIndex());
			write();       //force writing again
			_out.flush();  //write on disk
		}
		
		/**
		 * This class defines the operations we require to operate the
		 * ReaderWriter class ; it should be an interface, but as we don't want
		 * most of the methods to be seen, we've made it an abstract class. As
		 * happens, we can still inherit from it.
		 */
		abstract static class ReaderWriterIntf {
			/**@return true if the data is new and out of the input flow*/
			abstract boolean isNew();
			/**@return true if the data has been deleted*/
			abstract boolean isDeleted();
			/**@return true if the data has been modified*/
			abstract boolean isModified();
			/**
			 * returns the index for the first character in the input flow, or -1 if it was not in that flow.
			 * @return the index for the first character
			 */
			abstract int     begIndex();
			/**
			 * returns the index for the mid character in the input flow (would be the index for the first
			 * character of the contained data for a structure/list or anonymous, otherwise same as begIndex)
			 * @return the index for the middle character
			 */
			abstract int     midIndex();
			/**
			 * returns the index for the last character in the input flow, or -1 if it was not in that flow.
			 * @return the index for the last character
			 */
			abstract int     endIndex();
			/**method to write the whole data when it is new*/
			abstract void    write(ReaderWriter rw)     throws IOException;
			/**method to write the modified part of the data (from midIndex() when the data has been modified*/
			abstract void    writeData(ReaderWriter rw) throws IOException;
		};
		
		/**
		 * Method for writing something using the ReaderWriterIntf interface
		 */
		<T extends ReaderWriterIntf> int write(T[] l, int i) throws IOException {
			T f = l[i];
			//don't bother with a field that is new but deleted
			if (f.isNew() && f.isDeleted()) return 1;
			//for a new field, we have first to copy the reader up to the next
			//real (that was read from the data) element in the same context,
			//whether it has been deleted or not. If none is found, we can output
			//the field directly. We write all consecutive new fields in one pass.
			if (f.isNew()) {
				int j;
				for (j=i+1; j<l.length && l[j].isNew(); j++);
				if (j!=l.length) write(l[j].begIndex()-1);
				for (int k=i; k<j; k++) l[k].write(this);
				return j-i;
			}
			//for a deleted field, things are simpler : we write up to it's
			//beginning, then skip over all consecutive deleted fields.
			else if (f.isDeleted()) {
				write(f.begIndex()-1);
				int j;
				int end=f.endIndex();
				for (j=i+1; j<l.length && l[j].isDeleted(); j++)
					if (!l[j].isNew()) end=l[j].endIndex();
				skipTo(end);
				return j-i;
			}
			//for a modified field, things are simple too : we write up to it's
			//middle, then we output it's data.
			else if (f.isModified()) {
				write(f.midIndex()-1);
				f.writeData(this);
				return 1;
			}
			else {
				write(f.endIndex());
				return 1;
			}
		}
		<T extends ReaderWriterIntf> void write(T[] l)  throws IOException {
			for (int i=0; i<l.length; i+=write(l,i));
		}
	}

	/**
	 * This class contains the minimal information we need about a chunk of data
	 * that has been analyzed.
	 */
	public static class BaseData extends ReaderWriter.ReaderWriterIntf {
		/** index of the first character as read in the input */
		protected int _beg;
		/** index of the last character as read in the input */
		protected int _end;
		/** String value for that data */
		final public String _value;
		/** Constructor from a Token ; used by the analyzer */
		/**
		 * Dictionary containing all the names that have been found.
		 * While slowing down a little the analysis time, it decreases the
		 * amount of used memory as each name is now referenced only once.
		 */
		static private TreeSet<String> _nameDictionary = new TreeSet<String>();

		protected BaseData(Token t, boolean dictionary) {
			_beg=t._beg;
			_end=t._end;
			_value= (dictionary) ? putInDictionary(t._value) : t._value;
		}
		/**
		 * Basic Constructor
		 * @param beg index of the first character in the input flow
		 * @param end index of the last character in the input flow
		 * @param v value of the data
		 */
		public BaseData(int beg, int end, String v, boolean dictionary) {
			_beg=beg;
			_end=end;
			_value= (dictionary) ? putInDictionary(v) : v;
		}
		public boolean isNew()      { return _end==-1; };
		public boolean isDeleted()  { return _end!=-1 && _beg<0; };
		public boolean isModified() { return false; };
		protected int  begIndex()   { return (_end==-1) ? -1 : (_beg<0) ? -_beg-1 : _beg; };
		protected int  midIndex()   { return begIndex(); };
		protected int  endIndex()   { return _end; };
		protected void write(ReaderWriter rw) throws IOException { rw.write(" "+_value+" "); }
		protected void writeData(ReaderWriter rw) throws IOException { }

		String putInDictionary(String name) {
	    	SortedSet<String> s = _nameDictionary.subSet(name,name+"\0");
	    	if (s.isEmpty()) {
	    		_nameDictionary.add(name);
	    		return name;
	    	}
	    	return s.first();
	    }
	}
	
	/**
	 * A list of callbacks to keep track of the type of the fields, when these are
	 * given as when the template is output from a collapsed file.
	 * By themselves, the callbacks have no effect. But if they where absent, the analysis
	 * would crash and if we managed not to crash, we would lose that important information.
	 */
	private static class TypeMarks implements Callbacks {
		String _myName=null;
		TypeMarks(String myName)            { _myName=myName; }
		public boolean isMatch(String path) { return path.equals(_myName) || _myName.equals("*"); }
	}
	static private TypeMarks[] __templateFieldTypes = {
		new TypeMarks("B"),
		new TypeMarks("S"),
		new TypeMarks("A"),
		new TypeMarks("L"),
		new TypeMarks("E"),
		new TypeMarks("*"),
	};
	
	/**
	 * This class is used to control the insertion of new fields inside a
	 * structure. It serves two purposes:<p>
	 * <ul>
	 * <li>Reject the insertion of non declared fields</li>
	 * <li>Help place the new field at it's appropriate place in the structure.</li>
	 * </ul>
	 * A field that is not declared in the template is deemed non existing.<br>
	 * The order of placement is the order in which the fields are declared in
	 * the template.<p>
	 * Exemple of (small) template: "{globaldata{start_date{day,month,year}}}"
	 */
	static public class Template {
		/**Loaded template if any*/
		private Selector _s;
	    /**
	     * Constructor. Loads a field template for the current field hierarchy.
	     * @param r Reader describing the template
	     * @throws ParseException
	     */
		public Template(Reader r) {
			try {
			    _s = Patterns.SelectorAnalyzer.compile(r,__templateFieldTypes);
			}
			catch (ParseException e) {
				e.printStackTrace(System.out);
			}
		};
	    /**
	     * Constructor. Loads a field template for the current field hierarchy.
	     * @param s string containing the template
	     * @throws ParseException
	     */
		public Template(String s) {
			try {
			    _s = Patterns.SelectorAnalyzer.compile(s,__templateFieldTypes);
			}
			catch (ParseException e) {
				e.printStackTrace(System.out);
			}
		};
	    /**
	     * @return a String in the form /field1/field2/.../field describing the path
	     * to reach this field.
	     */
	    Selector getSelector(Field<?> f) {
	    	if (_s==null) return null;
	    	if (f._owner==null)     return _s;
	    	String[] s = new String[40];
	    	int n=0;
	    	Field<?> cur=f;
	    	do {
	    		s[n++] = cur._name._value;
	    		cur = cur._owner;
	    	}
	    	while (cur!=null && cur._name._value.length()!=0);
	    	Selector r=_s;
	    	while (n>0 && r!=null) {
	    		r = r.getMatch(s[--n]);
	    	}
	    	return r;
	    }
	};
	
		
	/**
	 * This class defines a generic field as analyzed from the data. It is generic, so as
	 * to specialize depending on the exact sub-type (structure, base field, list,
	 * anonymous field).<p>
	 * Because we know we may have to change the data or add new data, we use the
	 * following convention in order not to clutter the structure with additional,
	 * unnecessary fields (we are memory-aware):<p>
	 * <ul>
	 * <li>The _name BaseData is used to track the status of a field</li>
	 * <li>If _end is -1, then the field has been created from scratch (not read in
	 * the data)</li>
	 * <li>If _beg<0, the the field has been deleted ; more precisely, in order not
	 * to lose any valuable information, a deleted field has it's _beg field set
	 * to -_beg-1 ; the operation can be reversed at to loss.</li>
	 * </ul>
	 * This is noted for information as the user should not have to delve into this
	 * information. <p>
	 * Even though the _name, _owner and _data fields are public so that you can
	 * easily read them, you should most probably refrain from changing them without
	 * resorting to any of the provided methods.<p>
	 * @param <T>     the subclass associated with the field
	 * @author yvesp
	 */
	static abstract public class Field<T> extends ReaderWriter.ReaderWriterIntf {
		/**
		 * Expected layout for the fields.
		 */
		static public enum Layout { /** fields on one line */FLAT, /** one field per line */INDENT };
		/**Name of the field */
		public BaseData  _name;
		/** Field that owns the field ; allows easy navigation in the field tree. */
		public Field<?> _owner=null;
		/** Data specific for the field. */
		public T _data=null;
		/** Using dictionary ? */
		static public boolean _useDictionary = true;
				
		/**
		 * Marks a field for deletion ; the field is not actually deleted so the operation
		 * can be reversed. This is even true for a new field created from scratch.
		 */
		public final void delete()       { if (_name._beg>=0) _name._beg=-_name._beg-1; }
		/**
		 * Restores a deleted field.
		 */
		public final void undelete()     { if (_name._beg<0)  _name._beg=-_name._beg-1; }
		/**
		 * @return true if the field has been marked for deletion
		 */
		public final boolean isDeleted() { return _name._beg<0; }
		/**
		 * @return true if the field is a new field created from scratch
		 */
		public final boolean isNew()     { return _name._end==-1; }
		/**
		 * Checks if the field has been created from scratch or if it's content has been changed.
		 * At this generic level, it only checks if deleted or new.
		 * @return true if any of these conditions apply.
		 */
		public boolean isModified()      { return isNew() ^ isDeleted(); }

		/**
		 * Returns all the fields with that name from the current field.<br>
		 * This method is not always appropriate, as all fields don't always have sub-fields.
		 * @param name name of the fields to recover.
		 * @param deleted true if deleted fields are recovered too.
		 * @return The array of found fields. null if nothing.
		 */
		abstract public Field<?>[] getAll(String name, boolean deleted);
		/**
		 * Returns the first field with that name from the current field.<br>
		 * This method is not always appropriate, as all fields don't always have sub-fields.
		 * @param name name of the field to recover.
		 * @param deleted true if deleted fields are considered too.
		 * @return The found field. null if nothing.
		 */
		abstract public Field<?>   get   (String name, boolean deleted);
		/**
		 * Returns the name of the field.
		 * @return The name of the field.
		 */
		final public String name() { return _name._value; };
		/**
		 * Returns all the non deleted fields with that name from the current field.<br>
		 * This method is not always appropriate, as all fields don't always have sub-fields.
		 * @param name name of the fields to recover.
		 * @return The array of found fields. null if nothing.
		 */
		final public Field<?>[] getAll(String name) { return getAll(name,false); };
		/**
		 * Returns the first non deleted field with that name from the current field.<br>
		 * This method is not always appropriate, as all fields don't always have sub-fields.
		 * @param name name of the field to recover.
		 * @return The found field. null if nothing.
		 */
		final public Field<?>   get(String name)    { return get(name,false); };

		protected final int begIndex() { return (_name._end==-1) ? -1 : (_name._beg<0) ? -_name._beg-1 : _name._beg; }

		/**
		 * Writes the field to the output.<p>
		 * The required input and output are build in the ReaderWriter parameter. 
		 * 
		 * @param  rw the ReaderWriter required for the operation
		 * @throws IOException
		 */
		abstract protected void write(ReaderWriter rw) throws IOException;
		abstract protected void writeData(ReaderWriter rw) throws IOException;
		
		public Field(Token name) { _name  = new BaseData(name,_useDictionary); }
		/** sets the owner of that field. */
		protected final void setOwner(Field<?> owner) { _owner=owner; }
		/** make life easier for the garbage collector by disrupting all these cross references. */
		public void clean() { _owner=null; _data=null; _name=null; }
		
		/**
		 * Writes the field to the output.<p>
		 * Unlike the write using the ReaderWriter, only the fields in the hierarchy are output. 
		 * 
		 * @param  out the ReaderWriter required for the operation
		 * @param  debug deleted/new fields are specially marked
		 * @throws IOException
		 */
		abstract protected void write(Writer out, boolean debug) throws IOException;
		
		/**@return the current layout for that field */
	    protected     Layout  getLayout()  { return Layout.FLAT; }
	    private final boolean isIndented() { return _owner.getLayout()!=Layout.FLAT; }
	    protected final int     indentLvl()  {
	    	int l=0;
	    	for (Field<?> x=this; x._owner!=null; x=x._owner) { if (x.isIndented()) l++; }
	    	return l-1;
	    }
	    private final void doIndent(ReaderWriter rw, int l) throws IOException {
	    	char[] spaces = new char[1+4*l];
	    	for (int i=0; i<spaces.length; i++) spaces[i]=' ';
	    	spaces[0]='\n';
	    	rw.write(new String(spaces));
	    }
	    protected final void doIndent(ReaderWriter rw) throws IOException {
	    	if (!isIndented()) return;
	    	doIndent(rw,indentLvl());
	    }
	    protected final void doIndentEnd(ReaderWriter rw) throws IOException {
	    	if (getLayout()==Layout.FLAT) return;
	    	doIndent(rw,indentLvl()+1);
	    }
	    private final void doIndent(Writer out, int l) throws IOException {
	    	char[] spaces = new char[1+4*l];
	    	for (int i=0; i<spaces.length; i++) spaces[i]=' ';
	    	spaces[0]='\n';
	    	out.write(new String(spaces));
	    }
	    protected final void doIndent(Writer out) throws IOException {
	    	if (!isIndented()) return;
	    	doIndent(out,indentLvl());
	    }
	    protected final void doIndentEnd(Writer out) throws IOException {
	    	if (getLayout()==Layout.FLAT) return;
	    	doIndent(out,indentLvl()+1);
	    }
	    /**
	     * @return a String in the form /field1/field2/.../field describing the path
	     * to reach this field.
	     */
	    public String makePath() {
	    	String r="";
	    	Field<?> cur=this;
	    	do {
	    		r = "/" + cur._name._value +r;
	    		cur = cur._owner;
	    	}
	    	while (cur!=null && cur._name._value.length()!=0);
	    	return r;
	    }
	    
};
	
	
	/**
	 * Field that contain basic data<p>
	 * 
	 * @author yvesp
	 */
	static public class BaseField extends Field<BaseData> {
		String _newVal=null;
		/**
		 * Constructor used by the analyzer.
		 * @param name
		 * @param data
		 */
		public BaseField(Token name, Token data)   { super(name); _data=new BaseData(data,false); }
		/**
		 * Constructor for creating a field from scratch.<p>
		 * Useful for adding new fields in an existing tree.<p>
		 * @param name
		 * @param data
		 */
		public BaseField(String name, String data) { super(new Token(null,0,-1,name,0)); _newVal=data; }
		/**
		 * Constructor for creating a field from scratch.<p>
		 * Useful for adding new fields in an existing tree.<p>
		 * @param name
		 * @param data
		 */
		public BaseField(String name, int data) { this(name, (new Integer(data)).toString()); }
		/**
		 * Constructor for creating a field from scratch.<p>
		 * Useful for adding new fields in an existing tree.<p>
		 * @param name
		 * @param data
		 */
		public BaseField(String name, float data) { this(name, (new Float(data)).toString()); }
		/**
		 * Gets the field value
		 * @return the value of the field ; if the field has been changed, that value is given ; if the
		 *         field has been deleted, the method returns null;
		 */
		public String get() { return (_newVal==null) ? _data._value : _newVal; }
		/**
		 * Gets the field value unquoted if it was quoted
		 * @return the unquoted value of the field ; if the field has been changed, that value is given ;
		 *         if the field has been deleted, the method returns null;
		 */
		public String getUnquoted() {
			String s = (_newVal==null) ? _data._value : _newVal;
			if (s.charAt(0)=='"') return s.substring(1, s.length()-1);
			else return s;
		}
		/**
		 * Gets the field value as an integer
		 * @return the value of the field ; if the field has been changed, that value is given ; if the
		 *         field has been deleted, the method returns null;
		 */
		public int getAsInt() {
			return (new Integer(get()).intValue());
		}
		/**
		 * Gets the field value as an float
		 * @return the value of the field ; if the field has been changed, that value is given ; if the
		 *         field has been deleted, the method returns null;
		 */
		public float getAsFloat() {
			return (new Float(get()).floatValue());
		}
		/**
		 * Gets the field value as an double
		 * @return the value of the field ; if the field has been changed, that value is given ; if the
		 *         field has been deleted, the method returns null;
		 */
		public double getAsDouble() {
			return (new Double(get()).doubleValue());
		}
		/**
		 * Gets the field value as an boolean
		 * @param trueValue valeur textuelle associée à la valeur vraie
		 * @return the value of the field ; if the field has been changed, that value is given ; if the
		 *         field has been deleted, the method returns null;
		 */
		public boolean getAsBoolean(String trueValue) {
			return get().equals(trueValue);
		}
		/**
		 * Changes the field value
		 * @param val the new value for the field
		 */
		public void set(String val) { _newVal=val; }
		protected void write(ReaderWriter rw) throws IOException {
			doIndent(rw);
			rw.write(_name._value+" = "+_newVal+" ");
		}
		protected void writeData(ReaderWriter rw) throws IOException {
			rw.write(_newVal+" ");
			if (!isNew()) rw.skipTo(_data._end);
		}
		public boolean isModified() { return super.isModified() || _newVal!=null && _name._beg>=0; }
		protected void write(Writer out, boolean debug) throws IOException {
			if (!debug && isDeleted()) return;
		    doIndent(out);
		    if (!debug || (!isNew() && !isDeleted())) out.write(_name._value+"="+get()+" ");
		    else if (isNew() && isDeleted())          out.write(_name._value+"[ND]="+get()+" ");
		    else if (isNew())                         out.write(_name._value+"[N]="+get()+" ");
		    else if (isDeleted())                     out.write(_name._value+"[D]="+get()+" ");
		}
		public Field<?>[] getAll(String name, boolean deleted) { return null; };
		public Field<?>   get(String name, boolean deleted)    { return null; };
		protected int midIndex()     { return (isNew())?-1:_data._beg; };
		public int endIndex()     { return (isNew())?-1:_data._end; };
	};
	
	/**
	 * Structure with no name ; when this happens, it only contains BaseField.<p>
	 * 
	 * @author yvesp
	 */
	static public class AnonField extends Field<LinkedList<BaseField>> {
		/**
		 * Position in the input for the opening {
		 */
		protected int _beg=0;		
		/**
		 * Position in the input for the closing }
		 */
		protected int _end=0;
		/**
		 * Layout for the fields
		 */
		public Layout _layout=Layout.FLAT;
		/**
		 * Constructor used by the analyzer.<p>
		 * It has a special token as _name : Token(null,0,0,null)
		 * @param name of the field
		 */
		public AnonField(Token name) { super(name); _data=new LinkedList<BaseField>(); }
		/**
		 * Constructor for creating an anonymous field from scratch.<p>
		 * Useful for adding new fields in an existing tree. It has _name  as Token(null,0,0,"").<p>
		 */
		public AnonField() { super(new Token(null,0,0,"",0)); _data=new LinkedList<BaseField>(); }
		/**
		 * Adds a field in the field list. Used by the analyzer.<p>
		 * @param f to add
		 */
		public void addField(BaseField f) {
			if (f._data==null && !f.isNew()) return;
			f.setOwner(this);
			_data.addLast(f);
		}
		/**
		 * Adds a field in the field list a the specified position.<p>
		 * @param f to add
		 * @param i index of the position the new item is to occupy ; -1 means last.
		 */
		public void addFieldAt(int i, BaseField f) {
			f.setOwner(this);
			if (i==-1) _data.addLast(f);
			else       _data.add(i, f);
		}
		/**
	     * Adds the field in the position it should occupy as defined by the template.
	     * If some existing fields are not declared in the template, you may get unexpected
	     * results. The call doesn't check the template validity against the existing fields.
	     * The fields is place at the first legal position.
		 * @param f field to insert
		 */
		public void insertFieldWithTemplate(BaseField f, Template t) {
			addFieldAt(getFirstIndex(f._name._value,t),f);
		}
		/**
		 * Finds the first possible index position for a field of that name
		 * in the current field.
		 * @param name name of the field to add
		 * @return position found
		 * @throws IllegalStateException if there is any problem
		 */
		public int getFirstIndex(String name, Template t) throws IllegalStateException {
			if (t==null) throw new IllegalArgumentException("no template");
			//get container and check we know it
			Selector s = t.getSelector(this);
			if (s==null) throw new IllegalStateException("containing field"+_name._value+"not in template");
			Selector expected[] = s._children;
			//check if the field already exists in the data : if so, first place is the existing one
			Iterator<BaseField> x = _data.iterator();
			int k=0; while (x.hasNext()) { if (x.next()._name._value.equals(name)) break; k++; }
			if (k!=_data.size()) return k; //found!
			//check that we know the field in the template and find it's place
			int n; for (n=0; n<expected.length && !expected[n]._name.equals(name); n++);
			if (n==expected.length) throw new IllegalStateException("field not found in template");
			//we must find the existing field closest to n in the template
			k=0;
			for (BaseField f : _data) {
				int i;
                for (i=0; i<n && !f._name.equals(expected[i]._name); i++);
                if (i==n) return k; //that's the first field taht's not found : good : take it's position!
                k++;
            }
			//not found ???? put it last
			return _data.size();
		}
		/**
		 * Finds the last possible index position for a field of that name
		 * in the current field.
		 * @param name name of the field to add
		 * @return position found
		 * @throws IllegalStateException if there is any problem
		 */
		public int getLastIndex(String name, Template t) throws IllegalStateException {
			int k = getFirstIndex(name,t);
			if (k==_data.size()) return k;
			ListIterator<BaseField> x = _data.listIterator(k);
			if (x.hasNext()) {
				BaseField f = x.next();
				if (f._name._value.equals(name))
					do k++; while (x.hasNext() && x.next()._name.equals(name));
			}
			return k;
		}
		/**
		 * Adds a field in the field list. Used by the analyzer.<p>
		 * @param name name of the subfield
		 * @param data value of the subfield, as read from the data
		 * @deprecated
		 */
		public void addField(Token name, Token data) {
			if (data==null) return;
			BaseField f = new BaseField(name,data);
			f.setOwner(this);
			_data.addLast(f);
		}
		/**
		 * Adds a field in the field list<p>
		 * @param name name of the subfield. Useful for creating a new field from scratch.
		 * @param data value of the subfield.
		 */
		public void addField(String name, String data) {
			BaseField f = new BaseField(name,data);
			f.setOwner(this);
			_data.addLast(f);
		}
		/**
		 * Finds all the fields with a given name.
		 * @param name
		 * @param deleted true if you want to recover the deleted fields too
		 * @return the fields with that name
		 */
		public BaseField[] getAll(String name, boolean deleted) {
			BaseField[] f = new BaseField[_data.size()];
			int n = 0;
			for (BaseField x : _data)
				if ((deleted || !x.isDeleted()) && x._name._value.equals(name))
					f[n++] = x;
			BaseField[] g = new BaseField[n];
			System.arraycopy(f, 0, g, 0, n);
			return g;
		}
		public BaseField get(String name, boolean deleted) {
			Iterator<BaseField> i = _data.iterator();
			while (i.hasNext()) {
				BaseField f = i.next();
				if ((deleted || !f.isDeleted()) && f._name._value.equals(name))
					return f;
			}
			return null;
		}
		public boolean isModified() {
			if (_name._value!=null && !isDeleted()) return true;
			Iterator<BaseField> i = _data.iterator();
			while (i.hasNext()) {
				if (i.next().isModified()) return true;
			}
			return false;
		}
		protected void write(ReaderWriter rw) throws IOException {
			doIndent(rw);
			rw.write(" {");
			writeData(rw);
			doIndentEnd(rw);
			rw.write("}");
		}
		protected void writeData(ReaderWriter rw) throws IOException {
			rw.write(_data.toArray(new BaseField[_data.size()]));
		}
		protected void write(Writer out, boolean debug) throws IOException {
			if (!debug && (isDeleted())) return;
			doIndent(out);
			if (!debug || (!isNew() && !isDeleted()))  out.write(" {");
			else if (isNew() && isDeleted())           out.write("[ND] {");
			else if (isNew())                          out.write("[N] {");
			else if (isDeleted())                      out.write("[D] {");
			for (BaseField f : _data) f.write(out,debug);
			doIndentEnd(out);
			out.write("}");
		}
		protected int midIndex() { return (isNew())?-1:_beg; };
		public int endIndex() { return (isNew())?-1:_end; };
	    protected Layout getLayout()  { return _layout; }
	    public void clean() { for (BaseField f : _data) f.clean(); super.clean(); }
	};
	/**
	 * List Field ; when this happens, it only contains BaseData.<p>
	 * 
	 * @author yvesp
	 */
	static public class ListField extends Field<LinkedList<BaseData>> {
		/**
		 * Position in the input for the opening {
		 */
		protected int _beg=0;		
		/**
		 * Position in the input for the closing }
		 */
		protected int _end=0;
		/**
		 * Constructor used by the analyzer.
		 * @param name of the field
		 */
		public ListField(Token name) { super(name); _data=new LinkedList<BaseData>(); }
		/**
		 * Constructor for creating a list field from scratch.<p>
		 * Useful for adding new fields in an existing tree.<p>
		 * @param name of the field
		 */
		public ListField(String name) { super(new Token(null,0,-1,name,0)); _data=new LinkedList<BaseData>(); }
		/**
		 * Removes the first non deleted Token with the given value.<p>
		 * Removal doesn't actually occur : the Token is only marked as deleted
		 * by setting it's _beg field<p>
		 * @param value
		 */
		public void delToken(String value) {
			for (BaseData t : _data) {
				if (t._value.equals(value) && t._beg>=0) {
					t._beg = -t._beg-1;
					break;
				}
			}
		}
		public Field<?>[] getAll(String name, boolean deleted) { return null; };
		public Field<?>   get(String name, boolean deleted)    { return null; };
		/**
		 * Gets all the values associated with the list.
		 * 
		 * @param deleted true if we want to get the deleted fields too.
		 * @return list of non deleted values.
		 */
		public String[] get(boolean deleted) {
			int n=0;
			String[] s = new String[_data.size()];
			for (BaseData t : _data) if (deleted || t._beg>=0) s[n++] = t._value;
			String[] t = new String[n];
			System.arraycopy(s, 0, t, 0, n);
			return t;
		}
		/**
		 * Adds a new Token to the list ; used by the Analyzer.
		 * @param f
		 */
		public void addToken(Token f) {
			if (f==null) return;
			_data.addLast(new BaseData(f,_useDictionary));
		}
		/**
		 * Inserts a new Token into the list, after the Token with the given value.
		 * If that value is null, inserts at the beginning. If that value doesn't exist,
		 * inserts at the end. The inserted Token is recognized by a token type of null.
		 * @param value value of the element you want to insert in the list
		 * @param after value of the element after which you want to insert the new element
		 */
		public void addAfter(String value, String after) {
			if (value==null || value.length()==0)
				throw new IllegalArgumentException();
			BaseData t = new BaseData(0,-1,value,false);
			if (after==null)        { _data.addFirst(t); return; }
			if (after.length()==0)  { _data.addLast(t); return; }
			ListIterator<BaseData> i = _data.listIterator();
			while (i.hasNext()) {
				BaseData x = i.next();
				if (x._beg>=0 && x._value.equals(after)) {
					i.add(t);
					break;
				}
			}
			
		}
		protected void write(ReaderWriter rw) throws IOException {
			rw.write(_name._value+" = {");
			writeData(rw);
			rw.write("}");
		}
		protected void writeData(ReaderWriter rw) throws IOException {
			rw.write(_data.toArray(new BaseData[_data.size()]));
		}
		protected void write(Writer out, boolean debug) throws IOException {
			if (!debug && isDeleted()) return;
			doIndent(out);
			if (!debug || !(isNew() && isDeleted()))   out.write(_name._value+" = {");
			else if (isNew() && isDeleted())           out.write(_name._value+"[ND] = {");
			else if (isNew())                          out.write(_name._value+"[N] = {");
			else if (isDeleted())                      out.write(_name._value+"[D] = {");
			for (BaseData t : _data) out.write(t._value+" ");
			out.write("}");
		}
		public boolean isModified() {
			if (super.isModified()) return true;
			for (BaseData t : _data) if ((t._end==-1) ^ (t._beg<0)) return true;
			return false;
		}
		protected int midIndex() { return (isNew())?-1:_beg; };
		public int endIndex() { return (isNew())?-1:_end; };
	    public void clean() { _data.clear(); super.clean(); }
	};
	/**
	 * Full Structure ; it can contains any kind of Field and
	 * is the only Field sub-type with that ability.<p>
	 * 
	 * @author yvesp
	 */
	static public class StructField extends Field<LinkedList<Field<?>>> {
		/**
		 * Position in the input for the opening {
		 */
		protected int _beg=0;		
		/**
		 * Position in the input for the closing }
		 */
		protected int _end=0;
		/**
		 * Layout for the fields
		 */
		public Layout _layout=Layout.FLAT;
		/**
		 * Constructor used by the analyzer.
		 * @param name of the field
		 */
		public StructField(Token name) { super(name); _data=new LinkedList<Field<?>>(); }
		/**
		 * Constructor for creating a field from scratch.<p>
		 * Useful for adding new fields in an existing tree.<p>
		 * @param name of the field
		 */
		public StructField(String name) { super(new Token(null,0,-1,name,0)); _data=new LinkedList<Field<?>>(); }
		/**
		 * Adds a field at the end of the field list. Used by the analyzer.<p>
		 * @param f field to add
		 */
		public void addField(Field<?> f) {
			if (f._data==null && !f.isNew()) return;
			f.setOwner(this);
			_data.addLast(f);
		}
		/**
		 * Adds a field in the field list<p>
		 * @param position which the new field should occupy.
		 * @param f field to insert. It should have been created beforehand from scratch.
		 */
		public void addField(int position, Field<?> f) {
			f.setOwner(this);
			_data.add(position,f);
		}
		/**
		 * Adds a field in the field list a the specified position.<p>
		 * @param f to add
		 * @param i index of the position the new item is to occupy ; -1 means last.
		 */
		public void addFieldAt(int i, Field<?> f) {
			f.setOwner(this);
			if (i==-1) _data.addLast(f);
			else       _data.add(i, f);
		}
		/**
	     * Adds the field in the position it should occupy as defined by the template.
	     * If some existing fields are not declared in the template, you may get unexpected
	     * results. The call doesn't check the template validity against the existing fields.
	     * The fields is place at the first legal position.
		 * @param f field to insert
		 */
		public void insertFieldWithTemplate(Field<?> f, Template t) {
			addFieldAt(getFirstIndex(f._name._value,t),f);
		}
		/**
		 * Finds the first possible index position for a field of that name
		 * in the current field.
		 * @param name name of the field to add
		 * @return position found
		 * @throws IllegalStateException if there is any problem
		 */
		public int getFirstIndex(String name, Template t) throws IllegalStateException {
			if (t==null) throw new IllegalArgumentException("no template");
			//get container and check we know it
			Selector s = t.getSelector(this);
			if (s==null) throw new IllegalStateException("containing field"+_name._value+"not in template");
			Selector expected[] = s._children;
			//check if the field already exists in the data : if so, first place is the existing one
			Iterator<Field<?>> x = _data.iterator();
			int k=0; while (x.hasNext()) { if (x.next()._name._value.equals(name)) break; k++; }
			if (k!=_data.size()) return k; //found!
			//check that we know the field in the template and find it's place
			int n; for (n=0; n<expected.length && !expected[n]._name.equals(name); n++);
			if (n==expected.length) throw new IllegalStateException("field "+name+" not found in template");
			//we must find the existing field closest to n in the template
			k=0;
			for (Field<?> f : _data) {
				int i;
                for (i=0; i<n && !f._name._value.equals(expected[i]._name); i++);
                if (i==n) return k; //that's the first field taht's not found : good : take it's position!
                k++;
            }
			//not found ???? put it last
			return _data.size();
		}
		/**
		 * Finds the last possible index position for a field of that name
		 * in the current field.
		 * @param name name of the field to add
		 * @return position found
		 * @throws IllegalStateException if there is any problem
		 */
		public int getLastIndex(String name, Template t) throws IllegalStateException {
			int k = getFirstIndex(name, t);
			if (k==_data.size()) return k;
			ListIterator<Field<?>> x = _data.listIterator(k);
			if (x.hasNext()) {
				Field<?> f = x.next();
				if (f._name._value.equals(name))
					do k++; while (x.hasNext() && x.next()._name.equals(name));
			}
			return k;
		}
		/**
		 * Finds all the fields with a given name.<p>
		 * Note that all the subfields in the returned array will (normally) be of the
		 * same sub-type (all StructFields or all BaseField etc)<p>
		 * 
		 * @param name name of the field to find
		 * @param deleted if true, returns all fields, including deleted ones
		 * @return the fields with that name
		 */
		public Field<?>[] getAll(String name, boolean deleted) {
			int n=0;
			Field<?>[] f = new Field<?>[_data.size()];
			for (Field<?> x : _data)
				if ((deleted || !x.isDeleted()) && x._name._value.equals(name))
					f[n++] = x;
			Field<?>[] g = new Field<?>[n];
			System.arraycopy(f, 0, g, 0, n);
			return g;
		}
		/**
		 * Finds all the fields with a given name.<p>
		 * They are all returned as a StructField, which may generate a runtime error
		 * if one of the found instance is not a StructField. Such a situation may happen
		 * in particular if one of the instance is an empty structure, which would be
		 * analyzed as a ListField. In that case, the best would be to delete the
		 * faulty field and add a new one.<p>
		 * 
		 * @param name name of the field to find
		 * @param deleted if true, returns all fields, including deleted ones
		 * @return the fields with that name
		 */
		public StructField[] getAllStruct(String name, boolean deleted) {
			int n=0;
			StructField[] f = new StructField[_data.size()];
			for (Field<?> x : _data) {
				if ((deleted || !x.isDeleted()) && x._name._value.equals(name))
					f[n++] = (StructField)x;
			}
			StructField[] g = new StructField[n];
			System.arraycopy(f, 0, g, 0, n);
			return g;
		}
		/**
		 * Same, as {@link Field#getAll(String, boolean)} except we don't get the
		 * deleted fields and the cast is already done.
		 */
		public StructField[] getAllStruct(String name) {
			return getAllStruct(name,false);
		}
		/**
		 * Finds all the fields with a given name.<p>
		 * They are all returned as a AnonField, which may generate a runtime error
		 * if one of the found instance is not a AnonField. <p>
		 * 
		 * @param name name of the field to find
		 * @param deleted if true, returns all fields, including deleted ones
		 * @return the fields with that name
		 */
		public AnonField[] getAllAnon(String name, boolean deleted) {
			int n=0;
			AnonField[] f = new AnonField[_data.size()];
			for (Field<?> x : _data) {
				if ((deleted || !x.isDeleted()) && x._name._value.equals(name))
					f[n++] = (AnonField)x;
			}
			AnonField[] g = new AnonField[n];
			System.arraycopy(f, 0, g, 0, n);
			return g;
		}
		/**
		 * Same, as {@link Field#getAll(String, boolean)} except we don't get the
		 * deleted fields and the cast is already done.
		 */
		public AnonField[] getAllAnon(String name) {
			return getAllAnon(name,false);
		}
		/**
		 * Finds all the fields with a given name.<p>
		 * They are all returned as a BaseField, which may generate a runtime error
		 * if one of the found instance is not a BaseField.<p>
		 * 
		 * @param name name of the field to find
		 * @param deleted if true, returns all fields, including deleted ones
		 * @return the fields with that name
		 */
		public BaseField[] getAllBase(String name, boolean deleted) {
			int n=0;
			BaseField[] f = new BaseField[_data.size()];
			for (Field<?> x : _data) {
				if ((deleted || !x.isDeleted()) && x._name._value.equals(name))
					f[n++] = (BaseField)x;
			}
			BaseField[] g = new BaseField[n];
			System.arraycopy(f, 0, g, 0, n);
			return g;
		}
		/**
		 * Same, as {@link Field#getAll(String, boolean)} except we don't get the
		 * deleted fields and the cast is already done.
		 */
		public BaseField[] getAllBase(String name) {
			return getAllBase(name,false);
		}
		/**
		 * Finds all the fields with a given name.<p>
		 * They are all returned as a ListField, which may generate a runtime error
		 * if one of the found instance is not a ListField.<p>
		 * 
		 * @param name name of the field to find
		 * @param deleted if true, returns all fields, including deleted ones
		 * @return the fields with that name
		 */
		public ListField[] getAllList(String name, boolean deleted) {
			int n=0;
			ListField[] f = new ListField[_data.size()];
			for (Field<?> x : _data) {
				if ((deleted || !x.isDeleted()) && x._name._value.equals(name))
					f[n++] = (ListField)x;
			}
			ListField[] g = new ListField[n];
			System.arraycopy(f, 0, g, 0, n);
			return g;
		}
		/**
		 * Same, as {@link Field#getAll(String, boolean)} except we don't get the
		 * deleted fields and the cast is already done.
		 */
		public ListField[] getAllList(String name) {
			return getAllList(name,false);
		}
		public Field<?> get(String name, boolean deleted) {
			for (Field<?> f : _data)
				if ((deleted || !f.isDeleted()) && f._name._value.equals(name))
					return f;
			return null;
		}
		
		public Field<?> get(int index) {
			return _data.get(index);
		}
		public int size() {
			return _data.size();
		}
		/**
		 * Same as {@link Field#get(String, boolean)}, except we don't get a field
		 * and the appropriate cast is already done.
		 */
		public BaseField getBase(String name) {
			return (BaseField)get(name,false);
		}
		/**
		 * Same as {@link Field#get(String, boolean)}, except we don't get a field
		 * and the appropriate cast is already done.
		 */
		public StructField getStruct(String name) {
			return (StructField)get(name,false);
		}
		/**
		 * Same as {@link Field#get(String, boolean)}, except we don't get a field
		 * and the appropriate cast is already done.
		 */
		public AnonField getAnon(String name) {
			return (AnonField)get(name,false);
		}
		/**
		 * Same as {@link Field#get(String, boolean)}, except we don't get a field
		 * and the appropriate cast is already done.
		 */
		public ListField getList(String name) {
			return (ListField)get(name,false);
		}
		public boolean isModified() {
			if (super.isModified()) return true;
			Iterator<Field<?>> i = _data.iterator();
			while (i.hasNext()) {
				if (i.next().isModified()) return true;
			}
			return false;
		}
		/**
		 * This is meant to write a structure content ; however, we care for the important
		 * very special case where _name is "" : this is the artificial container for the
		 * whole data : it has to be treated in it's own particular way.
		 */
		protected void write(ReaderWriter rw) throws IOException {
			doIndent(rw);
			rw.write(_name._value+" = {");
			writeData(rw);
			doIndentEnd(rw);
			rw.write("}");   
		}
		protected void writeData(ReaderWriter rw) throws IOException {
			rw.write(_data.toArray(new Field<?>[_data.size()]));
		}
		/**
		 * Writes the StructField to the output.
		 * Because the first level of structure is considered as being fake (we use the
		 * construct of StructField to actually store what is a list of Field<?>), the
		 * name of the structure will not show, as will not it's opening { and closing }.
		 * @param in   The initial data which we analyzed. If null, we will only output
		 *             the known fields ; if not null, we will complement the known fields
		 *             with the data that was skipped during the analysis.
		 * @param out  The stream writer where we write the result.
		 * @throws IOException
		 */
		public void write(Reader in, Writer out)  throws IOException {
			ReaderWriter rw = new ReaderWriter(in, out);
			rw.write(_data.toArray(new Field<?>[_data.size()]));
			rw.flush(this);
		}
		public void write(Writer out, boolean debug) throws IOException {
			if (!debug && isDeleted()) return;
		    if (_owner!=null) {
			    doIndent(out);
			    if (!debug || !(isNew() && isDeleted()))   out.write(_name._value+" = {");
			    else if (isNew() && isDeleted())           out.write(_name._value+"[ND] = {");
			    else if (isNew())                          out.write(_name._value+"[N] = {");
			    else if (isDeleted())                      out.write(_name._value+"[D] = {");
		    }
		    for (Field<?> f : _data) f.write(out,debug);
		    if (_owner!=null) {
			    doIndentEnd(out);
		        out.write("} ");
		    }
		}
		protected int midIndex() { return (isNew())?-1:_beg; }
		public int endIndex() { return (isNew())?-1:_end; }
	    protected  Layout  getLayout()  { return _layout; }
	    public void clean() { for (Field<?> f : _data) f.clean(); super.clean(); }

	};
	
	
	/**
	 * A Hooks that loads the data analyzed into memory.<p>
	 * If the data is large, you may require a lot of memory!<p>
	 * <p>
	 * The afterXXX calls actually insert the data into the tree. If you set the
	 * _name field of the current Field to null before this happens, the whole
	 * subtree will be discarded. Not beautiful but efficient.<p>
	 * 
	 * @author yvesp
	 */
	static final public class TreeHooks implements Hooks {
		/**current field being analyzed*/
		public Field<?> _cur      = null; 
		/**last name Token found*/
		public Token    _lastName = null;
		/**true when analysis is finished*/
		public boolean  _done     = false;
        private int[]   _line     = new int[40];
        private int     _depth    = -1;
		
		public void getName(Token t) {
			_lastName=t;
		};
		public boolean beforeStruct(Token t) {
			//create a new structure associated with the current one
			StructField next = new StructField(_lastName);
			next._beg = t._beg;
			//we are now analyzing that new structure
			next._owner=_cur;
			_cur=next;
			_line[++_depth] = t._line;
			return false;
		};
		public boolean afterStruct(Token t) {
			StructField prev = (StructField)_cur._owner;
			((StructField)_cur)._end = t._end;
			((StructField)_cur)._layout = (_line[_depth--]==t._line) ? Field.Layout.FLAT : Field.Layout.INDENT;
			//only a StructField may contain a StructField : insert it in the tree
			if (_cur._name!=null)  prev.addField(_cur);
			else                   _cur.clean();
			//and return to previous level
			_cur = prev;
			return false;
		};
		public boolean beforeAnon(Token t) {
			//create a new anon associated with the current structure
			AnonField next = new AnonField(new Token(null,0,0,null,0));
			next._beg = t._beg;
			next._owner=_cur;
			//we are now analyzing that new structure
			_cur=next;
			_line[++_depth] = t._line;
			return false;
		};
		public boolean afterAnon(Token t) {
			//only a StructField may contain a AnonField
			StructField prev = (StructField)_cur._owner;
			((AnonField)_cur)._end = t._end;
			((AnonField)_cur)._layout = (_line[_depth--]==t._line) ? Field.Layout.FLAT : Field.Layout.INDENT;
			if (_cur._name!=null) prev.addField(_cur);
			else                  _cur.clean();
			_cur = prev;
			return false;
		};
		public boolean beforeList(Token t) {
			//create a new list associated with the current structure
			ListField next = new ListField(_lastName);
			next._beg = t._beg;
			//only a StructField may contain a ListField
			next._owner=_cur;
			//we are now analyzing that new structure
			_cur=next;
			return false;
		};
		public boolean afterList(Token t) {
			//only a StructField may contain a ListField
			StructField prev = (StructField)_cur._owner;
			((ListField)_cur)._end = t._end;
			if (_cur._name!=null) prev.addField(_cur);
			else                  _cur.clean();
			_cur = prev;
			return false;
		};
		public void beforeEmpty(Token t) {
			beforeList(t);
		};
		public boolean afterEmpty(Token t) {
			return afterList(t);
		};
		public void begin() {
			//the data is presented as a Structure with no container : create a fake container
			StructField all = new StructField(new Token(TokenType.TOKEN,0,0,"",0));
			all._layout=Field.Layout.INDENT;
			all._beg=0;
			_cur = all;
		};
		public void end(Token t) {
			_done = true;
			((StructField)_cur)._end  = t._end;
			//any processing scheme could be put here now that the data is loaded
		};
		public void beforeBase(Token t)  {
		}
		public boolean afterBase(Token t) {
			//that's where everything actually happens!
			BaseField baseField = new BaseField(_lastName,t);
			baseField._owner=_cur; 
			if (baseField._name==null) { baseField.clean(); return false; }
			if (_cur instanceof StructField)    //if we get a data in a structure level, we got a BaseField
				((StructField)_cur).addField(baseField);
		    else if (_cur instanceof AnonField) //if we get a data in a anon level, we just got another BaseField
		    	((AnonField)_cur).addField(baseField);
			return false;
		};
		public boolean afterListData(Token t) {
			//that's where everything actually happens!						
			//if we get a data in a list level, we just got another Token
			((ListField)_cur).addToken(t);
			return false;
		};
		
		/**
		 * Returns the data tree built after analysis.
		 * No use calling this before the analysis is complete.
		 * 
		 * @return  The data tree
		 */
		public StructField getDataTree() {
			if (!_done) throw new IllegalStateException();
			return (StructField)_cur;
		}
	}
	
}
