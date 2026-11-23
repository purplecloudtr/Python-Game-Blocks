package com.example.python

import com.example.model.BoardState
import com.example.model.Direction
import com.example.model.ExecutionStep
import com.example.model.MoveOutcome
import kotlin.coroutines.coroutineContext
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.pow
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withTimeout

sealed class PyValue {
    data class PyInt(val value: Long) : PyValue()
    data class PyFloat(val value: Double) : PyValue()
    data class PyString(val value: String) : PyValue()
    data class PyBool(val value: Boolean) : PyValue()
    data class PyList(val items: MutableList<PyValue>) : PyValue()
    data class PyFunc(val name: String, val params: List<String>, val body: List<Stmt>, val closure: Env) : PyValue()
    data class PyBuiltin(val name: String, val fn: (List<PyValue>, Int) -> PyValue) : PyValue()
    data object PyNone : PyValue()

    fun isTruthy(): Boolean = when (this) {
        is PyBool -> value
        is PyInt -> value != 0L
        is PyFloat -> value != 0.0
        is PyString -> value.isNotEmpty()
        is PyList -> items.isNotEmpty()
        is PyNone -> false
        is PyFunc, is PyBuiltin -> true
    }

    fun toDisplayString(): String = when (this) {
        is PyInt -> value.toString()
        is PyFloat -> if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
        is PyString -> value
        is PyBool -> if (value) "True" else "False"
        is PyList -> items.joinToString(", ", "[", "]") {
            if (it is PyString) "\"${it.value}\"" else it.toDisplayString()
        }
        is PyFunc -> "<fonksiyon $name>"
        is PyBuiltin -> "<komut $name>"
        is PyNone -> "None"
    }
}

class Env(val parent: Env? = null) {
    private val values = mutableMapOf<String, PyValue>()

    fun get(name: String): PyValue? = values[name] ?: parent?.get(name)

    fun set(name: String, value: PyValue) {
        if (values.containsKey(name) || parent == null) {
            values[name] = value
        } else {
            // In Python, assignment binds in local scope unless global
            values[name] = value
        }
    }

    fun setDirect(name: String, value: PyValue) {
        values[name] = value
    }

    fun allNames(): Set<String> {
        val result = mutableSetOf<String>()
        result.addAll(values.keys)
        parent?.let { result.addAll(it.allNames()) }
        return result
    }
}

private class ReturnSignal(val value: PyValue) : Throwable()
private class BreakSignal : Throwable()
private class ContinueSignal : Throwable()

data class PythonRunOutput(
    val finalBoard: BoardState,
    val steps: List<ExecutionStep>,
    val consoleLines: List<String>,
    val error: PythonException? = null,
)

class PythonEvaluator(
    initialBoard: BoardState,
    private val maxSteps: Int = 50_000,
    private val timeoutMs: Long = 2_500L,
) {
    private var board: BoardState = initialBoard
    private val steps = mutableListOf<ExecutionStep>()
    private val consoleLines = mutableListOf<String>()
    private var opCount = 0
    private var callDepth = 0

    suspend fun execute(source: String): PythonRunOutput {
        return try {
            withTimeout(timeoutMs) {
                val tokens = PythonLexer.tokenize(source)
                val ast = PythonParser(tokens).parseProgram()
                val globalEnv = createGlobalEnv()
                execBlock(ast, globalEnv)
                PythonRunOutput(
                    finalBoard = board,
                    steps = steps.toList(),
                    consoleLines = consoleLines.toList(),
                    error = null,
                )
            }
        } catch (timeout: TimeoutCancellationException) {
            PythonRunOutput(
                finalBoard = board,
                steps = steps.toList(),
                consoleLines = consoleLines.toList(),
                error = PythonRuntimeException(
                    errorType = "Zaman Aşımı (TimeoutError)",
                    turkishMessage = "Kodun çalışması 2.5 saniyeyi aştığı için durduruldu (olası sonsuz döngü). Döngü koşulunu kontrol et.",
                    lineNumber = null,
                ),
            )
        } catch (pyEx: PythonException) {
            PythonRunOutput(
                finalBoard = board,
                steps = steps.toList(),
                consoleLines = consoleLines.toList(),
                error = pyEx,
            )
        }
    }

    private fun createGlobalEnv(): Env {
        val env = Env()
        env.setDirect("hareket_et", PyValue.PyBuiltin("hareket_et") { args, line ->
            if (args.size !in 2..3) {
                throw PythonRuntimeException(
                    "Tür Hatası (TypeError)",
                    "Satır $line: hareket_et(blok, yon, adim) 2 veya 3 parametre alır (girilen: ${args.size}). Örnek: hareket_et(\"kirmizi\", \"sag\", 2)",
                    line,
                )
            }
            val blockName = (args[0] as? PyValue.PyString)?.value
                ?: throw PythonRuntimeException(
                    "Tür Hatası (TypeError)",
                    "Satır $line: hareket_et komutunun 1. parametresi metin (blok adı) olmalıdır, örn. \"kirmizi\".",
                    line,
                )
            val dirName = (args[1] as? PyValue.PyString)?.value
                ?: throw PythonRuntimeException(
                    "Tür Hatası (TypeError)",
                    "Satır $line: hareket_et komutunun 2. parametresi metin (yön) olmalıdır, örn. \"sag\".",
                    line,
                )
            val stepCount = if (args.size == 3) {
                when (val s = args[2]) {
                    is PyValue.PyInt -> s.value.toInt()
                    else -> throw PythonRuntimeException(
                        "Tür Hatası (TypeError)",
                        "Satır $line: hareket_et komutunun 3. parametresi (adım sayısı) tam sayı olmalıdır.",
                        line,
                    )
                }
            } else {
                1
            }

            when (val outcome = board.moveBlock(blockName, dirName, stepCount)) {
                is MoveOutcome.Success -> {
                    board = outcome.newBoard
                    steps += ExecutionStep(
                        boardSnapshot = board,
                        message = outcome.description,
                        lineNumber = line,
                    )
                    PyValue.PyNone
                }
                is MoveOutcome.Failure -> {
                    throw PythonRuntimeException(
                        "Hareket Hatası",
                        "Satır $line: ${outcome.turkishReason}",
                        line,
                    )
                }
            }
        })

        env.setDirect("yol_acik_mi", PyValue.PyBuiltin("yol_acik_mi") { args, line ->
            if (args.size != 2) {
                throw PythonRuntimeException(
                    "Tür Hatası (TypeError)",
                    "Satır $line: yol_acik_mi(blok, yon) tam 2 parametre alır. Örnek: yol_acik_mi(\"turuncu\", \"yukari\")",
                    line,
                )
            }
            val blockName = (args[0] as? PyValue.PyString)?.value
                ?: throw PythonRuntimeException(
                    "Tür Hatası (TypeError)",
                    "Satır $line: yol_acik_mi komutunun 1. parametresi blok adı metni olmalıdır.",
                    line,
                )
            val dirRaw = (args[1] as? PyValue.PyString)?.value
                ?: throw PythonRuntimeException(
                    "Tür Hatası (TypeError)",
                    "Satır $line: yol_acik_mi komutunun 2. parametresi yön metni olmalıdır.",
                    line,
                )
            if (board.findBlock(blockName) == null) {
                throw PythonRuntimeException(
                    "Değer Hatası (ValueError)",
                    "Satır $line: \"$blockName\" adında bir blok tahtada yok.",
                    line,
                )
            }
            val dir = Direction.parse(dirRaw)
                ?: throw PythonRuntimeException(
                    "Değer Hatası (ValueError)",
                    "Satır $line: \"$dirRaw\" geçerli bir yön değil (\"sag\", \"sol\", \"yukari\", \"asagi\").",
                    line,
                )
            PyValue.PyBool(board.canMoveOneStep(blockName, dir))
        })

        env.setDirect("adim_sayisi_acik", PyValue.PyBuiltin("adim_sayisi_acik") { args, line ->
            if (args.size != 2) {
                throw PythonRuntimeException(
                    "Tür Hatası (TypeError)",
                    "Satır $line: adim_sayisi_acik(blok, yon) 2 parametre alır.",
                    line,
                )
            }
            val blockName = (args[0] as? PyValue.PyString)?.value
                ?: throw PythonRuntimeException("Tür Hatası (TypeError)", "Satır $line: Blok adı metin olmalıdır.", line)
            val dirRaw = (args[1] as? PyValue.PyString)?.value
                ?: throw PythonRuntimeException("Tür Hatası (TypeError)", "Satır $line: Yön metin olmalıdır.", line)
            val dir = Direction.parse(dirRaw)
                ?: throw PythonRuntimeException("Değer Hatası (ValueError)", "Satır $line: Geçersiz yön '$dirRaw'.", line)
            PyValue.PyInt(board.availableSteps(blockName, dir).toLong())
        })

        env.setDirect("cikis_mesafesi", PyValue.PyBuiltin("cikis_mesafesi") { args, line ->
            if (args.isNotEmpty()) {
                throw PythonRuntimeException("Tür Hatası (TypeError)", "Satır $line: cikis_mesafesi() parametre almaz.", line)
            }
            PyValue.PyInt((board.distanceToExit() ?: 0).toLong())
        })

        env.setDirect("print", PyValue.PyBuiltin("print") { args, _ ->
            consoleLines += args.joinToString(" ") { it.toDisplayString() }
            PyValue.PyNone
        })

        env.setDirect("range", PyValue.PyBuiltin("range") { args, line ->
            val ints = args.map {
                (it as? PyValue.PyInt)?.value ?: throw PythonRuntimeException(
                    "Tür Hatası (TypeError)",
                    "Satır $line: range() yalnızca tam sayı (int) parametreleri kabul eder.",
                    line,
                )
            }
            val (start, stop, step) = when (ints.size) {
                1 -> Triple(0L, ints[0], 1L)
                2 -> Triple(ints[0], ints[1], 1L)
                3 -> Triple(ints[0], ints[1], ints[2])
                else -> throw PythonRuntimeException(
                    "Tür Hatası (TypeError)",
                    "Satır $line: range() 1, 2 veya 3 sayı alabilir.",
                    line,
                )
            }
            if (step == 0L) {
                throw PythonRuntimeException("Değer Hatası (ValueError)", "Satır $line: range() adım değeri 0 olamaz.", line)
            }
            val result = mutableListOf<PyValue>()
            var cur = start
            var safety = 0
            while ((step > 0 && cur < stop) || (step < 0 && cur > stop)) {
                result += PyValue.PyInt(cur)
                cur += step
                safety++
                if (safety > 10_000) {
                    throw PythonRuntimeException("Bellek/Sınır Hatası", "Satır $line: range() en fazla 10.000 eleman üretebilir.", line)
                }
            }
            PyValue.PyList(result)
        })

        env.setDirect("len", PyValue.PyBuiltin("len") { args, line ->
            if (args.size != 1) {
                throw PythonRuntimeException("Tür Hatası (TypeError)", "Satır $line: len() 1 parametre alır.", line)
            }
            when (val target = args[0]) {
                is PyValue.PyList -> PyValue.PyInt(target.items.size.toLong())
                is PyValue.PyString -> PyValue.PyInt(target.value.length.toLong())
                else -> throw PythonRuntimeException("Tür Hatası (TypeError)", "Satır $line: Bu türün uzunluğu ölçülemez.", line)
            }
        })

        env.setDirect("abs", PyValue.PyBuiltin("abs") { args, line ->
            if (args.size != 1) throw PythonRuntimeException("Tür Hatası (TypeError)", "Satır $line: abs() 1 sayı alır.", line)
            when (val v = args[0]) {
                is PyValue.PyInt -> PyValue.PyInt(abs(v.value))
                is PyValue.PyFloat -> PyValue.PyFloat(abs(v.value))
                else -> throw PythonRuntimeException("Tür Hatası (TypeError)", "Satır $line: abs() sayısal değer bekler.", line)
            }
        })

        env.setDirect("int", PyValue.PyBuiltin("int") { args, line ->
            if (args.size != 1) throw PythonRuntimeException("Tür Hatası (TypeError)", "Satır $line: int() 1 parametre alır.", line)
            when (val v = args[0]) {
                is PyValue.PyInt -> v
                is PyValue.PyFloat -> PyValue.PyInt(v.value.toLong())
                is PyValue.PyBool -> PyValue.PyInt(if (v.value) 1L else 0L)
                is PyValue.PyString -> v.value.trim().toLongOrNull()?.let { PyValue.PyInt(it) }
                    ?: throw PythonRuntimeException("Değer Hatası (ValueError)", "Satır $line: '${v.value}' tam sayıya dönüştürülemedi.", line)
                else -> throw PythonRuntimeException("Tür Hatası (TypeError)", "Satır $line: Bu değer int() ile tam sayıya çevrilemez.", line)
            }
        })

        env.setDirect("str", PyValue.PyBuiltin("str") { args, line ->
            if (args.size != 1) throw PythonRuntimeException("Tür Hatası (TypeError)", "Satır $line: str() 1 parametre alır.", line)
            PyValue.PyString(args[0].toDisplayString())
        })

        env.setDirect("bool", PyValue.PyBuiltin("bool") { args, line ->
            if (args.size != 1) throw PythonRuntimeException("Tür Hatası (TypeError)", "Satır $line: bool() 1 parametre alır.", line)
            PyValue.PyBool(args[0].isTruthy())
        })

        env.setDirect("min", PyValue.PyBuiltin("min") { args, line ->
            val items = if (args.size == 1 && args[0] is PyValue.PyList) (args[0] as PyValue.PyList).items else args
            if (items.isEmpty()) throw PythonRuntimeException("Değer Hatası (ValueError)", "Satır $line: min() boş koleksiyon alamaz.", line)
            items.minWith { a, b -> compareNumbers(a, b, line) }
        })

        env.setDirect("max", PyValue.PyBuiltin("max") { args, line ->
            val items = if (args.size == 1 && args[0] is PyValue.PyList) (args[0] as PyValue.PyList).items else args
            if (items.isEmpty()) throw PythonRuntimeException("Değer Hatası (ValueError)", "Satır $line: max() boş koleksiyon alamaz.", line)
            items.maxWith { a, b -> compareNumbers(a, b, line) }
        })

        return env
    }

    private suspend fun tick(line: Int) {
        coroutineContext.ensureActive()
        opCount++
        if (opCount > maxSteps) {
            throw PythonRuntimeException(
                errorType = "Süre / Adım Sınırı (TimeoutError)",
                turkishMessage = "Satır $line: Kod çok fazla adım ($maxSteps) çalıştırdığı için durduruldu (olası sonsuz döngü).",
                lineNumber = line,
            )
        }
    }

    private suspend fun execBlock(stmts: List<Stmt>, env: Env) {
        for (stmt in stmts) {
            execStmt(stmt, env)
        }
    }

    private suspend fun execStmt(stmt: Stmt, env: Env) {
        tick(stmt.line)
        when (stmt) {
            is Stmt.Pass -> Unit
            is Stmt.Break -> throw BreakSignal()
            is Stmt.Continue -> throw ContinueSignal()
            is Stmt.Return -> throw ReturnSignal(stmt.value?.let { evalExpr(it, env) } ?: PyValue.PyNone)
            is Stmt.ExprStmt -> evalExpr(stmt.expr, env)
            is Stmt.Assign -> {
                val rhs = evalExpr(stmt.value, env)
                val finalVal = when (stmt.op) {
                    "=" -> rhs
                    "+=" -> {
                        val existing = env.get(stmt.name) ?: throw undefinedNameError(stmt.name, stmt.line, env)
                        evalBinary(existing, "+", rhs, stmt.line)
                    }
                    "-=" -> {
                        val existing = env.get(stmt.name) ?: throw undefinedNameError(stmt.name, stmt.line, env)
                        evalBinary(existing, "-", rhs, stmt.line)
                    }
                    "*=" -> {
                        val existing = env.get(stmt.name) ?: throw undefinedNameError(stmt.name, stmt.line, env)
                        evalBinary(existing, "*", rhs, stmt.line)
                    }
                    else -> rhs
                }
                env.set(stmt.name, finalVal)
            }
            is Stmt.IndexAssign -> {
                val target = evalExpr(stmt.target, env)
                val idxVal = evalExpr(stmt.index, env)
                val newVal = evalExpr(stmt.value, env)
                val list = target as? PyValue.PyList
                    ?: throw PythonRuntimeException("Tür Hatası (TypeError)", "Satır ${stmt.line}: Yalnızca liste elemanları indeksle değiştirilebilir.", stmt.line)
                val rawIdx = (idxVal as? PyValue.PyInt)?.value?.toInt()
                    ?: throw PythonRuntimeException("Tür Hatası (TypeError)", "Satır ${stmt.line}: Liste indeksi tam sayı olmalıdır.", stmt.line)
                val idx = if (rawIdx < 0) list.items.size + rawIdx else rawIdx
                if (idx !in list.items.indices) {
                    throw PythonRuntimeException("Dizin Hatası (IndexError)", "Satır ${stmt.line}: Liste indeksi ($rawIdx) sınır dışında.", stmt.line)
                }
                list.items[idx] = newVal
            }
            is Stmt.If -> {
                var executed = false
                for ((cond, body) in stmt.branches) {
                    if (evalExpr(cond, env).isTruthy()) {
                        execBlock(body, env)
                        executed = true
                        break
                    }
                }
                if (!executed && stmt.elseBody != null) {
                    execBlock(stmt.elseBody, env)
                }
            }
            is Stmt.For -> {
                val iterable = evalExpr(stmt.iterable, env)
                val items = when (iterable) {
                    is PyValue.PyList -> iterable.items.toList()
                    is PyValue.PyString -> iterable.value.map { PyValue.PyString(it.toString()) }
                    else -> throw PythonRuntimeException(
                        "Tür Hatası (TypeError)",
                        "Satır ${stmt.line}: for döngüsü yalnızca liste, range(...) veya metin üzerinde gezinebilir.",
                        stmt.line,
                    )
                }
                for (item in items) {
                    tick(stmt.line)
                    env.set(stmt.varName, item)
                    try {
                        execBlock(stmt.body, env)
                    } catch (_: ContinueSignal) {
                        continue
                    } catch (_: BreakSignal) {
                        break
                    }
                }
            }
            is Stmt.While -> {
                while (evalExpr(stmt.condition, env).isTruthy()) {
                    tick(stmt.line)
                    try {
                        execBlock(stmt.body, env)
                    } catch (_: ContinueSignal) {
                        continue
                    } catch (_: BreakSignal) {
                        break
                    }
                }
            }
            is Stmt.Def -> {
                env.set(stmt.name, PyValue.PyFunc(stmt.name, stmt.params, stmt.body, env))
            }
        }
    }

    private suspend fun evalExpr(expr: Expr, env: Env): PyValue {
        tick(expr.line)
        return when (expr) {
            is Expr.NumberLit -> if (expr.isFloat) PyValue.PyFloat(expr.raw.toDouble()) else PyValue.PyInt(expr.raw.toLong())
            is Expr.StringLit -> PyValue.PyString(expr.value)
            is Expr.BoolLit -> PyValue.PyBool(expr.value)
            is Expr.NoneLit -> PyValue.PyNone
            is Expr.VarRef -> env.get(expr.name) ?: throw undefinedNameError(expr.name, expr.line, env)
            is Expr.ListLit -> PyValue.PyList(expr.elements.map { evalExpr(it, env) }.toMutableList())
            is Expr.UnaryOp -> {
                val v = evalExpr(expr.operand, env)
                when (expr.op) {
                    "not" -> PyValue.PyBool(!v.isTruthy())
                    "-" -> when (v) {
                        is PyValue.PyInt -> PyValue.PyInt(-v.value)
                        is PyValue.PyFloat -> PyValue.PyFloat(-v.value)
                        else -> throw PythonRuntimeException("Tür Hatası (TypeError)", "Satır ${expr.line}: '-' işareti sayı ile kullanılır.", expr.line)
                    }
                    "+" -> when (v) {
                        is PyValue.PyInt, is PyValue.PyFloat -> v
                        else -> throw PythonRuntimeException("Tür Hatası (TypeError)", "Satır ${expr.line}: '+' işareti sayı ile kullanılır.", expr.line)
                    }
                    else -> v
                }
            }
            is Expr.BinaryOp -> {
                when (expr.op) {
                    "and" -> {
                        val left = evalExpr(expr.left, env)
                        if (!left.isTruthy()) left else evalExpr(expr.right, env)
                    }
                    "or" -> {
                        val left = evalExpr(expr.left, env)
                        if (left.isTruthy()) left else evalExpr(expr.right, env)
                    }
                    else -> {
                        val left = evalExpr(expr.left, env)
                        val right = evalExpr(expr.right, env)
                        evalBinary(left, expr.op, right, expr.line)
                    }
                }
            }
            is Expr.Index -> {
                val target = evalExpr(expr.target, env)
                val idxVal = evalExpr(expr.index, env)
                val rawIdx = (idxVal as? PyValue.PyInt)?.value?.toInt()
                    ?: throw PythonRuntimeException("Tür Hatası (TypeError)", "Satır ${expr.line}: İndeks tam sayı olmalıdır.", expr.line)
                when (target) {
                    is PyValue.PyList -> {
                        val idx = if (rawIdx < 0) target.items.size + rawIdx else rawIdx
                        target.items.getOrNull(idx)
                            ?: throw PythonRuntimeException("Dizin Hatası (IndexError)", "Satır ${expr.line}: Liste indeksi ($rawIdx) sınır dışında.", expr.line)
                    }
                    is PyValue.PyString -> {
                        val idx = if (rawIdx < 0) target.value.length + rawIdx else rawIdx
                        target.value.getOrNull(idx)?.let { PyValue.PyString(it.toString()) }
                            ?: throw PythonRuntimeException("Dizin Hatası (IndexError)", "Satır ${expr.line}: Metin indeksi ($rawIdx) sınır dışında.", expr.line)
                    }
                    else -> throw PythonRuntimeException("Tür Hatası (TypeError)", "Satır ${expr.line}: Bu türde indeksleme yapılamaz.", expr.line)
                }
            }
            is Expr.Call -> {
                val callee = evalExpr(expr.callee, env)
                val args = expr.args.map { evalExpr(it, env) }
                when (callee) {
                    is PyValue.PyBuiltin -> callee.fn(args, expr.line)
                    is PyValue.PyFunc -> {
                        if (args.size != callee.params.size) {
                            throw PythonRuntimeException(
                                "Tür Hatası (TypeError)",
                                "Satır ${expr.line}: '${callee.name}' fonksiyonu ${callee.params.size} parametre bekliyor (girilen: ${args.size}).",
                                expr.line,
                            )
                        }
                        if (callDepth >= 80) {
                            throw PythonRuntimeException(
                                "Özyineleme Sınırı (RecursionError)",
                                "Satır ${expr.line}: Fonksiyon çağrısı çok derinleşti (olası sonsuz özyineleme).",
                                expr.line,
                            )
                        }
                        callDepth++
                        val fnEnv = Env(callee.closure)
                        callee.params.zip(args).forEach { (p, a) -> fnEnv.setDirect(p, a) }
                        return try {
                            execBlock(callee.body, fnEnv)
                            PyValue.PyNone
                        } catch (ret: ReturnSignal) {
                            ret.value
                        } finally {
                            callDepth--
                        }
                    }
                    else -> throw PythonRuntimeException(
                        "Tür Hatası (TypeError)",
                        "Satır ${expr.line}: Bu ifade bir fonksiyon gibi çağrılamaz.",
                        expr.line,
                    )
                }
            }
        }
    }

    private fun evalBinary(left: PyValue, op: String, right: PyValue, line: Int): PyValue {
        return when (op) {
            "==" -> PyValue.PyBool(pyEquals(left, right))
            "!=" -> PyValue.PyBool(!pyEquals(left, right))
            "in" -> PyValue.PyBool(pyContains(right, left, line))
            "not in" -> PyValue.PyBool(!pyContains(right, left, line))
            "<", "<=", ">", ">=" -> {
                val cmp = compareNumbers(left, right, line)
                PyValue.PyBool(
                    when (op) {
                        "<" -> cmp < 0
                        "<=" -> cmp <= 0
                        ">" -> cmp > 0
                        else -> cmp >= 0
                    }
                )
            }
            "+" -> when {
                left is PyValue.PyInt && right is PyValue.PyInt -> PyValue.PyInt(left.value + right.value)
                (left is PyValue.PyInt || left is PyValue.PyFloat) && (right is PyValue.PyInt || right is PyValue.PyFloat) ->
                    PyValue.PyFloat(toDouble(left) + toDouble(right))
                left is PyValue.PyString && right is PyValue.PyString -> PyValue.PyString(left.value + right.value)
                left is PyValue.PyList && right is PyValue.PyList ->
                    PyValue.PyList((left.items + right.items).toMutableList())
                else -> throw PythonRuntimeException(
                    "Tür Hatası (TypeError)",
                    "Satır $line: Farklı türler '+' ile doğrudan birleştirilemez (ör. sayı + metin).",
                    line,
                )
            }
            "-" -> when {
                left is PyValue.PyInt && right is PyValue.PyInt -> PyValue.PyInt(left.value - right.value)
                (left is PyValue.PyInt || left is PyValue.PyFloat) && (right is PyValue.PyInt || right is PyValue.PyFloat) ->
                    PyValue.PyFloat(toDouble(left) - toDouble(right))
                else -> throw PythonRuntimeException("Tür Hatası (TypeError)", "Satır $line: '-' işlemi yalnızca sayılarla yapılır.", line)
            }
            "*" -> when {
                left is PyValue.PyInt && right is PyValue.PyInt -> PyValue.PyInt(left.value * right.value)
                (left is PyValue.PyInt || left is PyValue.PyFloat) && (right is PyValue.PyInt || right is PyValue.PyFloat) ->
                    PyValue.PyFloat(toDouble(left) * toDouble(right))
                left is PyValue.PyString && right is PyValue.PyInt ->
                    PyValue.PyString(left.value.repeat(right.value.toInt().coerceIn(0, 1000)))
                else -> throw PythonRuntimeException("Tür Hatası (TypeError)", "Satır $line: '*' işlemi için uyumlu türler kullan.", line)
            }
            "/" -> {
                if ((right is PyValue.PyInt || right is PyValue.PyFloat) && toDouble(right) == 0.0) {
                    throw PythonRuntimeException("Sıfıra Bölme Hatası (ZeroDivisionError)", "Satır $line: Bir sayı 0'a bölünemez.", line)
                }
                if ((left is PyValue.PyInt || left is PyValue.PyFloat) && (right is PyValue.PyInt || right is PyValue.PyFloat)) {
                    PyValue.PyFloat(toDouble(left) / toDouble(right))
                } else {
                    throw PythonRuntimeException("Tür Hatası (TypeError)", "Satır $line: '/' bölme işlemi sayılarla yapılır.", line)
                }
            }
            "//" -> {
                if (left is PyValue.PyInt && right is PyValue.PyInt) {
                    if (right.value == 0L) {
                        throw PythonRuntimeException("Sıfıra Bölme Hatası (ZeroDivisionError)", "Satır $line: Bir sayı 0'a bölünemez.", line)
                    }
                    PyValue.PyInt(Math.floorDiv(left.value, right.value))
                } else if ((left is PyValue.PyInt || left is PyValue.PyFloat) && (right is PyValue.PyInt || right is PyValue.PyFloat)) {
                    val d = toDouble(right)
                    if (d == 0.0) throw PythonRuntimeException("Sıfıra Bölme Hatası (ZeroDivisionError)", "Satır $line: Bir sayı 0'a bölünemez.", line)
                    PyValue.PyFloat(floor(toDouble(left) / d))
                } else {
                    throw PythonRuntimeException("Tür Hatası (TypeError)", "Satır $line: '//' tam bölme işlemi sayılarla yapılır.", line)
                }
            }
            "%" -> {
                if (left is PyValue.PyInt && right is PyValue.PyInt) {
                    if (right.value == 0L) throw PythonRuntimeException("Sıfıra Bölme Hatası (ZeroDivisionError)", "Satır $line: Mod işleminde bölen 0 olamaz.", line)
                    PyValue.PyInt(Math.floorMod(left.value, right.value))
                } else {
                    throw PythonRuntimeException("Tür Hatası (TypeError)", "Satır $line: '%' kalan işlemi tam sayılarla yapılır.", line)
                }
            }
            "**" -> {
                if ((left is PyValue.PyInt || left is PyValue.PyFloat) && (right is PyValue.PyInt || right is PyValue.PyFloat)) {
                    val res = toDouble(left).pow(toDouble(right))
                    if (left is PyValue.PyInt && right is PyValue.PyInt && right.value >= 0) {
                        PyValue.PyInt(res.toLong())
                    } else {
                        PyValue.PyFloat(res)
                    }
                } else {
                    throw PythonRuntimeException("Tür Hatası (TypeError)", "Satır $line: '**' üs alma işlemi sayılarla yapılır.", line)
                }
            }
            else -> throw PythonRuntimeException("Tür Hatası (TypeError)", "Satır $line: Desteklenmeyen işlem '$op'.", line)
        }
    }

    private fun toDouble(v: PyValue): Double = when (v) {
        is PyValue.PyInt -> v.value.toDouble()
        is PyValue.PyFloat -> v.value
        is PyValue.PyBool -> if (v.value) 1.0 else 0.0
        else -> 0.0
    }

    private fun pyEquals(a: PyValue, b: PyValue): Boolean = when {
        a is PyValue.PyInt && b is PyValue.PyInt -> a.value == b.value
        (a is PyValue.PyInt || a is PyValue.PyFloat) && (b is PyValue.PyInt || b is PyValue.PyFloat) -> toDouble(a) == toDouble(b)
        a is PyValue.PyString && b is PyValue.PyString -> a.value == b.value
        a is PyValue.PyBool && b is PyValue.PyBool -> a.value == b.value
        a is PyValue.PyNone && b is PyValue.PyNone -> true
        a is PyValue.PyList && b is PyValue.PyList ->
            a.items.size == b.items.size && a.items.zip(b.items).all { (x, y) -> pyEquals(x, y) }
        else -> false
    }

    private fun pyContains(container: PyValue, needle: PyValue, line: Int): Boolean = when (container) {
        is PyValue.PyList -> container.items.any { pyEquals(it, needle) }
        is PyValue.PyString -> {
            val sub = (needle as? PyValue.PyString)?.value
                ?: throw PythonRuntimeException("Tür Hatası (TypeError)", "Satır $line: Metin içinde yalnızca metin aranabilir.", line)
            container.value.contains(sub)
        }
        else -> throw PythonRuntimeException("Tür Hatası (TypeError)", "Satır $line: 'in' işleci liste veya metin ile kullanılır.", line)
    }

    private fun compareNumbers(a: PyValue, b: PyValue, line: Int): Int {
        if ((a is PyValue.PyInt || a is PyValue.PyFloat) && (b is PyValue.PyInt || b is PyValue.PyFloat)) {
            return toDouble(a).compareTo(toDouble(b))
        }
        if (a is PyValue.PyString && b is PyValue.PyString) {
            return a.value.compareTo(b.value)
        }
        throw PythonRuntimeException("Tür Hatası (TypeError)", "Satır $line: Bu iki değer birbiriyle karşılaştırılamaz.", line)
    }

    private fun undefinedNameError(name: String, line: Int, env: Env): PythonRuntimeException {
        val known = env.allNames()
        val closest = known.minByOrNull { levenshtein(name, it) }
        val suggestion = if (closest != null && levenshtein(name, closest) <= 3) {
            " Bunu mu demek istedin: '$closest'?"
        } else {
            ""
        }
        return PythonRuntimeException(
            errorType = "İsim Hatası (NameError)",
            turkishMessage = "Satır $line: '$name' tanımlı değil.$suggestion (Metin yazıyorsan tırnak içine almayı unutma, örn. \"$name\".)",
            lineNumber = line,
        )
    }

    private fun levenshtein(a: String, b: String): Int {
        val dp = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) dp[i][0] = i
        for (j in 0..b.length) dp[0][j] = j
        for (i in 1..a.length) {
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                dp[i][j] = minOf(dp[i - 1][j] + 1, dp[i][j - 1] + 1, dp[i - 1][j - 1] + cost)
            }
        }
        return dp[a.length][b.length]
    }
}
