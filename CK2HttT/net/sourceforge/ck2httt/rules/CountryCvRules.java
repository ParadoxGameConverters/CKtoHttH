/**
 * 
 */
package net.sourceforge.ck2httt.rules;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

import net.sourceforge.ck2httt.ck.Characters;
import net.sourceforge.ck2httt.ck.Country;
import net.sourceforge.ck2httt.ck.County;
import net.sourceforge.ck2httt.ck.Title;
import net.sourceforge.ck2httt.pxAnalyzer.PXTree.BaseData;
import net.sourceforge.ck2httt.pxAnalyzer.PXTree.BaseField;
import net.sourceforge.ck2httt.pxAnalyzer.PXTree.Field;
import net.sourceforge.ck2httt.pxAnalyzer.PXTree.ListField;
import net.sourceforge.ck2httt.pxAnalyzer.PXTree.StructField;
import net.sourceforge.ck2httt.utils.FieldLoadable;
import net.sourceforge.ck2httt.utils.FieldSet;
import net.sourceforge.ck2httt.utils.KeyString;
import net.sourceforge.ck2httt.utils.OptionSection;

public class CountryCvRules {
	
	
	//RULES FOR AFFECTING COUNTRIES
	static public  final int MERGE=205;
	static public  final int UNION=204;
	static public  final int VASSAL=203;
	static public  final int ALLY=202;
	static public  final int MARRIAGE=201;
	static private final int DUCHY=2;
	static private final int COUNTY=1;
	
	private int        _mod;
	private char       _tier;
	private int        _min;
	private int        _max;
	private CountryCvRules.BaseRule[] _rules = new CountryCvRules.BaseRule[50];
	private int        _nb = 0;
	
	static private TreeMap<String,CountryCvRules.BaseRule>  _modifCrowns    = new TreeMap<String,CountryCvRules.BaseRule>();
	static private TreeMap<String,CountryCvRules.BaseRule>  _weakCrowns     = new TreeMap<String,CountryCvRules.BaseRule>();
	static private TreeMap<String,CountryCvRules.BaseRule>  _liegeTraits    = new TreeMap<String,CountryCvRules.BaseRule>();
	static private TreeMap<String,CountryCvRules.BaseRule>  _rulerTraits    = new TreeMap<String,CountryCvRules.BaseRule>();
	static private TreeMap<Integer,CountryCvRules.BaseRule> _ownCrownsC     = new TreeMap<Integer,CountryCvRules.BaseRule>();
	static private TreeMap<Integer,CountryCvRules.BaseRule> _ownCrownsD     = new TreeMap<Integer,CountryCvRules.BaseRule>();
	static private TreeMap<String,CountryCvRules.BaseRule>  _liegeCrowns    = new TreeMap<String,CountryCvRules.BaseRule>();
	static private TreeMap<Integer,CountryCvRules.BaseRule> _loyaltyC       = new TreeMap<Integer,CountryCvRules.BaseRule>();
	static private TreeMap<Integer,CountryCvRules.BaseRule> _loyaltyD       = new TreeMap<Integer,CountryCvRules.BaseRule>();
	static private TreeMap<Integer,CountryCvRules.BaseRule> _cultureC       = new TreeMap<Integer,CountryCvRules.BaseRule>();
	static private TreeMap<Integer,CountryCvRules.BaseRule> _cultureD       = new TreeMap<Integer,CountryCvRules.BaseRule>();
	static private TreeMap<Integer,CountryCvRules.BaseRule> _sizeC          = new TreeMap<Integer,CountryCvRules.BaseRule>();
	static private TreeMap<Integer,CountryCvRules.BaseRule> _sizeD          = new TreeMap<Integer,CountryCvRules.BaseRule>();
	static private TreeMap<Integer,CountryCvRules.BaseRule> _liegePietyC    = new TreeMap<Integer,CountryCvRules.BaseRule>();
	static private TreeMap<Integer,CountryCvRules.BaseRule> _liegePietyD    = new TreeMap<Integer,CountryCvRules.BaseRule>();
	static private TreeMap<Integer,CountryCvRules.BaseRule> _vassalPietyC   = new TreeMap<Integer,CountryCvRules.BaseRule>();
	static private TreeMap<Integer,CountryCvRules.BaseRule> _vassalPietyD   = new TreeMap<Integer,CountryCvRules.BaseRule>();
	static private TreeMap<Integer,CountryCvRules.BaseRule> _prestigeRatioC = new TreeMap<Integer,CountryCvRules.BaseRule>();
	static private TreeMap<Integer,CountryCvRules.BaseRule> _prestigeRatioD = new TreeMap<Integer,CountryCvRules.BaseRule>();
	static private TreeMap<Integer,CountryCvRules.BaseRule> _goldRatioC     = new TreeMap<Integer,CountryCvRules.BaseRule>();
	static private TreeMap<Integer,CountryCvRules.BaseRule> _goldRatioD     = new TreeMap<Integer,CountryCvRules.BaseRule>();
	static private TreeMap<Integer,Integer>  _result      = new TreeMap<Integer,Integer>();
	static private CountryCvRules.BaseRule _rulerCulture;
	static private CountryCvRules.BaseRule _capitalCulture;
	static private int _countyMod;
	static private int _duchyMod;
	static private int _kingdomMod;  //rare, but kingdoms can be vassals (Jerusalem)
	static private int _prestigeRadius;
	static private int _goldRadius;
	static boolean _cultureKing;
	static boolean _cultureCapital;
	static boolean _cultureMajority;
	static boolean _cultureRichest;
	static private CountryCvRules.BaseRule _mergedLiege;
	
	static String _jewish;
	static String _jewishGroup;
	static String _pagan;
	static String _paganGroup;
		
	static private int makeValue(String v) {
		if      (v.equals("merge"))    return MERGE;
		else if (v.equals("union"))    return UNION;
		else if (v.equals("vassal"))   return VASSAL;
		else if (v.equals("ally"))     return ALLY;
		else if (v.equals("marry"))    return MARRIAGE;
		else                           return (new Integer(v.substring(1))).intValue();

	}
			
	static private void load(Field<?> f, TreeMap<String,CountryCvRules.BaseRule> map) {
		if (f instanceof StructField)
			for (Field<?> c : ((StructField)f)._data) {
				CountryCvRules.BaseRule r = new BaseRule((StructField)c);
				char x='X';
				switch (r._tier) {
				case 2: x='D'; break;
				case 1: x='C'; break;
				}
				if (x!='X')
					map.put(x+c._name._value, r);
				else {
					map.put('D'+c._name._value, r);
					map.put('C'+c._name._value, r);						
				}
			}
	}
	static private void load(Field<?> f, TreeMap<Integer,CountryCvRules.BaseRule> mapC, TreeMap<Integer,CountryCvRules.BaseRule> mapD) {
		if (f instanceof StructField)
			for (Field<?> c : ((StructField)f)._data) {
				if (!Character.isDigit(c.name().charAt(0))) continue;
				CountryCvRules.BaseRule r = new BaseRule((StructField)c);
				char x='X';
				switch (r._tier) {
				case 2: x='D'; break;
				case 1: x='C'; break;
				}
				Integer n = new Integer(c.name());
				if (x=='C')
					mapC.put(n, r);
				else if (x=='D')
					mapD.put(n, r);
				else {
					mapC.put(n, r);
					mapD.put(n, r);						
				}
			}
	}		
	static private void loadResult(Field<?> f, TreeMap<Integer,Integer> map) {
		for (Field<?> c : ((StructField)f)._data) {
			BaseField x = (BaseField)c;
			map.put(new Integer(x.name()),new Integer(makeValue(x.get())));
		}
	}
	
	static public void loadCultureRules(ListField f) {
		for (BaseData b : f._data) {
			if (b._value.equals("king"))              _cultureKing=true;
			if (b._value.equals("capital"))           _cultureCapital=true;
			if (b._value.equals("major_by_income"))   _cultureRichest=true;
			if (b._value.equals("major_by_province")) _cultureMajority=true;
		}
		
	}

	static public void loadReligionRules(StructField f) {
		for (Field<?> x : f._data) {
			if (x.name().equals("jewish")) {
				StructField xx = (StructField)x;
				BaseField y = (BaseField)xx._data.getFirst();
				_jewishGroup = y.name();
				_jewish = y.get();
			}
			if (x.name().equals("pagan")) {
				StructField xx = (StructField)x;
				BaseField y = (BaseField)xx._data.getFirst();
				_paganGroup = y.name();
				_pagan = y.get();
			}
		}
	}

	static public void loadCountryRules(StructField root) throws IOException {
		for (Field<?> f : root._data) {
			String name = f.name();
			if      (name.equals("weak_crown"))       load(f,_weakCrowns);
			else if (name.equals("country_modifier")) load(f,_modifCrowns);
			else if (name.equals("liege_traits"))     load(f,_liegeTraits);
			else if (name.equals("own_crowns"))       load(f,_ownCrownsC,_ownCrownsD);
			else if (name.equals("liege_crowns"))     load(f,_liegeCrowns);
			else if (name.equals("loyalty"))          load(f,_loyaltyC,_loyaltyD);
			else if (name.equals("vassal_piety"))     load(f,_vassalPietyC,_vassalPietyD);
			else if (name.equals("liege_piety"))      load(f,_liegePietyC,_liegePietyD);
			else if (name.equals("prestige"))         load(f,_prestigeRatioC,_prestigeRatioD);
			else if (name.equals("gold"))             load(f,_goldRatioC,_goldRatioD);
			else if (name.equals("traits"))           load(f,_rulerTraits);
			else if (name.equals("size"))             load(f,_sizeC,_sizeD);
			else if (name.equals("effect"))           loadResult(f,_result);
			else if (name.equals("culture")) {
				load(f,_cultureC,_cultureD);
				StructField g = (StructField)f;
				StructField x = g.getStruct("ruler");
				if (x!=null)  _rulerCulture = new BaseRule(x);
				StructField y = g.getStruct("capital");
				if (y!=null)  _capitalCulture = new BaseRule(y);
			}
			else if (name.equals("tier")) {
				BaseField x = (BaseField)f.get("county");
				if (x!=null) _countyMod = x.getAsInt();
				BaseField y = (BaseField)f.get("duchy");
				if (y!=null) _duchyMod = y.getAsInt();
				BaseField z = (BaseField)f.get("kingdom");
				if (z!=null) _kingdomMod = y.getAsInt();
			}
			else if (name.equals("merged_liege")) {
				_mergedLiege = new BaseRule(f);
			}
			else if (name.equals("gold_radius")) {
				_goldRadius = ((BaseField)f).getAsInt();
			}
			else if (name.equals("prestige_radius")) {
				_prestigeRadius = ((BaseField)f).getAsInt();
			}
			else if (name.equals("accepted_cultures")) {
			    loadCultureRules(root.getList("accepted_cultures"));
			}
		}
	}
	
	static public CountryCvRules getCountryCvRules(Title.Tier tier) {
		CountryCvRules r = new CountryCvRules();
		r._mod  = (tier==Title.Tier.COUNTY) ? _countyMod : (tier==Title.Tier.DUCHY) ? _duchyMod : _kingdomMod;
		r._tier = (tier==Title.Tier.COUNTY) ? 'C' : 'D';  //we don't bother having specific modifiers for kingdoms ; they'll use duchies ones
		r._max=MERGE;
		r._min=0;
		return r;
	}
    public void checkWeak(String liege) {
    	CountryCvRules.BaseRule r = _weakCrowns.get(_tier+liege);
    	if (r!=null) _rules[_nb++]=r;
    }
    public void checkCrown(String self) {
    	CountryCvRules.BaseRule r = _modifCrowns.get(_tier+self);
    	if (r!=null) _rules[_nb++]=r;
    }
    public void checkMergedLiege(boolean mergedLiege) {
    	if (mergedLiege && _mergedLiege!=null)
    		_rules[_nb++]=_mergedLiege;
    }

    public void checkLiegeTraits(Characters.Stats liege) {
	 	for (BaseField f : liege.getTraits()) {
	  		CountryCvRules.BaseRule r = _liegeTraits.get(_tier+f.name());
	    	if (r!=null) _rules[_nb++]=r;
	  	}
    }

    public void checkRulerTraits(Characters.Stats ruler) {
		for (BaseField f : ruler.getTraits()) {
			CountryCvRules.BaseRule r = _rulerTraits.get(_tier+f.name());
		    if (r!=null) _rules[_nb++]=r;
		}
    }

    public void checkLiegeCrowns(boolean hasKingdom, boolean hasDuchy) {
   		CountryCvRules.BaseRule x = _liegeCrowns.get(_tier+"not_king");
   		CountryCvRules.BaseRule y = _liegeCrowns.get(_tier+"not_duke");
    	if (!hasKingdom && x!=null) _rules[_nb++]=x;
    	if (!hasDuchy && y!=null && _tier=='C') _rules[_nb++]=y;
    }
    public void checkLoyalty(int loyalty) {
    	if (loyalty==100) return;
    	SortedMap<Integer,CountryCvRules.BaseRule> x = (_tier=='C') ? _loyaltyC : _loyaltyD;
    	SortedMap<Integer,CountryCvRules.BaseRule> view = x.tailMap(loyalty);
    	if (!view.isEmpty()) _rules[_nb++] = view.get(view.firstKey());
    }
    public void checkLiegePiety(int piety) {
    	SortedMap<Integer,CountryCvRules.BaseRule> x = (_tier=='C') ? _liegePietyC : _liegePietyD;
    	SortedMap<Integer,CountryCvRules.BaseRule> view = x.headMap(piety+1);
    	if (!view.isEmpty()) _rules[_nb++] = view.get(view.lastKey());
    }
    public void checkVassalPiety(int piety) {
    	SortedMap<Integer,CountryCvRules.BaseRule> x = (_tier=='C') ? _vassalPietyC : _vassalPietyD;
    	SortedMap<Integer,CountryCvRules.BaseRule> view = x.headMap(piety+1);
    	if (!view.isEmpty()) _rules[_nb++] = view.get(view.lastKey());
    }
    public void checkPrestige(int liege, int vassal) {
    	SortedMap<Integer,CountryCvRules.BaseRule> x = (_tier=='C') ? _prestigeRatioC : _prestigeRatioD;
    	if (liege<=0)  _rules[_nb++] = x.get(x.lastKey());
    	if (vassal<=0) _rules[_nb++] = x.get(x.firstKey());
    	if (liege<=0 || vassal<=0) return;
    	if (liege*liege+vassal*vassal<_prestigeRadius*_prestigeRadius) return;
    	SortedMap<Integer,CountryCvRules.BaseRule> view = x.headMap((100*vassal)/liege);
    	if (!view.isEmpty()) _rules[_nb++] = view.get(view.lastKey());
    }
    public void checkGold(int liege, int vassal) {
    	SortedMap<Integer,CountryCvRules.BaseRule> x = (_tier=='C') ? _goldRatioC : _goldRatioD;
    	if (liege<=0)  _rules[_nb++] = x.get(x.lastKey()); 
    	if (vassal<=0) _rules[_nb++] = x.get(x.firstKey());
    	if (liege<=0 || vassal<=0) return; 
    	if (liege*liege+vassal*vassal<_goldRadius*_goldRadius) return;
    	SortedMap<Integer,CountryCvRules.BaseRule> view = x.headMap((100*vassal)/liege);
    	if (!view.isEmpty()) _rules[_nb++] = view.get(view.lastKey());
    }
    public void checkOwnCrowns(int crowns) {
    	SortedMap<Integer,CountryCvRules.BaseRule> x = (_tier=='C') ? _ownCrownsC : _ownCrownsD;
    	SortedMap<Integer,CountryCvRules.BaseRule> view = x.headMap(crowns+1);
    	if (!view.isEmpty()) _rules[_nb++]=view.get(view.lastKey());
    }
    public void checkSize(int size) {
    	SortedMap<Integer,CountryCvRules.BaseRule> x = (_tier=='C') ? _sizeC : _sizeD;
    	SortedMap<Integer,CountryCvRules.BaseRule> view = x.headMap(size+1);
    	if (!view.isEmpty()) _rules[_nb++]=view.get(view.lastKey());
    }
    public void checkCulture(boolean rulerFails, boolean capitalFails, int ratioByProvince, int ratioByMoney) {
    	if (rulerFails && _rulerCulture!=null)     _rules[_nb++]=_rulerCulture;
    	if (capitalFails && _capitalCulture!=null) _rules[_nb++]=_capitalCulture;
    	if (_cultureMajority || _cultureRichest) {
    	    SortedMap<Integer,CountryCvRules.BaseRule> x = (_tier=='C') ? _cultureC : _cultureD;
    	    int r=0;
    	    if (_cultureMajority) r=ratioByProvince;
    	    if (_cultureRichest)  r+=ratioByMoney;
    	    if (_cultureMajority && _cultureRichest) r/=2;
    	    SortedMap<Integer,CountryCvRules.BaseRule> view = x.headMap(r+1);
    	    if (!view.isEmpty()) _rules[_nb++]=view.get(view.lastKey());
    	}
    }
    
    public int getRelation(int mod) {
    	for (int i=0; i<_nb; i++)
    		_mod+=_rules[i]._modifier;
    	checkMinMax();
    	SortedMap<Integer,Integer> view = _result.headMap(_mod+1-mod);
    	int r=MERGE;
    	if (!view.isEmpty())
    	    r = view.get(view.lastKey()).intValue();
    	if (r<=_min) return _min;
    	if (r>=_max) return _max;
    	return r;
    }
    
    private void checkMinMax() {
    	if (_nb==0) return;
    	TreeSet<Integer> min = new TreeSet<Integer>();
    	TreeSet<Integer> max = new TreeSet<Integer>();
    	for (int i=0; i<_nb; i++) {
    		min.add((_rules[i]._relationMin<<8)+_rules[i]._priority);
    		max.add((_rules[i]._relationMax<<8)+_rules[i]._priority);
    	}
    	int M,m;
    	do {
    		int MM = max.first();
    		int mm = min.last();
    		M=MM>>8;
    		m=mm>>8;
    		if (M<m) {
    			if ((mm&0xff)<(MM&0xff)) min.remove(mm);
    			else                     max.remove(MM);
    		}
    	}
    	while (M<m && !max.isEmpty() && !min.isEmpty());
    	if (!min.isEmpty()) _min=m;
    	if (!max.isEmpty()) _max=M;
    }

	
	static private class BaseRule {
		public int _modifier=0;
		public int _relationMin=0;
		public int _relationMax=MERGE;
		public int _tier=3;
		public int _priority=0;
		private BaseRule(Field<?> g) { //in case of an empty rule
			if (g instanceof StructField) {
				StructField f = (StructField)g;
				BaseField x;
				x = f.getBase("mod");
				if (x!=null) _modifier=x.getAsInt();
				x = f.getBase("best");
				if (x!=null)
					_relationMax=makeValue(x.get());
				x = f.getBase("worst");
				if (x!=null)
					_relationMin=makeValue(x.get());
				x = f.getBase("tier");
				if (x!=null) {
					String v = x.get();
					if      (v.equals("duchy"))  _tier=DUCHY;
					else if (v.equals("county")) _tier=COUNTY;
				}
				x = f.getBase("prio");
				if (x!=null)
					_priority = x.getAsInt();
			}
		}
	}
	
	//RULES FOR CONVERTING COUNTRIES

	static void loadCountryConvertionRules(StructField root) throws IOException {
		for (Field<?> f : root._data) {
			String name = f.name();
			if (name.equals("tech_group"))
				TechGroupRule.loadAll(f);
			else if (name.equals("government"))
				GovernmentRule.loadAll(f);
		}
		SliderRule.loadAll(root);
	}
	
	static public class TechGroupRule {
		int    _priority;
		String _culture;
		String _religion;
		String _cultureGroup;
		String _religionGroup;
		String _techGroup;
		static private LinkedList<TechGroupRule> __list = new LinkedList<TechGroupRule>();
		private boolean load(Field<?> x) {
			StructField f = (StructField)x;
			_priority = f.getBase("prio").getAsInt();
			BaseField y,z;
			z = f.getBase("religion");
			if (z!=null) _religion=z.get();
			y = f.getBase("religious_group");
			if (y!=null) _religionGroup=y.get();
			if (y!=null && z!=null) return false;
			z = f.getBase("culture");
			if (z!=null) _culture=z.get();
			y = f.getBase("culture_group");
			if (y!=null) _cultureGroup=y.get();
			if (y!=null && z!=null) return false;
			_techGroup = x.name();
			return true;
		}
		static private void loadAll(Field<?> x) {
			StructField f = (StructField)x;
			for (Field<?> z : f._data) {
				TechGroupRule r = new TechGroupRule();
				r.load(z);
				__list.add(r);
			}
		}
		static public String getTechGroup(String religion, CultureCvRules.EU3Culture culture) {
			String rGroup = getReligiousGroup(religion);
			int p=-1;
			String group=null;
			for (TechGroupRule r : __list) {
				if (r._priority<p) continue;
				if ((r._culture!=null && r._culture.equals(culture._culture) ||
					 r._cultureGroup!=null && r._cultureGroup.equals(culture._group) ||
					 r._culture==null && r._cultureGroup==null) &&
					(r._religion!=null && r._religion.equals(culture._culture) ||
					 r._religionGroup!=null && r._religionGroup.equals(rGroup) ||
					 r._religion==null && r._religionGroup==null)) {
					group=r._techGroup;
					p=r._priority;
				}
			}
			return group;
		}
	}
	
	//translate character religion
	//no problem except muslim : choose between sunni and chiite
	static public String getReligion(Characters.Rulers ruler, String baseReligion) {
		String religion = ruler._religion;
		if (religion.equals("catholic")) return "catholic";
		if (religion.equals("orthodox")) return "orthodox";
		if (religion.equals("jewish"))   return _jewish;
		if (religion.equals("pagan"))    return _pagan;
		if (religion.equals("muslim")) { //complex rule here
			Field<?> traits = ruler.Characters()._base.get("traits");
			//check if we have the shiite or sunni traits
			if (traits!=null && traits instanceof StructField) {
			    if (traits.get("shiite")!=null) return "shiite";
			    if (traits.get("sunni")!=null) return "sunni";
			}
			//else if capital province culture is muslim => religion of capital, else sunni
			if (baseReligion.equals("sunni") || baseReligion.equals("shiite"))
				return baseReligion;
			else
				return "sunni";
		}
		throw new IllegalStateException("an unknown religion was found " + religion);
	}
	
	static private String getReligiousGroup(String s) {
		if (s.equals("catholic")) return "christian";
		if (s.equals("protestant")) return "christian";
		if (s.equals("reformed")) return "christian";		
		if (s.equals("orthodox")) return "christian";
		if (s.equals("shiite"))   return "muslim";
		if (s.equals("sunni"))    return "muslim";
		if (s.equals(_jewish))    return _jewishGroup;
		if (s.equals(_pagan))     return _paganGroup;
		throw new IllegalStateException("an unknown religion was found " + s);
	}
	

	static public class GovernmentRule {
		int    _priority;
		String _culture;
		String _religion;
		String _cultureGroup;
		String _religionGroup;
		String _govType;
		String _ckForm;
		TreeSet<String> _laws = new TreeSet<String>();
		
		public boolean equals(Object r) {
			if (! (r instanceof GovernmentRule)) return false;
			GovernmentRule g=(GovernmentRule)r;
			if (!_govType.equals(g._govType)) return false;
			if (_ckForm!=null && !_ckForm.equals(g._ckForm)) return false;
			if (_priority!=g._priority) return false;
			if (_culture!=null && !_culture.equals(g._culture)) return false;
			if (_religion!=null && !_religion.equals(g._religion)) return false;
			if (_cultureGroup!=null && !_cultureGroup.equals(g._cultureGroup)) return false;
			if (_religionGroup!=null && !_religionGroup.equals(g._religionGroup)) return false;
			return true;
		}
		
		static private class GovernmentRuleSection extends OptionSection {
			GovernmentRuleSection(StructField root) { super(root); }
			public void add(Field<?> data,String section) {
				GovernmentRule.loadList(data);
			}	
			public void remove(Field<?> data,String section) {
				GovernmentRule.deleteList(data);
			}	
			public void replace(Field<?> data,String section) {
				GovernmentRule.deleteList(data);
				GovernmentRule.loadList(data);
			}	
		}
		
		static private LinkedList<GovernmentRule> __list = new LinkedList<GovernmentRule>();
		private boolean load(Field<?> x) {
			StructField f = (StructField)x;
			_priority = f.getBase("prio").getAsInt();
			BaseField y,z;
			z = f.getBase("religion");
			if (z!=null) _religion=z.get();
			y = f.getBase("religious_group");
			if (y!=null) _religionGroup=y.get();
			if (y!=null && z!=null) return false;
			z = f.getBase("culture");
			if (z!=null) _culture=z.get();
			y = f.getBase("culture_group");
			if (y!=null) _cultureGroup=y.get();
			if (y!=null && z!=null) return false;
			z = f.getBase("form");
			if (z!=null) _ckForm=z.get();
			BaseField l[] = f.getAllBase("law");
			if (l!=null)
			    for (BaseField xx : l)
			    	_laws.add(xx.get());
			_govType = x.name();
			return true;
		}
		static private void loadList(Field<?> x) {
			StructField f = (StructField)x;
			for (Field<?> z : f._data) {
				GovernmentRule r = new GovernmentRule();
				r.load(z);
				__list.add(r);
			}
		}
		static private void deleteList(Field<?> x) {
			StructField f = (StructField)x;
			for (Field<?> z : f._data) {
				GovernmentRule r = new GovernmentRule();
				r.load(z);
				__list.remove(r);
			}
		}		
		static public String getGovType(String religion, CultureCvRules.EU3Culture culture, String ckForm, TreeSet<String> laws) {
			String rGroup = getReligiousGroup(religion);
			int p=-1;
			String gov=null;
			for (GovernmentRule r : __list) {
				if (r._priority<p) continue;
				if ((r._culture!=null && r._culture.equals(culture._culture) ||
					 r._cultureGroup!=null && r._cultureGroup.equals(culture._group) ||
					 r._culture==null && r._cultureGroup==null) &&
					(r._religion!=null && r._religion.equals(culture._culture) ||
					 r._religionGroup!=null && r._religionGroup.equals(rGroup) ||
					 r._religion==null && r._religionGroup==null) &&
					 (r._ckForm==null || r._ckForm.equals(ckForm)) &&
					 (r._laws.size()==0 || laws.containsAll(r._laws))) {
					gov=r._govType;
					p=r._priority;
				}
			}
			if (null == gov) {
				System.out.println("null gov type");
			}
			return gov;
		}
		static public void loadAll(Field<?> x) {
			GovernmentRuleSection g = new GovernmentRuleSection((StructField)x);
			g.load("convert");
		}		
	}
	
	static public class SliderRule {
		private FieldSet<BaseSliderRule> _buildingRules  = new FieldSet<BaseSliderRule>(BaseSliderRule.class);
		private FieldSet<BaseSliderRule> _hierarchyRules = new FieldSet<BaseSliderRule>(BaseSliderRule.class);
		private FieldSet<BaseSliderRule> _powerRules     = new FieldSet<BaseSliderRule>(BaseSliderRule.class);
		private FieldSet<BaseSliderRule> _adanceRules    = new FieldSet<BaseSliderRule>(BaseSliderRule.class);
		private FieldSet<BaseSliderRule> _lawRules       = new FieldSet<BaseSliderRule>(BaseSliderRule.class);
		private float _zero;
		private float _step;
		
		static public SliderRule __land_naval                        = new SliderRule();
		static public SliderRule __mercantilism_freetrade            = new SliderRule();
		static public SliderRule __serfdom_freesubjects              = new SliderRule();
		static public SliderRule __centralization_decentralization   = new SliderRule();
		static public SliderRule __innovative_narrowminded           = new SliderRule();
		static public SliderRule __aristocracy_plutocracy            = new SliderRule();
		static public SliderRule __offensive_defensive               = new SliderRule();
		static public SliderRule __quality_quantity                  = new SliderRule();
		
		static public void loadAll(Field<?> x) {
			__land_naval.load(x.get("land_naval"));
			__mercantilism_freetrade.load(x.get("mercantilism_freetrade"));
			__serfdom_freesubjects.load(x.get("serfdom_freesubjects"));
			__centralization_decentralization.load(x.get("centralization_decentralization"));
			__innovative_narrowminded.load(x.get("innovative_narrowminded"));
			__aristocracy_plutocracy.load(x.get("aristocracy_plutocracy"));
			__offensive_defensive.load(x.get("offensive_defensive"));
			__quality_quantity.load(x.get("quality_quantity"));
		}
		
		static public class BaseSliderRule extends KeyString implements FieldLoadable {
			public float _v;
			public BaseSliderRule() {}
			public boolean load(Field<?> x) {
				_v = ((BaseField)x).getAsFloat();
				_key = x.name();
				return true;
			}
		}
		
		public boolean load(Field<?> xx) {
			if (xx==null || !(xx instanceof StructField)) return false;
			StructField x = (StructField)xx;
			Field<?> f = x.get("power");
			if (f!=null) _powerRules.load(f);
			f = x.get("buildings");
			if (f!=null) _buildingRules.load(f);
			f = x.get("hierarchy");
			if (f!=null) _hierarchyRules.load(f);
			f = x.get("advances");
			if (f!=null) _adanceRules.load(f);
			f = x.get("laws");
			if (f!=null) _lawRules.load(f);
			f = x.get("zero");
			if (f!=null) _zero = ((BaseField)f).getAsFloat();
			f = x.get("step");
			if (f!=null) _step = ((BaseField)f).getAsFloat();
			return true;
		}
			
		public int getValue(Collection<County> counties, Country country) {
			float v=0;
			for (County c : counties)
				v+=inspect(c,country._holder);
			v /= counties.size();
			return (int)(v/_step+_zero+law(country));
		}
		
		private float law(Country c) {
			if (_lawRules.size()==0) return 0;
			float v=0;
			for (String law : c._laws) {
				BaseSliderRule r = _lawRules.search(law);
				if (r!=null) v += r._v;
			}
			return v;
		}
		
		private float inspect(County c, Characters.Rulers ruler) {
			float v=0;
			if (_buildingRules.size()!=0)
				for (byte b : c._improvements) {
					BaseSliderRule r = _buildingRules.search(County.Improvements.__array[b]._name);
					if (r!=null) v += r._v;
				}
			if (_adanceRules.size()!=0)
				for (short b : c._advances) {
					BaseSliderRule r = _adanceRules.search(County.Advances.__array[b]);
					if (r!=null) v += r._v;
				}
			if (_hierarchyRules.size()!=0)
				if (c._owner._holder==ruler)
					v += _hierarchyRules.search("direct")._v;
				else if (c._owner._title._tier==Title.Tier.COUNTY && c._owner._liege!=null && c._owner._liege._holder==ruler)
					v += _hierarchyRules.search("count")._v;
				else
					v += _hierarchyRules.search("other")._v;
			if (_powerRules.size()!=0) {
				StructField x = c._base.getStruct("privileges");
				BaseField f = x.getBase("peasants");
				BaseSliderRule r = _powerRules.search("peasants");
				if (f!=null && r!=null) v += f.getAsFloat()*r._v;
				f = x.getBase("burghers");
				r = _powerRules.search("burghers");
				if (f!=null && r!=null) v += f.getAsFloat()*r._v;
				f = x.getBase("clergy");
				r = _powerRules.search("clergy");
				if (f!=null && r!=null) v += f.getAsFloat()*r._v;
				f = x.getBase("nobles");
				r = _powerRules.search("nobles");
				if (f!=null && r!=null) v += f.getAsFloat()*r._v;
			}
			return v;
		}
	}

}
