package net.sourceforge.ck2httt.utils;
import java.io.*;

public class RegRead {

	  private static final String REGQUERY_UTIL  = "reg query ";
	  private static final String REGSTR_TOKEN   = "REG_SZ";
	  private static final String REGDWORD_TOKEN = "REG_DWORD";

	  public static final String CKPath = 
		    "\"HKLM\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\App Paths\\Crusaders.exe\" /v Path";
	  public static final String EUPath = 
		    "\"HKLM\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\App Paths\\eu3.exe\" /v Path";
	  public static final String CKPath1 = 
		    "\"HKLM\\Software\\Paradox Entertainment\\Crusader Kings\" /v path";
	  public static final String CKPath2 = 
		    "\"HKLM\\Software\\Paradox Interactive\\Crusader Kings\" /v path";
	  public static final String EUPath1 = 
	    "\"HKLM\\Software\\Paradox Interactive\\Europa Universalis III\" /v path";
	  
	  public static String getCKpath() {
		  String s = getString(CKPath);
		  if (s!=null) {
                      return s;
                  }
		  s = getString(CKPath1);
		  if (s!=null) return s;
		  s = getString(CKPath2);
		  return s;
	  }
	  
	  public static String getEUpath() {
		  String s = getString(EUPath);
		  if (s!=null) return s;
		  s = getString(EUPath1);
		  return s;
	  }
	  
	  public static String getString(String key) {
	    try {
	      Process process = Runtime.getRuntime().exec(REGQUERY_UTIL+key);
	      StreamReader reader = new StreamReader(process.getInputStream());

	      reader.start();
	      process.waitFor();
	      reader.join();

	      String result = reader.getResult();
	      int p = result.indexOf(REGSTR_TOKEN);

	      if (p == -1)
	         return null;

	      return result.substring(p + REGSTR_TOKEN.length()).trim();
	    }
	    catch (Exception e) {
	      return null;
	    }
	  }

	  public static String getInt(String key) {
	    try {
	      Process process = Runtime.getRuntime().exec(REGQUERY_UTIL+key);
	      StreamReader reader = new StreamReader(process.getInputStream());

	      reader.start();
	      process.waitFor();
	      reader.join();

	      String result = reader.getResult();
	      int p = result.indexOf(REGDWORD_TOKEN);

	      if (p == -1)
	         return null;

	      // usually (always ??) in HEX notation, convert it to DEC
	      String temp = result.substring(p + REGDWORD_TOKEN.length()).trim();
	      return Integer.toString
	          ((Integer.parseInt(temp.substring("0x".length()), 16) + 1));
	    }
	    catch (Exception e) {
	      return null;
	    }
	  }

	  static private class StreamReader extends Thread {
	      private InputStream is;
	      private StringWriter sw;

	      StreamReader(InputStream is) {
	         this.is = is;
	         sw = new StringWriter();
	      }

	      public void run() {
	         try {
	            int c;
	            while ((c = is.read()) != -1)
	                sw.write(c);
	         }
	         catch (IOException e) { ; }
	      }

	      String getResult() {
	         return sw.toString();
	      }
	  }
}
