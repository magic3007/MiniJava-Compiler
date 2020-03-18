class test76{
    public static void main(String[] a){
	System.out.println(new Test().start());
    }
}

class Test {

    Test test;
    int i;

    public int start(){

	i = test.next();
	
	return 0;
    }

    public int next(){

	i = test.start();
	
	return i;
    }

}
