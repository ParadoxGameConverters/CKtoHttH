package net.sourceforge.ck2httt.utils;

import net.sourceforge.ck2httt.pxAnalyzer.PXTree.Field;
import net.sourceforge.ck2httt.pxAnalyzer.PXTree.StructField;


public class FieldIntSet<T extends FieldLoadable & Key<Integer>> extends SearchSet<Integer,T> {

	static final long serialVersionUID=0;

	public FieldIntSet(Class<T> typeT) { super(typeT); }

	public boolean load(Field<?> x) {
		if (x==null || !(x instanceof StructField)) return false;
		return load((StructField)x);
	}
	public boolean put(Field<?> f) {
		try {
		    T data = _typeT.newInstance();
		    if (data.load(f)) {
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
	public boolean load(StructField x) {
		for (Field<?> f : x._data)
			put(f);
		return true;
	}	
	
}
