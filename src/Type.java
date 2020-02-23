import java.util.*;

class Type {}

class PrimitiveType extends Type {}

class BoolType extends PrimitiveType {}
class IntType extends PrimitiveType {}
class ArrayType extends PrimitiveType {}


class Variable {
	Type type;
	String name;

	Variable(Type type, String name) {
		this.type = type;
		this.name = name;
	}
}

class VariableList {
	List<Variable> variables;

	VariableList() {
		variables = new ArrayList<Variable>();
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
		if (rv != null) return rv;

		if (superclass == null) {
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
			return null;
		} else {
			return superclass.getMethodByName(name);
		}
	}

	class Method {
		String name;
		Type returnType;

		VariableList param;
		VariableList temp;

		// TODO: check if paramNames conflit with tempNames

		Method() {
			param = new VariableList();
			temp = new VariableList();
		}

		Type getTypeByName(String name) {
			Type rv = null;

			rv = temp.lookupByName(name);
			if (rv != null) return rv;
			rv = param.lookupByName(name);
			if (rv != null) return rv;
			
			// call getTypeByName() in ClassType
			return ClassType.this.getTypeByName(name);
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

	String dump(VariableList list) {
		String rv = "";
		for (Variable variable : list.variables) {
			rv += typeToName(variable.type) + " " + variable.name + ", ";
		}
		return rv;
	}

	void dump(ClassType.Method method) {
		String rv = typeToName(method.returnType) 
			+ " ( " + dump(method.param) + " )"
			+ "\t- " + dump(method.temp);
		Info.debug("\t", rv);
	}

	void dump(ClassType type) {
		String msg = type.name;
		if (type.superclass != null) {
			msg += " extends " + type.superclass.name;
		}
		Info.debug("class:", msg, "\t-", dump(type.field));

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
