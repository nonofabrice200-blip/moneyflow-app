package com.example.util

/**
 * Custom recursive descent parser for financial calculator.
 * Supports +, -, *, /, %, decimals, and parenthesis without eval().
 */
class ExpressionEvaluator {

    fun evaluate(expression: String): Double {
        val sanitized = expression
            .replace("×", "*")
            .replace("÷", "/")
            .replace("−", "-")
            .replace(" ", "")

        if (sanitized.isEmpty()) return 0.0

        val parser = Parser(sanitized)
        return parser.parse()
    }

    private class Parser(private val src: String) {
        private var pos = -1
        private var ch = 0

        private fun nextChar() {
            ch = if (++pos < src.length) src[pos].code else -1
        }

        private fun eat(charToEat: Int): Boolean {
            while (ch == ' '.code) nextChar()
            if (ch == charToEat) {
                nextChar()
                return true
            }
            return false
        }

        fun parse(): Double {
            nextChar()
            val x = parseExpression()
            if (pos < src.length) {
                // If there's extra trailing operator like "12+", return current evaluated part
                return x
            }
            return x
        }

        // Grammar:
        // expression = term | expression `+` term | expression `-` term
        // term = factor | term `*` factor | term `/` factor | term `%` factor
        // factor = `+` factor | `-` factor | `(` expression `)` | number
        private fun parseExpression(): Double {
            var x = parseTerm()
            while (true) {
                when {
                    eat('+'.code) -> x += parseTerm()
                    eat('-'.code) -> x -= parseTerm()
                    else -> return x
                }
            }
        }

        private fun parseTerm(): Double {
            var x = parseFactor()
            while (true) {
                when {
                    eat('*'.code) -> x *= parseFactor()
                    eat('/'.code) -> {
                        val divisor = parseFactor()
                        x = if (divisor == 0.0) 0.0 else x / divisor
                    }
                    eat('%'.code) -> {
                        val divisor = parseFactor()
                        x = if (divisor == 0.0) x * 0.01 else (x * divisor / 100.0)
                    }
                    else -> return x
                }
            }
        }

        private fun parseFactor(): Double {
            if (eat('+'.code)) return +parseFactor()
            if (eat('-'.code)) return -parseFactor()

            var x: Double
            val startPos = pos
            if (eat('('.code)) {
                x = parseExpression()
                eat(')'.code)
            } else if ((ch in '0'.code..'9'.code) || ch == '.'.code) {
                while ((ch in '0'.code..'9'.code) || ch == '.'.code) nextChar()
                val token = src.substring(startPos, pos)
                x = token.toDoubleOrNull() ?: 0.0
            } else {
                return 0.0
            }

            return x
        }
    }
}
