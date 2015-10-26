package goahead

import org.objectweb.asm.*
import goahead.GoNode.*
import goahead.GoNode.Expression.*
import goahead.GoNode.Declaration.*
import goahead.GoNode.Statement.*
import goahead.GoNode.Specification.*

class GoClassWriter(val classPath: ClassPath) : ClassVisitor(Opcodes.ASM5) {

    val defaultPackageName = "main"

    companion object {
        fun fromBytes(classPath: ClassPath, bytes: ByteArray): GoClassWriter {
            val writer = GoClassWriter(classPath)
            ClassReader(bytes).accept(
                writer,
                ClassReader.SKIP_CODE and ClassReader.SKIP_DEBUG and ClassReader.SKIP_FRAMES
            )
            return writer
        }
    }

    var packageName = ""
    var className = ""
    var simpleClassName = ""
    var methods = listOf<GoMethodWriter>()
    // Key is full path, value is alias
    var imports = mapOf<String, String>()

    override fun visit(version: Int, access: Int, name: String, signature: String?, superName: String?, interfaces: Array<out String>?) {
        packageName = name.substringBeforeLast('/', defaultPackageName)
        className = name
        simpleClassName = name.substringAfterLast('/')
    }

    override fun visitSource(source: String?, debug: String?) {
        // TODO()
    }

    override fun visitOuterClass(owner: String?, name: String?, desc: String?) {
        // TODO()
    }

    override fun visitAnnotation(desc: String?, visible: Boolean): AnnotationVisitor? {
        TODO()
    }

    override fun visitTypeAnnotation(typeRef: Int, typePath: TypePath?, desc: String?, visible: Boolean): AnnotationVisitor? {
        TODO()
    }

    override fun visitAttribute(attr: Attribute?) {
        // TODO()
    }

    override fun visitInnerClass(name: String?, outerName: String?, innerName: String?, access: Int) {
        // TODO()
    }

    override fun visitField(access: Int, name: String?, desc: String?, signature: String?, value: Any?): FieldVisitor? {
        TODO()
    }

    override fun visitMethod(access: Int, name: String, desc: String, signature: String?, exceptions: Array<out String>?): MethodVisitor? {
        // TODO: stop ignoring init
        if (name == "<init>") return null
        val writer = GoMethodWriter(this, access, name, desc)
        methods += writer
        return writer
    }

    override fun visitEnd() {
        // TODO()
    }

    fun classRefExpr(internalName: String, static: Boolean = false): Expression {
        val packageName = internalName.substringBeforeLast('/', defaultPackageName)
        val className = internalName.substringAfterLast('/') + if (static) "Static" else "Instance"
        val alias = importPackage(packageName) ?: return className.toIdentifier
        return SelectorExpression(alias.toIdentifier, className.toIdentifier)
    }

    fun importPackage(internalPackageName: String): String? {
        if (internalPackageName == packageName) return null
        val originalAlias = internalPackageName.substringAfterLast('/')
        var alias = originalAlias
        var counter = 0
        while (imports.containsKey(alias)) alias = originalAlias + ++counter
        imports += Pair(alias, internalPackageName)
        return alias
    }

    fun methodToFunctionType(returnType: Type, paramTypes: Array<Type>): FunctionType {
        val errorField = Field(emptyList(), classRefExpr("goahead/rt/JvmError"))
        val results =
            if (returnType === Type.VOID_TYPE) listOf(errorField)
            else listOf(Field(emptyList(), typeToGoType(returnType)), errorField)
        val params = paramTypes.withIndex().map {
            Field(listOf(("arg" + it.index).toIdentifier), typeToGoType(it.value))
        }
        return FunctionType(params, results)
    }

    fun staticClassReadReference(internalName: String): CallExpression {
        // TODO: decide how to handle references to default package
        val packageName = internalName.substringBeforeLast('/', defaultPackageName)
        // require(packageName.isNotEmpty())
        val className = internalName.substringAfterLast('/')
        val alias = importPackage(packageName) ?: return CallExpression(className.toIdentifier, emptyList())
        return CallExpression(SelectorExpression(alias.toIdentifier, className.toIdentifier), emptyList())
    }

    fun toFile(): File {
        // Add all imports with alias if necessary
        val importDecl = GenericDeclaration(Token.IMPORT, imports.map {
            ImportSpecification(
                name = if (it.key == it.value || it.key.endsWith("/" + it.value)) null else it.value.toIdentifier,
                path = it.key.toLiteral
            )
        })

        // All methods
        val methodDecls = methods.mapNoNull { it.toFunctionDeclaration() }

        return File(
            packageName = packageName.toIdentifier,
            declarations = listOf(importDecl) + methodDecls
        )
    }

    fun typeToGoType(internalName: String) = typeToGoType(Type.getType(internalName))

    fun typeToGoType(type: Type): Expression = when(type.sort) {
        Type.BOOLEAN -> "bool".toIdentifier
        Type.CHAR -> "rune".toIdentifier
        Type.SHORT -> "int16".toIdentifier
        Type.INT -> "int".toIdentifier
        Type.LONG -> "int64".toIdentifier
        Type.FLOAT -> "float32".toIdentifier
        Type.DOUBLE -> "float64".toIdentifier
        Type.ARRAY -> {
            var arrayType = Expression.ArrayType(typeToGoType(type.elementType))
            for (i in 2..type.dimensions) arrayType = Expression.ArrayType(arrayType)
            arrayType
        }
        Type.OBJECT -> {
            // TODO: change boxed primitives to primitive pointers
            val internalName = type.internalName
            if (internalName == "java/lang/String") Expression.StarExpression("string".toIdentifier)
            else if (classPath.isInterface(internalName)) classRefExpr(internalName)
            else Expression.StarExpression(classRefExpr(internalName))
        }
        else -> error("Unrecognized type: $type")
    }

}

