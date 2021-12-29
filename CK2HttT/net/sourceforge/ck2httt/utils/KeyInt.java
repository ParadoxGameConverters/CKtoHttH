/**
 * 
 */
package net.sourceforge.ck2httt.utils;

public class KeyInt implements Key<Integer> {
	public Integer _key;
	public Integer getKey()                { return _key; }
	public void    setKey(Integer k)       { _key=k; }
	public void    setSuccessor(Integer k) { _key=k+1; }
}