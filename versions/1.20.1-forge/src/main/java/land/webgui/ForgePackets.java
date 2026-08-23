package land.webgui;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import land.webgui.server.WebviewServerEvents;

import java.util.function.Supplier;

/**
 * Forge-specific networking: SimpleChannel setup and packet classes.
 * This file is only compiled for the Forge 1.20.1 target.
 */
public final class ForgePackets {
    private ForgePackets() {}

    private static final String PROTOCOL_VERSION_STR = String.valueOf(WebviewNetworking.PROTOCOL_VERSION);
    private static SimpleChannel CHANNEL;
    private static int packetId = 0;

    public static void register() {
        CHANNEL = NetworkRegistry.ChannelBuilder.named(WebviewPayloads.OPEN_WEB_CHANNEL)
                .networkProtocolVersion(() -> PROTOCOL_VERSION_STR)
                .clientAcceptedVersions(PROTOCOL_VERSION_STR::equals)
                .serverAcceptedVersions(PROTOCOL_VERSION_STR::equals)
                .simpleChannel();

        CHANNEL.messageBuilder(OpenWebS2CPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OpenWebS2CPacket::encode)
                .decoder(OpenWebS2CPacket::decode)
                .consumerMainThread(OpenWebS2CPacket::handle)
                .add();

        CHANNEL.messageBuilder(MainMenuS2CPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(MainMenuS2CPacket::encode)
                .decoder(MainMenuS2CPacket::decode)
                .consumerMainThread(MainMenuS2CPacket::handle)
                .add();

        CHANNEL.messageBuilder(EmitS2CPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(EmitS2CPacket::encode)
                .decoder(EmitS2CPacket::decode)
                .consumerMainThread(EmitS2CPacket::handle)
                .add();

        CHANNEL.messageBuilder(EntityContextS2CPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(EntityContextS2CPacket::encode)
                .decoder(EntityContextS2CPacket::decode)
                .consumerMainThread(EntityContextS2CPacket::handle)
                .add();

        CHANNEL.messageBuilder(TrustedOriginsS2CPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(TrustedOriginsS2CPacket::encode)
                .decoder(TrustedOriginsS2CPacket::decode)
                .consumerMainThread(TrustedOriginsS2CPacket::handle)
                .add();

        CHANNEL.messageBuilder(PageEventC2SPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(PageEventC2SPacket::encode)
                .decoder(PageEventC2SPacket::decode)
                .consumerMainThread(PageEventC2SPacket::handle)
                .add();
    }

    public static void sendToPlayer(ServerPlayer player, Object packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendToServer(Object packet) {
        CHANNEL.sendToServer(packet);
    }

    // --- Packet classes ---

    public static final class OpenWebS2CPacket {
        private final int protocolVersion;
        private final int displayMode;
        private final String url;

        public OpenWebS2CPacket(int protocolVersion, int displayMode, String url) {
            this.protocolVersion = protocolVersion;
            this.displayMode = displayMode;
            this.url = url;
        }

        public int protocolVersion() { return protocolVersion; }
        public int displayMode() { return displayMode; }
        public String url() { return url; }

        public void encode(FriendlyByteBuf buf) {
            buf.writeVarInt(protocolVersion);
            buf.writeVarInt(displayMode);
            buf.writeUtf(url, WebviewNetworking.MAX_URL_LENGTH);
        }

        public static OpenWebS2CPacket decode(FriendlyByteBuf buf) {
            return new OpenWebS2CPacket(buf.readVarInt(), buf.readVarInt(), buf.readUtf(WebviewNetworking.MAX_URL_LENGTH));
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            if (protocolVersion != WebviewNetworking.PROTOCOL_VERSION) return;
            ctx.get().enqueueWork(() -> WebGUIClient.handleOpenPayload(
                    net.minecraft.client.Minecraft.getInstance(), displayMode, url));
            ctx.get().setPacketHandled(true);
        }
    }

    public static final class MainMenuS2CPacket {
        private final String url;

        public MainMenuS2CPacket(String url) { this.url = url; }
        public String url() { return url; }

        public void encode(FriendlyByteBuf buf) { buf.writeUtf(url, WebviewNetworking.MAX_URL_LENGTH); }
        public static MainMenuS2CPacket decode(FriendlyByteBuf buf) { return new MainMenuS2CPacket(buf.readUtf(WebviewNetworking.MAX_URL_LENGTH)); }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> WebGUIMainMenuUrl.setUrl(url));
            ctx.get().setPacketHandled(true);
        }
    }

    public static final class EmitS2CPacket {
        private final String eventName;
        private final String jsonPayload;

        public EmitS2CPacket(String eventName, String jsonPayload) {
            this.eventName = eventName;
            this.jsonPayload = jsonPayload;
        }

        public String eventName() { return eventName; }
        public String jsonPayload() { return jsonPayload; }

        public void encode(FriendlyByteBuf buf) {
            buf.writeUtf(eventName, WebviewPayloads.MAX_EVENT_NAME_LENGTH);
            buf.writeUtf(jsonPayload, WebviewPayloads.MAX_EVENT_DATA_LENGTH);
        }

        public static EmitS2CPacket decode(FriendlyByteBuf buf) {
            return new EmitS2CPacket(buf.readUtf(WebviewPayloads.MAX_EVENT_NAME_LENGTH), buf.readUtf(WebviewPayloads.MAX_EVENT_DATA_LENGTH));
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> WebviewClientEmit.dispatch(eventName, jsonPayload));
            ctx.get().setPacketHandled(true);
        }
    }

    public static final class EntityContextS2CPacket {
        private final String entityJson;

        public EntityContextS2CPacket(String entityJson) { this.entityJson = entityJson; }
        public String entityJson() { return entityJson; }

        public void encode(FriendlyByteBuf buf) { buf.writeUtf(entityJson, WebviewPayloads.MAX_EVENT_DATA_LENGTH); }
        public static EntityContextS2CPacket decode(FriendlyByteBuf buf) { return new EntityContextS2CPacket(buf.readUtf(WebviewPayloads.MAX_EVENT_DATA_LENGTH)); }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> WebviewClientBridge.setEntityContext(entityJson));
            ctx.get().setPacketHandled(true);
        }
    }

    public static final class TrustedOriginsS2CPacket {
        private final String origins;

        public TrustedOriginsS2CPacket(String origins) { this.origins = origins; }
        public String origins() { return origins; }

        public void encode(FriendlyByteBuf buf) { buf.writeUtf(origins, WebviewPayloads.MAX_EVENT_DATA_LENGTH); }
        public static TrustedOriginsS2CPacket decode(FriendlyByteBuf buf) { return new TrustedOriginsS2CPacket(buf.readUtf(WebviewPayloads.MAX_EVENT_DATA_LENGTH)); }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> WebGUITrustedOrigins.set(origins));
            ctx.get().setPacketHandled(true);
        }
    }

    public static final class PageEventC2SPacket {
        private final String channel;
        private final String jsonPayload;

        public PageEventC2SPacket(String channel, String jsonPayload) {
            this.channel = channel;
            this.jsonPayload = jsonPayload;
        }

        public String channel() { return channel; }
        public String jsonPayload() { return jsonPayload; }

        public void encode(FriendlyByteBuf buf) {
            buf.writeUtf(channel, WebviewPayloads.MAX_EVENT_NAME_LENGTH);
            buf.writeUtf(jsonPayload, WebviewPayloads.MAX_EVENT_DATA_LENGTH);
        }

        public static PageEventC2SPacket decode(FriendlyByteBuf buf) {
            return new PageEventC2SPacket(buf.readUtf(WebviewPayloads.MAX_EVENT_NAME_LENGTH), buf.readUtf(WebviewPayloads.MAX_EVENT_DATA_LENGTH));
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            var player = ctx.get().getSender();
            if (player != null) {
                ctx.get().enqueueWork(() ->
                        WebviewServerEvents.firePageEvent(player, channel, jsonPayload));
            }
            ctx.get().setPacketHandled(true);
        }
    }
}
