package com.griefdefender.util;

import com.griefdefender.api.Tristate;
import me.lucko.luckperms.api.context.ContextSet;
import net.kyori.text.Component;
import net.kyori.text.serializer.gson.GsonComponentSerializer;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.chat.ChatType;
import org.spongepowered.api.text.chat.ChatTypes;
import org.spongepowered.api.text.serializer.TextSerializers;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SpongeUtil {

    public static org.spongepowered.api.service.context.Context getSpongeContext(com.griefdefender.api.permission.Context context) {
        return new org.spongepowered.api.service.context.Context(context.getKey(), context.getValue());
    }

    public static org.spongepowered.api.service.context.Context getSpongeContext(ContextSet context) {
        Context spongeContext = null;
        for (Map.Entry<String, String> mapEntry : context) {
            spongeContext = new Context(mapEntry.getKey(), mapEntry.getValue());
            break;
        }

        return spongeContext;
    }

    public static Text getSpongeText(Component component) {
        if (component == null) {
            return Text.EMPTY;
        }
        return TextSerializers.JSON.deserialize(GsonComponentSerializer.INSTANCE.serialize(component));
    }

    public static Set<Context> getSpongeContexts(com.griefdefender.api.permission.Context context) {
        final Set<Context> spongeContexts = new HashSet<>();
        spongeContexts.add(new Context(context.getKey(), context.getValue()));
        return spongeContexts;
    }

    public static Set<Context> getSpongeContexts(Set<com.griefdefender.api.permission.Context> contexts) {
        final Set<Context> spongeContexts = new HashSet<>();
        for (com.griefdefender.api.permission.Context gpContext : contexts) {
            spongeContexts.add(new Context(gpContext.getKey(), gpContext.getValue()));
        }

        return spongeContexts;
    }

    public static Set<Context> getSpongeContexts(ContextSet contexts) {
        final Set<Context> spongeContexts = new HashSet<>();
        for (Map.Entry<String, String> mapEntry : contexts) {
            spongeContexts.add(new Context(mapEntry.getKey(), mapEntry.getValue()));
        }

        return spongeContexts;
    }

    public static Set<com.griefdefender.api.permission.Context> fromSpongeContexts(Set<Context> contexts) {
        final Set<com.griefdefender.api.permission.Context> gpContexts = new HashSet<>();
        for (Context spongeContext : contexts) {
            gpContexts.add(new com.griefdefender.api.permission.Context(spongeContext.getKey(), spongeContext.getValue()));
        }
        return gpContexts;
    }

    public static Component fromSpongeText(Text text) {
        return GsonComponentSerializer.INSTANCE.deserialize(TextSerializers.JSON.serialize(text));
    }

    public static Tristate fromSpongeTristate(org.spongepowered.api.util.Tristate value) {
        if (value == org.spongepowered.api.util.Tristate.UNDEFINED) {
            return Tristate.UNDEFINED;
        }

        return Tristate.fromBoolean(value.asBoolean());
    }

    public static org.spongepowered.api.util.Tristate getSpongeTristate(Tristate value) {
        if (value == Tristate.UNDEFINED) {
            return org.spongepowered.api.util.Tristate.UNDEFINED;
        }

        return org.spongepowered.api.util.Tristate.fromBoolean(value.asBoolean());
    }

    public static ChatType getSpongeChatType(com.griefdefender.api.ChatType chatType) {
        switch (chatType.getName()) {
            case "CHAT" :
                return ChatTypes.CHAT;
            case "ACTION_BAR" :
                return ChatTypes.ACTION_BAR;
        }
        return ChatTypes.SYSTEM;
    }

    public static Tristate getPermissionValue(Subject subject, ContextSet contexts, String permission) {
        return fromSpongeTristate(subject.getPermissionValue(getSpongeContexts(contexts), permission));
    }
}
