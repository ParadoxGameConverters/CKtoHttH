package net.sourceforge.ck2httt.utils;

import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Comparator;

public class SearchSet<K extends Comparable<K>,T extends Key<K>> extends TreeSet<T> {

	static final long serialVersionUID=0;

	final protected Class<T> _typeT;
	
	static public class Cmp<K extends Comparable<K>, T extends Key<K>> implements Comparator<T> {
		public int compare(T t1, T t2) { return t1.getKey().compareTo(t2.getKey()); }
	}
	
	public SearchSet(Class<T> typeT) { super(new Cmp<K,T>()); _typeT=typeT; }
		
	public T search(K key) {
		try {
			T x = _typeT.newInstance();
			x.setKey(key);
			T y = _typeT.newInstance();
			y.setSuccessor(key);
			SortedSet<T> sub = subSet(x,y);
			if (sub.isEmpty()) return null;
			return sub.first();
		}
		catch(InstantiationException e) {
			e.printStackTrace();
			return null;
		}
		catch(IllegalAccessException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/*
	static class Toto extends KeyString {
		int    _value;
		public Toto(String key, int v)       { _key=key; _value=v; }
		
		public Toto()                        {}
	}
	static public void main(String[] args) throws Exception {
		SearchSet<String,Toto> list = new SearchSet<String,Toto>(Toto.class);
		list.add(new Toto("a",1));
		list.add(new Toto("z",2));
		list.add(new Toto("e",3));
		list.add(new Toto("b",4));
		
		for (Toto t : list) {
			System.out.format("%s => %d\n",t._key,t._value);
		}
		Toto t = list.search("e");
		System.out.format("%s => %d\n",t._key,t._value);
	}
	*/
	
}
