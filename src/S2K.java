import spiglet.parser.*;
import spiglet.syntaxtree.*;
import spiglet.visitor.*;
import java.util.*;

///// RIG

class RIG {
	static final int MAX_REG = TempReg.NUM_REG;

	static class Node {
		TempReg reg;
		int color = -1;

		Node alias; // disjoint set union 

		Set<Node> repel = new HashSet<Node>();
		int freeze;
		boolean deleted;

		boolean notFreezed() {
			if (deleted) {
				return false;
			}
			if (freeze > 0) {
				return false;
			}
			return color == -1;
		}

		Node(TempReg reg) {
			this.reg = reg;
			if (reg.isPrealloc()) {
				color = reg.alloc;
			}
		}
	}

	Map<TempReg, Node> nodeMap = new HashMap<TempReg, Node>();

	Deque<Node> stack = new ArrayDeque<Node>();

	Node lookupNode(TempReg reg) {
		Node node = nodeMap.get(reg);
		if (node == null) {
			node = new Node(reg);
		}
		nodeMap.put(reg, node);
		return node;
	}

	void removeNode(Node n) {
		if (n.freeze > 0) {
			Info.panic("never reach here");
		}

		for (Node neighbor : n.repel) {
			neighbor.repel.remove(n);
		}
		n.deleted = true;
		stack.push(n);
	}

	static class MoveRelated {
		Node n1, n2;

		MoveRelated(Node n1, Node n2) {
			this.n1 = n1;
			this.n2 = n2;
		}

		public String toString() {
			return n1.reg.getName() + "--" + n2.reg.getName();
		}

		void update() {
			while (n1.alias != null) {
				n1 = n1.alias;
			}
			while (n2.alias != null) {
				n2 = n2.alias;
			}
		}
	}

	void removeRelated(MoveRelated m) {
		m.update();
		if (m.n1 == m.n2) {
			return;
		}
		if (m.n2.color != -1) {
			removeRelated(new MoveRelated(m.n2, m.n1));
			return;
		}
		// otherwise, a temperal variable that is created for managing spilling
		// may be spilled again. This may cause a dead loop;
		if (m.n1.reg.isTemperal() && !m.n2.reg.isSpecial()) {
			removeRelated(new MoveRelated(m.n2, m.n1));
			return;
		}
		m.n2.alias = m.n1;
		m.n1.freeze += m.n2.freeze - 2;
		m.n2.freeze = 0;
		removeNode(m.n2);
		for (Node neigh : m.n2.repel) {
			neigh.repel.add(m.n1);
			m.n1.repel.add(neigh);
		}
	}

	List<MoveRelated> moveRelated = new ArrayList<MoveRelated>();

	void addInterference(TempReg r1, TempReg r2) {
		if (r1 == r2) {
			Info.panic("addInterference()");
		}
		Node n1 = lookupNode(r1);
		Node n2 = lookupNode(r2);
		n1.repel.add(n2);
		n2.repel.add(n1);
	}

	void addMove(TempReg r1, TempReg r2) {
		Node n1 = lookupNode(r1);
		Node n2 = lookupNode(r2);
		n1.freeze += 1;
		n2.freeze += 1;
		moveRelated.add(new MoveRelated(n1, n2));
	}

	Node findInsignificantNode() {
		Node rv = null;
		for (Map.Entry<TempReg, Node> entry : nodeMap.entrySet()) {
			Node n = entry.getValue();
			if (n.notFreezed() && n.repel.size() < MAX_REG) {
				return n;
			}
		}
		return rv;
	}

	Node findSignificantNode() {
		Node rv = null;
		for (Map.Entry<TempReg, Node> entry : nodeMap.entrySet()) {
			Node n = entry.getValue();
			if (n.notFreezed()) {
				return n;
			}
		}
		return rv;
	}

	// Briggs: Nodes a and b can be coalesced if the resulting node ab 
	// will have fewer than K neighbors of significant degree
	int neighborMoveRelated(MoveRelated m) {
		int rv = m.n1.repel.size() + m.n2.repel.size();
		for (Node n : m.n1.repel) {
			if (m.n2.repel.contains(n)) {
				rv -= 1;
			}
		}
		return rv;
	}

	// George: Nodes a and b can be coalesced if, for every neighbor t of 
	// a, either t already interferes with b or t is of insignificant degree. 
	boolean moveGeorge(Node a, Node b) {
		for (Node t : a.repel) {
			if (! ( b.repel.contains(t) || t.repel.size() < MAX_REG  )) {
				return false;
			}
		}
		return true;
	}

	MoveRelated findMoveRelated() {
		for (MoveRelated m : moveRelated) {
			if (neighborMoveRelated(m) < MAX_REG) {
				return m;
			}
		}

		for (MoveRelated m : moveRelated) {
			if (moveGeorge(m.n1, m.n2)) {
				return m;
			}
		}
		return null;
	}

	boolean canColor(Node n, int c) {
		for (Node neighbor : n.repel) {
			if (neighbor.color == c) {
				return false;
			}
		}
		return true;
	}

	boolean color(Node n) {
		if (n.alias != null) {
			n.color = n.alias.color;
			return true;
		}

		for (int i = 0; i < MAX_REG; i++) {
			if (canColor(n, i)) {
				n.color = i;
				return true;
			}
		}
		return false;
	}

	void paintColor() {
		for (Map.Entry<TempReg, Node> entry : nodeMap.entrySet()) {
			Node n = entry.getValue();
			if (!n.reg.isPrealloc()) {
				n.reg.alloc = n.color;
			}
		}
	}

	// return null if successfully colored the graph
	// otherwise, return the spilled temperal variable
	TempReg main() {
		while (true) {
			Node n = findInsignificantNode();
			if (n != null) {
				Info.dump("remove insignificant node");
				removeNode(n);
				continue;
			}

			ListIterator<MoveRelated> iter = moveRelated.listIterator();
			while(iter.hasNext()) {
				MoveRelated m = iter.next();
				m.update();
				if (m.n1.repel.contains(m.n2)) {
					m.n1.freeze -= 1;
					m.n2.freeze -= 1;
					iter.remove();
				}
			}

			MoveRelated m = findMoveRelated();
			if (m != null) {
				Info.dump("coalesce move-related nodes " + m);
				removeRelated(m);
				moveRelated.remove(m);
				continue;
			}

			if (!moveRelated.isEmpty()) {
				Info.dump("unfreeze move-related nodes");
				m = moveRelated.get(0);
				m.update();
				m.n1.freeze -= 1;
				m.n2.freeze -= 1;
				moveRelated.remove(0);
				continue;
			}

			n = findSignificantNode();
			if (n != null) {
				Info.dump("remove significant node");
				removeNode(n);
				continue;
			}

			break;
		}
		Info.dump("retrace stack");

		while (! stack.isEmpty()) {
			Node n = stack.pop();
			if (!color(n)) {
				return n.reg;
			}
			// n.reg.alloc = n.color;
		}
		return null;
	}
}

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
		+ 8 /* s0 - s7 */
		+ 4 /* a0 - a3 */
		+ 2 /* v0 - v1 */
		+ 10 /* t0 - t9 */;
	static final String[] REG_NAME = {
		"s0", "s1", "s2", "s3", "s4", "s5", "s6", "s7",
		"a0", "a1", "a2", "a3",
		"v0", "v1",
		"t0", "t1", "t2", "t3", "t4", "t5", "t6", "t7", "t8", "t9"
	};
	int alloc = -1;

	static final int SPECIAL_REG = 10000000;
	private static int tCounter = SPECIAL_REG + NUM_REG;

	boolean isPrealloc() {
		return SPECIAL_REG <= num && num < SPECIAL_REG + NUM_REG;
	}

	boolean isSpecial() {
		return num >= SPECIAL_REG;
	}

	boolean isTemperal() {
		return num >= SPECIAL_REG + NUM_REG;
	}

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

	static TempReg getSpecial(int num) {
		return newT(REG_NAME[num]);
	}

	static TempReg newT(String name) {
		for (int i = 0; i < NUM_REG; i++) {
			if (REG_NAME[i].equals(name)) {
				TempReg r = newT(SPECIAL_REG + i);
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
		if (num >= SPECIAL_REG) {
			num = -(num - SPECIAL_REG);
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
			if (!i.isNoop()) {
				i.emitLiveness(e);
			} else {
				e.emit("NOOP");
			}
		}
		e.emitClose("END");
	}

	void analyzeJump() {
		for (Instruction i = dummyFirst.next; i != dummyLast; i = i.next) {
			i.analyzeJump(label2instr);
		}
	}

	void analyzeLiveness() {
		for (Instruction i = dummyFirst.next; i != dummyLast; i = i.next) {
			i.in.clear();
			i.out.clear();
		}
		boolean updated = true;
		while (updated) {
			updated = false;
			for (Instruction i = dummyLast.prev; i != dummyFirst; i = i.prev) {
				updated = i.analyzeLiveness();
			}	
		}
	}

	/*
		x = y + z;
	=>  t1 = y + z;
	    ASTORE t1
	*/
	/*
		x = x + z;
	=>  t1 = ALOAD 
	    t1 = t1 + z;
	    ASTORE t1
	*/
	/*
		y = x;
	=>  t1 = ALOAD
		y = t1
	*/
	
	void spillRegister(TempReg spilled) {
		if (spilled.isPrealloc()) {
			Info.panic("pre-allocated register can not be spilled");
		}
		int os = stackSize;
		stackSize += 1;
		for (Instruction i = dummyFirst.next; i != dummyLast; i = i.next) {
			if (i.use.contains(spilled) || i.def.contains(spilled)) {
				TempReg t1 = TempReg.newTemp();
				Instruction j = i.replace(spilled, t1);
				j.insertBefore(i);

				if (i.use.contains(spilled)) {
					(new InstrALoad(t1, os)).insertBefore(j);
				}
				if (i.def.contains(spilled)) {
					(new InstrAStore(os, t1)).insertAfter(j);
				}

				i.remove();
			}
		}
	}

	RIG rig;

	// return null on success
	// else return hint for the spilled register
	TempReg analyzeInterference() {
		rig = new RIG();
		for (Instruction i = dummyFirst.next; i != dummyLast; i = i.next) {
			for (TempReg x : i.in) {
				for (TempReg y : i.in) {
					if (x != y) {
						rig.addInterference(x, y);
					}
				}
			}
			for (TempReg x : i.out) {
				for (TempReg y : i.out) {
					if (x != y) {
						rig.addInterference(x, y);
					}
				}
			}
		}

		for (Instruction i = dummyFirst.next; i != dummyLast; i = i.next) {
			if (i instanceof InstrMove) {
				InstrMove i2 = (InstrMove) i;
				TempReg t = i2.r;
				TempReg s = i2.exp.TempRegOnly();
				if (s != null) {
					rig.addMove(t, s);
				}
			}
		}

		// pre-allocated registers
		for (int i = 0; i < TempReg.NUM_REG; i++) {
			for (int j = i + 1; j < TempReg.NUM_REG; j++) {
				TempReg ri = TempReg.getSpecial(i);
				TempReg rj = TempReg.getSpecial(j);
				rig.addInterference(ri, rj);
			}
		}

		// caller-saved registers
		for (Instruction i = dummyFirst.next; i != dummyLast; i = i.next) {
			if (i instanceof InstrCall) {
				for (TempReg r : i.in) {
					if (i.out.contains(r) && !i.def.contains(r)) {
						for (int j = TempReg.CALLEE_SAVED_REG; j < TempReg.NUM_REG; j++) {
							rig.addInterference(r, TempReg.getSpecial(j));
						}
					}
				}
			}
		}

		TempReg t = rig.main();
		return t;
	}

	Instruction newMove(TempReg t, TempReg s) {
		return new InstrMove(t, new KangaExp(s));
	}

	void analyzeCalleeSavedRegisters() {
		for (int i = 0; i < TempReg.CALLEE_SAVED_REG; i++) {
			TempReg r = TempReg.getSpecial(i);
			TempReg t = TempReg.newTemp();
			Instruction instr;
			instr = newMove(t, r);
			instr.insertAfter(dummyFirst);
			instr = newMove(r, t);
			if ((dummyLast.prev instanceof InstrMove) && 
				((InstrMove) dummyLast.prev).r == TempReg.newT("v0")) {
				instr.insertBefore(dummyLast.prev);
			} else {
				instr.insertBefore(dummyLast);
			}
		}
	}

	void analyzeNoop() {
		for (Instruction i = dummyFirst.next; i != dummyLast; i = i.next) {
			if (i.isNoop()) {
				if (i.label == null) {
					i.remove();
				} else if (i.next != dummyLast && i.next.label == null) {
					i.next.label = i.label;
					i.remove();
				} else if (i.next == dummyLast && i.next.label != null) {
					// TODO: two entry points rendezvous
					// merge them!
				}
			}
		}
	}

	void analyze() {
		stackSize = numOfParams;
		analyzeJump();
		analyzeCalleeSavedRegisters();
		for (int t = 0; t < 32; t++) {
		// while (true) {
			analyzeLiveness();
			TempReg spilled = analyzeInterference();
			if (spilled == null) {
				rig.paintColor();
				analyzeNoop();
				return;
			}
			emit(new Emitter(Info.DEBUG == false));
			Info.dump("spilled: "  + spilled.getName());
			spillRegister(spilled);
		}
		Info.panic("timeout");
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
		if (Info.DEBUG == false) {
			emit(e);
			return;
		}

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

	Instruction replace(TempReg s, TempReg t) {
		Info.panic("Not implemented");
		return null;
	}

	boolean isNoop() {
		return false;
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

	TempReg TempRegOnly() {
		if (operator != null) {
			return null;
		}
		if (!(r1 instanceof TempReg)) {
			return null;
		}
		return (TempReg) r1;
	}

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

	KangaExp replace(TempReg s, TempReg t) {
		return new KangaExp(r1 == s ? t : r1, operator, r2 == s ? t : r2);
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

	boolean isNoop() {
		if (!r.isPrealloc() && !out.contains(r)) {
			return true;
		}
		return exp.TempRegOnly() != null && ((TempReg) exp.r1).alloc == r.alloc;
	}

	void emit(Emitter e) {
		e.emit("MOVE", r.getName(), exp.toString());
	}

	Instruction replace(TempReg s, TempReg t) {
		return new InstrMove(s == r ? t : r, exp.replace(s, t));
	}
}

class InstrNoop extends Instruction {
	void emit(Emitter e) {
		e.emit("NOOP");
	}

	boolean isNoop() {
		return true;
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
		if (t != null) {
			use.add(t);
		}
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

	Instruction replace(TempReg s, TempReg t) {
		return new InstrJump(s == this.t ? t : this.t, l);
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

	Instruction replace(TempReg s, TempReg t) {
		return new InstrStore(
			s == this.base ? t : this.base, 
			off,
			s == this.val ? t : this.val);
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

	boolean isNoop() {
		if (!reg.isPrealloc() && !out.contains(reg)) {
			return true;
		}
		return false;
	}

	Instruction replace(TempReg s, TempReg t) {
		return new InstrLoad(
			s == this.reg ? t : this.reg, 
			s == this.base ? t : this.base,
			off);
	}
}

/* ALOAD R1 SPILLEDARG integer */
class InstrALoad extends Instruction {
	TempReg reg;
	int os;

	InstrALoad(TempReg reg, int os) {
		this.reg = reg;
		this.os = os;

		def.add(reg);
	}

	void emit(Emitter e) {
		e.emit("ALOAD", reg.getName(), "SPILLEDARG", Integer.toString(os));
	}

	boolean isNoop() {
		if (!reg.isPrealloc() && !out.contains(reg)) {
			return true;
		}
		return false;
	}
	
	Instruction replace(TempReg s, TempReg t) {
		return new InstrALoad(
			s == this.reg ? t : this.reg, 
			os);
	}
}

/* ASTORE SPILLEDARG integer R1 */
class InstrAStore extends Instruction {
	int os;
	TempReg reg;

	InstrAStore(int os, TempReg reg) {
		this.os = os;
		this.reg = reg;

		use.add(reg);
	}

	void emit(Emitter e) {
		e.emit("ASTORE", "SPILLEDARG", Integer.toString(os), reg.getName());
	}

	Instruction replace(TempReg s, TempReg t) {
		return new InstrAStore(
			os,
			s == this.reg ? t : this.reg);
	}
}

class InstrCall extends Instruction {
	SimpleExp simple;
	int numOfArgs;

	InstrCall(SimpleExp simple, int numOfArgs) {
		this.simple = simple;
		this.numOfArgs = numOfArgs;
		
		if (simple instanceof TempReg) {
			use.add((TempReg) simple);
		}

		for (int i = 0; i < Math.min(4, numOfArgs); i++) {
			TempReg r = TempReg.newT("a" + i);
			use.add(r);
		}
		def.add(TempReg.newT("v0"));
	}

	void emit(Emitter e) {
		e.emit("CALL", simple.getName());
	}

	Instruction replace(TempReg s, TempReg t) {
		if (!(simple instanceof TempReg)) {
			return new InstrCall(simple, numOfArgs);
		}
		TempReg r = (TempReg) simple;
		return new InstrCall(
			r == s ? t : r,
			numOfArgs);
	}
}

class InstrPrint extends Instruction {
	SimpleExp simple;

	InstrPrint(SimpleExp simple) {
		this.simple = simple;

		if (simple instanceof TempReg) {
			use.add((TempReg) simple);
		}
	}

	void emit(Emitter e) {
		e.emit("PRINT", simple.getName());
	}

	Instruction replace(TempReg s, TempReg t) {
		if (!(simple instanceof TempReg)) {
			return new InstrPrint(simple);
		}
		TempReg r = (TempReg) simple;
		return new InstrPrint(
			r == s ? t : r
			);
	}
}

class InstrPassArg extends Instruction {
	int os;
	TempReg reg;

	InstrPassArg(int os, TempReg reg) {
		this.os = os;
		this.reg = reg;

		use.add(reg);
	}

	void emit(Emitter e) {
		e.emit("PASSARG", Integer.toString(os), reg.getName());
	}

	Instruction replace(TempReg s, TempReg t) {
		return new InstrPassArg(
			os,
			s == this.reg ? t : this.reg);
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
	ProcBlock selfProc;

	GetExpVisitor(ProcBlock selfProc) {
		this.selfProc = selfProc;
	}

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

	int i = 0;

	public KangaExp visit(Temp n) {
		TempReg r = (TempReg) n.accept(new GetSimpleVisitor());
		KangaExp e = new KangaExp(r);
		if (i < 4) {
			selfProc.newInstr(new InstrMove(TempReg.newT("a" + i), e));
		} else {
			selfProc.newInstr(new InstrPassArg(i - 3, r));
		}

		i += 1;
		if (i > selfProc.numCallSlots) {
			selfProc.numCallSlots = i;
		}
		return null;
	}

	public KangaExp visit(Call n) {
		SimpleExp o1 = n.f1.accept(new GetSimpleVisitor());
		n.f3.accept(this);
		selfProc.newInstr(new InstrCall(o1, i));
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
		for (int i = 0; i < Math.min(selfProc.numOfParams, 4); i++) {
			KangaExp e = new KangaExp(TempReg.newT("a" + i));
			selfProc.newInstr(new InstrMove(TempReg.newT(i), e));
		}
		for (int i = 4; i < selfProc.numOfParams; i++) {
			selfProc.newInstr(new InstrALoad(TempReg.newT(i), i - 4));
		}
		n.f4.accept(this);
		emit();
	}

	public void visit(Label n) {
		selfProc.newInstr(new InstrNoop());
		String name = n.f0.tokenImage;
		selfProc.label2instr.put(name, selfProc.lastInstr());
		selfProc.lastInstr().label = name;
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
		KangaExp e = n.f2.accept(new GetExpVisitor(selfProc));
		selfProc.newInstr(new InstrMove(r1, e));
	}

	public void visit(PrintStmt n) {
		SimpleExp exp = n.f1.accept(new GetSimpleVisitor());
		selfProc.newInstr(new InstrPrint(exp));
	}

	// the return value of the statement
	public void visit(StmtExp n) {
		n.f1.accept(this);
		SimpleExp ret = n.f3.accept(new GetSimpleVisitor());
		KangaExp e = new KangaExp(ret);
		selfProc.newInstr(new InstrMove(TempReg.newT("v0"), e));
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
