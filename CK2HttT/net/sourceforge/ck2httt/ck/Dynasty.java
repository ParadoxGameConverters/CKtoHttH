package net.sourceforge.ck2httt.ck;

import java.io.FileNotFoundException;
import java.io.IOException;

import net.sourceforge.ck2httt.pxAnalyzer.PXAdvancedAnalyzer;
import net.sourceforge.ck2httt.pxAnalyzer.PXTree.BaseField;
import net.sourceforge.ck2httt.pxAnalyzer.PXTree.Field;
import net.sourceforge.ck2httt.pxAnalyzer.PXTree.StructField;
import net.sourceforge.ck2httt.utils.FieldLoadable;
import net.sourceforge.ck2httt.utils.FieldSet;
import net.sourceforge.ck2httt.utils.Key;
import net.sourceforge.ck2httt.utils.KeyString;


public class Dynasty implements FieldLoadable, Key<String> {

    //inital data
    /** dynasty id */
    public String   _id;
    /** dynasty name */
    public String   _name;

    static public FieldSet<Dynasty> __list = new FieldSet<Dynasty>(Dynasty.class);

    static void loadAll(String CKPath, StructField root) throws FileNotFoundException, IOException {
		DynastyData.load(CKPath+"db/dynasties.txt");
		__list.load(root);
	}

	public boolean load(Field<?> x) {
		if (!x.name().equals("dynasty")) return false;
		StructField f = (StructField)x;
		_id = f.getBase("id").get();
                if (_id == null)
                {
                    _id = "";
                }

                _name = f.getBase("name").get();
                if (_name==null)
                {
                    _name = "";
                }
                
		return true;
	}

        public Dynasty() {}
        
        static public String getDynastyName(int id) {
		DynastyData dd = DynastyData.__list.search(Integer.toString(id));
		
		return dd._name;
	}

        static public class DynastyData extends KeyString implements FieldLoadable{
                public byte     _idx;
		private String  _id;
		private String  _name;

		public static FieldSet<DynastyData> __list = new FieldSet<DynastyData>(DynastyData.class);
                public DynastyData() {}

		static public void load(String filename) throws IOException {
			String _filter = "{dynasty{id{id},name}}";
			__list.load(new PXAdvancedAnalyzer(filename,true).analyze(_filter));
		}

                public boolean load(Field<?> g) {
			StructField f = (StructField)g;
			_name = f._name._value;
			_key = _name;
			StructField x = f.getStruct("id");
			if (x!=null) {
				BaseField y = ((StructField)x).getBase("id");
				    if (y!=null) _id=y.get();
			}
                        BaseField z = ((StructField)f).getBase("name");
                        if (z!=null) _name=z.getUnquoted();
                        _key = _id;
                    
			return true;
		}
	}

        public String getKey()               { return _id; }
        public void   setKey(String k)       { _id=k; }
	public void   setSuccessor(String k) { _id=k+"\0"; }
}
