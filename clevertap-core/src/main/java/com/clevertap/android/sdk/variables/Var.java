package com.clevertap.android.sdk.variables;


import android.text.TextUtils;
import androidx.annotation.NonNull;
import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.Utils;
import com.clevertap.android.sdk.variables.callbacks.VariableCallback;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * CleverTap variable.
 *
 * @param <T> Type of the variable. Can be Boolean, Byte, Short, Integer, Long, Float, Double,
 *            Character, String, List, or Map. You may nest lists and maps arbitrarily.
 * @author Ansh Sachdeva
 */
public class Var<T> {
    private final CTVariables ctVariables;
    private final VarCache varCache;

    public Var(final VarCache varCache, final CTVariables ctVariables) {
        this.varCache = varCache;
        this.ctVariables = ctVariables;
    }

    private String name;

    private String[] nameComponents;

    public String stringValue;

    private Double numberValue;

    private T defaultValue;

    private T value;

    private String kind;

    private boolean hadStarted = false;

    private final List<VariableCallback<T>> valueChangedHandlers = new ArrayList<>();
    private static boolean printedCallbackWarning;

    public static <T> Var<T> define(String name, T defaultValue, VarCache varCache, CTVariables ctVariables) {
        String type = CTVariableUtils.kindFromValue(defaultValue);
        return define(name, defaultValue, type, varCache, ctVariables);
    }

    /**
     * creates a  {@link  Var} object from given params and calls {@link VarCache#registerVariable(Var)}
     *
     * @param name         name of variable
     * @param defaultValue value
     * @param kind         datatype as string
     * @param <T>          Type of the variable.
     * @return instance of a {@link  Var} class
     */
    public static <T> Var<T> define(String name, T defaultValue, String kind, VarCache varCache,
            CTVariables ctVariables) {
        Logger.v("define() called with: name = [" + name + "], defaultValue = [" + defaultValue + "], kind = [" + kind
                + "]");
        if (TextUtils.isEmpty(name)) {
            Logger.v("Empty name parameter provided. aborting define operation");
            return null;
        }

        Var<T> existing = varCache.getVariable(name);
        if (existing != null) {
            Logger.v("A variable exists for current field name. returning existing variable");
            return existing;
        }

        Var<T> var = new Var<>(varCache,ctVariables);
        try {
            var.name = name;
            var.nameComponents = CTVariableUtils.getNameComponents(name);
            var.defaultValue = defaultValue;
            var.value = defaultValue;
            var.kind = kind;
            var.cacheComputedValues();
            varCache.registerVariable(var);
            var.update();
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return var;
    }


    /**
     * updates the value in {@link #value} by getting the final merged value from
     * {@link  VarCache#getMergedValueFromComponentArray(Object[])}
     * optionally calls {@link #triggerValueChanged()}
     */
    public synchronized void update() {
        Logger.v( "update() called");
        T oldValue = value;
        value = varCache.getMergedValueFromComponentArray(nameComponents);
        if (value == null && oldValue == null) {
            return;
        }
        if (value != null && oldValue != null && value.equals(oldValue) && hadStarted) {
            return;
        }
        cacheComputedValues();
        if (ctVariables.isVariableResponseReceived()) {
            hadStarted = true;
            triggerValueChanged();
        } else {
            Logger.v(
                    "CleverTap hasn't finished retrieving values from the server. the associated individual valueChangedHandlers won't be triggered");
        }
    }



    private void cacheComputedValues() {
        if (value instanceof String) {
            stringValue = (String) value;
            modifyNumberValue(stringValue);
            modifyValue(numberValue);

        } else if (value instanceof Number) {
            stringValue = "" + value;
            numberValue = ((Number) value).doubleValue();
            modifyValue((Number) value);

        } else if (value != null && !(value instanceof Iterable<?>) && !(value instanceof Map<?, ?>)) {
            stringValue = value.toString();
            numberValue = null;
        } else {
            stringValue = null;
            numberValue = null;
        }
    }

    /**
     * values come from the server as Number type. so we type cast in here to the actual
     * java type accordingly  using the defaultValue's type
     */
    private void modifyValue(Number src) {
        if (src == null)
            return;

        if (defaultValue instanceof Byte) {
            value = (T) (Byte) src.byteValue();
        } else if (defaultValue instanceof Short) {
            value = (T) (Short) src.shortValue();
        } else if (defaultValue instanceof Integer) {
            value = (T) (Integer) src.intValue();
        } else if (defaultValue instanceof Long) {
            value = (T) (Long) src.longValue();
        } else if (defaultValue instanceof Float) {
            value = (T) (Float) src.floatValue();
        } else if (defaultValue instanceof Double) {
            value = (T) (Double) src.doubleValue();
        } else if (defaultValue instanceof Character) {
            value = (T) (Character) (char) src.intValue();
        }
    }

    //prob a  fault-tolernance check around a case where server values are number but in string format (like "20_000") . this will convert the string to actual number  in double format
    private void modifyNumberValue(String src) {
        try {
            numberValue = Double.valueOf(src);
        } catch (NumberFormatException e) {
            numberValue = null;
            if (defaultValue instanceof Number) {
                numberValue = ((Number) defaultValue).doubleValue();
            }
        }
    }


    //triggers the user's callbacks of variable changing (VariableCallback)
    private void triggerValueChanged() {
        synchronized (valueChangedHandlers) {
            for (VariableCallback<T> callback : valueChangedHandlers) {
                callback.setVariable(this);
                Utils.runOnUiThread(callback);

            }
        }
    }

    // if variable is of type list, then it will return the size of list
    public int count() {
        try {
            warnIfNotStarted();
            Object result = varCache.getMergedValueFromComponentArray(nameComponents);
            if (result instanceof List) {
                return ((List<?>) result).size();
            }
        } catch (Throwable t) {
            t.printStackTrace();
            return 0;
        }
        CTVariableUtils.maybeThrowException(new UnsupportedOperationException("This variable is not a list."));
        return 0;
    }


    @Override @NonNull
    public String toString() {
        return "Var(" + name + ","+value+")" ;
    }

    void warnIfNotStarted() {
        if (!ctVariables.isVariableResponseReceived() && !printedCallbackWarning) {
            Logger.v(
                    "CleverTap hasn't finished retrieving values from the server. You should use a callback to make sure the value for "
                            + name + " is ready. Otherwise, your app may not use the most up-to-date value.");
            printedCallbackWarning = true;
        }
    }

    public String name() {
        return name;
    }
    public String[] nameComponents() {
        return nameComponents;
    }
    public String kind() {
        return kind;
    }
    public T defaultValue() {
        return defaultValue;
    }
    public T value() {
        warnIfNotStarted();
        return value;
    }
    public void addValueChangedHandler(VariableCallback<T> handler) {
        if (handler == null) {
            Logger.v( "Invalid handler parameter provided.");
            return;
        }

        synchronized (valueChangedHandlers) {
            valueChangedHandlers.add(handler);
        }

        if (ctVariables.isVariableResponseReceived()) {
            handler.handle(this);
        } else {
            Logger.v("The added listener will get triggered once the values are successfully retrieved");
        }
    }
    public void removeValueChangedHandler(VariableCallback<T> handler) {
        synchronized (valueChangedHandlers) {
            valueChangedHandlers.remove(handler);
        }
    }
    public Number numberValue() {
        warnIfNotStarted();
        return numberValue;
    }
    public String stringValue() {
        warnIfNotStarted();
        return stringValue;
    }
}
