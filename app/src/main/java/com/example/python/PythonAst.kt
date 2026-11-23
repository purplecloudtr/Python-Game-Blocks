package com.example.python

sealed class PythonException(
    val errorType: String,
    val turkishMessage: String,
    val lineNumber: Int?,
) : Exception(turkishMessage)

class PythonSyntaxException(
    turkishMessage: String,
    lineNumber: Int,
    errorType: String = "Sözdizimi Hatası (SyntaxError)",
) : PythonException(errorType, turkishMessage, lineNumber)

class PythonRuntimeException(
    errorType: String,
    turkishMessage: String,
    lineNumber: Int?,
) : PythonException(errorType, turkishMessage, lineNumber)

enum class TokenType {
    NUMBER,
    STRING,
    IDENT,
    TRUE,
    FALSE,
    NONE,
    IF,
    ELIF,
    ELSE,
    FOR,
    IN,
    WHILE,
    BREAK,
    CONTINUE,
    DEF,
    RETURN,
    PASS,
    AND,
    OR,
    NOT,
    PLUS,
    MINUS,
    STAR,
    DOUBLE_STAR,
    SLASH,
    DOUBLE_SLASH,
    PERCENT,
    EQ,
    NEQ,
    LT,
    LTE,
    GT,
    GTE,
    ASSIGN,
    PLUS_ASSIGN,
    MINUS_ASSIGN,
    STAR_ASSIGN,
    LPAREN,
    RPAREN,
    LBRACKET,
    RBRACKET,
    COMMA,
    COLON,
    NEWLINE,
    INDENT,
    DEDENT,
    EOF,
}

data class Token(
    val type: TokenType,
    val lexeme: String,
    val line: Int,
)

object PythonLexer {
    private val keywords = mapOf(
        "True" to TokenType.TRUE,
        "False" to TokenType.FALSE,
        "None" to TokenType.NONE,
        "if" to TokenType.IF,
        "elif" to TokenType.ELIF,
        "else" to TokenType.ELSE,
        "for" to TokenType.FOR,
        "in" to TokenType.IN,
        "while" to TokenType.WHILE,
        "break" to TokenType.BREAK,
        "continue" to TokenType.CONTINUE,
        "def" to TokenType.DEF,
        "return" to TokenType.RETURN,
        "pass" to TokenType.PASS,
        "and" to TokenType.AND,
        "or" to TokenType.OR,
        "not" to TokenType.NOT,
    )

    fun tokenize(source: String): List<Token> {
        val tokens = mutableListOf<Token>()
        val indentStack = ArrayDeque<Int>().apply { addLast(0) }
        val lines = source.replace("\r\n", "\n").replace("\r", "\n").split("\n")
        var parenDepth = 0

        for ((idx, rawLine) in lines.withIndex()) {
            val lineNum = idx + 1

            // Skip completely blank or comment-only lines when not inside parens
            var scanIndent = 0
            while (scanIndent < rawLine.length && (rawLine[scanIndent] == ' ' || rawLine[scanIndent] == '\t')) {
                if (rawLine[scanIndent] == '\t') {
                    // Treat tab as 4 spaces cleanly
                }
                scanIndent++
            }
            if (scanIndent >= rawLine.length || rawLine[scanIndent] == '#') {
                continue
            }

            if (parenDepth == 0) {
                val indentWidth = rawLine.substring(0, scanIndent).sumOf { if (it == '\t') 4 else 1 }
                val currentIndent = indentStack.last()
                if (indentWidth > currentIndent) {
                    indentStack.addLast(indentWidth)
                    tokens += Token(TokenType.INDENT, "", lineNum)
                } else if (indentWidth < currentIndent) {
                    while (indentStack.last() > indentWidth) {
                        indentStack.removeLast()
                        tokens += Token(TokenType.DEDENT, "", lineNum)
                    }
                    if (indentStack.last() != indentWidth) {
                        throw PythonSyntaxException(
                            turkishMessage = "Satır $lineNum: Girinti düzeyi önceki bloklarla uyuşmuyor. Boşlukları 4'er boşluk olarak hizala.",
                            lineNumber = lineNum,
                            errorType = "Girinti Hatası (IndentationError)",
                        )
                    }
                }
            }

            var i = scanIndent
            while (i < rawLine.length) {
                val ch = rawLine[i]
                when {
                    ch == ' ' || ch == '\t' -> i++
                    ch == '#' -> break
                    ch == '\'' || ch == '"' -> {
                        val quote = ch
                        i++
                        val sb = StringBuilder()
                        var closed = false
                        while (i < rawLine.length) {
                            val c = rawLine[i]
                            if (c == '\\' && i + 1 < rawLine.length) {
                                val next = rawLine[i + 1]
                                sb.append(
                                    when (next) {
                                        'n' -> '\n'
                                        't' -> '\t'
                                        '\\' -> '\\'
                                        '\'' -> '\''
                                        '"' -> '"'
                                        else -> next
                                    }
                                )
                                i += 2
                            } else if (c == quote) {
                                closed = true
                                i++
                                break
                            } else {
                                sb.append(c)
                                i++
                            }
                        }
                        if (!closed) {
                            throw PythonSyntaxException(
                                turkishMessage = "Satır $lineNum: Metin tırnağı ($quote) kapatılmamış.",
                                lineNumber = lineNum,
                            )
                        }
                        tokens += Token(TokenType.STRING, sb.toString(), lineNum)
                    }
                    ch.isDigit() -> {
                        val start = i
                        var hasDot = false
                        while (i < rawLine.length && (rawLine[i].isDigit() || (rawLine[i] == '.' && !hasDot && i + 1 < rawLine.length && rawLine[i + 1].isDigit()))) {
                            if (rawLine[i] == '.') hasDot = true
                            i++
                        }
                        tokens += Token(TokenType.NUMBER, rawLine.substring(start, i), lineNum)
                    }
                    ch == '_' || ch.isLetter() -> {
                        val start = i
                        while (i < rawLine.length && (rawLine[i] == '_' || rawLine[i].isLetterOrDigit())) {
                            i++
                        }
                        val word = rawLine.substring(start, i)
                        val type = keywords[word] ?: TokenType.IDENT
                        tokens += Token(type, word, lineNum)
                    }
                    else -> {
                        val twoChar = if (i + 1 < rawLine.length) rawLine.substring(i, i + 2) else ""
                        val matchedTwo = when (twoChar) {
                            "**" -> TokenType.DOUBLE_STAR
                            "//" -> TokenType.DOUBLE_SLASH
                            "==" -> TokenType.EQ
                            "!=" -> TokenType.NEQ
                            "<=" -> TokenType.LTE
                            ">=" -> TokenType.GTE
                            "+=" -> TokenType.PLUS_ASSIGN
                            "-=" -> TokenType.MINUS_ASSIGN
                            "*=" -> TokenType.STAR_ASSIGN
                            else -> null
                        }
                        if (matchedTwo != null) {
                            tokens += Token(matchedTwo, twoChar, lineNum)
                            i += 2
                        } else {
                            val matchedOne = when (ch) {
                                '+' -> TokenType.PLUS
                                '-' -> TokenType.MINUS
                                '*' -> TokenType.STAR
                                '/' -> TokenType.SLASH
                                '%' -> TokenType.PERCENT
                                '<' -> TokenType.LT
                                '>' -> TokenType.GT
                                '=' -> TokenType.ASSIGN
                                '(' -> { parenDepth++; TokenType.LPAREN }
                                ')' -> { parenDepth = (parenDepth - 1).coerceAtLeast(0); TokenType.RPAREN }
                                '[' -> { parenDepth++; TokenType.LBRACKET }
                                ']' -> { parenDepth = (parenDepth - 1).coerceAtLeast(0); TokenType.RBRACKET }
                                ',' -> TokenType.COMMA
                                ':' -> TokenType.COLON
                                else -> null
                            }
                            if (matchedOne == null) {
                                throw PythonSyntaxException(
                                    turkishMessage = "Satır $lineNum: Beklenmeyen karakter '$ch'.",
                                    lineNumber = lineNum,
                                )
                            }
                            tokens += Token(matchedOne, ch.toString(), lineNum)
                            i++
                        }
                    }
                }
            }

            if (parenDepth == 0) {
                tokens += Token(TokenType.NEWLINE, "\n", lineNum)
            }
        }

        while (indentStack.size > 1) {
            indentStack.removeLast()
            tokens += Token(TokenType.DEDENT, "", lines.size)
        }
        tokens += Token(TokenType.EOF, "", lines.size)
        return tokens
    }
}

sealed class Expr(val line: Int) {
    class NumberLit(val raw: String, val isFloat: Boolean, line: Int) : Expr(line)
    class StringLit(val value: String, line: Int) : Expr(line)
    class BoolLit(val value: Boolean, line: Int) : Expr(line)
    class NoneLit(line: Int) : Expr(line)
    class VarRef(val name: String, line: Int) : Expr(line)
    class ListLit(val elements: List<Expr>, line: Int) : Expr(line)
    class UnaryOp(val op: String, val operand: Expr, line: Int) : Expr(line)
    class BinaryOp(val left: Expr, val op: String, val right: Expr, line: Int) : Expr(line)
    class Call(val callee: Expr, val args: List<Expr>, line: Int) : Expr(line)
    class Index(val target: Expr, val index: Expr, line: Int) : Expr(line)
}

sealed class Stmt(val line: Int) {
    class Assign(val name: String, val op: String, val value: Expr, line: Int) : Stmt(line)
    class IndexAssign(val target: Expr, val index: Expr, val value: Expr, line: Int) : Stmt(line)
    class ExprStmt(val expr: Expr, line: Int) : Stmt(line)
    class If(
        val branches: List<Pair<Expr, List<Stmt>>>,
        val elseBody: List<Stmt>?,
        line: Int,
    ) : Stmt(line)
    class For(val varName: String, val iterable: Expr, val body: List<Stmt>, line: Int) : Stmt(line)
    class While(val condition: Expr, val body: List<Stmt>, line: Int) : Stmt(line)
    class Def(val name: String, val params: List<String>, val body: List<Stmt>, line: Int) : Stmt(line)
    class Return(val value: Expr?, line: Int) : Stmt(line)
    class Break(line: Int) : Stmt(line)
    class Continue(line: Int) : Stmt(line)
    class Pass(line: Int) : Stmt(line)
}

class PythonParser(private val tokens: List<Token>) {
    private var pos = 0

    private fun peek(offset: Int = 0): Token =
        tokens.getOrElse(pos + offset) { tokens.last() }

    private fun check(type: TokenType): Boolean = peek().type == type

    private fun match(vararg types: TokenType): Boolean {
        if (types.any { check(it) }) {
            pos++
            return true
        }
        return false
    }

    private fun consume(type: TokenType, errorMessage: String): Token {
        val tok = peek()
        if (tok.type == type) {
            pos++
            return tok
        }
        throw PythonSyntaxException(errorMessage, tok.line)
    }

    fun parseProgram(): List<Stmt> {
        val stmts = mutableListOf<Stmt>()
        while (!check(TokenType.EOF)) {
            if (match(TokenType.NEWLINE)) continue
            stmts += parseStatement()
        }
        return stmts
    }

    private fun parseStatement(): Stmt {
        val tok = peek()
        return when (tok.type) {
            TokenType.IF -> parseIf()
            TokenType.FOR -> parseFor()
            TokenType.WHILE -> parseWhile()
            TokenType.DEF -> parseDef()
            TokenType.RETURN -> {
                pos++
                val expr = if (!check(TokenType.NEWLINE) && !check(TokenType.EOF)) parseExpr() else null
                consumeEndOfLine()
                Stmt.Return(expr, tok.line)
            }
            TokenType.BREAK -> {
                pos++
                consumeEndOfLine()
                Stmt.Break(tok.line)
            }
            TokenType.CONTINUE -> {
                pos++
                consumeEndOfLine()
                Stmt.Continue(tok.line)
            }
            TokenType.PASS -> {
                pos++
                consumeEndOfLine()
                Stmt.Pass(tok.line)
            }
            else -> parseSimpleStatement()
        }
    }

    private fun parseSimpleStatement(): Stmt {
        val startTok = peek()
        if (startTok.type == TokenType.IDENT &&
            (peek(1).type == TokenType.ASSIGN ||
                peek(1).type == TokenType.PLUS_ASSIGN ||
                peek(1).type == TokenType.MINUS_ASSIGN ||
                peek(1).type == TokenType.STAR_ASSIGN)
        ) {
            val name = tokens[pos++].lexeme
            val opTok = tokens[pos++]
            val value = parseExpr()
            consumeEndOfLine()
            return Stmt.Assign(name, opTok.lexeme, value, startTok.line)
        }

        val expr = parseExpr()
        if (expr is Expr.Index && match(TokenType.ASSIGN)) {
            val value = parseExpr()
            consumeEndOfLine()
            return Stmt.IndexAssign(expr.target, expr.index, value, startTok.line)
        }
        if (check(TokenType.ASSIGN)) {
            throw PythonSyntaxException(
                "Satır ${startTok.line}: Geçersiz atama ifadesi. Sol tarafta bir değişken adı olmalıdır.",
                startTok.line,
            )
        }
        consumeEndOfLine()
        return Stmt.ExprStmt(expr, startTok.line)
    }

    private fun parseIf(): Stmt.If {
        val ifTok = consume(TokenType.IF, "if bekleniyordu.")
        val branches = mutableListOf<Pair<Expr, List<Stmt>>>()

        val firstCond = parseExpr()
        if (check(TokenType.ASSIGN)) {
            throw PythonSyntaxException(
                "Satır ${ifTok.line}: if koşulunda '=' yerine karşılaştırma için '==' kullanmalısın.",
                ifTok.line,
            )
        }
        consume(TokenType.COLON, "Satır ${ifTok.line}: if koşulunun sonuna ':' koymayı unutma.")
        consumeEndOfLine()
        branches += firstCond to parseBlock(ifTok.line, "if")

        while (match(TokenType.ELIF)) {
            val elifLine = tokens[pos - 1].line
            val cond = parseExpr()
            consume(TokenType.COLON, "Satır $elifLine: elif koşulunun sonuna ':' koymayı unutma.")
            consumeEndOfLine()
            branches += cond to parseBlock(elifLine, "elif")
        }

        var elseBody: List<Stmt>? = null
        if (match(TokenType.ELSE)) {
            val elseLine = tokens[pos - 1].line
            consume(TokenType.COLON, "Satır $elseLine: else sonrasında ':' koymalısın.")
            consumeEndOfLine()
            elseBody = parseBlock(elseLine, "else")
        }

        return Stmt.If(branches, elseBody, ifTok.line)
    }

    private fun parseFor(): Stmt.For {
        val forTok = consume(TokenType.FOR, "for bekleniyordu.")
        val varTok = consume(TokenType.IDENT, "Satır ${forTok.line}: for sonrasında bir döngü değişkeni adı yazmalısın (ör. for i in range(4):).")
        consume(TokenType.IN, "Satır ${forTok.line}: for döngüsünde 'in' anahtar sözcüğü eksik (ör. for i in range(4):).")
        val iterable = parseExpr()
        consume(TokenType.COLON, "Satır ${forTok.line}: for satırının sonuna ':' koymayı unutma.")
        consumeEndOfLine()
        val body = parseBlock(forTok.line, "for")
        return Stmt.For(varTok.lexeme, iterable, body, forTok.line)
    }

    private fun parseWhile(): Stmt.While {
        val whileTok = consume(TokenType.WHILE, "while bekleniyordu.")
        val cond = parseExpr()
        consume(TokenType.COLON, "Satır ${whileTok.line}: while koşulunun sonuna ':' koymayı unutma.")
        consumeEndOfLine()
        val body = parseBlock(whileTok.line, "while")
        return Stmt.While(cond, body, whileTok.line)
    }

    private fun parseDef(): Stmt.Def {
        val defTok = consume(TokenType.DEF, "def bekleniyordu.")
        val nameTok = consume(TokenType.IDENT, "Satır ${defTok.line}: Fonksiyon adı yazmalısın.")
        consume(TokenType.LPAREN, "Satır ${defTok.line}: Fonksiyon adından sonra '(' gelmelidir.")
        val params = mutableListOf<String>()
        if (!check(TokenType.RPAREN)) {
            do {
                params += consume(TokenType.IDENT, "Satır ${defTok.line}: Parametre adı geçerli bir isim olmalıdır.").lexeme
            } while (match(TokenType.COMMA))
        }
        consume(TokenType.RPAREN, "Satır ${defTok.line}: Parametre listesinde ')' kapatılmamış.")
        consume(TokenType.COLON, "Satır ${defTok.line}: def satırının sonuna ':' koymalısın.")
        consumeEndOfLine()
        val body = parseBlock(defTok.line, "def")
        return Stmt.Def(nameTok.lexeme, params, body, defTok.line)
    }

    private fun parseBlock(headerLine: Int, blockName: String): List<Stmt> {
        if (!match(TokenType.INDENT)) {
            throw PythonSyntaxException(
                turkishMessage = "Satır $headerLine: '$blockName' bloğundan sonra alt satırı 4 boşluk girintili yazmalısın.",
                lineNumber = peek().line,
                errorType = "Girinti Hatası (IndentationError)",
            )
        }
        val body = mutableListOf<Stmt>()
        while (!check(TokenType.DEDENT) && !check(TokenType.EOF)) {
            if (match(TokenType.NEWLINE)) continue
            body += parseStatement()
        }
        if (body.isEmpty()) {
            throw PythonSyntaxException(
                turkishMessage = "Satır $headerLine: '$blockName' bloğunun içi boş bırakılamaz.",
                lineNumber = headerLine,
                errorType = "Girinti Hatası (IndentationError)",
            )
        }
        match(TokenType.DEDENT)
        return body
    }

    private fun consumeEndOfLine() {
        if (check(TokenType.EOF) || check(TokenType.DEDENT)) return
        if (!match(TokenType.NEWLINE)) {
            val tok = peek()
            throw PythonSyntaxException(
                "Satır ${tok.line}: Beklenmeyen ifade '${tok.lexeme}'. Satır sonu bekleniyordu.",
                tok.line,
            )
        }
    }

    private fun parseExpr(): Expr = parseOr()

    private fun parseOr(): Expr {
        var expr = parseAnd()
        while (match(TokenType.OR)) {
            val line = tokens[pos - 1].line
            val right = parseAnd()
            expr = Expr.BinaryOp(expr, "or", right, line)
        }
        return expr
    }

    private fun parseAnd(): Expr {
        var expr = parseNot()
        while (match(TokenType.AND)) {
            val line = tokens[pos - 1].line
            val right = parseNot()
            expr = Expr.BinaryOp(expr, "and", right, line)
        }
        return expr
    }

    private fun parseNot(): Expr {
        if (match(TokenType.NOT)) {
            val line = tokens[pos - 1].line
            return Expr.UnaryOp("not", parseNot(), line)
        }
        return parseComparison()
    }

    private fun parseComparison(): Expr {
        var expr = parseAddSub()
        while (true) {
            val line = peek().line
            val op = when {
                match(TokenType.EQ) -> "=="
                match(TokenType.NEQ) -> "!="
                match(TokenType.LT) -> "<"
                match(TokenType.LTE) -> "<="
                match(TokenType.GT) -> ">"
                match(TokenType.GTE) -> ">="
                match(TokenType.IN) -> "in"
                peek().type == TokenType.NOT && peek(1).type == TokenType.IN -> {
                    pos += 2
                    "not in"
                }
                else -> null
            } ?: break
            val right = parseAddSub()
            expr = Expr.BinaryOp(expr, op, right, line)
        }
        return expr
    }

    private fun parseAddSub(): Expr {
        var expr = parseMulDiv()
        while (true) {
            val line = peek().line
            val op = when {
                match(TokenType.PLUS) -> "+"
                match(TokenType.MINUS) -> "-"
                else -> null
            } ?: break
            val right = parseMulDiv()
            expr = Expr.BinaryOp(expr, op, right, line)
        }
        return expr
    }

    private fun parseMulDiv(): Expr {
        var expr = parseUnary()
        while (true) {
            val line = peek().line
            val op = when {
                match(TokenType.STAR) -> "*"
                match(TokenType.SLASH) -> "/"
                match(TokenType.DOUBLE_SLASH) -> "//"
                match(TokenType.PERCENT) -> "%"
                else -> null
            } ?: break
            val right = parseUnary()
            expr = Expr.BinaryOp(expr, op, right, line)
        }
        return expr
    }

    private fun parseUnary(): Expr {
        val line = peek().line
        if (match(TokenType.MINUS)) return Expr.UnaryOp("-", parseUnary(), line)
        if (match(TokenType.PLUS)) return Expr.UnaryOp("+", parseUnary(), line)
        return parsePower()
    }

    private fun parsePower(): Expr {
        val left = parsePostfix()
        if (match(TokenType.DOUBLE_STAR)) {
            val line = tokens[pos - 1].line
            val right = parseUnary()
            return Expr.BinaryOp(left, "**", right, line)
        }
        return left
    }

    private fun parsePostfix(): Expr {
        var expr = parsePrimary()
        while (true) {
            if (match(TokenType.LPAREN)) {
                val line = tokens[pos - 1].line
                val args = mutableListOf<Expr>()
                if (!check(TokenType.RPAREN)) {
                    do {
                        args += parseExpr()
                    } while (match(TokenType.COMMA))
                }
                consume(TokenType.RPAREN, "Satır $line: Fonksiyon çağrısında ')' parantezi kapatılmamış.")
                expr = Expr.Call(expr, args, line)
            } else if (match(TokenType.LBRACKET)) {
                val line = tokens[pos - 1].line
                val index = parseExpr()
                consume(TokenType.RBRACKET, "Satır $line: Liste indeksinde ']' kapatılmamış.")
                expr = Expr.Index(expr, index, line)
            } else {
                break
            }
        }
        return expr
    }

    private fun parsePrimary(): Expr {
        val tok = peek()
        return when (tok.type) {
            TokenType.NUMBER -> {
                pos++
                Expr.NumberLit(tok.lexeme, isFloat = tok.lexeme.contains('.'), tok.line)
            }
            TokenType.STRING -> {
                pos++
                Expr.StringLit(tok.lexeme, tok.line)
            }
            TokenType.TRUE -> {
                pos++
                Expr.BoolLit(true, tok.line)
            }
            TokenType.FALSE -> {
                pos++
                Expr.BoolLit(false, tok.line)
            }
            TokenType.NONE -> {
                pos++
                Expr.NoneLit(tok.line)
            }
            TokenType.IDENT -> {
                pos++
                Expr.VarRef(tok.lexeme, tok.line)
            }
            TokenType.LPAREN -> {
                pos++
                val inner = parseExpr()
                consume(TokenType.RPAREN, "Satır ${tok.line}: Açılan '(' parantezi kapatılmamış.")
                inner
            }
            TokenType.LBRACKET -> {
                pos++
                val items = mutableListOf<Expr>()
                if (!check(TokenType.RBRACKET)) {
                    do {
                        if (check(TokenType.RBRACKET)) break
                        items += parseExpr()
                    } while (match(TokenType.COMMA))
                }
                consume(TokenType.RBRACKET, "Satır ${tok.line}: Açılan '[' liste parantezi kapatılmamış.")
                Expr.ListLit(items, tok.line)
            }
            else -> throw PythonSyntaxException(
                "Satır ${tok.line}: Beklenmeyen ifade '${tok.lexeme}'.",
                tok.line,
            )
        }
    }
}
