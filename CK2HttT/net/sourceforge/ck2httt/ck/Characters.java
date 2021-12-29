package net.sourceforge.ck2httt.ck;

import java.util.Iterator;
import java.util.Locale;

import net.sourceforge.ck2httt.pxAnalyzer.PXTree.BaseField;
import net.sourceforge.ck2httt.pxAnalyzer.PXTree.Field;
import net.sourceforge.ck2httt.pxAnalyzer.PXTree.StructField;
import net.sourceforge.ck2httt.utils.FieldSet;
import net.sourceforge.ck2httt.utils.Key;
import net.sourceforge.ck2httt.utils.OptionSection;
import net.sourceforge.ck2httt.utils.Rnd;
import net.sourceforge.ck2httt.utils.SearchSet;

/**
 * This class extracts the data relevant to a character.
 * Some of the data comes from the character structure in the save file, but some
 * information also come from different places, such as country or title lists.
 * Also, as we do not have to access all of the information relevant to a character
 * in all cases, it is important for performances sake to provide different views
 * about a character, views which would be filled on a per need basis.
 * 
 * @author yprelot
 */
public class Characters implements Key<String> {

	//initial data
	/** id of character */
    public String          _id;
    /** court tag of character ; main country for ruler! */
    public String          _tag;
	/** analyzed data */
    public StructField     _base;
	
    
	/*************************************************************/
	/*                                                           */
	/*            MANAGING THE LIST OF CHARACTERS                */
	/*                                                           */
	/*************************************************************/
    
	static public SearchSet<String,Characters> __list = new SearchSet<String,Characters>(Characters.class);

	/**
	 * Loads all the characters (even dead because they can be relevant).
	 * Because this can be very big, only a minimum of data is stored at this stage.
	 * @param root
	 */
	static public void loadAll(StructField root) {
		for (Field<?> f : root._data) {
			if (f._name._value.equals("character")) {
				StructField x = (StructField)f;
				String id = charId(x.getStruct("id"));
				Characters c = new Characters(id);
				c._base = x;
				c._tag = c._base.getBase("tag").get();
				__list.add(c);
			}
		}		
	}
	
	/**
	 * returns a compound character id containing id and type
	 * @param f
	 * @return
	 */
	static public String charId(StructField f) {
		return String.format(Locale.US,"%s,%s", f.getBase("id").get(), f.getBase("type").get());
	}
		
	static public Characters search(StructField idField) {
		return __list.search(charId(idField));
	}
				
	private Characters(String id)      {_id=id;}
	public Characters()                {}
	public String getKey()             { return _id; }
	public void setKey(String k)       { _id=k; }
	public void setSuccessor(String k) { _id=k+"\0"; }

	
	/*************************************************************/
	/*                                                           */
	/*                     VIEW FOR RULERS                       */
	/*                                                           */
	/*************************************************************/
	private Rulers _ruler;

	public Rulers getRuler() {
		if (_ruler==null) _ruler=new Rulers();
		return _ruler;
	}

	/**
	 * This will store the data which is typically only used by
	 * characters ruling a country.
	 */
	public class Rulers {
	    /** character culture */
	    public String          _culture;
	    /** character religion */
	    public String          _religion;
	    /** character loyalty */
	    public int             _loyalty;
	    /** character piety */
	    public int             _piety;
	    /** character prestige */
	    public int             _prestige;
	    /** character prestige */
	    public int             _gold;
		//filled when loading titles
		/** claims by this character */
	    public FieldSet<Title> _claims;
		/** titles owned by this character */
	    public FieldSet<Title> _titles;
		/** main title for this character */
	    public Title           _main;
		//filled when loading countries
		/** country ruled by this character */
	    public Country         _country;

		private Rulers() {
			BaseField b = _base.getBase("loyalty");
			_titles = new FieldSet<Title>(Title.class);
			_claims = new FieldSet<Title>(Title.class);
			_culture = _base.getBase("culture").getUnquoted();
			_religion = _base.getBase("religion").get();
			_loyalty = (b==null) ? 100 : (int)(100*b.getAsFloat());
			StructField score=_base.getStruct("score");
			if (score!=null) {
				BaseField f;
				f = score.getBase("piety");
				if (f!=null) _piety = (int)f.getAsFloat();
				f = score.getBase("gold");
				if (f!=null) _gold = (int)f.getAsFloat();
				f = score.getBase("prestige");
				if (f!=null) _prestige = (int)f.getAsFloat();
			}			
		}
		
		/**
		 * number of independent crowns this ruler has from another
		 */
		public int independentCrowns(Rulers from) {
			int n=0;
			for (Title t : getRuler()._titles) {
				if (t._tier==Title.Tier.DUCHY && !from._claims.contains(t))
					n++;
			}
			return n;
		}
		
		//reach the enclosing object
		public Characters Characters() { return Characters.this; }
		//reach the associated ruler
		public Stats getStats() { return Characters.this.getStats(false,false); }
	}

	
	/*************************************************************/
	/*                                                           */
	/*               VIEW FOR STATS and TRAITS                   */
	/*                                                           */
	/*************************************************************/
	private Stats _stats = null;
	
	public Stats getStats(boolean dead, boolean advisor) {
		//read or reread if conditions less strong
		if (_stats==null || !dead && _stats._dead || !advisor && _stats._advisor)
			_stats=new Stats(dead,advisor);
		return _stats;
	}

	public class Stats {
		public float    _intr           = 0;
		public float    _mart           = 0;
		public float    _dipl           = 0;
		public float    _stew           = 0;
		public String   _name           = null;
                public int      _dynasty        = 0;
                public short    _birth          = 0;
		public short    _death          = 0;
		public byte     _month          = 0;
		public byte     _day            = 0;
		public byte     _courtPos       = -1;
		public boolean  _isDead         = false;
		public boolean  _dead;
		public boolean  _advisor;
		
		/**
		 * builds the stats for a character
		 * @param dead     skip dead
		 * @param advisor  skip non advisors
		 */
		private Stats(boolean dead, boolean advisor) {
			_dead    = dead;
			_advisor = advisor;
			_isDead = _base.get("deathdate")!=null;
			if (dead && _isDead) return;          //we don't need stats for dead characters
			if (_base.get("court")!=null)
				_courtPos=(byte)_base.getBase("court").getAsInt();
			if (advisor && _courtPos==-1) return; //we don't want stats for a non advisor
			Field<?> stats = _base.get("attributes");
			if (stats instanceof StructField) {  //CK
				StructField f = (StructField)stats;
				_intr = f.getBase("intrigue").getAsFloat();
				_mart = f.getBase("martial").getAsFloat();
				_dipl = f.getBase("diplomacy").getAsFloat();
				_stew = f.getBase("stewardship").getAsFloat();
			}
			else {  //DV
				String v = ((BaseField)stats).getUnquoted();
				String[] y = v.split("\\.");
				_mart = new Integer(y[0]).floatValue();
				_dipl = new Integer(y[1]).floatValue();
				_intr = new Integer(y[2]).floatValue();
				_stew = new Integer(y[3]).floatValue();
			}
			_name = _base.getBase("name").getUnquoted();
                        _dynasty = _base.getBase("dyn").getAsInt();
			String date = _base.getBase("date").getUnquoted();
			_birth = (short) Integer.parseInt(date.substring(0,4));
			if (_birth > OptionSection.getStartYear()) { _birth = OptionSection.getStartYear(); }				
			_month = (byte) (Integer.parseInt(date.substring(4,6)) + 1);
			_day   = (byte) (Integer.parseInt(date.substring(6)) + 1);
			if (_month<1)  _month=1;
			if (_month>12) _month=12;
			if (_day<1)    _day=1;
			if (_day>30)   _day=30;
			do {
				double x=Rnd.get().nextGaussian();
				_death=(short)(_birth+x*15+40);
			}
			while(_death-_birth>100 || _death-_birth<25);
		}
				
		private class TraitIterator implements Iterator<BaseField>, Iterable<BaseField> {
			Iterator<Field<?>> _i=null;
			private TraitIterator() {
				Field<?> x= _base.get("traits");
				if (x==null || ! ( x instanceof StructField))
					return;
				_i = ((StructField)x)._data.iterator();
			}
			public boolean hasNext() {
				if (_i==null) return false;
				return _i.hasNext();
			}
			public BaseField next() {
				return (BaseField)_i.next();
			}
			public void remove() {
				_i.remove();
			}
			public Iterator<BaseField> iterator() {
				return new TraitIterator();
			}
		}
		
		public Iterable<BaseField> getTraits() {
			return new TraitIterator();
		}
		
		//reach the enclosing object
		public Characters Characters() { return Characters.this; }
		//reach the associated ruler
		public Rulers getRuler() { return Characters.this.getRuler(); }
	}
}
