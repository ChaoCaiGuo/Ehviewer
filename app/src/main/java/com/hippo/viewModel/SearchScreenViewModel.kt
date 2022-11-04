package com.hippo.viewModel

import android.text.TextUtils
import androidx.lifecycle.ViewModel
import com.hippo.composeUi.composeSearch.KeywordSuggestion
import com.hippo.composeUi.composeSearch.Suggestion
import com.hippo.composeUi.composeSearch.TagSuggestion
import com.hippo.composeUi.composeSearch.wrapTagKeyword
import com.hippo.ehviewer.client.EhTagDatabase
import com.hippo.ehviewer.widget.SearchDatabase
import com.hippo.viewModel.baseViewModel.Container
import com.hippo.viewModel.baseViewModel.UiAction
import com.hippo.viewModel.baseViewModel.UiState
import com.hippo.viewModel.baseViewModel.containers
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

@HiltViewModel
class SearchScreenViewModel @Inject constructor(
    mSearchDatabase: SearchDatabase,
    mEhTagDatabase:EhTagDatabase?
) : ViewModel() {

    data class SearchState(
        val mText:String ="",
        val mSuggestionList:MutableList<Suggestion> = ArrayList(),
        val mSearchHistory:MutableList<Suggestion> = ArrayList(),
        val mSearchDatabase: SearchDatabase,
        val mEhTagDatabase: EhTagDatabase?
    ) : UiState

    sealed class SearchAction : UiAction<SearchState>() {
        class SetSearchText(val value: String):SearchAction(){
            override suspend fun invoke(_viewState: MutableStateFlow<SearchState>) {
                _viewState.updateState { copy(mText =value) }
                UpdateSuggestions.invoke(_viewState)
            }
        }

        object UpdateSuggestions: SearchAction(){
            override suspend fun invoke(_viewState: MutableStateFlow<SearchState>) {
                val suggestions: MutableList<Suggestion> = ArrayList()
                val history: MutableList<Suggestion> = ArrayList()
                val text: String = _viewState.value.mText
                val keywords = _viewState.value.mSearchDatabase.getSuggestions(text, 128)
                for (keyword in keywords) {
                    history.add(Suggestion(mKeyword = keyword, type = KeywordSuggestion))
                }
                val ehTagDatabase = _viewState.value.mEhTagDatabase
                if (!TextUtils.isEmpty(text) && ehTagDatabase != null && !text.endsWith(" ")) {
                    val s = text.split(" ".toRegex()).dropLastWhile { it.isEmpty() }
                        .toTypedArray()
                    if (s.isNotEmpty()) {
                        val keyword = s[s.size - 1]
                        val searchHints = ehTagDatabase.suggest(keyword)
                        for (searchHint in searchHints) {
                            suggestions.add(Suggestion(mHint = searchHint.first, mKeyword =searchHint.second, type = TagSuggestion))
                        }
                    }
                }
                _viewState.updateState { copy(mSuggestionList = suggestions,mSearchHistory = history)  }
            }
        }

        class OnClick(private val suggestion: Suggestion): SearchAction(){
            override suspend fun invoke(_viewState: MutableStateFlow<SearchState>) {
                val temp = when (suggestion.type) {
                    TagSuggestion -> {
                        val text: String =_viewState.value.mText
                        var temp: String = wrapTagKeyword(suggestion.mKeyword) + " "
                        if (text.contains(" ")) {
                            temp = text.substring(0, text.lastIndexOf(" ")) + " " + temp
                        }
                        temp
                    }
                    KeywordSuggestion ->  suggestion.mKeyword
                    else -> "error"
                }
                SetSearchText(temp).invoke(_viewState)

            }
        }

        class OnLongClick(private val suggestion: Suggestion): UiAction<SearchState>(){
            override suspend fun invoke(_viewState: MutableStateFlow<SearchState>) {
                val temp = when (suggestion.type) {
                    TagSuggestion -> {
                        val text: String =_viewState.value.mText
                        var temp: String = wrapTagKeyword(suggestion.mKeyword) + " "
                        if (text.contains(" ")) {
                            temp = text.substring(0, text.lastIndexOf(" ")) + " " + temp
                        }
                        temp
                    }
                    KeywordSuggestion ->  suggestion.mKeyword
                    else -> "error"
                }
                SetSearchText(temp).invoke(_viewState)

            }
        }

    }

    fun sendEvent(event:SearchAction){
        _container.sendEvent(event)
    }


    private val _container by containers<SearchState,SearchAction>(SearchState(mSearchDatabase =mSearchDatabase,mEhTagDatabase= mEhTagDatabase))
    val container: Container<SearchState,SearchAction> = _container


}
