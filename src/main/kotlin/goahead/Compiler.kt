package goahead

import org.objectweb.asm.ClassReader
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

class Compiler(
    val classPath: ClassPath,
    val config: Compiler.Configuration = Compiler.Configuration()
) {
    class Configuration(
        val outDir: File? = null
    )

    class OutFile(
        val dir: String,
        val name: String,
        val node: GoNode.File
    )

    fun classFileToFileSystem(bytes: ByteArray) {
        outFileToFileSystem(classFileToOutFile(bytes))
    }

    fun classFileToOutFile(bytes: ByteArray): OutFile {
        val writer = GoClassBuilder.fromBytes(classPath, bytes)
        return OutFile(
            dir = writer.packageName,
            name = writer.simpleClassName.decapitalize() + ".go",
            node = writer.toFile()
        )
    }

    fun outFileToFileSystem(file: OutFile) {
        if (config.outDir == null) error("Out directory not specified")
        val dir = File(config.outDir, file.dir)
        if (!dir.isDirectory) require(dir.mkdirs()) { "Unable to create dir" }
        val fsFile = File(dir, file.name)
        fsFile.writeText(GoNodeWriter.fromNode(file.node))
    }
}