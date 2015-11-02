package goahead

import goahead.testclasses.HelloWorld
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
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

    @Rule
    @JvmField
    val tempFolder = TemporaryFolder();

    @Test
    fun test() {
        // First thing, compile to nodes
        val compiler = Compiler(ClassPath(emptyList()))
        val classBytes = spec.classes.toMap(
            { it },
            { it.getResourceAsStream(it.simpleName + ".class").use { it.readBytes() } }
        )
        val outFiles = classBytes.mapValues { compiler.classFileToOutFile(it.value) }

        // Now write them to strings (temporary for debug purposes right now)
        val strings = outFiles.mapValues { GoNodeWriter.fromNode(it.value.node) }

        println("Out strings: " + strings)

        TODO("""
            * Compile all classes to temporary dir
            * Run "gofmt -e -l" to make sure our printer is clean
            * Run "go run whatever.go" and confirm expected output
        """)
    }
}