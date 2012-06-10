package com.redhat.ceylon.compiler.typechecker.model;


/**
 * Represents s simple attribute or local.
 *
 * @author Gavin King
 */
public class Value extends MethodOrValue {

    private boolean variable;
    //private boolean formal;
    private boolean captured = false;
    private Volatility volatility = Volatility.UNKNOWN;

    /*public boolean isFormal() {
         return formal;
     }

     public void setFormal(boolean formal) {
         this.formal = formal;
     }*/

    @Override
    public boolean isVariable() {
        return variable;
    }

    public void setVariable(boolean variable) {
        this.variable = variable;
    }

    @Override
    public boolean isCaptured() {
        return captured;
    }

    public void setCaptured(boolean local) {
        this.captured = local;
    }

    @Override
	public Volatility getVolatility() {
		return volatility;
	}

	public void setVolatility(Volatility volatility) {
		this.volatility = volatility;
	}

}
