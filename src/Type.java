import java.util.*;

class Type {}

class PrimitiveType extends Type {}

class BoolType extends PrimitiveType {}
class IntType extends PrimitiveType {}
class ArrayType extends PrimitiveType {}

class ClassType extends Type {
	ClassType(String name) {
		this.name = name;
		this.methods = new ArrayList<Method>();
	}

	String name;
	// null for no superclass
	ClassType superclass;

	class Method {
		String name;
		Type returnType;
		List<Type> paramTypes;
		List<String> paramNames;

		Method() {
			paramTypes = new ArrayList<Type>();
			paramNames = new ArrayList<String>();
		}
	}

	List<Method> methods;
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

	ClassType get(String name) {
		ClassType type = collection.get(name);
		if (type == null) {
			Info.panic("can not find class", name);
		}
		return type;
	}

	String typeToName(Type type) {
		if (type instanceof ClassType) {
			return ((ClassType) type).name;
		}
		if (type instanceof IntType) {
			return "int";
		}
		if (type instanceof BoolType) {
			return "bool";
		}
		if (type instanceof ArrayType) {
			return "int[]";
		}
		return this.getClass().getSimpleName();
	}

	void dump(ClassType.Method method) {
		String rv = typeToName(method.returnType) + " ( ";
		for (Type type : method.paramTypes) {
			rv += typeToName(type) + " ";
		}
		rv += ")";
		Info.debug("\t", rv);
	}

	void dump(ClassType type) {
		String msg = type.name;
		if (type.superclass != null) {
			msg += " extends " + type.superclass.name;
		}
		Info.debug("class:", msg);

		for (ClassType.Method method : type.methods) {
			dump(method);
		}
	}

	void dump() {
		for (ClassType type : collection.values()) {
			dump(type);
		}
	}
}
