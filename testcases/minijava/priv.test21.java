// test21: more inheritance
//
// correct output: 55

class test21 {
  public static void main (String[] s) {
    System.out.println(new A24().m1(false));
  }
}

class A24 {
  int i;
  int[] ii;
  A24 a;
  B24 b;

  public int m1(boolean b1) {
    int i;
    int len;

    if(b1) { 
      i = 5; // i is local variable 
    } else {
      i = 10;
    }

    ii = new int[i];
    i = 0;
    len = ii.length;
    while(i < len) {
      ii[i] = i + 1;
      i = i + 1;
    }
    b = new B24();
    a = b;
    return a.m2(ii);
  }

  public int m2(int[] i) {
    return 1; 
  }
}

class B24 extends A24 {
  public int m2(int[] i) {
    int len;
    int sum;
    int tmp;
    int val;

    tmp = 0;
    sum = 0;
    len = i.length;
    while (tmp < len) {
      val = i[tmp];
      sum = sum + val;
      tmp = tmp + 1;
    }
    return sum;
  }
}
