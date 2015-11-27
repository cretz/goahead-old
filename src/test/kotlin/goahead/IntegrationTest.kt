package goahead

import goahead.testclasses.HelloWorld
import goahead.testclasses.SimpleInstance
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
        val expectedOutput: String get() {
            val ann = classes.first().getAnnotation(ExpectedOutput::class.java)
            if (ann != null) return ann.value.replace("\n", System.lineSeparator())
            val existingOut = System.out
            val byteStream = ByteArrayOutputStream()
            val printStream = PrintStream(byteStream)
            System.setOut(printStream)
            try {
                classes.first().getMethod("main", Array<String>::class.java).invoke(null, emptyArray<String>())
                System.out.flush()
            } finally {
                System.setOut(existingOut)
            }
            return byteStream.toString()
        }
    }

    companion object {
        val logger = LoggerFactory.getLogger(IntegrationTest::class.java)

        @JvmStatic
        @Parameterized.Parameters(name = "{index}: {0}")
        fun classes() = listOf(
            TestSpec(listOf(HelloWorld::class.java)),
            TestSpec(listOf(SimpleInstance::class.java))
        )
    }

    @Rule
    @JvmField
    val tempFolder = TemporaryFolder();

    val tempFolderFile: File get() = tempFolder.root

    @Test
    fun test() {
        val compiler = Compiler(
            ClassPath(emptyList()),
            Compiler.Configuration(
                outDir = File(tempFolderFile, "src")
            )
        )
        var files = spec.classes.map {
            val bytes = it.getResourceAsStream(it.simpleName + ".class").use { it.readBytes() }
            compiler.classFileToOutFile(bytes)
        }
        files = listOf(files[0].bootstrapMainFile()) + files

        logger.debug("Out code: {}", files)

        assertValidFormat(files)

        assertExpectedOutput(compiler, files)
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

    fun assertExpectedOutput(compiler: Compiler, files: List<Compiler.OutFile>) {
        val writtenFiles = files.map { compiler.outFileToFileSystem(it) }
        // Due to http://stackoverflow.com/questions/23695448/golang-run-all-go-files-within-current-directory-through-the-command-line-mul
        // on Windows and how we handle default packages, we have to compile first sadly
        val folder = writtenFiles[0].parentFile
        compileFolder(folder)
        val process = ProcessBuilder(File(folder, "test").absolutePath).start()
        val outReader = BufferedReader(
            InputStreamReader(SequenceInputStream(process.inputStream, process.errorStream))
        )
        assertTrue(process.waitFor(5, TimeUnit.SECONDS))
        val out = outReader.readText()
        outReader.close()
        assertEquals(spec.expectedOutput, out)
    }

    fun compileFolder(folder: File) {
        val builder = ProcessBuilder("go", "build", "-o", "test").directory(folder)
        builder.environment()["GOPATH"] = File(".", "etc/testworkspace").absolutePath +
            File.pathSeparatorChar + tempFolderFile.absolutePath
        val process = builder.start()
        val outReader = BufferedReader(
            InputStreamReader(SequenceInputStream(process.inputStream, process.errorStream))
        )
        assertTrue(process.waitFor(5, TimeUnit.SECONDS))
        val out = outReader.readText()
        outReader.close()
        assertEquals("", out)
        assertEquals(0, process.exitValue())
    }
}