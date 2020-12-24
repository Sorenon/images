package net.sorenon.images.init

import io.github.prospector.modmenu.api.ConfigScreenFactory
import io.github.prospector.modmenu.api.ModMenuApi
import me.shedaniel.clothconfig2.api.ConfigBuilder
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.text.TranslatableText

class ImagesModMenuConfig : ModMenuApi {

    override fun getModConfigScreenFactory(): ConfigScreenFactory<*> {
        if (FabricLoader.getInstance().isModLoaded("cloth-config2")) {
            return ConfigScreenFactory { parent ->
                ImagesModClient.reloadConfig()

                val builder =
                    ConfigBuilder.create().setParentScreen(parent).setTitle(TranslatableText("title.images.config"))

                val general = builder.getOrCreateCategory(TranslatableText("category.images.general"))

                general.addEntry(builder.entryBuilder()
                    .startIntField(TranslatableText("option.images.imageIdleTicks"), ImagesModClient.CFG_IDLE_TICKS)
                    .setDefaultValue(2 * 60 * 20)
                    .setTooltip(TranslatableText("option.images.imageIdleTicks.tooltip"))
                    .setMin(0)
                    .setSaveConsumer { newValue: Int ->
                        ImagesModClient.CFG_IDLE_TICKS = newValue
                        ImagesModClient.saveConfig()
                    }
                    .build())
                general.addEntry(builder.entryBuilder()
                    .startIntField(TranslatableText("option.images.maxImageSize"), ImagesModClient.CFG_MAX_SIZE)
                    .setDefaultValue(1024 * 1024 * 2)
                    .setTooltip(TranslatableText("option.images.maxImageSize.tooltip"))
                    .setMin(0)
                    .setSaveConsumer { newValue: Int ->
                        ImagesModClient.CFG_MAX_SIZE = newValue
                        ImagesModClient.saveConfig()
                    }
                    .build())
                general.addEntry(builder.entryBuilder()
                    .startBooleanToggle(TranslatableText("option.images.tooltipURL"), ImagesModClient.CFG_TOOLTIP_URL)
                    .setDefaultValue(true)
                    .setSaveConsumer { newValue: Boolean ->
                        ImagesModClient.CFG_TOOLTIP_URL = newValue
                        ImagesModClient.saveConfig()
                    }
                    .build())
                general.addEntry(builder.entryBuilder()
                    .startBooleanToggle(TranslatableText("option.images.tooltipPlayer"), ImagesModClient.CFG_TOOLTIP_PLAYER)
                    .setDefaultValue(true)
                    .setSaveConsumer { newValue: Boolean ->
                        ImagesModClient.CFG_TOOLTIP_PLAYER = newValue
                        ImagesModClient.saveConfig()
                    }
                    .build())
                if (FabricLoader.getInstance().isModLoaded("waila")) {
                    general.addEntry(builder.entryBuilder()
                        .startBooleanToggle(TranslatableText("option.images.wailaURL"), ImagesModClient.CFG_WAILA_URL)
                        .setDefaultValue(true)
                        .setSaveConsumer { newValue: Boolean ->
                            ImagesModClient.CFG_WAILA_URL = newValue
                            ImagesModClient.saveConfig()
                        }
                        .build())
                    general.addEntry(builder.entryBuilder()
                        .startBooleanToggle(TranslatableText("option.images.wailaPlayer"), ImagesModClient.CFG_WAILA_PLAYER)
                        .setDefaultValue(false)
                        .setSaveConsumer { newValue: Boolean ->
                            ImagesModClient.CFG_WAILA_PLAYER = newValue
                            ImagesModClient.saveConfig()
                        }
                        .build())
                }

                builder.build()
            }
        }

        return super.getModConfigScreenFactory()
    }

    override fun getModId(): String {
        return "images"
    }
}