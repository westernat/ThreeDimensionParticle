package org.mesdag.thr_dim_particle.client.impl;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.random.Weight;
import org.mesdag.particlestorm.api.IComponent;
import org.mesdag.particlestorm.data.component.ParticleAppearanceBillboard.Direction;
import org.mesdag.particlestorm.data.component.ParticleAppearanceBillboard.FaceCameraMode;
import org.mesdag.particlestorm.data.molang.FloatMolangExp;
import org.mesdag.particlestorm.data.molang.FloatMolangExp3;
import org.mesdag.particlestorm.editor.ComponentGrid;
import org.mesdag.particlestorm.editor.EditorScreen;
import org.mesdag.thr_dim_particle.TDP;
import org.mesdag.thr_dim_particle.client.impl.TDParticleAppearance.ModelAnimation;
import org.mesdag.thr_dim_particle.client.impl.TDParticleAppearance.WeightedModel;
import org.mesdag.thr_dim_particle.client.impl.TDParticleAppearance.WeightedModelEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/// `thr_dim_particle:appearance` 的编辑器 Grid。
///
/// 字段与 [TDParticleAppearance] 一一对应：
/// - `size`：三通道 Molang 尺寸；
/// - `face_camera_mode`：`(none)` + [FaceCameraMode] 全部取值（组件里是 `Optional`）；
/// - `direction`：和广告牌外观一样分「由速度推导」/「自定义方向」两种模式，按模式显示阈值或方向向量；
/// - `model_animation`：逐帧编辑 `type_frames`（每帧可含多个带权重的模型 id），再加上帧率、拉伸到寿命、循环；
///
/// 数据改动保存在 Grid 自己的草稿里，`build()` 时再 new 一个组件；结构性操作（加/删帧、加/删模型）
/// 会 `resetRows()` 重建整张表单。模型动画至少保留一帧、每帧至少保留一个模型，避免运行时空列表越界。
public final class TDParticleAppearanceGrid extends ComponentGrid {
    /// 新建组件/新建帧时用的占位模型（TDP 自带的测试几何体，找不到渲染器时按 DO_NOTHING 处理，不会崩）。
    private static final ResourceLocation DEFAULT_MODEL = TDP.asResource("test_model1");
    private static final List<String> DIRECTION_MODES = List.of("derive_from_velocity", "custom_direction");
    private static final int DERIVE_FROM_VELOCITY = 0;
    private static final int CUSTOM_DIRECTION = 1;

    private FloatMolangExp3 size = defaultSize();
    private Optional<FaceCameraMode> faceCameraMode = Optional.empty();
    private int directionMode = DERIVE_FROM_VELOCITY;
    private float minSpeedThreshold = Direction.DEFAULT.minSpeedThreshold();
    private FloatMolangExp3 customDirection = FloatMolangExp3.ZERO;
    /// 草稿：每帧 = 一组带权重的模型。
    private final List<List<ModelEntryDraft>> frames = new ArrayList<>();
    private float framesPerSecond = 1.0F;
    private boolean stretchToLifetime;
    private boolean loop;

    public TDParticleAppearanceGrid(EditorScreen screen, Font font) {
        super(screen, font);
        this.frames.add(defaultFrame());
        this.rebuildRows();
    }

    @Override
    public BuiltComponent createDefault() {
        return new BuiltComponent(TDParticleAppearance.ID,
                new TDParticleAppearance(defaultSize(), Optional.empty(), Direction.DEFAULT, defaultAnimation()));
    }

    @Override
    public BuiltComponent build() {
        Direction direction = this.directionMode == CUSTOM_DIRECTION
                ? new Direction(Direction.Mode.CUSTOM_DIRECTION, 0.0F, this.customDirection)
                : new Direction(Direction.Mode.DERIVE_FROM_VELOCITY, this.minSpeedThreshold, FloatMolangExp3.ZERO);
        List<WeightedModel> typeFrames = new ArrayList<>(this.frames.size());
        for (List<ModelEntryDraft> frame : this.frames) {
            if (frame.isEmpty()) {
                throw new IllegalArgumentException("Model animation frame must have at least one model");
            }
            List<WeightedModelEntry> entries = new ArrayList<>(frame.size());
            for (ModelEntryDraft entry : frame) {
                ResourceLocation model = ResourceLocation.tryParse(entry.model.trim());
                if (model == null) {
                    throw new IllegalArgumentException("Invalid model id: '" + entry.model + "'");
                }
                entries.add(new WeightedModelEntry(model, Weight.of(Mth.clamp(entry.weight, 1, 1_000_000))));
            }
            typeFrames.add(new WeightedModel(entries));
        }
        if (typeFrames.isEmpty()) {
            throw new IllegalArgumentException("Model animation requires at least one frame");
        }
        ModelAnimation animation = new ModelAnimation(typeFrames,
                Math.max(0.001F, this.framesPerSecond), this.stretchToLifetime, this.loop);
        return new BuiltComponent(TDParticleAppearance.ID,
                new TDParticleAppearance(this.size, this.faceCameraMode, direction, animation));
    }

    @Override
    public void load(IComponent component) {
        TDParticleAppearance appearance = (TDParticleAppearance) component;
        this.size = appearance.size();
        this.faceCameraMode = appearance.faceCameraMode();
        Direction direction = appearance.direction();
        this.directionMode = direction.mode() == Direction.Mode.CUSTOM_DIRECTION ? CUSTOM_DIRECTION : DERIVE_FROM_VELOCITY;
        this.minSpeedThreshold = direction.minSpeedThreshold();
        this.customDirection = direction.customDirection();
        this.frames.clear();
        for (WeightedModel frame : appearance.modelAnimation().typeFrames()) {
            List<ModelEntryDraft> entries = new ArrayList<>();
            for (WeightedModelEntry entry : frame.unwrap()) {
                entries.add(new ModelEntryDraft(entry.modelType().toString(), entry.weight().asInt()));
            }
            if (entries.isEmpty()) {
                entries.add(new ModelEntryDraft(DEFAULT_MODEL.toString(), 1));
            }
            this.frames.add(entries);
        }
        if (this.frames.isEmpty()) {
            this.frames.add(defaultFrame());
        }
        this.framesPerSecond = Math.max(0.001F, appearance.modelAnimation().framesPerSecond());
        this.stretchToLifetime = appearance.modelAnimation().stretchToLifetime();
        this.loop = appearance.modelAnimation().loop();
        this.rebuildRows();
    }

    private void rebuildRows() {
        this.resetRows();
        this.addAppearanceRows();
        this.addAnimationRows();
    }

    private void addAppearanceRows() {
        this.vectorRow("size", this.size, value -> {
            this.size = value;
            this.markChanged();
        });

        List<String> cameraModes = new ArrayList<>();
        cameraModes.add(Component.translatable("particle_editor.thr_dim_particle.appearance.face_camera_mode_none").getString());
        for (FaceCameraMode mode : FaceCameraMode.values()) {
            cameraModes.add(optionLabel("particle_appearance_billboard", "face_camera_mode_" + mode.getSerializedName()));
        }
        int cameraModeIndex = this.faceCameraMode.map(mode -> mode.ordinal() + 1).orElse(0);
        this.selectRow("face_camera_mode", cameraModes, cameraModeIndex, index -> {
            this.faceCameraMode = index <= 0 ? Optional.empty() : Optional.of(FaceCameraMode.values()[index - 1]);
            this.markChanged();
        });

        this.selectRow("direction_mode",
                localizedOptions("particle_appearance_billboard", DIRECTION_MODES.stream()
                        .map(value -> "direction_mode_" + value).toList()),
                this.directionMode, index -> {
                    this.directionMode = index;
                    this.markChanged();
                    this.rebuildRows();
                });
        if (this.directionMode == CUSTOM_DIRECTION) {
            this.vectorRow("custom_direction", this.customDirection, value -> {
                this.customDirection = value;
                this.markChanged();
            });
        } else {
            this.textRow("min_speed_threshold", String.valueOf(this.minSpeedThreshold), text -> {
                this.minSpeedThreshold = floatOf(text);
                this.markChanged();
            });
        }
    }

    private void addAnimationRows() {
        this.infoRow(() -> Component.translatable("particle_editor.thr_dim_particle.appearance.model_animation").getString(),
                Component.translatable("particle_editor.thr_dim_particle.appearance.frame_count", this.frames.size()).getString());
        for (int frameIndex = 0; frameIndex < this.frames.size(); frameIndex++) {
            List<ModelEntryDraft> frame = this.frames.get(frameIndex);
            int displayFrame = frameIndex + 1;
            this.infoRow(() -> Component.translatable("particle_editor.thr_dim_particle.appearance.frame", displayFrame).getString(),
                    Component.translatable("particle_editor.thr_dim_particle.appearance.frame_entries", frame.size()).getString());
            for (int entryIndex = 0; entryIndex < frame.size(); entryIndex++) {
                ModelEntryDraft entry = frame.get(entryIndex);
                int displayEntry = entryIndex + 1;
                this.textRow(() -> Component.translatable("particle_editor.thr_dim_particle.appearance.model_entry",
                                displayFrame, displayEntry).getString(),
                        entry.model, text -> {
                            entry.model = text;
                            this.markChanged();
                        });
                this.textRow(() -> Component.translatable("particle_editor.thr_dim_particle.appearance.weight_entry",
                                displayFrame, displayEntry).getString(),
                        String.valueOf(entry.weight), text -> {
                            entry.weight = Math.max(1, Math.round(floatOf(text)));
                            this.markChanged();
                        });
                if (frame.size() > 1) {
                    this.buttonRow(Component.translatable("particle_editor.thr_dim_particle.appearance.remove_entry").getString(), () -> {
                        frame.remove(entry);
                        this.markChanged();
                        this.rebuildRows();
                    });
                }
            }
            this.buttonRow(Component.translatable("particle_editor.thr_dim_particle.appearance.add_entry").getString(), () -> {
                frame.add(new ModelEntryDraft(DEFAULT_MODEL.toString(), 1));
                this.markChanged();
                this.rebuildRows();
            });
            if (this.frames.size() > 1) {
                int frameToRemove = frameIndex;
                this.buttonRow(Component.translatable("particle_editor.thr_dim_particle.appearance.remove_frame").getString(), () -> {
                    this.frames.remove(frameToRemove);
                    this.markChanged();
                    this.rebuildRows();
                });
            }
        }
        this.buttonRow(Component.translatable("particle_editor.thr_dim_particle.appearance.add_frame").getString(), () -> {
            this.frames.add(defaultFrame());
            this.markChanged();
            this.rebuildRows();
        });

        this.textRow("frames_per_second", String.valueOf(this.framesPerSecond), text -> {
            this.framesPerSecond = Math.max(0.001F, floatOf(text));
            this.markChanged();
        });
        this.toggleRow("stretch_to_lifetime", this.stretchToLifetime, value -> {
            this.stretchToLifetime = value;
            this.markChanged();
        });
        this.toggleRow("loop", this.loop, value -> {
            this.loop = value;
            this.markChanged();
        });
    }

    private static FloatMolangExp3 defaultSize() {
        FloatMolangExp value = FloatMolangExp.ofConstant(0.1F);
        return new FloatMolangExp3(value, value, value);
    }

    private static List<ModelEntryDraft> defaultFrame() {
        List<ModelEntryDraft> frame = new ArrayList<>();
        frame.add(new ModelEntryDraft(DEFAULT_MODEL.toString(), 1));
        return frame;
    }

    private static ModelAnimation defaultAnimation() {
        return new ModelAnimation(List.of(new WeightedModel(List.of(
                new WeightedModelEntry(DEFAULT_MODEL, Weight.of(1))))), 1.0F, false, false);
    }

    /// 草稿里的一个模型条目；提交时才转成 [WeightedModelEntry]。
    private static final class ModelEntryDraft {
        String model;
        int weight;

        ModelEntryDraft(String model, int weight) {
            this.model = model;
            this.weight = weight;
        }
    }
}
