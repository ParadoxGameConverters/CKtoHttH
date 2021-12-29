package net.sourceforge.ck2httt.eu3;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

public class EU3Header {

	private EU3Header() {
	}

	public static void eraseOutputFile(String fileName) throws IOException {
		File f = new File(fileName);
		if (f.exists()) {
			f.delete();	
		}
	}
	
	public static void writeHeader(String in, String out) throws IOException {
		PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(out, true), "ISO-8859-1"));
		
		writeHeader(in, pw);
		
		pw.close();
	}

	public static void writeHeader(String in, PrintWriter pw) throws IOException {
		BufferedReader headerBR = new BufferedReader(new FileReader(in));
		
		String line = null;
		while ((line = headerBR.readLine()) != null) {
			pw.println(line);
		}
				
		headerBR.close();
	}
	
}	
