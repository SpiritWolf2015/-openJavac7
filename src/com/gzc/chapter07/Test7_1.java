package com.gzc.chapter07;

import com.gzc.compile.*;

/**
 * 处理导入声明时，会为包名compile建立对应的PackageSymbol对象并给completer赋值为ClassReader对象。当分析定义变量a的语句时，
 * a变量声明的类型ImportedTest是compile包下定义的类，Javac会获取PackageSymbol对象，然后调用对象的complete()方法加载compile包下定义的所有类，
 * 最终会调用ClassReader对象的complete()方法来完成加载
 */
public class Test7_1 {
    ImportedTest a;
}
