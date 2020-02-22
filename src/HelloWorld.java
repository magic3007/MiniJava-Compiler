import minijava.parser.*;
import minijava.visitor.GJNoArguDepthFirst;
import minijava.syntaxtree.*;

class Print extends GJNoArguDepthFirst {
	public void MinusExpression(Expression n) {
		System.out.println("-");
	}
}

public class HelloWorld {
	public static void main(String []args) throws Exception {
		MiniJavaParser parser = new MiniJavaParser(System.in);
		Node root = parser.Goal();
		root.accept(new Print());
	}
}


