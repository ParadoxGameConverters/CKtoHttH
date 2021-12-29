package net.sourceforge.ck2httt.cv;

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

import net.sourceforge.ck2httt.ck.Analyzer;
import net.sourceforge.ck2httt.ck.Characters;
import net.sourceforge.ck2httt.eu3.EU3Country;
import net.sourceforge.ck2httt.pxAnalyzer.PXTree.Field;
import net.sourceforge.ck2httt.pxAnalyzer.PXTree.StructField;
import net.sourceforge.ck2httt.rules.CountryCvRules;
import net.sourceforge.ck2httt.rules.TagCvRules;
import net.sourceforge.ck2httt.utils.OptionSection;
import net.sourceforge.ck2httt.utils.Rnd;

public class CvRelationships implements Comparable<CvRelationships> {

	static private String  __destPath;
	
	public enum Type { UNION("union"), VASSAL("vassal"), ALLIANCE("alliance"), MARRIAGE("royal_marriage"), OTHER(null), WAR(null);
	    String _name;
	    Type(String name) { _name=name; }
	}
	public CvCountry            _first;
	public CvCountry            _second;
	public String			 	_firstTag;
	public String			 	_secondTag;	
	public String 				_startDate;
	public CvRelationships.Type _type;
	
	static public TreeSet<CvRelationships> __list = new TreeSet<CvRelationships>();
	static public TreeSet<CvRelationships> __eu3List = new TreeSet<CvRelationships>();

	static public void setOutputPath(String path) throws IOException {
		__destPath = path;
		File dir = new File(path);
		if (dir.exists()) {
//			for (File f: dir.listFiles())
//				f.delete();
		}
		else if (!dir.mkdirs())
			throw new IOException("Unable to create directory : "+dir);
	}
	
	static public void convertAlliances() {
		StructField global = Analyzer.__root.getStruct("relations");
		for (Field<?> fx : global._data) {
			if (!fx.name().equals("alliance")) continue;
			StructField f = (StructField)fx;
			Characters primary = Characters.search(f.getStruct("primary"));
			if (primary==null) continue;
			CvCountry pri = CvCountry.search(primary._tag);
			Characters secondary = Characters.search(f.getStruct("secondary"));
			if (secondary==null) continue;
			CvCountry sec = CvCountry.search(secondary._tag);
			if (pri==null || pri._eu3Tag==null || sec==null || sec._eu3Tag==null) continue;
			add(pri, sec, CountryCvRules.ALLY);
		}
	}
	static public CvRelationships add(CvCountry c1, CvCountry c2, int relation) {
		Type x = convert(relation);
		if (x==Type.OTHER) return null;
		
		CvRelationships r = new CvRelationships();
		r._first=c1;
		r._second=c2;
		r._type=x;
		r._startDate = makeRandomDateBefore();
		__list.add(r);
		
		return r;
	}
	
	static public CvRelationships add(String first, String second, Type type, String startDate) {
		CvRelationships r = new CvRelationships();
		r._firstTag = first;
		r._secondTag = second;
		r._type = type;
		r._startDate = startDate;
		__list.add(r);
		
		return r;
	}
	
	static public NavigableSet<CvRelationships> remove(CvCountry c1, CvCountry c2) {
		CvRelationships r = new CvRelationships();
		r._first=c1;
		r._second=c2;
		r._type=Type.UNION;

		CvRelationships r2 = new CvRelationships();
		r2._first=c1;
		r2._second=c2;
		r2._type=Type.WAR;
		NavigableSet<CvRelationships> sub = __list.subSet(r, true, r2, true);
		
		__list.removeAll(sub);
		
		return sub;
	}			
	
	static private Type convert(int r) {
		if (r==CountryCvRules.UNION)    return Type.UNION;
		if (r==CountryCvRules.VASSAL)   return Type.VASSAL;
		if (r==CountryCvRules.ALLY)     return Type.ALLIANCE;
		if (r==CountryCvRules.MARRIAGE) return Type.MARRIAGE;
		return Type.OTHER;
	}
	
	//ordered by first/second/relationship
	public int compareTo(CvRelationships r) {
		String first = (null != _first) ? _first._tag : _firstTag;
		String second = (null != _second) ? _second._tag : _secondTag;
		String rFirst = (null != r._first) ? r._first._tag : r._firstTag;
		String rSecond = (null != r._second) ? r._second._tag : r._secondTag;
		
		int x = first.compareTo(rFirst);
		if (x!=0) return x;
		x = second.compareTo(rSecond);
		if (x!=0) return x;
		return _type.compareTo(r._type);
	}
	
	static public String makeDate(int year, int month, int day) {
		return String.format(Locale.US,"%d.%d.%d",year,month,day);
	}
	
	static public String makeRandomDateStart() {
		return OptionSection.getStartDate();
	}
	static public String makeRandomDateBefore() {
		int year  = OptionSection.getStartYear()-Rnd.get().nextInt(8);
		int month = 12-Rnd.get().nextInt(12);
		int day   = 28-Rnd.get().nextInt(28);
		return makeDate(year,month,day);
	}
	
	static public String makeRandomDateAfter() {
		int year  = OptionSection.getStartYear()+Rnd.get().nextInt(10);
		int month = 1+Rnd.get().nextInt(12);
		int day   = 1+Rnd.get().nextInt(28);
		return makeDate(year,month,day);
	}
	
	static public void loadDefaultDiplomacy(String path) throws IOException {
		// load all the diplomacy from history/diplomacy
		// ignore anything with a participant in the tag pool, or anything starting after start or ending before start.
		File dir = new File(path);
		if (dir.exists()) {
			for (File f: dir.listFiles()) {		
				BufferedReader diploBR = new BufferedReader(new FileReader(f));						
/*
#Ottoman vassal
vassal = {
	first = TUR
	second = TUN
	start_date = 1531.1.1
	end_date = 1574.9.13
}
 */				
				String line = null;	
				
				Type type = null;
				String first = null;
				String second = null;
				String startDate = null;
				int startYear = OptionSection.getStartYear();
				int endYear = OptionSection.getStartYear();
				
				while ((line = diploBR.readLine()) != null) {
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
					if (line.startsWith("union")) {
						type = Type.UNION;
					} else if (line.startsWith("vassal")) {
						type = Type.VASSAL;
					} else if (line.startsWith("alliance")) {
						type = Type.ALLIANCE;
					} else if (line.startsWith("royal_marriage")) {
						type = Type.MARRIAGE;						
					} else if (line.startsWith("first")) {
						first = postEqual;
					} else if (line.startsWith("second")) {
						second = postEqual;
					} else if (line.startsWith("start_date")) {
						startDate = postEqual;
						startYear = Integer.parseInt(postEqual.substring(0, postEqual.indexOf('.')));
					} else if (line.startsWith("end_date")) {
						endYear = Integer.parseInt(postEqual.substring(0, postEqual.indexOf('.')));
					} else if (line.equals("}")) {
						if ((null != type) &&
								(null != first) &&
								(null != second) &&
								(null != startDate)) {
							
							// see if we care.  to care, neither of the participants 
							// can be in the tag pool (or we've already reassigned things)
							// and the diplomacy needs to start before start date and end after that.
							
							boolean ignore = false;
							ignore = ignore || TagCvRules.isFromTagPool(first); 
							ignore = ignore || TagCvRules.isFromTagPool(second);
							ignore = ignore || (startYear > OptionSection.getStartYear());
							ignore = ignore || (endYear <= OptionSection.getStartYear());
						
							if (!ignore) {								
								add(first, second, type, startDate);
							}
						}
						
						// reset diplomacy info
						type = null;
						first = null;
						second = null;
						startDate = null;
						startYear = OptionSection.getStartYear();
						endYear = OptionSection.getStartYear();						
					}
				}
			}
		}
	}

	static public void preWriteAll() throws IOException {
		for (CvRelationships r : __list) {
			if (((null != r._first) && r._first._removed) || 
				((null != r._second) && r._second._removed)) {
				continue;
			}

			String name = r._type._name;
			//remove alliances that are not alliances, and those with countries
			//who finally did not make it (no EU3 tag)
			if (name==null) {
				continue;
			}
			if (null != r._first) {
				r._firstTag = r._first._eu3Tag;
			}
			if (null != r._second) {
				r._secondTag = r._second._eu3Tag;
			}
			if ((r._firstTag == null) || (r._secondTag ==null)) {
				continue;		
			}
			
			__eu3List.add(r);
		}
	}

	static public void writeAll() throws IOException {
		PrintWriter x = new PrintWriter(new OutputStreamWriter(new FileOutputStream(__destPath + File.separatorChar + Analyzer.getSaveFile(), true), "ISO-8859-1"));
		x.println("diplomacy={");
		for (CvRelationships r : __eu3List) {
			r.write(x);
		}
		// and now the open markets.  essentially, if you can see them, the market is open to them
		// ...in NA or later.
		for (EU3Country eu3c : CvCountry.__eu3List) {
			// does this country hold any territory?
			if (eu3c.hasProvinces()) {
				for (EU3Country c2 : CvCountry.__eu3List) {
					if (eu3c.hasDiscovered(c2)) {
						x.println("    open_market={");
						x.println("        first=\"" + eu3c._tag + "\"");
						x.println("        second=\"" + c2._tag + "\"");
						x.println("        start_date=\"1.1.1\"");
						x.println("    }");
					}
				}
			}
		}
		
		x.println("}");
		x.close();
	}

	private void write(PrintWriter out) throws IOException {
		out.println("    " + _type.name() + "={");
		out.println("        first=\"" + _firstTag + "\"");
		out.println("        second=\"" + _secondTag + "\"");
		out.println("        start_date=\"" + _startDate + "\"");
		out.println("    }");		
	}
}