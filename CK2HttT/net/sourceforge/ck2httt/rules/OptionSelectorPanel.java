package net.sourceforge.ck2httt.rules;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;

import net.sourceforge.ck2httt.pxAnalyzer.PXTree.BaseField;
import net.sourceforge.ck2httt.pxAnalyzer.PXTree.Field;
import net.sourceforge.ck2httt.pxAnalyzer.PXTree.StructField;


import java.util.TreeMap;

class OptionSelectorPanel extends Panel implements ActionListener {
	
	static final long serialVersionUID = 0L;
	
	static public class VersionSelection {
		static public boolean __autodetect;       //autodetection ?
		static public String  __default=null;     //default value ?
		static private int    __n=0;
		public String _autodetect=null;
		public String _name=null;
		public String _accro=null;
		public int    _order=-1;
		static public TreeMap<String,VersionSelection> __versions = new TreeMap<String,VersionSelection>();
		static public void load(StructField data) {
			if (data==null) return;
			int n=0;
			for (Field<?> f : data._data) {
				if (n++==0) {
					if (f.name().equals("autodetect")) { //init autodetection scheme
						String x=((BaseField)f).get();
						if (x.equals("true"))
							__autodetect=true;
						else if (x.equals("false"))
							__autodetect=false;
						else {
							__autodetect=false;
							__default=x;
						}
					}
					else {
						System.out.println("FATAL ERROR");
						System.out.println("data file misformed ; autodetect section not found or misplaced");
						System.exit(0);
					}
				}
				else {
					StructField x = (StructField)f;
					VersionSelection v = new VersionSelection();
					v._order = __n++;
					v._accro=x.name();
					v._name=x.getBase("name").getUnquoted();
					if (__autodetect) {
						BaseField y = x.getBase("autodetect");
						String z = y.getUnquoted();
						if (z==null || z.length()==0) {
							System.out.println("FATAL ERROR");
						    System.out.println("data file misformed ; autodetect info not found for "+v._accro);
						    System.exit(0);
						}
						else if (z.equals("default")) {
							v._autodetect=null;
							__default=v._accro;
						}
						else
							v._autodetect=z;
					}
					__versions.put(v._accro, v);
				}
			}			
		}
		static public VersionSelection get(String accro) {
			return __versions.get(accro);
		}
	}
	
	final int _width=250;  //largeur d'un élément "de base"
	final int _height=30;  //hauteur d'un élément "de base"
	int       _nb=0;

	static private String   __choice=null;
	static private JDialog  __dialog=null;
	
	/**
	 * checks if all the tags (comma separated) are found
	 * @param tags  countries to check in the game
	 * @return true if all countries found
	 */
	static private boolean checkVersion(String tags) {
		if (tags==null) return false; 
		String tag[] = tags.split(",");
		for (String t : tag) {
			if (net.sourceforge.ck2httt.eu3.EU3Country.exists(t)!=net.sourceforge.ck2httt.eu3.EU3Country.CountryExistence.EXISTS)
				return false;
		}
		return true;
	}

	public OptionSelectorPanel(StructField data) {
		initPanel();
		initButtons(data);
	}
			
	private void initPanel() {
		FlowLayout layout = new FlowLayout();
		layout.setVgap(0);
		setLayout(layout);
	}
	
	private void initButtons(StructField data) {
		VersionSelection.load(data);
		int n=-1;
		for (VersionSelection v : VersionSelection.__versions.values()) {
			JButton b = new JButton(v._name);
			b.setPreferredSize(new Dimension(_width, _height));
			b.setMnemonic(_nb);
			b.setActionCommand(v._accro);
			b.addActionListener(this);
			add(b);
			_nb++;
			//try to autodetect if this is the version to convert to
			if (VersionSelection.__autodetect && checkVersion(v._autodetect)) {
	    	    VersionSelection.__default=null;
		    	if (n<v._order) {
		    		n=v._order;
		    	    __choice=v._accro;
		    	}
			}
		}
		setPreferredSize(new Dimension(_width, _nb*_height));
	}
	
    public void actionPerformed(ActionEvent e) {
    	__choice = e.getActionCommand();
    	__dialog.setVisible(false);
    	__dialog.dispose();
    	__dialog=null;
    }
    
    static private String getBaseOption(StructField data) {
        Panel contentPane = new OptionSelectorPanel(data);
        //at this point, we may already know what we are converting to
        //  * manual selection happens if and only if no __choice and no __default
        //  * we have a __default if the autodetect setting said so or their was
        //    a default entry in the list and we had no match
        if (VersionSelection.__autodetect && __choice!=null)
        	return __choice;
    	if (VersionSelection.__default!=null) 
    		return VersionSelection.__default;
        __dialog = new JDialog((JFrame)null,"Select option",true);
        __dialog.setContentPane(contentPane);
        __dialog.pack();
        __dialog.setResizable(false);
        __dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        __dialog.setLocationRelativeTo(null);
        __dialog.setVisible(true);
        return __choice;
    }

    static public String getOption(StructField data) {
        String opt = getBaseOption(data);
        if (opt==null) {
        	System.out.println("CONVERSION ABORTED");
        	System.exit(0);
        }
        else {
        	System.out.println("Converting for "+VersionSelection.get(opt)._name);
        }
        return opt;
    }

}