package goahead

import org.objectweb.asm.*
import goahead.GoNode.*
import goahead.GoNode.Expression.*
import goahead.GoNode.Declaration.*
import goahead.GoNode.Statement.*
import goahead.GoNode.Specification.*

class GoClassBuilder(val classPath: ClassPath) : ClassVisitor(Opcodes.ASM5) {

    val defaultPackageName = "main"

    companion object {
        fun fromBytes(classPath: ClassPath, bytes: ByteArray): GoClassBuilder {
            val writer = GoClassBuilder(classPath)
            ClassReader(bytes).accept(
                writer,
                ClassReader.SKIP_CODE and ClassReader.SKIP_DEBUG and ClassReader.SKIP_FRAMES
            )
            return writer
        }
    }

    var access = 0
    var packageName = ""
    var className = ""
    var simpleClassName = ""
    var methods = emptyList<GoMethodBuilder>()
    // Key is alias, value is full path
    var imports = emptyMap<String, String>()

    var staticInitBlocks = emptyList<BlockStatement>()
    var staticInitAssignments = emptyList<AssignStatement>()

    override fun visit(version: Int, access: Int, name: String, signature: String?, superName: String?, interfaces: Array<out String>?) {
        this.access = access
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
        val writer = GoMethodBuilder(this, access, name, desc)
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
        while (imports.containsKey(alias) && imports[alias] != internalPackageName)
            alias = originalAlias + ++counter
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
        var declarations = listOf<Declaration>()
        // Add all imports with alias if necessary
        declarations += GenericDeclaration(Token.IMPORT, imports.map {
            ImportSpecification(
                name = if (it.value == it.key || it.value.endsWith("/" + it.key)) null else it.key.toIdentifier,
                path = it.value.toLiteral
            )
        })

        // Static struct declaration
        // TODO: What we want here when we get around to static:
        //  * Struct for CLASS_NAME + "Static"
        //  * An private empty var for the static that can be populated on init if necessary
        //  * Struct contains a sync.Once if there is init to be had along with an init function that populates stuff in the var
        //  * At least a Class() function on the struct returning a pointer to lang.Class
        // TODO: Static fields
        val staticFields = emptyList<Field>()
        if (!staticInitAssignments.isEmpty() || !staticInitBlocks.isEmpty()) {
            TODO("the sync.Once + init")
        }
        declarations += GenericDeclaration(
            token = Token.TYPE,
            specifications = listOf(TypeSpecification(
                name = (simpleClassName + "Static").toIdentifier,
                type = StructType(fields = staticFields)
            ))
        )
        // TODO: static init

        // All static methods
        declarations += methods.filter { it.access.isAccessStatic }.mapNoNull { it.toFunctionDeclaration() }

        // TODO: instance + methods

        return File(packageName = packageName.toIdentifier, declarations = declarations)
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

