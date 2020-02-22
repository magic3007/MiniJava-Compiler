import java.util.*;

class Environment {
	static class VariableInfo {
		String name;
		Type type;
	}


	// how many variables in scope #i
	private Deque<Integer> numVarInScope;

	Deque<VariableInfo> environment;

	Environment() {
		environment = new LinkedList<VariableInfo>();
		numVarInScope = new LinkedList<Integer>();
	}

	void addVariable(VariableInfo var) {
		int num = numVarInScope.pop();
		environment.add(var);
		numVarInScope.add(num + 1);
	}

	void pushScope() {
		numVarInScope.add(0);
	}

	void popScope() {
		int num = numVarInScope.pop();
		for (int i = 0; i < num; i++) {
			environment.pop();
		}
	}

	VariableInfo getVariable(String name) {
		Iterator<VariableInfo> iter = environment.descendingIterator();
		while (iter.hasNext()) {
			VariableInfo var = iter.next();
			if (var.name.equals(name)) {
				return var;
			}
		}
		Info.panic("Unknown variable: " + name);
		return null;
	}
}

