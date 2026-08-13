package io.nekohasekai.sagernet.fmt.olcrtc;

// OLCRTCLink is one leg of a bonded olcrtc profile. A profile with a non-empty
// OLCRTCBean.links is a heterogeneous / cross-carrier bond (e.g. WB Stream + a
// Telemost lane): the core config lists each leg under `links:`, each with its
// own carrier / rooms / vp8 pacing. roomId may itself be a comma list (several
// rooms of the same carrier). A profile with no links is the flat single-carrier
// form (unchanged).
public class OLCRTCLink {
    public String authProvider;
    public String transport;
    public String roomId;   // one id/url, or a comma list of same-carrier rooms
    public String bind;     // per-leg egress network ("wifi"/"cell"); empty = default
    public int vp8Fps;
    public int vp8Batch;
    public int vp8KcpWnd;
}
