package com.hippo.composeUi.historyScene

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.hippo.easyrecyclerview.EasyRecyclerView
import com.hippo.easyrecyclerview.FastScroller
import com.hippo.easyrecyclerview.HandlerDrawable
import com.hippo.easyrecyclerview.MarginItemDecoration
import com.hippo.ehviewer.*
import com.hippo.ehviewer.client.EhUtils
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.dao.DownloadInfo
import com.hippo.ehviewer.dao.HistoryInfo
import com.hippo.ehviewer.download.DownloadManager
import com.hippo.ehviewer.download.DownloadManager.DownloadInfoListener
import com.hippo.ehviewer.ui.CommonOperations
import com.hippo.ehviewer.ui.GalleryActivity
import com.hippo.ehviewer.ui.dialog.SelectItemWithIconAdapter
import com.hippo.ehviewer.ui.scene.EhCallback
import com.hippo.ehviewer.ui.scene.GalleryDetailScene
import com.hippo.ehviewer.ui.scene.GalleryListScene
import com.hippo.ehviewer.ui.scene.ToolbarScene
import com.hippo.scene.Announcer
import com.hippo.scene.SceneFragment
import com.hippo.view.ViewTransition
import com.hippo.widget.recyclerview.AutoStaggeredGridLayoutManager
import com.hippo.yorozuya.AssertUtils
import com.hippo.yorozuya.ViewUtils
import dagger.hilt.android.AndroidEntryPoint
import rikka.core.res.resolveColor
import javax.inject.Inject

@AndroidEntryPoint
class HistoryScene : ToolbarScene() {
    /*---------------
     View life cycle
     ---------------*/
    @Inject lateinit var mDownloadManager: DownloadManager
    @Inject lateinit var mFavouriteStatusRouter: FavouriteStatusRouter

    private var mTip: TextView? = null
    private var mFastScroller: FastScroller? = null
    private var mRecyclerView: EasyRecyclerView? = null
    private var mViewTransition: ViewTransition? = null
    private var mAdapter: RecyclerView.Adapter<*>? = null
    private var mLazyList: List<HistoryInfo>? = null

    private var mDownloadInfoListener: DownloadInfoListener= object : DownloadInfoListener {
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
    private var mFavouriteStatusRouterListener: FavouriteStatusRouter.Listener =
        FavouriteStatusRouter.Listener { _, _ ->
            if (mAdapter != null) {
                mAdapter!!.notifyDataSetChanged()
            }
        }

    override fun onDestroy() {
        super.onDestroy()
        mDownloadManager.removeDownloadInfoListener(mDownloadInfoListener)
        mFavouriteStatusRouter.removeListener(mFavouriteStatusRouterListener)
    }

    override fun getNavCheckedItem(): Int {
        return R.id.nav_history
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mDownloadManager.addDownloadInfoListener(mDownloadInfoListener)
        mFavouriteStatusRouter.addListener(mFavouriteStatusRouterListener)
    }

    override fun onCreateViewWithToolbar(
        inflater: LayoutInflater,
        container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.scene_history, container, false)
        val content = ViewUtils.`$$`(view, R.id.content)
        mRecyclerView = ViewUtils.`$$`(content, R.id.recycler_view) as EasyRecyclerView
        mFastScroller = ViewUtils.`$$`(content, R.id.fast_scroller) as FastScroller
        mTip = ViewUtils.`$$`(view, R.id.tip) as TextView
        mViewTransition = ViewTransition(content, mTip)
        val context = context
        AssertUtils.assertNotNull(context)
        val resources = context!!.resources
        val drawable = ContextCompat.getDrawable(context, R.drawable.big_history)
        drawable!!.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
        mTip!!.setCompoundDrawables(null, drawable, null, null)
        mAdapter = HistoryAdapter()
        (mAdapter as HistoryAdapter).setHasStableIds(true)
        mRecyclerView!!.adapter = mAdapter
        val layoutManager = AutoStaggeredGridLayoutManager(
            0, StaggeredGridLayoutManager.VERTICAL
        )
        layoutManager.setColumnSize(resources.getDimensionPixelOffset(Settings.getDetailSizeResId()))
        layoutManager.setStrategy(AutoStaggeredGridLayoutManager.STRATEGY_MIN_SIZE)
        mRecyclerView!!.layoutManager = layoutManager
        mRecyclerView!!.clipToPadding = false
        mRecyclerView!!.clipChildren = false
        val interval = resources.getDimensionPixelOffset(R.dimen.gallery_list_interval)
        val paddingH = resources.getDimensionPixelOffset(R.dimen.gallery_list_margin_h)
        val paddingV = resources.getDimensionPixelOffset(R.dimen.gallery_list_margin_v)
        val decoration = MarginItemDecoration(interval, paddingH, paddingV, paddingH, paddingV)
        mRecyclerView!!.addItemDecoration(decoration)
        val itemTouchHelper = ItemTouchHelper(HistoryItemTouchHelperCallback())
        itemTouchHelper.attachToRecyclerView(mRecyclerView)
        mFastScroller!!.attachToRecyclerView(mRecyclerView)
        val handlerDrawable = HandlerDrawable()
        handlerDrawable.setColor(theme.resolveColor(com.google.android.material.R.attr.colorPrimary))
        mFastScroller!!.setHandlerDrawable(handlerDrawable)
        updateLazyList()
        updateView(false)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setTitle(R.string.history)
        setNavigationIcon(R.drawable.ic_baseline_menu_24)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (null != mLazyList) {
            mLazyList = null
            if (mAdapter != null) {
                mAdapter!!.notifyDataSetChanged()
            }
        }
        if (null != mRecyclerView) {
            mRecyclerView!!.stopScroll()
            mRecyclerView = null
        }
        mViewTransition = null
        mAdapter = null
    }

    // Remember to notify
    private fun updateLazyList() {
        mLazyList = EhDB.getHistoryLazyList()
    }

    private fun updateView(animation: Boolean) {
        if (null == mAdapter || null == mViewTransition) {
            return
        }
        if (mAdapter!!.itemCount == 0) {
            mViewTransition!!.showView(1, animation)
        } else {
            mViewTransition!!.showView(0, animation)
        }
    }

    @SuppressLint("RtlHardcoded")
    override fun onNavigationClick() {
        toggleDrawer(Gravity.LEFT)
    }

    override fun getMenuResId(): Int {
        return R.menu.scene_history
    }

    private fun showClearAllDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setMessage(R.string.clear_all_history)
            .setPositiveButton(R.string.clear_all) { dialog, which ->
                if (DialogInterface.BUTTON_POSITIVE != which || null == mAdapter) {
                    return@setPositiveButton
                }
                EhDB.clearHistoryInfo()
                updateLazyList()
                mAdapter!!.notifyDataSetChanged()
                updateView(true)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        // Skip when in choice mode
        val id = item.itemId
        if (id == R.id.action_clear_all) {
            showClearAllDialog()
            return true
        }
        return false
    }

    fun onItemClick(position: Int) {
        if (null == mLazyList) {
            return
        }
        val args = Bundle()
        args.putString(GalleryDetailScene.KEY_ACTION, GalleryDetailScene.ACTION_GALLERY_INFO)
        args.putParcelable(GalleryDetailScene.KEY_GALLERY_INFO, mLazyList!![position])
        val announcer = Announcer(GalleryDetailScene::class.java).setArgs(args)
        startScene(announcer)
        return
    }

    fun onItemLongClick(position: Int): Boolean {
        val context = context
        val activity = mainActivity
        if (null == context || null == activity || null == mLazyList) {
            return false
        }
        val gi = mLazyList?.get(position) ?: return true
        val downloaded = mDownloadManager.getDownloadState(gi.gid) != DownloadInfo.STATE_INVALID
        val favourited = gi.favoriteSlot != -2
        val items = if (downloaded) arrayOf<CharSequence>(
            context.getString(R.string.read),
            context.getString(R.string.delete_downloads),
            context.getString(if (favourited) R.string.remove_from_favourites else R.string.add_to_favourites),
            context.getString(R.string.delete),
            context.getString(R.string.download_move_dialog_title)
        ) else arrayOf<CharSequence>(
            context.getString(R.string.read),
            context.getString(R.string.download),
            context.getString(if (favourited) R.string.remove_from_favourites else R.string.add_to_favourites),
            context.getString(R.string.delete)
        )
        val icons = if (downloaded) intArrayOf(
            R.drawable.v_book_open_x24,
            R.drawable.v_delete_x24,
            if (favourited) R.drawable.v_heart_broken_x24 else R.drawable.v_heart_x24,
            R.drawable.v_delete_x24,
            R.drawable.v_folder_move_x24
        ) else intArrayOf(
            R.drawable.v_book_open_x24,
            R.drawable.v_download_x24,
            if (favourited) R.drawable.v_heart_broken_x24 else R.drawable.v_heart_x24,
            R.drawable.v_delete_x24
        )
        MaterialAlertDialogBuilder(context)
            .setTitle(EhUtils.getSuitableTitle(gi))
            .setAdapter(
                SelectItemWithIconAdapter(context, items, icons)
            ) { _: DialogInterface?, which: Int ->
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
                            .setPositiveButton(
                                android.R.string.ok
                            ) { _, _->
                                mDownloadManager.deleteDownload(
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
                            RemoveFromFavoriteListener(
                                context,
                                activity.stageId,
                                tag
                            )
                        )
                    } else {
                        CommonOperations.addToFavorites(
                            activity,
                            gi,
                            AddToFavoriteListener(
                                context,
                                activity.stageId,
                                tag
                            )
                        )
                    }
                    3 -> {
                        if (null == mLazyList || null == mAdapter) {
                            return@setAdapter
                        }
                        val info = mLazyList!![position]
                        EhDB.deleteHistoryInfo(info)
                        updateLazyList()
                        mAdapter!!.notifyDataSetChanged()
                        updateView(true)
                    }
                    4 -> {
                        val labelRawList =
                            mDownloadManager.labelList
                        val labelList: MutableList<String> =
                            ArrayList(labelRawList.size + 1)
                        labelList.add(getString(R.string.default_download_label_name))
                        var i = 0
                        val n = labelRawList.size
                        while (i < n) {
                            labelList.add(labelRawList[i].label)
                            i++
                        }
                        val labels =
                            labelList.toTypedArray()
                        val helper =
                            MoveDialogHelper(labels, gi)
                        MaterialAlertDialogBuilder(context)
                            .setTitle(R.string.download_move_dialog_title)
                            .setItems(labels, helper)
                            .show()
                    }
                }
            }.show()
        return true
    }


    private inner class MoveDialogHelper(
        private val mLabels: Array<String>,
        private val mGi: GalleryInfo
    ) : DialogInterface.OnClickListener {
        override fun onClick(dialog: DialogInterface, which: Int) {
            if (null != mRecyclerView) {
                mRecyclerView!!.outOfCustomChoiceMode()
            }
            val downloadInfo = mDownloadManager.getDownloadInfo(mGi.gid) ?: return
            val label = if (which == 0) null else mLabels[which]
            mDownloadManager.changeLabel(listOf(downloadInfo), label)
        }
    }



    inner class HistoryAdapter : RecyclerView.Adapter<HistoryAdapter.ComposeViewHolder>() {

        inner class ComposeViewHolder(val composeView: ComposeView) :
            RecyclerView.ViewHolder(composeView)

        override fun getItemId(position: Int): Long {
            return mLazyList?.get(position)?.gid ?: super.getItemId(position)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ComposeViewHolder {
            val view = ComposeView(parent.context)
            return ComposeViewHolder(view)
        }

        override fun onBindViewHolder(holder: ComposeViewHolder, position: Int) {
            if (null == mLazyList) {
                return
            }
            val gi = mLazyList!![position]

            holder.composeView.setContent {
                HistoryAdapterView(gi, mDownloadManager.containDownloadInfo(gi.gid),
                    { onItemClick(position) }, { onItemLongClick(position) })
            }

        }

        override fun getItemCount(): Int {
            return mLazyList?.size ?: 0
        }
    }


    private inner class HistoryItemTouchHelperCallback : ItemTouchHelper.Callback() {
        override fun getMovementFlags(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder
        ): Int {
            return makeMovementFlags(0, ItemTouchHelper.LEFT)
        }

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            return false
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val mPosition = viewHolder.bindingAdapterPosition
            if (null == mLazyList || mAdapter == null) {
                return
            }
            val info = mLazyList!![mPosition]
            EhDB.deleteHistoryInfo(info)
            updateLazyList()
            mAdapter!!.notifyItemRemoved(mPosition)
            updateView(true)
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
}
