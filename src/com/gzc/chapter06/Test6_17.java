package com.gzc.chapter06;

public class Test6_17 {
    // test()的方法体是一个作用域，方法内的匿名块是一个作用域，这两个作用域的owner都指向MethodSymbol(name=test)。
    // 匿名块形成的作用域的next指向了方法体形成的作用域
    public void test() {
        int a = 1;
        {
            int b = 2;
        }
    }
}
