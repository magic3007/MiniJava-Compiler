class testOverload{
    public static void main(String[] a){
	System.out.println(new Test().start());
    }
}

class Test {

    Test2 test2;
    int b;

    public int start(){

        b = test2.next(1);

	return 0;
    }

    public int next(){ 

	return 0;
    }
}


class Test2 extends Test {

    public int next(int a) { // TE

	return a;
    }

}