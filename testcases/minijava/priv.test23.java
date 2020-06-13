class test23{
    public static void main(String[] a){
	System.out.println(new Test().start());
    }
}

class Test{

    int i;

    public int start(){

	Test test;

	test = new Test();

	i = 10;

	i = test.first(new Test().second());

	return i;
    }

    public int first(int i){

	return i;

    }

    public int second(){

	return 30;
    }
}
