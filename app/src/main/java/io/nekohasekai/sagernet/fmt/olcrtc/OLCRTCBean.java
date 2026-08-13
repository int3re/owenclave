package io.nekohasekai.sagernet.fmt.olcrtc;

import androidx.annotation.NonNull;

import com.esotericsoftware.kryo.io.ByteBufferInput;
import com.esotericsoftware.kryo.io.ByteBufferOutput;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import io.nekohasekai.sagernet.fmt.AbstractBean;
import io.nekohasekai.sagernet.fmt.KryoConverters;

public class OLCRTCBean extends AbstractBean {

    public String authProvider;
    public String transport;
    public String roomId;
    public String encryptionKey;
    public String dnsServer;
    public String socksHost;
    public Integer socksPort;
    // vp8channel transport tuning. 0 = unset -> olcrtc core default (fps 60,
    // batch 64). Higher fps is the main throughput lever (see docs/THROUGHPUT).
    // vp8KcpWnd is the KCP send/recv window (segments): WB Stream wants ~8192 to
    // fill its fat pipe, Telemost ~768 or bufferbloat flaps its liveness.
    public Integer vp8Fps;
    public Integer vp8Batch;
    public Integer vp8KcpWnd;
    // links, when non-empty, makes this a heterogeneous / cross-carrier bond
    // (e.g. WB Stream + Telemost). Each leg has its own carrier / rooms / pacing;
    // the flat authProvider/roomId above mirror the first leg for display.
    public List<OLCRTCLink> links;

    @Override
    public void initializeDefaultValues() {
        super.initializeDefaultValues();
        if (authProvider == null) authProvider = "jitsi";
        if (transport == null) transport = "datachannel";
        if (roomId == null) roomId = "";
        if (encryptionKey == null) encryptionKey = "";
        if (dnsServer == null) dnsServer = "8.8.8.8:53";
        if (socksHost == null) socksHost = "127.0.0.1";
        if (socksPort == null) socksPort = 8808;
        if (vp8Fps == null) vp8Fps = 0;
        if (vp8Batch == null) vp8Batch = 0;
        if (vp8KcpWnd == null) vp8KcpWnd = 0;
    }

    @Override
    public void serialize(ByteBufferOutput output) {
        output.writeInt(4);
        super.serialize(output);
        output.writeString(authProvider);
        output.writeString(transport);
        output.writeString(roomId);
        output.writeString(encryptionKey);
        output.writeString(dnsServer);
        output.writeString(socksHost);
        output.writeInt(socksPort);
        output.writeInt(vp8Fps == null ? 0 : vp8Fps);
        output.writeInt(vp8Batch == null ? 0 : vp8Batch);
        output.writeInt(vp8KcpWnd == null ? 0 : vp8KcpWnd);
        int legs = links == null ? 0 : links.size();
        output.writeInt(legs);
        for (int i = 0; i < legs; i++) {
            OLCRTCLink l = links.get(i);
            output.writeString(l.authProvider);
            output.writeString(l.transport);
            output.writeString(l.roomId);
            output.writeString(l.bind);
            output.writeInt(l.vp8Fps);
            output.writeInt(l.vp8Batch);
            output.writeInt(l.vp8KcpWnd);
        }
    }

    @Override
    public void deserialize(ByteBufferInput input) {
        int version = input.readInt();
        super.deserialize(input);
        authProvider = input.readString();
        transport = input.readString();
        roomId = input.readString();
        encryptionKey = input.readString();
        dnsServer = input.readString();
        socksHost = input.readString();
        socksPort = input.readInt();
        if (version >= 2) {
            vp8Fps = input.readInt();
            vp8Batch = input.readInt();
        }
        if (version >= 3) {
            vp8KcpWnd = input.readInt();
        }
        if (version >= 4) {
            int legs = input.readInt();
            if (legs > 0) {
                links = new ArrayList<>(legs);
                for (int i = 0; i < legs; i++) {
                    OLCRTCLink l = new OLCRTCLink();
                    l.authProvider = input.readString();
                    l.transport = input.readString();
                    l.roomId = input.readString();
                    l.bind = input.readString();
                    l.vp8Fps = input.readInt();
                    l.vp8Batch = input.readInt();
                    l.vp8KcpWnd = input.readInt();
                    links.add(l);
                }
            }
        }
    }

    @Override
    public String displayName() {
        if (name != null && !name.isEmpty()) return name;
        return "olcrtc/" + authProvider;
    }

    @Override
    public String displayAddress() {
        return authProvider + " / " + transport + " / " + roomId;
    }

    @NotNull
    @Override
    public OLCRTCBean clone() {
        return KryoConverters.deserialize(new OLCRTCBean(), KryoConverters.serialize(this));
    }

    public static final Creator<OLCRTCBean> CREATOR = new CREATOR<>() {
        @NonNull
        @Override
        public OLCRTCBean newInstance() {
            return new OLCRTCBean();
        }

        @Override
        public OLCRTCBean[] newArray(int size) {
            return new OLCRTCBean[size];
        }
    };
}
