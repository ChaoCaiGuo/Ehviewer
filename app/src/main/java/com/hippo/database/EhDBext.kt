package com.hippo.database

import androidx.paging.PagingSource
import com.hippo.database.dao.GalleryInfo

object EhDBExt{
    @JvmStatic
    suspend fun putGalleryList(info:List<GalleryInfo>) {
        EhDB.db.galleryListSceneDao().insertAll(info)
    }

    @JvmStatic
    fun getGalleryList(): PagingSource<Int, GalleryInfo> {
        return EhDB.db.galleryListSceneDao().pagingSource()
    }

    @JvmStatic
    suspend fun clearGalleryListScene() {
        EhDB.db.galleryListSceneDao().clearAll()
    }

    @JvmStatic
    suspend fun deleteGalleryList(infoList: List<GalleryInfo>) {
        EhDB.db.galleryListSceneDao().deleteGalleryList(infoList)
    }
}
