class test03{
    public static void main(String[] a){
	System.out.println(new Test().start());
    }
}

class Test{

    int i;

    public int start(){

	i = (800 - 300) * 3;

	return i;
    }
}
