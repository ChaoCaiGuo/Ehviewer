package com.hippo.viewModel.baseViewModel

import android.util.Log
import androidx.annotation.Keep
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.update

val TAG ="MVIevent"

@Keep
interface UiState

@Keep
open class UiAction<STATE> {
    open suspend fun invoke(_viewState: MutableStateFlow<STATE>) {}

    /**
     *  这是扩展函数  用于简化_viewState.value= _viewState.value.copy()
     *   为_viewState.updaterState { copy() }
     *   有新简化方案可以继承并修改它
     *
     *   备选方案
     *     _viewState.apply { value.copy(result = result.toString()) }
     */
    fun MutableStateFlow<STATE>.updateState(action: STATE.() -> STATE) {
        this.update { action(this.value) }
        //    action(this.value)其实就是action.invoke(this.value)
    }

//备选方案
//    fun MutableStateFlow<STATE>.updaterState2(action: STATE.() -> STATE) =
//        this.apply { action.invoke(this.value) }


}


/**
 * 状态容器，分别存储UI状态和单次事件，如果不包含单次事件，则使用[Nothing]
 */
interface Container<STATE : UiState, SINGLE_EVENT : UiAction<*>> {

    //ui状态流
    val uiStateFlow: StateFlow<STATE>

    //单次事件流
    val singleEventChannel: Channel<SINGLE_EVENT>

}

interface MutableContainer<STATE : UiState, SINGLE_EVENT : UiAction<*>> :
    Container<STATE, SINGLE_EVENT> {
    //更新状态
    fun updateState(action: STATE.() -> STATE)

    //发送事件
    fun sendEvent(event: SINGLE_EVENT)

}

internal class RealContainer<STATE : UiState, SINGLE_EVENT : UiAction<*>>(
    initialState: STATE,
    viewModel: ViewModel,
) : MutableContainer<STATE, SINGLE_EVENT> {

    private val parentScope: CoroutineScope = viewModel.viewModelScope


    private val _internalStateFlow = MutableStateFlow(initialState)


    override val uiStateFlow: StateFlow<STATE> = _internalStateFlow


    override val singleEventChannel = Channel<SINGLE_EVENT>()


    override fun updateState(action: STATE.() -> STATE) {
        _internalStateFlow.update { action(_internalStateFlow.value) }
    }

    override fun sendEvent(event: SINGLE_EVENT) {
        parentScope.launch {
            singleEventChannel.send(event)
        }
    }

    init {

        parentScope.launch(CoroutineExceptionHandler { _, throwable ->
            Log.e("UiAction", "viewModelScope: ${throwable.message}")
        }) {

            singleEventChannel.consumeAsFlow().collect {
                withContext(Dispatchers.Main) {
                    (it as UiAction<STATE>).invoke(_internalStateFlow)
                }

            }
        }
    }

}

/**
 * 构建viewModel的Ui容器，存储Ui状态和一次性事件
 * 通过懒委托来初始化
 */
fun <STATE : UiState, SINGLE_EVENT : UiAction<*>> ViewModel.containers(
    initialState: STATE,
): Lazy<MutableContainer<STATE, SINGLE_EVENT>> {
    return ContainerLazy(initialState, this)
}


//懒委托
class ContainerLazy<STATE : UiState, SINGLE_EVENT : UiAction<*>>(
    initialState: STATE,
    viewModel: ViewModel
) : Lazy<MutableContainer<STATE, SINGLE_EVENT>> {

    private var cached: MutableContainer<STATE, SINGLE_EVENT>? = null
    //cached有的取cached 没有就new一个新对象并返回
    override val value: MutableContainer<STATE, SINGLE_EVENT> = cached
        ?: RealContainer<STATE,SINGLE_EVENT>(initialState, viewModel).also {
            cached = it
        }

    override fun isInitialized() = cached != null
}


/**
 *  MVI架构 --sin0改版
 *  val container: Container<MainState, MainAction> by containers(MainState(),MainAction::class)
 *  对外容器
 *
 *  val mainState by viewModel.container.uiStateFlow.collectAsState()
 *  compose转为状态
 *
 *  var eventSent by MVIeventDelegate(viewModel.container)
 *   eventSent = MainAction.Commit(adminText, passwordText)
 *   发送事件
 */