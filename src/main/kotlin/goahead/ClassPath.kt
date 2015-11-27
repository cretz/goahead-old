package goahead

import org.objectweb.asm.Opcodes

class ClassPath(val entries: List<ClassPath.ClassPathEntry>) {

    interface ClassPathEntry {
        fun findClass(internalClassName: String): ByteArray?
        fun close()

        companion object {
            fun fromString(classPath: String): List<ClassPathEntry> {
                TODO()
            }
        }
    }

    fun isInterface(internalClassName: String): Boolean {
//        TODO()
        return false
    }

    fun fieldAccess(internalClassName: String, fieldName: String): Int {
        // TODO()
        return Opcodes.ACC_PUBLIC
    }

    fun methodAccess(internalClassName: String, methodName: String, desc: String): Int {
        // TODO()
        return Opcodes.ACC_PUBLIC
    }
}