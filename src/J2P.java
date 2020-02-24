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

	public void visit(AssignmentStatement n) {
		String name = n.f0.f0.tokenImage;
		selfMethod.emitAssignByName(name);
		n.f2.accept(this);
	}

	/**
	 a[b] = c

	 HSTORE PLUS a TIMES 4 PLUS 1 b 0 c
	 */
	public void visit(ArrayAssignmentStatement n) {
		Info.emitOpen("HSTORE", "PLUS", "// array assign");
		String name = n.f0.f0.tokenImage;
		selfMethod.emitByName(name);
		Info.emitBuf("TIMES", Info.numToOffset(1), "PLUS", "1");
		n.f2.accept(this);
		Info.emitBuf("0");
		n.f5.accept(this);
		Info.emitClose();
	}

	/**
	 a[b]

	 BEGIN
		HLOAD TEMP 1 
		PLUS a 
		TIMES 4 PLUS 1 b 0
	 RETURN TEMP 1 END
	 */
	public void visit(ArrayLookup n) {
		Info.emitOpen("/*array*/", "BEGIN");
		String temp1 = Info.newTemp();
		Info.emit("HLOAD", temp1);
		Info.emitBuf("PLUS");
		n.f0.accept(this);
		Info.emitFlush();
		Info.emitBuf("TIMES", Info.numToOffset(1), "PLUS", "1");
		n.f2.accept(this);
		Info.emitBuf("0");
		Info.emitClose("RETURN", temp1, "END");
	}

	/**
	  a.length

	  BEGIN
	  	HLOAD TEMP 1 a
	  RETURN TEMP 1 END
	 */
	public void visit(ArrayLength n) {
		Info.emitOpen("/*length*/", "BEGIN");
		String temp1 = Info.newTemp();
		Info.emitBuf("HLOAD", temp1);
		n.f0.accept(this);
		Info.emitClose("RETURN", temp1, "END");
	}

	/**
	  if (a) b else c

	  CJUMP a L1
	  	b
	  JUMP L2
	  	L1 c
	  L2 NOOP
	 */
	public void visit(IfStatement n) {
		Info.emitBuf("/*if*/", "CJUMP");
		n.f2.accept(this);
		String L1 = Info.newLabel();
		String L2 = Info.newLabel();
		Info.emitBuf(L1);
		Info.emitOpen();
		n.f4.accept(this);
		Info.emitClose("/*else*/", "JUMP", L2);
		Info.emitOpen();
		Info.emitBuf(L1);
		n.f6.accept(this);
		Info.emitClose("/*endif*/", L2, "NOOP");
	}

	/** 
	  while (a) b

	  L1 NOOP
	  CJUMP a L2
	  	b
	  JUMP L1
	  L2 NOOP
	 */
    public void visit(WhileStatement n) {
    	String L1 = Info.newLabel();
		String L2 = Info.newLabel();
    	Info.emit("/*while*/", L1, "NOOP"); 
    	Info.emitBuf("CJUMP");
    	n.f2.accept(this);
    	Info.emit(L2);
    	Info.emitOpen();
    	n.f4.accept(this);
    	Info.emitClose("JUMP", L1);
    	Info.emit("/*endwhile*/", L2, "NOOP");
    }

    public void visit(PrintStatement n) {
    	Info.emitBuf("PRINT");
    	n.f2.accept(this);
    }

    /**
     a && b

  	 BEGIN
  	 	MOVE TEMP 1 a
  	 	CJUMP TEMP 1 L1
  	 	MOVE TEMP 1 b 
		L1 NOOP
  	 RETURN TEMP 1 END
     */
    public void visit(AndExpression n) {
    	Info.emitOpen("/*and*/", "BEGIN");
    	String temp1 = Info.newTemp();
    	Info.emitBuf("MOVE", temp1);
    	n.f0.accept(this);
    	Info.emitFlush();
    	String L1 = Info.newLabel();
    	Info.emit("CJUMP", temp1, L1);
    	Info.emitBuf("MOVE", temp1);
    	n.f2.accept(this);
    	Info.emitFlush();
    	Info.emit(L1, "NOOP");
    	Info.emitClose("RETURN", temp1, "END");
    }

    public void visit(CompareExpression n) {
    	Info.emitBuf("LT");
    	n.f0.accept(this);
    	n.f2.accept(this);
    }

    public void visit(PlusExpression n) {
    	Info.emitBuf("PLUS");
    	n.f0.accept(this);
    	n.f2.accept(this);
    }

    public void visit(MinusExpression n) {
    	Info.emitBuf("MINUS");
    	n.f0.accept(this);
    	n.f2.accept(this);
    }

    public void visit(TimesExpression n) {
    	Info.emitBuf("TIMES");
    	n.f0.accept(this);
    	n.f2.accept(this);
    }

    /**
    a.b(...)

    CALL BEGIN
    	HLOAD TEMP 1 a 0
    	HLOAD TEMP 2 TEMP 1 $(offset of b)
    RETURN TEMP 2 END
    */
    public void visit(MessageSend n) {
    	Info.emitOpen("CALL", "BEGIN");
    	String temp1 = Info.newTemp();
    	String temp2 = Info.newTemp();
    	Info.emitBuf("HLOAD", temp1);
    	n.f0.accept(this);

    	String methdName = n.f2.f0.tokenImage;

    	indexOfMethod
    	Info.emitClose("RETURN", temp2, "END");
    }

    public void visit(Identifier n) {
    	if (selfMethod == null) {
    		return;
    	}
    	String name = n.f0.tokenImage;
    	selfMethod.emitByName(name);
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



