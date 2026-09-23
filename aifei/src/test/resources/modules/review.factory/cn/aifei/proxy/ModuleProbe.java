package cn.aifei.proxy;

import java.lang.reflect.InvocationTargetException;

public class ModuleProbe {
    public static void main(String[] args) throws Exception {
        boolean readable = Boolean.parseBoolean(args[0]);
        for (boolean jit : new boolean[] {true, false}) {
            InstanceFactory.setJit(jit);
            InstanceFactory factory = new InstanceFactory();
            for (String name : new String[] {"exported.Service", "qualified.Service"}) {
                Class<?> type = Class.forName(name);
                Object first = factory.get(type);
                Object second = factory.get(type);
                check(first.getClass() == type && second.getClass() == type && first != second,
                        "Wrong instance: " + name);
            }

            try {
                factory.get(Class.forName("closed.Service"));
                throw new AssertionError("Non-exported package must remain inaccessible");
            } catch (RuntimeException failure) {
                check(failure.getCause() instanceof IllegalAccessException, "Unexpected access failure");
            }

            try {
                factory.get(Class.forName("exported.ThrowingService"));
                throw new AssertionError("Expected constructor failure");
            } catch (RuntimeException failure) {
                if (readable && jit) {
                    check(failure instanceof IllegalStateException, "Expected direct lambda invocation");
                } else {
                    check(failure.getCause() instanceof InvocationTargetException, "Expected reflection fallback");
                    check(failure.getCause().getCause() instanceof IllegalStateException, "Wrong constructor cause");
                }
            }
        }
        System.out.println("MODULE_OK reads=" + readable);
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
