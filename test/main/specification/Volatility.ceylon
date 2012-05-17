class VolatilityChecks(){

	interface VolPerson {
		shared formal variable String mood;  // varying
		shared formal String name; // immutable
	}
	
	class MoodyPerson(String arg) satisfies VolPerson  {
	    shared actual variable String mood := "flaky";
	    
	    shared actual String name {
	    	return "Hello " arg "";
	    }
	}
	
	class LessMoodyPerson (name, String moodArg) satisfies VolPerson  {
		// ok, but less volatile
	    shared actual default immutable String mood {
			return moodArg;
	    }
	    assign mood { }
	    shared actual default String name;
	}
	
	class VeryMoodyPerson (name, String moodArg) extends LessMoodyPerson (name, moodArg) {
		variable String _m := moodArg;
		// should get a warning for constraining volatility (while violating volatility)
		// and an error for violating, but none if no error occurs
		// but @error also covers warnings
	    shared actual immutable String mood { @error return _m; }
	    assign mood { }
	    shared actual immutable String name;
	}
	 
	void m() {
	    VolPerson pete = LessMoodyPerson("Pete", "haha");
	    if (pete.mood == "haha") {
	    	print(pete.mood);
	    }
	}
}