package com.hippo.composeUi.historyScene

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.stopScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.items
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.hippo.composeUi.widget.ComposeDialogWithSelectItem
import com.hippo.composeUi.widget.DialogBaseSelectItemWithIconAdapter
import com.hippo.composeUi.widget.SwipeToDismiss
import com.hippo.ehviewer.EhDB
import com.hippo.ehviewer.FavouriteStatusRouter
import com.hippo.ehviewer.R
import com.hippo.ehviewer.client.EhUtils
import com.hippo.ehviewer.dao.DownloadInfo
import com.hippo.ehviewer.dao.HistoryInfo
import com.hippo.ehviewer.download.DownloadManager
import com.hippo.ehviewer.ui.CommonOperations
import com.hippo.ehviewer.ui.GalleryActivity
import com.hippo.ehviewer.ui.scene.EhCallback
import com.hippo.ehviewer.ui.scene.GalleryDetailScene
import com.hippo.ehviewer.ui.scene.GalleryListScene
import com.hippo.ehviewer.ui.scene.ToolbarScene
import com.hippo.scene.Announcer
import com.hippo.scene.SceneFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class HistoryScene : ToolbarScene() {

    @Inject
    lateinit var mDownloadManager: DownloadManager

    @Inject
    lateinit var mFavouriteStatusRouter: FavouriteStatusRouter

    private var mShowDownloadRemove by mutableStateOf(false)
    private var mShowClearAllDialog by mutableStateOf(false)
    private var mSelectedGi by mutableStateOf<HistoryInfo?>(null)
    private val historyData = Pager(
        PagingConfig(20)
    ) { EhDB.getHistoryLazyList() }.flow
    private val mListState = LazyListState(0, 0)

    override fun getNavCheckedItem(): Int {
        return R.id.nav_history
    }

    override fun onCreateViewWithToolbar(
        inflater: LayoutInflater,
        container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .fillMaxSize()
                ) {
                    val historyLazyPagingItems = historyData.collectAsLazyPagingItems()
                    if ((historyLazyPagingItems.loadState.refresh is LoadState.Error) || historyLazyPagingItems.itemCount == 0) {
                        ComposeNoHistory()
                    } else
                        ComposeHistoryList(historyLazyPagingItems)

                    ComposeShowDialog()
                    ComposeClearAllDialog()
                    ComposeDownloadRemove()

                }

            }
        }
    }

    @Composable
    private fun ComposeHistoryList(lazyPagingItems: LazyPagingItems<HistoryInfo>) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(
                items = lazyPagingItems,
                key = {item: HistoryInfo -> item.gid }
            ) {  gi: HistoryInfo? ->
                gi?.let {
                    var mShowDownload by remember {
                        mutableStateOf(false)
                    }
                    LaunchedEffect(mSelectedGi) {
                        mShowDownload = mDownloadManager.containDownloadInfo(gi.gid)
                        delay(1000)
                        mShowDownload = mDownloadManager.containDownloadInfo(gi.gid)
                        //多次刷新保证UI正确
                    }
                    SwipeToDismiss(
                        onDismiss = {
                            EhDB.deleteHistoryInfo(gi)
                        },
                        dismissThresholds = 0.5f
                    ) {
                        HistoryAdapterView(gi,
                            mShowDownload,
                            { onItemClick(gi) },
                            { mSelectedGi = gi })
                    }
                }

            }

        }

    }

    @Composable
    private fun ComposeShowDialog() {
        if (mSelectedGi != null)
            dialogSelectItemAdapter(mSelectedGi!!) { mSelectedGi = null }?.let {
                ComposeDialogWithSelectItem(
                    title = {
                        Text(
                            text = if (mSelectedGi != null) EhUtils.getSuitableTitle(mSelectedGi)
                                    else "Error",
                            fontSize = 20.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    onDismissRequest = { mSelectedGi = null },
                    dialogSelectItemAdapter = it
                )
            }
    }

    @Composable
    private fun ComposeClearAllDialog() {
        if (mShowClearAllDialog) {
            AlertDialog(
                onDismissRequest = {
                    mShowClearAllDialog = false
                },
                title = {
                    Text(text = stringResource(id = R.string.clear_all))
                },
                text = {
                    Text(text = stringResource(id = R.string.clear_all_history))
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            EhDB.clearHistoryInfo()
                            mShowClearAllDialog = false
                        }
                    ) {
                        Text(stringResource(id = R.string.clear_all))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            mShowClearAllDialog = false
                        }
                    ) {
                        Text(stringResource(id = android.R.string.cancel))
                    }
                }
            )
        }
    }

    @Composable
    private fun ComposeDownloadRemove() {
        if (mShowDownloadRemove) {
            var title by remember {
                mutableStateOf(
                    getString(R.string.download_remove_dialog_message, mSelectedGi?.title)
                )
            }
            AlertDialog(
                onDismissRequest = {
                    mShowDownloadRemove = false
                },
                title = {
                    Text(text = stringResource(id = R.string.download_remove_dialog_title))
                },
                text = {
                    Text(text = title)
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            mSelectedGi ?: return@TextButton
                            mDownloadManager.deleteDownload(mSelectedGi!!.gid)
                            title = ""
                            mShowDownloadRemove = false
                        }
                    ) {
                        Text(stringResource(id = android.R.string.ok))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            mShowDownloadRemove = false
                        }
                    ) {
                        Text(stringResource(id = android.R.string.cancel))
                    }
                }
            )
        }

    }

    @Composable
    private fun ComposeNoHistory() {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.big_history),
                contentDescription = null
            )
            Spacer(modifier = Modifier.padding(vertical = 5.dp))
            Text(text = stringResource(id = R.string.no_history), fontSize = 20.sp)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setTitle(R.string.history)
        setNavigationIcon(R.drawable.ic_baseline_menu_24)
    }

    override fun onDestroyView() {
        viewLifecycleOwner.lifecycleScope.launch {
            mListState.stopScroll()
        }

        super.onDestroyView()
    }

    @SuppressLint("RtlHardcoded")
    override fun onNavigationClick() {
        toggleDrawer(Gravity.LEFT)
    }

    override fun getMenuResId(): Int {
        return R.menu.scene_history
    }


    override fun onMenuItemClick(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_clear_all) {
            mShowClearAllDialog = true
            return true
        }
        return false
    }

    fun onItemClick(gi: HistoryInfo) {
        val args = Bundle().apply {
            putString(
                GalleryDetailScene.KEY_ACTION,
                GalleryDetailScene.ACTION_GALLERY_INFO
            )
            putParcelable(
                GalleryDetailScene.KEY_GALLERY_INFO,
                gi
            )
        }
        val announcer = Announcer(GalleryDetailScene::class.java).setArgs(args)
        startScene(announcer)
        return
    }

    private fun dialogSelectItemAdapter(
        gi: HistoryInfo,
        closeDialog: () -> Unit = {}
    ): ArrayList<DialogBaseSelectItemWithIconAdapter>? {
        val context = context ?: return null
        val activity = mainActivity ?: return null
        val downloaded = mDownloadManager.getDownloadState(gi.gid) != DownloadInfo.STATE_INVALID
        val favourited = gi.favoriteSlot != -2
        return arrayListOf(
            //read
            DialogBaseSelectItemWithIconAdapter(
                R.string.read,
                R.drawable.v_book_open_x24
            ) {
                val intent = Intent(context, GalleryActivity::class.java)
                intent.action = GalleryActivity.ACTION_EH
                intent.putExtra(GalleryActivity.KEY_GALLERY_INFO, gi)
                startActivity(intent)
                closeDialog.invoke()
            },
            //downloads
            DialogBaseSelectItemWithIconAdapter(
                if (downloaded) R.string.delete_downloads else R.string.downloads,
                if (downloaded) R.drawable.v_delete_x24 else R.drawable.v_download_x24
            ) {
                if (downloaded) {
                    mShowDownloadRemove = true
                } else {
                    CommonOperations.startDownload(activity, gi, false)
                }
                closeDialog.invoke()
            },
            //favourites
            DialogBaseSelectItemWithIconAdapter(
                if (favourited) R.string.remove_from_favourites else R.string.add_to_favourites,
                if (favourited) R.drawable.v_heart_broken_x24 else R.drawable.v_heart_x24
            ) {

                if (favourited) {
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
                closeDialog.invoke()
            },
            //delete
            DialogBaseSelectItemWithIconAdapter(
                R.string.delete,
                R.drawable.v_delete_x24
            ) {
                EhDB.deleteHistoryInfo(gi)
                closeDialog.invoke()
            },

            ).also {
            if (downloaded)
                it.add(
                    //move
                    DialogBaseSelectItemWithIconAdapter(
                        R.string.download_move_dialog_title,
                        R.drawable.v_folder_move_x24
                    ) {

                        val labelRawList = mDownloadManager.labelList
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
                        MaterialAlertDialogBuilder(context)
                            .setTitle(R.string.download_move_dialog_title)
                            .setItems(labels , object : DialogInterface.OnClickListener{
                                override fun onClick(dialog: DialogInterface, which: Int) {
                                    val downloadInfo = mDownloadManager.getDownloadInfo(gi.gid) ?: return
                                    val label = if (which == 0) null else labels[which]
                                    mDownloadManager.changeLabel(listOf(downloadInfo), label)
                                }
                            })
                            .show()
                        closeDialog.invoke()
                    })
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
