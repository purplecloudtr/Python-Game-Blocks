package com.example

import com.example.data.LevelCatalog
import com.example.python.PythonEvaluator
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PythonInterpreterTest {

    @Test
    fun level1Variables_solvesPuzzleWithVariableDistance() = runTest {
        val level = LevelCatalog.getLevelById(1)
        val solution = """
            hareket_et("mavi", "asagi", 2)
            mesafe = 4
            print("Mesafe:", mesafe)
            hareket_et("kirmizi", "sag", mesafe)
        """.trimIndent()

        val result = PythonEvaluator(level.initialBoard).execute(solution)
        assertNull("Beklenmeyen hata: ${result.error?.turkishMessage}", result.error)
        assertTrue("1. bölüm çözülmüş olmalı", result.finalBoard.isSolved())
        assertEquals(listOf("Mesafe: 4"), result.consoleLines)
    }

    @Test
    fun level2Conditionals_solvesPuzzleWithIfElse() = runTest {
        val level = LevelCatalog.getLevelById(2)
        val solution = """
            if yol_acik_mi("turuncu", "yukari"):
                hareket_et("turuncu", "yukari", 2)
            else:
                hareket_et("turuncu", "asagi", 2)

            if yol_acik_mi("yesil", "yukari"):
                hareket_et("yesil", "yukari", 2)

            if yol_acik_mi("kirmizi", "sag"):
                hareket_et("kirmizi", "sag", 4)
        """.trimIndent()

        val result = PythonEvaluator(level.initialBoard).execute(solution)
        assertNull("Beklenmeyen hata: ${result.error?.turkishMessage}", result.error)
        assertTrue("2. bölüm çözülmüş olmalı", result.finalBoard.isSolved())
    }

    @Test
    fun level3Loops_solvesPuzzleWithForLoops() = runTest {
        val level = LevelCatalog.getLevelById(3)
        val solution = """
            engeller = ["mavi", "yesil", "mor"]
            for blok in engeller:
                hareket_et(blok, "asagi", 2)

            for adim in range(4):
                hareket_et("kirmizi", "sag", 1)
        """.trimIndent()

        val result = PythonEvaluator(level.initialBoard).execute(solution)
        assertNull("Beklenmeyen hata: ${result.error?.turkishMessage}", result.error)
        assertTrue("3. bölüm çözülmüş olmalı", result.finalBoard.isSolved())
        assertEquals(7, result.steps.size)
    }

    @Test
    fun infiniteLoop_terminatesWithTurkishTimeoutError() = runTest {
        val level = LevelCatalog.getLevelById(1)
        val infiniteCode = """
            sayac = 0
            while True:
                sayac += 1
        """.trimIndent()

        val result = PythonEvaluator(level.initialBoard, maxSteps = 5_000, timeoutMs = 1_500L)
            .execute(infiniteCode)

        assertNotNull(result.error)
        assertTrue(
            "Sonsuz döngü mesajı içermeli",
            result.error!!.turkishMessage.contains("sonsuz döngü", ignoreCase = true),
        )
    }

    @Test
    fun collisionAndSyntaxErrors_reportTurkishExplanations() = runTest {
        val level = LevelCatalog.getLevelById(1)

        val collisionResult = PythonEvaluator(level.initialBoard)
            .execute("""hareket_et("kirmizi", "sag", 4)""")
        assertNotNull(collisionResult.error)
        assertTrue(collisionResult.error!!.turkishMessage.contains("mavi"))

        val syntaxResult = PythonEvaluator(level.initialBoard)
            .execute("if True\n    print(1)")
        assertNotNull(syntaxResult.error)
        assertTrue(syntaxResult.error!!.turkishMessage.contains("':'"))
    }
}
