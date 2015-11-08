package goahead

import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import goahead.GoNode.*
import goahead.GoNode.Expression.*
import goahead.GoNode.Declaration.*
import goahead.GoNode.Statement.*
import goahead.GoNode.Specification.*
import org.objectweb.asm.Type

class GoMethodBuilder(
    val classBuilder: GoClassBuilder,
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

    // TODO
    var usesArguments = false

    override fun visitFieldInsn(opcode: Int, owner: String, name: String, desc: String) {
        when(opcode) {
            Opcodes.GETSTATIC ->
                stack += SelectorExpression(
                    classBuilder.staticClassReadReference(owner),
                    name.capitalizeOnAccess(classBuilder.classPath.fieldAccess(owner, name)).toIdentifier
                )
            else -> error("Unrecognized opcode $opcode")
        }
    }

    override fun visitLdcInsn(cst: Any) {
        when(cst) {
            is String -> stack += CallExpression(
                SelectorExpression(
                    classBuilder.importPackage("java/lang")!!.toIdentifier,
                    "String".toIdentifier
                ),
                listOf(cst.toLiteral)
            )
            else -> error("Unrecognized constant $cst")
        }
    }

    override fun visitMethodInsn(opcode: Int, owner: String, name: String, desc: String, itf: Boolean) {
        when(opcode) {
            Opcodes.INVOKEVIRTUAL -> {
                val callParamTypes = Type.getArgumentTypes(desc)
                val callReturnType = Type.getReturnType(desc)
                val params = pop(callParamTypes.size)
                val subject = pop(1).first()

                // We have to do a null-pointer check first
                statements += nullPointerAssertion(subject)

                // Create the call
                val call = CallExpression(
                    SelectorExpression(
                        subject,
                        name.capitalizeOnAccess(classBuilder.classPath.methodAccess(owner, name, desc)).toIdentifier
                    ),
                    params
                )

                // We put the result of the call on the stack if it's not void
                if (callReturnType !== Type.VOID_TYPE) {
                    val tempVar = newTempVar()
                    stack += tempVar
                    statements += AssignStatement(listOf(tempVar), Token.DEFINE, listOf(call))
                } else {
                    statements += ExpressionStatement(call)
                }
            }
            else -> error("Unrecognized opcode $opcode")
        }
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
                        classBuilder.importPackage("java/lang")!!.toIdentifier,
                        "NewNullPointerExceptionInstance".toIdentifier
                    ),
                    emptyList()
                )
            )))
        )
    }

    fun pop(amount: Int): List<Expression> {
        val ret = stack.takeLast(amount)
        require(ret.size == amount)
        stack = stack.dropLast(amount)
        return ret
    }

    fun throwError(expr: Expression): Statement {
        return ExpressionStatement(CallExpression("panic".toIdentifier, listOf(expr)))
    }

    fun toFunctionDeclaration(): FunctionDeclaration? {
        var expr = classBuilder.classRefExpr(classBuilder.className, access.isAccessStatic)
        if (access.isAccessStatic || !classBuilder.access.isAccessInterface) expr = StarExpression(expr)
        val receivers = listOf(Field(listOf("this".toIdentifier), expr))
        return FunctionDeclaration(
            name = name.capitalizeOnAccess(access).toIdentifier,
            receivers = receivers,
            type = classBuilder.methodToFunctionType(returnType, paramTypes),
            body = BlockStatement(statements)
        )
    }
}
