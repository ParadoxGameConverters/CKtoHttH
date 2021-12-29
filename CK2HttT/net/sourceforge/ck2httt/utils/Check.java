package net.sourceforge.ck2httt.utils;

public class Check {

	static public void checkFatal(Object s, String what, String info) {
		if (s==null) {
			System.out.println(what + " was not found for " + info);
			try {
				throw new IllegalStateException();
			}
			catch (Exception e) {
				e.printStackTrace(System.out);
			}
			System.exit(0);
		}
	}

	static public void checkReplace(String s, String what, String info, String by) {
		if (s==null || s.length()==0) {
			System.out.println(what + " was not found for " + info + " ; replaced by " + by);
		}
	}

}
