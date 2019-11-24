/*
 * This file is part of GriefDefender, licensed under the MIT License (MIT).
 *
 * Copyright (c) bloodmc
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.griefdefender.util;

import com.griefdefender.GDBootstrap;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

public class BootstrapUtil {

    private final static Method METHOD_ADD_URL;

    static {
        Method methodAddUrl = null;
        try {
            methodAddUrl = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            methodAddUrl.setAccessible(true);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        METHOD_ADD_URL = methodAddUrl;
    }

    public static void addUrlToClassLoader(String name, File input) {
        try {
            final ClassLoader classLoader = GDBootstrap.class.getClassLoader();
            if (classLoader instanceof URLClassLoader) {
                try {
                    METHOD_ADD_URL.invoke(classLoader, new URL("jar:file:" + input.getPath() + "!/"));
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            } else {
                throw new RuntimeException("Unknown classloader: " + classLoader.getClass());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
