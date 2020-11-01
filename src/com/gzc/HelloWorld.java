package com.gzc;

import java.util.ArrayList;
import java.util.List;

public class HelloWorld extends Object {
    private int aa = 1;

//    public static int bb = 1;
    static {
        bb = 2;
    }
    public static int bb = 1;

    public int getAa() {
        return aa;
    }

    public void setAa(int aa) {
        this.aa = aa;
    }

    public static void main(String[] args) {
        List<HelloWorld> list = new ArrayList<>();

//        HelloWorld.bb = 3;
        HelloWorld hw = new HelloWorld();
        hw.setAa(2);
        list.add(hw);

        for (HelloWorld item : list) {
            System.out.println("a=" + item.getAa());
            System.out.println("b=" + HelloWorld.bb);
        }
    }
}
