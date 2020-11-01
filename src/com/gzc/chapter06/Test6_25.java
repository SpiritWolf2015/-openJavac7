package com.gzc.chapter06;

// 通配符的使用让Java的泛型转换更容易，但也会造成部分功能失效。例如，对于实例6-24来说，向盘子中存或者取东西时，对于有上界的Plate<? extends Fruit>来说，
// 往盘子里放东西的set()方法都不可用，而取东西的get()方法都可用。
public class Test6_25 {
    public void test() {
        Plate<? extends Fruit> p = new Plate<Apple>();
        //p.set(new Fruit()); // 报错
        //p.set(new Apple()); // 报错

        Fruit f1 = p.get();
        //Apple f2 = p.get(); // 报错
        /*
        以上代码中，调用set()方法都报错，而调用get()方法时变量声明的类型只能是Fruit或者它的父类，因为Javac只知道容器内是Fruit或者它的派生类，
        而并不知道保存的具体类型。在这里实际上是Apple，但Javac并没有保存这个信息，因为保存实际的类型几乎不可能，实际的类型随时都可能被更改。
        */
    }

}