package goahead
/**
 * Modeled loosely off of AST in golang repo. We specifically choose vars in our implementations
 * here so that the writers can mutate them while visiting byte code. Sadly I can't make this
 * a sealed interface + data class combo per https://devnet.jetbrains.com/message/5559914 and
 * https://github.com/JetBrains/kotlin/blob/master/spec-docs/sealed-hierarchies-and-when.md.
 */
sealed class GoNode {

    /**
     * @property text It's a multiline comment if it contains newlines
     */
    class Comment(val text: String) : GoNode()

    class Field(
        val names: List<Expression.Identifier>,
        val type: Expression,
        val tag: Expression.BasicLiteral? = null
    ) : GoNode()

    class File(
        val packageName: Expression.Identifier,
        val declarations: List<Declaration>
    ) : GoNode()

    class Package(
        val name: String,
        val files: List<File>
    ) : GoNode()

    sealed class Expression : GoNode() {

        class Identifier(val name: String) : Expression()

        class Ellipsis(val elementType: Expression?) : Expression()

        class BasicLiteral(
            val token: Token,
            val value: String
        ) : Expression()

        class FunctionLiteral(
            val type: FunctionType,
            val body: Statement.BlockStatement
        ) : Expression()

        class CompositeLiteral(
            val type: Expression?,
            val elements: List<Expression>
        ) : Expression()

        class ParenthesizedExpression(val expression: Expression) : Expression()

        class SelectorExpression(
            val expression: Expression,
            val selector: Identifier
        ) : Expression()

        class IndexExpression(
            val expression: Expression,
            val index: Expression
        ) : Expression()

        class SliceExpression(
            val expression: Expression,
            val low: Expression?,
            val high: Expression?,
            val max: Expression?,
            val slice3: Boolean
        ) : Expression()

        class TypeAssertExpression(
            val expression: Expression,
            val type: Expression?
        ) : Expression()

        class CallExpression(
            val function: Expression,
            val args: List<Expression> = emptyList()
        ) : Expression()

        class StarExpression(val expression: Expression) : Expression()

        class UnaryExpression(
            val operator: Token,
            val operand: Expression
        ) : Expression()

        class BinaryExpression(
            val left: Expression,
            val operator: Token,
            val right: Expression
        ) : Expression()

        class KeyValueExpression(
            val key: Expression,
            val value: Expression
        ) : Expression()

        class ArrayType(
            val type: Expression,
            val length: Expression? = null
        ) : Expression()

        class StructType(val fields: List<Field>) : Expression()

        class FunctionType(
            val parameters: List<Field>,
            val results: List<Field>
        ) : Expression()

        class InterfaceType(val methods: List<Field>) : Expression()

        class MapType(
            val key: Expression,
            val value: Expression
        ) : Expression()

        enum class ChannelDirection {
            SEND, RECV
        }

        class ChannelType(
            val direction: ChannelDirection,
            val value: Expression
        ) : Expression()
    }

    sealed class Statement : GoNode() {

        class DeclarationStatement(val declaration: Declaration) : Statement()

        object EmptyStatement : Statement()

        class LabeledStatement(
            val label: Expression.Identifier,
            val statement: Statement
        ) : Statement()

        class ExpressionStatement(val expression: Expression) : Statement()

        class SendStatement(
            val channel: Expression,
            val value: Expression
        ) : Statement()

        class IncrementDecrementStatement(
            val expression: Expression,
            val token: Token
        ) : Statement()

        class AssignStatement(
            val left: List<Expression>,
            val token: Token,
            val right: List<Expression>
        ) : Statement()

        class GoStatement(val call: Expression.CallExpression) : Statement()

        class DeferStatement(val call: Expression.CallExpression) : Statement()

        class ReturnStatement(val results: List<Expression>) : Statement()

        class BranchStatement(
            val token: Token,
            val label: Expression.Identifier?
        ) : Statement()

        class BlockStatement(val statements: List<Statement>) : Statement()

        class IfStatement(
            val init: Statement? = null,
            val condition: Expression,
            val body: BlockStatement,
            val elseStatement: Statement? = null
        ) : Statement()

        class CaseClause(
            val expressions: List<Expression>,
            val body: BlockStatement
        ) : Statement()

        class SwitchStatement(
            val init: Statement?,
            val tag: Expression?,
            val body: BlockStatement
        ) : Statement()

        class CommClause(
            val comm: Statement?,
            val body: List<Statement>
        ) : Statement()

        class SelectStatement(val body: BlockStatement) : Statement()

        class ForStatement(
            val init: Statement?,
            val condition: Statement?,
            val post: Statement?,
            val body: BlockStatement
        ) : Statement()

        class RangeStatement(
            val key: Expression?,
            val value: Expression?,
            val token: Token?,
            val expression: Expression,
            val body: BlockStatement
        ) : Statement()
    }

    sealed class Specification : GoNode() {

        class ImportSpecification(
            val name: Expression.Identifier?,
            val path: Expression.BasicLiteral
        ) : Specification()

        class ValueSpecification(
            val names: List<Expression.Identifier>,
            val type: Expression?,
            val values: List<Expression.Identifier>
        ) : Specification()

        class TypeSpecification(
            val name: Expression.Identifier,
            val type: Expression
        ) : Specification()
    }

    sealed class Declaration : GoNode() {

        class GenericDeclaration(
            val token: Token,
            val specifications: List<Specification>
        ) : Declaration()

        class FunctionDeclaration(
            val receivers: List<Field>,
            val name: Expression.Identifier,
            val type: Expression.FunctionType,
            val body: Statement.BlockStatement?
        ) : Declaration()
    }

    enum class Token(val string: String? = null) {
        // Special tokens
        ILLEGAL(),
        EOF(),
        COMMENT(),

        IDENT(),
        INT(),
        FLOAT(),
        IMAG(),
        CHAR(),
        STRING(),

        ADD("+"),
        SUB("-"),
        MUL("*"),
        QUO("/"),
        REM("%"),

        AND("&"),
        OR("|"),
        XOR("^"),
        SHL("<<"),
        SHR(">>"),
        AND_NOT("&^"),

        ADD_ASSIGN("+="),
        SUB_ASSIGN("-="),
        MUL_ASSIGN("*="),
        QUO_ASSIGN("/="),
        REM_ASSIGN("%="),

        AND_ASSIGN("&="),
        OR_ASSIGN("|="),
        XOR_ASSIGN("^="),
        SHL_ASSIGN("<<="),
        SHR_ASSIGN(">>="),
        AND_NOT_ASSIGN("&^="),

        LAND("&&"),
        LOR("||"),
        ARROW("<-"),
        INC("++"),
        DEC("--"),

        EQL("=="),
        LSS("<"),
        GTR(">"),
        ASSIGN("="),
        NOT("!"),

        NEQ("!="),
        LEQ("<="),
        GEQ(">="),
        DEFINE(":="),
        ELLIPSIS("..."),

        LPAREN("("),
        LBRACK("["),
        LBRACE("{"),
        COMMA(","),
        PERIOD("."),

        RPAREN(")"),
        RBRACK("]"),
        RBRACE("}"),
        SEMICOLON(";"),
        COLON(":"),

        BREAK(),
        CASE(),
        CHAN(),
        CONST(),
        CONTINUE(),

        DEFAULT(),
        DEFER(),
        ELSE(),
        FALLTHROUGH(),
        FOR(),

        FUNC(),
        GO(),
        GOTO(),
        IF(),
        IMPORT(),

        INTERFACE(),
        MAP(),
        PACKAGE(),
        RANGE(),
        RETURN(),

        SELECT(),
        STRUCT(),
        SWITCH(),
        TYPE(),
        VAR(),
    }

}