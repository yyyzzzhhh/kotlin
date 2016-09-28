/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.backend.jvm.descriptors

import org.jetbrains.kotlin.backend.jvm.descriptors.JvmPropertyDescriptorImpl
import org.jetbrains.kotlin.backend.jvm.lower.FileClassDescriptor
import org.jetbrains.kotlin.backend.jvm.lower.FileClassDescriptorImpl
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil
import org.jetbrains.kotlin.ir.SourceManager
import org.jetbrains.kotlin.psi2ir.PsiSourceManager
import org.jetbrains.kotlin.resolve.source.KotlinSourceElement
import org.jetbrains.org.objectweb.asm.Opcodes
import java.util.*

class SpecialDescriptorsFactory(val psiSourceManager: PsiSourceManager) {
    private val enumEntryFieldDescriptors = HashMap<ClassDescriptor, PropertyDescriptor>()

    fun getFieldDescriptorForEnumEntry(enumEntryDescriptor: ClassDescriptor): PropertyDescriptor =
            enumEntryFieldDescriptors.getOrPut(enumEntryDescriptor) {
                createEnumEntryFieldDescriptor(enumEntryDescriptor)
            }

    fun createFileClassDescriptor(fileEntry: SourceManager.FileEntry, packageFragment: PackageFragmentDescriptor): FileClassDescriptor {
        val ktFile = psiSourceManager.getKtFile(fileEntry as PsiSourceManager.PsiFileEntry)
                   ?: throw AssertionError("Unexpected file entry: $fileEntry")
        val fileClassInfo = JvmFileClassUtil.getFileClassInfoNoResolve(ktFile)
        val sourceElement = KotlinSourceElement(ktFile)
        return FileClassDescriptorImpl(fileClassInfo.fileClassFqName.shortName(), packageFragment, sourceElement)
    }

    private fun createEnumEntryFieldDescriptor(enumEntryDescriptor: ClassDescriptor): PropertyDescriptor {
        assert(enumEntryDescriptor.kind == ClassKind.ENUM_ENTRY) { "Should be enum entry: $enumEntryDescriptor" }

        val enumClassDescriptor = enumEntryDescriptor.containingDeclaration as ClassDescriptor
        assert(enumClassDescriptor.kind == ClassKind.ENUM_CLASS) { "Should be enum class: $enumClassDescriptor"}

        return JvmPropertyDescriptorImpl.createVal(
                enumEntryDescriptor.name,
                enumClassDescriptor.defaultType,
                enumClassDescriptor,
                enumEntryDescriptor.annotations,
                Modality.FINAL,
                Visibilities.PUBLIC,
                Opcodes.ACC_ENUM,
                enumEntryDescriptor.source
        )
    }
}