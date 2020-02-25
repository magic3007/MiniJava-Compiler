import java.io.PrintStream;
import java.util.*;

class Info {
	static final boolean DEBUG = false;

	static void dd(String s) {
		if (!DEBUG) {
			// pass
		} else {
			System.out.print(s);
		}
	}

	static void dln() {
		if (!DEBUG) {
			// pass
		} else {
			System.out.println();
		}
	}

	static void dump(String... msg) {
		if (!DEBUG) {
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
}

class Emitter {
	private int indentNum = 0;
	private List<String> buf = new LinkedList<String>();
	boolean mute = true;

	java.io.PrintStream out = System.out;

	Emitter() {
	}

	Emitter(boolean mute) {
		this.mute = mute;
	}

	void emitBuf(String... msg) {
		for (String m : msg) {
			buf.add(m);
		}
	}

	void emitFlush() {
		if (buf.size() == 0) {
			return;
		}
		String[] args = buf.toArray(new String[0]);
		buf.clear();
		emit(args);
	}

	String numToOffset(int num) {
		return Integer.toString(num * 4);
	}

	void emitOpen(String... msg) {
		emitFlush();
		emit(msg);
		indentNum += 1;
	}

	void emitClose(String... msg) {
		emitFlush();
		indentNum -= 1;
		emit(msg);
	}

	void emit(String... msg) {
		if (mute) {
			return;
		}
		if (msg.length == 0) {
			return;
		}
		if (buf.size() != 0) {
			Info.panic("emit error");
		}
		for (int i = 0; i < indentNum; i++) {
			out.print("\t");
		}
		for (String m : msg) {
			out.print(m);
			out.print(" ");
		}
		out.println();
	}

	private int labelNum = 100;

	String newLabel() {
		labelNum += 1;
		return "L" + labelNum;
	}

	private int tempNum = 200;

	String newTemp() {
		tempNum += 1;
		return "TEMP " + tempNum;
	}
}
