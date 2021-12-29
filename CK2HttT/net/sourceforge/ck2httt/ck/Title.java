package net.sourceforge.ck2httt.ck;

import static net.sourceforge.ck2httt.utils.Check.checkFatal;
import net.sourceforge.ck2httt.pxAnalyzer.PXTree.Field;
import net.sourceforge.ck2httt.pxAnalyzer.PXTree.StructField;
import net.sourceforge.ck2httt.utils.FieldLoadable;
import net.sourceforge.ck2httt.utils.FieldSet;
import net.sourceforge.ck2httt.utils.Key;
import net.sourceforge.ck2httt.utils.SearchSet;


public class Title implements FieldLoadable, Key<String> {

	public enum Tier { KINGDOM, DUCHY, COUNTY; }

	/** title tag */
	public String               _tag;
	/** title tier : KINGDOM, DUCHY, COUNTY */
	public Tier                 _tier;
	/** title owner */
	public Characters.Rulers    _holder;
	/** liege title */
	public Title                _liege;
	/** claims on this title */
	public SearchSet<String,Characters> _claims;
	/** analyzed data */
	public StructField          _base;
	/** start year */
	public short                _year;
	/** start month */
	public byte                 _month;
	/** start day */
	public byte                 _day;
	/** list of titles */
	static public FieldSet<Title> __list = new FieldSet<Title>(Title.class);
	
	/**
	 * first processing pass : build the basis 
	 * @param root
	 */
	static void loadAll(StructField root) {
		__list.load(root);
		for (Title t : __list)
			t._liege = __list.search(t._base.getBase("liege").get());
	}
	public boolean load(Field<?> x) {
		if (!x.name().equals("title")) return false;
		StructField f = (StructField)x;
		_tag = f.getBase("tag").get();
		String tier=f.getBase("tier").get();
		if      (tier.equals("county"))  _tier=Tier.COUNTY;
		else if (tier.equals("duchy"))   _tier=Tier.DUCHY;
		else if (tier.equals("kingdom")) _tier=Tier.KINGDOM;
		_base=f;
		StructField hf = f.getStruct("holder");
		if (null == hf) { return false; } // tag associated with non-existent character.
		StructField cf = hf.getStruct("character"); // tag associated with non-existent character.
		if (null == cf) { return false; }
		Characters holder=Characters.search(f.getStruct("holder").getStruct("character"));
		if (null == holder) { return false; } // tag associated with non-existent character.
		checkFatal(holder,"holder","province "+_tag);
		_holder = holder.getRuler();
		_holder._titles.add(this);
		StructField date = f.getStruct("holder").getStruct("startdate");
		_year  = (short)date.getBase("year").getAsInt();
		_month = (byte)getMonth(date.getBase("month").get());
		_day   = (byte)date.getBase("day").getAsInt();
		if (_holder.Characters()._tag.equals(_tag))
			_holder._main=this;
		for (StructField cx : f.getAllStruct("claim")) {
			Characters c = Characters.search(cx.getStruct("character"));
			_claims.add(c);
			c.getRuler()._claims.add(this);
		}
		return true;
	}
	
	private int getMonth(String m) {
		if (m.startsWith("jan")) return 1;
		if (m.startsWith("feb")) return 2;
		if (m.startsWith("mar")) return 3;
		if (m.startsWith("apr")) return 4;
		if (m.startsWith("may")) return 5;
		if (m.startsWith("jun")) return 6;
		if (m.startsWith("jul")) return 7;
		if (m.startsWith("aug")) return 8;
		if (m.startsWith("sep")) return 9;
		if (m.startsWith("oct")) return 10;
		if (m.startsWith("nov")) return 11;
		if (m.startsWith("dec")) return 12;
		return 1;
	}
		
	public Title()                       {}
	public String getKey()               { return _tag; }
	public void   setKey(String k)       { _tag=k; }
	public void   setSuccessor(String k) { _tag=k+"\0"; }
	
}
