package com.gzc.chapter06;

import java.io.Serializable;

/**
 * 变量a声明的类型为Test<String>，这个类型与定义时的类型Test<T extends Serializable>已经不是同一种类型，
 * 这时可以通过表示Test<String>的ClassType对象的tsym找到定义Test类的符号，也就是获取Test<T extends Serializable>对应的ClassSymbol对象，
 * 进而获取这个对象的type值，这样就可以验证实际传递的泛型参数String是否在定义类时所声明的类型参数的上限Serializable之内。
 */
public class Test6_20<T extends Serializable> {
    Test6_20<String> a;
}
