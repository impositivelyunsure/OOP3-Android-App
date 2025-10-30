package com.example.myapplication

enum class SearchMethod { HASHMAP, BINARY_TREE, DOUBLY_LINKED_LIST }

// --- Public entry point ---
object SearchUtils {
    fun searchByEmotion(
        items: List<JournalEntry>,
        emotion: Emotion,
        method: SearchMethod
    ): List<JournalEntry> = when (method) {
        SearchMethod.HASHMAP -> HashSearch(items).find(emotion)
        SearchMethod.BINARY_TREE -> EmotionBST(items).find(emotion)
        SearchMethod.DOUBLY_LINKED_LIST -> DLL(items).find(emotion)
    }
}

/* -------------------- HashMap -------------------- */
private class HashSearch(items: List<JournalEntry>) {
    private val map: Map<Emotion, List<JournalEntry>> =
        items.groupBy { it.emotion }
    fun find(e: Emotion): List<JournalEntry> = map[e].orEmpty()
}

/* -------------------- Binary Search Tree -------------------- */
private class EmotionBST(items: List<JournalEntry>) {
    private data class Node(
        val key: Int,
        val bucket: MutableList<JournalEntry> = mutableListOf(),
        var left: Node? = null,
        var right: Node? = null
    )

    private var root: Node? = null

    init {
        items.forEach { insert(it) }
    }

    private fun insert(entry: JournalEntry) {
        val k = entry.emotion.ordinal
        root = insertRec(root, k, entry)
    }

    private fun insertRec(node: Node?, key: Int, entry: JournalEntry): Node {
        if (node == null) return Node(key, mutableListOf(entry))
        when {
            key < node.key -> node.left = insertRec(node.left, key, entry)
            key > node.key -> node.right = insertRec(node.right, key, entry)
            else -> node.bucket += entry
        }
        return node
    }

    fun find(e: Emotion): List<JournalEntry> {
        val k = e.ordinal
        var n = root
        while (n != null) {
            n = when {
                k < n.key -> n.left
                k > n.key -> n.right
                else -> return n.bucket.toList()
            }
        }
        return emptyList()
    }
}

/* -------------------- Doubly Linked List -------------------- */
private class DLL(items: List<JournalEntry>) {
    private class Node(val v: JournalEntry) {
        var prev: Node? = null
        var next: Node? = null
    }
    private var head: Node? = null
    private var tail: Node? = null

    init {
        var last: Node? = null
        for (e in items) {
            val n = Node(e)
            if (head == null) head = n
            last?.next = n
            n.prev = last
            last = n
        }
        tail = last
    }

    fun find(e: Emotion): List<JournalEntry> {
        val out = mutableListOf<JournalEntry>()
        var n = head
        while (n != null) {
            if (n.v.emotion == e) out += n.v
            n = n.next
        }
        return out
    }
}