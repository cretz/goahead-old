package goahead
/**
 * Modeled loosely off of AST in golang repo. We specifically choose vars in our implementations
 * here so that the writers can mutate them while visiting byte code. Sadly I can't make this
 * a sealed interface + data class combo per https://devnet.jetbrains.com/message/5559914 and
 * https://github.com/JetBrains/kotlin/blob/master/spec-docs/sealed-hierarchies-and-when.md.
 */
sealed abstract class GoNode {

    /**
     * @property text It's a multiline comment if it contains newlines
     */
    data class Comment(val text: String) : GoNode()

    data class Field(
        val names: List<Expression.Identifier>,
        val type: Expression,
        val tag: Expression.BasicLiteral? = null
    ) : GoNode()

    data class File(
        val packageName: Expression.Identifier,
        val declarations: List<Declaration>
    ) : GoNode()

    data class Package(
        val name: String,
        val files: List<File>
    ) : GoNode()

    sealed abstract class Expression : GoNode() {

        data class Identifier(val name: String) : Expression()

        data class Ellipsis(val elementType: Expression?) : Expression()

        data class BasicLiteral(
            val token: Token,
            val value: String
        ) : Expression()

        data class FunctionLiteral(
            val type: FunctionType,
            val body: Statement.BlockStatement
        ) : Expression()

        data class CompositeLiteral(
            val type: Expression?,
            val elements: List<Expression>
        ) : Expression()

        data class ParenthesizedExpression(val expression: Expression) : Expression()

        data class SelectorExpression(
            val expression: Expression,
            val selector: Identifier
        ) : Expression()

        data class IndexExpression(
            val expression: Expression,
            val index: Expression
        ) : Expression()

        data class SliceExpression(
            val expression: Expression,
            val low: Expression?,
            val high: Expression?,
            val max: Expression?,
            val slice3: Boolean
        ) : Expression()

        data class TypeAssertExpression(
            val expression: Expression,
            val type: Expression?
        ) : Expression()

        data class CallExpression(
            val function: Expression,
            val args: List<Expression>
        ) : Expression()

        data class StarExpression(val expression: Expression) : Expression()

        data class UnaryExpression(
            val operator: Token,
            val operand: Expression
        ) : Expression()

        data class BinaryExpression(
            val left: Expression,
            val operator: Token,
            val right: Expression
        ) : Expression()

        data class KeyValueExpression(
            val key: Expression,
            val value: Expression
        ) : Expression()

        data class ArrayType(
            val type: Expression,
            val length: Expression? = null
        ) : Expression()

        data class StructType(val fields: List<Field>) : Expression()

        data class FunctionType(
            val parameters: List<Field>,
            val results: List<Field>
        ) : Expression()

        data class InterfaceType(val methods: List<Field>) : Expression()

        data class MapType(
            val key: Expression,
            val value: Expression
        ) : Expression()

        enum class ChannelDirection {
            SEND, RECV
        }

        data class ChannelType(
            val direction: ChannelDirection,
            val value: Expression
        ) : Expression()
    }

    sealed abstract class Statement : GoNode() {

        data class DeclarationStatement(val declaration: Declaration) : Statement()

        object EmptyStatement : Statement()

        data class LabeledStatement(
            val label: Expression.Identifier,
            val statement: Statement
        ) : Statement()

        data class ExpressionStatement(val expressioon: Expression) : Statement()

        data class SendStatement(
            val channel: Expression,
            val value: Expression
        ) : Statement()

        data class IncrementDecrementStatement(
            val expression: Expression,
            val token: Token
        ) : Statement()

        data class AssignStatement(
            val left: List<Expression>,
            val token: Token,
            val right: List<Expression>
        ) : Statement()

        data class GoStatement(val call: Expression.CallExpression) : Statement()

        data class DeferStatement(val call: Expression.CallExpression) : Statement()

        data class ReturnStatement(val results: List<Expression>) : Statement()

        data class BranchStatement(
            val token: Token,
            val label: Expression.Identifier?
        ) : Statement()

        data class BlockStatement(val statements: List<Statement>) : Statement()

        data class IfStatement(
            val init: Statement? = null,
            val condition: Expression,
            val body: BlockStatement,
            val elseStatement: Statement? = null
        ) : Statement()

        data class CaseClause(
            val expressions: List<Expression>,
            val body: BlockStatement
        ) : Statement()

        data class SwitchStatement(
            val init: Statement?,
            val tag: Expression?,
            val body: BlockStatement
        ) : Statement()

        data class CommClause(
            val comm: Statement?,
            val body: List<Statement>
        ) : Statement()

        data class SelectStatement(val body: BlockStatement) : Statement()

        data class ForStatement(
            val init: Statement?,
            val condition: Statement?,
            val post: Statement?,
            val body: BlockStatement
        ) : Statement()

        data class RangeStatement(
            val key: Expression?,
            val value: Expression?,
            val token: Token?,
            val expression: Expression,
            val body: BlockStatement
        ) : Statement()
    }

    sealed abstract class Specification : GoNode() {

        data class ImportSpecification(
            val name: Expression.Identifier?,
            val path: Expression.BasicLiteral
        ) : Specification()

        data class ValueSpecification(
            val names: List<Expression.Identifier>,
            val type: Expression?,
            val values: List<Expression.Identifier>
        ) : Specification()

        data class TypeSpecification(
            val name: Expression.Identifier,
            val type: Expression
        ) : Specification()
    }

    sealed abstract class Declaration : GoNode() {

        data class GenericDeclaration(
            val token: Token,
            val specifications: List<Specification>
        ) : Declaration()

        data class FunctionDeclaration(
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