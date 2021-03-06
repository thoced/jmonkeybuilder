package com.ss.editor.ui.control.property.impl;

import static com.ss.rlib.util.ObjectUtils.notNull;
import com.ss.editor.annotation.FxThread;
import com.ss.editor.annotation.FromAnyThread;
import com.ss.editor.model.undo.editor.ChangeConsumer;
import com.ss.editor.ui.control.property.PropertyControl;
import com.ss.editor.ui.css.CssClasses;
import com.ss.rlib.function.SixObjectConsumer;
import com.ss.rlib.ui.control.input.FloatTextField;
import com.ss.rlib.ui.util.FXUtils;
import javafx.scene.layout.HBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;

/**
 * The implementation of the {@link PropertyControl} to edit float values.
 *
 * @param <C> the type parameter
 * @param <T> the type parameter
 * @author JavaSaBr
 */
public class FloatPropertyControl<C extends ChangeConsumer, T> extends PropertyControl<C, T, Float> {

    /**
     * The filed with current value.
     */
    @Nullable
    private FloatTextField valueField;

    public FloatPropertyControl(@Nullable final Float propertyValue, @NotNull final String propertyName,
                                @NotNull final C changeConsumer) {
        super(propertyValue, propertyName, changeConsumer);
    }

    public FloatPropertyControl(@Nullable final Float propertyValue, @NotNull final String propertyName,
                                @NotNull final C changeConsumer,
                                @Nullable final SixObjectConsumer<C, T, String, Float, Float, BiConsumer<T, Float>> changeHandler) {
        super(propertyValue, propertyName, changeConsumer, changeHandler);
    }

    @Override
    @FxThread
    public void changeControlWidthPercent(final double controlWidthPercent) {
        super.changeControlWidthPercent(controlWidthPercent);

        final FloatTextField valueField = getValueField();
        valueField.prefWidthProperty().unbind();
        valueField.prefWidthProperty().bind(widthProperty().multiply(controlWidthPercent));
    }

    @Override
    @FxThread
    protected void createComponents(@NotNull final HBox container) {
        super.createComponents(container);

        valueField = new FloatTextField();
        valueField.addChangeListener((observable, oldValue, newValue) -> updateValue());
        valueField.prefWidthProperty()
                .bind(widthProperty().multiply(CONTROL_WIDTH_PERCENT));

        FXUtils.addClassTo(valueField, CssClasses.ABSTRACT_PARAM_CONTROL_COMBO_BOX);
        FXUtils.addToPane(valueField, container);
    }

    /**
     * Sets scroll power.
     *
     * @param scrollPower the scroll power.
     */
    @FxThread
    public void setScrollPower(final float scrollPower) {
        getValueField().setScrollPower(scrollPower);
    }

    /**
     * Gets scroll power.
     *
     * @return the scroll power.
     */
    @FxThread
    public float getScrollPower() {
        return getValueField().getScrollPower();
    }

    /**
     * Set value limits for this field.
     *
     * @param min the min value.
     * @param max the max value.
     */
    @FxThread
    public void setMinMax(final float min, final float max) {
        getValueField().setMinMax(min, max);
    }

    @Override
    @FromAnyThread
    protected boolean isSingleRow() {
        return true;
    }

    /**
     * @return the filed with current value.
     */
    @FxThread
    private @NotNull FloatTextField getValueField() {
        return notNull(valueField);
    }

    @Override
    @FxThread
    protected void reload() {
        final Float value = getPropertyValue();
        final FloatTextField valueField = getValueField();
        final int caretPosition = valueField.getCaretPosition();
        valueField.setValue(value == null ? 0F : value);
        valueField.positionCaret(caretPosition);
    }

    /**
     * Update the value.
     */
    @FxThread
    private void updateValue() {
        if (isIgnoreListener()) return;

        final FloatTextField valueField = getValueField();
        final float value = valueField.getValue();

        final Float oldValue = getPropertyValue();
        changed(value, oldValue);
    }
}
