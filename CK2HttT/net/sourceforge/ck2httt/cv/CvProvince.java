package net.sourceforge.ck2httt.cv;
import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import net.sourceforge.ck2httt.ck.Analyzer;
import net.sourceforge.ck2httt.ck.County;
import net.sourceforge.ck2httt.eu3.EU3LocalizedText;
import net.sourceforge.ck2httt.eu3.EU3Province;
import net.sourceforge.ck2httt.pxAnalyzer.PXTree.BaseField;
import net.sourceforge.ck2httt.rules.CultureCvRules;
import net.sourceforge.ck2httt.rules.MergeRules;
import net.sourceforge.ck2httt.rules.ProvinceCvRules;
import net.sourceforge.ck2httt.rules.CultureCvRules.CultureCounter;
import net.sourceforge.ck2httt.rules.CultureCvRules.EU3Culture;
import net.sourceforge.ck2httt.utils.FieldSet;


/**
 * Class translating actual CK counties into a set of provinces for EU3
 * 
 * @author yprelot
 */
public class CvProvince implements Comparable<CvProvince> {

	static public boolean USE_AVERAGE = false;
	
	static private String     __destPath;
	static public Set<String> __owners = new HashSet<String>();
	
	public String             _id;
	public int				  _idNum;
	public ProvinceCvRules    _data;
	public FieldSet<County>   _counties;
	public CvCountry          _controller;
	public CvCountry          _owner;
	public boolean            _isCapital;
	public EU3Province        _province;
	public EU3Culture         _culture;
	public TreeSet<CvCountry> _core;
	
	public double			_pop;
	public double			_chPop;
	public double			_maPop;
	public double			_coPop;
	public double			_advances;
	public double			_improvements;
	
	public double			_uncool = 0.0;
	
	public double getCool() {
		double cool = (_chPop - (_uncool * 500));

		return cool;
	}
	
	static public TreeSet<CvProvince> __list = new TreeSet<CvProvince>();
	static public TreeSet<EU3Province> __eu3List = new TreeSet<EU3Province>();
	static public TreeSet<CvProvince> __coolList = new TreeSet<CvProvince>(new Comparator<CvProvince>() {
    		public int compare(CvProvince p1, CvProvince p2) {
    			double c1 = p1.getCool(); 		
    			double c2 = p2.getCool();
    				
    			if (c1 > c2) {
    				return -1;
    			} else if (c1 < c2) {
    				return 1;
    			} else {
    				return p1._id.compareTo(p2._id);    				
    			}
    		}
	});
	static public TreeSet<CvProvince> __pureEuropeList = new TreeSet<CvProvince>();
	
	static public void load() {
		for (ProvinceCvRules p : ProvinceCvRules._list) {
			__list.add(new CvProvince(p));
		}
		for (CvProvince p : __list) {
			for (String id : p._data._ck)
			    p._counties.add(County.__list.search(id));
		}
	}
	
	static public void setOutputPath(String path) throws IOException {
		__destPath = path;
		File dir = new File(path);
		if (dir.exists()) {
		}
		else if (!dir.mkdirs())
			throw new IOException("Unable to create directory : "+dir);
	}

	static public boolean isOwner(String tag) {
		return __owners.contains(tag);
	}
	
	static public void preWriteAll() throws IOException {
		double log10of2 = Math.log10(2);
		
		int europeCount = 0;
				
		int maxProv = 3000;
		
		// in pass 1, just calculate the scaled manpower and base tax numbers, and build a sorted order based 
		// on church educations
		double popLog = 0.0;
		double martialPopLog = 0.0;
		double courtPopLog = 0.0;
		double churchPopLog = 0.0;
		double defPop = 0.0;
		double defMp = 0.0;
		double defTax = 0.0;	
		double defCKTax = 0.0;
		for (int i = 1; i <= maxProv; i++) { // 3000 leaves a lot of room for expansion
			if (367 == i) {
				continue; // special handling of the Azores
			}
			CvProvince p = CvProvince.search(String.valueOf(i));
			EU3Province eu3P = null;
			if (null != p) {
				eu3P = p._province;	
				p._pop = 0;
				p._maPop = 0;
				p._coPop = 0;
				p._chPop = 0;
				for (County c : p._counties) {
					p._pop += c.getPopulation();
					p._maPop += c.getMartialPopulation();
					p._coPop += c.getCourtPopulation();
					p._chPop += c.getChurchPopulation();
					p._advances += c._advances.length;
					p._improvements += c._improvements.length;
					defCKTax += c._baseIncome;					
				}

				if (CvProvince.USE_AVERAGE) {
					// use average values based on demense for all of these values.
					p._pop = p._owner._main._owned.first().getPopulation(true);
					p._maPop = p._owner._main._owned.first().getMartialPopulation(true);
					p._coPop = p._owner._main._owned.first().getCourtPopulation(true);
					p._chPop = p._owner._main._owned.first().getChurchPopulation(true);
				}	
				
				__coolList.add(p); // put this in our sorted list downward by church population
				__pureEuropeList.add(p);
				if (p._pop >= 1.0) { popLog += (Math.log10(p._pop) / log10of2); }
				if (p._maPop >= 1.0) { martialPopLog += (Math.log10(p._maPop) / log10of2); }
				if (p._coPop >= 1.0) { courtPopLog += (Math.log10(p._coPop) / log10of2); }
				if (p._chPop >= 1.0) { churchPopLog += (Math.log10(p._chPop) / log10of2); }

				defPop += eu3P.getEU3DefaultCitySize();
				defMp += eu3P.getEU3DefaultMP();
				defTax += eu3P.getEU3DefaultBaseTax();
			} else {
				// default EU3 version, so meh.
			}
		}
		double popScale = defPop / popLog;
		double mpScale = defMp / martialPopLog;
		double courtTaxScale = defTax / courtPopLog;
		double ckTaxScale = defTax / defCKTax;
		ProvinceCvRules.setPopScale(popScale);
		ProvinceCvRules.setMpScale(mpScale);
		ProvinceCvRules.setCourtTaxScale(courtTaxScale);
		ProvinceCvRules.setCKTaxScale(ckTaxScale);
		
		// a different individual building version.  roughly distributing 1399 buildings
		int textileCount = 2;  		// exclusive to textile, university, shipyard
		int universityCount = 17;	// exclusive to textile, university, shipyard
		int shipyardCount = 1;		// exclusive to textile, university, shipyard
		int taxAssessorCount = 1;
		int customsHouseCount = 2;
		int workshopCount = 6;
		int courthouseCount = 10;
		int marketplaceCount = 71;
		int templeCount = 60;
		int bMax = (textileCount + universityCount + shipyardCount + taxAssessorCount + 
							customsHouseCount + workshopCount + courthouseCount + marketplaceCount + 
							templeCount);				
		for (int b = 0; b < bMax; b++) {		
			CvProvince p = null;
			boolean assigned = false;
			String tag = "----";
			while (!assigned) {	
				p = __coolList.first();
				__coolList.remove(p);
				// find the top level for purposes of display.
				tag = p._owner._tag;
				if (p._owner._liege != null) {
					tag = p._owner._liege._tag;
					if (p._owner._liege._liege != null) {
						tag = p._owner._liege._liege._tag;
					}
				}				
				/*
				if ("Sharizhor".equals(EU3LocalizedText.getName("PROV" + p._province._id))) {
					System.out.println();
				}
				*/							
				if ((textileCount > 0) && 
							!p._province.hasBuilding(EU3Province.Building.TEXTILE) &&
							!p._province.hasBuilding(EU3Province.Building.UNIVERSITY)) {
					assigned = true;
					textileCount--;
					p._province.setBuilding(EU3Province.Building.TEXTILE);
					System.out.format("%s:%03d:Textile Manufactury:%s\n", EU3LocalizedText.getName("PROV" + p._province._id), Math.round(p.getCool()), tag);
				} else if ((universityCount > 0) && 
						!p._province.hasBuilding(EU3Province.Building.UNIVERSITY) &&
						!p._province.hasBuilding(EU3Province.Building.TEXTILE)) {
					assigned = true;
					universityCount--;
					p._province.setBuilding(EU3Province.Building.UNIVERSITY);
					System.out.format("%s:%03d:University:%s\n", EU3LocalizedText.getName("PROV" + p._province._id), Math.round(p.getCool()), tag);
				} else if ((shipyardCount > 0) && 
						p._province.isPort() &&  
						!p._province.hasBuilding(EU3Province.Building.SHIPYARD) &&
						!p._province.hasBuilding(EU3Province.Building.UNIVERSITY) &&
						!p._province.hasBuilding(EU3Province.Building.TEXTILE)) {
					assigned = true;
					shipyardCount--;
					p._province.setBuilding(EU3Province.Building.SHIPYARD);
					System.out.format("%s:%03d:Shipyard:%s\n", EU3LocalizedText.getName("PROV" + p._province._id), Math.round(p.getCool()), tag);
				} else if ((taxAssessorCount > 0) && 
						!p._province.hasBuilding(EU3Province.Building.TAX_ASSESSOR)) {
					assigned = true;
					taxAssessorCount--;
					p._province.setBuilding(EU3Province.Building.TAX_ASSESSOR);
					System.out.format("%s:%03d:Tax Assessor:%s\n", EU3LocalizedText.getName("PROV" + p._province._id), Math.round(p.getCool()), tag);				
				} else if ((customsHouseCount > 0) && 
						!p._province.hasBuilding(EU3Province.Building.CUSTOMS_HOUSE)) {
					assigned = true;
					customsHouseCount--;
					p._province.setBuilding(EU3Province.Building.CUSTOMS_HOUSE);
					System.out.format("%s:%03d:Customs House:%s\n", EU3LocalizedText.getName("PROV" + p._province._id), Math.round(p.getCool()), tag);				
				} else if ((workshopCount > 0) && 
						!p._province.hasBuilding(EU3Province.Building.WORKSHOP)) {
					assigned = true;
					workshopCount--;
					p._province.setBuilding(EU3Province.Building.WORKSHOP);
					System.out.format("%s:%03d:Workshop:%s\n", EU3LocalizedText.getName("PROV" + p._province._id), Math.round(p.getCool()), tag);				
				} else if ((courthouseCount > 0) && 
						!p._province.hasBuilding(EU3Province.Building.COURTHOUSE)) {
					assigned = true;
					courthouseCount--;
					p._province.setBuilding(EU3Province.Building.COURTHOUSE);
					System.out.format("%s:%03d:Courthouse:%s\n", EU3LocalizedText.getName("PROV" + p._province._id), Math.round(p.getCool()), tag);				
				} else if ((marketplaceCount > 0) && 
						!p._province.hasBuilding(EU3Province.Building.MARKETPLACE)) {
					assigned = true;
					marketplaceCount--;
					p._province.setBuilding(EU3Province.Building.MARKETPLACE);
					System.out.format("%s:%03d:Marketplace:%s\n", EU3LocalizedText.getName("PROV" + p._province._id), Math.round(p.getCool()), tag);				
				} else if ((templeCount > 0) && 
						!p._province.hasBuilding(EU3Province.Building.TEMPLE)) {
					assigned = true;
					marketplaceCount--;
					p._province.setBuilding(EU3Province.Building.TEMPLE);
					System.out.format("%s:%03d:Temple:%s\n", EU3LocalizedText.getName("PROV" + p._province._id), Math.round(p.getCool()), tag);				
				} else {
					// usually happens with the predecessors in the list have the building being handed out (marketplace or temple)
				}
			}
			// and rewrite the _coolList.
			__coolList = new TreeSet<CvProvince>(new Comparator<CvProvince>() {
	    		public int compare(CvProvince p1, CvProvince p2) {
	    			double c1 = p1.getCool();    		
	    			double c2 = p2.getCool();
	    				
	    			if (c1 > c2) {
	    				return -1;
	    			} else if (c1 < c2) {
	    				return 1;
	    			} else {
	    				return p1._id.compareTo(p2._id);    				
	    			}
	    		}
			});
			for (CvProvince cv: __pureEuropeList) {
				String cvTag = cv._owner._tag;
				if (cv._owner._liege != null) {
					cvTag = cv._owner._liege._tag;
					if (cv._owner._liege._liege != null) {
						cvTag = cv._owner._liege._liege._tag;
					}
				}
				
				if (cvTag.equals(tag)) {
					cv._uncool += 1;
				}
				__coolList.add(cv);
			}
		}		
		
		// the individual building version
/*		boolean shipyard = false;
		for (CvProvince p: __coolList) {
			System.out.format("%s:%03d\n", EU3LocalizedText.getName("PROV" + p._province._id), Math.round(p._chPop + p._advances + p._improvements));
			// roughly distribute the normal 1399 or 1453 start buildings...for now, just 1399
			
			if (b < )
			if (b < 3) {
				// university, workshop, temple, marketplace (2)
				//	Ferrara, Pisa
				p._province.setBuilding(EU3Province.Building.UNIVERSITY);
				p._province.setBuilding(EU3Province.Building.WORKSHOP);
				p._province.setBuilding(EU3Province.Building.MARKETPLACE);
				p._province.setBuilding(EU3Province.Building.TEMPLE);
			} else if (b < 5) {
				// courthouse, marketplace, textile (2)
				//	Antwerpen, Vlaanderen
				p._province.setBuilding(EU3Province.Building.TEXTILE);
				p._province.setBuilding(EU3Province.Building.COURTHOUSE);
				p._province.setBuilding(EU3Province.Building.MARKETPLACE);
			} else if (b < 8) {				
				// university, marketplace, temple (3)
				//	Ile-de-France, Toulouse, Bohemia
				p._province.setBuilding(EU3Province.Building.UNIVERSITY);
				p._province.setBuilding(EU3Province.Building.MARKETPLACE);
				p._province.setBuilding(EU3Province.Building.TEMPLE);
			} else if (b < 13) {
				// university, marketplace (5)
				// 	Beira, East Anglia, Leipzig, Erfurt, Brabant
				p._province.setBuilding(EU3Province.Building.UNIVERSITY);
				p._province.setBuilding(EU3Province.Building.MARKETPLACE);
			} else if (b < 16) {
				// university, temple (3)
				// Parma, Modena, Romagna 
				p._province.setBuilding(EU3Province.Building.UNIVERSITY);
				p._province.setBuilding(EU3Province.Building.TEMPLE);
			} else if (b < 20) {
				// university (4)
				// Firenze, Siena, Salamanca, Oxfordshire
				p._province.setBuilding(EU3Province.Building.UNIVERSITY);				
			} else if (b < 21) {
				// courthouse, marketplace, tax_assessor, temple (1)
				//	Avignon
				p._province.setBuilding(EU3Province.Building.COURTHOUSE);
				p._province.setBuilding(EU3Province.Building.MARKETPLACE);
				p._province.setBuilding(EU3Province.Building.TAX_ASSESSOR);
				p._province.setBuilding(EU3Province.Building.TEMPLE);
			} else if (b < 22) {
				// workshop, temple, marketplace (1)
				//	Uppland
				p._province.setBuilding(EU3Province.Building.WORKSHOP);
				p._province.setBuilding(EU3Province.Building.MARKETPLACE);
				p._province.setBuilding(EU3Province.Building.TEMPLE);
			} else if (b < 24) {
				// customs_house, marketplace (2)
				//	Koln, Calais
				p._province.setBuilding(EU3Province.Building.CUSTOMS_HOUSE);
				p._province.setBuilding(EU3Province.Building.MARKETPLACE);
			} else if (b < 25) {
				// workshop, marketplace (1)
				//	Calabria 
				p._province.setBuilding(EU3Province.Building.WORKSHOP);
				p._province.setBuilding(EU3Province.Building.MARKETPLACE);
			} else if (b < 26) {
				// workshop, temple (1)
				// 	Vastergotland
				p._province.setBuilding(EU3Province.Building.WORKSHOP);
				p._province.setBuilding(EU3Province.Building.TEMPLE);
			} else if (b < 43) {
				// marketplace, temple (17)
				// Roma, Jylland, Caux, Koblenz, Orleanias, Othe, Champagne, Bourgogne, Lyonnais, Lisboa, London, Yorkshire, Gotland, Poznan, Smaland, Elsass, Trier
			} else if (b < 44) {
				// workshop (1)
				//   Varmland
			} else if (b < 83) {
				// marketplace (39)
				//  coolest port one picks up a shipyard (Venezia)
				if ((!shipyard) && p._province.isPort()) {
					p._province.setBuilding(EU3Province.Building.SHIPYARD);
					shipyard = true;
				}
				p._province.setBuilding(EU3Province.Building.MARKETPLACE);
			} else if (b < 115) {
				// temple (32)
				p._province.setBuilding(EU3Province.Building.TEMPLE);
			} else {
				break;
			}
			
			b++;
		}	
		*/
		
		/*
		// this is the building set version
		int b = 1;
		boolean shipyard = false;
		for (CvProvince p: __coolList) {
			System.out.format("%s:%03d\n", EU3LocalizedText.getName("PROV" + p._province._id), Math.round(p._chPop + p._advances + p._improvements));
			// roughly distribute the normal 1399 or 1453 start buildings...for now, just 1399
			
			if (b < 3) {
				// university, workshop, temple, marketplace (2)
				//	Ferrara, Pisa
				p._province.setBuilding(EU3Province.Building.UNIVERSITY);
				p._province.setBuilding(EU3Province.Building.WORKSHOP);
				p._province.setBuilding(EU3Province.Building.MARKETPLACE);
				p._province.setBuilding(EU3Province.Building.TEMPLE);
			} else if (b < 5) {
				// courthouse, marketplace, textile (2)
				//	Antwerpen, Vlaanderen
				p._province.setBuilding(EU3Province.Building.TEXTILE);
				p._province.setBuilding(EU3Province.Building.COURTHOUSE);
				p._province.setBuilding(EU3Province.Building.MARKETPLACE);
			} else if (b < 8) {				
				// university, marketplace, temple (3)
				//	Ile-de-France, Toulouse, Bohemia
				p._province.setBuilding(EU3Province.Building.UNIVERSITY);
				p._province.setBuilding(EU3Province.Building.MARKETPLACE);
				p._province.setBuilding(EU3Province.Building.TEMPLE);
			} else if (b < 13) {
				// university, marketplace (5)
				// 	Beira, East Anglia, Leipzig, Erfurt, Brabant
				p._province.setBuilding(EU3Province.Building.UNIVERSITY);
				p._province.setBuilding(EU3Province.Building.MARKETPLACE);
			} else if (b < 16) {
				// university, temple (3)
				// Parma, Modena, Romagna 
				p._province.setBuilding(EU3Province.Building.UNIVERSITY);
				p._province.setBuilding(EU3Province.Building.TEMPLE);
			} else if (b < 20) {
				// university (4)
				// Firenze, Siena, Salamanca, Oxfordshire
				p._province.setBuilding(EU3Province.Building.UNIVERSITY);				
			} else if (b < 21) {
				// courthouse, marketplace, tax_assessor, temple (1)
				//	Avignon
				p._province.setBuilding(EU3Province.Building.COURTHOUSE);
				p._province.setBuilding(EU3Province.Building.MARKETPLACE);
				p._province.setBuilding(EU3Province.Building.TAX_ASSESSOR);
				p._province.setBuilding(EU3Province.Building.TEMPLE);
			} else if (b < 22) {
				// workshop, temple, marketplace (1)
				//	Uppland
				p._province.setBuilding(EU3Province.Building.WORKSHOP);
				p._province.setBuilding(EU3Province.Building.MARKETPLACE);
				p._province.setBuilding(EU3Province.Building.TEMPLE);
			} else if (b < 24) {
				// customs_house, marketplace (2)
				//	Koln, Calais
				p._province.setBuilding(EU3Province.Building.CUSTOMS_HOUSE);
				p._province.setBuilding(EU3Province.Building.MARKETPLACE);
			} else if (b < 25) {
				// workshop, marketplace (1)
				//	Calabria 
				p._province.setBuilding(EU3Province.Building.WORKSHOP);
				p._province.setBuilding(EU3Province.Building.MARKETPLACE);
			} else if (b < 26) {
				// workshop, temple (1)
				// 	Vastergotland
				p._province.setBuilding(EU3Province.Building.WORKSHOP);
				p._province.setBuilding(EU3Province.Building.TEMPLE);
			} else if (b < 43) {
				// marketplace, temple (17)
				// Roma, Jylland, Caux, Koblenz, Orleanias, Othe, Champagne, Bourgogne, Lyonnais, Lisboa, London, Yorkshire, Gotland, Poznan, Smaland, Elsass, Trier
			} else if (b < 44) {
				// workshop (1)
				//   Varmland
			} else if (b < 83) {
				// marketplace (39)
				//  coolest port one picks up a shipyard (Venezia)
				if ((!shipyard) && p._province.isPort()) {
					p._province.setBuilding(EU3Province.Building.SHIPYARD);
					shipyard = true;
				}
				p._province.setBuilding(EU3Province.Building.MARKETPLACE);
			} else if (b < 115) {
				// temple (32)
				p._province.setBuilding(EU3Province.Building.TEMPLE);
			} else if (b < 123) {
				// courthouse (8)
				//   Limburg, Ile-de-France, Bourgogne, Toulouse, Avignon, Franken, Nassau, Liege
				p._province.setBuilding(EU3Province.Building.COURTHOUSE);
			} else { 
				break;
			}
			
			b++;
		}	
		*/	
		
		for (int i = 1; i <= 1804; i++) { // 3000 leaves a lot of room for expansion
			if (367 == i) {
				continue; // special handling of the Azores
			}
			
			CvProvince p = CvProvince.search(String.valueOf(i));
			EU3Province eu3P = null;
			if (null != p) {
				p.preWrite();
				eu3P = p._province;	
				eu3P.applyChanges();
				europeCount++;
			} else {
				// load default EU3 version from disk, if we haven't run off the edge.
                                //System.out.println("Looking up file for province " + i);
				String filename = EU3Province.findFileName(String.valueOf(i));
				if (null != filename) {
				    eu3P = new EU3Province(String.valueOf(i));				   
				    eu3P.takeEU3Defaults();
				} else {
					System.out.println("No file found for province: " + i);
					//break;
                                        continue;
				}
			}
			
			// save the owner in the owner list so I can see which countries 
			// really exist.
			__owners.add(eu3P.getOwner());
			
			// and save it in the eu3 list for later printout.
			__eu3List.add(eu3P);	
			
			// and for further reference
			EU3Province.saveProvince(eu3P);
		}
		doAzores(); // azores are 367
		
		System.out.println("provinces in Europe and buildings: " + europeCount);
		for (String building: EU3Province.__buildings) {
			System.out.println(buildingCount.get(building) + " " + building);
		}		
		System.out.println();
	}
	
	static public void writeAll() throws IOException {
		for (EU3Province prov: CvProvince.__eu3List) {
			prov.write(__destPath + File.separatorChar + Analyzer.getSaveFile());
		}
	}
	
	static Map<String, Integer> buildingCount = new HashMap<String, Integer>();
	
	private void preWrite() throws IOException {	
		int pop = ProvinceCvRules.getCitySize(this);
		_province.setCulture(_culture._culture);
		float tax = (float) ProvinceCvRules.getBaseTax(this);
		if (tax>=0) {
			_province.setBaseTax(tax);
			_province.setCKDefaultBaseTax(ProvinceCvRules.getCKDefaultBaseTax(this));
		}
		// test the defaults to see how the buildings go.
//		if (_province._id.equals("361)")) { System.out.println("Cairo"); }
		for (String building: EU3Province.__buildings) {
			BaseField b = _province._root.getBase(building);
			if (null != b) {
				Integer count = buildingCount.get(building);
				if (null == count) {
					count = new Integer(1);
				} else {
					count = new Integer(count.intValue() + 1);
				}
				buildingCount.put(building, count);
			}
		}		
		
		_province.setBuildings(ProvinceCvRules.getImprovements(_counties));
		_province.setCitySize(pop); 
		if (_controller!=_owner)
			if (CvCountry.isRemoved(_controller._tag)) _controller=_owner;
		if (_controller!=_owner)
			System.out.format("Contested province %s : owner %s, controller %s\n",_id,_owner._eu3Tag,_controller._eu3Tag);
		_province.setController(_controller._eu3Tag);
		_province.setOwner(_owner._eu3Tag);
		for (CvCountry c : _core)
			if (c._eu3Tag!=null)
			    _province.setCore(c._eu3Tag);
		int mp = (int)ProvinceCvRules.getBaseManpower(this);
		if (mp>=0) _province.setManpower(mp); 
		_province.setReligion(ProvinceCvRules.getReligion(_counties));
	}
	
	static public void assignAll() {
		for (CvProvince p : __list) {
			p.assign();
		}
	}

	private void assign() {
		//ownership comes from county  => title => holder
		//control comes from   country => controlled
		//ownership is easy : just count who has the best claim
		//control is trickier: if the nominal owner is losing a war
		//   against a third party which has yet no claim in the
		//   province, the third party claim may not be high enough
		//   to claim the province, while the nominal owner claim
		//   may have been lowered sufficiently to lose control.
		//   In that case, a country at peace with a low claim
		//   might get control. Worse, it'll get control over a
		//   province it doesn't hold while it is not even at war
		//   against the owner => tricky situation...
		//To avoid such situations, the controller has to have
		//control of at least one of the owner county.
		CvCountry[] countriesC = new CvCountry[20];
		float[]     weightsC   = new float[20];
		CvCountry[] countriesO = new CvCountry[20];
		float[]     weightsO   = new float[20];
		int         nC=0;
		int         nO=0;
		CvCountry[] contenders = new CvCountry[20];
		int         nbContenders = 0;
		
		for (County x : _counties) {
			CvCountry o = CvCountry.getCountyOwner(x);
			CvCountry c = CvCountry.getCountyController(x);
			_core.add(o);
			int i;
			for (i=0; i<nC && countriesC[i]!=c; i++);
			if (i==nC) countriesC[nC++]=c;
			weightsC[i] += MergeRules.countyWeight(x);
			for (i=0; i<nO && countriesO[i]!=o; i++);
			if (i==nO) countriesO[nO++]=o;
			weightsO[i] += MergeRules.countyWeight(x);
		}
		
		float maxO=-1000000000;
		int   imaxO=-1;
		for (int i=0; i<nO; i++) if (weightsO[i]>maxO) { maxO=weightsO[i]; imaxO=i; }
		_owner = countriesO[imaxO];
		
		//find who can compete for control
		for (County x : _counties) {
			CvCountry o = CvCountry.getCountyOwner(x);
			CvCountry c = CvCountry.getCountyController(x);
		    if (o==_owner && c!=_owner)
		    	contenders[nbContenders++]=c;
		}
		
		if (nbContenders==0)
			_controller = _owner;
		else {
			float maxC=-1000000000;
			int   imaxC=-1;
			for (int i=0; i<nC; i++) { 
				boolean player=false;
				for (int j=0; j<nbContenders && !player; j++)
					if (countriesC[i]==contenders[j])
						player=true;
				if (!player) continue;
				if (weightsC[i]>maxC) {
					maxC=weightsC[i];
					imaxC=i;
				}
			}
			_controller = countriesC[imaxC];
		}
		
        _owner._provinces.add(this);
        if (_owner._main._capital!=null && _counties.contains(_owner._main._capital)) {
        	_isCapital = true;
        	_owner._capital = this;
        }
	}
	
	static public void checkAllCulture() {
		for (CvProvince p : __list) {
			if (p._isCapital) p.checkCulture();
		}
		for (CvProvince p : __list) {
			if (!p._isCapital) p.checkCulture();
		}
	}
	
	private void checkCulture() {
		CultureCounter counter = new CultureCounter();
		for (County c : _counties) {
			EU3Culture culture = CultureCvRules.convertCulture(c._baseCulture,c._culture, (_isCapital || _owner._capital==null) ? null : _owner._capital._culture);
	        if (culture==null) culture=CultureCvRules.EU3Culture.search(_province._culture);
	        counter.add(culture,c._income);
		}
		_culture = counter.getCulture();
	}
	
	private CvProvince(ProvinceCvRules data) {
		_id       = data._id;
		_idNum    = -1;
		_idNum = Integer.parseInt(_id);
		_data     = data;
		_counties = new FieldSet<County>(County.class);
		_core     = new TreeSet<CvCountry>();
		try {
		    _province = new EU3Province(_id);
		}
		catch (Exception e) {
			System.out.println("unable to load EU3 province "+_id);
			e.printStackTrace();
			System.exit(0);
		}
	}
	
	static private void doAzores() throws IOException {
		//check Portugal exists and owns one of it's coastal province
		//otherwise, the Azores are supposed to be for the taking.
		//also, only in vanilla or NA (undiscovered in IN)
		CvCountry por = CvCountry.search("POR");
		EU3Province p = new EU3Province("367");		
                p.takeEU3Defaults();
		p.setController(null);
		p.setOwner(null);
		p.setCitySize(-1);
		
		
		__eu3List.add(p);
	}

	private CvProvince(String id) {
		_id  = id;
		_idNum = Integer.parseInt(_id);
	}
	
	private CvProvince(int idNum) {
		_idNum = idNum;
		_id = String.valueOf(_idNum);
	}

	static public CvProvince search(String id) {
		return search(id,__list);
	}
	static public CvProvince search(String id, SortedSet<CvProvince> list) {
		int idNum = Integer.parseInt(id);
		CvProvince dummy0 = new CvProvince(idNum);
		CvProvince dummy1 = new CvProvince(idNum + 1);
		SortedSet<CvProvince> sub = list.subSet(dummy0,dummy1);
		if (sub.isEmpty()) return null;
		return sub.first();
	}

    public int compareTo(CvProvince c) { 
    	return _idNum - c._idNum; 
	}

}
