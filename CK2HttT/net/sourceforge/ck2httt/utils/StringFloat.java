package net.sourceforge.ck2httt.utils;

import net.sourceforge.ck2httt.pxAnalyzer.PXTree.BaseField;
import net.sourceforge.ck2httt.pxAnalyzer.PXTree.Field;

/**
 * This structure allows the mapping between a list of String and float modifiers
 * and is loadable from a list of BaseField.
 */
public class StringFloat extends KeyString implements FieldLoadable {
	public FieldSet<StringFloat.BaseStringFloat> _assoc = new FieldSet<StringFloat.BaseStringFloat>(StringFloat.BaseStringFloat.class);

	static public class BaseStringFloat extends KeyString implements FieldLoadable {
		public float _v;
		public BaseStringFloat() {}
		public boolean load(Field<?> x) {
			_v = ((BaseField)x).getAsFloat();
			_key = x.name();
			return true;
		}
	}
	public StringFloat() {}
	public boolean load(Field<?> x) {
		_assoc.load(x);
		_key = x.name();
		return true;
	}
	public float get(String key) {
		return _assoc.search(key)._v;
	}
}