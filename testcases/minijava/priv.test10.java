class test10{
    public static void main(String[] a){
	System.out.println(new Test().start());
    }
}

class Test{

    int i;

    public int start(){
	
	Test test;

	i = 10; 

	test = new Test();

	i = test.next();

	return i;
    }

    public int next() {

	return 20;
    }
}
