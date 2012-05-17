package com.redhat.ceylon.compiler.typechecker.model;

/**
 * Represents the volatility classes distinguished.
 * @author Simon Thum
 */
public enum Volatility {
	/**
	 * Volatility could not be determined. This volatility may not be declared.
	 */
	UNKNOWN,
	/**
	 * The item is transparent regarding volatility; this volatility may not be declared. 
	 */
	IGNORE,
	/**
	 * varying, evaluation may cause side-effects
	 */
	VARYING,
	/**
	 * local (stack or thread) scope
	 */
	LOCAL,
	/**
	 * instance-bound immutable scope
	 */
	IMMUTABLE,
	/**
	 * constant, can be determined at comile time
	 */
	CONSTANT;
	
	/**
	 * @param b a volatility
	 * @return true iff b fulfils this volatility
	 */
	public boolean isFulfilledBy(Volatility b) {
		switch(this) {
		case CONSTANT: return b == CONSTANT;
		case IMMUTABLE: return b == CONSTANT || b == IMMUTABLE;
		case LOCAL: return b == LOCAL || b == CONSTANT || b == IMMUTABLE;
		case VARYING: return b == VARYING || b == LOCAL || b == CONSTANT || b == IMMUTABLE;
		default:
			return false;
		}
	}
	
	/**
	 * @param b a volatility
	 * @return true iff the b fulfils this volatility
	 */
	public boolean fulfils(Volatility b) {
		switch(this) {
		case VARYING:
		case LOCAL:
		case IMMUTABLE:
		case CONSTANT:
			return b.isFulfilledBy(this);
		default:
			return false;
		}
	}
	
	/**
	 * the supposed source code name for errors etc.
	 * @return
	 */
	public String sourceCodeName() {
		return this.toString().toLowerCase();
	}
	
}
