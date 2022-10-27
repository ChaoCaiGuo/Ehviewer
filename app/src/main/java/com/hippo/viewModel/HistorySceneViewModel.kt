package com.hippo.viewModel

import androidx.lifecycle.ViewModel
import androidx.paging.Pager
import androidx.paging.PagingConfig
import com.hippo.database.EhDB
import com.hippo.database.dao.HistoryInfo
import com.hippo.ehviewer.download.DownloadManager
import com.hippo.viewModel.baseViewModel.Container
import com.hippo.viewModel.baseViewModel.UiAction
import com.hippo.viewModel.baseViewModel.UiState
import com.hippo.viewModel.baseViewModel.containers
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

@HiltViewModel
class HistorySceneViewModel @Inject constructor(
    mDownloadManager: DownloadManager,
    ) : ViewModel() {

    val historyData = Pager(
        PagingConfig(20)
    ) { EhDB.getHistoryLazyList() }.flow

    data class HistoryState(
        val mShowDownloadRemove:Boolean = false,
        val mShowClearAllDialog:Boolean = false,
        val mSelectedGi: HistoryInfo? = null,
        val mDownloadManager: DownloadManager
    ) : UiState

     sealed class HistoryAction : UiAction<HistoryState>() {

         class Init(val mDownloadManager: DownloadManager) : HistoryAction() {
             override suspend fun invoke(_viewState: MutableStateFlow<HistoryState>) {
                 _viewState.updateState { copy(mDownloadManager = mDownloadManager) }
             }
         }

         class SetShowDownloadRemove(val value: Boolean) : HistoryAction() {
             override suspend fun invoke(_viewState: MutableStateFlow<HistoryState>) {
                 _viewState.updateState { copy(mShowDownloadRemove = value) }
             }
         }

         class SetShowClearAllDialog(val value: Boolean) : HistoryAction() {
             override suspend fun invoke(_viewState: MutableStateFlow<HistoryState>) {
                 _viewState.updateState { copy(mShowClearAllDialog = value) }
             }
         }

         class SetSelectedGi(val value: HistoryInfo?) : HistoryAction() {
             override suspend fun invoke(_viewState: MutableStateFlow<HistoryState>) {
                 _viewState.updateState { copy(mSelectedGi = value) }
             }
         }
    }

    fun sendEvent(event:HistoryAction){
        _container.sendEvent(event)
    }


    private val _container by containers<HistoryState,HistoryAction>(HistoryState(mDownloadManager = mDownloadManager))
    val container: Container<HistoryState, HistoryAction> = _container


}