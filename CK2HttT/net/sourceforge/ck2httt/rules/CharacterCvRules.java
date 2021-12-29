package net.sourceforge.ck2httt.rules;

import static net.sourceforge.ck2httt.utils.Check.checkFatal;
import net.sourceforge.ck2httt.ck.Characters;
import net.sourceforge.ck2httt.ck.Dynasty;
import net.sourceforge.ck2httt.ck.Title;
import net.sourceforge.ck2httt.cv.CvCountry;
import net.sourceforge.ck2httt.eu3.EU3Country;
import net.sourceforge.ck2httt.pxAnalyzer.PXTree.BaseField;
import net.sourceforge.ck2httt.pxAnalyzer.PXTree.Field;
import net.sourceforge.ck2httt.pxAnalyzer.PXTree.StructField;
import net.sourceforge.ck2httt.utils.FieldLoadable;
import net.sourceforge.ck2httt.utils.FieldMap;
import net.sourceforge.ck2httt.utils.FieldSet;
import net.sourceforge.ck2httt.utils.KeyString;
import net.sourceforge.ck2httt.utils.OptionSection;
import net.sourceforge.ck2httt.utils.SearchSet;
import net.sourceforge.ck2httt.utils.StringFloat;


public class CharacterCvRules {
	
	static private MonarchRuleSet __mrs = null;
	static private AdvisorRuleSet __ars = null;
	static private LeaderRuleSet  __lrs = null;

	static public void loadRules(StructField rules) {
		__mrs = new MonarchRuleSet(rules);
		__ars = new AdvisorRuleSet(rules);
		__lrs = new LeaderRuleSet(rules);
	}
	
	/************************************************************/
	/*                                                          */
	/*                       MONARCHS                           */
	/*                                                          */
	/************************************************************/
	
	static public class Monarch {
		public Characters.Stats  _ck=null;
		public float             _dip=0;
		public float             _adm=0;
		public float             _mil=0;
		public Title             _title=null;
		
		public Monarch(Characters.Rulers x, MonarchRuleSet mrs) {
			_ck = x.getStats();
    		_title = x._titles.search(x._country._tag);
    		check(mrs);
    		checkFatal(_title,"title",x._country._tag);
		}
        protected void check(MonarchRuleSet mrs) {
			_adm = mrs.getStat(_ck._stew+_ck._intr/3);
			_mil = mrs.getStat(_ck._mart+_ck._intr/3);
			_dip = mrs.getStat(_ck._dipl+_ck._intr/3);
			for (BaseField f : _ck.getTraits()) {
				MonarchRule rule = mrs.getTraitModifiers(f.name());
				if (rule!=null) {
					_adm += rule._adm;
					_dip += rule._dip;
					_mil += rule._mil;
				}
    		}
        }
        public EU3Country.Monarch convert() {
		    return new EU3Country.Monarch(_title._year,
		    		_title._month,
		    		_title._day,
		    		_ck._name,
                                Dynasty.getDynastyName(_ck._dynasty),
		    		(byte)_dip,
		    		(byte)_mil,
		    		(byte)_adm,
		    		((OptionSection.getStartYear() - _ck._birth) < 16)); // under 16 years old gets you a regent.		    
        }
	}
	
	static public class MonarchRule implements FieldLoadable {
		float _dip;
		float _adm;
		float _mil;
		public MonarchRule() {}
		public boolean load(Field<?> f) {
			if (f==null || !(f instanceof StructField)) return false;
			StructField g = (StructField)f;
			BaseField x;
			x=g.getBase("adm");
			if (x!=null) _adm=x.getAsFloat();
			x=g.getBase("dip");
			if (x!=null) _dip=x.getAsFloat();
			x=g.getBase("mil");
			if (x!=null) _mil=x.getAsFloat();
			return true;
		}
	}
	
	static public class MonarchRuleSet {
		static private FieldMap.IntFloat               _statTable   = new FieldMap.IntFloat();
		static private FieldMap.StringAny<MonarchRule> _traitsTable = new FieldMap.StringAny<MonarchRule>(MonarchRule.class);
		public float getStat(float attribute) {
			if (attribute > 23) { attribute = 23; }
			if (attribute < -10) { attribute  = -10; }
			float d = attribute-(int)attribute;
			float M = _statTable.searchMin((int)attribute);
			if (d==0) return M;
			float m = _statTable.searchMax((int)attribute-1);
			return (M-m)*d+m;
		}
		public MonarchRule getTraitModifiers(String trait) {
			return _traitsTable.search(trait);
		}
		public MonarchRuleSet(StructField rules) {
			_statTable.load(rules.getStruct("ruler_stats"));
			_traitsTable.load(rules.getStruct("ruler_traits"));
		}		
	}
		
	static public EU3Country.Monarch getMonarch(Characters.Rulers x) {
		return new Monarch(x,__mrs).convert();
	}
	
	/************************************************************/
	/*                                                          */
	/*                       ADVISORS                           */
	/*                                                          */
	/************************************************************/

	/**
	 * lists the advisor professions modifers associated to a trait.
	 */
	static public class BaseCharacterTraitRule extends StringFloat {}

	/**
	 * lists the advisor professions modifiers associated to all traits.
	 */
	static public class CharacterTraitRuleSet {
		public FieldSet<BaseCharacterTraitRule> _traits = new FieldSet<BaseCharacterTraitRule>(BaseCharacterTraitRule.class);
		public void load(StructField f, String section) {
			if (section!=null)
				_traits.loadOptionSection(f,section);
			else
				_traits.load(f);
		}
	}
	
	/**
	 * lists the modifers due to stats for one profession.
	 */
	static public class BaseCharacterStatsRule extends KeyString implements FieldLoadable {
		public FieldMap.IntFloat _ste = new FieldMap.IntFloat();
		public FieldMap.IntFloat _mil = new FieldMap.IntFloat();
		public FieldMap.IntFloat _dip = new FieldMap.IntFloat();
		public FieldMap.IntFloat _int = new FieldMap.IntFloat();
		public BaseCharacterStatsRule() {}
		public boolean load(Field<?> x) {
			StructField f = (StructField)x;
			_mil.load(f.get("mil"));
			_dip.load(f.get("dip"));
			_ste.load(f.get("ste"));
			_int.load(f.get("int"));
			_key=x.name();
			return true;
		}
	}

	/**
	 * the list of advisor professions with the stats modifiers associated.
	 */
	static public class CharacterStatRuleSet {
		public FieldSet<BaseCharacterStatsRule> _stats = new FieldSet<BaseCharacterStatsRule>(BaseCharacterStatsRule.class);
		public void load(StructField f,String section) {
			if (section!=null)
			    _stats.loadOptionSection(f,section);
			else
				_stats.load(f);
		}
	}

	/**
	 * the set of rules applying to advisors
	 */
	static private class AdvisorRuleSet {
		
		public CharacterTraitRuleSet _traits  = new CharacterTraitRuleSet();
		public CharacterStatRuleSet  _stats = new CharacterStatRuleSet();
		
	    public AdvisorRuleSet(StructField f) {
	    	if (f==null) return;
			_stats.load(f.getStruct("counselor_stats"),"convert");
			_traits.load(f.getStruct("counselor_traits"),null);
		}
	}
	
	/**
	 * rule in action for selecting an advisor profession
	 */
	static private class AdvisorRule {
		/** list of current possible professions with associated skill level */
		private SearchSet<String,AdvisorTypeAndSkill> _list = new SearchSet<String,AdvisorTypeAndSkill>(AdvisorTypeAndSkill.class);
		
		private final AdvisorRuleSet _ars;
		AdvisorRule(AdvisorRuleSet ars) {
			_ars=ars;
		}
		
		/** modifier for a type of advisor */
		static public class AdvisorTypeAndSkill extends KeyString {
			float  _skill;
			public AdvisorTypeAndSkill() {}
			public AdvisorTypeAndSkill(String name, float value) { _key=name; _skill=value; }
		}
		/**
		 * adds a modifier associated with a profession
		 */
		private void addData(String type, float skill) {
			AdvisorTypeAndSkill ats = _list.search(type);
			if (ats==null) _list.add(new AdvisorTypeAndSkill(type,skill));
			else { ats._skill += skill; }			
		}
		/**
		 * applies a trait
		 * @return false if the character is automatically rejected
		 */
		private boolean applyTrait(String trait) {
			BaseCharacterTraitRule bct = _ars._traits._traits.search(trait);
			if (bct==null) return true;
			for (StringFloat.BaseStringFloat x : bct._assoc) {
				if (x._key.equals("reject")) return false;
				addData(x._key,x._v);
			}
			return true;
		}
		/**
		 * applies traits from a character
		 */
		private boolean applyTraits(Characters.Stats ck) {
			for (BaseField f : ck.getTraits()) {
				if (!applyTrait(f.name())) return false;
			}
			return true;
		}
		/**
		 * applies stats from a character
		 */
		private void applyStats(Characters.Stats ck) {
			for (BaseCharacterStatsRule rule : _ars._stats._stats) {
				String profession = rule._key;
				if (rule._dip.size()!=0) addData(profession,rule._dip.searchMax((int)ck._dipl));
				if (rule._ste.size()!=0) addData(profession,rule._ste.searchMax((int)ck._stew));
				if (rule._mil.size()!=0) addData(profession,rule._mil.searchMax((int)ck._mart));
				if (rule._int.size()!=0) addData(profession,rule._int.searchMax((int)ck._intr));
			}
		}
		/**
		 * computes a character
		 */
		public boolean apply(Characters.Stats ck) {
			if (ck==null || !applyTraits(ck)) return false;
			applyStats(ck);
			return true;
		}
		public AdvisorTypeAndSkill findBest() {
			AdvisorTypeAndSkill current = null;
			AdvisorTypeAndSkill all     = null;
			for (AdvisorTypeAndSkill ats : _list) {
				if (ats._key.equals("all"))
					all = ats;
				else if (current==null || ats._skill>current._skill)
					current=ats;
			}
		    AdvisorTypeAndSkill found = new AdvisorTypeAndSkill(current._key,current._skill);
			if (all!=null)  found._skill += all._skill;
			if (found._skill>6) found._skill=6;
			if (found._skill<1) found = null;
			return found;
		}		
		public AdvisorTypeAndSkill search(String name) {
			return _list.search(name);
		}
	}
	
	static public class Advisor {
		public Characters.Stats _stats;
		public float            _skill;
		public String           _type;
		public CvCountry        _location;
		
		private Advisor(Characters c) {
			_stats = c.getStats(true, true);
			if (_stats._name==null) return;
			_location = CvCountry.search(c._tag);
			if (_location==null || _location._eu3Tag==null)
				return; //abort there if advisor has no country in eu3
			AdvisorRule rule = new AdvisorRule(__ars);
			if (!rule.apply(_stats))
				return; //abort there if a trait rejected the character
			AdvisorRule.AdvisorTypeAndSkill ats = rule.findBest();
			if (ats!=null && ats._skill>=1) {
				_type  = ats._key;
				_skill = ats._skill;
			}
		}
				
		static public Advisor getAdvisor(Characters c) {
			Advisor a = new Advisor(c);
			return (a._type==null && a._location==null) ? null : a;
		}
	}

	
	/************************************************************/
	/*                                                          */
	/*                       LEADERS                            */
	/*                                                          */
	/************************************************************/

	/**
	 * we are going to check leaders using the same structures as advisors,
	 * with fire, shock etc being professions.
	 */
	static private class LeaderRuleSet extends AdvisorRuleSet {
	    public LeaderRuleSet(StructField f) {
	    	super(null);
			_stats.load(f.getStruct("leader_stats"),null);
			_traits.load(f.getStruct("leader_traits"),null);
		}
	}
	
	static public class LeaderRule extends AdvisorRule {
		public LeaderRule (LeaderRuleSet lrs) {
			super(lrs);
		}
	}
	
	static public class Leader {
		public Characters.Stats  _stats = null;
		public float             _fire     = 0;
		public float             _shock    = 0;
		public float             _manuever = 0;
		public float             _siege    = 0;
		
		private Leader(Characters c) {
			_stats = c.getStats(true,true);
			if (_stats._name==null) return;
			LeaderRule rule = new LeaderRule(__lrs);
			rule.apply(_stats);
			float all = (float)0.5;  //allows rounding to nearest number
			AdvisorRule.AdvisorTypeAndSkill lts = rule.search("all");
			if (lts!=null) all += lts._skill;
			_fire     = rule.search("fire")._skill+all;
			_shock    = rule.search("shock")._skill+all;
			_manuever = rule.search("manuever")._skill+all;
			_siege    = rule.search("siege")._skill+all;
			_shock    = rule.search("shock")._skill+all;
			if (_fire<1)     _fire=0;
			if (_shock<1)    _shock=0;
			if (_manuever<1) _manuever=0;
			if (_siege<1)    _siege=0;
		}
		
		static public Leader getLeader(Characters c) {
			Leader a = new Leader(c);
			return (a._fire+a._shock+a._manuever+a._siege==0) ? null : a;
		}
	}
}
