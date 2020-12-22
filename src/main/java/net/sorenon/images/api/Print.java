package net.sorenon.images.api;

import net.fabricmc.fabric.api.util.NbtType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextHandler;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.UUID;

public class Print {
    @Nullable
    public URL url;

    @Nullable
    public UUID player;

    public MutableText toText(@Nullable World world) {
        if (url != null) {
            String urlStr = url.toString();
            MutableText text = Texts.bracketed(new LiteralText(urlStr)).styled(style -> style.withClickEvent(
                    new ClickEvent(
                            ClickEvent.Action.OPEN_URL,
                            urlStr
                    )
            ).withHoverEvent(
                    new HoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            new TranslatableText("images.open_or_copy_link")
                    )
            )).formatted(Formatting.GREEN);

            if (player != null) {
                PlayerEntity printer = null;
                if (world != null) {
                    printer = world.getPlayerByUuid(player);
                }
                String string;
                if (printer == null) {
                    string = player.toString();
                } else {
                    string = printer.getName().asString();
                }
                text.append(new LiteralText("->" + string).styled(style ->
                        style.withClickEvent(
                                new ClickEvent(
                                        ClickEvent.Action.COPY_TO_CLIPBOARD,
                                        player.toString()
                                )
                        ).withHoverEvent(
                                new HoverEvent(
                                        HoverEvent.Action.SHOW_TEXT,
                                        new TranslatableText("images.copy_player")
                                )
                        )
                ).formatted(Formatting.WHITE));
            }
            return text;
        }
        return null;
    }

    public void serialize(CompoundTag nbt) {
        if (url != null) {
            nbt.putString("url", url.toString());
        }
        if (player != null) {
            nbt.putUuid("player", player);
        }
    }

    public void deserialize(CompoundTag nbt) {
        if (nbt == null) return;
        player = null;
        url = null;
        if (nbt.contains("player")) {
            player = nbt.getUuid("player");
        }
        if (nbt.contains("url", NbtType.STRING)) {
            try {
                url = new URL(nbt.getString("url"));
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }
    }

    public void appendTooltip(List<Text> tooltip, World world, boolean isSneaking, int maxWidth) {
        if (url == null) return;

        String urlStr = url.toString();
        Style urlStyle = Style.EMPTY.withFormatting(Formatting.GREEN);
        Style playerStyle = Style.EMPTY.withFormatting(Formatting.GRAY);

        PlayerEntity printerEntity = null;
        if (player != null) {
            printerEntity = world.getPlayerByUuid(player);
        }

        if (!isSneaking) {
            boolean more = false;
            if (urlStr.length() > 24) {
                urlStr = urlStr.substring(0, 24) + '…';
                more = true;
            }
            tooltip.add(new LiteralText(urlStr).setStyle(urlStyle));
            if (player != null) {
                if (printerEntity != null) {
                    tooltip.add(new TranslatableText("images.printed_by", printerEntity.getName()).setStyle(playerStyle));
                }
                else {
                    tooltip.add(new TranslatableText("images.printed_by_offline").setStyle(playerStyle));
                    more = true;
                }
            }
            if (more) {
                tooltip.add(new TranslatableText("images.sneak_for_more").formatted(Formatting.GRAY, Formatting.ITALIC));
            }
        } else {
            TextHandler textHandler = MinecraftClient.getInstance().textRenderer.getTextHandler();
            while (urlStr.length() > 0) {
                int maxLen = textHandler.getTrimmedLength(urlStr, maxWidth, urlStyle);
                tooltip.add(new LiteralText(urlStr.substring(0, maxLen)).setStyle(urlStyle));
                urlStr = urlStr.substring(Math.min(maxLen, urlStr.length()));
            }

            if (player != null) {
                PlayerEntity printer = world.getPlayerByUuid(player);
                if (printer == null) {
                    tooltip.add(new TranslatableText("images.printed_by", player.toString()).setStyle(playerStyle));
                } else {
                    tooltip.add(new TranslatableText("images.printed_by", printer.getName()).setStyle(playerStyle));
                }
            }
        }
    }

    public Print copy() {
        Print clone = new Print();
        clone.url = url;
        clone.player = player;
        return clone;
    }
}
