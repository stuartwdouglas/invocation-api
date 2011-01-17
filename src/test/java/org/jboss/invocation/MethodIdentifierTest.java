/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.invocation;

import java.lang.reflect.Method;

import junit.framework.Assert;

import org.junit.Test;

public class MethodIdentifierTest {

    @Test
    public void testPrimitiveTypes() throws SecurityException, NoSuchMethodException, ClassNotFoundException {
        Method aMethod = getClass().getDeclaredMethod("someMethod", double.class, long.class, String.class);
        MethodIdentifier identifier = MethodIdentifier.getIdentifierForMethod(aMethod);
        identifier.getPublicMethod(getClass());
    }

    @Test
    public void testGetMethod() throws NoSuchMethodException, ClassNotFoundException {
        MethodIdentifier id1 = MethodIdentifier.getIdentifier("method", long.class);
        MethodIdentifier id2 = MethodIdentifier.getIdentifier("method", int.class);
        Method method1 = id1.getMethod(Private2.class);
        Assert.assertEquals(Private1.class, method1.getDeclaringClass());
        Assert.assertEquals(long.class, method1.getParameterTypes()[0]);
        Method method2 = id2.getMethod(Private2.class);
        Assert.assertEquals(Private2.class, method2.getDeclaringClass());
        Assert.assertEquals(int.class, method2.getParameterTypes()[0]);

    }

    public double someMethod(double d1, long l2, String s2) {
        return 0;
    }
}
