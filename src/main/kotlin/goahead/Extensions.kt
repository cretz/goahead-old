package goahead

import org.objectweb.asm.Opcodes
import goahead.GoNode.*
import org.objectweb.asm.Type

// Please keep in alphabetical order

val Int.isAccessPrivate: Boolean get() = this and Opcodes.ACC_PRIVATE == Opcodes.ACC_PRIVATE
val Int.isAccessProtected: Boolean get() = this and Opcodes.ACC_PROTECTED == Opcodes.ACC_PROTECTED
val Int.isAccessPublic: Boolean get() = this and Opcodes.ACC_PUBLIC == Opcodes.ACC_PUBLIC
val Int.isAccessStatic: Boolean get() = this and Opcodes.ACC_STATIC == Opcodes.ACC_STATIC

inline fun <T, R : Any> Iterable<T>.mapNoNull(transform: (T) -> R?) = this.map(transform).filterNotNull()

fun String.capitalizeOnAccess(access: Int): String =
    if (access and Opcodes.ACC_PUBLIC == Opcodes.ACC_PUBLIC) this.capitalize()
    else if (access and Opcodes.ACC_PROTECTED == Opcodes.ACC_PROTECTED) this.capitalize()
    else this

val String.golangUunescaped: String get() = /* TODO */ this
val String.toIdentifier: Expression.Identifier get() = Expression.Identifier(this)
val String.toLiteral: Expression.BasicLiteral get() = Expression.BasicLiteral(Token.STRING, this.golangUunescaped)