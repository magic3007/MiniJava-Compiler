import minijava.parser.*;
import minijava.visitor.*;
import minijava.syntaxtree.*;
import java.util.*;

class Info {
	static void dump(String... msg) {
		for (String m : msg) {
			System.out.print(m);
			System.out.print(" ");
		}
		System.out.println();
	}

	static void panic(String... msg) {
		dump(msg);
		System.out.println("Type error");
		System.exit(0);
	}

	static void debug(String... msg) {
		dump(msg);
	}
}

// 1st pass: scan globally for class name
class ScanForClassName<T extends ClassCollection> extends GJVoidDepthFirst<T> {
	public void visit(ClassDeclaration node, T classes) {
		// the class name
		classes.add(node.f1.f0.tokenImage);
	}

	public void visit(ClassExtendsDeclaration node, T classes) {
		// the class name
		classes.add(node.f1.f0.tokenImage);
	}
}

// 2nd pass:
class ScanForSuperClassName<T extends ClassCollection> extends GJVoidDepthFirst<T> {
	public void visit(ClassExtendsDeclaration node, T classes) {
		// the class name
		String derivedClassName = node.f1.f0.tokenImage;
		String superClassName = node.f3.f0.tokenImage;
		ClassType derivedClass = classes.get(derivedClassName);
		ClassType superClass = classes.get(superClassName);
		Type t = superClass;
		derivedClass.superclass = superClass;
	}
}

// 3rd pass:

class Option {
	Object value;
	Option() {
		value = null;
	}
}

class GetTypeVisitor extends DepthFirstVisitor {
	GetTypeVisitor(ClassCollection classes, Option option) {
		this.classes = classes;
		this.option = option;
	}
	ClassCollection classes;
	Option option;

	public void visit(BooleanType node) {
		option.value = new BoolType();
	}

	public void visit(IntegerType node) {
		option.value = new IntType();
	}

	public void visit(ArrayType node) {
		option.value = new ArrayType();
	}

	public void visit(Identifier node) {
		String name = node.f0.tokenImage;
		option.value = classes.get(name);
	}
}

class ScanClassMethods extends DepthFirstVisitor {
	ScanClassMethods(ClassCollection classes) {
		this.classes = classes;
	}

	ClassCollection classes;
	ClassType selfClass;
	ClassType.Method selfMethod;

	public void visit(ClassDeclaration node) {
		selfClass = classes.get(node.f1.f0.tokenImage);
		super.visit(node);
	}

	public void visit(ClassExtendsDeclaration node) {
		selfClass = classes.get(node.f1.f0.tokenImage);
		super.visit(node);
	}

	public void visit(MethodDeclaration node) {
		String name = node.f2.f0.tokenImage;
		ClassType.Method method = selfClass.new Method();
		method.name = name;
		Option option = new Option();
		node.f1.accept(new GetTypeVisitor(classes, option));
		method.returnType = (Type) option.value;
		selfMethod = method;
		super.visit(node);
		selfClass.methods.add(method);
		selfMethod = null;
	}

	public void visit(FormalParameter node) {
		Option option = new Option();
		node.f0.accept(new GetTypeVisitor(classes, option));
		Type type = (Type) option.value;
		String name = node.f1.f0.tokenImage;
		selfMethod.param.types.add(type);
		selfMethod.param.names.add(name);
	}

	public void visit(VarDeclaration node) {
		Option option = new Option();
		node.f0.accept(new GetTypeVisitor(classes, option));
		Type type = (Type) option.value;
		String name = node.f1.f0.tokenImage;
		if (selfMethod != null) {
			selfMethod.temp.types.add(type);
			selfMethod.temp.names.add(name);
		} else {
			selfClass.field.types.add(type);
			selfClass.field.names.add(name);
		}
	}
}

// pass 4: Check type

abstract class AbstractGetExpressionType<T> extends GJNoArguDepthFirst<T> {}

class GetExpressionType extends AbstractGetExpressionType<Type> {
	GetExpressionType(ClassCollection classes) {
		this.classes = classes;
	}

	ClassCollection classes;

	public Type visit(Expression n) {
		return n.f0.accept(this);
	}

	public Type visit(CompareExpression n) {
		Type a = n.f0.accept(this);
		Type b = n.f2.accept(this);
		if (a instanceof IntType && b instanceof IntType) {
			return new BoolType();
		} else {
			Info.panic("CompareExpression");
			return null;
		}
	}

	public Type visit(AndExpression n) {
		Type a = n.f0.accept(this);
		Type b = n.f2.accept(this);
		if (a instanceof IntType && b instanceof IntType) {
			return new BoolType();
		} else {
			Info.panic("AndExpression");
			return null;
		}
	}

	public Type visit(PlusExpression n) {
		Type a = n.f0.accept(this);
		Type b = n.f2.accept(this);
		if (a instanceof IntType && b instanceof IntType) {
			return new IntType();
		} else {
			Info.panic("PlusExpression");
			return null;
		}
	}

	public Type visit(MinusExpression n) {
		Type a = n.f0.accept(this);
		Type b = n.f2.accept(this);
		if (a instanceof IntType && b instanceof IntType) {
			return new IntType();
		} else {
			Info.panic("MinusExpression");
			return null;
		}
	}

	public Type visit(TimesExpression n) {
		Type a = n.f0.accept(this);
		Type b = n.f2.accept(this);
		if (a instanceof IntType && b instanceof IntType) {
			return new IntType();
		} else {
			Info.panic("TimesExpression");
			return null;
		}
	} 

	public Type visit(PrimaryExpression n) {
		return n.f0.accept(this);
	} 

	public Type visit(ArrayLookup n) {
		Type a = n.f0.accept(this);
		Type b = n.f2.accept(this);
		if (a instanceof ArrayType && b instanceof IntType) {
			return new IntType();
		} else {
			Info.panic("ArrayLookup");
			return null;
		}
	} 

	public Type visit(ArrayLength n) {
		Type a = n.f0.accept(this);
		if (a instanceof ArrayType) {
			return new IntType();
		} else {
			Info.panic("ArrayLookup");
			return null;
		}
	}

	public Type visit(MessageSend n) {
		Type a = n.f0.accept(this);
		if (!(a instanceof ClassType)) {
			Info.panic("MessageSend");
			return null;
		}
		String methodname = n.f2.f0.tokenImage;
		// TODO: jqwo
		return null;
	} 
}

public class TypeCheck {
	public static void main(String []args) throws Exception {
		MiniJavaParser parser = new MiniJavaParser(System.in);
		ClassCollection classes = new ClassCollection();
		Node root = parser.Goal();

		root.accept(new ScanForClassName(), classes);
		root.accept(new ScanForSuperClassName(), classes);
		root.accept(new ScanClassMethods(classes));
		classes.dump();

		System.out.println("Program type checked successfully");
	}
}


