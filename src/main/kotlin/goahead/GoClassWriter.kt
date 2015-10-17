package goahead

import org.objectweb.asm.*
import goahead.GoNode.*
import goahead.GoNode.Expression.*
import goahead.GoNode.Declaration.*
import goahead.GoNode.Statement.*
import goahead.GoNode.Specification.*

class GoClassWriter : ClassVisitor(Opcodes.ASM5) {
    var packageName = ""
    var staticStructName = ""
    var methods = listOf<MethodVisitor>()

    override fun visit(version: Int, access: Int, name: String, signature: String?, superName: String?, interfaces: Array<out String>?) {
        val pieces = name.split('/')
        packageName =
            if (pieces.size() == 1) "main"
            else pieces[pieces.size() - 2]
        staticStructName =
            if (access.isAccessPublic || access.isAccessProtected) pieces.last().decapitalize() + "Static"
            else pieces.last().capitalize() + "Static"
    }

    override fun visitSource(source: String?, debug: String?) {
        // TODO()
    }

    override fun visitOuterClass(owner: String?, name: String?, desc: String?) {
        // TODO()
    }

    override fun visitAnnotation(desc: String?, visible: Boolean): AnnotationVisitor? {
        TODO()
    }

    override fun visitTypeAnnotation(typeRef: Int, typePath: TypePath?, desc: String?, visible: Boolean): AnnotationVisitor? {
        TODO()
    }

    override fun visitAttribute(attr: Attribute?) {
        // TODO()
    }

    override fun visitInnerClass(name: String?, outerName: String?, innerName: String?, access: Int) {
        // TODO()
    }

    override fun visitField(access: Int, name: String?, desc: String?, signature: String?, value: Any?): FieldVisitor? {
        TODO()
    }

    override fun visitMethod(access: Int, name: String, desc: String, signature: String?, exceptions: Array<out String>?): MethodVisitor? {
        val writer = GoMethodWriter()
        methods += writer
        return writer
    }

    override fun visitEnd() {
        // TODO()
    }

    fun toFile(): File {
        TODO()
    }
}

