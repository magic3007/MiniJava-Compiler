class test77{
    public static void main(String[] a){
	System.out.println(new Test().start());
    }
}

class Test {

    Test test;
    boolean b;

    public int start(){

	b = test.next();
	
	return 0;
    }

    public boolean next(){

	return ((true && (7<8)) && b);
    }

}
