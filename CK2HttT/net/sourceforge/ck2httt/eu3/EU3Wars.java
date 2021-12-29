package net.sourceforge.ck2httt.eu3;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import net.sourceforge.ck2httt.rules.TagCvRules;
import net.sourceforge.ck2httt.utils.OptionSection;


public class EU3Wars {

	String		_name;
	String      _startDate;
	Set<String> _attackers;
	Set<String>	_defenders;
	SortedSet<Event>  _events;

	static LinkedList<EU3Wars> __list = new LinkedList<EU3Wars>();

	public EU3Wars(String name, String startDate, Set<String> attackers, Set<String> defenders, SortedSet<Event> events) {
		_name = name;
		_startDate = startDate;
		_attackers = attackers;
		_defenders = defenders;
		_events = events;
	}

	static public void addWar(String att, String def, Event[] battles) {
		String name = EU3LocalizedText.getName(att + "_ADJ") + "-" + EU3LocalizedText.getName(def + "_ADJ") + " War";
		String startDate = OptionSection.getStartYear() + ".1.1";
		Set<String> attackers = new HashSet<String>();
		attackers.add(att);
		Set<String> defenders = new HashSet<String>();
		defenders.add(def);
		Set<String> empty = new HashSet<String>();
		SortedSet<Event> events = new TreeSet<Event>();		
		short year = OptionSection.getStartYear();
		byte month = 1;
		byte day = 1;
		if (battles.length > 0) {
			year = battles[0]._year;
			month = battles[0]._month;
			day = battles[0]._day;
		}
		Event e = new Event(year, month, day, attackers, empty, defenders, empty);
		events.add(e);
		for (Event b: battles) {
			events.add(b);
		}
		
		EU3Wars w = new EU3Wars(name, startDate, attackers, defenders, events);
		__list.add(w);
	}
	
	static public void write (String out) throws IOException {
		PrintWriter x = new PrintWriter(new OutputStreamWriter(new FileOutputStream(out, true), "ISO-8859-1"));
		x.println("combat={");
		x.println("}");		
		for (EU3Wars w : __list) {
			w.write(x);
		}
		x.close();
	}
	
	public void write(PrintWriter x) throws IOException {
		x.println("active_war={");
		x.println("    name=\"" + _name + "\"");
		x.println("    history={");
		x.println("        name=\"" + _name + "\"");
		for (Event event: _events) {	
			event.write(x);
		}
		x.println("    }");		
		for (String attacker: _attackers) {
			x.println("    attacker=\"" + attacker + "\"");
		}
		for (String defender: _defenders) {
			x.println("    defender=\"" + defender + "\"");			
		}
		x.println("    action={");
		x.println("        year=" + OptionSection.getStartYear());
		x.println("        month=" + OptionSection.getStartMonth());
		x.println("        day=" + (OptionSection.getStartDay() - 1));
		x.println("    }");
	    x.println("}");	
	}	
	
	static public void loadDefaultWars(String path) throws IOException {
		// load all the wars from history/wars
		// ignore anything with a participant in the tag pool, or anything starting after start or ending before stary.
		File dir = new File(path);
		if (dir.exists()) {
			for (File f: dir.listFiles()) {		
				BufferedReader warBR = new BufferedReader(new FileReader(f));						
				String line = null;	
				
				String name = null;
				String startDate = null;
				Set<String> attackers = new HashSet<String>();
				Set<String> defenders = new HashSet<String>();
				Set<String> eventAddAttackers = new HashSet<String>();
				Set<String> eventRemAttackers = new HashSet<String>();
				Set<String> eventAddDefenders = new HashSet<String>();
				Set<String> eventRemDefenders = new HashSet<String>();
				SortedSet<Event> events = new TreeSet<Event>();
				short year = -1;
				byte month = -1;
				byte day = -1;				
				String batName = null;
				String batLocation = null; 
				int batAttInf = -1;
				int batAttCav = -1; 
				int batAttArt = -1;
				int batAttLossPercent = 0;
				int batDefInf = -1; 
				int batDefCav = -1; 
				int batDefArt = -1;
				int batDefLossPercent = 0;
				boolean batLoss = false;	
				
				boolean inBat = false;
				boolean inBatAtt = false;
				boolean inBatDef = false;
								
				// what we get in here is, in general, a series of events.  The only things global 
				// are the name, attacker, and defenders.

				/*
				1450.4.15 = {
						battle = {
							name = "Formigny"
							location = 168
							attacker = {
					#			leader =	# Thomas Kyriell
								infantry = 4000
								losses = 63	# percent
							}
							defender = {
					#			leader =	# Comte de Clermont
								infantry = 5000
								losses = 6	# percent
							}
							result = loss
						}
					}
				*/
				while ((line = warBR.readLine()) != null) {
					line = line.trim();
					int hash = line.indexOf("#");
					if (-1 != hash) {
						line = line.substring(0, hash);
					}
					int equal = line.indexOf("=");
					String postEqual = null;
					if (-1 != equal) {
						postEqual = line.substring(equal + 1).trim();
					}				
					int first = line.indexOf('.');
					int last = line.lastIndexOf('.');
					int space = line.indexOf(' ', last);
					if ((-1 != first) && (-1 != last) && (first != last) && (first < equal)) {
						// see if we can parse the date pieces.
						year = Short.parseShort(line.substring(0, first));;
						month = Byte.parseByte(line.substring(first + 1, last));
						day = Byte.parseByte(line.substring(last + 1, space));
					} else if (line.startsWith("name")) {
						String temp = postEqual.substring(1, postEqual.length() - 1); 
						if (inBat) {
							batName = temp;
						} else {
							name = temp;
						}							
					} else if (line.startsWith("add_attacker")) {						
						eventAddAttackers.add(postEqual);
					} else if (line.startsWith("add_defender")) {
						eventAddDefenders.add(postEqual);
					} else if (line.startsWith("rem_attacker")) {						
						eventRemAttackers.add(postEqual);
					} else if (line.startsWith("rem_defender")) {
						eventRemDefenders.add(postEqual);
					} else if (line.startsWith("battle")) {
						inBat = true; // now reading a battle.
					} else if (line.startsWith("location")) {
						batLocation = postEqual;
					} else if (line.startsWith("attacker")) {
						inBatAtt = true;
					} else if (line.startsWith("defender")) {
						inBatDef = true;
					} else if (line.startsWith("infantry")) {
						int inf = Integer.parseInt(postEqual);
						if (inBatAtt) {
							batAttInf = inf;
						} else if (inBatDef) {
							batDefInf = inf;
						}
					} else if (line.startsWith("cavalry")) {
						int cav = Integer.parseInt(postEqual);
						if (inBatAtt) {
							batAttCav = cav;
						} else if (inBatDef) {
							batDefCav = cav;
						}
					} else if (line.startsWith("artillery")) {
						int art = Integer.parseInt(postEqual);
						if (inBatAtt) {
							batAttArt = art;
						} else if (inBatDef) {
							batDefArt = art;
						}
					} else if (line.startsWith("losses")) {
						int lossPercent = Integer.parseInt(postEqual);
						if (inBatAtt) {
							batAttLossPercent = lossPercent;
						} else if (inBatDef) {
							batDefLossPercent = lossPercent;
						}
					} else if (line.startsWith("result")) {
						batLoss = "loss".equals(postEqual);
					} else if (line.equals("}")) {		
						if (inBatAtt || inBatDef) {
							inBatAtt = false;
							inBatDef = false;
						} else if (inBat) {
							inBat = false;
						} else {						
							// ok, at this point, transfer our event dudes into the
							// main list.
							if (((-1 != year) && (-1 != month) && (-1 != day)) &&
								((year < OptionSection.getStartYear()) || 										
							     ((year == OptionSection.getStartYear()) && (month < OptionSection.getStartMonth())))) {
								// means this event is appropriate to include  
								attackers.addAll(eventAddAttackers);
								attackers.removeAll(eventRemAttackers);
								defenders.addAll(eventAddDefenders);
								defenders.removeAll(eventRemDefenders);
								
								Event e = null;
								if (null == batName) {
									e = new Event(year, month, day, eventAddAttackers, eventRemAttackers, eventAddDefenders, eventRemDefenders);
								} else {
									e = new Event(year, month, day, 
													batName, 
													batLocation, 
													batAttInf,
													batAttCav, 
													batAttArt,
													batAttLossPercent,
													batDefInf, 
													batDefCav, 
													batDefArt,
													batDefLossPercent,
													batLoss);										
								}
								events.add(e);
								
								// first event is treated as war start
								if (null == startDate) {
									startDate = year + "." + month + "." + day;
								}
							}
							
							// reset event info
							year = -1;
							month = -1;
							day = -1;
							eventAddAttackers = new HashSet<String>();
							eventAddDefenders = new HashSet<String>();
							eventRemAttackers = new HashSet<String>();
							eventRemDefenders = new HashSet<String>();
							batName = null;
							batLocation = null; 
							batAttInf = -1;
							batAttCav = -1; 
							batAttArt = -1;
							batAttLossPercent = 0;
							batDefInf = -1; 
							batDefCav = -1; 
							batDefArt = -1;
							batDefLossPercent = 0;
							batLoss = false;
							
							inBatAtt = false;
							inBatDef = false;
							inBat = false;
						}
					}					
				}
				
				if ((null != name) && (null != startDate)) {
					boolean ignore = false;
					for (String att: attackers) {
						ignore = ignore || TagCvRules.isFromTagPool(att);
					}
					for (String def: defenders) {
						ignore = ignore || TagCvRules.isFromTagPool(def);
					}
					if (!ignore) {
						EU3Wars w = new EU3Wars(name, startDate, attackers, defenders, events);
						__list.add(w);
					}
				}

				/*
				// reset war global info
				name = null;
				startDate = null;
				attackers = new HashSet<String>();
				defenders = new HashSet<String>();
				events = new TreeSet<Event>();
				*/
			}
		}
	}	
	
	static public class Event implements Comparable<Event> {
		public short   _year;
		public byte    _month;
		public byte    _day;
		public Set<String> _addAttackers;
		public Set<String> _remAttackers;
		public Set<String> _addDefenders;
		public Set<String> _remDefenders;
		public String 	_batName;
		public String 	_batLocation;
		public int		_batAttInf;
		public int		_batAttCav;
		public int		_batAttArt;
		public int		_batAttLossPercent;
		public int		_batDefInf;
		public int		_batDefCav;
		public int		_batDefArt;
		public int		_batDefLossPercent;
		public boolean	_batLoss; // for the attacker				
		
		public Event(short year, byte month, byte day, Set<String> addAttackers, Set<String> remAttackers, Set<String> addDefenders, Set<String> remDefenders) {
			_year    = year;
			_month   = month;
			_day     = day;
			_addAttackers = addAttackers;
			_remAttackers = remAttackers;
			_addDefenders = addDefenders;
			_remDefenders = remDefenders;
		}

		public Event(short year, 
				byte month, 
				byte day, 
				String batName, 
				String batLocation, 
				int batAttInf, 
				int batAttCav, 
				int batAttArt,
				int batAttLossPercent,
				int batDefInf, 
				int batDefCav, 
				int batDefArt,
				int batDefLossPercent,
				boolean batLoss) {
			_year    = year;
			_month   = month;
			_day     = day;
			_batName = batName;
			_batLocation = batLocation; 
			_batAttInf = batAttInf;
			_batAttCav = batAttCav; 
			_batAttArt = batAttArt;
			_batAttLossPercent = batAttLossPercent;
			_batDefInf = batDefInf;
			_batDefCav = batDefCav; 
			_batDefArt = batDefArt;
			_batDefLossPercent = batDefLossPercent;
			_batLoss = batLoss;
		}
		
		public int compareTo(Event r) {
			if (_year != r._year) {
				return _year - r._year; 
			}
			if (_month != r._month) {
				return _month - r._month; 
			}
			if (_day != r._day) {
				return _day - r._day; 
			}
			
			// sort the add attacker / defenders to the front
			if ((null != _addAttackers) && (null == r._addAttackers)) {
				return -1;
			}
			if ((null != _addDefenders) && (null == r._addDefenders)) {
				return -1;
			}			
			
			return hashCode() - r.hashCode();
		}

		public void write(PrintWriter pw) {
			pw.println("        " + _year + "." + _month + "." + _day + "={");

			// decide if it's battle or attackers
			if ((null != _addAttackers) &&
				(null != _addAttackers) &&
				(null != _addAttackers) &&
				(null != _addAttackers)) {
				for (String addAttack: _addAttackers) {
					pw.println("            add_attacker=\"" + addAttack + "\"");
				}
				for (String remAttack: _remAttackers) {
					pw.println("            rem_attacker=\"" + remAttack + "\"");
				}
				for (String addDefend: _addDefenders) {
					pw.println("            add_defender=\"" + addDefend + "\"");
				}
				for (String remDefend: _remDefenders) {
					pw.println("            rem_defender=\"" + remDefend + "\"");
				}
			} else if ((null != _batName) && (null != _batLocation)) {
				pw.println("            battle={");
				pw.println("                name=\"" + _batName + "\"");
				pw.println("                location=" + _batLocation);
				pw.println("                result=" + (_batLoss ? "no" : "yes"));
				pw.println("                attacker={");
				if (_batAttInf > 0) {
					pw.println("                    infantry=" + _batAttInf);
				}
				if (_batAttCav > 0) {
					pw.println("                    cavalry=" + _batAttCav);
				}
				if (_batAttArt > 0) {
					pw.println("                    artillery=" + _batAttArt);
				}
				pw.println("                    losses=" + ((int) ((_batAttArt + _batAttCav + _batAttInf) * _batAttLossPercent / 100)));
				pw.println("                }");
				pw.println("                defender={");
				if (_batDefInf > 0) {
					pw.println("                    infantry=" + _batDefInf);
				}
				if (_batDefCav > 0) {
					pw.println("                    cavalry=" + _batDefCav);
				}
				if (_batDefArt > 0) {
					pw.println("                    artillery=" + _batDefArt);
				}
				pw.println("                    losses=" + ((int) ((_batDefArt + _batDefCav + _batDefInf) * _batDefLossPercent / 100)));
				pw.println("                }");
				pw.println("            }");
			}
	        pw.println("        }");	        
		}		
	}	
}
