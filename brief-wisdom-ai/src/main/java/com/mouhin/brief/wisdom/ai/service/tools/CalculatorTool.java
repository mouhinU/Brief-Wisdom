package com.mouhin.brief.wisdom.ai.service.tools;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

/**
 * 数学计算工具
 * <p>
 * 提供精确的数学计算能力，避免 AI 模型在大数计算、复杂公式上出错。
 * 使用 JavaScript 引擎解析数学表达式。
 *
 * @author Brief-Wisdom
 * @date 2026-07-15
 */
@Slf4j
@Component
public class CalculatorTool {

    /**
     * 执行数学计算
     *
     * @param expression 数学表达式
     * @return 计算结果
     */
    @Tool(description = "执行精确数学计算。当需要进行数学运算、单位换算、百分比计算、开方、幂运算时调用。支持加减乘除、括号、幂运算(^)、开方(sqrt)、三角函数等。")
    public String calculate(
            @ToolParam(description = "数学表达式，如 '(100 + 200) * 3 / 4'、'Math.sqrt(144)'、'Math.pow(2, 10)'") String expression) {

        log.info("[Tool] calculate 被调用: expression={}", expression);

        if (expression == null || expression.isBlank()) {
            return "表达式不能为空。";
        }

        try {
            // 预处理表达式：将常见数学符号转为 JavaScript 可识别的格式
            String jsExpression = preprocessExpression(expression);

            ScriptEngineManager manager = new ScriptEngineManager();
            ScriptEngine engine = manager.getEngineByName("js");

            if (engine == null) {
                // 如果 JS 引擎不可用，使用简单的手动解析
                return simpleCalculate(expression);
            }

            Object result = engine.eval(jsExpression);

            if (result instanceof Number number) {
                double value = number.doubleValue();
                // 如果是整数，去掉小数点
                if (value == Math.floor(value) && !Double.isInfinite(value)) {
                    return expression + " = " + (long) value;
                }
                return expression + " = " + value;
            }

            return expression + " = " + result.toString();
        } catch (Exception e) {
            log.error("[Tool] calculate 执行失败: expression={}, error={}", expression, e.getMessage());
            return "计算失败: " + e.getMessage() + "。请检查表达式格式是否正确。";
        }
    }

    /**
     * 百分比计算
     *
     * @param number  基数
     * @param percent 百分比
     * @return 计算结果
     */
    @Tool(description = "计算一个数的百分比。当用户问'XX的YY%是多少'、'XX占YY的百分之几'时调用。")
    public String percentage(
            @ToolParam(description = "基数") double number,
            @ToolParam(description = "百分比值，如 15 表示 15%") double percent) {

        log.info("[Tool] percentage 被调用: number={}, percent={}", number, percent);
        double result = number * percent / 100.0;
        return number + " 的 " + percent + "% = " + result;
    }

    /**
     * 预处理数学表达式
     */
    private String preprocessExpression(String expr) {
        String result = expr
                .replaceAll("×", "*")
                .replaceAll("÷", "/")
                .replaceAll("（", "(")
                .replaceAll("）", ")")
                .replaceAll("\\^", "**")
                .replaceAll("sqrt\\(", "Math.sqrt(")
                .replaceAll("sin\\(", "Math.sin(")
                .replaceAll("cos\\(", "Math.cos(")
                .replaceAll("tan\\(", "Math.tan(")
                .replaceAll("log\\(", "Math.log(")
                .replaceAll("abs\\(", "Math.abs(")
                .replaceAll("PI", "Math.PI")
                .replaceAll("E", "Math.E");
        return result;
    }

    /**
     * 简单计算（JS 引擎不可用时的降级方案）
     */
    private String simpleCalculate(String expression) {
        // 仅支持基本的四则运算
        String cleaned = expression.replaceAll("[^0-9+\\-*/().\\s]", "");
        if (cleaned.isBlank()) {
            return "无法解析表达式: " + expression;
        }

        try {
            // 使用 Java 的 ScriptEngine Nashorn 或简单解析
            javax.script.ScriptEngineManager mgr = new javax.script.ScriptEngineManager();
            javax.script.ScriptEngine engine = mgr.getEngineByName("nash");
            if (engine != null) {
                Object result = engine.eval(cleaned);
                return expression + " = " + result;
            }
            return "计算引擎不可用，请尝试更简单的表达式。";
        } catch (Exception e) {
            return "计算失败: " + e.getMessage();
        }
    }
}
