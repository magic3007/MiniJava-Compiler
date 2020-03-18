class test57{
    public static void main(String[] a){
	System.out.println(new A().start());
    }
}

class C extends B{

}

class B extends A{

}

class A{

    A a;
    C c;

    public int start(){

	a = c;
	
	return 0;
    }
}
