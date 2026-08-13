package io.nekohasekai.sagernet.fmt.olcrtc

import java.net.URLDecoder
import java.net.URLEncoder

// olcrtc link:
//   olcrtc://<block>[|<block>...]#<keyhex>[$<name>]
// where each block is  <carrier>?<transport><k=v&k=v>@<room1,room2,...>
// A single block is the flat single-carrier form; two or more blocks separated
// by '|' are a heterogeneous / cross-carrier bond (e.g. WB Stream + Telemost),
// stored in OLCRTCBean.links and emitted as the core's `links:` config.
fun parseOLCRTCLink(link: String): OLCRTCBean {
    val bean = OLCRTCBean()
    val uri = link.removePrefix("olcrtc://")

    val dollarIdx = uri.indexOf('$')
    val name = if (dollarIdx >= 0) URLDecoder.decode(uri.substring(dollarIdx + 1), "UTF-8") else ""
    val beforeDollar = if (dollarIdx >= 0) uri.substring(0, dollarIdx) else uri

    val hashIdx = beforeDollar.indexOf('#')
    val encKey = if (hashIdx >= 0) beforeDollar.substring(hashIdx + 1) else ""
    val rest = if (hashIdx >= 0) beforeDollar.substring(0, hashIdx) else beforeDollar

    val legs = rest.split('|').map { it.trim() }.filter { it.isNotEmpty() }.map { parseOlcrtcBlock(it) }
    val first = legs.firstOrNull() ?: OLCRTCLink().apply { transport = "datachannel"; roomId = "" }

    bean.name = name
    bean.encryptionKey = encKey
    bean.authProvider = first.authProvider
    bean.transport = first.transport
    bean.roomId = first.roomId
    bean.vp8Fps = first.vp8Fps
    bean.vp8Batch = first.vp8Batch
    bean.vp8KcpWnd = first.vp8KcpWnd
    if (legs.size > 1) {
        bean.links = ArrayList(legs)
    }
    return bean
}

// parseOlcrtcBlock parses one "<carrier>?<transport><opts>@<rooms>" leg.
private fun parseOlcrtcBlock(block: String): OLCRTCLink {
    val leg = OLCRTCLink()
    leg.transport = "datachannel"
    leg.roomId = ""

    val atIdx = block.indexOf('@')
    val roomId = if (atIdx >= 0) block.substring(atIdx + 1) else ""
    val authAndTransport = if (atIdx >= 0) block.substring(0, atIdx) else block

    val qIdx = authAndTransport.indexOf('?')
    val auth = if (qIdx >= 0) authAndTransport.substring(0, qIdx) else authAndTransport
    if (qIdx >= 0) {
        val afterQ = authAndTransport.substring(qIdx + 1)
        val ltIdx = afterQ.indexOf('<')
        if (ltIdx >= 0) {
            leg.transport = afterQ.substring(0, ltIdx)
            val gtIdx = afterQ.indexOf('>', ltIdx)
            val payload = if (gtIdx >= 0) afterQ.substring(ltIdx + 1, gtIdx) else afterQ.substring(ltIdx + 1)
            for (kv in payload.split('&')) {
                val eq = kv.indexOf('=')
                if (eq < 0) continue
                when (kv.substring(0, eq)) {
                    "vp8-fps" -> leg.vp8Fps = kv.substring(eq + 1).toIntOrNull() ?: 0
                    "vp8-batch" -> leg.vp8Batch = kv.substring(eq + 1).toIntOrNull() ?: 0
                    "kcp-wnd" -> leg.vp8KcpWnd = kv.substring(eq + 1).toIntOrNull() ?: 0
                    "bind" -> leg.bind = kv.substring(eq + 1)
                }
            }
        } else {
            leg.transport = afterQ
        }
    }
    leg.authProvider = URLDecoder.decode(auth, "UTF-8")
    leg.transport = URLDecoder.decode(leg.transport, "UTF-8")
    leg.roomId = URLDecoder.decode(roomId, "UTF-8")
    return leg
}

fun OLCRTCBean.toUri(): String {
    val legs = links
    val sb = StringBuilder("olcrtc://")
    if (!legs.isNullOrEmpty()) {
        sb.append(legs.joinToString("|") { legToBlock(it) })
    } else {
        sb.append(legToBlock(OLCRTCLink().also {
            it.authProvider = authProvider
            it.transport = transport
            it.roomId = roomId
            it.vp8Fps = vp8Fps ?: 0
            it.vp8Batch = vp8Batch ?: 0
            it.vp8KcpWnd = vp8KcpWnd ?: 0
        }))
    }
    sb.append("#").append(encryptionKey)
    if (!name.isNullOrEmpty()) {
        sb.append("$").append(URLEncoder.encode(name, "UTF-8"))
    }
    return sb.toString()
}

private fun legToBlock(l: OLCRTCLink): String {
    val sb = StringBuilder()
    sb.append(URLEncoder.encode(l.authProvider ?: "", "UTF-8"))
    sb.append("?").append(URLEncoder.encode(l.transport ?: "", "UTF-8"))
    val params = mutableListOf<String>()
    if (l.vp8Fps > 0) params.add("vp8-fps=${l.vp8Fps}")
    if (l.vp8Batch > 0) params.add("vp8-batch=${l.vp8Batch}")
    if (l.vp8KcpWnd > 0) params.add("kcp-wnd=${l.vp8KcpWnd}")
    if (!l.bind.isNullOrEmpty()) params.add("bind=${l.bind}")
    if (params.isNotEmpty()) sb.append("<").append(params.joinToString("&")).append(">")
    sb.append("@").append(URLEncoder.encode(l.roomId ?: "", "UTF-8"))
    return sb.toString()
}
