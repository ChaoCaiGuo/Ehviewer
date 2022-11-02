package com.hippo.composeUi.composeHomePage

import android.util.Log
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.hippo.database.EhDB
import com.hippo.database.EhDBExt
import com.hippo.database.dao.EhDatabase
import com.hippo.database.dao.GalleryInfo
import com.hippo.ehviewer.client.EhEngine
import com.hippo.ehviewer.client.data.ListUrlBuilder
import com.hippo.ehviewer.client.parser.GalleryListParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.io.File

@OptIn(ExperimentalPagingApi::class)
class GalleryListPagingSource(
    private var nextPage: Int = 0,
    private val database: EhDatabase = EhDB.db,
    private val mUrlBuilder: ListUrlBuilder,
    private val mOkHttpClient: OkHttpClient
) : RemoteMediator<Int, GalleryInfo>() {

    private suspend fun getPageData(page: Int): GalleryListParser.Result? {

            return try {
                withContext(Dispatchers.IO) {
                    mUrlBuilder.pageIndex = page
                    if (ListUrlBuilder.MODE_IMAGE_SEARCH == mUrlBuilder.mode) {
                        EhEngine.imageSearch(
                            null,
                            mOkHttpClient,
                            File(mUrlBuilder.imagePath ?: ""),
                            mUrlBuilder.isUseSimilarityScan,
                            mUrlBuilder.isOnlySearchCovers,
                            mUrlBuilder.isShowExpunged
                        )
                    }
                    else{
                        val url = mUrlBuilder.build()
                        EhEngine.getGalleryList(null, mOkHttpClient, url)
                    }
                }
            } catch (e: Exception) {
                Log.e("GalleryListPagingSource", "getPageData: ${e.message}", )
                return null
            }
    }

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, GalleryInfo>
    ): MediatorResult {

        return try {
            Log.d("GalleryListPagingSource", "result is $loadType state", )
            when (loadType) {
                LoadType.REFRESH -> null
                LoadType.PREPEND -> return MediatorResult.Success(true)
                LoadType.APPEND -> {
                    state.lastItemOrNull() ?: return MediatorResult.Success(true)
                }
            }
            Log.d("GalleryListPagingSource", "now page result is $nextPage", )
            val result  = getPageData(nextPage)
            if(result == null){
                Log.d("GalleryListPagingSource", "result is null", )
                return MediatorResult.Error(Exception("can't get Result"))
            }
            Log.d("GalleryListPagingSource", "result nextPage ${result.nextPage}", )
            result.galleryInfoList.forEach {
                it.pages = nextPage
            }

            database.withTransaction {
                if (loadType == LoadType.REFRESH) {
                    EhDBExt.deleteGalleryList(result.galleryInfoList)
                }
                nextPage = result.nextPage
                EhDBExt.putGalleryList(result.galleryInfoList)

                return@withTransaction MediatorResult.Success(nextPage == null)
            }

        } catch (e: Exception) {
            Log.e("GalleryListPagingSource", "dbError: ${e.message}", )
            MediatorResult.Error(e)
        }
    }
}