class test09{
    public static void main(String[] a){
	System.out.println(new Test().start());
    }
}

class Test{

    int i;

    public int start(){

	boolean b;

	i = 11; 

	b = true;

	while (i < 15) {

		if (b)
			System.out.println(1);
		else
			System.out.println(0);

		b = !b;
		i = i + 1;
	}

	return i;
    }
}
