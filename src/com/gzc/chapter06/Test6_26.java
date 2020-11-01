package com.gzc.chapter06;

// 对于有上界的Plate<? super Fruit>来说，从盘子中取东西的get()方法只能通过Object类接收，往盘子里放东西的set()方法都可用。
public class Test6_26 {
    public void test() {
        Plate<? super Fruit> p = new Plate<Fruit>();
        p.set(new Fruit());
        p.set(new Apple());

        //Apple f1 = p.get(); // 报错
        //Fruit f2 = p.get(); // 报错
        Object f3 = p.get();
        /*
        盘子里放着Fruit或者Fruit的父类，但并不知道具体的类型，因此调用set()方法放入Fruit或Fruit的子类都是允许的，但是当调用get()方法时，
        由于不知道具体的类型，可能为Fruit或Fruit的任何一个父类型，因而只能以Object来接收。
        */
    }

}