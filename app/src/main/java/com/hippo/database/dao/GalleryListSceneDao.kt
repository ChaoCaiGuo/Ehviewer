package com.hippo.database.dao

import androidx.paging.PagingSource
import androidx.room.*

@Dao
interface GalleryListSceneDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(galleryInfo: List<GalleryInfo>)

    @Query("SELECT * FROM GALLERY_INFO")
    fun pagingSource(): PagingSource<Int, GalleryInfo>

    @Query("DELETE FROM GALLERY_INFO")
    suspend fun clearAll()

    @Delete
    suspend fun deleteGalleryList(infoList: List<GalleryInfo>)
}