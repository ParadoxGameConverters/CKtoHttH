package net.sourceforge.ck2httt.rules;

import java.io.CharArrayWriter;
import java.io.PrintWriter;
import java.util.TreeSet;
import java.util.Vector;

import net.sourceforge.ck2httt.ck.Country;
import net.sourceforge.ck2httt.ck.County;
import net.sourceforge.ck2httt.ck.Title;
import net.sourceforge.ck2httt.pxAnalyzer.PXTree.BaseField;
import net.sourceforge.ck2httt.pxAnalyzer.PXTree.Field;
import net.sourceforge.ck2httt.pxAnalyzer.PXTree.StructField;
import net.sourceforge.ck2httt.utils.FieldLoadable;
import net.sourceforge.ck2httt.utils.FieldSet;
import net.sourceforge.ck2httt.utils.KeyString;
import net.sourceforge.ck2httt.utils.Rnd;
import net.sourceforge.ck2httt.utils.TreeSetSection;

public class TagCvRules {
	
	//pool of still available tags
	static private TreeSet<String> __unavailableTags = new TreeSet<String>();
	static private TreeSet<String> __listTags = new TreeSet<String>();
	static private TreeSet<String> __usedTags = new TreeSet<String>();
	
	static public boolean isFromTagPool(String tag) {
		boolean res = __listTags.contains(tag);
		res = res | __usedTags.contains(tag);
		if (__unavailableTags.contains(tag)) {
			res = false;
		}
		
		return res;
	}
	
	static public TreeSet<String> getListTags() {
		return __listTags;
	}
	
	static public boolean isUsed(String tag) {
		return __usedTags.contains(tag);
	}
	
	static public class TagMapping extends KeyString implements FieldLoadable {
		public String         _tagCK;
		public Vector<String> _tagEU3=new Vector<String>();
		public TagMapping() {}
		public boolean load(Field<?> f) {
			_tagCK = f.name();
			String tags[] = ((BaseField)f).getUnquoted().split(",");
			for (String t : tags)
			   _tagEU3.add(t.trim());
			_key=_tagCK;
			return true;
		}
	}
		
	private static FieldSet<TagMapping> __mappingList = new FieldSet<TagMapping>(TagMapping.class);

	/**
	 * Loading tags is a complex operation because tags depends on which extension/mod is in use
	 * We have to determine which set(s) to load and take tke appropriate actions when a differential
	 * tag definition list is found
	 * @param root   the root field
	 * @param option name of the option
	 */
  	static public void load(StructField root) {
  		StructField tags = root.getStruct("tags");
 		new TreeSetSection(tags,__listTags).load("tag_pool");
 		new TreeSetSection(tags,__usedTags).load("unavailable");
 		__mappingList.loadOptionSection(tags,"convert");
  		__listTags.removeAll(__unavailableTags);
  	}
	
	
	static public String[] getOptions(StructField root) {
		StructField tags = root.getStruct("tags");
		String[] list = new String[tags._data.size()];
		int i=0;
		for (Field<?> f : tags._data)
			list[i++] = f.name();
		return list;
	}

	static public String[] getOptionsNames(StructField root) {
		StructField tags = root.getStruct("tags");
		String[] list = new String[tags._data.size()];
		int i=0;
		for (Field<?> f : tags._data) {
			BaseField name = ((StructField)f).getBase("name");
			if (name==null)
				throw new IllegalStateException("a tag definition list has no name");
			list[i++] = name.get();
		}
		return list;
	}
	
	static public int nbTags() {
		return __listTags.size();
	}
	
    /**
     * Try to appropriate a tag for that country
     * @param tagCK    CK country being checked for
     * @param primary  true if we attempt to fetch to nominal EU3 tag, false if we also consider secondary tags
     * @return
     */
	static private String takeTag(String tagCK, boolean primary) {
		TagMapping r = __mappingList.search(tagCK);
		if (tagCK.equals("FRAN")) {
			int i=0;
			i++;
	    }
		if (r==null) return null;  //may happen for some tags voluntarily removed from the list, such as GERM or ITAL
		for (String tag : r._tagEU3) {
			if (!__usedTags.contains(tag) && !__unavailableTags.contains(tag)) { //tag not yet used : take it!
				__listTags.remove(tag);
				__usedTags.add(tag);
			    return tag;
			}
			if (primary) return null;
		}
		return null;
	}
		
	/**
	 * Get a tag for the country
	 * @param c     country for which the tag is generated
	 * @param limit a parameter limiting the depth of the search
	 *              0 : get nominal tag (for country tag)
	 *              1 : also try tag for owned county beginning by capital
	 *              2 : also try any tag for owning duchy provinces
	 *              3 : also try any tag for owning kingdom provinces
	 *              otherwise : get random tag...
	 * @param primary if true, limit search to the primary tag
	 *                otherwise, try all declared acceptable tags
	 * @return
	 */
	static public String getTag(Country c, int limit) {
		//try nominal tag for country
		String tag = takeTag(c._tag,true);
		if (tag!=null) return tag;
		if (limit==0) return null;
		//try alternate tag for country
		tag = takeTag(c._tag,false);
		if (tag!=null) return tag;
		//try tag for capital county
		if (c._capital!=null) {
			if (c._capital._duchy!=null) {
			    tag = takeTag(c._capital._duchy._tag,false);
			    if (tag!=null) return tag;
			}
		    tag = takeTag(makeCKtag(c._capital._id),false);
		    if (tag!=null) return tag;
		}
		//try any tag for any directly owned county
		for (County x : c._owned) {
			tag = takeTag(makeCKtag(x._id),false);
			if (tag!=null) return tag;
		}
		if (limit==1) return null;
		//try any tag at Duchy level
		tag = getRndTag(c,Title.Tier.DUCHY);
		if (tag!=null) return tag;
		if (limit==2) return null;
		//try any tag at Kingdom level
		tag = getRndTag(c,Title.Tier.KINGDOM);
		if (tag!=null) return tag;
		if (limit==3) return null;
		//try any tag
		if (__listTags.size()==0) return null;
		return getRndTag(c,null);
	}
	
	//getting a tag by watching owned eu3 provinces ; list ends with capital.
	static public String getTag(String[] eu3Owned) {
		int i=eu3Owned.length;
		//go in reverse order to check capital first
		while(i>0 && eu3Owned[--i]==null);
		while (i>=0) {
			String tag = eu3Owned[i--];
			if (__listTags.contains(tag)) { //tag not yet used : take it!
				__listTags.remove(tag);
			    return tag;
			}
		}
		return null;
	}
	
	/**
	 * Get random free tag from any province "owned" by the whole country
	 * or by one of it's over-lord.
	 * Choice up to that tier level. If tier is null, choice among all
	 * remaining tags.
	 */
	static private String getRndTag(Country c, Title.Tier tier) {
		if (tier==null) {
			String[] s = __listTags.toArray(new String[__listTags.size()]);
			String tag = s[Rnd.get().nextInt(__listTags.size())];
			__listTags.remove(tag);
			return tag;
		}
		else {
			Country k = c;
			while (k._liege!=null && k._title._tier!=tier) k=k._liege;
			TreeSet<String> list = new TreeSet<String>();
			for (County x : k.allProvinces())
				list.add(makeCKtag(x._id));
			if (list.size()==0) return null;
			if (list.size()==1) return takeTag(list.first(),true);
			String[] s = list.toArray(new String[list.size()]);
			return takeTag(s[Rnd.get().nextInt(list.size())],true);
		}
	}
	
	static private String makeCKtag(String id) {
		int n = (new Integer(id)).intValue();
		CharArrayWriter caw = new CharArrayWriter(4);
		PrintWriter out = new PrintWriter(caw);
		out.format("C%03d",n);
		return caw.toString();
	}

}
