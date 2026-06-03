package me.baiyueyue.dont_do_it.network;

import me.baiyueyue.dont_do_it.game.PlayerWordData;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 所有网络同步包定义
 */
public class GamePackets {

    // ---- 包 ID ----
    public static final Identifier SYNC_ALL_WORDS_ID   = Identifier.of("dont_do_it", "sync_all_words");
    public static final Identifier SYNC_ONE_PLAYER_ID  = Identifier.of("dont_do_it", "sync_one_player");
    public static final Identifier NOTIFICATION_ID     = Identifier.of("dont_do_it", "notification");
    public static final Identifier GAME_END_ID         = Identifier.of("dont_do_it", "game_end");
    public static final Identifier REQUEST_START_ID    = Identifier.of("dont_do_it", "request_start");
    public static final Identifier UPDATE_SETTINGS_ID  = Identifier.of("dont_do_it", "update_settings");
    public static final Identifier STATE_RESET_ID      = Identifier.of("dont_do_it", "state_reset");
    public static final Identifier SPECIAL_EVENT_BOSSBAR_ID = Identifier.of("dont_do_it", "special_event_bossbar");

    // ==================== S2C: 全量同步对手词条 ====================

    public record SyncAllWordsPayload(List<VisibleWordEntry> entries) implements CustomPayload {
        public static final Id<SyncAllWordsPayload> ID = new Id<>(SYNC_ALL_WORDS_ID);

        public static SyncAllWordsPayload fromPlayerData(List<PlayerWordData> dataList) {
            return new SyncAllWordsPayload(dataList.stream().map(d -> new VisibleWordEntry(
                    d.getPlayerId(), d.getTeamColor().name(), d.getWordText(),
                    d.getHearts(), d.isEliminated(), d.getCountdownSeconds())).toList());
        }

        @Override
        public Id<? extends CustomPayload> getId() { return ID; }

        public record VisibleWordEntry(UUID playerId, String teamColor, String wordText,
                                        int hearts, boolean eliminated, int countdownSeconds) {}
    }

    // ==================== S2C: 增量同步单玩家状态 ====================

    public record SyncOnePlayerPayload(UUID playerId, String teamColor, String wordText,
                                        int hearts, boolean eliminated, int countdownSeconds,
                                        int totalTimerSeconds, int maxHearts) implements CustomPayload {
        public static final Id<SyncOnePlayerPayload> ID = new Id<>(SYNC_ONE_PLAYER_ID);
        @Override
        public Id<? extends CustomPayload> getId() { return ID; }
    }

    // ==================== S2C: 中央通知（触发/淘汰/结束） ====================

    public record NotificationPayload(String type, String message) implements CustomPayload {
        public static final Id<NotificationPayload> ID = new Id<>(NOTIFICATION_ID);
        @Override
        public Id<? extends CustomPayload> getId() { return ID; }
    }

    // ==================== S2C: 游戏结束（胜者信息） ====================

    public record GameEndPayload(UUID winnerId, String winnerName, String teamName) implements CustomPayload {
        public static final Id<GameEndPayload> ID = new Id<>(GAME_END_ID);
        @Override
        public Id<? extends CustomPayload> getId() { return ID; }
    }

    // ==================== C2S: 请求开始游戏 ====================

    public record RequestStartGamePayload() implements CustomPayload {
        public static final Id<RequestStartGamePayload> ID = new Id<>(REQUEST_START_ID);
        @Override
        public Id<? extends CustomPayload> getId() { return ID; }
    }

    // ==================== S2C: 游戏状态重置（清空客户端 HUD） ====================

    public record GameStateResetPayload() implements CustomPayload {
        public static final Id<GameStateResetPayload> ID = new Id<>(STATE_RESET_ID);
        @Override
        public Id<? extends CustomPayload> getId() { return ID; }
    }

    // ==================== C2S: 更新设置 ====================

    public record UpdateSettingsPayload(int timerSeconds) implements CustomPayload {
        public static final Id<UpdateSettingsPayload> ID = new Id<>(UPDATE_SETTINGS_ID);
        @Override
        public Id<? extends CustomPayload> getId() { return ID; }
    }

    // ==================== C2S: 更新设置（含特殊事件） ====================

    public record UpdateSettingsFullPayload(int wordTimerSeconds, int specialEventTimerSeconds, int defaultHearts) implements CustomPayload {
        public static final Id<UpdateSettingsFullPayload> ID = new Id<>(Identifier.of("dont_do_it", "update_settings_full"));
        @Override
        public Id<? extends CustomPayload> getId() { return ID; }
    }

    // ==================== S2C: 特殊事件 BossBar 同步 ====================

    public record SpecialEventBossBarPayload(String displayText, float percent, String barColor, int countdownSeconds) implements CustomPayload {
        public static final Id<SpecialEventBossBarPayload> ID = new Id<>(SPECIAL_EVENT_BOSSBAR_ID);
        @Override
        public Id<? extends CustomPayload> getId() { return ID; }
    }

    // ==================== Codec 注册 ====================

    public static void register() {
        // ---- S2C ----
        PayloadTypeRegistry.playS2C().register(SyncAllWordsPayload.ID,
                PacketCodec.of(
                        (payload, buf) -> {
                            buf.writeVarInt(payload.entries().size());
                            for (var e : payload.entries()) {
                                buf.writeUuid(e.playerId());
                                buf.writeString(e.teamColor());
                                buf.writeString(e.wordText());
                                buf.writeInt(e.hearts());
                                buf.writeBoolean(e.eliminated());
                                buf.writeInt(e.countdownSeconds());
                            }
                        },
                        buf -> {
                            int count = buf.readVarInt();
                            List<SyncAllWordsPayload.VisibleWordEntry> entries = new ArrayList<>();
                            for (int i = 0; i < count; i++) {
                                entries.add(new SyncAllWordsPayload.VisibleWordEntry(
                                        buf.readUuid(), buf.readString(), buf.readString(),
                                        buf.readInt(), buf.readBoolean(), buf.readInt()));
                            }
                            return new SyncAllWordsPayload(entries);
                        }));

        PayloadTypeRegistry.playS2C().register(SyncOnePlayerPayload.ID,
                PacketCodec.of(
                        (payload, buf) -> {
                            buf.writeUuid(payload.playerId());
                            buf.writeString(payload.teamColor());
                            buf.writeString(payload.wordText());
                            buf.writeInt(payload.hearts());
                            buf.writeBoolean(payload.eliminated());
                            buf.writeInt(payload.countdownSeconds());
                            buf.writeInt(payload.totalTimerSeconds());
                            buf.writeInt(payload.maxHearts());
                        },
                        buf -> new SyncOnePlayerPayload(
                                buf.readUuid(), buf.readString(), buf.readString(),
                                buf.readInt(), buf.readBoolean(), buf.readInt(), buf.readInt(), buf.readInt())));

        PayloadTypeRegistry.playS2C().register(NotificationPayload.ID,
                PacketCodec.of(
                        (payload, buf) -> { buf.writeString(payload.type()); buf.writeString(payload.message()); },
                        buf -> new NotificationPayload(buf.readString(), buf.readString())));

        PayloadTypeRegistry.playS2C().register(GameEndPayload.ID,
                PacketCodec.of(
                        (payload, buf) -> {
                            buf.writeUuid(payload.winnerId());
                            buf.writeString(payload.winnerName());
                            buf.writeString(payload.teamName());
                        },
                        buf -> new GameEndPayload(buf.readUuid(), buf.readString(), buf.readString())));

        PayloadTypeRegistry.playS2C().register(GameStateResetPayload.ID,
                PacketCodec.of(
                        (payload, buf) -> {},
                        buf -> new GameStateResetPayload()));

        // ---- C2S ----
        PayloadTypeRegistry.playC2S().register(RequestStartGamePayload.ID,
                PacketCodec.of(
                        (payload, buf) -> {},
                        buf -> new RequestStartGamePayload()));

        PayloadTypeRegistry.playC2S().register(UpdateSettingsPayload.ID,
                PacketCodec.of(
                        (payload, buf) -> buf.writeInt(payload.timerSeconds()),
                        buf -> new UpdateSettingsPayload(buf.readInt())));

        PayloadTypeRegistry.playC2S().register(UpdateSettingsFullPayload.ID,
                PacketCodec.of(
                        (payload, buf) -> { buf.writeInt(payload.wordTimerSeconds()); buf.writeInt(payload.specialEventTimerSeconds()); buf.writeInt(payload.defaultHearts()); },
                        buf -> new UpdateSettingsFullPayload(buf.readInt(), buf.readInt(), buf.readInt())));

        // ---- S2C: 特殊事件 BossBar ----
        PayloadTypeRegistry.playS2C().register(SpecialEventBossBarPayload.ID,
                PacketCodec.of(
                        (payload, buf) -> {
                            buf.writeString(payload.displayText());
                            buf.writeFloat(payload.percent());
                            buf.writeString(payload.barColor());
                            buf.writeInt(payload.countdownSeconds());
                        },
                        buf -> new SpecialEventBossBarPayload(
                                buf.readString(), buf.readFloat(), buf.readString(), buf.readInt())));
    }
}
