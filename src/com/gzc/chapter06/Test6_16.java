package com.gzc.chapter06;

/**
 * 实例6-16中定义的3个变量不在同一个作用域范围内，因此即使名称相同也不会引起冲突。
 * 相同作用域内的符号都会存放到同一个Scope对象下.<p>
 *
 * @see com.sun.tools.javac.code.Scope
 */
public class Test6_16 {
    int x = 1;

    {
        float x = 2;
    }

    public void test() {
        long x = 2;
    }
}
