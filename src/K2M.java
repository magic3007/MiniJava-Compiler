import java.util.*;
import java.io.*;
import kanga.parser.*;
import kanga.syntaxtree.*;
import kanga.visitor.*;


// ====================================================================

class PackedArgu{
    public int spilled_args_num;
    public String reg1 = null;
    public boolean whether_emit_label = false;
    PackedArgu(int _spilled_args_num){
        this.spilled_args_num = _spilled_args_num;
    }
    PackedArgu(String _reg1){
        this.reg1 = _reg1;
    }
}

class Kangatranslator extends GJDepthFirst<String, PackedArgu>{
    Emitter e;

    Kangatranslator(Emitter _e){
        super();
        this.e = _e;
    }
    
    public String visit(Reg n, PackedArgu argu){
        return "$" + n.f0.choice.toString();
    }

    public String visit(IntegerLiteral n, PackedArgu argu){
        return n.f0.toString();
    }

    public String visit(Label n, PackedArgu argu){
        if(argu.whether_emit_label){
            e.emitClose(n.f0.toString(), ":");
            e.emitOpen();
        }
        return n.f0.toString();
    }

    /**
    * f0 -> ( ( Label() )? Stmt() )*
    */
    public String visit(StmtList n, PackedArgu argu) {
        for(Node node : n.f0.nodes){
            argu.whether_emit_label = true;
            node.accept(this, argu);
        }
        return null;
    }

    public String visit(Stmt n, PackedArgu argu) {
        argu.whether_emit_label = false;
        n.f0.accept(this, argu);
        return null;
    }


    /**
    * f0 -> Reg()
    *       | IntegerLiteral()
    *       | Label()
    */
    public String visit(kanga.syntaxtree.SimpleExp n, PackedArgu argu){
        return n.f0.accept(this, argu);
    }

     /**
    * f0 -> "SPILLEDARG"
    * f1 -> IntegerLiteral()
    */
    public String visit(SpilledArg n, PackedArgu argu){
        int spilled_args_num = argu.spilled_args_num;
        int index = Integer.parseInt(n.f1.accept(this, argu));
        String imm = Integer.toString(4 * (spilled_args_num - index - 1));
        return imm + "($sp)";
    }

     /**
    * f0 -> "PASSARG"
    * f1 -> IntegerLiteral()
    * f2 -> Reg()
    */
    public String visit(PassArgStmt n, PackedArgu argu){
        int index = Integer.parseInt(n.f1.accept(this, argu));
        String reg = n.f2.accept(this, argu);
        String imm = Integer.toString(-(8 + 4 * index));
        e.emit("sw", reg, imm + "($sp)");
        return n.f0.toString();
    }

    /**
    * f0 -> "NOOP"
    */
    public String visit(NoOpStmt n, PackedArgu argu){
        e.emit("nop");
        return n.f0.toString();
    }

    /**
    * f0 -> "ERROR"
    */
    public String visit(ErrorStmt n, PackedArgu argu){
        e.emit("jal _error");
        return n.f0.toString();
    }

    /**
    * f0 -> "CJUMP"
    * f1 -> Reg()
    * f2 -> Label()
    */
    public String visit(CJumpStmt n, PackedArgu argu){
        String reg = n.f1.accept(this, argu);
        String label = n.f2.accept(this, argu);
        e.emit("beqz", reg, label);
        return n.f0.toString();
    }

    /**
    * f0 -> "JUMP"
    * f1 -> Label()
    */
    public String visit(JumpStmt n, PackedArgu argu){
        String label = n.f1.accept(this, argu);
        e.emit("b", label);
        return n.f0.toString();
    }

    /**
    * f0 -> "CALL"
    * f1 -> SimpleExp()
    */
    public String visit(CallStmt n, PackedArgu argu){
        if (n.f1.f0.choice instanceof Reg){
            e.emit("jalr", n.f1.accept(this, argu));
        }else{
            e.emit("jal", n.f1.accept(this, argu));
        }
        return n.f0.toString();
    }

    /**
     *  HSTORE reg1 imm reg2
     *
     * sw $reg2, imm($rg1)
     */
    public String visit(HStoreStmt n, PackedArgu argu){
        String reg1 = n.f1.accept(this,argu);
        String reg2 = n.f3.accept(this, argu);
        String imm = n.f2.f0.toString();
        e.emit("sw", reg2, imm + "(" + reg1 + ")");
        return n.f0.toString();
    }

    /**
    * f0 -> "HLOAD"
    * f1 -> Reg()
    * f2 -> Reg()
    * f3 -> IntegerLiteral()
    * 
    * lw $reg1, imm($reg2)
    */
    public String visit(HLoadStmt n, PackedArgu argu){
        String reg1 = n.f1.accept(this, argu);
        String reg2 = n.f2.accept(this,argu);
        String imm = n.f3.f0.toString();
        e.emit("lw", reg1, imm + "(" + reg2 + ")");
        return n.f0.toString();
    }

    /**
    * f0 -> "ALOAD"
    * f1 -> Reg()
    * f2 -> SpilledArg()
    */
    public String visit(ALoadStmt n, PackedArgu argu) {
        String reg = n.f1.accept(this, argu);
        String mem_access = n.f2.accept(this, argu);
        e.emit("lw", reg, mem_access);
        return n.f0.toString();
    }

    /**
    * f0 -> "ASTORE"
    * f1 -> SpilledArg()
    * f2 -> Reg()
    */
    public String visit(AStoreStmt n, PackedArgu argu){
        String reg = n.f2.accept(this, argu);
        String mem_access = n.f1.accept(this, argu);
        e.emit("sw", reg, mem_access);
        return n.f0.toString();
    }
    
    /**
    * f0 -> "PRINT"
    * f1 -> SimpleExp()
    */
    public String visit(PrintStmt n, PackedArgu argu) {
        e.emit("sw $a0 -4($sp)");
        e.emit("sw $v0 -8($sp)");
        if (n.f1.f0.choice instanceof Reg){
            e.emit("move $a0,", n.f1.accept(this, argu));
        }else if (n.f1.f0.choice instanceof Label){
            e.emit("la $a0,", n.f1.accept(this, argu));
        }else if (n.f1.f0.choice instanceof IntegerLiteral){
            e.emit("li $a0,", n.f1.accept(this, argu));
        }
        e.emit("jal _print");
        e.emit("lw $v0 -8($sp)");
        e.emit("lw $a0 -4($sp)");
        return n.f0.toString();
    }
    
    /**
    * f0 -> "HALLOCATE"
    * f1 -> SimpleExp()
    */
    public String visit(HAllocate n, PackedArgu argu){
        if (n.f1.f0.choice instanceof Reg){
            e.emit("move $a0,", n.f1.accept(this, argu));
        }else if (n.f1.f0.choice instanceof Label){
            e.emit("la $a0,", n.f1.accept(this, argu));
        }else if (n.f1.f0.choice instanceof IntegerLiteral){
            e.emit("li $a0,", n.f1.accept(this, argu));
        }
        e.emit("jal _halloc");
        return n.f0.toString();
    }

    /**
     * f0 -> "MOVE"
     * f1 -> Reg()
     * f2 -> Exp()
     * 
     * MOVE reg HAllocate
     * 
     * MOVE reg SimpleExp
     * 
     * MOVE reg BinOp
     *  
     */
    public String visit(MoveStmt n, PackedArgu argu){
        String reg1 = n.f1.accept(this, argu);
        if(n.f2.f0.choice instanceof HAllocate){
            e.emit("sw $a0 -4($sp)");
            e.emit("sw $v0 -8($sp)");
            n.f2.accept(this,argu);
            e.emit("move", reg1, "$v0");
            if (!reg1.equals("$v0")) e.emit("lw $v0 -8($sp)");
            if (!reg1.equals("$a0")) e.emit("lw $a0 -4($sp)");
        }else if (n.f2.f0.choice instanceof kanga.syntaxtree.SimpleExp){
            kanga.syntaxtree.SimpleExp simple_exp = (kanga.syntaxtree.SimpleExp)n.f2.f0.choice;
            if (simple_exp.f0.choice instanceof Reg){
                e.emit("move", reg1, simple_exp.accept(this, argu));
            }else if (simple_exp.f0.choice instanceof Label){
                e.emit("la", reg1, simple_exp.accept(this, argu));
            }else if (simple_exp.f0.choice instanceof IntegerLiteral){
                e.emit("li", reg1, simple_exp.accept(this, argu));
            }
        }else if (n.f2.f0.choice instanceof BinOp){
            n.f2.accept(this, new PackedArgu(reg1));
        }else{
            Info.panic("MoveStmt: unkown expression type", n.f2.f0.choice.toString());
        }
        return n.f0.toString();
    }

    /**
    * f0 -> "LT"
    *       | "PLUS"
    *       | "MINUS"
    *       | "TIMES"
    */
   public String visit(Operator n, PackedArgu argu) {
       String op = n.f0.choice.toString();
       switch (op){
           case "LT": return "slt";
           case "PLUS": return "add";
           case "MINUS": return "sub";
           case "TIMES": return "mul";
           default: Info.panic("unkown operator", op);
       }
       return null; 
   }

   /**
    * f0 -> Operator()
    * f1 -> Reg()
    * f2 -> SimpleExp()
    */
    public String visit(BinOp n, PackedArgu argu){
        String reg1 = argu.reg1;
        String reg2 = n.f1.accept(this, argu);
        String op = n.f0.accept(this,argu);
        String reg3 = null;
        if (n.f2.f0.choice instanceof Reg){
            reg3 = n.f2.accept(this,argu);
            e.emit(op, reg1, reg2, reg3);
        }else {
            reg3 = reg2.equals("$v0") ? "$v1" : "$v0";
            if (!reg1.equals(reg3)) e.emit("sw", reg3, "-4($sp)");
            if (n.f2.f0.choice instanceof Label){
                e.emit("la", reg3, n.f2.accept(this,argu));
            }else if (n.f2.f0.choice instanceof IntegerLiteral){
                e.emit("li", reg3, n.f2.accept(this,argu));
            }
            e.emit(op, reg1, reg2, reg3);
            if (!reg1.equals(reg3)) e.emit("lw", reg3, "-4($sp)");
            
        }
        return null;
    }

    public String visit(Goal n, PackedArgu argu){
        String function_name = n.f0.toString().toLowerCase();
        int spilled_args_num = Integer.parseInt(n.f5.accept(this, argu));
        K2M.EmitOpenFunction(e, function_name);
        e.emit("move $fp, $sp");
        e.emit("subu $sp, $sp, " + Integer.toString((1 + spilled_args_num) * 4));
        e.emit("sw $ra, -4($fp)");
        n.f10.accept(this, new PackedArgu(spilled_args_num));
        e.emit("lw $ra, -4($fp)");
        e.emit("addu $sp, $sp, " + Integer.toString((1 + spilled_args_num) * 4));
        e.emit("j $ra");
        e.emitClose("\n");
        n.f12.accept(this, argu);
        return null;
    }


    public String visit(Procedure n, PackedArgu argu){
        String function_name = n.f0.f0.toString();
        int spilled_args_num = Integer.parseInt(n.f5.f0.toString());
        K2M.EmitOpenFunction(e, function_name);
        e.emit("sw $fp, -8($sp)");
        e.emit("move $fp, $sp");
        e.emit("subu $sp, $sp, " + Integer.toString((2 + spilled_args_num) * 4));
        e.emit("sw $ra, -4($fp)");
        n.f10.accept(this, new PackedArgu(spilled_args_num));
        e.emit("lw $ra, -4($fp)");
        e.emit("lw $fp, " + Integer.toString(spilled_args_num * 4) + "($sp)");
        e.emit("addu $sp, $sp, " + Integer.toString((2 + spilled_args_num) * 4));
        e.emit("j $ra");
        e.emitClose("\n");
        return function_name;
    }
}
// =================================================================

public class K2M {

    /**
     *      .text
     *      .globl <name>
     * <name>:
     */
    public static void EmitOpenFunction(final Emitter e, String name) {
        e.emitOpen();
        e.emit(".text");
        e.emit(".globl", name);
        e.emitClose();
        e.emitOpen(name, ":");
    }

    static void K2M(final Emitter e, final String filename) throws Exception {
        final KangaParser parser = new KangaParser(
            filename == null ? System.in : new FileInputStream(filename));
        final Node root = parser.Goal();
        root.accept(new Kangatranslator(e), null);
        e.emitFlush();

        /* manifestly print system call functions & global data */

        // _halloc (text)
        EmitOpenFunction(e, "_halloc");
        String[] megs = new String[]{
            "li $v0, 9",
            "syscall",
            "j $ra",
        };
        for(String m : megs){
            e.emit(m);
        }
        e.emitClose("\n");
        
        // _print (text)
        EmitOpenFunction(e, "_print");
        megs = new String[]{
            "li $v0, 1",
            "syscall",
            "la $a0, newl",
            "li $v0, 4",
            "syscall",
            "j $ra",
        };
        for(String m : megs){
            e.emit(m);
        }
        e.emitClose("\n");

        // _error (text)
        EmitOpenFunction(e, "_error");
        megs = new String[]{
            "la $a0, str_er",
            "li $v0, 4",
            "syscall",
            "li $v0, 10",
            "syscall",
        };
        for(String m : megs){
            e.emit(m);
        }
        e.emitClose("\n");
        
        // newl (data)
        e.emitOpen();
        e.emit(".data");
        e.emit(".align   0");
        e.emitClose();
        e.emitOpen("newl", ":");
        e.emit(".asciiz \"\\n\"");
        e.emitClose("\n");

        // str_er (data)
        e.emitOpen();
        e.emit(".data");
        e.emit(".align   0");
        e.emitClose();
        e.emitOpen("str_er", ":");
        e.emit(".asciiz \" ERROR: abnormal termination\\n\"");
        e.emitClose("\n");
    }

    public static void main(String[] args) throws Exception{
        final Emitter e = new Emitter(false);
        final String filename = args.length > 0 ? args[0] : null;
        if (Info.DEBUG){
            K2M(e, filename);
        }else{
            try{
                K2M(e, filename);
            } catch (final Exception e_){
                System.err.println("Kanga Parsing Error");
                System.exit(-1);
            }
        }
    }
}