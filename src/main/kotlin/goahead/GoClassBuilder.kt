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

    var instanceFields = emptyList<Field>()

    var staticFields = emptyList<Field>()
    var staticInitBuilder: GoMethodBuilder? = null

    var hasMain = false
    var mainUsesArgument = false

    val staticVarIdentifier: Identifier get() = (simpleClassName.decapitalize() + "Static").toIdentifier

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

    override fun visitField(access: Int, name: String, desc: String, signature: String?, value: Any?): FieldVisitor? {
        // TODO()
        val field = Field(
            names = listOf(name.capitalizeOnAccess(access).toIdentifier),
            type = typeToGoType(desc)
        )
        if (access.isAccessStatic) staticFields += field else instanceFields += field
        return null
    }

    override fun visitMethod(access: Int, name: String, desc: String, signature: String?, exceptions: Array<out String>?): MethodVisitor? {
        // TODO: stop ignoring init
        logger.debug("Visiting method {} with desc {}", name, desc)
        if (name == "<init>") return null
        val isMain = name == "main" && access.isAccessStatic && desc == "([Ljava/lang/String;)V"
        if (name == "<clinit>") {
            if (staticInitBuilder == null) staticInitBuilder = GoMethodBuilder(this, access, name, desc)
            return staticInitBuilder
        }
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

    fun constructorRefExpr(internalName: String, desc: String): Expression {
        val packageName = internalName.substringBeforeLast('/', defaultPackageName)
        // TODO: check visibility
        val constructorName = methodName(
            "New" + internalName.substringAfterLast('/') + "Instance",
            desc,
            classPath.methodAccess(internalName, "<init>", desc)
        )
        val alias = importPackage(packageName) ?: return constructorName.toIdentifier
        return SelectorExpression(alias.toIdentifier, constructorName.toIdentifier)
    }

    fun importPackage(internalPackageName: String): String? {
        if (internalPackageName == packageName) return null
        val originalAlias = internalPackageName.substringAfterLast('/')
        var alias = originalAlias
        var counter = 0
        while (imports.containsKey(alias) && imports[alias] != internalPackageName)
            alias = originalAlias + ++counter
        if (imports[alias] == internalPackageName) return alias
        imports += Pair(alias, internalPackageName)
        return alias
    }

    fun fieldName(owner: String, name: String): String =
        fieldName(name, classPath.fieldAccess(owner, name))

    fun fieldName(name: String, access: Int): String =
        name.capitalizeOnAccess(access)

    fun methodName(owner: String, name: String, desc: String): String =
        methodName(name, desc, classPath.methodAccess(owner, name, desc))

    fun methodName(name: String, desc: String, access: Int): String =
        methodName(name, Type.getArgumentTypes(desc), access)

    fun methodName(name: String, params: Array<Type>, access: Int): String {
        val proper = StringBuilder(name.capitalizeOnAccess(access))
        // We separate every method param type by double underscores
        // TODO: do I really care about possible ambiguity here if there is a Java method w/ underscores that may match?
        params.forEach { proper.append("__").append(typeToGolangIdentifier(it)) }
        return proper.toString()
    }

    fun typeToGolangIdentifier(type: Type): String = when(type.sort) {
        Type.BOOLEAN -> "boolean"
        Type.CHAR -> "char"
        Type.SHORT -> "short"
        Type.INT -> "int"
        Type.LONG -> "long"
        Type.FLOAT -> "float"
        Type.DOUBLE -> "double"
        Type.ARRAY -> "arrayof_".repeat(type.dimensions) + typeToGolangIdentifier(type.elementType)
        Type.OBJECT -> type.internalName.replace('/', '_')
        else -> error("Unrecognized type: $type")
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

        if (staticInitBuilder != null) {
            staticFields = listOf(Field(
                names = listOf("init".toIdentifier),
                type = SelectorExpression(importPackage("sync")!!.toIdentifier, "Once".toIdentifier)
            )) + staticFields
        }
        declarations += GenericDeclaration(
            token = Token.TYPE,
            specifications = listOf(TypeSpecification(
                name = (simpleClassName + "Static").toIdentifier,
                type = StructType(fields = staticFields)
            ))
        )
        declarations += GenericDeclaration(
            token = Token.VAR,
            specifications = listOf(ValueSpecification(
                names = listOf(staticVarIdentifier),
                type = (simpleClassName + "Static").toIdentifier,
                values = emptyList()
            ))
        )
        var staticConstructStatements = emptyList<Statement>()
        if (staticInitBuilder != null) {
            staticConstructStatements += ExpressionStatement(CallExpression(
                SelectorExpression(
                    SelectorExpression(staticVarIdentifier, "init".toIdentifier),
                    "Do".toIdentifier
                ),
                listOf((simpleClassName.decapitalize() + "StaticInit").toIdentifier)
            ))
        }
        staticConstructStatements += ReturnStatement(listOf(
            UnaryExpression(Token.AND, staticVarIdentifier)
        ))
        declarations += FunctionDeclaration(
            name = simpleClassName.toIdentifier,
            receivers = emptyList(),
            type = FunctionType(
                emptyList(),
                listOf(Field(names = emptyList(), type = StarExpression((simpleClassName + "Static").toIdentifier)))
            ),
            body = BlockStatement(staticConstructStatements)
        )
        if (staticInitBuilder != null) {
            declarations += FunctionDeclaration(
                name = (simpleClassName.decapitalize() + "StaticInit").toIdentifier,
                receivers = emptyList(),
                type = FunctionType(emptyList(), emptyList()),
                body = staticInitBuilder!!.toFunctionDeclaration().body
            )
        }

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
//            if (internalName == "java/lang/String") SelectorExpression(
//                importPackage("java/lang")!!.toIdentifier,
//                "String".toIdentifier
//            ) else
            if (classPath.isInterface(internalName)) classRefExpr(internalName)
            else Expression.StarExpression(classRefExpr(internalName))
        }
        else -> error("Unrecognized type: $type")
    }

    fun buildBootstrapMainFile(): File {
        require(hasMain)
        require(!mainUsesArgument) { TODO() }
        val staticCall = CallExpression(
            if (packageName == "main") simpleClassName.toIdentifier
            else SelectorExpression(packageName.substringAfterLast('/').toIdentifier, simpleClassName.toIdentifier)
        )
        val mainCall = CallExpression(
            SelectorExpression(staticCall, "Main__arrayof_java_lang_String".toIdentifier), listOf("nil".toIdentifier)
        )
        var declarations = emptyList<Declaration>()
        if (packageName != "main") declarations += GenericDeclaration(Token.IMPORT, listOf(
            ImportSpecification(name = null, path = packageName.toLiteral)
        ))
        declarations += FunctionDeclaration(
            name = "main".toIdentifier,
            receivers = emptyList(),
            type = FunctionType(emptyList(), emptyList()),
            body = BlockStatement(listOf(ExpressionStatement(mainCall)))
        )
        return File(
            packageName = "main".toIdentifier,
            declarations = declarations
        )
    }
}

