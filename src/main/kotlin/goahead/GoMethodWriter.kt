package goahead

import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import goahead.GoNode.*
import goahead.GoNode.Expression.*
import goahead.GoNode.Declaration.*
import goahead.GoNode.Statement.*
import goahead.GoNode.Specification.*
import org.objectweb.asm.Type

class GoMethodWriter(
    val classWriter: GoClassWriter,
    val access: Int,
    val name: String,
    val desc: String
) : MethodVisitor(Opcodes.ASM5) {

    val paramTypes = Type.getArgumentTypes(desc)
    val returnType = Type.getReturnType(desc)

    var stack = listOf<Expression>()
    var statements = listOf<Statement>()

    var tempVarCounter = 0;
    var errorVarDefined = false

    override fun visitFieldInsn(opcode: Int, owner: String, name: String, desc: String) {
        when(opcode) {
            Opcodes.GETSTATIC ->
                stack += SelectorExpression(
                    classWriter.staticClassReadReference(owner),
                    name.capitalizeOnAccess(classWriter.classPath.fieldAccess(owner, name)).toIdentifier
                )
            else -> error("Unrecognized opcode $opcode")
        }
    }

    override fun visitLdcInsn(cst: Any) {
        when(cst) {
            is String -> stack += cst.toLiteral
            else -> error("Unrecognized constant $cst")
        }
    }

    override fun visitMethodInsn(opcode: Int, owner: String, name: String, desc: String, itf: Boolean) {
        when(opcode) {
            Opcodes.INVOKEVIRTUAL -> {
                val callParamTypes = Type.getArgumentTypes(desc)
                val callReturnType = Type.getReturnType(desc)
                val params = pop(callParamTypes.size())
                val subject = pop(1).first()

                // We have to do a null-pointer check first
                statements += nullPointerAssertion(subject)

                // Create the call
                val call = CallExpression(
                    SelectorExpression(
                        subject,
                        name.capitalizeOnAccess(classWriter.classPath.methodAccess(owner, name, desc)).toIdentifier
                    ),
                    params
                )

                // We put the result of the call on the stack if it's not void
                if (callReturnType === Type.VOID_TYPE) {
                    val tempVar = newTempVar()
                    stack += tempVar
                    statements += AssignStatement(listOf(tempVar, errorVar()), Token.DEFINE, listOf(call))
                } else {
                    val token = if (errorVarDefined) Token.ASSIGN else Token.DEFINE
                    statements += AssignStatement(listOf(errorVar()), token, listOf(call))
                }

                // We "throw" if there is an error
                statements += throwIfError()
            }
            else -> error("Unrecognized opcode $opcode")
        }
    }

    fun errorVar(): Identifier {
        if (!errorVarDefined) errorVarDefined = true
        return "err".toIdentifier
    }

    fun newTempVar() = ("temp" + tempVarCounter++).toIdentifier

    fun nullPointerAssertion(expr: Expression): Statement {
        // We have to check whether the expression is nil, and if it is, "throw" an error
        return IfStatement(
            condition = BinaryExpression(
                left = expr,
                operator = Token.EQL,
                right = "nil".toIdentifier
            ),
            body = BlockStatement(listOf(throwError(
                CallExpression(
                    SelectorExpression(
                        classWriter.importPackage("goahead/rt")!!.toIdentifier,
                        "JvmErrorNullPointerException".toIdentifier
                    ),
                    emptyList()
                )
            )))
        )
    }

    fun pop(amount: Int): List<Expression> {
        val ret = stack.takeLast(amount)
        require(ret.size() == amount)
        stack = stack.dropLast(amount)
        return ret
    }

    fun throwError(expr: Expression): Statement {
        // If this is an init, we panic
        // If this is a void function, we return just the expr
        // Otherwise we return nil + expr
        if (name == "<init>" || name == "<clinit>")
            return ExpressionStatement(CallExpression("panic".toIdentifier, listOf(expr)))
        if (returnType === Type.VOID_TYPE)
            return ReturnStatement(listOf(expr))
        return ReturnStatement(listOf("nil".toIdentifier, expr))
    }

    fun throwIfError() = IfStatement(
        condition = BinaryExpression(
            left = errorVar(),
            operator = Token.NEQ,
            right = "nil".toIdentifier
        ),
        body = BlockStatement(listOf(throwError(errorVar())))
    )

    fun toFunctionDeclaration(): FunctionDeclaration? {
        val receivers =
            if (access.isAccessStatic) emptyList<Field>()
            else listOf(Field(listOf("this".toIdentifier), classWriter.typeToGoType(classWriter.className)))
        return FunctionDeclaration(
            name = name.capitalizeOnAccess(access).toIdentifier,
            receivers = receivers,
            type = classWriter.methodToFunctionType(returnType, paramTypes),
            body = BlockStatement(statements)
        )
    }
}
