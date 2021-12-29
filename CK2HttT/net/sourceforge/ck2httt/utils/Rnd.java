package net.sourceforge.ck2httt.utils;
import java.util.Random;

public class Rnd {

	static Random __rnd = new Random(123454321);
	static public Random get() { return __rnd; }

}
