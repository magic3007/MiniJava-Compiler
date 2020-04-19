import minijava.parser.*;
import minijava.visitor.*;
import minijava.syntaxtree.*;
import java.util.*;
import java.io.*;

// 1st pass: scan globally for class name
class ScanForClassName<T extends ClassCollection> extends GJVoidDepthFirst<T> {
	public void visit(final ClassDeclaration node, final T classes) {
		// the class name
		classes.add(node.f1.f0.tokenImage);
	}

	public void visit(final ClassExtendsDeclaration node, final T classes) {
		// the class name
		classes.add(node.f1.f0.tokenImage);
	}

	public void visit(final MainClass node, final T classes) {
		classes.add(node.f1.f0.tokenImage);
	}
}

// 2nd pass:
class ScanForSuperClassName<T extends ClassCollection> extends GJVoidDepthFirst<T> {
	public void visit(final ClassExtendsDeclaration node, final T classes) {
		// the class name
		final String derivedClassName = node.f1.f0.tokenImage;
		final String superClassName = node.f3.f0.tokenImage;
		final ClassType derivedClass = classes.get(derivedClassName);
		final ClassType superClass = classes.get(superClassName);
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
	GetTypeVisitor(final ClassCollection classes, final Option option) {
		this.classes = classes;
		this.option = option;
	}

	ClassCollection classes;
	Option option;

	public void visit(final BooleanType node) {
		option.value = new BoolType();
	}

	public void visit(final IntegerType node) {
		option.value = new IntType();
	}

	public void visit(final ArrayType node) {
		option.value = new ArrType();
	}

	public void visit(final Identifier node) {
		final String name = node.f0.tokenImage;
		option.value = classes.get(name);
	}
}

class ScanClassMethods extends DepthFirstVisitor {
	ScanClassMethods(final ClassCollection classes) {
		this.classes = classes;
	}

	ClassCollection classes;
	ClassType selfClass;
	ClassType.Method selfMethod;

	public void visit(final ClassDeclaration node) {
		selfClass = classes.get(node.f1.f0.tokenImage);
		super.visit(node);
	}

	public void visit(final ClassExtendsDeclaration node) {
		selfClass = classes.get(node.f1.f0.tokenImage);
		super.visit(node);
	}

	public void visit(final MethodDeclaration node) {
		final String name = node.f2.f0.tokenImage;
		final ClassType.Method method = selfClass.new Method();
		method.name = name;
		final Option option = new Option();
		node.f1.accept(new GetTypeVisitor(classes, option));
		method.returnType = (Type) option.value;
		selfMethod = method;
		super.visit(node);
		selfClass.methods.add(method);
		selfMethod = null;
	}

	public void visit(final FormalParameter node) {
		final Option option = new Option();
		node.f0.accept(new GetTypeVisitor(classes, option));
		final Type type = (Type) option.value;
		final String name = node.f1.f0.tokenImage;
		selfMethod.param.add(type, name);
	}

	public void visit(final VarDeclaration node) {
		final Option option = new Option();
		node.f0.accept(new GetTypeVisitor(classes, option));
		final Type type = (Type) option.value;
		final String name = node.f1.f0.tokenImage;
		if (selfMethod != null) {
			if (selfMethod.param.lookupByName(name) != null) {
				Info.panic("temporary variable name has conflict with parameter name");
			}
			selfMethod.temp.add(type, name);
		} else {
			selfClass.field.add(type, name);
		}
	}

	public void visit(final MainClass node) {
		selfClass = classes.get(node.f1.f0.tokenImage);
		final ClassType.Method method = selfClass.new StaticMethod();
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
// and Piglet generation


abstract class AbstractGetExpressionType<T> extends GJNoArguDepthFirst<T> {
}

class GetExpressionType extends AbstractGetExpressionType<Type> {
	GetExpressionType(final ClassCollection classes, final Emitter e) {
		this.classes = classes;
		this.e = e;
	}

	Emitter e;
	ClassCollection classes;
	ClassType selfClass;
	ClassType.Method selfMethod;
	final static int kMaxMethodParameterCount = 20;

	public Type visit(final ClassDeclaration node) {
		selfClass = classes.get(node.f1.f0.tokenImage);
		return node.f4.accept(this);
	}

	public Type visit(final ClassExtendsDeclaration node) {
		selfClass = classes.get(node.f1.f0.tokenImage);
		return node.f6.accept(this);
	}

	/**
	 * <Method Label> [ $(min(method.numOfParams()+1,20)) ] BEGIN
	 * 
	 * $(if method.numOfParams()>=19)
	 * HLOAD TEMP n   TEMP 19 $(4*(n-19))
	 * HLOAD TEMP n-1 TEMP 19 $(4*(n-1-19))
	 * ...
	 * HLOAD TEMP 20 TEMP 19 4
	 * HLOAD TEMP 19 TEMP 19 0
	 * $(endif)
	*/
	public Type visit(final MethodDeclaration node) {
		final String name = node.f2.f0.tokenImage;
		final ClassType.Method method = selfClass.getMethodByName(name);
		selfMethod = method;
		e.emitOpen(method.getLabel(), "[", Integer.toString(Math.min(method.numOfParams() + 1, kMaxMethodParameterCount)), "]", "BEGIN");
		if (method.numOfParams() + 1 >= kMaxMethodParameterCount){
			for(int i = method.numOfParams(); i >= 19; i--){
				e.emit("HLOAD", "TEMP", Integer.toString(i), "TEMP", Integer.toString(kMaxMethodParameterCount -1),
					Integer.toString(4 * (i - kMaxMethodParameterCount + 1)));
			}
		}
		node.f8.accept(this);
		e.emitClose();
		e.emitOpen("RETURN");
		final Type returnType = node.f10.accept(this);
		Type.typeCastCheck(returnType, method.returnType);
		selfMethod = null;
		e.emitClose("/* end", method.getLabel(), "*/", "END");
		return null;
	}

	public Type visit(final MainClass node) {
		e.emitOpen("MAIN");
		selfClass = classes.get(node.f1.f0.tokenImage);
		selfMethod = selfClass.getMethodByName("main");
		node.f15.accept(this);
		e.emitClose("END");
		return null;
	}

	public Type visit(final Expression n) {
		return n.f0.accept(this);
	}

	public Type visit(final CompareExpression n) {
		e.emitBuf("LT");
		final Type a = n.f0.accept(this);
		final Type b = n.f2.accept(this);
		if (a instanceof IntType && b instanceof IntType) {
			return new BoolType();
		} else {
			Info.panic("CompareExpression");
			return null;
		}
	}

	/**
	 * a && b
	 * 
	 * BEGIN MOVE TEMP 1 a CJUMP TEMP 1 L1 MOVE TEMP 1 b L1 NOOP RETURN TEMP 1 END
	 */
	public Type visit(final AndExpression n) {
		e.emitOpen("/* && */", "BEGIN");
		final String temp1 = e.newTemp();
		final String L1 = e.newLabel();
		e.emitBuf("MOVE", temp1);
		final Type a = n.f0.accept(this);
		e.emitFlush();
		e.emit("CJUMP", temp1, L1);
		e.emitBuf("MOVE", temp1);
		final Type b = n.f2.accept(this);
		if (!(a instanceof BoolType && b instanceof BoolType)) {
			Info.panic("AndExpression");
			return null;
		}
		e.emitFlush();
		e.emit(L1, "NOOP");
		e.emitClose("RETURN", temp1, "END");
		return new BoolType();
	}

	public Type visit(final PlusExpression n) {
		e.emitBuf("PLUS");
		final Type a = n.f0.accept(this);
		final Type b = n.f2.accept(this);
		if (a instanceof IntType && b instanceof IntType) {
			return new IntType();
		} else {
			Info.panic("PlusExpression");
			return null;
		}
	}

	public Type visit(final MinusExpression n) {
		e.emitBuf("MINUS");
		final Type a = n.f0.accept(this);
		final Type b = n.f2.accept(this);
		if (a instanceof IntType && b instanceof IntType) {
			return new IntType();
		} else {
			Info.panic("MinusExpression");
			return null;
		}
	}

	public Type visit(final TimesExpression n) {
		e.emitBuf("TIMES");
		final Type a = n.f0.accept(this);
		final Type b = n.f2.accept(this);
		if (a instanceof IntType && b instanceof IntType) {
			return new IntType();
		} else {
			Info.panic("TimesExpression");
			return null;
		}
	}

	public Type visit(final PrimaryExpression n) {
		return n.f0.accept(this);
	}

	/**
	 * a[b]
	 * 
	 * BEGIN HLOAD TEMP 1 PLUS a TIMES 4 PLUS 1 b 0 RETURN TEMP 1 END
	 */
	public Type visit(final ArrayLookup n) {
		e.emitOpen("BEGIN", "/* ArrayLookup */");
		final String temp1 = e.newTemp();
		e.emit("HLOAD", temp1, "PLUS");
		e.emitBuf("/* array: */");
		final Type a = n.f0.accept(this);
		e.emitFlush();
		e.emitBuf("TIMES", e.numToOffset(1), "PLUS", "1");
		e.emitBuf("/* index */");
		final Type b = n.f2.accept(this);
		e.emitBuf("0");
		e.emitFlush();
		if (!(a instanceof ArrType && b instanceof IntType)) {
			Info.panic("ArrayLookup");
			return null;
		}
		e.emitClose("RETURN", temp1, "END");
		return new IntType();
	}

	/**
	 * a.length
	 * 
	 * BEGIN HLOAD TEMP 1 a 0 RETURN TEMP 1 END
	 */
	public Type visit(final ArrayLength n) {
		final String temp1 = e.newTemp();
		e.emitOpen("/* .length */", "HLOAD", temp1);
		final Type a = n.f0.accept(this);
		e.emitBuf("0");
		e.emitClose("RETURN", temp1, "END");
		if (a instanceof ArrType) {
			return new IntType();
		} else {
			Info.panic("ArrayLookup");
			return null;
		}
	}

	class GetExpressionListType extends AbstractGetExpressionType<PackedTypeAndTemp> {
		public PackedTypeAndTemp visit(final NodeOptional n) {
			if (n.present())
				return n.node.accept(this);
			else
				return new PackedTypeAndTemp();
		}

		public PackedTypeAndTemp visit(final NodeListOptional n) {
			final PackedTypeAndTemp rv = new PackedTypeAndTemp();

			if (!n.present()) {
				return rv;
			}

			for (final Enumeration<Node> e = n.elements(); e.hasMoreElements();) {
				final ExpressionRest ee = (ExpressionRest) e.nextElement();
				final String temp = GetExpressionType.this.e.newTemp();
				GetExpressionType.this.e.emitBuf("MOVE", temp);
				rv.type_list.add(ee.f1.accept(GetExpressionType.this));
				rv.temp_list.add(temp);
				GetExpressionType.this.e.emitFlush();
			}
			return rv;
		}

		public PackedTypeAndTemp visit(final ExpressionList n) {
			final Expression e = (Expression) n.f0;
			final String temp = GetExpressionType.this.e.newTemp();
			GetExpressionType.this.e.emitBuf("MOVE", temp);
			final Type t = e.accept(GetExpressionType.this);
			GetExpressionType.this.e.emitFlush();
			final PackedTypeAndTemp rv = n.f1.accept(this);
			// prepend |e|
			rv.type_list.add(0, t);
			rv.temp_list.add(0, temp);
			return rv;
		}
	}


	class PackedTypeAndTemp {
		List<Type> type_list = new ArrayList<Type>();
		List<String> temp_list = new ArrayList<String>();
	}

	/**
	 * a.b(...)
	 * 
	 * MOVE TEMP a0 arg1
	 * MOVE TEMP a1 arg2
	 * ...
	 * MOVE TEMP an-1 argn
	 * 
	 * $(if method.param.size() >= 19)
	 * MOVE TEMP an HAllOCATE $(4*(method.param.size()-18))
	 * HSTORE TEMP an 0 TEMP a18
	 * HSTORE TEMP an 4 TEMP a19
	 * ...
	 * HSTORE TEMP an $(4*(method.param.size()-19)) an-1
	 * MOVE temp a18 an 
	 * $(endif)
	 * 
	 * CALL 
	 * 	BEGIN 
	 * 		MOVE TEMP 1 a 
	 * 		HLOAD TEMP 2 TEMP 1 0 
	 * 		HLOAD TEMP 2 TEMP 2 $(offset of b) 
	 * 		RETURN TEMP 2 
	 * 	END ( TEMP 1 TEMP a0 TEMP a1 ... TEMP a_{min(18, n-1)})
	 */
	public Type visit(final MessageSend n) {
		e.emitOpen("CALL", "BEGIN");
		
		final String temp1 = e.newTemp();
		final String temp2 = e.newTemp();
		e.emitBuf("MOVE", temp1);
		
		final Type a = n.f0.accept(this);
		if (!(a instanceof ClassType)) {
			Info.panic("MessageSend");
			return null;
		}
		final ClassType ct = (ClassType) a;
		final String methodname = n.f2.f0.tokenImage;
		final ClassType.Method method = ct.getMethodByName(methodname);


		final PackedTypeAndTemp rv = n.f4.accept(new GetExpressionListType());
		final List<Type> args = rv.type_list;
		final List<String> temps = rv.temp_list;

		final int length = args.size();
		if (length != method.param.size()) {
			Info.panic("unequal number of arguments " + length + " -> " + method.param.size());
		}

		for (int i = 0; i < length; i++) {
			Type.typeCastCheck(args.get(i), method.param.get(i).type);
		}

		if (method.param.size() +1 >= kMaxMethodParameterCount){
			String temp = e.newTemp();
			e.emit("MOVE", temp, "HALLOCATE", Integer.toString(4 * (method.param.size() - kMaxMethodParameterCount + 2)) );
			for(int i = kMaxMethodParameterCount - 2; i < method.param.size(); i++){
				e.emit("HSTORE", temp, Integer.toString(4 * (i - kMaxMethodParameterCount + 2)), temps.get(i));
			}
			e.emit("MOVE", temps.get(kMaxMethodParameterCount - 2), temp);
		}

		e.emitFlush();
		e.emit("HLOAD", temp2, temp1, "0");
		e.emitBuf("HLOAD", temp2, temp2, e.numToOffset(ct.indexOfMethod(methodname)));
		e.emitClose("RETURN", temp2, "END", "(");
		e.emitOpen();
		e.emit(temp1);
		for(int i = 0; i < Math.min(method.numOfParams() , kMaxMethodParameterCount -1); i++) {
			e.emitBuf(temps.get(i));
		}
		e.emitClose(")");
		return method.returnType;
	}

	public Type visit(final IntegerLiteral n) {
		e.emitBuf(n.f0.tokenImage);
		return new IntType();
	}

	public Type visit(final TrueLiteral n) {
		e.emitBuf("1");
		return new BoolType();
	}

	public Type visit(final FalseLiteral n) {
		e.emitBuf("0");
		return new BoolType();
	}

	public Type visit(final Identifier n) {
		final String name = n.f0.tokenImage;
		final Type type = selfMethod.getTypeByName(name);
		selfMethod.emitByName(e, name);
		return type;
	}

	public Type visit(final ThisExpression n) {
		e.emitBuf("TEMP", "0");
		return selfClass;
	}

	/**
	 * new int[a]
	 * 
	 * BEGIN MOVE TEMP 1 a MOVE TEMP 2 HALLOCATE TIMES 4 PLUS 1 TEMP 1 HSTORE TEMP 2
	 * 0 TEMP 1 RETURN TEMP 2 END
	 */
	public Type visit(final ArrayAllocationExpression n) {
		final String temp1 = e.newTemp();
		final String temp2 = e.newTemp();
		e.emitOpen("BEGIN", "/* new int[] */");
		e.emitBuf("MOVE", temp1);
		final Type a = n.f3.accept(this);
		if (!(a instanceof IntType)) {
			Info.panic("ArrayAllocationExpression");
		}
		e.emitFlush();
		e.emit("MOVE", temp2, "HALLOCATE", "TIMES", e.numToOffset(1), "PLUS", "1", temp1);
		e.emit("HSTORE", temp2, "0", temp1);
		e.emitClose("RETURN", temp2, "END");

		return new ArrType();
	}

	/**
	 * new a()
	 * 
	 * BEGIN MOVE TEMP 1 HALLOCATE $(size of a) MOVE TEMP 2 HALLOCATE $(size of
	 * vitrual table) HSTORE TEMP 1 0 TEMP 2 HSTORE TEMP 2 0 $(method1) HSTORE TEMP
	 * 2 4 $(method2) HSTORE TEMP 2 8 $(method3) ... RETURN TEMP 1 END
	 */
	public Type visit(final AllocationExpression n) {
		final String name = n.f1.f0.tokenImage;
		final ClassType type = classes.get(name);

		final String temp1 = e.newTemp();
		final String temp2 = e.newTemp();
		e.emitOpen("/*new", name, "*/", "BEGIN");
		e.emit("MOVE", temp1, "HALLOCATE",
				// plus one for virtual table
				e.numToOffset(type.sizeOfClass() + 1));
		e.emit("MOVE", temp2, "HALLOCATE", e.numToOffset(type.sizeOfTable()));
		e.emit("HSTORE", temp1, "0", temp2);
		for (final ClassType.Method method : type.dynamicMethods) {
			e.emit("HSTORE", temp2, e.numToOffset(type.indexOfMethod(method.name)), method.getLabel());
		}
		e.emitClose("RETURN", temp1, "END");

		return type;
	}

	/*
	 * !a
	 * 
	 * MINUS 1 a
	 */
	public Type visit(final NotExpression n) {
		e.emitBuf("MINUS", "1", "/* not */");
		final Type a = n.f1.accept(this);
		if (!(a instanceof BoolType)) {
			Info.panic("ArrayAllocationExpression");
		}
		return new BoolType();
	}

	public Type visit(final BracketExpression n) {
		return n.f1.accept(this);
	}

	/**
	 * if (a) b else c
	 * 
	 * CJUMP a L1 b JUMP L2 L1 c L2 NOOP
	 */
	public Type visit(final IfStatement n) {
		e.emitBuf("/* if */", "CJUMP");
		final Type a = n.f2.accept(this);
		if (!(a instanceof BoolType)) {
			Info.panic("IfStatement condition is not a BoolType");
		}
		final String L1 = e.newLabel();
		final String L2 = e.newLabel();
		e.emitBuf(L1);
		e.emitOpen();
		n.f4.accept(this);
		e.emitClose("/* else */", "JUMP", L2);
		e.emitOpen();
		e.emitBuf(L1);
		n.f6.accept(this);
		e.emitClose("/* endif */", L2, "NOOP");
		return null;
	}

	/**
	 * while (a) b
	 * 
	 * L1 NOOP CJUMP a L2 b JUMP L1 L2 NOOP
	 */
	public Type visit(final WhileStatement n) {
		final String L1 = e.newLabel();
		final String L2 = e.newLabel();
		e.emitFlush();
		e.emit("/* while */", L1, "NOOP");
		e.emitBuf("CJUMP");
		final Type a = n.f2.accept(this);
		if (!(a instanceof BoolType)) {
			Info.panic("IfStatement condition is not a BoolType");
		}
		e.emitBuf(L2);
		e.emitFlush();
		e.emitOpen();
		n.f4.accept(this);
		e.emitClose("JUMP", L1);
		e.emit("/* endwhile */", L2, "NOOP");
		return null;
	}

	/*
	 * a = b
	 */
	public Type visit(final AssignmentStatement n) {
		final String name = n.f0.f0.tokenImage;
		final Type a = selfMethod.getTypeByName(name);
		selfMethod.emitAssignByName(e, name);
		final Type b = n.f2.accept(this);
		Type.typeCastCheck(b, a);
		return null;
	}

	/**
	 * a[b] = c
	 * 
	 * HSTORE PLUS a TIMES 4 PLUS 1 b 0 c
	 */
	public Type visit(final ArrayAssignmentStatement n) {
		e.emitOpen("/* ArraryAssign */", "HSTORE", "PLUS");
		final Type a = n.f0.accept(this);
		e.emitFlush();
		e.emit("TIMES", e.numToOffset(1), "PLUS", "1");
		e.emitBuf("/* [] */");
		final Type b = n.f2.accept(this);
		e.emitBuf("0");
		e.emitFlush();
		e.emitBuf("/* = */");
		final Type c = n.f5.accept(this);
		if (!(a instanceof ArrType && b instanceof IntType && c instanceof IntType)) {
			Info.panic("ArrayAssignmentStatement");
		}
		e.emitClose();
		return null;
	}

	public Type visit(final PrintStatement n) {
		e.emitOpen("PRINT");
		final Type a = n.f2.accept(this);
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

	TypeCheckResult(final ClassCollection classes, final Node root) {
		this.classes = classes;
		this.root = root;
	}
}

public class TypeCheck {
	static TypeCheckResult TypeCheck(final Emitter e, String filename) throws Exception {
		final MiniJavaParser parser = new MiniJavaParser(
			filename == null ? System.in : new FileInputStream(filename));
		final ClassCollection classes = new ClassCollection();
		final Node root = parser.Goal();

		root.accept(new ScanForClassName(), classes);
		root.accept(new ScanForSuperClassName(), classes);
		root.accept(new ScanClassMethods(classes));
		classes.dump();
		classes.analyze();
		root.accept(new GetExpressionType(classes, e));
		return new TypeCheckResult(classes, root);
	}

	public static void main(final String[] args) throws Exception {
		final Emitter e = new Emitter();
		if (Info.DEBUG) {
			TypeCheck(e, args.length > 0 ? args[0] : null);
		} else {
			try {
				TypeCheck(e, args.length > 0 ? args[0] : null);
			} catch (final Exception e_) {
				System.out.println("Type error");
				System.exit(0);
			}
		}
		System.out.println("Program type checked successfully");
	}
}
