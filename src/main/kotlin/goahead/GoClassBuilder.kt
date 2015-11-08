package goahead

import org.objectweb.asm.*
import goahead.GoNode.*
import goahead.GoNode.Expression.*
import goahead.GoNode.Declaration.*
import goahead.GoNode.Statement.*
import goahead.GoNode.Specification.*
import org.slf4j.LoggerFactory

class GoClassBuilder(val classPath: ClassPath) : ClassVisitor(Opcodes.ASM5) {

    val defaultPackageName = "main"

    companion object {
        val logger = LoggerFactory.getLogger(javaClass)

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

    var hasMain = false
    var mainUsesArgument = false

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
        // TODO()
        return null
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
        logger.debug("Visiting method {} with desc {}", name, desc)
        if (name == "<init>") return null
        val isMain = name == "main" && access.isAccessStatic && desc == "([Ljava/lang/String;)V"
        val writer = GoMethodBuilder(this, access, name, desc)
        methods += writer
        if (isMain) {
            hasMain = true
            mainUsesArgument = writer.usesArguments
        }
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
        val results =
            if (returnType === Type.VOID_TYPE) emptyList<Field>()
            else listOf(Field(emptyList(), typeToGoType(returnType)))
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
        declarations += GenericDeclaration(
            token = Token.VAR,
            specifications = listOf(ValueSpecification(
                names = listOf((simpleClassName.decapitalize() + "Static").toIdentifier),
                type = (simpleClassName + "Static").toIdentifier,
                values = emptyList()
            ))
        )
        declarations += FunctionDeclaration(
            name = simpleClassName.toIdentifier,
            receivers = emptyList(),
            type = FunctionType(
                emptyList(),
                listOf(Field(names = emptyList(), type = StarExpression((simpleClassName + "Static").toIdentifier)))
            ),
            body = BlockStatement(listOf(
                ReturnStatement(listOf(
                    UnaryExpression(Token.AND, (simpleClassName.decapitalize() + "Static").toIdentifier)
                ))
            ))
        )

        // All static methods
        declarations += methods.filter { it.access.isAccessStatic }.mapNoNull { it.toFunctionDeclaration() }

        // TODO: instance + methods

        // We need to do imports here at the end)
        val importDeclaration = GenericDeclaration(Token.IMPORT, imports.map {
            ImportSpecification(
                name = if (it.value == it.key || it.value.endsWith("/" + it.key)) null else it.key.toIdentifier,
                path = it.value.toLiteral
            )
        }.sortedBy { it.path.value })
        declarations = listOf(importDeclaration) + declarations

        return File(packageName = packageName.substringAfterLast('/').toIdentifier, declarations = declarations)
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
            var arrayType = ArrayType(typeToGoType(type.elementType))
            for (i in 2..type.dimensions) arrayType = ArrayType(arrayType)
            arrayType
        }
        Type.OBJECT -> {
            // TODO: change boxed primitives to primitive pointers
            val internalName = type.internalName
            if (internalName == "java/lang/String") SelectorExpression(
                importPackage("fmt")!!.toIdentifier,
                "Stringer".toIdentifier
            ) else if (classPath.isInterface(internalName)) classRefExpr(internalName)
            else Expression.StarExpression(classRefExpr(internalName))
        }
        else -> error("Unrecognized type: $type")
    }

    fun buildBootstrapMainFile(): File {
        require(hasMain)
        require(!mainUsesArgument) { TODO() }
        val packageAlias = packageName.substringAfterLast('/')
        val staticCall = CallExpression(
            SelectorExpression(packageAlias.toIdentifier, simpleClassName.toIdentifier)
        )
        val mainCall = CallExpression(SelectorExpression(staticCall, "Main".toIdentifier), listOf("nil".toIdentifier))
        return File(
            packageName = "main".toIdentifier,
            declarations = listOf(
                GenericDeclaration(Token.IMPORT, listOf(
                    ImportSpecification(name = null, path = packageName.toLiteral)
                )),
                FunctionDeclaration(
                    name = "main".toIdentifier,
                    receivers = emptyList(),
                    type = FunctionType(emptyList(), emptyList()),
                    body = BlockStatement(listOf(ExpressionStatement(mainCall)))
                )
            )
        )
    }
}

