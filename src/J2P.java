import minijava.parser.*;
import minijava.visitor.*;
import minijava.syntaxtree.*;
import java.util.*;


public class J2P {

	public static void main(String []args) throws Exception {
		Emitter e = new Emitter(false);
		TypeCheck.TypeCheck(e, args.length > 0 ? args[0] : null);
	}
}
