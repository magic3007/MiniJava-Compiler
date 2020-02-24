import java.util.*;
import minijava.parser.*;
import minijava.visitor.*;
import minijava.syntaxtree.*;


public class J2P {

	static void J2P(TypeCheckResult r) {
		r.classes.analyze();
		r.classes.dump();
	}

	public static void main(String[] args) {
		TypeCheckResult root = null;
		try {
			root = TypeCheck.TypeCheck();
		} catch (Exception e) {
			System.out.println("TypeCheck failed");
			System.exit(233);
		}

		J2P(root);
	}
}



