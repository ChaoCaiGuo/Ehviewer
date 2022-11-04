package com.hippo.composeUi.composeSearch

import androidx.annotation.IntDef

const val TagSuggestion = 0
const val KeywordSuggestion = 1

@IntDef(TagSuggestion, KeywordSuggestion)
@Retention(AnnotationRetention.SOURCE)
annotation class SearchType


data class Suggestion(
    val mKeyword: String = "",
    val mHint: String = "",
    @SearchType val type:Int = TagSuggestion
)

fun wrapTagKeyword(keyword: String): String {
    var mKeyword = keyword
    mKeyword = mKeyword.trim { it <= ' ' }
    val index1 = mKeyword.indexOf(':')
    if (index1 == -1 || index1 >= mKeyword.length - 1) {
        // Can't find :, or : is the last char
        return mKeyword
    }
    if (mKeyword[index1 + 1] == '"') {
        // The char after : is ", the word must be quoted
        return mKeyword
    }
    val index2 = mKeyword.indexOf(' ')
    return if (index2 <= index1) {
        // Can't find space, or space is before :
        mKeyword
    } else mKeyword.substring(0, index1 + 1) + "\"" + mKeyword.substring(index1 + 1) + "$\""
}