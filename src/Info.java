import java.util.*;

class Info {
	static final boolean DEBUG = true;

	private static void dd(String s) {
		if (! DEBUG) {
			// pass
		} else {
			System.out.print(s);
		}
	}

	private static void dln() {
		if (! DEBUG) {
			// pass
		} else {
			System.out.println();
		}
	}

	private static void dump(String... msg) {
		if (! DEBUG) {
			// pass
		} else {
			for (String m : msg) {
				dd(m);
				dd(" ");
			}
			dln();
		}
	}

	static void panic(String... msg) {
		dump(msg);
		System.out.println("Type error");
		throw new RuntimeException("DEBUG");
	}

	static void debug(String... msg) {
		dump(msg);
	}

	static private int indentNum = 0;
	static private List<String> buf = new LinkedList<String>();

	static void emitBuf(String... msg) {
		for (String m : msg) {
			buf.add(m);
		}
	}

	static void emitFlush() {
		emit(buf.toArray(new String[0]));
		buf.clear();
	}

	static void emitOpen(String... msg) {
		emitFlush();
		emit(msg);
		indentNum += 1;
	}

	static void emitClose(String... msg) {
		emitFlush();
		indentNum -= 1;
		emit(msg);
	}

	static void emit(String... msg) {
		for (int i = 0; i < indentNum; i++) {
			dd("  ");
		}
		dump(msg);
	}

	static private int labelNum = 0;

	static String newLabel() {
		labelNum += 1;
		return "L" + labelNum;
	}

	static private int tempNum = 100;

	static String newTemp() {
		tempNum += 1;
		return "TEMP " + tempNum;
	}
}
