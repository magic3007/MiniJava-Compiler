import java.util.*;
import minijava.parser.*;
import minijava.visitor.*;
import minijava.syntaxtree.*;


class PigletGenVisitor extends DepthFirstVisitor {
	PigletGenVisitor(ClassCollection classes) {
		this.classes = classes;
	}

	ClassCollection classes;
	ClassType selfClass;
	ClassType.Method selfMethod;

	public void visit(ClassDeclaration node) {
		selfClass = classes.get(node.f1.f0.tokenImage);
		node.f4.accept(this);
	}

	public void visit(ClassExtendsDeclaration node) {
		selfClass = classes.get(node.f1.f0.tokenImage);
		node.f6.accept(this);
	}

	public void visit(MethodDeclaration node) {
		String name = node.f2.f0.tokenImage;
		ClassType.Method method = selfClass.getMethodByName(name);
		selfMethod = method;
		Info.emitOpen(method.getLabel(), "[", 
			Integer.toString(method.numOfParams()), "]");
		node.f8.accept(this);
		node.f10.accept(this);
		selfMethod = null;
		Info.emitClose("// end of", method.getLabel());
	}

	public void visit(MainClass node) {
		Info.emitOpen("MAIN");
		selfClass = classes.get(node.f1.f0.tokenImage);
		selfMethod = selfClass.getMethodByName("main");
		node.f15.accept(this);
		Info.emitClose("END");
	}
}

public class J2P {

	static void J2P(TypeCheckResult r) {
		r.classes.analyze();
		r.classes.dump();
		r.root.accept(new PigletGenVisitor(r.classes));
	}

	public static void main(String[] args) {
		TypeCheckResult root = null;
		try {
			root = TypeCheck.TypeCheck();
		} catch (Exception e) {
			System.out.println("TypeCheck failed");
			System.exit(233);
		}

		J2P(root);
	}
}



