package com.budgetnotes.app.navigation

object Routes {
    const val TABS = "tabs"

    const val NOTES_HOME = "notes_home"
    const val NOTE_EDITOR = "editor/{noteId}"
    fun noteEditor(noteId: Long) = "editor/$noteId"

    const val CARDS_HOME = "cards_home"
    const val CARD_EDITOR = "card/{cardId}"
    fun cardEditor(cardId: Long) = "card/$cardId"

    // Legacy aliases
    const val HOME = NOTES_HOME
    const val EDITOR = NOTE_EDITOR
    fun editor(noteId: Long) = noteEditor(noteId)
}
