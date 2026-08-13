package io.nekohasekai.sagernet.fmt.olcrtc

import java.net.URLDecoder
import java.net.URLEncoder

fun parseOLCRTCLink(link: String): OLCRTCBean {
    val bean = OLCRTCBean()
    val uri = link.removePrefix("olcrtc://")

    // Extract MIMO/name after '$'
    val dollarIdx = uri.indexOf('$')
    val name = if (dollarIdx >= 0) URLDecoder.decode(uri.substring(dollarIdx + 1), "UTF-8") else ""
    val beforeDollar = if (dollarIdx >= 0) uri.substring(0, dollarIdx) else uri

    // Extract encryptionKey after '#'
    val hashIdx = beforeDollar.indexOf('#')
    val encKey = if (hashIdx >= 0) {
        beforeDollar.substring(hashIdx + 1)
    } else ""
    val rest = if (hashIdx >= 0) beforeDollar.substring(0, hashIdx) else beforeDollar

    // Extract roomId after '@'
    val atIdx = rest.indexOf('@')
    val roomId = if (atIdx >= 0) rest.substring(atIdx + 1) else ""
    val authAndTransport = if (atIdx >= 0) rest.substring(0, atIdx) else rest

    // Extract transport after '?', plus an optional <key=value&...> param block
    val qIdx = authAndTransport.indexOf('?')
    val auth = if (qIdx >= 0) authAndTransport.substring(0, qIdx) else authAndTransport
    var transport = "datachannel"
    var vp8Fps = 0
    var vp8Batch = 0
    var vp8KcpWnd = 0
    if (qIdx >= 0) {
        val afterQ = authAndTransport.substring(qIdx + 1)
        val ltIdx = afterQ.indexOf('<')
        if (ltIdx >= 0) {
            transport = afterQ.substring(0, ltIdx)
            val gtIdx = afterQ.indexOf('>', ltIdx)
            val payload = if (gtIdx >= 0) afterQ.substring(ltIdx + 1, gtIdx) else afterQ.substring(ltIdx + 1)
            for (kv in payload.split('&')) {
                val eq = kv.indexOf('=')
                if (eq < 0) continue
                when (kv.substring(0, eq)) {
                    "vp8-fps" -> vp8Fps = kv.substring(eq + 1).toIntOrNull() ?: 0
                    "vp8-batch" -> vp8Batch = kv.substring(eq + 1).toIntOrNull() ?: 0
                    "kcp-wnd" -> vp8KcpWnd = kv.substring(eq + 1).toIntOrNull() ?: 0
                }
            }
        } else {
            transport = afterQ
        }
    }

    bean.name = name
    bean.authProvider = URLDecoder.decode(auth, "UTF-8")
    bean.transport = URLDecoder.decode(transport, "UTF-8")
    bean.roomId = URLDecoder.decode(roomId, "UTF-8")
    bean.encryptionKey = encKey
    bean.vp8Fps = vp8Fps
    bean.vp8Batch = vp8Batch
    bean.vp8KcpWnd = vp8KcpWnd

    return bean
}

fun OLCRTCBean.toUri(): String {
    val sb = StringBuilder("olcrtc://")
    sb.append(URLEncoder.encode(authProvider, "UTF-8"))
    sb.append("?")
    sb.append(URLEncoder.encode(transport, "UTF-8"))
    val params = mutableListOf<String>()
    if ((vp8Fps ?: 0) > 0) params.add("vp8-fps=$vp8Fps")
    if ((vp8Batch ?: 0) > 0) params.add("vp8-batch=$vp8Batch")
    if ((vp8KcpWnd ?: 0) > 0) params.add("kcp-wnd=$vp8KcpWnd")
    if (params.isNotEmpty()) {
        sb.append("<")
        sb.append(params.joinToString("&"))
        sb.append(">")
    }
    sb.append("@")
    sb.append(URLEncoder.encode(roomId, "UTF-8"))
    sb.append("#")
    sb.append(encryptionKey)
    if (name != null && name.isNotEmpty()) {
        sb.append("$")
        sb.append(URLEncoder.encode(name, "UTF-8"))
    }
    return sb.toString()
}
