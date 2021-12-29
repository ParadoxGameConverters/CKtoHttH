package net.sourceforge.ck2httt.utils;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import java.io.File;
import java.io.FilenameFilter;

public class Selector {
	
	static final private String ckExe = "crusaders.exe";
	static final private String euExe = "eu3.exe";

	static private class SaveFilter extends FileFilter {
		public String getDescription() { return "Crusader Kings Save File"; }
		public boolean accept(File f)  {
			return f.getName().endsWith(".eug");
		}
	}

	static private class DirFilter extends FileFilter {
		String _txt;
		DirFilter(String txt)          { _txt=txt; }
		public String getDescription() { return _txt; }
		public boolean accept(File f)  {
			return f.isDirectory();
		}
	}
	
	static private class ExeFilter implements FilenameFilter {
		String _name;
		ExeFilter(String s) { _name=s; }
		public boolean accept(File dir, String name) { return _name.toLowerCase().equals(name.toLowerCase()); }
	}

	static public File getCKDir() {
	    JFileChooser chooser = new JFileChooser();
	    chooser.setFileFilter(new DirFilter("Crusader Kings Install Directory"));
	    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
	    chooser.setAcceptAllFileFilterUsed(false);
	    do {
		    int returnVal = chooser.showOpenDialog(null);
		    if (returnVal == JFileChooser.APPROVE_OPTION) {
		        System.out.println("Crusader Kings Directory : " +
		            chooser.getSelectedFile().getAbsolutePath());
			    File f = chooser.getSelectedFile();
			    if (f.list(new ExeFilter(ckExe)).length>0) return f;
			    System.out.println("This is not a valid Crusader Kings Install Directory ; "+ckExe+" not found");
		    }
		    else
		    	System.exit(0);
	    }
		while (true);
	}
	
	static public File getEU3Dir() {
	    JFileChooser chooser = new JFileChooser();
	    chooser.setFileFilter(new DirFilter("Europa Universalis 3 Install Directory"));
	    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
	    chooser.setAcceptAllFileFilterUsed(false);
	    do {
		    int returnVal = chooser.showOpenDialog(null);
		    if (returnVal == JFileChooser.APPROVE_OPTION) {
		        System.out.println("Europa Universalis 3 Directory : " +
		            chooser.getSelectedFile().getAbsolutePath());
			    File f = chooser.getSelectedFile();
			    if (f.list(new ExeFilter(euExe)).length>0) return f;
			    System.out.println("This is not a valid Europa Universalis 3 Install Directory ; "+euExe+" not found");
		    }
		    else
		    	System.exit(0);
	    }
	    while (true);
	}	
	
	static public File getModDir(File EU3path) {
	    JFileChooser chooser = new JFileChooser();
	    chooser.setFileFilter(new DirFilter("Mod to use"));
	    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
	    chooser.setAcceptAllFileFilterUsed(false);
	    File savePath = new File(EU3path.getAbsolutePath()+"/mod");
	    chooser.setCurrentDirectory(savePath);
	    int returnVal = chooser.showOpenDialog(null);
	    if (returnVal == JFileChooser.APPROVE_OPTION) {
	        System.out.println("Mod used : " +
	            chooser.getSelectedFile().getAbsolutePath());
		    return chooser.getSelectedFile();
	    }
	    System.exit(0);
	    return null;
	}	
	
	static public File getSaveGame(File CKpath) {
	    JFileChooser chooser = new JFileChooser();
	    chooser.setFileFilter(new SaveFilter());
	    chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
	    chooser.setAcceptAllFileFilterUsed(false);
	    File savePath = new File(CKpath.getAbsolutePath()+"/scenarios/save games");
	    chooser.setCurrentDirectory(savePath);
	    int returnVal = chooser.showOpenDialog(null);
	    if (returnVal == JFileChooser.APPROVE_OPTION) {
	        System.out.println("Save File : " +
	            chooser.getSelectedFile().getAbsolutePath());
		    return chooser.getSelectedFile();
	    }
	    System.exit(0);
	    return null;
	}	
}
