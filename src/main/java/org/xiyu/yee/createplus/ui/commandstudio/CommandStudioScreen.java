package org.xiyu.yee.createplus.ui.commandstudio;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.xiyu.yee.createplus.Createplus;
import org.xiyu.yee.createplus.commands.studio.AiCommandGenerator;
import org.xiyu.yee.createplus.commands.studio.AiSessionHistory;
import org.xiyu.yee.createplus.commands.studio.CommandValidator;
import org.xiyu.yee.createplus.commands.studio.OocExporter;
import org.xiyu.yee.createplus.ui.Theme;

/**
 * AI 指令工作室：生成普通指令 / OOC / 两者。只展示与复制，不执行。
 * 上次提示词与回复会自动恢复。
 */
public class CommandStudioScreen extends Screen {
    private AiCommandGenerator.Mode mode = AiSessionHistory.getMode();
    private String lastCommand = AiSessionHistory.getCommand();
    private String lastOoc = AiSessionHistory.getOoc();
    private String lastStatus = AiSessionHistory.getStatus();
    private boolean busy;

    private MultiLineEditBox aiPrompt;
    private MultiLineEditBox resultBox;

    public CommandStudioScreen() {
        super(Component.translatable("createplus.command_studio.title"));
    }

    @Override
    protected void init() {
        clearWidgets();

        int y = 28;
        addRenderableWidget(Button.builder(modeLabel(AiCommandGenerator.Mode.COMMAND), b -> {
            mode = AiCommandGenerator.Mode.COMMAND;
            persistDraft();
            rebuildWidgets();
        }).bounds(8, y, 90, 20).build());
        addRenderableWidget(Button.builder(modeLabel(AiCommandGenerator.Mode.OOC), b -> {
            mode = AiCommandGenerator.Mode.OOC;
            persistDraft();
            rebuildWidgets();
        }).bounds(102, y, 90, 20).build());
        addRenderableWidget(Button.builder(modeLabel(AiCommandGenerator.Mode.BOTH), b -> {
            mode = AiCommandGenerator.Mode.BOTH;
            persistDraft();
            rebuildWidgets();
        }).bounds(196, y, 100, 20).build());

        aiPrompt = new MultiLineEditBox(this.font, 8, 56, this.width - 16, 70,
            Component.translatable("createplus.command_studio.ai_prompt"),
            Component.translatable("createplus.command_studio.ai_prompt"));
        aiPrompt.setCharacterLimit(4000);
        aiPrompt.setValue(AiSessionHistory.getPrompt());
        aiPrompt.setValueListener(v -> AiSessionHistory.setPrompt(v));
        addRenderableWidget(aiPrompt);

        addRenderableWidget(Button.builder(Component.translatable("createplus.command_studio.generate"), b -> generate())
            .bounds(8, 134, 90, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("createplus.command_studio.validate"), b -> validate())
            .bounds(102, 134, 80, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("createplus.command_studio.copy"), b -> copy(lastCommand))
            .bounds(186, 134, 70, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("createplus.command_studio.copy_ooc"), b -> copy(lastOoc))
            .bounds(260, 134, 90, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("createplus.command_studio.give_cb"), b -> {
            if (!lastCommand.isBlank()) {
                String give = OocExporter.toGiveCommandBlock(lastCommand);
                this.minecraft.keyboardHandler.setClipboard(give);
                lastStatus = "已复制：give 命令方块";
            } else if (!lastOoc.isBlank()) {
                this.minecraft.keyboardHandler.setClipboard(OocExporter.toGiveCommandBlock(lastOoc));
                lastStatus = "已复制：give 命令方块";
            }
        }).bounds(354, 134, 100, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("createplus.command_studio.installer"), b -> {
            String src = !lastCommand.isBlank() ? lastCommand : lastOoc;
            if (!src.isBlank()) {
                lastOoc = OocExporter.toInstaller(src);
                lastStatus = "已生成 FallingBlock 安装包（高级）";
                persistResult();
                refreshResult();
            }
        }).bounds(458, 134, 100, 20).build());

        resultBox = new MultiLineEditBox(this.font, 8, 162, this.width - 16, this.height - 190,
            Component.literal("result"), Component.literal("result"));
        resultBox.setCharacterLimit(32500);
        refreshResult();
        addRenderableWidget(resultBox);
    }

    private Component modeLabel(AiCommandGenerator.Mode m) {
        String key = "createplus.command_studio.mode." + m.name().toLowerCase();
        String text = Component.translatable(key).getString();
        if (mode == m) text = "§a[" + text + "]";
        return Component.literal(text);
    }

    private void generate() {
        if (busy) return;
        busy = true;
        lastStatus = "生成中…";
        String prompt = aiPrompt.getValue();
        AiSessionHistory.setPrompt(prompt);
        AiCommandGenerator.generateAsync(prompt, mode, result -> {
            busy = false;
            if (result.ok()) {
                lastCommand = result.command() == null ? "" : result.command();
                lastOoc = result.ooc() == null ? "" : result.ooc();
                lastStatus = result.message() + (result.retried() ? " (retried)" : "");
            } else {
                lastStatus = result.message();
                if (result.command() != null) lastCommand = result.command();
                if (result.ooc() != null) lastOoc = result.ooc();
            }
            persistResult();
            rebuildWidgets();
        });
    }

    private void validate() {
        String text = resultBox != null ? resultBox.getValue() : lastCommand;
        String target = lastCommand.isBlank() ? text : lastCommand;
        if (mode == AiCommandGenerator.Mode.OOC && !lastOoc.isBlank()) target = lastOoc;
        CommandValidator.ValidationResult r = CommandValidator.validate(target);
        lastStatus = r.ok() ? "OK" : ("ERR@" + r.cursor() + ": " + r.error());
        if (r.ok()) {
            if (mode == AiCommandGenerator.Mode.OOC) lastOoc = r.command();
            else lastCommand = r.command();
        }
        persistResult();
        refreshResult();
    }

    private void refreshResult() {
        if (resultBox == null) return;
        StringBuilder sb = new StringBuilder();
        if (!lastCommand.isBlank()) {
            sb.append("COMMAND:\n").append(lastCommand).append("\n\n");
        }
        if (!lastOoc.isBlank()) {
            sb.append("OOC:\n").append(lastOoc);
        }
        if (sb.isEmpty() && lastStatus != null) sb.append(lastStatus);
        resultBox.setValue(sb.toString());
    }

    private void copy(String text) {
        if (text == null || text.isBlank()) {
            lastStatus = "没有可复制内容";
            return;
        }
        this.minecraft.keyboardHandler.setClipboard(text);
        lastStatus = "已复制到剪贴板";
    }

    private void persistDraft() {
        String prompt = aiPrompt != null ? aiPrompt.getValue() : AiSessionHistory.getPrompt();
        AiSessionHistory.remember(prompt, mode, lastCommand, lastOoc, lastStatus);
        Createplus.FEATURE_MANAGER.saveConfig();
    }

    private void persistResult() {
        String prompt = aiPrompt != null ? aiPrompt.getValue() : AiSessionHistory.getPrompt();
        AiSessionHistory.remember(prompt, mode, lastCommand, lastOoc, lastStatus);
        Createplus.FEATURE_MANAGER.saveConfig();
    }

    @Override
    public void onClose() {
        persistDraft();
        super.onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 10, Theme.TEXT);
        graphics.drawString(this.font, lastStatus == null ? "" : lastStatus, 8, this.height - 12, Theme.TEXT_MUTED, false);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
