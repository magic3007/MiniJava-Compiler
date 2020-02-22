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
		node.f4.accept(this); // FormalParameterList ?
		selfClass.methods.add(method);
	}

	public void visit(FormalParameter node) {
		Option option = new Option();
		node.f0.accept(new GetTypeVisitor(classes, option));
		Type type = (Type) option.value;
		String name = node.f1.f0.tokenImage;
		selfMethod.paramTypes.add(type);
		selfMethod.paramNames.add(name);
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
	}
}


