package net.sourceforge.ck2httt.cv;

import static net.sourceforge.ck2httt.utils.Check.checkFatal;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import net.sourceforge.ck2httt.ck.Analyzer;
import net.sourceforge.ck2httt.ck.Characters;
import net.sourceforge.ck2httt.ck.Country;
import net.sourceforge.ck2httt.ck.County;
import net.sourceforge.ck2httt.ck.Dynasty;
import net.sourceforge.ck2httt.ck.Title;
import net.sourceforge.ck2httt.eu3.EU3Country;
import net.sourceforge.ck2httt.eu3.EU3Province;
import net.sourceforge.ck2httt.rules.CharacterCvRules;
import net.sourceforge.ck2httt.rules.CountryCvRules;
import net.sourceforge.ck2httt.rules.CultureCvRules;
import net.sourceforge.ck2httt.rules.MergeRules;
import net.sourceforge.ck2httt.rules.TagCvRules;
import net.sourceforge.ck2httt.utils.FieldSet;
import net.sourceforge.ck2httt.utils.Rnd;


/**
 * Class translating actual CK countries into a set of countries for EU3
 * 
 * @author yprelot
 */
public class CvCountry implements Comparable<CvCountry> {

	static public List<String> __western = new ArrayList<String>();
	static public List<String> __eastern = new ArrayList<String>();
	static public List<String> __ottoman = new ArrayList<String>();
	static public List<String> __muslim = new ArrayList<String>();
	static public List<String> __indian = new ArrayList<String>();
	static public List<String> __chinese = new ArrayList<String>();
	static public List<String> __sub_saharan = new ArrayList<String>();
	static public List<String> __new_world = new ArrayList<String>();

	static private String     __destPath;
	static private String	  __countriesFile;

	public String                    _tag;
    public Country                   _main;
    public CvCountry                 _liege;
    private FieldSet<County>         _owned;
    private FieldSet<County>         _controlled;
    public TreeSet<CvRelationships>  _relationships;
    public String[]                  _cultures;
    public TreeSet<CvProvince>       _provinces;
    public CvProvince                _capital;
    public String                    _eu3Tag;
    public CharacterCvRules.Leader   _leader  = null; 
    public boolean                   _removed = false;
    public EU3Country                _country;

	
    static public TreeSet<CvCountry> __list    = new TreeSet<CvCountry>();
    static public TreeSet<CvCountry> __removed = new TreeSet<CvCountry>();
    
    static public TreeSet<EU3Country> __eu3List = new TreeSet<EU3Country>();
    
    /*
     * Finds a county controller.
     */
    static public CvCountry getCountyController(County p) {
    	CvCountry x = search(p._controller._tag);
    	if (x!=null && x._controlled.contains(p)) return x;
    	CvCountry y = search(p._controller._liege._tag);
    	if (y!=null && y._controlled.contains(p)) return y;
    	CvCountry z = search(p._controller._liege._liege._tag);
    	if (z!=null && z._controlled.contains(p)) return z;
    	checkFatal(null,"eu3 county controller",p._id);
    	return null;
    }
    
    /*
     * Finds a county owner.
     */
    static public CvCountry getCountyOwner(County p) {
    	CvCountry x = search(p._owner._tag);
    	if (x!=null && x._owned.contains(p)) return x;
    	CvCountry y = search(p._owner._liege._tag);
    	if (y!=null && y._owned.contains(p)) return y;
    	CvCountry z = search(p._owner._liege._liege._tag);
    	if (z!=null && z._owned.contains(p)) return z;
    	checkFatal(null,"eu3 county owner",p._id);
    	return null;
    }
    
    /*
     * remove countries with no county ; likely an country still existing
     * because it has roaming armies.
     */
    static public TreeSet<CvCountry> purgeEmpty() {
		for (CvCountry c : __list)
			if (c._controlled.isEmpty() && c._owned.isEmpty() && !c._tag.equals("REBE")) {
				System.out.format("removed %s due to lack of territory (no counties)\n",c._tag);
				__removed.add(c);
				c._removed = true;
			}
		__list.removeAll(__removed);
		return __removed;
    }
    
    /*
     * remove countries with no province (after province assignment).
     */
    static public TreeSet<CvCountry> purgeNoProvince() {
		for (CvCountry c : __list)
			if (c._provinces.isEmpty() && !c._tag.equals("REBE")) {
				System.out.format("removed %s because it did not qualify for any province%n",c._tag);
				__removed.add(c);
				c._removed = true;
		    }
		__list.removeAll(__removed);
		return __removed;
    }
    
    /*
     * Sanity check that all CK provinces are known
     */
    static private void sanityCheck() {
		int n=0;
		int m=0;
		for (CvCountry c : __list) {
			n+=c._controlled.size();
			m+=c._owned.size();
		}
		if (n!=County.__list.size()) {
		    System.out.format("%d counties found (control) : %d were expected\n", n, County.__list.size());
			for (CvCountry c : __list)
				County.__list.removeAll(c._controlled);
			System.out.println("counties not found are");
			for (County c : County.__list)
				System.out.println(c._id);
		    System.exit(0);
		}
		if (m!=County.__list.size()) {
		    System.out.format("%d counties found (ownership) : %d were expected\n", m, County.__list.size());
			for (CvCountry c : __list)
				County.__list.removeAll(c._owned);
			System.out.println("counties not found are");
			for (County c : County.__list)
				System.out.println(c._id);
		    System.exit(0);
		}
    }
    
    /*
     * Sanity check that all CK countries have correct vassalization links.
     * Still, we don't halt the program but remove the vassalization link
     */
    static private void CKsanityCheck() {
		for (Country x : Country.__list) {
			Title.Tier myTier=x._title._tier;
			if (x._holder._main._liege==null) continue;
			Title.Tier liegeTier=x._liege._title._tier;
			if (myTier==Title.Tier.KINGDOM && x._tag.equals("JERU") && liegeTier==Title.Tier.KINGDOM) continue;
			if (myTier==Title.Tier.DUCHY && liegeTier==Title.Tier.KINGDOM) continue;
			if (myTier==Title.Tier.COUNTY && liegeTier!=Title.Tier.COUNTY) continue;
		    System.out.println("("+x._tag+", rank "+myTier.toString()+") has liege ("+x._liege._tag+", rank "+liegeTier.toString()+") : ranks are not consistent ; removing liege");
		    if (myTier==Title.Tier.KINGDOM) {
		    	x._holder._main._liege = null;
		    	x._liege = null;
			System.out.println("("+x._tag+", rank "+myTier.toString()+") made independent");
		    } else {
		    	x._holder._main._liege = x._holder._main._liege._holder._main._liege;
		    	x._liege = x._liege._liege;
		    	if (x._holder._main._liege==null) {
				    System.out.println("("+x._tag+", rank "+myTier.toString()+") made independent");		    	 
		    	} else {
		    		System.out.println("("+x._tag+", rank "+myTier.toString()+") liege moved to ("+x._liege._tag+", rank "+liegeTier.toString()+")");
		    	}
		    }
		}
    }    
	
    static public void setCountriesFile(String file) {
    	__countriesFile = file;
    }
    
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
	
	static public void makeCountries() {
		CKsanityCheck();
	    
    	//check truly independent countries
        int nbTags = TagCvRules.nbTags();
    	TreeSet<CvCountry> free = findIndependents();
    	int nbFree = free.size();
		if (nbTags<nbFree) {  //no chance to find enough tags
			System.out.println("too many ("+nbFree+") independant countries ; too few ("+nbTags+") available tags");
			System.out.println("aborting conversion");
			System.exit(0);
		}
		
    	//now find who is going to be added as independent ; this is an iterative process
		TreeSet<Relationship> relK=null;
		TreeSet<Relationship> relD=null;
		TreeSet<Relationship> relC=null;
		TreeSet<Relationship> r=null;
		int mod=-1;
    	do {
    		mod++;
    		if (mod>0)
    			System.out.println("not enough tags ; restricting indepedence level by "+mod);
    		TreeSet<CvCountry> lieges = new TreeSet<CvCountry>();
    		lieges.addAll(free);
    		
    		r = findOthers(lieges,Title.Tier.KINGDOM,mod);
    		if (lieges.size()>nbTags) continue;
    		relK=r;
    		
	    	r = findOthers(lieges,Title.Tier.DUCHY,mod);
    		if (lieges.size()>nbTags) continue;
    		relD=r;
    		
	    	r = findOthers(lieges,Title.Tier.COUNTY,mod);
    		if (lieges.size()>nbTags) continue;
    		relC=r;
    		break;
    	}
    	while (true);
    	
    	if (free.size()+relK.size()+relD.size()+relC.size()!=Country.__list.size())
    		throw new IllegalStateException("relationship lists badly formed");
    	
    	Relationship.apply(relK, free);
    	Relationship.apply(relD, free);
    	Relationship.apply(relC, free);
    	
    	__list = free;
	    sanityCheck();
	}
	  
    static public void checkCapitals() {
		for (CvCountry c : __list) {
			if (c._capital!=null) continue;
			if (c._tag.equals("REBE")) continue;
			int n = Rnd.get().nextInt(c._provinces.size());
			CvProvince[] p = c._provinces.toArray(new CvProvince[c._provinces.size()]);
			p[n]._isCapital = true;
			c._capital = p[n];
		}
   	
    }

    static private TreeSet<CvCountry> findIndependents() {
    	TreeSet<CvCountry> list = new TreeSet<CvCountry>();
		for (Country x : Country.__list) {				
			if (x._holder._main._liege==null) {
				CvCountry c = new CvCountry(x);
				System.out.println("created independent country : "+x._tag);
				list.add(c);
			}
		}
		return list;
    }
        
    static class Relationship implements Comparable<Relationship> {
    	final Country  _country;    //country examined
    	CvCountry      _this;       //new "real" country if this country becomes independent
    	CvCountry      _liege;      //liege fro this country if any
    	int            _relation;   //current relations between country and liege under the current conditions
    	boolean        _liegeMerged;//did the normal CK liege merge ?
    	CountryCvRules _rules;      //set of rules used
    	
    	public Relationship(Country country) {
			_country = country;    		
    	}
    	    	
    	static void apply(TreeSet<Relationship> rel, TreeSet<CvCountry> countries) {
    		for (Relationship r : rel)
    			r.apply(countries);
    	}
    		    		
    	/**
    	 * Builds a new relationship
    	 * Updates the lieges list in case that country would become free under that particular mod level
    	 * 
    	 * @param lieges  known countries that can be lieges
    	 * @param mod     modifier level for independence ; the greater, the lesser the chance of independence
    	 * @return true if the relationship would end up with a new CvCountry
    	 */
    	public boolean build(TreeSet<CvCountry> lieges, int mod) {
    		CvCountry liege = search(lieges,_country._liege._tag);
			if (liege==null && _country._title._tier==Title.Tier.COUNTY) {
				//At this point, a kingdom would have been created, so liege would exist
				_liege = search(lieges,_country._liege._liege._tag);
				_liegeMerged=true;
			}
			else {
				_liege = liege;
				_liegeMerged=false;
			}
			_rules = MergeRules.checkCountryStatus(_country,lieges,_liegeMerged);
			_relation = _rules.getRelation(mod);
			if (_relation!=CountryCvRules.MERGE) {
				if (_this==null)
					_this = new CvCountry(_country);
				lieges.add(_this);
				return true;
			}
			else
				_this=null;
			return false;
		}
    	
    	/**
    	 * applies that current relationship and updates all real relationships between countries.
    	 * That call is final (i.e. hard to cancel without a lot of unsavory code)
    	 * 
    	 * @param countries  list of CvCountries finally built
    	 */
    	public void apply(TreeSet<CvCountry> countries) {
			if (_relation==CountryCvRules.MERGE) {  //MERGE
				System.out.println("merged : "+_country._tag);
				_liege._controlled.addAll(_country._controlled);
				_liege._owned.addAll(_country._owned);
			}
			else {  //possibly independent
				if (_liegeMerged) System.out.println("subvassal count declared independence : "+_country._tag);
				else              System.out.println("vassal gained independence : "+_country._tag);
				CvRelationships rel = CvRelationships.add(_liege, _this, _relation);
				if (rel!=null) {
				    _this._relationships.add(rel);
				    _liege._relationships.add(rel);
				}
		        _this._liege=_liege;
		        countries.add(_this);
		    }    		
    	}
    	
    	public int compareTo(Relationship r) {
    		return _country._tag.compareTo(r._country._tag);
    	}
    }
    
    /**
     * This analyzes the CK countries of a given tier level
     * Any country that declares independence is added on creation to the lieges list
     * 
     * @param lieges  the current lieges list
     * @param tier    the tier level to consider
     * @param mod     the mod level to apply
     * @return        the new relationship list
     */
    static private TreeSet<Relationship> findOthers(TreeSet<CvCountry> lieges, Title.Tier tier, int mod) {
		//find semi-independent titles
    	TreeSet<Relationship> rel = new TreeSet<Relationship>();
		for (Country x : Country.__list) {
			if (x._title._tier!=tier || x._holder._main._liege==null)
				continue;
			Relationship r = new Relationship(x);
			r.build(lieges, mod);
			rel.add(r);
		}
		return rel;
	}
		
	
	public void print(PrintWriter out) {
		int n=0;
		int subs = 0;
		for (CvProvince p : _provinces) {
			if (p._controller!=p._owner)
				n++;
		}
		if (n==0)
		    out.format("%s - %s (%d owned counties / %d controlled counties / %d provinces)", _tag,_eu3Tag,_owned.size(),_controlled.size(),_provinces.size());
		else
		    out.format("%s - %s (%d owned counties / %d controlled counties / %d provinces / %d contested provinces)", _tag,_eu3Tag,_owned.size(),_controlled.size(),_provinces.size(),n);
			if (_liege!=null) out.format(" (liege %s) ", _liege._tag);
		for (CvRelationships r: _relationships) {
			if (r._first._removed || r._second._removed) continue;
			String other = null;
			boolean bonus = true;
			if (r._first._tag.equals(_tag)) {
				other = ">"+r._second._tag;
				bonus = true;
			} else {
				other = "<"+r._first._tag;
				bonus = false;
			}
            if (r._type==CvRelationships.Type.UNION) {
			    out.format(" (union %s) ", other);
			    if (bonus) { subs += r._second._provinces.size(); }
            } else if (r._type==CvRelationships.Type.VASSAL) {
			    out.format(" (vassal %s) ", other);
			    if (bonus) { subs += r._second._provinces.size(); }
            } else if (r._type==CvRelationships.Type.ALLIANCE) {
			    out.format(" (ally %s) ", other);
            } else if (r._type==CvRelationships.Type.MARRIAGE) {
			    out.format(" (marry %s) ", other);            
            }
		}		
		if (subs > 0) {
			out.format("  (%d sub provinces [%d/%d])", subs, subs, (subs + _provinces.size()));
		}
		out.println();
	}
	
	static public void getAllTags() {
		TreeSet<CvCountry> list = new TreeSet<CvCountry>(__list);
		System.out.format("%d countries to convert, %d tags available\n", list.size(),TagCvRules.nbTags());
		//assign tags, beginning by the most important countries
		getAllTags(list,Title.Tier.KINGDOM);
		getAllTags(list,Title.Tier.DUCHY);
		getAllTags(list,Title.Tier.COUNTY);
		if (!list.isEmpty()) {
			//some countries not yet assigned ? try any method to grab a tag!
			TreeSet<CvCountry> done = new TreeSet<CvCountry>();
			for (CvCountry x : list) {
				x.getTag(4,done);
			}
			//check all countries have on tag assigned
			if (done.size()!=list.size()) {
			    throw new IllegalStateException("could not find tags for all countries");
			}
		}
		//now load EU3 data
		for (CvCountry c : __list)
			if (c._eu3Tag!=null && !c._eu3Tag.equals("REB"))
				try {
				    c._country = EU3Country.get(c._eu3Tag);
				}
				catch (Exception e) {
					System.out.println("unable to load EU3 country "+c._eu3Tag);
					e.printStackTrace();
					System.exit(0);
				}
	}
	
	static private void getAllTags(TreeSet<CvCountry> list, Title.Tier tier) {
		TreeSet<CvCountry> done = new TreeSet<CvCountry>();
		TreeSet<CvCountry> todo = new TreeSet<CvCountry>();
		//create list of countries for the appropraite tier
		for (CvCountry x : list)
			if (x._main._title._tier==tier)
				todo.add(x);
		//for each of them, get a tag by first beginning by the "best" method
		//i.e. tag translation, then degrading to fuzzier methods (tag from
		//owned territory)
		int lvl=0;
		do {
			for (CvCountry y : todo)
				y.getTag(lvl,done);
			todo.removeAll(done);
			list.removeAll(done);
			done.clear();
		}
		while (todo.size()!=0 && ++lvl<=3);
	}
	
	private void getTag(int limit, TreeSet<CvCountry> done) {
		if (_eu3Tag!=null) return;
		//at limit 2 or more, we are trying for any territory under that
		//nominal title ; better try first "preferred" tags from actually
		//owned provinces, starting with the capital
		if (limit==2) {
			String[] l = new String[_provinces.size()];
			int i=0;
			for (CvProvince p : _provinces) {
				if (p._data._prefered==null || p._data._prefered.length==0 || p._data._prefered[0]==null) continue;
				String c=null;
				if (p._isCapital) c=p._data._prefered[0];
				else l[i++] = p._data._prefered[0];
				if (c!=null) l[i++]=c;
			}
			if (i>0)
			    _eu3Tag = TagCvRules.getTag(l);
		}
		if (_eu3Tag==null)
	        _eu3Tag = TagCvRules.getTag(_main, limit);		
		if (_eu3Tag!=null) {
		    done.add(this);
		    System.out.println("assigned tag " + _eu3Tag + " to " + _tag);
		}
	}

	private void preWrite() throws IOException {	
		if (_tag.equals("REBE")) return;
		_country.setSlider(EU3Country.Slider.ARISTOCRACY, CountryCvRules.SliderRule.__aristocracy_plutocracy.getValue(_owned, _main));
		_country.setSlider(EU3Country.Slider.CENTRALIZATION, CountryCvRules.SliderRule.__centralization_decentralization.getValue(_owned, _main));
		_country.setSlider(EU3Country.Slider.INNOVATIVENESS, CountryCvRules.SliderRule.__innovative_narrowminded.getValue(_owned, _main));
		_country.setSlider(EU3Country.Slider.LAND, CountryCvRules.SliderRule.__land_naval.getValue(_owned, _main));
		_country.setSlider(EU3Country.Slider.MERCANTILISM, CountryCvRules.SliderRule.__mercantilism_freetrade.getValue(_owned, _main));
		_country.setSlider(EU3Country.Slider.OFFENSIVE, CountryCvRules.SliderRule.__offensive_defensive.getValue(_owned, _main));
		_country.setSlider(EU3Country.Slider.QUALITY, CountryCvRules.SliderRule.__quality_quantity.getValue(_owned, _main));
		_country.setSlider(EU3Country.Slider.SERFDOM, CountryCvRules.SliderRule.__serfdom_freesubjects.getValue(_owned, _main));
	    _country.setCapital(_capital._id);
	    _country.setMonarch(CharacterCvRules.getMonarch(_main._holder));
	    _country.setReligion(CountryCvRules.getReligion(_main._holder,_capital._province._religion));
		CultureCvRules.EU3Culture[] cultures = cultures();
		_country.setCulture(cultures[0]._culture);
		for (int i=1; i<cultures.length; i++)
		    _country.addCulture(cultures[i]._culture);
		
	    if (_tag.equals("PRUS")) {
	    	int i = 0;
	    }
		String tech = CountryCvRules.TechGroupRule.getTechGroup(_country._religion, cultures[0]);
		_country.setTechGroup(tech);
		String ckForm = _main._base.getBase("form_of_goverment").get();
		_country.setGovType(CountryCvRules.GovernmentRule.getGovType(_country._religion, cultures[0], ckForm, _main._laws));
		if (_leader!=null) {
			Characters.Stats s = _leader._stats;
                        String d = Dynasty.getDynastyName(s._dynasty);
			_country.addLeader("general", 
					s._birth, 
					s._month, 
					s._day, 
					s._death, 
					(byte)1, 
					(s._name + " " + d),
					(byte)_leader._fire, 
					(byte)_leader._shock, 
					(byte)_leader._manuever, 
					(byte)_leader._siege);
		}
		_country.setStability(_main._stability);
		_country.setBadboy(_main._badboy);
		_country.setHolderTreasury((double) _main._holder._gold);
		_country.setHolderPrestige((double) _main._holder._prestige);
		
		_country.applyChanges();
	}
	
	private CultureCvRules.EU3Culture[] cultures() {
		//first, check the leadership cultures : it determines the most important cultures
		float limit= (float)0.15;
		CultureCvRules.EU3Culture[] res = new CultureCvRules.EU3Culture[50];
		int n=0;
		
		//leadership culture
		CultureCvRules.CultureCounter cnt0 = new CultureCvRules.CultureCounter();
		for (County c : _owned)
			cnt0.add(CultureCvRules.__list.search(c._owner._holder._culture)._EU3culture, (float)1.0);
		CultureCvRules.EU3Culture[] leaderCultures = cnt0.getCulture(limit);
		System.arraycopy(leaderCultures, 0, res, 0, leaderCultures.length);
		n=leaderCultures.length;

		//major culture in major culture group
		CultureCvRules.CultureCounter cnt1 = new CultureCvRules.CultureCounter();
		for (CvProvince p : _provinces)
			cnt1.add(p._culture, p._province._citysize);
		CultureCvRules.EU3Culture culture = cnt1.getCulture();
		boolean found = false;
		for (int i=0; i<n && !found; i++) 
			found = res[i]._culture.equals(culture._culture);
		if (!found) res[n++]=culture;
		
		//king culture
		CultureCvRules c = CultureCvRules.__list.search(_main._holder._culture);
		checkFatal(c,"culture",_main._holder._culture+" in cvdata.txt");
		culture = c._EU3culture;
		found = false;
		for (int i=0; i<n && !found; i++) 
			found = res[i]._culture.equals(culture._culture);
		if (!found) res[n++]=culture;
		
		//capital culture
		culture = _capital._culture;
		found = false;
		for (int i=0; i<n && !found; i++) 
			found = res[i]._culture.equals(culture._culture);
		if (!found) res[n++]=culture;
		
		CultureCvRules.EU3Culture[] result = new CultureCvRules.EU3Culture[n];
		System.arraycopy(res, 0, result, 0, n);
		return result;
	}

	static public void preWriteAll() throws IOException {
		for (CvCountry c : __list) {
			if (c._eu3Tag!=null) {
			    c.preWrite();
			    // sanity check for rebels
			    if (null != c._country) {
			    	__eu3List.add(c._country);
			    }
			}
		}
		
		// now that we have all the eu3 countries, calculate their prestige.
    	// set up the rulers on a 0-20 scale for positive prestige, 
    	TreeSet<EU3Country> prestige = new TreeSet<EU3Country>(new Comparator<EU3Country>() {
    		public int compare(EU3Country c1, EU3Country c2) {
    			if (c1.getHolderPrestige() != c2.getHolderPrestige()) {
    				return (int) (c1.getHolderPrestige() - c2.getHolderPrestige());	
    			} else {
    				return c1.compareTo(c2);
    			}    			 
    		}    		
    	});

    	prestige.addAll(__eu3List);

    	int maxNegativeIndex = -1;
    	int size = __eu3List.size();
    	for (EU3Country eu3c : prestige) {
    		double holderPrestige = eu3c.getHolderPrestige();
    		if (holderPrestige < 0) {
    			maxNegativeIndex++;
    		}
    	}
    	// ok, now we know the scale
		// scale based on the positive side for the negative, so a slim negative 
		// prestige in CK doesn't become a -20 in EU3.    	
    	if (maxNegativeIndex > 1) {
    		size = size - maxNegativeIndex;
    	}
		if (size < 2) {
			size = 2;
		}
		double delta = 0.20d / size;   		
		double cPrestige = 0;
		if (maxNegativeIndex > 1) {
			cPrestige = 0 - (delta * maxNegativeIndex); 
		}
		for (EU3Country eu3c : prestige) {
			eu3c.setPrestige(cPrestige);
			cPrestige += delta;
		}    	
		
		// top 7 
		for (int i = (prestige.size() - 1); i >=0; i--) {
			
		}
	
		// now pick up the baseline tags for the remainder of the tag pool and then the ROTW.  some of these are later-arising countries,
		// and so not necessarily in existence at start time...
		// apparently, though, the tags and full descriptions stay in the list,
		// so keep it up.
		for (String tag: TagCvRules.getListTags()) {
			EU3Country eu3c = EU3Country.get(tag);
			eu3c.takeEU3Defaults();
			addTechGroup(eu3c._tech_group, eu3c._tag);
		    __eu3List.add(eu3c);
		}		
		
		// ok, read the country file, and then add in any countries that we haven't already used.
	    BufferedReader r = new BufferedReader(new FileReader(__countriesFile)); 
	    r.readLine();
	    String s=null;
	    do {
	    	s = r.readLine();
	    	if (s==null) return;
	    	if (s.contains("=")) {
	    		String tag = s.substring(0,3);
				if (!TagCvRules.isUsed(tag)) {
					EU3Country eu3c = EU3Country.get(tag);
					eu3c.takeEU3Defaults();
					addTechGroup(eu3c._tech_group, eu3c._tag);					
				    __eu3List.add(eu3c);
				}
	    	}
	    } while (true);		
	}
	
	static public void calcRelations() {
		for (EU3Country eu3c : __eu3List) {
			/*
			if ("ARA".equals(eu3c._tag)) {
				System.out.println("ARA");
			}
			*/
			// does this country hold any territory?
			if (eu3c.hasProvinces()) {
				// calc for every other country...but since the relations are symmetric, it should be 
				// no trouble.
				for (EU3Country c2 : __eu3List) {
					// don't bother recalculating if we already have, or if the other
					// country has no territory either.
					if ((null == eu3c.getRelationTo(c2._tag)) && c2.hasProvinces()) {																	
						int rel = 0;
						
						// see if we have even discovered these guys.  Otherwise we keep the 0.
						if (eu3c.hasDiscovered(c2)) {						
							// ok, if this is us, the relation is a flat 110 to start
							if (eu3c._tag.equals(c2._tag)) {
								rel = 110;
							} else {
								// baseline is determined by religion.  For Christian groups, 
								// same religion gets +35, cross-christian gets -65, 
								// cross-religion gets -115, before any modifiers.
								// for non-christians, same gets +85.
								if (eu3c.isSameReligionAs(c2)) {
									if (eu3c.isChristian()) {
										rel = 35;
									} else {
										rel = 85;
									}
								} else {
									if (eu3c.isSameReligiousGroupAs(c2)) {
										rel = -65;
									} else {
										rel = -115;
									}
								}
								
								// -10 for monarch / republic crosses
								if ((eu3c._gov_type.contains("monarchy") && 
										c2._gov_type.contains("republic")) ||
									(eu3c._gov_type.contains("republic") && 
										c2._gov_type.contains("monarchy"))) {
									rel -= 10;
								}
																
								// +50 for royal marriage
								if (eu3c.isRMWith(c2)) {
									rel += 50;
								}
								
								// +100 for alliance
								if (eu3c.isAllianceWith(c2)) {
									rel += 100;
								}

								// +150 for vassal
								if (eu3c.isVassalWith(c2)) {
									rel += 150;
								}

								// +200 for union								
								if (eu3c.isUnionWith(c2)) {
									rel += 200;
								}
								
								// -200 for war
								if (eu3c.isWarWith(c2)) {
									rel -= 200;
								}
							}
						}
						
						// clamp to the -200<-->200 range.
						rel = Math.max(-200, rel);
						rel = Math.min(200, rel);
						eu3c.setRelationTo(c2._tag, rel);
					}
				}
			}
		}
	}
	
	static public void writeAll() throws IOException {
		for (EU3Country eu3c : __eu3List) {
			eu3c.write(__destPath + File.separatorChar + Analyzer.getSaveFile());
		}
	}
	
	static public void calcTechGroups() {
		for (CvCountry c : __list) {
			if ((c._eu3Tag != null) && (c._country != null)) {
				String tech = CountryCvRules.TechGroupRule.getTechGroup(c._country._religion, c.cultures()[0]);
				addTechGroup(tech, c._eu3Tag);
			}
		}
	}
	
	static public void addTechGroup(String tech, String tag) {
		if ("western".equals(tech)) {
			__western.add(tag);
		} else if ("eastern".equals(tech)) {
			__eastern.add(tag);
		} else if  ("ottoman".equals(tech)) {
			__ottoman.add(tag);
		} else if ("muslim".equals(tech)) {
			__muslim.add(tag);
		} else if ("indian".equals(tech)) {
			__indian.add(tag);
		} else if ("chinese".equals(tech)) {
			__chinese.add(tag);
		} else if ("sub_saharan".equals(tech)) {
			__sub_saharan.add(tag);
		} else if ("new_world".equals(tech)) {
			__new_world.add(tag);
		} else {
			System.out.println("WTF: invalid tech type: " + tech + " for: " + tag);
		}						
	}
	
    CvCountry(String tag) {
    	_tag = tag;
    }
    private CvCountry(Country x) {
    	_tag  = x._tag;
    	_main = x;
    	_owned         = new FieldSet<County>(County.class);
    	_controlled    = new FieldSet<County>(County.class);
    	_relationships = new TreeSet<CvRelationships>();
    	_provinces     = new TreeSet<CvProvince>();
    	_cultures      = MergeRules.getCultures(x);
    	_controlled.addAll(x._controlled);
    	_owned.addAll(x._owned);
   }
    
    static public String getEmperor() throws IOException {
    	// top prestige of Catholic rulers.
    	// 100,000 prestige bonus to GER, HAB, ITA, BOH, BUR if they exist.
    	TreeSet<EU3Country> emperor = new TreeSet<EU3Country>(new Comparator<EU3Country>() {
    		public int compare(EU3Country c1, EU3Country c2) {
    			double c1e = c1.getHolderPrestige();
    			String eu3Tag = c1._tag;
        		if ("GER".equals(eu3Tag) ||
            			"HAB".equals(eu3Tag) ||
            			"ITA".equals(eu3Tag) ||
            			"BOH".equals(eu3Tag) ||
            			"BUR".equals(eu3Tag)) {
        			c1e += 100000; // massive prestige bonus to certain EU3 tags.
        		}
    			double c2e = c2.getHolderPrestige();
    			eu3Tag = c2._tag;
        		if ("GER".equals(eu3Tag) ||
            			"HAB".equals(eu3Tag) ||
            			"ITA".equals(eu3Tag) ||
            			"BOH".equals(eu3Tag) ||
            			"BUR".equals(eu3Tag)) {
        			c2e += 100000; // massive prestige bonus to certain EU3 tags.
        		}    			
    			if (c1e != c2e) {
    				return (int) (c1e - c2e);	
    			} else {
    				return c1.compareTo(c2);
    			}    			     			
    		}    		
    	});

    	for (CvCountry c : __list) {
    		String eu3Tag = c._eu3Tag;
    		Country main = c._main;
    		if (null != main) {
    			if (null != main._holder) {
    				Characters.Rulers holder = main._holder;
    				if ("catholic".equals(holder._religion)) {
    					EU3Country eu3c = EU3Country.get(eu3Tag);
    					emperor.add(eu3c);
    				}
    			}
    		}
		}
    	
    	String res = null;
    	if (emperor.size() > 0) {
    		res = emperor.last()._tag;
    		emperor.last().setEmperor(true);

    		// set up the other 8 top countries inside the HRE as electors.
    		int electors = 0;
    		while (electors < 8) {    		
    			emperor.remove(emperor.last());  
    			if (emperor.size() > 0) {
    				EU3Country eu3c = emperor.last();
    				String cap = eu3c._capital;
    				if (null != cap) {
	    				EU3Province eu3p = EU3Province.getProvById(cap);
	    				if (null != eu3p) {
		    				if (eu3p.isHRE()) {
		    					// ok, the capital is in the HRE, so this guy counts.
		    					eu3c.setElector(true);
		    					electors++;
		    				}
	    				}
    				}
    			} else {
    				break; // ran out of countries.
    			}
    		}
    	}
    	
    	return res;
    }
    
	static public CvCountry search(TreeSet<CvCountry> list, String tag) {
		CvCountry dummy0 = new CvCountry(tag);
		CvCountry dummy1 = new CvCountry(tag+"\0");
		SortedSet<CvCountry> sub = list.subSet(dummy0,dummy1);
		if (sub.isEmpty()) return null;
		return sub.first();
	}

 	static public CvCountry search(String tag) {
 		return search(__list,tag);
 	}

	static public boolean isRemoved(String tag) {
		CvCountry dummy0 = new CvCountry(tag);
		CvCountry dummy1 = new CvCountry(tag+"\0");
		SortedSet<CvCountry> sub = __removed.subSet(dummy0,dummy1);
		return !sub.isEmpty();
	}

    public int compareTo(CvCountry c) { return _tag.compareTo(c._tag); }

}
