class test92{
    public static void main(String[] a){
	System.out.println(new Test().start());
    }
}

class Test2 extends Test {

    public int next(int i) {

	return 0;
    }
}

class Test {

    Test test;
    int[] i;

    public int start(){

	i = new int[10];

	test = test.next(i);	// TE TE
	
	return 0;
    }
}
