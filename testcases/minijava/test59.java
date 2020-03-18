class test59{
    public static void main(String[] a){
	System.out.println(new A().start());
    }
}

class B extends A{

}

class A{

    A a;
    B b;

    public int start(){

	b = a;	// TE
	
	return 0;
    }
}
