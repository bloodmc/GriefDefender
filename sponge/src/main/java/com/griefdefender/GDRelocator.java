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
package com.griefdefender;

import com.griefdefender.util.BootstrapUtil;
import me.lucko.jarrelocator.JarRelocator;
import me.lucko.jarrelocator.Relocation;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GDRelocator {

    private static GDRelocator instance;
    private List<Relocation> rules;

    public static GDRelocator getInstance() {
        if (instance == null) {
            instance = new GDRelocator();
        }
        return instance;
    }

    public GDRelocator() {
        this.rules = new ArrayList<>();
        for (String name : GDBootstrap.getInstance().getRelocateList()) {
            final String[] parts = name.split(":");
            final String key = parts[0];
            final String relocated = parts[1];
            this.rules.add(new Relocation(key, "com.griefdefender.lib." + relocated));
        }
    }

    public void relocateJars(Map<String, File> jarMap) {
        for (Map.Entry<String, File> mapEntry : jarMap.entrySet()) {
            final String name = mapEntry.getKey();
            final File input = mapEntry.getValue();
            final File output = Paths.get(input.getParentFile().getPath()).resolve(input.getName().replace(".jar", "") + "-shaded.jar").toFile();
            if (!output.exists()) {
                // Relocate
                JarRelocator relocator = new JarRelocator(input, output, this.rules);
        
                try {
                    relocator.run();
                } catch (IOException e) {
                    throw new RuntimeException("Unable to relocate", e);
                }
            }
            BootstrapUtil.addUrlToClassLoader(name, output);
        }
    }
}
