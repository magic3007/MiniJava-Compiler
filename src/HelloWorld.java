import minijava.parser.*;

public class HelloWorld {
	public static void main(String []args) throws Exception {
		System.out.println("read the minijava code to be compiled from stdin:");
		MiniJavaParser parser = new MiniJavaParser(System.in);
		parser.Goal();
	}
}
