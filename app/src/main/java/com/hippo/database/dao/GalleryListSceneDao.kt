package com.hippo.database.dao

import androidx.paging.PagingSource
import androidx.room.*

@Dao
interface GalleryListSceneDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(galleryInfo: List<GalleryInfo>)

    @Query("SELECT * FROM GALLERY_INFO WHERE HOME = TRUE ORDER BY GID DESC")
    fun homePagePagingSource(): PagingSource<Int, GalleryInfo>

    @Query("SELECT * FROM GALLERY_INFO WHERE HOT = TRUE ORDER BY GID DESC")
    fun whatsHotPagingSource(): PagingSource<Int, GalleryInfo>

    @Query("SELECT * FROM GALLERY_INFO WHERE TOP = TRUE ORDER BY GID DESC")
    fun topListPagingSource(): PagingSource<Int, GalleryInfo>
    @Query("DELETE FROM GALLERY_INFO")
    suspend fun clearAll()

    @Delete
    suspend fun deleteGalleryList(infoList: List<GalleryInfo>)
}