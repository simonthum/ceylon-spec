class VolatilityRefinement() {
	abstract class VtFormal() {
	    shared immutable formal String s;
	}
	
	abstract class VtFormalError() {
	    // might be allowable for metaprogramming - "a type has to declare this constant"
		@error shared const formal String cnst;
	}
	
	class VtActual() extends VtFormal() {
	    shared actual String s = "hello";
	}
	
	class VtDefault() {
	    shared default String s = "hi";
	}
	
	class VtActual2() extends VtDefault() {
	    shared actual String s = "hello";
	}
	
	class VtActual3() extends VtDefault() {
	    @error shared actual varying String s = "hello";
	}
	
	class VtActual4() extends VtDefault() {
	    @error shared actual variable String s := "hello";
	    shared constant String cnst { return "compile-time constant " 5 " template"; } 
	}
	
	interface VInterface {
		shared formal String iii;
		
		shared formal local Integer loc;
	}
	
	class VImpl () satisfies VInterface {
		shared actual String iii = "";
		shared actual local Integer loc { return 5; }
	}
	
	class VImplBad () satisfies VInterface {
		@error shared actual immutable variable String iii := "";
		shared actual constant Integer loc { return 5; }
	}
	
	class VImplBadGetters (arg) satisfies VInterface {
		variable String arg;
		shared actual String iii { @error return arg; }
		shared actual Integer loc { return 5; }
	}
}