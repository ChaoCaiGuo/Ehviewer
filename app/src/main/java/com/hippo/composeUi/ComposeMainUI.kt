package com.hippo.composeUi

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.paging.*
import com.hippo.composeUi.composeExt.LocalMainActivity
import com.hippo.composeUi.composeHistory.ComposeHistory
import com.hippo.composeUi.composeHomePage.ComposeHomePage
import com.hippo.composeUi.composeHomePage.GalleryListPagingSource
import com.hippo.composeUi.composeSearch.ComposeSearch
import com.hippo.composeUi.theme.EhViewerTheme
import com.hippo.database.EhDBExt
import com.hippo.database.dao.GalleryInfo
import com.hippo.ehviewer.client.data.ListUrlBuilder
import com.hippo.ehviewer.ui.scene.BaseScene
import com.hippo.ehviewer.ui.scene.DownloadsScene
import com.hippo.ehviewer.ui.scene.FavoritesScene
import com.hippo.scene.Announcer
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import javax.inject.Inject
import com.hippo.ehviewer.R

@AndroidEntryPoint
class ComposeMainUI : BaseScene() {

    @Inject
    lateinit var mUrlBuilder: ListUrlBuilder

    @Inject
    lateinit var mOkHttpClient: OkHttpClient

    var finish = false
    override fun onBackPressed() {
        lifecycleScope.launch {
            if (finish) {
                withContext(Dispatchers.IO){
                    EhDBExt.clearGalleryListScene()
                }
                delay(300)
                mainActivity?.finish()
            } else {
                finish = true
                showTip(R.string.press_twice_exit,Toast.LENGTH_LONG)
                delay(5000)
                finish = false
            }
        }
    }

    @OptIn(ExperimentalPagingApi::class, ExperimentalLayoutApi::class)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mUrlBuilder.reset()

        val galleryList0 = Pager(
            PagingConfig(
                pageSize = 25,
                prefetchDistance = 8,
                initialLoadSize = 30
            ),
            remoteMediator = GalleryListPagingSource(
                mUrlBuilder = mUrlBuilder,
                mOkHttpClient = mOkHttpClient
            )
        ) { EhDBExt.getGalleryList() }.flow.cachedIn(lifecycleScope)


        return ComposeView(requireContext()).apply {
            setContent {
                EhViewerTheme {
                    val isImeShow = WindowInsets.isImeVisible
                    val navController = rememberNavController()

                    CompositionLocalProvider(LocalMainActivity provides mainActivity) {
                        Column(Modifier.fillMaxSize()) {
                            Spacer(
                                modifier = Modifier
                                    .statusBarsPadding()
                                    .fillMaxWidth()
                            )
                            Box(modifier = Modifier.weight(1f)) {
                                navHost(navController, galleryList0)
                            }
                            AnimatedVisibility(
                                visible = !isImeShow,
                                enter = expandVertically(
                                    expandFrom = Alignment.Bottom
                                ) + fadeIn(
                                    initialAlpha = 0.3f
                                ),
                            ) {
                                NaviGate(navController)
                            }

                        }
                    }

                }
            }
        }
    }

    @Composable
    fun NaviGate(navController: NavHostController) {

        val navScreenList = remember {
            listOf(
                ScreenNav.Home,
                ScreenNav.History,
                ScreenNav.Search,
                ScreenNav.Favourite,
                ScreenNav.Downloads
            )
        }
        NavigationBar {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            navScreenList.forEach { item ->
                NavigationBarItem(
                    icon = { Icon(painterResource(id = item.ImageId), contentDescription = null) },
                    label = { Text(stringResource(id = item.resourceId)) },
                    selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                    onClick = {
                        navController.navigate(item.route) {
                            navController.popBackStack(item.route, true)
                        }
                    }
                )
            }
        }

    }

    @Composable
    fun navHost(navController: NavHostController, galleryList0: Flow<PagingData<GalleryInfo>>) {
        NavHost(
            navController = navController,
            startDestination = ScreenNav.Home.route
        ) {
            composable(ScreenNav.Home.route)      { ComposeHomePage(galleryList0 = galleryList0, startScene = { announcer -> startScene(announcer) }) }
            composable(ScreenNav.History.route)   { ComposeHistory(startScene = { announcer -> startScene(announcer) }) }
            composable(ScreenNav.Search.route)    { ComposeSearch() }
            composable(ScreenNav.Favourite.route) { startScene(Announcer(FavoritesScene::class.java))  }
            composable(ScreenNav.Downloads.route) { startScene(Announcer(DownloadsScene::class.java)) }
        }
    }

}

