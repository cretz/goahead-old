package goahead

import goahead.testclasses.HelloWorld
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class IntegrationTest(val spec: IntegrationTest.TestSpec) {
    data class TestSpec(
        // First is what is run
        val classes: List<Class<out Any>>,
        val expectedOutput: String? = null
    )

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{index}: {0}")
        fun classes() = listOf(
            TestSpec(
                classes = listOf(HelloWorld::class.java),
                expectedOutput = "Hello World"
            )
        )
    }

    @Test
    fun test() {

    }
}