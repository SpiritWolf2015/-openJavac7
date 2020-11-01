package com.gzc.chapter06;

public class Test6_22 {
    public void md(String className, Object o) {
        try {
            o = Class.forName(className).newInstance();
        } catch (IllegalAccessException | InstantiationException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
