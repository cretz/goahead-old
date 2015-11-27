package goahead

import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import goahead.GoNode.*
import goahead.GoNode.Expression.*
import goahead.GoNode.Declaration.*
import goahead.GoNode.Statement.*
import goahead.GoNode.Specification.*
import org.objectweb.asm.Label
import org.objectweb.asm.Type

class GoMethodBuilder(
    val classBuilder: GoClassBuilder,
    val access: Int,
    val name: String,
    val desc: String
) : MethodVisitor(Opcodes.ASM5) {

    val paramTypes = Type.getArgumentTypes(desc)
    val returnType = Type.getReturnType(desc)

    class TypedExpression(val expr: Expression, val type: Type) {
        fun exprToType(other: Type): Expression {
            if (other == Type.BOOLEAN_TYPE && expr is Identifier && type == Type.INT_TYPE) {
                if (expr.name == "1") return "true".toIdentifier
                else return "false".toIdentifier
            } else return expr
        }
    }

    var stack = listOf<TypedExpression>()
    // TODO: https://devnet.jetbrains.com/thread/475270
    var statementsStack = listOf(StatementList(emptyList(), null))
    data class StatementList(val list: List<Statement>, val label: Label?)

    var gotoLabels = emptySet<Label>()
    var tempVarCounter = 0;
    var errorVarDefined = false

    // TODO
    var usesArguments = false

    override fun visitFieldInsn(opcode: Int, owner: String, name: String, desc: String) {
        when(opcode) {
            Opcodes.GETSTATIC ->
                push(SelectorExpression(
                    staticClassReadReference(owner),
                    classBuilder.fieldName(owner, name).toIdentifier
                ), Type.getType(desc))
            Opcodes.PUTSTATIC -> {
                pushStatements(AssignStatement(
                    listOf(SelectorExpression(
                        staticClassReadReference(owner),
                        classBuilder.fieldName(owner, name).toIdentifier
                    )),
                    Token.ASSIGN,
                    pop(1).map { it.exprToType(Type.getType(desc)) }
                ))
            }
            else -> error("Unrecognized opcode $opcode")
        }
    }

    override fun visitInsn(opcode: Int) {
        when (opcode) {
            Opcodes.DUP -> {
                stack += stack.last()
            }
            Opcodes.ICONST_1 -> {
                push("1".toIdentifier, Type.INT_TYPE)
            }
            Opcodes.ICONST_5 -> {
                push("5".toIdentifier, Type.INT_TYPE)
            }
            Opcodes.RETURN -> {
                // Returns not supported on static init
                if (name != "<clinit>") pushStatements(ReturnStatement(emptyList()))
            }
            else -> error("Unrecognized opcode $opcode")
        }
    }

    override fun visitIntInsn(opcode: Int, operand: Int) {
        when (opcode) {
            Opcodes.BIPUSH -> {
                push(operand.toString().toIdentifier, Type.INT_TYPE)
            }
            else -> error("Unrecognized opcode $opcode")
        }
    }

    override fun visitJumpInsn(opcode: Int, label: Label) {
        when (opcode) {
            Opcodes.GOTO -> {
                gotoLabels += label
                pushStatements(BranchStatement(Token.GOTO, label.toString().toIdentifier))
            }
            Opcodes.IFEQ -> {
                gotoLabels += label
                val left = pop(1)[0]
                pushStatements(IfStatement(
                    condition = BinaryExpression(
                        left = left.expr,
                        operator = Token.EQL,
                        right = TypedExpression("0".toIdentifier, Type.INT_TYPE).exprToType(left.type)
                    ),
                    body = BlockStatement(listOf(BranchStatement(Token.GOTO, label.toString().toIdentifier)))
                ))
            }
            else -> error("Unrecognized opcode $opcode")
        }
    }

    override fun visitLabel(label: Label) {
        if (gotoLabels.contains(label)) {
            statementsStack += StatementList(emptyList(), label)
        }
    }

    override fun visitLdcInsn(cst: Any) {
        when (cst) {
            is String -> push(CallExpression(
                classBuilder.constructorRefExpr("java/lang/String", "(Ljava/lang/String;)Ljava/lang/String;"),
                listOf(cst.toLiteral)
            ), Type.getType(String.javaClass))
            else -> error("Unrecognized constant $cst")
        }
    }

    override fun visitMethodInsn(opcode: Int, owner: String, name: String, desc: String, itf: Boolean) {
        val callParamTypes = Type.getArgumentTypes(desc)
        val params = pop(callParamTypes.size)
        when(opcode) {
            Opcodes.INVOKEVIRTUAL -> {
                val callReturnType = Type.getReturnType(desc)
                val subject = pop(1).first().expr

                // We have to do a null-pointer check first
                pushStatements(nullPointerAssertion(subject))

                // Create the call
                val call = CallExpression(
                    SelectorExpression(
                        subject,
                        classBuilder.methodName(owner, name, desc).toIdentifier
                    ),
                    // TODO: exprToType for each param type
                    params.map { it.expr }
                )

                // We put the result of the call on the stack if it's not void
                if (callReturnType !== Type.VOID_TYPE) {
                    val tempVar = newTempVar()
                    push(tempVar, callReturnType)
                    pushStatements(AssignStatement(listOf(tempVar), Token.DEFINE, listOf(call)))
                } else {
                    pushStatements(ExpressionStatement(call))
                }
            }
            Opcodes.INVOKESPECIAL -> {
                when (name) {
                    "<init>" -> {
                        // We take two off the stack here because this is usually preceded by NEW + DUP
                        stack = stack.dropLast(2)
                        // Create and assign to var
                        val construct = CallExpression(
                            classBuilder.constructorRefExpr(owner, desc),
                            // TODO: exprToType for each param type
                            params.map { it.expr }
                        )
                        val tempVar = newTempVar()
                        push(tempVar, Type.getType(desc))
                        pushStatements(AssignStatement(listOf(tempVar), Token.DEFINE, listOf(construct)))
                    }
                    else -> error("Unrecognized name $name")
                }
            }
            else -> error("Unrecognized opcode $opcode")
        }
    }

    override fun visitTypeInsn(opcode: Int, type: String) {
        when (opcode) {
            Opcodes.NEW ->
                // Just put the class ref on the stack
                push(classBuilder.classRefExpr(type), Type.getType(type))
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

    fun push(expr: Expression, type: Type) {
        stack += TypedExpression(expr, type)
    }

    fun pop(amount: Int): List<TypedExpression> {
        val ret = stack.takeLast(amount)
        require(ret.size == amount)
        stack = stack.dropLast(amount)
        return ret
    }

    fun pushStatements(vararg stmts: Statement) {
        statementsStack = statementsStack.dropLast(1) +
            statementsStack.last().copy(statementsStack.last().list + stmts.toList())
    }

    fun staticClassReadReference(internalName: String): Expression {
        // We do not need an external reference of our self in a static context
        if (access.isAccessStatic && internalName == classBuilder.className) return classBuilder.staticVarIdentifier
        return classBuilder.staticClassReadReference(internalName)
    }

    fun throwError(expr: Expression): Statement {
        return ExpressionStatement(CallExpression("panic".toIdentifier, listOf(expr)))
    }

    fun toFunctionDeclaration(): FunctionDeclaration {
        var expr = classBuilder.classRefExpr(classBuilder.className, access.isAccessStatic)
        if (access.isAccessStatic || !classBuilder.access.isAccessInterface) expr = StarExpression(expr)
        val receivers = listOf(Field(listOf("this".toIdentifier), expr))
        // Each set of statements is its own block for now
        val stmts = statementsStack.flatMap {
            if (it.label != null) listOf(LabeledStatement(it.label.toString().toIdentifier), BlockStatement(it.list))
            else listOf(BlockStatement(it.list))
        }
        return FunctionDeclaration(
            name = classBuilder.methodName(name, paramTypes, access).toIdentifier,
            receivers = receivers,
            type = classBuilder.methodToFunctionType(returnType, paramTypes),
            body = BlockStatement(stmts)
        )
    }
}
