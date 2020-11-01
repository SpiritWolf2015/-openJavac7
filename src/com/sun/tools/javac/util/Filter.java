/*
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.sun.tools.javac.util;

/**
 * 策略模式，实现类实现Filter接口中声明的accepts(T t)方法，在实现方法中编写符号过滤条件，当方法返回false时，则表示符号不满足要求，
 * 调用lookup()方法继续查找；当方法返回true，表示已经查找到合适的符号。<p>
 * Simple filter acting as a boolean predicate. Method accepts return true if
 * the supplied element matches against the filter.
 */
public interface Filter<T> {
    /**
     * Does this element match against the filter?
     * @param t element to be checked
     * @return true if the element satisfy constraints imposed by filter
     */
    boolean accepts(T t);
}
