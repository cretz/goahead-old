package goahead

import org.objectweb.asm.Opcodes

// Please keep in alphabetical order

val Int.isAccessPrivate: Boolean get() = this and Opcodes.ACC_PRIVATE == Opcodes.ACC_PRIVATE
val Int.isAccessProtected: Boolean get() = this and Opcodes.ACC_PROTECTED == Opcodes.ACC_PROTECTED
val Int.isAccessPublic: Boolean get() = this and Opcodes.ACC_PUBLIC == Opcodes.ACC_PUBLIC

val String.toIdentifier: GoNode.Identifier get() = GoNode.Identifier(this)