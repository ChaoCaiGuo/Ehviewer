package com.hippo.database

import androidx.paging.PagingSource
import com.hippo.database.dao.GalleryInfo

object EhDBExt{
    @JvmStatic
    suspend fun putGalleryList(info:List<GalleryInfo>) {
        EhDB.db.galleryListSceneDao().insertAll(info)
    }

    @JvmStatic
    fun homePageGalleryList(): PagingSource<Int, GalleryInfo> {
        return EhDB.db.galleryListSceneDao().homePagePagingSource()
    }

    @JvmStatic
    fun whatsHotGalleryList(): PagingSource<Int, GalleryInfo> {
        return EhDB.db.galleryListSceneDao().whatsHotPagingSource()
    }

    @JvmStatic
    fun topListGalleryList(): PagingSource<Int, GalleryInfo> {
        return EhDB.db.galleryListSceneDao().topListPagingSource()
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
