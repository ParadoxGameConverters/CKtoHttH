package net.sourceforge.ck2httt.utils;

import java.util.SortedMap;
import java.util.TreeMap;

import net.sourceforge.ck2httt.pxAnalyzer.PXTree.BaseField;
import net.sourceforge.ck2httt.pxAnalyzer.PXTree.Field;
import net.sourceforge.ck2httt.pxAnalyzer.PXTree.StructField;


public class FieldMap<K extends FieldLoadable & Comparable<K> & Nextable<K>, T extends FieldLoadable> extends TreeMap<K,T> {

	static final long serialVersionUID=0;
	static final public class IntKey implements FieldLoadable, Nextable<IntKey>, Comparable<IntKey> {
		public Integer _i;
		IntKey() {}
		public IntKey(Integer i)        { _i=i; }
		public boolean load(Field<?> f) { _i = new Integer(f.name()); return true; }
		public IntKey next()            { return new IntKey(_i+1); }
		public int compareTo(IntKey i)  { return _i.compareTo(i._i); }
	}
	static final public class IntData implements FieldLoadable {
		public Integer _i;
		IntData() {}
		public IntData(Integer i)       { _i=i; }
		public boolean load(Field<?> f) {_i = new Integer(((BaseField)f).getAsInt()); return true; }
	}
	static final public class StringKey implements FieldLoadable, Nextable<StringKey>, Comparable<StringKey> {
		public String _s;
		StringKey() {}
		public StringKey(String s)        { _s=s; }
		public boolean load(Field<?> f)   { _s = f.name(); return true; }
		public StringKey next()           { return new StringKey(_s+"\0"); }
		public int compareTo(StringKey s) { return _s.compareTo(s._s); }
	}
	static final public class StringData implements FieldLoadable {
		public String _s;
		StringData() {}
		public StringData(String s)     { _s=s; }
		public boolean load(Field<?> f) {_s = ((BaseField)f).get(); return true; }
	}
	static final public class FloatData implements FieldLoadable {
		FloatData() {}
		public Float _f;
		public FloatData(Float f)       { _f=f; }
		public boolean load(Field<?> f) { _f = new Float(((BaseField)f).getAsFloat()); return true; }
	}
	
	static final public class IntInt extends FieldMap<IntKey,IntData> {
		static final long serialVersionUID=0;
		public IntInt() { super(IntKey.class,IntData.class); }
		public int search(int key)      { return search(new IntKey(key))._i.intValue(); }
		public int searchMin(int key)   { return searchMin(new IntKey(key))._i.intValue(); }
		public int searchMax(int key)   { return searchMax(new IntKey(key))._i.intValue(); }
	}
	static final public class IntFloat extends FieldMap<IntKey,FloatData> {
		static final long serialVersionUID=0;
		public IntFloat() { super(IntKey.class,FloatData.class); }
		public float search(int key)    { return search(new IntKey(key))._f.floatValue(); }
		public float searchMin(int key) { return searchMin(new IntKey(key))._f.floatValue(); }
		public float searchMax(int key) { return searchMax(new IntKey(key))._f.floatValue(); }
	}
	static final public class StringInt extends FieldMap<StringKey,IntData> {
		static final long serialVersionUID=0;
		public StringInt()              { super(StringKey.class,IntData.class); }
		public int search(String key)   { return get(new StringKey(key))._i.intValue(); }
	}
	static final public class StringFloat extends FieldMap<StringKey,FloatData> {
		static final long serialVersionUID=0;
		public StringFloat()            { super(StringKey.class,FloatData.class); }
		public float search(String key) { return get(new StringKey(key))._f.floatValue(); }
	}
	static public class StringAny<T extends FieldLoadable> extends FieldMap<StringKey,T> {
		static final long serialVersionUID=0;
		public StringAny(Class<T> typeT) { super(StringKey.class,typeT); }
		public T search(String key)      { return get(new StringKey(key)); }
	}
	static public class IntAny<T extends FieldLoadable> extends FieldMap<IntKey,T> {
		static final long serialVersionUID=0;
		public IntAny(Class<T> typeT)    { super(IntKey.class,typeT); }
		public T search(int key)         { return get(new IntKey(key)); }
		public T searchMin(int key)      { return searchMin(new IntKey(key)); }
		public T searchMax(int key)      { return searchMax(new IntKey(key)); }
	}
	
	
	final protected Class<K> _typeK;
	final protected Class<T> _typeT;
	public FieldMap(Class<K> typeK, Class<T> typeT) {
		_typeK=typeK;
		_typeT=typeT;
	}
	public boolean load(Field<?> x) {
		if (x==null || !(x instanceof StructField)) return false;
		return load((StructField)x);
	}
	public boolean put(Field<?> f) {
		try {
		    K key = _typeK.newInstance();
		    key.load(f);
		    T data = _typeT.newInstance();
		    if (data.load(f)) {
		        put(key,data);
		        return true;
		    }
		    return false;
		}
		catch(InstantiationException e) {
			e.printStackTrace();
			return false;
		}
		catch(IllegalAccessException e) {
			e.printStackTrace();
			return false;
		}
	}
	public boolean load(StructField x) {
		for (Field<?> f : x._data)
			put(f);
		return true;
	}
	public T search(K key) {
		return get(key);
	}
	/** takes head map for that key and returns last */
	public T searchMax(K key) {
	    SortedMap<K,T> view = headMap(((Nextable<K>)key).next());
	    if (view.isEmpty()) return null;
 	    return view.get(view.lastKey());
	}
	/** takes tail map for that key and returns first */
	public T searchMin(K data) {
	    SortedMap<K,T> view = tailMap(data.next());
	    if (view.isEmpty()) return null;
 	    return view.get(view.firstKey());
	}
}