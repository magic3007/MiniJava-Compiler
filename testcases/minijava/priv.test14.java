class test14{
    public static void main(String[] a){
	System.out.println(new Test().start());
    }
}

class Test{

    public int start(){

	int i;

	if (true && false) 
		i = 0;
	else 
		i = 1;

	return i;
    }
}
