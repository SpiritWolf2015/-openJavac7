package com.gzc.chapter06;

/**
 * Plate<? >是个ClassType对象，typarams_field列表中有一个WildcardType对象，用来表示无界通配符？WildcardType对象的type值为Object类型，
 * 所有无界通配符的默认上界都为Object类，kind值为UNBOUND，而bound就是Plate类在定义时声明的类型参数T，类型为TypeVar。
 * 对于Plate<? extends Fruit>来说，实际类型参数也是一个WildcardType对象，不过type为Fruit, kind为EXTENDS，而bound同样表示类型参数T的TypeVar对象。
 * Plate<? super Fruit>与Plate<? extends Fruit>类似，唯一不同的是kind的值，Plate<? super Fruit>类的kind值为SUPER。
 */
public class Test6_24 {
    public void test() {
        Plate<?> p1;
        Plate<? extends Fruit> p2;
        Plate<? super Fruit> p3;
    }
}

class Fruit {
}

class Apple extends Fruit {
}

class Plate<T> {
    private T item;

    public void set(T t) {
        item = t;
    }

    public T get() {
        return item;
    }
}

