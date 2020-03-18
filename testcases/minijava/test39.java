class test39{
    public static void main(String[] a){
	System.out.println(new Test().start());
    }
}

class Test{

    Test test;

    public int start(){

	while(test.next()) System.out.println(1);	// TE

	return 0;
    }

    public Test next(){

	while (true) System.out.println(1);

	return test;

    }
}
