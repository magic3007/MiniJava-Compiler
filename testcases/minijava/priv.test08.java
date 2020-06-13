class test08{
    public static void main(String[] a){
	System.out.println(new Test().start());
    }
}

class Test{

    int i;

    public int start(){

	boolean b;

	i = 10; 

	b = i < 20;

	if (b) 
		System.out.println(1);
	else
		System.out.println(0);

	return i;
    }
}
