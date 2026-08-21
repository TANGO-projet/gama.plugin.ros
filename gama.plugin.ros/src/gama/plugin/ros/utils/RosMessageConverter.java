package gama.plugin.ros.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import us.ihmc.jros2.ROS2Message;

/**
 * Utility class for converting jros2 message objects to/from GAMA-friendly
 * representations (plain Java {@link Map} and {@link List}).
 *
 * <p><b>Serialization strategy</b>: each ROS2 message class exposes public
 * {@code getXxx()} methods for its fields.  We iterate them via reflection,
 * skip {@code getClass()} / {@code hashCode()} / {@code toString()}, and
 * recursively convert nested messages.  The result is a {@code Map<String,Object>}
 * where values are Java primitives, {@code String}, nested {@code Map}, or
 * {@code List}.
 *
 * <p><b>Deserialization strategy</b>: the reverse walk uses public
 * {@code setXxx()} setters.  Types are coerced from the GAMA value (which may
 * be {@code Integer}, {@code Double}, {@code Long}, {@code String}, or nested
 * {@code Map}).
 */
public final class RosMessageConverter {

	private RosMessageConverter() {}

	// ── Message → GamaMap ─────────────────────────────────────────────────────

	/**
	 * Converts a ROS2 message instance to a {@code Map<String, Object>}.
	 * Keys are field names (camelCase, without the trailing underscore that
	 * jros2 uses internally).
	 *
	 * @param msg the message to convert
	 * @return a map representation suitable for GAMA
	 */
	public static Map<String, Object> toGamaMap(final ROS2Message<?> msg) {
		Map<String, Object> result = new HashMap<>();
		if (msg == null) return result;
		for (Method m : msg.getClass().getMethods()) {
			String name = m.getName();
			// Only zero-arg getters, skip standard Object methods
			if (!name.startsWith("get") || m.getParameterCount() != 0
					|| name.equals("getClass")) continue;
			// Also skip things like getHeaderMethod (returns Method)
			if (m.getReturnType() == Method.class || m.getReturnType() == Class.class) continue;
			try {
				Object value = m.invoke(msg);
				String key = fieldNameFromGetter(name);
				result.put(key, convertValue(value));
			} catch (Exception e) {
				// ignore inaccessible / throwing getters
			}
		}
		return result;
	}

	/**
	 * Converts an arbitrary getter return value to a GAMA-friendly object.
	 * {@link StringBuilder} is converted to {@link String}.
	 * Nested {@link ROS2Message}s are recursively converted to maps.
	 * Arrays/Iterables become {@link List}.
	 */
	private static Object convertValue(final Object value) {
		if (value == null) return null;
		if (value instanceof StringBuilder sb) return sb.toString();
		if (value instanceof ROS2Message<?> nested) return toGamaMap(nested);
		if (value instanceof Iterable<?> iter) {
			List<Object> list = new ArrayList<>();
			for (Object item : iter) list.add(convertValue(item));
			return list;
		}
		if (value.getClass().isArray()) {
			return convertArray(value);
		}
		// primitives + boxed types + String pass through as-is
		return value;
	}

	private static List<Object> convertArray(final Object array) {
		List<Object> list = new ArrayList<>();
		// Use reflection to handle all primitive array types uniformly
		int len = java.lang.reflect.Array.getLength(array);
		for (int i = 0; i < len; i++) {
			list.add(convertValue(java.lang.reflect.Array.get(array, i)));
		}
		return list;
	}

	/** {@code "getData"} → {@code "data"}, {@code "getLinear"} → {@code "linear"} */
	private static String fieldNameFromGetter(final String getterName) {
		if (getterName.length() <= 3) return getterName.toLowerCase();
		char first = Character.toLowerCase(getterName.charAt(3));
		return first + getterName.substring(4);
	}

	// ── GamaMap → Message ─────────────────────────────────────────────────────

	/**
	 * Populates a pre-allocated ROS2 message instance from a {@code Map}.
	 *
	 * <p>Matching is done by looking for a public {@code setXxx()} setter whose
	 * name corresponds to each map key.  Type coercion handles the common cases
	 * (Number subtypes, String, nested Map for sub-messages).
	 *
	 * @param msg  the message instance to populate (modified in-place)
	 * @param data the GAMA map
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void fromGamaMap(final ROS2Message<?> msg, final Map<String, Object> data) {
		if (msg == null || data == null) return;
		// Build a setter index once
		Map<String, Method> setters = new HashMap<>();
		for (Method m : msg.getClass().getMethods()) {
			if (m.getName().startsWith("set") && m.getParameterCount() == 1) {
				setters.put(setterNameToKey(m.getName()), m);
			}
		}
		for (Map.Entry<String, Object> entry : data.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();
			Method setter = setters.get(key);
			if (setter == null) continue;
			try {
				Class<?> paramType = setter.getParameterTypes()[0];
				Object coerced = coerce(value, paramType, setter);
				if (coerced != null) {
					setter.invoke(msg, coerced);
				}
			} catch (Exception e) {
				// skip fields that cannot be set
			}
		}
	}

	/** {@code "setData"} → {@code "data"} */
	private static String setterNameToKey(final String setterName) {
		if (setterName.length() <= 3) return setterName.toLowerCase();
		char first = Character.toLowerCase(setterName.charAt(3));
		return first + setterName.substring(4);
	}

	/**
	 * Coerces a GAMA value to the type expected by a setter.
	 * Handles numeric widening/narrowing, String ↔ StringBuilder, and
	 * nested Map → sub-message.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static Object coerce(final Object value, final Class<?> target, final Method setter) {
		if (value == null) return null;

		// Exact match or subtype
		if (target.isInstance(value)) return value;

		// Numeric coercions
		if (value instanceof Number num) {
			if (target == double.class || target == Double.class) return num.doubleValue();
			if (target == float.class  || target == Float.class)  return num.floatValue();
			if (target == int.class    || target == Integer.class) return num.intValue();
			if (target == long.class   || target == Long.class)    return num.longValue();
			if (target == short.class  || target == Short.class)   return num.shortValue();
			if (target == byte.class   || target == Byte.class)    return num.byteValue();
		}

		// String → StringBuilder (jros2 uses StringBuilder for string fields)
		if (value instanceof String str && target == StringBuilder.class) {
			return new StringBuilder(str);
		}
		if (value instanceof String str && (target == String.class)) {
			return str;
		}

		// Boolean
		if (value instanceof Boolean b && (target == boolean.class || target == Boolean.class)) {
			return b;
		}

		// Nested map → sub-message (ROS2Message subtype)
		if (value instanceof Map<?, ?> nested && ROS2Message.class.isAssignableFrom(target)) {
			try {
				ROS2Message sub = (ROS2Message) target.getDeclaredConstructor().newInstance();
				fromGamaMap(sub, (Map<String, Object>) nested);
				return sub;
			} catch (Exception e) {
				return null;
			}
		}

		return null;
	}

	// ── Helpers ───────────────────────────────────────────────────────────────

	/**
	 * Allocates a fresh instance of the given ROS2 message class using its
	 * no-arg constructor.
	 *
	 * @param className fully-qualified class name (e.g. {@code "geometry_msgs.Twist"})
	 * @return a new message instance
	 * @throws IllegalArgumentException if the class cannot be found or instantiated
	 */
	@SuppressWarnings("unchecked")
	public static <T extends ROS2Message<?>> T newInstance(final String className) {
		try {
			Class<?> cls = Class.forName(className);
			return (T) cls.getDeclaredConstructor().newInstance();
		} catch (Exception e) {
			throw new IllegalArgumentException("Cannot instantiate ROS2 message: " + className, e);
		}
	}
}
