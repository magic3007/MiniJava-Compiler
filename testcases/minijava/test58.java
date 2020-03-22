class test58{
    public static void main(String[] a){
	System.out.println(new A().start());
    }
}

class C extends B{

}

class B extends A{

}

class A{

    B b;
    C c;

    public int start(){

	b = c;
	
	return 0;
    }
}
