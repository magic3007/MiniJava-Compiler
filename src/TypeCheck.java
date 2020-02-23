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
		throw new RuntimeException("DEBUG");
		// System.exit(0);
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

	public void visit(MainClass node, T classes) {
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
		selfMethod.param.add(type, name);
	}

	public void visit(VarDeclaration node) {
		Option option = new Option();
		node.f0.accept(new GetTypeVisitor(classes, option));
		Type type = (Type) option.value;
		String name = node.f1.f0.tokenImage;
		if (selfMethod != null) {
			selfMethod.temp.add(type, name);
		} else {
			selfClass.field.add(type, name);
		}
	}

	public void visit(MainClass node) {
		selfClass = classes.get(node.f1.f0.tokenImage);
		ClassType.Method method = selfClass.new StaticMethod();
		method.name = "main";
		method.returnType = new VoidType();
		method.param.add(new VoidType(), node.f11.f0.tokenImage);
		selfMethod = method;
		super.visit(node);
		selfClass.methods.add(method);
		selfMethod = null;
	}
}

// pass 4: Check type

abstract class AbstractGetExpressionType<T> extends GJNoArguDepthFirst<T> {}

class GetExpressionType extends AbstractGetExpressionType<Type> {
	GetExpressionType(ClassCollection classes) {
		this.classes = classes;
	}

	ClassCollection classes;
	ClassType selfClass;
	ClassType.Method selfMethod;

	public Type visit(ClassDeclaration node) {
		selfClass = classes.get(node.f1.f0.tokenImage);
		return node.f4.accept(this);
	}

	public Type visit(ClassExtendsDeclaration node) {
		selfClass = classes.get(node.f1.f0.tokenImage);
		return node.f6.accept(this);
	}

	public Type visit(MethodDeclaration node) {
		String name = node.f2.f0.tokenImage;
		ClassType.Method method = selfClass.getMethodByName(name);
		selfMethod = method;
		node.f8.accept(this);
		Type returnType = node.f10.accept(this);
		typeCastCheck(returnType, method.returnType);
		selfMethod = null;

		return null;
	}

	public Type visit(MainClass node) {
		selfClass = classes.get(node.f1.f0.tokenImage);
		selfMethod = selfClass.getMethodByName("main");
		node.f15.accept(this);
		return null;
	}

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

	class GetExpressionListType extends AbstractGetExpressionType<List<Type>> {
		public List<Type> visit(NodeOptional n) {
			if (n.present())
				return n.node.accept(this);
			else
				return new ArrayList<Type>();
		}

		public List<Type> visit(NodeListOptional n) {
			List<Type> rv = new ArrayList<Type>();

			if (! n.present()) {
				return rv;
			}

			for (Enumeration<Node> e = n.elements(); e.hasMoreElements(); ) { 
				Expression ee = (Expression) e.nextElement();
				rv.add(ee.accept(GetExpressionType.this));
			}
			return rv;
		}

		public List<Type> visit(ExpressionList n) {
			Expression e = (Expression) n.f0;
			List<Type> rv = n.f1.accept(this);
			// prepend |e|
			rv.add(0, e.accept(GetExpressionType.this));
			return rv;
		}
	}

	void typeCastCheck(Type from, Type to) {
		if (from instanceof PrimitiveType) {
			if (! from.getClass().equals(to.getClass())) {
				Info.panic("incompatible primitive type");
			}
		} else if (to instanceof PrimitiveType) {
			Info.panic("cast ClassType to PrimitiveType");
		} else {
			ClassType a = (ClassType) from;
			ClassType b = (ClassType) to;
			while (a != null) {
				if (a.name.equals(b.name)) {
					return;
				}
				a = a.superclass;
			}
			Info.panic("cast to derived class failed");
		}
	}

	public Type visit(MessageSend n) {
		Type a = n.f0.accept(this);
		if (!(a instanceof ClassType)) {
			Info.panic("MessageSend");
			return null;
		}
		ClassType ct = (ClassType) a;
		String methodname = n.f2.f0.tokenImage;
		ClassType.Method method = ct.getMethodByName(methodname);

		List<Type> args = n.f4.accept(new GetExpressionListType());
		int length = args.size();
		if (length != method.param.size()) {
			Info.panic("unequal number of arguments");
		}

		for (int i = 0; i < length; i++) {
			typeCastCheck(args.get(i), method.param.get(i).type);
		}

		return method.returnType;
	} 

	public Type visit(IntegerLiteral n) {
		return new IntType();
	}

	public Type visit(TrueLiteral n) {
		return new BoolType();
	}

	public Type visit(FalseLiteral n) {
		return new BoolType();
	}

	public Type visit(Identifier n) {
		String name = n.f0.tokenImage;
		Type type = selfMethod.getTypeByName(name);
		return type;
	}

	public Type visit(ThisExpression n) {
		return selfClass;
	}

	public Type visit(ArrayAllocationExpression n) {
		Type a = n.f3.accept(this);
		if (!(a instanceof IntType)) {
			Info.panic("ArrayAllocationExpression");
		}
		return new ArrayType();
	}

	public Type visit(AllocationExpression n) {
		String name = n.f1.f0.tokenImage;
		ClassType type = classes.get(name);
		return type;
	}

	public Type visit(NotExpression n) {
		Type a = n.f1.accept(this);
		if (!(a instanceof BoolType)) {
			Info.panic("ArrayAllocationExpression");
		}
		return new BoolType();
	}

	public Type visit(BracketExpression n) {
		return n.f1.accept(this);
	}

	public Type visit(IfStatement n) {
		Type a = n.f2.accept(this);
		if (!(a instanceof BoolType)) {
			Info.panic("IfStatement condition is not a BoolType");
		}
		return null;
	}

	public Type visit(WhileStatement n) {
		Type a = n.f2.accept(this);
		if (!(a instanceof BoolType)) {
			Info.panic("IfStatement condition is not a BoolType");
		}
		return null;
	}

	public Type visit(AssignmentStatement n) {
		Type a = n.f0.accept(this);
		Type b = n.f2.accept(this);
		typeCastCheck(b, a);
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
		root.accept(new GetExpressionType(classes));

		System.out.println("Program type checked successfully");
	}
}


