package com.example.myapplication

object SortUtils {

    // the emotion order that the list will be sorted by
    private val EMOTION_ORDER = listOf(
        Emotion.ANGER,
        Emotion.SADNESS,
        Emotion.FEAR,
        Emotion.DISGUST,
        Emotion.SURPRISE,
        Emotion.JOY,
        Emotion.NEUTRAL
    )

    // the key used to sort the journal entries by the emotion
    private fun key(e: JournalEntry): Int =
        EMOTION_ORDER.indexOf(e.emotion)

    // sort by emotion
    // takes the emotion list and sort method
    fun sortByEmotion(list: MutableList<JournalEntry>, method: SortMethod) {
        when (method) {
            SortMethod.BUBBLE -> bubble(list)
            SortMethod.INSERTION -> insertion(list)
            SortMethod.SELECTION -> selection(list)
        }
    }

    // bubble sort
    private fun bubble(a: MutableList<JournalEntry>) {
        val n = a.size
        for (i in 0 until n - 1) {
            var swapped = false
            for (j in 0 until n - i - 1) {
                if (key(a[j]) > key(a[j + 1])) {
                    val tmp = a[j]
                    a[j] = a[j + 1]
                    a[j + 1] = tmp
                    swapped = true
                }
            }
            if (!swapped) return
        }
    }

    // insertion sort
    private fun insertion(a: MutableList<JournalEntry>) {
        for (i in 1 until a.size) {
            val cur = a[i]
            var j = i - 1
            while (j >= 0 && key(a[j]) > key(cur)) {
                a[j + 1] = a[j]
                j--
            }
            a[j + 1] = cur
        }
    }

    // selection sort
    private fun selection(a: MutableList<JournalEntry>) {
        for (i in 0 until a.size) {
            var min = i
            for (j in i + 1 until a.size) {
                if (key(a[j]) < key(a[min])) min = j
            }
            if (min != i) {
                val tmp = a[i]
                a[i] = a[min]
                a[min] = tmp
            }
        }
    }
}
