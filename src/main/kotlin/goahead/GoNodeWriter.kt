package goahead

import goahead.GoNode.*
import goahead.GoNode.Expression.*
import goahead.GoNode.Declaration.*
import goahead.GoNode.Statement.*
import goahead.GoNode.Specification.*

class GoNodeWriter {
    companion object {
        fun fromNode(node: GoNode) = GoNodeWriter().appendGoNode(node).toString()
    }

    val builder = StringBuilder()
    var indention = 0

    fun StringBuilder.indent(): StringBuilder {
        indention++
        return this
    }

    fun StringBuilder.dedent(): StringBuilder {
        indention--
        return this
    }

    fun StringBuilder.newline(): StringBuilder {
        appendln()
        for (i in 1..indention) append('\t')
        return this
    }

    fun <T : GoNode> List<T>.commaSeparated(f: GoNodeWriter.(T) -> Any): StringBuilder {
        this.withIndex().forEach {
            if (it.index > 0) builder.append(", ")
            f(it.value)
        }
        return builder
    }

    fun appendArrayType(expr: ArrayType): StringBuilder {
        builder.append('[')
        if (expr.length != null) appendExpression(expr.length)
        builder.append(']')
        return appendExpression(expr.type)
    }

    fun appendAssignStatement(stmt: AssignStatement): StringBuilder {
        stmt.left.commaSeparated { appendExpression(it) }
        when (stmt.token) {
            Token.ASSIGN -> builder.append(" = ")
            Token.DEFINE -> builder.append(" := ")
            else -> error("Unrecognized token: " + stmt.token)
        }
        return stmt.right.commaSeparated { appendExpression(it) }
    }

    fun appendBasicLiteral(lit: BasicLiteral) = builder.append(lit.value)

    fun appendBinaryExpression(expr: BinaryExpression): StringBuilder {
        require(expr.operator.string != null)
        appendExpression(expr.left).append(' ').append(expr.operator.string).append(' ')
        return appendExpression(expr.right)
    }

    fun appendBlockStatement(stmt: BlockStatement): StringBuilder {
        if (stmt.statements.isEmpty()) return builder.append("{ }")
        builder.append('{').indent()
        stmt.statements.forEach { builder.newline(); appendStatement(it) }
        return builder.dedent().newline().append('}')
    }

    fun appendBranchStatement(stmt: BranchStatement): StringBuilder {
        TODO()
    }

    fun appendCallExpression(expr: CallExpression): StringBuilder {
        appendExpression(expr.function).append('(')
        expr.args.commaSeparated { appendExpression(it) }
        return builder.append(')')
    }

    fun appendCaseClause(stmt: CaseClause): StringBuilder {
        TODO()
    }

    fun appendChannelType(expr: ChannelType): StringBuilder {
        TODO()
    }

    fun appendCommClause(stmt: CommClause): StringBuilder {
        TODO()
    }

    fun appendComment(node: Comment): StringBuilder {
        TODO()
    }

    fun appendCompositeLiteral(expr: CompositeLiteral): StringBuilder {
        TODO()
    }

    fun appendDeclaration(decl: Declaration) = when(decl) {
        is FunctionDeclaration -> appendFunctionDeclaration(decl)
        is GenericDeclaration -> appendGenericDeclaration(decl)
    }

    fun appendDeclarationStatement(stmt: DeclarationStatement): StringBuilder {
        TODO()
    }

    fun appendDeferStatement(stmt: DeferStatement): StringBuilder {
        TODO()
    }

    fun appendEllipsis(expr: Ellipsis): StringBuilder {
        TODO()
    }

    fun appendEmptyStatement(): StringBuilder {
        TODO()
    }

    fun appendExpression(expr: Expression) = when(expr) {
        is ArrayType -> appendArrayType(expr)
        is BasicLiteral -> appendBasicLiteral(expr)
        is BinaryExpression -> appendBinaryExpression(expr)
        is CallExpression -> appendCallExpression(expr)
        is ChannelType -> appendChannelType(expr)
        is CompositeLiteral -> appendCompositeLiteral(expr)
        is Ellipsis -> appendEllipsis(expr)
        is FunctionLiteral -> appendFunctionLiteral(expr)
        is FunctionType -> appendFunctionType(expr)
        is Identifier -> appendIdentifier(expr)
        is IndexExpression -> appendIndexExpression(expr)
        is InterfaceType -> appendInterfaceType(expr)
        is KeyValueExpression -> appendKeyValueExpression(expr)
        is MapType -> appendMapType(expr)
        is ParenthesizedExpression -> appendParenthesizedExpression(expr)
        is SelectorExpression -> appendSelectorExpression(expr)
        is SliceExpression -> appendSliceExpression(expr)
        is StarExpression -> appendStarExpression(expr)
        is StructType -> appendStructType(expr)
        is TypeAssertExpression -> appendTypeAssertExpression(expr)
        is UnaryExpression -> appendUnaryExpression(expr)
    }

    fun appendExpressionStatement(stmt: ExpressionStatement): StringBuilder {
        TODO()
    }

    fun appendField(field: Field): StringBuilder {
        field.names.commaSeparated { appendIdentifier(it) }
        if (field.names.isNotEmpty()) builder.append(' ')
        appendExpression(field.type)
        if (field.tag != null) {
            builder.append(' ')
            appendBasicLiteral(field.tag)
        }
        return builder
    }

    fun appendFile(file: File): StringBuilder {
        builder.append("package ")
        appendIdentifier(file.packageName).newline().newline()
        file.declarations.forEach { appendDeclaration(it).newline().newline() }
        return builder
    }

    fun appendForStatement(stmt: ForStatement): StringBuilder {
        TODO()
    }

    fun appendFunctionDeclaration(decl: FunctionDeclaration): StringBuilder {
        builder.append("func ")
        appendParameters(decl.receivers)
        if (decl.receivers.isNotEmpty()) builder.append(' ')
        appendIdentifier(decl.name)
        appendParameters(decl.type.parameters)
        if (decl.type.results.isNotEmpty()) builder.append(' ')
        if (decl.type.results.size == 1) appendField(decl.type.results.first())
        else appendParameters(decl.type.results)
        if (decl.body != null) {
            builder.append(' ')
            appendBlockStatement(decl.body)
        }
        return builder
    }

    fun appendFunctionLiteral(expr: FunctionLiteral): StringBuilder {
        TODO()
    }

    fun appendFunctionType(expr: FunctionType): StringBuilder {
        TODO()
    }

    fun appendGenericDeclaration(decl: GenericDeclaration): StringBuilder {
        require(decl.specifications.isNotEmpty())
        when (decl.token) {
            Token.IMPORT -> builder.append("import ")
            Token.CONST -> builder.append("const ")
            Token.TYPE -> builder.append("type ")
            Token.VAR -> builder.append("var ")
            else -> error("Unrecognized token: " + decl.token)
        }
        val multiSpec = decl.specifications.size > 1
        if (multiSpec) builder.append('(').indent()
        decl.specifications.forEach {
            if (multiSpec) builder.newline()
            appendSpecification(it)
        }
        if (multiSpec) builder.dedent().newline().append(')')
        return builder
    }

    fun appendGoNode(node: GoNode) = when(node) {
        is Comment -> appendComment(node)
        is Declaration -> appendDeclaration(node)
        is Expression -> appendExpression(node)
        is Field -> appendField(node)
        is File -> appendFile(node)
        is Package -> appendPackage(node)
        is Specification -> appendSpecification(node)
        is Statement -> appendStatement(node)
    }

    fun appendGoStatement(stmt: GoStatement): StringBuilder {
        TODO()
    }

    fun appendIdentifier(id: Identifier) = builder.append(id.name)

    fun appendIfStatement(stmt: IfStatement): StringBuilder {
        builder.append("if ")
        if (stmt.init != null) appendStatement(stmt.init).append("; ")
        appendExpression(stmt.condition).append(' ')
        appendBlockStatement(stmt.body)
        if (stmt.elseStatement != null) {
            builder.append(" else ")
            appendStatement(stmt.elseStatement)
        }
        return builder
    }

    fun appendImportSpecification(spec: ImportSpecification): StringBuilder {
        if (spec.name != null) appendIdentifier(spec.name).append(' ')
        return appendBasicLiteral(spec.path)
    }

    fun appendIncrementDecrementStatement(stmt: IncrementDecrementStatement): StringBuilder {
        TODO()
    }

    fun appendIndexExpression(expr: IndexExpression): StringBuilder {
        TODO()
    }

    fun appendInterfaceType(expr: InterfaceType): StringBuilder {
        TODO()
    }

    fun appendKeyValueExpression(expr: KeyValueExpression): StringBuilder {
        TODO()
    }

    fun appendLabeledStatement(stmt: LabeledStatement): StringBuilder {
        TODO()
    }

    fun appendMapType(expr: MapType): StringBuilder {
        TODO()
    }

    fun appendPackage(node: Package): StringBuilder {
        TODO()
    }

    fun appendParameters(params: List<Field>): StringBuilder {
        if (params.isEmpty()) return builder
        builder.append('(')
        params.commaSeparated { appendField(it) }
        return builder.append(')')
    }

    fun appendParenthesizedExpression(expr: ParenthesizedExpression): StringBuilder {
        TODO()
    }

    fun appendRangeStatement(stmt: RangeStatement): StringBuilder {
        TODO()
    }

    fun appendReturnStatement(stmt: ReturnStatement): StringBuilder {
        builder.append("return ")
        stmt.results.commaSeparated { appendExpression(it) }
        return builder
    }

    fun appendSelectorExpression(expr: SelectorExpression): StringBuilder {
        appendExpression(expr.expression).append('.')
        return appendIdentifier(expr.selector)
    }

    fun appendSelectStatement(stmt: SelectStatement): StringBuilder {
        TODO()
    }

    fun appendSendStatement(stmt: SendStatement): StringBuilder {
        TODO()
    }

    fun appendSliceExpression(expr: SliceExpression): StringBuilder {
        TODO()
    }

    fun appendSpecification(spec: Specification) = when(spec) {
        is ImportSpecification -> appendImportSpecification(spec)
        is ValueSpecification -> appendValueSpecification(spec)
        is TypeSpecification -> appendTypeSpecification(spec)
    }

    fun appendStarExpression(expr: StarExpression): StringBuilder {
        builder.append('*')
        return appendExpression(expr.expression)
    }

    fun appendStatement(stmt: Statement) = when(stmt) {
        is AssignStatement -> appendAssignStatement(stmt)
        is BlockStatement -> appendBlockStatement(stmt)
        is BranchStatement -> appendBranchStatement(stmt)
        is CaseClause -> appendCaseClause(stmt)
        is CommClause -> appendCommClause(stmt)
        is DeclarationStatement -> appendDeclarationStatement(stmt)
        is DeferStatement -> appendDeferStatement(stmt)
        is EmptyStatement -> appendEmptyStatement()
        is ExpressionStatement -> appendExpressionStatement(stmt)
        is ForStatement -> appendForStatement(stmt)
        is GoStatement -> appendGoStatement(stmt)
        is IfStatement -> appendIfStatement(stmt)
        is IncrementDecrementStatement -> appendIncrementDecrementStatement(stmt)
        is LabeledStatement -> appendLabeledStatement(stmt)
        is RangeStatement -> appendRangeStatement(stmt)
        is ReturnStatement -> appendReturnStatement(stmt)
        is SelectStatement -> appendSelectStatement(stmt)
        is SendStatement -> appendSendStatement(stmt)
        is SwitchStatement -> appendSwitchStatement(stmt)
    }

    fun appendStructType(expr: StructType): StringBuilder {
        builder.append("struct {").indent()
        expr.fields.forEach { builder.newline(); appendField(it) }
        builder.dedent()
        if (expr.fields.isEmpty()) builder.append(' ')
        else builder.newline()
        return builder.append('}')
    }

    fun appendSwitchStatement(stmt: SwitchStatement): StringBuilder {
        TODO()
    }

    fun appendTypeAssertExpression(expr: TypeAssertExpression): StringBuilder {
        TODO()
    }

    fun appendTypeSpecification(spec: TypeSpecification): StringBuilder {
        appendIdentifier(spec.name).append(' ')
        return appendExpression(spec.type)
    }

    fun appendUnaryExpression(expr: UnaryExpression): StringBuilder {
        TODO()
    }

    fun appendValueSpecification(spec: ValueSpecification): StringBuilder {
        TODO()
    }
}