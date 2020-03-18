import java.util.*;
import piglet.parser.*;
import piglet.visitor.*;
import piglet.syntaxtree.*;

// =================================================================

/**
 * Spiglet Token
 */
interface Token {
    public String toString();
}

class ExpToken implements Token {
}

class SimpleExpToken extends ExpToken {
}

class CallToken extends ExpToken {
    String tokenImage;

    CallToken(String tokenImage) {
        this.tokenImage = tokenImage;
    }

    public String toString() {
        return tokenImage;
    }
}

class HAllocateToken extends ExpToken {
    String tokenImage;

    HAllocateToken(String tokenImage) {
        this.tokenImage = tokenImage;
    }

    public String toString() {
        return tokenImage;
    }

}

class BinOpToken extends ExpToken {
    String tokenImage;

    BinOpToken(String tokenImage) {
        this.tokenImage = tokenImage;
    }

    public String toString() {
        return tokenImage;
    }
}

class TempToken extends SimpleExpToken {
    String tokenImage;

    TempToken() {
    }

    TempToken(String tokenImage) {
        this.tokenImage = tokenImage;
    }

    TempToken(Temp n) {
        this.tokenImage = "TEMP " + n.f1.f0.toString();
    }

    public String toString() {
        return tokenImage;
    }
}

class IntegerLiteralToken extends SimpleExpToken {
    int integerLiteral;

    IntegerLiteralToken(IntegerLiteral n) {
        this.integerLiteral = Integer.parseInt(n.f0.toString());
    }

    public String toString() {
        return Integer.toString(integerLiteral);
    }
}

class LabelToken extends SimpleExpToken {
    String label;

    LabelToken() {
    }

    LabelToken(String label) {
        this.label = label;
    }

    LabelToken(Label n) {
        this.label = n.f0.toString();
    }

    public String toString() {
        return label;
    }
}

// ===================================================================

/**
 * @description find the maximum TEMP index that has been used in source code.
 */
class PigletTempScanner extends DepthFirstVisitor {
    public int selfMaxTempIndex;

    PigletTempScanner() {
        this.selfMaxTempIndex = 0;
    }

    public void visit(Temp n) {
        selfMaxTempIndex = Integer.max(selfMaxTempIndex, Integer.parseInt(n.f1.f0.toString()));
        super.visit(n);
    }
}

// ====================================================================

class PigletTranslatorAugs {
    public Emitter e;
    /**
     * if |expectedToken| is null, it means we should emit this |Node| right now;
     * otherwise we should adjust this |Node| through a series of instructions and
     * return a expected token.
     */
    public Token expectedToken;

    PigletTranslatorAugs(Emitter e, Token expectedToken) {
        this.e = e;
        this.expectedToken = expectedToken;
    }

    Emitter getEmitter() {
        return e;
    }

    Token getExpectedToken() {
        return expectedToken;
    }
}

class PigletTranslator extends GJDepthFirst<Token, PigletTranslatorAugs> {

    public Token visit(NodeToken n, PigletTranslatorAugs argus) {
        Emitter e = argus.getEmitter();
        Token expectedToken = argus.getExpectedToken();
        if (expectedToken == null) {
            e.emitBuf(n.toString());
            return null;
        }
        Info.panic("Never reach here!");
        return null;
    }

    public Token visit(Label n, PigletTranslatorAugs argus) {
        Emitter e = argus.getEmitter();
        Token expectedToken = argus.getExpectedToken();
        if (expectedToken == null) {
            e.emitBuf(n.f0.toString());
            return null;
        } else if (expectedToken.getClass().isAssignableFrom(LabelToken.class)) {
            return new LabelToken(n);
        } else if (expectedToken.getClass().isAssignableFrom(TempToken.class)) {
            Token rv = new TempToken(e.newTemp());
            e.emit("MOVE", rv.toString(), n.f0.toString());
            return rv;
        }
        Info.panic("Never reach here!");
        return null;
    }

    public Token visit(IntegerLiteral n, PigletTranslatorAugs argus) {
        Emitter e = argus.getEmitter();
        Token expectedToken = argus.getExpectedToken();
        if (expectedToken == null) {
            e.emitBuf(n.f0.toString());
            return null;
        } else if (expectedToken.getClass().isAssignableFrom(IntegerLiteralToken.class)) {
            return new IntegerLiteralToken(n);
        } else if (expectedToken.getClass().isAssignableFrom(TempToken.class)) {
            Token rv = new TempToken(e.newTemp());
            e.emit("MOVE", rv.toString(), n.f0.toString());
            return rv;
        }
        Info.panic("Never reach here!");
        return null;
    }

    public Token visit(Temp n, PigletTranslatorAugs argus) {
        Emitter e = argus.getEmitter();
        Token expectedToken = argus.getExpectedToken();
        if (expectedToken == null) {
            e.emitBuf("TEMP ", n.f1.f0.toString());
            return null;
        } else if (expectedToken.getClass().isAssignableFrom(TempToken.class)) {
            return new TempToken(n);
        }
        Info.panic("Never reach here!");
        return null;
    }

    public Token visit(Goal n, PigletTranslatorAugs argus) {
        Emitter e = argus.getEmitter();
        Token expectedToken = argus.getExpectedToken();
        if (expectedToken == null) {
            e.emitOpen(n.f0.toString());
            n.f1.accept(this, new PigletTranslatorAugs(e, null));
            e.emitClose(n.f2.toString());
            n.f3.accept(this, new PigletTranslatorAugs(e, null));
            n.f4.accept(this, new PigletTranslatorAugs(e, null));
            e.emitFlush();
            return null;
        }
        Info.panic("Never reach here!");
        return null;
    }

    public Token visit(StmtList n, PigletTranslatorAugs argus) {
        Emitter e = argus.getEmitter();
        Token expectedToken = argus.getExpectedToken();
        NodeListOptional lst = n.f0;
        if (expectedToken == null) {
            if (lst.present()) {
                for (Enumeration<Node> ele = lst.elements(); ele.hasMoreElements();) {
                    Node node = ele.nextElement();
                    node.accept(this, new PigletTranslatorAugs(e, null));
                }
            }
            return null;
        }
        Info.panic("Never reach here!");
        return null;
    }

    public Token visit(Procedure n, PigletTranslatorAugs argus) {
        Emitter e = argus.getEmitter();
        Token expectedToken = argus.getExpectedToken();
        if (expectedToken == null) {
            n.f0.accept(this, new PigletTranslatorAugs(e, null));
            n.f1.accept(this, new PigletTranslatorAugs(e, null));
            n.f2.accept(this, new PigletTranslatorAugs(e, null));
            n.f3.accept(this, new PigletTranslatorAugs(e, null));
            e.emitFlush();
            n.f4.accept(this, new PigletTranslatorAugs(e, null));
            return null;
        }
        Info.panic("Never reach here!");
        return null;
    }

    public Token visit(Stmt n, PigletTranslatorAugs argus) {
        Emitter e = argus.getEmitter();
        Token expectedToken = argus.getExpectedToken();
        if (expectedToken == null) {
            n.f0.accept(this, argus);
            e.emitFlush();
            return null;
        }
        Info.panic("Never reach here!");
        return null;
    }

    public Token visit(CJumpStmt n, PigletTranslatorAugs argus) {
        Emitter e = argus.getEmitter();
        Token expectedToken = argus.getExpectedToken();
        if (expectedToken == null) {
            Token token1 = n.f1.accept(this, new PigletTranslatorAugs(e, new TempToken()));
            Token token2 = n.f2.accept(this, new PigletTranslatorAugs(e, new LabelToken()));
            e.emitBuf(n.f0.toString(), token1.toString(), token2.toString());
            return null;
        }
        Info.panic("Never reach here!");
        return null;
    }

    /**
     * Piglet syntax: HStoreStmt ::= "HSTORE" Exp IntegerLiteral Exp
     * Spiglet syntax: HStoreStmt ::= "HSTORE" Temp IntegerLiteral Temp
     */
    public Token visit(HStoreStmt n, PigletTranslatorAugs argus) {
        Emitter e = argus.getEmitter();
        Token expectedToken = argus.getExpectedToken();
        if (expectedToken == null) {
            Token token1 = n.f1.accept(this, new PigletTranslatorAugs(e, new TempToken()));
            Token token3 = n.f3.accept(this, new PigletTranslatorAugs(e, new TempToken()));
            e.emitBuf(n.f0.toString(), token1.toString(), n.f2.f0.toString(), token3.toString());
            return null;
        }
        Info.panic("Never reach here!");
        return null;
    }

    /**
     * Piglet syntax: HLoadStmt ::= "HLOAD" Temp Exp IntegerLiteral 
     * Spiglet syntax: HLoadStmt ::= "HLOAD" Temp Temp IntegerLiteral
     */
    public Token visit(HLoadStmt n, PigletTranslatorAugs argus) {
        Emitter e = argus.getEmitter();
        Token expectedToken = argus.getExpectedToken();
        if (expectedToken == null) {
            Token token1 = n.f1.accept(this, new PigletTranslatorAugs(e, new TempToken()));
            Token token2 = n.f2.accept(this, new PigletTranslatorAugs(e, new TempToken()));
            e.emitBuf(n.f0.toString(), token1.toString(), token2.toString(), n.f3.f0.toString());
            return null;
        }
        Info.panic("Never reach here!");
        return null;
    }

    /**
     * Piglet syntax: MoveStmt ::= "MOVE" Temp (piglet-)Exp 
     * Spiglet syntax: MoveStmt ::= "MOVE" Temp (spiglet-)Exp
     */
    public Token visit(MoveStmt n, PigletTranslatorAugs argus) {
        Emitter e = argus.getEmitter();
        Token expectedToken = argus.getExpectedToken();
        if (expectedToken == null) {
            Token token1 = n.f1.accept(this, new PigletTranslatorAugs(e, new TempToken()));
            Token token2 = n.f2.accept(this, new PigletTranslatorAugs(e, new ExpToken()));
            e.emitBuf(n.f0.toString(), token1.toString(), token2.toString());
            return null;
        }
        Info.panic("Never reach here!");
        return null;
    }

    /**
     * Piglet syntax: PrintStmt ::= "PRINT" Exp 
     * Spiglet syntax: PrintStmt ::= "PRINT" SimpleExp
     */
    public Token visit(PrintStmt n, PigletTranslatorAugs argus) {
        Emitter e = argus.getEmitter();
        Token expectedToken = argus.getExpectedToken();
        if (expectedToken == null) {
            Token token1 = n.f1.accept(this, new PigletTranslatorAugs(e, new SimpleExpToken()));
            e.emitBuf(n.f0.toString(), token1.toString());
            return null;
        }
        Info.panic("Never reach here!");
        return null;
    }

    public Token visit(Exp n, PigletTranslatorAugs argus){
        return n.f0.accept(this, argus);
    }

    /**
     * Piglet syntax: StmtExp ::= "BEGIN" StmtList "RETURN" Exp "END" Spuglet
     * syntax: StmtExp ::= "BEGIN" StmtList "RETURN" SimpleExp "END"
     */
    public Token visit(StmtExp n, PigletTranslatorAugs argus) {
        Emitter e = argus.getEmitter();
        Token expectedToken = argus.getExpectedToken();
        if (expectedToken == null) {
            e.emitOpen(n.f0.toString());
            n.f1.accept(this, new PigletTranslatorAugs(e, null));
            Token token3 = n.f3.accept(this, new PigletTranslatorAugs(e, new SimpleExpToken()));
            e.emit(n.f2.toString(), token3.toString());
            e.emitClose(n.f4.toString());
            return null;
        } else {
            n.f1.accept(this, new PigletTranslatorAugs(e, null));
            Token token3 = n.f3.accept(this, argus);
            return token3;
        }
    }

    public Token visit(Call n, PigletTranslatorAugs argus) {
        Emitter e = argus.getEmitter();
        Token expectedToken = argus.getExpectedToken();
        Token token1 = n.f1.accept(this, new PigletTranslatorAugs(e, new SimpleExpToken()));
        List<Token> parameters_tokens = new ArrayList<Token>();
        for (Enumeration<Node> ele = n.f3.elements(); ele.hasMoreElements();) {
            parameters_tokens.add(ele.nextElement().accept(this, new PigletTranslatorAugs(e, new TempToken())));
        }
        StringBuffer buf = new StringBuffer();
        buf.append(n.f0.toString(), token1.toString(), n.f2.toString());
        for (Token token : parameters_tokens) {
            buf.append(token.toString());
        }
        buf.append(n.f4.toString());
        if (expectedToken == null) {
            e.emit(buf.toString());
            return null;
        } else if (expectedToken.getClass().isAssignableFrom(CallToken.class)) {
            return new CallToken(buf.toString());
        } else {
            /**
             * MOVE TEMP 1 CAll SimpleExp (...)
             */
            Token rv = new TempToken(e.newTemp());
            buf.prepend(rv.toString());
            buf.prepend("MOVE");
            e.emit(buf.toString());
            return rv;
        }
    }

    public Token visit(HAllocate n, PigletTranslatorAugs argus) {
        Emitter e = argus.getEmitter();
        Token expectedToken = argus.getExpectedToken();
        Token token1 = n.f1.accept(this, new PigletTranslatorAugs(e, new SimpleExpToken()));
        StringBuffer buf = new StringBuffer();
        buf.append("HALLOCATE", token1.toString());
        if (expectedToken == null) {
            e.emit(buf.toString());
            return null;
        } else if (expectedToken.getClass().isAssignableFrom(HAllocateToken.class)) {
            return new HAllocateToken(buf.toString());
        } else {
            /**
             * MOVE TEMP 1 HALLOCATE SimpleExp
             */
            Token rv = new TempToken(e.newTemp());
            buf.prepend(rv.toString());
            buf.prepend("MOVE");
            e.emit(buf.toString());
            return rv;
        }
    }

    public Token visit(BinOp n, PigletTranslatorAugs argus) {
        Emitter e = argus.getEmitter();
        Token expectedToken = argus.getExpectedToken();
        Token token1 = n.f1.accept(this, new PigletTranslatorAugs(e, new TempToken()));
        Token token2 = n.f2.accept(this, new PigletTranslatorAugs(e, new SimpleExpToken()));
        StringBuffer buf = new StringBuffer();
        buf.append(n.f0.f0.choice.toString(), token1.toString(), token2.toString());
        if (expectedToken == null) {
            e.emit(buf.toString());
            return null;
        } else if (expectedToken.getClass().isAssignableFrom(BinOp.class)) {
            return new BinOpToken(buf.toString());
        } else {
            /**
             * MOVE TEMP 1 BinOp TEMP SimpleExp
             */
            Token rv = new TempToken(e.newTemp());
            buf.prepend(rv.toString());
            buf.prepend("MOVE");
            e.emit(buf.toString());
            return rv;
        }
    }
}

// ====================================================================

public class P2S {

    static void P2S(final Emitter e) throws Exception {
        final PigletParser parser = new PigletParser(System.in);
        final Node root = parser.Goal();
        final PigletTempScanner tempScanner = new PigletTempScanner();

        root.accept(tempScanner);
        e.setTempNum(tempScanner.selfMaxTempIndex);
        root.accept(new PigletTranslator(), new PigletTranslatorAugs(e, null));
        e.emitFlush();
    }

    public static void main(String[] args)throws Exception {
        final Emitter e = new Emitter(false);
        if(Info.DEBUG){
            P2S(e);
        }else{
            try {
                P2S(e);
            } catch (final Exception e_) {
                System.err.println("Piglet Parsing Error");
                System.exit(-1);
            }
        }
    }
}