import spiglet.parser.*;
import spiglet.syntaxtree.*;
import spiglet.visitor.*;
import java.util.*;

///// RIG



////////////////////////////////////////////////////////////////

abstract class SimpleExp {
	abstract public String getName();
}

// Label or Literal
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
	int alloc = -1;

	private static int tCounter = 10000000;

	static TempReg newTemp() {
		tCounter += 1;
		return new TempReg(tCounter);
	}

	private int num;

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

	static public void clear() {
		cached.clear();
	}

	static TempReg newT(Temp n) {
		String i = n.f1.f0.tokenImage;
		return init(Integer.parseInt(i));
	}

	static TempReg newT(int num) {
		return init(num);
	}

	static TempReg newT(String name) {
		for (int i = 0; i < NUM_REG; i++) {
			if (REG_NAME[i].equals(name)) {
				TempReg r = newTemp();
				r.alloc = i;
				return r;
			}
		}
		Info.panic("newT() falied for " + name);
		return null;
	}

	private TempReg(int num) {
		this.num = num;
	}

	public boolean equals(Object o) {
		if (!(o instanceof TempReg)) {
			return false;
		}
		if (this.num == ((TempReg) o).num) {
			if (o != this) {
				Info.panic("cache internel error");
			}
			return true;
		}
		return false;
	}

	int getNum() {
		int num = this.num;
		if (num >= 10000000) {
			num = -(num - 10000000);
		}
		return num;
	}

	public String getName() {
		int num = getNum();
		if (alloc == -1) {
			return "/*" + num + "*/";
		} else {
			return REG_NAME[alloc] + "/*" + num + "*/";
		}
	}
}

class ProcBlock {
	int numOfParams;
	int stackSize;
	int numCallSlots;
	String name;
	Map<String, Instruction> label2instr;
	Instruction dummyFirst;
	Instruction dummyLast;

	ProcBlock(String name) {
		this.name = name;
		label2instr = new HashMap<String, Instruction>();
		dummyFirst = new InstrNoop();
		dummyLast = new InstrNoop();
		dummyLast.prev = dummyFirst;
		dummyFirst.next = dummyLast;
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
		for (Instruction i = dummyFirst.next; i != dummyLast; i = i.next) {
			if (i.label != null) {
				e.emitBuf(i.label);
			}
			i.emitLiveness(e);
		}
		e.emitClose("END");
	}

	void analyzeJump() {
		for (Instruction i = dummyFirst.next; i != dummyLast; i = i.next) {
			i.analyzeJump(label2instr);
		}
	}

	void analyzeLiveness() {
		boolean updated = true;
		while (updated) {
			updated = false;
			for (Instruction i = dummyLast.prev; i != dummyFirst; i = i.prev) {
				updated = i.analyzeLiveness();
			}	
		}
	}

	void analyze() {
		analyzeJump();
		analyzeLiveness();
	}
}

// in = use + (out - def)
// out = sum_succ ( succ.in )
class Instruction {
	Set<TempReg> def;
	Set<TempReg> use;
	Set<TempReg> in;
	Set<TempReg> out;

	private boolean analyzeLivenessUpdate(Instruction next) {
		boolean updated = false;
		for (TempReg r : next.in) {
			if (!def.contains(r)) {
				if (!in.contains(r)) {
					in.add(r);
					updated = true;
				}
			}
			if (!out.contains(r)) {
				out.add(r);
				updated = true;
			}
		}
		return updated;
	}

	boolean analyzeLiveness() {
		boolean updated = false;
		updated |= analyzeLivenessUpdate(next);
		if (jump != null) {
			updated |= analyzeLivenessUpdate(jump);
		}
		for (TempReg r : use) {
			if (!in.contains(r)) {
				in.add(r);
				updated = true;
			}
		}
		return updated;
	}

	Instruction() {
		def = new HashSet<TempReg>();
		use = new HashSet<TempReg>();
		in = new HashSet<TempReg>();
		out = new HashSet<TempReg>();
	}

	String label;
	Instruction prev;
	Instruction next;

	// only meaningful when it's a jump instruction
	Instruction jump;

	void analyzeJump(Map<String, Instruction> label2instr) {
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

	void emitLiveness(Emitter e) {
		e.emitBuf("//in:");
		for (TempReg r : in) {
			e.emitBuf(Integer.toString(r.getNum()));
		}
		emit(e);
		e.emitBuf("//out:");
		for (TempReg r : out) {
			e.emitBuf(Integer.toString(r.getNum()));
		}
		e.emit("//----------------------------------------------");
	}

	void emit(Emitter e) {
		Info.panic("Not implemented");
	}
}

//  the AST for kanga

/* Exp = HAllocate | BinOp | SimpleExp
 * HAllocate = HALLOCATE SimpleExp
 * BinOp = op Reg SimpleExp
 */
class KangaExp {
	// BinOp: < + - *
	// HAllocate: a
	SimpleExp r1, r2;
	String operator;

	KangaExp(SimpleExp simple) {
		this.r1 = simple;
	}

	KangaExp(SimpleExp r1, String operator, SimpleExp r2) {
		this.r1 = r1;
		this.operator = operator;
		this.r2 = r2;
	}

	public String toString() {
		if (operator == null) {
			return r1.getName();
		}
		if ("HALLOCATE".equals(operator)) {
			return "HALLOCATE " + r1.getName();
		}
		return operator + " " + r1.getName() + " " + r2.getName();
	}
}

class InstrMove extends Instruction {
	TempReg r;
	KangaExp exp;

	InstrMove(TempReg r, KangaExp exp) {
		this.r = r;
		this.exp = exp;

		def.add(r);
		if (exp.r1 instanceof TempReg) {
			use.add((TempReg) exp.r1);
		}
		if (exp.r2 instanceof TempReg) {
			use.add((TempReg) exp.r2);
		}
	}

	void emit(Emitter e) {
		e.emit("MOVE", r.getName(), exp.toString());
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

/* JUMP L1
 * CJUMP R1 L1
 */
class InstrJump extends Instruction {
	// it's a unconditional jump iff t == null
	TempReg t;
	String l;

	InstrJump(TempReg t, String l) {
		this.t = t;
		this.l = l;
	}

	void analyzeJump(Map<String, Instruction> label2instr) {
		Instruction j = label2instr.get(l);
		if (j == null) {
			Info.panic("analyzeJump failed, can not find " + l);
		}
		jump = j;
	}

	void emit(Emitter e) {
		if (t == null) {
			e.emit("JUMP", l);
		} else {
			e.emit("CJUMP", t.getName(), l);
		}
	}
}

/* HSTORE R1 integer R2
 */
class InstrStore extends Instruction {
	TempReg base;
	String off;
	TempReg val;

	InstrStore(TempReg base, String off, TempReg val) {
		this.base = base;
		this.off = off;
		this.val = val;

		use.add(base);
		use.add(val);
	}

	void emit(Emitter e) {
		e.emit("HSTORE", base.getName(), off, val.getName());
	}
}

/* HLOAD R1 R2 integer
 */
class InstrLoad extends Instruction {
	TempReg reg;
	TempReg base;
	String off;

	InstrLoad(TempReg reg, TempReg base, String off) {
		this.reg = reg;
		this.base = base;
		this.off = off;

		use.add(base);
		def.add(reg);
	}

	void emit(Emitter e) {
		e.emit("HLOAD", reg.getName(), base.getName(), off);
	}
}

/* ALOAD R1 SPILLEDARG integer */
class InstrALoad extends Instruction {
	TempReg reg;
	int os;

	InstrALoad(TempReg reg, int os) {
		this.reg = reg;
		this.os = os;
	}

	void emit(Emitter e) {
		e.emit("ALOAD", reg.getName(), "SPILLEDARG", Integer.toString(os));
	}
}

/* ASTORE SPILLEDARG integer R1 */
class InstrAStore extends Instruction {
	int os;
	TempReg reg;

	InstrAStore(int os, TempReg reg) {
		this.os = os;
		this.reg = reg;
	}

	void emit(Emitter e) {
		e.emit("ASTORE", "SPILLEDARG", Integer.toString(os), reg.getName());
	}
}

class InstrCall extends Instruction {
	SimpleExp simple;

	InstrCall(SimpleExp simple) {
		this.simple = simple;
	}

	void emit(Emitter e) {
		e.emit("CALL", simple.toString());
	}
}

class InstrPrint extends Instruction {
	SimpleExp simple;

	InstrPrint(SimpleExp simple) {
		this.simple = simple;
	}

	void emit(Emitter e) {
		e.emit("PRINT", simple.toString());
	}
}

class InstrPass extends Instruction {
	int os;
	TempReg reg;

	InstrPass(int os, TempReg reg) {
		this.os = os;
		this.reg = reg;
	}

	void emit(Emitter e) {
		e.emit("PASSARG", Integer.toString(os), reg.getName());
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

class GetExpVisitor extends GJNoArguDepthFirst<KangaExp> {
	public KangaExp visit(Exp n) {
		return n.f0.accept(this);
	}

	public KangaExp visit(HAllocate n) {
		SimpleExp simple = n.f1.accept(new GetSimpleVisitor());
		return new KangaExp(simple, "HALLOCATE", null);
	}

	public KangaExp visit(BinOp n) {
		SimpleExp o1 = n.f1.accept(new GetSimpleVisitor());
		SimpleExp o2 = n.f2.accept(new GetSimpleVisitor());
		String op = ((NodeToken) (n.f0.f0.choice)).tokenImage;
		return new KangaExp(o1, op, o2);
	}

	public KangaExp visit(spiglet.syntaxtree.SimpleExp n) {
		SimpleExp o1 = n.accept(new GetSimpleVisitor());
		return new KangaExp(o1);
	}

	public KangaExp visit(Call n) {
		return new KangaExp(TempReg.newT("v0"));
	}
}

class SpigletVisitor extends DepthFirstVisitor {
	ProcBlock selfProc;
	Emitter e;

	void emit() {
		selfProc.analyze();
		selfProc.emit(e);
		TempReg.clear();
	}

	SpigletVisitor(Emitter e) {
		this.e = e;
	}

	public void visit(Goal n) {
		selfProc = new ProcBlock("MAIN");
		n.f1.accept(this);
		emit();
		n.f3.accept(this);
	}

	public void visit(Procedure n) {
		String name = n.f0.f0.tokenImage;
		selfProc = new ProcBlock(name);
		selfProc.numOfParams = Integer.parseInt(n.f2.f0.tokenImage);
		n.f4.accept(this);
		emit();
	}

	public void visit(Label n) {
		String name = n.f0.tokenImage;
		selfProc.label2instr.put(name, selfProc.lastInstr());
		selfProc.lastInstr().label = name;
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
		TempReg t = TempReg.newT(n.f1);
		String l = n.f2.f0.tokenImage;
		selfProc.newInstr(new InstrJump(t, l));
	}

	public void visit(JumpStmt n) {
		String l = n.f1.f0.tokenImage;
		selfProc.newInstr(new InstrJump(null, l));
	}

	public void visit(HStoreStmt n) {
		TempReg r1 = TempReg.newT(n.f1);
		String l = n.f2.f0.tokenImage;
		TempReg r2 = TempReg.newT(n.f3);
		selfProc.newInstr(new InstrStore(r1, l, r2));
	}

	public void visit(HLoadStmt n) {
		TempReg r1 = TempReg.newT(n.f1);
		TempReg r2 = TempReg.newT(n.f2);
		String l = n.f3.f0.tokenImage;
		selfProc.newInstr(new InstrLoad(r1, r2, l));
	}

	public void visit(MoveStmt n) {
		TempReg r1 = TempReg.newT(n.f1);
		KangaExp e = n.f2.accept(new GetExpVisitor());
		selfProc.newInstr(new InstrMove(r1, e));
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
		n.f1.accept(this);
		SimpleExp ret = n.f3.accept(new GetSimpleVisitor());
		KangaExp e = new KangaExp(ret);
		selfProc.newInstr(new InstrMove(TempReg.newT("v0"), e));
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


}

public class S2K {

	public static void main(String[] args) {
		SpigletParser parser = new SpigletParser(System.in);
		Node root = null;
		try {
			root = parser.Goal();
		} catch (ParseException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		Emitter e = new Emitter(false);
		root.accept(new SpigletVisitor(e));
	}

}
