import minijava.parser.*;
import minijava.visitor.*;
import minijava.syntaxtree.*;
import java.util.*;


public class J2P {

	public static void main(String []args) throws Exception {
		Emitter e = new Emitter(false);
		if (Info.DEBUG) {
			TypeCheck.TypeCheck(e);
		}

		try {
			TypeCheck.TypeCheck(e);	
		} catch (Exception e_) {
			System.exit(0);
		}

	}
}
