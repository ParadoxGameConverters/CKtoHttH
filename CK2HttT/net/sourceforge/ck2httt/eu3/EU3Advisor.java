package net.sourceforge.ck2httt.eu3;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Locale;
import java.util.NavigableSet;
import java.util.TreeSet;

import net.sourceforge.ck2httt.pxAnalyzer.PXTree.BaseField;
import net.sourceforge.ck2httt.pxAnalyzer.PXTree.StructField;
import net.sourceforge.ck2httt.utils.OptionSection;



public class EU3Advisor implements Comparable<EU3Advisor> {
	
	private static String __file;
	private static int __advisorId = 2001;
	
	int _id;
	int _prov;
	String _name;  
	int _skill;
	String _type;
	String _date;
	String _death_date;
	int _year;
	int _death_year;
	StructField _root = new StructField("");
	static TreeSet<EU3Advisor> __list = new TreeSet<EU3Advisor>();
	
	public enum Type {
		THEOLOGIAN("theologian"),
		STATESMAN("statesman"),
		ARTIST("artist"),
		NAVAL("naval_reformer"),
		DIPLOMAT("diplomat"),
		SCIENTIST("natural_scientist"),
		PHILOSOPHER("philosopher"),
		ARMY("army_reformer"),
		TRADER("trader"),
		SPYMASTER("spymaster")  
		;
		final private String _val;
		Type(String val) { _val=val; }
		public String toString() { return _val; }
	}
	
	// dummy constructor for location comparisons.
	public EU3Advisor(int where) {
		_prov = where;
		_name = "John Doe";
		_id = 2000; // before our start numbers 
	}
	
	public EU3Advisor(String name, String where, int skill, String type, short birth, byte month, byte day, short death) {
		if (month<1)  month=1;
		if (month>12) month=12;
		if (day<1)    day=1;
		if (day>30)   day=30;
		StructField f = new StructField("advisor");
		f._layout=StructField.Layout.INDENT;
		f.addField(new BaseField("name",("\"" + name + "\"")));
		f.addField(new BaseField("location",where));
		f.addField(new BaseField("skill",skill));
		f.addField(new BaseField("type",type));
		f.addField(new BaseField("date",String.format(Locale.US,"%d.%d.%d", birth,month,day)));
		f.addField(new BaseField("death_date",(new Integer(death)).toString()+".1.1"));
		_name = name;
		if ((_name.length() < 2) || (!_name.startsWith("\""))) {
			_name = '"' + _name + '"';
		}
		_prov = Integer.parseInt(where);
		_skill = skill;
		_type = type;
		_year = birth;
		_death_year = death;
		_date = String.format(Locale.US,"%d.%d.%d", birth,month,day);
		_death_date = (new Integer(death)).toString()+".1.1";
		_id = __advisorId++;
		_root.addField(f);
	}
	
	static public void addAdvisor(String name, String where, int skill, String type, short birth, byte month, byte day, short death) {
		EU3Advisor a = new EU3Advisor(name,where,skill,type,birth,month,day,death);
		__list.add(a);
	}
		
	static public void write(String out) throws IOException {		
		PrintWriter x = new PrintWriter(new OutputStreamWriter(new FileOutputStream(out, true), "ISO-8859-1"));
		for (EU3Advisor a : __list) {
		    x.println();
			a._root._layout = StructField.Layout.INDENT;
		    a._root.write(x,false);
		    x.println();
		}
		x.close();
	}
	
	public static NavigableSet<EU3Advisor> getAdvisors() {
		return __list;
	}
	
	public int compareTo(EU3Advisor a) {
		if (_prov != a._prov) {
			return _prov - a._prov;
		}
		
		return _id - a._id;
	}
	
	public static void setOutputFile(String file) {
		__file = file;
	}
	
	public static void loadDefaultAdvisors(String path) throws IOException {
		// load all the advisors from history/advisors
		File dir = new File(path);
		if (dir.exists()) {
			for (File f: dir.listFiles()) {		
				BufferedReader advisorBR = new BufferedReader(new FileReader(f));
						
				String line = null;
/*
advisor = {
	name = "Sri Rahula Sthavira" #greatest practitioner of the sandesa style of Sinhalese poetry, high point of pre-modern literary production
	location = 573 #Korales/Kandy
	skill = 4
	type = artist
	date = 1455.1.1 #at peak of career at start of scenario
	death_date = 1470.1.1 #dies
}
 */			
				String name = null;
				String location = null;
				int skill = -1;
				String type = null;
				short birth = -1;
				byte month = -1;
				byte day = -1;
				short death = -1;
				
				while ((line = advisorBR.readLine()) != null) {
					line = line.trim();
					int hash = line.indexOf("#");
					if (-1 != hash) {
						line = line.substring(0, hash);
					}
					int equal = line.indexOf("=");
					String postEqual = null;
					if (-1 != equal) {
						postEqual = line.substring(equal + 1).trim();
					}					
					if (line.startsWith("name")) {
						name = postEqual; // maintains quotes. 
					} else if (line.startsWith("location")) {
						location = postEqual;
					} else if (line.startsWith("skill")) {
						skill = Integer.parseInt(postEqual);
					} else if (line.startsWith("type")) {
						type = postEqual; 
					} else if (line.startsWith("date")) {
						int first = postEqual.indexOf('.');
						int last = postEqual.lastIndexOf('.');
						if ((-1 != first) && (-1 != last) && (first != last)) {
							birth = Short.parseShort(postEqual.substring(0, first));
							month = Byte.parseByte(postEqual.substring(first + 1, last));
							day = Byte.parseByte(postEqual.substring(last + 1));
						}						
					} else if (line.startsWith("death_date")) {
						int first = postEqual.indexOf('.');
						int last = postEqual.lastIndexOf('.');
						if ((-1 != first) && (-1 != last) && (first != last)) {
							death = Short.parseShort(postEqual.substring(0, first));
						}												
					} else if (line.equals("}")) {
						if ((null != name) &&
								(null != location) &&
								(-1 != skill) &&
								(null != type) &&
								(-1 != birth) &&
								(-1 != month) &&
								(-1 != day) &&
								(-1 != death)) {
							addAdvisor(name, location, skill, type, birth, month, day, death);					
						}
						
						// reset advisor info
						name = null;
						location = null;
						skill = -1;
						type = null;
						birth = -1;
						month = -1;
						day = -1;
						death = -1;									
					}
				}
				advisorBR.close();					
			}					
		}
	}
	
	public static void writeActiveAdvisors() throws IOException {
		// these are arranged by tech group, which is truly annoying.  I have to go through
		// every single advisor and then check its province and the province's country to 
		// see what advisors are in play.
		
		// our list will be shorter than the list for a default game because the system generates new ones
		// as needed.
		PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(__file, true), "ISO-8859-1"));
		
		pw.println("active_advisors={");
		pw.println("    notechgroup={");
    	pw.println("    }");
    	String[] techGroups = { "western", "eastern", "ottoman", "muslim", "indian", "chinese", "sub_saharan", "new_world" };
    	for (String techGroup: techGroups) {
    		pw.println("    " + techGroup + "={");
    		for (EU3Advisor adv: __list) {
    			// is this advisor even active?
    			if (adv._year <= OptionSection.getStartYear()) {
	    			EU3Province prov = EU3Province.getProvById(String.valueOf(adv._prov));
	    			if ((null != prov) && (null != prov.getOwner())) {    				
	    				EU3Country eu3c = EU3Country.get(prov.getOwner());
	    				if (null != eu3c) {
	    					if (techGroup.equals(eu3c._tech_group)) {
	    						// see how much of a pain in the ass that was?
	    						pw.println("        advisor={");
	    						pw.println("            id=" + adv._id);
	    						pw.println("            idtype=39");
	    						pw.println("        }");
	    					}
	    				}
	    			}
    			} 
    		}
        	pw.println("    }");
    	}
    	pw.println("}");
    	
    	pw.close();
	}
}
