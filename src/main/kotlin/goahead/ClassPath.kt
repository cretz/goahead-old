package goahead

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
        TODO()
    }

    fun fieldAccess(internalClassName: String, fieldName: String): Int {
        TODO()
    }

    fun methodAccess(internalClassName: String, methodName: String, desc: String): Int {
        TODO()
    }
}