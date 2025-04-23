package com.khairulin.kazakhverb.grammar

class PhrasalBuilder {
    companion object {
        val NOT_SUPPORTED = "<not supported>"
        val NOT_SUPPORTED_PHRASAL = Phrasal(parts = listOf(), raw = NOT_SUPPORTED, forbidden = false)
    }

    var parts: MutableList<PhrasalPart> = mutableListOf()
    var forbidden: Boolean = false

    val isEmpty: Boolean
        get() = parts.isEmpty()

    private fun addPart(part: PhrasalPart, allowEmpty: Boolean = false): PhrasalBuilder {
        if (!part.content.isEmpty() || allowEmpty) {
            parts.add(part)
        }
        return this
    }

    private fun addPart(type: PhrasalPartType, content: String): PhrasalBuilder {
        return addPart(PhrasalPart(type, content))
    }

    fun unclassified(content: String): PhrasalBuilder {
        return addPart(type = PhrasalPartType.Unclassified, content = content)
    }

    fun space(): PhrasalBuilder {
        return addPart(type = PhrasalPartType.Space, content = " ")
    }

    fun punctuation(content: String): PhrasalBuilder {
        return addPart(type = PhrasalPartType.Punctuation, content = content)
    }

    fun verbBase(content: String): PhrasalBuilder {
        return addPart(type = PhrasalPartType.VerbBase, content = content)
    }

    fun tenseAffix(content: String): PhrasalBuilder {
        return addPart(type = PhrasalPartType.VerbTenseAffix, content = content)
    }

    fun personalAffix(content: String): PhrasalBuilder {
        return addPart(type = PhrasalPartType.VerbPersonalAffix, content = content)
    }

    fun negation(content: String): PhrasalBuilder {
        return addPart(type = PhrasalPartType.VerbNegation, content = content)
    }

    fun questionParticle(content: String): PhrasalBuilder {
        return addPart(type = PhrasalPartType.QuestionParticle, content = content)
    }

    fun auxVerb(phrasal: Phrasal): PhrasalBuilder {
        var result = this
        for (part in phrasal.parts) {
            result = addPart(PhrasalPart(part.partType, part.content, aux = true))
        }
        return result
    }

    fun setForbidden(forbidden: Boolean): PhrasalBuilder {
        this.forbidden = forbidden
        return this
    }

    private fun getLastNonemptyIndex(): Int {
        var index = parts.size - 1
        while (index > 0 && parts[index].content.isEmpty()) {
            index -= 1
        }
        return index
    }

    fun getLastItem(): Char {
        val index = getLastNonemptyIndex()
        return parts[index].content.last()
    }

    fun build(): Phrasal {
        val partStrings = mutableListOf<String>()
        for (part in parts) {
            partStrings.add(part.content)
        }
        return Phrasal(parts = parts, raw = partStrings.joinToString(separator = ""), forbidden = forbidden)
    }
}