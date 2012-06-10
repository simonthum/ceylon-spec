package com.redhat.ceylon.compiler.typechecker.analyzer;

import static com.redhat.ceylon.compiler.typechecker.model.Util.isTypeUnknown;
import static com.redhat.ceylon.compiler.typechecker.tree.Util.name;

import java.util.ArrayList;
import java.util.List;

import com.redhat.ceylon.compiler.typechecker.model.Declaration;
import com.redhat.ceylon.compiler.typechecker.model.ProducedType;
import com.redhat.ceylon.compiler.typechecker.model.Scope;
import com.redhat.ceylon.compiler.typechecker.model.TypeDeclaration;
import com.redhat.ceylon.compiler.typechecker.model.TypedDeclaration;
import com.redhat.ceylon.compiler.typechecker.tree.Message;
import com.redhat.ceylon.compiler.typechecker.tree.Node;
import com.redhat.ceylon.compiler.typechecker.tree.Tree;
import com.redhat.ceylon.compiler.typechecker.tree.UnexpectedError;
import com.redhat.ceylon.compiler.typechecker.tree.Visitor;

/**
 * Bucket for some helper methods used by various
 * visitors.
 * 
 * @author Gavin King
 *
 */
class Util {
    
    static TypedDeclaration getBaseDeclaration(Tree.BaseMemberExpression bme, 
            List<ProducedType> signature) {
        Declaration result = bme.getScope().getMemberOrParameter(bme.getUnit(), 
                name(bme.getIdentifier()), signature);
        if (result instanceof TypedDeclaration) {
        	return (TypedDeclaration) result;
        }
        else {
        	return null;
        }
    }
    
    static TypeDeclaration getBaseDeclaration(Tree.BaseType bt) {
        Declaration result = bt.getScope().getMemberOrParameter(bt.getUnit(), 
                name(bt.getIdentifier()), null);
        if (result instanceof TypeDeclaration) {
        	return (TypeDeclaration) result;
        }
        else {
        	return null;
        }
    }
    
    static TypeDeclaration getBaseDeclaration(Tree.BaseTypeExpression bte, 
            List<ProducedType> signature) {
        Declaration result = bte.getScope().getMemberOrParameter(bte.getUnit(), 
                name(bte.getIdentifier()), signature);
        if (result instanceof TypeDeclaration) {
        	return (TypeDeclaration) result;
        }
        else {
        	return null;
        }
    }
    
    static void checkTypeBelongsToContainingScope(ProducedType type,
            Scope scope, Node that) {
        //TODO: this does not account for types 
        //      inherited by a containing scope!
        //TODO: what if the type arguments don't match?!
        while (scope!=null) {
            if (type.getDeclaration().getContainer()==scope) {
                return;
            }
            scope=scope.getContainer();
        }
        that.addError("illegal use of qualified type outside scope of qualifying type: " + 
                type.getProducedTypeName());
    }

    static List<ProducedType> getTypeArguments(Tree.TypeArguments tal) {
        List<ProducedType> typeArguments = new ArrayList<ProducedType>();
        if (tal instanceof Tree.TypeArgumentList) {
            for (Tree.Type ta: ( (Tree.TypeArgumentList) tal ).getTypes()) {
                ProducedType t = ta.getTypeModel();
                if (t==null) {
                    ta.addError("could not resolve type argument");
                    typeArguments.add(null);
                }
                else {
                    typeArguments.add(t);
                }
            }
        }
        return typeArguments;
    }
    
    /*static List<ProducedType> getParameterTypes(Tree.ParameterTypes pts) {
        if (pts==null) return null;
        List<ProducedType> typeArguments = new ArrayList<ProducedType>();
        for (Tree.SimpleType st: pts.getSimpleTypes()) {
            ProducedType t = st.getTypeModel();
            if (t==null) {
                st.addError("could not resolve parameter type");
                typeArguments.add(null);
            }
            else {
                typeArguments.add(t);
            }
        }
        return typeArguments;
    }*/
    
    static Tree.Statement getLastExecutableStatement(Tree.ClassBody that) {
        List<Tree.Statement> statements = that.getStatements();
        for (int i=statements.size()-1; i>=0; i--) {
            Tree.Statement s = statements.get(i);
            if (s instanceof Tree.ExecutableStatement) {
                return s;
            }
            else {
                if (s instanceof Tree.AttributeDeclaration) {
                    if ( ((Tree.AttributeDeclaration) s).getSpecifierOrInitializerExpression()!=null ) {
                        return s;
                    }
                }
                if (s instanceof Tree.MethodDeclaration) {
                    if ( ((Tree.MethodDeclaration) s).getSpecifierExpression()!=null ) {
                        return s;
                    }
                }
                if (s instanceof Tree.ObjectDefinition) {
                    Tree.ObjectDefinition o = (Tree.ObjectDefinition) s;
                    if (o.getExtendedType()!=null) {
                        ProducedType et = o.getExtendedType().getType().getTypeModel();
                        if (et!=null 
                                && !et.getDeclaration().equals(that.getUnit().getObjectDeclaration())
                                && !et.getDeclaration().equals(that.getUnit().getIdentifiableObjectDeclaration())) {
                            return s;
                        }
                    }
                    if (o.getClassBody()!=null) {
                        if (getLastExecutableStatement(o.getClassBody())!=null) {
                            return s;
                        }
                    }
                }
            }
        }
        return null;
    }
    
    private static String message(ProducedType type, String problem, ProducedType otherType) {
        String typeName = type.getProducedTypeName();
        String otherTypeName = otherType.getProducedTypeName();
        if (otherTypeName.equals(typeName)) {
            typeName = type.getProducedTypeQualifiedName();
            otherTypeName = otherType.getProducedTypeQualifiedName();
        }
        return ": " + typeName + problem + otherTypeName;
    }
    
    private static String message(ProducedType type, String problem) {
        String typeName = type.getProducedTypeName();
        return ": " + typeName + problem;
    }
    
    static boolean checkCallable(ProducedType type, Node node, String message) {
        if (isTypeUnknown(type)) {
            addTypeUnknownError(node, message);
            return false;
        }
        else if (!type.isCallable()) {
            if (!hasError(node)) {
                node.addError(message + message(type, " is not a subtype of Callable"));
            }
            return false;
        }
        else {
            return true;
        }
    }

    static ProducedType checkSupertype(ProducedType pt, TypeDeclaration td, 
            Node node, String message) {
        if (isTypeUnknown(pt)) {
            addTypeUnknownError(node, message);
            return null;
        }
        else {
            ProducedType supertype = pt.getSupertype(td);
            if (supertype==null) {
                node.addError(message + message(pt, " is not a subtype of " + td.getName()));
            }
            return supertype;
        }
    }

    static void checkAssignable(ProducedType type, ProducedType supertype, 
            Node node, String message) {
        if (isTypeUnknown(type) || isTypeUnknown(supertype)) {
        	addTypeUnknownError(node, message);
        }
        else if (!type.isSubtypeOf(supertype)) {
        	node.addError(message + message(type, " is not assignable to ", supertype));
        }
    }

    static void checkAssignable(ProducedType type, ProducedType supertype, 
            Node node, String message, int code) {
        if (isTypeUnknown(type) || isTypeUnknown(supertype)) {
            addTypeUnknownError(node, message);
        }
        else if (!type.isSubtypeOf(supertype)) {
            node.addError(message + message(type, " is not assignable to ", supertype), code);
        }
    }

    static void checkAssignable(ProducedType type, ProducedType supertype, 
            TypeDeclaration td, Node node, String message) {
        if (isTypeUnknown(type) || isTypeUnknown(supertype)) {
            addTypeUnknownError(node, message);
        }
        else if (!type.isSubtypeOf(supertype, td)) {
            node.addError(message + message(type, " is not assignable to ", supertype));
        }
    }

    static void checkIsExactly(ProducedType type, ProducedType supertype, 
            Node node, String message) {
        if (isTypeUnknown(type) || isTypeUnknown(supertype)) {
            addTypeUnknownError(node, message);
        }
        else if (!type.isExactly(supertype)) {
            node.addError(message + message(type, " is not exactly ", supertype));
        }
    }
    
    private static boolean containsErrors(List<Message> errors, boolean allowWarnings) {
		if (allowWarnings) {
			for (Message m : errors) {
				if (m instanceof AnalysisError || m instanceof UnexpectedError) {
					return true;
				}
			}
			return false;
		} else {
			return !errors.isEmpty();
		}
	}
    
    public static boolean hasError(Node node, final boolean allowWarning) {
        class ErrorVisitor extends Visitor {
            boolean found = false;
            @Override
            public void visitAny(Node that) {
                if (!containsErrors(that.getErrors(), allowWarning)) {
                    super.visitAny(that);
                }
                else {
                    found = true;
                }
            }
        }
        ErrorVisitor ev = new ErrorVisitor();
        node.visit(ev);
        return ev.found;
    }
    
    public static boolean hasError(Node node) {
    	return hasError(node, false);
    }

    private static void addTypeUnknownError(Node node, String message) {
        if (!hasError(node)) {
            node.addError(message + ": type cannot be determined");
        }
    }

}
