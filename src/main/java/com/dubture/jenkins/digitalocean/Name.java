/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Maxim Biro <nurupo.contributions@gmail.com>
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

package com.dubture.jenkins.digitalocean;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Name {
    private static final String PREFIX = "jenkins";
    private static final String NAME_REGEX = "([a-zA-Z0-9\\.]+)";
    private static final String UUID_REGEX = "\\p{XDigit}{8}-\\p{XDigit}{4}-\\p{XDigit}{4}-\\p{XDigit}{4}-\\p{XDigit}{12}";

    private static final String DROPLET_REGEX = PREFIX + "-" + NAME_REGEX + "-" + NAME_REGEX + "-" + UUID_REGEX;
    private static final String CONTAINER_REGEX = PREFIX + "-" + NAME_REGEX + "-" + NAME_REGEX + "-" + NAME_REGEX + "-" + UUID_REGEX;

    private static final Pattern NAME_PATTERN = Pattern.compile("^" + NAME_REGEX + "$");

    private static final Pattern CONTAINER_PATTERN = Pattern.compile("^" + CONTAINER_REGEX + "$");
    private static final Pattern DROPLET_PATTERN = Pattern.compile("^" + DROPLET_REGEX + "$");

    private Name() {
        throw new AssertionError();
    }

    public static boolean isValidTemplateName(final String name) {
        return NAME_PATTERN.matcher(name).matches();
    }

    public static String generateDropletName(final String cloudTemplateName, final String dropletTemplateName) {
        return PREFIX + "-" + cloudTemplateName + "-" + dropletTemplateName + "-" + UUID.randomUUID().toString();
    }

    public static String generateContainerName(final String cloudTemplateName, final String dropletTemplateName, final String containerTemplateName) {
        return PREFIX + "-" + cloudTemplateName + "-" + dropletTemplateName + "-" + containerTemplateName + "-" + UUID.randomUUID().toString();
    }

    public static boolean isDropletOfCloud(final String dropletName, final String cloudTemplateName) {
        Matcher m = DROPLET_PATTERN.matcher(dropletName);
        return m.matches() && m.group(1).equals(cloudTemplateName);
    }

    public static boolean isDropletOfDropletTemplate(final String dropletName, final String cloudTemplateName, final String dropletTemplateName) {
        Matcher m = DROPLET_PATTERN.matcher(dropletName);
        return m.matches() && m.group(1).equals(cloudTemplateName) && m.group(2).equals(dropletTemplateName);
    }

    public static boolean isContainerOfDropletTemplate(final String containerName, final String cloudTemplateName, final String dropletTemplateName) {
        Matcher m = CONTAINER_PATTERN.matcher(containerName);
        return m.matches() && m.group(1).equals(cloudTemplateName) && m.group(2).equals(dropletTemplateName);
    }

    public static boolean isContainerOfContainerTemplate(final String containerName, final String cloudTemplateName, final String dropletTemplateName, final String containerTemplateName) {
        Matcher m = CONTAINER_PATTERN.matcher(containerName);
        return m.matches() && m.group(1).equals(cloudTemplateName) && m.group(2).equals(dropletTemplateName) && m.group(3).equals(containerTemplateName);
    }
}
