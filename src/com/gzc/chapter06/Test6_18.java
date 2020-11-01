package com.gzc.chapter06;

//定义的三个成员都有相同的名称obj，在实际使用中会根据上下文确定引用的具体成员，因此Javac并不会报编译错误
public class Test6_18 {
    class obj {
    }

    int obj = 0;

    public void obj() {
    }
}
