package goahead

import goahead.GoNode.*
import goahead.GoNode.Expression.*
import goahead.GoNode.Declaration.*
import goahead.GoNode.Statement.*
import goahead.GoNode.Specification.*

class GoNodeBuilder {
    val builder = StringBuilder()

    fun StringBuilder.indent() = this.append('\t')

    fun appendArrayType(expr: ArrayType): StringBuilder {
        TODO()
    }

    fun appendAssignStatement(stmt: AssignStatement): StringBuilder {
        TODO()
    }

    fun appendBasicLiteral(lit: BasicLiteral) = builder.append(lit.value)

    fun appendBinaryExpression(expr: BinaryExpression): StringBuilder {
        TODO()
    }

    fun appendBlockStatement(stmt: BlockStatement): StringBuilder {
        TODO()
    }

    fun appendBranchStatement(stmt: BranchStatement): StringBuilder {
        TODO()
    }

    fun appendCallExpression(expr: CallExpression): StringBuilder {
        TODO()
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
        field.names.withIndex().forEach {
            if (it.index > 0) builder.append(", ")
            appendIdentifier(it.value)
        }
        if (field.names.isNotEmpty()) builder.append(' ')
        appendExpression(field.type)
        TODO("tag")
    }

    fun appendFile(file: File): StringBuilder {
        builder.append("package ").appendln(file.packageName).appendln().appendln()

        builder.append("import (").appendln()
        file.imports.forEach {
            builder.indent()
            // Should be fixed in https://github.com/jetbrains/kotlin/commit/4b35e3b1358dda468ec3eb8538ed4655302b7063
            // per https://youtrack.jetbrains.com/issue/KT-8643
            // if (it.name != null) appendIdentifier(it.name).append(' ')
            if (it.name != null) appendIdentifier(it.name!!).append(' ')
            appendBasicLiteral(it.path).appendln()
        }
        builder.append(')').appendln().appendln()

        file.declarations.forEach {
            appendDeclaration(it).appendln().appendln()
        }

        return builder
    }

    fun appendForStatement(stmt: ForStatement): StringBuilder {
        TODO()
    }

    fun appendFunctionDeclaration(decl: FunctionDeclaration): StringBuilder {
        builder.append("func ")
        appendParameters(decl.receivers)
        TODO()
    }

    fun appendFunctionLiteral(expr: FunctionLiteral): StringBuilder {
        TODO()
    }

    fun appendFunctionType(expr: FunctionType): StringBuilder {
        TODO()
    }

    fun appendGenericDeclaration(decl: GenericDeclaration): StringBuilder { TODO() }

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
        TODO()
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
        params.withIndex().forEach {
            if (it.index > 0) builder.append(", ")
            appendField(it.value)
        }
        return builder.append(')')
    }

    fun appendParenthesizedExpression(expr: ParenthesizedExpression): StringBuilder {
        TODO()
    }

    fun appendRangeStatement(stmt: RangeStatement): StringBuilder {
        TODO()
    }

    fun appendReturnStatement(stmt: ReturnStatement): StringBuilder {
        TODO()
    }

    fun appendSelectorExpression(expr: SelectorExpression): StringBuilder {
        TODO()
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

    fun appendSpecification(node: Specification): StringBuilder {
        TODO()
    }

    fun appendStarExpression(expr: StarExpression): StringBuilder {
        TODO()
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
        TODO()
    }

    fun appendSwitchStatement(stmt: SwitchStatement): StringBuilder {
        TODO()
    }

    fun appendTypeAssertExpression(expr: TypeAssertExpression): StringBuilder {
        TODO()
    }

    fun appendUnaryExpression(expr: UnaryExpression): StringBuilder {
        TODO()
    }
}