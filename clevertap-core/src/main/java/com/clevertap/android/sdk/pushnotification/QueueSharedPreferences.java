package com.clevertap.android.sdk.pushnotification;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import java.lang.reflect.Type;
import java.util.LinkedList;
import java.util.Queue;

public class QueueSharedPreferences {

    private static final String QUEUE_KEY = "queue_key";
    private static final String SHARED_PREFS_NAME = "QueueSharedPreferences";

    public static void enqueue(Context context, int value) {
        Queue<Integer> queue = loadQueue(context);
        queue.add(value);
        saveQueue(context, queue);
    }

    public static Queue<Integer> getQueue(Context context) {
        return loadQueue(context);
    }

    public static int dequeue(Context context) {
        Queue<Integer> queue = loadQueue(context);
        int value = queue.poll(); // Get and remove the first element
        saveQueue(context, queue);
        return value;
    }



    private static void saveQueue(Context context, Queue<Integer> queue) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        Gson gson = new Gson();
        String json = gson.toJson(queue);

        editor.putString(QUEUE_KEY, json);
        editor.apply();
    }

    public static int getQueueSize(Context context) {
        Queue<Integer> queue = loadQueue(context);
        return queue.size();
    }

    private static Queue<Integer> loadQueue(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        String json = sharedPreferences.getString(QUEUE_KEY, "");

        Gson gson = new Gson();
        Type type = new TypeToken<Queue<Integer>>() {}.getType();
        Queue<Integer> queue = gson.fromJson(json, type);

        if (queue == null) {
            queue = new LinkedList<>();
        }

        return queue;
    }
}

