package net.sourceforge.ck2httt.pxAnalyzer;

import java.io.Writer;
import java.io.IOException;
import java.text.ParseException;

import net.sourceforge.ck2httt.pxAnalyzer.Analyzer.Hooks;
import net.sourceforge.ck2httt.pxAnalyzer.PXTree.*;


/**
 * This class comes at the top of this package and provides an high level
 * class to analyze/modify paradox save files. It extends the {@link PXTreeChooser}
 * in order to make everything as easy as possible for the user. To fully use
 * it, you'll still have to understand patterns (from {@link Patterns}) and
 * fields (from {@link PXTree.Field}).<p>
 * <p>
 * In most cases, it should be sufficient to derive this class to do what you
 * intend with a save file.<p>
 * <p>
 * Here, you don't much bother about the process of analysis ; you can concentrate
 * on what you really want to do as a client application. Your interactions with
 * the parser/analyzer are rather limited (yet powerful), allowing you to dynamically
 * discard unwanted branches (to avoid useless memory usage) and skip parts of the
 * file (for faster processing).<p>
 * <p>
 * You interact with the parser/analyzer first by defining the field hierarchy you
 * want to analyze {pattern from PXChooser used as a filter}<p>
 * You have then the opportunity to define a callback for each analyzed field.
 * That callback can interact with the analyzing process depending on it's return
 * value. You should seriously consider using the callbacks defined in
 * {@link PXTreeChooser.TreeCallbacks} as they will make your life definitely
 * easier. Of course you can devise your own Callback class. A callback can be used
 * for other purposes as well such as extracting information on the fly, especially
 * if that information can influence the way the analyzer must behave later (for
 * example by using previous information to modify the return values of later
 * callbacks).<p>
 * <p>
 * At the end of the analysis, you get the tree of analyzed fields ; you can read
 * and modify that tree. And in the case you change it, you can write the modified
 * result in a new file by using the {@link #write(Writer)} method. Note that you don't have
 * to fully parse a file to be able to change it ; a better explanation for this process
 * is described in {@link PXTree}<p>
 * You also have the opportunity to again use callbacks (of a different kind) on the
 * tree after it has been built by using the {@link PXTreeChooser.BasicCallback} class
 * and the pattern of fields yout need to explore them and in the order you want, by using
 * one of the {@link PXTreeChooser#applyTemplate(String, BasicCallback[])} call.<br>
 * If you use this commodity, be sure to read how it works in {@link PXTreeChooser}.<p>
 * 
 * @see PXChooser
 * @see PXTreeChooser
 * @see Patterns
 * @see PXTree
 * @author Copyright 2007 Yves Prélot ; distributed under the terms of the GNU General Public License
 *
 */
public class PXAdvancedAnalyzer extends PXTreeChooser {

	private FileLoader.BufferReader _r=null;
	private byte[]                  _data=null;
	
	/**
	 * Constructor
	 * @param filename  the file you want to analyze
	 * @param fast      use the fast Parser instead of the slow and secure one 
	 * @throws IOException
	 */
	public PXAdvancedAnalyzer(String filename, boolean fast) throws IOException {
		_r = FileLoader.load(filename, FileLoader.Mode.READ);
		if (fast) _data = _r.getAll();
	}
	
	/**
	 * gets the array of callbacks used during the analysis.
	 * you should seriously consider overriding this method as the
	 * default implementation returns nothing.<p>
	 * @return the array of callbacks required to run your analysis
	 */
	public TreeCallbacks[] getCallbacks() { return null; }
	
	/**
	 * Analyzes the file using the given filter. Any required callbacks
	 * are recovered through the getMatchers method.<br>
	 * It is illegal to analyze the file more than once with the
	 * same PXAdvancedAnalyzer instance.<p>
	 * @param choice  The filter used for the analysis.
	 * @return        The top level structure containing the whole
	 * analyzed tree (which is probably not the whole data from the
	 * file, depending on your filter).
	 */
	public StructField analyze(String choice) {
		if (_h._done) throw new IllegalStateException();
		if (choice==null || choice.length()==0)
			return analyze();
		Hooks h;
		try {
	        h = get(choice, getCallbacks());
		}
		catch (ParseException e) {
			System.out.println("choice list analysis failed : ");
			e.printStackTrace(System.out);
			return null;
		}
		boolean b = (_data!=null) ? Analyzer.analyze(_data,h) : Analyzer.analyze(_r,h);
		if (!b) {
			System.out.println("analysis failed :" + Analyzer._error);
			if (_r!=null) {
			    int max = Analyzer._errorPosition+100;
			    int min = Analyzer._errorPosition-100;
			    if (_r._buffer.limit()<max) max=_r._buffer.limit();
			    if (min<0) min=0;
			    for (int i=min; i<max; i++)
				    System.out.print((char)_r._buffer.get(i));
			}
			return null;
		}
		else {
			return getDataTree();
		}
	 }

	/**
	 * Analyzes the file with no filter. This will build the full tree
	 * of Fields and may require a lot of memory.<br>
	 * It is illegal to analyze the file more than once with the
	 * same PXAdvancedAnalyzer instance.<p>
	 * @return  The top level structure containing the whole
	 * analyzed tree. On large files, you may require a lot of memory.
	 */
	public StructField analyze() {
		if (_h._done) throw new IllegalStateException();
		boolean b = (_data!=null) ? Analyzer.analyze(_data,_h) : Analyzer.analyze(_r,_h);
		if (!b) {
			System.out.println("analysis failed :" + Analyzer._error);
			if (_r!=null) {
			    int max = Analyzer._errorPosition+100;
			    if (_r._buffer.limit()<max) max=_r._buffer.limit();
			    for (int i=Math.max(0, Analyzer._errorPosition-100); i<max; i++)
				    System.out.print((char)_r._buffer.get(i));
			}
			return null;
		}
		else {
			return getDataTree();
		}
	 }
	
	/**
	 * Writes the (modified) Field tree.<br>
	 * Depending on your modification and the completeness of your analysis, the
	 * result may be safe or not. Here are, in decreasing order of safety, what
	 * you can do:
	 * <ul>
	 *     <li>modify an existing field : safe</li>
	 *     <li>delete an existing field : safe</li>
	 *     <li>add a new field : safe only if the containing field has been fully analyzed</li>
	 * </ul>
	 * It should be noted that all the combinations of modification/deletion/additions have not been
	 * tested thoroughly : The result from calls to this method should thus be checked for correctness
	 * in your context in the case you heavily modify adjacent fields in the same container field.<p>
	 * It is important to note that you do not have to load the whole data into memory to be able to
	 * edit a file : for modification/deletion, you only have to load the corresponding fields. For
	 * addition, it is safer (see above) to have the whole containing structure loaded (but not
	 * necessarily the untouched sub-structures).<p>
	 * Note that the result is different from calling {@link PXTree.StructField#write(Writer,boolean)}
	 * unless you parsed the whole tree. Be sure you understand why.<p>
	 * 
	 * @param out the output stream writer
	 * @throws IOException
	 * @see Field
	 * @see PXTree
	 */
	public void write(Writer out) throws IOException {
	    _r._buffer.rewind();
		getDataTree().write(_r,out);
	}
	
}
