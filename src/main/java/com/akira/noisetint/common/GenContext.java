package com.akira.noisetint.common;

public final class GenContext {
    private static final ThreadLocal<int[]> CHUNK = new ThreadLocal<>();

    private GenContext() {}

    /** Push current chunk coords (cx, cz) into the thread-local. */
    public static void push(int cx, int cz) {
        CHUNK.set(new int[]{cx, cz});
    }

    /** Get current chunk coords or null if none. */
    public static int[] peek() {
        return CHUNK.get();
    }

    /** Clear context after use. */
    public static void pop() {
        CHUNK.remove();
    }

    /** Center block X of current chunk, or Integer.MIN_VALUE if absent. */
    public static int centerBlockX() {
        int[] cc = CHUNK.get();
        return cc == null ? Integer.MIN_VALUE : (cc[0] * 16 + 8);
    }

    /** Center block Z of current chunk, or Integer.MIN_VALUE if absent. */
    public static int centerBlockZ() {
        int[] cc = CHUNK.get();
        return cc == null ? Integer.MIN_VALUE : (cc[1] * 16 + 8);
    }
}
