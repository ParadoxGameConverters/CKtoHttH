/**
 * 
 */
package net.sourceforge.ck2httt.utils;

public class KeyString implements Key<String> {
	public String _key;
	public String getKey()               { return _key; }
	public void   setKey(String k)       { _key=k; }
	public void   setSuccessor(String k) { _key=k+"\0"; }
}