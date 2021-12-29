package net.sourceforge.ck2httt.pxAnalyzer;

import java.io.Reader;
import java.text.ParseException;

import net.sourceforge.ck2httt.pxAnalyzer.Analyzer.Hooks;
import net.sourceforge.ck2httt.pxAnalyzer.Patterns.Callbacks;
import net.sourceforge.ck2httt.pxAnalyzer.Patterns.Selector;
import net.sourceforge.ck2httt.pxAnalyzer.Patterns.SelectorAnalyzer;


/**
 * This class defines Hooks whose purpose is to select which fields are going to be
 * analyzed and exclude the others ; it also provides a way to define callbacks to
 * execute after a field has been analyzed. It can also pass the found and accepted
 * fields to a sub-hook, who will then only get to see a portion of the original field
 * tree<p>
 * 
 * It is important to understand the difference between the callbacks presented here
 * and the Hooks interface (which are callbacks too):
 * <ul>
 * <li>The Hooks are called at specific places of the analysis : they are attached to
 * the grammatical structure of the data</li>
 * <li>The AnalyzerCallbacks callbacks are attached to individual fields, hence are called
 * depending on the content and not only on the structure of the file.</li>
 * </ul>
 * 
 * For this purpose, a pattern (defined in {@link Patterns}) must be created. Here
 * the pattern is used as a filter on the selected field hierarchy. You can attach a callback to
 * each field as described in Patterns. However, they now have to derive from the
 * {@link AnalyzerCallbacks} class.<br>
 * These callbacks will be called after the corresponding
 * field has been seen and is almost completely analyzed (i.e. at the time where the analyzer
 * will switch to the next field ; more precisely, it is called during the afterXXX
 * appropriate Hook, just <i>before</i> the corresponding sub-hook (if any) afterXXX call.
 * This means that the callback may influence the sub-hook behavior through the
 * {@link AnalyzerCallbacks.Merger} class, either in the doForMatch method (which is
 * discouraged), or in the mergeMatch method (normal way).<p>
 * 
 * You can influence the flow of analysis by providing a sub-hook and/or call-backs (which
 * may or may not dialog with the sub-hook).<p>
 * 
 * The sub-hook is pretty obvious : It works normally, except that it will only get
 * to see the fields that passed through the filter, as though the other fields never existed.<p>
 * The callback solution is somewhat more involved to put in place (at least initially), but
 * can reap large rewards. The {@link BasicAnalyzerCallbacks} class is a basic implementation
 * of {@link AnalyzerCallbacks} when you don't use any sub-hooks or when you don't require any
 * communication. The derived class {@link PXTreeChooser.TreeCallbacks} is another interesting
 * implementation  provided you are in the PXTreeChosser framework.<p>
 * 
 * Note that a filter equal to {*} may have an attached callback and will let pass all data.<p>
 * 
 * Continuing the example in Patterns, we could then suppose that you defined two callbacks m1
 * (intended for /header/startgame) and m2 (intended for /globaldata/startgame), for example
 * by deriving two instances of BasicAnalyzerCallbacks. You might want to use another callback in
 * globaldata (say m3) ; because callbacks are called in the order the items are found (and
 * finished analyzed) in the file, you know that at that time you have run the two other
 * callbacks (because in the file header comes before globaldata, and the fields inside globaldata
 * will be processed before globaldata is processed). You'd put them in an array {m1, m2, m3}
 * (the matcherList), and would call the {link #get(filter,matcherList) } function with the
 * following filter string:
 * <p>
 * <b>{header{!startgame(0)},globaldata(2){!startgame(1)}}</b><br>
 * <br>
 * This would result in a Hook doing precisely what you defined.<p>
 * 
 * 
 * @see Analyzer
 * @see PXTreeChooser
 * @see Patterns
 * 
 * @author Copyright 2007 Yves Prélot ; distributed under the terms of the GNU General Public License
 */
public class PXChooser {
	


	/**
	 * This class is used to define callbacks used during the data analysis.<br>
	 * See the package description.<br>
	 */
	static public class AnalyzerCallbacks implements Callbacks {
		protected Merger<?>      _m;
		protected PXChooserHooks _chooser;
		
		/** underlying hook if any*/
		public    Hooks          _h;
		
		/**
		 * Called when trying to load a callback by path or name rather than selecting it by
		 * it's index. This method will be called in two situations :<p>
		 * <ul>
		 * <li>Anonymous : The path will show as ;  /name1/name2/name3 etc ; jokers will
		 *                 show as jokers, and not the actual field.</li>
		 * <li>Named     : The path will be whatever string the user passed (not a number)</li>
		 * </ul>
		 * If there is a match, the doForMatch method will be called at any time
		 * the current node matches.<p>
		 * 
		 * @param  path the matching name or path
		 * @return true if this instance matches
		 */
		public boolean isMatch(String path) { return false; }
		/**
		 * Gets the last name field that was seen by the analyzer.
		 * @return the name of the last field analyzed
		 */
		public String getName() { return _chooser._lastName._value; }
		/**
		 * Invokes the callback.
		 * @param v value of the current data token if any
		 * @param where Position in the analysis where Matcher was invoked
		 * @return true if we want to skip to the end of the current structure
		 */
		private boolean invoke(String v, MatchPlace where) { return _m.doMerge(v,where); }
	
		/**
		 * Defines the places where a callback is called.
		 */
		public enum MatchPlace {
			/** callback was called after data was found */                   AFTERDATA,
			/** callback was called after a structure was found */            AFTERSTRUCT,
			/** callback was called after an anonymous structure was found */ AFTERANON,
			/** callback was called after a list was found */                 AFTERLIST,
			/** callback was called after a empty was found */                AFTEREMPTY;
			};
		
		/**
		 * This class bridges the gap between the underlying Hooks and the Chooser Hooks.
		 * If you own Hooks and want to use it with PXChooserHooks, you may have to
		 * provide your own implementation. If you don't need any dialoig between the
		 * PXChooserHooks and your own Hooks, then the default implementation in
		 * BasicAnalyzerCallbacks may be sufficient.
		 * 
		 * @param <T>
		 */
		abstract public static class Merger<T> {
			/**
			 * Action to take when that element is matched.<br>
			 * Usually, you would put here dynamic testing to know whether you want to
			 * keep the current data structure being analyzed. <br>
			 * This also allows the PXChooserHooks to "speak" to the underlying hook.<br>
			 * 
			 * @param  v    the current data read if AFTERDAT ; otherwise null..
			 * @return anything that will be used by the corresponding mergeMatch code.
			 */
			public abstract T doForMatch(String v);
			/**
			 * This method is closely associated with the method doForMatch.<br>
			 * While the second is oriented towards the application (what you do),
			 * the first is devoted to keeping the link with the underlying Hooks used
			 * (the analyzer).<br>
			 * In this way, you can devise a Hooks and determine how it speaks to the
			 * PXChooser it is called from independently from the way it is used.<br>
			 * 
			 * @return true if you want to skip the analyzer to the end of the current
			 * structure (performance gain mostly).
			 */
			public boolean mergeMatch(T result, MatchPlace where) { return false; };
			/**
			 * This does the actual merge and will only be used in this file
			 */
		    private boolean doMerge(String v, MatchPlace where) {
			    return mergeMatch(doForMatch(v),where);
		    }
		}
	}

	/**
	 * A basic implementation for Matchers, for which you only have to overload the 
	 * doMatch and isMatch calls. It doesn't cooperate with any sub-hook.
	 */
	abstract static public class BasicAnalyzerCallbacks extends AnalyzerCallbacks {
		/**
		 * Defines the action that the analyzer should take when the current callback returns.
		 */
		public enum Action {
			/** continue analysis */                         CONTINUE,
			/**skip analysis to end of current structure */  SKIP      
			};
		public BasicAnalyzerCallbacks() { _m=new BaseMerger(); }
		/**
		 * Action to take when that element is matched.
		 * @return command to the analyzer.
		 */
		abstract public Action doMatch(String value);
		
		private class BaseMerger extends Merger<Action> {
			public Action doForMatch(String value) { return doMatch(value); };
			public boolean mergeMatch(Action result, MatchPlace where) { return result==Action.SKIP; };
		}
	}
	
	/**
	 * Class implementing Hooks whose purpose is to select which fields
	 * are going to be analyzed. See The enclosing class description for more information.
	 * The various constructors take one to three parameters:<p>
	 * <ul>
	 *     <li><b>filter</b> : either a String or a Reader : it contains the filter description as defined
	 *         in the package description ; this filter may contain callback definitions.</li>
	 *     <li><b>callbackList</b> : the array of callbacks used by the filter.</li>
	 *     <li><b>hook</b> : the sub-hook called by this hook.</li>
	 * </ul>
	 * @see Analyzer.Hooks
	 */
	static final class PXChooserHooks implements Hooks {
		protected Selector[] _stack    = new Selector[40];
		protected int        _l        = 0;
		protected Hooks      _h        = null;
		protected Token      _lastName = null;
		
		public PXChooserHooks(String filter) throws ParseException                                               { _stack[0] = SelectorAnalyzer.compile(filter,null); }
		public PXChooserHooks(String filter, AnalyzerCallbacks[] callbackList) throws ParseException             { _stack[0] = SelectorAnalyzer.compile(filter,callbackList); initAnalyzerCallbacks(callbackList); }
		public PXChooserHooks(String filter, Hooks hook) throws ParseException                                   { this(filter); _h = hook; }
		public PXChooserHooks(String filter, AnalyzerCallbacks[] callbackList, Hooks hook) throws ParseException { this(filter,callbackList); _h = hook; }
		public PXChooserHooks(Reader filter) throws ParseException                                               { _stack[0] = SelectorAnalyzer.compile(filter,null); }
		public PXChooserHooks(Reader filter, AnalyzerCallbacks[] callbackList) throws ParseException             { _stack[0] = SelectorAnalyzer.compile(filter,callbackList); initAnalyzerCallbacks(callbackList); }
		public PXChooserHooks(Reader filter, Hooks hook) throws ParseException                                   { this(filter); _h = hook; }
		public PXChooserHooks(Reader filter, AnalyzerCallbacks[] callbackList, Hooks hook) throws ParseException { this(filter,callbackList); _h = hook; }
		
		public boolean beforeStruct(Token t) {
			Selector ok = _stack[_l].getMatch(_lastName._value);
			_l++;
			if (ok!=null) {
				ok.reset();
				_stack[_l]=ok;
				return (_h==null) ? false : _h.beforeStruct(t);
			}
			_stack[_l]=null;
			return true;
		};
		public boolean afterStruct(Token t) {
			Selector s = _stack[_l--];
			if (s==null) return false;
			boolean b1=false, b2=false;
			if (s._matcher!=null)
				b1 = ((AnalyzerCallbacks)s._matcher).invoke(null, AnalyzerCallbacks.MatchPlace.AFTERSTRUCT);
			if (_h!=null) b2 = _h.afterStruct(t);
			return b1 | b2;
		};
		public boolean beforeAnon(Token t) {
			Selector ok = _stack[_l].getMatch("");
			_l++;
			if (ok!=null) {
				ok.reset();
				_stack[_l]=ok;
				return (_h==null) ? false : _h.beforeAnon(t);
			}
			_stack[_l]=null;
			return true;
			
		}
		public boolean afterAnon(Token t) {
			Selector s = _stack[_l--];
			if (s==null) return false;
			boolean b1=false, b2=false;
			if (s._matcher!=null)
				b1 = ((AnalyzerCallbacks)s._matcher).invoke(null, AnalyzerCallbacks.MatchPlace.AFTERANON);
			if (_h!=null) b2 = _h.afterAnon(t);
			return b1 | b2;			
		}
		public boolean beforeList(Token t) {
			Selector ok = _stack[_l].getMatch(_lastName._value);
			_l++;
			if (ok!=null) {
				ok.reset();
				_stack[_l]=ok;
				return (_h==null) ? false : _h.beforeList(t);
			}
			_stack[_l]=null;
			return true;
		};
		public boolean afterList(Token t) {
			Selector s = _stack[_l--];
			if (s==null) return false;
			boolean b1=false, b2=false;
			if (s._matcher!=null)
			    b1 = ((AnalyzerCallbacks)s._matcher).invoke(null, AnalyzerCallbacks.MatchPlace.AFTERLIST);
			if (_h!=null) b2 = _h.afterList(t);
			return b1 | b2;
		};
		public void beforeEmpty(Token t)  {
			Selector ok = _stack[_l].getMatch(_lastName._value);
			_l++;
			if (ok!=null) {
				ok.reset();
				_stack[_l]=ok;
				if (_h!=null) _h.beforeEmpty(t);
				return;
			}
			_stack[_l]=null;
		}
		public boolean afterEmpty(Token t)        {
			Selector s = _stack[_l--];
			if (s==null) return false;
			boolean b1=false, b2=false;
			if (s._matcher!=null)
			    b1 = ((AnalyzerCallbacks)s._matcher).invoke(null, AnalyzerCallbacks.MatchPlace.AFTEREMPTY);
			if (_h!=null) b2 = _h.afterEmpty(t);
			return b1 | b2;
		};
		public boolean afterListData(Token t)  {
			if (_h!=null) return _h.afterListData(t);
			return false;
		}
		public void beforeBase(Token t)  {
			Selector ok = _stack[_l].getMatch(_lastName._value);
			_l++;
			if (ok!=null) {
				_stack[_l]=ok;
				if (_h!=null) _h.beforeBase(t);
				return;
			}
			_stack[_l]=null;
		}
		public boolean afterBase(Token t)  {
			Selector s = _stack[_l--];
			if (s==null) return false;
			boolean b1=false, b2=false;
			if (s._matcher!=null)
		        b1 = ((AnalyzerCallbacks)s._matcher).invoke(t._value, AnalyzerCallbacks.MatchPlace.AFTERDATA);
			if (_h!=null) b2=_h.afterBase(t);
			return b1 | b2 | s==Selector.__SKIP;
		};
		public void    begin()             { if (_h!=null) _h.begin(); };
		public void    end(Token t)        { if (_h!=null) _h.end(t); };
		public void    getName(Token t)    { _lastName=t; if (_h!=null) _h.getName(t); };
		public String  getLastName()       { return _lastName._value; }
		
		/**
		 * sets the hooks for the callbacks : these won't change during the analysis
		 */
		private void initAnalyzerCallbacks(AnalyzerCallbacks[] c) {
			if (c!=null) {
				for (AnalyzerCallbacks s : c) {
						s._chooser=this;
						s._h=_h;
				}
			}
		}
	};
	
	/**
	 * Gets a filter Hook with full features
	 * @param filter filter string to use (see syntax in {@link Patterns})
	 * @param callbackList list of callbacks (see class description)
	 * @param hook sub-hook attached to that filter
	 * @return a Hook suitable for PXAnalyzer
	 * @throws ParseException when the filter is not correctly written
	 */
	static public Hooks get(String filter, AnalyzerCallbacks[] callbackList, Hooks hook) throws ParseException { return new PXChooserHooks(filter,callbackList,hook); }
	/**
	 * Gets a filter Hook with no callbacks
	 * @param filter filter string to use
	 * @param hook sub-hook attached to that filter
	 * @return a Hook suitable for PXAnalyzer
	 * @throws ParseException when the filter is not correctly written
	 */
	static public Hooks get(String filter, Hooks hook) throws ParseException                        { return new PXChooserHooks(filter,hook); }
	/**
	 * Gets a filter Hook with no sub-hook
	 * @param filter filter string to use
	 * @param callbackList list of callbacks
	 * @return a Hook suitable for PXAnalyzer
	 * @throws ParseException when the filter is not correctly written
	 */
	static public Hooks get(String filter, AnalyzerCallbacks[] callbackList) throws ParseException             { return new PXChooserHooks(filter,callbackList); }
	/**
	 * Gets a basic filter Hook with no feature
	 * @param filter filter string to use
	 * @return a Hook suitable for PXAnalyzer
	 * @throws ParseException when the filter is not correctly written
	 */
	static public Hooks get(String filter) throws ParseException                                    { return new PXChooserHooks(filter); }
	/**
	 * Gets a filter Hook with full features
	 * @param filter filter Reader to use
	 * @param callbackList list of callbacks
	 * @param hook sub-hook attached to that filter
	 * @return a Hook suitable for PXAnalyzer
	 * @throws ParseException when the filter is not correctly written
	 */
	static public Hooks get(Reader filter, AnalyzerCallbacks[] callbackList, Hooks hook) throws ParseException { return new PXChooserHooks(filter,callbackList,hook); }
	/**
	 * Gets a filter Hook with no callbacks
	 * @param filter filter Reader to use
	 * @param hook sub-hook attached to that filter
	 * @return a Hook suitable for PXAnalyzer
	 * @throws ParseException when the filter is not correctly written
	 */
	static public Hooks get(Reader filter, Hooks hook) throws ParseException                        { return new PXChooserHooks(filter,hook); }
	/**
	 * Gets a filter Hook with no sub-hook
	 * @param filter filter Reader to use
	 * @param callbackList list of callbacks
	 * @return a Hook suitable for PXAnalyzer
	 * @throws ParseException when the filter is not correctly written
	 */
	static public Hooks get(Reader filter, AnalyzerCallbacks[] callbackList) throws ParseException             { return new PXChooserHooks(filter,callbackList); }
	/**
	 * Gets a basic filter Hook with no feature
	 * @param filter filter Reader to use
	 * @return a Hook suitable for PXAnalyzer
	 * @throws ParseException when the filter is not correctly written
	 */
	static public Hooks get(Reader filter) throws ParseException                                    { return new PXChooserHooks(filter); }
	
}
