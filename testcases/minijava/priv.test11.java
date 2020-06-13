class test11{
    public static void main(String[] a){
	System.out.println(new Test().start());
    }
}

class Test{

    int i;

    public int start(){

	i = 60;

	System.out.println(i);

	i = i + 10;

	return i;
    }
}
