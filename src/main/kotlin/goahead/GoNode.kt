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
    data class Comment(var text: String) : GoNode()

    data class Field(
        var names: MutableList<Expression.Identifier>,
        var type: Expression,
        var tag: Expression.BasicLiteral?
    ) : GoNode()

    data class File(
        var packageName: Expression.Identifier,
        var declarations: MutableList<Declaration> = arrayListOf(),
        var imports: MutableList<Specification.ImportSpecification> = arrayListOf()
    ) : GoNode()

    data class Package(
        var name: String,
        var files: MutableList<File>
    ) : GoNode()

    sealed abstract class Expression : GoNode() {

        data class Identifier(var name: String) : Expression()

        data class Ellipsis(var elementType: Expression?) : Expression()

        data class BasicLiteral(
            var token: Token,
            var value: String
        ) : Expression()

        data class FunctionLiteral(
            var type: FunctionType,
            var body: Statement.BlockStatement
        ) : Expression()

        data class CompositeLiteral(
            var type: Expression?,
            var elements: MutableList<Expression>
        ) : Expression()

        data class ParenthesizedExpression(var expression: Expression) : Expression()

        data class SelectorExpression(
            var expression: Expression,
            var selector: Identifier
        ) : Expression()

        data class IndexExpression(
            var expression: Expression,
            var index: Expression
        ) : Expression()

        data class SliceExpression(
            var expression: Expression,
            var low: Expression?,
            var high: Expression?,
            var max: Expression?,
            var slice3: Boolean
        ) : Expression()

        data class TypeAssertExpression(
            var expression: Expression,
            var type: Expression?
        ) : Expression()

        data class CallExpression(
            var function: Expression,
            var args: MutableList<Expression>
        ) : Expression()

        data class StarExpression(var expression: Expression) : Expression()

        data class UnaryExpression(
            var operator: Token,
            var operand: Expression
        ) : Expression()

        data class BinaryExpression(
            var left: Expression,
            var operator: Token,
            var right: Expression
        ) : Expression()

        data class KeyValueExpression(
            var key: Expression,
            var value: Expression
        ) : Expression()

        data class ArrayType(
            var length: Expression,
            var type: Expression
        ) : Expression()

        data class StructType(var fields: MutableList<Field>) : Expression()

        data class FunctionType(
            var parameters: MutableList<Field>,
            var results: MutableList<Field>
        ) : Expression()

        data class InterfaceType(var methods: MutableList<Field>) : Expression()

        data class MapType(
            var key: Expression,
            var value: Expression
        ) : Expression()

        enum class ChannelDirection {
            SEND, RECV
        }

        data class ChannelType(
            var direction: ChannelDirection,
            var value: Expression
        ) : Expression()
    }

    sealed abstract class Statement : GoNode() {

        data class DeclarationStatement(var declaration: Declaration) : Statement()

        object EmptyStatement : Statement()

        data class LabeledStatement(
            var label: Expression.Identifier,
            var statement: Statement
        ) : Statement()

        data class ExpressionStatement(var expressioon: Expression) : Statement()

        data class SendStatement(
            var channel: Expression,
            var value: Expression
        ) : Statement()

        data class IncrementDecrementStatement(
            var expression: Expression,
            var token: Token
        ) : Statement()

        data class AssignStatement(
            var left: MutableList<Expression>,
            var token: Token,
            var right: MutableList<Expression>
        ) : Statement()

        data class GoStatement(var call: Expression.CallExpression) : Statement()

        data class DeferStatement(var call: Expression.CallExpression) : Statement()

        data class ReturnStatement(var results: MutableList<Expression>) : Statement()

        data class BranchStatement(
            var token: Token,
            var label: Expression.Identifier?
        ) : Statement()

        data class BlockStatement(var statements: MutableList<Statement>) : Statement()

        data class IfStatement(
            var init: Statement?,
            var condition: Expression,
            var body: BlockStatement,
            var elseStatement: Statement?
        ) : Statement()

        data class CaseClause(
            var expressions: MutableList<Expression>,
            var body: BlockStatement
        ) : Statement()

        data class SwitchStatement(
            var init: Statement?,
            var tag: Expression?,
            var body: BlockStatement
        ) : Statement()

        data class CommClause(
            var comm: Statement?,
            var body: MutableList<Statement>
        ) : Statement()

        data class SelectStatement(var body: BlockStatement) : Statement()

        data class ForStatement(
            var init: Statement?,
            var condition: Statement?,
            var post: Statement?,
            var body: BlockStatement
        ) : Statement()

        data class RangeStatement(
            var key: Expression?,
            var value: Expression?,
            var token: Token?,
            var expression: Expression,
            var body: BlockStatement
        ) : Statement()
    }

    sealed abstract class Specification : GoNode() {

        data class ImportSpecification(
            var name: Expression.Identifier?,
            var path: Expression.BasicLiteral
        ) : Specification()

        data class ValueSpecification(
            var names: MutableList<Expression.Identifier>,
            var type: Expression?,
            var values: MutableList<Expression.Identifier>
        ) : Specification()

        data class TypeSpecification(
            var name: Expression.Identifier,
            var type: Expression
        ) : Specification()
    }

    sealed abstract class Declaration : GoNode() {

        data class GenericDeclaration(
            var token: Token,
            var specifications: MutableList<Specification>
        ) : Declaration()

        data class FunctionDeclaration(
            var receivers: MutableList<Field>,
            var name: Expression.Identifier,
            var type: Expression.FunctionType,
            var body: Statement.BlockStatement?
        ) : Declaration()
    }

    enum class Token(var string: String? = null) {
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