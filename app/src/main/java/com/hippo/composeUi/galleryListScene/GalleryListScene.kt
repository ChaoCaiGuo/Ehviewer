package com.hippo.composeUi.galleryListScene

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.paging.*
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.SubcomposeAsyncImage
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import com.hippo.composeUi.ScreenNav
import com.hippo.composeUi.composeExt.LocalNewPx
import com.hippo.composeUi.composeExt.items
import com.hippo.composeUi.composeExt.pagerTabIndicatorOffset
import com.hippo.composeUi.theme.EhViewerTheme
import com.hippo.database.EhDBExt
import com.hippo.database.dao.GalleryInfo
import com.hippo.ehviewer.R
import com.hippo.ehviewer.client.data.ListUrlBuilder
import com.hippo.ehviewer.ui.scene.BaseScene
import com.hippo.ehviewer.ui.scene.GalleryDetailScene
import com.hippo.ehviewer.widget.SimpleRatingView
import com.hippo.scene.Announcer
import com.hippo.viewModel.HistorySceneViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import javax.inject.Inject

@AndroidEntryPoint
class GalleryListScene2 : BaseScene() {

    @Inject
    lateinit var mUrlBuilder: ListUrlBuilder

    @Inject
    lateinit var mOkHttpClient: OkHttpClient


    @OptIn(ExperimentalPagingApi::class)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d("HistorySceneViewModel", "de: ${HistorySceneViewModel::class.nestedClasses}")
        mUrlBuilder.reset()

         val galleryList0 = Pager(
            PagingConfig(
                pageSize= 25,
                prefetchDistance = 8,
                initialLoadSize = 30
            ),
            remoteMediator = GalleryListPagingSource(mUrlBuilder = mUrlBuilder, mOkHttpClient = mOkHttpClient)
        ) { EhDBExt.getGalleryList()}.flow.cachedIn(lifecycleScope)


        return ComposeView(requireContext()).apply {
            setContent {
                EhViewerTheme{
                    val navController  = rememberNavController()
                    LaunchedEffect(Unit){
                        EhDBExt.clearGalleryListScene()
                    }

                    Column(Modifier.fillMaxSize()) {
                        Spacer(
                            modifier = Modifier
                                .statusBarsPadding()
                                .fillMaxWidth()
                        )
                        Box(modifier = Modifier.weight(1f)) {
                            navHost(navController,galleryList0)
                        }
                        NaviGate(navController)
                    }

                }
            }
        }
    }
    @Composable
    fun NaviGate(navController: NavHostController){

        val navScreenList = remember { listOf(ScreenNav.Home,ScreenNav.History,ScreenNav.Search,ScreenNav.Favourite,ScreenNav.Downloads) }
        NavigationBar {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination

            navScreenList.forEach {  item ->
                NavigationBarItem(
                    icon = {  Icon(painterResource(id = item.ImageId), contentDescription = null)},
                    label = { Text(stringResource(id = item.resourceId)) },
                    selected = currentDestination?.hierarchy?.any { it.route == item.route} == true,
                    onClick = {
                        navController.navigate(item.route){
                            navController.popBackStack(item.route,true)
                        }
                    }
                )
            }
        }

    }

    @Composable
    fun navHost(navController: NavHostController,galleryList0: Flow<PagingData<GalleryInfo>>){

        NavHost(navController =navController, startDestination = ScreenNav.Home.route) {
            composable(ScreenNav.Home.route)      {   ComposeHomePage(galleryList0 = galleryList0)  }
            composable(ScreenNav.History.route)   {   Text(text = "test")  }
            composable(ScreenNav.Search.route)    {   Text(text = "test")  }
            composable(ScreenNav.Favourite.route) {   Text(text = "test")  }
            composable(ScreenNav.Downloads.route) {   Text(text = "test")  }
        }
    }



    @Composable
    @OptIn(ExperimentalPagerApi::class)
    private fun ComposeHomePage(
        titles: Array<String> = stringArrayResource(id = R.array.galleryListScene_type),
        galleryList0: Flow<PagingData<GalleryInfo>>
    ) {
        Column {
            val pagerState = rememberPagerState(0)
            val scope = rememberCoroutineScope()

            TabRow(
                selectedTabIndex = pagerState.currentPage,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(Modifier.pagerTabIndicatorOffset(pagerState, tabPositions))
                },
            ) {
                titles.forEachIndexed { index, title ->
                    Tab(
                        text = { Text(title) },
                        selected = pagerState.currentPage == index,
                        onClick = {
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    pagerState.animateScrollToPage(index)
                                }
                            }
                        },
                    )
                }
            }
            HorizontalPager(count = titles.size, state = pagerState) {
                when (it) {
                    0 -> { ComposeGalleryList(galleryList0) }
                    1 -> { ComposeGalleryList(galleryList0) }
                    2 -> { ComposeGalleryList(galleryList0) }
                }

            }
        }
    }


    @Composable
    private fun ComposeGalleryList(galleryList: Flow<PagingData<GalleryInfo>>) {
        val galleryListPagingItems = galleryList.collectAsLazyPagingItems()
        if ((galleryListPagingItems.loadState.refresh is LoadState.Error) || galleryListPagingItems.itemCount == 0) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(id = R.string.gallery_list_empty_hit))
            }
        } else{
            LazyVerticalGrid(columns = GridCells.Fixed(2) ){
                items( items = galleryListPagingItems,
                    key = {index ->  galleryListPagingItems[index]!!.gid}
                ){
                    it?.let {
                        ItemCardView(
                            img = it.thumb,
                            title = it.title,
                            uploader = it.uploader ?: "",
                            rating = it.rating,
                            simpleLanguage = it.simpleLanguage ?: "",
                            pages = "${it.pages}p",
                            onClick = {
                                val args = Bundle().apply {
                                    putString(GalleryDetailScene.KEY_ACTION, GalleryDetailScene.ACTION_GALLERY_INFO)
                                    putParcelable(GalleryDetailScene.KEY_GALLERY_INFO, it)
                                }
                                val announcer = Announcer(GalleryDetailScene::class.java).setArgs(args)
                                startScene(announcer)
                            }
                        )
                    }
                }
            }
        }

    }
}

@Composable
fun ItemCardView(
    img: String = "",
    title: String = "",
    uploader: String = "",
    rating: Float = 0f,
    simpleLanguage: String = "",
    pages: String = "",
    onClick:()->Unit={}
) {
    Card(
        modifier = Modifier
            .clip(RoundedCornerShape(1.dp))
            .padding(3.dp)
            .fillMaxWidth(0.45f)
            .aspectRatio(0.95f),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier
            .clickable {
                onClick.invoke()
            }) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.6f),
                contentAlignment = Alignment.BottomStart
            )
            {
                SubcomposeAsyncImage(
                    model = img,
                    contentDescription = "",
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.CenterStart,
                    modifier = Modifier.fillMaxSize(),
                )
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(Color(0x80FFFFFF))) {
                    AndroidView(factory = { context ->
                        SimpleRatingView(context).also {
                            it.rating = rating
                        }
                    })
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = pages,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier.padding(end = 10.dp)
                    )
                }

            }

            Text(
                text = title,
                modifier = Modifier
                    .weight(1f)
                    .padding(5.dp, 3.dp, 5.dp, 0.dp)
                    .fillMaxWidth(),
                fontSize = 13.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                lineHeight=18.sp,
                letterSpacing=0.sp
            )

            Row(
                Modifier
                    .padding(5.dp, 0.dp, 10.dp, 4.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.file_upload_black_24dp),
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .height(20.dp)
                    )
                    Text(
                        text = uploader,
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.fillMaxWidth(0.5f),
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1

                    )
                }

                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = simpleLanguage,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )
            }
        }

    }
}