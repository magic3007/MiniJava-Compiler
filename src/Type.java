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

	void dump(ClassType type) {
		String msg = type.name;
		if (type.superclass != null) {
			msg += " extends " + type.superclass.name;
		}
		Info.debug("class:", msg);
	}

	void dump() {
		for (ClassType type : collection.values()) {
			dump(type);
		}
	}
}
