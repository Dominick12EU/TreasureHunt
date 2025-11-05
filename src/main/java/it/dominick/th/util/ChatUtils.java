package it.dominick.th.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ChatUtils {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_SERIALIZER =
            LegacyComponentSerializer.legacyAmpersand();

    public static void send(@NotNull Player player, @NotNull String message, @NotNull String... placeholders) {
        Component component = parse(message, placeholders);
        player.sendMessage(component);
    }

    public static void send(@NotNull CommandSender sender, @NotNull String message, @NotNull String... placeholders) {
        Component component = parse(message, placeholders);
        sender.sendMessage(component);
    }

    public static void sendList(@NotNull Player player, @NotNull List<String> messages, @NotNull String... placeholders) {
        messages.forEach(message -> send(player, message, placeholders));
    }

    public static void sendList(@NotNull CommandSender sender, @NotNull List<String> messages, @NotNull String... placeholders) {
        messages.forEach(message -> send(sender, message, placeholders));
    }

    public static Component parse(@NotNull String message, @NotNull String... placeholders) {
        String converted = convertLegacyToMiniMessage(message);
        TagResolver resolver = createPlaceholderResolver(placeholders);

        return MINI_MESSAGE.deserialize(converted, resolver);
    }

    public static Component parse(@NotNull String message, @NotNull Map<String, String> placeholders) {
        String converted = convertLegacyToMiniMessage(message);
        TagResolver resolver = createPlaceholderResolver(placeholders);

        return MINI_MESSAGE.deserialize(converted, resolver);
    }

    public static String toLegacy(@NotNull Component component) {
        return LEGACY_SERIALIZER.serialize(component);
    }

    public static Component fromLegacy(@NotNull String legacy) {
        return LEGACY_SERIALIZER.deserialize(legacy);
    }

    public static List<Component> parseList(@NotNull List<String> messages, @NotNull String... placeholders) {
        return messages.stream()
                .map(message -> parse(message, placeholders))
                .collect(Collectors.toList());
    }

    public static String placeholder(@NotNull String message, @NotNull String... placeholders) {
        String result = message;
        if (placeholders.length % 2 == 0) {
            for (int i = 0; i < placeholders.length; i += 2) {
                String placeholder = placeholders[i];
                String replacement = placeholders[i + 1];
                result = result.replace(placeholder, replacement);
            }
        }
        return result;
    }

    private static TagResolver createPlaceholderResolver(@NotNull String... placeholders) {
        if (placeholders.length == 0 || placeholders.length % 2 != 0) {
            return TagResolver.empty();
        }

        TagResolver.Builder builder = TagResolver.builder();
        for (int i = 0; i < placeholders.length; i += 2) {
            String key = placeholders[i].replace("{", "").replace("}", "");
            String value = placeholders[i + 1];
            builder.resolver(Placeholder.parsed(key, value));
        }
        return builder.build();
    }

    private static TagResolver createPlaceholderResolver(@NotNull Map<String, String> placeholders) {
        if (placeholders.isEmpty()) {
            return TagResolver.empty();
        }

        TagResolver.Builder builder = TagResolver.builder();
        placeholders.forEach((key, value) -> {
            String cleanKey = key.replace("{", "").replace("}", "");
            builder.resolver(Placeholder.parsed(cleanKey, value));
        });
        return builder.build();
    }

    private static String convertLegacyToMiniMessage(@NotNull String message) {
        String result = message;

        result = result.replaceAll("&#([A-Fa-f0-9]{6})", "<#$1>");

        result = result.replaceAll("&x&([A-Fa-f0-9])&([A-Fa-f0-9])&([A-Fa-f0-9])&([A-Fa-f0-9])&([A-Fa-f0-9])&([A-Fa-f0-9])",
                "<#$1$2$3$4$5$6>");

        result = result.replace("&0", "<black>");
        result = result.replace("&1", "<dark_blue>");
        result = result.replace("&2", "<dark_green>");
        result = result.replace("&3", "<dark_aqua>");
        result = result.replace("&4", "<dark_red>");
        result = result.replace("&5", "<dark_purple>");
        result = result.replace("&6", "<gold>");
        result = result.replace("&7", "<gray>");
        result = result.replace("&8", "<dark_gray>");
        result = result.replace("&9", "<blue>");
        result = result.replace("&a", "<green>");
        result = result.replace("&b", "<aqua>");
        result = result.replace("&c", "<red>");
        result = result.replace("&d", "<light_purple>");
        result = result.replace("&e", "<yellow>");
        result = result.replace("&f", "<white>");

        result = result.replace("&l", "<bold>");
        result = result.replace("&m", "<strikethrough>");
        result = result.replace("&n", "<underlined>");
        result = result.replace("&o", "<italic>");
        result = result.replace("&k", "<obfuscated>");
        result = result.replace("&r", "<reset>");

        return result;
    }

    public static String gradient(@NotNull String text, @NotNull String startColor, @NotNull String endColor) {
        return "<gradient:" + startColor + ":" + endColor + ">" + text + "</gradient>";
    }

    public static String rainbow(@NotNull String text) {
        return "<rainbow>" + text + "</rainbow>";
    }

    public static String hover(@NotNull String text, @NotNull String hover) {
        return "<hover:show_text:'" + hover + "'>" + text + "</hover>";
    }

    public static String click(@NotNull String text, @NotNull String action, @NotNull String value) {
        return "<click:" + action + ":'" + value + "'>" + text + "</click>";
    }

    public static String stripColors(@NotNull String message) {
        Component component = parse(message);
        return MINI_MESSAGE.stripTags(message);
    }
}
