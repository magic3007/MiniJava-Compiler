import spiglet.parser.*;
import spiglet.syntaxtree.*;
import spiglet.visitor.*;
import java.util.*;

class SpigletAST {}

abstract class SimpleExp {
	abstract public String getName();
}

class SimpleStringExp extends SimpleExp {
	String name;

	SimpleStringExp(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
}

class TempReg extends SimpleExp {
	static final int CALLEE_SAVED_REG = 8;
	static final int NUM_REG = 
		CALLEE_SAVED_REG 
		+ 10 /* t0 - t9 */
		+ 4 /* a0 - a3 */
		+ 2 /* v0 - v1 */;
	static final String[] REG_NAME = {
		"s0", "s1", "s2", "s3", "s4", "s5", "s6", "s7",
		"t0", "t1", "t2", "t3", "t4", "t5", "t6", "t7", "t8", "t9",
		"a0", "a1", "a2", "a3",
		"v0", "v1"
	};

	private static int tCounter = 10000000;

	static TempReg newTemp() {
		tCounter += 1;
		return new TempReg(tCounter);
	}

	int num;

	static Map<Integer, TempReg> cached =
		new HashMap<Integer, TempReg>();

	static private TempReg init(int i) {
		TempReg r = cached.get(i);
		if (r != null) {
			return r;
		}
		r = new TempReg(i);
		cached.put(i, r);
		return r;
	}

	static TempReg newT(Temp n) {
		String i = n.f1.f0.tokenImage;
		return init(Integer.parseInt(i));
	}

	static TempReg newT(int num) {
		return init(num);
	}

	private TempReg(int num) {
		this.num = num;
	}

	public boolean equals(Object o) {
		if (!(o instanceof TempReg)) {
			return false;
		}
		return this.num == ((TempReg) o).num;
	}

	public String getName() {
		return "TEMP " + num;
	}
}

// in = use + (out - def)
// out = sum_succ ( succ.in )
class Instruction {
	Set<TempReg> def;
	Set<TempReg> use;
	Set<TempReg> in;
	Set<TempReg> out;

	Instruction() {
		def = new HashSet<TempReg>();
		use = new HashSet<TempReg>();
		in = new HashSet<TempReg>();
		out = new HashSet<TempReg>();
	}

	Instruction prev;
	Instruction next;

	// only meaningful when it's a jump instruction
	Instruction jump;

	void analyzeJump() {
		// the 'jump' subclasses override this method
	}

	void remove() {
		prev.next = next;
		next.prev = prev;
	}

	void insertBefore(Instruction instr) {
		prev = instr.prev;
		next = instr;
		instr.prev.next = this;
		instr.prev = this;
	}

	void insertAfter(Instruction instr) {
		prev = instr;
		next = instr.next;
		instr.next.prev = this;
		instr.next = this;
	}

	void emit(Emitter e) {
		Info.panic("Not implemented");
	}
}

class InstrNoop extends Instruction {
	void emit(Emitter e) {
		e.emit("NOOP");
	}
}

class InstrError extends Instruction {
	void emit(Emitter e) {
		e.emit("ERROR");
	}
}

class InstrJump extends Instruction {
	// it's a unconditional jump iff t == null
	TempReg t;
	String l;

	InstrJump(TempReg t, String l) {
		this.t = t;
		this.l = l;
	}

	void emit(Emitter e) {
		if (t == null) {
			e.emit("JUMP", l);
		} else {
			e.emit("CJUMP", t.getName(), l);
		}
	}
}

class InstrStore extends Instruction {
	TempReg base;
	String off;
	TempReg val;

	InstrStore(TempReg base, String off, TempReg val) {
		this.base = base;
		this.off = off;
		this.val = val;
	}

	void emit(Emitter e) {
		e.emit("HSTORE", base.getName(), off, val.getName());
	}
}

class InstrLoad extends Instruction {
	TempReg reg;
	TempReg base;
	String off;

	InstrLoad(TempReg reg, TempReg base, String off) {
		this.reg = reg;
		this.base = base;
		this.off = off;
	}

	void emit(Emitter e) {
		e.emit("HSTORE", reg.getName(), base.getName(), off);
	}
}

class InstrALoad extends Instruction {
	TempReg reg;
	int os;

	InstrALoad(TempReg reg, int os) {
		this.reg = reg;
		this.os = os;
	}

	void emit(Emitter e) {
		e.emit("ALOAD", reg.getName(), Integer.toString(os));
	}
}

class Procedure {
	int numOfParams;
	int stackSize;
	int numCallSlots;
	String name;
	Map<String, Instruction> label2instr;
	Instruction dummyFirst;
	Instruction dummyLast;
	SimpleExp retval;

	Procedure(String name) {
		this.name = name;
		label2instr = new HashMap<String, Instruction>();
		dummyFirst = new InstrNoop();
		dummyLast = new InstrNoop();
	}

	void newInstr(Instruction instr) {
		instr.insertBefore(dummyLast);
	}

	Instruction lastInstr() {
		if (dummyFirst == dummyLast) {
			Info.panic("lastInstr()");
		}

		return dummyLast.prev;
	}

	void emit(Emitter e) {
		e.emitOpen(name,
			"[" + numOfParams + "]",
			"[" + stackSize + "]",
			"[" + numCallSlots + "]");
		for (Instruction i = dummyFirst.next; 
			i != dummyLast; i = i.next) {
			i.emit(e);
		}
		e.emit("MOVE", "v0", retval.getName());
		e.emitClose("END");
	}

	void analyzeJump() {
		for (Instruction i = dummyFirst.next;
			i != dummyLast; i = i.next) {
			i.analyzeJump();
		}
	}
}

abstract class AbstractGetSimpleVisitor<T> extends GJNoArguDepthFirst<T> {}

class GetSimpleVisitor extends GJNoArguDepthFirst<SimpleExp> {

	public SimpleExp visit(spiglet.syntaxtree.SimpleExp n) {
		return n.f0.accept(this);
	}

	public SimpleExp visit(Temp n) {
		int num = Integer.parseInt(n.f1.f0.tokenImage);
		return TempReg.newT(num);
	}

	public SimpleExp visit(IntegerLiteral n) {
		return new SimpleStringExp(n.f0.tokenImage);
	}

	public SimpleExp visit(Label n) {
		return new SimpleStringExp(n.f0.tokenImage);
	}
}

class SpigletVisitor extends DepthFirstVisitor {
	List<Procedure> procs;
	Procedure selfProc;

	SpigletVisitor(List<Procedure> procs) {
		this.procs = procs;
	}

	public void visit(Goal n) {
		selfProc = new Procedure("MAIN");
		n.f1.accept(this);
		procs.add(selfProc);
		n.f3.accept(this);
	}

	public void visit(Procedure n) {
		String name = n.f0.tokenImage;
		selfProc = new Procedure(name);
		selfProc.numOfParams = Integer.parseInt(n.f2.f0.tokenImage);
		n.f4.accept(this);
	}

	public void visit(Label n) {
		String name = n.f0.tokenImage;
		selfProc.label2instr.set(name, selfProc.lastIntr());
	}

	// hack NodeSequence in order to visit first the instruction
	// and then the label
	public void visit(NodeSequence n) {
		Enumeration<Node> e = n.elements();
		if (!e.hasMoreElements()) {
			Info.panic("Empty NodeSequence");
			return;
		}
		Node n1 = e.nextElement();
		if (!e.hasMoreElements()) {
			n1.accept(this);
			return;
		}
		Node n2 = e.nextElement();
		if (e.hasMoreElements()) {
			Info.panic("Too many nodes");
			return;
		}
		n2.accept(this);
		n1.accept(this);
	}

	public void visit(NoOpStmt n) {
		selfProc.newInstr(new InstrNoop());
	}

	public void visit(ErrorStmt n) {
		selfProc.newInstr(new InstrError());
	}

	public void visit(CJumpStmt n) {
		TempReg t = new TempReg(n.f1);
		String l = n.f2.f0.tokenImage;
		selfProc.newInstr(new InstrJump(t, l));
	}

	public void visit(JumpStmt n) {
		String l = n.f1.f0.tokenImage;
		selfProc.newInstr(new InstrJump(null, l));
	}

	public void visit(HStoreStmt n) {
		TempReg r1 = new TempReg(n.f1);
		String l = n.f2.f0.tokenImage;
		TempReg r2 = new TempReg(n.f3);
		selfProc.newInstr(new InstrStore(r1, l, r2));
	}

	public void visit(HLoadStmt n) {
		TempReg r1 = new TempReg(n.f1);
		TempReg r2 = new TempReg(n.f2);
		String l = n.f3.f0.tokenImage;
		selfProc.newInstr(new InstrLoad(r1, r2, l));
	}

	// only for MOVE parsing
	TempReg selfTemp;

	public void visit(MoveStmt n) {
		selfTemp = new TempReg(n.f1);
		n.f2.accept(this);
	}

   /**
    * f0 -> "PRINT"
    * f1 -> SimpleExp()
    */
   public void visit(PrintStmt n) {
      n.f0.accept(this);
      n.f1.accept(this);
   }

   /**
    * f0 -> "BEGIN"
    * f1 -> StmtList()
    * f2 -> "RETURN"
    * f3 -> SimpleExp()
    * f4 -> "END"
    */
   public void visit(StmtExp n) {
      n.f0.accept(this);
      n.f1.accept(this);
      n.f2.accept(this);
      n.f3.accept(this);
      n.f4.accept(this);
   }

   /**
    * f0 -> "CALL"
    * f1 -> SimpleExp()
    * f2 -> "("
    * f3 -> ( Temp() )*
    * f4 -> ")"
    */
   public void visit(Call n) {
      n.f0.accept(this);
      n.f1.accept(this);
      n.f2.accept(this);
      n.f3.accept(this);
      n.f4.accept(this);
   }

   /**
    * f0 -> "HALLOCATE"
    * f1 -> SimpleExp()
    */
   public void visit(HAllocate n) {
      n.f0.accept(this);
      n.f1.accept(this);
   }

   /**
    * f0 -> Operator()
    * f1 -> Temp()
    * f2 -> SimpleExp()
    */
   public void visit(BinOp n) {
      n.f0.accept(this);
      n.f1.accept(this);
      n.f2.accept(this);
   }

   /**
    * f0 -> "LT"
    *       | "PLUS"
    *       | "MINUS"
    *       | "TIMES"
    */
   public void visit(Operator n) {
      n.f0.accept(this);
   }



}

public class S2K {

	public static void main(String[] args) {
		SpigletParser parser = new SpigletParser(System.in);
		Node root = parser.Goal();
		LIst<Procedure> procedures = new ArrayList<Procedure>();
		root.visit(new SpigletVisitor(procedures));
		Emitter e = new Emitter();
		for (Procedure p : procedures) {
			p.genKanga();
		}
	}

}
