/*
 *  This file is part of Cubic Chunks Mod, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2015 contributors
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
package cubicchunks.worldgen.generator.config;

import com.google.common.base.Function;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.minecraft.util.JsonUtils;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import cubicchunks.util.ReflectionUtil;
import mcp.MethodsReturnNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class JsonConfigInitializer<T> {
	static final Function<Class<?>, Gson> JSON_ADAPTER = new JsonAdapterProvider();
	private final T value;
	private Class<T> type;
	private Map<String, Field> mapping = new HashMap<>();

	public JsonConfigInitializer(Class<T> type) {
		if (type.getAnnotation(JsonConfig.class) == null) {
			throw new IllegalArgumentException();
		}
		try {
			this.value = type.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
		this.type = type;
		ReflectionUtil.forEachField(type, f -> addMapping(f));
	}

	public JsonConfigInitializer(Class<T> type, T value) {
		if (type.getAnnotation(JsonConfig.class) == null) {
			throw new IllegalArgumentException();
		}
		this.value = value;
		this.type = type;
		ReflectionUtil.forEachField(type, f -> addMapping(f));
	}

	public static <T> JsonConfigInitializer<T> jsonToFactory(Class<T> type, String json) {
		if (json.isEmpty()) {
			return new JsonConfigInitializer(type);
		}
		try {
			return JsonUtils.gsonDeserialize(JSON_ADAPTER.apply(type), json, JsonConfigInitializer.class);
		} catch (Exception e) {
			return new JsonConfigInitializer(type);
		}
	}

	public JsonConfigInitializer<T> defaults() {
		forEachValue((name, f) -> set(name, getDefault(f)));
		return this;
	}

	private Number getDefault(Field f) {
		Value v = f.getAnnotation(Value.class);
		if (f.getType() == int.class || f.getType() == Integer.class) {
			return v.intValue();
		}
		if (f.getType() == float.class || f.getType() == Float.class) {
			return v.floatValue();
		}
		if (f.getType() == double.class || f.getType() == Double.class) {
			return v.doubleValue();
		}
		throw new IllegalArgumentException();
	}

	@Override
	public String toString() {
		return JSON_ADAPTER.apply(type).toJson(this);
	}

	@Override
	public boolean equals(Object otherObj) {
		if (this == otherObj) {
			return true;
		}
		if (otherObj != null && this.getClass() == otherObj.getClass()) {
			JsonConfigInitializer<?> otherAny = (JsonConfigInitializer<?>) otherObj;
			if (this.type != otherAny.type) {
				return false;
			}
			// TODO: hacks, remove that
			JsonConfigInitializer<T> other = (JsonConfigInitializer<T>) otherAny;
			boolean[] ret = {true};
			this.forEachValue((name, f) -> {
				if (!other.getNumber(name).equals(this.getNumber(name))) {
					ret[0] = false;
				}
			});
			return ret[0];
		}
		return false;
	}

	public int hashCode() {
		int i[] = {1};// TODO: Hack, remove
		forEachValue((name, f) -> i[0] = 31*i[0] + getNumber(name).hashCode());
		return i[0];
	}

	public T build() {
		return value;
	}

	private void addMapping(Field f) {
		Value v = f.getAnnotation(Value.class);
		if (v == null) {
			return;
		}
		String name;
		if (v.name().isEmpty()) {
			name = f.getName();
		} else {
			name = v.name();
		}
		mapping.put(name, f);
	}


	void forEachValue(BiConsumer<String, Field> cons) {
		mapping.forEach(cons);
	}

	public void set(String name, Object o) {
		try {
			getMap(name).set(value, o);
		} catch (IllegalAccessException e) {
			throw new Error(e);
		}
	}

	public int getInt(String name) {
		try {
			return (int) getMap(name).get(value);
		} catch (IllegalAccessException e) {
			throw new Error(e);
		}
	}

	public float getFloat(String name) {
		try {
			return (float) getMap(name).get(value);
		} catch (IllegalAccessException e) {
			throw new Error(e);
		}
	}

	public double getDouble(String name) {
		try {
			return (double) getMap(name).get(value);
		} catch (IllegalAccessException e) {
			throw new Error(e);
		}
	}

	private Field getMap(String name) {
		if (!mapping.containsKey(name)) {
			throw new IllegalArgumentException(name);
		}
		return mapping.get(name);
	}

	public Number getNumber(String name) {
		try {
			return (Number) getMap(name).get(value);
		} catch (IllegalAccessException e) {
			throw new Error(e);
		}
	}

	private static final class JsonAdapterProvider<X> implements Function<Class<X>, Gson> {
		@Nullable @Override public Gson apply(@Nullable Class<X> cl) {
			return (new GsonBuilder()).registerTypeAdapter(
				JsonConfigInitializer.class, new JsonConfigSerializer<>(cl, () -> new JsonConfigInitializer<>(cl))).create();

		}
	}
}
