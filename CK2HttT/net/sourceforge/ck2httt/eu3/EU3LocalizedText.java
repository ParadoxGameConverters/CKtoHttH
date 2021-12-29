package net.sourceforge.ck2httt.eu3;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeSet;

public class EU3LocalizedText {

	// I'm only bothering with the English names here.
/*
PROV6;Skåne;Skåne;Skåne;Skania;Escania;Skåne;Skåne;Sklne;;;;;;x
BRIT;Brittany;Bretagne;Bretagne;Bretaña;Bretagna;;;;;;X
PROV10;Mide;;;Mide;;;;;;;X
*/
	private static Map<String, String> EU3LocalizedText = new HashMap<String, String>();
	
	private EU3LocalizedText() {
	}
		
	public static void init(String eu3Path) throws IOException {
		BufferedReader worldBR = null;
		
		File textDir = new File(eu3Path + File.separator + "localisation" + File.separator);
		String[] files = textDir.list();
		NavigableSet<File> sortedFiles = new TreeSet<File>(new Comparator<File>() {
			public int compare(File f1, File f2) {
				if (f1.lastModified() < f2.lastModified()) {
					return -1;
				} else if (f1.lastModified() > f2.lastModified()) {
					return 1;
				} else {
					return f1.compareTo(f2);
				}
			}
		});
		for (String s: files) {
			sortedFiles.add(new File(textDir, s));
		}				

		for (File f: sortedFiles) {
			worldBR = new BufferedReader(new FileReader(f));
					
			String line = null;
			while ((line = worldBR.readLine()) != null) {
				if (!line.startsWith("#")) {
					String[] names = line.split(";");
					if (names.length > 2) {
						EU3LocalizedText.put(names[0], names[1]);
					}
				}
			}
			worldBR.close();
		}
	}
	
	public static String getName(String tag) {
		return EU3LocalizedText.get(tag);
	}
	
	public static void main(String[] args) throws Exception {
		init(null);
	}	
}	
