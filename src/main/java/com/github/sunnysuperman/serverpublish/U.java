package com.github.sunnysuperman.serverpublish;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.sunnysuperman.commons.config.PropertiesConfig;
import com.github.sunnysuperman.commons.utils.BeanUtil;
import com.github.sunnysuperman.commons.utils.BeanUtil.BeanException;
import com.github.sunnysuperman.commons.utils.JSONUtil;

public class U {

	public static void sleep(long mills) {
		try {
			Thread.sleep(mills);
		} catch (Throwable t) {
			L.error(t);
		}
	}

	public static Map<String, Object> readPropertiesAsMap(InputStream in) {
		PropertiesConfig config = new PropertiesConfig(in);
		Map<String, Object> configAsMap = new HashMap<>(config.size());
		for (String key : config.keySet()) {
			configAsMap.put(key, config.getString(key));
		}
		return configAsMap;
	}

	public static <T> List<T> jsonString2list(String s, Class<T> clazz) throws BeanException {
		List<?> items = JSONUtil.parseJSONArray(s);
		List<T> beans = new ArrayList<T>(items.size());
		for (Object item : items) {
			Map<?, ?> jsonObject = (Map<?, ?>) item;
			T bean;
			try {
				bean = clazz.newInstance();
			} catch (InstantiationException | IllegalAccessException e) {
				throw new BeanException("Failed to newInstance of " + clazz, e);
			}
			bean = BeanUtil.map2bean(jsonObject, bean);
			beans.add(bean);
		}
		return beans;
	}
}
