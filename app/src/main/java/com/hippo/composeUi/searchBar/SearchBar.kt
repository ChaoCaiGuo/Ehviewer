package com.hippo.composeUi.searchBar

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Parcelable
import android.text.TextUtils
import android.util.AttributeSet
import android.widget.LinearLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.IntDef
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.hippo.composeUi.composeExt.addComposeView
import com.hippo.composeUi.composeExt.firstBaselineToTop
import com.hippo.composeUi.theme.SystemBarsColor
import com.hippo.composeUi.theme.TextColor
import com.hippo.ehviewer.R
import com.hippo.ehviewer.client.EhTagDatabase
import com.hippo.ehviewer.widget.SearchDatabase
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class SearchBar @JvmOverloads constructor(
    mContext: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(mContext, attrs, defStyleAttr) {

    @Inject
    lateinit var mSearchDatabase: SearchDatabase
    private var mAllowEmptySearch = true
    private var mSuggestionProvider: SuggestionProvider? = null
    private var mHelper: Helper? = null
    private var mSuggestionList:MutableList<Suggestion> by mutableStateOf(ArrayList())
    private var drawerLockMode: ((Boolean) -> Unit)? = null

    private var mTitle by mutableStateOf("")
    private var mHilt by mutableStateOf("")
    private var mText by mutableStateOf("")
    private var mMenuButton by mutableStateOf<Drawable?>(null)
    private var mActionButton by mutableStateOf<Drawable?>(null)

    init {
        addComposeView { ComposeSearchBar() }
    }

    @OptIn(
        ExperimentalMaterial3Api::class,
        ExperimentalLayoutApi::class,
        ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class
    )
    @Composable
    fun ComposeSearchBar() {
        val haptic = LocalHapticFeedback.current
        val focusedManager = LocalFocusManager.current
        val isImeShow = WindowInsets.isImeVisible
        val imeManager = LocalSoftwareKeyboardController.current


        val focusRequester = remember {
            FocusRequester()
        }
        var init by remember {
            mutableStateOf(false)
        }

        LaunchedEffect(isImeShow) {
            if (init)
                drawerLockMode?.invoke(isImeShow)
            init = true
            if (!isImeShow) {
                focusedManager.clearFocus()
            } else {
                updateSuggestions()
            }
        }

        BackHandler(isImeShow) {
            if (isImeShow) {
                imeManager?.hide()
            } else {
                mHelper?.onSearchEditTextBackPressed()
            }
        }

        Column(Modifier.fillMaxSize()) {
            OutlinedTextField(
                value = mText,
                onValueChange = {
                    setSearchText(it)
                    updateSuggestions()
                },
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .padding(horizontal = 8.dp)
                    .height(56.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(25),
                singleLine = true,
                placeholder = {
                    Text(
                        if (isImeShow)
                            mHilt
                        else
                            mTitle,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.W400,
                        color = Color.Gray
                    )
                },
                leadingIcon = {

                    if (mMenuButton != null) {
                        IconButton(onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            if (isImeShow) {
                                imeManager?.hide()
                            }
                            mHelper?.onClickLeftIcon(isImeShow)

                        }) {
                            AsyncImage(
                                model = mMenuButton,
                                contentDescription = null
                            )
                        }
                    }

                },
                trailingIcon = {

                    if (mActionButton != null) {
                        IconButton(onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            if (isImeShow && mText.isEmpty()) {
                                imeManager?.hide()
                            }
                            mHelper?.onClickRightIcon(isImeShow)

                        }) {
                            AsyncImage(
                                model = mActionButton,
                                contentDescription = null
                            )
                        }
                    }

                },
                textStyle = TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.W300,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    applySearch()
                }),
                colors = TextFieldDefaults.outlinedTextFieldColors(containerColor = MaterialTheme.colorScheme.surface)
            )
            if (isImeShow ) {
                LazyColumn(
                    contentPadding = PaddingValues(5.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(bottomEnd = 20.dp, bottomStart = 20.dp))
                        .background(MaterialTheme.colorScheme.SystemBarsColor),
                ) {
                    items(
                        mSuggestionList,
                        key = { item ->
                            item.getText(Keyword)
                        }
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(horizontal = 15.dp)
                                .height(45.dp)
                                .combinedClickable(onLongClick = {
                                    it.onLongClick()
                                }) {
                                    it.onClick()
                                }
                        ) {
                            if (it.getText(Hint) != "") {
                                Text(
                                    text = it.getText(Keyword),
                                    fontSize = 20.sp,
                                    modifier = Modifier.firstBaselineToTop(20.dp),
                                    letterSpacing = 0.sp,
                                    color = MaterialTheme.colorScheme.TextColor
                                )
                                Text(
                                    text = it.getText(Hint),
                                    fontSize = 16.sp,
                                    modifier = Modifier.firstBaselineToTop(14.dp),
                                    color = Color.Gray
                                )
                            }else{
                                Column(Modifier.weight(1f),verticalArrangement = Arrangement.Center){
                                    Text(
                                        text = it.getText(Keyword),
                                        fontSize = 20.sp,
                                        modifier = Modifier.firstBaselineToTop(20.dp),
                                        letterSpacing = 0.sp,
                                        color = MaterialTheme.colorScheme.TextColor
                                    )
                                }

                            }
                            Divider(Modifier.fillMaxWidth())
                        }
                    }
                }
            }


        }
    }

    fun setSearchText(searchText: String) {
        mText = searchText
    }

    fun getSearchText(): String = mText

    fun cursorToEnd() {
        //todo 光标移动到最后
    }

    fun setTitle(title: String?) {
        mTitle = title ?: ""
    }

    fun setEditTextHint(hint: CharSequence?) {
        mHilt = hint?.toString() ?: ""
    }

    fun setDrawable(leftDrawable: Drawable? = null, rightDrawable: Drawable? = null) {
        mMenuButton = leftDrawable
        mActionButton = rightDrawable
    }

// todo 暂时屏蔽 只在GalleryListScene中测试
//    fun setAllowEmptySearch(allowEmptySearch: Boolean) {
//        mAllowEmptySearch = allowEmptySearch
//    }

    fun setSuggestionProvider(suggestionProvider: SuggestionProvider) {
        mSuggestionProvider = suggestionProvider
    }

    fun applySearch() {
        val query: String = mText.trim { it <= ' ' }
        if (!mAllowEmptySearch && TextUtils.isEmpty(query)) {
            return
        }
        // Put it into db
        mSearchDatabase.addQuery(query)
        // Callback
        mHelper!!.onApplySearch(query)
    }


    fun setHelper(helper: Helper) {
        mHelper = helper
    }

    fun setDrawerLockMode(onEvent: ((isImeShow: Boolean) -> Unit)?) {
        drawerLockMode = onEvent
    }

    private fun updateSuggestions() {
        val suggestions: MutableList<Suggestion> = ArrayList()
        val text: String = mText
        if (mSuggestionProvider != null) {
            val providerSuggestions = mSuggestionProvider!!.providerSuggestions(text)
            if (providerSuggestions != null && providerSuggestions.isNotEmpty()) {
                suggestions.addAll(providerSuggestions)
            }
        }
        val keywords = mSearchDatabase.getSuggestions(text, 128)
        for (keyword in keywords) {
            suggestions.add(KeywordSuggestion(keyword))
        }
        val ehTagDatabase = EhTagDatabase.getInstance(context)
        if (!TextUtils.isEmpty(text) && ehTagDatabase != null && !text.endsWith(" ")) {
            val s = text.split(" ".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()
            if (s.isNotEmpty()) {
                val keyword = s[s.size - 1]
                val searchHints = ehTagDatabase.suggest(keyword)
                for (searchHint in searchHints) {
                    suggestions.add(TagSuggestion(searchHint.first, searchHint.second))
                }
            }
        }
        mSuggestionList = suggestions
        //todo 滑动到头
    }

    override fun onSaveInstanceState(): Parcelable {
        return Bundle().apply {
            putParcelable("super", super.onSaveInstanceState())
            putString("string", mText)
        }
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state is Bundle) {
            super.onRestoreInstanceState(state.getParcelable("super"))
            mText = state.getString("string","")
        }
    }

    private fun wrapTagKeyword(keyword: String): String {
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

    abstract inner class SuggestionInner(protected val mKeyword: String) : Suggestion {
        abstract override fun getText(@KeywordText type: Int): String
        override fun onClick() { setSearchText(mKeyword) }
        override fun onLongClick() {}
    }

    inner class TagSuggestion(private val mHint: String, mKeyword: String) : SuggestionInner(mKeyword) {
        override fun getText(@KeywordText type: Int): String {
            return if (type == Hint)
                mHint
            else
                mKeyword
        }

        override fun onClick() {
            val text: String = mText
            var temp: String = wrapTagKeyword(mKeyword) + " "
            if (text.contains(" ")) {
                temp = text.substring(0, text.lastIndexOf(" ")) + " " + temp
            }
            setSearchText(temp)
        }
    }

    inner class KeywordSuggestion(mKeyword: String) : SuggestionInner(mKeyword) {
        override fun getText(@KeywordText type: Int): String {
            return if (type == Hint)
                ""
            else
                mKeyword
        }

        override fun onLongClick(){
            MaterialAlertDialogBuilder(context)
                .setMessage(context.getString(R.string.delete_search_history, mKeyword))
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.delete) { _, _ ->
                    mSearchDatabase.deleteQuery(mKeyword)
                    updateSuggestions()
                }
                .show()
        }
    }


}

const val Hint = 0
const val Keyword = 1

@IntDef(Hint, Keyword)
@Retention(AnnotationRetention.SOURCE)
private annotation class KeywordText

interface Helper {
    //onEditText 表示是否还在编辑中
    fun onClickLeftIcon(onEditText: Boolean)
    fun onClickRightIcon(onEditText: Boolean)
    fun onApplySearch(query: String?)
    fun onSearchEditTextBackPressed()
}


fun interface SuggestionProvider {
    fun providerSuggestions(text: String?): List<Suggestion>?
}


interface Suggestion {
    fun getText(@KeywordText type: Int): String{return ""}
    fun onClick()
    fun onLongClick(){}
}

