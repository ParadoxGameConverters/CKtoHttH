/**
 * 
 */
package net.sourceforge.ck2httt.utils;

import java.util.TreeSet;

import net.sourceforge.ck2httt.pxAnalyzer.PXTree.BaseData;
import net.sourceforge.ck2httt.pxAnalyzer.PXTree.Field;
import net.sourceforge.ck2httt.pxAnalyzer.PXTree.ListField;
import net.sourceforge.ck2httt.pxAnalyzer.PXTree.StructField;


/**
 * Used to load a ListField inside a TreeSet using option sections.
 * @author Yves
 *
 */
public class TreeSetSection extends OptionSection {
	private TreeSet<String> _list;
	public TreeSetSection(StructField root, TreeSet<String> list) { super(root); _list=list; }
	public TreeSetSection(StructField root) { super(root); _list=null; }
	public void setList(TreeSet<String> list) { _list=list; }
	public void add(Field<?> data,String section) {
		if (! (data instanceof ListField)) throw new IllegalStateException("ListField expected"); 
		for (BaseData d : ((ListField)data)._data)
			_list.add(d._value);				
	}
	public void remove(Field<?> data,String section) {
		if (! (data instanceof ListField)) throw new IllegalStateException("ListField expected"); 
		for (BaseData d : ((ListField)data)._data)
			_list.remove(d._value);				
	}
	public void replace(Field<?> data,String section) {
		if (! (data instanceof ListField)) throw new IllegalStateException("ListField expected"); 
		for (BaseData d : ((ListField)data)._data) {
			_list.add(d._value);
		}
	}	
}