package org.xiyu.yee.createplus.features;

import org.xiyu.yee.createplus.settings.BooleanSetting;
import org.xiyu.yee.createplus.settings.IntSetting;
import org.xiyu.yee.createplus.settings.StringSetting;

/**
 * AI 指令工作室：生成指令 / OOC，NBT 补全。纯客户端，不自动执行。
 */
public class CommandStudio extends CreativePlusFeature {
    private final BooleanSetting nbtAutocomplete = boolSetting("nbt_autocomplete", true);
    private final StringSetting aiEndpoint = stringSetting(
        "ai_endpoint", "https://api.openai.com/v1/chat/completions", 512);
    private final StringSetting aiModel = stringSetting("ai_model", "gpt-4o-mini", 128);
    private final StringSetting aiApiKey = stringSetting("ai_api_key", "", 256);
    private final IntSetting aiTimeoutSec = intSetting("ai_timeout_sec", 45, 5, 120);

    public CommandStudio() {
        super("command_studio", "AI 生成指令/OOC 与 NBT 补全（纯客户端，不自动执行）", Category.SYSTEM);
    }

    public boolean shouldSuggestNbt() {
        return isEnabled() && nbtAutocomplete.get();
    }

    public String getAiEndpoint() {
        return aiEndpoint.get();
    }

    public String getAiModel() {
        return aiModel.get();
    }

    public String getAiApiKey() {
        return aiApiKey.get();
    }

    public int getAiTimeoutSec() {
        return aiTimeoutSec.value();
    }

    @Override
    public String getDetailedDescription() {
        return getTranslatedDescription() + "\n§7F7 打开；生成结果仅显示/复制";
    }
}
