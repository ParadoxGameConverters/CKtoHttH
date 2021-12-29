package net.sourceforge.ck2httt.eu3;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import net.sourceforge.ck2httt.pxAnalyzer.PXTree.BaseField;
import net.sourceforge.ck2httt.pxAnalyzer.PXTree.StructField;


public class EU3Alliances {

	static StructField _root = new StructField("");
	static String      _endYear;
	
	static public void setStartYear(int year) {
		_endYear = (new Integer(year+10)).toString()+".1.1";
	}
	
	static public void makeAlliance(String tag1, String tag2) {
		StructField f = new StructField("alliance");
		f._layout=StructField.Layout.INDENT;
		f.addField(new BaseField("first",tag1));
		f.addField(new BaseField("second",tag1));
		f.addField(new BaseField("start_date","1400.1.1"));
		f.addField(new BaseField("end_date",_endYear));
		_root.addField(f);
	}
	
	static public void write (String out) throws IOException {
		PrintWriter x = new PrintWriter(new OutputStreamWriter(new FileOutputStream(out, true), "ISO-8859-1"));		
		_root._layout = StructField.Layout.INDENT;
		_root.write(x,false);
		x.close();
	}
	
}
