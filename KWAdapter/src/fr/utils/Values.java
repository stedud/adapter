package fr.utils;

import java.util.HashMap;

public class Values {

	protected static HashMap values;
	static {
		values = new HashMap();
	}

	public static String getValue(String value) {
//        if (value == null) return null;
//        return value.intern();

		String trueValue = (String) values.get(value);
		if (trueValue == null) {

			values.put(value, value);
			trueValue = (String) values.get(value);
			// Runtime.getLogger().info("Values.getValue(" + value + ") = " + trueValue +"
			// (new entry)");
			return trueValue;

		} else {
			// Runtime.getLogger().info("Values.getValue(" + value + ") = " + trueValue );
			return trueValue;
		}

	}

	public static void clear() {
		values.clear();
		values = new HashMap();
	}

	public static int getSize() {
		return values.size();
	}

}