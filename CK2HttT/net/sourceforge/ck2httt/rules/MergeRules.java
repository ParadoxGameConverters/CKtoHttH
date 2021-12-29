package net.sourceforge.ck2httt.rules;
import java.io.IOException;
import java.util.SortedSet;
import java.util.TreeSet;

import net.sourceforge.ck2httt.ck.Country;
import net.sourceforge.ck2httt.ck.County;
import net.sourceforge.ck2httt.ck.Title;
import net.sourceforge.ck2httt.cv.CvCountry;
import net.sourceforge.ck2httt.pxAnalyzer.PXAdvancedAnalyzer;
import net.sourceforge.ck2httt.pxAnalyzer.PXTree.Field;
import net.sourceforge.ck2httt.pxAnalyzer.PXTree.StructField;
import net.sourceforge.ck2httt.utils.OptionSection;


public class MergeRules {

	
	static public CountryCvRules checkCountryStatus(Country c, TreeSet<CvCountry> lieges, boolean mergedLiege) {
		CountryCvRules r = CountryCvRules.getCountryCvRules(c._title._tier);
		Country liege = c._liege;
		r.checkMergedLiege(mergedLiege);
		r.checkWeak(liege._tag);
		r.checkCrown(c._tag);
		r.checkLiegePiety(liege._holder._piety);
		r.checkVassalPiety(c._holder._piety);
		r.checkPrestige(liege._holder._prestige,c._holder._prestige);
		r.checkGold(liege._holder._gold,c._holder._gold);
		r.checkOwnCrowns(c._holder.independentCrowns(liege._holder));
		r.checkLiegeTraits(liege._holder.getStats());
		r.checkRulerTraits(c._holder.getStats());
		r.checkLoyalty(c._holder._loyalty);
		if (c._capital!=null) {
			boolean hasKingdom = (c._capital._kingdom==null) ? false : liege._holder._titles.contains(c._capital._kingdom);
			boolean hasDuchy   = (c._capital._duchy==null) ? false : liege._holder._titles.contains(c._capital._duchy);
		    r.checkLiegeCrowns(hasKingdom,hasDuchy);
		}
		r.checkSize(c.ownedSize());
		CvCountry cvLiege = CvCountry.search(lieges,(mergedLiege) ? liege._liege._tag : liege._tag);
		CultureCounter cc = new CultureCounter(c,false);
		r.checkCulture(
				!isIn(cvLiege._cultures,c._holder._culture),
				!isIn(cvLiege._cultures,(c._capital!=null) ? c._capital._culture : null),
				cc.ratioByNumber(cvLiege._cultures),
				cc.ratioByMoney(cvLiege._cultures)
				);
		return r;
	}
	
	//NOTE : the culture determination here is for county assignment and
	//EU3 Country creation ; it is not related to the final EU3 country cultures
	static public String[] getCultures(Country c) {
		CultureCounter cc = new CultureCounter();
		for (County p : c.allProvinces())
			cc.add(p);
		String[] x = new String[4];
		int n=0;
		if (CountryCvRules._cultureKing)
			n = addTo(x,c._holder._culture,n);
		if (CountryCvRules._cultureCapital && c._capital!=null)
			n = addTo(x,c._capital._culture,n);
		if (CountryCvRules._cultureMajority)
			n = addTo(x,cc.maxByNumber(),n);
		if (CountryCvRules._cultureRichest)
			n = addTo(x,cc.maxByMoney(),n);
		String[] r = new String[n];
		System.arraycopy(x,0, r, 0, n);
		return r;		
	}
	
	static public int addTo(String[] l, String v, int n) {
		for (int i=0; i<n; i++) {
			if (l[i].equals(v)) return n;
		}
		l[n]=v;
		return n+1;
	}
	

	/**
	 * Gets that province weight for deciding country assignment
	 */
	static public float countyWeight(County p) {
		float value=(ProvinceCvRules._weightMoney) ? p._income : 1;
		if (p._isCapital) {
			ProvinceCvRules.ProvinceRule r = 
				(p._owner._title._tier==Title.Tier.COUNTY) ? ProvinceCvRules._countyCapital :
				(p._owner._title._tier==Title.Tier.DUCHY) ?  ProvinceCvRules._duchyCapital :
				ProvinceCvRules._kingdomCapital;
			value *= r._mult;
			value += r._add;
			value += r._weight*((ProvinceCvRules._weightMoney) ? p._owner._income : p._owner._owned.size());
		}
		Field<?> f = p._base.get("improvements");
		if (f!=null && f instanceof StructField)
		    for (Field<?> x : ((StructField)f)._data) {
		    	float w=ProvinceCvRules._improvementsOther;
		    	try {w = ProvinceCvRules._improvements.search(x.name());}
		    	catch (Exception ignore) {} //no specific data for that improvement
		    	value += w;
		    }
		return value;
	}
	
	/**
	 * Utility : checks whether x is in v
	 * @param String
	 * @return
	 */
	static public boolean isIn(String[] v, String x) {
		if (x==null) return false;
		for (String s : v)
			if (s.equals(x)) return true;
		return false;
	}
	
	static public void load(String EU3path, String AltPath, String fname) throws IOException {
		PXAdvancedAnalyzer a = new PXAdvancedAnalyzer(fname,true);
		StructField root = a.analyze();
		OptionSection.setOption(OptionSelectorPanel.getOption(root.getStruct("selection")));
		CountryCvRules.loadReligionRules(root.getStruct("religions"));
		CountryCvRules.loadCountryRules(root.getStruct("country_convert_rules"));
		CountryCvRules.loadCountryConvertionRules(root.getStruct("country_conversion"));
		CultureCvRules.load(EU3path,AltPath,root);
		TagCvRules.load(root);
		ProvinceCvRules.loadAll(root);
		CharacterCvRules.loadRules(root.getStruct("character"));
	}

	static private class CultureCounter {
		private class CultureInfo implements Comparable<CultureInfo> {
		    String _culture;
		    int    _nb;
		    float  _total;
			private CultureInfo(String c) { _culture=c; }
			public int compareTo(CultureInfo cc) { return _culture.compareTo(cc._culture); }
		}
		private TreeSet<CultureInfo> _list;
		public CultureCounter()  { _list=new TreeSet<CultureInfo>(); }
		public CultureCounter(Country c, boolean restricted)  {
			this();
			if (restricted) for (County p : c._owned)         add(p);
			else            for (County p : c.allProvinces()) add(p);
		}
		public void add(County p) {
			CultureInfo dummy0 = new CultureInfo(p._culture);
			CultureInfo dummy1 = new CultureInfo(p._culture+"\0");
			SortedSet<CultureInfo> sub = _list.subSet(dummy0, dummy1);
			if (sub.isEmpty()) { dummy0._nb=1; dummy0._total+=p._income; _list.add(dummy0); }
			else  { sub.first()._nb++; sub.first()._total+=p._income; }
		}
		public int ratioByNumber(String[] accepted) {
			int total=0;
			int nb=0;
			for (CultureInfo x : _list) {
				total+=x._nb;
				for (String c : accepted) { if (c.equals(x._culture)) { nb+=x._nb; break; } }
			}
			return (total==0) ? 100 : (nb*100)/total;
		}
		public int ratioByMoney(String[] accepted) {
			float total=0;
			float nb=0;
			for (CultureInfo x : _list) {
				total+=x._total;
				for (String c : accepted) { if (c.equals(x._culture)) { nb+=x._total; break; } }
			}
			return (total==0) ? 100 : (int)((nb*100)/total);
		}
		public String maxByNumber() {
			CultureInfo cc=null;
			for (CultureInfo x : _list) if (cc==null || x._nb>cc._nb) cc=x;
			return (cc!=null) ? cc._culture : null;
		}
		public String maxByMoney() {
			CultureInfo cc=null;
			for (CultureInfo x : _list) if (cc==null || x._total>cc._total) cc=x;
			return (cc!=null) ? cc._culture : null;
		}
	}
}
