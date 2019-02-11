/*
 *  This file is part of Cubic Chunks Mod, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2015-2019 OpenCubicChunks
 *  Copyright (c) 2015-2019 contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */
package io.github.opencubicchunks.cubicchunks.core.util;

import com.google.common.base.Throwables;
import mcp.MethodsReturnNonnullByDefault;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class Mappings {

    private static boolean IS_DEV;
    //since srg field and method names are guarranted not to collide -  we can store them in one map
    private static final Map<String, String> srgToMcp = new HashMap<>();

    static {
        String location = System.getProperty("net.minecraftforge.gradle.GradleStart.srg.srg-mcp");
        IS_DEV = location != null;
        if (IS_DEV) {
            initMappings(location);
        }
    }

    public static String getNameFromSrg(String srgName) {
        if (IS_DEV) {
            String result = srgToMcp.get(srgName);
            return result == null ? srgName : result;
        }
        return srgName;
    }

    private static void initMappings(String property) {
        try (Scanner scanner = new Scanner(new File(property))) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                parseLine(line);
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private static void parseLine(String line) {
        if (line.startsWith("FD: ")) {
            parseField(line.substring("FD: ".length()));
        }
        if (line.startsWith("MD: ")) {
            parseMethod(line.substring("MD: ".length()));
        }
    }

    private static void parseMethod(String substring) {
        String[] s = substring.split(" ");

        final int SRG_NAME = 0/*, SRG_DESC = 1*/, MCP_NAME = 2/*, MCP_DESC = 3*/;

        int lastIndex = s[SRG_NAME].lastIndexOf('/') + 1;
        if (lastIndex < 0) {
            lastIndex = 0;
        }

        s[SRG_NAME] = s[SRG_NAME].substring(lastIndex);

        lastIndex = s[MCP_NAME].lastIndexOf("/") + 1;
        if (lastIndex < 0) {
            lastIndex = 0;
        }

        s[MCP_NAME] = s[MCP_NAME].substring(lastIndex);

        srgToMcp.put(s[SRG_NAME], s[MCP_NAME]);
    }

    private static void parseField(String str) {
        if (!str.contains(" ")) {
            return;
        }
        String[] s = str.split(" ");
        assert s.length == 2;

        int lastIndex = s[0].lastIndexOf('/') + 1;
        if (lastIndex < 0) {
            lastIndex = 0;
        }

        s[0] = s[0].substring(lastIndex);

        lastIndex = s[1].lastIndexOf("/") + 1;
        if (lastIndex < 0) {
            lastIndex = 0;
        }

        s[1] = s[1].substring(lastIndex);

        srgToMcp.put(s[0], s[1]);
    }
}
