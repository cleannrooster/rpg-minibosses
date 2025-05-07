package com.cleannrooster.rpg_minibosses.config;

import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import com.google.gson.Gson;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

public record ConfigSync() {
    public static Identifier ID = new Identifier(RPGMinibosses.MOD_ID, "config_sync");

    public static PacketByteBuf write(ServerConfig config) {
        var gson = new Gson();
        var json = gson.toJson(config);
        var buffer = PacketByteBufs.create();
        buffer.writeString(json);
        return buffer;
    }

    public static ServerConfig read(PacketByteBuf buffer) {
        var gson = new Gson();
        var json = buffer.readString();
        var config = gson.fromJson(json, ServerConfig.class);
        return config;
    }

}
