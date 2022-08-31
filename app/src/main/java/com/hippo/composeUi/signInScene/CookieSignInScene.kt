package com.hippo.composeUi.signInScene

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hippo.composeUi.theme.EhViewerTheme
import com.hippo.composeUi.widget.OutlinedTextFieldFix
import com.hippo.ehviewer.EhApplication
import com.hippo.ehviewer.R
import com.hippo.ehviewer.client.EhCookieStore
import com.hippo.ehviewer.client.EhUrl
import com.hippo.ehviewer.client.EhUtils
import com.hippo.ehviewer.ui.scene.SolidScene
import com.hippo.util.ClipboardUtil
import com.hippo.util.ExceptionUtils
import okhttp3.Cookie
import java.util.*

class CookieSignInScene : SolidScene() {

    override fun needShowLeftDrawer(): Boolean {
        return false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return (inflater.inflate(R.layout.compose_xml, container, false) as ComposeView)
            .apply {
                setContent { EhViewerTheme { ComposeCookieSignInScene() } }
            }
    }


    @OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
    @Composable
    fun ComposeCookieSignInScene() {


        val (ipbMemberId, setIpbMemberId) = rememberSaveable {
            mutableStateOf("")
        }
        val (ipbPassHash, setIpbPassHash) = rememberSaveable {
            mutableStateOf("")
        }
        val (igneous, setIgneous) = rememberSaveable {
            mutableStateOf("")
        }
        val isError = rememberSaveable {
            mutableStateListOf(false, false, false)
        }
        var showAlertDialog by rememberSaveable {
            mutableStateOf(false)
        }
        val softwareKeyboard = LocalSoftwareKeyboardController.current


        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,

        ) {
            Image(
                painter = painterResource(id = R.drawable.v_cookie_brown_x48),
                contentDescription = null,
                modifier = Modifier.padding(16.dp)
            )
            Text(
                text = stringResource(id = R.string.cookie_explain),
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 16.dp)
                    .padding(bottom = 16.dp),
                fontSize = 16.sp
            )
            Column(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .fillMaxWidth()
                    .height(200.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {


                OutlinedTextFieldFix(
                    value = ipbMemberId,
                    onValueChange = {
                        setIpbMemberId.invoke(it)
                        isError[0] = false
                    },
                    placeholder = { Text(text = "ipb_member_id") },
                    modifier = Modifier
                        .padding(bottom = 4.dp)
                        .width(260.dp),
                    singleLine = true,
                    isError = isError[0]
                )

                OutlinedTextFieldFix(
                    value = ipbPassHash,
                    onValueChange = {
                        setIpbPassHash.invoke(it)
                        isError[1] = false
                    },
                    placeholder = { Text(text = "ipb_pass_hash") },
                    modifier = Modifier
                        .padding(bottom = 4.dp)
                        .width(260.dp),
                    singleLine = true,
                    isError = isError[1]
                )

                OutlinedTextFieldFix(
                    value = igneous,
                    onValueChange = {
                        setIgneous.invoke(it)
                        isError[2] = false
                    },
                    placeholder = { Text(text = "igneous") },
                    modifier = Modifier
                        .padding(bottom = 4.dp)
                        .width(260.dp),
                    singleLine = true,
                    isError = isError[2]
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Column(
                Modifier
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = {
                        if (TextUtils.isEmpty(ipbMemberId.trim { it <= ' ' })) {
                            isError[0] = true
                            return@Button
                        }
                        if (TextUtils.isEmpty(ipbPassHash.trim { it <= ' ' })) {
                            isError[1] = true
                            return@Button
                        }
                        if (TextUtils.isEmpty(igneous.trim { it <= ' ' })) {
                            isError[2] = true
                            return@Button
                        }
                        softwareKeyboard?.hide()
                        enter(
                            ipbMemberId.trim { it <= ' ' },
                            ipbPassHash.trim { it <= ' ' },
                            igneous.trim { it <= ' ' }
                        ) { showAlertDialog = true }

                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(id = android.R.string.ok), fontSize = 18.sp)
                }

                TextButton(onClick = {
                    softwareKeyboard?.hide()
                    fillCookiesFromClipboard(
                        setIpbMemberId,
                        setIpbPassHash,
                        setIgneous
                    )
                }) {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(
                                style = SpanStyle(textDecoration = TextDecoration.Underline)
                            ) {
                                append(stringResource(id = R.string.from_clipboard))
                            }
                        },
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                    )
                }

            }

        }

        if (showAlertDialog) {
            AlertDialog(
                onDismissRequest = {
                    showAlertDialog = false
                },
                title = {
                    Text(text = stringResource(id = R.string.waring))
                },
                text = {
                    Text(text = stringResource(id = R.string.wrong_cookie_warning))
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            storeCookie(ipbMemberId, ipbPassHash, igneous)
                            setResult(RESULT_OK, null)
                            finish()
                        }
                    ) {
                        Text(text = stringResource(id = R.string.i_dont_think_so))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showAlertDialog = false
                        }
                    ) {
                        Text(stringResource(id = R.string.i_will_check_it))
                    }
                }
            )
        }


    }

    private fun fillCookiesFromClipboard(
        setIpMemberId: (String) -> Unit,
        setIpbPassHash: (String) -> Unit,
        setIgneous: (String) -> Unit
    ) {
        val text = ClipboardUtil.getTextFromClipboard()
        if (text == null) {
            showTip(R.string.from_clipboard_error, LENGTH_SHORT)
            return
        }
        try {
            val kvs: Array<String> = if (text.contains(";")) {
                text.split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            } else if (text.contains("\n")) {
                text.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            } else {
                showTip(R.string.from_clipboard_error, LENGTH_SHORT)
                return
            }
            if (kvs.size < 3) {
                showTip(R.string.from_clipboard_error, LENGTH_SHORT)
                return
            }
            for (s in kvs) {
                val kv: Array<String> = if (s.contains("=")) {
                    s.split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                } else if (s.contains(":")) {
                    s.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                } else {
                    continue
                }
                if (kv.size != 2) {
                    continue
                }
                when (kv[0].trim { it <= ' ' }.lowercase(Locale.getDefault())) {
                    "ipb_member_id" -> setIpMemberId.invoke(kv[1].trim { it <= ' ' })
                    "ipb_pass_hash" -> setIpbPassHash.invoke(kv[1].trim { it <= ' ' })
                    "igneous" -> setIgneous.invoke(kv[1].trim { it <= ' ' })
                }
            }
        } catch (e: Exception) {
            ExceptionUtils.throwIfFatal(e)
            e.printStackTrace()
            showTip(R.string.from_clipboard_error, LENGTH_SHORT)
        }
    }

    fun enter(ipbMemberId: String, ipbPassHash: String, igneous: String,showAlert:()->Unit) {
        if (!checkIpbMemberId(ipbMemberId) || !checkIpbPassHash(ipbPassHash)) {
            showAlert.invoke()
        } else {
            storeCookie(ipbMemberId, ipbPassHash, igneous)
            setResult(RESULT_OK, null)
            finish()
        }
    }


    private fun storeCookie(id: String, hash: String, igneous: String) {
        val context = context ?: return
        EhUtils.signOut(context)
        val store = EhApplication.getEhCookieStore(context)
        store.addCookie(
            newCookie(
                EhCookieStore.KEY_IPD_MEMBER_ID,
                id,
                EhUrl.DOMAIN_E
            )
        )
        store.addCookie(
            newCookie(
                EhCookieStore.KEY_IPD_MEMBER_ID,
                id,
                EhUrl.DOMAIN_EX
            )
        )
        store.addCookie(
            newCookie(
                EhCookieStore.KEY_IPD_PASS_HASH,
                hash,
                EhUrl.DOMAIN_E
            )
        )
        store.addCookie(
            newCookie(
                EhCookieStore.KEY_IPD_PASS_HASH,
                hash,
                EhUrl.DOMAIN_EX
            )
        )
        if (igneous.isNotEmpty()) {
            store.addCookie(
                newCookie(
                    EhCookieStore.KEY_IGNEOUS,
                    igneous,
                    EhUrl.DOMAIN_E
                )
            )
            store.addCookie(
                newCookie(
                    EhCookieStore.KEY_IGNEOUS,
                    igneous,
                    EhUrl.DOMAIN_EX
                )
            )
        }
    }


    private fun checkIpbMemberId(id: String): Boolean {
        var i = 0
        val n = id.length
        while (i < n) {
            val ch = id[i]
            if (ch !in '0'..'9') {
                return false
            }
            i++
        }
        return true
    }

    private fun checkIpbPassHash(hash: String): Boolean {
        if (32 != hash.length) {
            return false
        }
        var i = 0
        val n = hash.length
        while (i < n) {
            val ch = hash[i]
            if (ch !in '0'..'9' && ch !in 'a'..'z') {
                return false
            }
            i++
        }
        return true
    }

    private fun newCookie(name: String, value: String, domain: String): Cookie {
        return Cookie.Builder().name(name).value(value)
            .domain(domain).expiresAt(Long.MAX_VALUE).build()
    }
}