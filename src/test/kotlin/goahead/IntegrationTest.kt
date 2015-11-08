package goahead

import goahead.testclasses.HelloWorld
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.concurrent.TimeUnit
import org.junit.Assert.*
import org.slf4j.LoggerFactory
import java.io.*

@RunWith(Parameterized::class)
class IntegrationTest(val spec: IntegrationTest.TestSpec) {

    data class TestSpec(
        // First is what is run
        val classes: List<Class<out Any>>
    ) {
        val expectedOutput: String get() = classes.first().getAnnotation(ExpectedOutput::class.java).value
    }

    companion object {
        val logger = LoggerFactory.getLogger(IntegrationTest::class.java)

        @JvmStatic
        @Parameterized.Parameters(name = "{index}: {0}")
        fun classes() = listOf(TestSpec(listOf(HelloWorld::class.java)))
    }

    @Rule
    @JvmField
    val tempFolder = TemporaryFolder();

    @Test
    fun test() {
        // First thing, compile to nodes
        val compiler = Compiler(
            ClassPath(emptyList()),
            Compiler.Configuration(
                outDir = File(tempFolder.root, "src")
            )
        )
        var files = spec.classes.map {
            val bytes = it.getResourceAsStream(it.simpleName + ".class").use { it.readBytes() }
            compiler.classFileToOutFile(bytes)
        }
        files = listOf(files[0].bootstrapMainFile()) + files

        logger.debug("Out code: {}", files)

        assertValidFormat(files)

        assertExpectedOutput(compiler, spec, files)
    }

    fun assertValidFormat(files: List<Compiler.OutFile>) {
        // Assume gofmt is on PATH
        files.forEach {
            val process = ProcessBuilder("gofmt").start()
            val outReader = BufferedReader(InputStreamReader(process.inputStream))
            val errReader = BufferedReader(InputStreamReader(process.errorStream))
            val writer = BufferedWriter(OutputStreamWriter(process.outputStream))
            writer.write(it.code)
            writer.close()
            assertTrue(process.waitFor(5, TimeUnit.SECONDS))
            val out = outReader.readText()
            outReader.close()
            val err = errReader.readText()
            errReader.close()
            assertEquals("", err)
            assertEquals(it.code, out)
            assertEquals(0, process.exitValue())
        }
    }

    fun assertExpectedOutput(
        compiler: Compiler,
        spec: TestSpec,
        files: List<Compiler.OutFile>
    ) {
        val writtenFiles = files.map { compiler.outFileToFileSystem(it) }
        // Now take the first one and run it
        // Assume go is on the PATH
        val toRun = writtenFiles[0]
        logger.debug("Running: {} from {}", toRun, System.getProperty("user.dir"))
        val builder = ProcessBuilder("go", "run", toRun.absolutePath)
        // Set the GOPATH
        builder.environment()["GOPATH"] = File(".", "etc/testworkspace").absolutePath +
            File.pathSeparatorChar + tempFolder.root.absolutePath
        val process = builder.start()
        val outReader = BufferedReader(
            InputStreamReader(SequenceInputStream(process.inputStream, process.errorStream))
        )
        assertTrue(process.waitFor(5, TimeUnit.SECONDS))
        val out = outReader.readText()
        outReader.close()
        assertEquals(spec.expectedOutput, out)
    }
}