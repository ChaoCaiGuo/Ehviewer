package com.hippo.viewModel

import android.text.TextUtils
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import com.hippo.composeUi.composeSearch.KeywordSuggestion
import com.hippo.composeUi.composeSearch.Suggestion
import com.hippo.composeUi.composeSearch.TagSuggestion
import com.hippo.composeUi.composeSearch.wrapTagKeyword
import com.hippo.ehviewer.EhApplication
import com.hippo.ehviewer.R
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
        val mMinRating:Int = -1,
        val categorySelected: SnapshotStateList<Boolean> = mutableStateListOf(),
        val enabledAdvanceOptions: Boolean = false,
        val mSuggestionList:MutableList<Suggestion> = ArrayList(),
        val mSearchHistory:SnapshotStateList<Suggestion> = mutableStateListOf(),
        val mAdvanceOptions:SnapshotStateList<Boolean> = mutableStateListOf(),
        val mSearchDatabase: SearchDatabase,
        val mEhTagDatabase: EhTagDatabase?
    ) : UiState

    sealed class SearchAction : UiAction<SearchState>() {
        /**
         * 修改Search的文本
         */
        class SetSearchText(val value: String):SearchAction(){
            override suspend operator fun invoke(_viewState: MutableStateFlow<SearchState>) {
                _viewState.updateState { copy(mText =value) }
                UpdateSuggestions.invoke(_viewState)
            }
        }

        /**
         * 显示/隐藏高级选项
         */
        object ChangeEnabledAdvanceOptions:SearchAction(){
            override suspend fun invoke(_viewState: MutableStateFlow<SearchState>) {
                _viewState.updateState { copy(enabledAdvanceOptions =! _viewState.value.enabledAdvanceOptions) }
            }
        }

        /**
         * 修改高级选项里的最少页数
         */
        class ChangeMinRating(val value: Int):SearchAction(){
            override suspend fun invoke(_viewState: MutableStateFlow<SearchState>) {
                _viewState.updateState{copy(mMinRating = value)}
            }
        }

        /**
         * 选中AdvanceOptionsSelected
         */
        class ChangeAdvanceOptionsSelected(val value: Int):SearchAction(){
            override suspend fun invoke(_viewState: MutableStateFlow<SearchState>) {
                val temp =_viewState.value.mAdvanceOptions
                if (temp.size ==0){
                    (0 until EhApplication.getInstance().resources.getStringArray(R.array.AdvanceSearchOptions).size).forEach {
                        if (it < 2)
                            temp.add(true)
                        else
                            temp.add(false)
                    }
                }
                if(value in (0 until EhApplication.getInstance().resources.getStringArray(R.array.AdvanceSearchOptions).size)){
                    temp[value] = !temp[value]
                }
            }
        }
        /**
         * 选中ShowSearchTable的某个Category
         */
        class ChangeCategorySelected(val value: Int):SearchAction(){
            override suspend fun invoke(_viewState: MutableStateFlow<SearchState>) {
                val temp =_viewState.value.categorySelected
                if (temp.size ==0){
                    repeat(10){
                        temp.add(true)
                    }
                }
                if(value in (0..temp.size)){
                    temp[value] = !temp[value]
                }
            }
        }
        /**
         * 全选和全取消Category
         */
        class ChangeAllCategorySelected(val selected: Boolean):SearchAction(){
            override suspend fun invoke(_viewState: MutableStateFlow<SearchState>) {
                (0 until _viewState.value.categorySelected.size).forEach {
                    _viewState.value.categorySelected[it] = selected
                }
            }
        }
        /**
         * 更新搜索建议
         */
        object UpdateSuggestions: SearchAction(){
            override suspend fun invoke(_viewState: MutableStateFlow<SearchState>) {
                val suggestions: MutableList<Suggestion> = ArrayList()
                val history = _viewState.value.mSearchHistory
                val text: String = _viewState.value.mText
                if(history.size == 0){
                    val keywords = _viewState.value.mSearchDatabase.getSuggestions(text, 128)
                    for (keyword in keywords) {
                        history.add(Suggestion(mKeyword = keyword, type = KeywordSuggestion))
                    }
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
        /**
         * 单机搜索建议
         */
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
                SetSearchText(temp)(_viewState)
            }
        }

        class OnLongClick(private val suggestion: Suggestion): SearchAction(){
            override suspend fun invoke(_viewState: MutableStateFlow<SearchState>) {
                val temp = when (suggestion.type) {
                    KeywordSuggestion ->  suggestion.mKeyword
                    else -> "error"
                }
                SetSearchText(temp)(_viewState)
            }
        }

    }

    fun sendEvent(event:SearchAction){
        _container.sendEvent(event)
    }


    private val _container by containers<SearchState,SearchAction>(SearchState(mSearchDatabase =mSearchDatabase,mEhTagDatabase= mEhTagDatabase))
    val container: Container<SearchState,SearchAction> = _container


}
