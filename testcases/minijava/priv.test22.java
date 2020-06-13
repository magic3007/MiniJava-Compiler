// test22: one more inheritance
//
// correct output: 13
//		   60
//  		   132 

class test22 {
  public static void main(String[] s) {
    System.out.println(new B25().add(new C25(), 1));
  }
}

class A25 {
  int ai;
  A25 aa;

  public int add(A25 a, int i) {
    return this.init(i);  
  }

  public int init(int i) {
    if(i < 50) {
      ai = i;
    } else {
      ai = 2 * i;
    }
    return ai;
  }
}

class B25 extends A25 {
  int bi;

  public int add(A25 a, int i) {
    System.out.println(this.init(i)); // --> 13
    return a.add(aa, bi);
  }

  public int init(int i) {
    aa = new B25();
    ai = 2 * i;
    bi = 10 + i;
    return ai + bi;
  }
}

class C25 extends A25 {
  int ci;

  public int add(A25 a, int i) {
    System.out.println(this.init(i)); // --> 60
    return aa.add(a, ci);
  }

  public int init(int i) {
    aa = new A25();
    ai = i - 5;
    ci = ai * i;
    return ci - ai;
  }
}
