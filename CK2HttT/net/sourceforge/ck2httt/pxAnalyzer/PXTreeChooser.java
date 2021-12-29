package net.sourceforge.ck2httt.pxAnalyzer;
import java.text.ParseException;
import java.util.LinkedList;
import java.io.Reader;

import net.sourceforge.ck2httt.pxAnalyzer.Analyzer.Hooks;
import net.sourceforge.ck2httt.pxAnalyzer.PXChooser.*;
import net.sourceforge.ck2httt.pxAnalyzer.PXTree.*;
import net.sourceforge.ck2httt.pxAnalyzer.Patterns.Callbacks;
import net.sourceforge.ck2httt.pxAnalyzer.Patterns.Selector;
import net.sourceforge.ck2httt.pxAnalyzer.Patterns.SelectorAnalyzer;



/**
 *  This class merges the {@link PXChooser} hooks and the {@link PXTree} hooks.<p>
 *  We don't have much conflicts as PXTree never filters anything out,
 *  so all filtering is left out to PXChooser.<p>
 *  <p>
 *  Apart from simplifying the user's life by hiding the binding glue between
 *  both Hooks, this class also provides two convenient Callback classes, more
 *  appropriate to the underlying PXTree than the generic one provided in
 *  PXChooser. Unlike this last, for which things remain to define (namely the
 *  {@link PXChooser.AnalyzerCallbacks.Merger} class), the TreeCallbacks class
 *  comes complete and ready to use.<p>
 *  <p>
 *  However, the courageous can always devise their own Callback class as there
 *  really is nothing depending on it.<p>
 *  <p>
 *  In addition to running callbacks during the analysis, the 
 *  {@link #applyTemplate(String, pxAnalyzer.PXTreeChooser.BasicCallback[])}
 *  method lets you explore the field tree and apply another kind of callbacks
 *  ({@link BasicCallback}) to each matching field, in the order
 *  where they are written ; this commodity lets you easily explore the field tree
 *  without having to do convoluted recursive loops.<p>
 * 
 * @see PXTree
 * @see PXChooser
 * @see Hooks
 * @see PXAdvancedAnalyzer
 * @author Copyright 2007 Yves Prélot ; distributed under the terms of the GNU General Public License
 */
public class PXTreeChooser {

	/**
	 * This class extends the AnalyzerCallbacks class in order to enable the
	 * PXChooser hook to <i>speak to</i> the underlying PXTree sub-hook. The
	 * dialog remains minimal, with the possibilities defined by TreeMatcherResult.
	 * If you require a more elaborate dialog, you still have the TreeHooks
	 * parameter at your disposal.<p>
	 * @see AnalyzerCallbacks
	 */
	abstract public class TreeCallbacks extends AnalyzerCallbacks {
		TreeHooks _h;  //the underlying TreeHook used.
		public TreeCallbacks() {
			_m = new TreeMerger();
			_h = (TreeHooks)super._h;
		}
		
		/**
		 * A more appropriate method for doing what we want.
		 * This is the only method we will have to override while
		 * declining this class into various children.<p>
		 * Note that the parameter f is fully built when the callback is invoked.<p>
		 * 
		 * @param f  current field being constructed (it is complete)
		 * @return   expected Analyzer behavior
		 */
		abstract public TreeMatcherResult doMatch(Field<?> f);			
		
		/**
		 * An implementation for the sub-class Merger, appropriate for
		 * the TreeHooks class. It closes the gap between PXChooser and PXTree.
		 */
		private class TreeMerger extends Merger<TreeMatcherResult> {
			public TreeMatcherResult doForMatch(String value) {
				return doMatch(_h._cur);
			}
			public boolean mergeMatch(TreeMatcherResult r, MatchPlace where) {
				//Set the results to be discarded if required
			    if (r==TreeMatcherResult.DISCARD || r==TreeMatcherResult.DISCARD_AND_SKIP)
				    _h._cur._name = null;
			    //prepare for skipping further tokens if required
				switch (r) {
				case NONE:             return false;
				case SKIP:             return true;
				case DISCARD:          return false;
				case DISCARD_AND_SKIP: return true;
				}
				return false;
			}		
		}
	}

	/**
	 * Defines the possible results for the TreeMatcher callbacks.
	 */
	static public enum TreeMatcherResult {
		/**continue processing normally*/                                          NONE,                
		/**skip to the end of the current structure*/                              SKIP,                
		/**discard the current item but continue analysis normally*/               DISCARD,             
		/**discard the current item and skip to the end of the current structure*/ DISCARD_AND_SKIP;    
	}
	
	/**
	 * The underlying TreeHook being filtered out.
	 */
	final public TreeHooks _h = new TreeHooks();

	/**
	 * Gets a filter Hook with callbacks.<p>
	 * @param filter filter string to use
	 * @param matcherList list of callbacks (it's smart but non mandatory to use TreeCallbacks)
	 * @return a Hook suitable for PXAnalyzer ; it is unwise to merge it again with
	 * other hooks unless you master the complex interactions.
	 * @throws ParseException when the filter is not correctly written
	 */
	final public Hooks get(String filter, AnalyzerCallbacks[] matcherList) throws ParseException {
		return new PXChooserHooks(filter,matcherList,_h);
	}
	/**
	 * Gets a basic filter Hook with no callbacks.<p>
	 * @param filter filter string to use
	 * @return a Hook suitable for PXAnalyzer ; it is unwise to merge it again with
	 * other hooks unless you master the complex interactions.
	 * @throws ParseException when the filter is not correctly written
	 */
	final public Hooks get(String filter) throws ParseException {
		return new PXChooserHooks(filter,_h);
	}
	/**
	 * Gets a filter Hook with callbacks.<p>
	 * @param filter filter Reader to use
	 * @param matcherList list of callbacks (it's smart but non mandatory to use TreeMatcher)
	 * @return a Hook suitable for PXAnalyzer ; it is unwise to merge it again with
	 * other hooks unless you master the complex interactions.
	 * @throws ParseException when the filter is not correctly written
	 */
	final public Hooks get(Reader filter, AnalyzerCallbacks[] matcherList) throws ParseException {
		return new PXChooserHooks(filter,matcherList,_h);
	}
	/**
	 * Gets a basic filter Hook with no callbacks.<p>
	 * @param filter filter Reader to use
	 * @return a Hook suitable for PXAnalyzer ; it is unwise to merge it again with
	 * other hooks unless you master the complex interactions.
	 * @throws ParseException when the filter is not correctly written
	 */
	final public Hooks get(Reader filter) throws ParseException {
		return new PXChooserHooks(filter,_h);
	}
	
	/**
	 * Gets the top field containing the whole data.
	 * @return The fake StructField containing all analyzed fields.
	 */
	final public StructField getDataTree() {
		return _h.getDataTree();
	}
	
	/**
	 * Gets the underlying TreeHook.
	 * @return The TreeHook used during the analysis
	 */
	final public TreeHooks getTreeHook() {
		return _h;
	}


	/**
	 * A class to define simple callbacks for exclusive use with applyTemplate.
	 * Defining a callbacks means creating a new derived class overloading the
	 * doForMatch call. The callback will be invoked for the appropriate field
	 * to which it is attached ; you will receive that field as parameter.
	 */
	public interface BasicCallback extends Callbacks {
		/**
		 * callback for the given field. This method is called at the
		 * place(s) specified in the calling template.
		 * @param f Field for which the callback is invoked.
		 */
		void doForMatch(Field<?> f);
	}


	/**
	 * Apply the selectors (callbacks) to the sub-fields if field matches the
	 * current selector.
	 * @param s  current selectors
	 * @param f  field being explored
	 * @throws ParseException
	 */
	static private void applySelector(Selector s, Field<?> f) throws ParseException {
		if (s._name.equals(f._name._value) || s._name.equals("*"))
		    applySelectors(s._children,f);
	}

	/**
	 * Apply the selectors (callbacks) to the subfields (if any)
	 * @param s  array of sub-selectors
	 * @param f  field being explored
	 * @throws ParseException
	 */
	static private void applySelectors(Selector s[], Field<?> f) throws ParseException {
		if (f instanceof StructField) {
			LinkedList<Field<?>> list = ((StructField)f)._data;
			for (Selector i : s) {
				for (Field<?> j : list) {
				    applySelector(i,j);
					if (i.isMatch(j._name._value) && i._matcher!=null)
						((BasicCallback)i._matcher).doForMatch(j);
				}
			}
		}
		else if (f instanceof AnonField) {
			LinkedList<BaseField> list = ((AnonField)f)._data;
			for (Selector i : s) {
				for (Field<?> j : list) {
		            applySelector(i,j);
					if (i.isMatch(j._name._value) && i._matcher!=null)
						((BasicCallback)i._matcher).doForMatch(j);
				}
		    }
		}
		else
			return;
	}

	/**
	 * Same as {@link #applyTemplate(String, BasicCallback[])}, except that the
	 * template is read from a reader.
	 * @param pattern the callback pattern, in a syntax similar to that for filters
	 * @param callbackList a list of callbacks, for the same use as for filters
	 * @throws ParseException if the pattern contains errors
	 */
	public void applyTemplate(Reader pattern, BasicCallback[] callbackList) throws ParseException {
		Selector s = SelectorAnalyzer.compile(pattern,callbackList);
		applySelector(s, _h.getDataTree()); 
	}

	/**
	 * This method defines a new way for applying callbacks to the Field tree.<br>
	 * This is done post-analysis ; it uses the same pattern as that defined for
	 * the standard callbacks, with the following <i>important</i> differences:<p>
	 * <ul>
	 *     <li>The callbacks are applied to the fields in the order defined in
	 *         the template, rather than in the order found in the original data.
	 *         Subtrees are explored as they are met, and the callbacks of the
	 *         subtree called <b>before</b> the callback of the owner node. If you
	 *         want to process the node before it's chidren, you'd have to repeat
	 *         it as in: top(0),top{sub(1)} which would process top with callback 0,
	 *         then top/sub with callback 1. top(0){top{sub(1)} would process in the
	 *         reverse order</li>
	 *     <li>The ! and / modifiers have no effect here, even tough you can
	 *         (uselessly) use them.</li>
	 *     <li>The * special field will process ALL fields if a callback is
	 *         attached. Be carefull. Still, it may be usefull to reach some
	 *         sub-fields as in *{subfield1(1)}, which would invoke the calback 1
	 *         on all subfields named subfield1.</li>
	 *     <li>The callbacks apply only to fields in the tree : if a field has not
	 *         been analyzed (because it was filtered out or skipped), the original
	 *         data is <b>not</b> parsed again</li>
	 *     <li>It makes no sense to pass fields with no callback unless they have
	 *         children</li>
	 *     <li>It makes sense to repeat fields in the pattern : the repeated field
	 *         will be processed again against the corresponding callback.</li>
	 * </ul>
	 * 
	 * <b>This is merely a convenient way to explore the Field tree, and should not
	 * be seen as anything else.</b> It is not even particularly efficient : it just
	 * lets you write things up faster. It doesn't do anything you could not do
	 * yourself : it only loops through the given fields in the written order,
	 * nothing more!<p>
	 * 
	 * Example:<p>
	 * {province(2){culture(1),id(4)},header(0)} would call callback number 1
	 * (abbreviated c1) on the field province1.culture, then c4 on province1.id, then
	 * c2 on province1, then loop on all provinces. Then it would call c0 on header.
	 * It doesn't matter that header was found in the file before provinces.<p>
	 * By comparision, the same data used as a filter would probably have called c0 on
	 * header first, because that field would have come first in the file.
	 * 
	 * @param pattern the callback pattern, in a syntax similar to that for filters
	 * @param callbackList a list of callbacks, for the same use as for filters
	 * @throws ParseException if the pattern contains errors
	 */
	public void applyTemplate(String pattern, BasicCallback[] callbackList) throws ParseException {
		Selector s = SelectorAnalyzer.compile(pattern,callbackList);
		applySelector(s, _h.getDataTree()); 
	}	
}

