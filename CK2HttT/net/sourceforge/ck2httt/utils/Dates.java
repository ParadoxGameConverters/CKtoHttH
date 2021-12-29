package net.sourceforge.ck2httt.utils;

import java.util.HashMap;
import java.util.Map;

public class Dates {
	
	private static Map<String, Byte> __months = new HashMap<String, Byte>();
	static {
		__months.put("january", (byte) 1);
		__months.put("february", (byte) 2);
		__months.put("march", (byte) 3);
		__months.put("april", (byte) 4);
		__months.put("may", (byte) 5);
		__months.put("june", (byte) 6);
		__months.put("july", (byte) 7);
		__months.put("august", (byte) 8);
		__months.put("september", (byte) 9);
		__months.put("october", (byte) 10);
		__months.put("november", (byte) 11);
		__months.put("december", (byte) 12);
	}
	
	public static byte getMonth(String monthName) {
		byte ret = 1;
		
		Byte monthB = __months.get(monthName); 
		if (null != monthB) {
			ret = monthB.byteValue();
		}
		
		return ret;
	}
}