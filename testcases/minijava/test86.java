class test125{
    public static void main(String[] a){
	System.out.println(new Test().start());
    }
}

class Test2 {

}

class Test {

    Test test;

    public int start(){

	test = new Test2();	// TE
	
	return 0;
    }

}
