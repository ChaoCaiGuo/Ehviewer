-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    public static void check*(...);
    public static void throw*(...);
}

-keepattributes SourceFile,LineNumberTable

-keep class com.hippo.a7zip.* { *; }

-keep class com.hippo.ehviewer.ui.fragment.* extends com.hippo.ehviewer.ui.fragment.BaseFragment { }
-keep class com.hippo.ehviewer.ui.fragment.* extends com.hippo.ehviewer.ui.fragment.BasePreferenceFragment { }

-keepnames class com.hippo.ehviewer.ui.scene.* { }

-keep class com.hippo.image.* { *; }

-keep class com.hippo.ehviewer.dao.* { *; }

-dontwarn net.sqlcipher.database.**
-dontwarn rx.**
-dontwarn org.conscrypt.ConscryptHostnameVerifier
-dontwarn org.conscrypt.Conscrypt$Version
-dontwarn org.bouncycastle.jsse.provider.BouncyCastleJsseProvider
-dontwarn org.bouncycastle.jsse.BCSSLSocket
-dontwarn org.openjsse.javax.net.ssl.SSLParameters
-dontwarn net.sqlcipher.Cursor
-dontwarn org.bouncycastle.jsse.BCSSLParameters
-dontwarn org.conscrypt.Conscrypt
-dontwarn org.openjsse.javax.net.ssl.SSLSocket
-dontwarn org.openjsse.net.ssl.OpenJSSE