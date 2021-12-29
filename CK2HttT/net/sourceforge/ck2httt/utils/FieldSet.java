package net.sourceforge.ck2httt.utils;

import net.sourceforge.ck2httt.pxAnalyzer.PXTree.Field;
import net.sourceforge.ck2httt.pxAnalyzer.PXTree.StructField;


public class FieldSet<T extends FieldLoadable & Key<String>> extends SearchSet<String,T> {
	
	private enum Operation { ADD, REMOVE, REPLACE };
	
	static final long serialVersionUID=0;

	public FieldSet(Class<T> typeT) { super(typeT); }
	
	public boolean load(Field<?> x) {
		return load(x,false);
	}
	public boolean load(Field<?> x, boolean replace) {
		if (x==null || !(x instanceof StructField)) return false;
		return load((StructField)x, replace);
	}
	public boolean delete(Field<?> x) {
		if (x==null || !(x instanceof StructField)) return false;
		return delete((StructField)x);
	}
	public boolean put(Field<?> f) {
		return put(f,Operation.ADD);
	}
	private boolean put(Field<?> f, Operation op) {
		try {
		    T data = _typeT.newInstance();
		    if (data.load(f)) {
		    	if (op!=Operation.ADD && contains(data))
		    		remove(data);
		    	if (op!=Operation.REMOVE)
		    		add(data);
		        return true;
		    }
		}
		catch(InstantiationException e) {
			e.printStackTrace();
		}
		catch(IllegalAccessException e) {
			System.out.format("check that default constructor is public for %s\n",_typeT.getCanonicalName());
			e.printStackTrace();
		}
		return false;
	}
	private boolean load(StructField x, boolean replace) {
		for (Field<?> f : x._data)
			put(f, (replace)?Operation.REPLACE:Operation.ADD);
		return true;
	}	
	private boolean delete(StructField x) {
		for (Field<?> f : x._data)
			put(f, Operation.REMOVE);
		return true;
	}	
	
	static private class FieldSetSection extends OptionSection {
 		private FieldSet<?> _list;
 		public FieldSetSection(StructField root, FieldSet<?> list) { super(root); _list=list; }
 		public void add(Field<?> data,String section) {
 			_list.load(data,false);
 		}	
 		public void remove(Field<?> data,String section) {
 			_list.delete(data);
 		}	
 		public void replace(Field<?> data,String section) {
 			_list.load(data,true);
 		}	
 	}
	 		
 	public void loadOptionSection(StructField root, String subsection) {
 		new FieldSetSection(root,this).load(subsection);
 	}		
}
