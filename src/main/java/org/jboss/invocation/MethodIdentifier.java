/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, JBoss Inc., and individual contributors as indicated
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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jboss.marshalling.FieldSetter;

/**
 * A unique identification of a method within some class or interface.  Suitable for usage as a hash table key.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class MethodIdentifier implements Serializable {

    private static final long serialVersionUID = -4303462176794600579L;

    private static final FieldSetter hashCodeSetter = FieldSetter.get(MethodIdentifier.class, "hashCode");

    private final String name;
    private final String[] parameterTypes;
    private final transient int hashCode;
    private static final String[] NO_STRINGS = new String[0];

    private MethodIdentifier(final String name, final String... parameterTypes) {
        if (name == null) {
            throw new IllegalArgumentException("name is null");
        }
        if (parameterTypes == null) {
            throw new IllegalArgumentException("parameterTypes is null");
        }
        this.name = name;
        this.parameterTypes = parameterTypes == null || parameterTypes.length == 0 ? NO_STRINGS : parameterTypes.clone();
        hashCode = calculateHash(name, parameterTypes);
    }

    private MethodIdentifier(final Method method) {
        final String name = (this.name = method.getName());
        final Class<?>[] methodParameterTypes = method.getParameterTypes();
        final String[] parameterTypes = methodParameterTypes.length == 0 ? NO_STRINGS : namesOf(methodParameterTypes);
        hashCode = calculateHash(name, parameterTypes);
        this.parameterTypes = parameterTypes;
    }

    private static String[] namesOf(final Class<?>[] types) {
        final String[] strings = new String[types.length];
        for (int i = 0, typesLength = types.length; i < typesLength; i++) {
            strings[i] = types[i].getName();
        }
        return strings;
    }

    private static Class<?>[] typesOf(final String[] names, final ClassLoader classLoader) throws ClassNotFoundException {
        final Class<?>[] types = new Class<?>[names.length];
        for (int i = 0, namesLength = names.length; i < namesLength; i++) {
            if (names[i].equals("int")) {
                types[i] = int.class;
            } else if (names[i].equals("short")) {
                types[i] = short.class;
            } else if (names[i].equals("byte")) {
                types[i] = byte.class;
            } else if (names[i].equals("char")) {
                types[i] = char.class;
            } else if (names[i].equals("long")) {
                types[i] = long.class;
            } else if (names[i].equals("boolean")) {
                types[i] = boolean.class;
            } else if (names[i].equals("double")) {
                types[i] = double.class;
            } else if (names[i].equals("float")) {
                types[i] = float.class;
            } else {
                types[i] = Class.forName(names[i], false, classLoader);
            }
        }
        return types;
    }

    private static int calculateHash(final String name, final String[] parameterTypes) {
        return name.hashCode() * 7 + Arrays.hashCode(parameterTypes);
    }

    /**
     * Get the method name.
     *
     * @return the method name
     */
    public String getName() {
        return name;
    }

    /**
     * Get the parameter type names, as strings.
     *
     * @return the parameter type names
     */
    public String[] getParameterTypes() {
        final String[] parameterTypes = this.parameterTypes;
        return parameterTypes == NO_STRINGS ? parameterTypes : parameterTypes.clone();
    }

    /**
     * Determine whether this object is equal to another.
     *
     * @param other the other object
     * @return {@code true} if they are equal, {@code false} otherwise
     */
    public boolean equals(Object other) {
        return other instanceof MethodIdentifier && equals((MethodIdentifier)other);
    }

    /**
     * Determine whether this object is equal to another.
     *
     * @param other the other object
     * @return {@code true} if they are equal, {@code false} otherwise
     */
    public boolean equals(MethodIdentifier other) {
        return this == other || other != null && hashCode == other.hashCode && name.equals(other.name) && Arrays.equals(parameterTypes, other.parameterTypes);
    }

    /**
     * Get the hash code for this method identifier.  The hash code is equal to:
     * <pre>
     *    n * 7 + a
     * </pre>
     * where <em>n</em> is the method name's hash code and <em>a</em> is the result of calling
     * {@link Arrays#hashCode(Object[])} on the parameter type name list.
     *
     * @return the hash code
     */
    public int hashCode() {
        return hashCode;
    }

    /**
     * Look up a public method matching this method identifier using reflection.
     *
     * @param clazz the class to search
     * @return the method
     * @throws NoSuchMethodException if no such method exists
     * @throws ClassNotFoundException if one of the classes referenced by this identifier are not found in {@code clazz}'s
     *      class loader
     */
    public Method getPublicMethod(final Class<?> clazz) throws NoSuchMethodException, ClassNotFoundException {
        return clazz.getMethod(name, typesOf(parameterTypes, clazz.getClassLoader()));
    }

    /**
     * Look up a method matching this method identifier using reflection.
     * <p>
     * If two classes with the same name and parameter types exist then the behavoiur of
     * {@link Class#getDeclaredMethod(String, Class...)} is followed (that is if one methods return type is more specific than
     * the others then it is returned, otherwise it is not defined)
     *
     * @param clazz the class to search
     * @return the method
     * @throws NoSuchMethodException if no such method exists
     * @throws ClassNotFoundException if one of the classes referenced by this identifier are not found in {@code clazz}'s class
     * loader
     */
    public Method getMethod(final Class<?> clazz) throws NoSuchMethodException, ClassNotFoundException {
        Class<?>[] types = typesOf(parameterTypes, clazz.getClassLoader());
        Class<?> currentClass = clazz;
        List<Method> found = new ArrayList<Method>();
        while (currentClass != null) {
            Method[] methods = currentClass.getDeclaredMethods();
            for (Method method : methods) {
                if (method.getName().equals(name)) {
                    if (Arrays.equals(method.getParameterTypes(), types)) {
                        found.add(method);
                    }
                }
            }
            if (!found.isEmpty()) {
                int size = found.size();
                if (size == 1) {
                    return found.get(0);
                } else {
                    // we have to methods with the same name and parameters
                    // this is usually caused by covariant return types, and usually one of these methods will
                    // be a bridge method. We look for this simple case first as it is the most common
                    if (size == 2) {
                        Method m1 = found.get(0);
                        Method m2 = found.get(1);
                        if (m1.isBridge() && !m2.isBridge()) {
                            return m2;
                        } else if (!m1.isBridge() && m2.isBridge()) {
                            return m1;
                        }
                    }
                    // now we want to emulate getDeclaredMethods behaviour
                    // if one of the methods has a return type that is more specific that any of
                    // the other methods then it will be returned, otherwise a method is chosen at random
                    Method returnMethod = found.get(0);
                    for (int i = 1; i < size; ++i) {
                        Method compareMethod = found.get(i);
                        if (compareMethod.getReturnType().isAssignableFrom(returnMethod.getReturnType())) {
                            continue;
                        } else if (returnMethod.getReturnType().isAssignableFrom(compareMethod.getReturnType())) {
                            returnMethod = compareMethod;
                        } else {
                            // we just return one at random
                            return returnMethod;
                        }
                    }
                    return returnMethod;
                }
            }
            currentClass = currentClass.getSuperclass();
        }
        throw new NoSuchMethodException(toString() + " does not exist on " + clazz);
    }

    /**
     * Get the human-readable representation of this identifier.
     *
     * @return the string
     */
    public String toString() {
        final StringBuilder b = new StringBuilder();
        b.append("Method ").append(name).append('(');
        for (String type : parameterTypes) {
            b.append(type);
        }
        return b.append(')').toString();
    }

    private void readObject(final ObjectInputStream ois) throws ClassNotFoundException, IOException {
        ois.defaultReadObject();
        hashCodeSetter.setInt(this, calculateHash(name, parameterTypes));
    }

    /**
     * Get an identifier for the given reflection method.
     *
     * @param method the method
     * @return the identifier
     */
    public static MethodIdentifier getIdentifierForMethod(final Method method) {
        return new MethodIdentifier(method);
    }

    /**
     * Construct a new instance using class objects for the parameter types.
     *
     * @param name the method name
     * @param parameterTypes the method parameter types
     * @return the identifier
     */
    public static MethodIdentifier getIdentifier(final String name, final Class<?>... parameterTypes) {
        return new MethodIdentifier(name, namesOf(parameterTypes));
    }

    /**
     * The method identifier for {@code Object.equals()}.
     */
    public static final MethodIdentifier EQUALS = getIdentifier("equals", Object.class);
    /**
     * The method identifier for {@code Object.hashCode()}.
     */
    public static final MethodIdentifier HASH_CODE = getIdentifier("hashCode");
    /**
     * The method identifier for {@code Object.toString()}.
     */
    public static final MethodIdentifier TO_STRING = getIdentifier("toString");
}
