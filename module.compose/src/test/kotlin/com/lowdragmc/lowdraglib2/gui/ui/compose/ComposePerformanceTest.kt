package com.lowdragmc.lowdraglib2.gui.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** What recomposition costs, isolated from Minecraft. */
class ComposePerformanceTest {

    private data class Row(val id: Int, val text: String)

    /** Median of [runs] timings, after [warmup] untimed passes to let the JIT settle. */
    private fun measure(warmup: Int = 50, runs: Int = 100, block: (Int) -> Unit): Long {
        repeat(warmup) { block(it) }
        val samples = LongArray(runs)
        for (i in 0 until runs) {
            val start = System.nanoTime()
            block(warmup + i)
            samples[i] = System.nanoTime() - start
        }
        samples.sort()
        return samples[runs / 2]
    }

    private fun micros(nanos: Long) = "%.1fµs".format(nanos / 1000.0)

    /** A list of [n] keyed rows. */
    private class ListFixture(n: Int) {
        val harness = TestHarness()
        var rows by mutableStateOf((1..n).map { Row(it, "v0") })
        var rowCompositions = 0

        init {
            harness.setContent {
                Node("list") {
                    rows.forEach { row -> key(row.id) { RowNode(row) } }
                }
            }
        }

        @Composable
        private fun RowNode(row: Row) {
            rowCompositions++
            Node(row.text)
        }

        fun touchOne(index: Int, revision: Int) {
            rows = rows.toMutableList().also { it[index] = it[index].copy(text = "v$revision") }
        }
    }

    @Test
    fun `an idle frame is essentially free`() {
        val fixture = ListFixture(500)
        val idle = measure { fixture.harness.frame() }
        println("[perf] idle frame, 500 nodes mounted: ${micros(idle)}")

        assertTrue(idle < 1_000_000, "an idle frame took ${micros(idle)}")
        fixture.harness.dispose()
    }

    @Test
    fun `updating one row recomposes only that row`() {
        for (fixture in listOf(ListFixture(100), ListFixture(1000))) {
            val n = fixture.rows.size
            val cost = measure { rev -> fixture.touchOne(n / 2, rev); fixture.harness.frame() }
            println("[perf] update 1 of $n: ${micros(cost)}")

            fixture.rowCompositions = 0
            fixture.touchOne(n / 2, -1)
            fixture.harness.frame()

            assertEquals(1, fixture.rowCompositions, "rows recomposed for a one-row change in a list of $n")
            assertEquals("root(list(${fixture.rows.joinToString(",") { it.text }}))", fixture.harness.tree())
            fixture.harness.dispose()
        }
    }

    @Test
    fun `a one-row change applies one node edit, not a rebuild`() {
        val fixture = ListFixture(1000)
        fixture.harness.applier.resetCounts()

        fixture.touchOne(500, 1)
        fixture.harness.frame()

        assertEquals(0, fixture.harness.applier.inserts)
        assertEquals(0, fixture.harness.applier.removes)
        assertEquals(0, fixture.harness.applier.moves)
        fixture.harness.dispose()
    }

    @Test
    fun `appending to a long list costs one insert`() {
        val fixture = ListFixture(1000)
        fixture.harness.applier.resetCounts()

        val cost = measure(warmup = 20, runs = 50) { rev ->
            fixture.rows = fixture.rows + Row(100_000 + rev, "added")
            fixture.harness.frame()
        }
        println("[perf] append 1 to a 1000+ row list: ${micros(cost)}")

        assertEquals(0, fixture.harness.applier.removes)
        assertEquals(0, fixture.harness.applier.moves)
        fixture.harness.dispose()
    }

    @Test
    fun `initial composition throughput`() {
        for (n in listOf(100, 1000)) {
            val cost = measure(warmup = 5, runs = 20) {
                val fixture = ListFixture(n)
                fixture.harness.dispose()
            }
            println("[perf] build $n nodes from scratch: ${micros(cost)} (${micros(cost / n)}/node)")
        }
    }

    @Test
    fun `reordering a long list moves nodes instead of recreating them`() {
        val fixture = ListFixture(500)
        fixture.harness.applier.resetCounts()

        val cost = measure(warmup = 5, runs = 20) {
            fixture.rows = fixture.rows.reversed()
            fixture.harness.frame()
        }
        println("[perf] reverse a 500 row list: ${micros(cost)}")

        assertEquals(0, fixture.harness.applier.inserts)
        assertEquals(0, fixture.harness.applier.removes)
        assertTrue(fixture.harness.applier.moves > 0, "a reversal should have moved something")
        fixture.harness.dispose()
    }
}
