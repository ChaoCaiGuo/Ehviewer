package com.hippo.composeUi.historyScene

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.items
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.hippo.composeUi.composeExt.LocalMainActivity
import com.hippo.composeUi.widget.ComposeDialogWithSelectItem
import com.hippo.composeUi.widget.DialogBaseSelectItemWithIconAdapter
import com.hippo.composeUi.widget.SwipeToDismiss
import com.hippo.database.EhDB
import com.hippo.database.dao.DownloadInfo
import com.hippo.database.dao.HistoryInfo
import com.hippo.ehviewer.R
import com.hippo.ehviewer.client.EhClient
import com.hippo.ehviewer.client.EhUtils
import com.hippo.ehviewer.download.DownloadManager
import com.hippo.ehviewer.ui.CommonOperations
import com.hippo.ehviewer.ui.GalleryActivity
import com.hippo.ehviewer.ui.MainActivity
import com.hippo.ehviewer.ui.scene.EhCallback
import com.hippo.ehviewer.ui.scene.GalleryDetailScene
import com.hippo.ehviewer.ui.scene.GalleryListScene
import com.hippo.ehviewer.ui.scene.ToolbarScene
import com.hippo.scene.Announcer
import com.hippo.scene.SceneFragment
import com.hippo.viewModel.HistorySceneViewModel
import com.hippo.viewModel.HistorySceneViewModel.HistoryAction.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay

@AndroidEntryPoint
class HistoryScene : ToolbarScene() {

    override fun getNavCheckedItem() = R.id.nav_history

    override fun onCreateViewWithToolbar(
        inflater: LayoutInflater,
        container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                ComposeHistory()
            }
        }
    }

    @Composable
    private fun ComposeHistory(viewModel: HistorySceneViewModel = hiltViewModel()) {
        val state by viewModel.container.uiStateFlow.collectAsState()

        Box(
            modifier = Modifier
                .padding(horizontal = 3.dp)
                .fillMaxSize()
        ) {
            val historyLazyPagingItems = viewModel.historyData.collectAsLazyPagingItems()
            if ((historyLazyPagingItems.loadState.refresh is LoadState.Error) || historyLazyPagingItems.itemCount == 0) {
                ComposeNoHistory()
            } else{
                Column(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),verticalAlignment = Alignment.CenterVertically) {
                        Text(text = stringResource(id = R.string.history),fontSize = 22.sp)
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(
                            onClick = {
                                viewModel.sendEvent(SetShowClearAllDialog(true))
                            },
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.v_clear_all_dark_x24),
                                contentDescription = null
                            )
                        }
                       
                    }
                    ComposeHistoryList(
                        lazyPagingItems  = historyLazyPagingItems,
                        downloadManager = state.mDownloadManager,
                        state = state,
                        sendEvent = {historyAction -> viewModel.sendEvent(historyAction) }
                    )
                }
            }


            ComposeShowDialog(
                downloadManager = state.mDownloadManager,
                state = state,
                sendEvent = {historyAction -> viewModel.sendEvent(historyAction) },
            )
            ComposeClearAllDialog(
                state = state,
                sendEvent = {historyAction -> viewModel.sendEvent(historyAction) }
            )
            ComposeDownloadRemove(
                downloadManager = state.mDownloadManager,
                state = state,
                sendEvent = {historyAction -> viewModel.sendEvent(historyAction) }
            )

        }
    }

    @Composable
    private fun ComposeHistoryList(
        lazyPagingItems: LazyPagingItems<HistoryInfo>,
        downloadManager: DownloadManager,
        state: HistorySceneViewModel.HistoryState,
        sendEvent: (HistorySceneViewModel.HistoryAction) -> Unit
    ) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(
                items = lazyPagingItems,
                key = {item: HistoryInfo -> item.gid }
            ) {  gi: HistoryInfo? ->
                gi?.let {
                    var mShowDownload by remember {
                        mutableStateOf(false)
                    }
                    LaunchedEffect(state.mSelectedGi) {
                        mShowDownload = downloadManager.containDownloadInfo(gi.gid)
                        delay(1000)
                        mShowDownload = downloadManager.containDownloadInfo(gi.gid)
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
                            { sendEvent.invoke(SetSelectedGi(gi)) })
                    }
                }

            }

        }

    }

    @Composable
    private fun ComposeShowDialog(
        downloadManager: DownloadManager,
        state: HistorySceneViewModel.HistoryState,
        sendEvent: (HistorySceneViewModel.HistoryAction) -> Unit
    ) {
        if (state.mSelectedGi != null)
            dialogSelectItemAdapter(state.mSelectedGi,downloadManager,sendEvent) { sendEvent.invoke(SetSelectedGi(null)) }?.let {
                ComposeDialogWithSelectItem(
                    title = {
                        Text(
                            //不要去掉这儿的!= null 因为compose 可能会让Text的text比控件本身先重构  如果为null 有概率闪退
                            text = if (state.mSelectedGi != null) EhUtils.getSuitableTitle(state.mSelectedGi) else "Error",
                            fontSize = 20.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    onDismissRequest = { sendEvent.invoke(SetSelectedGi(null)) },
                    dialogSelectItemAdapter = it
                )
            }
    }

    @Composable
    private fun ComposeClearAllDialog(
        state: HistorySceneViewModel.HistoryState,
        sendEvent: (HistorySceneViewModel.HistoryAction) -> Unit
    ) {
        if (state.mShowClearAllDialog) {
            AlertDialog(
                onDismissRequest = {
                    sendEvent.invoke(SetShowClearAllDialog(false))
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
                            sendEvent.invoke(SetShowClearAllDialog(false))
                        }
                    ) {
                        Text(stringResource(id = R.string.clear_all))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            sendEvent.invoke(SetShowClearAllDialog(false))
                        }
                    ) {
                        Text(stringResource(id = android.R.string.cancel))
                    }
                }
            )
        }
    }

    @Composable
    private fun ComposeDownloadRemove(
        downloadManager: DownloadManager,
        state: HistorySceneViewModel.HistoryState,
        sendEvent: (HistorySceneViewModel.HistoryAction) -> Unit
    ) {
        LaunchedEffect(state.mShowDownloadRemove){
            if(!state.mShowDownloadRemove){
                sendEvent.invoke(SetSelectedGi(null))
            }
        }
        if (state.mShowDownloadRemove) {
            var title by remember {
                mutableStateOf(
                    getString(R.string.download_remove_dialog_message, state.mSelectedGi?.title)
                )
            }
            AlertDialog(
                onDismissRequest = {
                    sendEvent.invoke(SetShowDownloadRemove(false))
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
                            state.mSelectedGi ?: return@TextButton
                            downloadManager.deleteDownload(state.mSelectedGi.gid)
                            title = ""
                            sendEvent.invoke(SetShowDownloadRemove(false))
                        }
                    ) {
                        Text(stringResource(id = android.R.string.ok))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            sendEvent.invoke(SetShowDownloadRemove(false))
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

    @SuppressLint("RtlHardcoded")
    override fun onNavigationClick() {
        toggleDrawer(Gravity.LEFT)
    }

    override fun getMenuResId(): Int {
        return R.menu.scene_history
    }


    override fun onMenuItemClick(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_clear_all) {
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
        mDownloadManager: DownloadManager,
        sendEvent: (HistorySceneViewModel.HistoryAction) -> Unit,
        closeDialog: () -> Unit = {},

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
                    sendEvent.invoke(SetShowDownloadRemove(true))
                } else {
                    CommonOperations.startDownload(activity, gi, false)
                    closeDialog.invoke()
                }
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

@Composable
fun ComposeHistory(
    startScene: (Announcer) -> Unit,
    viewModel: HistorySceneViewModel = hiltViewModel()
) {
    val state by viewModel.container.uiStateFlow.collectAsState()

    Box(
        modifier = Modifier
            .padding(horizontal = 3.dp)
            .fillMaxSize()
    ) {
        val historyLazyPagingItems = viewModel.historyData.collectAsLazyPagingItems()
        if ((historyLazyPagingItems.loadState.refresh is LoadState.Error) || historyLazyPagingItems.itemCount == 0) {
            ComposeNoHistory()
        } else{
            Column(Modifier.fillMaxWidth()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),verticalAlignment = Alignment.CenterVertically) {
                    Text(text = stringResource(id = R.string.history),fontSize = 22.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = {
                            viewModel.sendEvent(SetShowClearAllDialog(true))
                        },
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.v_clear_all_dark_x24),
                            contentDescription = null
                        )
                    }

                }
                ComposeHistoryList(
                    lazyPagingItems  = historyLazyPagingItems,
                    downloadManager = state.mDownloadManager,
                    startScene = startScene,
                    state = state,
                    sendEvent = {historyAction -> viewModel.sendEvent(historyAction) }
                )
            }
        }


        ComposeShowDialog(
            downloadManager = state.mDownloadManager,
            state = state,
            sendEvent = {historyAction -> viewModel.sendEvent(historyAction) },
        )
        ComposeClearAllDialog(
            state = state,
            sendEvent = {historyAction -> viewModel.sendEvent(historyAction) }
        )
        ComposeDownloadRemove(
            downloadManager = state.mDownloadManager,
            state = state,
            sendEvent = {historyAction -> viewModel.sendEvent(historyAction) }
        )

    }
}

@Composable
private fun ComposeHistoryList(
    lazyPagingItems: LazyPagingItems<HistoryInfo>,
    downloadManager: DownloadManager,
    startScene: (Announcer) -> Unit,
    state: HistorySceneViewModel.HistoryState,
    sendEvent: (HistorySceneViewModel.HistoryAction) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(
            items = lazyPagingItems,
            key = {item: HistoryInfo -> item.gid }
        ) {  gi: HistoryInfo? ->
            gi?.let {
                var mShowDownload by remember {
                    mutableStateOf(false)
                }
                LaunchedEffect(state.mSelectedGi) {
                    mShowDownload = downloadManager.containDownloadInfo(gi.gid)
                    delay(1000)
                    mShowDownload = downloadManager.containDownloadInfo(gi.gid)
                    //多次刷新保证UI正确
                }
                SwipeToDismiss(
                    onDismiss = {
                        EhDB.deleteHistoryInfo(gi)
                    },
                    dismissThresholds = 0.5f
                ) {
                    HistoryAdapterView(
                        gi,
                        mShowDownload,
                        { onItemClick(gi,startScene) },
                        { sendEvent.invoke(SetSelectedGi(gi)) }
                    )
                }
            }

        }

    }

}

@Composable
private fun ComposeShowDialog(
    downloadManager: DownloadManager,
    state: HistorySceneViewModel.HistoryState,
    sendEvent: (HistorySceneViewModel.HistoryAction) -> Unit
) {
    val mainActivity = LocalMainActivity.current
    if (state.mSelectedGi != null)
        ComposeDialogWithSelectItem(
            title = {
                Text(
                    //不要去掉这儿的!= null 因为compose 可能会让Text的text比控件本身先重构  如果为null 有概率闪退
                    text = if (state.mSelectedGi != null) EhUtils.getSuitableTitle(state.mSelectedGi) else "Error",
                    fontSize = 20.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            onDismissRequest = { sendEvent.invoke(SetSelectedGi(null)) },
            dialogSelectItemAdapter = dialogSelectItemAdapter(mainActivity,state.mSelectedGi,downloadManager,sendEvent) { sendEvent.invoke(SetSelectedGi(null)) }
        )
}

@Composable
private fun ComposeClearAllDialog(
    state: HistorySceneViewModel.HistoryState,
    sendEvent: (HistorySceneViewModel.HistoryAction) -> Unit
) {
    if (state.mShowClearAllDialog) {
        AlertDialog(
            onDismissRequest = {
                sendEvent.invoke(SetShowClearAllDialog(false))
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
                        sendEvent.invoke(SetShowClearAllDialog(false))
                    }
                ) {
                    Text(stringResource(id = R.string.clear_all))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        sendEvent.invoke(SetShowClearAllDialog(false))
                    }
                ) {
                    Text(stringResource(id = android.R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun ComposeDownloadRemove(
    downloadManager: DownloadManager,
    state: HistorySceneViewModel.HistoryState,
    sendEvent: (HistorySceneViewModel.HistoryAction) -> Unit
) {
    var title by remember { mutableStateOf("") }
    val removeDialog = stringResource(id = R.string.download_remove_dialog_message)
    LaunchedEffect(state.mShowDownloadRemove){
        if(!state.mShowDownloadRemove){
            sendEvent.invoke(SetSelectedGi(null))
            title = removeDialog.replace("%s",state.mSelectedGi?.title ?: "")
        }
    }
    if (state.mShowDownloadRemove) {

        AlertDialog(
            onDismissRequest = {
                sendEvent.invoke(SetShowDownloadRemove(false))
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
                        state.mSelectedGi ?: return@TextButton
                        downloadManager.deleteDownload(state.mSelectedGi.gid)
                        title = ""
                        sendEvent.invoke(SetShowDownloadRemove(false))
                    }
                ) {
                    Text(stringResource(id = android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        sendEvent.invoke(SetShowDownloadRemove(false))
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

private fun dialogSelectItemAdapter(
    mainActivity: MainActivity?,
    gi: HistoryInfo,
    mDownloadManager: DownloadManager,
    sendEvent: (HistorySceneViewModel.HistoryAction) -> Unit,
    closeDialog: () -> Unit = {},
    ): ArrayList<DialogBaseSelectItemWithIconAdapter> {
    val downloaded = mDownloadManager.getDownloadState(gi.gid) != DownloadInfo.STATE_INVALID
    val favourited = gi.favoriteSlot != -2
    return arrayListOf(
        //read
        DialogBaseSelectItemWithIconAdapter(
            R.string.read,
            R.drawable.v_book_open_x24
        ) {
            val intent = Intent(mainActivity, GalleryActivity::class.java)
            intent.action = GalleryActivity.ACTION_EH
            intent.putExtra(GalleryActivity.KEY_GALLERY_INFO, gi)
            mainActivity?.startActivity(intent)
            closeDialog.invoke()
        },
        //downloads
        DialogBaseSelectItemWithIconAdapter(
            if (downloaded) R.string.delete_downloads else R.string.downloads,
            if (downloaded) R.drawable.v_delete_x24 else R.drawable.v_download_x24
        ) {
            if (downloaded) {
                sendEvent.invoke(SetShowDownloadRemove(true))
            } else {
                CommonOperations.startDownload(mainActivity, gi, false)
                closeDialog.invoke()
            }
        },
        //favourites
        DialogBaseSelectItemWithIconAdapter(
            if (favourited) R.string.remove_from_favourites else R.string.add_to_favourites,
            if (favourited) R.drawable.v_heart_broken_x24 else R.drawable.v_heart_x24
        ) {

            if (favourited) {
                CommonOperations.removeFromFavorites(
                    mainActivity,
                    gi,
                    object : EhClient.Callback<Void>{
                        override fun onSuccess(result: Void?) {}

                        override fun onFailure(e: java.lang.Exception?) {}

                        override fun onCancel() {}
                    }
                )
            } else {
                CommonOperations.addToFavorites(
                    mainActivity,
                    gi,
                    object : EhClient.Callback<Void>{
                        override fun onSuccess(result: Void?) {}

                        override fun onFailure(e: java.lang.Exception?) {}

                        override fun onCancel() {}
                    }
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
                    mainActivity ?: return@DialogBaseSelectItemWithIconAdapter
                    val labelRawList = mDownloadManager.labelList
                    val labelList: MutableList<String> =
                        ArrayList(labelRawList.size + 1)
                    labelList.add(mainActivity.getString(R.string.default_download_label_name))
                    var i = 0
                    val n = labelRawList.size
                    while (i < n) {
                        labelList.add(labelRawList[i].label)
                        i++
                    }
                    val labels =
                        labelList.toTypedArray()
                    MaterialAlertDialogBuilder(mainActivity)
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

fun onItemClick(gi: HistoryInfo,startScene: (Announcer) -> Unit) {
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
}