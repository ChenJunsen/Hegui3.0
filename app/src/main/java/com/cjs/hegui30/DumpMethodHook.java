package com.cjs.hegui30;

import static com.cjs.hegui30.HookTrack.TAG;

import android.util.Log;

import java.util.Map;

import de.robv.android.xposed.XC_MethodHook;


/**
 * 自定义Hook回调监听器
 * @author JasonChen
 * @email chenjunsen@outlook.com
 * @createTime 2021/7/6 9:15
 */
public abstract class DumpMethodHook extends XC_MethodHook {

    /**
     * 该方法会在Hook了指定方法后调用
     * @param param
     */
    @Override
    protected void afterHookedMethod(MethodHookParam param) {
        //在这里，我们dump一下调用的方法栈信息
        dump2();
    }

    /**
     * dump模式一:根据线程进行过滤
     */
    private static void dump() {
        for (Map.Entry<Thread, StackTraceElement[]> stackTrace : Thread.getAllStackTraces().entrySet()) {
            Thread thread = stackTrace.getKey();
            StackTraceElement[] stack = stackTrace.getValue();
            // 进行过滤
            if (thread.equals(Thread.currentThread())) {
                continue;
            }
            Log.d(TAG, "[Dump Stack]" + "**********线程名字：" + thread.getName() + "**********");
            int index = 0;
            for (StackTraceElement stackTraceElement : stack) {
                Log.d(TAG, "[Dump Stack]" + index + ": " + stackTraceElement.getClassName()
                        + "----" + stackTraceElement.getFileName()
                        + "----" + stackTraceElement.getLineNumber()
                        + "----" + stackTraceElement.getMethodName());
            }
            // 增加序列号
            index++;
        }
        Log.d(TAG, "[Dump Stack]" + "********************* over **********************");
    }

    /**
     * dump模式2：类信通院报告模式
     */
    private static void dump2() {
        Throwable ex = new Throwable();
        Log.e(TAG,Thread.currentThread().getName(),ex);
    }
}
