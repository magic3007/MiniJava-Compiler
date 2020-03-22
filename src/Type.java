import java.util.*;

class Type {
	public String toString() {
		Type type = this;
		if (type instanceof ClassType) {
			return ((ClassType) type).name;
		}
		if (type instanceof IntType) {
			return "int";
		}
		if (type instanceof BoolType) {
			return "bool";
		}
		if (type instanceof ArrType) {
			return "int[]";
		}
		if (type instanceof VoidType) {
			return "void";
		}
		return this.getClass().getSimpleName();
	}

	static void typeCastCheck(final Type from, final Type to) {
		if (from instanceof PrimitiveType) {
			if (!from.getClass().equals(to.getClass())) {
				Info.panic("incompatible primitive type " + from + " -> " + to);
			}
		} else if (to instanceof PrimitiveType) {
			Info.panic("cast ClassType to PrimitiveType " + from + " -> " + to);
		} else {
			ClassType a = (ClassType) from;
			final ClassType b = (ClassType) to;
			while (a != null) {
				if (a.name.equals(b.name)) {
					return;
				}
				a = a.superclass;
			}
			Info.panic("cast to derived class failed " + from + " -> " + to);
		}
	}
}

class PrimitiveType extends Type {
}

class BoolType extends PrimitiveType {
}

class IntType extends PrimitiveType {
}

class ArrType extends PrimitiveType {
}

class VoidType extends PrimitiveType {
}


class Variable {
	Type type;
	String name;

	Variable(Type type, String name) {
		this.type = type;
		this.name = name;
	}
}

// consider to extends the List
class VariableList {
	List<Variable> variables;

	VariableList() {
		variables = new ArrayList<Variable>();
	}

	int size() {
		return variables.size();
	}

	Variable get(int index) {
		return variables.get(index);
	}

	int indexOf(String name) {
		for (int i = 0; i < variables.size(); i++) {
			if (variables.get(i).name.equals(name)) {
				return i;
			}
		}
		return -1;
	}

	Type lookupByName(String name) {
		for (Variable variable : variables) {
			if (variable.name.equals(name)) {
				return variable.type;
			}
		}
		return null;
	}

	void add(Type type, String name) {
		variables.add(new Variable(type, name));
	}
}

class ClassType extends Type {
	ClassType(String name) {
		this.name = name;
		this.methods = new ArrayList<Method>();
		this.field = new VariableList();
	}

	String name;
	// null for no superclass
	ClassType superclass;

	VariableList field;

	Type getTypeByName(String name) {
		Type rv = field.lookupByName(name);
		if (rv != null)
			return rv;

		if (superclass == null) {
			Info.panic("can not find symbol " + name);
			return null;
		} else {
			return superclass.getTypeByName(name);
		}
	}

	Method getMethodByName(String name) {
		for (Method method : methods) {
			if (method.name.equals(name)) {
				return method;
			}
		}

		if (superclass == null) {
			Info.panic("can not find method", name);
			return null;
		} else {
			return superclass.getMethodByName(name);
		}
	}

	void emitByName(Emitter e, String name) {
		int rv = field.indexOf(name);
		if (rv < 0) {
			if (superclass == null) {
				Info.panic("can not find field", name);
			} else {
				superclass.emitByName(e, name);
				return;
			}
		}

		e.emitFlush();
		String tmp = e.newTemp();
		e.emit("/* field:", this.name, name, "*/", "BEGIN", "HLOAD", tmp, "TEMP 0",
				// extra 1 for the virtual table
				e.numToOffset(1 + rv + sizeOfSuperClasses), "RETURN", tmp, "END");
	}

	void emitAssignByName(Emitter e, String name) {
		int rv = field.indexOf(name);
		if (rv < 0) {
			if (superclass == null) {
				Info.panic("can not find field", name);
			} else {
				superclass.emitAssignByName(e, name);
				return;
			}
		}
		e.emitBuf("HSTORE", "TEMP 0", e.numToOffset(1 + rv + sizeOfSuperClasses));
	}

	class Method {
		String name;
		Type returnType;

		VariableList param;
		VariableList temp;


		Method() {
			param = new VariableList();
			temp = new VariableList();
		}

		Type getTypeByName(String name) {
			Type rv = null;

			rv = temp.lookupByName(name);
			if (rv != null)
				return rv;
			rv = param.lookupByName(name);
			if (rv != null)
				return rv;

			// call getTypeByName() in ClassType
			return ClassType.this.getTypeByName(name);
		}

		private int getTempAddressIndex(String name) {
			int rv;

			rv = param.indexOf(name);
			// the extra 1 is for `this` parameter
			if (rv >= 0) {
				return rv + 1;
			}
			rv = temp.indexOf(name);
			if (rv >= 0) {
				return param.size() + rv + 1;
			}
			return -1;
		}

		void emitByName(Emitter e, String name) {
			int rv;

			rv = getTempAddressIndex(name);
			if (rv >= 0) {
				e.emitBuf("TEMP", Integer.toString(rv));
				return;
			}
			ClassType.this.emitByName(e, name);
		}

		void emitAssignByName(Emitter e, String name) {
			int rv;

			rv = getTempAddressIndex(name);
			if (rv >= 0) {
				e.emitBuf("MOVE", "TEMP", Integer.toString(rv));
				return;
			}
			ClassType.this.emitAssignByName(e, name);
		}

		String getLabel() {
			return ClassType.this.name + "_" + name;
		}

		int numOfParams() {
			return param.size();
		}
	}

	class StaticMethod extends Method {
	}

	List<Method> methods;

	// the fileds and methods belower are for code generation

	List<Method> dynamicMethods;
	int sizeOfSuperClasses;

	int indexOfField(String name) {
		int rv = field.indexOf(name);
		if (rv >= 0) {
			return sizeOfSuperClasses + rv;
		}

		if (superclass == null) {
			Info.panic("can not find " + name);
		}
		return superclass.indexOfField(name);
	}

	int indexOfMethod(String name) {
		for (int i = 0; i < dynamicMethods.size(); i++) {
			if (dynamicMethods.get(i).name.equals(name)) {
				return i;
			}
		}
		return -1;
	}

	Method getDynamicMethodByName(String name){
		for(Method method : dynamicMethods){
			if(method.name.equals(name)){
				return method;
			}
		}
		return null;
	}

	int sizeOfClass() {
		return sizeOfSuperClasses + field.size();
	}

	int sizeOfTable() {
		return dynamicMethods.size();
	}

	static boolean isVariablesTypeSame(VariableList a, VariableList b) {
		if (a. size() != b. size()) return false;
		for(int i = 0; i < a.size(); i++){
			if(!a.get(i).type.toString().equals(b.get(i).type.toString())) return false;
		}
		return true;
	}

	enum EnumAnalyzeState{
		UNDO, ONGOING, DONE;
	}
	EnumAnalyzeState analyzeState = EnumAnalyzeState.UNDO;

	void analyze() {
		if (analyzeState.equals(EnumAnalyzeState.ONGOING)) {
			Info.panic("Recursive Extension.");
		}

		// if have been analyzed
		if (!(dynamicMethods == null)) {
			return;
		}

		analyzeState = EnumAnalyzeState.ONGOING;

		if (superclass != null) {
			superclass.analyze();
			sizeOfSuperClasses = superclass.sizeOfClass();
			dynamicMethods = new ArrayList<Method>(superclass.dynamicMethods);
		} else {
			sizeOfSuperClasses = 0;
			dynamicMethods = new ArrayList<Method>();
		}

		for (Method method : methods) {
			int i = indexOfMethod(method.name);
			if (i < 0) {
				dynamicMethods.add(method);
			} else {
				Method superMethod = getDynamicMethodByName(method.name);
				if(isVariablesTypeSame(method.param, superMethod.param) == false) {
					Info.panic("Overloading is not allowed in MiniJava!");
				}
				// override the superclass's method
				typeCastCheck(method.returnType, superMethod.returnType);
				dynamicMethods.set(i, method);
			}
		}

		analyzeState = EnumAnalyzeState.DONE;
	}
}

class ClassCollection {
	Map<String, ClassType> collection;

	ClassCollection() {
		collection = new HashMap<String, ClassType>();
	}

	void add(String name) {
		if (collection.containsKey(name)) {
			Info.panic("redefinition of class " + name);
		}

		collection.put(name, new ClassType(name));
	}

	void analyze() {
		for (ClassType c : collection.values()) {
			c.analyze();
		}
	}

	ClassType get(String name) {
		ClassType type = collection.get(name);
		if (type == null) {
			Info.panic("can not find class", name);
		}
		return type;
	}

	String dump(VariableList list) {
		String rv = "";
		for (Variable variable : list.variables) {
			rv += variable.type + " " + variable.name + ", ";
		}
		return rv;
	}

	void dump(ClassType.Method method) {
		String rv = method.returnType + " ( " + dump(method.param) + " )" + "\t- " + dump(method.temp);
		Info.debug("\t", rv);
	}

	void dump(ClassType type) {
		String msg = type.name;
		if (type.superclass != null) {
			msg += " extends " + type.superclass.name;
		}
		Info.debug("class:", msg, "\t-", dump(type.field));

		if (type.dynamicMethods != null) {
			for (ClassType.Method method : type.dynamicMethods) {
				dump(method);
			}
		} else {
			for (ClassType.Method method : type.methods) {
				dump(method);
			}
		}
	}

	void dump() {
		for (ClassType type : collection.values()) {
			dump(type);
		}
	}
}
