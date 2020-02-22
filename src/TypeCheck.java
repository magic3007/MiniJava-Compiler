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
		super.visit(node, classes);
	}

	public void visit(ClassExtendsDeclaration node, T classes) {
		// the class name
		classes.add(node.f1.f0.tokenImage);
		super.visit(node, classes);
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
		derivedClass.superclass = superClass;
	}
}


public class TypeCheck {
	public static void main(String []args) throws Exception {
		MiniJavaParser parser = new MiniJavaParser(System.in);
		ClassCollection classes = new ClassCollection();
		Node root = parser.Goal();

		root.accept(new ScanForClassName(), classes);
		root.accept(new ScanForSuperClassName(), classes);
		classes.dump();
	}
}


