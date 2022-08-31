/*
 * Copyright 2016 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hippo.ehviewer.ui.scene

import android.animation.Animator
import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.text.style.ImageSpan
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.IntDef
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.hippo.app.EditTextDialogBuilder
import com.hippo.composeUi.searchLayout.SearchLayout
import com.hippo.drawable.AddDeleteDrawable
import com.hippo.drawable.DrawerArrowDrawable
import com.hippo.easyrecyclerview.EasyRecyclerView
import com.hippo.easyrecyclerview.FastScroller.OnDragHandlerListener
import com.hippo.ehviewer.*
import com.hippo.ehviewer.client.EhClient
import com.hippo.ehviewer.client.EhRequest
import com.hippo.ehviewer.client.EhUrl
import com.hippo.ehviewer.client.EhUtils
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.client.data.ListUrlBuilder
import com.hippo.ehviewer.client.exception.EhException
import com.hippo.ehviewer.client.parser.GalleryDetailUrlParser
import com.hippo.ehviewer.client.parser.GalleryListParser
import com.hippo.ehviewer.client.parser.GalleryPageUrlParser
import com.hippo.ehviewer.dao.DownloadInfo
import com.hippo.ehviewer.dao.QuickSearch
import com.hippo.ehviewer.download.DownloadManager
import com.hippo.ehviewer.download.DownloadManager.DownloadInfoListener
import com.hippo.ehviewer.ui.CommonOperations
import com.hippo.ehviewer.ui.GalleryActivity
import com.hippo.ehviewer.ui.dialog.SelectItemWithIconAdapter
import com.hippo.ehviewer.widget.GalleryInfoContentHelper
import com.hippo.ehviewer.widget.SearchBar
import com.hippo.ehviewer.widget.SearchBar.OnStateChangeListener
import com.hippo.ehviewer.widget.SearchBar.Suggestion
import com.hippo.scene.Announcer
import com.hippo.scene.SceneFragment
import com.hippo.view.BringOutTransition
import com.hippo.view.ViewTransition
import com.hippo.viewModel.SearchViewModel
import com.hippo.widget.ContentLayout
import com.hippo.widget.FabLayout
import com.hippo.widget.FabLayout.OnClickFabListener
import com.hippo.widget.FabLayout.OnExpandListener
import com.hippo.widget.SearchBarMover
import com.hippo.yorozuya.*
import dagger.hilt.android.AndroidEntryPoint
import rikka.core.res.resolveColor
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
@SuppressLint("RtlHardcoded")
class GalleryListScene : BaseScene(), SearchBar.Helper, OnStateChangeListener,
    OnDragHandlerListener, SearchBarMover.Helper, View.OnClickListener, OnClickFabListener,
    OnExpandListener {
    /*---------------
     Whole life cycle
     ---------------*/
    @Inject lateinit var mClient: EhClient
    private var mUrlBuilder: ListUrlBuilder? = null
    private val viewModel by viewModels<SearchViewModel>()
    /*---------------
     View life cycle
     ---------------*/
    private var mContentLayout: ContentLayout? = null
    private var mRecyclerView: EasyRecyclerView? = null
    private var mSearchLayout: SearchLayout? = null
    private var mSearchBar: SearchBar? = null
    private var mSearchFab: View? = null
    private val mSearchFabAnimatorListener: Animator.AnimatorListener =
        object : SimpleAnimatorListener() {
            override fun onAnimationEnd(animation: Animator) {
                if (null != mSearchFab) {
                    mSearchFab!!.visibility = View.INVISIBLE
                }
            }
        }
    private var mFabLayout: FabLayout? = null
    private val mActionFabAnimatorListener: Animator.AnimatorListener =
        object : SimpleAnimatorListener() {
            override fun onAnimationEnd(animation: Animator) {
                if (null != mFabLayout) {
                    (mFabLayout!!.primaryFab as View).visibility = View.INVISIBLE
                }
            }
        }
    private var fabAnimator: ViewPropertyAnimator? = null
    private var mViewTransition: ViewTransition? = null
    private var mAdapter: GalleryListAdapter? = null
    private var mHelper: GalleryListHelper? = null
    private var mLeftDrawable: DrawerArrowDrawable? = null
    private var mRightDrawable: AddDeleteDrawable? = null
    private var mSearchBarMover: SearchBarMover? = null
    private var mActionFabDrawable: AddDeleteDrawable? = null
    private var mQuickSearchList: MutableList<QuickSearch>? = null
    private var mHideActionFabSlop = 0
    private var mShowActionFab = true

    @State
    private var mState = STATE_NORMAL
    private val mOnScrollListener: RecyclerView.OnScrollListener =
        object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {}
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy >= mHideActionFabSlop) {
                    hideActionFab()
                } else if (dy <= -mHideActionFabSlop / 2) {
                    showActionFab()
                }
            }
        }

    // Double click back exit
    private val mPressBackTime: Long = 0
    private var mHasFirstRefresh = false
    private var mNavCheckedId = 0
    private var mDownloadManager: DownloadManager? = null
    private var mDownloadInfoListener: DownloadInfoListener? = null
    private var mFavouriteStatusRouter: FavouriteStatusRouter? = null
    private var mFavouriteStatusRouterListener: FavouriteStatusRouter.Listener? = null
    private var mIsTopList = false
    override fun getNavCheckedItem(): Int {
        return mNavCheckedId
    }

    private fun handleArgs(args: Bundle?) {
        if (null == args || null == mUrlBuilder) {
            return
        }
        val action = args.getString(KEY_ACTION)
        if (ACTION_HOMEPAGE == action) {
            mUrlBuilder!!.reset()
        } else if (ACTION_SUBSCRIPTION == action) {
            mUrlBuilder!!.reset()
            mUrlBuilder!!.mode = ListUrlBuilder.MODE_SUBSCRIPTION
        } else if (ACTION_WHATS_HOT == action) {
            mUrlBuilder!!.reset()
            mUrlBuilder!!.mode = ListUrlBuilder.MODE_WHATS_HOT
        } else if (ACTION_TOP_LIST == action) {
            mUrlBuilder!!.reset()
            mUrlBuilder!!.mode = ListUrlBuilder.MODE_TOPLIST
            mUrlBuilder!!.keyword = "11"
        } else if (ACTION_LIST_URL_BUILDER == action) {
            val builder = args.getParcelable<ListUrlBuilder>(KEY_LIST_URL_BUILDER)
            if (builder != null) {
                mUrlBuilder!!.set(builder)
            }
        }
    }

    override fun onNewArguments(args: Bundle) {
        handleArgs(args)
        onUpdateUrlBuilder()
        if (null != mHelper) {
            mHelper!!.refresh()
        }
        setState(STATE_NORMAL)
        if (null != mSearchBarMover) {
            mSearchBarMover!!.showSearchBar()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val context = context
        AssertUtils.assertNotNull(context)

//        mClient = EhApplication.getEhClient(context!!)
        mDownloadManager = EhApplication.getDownloadManager(context!!)
        mFavouriteStatusRouter = EhApplication.getFavouriteStatusRouter(context)
        mDownloadInfoListener = object : DownloadInfoListener {
            override fun onAdd(info: DownloadInfo, list: List<DownloadInfo>, position: Int) {
                if (mAdapter != null) {
                    mAdapter!!.notifyDataSetChanged()
                }
            }

            override fun onUpdate(info: DownloadInfo, list: List<DownloadInfo>) {}
            override fun onUpdateAll() {}
            override fun onReload() {
                if (mAdapter != null) {
                    mAdapter!!.notifyDataSetChanged()
                }
            }

            override fun onChange() {
                if (mAdapter != null) {
                    mAdapter!!.notifyDataSetChanged()
                }
            }

            override fun onRenameLabel(from: String, to: String) {}
            override fun onRemove(info: DownloadInfo, list: List<DownloadInfo>, position: Int) {
                if (mAdapter != null) {
                    mAdapter!!.notifyDataSetChanged()
                }
            }

            override fun onUpdateLabels() {}
        }
        mDownloadManager!!.addDownloadInfoListener(mDownloadInfoListener)
        mFavouriteStatusRouterListener = FavouriteStatusRouter.Listener { gid: Long, slot: Int ->
            if (mAdapter != null) {
                mAdapter!!.notifyDataSetChanged()
            }
        }
        mFavouriteStatusRouter!!.addListener(mFavouriteStatusRouterListener)
        if (savedInstanceState == null) {
            onInit()
        } else {
            onRestore(savedInstanceState)
        }
    }

    fun onInit() {
        mUrlBuilder = ListUrlBuilder()
        handleArgs(arguments)
    }

    private fun onRestore(savedInstanceState: Bundle) {
        mHasFirstRefresh = savedInstanceState.getBoolean(KEY_HAS_FIRST_REFRESH)
        mUrlBuilder = savedInstanceState.getParcelable(KEY_LIST_URL_BUILDER)
        mState = savedInstanceState.getInt(KEY_STATE)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val hasFirstRefresh: Boolean = if (mHelper != null && 1 == mHelper!!.shownViewIndex) {
            false
        } else {
            mHasFirstRefresh
        }
        outState.putBoolean(KEY_HAS_FIRST_REFRESH, hasFirstRefresh)
        outState.putParcelable(KEY_LIST_URL_BUILDER, mUrlBuilder)
        outState.putInt(KEY_STATE, mState)
    }

    override fun onDestroy() {
        super.onDestroy()
//        mClient = null
        mUrlBuilder = null
        mDownloadManager!!.removeDownloadInfoListener(mDownloadInfoListener)
        mFavouriteStatusRouter!!.removeListener(mFavouriteStatusRouterListener)
    }

    private fun setSearchBarHint(searchBar: SearchBar?) {
        searchBar!!.setEditTextHint(getString(if (EhUrl.SITE_EX == Settings.getGallerySite()) R.string.gallery_list_search_bar_hint_exhentai else R.string.gallery_list_search_bar_hint_e_hentai))
    }

    private fun setSearchBarSuggestionProvider(searchBar: SearchBar?) {
        searchBar!!.setSuggestionProvider { text: String? ->
            val result1 = GalleryDetailUrlParser.parse(text, false)
            if (result1 != null) {
                return@setSuggestionProvider listOf<Suggestion>(
                    GalleryDetailUrlSuggestion(
                        result1.gid,
                        result1.token
                    )
                )
            }
            val result2 = GalleryPageUrlParser.parse(text, false)
            if (result2 != null) {
                return@setSuggestionProvider listOf<Suggestion>(
                    GalleryPageUrlSuggestion(
                        result2.gid,
                        result2.pToken,
                        result2.page
                    )
                )
            }
            null
        }
    }

    private fun wrapTagKeyword(keyword: String): String {
        var keyword = keyword
        keyword = keyword.trim { it <= ' ' }
        val index1 = keyword.indexOf(':')
        if (index1 == -1 || index1 >= keyword.length - 1) {
            // Can't find :, or : is the last char
            return keyword
        }
        if (keyword[index1 + 1] == '"') {
            // The char after : is ", the word must be quoted
            return keyword
        }
        val index2 = keyword.indexOf(' ')
        return if (index2 <= index1) {
            // Can't find space, or space is before :
            keyword
        } else keyword.substring(
            0,
            index1 + 1
        ) + "\"" + keyword.substring(index1 + 1) + "$\""
    }

    // Update search bar title, drawer checked item
    private fun onUpdateUrlBuilder() {
        val builder = mUrlBuilder
        val resources = resourcesOrNull
        if (resources == null || builder == null || mSearchLayout == null) {
            return
        }
        var keyword = builder.keyword
        val category = builder.category
        val isTopList = builder.mode == ListUrlBuilder.MODE_TOPLIST
        if (isTopList != mIsTopList) {
            mIsTopList = isTopList
            recreateDrawerView()
            mFabLayout!!.getSecondaryFabAt(0)
                .setImageResource(if (isTopList) R.drawable.ic_baseline_format_list_numbered_24 else R.drawable.v_magnify_x24)
        }

        // Update normal search mode
        //Todo 这下面的是改普通搜索的类别 比如订阅搜索等 这功能应该分离到具体的订阅等页面
//        mSearchLayout.setNormalSearchMode(builder.getMode() == ListUrlBuilder.MODE_SUBSCRIPTION
//                ? R.id.search_subscription_search
//                : R.id.search_normal_search);

        // Update search edit text
        if (!TextUtils.isEmpty(keyword) && null != mSearchBar && !mIsTopList) {
            if (builder.mode == ListUrlBuilder.MODE_TAG) {
                keyword = wrapTagKeyword(keyword)
            }
            mSearchBar!!.text = keyword
            mSearchBar!!.cursorToEnd()
        }

        // Update title
        var title = getSuitableTitleForUrlBuilder(resources, builder, true)
        if (null == title) {
            title = resources.getString(R.string.search)
        }
        if (null != mSearchBar) {
            mSearchBar!!.setTitle(title)
        }

        // Update nav checked item
        val checkedItemId: Int
        checkedItemId =
            if (ListUrlBuilder.MODE_NORMAL == builder.mode && EhUtils.NONE == category &&
                TextUtils.isEmpty(keyword)
            ) {
                R.id.nav_homepage
            } else if (ListUrlBuilder.MODE_SUBSCRIPTION == builder.mode) {
                R.id.nav_subscription
            } else if (ListUrlBuilder.MODE_WHATS_HOT == builder.mode) {
                R.id.nav_whats_hot
            } else if (ListUrlBuilder.MODE_TOPLIST == builder.mode) {
                R.id.nav_toplist
            } else {
                0
            }
        navCheckedItem = checkedItemId
        mNavCheckedId = checkedItemId
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.scene_gallery_list, container, false)
        val context = context
        AssertUtils.assertNotNull(context)
        val resources = context!!.resources
        mHideActionFabSlop = ViewConfiguration.get(context).scaledTouchSlop
        mShowActionFab = true
        val mainLayout = ViewUtils.`$$`(view, R.id.main_layout)
        mContentLayout = ViewUtils.`$$`(mainLayout, R.id.content_layout) as ContentLayout
        mRecyclerView = mContentLayout!!.recyclerView
        val fastScroller = mContentLayout!!.fastScroller
        mSearchLayout = ViewUtils.`$$`(mainLayout, R.id.search_layout) as SearchLayout
        mSearchLayout!!.viewModel = viewModel
        mSearchBar = ViewUtils.`$$`(mainLayout, R.id.search_bar) as SearchBar
        mFabLayout = ViewUtils.`$$`(mainLayout, R.id.fab_layout) as FabLayout
        mSearchFab = ViewUtils.`$$`(mainLayout, R.id.search_fab)
        ViewCompat.setWindowInsetsAnimationCallback(
            view, WindowInsetsAnimationHelper(
                WindowInsetsAnimationCompat.Callback.DISPATCH_MODE_STOP,
                mFabLayout,
                mSearchFab!!.parent as View
            )
        )
        val paddingTopSB = resources.getDimensionPixelOffset(R.dimen.gallery_padding_top_search_bar)
        mViewTransition = BringOutTransition(mContentLayout, mSearchLayout)
        mHelper = GalleryListHelper()
        mContentLayout!!.setHelper(mHelper)
        mContentLayout!!.fastScroller.setOnDragHandlerListener(this)
        mContentLayout!!.setFitPaddingTop(paddingTopSB)
        mAdapter = GalleryListAdapter(
            inflater, resources,
            mRecyclerView!!, Settings.getListMode()
        )
        mRecyclerView!!.clipToPadding = false
        mRecyclerView!!.clipChildren = false
        mRecyclerView!!.addOnScrollListener(mOnScrollListener)
        fastScroller.setPadding(
            fastScroller.paddingLeft, fastScroller.paddingTop + paddingTopSB,
            fastScroller.paddingRight, fastScroller.paddingBottom
        )
        mLeftDrawable =
            DrawerArrowDrawable(context, theme.resolveColor(android.R.attr.colorControlNormal))
        mRightDrawable =
            AddDeleteDrawable(context, theme.resolveColor(android.R.attr.colorControlNormal))
        mSearchBar!!.setLeftDrawable(mLeftDrawable)
        mSearchBar!!.setRightDrawable(mRightDrawable)
        mSearchBar!!.setHelper(this)
        mSearchBar!!.setOnStateChangeListener(this)
        setSearchBarHint(mSearchBar)
        setSearchBarSuggestionProvider(mSearchBar)

        mSearchLayout!!.setPadding(
            mSearchLayout!!.paddingLeft, mSearchLayout!!.paddingTop + paddingTopSB,
            mSearchLayout!!.paddingRight, mSearchLayout!!.paddingBottom
        )
        mFabLayout!!.setAutoCancel(true)
        mFabLayout!!.isExpanded = false
        mFabLayout!!.setHidePrimaryFab(false)
        mFabLayout!!.setOnClickFabListener(this)
        mFabLayout!!.setOnExpandListener(this)
        addAboveSnackView(mFabLayout)
        val colorID = theme.resolveColor(com.google.android.material.R.attr.colorOnSurface)
        mActionFabDrawable = AddDeleteDrawable(context, colorID)
        mFabLayout!!.primaryFab.setImageDrawable(mActionFabDrawable)
        mSearchFab!!.setOnClickListener(this)
        mSearchBarMover = SearchBarMover(this, mSearchBar, mRecyclerView)

        // Update list url builder
        onUpdateUrlBuilder()

        // Restore state
        val newState = mState
        mState = STATE_NORMAL
        setState(newState, false)

        // Only refresh for the first time
        if (!mHasFirstRefresh) {
            mHasFirstRefresh = true
            mHelper!!.firstRefresh()
        }
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (null != mSearchBarMover) {
            mSearchBarMover!!.cancelAnimation()
            mSearchBarMover = null
        }
        if (null != mHelper) {
            mHelper!!.destroy()
            if (1 == mHelper!!.shownViewIndex) {
                mHasFirstRefresh = false
            }
        }
        if (null != mRecyclerView) {
            mRecyclerView!!.stopScroll()
            mRecyclerView = null
        }
        if (null != mFabLayout) {
            removeAboveSnackView(mFabLayout)
            mFabLayout = null
        }
        mAdapter = null
        mSearchLayout = null
        mSearchBar = null
        mSearchFab = null
        mViewTransition = null
        mLeftDrawable = null
        mRightDrawable = null
        mActionFabDrawable = null
    }

    private fun showQuickSearchTipDialog() {
        val context = context ?: return
        val builder = MaterialAlertDialogBuilder(context)
        builder.setMessage(R.string.add_quick_search_tip)
        builder.setTitle(R.string.readme)
        builder.show()
    }

    private fun showAddQuickSearchDialog(
        adapter: QsDrawerAdapter,
        recyclerView: EasyRecyclerView, tip: TextView
    ) {
        val context = context
        val urlBuilder = mUrlBuilder
        if (null == context || null == urlBuilder) {
            return
        }

        // Can't add image search as quick search
        if (ListUrlBuilder.MODE_IMAGE_SEARCH == urlBuilder.mode) {
            showTip(R.string.image_search_not_quick_search, LENGTH_LONG)
            return
        }

        // Check duplicate
        for (q in mQuickSearchList!!) {
            if (urlBuilder.equalsQuickSearch(q)) {
                showTip(getString(R.string.duplicate_quick_search, q.name), LENGTH_LONG)
                return
            }
        }
        val builder = EditTextDialogBuilder(
            context,
            getSuitableTitleForUrlBuilder(context.resources, urlBuilder, false),
            getString(R.string.quick_search)
        )
        builder.setTitle(R.string.add_quick_search_dialog_title)
        builder.setPositiveButton(android.R.string.ok, null)
        val dialog = builder.show()
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener { v: View? ->
            val text = builder.text.trim { it <= ' ' }

            // Check name empty
            if (TextUtils.isEmpty(text)) {
                builder.setError(getString(R.string.name_is_empty))
                return@setOnClickListener
            }

            // Check name duplicate
            for (q in mQuickSearchList!!) {
                if (text == q.name) {
                    builder.setError(getString(R.string.duplicate_name))
                    return@setOnClickListener
                }
            }
            builder.setError(null)
            dialog.dismiss()
            val quickSearch = urlBuilder.toQuickSearch()
            quickSearch.name = text
            EhDB.insertQuickSearch(quickSearch)
            mQuickSearchList!!.add(quickSearch)
            adapter.notifyItemInserted(mQuickSearchList!!.size - 1)
            if (0 == mQuickSearchList!!.size) {
                tip.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
            } else {
                tip.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
            }
        }
    }

    override fun onCreateDrawerView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.drawer_list_rv, container, false)
        val toolbar = ViewUtils.`$$`(view, R.id.toolbar) as Toolbar
        val tip = ViewUtils.`$$`(view, R.id.tip) as TextView
        val context = context
        AssertUtils.assertNotNull(context)

        val recyclerView = view.findViewById<EasyRecyclerView>(R.id.recycler_view_drawer)
        recyclerView.layoutManager = LinearLayoutManager(context)
        val qsDrawerAdapter = QsDrawerAdapter(inflater)
        qsDrawerAdapter.setHasStableIds(true)
        recyclerView.adapter = qsDrawerAdapter
        val itemTouchHelper = ItemTouchHelper(GalleryListQSItemTouchHelperCallback(qsDrawerAdapter))
        itemTouchHelper.attachToRecyclerView(recyclerView)
        mQuickSearchList = EhDB.getAllQuickSearch()
        tip.setText(R.string.quick_search_tip)
        if (mIsTopList) {
            toolbar.setTitle(R.string.toplist)
        } else {
            toolbar.setTitle(R.string.quick_search)
        }
        if (!mIsTopList) toolbar.inflateMenu(R.menu.drawer_gallery_list)
        toolbar.setOnMenuItemClickListener { item: MenuItem ->
            val id = item.itemId
            if (id == R.id.action_add) {
                showAddQuickSearchDialog(qsDrawerAdapter, recyclerView, tip)
            } else if (id == R.id.action_help) {
                showQuickSearchTipDialog()
            }
            true
        }
        if (0 == EhDB.getAllQuickSearch().size && !mIsTopList) {
            tip.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            tip.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
        return view
    }

    override fun onBackPressed() {
        if (null != mFabLayout && mFabLayout!!.isExpanded) {
            mFabLayout!!.isExpanded = false
            return
        }
        when (mState) {
            STATE_NORMAL -> if (stackIndex == 0) requireActivity().moveTaskToBack(false) else finish()
            STATE_SIMPLE_SEARCH, STATE_SEARCH -> setState(STATE_NORMAL)
            STATE_SEARCH_SHOW_LIST -> setState(STATE_SEARCH)
        }
    }

    fun onItemClick(position: Int): Boolean {
        if (null == mHelper || null == mRecyclerView) {
            return false
        }
        val gi = mHelper!!.getDataAtEx(position) ?: return true
        val args = Bundle()
        args.putString(GalleryDetailScene.KEY_ACTION, GalleryDetailScene.ACTION_GALLERY_INFO)
        args.putParcelable(GalleryDetailScene.KEY_GALLERY_INFO, gi)
        val announcer = Announcer(GalleryDetailScene::class.java).setArgs(args)
        startScene(announcer)
        return true
    }

    override fun onClick(v: View) {
        if (STATE_NORMAL != mState && null != mSearchBar) {
            mSearchBar!!.applySearch()
            hideSoftInput()
        }
    }

    override fun onClickPrimaryFab(view: FabLayout, fab: FloatingActionButton) {
        if (STATE_NORMAL == mState) {
            view.toggle()
        }
    }

    private fun showGoToDialog() {
        val context = context
        if (null == context || null == mHelper) {
            return
        }
        val page = mHelper!!.pageForTop
        val pages = mHelper!!.pages
        val hint = getString(R.string.go_to_hint, page + 1, pages)
        val builder = EditTextDialogBuilder(context, null, hint)
        builder.editText.inputType =
            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        val dialog = builder.setTitle(R.string.go_to)
            .setPositiveButton(android.R.string.ok, null)
            .show()
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener { v: View? ->
            if (null == mHelper) {
                dialog.dismiss()
                return@setOnClickListener
            }
            val text = builder.text.trim { it <= ' ' }
            val goTo: Int
            goTo = try {
                text.toInt() - 1
            } catch (e: NumberFormatException) {
                builder.setError(getString(R.string.error_invalid_number))
                return@setOnClickListener
            }
            if (goTo < 0 || goTo >= pages) {
                builder.setError(getString(R.string.error_out_of_range))
                return@setOnClickListener
            }
            builder.setError(null)
            mHelper!!.goTo(goTo)
            dialog.dismiss()
        }
    }

    override fun onClickSecondaryFab(view: FabLayout, fab: FloatingActionButton, position: Int) {
        if (null == mHelper) {
            return
        }
        when (position) {
            0 -> openDrawer(Gravity.RIGHT)
            1 -> if (mHelper!!.canGoTo()) {
                showGoToDialog()
            }
            2 -> mHelper!!.refresh()
        }
        view.isExpanded = false
    }

    override fun onExpand(expanded: Boolean) {
        if (null == mActionFabDrawable) {
            return
        }
        if (expanded) {
            setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, Gravity.LEFT)
            setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, Gravity.RIGHT)
            mActionFabDrawable!!.setDelete(ANIMATE_TIME)
        } else {
            setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, Gravity.LEFT)
            setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, Gravity.RIGHT)
            mActionFabDrawable!!.setAdd(ANIMATE_TIME)
        }
    }

    fun onItemLongClick(position: Int): Boolean {
        val context = context
        val activity = mainActivity
        if (null == context || null == activity || null == mHelper) {
            return false
        }
        val gi = mHelper!!.getDataAtEx(position) ?: return true
        val downloaded = mDownloadManager!!.getDownloadState(gi.gid) != DownloadInfo.STATE_INVALID
        val favourited = gi.favoriteSlot != -2
        val items = if (downloaded) arrayOf<CharSequence>(
            context.getString(R.string.read),
            context.getString(R.string.delete_downloads),
            context.getString(if (favourited) R.string.remove_from_favourites else R.string.add_to_favourites),
            context.getString(R.string.download_move_dialog_title)
        ) else arrayOf<CharSequence>(
            context.getString(R.string.read),
            context.getString(R.string.download),
            context.getString(if (favourited) R.string.remove_from_favourites else R.string.add_to_favourites)
        )
        val icons = if (downloaded) intArrayOf(
            R.drawable.v_book_open_x24,
            R.drawable.v_delete_x24,
            if (favourited) R.drawable.v_heart_broken_x24 else R.drawable.v_heart_x24,
            R.drawable.v_folder_move_x24
        ) else intArrayOf(
            R.drawable.v_book_open_x24,
            R.drawable.v_download_x24,
            if (favourited) R.drawable.v_heart_broken_x24 else R.drawable.v_heart_x24
        )
        MaterialAlertDialogBuilder(context)
            .setTitle(EhUtils.getSuitableTitle(gi))
            .setAdapter(
                SelectItemWithIconAdapter(
                    context,
                    items,
                    icons
                )
            ) { dialog: DialogInterface?, which: Int ->
                when (which) {
                    0 -> {
                        val intent = Intent(activity, GalleryActivity::class.java)
                        intent.action = GalleryActivity.ACTION_EH
                        intent.putExtra(GalleryActivity.KEY_GALLERY_INFO, gi)
                        startActivity(intent)
                    }
                    1 -> if (downloaded) {
                        MaterialAlertDialogBuilder(context)
                            .setTitle(R.string.download_remove_dialog_title)
                            .setMessage(
                                getString(
                                    R.string.download_remove_dialog_message,
                                    gi.title
                                )
                            )
                            .setPositiveButton(android.R.string.ok) { dialog1: DialogInterface?, which1: Int ->
                                mDownloadManager!!.deleteDownload(
                                    gi.gid
                                )
                            }
                            .show()
                    } else {
                        CommonOperations.startDownload(activity, gi, false)
                    }
                    2 -> if (favourited) {
                        CommonOperations.removeFromFavorites(
                            activity,
                            gi,
                            RemoveFromFavoriteListener(context, activity.getStageId(), tag)
                        )
                    } else {
                        CommonOperations.addToFavorites(
                            activity,
                            gi,
                            AddToFavoriteListener(context, activity.getStageId(), tag)
                        )
                    }
                    3 -> {
                        val labelRawList = EhApplication.getDownloadManager(context).labelList
                        val labelList: MutableList<String> = ArrayList(labelRawList.size + 1)
                        labelList.add(getString(R.string.default_download_label_name))
                        var i = 0
                        val n = labelRawList.size
                        while (i < n) {
                            labelList.add(labelRawList[i].label)
                            i++
                        }
                        val labels = labelList.toTypedArray()
                        val helper= MoveDialogHelper(labels, gi)
                        MaterialAlertDialogBuilder(context)
                            .setTitle(R.string.download_move_dialog_title)
                            .setItems(labels, helper)
                            .show()
                    }
                }
            }.show()
        return true
    }

    private fun showActionFab() {
        if (null != mFabLayout && STATE_NORMAL == mState && !mShowActionFab) {
            mShowActionFab = true
            val fab: View = mFabLayout!!.primaryFab
            if (fabAnimator != null) {
                fabAnimator!!.cancel()
            }
            fab.visibility = View.VISIBLE
            fab.rotation = -45.0f
            fabAnimator = fab.animate().scaleX(1.0f).scaleY(1.0f).rotation(0.0f).setListener(null)
                .setDuration(ANIMATE_TIME).setStartDelay(0L)
                .setInterpolator(AnimationUtils.FAST_SLOW_INTERPOLATOR)
            fabAnimator!!.start()
        }
    }

    private fun hideActionFab() {
        if (null != mFabLayout && STATE_NORMAL == mState && mShowActionFab) {
            mShowActionFab = false
            val fab: View = mFabLayout!!.primaryFab
            if (fabAnimator != null) {
                fabAnimator!!.cancel()
            }
            fabAnimator =
                fab.animate().scaleX(0.0f).scaleY(0.0f).setListener(mActionFabAnimatorListener)
                    .setDuration(ANIMATE_TIME).setStartDelay(0L)
                    .setInterpolator(AnimationUtils.SLOW_FAST_INTERPOLATOR)
            fabAnimator!!.start()
        }
    }

    private fun selectSearchFab(animation: Boolean) {
        if (null == mFabLayout || null == mSearchFab) {
            return
        }
        mShowActionFab = false
        if (animation) {
            val fab: View = mFabLayout!!.primaryFab
            val delay: Long
            if (View.INVISIBLE == fab.visibility) {
                delay = 0L
            } else {
                delay = ANIMATE_TIME
                mFabLayout!!.setExpanded(false, true)
                fab.animate().scaleX(0.0f).scaleY(0.0f).setListener(mActionFabAnimatorListener)
                    .setDuration(ANIMATE_TIME).setStartDelay(0L)
                    .setInterpolator(AnimationUtils.SLOW_FAST_INTERPOLATOR).start()
            }
            mSearchFab!!.visibility = View.VISIBLE
            mSearchFab!!.rotation = -45.0f
            mSearchFab!!.animate().scaleX(1.0f).scaleY(1.0f).rotation(0.0f).setListener(null)
                .setDuration(ANIMATE_TIME).setStartDelay(delay)
                .setInterpolator(AnimationUtils.FAST_SLOW_INTERPOLATOR).start()
        } else {
            mFabLayout!!.setExpanded(false, false)
            val fab: View = mFabLayout!!.primaryFab
            fab.visibility = View.INVISIBLE
            fab.scaleX = 0.0f
            fab.scaleY = 0.0f
            mSearchFab!!.visibility = View.VISIBLE
            mSearchFab!!.scaleX = 1.0f
            mSearchFab!!.scaleY = 1.0f
        }
    }

    private fun selectActionFab(animation: Boolean) {
        if (null == mFabLayout || null == mSearchFab) {
            return
        }
        mShowActionFab = true
        if (animation) {
            val delay: Long
            if (View.INVISIBLE == mSearchFab!!.visibility) {
                delay = 0L
            } else {
                delay = ANIMATE_TIME
                mSearchFab!!.animate().scaleX(0.0f).scaleY(0.0f)
                    .setListener(mSearchFabAnimatorListener)
                    .setDuration(ANIMATE_TIME).setStartDelay(0L)
                    .setInterpolator(AnimationUtils.SLOW_FAST_INTERPOLATOR).start()
            }
            val fab: View = mFabLayout!!.primaryFab
            fab.visibility = View.VISIBLE
            fab.rotation = -45.0f
            fab.animate().scaleX(1.0f).scaleY(1.0f).rotation(0.0f).setListener(null)
                .setDuration(ANIMATE_TIME).setStartDelay(delay)
                .setInterpolator(AnimationUtils.FAST_SLOW_INTERPOLATOR).start()
        } else {
            mFabLayout!!.setExpanded(false, false)
            val fab: View = mFabLayout!!.primaryFab
            fab.visibility = View.VISIBLE
            fab.scaleX = 1.0f
            fab.scaleY = 1.0f
            mSearchFab!!.visibility = View.INVISIBLE
            mSearchFab!!.scaleX = 0.0f
            mSearchFab!!.scaleY = 0.0f
        }
    }

    private fun setState(@State state: Int) {
        setState(state, true)
    }

    private fun setState(@State state: Int, animation: Boolean) {
        if (null == mSearchBar || null == mSearchBarMover || null == mViewTransition || null == mSearchLayout) {
            return
        }
        if (mState != state) {
            val oldState = mState
            mState = state
            when (oldState) {
                STATE_NORMAL -> if (state == STATE_SIMPLE_SEARCH) {
                    mSearchBar!!.setState(SearchBar.STATE_SEARCH_LIST, animation)
                    mSearchBarMover!!.returnSearchBarPosition()
                    selectSearchFab(animation)
                } else if (state == STATE_SEARCH) {
                    mViewTransition!!.showView(1, animation)
                    //                        mSearchLayout.scrollSearchContainerToTop();
                    mSearchBar!!.setState(SearchBar.STATE_SEARCH, animation)
                    mSearchBarMover!!.returnSearchBarPosition()
                    selectSearchFab(animation)
                } else if (state == STATE_SEARCH_SHOW_LIST) {
                    mViewTransition!!.showView(1, animation)
                    //                        mSearchLayout.scrollSearchContainerToTop();
                    mSearchBar!!.setState(SearchBar.STATE_SEARCH_LIST, animation)
                    mSearchBarMover!!.returnSearchBarPosition()
                    selectSearchFab(animation)
                }
                STATE_SIMPLE_SEARCH -> if (state == STATE_NORMAL) {
                    mSearchBar!!.setState(SearchBar.STATE_NORMAL, animation)
                    mSearchBarMover!!.returnSearchBarPosition()
                    selectActionFab(animation)
                } else if (state == STATE_SEARCH) {
                    mViewTransition!!.showView(1, animation)
                    //                        mSearchLayout.scrollSearchContainerToTop();
                    mSearchBar!!.setState(SearchBar.STATE_SEARCH, animation)
                    mSearchBarMover!!.returnSearchBarPosition()
                } else if (state == STATE_SEARCH_SHOW_LIST) {
                    mViewTransition!!.showView(1, animation)
                    //                        mSearchLayout.scrollSearchContainerToTop();
                    mSearchBar!!.setState(SearchBar.STATE_SEARCH_LIST, animation)
                    mSearchBarMover!!.returnSearchBarPosition()
                }
                STATE_SEARCH -> if (state == STATE_NORMAL) {
                    mViewTransition!!.showView(0, animation)
                    mSearchBar!!.setState(SearchBar.STATE_NORMAL, animation)
                    mSearchBarMover!!.returnSearchBarPosition()
                    selectActionFab(animation)
                } else if (state == STATE_SIMPLE_SEARCH) {
                    mViewTransition!!.showView(0, animation)
                    mSearchBar!!.setState(SearchBar.STATE_SEARCH_LIST, animation)
                    mSearchBarMover!!.returnSearchBarPosition()
                } else if (state == STATE_SEARCH_SHOW_LIST) {
                    mSearchBar!!.setState(SearchBar.STATE_SEARCH_LIST, animation)
                    mSearchBarMover!!.returnSearchBarPosition()
                }
                STATE_SEARCH_SHOW_LIST -> if (state == STATE_NORMAL) {
                    mViewTransition!!.showView(0, animation)
                    mSearchBar!!.setState(SearchBar.STATE_NORMAL, animation)
                    mSearchBarMover!!.returnSearchBarPosition()
                    selectActionFab(animation)
                } else if (state == STATE_SIMPLE_SEARCH) {
                    mViewTransition!!.showView(0, animation)
                    mSearchBar!!.setState(SearchBar.STATE_SEARCH_LIST, animation)
                    mSearchBarMover!!.returnSearchBarPosition()
                } else if (state == STATE_SEARCH) {
                    mSearchBar!!.setState(SearchBar.STATE_SEARCH, animation)
                    mSearchBarMover!!.returnSearchBarPosition()
                }
            }
        }
    }

    override fun onClickTitle() {
        if (mState == STATE_NORMAL) {
            setState(STATE_SIMPLE_SEARCH)
        }
    }

    override fun onClickLeftIcon() {
        if (null == mSearchBar) {
            return
        }
        if (mSearchBar!!.state == SearchBar.STATE_NORMAL) {
            toggleDrawer(Gravity.LEFT)
        } else {
            setState(STATE_NORMAL)
        }
    }

    override fun onClickRightIcon() {
        if (null == mSearchBar) {
            return
        }
        if (mSearchBar!!.state == SearchBar.STATE_NORMAL) {
            setState(STATE_SEARCH)
        } else {
            if (mSearchBar!!.editText.length() == 0) {
                setState(STATE_NORMAL)
            } else {
                // Clear
                mSearchBar!!.text = ""
            }
        }
    }

    override fun onSearchEditTextClick() {
        if (mState == STATE_SEARCH) {
            setState(STATE_SEARCH_SHOW_LIST)
        }
    }

    override fun onApplySearch(query: String) {
        if (null == mUrlBuilder || null == mHelper || null == mSearchLayout) {
            return
        }
        if (mState == STATE_SEARCH || mState == STATE_SEARCH_SHOW_LIST) {
            try {
                viewModel.formatListUrlBuilder(mUrlBuilder!!, query)
            } catch (e: EhException) {
                showTip(e.message, LENGTH_LONG)
                return
            }
        } else {
            val oldMode = mUrlBuilder!!.mode
            // If it's MODE_SUBSCRIPTION, keep it
            val newMode =
                if (oldMode == ListUrlBuilder.MODE_SUBSCRIPTION) ListUrlBuilder.MODE_SUBSCRIPTION else ListUrlBuilder.MODE_NORMAL
            mUrlBuilder!!.reset()
            mUrlBuilder!!.mode = newMode
            mUrlBuilder!!.keyword = query
        }
        onUpdateUrlBuilder()
        mHelper!!.refresh()
        setState(STATE_NORMAL)
    }

    override fun onSearchEditTextBackPressed() {
        onBackPressed()
    }

    override fun onReceiveContent(uri: Uri?) {
        if (mSearchLayout == null || uri == null) {
            return
        }
        //Todo 这儿要改成compose版的修改SearchMode
//        mSearchLayout.setSearchMode(SearchLayout.SEARCH_MODE_IMAGE);
        viewModel.setImageUri(uri)
        setState(STATE_SEARCH)
    }

    override fun onStartDragHandler() {
        // Lock right drawer
        setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, Gravity.RIGHT)
    }

    override fun onEndDragHandler() {
        // Restore right drawer
        setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, Gravity.RIGHT)
        if (null != mSearchBarMover) {
            mSearchBarMover!!.returnSearchBarPosition()
        }
    }

    override fun onStateChange(
        searchBar: SearchBar,
        newState: Int,
        oldState: Int,
        animation: Boolean
    ) {
        if (null == mLeftDrawable || null == mRightDrawable) {
            return
        }
        when (oldState) {
            SearchBar.STATE_NORMAL -> {
                mLeftDrawable!!.setArrow(if (animation) ANIMATE_TIME else 0)
                mRightDrawable!!.setDelete(if (animation) ANIMATE_TIME else 0)
            }
            SearchBar.STATE_SEARCH -> if (newState == SearchBar.STATE_NORMAL) {
                mLeftDrawable!!.setMenu(if (animation) ANIMATE_TIME else 0)
                mRightDrawable!!.setAdd(if (animation) ANIMATE_TIME else 0)
            }
            SearchBar.STATE_SEARCH_LIST -> if (newState == STATE_NORMAL) {
                mLeftDrawable!!.setMenu(if (animation) ANIMATE_TIME else 0)
                mRightDrawable!!.setAdd(if (animation) ANIMATE_TIME else 0)
            }
            else -> {
                mLeftDrawable!!.setArrow(if (animation) ANIMATE_TIME else 0)
                mRightDrawable!!.setDelete(if (animation) ANIMATE_TIME else 0)
            }
        }
        if (newState == STATE_NORMAL || newState == STATE_SIMPLE_SEARCH) {
            setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, Gravity.LEFT)
            setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, Gravity.RIGHT)
        } else {
            setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, Gravity.LEFT)
            setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, Gravity.RIGHT)
        }
    }

    //    @Override
    //    public void onChangeSearchMode() {
    //        if (null != mSearchBarMover) {
    //            mSearchBarMover.showSearchBar();
    //        }
    //    }

    // SearchBarMover.Helper
    override fun isValidView(recyclerView: RecyclerView): Boolean {
        return mState == STATE_NORMAL && recyclerView === mRecyclerView || mState == STATE_SEARCH
    }

    // SearchBarMover.Helper
    override fun getValidRecyclerView(): RecyclerView? {
        return if (mState == STATE_NORMAL || mState == STATE_SIMPLE_SEARCH) {
            mRecyclerView
        } else {
            null
        }
    }

    // SearchBarMover.Helper
    override fun forceShowSearchBar(): Boolean {
        return mState == STATE_SIMPLE_SEARCH || mState == STATE_SEARCH_SHOW_LIST
    }

    private fun onGetGalleryListSuccess(result: GalleryListParser.Result, taskId: Int) {
        if (mHelper != null && mSearchBarMover != null &&
            mHelper!!.isCurrentTask(taskId)
        ) {
            val emptyString =
                getString(if (mUrlBuilder!!.mode == ListUrlBuilder.MODE_SUBSCRIPTION && result.noWatchedTags) R.string.gallery_list_empty_hit_subscription else R.string.gallery_list_empty_hit)
            mHelper!!.setEmptyString(emptyString)
            mHelper!!.onGetPageData(taskId, result.pages, result.nextPage, result.galleryInfoList)
        }
    }

    private fun onGetGalleryListFailure(e: Exception, taskId: Int) {
        if (mHelper != null && mSearchBarMover != null &&
            mHelper!!.isCurrentTask(taskId)
        ) {
            mHelper!!.onGetException(taskId, e)
        }
    }

    @IntDef(STATE_NORMAL, STATE_SIMPLE_SEARCH, STATE_SEARCH, STATE_SEARCH_SHOW_LIST)
    @Retention(AnnotationRetention.SOURCE)
    private annotation class State

    private class GetGalleryListListener(
        context: Context?,
        stageId: Int,
        sceneTag: String?,
        private val mTaskId: Int
    ) : EhCallback<GalleryListScene?, GalleryListParser.Result?>(context, stageId, sceneTag) {
        override fun onSuccess(result: GalleryListParser.Result?) {
            val scene = scene
            if (result != null) {
                scene?.onGetGalleryListSuccess(result, mTaskId)
            }
        }

        override fun onFailure(e: Exception) {
            val scene = scene
            scene?.onGetGalleryListFailure(e, mTaskId)
        }

        override fun onCancel() {}
        override fun isInstance(scene: SceneFragment): Boolean {
            return scene is GalleryListScene
        }
    }

    private class AddToFavoriteListener(context: Context?, stageId: Int, sceneTag: String?) :
        EhCallback<GalleryListScene?, Void?>(context, stageId, sceneTag) {
        override fun onSuccess(result: Void?) {
            showTip(R.string.add_to_favorite_success, LENGTH_SHORT)
        }

        override fun onFailure(e: Exception) {
            showTip(R.string.add_to_favorite_failure, LENGTH_LONG)
        }

        override fun onCancel() {}
        override fun isInstance(scene: SceneFragment): Boolean {
            return scene is GalleryListScene
        }
    }

    private class RemoveFromFavoriteListener(context: Context?, stageId: Int, sceneTag: String?) :
        EhCallback<GalleryListScene?, Void?>(context, stageId, sceneTag) {

        override fun onSuccess(result: Void?) {
            showTip(R.string.remove_from_favorite_success, LENGTH_SHORT)
        }

        override fun onFailure(e: Exception) {
            showTip(R.string.remove_from_favorite_failure, LENGTH_LONG)
        }

        override fun onCancel() {}
        override fun isInstance(scene: SceneFragment): Boolean {
            return scene is GalleryListScene
        }
    }

    private class QsDrawerHolder(itemView: View) :RecyclerView.ViewHolder(itemView) {
        val key: TextView
        val option: ImageView

        init {
            key = ViewUtils.`$$`(itemView, R.id.tv_key) as TextView
            option = ViewUtils.`$$`(itemView, R.id.iv_option) as ImageView
        }
    }

    private inner class MoveDialogHelper(
        private val mLabels: Array<String>,
        private val mGi: GalleryInfo
    ) : DialogInterface.OnClickListener {
        override fun onClick(dialog: DialogInterface, which: Int) {
            // Cancel check mode
            val context = context ?: return
            if (null != mRecyclerView) {
                mRecyclerView!!.outOfCustomChoiceMode()
            }
            val downloadManager = EhApplication.getDownloadManager(context)
            val downloadInfo = downloadManager.getDownloadInfo(mGi.gid) ?: return
            val label = if (which == 0) null else mLabels[which]
            downloadManager.changeLabel(listOf(downloadInfo), label)
        }
    }

    private inner class QsDrawerAdapter (private val mInflater: LayoutInflater) :
        RecyclerView.Adapter<QsDrawerHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QsDrawerHolder {
            return QsDrawerHolder(mInflater.inflate(R.layout.item_drawer_list, parent, false))
        }

        override fun onBindViewHolder(holder: QsDrawerHolder, position: Int) {
            if (mQuickSearchList != null && !mIsTopList) {
                holder.key.text = mQuickSearchList!![position].getName()
                holder.itemView.setOnClickListener { v: View? ->
                    if (null == mHelper || null == mUrlBuilder) {
                        return@setOnClickListener
                    }
                    mUrlBuilder!!.set(mQuickSearchList!![position])
                    mUrlBuilder!!.pageIndex = 0
                    onUpdateUrlBuilder()
                    mHelper!!.refresh()
                    setState(STATE_NORMAL)
                    closeDrawer(Gravity.RIGHT)
                }
            } else {
                val keywords = intArrayOf(11, 12, 13, 15)
                val toplists = intArrayOf(
                    R.string.toplist_alltime,
                    R.string.toplist_pastyear,
                    R.string.toplist_pastmonth,
                    R.string.toplist_yesterday
                )
                holder.key.text = getString(toplists[position])
                holder.option.visibility = View.GONE
                holder.itemView.setOnClickListener { v: View? ->
                    if (null == mHelper || null == mUrlBuilder) {
                        return@setOnClickListener
                    }
                    mUrlBuilder!!.keyword = keywords[position].toString()
                    mUrlBuilder!!.pageIndex = 0
                    onUpdateUrlBuilder()
                    mHelper!!.refresh()
                    setState(STATE_NORMAL)
                    closeDrawer(Gravity.RIGHT)
                }
            }
        }

        override fun getItemId(position: Int): Long {
            if (mIsTopList) {
                return position.toLong()
            }
            return if (mQuickSearchList == null) {
                0
            } else mQuickSearchList!![position].getId()
        }

        override fun getItemCount(): Int {
            return if (!mIsTopList) if (mQuickSearchList != null) mQuickSearchList!!.size else 0 else 4
        }

    }

    private abstract inner class UrlSuggestion : Suggestion() {
        override fun getText(textView: TextView): CharSequence {
            return if (textView.id == android.R.id.text1) {
                val bookImage =
                    ContextCompat.getDrawable(textView.context, R.drawable.v_book_open_x24)
                val ssb = SpannableStringBuilder("    ")
                ssb.append(getString(R.string.gallery_list_search_bar_open_gallery))
                val imageSize = (textView.textSize * 1.25).toInt()
                if (bookImage != null) {
                    bookImage.setBounds(0, 0, imageSize, imageSize)
                    ssb.setSpan(ImageSpan(bookImage), 1, 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                ssb
            } else {
                ""
            }
        }

        override fun onClick() {
            startScene(createAnnouncer())
            if (mState == STATE_SIMPLE_SEARCH) {
                setState(STATE_NORMAL)
            } else if (mState == STATE_SEARCH_SHOW_LIST) {
                setState(STATE_SEARCH)
            }
        }

        abstract fun createAnnouncer(): Announcer?
    }

    private inner class GalleryDetailUrlSuggestion (
        private val mGid: Long,
        private val mToken: String
    ) : UrlSuggestion() {
        override fun createAnnouncer(): Announcer? {
            val args = Bundle()
            args.putString(GalleryDetailScene.KEY_ACTION, GalleryDetailScene.ACTION_GID_TOKEN)
            args.putLong(GalleryDetailScene.KEY_GID, mGid)
            args.putString(GalleryDetailScene.KEY_TOKEN, mToken)
            return Announcer(GalleryDetailScene::class.java).setArgs(args)
        }
    }

    private inner class GalleryPageUrlSuggestion (
        private val mGid: Long,
        private val mPToken: String,
        private val mPage: Int
    ) : UrlSuggestion() {
        override fun createAnnouncer(): Announcer? {
            val args = Bundle()
            args.putString(ProgressScene.KEY_ACTION, ProgressScene.ACTION_GALLERY_TOKEN)
            args.putLong(ProgressScene.KEY_GID, mGid)
            args.putString(ProgressScene.KEY_PTOKEN, mPToken)
            args.putInt(ProgressScene.KEY_PAGE, mPage)
            return Announcer(ProgressScene::class.java).setArgs(args)
        }
    }

    private inner class GalleryListAdapter(
        inflater: LayoutInflater,
        resources: Resources, recyclerView: RecyclerView, type: Int
    ) : GalleryAdapter(inflater, resources, recyclerView, type, true) {
        override fun getItemCount(): Int {
            return if (null != mHelper) mHelper!!.size() else 0
        }

        public override fun onItemClick(view: View, position: Int) {
            this@GalleryListScene.onItemClick(position)
        }

        public override fun onItemLongClick(view: View, position: Int): Boolean {
            return this@GalleryListScene.onItemLongClick(position)
        }

        override fun getDataAt(position: Int): GalleryInfo? {
            return if (null != mHelper) mHelper!!.getDataAtEx(position) else null
        }
    }

    private inner class GalleryListHelper : GalleryInfoContentHelper() {
        override fun getPageData(taskId: Int, type: Int, page: Int) {
            val activity = mainActivity
            if (null == activity || null == mUrlBuilder) {
                return
            }
            mUrlBuilder!!.pageIndex = page
            if (ListUrlBuilder.MODE_IMAGE_SEARCH == mUrlBuilder!!.mode) {
                val request = EhRequest()
                request.method = EhClient.METHOD_IMAGE_SEARCH
                request.callback = GetGalleryListListener(
                    context,
                    activity.stageId, tag, taskId
                )
                request.setArgs(
                    File(StringUtils.avoidNull(mUrlBuilder!!.imagePath)),
                    mUrlBuilder!!.isUseSimilarityScan,
                    mUrlBuilder!!.isOnlySearchCovers, mUrlBuilder!!.isShowExpunged
                )
                mClient.execute(request)
            } else {
                val url = mUrlBuilder!!.build()
                val request = EhRequest()
                request.method = EhClient.METHOD_GET_GALLERY_LIST
                request.callback = GetGalleryListListener(
                    context,
                    activity.stageId, tag, taskId
                )
                request.setArgs(url)
                mClient.execute(request)
            }
        }

        override fun getContext(): Context {
            return this@GalleryListScene.requireContext()
        }

        override fun notifyDataSetChanged() {
            if (null != mAdapter) {
                mAdapter!!.notifyDataSetChanged()
            }
        }

        override fun notifyItemRangeRemoved(positionStart: Int, itemCount: Int) {
            if (null != mAdapter) {
                mAdapter!!.notifyItemRangeRemoved(positionStart, itemCount)
            }
        }

        override fun notifyItemRangeInserted(positionStart: Int, itemCount: Int) {
            if (null != mAdapter) {
                mAdapter!!.notifyItemRangeInserted(positionStart, itemCount)
            }
        }

        override fun onShowView(hiddenView: View, shownView: View) {
            if (null != mSearchBarMover) {
                mSearchBarMover!!.showSearchBar()
            }
            showActionFab()
        }

        override fun isDuplicate(d1: GalleryInfo, d2: GalleryInfo): Boolean {
            return d1.gid == d2.gid
        }

        override fun onScrollToPosition(postion: Int) {
            if (0 == postion) {
                if (null != mSearchBarMover) {
                    mSearchBarMover!!.showSearchBar()
                }
                showActionFab()
            }
        }
    }

    companion object {
        const val KEY_ACTION = "action"
        const val ACTION_HOMEPAGE = "action_homepage"
        const val ACTION_SUBSCRIPTION = "action_subscription"
        const val ACTION_WHATS_HOT = "action_whats_hot"
        const val ACTION_TOP_LIST = "action_top_list"
        const val ACTION_LIST_URL_BUILDER = "action_list_url_builder"
        const val KEY_LIST_URL_BUILDER = "list_url_builder"
        const val KEY_HAS_FIRST_REFRESH = "has_first_refresh"
        const val KEY_STATE = "state"
        private const val BACK_PRESSED_INTERVAL = 2000
        private const val STATE_NORMAL = 0
        private const val STATE_SIMPLE_SEARCH = 1
        private const val STATE_SEARCH = 2
        private const val STATE_SEARCH_SHOW_LIST = 3
        private const val ANIMATE_TIME = 300L
        private fun getSuitableTitleForUrlBuilder(
            resources: Resources, urlBuilder: ListUrlBuilder, appName: Boolean
        ): String? {
            val keyword = urlBuilder.keyword
            val category = urlBuilder.category
            return if (ListUrlBuilder.MODE_NORMAL == urlBuilder.mode && EhUtils.NONE == category &&
                TextUtils.isEmpty(keyword) && urlBuilder.advanceSearch == -1 && urlBuilder.minRating == -1 && urlBuilder.pageFrom == -1 && urlBuilder.pageTo == -1
            ) {
                resources.getString(if (appName) R.string.app_name else R.string.homepage)
            } else if (ListUrlBuilder.MODE_SUBSCRIPTION == urlBuilder.mode && EhUtils.NONE == category &&
                TextUtils.isEmpty(keyword) && urlBuilder.advanceSearch == -1 && urlBuilder.minRating == -1 && urlBuilder.pageFrom == -1 && urlBuilder.pageTo == -1
            ) {
                resources.getString(R.string.subscription)
            } else if (ListUrlBuilder.MODE_WHATS_HOT == urlBuilder.mode) {
                resources.getString(R.string.whats_hot)
            } else if (ListUrlBuilder.MODE_TOPLIST == urlBuilder.mode) {
                when (urlBuilder.keyword) {
                    "11" -> return resources.getString(R.string.toplist_alltime)
                    "12" -> return resources.getString(R.string.toplist_pastyear)
                    "13" -> return resources.getString(R.string.toplist_pastmonth)
                    "15" -> return resources.getString(R.string.toplist_yesterday)
                }
                null
            } else if (!TextUtils.isEmpty(keyword)) {
                keyword
            } else if (MathUtils.hammingWeight(category) == 1) {
                EhUtils.getCategory(category)
            } else {
                null
            }
        }

        fun startScene(scene: SceneFragment, lub: ListUrlBuilder?) {
            scene.startScene(getStartAnnouncer(lub))
        }

        fun getStartAnnouncer(lub: ListUrlBuilder?): Announcer {
            val args = Bundle()
            args.putString(KEY_ACTION, ACTION_LIST_URL_BUILDER)
            args.putParcelable(KEY_LIST_URL_BUILDER, lub)
            return Announcer(GalleryListScene::class.java).setArgs(args)
        }
    }
    private inner class GalleryListQSItemTouchHelperCallback(private val mAdapter: QsDrawerAdapter) :
        ItemTouchHelper.Callback() {
        override fun getMovementFlags(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder
        ): Int {
            return makeMovementFlags(
                ItemTouchHelper.UP or ItemTouchHelper.DOWN,
                ItemTouchHelper.LEFT
            )
        }

        @SuppressLint("NotifyDataSetChanged")
        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            val fromPosition = viewHolder.bindingAdapterPosition
            val toPosition = target.bindingAdapterPosition
            if (fromPosition == toPosition) {
                return false
            }
            if (null == mQuickSearchList) {
                return false
            }
            EhDB.moveQuickSearch(fromPosition, toPosition)
            val item: QuickSearch = mQuickSearchList!!.removeAt(fromPosition)
            mQuickSearchList!!.add(toPosition, item)
            mAdapter.notifyDataSetChanged()
            return true
        }

        @SuppressLint("NotifyDataSetChanged")
        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            if (mQuickSearchList == null) return
            val position = viewHolder.bindingAdapterPosition
            val quickSearch: QuickSearch = mQuickSearchList!!.get(position)
            EhDB.deleteQuickSearch(quickSearch)
            mQuickSearchList!!.removeAt(position)
            mAdapter.notifyDataSetChanged()
        }
    }
}