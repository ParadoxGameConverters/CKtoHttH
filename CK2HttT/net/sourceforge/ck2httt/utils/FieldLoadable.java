/**
 * 
 */
package net.sourceforge.ck2httt.utils;

import net.sourceforge.ck2httt.pxAnalyzer.PXTree.Field;

public interface FieldLoadable {
	boolean load(Field<?> f);  //loads data from Field f
}