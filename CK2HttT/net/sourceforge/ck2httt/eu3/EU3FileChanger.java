package net.sourceforge.ck2httt.eu3;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.File;
import java.util.Locale;

import net.sourceforge.ck2httt.pxAnalyzer.PXAdvancedAnalyzer;
import net.sourceforge.ck2httt.pxAnalyzer.PXTree.BaseField;
import net.sourceforge.ck2httt.pxAnalyzer.PXTree.Field;
import net.sourceforge.ck2httt.pxAnalyzer.PXTree.StructField;
import net.sourceforge.ck2httt.pxAnalyzer.PXTree.Template;


abstract public class EU3FileChanger {

	
	protected   PXAdvancedAnalyzer _a;
	public StructField        _root;
	public    File               _file;
	protected StructField[]      _list = new StructField[20]; 
	protected int                _nb=0;
	
	//utilities
	static private String[] resize(String[] x) {
		if (x==null) return new String[2];
		if (x[x.length-1]==null) return x;
		String[] y = new String[2*x.length];
		System.arraycopy(x, 0, y, 0, x.length);
		return y;
	}
	static protected String[] addTo(String[] a, String s) {
		if (s==null) return a;
		a = resize(a);
		int i=0;
	    for (i=0; i<a.length && a[i]!=null; i++)
		    if (a[i].equals(s)) return a;
		a[i]=s;
	    return a;
	}
	static protected String[] remFrom(String[] a, String s) {
		int i=0;
		boolean found=false;
	    for (i=0; i<a.length && a[i]!=null; i++)
	    	if (found)               { a[i-1]=a[i]; a[i]=null; }
	    	else if (a[i].equals(s)) { a[i]=null; found=true;  }
	    return a;
	}
	
	abstract protected Template getTemplate();
	abstract protected void     applyChanges();
		
	protected void applyBase(String name, String value) {
		BaseField f = _root.getBase(name);
		if (value==null) {
			if (f!=null) f.delete();
		}
		else if (f!=null)  f.set(value);
		else _root.addField(_root.getFirstIndex(name,getTemplate()), new BaseField(name,value));
	}
	protected void applyBase(String name, int value) {
	    applyBase(name,(new Integer(value)).toString());
	}
	protected void applyBase(String name, float value) {
		applyBase(name, String.format(Locale.US,"%.3f", value));
	}
	protected void applyBaseList(String name, String[] value) {
		BaseField[] list = _root.getAllBase(name);
		if (value==null || value[0]==null) {
			for (BaseField f : list) f.delete();
			return;
		}
		boolean[]   used = (list!=null) ? new boolean[list.length] : null;
		boolean[]   done = new boolean[value.length];
		if (list!=null) {
			//check the values already present : don't change them
			for (int i=0; i<value.length; i++) {
				done[i]=false;
				if (value[i]==null)
					done[i]=true;
				else
			        for (int j=0; j<list.length; j++)
					    if (value[i].equals(list[j].get())) {
						    used[j]=true;
						    done[i]=true;
					    }
			}
			//replace unused entries and add new fields if not enough
			for (int i=0; i<value.length; i++) {
				if (done[i]) continue;
			    for (int j=0; j<list.length; j++) {
					if (used[j]) continue;
					list[j].set(value[i]);
					used[j]=true;
					done[i]=true;
			    }
			    if (!done[i]) {
			    	_root.addField(_root.getLastIndex(name,getTemplate()), new BaseField(name,value[i]));
			    	done[i]=true;
			    }
			}
			//delete unused fields
		    for (int j=0; j<list.length; j++)
				if (!used[j])
					list[j].delete();
		}
	}
	protected void applyYesFields(String[] list, String[] chosen) {
		for (String b : list) {
			boolean done=false;
			if (chosen!=null)
				for (String bb : chosen) {
					if (bb!=null && bb.equals(b)) {
						BaseField f = _root.getBase(b);
						if (f==null)
							_root.addField(_root.getFirstIndex(b,getTemplate()), new BaseField(b,"yes"));
						done = true;
					}
				}
			if (!done) {
				BaseField f = _root.getBase(b);
				if (f!=null) f.delete();
			}
		}
	}
	protected void applyIndependentField(StructField f) {
		_list[_nb++] = f;
	}
	
	public EU3FileChanger(String filename, String filter) throws IOException {
		_file = new File(filename);
		_a = new PXAdvancedAnalyzer(filename,true);
		_root = (filter==null) ? _a.analyze() : _a.analyze(filter);
	}

	public void write (String out) throws IOException {
		applyChanges();
		PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(out, true), "ISO-8859-1"));		
		_a.write(pw);
		for (int i=0; i<_nb; i++) {
			pw.println();
			_list[i].write(pw, false);
		}
		pw.close();
	}
	
	public void delDateField(Field<?> f) {
		if ((f instanceof StructField) && f.name().length()>4 && f.name().charAt(4)=='.')
		    f.delete();
	}

	
}
