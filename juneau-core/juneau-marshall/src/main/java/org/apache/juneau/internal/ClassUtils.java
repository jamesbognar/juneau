// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.internal;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.utils.*;

/**
 * Class-related utility methods.
 */
public final class ClassUtils {

	private static final Map<Class<?>,ConstructorCacheEntry> CONSTRUCTOR_CACHE = new ConcurrentHashMap<>();
	private static final Map<Class<?>,Stringify<?>> STRINGIFY_CACHE = new ConcurrentHashMap<>();
	
	// Special cases.
	static {
		
		// TimeZone doesn't follow any standard conventions.
		STRINGIFY_CACHE.put(TimeZone.class, new Stringify<TimeZone>(TimeZone.class){
			@Override 
			public TimeZone fromString(String s) {
				return TimeZone.getTimeZone(s);
			}
			@Override 
			public String toString(TimeZone tz) {
				return tz.getID();
			}
		});
		
		// Locale(String) doesn't work on strings like "ja_JP".
		STRINGIFY_CACHE.put(Locale.class, new Stringify<Locale>(Locale.class){
			@Override 
			public Locale fromString(String s) {
				return Locale.forLanguageTag(s.replace('_', '-'));
			}
		});
	}
	
	/**
	 * Given the specified list of objects, return readable names for the class types of the objects.
	 * 
	 * @param o The objects.
	 * @return An array of readable class type strings.
	 */
	public static ObjectList getReadableClassNames(Object[] o) {
		ObjectList l = new ObjectList();
		for (int i = 0; i < o.length; i++)
			l.add(o[i] == null ? "null" : getReadableClassName(o[i].getClass()));
		return l;
	}

	/**
	 * Shortcut for calling <code><jsm>getReadableClassName</jsm>(c.getName())</code>
	 * 
	 * @param c The class.
	 * @return A readable class type name, or <jk>null</jk> if parameter is <jk>null</jk>.
	 */
	public static String getReadableClassName(Class<?> c) {
		if (c == null)
			return null;
		return getReadableClassName(c.getName());
	}

	/**
	 * Shortcut for calling <code><jsm>getReadableClassName</jsm>(c.getClass().getName())</code>
	 * 
	 * @param o The object whose class we want to render.
	 * @return A readable class type name, or <jk>null</jk> if parameter is <jk>null</jk>.
	 */
	public static String getReadableClassNameForObject(Object o) {
		if (o == null)
			return null;
		return getReadableClassName(o.getClass().getName());
	}

	/**
	 * Converts the specified class name to a readable form when class name is a special construct like <js>"[[Z"</js>.
	 * 
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<jsm>getReadableClassName</jsm>(<js>"java.lang.Object"</js>);  <jc>// Returns "java.lang.Object"</jc>
	 * 	<jsm>getReadableClassName</jsm>(<js>"boolean"</js>);  <jc>// Returns "boolean"</jc>
	 * 	<jsm>getReadableClassName</jsm>(<js>"[Z"</js>);  <jc>// Returns "boolean[]"</jc>
	 * 	<jsm>getReadableClassName</jsm>(<js>"[[Z"</js>);  <jc>// Returns "boolean[][]"</jc>
	 * 	<jsm>getReadableClassName</jsm>(<js>"[Ljava.lang.Object;"</js>);  <jc>// Returns "java.lang.Object[]"</jc>
	 * 	<jsm>getReadableClassName</jsm>(<jk>null</jk>);  <jc>// Returns null</jc>
	 * </p>
	 * 
	 * @param className The class name.
	 * @return A readable class type name, or <jk>null</jk> if parameter is <jk>null</jk>.
	 */
	public static String getReadableClassName(String className) {
		if (className == null)
			return null;
		if (! StringUtils.startsWith(className, '['))
			return className;
		int depth = 0;
		for (int i = 0; i < className.length(); i++) {
			if (className.charAt(i) == '[')
				depth++;
			else
				break;
		}
		char type = className.charAt(depth);
		String c;
		switch (type) {
			case 'Z': c = "boolean"; break;
			case 'B': c = "byte"; break;
			case 'C': c = "char"; break;
			case 'D': c = "double"; break;
			case 'F': c = "float"; break;
			case 'I': c = "int"; break;
			case 'J': c = "long"; break;
			case 'S': c = "short"; break;
			default: c = className.substring(depth+1, className.length()-1);
		}
		StringBuilder sb = new StringBuilder(c.length() + 2*depth).append(c);
		for (int i = 0; i < depth; i++)
			sb.append("[]");
		return sb.toString();
	}

	/**
	 * Converts the string generated by {@link #getReadableClassName(Class)} back into a {@link Class}.
	 * 
	 * <p>
	 * Generics are stripped from the string since they cannot be converted to a class.
	 * 
	 * @param cl The classloader to use to load the class.
	 * @param name The readable class name.
	 * @return The class object.
	 * @throws ClassNotFoundException
	 */
	public static Class<?> getClassFromReadableName(ClassLoader cl, String name) throws ClassNotFoundException {
		return cl.loadClass(name);
	}

	/**
	 * Returns <jk>true</jk> if <code>parent</code> is a parent class of <code>child</code>.
	 * 
	 * @param parent The parent class.
	 * @param child The child class.
	 * @param strict If <jk>true</jk> returns <jk>false</jk> if the classes are the same.
	 * @return <jk>true</jk> if <code>parent</code> is a parent class of <code>child</code>.
	 */
	public static boolean isParentClass(Class<?> parent, Class<?> child, boolean strict) {
		return parent.isAssignableFrom(child) && ((!strict) || ! parent.equals(child));
	}

	/**
	 * Returns <jk>true</jk> if <code>parent</code> is a parent class or the same as <code>child</code>.
	 * 
	 * @param parent The parent class.
	 * @param child The child class.
	 * @return <jk>true</jk> if <code>parent</code> is a parent class or the same as <code>child</code>.
	 */
	public static boolean isParentClass(Class<?> parent, Class<?> child) {
		return isParentClass(parent, child, false);
	}

	/**
	 * Returns <jk>true</jk> if <code>parent</code> is a parent class or the same as <code>child</code>.
	 * 
	 * @param parent The parent class.
	 * @param child The child class.
	 * @return <jk>true</jk> if <code>parent</code> is a parent class or the same as <code>child</code>.
	 */
	public static boolean isParentClass(Class<?> parent, Type child) {
		if (child instanceof Class)
			return isParentClass(parent, (Class<?>)child);
		return false;
	}

	/**
	 * Returns the signature of the specified method.
	 * 
	 * <p>
	 * For no-arg methods, the signature will be a simple string such as <js>"toString"</js>.
	 * For methods with one or more args, the arguments will be fully-qualified class names (e.g.
	 * <js>"append(java.util.StringBuilder,boolean)"</js>)
	 * 
	 * @param m The methods to get the signature on.
	 * @return The methods signature.
	 */
	public static String getMethodSignature(Method m) {
		StringBuilder sb = new StringBuilder(m.getName());
		Class<?>[] pt = m.getParameterTypes();
		if (pt.length > 0) {
			sb.append('(');
			for (int i = 0; i < pt.length; i++) {
				if (i > 0)
					sb.append(',');
				sb.append(getReadableClassName(pt[i]));
			}
			sb.append(')');
		}
		return sb.toString();
	}

	private static final Map<Class<?>, Class<?>>
		pmap1 = new HashMap<>(),
		pmap2 = new HashMap<>();
	static {
		pmap1.put(boolean.class, Boolean.class);
		pmap1.put(byte.class, Byte.class);
		pmap1.put(short.class, Short.class);
		pmap1.put(char.class, Character.class);
		pmap1.put(int.class, Integer.class);
		pmap1.put(long.class, Long.class);
		pmap1.put(float.class, Float.class);
		pmap1.put(double.class, Double.class);
		pmap2.put(Boolean.class, boolean.class);
		pmap2.put(Byte.class, byte.class);
		pmap2.put(Short.class, short.class);
		pmap2.put(Character.class, char.class);
		pmap2.put(Integer.class, int.class);
		pmap2.put(Long.class, long.class);
		pmap2.put(Float.class, float.class);
		pmap2.put(Double.class, double.class);
	}

	/**
	 * If the specified class is a primitive (e.g. <code><jk>int</jk>.<jk>class</jk></code>) returns it's wrapper class
	 * (e.g. <code>Integer.<jk>class</jk></code>).
	 * 
	 * @param c The class.
	 * @return The wrapper class, or <jk>null</jk> if class is not a primitive.
	 */
	public static Class<?> getPrimitiveWrapper(Class<?> c) {
		return pmap1.get(c);
	}

	/**
	 * If the specified class is a primitive wrapper (e.g. <code><jk>Integer</jk>.<jk>class</jk></code>) returns it's
	 * primitive class (e.g. <code>int.<jk>class</jk></code>).
	 * 
	 * @param c The class.
	 * @return The primitive class, or <jk>null</jk> if class is not a primitive wrapper.
	 */
	public static Class<?> getPrimitiveForWrapper(Class<?> c) {
		return pmap2.get(c);
	}

	/**
	 * If the specified class is a primitive (e.g. <code><jk>int</jk>.<jk>class</jk></code>) returns it's wrapper class
	 * (e.g. <code>Integer.<jk>class</jk></code>).
	 * 
	 * @param c The class.
	 * @return The wrapper class if it's primitive, or the same class if class is not a primitive.
	 */
	public static Class<?> getWrapperIfPrimitive(Class<?> c) {
		if (! c.isPrimitive())
			return c;
		return pmap1.get(c);
	}

	/**
	 * Returns <jk>true</jk> if the specified class has the {@link Deprecated @Deprecated} annotation on it.
	 * 
	 * @param c The class.
	 * @return <jk>true</jk> if the specified class has the {@link Deprecated @Deprecated} annotation on it.
	 */
	public static boolean isNotDeprecated(Class<?> c) {
		return ! c.isAnnotationPresent(Deprecated.class);
	}

	/**
	 * Returns <jk>true</jk> if the specified method has the {@link Deprecated @Deprecated} annotation on it.
	 * 
	 * @param m The method.
	 * @return <jk>true</jk> if the specified method has the {@link Deprecated @Deprecated} annotation on it.
	 */
	public static boolean isNotDeprecated(Method m) {
		return ! m.isAnnotationPresent(Deprecated.class);

	}

	/**
	 * Returns <jk>true</jk> if the specified constructor has the {@link Deprecated @Deprecated} annotation on it.
	 * 
	 * @param c The constructor.
	 * @return <jk>true</jk> if the specified constructor has the {@link Deprecated @Deprecated} annotation on it.
	 */
	public static boolean isNotDeprecated(Constructor<?> c) {
		return ! c.isAnnotationPresent(Deprecated.class);
	}

	/**
	 * Returns <jk>true</jk> if the specified class is public.
	 * 
	 * @param c The class.
	 * @return <jk>true</jk> if the specified class is public.
	 */
	public static boolean isPublic(Class<?> c) {
		return Modifier.isPublic(c.getModifiers());
	}

	/**
	 * Returns <jk>true</jk> if the specified class is public.
	 * 
	 * @param c The class.
	 * @return <jk>true</jk> if the specified class is public.
	 */
	public static boolean isStatic(Class<?> c) {
		return Modifier.isStatic(c.getModifiers());
	}

	/**
	 * Returns <jk>true</jk> if the specified class is abstract.
	 * 
	 * @param c The class.
	 * @return <jk>true</jk> if the specified class is abstract.
	 */
	public static boolean isAbstract(Class<?> c) {
		return Modifier.isAbstract(c.getModifiers());
	}

	/**
	 * Returns <jk>true</jk> if the specified method is public.
	 * 
	 * @param m The method.
	 * @return <jk>true</jk> if the specified method is public.
	 */
	public static boolean isPublic(Method m) {
		return Modifier.isPublic(m.getModifiers());
	}

	/**
	 * Returns <jk>true</jk> if the specified method is static.
	 * 
	 * @param m The method.
	 * @return <jk>true</jk> if the specified method is static.
	 */
	public static boolean isStatic(Method m) {
		return Modifier.isStatic(m.getModifiers());
	}

	/**
	 * Returns <jk>true</jk> if the specified constructor is public.
	 * 
	 * @param c The constructor.
	 * @return <jk>true</jk> if the specified constructor is public.
	 */
	public static boolean isPublic(Constructor<?> c) {
		return Modifier.isPublic(c.getModifiers());
	}

	/**
	 * Returns the specified annotation on the specified method.
	 * 
	 * <p>
	 * Similar to {@link Method#getAnnotation(Class)}, but searches up the parent hierarchy for the annotation
	 * defined on parent classes and interfaces.
	 * 
	 * <p>
	 * Normally, annotations defined on methods of parent classes and interfaces are not inherited by the child methods.
	 * This utility method gets around that limitation by searching the class hierarchy for the "same" method
	 * (i.e. the same name and arguments).
	 * 
	 * @param a The annotation to search for.
	 * @param m The method to search.
	 * @return The annotation, or <jk>null</jk> if it wasn't found.
	 */
	public static <T extends Annotation> T getMethodAnnotation(Class<T> a, Method m) {
		return getMethodAnnotation(a, m.getDeclaringClass(), m);
	}

	/**
	 * Returns the specified annotation on the specified method.
	 * 
	 * <p>
	 * Similar to {@link Method#getAnnotation(Class)}, but searches up the parent hierarchy for the annotation defined
	 * on parent classes and interfaces.
	 * 
	 * <p>
	 * Normally, annotations defined on methods of parent classes and interfaces are not inherited by the child methods.
	 * This utility method gets around that limitation by searching the class hierarchy for the "same" method
	 * (i.e. the same name and arguments).
	 * 
	 * @param a The annotation to search for.
	 * @param c
	 * 	The child class to start searching from.
	 * 	Note that it can be a descendant class of the actual declaring class of the method passed in.
	 * 	This allows you to find annotations on methods overridden by the method passed in.
	 * @param method The method to search.
	 * @return The annotation, or <jk>null</jk> if it wasn't found.
	 */
	public static <T extends Annotation> T getMethodAnnotation(Class<T> a, Class<?> c, Method method) {
		for (Method m : c.getDeclaredMethods()) {
			if (isSameMethod(method, m)) {
				T t = m.getAnnotation(a);
				if (t != null)
					return t;
			}
		}
		Class<?> pc = c.getSuperclass();
		if (pc != null) {
			T t = getMethodAnnotation(a, pc, method);
			if (t != null)
				return t;
		}
		for (Class<?> ic : c.getInterfaces()) {
			T t = getMethodAnnotation(a, ic, method);
			if (t != null)
				return t;
		}
		return null;
	}

	private static boolean isSameMethod(Method m1, Method m2) {
		return m1.getName().equals(m2.getName()) && Arrays.equals(m1.getParameterTypes(), m2.getParameterTypes());
	}

	/**
	 * Locates the no-arg constructor for the specified class.
	 * 
	 * <p>
	 * Constructor must match the visibility requirements specified by parameter 'v'.
	 * If class is abstract, always returns <jk>null</jk>.
	 * Note that this also returns the 1-arg constructor for non-static member classes.
	 * 
	 * @param c The class from which to locate the no-arg constructor.
	 * @param v The minimum visibility.
	 * @return The constructor, or <jk>null</jk> if no no-arg constructor exists with the required visibility.
	 */
	@SuppressWarnings({"rawtypes","unchecked"})
	public static final <T> Constructor<T> findNoArgConstructor(Class<T> c, Visibility v) {
		int mod = c.getModifiers();
		if (Modifier.isAbstract(mod))
			return null;
		boolean isMemberClass = c.isMemberClass() && ! isStatic(c);
		for (Constructor cc : c.getConstructors()) {
			mod = cc.getModifiers();
			if (cc.getParameterTypes().length == (isMemberClass ? 1 : 0) && v.isVisible(mod) && isNotDeprecated(cc))
				return v.transform(cc);
		}
		return null;
	}

	/**
	 * Finds the real parameter type of the specified class.
	 * 
	 * @param c The class containing the parameters (e.g. PojoSwap&lt;T,S&gt;)
	 * @param index The zero-based index of the parameter to resolve.
	 * @param oc The class we're trying to resolve the parameter type for.
	 * @return The resolved real class.
	 */
	public static Class<?> resolveParameterType(Class<?> c, int index, Class<?> oc) {

		// We need to make up a mapping of type names.
		Map<Type,Type> typeMap = new HashMap<>();
		while (c != oc.getSuperclass()) {
			extractTypes(typeMap, oc);
			oc = oc.getSuperclass();
		}

		Type gsc = oc.getGenericSuperclass();
		
		// Not actually a parameterized type.
		if (! (gsc instanceof ParameterizedType))
			return Object.class;

		ParameterizedType opt = (ParameterizedType)gsc;
		Type actualType = opt.getActualTypeArguments()[index];

		if (typeMap.containsKey(actualType))
			actualType = typeMap.get(actualType);

		if (actualType instanceof Class) {
			return (Class<?>)actualType;

		} else if (actualType instanceof GenericArrayType) {
			Class<?> cmpntType = (Class<?>)((GenericArrayType)actualType).getGenericComponentType();
			return Array.newInstance(cmpntType, 0).getClass();

		} else if (actualType instanceof TypeVariable) {
			TypeVariable<?> typeVariable = (TypeVariable<?>)actualType;
			List<Class<?>> nestedOuterTypes = new LinkedList<>();
			for (Class<?> ec = oc.getEnclosingClass(); ec != null; ec = ec.getEnclosingClass()) {
				try {
					Class<?> outerClass = oc.getClass();
					nestedOuterTypes.add(outerClass);
					Map<Type,Type> outerTypeMap = new HashMap<>();
					extractTypes(outerTypeMap, outerClass);
					for (Map.Entry<Type,Type> entry : outerTypeMap.entrySet()) {
						Type key = entry.getKey(), value = entry.getValue();
						if (key instanceof TypeVariable) {
							TypeVariable<?> keyType = (TypeVariable<?>)key;
							if (keyType.getName().equals(typeVariable.getName()) && isInnerClass(keyType.getGenericDeclaration(), typeVariable.getGenericDeclaration())) {
								if (value instanceof Class)
									return (Class<?>)value;
								typeVariable = (TypeVariable<?>)entry.getValue();
							}
						}
					}
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
			throw new FormattedRuntimeException("Could not resolve type: {0}", actualType);
		} else {
			throw new FormattedRuntimeException("Invalid type found in resolveParameterType: {0}", actualType);
		}
	}

	private static boolean isInnerClass(GenericDeclaration od, GenericDeclaration id) {
		if (od instanceof Class && id instanceof Class) {
			Class<?> oc = (Class<?>)od;
			Class<?> ic = (Class<?>)id;
			while ((ic = ic.getEnclosingClass()) != null)
				if (ic == oc)
					return true;
		}
		return false;
	}

	private static void extractTypes(Map<Type,Type> typeMap, Class<?> c) {
		Type gs = c.getGenericSuperclass();
		if (gs instanceof ParameterizedType) {
			ParameterizedType pt = (ParameterizedType)gs;
			Type[] typeParameters = ((Class<?>)pt.getRawType()).getTypeParameters();
			Type[] actualTypeArguments = pt.getActualTypeArguments();
			for (int i = 0; i < typeParameters.length; i++) {
				if (typeMap.containsKey(actualTypeArguments[i]))
					actualTypeArguments[i] = typeMap.get(actualTypeArguments[i]);
				typeMap.put(typeParameters[i], actualTypeArguments[i]);
			}
		}
	}

	/**
	 * Finds a public method with the specified parameters.
	 * 
	 * @param c The class to look for the method.
	 * @param name The method name.
	 * @param returnType
	 * 	The return type of the method.
	 * 	Can be a super type of the actual return type.
	 * 	For example, if the actual return type is <code>CharSequence</code>, then <code>Object</code> will match but
	 * 	<code>String</code> will not.
	 * @param argTypes
	 * 	The argument types of the method.
	 * 	Can be subtypes of the actual parameter types.
	 * 	For example, if the parameter type is <code>CharSequence</code>, then <code>String</code> will match but
	 * 	<code>Object</code> will not.
	 * @return The matched method, or <jk>null</jk> if no match was found.
	 */
	public static Method findPublicMethod(Class<?> c, String name, Class<?> returnType, Class<?>...argTypes) {
		for (Method m : c.getMethods()) {
			if (isPublic(m) && m.getName().equals(name)) {
				Class<?> rt = m.getReturnType();
				if (isParentClass(returnType, rt) && argsMatch(m.getParameterTypes(), argTypes)) 
					return m;
			}
		}
		return null;
	}

	/**
	 * Finds a public constructor with the specified parameters without throwing an exception.
	 * 
	 * @param c The class to search for a constructor.
	 * @param fuzzyArgs 
	 * 	Use fuzzy-arg matching.
	 * 	Find a constructor that best matches the specified args.
	 * @param argTypes
	 * 	The argument types in the constructor.
	 * 	Can be subtypes of the actual constructor argument types.
	 * @return The matching constructor, or <jk>null</jk> if constructor could not be found.
	 */
	@SuppressWarnings("unchecked")
	public static <T> Constructor<T> findPublicConstructor(Class<T> c, boolean fuzzyArgs, Class<?>...argTypes) {
		ConstructorCacheEntry cce = CONSTRUCTOR_CACHE.get(c);
		if (cce != null && argsMatch(cce.paramTypes, argTypes)) 
			return (Constructor<T>)cce.constructor;
	
		if (fuzzyArgs) {
			int bestCount = -1;
			Constructor<?> bestMatch = null;
			for (Constructor<?> n : c.getConstructors()) {
				int m = fuzzyArgsMatch(n.getParameterTypes(), argTypes);
				if (m > bestCount) {
					bestCount = m;
					bestMatch = n;
				}
			}
			if (bestCount >= 0) 
				CONSTRUCTOR_CACHE.put(c, new ConstructorCacheEntry(c, bestMatch));
			return (Constructor<T>)bestMatch;
		} 
		
		for (Constructor<?> n : c.getConstructors()) {
			if (argsMatch(n.getParameterTypes(), argTypes)) {
				CONSTRUCTOR_CACHE.put(c, new ConstructorCacheEntry(c, n));
				return (Constructor<T>)n;
			}
		}

		return null;
	}
	
	private static final class ConstructorCacheEntry {
		final Constructor<?> constructor;
		final Class<?>[] paramTypes;
		
		ConstructorCacheEntry(Class<?> forClass, Constructor<?> constructor) {
			this.constructor = constructor;
			this.paramTypes = constructor.getParameterTypes();
		}
	}
	
	/**
	 * Returns <jk>true</jk> if the specified argument types are valid for the specified parameter types.
	 * 
	 * @param paramTypes The parameters types specified on a method.
	 * @param argTypes The class types of the arguments being passed to the method.
	 * @return <jk>true</jk> if the arguments match the parameters.
	 */
	public static boolean argsMatch(Class<?>[] paramTypes, Class<?>[] argTypes) {
		if (paramTypes.length == argTypes.length) {
			for (int i = 0; i < paramTypes.length; i++) 
				if (! isParentClass(paramTypes[i], argTypes[i])) 
					return false;
			return true;
		}
		return false;
	}
	
	/**
	 * Returns a number representing the number of arguments that match the specified parameters.
	 * 
	 * @param paramTypes The parameters types specified on a method.
	 * @param argTypes The class types of the arguments being passed to the method.
	 * @return The number of matching arguments, or <code>-1</code> a parameter was found that isn't in the list of args.
	 */
	public static int fuzzyArgsMatch(Class<?>[] paramTypes, Class<?>[] argTypes) {
		int matches = 0;
		outer: for (Class<?> p : paramTypes) {
			p = getWrapperIfPrimitive(p);
			for (Class<?> a : argTypes) {
				if (isParentClass(p, a)) {
					matches++;
					continue outer;				
				}
			}
			return -1;
		}
		return matches;
	}

	/**
	 * Finds the public constructor that can take in the specified arguments.
	 * 
	 * @param c The class we're trying to construct.
	 * @param args The arguments we want to pass into the constructor.
	 * @return
	 * 	The constructor, or <jk>null</jk> if a public constructor could not be found that takes in the specified
	 * 	arguments.
	 */
	public static <T> Constructor<T> findPublicConstructor(Class<T> c, Object...args) {
		return findPublicConstructor(c, false, getClasses(args));
	}

	/**
	 * Finds the public constructor that can take in the specified arguments.
	 * 
	 * @param c The class we're trying to construct.
	 * @param args The argument types we want to pass into the constructor.
	 * @return
	 * 	The constructor, or <jk>null</jk> if a public constructor could not be found that takes in the specified
	 * 	arguments.
	 */
	public static <T> Constructor<T> findPublicConstructor(Class<T> c, Class<?>...args) {
		return findPublicConstructor(c, false, args);
	}

	/**
	 * Finds the public constructor that can take in the specified arguments.
	 * 
	 * @param c The class we're trying to construct.
	 * @param fuzzyArgs 
	 * 	Use fuzzy-arg matching.
	 * 	Find a constructor that best matches the specified args.
	 * @param args The arguments we want to pass into the constructor.
	 * @return
	 * 	The constructor, or <jk>null</jk> if a public constructor could not be found that takes in the specified
	 * 	arguments.
	 */
	public static <T> Constructor<T> findPublicConstructor(Class<T> c, boolean fuzzyArgs, Object...args) {
		return findPublicConstructor(c, fuzzyArgs, getClasses(args));
	}

	/**
	 * Returns the class types for the specified arguments.
	 * 
	 * @param args The objects we're getting the classes of.
	 * @return The classes of the arguments.
	 */
	public static Class<?>[] getClasses(Object...args) {
		Class<?>[] pt = new Class<?>[args.length];
		for (int i = 0; i < args.length; i++)
			pt[i] = args[i] == null ? null : args[i].getClass();
		return pt;
	}

	/**
	 * Returns a {@link MethodInfo} bean that describes the specified method.
	 * 
	 * @param m The method to describe.
	 * @return The bean with information about the method.
	 */
	public static MethodInfo getMethodInfo(Method m) {
		return new MethodInfo(m);
	}

	/**
	 * Returns {@link MethodInfo} beans that describe the specified methods.
	 * 
	 * @param m The methods to describe.
	 * @return The beans with information about the methods.
	 */
	public static MethodInfo[] getMethodInfo(Collection<Method> m) {
		MethodInfo[] mi = new MethodInfo[m.size()];
		int i = 0;
		for (Method mm : m)
			mi[i++] = getMethodInfo(mm);
		return mi;
	}

	/**
	 * Simple bean that shows the name, parameter types, and return type of a method.
	 */
	@SuppressWarnings("javadoc")
	public static class MethodInfo {
		public final String methodName;
		public final String[] parameterTypes;
		public final String returnType;

		MethodInfo(Method m) {
			methodName = m.getName();
			Type[] pt = m.getGenericParameterTypes();
			parameterTypes = new String[pt.length];
			for (int i  = 0; i < pt.length; i++)
				parameterTypes[i] = BeanContext.DEFAULT.getClassMeta(pt[i]).toString();
			returnType = BeanContext.DEFAULT.getClassMeta(m.getGenericReturnType()).toString();
		}
	}

	/**
	 * Creates an instance of the specified class.
	 * 
	 * @param c 
	 * 	The class to cast to.
	 * @param c2
	 * 	The class to instantiate.
	 * 	Can also be an instance of the class.
	 * @return 
	 * 	The new class instance, or <jk>null</jk> if the class was <jk>null</jk> or is abstract or an interface.
	 * @throws 
	 * 	RuntimeException if constructor could not be found or called.
	 */
	public static <T> T newInstance(Class<T> c, Object c2) {
		return newInstanceFromOuter(null, c, c2, false);
	}

	/**
	 * Creates an instance of the specified class.
	 * 
	 * @param c 
	 * 	The class to cast to.
	 * @param c2
	 * 	The class to instantiate.
	 * 	Can also be an instance of the class.
	 * @param fuzzyArgs 
	 * 	Use fuzzy constructor arg matching.  
	 * 	<br>When <jk>true</jk>, constructor args can be in any order and extra args are ignored.
	 * 	<br>No-arg constructors are also used if no other constructors are found.
	 * @param args 
	 * 	The arguments to pass to the constructor.
	 * @return 
	 * 	The new class instance, or <jk>null</jk> if the class was <jk>null</jk> or is abstract or an interface.
	 * @throws 
	 * 	RuntimeException if constructor could not be found or called.
	 */
	public static <T> T newInstance(Class<T> c, Object c2, boolean fuzzyArgs, Object...args) {
		return newInstanceFromOuter(null, c, c2, fuzzyArgs, args);
	}

	/**
	 * Creates an instance of the specified class from within the context of another object.
	 * 
	 * @param outer
	 * 	The outer object.
	 * 	Can be <jk>null</jk>.
	 * @param c 
	 * 	The class to cast to.
	 * @param c2
	 * 	The class to instantiate.
	 * 	Can also be an instance of the class.
	 * @param fuzzyArgs 
	 * 	Use fuzzy constructor arg matching.  
	 * 	<br>When <jk>true</jk>, constructor args can be in any order and extra args are ignored.
	 * 	<br>No-arg constructors are also used if no other constructors are found.
	 * @param args 
	 * 	The arguments to pass to the constructor.
	 * @return 
	 * 	The new class instance, or <jk>null</jk> if the class was <jk>null</jk> or is abstract or an interface.
	 * @throws 
	 * 	RuntimeException if constructor could not be found or called.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T newInstanceFromOuter(Object outer, Class<T> c, Object c2, boolean fuzzyArgs, Object...args) {
		if (c2 == null)
			return null;
		if (c2 instanceof Class) {
			try {
				Class<?> c3 = (Class<?>)c2;
				if (c3.isInterface() || isAbstract(c3))
					return null;

				// First look for an exact match.
				Constructor<?> con = findPublicConstructor(c3, false, args);
				if (con != null)
					return (T)con.newInstance(args);

				// Next look for an exact match including the outer.
				if (outer != null) {
					args = new AList<>().append(outer).appendAll(args).toArray();
					con = findPublicConstructor(c3, false, args);
					if (con != null)
						return (T)con.newInstance(args);
				}
				
				// Finally use fuzzy matching.
				if (fuzzyArgs) {
					con = findPublicConstructor(c3, true, args);
					if (con != null)
						return (T)con.newInstance(getMatchingArgs(con, args));
				}

				throw new FormattedRuntimeException("Could not instantiate class {0}/{1}.  Constructor not found.", c.getName(), c2);
			} catch (Exception e) {
				throw new FormattedRuntimeException(e, "Could not instantiate class {0}", c.getName());
			}
		} else if (isParentClass(c, c2.getClass())) {
			return (T)c2;
		} else {
			throw new FormattedRuntimeException("Object of type {0} found but was expecting {1}.", c2.getClass(), c.getClass());
		}
	}

	private static Object[] getMatchingArgs(Constructor<?> con, Object[] args) {
		Class<?>[] paramTypes = con.getParameterTypes();
		Object[] params = new Object[paramTypes.length];
		for (int i = 0; i < paramTypes.length; i++) {
			Class<?> pt = getWrapperIfPrimitive(paramTypes[i]);
			for (int j = 0; j < args.length; j++) {
				if (isParentClass(pt, args[j].getClass())) {
					params[i] = args[j];
					break;
				}
			}
		}
		return params;
	}

	/**
	 * Returns all the fields in the specified class and all parent classes.
	 * 
	 * <p>
	 * Fields are ordered in either parent-to-child, or child-to-parent order, then alphabetically.
	 * 
	 * @param c The class to get all fields on.
	 * @param parentFirst Order them in parent-class-to-child-class order, otherwise child-class-to-parent-class order.
	 * @return An iterable of all fields in the specified class.
	 */
	@SuppressWarnings("rawtypes")
	public static Iterable<Field> getAllFields(final Class c, final boolean parentFirst) {
		return new Iterable<Field>() {
			@Override
			public Iterator<Field> iterator() {
				return new Iterator<Field>(){
					final Iterator<Class<?>> classIterator = getParentClasses(c, parentFirst, false);
					Field[] fields = classIterator.hasNext() ? sort(classIterator.next().getDeclaredFields()) : new Field[0];
					int fIndex = 0;
					Field next;

					@Override
					public boolean hasNext() {
						prime();
						return next != null;
					}

					private void prime() {
						if (next == null) {
							while (fIndex >= fields.length) {
								if (classIterator.hasNext()) {
									fields = sort(classIterator.next().getDeclaredFields());
									fIndex = 0;
								} else {
									fIndex = -1;
								}
			 				}
							if (fIndex != -1)
								next = fields[fIndex++];
						}
					}

					@Override
					public Field next() {
						prime();
						Field f = next;
						next = null;
						return f;
					}

					@Override
					public void remove() {
					}
				};
			}
		};
	}

	/**
	 * Returns all the methods in the specified class and all parent classes.
	 * 
	 * <p>
	 * Methods are ordered in either parent-to-child, or child-to-parent order, then alphabetically.
	 * 
	 * @param c The class to get all methods on.
	 * @param parentFirst Order them in parent-class-to-child-class order, otherwise child-class-to-parent-class order.
	 * @return An iterable of all methods in the specified class.
	 */
	@SuppressWarnings("rawtypes")
	public static Iterable<Method> getAllMethods(final Class c, final boolean parentFirst) {
		return new Iterable<Method>() {
			@Override
			public Iterator<Method> iterator() {
				return new Iterator<Method>(){
					final Iterator<Class<?>> classIterator = getParentClasses(c, parentFirst, true);
					Method[] methods = classIterator.hasNext() ? sort(classIterator.next().getDeclaredMethods()) : new Method[0];
					int mIndex = 0;
					Method next;

					@Override
					public boolean hasNext() {
						prime();
						return next != null;
					}

					private void prime() {
						if (next == null) {
							while (mIndex >= methods.length) {
								if (classIterator.hasNext()) {
									methods = sort(classIterator.next().getDeclaredMethods());
									mIndex = 0;
								} else {
									mIndex = -1;
								}
			 				}
							if (mIndex != -1)
								next = methods[mIndex++];
						}
					}

					@Override
					public Method next() {
						prime();
						Method m = next;
						next = null;
						return m;
					}

					@Override
					public void remove() {
					}
				};
			}
		};
	}

	private static Comparator<Method> METHOD_COMPARATOR = new Comparator<Method>() {

		@Override
		public int compare(Method o1, Method o2) {
			int i = o1.getName().compareTo(o2.getName());
			if (i == 0) {
				i = o1.getParameterTypes().length - o2.getParameterTypes().length;
				if (i == 0) {
					for (int j = 0; j < o1.getParameterTypes().length && i == 0; j++) {
						i = o1.getParameterTypes()[j].getName().compareTo(o2.getParameterTypes()[j].getName());
					}
				}
			}
			return i;
		}
	};

	/**
	 * Sorts methods in alphabetical order.
	 * 
	 * @param m The methods to sort.
	 * @return The same array, but with elements sorted.
	 */
	public static Method[] sort(Method[] m) {
		Arrays.sort(m, METHOD_COMPARATOR);
		return m;
	}

	private static Comparator<Field> FIELD_COMPARATOR = new Comparator<Field>() {

		@Override
		public int compare(Field o1, Field o2) {
			return o1.getName().compareTo(o2.getName());
		}
	};

	/**
	 * Sorts methods in alphabetical order.
	 * 
	 * @param m The methods to sort.
	 * @return The same array, but with elements sorted.
	 */
	public static Field[] sort(Field[] m) {
		Arrays.sort(m, FIELD_COMPARATOR);
		return m;
	}

	/**
	 * Returns a list of all the parent classes of the specified class including the class itself.
	 * 
	 * @param c The class to retrieve the parent classes.
	 * @param parentFirst In parent-to-child order, otherwise child-to-parent.
	 * @param includeInterfaces Include interfaces.
	 * @return An iterator of parent classes in the class hierarchy.
	 */
	public static Iterator<Class<?>> getParentClasses(final Class<?> c, boolean parentFirst, boolean includeInterfaces) {
		List<Class<?>> l = getParentClasses(new ArrayList<Class<?>>(), c, parentFirst, includeInterfaces);
		return l.iterator();
	}

	private static List<Class<?>> getParentClasses(List<Class<?>> l, Class<?> c, boolean parentFirst, boolean includeInterfaces) {
		if (parentFirst) {
			if (includeInterfaces)
				for (Class<?> i : c.getInterfaces())
					l.add(i);
			if (c.getSuperclass() != Object.class && c.getSuperclass() != null)
				getParentClasses(l, c.getSuperclass(), parentFirst, includeInterfaces);
			l.add(c);
		} else {
			l.add(c);
			if (c.getSuperclass() != Object.class && c.getSuperclass() != null)
				getParentClasses(l, c.getSuperclass(), parentFirst, includeInterfaces);
			if (includeInterfaces)
				for (Class<?> i : c.getInterfaces())
					l.add(i);
		}
		return l;
	}

	/**
	 * Returns the default value for the specified primitive class.
	 * 
	 * @param primitiveClass The primitive class to get the default value for.
	 * @return The default value, or <jk>null</jk> if the specified class is not a primitive class.
	 */
	public static Object getPrimitiveDefault(Class<?> primitiveClass) {
		return primitiveDefaultMap.get(primitiveClass);
	}

	private static final Map<Class<?>,Object> primitiveDefaultMap = Collections.unmodifiableMap(
		new AMap<Class<?>,Object>()
			.append(Boolean.TYPE, false)
			.append(Character.TYPE, (char)0)
			.append(Short.TYPE, (short)0)
			.append(Integer.TYPE, 0)
			.append(Long.TYPE, 0l)
			.append(Float.TYPE, 0f)
			.append(Double.TYPE, 0d)
			.append(Byte.TYPE, (byte)0)
			.append(Boolean.class, false)
			.append(Character.class, (char)0)
			.append(Short.class, (short)0)
			.append(Integer.class, 0)
			.append(Long.class, 0l)
			.append(Float.class, 0f)
			.append(Double.class, 0d)
			.append(Byte.class, (byte)0)
	);

	/**
	 * Returns a readable representation of the specified method.
	 * 
	 * <p>
	 * The format of the string is <js>"full-qualified-class.method-name(parameter-simple-class-names)"</js>.
	 * 
	 * @param m The method to stringify.
	 * @return The stringified method.
	 */
	public static String toString(Method m) {
		StringBuilder sb = new StringBuilder(m.getDeclaringClass().getName() + "." + m.getName() + "(");
		for (int i = 0; i < m.getParameterTypes().length; i++) {
			if (i > 0)
				sb.append(",");
			sb.append(m.getParameterTypes()[i].getSimpleName());
		}
		sb.append(")");
		return sb.toString();
	}

	/**
	 * Returns a readable representation of the specified field.
	 * 
	 * <p>
	 * The format of the string is <js>"full-qualified-class.field-name"</js>.
	 * 
	 * @param f The field to stringify.
	 * @return The stringified field.
	 */
	public static String toString(Field f) {
		return f.getDeclaringClass().getName() + "." + f.getName();
	}

	/**
	 * Throws an {@link IllegalArgumentException} if the parameters on the method are not in the specified list provided.
	 * 
	 * @param m The method to test.
	 * @param args The valid class types (exact) for the arguments.
	 * @throws FormattedIllegalArgumentException If any of the parameters on the method weren't in the list.
	 */
	public static void assertArgsOfType(Method m, Class<?>...args) throws FormattedIllegalArgumentException {
		for (Class<?> c1 : m.getParameterTypes()) {
			boolean foundMatch = false;
			for (Class<?> c2 : args)
				if (c1 == c2)
					foundMatch = true;
			if (! foundMatch)
				throw new FormattedIllegalArgumentException("Invalid argument of type {0} passed in method {1}.  Only arguments of type {2} are allowed.", c1, m, args);
		}
	}
	
	/**
	 * Finds the public static "fromString" method on the specified class.
	 * 
	 * <p>
	 * Looks for the following method names:
	 * <ul>
	 * 	<li><code>fromString</code>
	 * 	<li><code>fromValue</code>
	 * 	<li><code>valueOf</code>
	 * 	<li><code>parse</code>
	 * 	<li><code>parseString</code>
	 * 	<li><code>forName</code>
	 * 	<li><code>forString</code>
	 * </ul>
	 * 
	 * @param c The class to find the method on.
	 * @return The static method, or <jk>null</jk> if it couldn't be found.
	 */
	public static Method findPublicFromStringMethod(Class<?> c) {
		for (String methodName : new String[]{"create","fromString","fromValue","valueOf","parse","parseString","forName","forString"}) {
			for (Method m : c.getMethods()) {
				if (isStatic(m) && isPublic(m) && isNotDeprecated(m)) {
					String mName = m.getName();
					if (mName.equals(methodName) && m.getReturnType() == c) {
						Class<?>[] args = m.getParameterTypes();
						if (args.length == 1 && args[0] == String.class) {
							return m;
						}
					}
				}
			}
		}
		return null;
	}
	
	/**
	 * Constructs a new instance of the specified class from the specified string.
	 * 
	 * <p>
	 * Class must be one of the following:
	 * <ul>
	 * 	<li>Have a public constructor that takes in a single <code>String</code> argument.
	 * 	<li>Have a static <code>fromString(String)</code> (or related) method.
	 * 		<br>See {@link #findPublicFromStringMethod(Class)} for the list of possible static method names.
	 * 	<li>Be an <code>enum</code>.
	 * </ul>
	 * 
	 * @param c The class.
	 * @param s The string to create the instance from.
	 * @return A new object instance, or <jk>null</jk> if a method for converting the string to an object could not be found.
	 */
	@SuppressWarnings({ "unchecked" })
	public static <T> T fromString(Class<T> c, String s) {
		return (T)getStringify(c).fromString(s);
	}
	
	/**
	 * Converts an object to a string.
	 * 
	 * <p>
	 * Normally, this is just going to call <code>toString()</code> on the object.
	 * However, the {@link Locale} and {@link TimeZone} objects are treated special so that the returned value
	 * works with the {@link #fromString(Class, String)} method. 
	 * 
	 * @param o The object to convert to a string.
	 * @return The stringified object, or <jk>null</jk> if the object was <jk>null</jk>.
	 */
	@SuppressWarnings({ "unchecked" })
	public static String toString(Object o) {
		if (o == null)
			return null;
		return getStringify(o.getClass()).toString(o);
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static Stringify getStringify(Class c) {
		Stringify fs = STRINGIFY_CACHE.get(c);
		if (fs == null) {
			for (Iterator<Class<?>> i = getParentClasses(c, false, true); i.hasNext(); ) {
				Class c2 = i.next();
				fs = STRINGIFY_CACHE.get(c2);
				if (fs != null) {
					STRINGIFY_CACHE.put(c, fs);
					break;
				}
			}
			if (fs == null) {
				fs = new Stringify(c);
				STRINGIFY_CACHE.put(c, fs);
			}
		}
		return fs;
	}
	
	@SuppressWarnings({"unchecked","rawtypes"})
	private static class Stringify<T> {
		final Constructor<?> constructor;
		final Method fromStringMethod;
		final Class<? extends Enum> enumClass;
		
		Stringify(Class<?> c) {
			enumClass = c.isEnum() ? (Class<? extends Enum>)c : null;
			fromStringMethod = enumClass != null ? null : findPublicFromStringMethod(c);
			constructor = enumClass != null || fromStringMethod != null ? null : findPublicConstructor(c, String.class);
		}
		
		public T fromString(String s) {
			try {
				if (fromStringMethod != null)
					return (T)fromStringMethod.invoke(null, s);
				if (constructor != null)
					return (T)constructor.newInstance(s);
				if (enumClass != null)
					return (T)Enum.valueOf(enumClass, s);
				return null;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		public String toString(T t) {
			return t.toString(); 
		}
	}
}
