import minijava.parser.*;
import minijava.visitor.*;
import minijava.syntaxtree.*;
import java.util.*;

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
		option.value = new ArrType();
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
//  and Piglet generation

abstract class AbstractGetExpressionType<T> extends GJNoArguDepthFirst<T> {}

class GetExpressionType extends AbstractGetExpressionType<Type> {
	GetExpressionType(ClassCollection classes, Emitter e) {
		this.classes = classes;
		this.e = e;
	}

	Emitter e;
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
		e.emitOpen(method.getLabel(), "[", 
			Integer.toString(method.numOfParams() + 1), "]", "BEGIN");
		node.f8.accept(this);
		e.emitClose();
		e.emitOpen("RETURN");
		Type returnType = node.f10.accept(this);
		typeCastCheck(returnType, method.returnType);
		selfMethod = null;
		e.emitClose("/* end", method.getLabel(), "*/", "END");
		return null;
	}

	public Type visit(MainClass node) {
		e.emitOpen("MAIN");
		selfClass = classes.get(node.f1.f0.tokenImage);
		selfMethod = selfClass.getMethodByName("main");
		node.f15.accept(this);
		e.emitClose("END");
		return null;
	}

	public Type visit(Expression n) {
		return n.f0.accept(this);
	}

	public Type visit(CompareExpression n) {
		e.emitBuf("LT");
		Type a = n.f0.accept(this);
		Type b = n.f2.accept(this);
		if (a instanceof IntType && b instanceof IntType) {
			return new BoolType();
		} else {
			Info.panic("CompareExpression");
			return null;
		}
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
	public Type visit(AndExpression n) {
		e.emitOpen("/*and*/", "BEGIN");
		String temp1 = e.newTemp();
		String L1 = e.newLabel();
		e.emitBuf("MOVE", temp1);
		Type a = n.f0.accept(this);
		e.emit("CJUMP", temp1, L1);
		e.emitBuf("MOVE", temp1);
		Type b = n.f2.accept(this);
		if (!(a instanceof BoolType 
			&& b instanceof BoolType)) {
			Info.panic("AndExpression");
			return null;
		}
		e.emit(L1, "NOOP");
		e.emitClose("RETURN", temp1, "END");
		return new BoolType();
	}

	public Type visit(PlusExpression n) {
		e.emitBuf("PLUS");
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
		e.emitBuf("MINUS");
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
		e.emitBuf("TIMES");
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
		if (a instanceof ArrType && b instanceof IntType) {
			return new IntType();
		} else {
			Info.panic("ArrayLookup");
			return null;
		}
	} 

	public Type visit(ArrayLength n) {
		Type a = n.f0.accept(this);
		if (a instanceof ArrType) {
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
				ExpressionRest ee = (ExpressionRest) e.nextElement();
				rv.add(ee.f1.accept(GetExpressionType.this));
				GetExpressionType.this.e.emitFlush();
			}
			return rv;
		}

		public List<Type> visit(ExpressionList n) {
			Expression e = (Expression) n.f0;
			Type t = e.accept(GetExpressionType.this);
			GetExpressionType.this.e.emitFlush();
			List<Type> rv = n.f1.accept(this);
			// prepend |e|
			rv.add(0, t);
			return rv;
		}
	}

	void typeCastCheck(Type from, Type to) {
		if (from instanceof PrimitiveType) {
			if (! from.getClass().equals(to.getClass())) {
				Info.panic("incompatible primitive type " + from + " -> " + to);
			}
		} else if (to instanceof PrimitiveType) {
			Info.panic("cast ClassType to PrimitiveType " + from + " -> " + to);
		} else {
			ClassType a = (ClassType) from;
			ClassType b = (ClassType) to;
			while (a != null) {
				if (a.name.equals(b.name)) {
					return;
				}
				a = a.superclass;
			}
			Info.panic("cast to derived class failed " + from + " -> " + to);
		}
	}

	/**
	a.b(...)

	CALL BEGIN
		MOVE TEMP 1 a
		HLOAD TEMP 2 TEMP 1 0
		HLOAD TEMP 2 TEMP 2 $(offset of b)
	RETURN TEMP 2 END ( 
		TEMP 1
		arg1
		arg2
	)	
	*/
	public Type visit(MessageSend n) {
		e.emitOpen("CALL", "BEGIN");
		String temp1 = e.newTemp();
		String temp2 = e.newTemp();
		e.emitBuf("MOVE", temp1);

		Type a = n.f0.accept(this);
		if (!(a instanceof ClassType)) {
			Info.panic("MessageSend");
			return null;
		}
		ClassType ct = (ClassType) a;
		String methodname = n.f2.f0.tokenImage;
		ClassType.Method method = ct.getMethodByName(methodname);

		e.emitFlush();
		e.emit("HLOAD", temp2, temp1, "0");
		e.emitBuf("HLOAD", temp2, temp2, 
			e.numToOffset(ct.indexOfMethod(methodname)));
		e.emitClose("RETURN", temp2, "END", "(");
		e.emitOpen();
		e.emit(temp1);

		List<Type> args = n.f4.accept(new GetExpressionListType());
		int length = args.size();
		if (length != method.param.size()) {
			Info.panic("unequal number of arguments " 
				+ length + " -> " + method.param.size());
		}

		for (int i = 0; i < length; i++) {
			typeCastCheck(args.get(i), method.param.get(i).type);
		}

		e.emitClose(")");
		return method.returnType;
	} 

	public Type visit(IntegerLiteral n) {
		e.emitBuf(n.f0.tokenImage);
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
		selfMethod.emitByName(e, name);
		return type;
	}

	public Type visit(ThisExpression n) {
		e.emitBuf("TEMP", "0");
		return selfClass;
	}

	public Type visit(ArrayAllocationExpression n) {
		Type a = n.f3.accept(this);
		if (!(a instanceof IntType)) {
			Info.panic("ArrayAllocationExpression");
		}
		return new ArrType();
	}

	/**
	new a()

	BEGIN
		MOVE TEMP 1 HALLOCATE $(size of a)
		MOVE TEMP 2 HALLOCATE $(size of vitrual table)
		HSTORE TEMP 1 0 TEMP 2
		HSTORE TEMP 2 0 $(method1)
		HSTORE TEMP 2 4 $(method2)
		HSTORE TEMP 2 8 $(method3)
		...
	RETURN TEMP 1 END
	*/
	public Type visit(AllocationExpression n) {
		String name = n.f1.f0.tokenImage;
		ClassType type = classes.get(name);

		String temp1 = e.newTemp();
		String temp2 = e.newTemp();
		e.emitOpen("/*new", name, "*/", "BEGIN");
		e.emit("MOVE", temp1, "HALLOCATE", 
			// plus one for virtual table
			e.numToOffset(type.sizeOfClass() + 1));
		e.emit("MOVE", temp2, "HALLOCATE",
			e.numToOffset(type.sizeOfTable()));
		e.emit("HSTORE", temp1, "0", temp2);
		for (ClassType.Method method : type.dynamicMethods) {
			e.emit("HSTORE", temp2, 
				e.numToOffset(type.indexOfMethod(method.name)),
				method.getLabel());
		}
		e.emitClose("RETURN", temp1, "END");

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

	/**
	if (a) b else c

	CJUMP a L1
		b
	JUMP L2
		L1 c
	L2 NOOP
	*/
	public Type visit(IfStatement n) {
		e.emitBuf("/*if*/", "CJUMP");
		Type a = n.f2.accept(this);
		if (!(a instanceof BoolType)) {
			Info.panic("IfStatement condition is not a BoolType");
		}
		String L1 = e.newLabel();
		String L2 = e.newLabel();
		e.emitBuf(L1);
		e.emitOpen();
		n.f4.accept(this);
		e.emitClose("/*else*/", "JUMP", L2);
		e.emitOpen();
		e.emitBuf(L1);
		n.f6.accept(this);
		e.emitClose("/*endif*/", L2, "NOOP");
		return null;
	}

	public Type visit(WhileStatement n) {
		Type a = n.f2.accept(this);
		if (!(a instanceof BoolType)) {
			Info.panic("IfStatement condition is not a BoolType");
		}
		n.f4.accept(this);
		return null;
	}

	public Type visit(AssignmentStatement n) {
		String name = n.f0.f0.tokenImage;
		Type a = selfMethod.getTypeByName(name);
		selfMethod.emitAssignByName(e, name);
		Type b = n.f2.accept(this);
		typeCastCheck(b, a);
		return null;
	}

	public Type visit(ArrayAssignmentStatement n) {
		Type a = n.f0.accept(this);
		Type b = n.f2.accept(this);
		Type c = n.f5.accept(this);
		if (!(a instanceof ArrType
			&& b instanceof IntType
			&& c instanceof IntType)) {
			Info.panic("ArrayAssignmentStatement");
		}
		return null;
	}

	public Type visit(PrintStatement n) {
		e.emitOpen("PRINT");
		Type a = n.f2.accept(this);
		if (!(a instanceof IntType)) {
			Info.panic("can not print " + a);
		}
		e.emitClose("/*PRINT*/");
		return null;
	}
}

class TypeCheckResult {
	ClassCollection classes;
	Node root;

	TypeCheckResult(ClassCollection classes, Node root) {
		this.classes = classes;
		this.root = root;
	}
}

public class TypeCheck {
	static TypeCheckResult TypeCheck(Emitter e) throws Exception {
		MiniJavaParser parser = new MiniJavaParser(System.in);
		ClassCollection classes = new ClassCollection();
		Node root = parser.Goal();

		root.accept(new ScanForClassName(), classes);
		root.accept(new ScanForSuperClassName(), classes);
		root.accept(new ScanClassMethods(classes));
		classes.dump();
		classes.analyze();
		root.accept(new GetExpressionType(classes, e));
		return new TypeCheckResult(classes, root);
	}

	public static void main(String []args) throws Exception {
		Emitter e = new Emitter();
		if (Info.DEBUG) {
			TypeCheck(e);
		}

		try {
			TypeCheck(e);	
		} catch (Exception e_) {
			System.exit(0);
		}

		System.out.println("Program type checked successfully");
	}
}


