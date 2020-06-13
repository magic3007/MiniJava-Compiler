class test16{
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

	i = i + ((test.first()).second());

	return i;
    }

    public Test first(){

	return new Test();

    }

    public int second(){

	i = i + 10;

	return i;
    }
}
