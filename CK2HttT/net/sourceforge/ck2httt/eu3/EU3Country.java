package net.sourceforge.ck2httt.eu3;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import net.sourceforge.ck2httt.cv.CvCountry;
import net.sourceforge.ck2httt.cv.CvRelationships;
import net.sourceforge.ck2httt.pxAnalyzer.PXTree.BaseField;
import net.sourceforge.ck2httt.pxAnalyzer.PXTree.Field;
import net.sourceforge.ck2httt.pxAnalyzer.PXTree.StructField;
import net.sourceforge.ck2httt.pxAnalyzer.PXTree.Template;
import net.sourceforge.ck2httt.rules.CultureCvRules.EU3Culture;
import net.sourceforge.ck2httt.utils.OptionSection;


public class EU3Country extends EU3FileChanger implements Comparable<EU3Country> {
		
	public static enum CountryExistence {
		DOESNT_EXIST,
		EXISTS_IN_MOD,
		EXISTS
	}
	
	private static Map<String, EU3Country> __countryByTag = new HashMap<String, EU3Country>();
	
	// 125 is filled in at the 0 and 1 position to avoid weirdness with 0-province nations.
	private static double[] __provResearchCost = { 125, 125, 150, 175, 187.5, 200, 225, 237.5, 250 };
	private static double __provResearchCostExtra = 3.12;
	private static int __maxProvCount = 88;
	private static double __maxResearchCount = 500;
	private static DecimalFormat __df = new DecimalFormat("#########0.000"); 
		                   
	private static String[] __files;
	private static String[] __filesAlt;
	private static String   __rootPath;
	private static String   __altPath;
	static Template __countryTemplate;
	
	static int __monarchId = 4001;	
	static int __leaderId = 2001;
	static int __regimentId = 2001;
		
	// religous groups.  map is from specific religion to group
	final static Map<String, String> __religionToGroup = new HashMap<String, String>();
	static {
		__religionToGroup.put("catholic", "christian");
		__religionToGroup.put("protestant", "christian");
		__religionToGroup.put("reformed", "christian");
		__religionToGroup.put("orthodox", "christian");
		__religionToGroup.put("sunni", "muslim");
		__religionToGroup.put("shiite", "muslim");
		__religionToGroup.put("buddhism", "eastern");
		__religionToGroup.put("hinduism", "eastern");
		__religionToGroup.put("confucianism", "eastern");
		__religionToGroup.put("shinto", "eastern");
		__religionToGroup.put("animism", "pagan");
		__religionToGroup.put("shamanism", "pagan");
	}			
	
	//we don't touch government type : by default, it is the gov of the EU3 country
	//we don't touch tech group : by default, it is the tech group of the EU3 country
	
	// set up the allowed min and max sliders by government type.  most don't have any restrictions.
	static public Map<String, int[]> __sliderMin = new HashMap<String, int[]>();
	static {
		int[] minFM = { Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, 
				Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, 
				Integer.MIN_VALUE, Integer.MIN_VALUE } ;
		minFM[Slider.CENTRALIZATION._id] = 0; // introduced in IN, but doesn't hurt anything.
		__sliderMin.put("feudal_monarchy", minFM);

		int[] minMR = { Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, 
				Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, 
				Integer.MIN_VALUE, Integer.MIN_VALUE } ;
		minMR[Slider.ARISTOCRACY._id] = 1;
		minMR[Slider.CENTRALIZATION._id] = 0; // introduced in IN, but doesn't hurt anything.
		__sliderMin.put("merchant_republic", minMR);
		
		int[] minDM = { Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, 
				Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, 
				Integer.MIN_VALUE, Integer.MIN_VALUE } ;
		minDM[Slider.CENTRALIZATION._id] = -2; // introduced in IN, but doesn't hurt anything.
		__sliderMin.put("despotic_monarchy", minDM);

		int[] minIG = { Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, 
				Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, 
				Integer.MIN_VALUE, Integer.MIN_VALUE } ;
		minIG[Slider.CENTRALIZATION._id] = -3; // introduced in IN, but doesn't hurt anything.
		__sliderMin.put("imperial_government", minIG); // of course, so was the whole imperial government.

		int[] minNR = { Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, 
				Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, 
				Integer.MIN_VALUE, Integer.MIN_VALUE } ;
		minNR[Slider.CENTRALIZATION._id] = -1; // introduced in IN, but doesn't hurt anything.
		__sliderMin.put("noble_republic", minNR);
		
		int[] minAM = { Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, 
				Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, 
				Integer.MIN_VALUE, Integer.MIN_VALUE } ;
		minAM[Slider.CENTRALIZATION._id] = -3; // introduced in IN, but doesn't hurt anything.
		__sliderMin.put("administrative_monarchy", minAM);

		int[] minAR = { Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, 
				Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, 
				Integer.MIN_VALUE, Integer.MIN_VALUE } ;
		minAR[Slider.CENTRALIZATION._id] = -3; // introduced in IN, but doesn't hurt anything.
		__sliderMin.put("administrative_republic", minAR);

		// absolute_monarchy

		// republican_dictatorship
		
		// constitutional_monarchy
		
		int[] minED = { Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, 
				Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, 
				Integer.MIN_VALUE, Integer.MIN_VALUE } ;
		minED[Slider.SERFDOM._id] = -3;
		__sliderMin.put("enlightened_despotism", minED);
		
		// constitutional_republic
		
		// bureaucratic_despotism

		int[] minTH = { Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, 
				Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, 
				Integer.MIN_VALUE, Integer.MIN_VALUE } ;
		minTH[Slider.INNOVATIVENESS._id] = 0;
		minTH[Slider.CENTRALIZATION._id] = -4; // introduced in IN, but doesn't hurt anything.
		__sliderMin.put("theocracy", minTH);
		__sliderMin.put("theocratic_government", minTH); // IN rename

		int[] minPA = { Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, 
				Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, 
				Integer.MIN_VALUE, Integer.MIN_VALUE } ;
		minPA[Slider.INNOVATIVENESS._id] = 0;
		__sliderMin.put("papacy", minPA);

		int[] minTDesp = { Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, 
				Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, 
				Integer.MIN_VALUE, Integer.MIN_VALUE } ;
		minTDesp[Slider.INNOVATIVENESS._id] = 1;
		minTDesp[Slider.CENTRALIZATION._id] = 2; // introduced in IN, but doesn't hurt anything.
		__sliderMin.put("tribal_despotism", minTDesp);

		int[] minTF = { Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, 
				Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, 
				Integer.MIN_VALUE, Integer.MIN_VALUE } ;
		minTF[Slider.CENTRALIZATION._id] = 2; // changed in IN, but doesn't hurt anything.
		__sliderMin.put("tribal_federation", minTF);

		int[] minTDemo = { Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, 
				Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, 
				Integer.MIN_VALUE, Integer.MIN_VALUE } ;
		minTDemo[Slider.ARISTOCRACY._id] = -1;
		minTDemo[Slider.CENTRALIZATION._id] = 2; // introduced in IN, but doesn't hurt anything.
		__sliderMin.put("tribal_democracy", minTDemo);

		int[] minRR = { Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, 
				Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, 
				Integer.MIN_VALUE, Integer.MIN_VALUE } ;
		minRR[Slider.ARISTOCRACY._id] = 1;
		__sliderMin.put("revolutionary_republic", minRR); // introduced in NA
		
		// revolutionary_empire
	}		    

	static public Map<String, int[]> __sliderMax = new HashMap<String, int[]>();
	static {	
		int[] maxFM = { Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, 
				Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, 
				Integer.MAX_VALUE, Integer.MAX_VALUE };
		maxFM[Slider.ARISTOCRACY._id] = -1;
		__sliderMax.put("feudal_monarchy", maxFM);

		// merchant_republic
		
		int[] maxDM = { Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, 
				Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, 
				Integer.MAX_VALUE, Integer.MAX_VALUE };
		maxDM[Slider.SERFDOM._id] = 1;
		__sliderMax.put("despotic_monarchy", maxDM);
		
		int[] maxIG = { Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, 
				Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, 
				Integer.MAX_VALUE, Integer.MAX_VALUE };
		maxIG[Slider.SERFDOM._id] = 1; // introduced in IN, but doesn't hurt anything.
		__sliderMin.put("imperial_government", maxIG); // of course, so was the whole imperial government.		

		// noble_republic
		
		// administrative_monarchy
		
		// administrative_republic
		
		int[] maxAM = { Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, 
				Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, 
				Integer.MAX_VALUE, Integer.MAX_VALUE };
		maxAM[Slider.CENTRALIZATION._id] = 1; // actually removed in IN, but hurts nothing to leave here.
		__sliderMax.put("absolute_monarchy", maxAM);
		
		// republican_dictatorship
		
		// constitutional_monarchy
		
		// enlightened_despotism
		
		// constitutional_republic
		
		// bureaucratic_despotism
		
		// theocracy
		// theocratic_government
		
		// papal_government
		
		// tribal_despotism
		
		// tribal_federation
		
		// tribal_democracy
		
		// revolutionary_republic
		
		// revolutionary_empire
	}
	
	static private String __template =
		"{government,aristocracy_plutocracy,centralization_decentralization," +
		"innovative_narrowminded,mercantilism_freetrade,offensive_defensive," +
		"land_naval,quality_quantity,serfdom_freesubjects," +
		"primary_culture,add_accepted_culture,religion,technology_group,capital}";
	

	//-5 (toward the name) to +5 (opposite of the name)
	static public enum Slider {
		ARISTOCRACY(0),
		CENTRALIZATION(1),
		INNOVATIVENESS(2),
		MERCANTILISM(3),
		OFFENSIVE(4),
		LAND(5),
		QUALITY(6),
		SERFDOM(7);
		final private int _id;
		Slider(int id) { _id=id; }
		public String toString() { return _names[_id]; }
		static private String[] _names =
		    {"aristocracy_plutocracy","centralization_decentralization",
		     "innovative_narrowminded","mercantilism_freetrade","offensive_defensive",
		     "land_naval","quality_quantity","serfdom_freesubjects"};

	};
	
	public String 	_tag;
	public String   _culture;              //compute it
	public String   _religion;             //compute it if christian, complicated if muslim
	public String   _capital;              //expected capital province
	public int[]    _sliders = new int[8]; //slider settings
	public String[] _accepted_cultures;    //compute it
	public String   _tech_group;           //compute it
	public String   _gov_type;             //compute it
	public Monarch  _monarch;              //compute it
        public Heir     _heir;                 //compute it
	public Set<Leader> _leaders = new HashSet<Leader>();               //compute it
	public String   _land_tech;				
	public String   _naval_tech;
	public String   _trade_tech;
	public String   _production_tech;
	public String   _government_tech;
	public double   _land_tech_part;			// compute it
	public double   _naval_tech_part;			// compute it
	public double   _trade_tech_part;			// compute it
	public double   _production_tech_part;	// compute it
	public double   _government_tech_part;	// compute it
	public List<String> _flags = new ArrayList<String>();
	public double   _treasury = 0.0; 
	public double	_stability = 1.0;
	public double 	_badboy;
	public double	_colonists; 
	public double	_merchants; 
	public double	_missionaries; 
	public double	_spies; 
	public double	_diplomats;
	public double   _manpower;
    public String   _infantry;
    public String   _cavalry;
    public String   _big_ship;
    public String   _galley;
    public String   _transport;	
    public double	_estimated_monthly_income;
    public String	_armyHome;
    public String	_navyHome;
    public int      _armySize;
    public int		_navySize;
    public double	_landMorale;
    public double	_navalMorale;
    public Map<String, Integer> _relations = new HashMap<String, Integer>();
    public boolean	_emperor; 
    public boolean	_elector; 
    public double	_holderTreasury = Double.MIN_VALUE; // original CK gold of the CK country's holder.
    public double   _holderPrestige = Double.MIN_VALUE; // original CK prestige of the CK country's holder.
    public double 	_prestige; // scaled EU3 prestige.
	
	protected Template getTemplate() {
		return __countryTemplate;
	}

	static public void setRootPath(String path, String alt, String complement) {
		if (__rootPath==null) {
		    __rootPath = path+complement;
		    File f = new File(__rootPath);
		    __files=f.list();
		    for (int i=0; i<__files.length; i++)
		    	__files[i] = __files[i].toLowerCase();
		}
		if (alt!=null && __altPath==null) {
			__altPath = alt+complement;
		    File f = new File(__altPath);
		    __filesAlt=f.list();
		    for (int i=0; i<__filesAlt.length; i++)
		    	__filesAlt[i] = __filesAlt[i].toLowerCase();
		}
	}

	static private String findFileName(String id) {
		String beg0 = id.toLowerCase();
		if (__filesAlt!=null)
			for (String f : __filesAlt)
				if (f.startsWith(beg0))
					return __altPath+"/"+f;
		for (String f : __files)
			if (f.startsWith(beg0))
				return __rootPath+"/"+f;
		return null;
	}
	
    static public EU3Country get(String tag) throws IOException {
    	EU3Country ret = __countryByTag.get(tag);
    	if (null == ret) {
    		ret = new EU3Country(findFileName(tag), tag);
    		__countryByTag.put(tag, ret);
    	}
    	
		return ret; 
    }
    
    /**
     * Checks that the tag exists in the base version of the game (not mods)
     * @param tag
     * @return if and how the country exists
     * @throws IOException
     */
    static public CountryExistence exists(String tag) {
		String beg0 = tag.toLowerCase();
		for (String f : __files)
			if (f.startsWith(beg0))
				return CountryExistence.EXISTS;
		if (__filesAlt!=null)
			for (String f : __filesAlt)
				if (f.startsWith(beg0))
					return CountryExistence.EXISTS_IN_MOD;
		return CountryExistence.DOESNT_EXIST;
    }    
	
	public EU3Country(String fname, String tag) throws IOException {
		super(fname,null);
		_tag = tag;
		BaseField religionF = _root.getBase("religion");
		if (null != religionF) {
			_religion = religionF.get();
		}
		if (__countryTemplate==null) {
			__countryTemplate = new Template(__template);
		}
		for (Field<?> f : _root._data)
			delDateField(f);
	}
	
	public void setSlider(Slider s, int v)   { if (v<-5) v=-5; if (v>5) v=5; _sliders[s._id]=v; }
	public void setCulture(String id)        { _culture=id; }
	public void setReligion(String id)       { if (id!=null) _religion=id; }
	public void setCapital(String v)         { _capital=v; }
	public void addCulture(String v)         { _accepted_cultures = addTo(_accepted_cultures,v); }
	public void setTechGroup(String v)       { _tech_group=v; }
	public void setGovType(String v)         { _gov_type=v; }
	
	public Integer getRelationTo(String tag) {
		return _relations.get(tag);
	}
	
	public void setRelationTo(String tag, int relation) {
		_relations.put(tag, new Integer(relation));
	}
	
	public boolean isSameReligionAs(EU3Country c2) {
		if (null == _religion) { 
			return (null == c2._religion);
		}
		
		return _religion.equals(c2._religion);
	}
	
	public boolean hasDiscovered(EU3Country c2) {
		// the question is, have we discovered the capital province of c2, and is c2 a real country?
		if (!c2.hasProvinces()) {
			 return false;
		} 
		
		// ok, pull their capital up
		EU3Province cap = EU3Province.getProvById(c2._capital);

		return cap.isDiscoveredBy(this); 
	}
	
	public boolean isAllianceWith(EU3Country c2) {
		return isRelationshipWith(c2, CvRelationships.Type.ALLIANCE);
	}
	
	public boolean isRMWith(EU3Country c2) {
		return isRelationshipWith(c2, CvRelationships.Type.MARRIAGE);
	}

	public boolean isUnionWith(EU3Country c2) {
		return isRelationshipWith(c2, CvRelationships.Type.UNION);
	}
	
	public boolean isVassalWith(EU3Country c2) {
		return isRelationshipWith(c2, CvRelationships.Type.VASSAL);
	}	
	
	public boolean isWarWith(EU3Country c2) {
		for (EU3Wars war: EU3Wars.__list) {
			if ((war._attackers.contains(_tag) && war._defenders.contains(c2._tag)) ||
				(war._attackers.contains(_tag) && war._defenders.contains(c2._tag))) {
				return true;
			}				
		}
		
		return false;
	}
	
	public boolean isRelationshipWith(EU3Country c2, CvRelationships.Type type) {
		for (CvRelationships rel: CvRelationships.__eu3List) {
			if (rel._firstTag.equals(_tag) && rel._secondTag.equals(c2._tag)) {
				if (rel._type == type) {
					return true;
				}
			} else if (rel._firstTag.equals(c2._tag) && rel._secondTag.equals(_tag)) {				
				if (rel._type == type) {
					return true;
				}
			}
		}		
		
		return false;		
	}
	
	public boolean isChristian() {
		if (null == _religion) {
			return false;
		}
		
		boolean ret = (_religion.equals("catholic")) || 
			(_religion.equals("protestant")) ||
			(_religion.equals("reformed"));
		
		return ret;			
	}
		
	public void setEmperor(boolean argEmperor) {
		_emperor = argEmperor;
	}	
	
	public boolean isEmperor() {
		return _emperor;
	}	

	public void setElector(boolean argElector) {
		_elector = argElector;
	}	
	
	public boolean isElector() {
		return _elector;
	}	
	
	public double getHolderTreasury() {
		return _holderTreasury;
	}

	public void setHolderTreasury(double treasury) {
		_holderTreasury = treasury;
	}

	public double getHolderPrestige() {
		return _holderPrestige;
	}

	public void setHolderPrestige(double prestige) {
		_holderPrestige = prestige;
	}

	public double getPrestige() {
		return _prestige;
	}

	public void setPrestige(double prestige) {
		this._prestige = prestige;
	}

	public boolean isSameReligiousGroupAs(EU3Country c2) {
		if (null == _religion) {
			return false;			
		}
		
		String group = __religionToGroup.get(_religion);
		if (null == group) {
			System.out.println("wtf religion is: " + _religion + " (no group found)");
			group = "";
		}

		String group2 = __religionToGroup.get(c2._religion);
		if (null == group2) {
			System.out.println("wtf religion is: " + c2._religion + " (no group found)");
			group2 = "";
		}
		
		return group.equals(group2);
	}

	public void applyChanges() {
		if (null != _gov_type) {
			applyBase("government",_gov_type);
		}
		applyBase("capital",_capital);
		if (null != _religion) {
			applyBase("religion",_religion);
		}
		applyBase("primary_culture",_culture);
		applyBase("technology_group",_tech_group);  //eastern western
		if (_tech_group.equals("western") || _tech_group.equals("eastern") ||
				   _tech_group.equals("indian") || _tech_group.equals("chinese")) {  
			_land_tech = "3";				
			_naval_tech = "3";
			_trade_tech = "3";
			_production_tech = "3";
			_government_tech = "3";
		} else if (_tech_group.equals("muslim") || _tech_group.equals("ottoman")) {
			_land_tech = "6";				
			_naval_tech = "6";
			_trade_tech = "6";
			_production_tech = "6";
			_government_tech = "6";					
		} else if (_tech_group.equals("sub_saharan")) {
			_land_tech = "1";				
			_naval_tech = "1";
			_trade_tech = "1";
			_production_tech = "1";
			_government_tech = "1";	
		} else if (_tech_group.equals("new_world")) {
			_land_tech = "0";				
			_naval_tech = "0";
			_trade_tech = "0";
			_production_tech = "0";
			_government_tech = "0";					
		}
		applyBaseList("add_accepted_culture",_accepted_cultures);
		for (Slider s : Slider.values()) {
			applyBase(s.toString(),_sliders[s._id]);
		}
		if (_monarch != null) applyIndependentField(_monarch.make());
		for (Leader leader: _leaders) {
			applyIndependentField(leader.make());
		}
		
		calcEtc();
	}
	
	public void takeEU3Defaults() {
		BaseField b = _root.getBase("government"); 
		_gov_type = (b != null) ? b.get() : null;
		b = _root.getBase("technology_group");
		_tech_group = (b != null) ? b.get() : "new_world";
		b = _root.getBase("religion");
		_religion = (b != null) ? b.get() : "shamanism";
		b = _root.getBase("primary_culture");
		_culture = (b != null) ? b.get() : "huron";
		b = _root.getBase("capital");
		_capital = (b != null) ? b.get() : "988";

		b = _root.getBase("aristocracy_plutocracy");
		_sliders[Slider.ARISTOCRACY._id] = (b != null) ? b.getAsInt() : 0;
		b = _root.getBase("centralization_decentralization");
		_sliders[Slider.CENTRALIZATION._id] = (b != null) ? b.getAsInt() : 0;
		b = _root.getBase("innovative_narrowminded");
		_sliders[Slider.INNOVATIVENESS._id] = (b != null) ? b.getAsInt() : 0;
		b = _root.getBase("mercantilism_freetrade");
		_sliders[Slider.MERCANTILISM._id] = (b != null) ? b.getAsInt() : 0;
		b = _root.getBase("offensive_defensive");
		_sliders[Slider.OFFENSIVE._id] = (b != null) ? b.getAsInt() : 0;
		b = _root.getBase("land_naval");
		_sliders[Slider.LAND._id] = (b != null) ? b.getAsInt() : 0;
		b = _root.getBase("quality_quantity");
		_sliders[Slider.QUALITY._id] = (b != null) ? b.getAsInt() : 0;
		b = _root.getBase("serfdom_freesubjects");
		_sliders[Slider.SERFDOM._id] = (b != null) ? b.getAsInt() : 0;				

		BaseField[] list = _root.getAllBase("add_accepted_culture");
		if ((null != list) && (list.length > 0)) {
			_accepted_cultures = new String[list.length];
			for (int i = 0; i < list.length; i++) {
				_accepted_cultures[i] = list[i].get();
			}
		}
		
		// searching for the monarchs and leaders in this truly sucks.
		// all of those fields are 1452.1.1 style.  The best I can hope for, I guess,
		// is that they all have 2 periods.
		for (int i = 0; i < _root.size(); i++) {
			Field<?> f = _root.get(i);
			String name = f.name();
			int first = name.indexOf('.');
			int last = name.lastIndexOf('.');
			if ((-1 != first) && (-1 != last) && (first != last)) {
				// 2 discrete periods, probably a monarch or something.  see if we 
				// can parse the date pieces.
				short year = 1970;
				byte month = 1;
				byte day = 1;
				try {
					year = Short.parseShort(name.substring(0, first));
					month = Byte.parseByte(name.substring(first + 1, last));
					day = Byte.parseByte(name.substring(last + 1));
					if (((year < OptionSection.getStartYear())) || 										
					    ((year == OptionSection.getStartYear()) && 
					     (month < OptionSection.getStartMonth()))) {
						// ooh, and it was in the period as well.
						// likely to be a struct field here (has the { } pieces)
						if (f instanceof StructField) {
							StructField sf = (StructField) f;
							StructField sf2 = sf.getStruct("monarch");
							if (null != sf2) {
								String mName = sf2.getBase("name").get();
								BaseField dipF = sf2.getBase("DIP");
								if (null == dipF) dipF = sf2.getBase("dip");
								BaseField milF = sf2.getBase("MIL");
								if (null == milF) milF = sf2.getBase("mil");
								BaseField admF = sf2.getBase("ADM");
								if (null == admF) dipF = sf2.getBase("adm");
								BaseField regentF = sf2.getBase("regent");
								boolean regent = (null != regentF);
								byte dip = (null != dipF) ? Byte.parseByte(dipF.get()) : 3;
								byte mil = (null != milF) ? Byte.parseByte(milF.get()) : 3;
								byte adm = (null != admF) ? Byte.parseByte(admF.get()) : 3;
								_monarch = new Monarch(year, month, day, mName, null, dip, mil, adm, regent);
							}
							sf2 = sf.getStruct("leader");
							if (null != sf2) {
								String type = sf2.getBase("type").get();
								String lName = sf2.getBase("name").get();
								byte rank = Byte.parseByte(sf2.getBase("rank").get());
								byte fire = Byte.parseByte(sf2.getBase("fire").get());
								byte shock = Byte.parseByte(sf2.getBase("shock").get());
								byte manuever = Byte.parseByte(sf2.getBase("manuever").get());
								byte siege = Byte.parseByte(sf2.getBase("siege").get());
								String death_date = null;
								BaseField dd = sf2.getBase("death_date");
								if (null != dd) {
									death_date = dd.get();
								}
								Leader leader = new Leader(type, year, month, day, death_date, rank, lName, fire, shock, manuever, siege);
								_leaders.add(leader);
							}
							
							// check for set flags.
							BaseField[] setFlags = sf.getAllBase("set_country_flag");
							if (null != setFlags) {				
								for (BaseField flag: setFlags) {
									_flags.add(flag.get());
								}
							}
							
							// I am assuming that these are found in order, so that 
							// any clears will be effective to remove any previous sets.
							// this is moot in general, because there are no clear flags
							// prior to the start date anyway.
							BaseField[] clrFlags = sf.getAllBase("clr_country_flag");
							if (null != clrFlags) {				
								for (BaseField flag: clrFlags) {
									_flags.remove(flag.get());
								}
							}							
						} else {
							System.out.println("wtf: non-struct: " + f);
						}
					}
				} catch (NumberFormatException nfe) {
					// guess it really wasn't a monarch or a leader.
				}
			}
		}
	}
	
	protected void calcEtc() {
		sanityCheckSliders();
		
		// do a quick guesstimate on the research costs and on the 
		// annual income.				
		// figure out the bonuses and penalties.
		// sliders, religion, and then cross-religion or cross-culture penalties.
		double bonus = _sliders[Slider.CENTRALIZATION._id] * -0.01;
		if (null == _religion) { } 			
		else if (_religion.equals("catholic")) {  }
		else if (_religion.equals("protestant")) { bonus += 0.1; }
		else if (_religion.equals("reformed")) { bonus += -0.1; }
		else if (_religion.equals("orthodox")) { }
		else if (_religion.equals("sunni")) { }
		else if (_religion.equals("shiite")) { bonus += -0.2; }
		else if (_religion.equals("buddhism")) { bonus += -0.2; }
		else if (_religion.equals("hinduism")) { bonus += 0.05; }
		else if (_religion.equals("confucianism")) { bonus += -0.5; }
		else if (_religion.equals("shinto")) { bonus += -0.2; }
		else if (_religion.equals("animism")) { }
		else if (_religion.equals("shamanism")) { }
		else { System.out.println("wtf religion is: " + _religion); }		
		bonus += _stability * 0.1; // (-30% to 30%)
		
		// find all the provinces owned by this tag and their base tax.
		Set<EU3Province> provsOwned = EU3Province.getProvsByTag(_tag);
		double base = 0.0;
		int count = 0;
		int cots = 0;
		int manpower = 0;
		int coastalTax = 0;
/*		if (_tag.equals("TIM")) {
			System.out.println();
		}*/
		if (null != provsOwned) {
			int provRawCount = provsOwned.size();
			float provRawBase = 0;
			float provEU3Base = 0;
			float provCKBase = 0;
			int provRawMP = 0;
			int provEU3MP = 0;
			int provRawPop = 0;
			int provEU3Pop = 0;
			for (EU3Province prov: provsOwned) {
				if (prov.isPort()) {
					coastalTax += prov._base_tax;
					if (prov.isCot()) {
						coastalTax += 10;
					}
				}
				provRawBase += prov._base_tax;
				provEU3Base += prov.getEU3DefaultBaseTax();
				provCKBase += prov.getCKDefaultBaseTax();
				provRawMP += prov._manpower;
				provEU3MP += prov.getEU3DefaultMP();
				provRawPop += prov._citysize;
				provEU3Pop += prov.getEU3DefaultCitySize();

				double provBonus = bonus;
				double provBase = prov._base_tax;
				
				/*
				// religion and culture modifiers don't seem to apply to the annual income guesstimate.
				if (!_religion.equals(prov._religion)) {
					provBonus += -0.3;
				}
				provBonus += getCultureBonus(prov._culture);
				*/
				
				if (prov.isCot()) {
					provBase += 10; 					
				}
				if (prov.hasBuilding("workshop")) {
					provBase += 2; 
				}
				if (prov.hasBuilding("constable")) {
					provBonus += 0.5;					
				}
				if (prov.hasBuilding("customs_house")) {
					provBonus += 0.05;					
				}
				base += provBase * (1.0 + provBonus);
				
				count++;
				if (prov.isCot()) { cots++; }
				
				double provMPBonus = 0.0;
				if (!isAccepted(prov._culture)) {
					provMPBonus = -0.5;
				}
				manpower += (prov._manpower * 125.0) * (1 + provMPBonus) * (1 + (prov._citysize / 200000.0));
			}	
			//System.out.println("TAG,PROV,BASE,EU3T,CKT,MP,EU3MP,POP,EU3POP");
			if ((provRawBase != provEU3Base) || (provRawMP != provEU3MP)) {
				System.out.format("%s,%03d,%03d,%03d,%03d,%02d,%02d,%d,%d\n", _tag, provRawCount, Math.round(provRawBase), Math.round(provEU3Base), Math.round(provCKBase), provRawMP, provEU3MP, provRawPop, provEU3Pop);
			}
		}
		_estimated_monthly_income = base * 3.5 / 10.0; // this is really an estimate.  I ignored trade tariffs.
		
		// if we already have a treasury, convert it 
		// by gold / 1000;
		if (Double.MIN_VALUE == _holderTreasury) {
			// we don't have one yet, so default to base * 2.
			_treasury = base * 2;
		} else {
			// divide CK treasury by 1000
			_treasury = _holderTreasury / 1000;
		}
		_treasury = Math.max(50, _treasury);

		// scale badboy from CK to EU3... actually, after looking at it, this doesn't seem necessary.
		// the dishonorable scum level seems to kick in the same in both games.
		_badboy = _badboy * 1.0;
				
		// figure out the cost for the next tech level (1 except for trade, which is 2)
		// except in in, much higher.
		/*
		province cost * 
		(tech date - previous tech date) * early research penalty * 
		((100 + centralization + innovative + scientific method + defender of faith + luck)/100) / tech group * (1 + inflation/100)
		
		most of this is set for us.  tech date is usually before start in any case, so 
		should be no penalty.
		there are no defenders of the faith,  no scientific methods, no luck, and no inflation.	
		*/
		double researchProvCost = 0.0;
		if (count > __maxProvCount) {
			researchProvCost = __maxResearchCount;			
		} else if (count >= __provResearchCost.length) {			
			researchProvCost = __provResearchCost[__provResearchCost.length - 1] + 
			__provResearchCostExtra * (count - __provResearchCost.length  + 1);
		} else {
			researchProvCost = __provResearchCost[count];
		}
		double sliderModifier = (100.0 + _sliders[Slider.CENTRALIZATION._id] + _sliders[Slider.INNOVATIVENESS._id])  / 100.0;
		double techGroupModifier = 1.0;
		if      (_tech_group.equals("western")) { techGroupModifier = 1.0; }
		else if (_tech_group.equals("eastern")) { techGroupModifier = 0.9; }
		else if (_tech_group.equals("ottoman")) { techGroupModifier = 0.85; }
		else if (_tech_group.equals("muslim")) { techGroupModifier = 0.8; }
		else if (_tech_group.equals("indian")) { techGroupModifier = 0.5; }
		else if (_tech_group.equals("chinese")) { techGroupModifier = 0.4; }
		else if (_tech_group.equals("sub_saharan")) { techGroupModifier = 0.2; }
		else if (_tech_group.equals("new_world")) { techGroupModifier = 0.1; }		
		
		double finalNonYearCost = researchProvCost * sliderModifier / techGroupModifier;
				
		// in IN, set these to 10% of cost. else, 		
		// set these at 25% of prod/land/naval, 5% of trade, 65% of govt.
		
		// 10 is an abstraction here.
		double govCost = 10 * finalNonYearCost;
		double tradeCost = 10 * finalNonYearCost;
		double prodLandNavalCost = 10 * finalNonYearCost;
		
		_land_tech_part = prodLandNavalCost * 0.10;
		_naval_tech_part = prodLandNavalCost * 0.10;
		_production_tech_part = prodLandNavalCost * 0.10;
		_trade_tech_part = tradeCost * 0.10;
		_government_tech_part = govCost * 0.10;			
		
		// figure out the colonists.  what would be the yearly gain?
		double colonists = 0.0;

		if (null == _religion) {			 
		} else if (_religion.equals("catholic")) {
			colonists = 1.0;
		} else if (_religion.equals("protestant")) { 
			colonists = 0.5;
		} else if (_religion.equals("reformed")) { 
			colonists = 1.0;
		} else if (_religion.equals("orthodox")) { 
			colonists = 0.5;
		} else if (_religion.equals("sunni")) {  
		} else if (_religion.equals("shiite")) {  
		} else if (_religion.equals("buddhism")) {  
		} else if (_religion.equals("hinduism")) {  
		} else if (_religion.equals("confucianism")) {  
		} else if (_religion.equals("shinto")) {  
		} else if (_religion.equals("animism")) {  
		} else if (_religion.equals("shamanism")) {  
		} else { System.out.println("wtf religion is: " + _religion); }

		_colonists = Math.min(6, Math.max(0, colonists * 13.0 / 12.0)); // strange but seems to be accurate.
					
		// figure out the merchants.  what would be the yearly gain?
		double merchants = _stability + 2.0 + cots; 
		merchants += (_sliders[Slider.MERCANTILISM._id] * 0.4);
		_merchants = Math.min(12, Math.max(0, merchants * 13.0 / 12.0));
		
		// missionaries
		double missionaries = 0.0;
		missionaries += (_sliders[Slider.INNOVATIVENESS._id] * 0.4);
		_missionaries = Math.min(6, Math.max(0, missionaries * 13.0 / 12.0));
		
		// spies
		double spies = 0.0;
		spies += Math.max(_sliders[Slider.ARISTOCRACY._id] * 0.2, 0) +
				 Math.max(_sliders[Slider.MERCANTILISM._id] * -0.2, 0);
		_spies = Math.min(6, Math.max(0, spies * 13.0 / 12.0));
		
		// diplomats
		double diplomats = 1.0;
		diplomats += (_sliders[Slider.ARISTOCRACY._id] * -0.4);
		if (null == _religion) { }
		else if (_religion.equals("catholic")) { diplomats += 2.0; }
		else if (_religion.equals("protestant")) { diplomats += 1.0; }
		else if (_religion.equals("reformed")) { diplomats += 1.0; }
		else if (_religion.equals("orthodox")) { diplomats += 1.0; }
		else if (_religion.equals("sunni")) {  }
		else if (_religion.equals("shiite")) {  }
		else if (_religion.equals("buddhism")) {  }
		else if (_religion.equals("hinduism")) {  }
		else if (_religion.equals("confucianism")) {  }
		else if (_religion.equals("shinto")) {  }
		else if (_religion.equals("animism")) {  }
		else if (_religion.equals("shamanism")) {  }
		else { System.out.println("wtf religion is: " + _religion); }
		_diplomats = Math.min(6, Math.max(0, diplomats)); // no extra 1/12 for some reason.
		
		double mpBonus = (_sliders[Slider.LAND._id] * -0.05) +
					     (_sliders[Slider.QUALITY._id] * 0.025);
		if (null == _gov_type) { } 
		else if (_gov_type.equals("feudal_monarchy")) { mpBonus += 0.15; } 
		else if (_gov_type.equals("tribal_despotism")) { mpBonus += 0.20; } 
		else if (_gov_type.equals("revolutionary_empire")) { mpBonus += 0.25; } 
		_manpower = (manpower * (1.0 + mpBonus)) / 1000;
		_manpower = Math.max(0.616, _manpower); // not sure why this is the minimum.
		
		if (_manpower == 0.0) { _manpower = 1.000; } // USA / REB / etc.
		_armySize = Math.max(1, (int) (_manpower * 1.0 / 2.0)); // 50% of manpower, minimum of 1.
		_navySize = Math.max(1, (int) (_manpower * 3.0 / 2.0)); // 150% of manpower, minimum of 1;
		int navalForceLimits = coastalTax / 4; // rough estimate of IN force limit size.
		
		// and, everybody starts with the level 1 units.
		// while they historically may want a different unit (ottoman_yaya vs mamluk_archer, say)
		// I'm not worrying with it.
		if (_tech_group.equals("western")) {
		    _infantry = "western_medieval_infantry";
		    _cavalry = "western_medieval_knights";
		    _big_ship = "carrack";
		    _galley = "galley";
		    _transport = "cog";	
		    _navySize = navalForceLimits / 2;
		}
		else if (_tech_group.equals("eastern")) { 
			_infantry = "eastern_medieval_infantry";
			_cavalry = "eastern_knights";
			_big_ship = "carrack";
			_galley = "galley";
			_transport = "cog";		
		    _navySize = navalForceLimits / 2;			
		}
		else if (_tech_group.equals("ottoman")) {
			_infantry = "ottoman_azab";
			_cavalry = "ottoman_musellem";
			_big_ship = "carrack";
			_galley = "galley";
			_transport = "cog";		
		    _navySize = navalForceLimits / 2;
		}
		else if (_tech_group.equals("muslim")) { 
			_infantry = "mamluk_archer";
			_cavalry = "mamluk_cavalry_charge";
			_big_ship = "carrack";
			_galley = "galley";
			_transport = "cog";		
		    _navySize = navalForceLimits / 2;			
		}
		else if (_tech_group.equals("indian")) { 
		    _infantry = "indian_footsoldier";
		    _cavalry = "rajput_hill_fighters";			
			_big_ship = "carrack";
			_galley = "galley";
			_transport = "cog";	
		    _navySize = navalForceLimits * 3 / 2;  // larger but only galleys.
		}
		else if (_tech_group.equals("chinese")) { 
		    _infantry = "east_asian_spearmen";
		    _cavalry = "eastern_bow";
			_big_ship = "carrack";
			_galley = "galley";
			_transport = "cog";			
		    _navySize = navalForceLimits * 3 / 2;  // larger but only galleys
		}
		else if (_tech_group.equals("sub_saharan")) {
		    _infantry = "african_spearmen";
		    _cavalry = null; // can't build cavalry
		    // I don't know if this will mean that players in sub_saharan tech groups
		    // can build ships now, but I do know that these are all over the save file 
			_big_ship = "carrack";
			_galley = "galley";
			_transport = "cog";	
			_navySize = 0; // no navy
		}
		else if (_tech_group.equals("new_world")) { 
			_infantry = "south_american_spearmen";
			_cavalry = null; // can't build cavalry
		    // I don't know if this will mean that players in sub_saharan tech groups
		    // can build ships now, but I do know that these are all over the save file 
			_big_ship = "carrack";
			_galley = "galley";
			_transport = "cog";	
			_navySize = 0; // no navy
		}	
		
		// see where we could drop an army.  try the capital first, and then search for any controlled
		// provinces.		
		EU3Province cap = EU3Province.getProvById(_capital);
		if ((null != cap) && (_tag.equals(cap.getController()))) { 
			_armyHome = _capital;			
		} else if (null != provsOwned) {
			for (EU3Province prov: provsOwned) {
				if ((null != prov) && (_tag.equals(prov.getController()))) {
					_armyHome = prov._id;
					break;
				}
			}
		}
		
		// see if we can drop a navy as well.
		if (_navySize > 0) {
			// ...any ports? (try the capital first here as well)
			if ((null != cap) && (cap.isPort()) && (_tag.equals(cap.getController()))) { 
				_navyHome = _capital;			
			} else if (null != provsOwned) {			
				for (EU3Province prov: provsOwned) {
					if ((null != prov) && (prov.isPort()) && (_tag.equals(prov.getController()))) {
						_navyHome = prov._id;
						break;
					}
				}
			}		
		} else {
			_navyHome = null;
		}		
		
		// what should be the morale for these troops?
		// 2 (land maintenance) + 0.1 * monarch mil + slider factors.
		double landMorale = 2.00;
		if (null != _monarch) { landMorale += 0.1 * _monarch._mil; }
		landMorale += (_sliders[Slider.OFFENSIVE._id] * -0.02) + 
				  Math.max(_sliders[Slider.LAND._id] * -0.02, 0) + 
				  (_sliders[Slider.SERFDOM._id] * 0.01);
		if (null == _gov_type) { } 
		else if (_gov_type.equals("republican_dictatorship")) { landMorale += 0.1; }
		else if (_gov_type.equals("revolutionary_republic")) { landMorale += 0.5; }
		if (null == _religion) { } 
		else if (_religion.equals("shiite")) { landMorale += 0.5; }
		else if (_religion.equals("shinto")) { landMorale += 0.5; }				
		_landMorale = landMorale;

		double navalMorale = 2.00;
		if (null != _monarch) { navalMorale += 0.1 * _monarch._mil; }
		navalMorale += Math.max(_sliders[Slider.LAND._id] * 0.02, 0); 
		if (null == _gov_type) { } 
		else if (_gov_type.equals("republican_dictatorship")) {
			navalMorale += 0.1;
		} 				
		_navalMorale = navalMorale;
	}
		
	public void sanityCheckSliders() {
		if (null == _gov_type) {
			return;
		}
		
		// clamp the sliders to those that won't cause revolt risk.
		int[] sliderMin = __sliderMin.get(_gov_type);
		if (null != sliderMin) {
			for (int i = 0; i < _sliders.length; i++) {
				if (sliderMin[i] > _sliders[i]) {
					_sliders[i] = sliderMin[i];
				}
			}
		}

		int[] sliderMax = __sliderMax.get(_gov_type);
		if (null != sliderMax) {
			for (int i = 0; i < _sliders.length; i++) {
				if (sliderMax[i] < _sliders[i]) {
					_sliders[i] = sliderMax[i];
				}
			}
		}
	}
	
	public void write (String out) throws IOException {
		applyChanges();
		PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(out, true), "ISO-8859-1"));
		pw.println(_tag + "={");
	    pw.println("    history={");
	    if (null != _gov_type) {
	    	pw.println("        government=" + _gov_type);
	    }
        for (int i = 0; i < 8; i++) {
        	pw.println("        " + Slider._names[i] + "=" + _sliders[i]);
        }
        if (isElector()) {
            pw.println("        elector=yes");        	
        }        
        pw.println("        technology_group=" + _tech_group);
        if (null != _religion) {
        	pw.println("        religion=" + _religion);
        }
        if (isEmperor()) {
        	pw.println("        emperor=yes");
        }
        pw.println("        primary_culture=" + _culture);
        if (null != _accepted_cultures) {
	        for (String accepted: _accepted_cultures) {
	        	if (null != accepted) pw.println("        add_accepted_culture=" + accepted);
	        }
        }
        pw.println("        capital=" + _capital);
        
        // write our history entries, monarch, leader, flags, tech
    	if (null != _monarch) {
    		_monarch.write(pw, true);
    	}
    	for (Leader leader: _leaders) {
    		leader.write(pw, true);
    	}
        
        // any flags.
        if (_flags.size() > 0) {
            pw.println("        " + OptionSection.getStartDate() + "={");
            for (String flag: _flags) {
            	pw.println("            set_country_flag=\"" + flag + "\""); 
            }
            pw.println("        }");
        }
        
        // now for tech, such as it is.         
        pw.println("        " + OptionSection.getStartDate() + "={");
        pw.println("            land_tech=" + _land_tech);
        pw.println("            naval_tech=" + _naval_tech);
        pw.println("            trade_tech=" + _trade_tech);
        pw.println("            production_tech=" + _production_tech);
        pw.println("            government_tech=" + _government_tech);
        pw.println("        }");
        pw.println("    }");
        
        // flags again.  now out of the history section.
        pw.println("    flags={");        
        if (_flags.size() > 0) {
            for (String flag: _flags) {
            	pw.println("        " + flag + "=yes"); 
            }
        }        
        pw.println("    }");
        
        // now to basically redo everything else we said in the history.
        pw.println("    capital=" + _capital);
        pw.println("    primary_culture=" + _culture);
        if (null != _accepted_cultures) {
	        for (String accepted: _accepted_cultures) {
	        	if (null != accepted) pw.println("    accepted_culture=" + accepted);
	        }
        }        
        if (null != _religion) {
        	pw.println("    religion=" + _religion);
        }
        pw.println("    technology_group=" + _tech_group);        
        pw.println("    technology={");        
        pw.println("        land_tech={" + _land_tech + " " + __df.format(_land_tech_part) + "}");
        pw.println("        naval_tech={" + _naval_tech + " " + __df.format(_naval_tech_part) + "}");
        pw.println("        trade_tech={" + _trade_tech + " " + __df.format(_trade_tech_part) + "}");
        pw.println("        production_tech={" + _production_tech + " " + __df.format(_production_tech_part) + "}");
        pw.println("        government_tech={" + _government_tech + " " + __df.format(_government_tech_part) + "}");
        pw.println("    }");    
        if (isElector()) {
            pw.println("    elector=yes");        	
        }        
        pw.println("    auto_send_merchants=yes");
        pw.println("    prestige=" + __df.format(_prestige));
        pw.println("    stability=" + __df.format(_stability));
        pw.println("    stability_investment=0.000");
        pw.println("    treasury=" + __df.format(_treasury));
        pw.println("    current_income=0.000");
        pw.println("    estimated_monthly_income=" + __df.format(_estimated_monthly_income));
        pw.println("    inflation=0.000");
        pw.println("    last_bankrupt=\"1.1.1\"");
        pw.println("    wartax=\"1.1.1\"");
        pw.println("    war_exhaustion=0.000");
        pw.println("    land_maintenance=1.000");
        pw.println("    naval_maintenance=1.000");
        pw.println("    army_tradition=0.000");
        pw.println("    navy_tradition=0.000");
        pw.println("    cultural_tradition=0.200");
//		EU3Header.writeHeader("EU3DistributionHeader.txt", pw); 
        pw.println("    cancelled_loans=0");
        pw.println("    loan_size=6");
        pw.println("    badboy=" + __df.format(_badboy));
        if (null != _gov_type) {
        	pw.println("    government=" + _gov_type);
        }
        pw.println("    colonists=" + __df.format(_colonists));
        pw.println("    merchants=" + __df.format(_merchants));
        pw.println("    missionaries=" + __df.format(_missionaries));
        pw.println("    spies=" + __df.format(_spies));
        pw.println("    diplomats=" + __df.format(_diplomats));
        pw.println("    last_policy_change=\"1.1.1\"");
        for (int i = 0; i < 8; i++) {
        	pw.println("    " + Slider._names[i] + "=" + _sliders[i]);
        }
        pw.println("    manpower=" + __df.format(_manpower));
	    pw.println("    infantry=\"" + _infantry + "\"");
	    if (null != _cavalry) { pw.println("    cavalry=\"" + _cavalry + "\""); }
	    pw.println("    big_ship=\"" + _big_ship + "\"");
	    pw.println("    galley=\"" + _galley + "\"");
	    pw.println("    transport=\"" + _transport + "\"");
        
	    // ok, for fun, let's drop an army in the capital (or somewhere we control);  
	    // make the army be 50% of manpower, and 1/3 cavalry if cavalry exists.
	    // giving them generic names, obviously.
	    if (null != _armyHome) {
	    	EU3Province prov = EU3Province.getProvById(_armyHome);
	    	String nameBase = null;
	    	if (null != prov) {
	    		nameBase = prov._name;
	    	} else {
	    		if (null != _monarch) {
	    			nameBase = _monarch._name;
	    		} else {
	    			nameBase = _tag;
	    		}
	    	}
	    	
	    	pw.println("    army={");
	    	pw.println("        id={");
	    	pw.println("           id=" + __regimentId++);
	    	pw.println("           idtype=43");
	    	pw.println("        }");
	    	pw.println();
	    	pw.println("        name=\"1st Army\"");
	    	pw.println("        movement_progress=0.000");
	    	pw.println("        location=" + _armyHome);
	    	for (int i = 1; i <= _armySize; i++) {
	    		pw.println("        regiment={");
	    		pw.println("            id={");
	    		pw.println("                id=" + __regimentId++);	    		
	    		pw.println("                idtype=43");	    		
	    		pw.println("            }");
	    		pw.println();
	    		pw.println("            name=\"" + nameBase + "'s " + i + getOrdinal(i) + " Regiment\"");
	    		pw.println("            home=" + _armyHome);
	    		if ((0 == (i % 3)) && (null != _cavalry)) {
	    			// every 3rd regiment is cavalry for those that can build it
		    		pw.println("            type=\"" + _cavalry + "\"");
	    		} else {
		    		pw.println("            type=\"" + _infantry + "\"");
	    		}
	    		pw.println("            morale=" + __df.format(_landMorale));
	    		pw.println("            strength=1.000");	    	
	    		pw.println("        }");
	    	}
    		pw.println("        target=0");
    		pw.println("        staging_province=0");	        	    	
	    	pw.println("    }");
	    }

	    // ok, for fun, let's drop a navy in the capital (or somewhere we control);  
	    // make the navy be 150% of manpower, = 0.25 * (sum of base tax in coastal cities)  
	    // and 1/3 transport, 1/3 big ship, 1/3 galley
	    // giving them generic names, obviously.
	    if (null != _navyHome) {
	    	EU3Province prov = EU3Province.getProvById(_armyHome);
	    	String nameBase = null;
	    	if (null != prov) {
	    		nameBase = prov._name;
	    	} else {
	    		if (null != _monarch) {
	    			nameBase = _monarch._name;
	    		} else {
	    			nameBase = _tag;
	    		}
	    	}
	    	
	    	pw.println("    navy={");
	    	pw.println("        id={");
	    	pw.println("           id=" + __regimentId++);
	    	pw.println("           idtype=43");
	    	pw.println("        }");
	    	pw.println();
	    	pw.println("        name=\"1st Navy\"");
	    	pw.println("        movement_progress=0.000");
	    	pw.println("        location=" + _navyHome);
	    	boolean bigShips = _tech_group.equals("western") ||
	    		_tech_group.equals("eastern") ||
	    		_tech_group.equals("ottoman") ||
	    		_tech_group.equals("muslim");
	    	for (int i = 1; i <= _navySize; i++) {
	    		pw.println("        ship={");
	    		pw.println("            id={");
	    		pw.println("                id=" + __regimentId++);	    		
	    		pw.println("                idtype=43");	    		
	    		pw.println("            }");
	    		pw.println();
	    		String shipType = "";
	    		if (bigShips) {
		    		if (0 == (i % 2)) {
		    			shipType = _big_ship;
		    		} else {
		    			shipType = _transport;
		    		}
	    		} else {
		    		if ((i % 4) < 3) {
		    			shipType = _galley;
		    		} else {
		    			shipType = _transport;
		    		}	    			
	    		}
	    		pw.println("            name=\"" + nameBase + "'s " + i + getOrdinal(i) + " " + shipType + "\"");
	    		pw.println("            home=" + _armyHome);
	    		pw.println("            type=\"" + shipType + "\"");
	    		pw.println("            morale=" + __df.format(_navalMorale));
	    		pw.println("            strength=1.000");	    	
	    		pw.println("        }");
	    	}
    		pw.println("        at_sea=0");
	    	pw.println("    }");
	    }
	    
	    /*
	    if ("ARA".equals(_tag)) {
	    	System.out.println("ARA");
		    for (EU3Country eu3c : CvCountry.__eu3List) {
		    	Integer rel = getRelationTo(eu3c._tag);
		    	if (null != rel) {
		    		System.out.println("    " + eu3c._tag + "={");
		    		System.out.println("        value=" + rel);
		    		System.out.println("    }");
		    	}
		    }
	    }
	    */
	    for (EU3Country eu3c : CvCountry.__eu3List) {
	    	Integer rel = getRelationTo(eu3c._tag);
	    	if (null != rel) {
	    		pw.println("    " + eu3c._tag + "={");
		    	pw.println("        value=" + rel);
		    	pw.println("    }");
	    	}
	    }
	    
	    for (Leader leader: _leaders) {
	    	pw.println("    leader={");
	    	pw.println("        id=" + leader._id);
	    	pw.println("        idtype=38");
	    	pw.println("    }");	    	
	    }
	    if (null != _monarch) {
	    	pw.println("    monarch={");
	    	pw.println("        id=" + _monarch._id);
	    	pw.println("        idtype=37");
	    	pw.println("    }");
	    }
	    
	    pw.println("}");
	    	    
        pw.close();
    }	
	
	public boolean hasProvinces() {
		boolean ret = (null != EU3Province.getProvsByTag(_tag));
		
		return ret;
	}

	public boolean isPrimary(String culture) {
		if ((null == _culture) || (null == culture)) {
			return false;
		}

		return _culture.equals(culture);
	}
	
	public String getOrdinal(int num) {
		if (num < 1) {
			return "th";
		}
		if (num == 1) {
			return "st";
		}	
		if ((num > 10) && (num < 20)) {
			return "th";
		}
		
		int digit = num % 10;
		if (digit == 1) {
			return "st";
		}
		if (digit == 2) {
			return "nd";
		}
		if (digit == 3) {
			return "rd";
		}
		
		return "th";
	}
	
	public boolean isAccepted(String culture) {
		if ((null == _culture) || (null == culture)) {
			return false;
		}
		
		EU3Culture mine = EU3Culture.search(_culture);
		EU3Culture other = EU3Culture.search(culture);
		
		String myGroup = mine._group;		
		if (null == myGroup) { System.out.println("wtf: no group found for: " + _culture); }
		String group = other._group;
		if (null == group) { System.out.println("wtf: no group found for: " + culture); }
		if (myGroup.equals(group)) {
			return true;
		}
		
        if (null != _accepted_cultures) {
	        for (String accepted: _accepted_cultures) {
	        	if (null != accepted) {
	        		if (accepted.equals(culture)) {
	        			return true;
	        		}
	        	}
	        }
        }	
        
        return false;
	}
	
	public double getCultureBonus(String culture) {
		// 3 tiers. 
		// 1) primary culture.  bonus of 0.0.
		// 2) accepted culture or in same group as primary culture.  bonus of -0.1 (10% penalty).
		// 3) non-accepted culture and not in same group as primary.  bonus of -0.3 (30% penalty).
		if (isPrimary(culture)) {
			return 0.0;
		}
		
		if (isAccepted(culture)) {
			return -0.1;
		}
		        
        return -0.3;
	}	
	
	public void setStability(double argStability) {
		_stability = argStability;
	}
	
	public void setBadboy(double argBadboy) {
		_badboy = argBadboy;
	}
	
	public void setTreasury(double argTreasury) {
		_treasury = argTreasury;
	}
	
    public int compareTo(EU3Country c) { 
    	return _tag.compareTo(c._tag); 
	}	
	
	static public class Monarch {
		public short   _year;
		public byte    _month;
		public byte    _day;
		public String  _name;
                public String  _dynasty;
		public byte    _dip;
		public byte    _mil;
		public byte    _adm;
		public boolean _regent;
		public int	   _id;
		
		public Monarch(short year, byte month, byte day, String name, String dynasty, byte dip, byte mil, byte adm, boolean regent) {
			if (dip>9) dip=9;
			if (mil>9) mil=9;
			if (adm>9) adm=9;
			if (dip<1) dip=1;
			if (mil<1) mil=1;
			if (adm<1) adm=1;
			_year    = year;
			_month   = month;
			_day     = day;
			_name    = "\"" + name + "\"";
                        _dynasty = dynasty;
			_dip     = dip;
			_mil     = mil;
			_adm     = adm;
			_regent  = regent;
			_id      = __monarchId++;
			
			if (_regent) {
				_name = "\"(Regency Council)\"";
			} 
		}
		
		private StructField make() {
			StructField f = new StructField("monarch");
			StructField g = new StructField(String.format(Locale.US,"%d.%d.%d", _year,_month,_day));
			StructField h = new StructField("");
			h.addField(g);
			g.addField(f);
			f.addField(new BaseField("name",_name));
                        f.addField(new BaseField("dynasty",_dynasty));
			f.addField(new BaseField("DIP",_dip));
			f.addField(new BaseField("MIL",_mil));
			f.addField(new BaseField("ADM",_adm));
			return h;
		}
		
		public void write(PrintWriter pw, boolean writeYear) {
			if (writeYear) pw.println("        " + _year + "." + _month + "." + _day + "={");
	        pw.println("            monarch={");
	        pw.println("                name=" + _name);
	        pw.println("                DIP=" + _dip);
	        pw.println("                ADM=" + _adm);
	        pw.println("                MIL=" + _mil);
	        if (_regent) {
		        pw.println("                regent=yes");
	        }
	        pw.println("                id={");
	        pw.println("                    id=" + _id);
	        pw.println("                    idtype=37");
	        pw.println("                }");
                if (_dynasty != null) {
                    pw.println("                dynasty=\"" + _dynasty + "\"");
                }
	        pw.println();
	        pw.println("            }");
	        if (writeYear) pw.println("        }");	        
		}
	}
	
	public void setMonarch(Monarch m) {
		_monarch = m;
	}

	static public class Heir {
		public short   _year;
		public byte    _month;
		public byte    _day;
		public String  _name;
                public String  _dynasty;
		public byte    _dip;
		public byte    _mil;
		public byte    _adm;
		public short   _claim;
                public Monarch _monarch;
		public int     _id;

		public Heir(short year, byte month, byte day, String name, String dynasty, byte dip, byte mil, byte adm, short claim, Monarch monarch) {
			if (dip>9) dip=9;
			if (mil>9) mil=9;
			if (adm>9) adm=9;
			if (dip<1) dip=1;
			if (mil<1) mil=1;
			if (adm<1) adm=1;
			_year    = year;
			_month   = month;
			_day     = day;
			_name    = "\"" + name + "\"";
                        _dynasty = dynasty;
			_dip     = dip;
			_mil     = mil;
			_adm     = adm;
                        _claim   = claim;
			_monarch = monarch;
			_id      = __monarchId++;
		}

		private StructField make() {
			StructField f = new StructField("heir");
			StructField g = new StructField(String.format(Locale.US,"%d.%d.%d", _year,_month,_day));
			StructField h = new StructField("");
			h.addField(g);
			g.addField(f);
			f.addField(new BaseField("name",_name));
                        f.addField(new BaseField("dynasty",_dynasty));
			f.addField(new BaseField("DIP",_dip));
			f.addField(new BaseField("MIL",_mil));
			f.addField(new BaseField("ADM",_adm));
			return h;
		}

		public void write(PrintWriter pw, boolean writeYear) {
			if (writeYear) pw.println("        " + _year + "." + _month + "." + _day + "={");
	        pw.println("            heir={");
	        pw.println("                name=" + _name);
	        pw.println("                DIP=" + _dip);
	        pw.println("                ADM=" + _adm);
	        pw.println("                MIL=" + _mil);
	        pw.println("                id={");
	        pw.println("                    id=" + _id);
	        pw.println("                    idtype=37");
	        pw.println("                }");
                pw.println("                dynasty=\"" + _dynasty + "\"");
	        pw.println("                birth_date=\"" + _year + "." + _month + "." + _day + "\"");
                pw.println("                death_date=\"" + (_year+40) + "." + _month + "." + _day + "\"");
                pw.println("                claim=" + _claim);
                pw.println("                monarch_name=\"" + _monarch._name +"\"");
	        pw.println("            }");
	        if (writeYear) pw.println("        }");
		}
	}

	public void setHeir(Heir h) {
		_heir = h;
	}
	
	static public class Leader {
		public short   _year;
		public byte    _month;
		public byte    _day;
		public String   _death_date;
		public String  _name;
		public String  _type;
		public byte    _rank;
		public byte    _fir;
		public byte    _sho;
		public byte    _man;
		public byte    _sie;
		public int    _id;
		
		public Leader(String type, short year, byte month, byte day, String death_date, byte rank, String name, byte fir, byte sho, byte man, byte sie) {
			this(type, year, month, day, (short) 0, rank, name, fir, sho, man, sie);
			_death_date = death_date;
			if (null == _death_date) {
				_death_date = OptionSection.getStartDate();
			}
		}
		
		public Leader(String type, short year, byte month, byte day, short death, byte rank, String name, byte fir, byte sho, byte man, byte sie) {
			_type    = type;
			_year    = year;
			_month   = month;
			_day     = day;
			_death_date = death + ".1.1";
			_rank    = rank;
			_name    = "\"" + name + "\"";
			_fir     = check(fir);
			_sho     = check(sho);
			_man     = check(man);
			_id      = __leaderId++;
		}
		
		private StructField make() {
			StructField f = new StructField("leader");
			StructField g = new StructField(String.format(Locale.US,"%d.%d.%d", _year,_month,_day));
			StructField h = new StructField("");
			BaseField i = new BaseField("death_date", _death_date);
			h.addField(g);
			g.addField(f);
			f.addField(new BaseField("name",_name));
			f.addField(new BaseField("type",_type));
			f.addField(new BaseField("rank",_rank));
			f.addField(new BaseField("fire",_fir));
			f.addField(new BaseField("shock",_sho));
			f.addField(new BaseField("manuever",_man));
			f.addField(new BaseField("siege",_sie));
			f.addField(i);
			return h;
		}
		
		private byte check(byte stat) { if (stat<0) return 0; if (stat>6) return 6; return stat; }
		
		public void write(PrintWriter pw, boolean writeYear) {
			if (writeYear) pw.println("        " + _year + "." + _month + "." + _day + "={");
	        pw.println("            leader={");
	        pw.println("                name=" + _name);
	        pw.println("                type=" + _type);
	        pw.println("                manuever=" + _man);
	        pw.println("                fire=" + _fir);
	        pw.println("                shock=" + _sho);
	        pw.println("                siege=" + _sie);
	        pw.println("                activation=\"" + _year + "." + _month + "." + _day + "\"");
	        pw.println("                death_date=\"" + _death_date + "\"");
	        pw.println("                id={");
	        pw.println("                    id=" + _id);
	        pw.println("                    idtype=38");
	        pw.println("                }");
	        pw.println();
	        pw.println("            }");
	        if (writeYear) pw.println("        }");	        
		}		
	}

	public void addLeader(String type, short year, byte month, byte day, short death, byte rank, String name, byte fir, byte sho, byte man, byte sie) {
		Leader leader = new Leader(type,year,month,day,death,rank,name,fir,sho,man,sie);
		_leaders.add(leader);
	}
}
