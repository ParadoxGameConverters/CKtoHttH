package net.sourceforge.ck2httt.eu3;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;

import net.sourceforge.ck2httt.cv.CvCountry;
import net.sourceforge.ck2httt.pxAnalyzer.PXTree.BaseField;
import net.sourceforge.ck2httt.pxAnalyzer.PXTree.Field;
import net.sourceforge.ck2httt.pxAnalyzer.PXTree.StructField;
import net.sourceforge.ck2httt.pxAnalyzer.PXTree.Template;
import net.sourceforge.ck2httt.utils.OptionSection;


public class EU3Province extends EU3FileChanger implements Comparable<EU3Province> {
	
	static public Map<String, Set<EU3Province>> __provsByTag = new HashMap<String, Set<EU3Province>>();
	static public Map<String, EU3Province> __provsById = new HashMap<String, EU3Province>();
	
	static public NavigableSet<String> __ports = new TreeSet<String>();	
		
	// default cots;
	static final String[] __cots = { "45", "90", "101", "112", "151", "183", "224", "227", "310", "358", "429", "431", "454", "464", "503", "559", "596", "685", "808", "856", "1021", "1132", "1201", "361" };
	static {
		Arrays.sort(__cots);
	}
		
	static Template __provTemplate;
	static String   __rootPath;
	static String   __altPath;
	static String[] __files;
	static String[] __filesAlt;

	static private String __template =
		"{owner,controller,culture,religion,hre,base_tax,trade_goods,"+
		"manpower,capital,citysize,add_core," +
		"temple,workshop,courthouse,regimental_camp,"+
		"shipyard,constable,marketplace,tax_assessor,customs_house,wharf,"+
		"war_college,naval_college,weapons,textile,refinery,university,fort1,fort2," +
		"fort3,fort4,fort5,fort6,discovered_by}";
	
	public static String[] __buildings = {
		"temple","workshop","courthouse","regimental_camp","shipyard","constable",
		"marketplace","tax_assessor","customs_house",
		"wharf","war_college","naval_college","weapons","textile","refinery","university",
		"fort1","fort2","fort3","fort4","fort5","fort6"
	};

	static public enum Building {
		TEMPLE(0),
		WORKSHOP(1),
		COURTHOUSE(2),
		SHIPYARD(4),
		MARKETPLACE(6),
		TAX_ASSESSOR(7),
		CUSTOMS_HOUSE(8),
		WHARF(9),
		TEXTILE(13),
		UNIVERSITY(15),
		FORT1(16),
		FORT2(17),
		FORT3(18);
		final private int _id;
		Building(int id) { _id=id; }
		public String toString() { return __buildings[_id]; }
	};
	
	public String _id;
	int _idNum;
	String   _owner;        //compute it
	String   _controller;   //compute it
	public String   _culture;      //compute it
	public String   _religion;     //compute it if christian, complicated if muslim
	double    _base_tax = -1;     //compute it
	double	_eu3_default_base_tax;
	double 	_ck_default_base_tax;
	int      _manpower = -1;     //compute it
	int		_eu3_default_mp;
	public int      _citysize = -1;     //compute it
	int _eu3_default_city_size;
	String[] _add_core;     //compute it
	String[] _buildings;    //compute it
	String[] _discovered_by;//get it from original file ; adapt to owned provinces
	int      _nb_disc;
	
	public String 	_name; // look it up
	String _capital;
	int	_garrison; // compute it
	String _trade_goods; 
	boolean _hre;
	boolean _cot;
	boolean _port;
	
	protected Template getTemplate() {
		return __provTemplate;
	}
	
	static public void setRootPath(String path, String alt, String complement) {
		if (__rootPath==null) {
		    __rootPath = path+complement;
		    __files=(new File(__rootPath)).list();
		}
		if (alt!=null && __altPath==null) {
		    __altPath = alt+complement;
		    __filesAlt=(new File(__altPath)).list();
		}
	}

	static public String findFileName(String id) {
		String beg0 = id+"-";
		String beg1 = id+" -";
		if (__filesAlt!=null)
			for (String f : __filesAlt)
				if (f.startsWith(beg0) || f.startsWith(beg1))
					return __altPath+"/"+f;
		for (String f : __files)
			if (f.startsWith(beg0) || f.startsWith(beg1))
				return __rootPath+"/"+f;
		return null;
	}
	
	public EU3Province(String id) throws IOException {
		super(findFileName(id),null);
		_id = id;
		_idNum = Integer.parseInt(id);
		if (null == _root) {
			_root = new StructField("Dummy");
		}
		BaseField b = _root.getBase("religion");
		_religion = (b != null) ? b.get() : null;
		b = _root.getBase("culture");
		_culture = (b != null) ? b.get() : null;
		b = _root.getBase("base_tax");
		_base_tax = (b != null) ? b.getAsFloat() : -1;
		_eu3_default_base_tax = _base_tax;
		b = _root.getBase("manpower");
		_manpower = (b != null) ? b.getAsInt() : -1;
		_eu3_default_mp = _manpower;
		b = _root.getBase("citysize");
		if (null != b) { _eu3_default_city_size = b.getAsInt(); }
		BaseField[] db = _root.getAllBase("discovered_by");
		_discovered_by = new String[db.length+1];
		for (BaseField f : db) _discovered_by[_nb_disc++]=f.get();
		if (__provTemplate==null) {
			__provTemplate = new Template(__template);
		}
		for (Field<?> f : _root._data)
			delDateField(f);
	}
	
	public void setOwner(String id)          { _owner=id; addTo(_discovered_by,id); }
	public String getOwner() 				 { return _owner; }
	public void setController(String id)     { _controller=id; addTo(_discovered_by,id); }
	public String getController()            { return _controller; }
	public void setCulture(String id)        { _culture=id; }
	public void setReligion(String id)       { if (id!=null) _religion=id; }

	public void setBaseTax(double v) { 
		_base_tax=v; 
	}	

	public void setCKDefaultBaseTax(double argCKDefaultBaseTax) {
		_ck_default_base_tax = argCKDefaultBaseTax;
	}
	
	public double getCKDefaultBaseTax() {
		return _ck_default_base_tax;	
	}
	
	public double getEU3DefaultBaseTax() {
		return _eu3_default_base_tax;
	}
	
	public int getEU3DefaultMP() {
		return _eu3_default_mp;
	}
	
	public int getEU3DefaultCitySize() {
		return _eu3_default_city_size;
	}
	
	public void setManpower(int v) { 
		_manpower = v; 	
	}
	
	public void setCitySize(int v) {
		_citysize = v; 	
	}
	public void setCore(String id)           { if (id==null || id.equals("REB")) return; _add_core = addTo(_add_core,id); _discovered_by = addTo(_discovered_by,id); }
	public void remCore(String id)           { _add_core = remFrom(_add_core, id); _discovered_by = remFrom(_discovered_by,id); }
	public void setBuilding(Building b)      { _buildings = addTo(_buildings, b.toString()); }
	public void setBuildings(Set<String> l) {
		for (String s : l) {
		    _buildings = addTo(_buildings, s);
		}
	}
	
	public void takeEU3Defaults() {
		BaseField b = _root.getBase("owner"); 
		_owner = (b != null) ? b.get() : null;
		b = _root.getBase("controller"); 
		_controller = (b != null) ? b.get() : null;		
		b = _root.getBase("culture"); 
		_culture = (b != null) ? b.get() : null;		
		b = _root.getBase("religion"); 
		_religion = (b != null) ? b.get() : null;		
		b = _root.getBase("base_tax"); 
		_base_tax = (b != null) ? b.getAsFloat() : -1;		
		b = _root.getBase("manpower"); 
		_manpower = (b != null) ? b.getAsInt() : -1;		
		b = _root.getBase("citysize"); 
		_citysize = (b != null) ? b.getAsInt() : -1;		
		
		BaseField[] list = _root.getAllBase("add_core");
		if ((null != list) && (list.length > 0)) {
			_add_core = new String[list.length];
			for (int i = 0; i < list.length; i++) {
				_add_core[i] = list[i].get();
			}
		}
		
		list = _root.getAllBase("discovered_by");
		if ((null != list) && (list.length > 0)) {
			_discovered_by = new String[list.length];
			for (int i = 0; i < list.length; i++) {
				_discovered_by[i] = list[i].get();
			}
		}		
		// see if any buildings are present here.
		List<String> temp = new ArrayList<String>();
		for (String building: __buildings) {
			b = _root.getBase(building);
			if (null != b) {
				temp.add(building);
			}
		}
		if (temp.size() > 0) {
			_buildings = new String[temp.size()];
			_buildings = temp.toArray(_buildings);
		}
		
		calcNameEtc();
	}

	public void applyChanges() {
		applyBase("owner",_owner);
		applyBase("controller",_controller);
		applyBase("culture",_culture);
		applyBase("religion",_religion);
		applyBase("base_tax",(float) _base_tax);
		applyBase("manpower",_manpower);
		applyBase("citysize",_citysize);
		applyBaseList("add_core",_add_core);
		applyBaseList("discovered_by",_discovered_by);
		applyYesFields(__buildings,_buildings);

		calcNameEtc();
	}
	
	public void calcNameEtc() {
		// don't really understand how these should be applyBased, but will put my updates here anyway
		_name = EU3LocalizedText.getName("PROV" + _id);
		BaseField b = _root.getBase("capital"); 
		_capital = (b != null) ? b.get() : null;
		b = _root.getBase("trade_goods");
		_trade_goods = (b != null) ? b.get() : null;
		b = _root.getBase("hre");
		_hre = (b != null) ? b.get().equals("yes") : false;

		// 1000 men for each fort level. 
		_garrison = 0;
		if (null != _buildings) {
			for (String building: _buildings) {
				if ((null != building) &&
						(building.equals("fort1") ||
						building.equals("fort2") ||
						building.equals("fort3") ||
						building.equals("fort4") ||
						building.equals("fort5") ||
						building.equals("fort6"))) {
					_garrison += 1000;
				}
			}
		}	
		
		// see if this is a cot...if it is, it will be in the cot listing.
		_cot = (Arrays.binarySearch(__cots, _id) >= 0);
		
		// see if this is a port
		_port = __ports.contains(_id);
	}
	
	public static void loadPorts(String file) {
		try {
			BufferedReader portBR = new BufferedReader(new FileReader(file));
					
			String line = null;
			String prov = null;
			while ((line = portBR.readLine()) != null) {
				if (line.contains(" = {")) { 
					// opening a new province
					prov = line.substring(0, line.indexOf(" "));
				} else if (line.contains("port = {") ||
							line.contains("port={")) {
					// means this province has a port
					if (null != prov) {
						__ports.add(prov);
						prov = null; // don't need it now.
					} else {
						System.out.println("wtf? prov should not be null here");
					}
				}
			}
			
			portBR.close();		
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("wtf? error reading for ports");
		}
	}
	
	public boolean hasBuilding(Building toFind) {
		return hasBuilding(toFind.toString());
	}
	
	public boolean hasBuilding(String toFind) {
		if (null ==_buildings) {
			return false;
		}
		
		for (String building: _buildings) {
			if ((null != building) && building.equals(toFind)) {
				return true;
			}
		}
		
		return false;
	}
	
	public static EU3Province getProvById(String id) {
		return __provsById.get(id);
	}
	
	public static Set<EU3Province> getProvsByTag(String tag) {
		return __provsByTag.get(tag);
	}
	
	public boolean isDiscoveredBy(EU3Country c) {
		// either we have a special tag or the religion matches.
		for (String disc: _discovered_by) {
			if (null != disc) {
				if (disc.equals(c._tag) || disc.equals(c._tech_group)) {
					return true;
				}
			}
		}
		
		return false;
	}
	
	public boolean isHRE() {
		return _hre;
	}
	
	public boolean isCot() {
		return _cot;
	}
	
	public boolean isPort() {
		return _port;
	}
	
	public static void saveProvince(EU3Province p) {
		if (null != p._owner) {
			// at this point, put it in the list of by owners and by id.
			Set<EU3Province> provsOwned = __provsByTag.get(p._owner);
			if (null == provsOwned) {
				provsOwned = new HashSet<EU3Province>(); 
				__provsByTag.put(p._owner, provsOwned);
			}
			provsOwned.add(p);
		}
		__provsById.put(p._id, p);		
	}
		
	public void write (String out) throws IOException {
		// applyChanges();		
		EU3Country eu3c = null;	
		PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(out, true), "ISO-8859-1"));
		pw.println(_id + "={");
		pw.println("    flags={");
		pw.println("    }");
		pw.println("    name=\"" + _name + "\"");
		if (null != _owner) { 
			pw.println("    owner=\"" + _owner + "\"");
			eu3c = EU3Country.get(_owner);
		}
		if (null != _controller) pw.println("    controller=\"" + _controller + "\"");
		if (null != _add_core) {
			for (String core: _add_core) {
				if (null != core) {
					pw.println("    core=\"" + core + "\"");
				}
			}
		}
		if (null != _culture) pw.println("    culture=" + _culture);
		if (null != _religion) pw.println("    religion=" + _religion);
		if (null != _capital) pw.println("    capital=" + _capital);
		if (-1 != _citysize) pw.println("    citysize=" + _citysize);
		if (_garrison > 0) {
			pw.println("    garrison=" + _garrison);
		}
		if (-1 != _base_tax) pw.println("    base_tax=" + String.format(Locale.US,"%.3f",_base_tax));
		if (-1 != _manpower) pw.println("    manpower=" + _manpower);				
		if (_hre) {
			pw.println("    hre=yes");
		}
		if (null != _trade_goods) pw.println("    trade_goods=" + _trade_goods);
		if (null != _buildings) {
			for (String building: _buildings) {
				if (null != building) {
					pw.println("    " + building + "=yes");					
				}
			}
		}
		pw.println("    history={");
		if (null != _owner) {
			pw.println("        owner=\"" + _owner + "\"");
		}
		if (null != _controller) {
			pw.println("        controller=\"" + _controller + "\"");
		}
		if (null != _culture) pw.println("        culture=" + _culture);
		if (null != _religion) pw.println("        religion=" + _religion);	
		if (_hre) pw.println("        hre=\"yes\"");
		if (_cot) pw.println("        cot=\"yes\"");
		if (-1 != _base_tax) pw.println("        base_tax=" + String.format(Locale.US,"%.3f",_base_tax));
		if (null != _trade_goods) pw.println("        trade_goods=" + _trade_goods);
		if (-1 != _manpower) pw.println("        manpower=" + _manpower);
		if ((-1 == _citysize) && (_root != null)) {
			BaseField b = _root.getBase("native_size");
			if (null != b) { pw.println("        native_size=" + ((int) Math.floor(b.getAsFloat()))); }
			b = _root.getBase("native_ferocity");
			if (null != b) { pw.println("        native_ferocity=" + ((int) Math.floor(b.getAsFloat()))); }
			b = _root.getBase("native_hostileness");
			if (null != b) { pw.println("        native_hostileness=" + ((int) Math.floor(b.getAsFloat()))); }
		}
		
		if (null != _capital) pw.println("        capital=" + _capital);
		if (-1 != _citysize) pw.println("        citysize=" + _citysize);		
		if (null != _buildings) {
			for (String building: _buildings) {
				if (null != building) {
					pw.println("        " + building + "=yes");					
				}
			}
		}
		boolean western     = false;
		boolean eastern     = false;
		boolean ottoman		= false;
		boolean muslim      = false;
		boolean indian      = false;
		boolean chinese     = false;
		boolean sub_saharan = false;
		boolean new_world   = false;
		if (null != _discovered_by) {						
			for (String disc: _discovered_by) {
				if ("western".equals(disc)) {
					western = true;
				} else if ("eastern".equals(disc)) {
					eastern = true;
				} else if ("ottoman".equals(disc)) {
					ottoman = true;
				} else if ("muslim".equals(disc)) {
					muslim = true;
				} else if ("indian".equals(disc)) {
					indian = true;
				} else if ("chinese".equals(disc)) {
					chinese = true;
				} else if ("sub_saharan".equals(disc)) {
					sub_saharan = true;
				} else if ("new_world".equals(disc)) {
					new_world = true;
				}
			}		
			
			if ((null != _owner) & ("NOR".equals(_owner))) {
				int i = 0;
				i++;
			}			
			
			// prune the discovery list of any specific tags that we already have the matches for.
			for (int i=0; i < _discovered_by.length; i++) {
				String disc = _discovered_by[i];
				if (null != disc) {
					// sanity check this, should either be 3 letters long or one of the predefines.
					if (3 == disc.length()) {
						EU3Country c = EU3Country.get(disc);
						boolean strip = false;
						if (null != c._tech_group) {
							strip |= western && "western".equals(c._tech_group);
							strip |= eastern && "eastern".equals(c._tech_group);
							strip |= ottoman && "ottoman".equals(c._tech_group);
							strip |= muslim && "muslim".equals(c._tech_group);
							strip |= chinese && "chinese".equals(c._tech_group);
							strip |= sub_saharan && "sub_saharan".equals(c._tech_group);
							strip |= new_world && "new_world".equals(c._tech_group);
						}
						if (strip) {
							_discovered_by = remFrom(_discovered_by, disc);
							i--;
						}
					}
				}
			}
			
			for (String disc: _discovered_by) {
				if (null != disc) {
					// sanity check this, should either be 3 letters long or one of the predefines.
					if ((3 == disc.length()) ||
							"western".equals(disc) ||
							"eastern".equals(disc) ||
							"ottoman".equals(disc) ||
							"muslim".equals(disc) ||
							"indian".equals(disc) ||
							"chinese".equals(disc) ||
							"sub_saharan".equals(disc) ||
							"new_world".equals(disc)) {						
						pw.println("        discovered_by=\"" + disc + "\"");
					} else {
						System.out.println("wtf discovery: " + disc + " for: " + _id + " (" + _name + ")");					
					}
				}
			}						
		}

		// see if we have any advisors in this province.
		NavigableSet<EU3Advisor> advisors = EU3Advisor.getAdvisors();
		for (EU3Advisor ad: advisors) {
			if (_idNum == ad._prov) {
/*				if (_idNum == 534) {
					int i = 0;
					i++;
				}	*/		
				if (ad._year <= OptionSection.getStartYear()){			
					pw.println("        " + OptionSection.getStartDate() + "={");
					pw.println("            advisor={");
					pw.println("                name=" + ad._name); // name is assumed to have quotes
					pw.println("                type=" + ad._type);
					pw.println("                skill=" + ad._skill);
					pw.println("                location=" + ad._prov);
	                pw.println("                date=\"" + OptionSection.getStartDate() + "\"");
	                pw.println("                hire_date=\"1.1.1\"");	                	
	                pw.println("                id={");
	                pw.println("                    id=" + ad._id);
	                pw.println("                    idtype=39");
	                pw.println("                }");
					pw.println("            }");
					pw.println("        }");
				}
			}
		}
		
		if (null != _add_core) {
			pw.println("        " + OptionSection.getStartDate() + "={");	        
			for (String core: _add_core) {
				if (null != core) {
					pw.println("            add_core=\"" + core + "\"");
				}
			}
			pw.println("        }");			
		}		
		
		pw.println("    }"); // closes history
		
		// all the CK provinces will be some combination of western, eastern, ottoman, and muslim discovered.
		// unfortunately for us, we don't know what the tags will be.
		String[] dates = { "9999.1.1",  // custom
				"9999.1.1", // western
				"9999.1.1", // eastern
				"9999.1.1", // ottoman
				"9999.1.1", // muslim
				"9999.1.1", // chinese
				"9999.1.1", // indian
				"9999.1.1", // sub_saharan
				"9999.1.1" }; // new world
		String[] discDates = { "9999.1.1", // custom
				"9999.1.1", // catholic, or at least the christian flag
				"9999.1.1", // protestant
				"9999.1.1", // reformed
				"9999.1.1", // orthodox
				"9999.1.1", // sunni? muslim flag? chinese flag? non-christian flag?
				"9999.1.1", // shiite
				"9999.1.1", // buddhism
				"9999.1.1", // hinduism
				"9999.1.1", // confucianism
				"9999.1.1", // shinto
				"9999.1.1", // animism
				"9999.1.1" }; // shamanism
		if (western) {
			dates[1] = "1.1.1";			
			discDates[1] = OptionSection.getStartDate();
		}
		if (eastern) {
			dates[2] = "1.1.1";
			discDates[1] = OptionSection.getStartDate();
			discDates[4] = OptionSection.getStartDate();
		}
		if (ottoman) {
			dates[3] = "1.1.1";
			discDates[5] = OptionSection.getStartDate();
			discDates[6] = OptionSection.getStartDate();
		}
		if (muslim) {
			dates[3] = "1.1.1";
			discDates[5] = OptionSection.getStartDate();
			discDates[6] = OptionSection.getStartDate();
		}
		if (indian) {
			dates[4] = "1.1.1";
			discDates[5] = OptionSection.getStartDate();
			discDates[7] = OptionSection.getStartDate();
			discDates[8] = OptionSection.getStartDate();
		}
		if (chinese) {
			dates[5] = "1.1.1";
			discDates[ 5] = OptionSection.getStartDate();
			discDates[ 7] = OptionSection.getStartDate();
			discDates[ 8] = OptionSection.getStartDate();
			discDates[ 9] = OptionSection.getStartDate();
			discDates[10] = OptionSection.getStartDate();
			discDates[11] = OptionSection.getStartDate();
			discDates[12] = OptionSection.getStartDate();
		}
		if (sub_saharan) {
			// none of these actually hit, because the sub_saharan nations all have different specific province lists.
		}
		if (new_world) {
			// none of these actually hit, because the new world nations all have different specific province lists.
		}
		
		// add any custom tags we need here		
		// need to make sure to add the arbitrary entries here from the base save...		
		// make sure we get the custom detected before we write out the discovery_dates
		NavigableSet<String> discList = new TreeSet<String>();		
		if (null != _discovered_by) {
			for (String disc: _discovered_by) {
				if ((null != disc) && (disc.length() == 3)) {
					discList.add(disc);
					// since we added a custom here
					dates[0] = "1.1.1";			
					discDates[0] = OptionSection.getStartDate();						
				}
			}
		}
		
		pw.print("discovery_dates={");
		for (String date: dates) {
			pw.print(date);
			pw.print(" ");
		}
		pw.println("}");
		pw.print("discovery_religion_dates={");
		for (String date: discDates) {
			pw.print(date);
			pw.print(" ");
		}
		pw.println("}");
		pw.print("discovered_by={");
		if (western) {
			discList.addAll(CvCountry.__western);
		}
		if (eastern) {
			discList.addAll(CvCountry.__eastern);
		}
		if (ottoman) {
			discList.addAll(CvCountry.__ottoman);
		}
		if (muslim) {
			discList.addAll(CvCountry.__muslim);
		}
		if (indian) {
			discList.addAll(CvCountry.__indian);
		}
		if (chinese) {
			discList.addAll(CvCountry.__chinese);
		}
		if (sub_saharan) {
			discList.addAll(CvCountry.__sub_saharan);
		}
		if (new_world) {
			discList.addAll(CvCountry.__new_world);
		}
		if (null != _owner) { // only null for the azores...well, and the sea zones.			
			if (!discList.contains(_owner)) {
				discList.add(_owner);
				// since we added a custom here
				dates[0] = "1.1.1";			
				discDates[0] = OptionSection.getStartDate();				
			}
		}
		
		for (String tag: discList) {
			pw.print(tag);
			pw.print(" ");
		}					
		pw.println("}");		
		if ((-1 == _citysize) && (_root != null)) {
			BaseField b = _root.getBase("native_size");
			if (null != b) { pw.println("    native_size=" + ((int) Math.floor(b.getAsFloat()))); }
			b = _root.getBase("native_hostileness");
			if (null != b) { pw.println("    native_hostileness=" + ((int) Math.floor(b.getAsFloat()))); }
			b = _root.getBase("native_ferocity");
			if (null != b) { pw.println("    native_ferocity=" + ((int) Math.floor(b.getAsFloat()))); }
		}
		
		pw.println("}");
		
		pw.close();		
	}
	
    public int compareTo(EU3Province p) { 
    	return _idNum - p._idNum; 
	}	
}
