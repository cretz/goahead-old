package goahead

import org.objectweb.asm.ClassReader
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

class Compiler(
    val classPath: ClassPath,
    val config: Compiler.Configuration = Compiler.Configuration()
) {
    val logger = LoggerFactory.getLogger(javaClass)

    class Configuration(
        val outDir: File? = null
    )

    class OutFile(
        val dir: String,
        val name: String,
        val node: GoNode.File,
        val bootstrapMain: GoNode.File? = null
    ) {
        val code by lazy { GoNodeWriter.fromNode(node) }

        override fun toString() = "$dir/$name:\n$code"

        fun bootstrapMainFile(): OutFile {
            if (bootstrapMain == null) error("No main bootstrap")
            return OutFile(dir = "", name = "main.go", node = bootstrapMain)
        }
    }

    fun classFileToFileSystem(bytes: ByteArray): File {
        return outFileToFileSystem(classFileToOutFile(bytes))
    }

    fun classFileToOutFile(bytes: ByteArray): OutFile {
        val writer = GoClassBuilder.fromBytes(classPath, bytes)
        return OutFile(
            dir = if (writer.packageName == "main") "" else writer.packageName,
            name = writer.simpleClassName.decapitalize() + ".go",
            node = writer.toFile(),
            bootstrapMain =
                if (!writer.hasMain) null
                else writer.buildBootstrapMainFile()
        )
    }

    fun outFileToFileSystem(file: OutFile): File {
        if (config.outDir == null) error("Out directory not specified")
        val dir = File(config.outDir, file.dir)
        if (!dir.isDirectory) require(dir.mkdirs()) { "Unable to create dir" }
        val fsFile = File(dir, file.name)
        logger.debug("Writing to: {}", fsFile)
        fsFile.writeText(GoNodeWriter.fromNode(file.node))
        return fsFile
    }
}