package com.redhat.ceylon.compiler.typechecker.analyzer;

import com.redhat.ceylon.compiler.typechecker.model.Annotation;
import com.redhat.ceylon.compiler.typechecker.model.Declaration;
import com.redhat.ceylon.compiler.typechecker.model.Getter;
import com.redhat.ceylon.compiler.typechecker.model.TypedDeclaration;
import com.redhat.ceylon.compiler.typechecker.model.Value;
import com.redhat.ceylon.compiler.typechecker.model.ValueParameter;
import com.redhat.ceylon.compiler.typechecker.model.Volatility;
import com.redhat.ceylon.compiler.typechecker.tree.Node;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.AnyAttribute;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.AttributeGetterDefinition;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.BaseMemberExpression;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.Expression;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.Literal;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.MemberOrTypeExpression;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.Outer;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.QualifiedMemberExpression;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.SelfExpression;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.Statement;
import com.redhat.ceylon.compiler.typechecker.tree.Visitor;

/**
 * Contains the checks for volatility contraints. Avoids noisy errors.
 * @author Simon Thum
 */
public class VolatilityVisitor extends Visitor {

	@SuppressWarnings("serial")
	protected static class ExitException extends RuntimeException {
		private final Node node;

		public ExitException(String msg, Node node) {
			super(msg);
			this.node = node;
		}
		
		public ExitException() {
			this.node = null;
		}

		public Node getNode() {
			return node;
		}
		
		/**
		 * default handler for exit exception in visitors.
		 * @param e
		 */
		public static void handle(Exception e) {
			// bubble up ExitException
			if (e instanceof ExitException)
				throw (ExitException)e;
			// avoid re-packing ISE
			else if (e instanceof IllegalStateException)
				throw (IllegalStateException)e;
			else
				throw new IllegalStateException(e);
		}
	}
	
	static boolean containsErrors(Node node) {
		return Util.hasError(node, true);
	}
	
	
 // copied from ceylondoc TODO this is not typesafe
    private static Annotation getAnnotation(Declaration decl, String name) {
        for (Annotation a : decl.getAnnotations()) {
            if (a.getName().equals(name))
                return a;
        }
        return null;
    }
    
	private static Volatility getDeclaredVolatility(TypedDeclaration decl) {
		// TODO the mutiple annotation case is intended to
		// be catched by using contrained annotations
		if (getAnnotation(decl, "constant") != null)
			return Volatility.CONSTANT;
		if (getAnnotation(decl, "immutable") != null)
			return Volatility.IMMUTABLE;
		if (getAnnotation(decl, "local") != null)
			return Volatility.LOCAL;
		if (getAnnotation(decl, "varying") != null)
			return Volatility.VARYING;
		return Volatility.UNKNOWN;
	}
	
	/**
	 * get the volatility applicable to the declaration without
	 * considering refinement.
	 * @param decl
	 */
	private static Volatility getNaiveVolatility(TypedDeclaration decl) {
		Volatility declaredVolatility = getDeclaredVolatility(decl);
		if (declaredVolatility != Volatility.UNKNOWN)
			return declaredVolatility;
		// default behaviour: decide using variability
		return decl.isVariable() ? Volatility.VARYING : Volatility.IMMUTABLE;
	}
	
	private static boolean checkRefinement(TypedDeclaration decl) {
		return getNaiveVolatility(decl).fulfils(
					getNaiveVolatility(
						(TypedDeclaration) decl.getRefinedDeclaration()));
	}
	
	private static Volatility getEffectiveVolatility(TypedDeclaration decl) {
		// check refinement is equal or stricter, this
		// forces to redeclare any non-default volatility
		if (!checkRefinement(decl))
			return Volatility.UNKNOWN;
		return getNaiveVolatility(decl);
	}

	/**
	 * Visits all declarations and checks if their refinement uphelds
	 * applicable volatility constraints, and assigns inheritance-checked volatility
	 * to model objects. (Which may later prove wrong when checking the
	 * actual getter code!)
	 * @author Simon Thum
	 */
	public static class VolatilityRefinementVisitor extends Visitor {

		@Override
		public void visit(AnyAttribute that) {
			super.visit(that);
			handleVolatility(that.getDeclarationModel(), that);
		}
		
		@Override
		public void visit(QualifiedMemberExpression that) {
			super.visit(that);
			Declaration declaration = that.getDeclaration();
			if (declaration instanceof TypedDeclaration)
				handleVolatility((TypedDeclaration)declaration, that);
		}
		
		@Override
		public void visit(BaseMemberExpression that) {
			super.visit(that);
			Declaration declaration = that.getDeclaration();
			if (declaration instanceof TypedDeclaration)
				handleVolatility((TypedDeclaration)declaration, that);
		}
		
		private void handleVolatility(TypedDeclaration model, Node that) {
			if (containsErrors(that)) {
				return;
			}
			// init volatility
			if (model.getVolatility() == Volatility.UNKNOWN) {
				Volatility effectiveVolatility = getEffectiveVolatility(model);
				if (effectiveVolatility == Volatility.UNKNOWN) {
					that.addError("volatility of attribute " + model.getName() + " does not correctly re-declare inherited constraints from " + model.getRefinedDeclaration().getContainer().getQualifiedNameString() + ", re-declare here or widen the refined attribute");
					return;
				}
				else if (model instanceof Getter) {
					// compliance checked in next run
					((Getter)model).setVolatility(effectiveVolatility);
				} else if (model instanceof Value) {
					// values have known semantics, check them earlier than getters
					// a variable value may not be constant or immutable
					if (model.isVariable() &&
						   (effectiveVolatility == Volatility.CONSTANT ||
							effectiveVolatility == Volatility.IMMUTABLE)) {
						that.addError("variable simple attributes cannot be constant or immutable");
					}
					// TODO local should also be constrained (semantics unclear so far)
					((Value)model).setVolatility(effectiveVolatility);
				}
			}
			switch (model.getVolatility()) {
			case CONSTANT:
				if (model.isFormal() || model.isDefault() || model.isVariable())
					that.addError("constant attributes must not be variable or refineable");
			default:
				break;
			}
		}
	}
	
	/**
	 * Visits all Getter and checks if they do not evaluate anything more
	 * volatile. (Not ideal, but a 95% soltion)
	 * @author Simon Thum
	 */
	public static class VolatilityGetterCheckVisitor extends Visitor {

		@Override
		public void visit(AttributeGetterDefinition that) {
			super.visit(that);
			handleVolatility(that);
		}

		// check if refinement is less volatile than its un-refined attribute
		// (that is, on or more refinements are contraining volatiliy)
		// That's fine, but may be unintended and can be used to improve error
		// mgs with hints.
		private static boolean isConstrainingRefinement(TypedDeclaration decl) {
			TypedDeclaration unRefined = getRefinedDeclaration(decl);
			
			Volatility unRefinedVolatility = unRefined.getVolatility();
			Volatility declVolatility = decl.getVolatility();
			return declVolatility.fulfils(unRefinedVolatility) && 
				  declVolatility != unRefinedVolatility;
		}

		private static TypedDeclaration getRefinedDeclaration(TypedDeclaration decl) {
			TypedDeclaration unRefined;
			for (unRefined = decl; unRefined != unRefined.getRefinedDeclaration(); unRefined = (TypedDeclaration)unRefined.getRefinedDeclaration());
			return unRefined;
		}

		private void handleVolatility(AttributeGetterDefinition getterDef) {
			if (containsErrors(getterDef)) {
				return;
			}
			// init volatility
			final TypedDeclaration model = getterDef.getDeclarationModel();
			if (model.getVolatility() == Volatility.UNKNOWN) {
				// unexpected as we bail out on known errors above
				getterDef.addUnexpectedError("expected getter to have known volatility: " + getterDef.getDeclarationModel().getName());
				return;
			}
			try {
				getterDef.visitChildren(new Visitor() {
					@Override
					public void visit(Expression that) {
						that.visit(new VolatilityExpressionVisitor(model.getVolatility()));
					}
					
					@Override
					public void handleException(Exception e, Node that) {
						ExitException.handle(e);
					}
				});
			} catch (ExitException e) {
				if (e.node != null) {
					if (isConstrainingRefinement(model)) {
						getterDef.addWarning("Note: The getter is declared less volatile than " + 
								getRefinedDeclaration(model).getQualifiedNameString());
					}
					e.node.addError(e.getMessage());
				}
			}
		}
	}
	
	/**
	 * Visits a block and checks if the code in there respects applicable
	 * volatility constraints.
	 * Possible strategy: identify constraints and check all influencing
	 * expressions in reach.
	 */
	public static class VolatilityBlockVisitor extends Visitor {
		
	}
	
	/**
	 * Determines the volatility of an expression, if possible. Will likely throw
	 * {@link ExitException} when done, only Exceptions with a node are volatility
	 * violations.
	 * TODO evaluating local variables is not covered.
	 * @author Simon Thum
	 */
	public static class VolatilityExpressionVisitor extends Visitor {
		/**
		 * The volatility as currently assumed.
		 */
		Volatility v = Volatility.UNKNOWN;
		
		final Volatility toValidate;
		
		/*
		 * Creates a volatilityVisitor which determines the volatility of an expression.
		 * As soon as varying is certain evaluation will end.
		 */
		public VolatilityExpressionVisitor() {
			super();
			this.toValidate = Volatility.IGNORE;
		}
		
		/*
		 * Creates a volatilityVisitor which checks if a certain volatility is fulfilled
		 * in the constructs it visits. If not, an early exist happens.
		 */
		public VolatilityExpressionVisitor(Volatility toValidate) {
			super();
			this.toValidate = toValidate;
		}
		
		/**
		 * @return the volatility as discovered so far
		 */		
		public Volatility getVolatility() {
			return v;
		}

		@Override
		public void handleException(Exception e, Node that) {
			ExitException.handle(e);
		}
	
		/**
		 * Asserts the volatility of a visited node
		 * @param o
		 */
		private void assertVolatility(Volatility o, Node node) {
			if (o == Volatility.IGNORE)
				return;
			if (o == Volatility.UNKNOWN)
				throw new ExitException("Volatility unknown", node);
			
			// unknown can't fulfil anything
			if (v == Volatility.UNKNOWN) {
				v = o;
			}
			else if (!v.isFulfilledBy(o)) {
				// adjust result volatility
				v = o;
			}
			
			// check for error condition if required
			if (toValidate != Volatility.IGNORE && !v.fulfils(toValidate)) {
				throw new ExitException("Element does not fulfil " + toValidate.sourceCodeName() + " volatility.", node);
			}
			// early-exit conditions
			if (v == Volatility.VARYING || toValidate == Volatility.VARYING)
				throw new ExitException();
		}
	
		@Override
		public void visit(Literal that) {
			assertVolatility(Volatility.CONSTANT, that);
			super.visit(that);
		}
	
		@Override
		public void visit(Outer that) {
			assertVolatility(Volatility.IMMUTABLE, that);
			super.visit(that);
		}
	
		@Override
		public void visit(SelfExpression that) {
			assertVolatility(Volatility.IMMUTABLE, that);
			super.visit(that);
		}
	
		@Override
		public void visit(MemberOrTypeExpression that) {
			// we also get here e.g. on class declarations
			if (!(that.getDeclaration() instanceof TypedDeclaration))
				return;
			TypedDeclaration typedDeclaration = (TypedDeclaration)that.getDeclaration();
			// TODO parameters are always immutable? Probably not.
			if (typedDeclaration instanceof ValueParameter) {
				assertVolatility(Volatility.IMMUTABLE, that);
			} else {
				assertVolatility(typedDeclaration.getVolatility(), that);
			}
			super.visit(that);
		}
	
		@Override
		public void visit(Statement that) {
			// some defensive programming - probably incomplete
			throw new IllegalArgumentException("This visitor only visits single expressions.");
		}	
	}
}