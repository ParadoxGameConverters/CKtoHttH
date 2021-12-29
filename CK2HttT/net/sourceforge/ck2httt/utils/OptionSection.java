package net.sourceforge.ck2httt.utils;

import net.sourceforge.ck2httt.pxAnalyzer.PXTree.BaseField;
import net.sourceforge.ck2httt.pxAnalyzer.PXTree.Field;
import net.sourceforge.ck2httt.pxAnalyzer.PXTree.ListField;
import net.sourceforge.ck2httt.pxAnalyzer.PXTree.StructField;

/**
 * Easily manage different configurations for data sections.
 * A data section is in the form:
 * 
 * tag = {
 *     opt1 = {
 *         use = { type=standalone }
 *         use = { type=differential base=optN } 
 *         # only one of the above
 *         section1= {
 *             .add = {
 *                whatever...
 *             }
 *             .rem = {
 *                whatever...
 *             }
 *         }
 *         section2= {
 *             ....
 *         }
 *         ....
 *     }
 *     opt2 = {
 *         use = { type=standalone }
 *         use = { type=differential base=optn } 
 *         # only one of the above
 *         section1= {
 *         }
 *         section2= {
 *         }
 *         ....
 *     }
 *     ...
 * }
 * 
 * opt1, opt2 etc are the option names defined in the selection section.
 * whatever the section1 etc contain is up to the user ; it has to be a structure.
 * The user just has to define how these subsections are loaded (and updated if
 * differential options are used, i.e. when a section 
 * 
 * @author yprelot
 *
 */
public abstract class OptionSection {
	
	// base option selected
	static private String __option=null;
	
	public static String getOption() {
		return __option;
	}
	
	public static short getMinBirth() {
		return (short) 1374;
	}
	
	public static short getStartYear() {
		short startYear = (short) 1399; 
		return startYear;
	}

	public static short getStartMonth() {
		return (short) 10;
	}

	public static short getStartDay() {
		return (short) 140;
	}
	
	public static String getStartDate() {
		return "1399.10.14";
	}	
	
	static private class OptionResult {
		private OptionResult(String base, StructField data, StructField root) {
			_data = data;
			_base = read(root,base);
		}
		final private StructField   _data;  //this option structure
		final private OptionResult  _base;  //based on this option
		
		static public OptionResult read(StructField root, String option) {
			if (option==null) return null;
			StructField opt = root.getStruct(option);
			if (opt==null)
				throw new IllegalStateException("undefined definition list : "+option+" for "+root.name());
			StructField use = opt.getStruct("use");
			String type = use.getBase("type").get();
			if (type.equals("stand_alone"))
				return new OptionResult(null,opt,root);
			else if (type.equals("differential")) {
				BaseField base=use.getBase("base");
				if (base==null)
					throw new IllegalStateException("no base definition defined for differential tag list : "+option+" in section"+ root.name());
				return new OptionResult(base.get(),opt,root);
			}
			else
				throw new IllegalStateException("unrecognized type for " + root.name() + " section, option " + option + " : " + type +" ; only stand_alone or differential allowed");
		}
		
		public void load(String section, OptionSection with) {
			//if that option is not final, load base section data first
			if (_base!=null)
				_base.load(section,with);

			//find the appropriate section to load
			Field<?> f = _data.get(section);
			if (f==null) return;  //hum... shouldn't that be an error ?
			Field<?> add = null;
			Field<?> rem = null;
			
			//check if we have .add or .remove special subsections
			if (f instanceof StructField) {
				add = f.get(".add");
				rem = f.get(".rem");
			}
			
			// now read the data ; ignore declared empty sections (ListField with size 0)
			if (add==null && rem==null && (!(f instanceof ListField) || ((ListField)f)._data.size()!=0)) {
				//no special subsections ? f is the data
			    with.replace(f,section);
			}
			else {
				if (rem!=null && (!(rem instanceof ListField) || ((ListField)rem)._data.size()!=0))
			        with.remove(rem,section);
				if (add!=null && (!(add instanceof ListField) || ((ListField)add)._data.size()!=0))
			        with.add(add,section);
			}
		}		
	}
	
	static public void setOption(String opt) {
		__option = opt;
	}
	
	/**
	 * Define these functions to load/remove/replace the data given.
	 * Since an option block may contain several sub-sections, and because these
	 * sections will likely have to use different methods to be loaded, the
	 * section name is also given. When there is only one section, you will
	 * likely ignore it.
	 * @param data    data to load (StructField or ListField actually)
	 * @param section name of the section loaded
	 */
	abstract public void add(Field<?> data, String section);
	abstract public void remove(Field<?> data, String section);
	abstract public void replace(Field<?> data, String section);
	
	private final OptionResult _r;
	
	/**
	 * Open a section with options using the current option (presumably set at start)
	 * @param data root StructField for that option section
	 */
	protected OptionSection(StructField data) {
		_r = OptionResult.read(data,__option);
	}

	/**
	 * Loads a subsection info, including all options it depends on
	 * Takes care of the .add or .rem subsections
	 * @param section to load
	 */
	public void load(String section) {
	    _r.load(section, this);
	}
	
}
