Crusader Kings to Heir to the Throne Save Game Mod by Stephen May (idhrendur).


OBJECT
------

This utility has been created in order to let you create a Europa Universalis III starting position 
consistent with any Crusader Kings save game you may still have.

After converting a saved game, you will get a new CK_Converted.eu3 save game in your 
Europa Universalis III save game diretory. Start EUIII, select the CK_Converted save game and enjoy. 
Note that each new conversion will erase the previous one.

This save converts to EUIII with the Heir to the Throne expansion.

SPECIFICS
---------

This will convert:
* countries [owned provinces, sliders, accepted culture, capital, religion, government type]
* provinces [culture, religion, manpower, population size, buildings]
* political situation [alliances, wars]
* king
* advisors
* marshal as a general
* badboy
* stability
* relationship levels between countries (applying the standard EU3 logic to the relationships)
* treasury [within reason]

It will NOT convert:
* technology level [always the same in EU3 starting points]
* armies and locations [always 1/2 manpower in troops, 3/2 manpower in ships if you have a port]
* centers of trade
* curia control

CONVERTING A GAME
-----------------

Use the CKmod.bat.

If this fails for some reason, you can resort to the command line and directly launch
the java executable in the following way:

java -Xms500m -Xmx500m -jar CKmod.jar CKinstallDir EU3installDir savegame 

The -Xm parameters are required because the CK saves are usually quite large, and the java machine may 
require a lot of memory. In some special cases, you might even have to increase this.

The first parameter (CKinstallDir) is the full path to you installed Crusader Kings game.
The second parameter (EU3installDir) is the full path to you installed Europa Universalis III game.
The third parameter (savegame) is the name of the file to convert (with the .eug extension)

ADAPTING THE CONVERSION
-----------------------

Many of the choices for conversion are open. You may for example decide that all vassals belong to their 
owning kingdom, or that some of them may break free under a vassalisation contract... Or maybe all counties 
stick with their direct liege and all dukes become vassals... there are a lot of things you can tweek if 
you wish. Just have a look at the cvdata.txt file if you are interested in changing the way the converter 
work.

One of the things you may definitely have to tweak is the number of countries created by the converter: 
this converter never creates any new country : it makes it's best to use existing tags. This means that it 
can currently only support 156 countries. If the conversion fails because of a lack of european tags, you 
have two choices:
	* change the country_convert_rules entry ; the easiest way would then be to decrease the values 
	  under the tier subentry (currently county=-2, duchy=0) ; this will make vassals more likely to 
	  stick with their lord. 

REQUIREMENTS
------------

You will require an installed Java 5 or higher JRE or JDK to use the library or the programs.
You can find a free one at www.sun.com for exemple.

This has been tested with EU3 version 4.1b and will not work on lesser versions.

Note that the way this converter works will likely make it compatible with later EU3 releases.


CONTENT
-------

This should contain the following files:

CKmod.bat		A script to easily launch the conversion program
CKmod.jar		The java archive to execute
cvdata.txt		Data file for the converter
EU3CountryHeader	Data for writing the save file
EU3DistributionHeader	Data for writing the save file
EU3FinalHeader		Data for writing the save file
EU3GlobalHeader		Data for writing the save file
gpl-3.0.txt		The license file (Gnu Public License version 3)
Readme.txt		this file


INSTALLATION
------------

Just unzip everything in any directory you see fit.


CREDITS
-------

This was built from Richard Ulmont Campbell's (Ulmont) save game converter,
which was built from Yves Prélot's Mod converter, in which he says:
I admit to having stolen from Halsten his county to province conversion list
	(published in his thread) and adapted it to my own needs. Everything else I have done myself.


VERSIONS
--------

v 1.0.0    initial release
v 1.1.0    fixed for patch 4.1 beta