package com.gzc.chapter06;

/**
 * 1·对于表示Outer<T1>类型的ClassType对象来说，typarams_field与allparams_field列表中只含有一个TypeVar(tsym.name=T1)对象；outer_field的值为空。
 * 2·对于表示内部类Inner<T2>的ClassType对象来说，其typarams_field列表中只含有一个TypeVar(tsym.name=T2)对象；allparams_field列表中包含TypeVar(tsym.name=T1)与TypeVar(tsym.name=T2)对象；
 * outer_field的值就是表示Outer<T1>类型的ClassType对象。
 * 3·对于表示参数化类型Outer<Integer>.Inner<String>的ClassType对象来说，typarams_field列表中包含ClassType(tsym.name=String)对象；
 * allparams_field列表中包含ClassType(tsym.name=String)与ClassType(tsym.name=Integer)对象；outer_field的值是表示参数化类型Outer<Integer>的ClassType对象。
 */
public class Test6_21_Outer<T1> {
    class Inner<T2> {
    }

    public void test() {
        Test6_21_Outer<Integer>.Inner<String> x;
    }
}
