package net.sourceforge.ck2httt.ck;

import static net.sourceforge.ck2httt.utils.Check.checkFatal;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Locale;
import java.util.TreeMap;

import net.sourceforge.ck2httt.pxAnalyzer.PXAdvancedAnalyzer;
import net.sourceforge.ck2httt.pxAnalyzer.PXTree.BaseField;
import net.sourceforge.ck2httt.pxAnalyzer.PXTree.Field;
import net.sourceforge.ck2httt.pxAnalyzer.PXTree.ListField;
import net.sourceforge.ck2httt.pxAnalyzer.PXTree.StructField;
import net.sourceforge.ck2httt.utils.FieldLoadable;
import net.sourceforge.ck2httt.utils.FieldSet;
import net.sourceforge.ck2httt.utils.Key;
import net.sourceforge.ck2httt.utils.KeyString;
import net.sourceforge.ck2httt.utils.SearchSet;

public class County implements FieldLoadable, Key<String> {

	//initial data
	/** province id */
	public String       _id;
	/** province culture */
	public String       _culture;
	/** province culture */
	public String       _religion;
	/** province culture */
	public String       _baseCulture;
	/** analyzed data */
	public StructField  _base;
	/** basic income */
	public int          _baseIncome;
	/** real income taking current buildings/effects into account */
	public float        _income;
	/** nominal title for that province => real owner */
	public Title        _county;
	/** nominal duchy for that province */
	public Title        _duchy;
	/** nominal kingdom for that province */
	public Title        _kingdom;
	//filled when loading countries
	/** controller for that province */
	public Country      _controller;
	/** owner for that province */
	public Country      _owner;
	/** is that province a capital ? */
	public boolean      _isCapital;
	/** province improvements */
	public byte[]       _improvements;
	/** province advances */
	public short[]      _advances;
	/** province effects */
	public byte[]       _effects;
	// count of living characters in this county
	public int			_population;
	// count of living characters with a martial education in this county
	public int			_martialPopulation;
	// count of living characters with a court education in this county
	public int			_courtPopulation;
	// count of living characters with a church education in this county
	public int			_churchPopulation;

	static public FieldSet<County> __list = new FieldSet<County>(County.class);
	
	
	static void loadAll(String CKPath, StructField root) throws FileNotFoundException, IOException {
		ProvinceData.loadFromCSV(CKPath+"db/province.csv");
		ProvinceData.loadFromScenario(CKPath+"scenarios/1066_scenario_provinces.inc");
		Improvements.load(CKPath+"db/provinceimprovements.txt");
		Effects.load(CKPath+"db/provinceeffects.txt");
		__list.load(root);		
	}
	
	static public County getFromTitle(String tag) {
		if (tag.charAt(0)!='C') return null;
		String id = null;
		try {
		    id = new Integer(tag.substring(1)).toString();
		}
		catch(NumberFormatException e) {
			return null;
		}
		return __list.search(id);
	}
	
	/**
	 * first processing pass : build the basis county list
	 * @param root
	 */
	public boolean load(Field<?> x) {
		if (!x.name().equals("province")) return false;
		StructField f = (StructField)x;
		_id = f.getBase("id").get();
		ProvinceData pd = ProvinceData.__list.search(_id);
		checkFatal(pd,"province "+_id,"db/province.csv");
		_baseIncome = (new Integer(pd._income)).intValue();
		if (_baseIncome==0) return false; //for sure, that's not a normal province
		_baseCulture = pd._culture;
		_base = f;
		BaseField c = _base.getBase("culture");
		if (c==null) return false; //can happen for some special provinces
		_culture = c.getUnquoted();
		c = _base.getBase("religion");
		if (c==null) return false; //can happen for some special provinces
		_religion = c.get();
		String title = String.format(Locale.US,"C%03d", new Integer(_id));
		_county = Title.__list.search(title);
		checkFatal(_county,"title",title);
		if (pd._duchy!=null)   _duchy   = Title.__list.search(pd._duchy);
		if (pd._kingdom!=null) _kingdom = Title.__list.search(pd._kingdom);
		byte[] imp = new byte[200];
		int    nb = 0;
		Field<?> fi = f.get("improvements");
		if (fi instanceof StructField) {
			for (Field<?> fix : ((StructField)fi)._data) {
				Improvements i = Improvements.__list.search(fix._name._value);
				checkFatal(i,fix._name._value,"db/provinceimprovements.txt");
				imp[nb++] = i._idx;
			}
		}
		_improvements=new byte[nb];
		System.arraycopy(imp, 0, _improvements, 0, nb);
		nb = 0;
		fi = f.get("effects");
		if (fi instanceof StructField) {
			for (Field<?> fix : ((StructField)fi)._data) {
				Effects e = Effects.__list.search(fix._name._value);
				checkFatal(e,fix._name._value,"db/provinceeffects.txt");
				if (e!=null) imp[nb++] = e._idx;
			}
		}
		_effects=new byte[nb];
		System.arraycopy(imp, 0, _effects, 0, nb);
		nb = 0;
		short[] tmp = new short[200];
		fi = f.get("advances");
		if (fi instanceof StructField) {
			for (Field<?> fix : ((StructField)fi)._data) {
				tmp[nb++] = Advances.search(fix._name._value);
			}
		}
		_advances=new short[nb];
		System.arraycopy(tmp, 0, _advances, 0, nb);
		_income = income();
		return true;
	}
		
	public County() {}
	
	private float income() {
		float base = _baseIncome;
		for (byte b : _improvements) base *= Improvements.__array[b]._multGold;
		for (byte b : _effects)      base *= Effects.__array[b]._multGold;
		for (byte b : _improvements) base += Improvements.__array[b]._plusGold;
		return base;
	}
	
	static public String getCountyName(String id) {
		ProvinceData pd = ProvinceData.__list.search(id);
		
		return pd._name;
	}
			
	static public class ProvinceData extends KeyString {
		private String _id;
		private String _name;
		private String _income;
		private String _culture;
		private String _duchy;
		private String _kingdom;
		
		static private SearchSet<String,ProvinceData> __list = new SearchSet<String,ProvinceData>(ProvinceData.class);
		public ProvinceData() {}
		
		static public void loadFromCSV(String filename) throws FileNotFoundException, IOException {
		    BufferedReader r = new BufferedReader(new FileReader(filename)); 
		    r.readLine();
		    String s=null;
		    do {
		    	s = r.readLine();
		    	if (s==null) return;
		    	String[] x = s.split(";");
		    	ProvinceData p = new ProvinceData(x[0]);
		    	p._key = p._id;
		    	p._name = x[2];
		    	p._income = x[9];
		    	p._culture = x[8];
		    	p._duchy = x[10];
		    	p._kingdom = x[11];
		    	__list.add(p);
		    }
		    while (true);
		}
		
		static public void loadFromScenario(String filename) throws FileNotFoundException, IOException {
		    BufferedReader r = new BufferedReader(new FileReader(filename)); 
		    r.readLine();
		    String s=null;
		    String id = null;
		    String culture = null;
		    do {
		    	s = r.readLine();
		    	if (s==null) return;
		    	s = s.trim();		    	
		    	if (s.startsWith("province")) {
		    		// beginning a new province.
		    		id = null;
		    		culture = null;
		    	}
		    	if (s.startsWith("id = ")) {
		    		id = s.substring("id = ".length()).trim();
		    	}
		    	if (s.startsWith("culture = ")) {
		    		culture = s.substring("culture = ".length()).trim();
		    		ProvinceData pd = ProvinceData.__list.search(id);
		    		if (null != pd) {
		    			pd._culture = culture;
		    		}
		    	}
		    }
		    while (true);
		}
		
		private ProvinceData(String id) { _id=id; }
	}

	static public class Improvements extends KeyString implements FieldLoadable {
		public byte   _idx;
		public String _name;
		public int    _plusGold=0;
		public float  _multGold=1;
		public static FieldSet<Improvements> __list = new FieldSet<Improvements>(Improvements.class);
		public static Improvements[] __array;
		public Improvements() {}
		static public void load(String filename) throws IOException {
			String _filter = "{*{lasting_effects{percentage_score{gold},periodic_score{gold}}}}";
			__list.load(new PXAdvancedAnalyzer(filename,true).analyze(_filter));
			__array = __list.toArray(new Improvements[__list.size()]);
			for (byte i=0; i<__array.length; i++) __array[i]._idx=i;
		}
		public boolean load(Field<?> g) {
			StructField f = (StructField)g;
			_name = f._name._value;
			_key = _name;
			StructField x = f.getStruct("lasting_effects");
			if (x!=null) {
				Field<?> y = x.getStruct("percentage_score");
				if (y!=null && y instanceof StructField) {
			        BaseField z = ((StructField)y).getBase("gold");
				    if (z!=null) _multGold=z.getAsFloat();
				}
			    StructField z = x.getStruct("periodic_score");
			    if (z!=null) {
			        BaseField zz = z.getBase("gold");
			        if (zz!=null) _plusGold=zz.getAsInt();
			    }
			}
			return true;
		}
	}

	static public class Effects extends KeyString implements FieldLoadable {
		public String _name;
		public float  _multGold=1;
		public byte   _idx;
		public static FieldSet<Effects> __list = new FieldSet<Effects>(Effects.class);
		public static Effects[]         __array;
		public Effects() {}
		static public void load(String filename) throws IOException {
			String _filter = "{*{effects{percentage_score{gold}}}}";
			__list.load(new PXAdvancedAnalyzer(filename,true).analyze(_filter));
			__array = __list.toArray(new Effects[__list.size()]);
			for (byte i=0; i<__array.length; i++) __array[i]._idx=i;
		}
		public boolean load(Field<?> g) {
			StructField f = (StructField)g;
			_name = f._name._value;
			_key = _name;
			Field<?> x = f.get("effects");
			if (x==null || x instanceof ListField) return true;
			StructField y = ((StructField)x).getStruct("percentage_score");
			if (y==null) return false;
			BaseField z = y.getBase("gold");
			if (z!=null) _multGold=z.getAsFloat();
			return true;
		}
	}
	
	static public class Advances {
		private static TreeMap<String,Integer> __list = new TreeMap<String,Integer>();
		static public String[] __array = new String[200];
		private static short __nb=0;
		static short search(String a) {
			Integer i = __list.get(a);
			if (i!=null) return i.shortValue();
			__array[__nb]=a;
			__list.put(a,new Integer(__nb));
			__nb++;
			return (short)(__nb-1);
		}
	}
	
	public String getKey()               { return _id; }
	public void   setKey(String k)       { _id=k; }
	public void   setSuccessor(String k) { _id=k+"\0"; }
	public void	incPopulation() { _population++; }
	public double getPopulation() { 
		return getPopulation(false); 
	}
	public double getPopulation(boolean average) {
		if (average) {
			double pop = _population; 
			double demense = _owner._owned.size(); 
			return 100 * pop / demense;
		} else {
			return _martialPopulation;
		}
	}	
	public void incMartialPopulation() { _martialPopulation++; }
	public double getMartialPopulation() { 
		return getMartialPopulation(false); 
	}
	public double getMartialPopulation(boolean average) {
		if (average) {
			double maPop = _martialPopulation; 
			double demense = _owner._owned.size(); 
			return 100 * maPop / demense;
		} else {
			return _martialPopulation;
		}
	}
	public void incCourtPopulation() { _courtPopulation++; }
	public double getCourtPopulation() {
		return getCourtPopulation(false);
	}
	public double getCourtPopulation(boolean average) {
		if (average) {
			double coPop = _courtPopulation; 
			double demense = _owner._owned.size(); 
			return 100 * coPop / demense;			
		} else {
			return _courtPopulation;
		}
	}
	public void incChurchPopulation() { _churchPopulation++; }
	public double getChurchPopulation() { 
		return getChurchPopulation(false);
	}
	public double getChurchPopulation(boolean average) {
		if (average) {
			double chPop = _churchPopulation; 
			double demense = _owner._owned.size(); 
			return 100 * chPop / demense;						
		} else {
			return _churchPopulation;
		}
	}	
}
