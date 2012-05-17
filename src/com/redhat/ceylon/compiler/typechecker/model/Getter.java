package com.redhat.ceylon.compiler.typechecker.model;


/**
 * An attribute getter.
 *
 * @author Gavin King
 */
public class Getter extends MethodOrValue implements Scope {

	private Setter setter;
	private Volatility volatility = Volatility.UNKNOWN;

    public Setter getSetter() {
        return setter;
    }

    public void setSetter(Setter setter) {
        this.setter = setter;
    }

    @Override
    public boolean isVariable() {
        return setter!=null;
    }
    
    @Override
	public Volatility getVolatility() {
		return volatility;
	}

	public void setVolatility(Volatility volatility) {
		this.volatility = volatility;
	}
}
