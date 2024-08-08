/*
 * Copyright (c) 2024 ModCore Inc. All rights reserved.
 *
 * This code is part of ModCore Inc.'s Essential Mod repository and is protected
 * under copyright registration # TX0009138511. For the full license, see:
 * https://github.com/EssentialGG/Essential/blob/main/LICENSE
 *
 * You may not use, copy, reproduce, modify, sell, license, distribute,
 * commercialize, or otherwise exploit, or create derivative works based
 * upon, this file or any other in this repository, all of which is reserved by Essential.
 */
package gg.essential.model.molang

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.truncate
import kotlin.random.Random

@Serializable(MolangSerializer::class)
interface MolangExpression {
    fun eval(context: MolangContext): Float

    companion object {
        val ZERO = LiteralExpr(0f)
        val ONE = LiteralExpr(1f)
    }
}

interface MolangVariable {
    fun assign(context: MolangContext, value: Float)
}

data class LiteralExpr(val value: Float) : MolangExpression {
    override fun eval(context: MolangContext): Float = value
}

data class NegExpr(val inner: MolangExpression) : MolangExpression {
    override fun eval(context: MolangContext): Float = -inner.eval(context)
}

data class InvExpr(val inner: MolangExpression) : MolangExpression {
    override fun eval(context: MolangContext): Float = 1 / inner.eval(context)
}

data class AddExpr(val left: MolangExpression, val right: MolangExpression) : MolangExpression {
    override fun eval(context: MolangContext): Float = left.eval(context) + right.eval(context)
}

data class MulExpr(val left: MolangExpression, val right: MolangExpression) : MolangExpression {
    override fun eval(context: MolangContext): Float = left.eval(context) * right.eval(context)
}

data class SinExpr(val inner: MolangExpression) : MolangExpression {
    override fun eval(context: MolangContext): Float = sin(inner.eval(context).toRadians())
}

data class CosExpr(val inner: MolangExpression) : MolangExpression {
    override fun eval(context: MolangContext): Float = cos(inner.eval(context).toRadians())
}

data class FloorExpr(val inner: MolangExpression) : MolangExpression {
    override fun eval(context: MolangContext): Float = floor(inner.eval(context))
}

data class CeilExpr(val inner: MolangExpression) : MolangExpression {
    override fun eval(context: MolangContext): Float = ceil(inner.eval(context))
}

data class RoundExpr(val inner: MolangExpression) : MolangExpression {
    override fun eval(context: MolangContext): Float = round(inner.eval(context))
}

data class TruncExpr(val inner: MolangExpression) : MolangExpression {
    override fun eval(context: MolangContext): Float = truncate(inner.eval(context))
}

data class AbsExpr(val inner: MolangExpression) : MolangExpression {
    override fun eval(context: MolangContext): Float = abs(inner.eval(context))
}

data class ClampExpr(val value: MolangExpression, val min: MolangExpression, val max: MolangExpression) : MolangExpression {
    override fun eval(context: MolangContext): Float =
        value.eval(context).coerceIn(min.eval(context), max.eval(context))
}

data class RandomExpr(val low: MolangExpression, val high: MolangExpression) : MolangExpression {
    override fun eval(context: MolangContext): Float {
        val low = low.eval(context)
        val high = high.eval(context)
        val random = (context.query as? MolangQueryRandom)?.random ?: Random
        return random.nextFloat() * (high - low) + low
    }
}

data class QueryExpr(val f: MolangQuery.() -> Float) : MolangExpression {
    override fun eval(context: MolangContext): Float = context.query.run(f)

    companion object {
        inline operator fun <reified T : MolangQuery> invoke(crossinline f: T.() -> Float) =
            QueryExpr { (this as? T)?.let(f) ?: 0f }
    }
}

data class ComparisonExpr(val left: MolangExpression, val right: MolangExpression, val op: Op) : MolangExpression {
    override fun eval(context: MolangContext): Float =
        if (op.check(left.eval(context), right.eval(context))) 1f else 0f

    enum class Op(val check: (a: Float, b: Float) -> Boolean) {
        Equal({ a, b -> a == b }),
        NotEqual({ a, b -> a != b }),
        LessThan({ a, b -> a < b }),
        LessThanOrEqual({ a, b -> a <= b }),
        GreaterThan({ a, b -> a > b }),
        GreaterThanOrEqual({ a, b -> a >= b }),
    }
}

data class LogicalOrExpr(val left: MolangExpression, val right: MolangExpression) : MolangExpression {
    override fun eval(context: MolangContext): Float =
        if (left.eval(context) != 0f || right.eval(context) != 0f) 1f else 0f
}

data class LogicalAndExpr(val left: MolangExpression, val right: MolangExpression) : MolangExpression {
    override fun eval(context: MolangContext): Float =
        if (left.eval(context) != 0f && right.eval(context) != 0f) 1f else 0f
}

data class TernaryExpr(
    val condition: MolangExpression,
    val trueCase: MolangExpression,
    val falseCase: MolangExpression,
) : MolangExpression {
    override fun eval(context: MolangContext): Float {
        return (if (condition.eval(context) != 0f) trueCase else falseCase).eval(context)
    }
}

data class VariableExpr(val key: String) : MolangExpression, MolangVariable {
    override fun eval(context: MolangContext): Float = context.variables[key]
    override fun assign(context: MolangContext, value: Float) {
        context.variables[key] = value
    }
}

data class AssignmentExpr(val variable: MolangVariable, val inner: MolangExpression) : MolangExpression {
    override fun eval(context: MolangContext): Float {
        variable.assign(context, inner.eval(context))
        return 0f
    }
}

data class StatementsExpr(val statements: List<MolangExpression>, val result: MolangExpression = MolangExpression.ZERO) : MolangExpression {
    override fun eval(context: MolangContext): Float {
        for (statement in statements) {
            statement.eval(context)
        }
        return result.eval(context)
    }
}

data class ReturnExpr(val inner: MolangExpression) : MolangExpression {
    override fun eval(context: MolangContext): Float {
        throw Return(inner.eval(context))
    }

    internal class Return(val value: Float) : Throwable()
}

data class ComplexExpr(val inner: MolangExpression) : MolangExpression {
    override fun eval(context: MolangContext): Float {
        return try {
            inner.eval(context)
        } catch (e: ReturnExpr.Return) {
            e.value
        }
    }
}

private fun Float.toRadians() = this / 180 * PI.toFloat()

private class Parser(str: String) {
    /** How many return expressions there are. Need to wrap the entire expression in a try-catch if any remain. */
    private var returns: Int = 0

    val str = str.lowercase()
    var i = 0

    val curr get() = str[i]

    fun reads(char: Char): Boolean = reads { it == char }
    fun reads(char: CharRange): Boolean = reads { it in char }
    inline fun reads(f: (char: Char) -> Boolean): Boolean = when {
        i >= str.length -> false
        f(curr) -> {
            i++
            skipWhitespace()
            true
        }
        else -> false
    }

    fun reads(s: String): Boolean = if (str.startsWith(s, i)) {
        i += s.length
        skipWhitespace()
        true
    } else {
        false
    }

    @Suppress("ControlFlowWithEmptyBody")
    fun skipWhitespace() {
        while (reads(' '));
    }

    @Suppress("ControlFlowWithEmptyBody")
    fun parseLiteral(): LiteralExpr {
        val start = i
        while (reads('0'..'9'));
        if (reads('.')) {
            while (reads('0'..'9'));
        }
        reads('f')
        return LiteralExpr(str.slice(start until i).replace(" ", "").toFloat())
    }

    fun parseIdentifier(): String {
        val start = i
        @Suppress("ControlFlowWithEmptyBody")
        while (reads { it.isLetterOrDigit() || it == '_' });
        return str.slice(start until i).replace(" ", "")
    }

    fun parseSimpleExpression(): MolangExpression = when {
        reads('(') -> parseExpression().also { reads(')') }
        curr in '0'..'9' -> parseLiteral()
        curr == '-' -> {
            reads('-')
            NegExpr(parseSimpleExpression())
        }
        reads("math.pi") -> LiteralExpr(PI.toFloat())
        reads("math.cos(") -> CosExpr(parseExpression()).also { reads(')') }
        reads("math.sin(") -> SinExpr(parseExpression()).also { reads(')') }
        reads("math.floor(") -> FloorExpr(parseExpression()).also { reads(')') }
        reads("math.ceil(") -> CeilExpr(parseExpression()).also { reads(')') }
        reads("math.round(") -> RoundExpr(parseExpression()).also { reads(')') }
        reads("math.trunc(") -> TruncExpr(parseExpression()).also { reads(')') }
        reads("math.abs(") -> AbsExpr(parseExpression()).also { reads(')') }
        reads("math.clamp(") -> ClampExpr(
            parseExpression().also { reads(',') },
            parseExpression().also { reads(',') },
            parseExpression(),
        ).also { reads(')') }
        reads("math.random(") -> RandomExpr(parseExpression().also { reads(',') }, parseExpression()).also { reads(')') }
        reads("query.anim_time") -> QueryExpr<MolangQueryAnimation> { animTime }
        reads("query.life_time") -> QueryExpr<MolangQueryEntity> { lifeTime }
        reads("query.modified_move_speed") -> QueryExpr<MolangQueryEntity> { modifiedMoveSpeed }
        reads("query.modified_distance_moved") -> QueryExpr<MolangQueryEntity> { modifiedDistanceMoved }
        reads("variable.") -> VariableExpr(parseIdentifier())
        else -> throw IllegalArgumentException("Unexpected character at index $i")
    }

    fun parseProduct(): MolangExpression {
        var left = parseSimpleExpression()
        while (true) {
            left = when {
                reads('*') -> MulExpr(left, parseSimpleExpression())
                reads('/') -> MulExpr(left, InvExpr(parseSimpleExpression()))
                else -> return left
            }
        }
    }

    fun parseSum(): MolangExpression {
        var left = parseProduct()
        while (true) {
            left = when {
                reads('+') -> AddExpr(left, parseProduct())
                reads('-') -> AddExpr(left, NegExpr(parseProduct()))
                else -> return left
            }
        }
    }

    fun parseComparisons(): MolangExpression {
        val left = parseSum()
        return when {
            reads("<=") -> ComparisonExpr(left, parseSum(), ComparisonExpr.Op.LessThanOrEqual)
            reads(">=") -> ComparisonExpr(left, parseSum(), ComparisonExpr.Op.GreaterThanOrEqual)
            reads('<') -> ComparisonExpr(left, parseSum(), ComparisonExpr.Op.LessThan)
            reads('>') -> ComparisonExpr(left, parseSum(), ComparisonExpr.Op.GreaterThan)
            else -> left
        }
    }

    fun parseEqualityChecks(): MolangExpression {
        val left = parseComparisons()
        return when {
            reads("==") -> ComparisonExpr(left, parseSum(), ComparisonExpr.Op.Equal)
            reads("!=") -> ComparisonExpr(left, parseSum(), ComparisonExpr.Op.NotEqual)
            else -> left
        }
    }

    fun parseLogicalAnds(): MolangExpression {
        var left = parseEqualityChecks()
        while (true) {
            left = when {
                reads("&&") -> LogicalAndExpr(left, parseEqualityChecks())
                else -> return left
            }
        }
    }

    fun parseLogicalOrs(): MolangExpression {
        var left = parseLogicalAnds()
        while (true) {
            left = when {
                reads("||") -> LogicalOrExpr(left, parseLogicalAnds())
                else -> return left
            }
        }
    }

    fun parseTernary(): MolangExpression {
        val condition = parseLogicalOrs()
        return if (reads('?')) {
            val trueCase = parseTernary()
            reads(':')
            val falseCase = parseTernary()
            TernaryExpr(condition, trueCase, falseCase)
        } else {
            condition
        }
    }

    fun parseNullCoalescing(): MolangExpression {
        return parseTernary() // TODO implement
    }

    fun parseExpression(): MolangExpression {
        return if (reads('{')) {
            parseStatements().also {
                reads('}')
            }
        } else {
            parseNullCoalescing()
        }
    }

    fun parseAssignment(): MolangExpression {
        val left = parseExpression()
        if (!reads('=')) {
            return left
        }
        if (left !is MolangVariable) {
            throw IllegalArgumentException("Cannot assign value to $left")
        }
        val right = parseExpression()
        return AssignmentExpr(left, right)
    }

    fun parseStatement(): MolangExpression {
        return when {
            reads("return") -> ReturnExpr(parseExpression()).also { returns++ }
            else -> parseAssignment()
        }
    }

    fun parseStatements(): MolangExpression {
        val first = parseStatement()
        if (!reads(';')) {
            return first
        }
        val statements = mutableListOf(first)
        while (i < str.length && curr != '}') {
            statements.add(parseStatement())
            reads(';')
        }
        return StatementsExpr(statements)
    }

    fun parseMolang(): MolangExpression {
        var expr = parseStatements()

        // Complex molang expressions require a `return` statement to return a value other than 0.
        // In most cases that'll be the last expression, so we can easily optimize it into a regular expression result.
        if (expr is StatementsExpr && expr.result == MolangExpression.ZERO) {
            val lastExpr = expr.statements.last()
            if (lastExpr is ReturnExpr) {
                expr = StatementsExpr(expr.statements.dropLast(1), lastExpr.inner)
                returns--
            }
        }

        // If there's still a `return` expression somewhere in this molang expression, we need to wrap the entire thing
        // in a try-catch to handle it.
        if (returns > 0) {
            expr = ComplexExpr(expr)
        }

        return expr
    }

    fun fullyParseMolang(): MolangExpression {
        return parseMolang().also {
            if (i < str.length) {
                throw IllegalArgumentException("Failed to fully parse input, remaining: ${str.substring(i)}")
            }
        }
    }

    fun tryFullyParseMolang(): MolangExpression = try {
        fullyParseMolang()
    } catch (e: Exception) {
        throw MolangParserException("Failed to parse `$str`:", e)
    }
}

class MolangParserException(message: String, cause: Throwable?) : Exception(message, cause)

fun String.parseMolangExpression(): MolangExpression = Parser(this).tryFullyParseMolang()
fun JsonPrimitive.parseMolangExpression(): MolangExpression = when {
    isString -> content.parseMolangExpression()
    else -> LiteralExpr(content.toFloat())
}

internal object MolangSerializer : KSerializer<MolangExpression> {
    override val descriptor: SerialDescriptor = JsonElement.serializer().descriptor

    override fun deserialize(decoder: Decoder): MolangExpression = parse((decoder as JsonDecoder).decodeJsonElement())
    override fun serialize(encoder: Encoder, value: MolangExpression) = throw UnsupportedOperationException()

    private fun parse(json: JsonElement): MolangExpression =
        (json as JsonPrimitive).parseMolangExpression()
}
